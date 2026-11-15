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

Unter **Konfiguration → E-Mail-Queue → Assistenten** können ausschließlich
Superadmins eine gemeinsame **Manuelle Bereinigung** bestätigen. Es gibt keine
Auswahl einzelner Empfänger. Abbrechen verändert nichts. Die bestätigende Aktion
heißt **Jetzt bereinigen**; beide Buttons und der Dialogkopf verwenden das Brush-Icon.
Die Warnung **Späterer Empfang möglich:** macht deutlich, dass eine zuvor
angenommene Mail trotzdem eintreffen kann. Die separate Infobox erklärt die
beim Anfordern gespeicherten Einstellungen und den Umfang des späteren Laufs.
Eine zusätzliche Rollenbeschriftung oder sichtbare Gesamtgrenze gibt es nicht.

Der Auftrag wird vor der Übergabe an den Hintergrunddienst dauerhaft gespeichert.
Bei laufendem Queueversand bleibt er unter **Wartet auf Ausführung** vorgemerkt.
Der Scheduler setzt aktive Aufträge auch bei ausgeschalteter Automatik fort;
ein weiterer Klick ist nicht nötig. Gleichzeitige Anforderungen schließen sich
dem bestehenden aktiven Lauf an und erzeugen keinen zweiten. Reine automatische
Prüfungen bei belegtem Versand erzeugen weder einen übersprungenen Historieneintrag
noch überschreiben sie einen vorgemerkten manuellen Auftrag.

### Umfang und Pakettransaktionen

Bei der Anforderung wird das gespeicherte Mindestalter übernommen. Beim tatsächlichen
Start werden Laufbeginn, Altersgrenze und höchste passende Auftrags-ID einmalig
festgelegt. Maßgeblich ist `processingStartedAt` des letzten Versandversuchs,
nicht das Erstellungsdatum. Eine Schleife verarbeitet dieses feste Fenster in
internen Paketen von höchstens 100 IDs, jeweils in einer eigenen kurzen Transaktion.
Neu hinzugekommene oder erst später ausreichend alte Claims gehören zum nächsten
Lauf. Nach dem erneuten Sperren nicht mehr passende oder inzwischen entfernte
Kandidaten werden nicht gezählt; der Cursor schreitet trotzdem voran. Ein nicht
fortschreitendes Abfragefenster beendet den Lauf mit einem Fehler statt einer Endlosschleife.

Die Zahl 100 begrenzt nur die einzelnen Datenbanktransaktionen. Ein logischer Lauf
kann mehr Aufträge bearbeiten; Gesamtzahl und Historieneintrag werden über alle
bestätigten Pakete fortgeschrieben. Es gibt keine Rekursion und keinen Neuversand.
Das Ergebnis eines bereinigten Claims ist `UNKNOWN`, ohne Zustellbehauptung.
Die Datenschutz-Sonderregel für Löschbestätigungen bleibt bestehen: Diese
Queueeinträge werden entfernt. Danach kann ein Benutzer ausdrücklich einen neuen
Link anfordern; die normalen Wartezeiten bleiben gültig. Claims niemals blind
auf `PENDING` zurücksetzen.

Die gemeinsame lokale Dateisperre je H2-Datenbank bleibt während des gesamten
logischen Laufs exklusiv, einschließlich aller Paketcommits. Sie gilt auch für
weitere lokale JVMs mit demselben Datenbankpfad. Neue Queueworker warten mit ihrem
Claim; SMTP hält weiterhin keine Datenbanktransaktion offen. Ein unterbrochener,
aber noch nicht tatsächlich beendeter SMTP-Worker bleibt geschützt.

Ein eigener begrenzter Ein-Thread-Executor führt die Bereinigung unabhängig vom
SMTP-Pool und vom Scheduler aus; Aufruferkontexte werden nicht vererbt. Der
manuelle Auftrag speichert während aktiver Arbeit ausschließlich die Kennung
des frisch geprüften Superadmins. Vor jedem Paket liest eine unabhängige Projektion
seine aktuelle Verfügbarkeit und explizite Rolle. Sperre, Selbstdeaktivierung,
ausstehende Verifikation, Rechteentzug oder Löschung verhindern weitere Pakete.
Die kurzlebige Auftraggeberreferenz wird bei einem terminalen Ergebnis entfernt;
bei Kontolöschung setzt der Fremdschlüssel sie sofort auf null.

### Ergebnis und Statusanzeige

| Zustand | Bedeutung |
| --- | --- |
| Noch nicht ausgeführt | Noch kein Lauf gespeichert; keine zusätzliche Erfolgs-/Fehlerzeile. |
| Wartet auf Ausführung | Dauerhaft vorgemerkt; automatische Übernahme, sobald kein Queueversand läuft. |
| Wird ausgeführt | Hintergrundarbeit aktiv; die Anzahl enthält nur bereits bestätigte Änderungen. |
| Abgeschlossen | Das feste Auftragsfenster ist vollständig bearbeitet, auch bei 0 Treffern. |
| Fehlgeschlagen | Kein Paketfortschritt bestätigt; keine erfolgreiche Bereinigung behaupten. |
| Teilweise abgeschlossen | Frühere Paketcommits bleiben erhalten; ein späteres Paket ist gescheitert. |

Native Badges kennzeichnen Warten und Teilfortschritt gelb, Abschluss grün und
Fehler rot. Die Statusüberschrift bleibt blau, Zeitpunkt und Gesamtzahl stehen
immer auf getrennten Monospace-Zeilen. Bei Abschluss steht zusätzlich
**Die Bereinigung konnte erfolgreich abgeschlossen werden.**; Fehler und
Teilergebnis erhalten die entsprechende Erklärung. Eine Unterbrechung zwischen
Paketcommits lässt den Auftrag aktiv und fortsetzbar. Ein technischer Fehler
rollt ausschließlich das fehlerhafte Paket zurück. Ein ausdrücklich neuer Lauf
bearbeitet danach die verbleibenden passenden Claims.

Die Ansicht lädt den letzten Stand beim Öffnen und über **Aktualisieren** mit
Erfolgs-/Fehlermeldung. Während aktiver Arbeit pollt sie vorübergehend; beim
Abschluss oder Verlassen entfernt sie ihren Listener und stellt das vorherige
Pollintervall wieder her. Der manuell beobachtete Lauf wird anhand seiner ID
verfolgt, damit eine inzwischen neuere automatische Bereinigung seine Rückmeldung
nicht verdrängt. Vormerkung ist kein Erfolg. Erst das gespeicherte Endergebnis
erzeugt die passende Erfolgs-, Fehler- oder Warnnotification; auch 0 Treffer sind
erfolgreich. Andere automatische Läufe erzeugen keine allgemeinen UI-Notifications.

### Automatik, Konfiguration und Historie

Die automatische Bereinigung ist standardmäßig ausgeschaltet. Nur Superadmins
können sie einschalten oder Prüfintervall (Vorgabe 15 Minuten) und Mindestalter
(Vorgabe 30 Minuten) ändern. Beide Dauern müssen mindestens eine Minute betragen.
Der Scheduler prüft alle zehn Sekunden; das Intervall zählt seit dem Abschluss
des letzten manuellen oder automatischen Laufs. Die manuelle Anforderung bleibt
unabhängig vom Intervall und Automatikschalter möglich. Ein vorgemerkter Auftrag
verwendet seinen gespeicherten Einstellungsstand; der nächste übernimmt Änderungen.

Die Reiter **Allgemein** und **Assistenten** verwenden den vorhandenen Panelstil.
Direktlinks sind `admin?section=configuration&group=queue&tab=general` und
`admin?section=configuration&group=queue&tab=assistants`. Der Wechsel aktualisiert
die URL ohne erneutes Laden der Formularwerte; Neuladen stellt die Auswahl wieder
her. Fehlende, unbekannte oder mehrfache `tab`-Werte wählen **Allgemein**.
Admins sehen fünf allgemeine Einstellungen, Superadmins zusätzlich den Assistenten.
Ein Direktlink erteilt keine zusätzlichen Rechte. Die beiden Zeitfelder sind
nur bei eingeschalteter Automatik bedienbar, gespeicherte Werte bleiben erhalten.
Die Historienfrist bleibt unabhängig davon bedienbar. Der gemeinsame äußere Footer
lädt beziehungsweise speichert beide Reiter.

`email_queue_cleanup_run` enthält einen Eintrag je logischem Gesamtlauf mit
Anforderung, tatsächlichem Start/Ende, Auslöser, Status, Gesamtzahl und
Fortsetzungscursor. Auch ein erfolgreicher Lauf ohne Treffer wird gespeichert.
`email_queue_cleanup_state` verweist auf den letzten beziehungsweise aktiven Lauf
und hält den Abschlusszeitpunkt für das Prüfintervall. Empfänger und Mailinhalte
werden nicht in die Bereinigungshistorie kopiert.

**Aufbewahrung der Bereinigungshistorie** ist Superadmins vorbehalten und verlangt
mindestens einen ganzen Tag, Vorgabe 30 Tage. Täglich um 03:15 Uhr Serverzeit werden
ältere terminale Einträge anhand ihres Endzeitpunkts entfernt, unabhängig vom
Automatikschalter. Der aktuelle Anzeigeeintrag und aktive Aufträge bleiben erhalten.
Änderungen gelten ohne Neustart für den nächsten Löschlauf. Eine sichtbare
Verlaufsliste bleibt eine spätere offene Aufgabe.

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
