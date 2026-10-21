---
name: aenderungen-pruefen
description: "Aktuelle uncommittierte Änderungen in VaadinSpringApp vor einem Zwischencommit prüfen, überschaubare Fehler und Lesbarkeitsprobleme korrigieren und eine deutsche Commit-Message liefern. Auch bei 'Gib mir eine Commit-Message' verwenden. Ausdrücklich reine Textwünsche, Beispiele und das Erstellen/Prüfen dieses Skills sind kein Änderungsreview."
---

# Aenderungen Pruefen

„Gib mir bitte eine Commit-Message“ beauftragt in diesem Projekt den folgenden
begrenzten Abschlusscheck. Der Benutzer muss keinen zusätzlichen Review-Befehl
verwenden. Ein ausdrücklich gewünschter reiner Textvorschlag oder Nur-Lese-Review
hat Vorrang. Beim Anlegen oder Erklären dieses Skills keine rekursive Prüfung starten.

## 1. Tatsächlichen Zwischenstand prüfen

- `README.md` und `AGENTS.md` beachten. Git-Branch, Arbeitsstand und Umfang prüfen.
  Standard ist der vollständige uncommittierte Änderungsstand: gestagte/ungestagte
  Änderungen sowie relevante neue nicht ignorierte Dateien. Geheimnisse und
  generierte Daten gemäß Projektregeln auslassen, fremde Änderungen erhalten.
- Einen ausdrücklich eingeschränkten Umfang respektieren, etwa nur gestagte
  Änderungen oder bestimmte Dateien. Keine Git-Aktionen ausführen: weder stagen,
  unstagen, committen, Branch wechseln, mergen noch pushen.
- Den Diff und nötige betroffene Aufrufer/Verträge lesen. Bei einer Featurearbeit
  den freigegebenen aktuellen Umfang berücksichtigen. Der Zwischencommit muss
  nicht das gesamte Feature abschließen; aktive Pläne nicht voreilig löschen.
- Wenn nichts geändert ist, das sagen. Keine Änderung oder Commit-Message erfinden.
  Bei mehreren unabhängigen Themen sinnvolle getrennte Commit-Vorschläge nennen,
  ohne selbst Dateien oder Index umzuräumen.

## 2. Bewusster zweiter Blick

- Verhalten und Fehlerfälle hinterfragen: Berechtigungen, Eingabegrenzen,
  Transaktionen, Nebenwirkungen und relevante Sicherheits-/Sitzungsverträge.
  Sonar- oder Vollscans sind nicht automatisch Teil dieses Reviews.
- Menschenlesbarkeit prüfen: sprechende fachliche Namen, erkennbare lokale Typen,
  klare Methodenzuständigkeit, verständliche Kontrollflüsse und überschaubare
  Verschachtelung. Methodennamen müssen ihren tatsächlichen Aktionen entsprechen.
  Bestehende Stilregeln aus `AGENTS.md` anwenden; keine pauschale Umformatierung.
- Kommentare/Javadoc ergänzen, wenn sie wichtige Voraussetzungen, Rückgabewerte,
  Seiteneffekte, Fehlerfälle oder besondere Verträge erklären. Selbsterklärenden
  Code nicht mit formelhaften Kommentaren wiederholen. Keine starren Methoden-
  längen, künstlichen Hilfsmethoden oder allgemeinen Stilumbauten erzwingen.
- Prüfen, ob passende Regressionen existieren und Dokumentation, Übersetzungen,
  Ressourcen und aktive Aufgaben zum tatsächlichen Verhalten passen. Auf den
  Änderungsumfang fokussieren, keine zusätzliche Projektbereinigung eröffnen.

## 3. Kleine Korrekturen und passende Nachweise

- Überschaubare fachlich klare Fehler, wichtige Dokumentationslücken und sinnvolle
  Lesbarkeitsverbesserungen im Umfang direkt beheben. Bei fremden Änderungen nicht
  unbesehen Inhalte überschreiben. Größere Umbauten, neue Funktionen oder grundlegende
  Vertragsentscheidungen gezielt mit dem Benutzer klären.
- Eine unbeantwortete größere Frage bleibt offen; unabhängige Teile können geprüft
  werden. Bekannte wesentliche Probleme vor dem Commit-Vorschlag nennen und den
  Zwischenstand nicht als bedenkenlos fertig ausgeben.
- Bereits erfolgreiche Prüfungen am unveränderten Stand berücksichtigen. Nach
  eigenen Korrekturen vorgeschriebene Formatierungs-/Stilschritte und erforderliche
  gezielte Regressionen ausführen. Keine vollständige QA nach jeder Commit-Anfrage.
- Stand der Testbelege prüfen: getestet wurde der tatsächlich ausgeführte Quellstand,
  nicht automatisch ein nur teilweise gestagter Index. Liegt eine Korrektur noch
  ungestagt oder außerhalb des gewählten Umfangs, dies ausdrücklich sagen; nichts
  heimlich nachstagen oder in der Message als bereits enthalten ausgeben.
- Sinnvolle noch nicht geprüfte automatisierte Lücken nach `docs/testbedarf.md`
  übernehmen. Browserrunden nur bei entsprechender Freigabe; weder sie noch die
  große Testrunde automatisch starten. Keine echten Mails oder Entwicklerdaten verwenden.
- Abschließend den eigenen finalen Diff und `git diff --check` prüfen. Nach einer
  weiteren Rückmeldung den Vorschlag auf die tatsächlich neue Version abstimmen.

## 4. Ergebnis ausschließlich im Chat

- Kurz erklären, was sich fachlich geändert hat, welche zusätzlichen Korrekturen
  dieser Review ergab und welche Prüfungen tatsächlich ausgeführt/vorhanden sind.
  Offene Fragen, Risiken und nicht ausgeführte Tests klar nennen. Ohne Ausführung
  keinen erfolgreichen Test behaupten.
- Zum Schluss eine kopierbare deutsche Commit-Message für den gewählten finalen
  Umfang liefern, gemäß Projektregel in abgeschlossener Form. Keine Pläne oder
  noch nicht enthaltenen Änderungen als fertige Arbeit beschreiben.
- Keinen zusätzlichen Review-Bericht, Plan oder datierte MD-Datei anlegen. Erkannte
  offene Testarbeit bleibt allein in der zentralen Testbedarfsliste.
- Der anschließende Benutzercommit schließt diesen Zwischenstand ab. Ein aktives
  Feature kann danach mit normalen Nachrichten auf demselben Branch weitergehen.
