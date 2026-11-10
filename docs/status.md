# Große Testrunde vom 10.11.2026

Vergleichsstand war `1f427fba`. Geprüft wurden der projektweite
Java-Lesbarkeitsumbau in App, Shared und Browser-Tests und die gemeinsame
interne Abfrage der transaktionalen Passwort-Reset-Einstiege bis `6b156cc9`
auf `main`.

| Prüfung | Ergebnis |
| --- | --- |
| Optimierter Maven-Reactor, JDK 25, Maven-Modell und Coverage-Prüfer | Bestanden |
| Spotless und Checkstyle | Bestanden |
| Anwendungstests | 2.794 bestanden, zwei Caddy-Opt-ins im Standardlauf übersprungen |
| Browser-Helfer und Frontend | 98 und 20 bestanden |
| Coverage- und Sonar-Offlineprüfungen | 18 und 47 bestanden |
| Isolierter Start des frischen JARs | Anmeldeseite lieferte HTTP 200 mit flüchtiger H2-Datenbank |
| Browser DE | 262 von 262 bestanden |
| Browser EN | 262 von 262 bestanden |
| SonarQube auf `66b00fe6` | Null offene Findings im Gesamtprojekt und im neuen Code, null Hotspots, Quality Gate grün |

Die beiden Browserläufe nutzten jeweils vier getrennte Anwendungs-, H2- und
Mailpit-Umgebungen mit dem Image
`sha256:f8ce06933b301fed2ed08f871de1809e288e60e2924f334d81214fba7fffbba4`.
Aufbau und Abbau waren erfolgreich.

Die transaktionalen Passwort-Reset-Abfragen wurden im Code korrigiert.
Die sechs übrigen Sonar-Befunde wurden einzeln geprüft und direkt am
Finding begründet. Die beiden Caddy-Prozesskanäle sind Fehlalarme.
Die Fehlerbereinigung des IDE-HTTPS-Starts, die Uhr der Benutzerfilterung,
der Rollen-Broadcaster und die begrenzte CSRF-Konfiguration sind
akzeptierte Ausnahmen. Der vollständige Nachscan
`2f2c9ffa-7f1d-49c1-8501-0461eebf1eb2` bestätigt den grünen Stand.

Offene automatisierte Szenarien und menschliche Sonderprüfungen werden im
[Testbedarf](testbedarf.md) und im [manuellen Prüfkatalog](manual-tests/README.md)
geführt.
