# Aktueller Prüfstand

Stand: 22.10.2026. Ausschließlich lokale Entwicklung mit austauschbaren Testdaten.

## Ergebnis der großen Testrunde

Die automatisierten Fälle der großen Runde sind vollständig bestanden. Der aktuelle
HTTPS-Stand hat inzwischen einen zugeordneten Sonar-Scan mit rotem Quality Gate;
die Gesamt-QA ist damit nicht grün. Geprüft wurde `feature/lokales-https`, Commit
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

Nach lokalem Fast-forward-Merge ist `main` auf
`e5b6e681320885f44b5902b1fa9b037eb4d4c690` geprüft: optimierter Build erfolgreich,
2.241 Anwendungstests bestanden, zwei erwartete optionale IDE-Skips. Der frisch
gebaute JAR startete isoliert mit HTTPS-Profil und frischer In-Memory-Datenbank:
Health 200, direkter HTTP-Login 403, Login mit konkreten Proxy-Headern 200.
Eigener Prozess beendet, Port freigegeben, keine SMTP-Verbindung. Diese Mergeprüfung
ersetzt keine vollständige Browser-QA und keine gemeinsame Featureabnahme.

Der anschließend ausdrücklich beauftragte Scan wurde mit diesen unveränderten
Build-/Coverage-Artefakten hochgeladen. Serverauftrag
`4528517b-21da-4410-8dcb-a23fed416dba` endete mit **SUCCESS**;
Analyse `bfb3a47f-be62-4c71-b604-04c39a303df8` vom 22.10.2026, 10:06:37 UTC.
Die Serverrevision stimmt exakt mit dem genannten lokalen `main` überein.

Quality Gate **ERROR** wegen 21 neuen offenen Issues, zulässig 0. Neue Coverage
82,7 % (Mindestwert 80 %) und neue Duplikation 0 % (Höchstwert 3 %) bestehen.
Gesamtcode und New Code enthalten dieselben 21 offenen Issues: eine von Sonar
klassifizierte Vulnerability im IDE-Starter, zwei Bugs in Test-Assertions und
18 Code Smells. Kein Nachweis eines ausnutzbaren Sicherheitsfehlers allein durch
diese Klassifikation. 0 Hotspots; bestehende 26 Accepted und 7 False positives
unverändert. Gesamtcoverage 85,0 %, Zeilencoverage 87,6 %, Branchcoverage 77,1 %.

Offene fachlich eingeordnete Arbeiten: [Sonar-Pakete](planung/sonar-pakete.md).
Keine Codebehebung oder Issue-Statusänderung in diesem Scanauftrag. Der Scanner
meldete Typauflösungs-/Preview-Warnungen; Analyseumfang des Hauptmoduls unverändert.
Hotspots wurden über den geschützten vollständigen REST-Lesehelfer geprüft, da
der MCP-Hotspotaufruf einen nicht unterstützten Filterparameter sendete (HTTP 400).
Die erfolgreiche lokale QA-Baseline wird wegen des roten Gates nicht ersetzt.

Es sind keine bestätigten offenen Anwendungsfehler bekannt; siehe [BUGS.md](../BUGS.md).
Die erfolgreich geprüften bisherigen Ergänzungen sind aus dem
[Testbedarf](testbedarf.md) entfernt; auch der gezielte Nachtlaufbedarf ist nachgewiesen.
Menschliche Sonderprüfungen bleiben im
[manuellen Katalog](manual-tests/README.md). Die [HTTPS-Planung](planung/https.md)
bleibt bis zur gemeinsamen Gesamt-Abnahme erhalten. Der Merge ist erfolgt;
kein Featureabschluss, keine zusätzlichen Commits oder Pushes im Scanauftrag.
Entwicklerdaten und fremde Prozesse wurden nicht als Fixtures verwendet oder verändert.
