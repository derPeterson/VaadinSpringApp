# Das Cosmos-Theme anpassen und als Vorlage verwenden

Das bestehende Aussehen bleibt der Standard. Farben und Flächen werden zentral in
[cosmos-tokens.css](../src/main/resources/META-INF/resources/custom-theme/cosmos-tokens.css)
bearbeitet. Die übrigen Dateien beschreiben, wie Komponenten diese Werte benutzen
und wie die Oberfläche angeordnet ist.

## Welche Datei ist für was da?

Alle CSS-Dateien liegen unter `src/main/resources/META-INF/resources/custom-theme/`.

| Datei | Zuständigkeit |
| --- | --- |
| `typography.css` | Lokale Inter-Schriften und globale Schriftfamilie |
| `cosmos-tokens.css` | Palette, Komponentenfarben, Flächen, Schatten und benannte gemeinsame Designwerte |
| `cosmos-theme.css` | Kontoansichten, Cards, Formularfelder, Buttons, Grids und Benachrichtigungen |
| `cosmos-shell.css` | Gemeinsame Topbar, Seitenkopf, Benutzermenü und Auswahl-Overlays |
| `cosmos-admin.css` | Navigation, Dashboard, Panels, Konfiguration, Mail-Test, Komponentenmaße und Dialoge |
| `cosmos-background.css` | Positionen, Animation und responsive Anordnung der Dekoration; Farben kommen aus den Tokens |
| `cosmos-account.css` | Nur an AccountView: lokale Anordnung von Kontoübersicht, Formularen, Beispielsitzungen und nativen Vaadin-Tabs |
| `admin-users.css` | Lokale Anordnung von Benutzerfiltern, Tabelle, Pagination und Benutzer-/Löschdialogen |

`Application.java` lädt die globalen Dateien in dieser Reihenfolge. Erst an
`AdminView` kommt `admin-users.css` hinzu. Die bisherige `styles.css` wurde dafür
umbenannt. `AccountView` lädt separat `cosmos-account.css`; alle dortigen Selektoren
beginnen mit `.cosmos-account` und verwenden bestehende Theme-Variablen.
Es gibt keine versteckten CSS-Imports und keine zusätzliche Buildtechnik.
Die Regelreihenfolge ist Teil der CSS-Kaskade: Bei gleicher Priorität gewinnt der
spätere Wert. Die Aufteilung behält die bisherige Reihenfolge bei.

## Einheitliche Badge-Schrift

Alle nativen Badges sowie Cosmos-Rollen- und Topbar-Badges verwenden 12 px bei
Schriftgewicht 400. Das betrifft Kontoaktivierung, „Dieses Gerät“, Sitzungslimit,
Dashboard-Rollen, Tabellenstatus und Rollen sowie aktive Filter und deren Anzahl.
Die gemeinsame Regel steht in `cosmos-theme.css`; die beiden Werte lassen sich über
`--cosmos-badge-font-size` und `--cosmos-badge-font-weight` in `cosmos-tokens.css` ändern.
Lokale Farben, Rahmen, Abstände und Groß-/Kleinschreibung bleiben davon unabhängig.

## Farben ändern: Palette oder einzelne Komponente?

Ein Token ist eine benannte CSS-Variable. `var(--cosmos-input-background)` setzt
den Wert mit diesem Namen ein. Es ist weder Java noch ein zusätzlicher Compiler nötig.

Die erste Gruppe in `cosmos-tokens.css` verwendet Vaadins etablierte Namen:
`--lumo-base-color`, `--lumo-body-text-color`, `--lumo-primary-color`, die Kontrastflächen
und die Error-/Warning-/Success-Farben. Änderungen wirken auf alle Bausteine, die
von dieser Palette abhängen, einschließlich nativer Vaadin-Zustände.
Die transparenten Farbstufen sind einzeln abgestimmt; 10pct ist keine Anweisung,
alle bisherigen Werte neu zu berechnen.

Danach folgen die Komponentenwerte. Sie beziehen ihre Standardfarben häufig aus
der Palette. Eine einzelne Komponente kann hier davon abweichen:

```css
/* Im bestehenden zentralen Tokenblock ändern: */
--cosmos-input-background: var(--lumo-contrast-5pct);
--cosmos-input-border: var(--lumo-contrast-20pct);
--cosmos-input-focus-border: var(--lumo-primary-color-50pct);
```

| Gewünschte Änderung | Zuständige Werte |
| --- | --- |
| Grundflächen und normale Texte | `--lumo-base-color`, `--lumo-body-text-color`, `--lumo-secondary-text-color` |
| Cards | `--cosmos-card-background`, `--cosmos-card-border`, `--cosmos-card-shadow`, `--cosmos-card-radius` |
| Primary-Buttons | `--cosmos-primary-background`, `--cosmos-button-primary-color`, `--cosmos-button-primary-border` |
| Secondary-Buttonrahmen | `--cosmos-button-secondary-border`; native Flächen/Zustände folgen weiter der Lumo-Palette |
| Löschen | `--cosmos-delete-accent-*` und `--cosmos-button-danger-*` |
| Eingaben / Fehler / Fokus | `--cosmos-input-*` |
| Tabellen | `--cosmos-table-header-background`, `--cosmos-table-row-background`, `--cosmos-table-alternate-row-background`, `--cosmos-table-highlight-background`, `--cosmos-table-*-color` |
| Kopfzeile / Seitenleiste | `--cosmos-topbar-*`, `--cosmos-sidebar-*` |
| Navigation | `--cosmos-nav-*`, `--cosmos-subnav-color` |
| Badges / Panelicons | `--cosmos-badge-*`, `--cosmos-panel-icon-*` |
| Overlays | `--cosmos-overlay-*` |
| Dialoge | `--cosmos-dialog-background`, `--cosmos-dialog-header-background`, `--cosmos-dialog-footer-background`, `--cosmos-dialog-border-color`, `--cosmos-dialog-backdrop-blur` |
| Filterfläche | `--cosmos-filter-panel-background` |
| Sterne / Nebel / Planeten | Letzte Tokengruppe: vollständige Verläufe und Schatten |

Filterfläche und gemeinsamer Dialogheader folgen zunächst der Tabellenheaderfarbe.
Ein eigener Wert an diesen beiden Tokens trennt sie bewusst davon. Schatten und
Verläufe sind ebenfalls Werte: Ein Verlauf darf z.B. durch eine einfarbige Fläche,
ein dekorativer Schatten durch `none` ersetzt werden.

Alle vier Dialogvarianten (E-Mail ändern, Benutzer anlegen/bearbeiten/löschen)
verwenden die gemeinsamen Farbregeln in `cosmos-admin.css`. Lokale Stylesheets
regeln ihre Anordnung und Breite. Der Standard bleibt Card-Verlauf für den Inhalt,
Tabellenheaderfarbe für den Header, `#11192a` für den Footer und Tabellenborder für
Außenrahmen und Trennlinien. Der Content selbst bleibt transparent, damit der Verlauf
der Dialogfläche nur einmal gezeichnet wird. Header und Footer verwenden innen
den Außenradius abzüglich des 1px-Rahmens; Lumos eigene kleinere Rundung würde sonst
den Außenrand übermalen. Die Dialogfläche bleibt für native Resize-Griffe nach außen
offen. Außenklick und Escape bleiben native Vaadin-Funktionen; der Hintergrund-Blur
ist eine Cosmos-CSS-Ergänzung auf dem nativen Backdrop. Der Profil-Footer folgt über
`--cosmos-account-footer-background` standardmäßig ebenfalls dem Dialogfooter;
bei Bedarf kann sein Token separat überschrieben werden.

## Warum werden die Werte an mehreren Elementen gesetzt?

`.cosmos-theme` begrenzt die Gestaltung auf die vorgesehenen Seiten. Popups wie
Dialogs, Notifications und Statusauswahl können außerhalb des ursprünglichen
Layouts erscheinen. Deshalb erhalten auch ausdrücklich mit `theme="cosmos"`
markierte Overlay-Komponenten die Palette. Beim zentralen Ändern bleibt diese
Selektorliste erhalten. Globale Farbregeln an `:root` würden andere Themes mit verändern.

CSS-Variablen werden dort aufgelöst, wo sie definiert sind. Deshalb zusammengehörige
Palette und Komponentenwerte im zentralen Block ändern. Lokale Overrides sollen
bewusst die konkrete Komponentenvariable überschreiben; ein später geänderter
Palettewert an einem einzelnen Kind berechnet bereits geerbte Aliaswerte nicht neu.

`::part(input-field)` ist eine von Vaadin freigegebene Oberfläche im Inneren eines
Felds. `:is(...)` fasst mehrere passende Typen zusammen. `:where(...)` gruppiert
Selektoren ohne zusätzliche Priorität, damit individuelle Ausnahmen weiter gewinnen.
`[focused]`, `[invalid]` und `[opened]` beschreiben Komponentenzustände.
Die Kommentare an den Regeln erklären die wichtigen Stellen.

Die benannten `@media lumo_cosmos_grid_colors` und `lumo_cosmos_grid_hover` sind
Vaadin-/Lumo-Einschübe für interne Grid-Zeilen, keine Bildschirmgrößen. Ihre Namen,
Zuordnung und Reihenfolge nicht wie normale responsive Regeln behandeln.
Normale `@media (max-width: ...)` regeln schmale Fenster. Animationen werden bei
`prefers-reduced-motion: reduce` abgeschaltet; diese Regel bleibt erhalten.

## Ein weiteres Theme auf dieser Grundlage bauen

1. `cosmos-tokens.css` unter einem eigenen Namen kopieren und dessen Werte anpassen.
2. In `Application.java` den Verweis auf die Tokendatei austauschen. Es wird genau
   eine Basis-Tokendatei geladen; gemeinsame Komponentenregeln können wiederverwendet werden.
3. Die vorhandenen Klassen und `theme="cosmos"` bleiben dabei zunächst die technischen
   Markierungen der gemeinsamen Bausteine. Ein neuer Dateiname allein verlangt keine
   Änderung aller Java-Komponenten. Dies ist ein alternatives Theme beim App-Start,
   kein bereits implementierter Laufzeit-Themewechsel.
4. Für bewusst neue Komponentenregeln eine nachgelagerte Ergänzungsdatei verwenden.
   Palette, Komponentenregeln und lokale Seitenanordnung dabei getrennt halten.
5. Kontoansichten, Home/Admin, Navigation, Fehler/Fokus, Grids, Badges und alle
   portalierten Overlays auf Desktop und schmalen Fenstern prüfen. Die festgelegten
   Maße und Icongrößen ändern sich durch eine reine Farbanpassung nicht.

Schriftwechsel werden separat in `typography.css` vorgenommen. Die dortigen Fonts
sind lokale Ressourcen; Schriftfamilie und benötigte Fontdateien gehören zusammen.
Die Farb-Tokens ändern keine Icons und ersetzen nicht die Gestaltung des Mail-HTMLs:
E-Mails bleiben eigenständige statische MJML-/HTML-Vorlagen.

## Änderungen sichtbar machen und prüfen

Beim aktuellen Entwicklungsstand die Anwendung aus IntelliJ/Eclipse neu starten,
damit die IDE Klassen und Ressourcen übernimmt, anschließend im Browser aktualisieren.
`DevelopmentCssConfiguration` verhindert im Entwicklungsmodus alte komprimierte
CSS-Antworten. Ein zusätzlicher Konsolenbuild ist für den normalen IDE-Start nicht nötig.

[Designstandard](design-standard.md) und [Browser-Testanleitung](../browser-tests/README.md)
bleiben maßgeblich für die vereinbarten Größen und Prüfabläufe. Nach CSS-Änderungen
muss ein Browser-Testimage frisch gebaut werden; `-NoBuild` würde ein altes Image verwenden.

## Gemeinsame Avatarflächen

`--cosmos-avatar-background`, `--cosmos-avatar-text-color`,
`--cosmos-avatar-border-color` und `--cosmos-avatar-shadow` in `cosmos-tokens.css`
steuern die gemeinsame Initialen-/Bildfläche. Die lokale Größe kommt von der
Avatar-Komponente. Kopfzeile, Benutzerpopup und Kontozusammenfassung zeigen dasselbe SVG-Kontursymbol
wie die Statusauswahl. Diese drei Avatarflächen übernehmen über
`--cosmos-avatar-status-color` und die vier zentralen `--cosmos-avatar-*-shadow`-
Tokens die Statusfarbe für Rahmen und Glow. Die freigegebene Vorschauvariante
„Kräftig“ kombiniert 6 px / 80 % und 26 px / 60 %; der Rahmen bleibt 2 px.
Am Kontobild liegt ein nativer Select über der vergrößerten Klickfläche des Zeichens;
nur dessen Feldfläche ist transparent, das Cosmos-Dropdown bleibt unverändert sichtbar.
Der Tastaturfokus erhält einen sichtbaren Ring. Die lokalen Regeln stehen in
`cosmos-account.css`; übrige Profilbildflächen zeigen Bild bzw. Initialen.
Beide Statuslisten tragen `theme="cosmos-status"`. Nur dort ist der `checkmark`-Part
ausgeblendet. Gewählte, gehoverte und fokussierte Einträge unter dem ListBox-Element
verwenden `--cosmos-status-option-background` (Tabellenhighlight); die geschlossene
Auswahl bleibt unverändert. Andere Selects behalten ihre Auswahlhaken.
Die verwendeten Parts folgen der [Vaadin-Select-Styling-API](https://vaadin.com/docs/latest/components/select/styling).
Bildspeicherung und Prüfgrenzen: [Profilbildvertrag](profile-avatar-contract.md).
