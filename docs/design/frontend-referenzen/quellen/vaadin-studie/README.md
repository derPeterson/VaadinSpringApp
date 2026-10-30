# Vaadin-Studie und isolierter Gestaltungspilot

## Persönliche Daten: bedienbarer Pilot

Der beauftragte Pilot bildet die angemeldete Referenz mit Header, Logo K,
Identitätskarte, SideNav, persönlichen Daten und einem echten E-Mail-Dialog nach.
Er verwendet Vaadin Flow, `CosmosCardComponent` und `CosmosSectionHeaderComponent`.
TextField, EmailField, ComboBox, DatePicker, PasswordField, Button und Dialog sind
echte Vaadin-Komponenten, keine eingebettete HTML-Referenz. Die festen HTML-Dateien
und das ausgelieferte Anwendungstheme bleiben unverändert.

Quellen:

- `app/src/test/java/de/derpeterson/designpilot/`: eigene Boot-Konfiguration und View.
- `app/src/test/resources/personal-data-pilot/`: ausschließlich lokale Styles und SVG-Icons.
- [pilot.ps1](pilot.ps1): Vorbereitung und optionaler lokaler Start.

Aus dem Projektroot:

```powershell
# Nur kompilieren und den eigenen Laufzeitordner vorbereiten:
& docs/design/frontend-referenzen/quellen/vaadin-studie/pilot.ps1
# Zusätzlich den eigenen lokalen Vorschauprozess starten:
& docs/design/frontend-referenzen/quellen/vaadin-studie/pilot.ps1 -Start
```

Die Vorschau läuft auf [127.0.0.1:60826](http://127.0.0.1:60826/).
Das Zahnrad im Header öffnet für diesen Piloten den E-Mail-Dialog. Die übrigen
Navigationsziele melden ausdrücklich den begrenzten Pilotumfang. Speichern und
Bestätigung prüfen ausschließlich synthetische UI-Werte und zeigen einen Hinweis;
sie speichern nichts und versenden keine E-Mail. Die Sprachwahl ist eine lokale
Auswahl, noch kein vollständiger Sprachwechsel der Oberfläche.

Der Starter verlangt JDK 25 und die über den Reactor aufgelösten Projektbibliotheken.
Er kompiliert nur die beiden Pilotklassen und die zwei verwendeten Cosmos-Komponenten
in `target/personal-data-pilot/`. Die normalen Anwendungsklassen, Shared,
Security-, Datenbank-, Session- und Mailmodule fehlen im Laufzeitklassenpfad.
Es werden keine normalen Anwendungseinstellungen oder `.env` geladen. Der Server
bindet ausschließlich an Loopback. Die Pilotquellen liegen im Testbereich und
werden nicht im Anwendungs-JAR ausgeliefert.

Als Frontend verwendet er unverändert ausschließlich `META-INF/VAADIN/` aus dem
vorhandenen Produktionsbundle `app/target/app-1.0-SNAPSHOT.jar.original`.
Dieses muss zu den aktuellen Projektabhängigkeiten passen. Fehlt es nach einem
Clean, ist zuvor ein regulärer Reactor-Produktionsbuild gemäß Entwicklungsanleitung
nötig. Der Starter erzeugt oder ändert keine Frontendquellen und verwendet kein
zweites Abhängigkeitsmodell. Schriften und Logo werden aus den bestehenden Quellen
in den eigenen Laufzeitordner kopiert.

Der eigene Prozess lässt sich anhand seiner gespeicherten ID beenden, nachdem
geprüft wurde, dass diese weiterhin zum Pilotstart gehört:

```powershell
Get-Process -Id (Get-Content target/personal-data-pilot/process-id.txt)
# Nur den eigenen Pilotprozess beenden:
Stop-Process -Id (Get-Content target/personal-data-pilot/process-id.txt)
```

Änderungen an Java erfordern Vorbereitung und Neustart des eigenen Piloten;
danach im Browser aktualisieren. Nach CSS-Änderungen die Ressourcen ebenfalls neu
vorbereiten, den eigenen Piloten neu starten und aktualisieren; Vaadin versieht
die ausgelieferten Styles mit einem beim Start ermittelten Cachewert. Der Pilot ist zur gemeinsamen Gestaltungssichtung
gedacht. Native Picker, Detailabstände und mobile Overlays werden mit der festen
Referenz verglichen; eine vollständige pixelgleiche oder fachliche Abnahme folgt
daraus nicht. Erst danach gemeinsame Themebausteine übernehmen.

Gezielt geprüft sind Compile, Spotless/Checkstyle und die Browseransichten mit
1280 × 720, 1280 × 900, 1024 × 900, 390 × 844 und 844 × 390 Pixeln: ohne horizontalen Seitenüberlauf,
aufklappende mobile Navigation einschließlich Escape/Fokusrückgabe,
Geschlechtsauswahl, nativer Kalender sowie Öffnen, Abbrechen, Kopf-Schließen,
Escape, Pflichtfeldmarkierung und synthetische Bestätigung des E-Mail-Dialogs.
Die Bestätigung zeigte ausdrücklich, dass keine Bestätigung versendet wurde.
Bei der Browsersteuerung wurden Formularwerte mit nativen Tastatureingaben und
Feldwechsel geprüft; bloßes programmatisches Befüllen synchronisierte die
Vaadin-Werte dort nicht zuverlässig. Keine große QA oder fachlichen Kontotests.
Das Formular ist anhand der unveränderten HTML-Referenz vermessen: Bei 1280 × 900,
1024 × 900, 390 × 844 und 844 × 390 stimmen Position, Breite und Höhe aller sechs
Feldflächen sowie Panel, Formularinhalt und Aktionsleiste überein. Das gilt für
den synthetischen Ausgangszustand, nicht pauschal für beliebige Inhalte oder Zoomstufen.
Hilfetext-Abstände, 14-px-Eingabetexte, Innenabstände, Pflichtpunkte, Schreibschutz-
und Kalendericons sowie die Navigation folgen den Referenzwerten. Im E-Mail-Dialog
sind Kopf-, Inhalts- und Hinweisgrößen angeglichen. Lumo zentriert den Dialog
weiterhin optisch etwas oberhalb der geometrischen Mitte; die native Kalender-
und Auswahlbedienung bleibt erhalten. Offen bleibt die gemeinsame visuelle Abnahme.

Der lokale Pilot übernimmt außerdem die Zahnradkontur, Copyright-Typografie sowie
Sterne und Planetensilhouette des aktiven Referenzhintergrunds. Das rote Zählerbadge
behält auf Benutzerwunsch seine Pillenform in größerer Darstellung. Die Avatarpunkte
haben ausdrücklich begrenzte Außenmaße und überdecken die Initialen nicht;
„Verfügbar“ erscheint grün. Beide ComboBoxen verwenden native Auswahlzustände mit
Punkten, blauem Auswahlrahmen und Häkchen rechts. Ihr ComponentRenderer und die
Itemklassen betreffen ausschließlich den Piloten. Der native Kalender bleibt auf
Benutzerwunsch in seiner bisherigen Gestaltung. Diese Details wurden am Desktop
sowie im Smartphone-Hoch-/Querformat nachgeprüft, einschließlich Auswahlwechsel,
Escape, mobiler Navigation und fehlendem horizontalem Seitenüberlauf.

## Vorherige Besucherfrontend-Studie

Die HTML-Gestaltung lässt sich mit den bestehenden Java-Komponenten und lokalem
CSS umsetzen. Es ist kein Wechsel des Frontends nötig. Die Studie ist eine
technische Einschätzung mit einem isolierten, kompilierten Layoutbeispiel;
eine pixelgenaue Vaadin-Browseransicht ist damit noch nicht nachgewiesen.

## Vorhandene Grundlage

Maßgeblich für die Version ist die [Anwendungs-POM](../../../../../app/pom.xml).
Die Studie wurde gegen deren tatsächlich aufgelöste Compile-Abhängigkeiten mit
JDK 25 und den vorhandenen Anwendungsklassen kompiliert. Die Anwendung wurde
dabei weder gestartet noch verändert.

| HTML-Referenz | Vorhandene Vaadin-Grundlage | Nötige Anpassung bei einer späteren Übernahme |
| --- | --- | --- |
| Öffentlicher Header mit Marke und Navigation | `TrackedUserAppLayout`, `HomeView`, vorhandener Anwendungskopf | Öffentlichen Kopf als gemeinsamen Baustein ausgliedern; feste Höhe, mobile Navigation und DE/EN-Segmente lokal gestalten |
| Startseite / Planet | `CosmosSectionHeaderComponent`, `CosmosBackgroundComponent`, normale Flow-Layouts | Hero als `Div`-Layout; die bereits korrigierte lokale SVG als dekoratives Bild verwenden, nicht als HTML-Template |
| Anmelden | `LoginView`, `EmailField`, `PasswordField`, `Checkbox` | Bestehende Sicherheits- und Recoverylogik behalten; nur Layout/Theme übertragen |
| Registrieren | `RegistrationView`, `TextField`, `EmailField`, `PasswordField`, `ComboBox<Gender>`, `DatePicker` | Zwei Spalten über bestehende Formularreihen; mobile Stapelung, keine dekorativen Feldpräfixe, vorhandene Platzhalter |
| Passwort vergessen / zurücksetzen | `ForgotPasswordView`, `ResetPasswordView` | Gemeinsame Panelstruktur; Fehler-/Erfolgszustände aus vorhandenen Fachentscheidungen darstellen |
| Kontoaktivierung / E-Mail-Bestätigung | vorhandene Token-/Bestätigungsansichten | Statuspanels mit gemeinsamen Icons und Aktionen; keine neue Tokenprüfung im Browser |
| Anmeldung fortsetzen / Sitzungen | `AccountSessionsComponent` und bestehender Recoveryablauf | Dialog-/Kontextdarstellung angleichen; Widerrufs- und Zugriffsprüfung im Dienst behalten |
| Technisch nicht verfügbar / Wartung | vorhandene Wartungs- und Fehlerzustände | Gemeinsame dunkle Statuskarte; kein neuer Wartungsvertrag |
| Globale Meldungen | `NotificationHelper` und vorhandenes Notification-Theme | Obere Overlaydarstellung mit gleichen Rollen/Icons prüfen; Inhalte nicht verschieben |

Die vorhandenen Formular-/Panel-/Overlayregeln sind in
[Theme und Gestaltung](../../../../theme.md) beschrieben. Insbesondere lassen sich
native Felder über ihre öffentlichen CSS-Parts und die vorhandenen Cosmos-Tokens
gestalten. Overlays liegen zum Teil außerhalb des View-Unterbaums: ihre Klassen
und Zustände müssen ausdrücklich mitgestaltet werden, nicht über einen zufälligen
Vorfahrenselektor.

## Isoliertes Beispiel

[VisitorLayoutStudy.java](VisitorLayoutStudy.java) zeigt Anmelden und Registrieren
mit echten Vaadin-Feldern, den bestehenden Card-/Überschrifts-/Hintergrundkomponenten,
Textkatalog und `CompactDateInput`. [studie.css](studie.css) ergänzt nur Anordnung
unter `.visitor-layout-study`; die bestehenden Controltokens bleiben zuständig.
Es gibt keine Route, Spring-Registrierung, Dienste, Eventhandler für Anmeldung/
Registrierung, Datenbank oder Versand. Das Beispiel liegt bewusst außerhalb der
Anwendungsquellen und wird nicht mit ausgeliefert.

Eine spätere isolierte Vorschau könnte die Komponente in einer eigenen Test-UI
einbetten und das CSS dort laden. Das ist noch nicht eingerichtet. Die Klasse
übernimmt Katalogtexte beim Erzeugen; ein produktiver Sprachwechsel muss wie in
den bestehenden Views sämtliche dynamischen Texte aktualisieren. Die Studie
ersetzt weder Übersetzung noch Binder-, Rechte- oder Sicherheitslogik.

Reproduzierbarer Compile-Abgleich aus dem Projektroot, ohne Anwendung:

```powershell
.\mvnw.cmd --version
.\mvnw.cmd -o -pl app -am dependency:build-classpath -DincludeScope=compile '-Dmdep.outputFile=C:/Dev/Projects/VaadinSpringApp/target/designkritik-stitch/portal/vaadin-study-classpath.txt'
$studyClasspath = (Get-Content target/designkritik-stitch/portal/vaadin-study-classpath.txt -Raw).Trim()
& C:/Dev/Java/current/bin/javac.exe --release 25 -encoding UTF-8 -cp "app/target/classes;$studyClasspath" -d target/designkritik-stitch/portal/vaadin-study-classes docs/design/frontend-referenzen/quellen/vaadin-studie/VisitorLayoutStudy.java
```

Der letzte Schritt setzt bereits kompilierte aktuelle Anwendungsklassen voraus;
der Dependency-Aufruf erzeugt diese nicht. Hier waren sie vorhanden. Es wird
keine zusätzliche Abhängigkeit oder Version eingeführt. Der Compile-Abgleich
prüft Java-/API-Verfügbarkeit, keine Browserdarstellung und keine Fachfunktion.

## Grenzen und sinnvoller Pilot

Hohe Sicherheit besteht bei Raster, Typografie, Flächen, Buttons und Feldanordnung.
Zusätzliche Feinabstimmung braucht es bei Shadow-DOM-Feldteilen, dem internen
Datumspicker, ComboBox-Popup, Fokusdarstellung und portalierten Dialogen/Meldungen.
Ein normaler DatePicker muss nicht dieselbe innere DOM-Struktur wie die
HTML-Kalendervorschau haben. Tastaturbedienung und native Funktionen sollen bei
der Gestaltung erhalten bleiben.

Als kleinster späterer Pilot eignen sich öffentlicher Header, Anmeldekarte und
Registrierung mit Geschlecht/Datum. Damit werden die kritischen Bausteine sichtbar,
bevor die übrigen Statusseiten übertragen werden. Erst nach einer gesondert
freigegebenen Vaadin-Vorschau lassen sich Desktop, Smartphone, DE/EN und
Overlayzustände visuell beurteilen. Der Java-Compile ist dafür keine Abnahme.

Offizielle Grundlagen: [Komponenten](https://vaadin.com/docs/latest/components),
[DatePicker](https://vaadin.com/docs/latest/components/date-picker) und
[Dialog](https://vaadin.com/docs/latest/components/dialog). Die Online-Dokumentation
ist fortlaufend; für das Beispiel ist der tatsächliche Compile gegen die
Projektabhängigkeiten entscheidend. Es verwendet nur bereits im Projekt benutzte APIs.
