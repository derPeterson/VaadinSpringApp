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
Ergänzungen oder Anpassungen automatisierter Tests. Umsetzungen, Sonar-Pakete,
Reviews und QA tragen dort sinnvolle erkannte Lücken ein, sofern sie diese nicht
im selben Auftrag bereits schließen und prüfen. Keine parallelen Listen in TODO,
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

Die Datei bleibt bei leerer Liste mit einer kurzen Leermeldung bestehen. Keine
Erledigt-Historie anhängen und die Liste nicht blind überschreiben. Der aktuelle
Ergebnisbericht steht in `docs/status.md`, menschliche Sonderprüfungen bleiben im
manuellen Katalog. Der Git-/Codevergleich ergänzt die Liste und erkennt nicht notierte Lücken.

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
