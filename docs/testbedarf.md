# Offener Testbedarf

## Browser-Tests

Aktuell keine offenen Ergänzungen.

## JUnit-Tests

- **IntelliJ-HTTPS-Start mit Caddy 2.11.9** – Die vorhandenen Startfälle in
  `IdeHttpsProxyTest` mit `https.test.caddy` für Produktions- und Entwicklungsmodus
  erneut ausführen. Erwartet sind die erreichbare HTTPS-Anmeldeseite, die korrekte
  Auslieferung des Stylesheets und das Ende der eigenen Prozesse beim Stoppen.
  Wartet auf freie Ports 8080, 8081 und 8443.

Menschliche Abnahmen bleiben im
[manuellen Katalog](manual-tests/README.md).
