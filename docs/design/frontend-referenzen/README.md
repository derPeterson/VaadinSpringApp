# Freigegebene Frontend-Referenzen

Feste HTML-Momentaufnahmen der gemeinsam abgestimmten Gestaltung. Die Dateien
liegen auf Benutzerwunsch außerhalb von `target/`, damit ein Clean sie nicht
entfernt. Sie gehören zur Git-versionierbaren Projektdokumentation.

## Öffnen

| Bereich | Vorschau mit Steuerung | Eigenständige Oberfläche |
| --- | --- | --- |
| Angemeldete Benutzer | [Referenz öffnen](index.html) | [Portal](portal.html) |
| Nicht angemeldete Benutzer | [Referenz öffnen](visitors.html) | [Besucherportal](public.html) |
| Admin · Benutzerverwaltung | [Referenz öffnen](admin-users.html) | [Benutzerverwaltung](admin.html) |

Eine der drei Vorschauseiten im Browser öffnen. Die Vorschauen sind gegenseitig
verlinkt. Für die Steuerung und Navigation alle sechs HTML-Dateien im selben Ordner
belassen. Die drei Oberflächendateien lassen sich auch einzeln öffnen; die äußere Steuerung
ergänzt Bildschirmgrößen, Beispieldaten und bei Besuchern die Seiten-/Zustandswahl.

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
  gesicherte Portalreferenz. Dashboard, Konfiguration, Mailtest und Nachrichtenversand
  sind hier noch deaktiviert und separat auszuarbeiten. Nur die Benutzerverwaltung
  ist damit als Adminreferenz abgenommen.
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
`build.py` benötigt Python mit lxml und erzeugt ausschließlich `admin.html` und
`admin-users.html`; eine vorhandene lokale Vorschau wird mit aktualisiert.
Der Neubau benötigt weder den früheren Inspirationsordner noch Dateien unter target.
Das Kopfzeichen wird aus `../service-logo/logo-k.svg` über `quellen/service_logo.py`
eingebettet. Nach Änderung des Zeichens zuerst diesen Helfer, danach den
Benutzerverwaltungs-Generator ausführen; alle HTMLs bleiben eigenständig nutzbar.
Die Ausgangsbasis dient nur dem Generator; geöffnet werden die Referenzdateien oben.
Vergleichssteuerung, alternative Varianten und Stitch-Rohentwürfe gehören nicht mehr
zur Referenzsammlung. Der frühere Inspirationsordner wurde auf Benutzerwunsch entfernt.
