# Codex-Arbeitsabläufe und SonarQube

Status: beschlossen, noch nicht umgesetzt. Dieses Dokument ist der befristete
Arbeitsauftrag; es wird mit Abschluss der Aufgabe gelöscht.

## Vereinbarter Rahmen

- Codex ist das primäre Werkzeug für die gemeinsame Entwicklungsarbeit.
- Zuerst diese Arbeitsabläufe und die SonarQube-Anbindung einrichten, danach HTTPS.
- Den OpenCode-Workflow separat erhalten; benötigte allgemeine Helfer weiterverwenden.
- Den bereits funktionierenden Fernzugriff weiter nutzen. Paseo ist zurückgestellt.
- Kleine, verständliche Projekt-Skills statt eines neuen umfangreichen Workflow-Frameworks.
- Die Anwendung bleibt ein lokales Entwicklungsprojekt mit austauschbaren Testdaten.

Diese Entscheidungen gelten, bis sie bewusst geändert werden. Konkrete Pfade und
Werkzeugparameter innerhalb dieses Rahmens werden bei der Umsetzung festgelegt.

## 1. Projektbezug und Werkzeugbestand ordnen

- [ ] Prüfen, dass Codex im tatsächlichen Repository arbeitet und dessen `AGENTS.md`
  sowie Projekt-Skills lädt. Ein ChatGPT-Projektspiegel ist keine zweite Codequelle.
- [ ] OpenCode-Befehle, Python-/Node-Helfer, Tests, Abhängigkeiten und Verweise erfassen.
  Allgemeine Funktionen wie Branchprüfung, Benchmarks und Auswertungen nach Bedarf
  übernehmen; OpenCode-spezifische Bedienung getrennt halten.
- [ ] Einen eigenen Ablageort außerhalb des Anwendungsprojekts für die vollständigen
  OpenCode-Werkzeugquellen festlegen und ihren Erhalt prüfen, bevor dort etwas entfernt wird.
  Keine Credentials, lokalen Laufdaten oder fremden Änderungen mit umräumen.
- [ ] Weiterverwendete allgemeine Helfer an einer eindeutigen Stelle im Projekt ablegen,
  etwa unter `tools/`. Aufrufer, Tests, Abhängigkeiten und Anleitungen gemeinsam anpassen.
  Keine parallelen Kopien oder dauerhaften Weiterleitungs-/Kompatibilitätswege behalten.

## 2. Native Codex-Struktur einrichten

- [ ] Dauerhafte Projektregeln bleiben zentral in `AGENTS.md`; Skills verweisen darauf,
  statt Stil-, Sicherheits- und Dokumentationsregeln mehrfach zu kopieren.
- [ ] Wiederholbare Abläufe als Projekt-Skills unter `.agents/skills/` beschreiben.
  Jeder Skill bekommt einen klaren Auslöser, Umfang, Voraussetzungen und Abschluss.
- [ ] Erforderliche projektbezogene Einstellungen/MCP-Verbindungen unter `.codex/`
  einrichten. Tokens und andere maschinenbezogene Geheimnisse bleiben lokal.
- [ ] Im tatsächlich verwendeten Codex-Client prüfen, dass die Skills gefunden werden
  und auf dem Desktop sowie über den vorhandenen Fernzugriff aufrufbar sind.

## 3. Vier wiederholbare Aufträge festlegen

| Auftrag | Verbindlicher Inhalt |
| --- | --- |
| Große Testrunde | Git-/Quellstand und laufende Prozesse prüfen; Java 25 und Stilregeln; vollständige JUnit-, Helfer-, Frontend- und Pythonprüfungen; ein optimiertes Image; vollständige Browserrunde DE/EN am identischen Image; Fehlerbehebung, erforderliche Nachtests und ehrlicher Abschluss |
| Sonar prüfen | Analysestand und Codezuordnung prüfen; Findings, Quality Gate und vorhandene Coverage abrufen; fachlich bewerten, passende Korrekturen testen und nach neuem Scan nachprüfen |
| Änderungen prüfen | Diff und Funktion bewerten; passende Nachweise prüfen; offene Punkte nennen; deutsche Commit-Message für die tatsächlich fertigen Änderungen vorschlagen |
| Branch lokal mergen | Ausdrücklich genannten Quell-/Zielbranch, Arbeitsstand, Tests und Konflikte prüfen; lokalen Merge durchführen; Ergebnis berichten |

- [ ] Die bekannte Beauftragung einer großen Testrunde muss ihren Browserumfang klar
  einschließen. Ein ausdrücklich aufgerufener vollständiger Auftrag gilt für diese Runde;
  automatische Skillwahl allein erteilt keine Browserfreigabe. Bereits vorliegende
  Freigaben nutzen, keine erneuten Rückfragen für denselben genehmigten Umfang.
- [ ] Kein automatischer Commit, Merge, Push, echter Mailversand oder Sonar-Statuswechsel
  allein durch einen Prüfauftrag. Ein ausdrücklich angeforderter Merge ist ein eigener Auftrag.
- [ ] Unterbrechungen so festhalten, dass Branch, geänderte Quellen, fertige Nachweise,
  laufende Tests und verwendetes Image eindeutig sind. Unveränderte erfolgreiche Prüfungen
  berücksichtigen; keine doppelten Läufe oder Image-Neubauten während Browserrunden.
- [ ] Ergebnisse im aktuellen Prüfstand pflegen. Keine neue Sammlung alter Runberichte
  oder zusätzliche Coverageauswertung ohne entsprechenden Auftrag aufbauen.

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

- [ ] Alle vier Aufträge sind im verwendeten Client auffindbar, knapp dokumentiert und
  anhand eines überschaubaren Beispiels überprüft. Prüfung und tatsächliche Git-Aktion
  sind eindeutig unterschieden.
- [ ] Sonar-Daten stammen nachweislich vom richtigen Projekt/Analysestand; fehlende
  Verbindung wird nicht als „keine Findings“ gewertet. Keine unbeauftragten Statusänderungen.
- [ ] Übernommene Helfer und angepasste Aufrufer sind getestet; Java-/Pythonänderungen
  erfüllen die jeweiligen Stilregeln. OpenCode-Quellen sind separat vollständig erhalten.
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
