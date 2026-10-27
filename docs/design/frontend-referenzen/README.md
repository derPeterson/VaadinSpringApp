# Freigegebene Frontend-Referenzen

Feste HTML-Momentaufnahmen der gemeinsam abgestimmten Gestaltung. Die Dateien
liegen auf Benutzerwunsch außerhalb von `target/`, damit ein Clean sie nicht
entfernt. Sie gehören zur Git-versionierbaren Projektdokumentation.

## Öffnen

| Bereich | Vorschau mit Steuerung | Eigenständige Oberfläche |
| --- | --- | --- |
| Angemeldete Benutzer | [Referenz öffnen](index.html) | [Portal](portal.html) |
| Nicht angemeldete Benutzer | [Referenz öffnen](visitors.html) | [Besucherportal](public.html) |
| Administration · Dashboard, Benutzerverwaltung, Konfiguration, Mailtest und Benachrichtigungen | [Referenz öffnen](admin.html) | [Administration](admin-inhalt.html) |

`admin.html` ist die Vorschau mit Steuerung und startet ohne Seitenfragment immer
mit dem Dashboard. `admin-inhalt.html` ist die eigenständige Oberfläche. Die
frühere Datei `admin-users.html` wurde umbenannt; es gibt keine zweite Altversion.

Die feste Adminreferenz enthält alle abgestimmten Bereiche: Dashboard A,
Benutzerverwaltung, Konfiguration, Mailtest A und Benachrichtigungen B. Der interne
Versand ist über Seitennavigation, Bereichsauswahl und Dashboard-Schnellzugriff
innerhalb derselben Referenz erreichbar. Empfängerauswahl, Vorschau und eigene
Versandhistorie bleiben synthetisch; es werden keine Nachrichten oder E-Mails versendet.
Die [zusätzliche Adminsichtung](admin-entwuerfe.html) und gesicherten Stitch-Originale
bleiben zum Vergleich erhalten. Die Konfiguration ist
mit Originaltexten, Einstellungsbadges sowie den Paneloberflächen der persönlichen
Daten abgeglichen. [Inhalt, Varianten und Quellinventar](quellen/admin-entwuerfe/README.md)
erläutern die Unterschiede zur bestehenden Anwendung.
Für das Dashboard liegen inzwischen [neue Stitch-Originalvorschläge ohne Statistik](quellen/admin-entwuerfe/README.md#varianten)
mit dunklem Bildmotiv, kurzem Orientierungstext und Schnellzugriffen vor.
Variante A ist als Einstieg der [Adminreferenz](admin.html#dashboard)
mit Logo K, Referenzicons und bedienbaren Schnellzugriffen übernommen. Auch
Benachrichtigungen öffnen jetzt direkt innerhalb der Adminreferenz.
Der Mailtest zeigt die gewählte kompakte Variante A mit Anwendungstexten,
einem Klartext-Hinweis unter der Nachricht, globalen Fehlermeldungen mit Feldmarkierung
und einem SMTP-Hinweis samt Konfigurationszugang ohne Pfeil
über der Buttonzeile. Er ist über die Bereichsauswahl und die Seitennavigation der Adminreferenz erreichbar; es erfolgt kein echter Mailversand.

Für die Feldhinweise zeigt der [Originalvergleich](konfiguration-hilfe-originale.html)
die drei unveränderten Stitch-Entwurfsboards mit Auswahl und Zoom. Die
[bedienbaren Referenz-Nachbauten](konfiguration-hilfe-vorschlaege.html) sind separat
gekennzeichnet: Sie interpretieren die Ideen und geben das Originaldesign nicht
unverändert wieder. Zur Designauswahl zuerst die Originale vergleichen.
Die freigegebene Konfiguration verwendet C als rechte Hilfeseitenleiste:
Infoicon öffnet den Originalhilfetext als Overlay, ohne die Eingabefelder zu verschieben.
X, Schließen, Escape oder Außenklick schließen die Leiste. Der Inhalt zeigt die
vollständigen Hilfen, kompakte Platzhalterchips, Standardwert und Eingaberegeln.
Checkboxen, Pflichtpunkte und blaue Zahlenpfeile entsprechen dem abgestimmten Stand.
Die [separate Komponentenansicht](konfiguration-seitenleiste.html) bleibt zum Vergleich erhalten.
Die Panelicons und die getrennte Hinweiszeile über
den Aktionen sind bereits an persönliche Daten beziehungsweise Geräte angeglichen.

Die Eintragsauswahl der Pagination verwendet im Frontend, in der Benutzerverwaltung
und in der Versandhistorie dasselbe gestaltete Popover. Auswahl per Maus/Tap und
Tastatur, Escape, Außenklick und erneutes Anklicken sind bedienbar; bei einem
Bereichswechsel schließt das Menü. Die ursprünglichen Seitengrößen und bestehenden
Seitenwechsel bleiben erhalten. Gemeinsame Quellen: `quellen/pagination.js`,
`quellen/pagination.css` und `quellen/pagination.py`. Zum Aktualisieren zuerst
`pagination.py`, anschließend `quellen/admin-entwuerfe/build.py` ausführen; die
Sicherung wird inline eingebettet und benötigt keine externen Ressourcen.

Alle drei äußeren Referenzvorschauen enthalten zwei Auswahllisten:
**Service-Logo A–O** und **Hintergrund** mit aktueller Referenz plus zwölf dunklen
CSS-Varianten. Die Auswahl verändert die vorhandene Oberfläche zur Laufzeit,
ohne sie neu zu laden oder zusätzliche Oberflächenkopien anzulegen. Die alte
Zweifachauswahl „Stitch · dezent / Kosmos · abgedunkelt“ ist dadurch ersetzt.
Standalone bleiben K und der bisherige Hintergrund voreingestellt.
Die Auswahl ist nur eine lokale Sichtung und wird nicht gespeichert.

Eine der drei Vorschauseiten im Browser öffnen. Die Vorschauen sind gegenseitig
verlinkt. Für die gesamte Navigation einschließlich der Zusatzentwürfe alle dreizehn HTML-Dateien im selben Ordner
belassen. Die drei Oberflächendateien lassen sich auch einzeln öffnen; die äußere Steuerung
ergänzt Bildschirmgrößen, Beispieldaten und bei Besuchern die Seiten-/Zustandswahl.

Die [Vaadin-Machbarkeitsstudie](quellen/vaadin-studie/README.md) ordnet die neun
Besucherszenen bestehenden Flow-Komponenten zu. Das isolierte Java-Beispiel für
Anmeldung/Registrierung ist kompiliert, aber ohne Route oder laufende Vaadin-Vorschau.
Es verändert die Anwendung nicht und beweist noch keine pixelgleiche Darstellung.

## Logo- und Hintergrundsichtung

`quellen/erscheinungsbild.py` bettet die unveränderten gespeicherten SVG-Kandidaten
mit getrennten Verlaufs-IDs in eine inerte Bibliothek ein. Der Empfänger akzeptiert
nur bekannte Auswahl-IDs vom eigenen Elternfenster mit passender Origin; SVG oder
CSS werden niemals aus Nachrichten übernommen. `sandbox="allow-scripts"` bleibt
unverändert. Die Standardzeichen der eigenständigen Oberflächen ändern sich nicht.

`quellen/hintergruende.json` enthält die zwölf tatsächlich von Stitch gelieferten
CSS-Gradienten samt nötiger Hintergrundgrößen. Die Werte stammen aus dem unabhängig
erneut abgerufenen [Originalboard](quellen/admin-entwuerfe/stitch/backgrounds.html).
Sie ersetzen ausschließlich die dekorative Hintergrundebene; Panels, Eingaben,
Buttons und Textfarben bleiben bei ihren Referenzwerten. Keine Bilder, Animationen
oder CDNs. Stitch bevorzugt subjektiv 01 Marineblaue Staubwolke und 04 Finsternis-Saum.
Für eine besonders ruhige Oberfläche eignen sich auch 09 Tintenblaue Vignette oder
12 Kosmisches Anthrazit. Eine endgültige Auswahl steht aus.

Nach gezielten Änderungen an den gespeicherten Referenzen `erscheinungsbild.py`
erneut ausführen, danach bei Bedarf `quellen/admin-entwuerfe/build.py`. Der
Benutzerverwaltungsbuilder ruft die Erscheinungsbildeinbettung selbst auf.
Beide neuen HTML-Entwürfe und alle Quell-/Stitch-Dateien liegen außerhalb von
`target/` und bleiben bei einem Clean erhalten. Zum Betrachten genügt ein
Browser-Refresh; Anwendungsbuild und Neustart sind dafür nicht nötig.

## Inhalt und Grenzen

- CSS, Inter-Schriften, SVGs und Demo-Skripte sind in den HTML-Dateien eingebettet.
  Zum Betrachten sind weder Anwendung noch Build, Datenbank, CDN oder laufender
  Vorschau-Server nötig.
- Die angemeldete Referenz enthält Startseite, Benachrichtigungen, persönliche
  Daten, Sicherheit, Geräte/Sitzungen und die zugehörigen Overlays/Dialoge.
  Logo und Servicename im Kopf führen gemeinsam zurück zur Startseite.
- Die Besucherreferenz enthält neun öffentliche Szenen samt Beispielzuständen,
  gemeinsamen Controls und globalen Meldungen.
- Der Startseitenplanet verwendet in beiden Bereichen dieselbe Ringgeometrie aus
  `quellen/startseite-planet.svg`: Rückseiten liegen hinter der Kugel, die unteren
  vorderen Ringhälften davor. Die SVG ist jeweils eingebettet; die unterschiedliche
  responsive Anordnung der beiden Startseiten bleibt erhalten.
- Die Benutzerverwaltung übernimmt die abgestimmte, überarbeitete Variante 2:
  sieben Tabellenspalten, 24 synthetische Konten, Kontenzähler, Sortierung,
  Referenz-Pagination mit 10/20/50 Einträgen und die Benutzer-/Löschdialoge.
  Der Tabellenkopf übernimmt Stitch-Variante 2 mit den abgestimmten Anpassungen:
  Großbuchstaben in Secondary, feine Spaltentrenner, separate Sortierindikatoren
  und eine unverstärkte untere Trennlinie.
  Bei Hover und aktiver Sortierung wird die Beschriftung weiß; ausschließlich
  der aktive Richtungspfeil ist blau. Hover ändert nur die Schriftfarbe, keine
  Kästen, Rahmen oder Hintergründe. Kein zusätzlicher Rahmen oder
  farbiger Hintergrund kennzeichnet die aktive Sortierung. Tastaturfokus bleibt
  sichtbar. Quelle ist `quellen/benutzerverwaltung/table-header.css`.
  Suche, Aktiv Alle/Ja/Nein, vier Status und vier Rollen stehen im rechten Filter-
  Drawer. Status/Rollen erlauben Mehrfachauswahl: ODER innerhalb der Gruppe,
  UND zwischen Gruppen; keine oder alle vier Werte schränken nicht ein.
  Anwenden übernimmt auch die Suche, Abbrechen/Escape verwerfen den Entwurf,
  Zurücksetzen leert nur den Entwurf im Drawer; erst Anwenden aktualisiert die
  Tabelle. Abbrechen stellt auch nach Zurücksetzen die übernommenen Werte wieder her.
  Der blaue Zähler zählt einschränkende
  Gruppen; die zusätzliche externe Filterzeile entfällt. Beschriftungen entsprechen
  den persönlichen Daten, der Uhrzeithinweis der E-Mail-Hinweisbox.
  Der Drawer reserviert unten ausschließlich die sichtbare Copyright-Footerfläche.
  Bei langen Seiten ohne sichtbaren Footer reicht er bis zum Viewportrand; auch
  teilweise sichtbare Footer, Größenwechsel und Zurücksetzen bei offenem Filter
  werden berücksichtigt. Klicks auf Header oder Copyright-Footer schließen wie
  Abbrechen und verwerfen den Entwurf; beide Bereiche bleiben optisch ungedimmt.
  Eigene und geschützte Konten behalten ihre vorhandenen Einschränkungen.
  Das eigene Konto zeigt unter dem Namen das grüne Badge „Dein Benutzer“ mit
  Punkt, dezentem Hintergrund und feinem Rahmen wie „Dieses Gerät“.
  Auch lange eigene Namen dürfen innerhalb der Namensspalte umbrechen; das
  Kennzeichen bleibt darunter und verbreitert die Tabelle nicht.
  Die Schriftgröße der Tabelle bleibt erhalten. Am Desktop entfällt horizontales
  Scrollen durch kompaktere Kopfabstände; schmale Desktopfenster kürzen E-Mails
  wie bisher mit Ellipse und zugänglichem Volltext, lange Kopfzeilen dürfen umbrechen.
  Smartphones behalten die seitlich scrollbar nutzbare Tabelle mit allen Spalten.
  Passworthinweise stehen unter
  den jeweils zugehörigen Eingaben; die Rollenauswahl hat ihren Hinweis direkt darunter.
  Im Anlegen-/Bearbeiten-Dialog liegt die Gruppentrennung oberhalb des gesamten
  Rollenblocks; seine Beschriftung steht mit normalem Labelabstand bei den Checkboxen.
  Die Admin-Sidenav entspricht dem Portal; Startseite und Glocke führen in dieselbe
  gesicherte Portalreferenz. Dashboard A, Benutzerverwaltung, Konfiguration und
  Mailtest und interne Benachrichtigungen sind abgenommen und gemeinsam erreichbar.
  Der Dashboard-Schnellzugriff bleibt innerhalb der festen Referenz.
- Standardaktionen in Dialogen: „Abbrechen“ verwendet das schlichte X wie bei
  E-Mail-/Passwortänderung; „Schließen“ im Dialogkopf bleibt das X im Kreis.
- Benutzerlöschung und Entfernen von Benachrichtigungen verwenden denselben
  Bestätigungsbaustein: Kopf und Aktionsleiste auf Overlayfläche, dunkler Inhalt,
  Frage und Erklärung in einer Gruppe, darunter die Kontextkarte. Der
  Nachrichtenkontext ergänzt seinen Postfachhinweis mit mittigem Infoicon.
  Beide Aktionen stehen rechts mit gleichen Buttonmaßen; „Löschen“ bleibt dezent rot.
  Titel, Aktionsbeschriftung und Hinweise verwenden einheitlich „Löschen“;
  der Bestätigungsbutton heißt bei Einzel- und Sammellöschung nur „Löschen“.
  Der Inhalt folgt der tatsächlichen Textlänge ohne reservierte Leerzeilen.
  Die Kontextkarte zeigt beim Benutzer den Avatar neben Name/E-Mail, bei
  Nachrichten eine cyanfarbene Glocke neben Betreff beziehungsweise Anzahl.
  Die jeweiligen Texte und Löschwirkungen bleiben getrennt. Die Arbeitsquellen
  sind `reference_confirmation.py` und `reference-confirmation.css` unter
  `quellen/dialogdurchsicht/`;
  die Referenzdatei enthält den Baustein vollständig eingebettet.
- E-Mail- und Passwortänderung verwenden ebenfalls die gemeinsame dunkle
  Inhaltsfläche zwischen den helleren Kopf- und Aktionsleisten.
- Konto- und Profilbilddialoge haben innerhalb ihrer Fußleiste gleich breite
  Aktionen. Die E-Mail-Aktion „Bestätigung anfordern“ bleibt einzeilig; auf schmalen
  Smartphones stehen die gleich breiten Aktionen untereinander. Nachrichtendetails
  verwenden ebenfalls die dunkle Inhaltsfläche.
  Nachrichtenbestätigungen zeigen im Kontext Betreff, Herkunft, Datum/Uhrzeit und
  Lesestatus. Gerätebestätigungen übernehmen den gemeinsamen Aufbau mit
  Geräteicon, Zeitangaben und Status aus der ausgewählten Sitzung; Abmelden und
  Widerrufen behalten ihre fachlichen Bezeichnungen. Kontexticons sind mittig zum
  gesamten sichtbaren Textblock ausgerichtet, ohne Leerabstand unter den Metadaten.
- Persönliche Daten enthalten die ergänzte Geschlechtsauswahl im bestehenden
  Dropdownstil mit Auswahlpunkt und ohne zusätzlichen Erklärungstext. Geschlecht
  trägt wie die anderen Pflichtfelder den cyanfarbenen Pflichtpunkt und ist für
  Screenreader als Pflichtauswahl ausgezeichnet. Die
  Anwesenheitsüberschrift verwendet in beiden Status-/Avatarmenüs Cyan;
  Uhrzeitlisten behalten die Zeilenmarkierung ohne zusätzlichen Punkt.
- Die Sidenav verwendet helle Standardschrift und cyanfarbene Icons; die aktive
  Seite wird zusätzlich durch Hintergrund, Rahmen und Schriftgewicht markiert.
  Kleine Bereichsüberschriften übernehmen Cyan vom Seitenkopf; Größe,
  Versalien und Laufweite bleiben erhalten.
- Ausschließlich erfundene Beispieldaten. Keine echten Zugangsdaten eingeben.
  Aktionen simulieren das Design; sie melden niemanden an, verschicken keine
  E-Mail und verändern keine echten Konten oder Sitzungen. Neuladen setzt die
  lokalen Beispieldaten zurück. Die Sprachauswahl bleibt Designvorschau.
- Gestalterische Abnahme des HTML-Prototyps; echte Mobilgeräte, Bildschirmtastatur,
  Screenreader und die technische Umsetzung sind gesondert zu prüfen.
- Als Servicezeichen ist Kandidat K „Pulsar-Nova“ zur Sichtung in allen drei
  Referenzköpfen eingesetzt. Das Zeichen wird am Desktop optisch auf 40 px
  vergrößert, sein Layoutplatz bleibt 28 px breit und hoch. Die bestehende mobile
  Verkleinerung auf 24 px Layoutplatz bleibt erhalten; optisch sind es dort rund
  34 px. Headerhöhe und Servicename verschieben sich dadurch nicht.
  Alle [Kandidaten A–O](../service-logo/README.md) bleiben
  unabhängig erhalten; eine endgültige Logoabnahme steht aus.

## Pflege

Offline-Prüfung ohne zusätzliche Pakete, aus dem Projektroot:

```powershell
node docs/design/frontend-referenzen/pruefen.mjs
```

Die Prüfung kontrolliert lokale Verknüpfungen, eindeutige IDs, ARIA-/SVG-Ziele,
eingebettete Scripts/CSP und die
Besucher-Zustandsauswahl einschließlich nachlaufender Hash-Navigation sowie
Absender-/Origin-Prüfung für HTTP und lokale Dateien. Auch die Leerung sichtbarer
Passwortentwürfe nach einer erfolgreichen Demo-Aktion ist abgedeckt. Sie verwendet für die
Filter den echten Reset-Handler und prüft, dass übernommene Kriterien und die
Tabellenseite beim Leeren des Entwurfs erhalten bleiben. Zusätzlich wird der
ausgelieferte Benutzerfilter gegen unabhängige Beispieldaten für Suche,
Mehrfachauswahl, UND/ODER und Datumsgrenze geprüft. Sie verwendet für die
Routinglogik eine kleine DOM-Testumgebung; eine gerenderte Browserprüfung oder
eine Prüfung der echten Anwendungsabläufe ersetzt sie nicht.

Diese Dateien bilden den freigegebenen Stand ab. Spätere Versuche zunächst in der
Spielwiese bearbeiten und eine hier gesicherte Referenz nur nach bewusst
abgestimmter Änderung ersetzen. Der aktuelle Feature-Plan bleibt unter
[Designkritik mit Stitch](../../planung/designkritik-stitch.md).

Die CSS-Einbettung dient der vollständigen, transportablen Referenzsicherung.
Die Vaadin-Umsetzung verwendet weiterhin die gemeinsamen Theme-Quelldateien und
Komponenten gemäß [Theme-Anleitung](../../theme.md). Generatoren und temporäre
Stitch-Rohentwürfe unter `target/designkritik-stitch/` gehören nicht zu dieser
HTML-Sicherung und benötigen bei Bedarf eine eigene Sicherung vor einem Clean.
Die Quellen der freigegebenen Dialogdurchsicht sind zusätzlich unter
`quellen/dialogdurchsicht/` gesichert; das eigenständige HTML enthält weiterhin
alle benötigten CSS- und JavaScript-Bausteine.

Alle drei Referenzen verwenden die gemeinsamen HTML-Feldregeln aus
`quellen/eingabefelder.css`: Feldaktionsicons neutral in Ruhe, heller bei Hover,
Cyan bei Fokus oder geöffnetem Picker und gedämpft bei Disabled. Normale
Aktionsbuttonicons bleiben unverändert. Platzhalter sind dezent grau und normal
gewichtet. `quellen/eingabefelder.py` liest ihre Texte aus dem deutschen
Anwendungskatalog; fehlende Admintexte verwenden die Kontofelder der Registrierung.
Nur leere Eingaben zeigen Platzhalter; vorhandene Werte und Labels bleiben erhalten.
Der Helfer aktualisiert Portal/Besucher und ihre Vorschaukopien; anschließend
`quellen/benutzerverwaltung/build.py` für die Benutzerverwaltung ausführen.
Dekorative linke Brief-/Schlosssymbole sind entfernt; Text startet ohne reservierte
Iconfläche. Suchfelder behalten die Lupe. Rechte Bedienicons, Pflichtpunkte,
Auswahl- und Statusmarkierungen bleiben erhalten.

Die Benutzerverwaltung ist unter `quellen/benutzerverwaltung/` mit eigenständiger
Ausgangsbasis, Drawer-CSS/-Controller, Viewervorlage und Generator gesichert.
`build.py` benötigt Python mit lxml und erzeugt ausschließlich `admin-inhalt.html` und
`admin.html`; eine vorhandene lokale Vorschau wird mit aktualisiert.
Der Neubau benötigt weder den früheren Inspirationsordner noch Dateien unter target.
Das Kopfzeichen wird aus `../service-logo/logo-k.svg` über `quellen/service_logo.py`
eingebettet. Nach Änderung des Zeichens zuerst diesen Helfer, danach den
Benutzerverwaltungs-Generator ausführen; alle HTMLs bleiben eigenständig nutzbar.
Die Ausgangsbasis dient nur dem Generator; geöffnet werden die Referenzdateien oben.
Vergleichssteuerung, alternative Varianten und Stitch-Rohentwürfe gehören nicht mehr
zur Referenzsammlung. Der frühere Inspirationsordner wurde auf Benutzerwunsch entfernt.
