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

Bei eindeutig vor dem Transport gescheiterten MIME-/Authentifizierungs- oder
Vorbereitungsfehlern gilt weiter: initialer Versuch plus konfigurierte
Wiederholungen. Der Retry-Zähler zählt Wiederholungen, die angezeigte aktuelle
Versuchszahl ist `retryCount + 1`. Der konfigurierte Grenzwert und seine bisherige
Behandlung werden nicht geändert.

Eine `MailSendException` beweist dagegen keine Nichtzustellung: etwa bei verlorenem
SMTP-Antwortpaket kann die Nachricht bereits angenommen sein. Deshalb bleiben
Transportfehler konservativ auf `IN_PROGRESS`, ohne automatischen Retry oder
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
