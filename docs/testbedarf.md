# Offener Testbedarf

## Browser-Tests

- Reaktivierungs- und Verifikationshinweis, **offen**: In DE/EN auf Desktop und
  Smartphone die gelben Header-Iconboxen prüfen. Die Reaktivierungsseite zeigt
  dieselbe E-Mail-Beschriftung und den gerahmten Badge-Stil wie die Verifikationsseite,
  weiterhin mit dem Status „Konto deaktiviert“. Spam-/Junkordner sind im Warnhinweis
  fett hervorgehoben. Der gekürzte Versandhinweis soll auf Desktop ohne einzelnen
  Wortüberhang passen und auf Smartphone natürlich umbrechen.

- Kontoaktionen nach Stitch-Variante A, **offen**: In DE/EN bei Desktop- und
  Smartphonebreite die beiden Aktionszeilen in den bestehenden Cards untereinander,
  farbigen Tabler-Iconboxen, zugeordneten Buttons und vollständigen Beschreibungen
  prüfen. Cardfläche, Rahmen, Radius und Abstände entsprechen Passwort/E-Mail.
  Alle drei Sicherheitsreiter haben den bestehenden Footerstil mit blauem
  Info-Icon und kompakter Inhaltshöhe ohne gestreckte Leerfläche. Portalzugang
  und Anmeldeanbieter verwenden die neuen DE-/EN-Texte. Der Kontoaktionshinweis
  lautet „Beide Aktionen erfordern eine Bestätigung Ihrer Identität.“ beziehungsweise
  „Both actions require confirmation of your identity.“.
  Header und Tabs bleiben unverändert. Die Deaktivierungsbeschreibung für
  Entwicklerkonten bleibt neutral, ohne technische Kontotypdetails oder Zusage
  eines Reaktivierungslinks. Beide Buttons öffnen weiterhin nur die passende
  Identitätsbestätigung; ohne ausdrückliche Bestätigung keine Kontoänderung.

- Passworthinweise bei Kontoaktionen, **offen**: In den bestehenden
  Kontodeaktivierungs-/Löschfällen in DE/EN bei Desktop-/Smartphonebreite die
  sekundäre Textfarbe des Passworthinweises prüfen; 12 px und Gewicht 400 bleiben
  erhalten. Die OTP-Hinweise behalten ihre bestehende Gestaltung.

- Eingerahmte Aktionslinks in E-Mails, **offen**: Die gemeinsame Linkbox in
  DE/EN auf Desktop und Smartphone isoliert in Mailpit prüfen. Alle acht
  Aktionsvorlagen verwenden Cosmos-Fläche und Rahmen; lange URLs bleiben
  vollständig sichtbar und kopierbar. Hinweistext und Buttonziel bleiben
  unverändert. Mails ohne Aktionslink erhalten keine leere Linkbox.

- Kontotyp im Benutzereditor, **offen**: In DE/EN auf Desktop und Smartphone
  den beschrifteten Kontotyp neben dem E-Mail-Nachweis prüfen. Standardkonto ist
  blau, Entwicklerkonto verwendet die Systemfarbe für Schrift, Rahmen und Fläche.
  Bei schmaler Ansicht sauber untereinander; Sprachwechsel aktualisiert beide
  Kontotypen. Die vorhandene Suite `email-verification` dafür ergänzen.

Die automatisierten Nachtests zu den vereinbarten Nachtpaketen sind abgedeckt
und in beiden Sprachen geprüft. Ergebnisse und verbleibende Qualitätsgrenzen
stehen im [Prüfstand](status.md).

- Segmentierte Benachrichtigungsfilter, **wartend auf die geplante Umstellung**:
  Nach dem Wechsel von `RadioButtonGroup` zu nativen `Tabs` im Posteingang und
  in der Glockenvorschau Filterwirkung, Badge-Zähler, erhaltene Auswahl bei
  Textaktualisierung und korrekten Seitenwechsel prüfen. Vorher-/Nachher-Vergleich
  in DE/EN auf Desktop und Smartphone, einschließlich Tastatur, zugänglichen
  Beschriftungen und Handzeiger über Fläche, Text und Badge.
  [Geplantes Paket](planung/vaadin-komponenten-abgleich.md#segmentierte-benachrichtigungsfilter-vereinheitlichen).

## JUnit-Tests

Keine offenen Ergänzungen aus den vereinbarten Nachtpaketen. Die Fach-,
Persistenz-, Vorlagen- und Rückmeldungstests sind im vollständigen Reactor bestanden.

Menschliche Abnahmen bleiben im [manuellen Katalog](manual-tests/README.md).
