# Offene Fehler

## Leere Fehlertext-Eigenschaft im Dauerfeld (Testanpassung, Nachtest offen)

- **Status:** Ursache am 17.11.2026 geklärt, kein Anwendungsfehler. Seit der Umstellung
  auf native `CustomField`s (`b9f77499`, auf `main` nach der letzten großen Testrunde)
  tragen `vaadin-custom-field.cosmos-duration-custom-field` den Zustand `invalid` und
  die eine sichtbare Meldung; das innere `vaadin-big-decimal-field` ist nur `invalid`.
  Im Browser nachgestellt: Meldung am CustomField, innere `errorMessage` leer.
- **Umgesetzt:** TOKEN-01 bis TOKEN-04 (`TokenConfigurationPage.reject`) und die
  Queue-Aufbewahrung in `ConfigurationBrowserTest` prüfen `errorMessage` jetzt an der
  Feldhülle (`AdminConfigurationPage.durationCustomField`), das innere Feld weiter auf
  `invalid`.
- **Nachtest:** Suites `token-configuration` und `configuration` in DE und EN; danach
  diesen Eintrag entfernen.

Die bewussten Grenzen der Google-Ersteinrichtung stehen in der
[Sicherheitsdokumentation](docs/externe-anmeldung.md#google-bestätigung-und-geschützte-ersteinrichtung).
Prüfergebnisse stehen im [aktuellen Prüfstand](docs/status.md);
offene automatisierte Ergänzungen im [Testbedarf](docs/testbedarf.md).
