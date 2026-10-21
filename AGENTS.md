# Instructions for coding agents

## 1. Orientation

- Read `README.md` first, then only files relevant to the task.
- Verify README statements against current code when the task depends on them.
- Search for relevant classes, callers and configuration; do not read the entire project.
- Skip dependencies (`node_modules/`), generated files, `data/` and `logs/`.
- Do not read `.env` files, databases or files containing credentials.
- Write progress updates and final reports in German.

## 2. Project conventions

- Das Projekt ist ausschließlich lokale Entwicklung ohne reale/produktive Daten.
  Es gibt keine Unterstützung anderer historischer Schemata. Keine Schema-Updater,
  Altspalten-/Umbenennungswege oder Backfills vorsorglich einführen. Das aktuelle
  H2-Schema steht in `db/application-schema-h2.sql`, JPA validiert es. Modell und
  Schema gemeinsam ändern und mit frischen isolierten Testdaten prüfen. Aktuelle
  Eingabe-, Rechte-, Widerrufs- und Versionsprüfungen sind keine Altlasten.
- Dokumentation beschreibt den heutigen Funktionsvertrag. Kurze Entscheidungen mit
  Vorteilen/Grenzen und echte offene Aufgaben sind erwünscht; keine neuen Triage-CSV,
  Nachscan-/Updatechroniken oder fortlaufenden QA-Meilensteinserien. Der aktuelle
  Prüfstand steht in `docs/status.md`; Git enthält die Entwicklungsgeschichte.

- Keep Java 25 and the Spring Boot / Vaadin Flow stack defined in `pom.xml`.
- Build views with the existing Vaadin Java components. Do not replace them with
  HTML templates, React or another frontend.
- Application Java source is under `src/main/java/de/derpeterson/app/`.
  The shared typed translation catalogue and route/app constants are under
  `shared/src/main/java/`, owned by the independent Maven project `shared/pom.xml`.
  Application and browser tests depend on its JAR; never add its source directory
  to another module. It has no Spring/Vaadin dependencies.
- Follow the existing architecture: `views/` and `ui/` for the UI,
  `service/` for application logic, `repository/` for JPA access, `model/` for data.
- Follow neighboring files: names such as `*View`, `*Service`, `*Repository`,
  `*Entity`; preserve existing constructor injection and Lombok patterns.
- Make small, focused changes. Do not perform unsolicited refactoring.
- Explain regular expressions in a nearby plain German comment: purpose, accepted
  characters, anchors, groups and replacement references where applicable. Reuse
  existing central validation rules instead of introducing parallel regex policies.
- Change dependencies or versions only when required by the task.
- Styles and UI images: `src/main/resources/META-INF/resources/custom-theme/`.
  Gemeinsame Farben, Flächen, Verläufe und Schatten zentral in `cosmos-tokens.css` pflegen.
  `cosmos-theme.css` enthält gemeinsame Komponenten, `cosmos-shell.css` App-Rahmen/Overlays,
  `cosmos-admin.css` Admin-Bausteine und `admin-users.css` lokale Benutzeranordnung.
  Die Stylesheet-Reihenfolge in Application/AdminView erhalten. Keine parallelen Hex-/RGBA-
  Farben in Komponentenregeln einführen; vorhandene Tokens verwenden. Erläuterungen und
  Vorgehen für weitere Themes: `docs/theme.md`. Native Lumo-Zustände und die benannten
  Grid-@media-Einschübe erhalten. Layoutmaße/Icons nicht als Nebenwirkung ändern.
  Styles are loaded in `Application.java`; additional icons are under
  `src/main/resources/META-INF/resources/icons/`.
- Translations: `src/main/resources/i18n/`; email templates: `src/main/resources/email/`.
- Use the existing typed `MessageProperties` getters for fixed translation keys.
  Their single definition is in shared `MessageTexts`; do not create a second key/getter
  catalogue. Use AdminConfigGroup enum values for configuration group identity, never
  properties keys or translated labels. Use typed status/configuration label methods
  in UI components and fixed-locale getters for UI test expectations. Keep raw
  translation lookup calls inside the i18n layer. Normal MessageProperties follows the
  session at each call; use forLocale only when a fixed explicit language is required.
  For visible notifications that must update on language change, pass dynamic
  translation suppliers for title and body to the existing `NotificationHelper`.
  Do not freeze translated strings before that update mechanism.
- Load server-side resources through classpath streams, not `src/` filesystem paths.
  Resource loading must also work inside a JAR.
- Do not manually edit `src/main/frontend/generated/`, `vite.generated.ts` or `target/`.
  Put custom Vite configuration in `vite.config.ts`.
- Java-Standard ist Google Java Format (`GOOGLE`, 2 Leerzeichen), zentral über
  Spotless im Root-`pom.xml`; Anwendung, Shared-Modul und Browsertests sind eingeschlossen.
- Nach JEDER eigenen Java-Änderung verbindlich vor Tests und vor dem Abschluss
  `.\mvnw.cmd spotless:apply` und anschließend `.\mvnw.cmd spotless:check` im
  Projektroot ausführen, auch bei kleinen Änderungen ohne Build/Testlauf.
  Nach späteren Java-Korrekturen diesen Schritt wiederholen. Dies ist der dauerhafte
  Standard für künftige Aufgaben, kein einmaliger Formatierungslauf.
- Spotless berücksichtigt über `ratchetFrom=HEAD` geänderte und neue Java-Dateien.
  Bei fremden vorhandenen Java-Änderungen beide Aufrufe mit `-DspotlessFiles=...`
  auf die selbst bearbeiteten Dateien begrenzen; der Wert enthält reguläre Ausdrücke
  für absolute Dateipfade. Fremde Änderungen erhalten. Den Ratchet nicht abschalten
  und keine projektweite Umformatierung ohne eigenen Auftrag ausführen.
  Anleitung und Grenzen: `docs/entwicklung.md`, Abschnitt Codestil.
- Java-Lesbarkeit: Kontrollblöcke bei `if`, `else`, `for`, erweitertem `for`,
  `while` und `do/while` immer mit geschweiften Klammern schreiben, auch für eine
  Anweisung. `else if` darf als Kette stehen. `try`/`catch`/`finally` übersichtlich
  gliedern; keine fachlich unterschiedlichen Aktionen in eine Zeile pressen.
- Explizite lokale Typen sind der Standard. `var` nur verwenden, wenn es einen
  konkreten Lesbarkeitsvorteil hat: z. B. anonyme oder nicht direkt benennbare
  Typen sowie sehr lange generische Typen, die bereits direkt im Konstruktor
  sichtbar sind. Bei Service-/Factory-Rückgaben den Typ grundsätzlich ausschreiben.
  `var` ist keine dynamische Typisierung und hat keinen Laufzeitvorteil.
- Explizite normale und statische Imports verwenden, keine Sternchenimporte.
  Spotless ordnet die Imports und entfernt unbenutzte Imports.
- Sprechende Namen für fachliche Objekte und längere Lambdas verwenden.
  Verschachtelte ternäre Ausdrücke in verständliche Schritte oder benannte
  Methoden aufteilen; einfache kurze Fallunterscheidungen dürfen ternär bleiben.
  Große Methoden nach tatsächlichen Aufgaben gliedern; keine starren
  Zeilenlimits und keine reine Verschiebung in bedeutungslose Hilfsmethoden.
  Bestehende Transaktions-, Sperr- und Fehlergrenzen erhalten. Kommentare erklären
  Gründe, Verträge und Besonderheiten statt den offensichtlichen Ablauf.
- Nach eigenen Java-Änderungen zusätzlich im Projektroot
  `.\mvnw.cmd checkstyle:check` vor Tests und Abschluss ausführen. Die zentrale
  Konfiguration `config/checkstyle.xml` prüft Klammern und Sternchenimporte über
  Anwendung, Shared und Browsertests. Die sinnvolle Typwahl und Methodengliederung
  bleiben Teil des menschlichen Reviews. Anleitung: `docs/entwicklung.md#codestil`.
- Logging: Normalbetrieb INFO; DEBUG gezielt für fachliche Diagnose. Keine Methoden-/
  Abzweig-/Poll-/Aktivitätsprotokollierung. Nur wichtige Ergebnisse, behandelbare
  Störungen und Fehler melden; keine doppelten Fehlerlogs an mehreren Grenzen.
  Erfolg innerhalb einer Transaktion erst nach Commit über `AfterCommitLog`; nach
  unabhängigem TransactionTemplate-Commit darf direkt geloggt werden. Keine Entities
  als verzögerte Logargumente. Queueaufnahme und Transportannahme klar unterscheiden.
- Logs enthalten nur freigegebene Metadaten (interne IDs, Typen, Anzahlen, Zustände).
  Keine Credentials, Token/Serien/Digests, Sessionkennungen, Principalobjekte,
  E-Mail-Adressen, Nachrichtentexte oder rohe Formular-/Konfigurationswerte ausgeben.
  Anwendungsfehler mit `LogSafety.safeFailure` protokollieren: Typ und Stackframes
  bleiben, freie Exceptiontexte entfallen; Originale weiter unverändert behandeln.
  Die beiden geerbten Security-Logger ausdrücklich auf INFO belassen, auch bei
  Diagnose-DEBUG am Elternpaket. Regeln und Einführung: `docs/logging.md`.
- Python-Standard ist Ruff, zentral in `ruff.toml`; die genaue Version steht auch
  in `requirements-dev.txt`. Installation nur in der virtuellen Projektumgebung:
  `.\.venv\Scripts\python.exe -m pip install -r requirements-dev.txt`.
- Nach JEDER eigenen Python-Änderung vor Tests und vor dem Abschluss
  `.\.venv\Scripts\python.exe -m ruff format <eigene Dateipfade>` und danach
  `.\.venv\Scripts\python.exe -m ruff format --check <eigene Dateipfade>`
  im Projektroot ausführen. Die Platzhalter durch explizite, korrekt zitierte
  Pfade ersetzen; nach weiteren Python-Korrekturen beide Schritte wiederholen.
  Anders als Spotless hat Ruff hier keinen Git-Ratchet: nur selbst bearbeitete
  Dateien übergeben, fremde Änderungen erhalten. Keine projektweite Formatierung
  ohne eigenen Auftrag; kein `ruff check --fix` als Formatierungsersatz.
  Anleitung und Grenzen: `docs/entwicklung.md`, Abschnitt Codestil.

## 3. Execution and troubleshooting

- Respect the actual shell: use PowerShell commands on Windows.
- Use the Maven Wrapper `.\mvnw.cmd` from the project root, not global Maven.
  On Linux/macOS, use `./mvnw`. Before direct application Maven builds on a
  fresh checkout or after changes to shared Java sources, run
  `.\mvnw.cmd -f shared/pom.xml clean install`. The browser starter, Docker build
  and workflow benchmark rebuild this dependency automatically.
- Before builds, run `.\mvnw.cmd --version` and verify that the actual JDK is 25.
  If it is different, do not continue building or change system software silently.
- Always specify Maven goals: according to `pom.xml`, a wrapper invocation without
  a goal starts the application by default.
- On build failure, inspect your diff and the first relevant cause before downstream
  errors. Report external blockers clearly.
- Do not downgrade Java or remove functionality to work around failures.
- For API changes, check official documentation for the version being used.
  Label assumptions and unverified behavior; do not present them as facts.

## 4. Validation

- Run appropriate existing tests when execution is allowed by the task.
- Browser-UI-Testlaeufe nur nach ausdruecklicher Freigabe des Benutzers starten;
  gesammelt zum Abschluss statt automatisch nach einzelnen Aenderungen.
  Gezielte JUnit-/Helpertests sowie Formatierungs- und Ressourcenpruefungen
  bleiben davon unberuehrt. Eine fruehere Testfreigabe gilt nicht pauschal
  fuer spaetere Aufgaben.
- For behavior changes, add focused tests for the expected behavior.
- Java tests belong under `src/test/java/`; follow existing JUnit patterns.
  `ImageHelperTest` is an example without application startup or a database.
- Browser tests live under `browser-tests/src/test/java/` and use
  `browser-tests/run.ps1`; follow `browser-tests/README.md`. The Java/JUnit runner
  creates disposable isolated Docker data and generates local Allure and Markdown reports.
  Suites live under `browser-tests/src/test/java/de/derpeterson/browsertests/suites/`;
  keep infrastructure, authentication, registration, verification, password-reset,
  account-lifecycle, presentation and token-configuration cases separate. Shared UI actions belong in
  `pages/`, not in one growing test class. Select suites with `-Suite`.
  `-Suite fast` selects existing cases explicitly marked `@Tag("fast")`; keep the
  documented case list in browser-tests/README.md synchronized. All marked cases
  remain in `all`; do not duplicate test implementations or weaken assertions.
  `-Workers 2` partitions whole classes into isolated JVM/Docker/H2/Mailpit stacks.
  Preserve sequential JUnit execution inside each worker. Never enable shared-fixture
  thread parallelism as a shortcut. Build the image once and pin all workers to its ID;
  compile shared/browser sources once before forking, not concurrently per worker.
  Do not build/retag/remove app images during a browser run. Restart cases need
  their exact image reference until cleanup; a running container alone is insufficient.
  Document each browser case independently with its ID, area, steps and expected result.
  Keep browser documentation and reports independent of the manual-test catalogue.
  Keep technical rules, transactions and concurrency in JUnit/persistence tests; browser
  tests check visible UI behavior and user journeys. Manual checks cover special cases
  requiring human judgment or unsuitable for automation.
  Manual special checks live in docs/manual-tests/README.md: human acceptance and
  explicit gaps only. Keep properties.example minimal. Do not recreate redundant
  protocol templates or fictional example result archives. Once automated, remove
  the manual duplication; preserve checks that require human judgment.
  Use the shared MessageTexts getters through UiTexts for browser labels/messages,
  and the shared AppRouteConstants/AppConstants for existing route/title/avatar contracts;
  BrowserRoutes derives local browser paths (never security wildcards). Read original
  i18n properties via the test module's UiTexts adapter; do not duplicate German/English strings or accept both
  languages at once. Use the explicit -Language de/en selection, choose it through the UI,
  and account for saved user preferences after login. Missing keys must fail. Keep stable
  route/title contracts and invented test data distinct from translated UI text.
  For mail journeys, register through the UI and read only the own recipient's captured
  SMTP mail through MailpitInbox; never use mail generation APIs or service/database
  calls to bypass the UI. Keep activation/reset links on the runner's exact local origin;
  distinguish captured mail by its own recipient and the expected localized subject.
  Do not target existing application databases.
- Database regression tests may use isolated test databases such as H2; never
  connect them to existing application databases or reuse application data.
  Use controlled synchronization for concurrency tests rather than timing alone.
  Keep required UI/session fixtures strongly referenced and clean up listeners
  and contexts after each test.
- Distinguish build success, tests actually executed and manual functional checks.
  A successful build without test classes is not a passed functional test.
- The compile phase may already generate frontend files.
- Maven profile `it` starts and stops the application; do not use it without authorization.
- Do not modify existing application database data, start the application or
  send emails unless included in the task.
- Browser-Testausgaben unter `browser-tests/runs/` sind ignorierte, bereinigbare
  Diagnosedaten. Ihre dauerhafte Aufbewahrung oder Sicherung ist nicht erforderlich.
  Keine konkreten alten Run-Pfade oder Run-Listen in dauerhafte Dokumentation aufnehmen.
  Aktuellen Funktions-/Prüfstand knapp in docs/status.md pflegen: geprüfter Stand,
  tatsächliche Ergebnisse, wesentliche Erkenntnisse und offene Lücken. Keine
  Abhängigkeit von lokalen Screenshots/Traces und keine Serie alter Laufberichte.
  Bestehende Run-Ordner nur auf entsprechenden Auftrag löschen; keine automatische
  Aufräumroutine oder neue unbegrenzte Archivablage einführen.
- Before finishing, run `git diff --check` and review the scope of changes.
- If a build changes generated files already tracked in Git, inspect and report them.
  Include them only when related to the task; do not discard them indiscriminately.

## 5. Git

- Before changes, check the branch and working tree with `git status --short --branch`.
- Preserve the user's existing changes. Do not overwrite or discard them.
- Follow the branch, commit and final-state workflow of an explicitly invoked
  OpenCode command. Do not invent another workflow; clarify ambiguities.
- Do not commit, merge or push without a corresponding instruction.
- When a commit is authorized, describe the completed change in the message
  (German past tense), not a planned change.
- Do not use destructive commands such as `git reset --hard`, `git checkout --`
  or `git clean` to remove changes made by others.

## 6. Honest final report

- Briefly report changed files and the outcome in German.
- Report only commands actually executed and results actually observed.
- State test counts, errors/failures and outstanding checks.
  Explicitly state when nothing was executed.
- Obtain commit hashes from actual Git output only.
- Explicitly label unfinished work as incomplete.
- If reporting task duration, capture start and end using the system clock.
  Never estimate or invent a duration; without measurement, give no duration.

## 7. Abgestimmtes Design und schrittweise Änderungen

- Designänderungen einzeln und ausschließlich im ausdrücklich angefragten Umfang umsetzen.
- Bestehende, abgestimmte Gestaltung beibehalten. Keine Änderungen mit globalen Auswirkungen
  ohne ausdrücklichen Auftrag des Benutzers; eine lokale Anpassung ist keine Erlaubnis
  zur Änderung gemeinsamer Styles auf anderen Views.
- Icons nicht aufgrund einer Preview austauschen. Änderungen am Button-Aussehen lassen
  das bestehende Icon und seine Größe unverändert, sofern nicht ausdrücklich angefragt.
- Primary-Buttons: zentraler blauer Cosmos-Verlauf mit sichtbarem Border.
- Secondary-Buttons: Hintergrund wie die Inputfelder, Schrift in Primary-Farbe und
  der zuletzt abgestimmte dezente Border: 1px solid var(--lumo-contrast-20pct).
- Gemeinsame Primary-/Secondary-Basis: Lumo M (36px), 9px Eckenradius, 13px Schrift / 600.
  Die vorhandene 14px-Schriftbasis für em-basierte Button-Icons bleibt erhalten;
  Icons und individuelle Pagination-/Tabellenaktionen nicht global umskalieren.
- Fragezeichen-, Erfolgs- und Warnsymbole der Kontoansichten verwenden zentral
  var(--lumo-font-size-xxxl) für Breite und Höhe (standardmäßig 40px).
- Die verbindlichen Gestaltungsregeln stehen in docs/theme.md#gestaltungsregeln.
  Neue Designänderungen bleiben auf den angefragten Umfang begrenzt. Nicht ohne Auftrag committen; der Benutzer
  sichert den Stand selbst.
- Nach jeder Änderung ausdrücklich angeben, wie sie sichtbar wird: Browser-Refresh
  (gegebenenfalls Strg+F5), Anwendungsneustart oder vorheriger Build. Dabei den
  tatsächlichen Entwicklungsstand berücksichtigen; bei veralteten komprimierten
  CSS-Ressourcen reicht ein Browser-Refresh nicht. Keine Übernahme behaupten,
  wenn nur die Quelldatei geändert wurde.
- Bestätigter Tabellenstandard: alle Vaadin-Grids im Cosmos-Theme verwenden die
  vier zentralen Variablen in cosmos-tokens.css: --cosmos-table-header-background
  (#223353b3), --cosmos-table-row-background (#101a2a73),
  --cosmos-table-alternate-row-background (#18213681) und
  --cosmos-table-highlight-background (#1B2B42). Zeilenfarben wechseln automatisch;
  GridVariant.ROW_STRIPES oder eine spezielle Benutzerverwaltungs-Klasse ist dafür
  nicht erforderlich. Hover und Auswahl verwenden dieselbe Hervorhebungsfarbe.
  Der Hover ist dauerhaft freigegeben und kein Testblock mehr. Gemeinsame Regeln
  wiederverwenden; keine parallelen Tabellenfarben für neue Views einführen.
