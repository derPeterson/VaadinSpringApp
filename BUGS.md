# Offene Fehler

## Doppelt wirkender Hover in Auswahllisten (Geschlecht, Sprache)

- **Status:** Vom Benutzer am 17.11.2026 gemeldet, Branch `design/aura-theme`, Ursache noch
  nicht vollständig behoben.
- **Reproduktion:** Mein Konto → Persönliche Daten → Auswahl „Geschlecht“ oder
  „Bevorzugte Sprache“ öffnen und mit der Maus über einen nicht ausgewählten Eintrag fahren.
- **Ist:** Die Hoverfläche wirkt doppelt, als lägen zwei unterschiedliche Flächen
  übereinander. Bereits umgesetzt: Auras Fokusrahmen und Innenabstand der Einträge
  (`cosmos-reference-option`) sowie Umrissring und Transparenz der Overlays sind
  abgeschaltet; der Hoverzustand selbst ist noch nicht im Browser bestätigt.
- **Erwartet:** Genau eine Hoverfläche wie im Lumo-Stand (`main`).
- **Nächster Schritt:** Hoverzustand im laufenden Browser untersuchen (Host, Part
  `content`, `.cosmos-reference-option-row`, Scroller) und gegen `main` vergleichen.
- **Nachtest:** Hover und Tastaturfokus in beiden Auswahllisten, Desktop und Smartphone.

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

## Zeitauswahl springt nicht mehr zur gewählten Uhrzeit

- **Status:** Vom Benutzer am 17.11.2026 gemeldet, Branch `design/aura-theme`.
- **Reproduktion:** Benutzerverwaltung → Filter → „Letzte Aktivität ab“ → Uhrzeit wählen,
  Auswahl erneut öffnen.
- **Ist:** Die Liste scrollt nicht mehr zum ausgewählten Zeitpunkt.
- **Erwartet:** Beim Öffnen steht der ausgewählte Zeitpunkt sichtbar in der Liste.
- **Nächster Schritt:** `compact-time-input.js`/`CompactTimePicker` prüfen; das Skript
  scrollt gezielt in die Auswahl und greift dafür auf die Komponente zu, deren Aufbau sich
  mit Aura/Vaadin 25 geändert haben kann.
- **Nachtest:** Uhrzeit wählen, schließen, wieder öffnen; auch mit Tastatur.

## Leere Fehlertext-Eigenschaft im Dauerfeld

- **Status:** Bestätigt im Browserlauf DE vom 17.11.2026, Ursache offen.
- **Reproduktion:** Superadmin → Konfiguration → Token-Laufzeiten, Einheit „Sekunden“,
  Wert 30 eingeben, speichern. Die Meldung „Die Dauer muss mindestens 1 min betragen.“
  ist unter dem Feld sichtbar, `vaadin-big-decimal-field.errorMessage` ist jedoch leer.
  Gleiches Bild bei der Queue-Aufbewahrung (Allgemeine E-Mail-Queue-Konfiguration).
- **Erwartet:** Feld ist `invalid` und `errorMessage` enthält den sichtbaren Text, wie es
  TOKEN-01 bis TOKEN-04 und CONFIG-Queue-Aufbewahrung prüfen.
- **Nächster Schritt:** Prüfen, wie `CosmosDurationField.setErrorMessage` den Text an
  die Vaadin-Felder übergibt und ob ein späterer Aufruf die Eigenschaft leert; Vergleich
  mit `main`.
- **Nachtest:** Suites `token-configuration` und `configuration` in DE und EN.

Die bewussten Grenzen der Google-Ersteinrichtung stehen in der
[Sicherheitsdokumentation](docs/externe-anmeldung.md#google-bestätigung-und-geschützte-ersteinrichtung).
Prüfergebnisse stehen im [aktuellen Prüfstand](docs/status.md);
offene automatisierte Ergänzungen im [Testbedarf](docs/testbedarf.md).
