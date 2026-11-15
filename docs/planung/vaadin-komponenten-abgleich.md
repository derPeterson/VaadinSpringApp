# Vaadin Flow und Cosmos

## Entscheidung und Ausgangspunkt

Die Anwendung bleibt bei **Vaadin Flow in Java**. Standardbedienung kommt aus
spezialisierten Flow-Komponenten. Cosmos bleibt die gemeinsame Gestaltung.
Anwendungsbausteine dürfen mehrere native Komponenten zu einer fachlichen Einheit
zusammenfassen, etwa eine Sitzungskarte, einen Ticketfilter oder die Pagination.
Ein eigener Ersatz für eine Vaadin-Komponente braucht einen konkreten Grund und
die vereinbarte Freigabe. Maßgeblich ist die [Theme-Anleitung](../theme.md#native-vaadin-bausteine-zuerst).

## Offene Komponentenpakete

Separat von der Nachtarbeit ist der anwendungsweite Hintergrundvergleich
eingebunden: **Advanced** („Horizont Plus“) ist ausgewählt, **Base** bleibt mit
einer zentralen Codezeile wieder aktivierbar. Der Benutzer hat den sichtbaren
Hintergrund auf Startseite und nach der Selektorkorrektur auch auf der Anmeldung
bestätigt; Browsernachtests bleiben im [Testbedarf](../testbedarf.md).
Auswahl und abschließende Bereinigung der Alternativen stehen in [TODO](../../TODO.md).

Die begonnene Umsetzung ist lokal nach `main` zwischenintegriert. Weitere
Arbeiten laufen auf
`codex/vaadin-komponenten-folgearbeiten-4`. Die Badge-Darstellungen und
einheitlichen Seitenpfade sind vom Benutzer abgenommen. Ihre geltenden Regeln
stehen im [Theme-Standard](../theme.md).

Der abgenommene Card-Pilot „Passwort vergessen“ ist in den gemeinsamen
`PublicPanelComponent` überführt. Die übrigen öffentlichen Standardpanels und
die Google-/Microsoft-Nebenkarte sind auf native Cards umgestellt und isoliert
in DE/EN auf Desktop und Smartphone mit Vorher-/Nachher-Aufnahmen geprüft.
Das öffentliche Paket ist vom Benutzer abgenommen. Die gemeinsamen Konto-, Admin-
und Ticketpanelhüllen einschließlich 2FA, Wiederherstellungshilfe und Sitzungslimit
verwenden inzwischen ebenfalls native Cards. Die gemeinsame menschliche Sichtung
dieses Nachtpakets steht noch aus. Auch kleine Innen-, Vorschau- und Kontextkarten
verwenden inzwischen native Cards; ihre DE-/EN-Vergleiche in beiden Breiten sind bestanden.

Die nächsten Gesamtpakete für verbleibende Karten und Bestätigungsdialoge sind
in der [Nachtarbeitsliste](nachtarbeit.md) mit einzelnen Arbeitspaketen gesammelt.
Zuerst alle vereinbarten Gesamtpakete mit ihren gezielten Prüfungen, Reviews und
Commits bearbeiten; die große Testrunde folgt als letzter Arbeitspunkt.
Der Startauftrag umfasst alle Gesamtpakete, Reviews und Commits, die anschließende
Zwischenintegration nach `main`, vollständige Sonar-Bereinigung, einen neuen
Folgebranch und erst danach die große Testrunde. Die Gesamtumsetzung bleibt offen.
Gesamtpaket 3 der Nachtarbeit ergänzt die vollständige Mailqueue-Bereinigung
mit internen 100er-Paketen, Gesamtergebnis und überarbeiteten Dialoghinweisen.
Bestätigte manuelle Aufträge warten bei laufendem Versand unter „Wartet auf
Ausführung“ und werden automatisch sicher ausgeführt; weitere Klicks legen
keinen zweiten Auftrag an. Warten wird nicht als Fehler oder Überspringen gemeldet.
Die Umsetzung ist vorhanden; die technische Paketgröße ist keine sichtbare Gesamtgrenze.
88 gezielte JUnit-Prüfungen und je neun isolierte Queue-Browserfälle in DE/EN
sind bestanden. Die vollständige Gesamtrunde und menschliche Sichtung stehen noch aus.

Der `ConfirmDialog`-Pilot für „Alle Benachrichtigungen als gelesen markieren“
in Inbox und Glockenvorschau ist vom Benutzer abgenommen.
Auch die zehn weiteren beauftragten Bestätigungsfälle verwenden inzwischen
`ConfirmDialog`: Die gemeinsame `PortalConfirmationDialog`-Hülle und die
administrative Benutzerlöschung sind übertragen. Fachliche Prüfungen, einmalige
Ausführung, Abbruch, Formularentwürfe und Fehlerkontext bleiben erhalten.
Die gezielten DE-/EN-Vergleiche auf Desktop und Smartphone sind bestanden;
je Sprache stimmen 61 Messwertsätze überein. Die 212 Bildpaare je Sprache dienen
der noch offenen menschlichen Sichtung. Der lange Anleitungstext scrollt im
Inhaltsbereich, ohne Kopf oder Footer abzuschneiden. Weitere Dialogarten sind
kein Teil dieses Bestätigungspakets.

Für die manuelle Mailqueue-Bereinigungsbestätigung hat Stitch Kritik und zwei
Gestaltungsvorschläge geliefert:
[A – strukturierte Information](https://stitch.google.com/projects/15026163818648585408?node-id=13082268492216412533)
und [B – Parameter und Konsequenzen](https://stitch.google.com/projects/15026163818648585408?node-id=15706497477797870243).
Der Benutzer hat Variante A als gesonderte sofortige Gestaltungsänderung freigegeben.
Der äußere Button und Dialogtitel heißen „Manuelle Bereinigung“, die Bestätigung
bleibt „Jetzt bereinigen“. `AppIcon.BRUSH_CLEANING` ersetzt das X in beiden Aktionen
und im Dialogkopf; die Statusüberschrift verwendet das gemeinsame Blau. Die gelbe
Warnung beginnt mit „Späterer Empfang möglich:“; darunter erklärt eine dezente
Infobox die gespeicherten Einstellungen, den vollständigen abgegrenzten Lauf
und die automatische Fortsetzung nach laufendem Versand.
Die Umsetzung und DE-/EN-Texte stehen zur Benutzersichtung bereit. Die früher
zurückgestellten Browsernachtests gehören zur freigegebenen Nachtarbeit;
noch offene Nachweise stehen im [Testbedarf](../testbedarf.md).
Diese Gestaltung ist aus der Nachtarbeit herausgenommen. Die technische
Übernahme auf `ConfirmDialog` sowie fachliche Gesamtläufe und neue
Statusanzeigen sind in Nachtpaket 3 umgesetzt.

Die separat beauftragte URL-Ansteuerung der Mailqueue-Reiter ist implementiert:
`tab=general` und `tab=assistants` erhalten die Auswahl beim Neuladen und
Direktaufruf, ein Reiterwechsel erhält den Formularentwurf. JUnit-Prüfung und
Ergebnisse stehen im [Prüfstand](../status.md). Die Benutzerabnahme dieser
Korrektur steht noch aus; der Browsernachtest gehört zur freigegebenen Nachtarbeit
und bleibt bis zum erfolgreichen Nachweis im [Testbedarf](../testbedarf.md).

Die gewöhnlichen Formularlayouts sind vom Benutzer abgenommen.
Kennzahlkarten, Avatar-Übernahme, Profil-/Benutzerformulare, segmentierte Auswahl,
Seitenpfade und die Mailqueue-Karte sind bereits vom Benutzer abgenommen.
Ihre aktuellen Verträge stehen im [Theme-Standard](../theme.md) und im
[Mailqueue-Vertrag](../mailqueue.md). Die sichtbare Bereinigungshistorie bleibt
als eigene spätere Aufgabe in [TODO](../../TODO.md).

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
| Zeilen, Spalten und Formulare | `HorizontalLayout` und `VerticalLayout` kommen durchgängig vor, etwa bei Registrierung, Profilaktionen, Ticketköpfen und Pagination. Profil, Registrierung, Admin-Benutzerformular und die Konfigurationsgruppen verwenden natives `FormLayout`. Einspaltige Formulare und Feldhilfezeilen verwenden native Zeilen und Spalten. | Für gewöhnliche Zeilen und Spalten ist die Vaadin-Basis bereits da. `FormLayout` ist eine passende Standardoption für wiederkehrende responsive Feldpaare. Der Bereinigungsassistent verwendet ebenfalls `FormLayout`. Den aktuellen Formularstand vor weiteren Anordnungsänderungen gemeinsam sichten. |
| Karten und Panels | [PublicPanelComponent](../../app/src/main/java/de/derpeterson/app/ui/components/PublicPanelComponent.java) und `ExternalLoginButtons` verwenden native `Card` mit den bestehenden Cosmos-Zonen. [CosmosCardComponent](../../app/src/main/java/de/derpeterson/app/ui/components/CosmosCardComponent.java) bleibt eine `VerticalLayout`-Basis für andere Karten, unter anderem [AccountPanelComponent](../../app/src/main/java/de/derpeterson/app/ui/components/AccountPanelComponent.java). [PortalContextCard](../../app/src/main/java/de/derpeterson/app/ui/components/PortalContextCard.java) basiert auf `Div`. | Die öffentlichen Standardkarten sind abgenommen. Andere Karten separat vergleichen. Dabei Kopf-Metadaten, Aktionen, vorhandene Selektoren und responsive Anordnung erhalten. |
| Dialoge, Feldhilfe und Filter | Formdialoge basieren auf `Dialog`. [AdminSidePanelDialog](../../app/src/main/java/de/derpeterson/app/ui/components/AdminSidePanelDialog.java) ist ein `Dialog` mit seitlicher Platzierung und eigener Größenanpassung. `Popover` wird etwa für Glocke, Profilmenü und Nachrichtenaktionen verwendet. | Overlay, Modalität und grundlegende Interaktion kommen von Vaadin. Header, Footer und Inhalte sind fachliche Zusammensetzungen. Ein `ConfirmDialog`-Pilot ist für einfache Bestätigungen sinnvoll. |

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
werden dagegen schon native `Avatar`-Instanzen eingesetzt.

Die vorhandene Identitätshülle mit Bildberechtigungen, Aktualisierung, Fallback,
Größen und Tickettooltip bleibt erhalten. `AvatarGroup` ist ein unabhängiger Kandidat für die
Empfängervorschau. Die vorhandene, separate Empfängerliste darf dabei ihre
Funktion behalten. Der native Überlauf öffnet eine eigene Liste und muss
deshalb als Bedienentscheidung betrachtet werden.
[Vaadin Avatar und Avatar Group](https://vaadin.com/docs/latest/components/avatar).

### Formulare, Karten und Dialoge

| Baustein | Konkreter Startpunkt | Umfang und Grenze |
| --- | --- | --- |
| `Card` | Die gemeinsame `CosmosCardComponent` und Kontopanels sind auf die native Card übertragen; auch die geeigneten Innenkarten sind übertragen. | Öffentliche Kopf-/Footer-Slots, bestehende Cosmos-Klassen und fachliche Panel-APIs weiterverwenden. Gestaltung und Bedienung vergleichen; menschliche Sichtung bleibt offen. |
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
| Mein Konto und Geräte | `AccountView` verwendet native Seitennavigation und `AccountSettingsComponent` ein `TabSheet`. Sitzungen, Anmeldewege und Schutzaktionen sind Java-Kompositionen aus nativen Buttons, Badges und Dialogen. | Karten und Bestätigungen jeweils als eigenes Paket prüfen; die Avatar-Übernahme ist abgenommen. Die Gruppierung aktiver und gespeicherter Anmeldungen, der Anmeldeweg und die Sitzungsaktionen bleiben fachlich in ihren Bausteinen. |
| Anmeldeanbieter und Identitätsbestätigung | `ExternalLoginButtons` und `ExternalConnectionsComponent` verwenden native Buttons, Logos und Status-Badges. `ExternalIdentityConfirmationDialog` und `TwoFactorActionDialog` verwenden `Dialog` und [TwoFactorCodeInput](../../app/src/main/java/de/derpeterson/app/ui/components/TwoFactorCodeInput.java). | Gemeinsamen Code-Eingabebaustein weiterverwenden. Auto-Bestätigung beim Verbinden und Klick beim Trennen oder bei sicherheitsrelevanten Änderungen bleiben pro Aufrufer bewusst verschieden. |
| 2FA-Einrichtung und Wiederherstellung | `TwoFactorView` verwendet `Tabs`, das Flow-Add-on `OtpField` und native `TextField` für Ersatzcodes. `RecoveryHelpView` verwendet den öffentlichen Rahmen und den Ticketbaustein für die anfragende Person. | In den bisher geprüften offiziellen APIs wurde kein gleichwertiges segmentiertes OTP-Feld festgestellt. Add-on mit Fokus, Einfügen, AutoFill und Completion-Ereignis erhalten. Prüfen lässt sich separat, ob der Style ohne manuelles Einfügen eines Stylesheets in den Shadow DOM auskommt. |
| Admin-Übersicht, Benutzer und Konfiguration | `AdminView` und die Admin-Sektionen verwenden native Layouts. Das Benutzer-Grid rendert Identität, Status, Rollen und Aktionen. Konfiguration, 2FA, Anmeldeanbieter und Mailtest verwenden native Felder, `TabSheet`, `Switch` und die gemeinsame Feldhilfe. `CosmosDurationField` und `AdminUserActivityFilter` kombinieren native Eingaben. | Die übertragenen Badges, Kennzahlkarten und Formularlayouts beibehalten. Den Dauerwert als `CustomField` erst kapseln, wenn ein gemeinsamer Wert-/Validierungsvertrag mehrere Aufrufer vereinfacht. `AdminUserActivityFilter` ist bereits ein `CustomField`. Statische Kennzahlkarten brauchen kein Dashboard-Widget-System. |
| Benachrichtigungen | `UserNotificationsView`, `UserNotificationsPanel` und `AdminUserNotificationsSection` verwenden native Buttons, Auswahl, `Popover`, `Dialog` und Empfänger-Avatare. Listen-, Vorschau- und Detaildarstellung sind eigene Kompositionen. | Die native Badge-Darstellung beibehalten und gegebenenfalls den Pager vereinheitlichen. Eine News-/Inbox-Zeile mit Lesen/Archivieren und Quellenstatus erfüllt einen anderen Vertrag als ein Chat-Eintrag. |
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
Farben, Abstände und freigegebenen Icons über `AppIcon`/`SvgIcon` bleiben Teil des
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

1. **Karten und einfache Bestätigungen.** Eine Konto-/Adminkarte bzw. eine einfache
   Bestätigung mit `ConfirmDialog` vergleichen. Bestehende Kopf-, Fuß- und Fokusverträge
   sind Abnahmekriterien. Der gemeinsame Kartenrahmen hat viele Verbraucher
   und ist deshalb erst nach einem lokalen Vergleich dran.
2. **Weiterführende Struktur.** `MasterDetailLayout`, mobile Navigation,
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
