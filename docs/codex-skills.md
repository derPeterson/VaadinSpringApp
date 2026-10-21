# Arbeiten mit Codex

Die fünf Projekt-Skills liegen unter `.agents/skills/`. Diese Seite beschreibt ihre
Bedienung; die verlinkten `SKILL.md`-Dateien enthalten die vollständigen Agentenregeln.
Aufträge im tatsächlichen Projektverzeichnis stellen. Ein Beispiel zu besprechen
oder diese Anleitung zu lesen startet keinen Ablauf.

| Ich möchte … | Auftrag | Skill |
| --- | --- | --- |
| vollständig testen und sinnvolle Regressionen ergänzen | „Starte die große Testrunde“ | [grosse-testrunde](../.agents/skills/grosse-testrunde/SKILL.md) |
| Sonar bewerten oder bereinigen | „Sonar analysieren und planen“, „Sonar-Paket X umsetzen“ oder „Sonar komplett bereinigen“ | [sonar-bereinigen](../.agents/skills/sonar-bereinigen/SKILL.md) |
| ein Feature planen und schrittweise umsetzen | „Umsetzung starten: FEATURE“ | [umsetzung-starten](../.agents/skills/umsetzung-starten/SKILL.md) |
| den Zwischenstand prüfen und eine Commit-Message erhalten | „Gib mir bitte eine Commit-Message“ | [aenderungen-pruefen](../.agents/skills/aenderungen-pruefen/SKILL.md) |
| einen fertigen Branch lokal integrieren | „Merge BRANCH nach ZIEL“ | [branch-lokal-mergen](../.agents/skills/branch-lokal-mergen/SKILL.md) |

Normale Folgeantworten gehören zum laufenden Auftrag; der Skillname muss nicht bei
jedem Arbeitsschritt wiederholt werden. Explizite Einschränkungen des Benutzers gelten.
Einrichtung und technische Sonar-Bedienung: [Entwicklungsanleitung](entwicklung.md#sonarqube-und-codex).

## Große Testrunde

Der Gesamtauftrag umfasst vollständige JUnit-/Shared-, Browser-Helfer-, Frontend-
und weitere gepflegte Offlineprüfungen sowie Browser `all` auf DE und EN am selben
aktuellen Produktionsimage in eigenen isolierten Umgebungen. OpenCode wird nicht
mitgetestet. Technische Ausführung: [Testanleitung](tests.md).

Änderungen seit der letzten erfolgreichen vollständigen QA und der zentrale
[Testbedarf](testbedarf.md) bestimmen sinnvolle neue oder anzupassende Regressionen.
Vorhandene passende Tests wiederverwenden. Fehler innerhalb geltender Verträge
beheben; neue Fachfunktionen und grundlegende Umbauten abstimmen. Der ausdrückliche
Gesamtauftrag umfasst die Browserfreigabe, einzelne Änderungen dagegen nicht.

Sonar wird mit passendem Analysestand mitgeprüft. Kleine klare Fehler oder Blocker
dürfen behoben werden; umfangreiche Pakete gehören zu „Sonar bereinigen“.
Keine unbegrenzte Umbau-/Scanschleife bis null während der QA. Testergebnisse,
Findings und Quality Gate getrennt berichten. Keine Sonar-Statusänderungen,
echten Mails oder automatischen Git-Abschlüsse.

Ergebnis ist der ersetzte knappe [Prüfstand](status.md), keine Berichtssammlung.
Die erfolgreiche QA-Baseline und eine laufende Runde bleiben im ignorierten lokalen
`.codex/qa/state.json` getrennt. Details: [Zustandsvertrag](../.agents/skills/grosse-testrunde/references/zustand.md).

## Sonar bereinigen

| Modus | Umfang |
| --- | --- |
| Analysieren und planen | Frischer Scan, vollständige Findings bewerten, priorisierte Arbeitspakete planen; keine Code-, Git- oder Issue-Statusänderungen |
| Paket X umsetzen | Genau ein geplantes Paket auf eigenem Branch mit Regressionen, eigenen lokalen Commits/Merge und main-Nachscan; danach anhalten |
| Komplett bereinigen | Analyse und Planung, geeignete Pakete nacheinander umsetzen, integrieren und nachscannen; Ziel möglichst null berechtigte offene Issues |

Die Ausführungsmodi erlauben begründete Accepted-/False-Positive-Entscheidungen mit
eigenständig verständlichem Kommentar direkt am Befund. Kein Push. Wesentliche neue
Funktionen oder Vertragsänderungen abstimmen; ohne Antwort bleiben sie wartend.
Die benötigte MCP-/Scanner-Anbindung muss verfügbar sein; der Skill installiert sie nicht.
Der vorbereitete Projektscan läuft über `scripts/sonar/sonar.py scan`; Voraussetzungen
und MCP-Bedienung stehen in der [Entwicklungsanleitung](entwicklung.md#sonarqube-und-codex).

Community Build analysiert im Hauptprojekt main. Paket-Branches daher zuerst lokal
testen und integrieren, anschließend main scannen. Kein separates Branch-Prüfprojekt.
Browserläufe benötigen einen eigenen Auftrag; nötige Fälle im Testbedarf erhalten.

Es gibt einen aktuellen lokalen Bericht `.codex/sonar/bericht.md` mit Datum und Modus
und daneben `state.json` für die Wiederaufnahme. Beide sind ignoriert und entstehen
erst beim echten Auftrag. Der Bericht wird im Chat verlinkt und beim nächsten Auftrag
ersetzt, nachdem offene Pakete unter `docs/planung/sonar-pakete.md` und offene Tests
im Testbedarf erhalten wurden. Keine datierten Archive. Fiktive Beispiele bleiben
unter einem festen Namen im temporären Chatordner und werden ebenfalls ersetzt.

## Umsetzung starten

Der Startauftrag legt standardmäßig einen Feature-Branch und einen knappen Plan
unter `docs/planung/<feature>.md` an; noch keine Implementierung. Abweichende
Branch-/Ausgangsstände ausdrücklich nennen. Anschließend den Plan besprechen,
anpassen und vollständig oder in einzelnen Schritten freigeben.

Backend und sinnvolle JUnit-Regressionen umsetzen. Bei UI-Funktionen vorhandene
Vaadin-/Cosmos-Komponenten integrieren und die Oberfläche gemeinsam verfeinern.
Technisch fertig bedeutet noch keine gemeinsame UI-Abnahme. Kleine Folgeänderungen
bleiben Teil desselben Features; ein Zwischencommit beendet es nicht.

Browserbedarf zentral festhalten; weder Browser- noch Gesamtrunden automatisch starten.
Größere Zusatzentscheidungen abstimmen. Bei fachlichem Abschluss gültige Dokumentation
pflegen, Provisorien und den erledigten Plan samt Verweisen entfernen; offene Aufgaben
erhalten. „Bitte nach main mergen“ gilt zugleich als Abnahme und Abschlussauftrag.
Commits, Merge und Push benötigen den jeweiligen Auftrag.

## Änderungen prüfen und Commit-Message

Der Auftrag prüft den tatsächlichen uncommittierten Zwischenstand auf Fehler,
Sicherheit, Menschenlesbarkeit, passende Tests und Dokumentation. Kleine zugehörige
Probleme dürfen korrigiert werden; größere Umbauten werden abgestimmt. Explizite
Nur-Lese-/Dateigrenzen beachten und teilweise gestagte Stände klar unterscheiden.

Erfolgreiche unveränderte Prüfungen berücksichtigen und nach Korrekturen gezielt
nachtesten. Keine automatische große QA, Browserrunde oder Sonar-Bereinigung.
Ergebnis nur im Chat: Änderungen, tatsächliche Prüfungen, offene Punkte und eine
kopierbare deutsche Commit-Message. Kein Commit, Merge, Push oder neuer Bericht.

## Branch lokal mergen

Quelle und Ziel müssen aus dem Auftrag eindeutig sein; sie sind nicht zwingend
aktueller Branch und main. Bei uncommittierten Änderungen sofort anhalten, bevor
bereinigt oder gemergt wird. Kein Stashen, Verwerfen oder automatisches Mitcommitten.

Bei sauberem Stand featurebezogene Provisorien, überflüssigen Code und erledigte
Pläne prüfen sowie Dokumentation pflegen. Offene Folge-/Testaufgaben erhalten.
Erzeugt diese Bereinigung Änderungen, mit Commit-Message zur Benutzerprüfung
zurückgeben und vor dem Merge auf Commit und Weiterauftrag warten.

Den sauberen Stand lokal integrieren; Quellbranch und Commit-Historie behalten.
Kein Push oder automatisches Löschen. Überschaubare Konflikte lösen, unklare
Fachentscheidungen abstimmen. Danach auf dem Ziel optimiert bauen, vollständige
JUnit-Anwendungstests und einen eigenen isolierten Start prüfen. Keine automatische
Browserrunde oder große QA; die letzte große QA-Baseline bleibt unverändert.

Ergebnis kurz im Chat, einschließlich offener Fehler und uncommittierter Nachkorrekturen.
Ein Merge nach main schließt das Feature erst nach erfolgreicher Integration und
Nachprüfung ab. Ein anderer Zielbranch bedeutet das nicht automatisch. Ein vom
Benutzer selbst ausgeführter Merge startet keinen Agentenablauf.

## Kontingent und Fortsetzung

Große Testrunde und Sonar-Bereinigung prüfen das Kontingent beim Start, zwischen
größeren Abschnitten und gelegentlich im längeren Lauf. Bei Erschöpfung sichern und
anhalten; kein bewusster längerer Betrieb auf Credits. Nach dem erforderlichen Reset
über einen eigenen Folgeauftrag nur unvollständige Arbeit fortsetzen, vorhandene
Prüfungen und Prozesse berücksichtigen. Ein ausdrückliches Stopp hebt die Fortsetzung
auf. Kein Kauf von Credits, Einlösen eines Resets oder Wechsel zur API-Abrechnung.
