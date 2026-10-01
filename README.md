# VaadinSpringApp

Java-Webanwendung als Grundlage für Anwendungen mit Benutzerkonten und Administration. Die Oberfläche wird serverseitig mit Vaadin Flow aufgebaut; ein weitergehender fachlicher Anwendungsbereich ist bisher nicht implementiert.

> Diese Beschreibung basiert auf dem Quellcode und der Build-Konfiguration. Für ihre Erstellung wurden weder Anwendung noch Build oder Tests ausgeführt. Beschriebene Abläufe sind damit kein Nachweis eines erfolgreichen Laufzeitbetriebs.

## Funktionen

- Registrierung mit zunächst deaktiviertem Konto und Aktivierung per E-Mail-Token.
- Anmeldung per E-Mail und Passwort, Abmeldung, persistentes „Remember me“ und BCrypt-Passwort-Hashing.
- Passwort-Zurücksetzen über zeitlich begrenzte, nach Verwendung verbrauchte Tokens.
- Adminbereich mit Benutzerverwaltung, Rollen und Kontofreigabe, Konfiguration sowie Mail-Testversand.
- Deutsche und englische Oberfläche, manuell wählbarer Benutzerstatus und automatische Abwesenheitserkennung mit Push-Aktualisierung.
- Datenbankgestützte E-Mail-Warteschlange mit Wiederholungsversuchen und geplanten Bereinigungsaufgaben.

Belege: `views/RegistrationView.java`, `views/LoginView.java`, `views/admin/`, `service/VerificationService.java`, `service/PasswordResetService.java`, `service/EmailQueueService.java`, `security/SecurityConfig.java` und `scheduler/UserStatusScheduler.java` unter `src/main/java/de/derpeterson/app/`.

## Technologien und Voraussetzungen

- **JDK 25**, **Spring Boot 4.1.1**, **Vaadin 25.3.0**, Lombok; Versionen und Abhängigkeiten stehen in `pom.xml`.
- Spring Security, Spring Data JPA/Hibernate, dateibasierte H2-Datenbank und Spring Mail/SMTP.
- Maven Wrapper: `mvnw` bzw. `mvnw.cmd`, konfiguriert für **Maven 3.8.4** in `.mvn/wrapper/maven-wrapper.properties`. Unter Windows muss `JAVA_HOME` auf das JDK zeigen. Die Kompatibilität dieser Wrapper-Version mit dem aktuellen Stack wurde nicht ausgeführt/geprüft.
- Frontend-Werkzeuge: Node.js/npm, Vite und TypeScript. Das Vaadin-Maven-Plugin bindet `prepare-frontend` und `build-frontend` an die Compile-Phase; Anpassungen stehen in `vite.config.ts`. Eine konkrete Node-Mindestversion ist im Projekt nicht festgelegt, die passende Toolchain muss beim ersten Build geprüft werden.
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

## Start und Build

Windows/PowerShell:

```powershell
# Entwicklungsstart
.\mvnw.cmd spring-boot:run

# JAR mit dem vorhandenen production-Profil bauen
.\mvnw.cmd clean package -Pproduction

# Gebautes JAR starten
java -jar target/app-1.0-SNAPSHOT.jar
```

Unter Linux/macOS entsprechend `./mvnw` statt `.\mvnw.cmd` verwenden. Alternativ ist `Application.java` der IDE-Einstiegspunkt. Standardadresse: `http://localhost:8080/`; Kontoseiten liegen unter `/login`, `/registration` und `/forgot-password`.

Die Befehle sind aus `pom.xml` und Wrapper abgeleitet, **nicht ausgeführt**. Das `production`-Profil ist vorhanden; der tatsächliche Produktionsmodus und JAR-Start sind ungeprüft. Insbesondere ist `vaadin-dev` zusätzlich direkt als Abhängigkeit deklariert, während das Profil es nur an `vaadin-core` ausschließt.

## Tests und Prüfstand

Unter `src/test/` liegt derzeit ausschließlich `resources/logback-test.xml`, **keine Testklasse**. `pom.xml` enthält Spring-Boot-Test- und Vaadin-TestBench-JUnit-5-Abhängigkeiten, aber damit noch keine automatisierte Funktionsabdeckung.

- `.\mvnw.cmd test`: durchläuft die Testphase, kann derzeit jedoch keine vorhandenen Testklassen ausführen; der Frontend-Build ist bereits an `compile` gebunden.
- `.\mvnw.cmd verify -Pit,production`: aktiviert die vorbereitete Failsafe-Konfiguration und startet/stoppt dabei die Anwendung. Es gibt derzeit keine Integrationstestklassen und keine explizite Browser-/WebDriver-Konfiguration im Profil.

**Ein erfolgreicher Build ohne Testklassen ist kein bestandener Funktionstest.** Build, Start, Browserdarstellung, Anmeldung, Rollenprüfung, Mailversand und Datenbankbetrieb wurden für diese Dokumentation nicht praktisch überprüft.

Ein konkreter offener Punkt ist bereits im Code sichtbar: `VerificationService` und `PasswordResetService` laden das Mail-Logo über `ImageHelper` noch vom alten Dateisystempfad `src/main/frontend/themes/custom-theme/service_logo.png`. Die Datei liegt inzwischen unter den unten genannten Ressourcen. Damit ist die Mail-Erstellung insbesondere auch beim JAR-Betrieb zu prüfen.

## Styles, Bilder und generierte Dateien

- **Styles und UI-Bilder:** `src/main/resources/META-INF/resources/custom-theme/`, insbesondere `styles.css`; eingebunden in `Application.java` über `custom-theme/styles.css`. Weitere Icons: `src/main/resources/META-INF/resources/icons/`.
- **Übersetzungen:** `src/main/resources/i18n/messages_de.properties` und `messages_en.properties`.
- **Mailvorlagen:** `src/main/resources/email/`; die Dienste laden HTML-Dateien. MJML-Quelldateien liegen daneben, eine automatische MJML-Kompilierung ist in `pom.xml` nicht eingerichtet.
- **Generierte Dateien:** `src/main/frontend/generated/`, `vite.generated.ts` und Build-Ausgaben unter `target/`; nicht manuell pflegen. `node_modules/` enthält Frontend-Abhängigkeiten. Diese Pfade sind in `.gitignore` ausgeschlossen. Eigene Vite-Anpassungen gehören in `vite.config.ts`.
