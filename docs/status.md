# Große Testrunde vom 09.11.2026

Geprüft wurde der nach `main` übernommene 2FA-Stand. Der vorherige belegte
Browserstand war `df4e1938`. Die Anwendung im neuen Image entspricht
`260a01d3`. Der anschließende reine Testfix `0d17bd3d` korrigierte die erwartete
Anzahl der Konfigurationsfelder von 30 auf 31.

| Prüfung | Ergebnis |
| --- | --- |
| Optimierter Maven-Reactor, JDK 25, Maven-Modell und Coverage-Prüfer | Bestanden |
| Spotless und Checkstyle | Bestanden |
| Anwendungstests | 2.779 bestanden, zwei manuelle HTTPS-Opt-in-Fälle übersprungen |
| Browser-Helfertests und Frontendtests | 96 und 20 bestanden |
| Coverage- und Sonar-Offlineprüfungen | 18 und 47 bestanden |
| Isolierter Start des frischen JARs | Anmeldeseite lieferte HTTP 200 mit flüchtiger H2-Datenbank |
| Browser DE | 261 verschiedene Fälle nachgewiesen. 260 bestanden im Gesamtlauf. Der korrigierte Fall bestand im Konfigurationsnachlauf mit 12 von 12 Fällen. |
| Browser EN | 261 von 261 Fällen im Gesamtlauf bestanden |
| SonarQube auf `0d17bd3d` | Quality Gate grün, null offene Findings, null offene Hotspots |

Die beiden Browser-Gesamtläufe verwendeten vier getrennte Anwendungs-, H2- und
Mailpit-Umgebungen. Der deutsche Nachlauf und beide Gesamtläufe verwendeten
dasselbe Image
`sha256:4a87cb4674e94c5fd15e5f139617e25e1dc4e98b826a66ba924e9fb84fdc1b33`.
Aufbau und Abbau waren erfolgreich. Die Daueranzeige „Sekunden“ und die kompakte
Info-Box „Anmeldung“ mit dem cyanfarbenen Badge für Selbstregistrierung wurden
in DE und EN bei 1.280 und 390 Pixeln geprüft.

Die erste Sonar-Analyse von `24dbcf03` meldete 22 offene Findings. Die
Korrekturen gliederten lange Browserabläufe, vereinfachten die Auswahl des
abgefangenen Vaadin-Requests und präzisierten unter anderem die
Transaktionsgrenze bei der Abfrage von Anmeldewegen. Der letzte Scan
`ed64311d-68cf-4dd1-852b-383935e3b2a6` bestätigt den bereinigten Stand.
Die vorhandenen 52 Sonar-Ausnahmen blieben unverändert.

Die 2FA-Erweiterung ist abgenommen. Die fachliche Dokumentation und der
[manuelle Prüfkatalog](manual-tests/README.md) bleiben für spätere Prüfungen
verfügbar. Der [Testbedarf](testbedarf.md) führt weitere offene automatisierte
Szenarien.
