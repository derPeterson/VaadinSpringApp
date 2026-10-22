# Aktueller Prüfstand

Stand: 22.10.2026. Ausschließlich lokale Entwicklung mit austauschbaren Testdaten.

## Ergebnis der großen Testrunde

Die automatisierten Fälle der großen Runde sind vollständig bestanden. Der aktuelle
HTTPS-Stand hat nach lokaler Integration und Sonar-Bereinigung einen zugeordneten
Scan mit grünem Quality Gate. Die vollständige Browserrunde wurde danach nicht
erneut ausgeführt; ihre Grenzen und die gezielten Nachweise stehen unten.
Geprüft wurde in der großen Runde `feature/lokales-https`, Commit
`fe1f47ee94e27d07ee90a3d6dac8a8daa4f611a4` einschließlich der uncommittierten
Testergänzungen. Eine revisiongebundene erfolgreiche lokale QA-Baseline lag nicht
vor; bisheriger Prüfbericht, tatsächliche Änderungen und offener Testbedarf wurden
abgeglichen. Erfolgreiche unveränderte Prüfphasen wurden bei der Fortsetzung erhalten.

| Prüfung | Tatsächliches Ergebnis |
| --- | --- |
| Java 25, Spotless und Checkstyle | Bestanden, keine Stilabweichungen |
| Shared | `clean install` erfolgreich; keine eigenen Testquellen |
| Optimierter Produktionsbuild / Anwendungs-JUnit | 2.243 erfasst, 2.241 bestanden, 0 Fehler, 2 erwartete optionale Skips |
| Browser-Helfer | 63 bestanden, 0 Fehler oder Skips |
| Frontend-JavaScript | 12 bestanden |
| Gepflegte Sonar-Python-Helfer | 36 bestanden, einschließlich künstlicher DPAPI-Testwerte |
| Browser all DE | 161/161 bestanden, 0 Testfehler oder Skips; Rückbau-Timeout eines Workers durch Elternabbau erfolgreich bereinigt |
| Browser all EN | 161/161 bestanden, 0 Fehler oder Skips; Aufbau und Abbau bestanden, Exitcode 0 |
| Eigene Testressourcen | Alle eigenen Test-CAs samt Zwischenzertifikaten und Besitzmarkern sowie Container, Volumes und Netze entfernt |

Die DE-Fälle sind sämtlich bestanden. Der Gesamtrunner meldete dennoch Exitcode 1,
weil Worker 1 beim Entfernen seiner CA nach 120 Sekunden noch auf Windows wartete.
Nach der Benutzerbestätigung hat der Elternrunner dessen Abbau erfolgreich
nachgeholt. Der spätere gezielte Ressourcencheck bestätigt den vollständigen
Rückbau. Dieser Fehlerstatus wird nicht als grüner DE-Gesamtlauf ausgegeben;
er ist kein fehlgeschlagener Browserfall. EN hat einschließlich Abbau bestanden.

Beide Sprachen verwendeten dasselbe unveränderte Produktionsimage:
`sha256:000b9da0a570ae540c651010181ce96c848f9715dd17cb4ff1017028a3ada99f`.
Die temporären Laufbelege stehen unter
`browser-tests/runs/2026-10-22T07-44-38_UTC__fe1f47ee__44d6ad37/` (DE) und
`browser-tests/runs/2026-10-22T08-21-38_UTC__fe1f47ee__2c3614b2/` (EN).
Keine zusätzliche Coverageauswertung und kein Berichtarchiv.

## Ergänzungen und geprüfte Verträge

- Importfehler, verweigerte Rechte, Abbruch, Teilimport und Timeout sind durch
  Helferregressionen abgesichert. Der originale Trust-Ablauf wird dabei mit
  künstlichen Zertifikatsspeichern geprüft; fremde Einträge bleiben erhalten.
- Der Elternabbau wartet nach hartem Workerabbruch auf die eigenen Kindprozesse,
  bevor Trust entfernt wird. Ein wartender Import wird beendet. Drei gleichzeitig
  reservierte Loopbackports je Worker sind separat geprüft.
- Neue Betriebsfälle belegen Persistenz von Konto, Rollen und Konfiguration ohne
  erneuten Bootstrap, den SMTP-Zielwechsel erst nach Neustart sowie echte
  SMTP-Verbindungsfehler mit Wiederanlauf oder terminalem Ende. Ein weiterer
  Retryzeitraum belegt keine doppelte bzw. terminal erneuerte Zustellung.
- Sprachwechsel erhält ungültige Konfigurationsentwürfe und übersetzt sichtbare
  Feldfehler. Offline-Speichern meldet keinen falschen Erfolg; nach Wiederverbindung
  kann bewusst weitergearbeitet werden. Neue anonyme Reset-Mails folgen der
  auslösenden Sitzungssprache; bereits empfangene Mails bleiben unverändert.
- HTTPS ist ohne TLS-Ausnahmen im normalen und persistenten Chromium geprüft:
  HTTP-Weiterleitung und POST-Grenze, Login/Logout, Mail-Origins, sichere Cookies,
  UIDL, Upload, Push, Widerruf sowie Browser- und Serverneustarts sind abgedeckt.
  Die ergänzten Browserfälle und die wiederverwendeten Journeys sind DE/EN bestanden.

Die beiden optionalen echten IDE-Starterfälle benötigen `https.test.caddy` und
freie feste IDE-Ports. Sie wurden in dieser Runde nicht aktiviert. Ihre bereits
bestandenen isolierten Nachweise und die menschliche IDE-Abnahme bleiben gültig:
Start/Stop, harter Reset, Debug/Breakpoint und HotSwap sind abgenommen.
Echter Mail-Sichtversand, externe Provider und der eingefrorene OpenCode-Workflow
gehören nicht zu dieser Runde. Mailpit belegt SMTP-Annahme im eigenen Testnetz,
keine externe Postfachzustellung.

## Gezielter Nachtest des unbeaufsichtigten HTTPS-Runners

Der danach überarbeitete Runner verwendet eine einmalig bewusst eingerichtete
separate Browser-Test-CA. Normale Läufe lesen Windows-Vertrauen ausschließlich;
Worker erhalten eigene Tageszertifikate und private Schlüssel. Der CA-Schlüssel
bleibt mit benutzerexklusiven Dateirechten auf dem Host, außerhalb von Git und
Containern. Fehlende, abgelaufene oder ungültige Einrichtung scheitert vor Appstart.

| Gezielte Prüfung am überarbeiteten Runner | Tatsächliches Ergebnis |
| --- | --- |
| Java-Formatierung / Checkstyle | Bestanden, keine Verstöße |
| Browser-Helfer | 75 bestanden, 0 Fehler oder Skips; einschließlich paralleler Ausstellung, Gültigkeits-/Trustfehler und Schlüsselabbau nach hartem Workerabbruch |
| `https-night` DE und EN | Je 8/8 bestanden, je zwei getrennte Worker; Aufbau, Tests und Abbau bestanden, beide Gesamtläufe Exitcode 0 |
| Echter Chromium-TLS-Negativtest | 1 bestanden: gültige eigene CA akzeptiert, falscher Hostname und unvertraute CA abgelehnt |
| Harter Abbruch beider eigener bereiter Worker | Erwarteter Gesamtexit 1; Elternabbau bestanden, keine privaten Worker-Schlüssel oder Compose-Ressourcen verblieben |
| Windows-Vertrauen / dauerhafte CA-Dateien | `CurrentUser/Root` und `CurrentUser/CA` vor/nach allen gezielten Läufen identisch; sämtliche CA-Dateihashes unverändert |
| Bewusste Einrichtung erneut gestartet | Bereits konfigurierte CA wiederverwendet; kein neuer Import |
| Gemeinsames Caddyfile im manuellen Standardmodus | Isolierte Syntaxprüfung mit `tls internal` bestanden |

Die gezielten Sprachläufe verwenden dasselbe oben genannte unveränderte Image:
`browser-tests/runs/2026-10-22T09-36-06_UTC__4e981458__30cb3efb/` (DE) und
`browser-tests/runs/2026-10-22T09-42-42_UTC__4e981458__f0f4b1bd/` (EN).
Normale und persistente Profile einschließlich echtem Chromium- und Serverneustart
sind enthalten. Bei diesen Läufen und beim Abbruch waren keine weiteren
Windows-Zertifikatsbestätigungen nötig. Die dauerhafte Test-CA bleibt absichtlich
eingerichtet; ihr ausdrücklicher Rückbau ist ein eigener Bedienvorgang.

Dies ist ein gezielter Nachtest, keine erneut ausgeführte große QA: Die vollständigen
161 DE-/EN-Fälle oben stammen vom vorherigen interaktiven Runner. Anwendungsquellen
und Produktionsimage wurden durch den Nachtlaufumbau nicht verändert.

## Sonar und verbleibender Abschluss

Beide Sonar-Pakete sind auf eigenen Branches umgesetzt und lokal nach `main`
integriert. Der IDE-Starter schützt seine kurzlebigen Argumentdateien bereits bei
Erstellung und erhält Fehlerursachen sowie das READY/PID-Protokoll. Die Tests
trennen Fixturevorbereitung von Fehlerassertions und prüfen gezielt auf
`IOException`. Geplante HTTPS-Proben ersetzen die Schlafschleife; künstliche
Caddy-Prozesse warten begrenzt auf den Stop durch ihren Besitzer.

Paket 2 ist vollständig durch Code behoben: alle sieben Finding-IDs sind im
Servernachscan als Fixed bestätigt. Keine neuen oder wiedereröffneten Issues,
keine zusätzlichen Accepted-/False-positive-Entscheidungen. Die bestehenden
28 Accepted und 9 False positives bleiben unverändert. Die vier Entscheidungen
von Paket 1 sind direkt an ihren Findings begründet und nachgelesen.
Keine Regel-, Profil-, Gate- oder Scopeänderung und keine manuelle Fixed-Markierung.

Formatierung und Checkstyle sind bestanden. Alle 21 gezielten JUnit-Fälle der
beiden betroffenen Klassen sind erfolgreich, einschließlich echtem Caddy- und
vollständigem Anwendungsstart mit eigener temporärer CA, voller HTTPS-Prüfung,
frischer In-Memory-Datenbank und Prozessabbau ohne Windows-Trustimport.
Auf dem integrierten Codestand sind Shared-Installation, optimierter Build und
vollständige Anwendungstests erfolgreich: 2.244 erfasst, 2.242 bestanden,
0 Fehler, zwei erwartete optionale IDE-Skips. Diese beiden Fälle sind im gezielten
Lauf ausdrücklich aktiviert und bestanden. Keine zusätzliche Browserrunde.

Nachscan des Code-Commits `5af925df1acba30e15337ba020b0f47b82676a4a`:
Analyse `bb70315d-1ba4-4ac7-aeec-1ee17f9b6560` vom 22.10.2026, 10:54:15 UTC;
Serverauftrag `1bca6867-b8b6-454e-a915-778523a66999` **SUCCESS**.
Die analysierte Revision stimmt mit diesem geprüften Codestand überein.
Reine Dokumentationsabschlusscommits verändern diese Code-/Testnachweise nicht;
die jeweils zuletzt hochgeladene Analyse steht im lokalen Sonar-Bericht.

Quality Gate **OK**: keine offenen Issues im Gesamtcode oder New Code, keine
offenen Vulnerabilities oder Hotspots. Neue Coverage 82,4 % (Mindestwert 80 %),
neue Duplikation 0 % (Höchstwert 3 %) und neue Issues 0 (zulässig 0) bestehen.
Gesamtcoverage 84,9 %, Zeilencoverage 87,5 %, Branchcoverage 77,1 %.

Der Scanner meldet weiterhin Typauflösungs-/Preview-Warnungen bei Java 25 ohne
aktivierte Preview; dies bleibt eine Analysegrenze und ist kein belegter
Compilerfehler. Hauptmodulumfang unverändert, separater Browser-Testmodulcode
nicht zusätzlich analysiert. Hotspots sind über den geschützten vollständigen
REST-Helfer geprüft. Prozessbezogenes UTF-8 ist für den Scannerhelfer erforderlich;
keine System- oder Projektkonfiguration wurde dafür geändert.

Es sind keine bestätigten offenen Anwendungsfehler bekannt; siehe [BUGS.md](../BUGS.md).
Offene automatisierte Testarbeit bleibt im [Testbedarf](testbedarf.md), menschliche
Sonderprüfungen im [manuellen Katalog](manual-tests/README.md). Die
[HTTPS-Planung](planung/https.md) bleibt bis zur gemeinsamen Gesamt-Abnahme erhalten.
Lokale Paketcommits und Integration sind erfolgt, kein Push und kein Featureabschluss.
Entwicklerdaten und fremde Prozesse wurden nicht als Fixtures verwendet oder verändert.
