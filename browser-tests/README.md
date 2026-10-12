# Browsertests mit Java, JUnit und Allure

Diese Anleitung erklärt die Bedienung und den Aufbau der Browsertests.
Den aktuellen Prüfstand, offene Fehler und die Zuordnung der ursprünglichen
manuellen Fälle findest du ausschließlich in [Abdeckung und offene Punkte](abdeckung.md).
Ein vorhandener Testfall bedeutet noch nicht, dass sein Ablauf erfolgreich geprüft ist.

## Starten

Voraussetzungen: Java 25, Docker Desktop mit Linux-Containern und Docker Compose
ab 2.24.4. Im Projektordner den vorhandenen Maven Wrapper verwenden.
Eine globale Playwright- oder Allure-Installation ist nicht erforderlich.

```powershell
# Einmalig: Bibliotheken und passenden Chromium-Testbrowser bereitstellen.
.\browser-tests\run.ps1 -InstallBrowsers

# Alle Suites auf Deutsch, mit aktuellem Anwendungsimage.
.\browser-tests\run.ps1 -Suite all -Language de

# Dasselbe unveränderte Anwendungsimage anschließend auf Englisch prüfen.
.\browser-tests\run.ps1 -Suite all -Language en -NoBuild
```

| Aktion | Befehl |
| --- | --- |
| Tests live im sichtbaren Browser verfolgen | `.\browser-tests\run.ps1 -Headed` |
| Nur Benutzerverwaltung prüfen | `.\browser-tests\run.ps1 -Suite user-management -Language de` |
| Helper und Reporter ohne Docker prüfen | `.\browser-tests\run.ps1 -ReportingTests` |
| Letzten Allure-Bericht öffnen | `.\browser-tests\run.ps1 -Report` |
| Bestimmten Bericht öffnen | `.\browser-tests\run.ps1 -Report -ReportPath '.\browser-tests\runs\DEIN-LAUFORDNER\html'` |
| Gespeicherten Trace öffnen | `.\browser-tests\run.ps1 -Trace 'C:\PFAD\ZUM\trace.zip'` |
| Absichtlich einen roten Bericht erzeugen | `.\browser-tests\run.ps1 -NoBuild -FailureProbe` |

`-Suite`, `-Language`, `-Headed` und `-NoBuild` lassen sich kombinieren.
Standardwerte sind `all` und `de`. `-NoBuild` verwendet das vorhandene App-Image;
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
| `registration` | Registrierung, Pflichtfelder und Korrektur ungültiger Eingaben | 3 |
| `verification` | Tatsächliche Aktivierungsmail, gültige, verbrauchte und ungültige Links | 3 |
| `password-reset` | Tatsächliche Resetmail, Passwortwechsel, Formularfehler und Links | 6 |
| `account-lifecycle` | Einzelvalidierungen, kanonisierte Dubletten, unverifizierte Anfrage und altes Resetformular | 5 |
| `presentation` | Sprachwechsel, gespeicherte Sprache, schmale Registrierung und Tastaturlogin | 4 |
| `token-configuration` | Vier Mindestdauern, Feldfehler, Speicherung und Erklärungen | 4 |
| `user-management` | Suche/Pagination, optionale Filter, Anlage, Bearbeitung, Löschung, Selbstschutz und Passwortwechsel | 8 |
| `all` | Alle neun Suites | 42 |

Die vollständigen Schritte und Erwartungen stehen pro Fall in `@Description`
und werden automatisch in den Markdown-Bericht übernommen. Die Test-ID erscheint
in `@DisplayName`, im Allure-Bericht und in der Abdeckungsübersicht.

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

Historische Run-Berichte werden nicht nachträglich geändert. Frühere Fassungen
der zusammengeführten Dokumentation liegen als ZIP unter `runs/dokumentationsarchiv/`.
Dieser Unterordner enthält Dokumentationssicherungen, keine zusätzlichen Testläufe.

## Aufbau und Änderungen

Die Testquellen liegen unter `browser-tests/src/test/java/de/derpeterson/browsertests/`.

| Bereich | Aufgabe |
| --- | --- |
| `suites/` | Kleine getrennte fachliche Suites mit unabhängigen Testfällen |
| `pages/` | Wiederkehrende UI-Aktionen; Liste, Benutzerformular und Löschbestätigung haben getrennte Helfer |
| `mail/MailpitInbox` | Nur eigene empfangene Testmail lesen und deren echten Link verwenden |
| `i18n/UiTexts` | Gemeinsame Übersetzungsgetter mit expliziter Sprache |
| `BrowserFixture` | Browserkontext, Screenshots und Traces |
| `BrowserSuites` | Auswahl mit `-Suite` |
| `BrowserRoutes` | Gemeinsame Routen als lokale Pfade und genaue URL-Erwartungen |
| `BrowserTestRunner` | Eigene Docker-Umgebung, JUnit-Ausführung, Abbau und Allure-Erzeugung |
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
Vergleich beide Sprachen ausführen; derzeit sind das 42 Fälle je Sprache.

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

Nur Chromium, ein Thread und keine automatischen Testwiederholungen.
Der Testaufbau besitzt einen eigenen Bootstrap-Admin mit erfundenen Zugangsdaten;
die Vorgaben stehen in `compose.browser-tests.yaml` und `BrowserTestAccounts`.
Die tatsächliche Queue versendet per SMTP ausschließlich an den eigenen Mailpit.
`MailpitInbox` sucht nach eigenem Empfänger und übersetztem Betreff, nicht nach der
global neuesten Mail. Aktivierungs-/Resetlinks müssen exakt zur lokalen Runner-Adresse passen.
Damit ist keine externe Postfachzustellung oder Outlook-/Gmail-Darstellung bewiesen.

Run-Daten sind nicht in Git; Screenshots/Traces können Testeingaben und Sitzungstokens
enthalten. Keine Bestandsdatenbank oder laufende manuelle Umgebung zur Vorbereitung verwenden.
Die Browserprüfung liefert keine Java-Coverage und ersetzt keinen vollständigen
Mehrbrowser-, Mobilgeräte-, Accessibility-, SMTP- oder Betriebsnachweis.

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
