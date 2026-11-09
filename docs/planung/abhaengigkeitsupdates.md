# Abhängigkeitsupdates

Umsetzung auf `codex/abhaengigkeitsupdates`, ausgehend vom lokalen `main`.
Ziel sind aktuelle stabile Versionen für Anwendung, Browser- und Buildwerkzeuge
sowie die lokalen Testdienste. Verwaltete Abhängigkeiten nach Möglichkeit über
die jeweilige Plattform aktualisieren.

- IntelliJ-Start mit dem neuen Caddy bei freien Ports nachprüfen. Das Szenario
  steht im [Testbedarf](../testbedarf.md#junit-tests).
- Technische Ergebnisse gemeinsam abnehmen.

Jackson 3 bleibt auf der von Spring Boot und Spring Security unterstützten
3.1-Linie. Frontend-Abhängigkeiten aus der Vaadin-Plattform werden über Vaadin
aufgelöst. Die eingefrorene OpenCode-Umgebung gehört nicht zu diesem Paket.
