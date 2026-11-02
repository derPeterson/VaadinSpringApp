# Offene Fehler

- **Ersatzcode-PDF und Erhalt der Ausgabe (hoch):** Nach der 2FA-Einrichtung
  schlägt „PDF herunterladen“ fehl und verlässt dabei die Ersatzcode-Seite in
  Richtung Portal. Die einmalige Ausgabe geht dadurch verloren, bevor der Benutzer
  sie sichern konnte. Erwartet: Download ohne Navigation; bei Fehler bleibt die
  Ausgabe verfügbar und erneut herunterladbar. Gezielt mit erfolgreichem sowie
  fehlgeschlagenem PDF-Abruf und anschließendem erneuten Kopieren/Download nachtesten.

Die bewussten Grenzen der Google-Ersteinrichtung stehen in der
[Sicherheitsdokumentation](docs/externe-anmeldung.md#google-bestätigung-und-geschützte-ersteinrichtung).
Offene automatisierte Ergänzungen und menschliche Abnahmen bleiben im
[Testbedarf](docs/testbedarf.md) und im [2FA-Plan](docs/planung/zwei-faktor-bestaetigung.md).
