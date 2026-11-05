# Native Vaadin-Bausteine: Bestandsaufnahme und offene Planung

## Auftrag und Grenze

Die projektweite Bestandsaufnahme und Einschätzung sind vom Benutzer abgenommen.
Der Umbau von Badges und weiteren ausgewählten Bausteinen wird als separate spätere
Umsetzung behandelt. Er ist vom abgeschlossenen Google-/Microsoft-Login und der
2FA-Umsetzung getrennt. Ein Bausteinumbau ist
ausdrücklich noch nicht beauftragt. Dieser Plan bleibt als Grundlage für den separaten
Start und die gemeinsame Auswahl der Arbeitspakete offen.

Arbeitsbasis ist `feature/google-microsoft-anmeldung`, nach der Profilbild-
Löschbestätigung. Bestehendes Aussehen, Fachregeln und Sicherheitsprüfungen sind
weiter verbindlich. Es wird keine neue Theme- oder Abhängigkeitsversion benötigt.

Untersucht wurden die 221 Java-Anwendungsdateien, insbesondere Views,
UI-Komponenten, UI-Basisklassen und UI-Helfer, die beiden eigenen Frontendmodule,
die zugehörigen Themequellen sowie relevante Shared-Texte, Aufrufer und Tests.
Die HTML-Referenzen wurden als Gestaltungsvorgabe berücksichtigt. Abhängigkeiten,
generierte Quellen, Datenbanken, private Konfiguration und Logs wurden nicht
pauschal durchsucht. Verfügbarkeit wurde zusätzlich an den lokalen JAR-APIs von
Vaadin 25.3.2 geprüft, nicht allein aus der neuesten Online-Dokumentation abgeleitet.

## Warum die aktuellen Badges anders gebaut sind

Der Hinweis ist berechtigt: `com.vaadin.flow.component.badge.Badge` und
`BadgeVariant` sind bereits vorhanden. Die Anwendung importiert derzeit keine
dieser Klassen. Vaadin unterstützt Beschriftung, Icon, Zahl und Varianten wie
`SUCCESS`, `WARNING`, `ERROR` und `NUMBER_ONLY` über eigene APIs.
Quelle: [Vaadin Badge](https://vaadin.com/docs/latest/components/badge).

Im Code stehen stattdessen zwei Muster: `Span` mit dem älteren Lumo-Thema
`badge` sowie `Span`/`Div` mit lokalen Cosmos-Klassen. Damit wurde die
HTML-Gestaltung nachgebildet, ohne diese Anzeigen auf die inzwischen vorhandene
Badge-Komponente umzustellen. Das erklärt den technischen Istzustand. Es ist
kein Beleg, dass die native Komponente unser Design nicht abbilden könnte.

Die vorhandenen Spans sind zwar selbst Vaadin-Flow-HTML-Komponenten. Sie sind
aber nicht die spezialisierte Badge-Komponente. Das ist die entscheidende
Unterscheidung. Ein reines Suchen nach Variablennamen wie `iconBadge` würde
zusätzlich viele bloße Iconrahmen fälschlich als Badge mitzählen.

## Badge-Inventar: 27 Anzeigearten in zwölf Dateien

Gezählt werden unterschiedliche wiederverwendete Erzeugungs-/Darstellungsstellen,
nicht die Zahl der zur Laufzeit sichtbaren Benutzer, Rollen oder Nachrichten.
Ein Renderer kann viele Badges erzeugen. Eine gemeinsame Stelle kann in Inbox,
Overlay und Dialog erscheinen. Sechs der 27 Stellen verwenden noch das ältere
`theme="badge"`-Muster, die übrigen 21 nur lokale Gestaltung.

| Quelldatei / Bereich | Anzahl | Konkrete Anzeigen und aktueller Aufbau |
| --- | ---: | --- |
| [PortalSidebarComponent](../../app/src/main/java/de/derpeterson/app/ui/components/PortalSidebarComponent.java) | 3 | Kontostatus (`active`, älteres Lumo-Badge), Anwesenheit (`presence`), Ungelesen-Zähler (`unread`) |
| [UserPopoverMenu](../../app/src/main/java/de/derpeterson/app/ui/components/UserPopoverMenu.java) | 2 | `accountStateText` und `menuPresence`, beide lokale Spans |
| [UserNotificationBell](../../app/src/main/java/de/derpeterson/app/ui/components/UserNotificationBell.java) | 1 | Header-Zähler `cosmos-notification-count` |
| [PortalHomeComponent](../../app/src/main/java/de/derpeterson/app/ui/components/PortalHomeComponent.java) | 1 | Roter Ungelesen-Zähler am Startseitenbutton |
| [UserNotificationsPanel](../../app/src/main/java/de/derpeterson/app/ui/components/UserNotificationsPanel.java) | 4 | Filterzähler, Overlay-Kopfzähler, Quelle Administration/System und Gelesen/Ungelesen in `messageMetadata`; letzteres älteres Lumo-Badge |
| [AccountSessionsComponent](../../app/src/main/java/de/derpeterson/app/ui/components/AccountSessionsComponent.java) | 4 | Sitzungsanzahl, Sitzungslimit, aktuelles Gerät (älteres Lumo-Badge), aktive/gespeicherte Sitzung. Auch in „Anmeldung fortsetzen“ verwendet |
| [AdminConfigurationSection](../../app/src/main/java/de/derpeterson/app/views/admin/AdminConfigurationSection.java) | 1 | Einstellungssumme im Panelkopf |
| [AdminExternalLoginSection](../../app/src/main/java/de/derpeterson/app/views/admin/AdminExternalLoginSection.java) | 1 | Nicht eingerichtet/Inaktiv/Aktiv; ein gemeinsamer Erzeugungspfad für Google und Microsoft |
| [AdminMailTestSection](../../app/src/main/java/de/derpeterson/app/views/admin/AdminMailTestSection.java) | 1 | SMTP-Test mit Statuspunkt, Tooltip und zugänglicher Statusinformation |
| [AdminUserFilterBar](../../app/src/main/java/de/derpeterson/app/views/admin/AdminUserFilterBar.java) | 1 | Anzahl aktiver Filterkategorien |
| [AdminUserManagementSection](../../app/src/main/java/de/derpeterson/app/views/admin/AdminUserManagementSection.java) | 4 | Aktiv und Anwesenheit (zwei ältere Lumo-Badges), Rollen, „Dein Benutzer“ |
| [AdminUserNotificationsSection](../../app/src/main/java/de/derpeterson/app/views/admin/AdminUserNotificationsSection.java) | 4 | Intern-/Versandstatus, Administration in der Vorschau, Gesendet in der Historie (älteres Lumo-Badge), Anzahl gesendeter Benachrichtigungen |
| **Summe** | **27** | **Sechs ältere Lumo-Badge-Stellen, 21 lokale Anzeigen; null native `Badge`-Verwendungen** |

Nicht mitgezählt sind Panel-Iconrahmen, Avatar-Statuspunkte, normale Zeit-/Hilfstexte,
Zeichenzähler und die großen Kennzahlkarten im Benutzer-/Benachrichtigungskopf.
Der runde Empfängerüberlauf „+N“ ist Teil einer Avatargruppe und wird separat
betrachtet. „Vorschau geprüft“ ist aktuell eine Statuszeile mit Icon, kein
automatisch zu ersetzender Badgekasten.

Die Statusspalte der Benutzertabelle ist ausdrücklich ohne sichtbaren Badgekasten
gestaltet. Hier vor einem Umbau klären, ob nur das alte Theme entfernt oder
`Badge` bewusst ohne Kasten gestaltet werden soll. Die Bestandszahl ist keine
Anweisung, alle 27 Anzeigen ungeprüft gleich aussehen zu lassen.

## Weitere Bausteine mit nativen Alternativen

| Bereich und Fundstellen | Native Möglichkeit | Einschätzung und Grenze |
| --- | --- | --- |
| `OverlayUserIcon`: fünf Erzeugungsstellen in vier Dateien (`TrackedUserAppLayout`, `PortalSidebarComponent`, `UserPopoverMenu`, zwei Stellen in `AdminUserManagementSection`) | [Avatar](https://vaadin.com/docs/latest/components/avatar) | Sinnvoller Kandidat für das eigentliche Bild/Initialen. Anwesenheitspunkt und Statusauswahl bleiben eine fachliche Ergänzung. Bildrevision, Cache, Initialen, lange Namen und geschützte Bildressourcen müssen erhalten bleiben. Mittlerer bis höherer Aufwand wegen gemeinsamer Nutzung. |
| Empfänger-Vorschau in `AdminUserNotificationsSection.refreshRecipientSummary` | `AvatarGroup` | Einzelne Avatare sind bereits native `Avatar`; nur Anordnung, sichtbare Begrenzung und „+N“ sind selbst zusammengesetzt. Standard-Überlauf öffnet eine eigene Liste. Diese Bedienung erst mit der vereinbarten separaten Detailsliste vergleichen. Kleiner bis mittlerer Aufwand, nicht bloß andere CSS-Klasse. |
| `CosmosCardComponent`, `PublicPanelComponent`, `AccountPanelComponent`, `PortalContextCard`: 26 Konstruktorstellen in 19 Dateien | [Card](https://vaadin.com/docs/latest/components/card) | Die aktuelle Hauptkarte ist bereits ein natives `VerticalLayout`, hat aber selbst gebaute Kopf-/Inhalts-/Footerstruktur. Native `Card` ist ein sinnvoller Pilot. Erst eine öffentliche Formularseite und eine Adminkarte vergleichen, dann zentrale Wrapper ändern. Höherer visueller Wirkungsbereich. Die 26 Stellen zählen nur explizite Konstruktoraufrufe, nicht alle dynamischen Panels. |
| Gemeinsame Bestätigungen in Sitzungen, Profilbild und Benachrichtigungen; eigener Benutzer-Löschdialog | [ConfirmDialog](https://vaadin.com/docs/latest/components/confirm-dialog) | Vier Einstiegspunkte. Schon heute echte Vaadin-`Dialog`-Komponenten mit nativer Modalität. `ConfirmDialog` unterstützt auch Component-Inhalt und eigene Buttons. Deshalb als Vereinfachung prüfen; Kontextkarten, Footer, Icons, Fokus, Abbrechen, doppelte Bestätigung und Sprachwechsel erhalten. Mittlerer Aufwand, kein aktueller Sicherheitsfehler. |
| Formularraster in `RegistrationView`, `AccountSettingsComponent`, `AdminConfigurationSection`, Benutzerformular in `AdminUserManagementSection`, Anbieter-Zugangsdaten in `AdminExternalLoginSection` | [FormLayout](https://vaadin.com/docs/latest/components/form-layout) | Fünf Quellbereiche, teilweise mehrere Formulare pro Datei. Bereits native Eingaben, aber eigene horizontale Layouts/CSS-Raster. Responsive Regeln, Außen-Infoicons und Feldlabelzuordnung müssen vor einer Umstellung nachgebaut werden. Optionaler Pilot, kein pauschaler Austausch aller Divs. |
| Vier manuelle Anwesenheitsbuttons in `UserPopoverMenu.createStatusOptions` | Native Auswahlkomponente mit Renderer, etwa `RadioButtonGroup` | Eine Stelle. Kann Auswahlsemantik und Tastatursteuerung vereinfachen. Renderer mit Statusicon und Beschreibung prüfen. SideNav-/Avatar-Statusauswahl verwendet bereits `Select`; dort kein zweiter Umbau nötig. |
| Sprachwahl in öffentlichem Kopf und Benutzermenü | Native Einzelauswahl oder bestehende Buttons | Kleine DE-/EN-Button-Gruppen bestehen bereits aus nativen `Button`. Eine `RadioButtonGroup` wäre eine mögliche zusätzliche Standardisierung, nicht zwingend ein Fehler. Bedienung und Platzbedarf zuerst bewerten. |
| `PageFrameComponent`, `PortalPageComponent`, `TrackedUserAppLayout`, `PublicPageComponent` | [AppLayout](https://vaadin.com/docs/latest/components/app-layout) und normale Layouts | Die angemeldete View-Hierarchie basiert bereits auf `AppLayout`; innen liegt der ausdrücklich vereinheitlichte eigene Seitenrahmen. Kein fehlender nativer Seitennavigationsbaustein: `PageNavigationComponent` verwendet `SideNav`. Eine erneute Rahmenumstellung hätte hohen Seiteneffekt und ist kein empfohlenes erstes Paket. |
| `AdminSidePanel` als rechter Hilfe-/Filterdrawer | `Dialog`; gegebenenfalls [MasterDetailLayout](https://vaadin.com/docs/latest/components/master-detail-layout) | Schon nativ modal. MasterDetailLayout ist für Master/Detail-Inhalte gedacht und hat andere Platz-/Overlayregeln. Kein automatischer Ersatz nur wegen ähnlicher Position. Das bestehende Verhältnis zu Header und Copyright muss erhalten bleiben. |

## Bereits native oder bewusst benötigte Ergänzungen

- Eingaben: `TextField`, `EmailField`, `PasswordField`, `TextArea`, `DatePicker`,
  `IntegerField`, `BigDecimalField`, `ComboBox`, `MultiSelectComboBox`, `Select`,
  `Checkbox`, `CheckboxGroup`, `RadioButtonGroup` und der neue `Switch`.
- Benutzertabelle: `Grid` mit nativer Sortierung und Komponentenrenderern.
  Pagination im Admin sowie im Postfach ist aus nativen Buttons/ComboBox und
  Layouts aufgebaut. Für die hier verwendete Flow-Version ist kein gleichwertiger
  Standard-Paginationbaustein als direkter Ersatz nachgewiesen. Keine neue
  Drittanbieter-Abhängigkeit allein für die Nummernleiste einführen.
- Postfachfilter: bereits `RadioButtonGroup`, keine selbst gebauten Tabs.
  Aktiv-/Status-/Rollenfilter und Rollen-/Empfängerauswahl verwenden ebenfalls
  native Auswahlkomponenten. Die `Div`-Renderer sind deren Inhalt, keine
  parallel implementierten Auswahloverlays.
- Aufklappbare Empfänger-/Versanddetails: bereits native `Details`. Kein
  `Accordion`-Umbau allein wegen eines Aufklappsymbols erforderlich.
- Header-Menüs und Benachrichtigungsvorschau: native `Popover`; globale Meldungen:
  native `Notification`; Hinweise: native `Tooltip`; Profilbildupload: native `Upload`.
- `CompactTimePicker` erweitert `TimePicker` nur um die vereinbarte kompakte
  Eingabe. `AdminUserActivityFilter` ist schon `CustomField<LocalDateTime>`.
  `AdminDurationField` setzt `IntegerField` und `Select<Unit>` zusammen.
  Eine spätere `CustomField`-Kapselung wäre eine fachliche Verbesserung, keine
  fehlende native Dauer-Komponente und nicht Teil eines Badge-Pakets.
- `account-avatar-editor.js` ergänzt Bildausschnitt, Zoom und Vorschau; Upload
  und Buttons sind nativ. Kein gleichwertiger eigener Flow-Bildausschnitt ist in
  der verwendeten Standard-API nachgewiesen. Diesen beauftragten Eigenanteil erhalten.
  Gleiches gilt für den kompakten Zeitparser in `compact-time-input.js`.
- Textareas sind echte `TextArea`. Die vertikale Größenänderung ergänzt die
  Browser-CSS-Eigenschaft `resize`; `setMinRows`/`setMaxRows` ersetzen diese
  Bedienung nicht. Kein Anlass, die Felder neu zu bauen.
- Lucide-Icons wurden ausdrücklich anstelle der gesetzten Vaadin-Icons gewählt.
  `AppIcon` liefert native `SvgIcon`. Diese Entscheidung bleibt bestehen.
- E-Mailvorlagen benötigen HTML. Dort wären Flow-Komponenten
  kein sachgerechter Ersatz. Hintergründe, Servicezeichen, normale Überschriften,
  Fließtexte und Layoutcontainer sind ebenfalls kein Komponenten-Nachbau.
- Vaadins kommerzielles [Dashboard](https://vaadin.com/docs/latest/components/dashboard)
  löst andere Aufgaben (Widgets, Verschieben, Größenänderung) als unsere statischen
  Schnellzugriffe. Es ist kein sinnvoller Pflichtumbau der Startseite.

## Empfohlene Reihenfolge nach Freigabe

1. **Badge-Pilot:** Ein einfacher Text-/Statusbadge und ein roter Zahlbadge mit
   nativer `Badge`-Komponente. Bisherige Farben, Größen, Abstände und sichtbare
   Texte erhalten. Nur passende Theme-Varianten aus der tatsächlichen Version
   verwenden. Zugängliche Bezeichnung auch bei reinen Zahlen erhalten.
2. **Badge-Paket:** Nach visueller Abnahme die freigegebenen Inventarstellen über
   einen kleinen gemeinsamen Cosmos-Baustein auf `Badge` umstellen. Variablen wie
   Quelle, Rolle, Anzahl und Versandzustand bleiben fachlich unverändert. Nicht
   mehr benötigte lokale/alte Lumo-Regeln erst danach entfernen.
3. **Avatar-/AvatarGroup-Pilot:** Gemeinsames Profilbild und Empfängerüberlauf
   getrennt testen; Statuspunkt, geschützte Bilder und Detailsliste erhalten.
4. **Card-Pilot, danach Bestätigungsdialoge:** Jeweils gesonderte Abnahme vor dem
   Umbau zentraler Wrapper. Eingaben und Speicherung bleiben unangetastet.
5. **Optionale Strukturpakete:** FormLayout und Anwesenheitsauswahl nur nach
   Entscheidung über Nutzen und gewünschte Bedienung. Seitenrahmen aktuell behalten.

Aufwand relativ zum Risiko: Badges klein bis mittel, Avatar mittel, Card mittel
bis groß wegen der zentralen CSS-/DOM-Verträge, Seitenrahmen groß. Ohne den Pilot
lassen sich belastbare Stundenangaben nicht aus der Fundstellenzahl ableiten.

## Abnahme- und Prüfkriterien des späteren Pakets

- Kein sichtbares Redesign: DE/EN, Desktop/Smartphone, schmale Header und lange
  Texte mit dem bisherigen Vaadin-Stand vergleichen. Fokus, Tooltip, Disabled,
  Statusfarben, Null/99+/hohe Zähler und Live-Aktualisierung prüfen.
- Bestehende Browserfälle erweitern statt neue parallele Journeys anlegen:
  `AccountSettingsBrowserTest`, `SessionManagementBrowserTest`,
  `NotificationInboxBrowserTest`, `UserNotificationsBrowserTest`,
  `UserManagementBrowserTest`, `CosmosComponentsBrowserTest` und zugehörige
  Designfälle anhand der tatsächlich betroffenen Klassen auswählen.
- Fach-/Rechteprüfung, serverseitige Zuordnung, Sessionwiderruf und Bildrevision
  nicht in Renderern verschieben oder für DOM-Umstellungen abschwächen.
- Nach jeder freigegebenen Teilumsetzung Theme-Anleitung und
  betroffene Tests gemeinsam nachziehen. Bei API- oder Selektoränderungen die
  tatsächlichen Aufrufer prüfen; keinen kompletten Umbau auf einmal durchführen.

Diese Abnahmekriterien sind noch kein aktuell fehlender automatisierter Nachweis
einer implementierten Änderung. Sobald ein Paket beauftragt ist, konkreten offenen
Testbedarf zentral in `docs/testbedarf.md` führen.
