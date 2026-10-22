# Gemeinsames Maven-Projekt und vollständige SonarQube-Analyse

Status: Arbeitspakete 1 bis 6 sind umgesetzt und passend geprüft. Offen sind nur
Integration, Sonar-Servernachweis und gemeinsame Abnahme aus Paket 7.
Paket 6 bleibt uncommittiert; kein Commit, Merge, Push oder Scan beauftragt.

Branch: `feature/multi-modul-sonarqube`. Basis: lokales `main`,
`93086b9d73b00280f3f1e0e560599dc9c37f9656`. Geprüfter Code-Commit vor den
Dokumentationskorrekturen aus Paket 6:
`e9f5884ff923118cf2815380e37e55e8f53cd377`.
`BUGS.md` enthält keine bestätigten offenen Anwendungsfehler.

## Ziel und geltende Entscheidungen

Anwendung, Shared und Browser-Tests gehören dauerhaft zu derselben Anwendung.
Der gemeinsame Maven-Reactor und Sonar-Einstieg verwenden weiterhin genau den
Projektkey `vaadin-spring-app` mit einem gemeinsamen Quality Gate. Jede Analyse
muss vollständig demselben Quellstand zugeordnet sein.

- Root-POM als neutraler Aggregator; `app` behält den Boot-Parent. Boot-/Vaadin-
  Vorgaben und Anwendungsstart bleiben auf `app` begrenzt. Java 25 und bestehende
  Bibliotheksversionen bleiben erhalten.
- `shared` bleibt ein unabhängiges Java-JAR ohne Spring-/Vaadin-Abhängigkeit.
  App und Browser-Tests konsumieren dieses JAR, keine zusätzlich eingehängten Quellen.
- Anwendung und Frontend liegen unter `app/`, Browser-Testcode unter `browser-tests/`.
  Shared wird im Reactor vor seinen Verbrauchern gebaut; Stilprüfung einmal im Root
  mit `-N`. Bestehende lokale Daten-, Log-, Konfigurations- und CA-Ablagen bleiben erhalten.
- Das optionale Profil `coverage` ergänzt nur das technische POM-Modul
  `coverage-report`. App und Shared sind Coverage-Ziele; Browsercode bleibt Testcode
  und liefert Helfermessdaten. Keine pauschalen neuen Quell-/Coverage-Ausschlüsse.
- Der geschützte Sonar-Helfer baut/installiert den gesamten Reactor mit gleichen
  Profilen für Build, Maven-Modell und Scanner. Der gemeinsame Coverage-Prüfer
  verhindert unvollständige Nachweise vor genau einem Upload. Hauptbranch,
  Sauberkeit, Scanlock, sichere Tokenbehandlung und konkreter Serverabschluss bleiben Pflicht.
- Normale Maven-Prüfungen starten keine Browserjourneys, Container oder Trustimporte.
  Freigegebene Browserläufe verwenden getrennte Worker und dasselbe unveränderliche
  Image. Keine Entwicklerdaten oder fremden Prozesse als Fixtures, keine echten Mails
  und keine TLS-Fehlerausnahmen. Der eingefrorene OpenCode-Workflow bleibt unverändert.

Gültiger Aufbau und Bedienung: [Entwicklung](../entwicklung.md),
[Tests](../tests.md), [Docker](../docker/README.md),
[Browseranleitung](../../browser-tests/README.md) und [Codex-Skills](../codex-skills.md).

## Aktueller Nachweis und Freigaben

Die gemeinsame Funktionsprüfung ist im [Prüfstand](../status.md) dokumentiert:
frischer optimierter Reactor-Install, 2.244 App-JUnit bestanden und zwei erwartete
optionale Skips, 83 Browser-Helfer, 44 Sonar- und 18 Coverage-Offlinefälle sowie
12 Frontendtests bestanden. Isolierter JAR-Start und Browser `all` DE/EN mit je
161/161 Fällen sind einschließlich Aufbau/Abbau erfolgreich. Beide Sprachen
verwenden dasselbe aktuelle Image; kein Zertifikatsdialog, keine Truständerung
oder verbliebene eigene Workerressource.

IDE-Start/Stop, harter Reset, Debug/Breakpoint und HotSwap sind vom Benutzer bestätigt;
Start/Stop und Debug nach Maven-Neuverknüpfung mit eindeutigem Modul `app` ebenfalls.
Diese Abnahmen nicht erneut zur Pflicht machen. Pakete 1 bis 5 sind committet;
Paket 6 ist technisch abgeschlossen und uncommittiert.

Paket 7 benötigt einen weiteren Auftrag mit ausdrücklicher Commit-/Merge- und
Scanfreigabe. Der erfolgreiche lokale Teststand ist keine Main-Integration,
Serveranalyse oder gemeinsame Gesamtabnahme. Keine automatische Sonar-Bereinigung.

## Offenes Arbeitspaket

### 7. Integration, Servernachweis und gemeinsame Abnahme

Nach ausdrücklichem Commit-/Mergeauftrag lokal integrieren und den gesamten
aktuellen main-Stand über `scripts/sonar/sonar.py scan` analysieren. Community Build
weiter nur als main verwenden; keinen Feature-Branch unter falscher Quellenzuordnung
hochladen. Konkreten Server-Task, Analyse-ID, Revision, vollständigen Modul-/Quell-/
Testumfang, Reports, Gate, sämtliche Findings und Hotspots prüfen. Bestehende
Accepted-/False-positive-Begründungen erhalten und nach Dateiverschiebungen ihre
wirkliche Zuordnung kontrollieren. Keine pauschale erneute Ausnahmevergabe, kein
Zurücksetzen der Baseline oder Schwächen des Gates.

Ziel ist ein grünes gemeinsames Gate bei korrektem Umfang. Neu sichtbar werdende
Shared-/Browser-Findings fachlich bewerten; Umbaufehler und überschaubare nötige
Korrekturen gezielt beheben und nachprüfen. Eine größere unabhängige Bestands-
bereinigung vorher gesondert abstimmen. Ein rotes Gate oder fehlender Modulnachweis
bleibt offen und verhindert den behaupteten grünen Gesamtabschluss.

Danach gemeinsame Abnahme. Erst bei tatsächlichem Abschluss gültige Fachdokumentation
pflegen und diesen Plan samt erledigten TODO-/Dokumentationsverweisen entfernen.
Offenen automatisierten Testbedarf zentral erhalten. Kein Push ohne Auftrag.

Technische Grundlagen: [Maven-Reaktor](https://maven.apache.org/guides/mini/guide-multiple-modules.html),
[SonarScanner für Maven](https://docs.sonarsource.com/sonarqube-server/analyzing-source-code/scanners/sonarscanner-for-maven),
[JaCoCo-Berichte und Modulaggregation](https://docs.sonarsource.com/sonarqube-server/analyzing-source-code/test-coverage/java-test-coverage).
