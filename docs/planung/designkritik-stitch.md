# Designkritik und Spielwiese mit Google Stitch

## Auftrag und aktueller Stand

**Kandidat K zur Sichtung in den Referenzen:** Der Benutzer lässt K „Pulsar-Nova“
im Besucherportal, angemeldeten Portal und der Benutzerverwaltung einsetzen und
optisch vergrößern. Das Zeichen nutzt am Desktop 40 px sichtbare SVG-Größe innerhalb
des vorhandenen 28-px-Layoutplatzes. Der bestehende mobile 24-px-Platz ergibt rund
34 px sichtbare Größe; Headerhöhe und Servicename bleiben an ihrer Position.
Die Einbettung kommt aus `quellen/service_logo.py` und der gesicherten Original-SVG.
Alle erstellten Logos A–O liegen unter `docs/design/service-logo/`, einschließlich
der früher verworfenen A/B/C. Keine endgültige Logoabnahme oder Vaadin-Übernahme.

**Zusätzliche Cosmos-Logorunden J/K/L und M/N/O zur Auswahl:** Auf ausdrücklichen Benutzerauftrag
entwarf Stitch drei weitere Zeichen mit Astronomiebezug und freier Farbwahl,
ohne die bisherigen Logos oder Referenzen zu ersetzen. Der unabhängig abgerufene
Screen `fd0ed2656fec4732a515d92f8f3a2de7` enthält J Orbital-Korona, K Pulsar-Nova
und L Nebula-Vortex; Stitch bevorzugt subjektiv K. Original-HTML und Bild sind
geprüft. J/K/L sind als SVGs unter `docs/design/service-logo/` gesichert; die
separate temporäre Sichtung ist `preview/logos-j-k-l.html`. Dort sind Inter und
gleiche Originalgeometrien für alle Größen eingebettet; keine Fremdskripte.
Desktop und Smartphone sind geprüft. Die endgültige Logoauswahl steht aus.
Eine weitere beauftragte Runde ergänzt M Korona-Finsternis, N Astro-Sextant und
O Kometen-Drift im unabhängig erneut abgerufenen Screen
`117f89e50c4e4748bf39519dd90664a8`. Stitch bevorzugt subjektiv N. HTML und Bild
sind geprüft; die SVGs liegen neben den erhaltenen Kandidaten, der getrennte
Vergleich unter `preview/logos-m-n-o.html`. Alle Größen verwenden dieselbe große
Originalgeometrie. Desktop und Smartphone sind geprüft. Die vorhandenen Logos
und Frontend-Referenz-HTMLs bleiben bytegleich erhalten; keine Auswahl übernommen.

**Aktuelle gesonderte Sichtung: Tabellenkopf der Benutzerverwaltung und drei neue Logos.**
Der Benutzer hat ausdrücklich eine neue Stitch-Runde mit freier Logo-Farbwahl
beauftragt. Der skriptfreie aktuelle Adminstand ist als Screen
`12095984541294395577` importiert. Der unabhängig erneut abgerufene Vorschlag
`173ede4f60a74a71a637a49801da765a` enthält zwei Tabellenkopfvarianten und die
neuen Vektorzeichen G/H/I. HTML und Bild wurden heruntergeladen und geprüft.
Option 1 ist Stitchs Empfehlung: ruhige dunkle Fläche, neutrale Labels und eine
eindeutige cyanfarbene aktive Sortierung; Option 2 setzt auf Versalien,
vertikale Separatoren und separate Indikatorkästchen. Beide sind statische
Gestaltungsvorschläge, keine neue Tabellenfunktion und keine Designabnahme.
Die generierten Kontextzeilen und Pagination sind keine Übernahmevorlage für
die bestehenden Funktionen. Lokal sind Inter und eine responsive Vergleichshülle
ergänzt; der erfundene Gesamtzähler 23 ist auf die vorhandenen 24 berichtigt.
Die ursprünglichen Vorschläge bleiben separat zur Sichtung erhalten:
`preview/header-logo-vorschlag.html` und `preview/logos-g-h-i.html` in der
temporären Spielwiese. G/H/I sind zusätzlich als eigenständige SVGs neben den
erhaltenen Kandidaten D/E/F unter `docs/design/service-logo/` gesichert.
Der Benutzer hat anschließend Tabellenkopf-Variante 2 gewählt und präzisiert:
Großbuchstaben und Secondary-Beschriftungen, bei Hover sowie aktiver Sortierung
weiß; ausschließlich der aktive Sortierpfeil blau. Hover ändert nur die Schrift,
keine Kästen, Rahmen oder Hintergründe. Die aktive Sortierung erhält keinen
zusätzlichen Buttonrahmen oder Hintergrund.
Diese Fassung ist in der Benutzerverwaltungsreferenz übernommen. Sortierrichtung
und Indikator folgen dem vorhandenen `aria-sort`; die Sortierfunktion bleibt erhalten.
Die endgültige Logoentscheidung steht weiter aus.
Die Tabelle bleibt auch auf Smartphones eine Tabelle; dort bleibt seitliches
Scrollen erlaubt. Am Desktop passen alle Spalten durch kompaktere Kopfabstände
und eine schmale Aufteilung mit E-Mail-Ellipse und umbrechender Aktivitätsüberschrift.
Die Schrift wird nicht verkleinert und keine Spalte entfernt. Das eigene Konto
kennzeichnet das grüne Badge „Dein Benutzer“ unter dem Namen, im Stil von
„Dieses Gerät“ mit Punkt, dezentem Hintergrund und feinem Rahmen.

**Paket 1 ist technisch fertig.** Profilaufnahmen, Bestandskritik und bereinigte
HTML-Referenzen sind vorhanden. Die benutzerseitige SingleFile-Aufnahme der
Account-Seite wurde über die vorhandene Stitch-CLI in das private Versuchsprojekt
„Workflow testen – Cosmos“ als sichtbarer Screen importiert und anschließend
erneut abgerufen sowie im Browser kontrolliert. Keine Stitch-Erweiterungen installiert
und keine Anwendungsoberfläche verändert.
**Paket 2 ist abgeschlossen: Gewählt ist die Kombination aus C und B.** Header,
Footer und Farbwelt folgen C; Contentstruktur und seitliche Kontonavigation folgen B.
Der Copyrighttext wird horizontal im Footer zentriert. Die zwei übereinanderliegenden
Tabreihen aus C werden durch die seitliche Navigation aus B ersetzt. A wird nicht
weiterverfolgt. Der erste Vorschau-Schritt aus Paket 3 ist durchgeführt:
die Kombination sichtbar zusammensetzen, ihre Desktop-/schmale Darstellung und
die Seitennavigation als möglichen gemeinsamen Rahmen für Home prüfen. Der
kombinierte Entwurf liegt als geprüfte Vorschau vor. Die gemeinsame Rückmeldung
legt die Verfeinerung und einen zusammenhängenden klickbaren HTML-Prototyp
des angemeldeten Benutzerportals fest. **Die HTML-Umsetzung aus 3a–3c ist
gestalterisch als Referenz abgenommen.** Der einmalige Folgeauftrag wurde am
24. Oktober 2026 nach Prüfung der Uhrzeit und des regulären Kontingents ausgeführt.
Die fünf Portalszenen, gemeinsame Navigation und zugehörige Beispieldialoge sind
lokal klickbar. Der angemeldete Benutzerbereich gilt inzwischen als feste
Gestaltungsreferenz. Der Besucherbereich ist nach den letzten Schriftkorrekturen
ebenfalls als feste HTML-Gestaltungsreferenz abgenommen. Gesamtfeature und
technische Übertragung bleiben offen; Paket 3d ist nicht freigegeben.
Vaadin-Spielwiese und globale Theme-Übernahme sind noch nicht begonnen.
**Nächste vereinbarte Erweiterung sind die übrigen Adminbereiche als HTML-Referenz.**
Die Dialog-/Notificationdurchsicht ist erfolgt; der Benutzer verwaltet die
ergänzende Komponentenübersicht inzwischen separat. Sie ist kein Bestandteil
der Repository-Referenzen. Neue Verbesserungen benötigen weiterhin eine
gemeinsame Sichtung vor ihrer Übernahme.
Der Benutzer möchte vor der technischen Übertragung auch diesen Bereich mit
Stitch anhand der beiden festen Referenzen gestalten lassen. Zuerst dient die
Benutzerverwaltung als Beispiel für dichte Inhalte; danach folgen die übrigen
bestehenden Adminbereiche. Die beiden abgenommenen Referenzen bleiben der
gestalterische Rahmen. Der konkrete Ablauf steht in der Ergänzung vor 3d.
Die überarbeitete Variante 2 der Benutzerverwaltung ist mit synthetischen Konten
als HTML-Referenz abgenommen und dauerhaft gesichert. Die übrigen Adminbereiche
stehen noch aus.
**Inbox-Variante C und die ergänzende Overlayrunde sind als zusätzliche
HTML-Entwurfsreferenz übernommen.** Die zuletzt gewünschten Kleinkorrekturen sind
umgesetzt: horizontale Toolbartrennung erhalten, vertikale Trennlinie entfernt
und kompakte konturierte Zahlenplakette ohne sichtbares „ungelesen“.
Persönliche Daten bleiben der gestalterische Maßstab;
Header, Seitenkopf, Seitennavigation, Footer, Farbpalette und die gelungenen
Benutzer-/Sprach-/Statusoverlays bleiben fest. Die drei Contentlösungen erhalten
den bestehenden Funktionsumfang einschließlich Pagination als statische
Gestaltungsvorlage. Der vollständige Benachrichtigungsbereich ergänzt Persönliche
Daten im klickbaren Portalprototyp einschließlich Glockenvorschau,
Nachrichtendetail und gemeinsamem Bestätigungsdialog. Die abschließende Sichtung
der Kleinkorrekturen und des Zusammenspiels bleibt möglich. Danach werden
weitere Seiten einzeln abgestimmt, nicht automatisch parallel umgestaltet.
**Startseite mit Stitch-Willkommensmotiv ist als vorläufiger Platzhalter positiv
bewertet und Arbeitsgrundlage.** Der Entwurf bleibt im unveränderten Referenzrahmen
klickbar. Die Benachrichtigungstoolbar hat eine durchgehende
horizontale Trennung; die zuvor gemischten Tooltipdarstellungen sind vereinheitlicht.
Beim Geburtsdatum ist der fehlende blaue Pflichtpunkt in der HTML-Referenz ergänzt;
der Anwendungscode kennzeichnet es bereits als Pflichtfeld. Die Markierung der
aktiven Sprache bleibt auf ausdrücklichen Benutzerwunsch unverändert.
Die Sicherheitsrunde einschließlich Passwort- und E-Mail-Änderungsdialog ist vom
Benutzer als Teil der HTML-Entwurfsreferenz abgenommen. Geräte & Sitzungen samt
gemeinsamer Bestätigung liegen jetzt als zusätzlicher HTML-Vorschlag zur Sichtung vor.
Der gesondert freigegebene Besucherbereich ist mit den bestehenden öffentlichen
Abläufen abgeglichen und als zusätzlicher HTML-Prototyp mit neun Szenen aufgebaut.
Die nachgearbeitete Besucherfassung übernimmt die festen Referenzbausteine und ist
nun ebenfalls gestalterisch abgenommen. Die Anwendung bleibt unverändert.
Der Profilbild-Dialog hat jetzt eine eigene Stitch-Entwurfsrunde und ist in die
HTML-Referenz eingepasst und vom Benutzer gestalterisch abgenommen. Drei separate
Stitch-Logoideen stehen zur Auswahl, ohne das bisherige Servicezeichen zu ersetzen.
A/B/C wurden aus derselben Account-Referenz erzeugt. Erfundene
Fachangaben wurden anschließend in den HTML-Exporten gezielt bereinigt; die
maßgeblichen Screens tragen den Zusatz „· Vergleich“. Der lokale Vergleich zeigt
Ausgangsstand und alle drei Richtungen bei `1440×1000` ohne externe Laufzeitressourcen.
Die weiteren Schritte aus Paket 3 und das gesamte Designfeature bleiben offen;
keine automatische Git-Aktion. Die Anwendungsoberfläche ist unverändert.

- Branch: `feature/designkritik-stitch`.
- Basis: lokales `main`, Commit `ac3b239fac5ae0e2e6f56be3483ae5811be51648`;
  beim Einstieg war der Arbeitsstand sauber.
- `BUGS.md` enthält derzeit keine bestätigten offenen Anwendungsfehler.
- Zielgruppe: zunächst ein allgemeines Benutzerportal, ohne erfundene zusätzliche
  Fachdomäne. Desktop hat Vorrang; Smartphone wird früh mitgeprüft.
- Stitch-Zugang ist vorhanden. Der Benutzer erlaubt dort vorbereitete Beispiele
  mit erfundenen Daten und bereinigten Bildern seiner Oberfläche.
- Das Ergebnis ist ein bewusst verwerfbarer Versuch. Eine überzeugende Richtung
  zu finden oder begründet zu verwerfen ist wertvoller als möglichst viel umzubauen.

## Zielbild und feste Grenzen

Silent Cosmos soll erkennbar bleiben, aber ruhiger, eindeutiger und hochwertiger
wirken. Der CSS-Weltraum mit Planeten gefällt dem Benutzer und bildet den
gestalterischen Anker. Gewünscht sind sehr dunkle, bläuliche Grundflächen,
kontrollierte Akzente und sichtbare gestalterische Sorgfalt. Weder sterile
Minimalgestaltung noch verspielte Effekte sind das Ziel. „Dark Midnight Blue“
beschreibt zunächst eine Richtung, noch keine freigegebene Farbpalette.

Zu untersuchen sind **Aussehen und Bedienung**: Informationshierarchie, Navigation,
Lesbarkeit, Zustände, Abstände und verständliche Aktionen. Ein bloßer Farbtausch
wäre zu wenig. Layoutänderungen sind Vorschläge; fachliche Änderungen benötigen
eine bewusste Entscheidung.

Unverändert gelten bis zu einer ausdrücklichen neuen Vereinbarung:

- Vaadin Flow mit Java-Komponenten, vorhandenen Cosmos-Bausteinen und lokalem Inter.
  Kein React-/Tailwind-Neubau der Anwendung aus einem Stitch-Export.
- Bestehende Funktionen, Rechte, Sicherheitsabläufe, Validierung und Übersetzungen.
  Bedienaktionen dürfen anders angeordnet, aber nicht stillschweigend entfernt werden.
- Pagination bleibt Pagination, einschließlich Suche, Filtern, Zählern und
  Seitenauswahl; kein Endlosscrollen.
- Kontobereiche Profil, Sicherheit und Geräte/Sitzungen bleiben erreichbar;
  bestehende URL-Zustände und Zurück-/Vorwärtsnavigation bleiben erhalten.
- Öffnen einer Benachrichtigung markiert sie nicht automatisch als gelesen.
  Lesen, Entfernen und Sammelaktionen behalten ihre getrennten Bedeutungen.
- Erst eine lokale Spielwiese. Ein neuer gemeinsamer Theme-Standard, der Umbau aller
  Seiten sowie Commit, Merge und Push gehören nicht zur jetzigen Freigabe.

## Gewählter Weg: Profilreferenz, klickbares Portal, danach Vaadin

**Erster Entwurf ist die Kontoansicht mit Profil**, einschließlich Appkopf,
Navigation, Avatar/Status, Formular und Hauptaktion. Sie enthält genug echte
Aufgaben, um Gestaltung und Bedienung zu bewerten. Die aktuelle Startseite enthält
vor allem Einleitung und Hintergrund; sie würde ein schönes Bild erlauben, aber
wenig über die spätere Alltagstauglichkeit aussagen.

Nach der positiven Rückmeldung zum kombinierten Profilentwurf möchte der Benutzer
vor dem Vaadin-Umbau das Zusammenspiel des angemeldeten Portals beurteilen.
Der nächste geplante Schritt erweitert deshalb die bisherige kleine Profilprobe
um einen klickbaren HTML-Prototyp. Erst dessen gemeinsame Bewertung entscheidet
über die Übertragung nach Vaadin. Der Prototyp umfasst:

| Szene | Zweck und Einordnung |
| --- | --- |
| Portal | Startseite mit ehrlichem Inhaltsplatzhalter, Benachrichtigungen mit Beispieldaten und vorhandener Pagination |
| Mein Konto | Persönliche Daten, Sicherheit sowie Geräte und Sitzungen mit erfundenen Daten |
| Gemeinsamer Rahmen | Header, Seitenkopf, Navigation, Footer, Benutzermenü und Benachrichtigungsvorschau |
| Dialoge/Zustände | Vorhandene Benutzeraktionen inventarisieren; E-Mail-/Passwortänderung, Profilbild, Nachrichtendetail und tatsächlich vorhandene Bestätigungen/Fehler als visuelle Beispiele |

Keine riesige Seite mit allen Widgets auf einmal und keine vollständige Kopie der
Anwendung. Die Szenen zeigen die bestehenden Funktionen im Seitenkontext, ohne
deren Dienste nachzubauen. Neue Dashboard-Inhalte werden nicht erfunden, um ein
leeres Bild attraktiver zu machen. Die zusätzlich beauftragten anonymen
Zugangsseiten sind inzwischen als Referenz abgenommen. Die Administration wird
als nächste HTML-Erweiterung anhand derselben festen Gestaltung ergänzt;
vorhandene Rechte und Funktionen bestimmen ihre sichtbaren Beispiele.

Vor dem Bau der Spielwiese werden bereits vorhandene Ansichten als Referenz
aufgenommen. Erst nach Bewertung des HTML-Portals entsteht der Vaadin-Pilot. Damit
kann der Benutzer frühe Entwürfe ablehnen, bevor viel Implementierungsarbeit anfällt.

## Bestandskritik: Quellenhypothesen und Profilaufnahme

Grundlage sind `docs/theme.md`, die gepflegten Cosmos-CSS-Quellen sowie
`HomeView`, `AccountView`, `UserNotificationsView`, `AccountSettingsComponent`,
`AccountNavigationTabs` und `AccountPanelComponent`. Die folgende Tabelle enthält
die ursprünglichen Quellenhypothesen. Die darunter beschriebene Profilaufnahme
ergänzt sie um tatsächliche visuelle Beobachtungen; eine gemeinsame Designabnahme
ist damit noch nicht erfolgt.

| Beobachtung | Einschätzung / zu prüfende Verbesserung |
| --- | --- |
| Zentrale semantische Tokens und gemeinsame Panels/Controls existieren | Gute Grundlage für eine konsistente Weiterentwicklung; nicht durch parallele Einzellösungen ersetzen |
| CSS-Hintergrund ist unabhängig von Interaktion; reduzierte Bewegung ist berücksichtigt | Erkennungsmerkmal bewahren, Konzentration und Bewegung im Alltag gesondert prüfen |
| Grundpalette ist bereits sehr dunkel | „Noch dunkler“ allein löst die Unruhe wahrscheinlich nicht; Trennung und Anzahl der Flächen sind entscheidend |
| Cards, Topbar, Sidebar und Hauptaktionen haben Verläufe; Avatare Glows; Panels eigene Iconflächen | Vermutlich konkurrierende Hervorhebungen. Im Bild prüfen, welche wirklich Orientierung geben |
| Kontoseite kombiniert Seitenkopf, Seitentabs, Zusammenfassung, Untertabs und Panelkopf | Möglicherweise zu viele Hierarchieebenen vor dem eigentlichen Formular. Zusammenfassen, ohne Orientierung oder Funktionen zu verlieren |
| Inbox-Zeilen besitzen eigene Cardflächen und Rahmen innerhalb eines übergeordneten Panels | Kartenverschachtelung als Ursache der Unruhe prüfen; ruhigere Liste mit gezielten Zustandssignalen vergleichen |
| Zahlreiche Texte liegen auf kleinen Schriftstufen; ausgefüllte Eingaben sind kräftig gewichtet | Lesbarkeit und Hierarchie prüfen, insbesondere längere Hilfen und DE/EN. Kein pauschales Größenurteil ohne Darstellung |
| Transparenz und bewegter Hintergrund wirken zusammen | Kontrast am tatsächlich zusammengesetzten Hintergrund messen, nicht nur einzelne Tokenwerte vergleichen |

Diese Hypothesen wurden in Paket 1 für die ausgewählte Profilansicht anhand der
Aufnahmen geprüft und präzisiert; Beobachtungen anderer Ansichten bleiben bis zu deren
optionaler Vertiefung offen. Geschmack, nachweisbares Bedienhindernis und technische
Ursache werden getrennt benannt.
Ein Finding erhält Ort/Zustand, Beleg, Auswirkung, Priorität und einen konkreten
Verbesserungsvorschlag. Gute Eigenschaften werden ebenso ausdrücklich festgehalten.

### Beobachtungen am Profil

Vergleichsstand: `ff108b19ea5ea194e250926c54d8b987e2fa2c35`, optimiertes JAR,
Chromium `153.0.8010.12`, Deutsch, Desktop `1440×1000` und schmal `390×844`.
Der sichtbare Benutzer „Lea Sommer“ und sämtliche Kontodaten sind erfunden.
Die Aufnahmen zeigen denselben Profilinhalt; Hintergrundbewegung wurde nur für
die Screenshots angehalten. Das ist keine Prüfung der Animation im normalen Betrieb.

**Erhalten:** Die dunkelblaue Farbwelt ist zusammenhängend; die Aufnahme zeigt
kein wahlloses Mischen vieler bunter Panelhintergründe. Hintergrund und Planeten
geben eine erkennbare Identität. Das zweispaltige Desktopformular ist ordentlich
ausgerichtet. In der schmalen Ansicht stehen die Felder verständlich untereinander,
und die Speicheraktion bleibt nach Scrollen erreichbar.

| Priorität | Ort und beobachteter Zustand | Auswirkung und Verbesserungshypothese |
| --- | --- | --- |
| Hoch im Designversuch | Schmal: Kopf, Einleitung, Seitentabs, große Profilzusammenfassung, Untertabs und Panelkopf stehen vor den Feldern; im ersten Viewport ist noch kein Eingabefeld sichtbar | Die eigentliche Profilaufgabe beginnt spät. Identitätsbereich kompakter machen und redundante Kopf-/Navigationsebenen zusammenführen |
| Mittel | Desktop: „Mein Konto / Einstellungen“, „Einstellungen“, aktive Einstellungen-Navigation, „Persönliches Profil“, „Persönliche Daten“ und „PROFIL“ markieren ähnliche Zusammenhänge mehrfach | Die Hierarchie wirkt aufwendig. Pro Ebene einen eindeutigen Zweck geben, wiederholte Beschriftungen reduzieren, Funktionen und Orientierung erhalten |
| Mittel | Schmal: Der Anwendungsname bricht mit einem einzelnen letzten Buchstaben in eine zweite Zeile um | Die Kopfzeile wirkt beengt. Platzverteilung von Logo, Name und Aktionen als Teil der Gestaltung prüfen; Namen nicht unkontrolliert umbrechen lassen |
| Mittel | Beide Größen: Der grün leuchtende Avatar erscheint im Appkopf und nochmals groß in der Profilzusammenfassung | Status und Identität ziehen viel Aufmerksamkeit auf sich. Eine primäre Identitätsstelle festlegen und die zusätzliche Statusdarstellung zurücknehmen |
| Mittel | Hintergrund, Cardrahmen, Trennlinien, Iconkästen und aktive Tabunterstreichungen sind gleichzeitig sichtbar | Mehrere Gliederungssignale konkurrieren. Weniger Rahmen-/Flächenwechsel mit klareren Abständen und Typografie vergleichen; kosmischen Hintergrund erhalten |

Dies sind Designbefunde, keine nachgewiesenen Fehler in Speichern, Rechten oder
Sitzungsabläufen. Kontrastwerte, Tastaturbedienung, EN-Layout und echte Mobilgeräte
wurden in dieser Referenzaufnahme nicht abgenommen. Andere Ansichten bleiben
bei den oben gekennzeichneten Quellenhypothesen.

## Was Stitch beitragen kann und was offen bleibt

Recherche vom 23. Oktober 2026:

| Fähigkeit | Beleg und praktische Konsequenz |
| --- | --- |
| Bestehende Bilder, Text und Code als Kontext; Varianten und interaktive Entwürfe | Von Google beschrieben. Der geplante Mensch–Coding-Agent–Stitch-Ablauf ist kein unbetretener Sonderweg. [Google-Produktbeschreibung](https://blog.google/innovation-and-ai/models-and-research/google-labs/stitch-ai-ui-design/) |
| Projekte/Screens lesen, Screens erzeugen, bearbeiten und Varianten anfordern | Offizielle MCP-Anbindung vorhanden; Anmeldung ist zusätzlich zum normalen Stitch-Webzugang nötig. [MCP-Einrichtung und Werkzeuge](https://stitch.withgoogle.com/docs/mcp/setup/) |
| HTML und Vorschaubilder abrufen | Im offiziellen Ablauf beschrieben. Das HTML dient als Designreferenz; es übernimmt nicht die Verträge unserer Anwendung. [MCP-Leitfaden](https://stitch.withgoogle.com/docs/mcp/guide/) |
| Gestaltungsregeln zwischen Werkzeugen austauschen | `DESIGN.md` kann Farbrollen, Typografie und weitere Regeln transportieren. Das ist eine Spezifikation, keine fertige Vaadin-Theme-Datei. [Google zu DESIGN.md](https://blog.google/innovation-and-ai/models-and-research/google-labs/stitch-design-md/) |
| Bestehende Oberfläche übernehmen und Design-Workflows mit Coding-Agenten verbinden | Das Google-Labs-Repository bietet entsprechende Skills einschließlich Codex-Anbindung. Es bezeichnet sich selbst als nicht offiziell unterstütztes Google-Produkt. [Stitch Skills](https://github.com/google-labs-code/stitch-skills) |

Zum Recherchezeitpunkt waren **keine Stitch-Werkzeuge verbunden**. Die Plugin-Suche nach
Stitch lieferte keinen Treffer im verfügbaren Katalog. Das schließt eine direkte
MCP-Anbindung oder den im Google-Labs-Repository beschriebenen Marketplace nicht aus.
Eine zusätzliche MCP-Anbindung wurde nicht eingerichtet oder praktisch geprüft.
Der unten beschriebene CLI-Übergabeweg ist dagegen nachgewiesen.

### HTML-Vorschau als Übergabe, Screenshots als Vergleich

Ein gespeichertes Seiten-HTML ist bei Vaadin nicht automatisch ein vollständiges
Abbild. Komponenten nutzen Shadow DOM; Werte, Icons, Overlay-Inhalte und Styles
müssen tatsächlich mitkommen. Ein normaler Quelltext- oder `outerHTML`-Export kann
deshalb unvollständig sein. [Vaadin-Komponentenstyling](https://vaadin.com/docs/latest/styling/styling-components)

Der Benutzer hat für diesen Ablauf nach seinem eigenen Stitch-Versuch eine
HTML-Vorschau statt einer reinen Screenshot-Übergabe vorgegeben. Die HTML-Vorlage
gehört damit zum Kern von Paket 1; die Screenshots bleiben Vergleichsbelege.
Das ist die Entscheidung für unseren Arbeitsweg, keine allgemeine Behauptung,
Stitch könne grundsätzlich keine Bilddateien annehmen.

1. Bereinigte Ausgangsaufnahmen mit festen Ansichtsgrößen und realistischen
   Beispieltexten als Vergleich verwenden.
2. Für den nachgewiesenen CLI-Weg die Seite mit SingleFile speichern und bereinigen;
   dessen deklarative Shadow-DOM-Vorlagen und eingebettete Gestaltung erhalten.
   Skripte, Handler, Sicherheitstoken und lokale Serververweise entfernen. Nur
   erfundene sichtbare Profildaten übernehmen. Die getrennt erstellten statischen
   Desktop-/schmalen Vorschauen mit aufgelösten Slots und berechneten Styles
   bleiben zusätzliches Vergleichsmaterial, kein allgemeiner Vaadin-Exporter.
3. Nach Abschalten der Ausgangsinstanz Desktop und schmale Vorschau in Chromium
   prüfen: Felder, Werte, Logo und Darstellung müssen erhalten bleiben; externe
   Netzaufrufe sind nicht erforderlich. Abweichungen im Vergleich korrigieren.
4. Die HTML-Datei in ein eigenes Stitch-Projekt übergeben und den dortigen Import
   tatsächlich prüfen. Keinen allgemeinen Vaadin-Exporter aus dieser einmaligen
   Profilaufnahme entwickeln.

Die vorbereiteten Dateien sind getrennte statische Momentaufnahmen für Desktop
und schmale Ansicht. Sie enthalten keine Fachlogik und sind weder ein fertig
responsives Theme noch ein Ersatz für die laufende Vaadin-Anwendung.

Die vorhandene Google-Labs-Extraktion ist ein Kandidat, kein nachgewiesener
Vaadin-Konverter. [Quellbeschreibung der HTML-Extraktion](https://github.com/google-labs-code/stitch-skills/blob/main/plugins/stitch-design/skills/extract-static-html/SKILL.md)
Externe Skills werden vor einer möglichen Installation geprüft und nur passend
eingesetzt; ihre React-Buildabläufe passen nicht zum Projektauftrag.

### Automatisierung mit überprüfbaren Ergebnissen

Für den aktuellen Versuch die bereits vorhandene Stitch-CLI verwenden. Der frühere
SingleFile-/CLI-Import ist durch die Ausgabe im
[geteilten Chat](https://chatgpt.com/share/6adb81e8-b7c8-83eb-8189-4b4e7c4da813)
und die sichtbare Benachrichtigungsreferenz im bestehenden Projekt belegt.
`stitch upload screen` importiert HTML als Screen; das Anhängen einer Codedatei im
Webdialog hat diesen Ablauf im aktuellen Versuch nicht ersetzt.
Eine zusätzliche MCP-Anbindung bleibt eine mögliche spätere Vertiefung, keine
Voraussetzung für den ersten funktionierenden Kreislauf.
Nur benötigte Designfunktionen einrichten. Zugangsdaten gehören in
eine geschützte lokale Einrichtung, weder in Chat-Ausgaben noch ins Repository.
Kein vollständiger Repository-Upload und keine Veröffentlichung/Tunnel der lokalen
Anwendung. Ein lokaler Link wird nicht als von Google erreichbare Vorlage behandelt.

Vor einer Übertragung nur die ausgewählten Dateien prüfen: keine Cookies,
Sitzungskennungen, echten E-Mail-Adressen, Zugangsdaten oder internen Konfigurationen
im Export. Entfernte Skripte dürfen nicht die sichtbare Referenz unbrauchbar machen;
bei Zweifeln statische Bilder verwenden. Heruntergeladenes HTML wird vor einer
lokalen Vorschau auf externe Skripte und Ressourcen geprüft und als fremder Entwurf behandelt.

Ein öffentliches, beim Abruf noch offenes Fehlerbild berichtet von erfolgreichen
Edit-Antworten ohne gespeicherte Änderung. Das ist ein **fremder Reproduktionsbericht,
kein Beweis für unser Konto**. Deshalb nach einer Probeänderung den Screen neu
abrufen und die sichtbare Änderung kontrollieren. Bei einem Timeout zunächst den
Projektstand prüfen, statt durch blindes Wiederholen doppelte Entwürfe anzulegen.
[Stitch-SDK-Issue 361](https://github.com/google-labs-code/stitch-sdk/issues/361)

Falls MCP bei Import oder Bearbeitung nicht zuverlässig funktioniert, bleibt der
Ablauf nutzbar: Codex bereitet bereinigte HTML-Vorschauen und kopierbare Prompts vor.
CLI oder ein verfügbarer Browserzugriff führen den Schritt in Stitch aus; falls
nötig übernimmt der Benutzer die Dateiauswahl. Anschließend werden
die ausgewählten Ergebnisse zurückgegeben. Nach einem gezielten Nachversuch die
Alternative nutzen; die Designaufgabe nicht in eine Integrationsbaustelle verwandeln.
Es werden weder kostenpflichtige Zugänge aktiviert noch Generierungslimits umgangen.

## Gestalterische Leitplanken für den Versuch

### Vereinbarte Verfeinerung des kombinierten Entwurfs

- Den charakteristischen Seitenkopf auf allen Seiten erhalten: übergeordnete
  Einordnung, Seitentitel mit dem vorhandenen Punktmotiv und kurze Beschreibung.
  Im Konto lautet die Einordnung **Mein Konto**, der Titel **Persönliche Daten**.
  Keine erfundene Zwischenebene „Einstellungen“. Navigation und Karte dürfen
  ebenfalls „Persönliche Daten“ heißen; „Profil“, „Persönliches Profil“ und
  zusätzliche „Einstellungen“-Überschriften nicht parallel verwenden.
- Desktopnavigation: Portal mit Startseite und Benachrichtigungen; **Mein Konto**
  als sichtbare Gruppenüberschrift über Persönliche Daten, Sicherheit sowie Geräte
  und Sitzungen. Schmal fährt dieselbe Desktop-Seitenleiste mit dunklem
  Seitenhintergrund über den Content zwischen sichtbarem Header und Footer.
  Keine Einfügung in den Seitenfluss und kein zusätzlicher Schließen-Bereich.
  Der Menüknopf im Header öffnet/schließt; Auswahl und Escape schließen ebenfalls;
  Seitenkopf und Kartentitel orientieren nach dem Schließen. Zahnrad führt zu Persönliche Daten, der Zugang zur
  vollständigen Inbox zu Benachrichtigungen; die bestehende Glocken-Vorschau erhalten.
- Identitätskarte behalten. Gewählter Anwesenheitsstatus erscheint mit Text und
  passender Statusfarbe und wird in Header, Karte und Benutzermenü konsistent
  dargestellt. Anwesenheit und Kontofreigabe nicht miteinander verwechseln.
- Gelbe Kennzeichnung „Schreibgeschützt“ behalten. Den allgemeinen Aktualitätshinweis
  unter der gesperrten E-Mail entfernen; nötige Hinweise zur Adressänderung gehören
  zum vorhandenen Sicherheitsablauf. Footer bleibt dezent, Copyright zentriert.
- C-Farben, Kartenabgrenzung und eingerückten Rahmen erhalten. Dasselbe Szenario
  mit zwei umschaltbaren Hintergründen zeigen: dezente Stitch-Atmosphäre sowie
  vorhandener CSS-Kosmos, an die dunkle Palette angepasst. Nur der Hintergrund
  wechselt, nicht Inhalt, Abstände oder Karten. Keine dritte Designrichtung.
- Das Blumenzeichen aus dem Entwurf als bevorzugte Logorichtung festhalten;
  die spätere Übertragung ist noch keine Änderung des gemeinsamen Anwendungslogos.
- Kurze und sehr lange erfundene Namen/E-Mail-Adressen umschaltbar zeigen.
  Headername bekommt begrenzten Platz; schmal darf der Avatar allein den Zugang
  bilden. In der Identitätskarte sind begrenzter Umbruch/Kürzung erlaubt, ohne
  die Seitennavigation zu verbreitern. Vollständige Werte bleiben im Profil und
  auch per Tastatur/Touch erreichbar; kein ausschließliches Hover-Tooltip-Konzept.

### Weiterhin geltende Gestaltungsziele

- **Atmosphäre außen, Klarheit innen:** Weltraum sichtbar lassen, Text und Formulare
  auf ruhigeren, ausreichend deckenden Flächen darstellen. Keine Sternen-/Neonoptik
  an jedem Control und keine Dekoration, die einen Zustand vortäuscht.
- **Wenige nachvollziehbare Ebenen:** Hintergrund, Arbeitsfläche und erforderliche
  erhöhte Ebene unterscheiden. Zusätzliche Farbe braucht einen Zweck; Abstand und
  Typografie dürfen Gruppierung übernehmen. Semantische Rollen statt einer neuen
  Farbe pro Panel sind auch in etablierten Designsystemen üblich.
  [Carbon: Farbe und Ebenen](https://www.carbondesignsystem.com/building-blocks/foundations/color/overview)
- **Klare Akzentrollen:** eine leitende blaue Akzentfamilie; Fehler, Warnung, Erfolg
  und Fokus bleiben eindeutig. Sehr dunkle Flächen dürfen aktive Controls,
  Grenzen und Texte nicht ununterscheidbar machen.
- **Lesbare Hierarchie:** Seitentitel, Abschnitt, Label, Inhalt und Hilfe bekommen
  erkennbare Rollen. Lokales Inter zunächst behalten, um nicht gleichzeitig eine
  neue Schrift als weitere Vergleichsvariable einzuführen.
- **Bedienung vor Zierrat:** Hauptaktion und aktuelle Position schnell erkennbar;
  Sekundär- und gefährliche Aktionen unterscheidbar. Progressive Offenlegung darf
  seltene Details ordnen, aber erforderliche Hinweise nicht verstecken.
- **Ruhe ist kein leerer Bildschirm:** Charakter entsteht aus Proportionen,
  Materialwirkung und bewusst gesetzten Akzenten. Weniger konkurrierende Information
  unterstützt Aufgaben, ohne einen strikt minimalistischen Stil zu verlangen.
  [NN/g: heuristische Evaluation](https://www.nngroup.com/articles/how-to-conduct-a-heuristic-evaluation/)

Konkrete Farben und Maße werden erst anhand der Varianten entschieden und später
in den zuständigen CSS-Quellen gepflegt. Der Plan eröffnet keine zweite verbindliche
Tokenliste neben `docs/theme.md` und den Stylesheets.

## Arbeitspakete und Entscheidungspunkte

Pakete 1 und 2 sind fertig; die menschliche Richtungsentscheidung ist getroffen.
Die HTML-Arbeit aus 3a–3c ist einschließlich der gezielten Verfeinerungen
durchgeführt. Angemeldeter Bereich und Besucherbereich sind als feste
Gestaltungsreferenzen abgenommen. Als Nächstes wird die Administration als
zusätzliche HTML-Referenz mit Stitch gestaltet und gemeinsam beurteilt.
Danach sind Umfang und Reihenfolge des Vaadin-Piloten festzulegen.
Vaadin-Integration und Theme-Übernahme bleiben nachgelagert; die Anwendungsoberfläche
wurde noch nicht verändert.

### Gestufter Kern und optionale Vertiefungen

Die fünf Pakete und die ausführlichen Überlegungen bleiben erhalten. Für den
weiteren Versuch gilt nach der Richtungswahl der folgende Kern:

| Paket | Kern des ersten Versuchs | Optional später / Nice to have |
| --- | --- | --- |
| 1 | Profil am Desktop und schmal aufnehmen, Bestandskritik, bereinigte HTML-Vorschau und kurzen Stitch-Übergabetest vorbereiten/durchführen | Weitere Seiten und Sprachvarianten aufnehmen, Export über den Profilfall hinaus ausbauen, Automatisierung vertiefen |
| 2 | Drei vergleichbare Designrichtungen vorstellen und eine auswählen | Zusätzliche Varianten oder weitere Screens erzeugen |
| 3a | Profilreferenz mit vereinbartem Seitenkopf verfeinern; lange Namen und zwei Hintergründe vergleichen | Zusätzliche rein ästhetische Varianten |
| 3b | Aufnahme-/Stitch-Kreislauf an Sicherheit und E-Mail-Dialog nachweisen; anschließend übrige angemeldete Benutzerbereiche und Overlays aufnehmen/gestalten | Allgemeiner Exporter für beliebige Vaadin-Anwendungen, zusätzliche Stitch-Anbindung |
| 3c | Ergebnisse in einem lokal klickbaren, zusammenhängenden HTML-Portal mit gemeinsamen Bausteinen beurteilen; Besucheransichten sind abgenommen, Administration wird als nächste Erweiterung ergänzt | Vollständige EN-Ausarbeitung |
| 3d | Nach positiver HTML-Bewertung einen begrenzten Vaadin-Piloten mit Profil, Dialog und Fehlerzustand erproben | Weitere echte Anwendungsansichten erst nach vereinbarter Übernahmereichweite |
| 4 | Kein Pflichtschritt vor der HTML-Bewertung | Vertiefte Komponenten-/Vaadin-Transferprobe einschließlich Administration und zusätzlicher Zustände |
| 5 | Nach HTML-Vorschau und gegebenenfalls Vaadin-Pilot über Fortführung, Übernahme oder Verwerfen entscheiden | Bei Bedarf nach einer Vertiefung erneut entscheiden |

Die Reihenfolge ist **1 → 2 → 3a → 3b → 3c → gemeinsame Entscheidung → 3d/5**.
3a–3c sind für Benutzerportal und Besucherbereich ausgeführt und gestalterisch
als Referenzen abgenommen; die ergänzende Administration steht noch aus.
Die Entscheidung über Umfang und Freigabe der technischen Übertragung
bleibt offen. Gemeinsame
Quellen für Header, Footer, Navigation, Palette und wiederkehrende Overlays
vermeiden unabhängige Kopien je Seite. Paket 4 und die anderen optionalen
Vertiefungen werden nur bei erkennbarem Nutzen und mit einem passenden Folgeauftrag
aufgenommen. Eine Freigabe des kleinen Versuchs umfasst diese Erweiterungen nicht
automatisch. Sie müssen auch nicht erledigt sein, um den ersten Versuch abzunehmen
oder zu verwerfen. Übernahme in den gemeinsamen Theme-Standard bleibt gesondert abzustimmen.

Optional ist der zusätzliche Umfang, nicht die Qualität bereits geänderter Bereiche:
Isolation, Datenschutz, bestehende Fachverträge, lesbare Darstellung, Fokus,
schmale Bedienbarkeit und erforderliche gezielte Regressionen bleiben verbindlich.

### Paket 1 – Kleinen Ausgangspunkt aufnehmen und Stitch-Weg erproben

**Stand: abgeschlossen für den vereinbarten Referenzumfang.** Bestandskritik,
Desktop-/schmale Profilaufnahmen und der funktionierende Stitch-Übergabeweg sind
vorhanden. Die mobile HTML wurde noch nicht zusätzlich in Stitch importiert.
Die Designentwürfe und ihre Bewertung stehen im Abschnitt zu Paket 2.

Der verwendete Weg ist **SingleFile → Bereinigung → CLI `upload screen`**.
Installiert ist `@google/stitch` in Version `0.11.1`; die vorhandene OAuth-/Canvas-
Anmeldung ließ sich automatisch erneuern. Neue Installation oder Anmeldung war
nicht erforderlich. Projektanlage, Screen-Upload und unabhängiger anschließender
Abruf wurden tatsächlich ausgeführt. Der Webdialog hatte die vorige HTML dagegen
als Codeanhang übernommen; die danach versuchte Generierung scheiterte. Eine
allgemeine HTML-Unverträglichkeit oder ein Größenlimit ist damit nicht nachgewiesen.
Der bisherige private Entwurf „Cosmos UI Design Review“ blieb unverändert.

Aktueller externer Arbeitsstand:

- Privates Projekt: [Workflow testen – Cosmos](https://stitch.withgoogle.com/projects/14011270174485926515),
  ID `14011270174485926515`.
- Referenzscreen: „Account – Ausgangsreferenz“, ID `5775520971778872726`.
  Der unabhängige CLI-Abruf bestätigt Titel und Abmessungen `2560×1600`.
- Nach Neuladen im Browser war die gerenderte Account-Seite mit Kopfbereich,
  kosmischem Hintergrund, Identitätsbereich und Profilfeldern sichtbar. Keine
  Designverbesserung oder Funktionsprüfung; es handelt sich um eine statische Vorlage.
- Die CLI meldete zusätzlich drei bereinigte mögliche Geheimnis-/Tokenwerte.
  Das ist eine Meldung ihres Scrubbers, kein Nachweis dreier echter Zugangsdaten.
  Die lokale Quelldatei blieb laut SHA256 unverändert.

Benutzerseitige SingleFile-Vorlage und lokale Belege unter
`target/designkritik-stitch/user-reference/`:

- `account-bereinigt.html`: erfundene Profilwerte und eingebettete Gestaltung;
  ursprüngliche CSRF-Metadaten, lokale Serverziele und Skripte entfernt.
  SHA256: `A2F5D389C76549C7A1B38FE064A49F05D84F741CABB1E986F7B88BA157DE4FA6`.
- `account-vorschau.jpg` und `account-formular.jpg`: bereinigte HTML bei `1280×720`
  lokal visuell geprüft, einschließlich gescrolltem Formular. Der nur für diese
  Datei gestartete lokale Vorschau-Server wurde anschließend beendet.
- `stitch-workflow-import.jpg`: nach Neuladen sichtbarer Referenzscreen in Stitch.
- `stitch-import.json`: Zuordnung von Projekt, Screen, Quelldatei und Prüfgrenzen.

Aktuelle HTML-Übergabe unter
`target/designkritik-stitch/html-reference-20261023-112909/`:

- `stitch-handoff/profile-desktop.html` und `profile-mobile.html`: eigenständige
  statische Referenzen für `1440×1000` und `390×844`.
- `stitch-handoff/uebergabe.txt`: konkreter Auftrag für einen Referenzimport ohne
  Designvarianten; `manifest.json`: Revision, Browser, Ansichtsgrößen und Prüfgrenzen.
- `stitch-profil-html.zip`: nur diese zwei HTML-Dateien, Übergabetext und Manifest.
- `preview-desktop.png` und `preview-mobile.png`: lokale Darstellung der HTML-Dateien
  nach Abschalten der Testanwendung. Beide Dateien wurden mit den erwarteten
  Profilwerten und geladenem Logo ohne externe HTTP-Aufrufe geprüft; die Desktop-
  Darstellung wurde zusätzlich visuell mit der ursprünglichen Aufnahme verglichen.
- `stitch-import-result.jpg`: sichtbarer Stitch-Fehler beim Referenzauftrag mit
  importierter HTML-Datei im eigenen Projekt; kein erfolgreicher Designscreen.

Die vorherige reine Bildübergabe ist durch diese HTML-Übergabe ersetzt. Die folgenden
ursprünglichen Profilaufnahmen bleiben Vergleichsmaterial.

Lokale Belege unter
`target/designkritik-stitch/reference-20261023-110804/`:

- `profile-desktop.png`: Ausgangsansicht `1440×1000`.
- `profile-mobile.png` und `profile-mobile-form.png`: Anfang und gescrollter
  Formularbereich derselben Seite bei `390×844`; kein künstlich zusammengesetztes Bild.
- `stitch-handoff/`: ausschließlich diese drei Bilder, `uebergabe.txt` und
  `manifest.json`; zusätzlich als `stitch-profil-referenz.zip` gepackt. Keine Logs,
  Zertifikate, Datenbanken, Kennwörter oder Sitzungsdaten in dieser Übergabe.
- Das Manifest ordnet Revision, Browser, Ansichtsgrößen und das JAR eindeutig zu.
  JAR-SHA256: `6BAD2442F3ECAF429FB58695320C5949806617B41C1109189FE59E6D35EE3D0D`.

Die Referenz verwendet einen frischen nativen JAR-/Caddy-/H2-Aufbau, da die
Docker-Linux-Engine beim Startversuch nicht erreichbar war. Das normale Testkonto
wurde über die vorhandene Bootstrap-Konfiguration angelegt, nicht aus Entwicklerdaten
übernommen. Es wurden keine Mailaktionen ausgeführt; als SMTP-Ziel war ausschließlich
ein eigens reservierter Loopbackport konfiguriert. Bestehendes Windows-Testvertrauen
wurde nur gelesen, HTTPS ohne Fehlerausnahmen geprüft. Eigene App-/Proxyprozesse,
Datenbank und temporäre private Serverschlüssel wurden nach der Aufnahme abgebaut.

Ausgeführt: Java-25-Prüfung, Reactor-Paketierung für App und Browserhilfen mit
übersprungenen Tests, zentrale Spotless-/Checkstyle-Prüfungen des temporären
Aufnahmehelfers sowie gezielte Anmeldung und Profilaufnahmen in Chromium. Keine
JUnit-Suite, keine große Browserrunde und kein Sonar-Scan. Die spätere sichtbare
Vaadin-Spielwiese wurde noch nicht implementiert.

**Kern des ersten Versuchs:**

- Eigene frische Testumgebung mit erfundenem normalen Benutzer vorbereiten;
  bestehende Entwicklerdaten, Prozesse und echte Mailziele bleiben unberührt.
  Vorhandenen HTTPS-/Health-Vertrag und eingerichtetes Testvertrauen verwenden;
  keine TLS-Ausnahmen. Nur eigene Testdienste starten und anschließend abbauen.
- Nach ausdrücklicher Freigabe des isolierten Starts und der Browseraufnahmen:
  Konto/Profil einschließlich Appkopf am Desktop und in einer schmalen Ansicht
  mit denselben realistischen Beispieldaten zunächst auf Deutsch erfassen.
  Revisionsstand, Sprache, Viewport und Szenario eindeutig zuordnen.
- Bestandskritik mit wenigen priorisierten Befunden und erhaltenswerten Merkmalen
  im aktuellen Plan konkretisieren; keine parallele Berichtsserie anlegen.
- Stitch-Anbindung mit dem Benutzer einrichten, zunächst lesend prüfen. Ein eigenes
  Designprojekt und einen bereinigten Referenzscreen für einen kurzen Übergabetest
  verwenden. Falls die Anbindung nicht zeitnah trägt, den vorbereiteten manuellen
  HTML-/Prompt-Weg verwenden, damit erste Entwürfe nicht an der Integration warten.
  Dokumentierte Funktionen von tatsächlich getesteten unterscheiden; verwendete
  automatisierte Änderungen weiterhin durch erneuten Abruf kontrollieren.

**Optional später / Nice to have:**

- Sicherheit, Sitzungen, Inbox, ein Dialog sowie Home/Appkopf zusätzlich erfassen;
  längere DE/EN-Texte und weitere Ansichtsgrößen als Referenz ergänzen.
- Den HTML-Export bei Bedarf auf zusätzliche Ansichten und Zustände erweitern;
  die aktuelle Profilvorschau bleibt eine bewusst begrenzte Referenz.
- Den automatisierten Ablauf für Import, Generierung, gezielte Änderung, erneuten
  Abruf und Download vertiefen, soweit die wiederholte Nutzung den Aufwand rechtfertigt.

**Ergebnis:** nachvollziehbare Ausgangsansichten, konkrete Kritik, abgegrenztes
Verbesserungsbriefing und ein funktionierender Übergabeweg. Fehlende Verbindung
blockiert nicht die Vorbereitung des Briefings.

### Paket 2 – Drei vergleichbare Richtungen in Stitch

**Aktiver Vergleichsstand im privaten Projekt „Workflow testen – Cosmos“:**

| Richtung | Maßgeblicher bereinigter Screen | Einschätzung und Grenzen |
| --- | --- | --- |
| A – Ruhiger Cosmos | `7790207111094436567`, „A – Ruhiger Cosmos · Vergleich“ | Vertraute horizontale Anordnung, kompaktere Identität und Formular im ersten Desktop-Viewport. Zwei Navigationsebenen und mehrere Kopf-/Paneltitel bleiben; kleine Hilfstexte und Feldgrenzen verdienen weitere Prüfung |
| B – Klarer Kontobereich | `12192353632977007112`, „B – Klarer Kontobereich · Vergleich“ | Klarste Gruppierung der Kontobereiche links, ruhige zusammenhängende Formularfläche. Zusätzliche Navigationsbreite und recht dunkle Trennlinien; kosmischer Charakter deutlich zurückgenommen |
| C – Persönlicher Arbeitsbereich | `11377747199826892012`, „C – Persönlicher Arbeitsbereich · Vergleich“ | Identität und Formular stehen als zwei klar erkennbare Bereiche nebeneinander. Identitätskarte beansprucht weiterhin viel Raum, horizontale Navigation bleibt doppelt; in schmaler Ansicht noch nicht geprüft |

Alle drei Richtungen verwenden sehr dunkle bläuliche Grundflächen, helle Texte,
gedämpfte sekundäre Beschriftungen und blaue beziehungsweise cyanblaue Hauptakzente.
Die kleinen grünen Anzeigen dienen Status/Konto. Rahmen und aktive Flächen trennen
Bereiche statt vieler bunter Karten. Das ist ein erster Vergleich, noch kein
abgenommener Farb-/Kontraststandard oder Nachweis einer exakt auf fünf Farben
reduzierten Palette. **Gemeinsame Schwäche:** Stitch hat den ursprünglichen
Weltraum-/Planetencharakter zu stark zurückgenommen; für eine Weiterführung den
atmosphärischen Hintergrund ausdrücklich mit der ursprünglichen Referenz vergleichen.

**Benutzerentscheidung für die Weiterführung:**

| Bereich | Gewählte Grundlage / Präzisierung |
| --- | --- |
| Header | C: eingerückter, klar begrenzter Kopfbereich; Einrückungen und Proportionen erhalten |
| Footer | C mit Copyright; Copyrighttext horizontal im Footer zentrieren |
| Farben | C ist der bevorzugte Ausgangspunkt für Grundflächen, Texte und Akzente; keine Übernahme der B-Palette als Nebenwirkung des B-Layouts |
| Content und Navigation | B: seitliche Kontonavigation und die ruhige zusammenhängende Formularfläche |
| Navigationsebenen | Keine zwei horizontalen Tabreihen wie in C; die Ziele über die gruppierte Seitennavigation aus B anbieten |
| A | Am nächsten am Original, vom Benutzer aber als schwächste Variante bewertet; nicht weiterverfolgen |

Maßgebliche Referenzen bleiben C `11377747199826892012` für Rahmen/Farben und
B `12192353632977007112` für Content/Navigation. Diese Kombination ist die gewählte
Versuchsrichtung, noch keine Abnahme eines fertigen kombinierten Screens oder
Freigabe für eine globale Theme-Übernahme. Die atmosphärische Hintergrundgestaltung
bleibt Bestandteil des vereinbarten Kosmos-Ziels.

**Nachweise und Arbeitsweg:**

- Die drei Stitch-Generierungen lieferten zunächst A `1679b0bb7f7c466d913b2eebfd109280`,
  B `04a768ba9fd8448f9bfa395b12a1d01f`, C `77712c7881974f08a7e5798a4be461dc`.
  Sie sind Rohentwürfe, nicht der maßgebliche Vergleichsstand.
- Erfundene Supportverträge, System-/Sitzungsangaben, Rollen-/Mandanteninformationen,
  Zusatzaktionen und Fremdsprachen wurden nicht als neue Fachverträge übernommen.
  Die separat erzeugten Stitch-Korrekturen A `2a0b4c15e4dd4fa8bb998415f554d031` und
  B `1559957313fb47958efac402f8af3510` sind ungeeignet: Sie ließen Geburtsdatum und
  Sprachauswahl weg. Keine weitere solche Korrekturrunde und keine Löschung externer
  Entwürfe; die ursprünglichen Layoutideen wurden im lokalen HTML erhalten.
- In den Exporten wurden die erfundenen Angaben entfernt, nur Deutsch/English
  belassen, E-Mail als readonly und „Verfügbar“ ausdrücklich sichtbar erhalten.
  Bei C wurden weiße/schlecht lesbare Eingaben dunkel korrigiert. Die Hauptaktion
  ist rechts angeordnet. Kein echter Speichervorgang und keine zweite Fachlogik.
- Die vorhandenen externen Tailwind-/Schriftverweise wurden vor der lokalen Vorschau
  geprüft; nur deklarative Farb-/Schriftkonfiguration und der bekannte Tailwind-CDN-
  Compiler wurden dafür zugelassen, Eventhandler entfernt. Anschließend wurden die
  erzeugten Styles und die vorhandene lokale Inter-Schrift eingebettet und sämtliche
  Skripte/externen Styles entfernt. Der finale Vergleich ist statisch und offline nutzbar.
- Alle finalen lokalen Ansichten wurden nach Neuladen bei `1440×1000` geprüft:
  je vier Eingaben und eine Sprachauswahl, die Ausgangswerte, readonly E-Mail,
  Profilbild-/Speicheraktion, Status und die bestehenden Navigationsziele erhalten.
  Keine Skripte oder externen Styles in den finalen Dateien. Lokale Python-Helfer
  wurden mit Projekt-Ruff formatiert und die Formatierung erfolgreich geprüft.
- Die bereinigten HTML-Dateien wurden als oben benannte Vergleichsscreens erneut
  hochgeladen und unabhängig per CLI abgerufen. Stitch meldet native Bildgrößen
  A `2560×2000`, B `2880×2000`, C `2560×1600`; diese sind kein Beleg gleicher
  CSS-Viewports. Der gleich große lokale Vergleich ist dafür maßgeblich.
- Lokale Dateien unter `target/designkritik-stitch/variants/`: `preview/vergleich.html`
  mit `original.html`, `a.html`, `b.html`, `c.html`; gemeinsam als
  `cosmos-designvergleich.zip`. `original-vergleich.jpg`, `a-vergleich.jpg`,
  `b-vergleich.jpg`, `c-vergleich.jpg` zeigen denselben Vergleichsrahmen.
  Rohantworten und temporäre Helfer bleiben ignorierte Arbeitsdaten. Ein Root-`clean`
  entfernt diese Dateien; die extern gespeicherten Vergleichsscreens bleiben abrufbar.

Nicht geprüft: echtes Speichern, Sicherheit/Sitzungen, Tastatur-/Screenreader-Verhalten,
Kontrastmessung, Smartphone und EN-Layout. Es wurden keine Anwendungstests oder
Builds für Paket 2 gestartet; `docs/status.md` bleibt die letzte große QA.
Die Richtungswahl ist getroffen. Mit Folgeauftrag die gewählte Kombination in
Paket 3 zunächst sichtbar zusammensetzen, gemeinsam prüfen und in Vaadin erleben.

Gemeinsames Briefing: Deep Midnight Blue mit kühlen blaugrauen Flächen und blauen
Akzenten, keine violette Hauptpalette. Ungefähr fünf begründete Grundflächen,
kosmischer Hintergrund erhalten, weniger Glows und wiederholte Titel. Alle
Richtungen erhalten dieselben erfundenen Angaben „Design Stitch“, die fünf
Profilfelder, schreibgeschützte E-Mail, Sprachauswahl, Profilbild-/Speicheraktion
sowie die bestehenden Konto-/Benachrichtigungsziele. Angeforderter Vergleichsviewport
ist `1440×1000`; tatsächliche Ausgabegrößen werden nach Abruf geprüft. Jede Richtung
wird einzeln aus Screen `5775520971778872726` erzeugt, damit die Layoutaufträge
eindeutig bleiben. Gemeinsames Briefing und Richtungsaufträge liegen vorläufig unter
`target/designkritik-stitch/variants/`; Screens und kritische Abweichungen werden
im aktuellen Plan zugeordnet. Noch keine Dialog-/Fehler- oder mobilen Zusatzscreens.

Für denselben Konto-/Profilinhalt drei gezielt verschiedene Entwürfe erzeugen.
Sprache, Datenmenge und Viewport bleiben gleich; die im gewählten Umfang enthaltenen
Felder und Aktionen werden nicht weggelassen. Weitere Kontobereiche müssen im
ersten Versuch nur als Orientierung erkennbar bleiben, nicht als ausgearbeitete Screens.
Jede Variante zeigt ein zusammenhängendes Farbkonzept mit Rollen für Hintergrund,
Arbeitsfläche, Text und Akzente; daraus entsteht noch kein neuer verbindlicher Theme-Standard.

| Richtung | Zu untersuchende Veränderung |
| --- | --- |
| A – Ruhiger Cosmos | Bestehende Anordnung wiedererkennbar, weniger Flächenwechsel/Glows, kompaktere Kopfbereiche |
| B – Klarer Kontobereich | Seitliche, eindeutig gruppierte Navigation am Desktop und eine zusammenhängende Arbeitsfläche; URLs und Bereiche erhalten |
| C – Persönlicher Arbeitsbereich | Kompakte Identitätszone mit Avatar/Status neben dem Hauptinhalt, stärker über Typografie und Abstand gegliedert |

Alle Varianten bleiben dunkel und behalten den kosmischen Bezug. B und C sind
Layout-Hypothesen, keine bereits beschlossenen Umbauten. Keine erfundenen Statistiken,
Social-Feeds, Bezahlinhalte oder zusätzlichen Funktionen einführen.

Codex prüft zunächst Vollständigkeit und erkennbare Bedienprobleme. Der Benutzer
bekommt Ausgangsstand und A/B/C in vergleichbarer Darstellung, dazu je wenige
konkrete Vor-/Nachteile. Er wählt eine Richtung, eine begründete Kombination oder
verwirft alle. **Hier liegt der erste bewusste Designentscheid.**

### Paket 3 – Zusammenhängendes HTML-Portal, danach Vaadin-Pilot

Die HTML-Umsetzung aus 3a–3c einschließlich geöffneter Sprach-, Status-, Kalender-,
Benachrichtigungs- und Kontodialoge ist als Gestaltungsreferenz abgenommen.
Die fünf Hauptszenen sind Startseite, Benachrichtigungen, Persönliche Daten,
Sicherheit sowie Geräte und Sitzungen. Home verwendet das abgestimmte
Willkommensmotiv als Inhaltsplatzhalter ohne erfundene Dashboard-Funktionen.
In der schmalen Ansicht ist die Navigation aufklappbar. Kein Administrationsbereich
für den normalen Beispielbenutzer.

#### 3a – Eine verbindliche Entwurfsreferenz verfeinern

Die oben vereinbarten Änderungen am bestehenden C/B-Entwurf umsetzen, mit langen
erfundenen Namen und zwei Hintergründen zeigen. Header/Footer, Seitenkopf,
Navigation, Karten, Controls und Typografie gemeinsam prüfen. Das Ergebnis als
neue eindeutig bezeichnete Referenz im bestehenden Stitch-Versuchsprojekt sichern;
noch nicht als gültigen Anwendungstheme-Standard ausgeben. Diese Referenz bleibt
für alle folgenden Seiten gleich. Keine weiteren unabhängigen A/B/C-Reihen.

**Erste Runde zur gemeinsamen Bewertung bereit:**

- Originale Sprachliste und Benutzermenü/Statusliste wurden mit erfundenem Konto
  in einer eigenen HTTPS-JAR-/Caddy-/H2-Umgebung aufgenommen. JAR-Hash entspricht
  der Profilaufnahme; kein neuer Anwendungsbuild. Java 25 wurde geprüft.
  Browserbedienung und sichtbare DOM-Aufnahme erfolgten über den Codex-Browser.
  Die HTML-Vorlagen enthalten aufgelöste Shadow-/Slot-Inhalte, eingebettete Assets
  und keine Skripte, Serverziele oder Sicherheitswerte. Die ausgeschnittenen
  Overlay-Referenzen vereinfachen die Positionierung und zwei Schrifticons;
  Originalaufnahmen bleiben zusätzlich erhalten. Kein allgemeiner Exporter.
- Originalscreens: Sprache `17228422313398362564`, Benutzermenü/Status
  `10844645901642133007`. Zusammen mit der C/B-Vorschau an Stitch übergeben.
  Eine gezielte REFINE-Generierung lieferte
  `1c9ce322af294971a8a39c7c4438d923`, **„Cosmos C+B – Persönliche Daten mit
  geöffneten Overlays“**. Seite, geöffnete Sprachliste und Statusoptionen stammen
  aus diesem Entwurf. Stitch schlägt die vier Statusoptionen direkt im
  Benutzermenü vor, statt eines weiteren verschachtelten Auswahlmenüs; diese
  Anordnung steht ausdrücklich zur gemeinsamen Bewertung.
- Lokal wurden C-Farben/Einrückungen erhalten, Kopf/Navigation/Titel vereinheitlicht,
  das Profilbadge und der allgemeine Hinweis unter E-Mail entfernt. Der gelbe
  Schreibgeschützt-Hinweis bleibt. Geprüfte eigene Vorschau-Interaktionen ersetzen
  die fremden Ereignisskripte; keine fachlichen Dienste. Kurze/lange Namen sowie
  beide statischen Hintergründe sind außerhalb des Produktbereichs umschaltbar.
- In Stitch gesicherte Screens der ersten Referenzrunde im bestehenden Projekt:
  `6286106089878372220` **„Referenzrunde 1 · geprüft – Persönliche Daten“**,
  `1467870673497310996` **„Referenzrunde 1 · geprüft – Sprache geöffnet“**,
  `5370950163606447208` **„Referenzrunde 1 · geprüft – Status geöffnet“**.
  Alle drei wurden unabhängig per CLI abgerufen. Frühere Zwischenstände bleiben
  erhalten; nur die mit „· geprüft“ bezeichneten Screens sind diese Referenzrunde.
- Lokale Dateien unter `target/designkritik-stitch/refinement-01/preview/`:
  `index.html` mit Vergleichssteuerung, `referenz.html`, `sprache-offen.html`,
  `status-offen.html`, `kalender-offen.html` und `Hinweise.txt`; gemeinsam als
  `target/designkritik-stitch/refinement-01/cosmos-referenzrunde-1.zip`.
  Keine externen Skripte/Styles/Schriften nötig; ein geprüftes lokales Skript für
  die Demo. SHA256 von `referenz.html`:
  `9BA37459C29F570A7AC1205AE9BDC531074CE9FD2C89E9848138ED00C00EA39F`.
  Dieser lokale Stand enthält die mobile Navigationskorrektur sowie die vereinheitlichte
  Palette, Avatare und den Kalender. Aktuelle skriptfreie DOM-Aufnahmen sind in Stitch
  als `6943287812131813050` **„Arbeitsreferenz geprüft – feste Flächenpalette und Avatare“**
  und `8911068278616302149` **„Arbeitsreferenz geprüft – Kalender geöffnet, feste Flächenpalette“**
  gespeichert und unabhängig per CLI mit Titel, HTML- und Screenshotartefakt abgerufen.
  Die älteren Screens bleiben erhalten; für den Folgeauftrag diese Arbeitsreferenzen
  und die neueste lokale Vorschau verwenden. Die interaktive Vorschau und ihre
  gemeinsam gepflegten Quellen liegen ausschließlich lokal unter `target/`.
- Tatsächlich geprüft: `1440×1000` und `390×844`, alle fünf Profilfelder,
  kein horizontaler Seitenüberlauf, begrenzte lange Namen in Header/Sidebar/Menü,
  beide Hintergründe, Sprachwahl mit sichtbarer Auswahlmarkierung, Pfeil-/Home-/End-/
  Enter-/Escape-Bedienung und Fokusrückkehr. Vier Statusoptionen schalten lokal
  die Markierung und passende Avatar-/Kartenfarbe um. Schmale Navigation und
  Popover bleiben erreichbar; Copyright am Desktop mittig. Eigene Helfer wurden
  formatiert/geprüft (Java Spotless/Checkstyle, Python Ruff), JavaScript-Syntax und
  ZIP-Integrität geprüft. Keine JUnit-/Browserjourney-Suite und keine große QA.
- Eigene Referenzanwendung/Proxy beendet, eigene Datenbank und private TLS-Dateien
  entfernt; Windows-Vertrauen unverändert. Der reine lokale Vorschau-Server darf
  während der gemeinsamen Sichtung das ausschließlich synthetische HTML-Paket
  bereitstellen. Kein Zugriff auf Entwicklerdaten oder echter Mailversand.

Die geöffnete Sprachliste und das Benutzermenü gefallen dem Benutzer. Die
konkret beauftragte Verfeinerung ist lokal umgesetzt und als Teil der Referenz abgenommen:

- Sprachliste mit 6 px Abstand zum Auslöser und einheitlicher Innenkante.
- Alle drei Avatare verwenden denselben neutralen Rahmen; Anwesenheit bleibt als
  farbiger Badge erkennbar. Karte und Benutzermenü zeigen dieselbe Statusauswahl.
- Der Avatar der Identitätskarte öffnet zusätzlich die vier vorhandenen Statusoptionen,
  auch in der mobilen Navigation. Auswählen synchronisiert beide Zugänge und die
  Statusdarstellung; Escape führt zum Auslöser zurück.
- Stitch lieferte eine schriftliche fachliche Bewertung: hellere Overlays unterstützen
  die räumliche Trennung; zusätzliche gesättigte Blautöne und verschiedenfarbige
  Popup-Sektionen sind unnötig. Dies ist ausdrücklich eine **nachträgliche Bewertung**,
  kein Nachweis seiner ursprünglichen Absicht. Behauptete Bestandsfarbduplikate aus
  der Antwort wurden nicht ungeprüft als Code-Inventar übernommen.
- Der Kalender stammt aus Stitch-Screen `a807a6ca902d4cf186ae7c89ad85959f`.
  Sein Kalenderfragment wurde ohne fremde Skripte übernommen und lokal bedienbar
  gemacht: Tag/Monat/Jahr, Heute, Abbrechen, Pfeiltasten und Escape. Der zusätzlich
  vorgeschlagene Übernehmen-Knopf entfällt, weil die Datumsauswahl direkt übernimmt.
  Das ist eine HTML-Gestaltungsvorlage, keine nachgewiesene Vaadin-Integration.
  Der echte geöffnete Vaadin-Kalender wurde für diese Runde nicht separat aufgenommen;
  aktueller DatePicker und CompactDateInput wurden im Quellcode abgeglichen. Keine
  neuen Datumsgrenzen oder fachlichen Validierungsregeln ergänzt.

**Gemeinsame Arbeitsregeln für Flächenfarben:**

| Rolle | Arbeitswert | Verwendung |
| --- | --- | --- |
| Seitenhintergrund | `#050811` | Seite, Header/Footer, mobile Navigationsgrundfläche |
| Panel | `#080E1B` | Content- und Identitätskarten |
| Overlay | `#0E1726` | Benutzermenü samt Kopf/Fuß, Sprach-/Statusliste und Kalender |
| Eingabe | `#091120` | Alle fünf Profilfelder und Avatargrundfläche |
| Hover | `#152238` | Interaktive Menü-/Navigations-/Kalendereinträge |
| Auswahl | `#122342` | Aktive Einträge mit gemeinsamer cyanblauer Kontur |

Vier deckende Basisflächen plus zwei Zustände, keine neuen Panelwerte pro Seite.
Stitchs optionale Alpha-Varianten wurden bewusst nicht übernommen. Deko, Konturen,
Schrift- und semantische Status-/Warn-/Aktionsfarben sind getrennte Rollen und keine
weiteren Panel-Hintergründe. Die Werte gelten für den **HTML-Versuch**, noch nicht
als neuer globaler Vaadin-Theme-Standard. „Verbunden“ bleibt vorerst stehen; der
Stitch-Hintergrund ist bevorzugt, weitere kosmische Dekoration bleibt offen.

Geprüft in der Vorschau: gleiche berechnete Panel-/Input-/Overlayfarben, einheitliche
Avatarrahmen, Statussynchronisation, Combo-Abstand, Datum-/Jahreswahl und Übernahme,
Kalenderpfeile/Escape samt Fokusrückkehr, lange Namen sowie Smartphone `390×844` und
Querformat `844×390` ohne horizontalen Überlauf. Der Kalender bleibt im Querformat
innerhalb des Viewports scrollbar. Stil-/Syntaxprüfungen und Offline-ZIP bestanden;
keine Anwendungstests oder große QA. Die Credits-Nutzung wurde ausschließlich für
diese laufende Referenzrunde ausdrücklich erlaubt. Der Folgeauftrag um 06:00 Uhr
prüft reguläres Kontingent und darf keine Credits verwenden.

Die mobile Navigation zeigt lokal dieselbe Desktop-Seitenleiste über dem Content,
mit dem dunklen Seitenhintergrund statt einer zusätzlichen andersfarbigen Fläche.
Header und Footer bleiben sichtbar. Ein zusätzlicher Navigationstitel und
Schließen-Knopf entfallen; der Menüknopf im Header öffnet und schließt die Ebene.
Geprüft bei `390×844`: Navigation exakt zwischen Headerunterkante und Footeroberkante,
Menüknopf und Escape schließen und geben den Content wieder frei. Bei `844×390`
bleibt die schmale seitliche Navigation scrollbar; Auswahl schließt und führt den
Fokus zum Menüknopf zurück. Kein horizontaler Überlauf in beiden Ansichten. Beim
Wechsel auf `1440×1000` schließt die Ebene und die ursprüngliche Desktopnavigation
kehrt in ihren Layoutbereich zurück. Vergleichssteuerung und Offline-ZIP sind
aktualisiert; keine neue Stitch-Generierung und keine Änderung der eigentlichen
Anwendung.
Diese mobile Navigation ist vom Benutzer gestalterisch bestätigt.

Offen bleiben Geschmack/Feinarbeit, die gemeinsame Wahl des Hintergrunds und die
Freigabe der Referenz für weitere Seiten. Vollständige EN-Oberfläche, echte
Mobilgeräte, Screenreader, Kontrastabnahme und Vaadin-Übertragung sind nicht geprüft.

#### 3b – Referenzen und Stitch-Kreislauf: technisch durchgeführt

- Aktuelle UI-Komponenten und ihre Fachverträge abgeglichen: Sicherheitsfelder und
  Hinweise, aktive/gespeicherte Sitzungen, Profilbildmodi und Ausschnittbedienung,
  Inbox-/Vorschaufilter, Lesestatus, Entfernen, Detailaktionen und Pagination.
- Originale aus einem eigenen frischen HTTPS-/H2-JAR-Aufbau mit normalem erfundenem
  Benutzer aufgenommen. JAR-SHA256 entspricht der Profilreferenz; Java 25 geprüft,
  kein Anwendungsneubuild. CUA-Browserbedienung und reine DOM-/Style-Lesung lösen
  Shadow-/Slot-Inhalte auf. Abschnittsweise Übertragung wurde auf vollständigen
  HTML-Abschluss geprüft. Sicherheitswerte, Passwortinhalte, Fremdskripte und
  Serverziele sind ausgeschlossen; Schriften/Bilder eingebettet. Für die Referenz
  herausgelöste Dialoge behalten ihre Inhalte; leere Passwortfelder sind neutral
  nachgebildet. Sicherheit und E-Mail-Dialog im Offline-Export sichtbar kontrolliert.
- Die Original-Inbox war leer, die echte Sitzungsliste enthielt nur das eigene Gerät.
  Gefüllte Inbox, Nachrichtendetail und weitere Geräte sind ausdrücklich synthetische
  Gestaltungsszenen nach dem aktuellen Codevertrag, keine Aufnahme echter Nutzerdaten.
  Der echte Kalender ist jetzt ebenfalls separat als Original-HTML vorhanden.
- Eigene Anwendungs-/Proxyprozesse sind beendet; eigene Datenbanken, temporäre
  Konfiguration und private Worker-TLS-Dateien entfernt. Bestehendes Windows-Vertrauen
  wurde nur gelesen; kein Import, echter Mailversand oder fremder Dienst als Fixture.

| Bereich | Hochgeladene Originalreferenzen | Neuer Stitch-Entwurf |
| --- | --- | --- |
| Sicherheit / E-Mail-Dialog | `3832466833126774545`, `9998790273924165944` | `ac4f0421af9f462b853594b54d4b4990` |
| Inbox / Glockenvorschau / Detail | `14393985130048207485`, `14376026531723469630` | `aad2b75d163442bab1d7463a3a36ea34` |
| Startseite / Sitzungen / Passwort / Profilbild | `2708411696220003083`, `1102108285391529627`, `304663314998354812`, `3712410914537252687` | `5baae01ff18e46099936767236264f6a` |

Alle drei neuen Entwürfe wurden unabhängig per CLI abgerufen. HTML kam je nach
Artefaktzugang über den Downloadlink oder den tatsächlich sichtbaren authentifizierten
Stitch-Vorschauframe; Browserzugang umgeht keine Schutzprüfung. Inhalte und sichtbare
Vorlagen wurden kontrolliert; Inbox-/Benutzerbereiche-Bilder liegen zusätzlich lokal.
Stitch blieb teilweise im Textmodus bzw. lieferte eine falsche Profilseite. Erst
gezielte Browser-Chat-Korrekturen erzeugten die passenden HTML-Szenen. Der Kreislauf
funktioniert damit mit kontrollierten Eingriffen, nicht als garantierte unbeaufsichtigte
Ein-Klick-Automatisierung. Erfundene 2FA, Sicherheitsbewertungen, Aktivitätsversprechen
und unpassende Unterteilungen wurden nicht übernommen. Fremde Ausführungsskripte
bleiben ausschließlich in ignorierten Rohartefakten, niemals im lokalen Portal.

#### 3c – Gemeinsamer klickbarer Portalprototyp: technisch bereit

Aktueller Einstieg: `http://127.0.0.1:60825/preview/index.html`.
Dateien unter `target/designkritik-stitch/portal/`: `preview/index.html`,
`preview/portal.html`, `preview/Hinweise.txt` und `cosmos-portalprototyp.zip`.
`build_portal.py`, `portal-core.js`, `portal.js` und `portal.css` sind die lokalen
Prototypquellen; die geprüfte Profilreferenz liefert den gemeinsamen Rahmen.
`integrate_notifications.py` übernimmt die verfeinerte Benachrichtigungsquelle samt
gemeinsamer Zustands-/Overlaylogik aus der separaten Designprobe. Die alte parallele
Benachrichtigungslogik und deren HTML-Erzeugung sind entfernt.
Die Dateien sind bewusst temporäre, ignorierte Entwurfsartefakte. Vor einem
Root-Clean die gewünschte ZIP sichern; sie startet ohne Anwendung oder Internet.

- **Fünf Szenen:** Startseite mit ehrlichem Inhaltsplatzhalter, Persönliche Daten,
  Benachrichtigungen, Sicherheit sowie Geräte/Sitzungen. Seitenkopf, Navigation,
  Identität, Header/Footer und sechs Farbrollen werden gemeinsam gepflegt. Der
  vorangegangene Stitch-Entwurf bleibt visuelle Grundlage; neue Rohentwürfe wurden
  fachlich bereinigt und in diese gemeinsamen Bausteine übertragen statt als
  unabhängig gestaltete Vollseiten angehängt. Gemeinsame Primäraktion und genau
  eine aktive Navigationsmarkierung; keine zusätzlichen Panel-Farben pro Seite.
- **Verbundene Aktionen:** Zahnrad/Kontozugang zur Profilseite; Glocke zur Vorschau
  mit maximal fünf Meldungen, von dort Detail oder Inbox. Suche, drei Lesefilter,
  10/20/50 Zeilen, nummerierte Pagination samt erster/letzter Seite auf 23 festen
  Beispielen. Lesen/Entfernen/Bereinigung verändern nur den lokalen Beispielsatz;
  Öffnen allein setzt den Lesestatus nicht. Systemdetails zeigen Konto öffnen,
  Verwaltungsmitteilungen nicht. Kopf-/Sidebar-Zähler und Detailstatus bleiben synchron.
- **Vier gemeinsame Dialoge:** E-Mail mit neuer/Bestätigungsadresse und aktuellem
  Passwort; Passwort mit neuem/Bestätigungs-/aktuellem Passwort; Profilbild mit
  Initialen/Eigenes Bild, synthetischem Bild, Zoom, per Pfeiltasten verschiebbarem
  Beispielausschnitt, Zurücksetzen/Verwerfen/Löschen; Nachrichtendetail. Ursprüngliche
  Sicherheits-/Sitzungshinweise sind erhalten. Native modale Ebene, Escape,
  Schließen/Abbrechen und Fokusrückkehr; Passwortentwürfe werden auch nach Reveal
  beim Schließen geleert. Kontoaktionen/Widerruf/Logout sind reine Demo-Rückmeldungen.
  Es gibt keine echte Dateiauswahl, Mails, Speicherung oder Sitzungseingriffe.
- **Gemeinsame Overlays:** Sprache, Avatarstatus, Benutzermenü, Kalender,
  Benachrichtigungsvorschau und Seitengröße verwenden dieselbe begrenzte
  Overlaypositionierung und Palette. Mobile Navigation bleibt zwischen sichtbarem
  Header/Footer und schließt bei Zielwahl. Sprachwahl ist beurteilbar, die Demo
  bleibt Deutsch; eine vollständige EN-Ausarbeitung wird nicht vorgetäuscht.
- **Geprüfter Stand:** alle fünf Szenen bei `1440×1000`, `390×844` und `844×390`
  ohne horizontalen Überlauf. Lange Identität/E-Mail, mobile Navigation, modale
  Begrenzung/Scrollen, verschachtelte Nachrichtendetails und Fokusrückkehr geprüft.
  Suche, Paginationgrenzen, Seitengröße, Filter, Lesen/Entfernen, synchrone Zähler,
  synthetische Bildauswahl/Zoom/Ausschnitt und Demo-Hinweise gezielt bedient.
  Kalender-/Sprachliste und 6-px-Combo-Abstand erhalten. Python-Ruff,
  JavaScript-Syntax, skriptfreie Referenzen, Selbstständigkeit und ZIP-Integrität
  geprüft. Kein Java geändert; keine JUnit-/Browserjourney-Suite, große QA oder Sonar.

Maßgebliche konsistente Referenzen im bestehenden Stitch-Projekt; alle fünf nach
Upload unabhängig mit HTML-/Bildartefakt erneut abgerufen:

| Portalszene | Screen mit Präfix „Portalprototyp Endstand“ |
| --- | --- |
| Startseite | `13326829905224710828` |
| Benachrichtigungen | `12468834517238631217` |
| Persönliche Daten | `15723986144608948394` |
| Sicherheit | `8097382058482847080` |
| Geräte & Sitzungen | `17584143165337575147` |

Der aktuelle lokale Portalprototyp enthält zusätzlich die danach verfeinerte
Benachrichtigungsreferenz; die oben genannten Stitch-Screens enthalten diese lokale
Ergänzung noch nicht.
Der reine Loopback-Vorschauserver für die synthetischen Artefakte bleibt zur
Benutzersichtung verfügbar. `docs/status.md` bleibt unverändert die letzte große QA;
Anwendungsquellen, Theme, bestehende Entwicklerdaten und fremde Prozesse unverändert.
Der einmalige Folgeauftrag ist beendet und seine Automation entfernt. Es wurde
ausschließlich reguläres Kontingent verwendet; das Creditguthaben blieb unverändert.

**Noch offen:** Umfang und Beauftragung von 3d sowie die zurückgestellte
Logoentscheidung. Popup-Anordnung auf echten Geräten, Kontrastmessung,
Smartphone-Tastatur, Screenreader und vollständiges EN sind nicht abgenommen.
Spätere ästhetische Vertiefungen bleiben optional; die abgenommene HTML-Gestaltung
wird dadurch nicht erneut zur offenen Richtungsentscheidung. Paket 3 mit seinem
technischen Piloten und das Gesamtfeature sind nicht abgeschlossen.

Die erste Gesamtsichtung hatte die Profilseite bestätigt, die neuen Contentbereiche
aber noch als zu wenig ausgearbeitet bewertet. Die inzwischen umgesetzte Feinarbeit betraf:
passende konsistente Aktions-/Geräteicons, gleiche Buttonhöhen und sinnvolle
Aktionsgruppen, rechtsbündige gemeinsame Abschlussaktionen, erkennbare Infohinweise,
lesbarer einheitlicher Dialog-Schließknopf sowie eine klare Hierarchie der
Benachrichtigungswerkzeuge, Zähler und Pagination. Aktive Sitzungen und gespeicherte
Anmeldungen erhielten eine verständlichere Gliederung; deren unterschiedliche
Widerrufsaktionen sind bestehende Fachfunktionen, keine Stitch-Erfindung.
Die anschließenden Designrunden erfolgten einzeln nach Benutzerfreigabe, beginnend
mit dem folgenden Vergleich dreier Inbox-Contentvarianten.

**Separater Benachrichtigungsvergleich:**
`http://127.0.0.1:60825/notifications-variants/index.html`, mit Umschaltung A/B/C,
unveränderter Profilreferenz sowie Desktop-/Smartphoneformat. Alle drei Entwürfe
wurden im bestehenden Stitch-Projekt erzeugt und unabhängig samt HTML abgerufen:

| Lokaler Vergleich | Stitch-Screen | Gestaltung |
| --- | --- | --- |
| A | `df4a5fc58e124a758502aa7121e15e1a` | Kompakte Inbox, Zeilenaktionen oben rechts |
| B | `5529ff78df5b4c4091ea1b6cd91b6c5f` | Zeitgruppen und größere Karten mit eigener Aktionszeile |
| C | `e5579c073372412e823f80757194eb48` | Strukturierte Karten und deutlichere Zähler-Badges |

A und C sind verwandte Vorschläge, B unterscheidet sich stärker. Der CLI-Aufruf
endete mit Timeout; der Browserweg wurde kontrolliert genutzt und die gespeicherten
Ergebnisse anschließend unabhängig gelesen. Die Rohbilder waren wegen fehlender
Utilitystyles bzw. blockierter CDN-Skripte teilweise unbrauchbar. Für den lokalen
Vergleich werden ausschließlich die Contentfragmente in den identischen bestehenden
Referenzrahmen gesetzt. Entwurfs-CSS ist auf den Content begrenzt; benötigte
Utilityregeln wurden mit dem bekannten Tailwind-Compiler erzeugt und eingefroren.
Die ausgelieferten Varianten enthalten keine Skripte oder externen Laufzeitressourcen.
Erfundene „als ungelesen“-Aktionen und fachfremde Beispielversprechen sind entfernt;
gleiche harmlose Nachrichtentexte, feste Flächenrollen und schmale Umbrüche gelten
für alle Varianten. Keine stillschweigende neue Designentscheidung oder Übernahme.

Arbeitsquellen liegen unter `target/designkritik-stitch/notifications-variants/`,
statische Vergleichsseiten unter `target/designkritik-stitch/portal/notifications-variants/`.
Das Offlinepaket heißt `cosmos-benachrichtigungen-varianten.zip`. Desktopvorschauen
wurden visuell geprüft; bei 390 px bleiben die Inhaltsaktionen innerhalb der Breite.
Der gemeinsame Rahmen ist strukturell mit der unveränderten Referenz abgeglichen.
Python-Formatierung/Formatprüfung, Offline- und ZIP-Prüfung sind erfolgt; keine
Anwendungsbuilds, Browserjourneys oder große QA. Der bisherige klickbare Prototyp
bleibt unverändert. Der Benutzer hat C gewählt; die anderen Varianten bleiben als
Vergleich erhalten.

**Aktuelle C-Verfeinerung zur Sichtung:**
`http://127.0.0.1:60825/notifications-variants/c-refined-preview.html`.
Sie enthält C vorher/nachher sowie die Profilseite mit dem vereinbarten roten
Ungelesen-Zähler. Karten und linke cyanblaue Ungelesen-Markierung bleiben erhalten.
Zähler sind gleich groß und mittig, die drei Lesefilter gleich breit. Toolbar und
Nachrichten verwenden quadratische Iconbuttons mit verständlichen Tooltips bei
Hover/Tastaturfokus und zugänglichen Namen. Aktualisieren, Gelesen-Markieren und
Löschen behalten ihre getrennte Bedeutung; Löschicons sind rot. Pagination ist in
Seitengröße, mittigen Eintragsbereich und gleichmäßige Seitenschaltflächen gegliedert;
die 180-Tage-Information steht separat mit Info-Icon. Glocken sind konsistent,
Header-/Navigationsplaketten rot mit weißer Zahl; innerhalb der Inbox bleibt Blau.
Die Farbregel zum Ausblenden bei Zähler null ist enthalten, die statische Vorschau
zeigt jedoch fest zwölf ungelesene Nachrichten und simuliert keine Kontoaktionen.

Stitch-Screen `3da36e95ce76444fab9a45f9914a1331`, „Portalprototyp verfeinert –
Benachrichtigungen“, wurde neu erzeugt und unabhängig samt HTML/Bild abgerufen.
Er bestätigt die gewünschte Pagination-/Iconanordnung, verändert jedoch zusätzlich
den Referenzrahmen und verdichtet die Karten. Die lokale Sichtvorlage setzt deshalb
die konkret vereinbarten Anpassungen gezielt auf die bisherige C-Grundlage; der
neue Stitch-Rohentwurf wird nicht als vollständige Seite übernommen. Der direkte
Vergleich und diese Herkunftsunterscheidung bleiben erhalten.

Quellen: `refine_c.py`/`c-refined.css` im bestehenden ignorierten Variantenbereich;
Offlinepaket `cosmos-benachrichtigungen-c-verfeinert.zip`. Geprüft: Desktop,
Smartphone Hoch-/Querformat, gleiche Maße, lesbare Namenszuordnung aller Iconbuttons,
Tastatur-Tooltip sowie Paginationumbruch und Profil-Zählervorschau. Formatprüfung,
Offline-/ZIP-Prüfung und `git diff --check` durchgeführt. Keine Anwendungsänderung,
kein Build/Scan und keine Browserjourneys. Die Profil-Zählervorschau ist ausdrücklich
eine statische Referenz; die interaktiven Benutzer-/Sprach-/Statusoverlays bleiben
im Portal erhalten. Der vollständige Benachrichtigungsbereich ist nach der
gemeinsamen Sichtung einschließlich der folgenden Kleinkorrekturen übernommen.

**Aktuelle Overlayrunde:**
`http://127.0.0.1:60825/notifications-overlays/index.html`.
Lesefilter stehen links, die drei Sammelaktionsicons rechts in der ersten Zeile;
die senkrechte Trennlinie entfällt. Die horizontale Linie zwischen dieser Zeile
und der darunter vollbreiten Suche reicht ohne seitliche Einrückung von innerer
Panelkante zu innerer Panelkante. Die Inhalte behalten ihre normalen Einrückungen.
Filterzahlen sind abgesetzte runde Badges, auch in der Glockenvorschau.
Die bestehende C-Kartenstruktur,
Zähler, Highlight-/Icongestaltung und Pagination bleiben die Grundlage.

- Glockenvorschau: rechts unter der Glocke, höchstens fünf neueste Meldungen mit
  scrollbar begrenztem Inhalt, Lesefiltern, Iconaktionen, sichtbarem Schließsymbol,
  mittigem blauem Inbox-Zugang über die volle Breite und 180-Tage-Info. Der Kopf
  nennt „Neueste Nachrichten · max. 5“; seine Zahl zählt alle ungelesenen Nachrichten
  im Postfach, unabhängig vom Filter oder den höchstens fünf sichtbaren Einträgen.
  Die Zahl steht ohne sichtbaren Zusatz in einer kompakten blau konturierten
  Plakette; Tooltip und zugänglicher Name erklären „ungelesene Nachrichten insgesamt“.
  Nachrichtendetail lässt die Vorschau
  im Hintergrund bestehen; Escape und Schließen führen zum Auslöser zurück.
- Nachrichtendetail: Betreff, Herkunft, Zeit und Lesestatus, vollständiger Text,
  Gelesen-Aktion nur wenn nötig, Konto-Zugang nur bei Systemnachrichten und
  Inbox-Zugang bei Aufruf aus der Glockenvorschau. Öffnen allein bleibt ohne
  Änderung des Lesestatus. Einheitliche Text-/Iconbuttons und Schließen-Symbole.
- Gemeinsamer Bestätigungsdialog als ausdrücklich gewünschter neuer Vorschlag:
  eine Struktur für Titel, Erklärung, betroffene Nachricht/Anzahl, Info und
  rechtsbündige Abbrechen-/Bestätigenaktionen. Einzelnes Entfernen und alle
  gelesenen Nachrichten verwenden Rot; eine neutrale Sammelbestätigung für
  Gelesen-Markieren wird separat blau gezeigt. Abbrechen erhält initial den Fokus.
  Es gibt derzeit keinen solchen Löschdialog in `UserNotificationsPanel`; die
  Anwendung entfernt direkt. Der neue Ablauf wird ausschließlich in der Designprobe
  simuliert. Ob künftig beim Sammel-Gelesen-Markieren bestätigt werden soll, bleibt
  eine offene Entscheidung; einzelne Leseklicks erhalten keine Pflichtbestätigung.

Stitch-Arbeitsreferenz: importierter synthetischer C-Screen `2428522476801571352`;
Original-Glockenreferenz `14376026531723469630`. Fachvertrag der Detailanzeige und
des direkten Entfernens wurde mit `UserNotificationsPanel`/`UserNotificationBell`
abgeglichen. Es wurde keine neue Originalaufnahme oder ein vorhandener Löschdialog
behauptet. Stitch lieferte `2a01d06a65994f20bd60cf4d880378e2`, „Cosmos
Benachrichtigungen – Overlays & Toolbar“; HTML und Bild sind unabhängig abgerufen.
Seine Overlay-/Bestätigungsstruktur und gruppierte Toolbar wurden für die lokale
Probe an den erhaltenen C-Rahmen, feste Flächen und vorhandene Schließen-Symbole
angepasst. Fremde Skripte, erfundene Sicherheits-/Supportversprechen und zusätzliche
Fachfunktionen wurden nicht übernommen. Die lokale Bedienung stammt aus eigenen
Vorschauskripten und verändert ausschließlich synthetische Daten im Browser.

Die Prüfsteuerung zeigt Inbox, Glockenvorschau, Nachricht, einzelne/Sammelentfernung
und optionale neutrale Bestätigung. Suche, Lesefilter, Seitengrößen 10/20/50,
Pagination, Markieren und Entfernen sind innerhalb dieser isolierten Probe bedienbar;
Zurücksetzen stellt 23/12 Beispieldaten wieder her. Andere Portalbereiche dienen
als unveränderter Rahmen, die bisherigen Benutzer-/Sprach-/Statusoverlays bleiben
im vollständigen Portal erhalten. Nach positiver Benutzersichtung ist diese Runde
einschließlich der gewünschten Kleinkorrekturen als zusätzliche Referenz übernommen;
Profil und andere Portalinhalte sind gestalterisch unverändert.

Inbox, Glockenvorschau und Nachrichtendetail verwenden dieselbe Metadatendarstellung:
Herkunfts-Badge System/Administration, Zeit und separater Lesestatus. Ein gemeinsamer
Renderer und gemeinsame CSS-Klassen verhindern drei voneinander abweichende Varianten.
Ungelesen hat in allen drei Ansichten denselben dunklen Eingabehintergrund,
cyanfarbenen linken Rand sowie einen blauen Punkt im Status-Badge; Gelesen verwendet
die normale Panelfläche und den zurückhaltenden Status ohne Punkt. Das Markieren
aktualisiert Darstellung, Filterzahlen und globale Zähler gemeinsam. Der äußere
Overlayrahmen ist mit dem tatsächlich berechneten Avatarmenü abgeglichen: Grundfläche,
Kontur, Rundung und Schatten folgen derselben Referenz. Nachrichtenzustände bleiben
innere Panel-/Statusrollen innerhalb der festen Palette, keine neuen Overlayfarben.

Gezielt nachgeprüft: Trennlinie 0 px und Sammelaktionen an der rechten Innenkante,
gleich breite Filter mit Zahlen-Badges, fünf Vorschauzeilen und mittiger Zugang über
die volle Breite. Hochformat 390×844 und Querformat 844×390 bleiben ohne horizontalen
Überlauf; der Vorschauabschluss bleibt erreichbar. Der Lesestatuswechsel im Detail
ändert die globale Zahl von zwölf auf elf und dieselbe Nachricht in der Vorschau
auf Gelesen. Hintergrund, Kontur, 16-px-Rundung und Schatten der äußeren Vorschau
entsprechen den berechneten Werten des bestehenden Avatarmenüs. Synthetische
Nachrichten sind innerhalb der Tage absteigend datiert, passend zur Neueste-Angabe.

Quellen: `target/designkritik-stitch/notifications-variants/overlay-round/`;
Vorschau: `target/designkritik-stitch/portal/notifications-overlays/`;
Offlinepaket: `cosmos-benachrichtigungen-overlays.zip`. Tatsächlich geprüft:
Desktop und Smartphone Hoch-/Querformat ohne seitliches Abschneiden, begrenzte
scrollbare Inhalte, erreichbare Dialogaktionen, Escape/Fokusrückkehr, getrennte
System-/Administrationsaktionen, Öffnen ohne Gelesen-Markieren, Abbrechen ohne
Änderung, Einzelentfernung 23/12 → 22/11 und Entfernen von elf gelesenen Nachrichten
mit zwölf ungelesenen verbleibend. Neutrale Bestätigung arbeitet mit sichtbarem
Snapshot; bei null Ungelesenen verschwinden beide roten Plaketten. Suche und
Paginationgrenzen sowie Seitengrößen 20/50 sind beobachtet. Ruff-Formatierung,
JavaScript-Syntax, Offline-/ZIP- und Diffprüfung sind erfolgt. Keine Anwendungs-
oder Themeänderung, kein Build, Scan, JUnit- oder vollständiger Browserlauf.
Der gezielte Stitch-Nachvorschlag `a197ed446e8648dfbf34f6d7bd1e415a`,
„Cosmos Benachrichtigungen – Verfeinerte Glockenvorschau & Toolbar“, wurde erzeugt
und unabhängig samt HTML/Bild abgerufen. Er zeigt die gewünschte kompakte blaue
Zahlenkontur ohne Zusatzwort; eine separate textliche Begründung lieferte er nicht.
Nur diese Kleingestaltung ist übernommen, keine weiteren Rahmen-/Iconänderungen
aus seinem Rohentwurf. Die horizontale Trennung bleibt ausdrücklich erhalten.

Gemeinsamer Einstieg ist wieder `http://127.0.0.1:60825/preview/index.html`;
die separate Overlayprobe bleibt zum Vergleich verfügbar. Profil, Glockenvorschau,
Inbox und Detail verwenden nun denselben Benachrichtigungsbestand. „Alle anzeigen“
führt in die Inbox, der Systemnachrichten-Kontozugang zu Sicherheit. Profilmenüs,
Sprache und mobile Navigation sind gezielt zusammen mit der Integration geprüft;
der Leseklick aktualisiert beide roten Zähler von zwölf auf elf. Die technische
Übernahme in die HTML-Referenz ist erfolgt, keine Vaadin- oder globale Themefreigabe.

**Gemeinsame Tooltips im HTML-Prototyp:** Die bisherige Mischung aus nativen
Browser-Tooltips und lokalen CSS-Pseudoelementen war kein abgestimmter Stitch-
Tooltipentwurf. `portal/tooltips.css` und `tooltips.js` liefern nun denselben
lokalen Baustein für den Portalprototyp und die separate Benachrichtigungsprobe:
vorhandene Overlayfläche, einheitliche Schrift/Kontur/Rundung, begrenzte Breite und
Viewportposition, Hover sowie Tastaturfokus. Ein manuelles Popover hält den Hinweis
auch über nativen modalen Dialogen sichtbar; vorhandene ARIA-Beschreibungen bleiben
erhalten. Scrollen, Escape, Schließen und Zielwechsel entfernen den Hinweis.
Diese lokale Vereinheitlichung ist keine neue Vaadin-Themefreigabe.

**Startseitenprobe:** Stitch-Screen `1e20dc6febf94039a8a2af2cdc51a283`,
„Silent Cosmos – Startseite (Atmosphärischer Einstieg)“, aus der bestehenden
Profilreferenz `15723986144608948394` erzeugt und unabhängig erneut abgerufen.
HTML und Vorschaubild sind vorhanden. Die authentifizierungspflichtigen direkten
Downloadlinks waren nicht anonym lesbar; die zusätzlich in der Generierungsantwort
gelieferten Artefaktlinks funktionierten ohne Übernahme von Zugangsdaten.
Die Bildidee und SVG-Illustration mit Planet/Umlaufbahnen stammen aus Stitch.
Der lokale Entwurf erhält den Referenzrahmen und übernimmt nur den Inhaltsbereich:
ruhige Willkommenskomposition, vorhandene Flächen/Buttons und eine Illustration
ohne externe Bild-/Schriftanforderungen. Eine erfundene Versionsnummer, eine
automatische Verfügbarkeitszusage und der redundante zusätzliche Identitätsstatus
wurden nicht übernommen; die Texte folgen der bisherigen Sie-Ansprache.

`portal/prepare_home.py`, `home.html` und `home.css` bilden die lokale Probe.
Einstieg: `http://127.0.0.1:60825/preview/home.html` oder Startseite im normalen
Portal. Die Zugänge führen ausschließlich zu bestehenden Szenen: Persönliche
Daten und Benachrichtigungen. Der optionale Zahlenhinweis teilt deren lokalen
Nachrichtenbestand; es gibt keine Registrierung oder erfundenen neuen Dienst.
Die Startseite ist ausdrücklich ein vorläufiger gestalterischer Inhaltsplatzhalter
und wurde vom Benutzer positiv bewertet. Die Sicherheitsrunde folgt unten;
Geräte bleiben für die gesonderte seitenweise Überarbeitung offen.

Gezielt beobachtet: Desktop 1440×1000, Smartphone 390×844 und Querformat 844×390
ohne horizontalen Überlauf; beide Startseitenziele, synchroner Hero-Zähler 12→11,
durchgehende Toolbarlinie bis auf die 1-px-Panelkontur am Desktop und Smartphone,
gleich gestalteter Tastaturtooltip einschließlich über dem Nachrichtendialog.
JavaScript-Syntax, Python-Ruff, Offline-/ZIP- und Diffprüfungen durchgeführt.
Kein Anwendungsbuild, keine Browserjourneys, JUnit oder Sonar. Der gemeinsame
Portalprototyp und seine ZIP enthalten die neue Probe, keine Übernahme ins
Anwendungstheme und keine endgültige Startseitenabnahme.

**Sicherheitsrunde – als HTML-Entwurfsreferenz abgenommen:**
`http://127.0.0.1:60825/preview/security.html`, ebenfalls über die bestehende
Seitennavigation erreichbar. Gestaltung ausschließlich im HTML-Prototyp.

| Stitch-Vorschlag | Unabhängig abgerufener Screen |
| --- | --- |
| Sicherheit & Zugang | `f668de3795f94ddca333ac4b2046e3df` |
| Passwort ändern | `bacb7fba36e3488cb0dac981f113b2d2` |
| E-Mail-Adresse ändern | `98c623f6051a4ebc8bafd9b2caa8a728` |

Alle drei basieren auf der bestehenden Profilreferenz und den zugehörigen
bereinigten Originalen. Sicherheitsseite und E-Mail-Dialog sind mit HTML/Bild
heruntergeladen. Der Passwortscreen war über die angebotenen Downloadlinks nicht
anonym abrufbar; sein tatsächlich gerenderter HTML-Vorschauframe wurde im bereits
angemeldeten Stitch-Projekt gelesen und lokal gesichert. Seine Felder und Hinweise
sind damit ebenfalls inhaltlich geprüft. Kein Lesen oder Übertragen von Zugangsdaten.

Übernommen wurden zwei geordnete Zugangsbereiche, gleich breite Ändern-Buttons
mit Stifticon, ein Briefumschlag für E-Mail und gemeinsame Infohinweise mit
Info-Kreis. Fremde Rahmen-/Footeränderungen, neue Fachfunktionen und die von Stitch
ergänzte pauschale Aufforderung zum regelmäßigen Passwortwechsel sind nicht
übernommen. Die lokalen Quellen `refine_security.py` und `security.css` erhalten
die gemeinsamen Flächenrollen; Passwort-/E-Mail-Dialoge übernehmen das vorhandene
Schließen-Symbol und die 36-px-Aktionen der Referenz. Andere Szenen ändern dadurch
weder Layout noch eigene Dialoggestaltung. Die Passwortanzeige hat ein echtes
Augensymbol sowie passende zugängliche Anzeigen-/Verbergen-Bezeichnungen.
Die Panelköpfe von Persönliche Daten, Benachrichtigungen und Sicherheit verwenden
auf Benutzerwunsch den größeren Benachrichtigungsmaßstab gemeinsam. Die lokale
Quelle `panel-heads.css` bündelt 40-px-Iconfläche mit 20-px-SVG, blauer Kontur,
gleicher Fläche/Rundung sowie identische 18-px-Titel und 12-px-Beschreibungen.
Die untergeordneten Passwort-/E-Mail-Symbole sind ebenfalls cyanfarben; Passwort
verwendet zur Unterscheidung vom Sicherheits-Schloss einen Schlüssel, auch im
Änderungsdialog. Die Eingabefelder behalten ihre bekannten Passwortsymbole.
Berechnete Icon-/Titelwerte aller drei Köpfe sind direkt verglichen und stimmen
überein; die Sicherheitsansicht ist zusätzlich schmal ohne horizontalen Überlauf
geprüft. Diese Vereinheitlichung gilt weiterhin nur für die HTML-Entwurfsreferenz.
Die Titelicons sind zur vollständigen Titel-/Beschreibungsgruppe mittig ausgerichtet;
bei Benachrichtigungen sind die Mittelpunkte direkt als gleich geprüft. Die
Headericons aller fünf Inhaltsdialoge (Passwort, E-Mail, Profilbild, Nachricht,
Bestätigung) tragen einen sichtbaren Rahmen. Neutrale Icons verwenden dieselbe
blaue Kontur; die Danger-Bestätigung behält ihre fachlich rote Gestaltung.

Fachlicher Abgleich mit `AccountSettingsComponent`, `AccountPasswordChangeDialog`
und `AccountEmailChangeDialog`: je drei vorhandene Felder in bestehender Reihenfolge;
beim Passwortwechsel Ende anderer Sitzungen/gespeicherter Anmeldungen, diese Sitzung
bleibt; bei E-Mail-Änderung bisherige Adresse bis zur Bestätigung gültig, erst danach
Ende aller Sitzungen/gespeicherter Anmeldungen. Die Vorschau versendet keine Mail und
ändert keine echten Zugangsdaten. Native Dialoge, Escape, Abbrechen und das Leeren
auch zuvor sichtbar gemachter Entwürfe bleiben erhalten. Die neue Passwortanzeige
setzt beim Schließen Typ, Bezeichnung und Tooltip zurück.

Gezielt geprüft: Desktop 1440×1000 und Smartphone 390×844/844×390; lange
Beispielidentität/-adresse ohne horizontalen Überlauf; beide Übersichtsaktionen
110×36 px, Dialogaktionen 36 px; gemeinsamer Overlayhintergrund; Feldreihenfolge,
Passwortanzeige, Escape-/Abbrechen-Rücksetzung und Fokusrückkehr. Im Querformat
scrollt der Body bei sichtbarer Aktionsleiste. Python-Ruff, JavaScript-Syntax,
Offline-/ZIP- und Diffprüfungen sind erfolgt. Kein Anwendungsbuild, JUnit,
vollständiger Browserlauf oder Sonar. Die Sicherheitsrunde einschließlich beider
Änderungsdialoge ist vom Benutzer gestalterisch abgenommen. Geräte/Sitzungen und
der nicht angemeldete Bereich bleiben offen; deren Umsetzung beginnt nicht allein
durch diese Teilabnahme. Vaadin-Pilot und Gesamtfeature bleiben ebenfalls offen.

**Profilbild bearbeiten – als HTML-Entwurfsreferenz gestalterisch abgenommen:**
Die aktuelle synthetische HTML-Referenz ist als Screen `15014059398700707951`
hochgeladen. Stitch-Entwurf `3cb18e0f282f4e69a556c142ff42c0c1`, „Cosmos – Profilbild
bearbeiten (Verfeinerter Dialog)“, ist unabhängig abgerufen. Übernommen sind
geordnete Moduswahl mit Icons, runde Vorschau, gegliederte Bildauswahl/Zoom,
Infohinweis und gemeinsame Dialogrollen. Der Rohentwurf ließ den bestehenden
Ausschnitteditor weg und verwendete ein Personenicon. Ein gezielter Nachauftrag
lieferte keinen neuen verwendbaren Screen. Daher bleiben Ausschnitteditor und
seine Bedienung aus der vorhandenen Demo erhalten, mit runder Maske und dem
Stitch-Bildicon ergänzt; diese Korrekturen sind lokale Anpassungen.

`refine_avatar.py` und `avatar.css` begrenzen die Gestaltung auf diesen Dialog.
Initialen/eigenes Bild bleiben zwei gleich breite Optionen nebeneinander.
JPG/JPEG oder PNG bis 5 MiB sind der bestehende Anwendungsvertrag; die Demo lädt
ausschließlich ihr synthetisches SVG, keine lokale Datei. Auswahl, Zoom,
Pfeiltasten-Ausschnitt, Zurücksetzen, Auswahl verwerfen, bedingtes Bildlöschen,
Abbrechen und Speichern bleiben vorhanden. Vorschau und Ausschnitt verwenden
denselben proportionalen Transform; der lokal übernommene Beispielausschnitt bleibt
beim erneuten Öffnen erhalten. Abbrechen verwirft Änderungen einschließlich einer
vorgemerkten Löschung. Kein echter Upload oder Kontoeingriff.

Geprüft: Desktop sowie Smartphone 390×844 und 844×390 ohne horizontalen Überlauf;
gleich hohe Modusbuttons auf einer Zeile, erreichbarer Footer und scrollbar
begrenzter Body. Zoom/Pfeiltasten aktualisieren Vorschau und Editor gleich;
Zurücksetzen, Demoübernahme/erneutes Öffnen und Abbrechen nach Änderungen/Löschung
geprüft. Escape schließt. Ruff, JavaScript-Syntax, Offline-/ZIP- und Diffprüfung
erfolgt. Keine Anwendungstests oder große QA. Der Benutzer hat die Profilbildgestaltung
einschließlich der letzten Zentrierungen abgenommen; der ergänzte Besucherbereich
ist inzwischen ebenfalls als eigene Gestaltungsreferenz abgenommen.

Der Hinweis unter dem Ausschnitt ist auf Benutzerwunsch zentriert. Die beiden
Ausschnittaktionen teilen am Desktop eine Zeile in gleich breite, mittig
beschriftete Buttons; schmal stehen sie gleich breit untereinander. Die gemeinsame
HTML-Referenz richtet Info-Icons neben ein- und mehrzeiligen Hinweistexten mittig
zum gesamten Textblock aus, über die zentrale Regel in `reference-controls.css`.
Desktop und Smartphone geprüft: gleiche Buttonbreiten/-höhen, zentrierter
Ausschnitthinweis, mittige Icons auch neben mehrzeiligem Text und kein horizontaler
Überlauf. Die Regel gilt für die HTML-Entwurfsreferenz, nicht für das Anwendungstheme.

**Besucherbereich – feste, gestalterisch abgenommene HTML-Referenz:**
Der ausdrücklich beauftragte einmalige Folgeauftrag vom 26. Oktober 2026 ist
technisch umgesetzt. Vor Beginn wurden tatsächliche Uhrzeit und das erneuerte
reguläre Kontingent geprüft; zwischen größeren Abschnitten erneut geprüft.
Keine Credits, Käufe, Resets oder API-Abrechnung verwendet. Die eigene Automation
`besucherbereich-heute-03-15-angleichen` ist nach der Ausführung entfernt.
Nach den gezielten Nacharbeiten und der letzten Schriftkorrektur hat der Benutzer
die aktuelle Fassung als Referenz für nicht angemeldete Benutzer bestätigt.
Diese Abnahme umfasst die Gestaltung des HTML-Prototyps, keine Vaadin-Übertragung,
technische Anwendungsabnahme oder abschließende Logoentscheidung.

Die neun Szenen bleiben an die vorhandenen öffentlichen Fachverträge gebunden:
Startseite, Anmeldung, Registrierung mit acht Pflichtfeldern, Passwort vergessen,
Passwort zurücksetzen, Kontoaktivierung, ausdrückliche E-Mail-Änderungsbestätigung,
eingeschränkte Anmeldung bei Sitzungsgrenze und Wartung. Keine sozialen Logins,
Magic-Links, Einladungen, zusätzlichen Einwilligungen oder Betriebsversprechen.
Die Passwort-vergessen-Antwort ist kontounabhängig. Ein E-Mail-Änderungslink zeigt
zunächst die Bestätigungsfrage; erst die Demo-Aktion führt zum Ergebniszustand.
Gespeicherte Anmeldungen ohne aktive Sitzung belegen keinen Sitzungsplatz.

Die feste angemeldete Referenz wird gestalterisch nicht verändert. Ihr Headerrahmen,
Footer-DOM, Seitenkopf, eingebettete Schriften, gemeinsame Styles und Formularbausteine
werden für die Besucheransicht wiederverwendet. `public_reference.py` übernimmt diese
Quellen beim Zusammenbau; `public.css` ergänzt nur die öffentliche Anordnung sowie
die lokalen Checkbox-/Meldungsmuster. Kein unabhängiges Besucher-Farb- oder Titelsystem.
Kicker cyan/großgeschrieben, Titelpunkt, Titel/Beschreibung und responsive Abstände
folgen der Referenz. Headeraktionen besitzen passende Haus-/Anmelde-/Registrierungsicons;
der aktive Eintrag ist cyan ohne Unterstrich. Sehr schmal bleiben die beschrifteten
zugänglichen Iconaktionen erhalten; der Markenname kann Platz machen.

Die öffentliche Sprachauswahl übernimmt auf Benutzerwunsch den DE/EN-Umschalter
aus dem Avatar-Menü der festen Referenz direkt in den öffentlichen Header.
Sichtbar ist ausschließlich die DE/EN-Umschaltfläche mit gleicher Schrift,
Flächenpalette und ausgewähltem Zustand; kein zusätzlicher Sprachbutton und kein
Popup. Geschlecht verwendet weiterhin die vorhandene gestaltete Listbox, Geburtsdatum
das vorhandene Textfeld mit Kalenderauslöser und Kalender einschließlich Monats-/
Jahrauswahl. Die sichtbaren nativen Browserauswahlen sind ersetzt. Gemeinsam verwendet
werden Overlaygeometrie, Kalenderquelle und Tooltips. `public-controls.js` ergänzt
Auswahl/Tastatur/Fokus; `public-calendar.js` wird aus der bestehenden Kalenderquelle
mit angepasster Feld-ID und gemeinsamer Verankerung erzeugt. Die ausgewählte Sprache
bleibt ausdrücklich Designvorschau; sämtliche Texte bleiben Deutsch.

Stitch wurde mit zwei bereinigten, skriptfreien Referenzimporten gezielt beauftragt.
Maßgebliche unabhängig erneut abgerufene Entwürfe sind
`8059efa75bd0454dba334130d524360d` (globale transiente Meldungen) und
`edecfc5050544ddfb67b08a0bea59d77` (Registrierung nach gezielter Korrekturrunde).
Der erste Kontrollentwurf hatte erneut Pflichtfelder ausgelassen und Sprache,
Einwilligung sowie Sicherheitsversprechen erfunden. Das wurde ausdrücklich bei
Stitch korrigiert und lokal am tatsächlichen Fachvertrag geprüft. Die generierten
Tailwind-/Fremdskripte werden nicht ausgeführt oder übernommen. Roh-HTML/Bilder und
Aufträge liegen temporär unter `portal/stitch/alignment-*`. Die lokale Einpassung
wiederverwendet die bereits akzeptierten Komponenten statt unkontrolliert eine
neue generierte Seite zu übernehmen.

Das tatsächliche `NotificationHelper`-Verhalten wurde samt Theme geprüft: vier
Meldungstypen, TOP_CENTER, jeweils eine aktuelle Meldung, Standarddauer und mögliche
dauerhafte Meldungen, semantische Icons/Farben und manuelles Schließen. Der lokale
Vorschlag `public-notifications.js` zeigt eine begrenzte globale Meldung unter dem
Header außerhalb des Layoutflusses, mit Titel/Text, Cyan-Kreis-X, Alert/Status-Ausgabe
und pausierender Ablaufzeit bei Hover/Fokus. Fehler sind rot mit Warnsymbol, Erfolg
grün mit Hakenkreis, Warnungen gelb mit Ausrufekreis; normale Demohinweise cyan. Wichtige
Beispiel-Fehler können bis zum Schließen stehen bleiben. Escape schließt die Meldung,
sofern kein Auswahl-/Kalenderoverlay Vorrang hat. Kein automatischer Fokuswechsel.
Dauerhafte Link-/Ergebnisansichten ersetzen die jeweilige Karte, während transiente
Fehler keinen zusätzlichen Bereich im Seitenfluss einfügen. Erfolgskarten sind
dezent grün konturiert. Das ist ein HTML-Gestaltungsvorschlag, keine Änderung des
Anwendungs-NotificationHelper oder seines Themes.

Buttons einschließlich dynamischer Ergebnisse haben richtige semantische SVGs,
gemeinsame Rollen/Größen und Cyan-Secondary-/weiße Primary-Icons. Refresh und Anmelden
sind unterscheidbar; Danger bleibt rot. Info-Icons stehen mittig zum gesamten Text.
Die Recovery-Szene trennt Informationen und Aktionsflächen mit durchgehenden Linien;
Aktualisieren/Abmelden und Abbrechen/Fortsetzen folgen demselben Panelstandard.

Die aktuelle Feinkorrektur der ersten sieben Besucheransichten übernimmt die
hellweiße Aktionsschrift der festen Referenz; aktiv bleibt die Navigation cyan.
Kontowechselfragen und Textlinks bilden eine Gruppe mit acht Pixel Abstand:
„Sie haben noch kein Konto? Jetzt registrieren“ beziehungsweise „Sie haben bereits
ein Konto? Anmelden“. Passwort-vergessen, Passwort-Reset und E-Mail-Bestätigung
bündeln gleich breite Secondary-/Primary-Aktionen rechts in einer Fußleiste;
schmal stehen sie gleich breit untereinander. Der ungültige Aktivierungslink hat
seine Anforderung ebenfalls in dieser Fußleiste und nur eine Primary-Aktion.
Globale Meldungen verwenden 28-px-Symbole; ungültige/abgelaufene Links bekommen
einen Ausrufekreis statt der für die Inbox verwendeten Glocke. Technische Fehler
behalten das Warndreieck. Die Anwendung und ihre Warnsymbolzuordnung bleiben unverändert.

Die gezielte Stitch-Nachrunde ist Screen `4e5fca3bc97d43db9e7430677fa8fc63`
(„Komponenten-Korrekturboard“), unabhängig ohne CLI-Cache erneut mit HTML/Bild
abgerufen. Sie zeigt die rechts gruppierten Abschlussaktionen und eine eindeutige
Primary-Hierarchie. Die erste Runde ließ diese Korrekturen noch aus. Erfundenen
Statuscode, 24-Stunden-Frist, Versandversprechen und weitere Farb-/Textabweichungen
aus dem Rohentwurf nicht übernommen. Lokale Aufträge/Belege: `stitch/public-polish-*`.

Die Texte dieser sieben Ansichten sind mit `messages_de.properties` und den
tatsächlichen Views abgeglichen: Die Kontowechselfragen folgen der Anwendung,
die Registrierung heißt jetzt „Registrierung erfolgreich“ mit weiterhin nötiger
E-Mail-Aktivierung. Passwort-vergessen übernimmt den kontounabhängigen Hinweis
einschließlich Spamordner; E-Mail-Bestätigung nennt den tatsächlichen Sitzungswiderruf
und die erneute Anmeldung. Ungültiger, abgelaufener und konkurrierender E-Mail-Link
behalten ihre unterschiedlichen Folgen. Klarere Begriffe wie „Link“ statt „Schlüssel“
und die einheitliche Sie-Anrede bleiben erhalten. Technische Fehlermeldungen nennen
den betroffenen Vorgang, ohne pauschal zu behaupten, es sei kein Konto angelegt worden.
Anwendungskatalog und Fachabläufe wurden nicht geändert.

Gezielter Nachtest: sieben Grundansichten und 19 ergänzende Zustände jeweils in
Desktop/Hoch-/Querformat, insgesamt 78 geprüfte Kombinationen. Kein Überlauf,
keine doppelte Primary-Aktion pro sichtbarer Karte, gleiche Buttonbreiten/-höhen
in den Aktionspaaren, Footer weiterhin 41 px und keine fehlenden Aktionsicons.
Anforderung per Enter nach Verlagerung der Aktivierungsaktion sowie der ausdrückliche
E-Mail-Bestätigungsbutton sind bedient. Temporäre Belege: `alignment-checks/public-polish-*`.
Die angemeldete Referenz bleibt unverändert. Die anschließend ausdrücklich
beauftragte Detailrunde für **Sitzungsgrenze/Anmeldung fortsetzen und Wartung**
ist ebenfalls lokal eingepasst und liegt zur gemeinsamen Sichtung vor.
Die aktuelle Besucherfassung ist nach der letzten Schriftkorrektur gestalterisch abgenommen.

Die eingeschränkte Anmeldung übernimmt Zeilen, Geräte-/Zustandssymbole,
Zustandsbadges, Aktivitäts-/Anmeldezeiten und Limitplakette aus der festen
Gerätereferenz. `apply_recovery_reference` verwendet deren DOM und gemeinsame Styles;
kein zweites Geräte-Design. Vier synthetische Einträge bilden bewusst drei aktive
Sitzungen und eine gespeicherte Anmeldung ohne aktive Sitzung ab. Nur die drei
aktiven zählen gegen „Limit: 3 Sitzungen“. Die gespeicherte Zeile zeigt „Keine aktive
Sitzung“ und „Anmeldung gespeichert“, aktive Zeilen das blaue Sitzungsbadge und bei
Bedarf den gespeicherten Zugang. Im eingeschränkten Login gibt es keine bereits
angemeldete aktuelle Sitzung und daher kein grünes „Dieses Gerät“. Alle betroffenen
Zeilenaktionen bleiben bedienbar. Einzelaktionen übernehmen den neutralen
Secondary-Stil der Referenz; die Sammelaktion bleibt Danger.

Der Wartungsbereich verwendet einen Schraubenschlüssel statt der Glocke. Die vom
Benutzer positiv bewerteten aktuellen Texte bleiben erhalten; sie sind nicht
wortgleich mit der Anwendung. `MaintenanceView` zeigt „Wartungsarbeiten“ und je nach
Zustand „Die Anwendung wird gerade gewartet. Bitte versuchen Sie es später erneut.“
oder die vorübergehende Nichtverfügbarkeit. Die Anwendung nennt die Loginaktion
„Administrative Anmeldung“; der Prototyp bleibt bei „Anmelden“. Das ändert keine
Zugangsrechte oder realen Abläufe. Der abgelaufene eingeschränkte Anmeldeschritt
verwendet nun ebenfalls den gelben Ausrufekreis; technische Meldungen das rote
Warndreieck und Hinweise den blauen Infokreis. Die private Inbox-Glocke bleibt erhalten.

Die geerbte Gerätetypografie gilt nur für die normale Recovery-Geräteliste.
Ergebnisansichten wie „Anmeldeschritt abgelaufen“ verwenden den gemeinsamen
13-px-Hinweistext in Secondary-Farbe; die Listenregel darf dort nicht die
16-px-Basisschrift und helle Primärfarbe erzwingen. Die übrige Gestaltung dieser
beiden Seiten ist vom Benutzer positiv bewertet; die Schriftabweichung ist korrigiert.

Gezielt geprüft: beide Seiten samt den vorhandenen Recovery-Zuständen in Desktop,
Hoch-/Querformat (12 Kombinationen), ohne horizontalen Überlauf, Footer weiterhin
41 px. Schrift, Farben und Zeilenbausteine direkt mit der gerenderten Gerätereferenz
verglichen. Gespeicherten Zugang entfernen lässt drei aktive Sitzungen und die
gesperrte Fortsetzung unverändert. Aktive Sitzung entfernen gibt einen Platz frei;
Aktualisieren erhält den Demozustand. Nach vollständiger Abmeldung ist die leere
Sammelaktion deaktiviert. Keine echte Sitzung oder Anmeldung verändert.

Vorschau: `http://127.0.0.1:60825/preview/visitors.html`, mit externer Seiten-/
Zustandswahl und Desktop/Smartphone Hoch-/Querformat. Diese Prüfsteuerung gehört
nicht zur späteren Oberfläche. Der gemeinsame Download `cosmos-portalprototyp.zip`
enthält Besucher- und angemeldeten Bereich einschließlich eingebetteter Schriften/SVGs.
Lokale Quellen unter `target/designkritik-stitch/portal/`: `build_public.py`,
`public_reference.py`, `public.css`, `public.js`, `public-controls.js`,
`public-notifications.js`, `public-viewer.js`, `validate_public.py` sowie die bestehenden
Referenzquellen. Nach einer bewussten Neugenerierung der angemeldeten Referenz zuerst
`build_portal.py`, danach `build_public.py` ausführen. In dieser Runde wurde nur
`build_public.py` ausgeführt. Die freigegebenen Referenzen sind auf Benutzerwunsch
unter [`docs/design/frontend-referenzen/`](../design/frontend-referenzen/README.md)
als feste HTML-Momentaufnahmen gesichert: `portal.html` und `public.html` mit
eingebettetem CSS, Schriften, SVGs und Demo-Skripten sowie `index.html` und
`visitors.html` für die gemeinsame Vorschau-Steuerung. Alle vier Dateien bleiben
im selben Ordner offline nutzbar und sind unabhängig von `target/`.
Vor einem Clean weiterhin benötigte Arbeitsgeneratoren und Rohentwürfe bewusst
sichern; diese sind nicht Bestandteil der HTML-Sicherung. Vorhandene Quellen und
Artefakte wurden nicht bereinigt oder gelöscht.

Die gesicherten Exporte enthalten eine rein funktionale Korrektur der
Besucher-Demologik: Ein nachlaufendes Hash-Ereignis setzt einen bereits gewählten
Fehlerzustand nicht mehr zurück. Erfolgreiche Registrierungs-/Reset-Beispiele
leeren auch zuvor über das Augensymbol sichtbare Passwortentwürfe. Die abgenommene
Gestaltung bleibt erhalten. `docs/design/frontend-referenzen/pruefen.mjs` prüft
die Exporte ohne Zusatzpakete sowie diese Regressionen und die Absender-/Origin-
Grenzen in einer kleinen DOM-Testumgebung für HTTP und lokale Dateien. Eine neue
gerenderte Browserrunde wurde für diese beiden Reviewkorrekturen nicht ausgeführt.
Die öffentliche Sprachauswahl ist nach Benutzerkorrektur unmittelbar im Header
platziert. Auswahl und Pfeiltasten/Enter werden direkt am Umschalter bedient;
die übrigen Auswahl- und Kalenderoverlays bleiben davon unabhängig. Die Demo bleibt Deutsch.

Tatsächlich geprüft: neun Grundansichten in drei Formaten (27 Ansichten) und
21 zusätzliche Erfolgs-/Link-/Fehlerzustände je Format (63 Zustandsprüfungen).
Kein horizontaler Überlauf, keine fehlenden Aktions-SVGs und keine sichtbaren nativen
Datums-/Geschlechtsauswahlen. Äußerer Footer überall 41 px, Padding 12 px/0,
Copyright Inter 12 px/16 px, zentriert, gleiche Farben und Kontur wie die feste
Referenz; Header 65 px. Der Login-Kartenkopf und die Kartenhöhe bleiben bei der
globalen Fehlermeldung unverändert. Geöffnete Menüs/Kalender liegen in den geprüften
Viewports; Sprachmenü sechs Pixel unter dem Auslöser, auch nach Größenwechsel.
Kalender ist im kurzen Querformat intern scrollbar. Auswahl per Tastatur,
Datumspfeiltaste/Enter, Escape und Fokusrückkehr geprüft. Checkbox-Auswahl,
lange synthetische Namen/Adressen, Bestätigungsabgleich und Passwortentwurfsleerung
geprüft. Zwei verschiedene Adressen ergeben dieselbe Passwort-vergessen-Rückmeldung.
Nur gespeicherten Zugang entfernen lässt drei aktive Sitzungen und die gesperrte
Fortsetzung unverändert; aktive Sitzung entfernen gibt einen Platz frei, Aktualisieren
erhält den Demozustand. Explizite E-Mail-Bestätigung wurde bedient.

Ruff-Format/Formatcheck, JavaScript-Syntax, Offline-/CSP-/ZIP-Prüfung und
`git diff --check` sind erfolgreich durchgeführt. `validate_public.py` prüft unter anderem
neun Szenen, acht Pflichtfelder, eindeutige IDs, keine Fremdressourcen/-skripte,
unterbundenen Netzwerk-/Formularversand und aktuelle Offline-Dateien. Temporäre
Browserbelege liegen unter `portal/alignment-checks/`. Die feste `preview/portal.html`
ist byteidentisch geblieben. Keine Anwendung, Datenbank, echten Zugangsdaten,
Mails, Tokens oder Sitzungen verwendet; keine Builds, JUnit, große QA oder Sonar.
`docs/status.md` bleibt die letzte große Testrunde. Reale Mobilgeräte,
Bildschirmtastatur und Screenreader sind damit nicht abgenommen. Die menschliche
Designsichtung der HTML-Referenz ist erfolgt. Logoentscheidung sowie Umfang und
Freigabe des Vaadin-Piloten bleiben offen.

**Service-Logo – gesonderte Stitch-Auswahl, noch nicht übernommen:**
Die ersten blau/cyanfarbenen A/B/C-Entwürfe sind vom Benutzer verworfen. Das
Servicezeichen darf ausdrücklich mehrfarbig sein und eine eigene Farbwelt haben.
Die neue, unabhängig abgerufene Stitch-Auswahl ist Screen
`94bb9052d0474c6f8bd38a037bdab905`, „Cosmos – Logo Exploration Board (Richtungen
D / E / F · Mehrfarbig)“: farbige Blüte, geschwungene Flügel und vierfarbiger Ring.
Stitch bevorzugt E; die gestalterische Auswahl bleibt beim Benutzer offen.

`portal/logo-options.html` zeigt die geprüfte native SVG-Geometrie jeder Variante
groß, unverändert in 16/24/40 px, neben `{ServiceName}` sowie auf hellem Grund.
Die Vektordateien sind einzeln herunterladbar. `prepare_logo_preview.py` übernimmt
nur erlaubte SVG-Elemente ohne Fremdskripte/-ressourcen; die Vergleichsansicht ist
lokal aufgebaut. Kein fester Servicename erfunden und keine ungeprüften Aussagen
zu garantierter Erkennbarkeit oder Markeneigenständigkeit übernommen. Das bestehende
Headerlogo und die feste Oberflächenpalette bleiben unverändert. Auswahl und
eventuelle Vereinfachung kleiner Details sind offen; keine automatische Übernahme.
Der Benutzer möchte D/E/F ausdrücklich als Kandidaten behalten. Die unveränderten
Vektorentwürfe sind deshalb unabhängig von temporären Buildausgaben unter
[`docs/design/service-logo/`](../design/service-logo/README.md) gesichert.
Diese positive Bewertung ist noch keine endgültige Logoauswahl.
Die endgültige Logoentscheidung ist auf Benutzerwunsch zurückgestellt. D/E/F
bleiben mögliche Platzhalter und sind keine Voraussetzung für die nächste
Designrunde. Der angemeldete Benutzerbereich gilt als feste Gestaltungsreferenz;
der nicht angemeldete Besucherbereich ist nun ebenfalls als feste HTML-Referenz abgenommen.
Dies schließt weder das Gesamtfeature noch die spätere Vaadin-Übertragung ab.

**Geräte & Sitzungen – HTML-Vorschlag zur Sichtung:**
`http://127.0.0.1:60825/preview/sessions.html` oder Seitennavigation. Vorhandene
Credits sind ausdrücklich nur für diese Runde freigegeben, nicht für den Besucherbereich.
Stitch-Screens `fda011c939ef46a4975640584b5febb4` „Cosmos – Geräte und Sitzungen“
und `bc0e4225a35c4b18b5a869f99efe932b` „Cosmos – Geräte & Sitzungen
(Bestätigungsdialog)“ sind unabhängig mit HTML/Bild abgerufen. Ihre Geräteanordnung,
Zustände und Bestätigungsstruktur sind in den gemeinsamen Rahmen übertragen;
zusätzliche helle Konturen, Fremdskripte und globale Rahmenänderungen nicht übernommen.
Lokale Quellen: `refine_sessions.py`, `sessions.css`, `sessions.js`.

Der gemeinsame 40-px-Panelkopf übernimmt das vorhandene kombinierte Geräte-SVG aus
der Anwendung, mit eindeutigen Masken-IDs für Kopf und Navigation. Zeilen zeigen
passende farbige Desktop-/Smartphone-Symbole. Drei synthetische Zustände: aktuelles
Gerät (grün, nicht abmeldbar), andere aktive Sitzung (blau, Anmeldung gespeichert) und
nur gespeicherte Anmeldung (neutral). Die zwei aktiven Sitzungen zählen getrennt
vom gespeicherten Zugang gegen das Limit drei. Rechte 36-px-Aktionen, Infohinweis
mit Info-Kreis und gemeinsame rechtsbündige Footeraktionen folgen der Referenz.
„Widerrufen“ hat die eindeutige zugängliche Bezeichnung „Gespeicherte Anmeldung
widerrufen“. Abgleich mit `AccountSessionsComponent` bestätigt diese vorhandene
Funktion; sie ist keine Erfindung von Stitch.

Der Bestätigungsdialog ist ein bewusst neuer Vorschlag nur für den Prototyp;
die Anwendung führt Sitzungsaktionen bisher direkt aus. Er entsteht aus derselben
Struktur wie der Benachrichtigungs-Confirm, mit vorhandener Danger-/Schließen-Gestaltung.
Einzelne aktive Sitzung, gespeicherter Zugang und Sammelaktion erhalten passende
Titel, Kontext und Hinweise. Abbrechen hat initialen Fokus. Bestätigen entfernt
ausschließlich synthetische Zeilen; die aktuelle Sitzung bleibt stets erhalten.
Aktualisieren erhält den Beispielsatz, Neuladen setzt ihn zurück. Keine echte
Sitzung, Datenbank oder externe Verbindung wird verändert.

Gezielt geprüft: Desktop 1440×1000, Smartphone 390×844 und Querformat 844×390,
lange Identität, kein horizontaler Überlauf, 160×36-px-Zeilenaktionen und erreichbare
Dialogaktionen bei scrollbar begrenztem Body. Abbrechen/Escape erhalten drei
Einträge/zwei aktive Sitzungen; Fokusrückkehr beobachtet. Einzelne aktive Abmeldung
ergibt zwei Einträge/eine aktive Sitzung; nur gespeicherten Zugang widerrufen lässt
den Aktivzähler bei zwei. Sammelaktion lässt das aktuelle Gerät/eine aktive Sitzung
übrig und deaktiviert Alle-anderen; Aktualisieren setzt dies nicht zurück.
Sicherheit bleibt erreichbar. Ruff, JavaScript-Syntax, Offline-/ZIP- und Diffprüfung
erfolgt. Kein Anwendungsbuild, JUnit, Browserjourneys oder Sonar. Gerätebereich
und ergänzter Besucherbereich gehören inzwischen zu den gestalterisch abgenommenen
HTML-Referenzen. Gesamtfeature und technische Übertragung bleiben offen.

Die Limitplakette verwendet auf Benutzerwunsch die vorhandene gelbe Hinweisrolle.
Hinweis und Geräteaktionen sind durch eine durchgehende horizontale Linie getrennt.
`panel-footers.css` bündelt Profil-/Geräteaktionsleisten mit 16 px unterem Abstand
und gleichen seitlichen Einrückungen. Beim Profil lagen zuvor 32 px Formularpadding
unter dem Speichern-Button, bei Geräten 18 px. Die Profilaktion ist nun als eigene
Leiste unter dem Formular angeordnet; Felder, Demo-Speichern und andere Szenen bleiben
erhalten. Für aktuelle gerenderte Quellen enthalten die Vorschauframes eine aus
ihrem Inhalt berechnete Revisionskennung, damit Neuladen keine alte Framefassung zeigt.

Erneuter Codeabgleich in `AccountSessionsComponent.sessionAction` und
`SessionManagementService.snapshot`/`revokeOwned`: aktive Sitzung und gespeicherte
Anmeldung können zum selben Eintrag gehören. Abmelden beendet den aktiven Zugang
und widerruft gegebenenfalls auch dessen gespeicherten Zugang; ein ausschließlich
gespeicherter Eintrag widerruft nur die spätere automatische Anmeldung. „Aktiv“ ist
eine vom vorhandenen Aktivzustand abgeleitete Prototypkennzeichnung. Die bisherige
Checkboxbeschriftung „Login speichern“ wird dort als Zustand „Anmeldung gespeichert“
verständlicher formuliert. „2 aktive Sitzungen“ bleibt grammatisch kleingeschrieben.
Desktop und Smartphone erneut geprüft: gelbe Limitwerte, volle Trennlinie, jeweils
16 px untere Innenkante, kein horizontaler Überlauf, Gerätebuttons 36 px und
Profil-Demospeichern weiterhin ohne echte Speicherung. Keine Anwendungstests gestartet.

**Gezielter Stilabgleich als Rückmeldung zur Geräterunde:** Der Benutzer bestätigt
die Geräteanordnung grundsätzlich, benennt aber uneinheitliche Panel-/Contentflächen
und Buttonfarben/-größen als gemeinsame Stilentscheidung. Gewählt und in die normale
HTML-Referenz übernommen sind der dunklere Inhaltsbereich und cyanfarbene
Secondary-Icons. Der Vergleich unter `http://127.0.0.1:60825/preview/style.html`
bleibt optional verfügbar und startet ebenfalls mit der gewählten Kombination.
Keine zusätzliche Besucheransicht.

Gemessene Ausgangslage: Benachrichtigungslistenbody `rgba(5,8,17,.3)` bei transparentem
äußerem Panel, die anderen Inhalte innerhalb blickdichter Panelkarten; Ändern 12 px,
Gerätezeilenaktionen 11 px, neutraler Text und teils cyanfarbene, teils helle/graue
Aktionsicons. Diese Unterschiede stammen aus den einzelnen Entwurfsrunden und sind
keine einheitliche Bedeutungskennzeichnung.

Stitch-Screen `95b56505514c43139a5bb804878bc720`, „Cosmos – Stilabgleich &
Vereinheitlichung“, ist unabhängig mit HTML/Bild abgerufen. Seine ausdrücklich
formulierte Empfehlung lautet: blickdichte Hauptpanels und neutrale Secondary-
Icons wie der helle Text; Cyan für Fachicons/ausgewählte Zustände. Die Musterseite
zeigt ebenfalls die eindeutigen parallelen Sitzungszustände. Ihre Bezeichnungen
„verbindlich“ und „produktiv“ sind keine Benutzerabnahme; abweichende Rohvorgaben
wie 38-px-Primarybuttons sind nicht übernommen.

Der lokale Vergleich zeigt A (durchgehender Panelgrund) und B (dunklerer, ebenfalls
blickdichter Inhaltsbereich in Canvasfarbe), jeweils wahlweise neutrale oder
cyanfarbene Secondary-Icons. Sternenhintergrund bleibt außerhalb der Hauptpanels.
Gleiche sekundäre Rollen verwenden nun 12 px/600, 36 px Texthöhe, 16 px Icons,
gleiche Text-/Kontur-/Hoverwerte. Primary bleibt blau/weiß, Danger rot, dekorative
Kategorie-/Geräteicons cyan, Disabled gedimmt. Kompakte Iconaktionen/Pagination
behalten ihre begründete kleinere Dichte. Die Standardwerte liegen zentral in
`reference-controls.css`; Vergleichsabweichungen werden ausschließlich über
`style-preview.js`, `style-controls.html` und `style-viewer.js` gewählt. Ohne diese
Vergleichssteuerung gilt automatisch die freigegebene Kombination.

Auch „Profilbild bearbeiten“ ist in die gemeinsamen Secondary-Regeln aufgenommen.
„Mein Konto“ im Benutzermenü besitzt das exakt aus dem Header kopierte Zahnrad;
als Primary ist es weiß. Header-Zahnrad und Menübutton heißen beide „Mein Konto“,
einschließlich Tooltip/zugänglichem Namen, und führen zu Persönliche Daten.
Startseitenicon im Benutzermenü ist cyan, Abmelden bleibt rot. Der Nachrichtenbereich
der Glockenvorschau erhält ebenfalls den dunkleren Grund; Kopf/Fuß behalten die
Overlayfläche. Das Benutzermenü erhält keine zusätzliche künstliche Contentfläche.

Die Glockenvorschau trennt ihren vollbreiten Verlaufsbutton und den
Aufbewahrungshinweis durch eine durchgehende horizontale Linie, auch mobil.
Dialoge verwenden dasselbe Kontur-Schließen aus `portal/close-icon.svg`:
Kreis mit Kreuz, cyan im Kopf/auf Secondary, weiß auf Primary. E-Mail, Passwort,
Profilbild, Nachrichtendetail und beide Bestätigungsdialoge teilen diese Quelle;
keine gefüllten grauen Schließenkreise mehr. Bestehende Fokusrahmen, Escape und
Schließaktionen bleiben erhalten. Die Kopfbuttons und das Nachrichtendetail
sind in der Vorschau geprüft; keine Änderung am Vaadin-Theme.

Für die Herkunft „System“ ist Indigo vom Benutzer freigegeben und umgesetzt,
einheitlich in Inbox, Glockenvorschau und Nachrichtendetail: heller Text
`#A5B4FC`, Fläche `rgba(99,102,241,.10)` und Kontur `rgba(129,140,248,.30)`.
Administration bleibt cyan; Herkunft
und Lesestatus sind weiterhin unabhängig. Auf ausdrücklichen Benutzerwunsch
bewertet Stitch dies im Screen `f92d1b69269341e9a428480cf8422013`,
„Cosmos – Entscheidungsvorlage: Herkunftsbadge System“. Stitch hält Violett für
geeignet, bevorzugt gestalterisch jedoch kühleres Indigo (`#A5B4FC`), näher an der
blauen Grundpalette. Diese ästhetische Einschätzung ist kein Nachweis für die
weitergehenden Wahrnehmungsbehauptungen des Rohentwurfs. Ein kleiner lokaler
Vergleich `portal/system-badge-comparison.html` zeigt beide Farbwerte auf gleichen
Flächen. Im Portal gilt die freigegebene Indigo-Variante; die Farbwahl ist abgeschlossen.
Keine neuen Funktionen, Statusfarben oder Rohentwurfsinhalte übernommen.

Die gespeicherten Zugänge sind bereits verständlicher parallel benannt:
„Aktive Sitzung · Anmeldung gespeichert“ und „Keine aktive Sitzung · Anmeldung
gespeichert“. Es ist dieselbe Speicherfunktion, aber mit bzw. ohne laufende Sitzung;
es gibt keine zweite Remember-me-Funktion. Das aktuelle Gerät bleibt gesondert markiert.

Geprüft: identische gemessene Hintergrundwerte aller vier Contentbereiche in A,
Umschaltung zu Canvasfarbe in B, neutrale/cyanfarbene Aktionsicons, gemeinsame
12-px-Typografie und 36-px-Dialogaktionen; Referenzmodus stellt die freigegebenen
Standardwerte wieder her. Seitennavigation, Sicherheitsdialog und Smartphone ohne
horizontalen Überlauf geprüft. Syntax-/Ruff-, Offline-/ZIP- und Diffprüfungen
durchgeführt. Die gemeinsame Stilwahl ist übernommen; kein Anwendungstheme geändert
und keine Gesamt-QA gestartet. Weitere gestalterische Details bleiben separat abstimmbar.

#### Bereits geprüfte Grundlage des kombinierten Entwurfs

**Erste kombinierte Vorschau fertig; Rückmeldung in 3a–3c berücksichtigt:**

- Maßgeblicher Screen im Projekt „Workflow testen – Cosmos“:
  `3491682885136864837`, **„Kombination C + B · Vorschau“**. Der erzeugte
  Stitch-Rohentwurf `84d8420ec22449859b2f6ebe205ba420` bleibt als Ausgangspunkt
  erhalten; maßgeblich ist die anschließend lokal präzisierte und neu importierte
  Vorschau. Keine vorhandenen Screens gelöscht.
- Header und Content besitzen bei `1440×1000` denselben begrenzten Innenbereich
  (gemessene Kanten bei 100 und 1340 CSS-Pixeln). C-Grundfarben und cyanblauer
  Hauptakzent wurden gezielt beibehalten; die leicht abweichenden Aliasfarben der
  Stitch-Ausgabe wurden an C angeglichen. Die beiden C-Hintergrundbausteine sind
  übernommen. Der Footer ist bei Desktop und schmaler Ansicht horizontal zentriert.
- Die B-Seitennavigation ist in Portalziele (Startseite, Benachrichtigungen) und
  Kontobereiche gegliedert. Keine doppelten horizontalen Tabreihen und keine große
  zusätzliche C-Profilkarte neben dem Formular. Die Identität sitzt kompakt in der
  Seitenleiste; Home wird nur als Navigationsziel dargestellt, ohne neuen Seiteninhalt.
- Geprüft bei `1440×1000` und `390×844`: keine horizontalen Überläufe; alle fünf
  Profilfelder und Ausgangswerte, readonly E-Mail, zwei Sprachoptionen, Status,
  Profilbild-/Speicheraktion sowie die bestehenden Kontoziele erhalten. Schmal
  stehen die Felder untereinander. Die Speicheraktion ist durch Scrollen erreichbar.
- Das schmale Menü wurde geöffnet und mit Escape geschlossen; `aria-expanded`
  und Beschriftung folgen dem Zustand, der Fokus kehrt zum Menüsymbol zurück.
  Diese kleine lokale Vorschau-Interaktion ersetzt keine Vaadin-Funktionsprüfung.
  Die eigentlichen Navigationsziele und Speichern sind weiterhin nur Designreferenzen.
- Nach Einbetten von Styles/Inter und Entfernen externer Skripte und Styles wurde
  die Vorschau erneut geladen und geprüft. Ein überprüftes lokales Menüskript bleibt;
  es ruft keine Dienste auf. Kein Anwendungsstart, keine Mail-/Datenbankaktionen,
  keine JUnit-/Browserjourney-Suite und keine globale Theme-Änderung.
- Arbeitsdateien unter `target/designkritik-stitch/combined/`: `preview/kombination.html`
  ist die eigenständig responsive Datei, `desktop.html`/`smartphone.html` stellen
  feste Vorschaugrößen bereit. `cosmos-kombination.zip` enthält diese drei Dateien
  und Bedienhinweise. Belege: `desktop-vorschau.jpg`, `smartphone-vorschau.jpg`,
  `smartphone-formular-footer.jpg`, `smartphone-menue.jpg`. Der lokale Vorschau-Server
  wurde nach der Prüfung beendet. Quellen und fremde Rohantworten bleiben ignoriert.
- SHA256 der hochgeladenen Vorschau:
  `0C5E220A8D3C390CF0B5836421BC6B4A63F524AC1C1A0F9F056C387CA8EFEB33`.
  Projekt-Ruff-Formatierung und Formatprüfung der lokalen Python-Helfer bestanden.
  Der hochgeladene Endstand wurde zusätzlich unabhängig per CLI abgerufen.

Die gemeinsame Rückmeldung ist in 3a–3c umgesetzt; der aktuelle Portalprototyp
steht oben und ist als Gestaltungsreferenz abgenommen. Offen bleiben Umfang und
gesonderte Beauftragung einer Vaadin-Spielwiese.
Reales Smartphone, EN-Layout, Kontrastmessung, Screenreader sowie echte Fachabläufe
sind nicht abgenommen.
Paket 3 und das Gesamtfeature sind damit noch nicht abgeschlossen.

#### Ergänzung vor 3d – Administration als HTML-Referenz

**Abgenommen: Benutzerverwaltung, überarbeitete Variante 2.** Die maßgebliche
HTML-Referenz liegt gemeinsam mit Portal und Besucherbereich unter
`docs/design/frontend-referenzen/`: `admin-users.html` als Vorschau und
`admin.html` als eigenständige Oberfläche. Aktuelle bearbeitbare Quellen unter
`quellen/benutzerverwaltung/` sind unabhängig von target gesichert. Die vorherige
Inspirationsablage mit alternativen Varianten und A/B-Steuerung ist auf Benutzerwunsch
entfernt. Die Gestaltung der beiden anderen Referenzen bleibt erhalten; die Vorschauen
sind verlinkt, und die Portalreferenz unterstützt den gezielten Home-/Inbox-Einstieg.

Die Benutzerverwaltung enthält sieben Informationsspalten, 24 synthetische Konten,
Sortierung, 10/20/50-Einträge-Pagination wie bei Benachrichtigungen und Anlegen-,
Bearbeiten- und Löschdialoge. Eigene und geschützte Konten behalten die vorhandenen
Einschränkungen. Die Sidenav übernimmt den Portalstandard mit cyanfarbenen Icons,
heller Schrift und den Gruppen ADMINISTRATION/PORTAL. Noch nicht gestaltete
Adminziele bleiben deaktiviert. Kontenzähler und Filterzähler sind blau;
Benutzer hinzufügen verwendet das Personen-Plus, Zurücksetzen den Dangerstil.

Der rechte Filter-Drawer übernimmt Smartphone-Sidenavverhalten, eine linke
Rahmenlinie, Panelkopf/-fuß wie persönliche Daten und dunklen Inhalt.
Der untere Abstand entspricht nur der sichtbaren Copyright-Footerfläche; bei
nicht sichtbarem Footer endet der Drawer am Viewportrand. Header-/Footerklicks
schließen mit Verwerfen. Die volle Backdrop-Klickfläche bleibt dort transparent;
Neupositionierung auch bei Inhaltsänderungen und Rotation.
Suche und
Kriterien werden gemeinsam über Anwenden übernommen. Abbrechen, Escape und
Hintergrundklick verwerfen den Entwurf einschließlich Suche; Zurücksetzen leert
nur den Entwurf, die Tabelle ändert sich erst mit Anwenden. Abbrechen erhält die
zuletzt übernommenen Filter auch nach Zurücksetzen. Aktiv ist Alle/Ja/Nein; Status und die vier bestehenden Rollen sind
Mehrfach-Badges. Innerhalb einer Gruppe gilt ODER, zwischen Gruppen UND; keine
oder alle vier Werte schränken nicht ein. Unausgewählte Rollen verwenden Secondary,
Auswahl wird durch Fläche, Kontur und Häkchen erkennbar. Der 36px-Gruppenzähler
passt zum Zurücksetzen; die externe aktive Filterzeile und zusätzliche Erklärungen
sind entfernt. Die fünf Labels übernehmen den Stil der persönlichen Daten.
Datum/Kalender und optionale Uhrzeit bleiben vorhanden; die Uhrzeit hat keine
sichtbare Beschriftung und wird über eine Hinweisbox im E-Mail-Dialogstil erklärt.

Standardaktionen und Löschdialoge bleiben im gemeinsam abgestimmten Referenzstil:
Abbrechen mit schlichtem X, Kopf-Schließen im Kreis, dunkler Inhalt, Frage und
Erklärung zusammen sowie Kontextkarte mit Avatar/Name/E-Mail. Der rote
Bestätigungsbutton heißt Löschen. Keine neuen Fach-, Rechte- oder Sicherheitsregeln.
Nur HTML-Simulation; kein Vaadin-/Java-Umbau, globaler Themewechsel, echte Anmeldung,
Datenbank oder Mailaktion. Logoentscheidung bleibt zurückgestellt.

Aus der gezielten Stitch-Nachsicht sind die Feldicon-Regel und getrennten
Passworthinweise nach Benutzerfreigabe übernommen. Gemeinsame Feldregeln und
Anwendungs-Platzhalter gelten nun in allen drei HTML-Referenzen; normale
Aktionsbuttonicons bleiben unverändert. Nach weiterer Stitch-Rückfrage sind
dekorative linke Feldsymbole entfernt; Suchlupen und rechte Bedienicons bleiben.
Sonstige Dialoganordnung beibehalten,
Rollenhinweis direkt bei der Auswahl und das grüne Badge „Dein Benutzer“ unter dem Namen. Das aktuelle
Ergebnis steht zur gemeinsamen Sichtung bereit. Zusammenfassung und private Screens unter
`docs/design/frontend-referenzen/quellen/benutzerverwaltung/stitch-dialogvorschlag.md`.
Der fehlende Pflichtpunkt bei Geschlecht und das grüne Badge „Dein Benutzer“ in der Tabelle sind
bereits in die Referenzen übernommen. Den bestehenden Löschdialog erhalten.

Danach Dashboard, Konfiguration, Mailtest und berechtigten
Benachrichtigungsversand mit Stitch auf derselben Grundlage gestalten. Dafür die
bestehenden Anwendungsquellen und Fachverträge vor dem jeweiligen Entwurf prüfen.
Die Konfiguration umfasst GENERAL, SECURITY, TOKENS, USER_STATUS, NOTIFICATIONS,
EMAIL_SENDER, SMTP und QUEUE. Benachrichtigungsversand setzt canSend voraus.
Keine erfundenen Dashboard-Kennzahlen oder Komfortfunktionen übernehmen.
Ergebnisse unabhängig abrufen, mit vorhandenen Komponenten einpassen und gemeinsam
sichten. Erst nach dem vollständigen HTML-Bild Umfang/Reihenfolge des Vaadin-Piloten
abgrenzen; dessen Umsetzung und die übrigen Adminseiten sind noch nicht freigegeben.

Gezielte Prüfungen der ausgewählten Variante umfassen kombinierte Status-/Rollenfilter,
gemeinsames Anwenden der Suche, Abbrechen/Zurücksetzen, ungültiges Datum, leere
Treffer, Sortierung und Pagination sowie Desktop/Smartphone hoch/quer. Labels und
Infoausrichtung sind gerendert gemessen. Archivprüfung kontrolliert CSP, Scripts,
Links und die gezielten Portal-Einstiegsrouten. Dies ist keine große QA;
`docs/status.md`, offene echte Geräte-/Screenreader-/EN-Prüfungen und der zentrale
Testbedarf bleiben erhalten. Kein Commit, Merge oder Push aus dieser Abnahme ableiten.

**Vorgeschaltete Komponentenübersicht zur Sichtung:**
`preview/dialogs.html` und `preview/notifications.html` bieten wie die Besuchervorschau
eine gruppierte Auswahl, Vor/Zurück, Wiederöffnen und drei Prüfgrößen. Die separate Sicherung dieser Übersicht wurde vom Benutzer aus der Projektablage
verschoben; sie wird nicht neu angelegt. Die freigegebenen Bausteine und ihre Quellen
stehen in den Frontend-Referenzen. Die temporäre Übersicht enthält 50 Dialog-/Overlay-/Panelauswahlen
(48 Bestands-/Standardbeispiele plus zwei Vergleiche) und 82 Meldungsauswahlen
(57 Anwendungsaufrufer mit 58 Ergebniszweigen, 20 Standardmuster, vier Vergleiche).
Alle aktuellen nativen HTML-Dialog-IDs sowie Dialogklassen/Instanziierungen und
Notification-Aufrufer im Anwendungscode sind gegen das Inventar abgeglichen.
Gerät abmelden, gespeicherte Anmeldung widerrufen und alle anderen abmelden sind
getrennt enthalten, ebenso Recovery als ausdrücklich nichtmodaler Schritt.

Stitch-Neuanlage `6337c98cfbc04e7ab5e4d01f9e0b55da` wurde getrennt erzeugt und
unabhängig erneut abgerufen. HTML-Upload war mit 403 blockiert; Original-Downloads
verlangen Anmeldung (401), der Webzugang war nicht angemeldet. Die anschließende
Textkritik wurde gelesen und gesichert; lokale Vergleichsbeispiele daraus sind
ausdrücklich keine visuell geprüften Originalexporte. Entscheidungen betreffen
Warnakzent beim Abmelden, Sperrsymbol beim Widerrufen, Warn-Dreieck statt Glocke
beziehungsweise aktuellem Hinweis-Kreis und kleinere 16px Glyphen in 28px
Notificationflächen. Keine Fach-/Timer-/Stapelregeln oder unpräzisen Löschfolgen
aus Stitch übernehmen. Vorschläge bleiben getrennt; Logos sind unverändert.

Die vom Benutzer beauftragte Dialogdurchsicht ist in den HTML-Referenzen und
Übersichten synchron eingepasst: gleiche Footerbuttonbreiten bei E-Mail, Passwort
und Profilbild; dunkler Nachrichtendetail-Inhalt; Gerätebestätigungen im gemeinsamen
Aufbau mit Geräteicon, Sitzungsdaten und Zustandskennzeichen; Nachrichtenkontext
mit Herkunft, Datum/Uhrzeit und Lesestatus. Fachliche Aktionen bleiben Abmelden,
Widerrufen beziehungsweise Löschen. Die persönlichen Daten enthalten nun die
Geschlechtsauswahl entsprechend dem vorhandenen Dropdownstil. Statusüberschrift
in Status- und Avatarmenü cyan; kein zusätzlicher Erklärungstext bei Geschlecht.
Kontexticons mittig zum gesamten sichtbaren Textblock; E-Mail-Aktionen gleich breit
und einzeilig, auf schmalen Smartphones untereinander. Uhrzeitlisten ohne
zusätzlichen Punkt. Änderungsquellen außerhalb von
target unter `docs/design/frontend-referenzen/quellen/dialogdurchsicht/` erhalten.
16 betroffene Auswahlen in drei Größen gezielt nachgeprüft, Buttonbreiten und
Inhaltsfarbe gemessen; neue Auswahl mit End/Enter/Escape in der Direktreferenz
bedient. Frame-Tastaturgrenze bleibt im zentralen Testbedarf dokumentiert.

Geprüft: alle 49 und 82 Auswahlen in Desktop/Hoch-/Querformat (147 + 246
Sicht-/Überlaufprüfungen), ausgewählte Escape-/Abbrechen-/Fokusfälle, modale
Meldungen, Originaldauer und Wiederöffnen. Der Frame wird pro Aufruf neu erzeugt;
Antworten alter Szenen/Läufe oder fremder Absender/Origins werden verworfen.
Ruff, JS-/CSP-/Offline-/Quellenhash-/ZIP-Prüfung und Neubau ausschließlich aus
gesicherten Eingaben bestanden. Der verschachtelte Escape-Nachtest ist unter
`docs/testbedarf.md` offen; keine Behauptung einer vollständigen Bedienabnahme.
Kein Anwendungsstart, keine große QA und keine Änderung an `docs/status.md`.
Die Benutzerverwaltung ist inzwischen als Referenz abgenommen; als Nächstes folgen
einzeln Dashboard, Konfiguration, Mailtest und berechtigter Benachrichtigungsversand.

#### 3d – Nach gemeinsamer HTML-Bewertung in Vaadin erleben

- Gewählte Grundlage: Header, Footer und Farben aus C mit Content/Seitennavigation
  aus B verbinden. Copyright im Footer zentrieren; die doppelten horizontalen
  Tabreihen entfallen. Zuerst einen zusammenhängenden kombinierten Entwurf zeigen,
  bevor einzelne Bausteine in die Vaadin-Spielwiese übertragen werden.
- Pro gezielter Stitch-Runde die benannten Probleme ändern und gelungene
  Eigenschaften festhalten. Weitere Runden nach gemeinsamer Rückmeldung, bis die
  Referenz passt. Bei mehreren widersprüchlichen
  Änderungswünschen erneut priorisieren, statt alles gleichzeitig zu vermischen.
- Bereits vor dem Java-Umbau die gewählte Richtung zusätzlich schmal darstellen:
  Navigation, Formularreihenfolge und erreichbare Aktionen am Smartphone prüfen.
  Ein Desktop-Entwurf, der nur als geschrumpftes Bild funktioniert, wird zuerst korrigiert.
- Gewählte Screens und ihre Zuordnung sichern; relevante Gestaltungsregeln aus
  dem Entwurf für den Profilpiloten festhalten. Ein umfassender bearbeitbarer
  `DESIGN.md`-Austauschstand ist eine optionale Vertiefung. Ein solcher Stand
  beschreibt den Versuch und wird noch nicht zum gültigen Anwendungstheme.
- Eine branchlokale, ausdrücklich aktivierbare Spielwiese mit echten Vaadin-
  Controls und rein lokalen Beispieldaten bauen. Der Kern enthält Konto/Profil,
  einen repräsentativen Dialog und einen Fehlerzustand. Weitere Kontobereiche
  können zur Orientierung als klar benannte Platzhalter erscheinen, ohne ihre
  Abläufe bereits nachzubauen.
- Die für diese drei Beispiele nötigen Controls dürfen bedienbar sein. Speichern
  bleibt eine klar bezeichnete Simulation ohne Verbindung zu fachlichen Diensten.
  Keine zweite Fachlogik bauen.
- Vorhandene präsentierende Bausteine wiederverwenden. Komponenten, die direkt
  echte Dienste aufrufen, nicht ungeprüft in die Spielwiese übernehmen. Falls eine
  kleine Trennung nötig ist, Umfang vorab benennen und gezielt prüfen.
- Versuchsstile unter einer eigenen Spielwiesenklasse kapseln. Overlay-/Popover-
  Klassen ausdrücklich mitführen, weil diese außerhalb der View liegen können.
  Kein pauschaler `:root`-/`html`-/`body`-Override oder dauerhaftes Ändern globaler
  Cosmos-Tokens. Beim Verlassen dürfen keine Versuchsstile hängen bleiben.
- Baseline und Entwurf bei gleichem Szenario umschaltbar machen; ein Dialog oder
  zwei widersprüchliche Themes dürfen nicht aus der vorherigen Variante fortleben.

**Optional später / Nice to have im Vaadin-Piloten:** Sicherheit/Sitzungen als
bedienbare Vaadin-Beispiele, weitere Dialoge und Zustände sowie ein umfassenderer
DESIGN.md-Austausch. Im vorangehenden HTML-Portal gehören diese Benutzerbereiche
bereits zum geplanten Umfang. Auch im Vaadin-Piloten
bleiben Passwortwechsel, Widerruf und Versand eindeutig simuliert und ohne fachliche
Seiteneffekte. Pagination ist im HTML-Portal enthalten; ihre Vaadin-Transferprobe
gehört zu Paket 4.

**Ergebnis:** Der Benutzer beurteilt die Gestaltung mit den tatsächlichen
Vaadin-Bedienelementen. Abweichungen vom Stitch-Bild werden erklärt. Ein schönes
Stitch-Preview allein gilt nicht als erfolgreiche Integration.

### Paket 4 – Optional später: Tragfähigkeit an weiteren Beispielen prüfen

Dieses Paket ist eine erhaltene Vertiefungsoption für den Vaadin-Transfer, keine
Pflicht vor der HTML-Bewertung. Die Inbox-Vorschau gehört bereits zu 3b/3c.
Erst nach positivem HTML-Vergleich und passendem Folgeauftrag den Entwurf auf
eine echte Vaadin-Inbox-Szene und die Komponentenprobe übertragen.
Schwerpunkt sind dichte Inhalte, Pagination, Suche/Filter, eindeutige
gelesen/ungelesen-Zustände, ein Dialog sowie Formularfehler. Keine vollständige
Administration nachbauen.

- Leere, normale und längere Inhalte; Lade-/Fehler-/Erfolgszustand; Fokus, Hover,
  Auswahl, deaktiviert und schreibgeschützt unterscheiden.
- Desktop, schmale Ansicht, Zoom und DE/EN prüfen. Kein horizontales Seitenscrollen
  als unbeabsichtigte Folge; ein begründet horizontal scrollbares Grid separat bewerten.
- Hintergrundbewegung und reduzierte Bewegung prüfen. Nicht funktionale Effekte
  nur behalten, wenn sie die Benutzung nicht stören.
- Bestehende passende Tests, etwa `CosmosDesignBrowserTest`,
  `AccountSettingsBrowserTest` und Benachrichtigungsfälle, vor Ergänzungen prüfen.
  Designabhängige Erwartungen bei freigegebenen Änderungen fachlich anpassen;
  Sicherheits-/Funktionsassertions nicht abschwächen.

**Ergebnis:** gemeinsamer Vergleich mit dem alten Stand und benannte Grenzen.
Eine ungelöste Bedienhürde kann nicht durch einen guten Schönheitseindruck
ausgeglichen werden. Weitere Designrunden nur mit einer konkreten neuen Hypothese.

### Paket 5 – Versuch bewerten und bewusst abschließen oder weiterführen

Diese Entscheidung ist bereits nach dem HTML-Portal aus 3c möglich; die technische
Tragfähigkeit in Vaadin wird erst mit 3d beurteilt. Paket 4
und die anderen optionalen Vertiefungen sind dafür keine Voraussetzung. Der
Benutzer entscheidet zwischen:

1. **Übernehmen:** gewünschte Bausteine und Reichweite ausdrücklich festlegen.
   Erst dann ein begrenztes Übernahmepaket in den gemeinsamen Standard planen;
   Anwendungen der neuen Tokens/Komponenten und betroffene Regressionen benennen.
2. **Weiter erkunden:** brauchbare Teile festhalten, den nächsten Versuch klein
   eingrenzen und den bestehenden Plan auf den offenen Stand bringen.
3. **Verwerfen:** Anwendung auf `main` bleibt der Ausgangspunkt. Nützliche
   Erkenntnisse nur auf Wunsch übernehmen; Experimente und externe Designs nicht
   ohne Auftrag löschen. Fremde/uncommittierte Arbeit bleibt erhalten.

Die lokale Spielwiese ist kein automatischer Auftrag für einen globalen Rollout.
Bei Übernahme erst nach Freigabe gemeinsame Theme-Dateien und `docs/theme.md`
zusammen pflegen und ersetzte Versuchsoverrides entfernen. Technischen Stand,
gemeinsame Designabnahme und Git-Integration getrennt berichten.

## Zusammenarbeit und konkrete Auftragsbeispiele

Codex übernimmt Analyse, Vorbereitung der bereinigten Vorlagen, präzise Prompts,
verfügbare Automatisierung, Vergleich, Übertragung in Vaadin und technische Prüfung.
Stitch liefert Varianten und Verfeinerungen. Der Benutzer bewertet Wirkung und
Vorlieben sowie die Entscheidung über das neue gemeinsame Design.

Beispiele für spätere Aufträge, noch nicht ausgeführt:

- „Setze Paket 1 einschließlich isoliertem Anwendungsstart und gezielten
  Browseraufnahmen um. Richte die Stitch-Anbindung mit mir ein.“
- „Lass Stitch drei Konto-Varianten anhand des vorbereiteten Briefings erzeugen.“
- „Nimm B, behalte den Hintergrund von A und reduziere die abgesetzten Flächen.
  Zeige mir denselben Inhalt erneut.“
- „Übertrage die ausgewählte Variante in die Vaadin-Spielwiese.“

Ausgangsprompt für Paket 2, nach der Bestandsaufnahme konkretisieren:

> Überarbeite die beigefügte Kontoansicht eines allgemeinen Benutzerportals.
> Ziel sind Ruhe, klare Aufgaben und eine hochwertige eigenständige Wirkung.
> Erhalte die kosmische Identität mit CSS-Weltraum und Planeten. Verwende sehr
> dunkle blaue Grundflächen und wenige gezielte Akzente. Ordne Inhalte durch
> Hierarchie, Typografie und Abstand; vermeide viele verschachtelte farbige Karten.
> Verwende die vorhandenen Beispielinhalte, Profilfelder und Profilaktionen.
> Erhalte Avatar/Status und die Sprachauswahl. Andere Kontobereiche bleiben als
> Navigationsziele erkennbar, werden jetzt aber nicht als eigene Screens ausgearbeitet.
> Inbox und Pagination sind nicht Teil dieser ersten Richtungsvarianten;
> eine dort vorhandene Pagination darf nicht durch Endlosscrollen ersetzt werden.
> Erfinde keine zusätzlichen Features. Zeige zu jeder Variante ein zusammenhängendes
> Farbkonzept mit benannten Rollen. Das Zielsystem ist Vaadin Flow;
> die Ausgabe wird als Gestaltungsvorlage umgesetzt. Entwickle die vereinbarten
> Richtungen A, B und C bei gleichem Desktop-Viewport. Benenne jeweils die
> wichtigste gestalterische Idee und relevante Kompromisse. Zeige lesbare echte
> Beispieltexte statt dekorativer Platzhalterblöcke.

Verfeinerungen nennen Screen, konkretes Problem, gewünschte Änderung und ausdrücklich
die gelungenen Eigenschaften, die erhalten bleiben sollen. Nach dem Abruf wird
kontrolliert, ob der Auftrag wirklich umgesetzt wurde.

## Bewertung und Prüfgrenzen

Jede Variante wird gegen dieselben Aufgaben des jeweils gewählten Umfangs beurteilt.
Im geplanten HTML-Portal: zwischen den fünf Bereichen wechseln, Profilangabe
bearbeiten, Beispieldialoge öffnen/schließen, Status im Benutzermenü wählen,
aktuelle Sitzung erkennen, ungelesene Nachricht finden und zur nächsten Seite
wechseln. Lange Namen und beide Hintergründe sind im selben Szenario vergleichbar.
In frühen Bildern ist das eine Durchsicht, im Prototyp eine simulierte Bedienung;
echte Fachfunktion folgt erst bei Übernahme. Der spätere kleine Vaadin-Pilot prüft
zusätzlich die Übertragung auf native Controls anhand Profil, Dialog und Fehlerzustand.

| Kriterium | Entscheidende Frage |
| --- | --- |
| Ruhe und Hierarchie | Ist sofort erkennbar, wo ich bin und was wichtig ist? |
| Eigenständigkeit | Erkenne ich Cosmos wieder, und gefällt mir die Wirkung auch nach längerer Betrachtung? |
| Bedienbarkeit | Finde und verstehe ich die Aktionen ohne zusätzliche Erklärung? |
| Lesbarkeit | Sind lange Texte, Hilfen und Zustände angenehm erfassbar? |
| Konsistenz | Trägt dieselbe Gestaltung die fünf Portalszenen, gemeinsame Navigation, Popovers und Dialoge? |
| Technische Tragfähigkeit | HTML zunächst als Gestaltungsvorlage; funktioniert die spätere Übertragung mit nativen Vaadin-Controls und lokal begrenzten Styles? |

Der Benutzer benennt zu jeder Richtung konkret „behalten“, „stört“ und „ändern“.
Optional hilft eine grobe Bewertung von 1 bis 5 pro Kriterium; eine errechnete
Gesamtnote ersetzt keine Designentscheidung. Alle Varianten dürfen abgelehnt werden.

Technische Mindestziele für geänderte Bereiche:

- Textkontrast grundsätzlich mindestens 4,5:1, großer Text 3:1, unter Beachtung
  der definierten Ausnahmen; auf transparenten Flächen das zusammengesetzte Ergebnis
  prüfen. [WCAG 2.2: Textkontrast](https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html)
- Bedienelemente ausreichend groß/weit auseinander; WCAG-AA-Mindestziel 24×24
  CSS-Pixel mit den dort definierten Ausnahmen, für zentrale Touchaktionen möglichst
  großzügiger. [WCAG 2.2: Zielgrößen](https://www.w3.org/WAI/WCAG22/Understanding/target-size-minimum.html)
- Sichtbarer Tastaturfokus, sinnvolle Reihenfolge, Escape/Schließen und Rückkehr
  des Fokus; Zustände nicht allein durch Farbe vermitteln. Die Prüfung einzelner
  Kriterien ist keine Behauptung vollständiger WCAG-Konformität.
- Spielwiese ohne fachliche Seiteneffekte, ohne neue Zugriffsabkürzung und im normalen
  Anwendungsbetrieb nicht versehentlich aktiv. Unveränderte Seiten und deren
  portalisierte Overlays behalten ihre Gestaltung.

Gezielte Java-/Helperprüfungen und vorgeschriebene Stilprüfungen gehören zu den
späteren Codepaketen. Java 25 und Maven Wrapper mit Reactor verwenden; vor Tests
Java-Änderungen mit den zentralen `-N`-Zielen für Spotless und Checkstyle prüfen.
Browseraufnahmen, gezielte Browsertests und eine spätere große DE/EN-Runde haben
unterschiedlichen Umfang und werden im jeweiligen Auftrag ausdrücklich freigegeben.
Bei DE/EN dasselbe unveränderte aktuelle Image und die bestehende Isolation verwenden.
Der bisherige isolierte Build/Start, Profilaufnahmen und HTML-/Stitch-Prüfungen
sind durchgeführt. 3a–3c samt Originalaufnahmen und gezielten Prototypprüfungen
sind technisch bereit. Zusätzliche Bereiche ab 3d benötigen einen entsprechenden
Umsetzungsauftrag; keine große QA oder Sonar-Scans ableiten.

Sobald konkrete Implementierungen neue Testlücken erzeugen, diese in
`docs/testbedarf.md` aufnehmen und erst nach erfolgreichem Nachweis entfernen.
Menschliche Abnahmen orientieren sich an HUM-01 bis HUM-05 im manuellen Katalog.
`docs/status.md` bleibt der Stand der letzten großen QA; Entwurfssichtungen ersetzen ihn nicht.

## Artefakte, Wiederaufnahme und Aufwandbegrenzung

- Dieser Plan bleibt die einzige offene Featureplanung; TODO und Dokumentation
  verweisen darauf. Kein neuer Projekt-Skill allein für diesen Versuch nötig.
- Temporäre Screenshots und rohe Stitch-Exporte später in einem klar zugeordneten,
  ignorierten Bereich unter `target/designkritik-stitch/` ablegen. Dieser ist
  reproduzierbar und wird durch `clean` gelöscht; benötigte ausgewählte Referenzen
  vorher bewusst sichern. Keine großen Roh-HTML-/Bildserien ins Repository übernehmen.
- Kleine, bewusst gepflegte Designvorgaben/ausgewählte Belege erst bei Bedarf in
  einem benannten Featurebereich versionieren. Ein `DESIGN.md`-Austauschstand darf
  die geltenden Theme-Regeln nicht unbemerkt ersetzen. Stitch-Projekt-/Screen-IDs
  sowie die tatsächlich gewählte Variante im aktuellen Plan zuordnen.
- Nach jedem Paket den noch offenen Stand aktualisieren. Nach gemeinsamer Abnahme
  gültige Erkenntnisse in die Fachdokumentation übernehmen und den erledigten Plan
  samt Verweisen entfernen; kein automatisches Archiv.

Grundsätzlich wird nur reguläres Codex-Kontingent verwendet. Für Geräte & Sitzungen,
die öffentliche Besucherrunde und nun gesondert die Admin-Benutzerverwaltungsrunde
hat der Benutzer den Einsatz vorhandener Credits ausdrücklich freigegeben. Für
die Adminrunde wurde die anfängliche Pausenwahl ausdrücklich als Fehlklick
korrigiert. Die Ausnahmen gelten nur für die jeweils benannten Runden.
Kein Kauf von Credits, kein Reset-Guthaben und keine separate API-Abrechnung.
Eine automatische Fortsetzung ist nicht eingerichtet.

Zeit und Kontingent sind eine Obergrenze, kein Verbrauchsziel. Zuerst ein kleiner
vollständiger Kreislauf aus verfeinerter Referenz, einer weiteren Szene samt
Overlay und geprüftem Rückabruf. Danach gezielt zum geplanten HTML-Portal ausbauen;
Vaadin-Vergleich folgt nach dessen Bewertung. Weitere Designrunden nur, wenn sie
eine konkrete offene Frage beantworten.
