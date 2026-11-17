# Offene Fehler

## Datumsauswahl noch nicht im Cosmos-Stil

- **Status:** Vom Benutzer am 17.11.2026 gemeldet, Branch `design/aura-theme`.
- **Reproduktion:** Ein Datumsfeld öffnen, z. B. Geburtsdatum unter Persönliche Daten oder
  „Letzte Aktivität ab“ im Benutzerfilter.
- **Ist:** Das Kalender-Overlay (`vaadin-date-picker`) zeigt weitgehend Aura-Standards.
- **Erwartet:** Kalender im Cosmos-Stil wie die übrigen Overlays (Flächen, Rahmen,
  Auswahl- und Hoverfarben aus `cosmos-palette.css`, Schriftskala).
- **Nächster Schritt:** Vergleich mit `main`; Gestaltung über öffentliche Parts und
  `--vaadin-date-picker-*`-Variablen in `cosmos-aura.css` bzw. `cosmos-theme.css`.
- **Nachtest:** Kalender öffnen, Monat wechseln, Tag auswählen, heute markiert, Desktop
  und Smartphone.

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
