# Offener Testbedarf

## Browser-Tests

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
