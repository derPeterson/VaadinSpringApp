# Offene Fehler

## Sicherheitsreiter auf dem Smartphone

- **Status:** Vom Benutzer per Screenshot gemeldet. Umsetzung und gezielte
  isolierte Browserprüfungen sind im Nachbearbeitungsauftrag freigegeben;
  vor dem ersten Codepaket wird der bisherige Dokumentationsstand committed.
- **Ist:** Unter „Mein Konto → Sicherheit“ und bei den Rollenreitern der
  2FA-Konfiguration werden die Reiter bei schmaler Smartphonebreite gequetscht,
  Beschriftungen abgeschnitten oder unterschiedlich umgebrochen. Die Ursachen
  sind noch nicht durch Browsermessung bestätigt.
- **Erwartet:** Reiter sind erreichbar und ihre Bezeichnungen lesbar. Bei
  Platzmangel native Pfeil-/Scrollnavigation verwenden, auch bei schmalem Desktop;
  mit ausreichendem Platz die vorhandene Darstellung erhalten.
- **Nächster Schritt:** Anwendungsweit alle Reiterleisten auf native
  Überlaufsteuerung und passende Breitenregeln prüfen; siehe
  [Nachbearbeitung, Punkt 7](docs/planung/vaadin-komponenten-abgleich.md#nachbearbeitungsliste).
- **Nachtest:** DE/EN auf Smartphone und Desktop mit und ohne Überlauf,
  Tastatur-/Touch-/Pfeilnavigation; siehe [Testbedarf](docs/testbedarf.md#browser-tests).

## Scrollbereiche erneut fehlerhaft

- **Status:** Auf ausdrücklichen Benutzerwunsch zurückgestellt. Gewünschtes Verhalten
  und Aussehen werden später gemeinsam geklärt; kein bestätigtes Sicherheitsproblem.
- **Ist:** Die Scrollpanes funktionieren laut Benutzer wieder nicht korrekt.
  Betroffene Ansichten, konkretes Verhalten, Gerät und Browser sind noch zu klären.
- **Erwartet:** Scrollbereiche bleiben bedienbar; Inhalte und Aktionen sind
  vollständig erreichbar.
- **Nächster Schritt:** Konkrete Stelle und Reproduktionsschritte erfassen,
  Ursache beheben und die betroffenen Scrollbereiche gezielt nachprüfen.
- **Nachtest:** Siehe [offenen Browser-Testbedarf](docs/testbedarf.md#browser-tests).

Dieser Punkt blockiert die freigegebene Nachtarbeit auf ausdrücklichen Benutzerwunsch
nicht. Bisherige Abnahmen ersetzen die spätere Klärung und den passenden Nachtest nicht.

Die bewussten Grenzen der Google-Ersteinrichtung stehen in der
[Sicherheitsdokumentation](docs/externe-anmeldung.md#google-bestätigung-und-geschützte-ersteinrichtung).
Prüfergebnisse stehen im [aktuellen Prüfstand](docs/status.md);
offene automatisierte Ergänzungen im [Testbedarf](docs/testbedarf.md).
