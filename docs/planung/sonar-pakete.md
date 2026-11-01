# Offene Sonar-Nachkorrekturen

Auftrag: vollständige Bereinigung nach Integration der externen Anmeldung.
Aktuell: Main `1b05f96`, Analyse `a3ca1726-f439-4f20-b158-f796aaf5e28c`.
Sechs Wartbarkeitshinweise sind noch offen. Bugs, Vulnerabilities und Hotspots: null.

## Abschließendes lokales Paket

- `29a59d84-b5ee-4acd-b58b-bec5865bf0df`,
  `65d8261d-4554-4f0f-8607-5eb4402e5d97` (S8786): URL-Prüfungen ohne
  mehrdeutige Regex, mit derselben Warte- und Zielseitenprüfung.
- `21f169f2-01fa-48f3-8cb5-7cb67e4b02c3`,
  `033bf0a6-c341-4a82-87e5-26073e2cab5f` (S3398): Feldhilfe vollständig
  dem zugehörigen Anbieterformular zuordnen.
- `36c062d2-4c4a-4131-91b2-83babf894824` (S3776): Cookie-Vorauswahl
  aus dem Login-Konstruktor als verständliche Aufgabe gliedern.
- `da207394-8b38-4947-9056-26f91bb37d2f` (S7467): unbenutzte
  Fehlerbindung im Konkurrenztest entfernen.

Geprüfte Verträge, Styling und Sicherheitsgrenzen bleiben erhalten. Vorhandene
Login-/Anbieter-/Navigations- und Persistenztests verwenden. Danach vollständiger
Main-Nachscan und die bereits beauftragte große Browserrunde DE/EN. Kleiner
Gliederungsumfang ohne neue Architektur.

Keine Scope-, Regel- oder Gateänderungen. Die drei begründeten Framework-Fehlalarme
sind direkt am Finding dokumentiert. Akzeptierte Google-Zwischenlösung und offene
Stitch-Entscheidung bleiben unverändert. Nach belegtem Abschluss diesen Plan entfernen.
