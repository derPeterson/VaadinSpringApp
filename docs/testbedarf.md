# Offener Testbedarf

## Browser-Tests

- Anmeldungsbereich im Benutzer-Bearbeitungsdialog, **offen, noch kein Browserlauf beauftragt**:
  In DE/EN auf Desktop und Smartphone Informationen mit bestehenden Status-Badges
  oberhalb der Trennlinie prüfen. Darunter stehen Bestätigungsmail und Passwortreset
  mit unveränderten Freigaben sowie ein blaues Info-Icon mit kurzer Erklärung
  beider Aktionen und Hinweis auf die gespeicherte Adresse.
  Buttons und lange Beschriftungen müssen bei Platzmangel sauber umbrechen.
  Die vorhandene Suite `email-verification` für ausstehenden, bestätigten und
  ausdrücklich ungeprüften Nachweis sowie Konten ohne Portalpasswort verwenden.

- Headerlinks, **offen, noch kein Browserlauf beauftragt**:
  In Portal und Administration in DE/EN bei Desktop- und Smartphonebreite prüfen,
  dass der Profilbereich bei Hover transparent bleibt. Menü per Klick und Tastatur
  öffnen, sichtbaren Tastaturfokus erhalten und Menüaktionen weiterhin bedienen.
  Der Logo-Link zeigt bei Hover keinen Unterstrich, behält seine Textfarbe und
  verwendet wie im Besucherbereich den Handzeiger.
  Handzeiger auch über Header-Iconbuttons, Profilavatar, Pagination und den
  Benachrichtigungsfiltern einschließlich ihrer Zähler prüfen. Deaktivierte
  Seitenbuttons und reine Identitätsavatare dürfen keinen Handzeiger erhalten;
  Textfelder behalten den Textzeiger. Native Auswahlaktionen folgen demselben
  zentralen Cursorwert.
  Bei den als Segmente gestalteten Radiobuttons „Alle / Ungelesen / Gelesen“
  jeweils die freie Fläche, die Beschriftung und das Zahlen-Badge prüfen.

- Segmentierte Benachrichtigungsfilter, **wartend auf die geplante Umstellung**:
  Nach dem Wechsel von `RadioButtonGroup` zu nativen `Tabs` im Posteingang und
  in der Glockenvorschau Filterwirkung, Badge-Zähler, erhaltene Auswahl bei
  Textaktualisierung und korrekten Seitenwechsel prüfen. Vorher-/Nachher-Vergleich
  in DE/EN auf Desktop und Smartphone, einschließlich Tastatur, zugänglichen
  Beschriftungen und Handzeiger über Fläche, Text und Badge.
  [Geplantes Paket](planung/vaadin-komponenten-abgleich.md#segmentierte-benachrichtigungsfilter-vereinheitlichen).

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
