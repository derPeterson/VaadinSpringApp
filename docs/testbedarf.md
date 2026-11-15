# Offener Testbedarf

## Browser-Tests

- **Native Überlaufnavigation aller Reiterleisten – offen:** Umsetzung und
  gezielter isolierter Browsernachtest sind im Nachbearbeitungsauftrag freigegeben. Nach der Korrektur
  alle betroffenen Reiterleisten einschließlich Kontosicherheit und
  2FA-Rollenkonfiguration in DE/EN auf Smartphone und Desktop mit und ohne
  Überlauf prüfen: lesbare Beschriftungen, erreichbare Reiter über native
  Pfeilnavigation, Touch und Tastatur, korrekte Auswahl und zugehöriger Inhalt.
  Die bestehende Darstellung bei ausreichendem Platz und die unterschiedlichen
  freigegebenen Reiterstile erhalten. Keine große Testrunde; siehe
  [Fehlerdetails](../BUGS.md#sicherheitsreiter-auf-dem-smartphone).

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
