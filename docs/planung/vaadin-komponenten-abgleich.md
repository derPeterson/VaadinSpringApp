# Vaadin Flow und Cosmos

## Entscheidung und Ausgangspunkt

Die Anwendung bleibt bei **Vaadin Flow in Java**. Standardbedienung kommt aus
spezialisierten Flow-Komponenten. Cosmos bleibt die gemeinsame Gestaltung.
Anwendungsbausteine dürfen mehrere native Komponenten zu einer fachlichen Einheit
zusammenfassen, etwa eine Sitzungskarte, einen Ticketfilter oder die Pagination.
Ein eigener Ersatz für eine Vaadin-Komponente braucht einen konkreten Grund und
die vereinbarte Freigabe. Maßgeblich ist die [Theme-Anleitung](../theme.md#native-vaadin-bausteine-zuerst).

## Offene Komponentenpakete

Die Gesamtumsetzung bleibt ausdrücklich offen. Der technisch geprüfte Stand ist
lokal nach `main` zwischenintegriert; Folgearbeiten laufen auf
`codex/vaadin-komponenten-folgearbeiten-4`. Native Karten, weitere Bestätigungen
und vollständige Mailqueue-Bereinigung sind einschließlich Sonar-Bereinigung und
abschließender großer Testrunde fertig. Ergebnisse und Grenzen stehen im
[Prüfstand](../status.md); geltende Bausteine im [Theme-Standard](../theme.md)
und im [Mailqueue-Vertrag](../mailqueue.md).

Karten/Panels, die weiteren Bestätigungsdialoge und der Mailqueue-Assistent sind
vom Benutzer abgenommen. Karten und Dialoge wurden anhand der Vergleichsaufnahmen
gesichtet; den Assistenten hat der Benutzer einschließlich F5 geprüft.
Umfang und Grenzen dieser Abnahme stehen unter [HUM-01](../manual-tests/README.md#hum-01).
Offen bleibt die nachfolgend gesammelte Nachbearbeitung. Sie ist jetzt ausdrücklich
zur Umsetzung freigegeben, jeweils mit Review, eigenem Commit und gezielten
Screenshots der betroffenen Stellen für die anschließende Benutzerabnahme.

Die bisherigen öffentlichen Karten und der Bestätigungsdialog-Pilot sind ebenso
wie Formulare, Kennzahlkarten, Avatare, Badges, Seitenpfade und segmentierte
Auswahl bereits vom Benutzer abgenommen. Ihre fertigen Umstellungen sind keine
weiteren offenen Arbeitspakete.

Advanced („Horizont Plus“) bleibt ausgewählt; Base ist mit einer zentralen
Codezeile wieder aktivierbar. Die automatisierten Nachtests beider Varianten
sind bestanden. Die endgültige Hintergrundauswahl samt Bereinigung ungenutzter
Varianten sowie die sichtbare Bereinigungshistorie bleiben in [TODO](../../TODO.md).
Der Scrollpunkt ist auf Benutzerwunsch zurückgestellt; siehe [BUGS](../../BUGS.md).

## Nachbearbeitungsliste

**Status: Umsetzung läuft; technisch fertige Punkte warten auf Endabnahme.** Die Punkte einzeln
bearbeiten, jeweils mit Review und eigenem Commit. Nur die aufgenommenen Stellen
isoliert in DE/EN auf Desktop und Smartphone prüfen und Vorher-/Nachher-Aufnahmen
für die Endabnahme bereitstellen; keine große Testrunde. Der bestätigte Reiterfehler
hat Vorrang; der separat zurückgestellte Scrollpunkt bleibt unberührt.

1. [ ] **Warndreieck im Mailqueue-Bereinigungsassistenten:** Beim gelben
   Sicherheitshinweis das bisherige Kreis-Icon durch das vorhandene Warndreieck
   unserer Sicherheitswarnungen ersetzen. Bestehende Farben, Texte und Anordnung
   beibehalten; in DE und EN gleich darstellen. Die Rückmeldung betrifft den
   Hinweis im Assistenten, keine anwendungsweite Icon-Änderung.
2. [ ] **Warndreieck im Bereinigungsdialog:** Auch die gelbe Warnung
   „Späterer Empfang möglich:“ im Dialog „Manuelle Bereinigung“ erhält das
   vorhandene Warndreieck anstelle des Kreis-Icons. Die separate dezente
   Parameter-Infobox behält ihr Info-Icon.
3. [ ] **Roter Brush im Dialogkopf:** Das Brush-Icon oben im Dialog
   „Manuelle Bereinigung“ mit der bestehenden roten Gefahrenvariante darstellen.
   Die blaue Icongestaltung im Kopf der Assistentenkarte bleibt bestehen.
4. [ ] **Kürzerer Wartetext:** Die gelbe Erklärung unter „Wartet auf Ausführung“
   so kürzen, dass sie auf üblicher Desktopbreite in eine Zeile passt.
   Textvorschlag DE: „Vorgemerkt. Startet automatisch nach Ende des E-Mail-Versands.“
   EN: „Queued. Starts automatically when email delivery finishes.“
   Auf schmalen Ansichten weiterhin normalen Umbruch zulassen.
5. [ ] **Lange Namen und E-Mail-Adressen in den Benutzerangaben begrenzen:**
   Betroffen sind der Benutzer-Menüauslöser im Header, die Profilkarte links
   auf „Mein Konto → Persönliche Daten“, der Identitätskopf im geöffneten
   Benutzermenü und die Benutzerkarte im administrativen Dialog „Benutzer löschen“.
   An allen vier Stellen zuerst prüfen, welche Kürzungs- und
   Tooltiplogik bereits vorhanden ist; nur fehlende Teile ergänzen.
   Name und E-Mail anhand der verfügbaren Breite begrenzen und grundsätzlich
   einzeilig mit „…“ darstellen; für die Smartphone-Benutzerkarte im Löschdialog
   gilt die unten abgestimmte Zwei-Zeilen-Ausnahme. Keine feste Zeichengrenze
   und keine Änderung der gespeicherten Kontodaten. Avatar, Präsenzpunkt und
   Aufklapp-Pfeil behalten ihren Platz; lange Werte verbreitern den Header nicht.
   Vollständigen Namen und vollständige E-Mail-Adresse mit dem nativen Tooltip
   bei Hover beziehungsweise Tastaturfokus zugänglich machen. Auf ausdrücklichen
   Benutzerwunsch keinen zusätzlichen Info-Button oder Touch-Ersatz einbauen;
   die normale native Tooltip-Bedienung bleibt bestehen. Bei der Umsetzung lange Namen,
   lange E-Mail-Adressen und normale Werte in DE/EN auf Desktop und Smartphone
   prüfen; vorhandene Kopfzeilenvarianten im betroffenen Baustein berücksichtigen.
   Im Löschdialog den vorhandenen Platz großzügig nutzen: passende Namen und
   E-Mail-Adressen vollständig anzeigen, erst bei tatsächlichem Platzmangel
   kürzen und den vollständigen Wert im Tooltip anbieten. Lange Kontodaten dürfen
   den Dialog nicht verbreitern. Die bestehende responsive Dialogbreite erhalten;
   auf Smartphonebreite weder horizontales Überlaufen noch verdrängte Aktionen
   zulassen. Die eindeutig erkennbare Identität des zu löschenden Kontos erhalten.
   In der Benutzerkarte des Löschdialogs dürfen auf dem Smartphone Name und
   E-Mail-Adresse jeweils auf höchstens zwei Zeilen umbrechen. Erst wenn auch
   diese zwei Zeilen nicht ausreichen, mit „…“ kürzen; keine vier oder fünf
   Zeilen pro Angabe zulassen. Vollständige Werte bleiben über normale native
   Tooltips zugänglich; kein zusätzlicher Info-Button. Die Headeranzeige bleibt
   weiterhin einzeilig.
6. [ ] **Smartphone-Footer der gezeigten Bestätigungen:** Ausschließlich im
   Smartphone-Layout der Dialoge „Ungespeicherte Änderungen“ und „Alle anderen
   abmelden“ beide Buttons untereinander über die volle verfügbare Breite innerhalb der bestehenden
   Footer-Innenabstände anordnen. Gleiche Breite und Höhe sowie einen einheitlichen
   vertikalen Abstand verwenden. Reihenfolge und Farben beibehalten: oben gelb
   „Änderungen verwerfen“, darunter blau „Weiter bearbeiten“. Bei „Alle anderen
   abmelden“ oben „Abbrechen“, darunter die rote Aktion „Alle anderen abmelden“.
   Desktopdarstellung, Anfangsfokus und Verhalten des Formularschutzes bleiben unverändert.
   Bei späterer Umsetzung DE/EN auf Smartphonebreite einschließlich langer
   Beschriftungen prüfen und die unveränderte Desktopanordnung gegenprüfen.

7. [ ] **Native Überlaufnavigation aller Reiterleisten – umgesetzt, Endabnahme offen:**
   Seitenreiter behalten lesbare Breiten und nutzen bei Platzmangel native
   Vor-/Zurück-Pfeile und horizontales Scrollen. Dasselbe gilt für segmentierte
   Umschalter; ihre freigegebene Gestaltung bleibt erhalten. Die gequetschten
   Sicherheits- und 2FA-Rollenreiter sind korrigiert. Gezielte Prüfungen in DE/EN
   bei 1280/1024/390/320 px einschließlich Pfeilen, Tastatur und echter Touchgeste
   sind bestanden; segmentierte Umschalter zusätzlich bei 1280/390/320 px.
   Vorher-/Nachher-Aufnahmen der betroffenen Leisten liegen zur Endabnahme vor.
   Aktuelle Regeln stehen im [Theme](../theme.md#native-vaadin-bausteine-zuerst),
   die gezielten Ergebnisse im [Prüfstand](../status.md).

8. [ ] **Smartphone-Aktionen in den Kontoverwaltungskarten:** Unter „Mein Konto
   → Sicherheit → Konto verwalten“ die Buttons „Konto deaktivieren“ und
   „Konto endgültig löschen“ jeweils in ihrer eigenen Karte rechtsbündig
   innerhalb der bestehenden Innenabstände anordnen; innerhalb der Aktionszeile
   vertikal zentrieren. Nur Smartphone-Layout ändern. Bestehende Buttonfarben,
   Icons, Beschriftungen und Desktopdarstellung erhalten. Nach Startfreigabe
   beide Karten auf schmaler Breite in DE/EN auf passende Ausrichtung und
   vollständig erreichbare Aktionen prüfen.

9. [ ] **Einheitliche Smartphone-Footer in der Konfiguration:** Alle
   Konfigurationsbereiche auf die Anordnung von „Werte neu laden“ und
   „Konfiguration speichern“ prüfen. Auf Smartphonebreite stehen beide Buttons
   untereinander und nehmen jeweils die volle verfügbare Breite innerhalb der
   bestehenden Footer-Innenabstände ein. Insbesondere die 2FA-Konfiguration
   einschließlich ihrer Rollenansichten mit dem bereits vorhandenen mobilen
   Aufbau bei Anmeldeanbietern abgleichen und abweichende Footer angleichen.
   Reihenfolge, Farben, Icons, Beschriftungen sowie Lade-, Speicher- und
   Berechtigungslogik erhalten. Desktopdarstellung unverändert lassen.
   Nach ausdrücklichem Startauftrag alle betroffenen Konfigurationsfooter
   in DE/EN auf Smartphonebreite und unverändertem Desktop gegenprüfen.

**Abgestimmt zur Vormerkungs-Notification:** Blaues Informationsdesign samt
Info-Icon beibehalten. Die Meldung bestätigt die Vormerkung; Warten ist ein
regulärer Zwischenzustand. Badge und Statuszeile bleiben gelb. Die gelben
Sicherheitswarnungen zu den Folgen der Bereinigung erhalten dagegen das
Warndreieck. Die Endabnahme der umgesetzten Nachbearbeitung bleibt beim Benutzer.

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
| `Card` | Die gemeinsame `CosmosCardComponent` und Kontopanels sind auf die native Card übertragen; auch die geeigneten Innenkarten sind übertragen. | Öffentliche Kopf-/Footer-Slots, bestehende Cosmos-Klassen und fachliche Panel-APIs weiterverwenden. Der übertragene Kartenstand ist vom Benutzer anhand der Vergleichsaufnahmen abgenommen; neue Nachbearbeitungspunkte bleiben separat offen. |
| `ConfirmDialog` | [PortalConfirmationDialog](../../app/src/main/java/de/derpeterson/app/ui/components/PortalConfirmationDialog.java) und administrative Benutzerlöschung verwenden native Bestätigungen; die weiteren beauftragten Fälle einschließlich Geräteabmeldung, Profilbildlöschung und Anleitungsvorschau sind übertragen. | Fachlicher Kontext, Abbruch, Fokus, Warnfarbe, einmalige Ausführung und Fehlerbehandlung sind geprüft. Die weiteren Bestätigungen sind anhand der Vergleichsaufnahmen vom Benutzer abgenommen; Form- und Identitätsdialoge bleiben bei `Dialog`. |
| Info-/Warnboxen und Leerzustände | Statische Hinweise in Kontodialogen, Konfiguration und Ticketeditor, außerdem `NotificationEmptyState`. | Native `Notification` ist für vorübergehende Meldungen. Fest eingebettete Hinweise dürfen als gemeinsame Flow-Komposition aus Icon und Text bestehen. Abstände, Hervorhebung und Icon-Ausrichtung zentral halten. Einen bestehenden Baustein erweitern, sobald eine zweite Stelle denselben Aufbau benötigt. |
| Seitliche Feldhilfe und Filter | `AdminSidePanelDialog` und `ConfigInfoButton` in der Konfiguration, im Mailtest und in den Ticketfiltern. | `Dialog` bleibt die Basis für die seitliche Feldhilfe. Vor einem Wechsel zu `Popover` oder einer anderen Hülle zuerst Anker, Modalität, Fokus, Scrollgrenzen und mobile Bedienung vergleichen. Ein `Tooltip` deckt kurze Zusatztexte ab. |

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

1. **Gesammelte Nachbearbeitung.** Die oben freigegebenen neun Punkte einzeln
   umsetzen, reviewen, committen und gezielt mit Vergleichsaufnahmen nachprüfen.
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
