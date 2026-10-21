# Browsertests

Java 25, Docker Desktop mit Linux-Containern und Docker Compose ab 2.24.4 werden
benötigt. Playwright, JUnit und Allure sind im unabhängigen Maven-Modul definiert.
Der [aktuelle Prüfstand](../docs/status.md) enthält die tatsächlich ausgeführten
Prüfungen; der [Umfang](abdeckung.md) erklärt die Grenzen.

## Starten

```powershell
# Einmalig den passenden Chromium bereitstellen:
.\browser-tests\run.ps1 -InstallBrowsers
# Helfer und Reporter ohne App-Container:
.\browser-tests\run.ps1 -ReportingTests
# Gesamtprüfung, einmal bauen und dasselbe Image in beiden Sprachen verwenden:
.\browser-tests\run.ps1 -Suite all -Workers 2 -Language de
.\browser-tests\run.ps1 -Suite all -Workers 2 -Language en -NoBuild
```

| Option | Bedeutung |
| --- | --- |
| `-Suite fast` | 34 ausdrücklich markierte Kernfälle |
| `-Suite NAME` | Fachliche Auswahl aus der Tabelle |
| `-Language de/en` | Genau eine über die UI gewählte Sprache |
| `-Workers 1..4` | Getrennte JVM-/Docker-/H2-/Mailpit-Umgebungen; Standard 1, lokal meist 2 |
| `-NoBuild` | Vorhandenes Image verwenden; muss zum Quellstand passen |
| `-Headed` | Browser sichtbar anzeigen |
| `-Coverage` | Optional serverseitigen Java-Code messen; für reine Funktionsabnahme nicht erforderlich |
| `-Report` | Letzten Allure-Bericht lokal öffnen, keine Tests starten |
| `-ReportPath PFAD` | Bestimmten vorhandenen HTML-Bericht öffnen |
| `-Trace PFAD` | Gespeicherte Playwright-Trace-ZIP öffnen |
| `-FailureProbe` | Eigenständiger absichtlich falscher Titelvergleich; Exitcode 1 erwartet |

Standardauswahl ist `all`, `de`, ein Worker. Nach App-, Shared-, Text- oder
Styleänderungen zuerst ohne `-NoBuild` ausführen. Der Starter baut Shared und
kompiliert die Browserquellen einmal, bevor er Worker verteilt. Keine zwei Starter
gleichzeitig gegen dieselben Ausgabeordner ausführen.

## Suites

| Suite | Inhalt | Fälle |
| --- | --- | ---: |
| `infrastructure` | Startseite, Bilder, Loginformular, Mailpit-Erreichbarkeit | 3 |
| `authentication` | Admin-/Benutzerlogin, Fehler, Gastzugriff und Logout | 7 |
| `login-attempts` | Drei Sperr-/Resetfälle plus sieben Anmelde- und vier Remember-me-Regressionsfälle | 14 |
| `session-limits` | Sitzungslimit, eigene Sitzungsverwaltung einschließlich Serverneustart und Login-/Remember-me-/Rechte-/Sitzungsregressionen | 37 |
| `registration` | Registrierung, Pflichtfelder und Korrektur ungültiger Eingaben | 3 |
| `verification` | Tatsächliche Aktivierungsmail, gültige, verbrauchte und ungültige Links | 3 |
| `password-reset` | Tatsächliche Resetmail, Passwortwechsel, Formularfehler und Links | 6 |
| `account-security` | Passwortvalidierung/-wechsel mit SMTP-Mail und verifizierte E-Mail-Änderung mit beiden Sicherheitsmails | 4 |
| `session-persistence` | Gezielte Neustartabnahme: Sitzungs-/Loginfälle, Wartung und eigene Kontosicherheit | 50 |
| `session-management` | Eigene Sitzungen, Einzel-/Sammelwiderruf, dauerhafte Anmeldung, UI-Neuaufbau und eingeschränkter Login | 6 |
| `account-settings` | Profil/Sprachen/E-Mail-Dialog, persistente Profilbilder Desktop/schmal, kompakte Geburtstage und Status am Kontobild | 12 |
| `account-lifecycle` | Einzelvalidierungen, kanonisierte Dubletten, unverifizierte Anfrage und altes Resetformular | 5 |
| `presentation` | Sprachwechsel, gespeicherte Sprache, schmale Registrierung mit Datumeingabe/Kalenderauswahl und Tastaturlogin | 5 |
| `token-configuration` | Vier Mindestdauern, Feldfehler, Speicherung und Erklärungen | 4 |
| `user-management` | Suche/Pagination, Filterentwürfe/Badges, Anlage, Bearbeitung, sichere Dialogaktionen und Passwortbestätigung und kompakte Zeit | 15 |
| `user-filters` | Entwürfe/Anwenden/Badges, Kalender/Datumabhängigkeit, Zeitoverlay und kombinierte Filter/Reset und kompakte Zeit mit Sprachwechsel | 5 |
| `user-access` | Sperren/Freigeben, Rollenvergabe/-entzug mit und ohne bestehende Sitzung, Superadmin-/Gastzugriff, unverifizierte Konten, zwei offene Formulare und Selbstschutz | 8 |
| `session-security` | Alte Speichern-Aktion nach Rollenentzug/Sperre/Löschung, Sperre/Löschung auch mit Remember-me | 5 |
| `blocked-links` | Alte Verifikations-/Resetbuttons nach Adminänderung; neutrale Anfrage gesperrter Adresse | 5 |
| `security-journeys` | Sitzungs- und Maillinkfälle gemeinsam | 10 |
| `remember-me` | Echter Chromium- und Anwendungsneustart mit/ohne Remember-me und nach Logout | 4 |
| `user-status` | Manuelle Wahl, Live-Anzeige in zweitem Tab, Abwesenheit/Rückkehr | 2 |
| `configuration` | String-, Boolean- und Ganzzahlwerte, native SideNav, alle 30 Klick-Infos, mobile Tooltipbreite und sichere Enter-Bedienung | 9 |
| `admin-mail` | Formularfehler, Feldinfos/Sprachwechsel, sicherer mehrzeiliger Text und tatsächliche Unicode-SMTP-Mails | 3 |
| `token-expiry` | Echte Mindestdauer abwarten, erneuter Verifikationsversand, offenes Resetformular, Anfragefehler | 3 |
| `remaining-configuration` | Konfiguration, Adminmail und Tokenablauf gemeinsam | 15 |
| `remaining-coverage` | Remember-me/Status sowie weitere Konfiguration/Mailabläufe gemeinsam | 21 |
| `login-layout` | Login bei fünf Fenstergrößen, reale Fehler-/Erfolgsanmeldung und reduzierte Bewegung | 6 |
| `login-checks` | Loginlayout, bestehende Anmeldung/Zugriff, Präsentation/Tastatur und Remember-me gemeinsam | 22 |
| `cosmos-design` | Kontoansichten, mobiler Mailablauf, Copyright, Theme, Navigation sowie Infoicon-Ausrichtung in Konto/Admin bei Desktop/schmal | 13 |
| `fast` | Explizit markierte Kernfälle; Auswahl und Grenzen siehe unten | 34 |
| `maintenance` | Wartung, Rollenmatrix, erhaltene Sitzungen, alte Formulare, echte Mailtokens und Remember-me-Neustart | 9 |
| `notifications` | Versand, persönliche Inbox/Header-Vorschau, Löschaktionen und Versandarchiv | 8 |
| `all` | Alle 28 Testklassen einschließlich der Fast-Fälle | 155 |

IDs, Schritte und erwartetes Ergebnis stehen pro Fall in `@DisplayName` und
`@Description`; diese Angaben übernimmt der Bericht. AVATAR-01 enthält zwei
Fenstergrößen, AVATAR-02 prüft beschädigten Upload und anschließende Korrektur.
Sitzungsfälle führen echte Browser-/Serverneustarts mit erhaltenem Testprofil aus.
Tokenablauffälle warten die tatsächliche Mindestdauer ab.

## Schnellprüfung

`fast` verwendet dieselben Implementierungen wie `all`, ausgewählt durch
`@Tag("fast")`. Es erkennt keine geänderten Dateien automatisch.

| Bestehende Fälle im Schnelllauf | Warum enthalten |
| --- | --- |
| INFRA-01/02 | Start, Ressourcen und echtes Loginformular |
| AUTH-01A–D, AUTH-02A, SEC-01A | Anmeldung, Fehler, Gast-/Benutzerrechte und Logout |
| REG-02A/B | Pflichtfelder, E-Mail und Passwortbestätigung |
| VER-01A, RESET-01A | Echte SMTP-Mail, Aktivierung und Passwortreset bis zur Anmeldung |
| LANG-01C, UI-01B, UI-02A | Sprache, Kalenderbedienung und Tastaturlogin |
| COSMOS-01A–E, COSMOS-04, COSMOS-05A–C | Kontoansichten und Home/Admin bei schmalen und breiten Fenstern, Theme und Navigation |
| USER-06B/C, USER-12A/B | Bearbeiten, sicheres Löschen/Abbrechen und Passwortvalidierung beim richtigen Ereignis |
| USER-11A/B | Filterentwürfe, Anwenden, Badges, Datumabhängigkeit und Zeitoverlay |
| CONFIG-05A/B/E | SideNav, Feldinfo und Speichern per Enter |
| MAIL-01 | Formularfehler und tatsächlich empfangene Unicode-Testmail |

Nicht enthalten sind die vollständige Rechte-/Sitzungsmatrix, Tokenwartezeiten,
Chromium-Neustarts, alle Konfigurationsvarianten und die große Pagination-Anlage.
Nach Sicherheits- oder Kontovertragsänderungen die passende Suite bzw. `all` nutzen.

## Isolation

Der Runner verwendet `compose.browser-tests.yaml` mit eindeutigem Projektnamen
und eigenen wegwerfbaren Volumes. Standardports sind 18081 für die Anwendung und
18026 für Mailpit; Worker reservieren eigene freie Ports. Die manuelle Umgebung
18080/18025 bleibt unberührt. H2-Webkonsole und externe SMTP-Zustellung sind aus.

Der Elternrunner baut einmal und übergibt die unveränderliche Image-ID an alle
Worker. Während eines Laufs keine Appimages neu bauen, Tags umhängen oder
benutzte Images entfernen: Neustartfälle benötigen dieselbe verfügbare Referenz
bis zum Abbau. Ganze Klassen werden verteilt, innerhalb jedes Workers läuft JUnit
sequenziell. Tests verwenden frische Browserkontexte, erfundene Konten und echte
UI-Aktionen. Kontoanlage/Änderungen niemals durch SQL oder Serviceaufrufe ersetzen.
`MailpitInbox` liest nur eigene Empfänger mit lokalisiertem Betreff; Mail-Links
müssen auf die genaue lokale Runner-Adresse zeigen. Konfigurationsfälle stellen
ihre Ausgangswerte wieder her. Das normale Fixture meldet sich über die UI ab,
damit sein Sitzungsplatz frei wird; Neustartfälle verwalten ihr Profil selbst.

Nach Abschluss oder Testfehler baut der Runner nur seine Container/Volumes ab.
Bei hartem Systemabbruch das konkrete eigene Projekt aus `metadata.json` prüfen;
keine globale Docker-Bereinigung ausführen.

## Aufbau und Pflege

| Bereich unter `src/test/java/de/derpeterson/browsertests/` | Aufgabe |
| --- | --- |
| `suites/` | Unabhängige fachliche Fälle |
| `pages/` | Wiederkehrende echte UI-Aktionen, Vaadin-Wartebedingungen |
| `mail/` | Eigene SMTP-Mails und lokale Links |
| `i18n/UiTexts` | Gemeinsame MessageTexts-Getter mit fester Locale |
| `BrowserFixture`, `BrowserActor` | Kontexte, Akteure, Belege |
| `BrowserSuites`, `BrowserExecutionPlan` | Auswahl und eindeutige Klassenverteilung |
| `BrowserTestRunner`, `BrowserParallelWorkers` | Docker, Prozesse, Abbau, Berichte |
| `BrowserCoverage`, `MarkdownSummary` | Optionale Messung und Berichtsauswertung |

Neue Journeys in die fachliche Suite einordnen, technische Konkurrenz-/Rollback-
und Grenzregeln in Anwendungs-JUnit prüfen. Wartende Assertions statt beliebiger
Schlafzeiten nutzen. Keine automatischen Wiederholungen oder abgeschwächten
Erwartungen, um Fehler zu verdecken. Fehlende oder übersprungene Fälle und
fehlgeschlagener Abbau sind kein grüner Gesamtlauf.

Übersetzungen kommen aus dem einzigen Shared-Katalog und den originalen
Properties; feste Labels über UiTexts beziehen. Keine gleichzeitige Akzeptanz
beider Sprachen. Nach Login normalerweise die Laufsprache wählen; ein Test der
gespeicherten Präferenz setzt sie bewusst nicht erneut. Routen und Konstanten
ebenfalls aus Shared beziehen. In IntelliJ Shared und Browser-POM als eigene
Maven-Projekte importieren, keine zweiten Shared-Source-Roots anlegen.

## Berichte und Fehlerhilfe

`runs/` enthält pro Lauf `report.md`, `metadata.json`, `runner.log`, Allure-Daten,
Screenshots/Traces und `html/`. Metadaten halten Sprache, Quellstand, Image-ID und
Aufbau/Abbau fest. Bei Fehlern die erste konkrete Ursache und den Trace ansehen.
`html/open-report.bat` oder `-Report` verwenden; `index.html` benötigt einen lokalen
HTTP-Server. Chromium fehlt: `-InstallBrowsers`. Port-/Dockerfehler: eigene
Metadaten und verfügbare Ports prüfen. Falsches JDK nicht durch Downgrade umgehen.

Runs sind ignorierte lokale Diagnosedaten. Sie können Testeingaben und
Sitzungstoken enthalten und müssen nicht dauerhaft archiviert werden. Nicht mehr
benötigte ganze Runs dürfen nach bewusster Auswahl gelöscht werden; keine
automatische Löschroutine. Der dauerhafte Prüfstand steht knapp in docs/status.md,
ohne eine Liste alter Run-Ordner.
