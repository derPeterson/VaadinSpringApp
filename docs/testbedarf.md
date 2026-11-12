# Offener Testbedarf

## Browser-Tests

- Integrierte Einheitenauswahl (`CosmosDurationField`), **offen, weitergehende Browserprüfung auf
  Benutzerwunsch zurückgestellt**: In DE/EN auf Desktop und Smartphone ganze
  Sekunden, genaue Zeitspannen und feste Tageswerte in der Konfiguration prüfen,
  einschließlich 2FA-Inaktivität mit eingeschränkten Einheiten. Zahl, Löschen,
  Plus und Einheit teilen die Feldfläche, Einheit ganz rechts mit Trennlinie.
  Die Trennlinie bleibt kurz und mittig; auch bei fokussierter Einheit erscheint
  nur der gemeinsame Feldrahmen, keine zusätzliche innere Box.
  Overlay liegt außerhalb des Eingabefelds, Text wird nicht abgeschnitten,
  Markierung und Häkchen stehen wie im Cosmos-Standard. Tab, Pfeiltasten, Enter,
  Escape, sichtbarer Tastaturfokus, Disabled-/Fehlerzustände und Sprachwechsel
  prüfen. Einheitenwechsel erhält die Zahl und ändert den Zeitraum; gespeicherte
  Werte, Genauigkeit und Formularschutz bleiben erhalten. Die Umsetzung ist
  vom Benutzer abgenommen; die automatisierte Varianten-/Bedienprüfung bleibt
  unabhängig davon offen. Der gezielte Ausrichtungsnachtest am Feld für gespeicherte
  Anmeldungen ist auf Desktop und
  Smartphone abgedeckt, einschließlich Overlay-Grundgestaltung und vollständiger
  Zeilenhöhe des Einheitenwerts; die übrigen Varianten und Bedienzustände bleiben offen.
  Bestehende Page Objects sind angepasst.

- Details-Schalter ohne Hoverfläche, **offen**: In DE/EN auf Desktop und Smartphone
  2FA-Hilfe/Ersatzcodes, Benutzereditor, Anwesenheitsmenü, Ticketanleitung sowie
  Empfängervorschau und Versandhistorie prüfen. Beim Hover bleibt die Grundfläche
  unverändert; Handzeiger, Pfeilwechsel, Tastaturfokus und Auf-/Zuklappen bleiben
  bedienbar. Auf Touch-Geräten keine zurückbleibende Hoverfläche.

- Administrative Sperre und Selbstdeaktivierung, **offen**: In DE/EN auf Desktop
  und Smartphone den Schalter „Zugang gesperrt“ sowie den standardmäßig eingeklappten
  nativen Anmeldungsbereich prüfen. „Details“ steht wie bei 2FA rechts mit blauem
  Pfeil, ohne zusätzlichen linken Aufklapppfeil. Aufklappen allein löst keinen Änderungsschutz aus.
  Der gelbe Warnhinweis zeigt „Selbst deaktiviert:“ fett mit anschließendem
  Löschtermin in normaler Schrift und korrekt lesbaren Umlauten. Die Trennlinie
  oberhalb erscheint nur mit dem Hinweis. Speichern einer Namensänderung,
  Sperren und Entsperren erhalten die Löschvormerkung. Gesperrte Konten erhalten
  weder Portalzugang noch Selbstreaktivierung, auch mit vorhandenem Link.
  Nach Entsperren bleibt die Selbstdeaktivierung bis zur Benutzerreaktivierung bestehen.
  Die vorhandenen Page Objects sind angepasst; der Browserlauf steht aus.

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

Die Ergebnisse der letzten großen Testrunde und deren Qualitätsgrenzen stehen
im [Prüfstand](status.md). Die hier aufgeführten Nachtests bleiben davon unabhängig
offen, bis sie passend abgedeckt und erfolgreich ausgeführt wurden.

- Segmentierte Benachrichtigungsfilter, **wartend auf die geplante Umstellung**:
  Nach dem Wechsel von `RadioButtonGroup` zu nativen `Tabs` im Posteingang und
  in der Glockenvorschau Filterwirkung, Badge-Zähler, erhaltene Auswahl bei
  Textaktualisierung und korrekten Seitenwechsel prüfen. Vorher-/Nachher-Vergleich
  in DE/EN auf Desktop und Smartphone, einschließlich Tastatur, zugänglichen
  Beschriftungen und Handzeiger über Fläche, Text und Badge.
  [Geplantes Paket](planung/vaadin-komponenten-abgleich.md#segmentierte-benachrichtigungsfilter-vereinheitlichen).

## JUnit-Tests

- Rollen-/Sperrregressionen und Filterrücksetzung, **offen**: Der vollständige
  Testimage-Build zur isolierten Zeitfelduntersuchung scheiterte mit acht
  Assertion-Fehlern und einem Fehler in 2935 ausgeführten Tests (zwei übersprungen).
  `RoleMutationPersistenceTest` (drei Fälle), `VerificationQueueConcurrencyTest`
  (fünf Fälle) und `AdminUserManagementFilterTest.resetWaitsForApplyAndCancellationRestoresTheAppliedDraft`
  einzeln untersuchen. Aktuelle Trennung von administrativer Sperre und Selbstdeaktivierung
  sowie Filtervertrag gegen die Erwartungen prüfen; echte Fehler beheben und
  ausschließlich veraltete Erwartungen fachlich begründet anpassen. Erfolgreiche
  gezielte Nachtests und anschließend regulären vollständigen Build nachweisen.
  Die separate Zeitfeldvorschau ersetzt diesen fehlgeschlagenen Nachweis nicht.

Menschliche Abnahmen bleiben im [manuellen Katalog](manual-tests/README.md).
