# Nächtliche QA: Benachrichtigungen, Live-Rollen und Wartung

## Stand und Auftrag

Ausgangspunkt ist der saubere Branch `feature/user-messages` mit Commit
`3fc09c18d383d51e3954a69d80f4efcef58c142e`. Die Prüfung begann nach dem vereinbarten
Termin am 19.10.2026 um 04:00 Europe/Berlin. Alle nachfolgend beschriebenen
Test-/Dokumentationsänderungen bleiben uncommittet. Keine automatische Übernahme,
kein Branchwechsel, Stage, Commit, Merge oder Push.

Das feste Produktionsimage ist
`sha256:129b59e552859b7d2dd242e41c963e3dc370810062c48f8d318a74b1b0ce6cdf`.
Es waren keine Produktionscodeänderungen erforderlich. Die spätere Korrektur
der Kalenderprüfung und die ergänzten Browserassertions verändern ausschließlich
den Testcode; das Image bleibt daher für alle Browserrunden identisch.

## Ausdrückliche Bereichsausnahmen

Nicht geprüft oder überarbeitet wurden das Unterpanel der gesendeten
Benachrichtigungen einschließlich Details/Pagination und das Benachrichtigungs-
Header-Overlay einschließlich Einträgen/Aktionen. Ihre noch veränderliche
Darstellung und UI-Bedienung gehören nicht zum freigegebenen Prüfumfang.

Fünf JUnit-Ausprägungen werden für diesen Lauf über den optionalen Tag
`notification-ui-pending-redesign` nicht ausgewählt: zwei kompakte Vorschau-
Sprachvarianten, das Öffnen/Schließen des Header-Overlays und zwei aus dem
Verfassen-Wartungstest ausgegliederte Historien-Paginationsvarianten. Alle
Assertions bleiben erhalten. Die normale Standardausführung schließt den Tag
nicht aus; es gibt keine dauerhafte Deaktivierung. Die persönliche vollständige
Inbox, Header-Zähler/Sessionbindung, Dienste, Persistenz und Berechtigungen
bleiben im Umfang. Empfängerdetails der **geprüften Verfassen-Vorschau** sind
weiterhin ausdrücklich enthalten. Bestehende Browserfälle der beiden unfertigen
Oberflächen gab es nicht; deshalb wurden keine Browserfälle pauschal verworfen.

## Ergänzungen und Befunde

- Fünf neue Browserausprägungen prüfen Verfassen bei 1280 und 390 Pixeln,
  Einzel-/Sammelversand, persönliche persistente Inboxen und Lesestatus sowie
  beide Versandpanels bei Wartungswechsel im anderen Fenster.
- USER-07H verwendet zwei getrennte Benutzerkontexte. Eine angemeldete normale
  Sitzung erhält Adminnavigation durch Rollenzuweisung; nach Entzug im geöffneten
  Adminformular landet sie auf Home. Die Session-ID bleibt gleich, es gibt keinen
  weiteren Login-POST und die persönliche Rollenbenachrichtigung ist sichtbar.
- SESSION-01A folgt demselben erhaltenen Login statt der alten Logout-Erwartung.
  Die vier Sperr-/Löschvarianten verlangen weiterhin die ursprüngliche 401-Abwehr
  und anschließende Loginmaske.
- USER-07E wählt vor dem Selbstentzug eine normale Rolle, weil Bootstrap-Admins
  jetzt nur Admin besitzen. Die bisherige Sicherheitsassertion bleibt erhalten;
  die vollständig leere Rollenauswahl wird bereits vom Formular abgewiesen.
- Eine neue Helperprüfung sichert die Aufnahme der fünf Benachrichtigungsfälle
  in `all`; `fast` bleibt bei 34 Fällen. Die Gesamtsuite hat 27 Klassen/147 Fälle.
- Der nicht kompakte Inbox-Sprachtest ist von den bewusst ausgenommenen kompakten
  Overlayvarianten getrennt. Der Wartungstest des Verfassenpanels bleibt vollständig
  aktiv; seine Historien-Paginationsassertion bleibt als eigene getaggte Prüfung erhalten.

Die erste gezielte deutsche Vorprüfung hatte 13 Fälle: sieben bestanden, vier
Assertions fehlgeschlagen und zwei Ablauf-/Fixturefehler. Korrigiert wurden die
Prüfung nativer Inputs statt eines deaktivierten Custom-Element-Hosts, der
Vaadin-Dialoghost statt des textlosen portalisierten Overlaygerüsts, eine fehlende
Adminnavigation im neuen Rollenfall und die oben beschriebene Bootstrap-Fixture.
Diese rote Vorprüfung bleibt als Befund erhalten; sie war kein vollständiger Lauf.

Der erste deutsche Gesamtlauf hatte einen weiteren Fehler in UI-01B. Der Trace
zeigte einen Mausklick auf die noch nachscrollende mobile Kalenderliste unmittelbar
nach Pfeiltastennavigation. Der Test wartet nun nach nativem Scrollen auf eine über
300 ms stabile sichtbare Zellenposition und klickt dann tatsächlich. Keine
Zustandsinjektion, erzwungenen Klicks, Escape-Abkürzung oder abgeschwächten
Datumsassertions. Auswahl, Wiederöffnen und tatsächliche Registrierung sind im
gezielten Nachtest erfolgreich. Die erste rote Gesamtrunde wird nicht verdeckt.

## Tatsächliche Ergebnisse

| Prüfung | Ausgeführt / bestanden | Fehler / Skips | Umfang |
| --- | ---: | --- | --- |
| Anwendungs-JUnit | 1706 / 1706 | 0 Fehler, 0 Skips | Zusätzlich fünf oben benannte Ausprägungen ausdrücklich nicht ausgewählt |
| Browser-Helper/Reporter | 48 / 48 | 0 Fehler, 0 Skips | Suite-Auswahl, Workeraufteilung, Belege und Berichte |
| Python offline | 205 / 205 | 0 Fehler, 0 Skips | Vorhandene gemockte Workflow-/Provider-/Git-Fixtures |
| JavaScript offline | 26 / 26 | 0 Fehler, 0 Skips | 14 Workflow-/SDK-Fixtures, 12 Profilbild-/Zeiteingabefälle |
| Browser DE, erster Gesamtlauf | 147 / 146 | 1 Kalenderfehler, 0 Skips | Zwei isolierte Workers, vollständiger freigegebener Katalog |
| Browser DE, gezielter Nachtest | 10 / 10 | 0 Fehler, 0 Skips | PresentationBrowserTest und UserNotificationsBrowserTest mit finalen Helfern |
| Browser EN, Gesamtlauf | 147 / 147 | 0 Fehler, 0 Skips | Dasselbe Produktionsimage, zwei eigene Workers |

Die zehn Nachtests sind keine zehn zusätzlichen unabhängigen Produktfunktionen.
Im deutschen Umfang sind damit alle 147 eindeutigen Katalogfälle mindestens einmal
erfolgreich geprüft; die Änderung der Testhelfer betrifft die separat nachgeprüften
Klassen. Ein zweiter identischer deutscher Gesamtlauf wurde nicht gestartet.

Shared ist gebaut/installiert, Anwendung und Tests kompiliert. Der isolierte
Docker-Produktionsbuild einschließlich erzwungenem Frontend-Build und Verify ist
erfolgreich; sein Testlauf verwendet dieselben ausdrücklichen JUnit-Ausnahmen.
Hierzu wurde ausschließlich in der eigenen Buildkopie der Verify-Aufruf im
Dockerfile um den Lauf-Filter ergänzt. Das versionierte Dockerfile blieb unverändert.

## Coverage und Messgrenzen

JUnit-JaCoCo: **8978/10450 Zeilen (85,91 %)** und **2566/3474 Zweige (73,86 %)**.
Gemessen sind die 209 ausführbaren Anwendungsklassen unter
`de.derpeterson.app`; Tests, Shared-JAR und Bibliotheken sind nicht im Nenner.
Der Nenner enthält auch derzeit bewusst nicht ausgeübte UI-Implementierung.

Die erste deutsche Browser-Gesamtrunde misst separat **8949/10450 Zeilen
(85,64 %)** und **2248/3474 Zweige (64,71 %)**. Die englische Gesamtrunde misst separat **8958/10450 Zeilen (85,72 %)** und
**2250/3474 Zweige (64,77 %)**.

Zusammengeführt aus DE-Gesamtlauf, deutschem Nachtest und EN-Gesamtlauf:
**8960/10450 Zeilen (85,74 %)** und **2254/3474 Zweige (64,88 %)**.
Vor dem tatsächlichen JaCoCo-Rohdatenmerge wurde die identische SHA-256-Prüfsumme
aller drei Produktions-JARs bestätigt:
`0558dba214b95310c675492b252ab2423b3919d6927f2f068a5728f784c93e18`.
Die erste gezielte 13-Fall-Vorprüfung wurde nicht in diesen Gesamtwert eingemischt.
Browser-JaCoCo misst serverseitigen Code einschließlich Anwendungsstart,
Hintergrundaufgaben und der ausgewählten Browserabläufe gegen die Klassendateien
im exakt getesteten Produktions-JAR. Ausführungsdateien dürfen nur bei gleichem
Image/JAR vereinigt werden; Prozentwerte werden nicht gemittelt oder addiert.

Fall-Erfolgsquoten sind keine vollständige Funktions- oder Codeabdeckung. Die
Funktionszuordnung steht im Browserfallkatalog; über alle denkbaren fachlichen
Kombinationen existiert kein belastbarer prozentualer Nenner. Keine CSS-/JS-
Coverage gemessen. Schmale Chromium-Ansichten sind kein Realgeräte-, Screenreader-
oder Mailclientnachweis. Neue Rollenbrowserfälle belegen Home/Admin und normale
Anmeldung; Konto-Tab-Erhalt und Remember-me nach Rollenwechsel sind technisch
abgesichert, aber nicht als neue eigenständige Browserkombination behauptet.

## Isolation und Übernahme

Tests verwendeten eine eigene Quellkopie, eigene H2-Daten, zwei getrennte
JVM-/Docker-/Mailpit-Stacks je Browserrunde und erfundene Benutzer. Die reale
Datenbank, .env, Postfächer sowie IntelliJ/HotSwap/Vite blieben unberührt. Für den
Workflow-JavaScript-Offlinetest wurden vorhandene SDK-Abhängigkeiten lesend in der
Kopie referenziert; keine konkurrierende Installation. Lokale Test-SMTP-Fixtures
und Mailpit sind keine realen Mailprovider.

Alle eigenen Docker-Projekte und Volumes sind nachweislich abgebaut; geprüft wurden
sowohl die Runner-Metadaten als auch die ausschließlich eigenen Projektnamen in den
Dockerlisten. Alle vier eigenen Browserrunden einschließlich der roten Vorprüfung,
JUnit-/Helperberichte, Coverage-Rohdaten und HTML-Berichte bleiben im ignorierten
Run-Bereich erhalten. Vorhandene ältere Runs wurden nicht gelöscht.

355 Java-/Properties-/CSS-/POM-Dateien wurden per SHA-256 mit der tatsächlich
getesteten Kopie abgeglichen. Die einzige Dockerfile-Abweichung war der ausdrücklich
genehmigte JUnit-Lauffilter in der isolierten Kopie. Die SDK-Verknüpfung wurde ohne
Rekursion in ihr Ziel gelöst, die eigene Buildkopie danach entfernt. Formatierung,
Diffprüfung und Änderungsreview sind erfolgreich. Keine erzeugten Dateien oder
Abhängigkeiten wurden in den versionierten Produktstand übernommen.

Die einmalige Automation wurde nach Abschluss gelöscht; es wurden keine Folgetermine angelegt. Da nur Tests und Dokumentation verändert
wurden, ist kein Anwendungsneustart zur Übernahme dieser Änderungen erforderlich.
