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

## Weitere Kontoabläufe – Versuch 1 (abgebrochen)

`2026-10-12T02-40-22_UTC__fbc1ba18__7a45978d`: 3/5 bestanden
(Dublette, neutrale Antwort für unverifizierte Adresse, veraltetes Resetformular).
Die Einzelvalidierungen scheiterten an gleichzeitig vorhandenen gleichlautenden
Notifications nach mehreren Versuchen. Versuch 2 beginnt neu und schließt geprüfte
Hinweise über deren echte UI-Buttons, bevor die nächste Variante eingegeben wird.

### Weitere Kontoabläufe – Versuch 2 erfolgreich

DE: `2026-10-12T02-42-02_UTC__fbc1ba18__39d243db`, 5/5 bestanden.
Der zugehörige EN-Lauf wurde ebenfalls mit 5/5 bestätigt; Belege in den Run-Archiven.
Die Paketgrenze wurde eingehalten: keine dritte Reparaturrunde.

## Konfiguration/Mail – nach zwei Versuchen offen

Versuch 1: `2026-10-12T02-47-02_UTC__ea41bc7e__420ff512`, 0/3 bestanden.
Der Wartungs-Boolean wurde in der falschen Gruppe gesucht; ein korrekt übertragener
Unicode-Mailtext enthielt den SMTP-typischen abschließenden Zeilenumbruch.
**Bestätigter Produktionsbefund:** `PT0S` wurde als Reset-Token-Gültigkeit gespeichert,
obwohl VerificationService/PasswordResetService strikt positive Dauern voraussetzen.
AdminConfigurationSection validiert derzeit nur die Syntax einer Dauer.

Versuch 2 wurde schon bei der Vorbereitung der Korrektur abgebrochen:
Das Bearbeitungsskript las die UTF-8-Produktionsdatei irrtümlich als Windows-1252
und scheiterte mit UnicodeDecodeError. Es wurden noch keine Produktionsänderungen
geschrieben und kein weiterer Browserlauf gestartet. Die unbestätigten neuen
Helper-/Suiteänderungen wurden zum erfolgreichen Konto-Paketstand zurückgeführt.
Kein dritter Versuch. Nötig: explizites UTF-8-Lesen, positive Prüfung für die vier
Token-Dauern, korrigierte Gruppenbedienung und SMTP-Zeilenabschlussvergleich;
anschließend DE/EN-Regressionsläufe. CONFIG-01/02 und MAIL-01 bleiben offen.
