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
| SonarQube auf `6b156cc9` | Sechs offene Findings, null Hotspots, Quality Gate rot |

Die beiden Browserläufe nutzten jeweils vier getrennte Anwendungs-, H2- und
Mailpit-Umgebungen mit dem Image
`sha256:f8ce06933b301fed2ed08f871de1809e288e60e2924f334d81214fba7fffbba4`.
Aufbau und Abbau waren erfolgreich.

Der Sonar-Nachscan `4d9cc89c-982f-44fb-95b4-26f6a32bdf08` bestätigt die
Behebung des Transaktions-Findings. Die verbleibenden Befunde betreffen die
Ausgabekanäle und Fehlerbereinigung des IDE-HTTPS-Supervisors, die Uhr der
Vaadin-Benutzerfilterung, den Rollen-Broadcaster und die begrenzte
CSRF-Konfiguration. Die technische Testrunde ist vollständig bestanden.
Die [Entscheidung zu den Sonar-Befunden](planung/java-lesbarkeit.md) bleibt offen.

Offene automatisierte Szenarien und menschliche Sonderprüfungen werden im
[Testbedarf](testbedarf.md) und im [manuellen Prüfkatalog](manual-tests/README.md)
geführt.
