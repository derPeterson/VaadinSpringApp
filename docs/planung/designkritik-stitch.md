# Designübernahme: offene Abnahme und Abschluss

## Gültiger Umfang

Die drei Bereiche Besucher, angemeldetes Portal und Administration sind anhand der
[freigegebenen HTML-Referenzen](../design/frontend-referenzen/README.md) in Vaadin
umgesetzt. Dieser Branch bleibt bis zur gemeinsamen Abnahme offen. Es gibt keinen
Auftrag zum Merge oder Push.

Native Vaadin-Komponenten und die vorhandenen Fach-, Eingabe-, Berechtigungs-,
Widerrufs- und Versandregeln bleiben maßgeblich. Selbst gesetzte Icons stammen
aus Lucide. Komponenteninterne Feld-, Checkbox- und Tabellensymbole bleiben bei
Vaadin. Gestaltungsbausteine und Zuständigkeiten stehen im [Theme-Standard](../theme.md).
Die feste Referenz wird nur für ausdrücklich abgestimmte Änderungen mitgezogen.

Besucher, Portal und Administration verwenden denselben Seitenrahmen. Nur der
Serviceheader bleibt beim Scrollen stehen. Seitentitel, Navigation und Inhalt
scrollen zusammen; Copyright folgt im Dokument. Portal und Administration nutzen
dieselbe native Vaadin SideNav einschließlich möglicher Unterpunkte. Besucher
behalten ausschließlich die obere Navigation. Felder, Buttons, Tabellen und Dialoge
behalten ihre abgestimmte Gestaltung.

## Gemeinsame Durchsicht nach der Prüfung

- Angemeldetes Portal und Administration sind vom Benutzer nach manueller
  Durchsicht gestalterisch abgenommen. Die laufenden Tooltip- und
  Bereinigungsnachträge erhalten ihre gezielten Nachprüfungen.
- Besucherbereich ist als gestalterischer Erststand gemeinsam akzeptiert.
  Die Gesamtregression umfasst weiterhin Anmeldung, Registrierung, Passwortlinks,
  Aktivierung, E-Mail-Bestätigung, Wartung und eingeschränkte Anmeldung.
- Echte Mobilgeräte, Bildschirmtastatur, Zoom, Tastaturfokus, Hilfsmittel und
  sprachliche Verständlichkeit gemäß [manuellem Katalog](../manual-tests/README.md)
  menschlich beurteilen. Viewporttests ersetzen diese Abnahme nicht.

## Offene Bereinigung

Benutzermenü getrennt durchsehen: alten Erstaufbau und Besucheraktionen von der
weiterhin benötigten Statusauswahl und Speicherlogik trennen. Die konkrete
Bereinigung gemeinsam abstimmen. Am Benutzermenü noch nichts ändern.

## Offener Abschluss

Nach den freigegebenen Tooltip- und Bereinigungsnachträgen die nächsten Schritte
gemeinsam festlegen. Der Branch bleibt auf Benutzerwunsch offen. Ein Merge nach main, ein aktueller Sonar-Scan mit Behandlung
der Befunde und eine anschließende große Testrunde sind gesondert zu beauftragen.
Die bisherige große Testrunde bleibt im Prüfstand dokumentiert. Gezielte Nachtests
ersetzen sie nicht.

Der Projekt-Skill `stitch-entwurf` ist für künftige gezielte Entwurfsaufträge
vorhanden; Bedienung unter [Arbeiten mit Codex](../codex-skills.md).
Nach bestandener technischer Prüfung und menschlicher Abnahme verbleibende
Branchkorrekturen abschließen. Merge nach main erst auf ausdrücklichen Auftrag.
Dann gültige Entscheidungen in der Fachdokumentation erhalten und diesen erledigten
Plan samt TODO-Verweisen entfernen.
