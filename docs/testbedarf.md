# Offener Testbedarf

## Browser-Tests

- **Vollständige Browserrunde DE/EN nach der Aura-Umstellung – offen:** Die Läufe vom
  17.11.2026 erreichten das 90-Minuten-Budget je Worker, bevor alle Klassen liefen
  (zuletzt drei Worker). Vollständigen Lauf mit vier Workern bzw. aufgeteilten Suites
  wiederholen; offene Fehler siehe [BUGS](../BUGS.md#grid--und-textarea-scrollleisten-seit-der-aura-umstellung).
- **Scrollbereiche nach Regression prüfen – wartend:** Betroffene Ansicht,
  konkretes Fehlerbild und Reproduktionsschritte sind noch zu klären.
  Der Benutzer möchte Aussehen und Verhalten später gemeinsam festlegen;
  keine Scrollgestaltung in der Nachtarbeit ändern. Normale Erreichbarkeit wurde
  geprüft; das ersetzt den gezielten Nachtest nicht.
  Nach der Behebung die betroffenen Scrollpanes gezielt prüfen: Inhalte und
  Aktionen bleiben vollständig erreichbar, Scrollen funktioniert wie vorgesehen.
  Passende bestehende Browserfälle wiederverwenden beziehungsweise ergänzen.
  Ein Browserlauf benötigt eine Freigabe für den Behebungsauftrag.
  Fehlerdetails: [Scrollbereiche erneut fehlerhaft](../BUGS.md#scrollbereiche-erneut-fehlerhaft).

## JUnit-Tests

Kein gesonderter offener JUnit-Testbedarf.

Ergebnisse und Grenzen stehen im [aktuellen Prüfstand](status.md).
Menschliche Sonderprüfungen und die festgehaltene Abnahme der Karten,
Bestätigungen und des Queue-Assistenten stehen im
[manuellen Katalog](manual-tests/README.md#hum-01).
Zurückgestellte Folgepakete stehen im
[Komponentenplan](planung/vaadin-komponenten-abgleich.md).
