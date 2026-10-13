# VaadinSpringApp

Lokale Docker-Testumgebung mit Mailpit und H2: [Docker-Anleitung](docs/docker/README.md).

Automatisierte Browserprüfungen mit Java, JUnit und Allure in eigener Docker-Testumgebung; getrennte Suites für Infrastruktur, Anmeldung und Zugriffsrechte sowie Registrierung, Verifikation und Passwortreset über echte Testmails, Einzelvalidierung, Sprachpräferenz und grundlegende Bedienung: [Browser-Test-Anleitung](browser-tests/README.md).

Java-Webanwendung als Grundlage für Anwendungen mit Benutzerkonten und Administration. Die Oberfläche wird serverseitig mit Vaadin Flow aufgebaut; ein weitergehender fachlicher Anwendungsbereich ist bisher nicht implementiert.

> Vollständiger Gesamtprüfstand vom 13.10.2026 einschließlich Home-/Admin-Umstellung und Eingabeschrift: Java 25, 1.055 Anwendungstests mit JaCoCo sowie 26 Browser-Helper-/Reportertests erfolgreich. Das frisch gebaute Docker-Produktions-JAR wurde mit jeweils 87 Browserfällen auf Deutsch und Englisch in isoliertem H2/Mailpit geprüft. Umfang, Berichte und verbleibende Grenzen: [Browserabdeckung](browser-tests/abdeckung.md).

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

## Kontoansichten, Home und Admin: Silent Cosmos und Inter

Login, Registrierung, Verifikation, Passwortanfrage und Passwortänderung verwenden
denselben animierten CSS-Hintergrund und dieselben Card-/Formularbausteine.
Grundlage ist die bereitgestellte Silent-Cosmos-Referenz; Servicebezeichnung,
Vaadin-Komponenten, Übersetzungen, Validierung und Authentifizierung bleiben erhalten.
Die Glas-Card verwendet den gewünschten RGBA-Verlauf der Vorlage; Primary-Buttons
den zentralen blauen Verlauf `#3a538d` → `#526ebc`.

| Zentrale Datei unter `custom-theme/` | Aufgabe |
| --- | --- |
| `typography.css` | Inter als Standardschrift für die gesamte Anwendung |
| `cosmos-theme.css` | Gemeinsame Palette, Lumo-Farbvariablen, Cards, Formulare, App-Layout und markierte Overlays |
| `cosmos-background.css` | CSS-Sterne, Nebel, Planeten und Animation |
| `styles.css` | Nur in AdminView geladen; Tabellen, Filter, Pagination und Löschdialog-Akzente |

Typografie, Cosmos-Theme und Hintergrund werden einmalig in `Application.java`
eingebunden. `styles.css` wird ausschließlich an `AdminView` geladen; seine allgemeinen
Selektoren sind auf Admin begrenzt und wirken auch nach einer Navigation nicht auf Home. Die Cosmos-Palette
gilt für `.cosmos-theme` und die aus diesen Ansichten geöffneten Notifications;
`HomeView` und `AdminView` verwenden dieselbe Palette, Topbar und Avatar-Popup.
**Inter gilt für die gesamte Anwendung.**
Aus dem bereitgestellten Inter-ZIP werden beide vollständigen variablen Fonts lokal
geladen: normal und kursiv, alle Gewichte von 100 bis 900 einschließlich optischer
Größen. Es sind keine externen Fontdienste nötig. Die statischen Einzelschnitte
aus dem ZIP werden durch diese beiden variablen Dateien abgedeckt.
Lizenz: `custom-theme/fonts/Inter-LICENSE.txt` (SIL Open Font License 1.1).

`CosmosBackgroundComponent` und `CosmosCopyrightComponent` werden von den Kontoansichten
und der Startseite gemeinsam verwendet. `CosmosPageComponent` ordnet Marke, Cards und Fußzeile an;
`CosmosCardComponent` stellt den gemeinsamen Card-Rahmen bereit;
`CosmosSectionHeaderComponent` die übersetzten Admin-Bereichsüberschriften. Registrierung
nutzt eine breitere Card; Feldpaare stehen unter 600 px untereinander. Die Card selbst
steht bei ausreichend Platz exakt in der Bildschirmmitte. Logo/Servicebezeichnung
stehen darüber, Copyright darunter; ausgeglichene Grid-Zeilen verhindern ein
Verschieben durch die unterschiedlichen Höhen. Bei wenig
Höhe wächst die Seite und scrollt vertikal. Reduzierte Bewegung deaktiviert die
Animation. Platzhalter folgen über die gemeinsamen Getter der gewählten Sprache.

Unter **Admin → Konfiguration → Allgemein** kann der Copyright Text geändert
werden. Datenbankschlüssel: `copyright.text`; Default: `© {year} {serviceName}`.
Der unveränderte Standard wird über den gemeinsamen Übersetzungsgetter angezeigt:
DE `© {year} {serviceName}. Alle Rechte vorbehalten.`,
EN `© {year} {serviceName}. All rights reserved.`.
Eigene gespeicherte Texte werden unverändert verwendet. Beim Sprachwechsel wird
der Standardtext aktualisiert; dafür werden keine Datenbankwerte überschrieben.
`{year}` wird durch das aktuelle lokale Jahr, `{serviceName}` durch den konfigurierten
Servicenamen ersetzt. Leerer Text blendet die Fußzeile aus; HTML bleibt sicher Text.
Änderungen erscheinen beim erneuten Öffnen der betreffenden Ansichten und in neu erzeugten E-Mails. Beim normalen Start
wird nur ein fehlender Eintrag ergänzt; gespeicherte Werte werden nicht überschrieben.

Frühere Hintergrundbilder, Welcome-Bild und alte Login-Styles wurden aus den
aktiven Ressourcen entfernt. Die vorher versionierten Dateien bleiben über die
Git-Historie nachvollziehbar. Der ursprünglich angelegte lokale Ordner
`design-backup/custom-theme/` ist im aktuellen Arbeitsbaum nicht mehr vorhanden.
Service-Logo, Avatar und Sprachflaggen bleiben aktiv. Gezielt prüfen:
`browser-tests/run.ps1 -Suite cosmos-design -Language de` (danach EN mit `-NoBuild`).
Service-Logo (96×96), Servicename (Lumo XL), Platzhalter und Statuscards wurden
abschließend im vollständigen DE-/EN-Lauf geprüft: je 11 Cosmos- und 6 Login-Layoutfälle
innerhalb der jeweils 87 Browserfälle. Den aktuellen Nachweis dokumentiert [Browserabdeckung](browser-tests/abdeckung.md).

### Startseite nach der Dashboard-Vorschau

`HomeView` verwendet dieselbe zentrale Palette und den animierten Hintergrund.
Die Kopfzeile zeigt Service-Logo/-Name und das bestehende Benutzermenü. Verlauf,
Rahmen, Schatten und Abstände entsprechen der Dashboard-Vorschau und stehen zentral
in `cosmos-theme.css`: `cosmos-app-shell` markiert das AppLayout, `cosmos-topbar`
die Leiste; Brand, Logo, Servicename und Kontomenü haben gemeinsame `cosmos-topbar-*`
Klassen. Diese Bausteine werden von Home und Admin gemeinsam verwendet. Die Leiste
ist regulär 68 px hoch, mit 20/24 px seitlichem Abstand und unter 750 px mit 11 px.
Die bestehenden Logo-/Avatar-Ressourcen bleiben erhalten. Das Avatar-Popup verwendet
denselben Card-Verlauf, Rahmen und Schatten, zentrale Cosmos-Buttons sowie eine passende
Statusauswahl. Menüabstände und schmale Fenster werden über `cosmos-user-menu` im Theme
gesteuert; Kontofunktionen, Statuswechsel und Sprachwahl bleiben erhalten.
Die Willkommen-Card wurde entfernt. Anmeldung, Registrierung und der Zugang zur
Administration bleiben über das Benutzermenü erreichbar. Authentifizierung, Status,
Sprachwahl und Sicherheitsnavigation bleiben erhalten. Die Adminansicht verwendet
inzwischen dieselben gemeinsamen Bausteine; ihr eigener Aufbau steht im folgenden Abschnitt.
Die Startseite ist für schmale Fenster mit umbrechenden Inhalten gestaltet. Copyright
und Sprachwechsel verwenden dieselben gemeinsamen Bausteine wie die Kontoansichten.

**Geprüft:** Home ist im vollständigen DE-/EN-Lauf enthalten. UI-03F prüft reduzierte
Bewegung auch auf Home; COSMOS-04 die gemeinsame Palette und Notificationgestaltung.
COSMOS-05A–C prüfen Topbar, Avatar-Popup und Adminnavigation bei 390×844, 844×390 und
1280×900. Die entfernte Willkommen-Card wird nicht mehr vorausgesetzt.

### Admin im gemeinsamen Cosmos-Theme

Die Admin-Umstellung verwendet die Dashboard-HTML als gestalterische Referenz.
Topbar (68 px), Service-Logo, Avatar-Schimmer, Popup, Hintergrund und Copyright sind
dieselben Bausteine wie in Home. Die dunkle Seitenleiste verwendet kompakte Einträge,
eine hervorgehobene aktuelle Auswahl und die bestehenden Konfigurationsgruppen.
Auf schmalen Fenstern verwendet Vaadin seine aufklappbare Navigation; sie bleibt über
das Service-Logo erreichbar. Es werden keine vorgetäuschten Systemstatuswerte angezeigt.

Dashboard, Konfiguration, Mailtest und Benutzerverwaltung verwenden gemeinsame
Bereichsüberschriften und Cosmos-Cards. Formulare, Filter und Benutzer-/Informationsdialoge
verwenden dieselbe Palette und Eingabegestaltung. Tabelle, Pagination sowie die roten
Löschdialog-Akzente bleiben Admin-Spezialfälle in `styles.css`; dieses Stylesheet wird
weiterhin nur an AdminView geladen. Der gemeinsame Rest steht in `cosmos-theme.css`.
Formularpaare und Aktionsleisten können bei wenig Platz umbrechen.

Home und Dashboard verwenden denselben `CosmosSectionHeaderComponent` mit kleiner
Bereichszeile und blauem Akzentpunkt. Im Dashboard steht die Bereichszeile lokalisiert
als „Admin Center / Übersicht“ bzw. „Admin Center / Overview“. Seine Überschrift beginnt
links im Inhaltsbereich, die Kontoinformations-Card steht mittig darunter.
Die kompakte Card zeigt „Willkommen zurück“, die aktuelle E-Mail fett und jede
Berechtigung als eigenen Rollen-Badge; ihre Gestaltung bleibt im zentralen CSS.
Die Seitennavigation verwendet Vaadins native `SideNav`/`SideNavItem`-Hierarchie.
„Konfiguration“ ist ein Elternpunkt ohne Zielpfad mit `VaadinIcon.COGS`; Vaadin
übernimmt das Auf-/Zuklappen, den separaten Pfeil und die Tastaturbedienung.
Die Unterpunkte öffnen weiterhin die bestehenden Konfigurationsgruppen.
`ConfigGroupIcons` ordnet Cards und Unterpunkten gemeinsam dieselben Gruppenicons zu.
Die früheren Chevron-/Button-Sonderregeln sind entfernt. Das zentrale Theme
stellt den kompakten Look wieder her: Hauptpunkte 13 px/42 px, Unterpunkte 12 px/31 px,
Icons 20 px und dezente Flächen ausschließlich für die aktive Auswahl bzw. Hover.
Zielparameter werden über `SideNavItem.setQueryParameters` gesetzt, damit die
Einträge unterschiedliche Ziele und korrekt zugeordnete Panel-Aktionen behalten.
Der öffentliche `onNavigate`-Hook verbindet die Einträge mit den bestehenden
Admin-Panels, ohne die View neu zu laden. Labels und die Aufklapphilfe folgen DE/EN.
Die Browser-Navigation muss beim späteren gemeinsamen Testlauf angepasst werden.
Referenz: [Vaadin SideNav: Hierarchy](https://vaadin.com/docs/latest/components/side-nav#hierarchy).
Konfiguration nutzt denselben Kopfbereich und die bisherigen Schriftgrößen; sein
Breadcrumb zeigt Admin Center, Konfiguration und die ausgewählte Gruppe und wird
bei Gruppen- und Sprachwechsel aktualisiert. Die Konfigurations-Card zeigt
Gruppenicon, Kurzbeschreibung und tatsächliche Einstellungsanzahl; Trennlinien
begrenzen Kopf und Fuß mit Speicherhinweis und rechts angeordneten Aktionsbuttons.
Alle 29 Konfigurationsfelder verwenden denselben `ConfigInfoButton`: rechts am
Input, Lumo-Icongröße l, Primary-Farbe, manuelles Öffnen per Klick oder Tastatur.
Ein Außenklick schließt den Tooltip; die Steuerung wird beim Entfernen des Felds
aufgeräumt. Die Texte folgen beim Sprachwechsel der aktuellen Sprache.
Tooltips enthalten Beschreibung, Standardwert und die tatsächlich geprüften
Eingaberegeln als Aufzählung; Satzpunkte stehen in den DE-/EN-Message-Dateien.
Sie nutzen Card-Hintergrund, Border, Rundung und Schatten, dezente Schrift (12 px)
und die zentrale Breite `--cosmos-info-tooltip-width` (360 px, begrenzt durch den Viewport).

Mail Test verwendet denselben Breadcrumb-/Titelbereich und die gemeinsamen
Panel-Stile für Icon, Card-Kopf, Trennlinien und Footer. Die Card steht mittig
und bleibt bei kleinen Auflösungen flexibel. Die drei Pflichtfelder nutzen
denselben `ConfigInfoButton` rechts neben dem Input: Beschreibung und Eingaberegel
im Klick-Tooltip, mit Card-Stil und Schließen durch Außenklick. Die Texte werden
beim Sprachwechsel aktualisiert. Bisherige Feldvorgaben bleiben erhalten;
der Versandbutton steht rechts im Footer.
Der SMTP-Hinweis beschreibt die beim Start geladene Verbindung und deren
Neustartbedarf. Versand und Validierung bleiben unverändert. Diese Gestaltung
ist noch nicht durch einen neuen Anwendungsbuild oder Testlauf geprüft.

Standardwerte und Platzhalter kommen über `ConfigService.getDefaultValue` aus
`app.config.defaults.*`, mit dem Enumwert als Fallback, wie im Backend und bei der
Initialisierung. Aktuelle gespeicherte Eingaben bleiben davon getrennt. Leere
Zahlenfelder übernehmen beim Speichern diesen Standardwert; sichtbare Zahlgrenzen
folgen den vorhandenen Prüfungen. Checkboxen zeigen ihren Standardwert im Tooltip,
weil sie keinen Platzhalter besitzen. Geheime Standardwerte erscheinen ausschließlich
als „(vertraulich)“, leere Standards als „(leer)“; bei leeren Textstandards lautet der
Platzhalter „Leer lassen“. Gespeicherte Passwörter bleiben im vorhandenen Passwortfeld.

Die Regelhinweise wurden mit der bestehenden serverseitigen Prüfung abgeglichen:
Token-Gültigkeit mindestens eine Minute, Token-Aufbewahrung mindestens zwei Minuten,
Port 1–65535, positive Zähler bzw. nichtnegative Retry-Anzahl sowie vorhandene
URL-, E-Mail-, Host-, Trustlisten-, Geheimschlüssel- und Klassenname-Prüfungen.
Für automatische Abwesenheit existiert bisher keine Mindestdauer. Die
Mailqueue-Aufbewahrung muss wie im Backend strikt positiv sein; Null und negative
Werte werden bereits im Formular abgelehnt. Dafür gilt keine zusätzliche Minutengrenze. Zeitspannen
benötigen Einheit oder ISO-Format, während Remember-me weiterhin Sekunden verwendet.
Es wurden keine neuen fachlichen Mindestdauern eingeführt.
Die Erklärungen nennen den heutigen Einsatz: Copyright Text für Oberfläche und
E-Mail-Vorlagen, Basis-URL für neu erzeugte Mail-Links und die Neustartwirkung von
Remember-me-, SMTP- und Threadpool-Einstellungen. Queue-Kapazität beschreibt die
geladene Menge pro Durchlauf statt einer Obergrenze für alle gespeicherten Mails.
Die Bereinigung versendeter Queue-Einträge richtet sich nach ihrem Erstellungsdatum.
Dieser spätere Designfeinschliff ist noch nicht als Anwendung gebaut oder getestet; der oben genannte
Gesamtlauf dokumentiert den Stand davor.

Rollenprüfung, fachliche Prüfregeln, Filter, Mailaktionen und Kontoschutz bleiben
erhalten. Zahlenfeldgrenzen und Standardwertanzeigen sind an die vorhandenen
serverseitigen Regeln und Backend-Defaults angeglichen.
**Geprüft:** Produktionsbuild sowie vollständige DE-/EN-Browserläufe erfolgreich.
Die bisherigen funktionalen Konfigurations-, Mail-, Status-, Benutzer- und Sicherheitsfälle
bleiben erhalten. COSMOS-05A–C ergänzen reale Navigation und Benutzerformular bei drei
Fenstergrößen. Das Aussehen ist ein Zwischenstand; gestalterischer Feinschliff und echte
Mobilgeräte bleiben gesonderte menschliche Prüfungen.

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

Geprüft wurden `clean test jacoco:report` mit dem Wrapper und ein frischer Produktions-Frontend-/JAR-Build im Docker-Build. Das verpackte JAR wurde anschließend in der isolierten Browser-Testumgebung gestartet. Ein separater aktueller Start außerhalb Docker wurde nicht geprüft. `vaadin-dev` ist zusätzlich direkt als Abhängigkeit deklariert, während das Profil es nur an `vaadin-core` ausschließt.

## Benutzerverwaltung: Filter und Seiten

Die sofort ausgewertete Suche berücksichtigt nur Name und E-Mail. Über „Filter hinzufügen“ / „Add Filter“ werden benötigte Filter unter der Suche eingeblendet.
Entfernen setzt ihre Werte zurück. Einzeln löschbare Filter für Freigabe, Status, eine Rolle und Aktivität seit
Datum und optionaler Uhrzeit werden gemeinsam angewendet. Eine Rollenmitgliedschaft reicht aus,
auch bei mehreren Rollen. „Letzte Aktivität“ schließt den gewählten lokalen Zeitpunkt
ein; ohne Uhrzeit gilt der Tag ab 00:00 Uhr. Ohne gespeicherte Aktivität gibt es
bei aktivem Zeitfilter keinen Treffer.
Datum und Uhrzeit öffnen ihre Auswahllisten nur über die Icons oder vorgesehenen
Pfeiltasten. Kurze DE-/EN-Beispiele stammen aus dem gemeinsamen Übersetzungskatalog;
„Uhrzeit optional“ steht klein neben der Filterüberschrift. Das Geburtstagsfeld im
Anlegen-/Bearbeiten-Dialog hat dieselbe Bedienung; Sprachwechsel erhalten den Wert.

Beide Zeitfelder leeren hebt die Zeitbedingung auf. Freigabe und Präsenzstatus
sind unterschiedliche Kriterien.

Suche, „Add Filter“ und „Add User“ stehen oben auf einer gemeinsamen Höhe. Gewählte
Filter laufen darunter nach rechts und umbrechen bei Platzmangel. Der Tabellenkopf
ist blau abgesetzt, Zeilen wechseln dezent die Hintergrundfarbe. Die Zeitvorschläge
verwenden 15-Minuten-Schritte, damit Vaadin die Auswahlliste anbietet. Unter der Tabelle
stehen Zeilen pro Seite, Trefferzahl und nummerierte Seiten mit erster/vorheriger/
nächster/letzter Seite und Auslassungspunkten bei vielen Seiten. Filter- und
Seitengrößenänderungen beginnen auf Seite 1. Sortierung erfolgt vor der Seitenteilung.
`AdminUserFilters` und `AdminUserPagination` halten diese Logik getrennt vom CRUD-Formular.
Die Filterung bleibt wie bisher im Speicher über die geladene Benutzerliste;
keine neue Datenbankabfrage oder Schemaänderung.

Bedienung der Tests: [Browser-Testanleitung](browser-tests/README.md).
Aktuelle Ergebnisse, Abdeckung und verbleibende Prüfungen:
[Browserabdeckung und offene Punkte](browser-tests/abdeckung.md).
Die freigegebenen CSS-Farben und Abstände wurden durch die Nachprüfung nicht verändert.

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

Aktueller Prüfstand am 12.10.2026 unter Windows:

| Prüfung | Ergebnis |
| --- | --- |
| `.\mvnw.cmd clean test jacoco:report` | Java 25, 1.036 Tests, keine Fehler/Fehlschläge/Skips; Coveragebericht erzeugt |
| Browser-Helper/Reporter | 24/24 erfolgreich |
| Vollständige Browserläufe DE und EN | jeweils 70/70 erfolgreich; isoliertes Docker/H2/Mailpit |

Der eigene Vaadin-Login speichert seinen Sicherheitskontext ausdrücklich in der Sitzung,
bevor Remember-me ausgegeben wird. Die Browserprüfung verwendet echte separate
Chromium-Prozessneustarts mit einem eigenen temporären Profil; Cookie- und
Storage-Zustände werden nicht künstlich eingesetzt.

Aktuelle Einzelbelege und verbleibende Journeys stehen in [Browserabdeckung](browser-tests/abdeckung.md).

Historischer Prüfstand vom 07.10.2026:

| Befehl | Ergebnis |
| --- | --- |
| `.\mvnw.cmd --version` | Maven **3.9.16**, Java **25.0.4.1**, Eclipse Adoptium |
| `.\mvnw.cmd clean test` | **BUILD SUCCESS**, **365 Tests, 0 Fehler, 0 Fehlschläge, 0 übersprungen** |
| `.\mvnw.cmd jacoco:report` | **BUILD SUCCESS**, Coveragebericht erzeugt |

Öffentliche Servicevoraussetzungen und Exceptions: [UserService-Verträge](docs/user-service-contract.md).

Historischer Prüfstand vom 01.10.2026:

| Befehl | Ergebnis |
| --- | --- |
| `.\mvnw.cmd --version` | Maven **3.9.16**, Java **25.0.4.1**, Eclipse Adoptium/Temurin, `JAVA_HOME=C:\Dev\Java\current` |
| `node --version` | **v24.21.0**, erfüllt die Node.js-Voraussetzung |
| `.\mvnw.cmd clean test` | **BUILD SUCCESS**; kompiliert mit `release 25`; **1 Test, 0 Fehler, 0 Fehlschläge, 0 übersprungen** |
| `.\mvnw.cmd package -Pproduction` | **BUILD SUCCESS**; derselbe Test erneut erfolgreich; Spring-Boot-JAR erstellt |
| `& "$env:JAVA_HOME\bin\jar.exe" tf target/app-1.0-SNAPSHOT.jar` | Enthält `META-INF/resources/custom-theme/service_logo.png` |

Die beiden historischen Builds vom 01.10.2026 meldeten, dass kein neuer Produktions-Frontend-Bundle-Build nötig war. Der aktuelle Docker-Build vom 12.10.2026 hat das Produktionsfrontend neu gebaut. Es gab JDK-Warnungen zu Jansi-Native-Access und beim Kompilieren zu Lomboks Nutzung von `sun.misc.Unsafe`, aber keine Build-Fehler.

Die Logo-Ressource wird weiterhin als **Classpath-Stream** geladen und funktioniert
auch ohne lokalen Quelldateipfad. Neu erzeugte Verifikations- und Resetmails verwenden
`cid:service-logo@vaadinspringapp` statt einer Data-URI. `EmailService` ergänzt dafür
einen Inline-PNG-Teil in `multipart/related`; anderer HTML-Inhalt bleibt ein einzelner
HTML-Teil. `EmailLogo` verwaltet CID und Ressourcenpfad zentral. Die Ressource wird
vor der Einreihung geprüft und vor dem Transport als Bytes gelesen; Lesefehler dürfen
keinen Versand beginnen. Die bestehende Behandlung nachweislich unversandter und
unklarer Transportausgänge bleibt erhalten.

Die DE-/EN-Mailvorlagen übernehmen die Cosmos-Farben mit einer statischen Stern-/Nebel-
Variante. Feste Grundfarben bleiben als Rückfall für Mailprogramme ohne CSS-Verläufe.
Inter/Roboto verwenden dort Arial/sans-serif als Rückfall; es werden keine externen
Schriften nachgeladen. MJML ist die Quelle; die vier danebenliegenden HTML-Dateien
wurden mit MJML 5.4.1 erzeugt. Für Änderungen jeweils aus dem Projektroot ausführen:

```powershell
npx --yes --package mjml@5.4.1 mjml src/main/resources/email/welcome_de.mjml -o src/main/resources/email/welcome_de.html --config.validationLevel strict
```

Entsprechend für `welcome_en`, `reset_password_de` und `reset_password_en` wiederholen.
MJML ist keine Anwendungsabhängigkeit und wird nicht durch Maven ausgeführt.

`{{COPYRIGHT_TEXT}}` wird beim Erzeugen der Queue-Nachricht aus `copyright.text`
eingesetzt. `CopyrightTextFormatter` löst für Oberfläche und Mails gemeinsam Jahr,
Servicenamen und den sprachabhängigen Standard auf. Eigene DB-Texte bleiben erhalten;
Mailtexte werden HTML-escaped. Bereits gespeicherte Queue-Nachrichten werden nicht
nachträglich umgeschrieben.

**Aktueller Feinschliff geprüft:** CID-MIME, Mailvorlagen und Konto-Styles wurden
abschließend gebaut und getestet. Gezielte MIME-Roundtrips prüfen CID/PNG-Bytes,
UTF-8 und Logo-Lesefehler vor Transport. DE-/EN-Browserjourneys bestätigen geladene
Inline-Bilder und den gespeicherten Copyrighttext über regulär versandte Testmails.
Gmail/Outlook-Darstellung bleibt separat zu prüfen. Nachweise: [Browserabdeckung](browser-tests/abdeckung.md).

**Die erfolgreichen Tests belegen die beschriebenen Fälle, keine vollständige Prüfung aller Betriebsabläufe.** Die Browserläufe belegen Start, Darstellung, Login und ausgewählte Mailjourneys mit lokaler SMTP-Zustellung an Mailpit. Reale Geräte, externe Maildarstellung/-zustellung, zusätzliche Transport-/Betriebsvarianten und Bestandsmigrationen bleiben offen (siehe Abdeckungsübersicht). `.\mvnw.cmd verify -Pit,production` würde die vorbereitete Failsafe-Konfiguration aktivieren und die Anwendung starten/stoppen; dieser Befehl wurde nicht ausgeführt. Es gibt keine explizite Browser-/WebDriver-Konfiguration im Profil.

## Styles, Bilder und generierte Dateien

- **Styles und UI-Bilder:** `src/main/resources/META-INF/resources/custom-theme/`, insbesondere `styles.css`; eingebunden in `Application.java` über `custom-theme/styles.css`. Weitere Icons: `src/main/resources/META-INF/resources/icons/`.
- **Übersetzungen:** `src/main/resources/i18n/messages_de.properties` und `messages_en.properties`.
- **Mailvorlagen:** `src/main/resources/email/`; die Dienste laden HTML-Dateien. MJML-Quelldateien liegen daneben, eine automatische MJML-Kompilierung ist in `pom.xml` nicht eingerichtet.
- **Generierte Dateien:** `src/main/frontend/generated/`, `vite.generated.ts` und Build-Ausgaben unter `target/`; nicht manuell pflegen. `node_modules/` enthält Frontend-Abhängigkeiten. Diese Pfade sind in `.gitignore` ausgeschlossen. Eigene Vite-Anpassungen gehören in `vite.config.ts`.

## Aktuelle Styles beim Start aus IntelliJ

Im Entwicklungsmodus ist Vaadins Frontend-Livebetrieb aktiviert (vaadin.frontend.hotdeploy=true).
Dadurch wird das vorberechnete Entwicklungspaket umgangen; Vite verarbeitet aktuelle Frontenddateien.
Docker setzt diese Option auf false und verwendet weiterhin den gebauten Produktionsstand.
Nach der erstmaligen Umstellung die IntelliJ-Anwendung einmal stoppen und erneut starten;
anschließend die Browserseite mit Strg+F5 vollständig neu laden. Der erste Start kann das Frontend vorbereiten.
Es wurden für diese Einstellung keine Tests ausgeführt; die Übernahme durch den IntelliJ-Neustart steht noch aus.


Zusätzlich kann ein vorheriger Produktionsbuild komprimierte Kopien von styles.css
(styles.css.gz / styles.css.br) im Klassen-Ausgabeordner hinterlassen. Werden anschließend
über die IDE nur die unkomprimierten Ressourcen aktualisiert, können diese Kopien veraltet bleiben.
Am 12.10.2026 wurden die beiden veralteten Ausgabedateien gezielt über Maven entfernt;
der laufende IDE-Server liefert bei einem frischen CSS-Abruf wieder die neue Filterleiste
und den neuen Tabellenkopf. Ein Neustart beseitigt gegebenenfalls noch zwischengespeicherte Ressourcen.
Das ist getrennt von der Livebetrieb-Einstellung zu betrachten; dafür ist kein neuer Run-Parameter nötig.


Aktuelle Layoutrevision der Benutzerverwaltung (noch ohne neue Testläufe):
„Clear Filter“ setzt Suche und Filterwerte zurück und blendet die Zusatzfilter aus.
Der Button verwendet das dezente Error-Theme; „Add User“ verwendet dieselbe
Standardform wie der Speichern-Button der Konfiguration.
Die aktive Seite verwendet das gleiche Primary-Theme wie „Add User“. Die Tabelle
zeigt alle Zeilen der gewählten Seite ohne innere vertikale Scrollleiste und hält
mindestens Platz für zehn Zeilen vor; bei kleinen Fenstern scrollt die ganze Seite.
Die Aktionsspalte ist zentriert, Sortierindikatoren werden mit drei einfachen CSS-Regeln
an den rechten Rand mit 15 Pixel Abstand zur nächsten Spalte gestellt.
„Letzte Aktivität“ zeigt relative DE/EN-Texte (z. B. „vor 4 Stunden“); der Tooltip
enthält den exakten Zeitpunkt. Aktualisierung erfolgt beim Neuladen oder erneuten
Rendern der Tabelle, ohne zusätzlichen Timer. Der gleichnamige Filter bleibt
eine inklusive Untergrenze. Ohne optionale Uhrzeit gilt der Tag ab 00:00 Uhr;
ohne Datum ist keine Zeitfilterung aktiv. Freigabe und Status verwenden Lumo-Badgefarben;
auch die vorhandenen Statusicons verwenden nun diese semantischen Lumo-Farben.
Beim Namen steht das vorhandene Standard-Avatarbild. Benutzerdefinierte Bildspeicherung
oder Uploads sind noch nicht implementiert; dafür wurde das Benutzerschema nicht geändert.

Der Benutzerformular-Dialog verwendet denselben farbigen Kopf wie die Tabelle.
Beim Bearbeiten steht links unten „Benutzer löschen“ mit dezentem Error-Theme.
Der vorhandene Bestätigungsdialog und die Schutzregeln bleiben erhalten; das
Bearbeitungsformular wird erst nach erfolgreicher Löschung geschlossen.
Die Löschbestätigung verwendet normale Textfarben und den gleichen nativen
Dialogfuß wie das Bearbeitungsformular. Der Inhalt hat oben und unten denselben
Abstand. Filter-/Löschicons verwenden `--lumo-error-text-color`. Für die größeren
Flächen von Löschheader und Löschbutton hat der Nutzer die Mischung
`color-mix(in srgb, var(--lumo-error-color) 50%, var(--lumo-base-color))` gewählt.
Der rot gefüllte Löschbutton hat ein Papierkorb-Icon und die
Kontrastschrift eines Primary-Buttons. Name und E-Mail sind fett hervorgehoben;
das Löschverhalten ist unverändert.
Registrierung und Adminformular nutzen dieselben fachlichen UserInputRules über
ValidationHelper (Pflichttexte, E-Mail, Passwort, Geburtsdatum). Registrierung
prüft beim Absenden, das Adminformular über Binder. Bestätigungsfelder gehören
zur Registrierung; Rollen/Freigabe zur Administration. Nur beim Bearbeiten darf
das Passwort leer bleiben. Passwort-Hilfen und Fehlertexte leiten die Beschreibung
aus dem gemeinsamen Übersetzungstext der zentralen Regel ab.
