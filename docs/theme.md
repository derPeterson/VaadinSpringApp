# Silent Cosmos: Theme und Gestaltung

**Cosmos ist das gemeinsame Theme auf Basis von Vaadin Aura.** Neue Ansichten
verwenden die vorhandenen Komponenten und Tokens. Die laufende Vaadin-Oberfläche
dient als gestalterische Vorlage.

| Ich möchte … | Hier nachschlagen |
| --- | --- |
| Farben, Schrift und Flächen ändern | [Grundlagen](#grundlagen) |
| einen vorhandenen Baustein einsetzen | [Gemeinsame Bausteine](#gemeinsame-bausteine) |
| Header, Navigation und Seitenpfade verstehen | [Seitenrahmen](#seitenrahmen-und-navigation) |
| Anbieter-, Queue- oder 2FA-Ansichten erweitern | [Fachoberflächen](#fachoberflächen) |
| Icons und Avatare gestalten | [Icons und Identität](#icons-und-identität) |
| die zuständige CSS-Datei finden | [Dateien und Ladereihenfolge](#dateien-und-ladereihenfolge) |
| Gestaltung in den Standard übernehmen | [Änderungen am Theme](#änderungen-am-theme) |

Fachliche Abläufe stehen unter [Anwendung](anwendung.md), [Sicherheit](sicherheit.md),
[Mailqueue](mailqueue.md) und [2FA](zwei-faktor-authentifizierung.md).
Hier stehen Darstellung, Wiederverwendung und die wichtigen technischen Grenzen.

## Grundlagen

### Basis: Vaadin Aura mit Cosmos-Schichten

Grundlage ist das Vaadin-Theme **Aura** (`Aura.STYLESHEET`, Version laut `pom.xml`).
Lumo wird nicht geladen; Lumo-Variablen, `LumoUtility` und `ButtonVariant.LUMO_*`
werden nicht verwendet (Stylelint lehnt `--lumo-*` ab). Das eigene Theme heißt weiterhin
**Cosmos** und legt sich in Schichten über Aura. Das dunkle Schema ist Standard
(`@ColorScheme(DARK)`); die Tokens sind so angelegt, dass ein späterer Light-Modus über
`light-dark()` ergänzt werden kann. Ein Light-Modus ist derzeit nicht umgesetzt.

Vorteile: native Vaadin-Komponenten mit aktuellem Basistheme, eine einzige Farbquelle,
sichtbare Ladeordnung. Grenzen: Aura-Standards weichen teils ab und werden in
`cosmos-aura.css` zurückgesetzt (siehe [Aura-Abgleich](#aura-abgleich-und-akzeptierte-abweichungen));
Aura-Updates können dort Nacharbeit erfordern.

### Theme-Quellen und Template

Das gemeinsame Template besteht aus den CSS-Dateien unter
`app/src/main/resources/META-INF/resources/custom-theme/`. Es gibt keine separate
Template-Eingabedatei und keinen CSS-Generator; der optimierte Vaadin-Build erzeugt die
ausgelieferten Ressourcen. Generierte Ausgaben nicht von Hand bearbeiten.

„Ins Template übernehmen“ bedeutet: die freigegebene Gestaltung an der passenden
gemeinsamen Stelle dieser Quellen verankern und hier dokumentieren. Konkrete Werte
werden in CSS definiert; die folgenden Tabellen beschreiben den dazugehörigen Standard.

### Palette

`cosmos-palette.css` ist die einzige Stelle für Farbwerte der Oberfläche
(`--cosmos-color-*`). Semantische Tokens in `cosmos-tokens.css` verweisen darauf;
Fachdateien verwenden Tokens oder Palettenfarben. Transparente Flächen entstehen per
`color-mix()` aus diesen Farben, nicht als eigene RGBA-Werte. Ausnahmen mit eigenen
Farbwerten: die dekorative Hintergrundszene (`cosmos-background.css`) und die
Google-Markenfarben in `cosmos-external-login.css`.

Kontraste nach WCAG 2.x (normaler Text ≥ 4,5:1, große Schrift und Bedienelemente ≥ 3:1):

| Rolle | Token | Wert | Kontrast |
| --- | --- | --- | --- |
| Seite | `--cosmos-color-space` | `#050811` | – |
| Panel | `--cosmos-color-panel` | `#080e1b` | – |
| Eingabe | `--cosmos-color-input` | `#091120` | – |
| Dialog/Overlay | `--cosmos-color-raised` | `#0e1726` | – |
| Auswahl | `--cosmos-color-selection` | `#122342` | – |
| Hover | `--cosmos-color-hover` | `#152238` | – |
| Text | `--cosmos-color-text` | `#e2e8f0` | 15,6 auf Panel |
| Überschrift | `--cosmos-color-white` | `#ffffff` | – |
| Label | `--cosmos-color-label` | `#cbd5e1` | 13,0 |
| Sekundär | `--cosmos-color-secondary` | `#94a3b8` | 7,5 |
| Gedämpft | `--cosmos-color-muted` | `#7c8aa0` | 5,4 auf Eingabe |
| Akzent (Text/Icon) | `--cosmos-color-sky` | `#38bdf8` | 9,0 |
| Primary-Fläche | `--cosmos-color-primary` | `#0077b6` | Weiß 4,9 |
| Primary-Hover | `--cosmos-color-primary-hover` | `#00679e` | Weiß 6,0 |
| Erfolg | `--cosmos-color-success` | `#34d399` | 10,0 |
| Warnung | `--cosmos-color-warning` | `#fbbf24` | 11,6 |
| Fehler | `--cosmos-color-danger` | `#f87171` | 7,0 |
| Fehlertext hell | `--cosmos-color-danger-light` | `#fca5a5` | – |
| Zählerbadge | `--cosmos-color-danger-strong` | `#b4233b` | Weiß 6,5 |
| System (Violett) | `--cosmos-color-violet` / `-light` | `#a855f7` / `#d8b4fe` | Text 9,5 |

Weiß auf Violett erreicht nur 3,96:1; Violett dient daher als Rahmen/Akzent, nicht als
Fläche hinter normalem Text. Gedämpfter Text und Primary-Fläche sind für ausreichenden
Kontrast bewusst heller bzw. dunkler gewählt.

### Typografie

`typography.css` definiert Inter (variable Schrift) mit Roboto/Systemschrift als Ersatz
(`--cosmos-font-family`) und Space Mono für Codes, Schlüssel und technische Ausgaben
(`--cosmos-font-family-mono`). Markenvorgaben wie die Google-Schrift im Google-Button
bleiben davon unberührt. Alle Schriftgrößen verwenden die Skala aus `cosmos-tokens.css`:

| Token `--cosmos-font-size-*` | Wert | Einsatz |
| --- | --- | --- |
| `2xs` | 10 px | Großbuchstaben-Labels (Eyebrows, Kennzahlbeschriftungen) |
| `xs` | 11 px | Badges, Metadaten, Tabellenköpfe |
| `s` | 12 px | Labels, Hilfetexte |
| `m` | 13 px | Fließtext in Panels, Eingaben, Buttons |
| `l` | 14 px | Hervorgehobener Text, Navigation |
| `xl` | 15 px | Bestätigungsfragen |
| `2xl` | 16 px | Marke, Abschnittstitel |
| `title` | 18 px | Panel- und Dialogtitel |
| `heading-s` / `heading-m` / `heading-l` | 20 / 24 / 30 px | Seitenüberschriften (mobil/Desktop) |
| `3xl` / `display` | 28 / 32 px | Große Symbolschrift, Hero-Zahlen |

Stärken: 400 regulär, 500 mittel, 600 halbfett, 700 fett
(`--cosmos-font-weight-*`).

Zeilenhöhen stehen als Tokens in `cosmos-tokens.css`. Neue gemeinsame Bausteine verwenden
diese Skala; vorhandene fachliche Varianten stehen in ihren CSS-Quellen:

| Token | Wert | Einsatz |
| --- | --- | --- |
| `--cosmos-line-height-xs` | 16 px | Großbuchstaben-Labels, Badges, Metadaten |
| `--cosmos-line-height-s` | 18 px | kompakte Texte und Labels |
| `--cosmos-line-height-m` | 20 px | Fließtext in Controls, Panels und Listen |
| `--cosmos-line-height-l` | 24 px | 16-px-Titel |
| `--cosmos-line-height-xl` | 28 px | Panel- und Dialogtitel |
| `--cosmos-line-height-heading-m` / `-heading-l` | 32 / 36 px | Seitenüberschriften mobil / Desktop |
| `--cosmos-line-height-display` | 38 px | Hero-Zahlen |
| `--cosmos-line-height-tight` | 1.375 | mehrzeilige Überschriften und Tooltips |
| `--cosmos-line-height-copy` | 1.5 | Standardfließtext |
| `--cosmos-line-height-relaxed` | 1.7 | längere Hinweis- und Hilfetexte |

Bewusste Ausnahmen: `1` für einzeilige Symbol- und Zählerflächen sowie die fließende
Hero-Überschrift der Startseite.

## Gemeinsame Bausteine

### Native Vaadin-Bausteine zuerst

Vor neuen UI-Bausteinen die APIs der tatsächlich verwendeten Vaadin-Version prüfen.
Native Flow-Komponenten, öffentliche Parts, Theme-Varianten und Variablen haben Vorrang.

- Einfache Zeilen und Spalten: `HorizontalLayout` und `VerticalLayout`.
- Formulare: `FormLayout` mit den vorhandenen responsiven Grenzen.
- CSS Grid/Flex gezielt für fachliche responsive Kompositionen verwenden.
- Ein eigener Ersatz für eine spezialisierte Komponente benötigt eine ausdrückliche
  Freigabe. Normale Layoutcontainer sind kein solcher Ersatz.
- Die freigegebenen Lucide-/Tabler-Icons und der Canvas-Bildausschnitt bleiben zulässig.

Aktionen, Bestätigungen und Downloads verwenden native **Buttons**; ein `Anchor`
dient der Navigation. Fachliche Prüfungen bleiben in den Diensten.

### Vorhandene Bausteine wiederverwenden

| Aufgabe | Baustein / Grundlage | Gestaltung |
| --- | --- | --- |
| Seitenrahmen und Navigation | `PageFrameComponent`, `PageContentLayout`, `PageNavigationComponent` | `cosmos-shell.css` |
| Große Panels | `CosmosCardComponent`, `AccountPanelComponent` | `cosmos-cards.css`, Fachdatei |
| Kleine Kontext-, Anbieter- und Sitzungskarten | `CosmosContentCardComponent` | `cosmos-cards.css`, Fachdatei |
| Öffentliche Formularpanels | `PublicPanelComponent` | `cosmos-cards.css`, `cosmos-public.css` |
| Kennzahlen | `CosmosMetricCard` | `cosmos-metric-card.css` |
| Eingaben | Native Vaadin-Felder | `cosmos-theme.css` |
| Zahl und Zeiteinheit | `CosmosDurationCustomField`, `CosmosDurationField` | `cosmos-duration-field.css` |
| Eingabe mit Feldhilfe | `FieldHelpRowLayout`, `ConfigInfoButton`, `AdminSidePanelDialog` | `cosmos-admin.css` |
| Segmentierte Auswahl | `CosmosSegmentedTabs<T>` | `cosmos-segmented-tabs.css` |
| Auf-/Zuklappen | Native `Details`, `DetailsToggleButton` | `cosmos-theme.css` |
| Bestätigung | `PortalConfirmationDialog`, `NotificationReadAllConfirmationDialog` | `cosmos-confirmation.css` |
| Globale Meldung | `NotificationHelper` | Gemeinsame Toast-Regeln |
| Identität | `CosmosAvatar`, `OverlayUserIcon` | `cosmos-avatar.css`, `cosmos-shell.css` |
| Seitennavigation | `CosmosPagination` | `admin-users.css`, lokale Anordnung in den Fachdateien |

Wiederkehrende Klassen im Java-Code über `CosmosCssClasses` verwenden. Der Halter
enthält Bezeichner, keine Gestaltung. Ein neues Textfeld braucht keine zusätzlichen
lokalen Farb-, Fokus- oder Disabled-Regeln.

`CosmosPagination` verbindet native `HorizontalLayout`, `ComboBox` und `Button` für
Benutzerverwaltung, Versandhistorie, Ticketliste und persönliche Inbox. Nummern,
Pfeile und Seitengrößen werden gemeinsam verwaltet; Filter und Datenabfragen bleiben
beim Aufrufer. Die Inbox behält ihre eigene Footeranordnung in
`cosmos-notifications.css`, verwendet aber die gemeinsamen `admin-user-pagination-*`-
Klassen. `showPage` kann die tatsächlich geladenen Zeilen berücksichtigen;
`showUnavailable` sperrt die Navigation und zeigt einen Gedankenstrich statt des
Bereichs. Die Seitengröße bleibt dabei bedienbar. Seitenzahlen erhalten in
`cosmos-aura.css` keinen Abstand zu einem leeren Präfix, damit sie mittig stehen.

### Gestaltungsregeln

#### Buttons und Eingaben

| Element | Gemeinsamer Standard |
| --- | --- |
| Normale Aktionsbuttons | 36 px Höhe, 9 px Radius; vorhandene Primary-/Secondary-Variante |
| Tabellenaktionen | 32 × 32 px, 16-px-Icon |
| Pagination | Kompakte native Controls; aktive Seite in Primärfarbe |
| Eingaben | Gemeinsame Feldfläche; Höhe und Schrift aus der jeweiligen Controlvariante |
| Labels | Gemeinsame Labelschrift; rotes Sternchen nur bei tatsächlichem Pflichtfeld |
| Schließen | `cosmos-overlay-close`, `CLOSE_CIRCLE`, feste Icongröße aus `--cosmos-close-icon-size` |
| Löschaktionen | `cosmos-delete-button`, gemeinsame Danger-Tokens |
| Warnaktionen | `cosmos-warning-button`, gemeinsame Warnpalette |

Schließen-Buttons in Dialogköpfen, Popovers und eingeblendeten Notifications teilen
die Regeln in `cosmos-theme.css`: 36-px-Button und festes 18-px-Kreis-X. Die Icongröße
kommt ausschließlich aus `--cosmos-close-icon-size` in `cosmos-tokens.css`, unabhängig
von geerbter Schriftgröße oder Seitenrahmen. Keine em-Größen oder lokalen Kopien
in Shell-, Aura- oder Bereichs-CSS ergänzen. Die Empfängerliste verwendet denselben Stil.

Beschriftete Felder erhalten keine dekorativen Präfixicons. Suchlupe und native
rechte Feldaktionen bleiben erhalten. Feldicons sind neutral in Ruhe, heller bei
Hover und akzentblau bei Fokus beziehungsweise geöffnetem Picker.
Disabled ist auch an Label, Wert und Platzhalter erkennbar. Read-only, Fehler und
Tastaturfokus bleiben unterscheidbar.

Zentrierte Zahlen- und Dauerfelder mit Löschen-Button reservieren dessen Platz
auch bei leerem Wert. Die Aktion bleibt dabei unsichtbar und nicht bedienbar;
der Placeholder springt dadurch nicht in eine breitere Eingabefläche. Die Regel
liegt gemeinsam in `cosmos-duration-field.css` und verwendet `::part(clear-button)`.

Vergrößerbare TextAreas setzen `resize:vertical` auf `::part(input-field)`.
Den Host nicht mit `overflow:hidden` beschneiden: Der außerhalb der Fläche
liegende gemeinsame Fokusrahmen muss wie bei einzeiligen Textfeldern sichtbar bleiben.

Bearbeitbare `TextField`-, `EmailField`-, `PasswordField`- und `TextArea`-Eingaben
aktivieren das native Löschkreuz mit `setClearButtonVisible(true)`. Das gilt auch
für neue Eingabefelder. Vaadin blendet das X bei leeren, deaktivierten und
schreibgeschützten Feldern selbst aus. Keine zusätzlichen Clear-Buttons oder
CSS-Nachbauten verwenden. Die native Einstellung aktiviert auch Esc zum Leeren;
bei einer TextArea betrifft das den gesamten Entwurf.

Ausgenommen bleiben einzelne OTP-Ziffern und die E-Mail-Codeeingabe der 2FA;
Ersatzcodes als zusammenhängende Texteingabe haben ein X. Reine Anzeige-/Kopierfelder
bleiben schreibgeschützt und ohne Löschkreuz. Zahlen- und Cosmos-Dauerfelder
verwenden weiterhin ihre bereits vorhandenen nativen Löschaktionen.

Checkboxen verwenden für den Haken und den Strich einer Teilauswahl die weiße
Überschriftenfarbe über `--vaadin-checkbox-marker-color` in `cosmos-tokens.css`.
Dies gilt auch innerhalb von Checkbox-Gruppen. Deaktivierte und schreibgeschützte
Checkboxen behalten Auras eigene Markerfarben; Radio-Buttons und Switches sind davon unabhängig.

Pflichtfelder verwenden Vaadins natives Sternchen `*` statt eines eigenen Punkts.
Die gemeinsame Farbe steht in `--vaadin-input-field-required-indicator-color` und
verweist auf `--cosmos-error-color`. Das helle Rot erreicht auf den üblichen dunklen
Seiten-, Panel-, Eingabe- und Overlayflächen etwa 6,5:1 bis 7,2:1 Kontrast. Der Indikator
bleibt direkt hinter dem Label. Die vorhandenen separaten Konfigurationslabels zeigen
dieselbe Kennzeichnung; Ticket-Vermerke verwenden dafür ebenfalls Rot statt Violett.
Das Sternchen kennzeichnet eine Pflichtangabe unabhängig vom aktuellen Fehlerzustand.

#### ComboBox-Auswahllisten

Einfach- und Mehrfachauswahl verwenden denselben Außenabstand von 8 px:
`--cosmos-choice-overlay-padding` verweist auf `--cosmos-space-s`.
`cosmos-shell.css` übergibt ihn an Vaadins `--vaadin-item-overlay-padding`; der native
Scroller berücksichtigt ihn an allen vier Seiten. Inhalt und Overlay erhalten keinen
zusätzlichen Innenabstand. Das gilt auch für Rollen, Geschlecht, Sprache und Pagination.

Zwischen Auswahlflächen bleiben 2 px frei: eine transparente Host-Border von 1 px
je Eintrag wird vom Virtualizer mitgemessen. `cosmos-theme.css` verwendet dieselbe
Abstandsbildung für die gerenderten Einfachauswahlen und die nativen Mehrfachauswahlen.
Kompakte Pagination-Einträge behalten ihre 32 px hohe innere Zeile; normale
Einfachauswahlen ihre 42 px hohe Zeile. Mehrzeilige Empfänger bleiben entsprechend höher.
Fachdateien gestalten Beschriftung und Zeileninhalt, nicht den gemeinsamen Außenabstand.

#### Hinweise und Warnungen

- Gerahmte Infoboxen verwenden `--cosmos-info-box-padding` und `--cosmos-info-box-gap`.
- Sicherheitswarnungen verwenden `cosmos-security-warning` und die Warnpalette.
- Bei **fetter Einleitung mit Doppelpunkt** steht das Icon an der ersten Textzeile
  (`Alignment.START`). Die Erklärung folgt direkt daneben.
- Bei reinem Fließtext darf das Icon zur gesamten Texthöhe zentriert bleiben.
- Inhaltlich getrennte Hinweise stehen in Absätzen, ohne HTML im Textkatalog.
- Footerhinweise verwenden die bestehende Fläche und Trennlinie.

#### Badges und Tabellen

Native `Badge`s zeigen Status und Zähler. Beschriftete Statusbadges bekommen keinen
zusätzlichen Punkt; eigenständige Avatar- und Aktivitätspunkte bleiben.
Rahmen und Fläche kommen aus `cosmos-theme.css`, die fachliche Farbe aus dem Bereich.
Systemkennzeichnungen verwenden `--cosmos-system-*` in Violett.

Grids teilen Header-, Zeilen-, Wechsel-, Hover- und Auswahlfarben aus den gemeinsamen
`--cosmos-table-*`-Tokens. Keine zweite Tabellenpalette pro View einführen.
Sortierung, Tastaturfokus und vorhandene responsive Regeln bleiben erhalten.

### Native Kartenhüllen für bestehende Ansichten

`CosmosCardComponent` und `CosmosContentCardComponent` verwenden native Cards.
Die Fachklassen bestimmen Rahmen, Fläche und Anordnung. `cosmos-cards.css`
neutralisiert zusätzliche native Innenabstände; Kopf und Footer nutzen öffentliche Slots.

- Sichtbare Überschriften als zugängliche Card-Beschriftung anbinden.
- Keine zusätzlichen Kartenrahmen um Tabellenzeilen oder illustrative Kompositionen legen.
- `ComponentTestTree` berücksichtigt in JUnit auch Header-/Footer-Slots;
  `Card.getChildren()` allein liefert nur den normalen Inhalt.
- Eine Komponenten-Umstellung erlaubt keine beiläufige Neugestaltung.

`CosmosMetricCard` ist eine statische Kennzahl ohne Klick-/Hoveraktion.
`setLabel(...)`, `setValue(...)` und `setHighlighted(...)` aktualisieren den Inhalt.
Die Label-/Wert-Spans bleiben erhalten, damit äußere Schriftanpassungen und IDs
bei Sprach- oder Zählerwechseln nicht verloren gehen. Inbox und Benutzerverwaltung
verwenden ihre vorhandenen lokalen Größenvarianten.

### Formulare und Zeitfelder

| Bereich | Lokale Gestaltung |
| --- | --- |
| Persönliche Daten | `account-profile-form` / `cosmos-account.css` |
| Registrierung | `public-registration-form` / `cosmos-public.css` |
| Benutzer anlegen/bearbeiten | `admin-user-paired-form` / `admin-users.css` |
| Allgemeine Konfiguration | `cosmos-config-fields` / `cosmos-admin.css` |
| Anmeldeanbieter | `cosmos-provider-settings.css` |
| 2FA-Regeln | `cosmos-mfa.css` |

FormLayout nutzt die vorhandenen Breakpoints und öffentlichen Variablen, keine
privaten Vaadin-Layoutvariablen. `FieldHelpRowLayout` lässt das Feld wachsen und
schrumpfen, während die Feldhilfe ihren Platz behält.
In der Benutzeranlage/-bearbeitung belegt die Rollen-Auswahl auf Desktop nur die
linke Feldspalte; rechts bleibt frei. Zugang gesperrt sowie bei der Anlage durch
Superadmins Verifikation und Entwicklerkonto sind native Switches. Sie stehen
jeweils in einer eigenen Zeile über die gesamte Formularbreite. Jeder Switch erhält
oben und unten S (`--cosmos-space-s`); zwischen zwei Switches ergeben sich damit 16 px.
Beschriftung und Untertext behalten ihre bisherige Typografie und dürfen umbrechen.
Umschalten ändert zunächst den Entwurf, erst Speichern übernimmt ihn.
Boolesche Konfigurationen nutzen native Switches mit `cosmos-config-switch`:
Label links, Bahn rechts, Hilfe außerhalb. Erst Speichern übernimmt den Entwurf.
Auch „Alle Benutzer“ im Benachrichtigungsentwurf verwendet einen nativen
Switch. Beschriftung und Untertext entsprechen den Optionen im Benutzerdialog:
Titel mit Gewicht 500, Hinweis in kleiner sekundärer Schrift mit Gewicht 400. Er verändert die Auswahl für
die nächste Vorschau; die Freigabe zum Senden entsteht weiterhin erst durch Prüfung.

**Zentrales Switch-Grundstyling:** `cosmos-theme.css` gestaltet alle nativen
`vaadin-switch` direkt, auch in ausgelagerten Overlays. Eine zusätzliche Klasse
oder Java-Hülle ist dafür nicht nötig. Bahn, runder Griff, aktive Ein-/Aus-Farben
und Tastaturfokus haben genau eine gemeinsame Quelle. Disabled-/Read-only-Farben
bleiben bei Aura; die normale Farbregel schließt diese Zustände ausdrücklich aus.
Die Griffgröße bleibt beim Drücken stabil. Keine lokale Pixelverschiebung des Griffs.

Die Bereichsdateien regeln weiterhin Labeltypografie, Untertext, Umbruch,
Labelabstand, Reihenfolge links/rechts, Breite, Grid und äußere Abstände. Das
betrifft Login, Benutzeranlage/-bearbeitung, Konfiguration und Anbieter,
2FA-Regeln/-Wiederherstellung, Ticketbeobachtung und Benachrichtigungsentwurf.
Dort keine erneuten Kopien von Bahn-/Griffmaßen, Zustandsfarben oder Fokusrahmen
anlegen. Die bisherigen Grundregelkopien sind entfernt; die Anordnungen bleiben lokal.

**Zeitfelder:** Platzhalter im Zahlenanteil sind über `cosmos-duration-field.css`
ausdrücklich mit `--cosmos-input-placeholder-color` gedämpft, auch im präzisen
`BigDecimalField`. Der Standardwert als Platzhalter bleibt von einer tatsächlichen
Eingabe unterscheidbar; Leeren setzt keinen neuen Zahlenwert.
Aufrufer verwenden `CosmosDurationCustomField`. Die Hülle trägt
Label sowie Enabled/Read-only/Invalid; in der Konfiguration stehen Fehlertexte
in der Notification statt unter dem Feld.
`CosmosDurationField` liefert innen Zahl und Einheit.

- Speicherformate bleiben Sekunden, ISO-Duration oder ganze Tage.
- Laden wählt die größte ganzzahlig passende Einheit. Einheitenwechsel erhält die
  Zahl; Nachkommastellen werden nicht unbemerkt gerundet.
- Leerwerte behalten den Fachvertrag des Aufrufers: Standardwert, unbegrenzt oder Pflichtfeld.
- Feste Tageswerte haben nur ein Suffix. 2FA-Inaktivität erlaubt Minuten, Stunden und Tage.
- Zahl und Einheit teilen eine Fläche, ohne zweiten inneren Fokusrahmen.
- Der Plusbutton der präzisen Eingabe gehört direkt ins Suffixlayout, ohne eigenen
  Suffix-Slot innerhalb dieses Layouts.
- Die Einheit ist per ARIA beschriftet. Leere Label-/Baselineflächen dürfen sie nicht
  nach unten verschieben; Unterlängen müssen sichtbar bleiben.

Native Switches behalten ihr Grid-Layout. Flex kann die Klickfläche des inneren
Inputs auf Breite null reduzieren.

### Details und Bestätigungsdialoge

Details-Schalter bleiben bei Hover und Klick ohne zusätzliche Fläche. Handzeiger,
Pfeilwechsel, `aria-controls`, `aria-expanded` und Tastaturfokus bleiben erhalten.
`DetailsToggleButton` vermittelt den Zustand der nativen `Details`.

`PortalConfirmationDialog`, die Sammel-Lesebestätigung und administrative
Benutzerlöschung verwenden `ConfirmDialog`. Formular-/Identitätsdialoge bleiben `Dialog`s.

- Close, Escape und Abbrechen verwerfen die Aktion. Außenklick schließt normale
  Bestätigungen; beim Formularschutz bleibt er ohne Wirkung.
- Nur das Bestätigungsereignis führt den erfassten Auftrag einmal aus.
  Schließereignisse dürfen weder die folgende Bestätigung vorzeitig verwerfen
  noch einen erneut geöffneten Dialog zurücksetzen.
- `ConfirmDialogInitialFocus` setzt den Fokus am öffentlichen `vaadin-overlay-open`-
  Ereignis. `opened-changed` allein kommt dafür zu früh.
- Lange Inhalte scrollen innerhalb der maximalen Höhe; der Footer bleibt erreichbar.
- Eine fehlgeschlagene administrative Löschung öffnet denselben Kontext erneut.
  Erst eine neue ausdrückliche Bestätigung wiederholt den Versuch.

`EqualButtonWidths` gleicht sichtbare Textaktionen nach der längeren Beschriftung an.
Sprachwechsel, Sichtbarkeit und verfügbare Breite werden berücksichtigt; Iconbuttons
zählen nicht. Mobile Vollbreite wird am zuständigen Container gesetzt.

Beim Formularschutz steht auf Desktop Verwerfen links und Weiterbearbeiten rechts.
Bis 600 px stehen beide in dieser Reihenfolge untereinander in voller Breite;
Anfangsfokus ist Weiterbearbeiten. „Alle anderen abmelden“ nutzt ebenfalls mobile
Vollbreite. Der Browserhinweis bei F5/Tab-Schließen ist ein separater Schutz:
[Ungespeicherte Formularänderungen](anwendung.md#ungespeicherte-formularänderungen).

Mitteilungskarten verwenden einheitliche Innenabstände: Gesamtübersicht und
geöffneter Dialog L, kompakte Glockenvorschau M. Der zusätzliche linke Statusrahmen
reserviert unabhängig vom Zustand dieselbe Breite, ohne das Padding umzurechnen.
Glockenvorschau, Gesamtübersicht und Detaildialog zeigen Titel, Nachricht und
Metadaten in dieser Reihenfolge. Übersicht und Vorschau nutzen dasselbe Raster:
Titel und Nachricht links, Werkzeuge rechts über beide Zeilen, Metadaten darunter
über die Kartenbreite. Dadurch erhöht die Werkzeughöhe nicht den Abstand zwischen
Titel und Nachricht. Die Werkzeuge bleiben auch mobil rechts oben; lange Titel
umbrechen neben ihnen. Der native
Titelbutton behält Flexdarstellung statt des früheren lokalen Blocklayouts,
damit seine Shadow-DOM-Slots keinen zusätzlichen Textzeilenraum erzeugen.
Die sichtbare Wirkung ist noch manuell zu beurteilen; XL ist keine Voreinstellung.

Die Nachrichtenkarte im geöffneten Dialog ist eine fachliche `Div`-Zusammensetzung.
Ihr äußerer Innenabstand stammt aus `cosmos-notifications.css`, nicht aus Aura oder
der nativen Card. Er ist einheitlich L. Für Übersicht und Detaildialog gilt:
Titel zur Nachricht S, Nachricht zu den Metadaten M. Die kompakte Glockenvorschau
verwendet entsprechend XS und S. Zusätzliche Layoutgaps und untere Nachrichtenränder
werden nicht addiert. Nach den Metadaten bleibt es beim äußeren Kartenpadding.
Frühere lokale 14-/20-px-Metadatenabstände sind entfernt. Die Werte
gelten unabhängig von der Fensterbreite; der Dialogaußenraum bleibt getrennt.

„Als gelesen markieren“ in einer geöffneten Nachricht ist ein nativer Primary-Button.
Er übernimmt Schrift, Höhe und Innenabstände der gemeinsamen Cosmos-Buttonregeln;
keine lokale Inline-/Tertiary-Sondergestaltung. Die Aktionszeile bleibt rechtsbündig
innerhalb des Kartenpaddings.

Die kompakten Benachrichtigungskarten richten ihren Titelbutton linksbündig zum
Text aus. Ihre linke Linie zeigt unabhängig von der Ticketrelevanz den Lesestatus:
Blau bei ungelesen, sekundäres Grau bei gelesen. Fachliche Statusbadges und
Aktionssperren bleiben davon getrennt (`cosmos-notifications.css`).

### Globale Notifications

Alle globalen UI-Meldungen verwenden `NotificationHelper`. Der angehängte Seitenrahmen
bestimmt die Palette; Supplier halten Texte beim Sprachwechsel aktuell.

| Methode | Inhalt |
| --- | --- |
| `showTextNotification` | Nativer Paragraph; ohne Nachricht nur der Titel |
| `showTextNotificationWithDetails` | Text mit nativer Liste |
| `showHighlightedTextNotification` | Erstes exaktes Vorkommen eines gelieferten Texts hervorheben |

Die Methoden werten kein HTML aus. Fehlt die Hervorhebung, bleibt normaler Text.
Schweregrad, Symbole, Dauer und Fensterisolation bleiben erhalten.
URL-Rückmeldungen erst nach der Navigation öffnen, damit der Zielrahmen feststeht.
Manuelles Laden meldet Erfolg oder Fehler; automatisches Laden bleibt ohne Erfolgstoast.

Notification-Überschriften verzichten auf abschließende Ausrufe- und Fragezeichen.
Die gemeinsamen Titel lauten „Erfolgreich“/„Fehlgeschlagen“ beziehungsweise
„Successful“/„Failed“. Statusseiten mit Erfolgs-/Fehlersymbol verwenden ebenfalls
diese Titel. Nachrichtensätze, Links wie „Passwort vergessen?“ und echte Fragen
in Bestätigungsdialogen behalten ihre bisherigen Satzzeichen.

Nachrichtentexte können gezielte Zeilenumbrüche aus dem Textkatalog enthalten;
`cosmos-notification-message` erhält sie mit `white-space: pre-line`. Zum Beispiel
trennt die Passwortmeldung der Benutzerverwaltung die Fehlerbeschreibung von den
Passwortanforderungen. Keine automatische Trennung nach jedem Satz und kein HTML
im Nachrichtentext.

Die Notifications im Cosmos-Referenzstil verwenden den nativen Aura-Innenabstand
der äußeren Karte. Der Inhaltsbereich erhält kein zusätzliches Padding; seine
Anordnung und die Cosmos-Farben, Rahmen und Icons bleiben separat gestaltet.

Globale Meldungen erscheinen ab 768 px Fensterbreite nativ oben rechts
(`TOP_END`), darunter unten mittig (`BOTTOM_CENTER`). Vaadins `windowSizeSignal`
und ein an die konkrete Notification gebundener `Signal.effect` folgen auch
Größenänderungen eines offenen Fensters. Bei noch unbekannter Fensterbreite gilt
die untere Position. `cosmos-notification-top` koppelt den oberen Abstand an
`--cosmos-topbar-height`; untere Meldungen reservieren keinen Headerabstand.
Die nativen Bildschirmrandabstände berücksichtigen die Safe Area.
Am Desktop erhält der native `vaadin-notification-container` zusätzlich einen
rechten Abstand von `--cosmos-space-2xl` (24 px), mindestens jedoch die rechte
Safe Area. Damit bleibt Platz für eine Seiten- oder AppLayout-Scrollleiste. Die
Reserve gilt auch ohne sichtbare Scrollleiste; eine zusätzliche JavaScript-Messung
ist nicht nötig. Oberer und unterer Abstand sowie die Smartphoneposition bleiben
unverändert. Zuständig ist `cosmos-theme.css`.

Statussymbole im Cosmos-Referenzstil: Information als blauer Info-Kreis, Erfolg
als grüner Haken im Kreis, Warnung als gelbes Dreieck mit Ausrufezeichen und Fehler
als rotes Kreuz im Kreis. Dafür verwendet der Helper `INFO_CIRCLE`, `CHECK_CIRCLE`,
`WARNING_TRIANGLE` und `ERROR`. Dieselbe Zuordnung gilt für die Statusanzeigen bei
E-Mail-Verifikation, Passwort-Reset und E-Mail-Änderungsbestätigung: abgelaufen
als Warnung, ungültig oder technisch fehlgeschlagen als Fehler. Feste Sicherheits-,
Fristen- und Neustarthinweise verwenden ausdrücklich `WARNING_TRIANGLE` und
behalten ihre freigegebenen Farben, einschließlich roter Löschwarnungen.
`CLOSE_CIRCLE` bleibt das Symbol für Schließen-Aktionen.

## Seitenrahmen und Navigation

### Übernahme des angemeldeten Benutzerportals

`HomeView`, `UserNotificationsView` und `AccountView` nutzen `cosmos-reference-portal`.
`PageFrameComponent` stellt den Servicekopf, `PageContentLayout` Überschrift und Inhalt
bereit. Der Kopf bleibt oben; übriger Inhalt und Copyright scrollen im Dokument.
`PortalSidebarPageLayout` ergänzt Identitätskarte und Kontonavigation.

`PageNavigationComponent` nutzt `SideNav`/`SideNavItem`; die aktive Seite trägt
`aria-current="page"`. Mobile Navigation überlagert den Inhalt und schließt bei
Seitenwahl, Außenklick oder Escape. Dialoge behalten ihr eigenes Escape-Verhalten.
Startseite/Dashboard bieten echte Navigation, keine Beispielstatistiken.
Leere Benachrichtigungslisten und Ladefehler bleiben unterschiedliche Zustände.

### Besucherportal in Vaadin

`PublicPageComponent` und `PublicPanelComponent` bilden den gemeinsamen Rahmen für
öffentliche Formulare und Linkzustände. Gestaltung: `cosmos-public.css`;
Anbieter-Schnellzugänge: `cosmos-external-login.css`.

- Gemeinsamer Servicekopf, DE-/EN-Auswahl, Inhaltsrahmen und Copyright.
- „Login speichern“ ist ein nativer Switch, keine Checkbox.
- Registrierung erlaubt Autovervollständigung beider E-Mail-Felder (`email`);
  beide Passwortfelder nutzen `new-password`. Passwortmanager können Hinweise übergehen.
- Ausstehende Verifikation und Reaktivierung zeigen eine beschriftete Adresszeile,
  gelbes Badge und Warnung mit hervorgehobenem Spam-/Junkhinweis.
- Gültige, abgelaufene und ungültige Links behalten unterschiedliche Texte,
  Farben und tatsächlich verfügbare Aktionen. Keine Zustellbestätigungen erfinden.

Statische HTML-Referenzen sind keine laufende zweite Oberfläche. Sie liegen bei
Bedarf im Git-Tag `archiv/html-referenzen`; neue Entwürfe gehen von Vaadin aus.

### Administration in Vaadin

`AdminView` nutzt `cosmos-reference-admin`; Header und Menü sind dieselben
Portalbausteine. Adminregeln bleiben auf ihren Scope begrenzt. Navigation und
Panelköpfe verwenden dieselbe `ConfigGroupIcons`-Zuordnung.

**Benutzerverwaltung:** natives Grid, Werkzeugleiste und Pagination. Filter sind
Entwürfe: Anwenden übernimmt, Zurücksetzen ändert den Entwurf, Abbrechen verwirft ihn.
Der eingeklappte Bereich Anmeldung zeigt Registrierungsweg, Anmeldewege,
E-Mail-Nachweis und Kontotyp. Standardkonten sind blau, Entwicklerkonten systemviolett.
Selbstdeaktivierung erscheint als gelber Warnhinweis mit Trennlinie und Löschtermin.
Die administrative Sperre ist davon unabhängig.

**Konfiguration:** Die Feldhilfe zeigt Beschreibung, Standardwert, Regeln und
„Wann gilt die Änderung?“. Vertrauliche Werte bleiben verdeckt; die Hilfe speichert
nichts. Laden/Speichern stehen im äußeren Footer, bis 600 px untereinander in voller
Breite, auch bei Anbietern, Queue und 2FA.

**Mailtest:** native Eingaben und dieselbe Feldhilfe. Die Nachricht ist vertikal
vergrößerbar; Klartexthinweis und SMTP-Navigation stehen getrennt darunter.
Fehler sind Fehlermeldungen, keine erfolgreichen Testergebnisse.

### Seitenpfade und Navigationsziele

Die blauen Elternpfade und der große aktuelle Seitentitel sind wie folgt zugeordnet.
Untertabs bleiben Tabs und werden nicht zusätzlich als Breadcrumb oder Seitentitel
wiederholt. Alle Beschriftungen folgen dem gemeinsamen DE-/EN-Textkatalog.
Der manuelle Breadcrumb-Modus berücksichtigt die vorhandenen Queryparameter.

| Bereich | Blauer Elternpfad | Große Überschrift |
| --- | --- | --- |
| Besucherseiten einschließlich öffentlicher Startseite, Anmeldung, Registrierung und Reset | `PORTAL` | Aktueller Seitentitel |
| Angemeldete Startseite | `PORTAL` | Startseite |
| Persönliche Daten, Sicherheit, Geräte & Sitzungen | `PORTAL / MEIN KONTO` | Jeweiliger Seitentitel |
| Eigene Benachrichtigungen | `PORTAL` | Benachrichtigungen |
| Admin-Dashboard, Benutzerverwaltung, Tickets und Benachrichtigungen | `ADMINISTRATION` | Jeweiliger Seitentitel |
| Sämtliche Konfigurationsgruppen einschließlich Anmeldeanbieter und 2FA | `ADMINISTRATION / KONFIGURATION` | Gewählte Gruppe |
| Mailtest | `ADMINISTRATION` | Mail Test |
| 2FA, Wiederherstellung und Hilfe aus dem Konto | `PORTAL / MEIN KONTO / SICHERHEIT` | Aktueller Schritt |
| 2FA beim Login, ausstehende Verifikation und Reaktivierung | `PORTAL` | Aktueller Schritt |
| Verifikations-, E-Mail-Änderungs- und Reaktivierungslinks | `PORTAL` | Aktueller Seitentitel |


Portal führt zur Startseite, Mein Konto zu `account?section=profile` und Sicherheit
zu `account?section=security&tab=portal`. Administration führt zum Dashboard,
Konfiguration zur ersten für den Benutzer zulässigen Gruppe. In eingeschränkten
Sicherheitsabläufen bleiben alle Pfaditems ohne Navigationsziel; die expliziten
Zurück-/Abmeldeaktionen und serverseitigen Sperren bleiben maßgeblich.

## Fachoberflächen

### Externe Anmeldebausteine

`PublicAuthenticationPanelLayout` verbindet lokales Formular, ODER-Trenner und den
optionalen Schnellzugang aus `ExternalLoginButtons`. Ohne verfügbare Anbieter bleibt
nur das lokale Formular. Bei Platzmangel stehen die Panels untereinander.

`ExternalConnectionsComponent` nutzt Sicherheitskarten mit Anbieterlogo, Status
und Aktion. `ExternalIdentityConfirmationDialog` liegt außerhalb der Tabinhalte,
damit ein inaktiver Tab ihn nicht sperrt. Die letzte nutzbare Anmeldung bleibt geschützt.
Details: [Externe Anmeldung](externe-anmeldung.md).

Die Anbieterkonfiguration nutzt native TabSheets, Felder, Switches und Feldhilfen.
Tabwechsel speichern/laden nicht. Statusbadges zeigen den gespeicherten Zustand,
nicht den Entwurf. Gespeicherte Secrets bleiben im Feld leer und in der Hilfe verdeckt.
`cosmos-external-login.css` und `cosmos-provider-settings.css` gestalten Anordnung
und zugelassenes Branding aus `icons/external-login/`.

### Administrative Mailqueue-Bereinigung

| Queue-Reiter | Sichtbarkeit | Inhalt / Zähler |
| --- | --- | --- |
| Allgemein | Admin und Superadmin | Fünf normale Queue-Einstellungen |
| Assistenten | Nur Superadmin | Vier Bereinigungseinstellungen und Ausführung |

`tab=general`/`tab=assistants` innerhalb von `section=configuration&group=queue`
erhält die Auswahl bei F5. Tabwechsel bewahren Entwürfe; ungültige Parameter oder
fehlende Rechte führen zu Allgemein. Der Kopfzähler zählt den aktiven Reiter,
auch deaktivierte Felder. Beide Reiter werden gemeinsam gespeichert.

Die innere `CosmosCardComponent` hat einen blauen Brush im Kopf. Automatik und
Historienfrist stehen vor den Zeitfeldern; darunter Warnung, Ausführung und Info-Box.
Laden/Speichern bleiben im äußeren Footer. Gestaltung: `cosmos-mailqueue-assistant.css`.

- Äußere Startaktion und Dialogtitel: „Manuelle Bereinigung“.
- Bestätigungsdialog: roter Brush, gelbe Warnbox „Späterer Empfang möglich:“ mit
  Warndreieck, gemeinsame rote Bestätigungsaktion.
- Status: neutral vor dem ersten Lauf; gelb bei Warten, Lauf oder Teilergebnis;
  grün bei Abschluss; rot bei Fehler. Die Vormerkungs-Notification bleibt blau mit Info-Icon.
- Laufzeitpunkt und bereinigte Gesamtzahl stehen in getrennten Monospace-Zeilen.
- Während aktiver Aufträge ist die Startaktion deaktiviert. Vorübergehendes Polling
  stellt danach den vorherigen Abstand wieder her.

Umfang, Paketgrenzen und Historie: [Mailqueue](mailqueue.md).

### Segmentierte Auswahl mit nativen Tabs

`CosmosSegmentedTabs<T>` verbindet native Tabs mit einem typisierten CustomField.
Profilbildmodus und Benachrichtigungs-, Ticket- und Zugangsfilter verwenden ihn.
Icons/Zähler bleiben native Tabinhalte. Textupdates bewahren Tabinstanz, Wert und Fokus.

Der OTP-Stil steht in `cosmos-segmented-tabs.css`; die Glockenvorschau hat eine kompakte
Variante. App-Code/Ersatzcode, QR/Schlüssel und Antwort/Vermerk teilen die Gestaltung.
Normale Seitenreiter und RadioButtonGroups bleiben davon getrennt.
Alle Reiterleisten behalten bei Platzmangel die native horizontale Navigation mit
Pfeilen – mobil und auf schmalem Desktop. Kein Zusammendrücken oder mehrzeiliges Tab-Raster.

### Zwei-Faktor-Bereiche

`cosmos-mfa*.css` kapselt 2FA, Wiederherstellung und Tickets. Panels, Eingaben,
Buttons und Statusfarben verwenden die gemeinsamen Bausteine.

#### Einrichtung und Ersatzcodes

- Der reduzierte Header zeigt Initialenavatar und Identität, kein Profilbild,
  Benutzermenü oder Statuspunkt.
- QR-Code/manueller Schlüssel sind native Tabs. Der Schlüssel ist eine
  schreibgeschützte TextArea; Codes verwenden Space Mono.
- Geschützte No-Store-Abrufe setzen Schlüssel/Ersatzcodes nur ins öffentliche
  `inputElement`. Hostwert und serverseitiges Flow-Modell bleiben leer;
  Geheimnisse gehören nicht in den JDBC-Sitzungszustand.
- `TwoFactorCodeInput` verbindet sechsstelliges OtpField und Ersatzcodefeld.
  Ein vollständiger OTP bestätigt die wiederkehrende Abfrage automatisch.
  In Aktionsdialogen gilt das nur beim Verbinden eines Anbieters; sonst bleibt
  die ausdrückliche Bestätigung erforderlich.
- `cosmos-mfa-otp.css` wird in die OTP-Schattenwurzel geladen. Den Eingabewert nicht
  zusätzlich über den Kästchen zeichnen; native Bedienung und Fehlerzustände erhalten.
- `RecoveryCodeDownload` nutzt einen nativen Button mit DownloadHandler und prüft
  Status, PDF-Inhaltstyp und Signatur. Fehler werden nicht als Datei heruntergeladen.
- Kopieren/PDF verbrauchen die Ausgabe nicht. Erst „Codes gesichert“ mit bestätigtem
  Switch verwirft sie. Sie bleibt bis dahin oder bis zum Ende der berechtigten Sitzung verfügbar.

#### Ticketsystem

Antragsteller sehen eigene Nachrichten und anonymisierte Antworten der Systemadministration.
Die berechtigte Adminansicht darf tatsächliche Autoren, Zuständigkeit und interne Vermerke
zeigen. Diese Grenze gilt auch für Tooltips.

Vorgangsliste, Details und Verlauf nutzen die vorhandenen Cards und nativen Scroller.
Bei schmalem Inhalt stehen Liste und Details untereinander. Maximieren blendet nur die
Liste aus; Filter, Auswahl und Antwortentwurf bleiben erhalten.

Die eigene Ticketliste in der öffentlichen Wiederherstellungsanfrage verwendet
`VirtualList` mit höchstens fünf sichtbaren Einträgen. Bei weniger Tickets schrumpft
sie; ohne Tickets erscheint der vorhandene Leerzustand. Die Dienstgrenze von
höchstens 20 eigenen Tickets bleibt erhalten. Native Buttons öffnen den jeweiligen
Verlauf per Klick oder Tastatur und kennzeichnen die aktuelle Auswahl über
`aria-pressed`. Strg+Klick auf das ausgewählte Ticket hebt die Auswahl auf;
der Antwortentwurf bleibt beim erneuten Öffnen erhalten. Nachladen und
Live-Aktualisierungen lassen eine bewusst aufgehobene Auswahl leer.
Stabile Ticket-IDs und ein aktualisierbarer Renderer erhalten die
Buttonidentität bei Datenänderungen. Die Liste verwendet die gemeinsamen
Scrollleistenregeln und die bisherigen Ticketfarben.

Geometrie: `support-user-ticket-list` / `support-user-ticket-button` in
`cosmos-mfa-confirmation.css`. Pro Zeile 64 px, unter 640 px Fensterbreite 112 px
für umgebrochene Inhalte; beide Höhen sind aus vorhandenen Zeilenhöhen und
Abständen abgeleitet. Die Adminliste bleibt ein Grid mit ihrer Pagination.
Der Button-Vorspann füllt die gesamte Ticketzeile ohne den sonst üblichen
Iconabstand aus, damit Auswahlfläche und Trennlinien bis an die Ränder reichen.
Die Adminliste verwendet für die einfachen Zeilentrenner `--cosmos-border-color`,
wie die Trennlinien innerhalb der Ticketkarten.

- Öffentliche Antworten und interne Vermerke nutzen native Tabs; Vermerke tragen die Systempalette.
- Filter werden erst mit Anwenden übernommen. Direkte Ticketsprünge heben entgegenstehende
  Filter auf; erfolglose Sprünge behalten die Auswahl.
- `tab=all`/`tab=recovery` und Ticketnummer bleiben ohne Formularnavigation in der URL.
- `RecoveryInstructionsComponent` nutzt native Layouts, nummerierte Badges und Details.
  Nachrichten sind Text, kein HTML.
- Der Sperrgrund von „Anleitung senden“ nutzt einen nativen Markdown-Tooltip am
  `HorizontalLayout` um den Button (`support-instructions-target`), damit Maus und
  Tastatur ihn auch beim deaktivierten Button erreichen. Das Layout übernimmt
  ausschließlich die bisherige Position; Button- und Tooltipgestaltung bleiben zentral.
  Flow hängt freie Tooltips direkt an die UI. Die vorhandene Zuordnung über das öffentliche
  native Ziel ergänzt deshalb beim Anhängen die lokale Klasse `support-instructions-tooltip`.
  `cosmos-mfa-confirmation.css` entfernt damit die zusätzlichen Markdown-Absatzränder
  und den formatierten Abschlussumbruch; der gemeinsame Tooltip-Innenabstand bleibt erhalten.
  Während Mailauftrag oder Wartezeit sperren, aktualisiert natives UI-Polling den
  Status und wird bei Freigabe beziehungsweise Verlassen wieder entfernt.
- Listen-/Verlaufsgeometrie bleibt lokal; globale Scrollregeln liefern die Leisten.

Fachliche Zustände, Zähler und Beobachtung:
[Ticketregeln](zwei-faktor-authentifizierung.md#ticketzustände-zähler-und-glocke).

#### E-Mails und Druckvorlagen

Kontomails verwenden MJML und erzeugtes HTML im Cosmos-Stil. Inter wird über die
lokale `email-fonts.css` eingebunden, ohne externen Schriftdienst. Empfänger müssen
die Basisadresse erreichen können; ohne Webfonts greifen Ersatzschriften.
`fonts: {}` verhindert zusätzliche MJML-Standardimporte.

`email/partials/action-link.css` gestaltet vollständige Links in einer gemeinsamen Box.
URL und Kopierhinweis bleiben lesbar; kein Clipboard-JavaScript in E-Mails.
Die PDF nutzt Classpath-Vorlagen unter `pdf/`, eingebettete Schriften und hellen Druckhintergrund.

## Icons und Identität

### Icon und Text in Aktionsbuttons

Button-Präfix und Text haben im Portal und seinen Overlays 8 px Abstand.
Leere Präfixe/Suffixe reservieren keinen Platz. Iconbuttons und Identitätszeilen
behalten ihre Maße. Dafür keine globalen Avatar-/Buttonregeln verändern.

### Gemeinsames Lucide-Iconset

`AppIcon` liefert native SvgIcons aus lokalen Lucide- und freigegebenen Tabler-Dateien.
Icons beschrifteter Aktionen sind dekorativ (`aria-hidden`); Iconbuttons brauchen
zugängliche Namen und gegebenenfalls Tooltips.

- Lucide ist das reguläre Set; Tabler ergänzt gewählte Motive wie Cloud-Lock,
  Formularschutz, Schrittnummern und Ticket-/Terminalsymbole.
- SVGs und Lizenzen liegen unter `icons/lucide/` und `icons/tabler/`; kein CDN oder zusätzliches Icon-JavaScript.
- Native Komponentenicons bleiben grundsätzlich erhalten. Der Dauerbaustein hat
  eigene freigegebene Lucide-Masken für Löschen und Schrittaktionen.
- Logo, Anbieterzeichen und Illustrationen sind eigenständige Ressourcen.
  Die Sprachwahl verwendet DE/EN, keine Flaggen.

### Avatare und Overlays

`CosmosAvatar` liefert den nativen Bild-/Initialenkern, `OverlayUserIcon` ergänzt
Status, Tooltip, geschützte Bildressource und Profilereignisse. Header, Konto,
Menü, Tabelle und Tickets nutzen denselben Kern mit lokalen Größen.

- Ohne Namen erscheint das Benutzersymbol; bei Bildfehlern übernimmt Vaadin den Initialen-Rückfall.
- Anwesenheit ist ein gefüllter grüner, roter, gelber oder grauer Punkt. Seine Mitte
  liegt größenabhängig bei 45 Grad auf der Avatar-Kreislinie. Keine doppelten Ringe.
- Statusauswahl und Avatarpunkt sind getrennte Darstellungen.
- Der Bildeditor bleibt die freigegebene Canvas-Sonderlösung mit nativen Uploadaktionen.

**AvatarGroup:** Die Empfängervorschau nutzt Initialen ohne zusätzliche Bildabfragen.
Bis sechs Empfänger alle anzeigen; ab sieben fünf Avatare plus „+N“. Früherer Überlauf
bei Platzmangel bleibt nativ. Rahmen/Farben entsprechen Cosmos, die Namen-Tooltips bleiben.
Die gesamte Reihe öffnet per Klick, Enter oder Leertaste die vollständige Liste,
auch ohne „+N“. `NotificationRecipientPopover` setzt diese aus nativem Popover,
Suchfeld und VirtualList zusammen: Name, darunter sekundäre E-Mail; Suche über beide Werte,
keine Zeilenauswahl. Der Kopf verwendet Personen-Icon und Schließen-Button im
gemeinsamen Dialogstil. Unter „Empfänger“ steht der übersetzte Untertitel
„Auswahl prüfen und durchsuchen.“ mit der gemeinsamen `cosmos-panel-description`-
Gestaltung; eine Trennlinie trennt den Kopf von Suche und Liste auf der
Content-Fläche. Header, Außenrahmen und Content übernehmen die Farben der
Benachrichtigungsvorschau: `--cosmos-raised-background`, `--cosmos-control-border-color`
und `--cosmos-page-background`; die Trennlinie nutzt `--cosmos-border-color`.
Das Suchfeld übernimmt die Benutzerfilter-Geometrie: 40-px-Feld,
13-px-Eingabeschrift, 14-px-Lupe und M-Innenabstand. Die Breite beträgt 380 px und wird bei
kleineren Fenstern auf die verfügbare Breite mit seitlichem L-Abstand begrenzt.
Die innere Hülle füllt diese Breite aus. Die Liste verwendet wie die Ticketlisten
eine einheitliche Fläche aus `--cosmos-base-color`, ohne wechselnde Zeilenfarben.
Native Buttons zeigen Handzeiger sowie die gemeinsamen Hover- und Fokuszustände.
Klick, Enter oder Leertaste wählen genau einen Empfänger visuell aus:
Tabellen-Hervorhebungsfarbe, linke Akzentlinie und `aria-pressed` kennzeichnen ihn.
Strg+Klick auf den ausgewählten Eintrag hebt die Auswahl auf. Suche und Scrollen
erhalten die Auswahl; die Versandempfänger bleiben unverändert. Es gibt noch
keine weitere Aktion für die ausgewählte Person. Eine Touch-Bedienung zum
Abwählen ist im TODO offen. Die Liste zeigt höchstens fünf 60-px-Zeilen; bei weniger
Treffern schrumpft sie. Ohne Treffer erscheint ein zentrierter Leerzustand auf
Content-Hintergrund: blaues Tabler-Icon `user-question`, Ergebnistitel und sekundärer
Hinweis zum Anpassen der Suche. Schrift- und Icongrößen entsprechen dem kompakten
Leerzustand links in der Admin-Ticketliste. Die Zeilenhöhe wird aus
Zeilenhöhen-/Abstandstokens abgeleitet. Weitere Zeilen scrollt und virtualisiert
die VirtualList; bei kleinen Fenstern darf die sichtbare Liste weiter schrumpfen.
Ihr Scrollbereich liegt am öffentlich gestaltbaren Host; die bestehenden Regeln
aus `cosmos-scrollbars.css` greifen dort direkt. Keine zusätzlichen Scrollbar-Regeln
oder Zugriffe auf interne Grid-Elemente verwenden.
Der freigegebene, gekapselte Listener in `notification-recipient-popover.js` fängt
Klick/Tastatur vor dem nativen Überlaufmenü ab. Er nutzt nur DOM-Ereignisse und
öffentliche Light-DOM-Slots, keine privaten Vaadin-Methoden. „Details“ entfällt.
Gestaltung: `cosmos-notifications.css`, Klassen `cosmos-recipient-*`.

### Tooltips

Native Tooltips verwenden eine dunkle Fläche ohne Pfeil, feinen Rahmen und kleine
helle Schrift. Tokens: `--cosmos-tooltip-*`; Gestaltung: `cosmos-theme.css`.
Die rechte Feldhilfe ist ein eigener Baustein.

Name/E-Mail im Header, in Profilkarte, Benutzermenü und Löschbestätigung werden nach
verfügbarer Breite begrenzt. Normal ist eine Zeile mit Ellipse; im mobilen Löschdialog
höchstens zwei Zeilen je Angabe. Gespeicherte Werte werden nicht gekürzt. Tooltips zeigen
vollständige Werte bei Hover oder Tastaturfokus.
Eine allgemeine Touch-Lösung ist nicht eingebaut; die offene Entscheidung steht im [TODO](../TODO.md).
Reine Avatare bleiben Anzeigeelemente; interaktive Container geben ihren Zeiger vor.

## CSS-Organisation

### Ersatzklassen für frühere Lumo-Varianten

`CosmosCssClasses` stellt die Java-Konstanten bereit; die Regeln stehen in
`cosmos-utilities.css`.

| Konstante | Klasse | Wirkung |
| --- | --- | --- |
| `INLINE_BUTTON` | `cosmos-inline-button` | Textartiger Button ohne Fläche, Innenabstand und eigene Höhe; Schrift des Umfelds. Die Akzentfarbe ist nur ein Standard ohne Spezifität (`:where`), lokale Regeln wie weiße Secondary-Texte gehen vor; Auras Tertiary-Innenabstand ist über `--vaadin-padding-s: 0` neutralisiert |
| `ICON_BUTTON` | `cosmos-icon-button` | Quadratischer Button in Controlhöhe ohne Innenabstand |
| `EQUAL_WIDTH_TABS` | `cosmos-equal-width-tabs` | Reiter teilen sich die verfügbare Breite gleichmäßig |
| `TEXT_PRIMARY` / `TEXT_SECONDARY` | `cosmos-text-primary` / `-secondary` | Akzent- bzw. Sekundärtextfarbe |
| `TEXT_SUCCESS` / `TEXT_WARNING` / `TEXT_ERROR` | `cosmos-text-success` / `-warning` / `-error` | Statustextfarben |

Die Datei lädt direkt nach `cosmos-theme.css`, damit diese Klassen die gemeinsamen
Komponentenregeln gleicher Spezifität ergänzen. Abstände und Hervorhebung früherer
Utility-Klassen in Systemmeldungen und Konfigurationsfeldern stehen ebenfalls dort.

### Aura-Abgleich und akzeptierte Abweichungen

`cosmos-aura.css` lädt direkt nach den Tokens und setzt Aura-Standards zurück, die von der
Cosmos-Gestaltung abweichen: freistehende Reiter statt gefüllter Segmentleiste,
kleinerer Aufklapppfeil der Seitennavigation, nativer Pflichtfeldindikator direkt hinter dem Label,
Feldsymbole mit 16 px breiter Symbolfläche (Klickfläche bleibt 24 px), Aufklappbereiche
ohne Einrückung um die Pfeilbreite, Kalender (Datumsauswahl) im Cosmos-Stil mit
dunkler Kalenderfläche, 36 px hohen Tagen (am Desktop auch 36 px breit,
mobil über die verfügbare Kalenderbreite verteilt), „heute“ als Akzentschrift ohne
Kreis, gemeinsamer Hoverfläche, ausgewähltem Tag in `--cosmos-primary-color`, kleiner
Jahresspalte mit leicht abgedunkeltem Hintergrund, feiner Trennlinie und nativer seitlicher
Markierung. Die Jahresspalte verwendet `--vaadin-date-picker-year-scroller-background`
und `--vaadin-date-picker-year-scroller-border-color`; ihre Auswahl bleibt ohne zusätzliche
Markierungsfläche. „Heute“/„Abbrechen“ sind native Tertiärbuttons ohne Rahmen oder Füllung
in `--cosmos-primary-text-color`. Fokus und Hover bleiben nativ.
Weitere Anpassungen: Schließen-Buttons (`cosmos-overlay-close`) mit mittigem 18-px-Symbol
ohne Abstand zum leeren Textbereich, Cosmos-Auswahlzeilen (`cosmos-reference-option`) ohne
Auras Fokusrahmen, Innenabstand und Hover-/Active-Fläche auf dem Eintrag (die einzige
Fläche zeichnet die Zeile), Feldfehler in 13 px/16 px mit `--cosmos-error-text-color` ohne
Auras Warnsymbol (die Variablen stehen zusätzlich an Elementen mit `theme`-Attribut, weil
Aura die Fehlerfarbe dort direkt setzt) sowie eine kompakte Seitengrößenauswahl mit 4 px
Abstand zum Pfeil. Textlinks in `.two-factor-help-row` zeigen beim Überfahren nur hellere Schrift
ohne Fläche (`cosmos-mfa-confirmation.css`).

Reine Icon-Buttons (Symbol ohne Slot, also ohne Text) und `cosmos-icon-button` setzen
`--vaadin-button-gap: 0`: Vaadin legt das Symbol in den Textbereich, Auras Abstand zum
leeren Präfix würde es sonst 4 px aus der Mitte schieben. Overlays verzichten über
`--aura-overlay-outline-shadow` und `--aura-overlay-surface-opacity: 1` auf Auras Umrissring
und die halbtransparente Fläche; den Rahmen zeichnet Cosmos selbst. Checkbox-Chips mit
absolut gelegtem Label (Benutzerfilter) heben die Rasterzelle des Label-Parts auf, damit
das Label senkrecht mittig in der ganzen Fläche steht.
Spätere Bausteine und Fachbereiche dürfen diese Grundwerte gezielt überschreiben.

Bei der Regelbereinigung sind lokale Anordnungen von gemeinsamen Grundregeln zu
unterscheiden: Überdeckte Werkzeugleisten-, Karten- und Kontoeintrag-Regeln entfallen,
wenn die zuständige Fachgestaltung sie über alle relevanten Breiten ersetzt.
Button-, Feld-, Dialog- und Avatargrundregeln sowie von Java verwendete Utilities
sind anhand ihrer Aufrufer, Zustände und nativen Varianten zu beurteilen. Eine
Kaskadenaufnahme ohne Gewinn allein beweist ihre Entbehrlichkeit nicht.
Insbesondere bleibt der Inhaltsabstand von
`.portal-menu-presence-details` zusammen mit der nativen Variante `AURA_NO_PADDING`
erhalten; die Variante und die ergänzende Cosmos-Regel wirken gemeinsam.

Zwei Vaadin-Basisregeln sind beim Gestalten zu beachten: Schalter (`vaadin-switch`)
behalten ihr Grid-Layout (`grid`/`inline-grid`, nicht `flex`), sonst erhält das native
`input` die Breite 0 und ist nicht mehr klickbar. Felder im Suffix eines anderen Felds
(Zeiteinheit im Dauerfeld) blenden das Baseline-Pseudoelement `::before` aus, sonst
reserviert es eine Labelzeile und schiebt die Auswahl aus dem Feld.

Einheitlicher Deaktiviert-Zustand (vom Benutzer am 17.11.2026 freigegeben): Deaktivierte
Bausteine sind auf einen Blick erkennbar. Felder übernehmen Auras gedämpftes Label (halbe
Deckkraft) und Eingabefeld; Wert und Platzhalter erhalten `--cosmos-text-disabled-color`
(Regel neben der Eingabegrundfarbe in `cosmos-theme.css`). Checkboxen, Schalter, Radio-
Buttons und alle Button-Arten behalten Auras gedämpfte Darstellung. Lokale Ausnahmen, die
deaktivierte Felder wie aktive aussehen lassen, sind nicht vorgesehen.
Vom Benutzer akzeptierte Abweichungen durch Aura (kein pixelgenauer Nachbau):

- Eingabefelder sind teils 2 px niedriger.
- Feldsymbole stammen aus Aura (u. a. anderes Kalendersymbol).
- Überschriften brechen mit `text-wrap: balance` ausgewogener um.
- Checkboxen 16 statt 18 px; Buttons mit Aura-Innenabstand teils 2–4 px breiter.
- Textbereich und Mehrfachauswahl verwenden denselben dezenten Rahmen wie alle übrigen Felder.
- Primärflächen in `#0077b6` statt `#0088cc`, damit weiße Schrift den WCAG-Kontrast erreicht.
- Chips (Rollen, „Dein Benutzer“) mit Auras Innenabstand etwas höher; Tabellenzeilen mit
  Chips dadurch einige Pixel höher.
- Sortierpfeile der Tabellen größer und heller.
- Schließen-Symbol in Dialogen kräftiger; gestrichelter Rahmen schreibgeschützter Felder
  schwächer.

### Dateien und Ladereihenfolge

Basis: `app/src/main/resources/META-INF/resources/custom-theme/`. Jede Datei beginnt mit
einem kurzen Kopfkommentar zu ihrer Zuständigkeit. `Application` lädt die globalen
Schichten in dieser Reihenfolge; `CosmosThemeStylesheetsTest` sichert Reihenfolge,
Vollständigkeit und den Verzicht auf `@import`:

| Schicht | Datei | Aufgabe |
| --- | --- | --- |
| Basis | `Aura.STYLESHEET` | Vaadin Aura |
| Palette | cosmos-palette.css | Einzige Quelle der Farbwerte |
| Typografie | typography.css | Inter und Space Mono, Schriftfamilien |
| Tokens | cosmos-tokens.css | Semantische Farbrollen, Schrift-/Abstandsskalen, Maße, Rahmen, Flächen, Schatten |
| Aura-Abgleich | cosmos-aura.css | Zurückgesetzte Aura-Standards |
| Bausteine | cosmos-scrollbars.css | Gemeinsame Scrollleiste (Himmelblau-Strich) für Dokument, Container, Vaadin-Scrollflächen und Grid |
| | cosmos-duration-field.css | Integrierte Zeiteinheit im Dauerfeld |
| | cosmos-breadcrumbs.css | Native Seitenpfade |
| | cosmos-segmented-tabs.css | Segmentierte Auswahl mit nativen Tabs |
| | cosmos-theme.css | Formulare, Felder, Switch-Grundstyling, Buttons, Grids, Badges, Systemmeldungen, Anwendungsicons |
| | cosmos-utilities.css | Ersatzklassen aus `CosmosCssClasses` |
| | cosmos-cards.css | Hüllen für native Karten |
| | cosmos-avatar.css | Avatarkern und Statuspunkte |
| App-Rahmen | cosmos-shell.css | Appkopf, Inhaltsrahmen, Benutzermenü und Overlays |
| | cosmos-background.css | Hintergrundszene „Horizont mit Mondaufgang“ samt Szenenfarben |
| Fachbereiche | cosmos-admin.css | Adminnavigation, Panels, Konfiguration, Dashboard, Feldhilfe, Seitenpanels |
| | cosmos-notifications.css | Inbox, Vorschau, Detail, Versand und Archiv |

Fachliche Einzeldateien laden die zuständigen Views und Komponenten selbst per
`@StyleSheet`: `admin-users.css` (Benutzerverwaltung, Pagination), `cosmos-account.css`,
`cosmos-external-login.css`, `cosmos-public.css`, `cosmos-confirmation.css`,
`cosmos-email-verification.css`, `cosmos-metric-card.css`, `cosmos-mailqueue-assistant.css`,
`cosmos-provider-settings.css` und die `cosmos-mfa*.css`-Dateien.
Sonderwege: `cosmos-mfa-otp.css` wird von `TwoFactorCodeFields` in die Schattenwurzel des
OTP-Felds geladen und lässt sich deshalb nicht mit anderen Dateien zusammenfassen;
`email-fonts.css` dient nur den Mailvorlagen.

Die Reihenfolge ist Teil der Kaskade. Stylesheets aus Views und Komponenten verlieren
Gleichstände gleicher Spezifität gegen die globalen Dateien; eine fachliche Regel, die
eine globale überstimmen soll, braucht daher eine höhere Spezifität statt einer
bestimmten Ladereihenfolge. Dialoge brauchen eigene Klassen, da Vaadin sie außerhalb der
View platziert. Keine versteckten CSS-Imports einführen.

`!important` nur mit begründendem Kommentar, etwa gegen Inline-Styles aus Java
(`UserStatus`, `OverlayUserIcon`, `CosmosPagination`), für Seitenpanels über dem
Vaadin-Dialogoverlay, `[hidden]` und reduzierte Bewegung.

#### CSS prüfen und formatieren

Prettier formatiert, Stylelint prüft die Theme-Dateien (Farben nur aus der Palette, keine
`--lumo-*`-Variablen, keine doppelten Selektoren/Eigenschaften, keine unbekannten
Eigenschaften). Im Verzeichnis `app`:

```powershell
npm.cmd run format:css
npm.cmd run format:css:check
npm.cmd run lint:css
```

Konfiguration und Grenzen: [CSS prüfen und formatieren](entwicklung.md#css-prüfen-und-formatieren).

#### Hintergrund

Es gibt genau einen Anwendungshintergrund, „Horizont mit Mondaufgang“ (vom Benutzer
gewählte Variante A). `CosmosBackgroundComponent` erzeugt im gemeinsamen
`PageFrameComponent` die nativen dekorativen Div-Ebenen; `Application` lädt
`CosmosBackgroundComponent.STYLESHEET` (`cosmos-background.css`). Besucherbereich, Portal
und Administration verwenden denselben Hintergrund.

Die Szene besteht aus Horizont mit Atmosphäre und Wolkenbändern, zwei Sternflächen, vier
Funkelsternen, Mond, drei Sternschnuppen (die dritte unten rechts länger und flacher), einem
Satelliten (links nach rechts), einem blinkenden Wanderstern (`cosmos-drift`, rechts nach
links) und einer entfernten Supernova am rechten Rand (`cosmos-nova`, auf Smartphones
ausgeblendet) im System-Violett der Badges.
Bei reduzierter Bewegung sind Sternschnuppen, Satellit und Wanderstern ausgeblendet; alle
übrigen Ebenen stehen still. Gestaltungsregel: Dekoration
liegt nur in den Randzonen links und rechts der Inhaltsbreite oder über dem Horizont, nie
fest nach Bildschirmprozenten im Kopfbereich. Der Mond steht deshalb tief rechts über dem
Horizont (`bottom` statt `top`), die Funkelsterne liegen an den Rändern, und die Sternflächen
sind hinter dem Inhaltsbereich per zusätzlicher Maske gedämpft. Ziel ist, Seitenüberschrift und Beschreibung bei allen Breiten freizuhalten;
der noch offene Nachtest steht im [Testbedarf](testbedarf.md). Farben, Verläufe, Positionen und
Animationen liegen gemeinsam in `cosmos-background.css` (`--cosmos-background-*` am
Dateianfang); als dekorative Ausnahme darf diese Datei eigene Farbwerte außerhalb der
Palette enthalten (Stylelint-Ausnahme).

Die leichte Zeigerparallaxe setzt ausschließlich Variablen auf der Hintergrundebene.
Bei Navigation wird der vorherige Listener abgemeldet; ein entfernter Hintergrund
meldet sich spätestens beim nächsten Zeigerereignis ab. Reduzierte Bewegung
deaktiviert Animationen und Zeigerparallaxe; bei einer Änderung dieser
Systemeinstellung werden vorhandene Parallaxenwerte zurückgesetzt.
Alle Ebenen sind dekorativ und `aria-hidden`; sie fangen keine Eingaben ab.
Nur die feste Hintergrundebene verwendet `overflow: hidden`, keine Inhaltsfläche.

## Änderungen am Theme

### Neue Oberfläche und Übernahme in den Standard

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
Reine Nachrichtentitel bleiben native Aktionsbuttons. Ihre Klasse
`cosmos-notification-title` setzt den nativen Icon-/Textzwischenraum auf null,
damit ein leerer Vorspann den Titel gegenüber Nachricht und Metadaten nicht einrückt.
Ähnliche neue Komponenten knüpfen daran an, statt eine zweite Gestaltung zu beginnen.

### Theme ersetzen oder erweitern

Nur im ausdrücklich freigegebenen Umfang:

1. Farbwerte in `cosmos-palette.css`, Typografie, Flächen und Schatten in
   `cosmos-tokens.css` ändern.
2. Gemeinsame Bausteine verwenden diese Tokens; keine neuen lokalen Hex-/RGBA-Farben
   neben der zentralen Palette anlegen.
3. Gemeinsame Regeln in cosmos-theme/shell/admin pflegen, nur Anordnung in lokalen Dateien.
4. Ein neues Theme ersetzt die aktive Basis an der zentralen Ladestelle; keine
   zweite unübersichtliche Override-Kaskade parallel laden.
5. Desktop, schmale Ansichten, Fokus, Fehler, Hover, Auswahl und Dialoge prüfen.

Typische Skalen: `--cosmos-space-xs/s/m/l/xl/2xl/3xl` und die
[Schriftskala](#typografie). Komponentenmaße haben eigene Aliase,
z. B. `--cosmos-control-height`, `--cosmos-pagination-size`, `--cosmos-panel-icon-size`
und `--cosmos-notification-preview-width`. Die vier Tabellenfarben bleiben gemeinsame Tokens.

Nicht jedes gleiche Maß muss gekoppelt werden: Bildausschnitt, Dekoration und
besondere Seitenbreiten können eigene Verträge haben. CSS-Variablen sind keine
Viewportwerte für Mediaqueries; die vorhandenen Breakpoints bleiben wörtlich.

### Sichtbar machen

| Änderung / Betrieb | Übernahme |
| --- | --- |
| CSS im Entwicklungsmodus, Ressource bereits aktualisiert | Seite neu laden, bei Bedarf Strg+F5 |
| Java, neue Stylesheet-Einbindung oder veraltete Ressourcen | Passenden Build/IDE-Abgleich und Neustart benennen, danach neu laden |
| Optimiertes JAR / Container | Neues Artefakt/Image bauen und die eigene Anwendung damit starten |

`DevelopmentCssConfiguration` verhindert im Entwicklungsmodus die Auswahl älterer
komprimierter CSS-Kopien. Es ersetzt weder Ressourcenabgleich noch Neubau.
Quelländerung, sichtbare Übernahme und ausgeführte Prüfung sind getrennte Aussagen.
Der belegte Gesamtprüfstand steht in [status.md](status.md).
