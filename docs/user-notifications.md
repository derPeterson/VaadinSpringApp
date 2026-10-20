# Persönliche Benachrichtigungen

Aktualisierung 20.10.2026: Produktionsbuild und Browserabnahme des fertigen Redesigns
sind abgeschlossen, DE/EN je 152 eindeutige Gesamtfälle erfolgreich.
[Gesamt-QA, Erweiterungen und Messgrenzen](meilensteine/2026-10-20-gesamtqa-und-benachrichtigungsredesign.md).
Die älteren Ergebnisabschnitte unten beschreiben den jeweiligen damaligen Stand.

Erstimplementierung: 18.10.2026, auf Grundlage von `7dedccc`.
Für diesen damaligen ersten Umfang waren Browserabnahme und Produktionsbuild noch offen;
der aktuelle Abschluss des fertigen Redesigns ist oben verlinkt.
Die zuvor abgestimmte [Planung](mitteilungsfunktion-plan.md) bleibt als ursprüngliche
Entscheidungsgrundlage erhalten; dieses Dokument beschreibt den tatsächlichen Vertrag.

Erweiterung vom selben Tag: [Rollenänderungen ohne Neuanmeldung](role-updates-and-bootstrap.md)
ergänzen den automatischen Typ `ROLES_CHANGED`, aktualisierte Zugriffssynchronisierung und
die gezielte Erweiterung bestehender H2-Enum-Spalten. Die Nachweise des ersten Umfangs unten
beziehen sich auf den Stand vor dieser Erweiterung.

## Bedienung

- Angemeldet: Das Paperplane-Symbol im Header zeigt die Anzahl ungelesener Benachrichtigungen,
  oberhalb von 99 als `99+`. Ein Klick öffnet bis zu fünf aktuelle Einträge.
  Anonym bleiben Benachrichtigungen und Konto-Schnelleinstieg ausgeblendet.
- Alle Benachrichtigungen anzeigen öffnet `/notifications`: wahlweise 10, 20 oder 50 Einträge pro Seite (Standard 10), neueste zuerst,
  Filter Alle/Gelesen/Ungelesen, Aktualisieren und Vor-/Zurückblättern.
- Das Öffnen der Header-Vorschau und eines Detaildialogs verändert den Lesestatus nicht.
  Nur „Als gelesen markieren“ beziehungsweise „Alle als gelesen markieren“ ändert ihn. „Alle als gelesen markieren“ verwendet die zuletzt geladene
  Liste der eigenen ungelesenen Empfangs-IDs; später eingetroffene Einträge bleiben ungelesen.
- Detaildialog: Escape, Außenklick, Schließen-Icon und Footer-Schließen. Systemeinträge
  bieten zusätzlich den internen Einstieg in Konto/Sicherheit an.
- Lesestatus gehört zum Konto, unabhängig vom verwendeten Tab oder Gerät.
  Neue Benachrichtigungen und Leseänderungen lösen nach Commit lokale Push-Aktualisierungen aus.

## Gemeinsame Anwendungskopfzeile

Home, Kontokonfiguration und `/notifications` erzeugen ihre Kopfzeile zentral über
`TrackedUserAppLayout.createApplicationHeader`: Service-Logo und konfigurierter
Servicename links, bestehende Konto-Schnelleinstiege und Avatar rechts. Darstellung,
Avatar-/Popover-Anbindung und Cosmos-Menütheme werden nur dort gepflegt. Auf den
persönlichen Seiten kommt auch die Menübelegung aus derselben Methode: Startseite
im Avatar-Menü und Verwaltung ausschließlich für Admin/Superadmin. Home behält
seine bisherigen Menüziele einschließlich Registrierung/Anmeldung für anonyme Gäste.
Ein zusätzlicher Startseite-Button direkt im Header wird nicht erzeugt.

Damit sind das fehlende Logo und der fehlende Verwaltungseinstieg der
Benachrichtigungsseite korrigiert. Der Adminbereich behält seine eigene Navigation
mit Drawer-Umschalter. Die bestehenden CSS-Regeln und Abmessungen bleiben erhalten.

Gezielter Nachweis: **138 JUnit-Prüfungen**, keine Fehler/Fehlschläge/Skips: 20 neue
Headerfälle für alle drei Seiten, Benutzer/Admin/Superadmin und anonymes Home in
DE/EN einschließlich Sprachwechsel und tatsächlicher Button-Navigationsaktionen;
zusätzlich 21 Kontoseiten-, vier Konto-Schnelleinstiegs- und 93 Benachrichtigungs-
Komponentenprüfungen. Shared und Anwendung mit Java 25 in isolierter Quellkopie
gebaut; Spotless apply/check und Diffprüfung erfolgreich. Kein Browserlauf und
kein Produktions-/Frontendbuild. Durch die neue Basismethode ist zur Übernahme
ein Anwendungsneustart mit anschließendem Strg+F5 empfohlen.

## Persönliche Seite: Seitenkopf

Die bestehende Seite `/notifications` verwendet denselben
`CosmosSectionHeaderComponent` wie „Mein Konto“: Bereichszeile „MEIN KONTO /
BENACHRICHTIGUNGEN“, Titel „Benachrichtigungen“ mit Akzentpunkt und Beschreibung
„Alle Hinweise, Systemmeldungen und Aktualisierungen an einem Ort.“. Schriftgrößen,
Farben und Abstände kommen vollständig aus dem bestehenden Seitenkopf-Standard.
Alle drei Texte folgen der aktuellen Sprache (DE/EN); die Beschreibung steht im
Seitenkopf statt zusätzlich in der Inbox-Card. Gezielte Prüfung: 87 Komponenten-
und 13 Sprach-/Katalogprüfungen erfolgreich (100 insgesamt, keine Fehler/Skips),
einschließlich zwei neuer Sprachwechselprüfungen des Seitenkopfs. Shared und
Anwendung mit Java 25 in einer eigenen Quellkopie gebaut und abgeglichen;
Spotless apply/check erfolgreich. Kein Produktions-/Frontendbuild und kein
Browserlauf. Browserabnahme dieses Seitenkopfs offen. Shared ist installiert;
Anwendung neu starten und Strg+F5.

## Gemeinsame Konto-Seitenauswahl

Unter dem Seitenkopf und über dem ersten Panel stehen auf der Konto- und der
Benachrichtigungsseite die Tabs „Einstellungen“ (Cog) und „Benachrichtigungen“
(Paperplane). Die gemeinsame Vaadin-Komponente verwendet dieselben CSS-Regeln
wie die bestehenden Profil-/Sicherheits-/Gerätetabs, einschließlich gleich breiter
Einträge, aktivem Unterstrich, Tastaturbedienung und Scrollverhalten. Ihre Breite
folgt den mittig begrenzten Panels mit maximal 1100px. Seitenkopf, Tabs und erstes Panel liegen auf beiden
Seiten in einer gemeinsamen Inhaltsgruppe mit `gap: var(--lumo-space-l)` (24px im
bestehenden Theme). Der äußere Seitenabstand zum Copyright bleibt erhalten.

Die aktive Auswahl folgt der aufgerufenen Seite; ein Wechsel navigiert nach
`/account` beziehungsweise `/notifications`. Die vorhandenen drei Einstellungstabs
und ihre Queryparameter bleiben erhalten. Beschriftungen und zugänglicher Name
folgen einem Sprachwechsel. Die Komponente steht auf beiden Seiten an derselben
Position; die Startseite erhält keine zusätzliche Tab-Leiste.

Gezielter Nachweis: **177 JUnit-Prüfungen erfolgreich**, keine Fehler,
Fehlschläge oder Skips: 20 gemeinsame Header-/Navigationsfälle, 21 Kontoseiten-,
18 Kontokomponenten-, 105 Benachrichtigungskomponenten- und 13 Sprach-/Katalogfälle.
Die Navigation ist in DE/EN für Benutzer/Admin/Superadmin einschließlich aktivem
Tab, Position, Sprachwechsel, beiden Routenzielen und Wiederanheften ohne erneute
Navigation geprüft. Der zunächst unvollständige UI-Router-Testaufbau wurde durch
eine eigene aufzeichnende Test-UI ersetzt. Shared und sämtliche Java-/Testquellen
mit Java 25 in eigener Quellkopie gebaut, 357 Quelldateien abgeglichen; Spotless,
CSS-Syntax und Diffprüfung erfolgreich. Keine Browserprüfung und kein
Produktions-/Frontendbuild. Zur Übernahme: Shared ist installiert; Anwendung
neu starten und Strg+F5.

Nachtest der einheitlichen Seitenabstände: 125 Komponenten-/Headerprüfungen
erfolgreich, keine Fehler/Skips. Java-/Testkompilierung mit Java 25 in eigener
Quellkopie, 357 Quelldateien abgeglichen, Spotless apply/check, CSS-Syntax und
Diffprüfung erfolgreich. Keine Browserprüfung. Zur Übernahme Strg+F9, Reload
und Strg+F5.

## Persönlicher Posteingang: Panelentwurf

Die Inbox steht als mittig begrenzte Standard-Card (maximal 1100 px) unter dem
Seitenkopf. Der Panelkopf verwendet Archive, „Benachrichtigungen“ und den
Secondary-Text „Persönliche Hinweise lesen und den Lesestatus verwalten.“. Gesamt-
und Ungelesen-Zähler rechts beziehen sich auf den eigenen sichtbaren Posteingang,
unabhängig von der aktuellen Suche und Seite; entfernte/abgelaufene Einträge fehlen.

Die Werkzeugleiste zeigt links die Suche über Betreff/Nachricht/Herkunft, mittig
die native Segmentauswahl Alle/Gelesen/Ungelesen mit denselben Styles wie die
Header-Vorschau und rechts drei Iconbuttons: Aktualisieren (Refresh), alle als
gelesen markieren (eigenes SVG mit geöffnetem Umschlag und Häkchen) sowie alle
gelesenen Benachrichtigungen löschen (roter Mülleimer). Die Buttons haben lokalisierte Tooltips und
zugängliche Namen; der bisherige Erklärungstext unter der Leiste ist entfernt.
Bei schmalen Ansichten stehen Suche und Icons oben, die Segmentauswahl mittig
darunter. Standardfilter ist Alle. Alle drei Zustände gelten auch bei der Suche und
werden vor Pagination in der Datenbank angewendet; Suche und Filter wechseln auf
Seite 1, während Sprache und Seitengrößenwechsel den gewählten Filter erhalten.
Die Suche berücksichtigt die sichtbaren DE/EN-Texte automatischer Hinweise;
manuelle Texte bleiben in ihrer Originalsprache. Sie wird vor der Pagination in der
Datenbank angewendet. Groß-/Kleinschreibung ist unerheblich; `%`, `_` und `!` werden
wörtlich gesucht. Maximal 120 Zeichen; ein leerer Suchtext stellt die normale Liste
wieder her. Suche und Filter starten auf Seite 1, ein Sprachwechsel erhält die Eingaben.
Eigentümertrennung, persönliche Löschung und aktuelle Aufbewahrungsfrist bleiben
für sämtliche Ergebnisse und Zähler wirksam.

Karten zeigen Betreff, höchstens drei Vorschauzeilen, Datum, fett gesetzte Herkunft
und Lesestatus-Badge. Metadaten und Aktionsbuttons teilen sich die unterste Zeile:
Metadaten links, Aktionen rechts, vertikal mittig ausgerichtet. Bei wenig Platz dürfen
die Aktionen unter die Metadaten umbrechen. Auch die Header-Vorschau zeigt Betreff
und Nachricht vor einer gemeinsamen untersten Zeile mit Metadaten links und
Iconbuttons rechts; bei wenig Platz darf diese Zeile umbrechen. Betreff und Vorschautext nutzen darüber die gesamte
Kartenbreite; lange Betreffzeilen bleiben einzeilig mit Auslassungszeichen. Die inneren
Gridspalten dürfen schrumpfen, damit weder Titel noch Vorschautext über die Karte
laufen. Der vollständige Inhalt bleibt im Detaildialog verfügbar. CSS-Syntax geprüft,
Browserabnahme der neuen Anordnung offen.
Hintergrund und Ungelesen-Punkt entsprechen dem Overlay.
Inbox und Overlay haben dasselbe Lösch-X rechts in der Betreffzeile (Close Circle,
20px-Icon, lokalisierter Tooltip und zugänglicher Name mit Betreff). Beide verwenden
die gemeinsame Button-Erzeugung und persönliche Löschaktion. Liste und Zähler
werden danach aktualisiert; es wird weder ein Dialog geöffnet noch als gelesen
markiert. Versandhistorie und andere Empfänger bleiben unberührt.
Gezielter Nachtest: 101 Komponentenprüfungen erfolgreich, keine Fehler/Skips;
vier neue DE/EN-Ausprägungen für gelesene und ungelesene Inboxeinträge prüfen
Löschaktion, Tooltip, Empfänger-ID sowie Listen-/Zähleraktualisierung. Spotless,
CSS-Syntax und Java-Build in eigener Quellkopie erfolgreich; keine Browserprüfung.
„Öffnen“ und „Als gelesen markieren“ sind in Inbox und Header-Vorschau quadratische
36px-Secondary-Iconbuttons mit Auge beziehungsweise Envelope Open. Ihre Texte
stehen als lokalisierte Tooltips bereit; zugängliche Namen enthalten zusätzlich den
Betreff. Die Einzel-Leseaktion bleibt nur für ungelesene Einträge sichtbar. Die
beschrifteten Aktionen im Detaildialog bleiben erhalten.
Einfacher Klick auf die Kartenfläche, Titel oder „Öffnen“ öffnet den bestehenden
Detaildialog; Button-Ereignisse lösen die Kartenaktion nicht zusätzlich aus.
„Als gelesen markieren“ betrifft nur den eigenen Eintrag. Öffnen verändert den
Lesestatus weiterhin nicht. Die Header-Vorschau behält ihren bisherigen Doppelklick
auf die Kartenfläche.

Die Pagination entspricht der Benutzerverwaltung: links „Zeilen pro Seite“, eine
native 56px-ComboBox mit 10/20/50 (Standard 10) und die gefilterte Trefferzahl; rechts
erste/vorherige/nummerierte/nächste/letzte Seite im kompakten Pagination-Stil.
Beim Wechsel der Seitengröße beginnt die Liste auf Seite 1; Suche und Lesestatusfilter
bleiben erhalten. Auch ein Sprachwechsel erhält Größe und Filter. Nach verkleinerter
Trefferzahl wird eine nicht mehr vorhandene Seite zurückgesetzt. Der Footer zeigt
darunter mittig die aktuelle Aufbewahrungsregel (einschließlich unbegrenzt). Auf schmalen Ansichten dürfen
Kopfzähler, Werkzeuge, Kartenaktionen und Footer umbrechen. Verwendet werden native
Vaadin-Komponenten/-Icons und lokale Anordnungsregeln mit vorhandenen Cosmos-Tokens.
Browserdarstellung und tatsächliche Klick-/Fokusgeometrie dieses Entwurfs sind offen.

Gezielter Nachweis: **93 Komponentenprüfungen, 65 H2-Persistenzprüfungen und
13 Sprach-/Katalogprüfungen** erfolgreich (171 insgesamt, zwölf neue Ausprägungen).
Zusätzlich **48 Browser-Helfer-/Reporterprüfungen** erfolgreich; jeweils keine
Fehler oder Skips. Enthalten sind Suche vor Pagination, lokale Systemtexte,
wörtliche LIKE-Sonderzeichen, Eigentümertrennung, Löschung/Aufbewahrung, Zähler,
Seitenwechsel, Sprachwechsel, Kartenklick und ausdrückliche Einzel-Leseaktion.
Shared und Java mit Java 25 in einer eigenen Quellkopie gebaut und per Hash
abgeglichen. Spotless apply/check, CSS-Syntax/Tokenprüfung und Änderungsreview
erfolgreich. Kein Browserlauf und kein Produktions-/Frontendbuild.
Zur Übernahme: Shared ist installiert; Anwendung neu starten und Strg+F5.

Gezielter Nachtest der Pagination: **95 Komponentenprüfungen erfolgreich**, keine
Fehler/Fehlschläge/Skips. Zwei neue DE/EN-Ausprägungen prüfen Größenwechsel,
Seitenreset, Such-/Ungelesen-Erhalt, Trefferzahl, Leerzustand und Sprachwechsel.
Im ersten Lauf scheiterten diese zwei neuen Fälle an unvollständig simulierten
letzten Seiten; die Testdaten wurden vervollständigt, die Assertions beibehalten.
Java 25, isolierte Quellkopie, Shared-/Java-Build, Spotless apply/check und
CSS-Syntaxprüfung erfolgreich. Kein Browserlauf und kein Produktions-/Frontendbuild.
Zur Übernahme Anwendung neu starten und Strg+F5.

Nachfolgender Nachtest der Inbox-Werkzeugleiste: **198 JUnit-Prüfungen erfolgreich**,
keine Fehler/Fehlschläge/Skips: 97 Komponenten-, 68 H2-Persistenz-, 13 Sprach-/Katalog-
und 20 gemeinsame Headerprüfungen. Fünf neue Ausprägungen prüfen alle Lesestatusfilter
vor Suchpagination, leere Suchtexte, Tooltips und Filter-/Spracherhalt; vorhandene
Checkbox-Erwartungen sind auf die neue Segmentauswahl umgestellt. Der bisherige
Suchaufruf mit booleschem Ungelesen-Parameter bleibt kompatibel. Shared/Java mit
Java 25 in isolierter und per Hash abgeglichener Quellkopie gebaut; Spotless und
CSS-Syntax erfolgreich. Kein Browserlauf oder Produktions-/Frontendbuild.
Durch die neue Suchüberladung und den entfernten Hinweistext zur Übernahme
Anwendung neu starten und Strg+F5.

## Gelesene Benachrichtigungen gesammelt entfernen

Nur auf der Seite „Alle Benachrichtigungen“ steht direkt rechts neben der Sammel-
Leseaktion ein roter Mülleimer im bestehenden Delete-Button-Stil. Tooltip und
zugänglicher Name lauten „Alle gelesenen Benachrichtigungen löschen“ (DE/EN).
Im Header-Overlay wird dieser Button nicht angeboten. Ohne gelesene Einträge ist
er deaktiviert. Suche, Lesestatusfilter und aktuelle Seite begrenzen die Aktion
nicht; sie bleiben bei der anschließenden Aktualisierung erhalten, soweit die
Seite noch vorhanden ist.

Die Aktion entfernt ausschließlich aktuell gelesene, noch sichtbare Einträge des
eigenen Kontos innerhalb der aktuellen Inbox-Aufbewahrung. Ungelesene Nachrichten,
andere Empfänger und die Versandhistorie bleiben erhalten. Wie beim einzelnen X
bleiben Empfangseinträge als persönlich ausgeblendet gespeichert, damit ein
idempotenter Versandwiederholungsversuch sie nicht wieder einblendet. Die Datenbank
prüft Eigentümer und Lesestatus unmittelbar beim Update. Erst nach erfolgreichem
Commit werden die betroffenen Kontoansichten aktualisiert; ein wirkungsloser
Wiederholungsaufruf und ein Rollback senden kein Ereignis.

Gezielter Nachweis: **189 JUnit-Prüfungen erfolgreich**, keine Fehler,
Fehlschläge oder Skips: 105 Komponenten-, 71 H2-Persistenz- und 13 Sprach-/Katalog-
prüfungen. Neue Fälle prüfen die globale Aktion trotz Such-/Ungelesenfilter,
Position/Tooltip/Deaktivierung, ihr Fehlen im Overlay, Eigentümertrennung,
ungelesene und abgelaufene Einträge, unbegrenzte Aufbewahrung, automatische
Hinweise, Versandhistorie, Idempotenz, Berechtigung und Rollback. Shared und alle
Java-/Testquellen mit Java 25 in eigener Quellkopie gebaut; 356 Quelldateien und
das SVG mit dem geprüften Stand abgeglichen. Spotless apply/check und Diffprüfung
erfolgreich. Keine Browserprüfung und kein Produktions-/Frontendbuild.
Zur Übernahme: Shared ist installiert; Anwendung neu starten und Strg+F5.

## Eindeutiges Icon für die Sammel-Leseaktion

„Alle als gelesen markieren“ verwendet in Header-Vorschau und Inbox dasselbe
`icons/envelopes-check.svg`: ein geöffneter Vaadin-Umschlag mit einem gefüllten
Bestätigungsbadge rechts unten. Die zusätzlichen Umschläge sind entfernt, damit
die Sammelaktion bei normaler Icongröße klar erkennbar bleibt. Das SVG übernimmt
`currentColor`; Freiräume und das Häkchen bleiben
transparent, wie das vorhandene Geräte-Icon. Buttongrößen, Tooltips und Aktionen
bleiben erhalten; einzelne Nachrichten verwenden weiterhin Envelope Open.
97 gezielte Komponentenprüfungen erfolgreich, keine Fehler/Skips. SVG-Struktur
geprüft und als Bild gerendert/angesehen; Java 25 in eigener Quellkopie gebaut,
Spotless und Diffprüfung erfolgreich. Keine Browserprüfung oder neue Coverage.
Zur Übernahme Strg+F9, Reload und Strg+F5.

## Header-Vorschau: Entwurf vom 19.10.2026

Die kompakte Vorschau verwendet den Header-Aufbau der Kontoansichten mit Paperplane,
Titel, Beschreibung und Schließen-Icon. Kopf, Inhalt und Footer beziehen ihre Flächen
und Trennlinien aus den gemeinsamen Dialog-Tokens. Der mittlere Bereich ist scrollbar;
Footer und Kopf bleiben außerhalb dieses Scrollbereichs. Die Liste verwendet eine
schrumpfbare Gridspalte, damit auch maximale Betreff-/Nachrichtenlängen keine
horizontale Scrollleiste erzeugen. Der Betreff bleibt einzeilig mit Auslassungszeichen;
die Nachrichtenvorschau ist auf drei Zeilen begrenzt und endet bei Überlänge mit
Auslassungszeichen. Lange Wörter dürfen innerhalb der Vorschau umbrechen. Die
X-Achse des Inhaltsbereichs ist gesperrt, vertikales Scrollen bleibt erhalten.
„Öffnen“ zeigt weiterhin den vollständigen unveränderten Inhalt im Detaildialog.
Die gezielte CSS-Korrektur wurde auf Syntax geprüft; eine Browserprüfung mit
maximalen Textlängen steht noch aus.

Ein Doppelklick auf eine Vorschaukarte öffnet denselben Detaildialog wie „Öffnen“.
Doppelklicks auf Buttons lösen diese Kartenaktion nicht zusätzlich aus. Auch dabei bleibt
der Lesestatus unverändert; gelesen wird ausdrücklich markiert. Historischer Nachweis der
Ereignisbehandlung: 58 Komponentenprüfungen erfolgreich, keine Fehler oder Skips;
die tatsächliche Browserausführung des Ereignisfilters bleibt ungeprüft.

Ungelesene Einträge zeigen einen Punkt und eine hervorgehobene Fläche. „Als gelesen
markieren“ betrifft nur den jeweiligen Eintrag und öffnet keinen Dialog. Der Filter
„Alle / Gelesen / Ungelesen“ im Stil des Profilbilddialogs ersetzt die Checkbox
der Header-Vorschau. Aktualisieren und die bisherige Sammelaktion bleiben verfügbar.
Manuelle Ankündigungen tragen „Administration“, automatische Sicherheits- und
Rollenhinweise „System“. Beim Öffnen einer Nachricht bleibt das Popover hinter dem
Detaildialog erhalten. Solange der Dialog offen ist, werden Außenklick und Escape nur
vom Dialog verarbeitet; danach gelten wieder die normalen Schließregeln des Popovers.
Der Envelopes-Button zur vollständigen Liste schließt das Popover bei der Navigation.
Die eigenständige Inbox behält ihre bisherige Darstellung. Der Aufbewahrungshinweis
im Footer ist mittig; die Werkzeugleiste enthält Aktualisieren und die Sammel-Leseaktion.
Eine dezente Zeile nennt die Grenze von fünf neuesten Einträgen passend zum Filter
(alle, gelesen oder ungelesen). Standard ist „Alle“; beim Sprachwechsel bleibt die
Auswahl erhalten. Der Filter wird vor Sortierung und Pagination in der Datenbank
angewendet. Eigentümertrennung, persönliche Löschung und Aufbewahrungsfrist gelten
für alle drei Zustände. Der Filter selbst ändert keinen Lesestatus. Die eigenständige
Inbox verwendet inzwischen dieselbe Segmentauswahl und eine eigene Pagination. Native Vaadin-Radiobuttons
übernehmen Tastatursteuerung, Auswahl und Fokus; die lokalen Styles verwenden die
Pillenform, Farben und Auswahlfläche des Profilbilddialogs. Schmale Ansichten ordnen
die Werkzeugbuttons bei Bedarf unter der Segmentauswahl an. Neue Browserabnahme offen.

Gezielter Nachtest des Filters: **85 Komponentenprüfungen, 59 H2-Persistenzprüfungen
und 13 Sprach-/Katalogprüfungen** erfolgreich (157 insgesamt, davon sechs neue
Ausprägungen; keine Fehler/Skips). Shared und Anwendung mit Java 25 in einer eigenen
Quellkopie gebaut und per Hash abgeglichen. Spotless apply/check, CSS-Syntax und
Änderungsprüfung erfolgreich. Kein Browserlauf oder Produktions-/Frontendbuild.
Zur Übernahme dieses neuen Filters Anwendung neu starten und Strg+F5.

Jede Vorschaukarte hat ein Schließen-Icon mit „Benachrichtigung löschen“. Es entfernt
nur diesen persönlichen Empfangseintrag aus Overlay, Inbox, Ungelesen-Zähler und
Sammel-Lesesnapshot. Technisch bleibt er mit `dismissed_at` bis zur regulären
Aufbewahrungsbereinigung erhalten. Das bewahrt den ursprünglichen Versandnachweis
und verhindert, dass ein idempotenter Versand-Retry den Eintrag wiederherstellt.
Andere Empfänger und die gesendete Historie sind davon nicht betroffen. Fremde IDs
werden nicht verändert; wiederholtes Entfernen bleibt folgenlos. Erst nach Commit
wird ausschließlich der betroffene Besitzer per bestehendem Push aktualisiert.

Vorheriger Overlay-Zwischenstand: 58 Komponentenprüfungen, 47 H2-Persistenzprüfungen,
13 Sprach-/Katalogprüfungen und 22 Admin-Navigationsprüfungen erfolgreich: insgesamt
140, keine Fehler oder Skips. Eigentümertrennung, Wiederholung, neue Sitzung,
Transaktionsrollback und Versand-Idempotenz nach dem Entfernen sind enthalten.
CSS-Syntax und verwendete Tokens separat geprüft.
Browserdarstellung, Scrollgeometrie und Fokus nach dem Schließen wurden für diesen
Entwurf noch nicht im Browser geprüft; die frühere nächtliche QA deckt ihn nicht ab.

Für diese Erweiterung Shared und Anwendung bauen und die Anwendung vollständig
neu starten. Hibernate ergänzt über die vorhandene `ddl-auto=update`-Konfiguration
das nullable Empfangsfeld `dismissed_at`; vorhandene Einträge bleiben sichtbar.
Die Nutzer-Datenbank wurde für diese Umsetzung weder gelesen noch verändert.

## Detaildialog: Entwurf vom 19.10.2026

Der Detaildialog verwendet den gemeinsamen Cosmos-Dialog mit Paperplane und dem
allgemeinen Header „Benachrichtigung“ (EN: „Notification“). Die Secondary-Zeile lautet
„Nachricht lesen und Lesestatus verwalten.“ (EN: „Read the message and manage its read status.“).
Betreff, Datum/Uhrzeit,
Herkunft und Lesestatus stehen ausschließlich in der gerahmten Inhaltskarte.
„Ungelesen“ beziehungsweise „Gelesen“ verwendet den bestehenden Badge-Hintergrund
und -Rahmen und steht direkt hinter der fett gesetzten Herkunft
(System/Administration) in der Metazeile. Die Nachrichtenkarte verwendet abhängig vom Lesestatus dieselben
Hintergründe wie die Overlay-Einträge: reguläre Card-Fläche bei gelesen, Tabellen-
Hervorhebungsfläche bei ungelesen; die Dialogfläche bleibt davon unabhängig. Vor dem Betreff erscheint bei ungelesenen
Einträgen derselbe blaue Punkt wie in der Header-Vorschau. Beim Markieren als gelesen
verschwindet er. Die Inhaltskarte verwendet 8 px Abstand zwischen den Zeilen. Zeilenumbrüche bleiben
erhalten, Inhalte werden weiterhin ausschließlich als Text dargestellt. Auf einen
zusätzlichen Erklärungshinweis wird verzichtet; Badge und Leseaktion vermitteln
das Verhalten.

Öffnen, Doppelklick, Sprachwechsel und Schließen markieren keinen Eintrag mehr als
gelesen. Die neue Secondary-Aktion „Als gelesen markieren“ betrifft ausschließlich
den geöffneten Empfangseintrag, lässt den Dialog offen und aktualisiert den Status.
Bei bereits gelesenem Eintrag entfällt die Aktion. Ein fehlgeschlagener Schreibzugriff
zeigt keinen erfolgreichen Lesestatus. Der Service prüft vor dem Öffnen erneut die
aktuelle Sitzung. Bestehende Eigentümerprüfung und Push-Ereignisse für Leseänderungen
bleiben erhalten; es gibt keine neue Datenbankmigration.

Im Footer steht „Alle Benachrichtigungen anzeigen“ (Envelopes) nur beim Öffnen aus
der Header-Vorschau; beim Öffnen aus der vollständigen Inbox ist der Einstieg
ausgeblendet. Daneben stehen die Leseaktion
(Envelope Open) und der primäre Schließen-Button (Close Circle). Automatische
Sicherheits-/Rollenhinweise behalten den internen Kontoeinstieg (Cog). Navigation
schließt Detail und Header-Popover; normales Schließen des Details erhält das Popover.
Breite und schmale Anordnung werden lokal mit bestehenden Tokens umgesetzt;
andere Dialoge bleiben unverändert. Browserdarstellung und Fokus sind noch ungeprüft.
Der vorhandene Browserhelfer für NOTIFY-02 markiert nun ausdrücklich als gelesen,
statt das Lesen aus dem Öffnen abzuleiten. Für diesen Zwischenstand wurde kein
Browserlauf gestartet.

Gezielte Prüfung des Dialogumbaus vor Entfernen des Erklärungshinweises: **73 Komponentenprüfungen** (davon 13 neue
Ausprägungen), **55 H2-Persistenzprüfungen** und **13 Sprach-/Katalogprüfungen**
erfolgreich, insgesamt 141 eindeutige Anwendungs-JUnits. Zusätzlich **48 Browser-
Helfer-/Reporterprüfungen** erfolgreich, ohne Browserstart. Jeweils keine verbleibenden
Fehler oder Skips. Vorherige Fehler in der neuen Testbaum-Auswertung separater
Vaadin-Header-/Footerbereiche wurden im Testhelfer korrigiert; keine Assertions
abgeschwächt. Shared und Java mit Java 25 in einer eigenen, per Quellhash abgeglichenen
Buildkopie gebaut. Spotless apply/check sowie CSS-Syntax und Tokenreferenzen geprüft.
Keine neue vollständige JUnit-/Browserrunde, kein Produktions-/Frontendbuild.
Nach vollständigem Entfernen des Erklärungshinweises wurden Shared und Anwendung
erneut gebaut sowie 13 Sprach-/Katalogprüfungen erfolgreich ausgeführt (keine
Fehler oder Skips). Hinweis, Getter, DE/EN-Schlüssel und zugehörige Styles sind entfernt.
Nach Vereinheitlichung der Status-Badges und der fett gesetzten Herkunft in Header
und Inhalt: 73 Komponentenprüfungen erfolgreich, keine Fehler/Skips; abschließende
lokale CSS-Präzisierung separat syntaktisch geprüft, weiterhin kein Browserlauf.
Nach Vereinfachung des Headers: **83 Komponentenprüfungen und 13 Sprach-/Katalogprüfungen**
erfolgreich (96 insgesamt, keine Fehler/Skips). Darin werden einfacher Klick auf
Titel/Öffnen sowie Doppelklick auf die übrige Vorschaukarte jeweils für gelesene und
ungelesene Einträge in DE/EN geprüft; Button-Ereignisse lösen die Kartenaktion nicht
zusätzlich aus. Die Klicklogik wurde nicht verändert. Dieser Nachweis betrifft
Java-Handler und den registrierten Ereignisfilter, keine neue Browserreproduktion.
Nach Ergänzung der Secondary-Zeile und rechtsbündigem Status-Badge: 8 gezielte
Komponenten- und 13 Sprach-/Katalogprüfungen erfolgreich, keine Fehler/Skips.
CSS-Syntax geprüft; kein Browserlauf.
Nach Umsetzen des Badges direkt hinter die Herkunft und Ergänzen des ungelesenen
Punkts: 8 gezielte Komponentenprüfungen erfolgreich (DE/EN, beide Lesestatus,
Punkt nach Markieren verborgen), keine Fehler/Skips. Kein Browserlauf.
Nach Angleichung der Nachrichtenkarte an beide Overlay-Hintergründe: 8 gezielte
Komponentenprüfungen erfolgreich, einschließlich Wechsel nach explizitem Markieren;
keine Fehler/Skips. CSS-Syntax und identische Hintergrund-Tokens separat geprüft.
Kein Browserlauf.
Zur Übernahme: Shared ist bereits installiert; Anwendung neu starten und Strg+F5.

## Superadmin-Versand

Nur mit ausdrücklich zugewiesener Rolle **Superadmin** erscheint Administration →
Benachrichtigungen (`/admin?section=notifications`). Normale Administratoren erhalten diesen Versandzugang
nicht. Eine unberechtigte direkte Bereichs-URL zeigt das Dashboard; die Serviceoperationen
weisen unberechtigte Aufrufe zusätzlich ab.

Einzelne/mehrere Konten oder alle freigeschalteten bestehenden Konten auswählen, Titel
und Nachricht ausfüllen, Vorschau prüfen und danach senden. Titel maximal 120, Text maximal
4000 Java-/Frontend-Zeichen. Beide Felder sind Pflichtfelder. Inhalte werden ausschließlich
als Text dargestellt, einschließlich Zeichenfolgen wie HTML. Manuelle Mitteilungen bleiben
in der vom Verfasser gewählten Sprache. Es werden keine zusätzlichen E-Mails versandt.

„Alle aktiven Konten“ meint freigeschaltet und verifiziert, unabhängig vom Online-Status:
aktivierte Gäste, Offlinekonten und der Absender sind eingeschlossen. Deaktivierte und
noch nicht verifizierte Konten fehlen. Später registrierte Konten erhalten frühere
Rundmitteilungen nicht nachträglich.

Die Vorschau fixiert die Empfänger-IDs. Vor dem Versand wird die berechtigte Menge erneut
aus skalaren Datenbankwerten bestimmt. Änderungen erfordern eine neue Vorschau; eine
nachträgliche Erweiterung passiert nicht stillschweigend. Bei gezielter Auswahl sind nur
Änderungen innerhalb dieser Auswahl relevant. Kontomutationen und manueller Versand
verwenden dieselbe Superadmin-Rollensperre und Sperrreihenfolge.

Ein UUID-Vorgangsschlüssel bleibt bei einem fehlgeschlagenen/unklar beantworteten
Versuch erhalten. Gleicher Absender, Inhalt, Auswahlart und Empfängersnapshot liefern bei
Wiederholung dieselbe Sendung. Abweichende Wiederverwendung wird abgewiesen. Die DB erzwingt
zusätzlich einen eindeutigen Schlüssel und eindeutige Kombinationen aus Nachricht und Empfänger.
Ein neuer Entwurf ist eine neue Sendung. Die Wiederholungserkennung gilt innerhalb der
Aufbewahrung; bereits gelöschte Empfänger können eine alte Wiederholung zum Konflikt machen.

Eigene gesendete Nachrichten erscheinen mit Versandzeit, Inhalt und ursprünglicher
Empfängeranzahl, seitenweise zu 20 Einträgen. Es gibt keine fremden privaten System-Inboxen,
individuellen Leselisten, nachträgliche Bearbeitung oder Rücknahme der Sendung.

## Automatische Ereignisse und Transaktionen

| Erfolgreiche Fachaktion | Neuer Inbox-Typ | Bestehende Kanäle |
| --- | --- | --- |
| Passwortwechsel im Konto | `PASSWORD_CHANGED` | UI-Rückmeldung und bestehende Sicherheitsmail bleiben erhalten |
| Passwortreset über gültigen Link | `PASSWORD_RESET` | Bestehender Resetablauf bleibt erhalten; keine zusätzliche neue Reset-Bestätigungsmail eingeführt |
| Rollenmenge erfolgreich geändert | `ROLES_CHANGED` | Konto bleibt angemeldet; keine zusätzliche E-Mail |
| Bestätigte E-Mail-Änderung | `EMAIL_CHANGED` | Bestehende Hinweise an alte und neue Adresse bleiben erhalten |

Eine bloß angeforderte E-Mail-Änderung erzeugt noch keinen Erfolgseintrag. Verwendete,
ungültige, abgelaufene oder erfolglose Aktionen erzeugen keine Erfolgsmitteilung.

`UserNotificationService.recordSecurityEvent` verlangt eine bestehende Transaktion. Fachaktion,
Nachricht und Empfangseintrag werden gemeinsam gespeichert. Scheitert die Pflichtmitteilung,
rollt auch die Fachaktion einschließlich ihrer Mail-Outbox-Einträge zurück. Der bisherige
Sitzungswiderruf beim Konto-Passwortwechsel/bei E-Mail-Bestätigung erfolgt weiter nach Commit.
Für den Passwortreset wurde keine zusätzliche Änderung der bisherigen Sitzungsregeln eingeführt.

Systeminhalte speichern nur Typ und Erstellungszeit; Empfänger ist die stabile Benutzer-ID.
Keine Passwörter, Hashes, Tokenlinks, alten/neuen E-Mail-Adressen oder Sitzungs-IDs in den
Mitteilungsinhalten. Vorlagen werden beim Anzeigen über den typisierten DE-/EN-Katalog
übersetzt. Die Benutzer-ID hält den Eintrag auch nach Änderung der Anmeldeadresse beim Konto.

## Zugriff, Datenmodell und Aufbewahrung

`SecurityService.getSessionUser` prüft die konkrete HTTP-Sitzung, Widerruf, aktuellen
Kontostatus und frisch geladene Berechtigungen. Push übernimmt weder Thread-Authentifizierung
noch einen Request-Proxy. Ein eingeschränkter Verwaltungszugang bei erreichtem Sitzungslimit
ist keine vollständige Anmeldung und eröffnet keine Inbox. Wartungsmodus gilt auch hier:
normale Konten pausieren, administrative Rollen bleiben nach bestehenden Regeln zugelassen;
unlesbare Wartungskonfiguration sperrt den Zugriff.

`user_notifications` hält unveränderlichen Inhalt, Typ, Zeit, Vorgangsschlüssel und optionalen
Absender. `user_notification_recipients` hält Benutzerbezug und `read_at`. Benutzerlöschung entfernt
nur seine Empfangseinträge; Senderlöschung setzt den Senderbezug auf NULL. Gemeinsame Inhalte
anderer Empfänger bleiben erhalten. Index auf Empfänger/Lesestatus/ID sowie Zeitindex und
Unique-Constraints werden durch das JPA-Schema angelegt. Empfangslisten lesen skalare
Projektionen; Leseupdates benötigen kein Leeren fremder Änderungen im Persistence Context.

## Getrennte Aufbewahrung ab 19.10.2026

Unter **Administration → Konfiguration → Benachrichtigungen** stehen zwei Felder
für positive Tageswerte oder einen leeren Wert für unbegrenzte Aufbewahrung:

| Einstellung | Datenbankschlüssel | Standard |
| --- | --- | --- |
| Aufbewahrung im Posteingang | `user.notifications.inbox.retention.days` | 180 Tage |
| Aufbewahrung der Versandhistorie | `user.notifications.history.retention.days` | 365 Tage |

Die Einheit und weitere Erläuterungen stehen in den Info-Tooltips. Ein leeres Feld
zeigt den Platzhalter „Unbegrenzt“ und wird ausdrücklich als leer gespeichert. Es bleibt
auch nach erneutem Öffnen und Sprachwechsel leer. Die Voreinstellungen 180/365 gelten
für neue beziehungsweise fehlende Konfigurationseinträge; leere gespeicherte Werte
haben Vorrang. Diese Leerwert-Regel gilt ausschließlich für diese beiden Felder.
Null und negative Zahlen bleiben ungültig; die Zahl `0` bedeutet nicht unbegrenzt.

Beide Werte sind unabhängig. Sie gelten ab Erstellung der Benachrichtigung und werden
bei jedem Abruf neu aus `ConfigService` gelesen. Änderungen wirken ohne Neustart beim
nächsten Abruf beziehungsweise Aktualisieren; der Inbox-Fußtext verwendet denselben Wert.
Abgelaufene Inbox-Einträge fehlen auch in Ungelesen-Zähler und Sammel-Lesesnapshot.
Die Historienfrist betrifft ausschließlich manuell versendete `ADMIN_ANNOUNCEMENT`-Einträge.
Automatische Systemhinweise haben keine gesendete Admin-Historie und folgen nur der Inbox-Frist.

Der tägliche Job um 03:20 in der Server-Zeitzone entfernt Inhalte physisch erst dann,
wenn weder Inbox noch Historie sie noch benötigen. Bei 180/365 Tagen bleiben manuelle
Inhalte zwischen beiden Grenzen ausschließlich für die Historie gespeichert. Wird die
Historie kürzer als die Inbox eingestellt, bleibt der Inhalt bis zum Ende der Inbox-Frist
erhalten. Empfangszuordnungen bleiben bis zur endgültigen Bereinigung als Versandnachweis
für idempotente Wiederholungen bestehen. Verwaiste Systeminhalte werden direkt bereinigt;
verwaiste manuelle Inhalte erst nach Ende der Historienfrist. Gelesen bedeutet keine
nachgewiesene menschliche Kenntnisnahme.

Genau auf der Zeitgrenze sind Einträge noch enthalten (`created_at >= cutoff`);
die physische Bereinigung entfernt strikt ältere Inhalte. Eine Verlängerung der Frist
kann bisher nur ausgeblendete Einträge wieder sichtbar machen; physisch bereinigte
Inhalte bleiben entfernt. Persönlich per Lösch-Icon entfernte Einträge bleiben ausgeblendet.
Bei unbegrenzter Aufbewahrung entfällt die jeweilige Altersgrenze vollständig;
insbesondere darf eine kürzere Frist im anderen Bereich den Inhalt nicht entfernen.
Auch automatische Systemhinweise bleiben bei unbegrenzter Inbox-Frist erhalten.
Der Inbox-Fußtext lautet dann „Benachrichtigungen werden unbegrenzt aufbewahrt.“

Nichtpositive Zahlen oder fehlerhafte Betriebswerte werden zurückgewiesen. Bei fehlerhafter
Konfiguration bricht die Bereinigung ab, statt mit einer verkürzten Ersatzfrist zu löschen.

Standardwerte für neue Datenbanken beziehungsweise noch fehlende Einträge lassen sich
über `APP_CFG_USER_NOTIFICATIONS_INBOX_RETENTION_DAYS` und
`APP_CFG_USER_NOTIFICATIONS_HISTORY_RETENTION_DAYS` setzen; Beispiele stehen in
`.env.example`. Gespeicherte Admin-Konfiguration hat Vorrang. Die neue Gruppe, Defaults
und Shared-Klassen benötigen einmalig einen vollständigen Anwendungsneustart; spätere
Friständerungen im Adminbereich nicht. Die reale `.env` und Nutzerdatenbank wurden nicht
geöffnet oder verändert.

### Prüfung der getrennten Aufbewahrung

Am 19.10.2026 wurde die vollständige Anwendungs-JUnit-Suite in einer isolierten
Buildkopie mit Java 25 ausgeführt: **1.738 Tests erfolgreich, keine Fehler oder Skips**.
Darin enthalten: 50 Benachrichtigungs-Persistenztests, fünf Policytests und sechs
Konfigurationsgruppen-Tests. Geprüft sind die Defaults 180/365, positive Ganzzahlen,
gespeicherte Werte vor Umgebungsdefaults, Änderungen ohne Neustart, die genaue
Zeitgrenze und die getrennte Sichtbarkeit/Bereinigung auch bei kürzerer Historienfrist.
Die vorhandenen Passwort-, Rollen- und Kontoregressionen bleiben grün. Shared wurde
vor dem Anwendungsbuild gebaut; Javaformatierung und Diff-Prüfung sind erfolgreich.
Browser- und Produktionsbuild wurden für diese Änderung nicht ausgeführt.

### Ergänzung: Leere Werte bedeuten unbegrenzt

Nach dieser Anpassung wurde die vollständige Anwendungs-JUnit-Suite erneut in einer
isolierten Kopie geprüft: **1.749 Tests erfolgreich, keine Fehler oder Skips**.
Die zusätzlichen Prüfungen sichern alle vier Kombinationen begrenzter/unbegrenzter
Fristen, alte Systemhinweise, leere gespeicherte Werte vor Defaults, Speichern und
Wiederöffnen leerer Felder sowie den Sprachwechsel und den unbegrenzten Footer ab.
Die übrigen Konfigurationsfelder behalten ihre bisherigen Leerwert-Regeln.
Shared-Build, Spotless und Diff-Prüfung sind erfolgreich. Browser- und Produktionsbuild
wurden auch für diese Anpassung nicht ausgeführt.

## Aufteilung und Einführung

| Bereich | Implementierung |
| --- | --- |
| Datenmodell / JPA | `UserNotificationEntity`, `UserNotificationRecipientEntity`, `UserNotificationType`, zwei Repositories |
| Fachlogik und Zugriff | `UserNotificationService` mit getrennten öffentlichen Operationen für eigene Inbox, geprüften Superadmin-Versand und interne Sicherheitsaktionen; der gemeinsame Guard ist privat |
| Sprache / Push | `UserNotificationTexts`, Shared-`MessageTexts`, `UserNotificationBroadcaster` |
| Oberfläche | `UserNotificationBell`, `UserNotificationsPanel`, `UserNotificationsView`, `AdminUserNotificationsSection`; gemeinsame Header-Anbindung über Spring |
| Aufbewahrung | `UserNotificationPolicy`, `UserNotificationCleanup` |

Für den kleinen gemeinsamen Persistenzablauf ist ein Service umgesetzt, anstelle der im
Plan vorgeschlagenen drei separaten Serviceklassen. Es gibt keine neue Method-Security-
Infrastruktur, keinen externen Broker und keine Kopplung an den SMTP-Workerpool.

Shared neu bauen/installieren, dann Anwendung mit dem aktuellen Stand bauen und vollständig
neu starten. HotSwap allein registriert die neuen Beans, JPA-Tabellen und Route nicht
zuverlässig. Die vorhandene Hibernate-Einstellung `ddl-auto=update` legt die neuen Tabellen
beim regulären Start an. Es wurde keine bestehende Benutzer-Datenbank geöffnet oder verändert.
Neue Tabellen/FKs sind mit frischem H2 getestet; der Start gegen eine künstliche alte
Struktur und ein echter Prozessneustart sind noch separat zu prüfen.

Persistenz überlebt Neustarts durch die Datenbank; unmittelbare Push-Aktualisierungen gelten
zunächst für eine laufende Anwendungsinstanz. Bei mehreren Instanzen, Verbindungsunterbrechung
oder verlorenem Push liefert erneutes Laden den verbindlichen DB-Stand. Keine Zustellgarantie
an geschlossene Browser, keine Desktop-Benachrichtigungen und kein serverseitiger Broadcast
zwischen Instanzen. Abonnements werden beim Detach abgemeldet.

## Nachweise der ersten Funktionsumsetzung vom 18.10.2026

- Java 25.0.4.1, Shared erfolgreich neu installiert, Anwendungs- und Testcode kompiliert.
- Vollständiger Anwendungs-JUnit-Lauf: **1613/1613**, 0 Fehler, 0 Fehlschläge, 0 Skips.
  Gegenüber dem Ausgangsstand 58 zusätzliche Fälle; bestehende Sicherheits- und Routingfälle
  um die Inbox erweitert. Neue reine Mitteilungsklassen: 38 Persistenz- und 15 Komponentenfälle.
- Echte isolierte H2-Transaktionen prüfen Empfängergrenzen, Rollenverlust, deaktivierte/
  abgelaufene Sitzungen, Thread-Kontext-Trennung, Wartung, konkurrierende Wiederholung,
  Vorschaukonflikte, kontoweite Leseänderung, Snapshots, Cacheeffekte, Rollbacks, Fremd-IDs,
  Sender-/Empfängerlöschung, Aufbewahrung und defekte Push-Abonnenten.
- Die vorhandenen Passwort-/E-Mail-Integrationstests prüfen zusätzlich die echten Inbox-
  Einträge und simulierte Pflichtmitteilungsfehler mit Rollback der Fachaktion. UI-Tests
  prüfen Textdarstellung, DE/EN, Pagination, Dialogkonfiguration, Glockenzähler, Detach,
  Vorschauvalidierung und gleiche Vorgangs-ID beim Wiederholen. Header-/Admin-Anbindung
  wird über echte Spring-Autowire-Verarbeitung geprüft; HTTP-Filtertests umfassen `/notifications`.
- JaCoCo: Zeilen **8410/9871 (85,20 %)**, Zweige **2354/3225 (72,99 %)** über 197 ausführbare
  Anwendungs-Java-Klassen. Die neuen Mitteilungsklassen einschließlich ihrer inneren
  Klassen erreichen **540/654 (82,57 %)** Zeilen und **126/187 (67,38 %)** Zweige.
  Messung aus einem frischen vollständigen Lauf, ohne Shared-/Test-/Bibliothekscode.
- Spotless Apply/Check erfolgreich; 482 eindeutige Schlüssel in DE und EN,
  Sprachparität und bestehender Latin-1-Dateicodec erhalten.

Keine Browserausführung, kein Produktionsbuild, keine CSS-/JavaScript-Abdeckungsmessung,
keine Prüfung mit Screenreader oder realem Gerät. Die bestehende Gesamtsuite wurde nicht
als Nachweis der neuen UI-Journeys ausgegeben. Keine realen Postfächer, Zugangsdaten,
Bestandsdatenbanken oder Nutzerprozesse verwendet. JUnit-Fixtures wurden beendet.

### Noch auszuführende Browser-/Betriebsprüfungen

| Geplanter Fall | Sichtbarer Ablauf / Erwartung | Status |
| --- | --- | --- |
| NOTIF-01 | Superadmin sendet an zwei über UI angelegte Konten; beide sehen die Nachricht, ein drittes nicht | Browser DE/EN offen |
| NOTIF-02 | Vorschau und Detail öffnen lassen ungelesen bestehen; ausdrücklich gelesen markieren aktualisiert Zähler und zweiten Tab | Browser DE/EN offen |
| NOTIF-03 | Alle/ungelesen, Pagination und „alle gelesen“ mit währenddessen eintreffender Nachricht | Browser DE/EN offen |
| NOTIF-04 | Vorschau, Feldänderung, Doppelversand, deaktivierter Empfänger und direkte Admin-URL mit normalem Admin | Browser DE/EN offen; technische Regeln JUnit geprüft |
| NOTIF-05 | Passwortwechsel/-reset und E-Mail-Bestätigung über echte UI-/Mailpit-Journeys erzeugen jeweils den eigenen Eintrag | Browser DE/EN offen; Fachtransaktionen JUnit geprüft |
| NOTIF-06 | Sprachwechsel, lange Texte/HTML-Zeichen, schmale Ansichten, Tastaturfokus, Popover und Detaildialog | Browser DE/EN offen; Text-/Komponentenregeln JUnit geprüft |
| NOTIF-07 | Inhalt/Lesestatus nach echtem Prozessneustart; künstliches bisheriges H2-Schema erweitern | Eigene isolierte Betriebsfixture offen |

Das sind geplante Prüfungen, keine bereits implementierten Browserfälle oder Testergebnisse.
Designfeinschliff erfolgt nach der ersten Benutzeransicht im bestehenden Cosmos-Theme.

## Begriffe und Schnittstellen

Das Fachmodell heißt **UserNotification**; die Oberfläche verwendet
„Benachrichtigungen“ / „Notifications“. Manuell vom Superadmin verfasste Mitteilungen
heißen „Benachrichtigung senden“ / „Send notification“; ihr Typ ist `ADMIN_ANNOUNCEMENT`.

| Bereich | Bezeichnung |
| --- | --- |
| Fachmodell und Persistenz | `UserNotificationService`, `UserNotificationEntity`, `UserNotificationRecipientEntity`, `UserNotificationType`, zugehörige Repositories |
| Oberfläche | `UserNotificationBell`, `UserNotificationsPanel`, `UserNotificationsView`, `AdminUserNotificationsSection` |
| Infrastruktur | `UserNotificationBroadcaster`, `UserNotificationCleanup`, `UserNotificationPolicy`, `UserNotificationSchemaUpdater`, `UserNotificationTexts` |
| Kurzlebige UI-Rückmeldung | `NotificationHelper`; keine gespeicherte Inbox |
| Allgemeiner Anwendungstextkatalog | `MessageTexts`, `MessageProperties`, Sprachdateien unter `i18n/`; unabhängig vom Benachrichtigungsmodell |

Die Inbox ist ausschließlich über `/notifications` erreichbar; der Superadminbereich über
`/admin?section=notifications`. Getter und Übersetzungsschlüssel heißen `getNotifications...`
beziehungsweise `notifications.*`; DOM-/CSS-Bezeichner verwenden `notification`/`notifications`.
Die Aufbewahrung verwendet die zwei `ConfigEntry`-Werte der Gruppe Benachrichtigungen;
Defaults sind 180 Tage für die Inbox und 365 Tage für die gesendete Historie.
Es gibt keine weiteren Feature-Routen oder Ersatzkonfigurationsnamen.

Das SQL-Schema besteht aus `user_notifications` und `user_notification_recipients` mit
`notification_id`. Constraints heißen `uk_user_notification_request` und
`uk_user_notification_recipient`, Indizes `idx_user_notification_created` und
`idx_user_notification_inbox`. Der H2-Metadatentest prüft diese tatsächlich erzeugten Namen
sowie gespeicherte Inhalte, Typ, Empfänger und Lesestatus. Weitere Persistenztests decken
Löschkaskaden, Rollback, Cleanup und konkurrierenden Versand ab.

Der Feature-Stand verwendet eine neu angelegte Datenbank. Vor dem Start in IntelliJ
das Projekt vollständig neu erstellen (Rebuild), anschließend die Anwendung starten.
HotSwap ersetzt keinen vollständigen Neuaufbau der Bean-/Entityklassen.

## Prüfung des aktuellen Schnittstellenvertrags

Java 25.0.4.1; Shared neu gebaut/installiert, vollständiger Anwendungs-/Testcompile und
Spotless Apply/Check erfolgreich. **119 gezielte Anwendungs-JUnits und 47 Helfertests**,
jeweils 0 Fehler/Fehlschläge/Skips. Auswahl: Aufbewahrungsbindung, Adminnavigation,
Rollen-UI-Guard, echte HTTP-Sicherheitskette für alle Rollen/Wartungsmodus,
Benachrichtigungs-Persistenz und Java-Komponenten. Der Vertrag verwendet ausschließlich
die hier dokumentierten Namen. Die allgemeine Behandlung ungültiger Routen/Parameter bleibt aktiv.

Geprüft in frischer isolierter Buildkopie mit SHA-256-Abgleich von 352 Java-/Properties-/CSS-/
POM-Dateien zum Arbeitsbaum. Keine reale Datenbank, Zugangsdaten oder Nutzerprozesse verwendet.
Browserquellen kompiliert; kein Browserstart und kein Produktionsbuild. Dies war eine gezielte
Prüfung; keine neue vollständige JUnit- oder Gesamt-Coverage-Messung behauptet.
Die obigen Zahlen der ersten Funktionsumsetzung sind frühere Nachweise.

## Versandpanel

Das erste Adminpanel verwendet den gemeinsamen Cosmos-Panelheader mit Vaadin-Paperplane,
Titel/Untertitel und der Metainformation „INTERN“ mit Betriebsstatuspunkt. Eingaben bleiben native Checkbox,
MultiSelectComboBox, TextField und TextArea. Zeichenanzeigen folgen den Servicegrenzen
120/4000, die TextArea nutzt native Min-/Max-Rows. Die Vorschau zeigt bis zu drei native
Vaadin-Avatare mit Namen; eine native Details-Komponente enthält die vollständige Auswahl.
Inhalt wird als Text ausgegeben. Bearbeitungen verwerfen die Versandfreigabe und markieren die letzte Vorschau als ungeprüft;
die vorhandene Wiederholungserkennung nach unklarer Versandantwort bleibt erhalten.

Eigene Darstellung besteht ausschließlich aus lokalen CSS-Regeln für Infozeile,
Zeichenanzeigen und Vorschau-/Empfängeranordnung. Sie verwenden vorhandene Cosmos-/Lumo-
Tokens. Keine selbst gezeichneten Icons, kein eigenes JavaScript und keine neue globale
Farbpalette. Das zweite Panel bleibt bei der bisherigen Gestaltung.

Gezielte Prüfung: 81 JUnits, 0 Fehler/Fehlschläge/Skips (17 Java-Komponentenfälle einschließlich
zwei neuer DE/EN-Vorschaufälle, 3 Katalogfälle, 22 Adminnavigationsfälle, 39 Persistenzfälle).
Shared-Build, App-/Testcompile und Spotless erfolgreich. Isolierte Buildkopie ohne reale Daten;
kein Browserlauf oder Produktionsbuild, sichtbare Darstellung noch manuell zu prüfen.

### Empfängerauswahl und Pflichtmarkierungen

Die Pflichtpunkte stehen wieder direkt an Titel und Nachricht; die Zähler bleiben rechts
auf derselben Beschriftungszeile. Das Mehrfachauswahl-Overlay nutzt die gleiche
Schriftgestaltung wie die normale ComboBox und den vorhandenen Cosmos-Overlayrahmen.
Bei „Alle aktiven Konten“ wird die Mehrfachauswahl deaktiviert und ausgeblendet.
An ihrer Stelle zeigt ein natives, schreibgeschütztes und deaktiviertes TextField wie
die Konto-E-Mail „Alle aktiven Benutzer (Anzahl)“. Die Anzahl stammt aus den aktuell
freigeschalteten Empfängerkonten. Beim Entfernen des Häkchens bleibt eine vorherige
Einzelauswahl erhalten. Sprachwechsel aktualisiert Beschriftung und Anzahltext.

Gezielter Nachtest: 22 JUnits erfolgreich (19 Komponentenfälle, davon zwei neue DE/EN-
Umschaltfälle, und drei Katalogfälle), 0 Fehler/Fehlschläge/Skips. Shared sowie alle
Java-Anwendungs- und Testquellen in einer frischen isolierten Kopie kompiliert.
Spotless und Diffprüfung erfolgreich. Keine Browserprüfung; die sichtbare Geometrie
und Overlaydarstellung sind damit noch nicht visuell bestätigt.
Die Einzelauswahl zeigt inzwischen ausschließlich die übersetzte Anzahl
„{Anzahl} Benutzer ausgewählt“ statt einzelner Chips; die tatsächlichen Auswahlen bleiben
im nativen Menü markiert. Ein lokaler Span/CSS ergänzt die Anzeige, ohne eigenes JavaScript
oder Änderung der ausgewählten Empfänger. Beim Tippen bleibt der native Suchtext sichtbar.
Die Anzahl aktualisiert sich auch beim Entfernen, Leeren und Sprachwechsel. Texte zu
Empfängerkonten verwenden einheitlich „aktive Benutzer“ statt „freigeschaltete Benutzer“;
fachlich bleiben aktivierte Konten gemeint, unabhängig vom Verfügbarkeitsstatus.
Gezielter Nachtest: 24 JUnits, 0 Fehler/Fehlschläge/Skips (21 Komponentenfälle einschließlich
zwei neuer DE/EN-Zählfälle, drei Katalogfälle). Shared, App- und Testcompile sowie Spotless
und Diffprüfung erfolgreich. Keine Browserprüfung oder Bestätigung der sichtbaren Geometrie.
### Prüfung vor dem Versand (19.10.2026)

„Benachrichtigung senden“ ist initial deaktiviert und wird ausschließlich nach einer
vollständig erfolgreichen Prüfung freigegeben. Titel, Nachricht, Einzelauswahl und
„Alle aktiven Konten“ verwerfen bei Änderung die Freigabe und vorherigen Feldmarkierungen.
Die letzte Vorschau bleibt dabei sichtbar und wird als erneut zu prüfen markiert. Eine neue, fehlgeschlagene Prüfung hebt auch ohne Eingabeänderung
eine zuvor erfolgreiche Prüfung auf. Ein Versandaufruf ohne gültige Vorschau bleibt
wirkungslos; die Serviceprüfungen gelten weiterhin unverändert.

TextField, TextArea und MultiSelectComboBox verwenden manuelle Validierung. Die roten
Markierungen und Fehlermeldungen werden erst beim Prüfen gesetzt, einschließlich leerer
oder inzwischen ungültiger Empfängerauswahl. Automatische Validierungsaufrufe beim
Bearbeiten/Fokuswechsel setzen keine neuen Feldfehler. Bei „Alle“ erscheint ein
Empfängerfehler am deaktivierten Zusammenfassungsfeld. Dessen Einzelauswahl-Zählanzeige
bleibt oberhalb einer gegebenenfalls eingeblendeten Fehlermeldung am Eingabefeld.
Bei einer unklaren Versandantwort bleiben Vorschau und Request-Schlüssel zum sicheren
Wiederholen erhalten; fachliche Ablehnungen verlangen eine neue Prüfung.

Gezielter Nachtest: 35 JUnits, 0 Fehler/Fehlschläge/Skips (32 Komponentenfälle mit elf neuen
Prüf-/Freigabefällen, drei Katalogfälle). Die neuen Fälle decken DE/EN-Fehleranzeige,
ausdrückliche serverseitige Validierungsaufrufe, alle vier Eingabeänderungen,
Längengrenzen und fehlgeschlagene erneute Prüfungen ab. Shared, App-/Testcompile,
Spotless und Diffprüfung erfolgreich. Frische isolierte Kopie ohne reale Daten;
keine Browserprüfung, daher keine Bestätigung tatsächlicher Browser-Fokusabläufe oder
sichtbarer Geometrie. Übernahme in IntelliJ: Strg+F9, Reload, danach Browser Strg+F5.
Feldtexte sind getrennt: Titel verlangt einen Titel mit maximal 120 Zeichen,
Nachricht eine Nachricht mit maximal 4000 Zeichen (DE/EN). Der gemeinsame Entwurfsfehler
bleibt als zusammenfassende Notification erhalten. Fehlertexte aller Eingabefelder im
Versandformular verwenden lokal die vorhandenen Theme-Tokens für 11px und Schriftgewicht
400. Erneuter Nachtest: 35 JUnits erfolgreich, 0 Fehler/Fehlschläge/Skips; vorhandene
DE/EN-Prüfung sichert zusätzlich unterschiedliche und richtig zugeordnete Feldtexte ab.
Shared, App-/Testcompile, Spotless und Diffprüfung erfolgreich; keine Browserprüfung.
### Vorschaukopf und Empfängerdetails

Der Status steht rechts im Vorschaukopf: „Vorschau geprüft“ in Success-Farbe oder
„Vorschau erneut prüfen“ in Warning-Farbe. Nach Eingabeänderungen oder fehlgeschlagener
erneuter Prüfung bleibt der zuletzt geprüfte Inhalt unverändert sichtbar. Die technische
Versandfreigabe wird trotzdem gelöscht; ein deaktivierter Versand lässt sich dadurch
nicht umgehen. Erst eine neue erfolgreiche Prüfung übernimmt den aktuellen Inhalt und
setzt den Status wieder auf geprüft. Nach erfolgreichem Versand und beim Neuöffnen des
Bereichs wird die Vorschau vollständig entfernt; ein Sprachwechsel verwirft sie ebenfalls.

Ein nativer Vaadin-Button „Details anzeigen/ausblenden“ mit Vaadin-Winkelsymbol steht
direkt neben den Empfängerchips. Wie bei den Benutzerfiltern steuert er ein natives
Details mit ausgeblendeter eigener Summary. aria-controls/aria-expanded folgen dem
Zustand. Die Liste öffnet darunter über die ganze Breite, Namen links und E-Mail-Adressen
rechts; schmale Ansichten dürfen umbrechen. Keine eigenen Icons oder JavaScript.

35 JUnits erneut erfolgreich, 0 Fehler/Fehlschläge/Skips. Bestehende DE/EN-Fälle prüfen
Details über den Button, Statusposition und Statuswechsel, unveränderten alten Inhalt
bis zur erneuten Prüfung, die weiterhin gesperrte Versandaktion sowie das anschließende
Aktualisieren und vollständige Entfernen nach Versand. Shared, App-/Testcompile,
Spotless und Diffprüfung erfolgreich. Keine Browserprüfung oder visuelle Bestätigung.
### Interner Versand und Wartungsmodus (19.10.2026)

Das Versandpanel trägt „INTERN“ / „INTERNAL“ mit grünem Punkt bei offenem Betrieb und
rotem Punkt bei Wartung bzw. nicht lesbarer Wartungskonfiguration. Der Status hat eine
sprachabhängige ARIA-Beschriftung. Während der Wartung wird das gesamte erste Panel über
die native Vaadin-Enabled-Vererbung deaktiviert; der Verlauf gesendeter Benachrichtigungen
bleibt für berechtigte Superadmins erreichbar. Entwürfe bleiben erhalten, eine vorherige
Versandfreigabe wird verworfen und muss nach der Wartung neu geprüft werden.

Geöffnete Versandpanels prüfen den Modus mit nativem UI-Polling spätestens im angeforderten
5-Sekunden-Intervall, soweit der Browser Poll-Anfragen ausführt. Ein schon kürzeres Intervall
wird erhalten. Beim Verlassen wird der eigene Listener entfernt und das vorherige Intervall
wiederhergestellt, sofern es nicht zwischenzeitlich anderweitig geändert wurde. Speichern in
der Konfiguration derselben AdminView aktualisiert den Panelstatus zusätzlich direkt.
Die Anzeige ist damit keine allgemeine globale Wartungs-Push-Infrastruktur.

Prüfen und Senden kontrollieren den Modus außerdem bei jeder Aktion frisch, unabhängig vom
Polling. Der Service lehnt auch den direkten manuellen Aufruf während der Wartung ab, bevor
Benachrichtigungen/Empfänger angelegt werden. Die bestehende Superadmin-Berechtigungsprüfung
für Navigation und Verlauf bleibt erhalten. Automatische Sicherheitshinweise und das Lesen
der eigenen Inbox behalten ihre bisherigen Regeln; gesperrt ist hier der manuelle Versand.
Die Konfiguration wird vor der jeweiligen Operation gelesen, ohne globale Serialisierung
zwischen einer bereits laufenden Operation und einem zeitgleichen Konfigurations-Commit.

Vorschau-Nachrichtentext: 12px, Secondary-Farbe; Abstand der inneren Vorschautexte über
var(--lumo-space-s), im bestehenden Theme 8px. Die bisherigen Chips/Buttons/Detailflächen
bleiben in der zuletzt abgestimmten Gestaltung.

Gezielter Nachweis: 105 JUnits erfolgreich, 0 Fehler/Fehlschläge/Skips: 39 Komponentenfälle,
41 Persistenzfälle, 22 Adminnavigationsfälle und drei Katalogfälle. Neu sind sieben Fälle
für Wartung, Reaktivierung, Aktionen vor dem nächsten Poll und Listener-/Intervall-Cleanup
sowie zwei Persistenzfälle für direkten Versand/Prüfung während Wartung und unlesbare
Konfiguration. Alle Java-Anwendungs-/Testquellen und Shared kompiliert, Spotless/Diffprüfung
erfolgreich. Eigene frische H2-/Buildkopie ohne reale Daten; keine Browser-/Produktionsprüfung
und keine neue Gesamt-Coverage-Messung. Shared wurde lokal installiert. Für die neue
Wartungsanbindung wird einmalig ein vollständiger Neustart empfohlen.
### Allgemeiner Hinweis bei ungültiger Vorschau

Die Vorschauvalidierung meldet nun „Vorschau fehlgeschlagen“ und „Bitte überprüfen
Sie Ihre Eingaben. Hinweise finden Sie direkt bei den betroffenen Feldern.“
(DE/EN lokalisiert). Derselbe Hinweis gilt bei mehreren fehlerhaften Feldern,
fehlenden Empfängern und bei der Prüfung inzwischen ungültigen Empfängern.
Feldbezogene Hinweise bleiben erhalten; Wartungs- und technische Fehler behalten
ihre eigenen Meldungen. Die Registrierung wurde nicht verändert.

Gezielter Nachtest: 44 JUnits erfolgreich, keine Fehler oder Skips (41 Komponenten-
und drei Katalogfälle). Shared gebaut; Java- und Testkompilierung sowie Spotless
und Diffprüfung erfolgreich. Keine Browserprüfung.
## Bereinigter Stand vom 20.10.2026

Alle Inbox-Aufrufe verwenden jetzt den typisierten `ReadFilter`; die früheren
booleschen Überladungen hatten keine Produktionsaufrufer mehr und sind entfernt.
Auch die Tests verwenden die aktuellen Schnittstellen. Der alte Pagination-Text
`notifications.page` ist mit seinem Getter entfernt; die native Pagination nutzt
die bestehenden zugänglichen Navigationsbeschriftungen. Die Herkunftsbestimmung
ist für Inbox, Overlay und Detaildialog gemeinsam.

Die bereichsspezifischen Styles stehen zentral in `cosmos-notifications.css`.
Konto-Tabs und Inhaltsrahmen kommen aus den gemeinsamen Komponenten-/Shellregeln;
`UserNotificationsView` benötigt keine lokale Konto-CSS-Datei mehr. Farben,
Abstände und Größen des bestätigten Designs sind im statischen Wertevergleich
erhalten. Technische Tests und Grenzen: [Bereinigungsnachweis](codebereinigung-2026-10-20.md).

## Nächtliche Gesamt-QA vom 19.10.2026

Neue Browserfälle prüfen das überarbeitete Verfassenpanel bei 1280/390 Pixeln,
Feldfehler erst nach ausdrücklicher Prüfung, allgemeinen Vorschauhinweis, native
Einzelauswahl, gesperrte Zusammenfassung aller aktiven Konten, Zeichenstände,
Vorschau-Empfängerdetails, erneute Freigabe nach Änderungen und sichtbaren Tastaturfokus.
Einzel-/Sammelversand und persistenter Lesestatus werden in getrennten eigenen Inboxen
über die tatsächliche Oberfläche geprüft, ohne Header-Overlay oder gesendete Historie.

Genau diese beiden unfertigen Oberflächen bleiben bewusst ausgenommen. Dienste,
Persistenz und Berechtigungen sowie die vollständige persönliche Inbox bleiben im
Umfang. Fünf betroffene JUnit-Ausprägungen sind optional getaggt und nur für diesen
Lauf nicht ausgewählt; keine dauerhafte Deaktivierung. Aktuelle Ergebnisse und
Messgrenzen: [QA-Meilenstein](meilensteine/2026-10-19-benachrichtigungen-rollen-und-gesamtqa.md).
