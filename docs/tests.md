# Tests

## Testarten

| Ebene | Prüft |
| --- | --- |
| JUnit / isoliertes H2 | Fachregeln, Transaktionen, Rollback, Rechte, Zeitgrenzen und Konkurrenz |
| Browser DE und EN | Sichtbare Abläufe über echte UI und eigene Mailpit-Mails |
| Browser-Helfertests | Auswahl, Reporter, Sprachkatalog und Runner ohne App-Container |
| JavaScript-Offlinetests | Frontendparser und Bildeditorfunktionen |
| Manuelle Abnahme | Menschliche Wahrnehmung, echte Geräte, Hilfsmittel und Mailclients |

Aktuelle Ergebnisse stehen nur im [Prüfstand](status.md), nicht in einer Kette alter
Laufberichte. Vorhandene Testfälle sind kein Nachweis eines erfolgreich ausgeführten Laufs.

## Große Testrunde mit Codex

Auftrag, Umfang, Sonar-Abgrenzung und Wiederaufnahme stehen gemeinsam bei den
[Codex-Skills](codex-skills.md#große-testrunde). Diese Testanleitung beschreibt die
technischen Prüfungen und ihre Ausführung.

## Übergabe offener Testaufgaben

[docs/testbedarf.md](testbedarf.md) ist die einzige gemeinsame Liste offener
Ergänzungen oder Anpassungen automatisierter Tests, getrennt nach Browser- und
JUnit-Tests. Umsetzungen, Sonar-Pakete, Reviews und QA tragen dort sinnvolle erkannte
Lücken ein, sofern sie diese nicht im selben Auftrag bereits schließen und prüfen. Keine parallelen Listen in TODO,
Abschlussberichten oder Paketplänen als alleinige Übergabe führen.

Ein Eintrag enthält kurz: betroffene Funktion, Testart (JUnit/Browser), Szenario,
erwartetes Verhalten sowie Zustand offen oder wartend mit Grund. Geeignete vorhandene
Tests berücksichtigen, ähnliche Aufgaben zusammenführen und bestehende Einträge erhalten.
Eine Testaufgabe darf auch älter als der letzte Git-Vergleichsstand sein.

Der Agent liest die Liste bei der großen Testrunde, prüft vorhandene Abdeckung,
ergänzt oder korrigiert geeignete Tests und lässt sie ausführen. Playwright führt die
Browserfälle aus; es wertet diese Aufgabenliste nicht selbst aus. Erst nach passender
Implementierung und erfolgreichen Nachweisen den Eintrag entfernen. Bei Browserfällen
gehören DE und EN dazu. Schon passende vorhandene Tests nachweisen, keine Duplikate anlegen.

Erfordert eine Aufgabe zuvor eine neue Fachentscheidung oder einen grundlegenden
Umbau, gezielt fragen. Ohne Antwort bleibt sie mit Grund wartend; unabhängige Arbeiten
gehen weiter. Offener Bedarf wird weder durch einen grünen Teiltest noch beim Löschen
eines erledigten Paketplans entfernt. Im Bericht die verbleibende Lücke nennen.

Menschliche Sonderprüfungen stehen im [manuellen Katalog](manual-tests/README.md).
Die Properties-Vorlage bleibt minimal; keine zusätzlichen Protokollvorlagen oder
fiktiven Ergebnisarchive anlegen. Nach Automatisierung entfallene manuelle Doppelungen
entfernen, Abnahmen mit menschlichem Urteil erhalten.

Die Datei bleibt bei leerer Liste mit einer kurzen Leermeldung bestehen. Keine
Erledigt-Historie anhängen und die Liste nicht blind überschreiben. Der aktuelle
Ergebnisbericht steht in `docs/status.md`, menschliche Sonderprüfungen bleiben im
manuellen Katalog. Der Git-/Codevergleich ergänzt die Liste und erkennt nicht notierte Lücken.

## JUnit-Regressionsprüfungen

Anwendungstests liegen unter `src/test/java/`; vorhandene JUnit-Muster verwenden.
`ImageHelperTest` ist ein Beispiel ohne Anwendungsstart oder Datenbank.
Technische Regeln, Rollback und Konkurrenz bevorzugt hier prüfen, sichtbare Journeys
im Browser. Konkurrenz mit kontrollierter Synchronisierung statt allein mit Wartezeiten
prüfen. Benötigte UI-/Sessionfixtures stark referenzieren; Listener und Kontexte nach
jedem Test bereinigen. Nur isolierte Testdatenbanken verwenden.

Build-Erfolg, ausgeführte Tests und menschliche Abnahme unterscheiden: Ein Build ohne
Testklassen ist kein bestandener Funktionstest. Schon die Compile-Phase kann
Frontenddateien erzeugen; das Maven-Profil `it` startet und stoppt die Anwendung.

## Java und Frontend

Nach Shared-Änderungen zuerst `shared/pom.xml clean install` ausführen. Java25 und
die [Stilprüfungen](entwicklung.md#codestil) gehören vor den Testlauf.

```powershell
# Anwendungstests ohne vollständigen optimierten Build:
.\mvnw.cmd test
# Alternativ: optimierter Build einschließlich aller Anwendungstests:
.\mvnw.cmd clean verify -Pproduction -Dvaadin.productionMode=true
node --test src/test/frontend/*.test.mjs
```

Der eingefrorene OpenCode-Workflow einschließlich seiner Python-/JavaScript-
Werkzeugtests gehört nicht zum gepflegten Standardprüflauf. Keine Anpassungen für
OpenCode vornehmen. Seine Provider-Smokes können echte Anfragen auslösen und werden
ebenfalls nicht ausgeführt. Eine gezielte Wiederaufnahme braucht einen eigenen Auftrag.

Gezielte Regressionen verwenden `-Dtest=KlasseA,KlasseB`; in PowerShell das ganze
Argument zitieren. Datenbanktests verwenden ausschließlich eigene isolierte H2-Datenbanken.
Frisches Schema und Wiederanlauf desselben Schemas werden separat geprüft.

`IdeHttpsProxyTest` prüft den IDE-Starter mit eigenen kurzlebigen Prozessen,
einschließlich hartem Besitzerabbruch mit aktiviertem Java-Debugger. Der optionale
echte Caddy- und Anwendungsstart wird mit `-Dhttps.test.caddy=<absoluter Pfad zur caddy.exe>`
zugeschaltet: eigene temporäre CA, vollständige HTTPS-Zertifikatsprüfung und
frische In-Memory-Datenbank ohne `.env`-Import. Der vollständige Starterfall
benötigt freie IDE-Ports 8080/8081/8443 und verwendet ausschließlich eigene
Prozesse; ein belegter Port führt zum Fehler, vorhandene Dienste bleiben erhalten.
Der Anwendungsprozess startet ohne Testklassen und ohne Windows-Trustimport.
Die zusätzliche menschliche Abnahme des IDE-Komfortstarts ist bestätigt:
Start, Stop, harter Reset, Debug mit Breakpoint und HotSwap wurden vom Benutzer
in IntelliJ erfolgreich geprüft.
Für den vollständigen Starterfall zuvor den optimierten Vaadin-Frontendbuild
erstellen; dieser Fall verwendet dessen Ressourcen und startet keinen Entwicklungs-Frontendbau.

## Sonar-Helfertests

```powershell
.\.venv\Scripts\python.exe -m unittest discover -s scripts/sonar -p test_sonar.py -v
```

Die Offline-Regressionen prüfen Zugangsbehandlung, Pagination, Quellenzuordnung,
Branch-/Parallelitätsschutz und Serverabschluss ohne echten Sonar-/Docker-/Mavenzugriff.
Sie gehören als gepflegte Python-Helfer zur großen Testrunde. Unter Windows wird
auch DPAPI mit künstlichen Testwerten geprüft; echte Zugangsdaten werden nicht gelesen.
Die tatsächliche MCP-Verbindung und der main-Projektscan bleiben separate Abnahmen:
[Einrichtung und Bedienung](entwicklung.md#sonarqube-und-codex).

## Vollständige Browserrunde

```powershell
.\browser-tests\run.ps1 -ReportingTests
.\browser-tests\run.ps1 -Suite all -Workers 2 -Language de
.\browser-tests\run.ps1 -Suite all -Workers 2 -Language en -NoBuild
```

DE baut das optimierte Image einmal; EN verwendet denselben unveränderten Image-Stand.
Der Runner legt pro Worker eigene Datenbank, Mailpit und Ports an und räumt seine
Umgebung wieder ab. Bestehende Entwicklungscontainer werden nicht als Testfixture benutzt.
Keine zweite unabhängige Testausführung gegen dieselben Ausgaben starten, während
der eigene Runner noch arbeitet. Seine getrennten Worker laufen ausdrücklich parallel;
innerhalb jedes Workers bleibt JUnit sequenziell.

Schritte und erwartete Ergebnisse stehen pro Fall in `@Description`, die ID in
`@DisplayName`. Gemeinsame UI-Aktionen gehören in `pages/`, technische Regeln in JUnit.
Neue Fälle nicht als Kopie vorhandener Journeys hinzufügen; konkrete Lücken schließen.

Vor dem Schreiben, Ändern oder Ausführen von Browserfällen die
[Browseranleitung](../browser-tests/README.md) lesen und anwenden. Sie beschreibt
Auswahl, [Worker-Parallelisierung](../browser-tests/README.md#parallelisierung),
Isolation, Fallgestaltung, Reports und Fehlerdiagnose. Lokale Screenshots/Traces
unter `browser-tests/runs/` sind temporäre Diagnosedaten. Wichtige aktuelle Ergebnisse werden knapp im Prüfstand festgehalten.
Browserläufe durch Agenten benötigen eine ausdrückliche Freigabe pro Auftrag.

## Mailansicht mit eigenem Postfach

Der ausdrücklich aktivierte `ManualEmailPreviewIT` sendet Beispiele zur menschlichen
Sichtprüfung. Er gehört nicht zum normalen Testlauf. Einrichtung und Sprachwahl:
[Mailbeispiele zur Sichtprüfung](manual-tests/README.md#mailbeispiele-zur-sichtprüfung).
`MailVisualPreviewTest` prüft Katalog, Empfängergrenzen und MIME-Versand offline mit
gemocktem Transport; dort werden keine echten E-Mails versendet.

## Grenzen

Mailpit prüft tatsächlichen SMTP-Versand im Testnetz, keine reale Internetzustellung.
Headless-Viewporttests ersetzen kein Mobilgerät oder Screenreader. Lokal getestete
Sperr- und Transaktionsverträge sind kein Nachweis eines Mehrinstanzbetriebs.
Coverage ist eine zusätzliche Sonar-Sicht, kein Ersatz für beobachtete Journeys.
