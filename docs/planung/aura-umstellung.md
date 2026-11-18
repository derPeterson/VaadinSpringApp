# Umstellung auf Vaadin Aura mit neu gegliedertem Cosmos-Theme – offene Punkte

Branch: `design/aura-theme`. Auftrag des Benutzers vom 17.11.2026.

Umgesetzt sind Aura als Basis, die eine Palette, die Typografieskala, zusammengeführte
Token-Familien, Prettier/Stylelint, die neu gegliederte Dateistruktur und die Bereinigung
nachweislich überdeckter Anordnungsregeln. Gemeinsame Grundregeln und genutzte Utilities
bleiben erhalten; ein fehlender Kaskadengewinn auf einzelnen Seiten reicht nicht als
Löschgrund. Der gültige Stand steht in der
[Theme-Anleitung](../theme.md#basis-vaadin-aura-mit-cosmos-schichten)
einschließlich akzeptierter Abweichungen; Werkzeuge in der
[Entwicklungsanleitung](../entwicklung.md#css-prüfen-und-formatieren).

Grenzen bleiben: Die Anwendung sieht wie bisher aus; Änderungen, die das Aussehen spürbar
ändern würden, werden vorher mit dem Benutzer abgestimmt. Ein Light-Modus ist nicht Teil
des Auftrags. Commits nur auf ausdrücklichen Auftrag.

## Offene Arbeiten

- [ ] Gemeinsame große Testrunde DE/EN nach Umsetzung der
      [Versionierung](releases.md) durchführen. Offene Fälle im
      [Testbedarf](../testbedarf.md) sowie den Dauerfeld-Nachtest aus
      [BUGS](../../BUGS.md) einschließen. Keine vorgezogene Browserrunde.
- [ ] Bei vollständig grünem Gesamtergebnis ist der Abschluss vom Benutzer
      beauftragt: erfolgreich nachgeprüfte Bug-/Testbedarfseinträge, den Aura-TODO
      und diesen erledigten Plan samt Verweisen entfernen. Bei offenen Fehlern
      oder fehlenden Nachweisen die betroffenen Punkte erhalten. Die separate
      Vaadin-Komponenten-Gesamtumsetzung wird dadurch nicht abgeschlossen.

## Wiedereinstieg

Die Browserrunde verwendet eigene isolierte Testdaten und denselben unveränderten Stand
für DE und EN gemäß [Testanleitung](../tests.md) und
[Browser-Testanleitung](../../browser-tests/README.md). Die normale Entwicklungsanwendung
und ihre Daten bleiben unberührt. Temporäre CSS-Messungen ersetzen diese Runde nicht.
