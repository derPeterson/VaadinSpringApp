# Zwei-Faktor-Authentifizierung – offene Abnahme

## Auftrag und Stand

Branch: `codex/zwei-faktor-authentifizierung`, Basis:
`9be7038f3d0a3bd986d3733777a22d216650fa3d` auf lokalem `main`.
Die acht vereinbarten Pakete sind technisch umgesetzt. Die automatisierten Nachweise
einschließlich der großen Testrunde stehen im [Prüfstand](../status.md).
Die gemeinsame fachliche und gestalterische Abnahme, der Featureabschluss sowie
Merge und Push stehen noch aus.

Das aktuelle Verhalten einschließlich Standardwerten und Grenzen steht in der
[Fachdokumentation](../zwei-faktor-authentifizierung.md). Dort sind TOTP,
Einrichtung, einmalige Ersatzcodes/PDF, Rollen-/Funktionsregeln, Sitzungsfristen,
Entwicklungsoptionen und Wiederherstellungsanfragen beschrieben. Der manuelle
[Betreiber-Notfallweg](../betrieb/2fa-notfall.md) bleibt von Benutzerhilfe getrennt.
Die Google-/Microsoft-Verwaltung ist im selben Paket auf Superadmins beschränkt.

## Noch offen

1. Einrichtung, Bestätigung und Verwaltung im bestehenden Cosmos-Stil gemeinsam
   durchsehen: Mein Konto → Sicherheit → Portalzugang, eingeschränkte Anmeldung,
   Ersatzcodeausgabe, Hilfeanfrage und deren Adminbearbeitung. DE/EN und
   Desktop/Smartphone berücksichtigen. Eine spätere Stitch-Verfeinerung ist
   noch keine Freigabe für eine neue Gestaltung.
2. Die Superadmin-Konfiguration einschließlich Feldhilfen, Zeiteinheiten und
   Rollenregeln sowie den Ausschluss normaler Admins aus beiden geschützten
   Konfigurationsgruppen gemeinsam abnehmen.
3. [HUM-09](../manual-tests/README.md#hum-09) mit echter frei gewählter
   Authenticator-App durchführen. QR/manuelle Einrichtung, TOTP, Ersatzcode,
   PDF-Download/Druck, verständliche Texte und Tastatur-/Mobilbedienung beurteilen.
4. Nach ausdrücklicher Abnahme aktuelle Fachdokumentation und Aufgabenlisten
   abschließend bereinigen und diesen erledigten Plan entfernen. Git-Schritte
   nur auf entsprechenden Auftrag ausführen.

## Administrative 2FA-Konfiguration und Ticketbearbeitung

Der Vorgangsfilter ist im Stil der Benutzerverwaltung ergänzt: Statusmehrfachwahl,
Zuständigkeit, „Alle / Ungelesen“, aktiver Gruppenzähler und direkter Ticketsprung.
Für die Zuständigkeitsauswahl und eine zukünftige Seitennavigation der Vorgangsliste
liegt ein [Stitch-Vorschlag zur gemeinsamen Sichtung](https://stitch.google.com/projects/1332538028618138858?node-id=11681483480554492553)
vor: vertikale Zuständigkeitsoptionen und ein kompakter Listenfooter mit Bereich,
Einträgen pro Seite und zwei Pfeilbuttons. Seine zusätzlichen Kartenrahmen und
abweichenden Header-/Tabgestaltungen sind keine Übernahmevorgabe. Freigegeben sind
die technische Seitennavigation im Listenfooter (10, 20 oder 50 Einträge, nur vorherige
und nächste Seite) sowie die datenbankseitige Filterung über alle berechtigten Vorgänge.
Das übrige Design bleibt bestehen. Die gemeinsame Sichtabnahme steht aus.

Die große Testrunde, die Zwischenintegration nach `main` und die anschließende
Sonar-Bereinigung sind zurückgestellt. Die Arbeit geht gemeinsam auf dem
bestehenden Feature-Branch weiter. Die fachliche und gestalterische Abnahme
bleibt offen.

Für die laufende Umsetzung sind zusätzlich die abgestimmten status- und
zuständigkeitsabhängigen Zähler, getrennte Relevanz-/Lesestände der
Glockenhinweise, Hinweise beim Nutzerabschluss und die Quittierung bereits
sichtbarer Nachrichten freigegeben. Die Regeln stehen gesammelt unter
[Ticketzustände, Zähler und Glocke](../zwei-faktor-authentifizierung.md#ticketzustände-zähler-und-glocke).
Offen bleibt die gemeinsame Sichtprüfung der Regeln; eine Übergabe an andere
Bearbeiter ist nicht beauftragt. Weiterer automatisierter Prüfbedarf steht
zentral im [Testbedarf](../testbedarf.md).

Für die 2FA-Konfiguration sind Variante A und die Erklärspalte aus Variante B
ausgewählt. Die administrative Ticketansicht wurde anhand separater
Vaadin-Aufnahmen mit synthetischen Anfragen weiterentwickelt.

Für den Inhalt offener Tickets liegt ein
[Stitch-Vorschlag mit geteilter Ansicht und Fokusansicht](https://stitch.google.com/projects/1332538028618138858?node-id=17845443557095059631)
als freigegebene Grundlage vor: mittlere Trennlinie entfernen, vorhandenen Hinweis
als Infobox mit weißer, fetter Einleitung gliedern, Benachrichtigungsschalter im
gemeinsamen Stil in die Werkzeugleiste setzen und „Nachrichtenverlauf“ blau hervorheben.
Ein nativer Iconbutton blendet die Vorgangsliste aus und stellt die geteilte Ansicht
wieder her, auch mobil. Die Symbole sind Lucide `expand` und `minimize`.
Der Ticketkopf und die bestehenden Sprechblasen bleiben verbindlich; ihre
Abweichungen im Entwurf werden nicht übernommen. Die gemeinsame Sichtprüfung
der Übernahme steht noch aus.

2FA-Konfiguration:

- [A: nach Funktionen gegliedertes Formular](https://stitch.withgoogle.com/projects/1332538028618138858?node-id=17680211258750069028)
- [B: Regelbereiche mit zusätzlicher Erklärspalte](https://stitch.withgoogle.com/projects/1332538028618138858?node-id=2958079107723707652)
- [C: tabellarische Regelübersicht](https://stitch.withgoogle.com/projects/1332538028618138858?node-id=12170611722839130185)

Die vier Rollen, neun dreiwertigen Aktionsregeln samt Feldhilfe und der globale
Inaktivitätsschalter mit Dauer bleiben fachlich unverändert. Eine globale
„Sitzungssperre“ ist keine passende Bezeichnung für die erneute Faktorbestätigung.
Die in C zusätzlich gezeigten Bereichsbezeichnungen sind reine Gliederungsvorschläge,
keine neuen Konfigurationswerte. Entwickleroptionen bleiben im Normalbetrieb verborgen.
Vaadin kombiniert die drei Regelgruppen aus A mit
der Erklärspalte aus B. Der Seitenkopf bleibt bestehen und die Rollentabs nutzen
die Gestaltung der Anmeldeanbieter. Die gemeinsame Sichtabnahme steht aus.

Administrative Tickets:

Der freigegebene [Stitch-Vorschlag zu Vermerken, Editorumschaltung und Status-History](https://stitch.google.com/projects/1332538028618138858?node-id=16005963741703421288)
ist mit nativen Tabs direkt am Editor, violett gekennzeichneten internen
Vermerken und kompakten administrativen Statusereignissen im Verlauf umgesetzt.
Die fachlichen Regeln stehen in der [2FA-Dokumentation](../zwei-faktor-authentifizierung.md#ticketzustände-zähler-und-glocke).
Header und Footer bleiben bestehen. Die gemeinsame Sichtabnahme steht aus.
Die zusätzliche [Stitch-Kritik zu Scrollbereich und Vermerkfarbe](https://stitch.google.com/projects/1332538028618138858?node-id=1618580295561432737)
ist übernommen: schmale dezente Scrollbahn, Abstand zur Bahn und wärmeres
Violett für interne Vermerke, weiterhin mit Schloss-Badge und ausdrücklicher Kennzeichnung.
Die Palette ist zugleich für vorhandene System-Badges zentral festgelegt und setzt
sich deutlicher von öffentlichen blaugrauen Adminantworten ab.
Zusätzliche Navigation, Footer, Sicherheitsbehauptungen, andere Icons und abweichende
Sprechblasenformen aus dem Entwurf sind keine freigegebenen Vorgaben.
Die gemeinsame Sichtabnahme bleibt offen.
Für die 2FA-Konfiguration liegt ein [Stitch-Feinschliff zur Sichtung](https://stitch.google.com/projects/1332538028618138858?node-id=11013469201324753713)
vor. Er empfiehlt gemeinsame Label-/Feldfluchten, mittige Info-Icons, eine oben
bündige Erklärspalte und eine ruhig ausgerichtete Inaktivitätszeile. Aufbau,
Rollentabs und Fachfunktionen sollen erhalten bleiben. Außer dem bereits angeglichenen
`shield-keyhole`-Icon ist daraus noch keine Formularänderung übernommen. Die
Entwurfsschrift, Tabdetails und Navigationsabweichungen sind keine neue Themevorgabe;
größere Änderungen benötigen die gemeinsame Sichtung.

- [A: Durchgehende Ticketliste mit großem Arbeitsbereich](https://stitch.withgoogle.com/projects/1332538028618138858?node-id=d4098cd0685d4835b377011c531f1db7)
- [B: Kompakte Ticketliste neben einer ruhigen Detailfläche](https://stitch.withgoogle.com/projects/1332538028618138858?node-id=952fff07c2a74590ad35ae098434dc51)
- [C: Tabelle und Detailfläche untereinander](https://stitch.withgoogle.com/projects/1332538028618138858?node-id=74f2a6d1e3e944f892d88fe989bc52ab)

Die Entwürfe sind Gestaltungsideen, keine fachlichen Vorlagen: Trotz ausdrücklichem
Ausschluss hat Stitch zusätzliche SLA-/Online-/Versionsangaben, Filter, Prioritäten,
Auditinformationen und teilweise Aussagen über telefonischen Kontakt oder
Personalnummernprüfung erfunden. Auch die Aufforderung, Ersatzcodes vorzulegen,
ist falsch. Diese Inhalte und Funktionen gehören nicht zur Anwendung und dürfen
nicht übernommen werden. Eine Anfrage entsperrt kein Konto; Passwörter,
Authenticator-Schlüssel und Ersatzcodes gehören nicht in den Schriftverkehr.
Vaadin übernimmt B als Grundlage mit dem klar gegliederten Ticketkopf aus
[der ersten Verfeinerung](https://stitch.withgoogle.com/projects/1332538028618138858?node-id=e680bc51c74644e384f116243d18b6dc).
Aus [der mobilen Runde](https://stitch.withgoogle.com/projects/1332538028618138858?node-id=766275d05e004d3997f72fa43e0daee3)
bleiben die gestapelte Ansicht und die breite Antwortaktion. Die Ticketliste
scrollt auch dort vertikal. [Die dritte Runde](https://stitch.withgoogle.com/projects/1332538028618138858?node-id=f1553756865643e598532ed5ab488927)
bestätigt die Trennung von Nebenaktionen und Hauptaktion. Der Verlauf und das
Antwortfeld haben getrennte Flächen, damit der Text beim Scrollen lesbar bleibt.
Die Adminnavigation bündelt den Einstieg unter „Tickets“. Die Kategorien „Alle
Tickets“ und „Wiederherstellung“ verwenden die vorhandene Ticketart. Die
Anfragenliste mit Status und Bearbeiter, der Verlauf, interne Vermerke, Übernehmen,
Antwort, Anleitung mit Vorschau, Abschließen und Aktualisieren bleiben erhalten.

Für beide Bereiche gelten weiterhin die bestehenden
Cosmos-Maße, Farben, Kopficons, nativen Vaadin-Komponenten und Footerregeln.
Geänderte Navigation, zusätzliche Statusboxen, kräftige Warnkästen, fremde Schriften
und umgebrochene oder übergroße Buttons sind nicht automatisch freigegeben.
Die gemeinsame Desktop-/Smartphone-Abnahme bleibt offen.

## Wiederkehrende Anmeldung und Wiederherstellung: Variante A zur Abnahme

Für den mobilen Benachrichtigungsdialog liegt ein
[Stitch-Vorschlag zur Aktionsanordnung](https://stitch.withgoogle.com/projects/1332538028618138858?node-id=4def0463625e42369d398c2a1db4691e)
als Gestaltungsgrundlage vor. Die freigegebene Übernahme setzt die Leseaktion an
die Nachricht, „Weitere Aktionen“ mit beschrifteten Buttons im nativen Popover,
neutrales „Schließen“ und den direkten Ticketzugang um. Mobil liegt die Zielaktion
in einer eigenen vollen Zeile. Die langen und verkürzten Beschriftungen des
Entwurfs werden durch „Zum Ticket“ beziehungsweise die vollständigen vorhandenen
Aktionstexte ersetzt. Bei Ticketmeldungen entfällt der Kontosicherheitsbutton.
Die [Desktop-Ergänzung](https://stitch.withgoogle.com/projects/1332538028618138858?node-id=4697a53cb3e8412a989832f7310738db)
zeigt die Leseaktion innerhalb der Karte und eine beschriftete Footerzeile mit
„Alle Benachrichtigungen“, „Löschen“, neutralem „Schließen“ und blauem „Zum Ticket“.
Die vier sichtbaren Footerbuttons wurden zugunsten der beschriebenen Menüanordnung
zusammengefasst. Die gemeinsame Sichtabnahme der Vaadin-Übernahme ist offen.

Beauftragt sind drei zusammenhängende Stitch-Varianten, jeweils als Übersicht mit
diesen drei Ansichten: Anmeldung bestätigen, Authenticator mit Ersatzcode
wiederherstellen und Wiederherstellungsanfragen ohne App/Ersatzcodes. Die Entwürfe
bleiben im bestehenden 2FA-Projekt. Ausgewählt und zur Übernahme freigegeben ist A.
Die drei Ansichten sind mit den bestehenden Cosmos-Bausteinen in Vaadin umgesetzt.
Die gemeinsame Sichtabnahme und spätere Verfeinerung
bleiben offen.

Der bestehende Cosmos-Seitenrahmen, Panelkopf, Inhaltsfläche, Footer, Inter,
Feld-/Buttonstandards und die vereinbarten Farben bleiben Grundlage. Die Anmeldung
soll zwischen dem vorhandenen sechsstelligen OTP-Add-on und einem Ersatzcodefeld
umschalten können. Seltene Wiederherstellungs- und Hilfeaktionen werden ruhiger
angeordnet; doppelte Erklärungen entfallen. Der Hilfeablauf darf im Inhalt stärker
überarbeitet werden: verständlicher Leerzustand, bestehende eigene Anfragen und
Konversation, Anfragefeld sowie kompakte Footeraktionen mit der Hauptaktion rechts.

Die unterschiedlichen Zwecke bleiben sichtbar: Ein Ersatzcode kann die Anmeldung
bestätigen oder gezielt einen geschützten Authenticatorwechsel beginnen. Beim
Wechsel bleibt der alte Faktor bis zur Bestätigung der neuen App gültig; der
Ersatzcode wird erst beim erfolgreichen Wechsel verbraucht. Eine Hilfeanfrage
ersetzt keinen unabhängigen Nachweis und entsperrt das Konto nicht automatisch.

Die drei über die CLI erzeugten, exportierten und visuell gesichteten Übersichten
liegen auf der Projektfläche. Jede zeigt alternative Wege, keine drei Pflichtschritte:

- [A: kompakte Anmeldung mit offenen Hilfeaktionen](https://stitch.withgoogle.com/projects/1332538028618138858?node-id=1024361580137853276)
- [B: eigener Bereich für die Anmeldehilfe](https://stitch.withgoogle.com/projects/1332538028618138858?node-id=17144405251035875117)
- [C: einklappbare Hilfe und Anfrageübersicht](https://stitch.withgoogle.com/projects/1332538028618138858?node-id=15330646331966040633)

A bildet die Grundlage. Die exportierten Stitch-Entwürfe sind keine exakten Themevorlagen:
abweichende Navigation, fehlende beziehungsweise anders platzierte Kopficons,
zusätzliche Formatangaben an Ersatzcodefeldern, gelbe Hinweisicons, kräftig gefüllte
Tabs und umgebrochene Footerbuttons werden nicht automatisch übernommen.
Die Übernahme verwendet die vorhandenen Cosmos-Bausteine,
nativen Vaadin-Komponenten und Texte statt dieser Abweichungen. Bestehende Anfragen
und deren Konversation müssen weiterhin erreichbar bleiben; die Bilder zeigen nur
den Leerzustand beziehungsweise eine eingeklappte Übersicht.

Die Anmeldung verwendet die vorhandene native OTP-Eingabe und ein getrenntes
Ersatzcodefeld, umschaltbar mit nativen Tabs. Der Wiederherstellungsweg bleibt von
der Sitzungsbestätigung getrennt; Rückweg und Hauptaktionen stehen im Panel-Footer.
Die Hilfe behält eigene Anfragen und Konversationen sowie die serverseitigen
Zugriffsgrenzen, zeigt bei leerer Liste einen Leerzustand und verwendet native
Footerbuttons.
Der gezielte Bediennachweis wird zentral im [Testbedarf](../testbedarf.md) geführt.

Für den Wiederherstellungsdialog liegt eine [Stitch-Verfeinerung zur Sichtung](https://stitch.withgoogle.com/projects/1332538028618138858?node-id=12469882513791176942)
vor. Offen ist, ob der Rückweg eindeutiger beschriftet und ein kurzer Formathinweis
am Ersatzcodefeld ergänzt werden soll. Der Entwurf verändert den Portalrahmen und
zeigt einen Platzhalter mit uneinheitlichen Gruppenlängen. Maßgeblich bleiben die
bestehenden Cosmos-Bausteine und das Format mit vier Gruppen zu je acht Zeichen.

Für „Hilfe zur Wiederherstellung“ steht die gemeinsame Sichtprüfung der
Übernahme nach [Stitch-Runde 3](https://stitch.withgoogle.com/projects/1332538028618138858?node-id=10858692231921999861)
aus. Zu prüfen sind das kompakte Anfrage-Grid mit zugeordnetem Verlauf und der
zweizeilige mobile Footer mit „Zurück“ links und der Hauptaktion darunter.

Variante A ist für die Aktionsanordnung übernommen. „Aktualisieren“ steht am
Verlauf oder bei der leeren Anfragenübersicht. Die Vaadin-Ansicht verwendet
primärblaue Sprechblasen für den Antragsteller und dunklere Sprechblasen für die
Systemadministration, jeweils mit Spitze zur Absenderseite, sowie
eine schmale Scrollbar. Die gemeinsame Sichtprüfung bleibt offen. Die
Stitch-Entwürfe zeigen den Desktopstand mit synthetischen Nachrichten.

- [A: Aktualisieren am Verlauf](https://stitch.withgoogle.com/projects/1332538028618138858?node-id=110782652643208336)
- [B: Kompakter Footer mit grünem Abschluss](https://stitch.withgoogle.com/projects/1332538028618138858?node-id=11932244678383587741)
- [C: Menü für Nebenaktionen](https://stitch.withgoogle.com/projects/1332538028618138858?node-id=8154288544097916227)

Bei der gemeinsamen Abnahme die Live-Aktualisierung, rote Ungelesen-Zähler und
ticketbezogene Abos mit getrennten Nutzer-, Admin- und Superadmin-Sitzungen prüfen.
Anschließend die Standardnachricht sichten und entscheiden, ob ein Direktlink
oder eine Textvorschau im Postfach hilfreich ist.

## Gestaltung der verpflichtenden Ersteinrichtung

### Ersatzcodeausgabe: Variante B mit kompakter Codeübersicht

Die [aktuelle Stitch-Verfeinerung](https://stitch.withgoogle.com/projects/1332538028618138858?node-id=11513303871659797662)
zeigt nach erneuter Kritik-Anfrage den Sicherungs-Switch rechts und die Beschriftung
links sowie einen stärker gerahmten Tresorbereich. Die Darstellung wurde über die CLI
mit einem aktuellen Screenshot der synthetischen HTML-Referenz erzeugt, exportiert
und gesichtet. Ein getrennter Kritiktext war im CLI-Ergebnis nicht enthalten.
Übernommen ist ausschließlich die Switch-Position rechts mit linksbündigem Text.
Der zusätzliche Tresorrahmen und die übrigen Änderungen sind nicht gewünscht.

Die folgenden drei Stitch-Exporte liegen im bestehenden 2FA-Projekt; sie verwenden
ausschließlich synthetische Codes. Ausgewählt ist B mit einklappbarer Codeübersicht aus C:

- [A: zentrale Sicherheitskarte](https://stitch.withgoogle.com/projects/1332538028618138858?node-id=12100564379038591947)
- [B: zweispaltiges Panel mit Tresorillustration](https://stitch.withgoogle.com/projects/1332538028618138858?node-id=16507521264873901502)
- [C: Sicherungsaktionen und einklappbare Codeübersicht](https://stitch.withgoogle.com/projects/1332538028618138858?node-id=18067519117044536055)

Die Umsetzung verwendet native Vaadin-Layouts, `Details`, den vorhandenen `Switch`
und den geschützten Download. Links stehen die Tresorillustration mit blauem Glow,
drei kurze Hinweise und darunter die vollbreiten Sicherungsaktionen mit Trennlinie;
rechts zehn native schreibgeschützte Textfelder mit Kopieraktion, einklappbar über
einen rahmenlosen Details-Button, und die Bestätigung ohne zusätzlichen Helpertext. Schmale
Ansichten ordnen die Bereiche untereinander an. Die gemeinsame Designabnahme steht aus.
Zusätzliche Statusanzeigen, veränderte Codestruktur, technische
Download-Erklärungen und falsche Behauptungen über einen unvollständigen Login
sind keine Anforderungen. Die Codebestätigung soll ein bewusster Switch statt
eines zusätzlichen Bestätigungsdialogs sein. Sie beweist keine tatsächliche Ablage.
Kopieren und PDF bleiben bis zur Bestätigung oder zum Ende der berechtigten Sitzung
verfügbar. Die Fünf-Minuten-Frist gilt nur für Sicherheitsbestätigungen; deren Nachweise
bleiben unverändert. Der PDF-Fehler ist unabhängig vom Entwurf behoben;
der gezielte Ersatzcode-/PDF-Nachtest auf Desktop/Smartphone in DE/EN ist bestanden.

Die reine Textkritik zum fertigen QR-/Manuell-Einrichtungsschritt wurde nach
ausdrücklicher Freigabe im Stitch-Browserchat abgefragt und im Chat wiedergegeben.
Stitch lobt Aufbau, Aktionsgewichtung und mobile Trennlinie; es empfiehlt mehr
Abstand zwischen den Schritten, lesbare Hinweise und einen klaren Codefokus.
Das kritisierte Vorschau-Banner gehört nicht zur echten Oberfläche; die
Kontrastaussage war keine Messung.

Auf weiteren Auftrag liegt eine einzelne
[Überarbeitung nach der Kritik](https://stitch.withgoogle.com/projects/1332538028618138858?node-id=2934366688844334879)
im Projekt. Sie wurde über die CLI erzeugt, exportiert und gesichtet. Die
ausgewählten Elemente sind in Vaadin übernommen:
Trennlinie zwischen den Schritten, Punkt-Platzhalter mit wandernder Codemarkierung,
Tabicons und überarbeitete Hinweise. Die Gestaltung dieses Einrichtungsschritts ist
gemeinsam abgenommen. Zusätzliche Codegruppierung, Footeränderungen und erfundene
Statusanzeigen aus Stitch bleiben ausgeschlossen. Der automatisierte Bediennachweis
für DE/EN steht im [Prüfstand](../status.md); die menschlichen Sonderprüfungen
bleiben in [HUM-09](../manual-tests/README.md#hum-09).

### QR-Einrichtungsschritt: Variante B mit Ergänzungen

Drei gesichtete Stitch-Entwürfe für „Authenticator-App einrichten“ liegen im
selben Projekt. Panelkopf und Aktionsfooter bleiben bei einer Übernahme Grundlage;
„Abmelden“ verwendet die bestehende rote Aktionsvariante. Der Inhalt darf neu
angeordnet werden. Ausgewählt ist B mit Umschaltung QR/manueller Schlüssel aus C,
den drei App-Empfehlungen und den blauen Tabler-Schrittüberschriften wie in der
Anbietereinrichtung. Die erste Codeeingabe verwendet das freigegebene OTP-Add-on
mit sechs sichtbaren Kästchen und gemeinsamem Einfügen. Der Content verwendet
die Standardfarbe. Die gemeinsame Sichtabnahme
dieses Screens ist erfolgt. Die automatisierten Einrichtungs- und
Wiederherstellungsabläufe sind geprüft; der manuelle Durchlauf mit echter App
bleibt Teil von [HUM-09](../manual-tests/README.md#hum-09).

- [A: Schritte untereinander](https://stitch.withgoogle.com/projects/1332538028618138858?node-id=7593142201349970462)
- [B: QR und Code nebeneinander](https://stitch.withgoogle.com/projects/1332538028618138858?node-id=271433440045913590)
- [C: QR/manueller Schlüssel mit Codefeldern](https://stitch.withgoogle.com/projects/1332538028618138858?node-id=15548966782208944965)

Die Entwürfe enthalten ausschließlich synthetische QR-/Schlüsselbeispiele.
Zusätzliche von Stitch erfundene Seitenstatus und technische Erklärungen sind
keine abgenommenen Anforderungen. Die Windows-App 2fast ist bereits als weitere
frei wählbare App-Empfehlung in Vaadin ergänzt.

### Bereits ausgewählter Einstieg

Im eigenen Stitch-Projekt
[VaadinSpringApp – Zwei-Faktor-Authentifizierung](https://stitch.withgoogle.com/projects/1332538028618138858)
liegen die drei gesichteten Entwürfe:

- [A: Zentrierte Schlosskarte](https://stitch.withgoogle.com/projects/1332538028618138858?node-id=0767d365b62e4df296209ad37bc924bd)
- [B: Geteiltes Panel mit Schildmotiv](https://stitch.withgoogle.com/projects/1332538028618138858?node-id=c3bcc6e662e34ccba6f0abca4a51b89f)
- [C: Sachliches Panel mit Kosmos-Motiv](https://stitch.withgoogle.com/projects/1332538028618138858?node-id=c0fce938cba44dfb9fee508cb71f340b)

Variante A ist ausgewählt und in Vaadin übernommen.
Die gemeinsame Sichtprüfung der Übernahme steht aus. Zusätzliche von Stitch
erfundene Statusangaben und Schrittzahlen werden nicht übernommen.

Vereinbarter Einstieg nach einer frischen Portalpasswort-Anmeldung: Überschrift
„Zwei-Faktor-Authentifizierung erforderlich“, Erklärung „Für Ihr Konto ist eine
zusätzliche Absicherung erforderlich. Verbinden Sie jetzt eine Authenticator-App,
um die Anwendung weiter zu nutzen.“ sowie „Abmelden“ und „Einrichten“.
„Abmelden“ beendet die Anmeldung; Schließen oder Überspringen darf die Pflicht
nicht umgehen. QR-Code, Codeeingabe und Ersatzcodes folgen erst danach.

Die verpflichtende Ersteinrichtung verwendet den echten Passwortnachweis derselben
Sitzung, solange er weniger als fünf Minuten alt ist. Die Prüfung erfolgt auch beim
Start der Einrichtung. Alte Sitzungen, dauerhafte Anmeldung und externe Nachweise
gelten nicht als frischer Passwortnachweis. Ohne passenden Nachweis folgt die
bestehende Identitätsprüfung. Der erste gültige TOTP bleibt Voraussetzung für den
Abschluss. Frühere Faktoren und Betreiber-Wiederherstellungen nutzen diese Ausnahme
nicht.

## Geltende Umfangsgrenzen

- Ein neuer Google-Durchlauf allein bestätigt im normalen Betrieb keine sensible
  Kontoaktion. Ausschließlich bei echter erstmaliger Einrichtung gilt die
  vereinbarte Kombination aus kontogebundenem Google-Durchlauf, Code an die bereits
  bestätigte Kontaktadresse und erstem TOTP. Das akzeptierte Risiko einer gemeinsam
  übernommenen Google-/Mailsitzung ist dokumentiert. Der Weg ersetzt keinen
  vorhandenen, entfernten oder verlorenen Faktor.
- Keine fest eingebaute unveränderliche Adminpflicht. Die strengste tatsächlich
  konfigurierte explizite Rolle gilt. Änderungen werden nach der vorherigen Regel
  bestätigt und wirken auch auf offene bzw. dauerhaft angemeldete Sitzungen.
- Entwicklungs-Ausnahmen sind standardmäßig aus und ohne ausdrückliche
  Startfreigabe unwirksam. Die Startfreigabe allein schwächt keine Prüfung ab.
- Die kleine Ticketgrundlage ist Kontakt und Unterstützung. Weder Adminantwort,
  Anleitung noch Abschluss erzeugt einen Kontonachweis oder Faktorreset.
  Ohne Authenticator/Ersatzcode keine automatische Freischaltung.
- Kein vollständiges Ticketsystem, keine Feedback-Funktion, kein versteckter
  Notfallbenutzer und keine zweite Reparaturoberfläche. Die separate
  Vaadin-Komponentenübernahme gehört nicht zu dieser Umsetzung.
