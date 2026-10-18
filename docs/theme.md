# Das Cosmos-Theme anpassen und als Vorlage verwenden

Das bestehende Aussehen bleibt der Standard. Farben, Schriftwerte, gemeinsame Maße und Flächen werden zentral in
[cosmos-tokens.css](../src/main/resources/META-INF/resources/custom-theme/cosmos-tokens.css)
bearbeitet. Die übrigen Dateien beschreiben, wie Komponenten diese Werte benutzen
und wie die Oberfläche angeordnet ist.

## Welche Datei ist für was da?

Alle CSS-Dateien liegen unter `src/main/resources/META-INF/resources/custom-theme/`.

| Datei | Zuständigkeit |
| --- | --- |
| `typography.css` | Lokale Inter-Schriften und globale Schriftfamilie |
| `cosmos-tokens.css` | Palette, Typografie-/Abstandsskalen, Rahmen, Radien, Blur, Komponentenmaße, Flächen und Schatten |
| `cosmos-theme.css` | Kontoansichten, Cards, Formularfelder, Buttons, Grids und Benachrichtigungen |
| `cosmos-shell.css` | Gemeinsame Topbar, Seitenkopf, Benutzermenü und Auswahl-Overlays |
| `cosmos-admin.css` | Navigation, Dashboard, Panels, Konfiguration, Mail-Test, Komponentenmaße und Dialoge |
| `cosmos-background.css` | Positionen, Animation und responsive Anordnung der Dekoration; Farben kommen aus den Tokens |
| `cosmos-account.css` | An AccountView: Kontoübersicht, Formulare, echte Sitzungen, native Tabs sowie Konto-/Profilbilddialoge |
| `admin-users.css` | Lokale Anordnung von Benutzerfiltern, Tabelle, Pagination und Benutzer-/Löschdialogen |

`Application.java` lädt die globalen Dateien in dieser Reihenfolge. Erst an
`AdminView` kommt `admin-users.css` hinzu. Die bisherige `styles.css` wurde dafür
umbenannt. `AccountView` lädt separat `cosmos-account.css`; alle dortigen Selektoren
sind auf `.cosmos-account`, `.account-change-dialog` oder `.account-avatar-dialog`
begrenzt und verwenden die gemeinsamen Theme-Variablen. Dialoge tragen ihren
eigenen Geltungsbereich, weil Vaadin sie außerhalb der View platzieren kann.
Es gibt keine versteckten CSS-Imports und keine zusätzliche Buildtechnik.
Die Regelreihenfolge ist Teil der CSS-Kaskade: Bei gleicher Priorität gewinnt der
spätere Wert. Die Aufteilung behält die bisherige Reihenfolge bei.

## Gemeinsame Werte ändern

Der Tokenblock ist die feste Basis des Webthemes. Die Konsolidierung vom
18.10.2026 übernimmt die bisherigen Werte; sie ist kein weiterer Designentwurf.
Die bestehenden Token-Namen für Farben und die vier Tabellenflächen bleiben erhalten.

| Gewünschte Änderung | Zentrale Tokens |
| --- | --- |
| Wiederverwendete Schriftgrößen | `--cosmos-font-size-xs/s/m/l/xl/title` = 11/12/13/14/15/18 px |
| Schriftgewichte | `--cosmos-font-weight-regular/label/medium/semibold/strong/bold` = 400/450/500/600/650/700 |
| Wiederverwendete Abstände | `--cosmos-space-xs/s/m/l/xl/2xl/3xl` = 4/8/12/16/20/24/32 px |
| Rahmen und Rundungen | `--cosmos-border-width`, `--cosmos-control-radius`, `--cosmos-badge-radius`, `--cosmos-segment-radius` |
| Gemeinsame Controls | `--cosmos-control-height`, `--cosmos-control-font-size`, `--cosmos-control-icon-font-size` |
| Icons | `--cosmos-icon-size-xs/s/m/l`; Panelicons separat über `--cosmos-panel-icon-size`, `--cosmos-panel-icon-symbol-size` und ihre `-small`-Varianten |
| Tabellenaktionen / Pagination | `--cosmos-table-action-size`, `--cosmos-table-action-icon-size`, `--cosmos-pagination-size` |
| Meldungen | `--cosmos-notification-icon-size`, `--cosmos-notification-title-font-size`, `--cosmos-notification-message-font-size` |
| Dialogbreiten | `--cosmos-dialog-width-account`, `--cosmos-dialog-width-user`, `--cosmos-dialog-width-confirm` |
| Dialogabstände | `--cosmos-dialog-chrome-padding`, `--cosmos-dialog-content-padding`, `--cosmos-dialog-compact-padding` |
| Hintergrundweichzeichnung | `--cosmos-surface-blur`, `--cosmos-surface-blur-compact`, `--cosmos-sidebar-blur`; Dialogbackdrop separat über `--cosmos-dialog-backdrop-blur` |
| Alle Status-Glows gemeinsam | `--cosmos-avatar-glow-near-blur`, `--cosmos-avatar-glow-far-blur`, `--cosmos-avatar-glow-near-opacity`, `--cosmos-avatar-glow-far-opacity` |
| Breite der Statusauswahl | `--cosmos-status-overlay-width` |

Die Schreibweise mit Schrägstrichen in der Tabelle steht für einzelne Variablen,
z. B. `--cosmos-font-size-xs` und `--cosmos-font-size-s`.
Komponententokens sind häufig Aliase einer gemeinsamen Skala. Für eine Änderung
nur an Buttons `--cosmos-control-font-size` setzen; eine Änderung an
`--cosmos-font-size-m` betrifft dagegen alle bisher mit 13 px gestalteten Bereiche.
Die 14-px-Schriftbasis em-basierter Buttonicons bleibt bewusst separat.

Nicht jedes zufällig gleiche Maß wird gekoppelt: Bildausschnitt, Dekoration,
seitenbezogene Maximalbreiten und besondere Displayüberschriften bleiben lokal.
Bestehende Lumo-Abstände und native Größen behalten ihren eigenen Bezug;
`--cosmos-control-height` folgt standardmäßig weiter `--lumo-size-m`.
Die Breakpoints bleiben wörtliche `@media`-Bedingungen: normale CSS-Variablen
lassen sich dort nicht als Bildschirmbreite einsetzen. Keine zusätzlichen
Override-Dateien oder wechselnden Themes werden gleichzeitig geladen.

### Prüfung der Konsolidierung

Alle acht Stylesheets wurden syntaktisch und auf auflösbare Cosmos-Verweise,
Tokenzyklen und lokale Ressourcen geprüft. Der statische Vergleich mit dem
vorherigen Gitstand löst die Basis-Tokens auf und vergleicht die letzten Werte je
Selektor, Eigenschaft und Media-/Keyframe-Bedingung. Die nachträglichen Konto-
Regeln sind zusammengeführt; eine wirkungslose Flex-Richtung am Grid entfällt.
Ergebnis: acht Stylesheets, 1.732 verglichene Einstellungen und alle 121
bisherigen Basiswerte ohne Wertabweichungen; 513 Cosmos-Verweise und zwei lokale
Schriftressourcen geprüft, keine Tokenzyklen, Media-/Keyframe-Struktur erhalten.
Der zentrale Block enthält jetzt 179 Basiswerte, darunter 58 neue gemeinsame
Skalen und Komponentenparameter.
Dieser Quellvergleich ersetzt keine Prüfung von Browserdarstellung, Vererbung
oder der vollständigen Kaskade. Die visuelle Gesamtprüfung bleibt Teil der
separat eingeplanten DE-/EN-Browsertestrunde; dafür hier kein Testlauf gestartet.

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

Alle Cosmos-Dialoge, einschließlich E-Mail-/Passwortwechsel, Profilbild und
Benutzer anlegen/bearbeiten/löschen, verwenden die gemeinsamen Farbregeln in `cosmos-admin.css`. Lokale Stylesheets
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


## Begrenzung des Benutzermenüs

Das Cosmos-Popover verwendet die native verfügbare Overlayhöhe neben seinem Auslöser.
Sein Overlay ist ein vertikaler Flexrahmen; der Inhalt darf mit `min-height: 0`
schrumpfen und scrollt intern. Eine Maximalhöhe aus der gesamten Viewporthöhe darf
hier die kleinere verfügbare Höhe nicht übersteuern: Bei 320×568 war sonst die
Abmeldeaktion abgeschnitten. Die Palette, Inhaltsabstände und Avatar-/Buttongrößen
bleiben erhalten. CONFIG-05F prüft den Popupinhalt auf horizontalen Überlauf,
die mobile Statusfläche innerhalb des Fensters sowie echtes Scrollen und Abmelden.
Die native Select-Öffnungs- und Schließanimation wird dabei abgewartet.

Die Bereinigung erhält native Lumo-Variablen, interne Grid-Klassen und die vier aus
Java dynamisch gewählten Avatar-Schatten. Entfernt wurden nur drei nachweislich
verwaiste eigene Klassen mit vier Regeln; Details im
[Bereinigungsnachweis](codebereinigung-2026-10-18.md). Die vollständigen
Sprachläufe bestehen DE/EN jeweils mit 141/141 Fällen am gleichen endgültigen Image;
Ergebnisse und Messgrenzen stehen in der [Abdeckung](../browser-tests/abdeckung.md).
