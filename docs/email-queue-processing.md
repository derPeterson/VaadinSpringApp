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
werden. PENDING, IN_PROGRESS und FAILED werden durch diese SENT-Bereinigung weiterhin
nicht bereinigt.

## FAILED-Aufbewahrung

Die eigene geplante Aufgabe `deleteFailedEmails` läuft wie die SENT-Bereinigung
sonntags um 03:00 Uhr, aber in einer separaten Transaktion und unabhängig von deren
konfigurierter Dauer. Sie löscht ausschließlich `FAILED` mit
`lastRetryAt < LocalDateTime.now(clock).minusDays(30)`. Die Grenze ist strikt:
Gleichheit bleibt erhalten; erst beim nächsten Lauf mit überschrittener Grenze wird
gelöscht. Es sind 30 Tage im vorhandenen lokalen Zeitmodell, keine Garantie von
720 Echtzeitstunden über Sommerzeitwechsel. Maßgeblich ist der letzte committete
fehlgeschlagene Versuch, nicht `createdAt`. Ein terminaler Fehler setzt `lastRetryAt`
bereits im bestehenden Ergebnis-Commit frisch. Fehlende Zeitangaben werden weder
ersetzt noch gelöscht. PENDING und IN_PROGRESS bleiben vollständig unangetastet.
Ein fehlgeschlagener Cleanup-Commit rollt dessen Löschungen zurück.

## Dauerhafte Adminbeobachtbarkeit

Nach dem FAILED-Ergebnis-Commit wird für den Adminversuch eine eigene
`REQUIRES_NEW`-Transaktion committet: `queueId`, `startedAt`, `IN_PROGRESS`.
Erst nach erfolgreichem Commit wird die Adminadresse gelesen und Versand vorbereitet.
SMTP läuft weiter ohne Datenbanktransaktion. Anschließend wird unter Audit-Lock in
einer weiteren `REQUIRES_NEW`-Transaktion der Ausgang mit `completedAt` gespeichert:

- `SENT`: Senderaufruf erfolgreich zurückgekehrt; keine Garantie von Postfachzustellung.
- `NOT_SENT`: Fehler vor dem Senderaufruf (z. B. Admin-Konfiguration) oder expliziter
  Authentifizierungs-/MIME-Vorbereitungs-/Parsefehler, der Nichtversand belegt.
- `UNKNOWN`: unmarkierter Transportfehler (`MailSendException`) oder unerwarteter
  Runtimefehler während des Senderaufrufs; keine Text-/Cause-Heuristiken.
- `IN_PROGRESS` ohne Abschluss: Start ist dauerhaft, aber kein Ergebnis dauerhaft
  bestätigt; etwa Prozessabbruch oder Ergebnis-Save-/Commitfehler. Auch ein Abbruch
  vor dem eigentlichen Versand kann so aussehen. Kein Beweis einer SMTP-Annahme.

`sendAdminEmail` nutzt weiterhin den vorhandenen SimpleMailMessage-Vertrag. Die F6-
MIME-Nichtversandmarkierung des normalen Queueversands wird nicht ohne exakten
Nachrichtenbeweis auf diesen anderen Aufruf übertragen. Deshalb kann selbst ein
Admin-Verbindungsfehler konservativ UNKNOWN sein. Alle technischen Exceptiondetails
bleiben im Log; die neue Tabelle enthält keine Exceptiontexte, Empfänger, Benutzer,
Betreffzeilen, Mailbodies oder Tokens. Bestehender Inhalt der Adminmail bleibt erhalten.

Speicherfehler verändern FAILED oder andere bereits abgeschlossene Queueeinträge
nicht. Start-Save-/Commitfehler verhindern Versand; ein zweifelhafter Start-Commit
wird nicht erneut angelegt. Ergebnis-Save-/Commitfehler lösen keinen zweiten Versand
und keinen nachträglichen Ersatzstatus aus. Ein tatsächlich committetes Ergebnis
kann trotz nachfolgender Exception vorhanden sein; Betreiber lesen den frischen
Datenbankzustand. Der unique `queue_id` erlaubt höchstens einen Auditversuch je
Queue-ID, auch nach versehentlicher manueller Wiederverwendung. Es gibt keine
automatischen Admin-Retries, auch nicht für NOT_SENT. Fehlende Auditzeilen sind
**kein Nichtversandbeweis** und dürfen nicht als erfolgreiche Benachrichtigung gelten:
ein Crash zwischen FAILED-Commit und Auditstart oder ein Datenbankausfall kann die
Aufzeichnung verhindern. Bei einem vollständig ausgefallenen Speicher ist dauerhafte
Speicherung nicht garantierbar. Logs und Überwachung müssen diese Lücke abdecken.

Die Auditzeilen überleben Queue-/Benutzerlöschung bewusst. Sie werden hier nicht
automatisch bereinigt; keine neue Admin-Retention-Policy oder Recovery-Oberfläche.

## Manueller Recovery-Vertrag / Betriebsvorgehen

1. Betroffene Queue-ID, Status, Retryzähler, `createdAt`, `lastRetryAt` und
   Untersuchungszeit in einem zugriffsgeschützten Betriebsvorgang erfassen. Zeitstempel
   können vorangegangene Versuche betreffen: IN_PROGRESS hat keinen eigenen
   Dispatchzeitstempel. Nur erforderliche Daten einsehen, keine Mailinhalte/Token oder
   vollständigen Benutzerentitäten in zusätzliche Fehler-/Auditdaten kopieren.
2. **Alle** Worker aller Instanzen stoppen, Scheduler deaktivieren und Neustarts/
   Deployment-Autorestarts unterbinden. Bereits gestartete Aufrufe und SMTP-Sockets
   müssen nachweislich beendet sein. Nur ein abgelaufener Timeout, das Alter des
   Eintrags oder die Beendigung einer einzigen Instanz reichen nicht.
3. Mit Queue-ID aus dem Anwendungslog das tatsächliche Versandfenster bestimmen.
   Empfänger und Zeitfenster mit SMTP-/Providerprotokollen einschließlich Annahme-
   antworten, Relay-IDs und ggf. Providerrecherche abgleichen. Fehlende Logs,
   fehlende Postfachmail, ein Timeout oder fehlendes SENT sind kein Beweis für
   Nichtversand. Wiederholte ähnliche Mails dürfen nicht verwechselt werden. Bei
   unvollständiger Korrelation bleibt der Ausgang unklar.
4. Ergebnis dokumentieren: nachgewiesene SMTP-Annahme bedeutet **keine** erneute
   Einreihung; endgültige Zustellung ist davon verschieden. Bleibt der Ausgang
   unklar, IN_PROGRESS unverändert quarantänisieren und eskalieren. Eine eventuell
   erforderliche manuelle terminale Korrektur benötigt eine separate autorisierte
   Betriebsentscheidung; nicht einfach alle alten Claims verändern.
5. Nur wenn **Nichtversand des betroffenen Versuchs positiv nachgewiesen** und alle
   Worker sicher beendet sind, eine gezielte Wiederfreigabe autorisieren. Vorher
   Benutzerkonto/Typ und Tokenzustand erneut fachlich prüfen: insbesondere dürfen
   verbrauchte/abgelaufene Reset- oder Verifikationstokens nicht durch Recovery wieder
   gültig gemacht werden. Eine neue, fachlich zulässige Nachricht kann nötig sein.
6. Bei genehmigter Wiederfreigabe in einer kontrollierten Datenbanktransaktion zuerst
   die gemeinsame Kontosperre, dann den Queue-Lock erwerben (wie Produzenten),
   Status und Retryhistorie frisch prüfen und andere offene Nachrichten desselben
   Kontos/Typs ausschließen. Nur genau die untersuchte ID von IN_PROGRESS nach
   PENDING ändern, Retryzähler und vorhandene Zeitangaben unverändert lassen;
   nicht Retrylimit oder Fälligkeit umgehen. Erwartet genau eine betroffene Zeile,
   sonst Rollback und erneute Untersuchung. Commit und frischen Zustand prüfen;
   bei Commit-Ambiguität nicht blind erneut ausführen. Erst danach Worker gezielt
   wieder starten und die nächste Verarbeitung beobachten.

Dieses Vorgehen ist ein Vertrag für autorisierte Betreiber, keine hier ausgeführte
Datenänderung und kein neues Recovery-API. Für offene Admin-Audits analog Worker-
beendigung und SMTP-Belege prüfen; kein automatischer oder blinder manueller Retry.

## Schemaänderung und Einführung

Neue Tabelle `admin_notification_attempt` mit Identity-PK `id` (Long),
`queue_id` (Long, NOT NULL, unique Constraint `uk_admin_attempt_queue`),
`started_at` (LocalDateTime, NOT NULL), `completed_at` (LocalDateTime, nullable),
`status` (Enum als String, max. 32, NOT NULL; IN_PROGRESS/SENT/NOT_SENT/UNKNOWN).
`queue_id` ist absichtlich nur ein Korrelationswert, **kein Foreign Key** und keine
JPA-Beziehung; keine Löschkaskade. Bestehende Queue-Spalten werden nicht verändert:
FAILED-Alter nutzt das vorhandene nullable `lastRetryAt`.

Das Projekt ist derzeit auf `spring.jpa.hibernate.ddl-auto=update` eingestellt.
Hibernate kann die neue Tabelle beim autorisierten Start anlegen; dies ist hier
nicht an Bestandsdaten getestet oder ausgeführt worden. Vor Einführung mit Backup
und gestoppten Workern das konkrete Dialekt-DDL einschließlich Timestamppräzision,
String-Enum-Constraints, Identity und Unique-Constraint in einer isolierten Kopie
prüfen und nach dem betrieblichen Schemafreigabeverfahren bereitstellen. Kein neues
Migrationstool oder Bestandsdaten-Backfill: alte FAILED-Einträge ohne lastRetryAt
bleiben erhalten, historische Adminausgänge werden nicht erfunden. Bei Einführung
können bereits ältere FAILED mit bekanntem Zeitstempel beim nächsten Cleanup
gelöscht werden; erforderliche Untersuchungsbelege vorher autorisiert sichern.
Die JPA-Schemaerzeugung, Uniqueness, Persistenz und fehlende Löschkaskade sind nur
mit isoliertem H2 geprüft; keine reale Produktionsmigration zugesichert.
