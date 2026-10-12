# VaadinSpringApp

Lokale Docker-Testumgebung mit Mailpit und H2: [Docker-Anleitung](docs/docker/README.md).

Automatisierte Browserprüfungen mit Java, JUnit und Allure in eigener Docker-Testumgebung; getrennte Suites für Infrastruktur, Anmeldung und Zugriffsrechte sowie Registrierung, Verifikation und Passwortreset über echte Testmails, Einzelvalidierung, Sprachpräferenz und grundlegende Bedienung: [Browser-Test-Anleitung](browser-tests/README.md).

Java-Webanwendung als Grundlage für Anwendungen mit Benutzerkonten und Administration. Die Oberfläche wird serverseitig mit Vaadin Flow aufgebaut; ein weitergehender fachlicher Anwendungsbereich ist bisher nicht implementiert.

> Diese Beschreibung basiert auf dem Quellcode und der Build-Konfiguration. Der vollständige serverseitige Testlauf wurde am 07.10.2026 mit Java 25 geprüft (siehe „Tests und Prüfstand“). Node.js und der Package-Build wurden zuletzt am 01.10.2026 geprüft. Die Anwendung wurde nicht gestartet; ein erfolgreicher Laufzeitbetrieb ist damit nicht nachgewiesen.

## Funktionen

- Registrierung mit zunächst deaktiviertem Konto und Aktivierung per E-Mail-Token.
- Anmeldung per E-Mail und Passwort, Abmeldung, persistentes „Remember me“ und BCrypt-Passwort-Hashing.
- Passwort-Zurücksetzen über zeitlich begrenzte, nach Verwendung verbrauchte Tokens.
- Adminbereich mit Benutzerverwaltung, Rollen und Kontofreigabe, Konfiguration sowie Mail-Testversand.
- Deutsche und englische Oberfläche, manuell wählbarer Benutzerstatus und automatische Abwesenheitserkennung mit Push-Aktualisierung.
- Datenbankgestützte E-Mail-Warteschlange mit Wiederholungsversuchen und geplanten Bereinigungsaufgaben.

Worker-Koordination, Retry-Verhalten und konservativer Umgang mit unklaren
Versandausgängen: [Mailqueue-Verarbeitung](docs/email-queue-processing.md).

Belege: `views/RegistrationView.java`, `views/LoginView.java`, `views/admin/`, `service/VerificationService.java`, `service/PasswordResetService.java`, `service/EmailQueueService.java`, `security/SecurityConfig.java` und `scheduler/UserStatusScheduler.java` unter `src/main/java/de/derpeterson/app/`.

## Gemeinsame Texte und UI-Konstanten

Die stabilen Übersetzungs-Getter sind einmalig in
[MessageTexts](shared/src/main/java/de/derpeterson/app/i18n/MessageTexts.java) definiert.
Die Anwendung nutzt sie über `MessageProperties`, die Browsertests über `UiTexts`.
Bei einer Änderung eines Properties-Schlüssels wird der gemeinsame Getter angepasst;
seine Aufrufer bleiben unverändert. Direkte Übersetzungsaufrufe gehören in die I18n-Schicht.
Für Statuswerte verwenden UI-Komponenten `getUserStatusLabel(status)` und
`getUserStatusDescription(status)`, für Konfiguration die vorhandenen typisierten Methoden.
Konfigurationsgruppen werden durch `AdminConfigGroup` identifiziert, beispielsweise
`AdminConfigGroup.GENERAL`. Der Text kommt aus `getAdminConfigGroupLabel(group)`;
übersetzte Texte oder Properties-Schlüssel sind keine Gruppenkennungen. Somit bleiben
Navigation und Gruppenauswahl bei Schlüsseländerungen und DE/EN-Wechsel stabil.
Auch UI-Test-Erwartungen verwenden Getter mit expliziter Locale. Direkte Lookup-Aufrufe
gehören nur in die I18n-Adapter und deren gezielte Tests für fehlende Schlüssel.

`new MessageProperties(provider)` liest die Session-Sprache bei jedem Aufruf neu.
Für eine feste Übersetzung ohne Session gibt es
`MessageProperties.forLocale(provider, Locale.GERMAN)` beziehungsweise `Locale.ENGLISH`.
Die normale Oberfläche verwendet weiterhin die dynamische Variante.

[AppRouteConstants](shared/src/main/java/de/derpeterson/app/config/AppRouteConstants.java)
und [AppConstants](shared/src/main/java/de/derpeterson/app/config/AppConstants.java)
liegen ebenfalls unter `shared/src/main/java/`. Diese Quellen gehören ausschließlich
zum eigenständigen Maven-Projekt [shared/pom.xml](shared/pom.xml). Anwendung und
Browsertests verwenden dessen JAR als Abhängigkeit; die Packages und Namen bleiben gültig.
Es gibt keine doppelten Source-Roots und keine Spring-/Vaadin-Abhängigkeit im Shared-Modul.

In IntelliJ `shared/pom.xml` zusätzlich zu `pom.xml` und `browser-tests/pom.xml` als
Maven-Projekt hinzufügen und alle Maven-Projekte neu laden. Danach hat `MessageTexts`
einen eindeutigen Modulbesitzer; Find Usages mit dem Suchbereich „All Places“ kann
Aufrufe in Anwendung und Browsertests finden. Eine Getter-Umbenennung erfolgt über
Java-Refactoring an dieser gemeinsamen Definition.

Vor direkten Anwendungs-Maven-Builds auf einem frischen Checkout und nach Änderungen
an den gemeinsamen Java-Quellen das Shared-JAR lokal neu bauen:

```powershell
.\mvnw.cmd -f shared/pom.xml clean install
```

Der Browserstarter, Docker-Build und das Workflow-Benchmarking erledigen diesen Schritt
automatisch. Ein direkter `clean test`, `package` oder `spring-boot:run` im Projektroot
verwendet hingegen das zuletzt installierte Shared-JAR; nach Shared-Änderungen daher
zuerst den obigen Befehl ausführen. IntelliJ verwendet bei geladenem Shared-Modul die
Modulabhängigkeit. Die vorhandenen Anwendungsquellen und `target/` bleiben an ihrem Ort.

## Technologien und Voraussetzungen

- **JDK 25**, **Spring Boot 4.1.1**, **Vaadin 25.3.0**, Lombok; Versionen und Abhängigkeiten stehen in `pom.xml`.
- Spring Security, Spring Data JPA/Hibernate, dateibasierte H2-Datenbank und Spring Mail/SMTP.
- Maven Wrapper: `mvnw` bzw. `mvnw.cmd`, konfiguriert für **Maven 3.9.16** in `.mvn/wrapper/maven-wrapper.properties`. Dies ist laut [Apache-Downloadseite](https://maven.apache.org/download.cgi) die aktuelle stabile Maven-3.9.x-Version (geprüft am 01.10.2026). Das vorhandene JAR-basierte Wrapper-Verfahren mit Wrapper 3.1.0 bleibt erhalten. Unter Windows muss `JAVA_HOME` auf das JDK zeigen.
- Frontend-Werkzeuge: **Node.js 24 oder neuer**, bei Verwendung von npm **npm 11.3 oder neuer**, gemäß den [offiziellen Voraussetzungen für Vaadin 25.3.0](https://github.com/vaadin/platform/releases/tag/25.3.0). Lokal wurde Node.js **24.21.0** festgestellt; die npm-Version wurde nicht separat geprüft. Das Vaadin-Maven-Plugin bindet `prepare-frontend` und `build-frontend` an die Compile-Phase; Anpassungen stehen in `vite.config.ts`.
- Für den ersten Build ist Zugriff auf die Maven-/Frontend-Paketquellen erforderlich. Für Mailfunktionen wird zusätzlich ein erreichbarer SMTP-Server benötigt; er wird nicht mitgeliefert.

## Architektur und Einstiegspunkte

Der typische Ablauf ist **Browser → Vaadin-View → Service → JPA-Repository → H2**. Die UI ruft Java-Dienste direkt auf. Hintergrundaufgaben verarbeiten E-Mails und Benutzerstatus.

Die folgenden Java-Pfade liegen unter `src/main/java/de/derpeterson/app/`:

| Pfad | Aufgabe |
| --- | --- |
| `Application.java` | Spring-Boot-Einstieg, App-Shell, Styles, dunkles Farbschema, Push und Scheduling |
| `views/MainView.java`, `views/HomeView.java` | Einstieg unter `/`, Weiterleitung zur Startansicht `/home` |
| `views/`, `views/admin/` | Kontoseiten und Adminbereich `/admin`; Routen in `config/AppRouteConstants.java` |
| `security/` | Authentifizierung, Sitzungen, Remember-me und Zugriffsschutz |
| `service/`, `repository/`, `model/` | Anwendungslogik und Speicherung von Benutzern, Rollen, Tokens, Einstellungen und Mailwarteschlange |
| `config/DatabaseInitializer.java`, `service/ConfigService.java` | Initiale Rollen und Konfiguration, optionales erstes Adminkonto, datenbankgestützte Einstellungen |
| `scheduler/`, `websocket/UserStatusBroadcaster.java` | Automatische Statuswechsel und Verteilung von Statusänderungen an die UI |

## Einrichtung und Konfiguration

Alle Befehle im Projektverzeichnis ausführen. Zentrale Laufzeitkonfiguration: `src/main/resources/application.properties`. Sie importiert optional eine lokale `.env` im **Java-Properties-Format**; alternativ können die dort referenzierten Umgebungsvariablen gesetzt werden. Zugangsdaten lokal bereitstellen und nicht einchecken.

| Konfigurationsname | Bedeutung |
| --- | --- |
| `PORT` | HTTP-Port; Vorgabe `8080` |
| `APP_BOOTSTRAP_DEFAULT_ADMIN_ENABLED` | Optionales Adminkonto anlegen; Vorgabe `false` |
| `APP_BOOTSTRAP_DEFAULT_ADMIN_EMAIL`, `APP_BOOTSTRAP_DEFAULT_ADMIN_PASSWORD` | Für die aktivierte Adminanlage beide erforderlich; eine vorhandene E-Mail wird übersprungen |
| `APP_SECURITY_EXPOSE_H2_CONSOLE` | H2-Konsole unter `/h2` freigeben; Vorgabe `false` |
| `APP_CFG_REMEMBER_ME_SECRET_KEY` | Eigener geheimer Schlüssel statt des eingebauten Platzhalters |
| `APP_CFG_EMAIL_FROM`, `APP_CFG_EMAIL_ADMIN` | Absender und Empfänger für Adminbenachrichtigungen |
| `APP_CFG_MAIL_HOST`, `APP_CFG_MAIL_PORT` | SMTP-Ziel; Vorgabe `localhost:1025` |
| `APP_CFG_MAIL_USERNAME`, `APP_CFG_MAIL_PASSWORD` | SMTP-Anmeldedaten, falls benötigt |
| `APP_CFG_MAIL_SMTP_AUTH`, `APP_CFG_MAIL_SMTP_STARTTLS_ENABLE`, `APP_CFG_MAIL_SMTP_SSL_ENABLE`, `APP_CFG_MAIL_SMTP_SSL_TRUST`, `APP_CFG_MAIL_DEBUG` | SMTP-Authentifizierung, Transport- und Diagnoseeinstellungen |

Sicheres Beispiel für einen **lokalen Test-Mailserver ohne Authentifizierung**, als PowerShell-Umgebungsvariablen (der Server muss separat laufen):

```powershell
$env:APP_CFG_MAIL_HOST = 'localhost'
$env:APP_CFG_MAIL_PORT = '1025'
$env:APP_CFG_MAIL_SMTP_AUTH = 'false'
$env:APP_SECURITY_EXPOSE_H2_CONSOLE = 'false'
```

**Zwei Konfigurationsebenen beachten:** `DatabaseInitializer` übernimmt `app.config.defaults.<Schlüssel>` nur für noch fehlende Datenbankeinträge. Bereits gespeicherte Werte werden durch geänderte `APP_CFG_*`-Vorgaben nicht überschrieben. Die Schlüssel definiert `model/enums/ConfigEntry.java`; Änderungen vorhandener Werte sind im Adminbereich möglich. `service.name` bestimmt den Anzeigenamen, `base.url` die Basis für Mail-Links und muss zur erreichbaren URL passen. Für die Erstbefüllung ist beispielsweise `app.config.defaults.base.url=http://localhost:8080/` möglich.

SMTP-Sender und Remember-me-Dienst lesen ihre Einstellungen bei der Bean-Erstellung. Änderungen daran erfordern einen Neustart; bei einer frischen Datenbank erfolgt die Erstbefüllung erst danach durch einen `CommandLineRunner`. Die Wirksamkeit der Vorgaben beim allerersten Start ist deshalb noch zu prüfen (`config/MailConfig.java`, `security/SecurityConfig.java`, `config/DatabaseInitializer.java`).

H2 speichert standardmäßig unter `data/appdb`; Hibernate aktualisiert das Schema automatisch (`ddl-auto=update`). Logs landen unter `logs/` (`src/main/resources/logback-spring.xml`). Ein Start kann Datenbank, Logs und Frontend-Artefakte anlegen oder verändern. Ohne aktivierten Admin-Bootstrap entsteht kein automatisches Adminkonto.

Für bestehende Datenbanken ist vor dem Deployment die [Anleitung zur Benutzer-Versionierung](docs/user-version-migration.md) zu beachten. Bei einer Neuinstallation mit leerer Datenbank ist keine Bestandsmigration erforderlich.
Zusätzlich müssen bei bestehenden Datenbanken die [E-Mail-Identität und Löschkaskaden](docs/user-identity-migration.md) vor dem Deployment migriert und geprüft werden; diese Bestandsmigration wird nicht automatisch ausgeführt.
Für die Absicherung von Verifikation und Adminsperren ist außerdem **nur bei bestehenden Datenbanken** die [Migration des Verifikationsstatus](docs/verification-state-migration.md) vor dem Deployment erforderlich; sie wird nicht automatisch ausgeführt. Bei einer Neuinstallation mit leerer Datenbank ist diese Bestandsmigration nicht erforderlich.

## Start und Build

Windows/PowerShell:

```powershell
# Shared-Abhängigkeit bereitstellen (erster Build / nach Shared-Änderungen)
.\mvnw.cmd -f shared/pom.xml clean install

# Entwicklungsstart
.\mvnw.cmd spring-boot:run

# JAR mit dem vorhandenen production-Profil bauen
.\mvnw.cmd clean package -Pproduction

# Gebautes JAR starten
java -jar target/app-1.0-SNAPSHOT.jar
```

Unter Linux/macOS entsprechend `./mvnw` statt `.\mvnw.cmd` verwenden. Alternativ ist `Application.java` der IDE-Einstiegspunkt. Standardadresse: `http://localhost:8080/`; Kontoseiten liegen unter `/login`, `/registration` und `/forgot-password`.

Geprüft wurden `clean test` und anschließend `package -Pproduction` mit dem Wrapper. Startbefehle wurden nicht ausgeführt; der tatsächliche Produktionsmodus im laufenden Server und der JAR-Start bleiben ungeprüft. Insbesondere ist `vaadin-dev` zusätzlich direkt als Abhängigkeit deklariert, während das Profil es nur an `vaadin-core` ausschließt.

## Mindestdauern für Verifikations- und Resettokens

| Einstellung | Mindestdauer |
| --- | --- |
| `VERIFICATION_TOKEN_VALID_DURATION` | 1 Minute |
| `PASSWORD_RESET_TOKEN_VALID_DURATION` | 1 Minute |
| `VERIFICATION_TOKEN_LIVE_DURATION` | 2 Minuten nach Ablauf |
| `PASSWORD_RESET_TOKEN_LIVE_DURATION` | 2 Minuten nach Ablauf |

Die Konstanten liegen in der gemeinsamen `TokenDurationPolicy`; `ConfigEntry`
ordnet sie den vier Einstellungen zu. Adminformular und Tokendienste prüfen dieselben
Grenzen. Das Formular akzeptiert beispielsweise `1m`/`2m` und speichert ISO-8601
(`PT1M`/`PT2M`). Ungültige Syntax, Null-, negative und zu kurze Dauern verhindern
das Speichern; Hinweise und Erklärungen nennen die Mindestdauer auf Deutsch/Englisch.
Die Dienste lehnen unzulässige gespeicherte Werte vor Tokenänderung bzw. Bereinigung ab.
Aufbewahrung verlängert keinen Link; Null bedeutet nicht unbegrenzt.

Vor Übernahme dieses Stands vorhandene Konfigurationswerte unter den Mindestdauern
anpassen. Sie werden nicht still aufgerundet. Bereits ausgegebene Tokens behalten
ihre gespeicherten Ablaufdaten; für diese Mindestdaueränderung ist keine Schemaänderung nötig.

Prüfung dieses Fixes am 12.10.2026: 1007 Anwendungstests, 21 Browser-Helper-/Reportertests
und die vier neuen Token-Konfigurationsfälle jeweils auf DE und EN erfolgreich.
Das ist kein erfolgreicher Gesamtnachweis aller Browserfälle; die unabhängig davon
offenen Punkte stehen in der Browserdokumentation.

## Tests und Prüfstand

Unter `src/test/java/` liegen Unit- und serverseitige Regressionstests für Benutzerverwaltung, Eingaberegeln, Registrierung, Konfiguration, Benachrichtigungen, Session-/Remember-me-/Push-RPC-Sicherheit sowie Status und Nebenläufigkeit. Persistenztests verwenden isolierte H2-In-Memory-Datenbanken, nicht die Anwendungsdatenbank. Vaadin-Tests prüfen serverseitige Komponenten und Callbacks mit kontrollierten UI-/Sessionfixtures, ohne Browser oder Anwendungsstart. `ImageHelperTest` prüft zusätzlich das echte Mail-Logo als Classpath-Ressource. `src/test/resources/logback-test.xml` konfiguriert das Test-Logging.

Am 07.10.2026 unter Windows tatsächlich ausgeführt:

| Befehl | Ergebnis |
| --- | --- |
| `.\mvnw.cmd --version` | Maven **3.9.16**, Java **25.0.4.1**, Eclipse Adoptium |
| `.\mvnw.cmd clean test` | **BUILD SUCCESS**, **365 Tests, 0 Fehler, 0 Fehlschläge, 0 übersprungen** |
| `.\mvnw.cmd jacoco:report` | **BUILD SUCCESS**, Coveragebericht erzeugt |

Öffentliche Servicevoraussetzungen und Exceptions: [UserService-Verträge](docs/user-service-contract.md).

Historischer Prüfstand vom 01.10.2026 (Package/Node/JAR seitdem nicht erneut geprüft):

| Befehl | Ergebnis |
| --- | --- |
| `.\mvnw.cmd --version` | Maven **3.9.16**, Java **25.0.4.1**, Eclipse Adoptium/Temurin, `JAVA_HOME=C:\Dev\Java\current` |
| `node --version` | **v24.21.0**, erfüllt die Node.js-Voraussetzung |
| `.\mvnw.cmd clean test` | **BUILD SUCCESS**; kompiliert mit `release 25`; **1 Test, 0 Fehler, 0 Fehlschläge, 0 übersprungen** |
| `.\mvnw.cmd package -Pproduction` | **BUILD SUCCESS**; derselbe Test erneut erfolgreich; Spring-Boot-JAR erstellt |
| `& "$env:JAVA_HOME\bin\jar.exe" tf target/app-1.0-SNAPSHOT.jar` | Enthält `META-INF/resources/custom-theme/service_logo.png` |

Beide Builds meldeten, dass kein neuer Produktions-Frontend-Bundle-Build nötig war; ein vollständiger Neuaufbau dieses Bundles ist damit nicht geprüft. Es gab JDK-Warnungen zu Jansi-Native-Access und beim Kompilieren zu Lomboks Nutzung von `sun.misc.Unsafe`, aber keine Build-Fehler.

`VerificationService` und `PasswordResetService` laden das Logo nun über `ImageHelper` als **Classpath-Stream**, ohne lokalen Dateipfad oder Dateiextraktion. Das bisherige Mailformat bleibt erhalten: Der Helper liefert reines Base64, die HTML-Vorlagen ergänzen `data:image/png;base64,`. Der Test prüft die Ressource im Test-Classpath; `jar tf` prüft deren Verpackung, nicht den Ladevorgang im laufenden JAR.

**Die erfolgreichen serverseitigen Regressionen sind kein vollständiger End-to-End-Funktionstest.** Start, Browserdarstellung, Login im Browser, echte Netzwerk-WebSockets, Mailversand/-darstellung und Bestandsmigrationen bleiben ungeprüft. `.\mvnw.cmd verify -Pit,production` würde die vorbereitete Failsafe-Konfiguration aktivieren und die Anwendung starten/stoppen; dieser Befehl wurde nicht ausgeführt. Es gibt keine explizite Browser-/WebDriver-Konfiguration im Profil.

## Styles, Bilder und generierte Dateien

- **Styles und UI-Bilder:** `src/main/resources/META-INF/resources/custom-theme/`, insbesondere `styles.css`; eingebunden in `Application.java` über `custom-theme/styles.css`. Weitere Icons: `src/main/resources/META-INF/resources/icons/`.
- **Übersetzungen:** `src/main/resources/i18n/messages_de.properties` und `messages_en.properties`.
- **Mailvorlagen:** `src/main/resources/email/`; die Dienste laden HTML-Dateien. MJML-Quelldateien liegen daneben, eine automatische MJML-Kompilierung ist in `pom.xml` nicht eingerichtet.
- **Generierte Dateien:** `src/main/frontend/generated/`, `vite.generated.ts` und Build-Ausgaben unter `target/`; nicht manuell pflegen. `node_modules/` enthält Frontend-Abhängigkeiten. Diese Pfade sind in `.gitignore` ausgeschlossen. Eigene Vite-Anpassungen gehören in `vite.config.ts`.
