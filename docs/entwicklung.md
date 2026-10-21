# Entwicklung

## Voraussetzungen und Aufbau

JDK 25 und der Maven Wrapper sind verbindlich. Die tatsächlichen Bibliotheksversionen
stehen in `pom.xml`; Node-Abhängigkeiten in `package-lock.json`. Nicht neben diesen
Dateien eine zweite Versionsliste pflegen. Docker Desktop verwendet Linux-Container.

Die Paketnamen beziehen sich auf `src/main/java/de/derpeterson/app/`:

| Paket | Zuständigkeit |
| --- | --- |
| `Application` (Basispaket) | Spring-Boot-Einstieg, App-Shell und zentrale Stylesheets |
| `config` | Spring-/Vaadin-Startkonfiguration, Bootstrap, Schemaanlage, Mail und Threadpools |
| `views`, `views/admin` | Navigierbare Seiten und Administrationsbereiche |
| `ui/base`, `ui/components` | Gemeinsame View-Basen und wiederverwendbare Komponenten/Dialoge |
| `service` | Fachaktionen, Transaktionen, Konto-/Bild-/Benachrichtigungslogik und Mailqueue |
| `repository` | JPA-Abfragen, Datenzugriff und Sperren |
| `model`, `model/enums` | Persistente Datenmodelle und fachliche Typen |
| `security`, `security/session` | Login, Rechte, Wartungsschutz, Geräte und dauerhafte Sitzungen |
| `scheduler` | Zeitgesteuerte Verfügbarkeitsprüfung und Benachrichtigungsbereinigung |
| `i18n` | Sitzungsabhängige Übersetzungen und Sprachanbindung |
| `validation` | Gemeinsame Eingaberegeln und fachliche Validierungsfehler |
| `events`, `websocket` | Sprach-/Rollenereignisse und Live-Aktualisierung angebundener UIs |
| `helper/*` (email, image, logging, text, time, ui) | Begrenzte Hilfsfunktionen für Ressourcen, Diagnose, Zeit und Komponenten |

| Weiterer Bereich | Zuständigkeit |
| --- | --- |
| `shared/src/main/java` | Eigenes Maven-Modul mit typisiertem Textkatalog, Konfigurations-/Routenkonstanten und Token-Zeitregeln |
| `src/main/resources/i18n`, `email` | DE-/EN-Texte und Mailvorlagen |
| `src/main/resources/db` | Aktuelles H2-Anwendungs- und Session-Schema |
| `src/main/resources/META-INF/resources/{custom-theme,icons}` | Gemeinsames Theme und zusätzliche Icons |
| `src/test`, `browser-tests` | Technische Tests, echte Browserjourneys und deren Runner |
| `.agents/skills` | Codex-Projektabläufe; Bedienung unter [Arbeiten mit Codex](codex-skills.md) |

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

## Arbeiten mit Codex

Die [Skill-Anleitung](codex-skills.md) beschreibt alle fünf Abläufe: große Testrunde,
Sonar-Bereinigung, Umsetzung starten, Änderungen prüfen und lokal mergen.
Testbefehle stehen in der [Testanleitung](tests.md).

## Codestil

Java folgt Google Java Format, zwei Leerzeichen. Nach jeder eigenen Java-Änderung
vor Tests und Abschluss im Projektroot:

```powershell
.\mvnw.cmd spotless:apply
.\mvnw.cmd spotless:check
.\mvnw.cmd checkstyle:check
```

Code soll für Menschen lesbar sein: sprechende, verständliche Variablen- und
Methodennamen statt kryptischer Kürzel. Methoden haben eine klar erkennbare fachliche
Aufgabe. Tiefe Verschachtelung und verschachtelte ternäre Ausdrücke vermeiden;
Hilfsmethoden nur für tatsächlich benennbare Aufgaben, nicht zur bloßen Zeilenverteilung.
Javadoc oder Kommentare erklären nötige Verträge, Gründe und Besonderheiten;
offensichtlichen Ablauf nicht nacherzählen. Regex direkt auf Deutsch erläutern.

Kontrollblöcke bei `if`/`else`, Schleifen und `try`/`catch` übersichtlich mit Klammern
gliedern, auch bei einzelnen Anweisungen. Explizite Typen und Imports sind Standard;
`var` nur bei einem konkreten Lesbarkeitsvorteil, etwa bei nicht direkt benennbaren
Typen. Keine Sternchenimporte. Transaktions-, Sperr- und Fehlergrenzen erhalten.
Spotless berücksichtigt Änderungen gegen HEAD. Bei fremden Java-Änderungen mit
`-DspotlessFiles=...` auf die eigenen Pfade begrenzen; Anleitung der Option beachten.

Python verwendet Ruff aus `requirements-dev.txt`, installiert in `.venv`:

```powershell
.\.venv\Scripts\python.exe -m pip install -r requirements-dev.txt
.\.venv\Scripts\python.exe -m ruff format PFAD
.\.venv\Scripts\python.exe -m ruff format --check PFAD
```

Nur eigene Python-Dateien formatieren. Verbindliche Agentenregeln stehen in [AGENTS.md](../AGENTS.md).
Neue feste Texte über den gemeinsamen `MessageTexts`-/`MessageProperties`-Katalog
bereitstellen. Dynamische UI-Texte müssen dem aktuellen Sprachwechsel folgen.

Für sinnvolle Ereignisse, Logpegel und sichere Diagnose gilt der [Logging-Vertrag](logging.md).
