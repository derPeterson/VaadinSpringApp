# Mailqueue

## Auftrag und Versand

Entwicklungskonten besitzen ein eigenes, bei der Anlage festgelegtes Kennzeichen.
Die Queue unterdrückt ihre Kontomails anhand des frisch gesperrten Kontodatensatzes,
auch bei festen abweichenden Empfängeradressen. Der Löschablauf berücksichtigt das
Kennzeichen noch vor Entfernung des Kontos. Verifikations-, Reset-, Sicherheits-,
Frist- und Löschmails sind gleichermaßen ausgeschlossen. Mailabhängige Nachweise
werden nicht übersprungen. Der direkte administrative SMTP-Mailtest bleibt unabhängig.
„Nicht geprüft“ allein ist keine Versandunterdrückung.

Fachaktionen reihen ihre Mails transaktional ein. Verifikation und Passwortreset
sperren den Kontodatensatz während der Transaktion; offene gleichartige Aufträge werden dedupliziert.
Sicherheitsereignisse mit fester Zieladresse werden vollständig angelegt.
Eine Queuezusage ist keine SMTP-Zustellbestätigung.
Ein erneuter Verifikationsversand meldet nur dann Erfolg, wenn tatsächlich ein neuer
Auftrag angelegt wurde. Ein bereits offener Auftrag bleibt unverändert und führt
zu einer Ablehnung statt einer erneuten Versandzusage. Die eingeschränkte
Bestätigungsseite erklärt die Wartezeit, offene Aufträge und den Kontakt zur
Administration bei anhaltender Blockierung.

Verifikations- und Passwortresetmails halten die Zieladresse bei der Queueaufnahme fest.
Bei einer administrativen Adressänderung werden alte Links ungültig und noch wartende
Adressmails entfernt. Bereits beanspruchte Aufträge behalten ihr ursprüngliches Ziel
und ihren Zustand. Sie werden nicht erneut freigegeben und blockieren keinen Auftrag
für die neue Adresse. Änderungen am Konto und der automatische Verifikationsauftrag
für die neue Adresse werden gemeinsam gespeichert oder gemeinsam zurückgerollt.

Der Scheduler wählt fällige PENDING-IDs nach Erstellungszeit und ID. Pro Lauf gilt
die konfigurierte Kapazität; der Workerpool nimmt höchstens seine konfigurierte
Parallelität an. Jeder Worker lädt und beansprucht seinen Auftrag in einer eigenen
frischen Transaktion. SMTP läuft außerhalb der Datenbanksperre. Überschneidende
Batches derselben Instanz werden nicht zusätzlich gestartet.

## Ergebnisse und Wiederholung

| Zustand / Ergebnis | Verhalten |
| --- | --- |
| SMTP-Annahme und Ergebniscommit erfolgreich | SENT |
| Eindeutig vor Transport gescheitert | Begrenzte Wiederholung; initialer Versuch plus konfigurierte Retries |
| Fälligkeit nach Fehler | Frühestens eine Minute nach lastRetryAt; Gleichheit ist fällig |
| Retrylimit erreicht | FAILED mit Fehlerzeitpunkt |
| Transportausgang oder anschließender Commit unklar | IN_PROGRESS bleibt bestehen; kein automatischer erneuter Versand |

`ConnectionAwareJavaMailSender` markiert den eindeutig vor sendMessage fehlgeschlagenen
Verbindungsaufbau. Ein beliebiger Fehlertext oder eine allgemeine MailSendException
ist kein sicherer Nachweis der Nichtzustellung. SMTP-Annahme und Datenbankcommit sind
nicht atomar; es gibt keine Exactly-once-Garantie.

Bei einem unklaren Claim zunächst den Worker beenden bzw. seinen Stillstand sicherstellen
und anhand Auftrag, Zeit und Mailpit-/SMTP-Ergebnis entscheiden. Nicht blind auf
PENDING zurücksetzen: Das kann eine bereits angenommene Mail erneut versenden.
Eine komfortable Recovery-Oberfläche ist derzeit nicht vorhanden.

## Konfiguration und Bereinigung

- Workerzahl, Auswahlgrenze, Retrylimit und Wartezeit gelten ab dem nächsten
  Durchlauf. Laufende Durchläufe behalten ihre Werte.
- Die Wartezeit beträgt standardmäßig eine Minute. Sie gilt auch für bereits
  wartende Wiederholungen. Auswahlgrenze und Wartezeit müssen positiv sein,
  das Retrylimit muss mindestens null sein.
- Neue SMTP-Verbindungen verwenden den zuletzt vollständig gespeicherten Stand
  der SMTP-Einstellungen. Bereits verbundene Transporte behalten ihre Einstellungen.
- SENT-Aufbewahrung folgt createdAt und einer positiven Dauer; die Löschgrenze ist strikt.
- FAILED-Einträge werden nach mehr als 30 Tagen seit lastRetryAt bereinigt.
- PENDING und IN_PROGRESS werden durch diese Aufbewahrungsjobs nicht gelöscht.
- Admin-Versandbeobachtung unterscheidet SENT, NOT_SENT und UNKNOWN; Wiederholungen
  derselben Beobachtung erzeugen keine doppelten Erfolgsmeldungen.

Die Queue verwendet lokale Serverzeit. Zeitumstellungen können reale Abstände beeinflussen;
Tests prüfen den vereinbarten Zeitvertrag mit kontrollierter Uhr.

## Beenden und Diagnose

Context-Close stoppt neue Annahmen. Der Pool wartet bis zu 30 Sekunden auf angenommene
Aufgaben und fordert danach Abbruch an. Ein nicht unterbrechbarer SMTP-Aufruf kann
nicht zuverlässig zwangsweise beendet werden. Ungeklärte Claims werden nicht freigegeben.
Ein Interrupt des Koordinators stoppt das Nachfüllen und wird nach dem Drain wieder gesetzt.

Der Admin-Mailtest ist ein direkter Einzelversand außerhalb dieser Queue.
Lokal werden synthetische Mails mit [Mailpit](docker/README.md) aufgefangen.
Diagnosemeldungen folgen dem [Logging-Vertrag](logging.md).

## Kontostatus-Vorlagen

Deaktivierung, Löschwarnung, endgültige Löschung und erfolgreiche Reaktivierung
verwenden `account_deactivated`, `account_deletion_reminder`, `account_deleted`
und `account_reactivated` aus `resources/email`, jeweils als MJML und erzeugtes HTML
in DE/EN. `AccountSecurityMail` ersetzt die Werte mit HTML-Escaping und verwendet
die Kontosprache, das gemeinsame Logo, die Basisadresse und den Cosmos-Footer.

Deaktivierung und Erinnerung zeigen den festgelegten Löschtermin sowie denselben
Reaktivierungslink als Button und kopierbare URL. Die übrigen Bestätigungen haben
keinen Aktionslink. Das Öffnen eines Reaktivierungslinks erzeugt noch keine Mail:
Erst die erfolgreiche Bestätigung reiht die Nachricht über die Wiederaktivierung
in derselben Transaktion ein. Ungültige oder erneut verwendete Links tun das nicht.
Entwicklungskonten bleiben bei allen vier Ereignissen ohne Versand.
