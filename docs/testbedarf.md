# Offener Testbedarf

## Browser-Tests

- **Queue-Gesamtbereinigung instabil – offen:** `MailQueueResolutionBrowserTest.
  fullCleanupWaitsForTransportAndAggregatesMoreThanOneBatch` scheiterte am 17.11.2026 in DE
  zweimal an einer bereits wieder entfernten Meldungskarte („Element is not attached to the
  DOM“ bzw. nicht gefunden), bestand aber in EN am selben Image. Wartebedingung auf die
  Meldung prüfen, ohne Timeouts pauschal zu erhöhen; danach DE/EN erneut ausführen.
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
