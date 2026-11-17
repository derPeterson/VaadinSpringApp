# Offene Fehler

## Grid- und TextArea-Scrollleisten seit der Aura-Umstellung

- **Status:** Bestätigt im Browserlauf DE vom 17.11.2026 (COMPONENT-06), Branch
  `design/aura-theme`. Nicht behoben, weil Scrollgestaltung auf Benutzerwunsch
  zurückgestellt ist (siehe nächster Eintrag).
- **Ist:** Das innere Scrollelement des Grids (`#table` im Shadow DOM) zeigt die native
  Browser-Scrollleiste statt der Cosmos-Gestaltung; die Cosmos-Regeln wurden früher über
  den privaten Lumo-Einschub (`--_lumo-vaadin-grid-inject-modules`) eingebunden, den es
  unter Aura nicht mehr gibt, und `#table` besitzt keinen öffentlichen Part. Eine
  TextArea mit fester Höhe scrollt nicht mehr im Part `input-field`.
- **Erwartet:** Dokument, Chat, Grid, TextArea und Dialog teilen die Cosmos-Scrollleiste.
- **Nächster Schritt:** Mit dem Benutzer entscheiden: native Grid-Scrollleiste akzeptieren
  oder einen unterstützten Weg (öffentliche Vaadin-API) wählen; danach COMPONENT-06
  anpassen bzw. erneut prüfen.
- **Nachtest:** `.rowser-testsun.ps1 -Suite cosmos-scrollbars` in DE und EN.

## Leere Fehlertext-Eigenschaft im Dauerfeld

- **Status:** Bestätigt im Browserlauf DE vom 17.11.2026, Ursache offen.
- **Reproduktion:** Superadmin → Konfiguration → Token-Laufzeiten, Einheit „Sekunden“,
  Wert 30 eingeben, speichern. Die Meldung „Die Dauer muss mindestens 1 min betragen.“
  ist unter dem Feld sichtbar, `vaadin-big-decimal-field.errorMessage` ist jedoch leer.
  Gleiches Bild bei der Queue-Aufbewahrung (Allgemeine E-Mail-Queue-Konfiguration).
- **Erwartet:** Feld ist `invalid` und `errorMessage` enthält den sichtbaren Text, wie es
  TOKEN-01 bis TOKEN-04 und CONFIG-Queue-Aufbewahrung prüfen.
- **Nächster Schritt:** Prüfen, wie `CosmosDurationField.setErrorMessage` den Text an
  die Vaadin-Felder übergibt und ob ein späterer Aufruf die Eigenschaft leert; Vergleich
  mit `main`.
- **Nachtest:** Suites `token-configuration` und `configuration` in DE und EN.

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
