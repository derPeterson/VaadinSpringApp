# Browsertests

Java 25, Docker Desktop mit Linux-Containern und Docker Compose ab 2.24.4 werden
benötigt. Playwright, JUnit und Allure sind im unabhängigen Maven-Modul definiert.
Der [aktuelle Prüfstand](../docs/status.md) enthält die tatsächlich ausgeführten
Prüfungen; der [Umfang](abdeckung.md) erklärt die Grenzen.

## Arbeiten an Browsertests

Vor dem Schreiben, Ändern oder Ausführen von Browserfällen diese Anleitung lesen
und anwenden; [Codestil](../docs/entwicklung.md#codestil) und
[Testgrundlagen](../docs/tests.md) gelten ergänzend.

Agenten starten Browserläufe nur mit ausdrücklicher Freigabe für den aktuellen
Auftrag. Der Auftrag „große Testrunde“ umfasst die vollständigen isolierten DE-/EN-
Läufe; dafür nicht erneut fragen. Das Bearbeiten einer Anleitung oder eines Skills
ist keine Testfreigabe.

Offene Browserergänzungen stehen zentral im [Testbedarf](../docs/testbedarf.md#browser-tests).
Vor neuen Fällen vorhandene Abdeckung prüfen; erledigte Einträge erst nach passender
Implementierung und erfolgreicher DE-/EN-Prüfung entfernen. Nicht erledigte oder
wegen einer Entscheidung wartende Aufgaben dort erhalten.

## Starten

```powershell
# Einmalig den passenden Chromium bereitstellen:
.\browser-tests\run.ps1 -InstallBrowsers
# Einmalig die separate Browser-Test-CA bewusst im Windows-Benutzerkonto einrichten:
.\browser-tests-zertifikat-einrichten.bat
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
| `-SetupHttps` / `-RemoveHttps` | Bewusste einmalige Test-CA-Einrichtung bzw. ausdrücklicher Rückbau, keine Browserfälle |

Standardauswahl ist `all`, `de`, ein Worker. Nach App-, Shared-, Text- oder
Styleänderungen zuerst ohne `-NoBuild` ausführen. Der Starter prüft Java 25 und
baut Shared und Browserquellen gemeinsam über die Root-POM mit
`-pl browser-tests -am`, bevor er Worker verteilt. Die Anwendung wird dabei nicht
auf dem Host gebaut. `-ReportingTests` führt die normalen Helfertests im selben
Reactor aus; der Klassenpfadaufbau für Journeys verwendet `package` und das
Dependency-Ziel mit übersprungenen Tests. Ein separates Shared-Install entfällt.
Keine zwei Starter
gleichzeitig gegen dieselben Ausgabeordner ausführen.

## Suites

| Suite | Inhalt | Fälle |
| --- | --- | ---: |
| `infrastructure` | Startseite, Bilder, Loginformular, Mailpit-Erreichbarkeit und HTTPS-/HTTP-Grenze | 4 |
| `https-night` | Infrastruktur und Remember-me: normale Profile, echter Chromium-/Serverneustart; mit zwei Workern gezielter Nachtlaufnachweis | 8 |
| `authentication` | Admin-/Benutzerlogin, Fehler, Gastzugriff und Logout | 7 |
| `login-attempts` | Drei Sperr-/Resetfälle plus sieben Anmelde- und vier Remember-me-Regressionsfälle | 14 |
| `session-limits` | Sitzungslimit, eigene Sitzungsverwaltung einschließlich Serverneustart und Login-/Remember-me-/Rechte-/Sitzungsregressionen | 37 |
| `registration` | Registrierung, Pflichtfelder und Korrektur ungültiger Eingaben | 3 |
| `verification` | Tatsächliche Aktivierungsmail, gültige, verbrauchte und ungültige Links | 3 |
| `password-reset` | Tatsächliche Resetmail, Passwortwechsel, Formularfehler, Links und Sprache neuer anonymer Anfragen | 7 |
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
| `configuration` | String-, Boolean- und Ganzzahlwerte, Sprachwechsel mit Feldfehlern, Offline-Speichern, native SideNav, Klick-Infos und Enter-Bedienung | 10 |
| `operations` | Persistenz ohne Bootstrap, zwei eigene SMTP-Ziele und echte begrenzte Transportausfälle mit Wiederanlauf | 3 |
| `admin-mail` | Formularfehler, Feldinfos/Sprachwechsel, sicherer mehrzeiliger Text und tatsächliche Unicode-SMTP-Mails | 3 |
| `token-expiry` | Echte Mindestdauer abwarten, erneuter Verifikationsversand, offenes Resetformular, Anfragefehler | 3 |
| `remaining-configuration` | Konfiguration, Adminmail und Tokenablauf gemeinsam | 16 |
| `remaining-coverage` | Remember-me/Status sowie weitere Konfiguration/Mailabläufe gemeinsam | 21 |
| `login-layout` | Login bei fünf Fenstergrößen, reale Fehler-/Erfolgsanmeldung und reduzierte Bewegung | 6 |
| `login-checks` | Loginlayout, bestehende Anmeldung/Zugriff, Präsentation/Tastatur und Remember-me gemeinsam | 22 |
| `cosmos-design` | Kontoansichten, mobiler Mailablauf, Copyright, Theme, Navigation sowie Infoicon-Ausrichtung in Konto/Admin bei Desktop/schmal | 13 |
| `fast` | Explizit markierte Kernfälle; Auswahl und Grenzen siehe unten | 34 |
| `maintenance` | Wartung, Rollenmatrix, erhaltene Sitzungen, alte Formulare, echte Mailtokens und Remember-me-Neustart | 9 |
| `notifications` | Versand, persönliche Inbox/Header-Vorschau, Löschaktionen und Versandarchiv | 8 |
| `all` | Alle 29 Testklassen einschließlich der Fast-Fälle | 161 |

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

## Parallelisierung

`-Workers 2` führt bis zu zwei Worker gleichzeitig aus; erlaubt sind 1–4.
Es starten höchstens so viele Worker wie ausgewählte Testklassen. Ganze Klassen
werden verteilt, jeder Worker besitzt eine eigene JVM, Anwendung, H2-Datenbank,
Mailpit-Instanz und Ports. Innerhalb eines Workers läuft JUnit sequenziell;
Threadparallelität mit gemeinsamem Fixture nicht als Ersatz aktivieren.

Die Suite `operations` verwendet zusätzlich ein zweites eigenes Mailpit ohne
Hostports. Sie prüft die beim Start gebundene SMTP-Konfiguration sowie echte
Verbindungsfehler mit Retry und terminalem Ende. Anwendungsneustarts verwenden
dieselbe eigene Datenbank und deaktivieren Bootstrap ausdrücklich.

Der Elternrunner kompiliert Shared-/Browserquellen und baut das Anwendungsimage
einmal vor der Verteilung. Alle Worker verwenden dieselbe unveränderliche Image-ID.
Das Build-Verbot während eines Laufs verbietet keine parallelen Worker, sondern
verhindert wechselnde oder entfernte Images bei laufenden Tests und Neustartfällen.
Die vollständige Runde läuft üblicherweise zuerst auf DE, anschließend EN mit
`-NoBuild`; beide Sprachläufe können jeweils die Worker-Parallelisierung nutzen.

Bei `-Coverage` stammen App-Klassen und das eingebettete Shared-JAR aus dem
tatsächlich gestarteten Container. Das Image enthält zusätzlich
`/app/coverage-sources.jar` mit den beim Imagebuild verwendeten
Java-Quellen beider Module; sie werden zusammen mit dem App-JAR aus demselben
Container übernommen. Der Bericht verwendet ausschließlich diese Klassen und
Quellen, keine lokalen `target/`- oder `src/`-Bestände. Alle Worker müssen dieselbe
Image-ID nachweisen; der Elternrunner übernimmt deren Messdaten und die Archive
des ersten Workers. Fehlende Shared-Klassen/-Quellen, fehlende Archive und bereits
vorhandene Extraktionsverzeichnisse führen zum Fehler statt zu Teilberichten oder
zur Wiederverwendung alter Messstände. Die optionale Browsermessung ersetzt keine
JUnit-Coverage und startet durch sich selbst keine Journeys.

## Isolation

Der Runner verwendet `compose.browser-tests.yaml` mit eindeutigem Projektnamen
und eigenen wegwerfbaren Volumes. Standardports sind 18081 für den HTTPS-Proxy,
18082 für HTTP-Weiterleitung und 18026 für Mailpit; Worker reservieren drei eigene
freie Ports. Die manuellen Zugänge 8443/18443/18080/18025 bleiben unberührt.
H2-Webkonsole und externe SMTP-Zustellung sind aus.

Der lokale HTTPS-Runner benötigt derzeit Windows. Die separate Browser-Test-CA wird
einmalig mit `browser-tests-zertifikat-einrichten.bat` im Projektroot eingerichtet.
Nur dieser bewusst gestartete Vorgang darf einen Windows-Bestätigungsdialog öffnen
(Zeitgrenze 120 Sekunden). Er verwendet ausschließlich `CurrentUser/Root` und ist
von der CA des IDE-/manuellen Zugangs getrennt. Eine vorhandene gültige Einrichtung
wird wiederverwendet. Das Vertrauen bleibt bis zum ausdrücklichen Rückbau bestehen.

Normale Läufe lesen den Zertifikatsspeicher ausschließlich. Fehlende, nicht vertraute,
beschädigte oder binnen eines Tages ablaufende CA-Einrichtung führt vor App-/Dockerstart
zum Fehler; nachts erfolgt kein Import und keine automatische Reparatur. Jeder Worker
erhält einen eigenen Schlüssel und ein für einen Tag gültiges Serverzertifikat für
`127.0.0.1`. Kette, Gültigkeit und Hostname werden vollständig geprüft, auch in eigenen
persistenten Chromium-Profilen. Es gibt keine TLS-Fehlerausnahme.

CA-Schlüssel und zufälliges Keystorepasswort liegen unter
`data/https/browser-tests-ca/`, außerhalb von Git und mit Dateirechten ausschließlich
für den aktuellen Benutzer. Die CA ist drei Jahre gültig; sie darf ausschließlich
für lokale Tests verwendet werden. Nur das Worker-Verzeichnis mit dessen eigenem
Serverzertifikat/-schlüssel und öffentlicher CA wird schreibgeschützt in seinen
Proxy eingebunden. Der CA-Schlüssel gelangt in keinen Container. Nach regulärem Ende,
Fehler oder vom Elternrunner erkanntem hartem Workerabbruch werden eigene
Worker-Schlüssel sowie Compose-Ressourcen entfernt; das Windows-Vertrauen bleibt gleich.

`browser-tests-zertifikat-entfernen.bat` entfernt ausdrücklich nur den eigenen,
durch Zertifikat und Besitzmarker nachgewiesenen Trust samt CA-Schlüsseldateien.
Ein Windows-Entfernungsdialog ist hierbei möglich. Einrichtung/Rückbau sind während
laufender Tests gesperrt. Für Erneuerung vor dem Ablauf bewusst entfernen und neu
einrichten; keinen privaten CA-Schlüssel teilen oder die Dateien vor dem Rückbau löschen.
Nach hartem Rechnerabbruch die eigene Umgebung anhand von `metadata.json` prüfen und
deren Compose-Ressourcen und Worker-Schlüssel gezielt abbauen. Keine fremden
Zertifikate oder systemweiten Stores bereinigen.

Die [Parallelisierungsregeln](#parallelisierung) gelten auch bei Neustartfällen:
Die verwendete Image-Referenz muss bis zum Abbau verfügbar bleiben. Ein noch
laufender Container allein genügt dafür nicht. Tests verwenden frische Browserkontexte,
erfundene Konten und echte UI-Aktionen. Kontoanlage/Änderungen niemals durch SQL oder Serviceaufrufe ersetzen.
Mailjourneys lösen ihre Nachrichten über diese UI aus, nicht über Mailgenerierungs-APIs.
`MailpitInbox` liest nur eigene Empfänger mit lokalisiertem Betreff; Mail-Links
müssen auf die genaue lokale Runner-Adresse zeigen. Konfigurationsfälle stellen
ihre Ausgangswerte wieder her. Das normale Fixture meldet sich über die UI ab,
damit sein Sitzungsplatz frei wird; Neustartfälle verwalten ihr Profil selbst.

Nach Abschluss oder Testfehler baut der Runner nur seine Container/Volumes ab.
Bei hartem Systemabbruch das konkrete eigene Projekt aus `metadata.json` prüfen;
keine globale Docker-Bereinigung ausführen.

Der explizite TLS-Negativtest startet nur eigene native Caddy-/Chromium-Prozesse,
keine Anwendung und keinen Trustimport. Er benötigt die eingerichtete Test-CA und
einen vorhandenen Caddy. Beispiel aus dem Projektroot:

```powershell
.\mvnw.cmd -pl browser-tests -am test '-Dtest=BrowserTlsVerificationTest' `
  '-Dsurefire.failIfNoSpecifiedTests=false' `
  '-Dbrowser.tests.excludedGroups=none' '-Dbrowser.tests.tlsProbe=true' `
  '-Dbrowser.tests.caddyExecutable=C:/Dev/Tools/caddy.exe'
```

Geprüft werden die Annahme der eigenen CA sowie die Ablehnung eines falschen
Hostnamens und einer frischen unvertrauten CA. Ohne ausdrückliche Aktivierung
gehört dieser Prozess-/Browsertest weder zu `-ReportingTests` noch zur Suite `all`.

## Aufbau und Pflege

| Bereich unter `browser-tests/src/test/java/de/derpeterson/browsertests/` | Aufgabe |
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
ebenfalls aus Shared beziehen: `AppRouteConstants`/`AppConstants` für vorhandene
Routen-/Titel-/Avatarverträge, `BrowserRoutes` für lokale Pfade statt Security-Wildcards.
Fehlende Übersetzungsschlüssel müssen fehlschlagen. Erfundenes Testmaterial von
übersetzten UI-Texten unterscheiden. In IntelliJ ausschließlich die Root-POM
verknüpfen; Shared und Browsermodul werden daraus gemeinsam importiert. Keine
zweiten Shared-Source-Roots anlegen.

Jeden Fall unabhängig mit ID, Bereich, Schritten und Erwartung beschreiben. Browser-
fälle und Berichte hängen nicht vom manuellen Katalog ab; `fast` bleibt eine Auswahl
derselben Implementierungen aus `all`. Bei neuen Fast-Tags die Fallliste oben pflegen.

## Berichte und Fehlerhilfe

`runs/` enthält pro Lauf `report.md`, `metadata.json`, `runner.log`, Allure-Daten,
Screenshots/Traces und `html/`. Metadaten halten Sprache, Quellstand, Image-ID und
Aufbau/Abbau fest. Bei Fehlern die erste konkrete Ursache und den Trace ansehen.
`html/open-report.bat` oder `-Report` verwenden; `index.html` benötigt einen lokalen
HTTP-Server. Chromium fehlt: `-InstallBrowsers`. Port-/Dockerfehler: eigene
Metadaten und verfügbare Ports prüfen. Falsches JDK nicht durch Downgrade umgehen.

Runs sind ignorierte lokale Diagnosedaten. Sie können Testeingaben und
Sitzungstoken enthalten und müssen nicht dauerhaft archiviert werden. Nicht mehr
benötigte ganze Runs dürfen auf entsprechenden Benutzerauftrag nach bewusster
Auswahl gelöscht werden; keine automatische Löschroutine. Der dauerhafte Prüfstand
steht knapp in [docs/status.md](../docs/status.md), ohne eine Liste alter Run-Ordner.
