# Mailqueue: Verarbeitung und unklare Versandausgänge

`EmailQueueService` koordiniert Worker über die Datenbank, nicht über einen
prozesslokalen Java-Lock. Die Kandidatenliste ist keine Reservierung:

1. Jeder Kandidat wird in einer eigenen `REQUIRES_NEW`-Transaktion mit
   `PESSIMISTIC_WRITE` frisch geladen. Nur `PENDING` wird auf `IN_PROGRESS`
   gesetzt. Erst nach erfolgreichem Claim-Commit darf der Worker versenden.
2. Der Versand läuft ohne offene Queue-Transaktion. Andere Worker überspringen
   das committete `IN_PROGRESS`, auch wenn sie denselben Kandidaten zuvor gelesen
   haben. Produzenten behandeln `IN_PROGRESS` weiterhin als offen und deduplizieren
   unter dem unveränderten Account-Lock.
3. Das Ergebnis wird in einer weiteren eigenen Transaktion unter Queue-Lock
   gespeichert. Nur das erwartete `IN_PROGRESS` mit unverändertem Retry-Zähler
   darf abgeschlossen werden; terminale Zustände werden nicht überschrieben.
4. Erst nach dem Ergebnis-Commit erfolgt bei `FAILED` die Adminbenachrichtigung.
   Nachrichten-, Konfigurations-, Speicher- oder Admin-Runtimefehler werden pro
   Kandidat protokolliert; andere Einträge können weiterlaufen. Bereits committete
   Ergebnisse werden nicht durch spätere Fehler oder einen Aufruferrollback verloren.

## Retry und IN_PROGRESS

Fällige PENDING-Nachrichten werden deterministisch nach `createdAt` aufsteigend
und bei Gleichstand nach `id` aufsteigend ausgewählt. Die Kapazitätsgrenze wird
erst nach dem Fälligkeitsfilter angewendet: `lastRetryAt == null` ist sofort
fällig; andernfalls muss `lastRetryAt <= jetzt - 1 Minute` sein (Gleichheit gilt
als fällig). Unter dem Claim-Lock wird diese Bedingung frisch erneut geprüft,
damit ein anderer Worker nach einem zwischenzeitlich committeten Retryfehler
keinen vorzeitigen weiteren Versuch startet. Die Auswahl ist eine Momentaufnahme;
parallel arbeitende Worker garantieren keine globale SMTP-Abschlussreihenfolge.

Der einfache feste Abstand beträgt eine Minute ab dem gespeicherten Fehlerzeitpunkt,
ohne exponentielles Backoff oder neue Konfigurationsoption. Nicht fällige Nachrichten
verbrauchen keine Auswahlplätze. Jeder Aufruf verarbeitet höchstens seine ausgewählte
Seite; es gibt keine zusätzliche Nachfüllschleife oder weitergehende Fairnessgarantie.

Bei eindeutig vor dem Transport gescheiterten Verbindungs-, MIME-/Authentifizierungs- oder
Vorbereitungsfehlern gilt weiter: initialer Versuch plus konfigurierte
Wiederholungen. Der Retry-Zähler zählt Wiederholungen, die angezeigte aktuelle
Versuchszahl ist `retryCount + 1`. Der konfigurierte Grenzwert und seine bisherige
Behandlung werden nicht geändert.

Der konfigurierte `ConnectionAwareJavaMailSender` markiert `MessagingException`
direkt beim fehlgeschlagenen `connectTransport`, bevor Spring `sendMessage`
aufrufen kann. `EmailService` übersetzt die von Spring erzeugte `MailSendException`
nur dann in `MailPreparationException`, wenn Ursache und einziger Eintrag der
Failed-Messages-Map dieselbe Markierung tragen und genau die aktuell versandte
MIME-Nachricht betreffen. Das erhält begrenzte Retries auch bei Verbindungsfehlern,
ohne Fehlertexte oder etwa eine `ConnectException` allein als Beweis zu verwenden.
Spring-Authentifizierungsfehler behalten ihre bisherige Klassifizierung. Ein
unmarkierter Sender oder ein anderer/batchweiser Nachrichtenkontext gilt nicht
als dieser Nachweis. Die Markierung ist pro Aufruf, ohne gemeinsam veränderlichen
Senderzustand. Runtimefehler ohne diese Markierung bleiben konservativ unklar.

Eine unmarkierte `MailSendException` beweist dagegen keine Nichtzustellung: etwa bei verlorenem
SMTP-Antwortpaket kann die Nachricht bereits angenommen sein. Deshalb bleiben
unklare Transport-/Closefehler konservativ auf `IN_PROGRESS`, ohne automatischen Retry oder
terminalen Adminversand. Auch unerwartete Sender-Runtimefehler, Prozessabbruch
nach dem Claim sowie fehlgeschlagene Ergebnis-Speicherung/Commits lassen den
committeten Claim bestehen. Ein vor Versand fehlgeschlagenes Claim wird nicht
versandt und kann nach Rollback weiter `PENDING` sein.

`IN_PROGRESS` wird weder durch Alter/Timeout noch durch einen Neustart automatisch
freigegeben oder von der SENT-Bereinigung gelöscht. Es gibt hier keine neue
Recovery-Oberfläche. Vor jeder manuellen Änderung muss der Betrieb sicherstellen,
dass kein Worker mehr aktiv ist, und Queue-ID/Empfänger/Zeitpunkt mit SMTP- und
Anwendungsprotokollen abgleichen. Ohne gesicherten Ausgang darf der Eintrag nicht
blind zurück auf `PENDING` gesetzt werden. Auch ein alter Claim allein beweist
keinen gescheiterten Versand. Solche Einträge können neue gleichartige Mails für
das Konto blockieren; diese konservative Einschränkung ist beabsichtigt.

## Verbleibende Konsistenzlücke

SMTP-Annahme und Datenbank-Commit sind **nicht atomar**. Nach erfolgreicher Annahme
und vor dem SENT-Commit kann ein Absturz einen unklaren `IN_PROGRESS` hinterlassen.
Auch ein fehlgeschlagener Commit kann einen unklaren Ausgang haben. Es gibt keine
Exactly-once-Garantie, keinen SMTP-Idempotenzschlüssel und keine automatische
Recovery mit sicherer Entscheidung zwischen Verlust und Doppelzustellung.
Konservative Quarantäne verhindert automatische erneute Zustellung, garantiert
aber nicht, dass jeder Eintrag zugestellt wird oder selbständig fortschreitet.

Die automatisierten Persistenzbelege verwenden isoliertes H2 und gemockten Versand.
Sie ersetzen keine reale SMTP-, Prozessabsturz- oder Multiinstanz-Deploymentprüfung.

## Eingabe-, Konfigurations- und Zeitverträge

Einreihung benötigt einen nicht-null Benutzer mit nicht-null persistierter ID,
einen nicht-null Typ sowie nicht-null Betreff und Body. Die offene Statusabfrage
benötigt ebenfalls Benutzer, ID und Typ. Null verletzt den Vertrag und wird vor
Repositoryzugriff abgewiesen. Leere Betreff-/Bodywerte bleiben erlaubt, unverändert
und ungetrimmt. Keine zusätzliche Adressformat-/Enabled-Regel; der Benutzer wird
weiter unter dem bestehenden Account-Lock frisch geladen.

Vor Auswahl/Claims/Versand prüft jeder Verarbeitungslauf positive Kapazität und
nichtnegative Retryzahl. Das validierte Retrylimit bleibt für diesen Lauf konstant;
Änderungen gelten ab dem nächsten Aufruf. Ungültige Werte schlagen fehl, statt
erst nach Versand eine Nachricht zu blockieren. Bereinigung erfordert eine gültige,
nicht-null, positive ISO-8601-Dauer; Null, Null-/Negativdauer und Parsefehler führen
zu keiner Löschung. Es gibt keine erfundenen Obergrenzen und keinen stillen Fallback.

`EmailQueueService` verwendet eine injizierbare, nicht-null `Clock`, standardmäßig
`Clock.systemDefaultZone()` wie der Passwortreset-Service. Einreihungszeit,
Retryzeitpunkt, Fälligkeitsprüfung und Bereinigung lesen `LocalDateTime.now(clock)`.
Das lokale Zeitmodell bleibt erhalten; keine Umstellung auf UTC/Instant-Spalten.
Sommerzeit- oder rückwärts gerichtete lokale Uhränderungen können den realen
Retryabstand beeinflussen. Der Abstand ist bewusst ein lokaler Zeitvertrag,
kein monotones Wall-Clock-Versprechen zwischen mehreren Instanzen.

Die SENT-Bereinigung behält strikt `createdAt < jetzt - Dauer`. Eintrag genau auf
der Grenze bleibt erhalten. `lastRetryAt` und ein jüngster Versand ändern die
Altersbasis nicht: Eine alte Einreihung kann kurz nach erfolgreichem Versand gelöscht
werden. PENDING, IN_PROGRESS und FAILED werden dadurch weiterhin nicht bereinigt.
