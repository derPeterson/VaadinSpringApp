# Zusätzliche Administrationsentwürfe

Die [bedienbare Sichtung](../../admin-entwuerfe.html) ergänzt die abgenommene
[Adminreferenz](../../admin.html). Dashboard A, Benutzerverwaltung,
Konfiguration mit allen acht Gruppen und abgestimmter Hilfeseitenleiste, Mailtest A
und Benachrichtigungen B sind gemeinsam in die feste Adminreferenz übernommen.
Die Seitennavigation, Bereichsauswahl und Dashboard-Schnellzugriffe öffnen alle
Bereiche innerhalb derselben Oberfläche. Es erfolgte keine Vaadin-Übernahme.
Die vorhandene Benutzerverwaltung ist innerhalb der Sichtung ebenfalls erreichbar.

Für die Konfiguration ist Variante A ausgewählt. Ihre acht Unterbereiche verwenden
die Originaltexte der Anwendung, ein blaues Einstellungsbadge und den vollständigen
Speicherhinweis mit Infoicon in einer eigenen Zeile oberhalb der Aktionen wie bei
Geräte/Sitzungen. Die Layoutauswahl bleibt für die
Konfiguration auf A; die übrigen neuen Bereiche stehen weiter zur Auswahl.

## Freigegebene Konfiguration und Feldhilfe

Die freigegebene Feldhilfe C (zusätzlich als [Komponentenansicht](../../konfiguration-seitenleiste.html) verfügbar)
greift ausdrücklich den vorhandenen Filterdrawer auf: gemeinsame Kopf-/Footerflächen,
Iconrahmen, linker Rand, Schließen-Icon und tatsächliche sichtbare Footergrenze.
Auf allen Bildschirmgrößen liegt sie als modales Overlay über der Oberfläche;
das Formular reserviert keinen Platz und seine Felder verschieben sich nicht.
Die Scrollbarfläche bleibt beim Öffnen reserviert. Alle 30
Originalhilfen der acht Gruppen sind über Infoicons erreichbar.
Bei Checkboxen stehen Bezeichnung links und Checkbox rechts zusammen im Kasten; das Infoicon
liegt wie bei Textfeldern rechts außerhalb in derselben zusätzlichen Spalte.
Die Kästchen verwenden den bestehenden `au-checkbox`-Baustein der Benutzerverwaltung
mit dunkler Leerfläche, abgerundeten Ecken und blauem Hakenstatus.
Bloßer Eingabefokus öffnet oder wechselt keine Hilfe. X, Escape, Schließen sowie Außenklick
schließen die Leiste; Eingaben bleiben erhalten. Fokus kehrt beim expliziten
Schließen zum Infoicon zurück. Ein Gruppenwechsel schließt die aktuelle Hilfe.
Der Inhalt gliedert den vollständigen Originalhinweis, einen dezenten Standardwert
und Eingaberegeln mit kompakten blauen Abschnittstiteln. Beim Copyrightfeld kommen
die tatsächlich unterstützten Platzhalter `{year}` und `{serviceName}` als kleine
Codechips mit Bedeutung hinzu (`CopyrightTextFormatter`); andere Felder erhalten
keine erfundenen Platzhalter. Es gibt keine zusätzliche Livevorschau oder Validierungsanzeige.
`hilfe-seitenleiste.js` und `.css` werden in der festen Adminreferenz, der
Entwurfsnavigation und der separaten Komponentenansicht gemeinsam eingebettet.
Die ursprünglichen Stitch-Boards und älteren Nachbauten bleiben separat erhalten.

Pflichtpunkte und HTML-`required` folgen den typisierten Eingaberegeln aus
`AdminConfigurationSection`: 13 Felder benötigen eine Eingabe, darunter Secret Key
und SSL-Vertrauen. Neun Eingaben dürfen leer bleiben (Standardwert oder
unbegrenzte Aufbewahrung), ebenso die ausdrücklich optionalen Texte. Checkboxen
tragen keinen Pflichtpunkt. Das Inventar übernimmt dafür `required` aus den
Regel-Gettern statt aus übersetzten Texten oder einer separaten Ausnahmeliste.
Zahlenfelder verwenden kleine blaue Pfeilbuttons innerhalb des Eingabefelds;
native graue Spinbuttons sind ausgeblendet. Die Buttons nutzen den nativen
Ganzzahlschritt und die vorhandenen Min-/Maxgrenzen; freie Eingabe und Pfeiltasten
bleiben erhalten. Das ist eine lokale HTML-Darstellung, kein Vaadin-Umbau.

`duration-controls.js` ist der gemeinsame HTML-Baustein für alle neun Zeitfelder.
Er erhält das Speicherformat aus dem Inventar: Sekunden, ISO-Zeitspanne oder ganze
Tage. Die Oberfläche bietet Zahl und passende Einheit, die Feldhilfe zeigt aktuelle
Dauer und Umrechnung. Token-Mindestdauern bleiben erhalten. Die beiden
Benachrichtigungs-Aufbewahrungen behalten die feste Einheit Tage und leer bedeutet
unbegrenzt. Der HTML-Baustein rechnet mit Festkommazahlen und zeigt Validierung
nur im synthetischen Entwurf. Die echte Anwendung nutzt weiterhin ihre Fachprüfung.
Alle vier transportablen Konfigurationsseiten betten denselben Baustein ein.

Der [Originalvergleich](../../konfiguration-hilfe-originale.html) zeigt die drei
unveränderten Stitch-Entwurfsboards als eingebettete Bilder, einschließlich
Desktop-/Smartphonebeispielen und Erläuterungen. Auswahl und Zoom sind bedienbar;
die Originalboards selbst sind statisch. Die PNG-Dateien liegen unter `stitch/`.
Stitchs zusätzliche Funktionsideen und Aussagen sind keine vorhandenen Verträge.

Die [Referenz-Nachbauten](../../konfiguration-hilfe-vorschlaege.html) setzen die
Ideen mit unseren Komponenten und Originalhilfen für alle acht Gruppen um. Dabei
wurden Layout, Abstände und Darstellung angepasst; sie sind ausdrücklich keine
originalgetreue Wiedergabe. Die folgende Tabelle beschreibt diese Nachbauten:

| Feldhilfe | Darstellung | Abwägung |
| --- | --- | --- |
| A · Infoicon rechts | Klick-/Tap-Popover mit vollständiger Beschreibung, Standardwert und Regeln | Ruhige Formularzeilen; das geöffnete Fenster kann Nachbarfelder überdecken |
| B · Inline am Label | Hilfe klappt unter dem jeweiligen Feld auf | Bleibt beim Bearbeiten sichtbar; nachfolgende Zeilen verschieben sich |
| C · Kontextpanel | Gemeinsamer Bereich neben den Feldern, Auswahl per Fokus oder Infoicon | Gut für umfangreiche Hinweise; benötigt mehr Platz. Mobil steht die Hilfe beim ausgewählten Feld |

Die Panelgestaltung bleibt in allen drei Fällen beim bereits gewählten Aufbau A.
Die Feldhilfe-Buchstaben bezeichnen ausschließlich die zusätzliche Auswahl, keine
erneute Abstimmung des Panelaufbaus. Die Auswahl ist abgeschlossen: Die feste
Konfiguration verwendet C in der gemeinsam angepassten Form als rechtes Overlay.

Stitch bevorzugt subjektiv C für komplexe Verwaltungswerkzeuge. Die bisherige
Empfehlung A bezog sich auf den lokalen Nachbau. Zur Auswahl der Gestaltung
werden zuerst die tatsächlichen Originale verglichen; danach erfolgt eine
ausdrücklich abgestimmte Anpassung an die Referenz.

Stitch erhielt zuerst den aktuellen Screenshot mit drei eingeklappten Hinweisen,
nachdem Iconrahmen und Hinweis-/Aktionszeile angeglichen waren. Die
[Screen-Herkunft](stitch/field-help-screens.json) und der einzeilige Prompt sind
gesichert. Alle drei Screens wurden unabhängig erneut abgerufen, HTML und Bild
geprüft. Die Originale benötigen eine externe Tailwind-Runtime; sie sind deshalb
als nicht ausführbare `.html.txt`-Quellen und unveränderte PNG-Boards erhalten;
die fremden Scripts wurden nicht ausgeführt.
Die bedienbare Sichtung setzt ihre Layoutideen mit unseren eigenen Referenzquellen
um. Von Stitch zusätzlich erfundene Livevalidierung, Datenbankspalten, Revisions-
und Rollbackhistorien sind nicht Teil des bedienbaren Nachbaus oder vorhandener
Funktionen. Im Originalbild bleiben sie als Teil des Stitch-Entwurfs sichtbar.

`hilfe-vorschlaege.js`, `hilfe-vorschlaege.css` und `hilfe-viewer.js` betreffen
ausschließlich die zwei zusätzlichen Vergleichsdateien. Das Popover nutzt die
bestehende Overlaypositionierung und native Außenklick-/Escape-Schließung.
Die normale Referenz enthält diese Controller nicht.

## Varianten

Für das Dashboard gilt inzwischen eine neue Vorgabe: **keine Kennzahlen oder
Charts**, sondern ein ruhiges dunkles kosmisches Bild, kurzer Willkommenstext mit
ehrlichem Inhaltsplatzhalter, Orientierung zu den bestehenden Bereichen und
Schnellzugriffe. Die bisherigen Statistiklayouts A/B sind keine weiterverfolgte
Gestaltungsrichtung. Sie bleiben als frühere Entwürfe erhalten.
Die neue [Stitch-Anweisung](stitch/dashboard-platzhalter-prompt.txt) fordert zwei
deutlich unterschiedliche Vorschläge; die [bereinigte aktuelle Vorlage](stitch/dashboard-platzhalter-reference.html)
übernimmt den Adminrahmen und das Startseitenmotiv ohne Skripte, Formularwerte oder
Kennzahlen. Die neue Runde ist abgeschlossen; die Vorlage bleibt eine
Gestaltungsgrundlage für Stitch, kein fertiger Vorschlag oder neue Referenz.

Das [aktuelle Stitch-Originalboard](https://stitch.google.com/projects/14011270174485926515?node-id=d66897b7e33a47d3a23b3f433149d3ad)
zeigt beide Richtungen vollständig einschließlich eigener Smartphonekompositionen:
A kombiniert Bild/Text mit einem Kachelraster, B ein seitliches Bild/Textpanel mit
kompakten Schnellzugriffszeilen. Stitch bevorzugt subjektiv A wegen der Nähe zur
Portalstartseite. Auf Benutzerauftrag ist A als bedienbarer Entwurf im Referenzstil
umgesetzt und nach gemeinsamer Durchsicht in die feste Adminreferenz übernommen.

Das [Dashboard A](../../admin.html#dashboard) übernimmt Bildmotiv,
Begrüßung, kurzen Ausblick und das Schnellzugriffsraster. Logo K, Schrift,
Navigation, Iconrahmen und Buttons stammen aus der Referenz. Benutzerverwaltung,
Konfiguration, Mailtest und interner Versand öffnen die vorhandenen synthetischen
Bereiche; der Zielseitenkopf erhält den Tastaturfokus. Benachrichtigungen tragen
die Glocke ohne zusätzliches Rollenbadge. Auch dieser Schnellzugriff öffnet nun den
integrierten Versandbereich innerhalb der festen Referenz. Der Viewer prüft Quelle,
Origin und Antwortzuordnung; die bestehende Frame-Sandbox bleibt erhalten.
Die echten Dienstrechte bleiben unverändert.
Der kurze Ausblick verwendet unsere bestehende Infozeile; „Öffnen“ bleibt kompakt,
der zugängliche Buttonname nennt zusätzlich den jeweiligen Bereich.
Mobil steht das Bild über dem Text und die Kacheln untereinander. Das Bild ist
als Daten-URI eingebettet; es gibt keinen externen Bildabruf oder neue Statistiken.
Die Layoutauswahl bleibt für das Dashboard auf A. Ein statischer Inhaltsplatzhalter
hat keinen erfundenen Datenabruf-, Leer- oder Fehlerzustand. Die alten Statistik-
Originalboards bleiben gesichert; ihre lokalen Kennzahlen und Charts entfallen.

[Originalbild](stitch/dashboard-platzhalter-korrigiert.png),
[Original-HTML als nicht ausführbare Quelle](stitch/dashboard-platzhalter-korrigiert.html.txt),
[Herkunft](stitch/dashboard-platzhalter-korrigiert-screen.json),
[Korrekturanweisung](stitch/dashboard-platzhalter-korrektur-prompt.txt) und das
lokal gesicherte [Bildmotiv](stitch/dashboard-platzhalter-motiv.jpg) sind erhalten.
Die PNG-Ausschnitte A/B und mobil sind reine Zuschnitte dieses Originalbilds,
keine lokalen Designnachbauten. Beide Screens wurden unabhängig erneut abgerufen;
HTML und Bilder sind tatsächlich geprüft. Fremder HTML-Code wurde nicht ausgeführt.
Die Korrektur entfernt den übernommenen Beispielzähler an der Glocke und zeigt
beide Desktopentwürfe untereinander, damit B nicht abgeschnitten wird. Navigation
und Schnellzugriffe verwenden die Glocke für Benachrichtigungen. Stitchs generisches
Logo, Schriftfamilie und abweichende Details der Shell sind keine Designabnahme;
bei einer Übertragung bleiben die vorhandenen Referenzbausteine maßgeblich.

| Bereich | A | B | Einschätzung |
| --- | --- | --- | --- |
| Dashboard | Freigegeben: dunkles Bild, Begrüßung, Ausblick und Schnellzugriffsraster | Seitliches Bild und kompakte Icon-Zeilen bleiben im neuen Originalboard erhalten | A ist als Einstieg der festen Adminreferenz übernommen |
| Konfiguration | Freigegeben: beschriftete Formularfelder, zwei Spalten, rechtes Hilfeoverlay C | Früherer Vorschlag bleibt im Originalboard erhalten | In die feste Adminreferenz übernommen |
| Mailtest | Freigegeben: kompakte Formularkarte, Originaltexte, globale Notifications, Hinweis über der Buttonzeile und SMTP-Zugang | Früherer Vorschlag bleibt im Originalboard erhalten | In die feste Adminreferenz übernommen |
| Interne Benachrichtigungen | Ursprünglicher Aufbau bleibt im Originalboard erhalten | Gewählt und angepasst: Verfassen links, geprüfte Vorschau rechts, Historie darunter; mobil gestapelt | In die feste Adminreferenz übernommen |

Die wiederverwendeten Controls, Icons, dunklen Flächen, Größen und Navigation
stammen aus der aktuellen Referenz. Die lokal bedienbaren Entwürfe sind eine
Übertragung der abgerufenen Stitch-Ideen in diese Bausteine, keine unveränderte
Ausführung fremden HTMLs. Die [bereinigten Originalboards](stitch/screens.json)
und einzeiligen Prompts bleiben für den direkten Vergleich erhalten. Dortige
Geschmacksurteile und ungeprüfte Zusicherungen sind keine Messergebnisse.

Die Anwendung hat heute lediglich Begrüßung/Rollen im Dashboard. Der HTML-Entwurf
ergänzt Orientierung und Schnellzugriffe, keine Aggregationsabfragen, Login-Historie
oder sonstige Statistiken. Frühere Statistikboards sind überholte Vorschläge und
kein vorhandener Funktionsumfang.

## Vollständiger Quellabgleich

Der Mailtest verwendet die Labels, Titel, Beschreibung, Statusbeschriftungen,
Button-, Hinweis-, Standard- und Validierungstexte aus `AdminMailTestSection` und
den typisierten Gettern. Es gibt dort keine eigenen Feldplatzhalter; für den
Empfänger wird der vorhandene E-Mail-Platzhalter verwendet, für Betreff/Nachricht
deren tatsächlicher Standardtext. Die drei Feld-Infoicons entfallen; unter dem
Nachrichtenfeld steht der originale Hinweis auf den Versand als reinen Text.
Pflichtpunkte bleiben erhalten. Validierungsfehler erscheinen ausschließlich
als globale Fehler-Notification mit dem Originaltext; das betroffene Feld wird
markiert und fokussiert. Auch simulierte Versandfehler und Erfolge nutzen den
vorhandenen globalen Notification-Baustein, ohne das Panel zu verschieben.
Der originale Neustarthinweis steht mit Infoicon separat über der Aktionszeile;
der Link „SMTP Einstellungen“ ohne zusätzlichen Pfeil öffnet den Konfigurationsbereich.
Die rechte Erklärungsspalte entfällt, die Layoutauswahl bleibt für Mailtest auf A.
Enter in Empfänger/Betreff löst ausschließlich die lokale Simulation aus;
Enter im Nachrichtentext bleibt ein Zeilenumbruch. Erfolg/Fehler sind als
Simulation gekennzeichnet; es wird kein SMTP-Dienst aufgerufen. Die feste
Adminreferenz enthält Dashboard A, Benutzerverwaltung, Konfiguration, Mailtest A und Benachrichtigungen B.

[inventar.json](inventar.json) enthält Fundstellen, alle 30 Einstellungen mit
originalen deutschen Labels/Hinweisen, Feldtypen, Gruppen, sowie Mail- und
Nachrichtenfelder, Zustände, Limits und Rechte. Grundlagen sind `AdminView`,
`AdminConfigGroup`, `ConfigEntry`, die drei zuständigen Adminsections,
`UserNotificationService` und der typisierte Textkatalog. Es wurden keine lokalen
Konfigurationswerte, Zugangsdaten oder Datenbanken gelesen.

Benachrichtigungen B übernimmt alle verwendeten `getNotifications…`-Texte aus
`AdminUserNotificationsSection` über den typisierten Shared-Katalog, einschließlich
Hinweisen, Fehlermeldungen, Status- und Detailbeschriftungen sowie Singular/Plural.
Der Anwendungskatalog der Pagination bleibt im Inventar dokumentiert. Sichtbar
verwendet die Historie den bereits eingebetteten Frontend-Paginierungsbaustein
`c-pagination-bar` mit seinen gemeinsamen Klassen, Originalicons und Tooltips.
Beschriftung „Einträge pro Seite“, Bereichsanzeige und responsive Anordnung
entsprechen den Frontend-Benachrichtigungen. Die Seitengröße öffnet das gemeinsame
gestaltete Popover aus `../pagination.js`/`.css`; das zugrunde liegende Select
behält seinen Wert und seine vorhandenen Change-Aufrufer. Auch neu erzeugte
Historien-/Formularzustände verwenden diesen Baustein. Der Paneltext lautet
„Mitteilung erstellen, Vorschau prüfen und innerhalb der Anwendung senden.“ Seitengrößen (5/10/15) und
Längengrenzen stammen weiterhin aus den tatsächlichen Admin-/Servicequellen.
Die abgestimmten Kurzbeschriftungen „Vorschau“, „Details“, „Nachrichteninhalt“
und „Nach jeder Änderung erneut prüfen.“ sind lokale Entwurfsanpassungen. Für Titel/Nachricht existiert
kein eigener Anwendungsplatzhalter;
die vorhandenen Feldbezeichnungen dienen als dezente Platzhalter.

Der SMTP-Test-Badge verwendet den bereits vorgesehenen Statuspunkt, im
Ausgangszustand grün. Der Empfängerlabel im Versandformular trägt den blauen
Pflichtpunkt; die nicht bearbeitbare Empfängerübersicht erhält keinen.

Die linke Karte verwendet den Glockenrahmen der Navigation, „Intern“ und den
originalen Hinweis auf Versand ohne E-Mail. Eine zweite Infozeile links lautet
„Änderungen an der Mitteilung oder Auswahl erfordern ein erneutes Vorschau prüfen.“
Rechts bleibt nur „Vor dem Senden die Empfänger prüfen.“ stehen.
Checkboxen entsprechen dem bestehenden
`au-checkbox`-Baustein und stehen vertikal mittig neben der Bezeichnung samt
Hinweis. Die Mehrfachauswahl ist für die HTML-Sichtung ein nativer
Popover mit Referenzcheckboxen, kein Nachbau der späteren Vaadin-Komponente.
Escape und Außenklick schließen ihn; die Auswahl verschiebt keine Formularfelder.
Die Vorschau zeigt den originalen Prüfstatus und den Textinhalt. Über dem Titel
steht auf Benutzerwunsch „Administration · 27.10.2026 · 08:30“ als synthetische
Herkunfts-/Zeitangabe, keine bestätigte Versandzeit. Darunter stehen
bis zu drei runde Empfängeravatare ohne Namens-Badges; weitere Empfänger werden
als runder +N-Zähler angezeigt. Die vollständige Empfängerliste bleibt über
„Details anzeigen“ / „Details ausblenden“ aufklappbar. Jede Änderung sperrt Senden bis zur erneuten Prüfung.
Der Verlauf verwendet im Panelkopf und je Eintrag das Glockenicon. Seine aktuelle
Pagination bleibt auf Desktop und Smartphone erhalten. Die freigegebene Optimierung
aus dem [separaten Stitch-Vorschlag](https://stitch.google.com/projects/14011270174485926515?node-id=552aeeef457f426b9eed66525a42b025)
ist in den Entwurf übernommen: Titel, Zeitpunkt und Empfängerzahl bleiben einmalig
in der Zusammenfassung. Der Button heißt in beiden Zuständen „Details“; der Pfeil
zeigt den offenen/geschlossenen Zustand. Trennlinien laufen über die volle Panelbreite.
Aufgeklappt erscheint der Nachrichtentext auf dezenter Insetfläche mit blauer
Seitenkante. Die zusätzlich gewünschte blaue Überschrift „Nachrichteninhalt“
mit Sprechblasenicon und die dezente untere Zeile entsprechen der Stitch-Vorlage.
Die Fußzeile „Zustellung: Internes Postfach“ / „Unveränderliche Versandprotokollierung“
ist ausdrücklich nur eine statische Gestaltung im HTML-Entwurf
(`data-ad-visual-example`), keine Zusage oder Einführung einer Protokollfunktion. Auf-/Zuklappen per Maus und Tastatur, Titel-/Metadaten-Einmaligkeit,
Seitenwechsel und Seitengröße sind auf Desktop sowie Smartphone hoch/quer geprüft;
kein horizontaler Seitenüberlauf wurde beobachtet.
[Anfrage](stitch/history-optimization-prompt.txt), [Vorlage](stitch/history-current.png),
[Originalbild](stitch/history-optimization.png) und inaktives Original-HTML sind gesichert.
Stitchs vereinfachte Mobilpagination wurde nicht übernommen. Die ergänzte
Kopf-/Fußzeile ist auf Desktop und Smartphone hoch/quer ohne horizontalen
Seitenüberlauf geprüft. Die Vaadin-Anwendung bleibt unverändert.

Fehler nutzen die vorhandene globale Notification und markieren betroffene Felder;
nach Schließen kehrt der Fokus zurück. Simulierter Erfolg verwendet den Originaltext
mit ausdrücklichem „Simulation:“-Präfix und leert den Entwurf wie die Anwendung.

| Konfigurationsgruppe | Felder | Auswählbares Beispiel |
| --- | --- | --- |
| Allgemein | Service Name, Copyright Text, Basis URL | `settings:GENERAL` · 3 |
| Sicherheit | Remember-me-Dauer, Secret, Loginversuche, Sitzungsgrenze, Wartungsmodus | `settings:SECURITY` · 5 |
| Token | Verifizierungs- und Passwortreset-Gültigkeit/Lebensdauer | `settings:TOKENS` · 4 |
| Benutzerstatus | Automatische Abwesenheitszeit | `settings:USER_STATUS` · 1 |
| Benachrichtigungen | Aufbewahrung Posteingang / eigene Versandhistorie | `settings:NOTIFICATIONS` · 2 |
| E-Mail-Absender | Absender- und Adminadresse | `settings:EMAIL_SENDER` · 2 |
| SMTP-Server | Host, Port, Benutzername, Passwort, Auth, STARTTLS, SSL, SSL-Trust, Debug | `settings:SMTP` · 9 |
| E-Mail-Warteschlange | Poolgröße, Kapazität, Wiederholungen, Aufbewahrung | `settings:QUEUE` · 4 |

Die Seitennavigation klappt die acht Konfigurationsunterpunkte auf und zeigt den
aktuellen Bereich. Auf schmalen Ansichten nutzt sie den bereits vorhandenen
mobilen Navigationsdrawer. Speichern/Neu laden wirken auf die geöffnete Gruppe.
Alle acht Unterpunkte tragen cyanfarbene Icons im Linienstil der Referenz:
Zahnrad, Schutzschild, Schlüssel, Benutzer, Glocke, Briefumschlag, Server und Liste.
Die Einrückung kommt ohne senkrechte Verbindungslinie aus. Die Konfigurationspanels
verwenden dieselbe Icon-Zuordnung wie die Unterpunkte; auch das Hauptpanel der
internen Benachrichtigungen übernimmt die Glocke seines Navigationseintrags.
Die Panelköpfe verwenden den gemeinsamen Rahmen `portal-reference-icon` wie die
persönlichen Daten, einschließlich Hintergrund, Rand und Größe. Die Hinweiszeile
und die Aktionszeile der Konfiguration sind getrennt und jeweils vollbreit.
Die Glocke übernimmt die Geometrie aus dem angemeldeten Portal und kennzeichnet
auch den Hauptpunkt Benachrichtigungen. Der Papierflieger bleibt bei Versandaktionen.
Die zusätzlichen lokalen Navigationssymbole und das Archivsymbol stehen in `navigation-icons.html`;
Zuordnung und Darstellung liegen in `entwurf.js` und `entwurf.css`.
`konfigurationstexte.py` übernimmt Gruppen-/Feldbezeichnungen, Beschreibungen,
Platzhalter, Buttontexte und Speicherhinweis aus den vorhandenen typisierten
Textgettern. Die Feldhilfe umfasst den Originalhinweis, Standardwert und sämtliche
Eingaberegeln aus `AdminConfigurationSection`. Öffentliche Enum-Standardwerte
werden angezeigt, vertrauliche Werte maskiert; Umgebungswerte und echte
Konfigurationen werden nicht gelesen. Der Builder aktualisiert damit das Inventar.
Die Header-/Footerfläche nutzt wie „Persönliche Daten“ `--surface-panel`; der
Panelinhalt bleibt auf der dunklen Inhaltsfläche.
Die Vorschau übernimmt synthetische Werte nur im Arbeitsspeicher; vollständige
anwendungsseitige Konfigurationsvalidierung wird nicht dupliziert. Insbesondere
gibt es keinen neuen globalen Speichern-Dialog, keine erfundene Änderungswarnung
und keine echten Secrets. Ein Neuladen der HTML-Datei setzt Beispieldaten zurück.

## Zustände und Bedienung

- Dashboard: statischer Bild-/Textplatzhalter A mit vier bedienbaren Schnellzugriffen.
- Konfiguration: alle acht Gruppen, Ausgangszustand, markierter Fehler und
  technisch nicht verfügbar. Hilfen sind per Tastatur aufklappbar;
  Passwortanzeigen sind lokale Feldaktionen.
- Mailtest: Empfänger/Betreff/Nachricht, Pflichteingabefehler, SMTP-Annahme,
  Fehler, Wartung und nicht verfügbar. SMTP-Annahme bleibt von Zustellung getrennt.
- Interne Nachrichten: Superadmin als reguläres Rollenbeispiel; alle aktiven
  Beispielkonten oder Mehrfachauswahl. Titel 120, Nachricht 4000 Zeichen.
  Vorschau vor Versand; jede Eingabeänderung macht sie ungültig. Empfänger fehlen,
  Validierungsfehler, geänderte Empfängerlage, Erfolg, leere Historie, Wartung,
  nicht verfügbar und verweigerte Berechtigung sind zusätzlich auswählbar.
- Die Vorschau zeigt Titel/Nachricht, erste drei Empfängeravatare und weitere Anzahl
  sowie aufklappbare Empfängernamen und E-Mail-Adressen. Die auf Benutzerwunsch
  dargestellte Zeitangabe in der Vorschau ist ausdrücklich ein synthetisches Beispiel.
  Die Historie hat die Größen 5/10/15, die Frontend-Bereichsanzeige, „Gesendet“
  und aufklappbare Mitteilungsdetails mit Zeit/Empfängerzahl.
  Namen/E-Mail-Adressen in der Historie würden über die tatsächliche View hinausgehen
  und werden dort nicht erfunden.
  Es gibt keine erfundene Historienlöschung/-bearbeitung oder E-Mail-Funktion.

Der Zustand „Keine Berechtigung“ demonstriert die Ablehnung für Administrator;
das echte Recht wird im Dienst geprüft. Der HTML-Prototyp ist kein Rechtecheck.
Senden/SMTP/Speichern sind ausschließlich lokale Simulationen. Die Vorschau
enthält weder neue Backendfunktionen noch Zusagen über Zustellung, Laufzeit,
Wiederholungen oder dauerhafte Speicherung.

## Pflege

`build.py` liest die **aktuelle** gesicherte `admin-inhalt.html`, das Inventar sowie
`entwurf.css`, `entwurf.js` und `viewer.js`. Es erzeugt die beiden zusätzlichen,
selbstständigen HTML-Dateien und vorhandene temporäre Vorschaukopien. Es verändert
die bestehende Benutzerverwaltung inhaltlich nicht. Es bettet die freigegebenen
Bereiche zusätzlich in `admin-inhalt.html` und die Bereichsauswahl in `admin.html`
ein; Dashboard A, Benutzerverwaltung, Konfiguration, Mailtest und Benachrichtigungen
sind dort freigeschaltet. Der interne Versand benötigt keine Weiterleitung mehr.
Eigene Erweiterungen sind markiert, sodass wiederholtes Erzeugen keine doppelten
Controller, Icons oder Auswahllisten erzeugt. Der Benutzerverwaltungsbuilder ruft
diesen Schritt nach seinem normalen Aufbau auf. Nach Änderungen am Quellinventar alle
Gruppen erneut gegen den Anwendungscode abgleichen. Ruff und den gemeinsamen
Offline-Prüfer ausführen; der Build allein prüft keine Browserdarstellung.
Reine Entwurfsrunden verwenden `build.py --draft-only`: Dabei bleiben `admin-inhalt.html`
und `admin.html` bytegleich erhalten. Das Dashboardmotiv wird aus der lokal
gesicherten JPG-Datei in ein markiertes Template eingebettet.
