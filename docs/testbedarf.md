# Offener Testbedarf

## Browser-Tests

- **Anbietereinstellungen nach Tab-Umbau – OIDC-08:** Bestehenden Fall
  `ExternalLoginBrowserTest.administrativeProviderSettings` auf Desktop/Smartphone
  in DE/EN erneut ausführen. Die Auswahl der Google-/Microsoft-Tabs ist angepasst.
  Erwartung: Aktivieren/Deaktivieren und Speichern wirken nur auf den ausgewählten
  Anbieter; ein leeres Secret erhält das gespeicherte Geheimnis, auch nach Neustart.
  **Wartend:** Der aktuelle Darstellungsauftrag umfasst keine Speicherung.
  Tabwechsel, ungespeicherte Eingaben und HTML-Referenz sind durch OIDC-10 geprüft.

## JUnit-Tests

Kein offener automatisierter Ergänzungsbedarf.

Menschliche Abnahmen bleiben im [manuellen Katalog](manual-tests/README.md).
