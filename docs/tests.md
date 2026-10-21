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
