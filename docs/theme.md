# Silent Cosmos: Theme und Gestaltung

Vor Gestaltung oder Änderung einer Oberfläche diese Anleitung und die betroffenen
CSS-Quellen lesen und anwenden. Sie beschreibt den aktuellen gemeinsamen Standard
und wird bei jeder ausdrücklich freigegebenen Erweiterung im selben Arbeitsstand gepflegt.

## Theme-Quellen und Template

Das gemeinsame Template besteht aus den vorhandenen Theme-CSS-Dateien und ihren
Tokens. Es gibt derzeit keine separate Template-Eingabedatei und keinen eigenen
CSS-Generator. Die CSS-Dateien unter `app/src/main/resources/META-INF/resources/custom-theme/`
sind die gepflegten Quellen; der optimierte Vaadin-Build erzeugt die ausgelieferten
Frontend-/JAR-Ressourcen. Generierte Ausgaben nicht von Hand bearbeiten.

„Ins Template übernehmen“ bedeutet: die freigegebene Gestaltung an der passenden
gemeinsamen Stelle dieser Quellen verankern und hier dokumentieren. Konkrete Werte
werden in CSS definiert; die folgenden Tabellen beschreiben den dazugehörigen Standard.

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
| Frage-/Erfolgs-/Warnsymbole der Kontoansichten | Breite/Höhe über --lumo-font-size-xxxl, standardmäßig 40 px |

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

Zeilenwechsel gilt im Cosmos-Theme automatisch; keine zusätzliche ROW_STRIPES-
Variante oder benutzerverwaltungsspezifische Klasse nötig. Eine neue Tabelle übernimmt
die gemeinsame Palette über die vorhandenen Gridregeln. Hover und Auswahl verwenden
dieselbe Hervorhebung. Keine eigene Tabellenpalette pro View hinzufügen.
Tastaturfokus, native Lumo-Zustände und die vorhandenen responsiven Gridregeln erhalten.
Die benannten `@media lumo_cosmos_grid_colors`-/`lumo_cosmos_grid_hover`-Blöcke und
ihre `--_lumo-...`-Zuordnungen sind Vaadin-/Lumo-Einschübe, keine Bildschirmbreakpoints;
sie nicht als vermeintlich ungenutzte Mediaqueries entfernen oder umbenennen.

## Vorhandene Bausteine wiederverwenden

Im vorhandenen `.cosmos-theme`-Bereich gelten gemeinsame Vaadin-Controlregeln.
Neue Textfelder oder Buttons benötigen keine eigenen Farb-/Typografieregeln, um
wie die bisherigen auszusehen. Eine vorhandene passende Ansicht dient als Vorlage
für Klassen und Struktur; nicht nur einzelne sichtbare Maße nachbauen.

| Baustein | Bestehende Grundlage | Zuständige CSS-Datei |
| --- | --- | --- |
| Eingabefelder und Formularpaare | Native Vaadin-Felder, `cosmos-form` / `cosmos-form-row` | cosmos-theme.css |
| Primary-/Secondary-/Löschaktionen | Vorhandene Button-Themes und zentrale Control-/Danger-Tokens | cosmos-theme.css / cosmos-tokens.css |
| Panels | Vorhandene Cardbasis und `cosmos-panel-card`, Header, Icon, Heading, Titel, Beschreibung, Footer und Aktionen | cosmos-admin.css |
| Tabellen | Vaadin Grid im Cosmos-Theme, gemeinsame Zeilen-/Header-/Hoverregeln | cosmos-theme.css |
| Dialoge und Schließen | Bestehende Dialogstruktur, Overlay-/Dialogtokens und `cosmos-overlay-close` | cosmos-theme.css / cosmos-admin.css / cosmos-shell.css |
| Konto und Avatare | Bestehende Konto-/Profilbild-/Sitzungskomponenten und `cosmos-avatar-*`-Tokens | cosmos-account.css / cosmos-shell.css |
| Benachrichtigungen | Bestehende Vorschau-/Inbox-/Detail-/Versandkomponenten; Controls und Panelteile aus dem gemeinsamen Theme | cosmos-notifications.css |

Für neue Panels die vorhandene Header-/Inhalts-/Footerstruktur samt passenden
`cosmos-panel-*`-Klassen verwenden. Lokale Fachklassen ergänzen nur die benötigte
Anordnung. Ein neues Eingabefeld bleibt beim vorhandenen Controlstandard;
zusätzliche globale Feldregeln sind dafür nicht nötig.

## Dateien und Ladereihenfolge

Basis: `app/src/main/resources/META-INF/resources/custom-theme/`.

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

## Neue Oberfläche und Übernahme in den Standard

1. Die passende vorhandene Oberfläche, ihre Klassen und zentralen Tokens prüfen.
   Neue Panels, Felder, Buttons und Tabellen zunächst aus diesen Bausteinen zusammensetzen.
2. Benötigte neue Gestaltung in der zuständigen Style-CSS auf die neue Komponente
   begrenzen, zum Beispiel über eine eindeutige Fachklasse. Vorhandene lokale Dateien
   nutzen; eine zusätzliche Datei nur, wenn eine eigene Zuständigkeit sinnvoll ist.
   Bestehende gemeinsame Selektoren und Tokens dabei nicht unbemerkt ändern.
3. Mit dem Benutzer Layout und Bedienung verfeinern. Eine Freigabe für eine lokale
   Anpassung ist keine Freigabe zur Änderung des gemeinsamen Standards.
4. Erst auf ausdrückliche Anweisung wie „fest ins Template übernehmen“ den freigegebenen
   Baustein in den gemeinsamen Theme-Quellen verankern: Werte/Aliase in `cosmos-tokens.css`,
   wiederverwendbare Komponentenregeln in der zuständigen gemeinsamen Datei. Fachliche
   Anordnung bleibt lokal; ein eigener Bereich wie Benachrichtigungen darf seine Datei behalten.
5. Ersetzte Entwurfsregeln und doppelte Overrides im beauftragten Bereich entfernen.
   Gemeinsam genutzte Komponenten auf unbeabsichtigte Auswirkungen prüfen; nötige
   Browserprüfungen nach [Testfreigabe](tests.md#vollständige-browserrunde) durchführen.
6. Diese Anleitung im selben Arbeitsstand ergänzen: Zweck und Einsatz des neuen Bausteins,
   Klassen/Tokens, zuständige Datei und relevante Zustände bzw. Grenzen. Keine datierten
   Designchroniken. Erst mit dieser Pflege ist die Übernahme vollständig.

Die Benachrichtigungsbausteine sind bereits Teil des gemeinsamen Standards:
`cosmos-notifications.css` verwendet die gemeinsamen Controls, Panelregeln und Tokens.
Ähnliche neue Komponenten knüpfen daran an, statt eine zweite Gestaltung zu beginnen.

## Theme ersetzen oder erweitern

Nur im ausdrücklich freigegebenen Umfang:

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

Bei jeder UI-/Designänderung angeben, welcher dieser Fälle tatsächlich gilt:

| Änderung / Betrieb | Übernahme |
| --- | --- |
| Nur CSS, Entwicklungsmodus, aktualisierte Ressource wird bereits ausgeliefert | Seite neu laden, bei Bedarf Strg+F5 |
| Java-Komponenten, neue Stylesheet-Einbindung oder noch nicht aktualisierte Ressourcen | Erforderlichen Build bzw. IDE-Ressourcenabgleich und Anwendungsneustart benennen, danach neu laden |
| Optimierter Frontend-/JAR-/Containerbetrieb | Neu bauen, die eigene Anwendung mit dem neuen Artefakt/Image starten, danach Seite neu laden |

`DevelopmentCssConfiguration` verhindert im Entwicklungsmodus die Auswahl älterer
komprimierter CSS-Kopien. Das ersetzt keinen nötigen Ressourcenabgleich oder Neubau.
Ein Browserrefresh allein ersetzt kein gepacktes Stylesheet. Nur tatsächlich
ausgeführte Prüfungen als erfolgt melden; eine Änderung der Quelle allein belegt
keine sichtbare Übernahme. Die letzten Prüfungen stehen im [aktuellen Stand](status.md).
