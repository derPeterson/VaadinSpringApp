# Vaadin Flow und Cosmos

## Entscheidung und Ausgangspunkt

Die Anwendung bleibt bei **Vaadin Flow in Java**. Standardbedienung kommt aus
spezialisierten Flow-Komponenten. Cosmos bleibt die gemeinsame Gestaltung.
Anwendungsbausteine dürfen mehrere native Komponenten zu einer fachlichen Einheit
zusammenfassen, etwa eine Sitzungskarte, einen Ticketfilter oder die Pagination.
Ein eigener Ersatz für eine Vaadin-Komponente braucht einen konkreten Grund und
die vereinbarte Freigabe. Maßgeblich ist die [Theme-Anleitung](../theme.md#native-vaadin-bausteine-zuerst).

## Offene Komponentenpakete

Die bisherigen Nachbearbeitungen einschließlich AvatarGroup und Empfängerliste
sind vom Benutzer abgenommen. Die geltenden Größen und Schriftwerte für Badges,
Eingaben und Buttons stehen in der [Theme-Anleitung](../theme.md).
Die eigene Ticketliste der Wiederherstellungsanfrage ist zusätzlich auf VirtualList
mit höchstens fünf sichtbaren Zeilen umgestellt und vom Benutzer abgenommen.
Die jüngsten Auswahl-, Zeilenflächen- und Leerzustandsänderungen an Ticket- und
Empfängerlisten sind ebenfalls abgenommen.
Die Adminliste bleibt bei Grid und Pagination. Gültige Gestaltung und offener
Nachtest stehen in Theme-Anleitung und zentralem Testbedarf.
Der Anwesenheitsstatus behält seine bisherige Darstellung.
Gemeinsames Switch-Grundstyling, Schließen-Icons und die übrigen freigegebenen
Gestaltungsregeln stehen in der Theme-Anleitung. Automatisierte Browsernachtests
bleiben unabhängig von der menschlichen Abnahme im Testbedarf offen.

Die Gesamtumsetzung bleibt ausdrücklich offen. Der bisherige Komponentenstand
liegt auf `main`; die Integration war kein Featureabschluss.
Der aktuelle Folgebranch `codex/vaadin-komponenten-folgearbeiten-6` wurde vom
lokalen `main` bei `aa9c70fa` angelegt.
Globale Notifications mit responsiver nativer Position, Aura-Innenabstand und
einheitlichen Warn-/Fehlersymbolen einschließlich der übrigen Einsatzstellen sind
umgesetzt und vom Benutzer abgenommen. Die automatisierten Browsernachtests bleiben
zentral im [Testbedarf](../testbedarf.md) offen.

Registrierung, Anmeldung, persönliche Daten, Benutzeranlage/-bearbeitung und Konfiguration verwenden
die native manuelle Validierung: Fehler erscheinen erst beim Absenden, Änderungen nehmen die alte
Feldmarkierung zurück. Die umgesetzten Validierungsabläufe sind vom Benutzer abgenommen.
Die untereinanderstehenden Checkboxen der Benutzeranlage mit nativen Aura-Abständen
sind vom Benutzer abgenommen. Der Client-/Blur-Nachtest
bleibt zentral im Testbedarf offen.

Die bisherigen Formularnachbearbeitungen sind abgenommen. Eventuelle weitere
Kandidaten wie externer Registrierungsabschluss und Konto-/Passwortdialoge nur
bei einem konkreten neuen Auftrag untersuchen. Mail-Test, Benachrichtigungs- und
Ticketformulare sind bereits im abgenommenen Nachbearbeitungspaket enthalten;
sie sind keine erneut offenen Umsetzungspunkte. Die offenen automatisierten
Nachtests stehen im zentralen Testbedarf.

Die vereinheitlichten nativen Löschkreuze und Pflichtfeldindikatoren sind vom
Benutzer abgenommen; der gemeinsame Vertrag steht in der Theme-Anleitung,
offene Browserprüfungen zentral im Testbedarf.

Die Inbox-Seitennavigation mit `CosmosPagination` und die Korrektur verschobener
Seitenzahlen sind nach den isolierten DE-/EN-Vergleichen bei 1280/390 px vom
Benutzer abgenommen. Daraus bleibt kein offenes Paket.
Die übrigen offenen Kandidaten bleiben in diesem Plan und in
[TODO](../../TODO.md) erhalten.

Die OTP-Eingabe ist manuell abgenommen. Der blaue Fokusrahmen erscheint nur
am aktiven Kästchen, der Cursor blinkt nicht und das Autofill mit einem
Passwortmanager funktioniert. Die Position der Reiter für App-Code und
Ersatzcode, der Übergang zum Dialoginhalt und das Leeren eines Teilcodes
beim Reiterwechsel sind ebenfalls bestätigt. Die automatisierten Nachtests
bleiben im [Testbedarf](../testbedarf.md#browser-tests) offen.

Native Karten, weitere Bestätigungen
und vollständige Mailqueue-Bereinigung sind einschließlich Sonar-Bereinigung und
abschließender großer Testrunde fertig. Ergebnisse und Grenzen stehen im
[Prüfstand](../status.md); geltende Bausteine im [Theme-Standard](../theme.md)
und im [Mailqueue-Vertrag](../mailqueue.md).

Karten/Panels, die weiteren Bestätigungsdialoge und der Mailqueue-Assistent sind
vom Benutzer abgenommen. Karten und Dialoge wurden anhand der Vergleichsaufnahmen
gesichtet; den Assistenten hat der Benutzer einschließlich F5 geprüft.
Umfang und Grenzen dieser Abnahme stehen unter [HUM-01](../manual-tests/README.md#hum-01).
Auch die neun Nachbearbeitungspunkte sind nach Sichtung der gezielten
Vorher-/Nachher-Aufnahmen vom Benutzer abgenommen. Daraus bleibt kein weiteres
Umsetzungs- oder Abnahmepaket offen. Weitere Kandidaten sind unten eingeordnet;
ihre Umsetzung benötigt einen eigenen Auftrag.

Die bisherigen öffentlichen Karten und der Bestätigungsdialog-Pilot sind ebenso
wie Formulare, Kennzahlkarten, Avatare, Badges, Seitenpfade und segmentierte
Auswahl sowie die einheitlichen nativen Zeitfelder bereits vom Benutzer
abgenommen. Ihre fertigen Umstellungen sind keine weiteren offenen Arbeitspakete.

## Abgrenzung der weiteren Umsetzung

Die nächsten Pakete betreffen ausschließlich weitere geeignete Vaadin-Komponenten
und die Wiederverwendung vorhandener nativer Cosmos-Bausteine. Gestaltung und
Fachverhalten bleiben erhalten, soweit keine andere Entscheidung getroffen wird.
Die unten aufgeführten Kandidaten sind noch keine pauschale Umbaufreigabe.

Eine allgemeine Smartphone-Tooltip-Lösung, sichtbare Bereinigungshistorie, Neuigkeiten-Oberfläche,
Passkeys und Hosting gehören zu eigenen Themen nach dem Komponentenumbau.
Die offenen Aufgaben bleiben in [TODO](../../TODO.md). Diese Abgrenzung behebt
oder erledigt die betreffenden Aufgaben nicht.

## Grundlage für weitere Pakete

Die folgenden Kandidaten dienen der nächsten gemeinsamen Entscheidung. Vor
jedem neuen Paket die tatsächliche Vaadin-Version aus der
[Anwendungs-POM](../../app/pom.xml), ihre öffentlichen APIs und die aktuelle
Ansicht prüfen. Aktuelle Bausteine beschreibt der [Theme-Standard](../theme.md).

## Wie unsere Seiten aufgebaut sind

| Ebene | Aktueller Aufbau | Bewertung |
| --- | --- | --- |
| Angemeldeter Rahmen | [UserActivityAwareView](../../app/src/main/java/de/derpeterson/app/ui/base/UserActivityAwareView.java) erbt von `AppLayout`. [TrackedUserAppLayout](../../app/src/main/java/de/derpeterson/app/ui/base/TrackedUserAppLayout.java) setzt über `setContent` einen [PageFrameComponent](../../app/src/main/java/de/derpeterson/app/ui/components/PageFrameComponent.java) ein. Der Kopf verwendet unter anderem `HorizontalLayout`, der innere Rahmen Divs für Kopf, Hauptinhalt und Hintergrund. | Der äußere Rahmen ist nativ. Der Cosmos-Rahmen innen ist selbst zusammengesetzt. Ein späterer Wechsel zu den Navbar-/Drawer-Bereichen von `AppLayout` wäre eine Änderung am responsiven Seitenverhalten. Erst bei einem konkreten Gewinn prüfen. |
| Öffentlich und geschützte Zwischenschritte | [PublicPageComponent](../../app/src/main/java/de/derpeterson/app/ui/components/PublicPageComponent.java) verwendet denselben Rahmen, native Zeilen und Spalten sowie einen eigenen Kopf. Die Gast-Views nutzen unter anderem [IsNotAuthenticatedBaseView](../../app/src/main/java/de/derpeterson/app/security/IsNotAuthenticatedBaseView.java) mit `Composite` und Flow-Layouts. Die 2FA-Views verwenden `VerticalLayout` und je nach Zustand den reduzierten Kopf. | Gemeinsame Hülle erhalten. Visuelle Ausrichtung und serverseitige Ablauf- und Zugriffssperren getrennt betrachten. |
| Seitennavigation und Inhalt | [PageNavigationComponent](../../app/src/main/java/de/derpeterson/app/ui/components/PageNavigationComponent.java) erweitert `SideNav` und arbeitet mit `SideNavItem`. Ein Browser-Listener verbindet interne Einträge mit Java-Aktionen. [PageContentLayout](../../app/src/main/java/de/derpeterson/app/ui/components/PageContentLayout.java) gruppiert Sidebar und Inhalt in einem responsiven CSS-Raster und steuert die mobile Navigation. `AdminView` verwendet ebenfalls diese Seitennavigation. | Die Menüpunkte und Navigationselemente sind nativ. Der responsive Rahmen und die Anbindung an die abschnittsbasierte Navigation sind eigene Ergänzungen. |
| Zeilen, Spalten und Formulare | `HorizontalLayout` und `VerticalLayout` kommen durchgängig vor, etwa bei Registrierung, Profilaktionen, Ticketköpfen und Pagination. Profil, Registrierung, Admin-Benutzerformular und die Konfigurationsgruppen verwenden natives `FormLayout`. Einspaltige Formulare und Feldhilfezeilen verwenden native Zeilen und Spalten. | Für gewöhnliche Zeilen und Spalten ist die Vaadin-Basis bereits da. `FormLayout` ist eine passende Standardoption für wiederkehrende responsive Feldpaare. Der Bereinigungsassistent verwendet ebenfalls `FormLayout`. Die abgenommenen Formularanordnungen bei weiteren Änderungen erhalten. |
| Karten und Panels | [PublicPanelComponent](../../app/src/main/java/de/derpeterson/app/ui/components/PublicPanelComponent.java), `ExternalLoginButtons` und [CosmosCardComponent](../../app/src/main/java/de/derpeterson/app/ui/components/CosmosCardComponent.java) verwenden native `Card`. Innenkarten verwenden `CosmosContentCardComponent`, auch [PortalContextCard](../../app/src/main/java/de/derpeterson/app/ui/components/PortalContextCard.java) erbt davon. | Kartenumstellungen sind technisch geprüft und vom Benutzer anhand der Vergleichsaufnahmen abgenommen. Cosmos-Zonen, Kopf-Metadaten, Aktionen und responsive Anordnung bleiben erhalten. |
| Dialoge, Feldhilfe und Filter | Bestätigungen verwenden den nativen `ConfirmDialog`. Form- und Identitätsdialoge bleiben bei `Dialog`. [AdminSidePanelDialog](../../app/src/main/java/de/derpeterson/app/ui/components/AdminSidePanelDialog.java) ist ein `Dialog` mit seitlicher Platzierung und eigener Größenanpassung. `Popover` wird etwa für Glocke, Profilmenü und Nachrichtenaktionen verwendet. | Overlay, Modalität und grundlegende Interaktion kommen von Vaadin. Header, Footer und Inhalte sind fachliche Zusammensetzungen. Die weiteren Bestätigungen sind vom Benutzer anhand der Vergleichsaufnahmen abgenommen. |

`Div`, `Span` und Überschriften sind Java-Klassen aus Flow. Ein `Span` für
eine E-Mail-Adresse oder ein `Div` als Container für ein responsives Raster
ist ein regulärer Einsatz. Ein `Span`, der mit mehreren eigenen CSS-Regeln
einen Badge nachbildet, ist ein Kandidat für die spezialisierte Komponente.
Dieser Unterschied ist aussagekräftiger als die Zahl der Divs.
[Vaadin-Layouts](https://vaadin.com/docs/latest/building-apps/ui-basics/layouts),
[Flow-HTML-Elemente](https://vaadin.com/docs/latest/components/html-elements).

## Wo ein Umbau tatsächlich etwas bringt

### Avatare und Identität

[OverlayUserIcon](../../app/src/main/java/de/derpeterson/app/ui/components/OverlayUserIcon.java)
verwendet den nativen `CosmosAvatar` für Bild und Initialen. Derselbe Baustein erscheint
im angemeldeten Kopf, im reduzierten Sicherheitskopf, im Profilmenü, in der
Sidebar, der Benutzerverwaltung und bei Ticketidentitäten. Er behandelt auch
Bildrevisionen, Bildfehler, Tooltips und je nach Aufrufer den Präsenzpunkt
oder die Präsenzsteuerung. In der Empfängervorschau der Admin-Benachrichtigungen
wird jetzt die native `AvatarGroup` eingesetzt.

Die vorhandene Identitätshülle mit Bildberechtigungen, Aktualisierung, Fallback,
Größen und Tickettooltip bleibt erhalten. Die Empfängervorschau und ihre
vollständige Empfängerliste im Popover mit VirtualList sind ebenfalls
abgenommen; der gültige Aufbau steht in der [Theme-Anleitung](../theme.md#icons-und-identität).
[Vaadin Avatar und Avatar Group](https://vaadin.com/docs/latest/components/avatar).

### Formulare, Karten und Dialoge

| Baustein | Konkreter Startpunkt | Umfang und Grenze |
| --- | --- | --- |
| `Card` | Die gemeinsame `CosmosCardComponent` und Kontopanels sind auf die native Card übertragen; auch die geeigneten Innenkarten sind übertragen. | Öffentliche Kopf-/Footer-Slots, bestehende Cosmos-Klassen und fachliche Panel-APIs weiterverwenden. Karten und zugehörige Nachbearbeitung sind vom Benutzer anhand der Vergleichsaufnahmen abgenommen. |
| `ConfirmDialog` | [PortalConfirmationDialog](../../app/src/main/java/de/derpeterson/app/ui/components/PortalConfirmationDialog.java) und administrative Benutzerlöschung verwenden native Bestätigungen; die weiteren beauftragten Fälle einschließlich Geräteabmeldung, Profilbildlöschung und Anleitungsvorschau sind übertragen. | Fachlicher Kontext, Abbruch, Fokus, Warnfarbe, einmalige Ausführung und Fehlerbehandlung sind geprüft. Die weiteren Bestätigungen sind anhand der Vergleichsaufnahmen vom Benutzer abgenommen; Form- und Identitätsdialoge bleiben bei `Dialog`. |
| Info-/Warnboxen und Leerzustände | Statische Hinweise in Kontodialogen, Konfiguration und Ticketeditor, außerdem `NotificationEmptyState`. | Native `Notification` ist für vorübergehende Meldungen. Fest eingebettete Hinweise dürfen als gemeinsame Flow-Komposition aus Icon und Text bestehen. Abstände, Hervorhebung und Icon-Ausrichtung zentral halten. Einen bestehenden Baustein erweitern, sobald eine zweite Stelle denselben Aufbau benötigt. |

[Vaadin Form Layout](https://vaadin.com/docs/latest/components/form-layout),
[Card](https://vaadin.com/docs/latest/components/card),
[Confirm Dialog](https://vaadin.com/docs/latest/components/confirm-dialog),
[Dialog](https://vaadin.com/docs/latest/components/dialog).

### Anmeldung, Wiederherstellung und Administration

| Fläche | Aktueller Standard und eigener Anteil | Entscheidung |
| --- | --- | --- |
| Startseite, Anmeldung, Registrierung und Bestätigungen | `HomeRedirectView` leitet weiter. `HomeView` nutzt den angemeldeten Rahmen. `LoginView` setzt echte `EmailField`, `PasswordField`, `Switch` und `Button` mit eigenen Anmeldeschritten zusammen. Registrierung, Passwort vergessen/zurücksetzen, Verifikation, E-Mail-Bestätigung, externe Registrierung und Wartung nutzen Flow-Eingaben, Panels und Texte. | `LoginForm` und `LoginOverlay` sind verfügbar. Das Standardformular hat eigene Felder und ein begrenztes Set an Aktionen. Unser Layout bindet „Anmeldung speichern“, externe Anbieter, Wiederherstellung und Fortsetzung ein. Die bestehende Flow-Komposition bietet dafür die passenden APIs. Die Panelhüllen sind bereits auf native Cards übertragen. |
| Mein Konto und Geräte | `AccountView` verwendet native Seitennavigation und `AccountSettingsComponent` ein `TabSheet`. Sitzungen, Anmeldewege und Schutzaktionen sind Java-Kompositionen aus nativen Buttons, Badges und Dialogen. | Karten und Bestätigungen sind übertragen, technisch geprüft und anhand der Vergleichsaufnahmen vom Benutzer abgenommen. Die Avatar-Übernahme ist abgenommen. Die Gruppierung aktiver und gespeicherter Anmeldungen, der Anmeldeweg und die Sitzungsaktionen bleiben fachlich in ihren Bausteinen. |
| Anmeldeanbieter und Identitätsbestätigung | `ExternalLoginButtons` und `ExternalConnectionsComponent` verwenden native Buttons, Logos und Status-Badges. `ExternalIdentityConfirmationDialog` und `TwoFactorActionDialog` verwenden `Dialog` und [TwoFactorCodeInput](../../app/src/main/java/de/derpeterson/app/ui/components/TwoFactorCodeInput.java). | Gemeinsamen Code-Eingabebaustein weiterverwenden. Auto-Bestätigung beim Verbinden und Klick beim Trennen oder bei sicherheitsrelevanten Änderungen bleiben pro Aufrufer bewusst verschieden. |
| 2FA-Einrichtung und Wiederherstellung | `TwoFactorView` verwendet `Tabs`, das Flow-Add-on `OtpField` und native `TextField` für Ersatzcodes. `RecoveryHelpView` verwendet den öffentlichen Rahmen und den Ticketbaustein für die anfragende Person. | Das Add-on verwendet seine öffentlichen Styling-Schnittstellen im [Cosmos-Theme](../theme.md#einrichtung-und-ersatzcodes). Fokus, Einfügen, Autofill und das Completion-Ereignis laufen über das Add-on. |
| Admin-Übersicht, Benutzer und Konfiguration | `AdminView` und die Admin-Sektionen verwenden native Layouts. Das Benutzer-Grid rendert Identität, Status, Rollen und Aktionen. Konfiguration, 2FA, Anmeldeanbieter und Mailtest verwenden native Felder, `TabSheet`, `Switch` und die gemeinsame Feldhilfe. `CosmosDurationCustomField` und `AdminUserActivityFilter` kombinieren native Eingaben als `CustomField`. | Die übertragenen Badges, Kennzahlkarten und Formularlayouts beibehalten. Die Zeitfelder sind einheitlich nativ gekapselt; ihre Speicherformate und fachliche Validierung bleiben erhalten. Statische Kennzahlkarten brauchen kein Dashboard-Widget-System. |
| Benachrichtigungen | `UserNotificationsView`, `UserNotificationsPanel` und `AdminUserNotificationsSection` verwenden native Buttons, Auswahl, `Popover`, `Dialog` und Empfänger-Avatare. Listen-, Vorschau- und Detaildarstellung sind eigene Kompositionen. | Die native Badge-Darstellung und den gemeinsamen Pager beibehalten. Eine News-/Inbox-Zeile mit Lesen/Archivieren und Quellenstatus erfüllt einen anderen Vertrag als ein Chat-Eintrag. |
| Ticketliste und Detail | [SupportTicketsComponent](../../app/src/main/java/de/derpeterson/app/ui/components/SupportTicketsComponent.java) verwendet im Adminbereich `MasterDetailLayout` für Vorgangsliste und Detail. | Das Layout ist manuell bestätigt. Der automatisierte Nachtest für Fokusmodus, Rückkehr zur Liste, Direktlinks, Entwürfe, Filter und Pagination steht im [Testbedarf](../testbedarf.md#browser-tests). |
| Ticketverlauf und Editor | Der Verlauf setzt öffentliche Nachrichten, Vermerke und Statusereignisse unterschiedlich zusammen. Der Editor koppelt `TextArea` und native Aktionen mit dem Modus für interne Vermerke, Warnhinweisen und der jeweils erlaubten Aktion. | Die Java-API von `MessageListItem` unterstützt Autor, Zeit, Text, Avatar, Varianten und Anhänge. Ein allgemeiner Komponenteninhalt für Statuszeilen mit zwei nativen Badges war in der geprüften API nicht ersichtlich. `MessageInput` hat ein Submit-Ereignis, bildet unseren Editorvertrag aber nicht vollständig ab. Die bestehende Komposition ist dafür passend. |
| Pagination | [CosmosPagination](../../app/src/main/java/de/derpeterson/app/ui/components/CosmosPagination.java) kombiniert `HorizontalLayout`, `ComboBox` und `Button`. Benutzerverwaltung, Versandhistorie, Ticketliste und persönliche Inbox verwenden ihn. `UserNotificationsPanel` behält nur Datenabfrage, Filter und lokale Anordnung. | Vaadin Grid hat keine eingebaute Leiste für manuelle Seitennavigation. Den abgenommenen gemeinsamen Baustein beibehalten. Datenabfrage, Ticket-Sprung und Filterzustand gehören weiterhin in den jeweiligen Fachbereich. |

[Vaadin Login](https://vaadin.com/docs/latest/components/login),
[Master-Detail Layout](https://vaadin.com/docs/latest/components/master-detail-layout),
[Message List](https://vaadin.com/docs/latest/components/message-list),
[Message Input](https://vaadin.com/docs/latest/components/message-input),
[Grid-Pagination](https://vaadin.com/docs/latest/components/grid#pagination).

## Wo Browsercode und CSS beteiligt sind

Die Anwendung lädt Vaadin Aura und darüber das eigene Silent-Cosmos-Theme
(Schichten und Ladereihenfolge: [Theme](../theme.md#dateien-und-ladereihenfolge)).
`cosmos-tokens.css` liefert gemeinsame Werte. Gemeinsame Controls und
Overlays liegen im Theme, fachbezogene Selektoren unter anderem in
`cosmos-account.css`, `cosmos-admin.css`, `cosmos-notifications.css` und
`admin-users.css`. Der Wechsel auf eine native Komponente soll ihre
öffentlichen Eigenschaften, Varianten und CSS-Parts nutzen. Die genehmigten
Farben, Abstände und freigegebenen Icons über `AppIcon`/`SvgIcon` bleiben Teil des
Designsystems. [Theme-Quellen](../theme.md#dateien-und-ladereihenfolge),
[Vaadin Styling](https://vaadin.com/docs/latest/styling).

| Vorhandener Eigenanteil | Einordnung |
| --- | --- |
| `account-avatar-editor.js`, `AccountAvatarEditor` mit Canvas und HTML-`Input` für Zoom | Gezielte Ausnahme für Bildausschnitt und Vorschau. Dateiübernahme erfolgt über natives `Upload`. Für den Bildausschnitt wurde keine gleichwertige Standardkomponente nachgewiesen. |
| `compact-time-input.js` und `CompactTimePicker` | Ergänzt den nativen `TimePicker` für kompakte Eingabe. Der Aktivitätsfilter scrollt zusätzlich gezielt in dessen Auswahl, ohne einen Wert zu übernehmen. Dieser Zugriff auf die Komponente ist bei Updates prüfenswert. |
| `equal-button-widths.js` | Synchronisiert Buttonbreiten für die übrigen Aktionsgruppen und nativen Bestätigungsdialoge. CSS Grid übernimmt die festen Buttonpaare in den Konto- und Identitätsdialogen. Das Layout ist auch auf schmalen Ansichten manuell bestätigt. Der automatisierte Nachtest steht im [Testbedarf](../testbedarf.md#browser-tests). |
| `recovery-code-download.js` und Skripte in `TwoFactorView` | Browserfunktionen für PDF-Download, Kopieren, Auswahl und Fokus bei Schlüsseln oder Codes. Auch die Anbieter-Konfiguration verwendet die Clipboard-API. Ein nativer Button bleibt der Auslöser. |
| `PageNavigationComponent`, `PageContentLayout`, `AdminSidePanelDialog` | Brücken für Seitennavigationsaktionen, Fokus der mobilen Navigation und Begrenzung der seitlichen Overlays. Bei einem Layout-Pilot anhand öffentlicher Flow-APIs überprüfen. |
| `UserActivityAwareView`, `ConfirmEmailChangeView` und `SupportTicketsComponent` | Browseraktivität, Navigation nach Sitzungswiderruf sowie Erkennung, wann der Ticketverlauf sichtbar gelesen werden kann. Vor einer Änderung den fachlichen Grund und die Folgeprüfung erfassen. |
| `OverlayUserIcon`, `TwoFactorCodeFields` | Sonderverhalten für Tooltips und gezielte Styling-/DOM-Anbindung. Kandidaten für weniger direkte DOM-Zugriffe, sobald eine gleichwertige öffentliche API verfügbar ist. |
| `NotificationHelper` | Alle Aufrufer verwenden das native `Notification` mit gemeinsamen Textmethoden: `Paragraph`, hervorgehobene Registrierung mit `Span` und Neustart-Aufzählung mit `UnorderedList`/`ListItem`. Die repräsentativen Varianten sind anhand der Vergleichsaufnahmen abgenommen; daraus bleibt kein weiterer Komponentenumbau offen. |
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

**Weiterführende Struktur.** Mobile Navigation und
Anwesenheitsauswahl nur einzeln betrachten, wenn
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
