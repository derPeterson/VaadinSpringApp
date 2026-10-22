# VaadinSpringApp

Lokales Entwicklungsprojekt mit Java 25, Spring Boot und Vaadin Flow. Die Anwendung
bildet Benutzerkonten, Sicherheit, Administration und persönliche Benachrichtigungen ab.
Sie wird ausschließlich auf der Entwicklungsmaschine mit austauschbaren Testdaten betrieben.

## Einstieg

| Ich möchte … | Hier weiterlesen |
| --- | --- |
| die Funktionen verstehen | [Anwendung](docs/anwendung.md) |
| entwickeln und konfigurieren | [Entwicklung](docs/entwicklung.md) |
| die fünf Codex-Arbeitsabläufe nutzen | [Arbeiten mit Codex](docs/codex-skills.md) |
| die Docker-Testumgebung mit Mailpit verwenden | [Docker](docs/docker/README.md) |
| Anmeldung, Rechte und Sitzungen verstehen | [Sicherheit](docs/sicherheit.md) |
| Farben, Komponenten oder das Theme anpassen | [Theme](docs/theme.md) |
| Mailversand, Wiederholungen und Versandfehler verstehen | [Mailqueue](docs/mailqueue.md) |
| Logmeldungen und Diagnose verstehen | [Logging](docs/logging.md) |
| automatisierte Tests ausführen | [Tests](docs/tests.md) und [Browser-Testanleitung](browser-tests/README.md) |
| manuell prüfen oder Mailbeispiele versenden | [Manuelle Sonderprüfungen](docs/manual-tests/README.md) |
| noch fehlende automatisierte Prüfungen sehen | [Testbedarf](docs/testbedarf.md) |
| die letzten Prüfergebnisse sehen | [Aktueller Stand](docs/status.md) |
| technische Entscheidungen und ihre Grenzen verstehen | [Entscheidungen](docs/entscheidungen.md) |
| offene Anwendungsfehler sehen | [BUGS](BUGS.md) |
| die nächsten Aufgaben sehen | [TODO](TODO.md) |

Der [Dokumentationseinstieg](docs/README.md) führt zusätzlich zu den aktiven Umsetzungsplänen.

## Start und Build

Voraussetzungen: JDK 25; für Container- und Browserprüfungen Docker Desktop mit Linux-Containern.
Im Projektverzeichnis:

```powershell
.\mvnw.cmd --version
.\mvnw.cmd clean verify -Pproduction -Dvaadin.productionMode=true
```

Die Root-POM aggregiert `shared`, `app` und `browser-tests`; das Anwendungs-JAR
entsteht unter `app/target/`. Gezielter Build mit benötigtem Shared-Modul:

```powershell
.\mvnw.cmd -pl app -am verify -Pproduction -Dvaadin.productionMode=true
```

Reactor, lokaler IDE-/HTTPS-Start, Docker-Build, Browserstarter und gemeinsamer
Coverage-/Sonar-Ablauf sind eingerichtet. Die gemeinsame Funktionsprüfung samt
vollständiger Browserrunde DE/EN am selben aktuellen Image ist bestanden;
Ergebnisse und Grenzen stehen im [Prüfstand](docs/status.md). Main-Integration,
Sonar-Servernachweis und gemeinsame Gesamtabnahme bleiben im
[Multi-Modul-Plan](docs/planung/multi-modul-sonarqube.md) offen.

Der lokale HTTPS-Zugang ist `https://localhost:8443`; dafür den Caddy-Proxy und
das Zertifikatsvertrauen gemäß [Entwicklungsanleitung](docs/entwicklung.md#lokales-https)
einrichten. In IntelliJ startet die gespeicherte Konfiguration **Application HTTPS**
Anwendung und Caddy gemeinsam über Run oder Debug und beendet den eigenen Proxy
beim Stoppen. `https-zertifikat-einrichten.bat` richtet nach dem ersten Proxystart
einmalig das Windows-Zertifikatsvertrauen ein. Alternativ startet
`https-server-starten.bat` nur den Proxy; dazu die Anwendung separat mit Profil
`https` in IntelliJ oder über Maven starten.
Der interne HTTP-Port 8080 dient dem Proxy. Lokale
Einstellungen werden über Spring Properties, Umgebungsvariablen oder die nicht
versionierte `.env` gesetzt. Zugangsdaten gehören nicht ins Repository.

Optimierten Vaadin-Build samt Anwendungstests erstellen:

```powershell
.\mvnw.cmd clean verify -Pproduction -Dvaadin.productionMode=true
```

Der Begriff Produktionsbuild bezeichnet den optimierten Frontend-/JAR-Build;
er bedeutet in diesem Projekt keinen produktiven Betrieb.
