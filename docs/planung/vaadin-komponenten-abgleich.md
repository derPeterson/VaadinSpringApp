# Vaadin Flow und Cosmos

## Entscheidung und Ausgangspunkt

Die Anwendung bleibt bei **Vaadin Flow in Java**. Standardbedienung kommt aus
spezialisierten Flow-Komponenten. Cosmos bleibt die gemeinsame Gestaltung.
Anwendungsbausteine dürfen mehrere native Komponenten zu einer fachlichen Einheit
zusammenfassen, etwa eine Sitzungskarte, einen Ticketfilter oder die Pagination.
Ein eigener Ersatz für eine Vaadin-Komponente braucht einen konkreten Grund und
die vereinbarte Freigabe. Maßgeblich ist die [Theme-Anleitung](../theme.md#native-vaadin-bausteine-zuerst).

## Offene Komponentenpakete

Die begonnene Umsetzung ist lokal nach `main` zwischenintegriert. Weitere
Arbeiten laufen auf
`codex/vaadin-komponenten-folgearbeiten`. Die Badge-Darstellungen und
einheitlichen Seitenpfade sind vom Benutzer abgenommen. Ihre geltenden Regeln
stehen im [Theme-Standard](../theme.md).

### Freigegebener Nachtauftrag: übrige Formulare

Branch und Ausgangspunkt: `codex/vaadin-komponenten-folgearbeiten`, `c7b5a356`.
Aussehen, Verhalten, Feldreihenfolge, Rechte und Speichern bleiben erhalten.
Vorhandene native Zeilen/Spalten und fachlich passende Sonderbausteine bleiben bestehen;
gewöhnliche CSS-Feldraster werden durch native Formular-/Zeilenlayouts ersetzt.

- [x] 1. Allgemeine Anwendungskonfiguration einschließlich Mailqueue-Allgemein
  auf `FormLayout` und native Eingabe-/Feldhilfezeilen übertragen.
  Quellreview und gezielte Java-Prüfungen sind erfolgreich; der abschließende
  Bild-/Bedienvergleich bleibt Bestandteil von Punkt 4. Die unveränderten
  Vorher-Aufnahmen aller Formulargruppen liegen isoliert in DE/EN vor.
- [x] 2. 2FA-Rollenregeln und Google-/Microsoft-Konfiguration prüfen und umstellen.
  Native Formularpaare und Feldhilfezeilen sind umgesetzt, Quellreview und
  57 gezielte Java-Prüfungen erfolgreich. Der Bildvergleich folgt in Punkt 4.
- [ ] 3. Übrige Formularstellen einzeln sichten; sinnvolle native Übernahmen in
  Mailtest, Benachrichtigungen, Kontodialogen und öffentlichen Formularen vornehmen.
  Bereits passende native Anordnungen mit Begründung in der Fachdokumentation einordnen.
- [ ] 4. Isolierter Vorher-/Nachher-Vergleich einschließlich Superadmin-Varianten,
  DE/EN bei Desktop-/Smartphonebreite, Feldhilfen, Bedienung und Regressionen.
- [ ] Abschließend große Testrunde mit vollständigem Reactor, Offlineprüfungen,
  Browser `all` DE/EN und lesender Sonar-Einordnung auf dem Feature-Branch.

Nach jedem der vier Punkte Review einschließlich Lesbarkeit und separater Commit.
Für diese Pakete und die abschließende große Testrunde sind isolierte Browserläufe
freigegeben. Vorher-Aufnahmen am gesicherten unveränderten Ausgangsimage erfassen,
Nachher DE/EN am selben finalen Image. Eigene Testdaten/-dienste; die laufende
Entwickleranwendung bleibt unverändert. Kein Merge, Push oder Release beauftragt.
Die beiden offenen Queue-Nachtests werden in der großen Testrunde mitgeführt.
Die abschließende menschliche Designabnahme bleibt offen.

`CosmosMetricCard` wird für „Gesamt“ und „Ungelesen“ im Postfach sowie die
Konten-Karte in der Benutzerverwaltung verwendet.
Eine native Vaadin-`Card` mit nativen Text-/Layoutkomponenten
übernimmt die bestehende neutrale und blaue Darstellung. Beschriftung, Wert
und deren Schriftgestaltung sind von außen konfigurierbar.
Der isolierte Vorher-/Nachher-Vergleich in DE/EN bei 1280 und 390 px ist
bestanden: Alle acht Kartenausschnitte und ihre gemessenen Stilwerte sind
identisch. Die Konten-Karte behält ihre Maße von 96 × 59 px und die bisherige
dezente blaue Metadatenpalette. Gesamtzahl bei Anlage und Löschung,
Filterunabhängigkeit, Sprachwechsel und zugängliche Beschriftung sind geprüft.
Die gezielte Suite `metric-cards` hat je Sprache vier erfolgreiche Browserfälle;
314 gezielte Java-Prüfungen und die Stilprüfungen sind ebenfalls bestanden.
Weitere Badge-Zähler bleiben unverändert. Die Kennzahlkarten sind vom Benutzer
abgenommen.

Der native Avatar-Pilot im reduzierten 2FA-Header ist vom Benutzer abgenommen.
Die Übernahme in die übrigen Avatarstellen ist freigegeben. Ein gemeinsamer
nativer `CosmosAvatar` ersetzt den Bild-/Initialenkern von `OverlayUserIcon`.
Statushülle, Tooltip, Bildberechtigungen und Ereignisaktualisierung bleiben
erhalten. Der vorhandene Canvas-Bildeditor bleibt eine getrennte Sonderlösung.
Die native SVG-Schrift hat geringfügig andere Kantenglättung als der bisherige
HTML-Text. 202 gezielte Java-Prüfungen und die Stilprüfungen sind bestanden.
Die isolierte Suite `avatars` prüft Header, Sidebar, Menü, Benutzertabelle und
Bestätigung, Bildmodi/-fehler, Status-/Namenswechsel sowie Ticketidentitäten und
2FA-Abläufe. Vorher und nachher sind in DE/EN bei 1280 und 390 px jeweils zehn
Browserfälle erfolgreich. Beide abschließenden Sprachläufe verwenden dasselbe
unveränderte Image. Alle 56 gepaarten Messwertsätze stimmen hinsichtlich Größe,
Rahmen, Palette, Statuspositionen und Schriftgröße überein; die
SVG-Kantenglättung kann geringfügig abweichen. Die 48-px-Initialen in Kontokarte
und Menü sind auch optisch auf die bisherige Höhe abgeglichen. Der vollständige
Anwendungsbuild einschließlich regulärer Tests ist erfolgreich. Der Benutzer
hat auch die Übernahme der übrigen Avatarstellen anhand des Vergleichs abgenommen.

### Abgenommener Pilot: Persönliche Daten

Das Profilformular in `AccountSettingsComponent` ist auf natives `FormLayout`
umgestellt. Bestehende Felder, Reihenfolge, Beschriftungen, E-Mail über die ganze
Zeile, Speichern, Validierung und Formularschutz bleiben erhalten. Die bisherige
Bildschirmgrenze zwischen einer und zwei Spalten bleibt ebenfalls erhalten.

Der isolierte Vorher-/Nachher-Abgleich ist in DE/EN auf Desktop und Smartphone
mit je fünf Browserfällen erfolgreich. Alle vier Panelaufnahmen sind pixelgleich.
Auch die Zwischenbreiten 639, 640, 767, 768 und 1024 px behalten ihre Anordnung;
alle 14 gepaarten Messwertsätze stimmen bis auf browserübliche Rundung unterhalb
eines Pixels überein. Speichern, Validierung, Sprachwechsel und Formularschutz
sind geprüft. Die 85 gezielten Java-Prüfungen, Stilprüfungen und der vollständige
Anwendungsbuild sind bestanden. Die Vorher-/Nachher-Screenshots für beide
Gerätebreiten wurden im Chat gesichtet; der Benutzer hat den Piloten abgenommen.
Die beiden offenen Queue-Nachtests bleiben für die Nacht im zentralen Testbedarf.

### Abgenommen: Registrierung und Benutzerformular

Registrierung sowie Benutzer anlegen/bearbeiten verwenden natives `FormLayout`.
Feldreihenfolge, responsive Umschaltgrenzen, Abstände, Beschriftungen, Validierung,
Rechte und die bestehende Passwortbehandlung bleiben erhalten. Feldpaare und
Passworthinweise bleiben fachlich gruppiert; Kopf, Aktionen und Anmeldung unverändert.

Die gezielte Suite `account-forms` ist vorher und nachher in DE/EN bei 1280 und
390 px mit je acht Browserfällen erfolgreich. Beide abschließenden Sprachläufe
verwenden dasselbe unveränderte Image. Alle zwölf Feldmesswertsätze stimmen
überein, ebenso die Scrollbereiche. 18 der 20 gepaarten Aufnahmen sind pixelgleich;
zwei englische Smartphone-Aufnahmen haben lediglich 13 beziehungsweise 25
abweichende Kantenpixel an Info-Icon/Panelrundung (ein bis zwei Farbstufen).
172 gezielte Java-Prüfungen, Stilprüfungen und der vollständige Anwendungsbuild
sind bestanden. Die Vorher-/Nachher-Vergleiche für Desktop und Smartphone wurden
im Chat bereitgestellt; der Benutzer hat das Folgepaket abgenommen.
Die Queue-Nachtests bleiben für die Nacht im zentralen Testbedarf zurückgestellt.

## Abgenommene Pakete und Prüfstand

Die abgenommenen Auswahlfelder, die bestehende Formularwarnung und die E-Mail-/
Benutzerverwaltungsoberflächen sind keine offenen Abnahmepunkte mehr. Ihre
Funktionen stehen in der aktuellen [Anwendungsdokumentation](../anwendung.md)
und im [Theme-Standard](../theme.md). Offene automatisierte Nachtests bleiben
unabhängig davon im [Testbedarf](../testbedarf.md) erhalten.

### Mailqueue-Bereinigung

Die gemeinsame manuelle und optionale automatische Bereinigung ist umgesetzt
und vom Benutzer abgenommen.
Der [Mailqueue-Vertrag](../mailqueue.md#ergebnisse-und-wiederholung) beschreibt
SMTP-Ablehnungen, ungeklärte Ausgänge, Mindestalter, Intervalle und Grenzen.
Schalter, Zeitfelder, Bestätigung und Ergebnisanzeige sind gesichtet.
Der feste Zeilenumbruch der Anzahl bereinigter Aufträge ist auf Desktop und
Smartphone manuell abgenommen. Automatisierte Nachweise und verbleibender
Testbedarf stehen im [Prüfstand](../status.md) und im [Testbedarf](../testbedarf.md).

Die abgestimmte Variante A ist als innere Konfigurationskarte unter dem
Superadmin-Reiter Assistenten umgesetzt. Die fünf allgemeinen Einstellungen
bleiben im Reiter Allgemein. Der neue Historienwert ist unabhängig von der
Automatik bedienbar. Auch die konfigurierbare D:-Ablage der Browserläufe ist
vom Benutzer abgenommen. Die sichtbare Laufhistorie bleibt eine spätere
Aufgabe in [TODO](../../TODO.md).

### Segmentierte Auswahl vereinheitlichen

- Native `Tabs`/`Tab` sind über `CosmosSegmentedTabs` in Profilbild, Posteingang,
  Glockenvorschau, Ticket-Nachrichtenfilter und Zugangsfilter der Benutzerverwaltung
  umgesetzt. Bestehende native OTP-, QR-/Schlüssel- und Antwort-/Vermerk-Umschalter
  verwenden dieselbe gemeinsame Gestaltung.
- Der Benutzer hat die bisherigen Seitenreiter ausdrücklich ausgenommen. Konto-
  sicherheit, Anbieter-/Rollen-TabSheets und Ticketkategorien behalten ihre Gestaltung.
  Die normale Zuständigkeitsauswahl bleibt eine native RadioButtonGroup.
- Die bisherige Gestaltung der segmentierten Auswahl ist abgenommen. Der gezielte
  Nachtest einschließlich Vorher-/Nachher-Vergleich, Filterwirkung, Zählern,
  Sprachwechsel, Tastatur und Handzeiger ist in DE/EN auf Desktop und Smartphone
  erfolgreich. Nachweis und Grenzen stehen im [Prüfstand](../status.md).

### Gemeinsame Sichtung und Prüfstand

Die Trennung von administrativer Sperre und Selbstdeaktivierung sowie die
integrierte Einheitenauswahl sind vom Benutzer abgenommen.
`CosmosDurationField`, feste Tageswerte und
Gestaltungsregeln stehen im [Theme-Standard](../theme.md).
Die vereinbarten gezielten Nachtests sind in DE/EN auf Desktop und Smartphone
erfolgreich; auch der reguläre Build einschließlich aller Anwendungstests ist
erfolgreich. Nachweise und Grenzen stehen im [Prüfstand](../status.md).
Offene zukünftige Prüfungen werden zentral im [Testbedarf](../testbedarf.md) geführt.

Die [Seitenpfade](../theme.md#seitenpfade-und-navigationsziele) sind abgenommen.
Die gezielten DE-/EN-Prüfungen einschließlich gesperrter Sicherheitsabläufe sind
erfolgreich; der erledigte Einzelplan ist entfernt.

## Bestandsaufnahme

Die Bestandsaufnahme bezieht sich auf main, Stand `1f427fba`. Maßgeblich für
Vaadin 25.3.5 ist die [Anwendungs-POM](../../app/pom.xml). Erfasst wurden alle 283
Java-Quelldateien im Anwendungsmodul, darunter 15 Klassen mit `@Route`, außerdem
die versionierten Frontendquellen und relevante Theme-Regeln und Testaufrufer.
Die Zahlen in der Komponentenliste zählen Dateien mit einem direkten Import.
Vererbung, vollqualifizierte Namen und wiederverwendete Instanzen machen die Zahl
der sichtbaren Komponenten größer. Die Eignung möglicher Alternativen wurde mit
der offiziellen Dokumentation und den öffentlichen Java-APIs der lokalen JARs
für 25.3.5 abgeglichen. Die Analyse ist statisch. Für die Kandidaten stehen
Darstellungsvergleich und Bedienprüfung im Browser noch aus.

## Wie unsere Seiten aufgebaut sind

| Ebene | Aktueller Aufbau | Bewertung |
| --- | --- | --- |
| Angemeldeter Rahmen | [UserActivityAwareView](../../app/src/main/java/de/derpeterson/app/ui/base/UserActivityAwareView.java) erbt von `AppLayout`. [TrackedUserAppLayout](../../app/src/main/java/de/derpeterson/app/ui/base/TrackedUserAppLayout.java) setzt über `setContent` einen [PageFrameComponent](../../app/src/main/java/de/derpeterson/app/ui/components/PageFrameComponent.java) ein. Der Kopf verwendet unter anderem `HorizontalLayout`, der innere Rahmen Divs für Kopf, Hauptinhalt und Hintergrund. | Der äußere Rahmen ist nativ. Der Cosmos-Rahmen innen ist selbst zusammengesetzt. Ein späterer Wechsel zu den Navbar-/Drawer-Bereichen von `AppLayout` wäre eine Änderung am responsiven Seitenverhalten. Erst bei einem konkreten Gewinn prüfen. |
| Öffentlich und geschützte Zwischenschritte | [PublicPageComponent](../../app/src/main/java/de/derpeterson/app/ui/components/PublicPageComponent.java) verwendet denselben Rahmen, native Zeilen und Spalten sowie einen eigenen Kopf. Die Gast-Views nutzen unter anderem [IsNotAuthenticatedBaseView](../../app/src/main/java/de/derpeterson/app/security/IsNotAuthenticatedBaseView.java) mit `Composite` und Flow-Layouts. Die 2FA-Views verwenden `VerticalLayout` und je nach Zustand den reduzierten Kopf. | Gemeinsame Hülle erhalten. Visuelle Ausrichtung und serverseitige Ablauf- und Zugriffssperren getrennt betrachten. |
| Seitennavigation und Inhalt | [PageNavigationComponent](../../app/src/main/java/de/derpeterson/app/ui/components/PageNavigationComponent.java) erweitert `SideNav` und arbeitet mit `SideNavItem`. Ein Browser-Listener verbindet interne Einträge mit Java-Aktionen. [PageContentLayout](../../app/src/main/java/de/derpeterson/app/ui/components/PageContentLayout.java) gruppiert Sidebar und Inhalt in einem responsiven CSS-Raster und steuert die mobile Navigation. `AdminView` verwendet ebenfalls diese Seitennavigation. | Die Menüpunkte und Navigationselemente sind nativ. Der responsive Rahmen und die Anbindung an die abschnittsbasierte Navigation sind eigene Ergänzungen. |
| Zeilen, Spalten und Formulare | `HorizontalLayout` und `VerticalLayout` kommen durchgängig vor, etwa bei Registrierung, Profilaktionen, Ticketköpfen und Pagination. Das Profilformular verwendet im Piloten natives `FormLayout`. Die Registrierung und das Admin-Benutzerformular bauen Feldpaare mit `HorizontalLayout`. | Für gewöhnliche Zeilen und Spalten ist die Vaadin-Basis bereits da. `FormLayout` ist eine passende Standardoption für wiederkehrende responsive Feldpaare. Der Bereinigungsassistent verwendet `FormLayout` bereits; weitere Formularübernahmen folgen erst nach Sichtung des Piloten. |
| Karten und Panels | [CosmosCardComponent](../../app/src/main/java/de/derpeterson/app/ui/components/CosmosCardComponent.java) erbt von `VerticalLayout`. [PublicPanelComponent](../../app/src/main/java/de/derpeterson/app/ui/components/PublicPanelComponent.java) und [AccountPanelComponent](../../app/src/main/java/de/derpeterson/app/ui/components/AccountPanelComponent.java) setzen Kopf, Inhalt und Aktionen daraus zusammen. [PortalContextCard](../../app/src/main/java/de/derpeterson/app/ui/components/PortalContextCard.java) basiert auf `Div`. Admin-Panels verwenden zusätzlich die gemeinsamen Panelklassen. | Gemeinsame Gestaltung ist vorhanden. Die HTML-Struktur der Karten ist unsere. `Card` ist ein konkreter Pilotkandidat, weil Vaadin dafür eine eigene Komponente anbietet. |
| Dialoge, Feldhilfe und Filter | Formdialoge basieren auf `Dialog`. [AdminSidePanelDialog](../../app/src/main/java/de/derpeterson/app/ui/components/AdminSidePanelDialog.java) ist ein `Dialog` mit seitlicher Platzierung und eigener Größenanpassung. `Popover` wird etwa für Glocke, Profilmenü und Nachrichtenaktionen verwendet. | Overlay, Modalität und grundlegende Interaktion kommen von Vaadin. Header, Footer und Inhalte sind fachliche Zusammensetzungen. Ein `ConfirmDialog`-Pilot ist für einfache Bestätigungen sinnvoll. |

`Div`, `Span` und Überschriften sind Java-Klassen aus Flow. Ein `Span` für
eine E-Mail-Adresse oder ein `Div` als Container für ein responsives Raster
ist ein regulärer Einsatz. Ein `Span`, der mit mehreren eigenen CSS-Regeln
einen Badge nachbildet, ist ein Kandidat für die spezialisierte Komponente.
Dieser Unterschied ist aussagekräftiger als die Zahl der Divs.
[Vaadin-Layouts](https://vaadin.com/docs/latest/building-apps/ui-basics/layouts),
[Flow-HTML-Elemente](https://vaadin.com/docs/latest/components/html-elements).

## Vollständige Komponentenübersicht

Die Liste umfasst die direkt verwendeten Flow-Oberflächenklassen. Typvarianten,
Events, Datenquellen und Anwendungsdienste sind dabei ausgeklammert. Eigene
Unterklassen wie `CosmosPagination` werden in den Fachbereichen eingeordnet.

| Familie | Im Anwendungscode verwendete Klassen, mit Zahl direkter Importstellen |
| --- | --- |
| Layout und Scrollen | `AppLayout` (1), `HorizontalLayout` (26), `VerticalLayout` (29), `Scroller` (1). `Composite` dient als Java-Hülle, unter anderem für die Gast-Views. |
| Seitennavigation und Umschaltung | `SideNav` (2), `SideNavItem` (5), `Tabs` (3), `Tab` (6), `TabSheet` (3), `Details` (5). |
| Aktionen und Eingabefelder | `Button` (50), `TextField` (14), `EmailField` (12), `PasswordField` (12), `TextArea` (4), `IntegerField` (2), `BigDecimalField` (1), `DatePicker` (7), `TimePicker` (2), `Upload` (1), `CustomField` (1). |
| Auswahl | `Checkbox` (3), `CheckboxGroup` (3), `Switch` (6), `ComboBox` (7), `MultiSelectComboBox` (2), `Select` (5), `RadioButtonGroup` (5). |
| Tabellen, Identität und Anzeige | `Grid` (2), `Badge` (10), `Avatar` (1), `SvgIcon` (22). |
| Dialoge und Feedback | `Dialog` (9), `Popover` (4), `Tooltip` (7), `Notification` (1). |
| HTML-Elemente über Java | `Anchor` (5), `Div` (52), `Span` (55), `H1` (9), `H2` (21), `H3` (10), `H4` (5), `Hr` (2), `Image` (13), `Input` (1), `Paragraph` (4), `Section` (1), `ListItem` (6), `UnorderedList` (6). `Html` wird im `NotificationHelper` für Meldungstext verwendet. |
| Text über Java | `Text` (2) für Textknoten ohne eigenen Container. |
| Flow-Erweiterung | `OtpField` des FlowingCode-Add-ons (3 direkte Importe). Dazu kommt der eigene `AccountAvatarEditor` als Flow-Wrapper für die Bildausschnitt-Bedienung. |

`Grid` steht sowohl hinter der Benutzertabelle als auch hinter der Ticketliste.
Bei den Tickets ist die Darstellung der einzelnen Zeile eine Java-Komposition
aus nativen Komponenten. Gleiches gilt für Auswahloptionen mit Symbol und Text
in der Benutzerverwaltung und in der Konfiguration. Ein eigener Renderer ist
hier Teil der vorgesehenen Flow-API.
[Vaadin Grid](https://vaadin.com/docs/latest/components/grid),
[FlowingCode OTP Field](https://vaadin.com/directory/component/otp-field-add-on).

## Wo ein Umbau tatsächlich etwas bringt

### Avatare und Identität

[OverlayUserIcon](../../app/src/main/java/de/derpeterson/app/ui/components/OverlayUserIcon.java)
verwendet den nativen `CosmosAvatar` für Bild und Initialen. Derselbe Baustein erscheint
im angemeldeten Kopf, im reduzierten Sicherheitskopf, im Profilmenü, in der
Sidebar, der Benutzerverwaltung und bei Ticketidentitäten. Er behandelt auch
Bildrevisionen, Bildfehler, Tooltips und je nach Aufrufer den Präsenzpunkt
oder die Präsenzsteuerung. In der Empfängervorschau der Admin-Benachrichtigungen
werden dagegen schon native `Avatar`-Instanzen eingesetzt.

`Avatar` bietet Name, Kürzel, Bild und `setImageHandler`. Die Identitätshülle
bleibt bei der beauftragten Übernahme erhalten. Zugriffsschutz des Bildhandlers,
Aktualisierung bei Bildwechsel, Fallback, Größen und der Tickettooltip mit Name
und E-Mail gehören zur gezielten Prüfung. `AvatarGroup` ist ein unabhängiger Kandidat für die
Empfängervorschau. Die vorhandene, separate Empfängerliste darf dabei ihre
Funktion behalten. Der native Überlauf öffnet eine eigene Liste und muss
deshalb als Bedienentscheidung betrachtet werden.
[Vaadin Avatar und Avatar Group](https://vaadin.com/docs/latest/components/avatar).

### Formulare, Karten und Dialoge

| Baustein | Konkreter Startpunkt | Umfang und Grenze |
| --- | --- | --- |
| `FormLayout` | Profilformular in `AccountSettingsComponent`, danach die Feldpaare in `RegistrationView` und im Admin-Benutzerformular. | Responsive Spalten, Reihen und Feldbreiten in einer öffentlichen API. Feldlabels, DOM-Reihenfolge, Fehlermeldungen und Autofill bleiben erhalten. Die Konfiguration mit Info-Buttons neben Feldern, Dauereingaben und variablen Zeilen separat prüfen. |
| `Card` | Ein öffentlicher Paneltyp wie „Passwort vergessen“. Danach eine Konto- oder Adminkarte mit Kopf-Metadaten und Aktionen vergleichen. | `Card` bietet unter anderem Header, Präfix, Suffix und Footer. Ein kleines Beispiel zeigt, welche bestehenden CSS- und Testselektoren an der aktuellen Wrapperstruktur hängen. Erst danach eine Entscheidung über `CosmosCardComponent` treffen. Die fachlichen Panel-APIs bleiben nützlich. |
| `ConfirmDialog` | Ein einfacher Bestätigungsfall im Bereich Benachrichtigungen über [PortalConfirmationDialog](../../app/src/main/java/de/derpeterson/app/ui/components/PortalConfirmationDialog.java). | Komponenten für Header, Text und Aktionen werden unterstützt. Schließen oben rechts, Escape, Anfangsfokus, Warnfarbe und der vereinbarte Footer müssen passen. Benutzerlöschung, Geräteabmeldung, Profilbildlöschung und Anleitungsvorschau haben zusätzlichen Kontext. An die erst nach erfolgreichem Pilot herangehen. |
| Info-/Warnboxen und Leerzustände | Statische Hinweise in Kontodialogen, Konfiguration und Ticketeditor, außerdem `NotificationEmptyState`. | Native `Notification` ist für vorübergehende Meldungen. Fest eingebettete Hinweise dürfen als gemeinsame Flow-Komposition aus Icon und Text bestehen. Abstände, Hervorhebung und Icon-Ausrichtung zentral halten. Einen bestehenden Baustein erweitern, sobald eine zweite Stelle denselben Aufbau benötigt. |
| Seitliche Feldhilfe und Filter | `AdminSidePanelDialog` und `ConfigInfoButton` in der Konfiguration, im Mailtest und in den Ticketfiltern. | `Dialog` bleibt die Basis für die seitliche Feldhilfe. Vor einem Wechsel zu `Popover` oder einer anderen Hülle zuerst Anker, Modalität, Fokus, Scrollgrenzen und mobile Bedienung vergleichen. Ein `Tooltip` deckt kurze Zusatztexte ab. |

[Vaadin Form Layout](https://vaadin.com/docs/latest/components/form-layout),
[Card](https://vaadin.com/docs/latest/components/card),
[Confirm Dialog](https://vaadin.com/docs/latest/components/confirm-dialog),
[Dialog](https://vaadin.com/docs/latest/components/dialog).

### Anmeldung, Wiederherstellung und Administration

| Fläche | Aktueller Standard und eigener Anteil | Entscheidung |
| --- | --- | --- |
| Startseite, Anmeldung, Registrierung und Bestätigungen | `HomeRedirectView` leitet weiter. `HomeView` nutzt den angemeldeten Rahmen. `LoginView` setzt echte `EmailField`, `PasswordField`, `Switch` und `Button` mit eigenen Anmeldeschritten zusammen. Registrierung, Passwort vergessen/zurücksetzen, Verifikation, E-Mail-Bestätigung, externe Registrierung und Wartung nutzen Flow-Eingaben, Panels und Texte. | `LoginForm` und `LoginOverlay` sind verfügbar. Das Standardformular hat eigene Felder und ein begrenztes Set an Aktionen. Unser Layout bindet „Anmeldung speichern“, externe Anbieter, Wiederherstellung und Fortsetzung ein. Die bestehende Flow-Komposition bietet dafür die passenden APIs. Ein Kartentausch ist unabhängig davon möglich. |
| Mein Konto und Geräte | `AccountView` verwendet native Seitennavigation und `AccountSettingsComponent` ein `TabSheet`. Sitzungen, Anmeldewege und Schutzaktionen sind Java-Kompositionen aus nativen Buttons, Badges und Dialogen. | Karten/Avatar/Bestätigung jeweils als eigenes Paket prüfen. Die Gruppierung aktiver und gespeicherter Anmeldungen, der Anmeldeweg und die Sitzungsaktionen bleiben fachlich in ihren Bausteinen. |
| Anmeldeanbieter und Identitätsbestätigung | `ExternalLoginButtons` und `ExternalConnectionsComponent` verwenden native Buttons, Logos und Status-Badges. `ExternalIdentityConfirmationDialog` und `TwoFactorActionDialog` verwenden `Dialog` und [TwoFactorCodeInput](../../app/src/main/java/de/derpeterson/app/ui/components/TwoFactorCodeInput.java). | Gemeinsamen Code-Eingabebaustein weiterverwenden. Auto-Bestätigung beim Verbinden und Klick beim Trennen oder bei sicherheitsrelevanten Änderungen bleiben pro Aufrufer bewusst verschieden. |
| 2FA-Einrichtung und Wiederherstellung | `TwoFactorView` verwendet `Tabs`, das Flow-Add-on `OtpField` und native `TextField` für Ersatzcodes. `RecoveryHelpView` verwendet den öffentlichen Rahmen und den Ticketbaustein für die anfragende Person. | In den geprüften offiziellen APIs für 25.3.5 wurde kein gleichwertiges segmentiertes OTP-Feld festgestellt. Add-on mit Fokus, Einfügen, AutoFill und Completion-Ereignis erhalten. Prüfen lässt sich separat, ob der Style ohne manuelles Einfügen eines Stylesheets in den Shadow DOM auskommt. |
| Admin-Übersicht, Benutzer und Konfiguration | `AdminView` und die Admin-Sektionen verwenden native Layouts. Das Benutzer-Grid rendert Identität, Status, Rollen und Aktionen. Konfiguration, 2FA, Anmeldeanbieter und Mailtest verwenden native Felder, `TabSheet`, `Switch` und die gemeinsame Feldhilfe. `CosmosDurationField` und `AdminUserActivityFilter` kombinieren native Eingaben. | Badge- und Formularpiloten hier gezielt übertragen. Den Dauerwert als `CustomField` erst kapseln, wenn ein gemeinsamer Wert-/Validierungsvertrag mehrere Aufrufer vereinfacht. `AdminUserActivityFilter` ist bereits ein `CustomField`. Statische Kennzahlkarten brauchen kein Dashboard-Widget-System. |
| Benachrichtigungen | `UserNotificationsView`, `UserNotificationsPanel` und `AdminUserNotificationsSection` verwenden native Buttons, Auswahl, `Popover`, `Dialog` und Empfänger-Avatare. Listen-, Vorschau- und Detaildarstellung sind eigene Kompositionen. | Badges und gegebenenfalls den Pager vereinheitlichen. Eine News-/Inbox-Zeile mit Lesen/Archivieren und Quellenstatus erfüllt einen anderen Vertrag als ein Chat-Eintrag. |
| Ticketliste und Detail | [SupportTicketsComponent](../../app/src/main/java/de/derpeterson/app/ui/components/SupportTicketsComponent.java) enthält `Grid`, `Scroller`, `Tabs`, `Switch`, `TextArea` und native Buttons. Das Admin-Arbeitsraster liegt über CSS. Die eigene Ticketzeile ist ein Grid-Renderer. | `MasterDetailLayout` ist ein späterer Vergleichskandidat. Es bietet ein responsives Detail-Overlay. Das muss mit Fokusansicht, leerem Detail, mobilem Ablauf, URL-Zustand und Rückkehr zur Liste verglichen werden. `SplitLayout` ist interessant, wenn eine verschiebbare Trennung gewünscht ist. Der aktuelle Bedarf ist eine stabile Zweiteilung mit Fokusumschaltung. |
| Ticketverlauf und Editor | Der Verlauf setzt öffentliche Nachrichten, Vermerke und Statusereignisse unterschiedlich zusammen. Der Editor koppelt `TextArea` und native Aktionen mit dem Modus für interne Vermerke, Warnhinweisen und der jeweils erlaubten Aktion. | Die Java-API von `MessageListItem` unterstützt Autor, Zeit, Text, Avatar, Varianten und Anhänge. Ein allgemeiner Komponenteninhalt für Statuszeilen mit zwei nativen Badges war in der geprüften API nicht ersichtlich. `MessageInput` hat ein Submit-Ereignis, bildet unseren Editorvertrag aber nicht vollständig ab. Die bestehende Komposition ist dafür passend. |
| Pagination | [CosmosPagination](../../app/src/main/java/de/derpeterson/app/ui/components/CosmosPagination.java) kombiniert `HorizontalLayout`, `ComboBox` und `Button`. Benutzerverwaltung, Versandhistorie und Ticketliste verwenden ihn. Die persönliche Inbox hat eigene Seitennavigation in `UserNotificationsPanel`. | Vaadin Grid hat keine eingebaute Leiste für manuelle Seitennavigation. Den vorhandenen Baustein beibehalten. Inbox später angleichen, wenn die gemeinsame API ihre vorhandene Bedienung trägt. Datenabfrage, Ticket-Sprung und Filterzustand gehören weiterhin in den jeweiligen Fachbereich. |

[Vaadin Login](https://vaadin.com/docs/latest/components/login),
[Master-Detail Layout](https://vaadin.com/docs/latest/components/master-detail-layout),
[Message List](https://vaadin.com/docs/latest/components/message-list),
[Message Input](https://vaadin.com/docs/latest/components/message-input),
[Grid-Pagination](https://vaadin.com/docs/latest/components/grid#pagination).

## Wo Browsercode und CSS beteiligt sind

Die Anwendung lädt Lumo und darüber das eigene Silent-Cosmos-Theme.
`cosmos-tokens.css` liefert gemeinsame Werte. Gemeinsame Controls und
Overlays liegen im Theme, fachbezogene Selektoren unter anderem in
`cosmos-account.css`, `cosmos-admin.css`, `cosmos-notifications.css` und
`admin-users.css`. Der Wechsel auf eine native Komponente soll ihre
öffentlichen Eigenschaften, Varianten und CSS-Parts nutzen. Die genehmigten
Farben, Abstände und Lucide-Icons über `AppIcon`/`SvgIcon` bleiben Teil des
Designsystems. [Theme-Quellen](../theme.md#dateien-und-ladereihenfolge),
[Vaadin Styling](https://vaadin.com/docs/latest/styling).

| Vorhandener Eigenanteil | Einordnung |
| --- | --- |
| `account-avatar-editor.js`, `AccountAvatarEditor` mit Canvas und HTML-`Input` für Zoom | Gezielte Ausnahme für Bildausschnitt und Vorschau. Dateiübernahme erfolgt über natives `Upload`. Für den Bildausschnitt wurde keine gleichwertige Standardkomponente nachgewiesen. |
| `compact-time-input.js` und `CompactTimePicker` | Ergänzt den nativen `TimePicker` für kompakte Eingabe. Der Aktivitätsfilter scrollt zusätzlich gezielt in dessen Auswahl, ohne einen Wert zu übernehmen. Dieser Zugriff auf die Komponente ist bei Updates prüfenswert. |
| `equal-button-widths.js` | Synchronisiert Buttonbreiten. Wenn ein betroffener Footer umgebaut wird, prüfen, ob gleiche CSS-Spalten die gewünschte Anordnung bereits vollständig liefern. |
| `recovery-code-download.js` und Skripte in `TwoFactorView` | Browserfunktionen für PDF-Download, Kopieren, Auswahl und Fokus bei Schlüsseln oder Codes. Auch die Anbieter-Konfiguration verwendet die Clipboard-API. Ein nativer Button bleibt der Auslöser. |
| `PageNavigationComponent`, `PageContentLayout`, `AdminSidePanelDialog` | Brücken für Seitennavigationsaktionen, Fokus der mobilen Navigation und Begrenzung der seitlichen Overlays. Bei einem Layout-Pilot anhand öffentlicher Flow-APIs überprüfen. |
| `UserActivityAwareView`, `ConfirmEmailChangeView` und `SupportTicketsComponent` | Browseraktivität, Navigation nach Sitzungswiderruf sowie Erkennung, wann der Ticketverlauf sichtbar gelesen werden kann. Vor einer Änderung den fachlichen Grund und die Folgeprüfung erfassen. |
| `ConfigInfoButton`, `OverlayUserIcon`, `TwoFactorCodeFields` | Sonderverhalten für Tooltips und gezielte Styling-/DOM-Anbindung. Kandidaten für weniger direkte DOM-Zugriffe, sobald eine gleichwertige öffentliche API verfügbar ist. |
| `NotificationHelper` | Verwendet natives `Notification`. Eine Meldungsvariante erzeugt `Html` für den Nachrichtentext. Bei Arbeiten an diesem Baustein Aufrufer und erlaubte Formatierungen prüfen und reine Textmeldungen gegebenenfalls als `Paragraph` ausgeben. |
| HTML für E-Mails und Ersatzcode-PDF | Vorlagen für separate Ausgabeformate. In der versionierten Anwendungsoberfläche wurde kein React-/Hilla-Quellbestand gefunden. |

## Vorgeschlagene Arbeitspakete

Für neue Ansichten gilt eine feste Reihenfolge. Zuerst den vorhandenen
Cosmos-Baustein und die spezialisierte Flow-Komponente prüfen. Dann die
passenden Java-Layouts und semantischen Elemente zur Fachansicht
zusammensetzen. Gemeinsame Maße und Varianten im Theme halten, lokale
Anordnung an der Ansicht. Eine eigene Bedienkomponente wird erst erwogen,
wenn eine konkrete Funktion fehlt. In dem Fall die geprüfte Alternative,
den nötigen Eigenanteil und die zu erhaltende Bedienung zur Entscheidung
vorlegen.

1. **Kleine Formulare.** Nach dem Avatar-Paket ein einfaches responsives Formular
   als getrennten Piloten umstellen. Danach entscheiden, ob der Nutzen eine
   Übernahme in die gemeinsamen Bausteine rechtfertigt.
2. **Karten und einfache Bestätigungen.** Je eine Oberfläche mit `Card` bzw.
   `ConfirmDialog` vergleichen. Bestehende Kopf-, Fuß- und Fokusverträge
   sind Abnahmekriterien. Der gemeinsame Kartenrahmen hat viele Verbraucher
   und ist deshalb erst nach einem lokalen Vergleich dran.
3. **Weiterführende Struktur.** `MasterDetailLayout`, mobile Navigation,
   Anwesenheitsauswahl und Inbox-Pagination nur einzeln betrachten, wenn
   die Änderung Bedienelemente, Code oder Wartung konkret vereinfacht.
   Bereits passende Bausteine wie native Switches, Tabs, Eingaben, Grids,
   Popovers und die Ticket-Status-Badges werden weiterverwendet.

### Abnahme nach einem beauftragten Paket

- Layout und Bedienung in DE/EN auf Desktop und Smartphone vergleichen.
  Dazu lange Übersetzungen, kleine Breiten, Tastaturfokus, zugängliche Namen,
  Tooltips, Hover, Disabled und Live-Updates prüfen.
- Bei Avataren Bild vorhanden, Initialen, fehlerhaftes Bild, Bildwechsel und
  einen langen Namen mit E-Mail prüfen.
- Bei Layout und Dialogen auf echte Fachabläufe prüfen. Beispiele sind Fehler
  beim Speichern, Escape und Rückfokus, Sitzungswiderruf, geschützte Navigation,
  Ticketfilter, direkter Ticketsprung, Refresh und interner Vermerk.
- Bestehende Suiten passend zum Paket erweitern. Ausgangspunkte sind
  `CosmosComponentsBrowserTest`, `CosmosDesignBrowserTest`,
  `NotificationInboxBrowserTest`, `UserNotificationsBrowserTest`,
  `UserManagementBrowserTest`, `AccountSettingsBrowserTest`,
  `SessionManagementBrowserTest` und die passenden `TwoFactor*BrowserTest`-Fälle.
  Konkrete offene Prüfungen nach einer Umsetzung zentral im
  [Testbedarf](../testbedarf.md) führen. Die Regeln für Browserläufe stehen in
  der [Testanleitung](../tests.md).
