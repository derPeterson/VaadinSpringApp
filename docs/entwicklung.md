# Entwicklung

## Voraussetzungen und Aufbau

JDK 25 und der Maven Wrapper sind verbindlich. Die tatsächlichen Bibliotheksversionen
stehen in `pom.xml`; Node-Abhängigkeiten in `package-lock.json`. Nicht neben diesen
Dateien eine zweite Versionsliste pflegen. Docker Desktop verwendet Linux-Container.

| Verzeichnis | Zuständigkeit |
| --- | --- |
| `src/main/java/de/derpeterson/app/views`, `ui` | Vaadin-Views und Komponenten |
| `service`, `repository`, `model` | Fachlogik, JPA-Zugriff und Datenmodell |
| `security`, `config`, `scheduler` | Zugangsschutz, Startkonfiguration und Hintergrundaufgaben |
| `shared` | Eigenes Maven-Modul für typisierte Texte und gemeinsame Konstanten |
| `src/main/resources/i18n`, `email` | DE-/EN-Texte und Mailvorlagen |
| `src/main/resources/db` | Aktuelles H2-Anwendungs- und Session-Schema |
| `src/test`, `browser-tests` | Technische Tests und echte Browserjourneys |

Shared-Quellen nicht als Quellordner in andere Module einhängen. Vor einem frischen
Anwendungsbuild oder nach Shared-Änderungen das Modul installieren:

```powershell
.\mvnw.cmd --version
.\mvnw.cmd -f shared/pom.xml clean install
.\mvnw.cmd spring-boot:run
```

## Konfiguration und Datenbank

Spring lädt `application.properties` sowie optional die lokale, nicht versionierte
`.env`. `app.config.defaults.*` bzw. die vorgesehenen `APP_CFG_*`-Variablen liefern
Vorgaben für fehlende Datenbankeinträge. Gespeicherte Adminwerte haben Vorrang.
`ConfigService` bewahrt Strings unverändert; typisierte Getter lehnen ungültige
Zahlen/Booleans ab. SMTP, Remember-me und Workerzahl werden beim Start gebunden.

Bootstrap ist standardmäßig aus. Die optionalen Listen `app.bootstrap.superadmins`,
`admins`, `users` und `guests` legen explizite lokale Testkonten mit genau ihrer
vorgesehenen Rolle an. Alle Einträge werden vor der ersten Anlage geprüft; die
Anlage ist transaktional und überschreibt keine schon vorhandenen Konten.
[Beispiel](bootstrap-accounts.example.properties).

`db/application-schema-h2.sql` beschreibt das aktuelle Anwendungsschema.
Beim Start werden fehlende Tabellen/Indizes angelegt; Hibernate validiert die
Übereinstimmung mit den JPA-Mappings. Bestehende Tabellen werden nicht umgebaut.
Schema- und Modelländerungen gehören zusammen in denselben Arbeitsstand und werden
mit einer frischen isolierten Testdatenbank geprüft. Lokale Testdaten sind austauschbar.
Remember-me legt seine Tabellen in `DeviceTokenRepository` an; Spring Session
verwendet `db/durable-sessions-h2.sql`. Beide Initialisierungen sind idempotent.
Kontoadresse, Remember-me und dauerhafte Sessionidentität unterstützen 255 Zeichen.

Die normale lokale H2-Datei liegt unter `data/`; Docker und der Browserrunner haben
getrennte Datenbanken. Dateien aus `data/`, `logs/`, `.env` und Browserläufen nicht
als Build-Eingaben oder Dokumentationsarchiv übernehmen.

## Build und Änderungen übernehmen

```powershell
.\mvnw.cmd clean verify -Pproduction -Dvaadin.productionMode=true
```

Java-/Konfigurationsänderungen benötigen Build und Neustart. Im Entwicklungsmodus
lädt Vaadin die Quellstyles; bei optimiertem Frontend sind Neubau und anschließendes
Browser-Neuladen erforderlich. `vite.config.ts` ist für eigene Vite-Anpassungen;
generierte Frontenddateien und `target/` nicht von Hand bearbeiten.
Classpathressourcen als Streams laden, damit auch das gepackte JAR funktioniert.

## Codestil

Java folgt Google Java Format, zwei Leerzeichen. Nach jeder eigenen Java-Änderung
vor Tests und Abschluss im Projektroot:

```powershell
.\mvnw.cmd spotless:apply
.\mvnw.cmd spotless:check
.\mvnw.cmd checkstyle:check
```

Kontrollblöcke verwenden immer Klammern. Explizite Typen und Imports sind Standard;
`var` nur bei einem konkreten Lesbarkeitsvorteil. Keine Sternchenimporte. Methoden
nach fachlichen Aufgaben gliedern, Transaktions-/Sperrgrenzen bewusst erhalten.
Spotless berücksichtigt Änderungen gegen HEAD. Bei fremden Java-Änderungen mit
`-DspotlessFiles=...` auf die eigenen Pfade begrenzen; Anleitung der Option beachten.

Python verwendet Ruff aus `requirements-dev.txt`, installiert in `.venv`:

```powershell
.\.venv\Scripts\python.exe -m pip install -r requirements-dev.txt
.\.venv\Scripts\python.exe -m ruff format PFAD
.\.venv\Scripts\python.exe -m ruff format --check PFAD
```

Nur eigene Python-Dateien formatieren. Technische Projektregeln stehen in `AGENTS.md`.
Neue feste Texte über den gemeinsamen `MessageTexts`-/`MessageProperties`-Katalog
bereitstellen. Dynamische UI-Texte müssen dem aktuellen Sprachwechsel folgen.
