# SonarQube-Anbindung: offene Abnahmen

Branch: `feature/sonarqube-anbindung`, Basis: `66908545a7610c2cd9cc5ad9623a33c7318cd40a`.
Die vollständige Umsetzung des Plans ist freigegeben. Einrichtung und technische
Bedienung stehen in der [Entwicklungsanleitung](../entwicklung.md#sonarqube-und-codex).
Der Featurestand ist lokal nach main integriert; die Umsetzung bleibt ausdrücklich offen.
Dieser Plan bleibt nur für die tatsächlich offenen Schritte bestehen.

## Noch offen

- [ ] Offiziellen MCP-Server mit echtem Werkzeugaufruf im gewünschten Client abnehmen.
  Aktueller Chat: ChatGPT-Projektspiegel mit lokalem Ausführer; projektbezogene
  Codex-Konfiguration allein belegt keine Werkzeugeinbindung in diesen Chat.
  Tatsächliche Auffindbarkeit von AGENTS und allen fünf Skills im Client mitprüfen.
- [ ] Danach erledigten TODO-Eintrag, diese Datei und Planverweise entfernen;
  keinen Abschlussbericht archivieren. Der separate HTTPS-Plan bleibt bestehen.

## Bereits verfügbarer technischer Stand

SonarQube Community `26.10.0.133078` läuft lokal; der Netzwerkweg aus dem festgelegten
MCP-Container über `host.docker.internal:9000` liefert `UP`. Offizielles MCP-Image
`1.29.0.4488` mit festem Digest ist bereitgestellt, Maven-Scanner `5.8.0.7211` festgelegt.
Lokaler Zugang ist benutzergebunden verschlüsselt hinterlegt. Authentifizierter API-
Abruf und echte Lese-Werkzeugaufrufe am offiziellen MCP-Server sind erfolgreich;
33 Ausnahmen wurden über zwei MCP-Seiten vollständig mit eindeutigen IDs gelesen.
Der frische main-Scan ist hochgeladen und serverseitig mit SUCCESS abgeschlossen.
Revision `20d1775e418b97a7d5781b4eb9c0d356b555bb94` stimmt mit dem geprüften Quellstand
überein; Analyse-ID: `e2ab4f1c-b681-445c-83b4-a473e957bdaf`.
Ergebnis: 0 offene Issues, 26 Accepted, 7 False Positives, 0 Hotspots; Quality Gate OK.
Issue-Status wurden nicht verändert. Der aktuelle lokale Scanbericht liegt unter
`.codex/sonar/bericht.md`; keine Berichtserie.

36 Offline-Regressionen und Ruff sind geprüft. Auf main sind Stilprüfungen,
optimierter Build, 2.222 JUnit-Anwendungstests ohne Fehler/Skips und isolierter
HTTP-Start mit 200 erfolgreich. Für den anschließenden Scan wurde dieser unveränderte
Build wiederverwendet. Die native Werkzeugverfügbarkeit im gewünschten Client
und die dortige Skill-Abnahme bleiben offen.
