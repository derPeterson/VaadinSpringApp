# Große Testrunde vom 10.11.2026

Geprüft wurde `main` auf `9c0d53e8` gegenüber dem letzten vollständigen
Teststand `6b156cc9`. Seitdem wurden Java-Klassen in App, Shared und Browsertests
lesbarer gegliedert, Klassen und ihre Verweise umbenannt und die Prüfungen für
Statusauswahl, Avatar-Rücksetzung und Seitenüberschriften präzisiert. Die vorhandenen
Tests deckten die betroffenen Abläufe ab. Die Runde ergab keinen Korrektur- oder
Ergänzungsbedarf.

| Prüfung | Ergebnis |
| --- | --- |
| Optimierter Maven-Reactor, JDK 25, Maven-Modell und Coverage-Prüfer | Bestanden |
| Spotless und Checkstyle | Bestanden |
| Anwendungstests | 2.794 bestanden, zwei optionale Caddy-Tests übersprungen |
| Browser-Helfer und Frontend | 98 und 20 bestanden |
| Coverage- und Sonar-Offlineprüfungen | 18 und 47 bestanden |
| Isolierter Start des frischen JARs | `/login` lieferte HTTP 200 mit flüchtiger H2-Datenbank |
| Browser `all` DE | 262 von 262 bestanden |
| Browser `all` EN | 262 von 262 bestanden |
| SonarQube auf `9c0d53e8` | 0 offene Findings im Gesamtprojekt und im neuen Code, 0 Security Hotspots, Quality Gate grün |

Die beiden Browserläufe nutzten jeweils vier getrennte Anwendungs-, H2- und
Mailpit-Umgebungen mit demselben Image
`sha256:897365897184dae1aea50b280c46bf80e74086e92beb880d5875bbea0381d317`.
Aufbau und Abbau waren erfolgreich. Der isolierte Prozess für die Startprüfung
wurde ebenfalls beendet.

Die SonarQube-Analyse `7ea67210-41a7-4114-be6e-f0c73d7ae846` gehört zum
geprüften Commit. Sie bestätigt die Behebung der beiden Befunde zur
Rollenauswertung und Browsertest-Auswahl durch Methodenreferenzen. Die drei
weiteren Befunde sind direkt in SonarQube als zwei Fehlalarme und eine
akzeptierte TOTP-Ausnahme begründet.

Der [Testbedarf](testbedarf.md) enthält keine offenen automatisierten Fälle.
Menschliche Sonderprüfungen stehen im [manuellen Prüfkatalog](manual-tests/README.md).
