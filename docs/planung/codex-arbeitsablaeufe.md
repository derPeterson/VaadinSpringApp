# Codex-Arbeitsabläufe und SonarQube

Status: teilweise umgesetzt. „Große Testrunde“, „Sonar bereinigen“, „Umsetzung starten“ und „Änderungen prüfen“ sind angelegt
und strukturell/trocken geprüft. Keine echte Gesamtrunde oder Sonar-Ausführung wurde
dabei gestartet. Bedienung: `docs/tests.md` und `docs/entwicklung.md`.
Offen sind „Branch lokal mergen“, die SonarQube-Anbindung und die praktische Abnahme
im verwendeten Client. Der Plan enthält nur verbleibende Arbeiten und wird nach Abschluss gelöscht.

## Vereinbarter Rahmen

- Codex ist das primäre Werkzeug für die gemeinsame Entwicklungsarbeit.
- Zuerst diese Arbeitsabläufe und die SonarQube-Anbindung einrichten, danach HTTPS.
- OpenCode vollständig unverändert am bisherigen Ort belassen; nichts verschieben oder entfernen.
- Codex unabhängig nach den heutigen Anforderungen aufbauen, ohne OpenCode-Abläufe zu übernehmen.
- Den bereits funktionierenden Fernzugriff weiter nutzen. Paseo ist zurückgestellt.
- Kleine, verständliche Projekt-Skills statt eines neuen umfangreichen Workflow-Frameworks.
- Die Anwendung bleibt ein lokales Entwicklungsprojekt mit austauschbaren Testdaten.

Diese Entscheidungen gelten, bis sie bewusst geändert werden. Konkrete Pfade und
Werkzeugparameter innerhalb dieses Rahmens werden bei der Umsetzung festgelegt.

## 1. Projektbezug und Anforderungen klären

- [ ] Prüfen, dass Codex im tatsächlichen Repository arbeitet und dessen `AGENTS.md`
  sowie Projekt-Skills lädt. Ein ChatGPT-Projektspiegel ist keine zweite Codequelle.
- [ ] Die fünf vereinbarten Aufträge aus den heutigen Anforderungen und aktuellen
  Projektregeln ausarbeiten. OpenCode ist keine Vorlage für den neuen Ablauf.
- [ ] OpenCode einschließlich Befehlen, Helfern, Tests, Abhängigkeiten und Konfiguration
  unverändert belassen. Für dieses Paket ist kein Umzug und keine Bereinigung vorgesehen.
- [ ] Nur wenn bei einer konkreten Aufgabe eine passende vorhandene Lösung auffällt,
  gezielt den relevanten OpenCode-Code prüfen und geeignete Teile übernehmen.
  Keine vorsorgliche Portierung oder vollständige OpenCode-Bestandsaufnahme einplanen;
  eine Übernahme darf den bestehenden OpenCode-Ablauf nicht verändern und ist zu testen.

## 2. Native Codex-Struktur einrichten

- [ ] Dauerhafte Projektregeln bleiben zentral in `AGENTS.md`; Skills verweisen darauf,
  statt Stil-, Sicherheits- und Dokumentationsregeln mehrfach zu kopieren.
- [ ] Wiederholbare Abläufe als Projekt-Skills unter `.agents/skills/` beschreiben.
  Jeder Skill bekommt einen klaren Auslöser, Umfang, Voraussetzungen und Abschluss.
- [ ] Erforderliche projektbezogene Einstellungen/MCP-Verbindungen unter `.codex/`
  einrichten. Tokens und andere maschinenbezogene Geheimnisse bleiben lokal.
- [ ] Im tatsächlich verwendeten Codex-Client prüfen, dass die Skills gefunden werden
  und auf dem Desktop sowie über den vorhandenen Fernzugriff aufrufbar sind.

## 3. Letzten wiederholbaren Auftrag einrichten

| Auftrag | Verbindlicher Inhalt |
| --- | --- |
| Branch lokal mergen | Ausdrücklich genannten Quell-/Zielbranch, Arbeitsstand, Tests und Konflikte prüfen; lokalen Merge durchführen; Ergebnis berichten |

- [ ] Bei Sonar-Paketen und Umsetzungen notwendigen, noch nicht ausgeführten
  automatisierten Testbedarf in `docs/testbedarf.md` übergeben: Funktion, Szenario, Erwartung und Lücke
  der bestehenden Abdeckung. Im Abschlussbericht/Chat darauf hinweisen. Die Aufgabe
  bleibt bis zur erfolgreichen Abnahme bestehen, auch wenn Paketplan oder Bericht
  ersetzt/gelöscht werden; die große Testrunde übernimmt sie ausdrücklich.
- [ ] Die bekannte Beauftragung einer großen Testrunde muss ihren Browserumfang klar
  einschließen. Ein ausdrücklich aufgerufener vollständiger Auftrag gilt für diese Runde;
  automatische Skillwahl allein erteilt keine Browserfreigabe. Bereits vorliegende
  Freigaben nutzen, keine erneuten Rückfragen für denselben genehmigten Umfang.
- [ ] Kein automatischer Commit, Merge, Push, echter Mailversand oder Sonar-Statuswechsel
  allein durch einen Prüf- oder Umsetzungsauftrag. Ein ausdrücklich angeforderter Merge
  ist ein eigener Auftrag.
- [ ] Unterbrechungen so festhalten, dass Branch, geänderte Quellen, fertige Nachweise,
  laufende Tests und verwendetes Image eindeutig sind. Unveränderte erfolgreiche Prüfungen
  berücksichtigen; keine doppelten Läufe oder Image-Neubauten während Browserrunden.
- [ ] Ergebnisse im aktuellen Prüfstand pflegen. Keine neue Sammlung alter Runberichte
  oder zusätzliche Coverageauswertung ohne entsprechenden Auftrag aufbauen.

Die große Testrunde enthält Sonar-Kontrolle und begrenzte kleine Korrekturen.
Größere Bereinigungspakete sind ein eigener Auftrag und keine Voraussetzung für
einen belegten Testabschluss; der offene Qualitätsstand bleibt im Bericht sichtbar.

## 4. SonarQube direkt anbinden

- [ ] Installierte SonarQube-Version und passende offizielle MCP-Version abgleichen;
  Verbindung vom tatsächlichen MCP-Ausführungsort zum lokalen Sonar-Server prüfen.
- [ ] Authentifizierung mit lokal hinterlegtem Zugang einrichten, ohne Tokens im Chat,
  Repository, Befehlsverlauf oder Bericht auszugeben. Zunächst die benötigten lesenden
  Werkzeuge aktivieren; Änderungen an Kommentaren/Status nur im ausdrücklich beauftragten Umfang.
- [ ] Projekt, Branch, Scanzeitpunkt und analysierten Quellstand zuordnen. Veraltete oder
  unvollständige Analysen, fehlende Rechte und Verbindungsfehler sichtbar melden.
- [ ] Findings mit IDs/Regeln/Codestellen sowie Quality Gate und vorhandene Coverage
  direkt abrufen. JSON-Export ist keine Voraussetzung des normalen Arbeitsablaufs mehr.
- [ ] Den vollständigen Projektscan als eigenen Build-/Analyseschritt anbinden.
  Auf dessen tatsächlichen Serverabschluss warten; erst dann Findings und Gate bewerten.
  Bloßes Abrufen vorhandener Ergebnisse ist kein neuer Scan.

## Abnahme

- [ ] Alle fünf Aufträge sind im verwendeten Client auffindbar, knapp dokumentiert und
  anhand eines überschaubaren Beispiels überprüft. Prüfung und tatsächliche Git-Aktion
  sind eindeutig unterschieden.
- [ ] Sonar-Daten stammen nachweislich vom richtigen Projekt/Analysestand; fehlende
  Verbindung wird nicht als „keine Findings“ gewertet. Keine unbeauftragten Statusänderungen.
- [ ] Codex funktioniert unabhängig vom OpenCode-Workflow; dessen Dateien und Abläufe
  sind unverändert. Falls gezielt Code übernommen wurde, ist diese Übernahme getestet.
  Java-/Pythonänderungen erfüllen die jeweiligen Stilregeln.
- [ ] Unnötige zweite Implementierungen, kaputte Pfade und doppelte Anleitungen fehlen.

## Pflichtabschluss: Plan entfernen

- [ ] Tatsächliche Bedienung, Pfade und Voraussetzungen in den bestehenden Dokumenten
  `docs/entwicklung.md` und `docs/tests.md` beschreiben; Ergebnisse nach `docs/status.md`.
- [ ] Erledigten TODO-Eintrag und den Verweis im Dokumentationseinstieg entfernen.
- [ ] **Diese Plandatei im selben Arbeitsstand löschen. Nicht als Abschlussbericht
  behalten und nicht in ein Planarchiv verschieben.** Verweise prüfen; einen danach
  leeren Ordner `docs/planung/` entfernen. HTTPS bleibt als eigener offener Auftrag bestehen.

Referenzen: [Projekt-Skills](https://learn.chatgpt.com/docs/build-skills),
[Codex-MCP-Konfiguration](https://learn.chatgpt.com/docs/extend/mcp?surface=cli),
[offizieller SonarQube-MCP-Server](https://docs.sonarsource.com/sonarqube-mcp-server/about-the-mcp-server).
