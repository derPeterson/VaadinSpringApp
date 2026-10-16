# Verbindlicher Oberflächenstandard: Silent Cosmos

Stand: 15.10.2026. Das gemeinsam abgestimmte Redesign ist der Standard für neue
Oberflächen. Änderungen erfolgen gezielt; eine lokale Anpassung ändert keine
anderen Ansichten. Neue Entwürfe übernehmen zuerst die bestehenden Bausteine.

## Gemeinsame Bausteine

Vaadin Flow bleibt die Komponentenbasis. `cosmos-tokens.css` enthält Palette,
Komponentenfarben, Verläufe, Schatten und gemeinsame Designwerte. `cosmos-theme.css`
verwendet sie für Kontoansichten, Cards, Eingaben, Buttons und Grids. App-Kopfzeile,
Benutzermenü und Auswahl-Overlays stehen in `cosmos-shell.css`; Navigation, Panels,
Konfiguration und Dialogstandards in `cosmos-admin.css`.
`admin-users.css` ergänzt nur die lokale Benutzerverwaltung und wird an AdminView geladen.
Die bisherige `styles.css` wurde dafür umbenannt. Dekorationsfarben stehen ebenfalls
in den Tokens; `cosmos-background.css` ordnet und animiert die Dekoration.
Inter wird separat lokal geladen. [Theme-Anleitung](theme.md): Zuständigkeiten,
Farben ändern und ein weiteres Theme auf dieser Basis aufbauen.
Formularpaare verwenden `cosmos-form-row` und stehen unter 600 px untereinander.

| Element | Abgestimmte Gestaltung |
| --- | --- |
| Primary/Secondary | Lumo M (36 px), Radius 9 px, Text 13 px / 600 |
| Primary | Vorhandener blauer Verlauf mit Primary-Border und Kontrastschrift |
| Secondary | Input-Hintergrund, Primary-Text, 1 px `--lumo-contrast-20pct` |
| Button-Icons | Bisherige Größe erhalten; gemeinsame Primary-/Secondary-Regel bewahrt die 14-px-Schriftbasis für Lumo-em-Icons |
| Tabellenaktionen | Fläche 32 × 32 px, Icons 16 × 16 px |
| Pagination | Fläche 28 × 28 px, Schrift 11 px, Abstand 4 px; aktiv Primary, sonst Secondary; margin-top 16 px |
| Eingaben | Lumo M, Text 13 px / 700; leere Eingaben und Platzhalter 13 px / 400 |
| Feldlabels | 12 px / 450, Secondary |
| Hilfetexte im Benutzerformular | 11 px, Secondary |
| Paneltitel / Untertitel | Gemeinsame `cosmos-panel-title` / `cosmos-panel-description` |
| Card-Zähler oben rechts | 11 px, Primary, Großbuchstaben |
| Bereichsüberschriften | Abstand Bereichszeile–Titel 10 px, Titel–Beschreibung 6 px; Beschreibung 12 px |
| SideNav | Hauptpunkte 13 px / 600, aktiv 700; Unterpunkte 12 px / 500, aktiv 600 |
| Schließen-Buttons | Gemeinsamer `cosmos-overlay-close`, Fläche und Icon Lumo M; vorhandenes CLOSE_CIRCLE |

Größen und Icons werden nicht durch neue Vorschauen stillschweigend überschrieben.
Individuelle Pagination und Tabellenaktionen behalten ihre abgestimmten Ausnahmen.

## Farben und Rahmen

Cards verwenden `--cosmos-card-background`, `--cosmos-card-border` und
`--cosmos-card-radius` (18 px). Benutzer- und Löschdialog verwenden außen sowie
unter Header/über Footer den Tabellen-Border: 1 px `--lumo-contrast-20pct`.
Beide Dialogheader und die Benutzerfläche im Löschdialog verwenden die Tabellen-Headerfarbe.

| Tabellenvariable | Wert |
| --- | --- |
| `--cosmos-table-header-background` | `#223353b3` |
| `--cosmos-table-row-background` | `#101a2a73` |
| `--cosmos-table-alternate-row-background` | `#18213681` |
| `--cosmos-table-highlight-background` | `#1B2B42` für Hover und Auswahl |

Diese Farben gelten für weitere Cosmos-Grids. Header: 12 px, Primary,
Großbuchstaben, Höhe Lumo L. Zellen: 12 px, Secondary, normale Gewichtung;
Namen bleiben fett. Sortierindikatoren sind Primary und vertikal ausgerichtet.
Ein Fokusrahmen bleibt für Tastaturbedienung erhalten.

`cosmos-delete-button` und `cosmos-delete-accent` teilen sich die rote Farbregel:
`--lumo-error-text-color`, 1 px Border in derselben Farbe, dezenter Hintergrund
`--lumo-error-color-10pct`. Der Akzent ist auch für nicht interaktive Warnflächen
geeignet. Der endgültige Primary-Löschbutton verwendet die vereinbarte 50-%-Mischung
von `--lumo-error-color` mit `--lumo-base-color`. Keine zusätzlichen roten Hexfarben.

## Filter und Dialogverhalten

- Die Namens-/E-Mail-Suche wirkt sofort. Der Secondary-Button „Filter“ schaltet
  einen nativen Details-Bereich mit dauerhaft vorhandenen Eingaben ein und aus.
- Zusätzliche Kriterien sind Entwürfe, bis „Filter anwenden“ sie übernimmt.
  Aktive Badges und deren Anzahl beschreiben die angewandten Kriterien.
  Ein Badge-X entfernt nur dieses Kriterium und übernimmt keine anderen Entwürfe.
  „Alle zurücksetzen“ leert Suche, Entwürfe und angewandte Kriterien.
- Das Aktivitätsdatum ist inklusive. Ohne Zeit gilt 00:00 Uhr. Die Zeit ist nur mit
  gültigem Datum aktiv; Löschen/ungültiges Datum leert und deaktiviert sie.
  Platzhalter: heutiges Datum und auf die vorherige Viertelstunde gerundete Uhrzeit,
  jeweils lokalisiert. Das Zeitoverlay ist höchstens 300 px hoch. Sein Scrollen
  zum Platzhalter darf keinen Wert auswählen.
- Im Bearbeitungsdialog lässt ein leeres Passwort den bisherigen Hash unverändert.
  Erst bei Eingabe wird „Bestätigen“ aktiv und verpflichtend. Beide Passwörter
  müssen exakt übereinstimmen; die bisherigen zentralen Passwortregeln gelten weiter.
  Leeren des ersten Feldes leert/deaktiviert die Bestätigung. Fehler erscheinen
  nach Verlassen des jeweiligen Feldes oder beim Speichern, nicht beim ersten Zeichen.
- Der Passworthinweis nutzt die Breite beider Spalten. Das Geburtstagsbeispiel
  entspricht der Registrierung. Aktiv/Rollennamen entsprechen ausgefüllten Eingaben.
- Öffnen, Abbrechen oder Schließen des Löschdialogs löscht nichts. Nur die bestätigte
  Aktion darf das ausgewählte Konto löschen; bestehende Schutzprüfungen bleiben erhalten.

## Dialoge schließen und Hintergrund

Seit 16.10.2026 verwenden alle Cosmos-Dialoge den gemeinsamen Hintergrund-Blur
von 6 px (`--cosmos-dialog-backdrop-blur`). Die Regel liegt in `cosmos-admin.css`
und betrifft nur den nativen Backdrop; der Dialog selbst bleibt scharf.
Benutzer anlegen, bearbeiten und löschen sowie der E-Mail-Dialog erlauben
Escape und einen Klick außerhalb zum Schließen. Dabei wird nichts gespeichert
oder gelöscht; ungespeicherte Dialogeingaben werden verworfen.

## Änderungen sichtbar machen

Bei diesem Entwicklungsstand: Anwendung aus IntelliJ/Eclipse neu starten und den
Browser aktualisieren. Reiner Refresh reicht erst, wenn die IDE die aktuelle
Ressource kopiert hat. `DevelopmentCssConfiguration` verhindert im Entwicklungsmodus
veraltete komprimierte CSS-Antworten. Das ist kein Ersatz für das Kopieren von Ressourcen.
Neue Getter in `shared/MessageTexts` erfordern zuvor `shared` → Maven → Lifecycle →
`install` und bei Bedarf ein Neuladen der Maven-Projekte. Ein App-Build allein
aktualisiert das eigenständige Shared-JAR nicht.

Prüfverfahren und tatsächliche Ergebnisse: [Browseranleitung](../browser-tests/README.md)
und [Abdeckung](../browser-tests/abdeckung.md). Dieses Dokument beschreibt den
abgestimmten Sollzustand, keine allein daraus abgeleitete Testgarantie.
