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
| Explizite SMTP-Ablehnung für den einzelnen Empfänger | 4xx begrenzt wiederholen, 5xx sofort FAILED |
| Fälligkeit nach Fehler | Frühestens eine Minute nach lastRetryAt; Gleichheit ist fällig |
| Retrylimit erreicht | FAILED mit Fehlerzeitpunkt |
| Transportausgang oder anschließender Commit unklar | IN_PROGRESS bleibt bestehen; kein automatischer erneuter Versand |
| Manuell oder automatisch bereinigt, Versand nicht mehr aktiv | UNKNOWN; keine Zustellbehauptung und kein erneuter Versand |

`ConnectionAwareJavaMailSender` markiert den eindeutig vor sendMessage fehlgeschlagenen
Verbindungsaufbau. Ein beliebiger Fehlertext oder eine allgemeine MailSendException
ist kein sicherer Nachweis der Nichtzustellung. Explizite negative Antworten des
verwendeten Angus-Transports werden nur für die einzelne Nachricht mit genau einem
Empfänger und ohne Teilversand eingeordnet. Fehlende Antworten bleiben unklar.
SMTP-Annahme und Datenbankcommit sind nicht atomar; es gibt keine Exactly-once-Garantie.

Unter **Konfiguration → Mailqueue → Ungeklärte Mailaufträge** können ausschließlich
Superadmins eine gemeinsame Bereinigung ausdrücklich bestätigen. Es gibt keine
Auswahl einzelner Empfänger. Abbrechen verändert nichts. Pro Lauf werden höchstens
100 ausreichend alte Claims beendet: maßgeblich ist `processingStartedAt` des
letzten Versandversuchs, nicht das Erstellungsdatum. Das Ergebnis ist `UNKNOWN`,
ohne Zustellbehauptung und ohne erneuten Versand. Danach ist eine ausdrücklich
neue Anforderung möglich; die normalen Versandwartezeiten gelten weiter. Eine
zuvor angenommene Mail kann weiterhin eintreffen. Deshalb keinen Auftrag blind
auf `PENDING` zurücksetzen.

Die automatische Bereinigung ist standardmäßig ausgeschaltet. Nur Superadmins
können sie einschalten oder Prüfintervall (Vorgabe 15 Minuten) und Mindestalter
(Vorgabe 30 Minuten) ändern. Beide Dauern müssen mindestens eine Minute betragen.
Der Scheduler prüft alle zehn Sekunden die gespeicherten Einstellungen; das
Intervall zählt seit dem letzten manuellen oder automatischen Versuch, auch wenn
laufendes SMTP ihn verhindert hat. Manuell lässt sich unabhängig vom Intervall
prüfen. Ein Lauf arbeitet mit seinem gelesenen Einstellungsstand, der nächste
berücksichtigt Änderungen ohne Neustart auch bei vorhandenen Claims.

Die Queue-Konfiguration hat die Reiter **Allgemein** und **Assistenten** im
vorhandenen Panelstil. Admins sehen die fünf allgemeinen Einstellungen,
Superadmins zusätzlich den Bereinigungsassistenten. Seine beiden Zeitfelder
sind nur bei eingeschalteter Automatik bedienbar, gespeicherte Werte bleiben
erhalten. Die Historienfrist bleibt unabhängig davon bedienbar. Der gemeinsame
äußere Footer lädt beziehungsweise speichert beide Reiter. Die manuelle
Bereinigung ist weiterhin unabhängig vom Automatikschalter möglich und verwendet
gespeicherte Einstellungen. Bewusstes Aktualisieren meldet Erfolg oder Fehler.

Letzter Versuch, Anzahl und eine mögliche Blockierung durch laufenden Versand
werden gemeinsam mit dem Abschluss gespeichert und bleiben nach Neustart erhalten.
Die Anzeige lädt den Stand beim Öffnen und über „Aktualisieren“, ohne laufendes
Polling. Zusätzlich wird jeder tatsächlich fällige automatische oder manuelle
Versuch mit Zeitpunkt, Auslöser, Anzahl und Blockierung als eigener Datensatz in
`email_queue_cleanup_run` erfasst, auch ein Lauf ohne Treffer. Reine Scheduler-
Prüfungen ohne fälligen Versuch erzeugen keinen Eintrag. Status und Historie
werden mit demselben Transaktionsabschluss gespeichert. Es wird keine Empfänger-
oder Mailinhaltshistorie angelegt. Die neue Einstellung **Aufbewahrung der
Bereinigungshistorie** ist Superadmins vorbehalten und verlangt mindestens einen
ganzen Tag, Vorgabe 30 Tage. Täglich um 03:15 Uhr Serverzeit werden ältere Einträge
entfernt, unabhängig vom Automatikschalter. Änderungen gelten ohne Neustart beim
nächsten Löschlauf auch für vorhandene Einträge. Letzter Status und Intervall bleiben
davon unberührt. Eine sichtbare Verlaufsliste ist eine spätere offene Aufgabe.
Weitere
passende Aufträge bleiben nach der begrenzten Menge für den nächsten Lauf erhalten.

Der Dienst prüft für manuelle Aktionen die Superadminrolle frisch und sperrt die
betroffenen Datensätze. Eine gemeinsame lokale Dateisperre je H2-Datenbank verhindert
die Bereinigung, solange noch irgendein Queueversand läuft. Sie gilt auch für weitere
lokale JVMs mit demselben Datenbankpfad. Bei laufender Bereinigung warten neue Worker
mit ihrem Claim bis zum Ende des Datenbankabschlusses. SMTP hält keine
Datenbanktransaktion offen. Ein Abbruchsignal an einen nicht unterbrechbaren Worker
gilt nicht als tatsächliches Versandende. Eine echte Prozessunterbrechung oder eine
fehlende finale SMTP-Antwort kann weiterhin einen ungeklärten Ausgang verursachen.

Die Sperrdatei liegt neben der H2-Datei (`*.email-dispatch.lock`); sie nicht bei
laufenden Instanzen löschen. Das Betriebssystem gibt die Sperre bei Prozessende frei.
Die Koordination ist auf den gemeinsamen lokalen H2-Dateipfad begrenzt, kein
verteiltes Mehrserverprotokoll. Fehler beim Sperren verhindern die Auflösung.
Das aktuelle Schema enthält `UNKNOWN`, den Claim-Zeitstempel und den Bereinigungsstand.
Für einen älteren lokalen Entwicklungsstand ist eine frische Datenbank erforderlich.

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
- Bewusst beendete UNKNOWN-Einträge werden nach mehr als 30 Tagen seit createdAt
  bereinigt. Bei der Bestätigungsmail eines bereits gelöschten Kontos werden die
  verbliebenen Queueinhalte direkt mit dem bewussten Abschluss entfernt.
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
