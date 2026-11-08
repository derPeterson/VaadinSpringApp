# Silent Cosmos: Theme und Gestaltung

Vor Gestaltung oder Änderung einer Oberfläche diese Anleitung und die betroffenen
CSS-Quellen lesen und anwenden. Sie beschreibt den aktuellen gemeinsamen Standard
und wird bei jeder ausdrücklich freigegebenen Erweiterung im selben Arbeitsstand gepflegt.

Die laufende Vaadin-Anwendung ist die verbindliche Vorlage für Besucherbereich,
Portal und Administration. Für Stitch kann die betroffene Ansicht in einer
isolierten Instanz mit synthetischen Daten als temporärer HTML-Abzug erfasst werden.
Der frühere Bestand statischer Referenzen liegt im Git-Tag `archiv/html-referenzen`.

Scrollleisten verwenden anwendungsweit `cosmos-scrollbars.css`: transparente Bahn,
gedeckter runder Griff, bei Hover und Ziehen Cyan, ohne Richtungspfeile. Die zentralen
`--cosmos-scrollbar-*`-Tokens in `cosmos-tokens.css` gelten auch am Dokument:
Breite/Höhe 6 px, Griff `#475569`, Hover `#38bdf8`, Bahn transparent. Browser ohne
WebKit-Scrollselektoren verwenden die native schmale Breite und dieselben Farben.
Im erzwungenen Systemkontrast bleiben native Farben und Breite erhalten.
`cosmos-theme.css` importiert die gemeinsame Quelle für Dokument, normale
Scrollcontainer und öffentliche Vaadin-Parts. Grid verwendet dieselbe unveränderte
Quelle über seinen bereits vorhandenen Lumo-Einschub; private Element-IDs sind
nicht Teil der Gestaltung. Scrollfunktionen und Innenabstände werden nicht geändert.
Nur Chatverläufe reservieren zusätzlich Platz für die Scrollbahn und Abstand zu den
Sprechblasenspitzen; diese lokalen Regeln bleiben in `cosmos-mfa-confirmation.css`.

Systemkennzeichnungen verwenden die gemeinsamen Tokens `--cosmos-system-*` aus
`cosmos-tokens.css`: Akzent `#a855f7`, Fläche `#241638`, Rahmen `#a855f759`
und Text `#d8b4fe`. System-Badges in Benachrichtigungen und interne Ticketvermerke
teilen diese warme Violett-Palette. Die Kennzeichnung ergänzt Text und Icons;
fachliche Statusfarben, blaue Adminantworten und Statusereignis-Icons bleiben erhalten.

Textbuttons zusammengehöriger Dialogaktionen verwenden `EqualButtonWidths.configure`
am vorhandenen nativen Layoutcontainer. `equal-button-widths.js` lädt über den
Anwendungskontext und misst nur sichtbare Beschriftungen einschließlich Präfix,
Padding und Rahmen. Die größte natürliche Breite gilt für die sichtbaren Textbuttons
dieser Gruppe. Versteckte oder entfernte Buttons und reine Iconbuttons zählen nicht.
Bei Sprachwechsel, geändertem Text, Sichtbarkeit oder verfügbarer Breite wird neu
gemessen. Öffnungsanimationen verfälschen die Layoutmessung nicht. Die Aktionsleisten
brechen bei Platzmangel um.

Der Benachrichtigungsdetaildialog trennt Leseaktion, Nebenaktionen und Zielaktion.
Erledigte Tickethinweise zeigen ihren fachlichen Zustand („Bereits übernommen“,
„Vom Nutzer geschlossen“, „Abgeschlossen“) als neutrales natives `Badge` mit der
vorhandenen Klasse `cosmos-notification-preview-state`. Sie erhalten keine blaue
Ungelesen-Markierung oder Leseaktion. Der persönliche Lesestatus bleibt separat
gespeichert; ein geöffneter Dialog übernimmt Statusänderungen auch dann, wenn der
Eintrag aus dem aktiven Ungelesen-Filter fällt.
Die Leseaktion steht in der Nachrichtenkarte. Ein nativer `Button` öffnet
`Popover` mit beschrifteten Standardbuttons unter „Weitere Aktionen“.
Der Footer nutzt native `HorizontalLayout`-Komponenten und ordnet die Zielaktion
mobil über CSS Grid in einer eigenen vollen Zeile an. Die unabhängig angeordneten
Aktionen erhalten deshalb keine gemeinsame Breitenmessung.
Lokale Anordnungsregeln stehen unter `.cosmos-notification-detail-*` in
`cosmos-notifications.css`; die Menüfläche unter
`.cosmos-notification-actions-popover` nutzt die vorhandenen Cosmos-Farben und
Standardbuttons einschließlich des gemeinsamen Löschstils.

Anbieterlogos in der Identitätsbestätigung haben eigene Klassen direkt am Bild:
`external-proof-provider-mark` für die Aktionsbuttons und
`external-proof-heading-mark` für den Dialogkopf. Die Regeln liegen in
`cosmos-external-login.css`, das die Kontoroute ausdrücklich einbindet, auch beim
direkten Seitenaufruf. Die Iconregeln benötigen keinen Vorfahren außerhalb des
nativen Dialoginhalts, da dessen Darstellung die Komponentengrenze überschreitet.

Feldicons sind neutral in Ruhe, heller bei Hover, Cyan bei Fokus oder geöffnetem
Picker und gedämpft bei Disabled. Dies betrifft Feldaktionen, keine normalen
Aktionsbuttonicons. Beschriftete Formularfelder haben keine dekorativen
Präfix-Icons links. Suchfelder behalten ihre Lupe, rechte Passwort-, Kalender-
und Auswahlaktionen bleiben erhalten. Platzhalter verwenden vorhandene
Anwendungstexte und bleiben dezent und normal gewichtet. Öffentliche Eingaben
verwenden einschließlich Passwort dieselbe 14-px-Schrift, Platzhalter Gewicht 400.

„Login speichern“ verwendet auf der Anmeldung einen nativen Vaadin-`Switch`.
Die lokale Gestaltung in `cosmos-public.css` setzt eine 34 × 20 px große Bahn
mit 14-px-Marker, 9 px Labelabstand und 12 px / Gewicht 400 für die Beschriftung.
Aus ist dunkel mit grauem Marker, an verwendet die bestehende Primärfarbe und
einen weißen Marker. Der Tastaturfokus bleibt cyan.

Der Tabellenkopf der Benutzerverwaltung folgt Stitch-Variante 2: Versalien in
Secondary, weiße Beschriftung bei Hover beziehungsweise aktiver Sortierung,
nur der aktive Richtungspfeil in Cyan. Hover ändert allein die Schriftfarbe.
Die Regeln liegen in `admin-users.css`. Die Seitengrößenauswahl verwendet die
native, im Theme gestaltete ComboBox.

Das feste Servicezeichen ist `icons/service-logo.svg`, das gewählte Pulsarmotiv.
Header und Browser-Favicon laden das SVG. `icons/service-logo.png` ist die
transparente 512-px-Ausgabe desselben Motivs für eingebettete E-Mail-Logos und
PWA-Installationssymbole. `EmailLogo` lädt sie per Classpath-Stream und behält
seinen bisherigen CID-/Fehlervertrag. Alternativen und altes PNG liegen bewusst
in `icons/service-logo-alternatives/`. Sprachumschaltung verwendet ausschließlich
DE-/EN-Textbuttons, keine Flaggenbilder.

„Bild löschen“ im Profilbildeditor öffnet den gemeinsamen
`PortalConfirmationDialog` mit destruktivem Stil und einer `PortalContextCard`.
Abbrechen erhält das Bild. Bestätigen merkt die Entfernung nur im aktuellen
Editorentwurf vor; erst dessen „Speichern“ übernimmt sie. Der Hinweis im Dialog
erklärt diese Grenze.

Die Symbolkarten in Profilbild-, Benachrichtigungs- und Gerätebestätigungen
verwenden den bestehenden rechteckigen `cosmos-panel-icon`-Rahmen mit abgerundeten
Ecken. Die Kontextkarte enthält dafür einen Rahmen um das unveränderte Lucide-Icon,
keine Kreisfläche direkt am SVG. Echte Benutzeravatare bleiben rund.
Gerahmte Info-Boxen verwenden 14 px umlaufenden Innenabstand und 10 px zwischen
Icon und Text. Die gemeinsamen Tokens `--cosmos-info-box-padding` und
`--cosmos-info-box-gap` stehen in `cosmos-tokens.css`. Hinweise ohne eigene Box,
etwa im Panel-Footer, folgen den Abständen ihres Layouts.
Fest eingebundene Info- und Warnboxen mit **fetter Einleitung und Doppelpunkt**
richten das Icon an der ersten Textzeile aus. Die Erklärung läuft direkt nach
der Einleitung weiter. Native `HorizontalLayout`-Container verwenden dafür
`Alignment.START`; vorhandene kleine Iconabstände gleichen die Iconhöhe an die
erste Zeilenhöhe an. Bei reinem Fließtext ohne solche Einleitung bleibt das
Icon mit `Alignment.CENTER` zur gesamten Texthöhe vertikal zentriert. Das gilt
auch für Hinweise in Bestätigungen. Tooltip- und Notification-Gestaltung bleiben
unverändert.
Die Auswahl „Initialen / Eigenes Bild“ bleibt eine native Radiogruppe, deren
Label die ganze Auswahlfläche abdeckt. Der native Vaadin-Upload unterstützt
Dateiauswahl und Drag-and-drop im selben Bereich mit derselben Validierung.
Zwischen Modusauswahl, Profilbildvorschau und Uploadbereich liegen jeweils 24 px.
Beim Speichern ist auch das Ablegen gesperrt.

## Tooltips

Anwendungsweit gilt die freigegebene Stitch-Variante A: kompakte dunkle Fläche,
feiner Rahmen, ruhige helle Schrift und kein Pfeil. Die Tokens
`--cosmos-tooltip-*` in `cosmos-tokens.css` setzen Hintergrund `#0f172a`,
Rahmen `#334155`, Text `#e2e8f0`, Radius 4 px, Inter 11 px / 1.4 bei Gewicht 400,
Padding 7 px 11 px, maximale Breite 220 px und Abstand 8 px. Der Schatten ist
`0 4px 14px rgba(0,0,0,.65)`. Schmale Viewports behalten seitlich Platz.
`cosmos-theme.css` gestaltet die nativen Vaadin-Tooltip-Parts `overlay` und
`content`. Bestehende Hinweise, Tastaturbedienung und Sprachwechsel bleiben erhalten.
Tooltips fangen keine Mausereignisse ab und ändern den Mauszeiger des auslösenden
Elements nicht. Reine Identitätsavatare und Anbieterstatus behalten den normalen
Zeiger; Avatare innerhalb nativer Buttons erben dessen Mauszeiger. Eingabefelder
behalten ihren Textzeiger.
Die rechte Feldhilfe ist ein eigener Baustein und bleibt von Tooltips getrennt.

## Native Vaadin-Bausteine zuerst

Vor neuen Oberflächenelementen einschließlich Layouts die tatsächlich verwendete
Vaadin-Version prüfen. Passende native Flow-Komponenten haben Vorrang und werden mit öffentlichen APIs,
Theme-Varianten und Parts an Cosmos angepasst. Ein eigener Div-/Span-/JavaScript-
Ersatz für eine spezialisierte Komponente benötigt vorher eine ausdrückliche
Freigabe mit Begründung. Normale Layoutcontainer und Zusammensetzungen vorhandener
nativer Komponenten bleiben möglich. Vereinbarte Sonderlösungen, insbesondere
die Lucide-Anwendungsicons und der Bildausschnitt, bleiben gültig.

Für einfache Zeilen, Spalten und Formanordnungen bevorzugt `HorizontalLayout`,
`VerticalLayout` und `FormLayout` verwenden. Anordnung, Abstände und Ausrichtung
sollen im Java-Code gut nachvollziehbar sein. Div-Container mit CSS Grid/Flex
gezielt einsetzen, wenn konkrete responsive Anforderungen damit klarer und
einfacher umzusetzen sind, beispielsweise mehrere Spalten auf breiten Bildschirmen
und dieselben Bereiche untereinander auf schmalen Displays. Die Unterstützung
mehrerer Bildschirmgrößen allein rechtfertigt keinen Ersatz der Vaadin-Layouts.
Diese Regel ist kein Auftrag, bestehende Ansichten nachträglich umzubauen.

`Badge` ist in der aktuell verwendeten Vaadin-Version bereits vorhanden. Die
Anwendung verwendet überwiegend noch ältere Lumo-Badge-Spans und lokale CSS-Anzeigen.
Die neuen Anbieterstatusanzeigen verwenden bereits native `Badge`-Komponenten.
Die [Bestandsaufnahme und offene Planung](planung/vaadin-komponenten-abgleich.md)
beschreibt die betroffenen Stellen. Die Planung ist kein Auftrag zur Migration.

## Übernahme des angemeldeten Benutzerportals

`HomeView` für angemeldete Benutzer, `UserNotificationsView` und `AccountView`
verwenden `cosmos-reference-portal`. Der gemeinsame Rahmen aller drei Einstiegsbereiche ist `PageFrameComponent`:
Der Servicekopf liegt in `.cosmos-frame-header` und bleibt beim Scrollen oben.
Seitenüberschrift, optionale Seitennavigation, Inhalt und Copyrightzeile scrollen
zusammen im Dokument. `PortalPageComponent` ordnet Überschrift und optionale
Navigations-/Inhaltsspalten an. Besucher übergeben keine Seitennavigation.
`PortalSidebarComponent` ergänzt im angemeldeten Portal die Identitätskarte;
`PortalContentComponent` liefert die Startseiten- und Postfachinhalte.

Escape für die mobile Navigation wird nur innerhalb der Seitenleiste behandelt.
Dialoge und Popovers behalten ihre native Escape-Bedienung. Lange Servicenamen im
öffentlichen Header werden bei Platzmangel gekürzt, damit die Navigation erreichbar bleibt.

Portal und Administration verwenden `PageNavigationComponent` auf Basis von
Vaadins `SideNav` und `SideNavItem`, auch für die Kontobereiche. Native Links,
Tastaturbedienung und verschachtelte Einträge bleiben erhalten. Registrierte
Seitenaktionen bewahren den jeweiligen Bereichszustand und schließen die mobile
Navigation beim Wechsel. Die aktive Seite trägt `aria-current="page"`.
Die gemeinsame Gestaltung und die mobilen Grenzen liegen in `cosmos-shell.css`.
Die mobile Navigation überlagert den Inhalt unter dem Servicekopf, ohne die
Formularspalten zu verschieben. Außenklick, Escape und Seitenwahl schließen sie.

Wiederkehrende gemeinsame CSS-Klassen werden im Java-Code über `CosmosClasses`
verwendet, etwa `REFERENCE_PORTAL`, `REFERENCE_OVERLAY`, `PANEL_ICON` und `DELETE_BUTTON`.
Der Halter enthält ausschließlich Klassenbezeichner. Gestaltung, Farben, Größen
und Zustände bleiben in den bestehenden CSS-Quellen; lokale Klassen bleiben bei
ihrem jeweiligen Baustein.

`--cosmos-reference-*` in `cosmos-tokens.css` begrenzen die akzeptierten Werte.
`cosmos-theme.css` gestaltet markierte native Controls, ComboBox-Optionen und
Notification-Cards; `cosmos-shell.css` Header, Startseite, Benutzermenü und
portalisierte Dialoge; `cosmos-account.css` Konto, Profilbild und Sitzungsliste;
`cosmos-notifications.css` Vorschau, Postfach, Detail und Pagination.
`cosmos-background.css` enthält die Portal-Dekoration. Alle ausdrücklich gesetzten Anwendungsicons
kommen nach ausdrücklicher Freigabe aus Lucide. Als ausdrücklich gewählte Ausnahme
verwendet die Anbieterkonfiguration Tablers `cloud-lock` in Panelkopf und SideNav.
SVG und MIT-Lizenz liegen unter `icons/tabler/`, die Lucide-Dateien weiterhin unter
`icons/lucide/`. Der gemeinsame Baustein `AppIcon` kennt die Herkunft und liefert
für beide native `SvgIcon`-Komponenten. Es wird keine zusätzliche Iconbibliothek eingebunden.

Feldlabels erfassen nur Feldhosts, keine Buttonlabels. `PortalFields` erhält
native ComboBox-Bedienung und ergänzt die Referenzdarstellung von Punkt und Haken;
Seitengrößen erhalten keinen dekorativen Punkt.

Sekundäre Aktionsbuttons und Schließen-Buttons nutzen die Eingabefläche, bei Hover
`--cosmos-reference-button-hover` (#152238). Die allgemeine dezente Hoverfläche
anderer Komponenten bleibt davon getrennt. Schließen-Buttons setzen auch Vaadins native Variable
`--vaadin-button-tertiary-background`, damit `tertiary-inline` die Fläche nicht
transparent überschreibt. Navigation und Menüoptionen behalten ihre jeweiligen Zustände.
Destruktive Buttons verwenden gemeinsam `.cosmos-delete-button`: rotes Icon und
Text, 5 % rote Fläche und 25 % roter Rahmen; bei Hover 10 % rote Fläche, 40 %
roter Rahmen und helleres Rot. Die Werte liegen in `cosmos-tokens.css` unter
`--cosmos-reference-danger-*`, die Grundregeln in `cosmos-theme.css`.
Das gilt für einzelne und gesammelte Lösch-/Geräteaktionen sowie Bestätigungen.
Deaktivierte Buttons bleiben gedämpft ohne Hover; der Tastaturfokus bleibt cyan.
Die aktuelle Sitzung bleibt eine deaktivierte Statusanzeige ohne Gefahrenstil.
Die Seitennavigation bleibt linksbündig. Hauptzeilen verwenden 12 px bei Gewicht
500 (aktiv 600), 34 px Höhe und 8 px Iconabstand. Unterpunkte verwenden 11 px bei
Gewicht 400 (aktiv 500), 30,5 px Höhe und 28 px Einrückung. Gemeinsame Regeln
verwenden `.cosmos-side-navigation`, nicht globale Buttonzentrierung.
Klickbare Einträge und Unterpunkt-Toggles verwenden den Handzeiger.
Der Navigationspfeil ist 16 × 16 px groß, seine Klickfläche bleibt 24 × 32 px.
Die Hoverfläche
ist `--cosmos-reference-button-hover` (#152238). Die aktive blaue Fläche bleibt
bei Hover erhalten; Hauptpunkte tragen die freigegebene seitliche Akzentmarkierung.

Header und SideNav teilen die
Zählerform und Schriftstärke. „Verfügbar“ zeigt ausdrücklich grünen Text und Punkt;
andere Zustände folgen ihrer Statusfarbe. Kontostatus bleibt ein eigener Badge.
Der Startseitenbutton verwendet denselben roten Ungelesen-Zähler wie Header und
SideNav. Avatarflächen in Header, Kontokarte und Benutzermenü verwenden die dunkle
Eingabefläche mit dezentem Rahmen wie in der Referenz. Alle Avatarstatus sind
gefüllte Punkte: verfügbar grün, beschäftigt rot, abwesend gelb und offline grau.
Die Farben folgen `--cosmos-reference-avatar-presence`; Kontursymbole erscheinen
nur in der Statusauswahl. Die Punkte haben eine einfache schmale Freistellung; Border und äußerer Schatten
werden nicht als doppelter Ring kombiniert. Die Punktmitte liegt bei 45 Grad
auf der Mitte der Avatar-Kreislinie. Header, Kontokarte und Benutzermenü verwenden
dieselbe größenabhängige Verankerung.
In Kontokarte und Benutzermenü ist der sichtbare Statuskreis 12 × 12 px groß,
mit einer schmalen Freistellung zur Avatarfläche.

`PortalHomeComponent` verwendet den gesicherten SVG-Planeten und echte
Navigationsaktionen. Es erfindet keine Kennzahlen. Das Postfach erhält Lesefilter
mit tatsächlichen Zählern, Suche, Pagination und getrennte Metadaten/Aktionen.
Suchlupe und Eingabetext haben einen eigenen Abstand. Die Pagination verwendet
kompakte Seitengrößen- und Zahlencontrols; ihr Hinweis steht mit Infoicon zentriert.
Die Zähler übernehmen auch die blaue Ungelesen-Beschriftung der Referenz.
Leere Benachrichtigungslisten verwenden im Postfach, in der Glockenvorschau und
im Admin-Versandverlauf denselben
Baustein `.cosmos-notification-empty`: zentriertes blaues Glocken-Icon (32 px),
darunter mit 8 px Abstand eine kurze Überschrift mit 14 px und Gewicht 600,
ohne Zusatztext.
Die Anordnung übernimmt den Leerzustand der Benutzerverwaltung mit 48 px vertikalem
und 24 px seitlichem Padding. Bei erfolgloser Suche bleibt der passende Suchtitel
erhalten. Der Versandverlauf verwendet die Überschrift „Noch keine Benachrichtigungen gesendet“.
Ein Ladefehler wird weiterhin als Fehler und nicht als leere Liste gezeigt.
Titel und Text einer Inboxkarte haben einen einzigen Absatzabstand; ein zusätzlicher
Gridabstand entfällt. Kompakte Seitengrößenfelder reservieren keinen Leerraum für
nicht vorhandene Feldlabels.
Die native ComboBox behält ihre Bedienung und Platzierungslogik. Ihr Menü verwendet
kompakte Optionen ohne zusätzliche blaue Auswahlumrandung.
Nachrichtenmetadaten werden in Liste, Detail und Bestätigung gemeinsam aufgebaut:
Administration blau, System violett, Ungelesen mit Punkt und abgerundetem Rahmen.
Die Glockenvorschau verwendet kompakte Karten mit Aktionen rechts, gemeinsamem
Metadatenbaustein, eigenem Scrollbereich sowie festem Kopf und Footer.
Gelesene Karten nutzen die Panelfläche, ungelesene die Eingabefläche mit blauer
linker Kante. Die Metadatenzeile erstreckt sich unter Text und Aktionen über die
gesamte Kartenbreite; die Aktionen bleiben rechts neben Titel und Textauszug.
Ihre Werkzeugzeile verwendet die dunkle Overlayfläche. Lesefilter und Aktionsgruppe
sind getrennt angeordnet: 12 px dazwischen (mobil 10 px), 6 px zwischen den quadratischen
32-px-Iconbuttons. Nur der äußere Popoverrahmen rundet und beschneidet die Innenflächen.
Der Detaildialog zeigt Titel, Metadaten und Inhalt in einer inneren Nachrichtenkarte.
Sein Footer trennt linksbündige Iconaktionen (`.cosmos-notification-detail-tools`)
von den rechtsbündigen Dialogaktionen (`.cosmos-notification-detail-primary`).
Glocke öffnet die Übersicht, das Schloss wie in der SideNav die Kontosicherheit, Papierkorb den bestehenden
Löschdialog. Die Sichtbarkeitsbedingungen bleiben erhalten; Iconbuttons haben
Tooltip und zugängliche Beschriftung. „Als gelesen markieren“ und „Schließen“
behalten ihren Text. Die Gruppen umbrechen auf schmalen Displays gemeinsam.
Manuelles Aktualisieren meldet Erfolg als alleinigen Titel ohne leeren Textabsatz
erst nach erfolgreichem Laden, bei einem
Ladefehler eine Fehlermeldung. Automatische Aktualisierungen bleiben ohne Toast.
`UserPopoverMenu` baut ausschließlich das aktuelle angemeldete Menü direkt auf.
Ein alter Erstaufbau, eine ausgeblendete Schließen-Kopfzeile und eine anschließend
entfernte Menü-Statusauswahl entfallen. Besucher verwenden `PublicPageComponent`;
Anmeldung und Registrierung gehören nicht in dieses Benutzermenü.
Die gemeinsame Statusauswahl am Kontobild und die Speicher-/Fehlerlogik bleiben erhalten.
Das Benutzermenü zeigt die vier Statusoptionen zunächst aufgeklappt und eine kompakte Sprachwahl.
Rechts neben „Anwesenheitsstatus“ schaltet der gemeinsame native `DetailsToggleButton`
ohne Beschriftung die nativen `Details` auf und zu. Sein zugänglicher Name benennt
den Bereich; der blaue Pfeil und `aria-expanded` folgen dem Öffnungszustand.
Sprachwechsel erhalten den Klappzustand. Die Auswahl- und Speicherlogik bleibt erhalten.
Das Popover richtet sich unter dem rechten Rand des Avatar-Auslösers aus
(`BOTTOM_END`). Mobil reserviert ein zusätzlicher Abstand von 16 px Platz für die
innere AppLayout-Scrollbar; die Breite berücksichtigt diesen Abstand auch auf schmalen Displays.
Die separate Avatar-Statusauswahl bleibt ein natives Select mit Referenzmaßen.
Beide Statusauswahlen verwenden `.portal-status-symbol`: 20-px-Kreise mit
20 % Statusfarbe und 12-px-Lucide-Symbolen für Haken, Minus, Uhr und X.
Die Darstellung stammt aus `UserStatus.getSelectionComponent()` und
`cosmos-shell.css`; Avatarpunkte und die übrigen Kontursymbole bleiben getrennt.
Profilbildbearbeitung bleibt am vorhandenen Avatarzugang; das Benutzermenü hat
keinen zweiten Bearbeitungsbutton. Kontostatus und Anwesenheitsbadge bleiben erhalten,
die alten Verbunden-/Nicht-verbunden-Texte und deren Katalogeinträge sind entfernt.
Der Menüpunkt Administration wird nur mit der vorhandenen Adminberechtigung angeboten;
die Zugriffsprüfung des Adminbereichs bleibt maßgeblich.
Einzelbestätigungen enthalten dieselben tatsächlichen Metadaten, Sammelbestätigungen
die betroffene Anzahl. Der abweichende echte Lesebestätigungsablauf bleibt erhalten.
Die Sitzungsliste unterscheidet die aktuelle Sitzung, andere aktive Sitzungen
und gespeicherte Anmeldungen.
Aktive Sitzungen zeigen „Angemeldet über“ bei den Geräte- und Zeitangaben im
vorhandenen Metadatenstil. Der Abschnitt „Anmeldung“ in den Benutzerdetails
zeigt Registrierungsweg und verfügbare Zugänge in schreibgeschützten Vaadin-Feldern.
Die Sitzungsübersicht verwendet native Vaadin-Badges für Anzahl, Limit und
Sitzungsstatus. Statusbadges haben einen kompakten Rahmen und keinen Punkt.
Das Badge `.account-session-count` verbindet Zahl und übersetzte Einheit
auf dunkler Fläche mit Cyanrahmen.
`.account-session-count-value` zeigt die Zahl mit 15 px und Schriftstärke 700;
`.account-session-count-divider` trennt sie mit einer 12-px-Linie von der Einheit
(13 px, Schriftstärke 600). Das Limit steht rechts mit gelbem Rahmen und
dezenter gelber Fläche. Der Erklärungstext steht mit 12 px und Schriftgewicht
400 unter den beiden Badges.
Auf sehr schmalen Displays stehen auch die Badges untereinander.
Der ausführliche DE-/EN-Hinweis einschließlich des Widerrufs zugehöriger
gespeicherter Anmeldungen bleibt erhalten.

`PortalConfirmationDialog` ist der gemeinsame native Bestätigungsbaustein für
Lesen, Löschen und Geräteaktionen. Kontextkarten zeigen den tatsächlich betroffenen
Eintrag. Der einfache Infohinweis innerhalb der Bestätigung behält seine
eigenen Abstände und Schriftwerte auch im Adminlayout. Die spezifische Regel
in `cosmos-shell.css` verhindert eine Übernahme der Admin-Panelinnenabstände.
Abbrechen, Escape und Außenklick verwerfen die ausstehende Aktion;
Bestätigen verbraucht sie einmalig. Löschaktionen heißen „Löschen“, Sitzungsaktionen
behalten ihre fachlichen Begriffe Abmelden/Widerrufen. Bei „alle als gelesen“ wird
die beim Öffnen erfasste ID-Auswahl bestätigt; später eintreffende Nachrichten
werden davon nicht erfasst. Der Sammelwiderruf beendet gemäß bestehendem Dienst
alle anderen beim Bestätigen vorhandenen Zugänge. Die Dialogtexte nennen knapp
die Wirkung und den Erhalt der aktuellen Sitzung; der technische Zeitpunkt wird
nicht als zusätzlicher Erklärungssatz wiederholt.
Die Gerätebestätigungen verwenden das Lucide-Icon `log-out` in Kopf
und Aktion. Ihr Untertitel nennt die Auswahl beziehungsweise die Anzahl der
betroffenen aktiven Sitzungen und alleinstehenden gespeicherten Anmeldungen.
Kontextkarten übernehmen Gerät, Zeitangaben und tatsächliche Zustandsbadges aus
derselben Sitzungsliste. Die Erklärung steht wie in der Referenz vor dem Kontext;
ein leerer zusätzlicher Hinweisbereich entfällt. Geräteaktionsbuttons sind gleich
breit, richten sich ohne feste Mindestbreite nach der längeren Beschriftung
und bleiben einzeilig; bei schmalen Dialogen stehen die beiden
Aktionen untereinander. Die aktuelle Sitzung bleibt deaktiviert. Der Gerätefooter verwendet normale
Satzpunkte und nennt auch den Widerruf einer zugehörigen gespeicherten Anmeldung.

E-Mail-, Passwort-, Profilbild-, Nachrichten- und Bestätigungsdialoge tragen
`cosmos-reference-overlay` und das Theme `cosmos-reference`, damit portalisierte
Inhalte denselben dunklen Inhalt zwischen Kopf und Footer erhalten. Profilbild-
Upload/Ausschnitt und die bestehende Konflikt-/Entwurfsprüfung bleiben erhalten.
Der gemeinsame äußere Dialograhmen beschneidet Kopf, Inhalt und Footer mit
`overflow: hidden` an seiner 16-px-Rundung. Dadurch überdecken innere Flächen weder
die Ecken noch den Rahmen; das native Scrollen im Inhaltsbereich bleibt erhalten.
Buttons ohne gesetzten Prefix entfernen dessen Leerabstand, damit Icons, Zahlen
und reine Beschriftungen zentriert bleiben. Schließen-Buttons entfernen zusätzlich
den leeren Suffixabstand.
Die Theme-Erkennung globaler Meldungen folgt dem tatsächlich angehängten
Seitenrahmen, einschließlich verschachtelter Inhalte. Zuständig ist
`NotificationHelper`. Globale Meldungen im angemeldeten Portal verwenden alle vier Schweregradrollen,
die gesicherte Symbolzuordnung und eine eigene `.cosmos-toast-title` ohne Kollision
mit Nachrichtenbuttons. Supplier-Sprachwechsel, Anzeigedauer und Fensterisolation
bleiben erhalten.
Rückmeldungen aus URL-Parametern werden nach abgeschlossener Navigation geöffnet,
damit bei direkten Aufrufen und Anbieter-Rücksprüngen bereits der Zielrahmen die
Palette bestimmt. Das gilt für Sicherheitsbestätigungen und Anmeldefehler.

Texte kommen aus Shared-`MessageTexts` und den DE-/EN-Ressourcen. Geschlecht ist
ein echtes Pflichtfeld im bestehenden Profilvertrag, ohne neue Datenbankspalte
oder Migration. Profil-E-Mail bleibt nur lesbar und fokussierbar; die E-Mail in der
Sicherheitskarte ist weiß. Sicherheits-, Versions- und Eingabeprüfungen bleiben in
den bestehenden Diensten. Die tatsächlichen Abläufe prüfen die isolierten Browsertests.

Die Statuspunkte bei „SMTP Test“ und „Intern“ zeigen die Freigabe der Funktion
an. Verfügbar bedeutet ausgeschalteten Wartungsmodus. Bei aktivem Wartungsmodus
oder nicht lesbarer Wartungskonfiguration sind die Aktionen gesperrt. Der Punkt
bestätigt keine erfolgreiche SMTP-Verbindung. Die Zustandsbeschreibung steht als
ARIA-Label und nativer Tooltip im gemeinsamen Tooltipstil zur Verfügung und folgt
Sprache und aktuellem Wartungszustand. `MaintenanceService` und die bestehenden
fachlichen Versandprüfungen bestimmen weiterhin die Freigabe.

## Besucherportal in Vaadin

`PublicPageComponent` stellt den gemeinsamen Besucherrahmen mit Service-Logo,
Startseite/Anmeldung/Registrierung, kompakter DE-/EN-Auswahl, Seitenkopf und dem
konfigurierten Copyright bereit. `PublicHomeComponent` verwendet das vorhandene
lokale Kosmosmotiv. `PublicPanelComponent` verwendet die Cosmos-Panelklassen mit
getrenntem Kopf, Inhalt und Aktionsfooter; seine Inhalte bleiben echte native
Formularfelder und die bisherigen Aktionen der jeweiligen View.

„Anmeldung fortsetzen“ zentriert die eingeschränkte Sitzungsliste im öffentlichen
Inhaltsrahmen. Das Panel heißt „Sitzungsgrenze erreicht“ und verwendet den kurzen
Referenzuntertitel. Der Listenhinweis und die beiden Anmeldeaktionen stehen im
Panel-Footer. Die Listenbeschreibung steht unter dem Sitzungsanzahl-Badge;
das Limit bleibt rechts und wechselt bei sehr schmalen Ansichten darunter.
Abmelden und Widerrufen verwenden auch in der öffentlichen HTML-Sitzungsliste
den gemeinsamen dezent roten Aktionsstil einschließlich Hover.
„Anmeldung fortsetzen“ ist erst bei einem tatsächlich freien aktiven Sitzungsplatz
aktiv. Aktualisieren und bestätigte Abmeldungen lesen den Zustand erneut;
die serverseitige Zulassungsprüfung bleibt auch bei zwischenzeitlicher Belegung maßgeblich.
Die normale Geräteseite und die Wirkungstexte ihrer Bestätigungen
bleiben davon getrennt; die bestehende Passwortprüfung und Sitzungsfreigabe gelten weiter.

Alle öffentlichen Seiten verwenden denselben zentrierten Header-Innenbereich
`.public-header-inner` und Inhaltsrahmen `.public-main` mit maximal 1240 px.
Der Header-Innenbereich ist 64 px hoch, der äußere Trenner ergänzt 1 px.
Die seitlichen Innenabstände betragen 32 px ab 1024 px, 24 px ab 640 px und
16 px darunter; im Header unter 441 px sind es wie in der Referenz 12 px.
`.public-title-row` trennt Rubrik und Titel durch 8 px und richtet die
Seitenbeschreibung am Titel aus. Header, Schrift und mobile Navigation gelten
einheitlich für sämtliche öffentlichen Formular-, Link- und Wartungszustände.

Der Ausblick am Fuß der öffentlichen Startseite verwendet wie die angemeldete
Startseite `.portal-home-note`: durchgehender Trenner, mittig ausgerichteter
dezent grauer Text und kleiner Cyanstern. Die Besucher-HTML übernimmt denselben
abgestimmten Hinweis; `.vp-welcome-note` sorgt dort nur für den flächenbündigen
Abschluss innerhalb des vorhandenen Grids.

Die öffentliche Startseitenkarte grenzt ihre Anordnung in `cosmos-public.css`
über `.cosmos-reference-public .public-home` vom angemeldeten Startseitenstil ab:
zweispaltig ab 640 px. Darunter steht das zentrierte, maximal 260 px breite Motiv
12 px über dem Willkommenstext. Die breite Textspalte hat keine 13-ch-Grenze.
Die Überschrift ist 32 px groß (mobil 28 px), die Paneloberfläche hat keinen zusätzlichen Verlauf.
Der öffentliche Rahmen verwendet dort dieselbe 1240-px-Begrenzung und die
Abstände der festen Besucherreferenz. Der angemeldete Startseitenstil bleibt separat.
Der Button „Link anfordern“ verwendet das Lucide-Mail-Icon für den bestehenden
E-Mail-Anforderungsweg.

Das öffentliche Resetformular heißt „Neues Passwort festlegen“, bestätigt das
Passwort mit klar beschriftetem zweitem Feld und verwendet „Passwort speichern“
mit Häkchen. Die zweite Aktion führt mit dem Anmelde-Icon zur Anmeldung.
Die Aktionen des Reset- und Anforderungsformulars stehen in `.public-panel-actions`: gleich breite Spalten nach
der längeren Beschriftung, am Desktop mindestens 156 px je Aktion; unter 640 px zwei gleich breite Zeilen in voller
verfügbarer Breite. Zuständig ist die lokale `cosmos-public.css`.
Validierung, Tokenbindung und erfolgreicher Passwortwechsel bleiben im bestehenden
Resetdienst; die neutralen Seiten- und Panelüberschriften bleiben getrennt.

Die lokale Gestaltung liegt ausschließlich in `cosmos-public.css` unter
`.cosmos-reference-public`. Sie nutzt die vorhandenen Referenztokens und Controls.
`--cosmos-input-font-size` steht hier auf
14 px; leere und ausgefüllte Eingaben sowie Platzhalter bleiben bei Gewicht 400.
Mobil stehen Navigation und Sprachwahl kompakt neben der Marke; die beschrifteten
Navigationsbuttons behalten zugängliche Namen. Registrierungsfelder stehen auf
schmalen Displays untereinander. Dekorative Feldpräfixe entfallen; native Augen,
Kalender, Auswahllisten und Clear-Aktionen bleiben erhalten. Formularvalidierung,
Tokenzustände, Fehlerverträge, Enumeration-Schutz und eingeschränkte Anmeldung
verwenden weiterhin die vorhandenen Dienste. Neue neutrale Überschriften und
Seitenbeschreibungen liegen im gemeinsamen typisierten DE-/EN-Katalog.

Die Anmeldezeile „Passwort vergessen?“ hat wie die Referenz keine dauerhafte
Unterstreichung. Im Aktionsfooter bleibt der Kontohinweis normal gewichtet und
in Secondary. Die Footerlinks zur Anmeldung beziehungsweise Registrierung verwenden
wie die Referenz die Akzentfarbe und Gewicht 600.
„Login speichern“ verwendet ein 18-px-Kästchen ohne native Außenmarge und 9 px
Abstand zur vertikal mittigen Beschriftung, lokal unter `.cosmos-remember`.
Manuelles Aktualisieren der Sitzungsliste meldet den erfolgreichen Abschluss
über eine globale einzeilige Erfolgsmeldung. Automatisches Laden bleibt still;
bei Ladefehlern wird ausschließlich die bestehende Fehlermeldung angezeigt.
Bei Wartungsarbeiten heißt die Aktion „Anmelden“; der Erklärungstext nennt die
währenddessen auf Administratoren beschränkte Anmeldung. Technische
Nichtverfügbarkeit bleibt ein eigener Zustand mit neutralem Fehlertext.
Das Wartungspanel verwendet den Lucide-Schraubenschlüssel im normalen blauen
Iconrahmen. Ein zusätzlicher Hinweis, der nur die beiden Aktionen wiederholt,
entfällt in HTML und Vaadin.

Der öffentliche Copyright-Footer verwendet einheitlich 12 px vertikales Padding
und 16 px Zeilenhöhe (einzeilig insgesamt 41 px einschließlich Trenner). Der Text
darf bei langen Angaben umbrechen; eine feste Höhe schneidet ihn nicht ab.
Ergebnisansichten richten ihre Beschreibung links aus. Erfolgs- und Warnsymbole
erhalten im bestehenden Iconrahmen die passende grüne beziehungsweise gelbe Fläche.
Ungültige Passwortlinks bieten „Neuen Link anfordern“ und öffnen den bestehenden
Anforderungsweg, ohne selbst eine E-Mail auszulösen. Die Aktivierungsbestätigung
verwendet die abgestimmten DE-/EN-Texte und führt mit „Anmelden“ zur Anmeldung.
Beim abgelaufenen Aktivierungslink stehen „Startseite“ mit Haus-Icon und
„Erneut senden“ mit Mail-Icon als gleich breite Aktionen bereit.
Ein ungültiger Aktivierungslink zeigt zwei getrennte Sätze und vor dem
E-Mail-Feld 20 px Abstand. „Startseite“ und „Aktivierungslink anfordern“ sind
gleich breit; die Anforderung verwendet das Mail-Icon.
Ungültige Aktivierungs- und E-Mail-Links zeigen ein rotes Ausrufezeichen im Kreis
mit dezent rotem Hintergrund und rotem Rand. Hintergrund und Rand verwenden
dieselben Deckkräfte wie die gelbe Warnvariante (8 % beziehungsweise 27 %).
Abgelaufene Links bleiben gelb. Auch Passwortlinks haben getrennte rote
Ungültig- und gelbe Ablaufansichten mit passenden DE-/EN-Texten. Der Resetdienst
unterscheidet gültige, abgelaufene und ungültige Links unter der bestehenden
Benutzersperre. Verbrauchte und widerrufene Links bleiben ungültig; beim
Passwortwechsel werden Gültigkeit, Ablauf und Kontostatus erneut geprüft.
Die E-Mail-Bestätigung verwendet die kurze Frage „Änderung bestätigen“ und einen
separaten vollständigen Sitzungshinweis in `.cosmos-panel-hint`. Die beiden
Aktionen sind gleich breit. Ergebnisansichten haben eigene Titel und eine
primäre Anmeldeaktion; sie zeigen keinen Formularuntertitel.
Die HTML-Formulare verwenden 20 px Feldabstand ohne zusätzlichen Endabstand;
vor dem Aktionsfooter bleibt allein das bestehende Panelpadding.

Die Umsetzung umfasst öffentliche Startseite, Anmeldung und Sitzungsfreigabe,
Registrierung, Passwortanforderung/-reset, Verifikation, E-Mail-Bestätigung und
Wartung. Die feste Besucher-HTML bleibt die abgestimmte Vergleichsgrundlage. Die früheren Vergleichsviewer, Piloten und separaten Testbeispiele sind entfernt.
Die echten Linkzustände und Recoveryabläufe prüfen die isolierten Browsertests.

## Administration in Vaadin

`AdminView` übernimmt die freigegebene Adminreferenz mit dem lokalen Scope
`cosmos-reference-admin`. `cosmos-admin-reference.css` ergänzt die gemeinsamen
Referenztokens und Controls; Besucher- und Kontoseiten erhalten keine Adminregeln.
Header und Benutzermenü verwenden die bestehenden Portalbausteine. Die native
SideNav enthält die acht Konfigurationsgruppen mit denselben Lucide-Icons wie die
Panelköpfe sowie einen getrennten Portalzugang. Header, Bereichsüberschrift und Inhalt teilen die zentrierte maximale Breite
der Referenz (1240 px); der gemeinsame Inhaltsabstand beträgt am Desktop 32 px.
Die Desktopnavigation verwendet 208 px mit 24 px Abstand zum Inhalt, unterhalb
von 1200 px 180 px mit 16 px Abstand. Die Desktop-SideNav steht ohne senkrechte Trennlinie daneben; die
Copyrightzeile steht außerhalb der Inhaltsspalte über die gesamte Breite. Die
Desktopnavigation hat einen transparenten Hintergrund und liegt für Mausklicks
über der transparenten Innenfläche des Inhaltsrahmens; mobil bleibt sie eine
undurchsichtige Überlagerung. Die mobile
SideNav bleibt eine native Überlagerung. Das Infoicon des Dashboardausblicks verwendet
die Akzentfarbe. Schnellzugriffe übernehmen die Beschreibung der jeweiligen Zielseite.
Die Hauptnavigation verwendet wie das Frontend 12 px bei Gewicht 500 (aktiv 600), 34 px
Zeilenhöhe und 4 px Abstand. Gruppenüberschriften haben 8 px Abstand zur ersten
Zeile; die Portalgruppe beginnt mit 16 px Abstand. Unterpunkte verwenden 11 px
bei Gewicht 400 (aktiv 500), 30,5 px Zeilenhöhe, 2 px Abstand und insgesamt 28 px
Einrückung. Ihre aktive Fläche ist dezent blau (#008ecc1a).
Die seitliche 3-px-Akzentmarkierung gilt für den aktiven Hauptpunkt im Portal und
in der Administration.
Der Ticketsystem-Eintrag zeigt relevante ungelesene Aktivitäten als roten Punkt
rechts am nativen `SideNavItem`. Ein natives `Badge` mit der lokalen Klasse
`.admin-nav-support-activity` in `cosmos-admin-reference.css` verwendet 8 px
Durchmesser und die bestehende rote Zählerfarbe `--cosmos-reference-badge`.
Der native Tooltip erscheint nur bei aktivem Punkt und folgt dem Sprachwechsel.
Mobil verwendet die Seitenleiste denselben Innenabstand von 20 px wie das Frontend.

Das Dashboard verwendet das lokal mitgelieferte freigegebene Bildmotiv und echte
Schnellzugriffe, ohne Beispielstatistik. Der Benachrichtigungszugang folgt der
vorhandenen Superadminberechtigung; Dienste prüfen Rechte weiterhin unabhängig.

Die Benutzerverwaltung verwendet weiterhin das native Grid und seine Sortierung.
Ihre lokale Referenzgestaltung in `cosmos-admin-reference.css` übernimmt die
kompakte Werkzeugleiste ohne zusätzlichen oberen Abstand, einmalige Zellinnenabstände,
Referenzzeilenfarben sowie Punkt-/Iconstatus ohne Badgekästen. Die sieben Spalten
reservieren ausreichend Platz für die vollständigen Aktionen und Sortierköpfe;
die E-Mail-Spalte nimmt den verbleibenden Platz ein. Die lokale Palette gilt nur
für dieses Grid.
Zeilen und Pagination sind durch sichtbare, durchgehende Linien getrennt.
Senkrechte Spaltentrenner entfallen. Tabellenavatare verwenden den dunklen
Hintergrund und neutrale Initialen. Ein zusätzliches Statussymbol entfällt
am Avatar, weil die Tabelle den Status bereits in einer eigenen Spalte anzeigt.
Der Benutzer-Löschdialog übernimmt dieselbe neutrale Avatarfläche, auch außerhalb
des Seitenrahmens. Hinterlegte Profilbilder bleiben in Vaadin erhalten.
Der Löschdialog übernimmt lokal auch Breite, Kopf-/Inhaltsabstände, Textgrößen
und gleich breite Aktionen der HTML-Bestätigung; der Bearbeitungsdialog bleibt separat.
Das Benutzerformular übernimmt Kopf, Feldhöhen, Typografie und Abschnittsabstände
aus der Admin-HTML. Passworthinweise stehen unter dem zugehörigen Feld, Rollen
und Sicherheitshinweis sind durch einzelne Linien getrennt. Die normalen
Formularaktionen stehen am Desktop kompakt rechts und mobil nebeneinander.
Die Geschlechtsauswahl verwendet auch im ausgelagerten Benutzerformular den
Punkt, die dunkle Auswahlliste und die Leerenaktion der Registrierung/Kontodaten.
Native Checkbox-Häkchen werden im Dialog innerhalb der Checkboxfläche zentriert.
Der Formularhinweis verwendet das lokale Lucide-Icon `shield-check`.
Im Bearbeitungsmodus nennt der Hinweis bei leerem Passwort sowohl das Beibehalten
als auch die Regeln für ein neues Passwort. Nach Eingabe bleiben nur die
Passwortregeln stehen. Leeren und Sprachwechsel aktualisieren diesen Hinweis.
Die lokale Benutzerverwaltung zeichnet Streifen, Hover und Auswahl auf der ganzen
Zeile statt auf einzelnen Zellen. So entstehen auch im skalierten Vergleich keine
dunklen Fugen zwischen den Zellflächen. Native Fokusmarkierungen bleiben erhalten.
Die Tabellenaktionen sind außen 32 × 32 px groß. Innerhalb des 1-px-Rahmens
bleiben 30 × 30 px; eine geerbte native Mindesthöhe darf die Innenfläche nicht
über den unteren Buttonrand schieben.
Statusicons haben keine negativen Badge-Ränder und halten den Referenzabstand
zum Text ein. Der Aktualisieren-Button meldet erst nach vollständigem Neuladen
Erfolg. Ein Ladefehler erzeugt eine globale Fehlermeldung; automatische interne
Aktualisierungen ergänzen keine Erfolgsmeldung.
`AdminUserFilterBar` öffnet `AdminSidePanel` rechts über der Oberfläche. Suche,
Aktivschalter, Status- und Rollenmehrfachwahl sowie Aktivitätsgrenze sind ein
Entwurf: Nur Anwenden übernimmt einen unveränderlichen Filterstand. Zurücksetzen
ändert den Entwurf; Abbrechen, Außenklick und Escape stellen ihn wieder her.
Innerhalb einer Mehrfachgruppe gilt Oder, zwischen den Gruppen Und.
Der Zähler zählt ausgewählte Gruppen: Suche, Aktiv, Status, Rollen und
Aktivitätsgrenze jeweils höchstens einmal. Datum und Uhrzeit bilden zusammen
eine Gruppe. Auch alle ausgewählten Status- oder Rollenwerte zählen einmal.
Navigation und Tastatur bleiben linksbündig beziehungsweise nativ.
Benutzerformulare behalten Binder, Versions- und Rechteschutz; Löschen verwendet
den gemeinsamen Gefahrenstil.
Bei Status- und Rollenbadges deckt das native Checkbox-Label die gesamte sichtbare
Fläche ab. Dadurch aktivieren auch freie Randbereiche die Auswahl, ohne einen
zusätzlichen JavaScript-Klickhandler. Die lokale Regel steht in
`cosmos-admin-reference.css`.

Alle Konfigurationsgruppen behalten ihre vorhandenen Eingaben und Validierung.
Die rechte Feldhilfe nutzt `AdminSidePanel` im kompakten Referenzaufbau: fester
Kopf „Feldhilfe“ mit Unterzeile, Gruppenname, Feldtitel, Beschreibung, geteilte
Standardwertzeile und vollständige Eingaberegeln. Bei Copyright stehen die zulässigen
Platzhalter mit Erklärung vor dem Standardwert. Vertrauliche Standardwerte bleiben
verdeckt. Die Feldhilfe ist bis zu 380 px breit. Der Schließen-Button füllt die Footerbreite. Header und sichtbarer
Copyrightbereich begrenzen die Leiste; beim Scrollen und bei Größenänderung werden
die Grenzen aktualisiert. Ein nicht sichtbarer Seitenfooter reserviert keinen Abstand.
Feldhilfe und Benutzerfilter enden auf Desktop am rechten Rand der Inhaltsspalte,
auch auf breiten Bildschirmen. Dafür misst `AdminSidePanel` die aktuelle Inhaltskante
und setzt `--admin-drawer-right`. Unterhalb der Desktopnavigation bleibt der Rand
am Viewport.
Es gibt keine behauptete Livevalidierung oder Speicherung durch die Hilfe.
Pflichtpunkte folgen der tatsächlichen Leerwertprüfung; Integer-Leerwerte mit
Standardwert-/Unbegrenzt-Vertrag bleiben optional. Infoaktionen stehen außerhalb
der Checkbox-/Eingabefläche; Zahlenaktionen bleiben native Vaadin-Komponenten.

Der Mailtest verwendet ein kompaktes Panel, Klartexthinweis und SMTP-Direktzugang.
Das Infoicon steht mittig zum gesamten Block aus Hinweistext und SMTP-Zugang.
Der SMTP-Zugang ist ein echter Navigationslink in einer neuen Zeile. Er übernimmt
den rahmenlosen Avatar-Menüstil des Anbieter-Links, mit weißer Beschriftung und
dem blauen Lucide-`square-arrow-right-enter` aus den Dashboard-Schnellzugriffen.
Die Maße entsprechen dem Anbieter-Link; der obere Abstand beträgt 4 px.
Wie bei „Startseite“ und „Abmelden“ im Avatar-Overlay bleiben Fläche, Text- und
Iconfarbe bei Hover unverändert. Der Tastaturfokus bleibt sichtbar.
Die Regeln liegen in `cosmos-admin-reference.css`. Der Panelkopf verwendet 18 px
vertikal und 22 px seitlich, der Inhalt 22 px. Bis 600 px verwenden Kopf und
Inhalt 16 px. Die Nachrichtenfläche ist mindestens 144 px hoch.
Der Klartexthinweis unter dem Nachrichtenfeld verwendet konstant 11 px bei Gewicht 400
und die sekundäre Textfarbe, auch bei Hover. Feldlabels verwenden 12 px bei
Gewicht 500. Der
Aktionstrenner läuft über die gesamte Panelbreite. Das Nachrichtenfeld hat vertikalen
Innenabstand; Eingabetexte verwenden wie die Kontofelder 14 px bei Gewicht 400.
Fehler bleiben globale Notifications mit markiertem Feld; es gibt keine zusätzliche
Fehlerbox im Panel. Der reale SMTP- und Wartungsvertrag bleibt erhalten.

Benachrichtigungen zeigen Verfassen und Vorschau nebeneinander, mobil untereinander.
Empfängerauswahl, Vorschauentwertung, erfasste Empfängermenge und Request-ID bleiben
im vorhandenen Dienst. Die Vorschau zeigt Nachricht und Empfängeravatare, Namen
und Adressen erst in den Details. Der Verlauf wiederholt Titel und Metadaten nicht
im aufgeklappten Inhalt. Er verwendet Glockensymbole, durchgehende Trenner und die
gemeinsame Pagination. Ungeprüfte Aussagen zur unveränderlichen Protokollierung
werden aus dem synthetischen HTML-Beispiel nicht als echte Funktion übernommen.

## Icon und Text in Aktionsbuttons

Im Referenzportal und seinen Overlays haben native Button-Präfixe zum Text 8 px
Abstand. Präfix und Label richten ihre Inhalte mittig aus; leere Präfixe reservieren
keinen Abstand. Die lokale 10-px-Ausnahme der Seitennavigation und die 7-px-Ausnahme
der Sitzungsaktionen sind auf diesen Standard angeglichen. Auch die Kontotabs
verwenden denselben Abstand. Zuständig sind `cosmos-theme.css` und
`cosmos-account.css`. Die Avatar-/Identitätsanordnung im Kopf und reine Iconbuttons
behalten ihre eigenen Maße; native Eingabe- und Tabellensymbole werden nicht umgebaut.

## Gemeinsames Lucide-Iconset

`AppIcon` definiert die benötigten Symbolfunktionen und erzeugt jeweils ein natives
Vaadin-`SvgIcon` aus `META-INF/resources/icons/lucide/<name>.svg`. Buttons behalten
ihre Beschriftung, Icons sind dekorativ mit `aria-hidden`. Direkte `VaadinIcon`-,
`LumoIcon`-Aufrufe und der frühere `PortalIcon`-Wrapper entfallen. Logo, Flaggen und
Kosmosillustration sind eigenständige Bildinhalte und kein Teil des UI-Iconsets.
Lokale Original-SVGs, vollständige Lizenz und Paketquelle liegen zusammen im
Lucide-Ordner; es gibt keinen CDN-Aufruf und keine zusätzliche JavaScript-Bibliothek.

`cosmos-icons.css` wird nach den übrigen Themequellen geladen und betrifft nur
ausdrücklich erzeugte `AppIcon`-Symbole. Es übernimmt die Lucide-Originalstrichstärke;
vorhandene Größen, Statusfarben und Zustandsregeln bleiben maßgeblich.
Anwesenheitsanzeigen verwenden `circle-check`, `circle-minus`,
`clock` und `circle-x`. Der Gerätebaustein verwendet `monitor-smartphone`, die
Sammelaktion „alle gelesen“ `check-check` entsprechend der Referenzfunktion.

Auf ausdrücklichen Benutzerwunsch bleiben die eingebauten Symbole der nativen
Vaadin-Komponenten unverändert. Dazu gehören Passwortauge, Kalender, Auswahl- und
Leerenzeichen, Zahlenpfeile, Checkbox-Haken, interne Tab-/Drawer-Schalter und
Tabellensortierung. Tabellenköpfe und Komponenten werden für diese Umstellung
nicht umgebaut. Die Suche verwendet Lucide, wenn ihre Lupe durch Anwendungscode
gesetzt wird. Es gibt keine Masken- oder Schriftglyphenüberschreibung für native
Komponenten und keine Abhängigkeit von internen Icon-CSS-Variablen.

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
| cosmos-scrollbars.css | Gemeinsame native Scrollleisten für Dokument, Container und Vaadin-Scrollflächen |
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
„Abmelden“ bleibt im Benutzermenü-Footer rechtsbündig, auch wenn der
Startseiten-Button auf der bereits geöffneten Startseite entfällt.
Die Administrationsaktion im Avatar-Menü verwendet den sekundären Buttonstil
mit Rahmen, weißem Text und `shield-cog` als Akzenticon, eingerückt wie die primäre
Kontoaktion. „Mein Konto“ behält das einfache Zahnrad.

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

Der Textblock im Admin-Dashboard verwendet die Canvasfarbe
(`--cosmos-reference-page`). Konfigurationsaktionen stehen über eine vollbreite
Trennlinie getrennt. Dialogaktionen verwenden den gemeinsamen Größenabgleich der
sichtbaren Textbuttons und brechen bei wenig Platz um. Konfigurationsaktionen
behalten ihre eigene responsive Anordnung. Der Mailtest erlaubt
über `resize:vertical` am nativen TextArea-Host das manuelle Vergrößern des
Eingabefelds einschließlich Label. Die Mindesthöhe beträgt 168 px. Der sichtbare
Klartexthinweis steht außerhalb, damit der Griff direkt an der Eingabekante sitzt.
`.cosmos-mail-body-hint` zeigt den Hinweis. Die native Hilfsbeschreibung bleibt
visuell verborgen für assistive Technik erhalten, sodass sie weiterhin mit der
Eingabe verknüpft ist. Die Infoaktion wird zur Eingabefläche zentriert.
Die Breite bleibt fest. Diese Regeln gelten nur für diese Adminbausteine.
Auch die Nachrichten-Textarea lässt sich vertikal vergrößern. Sie hat keine
maximale Zeilenbegrenzung, damit die Eingabefläche dem Ziehgriff vollständig folgt.

Konfigurationsbeschreibungen und Eingaberegeln verwenden vollständige Sätze ohne
Semikolons. Unterschiedliche Hinweisbereiche stehen in eigenen Absätzen. Der
Textkatalog enthält dafür normale Zeilenumbrüche; `.admin-help-description` und
`.hc-description` erhalten sie über `white-space:pre-line`, ohne HTML im Textkatalog.

Das Dashboardbild übernimmt den Rahmen der HTML-Vorlage: Seitenverhältnis 512/286,
10 px Rundung und dezente Border. Das Raster hat 28 px Innenabstand und Lücke,
bis 1100 px jeweils 24/22 px, bis 760 px eine Spalte mit 20 px Innenabstand
und Lücke sowie Bild vor dem Text. Die gleich breiten HTML-Aktionsbuttons sind
auf `.ad-config` begrenzt. Der Mailtest bleibt mit natürlicher Buttonbreite
rechtsbündig, einschließlich Smartphone.

`AdminDurationField` wird in allen neun Zeitfeldern der Konfiguration verwendet:
Login speichern, vier Tokenfristen, automatische Abwesenheit, zwei
Benachrichtigungs-Aufbewahrungen und gesendete E-Mails. Zahl und Einheit stehen
nebeneinander. Aktueller Wert, Umrechnung und Standardwert stehen nur in der Feldhilfe.
Sekunden, ISO-Zeitspanne und ganze Tage behalten ihr jeweiliges Speicherformat.
Die bestehenden Mindestdauern und Bereichsgrenzen bleiben erhalten. Beim Laden
wird die größte ganzzahlig passende Einheit gewählt. Einheitenwechsel erhält
die Zahl, die Schaltflächen ändern sie um eins. Bestehende Nachkommastellen einer
Zeitspanne werden ohne Rundung erhalten, bis zur Nanosekundenpräzision von Duration.
Nur Login speichern verwendet bei leerer Eingabe den Standardwert. Leere
Benachrichtigungs-Aufbewahrung bedeutet weiterhin unbegrenzt. Diese beiden Felder
haben deshalb eine feste Einheit Tage. Andere Zeitspannen bleiben Pflichtfelder.

Die Regeln bleiben lokal in `cosmos-admin-reference.css`: `admin-duration-field`
hält Eingaben und Einheiten in einer Zeile mit 40 px hohen Flächen. Der geschlossene
Einheitenwert ist normaler Feldtext. Nur das Menü markiert die Auswahl.
`admin-duration-step` gestaltet die Schaltflächen an der präzisen Vaadin-Zahleneingabe.
Konfigurationsfelder mit externem Label blenden die leere interne Labelzeile aus,
damit beide Spalten dieselbe Ober- und Unterkante haben.

Alle booleschen Konfigurationsfelder verwenden native Vaadin-Switches mit der
Klasse `cosmos-config-switch`, auch in der Anbietereinrichtung. Der Text steht
links, der Schalter rechts und das Infoicon außerhalb. Die Zeile hat keinen
Rahmenkasten. Die gemeinsame Gestaltung liegt in `cosmos-admin-reference.css`:
34 × 20 px Bahn, 14-px-Marker und mindestens 40 px Zeilenhöhe. Das Label steht
links, die Bahn am rechten Rand der Eingabespalte.
Die öffentlichen Parts `label` und `switch` sowie der native Eingabeslot liegen
in derselben Rasterzeile. Eine RTL-Umkehrung ist dafür nicht nötig. Fokus, Tastatur,
Disabled-Zustand und Labelklick bleiben nativ. Ein Umschalten ändert zunächst den
Formularentwurf; übernommen wird er weiterhin mit „Konfiguration speichern“.
Die übrigen Checkboxen, beispielsweise bei Benutzerrollen, bleiben Checkboxen.

Die Admin-Benachrichtigung verwendet eine native Mehrfachauswahl mit zweizeiligen
Empfängern (Name und E-Mail). Die Anzahl liegt innerhalb der Eingabefläche und weicht
beim Tippen dem Suchtext. `admin-notification-body-field` erlaubt ausschließlich
vertikales Vergrößern und behält den Innenabstand der Textarea. Vorschau und
Eingabepanel beginnen bündig. Ihre Aktionsleisten erhalten vollbreite Trennlinien;
Metadaten bleiben einfacher Text und der Prüfstatus verwendet ein grünes Häkchen.
Die Darstellung bleibt lokal in `cosmos-admin-reference.css`.
Panel-Badges in Admin-HTML und Vaadin verwenden 30 px Höhe und Schrift mit
10 px, Gewicht 600, 16 px Zeilenhöhe und 1,2 px Zeichenabstand wie
Konfiguration und Mailtest. Die Empfängeranzahl verwendet Gewicht 600 bei einer
Auswahl und Gewicht 400 ohne Auswahl. Die weiße Details-Aktion steht rechts neben den Empfängeravataren;
ihre Liste klappt darunter auf. Native Admin-Checkboxen zentrieren das Häkchen
innerhalb der unveränderten Checkboxfläche.
Ohne individuelle Auswahl bleibt die Empfängeranzahl in der dezenten
Placeholderfarbe. Die geöffnete Empfängerliste zeigt wie die Referenz Name und
E-Mail untereinander mit feinen Zeilentrennern und eigenem Scrollbereich.
„Details“ öffnet und schließt sie. Der Pfeil kennzeichnet den Zustand.
Die Herkunft „Administration“ verwendet in
der Vorschau dasselbe Badge wie im persönlichen Postfach. Der zusätzliche
Empfängerzähler ist rund und verwendet die Referenz-Akzentfarbe.
Die Abschnittsüberschrift „EMPFÄNGER“ in der Vorschau verwendet wie die Feldhilfe
die Akzentfarbe, Großbuchstaben und den kompakten Abschnittstitelstil.
HTML und Vaadin zeigen bei individueller Auswahl ausschließlich die Anzahl im
Feld, einschließlich einer dezenten Nullanzeige. Die letzte Empfängerzeile hat
keine untere Trennlinie. Der zusätzliche runde Zähler hat eine feste Breite und
kräftige, zentrierte Ziffern. Der Prüfstatus übernimmt Inter mit 11 px, Gewicht 400
und 16,5 px Zeilenhöhe. Eine entwertete Vorschau zeigt wie die Referenz eine gelbe
Glocke. Der Versandverlauf verwendet durchgehende Zeilen statt einzelner Karten.
Beim Wechsel zwischen allen Konten und Einzelauswahl liegt der Anzahltext auf
derselben inneren Textkante des nativen Feldrahmens. Die Details-Beschriftungen
bei Empfängern und im Verlauf verwenden dieselbe Größe und Stärke. Der Verlauf
zeigt Nachrichteninhalt mit `message-square`, kompaktem Abschnittstitel,
Referenz-Innenabständen und einer getrennten Zustellzeile. Der äußere native
Details-Container ergänzt keine zusätzliche Linie oberhalb der Inhaltskarte.
Die Anzeige aller aktiven Konten verwendet dieselben kräftigen Schriftwerte wie
die individuelle Auswahl und setzt die Textfarbe ausdrücklich auf das dezente Grau.
Die untere Verlaufszeile zeigt links die Zustellung ins persönliche Postfach und
rechts „Unveränderliche Versandprotokollierung“, bei schmaler Breite umbrechend.
Die Zeichenzähler stehen rechts auf der
Beschriftungszeile über Titel und Nachricht, ohne die Eingabebreite zu verkleinern.
Die Beschriftung „Empfänger“ und ihr blauer Pflichtpunkt bleiben beim Wechsel
der Auswahlart unverändert. Nur der Wert für alle aktiven Konten ist grau.
Beide Empfängerfelder verwenden dieselbe Labelausrichtung und denselben Abstand
zum Pflichtpunkt. Die Auf-/Zuklapppfeile der Admin-SideNav sind akzentblau.
Die geprüfte Nachrichtenvorschau zeigt links neben dem Prüfstatus eine blaue
Abschnittsüberschrift mit demselben Nachrichtenicon und Stil wie der Nachrichteninhalt.
Beide Abschnittsüberschriften verwenden 10 px und Schriftgewicht 600.
Die Empfängerüberschrift der Vorschau ergänzt links das vorhandene Gruppenicon.

Die Benutzerdialoge Anlegen/Bearbeiten verwenden eine native Mehrfachauswahl für
Rollen neben der Aktiv-Checkbox, mobil untereinander. Im Feld steht die Auswahlanzahl;
Die Liste verwendet wie die Empfängerauswahl einen dunklen Rahmen, kompakte
12-px-Optionen und Auswahlkästchen. Der Abstand zum Eingabefeld beträgt unabhängig
von der Auswahl vier Pixel. Tippen im Feld filtert Rollen, ohne die Auswahl zu
verändern.
Optionsbeschriftungen und Anzahl folgen dem Sprachwechsel. Mindestens eine Rolle
bleibt erforderlich. Die Superadminrolle wird nur entsprechend der bestehenden
Berechtigung angeboten. Der Prüfhinweis steht in einer eigenen Footerzeile über den
rechtsbündigen Aktionen, mit durchgehender Trennlinie. Die lokalen Regeln liegen in
`cosmos-admin-reference.css`.
Am unteren Ende der Benutzertabelle zeichnet allein die Pagination die Abschlusslinie;
die letzte Zeile erhält keinen zusätzlichen unteren Rahmen.

Der Benutzerfilter gleicht seine rechte Seitenleiste lokal über
`.admin-user-filter-drawer` an den vereinbarten Stil an: Kopf mit Beschreibung,
Aktivzähler in Buttonhöhe, kompakte Status-/Rollenkarten mit farbigen Symbolen und
Bestätigungshaken, dunkler Scrollinhalt und gleich breite Footeraktionen. Statusfarbe
kennzeichnet das Symbol und die ausgewählte Karte; unausgewählte Texte bleiben
Secondary. Die gemeinsame Konfigurationshilfe wird dadurch nicht umgestaltet.
Filterentwurf, Zurücksetzen, Anwenden und Verwerfen bleiben unverändert.

Der Datumsfilter „Letzte Aktivität ab“ verwendet denselben übersetzten
Beispielplatzhalter wie das Geburtsdatum (DE: „z. B. 31.12.1990“).
Die Zeitvorschau und der tatsächliche Filterwert bleiben davon unberührt.

## Externe Anmeldebausteine

`ExternalLoginButtons` ergänzt die vorhandenen Anmelde-/Registrierungspanels.
`ExternalConnectionsComponent` verwaltet Anbieter im bestehenden Sicherheitspanel.
Das Sicherheitspanel gliedert seine Inhalte mit einem nativen `TabSheet` in
„Portalzugang“ (Service-Logo, Passwort, E-Mail, Zugangsdatenhinweis) und
„Anmeldeanbieter“ (Cloud-Lock, Google/Microsoft). Die Tabs übernehmen lokal in
`cosmos-account.css` dieselbe Form wie die Anbieterkonfiguration. Der normale
Seiteninhalt wächst mit; ein eigener Scrollbereich entfällt. Portalzugang ist
anfangs ausgewählt. Beide Tabs behalten ihre Komponenten beim Umschalten.
Google/Microsoft verwenden denselben Sicherheitskartenstil wie Passwort und E-Mail
mit Markenlogo, nativem `Badge`, Verbindung und passender Aktion. Die allgemeine
Aktionsauswahl und das dauerhaft sichtbare Passwortfeld entfallen.
Die Anbieterbuttons gleichen ihre natürliche Breite ausschließlich untereinander an.
Die schmale „Ändern“-Breite des Portalzugangs gilt nicht für diese Karten.
Das gemeinsame Kartenraster verwendet deshalb eine automatische Aktionsspalte.
Eine später geladene Kontogestaltung darf die Anbieteraktionen nicht begrenzen.
Verbindungsbadges übernehmen den kompakten Kontostil mit Rahmen, kleinen Ecken und
derselben Schriftstufe. Verbundene Anbieter sind grün, nicht verbundene blau.
Service- und Cloud-Lock-Symbole in den Kontotabs sind lokal 20 px groß, um ihre
schlankeren Motive gegenüber den gefüllten Markenlogos auszugleichen.
`ExternalIdentityConfirmationDialog` ist ein nativer `Dialog` im vorhandenen
Kontodialogstil. Er liegt außerhalb der Tabinhalte, damit ein inaktiver Tab seine
Bedienung nicht sperrt. Er bestätigt die konkret gewählte Aktion mit Portalpasswort oder
einem bereits verbundenen, verfügbaren Anbieter. Nach externer Bestätigung werden
Passwort-/E-Mail-Dialoge wieder geöffnet. Nach einem Nachweis über einen bereits
verbundenen Anbieter folgt beim Verbinden/Trennen die ausdrückliche Bestätigung.
Die letzte nutzbare Anmeldung bleibt geschützt.
Verbinden/Trennen zeigt das Dienstlogo im Dialogkopf.
Bei erforderlicher 2FA verwendet der Dialog die Tabs „App-Code“ und „Ersatzcode“
und die Codefelder der Anmeldebestätigung mit Space Mono. Die erste Ziffer erhält
beim Öffnen den Fokus. Beim Verbinden eines Anbieters startet ein vollständiger
App-Code nach erfolgreicher Prüfung den Anbieterschritt automatisch. Der Aktionsbutton
bestätigt beide Codearten. Fehler verwenden die vorhandene Pflichtfeld- oder
Kontosicherheitsmeldung als Notification und markieren das betroffene Feld rot.
Ein zusätzlicher Fehlertext unter dem Passwortfeld entfällt. Der Trenner
„ODER“ verwendet Großbuchstaben, Gewicht 600 und die Secondary-Textfarbe.
Nur die Anordnung von Zeilen, Anbieterlogos und Bestätigungsalternativen wird lokal
gestaltet.
Ihre lokalen Regeln liegen in `cosmos-external-login.css`. Die vorhandenen globalen
Feld-, Dialog- und Buttonregeln bleiben die Grundlage. Anbieterbuttons verwenden
die offiziellen Google-/Microsoft-Markenressourcen unter `icons/external-login/`
und eine begrenzte Abweichung für das Anbieterbranding.

Anmeldung und Registrierung verwenden `PublicAuthenticationPanels` für das lokale
Formular und den optionalen eigenständigen Schnellzugang. `ExternalLoginButtons`
liefert in seiner Paneldarstellung Kicker, Überschrift, Erklärung und „Mit … fortfahren“.
`.public-auth-panels--external` ordnet das lokale Formular, den Trenner und den
Schnellzugang nebeneinander an. Bei der Anmeldung bleibt das Formular 600 px breit,
die Zeile höchstens 1048 px. Unter 1024 px stehen die Panels untereinander.
Die Variante `.public-auth-panels--register` lässt dem bestehenden zweispaltigen
Registrierungsformular mehr Raum: ohne Anbieter maximal 880 px; mit Anbieter
600 bis 880 px für das Formular, 376 px für den Schnellzugang und maximal 1328 px
für die gesamte Zeile, begrenzt durch den gemeinsamen Seitenrahmen. Unter 1200 px
stehen die Registrierungspanels untereinander, weiterhin mit maximal 880 px Breite.
Die vorhandenen Felder, Feldspalten und Aktionen bleiben erhalten.

Dazwischen steht ein 40 px breiter Trenner mit je 16 px Abstand zu den Panels.
Seine dezente dunkle Linie trägt mittig „ODER“ in der Secondary-Textfarbe, bei
englischer Sprache „OR“. Der Text hat keinen Rahmen, Hintergrund oder eigenen
Innenabstand. Schrift und Linienabstand entsprechen dem Trenner im
Identitätsbestätigungsdialog: 10 px, Gewicht 600, 18 px Zeilenhöhe,
0,06 em Zeichenabstand und 12 px Abstand zu den Linien.
Beim Stapeln verläuft die Trennlinie waagerecht; der Textstil bleibt gleich.
Der Schnellzugang behält seine Inhaltshöhe und steht auf Desktop vertikal mittig
neben dem Formular. Er verwendet die Panelfläche von Kopf und Footer
(`--cosmos-reference-panel`, in HTML `--surface-panel`) und 32 px Innenabstand,
mobil 24 px / 20 px. Seine Anbieterbuttons übernehmen die Schrift des lokalen
Anmelden-Buttons (Inter, 12 px, Gewicht 600). Die unveränderten farbigen Markenlogos
stehen in einer 16 × 16 px großen Iconfläche mit 8 px Abstand zum Text.
Die Anbieterbuttons stehen in `.external-provider-actions`: mindestens 220 px pro
Button und 8 px Abstand. Reicht die tatsächliche Inhaltsbreite für beide, stehen
sie gleich breit nebeneinander. Andernfalls wechseln sie untereinander. Unterhalb
der Mindestbreite begrenzt die verfügbare Fläche den Button, damit kein Überlauf
entsteht. Diese Regel gilt für Anmeldung und Registrierung unabhängig davon,
ob der Schnellzugang neben oder unter dem Formular steht.

Die dunkle Formularfläche und der lokale Aktionsfooter bleiben erhalten.
Kontohinweis und Link zur jeweils anderen Anmeldeseite stehen bei genügend Platz
nebeneinander. Zuerst darf der Link unter den Hinweis umbrechen. Reicht der Platz
auch dafür nicht, rückt die Primäraktion rechtsbündig in eine eigene Zeile.
Der Umbruch folgt der tatsächlichen Textbreite statt einer festen Smartphonegrenze.
Ohne aktive Anbieter bleibt allein das lokale Panel sichtbar. Die eingeschränkte
Sitzungsverwaltung blendet beide Anmeldewege gemeinsam aus. Die Regeln bleiben
in `cosmos-external-login.css`.

Die Anbieterverwaltung unter Konfiguration verwendet die bestehenden Adminpanel-,
Formular-, Switch- und Footerbausteine. Panelkopf und SideNav heißen einheitlich
„Anmeldeanbieter“. Die zwei Erklärungssätze im Kopf stehen mit festem Zeilenumbruch
und 20 px Zeilenhöhe, damit rechts Platz für die Statusbadges bleibt.
Ein gemeinsamer Panelkopf steht über einer
nativen `TabSheet` mit Google-/Microsoft-Logo und Name. Der Einrichtungsassistent
und das Anbieterformular liegen im jeweiligen Tabinhalt. Beide Formulare bleiben beim Tabwechsel
erhalten; weder Speicherung noch Neuladen werden dadurch ausgelöst.
Der aktive Anbietertab hat einen blauen oberen Rand, abgerundete obere Ecken und
die dunkle Inhaltsfläche. Desktop-Tabs sind mindestens 168 px breit und zentrieren
Logo und Namen. Mobil teilen sie sich die verfügbare Breite gleichmäßig.
Die Tableiste hat keinen oberen Innenabstand. Unter dem Panelkopf gibt es keine
Trennlinie. Die Linie vor dem Inhalt wird als
Hintergrund der Tableiste gezeichnet und nur unter dem aktiven Tab verdeckt.
Der Tabinhalt wächst mit der Seite und hat keinen eigenen Scrollbereich.
Seine native obere Scrollmarkierung ist deaktiviert, damit unter dem aktiven
Tab auch nach Fokuswechseln keine zusätzliche feine Linie erscheint.
Der zusätzliche TabSheet-Rahmen ist lokal entfernt. Rechts im Panelkopf stehen
zwei native `Badge`-Komponenten mit Anbieterlogo und gespeichertem Status, aktiv
in Grün, deaktiviert oder noch nicht eingerichtet in Gelb. Sichtbar ist nur
„Aktiv“/„Inaktiv“. Anbietername und Status bleiben als zugängliche Beschriftung erhalten.
Native Tooltips erläutern
die Freigabe für neue Anmeldungen, die Prüfung der Zugangsdaten beim Anmeldeversuch
und bei deaktivierten Anbietern den Erhalt bestehender Sitzungen. Ungespeicherte
Formularänderungen ändern diese Statusanzeige nicht. Die Statusanzeigen bleiben
auch mit Tastaturfokus lesbar. Client-ID, Client-Secret
und Freigabe haben rechts einen `ConfigInfoButton`. Ein Klick öffnet die bestehende
`AdminSidePanel`-Feldhilfe mit anbieterspezifischer Erklärung und Eingaberegeln.
Geheimniswerte werden dabei weder angezeigt noch zurückgeladen. Die Hilfe und
Status-Tooltips folgen dem Sprachwechsel. Der Tabinhalt beginnt mit „Einrichtungsassistent“
in 11 px bei Gewicht 600 und dem Lucide-`bot`-Icon, einer Anbieterüberschrift
und einer kurzen Erklärung. Die Schrittüberschriften behalten 10 px.
Darunter gliedern drei Schrittüberschriften Einrichtung mit Rücksprungadresse,
Zugangsdaten und Freigabe. Sie tragen die blauen Tabler-Icons
`square-rounded-number-1`, `square-rounded-number-2` und `square-rounded-number-3`
mit den Titeln „Anbieter einrichten“, „Zugangsdaten hinterlegen“ und „Anmeldung
aktivieren“. Der native Link zur Anbieter-Verwaltung übernimmt die rahmenlose
Startseitenaktion des Avatar-Overlays: 36 px Höhe, 12 px seitlicher Innenabstand,
12-px-Text mit Gewicht 600 und ein blaues 16-px-Tabler-`external-link`-Icon
mit 8 px Textabstand. Zwischen Erklärung und Linkfläche liegen 4 px.
Der Link bleibt auch bei Hover transparent mit unveränderten Text- und Iconfarben.
Sein sichtbarer Tastaturfokus bleibt erhalten.
URL, neues Fenster und Linksemantik bleiben erhalten. Die Rücksprungadresse trägt
das kurze Label „Rücksprungadresse“. Der Kopierbutton hat dieselbe Höhe wie die
Eingabefläche (40 px) und das blaue 16-px-Lucide-Icon `clipboard-copy`.
Die Aktivierungszeile verwendet denselben nativen Konfigurations-Switch ohne Rahmenkasten.
Text und Switch bleiben zusammen mit der rechts stehenden Feldhilfe auf der Breite
einer Zugangsdaten-Spalte: halbe Inhaltsbreite abzüglich des halben 20-px-Spaltenabstands.
Unter 600 px wird die Zeile vollständig breit. Der Switch übernimmt lokal die
34 × 20 px große Bahn und den 14-px-Marker des Anmeldeschalters.
Beide Zugangsdatenlabels haben den üblichen
blauen Pflichtpunkt für die Aktivierung. Ein gespeichertes Secret erfüllt diese
Voraussetzung trotz leer angezeigtem Passwortfeld. Ohne Client-ID oder neues/gespeichertes
Secret lässt sich die Freigabe nicht einschalten. Ausschalten bleibt auch mit
unvollständigem Entwurf möglich. Eingaben aktualisieren diese Verfügbarkeit direkt,
ändern aber weder den gespeicherten Status noch die tatsächliche Anbieterfreigabe.
Panelkopf und SideNav verwenden dieselbe
`ConfigGroupIcons`-Zuordnung zu `AppIcon.CLOUD_LOCK`.
Nur das Anordnen von Einrichtungshinweis,
Rücksprungadresse, Zugangsdaten und Freigabe liegt lokal in
`cosmos-provider-settings.css`. Anbieterzeichen stammen aus `icons/external-login/`.
Globale Feld-, Button- und Dialogregeln werden dafür nicht verändert.

Die beiden Textbuttons in `.portal-home-actions` der angemeldeten Startseite
verwenden den bestehenden sichtbarkeits- und sprachabhängigen Größenabgleich
`EqualButtonWidths`.
Die Sprachbeschriftung im Avatar-Overlay verwendet die normale Primärtextfarbe.
Sprachnamen unter „Bevorzugte Sprache“ folgen der aktuellen Oberflächensprache:
DE „Deutsch/Englisch“, EN „German/English“. Die gespeicherte Locale bleibt unverändert.
Beim Sprachwechsel wird auch der native Beschriftungsgeber neu gesetzt, damit
Vaadin den bereits ausgewählten Feldwert ebenso aktualisiert wie die Auswahlliste.

## Zwei-Faktor-Bereiche

Die gemeinsame Klasse `cosmos-panel-tabs` (`CosmosClasses.PANEL_TABS`) gestaltet
die nativen Ticket-`Tabs` und die `TabSheet`-Leisten der Anmeldeanbieter sowie der
2FA-Rollen. Die gemeinsamen Regeln stehen in `cosmos-provider-settings.css`:
48 px Höhe, mindestens 168 px Breite auf Desktop, abgerundete obere Ecken und
eine blaue Oberkante am aktiven Tab. Die untere Trennlinie bleibt außerhalb des
aktiven Tabs sichtbar. Unter 600 px teilen sich die Tabs den verfügbaren Platz.
Der Ticketfilter behält seine bestehenden Kategorien und seine Auswahlfunktion.
Im „Ticketsystem“ entfällt die Linie zwischen Panelkopf und Tableiste.
Der vorhandene `CosmosSectionHeaderComponent` liefert wie in den anderen
Adminbereichen den großen Seitenkopf mit „Administration / Ticketsystem“,
Titel und allgemeinem Erklärungstext. `AdminView` ordnet ihn im gemeinsamen
Seitenrahmen an. Der separate Panel-Untertitel erklärt die Auswahl und Bearbeitung
eines Tickets, ohne einen Nachrichtenverlauf für jede Ticketart vorauszusetzen.
Lucides `messages-square` steht am Panelkopf, in der Navigation, am Dashboardzugang
und vor „Alle Tickets“. Der Tab „Wiederherstellung“ nutzt wie die Benutzerhilfe
Tablers `lock-question`. Die nativen Tabicons sind 16 px groß und akzentblau.

Die administrative 2FA-Konfiguration gruppiert die neun Regeln je Rolle in
Zugang und Administration, Konto- und Sicherheitsdaten sowie Authenticator und
Anmeldeanbieter. Das native `TabSheet` verwendet die Tableiste der Anmeldeanbieter
aus `cosmos-provider-settings.css`. Die Erklärspalte steht auf breiten Inhaltsflächen
neben den Gruppen und rückt auf schmalen darunter. Die rollenübergreifende
Inaktivitätsregel folgt außerhalb der Tabs und trägt das vierte Tabler-Zahlenicon.
`cosmos-mfa.css` enthält das lokale Layout. Die Erklärspalte trennt die blaue
Info-Box mit einer horizontalen Linie von den Hinweisen darüber und hebt den Namen
der gerade bearbeiteten Rolle fett hervor.
`.mfa-role-scope` steht in `cosmos-mfa-confirmation.css`. Regelauswahl und
Inaktivitätsdauer verwenden die 40 px hohen Eingabeflächen
der Konfiguration mit externen Feldlabels und 6 px Abstand. Die Regelauswahl
verwendet eine native `ComboBox` mit dem gemeinsamen Optionsstil der Pagination
und der nativen Platzierung des Auswahlmenüs am Feld.
Die Feldhilfe nutzt `ConfigInfoButton` und `AdminSidePanel` mit demselben Aufbau
wie die allgemeine Konfiguration. Sie zeigt Gruppe, Feldtitel, Beschreibung,
Standardwert aus der 2FA-Standardrichtlinie und die Eingaberegeln als Liste.

Die neue 2FA-Oberfläche verwendet `AccountPanelComponent`, native Vaadin-Eingaben,
Buttons, `Badge`, `TabSheet`, `ComboBox`, `Switch`, `Grid` und die bestehenden Cosmos-
Bestätigungsdialoge. Die Kontoverwaltung liegt im Tab „Portalzugang“. Einrichtung
und eingeschränkte Anmeldung sowie 2FA-Hilfe verwenden den Seitenrahmen mit
reduziertem Kopf. Die Marke ist statisch. Rechts stehen Sprachwahl, Trennlinie
und ab dem ersten erfolgreichen Anmeldenachweis der Initialenavatar mit Name
und E-Mail als reine Anzeige. Die Kontextzeile unterscheidet Anmeldung und
Kontosicherheit. Rückweg, Abmelden
und Abschluss liegen im jeweiligen Panel-Footer. Kurzlebige
QR-/Code-Dokumente werden geschützt eingebettet, Geheimnisse nicht als Flow-Komponenten-
Werte im dauerhaften Sitzungszustand gehalten.

Die wiederkehrende Codebestätigung, der geschützte Authenticatorwechsel und die
Benutzerhilfe folgen der ausgewählten Stitch-Variante A. Die Bestätigung verwendet
native `Tabs` im vorhandenen 42-px-Methodenstil: Authenticator-Code mit demselben
sechsstelligen `OtpField`-Add-on wie bei der Einrichtung oder ein normales natives
`TextField` für den vollständigen Ersatzcode. Ersatzcode-Eingaben verwenden Space
Mono und zeigen vier Gruppen mit je acht Zeichen als Platzhalter. Ein vollständiger
sechsstelliger Authenticator-Code löst in beiden OTP-Feldern über das native
`OtpField.addCompleteListener`-Ereignis dieselbe Prüfung wie der Footerbutton aus,
auch beim Tippen und Autofill. Der Button bleibt verfügbar. Die gewählte Methode bleibt beim Sprachwechsel
erhalten; sensible Eingaben werden dabei geleert. Die beiden
Hilfsaktionen liegen in „Zugangsprobleme und Alternativen“ unter der Trennlinie.

Die Bestätigungsdialoge für Anmeldeanbieter und 2FA-Aktionen verwenden für
App-Code und Ersatzcode gemeinsam `TwoFactorCodeInput` mit
`.two-factor-dialog-codes`. Beim Öffnen beginnt die Eingabe auf der ersten
App-Code-Stelle. Beide Felder verwenden Space Mono. Ein vollständiger App-Code
bestätigt nur beim Verbinden eines Anmeldeanbieters automatisch. Ersatzcodes und
alle übrigen Dialogaktionen erfordern den jeweiligen Bestätigungsbutton.

Vaadin `Details` ist anfangs geschlossen. Die Anmeldung und die Ersatzcode-Ausgabe
verwenden denselben nativen Vaadin-Button und dieselbe Gestaltung in `cosmos-mfa.css`.
Die Schalter bleiben bei Hover und Mausklick neutral. Der Tastaturfokus ist sichtbar.
Bei der Anmeldung ist die ganze Zeile klickbar, mit Titel und Fragezeichen links
und „Details“ rechts. Der blaue 16-px-Pfeil zeigt geschlossen nach rechts und
geöffnet nach unten. Darunter
folgen zwei Fragen in Sekundärfarbe mit rahmenlosen nativen Vaadin-Buttons.
Wie der Details-Schalter bleiben beide Aktionen bei Hover und Mausklick neutral
und zeigen den Tastaturfokus.
Beide zeigen Lucides `square-arrow-right-enter` mit 8 px Abstand hinter dem
Aktionstext, wie bei den App-Links mit Icon links.
Die Bestätigungsdialoge im angemeldeten Bereich verwenden für „Hilfe zur
Wiederherstellung“ dieselbe Zeile mit Frage, rahmenloser Aktion und Icon rechts.
„Zurück“ steht mit einem Linkspfeil als nativer Button links im Footer und führt
vom Faktorwechsel und von der Benutzerhilfe zur Codeabfrage.
Der Faktorwechsel bleibt ein eigener Ablauf mit Ersatzcodefeld und Verbrauchshinweis.
Die Benutzerhilfe verwendet den vorhandenen Panelkopf mit Tablers `lock-question`.
„Bestehende Anfragen“ zeigt ein natives Grid mit kompakten Zeilen für Nummer,
Betreff in der aktuellen Oberflächensprache, Aktualisierungszeit und gerahmtem
Statusbadge. In der Liste steht die Vorgangsnummer links, vertikal mittig neben
Betreff und Zeitstempel. Die ausgewählte Vorgangsnummer und die Nummer im
Verlaufskopf sind primärblau. Die übrigen Nummern nutzen die Standardtextfarbe.
Offene Vorgänge verwenden in Nutzer- und Adminansicht die Bernsteinfarben des
Sicherheitshinweises (`--cosmos-security-warning-*`). Vorgänge in Bearbeitung
haben blaue Badges, abgeschlossene graue und vom Nutzer geschlossene grüne.
Die Liste nutzt denselben Grundton
wie der Nachrichtenverlauf und die gemeinsame Tabellenfarbe für Hover und Auswahl.
Der Nachrichtenverlauf mit Vorgangsnummer und Anzahl nutzt einen nativen
`Scroller`. Eigene Nachrichten stehen rechts
mit dem vorhandenen Initialenavatar und einem Tooltip für Name und E-Mail.
Antworten aus der Administration stehen links mit dem Lucide-Icon `shield-cog`
und der neutralen Beschriftung „Systemadministration“. Der Verlauf richtet die
Nachricht nach ihrem Schreibkontext aus, auch wenn beide Seiten dasselbe Konto
verwenden. Der Avatar behält beim Überfahren den normalen Mauszeiger.
Eigene Nachrichten nutzen Primärblau mit weißer Schrift. Antworten der
Systemadministration nutzen die blaue Hervorhebungsfläche mit akzentblauem
Absender. Die Spitze zur Absenderseite übernimmt die Kontur der Sprechblase.
Beide Abschnittsüberschriften stehen in Versalien. Ein graues natives Badge zeigt
rechts über der Anfragenliste die Zahl der Vorgänge und am Verlauf die Zahl seiner
Nachrichten. Vorgangsnummern, Zeitstempel und der Zeichenzähler stehen in Space Mono.
Ein Mittelpunkt trennt Datum und Uhrzeit und steht im Verlauf auch vor dem Zeitstempel.
Der quadratische native Iconbutton „Aktualisieren“ mit Rahmen und übersetztem Tooltip
steht bei vorhandenen Anfragen am Verlauf oder an der Anfragenübersicht. Seine Höhe
und Breite folgen wie in der Benutzerverwaltung `--cosmos-control-height`. Die
Zustände übernehmen die gemeinsamen Buttonregeln. „Anfrage schließen“ steht im Verlauf links neben
„Aktualisieren“ und zeigt Lucides grünes `circle-check`-Icon.
Der Hinweis zum Identitätsnachweis nutzt
den gerahmten Infostil mit blauem Icon aus dem Faktorwechsel. Mit demselben Abstand
wie die übrigen Inhaltsabschnitte steht vor der nativen `TextArea`
der Sicherheitshinweis im gemeinsamen Stil `.cosmos-security-warning` aus
`cosmos-theme.css`. Variante B setzt auf eine dunkle bernsteinfarbene Fläche,
einen dezenten Rahmen und eine 4 px starke bernsteinfarbene Kante links.
Das Lucide-Warndreieck und die fett gesetzte Beschriftung verwenden dieselbe
Akzentfarbe. Der Text verwendet die helle Standardtextfarbe. Die Farben stehen
unter `--cosmos-security-warning-*` in `cosmos-tokens.css`.
Im Footer steht „Zurück“ links. Rechts stehen „Abmelden“ und die jeweilige
Hauptaktion gleich breit, auf sehr schmalen Bildschirmen untereinander.
`cosmos-mfa-confirmation.css` begrenzt die Gestaltung
auf `.two-factor-confirmation`, `.two-factor-restore` und `.support-recovery-help`;
Panels, Buttonzustände und Eingaben folgen weiterhin den vorhandenen Standards.

Anfragen und Antworten verwenden dieselbe native Vaadin-`TextArea`. Für den
Zeichenzähler rechts neben der Beschriftung nutzen sie die vorhandenen
`cosmos-notification-compose-*`-Klassen. `.support-message-input` in
`cosmos-mfa.css` gleicht die Eingabefläche an die Benachrichtigungen an und
erlaubt das vertikale Ziehen bei fester Breite.

Die Adminansicht „Ticketsystem“ übernimmt denselben Nachrichtenverlauf und den
gelben Sicherheitshinweis. In der Administration stehen Anfragen links und
Antworten rechts. Interne Vermerke haben eine eigene Kennzeichnung.
Die ausgewählten Ticketdetails stehen in einem eigenen `CosmosCardComponent`
mit dem bestehenden Panelrahmen. Der native `VerticalLayout`-Panelkopf gruppiert
`lock-question` im Cosmos-Iconrahmen, „Ticket #…“ in Primärblau und Status in einer
Orientierungszeile mit rechtsbündiger Aktualisierungszeit. Darunter steht der Betreff.
Eine dezente Trennlinie setzt Antragsteller und Bearbeiter als eigene Rollen ab;
auf schmalen Panelbreiten stehen die Rollen untereinander. Die Titelgestaltung übernimmt
`cosmos-panel-title`; Statusfarben und Identitätstooltips bleiben gemeinsam.
Im Kopf stehen Antragsteller und Bearbeiter in zwei gleich breiten Spalten mit
kleinen blauen Rollenüberschriften in Großbuchstaben ohne Doppelpunkt. Unter der
Rolle stehen ein 32-px-`OverlayUserIcon` und daneben Name sowie E-Mail in zwei
Zeilen. E-Mail-Adressen bleiben in Inter. Beim eigenen Bearbeiterkonto erscheinen
statt Name und E-Mail der Avatar und das grüne „Sie“-/„You“-Badge. Der gesamte
Bearbeiterblock einschließlich Überschrift und „Nicht Zugewiesen“ steht rechtsbündig.
Native `HorizontalLayout`-/`VerticalLayout`-Komponenten halten die Rollen getrennt;
lange Namen und Adressen können umbrechen. Der Dienst liefert diese Identität
ausschließlich für die berechtigte Adminansicht. Im administrativen Verlauf
zeigen Antragstellernachrichten denselben Avatar und den Namen. Öffentliche
Adminantworten und interne Vermerke zeigen dort ebenfalls den tatsächlichen Autor
mit 24-px-Initialenavatar und Namen. Der gemeinsame Avatar-Tooltip zeigt Name und
E-Mail zweizeilig. Für Antragsteller bleiben Adminantworten als „Systemadministration“
anonymisiert, ebenso bei gelöschtem Autorenkonto in der Administration.
Eine separate native Werkzeugleiste darunter enthält die vorhandenen Aktionen
„Übernehmen“ rechtsbündig, bei offenen Tickets als primäre Aktion, und
„Anleitung senden“ mit der unveränderten Vorschaufunktion. Links steht der native
Benachrichtigungsschalter mit der gemeinsamen Klasse `cosmos-config-switch`.
Die lokale Stitch-Anordnung setzt die Bahn links vor „Beobachten“ mit 8 px Abstand
am öffentlichen Label-Part. Schriftfarbe, Größe und Gewicht übernimmt der Schalter
aus der Konfiguration (12 px / 500). Ein nativer Tooltip erklärt die internen
Benachrichtigungen für neue Ticketnachrichten und die unveränderte Zuständigkeit.
Öffentliche Parts und der native Eingabeslot halten Label und
Schalter in derselben Zeile; bei Platzmangel kann die Beschriftung umbrechen.
Die Leiste bricht bei Platzmangel um.
Der Inhaltsbereich gliedert die Sicherheitsleitlinie als bestehende Infobox:
Infoicon in Cyan, „Sicherheitsleitlinie:“ weiß und fett, direkt anschließend die
Erklärung in Sekundärfarbe ohne erzwungenen Zeilenumbruch. Das Icon steht oben zur
ersten Textzeile. In der Werkzeugleiste bleibt „Beobachten“ links; die sichtbare
Aktion „Übernehmen“ beziehungsweise „Anleitung senden“ steht rechtsbündig.
Die Verlaufsüberschrift ist blau, der Nachrichten-Zähler bleibt grau. Unter dem
Verlauf steht ein statusabhängiger zentrierter, kursiver Hinweis mit 8 px Abstand
(`.support-admin-status-note`): Bei offenen, unzugewiesenen Tickets erklärt er die
nötige Übernahme. Abgeschlossene Tickets weisen auf die gesperrten Nachrichten
und Änderungen hin. Bei vom Antragsteller geschlossenen Tickets fordert er zum
Prüfen des Verlaufs und abschließenden Schließen auf. In Bearbeitung entfällt er.
Die vorhandenen Abschluss- und
Antwortbuttons liegen im gemeinsamen Panel-Footer. Werkzeugleiste und Footer
sind ohne sichtbare Aktionen ausgeblendet; Berechtigungen bleiben unverändert.
Die lokalen Anordnungsregeln stehen unter `.support-admin-ticket-*` und
`.support-admin-detail-*` in `cosmos-mfa-confirmation.css`.
Die Ticketkarten verwenden native `VerticalLayout`- und
`HorizontalLayout`-Komponenten: Nummer und Status mit Ungelesen-Zähler oben,
Betreff über die volle Breite, darunter Bearbeiter links und Zeitpunkt rechts.
Eine dezente Linie trennt diese Fußzeile. Die Texte können bei Platzmangel umbrechen;
die bisherige zweispaltige Anordnung der Nutzertickets bleibt erhalten.
Die Bearbeiterinfo der Admin-Karten verwendet denselben `OverlayUserIcon`
mit 24 px wie die Anfrageansicht; der Detailkopf verwendet 32 px. Der gemeinsame
Identitätstooltip zeigt oben den Namen und darunter die E-Mail-Adresse. Beim eigenen Bearbeiterkonto
steht daneben ein grünes natives „Sie“-/„You“-Badge mit Rahmen und den bestehenden
`--cosmos-reference-success-*`-Farben (`.support-assignee-self`).
Unzugewiesene Tickets zeigen „Nicht Zugewiesen“ / „Unassigned“ kursiv.
Die Identitätsdaten des Bearbeiters
liefert der Dienst ausschließlich im Adminzugang, niemals in der Nutzeransicht.
Unter `.support-admin-workspace` heißt die administrative Liste „Vorgänge“ / „Cases“.
Die Liste und ihr Kopf stehen gemeinsam in einem nativen `CosmosCardComponent`
mit den vorhandenen Panelklassen. Der Kopf verwendet `HorizontalLayout`,
links `list` im gemeinsamen `cosmos-panel-icon`-Rahmen und den Titel
mit `cosmos-panel-title`. Darunter steht eine eigene native `HorizontalLayout`-
Werkzeugleiste mit dem vorhandenen Aktualisieren-Button rechts, vor dem ersten Eintrag.
Links öffnet ein nativer Filterbutton den bestehenden `AdminSidePanel`-Baustein.
`SupportTicketFilterPanel` übernimmt die vorhandenen Klassen des Benutzerfilters:
`admin-user-filter-drawer`, `admin-filter-tools`, `admin-user-filters` und
`admin-filter-choices`. Oben steht die native `RadioButtonGroup` „Alle / Ungelesen“.
Darunter zeigen native Checkboxen die Statusmehrfachwahl in zwei Spalten. Bei
Auswahl übernehmen Rahmen, Fläche und Haken die Farben des jeweiligen Ticket-Badges.
Die Zuständigkeit unterscheidet alle, eigene und nicht zugewiesene Vorgänge.
Leere Statuswahl bedeutet alle Status. Innerhalb der Statuswahl gilt Oder,
zwischen Status, Zuständigkeit und Lesestand Und. Erst Anwenden übernimmt den Filter;
Zurücksetzen ändert den Entwurf, Abbrechen und Escape lassen den aktiven Stand bestehen.
Ein natives graues Badge am Button zählt aktive Filtergruppen. Aktualisieren und
Live-Antworten behalten diese Auswahl und vorhandene Antwortentwürfe bei.
Ein ausdrücklich geöffneter Ticketlink hebt entgegenstehende Listenfilter auf.
Dasselbe gilt für den Ticketsprung im Filterpanel. Ein natives `TextField` nimmt
die sichtbare Nummer mit oder ohne Raute auf. Ein nativer Button öffnet das Ticket.
Darunter erklärt die Info-Box `admin-filter-hint` aus der Benutzerverwaltung den
Suchumfang und das Zurücksetzen der Filter bei einem Treffer. Die lokale Anordnung
der Eingabezeile steht in `cosmos-mfa.css` unter `.support-filter-jump`.
Die Ticketnummer zählt nicht als Filtergruppe. Ein erfolgloser Sprung behält die
bisherige Auswahl bei und meldet den Fehler über die Standardnotification.
Beim direkten Sprung werden Listenfilter aufgehoben, damit auch ein gerade als
gelesen quittiertes Ticket ausgewählt bleibt. Ein aus dem Ungelesen-Filter gelesener
Vorgang verschwindet nur aus der Liste, seine geöffnete Detailansicht bleibt erhalten;
das native Grid enthält dann keine ausgewählte Zeile außerhalb seiner Daten.
Status- und Zuständigkeitsfilter bleiben für die Detailauswahl maßgeblich.
Die Zuständigkeitsoptionen verwenden
12 px / 18 px, Gewicht 400 (ausgewählt 500) und die gemeinsamen Textfarben.
Der Footer übernimmt allein `admin-drawer-actions` aus der Benutzerverwaltung;
zusätzliche Dialogaktionsklassen würden dessen Raster überschreiben.
Die native Tab-/Ticketauswahl meldet ihren Zustand an die Adminansicht. Diese hält
`tab=all`/`tab=recovery` und die öffentliche Ticketnummer ohne Raute in der URL,
ohne neu zu navigieren. Beim Browser-Refresh werden Tab und Auswahl daraus
wiederhergestellt; unveränderte Live-Updates schreiben die Adresse nicht erneut.
Die Filter grenzen ausschließlich bereits berechtigte Admin-Daten ein und verändern
weder Zuständigkeit noch Status oder die fachliche Zugriffskontrolle.
Titelschrift und Rahmenstil übernimmt er vom Ticketsystem-Panelkopf;
der kompakte Vorgänge-Kopf und der Pagination-Footer verwenden lokal 12 px
umlaufenden Innenabstand.
Der Vorgänge-Kopf verwendet den 32 × 32 px großen Cosmos-Iconrahmen mit dem
16 × 16 px großen Lucide-Icon `list`.
Der äußere Panelrahmen umschließt die Liste ohne zusätzlichen Gridrahmen;
Eintragsinhalte, Zustandsfarben und Auswahl bleiben unverändert.
Eine leere Vorgangsliste übernimmt den zentrierten Leerzustand der Benutzerverwaltung
mit blauem 24-px-`list`-Icon ohne Rahmen und einem Hinweis passend zu den aktiven Filtern.
Native Tabs gliedern die Kategorien, ein natives Grid
zeigt die Anfragen in ihrer tatsächlichen Inhaltshöhe. Ein nativer Scroller begrenzt
die Listenhöhe ohne geschätzten Leerraum unter den Einträgen; ein weiterer Scroller
begrenzt den Verlauf. Unter der Liste liegt der gemeinsame `cosmos-panel-footer`.
`CosmosPagination` zeigt links die native Auswahl der Seitengröße, mittig den
Trefferbereich und rechts die beiden Pfeilbuttons. Der Modus `PREVIOUS_NEXT`
ist auf die Vorgangsliste begrenzt. Die lokale Anordnung steht in
`cosmos-mfa-confirmation.css` unter `#support-queue-footer` und `#support-pagination`.
Die Vorgangsliste erhält mindestens 330 px tatsächliche
Panelbreite. Zwischen Liste und Details liegen 16 px ohne zusätzliche Trennlinie;
der gemeinsame Arbeitsbereich hat 20 px Innenabstand, auf Smartphones 16 px.
Unter 820 px tatsächlicher Panelbreite stapeln sich beide Bereiche;
die Containerabfrage berücksichtigt auch den Platzbedarf der Seitennavigation.
Ein nativer Iconbutton neben dem Zeitpunkt maximiert das ausgewählte Ticket mit
Lucide `expand`. Dabei wird nur die Vorgangsliste ausgeblendet; Auswahl, Filter und
Antwortentwurf bleiben erhalten. `minimize` stellt die geteilte Ansicht wieder her.
Tooltips und `aria-pressed` erklären den Zustand auch bei Tastaturbedienung. Wird
die Auswahl aufgehoben, erscheint die Vorgangsliste automatisch wieder.
Die native Tableiste verwendet `border-box`, reserviert ihre volle Höhe und schließt
ohne zusätzlichen Inhaltsabstand am Panel ab. Der Auswahlhinweis verwendet ein natives
`VerticalLayout` mit dem bestehenden `.cosmos-notification-empty`-Stil: mittig,
blaues 36-px-Lucide-Icon `mouse-pointer-click` ohne Rahmen, die Überschrift
„Kein Ticket ausgewählt“ mit 18 px / 600 und darunter ein kurzer Hinweis in
Sekundärfarbe. Zwischen Icon, Überschrift und Hinweis liegen jeweils 8 px.
Der Titel und der Hinweis wechseln zwischen DE und EN.
Diese lokale Übernahme aus Stitch lässt die zusammenhängenden Ticketzeilen
ohne zusätzliche Kartenrahmen und den mobilen Umbruch unverändert.
In `cosmos-mfa-confirmation.css` sind diese Regeln auf den
Ticketarbeitsbereich begrenzt. Die Sprechblasen nutzen die gemeinsamen
Nachrichtenklassen und orientieren sich am jeweiligen Absender.

`RecoveryInstructionsComponent` stellt die feste Anleitung mit nativen
`HorizontalLayout`-/`VerticalLayout`-Komponenten und vier nummerierten `Badge`-
Komponenten dar. Warnung und Information nutzen die vorhandenen Hinweisbausteine.
Die lokale Gestaltung liegt unter `.support-instruction-*` in
`cosmos-mfa-confirmation.css`. Die kompakte Anleitung im Verlauf verwendet ein
natives `Details` und den gemeinsamen `DetailsToggleButton`. Auch die bestehenden
2FA-Details verwenden diesen Button mit der unveränderten Klasse
`.two-factor-details-toggle`. Deren gemeinsame Gestaltung liegt in `cosmos-theme.css`,
damit der Anwesenheitsschalter denselben Stil im Benutzermenü nutzt.
Pfeil und Zugänglichkeitszustand werden gemeinsam
aktualisiert. Die feste Textvorlage enthält sieben durch Leerzeilen getrennte
Abschnitte: Kopf, vier Schritte, Warnung und Grenze. Kopf und Schritte enthalten
jeweils eine Überschrift und einen Zeilenumbruch vor der Beschreibung. Der Text
wird ausschließlich als Text gerendert, niemals als Nachrichten-HTML.

`cosmos-mfa.css` begrenzt die neue Gestaltung auf `.two-factor-view`,
`.cosmos-mfa-settings` und `.two-factor-account-entry`. Panel-, Button- und
Eingabestandards bleiben unverändert. Das Anmeldepanel ist höchstens 640 px breit.
Der QR-Einrichtungsschritt verwendet die freigegebene Variante B mit maximal 920 px:
links native `Tabs` für QR-Code oder manuellen Schlüssel, rechts zwei
Abschnitte mit den vorhandenen blauen Tabler-Zahlenicons aus der Anbietereinrichtung.
Die erste Anleitung enthält die drei frei wählbaren App-Empfehlungen als rahmenlose
Links mit dem vorhandenen blauen Tabler-External-Link-Icon und 12 px / 600 wie in
der Anbietereinrichtung. Ein Hinweis darunter erklärt die Unterstützung weiterer
TOTP-Apps nach RFC 6238. Der Scan-Hinweis steht direkt unter dem QR-Code; Schritt 2
beschreibt nur die Codeeingabe und endet mit einem Doppelpunkt. Die App-Einleitung
endet nach „beispielsweise:“, ohne die verlinkten Namen zusätzlich aufzuzählen.
Unter dem Kompatibilitätshinweis trennt eine dünne Linie die beiden Schritte.
Die vertikale Trennlinie zwischen Einrichtungsmethode und Anleitung reicht über
die volle gemeinsame Inhaltshöhe, auch wenn der QR-Bereich höher als die Anleitung ist.
Die Zahlenicons sind 20 × 20 px groß. Unter 760 px
stehen die Bereiche untereinander; statt der vertikalen Trennlinie trennt dann
eine horizontale Linie mit 24 px Abstand zu den Schritten die beiden Bereiche.
Der Inhalt verwendet die normale dunkle
Contentfläche, Kopf und Footer die bestehende Panelfläche.
Die Panelköpfe der Authenticator-Einrichtung und der Anmeldebestätigung sowie die
2FA-Zeile unter „Sicherheit & Zugang“ zeigen das Lucide-Icon `shield-keyhole`.
Das native Badge „Eingerichtet“ verwendet den kompakten grünen Kontostil mit Rahmen
und ohne Statuspunkt.
Die Tabs schalten normale Inhaltscontainer um; ein zusätzliches `TabSheet` und
ein eigener Scrollbereich entfallen. `cosmos-mfa-methods.css` gestaltet Form,
Innenabstände, Schrift und Auswahlfarbe.
Die Methodenauswahl ist 42 px hoch und zeigt die Lucide-Icons
`qr-code` und `key-round` mit jeweils 16 px neben den Beschriftungen.
Der aktive Tab verwendet wie die Profilbild-Umschaltung weiße Schrift mit Gewicht
600 und ein blaues Icon; inaktive Beschriftungen bleiben Secondary mit Gewicht 500.
Der QR-Code steht horizontal
und vertikal mittig im Bereich unter den Tabs. Der manuelle Schlüssel erscheint
in zwei Zeilen mit je vier Vierergruppen in einem schreibgeschützten Feld mit
normaler Labelschrift und ausschließlich blauem Schlüsselicon sowie
einem nativen Vaadin-Iconbutton als Suffix rechts. Das Feld ist eine native,
schreibgeschützte `TextArea` mit nativem Feldlabel und zwei festen Zeilen. Sie nutzt
die volle Breite der manuellen Ansicht; die Schlüsselzeilen sind horizontal
zentriert. Das Label verwendet wie die bestehenden Formularfelder 12 px / 500;
der Feldrahmen ist wie bei schreibgeschützten Textfeldern gestrichelt.
Das Kopiericon ist in Ruhe neutral, bei Hover heller und bei Feldfokus
wie andere Feldaktionen blau. Es bekommt keine eigene Hoverfläche oder Umrandung.
Nur der Schlüsseltext verwendet Space Mono Regular mit 14 px / 20 px. Die Datei
`custom-theme/fonts/SpaceMono-Regular.ttf` und `SpaceMono-LICENSE.txt` liegen neben
Inter.
Die Schrift wird lokal geladen. Feldlabel und übrige Oberfläche verwenden weiterhin Inter.
Die manuelle Ansicht
verwendet `VerticalLayout`. Rahmen, Schrift und Fokus folgen dem bestehenden
Feldstandard. Der Browser lädt den Wert vom geschützten No-Store-Endpunkt und setzt
ihn ausschließlich in das öffentliche `inputElement` des schreibgeschützten Felds.
Der Hostwert und das serverseitige Flow-Modell bleiben leer. Dadurch gelangt der
Seed weiterhin nicht in den JDBC-Sitzungszustand. Die frühere HTML-/IFrame-
Sonderdarstellung entfällt. Kopiert wird ohne
Gruppierungsleerzeichen und Zeilenumbruch. Die gewählte Einrichtungsmethode bleibt
beim Sprachwechsel erhalten.
Die manuelle Anleitung erklärt die Schlüsseleingabe und die Auswahl zeitbasierter Codes
in einem vollständigen Satz. Sie ist über die volle Breite zentriert und bricht natürlich um.
Der Scan-Hinweis direkt unter dem QR erscheint nur im QR-Tab. Schritt 2 fordert
in beiden Tabs nur zur App-Codeeingabe auf.

Die erste Codebestätigung verwendet das freigegebene Flowing-Code-Add-on `OtpField`
mit sechs sichtbaren Kästchen und einem gemeinsamen Eingabefeld. Einfügen,
Auswählen, Rücktaste, Pfeiltasten und Autofill übernimmt der Baustein. Die sechste
Ziffer löst automatisch die Bestätigung aus; der vorhandene Button bleibt zusätzlich
bedienbar. Längere Ersatzcode-
und E-Mail-Code-Felder bleiben unverändert. Die Gestaltung ist auf
`.two-factor-otp` in `cosmos-mfa.css` begrenzt und nutzt die öffentlichen Parts
und `--fc-otp-field-*`-Variablen.
Die Kästchen verwenden Space Mono bei der Einrichtung und bei der späteren
Anmeldebestätigung.
Label, 4 px Abstand und Position des Pflichtfeldpunkts übernehmen die bestehenden
Vaadin-Feldregeln mit 12 px / 16 px Labelschrift und 14 px / 14 px für den Punkt.
Die native Eingabe über den Slots bleibt durch eine auf `.two-factor-otp`
begrenzte Regel unsichtbar. Allgemeine Formularfarben dürfen den vollständigen
Eingabewert nicht zusätzlich über die sechs angezeigten Ziffern legen. Die Eingabe
bleibt für Fokus, Tastatur, Einfügen und Autofill vorhanden.
Einzelne Punkt-Platzhalter setzt die native Add-on-API. Das nächste leere Kästchen
bleibt auch ohne echten Tastaturfokus blau markiert; Eingabe und Positionswechsel
steuert weiterhin das Add-on. `cosmos-mfa-otp.css` wird gezielt in dessen Shadowroot
geladen und gestaltet ausschließlich die dokumentierten `slot`-Parts und deren
`active`-/`filled`-Zustände. Beide OTP-Felder verwenden den nativen Autofokus, damit
die Eingabe beim Öffnen mit dem ersten Kästchen beginnen kann. Ob dabei die virtuelle
Tastatur erscheint, entscheidet der mobile Browser. Fehler-, Disabled- und
Read-only-Zustände behalten Vorrang.
Die vorhandene Vaadin-Frontendversion bleibt auch für `field-base` maßgeblich;
die ältere Paketdeklaration des Add-ons darf die gemeinsamen Felder nicht herunterstufen.

Die Ersatzcodeausgabe verwendet einen nativen `Button` mit einem per öffentlicher
Element-API registrierten `DownloadHandler` über `RecoveryCodeDownload`. Wie
„Alle Codes kopieren“ verwendet er ausschließlich die gemeinsamen Buttonregeln
aus `cosmos-theme.css`; lokale Downloadbutton-Styles entfallen.
Der Abruf prüft die aktuelle Berechtigung und
verbraucht die Ausgabe weder bei Erfolg noch bei Fehler. `recovery-code-download.js`
prüft vor dem Dateidownload Status, PDF-Inhaltstyp und PDF-Signatur. Erst danach
startet der Browser den Download. Fehlerantworten und Anmeldeseiten werden nicht
als Datei angeboten; die Browserseite bleibt stehen und zeigt die Standardnotification. Ein nativer `Switch`
bestätigt die Sicherung und schaltet den einzigen Abschlussbutton „Codes gesichert“ frei. Die zusätzliche
Aktion „Zurück zum Konto“ entfällt ausschließlich in der Ersatzcodeausgabe. Nur die ausdrückliche
bestätigte Aktion verwirft die einmalige Ausgabe; reine Navigation ist kein Download.
Die gemeinsame `.account-panel-body`-Regel in `cosmos-account.css` setzt 24 px
umlaufenden Innenabstand und unter 640 px die vorhandenen 20 px. Persönliche Daten,
Tickets und die normalen 2FA-Ansichten einschließlich Einrichtung und Ersatzcodes
übernehmen diesen Standard. Bereiche mit eigener innerer Aufteilung
wie Sicherheit und Sitzungen behalten ihre bewusst randlosen äußeren Inhaltsflächen.
Die separat freigegebene Einführungskarte behält 32 px, unter 480 px weiterhin 20 px.
In der 2FA-Kontokarte steht eine einzelne Aktion wie „Einrichten“ ab 651 px rechts
neben der Beschreibung und ist vertikal mittig ausgerichtet. Mehrere Aktionen und
schmale Ansichten behalten ihre vorhandene separate Aktionszeile.
Die freigegebene Variante B verwendet auf maximal 920 px ein responsives Grid:
links die lokale Tresorillustration `images/two-factor-vault.svg` mit dezentem blauem
Glow aus der Primärfarbe und drei Hinweise aus nativen `HorizontalLayout`-/`VerticalLayout`-
Komponenten. Überschrift und Erklärung haben keinen zusätzlichen Zwischenabstand.
Kopieren und PDF stehen darunter nach einer horizontalen Trennlinie in einem nativen
`VerticalLayout`, beide über die volle Spaltenbreite. Rechts stehen ein natives
`Details` mit anfangs aufgeklappter Codeübersicht und der vorhandene Sicherungs-Switch.
Die Sicherungsbestätigung bleibt am unteren Rand der rechten Spalte, auch bei
eingeklappter Codeübersicht.
Über den Codes stehen der blaue Schlüssel und „Ihre 10 Ersatzcodes“. Rechts steuert
ein rahmenloser nativer „Details“-Button mit blauem Pfeil nach rechts (geschlossen)
beziehungsweise unten (offen) den Bereich,
wie bei den Admin-Benachrichtigungen; `aria-controls` und `aria-expanded` folgen dem Zustand.
`cosmos-mfa-recovery.css` kapselt ausschließlich diese Ansicht.
Unter 760 px stehen beide Bereiche untereinander; eine horizontale ersetzt dann
die vertikale Trennlinie. Kopf und Footer verwenden Panelfläche, der Inhalt die
normale Contentfarbe; Überschriften sind Primary, Hinweise Secondary und Icons blau.
Der native Sicherungs-Switch folgt dem öffentlichen Login-Switch mit 34 × 20 px,
14-px-Marker, 9 px Labelabstand und 12 px / 400 für den Secondary-Text.
Seine native Reverse-Variante setzt die Beschriftung links und den Schalter rechts.
Die lokale Grid-Aufteilung und die öffentlichen Parts halten den Schalter ganz am
rechten Spaltenrand; die Beschriftung nutzt den verbleibenden Platz links.
Zusätzliche Rahmen oder Änderungen an der Tresorillustration entfallen.
Ein-/Ausklappzustand und Sicherungsbestätigung bleiben beim Sprachwechsel für
dieselbe Ausgabe erhalten. Kopieren und PDF funktionieren auch bei eingeklappten Codes.
Die Ausgabe bleibt bis „Codes gesichert“ oder zum Ende der berechtigten Sitzung
verfügbar; die Fünf-Minuten-Frist gilt nur für Sicherheitsbestätigungen.
Zehn native schreibgeschützte `TextField`-Komponenten zeigen die Codes einzeln mit
gestricheltem Rahmen und einem Kopierbutton im Suffix. Space Mono mit 12 px / 20 px
erleichtert das Ablesen. Die Schrift ist am Eingabe-Part und am nativen Eingabetext
festgelegt. Die PDF bettet Inter für ihre normalen Texte und Space Mono für die Codes
aus den mitgelieferten Schriftdateien ein. Die Codefelder sind 36 px hoch und stehen mit 6 px Abstand
untereinander; der Rahmen folgt dem vorhandenen Eingabestandard.
Die Kopiericons sind neutral, bei Hover heller und bei Feldfokus blau, ohne eigene
Hoverfläche. Ein geschützter No-Store-Abruf setzt die Werte ausschließlich in die
öffentlichen `inputElement`-Eingaben. Hostwerte und gespeicherter Flow-Zustand bleiben leer.

Die Kontomails deklarieren Inter in allen MJML-Vorlagen und den daraus erzeugten
HTML-Dateien. `mj-font` bindet `custom-theme/email-fonts.css` über die konfigurierte
Basisadresse ein; die Schrift stammt aus den lokalen Theme-Ressourcen. Es gibt keinen
externen Schriftdienst. Mailprogramme ohne Webfonts oder mit blockierten externen
Inhalten verwenden Roboto, Arial oder Sans-Serif. Die Basisadresse muss vom Empfänger
erreichbar sein; eine lokale Entwicklungsadresse ist außerhalb des Rechners ungeeignet.
Beim MJML-Erzeugen `fonts: {}` setzen: Die `mj-font`-Deklaration liefert Inter,
automatische MJML-Standardimporte von Google Fonts werden damit vermieden.

Der Rollenbereich übernimmt die bestehende Anbietertabform, stellt
Regeln zweispaltig dar und wechselt unter 650 px auf eine Spalte. Neue lokale
Details bleiben bis zur gemeinsamen Designabnahme lokal.

Der freigegebene verpflichtende Einrichtungseinstieg folgt Stitch-Variante A:
zentrierte Schlossillustration, Überschrift, Erklärung und die nativen Aktionen
„Abmelden“ / „Einrichten“. `.two-factor-introduction` in `cosmos-mfa.css` begrenzt
die Karte auf 440 px, mit 32 px Innenabstand, einem 128 × 112 px großen Motiv
und Inter 22 px / 600 für die Überschrift. Unter 480 px gelten 20 px umlaufender
Innenabstand und 20 px Überschrift. Bei Platzmangel brechen die Aktionen untereinander um.
Buttonhöhen, Farben und Icongrößen folgen dem vorhandenen Standard.
Der Aktionsbereich verwendet den Standard-Panel-Footer mit durchgehender oberer
Trennlinie, Panelfläche und rechtsbündigen, bei Platzmangel umbrechenden Buttons.
„Abmelden“ verwendet dort die bestehende rote Aktionsvariante mit Rahmen und
dezentem Hintergrund (`cosmos-delete-button`).
Der öffentliche Seitenrahmen bleibt erhalten. Die Illustration liegt lokal als
`images/two-factor-locks.svg`. Nur dieser Einstieg blendet den normalen Panelkopf aus.

Die druckbare PDF verwendet eine eigene Classpath-HTML-/CSS-
Vorlage unter `pdf/`, das vorhandene Service-Logo und einen hellen Druckhintergrund.

Die administrative Ticketbearbeitung verwendet für die Editorumschaltung native
`Tabs` mit der vorhandenen Gestaltung `.two-factor-method-tabs`. Die lokalen
Ergänzungen in `cosmos-mfa-confirmation.css` kennzeichnen interne
Vermerke und deren Editor mit der gemeinsamen Systempalette (`.support-internal-message`,
`.support-admin-internal`). Im internen Modus verwendet die Speicheraktion die
Systemfarbe mit dem unveränderten nativen Primary-Buttonstil. Auch der
Pflichtfeldpunkt am Vermerkfeld verwendet den Systemakzent.
Die lokale Regel bindet nur dessen bestehende Farbvariablen an den Systemakzent,
für lesbare weiße Beschriftung etwas abgedunkelt. Beim Wechsel zur öffentlichen
Antwort gelten wieder die normalen blauen Farben. Im internen Modus bündelt die bestehende
Sicherheitswarnung das Verbot sensibler Inhalte und den Hinweis auf die ausschließlich
administrative Sichtbarkeit; eine zusätzliche Infobox entfällt. Öffentliche Nachrichten behalten
ihre Sprechblasenfarben. `.support-history-event` zentriert kompakte Systemereignisse
zwischen Nachrichten; Zustandsfarben verwenden die vorhandenen Ticket-Badges.
Erstellung verwendet Lucides `message-square-plus`, Statuswechsel Tablers
`status-change`. Beide lokalen SVGs werden als native `SvgIcon` mit 16 × 16 px
und dem vorhandenen blauen Akzent unter `.support-history-event-icon` angezeigt.
Panelkopf, Werkzeugleiste und Aktionsfooter bleiben davon unabhängig.
Der native Verlaufsscroller auf Antragseite und in der Administration verwendet
die globale Scrollgestaltung. Lokal bleiben `.support-message-scroll` und
`.support-message-thread` für die Geometrie zuständig. Die reservierte Scrollbahn verhindert einen
Breitensprung. Rechts bleiben einschließlich der 11-px-Sprechblasenspitze 12 px
Abstand zur Bahn; Form und Rahmen der Sprechblasen bleiben erhalten.
