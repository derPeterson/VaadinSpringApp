# Offener Testbedarf

Hier stehen ausschließlich offene Aufgaben zur Ergänzung oder Anpassung
automatisierter Tests. Pflege und Abnahme: [Testablauf](tests.md#übergabe-offener-testaufgaben).

## Browser-Tests

Aktuell keine offenen automatisierten Browser-Testaufgaben.

## JUnit-Tests

| Funktion | Szenario und Erwartung | Zustand |
| --- | --- | --- |
| IDE-HTTPS-Fehlerassertions | Fixturevorbereitung außerhalb der geprüften Lambda; erwarteten Start-/Bestätigungsfehler und anschließenden eigenen Prozessabbau gezielt belegen. Bestehende Tests anpassen, keine doppelten Fälle. | Offen nach Sonar-S5783, [Paket 2](planung/sonar-pakete.md#paket-2-aussagekräftige-start--und-konfigurationstests-7-issues); Umsetzung nicht beauftragt. |

Menschliche Abnahmen und die nicht beauftragte optionale externe SMTP-Abnahme
bleiben im [manuellen Katalog](manual-tests/README.md).
