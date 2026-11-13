# Entwicklung

## Voraussetzungen und Aufbau

JDK 25 und der Maven Wrapper sind verbindlich. Die tatsächlichen Bibliotheksversionen
stehen in den Modul-POMs und der Root-`pom.xml`; Node-Abhängigkeiten in
`app/package-lock.json`. Nicht neben diesen
Dateien eine zweite Versionsliste pflegen. Docker Desktop verwendet Linux-Container.

Die Paketnamen beziehen sich auf `app/src/main/java/de/derpeterson/app/`:

| Paket | Zuständigkeit |
| --- | --- |
| `Application` (Basispaket) | Spring-Boot-Einstieg, App-Shell und zentrale Stylesheets |
| `development` | Expliziter IDE-HTTPS-Starter mit eigener Caddy-Prozessverwaltung; vom Anwendungs-JAR ausgeschlossen |
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
| `app/src/main/resources/i18n`, `email` | DE-/EN-Texte und Mailvorlagen |
| `app/src/main/resources/db` | Aktuelles H2-Anwendungs- und Session-Schema |
| `app/src/main/resources/META-INF/resources/{custom-theme,icons}` | Gemeinsames Theme und zusätzliche Icons |
| `app/src/test`, `browser-tests` | Technische Tests, echte Browserjourneys und deren Runner |
| `scripts/sonar` | Lokale Codex-/Sonar-Anbindung, geschützter Zugang und main-Scanner; keine Anwendungslogik |
| `scripts/coverage`, `coverage-report` | Gemeinsamer Offline-Prüfer und technisches JaCoCo-Berichtsmodul im optionalen Profil `coverage` |
| `.agents/skills` | Codex-Projektabläufe; Bedienung unter [Arbeiten mit Codex](codex-skills.md) |

Die Root-POM aggregiert alle drei Module. Shared und Browser-Tests erben die neutrale
Root-POM; `app` behält den Spring-Boot-Parent und dessen bestehende Buildvorgaben.
Die Aggregation erfordert keine gemeinsame Eltern-POM. Dadurch gelangen Boot-/Vaadin-
Vorgaben nicht in Shared oder Browser-Tests. Java 25 gilt für alle Module.
Shared-Quellen nicht als Quellordner in andere Module einhängen. Der Reactor baut
Shared vor seinen Verbrauchern; kein vorheriges manuelles Shared-Install nötig:

```powershell
.\mvnw.cmd --version
.\mvnw.cmd -pl app -am verify -Pproduction -Dvaadin.productionMode=true
```

Stilprüfungen laufen einmal zentral im Root mit `-N` über alle Modulquellen;
Anwendungsartefakte liegen unter `app/target/`. IDE-/HTTPS-Start, Docker-Build und
Browserstarter sind an die Modulstruktur angepasst. Gemeinsamer Java-Analyseumfang
und JUnit-Coverage sind konfiguriert. Der geschützte Sonar-Ablauf verwendet ebenfalls
den vollständigen Reactor. Gemeinsame Funktionsprüfung, lokale Main-Integration und
Servernachweis mit grünem Gate sind im [Prüfstand](status.md) belegt. Der Multi-Modul-
Umbau ist insgesamt abgenommen.

## Konfiguration und Datenbank

Spring lädt `application.properties` sowie optional die lokale, nicht versionierte
`.env`. `app.config.defaults.*` bzw. die vorgesehenen `APP_CFG_*`-Variablen liefern
Vorgaben für fehlende Datenbankeinträge. Gespeicherte Adminwerte haben Vorrang.
Alle allgemeinen Admin-Konfigurationen besitzen eine entsprechende `APP_CFG_*`-
Startvorgabe. Die `.env.example` gliedert sie nach Konfigurationsbereich und nennt
die jeweiligen Einheiten. Dauerwerte verwenden ISO 8601, beispielsweise `PT10M`
für zehn Minuten oder `P7D` für sieben Tage. Die Remember-me-Dauer und der
Sitzungs-Timeout werden in Sekunden angegeben. Die Aufbewahrung von
Benachrichtigungen wird in Tagen angegeben.
Der Servicename wird mit `APP_CFG_SERVICE_NAME=derPeter` in der `.env` vorgegeben.
Ohne diese Variable gilt `{ServiceName}`. Änderungen benötigen einen Neustart und
überschreiben keinen bereits gespeicherten Servicenamen.
`ConfigService` bewahrt Strings unverändert; typisierte Getter lehnen ungültige
Zahlen und Booleans ab. Die Admin-Konfiguration gilt ab der nächsten betroffenen
Aktion. SMTP-Einstellungen werden gemeinsam gespeichert und bei Verbindungsaufbau
gelesen. Die Workerzahl gilt ab dem nächsten Durchlauf der E-Mail-Queue. Der
Schlüssel für gespeicherte Anmeldungen benötigt einen Neustart.

### Zentraler Konfigurationskatalog

`ConfigurationCatalog` unter `app/.../config/` erfasst die fachlichen
Konfigurationsnamen, Bereiche, Speicherorte, Umgebungsvariablen und Geheimniskennzeichen.
Beschriftungen und Feldhilfen verwenden den vorhandenen typisierten Textkatalog.
Die allgemeine Konfiguration und die Anbieter-Feldhilfen nutzen diese Zuordnungen
direkt. Der Katalog enthält keine tatsächlichen Werte oder Zugangsdaten.

| Bereich | Speicherung und Startverhalten |
| --- | --- |
| Allgemeine Einzelwerte aus `ConfigEntry` | Fehlende Werte werden aus `APP_CFG_*` in `config` angelegt. |
| Google und Microsoft | Fehlende Anbieter werden aus `APP_EXTERNAL_LOGIN_*` in `external_providers` angelegt. Client-ID, Secret und Aktivierung werden gemeinsam geprüft und gespeichert. |
| Rollen- und Funktionsregeln für 2FA | Gemeinsamer Regelstand in `two_factor_policy`, verwaltet durch `TwoFactorPolicies`. Ohne gespeicherten Stand gelten dessen Standardregeln. |
| TOTP-Schlüssel, Anbieter-Verschlüsselungsschlüssel und Freigabe der Entwicklungsoptionen | Ausschließlich Startkonfiguration. Sie werden nicht als Adminwerte in der Datenbank abgelegt. |

Vorhandene Anbieterzeilen bleiben beim Neustart unverändert, einschließlich einer
gespeicherten Deaktivierung. Für eine erstmalige Aktivierung in der `.env`
`APP_EXTERNAL_LOGIN_GOOGLE_ENABLED=true` bzw.
`APP_EXTERNAL_LOGIN_MICROSOFT_ENABLED=true` zusammen mit vollständigen Zugangsdaten
setzen. Beide Schalter sind standardmäßig aus. Eine angeforderte Aktivierung ohne
vollständige oder mit formal ungültigen Zugangsdaten lässt den Start fehlschlagen.
Ein deaktivierter Anbieter darf bereits Zugangsdaten enthalten.

Der Katalog ist keine allgemeine Schreibschnittstelle. Anbieter und 2FA behalten
ihre Transaktionen, Revisionsprüfungen und Superadmin-/Faktorprüfungen. Technische
Startparameter wie Datenbankverbindung, HTTP-Port, H2-Konsole und Bootstrap bleiben
bei ihren zuständigen Spring-Konfigurationen. Die getrennten Verschlüsselungsschlüssel
für TOTP und Anmeldeanbieter bleiben außerhalb der Datenbank. Anbieter-Secrets
liegen ausschließlich als authentifiziert verschlüsselter Inhalt in
`external_providers.encrypted_client_secret`. Einrichtung und Grenzen stehen in
der [Anbieteranleitung](externe-anmeldung.md).

Das Anmeldeversuchsfenster und die Wartezeit für erneute E-Mail-Zustellungen
sind fachliche Werte und deshalb in der Admin-Oberfläche einstellbar.
Die Dauer einer bereits verhängten Anmeldesperre, technische Takte für
Hintergrundprüfungen und die Grenzen des 2FA-Protokolls bleiben im Code.
Die Bootstrap-Kontolisten und die Netz- und Datenbankparameter bleiben in der
Startkonfiguration, da sie den Aufbau der Anwendung steuern.

Bootstrap ist standardmäßig aus. Die optionalen Listen `app.bootstrap.superadmins`,
`admins`, `users` und `guests` legen explizite lokale Testkonten mit genau ihrer
vorgesehenen Rolle an. Alle Einträge werden vor der ersten Anlage geprüft; die
Anlage ist transaktional und überschreibt keine schon vorhandenen Konten.
Pro Eintrag steuert `email-verification-required` den E-Mail-Nachweis.
Standard `false`: Das Konto ist ohne Bestätigung nutzbar und als „Nicht geprüft“
gekennzeichnet. Bei `true` wird die erste Bestätigungsmail in derselben Transaktion
vorgemerkt. Bis zur Bestätigung entsteht kein vollständiger Portalzugang.
Der Anlageweg bleibt in beiden Fällen „Beim Anwendungsstart angelegt“.
Neustarts ändern weder bestehende Konten noch deren Nachweis und erzeugen keine
weiteren Initialmails. Ein Pflicht-Mailfehler rollt die gesamte Kontoanlage zurück.
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

### E-Mail-Bestätigung für Startkonten

`development-account=true` kennzeichnet ein Startkonto ausdrücklich als Entwicklungskonto.
Für solche Konten ist sämtlicher kontobezogener Mailversand ausgeschaltet. Deshalb
ist die Kombination mit `email-verification-required=true` ungültig und wird vor
der Kontoanlage abgewiesen. Die Beispielkonten setzen den Entwicklungsschalter
ausdrücklich. Für echte Konten gilt `development-account=false` (auch ohne Angabe).
Beide Eigenschaften werden nur bei der Anlage übernommen; bestehende Konten
werden nicht umklassifiziert. Das Kennzeichen erteilt keine Rollen- oder 2FA-Ausnahme.

In [`.env.example`](../.env.example) steht die Option direkt beim vollständigen
Beispielbenutzer. Die benötigten Beispielzeilen in die lokale `.env` übernehmen,
deren führendes `#` entfernen und eigene Kontodaten einsetzen. Die Kontoanlage muss mit
`APP_BOOTSTRAP_ENABLED=true` eingeschaltet sein. Beispiel für den ersten Benutzer:

```properties
app.bootstrap.users[0].email-verification-required=false
```

- `false` oder weggelassen: ohne E-Mail-Bestätigung nutzbar, Anzeige „Nicht geprüft“.
  Es wird keine Bestätigungsmail vorgemerkt.
- `true`: Bei der ersten Anlage wird eine Bestätigungsmail vorgemerkt. Erst nach
  erfolgreicher Bestätigung kann das Konto das Portal nutzen. Für die Zustellung
  müssen Mailkonfiguration und Versand eingerichtet sein.

Die Option gehört jeweils zum Konto mit demselben Listenindex. Sie gilt genauso
für `superadmins`, `admins` und `guests`, beispielsweise
`app.bootstrap.admins[0].email-verification-required=true`. Sie ändert weder Rolle
noch Anlageweg des Kontos. Änderungen an diesen Startvorgaben werden beim nächsten
Anwendungsstart gelesen und gelten nur für neu angelegte Konten. Eine bereits
vorhandene Adresse wird übersprungen, auch wenn die Option inzwischen geändert wurde.

## Build und Änderungen übernehmen

```powershell
.\mvnw.cmd clean verify -Pproduction -Dvaadin.productionMode=true
```

Java-/Konfigurationsänderungen benötigen Build und Neustart. Im Entwicklungsmodus
lädt Vaadin die Quellstyles; bei optimiertem Frontend sind Neubau und anschließendes
Browser-Neuladen erforderlich. `app/vite.config.ts` ist für eigene Vite-Anpassungen;
generierte Frontenddateien und `target/` nicht von Hand bearbeiten.
Classpathressourcen als Streams laden, damit auch das gepackte JAR funktioniert.

Für einen direkten Maven-Entwicklungsstart zuerst Shared samt Root-POM installieren;
das Startziel danach ausschließlich auf `app` anwenden:

```powershell
.\mvnw.cmd -pl shared -am install
.\mvnw.cmd -pl app '-Dspring-boot.run.profiles=https' spring-boot:run
```

Beim Startziel kein `-am` verwenden, da sonst auch die anderen Reactorprojekte
das Boot-Startziel ausführen würden. Das Boot-Maven-Plugin setzt das
Arbeitsverzeichnis auf den Repositoryroot und `vaadin.project.basedir` auf `app`.
So bleiben `.env`, Daten und Logs im Root, während Vaadin seine Frontendquellen
im Anwendungsmodul findet. Caddy bei diesem Start separat starten.

Das optimierte JAR ebenfalls vom Repositoryroot aus starten:

```powershell
java -jar .\app\target\app-1.0-SNAPSHOT.jar --spring.profiles.active=https --vaadin.productionMode=true
```

## Lokales HTTPS

TLS endet an Caddy; die Anwendung verwendet intern HTTP. Das Profil `https`
bindet den IDE-Appport an `127.0.0.1` und akzeptiert außer dem harmlosen internen
`GET /internal/health` nur die konfigurierte äußere HTTPS-Origin.
`APP_HTTPS_ORIGIN` ist standardmäßig `https://localhost:8443`,
`APP_HTTPS_PROXY_HOST` die konkrete lokale Proxyadresse. Im Dockerprofil werden
Bindadresse und Proxyhost ausdrücklich durch Compose gesetzt. Die nativen
Tomcat-Forwarded-Header vertrauen ausschließlich den beim Appstart aufgelösten
Proxyadressen. Nach einem Wechsel der Proxy-IP die App neu starten.

Für die IDE Caddy **v2.11.9** aus der offiziellen Distribution unter
`data/https/tools/caddy.exe` bereitstellen. Dieser ignorierte lokale Werkzeugpfad
bleibt bei Maven-Clean erhalten.

### Gemeinsamer Start in IntelliJ

Die im Repository gespeicherte `.run/Application HTTPS.run.xml` erscheint nach
Projektaktualisierung in der Run-/Debug-Auswahl als **Application HTTPS**.

Die Prozessaufsicht übergibt ihren Java-Klassenpfad über eine kurzlebige Argumentdatei
unter `data/https/ide-launcher/start-*/launcher.args`. Verzeichnis und Datei erhalten
bereits bei der Anlage ausschließlich Benutzerrechte (Windows-ACL bzw. POSIX-Rechte).
Die eigene Startablage wird nach erfolgreicher Bestätigung und bei Startfehlern
entfernt; CA-Dateien und fremde Ablagen bleiben unberührt. Kann das Dateisystem die
Rechte nicht sicher setzen, startet der Proxy nicht. Technische Grundlage:
[initiale ACLs mit Java NIO](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/nio/file/attribute/AclFileAttributeView.html).
Diese Konfiguration auswählen und **Run** oder **Debug** drücken; anschließend
`https://localhost:8443/` öffnen. JDK 25 und den Klassenpfad des Anwendungsmoduls
mit Quellen unter `app/src/main/java` verwenden. Die gespeicherte Konfiguration
verwendet das Modul `app`. Maven über die Root-`pom.xml` verknüpfen; die drei
Module werden daraus gemeinsam eingelesen und nicht zusätzlich einzeln verknüpft.
Maßgeblich für die Klassenpfadauswahl ist das Anwendungsmodul mit seinen Quellen,
nicht der Root-Aggregator.

Nach dem Modulumbau im Maven-Fenster **Reload All Maven Projects** ausführen;
**Reload from Disk** allein synchronisiert das Maven-Modell nicht vollständig.
Bei einer `ClassNotFoundException` für die Startklasse unter **Run → Edit
Configurations → Application HTTPS → Use classpath of module** das Anwendungsmodul
auswählen. Arbeitsordner ist `$PROJECT_DIR$`, die VM-Option lautet
`"-Dvaadin.project.basedir=$PROJECT_DIR$/app"`. Hauptklasse ist
`de.derpeterson.app.development.HttpsDevelopmentApplication`, die Programmargumente
lauten `--spring.profiles.active=https --server.port=8080`.

Der Einstieg liegt in `app/src/main/java/de/derpeterson/app/development`, damit die IDE
ihn ohne Testklassen startet und Spring keine Testkonfigurationen einliest.
Dieses Entwicklungspaket wird beim Packen ausdrücklich aus dem Anwendungs-JAR
ausgeschlossen; dessen Einstieg bleibt `Application`. Der IDE-Einstieg prüft
Caddy-Version und freie Ports 8080/8081/8443, wartet auf
Caddys eigene erfolgreiche Startbestätigung und startet danach Spring Boot mit
Profil `https`. Eine separate JVM überwacht den selbst gestarteten Caddy anhand
seines Prozesshandles und der Steuerleitung. Stoppen, Startfehler und harter
Abbruch des Appprozesses räumen diesen eigenen Proxy ab. Stirbt Caddy unerwartet,
wird auch die Entwicklungsanwendung beendet. Bestehende Prozesse werden weder
übernommen noch beendet; insbesondere vor dem IDE-Start einen über die BAT-Datei
gestarteten Caddy mit Strg+C stoppen. Die Konfiguration erlaubt nur eine Instanz.

Java-Breakpoints und HotSwap bleiben nutzbar. Der automatische Spring-DevTools-
Neustart ist in diesem Einstieg deaktiviert, damit er keinen zweiten Proxy
startet. Bei Änderungen, die HotSwap nicht übernehmen kann, über IntelliJ neu starten.
Ein anderer lokaler Caddy-Pfad lässt sich in den VM-Optionen mit
`-Dhttps.dev.caddy=C:\Dev\Tools\Caddy\caddy.exe` setzen; Pfade mit Leerzeichen
als ganze VM-Option in doppelte Anführungszeichen setzen.

### Separater Proxy und Zertifikatseinrichtung

Alternativ im Projektroot `https-server-starten.bat` per Doppelklick starten und
das Fenster offen lassen. Strg+C beendet den eigenen Proxy. Die BAT-Datei startet
nur Caddy; die Anwendung dann separat in IntelliJ starten.

Nach dem ersten Proxystart einmalig `https-zertifikat-einrichten.bat` per Doppelklick
ausführen und den Windows-Dialog bestätigen. Dadurch wird die eigene lokale CA
für den Windows-Benutzer bis zum ausdrücklichen Entfernen als vertrauenswürdig
eingerichtet. Beide Starter wechseln selbst ins Projektverzeichnis und setzen
`RemoteSigned` nur für den gestarteten PowerShell-Prozess, ohne eine dauerhafte
Ausführungsrichtlinie zu ändern. Fehler bleiben im Fenster sichtbar.

Alternativ in einem eigenen PowerShell-Terminal aus dem Projektroot starten:

```powershell
.\scripts\https\ide.ps1 -CaddyPath C:\Tools\caddy.exe
```

Der konkrete Binärpfad ist lokal wählbar. Das Skript prüft die Version und läuft
im Vordergrund; Strg+C beendet den eigenen Proxy. Seine CA liegt geschützt im
ignorierten `data/https/ide/`; automatisches Speichern der Konfiguration im
allgemeinen Caddy-Benutzerverzeichnis ist deaktiviert. Der Zugriff über `http://localhost:8081` leitet
GET/HEAD auf HTTPS um; andere HTTP-Methoden werden abgewiesen.

Nach dem ersten Proxystart die **eigene** CA bewusst für den Windows-Benutzer
freigeben; dieser Schritt verändert dessen Zertifikatsspeicher und bleibt bis zum
ausdrücklichen Entfernen wirksam:

```powershell
.\scripts\https\trust.ps1 -Action Add -Certificate .\data\https\ide\pki\authorities\local\root.crt -Intermediate .\data\https\ide\pki\authorities\local\intermediate.crt
# Rückbau vor dem Löschen der CA-Dateien:
.\scripts\https\trust.ps1 -Action Remove -Certificate .\data\https\ide\pki\authorities\local\root.crt -Intermediate .\data\https\ide\pki\authorities\local\intermediate.crt
```

`Add` gehört zur Einrichtung, `Remove` ausschließlich zum späteren Rückbau.
Der Marker `root.crt.owned` bindet den Rückbau an die tatsächlich aufgenommenen
Fingerabdrücke. Das Skript übernimmt keine bereits vorhandenen Trust-Einträge.
Nur öffentliche CA-Zertifikate an Clients weitergeben; CA-Schlüssel verbleiben
lokal. Ohne bestätigtes Vertrauen sind Zertifikatsfehler erwartbar; nicht umgehen.

Beim direkten IntelliJ-Start der Klasse `Application` im Feld **Program arguments**
`--spring.profiles.active=https` eintragen. Alternativ gehört
`-Dspring.profiles.active=https` in **VM options**; bei einer Spring-Boot-
Run-Konfiguration kann das Feld **Active profiles** auf `https` gesetzt werden.
`-Dspring-boot.run.profiles=https` gehört ausschließlich zum Maven-Ziel
`spring-boot:run` und aktiviert beim direkten Klassenstart kein Profil.
Auch hier das Anwendungsmodul als Klassenpfad, den Repositoryroot als Arbeitsordner
und `"-Dvaadin.project.basedir=$PROJECT_DIR$/app"` als VM-Option verwenden.
Danach die Anwendung vollständig neu starten. Direkte Aufrufe von
`http://localhost:8080/` bzw. `/login` werden im HTTPS-Profil mit 403 abgewiesen;
der harmlose interne Healthcheck bleibt die Ausnahme. Der äußere Zugang über den
separat gestarteten Caddy-Proxy ist `https://localhost:8443/`.
Die Mailbasis wird als Default aus der HTTPS-Origin gesetzt. Bereits gespeicherte
Adminwerte behalten Vorrang; einen dort noch gespeicherten HTTP-Wert bewusst in
der Oberfläche ändern. Kein Backfill und keine Änderung vorhandener Daten beim Start.
Für Nachweise ausschließlich frische isolierte Daten verwenden.

JSESSIONID und Remember-me werden bei HTTPS als Secure und HttpOnly ausgegeben,
beide verwenden SameSite=Lax. Caddy überschreibt fremde Forwarded-Header und prüft
den äußeren Host einschließlich Port. Für die wechselnden lokalen Zugänge wird
kein HSTS ausgeliefert. Der normale IDE-Start ist nur lokal erreichbar.

### LAN-Test auf Handy und Tablet

Den lokalen IDE-Server stoppen und in IntelliJ **Application HTTPS – LAN**
starten. Der Startdialog fragt die private IPv4 des PCs ab, die `ipconfig`
am aktiven Netzwerkadapter zeigt. Die Adresse muss diesem Rechner zugewiesen
sein. Die Konfiguration verwendet diese IP als einzige HTTPS-Origin auf Port
8443. Caddy lauscht auf dieser Adresse, Spring Boot bleibt auf `127.0.0.1:8080`.
Der separate BAT-Starter ist für den lokalen Zugang vorgesehen.

Handy oder Tablet müssen im selben privaten Netzwerk sein. Wenn die
Windows-Firewall den Zugriff blockiert, eine eingehende Regel für Caddy auf TCP
8443 einrichten und sie auf das Profil **Privat** und **Lokales Subnetz**
begrenzen. Die Startkonfiguration legt selbst keine Firewallregel an.

Auf dem Testgerät einmalig dem öffentlichen Stammzertifikat
`data/https/ide/pki/authorities/local/root.crt` vertrauen. Die Adresse im
Browser hat dann die Form `https://<LAN-IP>:8443/`. Der LAN-Modus eignet sich
für Layout, Portal-Anmeldung und 2FA mit Testkonten. Für Google oder Microsoft
muss eine vom Anbieter akzeptierte Rücksprungadresse gesondert eingerichtet
werden. Nach dem Stoppen des LAN-Profils ist die App wieder über den normalen
IDE-Start unter `https://localhost:8443/` erreichbar.

Technische Grundlagen: [Caddy Local HTTPS](https://caddyserver.com/docs/automatic-https#local-https),
[Tomcat RemoteIpValve](https://tomcat.apache.org/tomcat-11.0-doc/config/valve.html#Remote_IP_Valve),
[IntelliJ Application-Konfiguration](https://www.jetbrains.com/help/idea/run-debug-configuration-java-application.html).

## Gemeinsame Java-Coverage

Die Root-POM legt den bestehenden Sonar-Projektkey `vaadin-spring-app` fest.
Der Maven-Scanner übernimmt die Java-Quell-/Testverzeichnisse, Klassen und
Abhängigkeiten aus den Modulen. Keine globalen `sonar.sources`-/`sonar.tests`-
Listen oder neuen Quell-/Coverage-Ausschlüsse setzen. Shared bleibt Anwendungscode;
die vollständigen Browserquellen unter `browser-tests/src/test/java` bleiben Testcode.

Das optionale Profil `coverage` ergänzt ausschließlich das technische POM-Modul
`coverage-report`; die drei fachlichen Module bleiben unverändert. Dieses Modul
wird mit `sonar.skip=true` nicht als Anwendung analysiert. JaCoCo führt App- und
Shared-Klassen samt Quellen zusammen; das Browsermodul liefert über seine
Testabhängigkeit nur Messdaten. Die festen Messsitzungen `app` und `browser-tests`
belegen, dass beide Testmodule im Bericht angekommen sind. Nur `target/jacoco.exec`
aus den Modulen wird geladen, keine optionale Browsermessung aus `runs/`.
Das Berichtsmodul benötigt keine transitiven Framework-/Browserbibliotheken;
die dortigen Maven-Abhängigkeitsausschlüsse verändern nicht die Anwendung oder
den Sonar-Quellumfang. Die bereits vorhandene JaCoCo-Version bleibt erhalten.

Aus dem Projektroot den frischen Bericht und das dazu passende wirksame Maven-Modell
erzeugen und offline prüfen:

```powershell
.\mvnw.cmd --version
.\mvnw.cmd clean verify '-Pproduction,coverage' '-Dvaadin.productionMode=true'
.\mvnw.cmd '-Pproduction,coverage' '-Dvaadin.productionMode=true' help:effective-pom '-Doutput=coverage-report/target/effective-pom.xml'
.\.venv\Scripts\python.exe scripts/coverage/verify_coverage.py
```

XML und HTML liegen unter `coverage-report/target/site/jacoco-aggregate/`.
Alle analysierten Module verwenden diesen absoluten gemeinsamen XML-Berichtspfad,
auch `app` mit seinem separaten Boot-Parent. Der Prüfer kontrolliert die drei
Java-Module, vollständigen Bytecode, getrennte Quell-/Testverzeichnisse,
Quellzuordnung, Messdateien/-sitzungen und Aktualität von Maven-Modell und Bericht.
Messdaten müssen außerdem mindestens so aktuell wie die eigenen Klassen und
Quellen sowie die gemeinsam verwendeten Shared-Klassen und -Quellen sein. Fehlende oder
veraltete Nachweise führen zum Fehler; eine tatsächlich gemessene Null bleibt ein
gültiger Zahlenwert. JaCoCo-Zeilencoverage ist nicht mit der Sonar-Gate-Bewertung
gleichzusetzen. Dieser lokale Nachweis ist keine Serveranalyse.

Grundlagen: [Sonar-Maven-Analyseumfang](https://docs.sonarsource.com/sonarqube-server/analyzing-source-code/scanners/sonarscanner-for-maven),
[Java-Coverage und Berichtsaggregation](https://docs.sonarsource.com/sonarqube-server/analyzing-source-code/test-coverage/java-test-coverage),
[JaCoCo-Aggregation der verwendeten Version](https://github.com/jacoco/jacoco/blob/v0.8.16/jacoco-maven-plugin/src/org/jacoco/maven/ReportAggregateMojo.java).

## SonarQube und Codex

Der geschützte Scanner-Helfer baut und prüft den gemeinsamen Modul-/Coverageumfang.
Sein CLI-Einstieg setzt stdout und stderr auf UTF-8, damit umgeleitete
Windows-Ausgaben mit Unicode-Diagnosen nicht am lokalen ANSI-Zeichensatz
abbrechen. Die Maskierung des Sonar-Tokens bleibt dabei erhalten.
Der Servernachweis umfasst App, Shared und alle Java-Browsertests unter demselben
Projektkey; das technische Berichtsmodul selbst bleibt ausgeschlossen. Aktuelle
Revision, Ergebnisse und Analysegrenzen stehen im [Prüfstand](status.md).

Die lokale Anbindung liegt unter `scripts/sonar/`. Sie benötigt die virtuelle
Python-Projektumgebung (Python 3.11 oder neuer), Docker Desktop und den vorhandenen
SonarQube Community Build unter `http://localhost:9000`. Die Versionen von MCP-Image
und Maven-Scanner sind im Helfer festgelegt; das Image ist zusätzlich per Digest gebunden.

Einmalige Einrichtung im Projektroot:

```powershell
# Offizielles Image in der im Helfer festgelegten Version bereitstellen:
docker pull sonarsource/sonarqube-mcp:1.29.0.4488
# Verdecktes lokales Eingabefenster öffnen:
.\.venv\Scripts\python.exe scripts/sonar/sonar.py token
# Projektbezogene Codex-Konfiguration erzeugen:
.\.venv\Scripts\python.exe scripts/sonar/sonar.py configure
```

Unter SonarQube „My Account → Security“ einen **Benutzertoken** erstellen.
Das Konto benötigt Browse für `vaadin-spring-app`, für Scans zusätzlich Execute
Analysis und für ausdrücklich beauftragte Statusentscheidungen die entsprechenden
Issue-/Hotspot-Rechte. Ein Projekt-/globaler Analysetoken ersetzt den MCP-Benutzertoken nicht.
Token ausschließlich im lokalen Fenster eingeben. Die Datei
`%LOCALAPPDATA%/VaadinSpringApp/Sonar/token.dpapi` ist mit Windows DPAPI an den
Windows-Benutzer gebunden und liegt außerhalb des Repositorys. Zum Ersetzen das
Fenster erneut aufrufen. Alternativ ist `SONARQUBE_TOKEN` im aufrufenden Prozess
möglich. Keine Werte in Chat, Skript, Konfiguration oder Befehlsargumente kopieren.

`configure` erzeugt die ignorierte `.codex/config.toml` mit lokalen absoluten Pfaden.
Nach einem Umzug die vorhandene lokale Konfiguration bewusst abgleichen; fremde
Konfiguration wird nicht überschrieben. In Codex das tatsächliche Repository als
vertrauenswürdiges Projekt öffnen und MCP neu laden. Lokale Clients verwenden die
[Codex-MCP-Konfiguration](https://learn.chatgpt.com/docs/extend/mcp?surface=cli);
Werkzeuge in einem ChatGPT-Chat mit lokalem Ausführer sind dadurch nicht automatisch
verfügbar. Im jeweiligen Client einen echten Leseaufruf prüfen. `codex mcp get/list`
verwaltet globale Einträge und ist keine Abnahme dieser Projektkonfiguration.

Der MCP-Start öffnet einen eigenen kurzlebigen Container ohne Arbeitsverzeichnismount,
Docker-Socket oder veröffentlichten Port. Er verbindet sich über
`host.docker.internal:9000` mit dem vorhandenen Sonar-Dienst. Aktiviert sind die für
Projekt, Findings, Regeln, Gate, Metriken und Hotspots benötigten Werkzeuggruppen.
Schreibende Werkzeuge ausschließlich im freigegebenen Sonar-Ausführungsmodus nutzen;
Einrichtung, Analyseplanung und große QA verändern keine Issue-Status.

Zusätzliche technische Leseprüfung, ohne Scan oder Statusänderung:

```powershell
.\.venv\Scripts\python.exe scripts/sonar/sonar.py read
```

Sie prüft Authentifizierung, Hauptbranch, vollständige Findings/Hotspots, Gate,
Metriken und Analyseidentität. Die Ausgabe enthält getrennte Anzahlen sowie die
Quellzuordnung zu lokalem main. `sourceMatchesLocalMain: false` bedeutet abweichenden
Stand; `null` bedeutet fehlende Revision. Fehlende Coverage bleibt fehlend. Ein
Verbindungs-/Seitenfehler ist kein erfolgreicher Abruf mit null Findings.
Im normalen Skill-Ablauf werden die einzelnen Codestellen über MCP abgerufen.

Vollständigen Scan nur als beauftragten Analyseschritt auf sauberem **main** starten:

```powershell
.\.venv\Scripts\python.exe scripts/sonar/sonar.py scan
```

Der Ablauf prüft JDK 25 und den Stil zentral mit `-N`. Er baut und installiert
den vollständigen Reactor mit `clean install`, Profilen `production,coverage` und
optimiertem Frontend einschließlich App-/Shared- und Browser-Helfertests. Das
Install ist für den separaten Maven-Scanner erforderlich; ein einzelnes Shared-
Install entfällt. Danach erzeugt er mit denselben Profilen das wirksame Maven-Modell
und ruft den gemeinsamen Coverage-Prüfer auf. Fehlende Module, Klassen, Berichte
oder unpassende Messdaten verhindern den Upload. Build, Modell und Scanner verwenden
denselben Modulumfang. Nur ein Gesamtscan unter `vaadin-spring-app` wird mit fester
Quellrevision hochgeladen; keine Teiluploads oder Browserjourneys.
Feature-Branches und uncommittierte Änderungen werden abgewiesen. Eine lokale
`.codex/sonar/scan.lock` verhindert doppelte Helferläufe; nach einem harten Abbruch
vor Entfernen einer verbliebenen Sperre den tatsächlichen Prozessstand prüfen.
Serverwarteschlange und Quellstand werden vor Upload erneut geprüft. Andere externe
Scanner müssen ebenfalls koordiniert werden; die Sperre erfasst nur diesen Helfer.

Der konkrete Serverauftrag muss erfolgreich enden und zur erwarteten Analyse-ID
und Revision gehören. Ein rotes Gate liefert trotz abgeschlossener Verarbeitung
einen Fehlerstatus. Task-Metadaten stehen nur aktuell unter `.codex/sonar/`;
kein Berichtarchiv und kein manueller JSON-Export sind nötig. Eine gelesene frühere
Analyse ersetzt keinen neuen Scan.

Die manuellen BAT-Einstiege im Projektroot verwenden denselben geschützten Helfer
und die Python-Projektumgebung, unabhängig vom aufrufenden Arbeitsverzeichnis:

```powershell
.\sonar-scan.bat
.\sonar-extract-findings.bat
```

`sonar-scan.bat` startet den oben beschriebenen vollständigen Scan auf sauberem main.
`sonar-extract-findings.bat` führt nur `sonar.py export` aus, ohne Build, Scan oder
Statusänderung. Der lesende Export enthält Analyse/Revision, Gate, Metriken und alle
paginiert gelesenen offenen Issues, Ausnahmen und Hotspots. Er ersetzt erst nach
vollständigem Abruf atomar die ignorierte `sonar-findings.json` im Projektroot;
ein fehlgeschlagener Abruf erhält einen vorhandenen Export. Ohne bisherigen Export
bleibt die Datei bis zum ersten bewussten Aufruf aus. Beide BATs geben Fehlercodes
weiter und enthalten keine Zugangsdaten; sie verwenden die vorhandene DPAPI-Ablage
oder Prozessvariable. Mit `--help` nur die Bedienhilfe anzeigen, ohne Serverzugriff.

Offizielle Grundlagen: [Sonar-MCP-Einrichtung](https://docs.sonarsource.com/sonarqube-mcp-server/setup/quickstart-guides/codex-cli),
[Maven-Scanner](https://docs.sonarsource.com/sonarqube-server/analyzing-source-code/scanners/sonarscanner-for-maven).

## Arbeiten mit Codex

Die [Skill-Anleitung](codex-skills.md) beschreibt die Projektabläufe: große Testrunde,
Sonar-Bereinigung, Umsetzung starten, Änderungen prüfen und lokal mergen.
Testbefehle stehen in der [Testanleitung](tests.md).

## Codestil

Diese Regeln gelten beim Schreiben, Ändern und Prüfen von Java-Code. Die Beispiele
illustrieren Schreibweise und Lesbarkeit, ohne neue fachliche Anforderungen einzuführen.

Der Projekt-Codestil baut auf dem [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html),
Fowlers Beispielen zu [Extract Function](https://refactoring.com/catalog/extractFunction.html)
und [Decompose Conditional](https://refactoring.com/catalog/decomposeConditional.html)
sowie Oracles [Leitfaden zur Sichtbarkeit](https://docs.oracle.com/javase/tutorial/java/javaOO/accesscontrol.html) auf.

### Namen und Methodenaufgaben

Klassennamen verbinden die fachliche Zuständigkeit mit der Art des Bausteins,
etwa `ResetPasswordView` oder `AccountSessionsComponent`. Der Einsatz im Code
bestimmt den Bereichspräfix.

| Namensbestandteil | Verwendung |
| --- | --- |
| `Admin` | Aufgabe der Administration, etwa `AdminExternalProviderSettingsSection`. |
| `Portal` | Aufgabe des angemeldeten Benutzerportals, etwa `PortalSidebarPageLayout`. |
| `Public` | Aufgabe im Besucherbereich, etwa `PublicAuthenticationPanelLayout`. |
| `Cosmos` | Gemeinsame Gestaltung im Cosmos-Theme, etwa `CosmosComboBoxStyling` oder `CosmosCssClasses`. |
| `Page` am Anfang | Bereichsübergreifender Seitenaufbau oder Seitennavigation, etwa `PageContentLayout` oder `PageNavigationComponent`. |
| `Page` am Ende | Page Object für Browsertests, etwa `AccountPage` oder `AdminConfigurationPage`. |

Bei bereichsübergreifenden Bausteinen den tatsächlichen gemeinsamen Zweck nennen.
Für die Art des Bausteins den passenden Zusatz verwenden, etwa `Layout`, `Dialog`,
`Controller` oder `Listener`. Bei Umbenennungen auch Klassennamen in Zeichenketten
und Dokumentationsverweise prüfen. Die Browsertest-Auswahl und ihre
Laufzeitgewichte stehen in `BrowserSuites` und `BrowserExecutionPlan`.

Methodennamen benennen die Aktion und die Art ihres Ziels oder Ergebnisses,
etwa `closeAccountDialog()` oder `buildAccountHeaderLayout()`. Das soll auch am
Aufruf erkennbar sein.

**Namen enthalten Zweck und Typ.** Felder, Konstruktorparameter, andere
Methodenparameter und lokale Variablen enthalten den Typnamen, etwa
`closeDialogButton` für einen `Button`, `emailTextField` für ein `TextField`,
`twoFactorStore` für einen `TwoFactorStore` und `messageProperties` für
`MessageProperties`. Das gilt auch für Argumente, die beim Aufruf weitergereicht
werden. Bei einfachen Werten benennt der Name den fachlichen Typ, etwa
`unreadNotificationCount` für eine Anzahl und `hasActiveSession` für eine
Bedingung. Sammlungen benennen ihren Elementtyp im Plural, etwa `sessionEntries`.
Bei UI-Elementen bezieht sich der Typ auf das Objekt. `expiredLinkHintSpan`
bezeichnet die Komponente, `expiredLinkHintText` den darin angezeigten String.

Bei bewusst typübergreifenden Hilfsmethoden beschreibt der Name die gemeinsame
Aufgabe oder Rolle, etwa `collectNonBlankNames()` oder `itemToValidate`.
Extern vorgegebene Namen bleiben bestehen, etwa `run()`, `beforeEnter()` und
Bezeichner, die direkt an bestehende Konfigurationsschlüssel gebunden sind.
Auch Schleifenindizes benennen ihren Bezug, etwa `ticketIndex` oder
`columnIndex`.

**Zusammengehörige Namen immer als Block prüfen.** Bei einer Java-Änderung oder
einem Review die Namen im betroffenen fachlichen Block vollständig prüfen, etwa
die Abhängigkeiten einer Klasse samt Konstruktorparametern oder die Felder und
Aktionen eines Dialogs. Eine Textsuche nach bekannten Beispielen dient nur zum
Auffinden der Stellen. An jeder Verwendung muss aus dem Namen hervorgehen,
welcher fachliche Gegenstand gemeint ist und welche Rolle das Objekt hat.
Bei einer Überarbeitung einer ganzen Klasse alle ihre selbst gewählten Namen
prüfen, auch in fachlichen Blöcken, die der ursprüngliche Befund nicht erwähnt.
Unklare Namen im selben Block gemeinsam korrigieren, wenn der Auftrag
Lesbarkeitsänderungen erlaubt. Bei einem reinen Review alle konkreten Stellen
benennen. Bereits klare Namen beibehalten.

Beispiel: In einem 2FA-Dienst genügt es nicht, nur `authorization` in
`twoFactorAuthorization` umzubenennen. Die benachbarten Abhängigkeiten
`policies`, `factors` und `enrollment` werden im selben Durchgang geprüft und
heißen hier `twoFactorPolicies`, `twoFactorStore` und `twoFactorEnrollment`.
`factors` klingt wie eine Sammlung von Faktoren; `twoFactorStore` benennt den
Speicher, der tatsächlich verwendet wird.

| Wenig aussagekräftig | Verständlicher im passenden Kontext |
| --- | --- |
| `list`, `data` | `unreadNotifications` |
| `u` in einer längeren Verarbeitung | `account` |
| `flag` | `hasUnreadNotifications` |
| `texts` für den Textkatalog | `messageProperties` |
| `close` für ein UI-Element | `closeDialogButton`, `closeDialogIcon` oder `closeDialogLabel` |
| `refresh()` für die Sitzungsliste | `refreshSessionList()` |
| `process()` | `markNotificationsAsRead()` |

Eine Methode hat eine klar erkennbare Aufgabe. Ihr Name passt zum Ergebnis und zu
wichtigen Seiteneffekten. Beim Wechsel der Aufgabe eine fachlich benannte Teilmethode
erwägen. Der Aufbau eines Buttons kann beispielsweise direkt auf
`submitPasswordReset(token)` verweisen, wo Eingabeprüfung, Speichern und Rückmeldung
als zusammenhängender Ablauf zu lesen sind. Umfangreiche Verzweigungen auslagern,
wenn die Namen der Teilmethoden die fachlichen Fälle beim Lesen sichtbar machen.
Aufrufer, Fallunterscheidungen, Ausführungsreihenfolge sowie Transaktions-, Sperr-
und Fehlergrenzen bei reinen Lesbarkeitsänderungen erhalten. Zeilenzahl und
Sonar-Hinweise sind Anhaltspunkte. Entscheidend ist, ob die Aufteilung den Ablauf
verständlicher macht.

Hilfsmethoden nur so weit sichtbar machen, wie es ihre Aufrufer erfordern. Innerhalb
der eigenen Klasse genügt `private`. Zugriff aus demselben Paket kann ohne Modifier
auskommen. `protected` ist für benötigte Erweiterungspunkte in Unterklassen gedacht.
Vor einer Änderung der Sichtbarkeit auch Overrides und Framework-Einstiege prüfen.

### Gliederung in Blöcke

Felder nach fachlichem Zusammenhang gruppieren. In einer View können etwa das
E-Mail-Feld, sein Fehlerhinweis und die zugehörige Listener-Registrierung einen Block
bilden. Navigation und ihre Felder stehen in einem eigenen Block. Die Methoden in
einer nachvollziehbaren Reihenfolge anordnen und Hilfsmethoden in der Nähe des
zugehörigen Ablaufs halten. Die Initialisierungsreihenfolge von Feldern erhalten,
wenn sie das Verhalten beeinflusst.

**Auch innerhalb einer Methode jeden neuen logischen Arbeitsschritt mit einer
Leerzeile absetzen.** Das gilt ebenso für kurze Methoden, wenn sie mehrere Schritte
enthalten. Beim Formularaufbau können etwa Panel, Eingabebereich, Ereignisbehandlung
und Einhängen der Komponenten eigene Absätze bilden. Die Deklaration eines Felds
und seine Konfiguration gehören in denselben Absatz. Ein Beispiel ist
`showLoginMethodTabs()` in
[TwoFactorView](../app/src/main/java/de/derpeterson/app/views/TwoFactorView.java).

Bei einem größeren Themenwechsel können es zwei sein, sofern der Projektformatter
sie erhält. Bei größeren Feld- oder Methodenblöcken kann eine kurze Überschrift
wie `// E-Mail ändern` beim Überfliegen helfen. Die Leerzeile trennt den Block
vom vorherigen Code und steht vor der Überschrift. Der erste Code des Blocks
folgt direkt darunter. Die Überschrift benennt den fachlichen Zusammenhang
und muss zum gesamten Block passen. Der Aufbau der Passwortfelder
in [ResetPasswordView](../app/src/main/java/de/derpeterson/app/views/ResetPasswordView.java)
zeigt diese Gliederung im Projekt.

Bei einer klassenweiten Lesbarkeitsprüfung die ganze Klasse lesen und die
Aufrufstellen der betroffenen Abläufe mitprüfen. Den Diff anschließend aus
Lesersicht prüfen. Sind Felder und Methoden fachlich gruppiert? Stimmen Namen,
Leerzeilen und Kommentare mit dem tatsächlichen Ablauf überein? Sind die
Verzweigungen und Seiteneffekte noch vollständig? Eine Klasse mit einer
angepassten Aufrufstelle bleibt bis zu ihrer eigenen Prüfung offen. Bei
Umbenennungen auch vorhandene Tests prüfen, die Bezeichner als Zeichenketten
referenzieren, und ihre bisherigen Erwartungen erhalten.

Bei mehreren Klassen den Arbeitsstand pro Klasse erst nach einem Durchgang durch
den vollständigen neuen Code abhaken. Der Diff allein reicht dafür nicht. Auch
Methoden ohne Änderung und kurze Methoden mit mehreren Anweisungen lesen. Pro
Ablauf unterscheiden, welche Anweisungen vorbereiten, ausführen, sichtbaren
Zustand ändern oder andere Teile benachrichtigen. Dabei auch Namen und Aufrufer
prüfen, wenn ein Methodenaufruf neben dem sichtbaren Ergebnis weitere Arbeit
auslöst. Zum Beispiel kann eine Methode beim Anzeigen eines Panels auch Sitzungen
laden. Der Aufruf soll beides erkennen lassen. Solche Befunde im laufenden
Lesbarkeitsauftrag direkt bearbeiten, sofern sich die Reihenfolge und das Verhalten
erhalten lassen.

### Klammern und Kontrollfluss

`if`, `else`, `for`, erweitertes `for`, `while` und `do/while` haben auch bei einer
Anweisung geschweifte Klammern. `else if` darf als Kette stehen. `try`, `catch` und
`finally` übersichtlich gliedern; Fehlerbehandlung nicht mit anderen Aktionen in
eine Zeile pressen. Frühe Rückgaben oder `continue` können Verschachtelung reduzieren,
sofern sie den Ablauf und bestehende Aufräum-/Fehlerverträge erhalten.

Beispiel für einen gut überblickbaren Filter mit bewusstem Umgang mit leeren Namen:

```java
private List<String> collectNonBlankNames(List<String> candidateNames) {
  List<String> names = new ArrayList<>();
  for (String candidateName : candidateNames) {
    if (candidateName == null || candidateName.isBlank()) {
      continue;
    }
    names.add(candidateName);
  }
  return names;
}
```

Einfache kurze Bedingungen dürfen ternär bleiben. Verschachtelte ternäre Ausdrücke
in verständliche Schritte oder fachlich benannte Methoden aufteilen. Eine Schleife
ist oft verständlicher als eine lange Stream-Kette; kurze klare Streams sind ebenfalls
in Ordnung. Die Aufgabe entscheidet, nicht eine bevorzugte Syntax.

### Explizite Typen und sinnvoller Einsatz von var

Explizite lokale Typen sind Standard, besonders bei Service- und Factory-Rückgaben.
Normale und statische Einzelimporte verwenden; keine Sternchenimporte.

```java
List<String> supportedLanguages = List.of("de", "en");
Instant notificationTime = clock.instant();
```

`var` nur verwenden, wenn es einen konkreten Lesbarkeitsvorteil hat: bei anonymen
oder nicht direkt benennbaren Typen sowie bei sehr langen generischen Typen,
die unmittelbar im Konstruktor sichtbar sind. Beispielsweise entfällt hier eine
lange doppelte Typangabe:

```java
var translationsByAccount = new LinkedHashMap<Long, Map<Locale, List<String>>>();
```

`var` ist weder dynamische Typisierung noch ein Laufzeitvorteil. Bei einer unklaren
Rückgabe wie `service.load(...)` den Typ ausschreiben.

### Kommentare und Javadoc

Verträge, notwendige Voraussetzungen, wichtige Seiteneffekte und ungewöhnliche
Fehlerfälle erklären, soweit sie aus Namen und Code nicht klar hervorgehen.
Wenn eine Methode mehrere eng zusammengehörige Schritte koordiniert, kann ein
kurzer Kommentar darüber ihren Zweck oder eine notwendige Reihenfolge erklären.
Selbsterklärende Getter oder offensichtliche Schritte brauchen keinen Kommentar.
Beim Schreiben und im Review jeden betroffenen Kommentar mit dem Code darunter
abgleichen. Kommentare direkt an die Stelle setzen, deren Grund oder Vertrag sie
erklären. Beim Verschieben von Code die zugehörigen Kommentare mit verschieben
und die Blocküberschriften erneut prüfen. Kommentare streichen, wenn sie nur
einen Namen oder eine unmittelbar verständliche Anweisung wiederholen.

```java
// Der Grenzzeitpunkt ist bereits fällig; Gleichheit darf keinen Retry verzögern.
boolean retryIsDue = !nextAttemptAt.isAfter(now);
```

Eine Methodendokumentation kann knapp bleiben, muss aber den wichtigen Vertrag nennen:

```java
/**
 * Prüft die Retry-Fälligkeit einschließlich des Grenzzeitpunkts; beide Werte sind nicht null.
 */
private boolean isRetryDue(Instant nextAttemptAt, Instant now) {
  return !nextAttemptAt.isAfter(now);
}
```

Regex direkt daneben auf Deutsch erläutern: Zweck, erlaubte Zeichen, Anker,
Gruppen und bei Ersetzungen die verwendeten Gruppenreferenzen, soweit vorhanden.
Vor einer neuen Regel vorhandene zentrale Validierung prüfen und wiederverwenden.

### Java formatieren und prüfen

Google Java Format (`GOOGLE`, zwei Leerzeichen) ist über Spotless im Root-`pom.xml`
konfiguriert und umfasst Anwendung, Shared und Browsertests. Nach jeder eigenen
Java-Änderungsrunde vor Tests und Abschluss im Projektroot ausführen, auch ohne
weiteren Build/Testlauf; nach späteren Korrekturen wiederholen:

```powershell
.\mvnw.cmd -N spotless:apply
.\mvnw.cmd -N spotless:check
.\mvnw.cmd -N checkstyle:check
```

Spotless ordnet Imports und entfernt unbenutzte Imports. `ratchetFrom=HEAD`
begrenzt die Bearbeitung auf geänderte/neue Java-Dateien. Bei fremden Java-Änderungen
beide Spotless-Aufrufe zusätzlich mit `-DspotlessFiles=...` auf die eigenen Dateien
begrenzen; der Wert ist ein regulärer Ausdruck für absolute Dateipfade.
Beispiel ausschließlich für eine selbst bearbeitete `views/AccountView.java`:

```powershell
.\mvnw.cmd -N '-DspotlessFiles=.*[\\/]views[\\/]AccountView\.java' spotless:apply
.\mvnw.cmd -N '-DspotlessFiles=.*[\\/]views[\\/]AccountView\.java' spotless:check
.\mvnw.cmd -N checkstyle:check
```

Den Filter passend zu den tatsächlich eigenen Dateien wählen; den Ratchet nicht
abschalten. Keine ungefragte projektweite Umformatierung. Checkstyle prüft Klammern
und Sternchenimporte; gute Namen, sinnvolle Typwahl und Methodengliederung bleiben
Teil des Reviews.

### Python formatieren

Ruff-Version und Regeln stehen in `requirements-dev.txt` und `ruff.toml`.
Nur in der virtuellen Projektumgebung installieren:

```powershell
.\.venv\Scripts\python.exe -m pip install -r requirements-dev.txt
```

Nach jeder eigenen Python-Änderungsrunde vor Tests und Abschluss die eigenen
Dateien formatieren und prüfen; nach Korrekturen wiederholen:

```powershell
.\.venv\Scripts\python.exe -m ruff format PFAD
.\.venv\Scripts\python.exe -m ruff format --check PFAD
```

`PFAD` durch explizite eigene Dateipfade ersetzen und bei Leerzeichen zitieren.
Ruff hat hier keinen Git-Ratchet; fremde Dateien nicht mitformatieren.
`ruff check --fix` ersetzt die Formatierung nicht. OpenCode bleibt eingefroren.

Verbindliche Agentenregeln: [AGENTS.md](../AGENTS.md).
Für sinnvolle Ereignisse, Logpegel und sichere Diagnose gilt der [Logging-Vertrag](logging.md).

## Übersetzungen und Ressourcen

Bei Arbeiten an diesen Bereichen die folgenden Hinweise lesen und anwenden.

### Gemeinsamer Textkatalog und Sprachwechsel

- Übersetzungsdateien liegen unter `app/src/main/resources/i18n/`, Mailvorlagen unter
  `app/src/main/resources/email/`. Feste Schlüssel und typisierte Getter sind einmalig in
  Shared-`MessageTexts` definiert; keine zweite Liste im Anwendungs-/Testmodul pflegen.
- In Anwendungskomponenten die vorhandenen `MessageProperties`-Getter verwenden.
  Direkte rohe Übersetzungsabfragen bleiben innerhalb der i18n-Schicht.
- Normales `MessageProperties` folgt bei jedem Zugriff der aktuellen Sitzungssprache.
  `MessageProperties.forLocale(provider, locale)` bindet ausdrücklich eine feste Sprache,
  ohne die Sitzung zu lesen oder zu ändern; nur für entsprechend feste Übersetzungen verwenden.
- Für Konfigurationsgruppen `AdminConfigGroup` als Identität verwenden, nicht Properties-
  Schlüssel oder übersetzte Labels. Vorhandene typisierte Methoden für Konfigurations-
  und Statusbeschriftungen nutzen.
- Texte sichtbarer Komponenten über den vorhandenen Sprachwechselmechanismus aktualisieren.
  Dynamische Benachrichtigungen erhalten Titel und Inhalt als `Supplier<String>` über
  `NotificationHelper`; seine Textaktualisierung wertet die Supplier erneut aus.
  Bereits übersetzte Strings nicht vorab festhalten, wenn die Meldung die Sprache wechseln soll.
- Browsertests verwenden die gemeinsamen Getter über `UiTexts` mit fester Testsprache;
  keine eigene DE-/EN-Textliste und keine Erwartung „eine von beiden Sprachen“.
  Details: [Browser-Testanleitung](../browser-tests/README.md).

### E-Mail-Vorlagen und Cosmos-Design

- Neue und überarbeitete Anwendungs-E-Mails verwenden verbindlich das bestehende
  Cosmos-Maildesign. Für jede Vorlage deutsche und englische MJML-Quelldateien
  unter `app/src/main/resources/email/` pflegen und daraus die zugehörigen
  HTML-Dateien erzeugen. Die Gestaltung folgt den bestehenden Kontomails und der
  [Theme-Anleitung](theme.md); keine parallelen Maildesigns einführen.
- MJML ist die bearbeitbare Gestaltungsquelle. Das erzeugte HTML nicht unabhängig
  davon von Hand weiterentwickeln. Zur Laufzeit die HTML-Vorlage über den
  bestehenden Vorlagenweg aus dem Classpath laden und vorgesehene Platzhalter
  sicher mit den fachlichen Daten befüllen; keine MJML-Kompilierung im Mailversand.
- Gemeinsame Kopierhinweise und Linkgestaltung liegen unter `email/partials/`.
  Aktionsbuttons haben darunter dieselbe vollständige URL als sichtbaren Link.
  HTML aus dem Projektroot mit `npx.cmd --yes mjml@5.4.1 'app/src/main/resources/email/*.mjml' --config.allowIncludes true --config.fonts '{}' --config.validationLevel strict -o app/src/main/resources/email/`
  erzeugen. Die leere Fontliste verhindert zusätzliche externe Standardschriften;
  Inter bleibt über den vorhandenen lokalen Platzhalter eingebunden. Abschließend
  nur nachlaufende Leerzeichen und Leerzeilen im erzeugten HTML entfernen.
- Java-Dienste steuern Fachaktion, Daten und Versandauftrag. Keine vollständigen
  Mailinhalte durch HTML-Strings, Tag-Verkettungen oder eigene Inline-Styles im
  Java-Code zusammenbauen. Bei einer Vorlagenumstellung den ersetzten HTML-Aufbau
  und ausschließlich dafür benötigte Hilfen vollständig entfernen; keinen alten
  Parallel- oder Fallbackpfad behalten. Weiter benötigte Sicherheits- und
  Versandprüfungen bleiben erhalten.
- Bei jedem E-Mail-Auftrag diese Regeln auch im Review prüfen: MJML und erzeugtes
  HTML stimmen überein, DE/EN sind vorhanden und die Mail übernimmt Logo,
  Typografie, Farben, Aufbau und Footer des gemeinsamen Cosmos-Maildesigns.
  Eine ergänzende Klartextalternative ersetzt die gestaltete HTML-Mail nicht.

### Ressourcen im gepackten JAR

Serverseitige Dateien über Classpath-Streams laden, beispielsweise mit
`ClassPathResource.getInputStream()` oder `getResourceAsStream(...)`. Eigene Streams
zuverlässig schließen, üblicherweise mit `try`-with-resources. Ressourcen nicht über
`new File("src/main/resources/...")` oder die Annahme einer normalen Datei im JAR öffnen.
Öffentliche Theme-/Iconressourcen liegen unter `META-INF/resources/`.

`app/src/main/frontend/generated/`, `app/vite.generated.ts` und die Modul-`target/`-Ordner
werden von Werkzeugen erzeugt und nicht von Hand bearbeitet. Eigene Vite-Konfiguration gehört in
`app/vite.config.ts`. Die [Theme-Anleitung](theme.md) beschreibt Stylesheets und Ladereihenfolge.
