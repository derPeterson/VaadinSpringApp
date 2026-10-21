# Mailqueue

## Auftrag und Versand

Fachaktionen reihen ihre Mails transaktional ein. Verifikation und Passwortreset
sperren den Kontodatensatz während der Transaktion; offene gleichartige Aufträge werden dedupliziert.
Sicherheitsereignisse mit fester Zieladresse werden vollständig angelegt.
Eine Queuezusage ist keine SMTP-Zustellbestätigung.

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

- Workerzahl wird beim Start gebunden; Kapazität und Retrylimit pro Lauf gelesen.
- Kapazität muss positiv, Retrylimit nichtnegativ sein.
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
