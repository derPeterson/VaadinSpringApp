# Offener Testbedarf

## Browser-Tests

- Formularschutz bei Browseraktionen, **wartend auf die Umsetzung** aus dem
  [Komponentenplan](planung/vaadin-komponenten-abgleich.md#ungespeicherte-formularänderungen).
  Persönliche Daten und die Speicherformulare der Konfiguration einschließlich
  Anbieter-/2FA-Einstellungen sowie das Benutzerformular mit ungespeicherten
  Eingaben prüfen. Szenarien sind F5/Strg+R, Browser-Aktualisieren, Tab-/Fenster-
  Schließen und Verlassen über Adresszeile oder externe Navigation.
  Erwartung: Eine Warnung erlaubt das Abbrechen mit erhaltenem Entwurf oder
  bewusstes Verlassen. Unveränderte und erfolgreich gespeicherte Formulare
  warnen nicht. Gezielt in DE/EN bei Desktop- und Smartphonebreite prüfen.

## JUnit-Tests

Aktuell keine offenen Ergänzungen.

Menschliche Abnahmen bleiben im
[manuellen Katalog](manual-tests/README.md).
