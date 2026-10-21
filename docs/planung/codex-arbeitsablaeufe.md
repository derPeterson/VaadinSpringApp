# SonarQube-Anbindung: offene Abnahmen

Branch: `feature/sonarqube-anbindung`, Basis: `66908545a7610c2cd9cc5ad9623a33c7318cd40a`.
Die vollständige Umsetzung des Plans ist freigegeben. Einrichtung und technische
Bedienung stehen in der [Entwicklungsanleitung](../entwicklung.md#sonarqube-und-codex).
Dieser Plan bleibt nur für die tatsächlich offenen Schritte bestehen.

## Noch offen

- [ ] Offiziellen MCP-Server mit echtem Werkzeugaufruf im gewünschten Client abnehmen.
  Aktueller Chat: ChatGPT-Projektspiegel mit lokalem Ausführer; projektbezogene
  Codex-Konfiguration allein belegt keine Werkzeugeinbindung in diesen Chat.
  Tatsächliche Auffindbarkeit von AGENTS und allen fünf Skills im Client mitprüfen.
- [ ] Nach ordnungsgemäßer Integration einen frischen vollständigen main-Scan abnehmen:
  Serverauftrag, Analyse-ID, Revision und Gate müssen zusammenpassen. Kein Feature-
  Branchscan in das Hauptprojekt, kein Merge oder Commit allein zur Einrichtung.
- [ ] Danach erledigten TODO-Eintrag, diese Datei und Planverweise entfernen;
  keinen Abschlussbericht archivieren. Der separate HTTPS-Plan bleibt bestehen.

## Bereits verfügbarer technischer Stand

SonarQube Community `26.10.0.133078` läuft lokal; der Netzwerkweg aus dem festgelegten
MCP-Container über `host.docker.internal:9000` liefert `UP`. Offizielles MCP-Image
`1.29.0.4488` mit festem Digest ist bereitgestellt, Maven-Scanner `5.8.0.7211` festgelegt.
Lokaler Zugang ist benutzergebunden verschlüsselt hinterlegt. Authentifizierter API-
Abruf und echte Lese-Werkzeugaufrufe am offiziellen MCP-Server sind erfolgreich;
33 Ausnahmen wurden über zwei MCP-Seiten vollständig mit eindeutigen IDs gelesen.
Die vorhandene Analyse zeigt 0 offene Issues, 26 Accepted, 7 False Positives und ein
grünes Gate. Ihre Revision `3153a24d9af33508304c379178b854b533942f29` weicht von
lokalem main ab; sie bestätigt nicht den neuen Featurestand.

36 neue Offline-Regressionen, Ruff und der Branchschutz am echten Repository sind
geprüft. Maven Wrapper verwendet JDK 25. Das ersetzt weder native Werkzeugverfügbarkeit
in diesem Chat noch den frischen main-Scan. Anwendung und bestehende Sonar-Befunde
wurden nicht verändert.
