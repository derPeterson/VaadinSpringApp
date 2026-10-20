# Sonar-Nachscan: sieben begrenzte Codekorrekturen

Stand: 20.10.2026, Umsetzung ausdrücklich auf `main`, Ausgangspunkt `d527ab3`.
Keine Commits, Pushes, Sonar-Statusänderungen oder Lockerung des Quality Gates.

## Umfang und Ergebnis

Der Nachscan enthält 409 offene eindeutige Findings: 398 bekannte, 13 gegenüber
dem Ursprungsexport entfallene und elf neue. Für dieses Paket wurden sieben
kleine Codehinweise zur Umsetzung freigegeben. Die vier übrigen Kontextmeldungen
bleiben zur gesonderten Entscheidung offen. Keine pauschale Serialisierungs-,
Transaktions- oder Architekturänderung.

| Finding-ID / Regel | Umsetzung |
| --- | --- |
| `495340e1-a106-4f3a-bdf2-dd79b341244f` / S2259 | `H2ConsoleOriginFilter` liest das Servlet einmal und verweigert bei null eine Konsolenausnahme. Die normale Anwendungssicherheitskette läuft weiter. |
| `8c136d19-0ac4-4ee4-9e03-ebd0ef7dcc63` / S1192 | Gemeinsame lokale `NO_STORE`-Konstante statt dreier Literale. |
| `bc8a4e96-e14e-4b89-89bd-21c8aa4c2fea` / S1192 | Vorhandene Spring-Konstante `HttpHeaders.CACHE_CONTROL` wiederverwendet. |
| `dd16e82f-b4f3-4874-ae2b-241c9c860ed3` / S3776 | Zusammengehörige Prüfung auf doppelte Herkunftsheader in `hasRepeatedSourceHeaders()` gegliedert. Reihenfolge und Ablehnungsverhalten bleiben gleich. Die endgültige Regelbewertung erfolgt erst beim nächsten Sonar-Scan. |
| `634435e1-8ebf-45df-aa5e-ad5b50608523` / S7467 | Unbenutzte Catch-Variable der URI-Ablehnung durch die unbenannte Variable `_` ersetzt. |
| `23fdf55d-db29-47de-8564-e1bedf536877` / S7467 | Unbenutzte Catch-Variable der ungültigen Aktivitätszeit durch `_` ersetzt. |
| `aa2dfaa8-c394-4df7-9ee2-28367537122f` / S1612 | `FrameOptionsConfig::sameOrigin` statt der gleichwertigen Lambda verwendet. |

Die Produktionsänderungen betreffen nur `H2ConsoleOriginFilter`, `SecurityConfig`
und `AdminUserActivityFilter`; eine neue Regression ergänzt
`H2ConsoleOriginFilterTest`. Keine eigenen Änderungen an Oberfläche, Übersetzungen,
Konfigurationen oder Abhängigkeiten.

## Nachgewiesener Nullfall und Sicherheitsvertrag

Der neue Test verwendet eine eigene, noch nicht mit einem Servlet vervollständigte
`ServletRegistrationBean`. Gegen den unveränderten Filter trat vor `getClass()`
die gemeldete `NullPointerException` auf: ein ausgeführter Test, ein erwarteter
Error im Vorher-Nachweis. Das belegt den Randfall, nicht einen beobachteten Fehler
beim normalen Boot-H2-Start.

Nach der Korrektur gewährt diese Registration keine H2-/CSRF-Ausnahme und wird ohne
Nullzugriff an die übrige Sicherheitskette weitergereicht. Die vorhandenen Tests
prüfen weiter tatsächliche Servletzuordnung, Freigabe, Host-/Port-/Schemaabgleich,
fremde oder widersprüchliche Quellen, doppelte Header, GET-/POST-Mutationen sowie
fehlende und falsche Spring-/Flow-Tokens. Cache-Control bleibt auch bei nativen
H2-Headern `no-store`; Frame-Options bleibt `SAMEORIGIN`.

## Prüfungen

| Tatsächlich ausgeführte Prüfung | Ergebnis |
| --- | --- |
| Neue Null-Servlet-Regression vor der Produktivkorrektur | 1 Test, 1 erwarteter Error durch den nachgewiesenen Nullzugriff |
| Gezielter H2-/CSRF-/Flow-/Komponentenlauf nach Korrektur | **174/174 erfolgreich**, keine Failures/Errors/Skips |
| Vollständiger Maven-Lauf mit Produktionsprofil und `verify` | **1978/1978 erfolgreich**, keine Failures/Errors/Skips; ausführbares Produktions-JAR gebaut |
| Browser-Helfertests ohne Browser-/Anwendungsstart | **48/48 erfolgreich**, keine Failures/Errors/Skips |
| Spotless apply/check und Checkstyle nach letzter Java-Änderung | Erfolgreich, keine Stilabweichungen |
| Abschließende Diff-/Whitespaceprüfung | Erfolgreich |

Eine zusätzliche Testausprägung gegenüber dem Ausgangsstand mit 1977 Tests.
Die 174 gezielten Fälle sind Teil der vollständigen Runde und werden nicht
zusätzlich als eindeutige Tests gezählt. JDK 25.0.4.1, Maven Wrapper 3.9.16.

Frische JaCoCo-Messung ausschließlich aus dem vollständigen aktuellen Testlauf:
**10207/11636 Zeilen = 87,72 %**, **2963/3854 Zweige = 76,88 %**,
219 analysierte Anwendungsklassen. Die Quote ersetzt weder einen neuen Sonar-Scan
noch eine Browser-/Betriebsabnahme. Die bestehende vollständige UI-Serialisierung
bleibt außerhalb dieses Pakets.

## Vier weiterhin offene Kontextentscheidungen

| IDs / Regel | Sachstand und Grenze |
| --- | --- |
| `685055b4-02cc-4dd8-8181-6609338fcfd9`, `6aebf5ce-9e84-45c8-9c71-f7696ba79a7c` / S1948 | Die beiden Clock-Felder verwenden im Standardbetrieb die laut JDK serialisierbare `Clock.systemUTC()`. Der abstrakte Typ garantiert dies nicht für beliebige Sonderimplementierungen. Weder blindes `transient` noch eine pauschale Wiederherstellung angemeldeter Vaadin-Sitzungen eingeführt. |
| `67c5a51d-9895-41e9-8211-2ae3519813dd` / S1872 | Exakte native H2-Klasse bleibt beabsichtigt. H2 ist Runtime-Abhängigkeit; ein direktes `instanceof` würde außerdem Unterklassen einschließen. Keine Änderung dieser Typ-/Autorisierungsgrenze allein für eine Regel. |
| `aa8853f3-b91a-4fdd-bd8f-fcc970fe10cb` / S4502 | Spring-CSRF ist aktiv. Echte Flow-Transporte und die eng begrenzte lokale H2-Herkunftsprüfung besitzen den dokumentierten eigenen Schutz. Die Meldung bleibt ein Review der Ausnahme, keine erneut eingeführte globale Abschaltung. |

Keiner dieser vier Befunde wurde unterdrückt oder im Sonar-Server akzeptiert,
als False Positive markiert oder geschlossen. Ihre Entscheidung ist gesondert
freizugeben. Auch die sieben Codekorrekturen sind noch keine Zusage eines grünen
Quality Gates: Ein neuer Scan muss den aktuellen Quellstand erneut bewerten.

Grundlagen und bestehende technische Nachweise:
[H2-Schutz](h2-console-csrf-2026-10-20.md),
[CSRF-Umsetzung](sonar-csrf-umsetzung-2026-10-20.md),
[Java-25-Clock-Vertrag](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/time/Clock.html#systemUTC()).

## Separat aufgetretene Lockdateiänderung

Zu Aufgabenbeginn war `main` sauber. Während der Arbeit wurde zusätzlich
`package-lock.json` verändert, unter anderem Rollup 4.64.7 auf 4.64.10. Diese
Frontend-Abhängigkeitsänderung wurde nicht als Teil der sieben Korrekturen
beauftragt oder manuell von ihnen vorgenommen. Ihre Herkunft wurde nicht sicher
zugeordnet; sie bleibt erhalten und gehört nicht zum freigegebenen Codepaket.
Der vollständige Build prüft den tatsächlich vorhandenen lokalen Arbeitsstand.

## Einführung und Grenzen

Das aktuelle Produktions-JAR bauen und die Anwendung vollständig neu starten;
ein Browser-Refresh übernimmt Java-Änderungen nicht. Kein bestehender App-Prozess,
Container oder Datensatz wurde verändert. Keine neue Browser-UI-Abnahme oder
Dockerimage für dieses Paket. Die serverseitigen Komponenten-JUnits prüfen
auch die DE-/EN-Aktivitätsfilter; sie sind keine neue Browserabnahme.
