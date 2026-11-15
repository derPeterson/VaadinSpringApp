# Offene Fehler

## Scrollbereiche erneut fehlerhaft

- **Status:** Vom Benutzer erneut gemeldet; noch nicht gezielt reproduziert oder behoben.
- **Ist:** Die Scrollpanes funktionieren laut Benutzer wieder nicht korrekt.
  Betroffene Ansichten, konkretes Verhalten, Gerät und Browser sind noch zu klären.
- **Erwartet:** Scrollbereiche bleiben bedienbar; Inhalte und Aktionen sind
  vollständig erreichbar.
- **Nächster Schritt:** Konkrete Stelle und Reproduktionsschritte erfassen,
  Ursache beheben und die betroffenen Scrollbereiche gezielt nachprüfen.
- **Nachtest:** Siehe [offenen Browser-Testbedarf](docs/testbedarf.md#browser-tests).

Dieser Fehler hat vor weiteren Komponentenumbauten Vorrang. Bisherige Abnahmen
ersetzen den nötigen Nachtest dieser Regression nicht.

Die bewussten Grenzen der Google-Ersteinrichtung stehen in der
[Sicherheitsdokumentation](docs/externe-anmeldung.md#google-bestätigung-und-geschützte-ersteinrichtung).
Prüfergebnisse stehen im [aktuellen Prüfstand](docs/status.md);
offene automatisierte Ergänzungen im [Testbedarf](docs/testbedarf.md).
