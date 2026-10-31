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
  Durchsicht gestalterisch abgenommen. Die Tooltip- und Bereinigungsnachträge
  sind umgesetzt und gezielt nachgeprüft.
- Besucherbereich ist als gestalterischer Erststand gemeinsam akzeptiert.
  Die Gesamtregression umfasst weiterhin Anmeldung, Registrierung, Passwortlinks,
  Aktivierung, E-Mail-Bestätigung, Wartung und eingeschränkte Anmeldung.
- Echte Mobilgeräte, Bildschirmtastatur, Zoom, Tastaturfokus, Hilfsmittel und
  sprachliche Verständlichkeit gemäß [manuellem Katalog](../manual-tests/README.md)
  menschlich beurteilen. Viewporttests ersetzen diese Abnahme nicht.

## Offener Abschluss

Die freigegebenen Tooltip- und Bereinigungsnachträge sind umgesetzt und gezielt
geprüft. Der Branch bleibt bis zur Integration offen. Als Nächstes folgen ein Merge
nach main, ein aktueller Sonar-Scan mit Behandlung der Befunde und eine anschließende
große Testrunde. Diese Schritte sind gesondert zu beauftragen.
Die bisherige große Testrunde bleibt im Prüfstand dokumentiert. Gezielte Nachtests
ersetzen sie nicht.

Der Projekt-Skill `stitch-entwurf` ist für künftige gezielte Entwurfsaufträge
vorhanden; Bedienung unter [Arbeiten mit Codex](../codex-skills.md).
Nach bestandener technischer Prüfung und menschlicher Abnahme verbleibende
Branchkorrekturen abschließen. Merge nach main erst auf ausdrücklichen Auftrag.
Dann gültige Entscheidungen in der Fachdokumentation erhalten und diesen erledigten
Plan samt TODO-Verweisen entfernen.
