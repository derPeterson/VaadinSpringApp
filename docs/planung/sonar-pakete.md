# Offene Sonar-Bereinigung

Auftrag: vollständige Bereinigung nach Integration der externen Anmeldung.
Ausgang: Main `6cfea564`, Analyse `691155cb-fae6-409c-a80e-eaf9d79205d6`,
95 offene Issues, keine Hotspots. Bestehende Ausnahmen bleiben erhalten.

## Anmeldesicherheit und Sitzungsverträge

- OIDC-Rücksprung gliedern, fehlenden Issuer kontrolliert ablehnen, Sitzungswerte
  serialisierbar machen, Transaktionsgrenzen eindeutig erhalten und unbenutzten
  Sicherheitscode entfernen. Zugehörige Sonar-Regeln S3776, S2441, S2259, S6809,
  S2160, S8433, S4449, S6878, S1186, S1192, S7467 und Testregel S5778.
- Vorhandene Protokoll-, Datenschutz-, Sitzungs- und Persistenztests verwenden.
  Serialisierung und fehlenden Issuer gezielt ergänzen. Keine Änderung des
  akzeptierten Google-Zwischenvertrags oder der späteren 2FA-Planung.
- Spring-spezifische Identitäts- und Fehlerverträge vor Ausnahmen prüfen.
  Risiko: Sicherheitsgrenzen müssen beim Gliedern erhalten bleiben.

## Oberfläche, Bildwerte und Tests

- UI-Komplexität, gemeinsame Anbieter-/CSS-Konstanten, serialisierbare Feldhilfe,
  Bildwert-Gleichheit und überflüssige Bedingungen bereinigen.
- Assertions auf fachlich benannte Prüfabschnitte verteilen, vorbereitende
  Testaufrufe außerhalb der Fehler-Assertions ausführen, Regex vereinfachen.
- Gezielte JUnit-/Frontendprüfungen sowie anschließend die ausdrücklich beauftragte
  große Browserrunde DE/EN. Layout, Texte und abgestimmtes Design bleiben erhalten.
- Die konkreten Finding-IDs und Codestellen stammen aus dem vollständigen aktuellen
  lokalen Export `sonar-findings.json`; nach jedem Paket auf Main nachscannen.

Keine Scope-, Regel- oder Gateänderungen. Erledigte Pakete nach belegtem Nachscan
entfernen; verbleibende Arbeit und Testbedarf erhalten.
