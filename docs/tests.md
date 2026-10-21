# Tests

## Testarten

| Ebene | Prüft |
| --- | --- |
| JUnit / isoliertes H2 | Fachregeln, Transaktionen, Rollback, Rechte, Zeitgrenzen und Konkurrenz |
| Browser DE und EN | Sichtbare Abläufe über echte UI und eigene Mailpit-Mails |
| Browser-Helfertests | Auswahl, Reporter, Sprachkatalog und Runner ohne App-Container |
| JavaScript-Offlinetests | Frontendparser und Bildeditorfunktionen |
| Pythonprüfungen | Lokale Test-/Benchmarkwerkzeuge |
| Manuelle Abnahme | Menschliche Wahrnehmung, echte Geräte, Hilfsmittel und Mailclients |

Aktuelle Ergebnisse stehen nur im [Prüfstand](status.md), nicht in einer Kette alter
Laufberichte. Vorhandene Testfälle sind kein Nachweis eines erfolgreich ausgeführten Laufs.

## Codex: Große Testrunde

Der Projekt-Skill [grosse-testrunde](../.agents/skills/grosse-testrunde/SKILL.md)
fasst den vereinbarten Gesamtauftrag zusammen. Im tatsächlichen Projektverzeichnis
„Starte die große Testrunde“ schreiben oder den Skill mit `$grosse-testrunde` aufrufen.
Der ausdrückliche Gesamtauftrag umfasst Browser DE/EN; keine zweite Freigabe nötig.
Einzelne Änderungen oder das Prüfen des Skills starten keine Gesamtrunde.

Der Ablauf bewertet Änderungen seit der letzten vollständigen QA, ergänzt sinnvolle
Regressionen, behebt Fehler innerhalb bestehender Verträge und führt die vorhandenen
Prüfungen aus. Neue Fachfunktionen oder grundlegende Vertragsänderungen werden abgestimmt.
Sonar wird mitgeprüft; kleine fachlich klare Fehler und Blocker werden gezielt behoben.
Größere Umbauten und umfangreiche Finding-Pakete gehören zum separaten Auftrag
„Sonar bereinigen“, dessen Ziel möglichst null berechtigte offene Issues ist.
Eine bestandene Testrunde setzt keine Null-Issue-Zahl voraus: Testergebnisse, offene
Sonar-Findings und Quality Gate werden getrennt ausgewiesen. Keine Umbau-/Scanschleife
bis null während der QA. Sonar-Ergebnisse werden nur mit passendem Analysestand bestätigt;
die direkte Anbindung ist noch offen. Kein echter Mailversand oder automatischer Git-Abschluss.

`docs/status.md` bleibt der einzige knappe aktuelle Ergebnisbericht. Der lokale
Zwischenstand unter `.codex/qa/state.json` wird erst beim tatsächlichen Auftrag
angelegt und ist ignoriert. Er hält erfolgreiche Baseline und laufende Runde getrennt;
bei Unterbrechung bleiben erfolgreiche passende Belege erhalten. Der
[Zustandsvertrag](../.agents/skills/grosse-testrunde/references/zustand.md) definiert
Vergleich, Wiederaufnahme und Zählung. Kontingent wird gelegentlich geprüft; bei
Erschöpfung wird gesichert und nach Reset über einen eigenen Folgeauftrag fortgesetzt.
Ein ausdrückliches Stopp hebt diese Fortsetzung auf.

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

Die Offlineprüfungen der lokalen Werkzeuge separat vom Projektroot ausführen:

```powershell
Push-Location .opencode/scripts
..\..\.venv\Scripts\python.exe -B -m unittest discover -s tests -v
node --test tests/test_workflow_usage.mjs
Pop-Location
```

Die Dateien unter `.opencode/scripts/prompt/tests` sind gesonderte manuelle
Provider-Smoke-Skripte, keine zusätzliche unittest-Suite. Sie gehören nicht zum
Offline-Prüflauf und können echte Anfragen auslösen.

Gezielte Regressionen verwenden `-Dtest=KlasseA,KlasseB`; in PowerShell das ganze
Argument zitieren. Datenbanktests verwenden ausschließlich eigene In-Memory-Datenbanken.
Frisches Schema und Wiederanlauf desselben Schemas werden separat geprüft.

## Vollständige Browserrunde

```powershell
.\browser-tests\run.ps1 -ReportingTests
.\browser-tests\run.ps1 -Suite all -Workers 2 -Language de
.\browser-tests\run.ps1 -Suite all -Workers 2 -Language en -NoBuild
```

DE baut das optimierte Image einmal; EN verwendet denselben unveränderten Image-Stand.
Der Runner legt pro Worker eigene Datenbank, Mailpit und Ports an und räumt seine
Umgebung wieder ab. Bestehende Entwicklungscontainer werden nicht als Testfixture benutzt.
Keine zweite Testausführung starten, während der eigene Runner noch arbeitet.

Schritte und erwartete Ergebnisse stehen pro Fall in `@Description`, die ID in
`@DisplayName`. Gemeinsame UI-Aktionen gehören in `pages/`, technische Regeln in JUnit.
Neue Fälle nicht als Kopie vorhandener Journeys hinzufügen; konkrete Lücken schließen.

Die [Browseranleitung](../browser-tests/README.md) beschreibt Auswahl, Reports und
Fehlerdiagnose. Lokale Screenshots/Traces unter `browser-tests/runs/` sind temporäre
Diagnosedaten. Wichtige aktuelle Ergebnisse werden knapp im Prüfstand festgehalten.
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
