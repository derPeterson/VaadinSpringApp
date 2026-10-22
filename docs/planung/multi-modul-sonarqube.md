# Gemeinsames Maven-Projekt und vollständige SonarQube-Analyse

Status: Arbeitspakete 1 und 2 sind umgesetzt und mit passenden Build-/JUnit-/
Stilprüfungen nachgewiesen. Der angepasste IDE-Start ist vom Benutzer mit
Start/Stop, Debug und Breakpoint bestätigt. Pakete 3 bis 7 sind noch nicht freigegeben.
Keine Browserjourneys, Scans, Commits, Merges oder Pushes.
Branch: `feature/multi-modul-sonarqube`, Basis: lokales `main`,
`93086b9d73b00280f3f1e0e560599dc9c37f9656`. Arbeitsstand vor Plananlage sauber.
`BUGS.md` enthält keine bestätigten offenen Anwendungsfehler.

## Ziel und geprüfter Ausgangsstand

Anwendung, Shared und Browser-Tests gehören dauerhaft zu derselben Anwendung.
Sie erhalten einen gemeinsamen Maven-Einstieg und werden unter dem bestehenden
Sonar-Projekt `vaadin-spring-app` mit einem gemeinsamen Quality Gate analysiert.
Jede Analyse muss vollständig demselben Quellstand zugeordnet sein.

Ausgang vor dem Umbau: drei eigenständige Maven-Projekte ohne gemeinsamen Aggregator.
Die bisherige Root-POM baut das Anwendungs-JAR; Shared wird vorher separat installiert.
Der geschützte Sonar-Helfer analysiert anschließend nur das Hauptmodul.
Shared enthält fünf Java-Dateien, das Browsermodul 76 Java-Dateien im Testquellbaum.
Die bisherige grüne Analyse gilt für ihren dokumentierten bisherigen Umfang;
für die Planung wurde keine neue Analyse durchgeführt.

Konkrete Umbaustellen sind unter anderem `Dockerfile` und `.dockerignore`,
`browser-tests/run.ps1`, dessen Ressourcenverweis auf `../src/main/resources`,
`BrowserCoverage`, der IDE-Starter mit dem bisherigen Arbeitsverzeichnis als
Repositoryroot, pfadabhängige Ressourcen-/JUnit-Tests sowie die Root-Stilprüfungen.

## Vorgeschlagenes Zielbild

```text
pom.xml                 Gemeinsamer Aggregator und gemeinsame Buildregeln
app/                    Spring-Boot-/Vaadin-Anwendung samt Quellen und Frontend
shared/                 Eigenes Java-JAR mit gemeinsamen Texten und Regeln
browser-tests/          Browserfälle, Runner und technische Helfertests
scripts/, config/       Gemeinsame Werkzeuge und Prüfvorgaben
docs/, .agents/         Dokumentation und Projektabläufe
```

- Root-POM mit Packaging `pom`; Modulnamen und bestehende JAR-Koordinaten möglichst
  erhalten. Gemeinsame Java-/Pluginvorgaben zentral verwalten. Spring-Boot-/Vaadin-
  Abhängigkeiten, Repackage, Frontendbau, Startziele und Profil `it` bleiben auf `app`
  begrenzt. Shared und Browser-Tests bekommen keine Anwendungsabhängigkeit.
- `app` und Browser-Tests konsumieren weiterhin das Shared-JAR, keine eingehängten
  Shared-Quellen. Shared bleibt frei von Spring und Vaadin.
- Ein Reactor-Build baut benötigte Module in Abhängigkeitsreihenfolge. Gezielte
  Modulbuilds bleiben möglich. Root-Aufrufe führen nur ausdrückliche Ziele aus;
  der bisherige automatische Anwendungsstart ohne Ziel wird nicht vererbt.
- Eigene Vaadin-/Frontendquellen, Frontendtests und zugehörige Konfiguration gehören
  zu `app`. Generierte Frontenddateien entstehen durch den Build; keine manuellen
  Änderungen an generierten Inhalten und keine ungefragten Versionsupdates.
- Wrapper, gemeinsame BAT-Einstiege und HTTPS-Werkzeuge bleiben im Repositoryroot.
  Bestehende lokale `.env`, Daten-, Log- und CA-Ablagen bleiben an ihrem Ort und
  werden weder gelesen noch verschoben oder als Tests verwendet. Arbeitsverzeichnis
  und Modulpfad werden an den Einstiegspunkten ausdrücklich passend festgelegt.
- Normales `test`/`verify` und der Sonar-Ablauf starten keine Browserjourneys,
  Container, Trustimporte oder echte Mails. Die Helfertests im Browsermodul bleiben
  ausführbar; bestehende Browser-Tags und die Workerisolation bleiben erhalten.

## Aktueller Teilstand

Die Root-POM aggregiert `shared`, `app` und `browser-tests`. Die Anwendungsquellen
und eigenen Frontenddateien liegen unter `app`; alle 365 verschobenen Dateien sind
inhaltlich unverändert. `app` behält seinen Boot-Parent mit explizit leerem
`relativePath`; Shared und Browser-Tests erben die neutrale Root-POM. App-Abhängigkeiten
und verbleibende Pluginversionen sowie der Browser-Abhängigkeitsbaum sind gegenüber
dem Ausgang unverändert. Stilprüfungen laufen mit `-N` einmal zentral über alle Module.
Der originale Text-/Mailressourcenverweis des Browsermoduls ist bereits angepasst,
damit die normalen Helfertests im Reactor funktionieren; die übrige Runnerumstellung
bleibt Paket 3.

Nachgewiesen: optimierter `clean verify` des gesamten Reactors mit eigener, zunächst
leerer Maven-Ablage ohne installierte Projektartefakte; 2.242 App-JUnit bestanden,
zwei erwartete optionale IDE-Skips, 75 Browser-Helfertests bestanden. Zusätzlich
gezielter App-/Shared-Paketbuild mit `-pl app -am` ohne Tests zu wiederholen,
Shared-Auswahl samt leerem Abhängigkeitsbaum, 12 Frontend-Offlinetests sowie
Spotless apply/check, Checkstyle und `git diff --check` bestanden. Das gepackte
App-JAR enthält Schema, DE-/EN-Ressourcen und Shared-JAR, keine IDE-Werkzeuge.
Die optionalen echten Starterprüfungen wurden anschließend in Paket 2 aktiviert.

IntelliJ-Konfiguration und Boot-Maven-Plugin setzen das Laufzeitverzeichnis
explizit auf den Repositoryroot und den Vaadin-Projektpfad auf `app`. Die isolierten
Starter-/HTTPS-Regressionen sind mit 24 bestandenen Fällen nachgewiesen, einschließlich
Entwicklungs-/Produktionsmodus, Debugger, TLS-/CSS-Prüfung und Prozessabbau. Auch der
isolierte Start des gepackten JARs ist bestanden. Das Entwicklungsbundle wurde beim
echten Entwicklungsstart automatisch neu erzeugt; Paket-/Lockdateien sind unverändert.
Der optimierte Paketbuild, Spotless apply/check, Checkstyle, das effektive
Boot-Maven-Modell und die PowerShell-Syntaxprüfungen sind erfolgreich. Die Root-BAT-
und PowerShell-Einstiege behalten ihre bisherigen Rootpfade und die bestehende CA.
Die zunächst fehlende IntelliJ-Startklasse war dem falschen Modulklassenpfad
zugeordnet; Run/Stop, Debug und Breakpoint sind vom Benutzer erfolgreich bestätigt.
Nach dem erneuten Verknüpfen ausschließlich der Root-POM heißt das Anwendungsmodul
`app`; die gespeicherte Run-Konfiguration verwendet diese eindeutige Zuordnung.
Der Benutzer hat die Funktion auch nach diesem Maven-Neuimport bestätigt.
Die Entwicklungshilfe beschreibt die gemeinsame Maven-Verknüpfung und Modulwahl.

Dies ist kein vollständiger Featureabschluss und keine neue große QA. Docker,
Browserstarter, optionale Browser-Coverage und Sonar-Helfer bleiben offen.
Die bestehende Sonar-Analyse ist kein Nachweis für die neue Struktur.

## Offene Arbeitspakete

### 3. Docker, Browserrunner und Testressourcen

Docker-Build auf den Reactor und `app`-Artefaktpfad umstellen. Buildkontext und
Allowlist müssen alle benötigten POMs/Buildvorgaben enthalten und weiterhin lokale
Geheimnisse, Daten und Ausgaben ausschließen. Unbenötigte Browserausführung bleibt
auch im Imagebuild aus. Laufzeitimage, Nutzerrechte, HTTPS-/Health-Vertrag erhalten.

Browserstarter auf eine gemeinsame abhängige Modulauswahl umstellen; bestehende
klassenweise Worker-Parallelisierung behalten. Originalübersetzungen und Mailvorlagen
aus dem neuen Anwendungsressourcenpfad beziehen, keine Kopien oder Anwendungs-JAR-
Abhängigkeit einführen. Testfixtures, Schema-/Theme-/Ressourcenpfade und den optionalen
Browser-Coveragepfad anpassen. Bei optionaler Coverage nur zum tatsächlich geprüften
Image passende App-/Shared-Klassen und Quellen verwenden, einschließlich des Shared-
JARs; keine alten Laufdaten stillschweigend übernehmen.

Nachweis nach Freigabe: Runner-/Coverage-/Ressourcenhelfer, frischer isolierter
Imagebuild und Startprüfung; Browser DE/EN erst mit ausdrücklicher Freigabe.
Beide Sprachen am selben unveränderten Image, keine konkurrierenden Builds.
Abhängigkeiten: Pakete 1 und 2. Risiko: alte Hostpfade funktionieren nur durch Caches.

### 4. Vollständiger Sonar-Umfang und korrekte Coverage

Eine Reactor-Analyse unter dem bestehenden Projektkey konfigurieren; keine drei
Projektkeys und keine aufeinanderfolgenden Teiluploads auf denselben Key.
App-/Shared-Quellen und sämtliche Browser-Testquellen mit aktuellen Klassen und
Abhängigkeiten erfassen. Testcode als Testcode belassen; seine Qualitätsregeln
anwenden, ohne ihn für Coverage künstlich als Anwendungscode zu klassifizieren.
Keine pauschalen neuen Ausschlüsse zur Gate-Beruhigung.

JaCoCo-Ausführung und XML-Berichte so zuordnen, dass auch Shared-Code, den App-
oder Helfertests ausführen, korrekt berücksichtigt wird. Bei nötiger Aggregation
ein rein technisches Berichtsmodul im Coverage-Profil vorsehen; die drei fachlichen
Module bleiben getrennt. Gemessene Null und fehlender Bericht müssen unterscheidbar
sein. Die optionale Browsermessung bleibt separat und löst keine Browserläufe aus.

Nachweis nach Freigabe: Quell-/Testdateien aller Module vollständig und ohne
Doppelindexierung nachlesen; vorhandene modulübergreifende Tests zur Coverage-
Zuordnung nutzen. Zählwerte und neue Gate-Bewertung ehrlich vom bisherigen engeren
Analyseumfang unterscheiden. Abhängigkeiten: Pakete 1 und 3.

### 5. Geschützter Sonar-Ablauf und Codex-Skills

`scripts/sonar/sonar.py` samt Offline-Regressionen auf gemeinsamen Build, aktuelle
Reports und eine einzige Gesamtanalyse umstellen. JDK-25-Prüfung, sauberes main,
Scanlock, Serverwarteschlange, sicherer Tokenzugriff, feste Revision und Abwarten
des konkreten Serverauftrags erhalten. Unvollständiger Build oder fehlendes Modul
darf keine erfolgreiche Gesamtanalyse ergeben. Ein aktueller Bericht und Zustand
genügen weiterhin; keine Berichtsdateien pro Modul oder neue Archivserie.

Alle fünf Projekt-Skills auf tatsächlich betroffene Pfade, Build-/Testaufrufe,
Artefakte und den gemeinsamen Sonar-Vertrag abgleichen und gezielt aktualisieren.
Insbesondere Sonar-Bereinigung, große Testrunde und lokaler Merge müssen sämtliche
Module berücksichtigen. Bestehende Git-/Browserfreigaben und Kontingentregeln bleiben
erhalten. Gepflegte Sonar-Einstiege konsistent dokumentieren; der eingefrorene
OpenCode-Workflow bleibt unverändert.

Nachweis nach Freigabe: Offline-Helfertests für Reactor-Aufrufe, fehlgeschlagene
Modulbuilds, fehlende Reports und Quellenzuordnung erweitern, vorhandene Sicherheits-
und Serverabschlussfälle erhalten. Eine echte main-Analyse ist erst in Paket 7
nach gesonderter Integration möglich. Abhängigkeit: Paket 4.

### 6. Dokumentation und gemeinsame Funktionsprüfung

README, Entwicklungs-/Test-/Docker-/Browseranleitung, Projektregeln und Skill-
Bedienung auf tatsächliche neue Pfade und Befehle aktualisieren. Keine zweite
Versionsliste und keine Unterstützung der überholten Ordnerstruktur aufbauen.
Alle gepflegten Aufrufer, Ressourcen und Verweise im Aufgabenbereich abgleichen.

Nachweis nach Freigabe: vollständiger optimierter Reactor-Build, App-JUnit,
Browser-Helfer, gepflegte Python-/Frontend-Offlinetests und vorgeschriebene
Stilprüfungen. Frischer isolierter JAR-/Docker-Start. Wegen der breiten Pfadänderungen
ist abschließend eine vollständige Browserrunde DE/EN vorgesehen, benötigt aber
einen ausdrücklichen Auftrag. Resultate und offene Fehler in `docs/status.md`,
offene automatisierte Nachweise ausschließlich in `docs/testbedarf.md` erhalten.
Abhängigkeiten: Pakete 1 bis 5. Größter Prüfaufwand: vollständige Browserrunde.

### 7. Integration, Servernachweis und gemeinsame Abnahme

Nach ausdrücklichem Commit-/Mergeauftrag lokal integrieren und den gesamten
aktuellen main-Stand analysieren. Community Build weiter nur als main verwenden;
keinen Feature-Branch unter falscher Quellenzuordnung hochladen. Konkreten Server-
Task, Analyse-ID, Revision, Module, Reports, Gate, vollständige Findings und Hotspots
prüfen. Bestehende Accepted-/False-positive-Begründungen erhalten und nach
Dateiverschiebungen ihre tatsächliche Zuordnung kontrollieren; keine pauschale
erneute Ausnahmevergabe, kein Zurücksetzen der Baseline oder Schwächen des Gates.

Ziel ist ein grünes gemeinsames Gate bei korrektem Umfang. Neu sichtbar werdende
Shared-/Browser-Findings fachlich bewerten; Umbaufehler und überschaubare nötige
Korrekturen gezielt beheben und nachprüfen. Eine größere unabhängige Bestands-
bereinigung vor Ausführung gesondert abstimmen. Ein rotes Gate oder fehlender
Modulnachweis bleibt ausdrücklich offen und verhindert den behaupteten grünen
Gesamtabschluss. Danach gemeinsame Abnahme; erst bei tatsächlichem Abschluss Plan
und erledigte TODO-/Dokumentationsverweise entfernen. Kein Push ohne Auftrag.

## Freigaben und Abschlusskriterien

Die freigegebenen Arbeitspakete 1 und 2 sind umgesetzt und geprüft.
Pakete 3 bis 7 benötigen einen weiteren Umsetzungsauftrag; nötige eigene
isolierte Anwendungs-/Dockerstarts, Browser DE/EN sowie main-Integration und Upload
im jeweiligen Auftrag ausdrücklich mit erfassen. Keine automatische große QA,
Sonar-Bereinigung, Commits oder Merges aus bloßer Planabnahme.

Fertig ist das Feature erst bei funktionsfähigem gemeinsamen und gezieltem Build,
korrekter Modultrennung, erhaltenem IDE-/HTTPS-Komfort, belegtem Test-/Ressourcen-
verhalten und einer revisionsgebundenen vollständigen Sonar-Analyse samt gemeinsamer
Abnahme. Bestehende Daten und Windows-Vertrauen bleiben durch den Umbau unberührt.
Keine neue Fachfunktion, Designänderung, Java-Absenkung oder Versionsmodernisierung.

Technische Grundlagen: [Maven-Reaktor](https://maven.apache.org/guides/mini/guide-multiple-modules.html),
[SonarScanner für Maven](https://docs.sonarsource.com/sonarqube-server/analyzing-source-code/scanners/sonarscanner-for-maven),
[JaCoCo-Berichte und Modulaggregation](https://docs.sonarsource.com/sonarqube-server/analyzing-source-code/test-coverage/java-test-coverage).
