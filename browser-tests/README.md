# Browsertests mit Java, JUnit und Allure

Die Browsertests prüfen die im Browser gerenderte Vaadin-Anwendung in einer frischen Docker-Testumgebung. Die Testfälle, Docker-Steuerung und Markdown-Erzeugung sind Java. Playwright übernimmt die Browserautomatisierung, JUnit die Testausführung und Allure die HTML-Berichte samt Aktionen und Belegen. Es wird kein eigener HTML-Reporter entwickelt.

## Inhalt

- [Einrichten und starten](#einrichten-und-starten)
- [Suites und Testfälle](#suites-und-testfälle)
- [Sprache und Übersetzungen](#sprache-und-übersetzungen)
- [Ergebnisse ansehen](#ergebnisse-ansehen)
- [Java-Dateien verstehen](#java-dateien-verstehen)
- [Isolation und Grenzen](#isolation-und-grenzen)
- [IntelliJ und weitere Tests](#intellij-und-weitere-tests)
- [Fehlerhilfe](#fehlerhilfe)

## Einrichten und starten

Voraussetzungen: Java 25, Docker Desktop mit Linux-Containern und Docker Compose ab 2.24.4. Den vorhandenen Maven Wrapper des Projekts verwenden. Allure wird lokal über Maven bereitgestellt; keine globale Allure-Installation nötig.

In PowerShell im Projektordner:

```powershell
# Einmalig: Abhängigkeiten und passenden Chromium-Browser bereitstellen.
.\browser-tests\run.ps1 -InstallBrowsers

# Normaler Testlauf mit aktuellem App-Image-Build (Docker-Cache möglich).
.\browser-tests\run.ps1
```

| Aktion | Befehl |
| --- | --- |
| Tests mit sichtbarem Browser | `.\browser-tests\run.ps1 -Headed` |
| Bereits gebautes Browser-Testimage wiederverwenden | `.\browser-tests\run.ps1 -NoBuild` |
| Reporter, URL-/Link-Isolation und Übersetzungs-Helper mit JUnit prüfen, ohne Docker | `.\browser-tests\run.ps1 -ReportingTests` |
| Letzten Allure-HTML-Bericht anzeigen | `.\browser-tests\run.ps1 -Report` |
| Bewusst einen Fehlerbericht erzeugen | `.\browser-tests\run.ps1 -NoBuild -FailureProbe` |
| Eine gespeicherte Ablaufaufzeichnung öffnen | `.\browser-tests\run.ps1 -Trace 'C:\PFAD\ZUM\trace.zip'` |

`-FailureProbe` lässt absichtlich einen falschen Seitentitel erwarten. Exitcode 1 und ein roter Test sind dabei korrekt. Dieser Fall läuft nicht bei normalen Browser- oder Reporter-Prüfungen mit. Auch nach dem Fehler wird die eigene Docker-Umgebung abgebaut.

`run.ps1` ist nur der kurze Windows-Einstieg: Es kompiliert das unabhängige Maven-Testprojekt und startet Java mit den Bibliotheken. Die eigentliche Teststeuerung liegt in Java. JUnit startet die Browserfälle ohne ein zweites selbst entwickeltes Testframework.

## Suites und Testfälle

Die Fälle sind nach fachlichem Bereich getrennt, mit eigenen JUnit-Klassen unter
src/test/java/de/derpeterson/browsertests/suites/. Ein gewöhnlicher Start führt alle vier
Suites aus. Jede Prüfung bekommt einen frischen Browserkontext; alle Fälle funktionieren
ohne vorherige Testfälle. Die Datenbank und Mailpit sind für den gesamten Lauf frisch.

| Suite | Inhalt | Anzahl |
| --- | --- | ---: |
| infrastructure | Startseite und Bilder, Loginformular, erreichbare Mailpit-API | 3 |
| authentication | Adminlogin, falsches Passwort, unbekanntes Konto, Gastzugriff, Logout, normale Benutzerrechte | 6 |
| registration | Registrierung, Pflichtfelder und korrigierbare Eingabefehler | 3 |
| verification | Echter Mailbutton, Aktivierung, erneuter Linkaufruf, ungültiger/fehlender Token | 3 |
| all | Alle vier Suites; Standardwert | 15 |

~~~powershell
# Nur die Anmeldungssuite:
.\browser-tests\run.ps1 -Suite authentication

# Infrastruktur unabhängig prüfen:
.\browser-tests\run.ps1 -Suite infrastructure

# Registrierung oder Verifikation einzeln prüfen:
.\browser-tests\run.ps1 -Suite registration
.\browser-tests\run.ps1 -Suite verification

# Alle Suites live mitverfolgen:
.\browser-tests\run.ps1 -Suite all -Headed
~~~

-Suite lässt sich mit -Headed oder -NoBuild kombinieren. -NoBuild verwendet das vorhandene
App-Image; nur bei unverändertem Anwendungscode einsetzen. -FailureProbe führt weiterhin
ausschließlich den absichtlichen Fehlerfall aus, unabhängig von der Suite-Auswahl.

Die IDs kennzeichnen eigenständige Browserfälle. Jeder Fall beschreibt seine Schritte
und Erwartungen vollständig in der Suite und im erzeugten Bericht.

| Fall | Geprüftes Verhalten |
| --- | --- |
| INFRA-01 | Startseite, Serviceanzeige und geladene Bilder bei 1280 × 900 |
| INFRA-02 | Sichtbares Loginformular |
| INFRA-03 | Mailpit-API erreichbar und Nachrichtenliste lesbar; kein Leerheitszwang nach anderen Versandtests |
| AUTH-01A | Admin meldet sich an; Konto und Dashboard sichtbar |
| AUTH-01B | Falsches Passwort: neutraler Fehler und kein Adminzugriff |
| AUTH-01C | Unbekannte Adresse: gleicher Fehler und kein Adminzugriff |
| AUTH-01D | Gast wird beim direkten Adminaufruf zur Anmeldung geführt |
| AUTH-02A | Logout über UI; bestehender Tab, Zurück und erneuter Adminaufruf |
| SEC-01A | Aktiver normaler Benutzer kann sich anmelden, aber keine Adminfunktionen nutzen |
| REG-01A | Registrierung bestätigt, tatsächliche SMTP-Mail eingegangen, Anmeldung vor Aktivierung abgelehnt |
| REG-02A | Leere Pflichtfelder markiert; nach gültiger Korrektur Registrierung bestätigt |
| REG-02B | Ungültige E-Mail und abweichende Passwortbestätigung verständlich markiert und korrigierbar |
| VER-01A | Mailinhalt geprüft, echter Mailbutton öffnet Aktivierung; anschließend normale Anmeldung ohne Adminrechte |
| VER-01B | Bereits verbrauchter Link zeigt beim weiterhin aktiven Konto gemäß Vertrag erneut Erfolg; Anmeldung funktioniert |
| VER-02A | Unbekannter Token zeigt verständlichen Fehler; Aufruf ohne Token führt zur Startseite |

Noch nicht automatisiert: gesperrte Konten, Remember-me, Rollenänderungen in laufenden
Sitzungen, mutierende Aktionen aus alten Formularen, weitere Eingabe-/Dublettenvarianten,
Verifikationsablauf und erneuter Versand, nachträgliche Adminsperren, Passwortreset,
Adminbearbeitung sowie mobile/Tastaturvarianten. Die Verifikationsfälle prüfen keine
direkten Datenbankzustände, keine exakten Zeitgrenzen und keine Tokenkonkurrenz.

### Aufgaben der Testarten

- JUnit- und Persistenztests prüfen fachliche Regeln, Datenbankverhalten, Transaktionen,
  Nebenläufigkeit und technische Fehlerfälle. Diese bestehenden Tests bleiben erhalten.
- Browsertests bedienen die echte Oberfläche und prüfen das sichtbare Verhalten und
  vollständige Benutzerabläufe. Datenbank- oder API-Zugriffe dürfen diese Bedienung nicht
  ersetzen. Infrastrukturprüfungen sichern die Voraussetzungen der Browserläufe ab.
- Manuelle Sonderprüfungen bleiben für nicht sinnvoll automatisierbare Fälle und
  menschliche Bewertungen, etwa Bediengefühl, Verständlichkeit und visuelle Qualität.

Die Browserdokumentation und ihre Berichte sind eigenständig. Sie benötigen keine
manuelle Vorlage und führen keine manuellen Prüfaufträge mit.

### Testkonten und kleine UI-Helfer

Nur die eigene Browser-Testumgebung bekommt den Bootstrap-Admin
browser-admin@example.test mit dem erfundenen Passwort BrowserAdmin1!.
Die Vorgaben stehen in compose.browser-tests.yaml; passende Testwerte in
BrowserTestAccounts.java. Diese Werte gehören ausschließlich zur wegwerfbaren
Testumgebung, nicht zu einer echten Installation.

Der normale Benutzer für SEC-01A wird im selben Fall durch die echte Adminoberfläche
angelegt: aktive Freigabe, nur ROLE_USER, eindeutige Adresse und BrowserUser1! als
erfundenes Testpasswort. Kein SQL-Seeding, keine Testendpunkte und keine gemockte
Authentifizierung. Die Anlage ist Vorbereitung dieses Falls, keine vollständige
Prüfung der Benutzerverwaltung oder des Verifikationswegs.

Die kleinen Klassen unter pages/ enthalten wiederkehrende Klicks und Eingaben.
Die Suites beschreiben die fachlichen Fälle. BrowserFixture verwaltet ausschließlich
Browserkontexte, Screenshots und Traces. Neue fachliche Bereiche erhalten später
eigene Suite-Klassen und einen Eintrag in BrowserSuites.

### Registrierungsdaten und tatsächliche Testmails

Jeder neue Registrierungs-/Verifikationsfall verwendet eine eigene UUID-Adresse unter
example.test und das erfundene Testpasswort BrowserUser1!. Namen, Geschlecht und
Geburtsdatum werden über das sichtbare Formular eingegeben. Kein Test registriert oder
aktiviert Konten durch SQL, Serviceaufrufe oder eine Test-API.

MailpitInbox liest ausschließlich die Mailablage der isolierten Testumgebung.
Es wartet höchstens 30 Sekunden auf die konkrete Empfängeradresse, niemals auf die
global neueste Nachricht. Die Anwendung versendet über ihre reguläre Queue und SMTP
an Mailpit. Der Helfer erzeugt oder versendet selbst keine Mail.

Empfänger, Betreff und ausgefüllte Platzhalter werden am empfangenen Inhalt geprüft.
Die HTML-Mail wird im Browser geöffnet; dort werden Name, Serviceanzeige und der echte
Aktivierungsbutton geprüft und angeklickt. Sein Ziel muss exakt zum lokalen App-Port
des Testlaufs gehören. Mailansicht und Aktivierung bekommen zusätzliche Screenshots;
Empfänger/Betreff werden als Beleg angehängt. Grundlage ist die offizielle
[Mailpit-API](https://mailpit.axllent.org/docs/api-v1/) und ihre
[HTML-Ansicht für Tests](https://mailpit.axllent.org/docs/integration/).

VER-01B folgt dem bestehenden Vertrag: Ein verwendeter Link darf bei weiterhin aktivem
Konto erneut Erfolg anzeigen. Der Fall behauptet keinen Nachweis für nachträgliche
Adminsperren. Die isolierte Mailpit-Zustellung beweist außerdem keine Zustellung an
externe Postfächer oder Darstellung in Outlook/Gmail.

## Sprache und Übersetzungen

Jeder Lauf prüft **eine ausdrücklich gewählte Oberflächensprache**. Standard ist Deutsch.
Für die englische Variante denselben Lauf mit `-Language en` starten:

```powershell
.\browser-tests\run.ps1 -Language de
.\browser-tests\run.ps1 -Language en
.\browser-tests\run.ps1 -Suite registration -Language en -Headed
```

Die Sprache lässt sich mit `-Suite`, `-Headed` und `-NoBuild` kombinieren. Jeder Lauf
erhält sein eigenes Archiv mit Oberflächensprache und Browser-Locale. Die 15 Fälle
laufen pro Sprache; Deutsch und Englisch werden nicht gleichzeitig als richtige
Antwort akzeptiert. Beide Läufe zusammen prüfen 30 Fallausführungen.

`i18n/UiTexts` lädt die **originalen** Dateien `src/main/resources/i18n/messages_de.properties`
und `messages_en.properties` über den Test-Classpath. Maven stellt diese Ressourcen
beim Testbuild bereit. Es gibt keinen zweiten, von Hand gepflegten Textkatalog und
keine Abhängigkeit auf Spring oder Vaadin. Der Java-`PropertyResourceBundle` liest die
Zeichenkodierung wie in der Anwendung, einschließlich der vorhandenen deutschen
ISO-8859-1-Datei und UTF-8-Dateien. Die Originaldateien werden nicht umkodiert.
[Java-Ressourcenleser](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/PropertyResourceBundle.html).
Fehlende/leere Schlüssel und nicht unterstützte
Sprachen brechen die Prüfung ab; kein stiller Rückfall auf Englisch oder den Schlüssel.

Anwendung und Browsertests verwenden **denselben Getter-Katalog** in
`shared/src/main/java/de/derpeterson/app/i18n/MessageTexts.java`. `MessageProperties`
ist der Spring-/Vaadin-Adapter und `UiTexts` der Browser-Adapter; beide erben diese
Getter. In einem Page-Helper steht beispielsweise `texts.getLoginEmailField()` statt
eines Properties-Schlüssels. Bei einer Schlüsseländerung wird nur der gemeinsame Getter
angepasst. Ein umbenannter Getter kann über Java-Refactoring in beiden Projekten geändert
werden; anschließend beide Maven-Projekte neu laden und kompilieren.

Die zusätzlichen Source-Roots stellt der
[Maven Build Helper](https://www.mojohaus.org/build-helper-maven-plugin/usage.html) bereit.
Es gibt kein zusätzlich zu installierendes Maven-Artefakt, keine generierten Getter und
keine kopierten Java-Quellen. `UiTexts` lädt die Texte mit der ausgewählten Locale ohne
Spring oder Vaadin. Die Anwendung verwendet standardmäßig ihre aktuelle Session-Sprache;
`MessageProperties.forLocale(provider, Locale.GERMAN)` ist für ausdrücklich festgelegte
Übersetzungen verfügbar. Normale Oberflächen bleiben beim dynamischen Adapter, damit
Sprachwechsel weiterhin funktionieren.

Eine Änderung der Properties wird beim nächsten Testbuild übernommen. Bei `-NoBuild`
muss zusätzlich das wiederverwendete App-Image zu diesen Texten passen; nach Änderungen
am Anwendungscode oder den gemeinsamen Quellen deshalb mit Image-Build starten.

Die Tests wählen die Sprache über das echte Flaggenmenü und prüfen anschließend
exakt die entsprechenden Labels, Buttons, Hinweise und Mailbetreffe. Nach einer
erfolgreichen Anmeldung wird die Testsprache nochmals über das Menü gesetzt, weil
die gespeicherte Benutzersprache die Browser-Vorgabe überschreiben kann. Die Tests
behaupten damit keine Prüfung der automatischen Spracherkennung oder gespeicherten
Sprachpräferenz beim Login. Datumsangaben werden passend zur gewählten Sprache eingegeben.

Formatierte Benachrichtigungen behalten ihre Empfängerparameter; deren HTML-Markup
wird für den sichtbaren Textvergleich in einem inaktiven Template ausgewertet. Die
Anwendungsseite wird dabei nicht verändert. Prüfungen auf Fehlermarkierungen,
Zugriffsverweigerung, Navigation und Mailzustellung bleiben unabhängig davon erhalten.

Routen, feste Seitentitel und vorhandene Avatar-Konstanten stammen direkt aus
`AppRouteConstants` und `AppConstants` unter `shared/src/main/java/`. Die ursprünglichen
Java-Packages und Namen bleiben erhalten. `BrowserRoutes` bildet daraus Browserpfade
und prüft URLs auf der genauen lokalen Runner-Adresse. Sicherheitsmuster mit `/**`
werden nicht als Browserpfade verwendet. Der Mail-Link-Guard prüft weiterhin Host,
Port, Protokoll, UUID-Pfad und fehlende Query-/Fragment-/Benutzerangaben.

Eine Änderung dieser gemeinsamen Werte wird nach erneutem App-Image-Build auch in
den Tests verwendet. Sichtbare Inhalte, Kontorechte, Fehlermarkierungen und Mailzustellung
werden weiterhin eigenständig geprüft: Ein richtiger Titel allein reicht nicht.
Erfundene Testdaten und Bildnamen ohne bestehende gemeinsame Konstante bleiben
als ausdrückliche Testvorgaben erhalten.
Diese Tests prüfen die Verwendung der richtigen Übersetzung; die sprachliche Qualität
des Inhalts benötigt weiterhin eine gesonderte Einschätzung. Änderungen von Schlüsseln
oder der UI-Struktur können weiterhin eine Anpassung der Tests erfordern.

## Ergebnisse ansehen

Jeder Lauf legt ein eigenes Archiv an:

```text
browser-tests/runs/
  2026-10-11T19-40-00_UTC__1234abcd__a1b2c3d4/
    report.md
    metadata.json
    runner.log
    allure-results/
    html/index.html
    html/open-report.bat
```

`report.md` enthält pro Fall die Schritte und Erwartung aus der Testbeschreibung sowie Häkchen, Status, Ist/Grund, UTC-Zeitpunkt und Links zu Screenshots und Traces. Alle Ergebnisse werden automatisch eingetragen, ohne manuelle Abhakpflicht. Die Angaben erfassen UTC-Start/Ende, Git-Commit/Branch, lokale Änderungen, App-Image-ID sowie Aufbau, Testausführung, HTML-Erzeugung und Abbau. Auch übersprungene Fälle, fehlende Belege oder ein fehlgeschlagener Abbau dürfen keinen erfolgreichen Gesamtlauf ergeben.

Der Allure-Bericht enthält die Testfälle, Browseraktionen, Fehlerdetails, Screenshots und Trace-Anhänge. Browseraktionen werden durch die fertige Allure-Playwright-Integration aufgezeichnet. Screenshots und Traces werden auch bei erfolgreichen Fällen behalten, damit du die Belege direkt ansehen kannst. Der Trace Viewer ist direkt im Allure-Bericht über den Trace-Anhang erreichbar. Alternativ kann die ZIP-Ablaufaufzeichnung heruntergeladen und mit `-Trace` separat geöffnet werden. [Allure Playwright Java](https://allurereport.org/docs/playwright-java/), [Playwright Trace Viewer](https://playwright.dev/java/docs/trace-viewer).

`-Report` startet nur den lokalen Berichtserver an `127.0.0.1`; es startet keine Anwendung und führt keine Tests aus. Mit Strg+C beenden. Jeder HTML-Ordner enthält außerdem eine open-report.bat. Per Doppelklick öffnet sie genau den Bericht dieses Laufs, auch wenn inzwischen neuere Läufe vorhanden sind. Das Konsolenfenster geöffnet lassen; mit Strg+C wird der Berichtserver beendet. Die index.html nicht direkt per Doppelklick öffnen, weil Allure seine Daten über HTTP nachlädt. Die BAT-Datei benötigt weiterhin den Testordner samt Maven Wrapper, Java und den bereitgestellten Allure-Werkzeugen.

Ein bestimmter Bericht lässt sich auch ausdrücklich auswählen:

    .\browser-tests\run.ps1 -Report -ReportPath '.\browser-tests\runs\DEIN-LAUFORDNER\html'

Alternativ kannst du den lokal bereitgestellten Allure-Starter verwenden:

```powershell
.\browser-tests\target\tools\allure-2.46.1\bin\allure.bat open '.\browser-tests\runs\DEIN-LAUFORDNER\html' --host 127.0.0.1
```

Allure Java 3.0.0 ist hier der Testadapter; Allure Report 2.46.1 ist der HTML-Generator. Das sind unterschiedliche Werkzeuge mit eigenen Versionsnummern. Playwright Java ist auf 1.63.0 festgeschrieben. Dazu wird der passende Chromium-Testbrowser installiert.

## Java-Dateien verstehen

Alle Java-Dateien liegen unter `src/test/java/de/derpeterson/browsertests/`:

| Datei | Aufgabe |
| --- | --- |
| `suites/InfrastructureBrowserTest.java` | Die drei lesbaren JUnit-Testfälle: Startseite/Bilder, Loginformular, erreichbares Mailpit |
| `suites/AuthenticationBrowserTest.java` | Die sechs unabhängigen Fälle für Anmeldung, Abmeldung und Adminrechte |
| `suites/RegistrationBrowserTest.java` | Drei Fälle für Registrierung, Feldfehler und Korrektur |
| `suites/VerificationBrowserTest.java` | Drei Fälle für Mailaktivierung und gültige/ungültige Linkaufrufe |
| `mail/MailpitInbox.java` | Eigene SMTP-Mail abwarten, lesen und den sichtbaren Mailbutton verwenden |
| `mail/MailpitInboxTest.java` | Fremde Linkziele und die manuelle Umgebung vor Navigation ausschließen |
| `i18n/UiTexts.java / UiTextsTest.java` | Gemeinsame Getter mit fester DE/EN-Sprache verwenden und fehlende Schlüssel/Sprachen absichern |
| `../shared/src/main/java/` | Einzige Definition der Getter, Routen, Seitentitel und vorhandenen App-Konstanten; keine Framework-Abhängigkeiten |
| `pages/` | Kleine Bedienhelfer für Loginformular, Benutzermenü und Benutzeranlage |
| `BrowserSuites.java / BrowserTestAccounts.java` | Suite-Auswahl und erfundene lokale Testkonten |
| `BrowserRoutes.java / BrowserRoutesTest.java` | Gemeinsame Routenkonstanten als Pfade und genaue lokale URL-Erwartungen verwenden |
| `BrowserFixture.java` | Browser und isolierten Kontext bereitstellen; Screenshot und Trace an Allure übergeben |
| `FailureProbeTest.java` | Separat auswählbarer absichtlicher Fehler für den Berichtsvergleich |
| `BrowserTestRunner.java` | Frische Docker-Umgebung starten, JUnit aufrufen, aufräumen und Allure-Generator starten |
| `MarkdownSummary.java` | Projektspezifische Markdown-Checkliste aus fertigen Allure-Ergebnissen erstellen |
| `MarkdownSummaryTest.java` | Protokollstatus, fehlende Fälle und Beleglinks ohne Browser prüfen |

`pom.xml` verwaltet die Testwerkzeuge und bindet die kleinen gemeinsamen Java-Quellen ein. Es hat keinen Spring-Boot-/Vaadin-Parent und verändert die produktiven Abhängigkeiten oder den normalen Java-Testlauf der Anwendung nicht.

## Isolation und Grenzen

Der Starter verwendet [compose.browser-tests.yaml](../compose.browser-tests.yaml). Jeder Java-Lauf startet ein eigenes Projekt `vaadinspringapp-e2e-java-…` mit frischen eigenen Datenbank- und Mailpit-Volumes. Anwendung: `127.0.0.1:18081`, Mailpit: `127.0.0.1:18026`; H2-Webkonsole deaktiviert. Deine manuelle Umgebung auf `18080`/`18025` bleibt bestehen. Diese Ports werden als Browser-Testports abgelehnt.

Mehrere Browser-Testläufe auf denselben Ports nacheinander ausführen. Für einen weiteren gleichzeitigen Lauf in dessen PowerShell-Prozess andere freie Ports einstellen:

```powershell
$env:BROWSER_TEST_APP_PORT = '18082'
$env:BROWSER_TEST_MAILPIT_PORT = '18027'
.\browser-tests\run.ps1
```

`-NoBuild` kann einen älteren App-Stand testen und ist nur bei bewusst unverändertem App-Code sinnvoll. Die tatsächliche Image-ID und Wiederverwendung stehen im Protokoll. Daten und Belege liegen außerhalb von Git; Screenshots/Traces können spätere Testeingaben oder Sitzungstokens enthalten.

Der aktuelle Umfang umfasst Infrastruktur, Login/Logout, Zugriffsrechte, Registrierung und die oben beschriebenen Verifikationsabläufe. Weitere Benutzerabläufe sind im Abschnitt „Suites und Testfälle“ als offen aufgeführt. Nur Chromium, ein Thread, eine explizite Sprache pro Lauf, keine automatischen Wiederholungen. Die zusätzliche Browserprüfung liefert keine Java-Coverage.

Bei normalem Abschluss und Testfehlern werden eigene Container und Volumes entfernt. Hartes Beenden des Prozesses oder ein ausgefallener Docker-Dienst kann das verhindern. Dann nur den genauen Projektnamen aus `metadata.json` zum Abbau verwenden:

```powershell
docker compose -f compose.browser-tests.yaml -p vaadinspringapp-e2e-java-DEINEKENNUNG down --volumes --remove-orphans
```

Keine fremden oder manuellen Docker-Projekte zum Aufräumen einsetzen.

## IntelliJ und weitere Tests

`browser-tests/pom.xml` als zusätzliches Maven-Projekt in IntelliJ hinzufügen (Maven-Fenster → Maven-Projekt hinzufügen). Dann bekommst du Navigation und Autovervollständigung wie bei deinen anderen Java-Tests.

Die Browserfälle werden über den Starter ausgeführt. Ein direkter JUnit-Start ohne verwaltete Testumgebung bricht bewusst ab, damit Tests nicht versehentlich gegen eine laufende Anwendung oder deren Datenbank gehen. Die Tests für MarkdownSummary, BrowserRoutes, Mailpit-Link-Isolation und UiTexts können direkt als gewöhnliche JUnit-Tests ausgeführt werden.

Neue Fälle in der fachlich passenden Suite unter `suites/` ergänzen; für einen neuen Bereich eine eigene Suite-Klasse und einen Eintrag in `BrowserSuites` anlegen. Test-ID, `@DisplayName`, `@Feature`, `@Story` und `@Description` pflegen: Daraus entstehen Gruppierung, Prüfbereich, Schritte und Erwartung im Bericht. Playwrights wartende Assertions verwenden, keine festen Schlafzeiten. Eine automatische Anbindung an das `/start`-Benchmarking ist noch nicht eingerichtet.

## Fehlerhilfe

- Fehlender Testbrowser: `-InstallBrowsers` ausführen. Der normale Starter lädt keine weiteren Browser automatisch.
- Aufbaufehler: Docker Desktop prüfen; `runner.log` und `metadata.json` ansehen. Fremdbelegte Testports ändern.
- Falsches JDK: Der Starter benötigt Java 25. `JAVA_HOME`/IntelliJ-JDK prüfen, keine Bibliotheksversionen zur Umgehung downgraden.
- Bericht startet nicht: Zuerst einen abgeschlossenen Lauf ausführen; `-Report` benötigt dessen HTML-Ordner.
- Maven muss beim ersten Start Werkzeuge herunterladen. Danach sind Bibliotheken und Allure im lokalen Maven-/Build-Verzeichnis vorhanden.

Die Browsertests brauchen kein eigenes npm-Testprojekt. Playwright verwendet intern weiterhin seinen mitgelieferten Browser-Treiber; der von dir bearbeitete Testcode bleibt Java.
