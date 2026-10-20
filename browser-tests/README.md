# Browsertests mit Java, JUnit und Allure

Diese Anleitung erklärt die Bedienung und den Aufbau der Browsertests.
Den aktuellen Prüfstand, offene Fehler und die Zuordnung der ursprünglichen
manuellen Fälle findest du ausschließlich in [Abdeckung und offene Punkte](abdeckung.md).
Ein vorhandener Testfall bedeutet noch nicht, dass sein Ablauf erfolgreich geprüft ist.

Aktuelle Bootstrap-Listen: Der isolierte Compose-Override enthält einen gewöhnlichen
Admin und einen getrennten Superadmin. Nur Fälle, die Superadmin vergeben müssen, verwenden
das zweite Konto. Die Rollenfälle berücksichtigen Live-Aktualisierung vorhandener Sitzungen; der tatsächliche aktuelle Nachweis steht in abdeckung.md. [Rollen-/Bootstrap-Vertrag](../docs/role-updates-and-bootstrap.md).

## Starten

Voraussetzungen: Java 25, Docker Desktop mit Linux-Containern und Docker Compose
ab 2.24.4. Im Projektordner den vorhandenen Maven Wrapper verwenden.
Eine globale Playwright- oder Allure-Installation ist nicht erforderlich.

```powershell
# Einmalig: Bibliotheken und passenden Chromium-Testbrowser bereitstellen.
.\browser-tests\run.ps1 -InstallBrowsers

# Alle Suites auf Deutsch, mit aktuellem Anwendungsimage.
.\browser-tests\run.ps1 -Suite all -Language de -Coverage

# Dasselbe unveränderte Anwendungsimage anschließend auf Englisch prüfen.
.\browser-tests\run.ps1 -Suite all -Language en -NoBuild -Coverage
```

| Aktion | Befehl |
| --- | --- |
| Schnelle Kernprüfung in zwei isolierten Umgebungen | `.\browser-tests\run.ps1 -Suite fast -Workers 2 -Language de` |
| Alle Fälle auf zwei Umgebungen verteilen | `.\browser-tests\run.ps1 -Suite all -Workers 2 -Language de` |
| Tests live im sichtbaren Browser verfolgen | `.\browser-tests\run.ps1 -Headed` |
| Login-Schutz, bestehende Anmeldung und Remember-me prüfen | `.\browser-tests\run.ps1 -Suite login-attempts -Workers 2 -Language de` |
| Sitzungslimit und zugehörige Sicherheitsabläufe prüfen | `.\browser-tests\run.ps1 -Suite session-limits -Workers 2 -Language de` |
| Wartungsmodus und Wiederfreigabe prüfen | `.\browser-tests\run.ps1 -Suite maintenance -Language de -Coverage` |
| Nur Benutzerfilter prüfen | `.\browser-tests\run.ps1 -Suite user-filters -Language de` |
| Nur Benutzerverwaltung prüfen | `.\browser-tests\run.ps1 -Suite user-management -Language de` |
| Helper und Reporter ohne Docker prüfen | `.\browser-tests\run.ps1 -ReportingTests` |
| Letzten Allure-Bericht öffnen | `.\browser-tests\run.ps1 -Report` |
| Bestimmten Bericht öffnen | `.\browser-tests\run.ps1 -Report -ReportPath '.\browser-tests\runs\DEIN-LAUFORDNER\html'` |
| Gespeicherten Trace öffnen | `.\browser-tests\run.ps1 -Trace 'C:\PFAD\ZUM\trace.zip'` |
| Absichtlich einen roten Bericht erzeugen | `.\browser-tests\run.ps1 -NoBuild -FailureProbe` |

`-Suite`, `-Language`, `-Workers`, `-Headed`, `-NoBuild` und `-Coverage` lassen sich kombinieren.
Standardwerte sind `all`, `de` und `-Workers 1` (sequenziell). `-NoBuild` verwendet das vorhandene App-Image;
nach Änderungen an Anwendung, gemeinsamen Quellen, Übersetzungen oder Styles
zuerst einen Lauf mit Image-Build starten. Sonst kann der Browser einen alten Stand prüfen.

`-FailureProbe` führt ausschließlich einen absichtlich falschen Titelvergleich aus.
Exitcode 1 und ein roter Fall sind dabei erwartet. Er gehört nicht zum normalen Lauf.
Auch bei Testfehlern wird die eigene Testumgebung abgebaut.

## Suites

Alle Fälle sind eigenständige JUnit-Tests. Jeder bekommt einen frischen Browserkontext;
die isolierte Datenbank und Mailpit sind pro Lauf frisch. Benötigte Konten werden im
jeweiligen Fall angelegt, ohne von der Reihenfolge anderer Fälle abzuhängen.

| Suite | Inhalt | Fälle |
| --- | --- | ---: |
| `infrastructure` | Startseite, Bilder, Loginformular, Mailpit-Erreichbarkeit | 3 |
| `authentication` | Admin-/Benutzerlogin, Fehler, Gastzugriff und Logout | 6 |
| `login-attempts` | Drei Sperr-/Resetfälle plus sechs Anmelde- und vier Remember-me-Regressionsfälle | 13 |
| `session-limits` | Sitzungslimit, eigene Sitzungsverwaltung und Login-/Remember-me-/Rechte-/Sitzungsregressionen | 35 |
| `registration` | Registrierung, Pflichtfelder und Korrektur ungültiger Eingaben | 3 |
| `verification` | Tatsächliche Aktivierungsmail, gültige, verbrauchte und ungültige Links | 3 |
| `password-reset` | Tatsächliche Resetmail, Passwortwechsel, Formularfehler und Links | 6 |
| `account-security` | Passwortvalidierung/-wechsel mit SMTP-Mail und verifizierte E-Mail-Änderung mit beiden Sicherheitsmails | 4 |
| `session-management` | Eigene Sitzungen, Einzel-/Sammelwiderruf, gespeicherte Zugänge nach Neustart und eingeschränkter Login | 5 |
| `account-settings` | Profil/Sprachen/E-Mail-Dialog, persistente Profilbilder Desktop/schmal, kompakte Geburtstage und Status am Kontobild | 11 |
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
| `login-checks` | Loginlayout, bestehende Anmeldung/Zugriff, Präsentation/Tastatur und Remember-me gemeinsam | 21 |
| `cosmos-design` | Kontoansichten, mobiler Mailablauf, Copyright, Theme, Navigation sowie Infoicon-Ausrichtung in Konto/Admin bei Desktop/schmal | 13 |
| `fast` | Explizit markierte Kernfälle; Auswahl und Grenzen siehe unten | 34 |
| `maintenance` | Wartung, Rollenmatrix, erhaltene Sitzungen, alte Formulare, echte Mailtokens und Remember-me-Neustart | 9 |
| `notifications` | Versand, persönliche Inbox/Header-Vorschau, Löschaktionen und Versandarchiv | 8 |
| `all` | Alle 28 Testklassen einschließlich der Fast-Fälle | 152 |

AVATAR-01 prüft zwei eigenständige Parameterfälle bei 1280×900 und 390×844:
PNG hochladen, Ausschnitt/Zoom, Uploadfläche ausblenden, gleich große lokale Aktionen,
Speichern und echte Bilder in Kopfzeile/Popup/Konto/Benutzerverwaltung, Wiederöffnen im
richtigen Modus, lokales und Footer-Abbrechen, Initialen/Bild sowie JPEG-Ersetzen und Löschen.
INPUT-01 prüft kompakte Geburtstage mit vier/sechs/acht Ziffern, 1950/2049, ungültigen Tag
und Tab/Enter im tatsächlichen Frontend. INPUT-02 prüft 930/1430 mit Tab/Enter,
Sprachwechsel und ungültiger Zeit im echten Aktivitätsfilter. STATUS-03 prüft die direkte
Wahl am Kontobild und synchronisierte Kontursymbole, Statusrahmen und Glow.
Die detaillierten Schritte stehen in den Testbeschreibungen; Ausführungsergebnisse
stehen getrennt in der Abdeckungsübersicht. Die 34 Fast-Fälle bleiben unverändert.

Die sieben bisherigen Account-Fälle prüfen Profilspeichern und Dialogschließen.
Die neuen Suites `account-security` und `session-management` ergänzen neun eigenständige
Journeys ACCOUNT-SEC-01–04 und SESSIONS-01–05. Sie erstellen Zustände ausschließlich
über UI und eigene abgefangene Mailpit-Mails; Sicherheitsmails folgen der gespeicherten
Kontosprache. Ein Mail-Linkaufruf allein darf die neue Adresse nicht übernehmen;
Bestätigung, beide Benachrichtigungen, Sitzungswiderruf und Einmaligkeit werden geprüft.
Gespeicherte Zugänge ohne aktive Sitzung belegen keinen Platz. SESSIONS-01 prüft
zusätzlich zwei gleichzeitig geöffnete Fenster: Ein Feldfehler bleibt erhalten,
während das andere Fenster erfolgreich speichert und seinen eigenen Hinweis schließt.
Die tatsächlichen Ergebnisse stehen in [Abdeckung](abdeckung.md), nicht in der bloßen Fallzählung.

Gezielt ausführen: `.\browser-tests\run.ps1 -Suite account-security -Language de`
beziehungsweise `-Suite session-management`. Beide sind in `all` enthalten und
bewusst noch nicht in `fast`; dessen 34 Fälle bleiben unverändert.

SESSION-LIMIT-01–04 prüfen das gespeicherte Sitzungslimit über echte Anmeldungen:
zusätzliche Tabs belegen keinen Platz, Logout gibt einen frei, Reduzierung erhält
bestehende Geräte und Erhöhung wirkt sofort. Ein echter Chromium-Prozess wird
beendet und neu gestartet, um Browserschließen ohne Logout und die begrenzte
Remember-me-Rückkehr zu prüfen. Die gemeinsame Suite enthält mit den fünf Verwaltungsfällen 34 Fälle; `fast`
bleibt unverändert bei 34. Timeout- und Konkurrenzgrenzen prüft JUnit.

Das gemeinsame Fixture meldet sein Konto nach dem abschließenden Screenshot über
die Oberfläche ab. Bloßes Schließen des Testkontextes würde korrekt einen Platz bis
zum Timeout belegen und die wiederverwendete Bootstrap-Identität über mehrere
unabhängige Fälle blockieren. Spezielle Browserneustart-Fälle behalten ihr eigenes
Profil und schließen ausdrücklich ohne diese Fixture-Abmeldung.

AUTH-LIMIT-01–03 prüfen gespeichertes Versuchslimit, erhaltene angemeldete Sitzung,
sofortige Reduzierung für bekannte/unbekannte Kennungen und Entsperrung erst nach
abgeschlossenem echten Passwortreset. Die Suite enthält auch reale Browser- und Anwendungsneustarts
für Remember-me. Zeitgrenzen und Konkurrenz sind JUnit-Aufgabe; der Browser wartet
keine 15 Minuten. `fast` bleibt bei seinen bisherigen 34 Fällen.

UI-01A prüft die direkte Datumseingabe mit lokalisiertem Beispiel und anschließendem
Registrieren ohne Enter/Escape. UI-01B prüft getrennt die Auswahl über das Kalender-Icon,
das markierte Datum beim Wiederöffnen und das tatsächliche Absenden. Ein Feldklick
öffnet bei der Registrierung kein Overlay. Aktuelle Nachweise stehen in `abdeckung.md`.

Datum und Uhrzeit im Aktivitätsfilter öffnen nur über Icon/Pfeiltaste. Der Button
„Filter“ schaltet alle permanenten Filterfelder ein/aus. Die Helfer unterscheiden
`selectDraftFilter` und `applyFilters`; `selectFilter` führt beide echten UI-Aktionen
aus. Ein Badge-X entfernt nur das angewandte Kriterium. Reset leert auch Entwürfe,
blendet die Felder aber nicht aus. Platzhalter sind das heutige Datum und die
aktuelle abgerundete Viertelstunde. Ohne gültiges Datum ist die Zeit deaktiviert.
Das Geburtstagsfeld nutzt den Platzhalter der Registrierung.

`AdminUserDialog.fillNewUser` füllt Passwort und Bestätigung. Neue Fälle USER-11A/B
prüfen Entwurfszustand, einzelne Badge-Aktionen, Datumabhängigkeit, begrenztes
Zeitoverlay und Scrollen ohne Auswahl. USER-12A/B prüfen den Zeitpunkt der
Passwortfehler sowie die beiden sicheren Schließen-Aktionen der Dialoge.

Die vollständigen Schritte und Erwartungen stehen pro Fall in `@Description`
und werden automatisch in den Markdown-Bericht übernommen. Die Test-ID erscheint
in `@DisplayName`, im Allure-Bericht und in der Abdeckungsübersicht.


Der Wartungsmodus hat neun eigene Fälle MAINT-01 bis MAINT-05 einschließlich
Rollen- und Linkvarianten. Sie prüfen echte HTTP-503-Dokumente, erhaltene Sitzungen,
Admin-/Superadmin-Wiederfreigabe, alte Registrierungs-/Resetformulare, unverbrauchte
SMTP-Mailtokens und einen tatsächlichen Remember-me-Chromium-Neustart. `all` enthält
sie; `fast` bleibt bei 34 Fällen. Fachregeln und Grenzen stehen im
[Wartungsvertrag](../docs/maintenance-mode-contract.md). Der zusätzliche Helper prüft
Suite-Aufnahme und unveränderten Fast-Umfang; die aktuelle Helper-Suite umfasst 47 Fälle.

## Schnellprüfung und parallele Ausführung

`fast` beantwortet: Funktionieren die wichtigsten sichtbaren Abläufe nach einer
kleinen Änderung noch? Die Auswahl besteht aus vorhandenen Fällen mit JUnit
`@Tag("fast")`. `all` führt dieselben Fälle und alle übrigen aus; es gibt keine
zweite, abweichende Implementierung der Kernprüfungen. Im Bericht sind die
Schnellprüfung und ihre Grenzen ausdrücklich gekennzeichnet. Der `fast`-Tag ist
auch im Allure-Bericht sichtbar und dort filterbar.

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

Nicht enthalten sind unter anderem die echten Tokenablauf-Wartezeiten, automatische
Abwesenheit, Chromium-Neustarts für Remember-me, die vollständige Rechte-/Sitzungsmatrix,
alle Konfigurationsvarianten und die große Pagination-Anlage. Das macht diese Prüfungen
nicht unwichtig: Vor Freigaben sowie nach Änderungen an Sicherheit, Konfiguration oder
Kontolebenszyklus `all` beziehungsweise die passende fachliche Suite ausführen.
`fast` erkennt auch nicht automatisch, welche Dateien geändert wurden. Neue Fälle bewusst
nach häufigem Fehlerrisiko, schnellem Ablauf und zusätzlichem Erkenntnisgewinn markieren.
Bei einem parametrisierten markierten Fall werden **alle** seine Varianten ausgeführt.

`-Workers 2` verteilt ganze Testklassen auf zwei separate Java-Prozesse. Jeder hat
seinen eigenen Chromium, Docker-Projektnamen, H2-Datenbestand, Mailpit und freie lokale
Ports. Innerhalb eines Workers laufen die Fälle nacheinander: Globale Konfiguration,
Kontenzahlen und statische Playwright-Objekte werden niemals zwischen gleichzeitig
laufenden Fällen geteilt. Die Gruppierung verwendet grobe feste Klassengewichte und
benötigt keine alten Run-Ordner. Bei einer einzelnen Klasse startet nur ein Worker.

Zulässig sind 1 bis 4 Worker; für den lokalen Rechner zunächst 2 verwenden. Mehr Worker
benötigen zusätzliche CPU/RAM und sind nicht automatisch schneller. Die beiden eigenen
Ports werden vor dem Start geprüft/reserviert und an Docker übergeben. Bei einer
externen Kollision scheitert der Aufbau, bevor die Tests starten. Im Parallelmodus
werden die manuell gesetzten `BROWSER_TEST_APP_PORT`/`BROWSER_TEST_MAILPIT_PORT` durch
diese eigenen Worker-Ports ersetzt; sequenziell behalten sie ihre bisherige Bedeutung.

Der Elternrunner baut das App-Image einmal und gibt dessen unveränderliche Image-ID
an alle Worker weiter. `-NoBuild` überspringt nur den Image-Build und muss zum Quellstand
passen. Der Starter kompiliert Shared/Testcode einmal vor dem Aufteilen. Zwei parallele
Starter gegen denselben Arbeitsordner sind dafür nicht erforderlich und sollen wegen
der gemeinsamen Maven-Ausgabeordner vermieden werden.

Ein Run-Ordner enthält einen gemeinsamen Markdown-/Allure-Bericht und unter `workers/`
jeweils Metadaten und Konsolenlogs. Screenshots und Traces werden in den gemeinsamen
Nachweisordner verschoben, nicht verdoppelt; HTML wird nur einmal erzeugt. Exitcode,
Fallzahlen, Nachweise, Image-ID und Abbau jedes Workers müssen stimmen. Fehlende,
doppelte, übersprungene oder fehlgeschlagene Fälle werden nicht als Erfolg gewertet.
Es gibt keine automatischen Wiederholungen, die einen ersten Fehlschlag verdecken.
`-Coverage` vereinigt die JaCoCo-Rohdaten aller Worker und berichtet gegen dasselbe
geprüfte JAR; Prozentwerte werden niemals gemittelt oder addiert.

Normaler Abschluss und Fehler bauen nur die eigenen Container/Volumes ab. Beim
kontrollierten Eltern-Abbruch beendet dieser auch seine Worker-Prozesse und räumt deren
konkrete Compose-Projekte auf. Bei hartem Betriebssystemabbruch kann kein Programm
Aufräumen garantieren: dann ausschließlich die eigenen `project`-Werte aus
`workers/*/metadata.json` verwenden. Keine globale Docker-Bereinigung.

### Gemessener Schnelllauf und Prüfumfang (15.10.2026)

Einzelmessung auf dem lokalen Windows-Rechner, Java 25 und identisches App-Image;
keine zugesicherte Dauer und kein Langzeitmittel:

| DE `fast` | Sequenziell | Zwei Worker |
| --- | ---: | ---: |
| Genau dieselben Browserfälle | 34/34 | 34/34 |
| Zeitspanne erster Fallstart bis letztes Fallende | 3:53 min | 2:16 min |
| Gesamter Java-Runner einschließlich Docker-Aufbau/-Abbau und Bericht | 4:21 min | 2:42 min |

Die reine Fallspanne sank in diesem Vergleich um rund 41 %. Der erste Lauf rief
den Image-Build mit vorhandenem Cache auf, der parallele Vergleich nutzte `-NoBuild`;
beide prüften dieselbe Image-ID. Die Vorbereitungszeit des PowerShell-Starters
(Shared/Testcode kompilieren) liegt vor der Runner-Messung und ist nicht enthalten.
Mehrere Worker verbrauchen mehr CPU/RAM; bei anderer Last können die Zeiten abweichen.

Auch `all -Workers 2 -Language de` ist mit 100/100 Fällen erfolgreich: **12:59 min**
für den Java-Runner, davon **12:26 min** Fallspanne. Beide Worker prüften dasselbe
Image, der normale Image-Build wurde einmal mit Cache aufgerufen. Alle 100
Fall-/Parameteridentitäten stimmen mit dem vorherigen vollständigen DE-Lauf überein;
kein Fall fehlt und keiner läuft doppelt. Ein neuer sequenzieller `all`-Vergleich
wurde hierfür nicht gestartet.

Zusätzlich sind 34 englische Fast-Fälle mit zwei Workern und zusammengeführter
Coverage erfolgreich. 43 Helper-/Reportertests prüfen unter anderem die Tag-Auswahl,
die eindeutige Klassenverteilung, zulässige Reihenfolgeunterschiede, fehlende/mehrfache
Belege und die Fehlerdarstellung. Ein absichtlicher Fehlerfall liefert den erwarteten
Exitcode 1 samt rotem Bericht und Abbau. Ein temporärer QA-Helfer löste nach dem Aufbau
beider Worker einen kontrollierten JVM-Abbruch aus: beide Projekte wurden entfernt,
der Lauf als `interrupted` mit Endzeit gespeichert. Dies ist kein Nachweis für das
Aufräumen nach hartem Betriebssystem-/Rechnerabbruch. Vollständiger Prüfstand:
[Abdeckung](abdeckung.md).

## Code-Coverage der Browserabläufe

`-Coverage` startet ausschließlich die eigene Docker-Testanwendung mit einem
JaCoCo-Agenten. Der Agent wird schreibgeschützt eingebunden; zusätzliche Ports
oder Zugriff auf eine bestehende Instanz sind nicht erforderlich. Nach den Tests
wird die eigene App geordnet gestoppt, die Ausführungsdatei vor dem Löschen der
Container gesichert und das exakt getestete Produktions-JAR kopiert. Die
Klassendateien für den Bericht stammen aus diesem JAR, nicht aus `target/classes`.

Im jeweiligen Laufordner entstehen `coverage/html/index.html`, `coverage/jacoco.xml`,
`coverage/jacoco.csv`, die Rohdaten und das geprüfte JAR. Der Markdown-Bericht verlinkt
sie. Eine angeforderte, fehlgeschlagene Messung macht den Lauf insgesamt unvollständig.
Ohne `-Coverage` bleiben Start und Abbau des bisherigen Runners erhalten.

Gemessen wird serverseitiger Anwendungscode einschließlich Start, Hintergrundaufgaben
und der ausgewählten Browserjourneys. Der Prozentsatz ist weder CSS-/JavaScript-
Coverage noch ein Anteil aller fachlich denkbaren Fälle. „100/100 bestanden“ ist eine
Fall-Erfolgsquote, keine Aussage über 100 % Code-Coverage. JUnit-Coverage liegt getrennt
unter `target/site/jacoco/index.html`. Tatsächlich gemessene Werte: [Abdeckung](abdeckung.md).

## Neue Adminregressionen

CONFIG-05A prüft zusätzlich die Menü-URL, echten Browser-Refresh, Direktlinks im
zweiten Tab und Zurück/Vorwärts zwischen Benutzerverwaltung und Security.
CONFIG-05A–F prüfen die native Hierarchie mit Klick/Enter/Leertaste, den Erhalt
ungespeicherter Werte und den echten Klick-Tooltip samt Außenklick, Sprache und
Card-Stil. Alle 28 Feldinfos in sieben Gruppen sowie 320×568 mit begrenzter
Tooltipbreite sind enthalten. CONFIG-05F prüft zusätzlich den horizontal begrenzten
Benutzermenüinhalt, die native mobile Statusfläche und tatsächliches Scrollen/Abmelden.
Native Öffnungs-/Schließanimationen werden vor Bounds- und Folgeaktionen abgewartet. Enter am Navigationspunkt darf nicht speichern;
Enter im Eingabefeld muss den neuesten Wert übernehmen. Positive Queue-Dauern
bleiben auch unter einer Minute zulässig. Geheimwerte werden nicht als Default gezeigt.

MAIL-02/03 prüfen alle drei Feldinfos mit Tastatur und Sprachwechsel sowie Enter
als Zeilenumbruch im Textarea. Erst bewusstes Absenden erzeugt genau eine eigene
SMTP-Mail; MAIL-01 prüft zusätzlich Enter im Betreff. Der Vergleich berücksichtigt
SMTP-CRLF und Textarea-LF, lehnt geänderten Unicode, Leerzeichen und zusätzliche
Leerzeilen weiterhin ab. USER-10 prüft die reale Gesamtzahl nach Anlage/Löschung,
unabhängig vom Suchfilter und mit korrektem DE-/EN-Sprachwechsel.

## Loginlayout

UI-03A–E prüfen den bestehenden Login bei 320×568, 390×844, 844×390,
360×240 und 1280×900. Formularelemente liegen innerhalb der Fensterbreite;
geringe Höhe darf vertikal scrollen. Remember-me wird tatsächlich gewählt/abgewählt,
eine falsche Anmeldung korrigiert und das echte Admin-Dashboard erreicht.
UI-03F prüft die CSS-Animation und `prefers-reduced-motion` auf Kontoansichten und Home.
Die echten Vaadin-Felder/Buttons verwenden die zentrale Cosmos-Lumo-Palette; Card und
Anordnung folgen der Designvorlage. Inter wird lokal und global bereitgestellt. Home und
Admin verwenden dieselbe zentrale Palette und sind im vollständigen DE-/EN-Lauf geprüft.

```powershell
.\browser-tests\run.ps1 -Suite login-checks -Language de
.\browser-tests\run.ps1 -Suite login-checks -Language en -NoBuild
```

Die Fenstergrößen prüfen typische Platzverhältnisse, nicht alle Geräte,
Bildschirmtastaturen oder die Leistung echter Smartphones. Ein kontrollierter
Browserlauf ersetzt kein menschliches Urteil über das Aussehen.

## Gemeinsames Cosmos-Design

Die eigene Suite `cosmos-design` bleibt von den fachlichen Konto-Suites getrennt.
COSMOS-01A–E prüfen alle fünf Ansichten bei 320×568, 390×844, 844×390, 1280×900 und 1280×1200:
geladenes Service-Logo oberhalb der Überschrift, Cardzentrierung bei ausreichend Platz,
gemeinsames Design, Inter, Fußzeile, übersetzte Platzhalter samt kleiner normaler Schrift und Erreichbarkeit
innerhalb der Fensterbreite. Jeder Fall enthält Screenshots aller fünf Ansichten.
Unbekannte UUID-Links zeigen die echten Fehlerzustände, ohne Tokens zu erzeugen.

COSMOS-02 prüft zusätzlich die gültigen und erfolgreichen Verifikations-/Resetansichten
bei 390×844 über Registrierung und tatsächlich empfangene Mailbuttons bis zur Anmeldung.
Nach dem Reset werden Erfolgscard, Häkchen und entfernte Passwortfelder geprüft;
ungültige Links zeigen das Warnsymbol.
COSMOS-03 speichert den Copyright-Text im Adminbereich, lädt ihn neu und prüft die
ausgefüllte, sichere Fußzeile nach Abmeldung. Registrierung und Passwortanfrage erzeugen
anschließend echte SMTP-Mails; deren geladenes Inline-Logo und derselbe gespeicherte
Fußtext werden geprüft. Der ursprüngliche Wert wird wiederhergestellt.
COSMOS-04 prüft die schmale Fehlermeldung mit Card-Verlauf und dieselbe Palette auf Home.
Die globalen HTML-Farbvariablen bleiben unabhängig davon unverändert.
COSMOS-05A–C prüfen Home und Admin bei 390×844, 844×390 und 1280×900: Topbar/Avatar,
Popupbreite, horizontale Erreichbarkeit, echte Drawer-Navigation, Allgemein und das
Benutzerformular. Im mobilen Overlaymodus wird die Navigation vor der Prüfung des
dahinterliegenden Inhalts geschlossen; die native Einschiebe-Animation wird abgewartet.
Alle elf Cosmos-Fälle sind Bestandteil der vollständigen DE-/EN-Läufe. Screenshots
dokumentieren den abgestimmten Designstand; echte Geräte und eine vollständige
menschliche Bewertung werden dadurch nicht ersetzt.

```powershell
.\browser-tests\run.ps1 -Suite cosmos-design -Language de
.\browser-tests\run.ps1 -Suite cosmos-design -Language en -NoBuild
```

Palette und Komponentenfarben stehen in `custom-theme/cosmos-tokens.css`; die Regeln
in `cosmos-theme.css`, `cosmos-shell.css`, `cosmos-admin.css` und
`cosmos-notifications.css`. `admin-users.css`
ergänzt das lokale Benutzerlayout. Die Dekoration steht in `cosmos-background.css`
und der globale Font in `typography.css`. Siehe [Theme-Anleitung](../docs/theme.md). Die Java-Bausteine
heißen `CosmosPageComponent`, `CosmosCardComponent`, `CosmosBackgroundComponent`
und `CosmosCopyrightComponent`. Die Übersetzungen bleiben im
gemeinsamen typisierten Katalog. JUnit prüft zusätzlich Copyright-Defaults, Textsicherheit
und dynamischen Sprachwechsel ohne Verlust der Eingaben. Keine doppelten Navigationstests.

## Weitere Konten und echte Wartezeiten

Die neuen Abläufe verwenden kleine eigenständige Suites statt einer wachsenden
Gesamtklasse. `BrowserActor` öffnet tatsächlich getrennte Sitzungen; die Konten
werden über Registrierung oder das Adminformular angelegt. Neue Editierformulare
werden erst nach dem angebotenen Neuladen der Benutzerliste geöffnet, damit echte
An-/Abmeldungen anderer Konten keinen unbeabsichtigten alten Datenstand vorbereiten.
Im Versionskonflikttest bleiben dagegen absichtlich beide ursprünglichen Formulare offen.

`session-security` klickt den bereits geöffneten Speichern-Button nach einer
Adminänderung. Die echte Browseranfrage muss 401 erhalten; das Probe-Konto darf
anschließend nicht existieren. Das ist ein Nachweis dieses UI-Callbacks, keine
vollständige Aussage zu allen WebSocket-Transportvarianten oder laufenden Aktionen.

Remember-me verwendet einen eigenen temporären Chromium-Profilordner. Der ganze
zusätzliche Prozess wird geschlossen und mit demselben Ordner neu gestartet;
Cookies und Storage werden nicht künstlich eingesetzt. JUnit räumt das Testprofil auf.
Die Browserberichte enthalten nur erfundene Konten; Cookieinhalte werden nicht protokolliert.

Konfigurationsfälle merken ihre Ausgangswerte und stellen sie im `finally`-Block
über die Oberfläche wieder her. Neuladen bestätigt Speicherung, keinen Neustart
der Anwendung. CONFIG-04B prüft gezielt die Checkbox/Persistenz. Die eigene
Suite `maintenance` prüft zusätzlich den tatsächlichen Zugangsschutz und die
Wiederfreigabe. Alle Änderungen treffen ausschließlich die Wegwerf-Umgebung.

Tokenablauf prüft die echte Mindestdauer von einer Minute und wartet pro Ablauf
65 Sekunden. Deshalb dauert `token-expiry` mehrere Minuten. Die erneute Mail wird
an derselben eigenen Adresse über ihre neue Mailpit-ID unterschieden; kein Mail-
oder Datenbankzustand wird zum Beschleunigen verändert. Die empfangenen HTML-Mails
werden zusätzlich auf ersetzte Platzhalter, geladenes CID-Inline-Logo und vollständigen
Copyright Text geprüft. Der Standardtext kommt über den gemeinsamen DE-/EN-Getter
mit aktuellem Jahr; im Konfigurationstest stammt die Erwartung aus der über die UI
gespeicherten Fußzeile. Die kanonischen HTML-Dateien bleiben Testressourcen und werden
auf den Copyright-Platzhalter geprüft. Es gibt keinen zweiten Übersetzungskatalog.
Die Adminmail prüft ihren im UI konfigurierten Absender.

## Ergebnisse und Traces

Jeder Lauf erhält einen eigenen Ordner:

```text
browser-tests/runs/
  ZEITSTEMPEL_UTC__GIT-COMMIT__RUN-ID/
    report.md
    metadata.json
    runner.log
    allure-results/
    html/index.html
    html/open-report.bat
```

`report.md` enthält Fallbeschreibung, Häkchen, Status, Ist/Grund, UTC-Zeitpunkte
und Links zu Screenshots und Traces. `metadata.json` dokumentiert zusätzlich
Branch/Commit, lokale Änderungen, Image-ID, Sprache und Aufbau/Abbau.
Übersprungene Fälle, fehlende Ergebnisse oder fehlgeschlagener Abbau ergeben
keinen erfolgreichen Gesamtstatus.

**Für einen beliebigen gespeicherten HTML-Bericht `html/open-report.bat`
doppelklicken.** Das zugehörige Konsolenfenster offen lassen; mit Strg+C beenden.
`index.html` nicht direkt öffnen: Allure lädt seine Daten über den lokalen HTTP-Server.
Der Starter benötigt den Testordner samt Maven Wrapper, Java und bereitgestellten Tools.

Im Allure-Bericht enthält jeder Fall Aktionen, Diagnose, Screenshots und den
Playwright-Trace. Der Trace-Anhang öffnet den Trace Viewer direkt im Bericht.
Alternativ die ZIP herunterladen und mit `-Trace` öffnen. Belege werden auch bei
erfolgreichen Fällen behalten. `-Report` startet ausschließlich einen lokalen
Berichtserver, keine Anwendung und keine neuen Tests.

### Aufbewahrung und Bereinigung

`runs/` enthält ignorierte lokale Diagnoseausgaben, keine dauerhaft erforderliche
Projektdokumentation. Screenshots, Traces, Allure-Daten und bei Coverage zusätzlich
JAR/Klassendateien können viel Speicher benötigen. Nach Auswertung dürfen nicht mehr
benötigte vollständige Laufordner entfernt werden. Eine dauerhafte Aufbewahrung
oder Sicherung aller Runs ist nicht vorgesehen; es gibt keine vorgeschriebene Frist
und keine automatische Löschung.

Wichtige Abschlüsse vorher als kurze eigenständige Markdown-Zusammenfassung unter
[Meilensteine](../docs/meilensteine/README.md) festhalten: geprüfter Stand, Ergebnisse,
Coverage mit Messgrenzen und wichtigste Erkenntnisse. Keine konkreten Run-Pfade in
dauerhafte Dokumentation übernehmen. Der Redesign-Abschluss ist dort bereits dokumentiert.
Die Links innerhalb eines Run-Berichts sind nur nutzbar, solange sein Ordner besteht.
Auch `-Report` benötigt einen vorhandenen Bericht; nach Bereinigung kann ein neuer
Lauf erstellt oder mit `-ReportPath` ein noch vorhandener Bericht ausgewählt werden.

## Aufbau und Änderungen

Die Testquellen liegen unter `browser-tests/src/test/java/de/derpeterson/browsertests/`.

| Bereich | Aufgabe |
| --- | --- |
| `suites/` | Kleine getrennte fachliche Suites mit unabhängigen Testfällen |
| `pages/` | Wiederkehrende UI-Aktionen; Liste, Benutzerformular und Löschbestätigung haben getrennte Helfer |
| `pages/AdminNavigation` | Native SideNav-Hierarchie, sichtbare Links, mobiler Drawer und dessen Einschiebe-Animation |
| `pages/UiActions` | Wartet nach echten Klicks auf die Vaadin-Antwort; Wertänderungen werden getrennt vom Öffnen eines Dropdowns erkannt |
| `mail/MailpitInbox` | Nur eigene empfangene Testmail lesen und deren echten Link verwenden |
| `i18n/UiTexts` | Gemeinsame Übersetzungsgetter mit expliziter Sprache |
| `BrowserFixture` | Browserkontext, Screenshots und Traces |
| `BrowserSuites` | Fachliche Auswahl mit `-Suite` |
| `BrowserExecutionPlan` | Fast-Markierungen, sequenzielle JUnit-Auswahl und eindeutige Klassenverteilung |
| `BrowserParallelWorkers` | Getrennte JVMs/Ports, Prozessgrenzen und gemeinsame Belege |
| `BrowserRoutes` | Gemeinsame Routen als lokale Pfade und genaue URL-Erwartungen |
| `BrowserTestRunner` | Eigene Docker-Umgebung, JUnit-Ausführung, Abbau und Allure-Erzeugung |
| `BrowserCoverage` | Optionaler JaCoCo-Agent, sichere Extraktion der geprüften JAR-Klassen und statischer Code-Coverage-Bericht |
| `MarkdownSummary` | Markdown-Checkliste aus fertigen Allure-Ergebnissen |

`run.ps1` kompiliert das unabhängige Maven-Testprojekt und startet Java.
Die Ablaufsteuerung und Markdown-Erzeugung sind Java; Playwright steuert den Browser,
JUnit führt die Fälle aus und Allure erzeugt die HTML-Berichte.
Kein eigenes npm-Testprojekt oder selbst entwickelter HTML-Reporter.
Die aktuellen Werkzeugversionen stehen in `browser-tests/pom.xml`.

Neue Fälle in der passenden Suite ergänzen; neue fachliche Bereiche erhalten eine
eigene Suite und Registrierung in `BrowserSuites`. ID, `@DisplayName`, `@Feature`,
`@Story` und `@Description` pflegen. Playwrights wartende Assertions verwenden,
keine festen Schlafzeiten und keine Abschwächung bei roten Ergebnissen.
Eine automatische Anbindung an das OpenCode-Benchmarking ist nicht eingerichtet.

## Sprachen und gemeinsame Konstanten

Ein Lauf prüft eine ausdrücklich über die Oberfläche gewählte Sprache: `de` oder `en`.
Erwartungen akzeptieren nicht gleichzeitig beide Übersetzungen. Für einen vollständigen
Vergleich beide Sprachen ausführen; derzeit sind das 96 Fälle je Sprache.

Die Getter sind einmalig in `shared/src/main/java/de/derpeterson/app/i18n/MessageTexts.java`
definiert. Die Anwendung verwendet `MessageProperties`, die Browsertests `UiTexts`.
`UiTexts` liest die originalen Properties-Ressourcen mit fester Locale; fehlende
Schlüssel/Sprachen müssen scheitern. Properties-Dateien nicht duplizieren oder umkodieren.
Routen, Seitentitel und vorhandene Avatar-Konstanten kommen aus den gemeinsamen
`AppRouteConstants`/`AppConstants`; `AdminConfigGroup` bezeichnet Konfigurationsgruppen.

Normalerweise wird nach Login die Laufsprache über das echte Menü gewählt, weil eine
Kontopräferenz sie überschreiben kann. **LANG-02A ist die bewusste Ausnahme:**
Nach frischer Anmeldung wird die Sprache nicht erneut gesetzt, damit der Fall die
gespeicherte Präferenz tatsächlich prüft. Datum und Uhrzeit werden zur Sprache passend eingegeben.

## Isolation und Aufgaben der Testarten

- JUnit-/Persistenztests prüfen Regeln, Transaktionen, Nebenläufigkeit und technische Fehler.
- Browsertests prüfen sichtbare UI-Abläufe. Testzustände entstehen über die Oberfläche;
  kein SQL, kein Serviceaufruf und keine API zum Anlegen/Aktivieren/Ändern von Konten.
- Manuelle Sonderprüfungen bleiben für echte Geräte, Bediengefühl, Lesbarkeit und
  weitere Fälle mit notwendiger menschlicher Einschätzung.

Der Runner verwendet [compose.browser-tests.yaml](../compose.browser-tests.yaml)
und pro Lauf ein eigenes Projekt `vaadinspringapp-e2e-java-…` mit wegwerfbaren Volumes.
Standardports: Anwendung `127.0.0.1:18081`, Mailpit `127.0.0.1:18026`;
H2-Webkonsole deaktiviert. Die manuelle Umgebung auf `18080`/`18025` bleibt unberührt.
Diese manuellen Ports werden vom Runner abgelehnt.

Mehrere Läufe auf denselben Ports nacheinander ausführen. Für einen parallelen Lauf
in dessen PowerShell-Prozess andere freie Ports wählen:

```powershell
$env:BROWSER_TEST_APP_PORT = '18082'
$env:BROWSER_TEST_MAILPIT_PORT = '18027'
.\browser-tests\run.ps1
```

Nur Chromium und keine automatischen Testwiederholungen. Pro Worker laufen die
JUnit-Fälle weiterhin auf einem Thread; mehrere Worker haben getrennte Umgebungen.
Der Testaufbau besitzt einen eigenen Bootstrap-Admin mit erfundenen Zugangsdaten;
die Vorgaben stehen in `compose.browser-tests.yaml` und `BrowserTestAccounts`.
Die tatsächliche Queue versendet per SMTP ausschließlich an den eigenen Mailpit.
`MailpitInbox` sucht nach eigenem Empfänger und übersetztem Betreff, nicht nach der
global neuesten Mail. Aktivierungs-/Resetlinks müssen exakt zur lokalen Runner-Adresse passen.
Damit ist keine externe Postfachzustellung oder Outlook-/Gmail-Darstellung bewiesen.

Run-Daten sind nicht in Git; Screenshots/Traces können Testeingaben und Sitzungstokens
enthalten. Keine Bestandsdatenbank oder laufende manuelle Umgebung zur Vorbereitung verwenden.
Mit `-Coverage` misst die Browserprüfung serverseitigen Java-Code einschließlich
Start und Hintergrundjobs. Ohne diesen Schalter wird keine Code-Coverage erfasst.
Sie ersetzt keinen vollständigen Mehrbrowser-, Mobilgeräte-, Accessibility-,
SMTP- oder Betriebsnachweis.

## IntelliJ und Fehlerhilfe

`shared/pom.xml` und `browser-tests/pom.xml` zusätzlich zur Anwendungs-POM als
Maven-Projekte hinzufügen und neu laden. Shared besitzt seine Quellen allein;
Anwendung und Tests hängen von dessen JAR ab. Keine zusätzlichen Shared-Source-Roots.
Find Usages an einem Getter mit „All Places“ findet beide Modulaufrufer.

Browserfälle über den Starter ausführen. Ein direkter JUnit-Start ohne verwaltete
Testumgebung bricht absichtlich ab. Helper-/Reportertests funktionieren ohne Docker.
Vor direkten Maven-Aufrufen das Shared-JAR bauen:

```powershell
.\mvnw.cmd -f shared/pom.xml clean install
.\mvnw.cmd -f browser-tests/pom.xml test
```

| Problem | Vorgehen |
| --- | --- |
| Chromium fehlt | `-InstallBrowsers` ausführen |
| Aufbau/Ports scheitern | Docker Desktop, `runner.log` und `metadata.json` prüfen; andere freie Testports wählen |
| Falsches JDK | Tatsächliches Wrapper-JDK muss Java 25 sein; keine Versionen zur Umgehung downgraden |
| HTML lädt nicht | `open-report.bat` oder `-Report` verwenden; nicht `index.html` direkt öffnen |
| Alter Anwendungsstand | Nach Code-/Text-/Styleänderungen ohne `-NoBuild` starten |
| Test rot | Screenshot, Trace und erste konkrete Ursache ansehen; offenen Stand in `abdeckung.md` aktualisieren |

Bei normalem Abschluss und Testfehlern räumt der Runner seine Container/Volumes auf.
Nach hartem Abbruch nur das konkrete eigene Projekt aus `metadata.json` abbauen:

```powershell
docker compose -f compose.browser-tests.yaml -p vaadinspringapp-e2e-java-DEINEKENNUNG down --volumes --remove-orphans
```

Keine fremden oder manuellen Docker-Projekte zum Aufräumen verwenden.

### Abgeschlossene Redesign-Prüfung

Die gemeinsame Gestaltung von Kontoansichten, Home und Administration einschließlich
Benutzerfiltern und Dialogen wurde am 15.10.2026 vollständig auf DE und EN geprüft.
Ergebnisse und Erkenntnisse bleiben im
[Redesign-Meilenstein](../docs/meilensteine/2026-10-15-redesign-abgeschlossen.md) erhalten.
Aktuelle Fallzuordnungen und offene Betriebs-/Gerätegrenzen stehen in
[Abdeckung und offene Punkte](abdeckung.md).

## Neue Kontoprüfungen und Serverneustart

`-Suite account-settings` prüft ACCOUNT-01–03 und ACCOUNT-04A–D: dauerhaftes
Speichern einschließlich zweitem Save, Pflichtfelder/Datumsregel ohne ungewollte
Speicherung, gespeicherte Sprache nach erneuter Anmeldung, den E-Mail-Einstieg unter Sicherheit und den fehlenden Profil-Ändern-Button,
Tabentwürfe, lokalisierte Dialogfelder, Speichern-Icon, berechneten Backdrop-Blur
und vier echte Schließwege. Passwort und E-Mail-Entwürfe müssen beim Wiederöffnen
leer sein. Der eigentliche E-Mail-Wechsel ist noch nicht implementiert und wird
nicht als erfolgreich geprüft ausgegeben. `fast` bleibt bewusst bei 34 Fällen.

AUTH-07C/D ergänzen die beiden Browserneustartfälle um einen echten Neustart der
runner-eigenen Anwendung mit zwei offenen Tabs. Der Runner bietet diesen Schritt
nur während seiner eigenen laufenden Testumgebung an; der normale Anwendungsstack
wird nicht berührt. [Einordnung des IntelliJ-Befunds](../docs/login-restart-analysis.md).
Messdaten vor und nach dem Neustart werden innerhalb des frischen eigenen Volumes
zusammengeführt; alte lokale Run-Verzeichnisse werden nicht benötigt.

Auf dem Windows-Entwicklungsrechner vollständige DE-/EN-Runden **nacheinander**
starten. Zwei Worker innerhalb eines Laufs bleiben isoliert parallel. Mehrere
zeitlich überlappende Gesamtläufe können trotz freier CPU/RAM die nativen
Netzwerkressourcen erschöpfen (`ERR_NO_BUFFER_SPACE` beim Laden von JavaScript).
Das ist kein bestandener Funktionstest: Lauf abbrechen, nur eigene Umgebungen
abbauen, die Ressourcen erholen lassen und sequenziell neu prüfen. Assertions
oder Zeitlimits dafür nicht pauschal lockern.

USER-05C prüft die optionale Zeitliste auch vom Tagesende zurück zur früheren Uhrzeit.
Bei der virtualisierten Liste bestimmt die erste tatsächlich sichtbare Zeile die
Scrollrichtung; unsichtbare DOM-Pufferzeilen sind dafür ungeeignet. Der Test prüft
die echte Mausradbewegung und wirksame Filterung in beiden Richtungen unabhängig von
der aktuellen Uhrzeit, ohne künstliche DOM- oder Uhrmanipulation.
## Neue Benachrichtigungs- und Rollenfälle

Die Gesamtsuite enthält zusätzlich fünf Fälle in `UserNotificationsBrowserTest`
und einen Live-Rollenfall in `UserAccessBrowserTest`. Das fertige Redesign ergänzt
drei Ausprägungen in `NotificationInboxBrowserTest` (NOTIFY-03 bei 1280/390 px und
NOTIFY-04). `notifications` enthält alle acht Fälle beider Benachrichtigungsklassen,
`all` insgesamt 152; `fast` bleibt bei 34 Fällen.

| ID | Schritte und erwartetes Verhalten |
| --- | --- |
| NOTIFY-01 (1280/390 px) | Entwurf leeren/Blur ohne Fehler; ausdrücklich prüfen markiert Empfänger, Titel und Nachricht mit allgemeiner Fehlernotification; alle aktiven Konten zeigen gesperrte Empfängerzusammenfassung; prüfen gibt Versand frei; Vorschau-Empfängerdetails öffnen/schließen; Eingabeänderung sperrt Versand; erneute Prüfung aktualisiert Inhalt. Gleiche Aktionsbreiten, 12px Nachrichtentext, 8px Textabstand, Fokus und kein Panelüberlauf. |
| NOTIFY-02 (Einzel/Alle) | Zwei eigene aktive Benutzer über UI anlegen; geprüfte Benachrichtigung senden; getrennte Sitzungen öffnen direkt ihre Inbox. Empfänger öffnet den Detaildialog zunächst weiterhin ungelesen, markiert ausdrücklich als gelesen und findet die Nachricht nach Reload als gelesen. Nicht ausgewählter Benutzer erhält beim Einzelversand keinen Eintrag, beim Sammelversand seinen eigenen. |
| NOTIFY-03 (1280/390 px) | Drei echte Sendungen, davon 120 Zeichen Betreff/4000 Zeichen Inhalt. Gemeinsamer Header samt Logo/Adminzugang und Kontotabs; Vorschau-/Inboxdetails ohne automatische Lesemarkierung, Inbox-Link nur aus Vorschau. Iconaktionen und Metadaten im Footer ohne horizontalen Überlauf, lokalisierte Hover-Tooltips und Tab-Fokus. Gelesen-/Ungelesenfilter, Sammellöschen erhält ungelesene Nachrichten, Einzel-X öffnet kein Detail, Sammellesen und persistenter Leerzustand. |
| NOTIFY-04 | Elf echte UI-Sendungen, Versandarchiv mit vollständigen Details und Pagination. Persönliche Inbox 10+1, 56px Seitenauswahl und Größen 20/50, Suche mit Lesefiltern/Leerzustand, Vorschaugrenze fünf, Sammellesen über alle elf mit Reload. |
| MAINT-06 | Verfassenpanel, Mail-Test und Konfiguration in getrennten eigenen Tabs öffnen; Entwürfe vorbereiten; Wartung im Konfigurationstab schalten. Beide Panels aktualisieren sich ohne Reload, zeigen roten Status und sperren ihre Controls. Nach Wiederfreigabe bleiben Entwürfe erhalten; Benachrichtigungsversand erfordert neue Prüfung. |
| USER-07H | Normales Konto in unabhängiger Sitzung anmelden; zweiter Admin vergibt Administrator. Bestehende Sitzung erhält Adminnavigation, öffnet Adminformular; Rollenentzug führt auf Home und entfernt altes Formular bei erhaltenem Login. Persönliche Rollenbenachrichtigung sichtbar, dieselbe Session-ID und kein erneuter Login-POST. |

SESSION-01A erwartet beim Rollenentzug erhaltenen Login und automatisch verworfene
Adminansicht. Die unveränderten Sperr-/Löschvarianten erwarten weiterhin 401 und Login.
USER-07E wählt vor dem eigenen Adminrollenentzug eine normale Rolle: Bootstrap-Admins
haben jetzt ausschließlich Admin, und eine vollständig leere Rollenauswahl ist bereits
im Formular ungültig. Die ursprüngliche Sicherheitsassertion bleibt erhalten.

Seit dem QA-Auftrag vom 20.10.2026 sind das fertige Header-Overlay und das gesendete
Archiv ausdrücklich im Prüfumfang. NOTIFY-03/04 ergänzen die bestehenden
Zustellungs-/Berechtigungsfälle. Die Tests erzeugen ihren Zustand ausschließlich
über native UI-Aktionen; keine Backend-/Datenbankabkürzung oder DOM-Manipulation.
Ausführung: `.\browser-tests\run.ps1 -Suite notifications -Language de -Coverage`,
anschließend dieselben Fälle auch im vollständigen `all`-Lauf beider Sprachen.
Laufresultate und Messgrenzen stehen in `abdeckung.md`; diese Tabelle ist ein Fallkatalog.

COSMOS-06 ergänzt zwei Breiten für die vertikale Infoicon-Ausrichtung: alle drei
Kontotabs, alle acht Konfigurationsgruppen, Mail-Test und Benachrichtigungsversand.
Die Hinweiszeilen werden tatsächlich gerendert und ihre Iconmitten geometrisch
verglichen. USER-05A prüft zusätzlich die 56px Seitenauswahl der Benutzerverwaltung.
