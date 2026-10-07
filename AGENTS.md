# Instructions for coding agents

## 1. Orientation

- Read `README.md` first, then only files relevant to the task.
- Verify README statements against current code when the task depends on them.
- Search for relevant classes, callers and configuration; do not read the entire project.
- Skip dependencies (`node_modules/`), generated files, `data/` and `logs/`.
- Do not read `.env` files, databases or files containing credentials.
- Write progress updates and final reports in German.

## 2. Project conventions

- Keep Java 25 and the Spring Boot / Vaadin Flow stack defined in `pom.xml`.
- Build views with the existing Vaadin Java components. Do not replace them with
  HTML templates, React or another frontend.
- Java source is under `src/main/java/de/derpeterson/app/`.
- Follow the existing architecture: `views/` and `ui/` for the UI,
  `service/` for application logic, `repository/` for JPA access, `model/` for data.
- Follow neighboring files: names such as `*View`, `*Service`, `*Repository`,
  `*Entity`; preserve existing constructor injection and Lombok patterns.
- Make small, focused changes. Do not perform unsolicited refactoring.
- Change dependencies or versions only when required by the task.
- Styles and UI images: `src/main/resources/META-INF/resources/custom-theme/`.
  Styles are loaded in `Application.java`; additional icons are under
  `src/main/resources/META-INF/resources/icons/`.
- Translations: `src/main/resources/i18n/`; email templates: `src/main/resources/email/`.
- Use the existing typed `MessageProperties` getters for fixed translation keys.
  For visible notifications that must update on language change, pass dynamic
  translation suppliers for title and body to the existing `NotificationHelper`.
  Do not freeze translated strings before that update mechanism.
- Load server-side resources through classpath streams, not `src/` filesystem paths.
  Resource loading must also work inside a JAR.
- Do not manually edit `src/main/frontend/generated/`, `vite.generated.ts` or `target/`.
  Put custom Vite configuration in `vite.config.ts`.
- Preserve the local code style. Spotless uses `eclipse-formatter.xml`;
  do not reformat the entire project without a task requiring it.

## 3. Execution and troubleshooting

- Respect the actual shell: use PowerShell commands on Windows.
- Use the Maven Wrapper `.\mvnw.cmd` from the project root, not global Maven.
  On Linux/macOS, use `./mvnw`.
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
- For behavior changes, add focused tests for the expected behavior.
- Tests belong under `src/test/java/`; follow existing JUnit patterns.
  `ImageHelperTest` is an example without application startup or a database.
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
