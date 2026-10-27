# Besucherfrontend mit Vaadin Flow

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
