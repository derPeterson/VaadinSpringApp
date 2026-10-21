# Codex-Arbeitsabläufe: verbleibende Einrichtung

Alle fünf Projekt-Skills sind angelegt und strukturell/trocken geprüft.
Bedienung: [Entwicklung](../entwicklung.md) und [Tests](../tests.md).
Noch offen sind die direkte SonarQube-Anbindung und die praktische Abnahme im
verwendeten Client. Diese Datei enthält nur diese offenen Arbeiten und wird danach gelöscht.

## 1. SonarQube direkt anbinden

- [ ] Aktuelle SonarQube-Edition/-Version mit der passenden offiziellen MCP-Version
  abgleichen und den tatsächlichen Netzwerkweg vom MCP-Ausführungsort prüfen.
- [ ] Projektbezogene Konfiguration und lokal hinterlegten Zugang einrichten.
  Keine Tokens im Chat, Repository, Befehlsverlauf oder Bericht ausgeben.
  Benötigte Werkzeuge/Berechtigungen prüfen; schreibende Issue-Aktionen nur in den
  dafür ausdrücklich freigegebenen Sonar-Ausführungsmodi verwenden.
- [ ] Projekt, Hauptbranch, Scanzeitpunkt und analysierten Quellstand verlässlich
  zuordnen. Veraltete/unvollständige Analysen, fehlende Rechte und Verbindungsfehler
  sichtbar melden; kein Verbindungsfehler darf als null Findings erscheinen.
- [ ] Findings mit IDs, Regeln, Codestellen, vollständiger Pagination sowie Quality
  Gate und vorhandene Coverage direkt abrufen. JSON-Export nicht als Voraussetzung
  des normalen Arbeitsablaufs beibehalten.
- [ ] Aktuellen Projektscan als separaten Build-/Analyseschritt anbinden und dessen
  tatsächlichen Serverabschluss prüfen. Abruf alter Ergebnisse ist kein neuer Scan.
- [ ] Community-Build-Grenze berücksichtigen: Hauptprojekt analysiert main;
  Paket-Branches dürfen dessen Stand nicht durch vermeintliche Branchscans ersetzen.

## 2. Praktische Abnahme

- [ ] Im tatsächlichen Repository und verwendeten Codex-Client prüfen, dass AGENTS
  und alle fünf Projekt-Skills geladen/auffindbar sind. Ein ChatGPT-Projektspiegel
  ist keine zweite Codequelle. Vorhandenen Fernzugriff dabei berücksichtigen.
- [ ] Sonar-Anbindung mit überschaubarem echten Lese-/Analyseauftrag prüfen und
  Projekt/Quellstand sowie Fehlerfälle nachvollziehbar bestätigen.
- [ ] Weitere reale Skill-Abnahmen nur im tatsächlich beauftragten Umfang durchführen;
  keine ungefragte große QA, Feature-Umsetzung oder echten Merge zur Installation starten.
- [ ] Verbleibende Grenzen knapp nennen; technische Strukturprüfung nicht als
  erfolgreiche vollständige Laufzeitabnahme ausgeben.

## Pflichtabschluss

- [ ] Tatsächliche Bedienung und Konfiguration in bestehenden Fachdokumenten pflegen,
  verbleibende offene Testaufgaben zentral in `docs/testbedarf.md` erhalten.
- [ ] Erledigten TODO-Eintrag und Verweis im Dokumentationseinstieg entfernen.
- [ ] Diese Plandatei im selben Arbeitsstand löschen, kein Abschluss-/Planarchiv
  anlegen. Verweise prüfen und einen danach leeren Planordner entfernen.
  Der separate HTTPS-Plan bleibt bis zu seiner eigenen Umsetzung bestehen.

Referenzen: [Projekt-Skills](https://learn.chatgpt.com/docs/build-skills),
[Codex-MCP-Konfiguration](https://learn.chatgpt.com/docs/extend/mcp?surface=cli),
[offizieller SonarQube-MCP-Server](https://docs.sonarsource.com/sonarqube-mcp-server/about-the-mcp-server).
