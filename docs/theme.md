# Silent Cosmos: Theme und Gestaltung

## Gestaltungsregeln

Vaadin Flow ist die Komponentenbasis. Das gemeinsame Cosmos-Theme ist der Standard.
Neue Ansichten verwenden vorhandene Bausteine. Lokale Anpassungen verändern keine
anderen Views, Icons oder Maße als Nebenwirkung.

| Element | Standard |
| --- | --- |
| Primary-/Secondary-Button | Lumo M (36 px), 9 px Radius, 13 px / 600 |
| Primary | Blauer Cosmos-Verlauf, Primary-Border, Kontrastschrift |
| Secondary | Input-Hintergrund, Primary-Text, 1 px lumo-contrast-20pct |
| Buttonicons | Vorhandene Größe; die 14-px-Basis für em-Icons erhalten |
| Tabellenaktionen | 32×32 px, Icons 16×16 px |
| Pagination | 28×28 px, 11 px Schrift, 4 px Abstand; aktiv Primary |
| Eingaben | Lumo M, 13 px / 700; Platzhalter/leere Eingaben 13 px / 400 |
| Labels | 12 px / 450, Secondary |
| Paneltitel/-beschreibung | Gemeinsame cosmos-panel-title / cosmos-panel-description |
| Schließen | cosmos-overlay-close und bestehendes CLOSE_CIRCLE |

Formularpaare nutzen `cosmos-form-row` und stehen unter 600 px untereinander.
Die Löschaktion verwendet die vereinbarte 50-%-Mischung der Error- und Basisfarbe;
weitere rote Komponentenfarben werden nicht parallel eingeführt.

Alle Cosmos-Grids verwenden dieselben vier zentralen Variablen:

| Token | Wert |
| --- | --- |
| --cosmos-table-header-background | #223353b3 |
| --cosmos-table-row-background | #101a2a73 |
| --cosmos-table-alternate-row-background | #18213681 |
| --cosmos-table-highlight-background | #1B2B42 für Hover und Auswahl |

Zeilenwechsel gilt automatisch; keine zusätzliche ROW_STRIPES-Variante nötig.
Tastaturfokus, native Lumo-Zustände und die vorhandenen responsiven Gridregeln erhalten.

## Dateien und Ladereihenfolge

Basis: `src/main/resources/META-INF/resources/custom-theme/`.

| Datei | Aufgabe |
| --- | --- |
| typography.css | Lokale Inter-Schrift und Schriftfamilie |
| cosmos-tokens.css | Palette, Schrift-/Abstandsskalen, Maße, Rahmen, Flächen, Verläufe und Schatten |
| cosmos-theme.css | Gemeinsame Cards, Formulare, Buttons, Grids und Systemmeldungen |
| cosmos-shell.css | Appkopf, Inhaltsrahmen, Benutzermenü und Overlays |
| cosmos-admin.css | Adminnavigation, Panels, Konfiguration und Dialogstandards |
| cosmos-notifications.css | Inbox, Vorschau, Detail, Versand und Archiv |
| cosmos-background.css | Dekoration, Animation und responsive Anordnung |
| cosmos-account.css | Lokale Konto-/Profilbild-/Sitzungsansichten |
| admin-users.css | Lokale Benutzerfilter, Tabelle, Pagination und Dialoge |

`Application.java` lädt die ersten sieben Dateien in dieser Reihenfolge.
AdminView ergänzt admin-users.css; AccountView/LoginView die benötigten Kontoregeln.
Die Reihenfolge ist Teil der Kaskade. Dialoge brauchen eigene Klassen, da Vaadin
sie außerhalb der View platzieren kann. Keine versteckten CSS-Imports einführen.

## Theme ersetzen oder erweitern

1. Palette, Typografie, Flächen und Schatten in `cosmos-tokens.css` ändern.
2. Gemeinsame Bausteine verwenden diese Tokens; keine neuen lokalen Hex-/RGBA-Farben
   neben der zentralen Palette anlegen.
3. Gemeinsame Regeln in cosmos-theme/shell/admin pflegen, nur Anordnung in lokalen Dateien.
4. Ein neues Theme ersetzt die aktive Basis an der zentralen Ladestelle; keine
   zweite unübersichtliche Override-Kaskade parallel laden.
5. Desktop, schmale Ansichten, Fokus, Fehler, Hover, Auswahl und Dialoge prüfen.

Typische Skalen: `--cosmos-space-xs/s/m/l/xl/2xl/3xl` und
`--cosmos-font-size-xs/s/m/l/xl/title`. Komponentenmaße haben eigene Aliase,
z. B. `--cosmos-control-height`, `--cosmos-pagination-size`, `--cosmos-panel-icon-size`
und `--cosmos-notification-preview-width`. Die vier Tabellenfarben bleiben gemeinsame Tokens.

Nicht jedes gleiche Maß muss gekoppelt werden: Bildausschnitt, Dekoration und
besondere Seitenbreiten können eigene Verträge haben. CSS-Variablen sind keine
Viewportwerte für Mediaqueries; die vorhandenen Breakpoints bleiben wörtlich.

## Avatare und Overlays

`OverlayUserIcon` verbindet Initialen/Bild mit Statuskontur und Glow. Gemeinsame
`--cosmos-avatar-*`-Tokens steuern die Flächen. Header, Popup und Kontobild verwenden
dieselben Statussymbole. Eigene Avatar-/Dialogregeln nicht global auf Buttonicons übertragen.
Benutzermenü und Vorschau bleiben durch Viewportgrenzen scrollbar und erreichbar.

## Sichtbar machen

Im Entwicklungsmodus werden Quellstyles geladen. Bei optimiertem Frontend zuerst
neu bauen und die Anwendung neu starten, dann die Seite neu laden, bei Bedarf Strg+F5.
Ein Browserrefresh allein ersetzt kein bereits gepacktes Stylesheet.
Die tatsächlichen Prüfungen stehen im [aktuellen Stand](status.md).
