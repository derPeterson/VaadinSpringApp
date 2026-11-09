# Große Testrunde vom 09.11.2026

Vergleichsstand war `0d17bd3d`. Geprüft wurden die Anwendungskonfiguration
mit Änderungen zur Laufzeit, Feldhilfen und Neustartwarnung, die
Abhängigkeitsupdates sowie die Sonar-Korrekturen bis `b51470c2` auf `main`.

| Prüfung | Ergebnis |
| --- | --- |
| Optimierter Maven-Reactor, JDK 25, Maven-Modell und Coverage-Prüfer | Bestanden |
| Spotless und Checkstyle | Bestanden, auch nach der Browser-Testkorrektur |
| Anwendungstests | 2.794 bestanden, zwei Caddy-Opt-ins im Standardlauf übersprungen |
| Browser-Helfer und Frontend | 98 und 20 bestanden |
| Coverage- und Sonar-Offlineprüfungen | 18 und 47 bestanden |
| Isolierter Start des frischen JARs | Anmeldeseite lieferte HTTP 200 mit flüchtiger H2-Datenbank |
| Browser DE | 261 von 262 im Gesamtlauf. Der korrigierte Fall bestand im Nachlauf mit 6 von 6 Cosmos-Komponententests. Damit sind 262 verschiedene Fälle nachgewiesen. |
| Browser EN | 262 von 262 im Gesamtlauf bestanden |
| SonarQube auf `b51470c2` | Quality Gate grün, null offene Findings, null Hotspots |

Die Browser-Gesamtläufe nutzten jeweils vier getrennte Anwendungs-, H2- und
Mailpit-Umgebungen. Beide Sprachen und der deutsche Nachlauf verwendeten das
Image `sha256:55ca6a69d18761336ee11e31d54fc3ca9ab9e5ad9d97b0a4c221674527f1fde5`.
Aufbau und Abbau waren erfolgreich. Vor dem ersten deutschen Lauf fehlte die
passende Chromium-Revision für Playwright. Nach der Installation wurde derselbe
Image-Stand erneut geprüft.

Der deutsche Fall `COMPONENT-04` erwartete noch einen Tooltip im Mailtest.
Der Test prüft jetzt die seitliche Feldhilfe als eigenen Dialog und weiterhin
die Gestaltung der Aktions-Tooltips bei Hover und Fokus auf Desktop und
Smartphone. Der deutsche Nachlauf und der englische Gesamtlauf enthalten
diese Testkorrektur.

Der Sonar-Scan `0c260297-f9fe-43fd-bd09-aef225c1b07c` bestätigt die Behebung
der sieben Findings zu Konfigurationsspeicherung, Test-Lambdas und
Mailqueue-Benennung. Die 52 bestehenden Ausnahmen blieben bestehen.
Diese Analyse lief vor der Korrektur des Browsertests.

Offene automatisierte Szenarien und menschliche Sonderprüfungen werden im
[Testbedarf](testbedarf.md) und im [manuellen Prüfkatalog](manual-tests/README.md) geführt.
