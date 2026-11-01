# Offene Sonar-Bereinigung

Auftrag: vollständige Bereinigung nach Integration der externen Anmeldung.
Ausgang: Main `6cfea564`, Analyse `691155cb-fae6-409c-a80e-eaf9d79205d6`,
95 offene Issues, keine Hotspots. Bestehende Ausnahmen bleiben erhalten.

## Oberfläche, Bildwerte und Tests

- Aktuelle Zuordnung: Main `31ed947`, Analyse
  `6f8b62ec-7496-4b06-b084-60c82c2a62a6`, noch 57 offene Issues.
- UI-Komplexität, gemeinsame Anbieter-/CSS-Konstanten, serialisierbare Feldhilfe,
  Bildwert-Gleichheit und überflüssige Bedingungen bereinigen.
- Zwei kleine Nachträge aus dem Sicherheitspaket: vorhandene Google-Konstante
  konsequent nutzen und den nativen Entfernen-Vertrag bei leerer OIDC-Anfrage
  ausdrücklich aufrufen. Den Nullfall mit einer Regression absichern.
- Assertions auf fachlich benannte Prüfabschnitte verteilen, vorbereitende
  Testaufrufe außerhalb der Fehler-Assertions ausführen, Regex vereinfachen.
- Gezielte JUnit-/Frontendprüfungen sowie anschließend die ausdrücklich beauftragte
  große Browserrunde DE/EN. Layout, Texte und abgestimmtes Design bleiben erhalten.
- Die konkreten Finding-IDs und Codestellen stammen aus dem vollständigen aktuellen
  lokalen Export `sonar-findings.json`; nach jedem Paket auf Main nachscannen.

Keine Scope-, Regel- oder Gateänderungen. Erledigte Pakete nach belegtem Nachscan
entfernen; verbleibende Arbeit und Testbedarf erhalten.
