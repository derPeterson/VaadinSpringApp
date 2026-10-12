# Offene Automatisierung und Paketversuche

## Benutzerverwaltung – Versuch 1 (abgebrochen)

Der Browserlauf `2026-10-12T02-34-27_UTC__5310516d__3b69f418`
scheiterte an mehrdeutigen Locators: Vaadin behält unsichtbare alte Grid-Zellen
und Aktionsbuttons nach dem Filtern. Die sichtbare Tabelle enthielt ein Konto;
die globale DOM-Suche traf zusätzliche verborgene Renderer.
Versuch 2 beginnt erneut mit Bestandsaufnahme und grenzt auf sichtbare Inhalte ein.
Der rote Bericht und seine Screenshots/Traces bleiben unverändert.

### Versuch 2 – Paket bleibt offen

`2026-10-12T02-36-22_UTC__5310516d__f5349832`: sichtbare Zellen/Buttons
wurden eindeutig erkannt, anschließend öffneten vier Fälle den erwarteten
Bearbeitungsdialog nicht; beim Löschen lieferte der Overlay-Locator keinen
gerenderten Kontotext. 0/5 bestätigt. Kein Nachweis eines Produktionsfehlers.
Die fehlgeschlagene Suite und Helfererweiterung wurden auf den sauberen
Paket-Ausgangsstand zurückgeführt; beide historischen Berichte bleiben erhalten.
Nötig: Vaadin-Grid-Aktionszuordnung und Dialog-Slot-Struktur gezielt untersuchen.
USER-01 bis USER-04 bleiben offen; nur die bisherige Anlagevorbereitung ist belegt.

## Abhängiges Paket: Sitzungsrollen und Kontosperren

SEC-02/03/04, VER-04 und RESET-06 benötigen verlässlich bedienbare Adminänderungen
und Löschaktionen. Sie werden nicht auf dem unbestätigten neuen Helfer aufgebaut.
Nach Lösung des Benutzerverwaltungs-Pakets sind sie als eigene UI-Fälle nachzuholen.
