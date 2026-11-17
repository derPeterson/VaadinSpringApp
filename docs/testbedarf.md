# Offener Testbedarf

## Browser-Tests

- **Queue-Gesamtbereinigung instabil – offen:** `MailQueueResolutionBrowserTest.
  fullCleanupWaitsForTransportAndAggregatesMoreThanOneBatch` scheiterte am 17.11.2026 in DE
  zweimal an einer bereits wieder entfernten Meldungskarte („Element is not attached to the
  DOM“ bzw. nicht gefunden), bestand aber in EN am selben Image. Wartebedingung auf die
  Meldung prüfen, ohne Timeouts pauschal zu erhöhen; danach DE/EN erneut ausführen.

- **Aura-Nacharbeiten an Buttons und Auswahllisten – wartend:** Auf Benutzerwunsch erst in
  der gemeinsamen Browserrunde am Schluss prüfen. Szenarien: Icon-only-Buttons (u. a.
  Schließen im Benachrichtigungs-Popover, Tabellenaktionen, Pagination, Inbox-Werkzeuge)
  sind horizontal mittig (Abweichung 0 px); Overlays zeigen keinen zusätzlichen Umrissring;
  Status-/Rollen-Chips im Benutzerfilter zeigen ihren Text senkrecht mittig; Secondary-Buttons mit Rahmen wie
  „Zurück“ zeigen weißen Text und blaues Icon; Textlinks in `.two-factor-help-row` zeigen
  beim Überfahren nur hellere Schrift ohne Fläche; Cosmos-Auswahllisten
  (`cosmos-reference-option`, etwa Geschlecht und Sprache im Konto) haben bei Fokus/Hover
  genau eine Fläche ohne Randstreifen, der Eintrag selbst bleibt transparent; Feldfehler
  (Geburtsdatum, Benutzerfilter, Dauerfelder der Konfiguration) erscheinen in 13 px ohne
  Warnsymbol, auch bei 390 px;
  die Seitengrößenauswahl zeigt zweistellige Werte vollständig. Passende bestehende Fälle
  in `cosmos-components` bzw. den Pagination-/2FA-Suites erweitern, DE und EN.

- **Datumsauswahl im Cosmos-Stil – wartend:** Auf Benutzerwunsch in der gemeinsamen
  Browserrunde am Schluss. Geburtsdatum und Benutzerfilter: Monats- und Jahresbereich sowie
  Werkzeugleiste ohne eigene Fläche, Tage 36 px, „heute“ in `--cosmos-primary-text-color`
  ohne Kreis, ausgewählter Tag in `--cosmos-primary-color` mit weißer Schrift, Hover mit
  `--cosmos-hover-background`, Jahreszahlen 11 px, „Heute“/„Abbrechen“ ohne Füllung; Monat
  wechseln und Tag wählen; Desktop und 390 px (Ansicht von unten), DE und EN.

- **Einheitlicher Deaktiviert-Zustand – wartend:** Auf Benutzerwunsch in der gemeinsamen
  Browserrunde am Schluss. Benutzerdialog „Passwort bestätigen“ ohne Passwort, Zeitauswahl
  im Benutzerfilter ohne Datum und eine Testanordnung aller Feldtypen: Label mit halber
  Deckkraft, Wert und Platzhalter in `--cosmos-text-disabled-color`; Checkbox, Schalter und
  Buttons gedämpft. Passenden Fall in `cosmos-components` ergänzen, DE und EN.

- **Zeitauswahl zeigt die gewählte Uhrzeit – wartend:** Auf Benutzerwunsch in der
  gemeinsamen Browserrunde am Schluss. Benutzerfilter „Letzte Aktivität ab“: Datum setzen,
  Uhrzeit 07:30 wählen, Liste über das Uhrsymbol erneut öffnen; 07:30 ist sichtbar und
  markiert. Bei 07:37 zeigt die Liste 07:30, Schließen ohne Auswahl lässt 07:37 stehen.
  Leeres Feld zeigt weiterhin die Platzhalterzeit. Passenden Fall der Benutzerverwaltung
  erweitern, DE und EN, Desktop und Smartphone.

- **Einheitlicher Hintergrund – wartend:** Auf Benutzerwunsch in der gemeinsamen
  Browserrunde am Schluss. `BackgroundVariantsBrowserTest` und `LoginLayoutBrowserTest`
  sind auf den einzigen Hintergrund (`cosmos-background.css`) umgestellt; zusätzlich bei
  1440/1280/1100/390 px prüfen, dass Mond und Funkelsterne weder Seitenüberschrift noch
  Seitenbeschreibung überdecken. Neu: dritte Sternschnuppe, Wanderstern (`cosmos-drift`,
  rechts nach links) und Supernova (`cosmos-nova`, am rechten Rand, bei 390 px
  ausgeblendet) vorhanden; bei reduzierter Bewegung Wanderstern ausgeblendet und Supernova
  ohne Animation.

## JUnit-Tests

Kein gesonderter offener JUnit-Testbedarf.

Ergebnisse und Grenzen stehen im [aktuellen Prüfstand](status.md).
Menschliche Sonderprüfungen und die festgehaltene Abnahme der Karten,
Bestätigungen und des Queue-Assistenten stehen im
[manuellen Katalog](manual-tests/README.md#hum-01).
Zurückgestellte Folgepakete stehen im
[Komponentenplan](planung/vaadin-komponenten-abgleich.md).
