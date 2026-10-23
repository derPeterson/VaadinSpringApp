# Designkritik und Spielwiese mit Google Stitch

## Auftrag und aktueller Stand

**Aktuell freigegeben: Recherche, Planung und eigener Branch.** Der Plan wird zur
Diskussion vorgelegt; seine Umsetzung beginnt erst mit dem nächsten Auftrag.
Es wurden weder Anwendung noch Tests gestartet, keine Designs hochgeladen und
keine Stitch-Erweiterungen installiert.

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

## Empfehlung: Konto als Pilot, kleine Komponentenprobe als Ergänzung

**Erster Entwurf ist die Kontoansicht mit Profil**, einschließlich Appkopf,
Navigation, Avatar/Status, Formular und Hauptaktion. Sie enthält genug echte
Aufgaben, um Gestaltung und Bedienung zu bewerten. Die aktuelle Startseite enthält
vor allem Einleitung und Hintergrund; sie würde ein schönes Bild erlauben, aber
wenig über die spätere Alltagstauglichkeit aussagen.

Der erste Versuch umfasst nur Konto/Profil, einen Dialog und einen Fehlerzustand.
Die weiteren sinnvollen Ideen bleiben als optionale spätere Vertiefungen erhalten.
Die Spielwiese kann damit später aus drei überschaubaren Szenen bestehen:

| Szene | Zweck und Einordnung |
| --- | --- |
| Konto | Kern: realistischer Profilentwurf mit einem Dialog und einem Fehlerzustand. Optional später: Sicherheit und Sitzungen als umschaltbare Beispiele |
| Benachrichtigungen | Optional später: Transferprobe mit gelesenen/ungelesenen Einträgen, langen Texten, Filtern und Pagination |
| Komponenten/Zustände | Optional später: zusätzliche Felder, Buttons, Badge, Grid, Dialoge sowie leere und weitere Rückmeldezustände |

Keine riesige Seite mit allen Widgets auf einmal und keine vollständige Kopie der
Anwendung. Die Komponentenprobe prüft die Tragfähigkeit der gewählten Gestaltung;
sie ersetzt den realistischen Seitenkontext nicht. Die Startseite kann später
zusätzlich als Vergleich für Hintergrund und Appkopf dienen. Neue Dashboard-Inhalte
werden nicht erfunden, um ein leeres Bild attraktiver zu machen.

Vor dem Bau der Spielwiese werden bereits vorhandene Ansichten als Referenz
aufgenommen. Erst nach Auswahl einer Richtung entsteht der Vaadin-Pilot. Damit
kann der Benutzer frühe Entwürfe ablehnen, bevor viel Implementierungsarbeit anfällt.

## Vorläufige Bestandskritik

Grundlage sind `docs/theme.md`, die gepflegten Cosmos-CSS-Quellen sowie
`HomeView`, `AccountView`, `UserNotificationsView`, `AccountSettingsComponent`,
`AccountNavigationTabs` und `AccountPanelComponent`. **Dies ist eine Quellenanalyse,
noch keine visuelle Abnahme der laufenden Anwendung.**

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

Diese Hypothesen werden in Paket 1 für die ausgewählte Profilansicht bestätigt,
widerlegt oder präzisiert; Beobachtungen anderer Ansichten bleiben bis zu deren
optionaler Vertiefung offen. Geschmack, nachweisbares Bedienhindernis und technische
Ursache werden getrennt benannt.
Ein Finding erhält Ort/Zustand, Beleg, Auswirkung, Priorität und einen konkreten
Verbesserungsvorschlag. Gute Eigenschaften werden ebenso ausdrücklich festgehalten.

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
Installation, Authentifizierung und reale Übertragbarkeit sind noch nicht geprüft.

### HTML-Export: zuerst die einfache, verlässliche Referenz

Ein gespeichertes Seiten-HTML ist bei Vaadin nicht automatisch ein vollständiges
Abbild. Komponenten nutzen Shadow DOM; Werte, Icons, Overlay-Inhalte und Styles
müssen tatsächlich mitkommen. Ein normaler Quelltext- oder `outerHTML`-Export kann
deshalb unvollständig sein. [Vaadin-Komponentenstyling](https://vaadin.com/docs/latest/styling/styling-components)

Für den ersten Versuch genügen Screenshots und Beschreibung. Der HTML-Export ist
eine optionale spätere Verbesserung, falls er einen konkreten Nutzen für die
Übergabe oder weitere Iterationen bietet; er ist keine Voraussetzung für Paket 2.
Falls er später beauftragt wird, gilt folgende Reihenfolge:

1. Bereinigte Screenshots mit festen Ansichtsgrößen, realistischen Beispieltexten
   und einer kurzen Liste der Funktionen/Zustände als verlässliche Referenz.
2. Einen kleinen HTML-Export exemplarisch prüfen: Feldwert, Button/Icon, Tabs,
   geöffneter Dialog und Hintergrund. Nach Abschalten der eigenen Ausgangsinstanz
   muss die exportierte Ansicht noch vollständig dargestellt werden.
3. Nur bei ausreichender Darstellungstreue HTML zusätzlich verwenden. Andernfalls
   bei Bildern und Beschreibung bleiben. Keinen allgemeinen Vaadin-Exporter bauen,
   nur um diesen Designversuch zu ermöglichen.

Die vorhandene Google-Labs-Extraktion ist ein Kandidat, kein nachgewiesener
Vaadin-Konverter. [Quellbeschreibung der HTML-Extraktion](https://github.com/google-labs-code/stitch-skills/blob/main/plugins/stitch-design/skills/extract-static-html/SKILL.md)
Externe Skills werden vor einer möglichen Installation geprüft und nur passend
eingesetzt; ihre React-Buildabläufe passen nicht zum Projektauftrag.

### Automatisierung mit überprüfbaren Ergebnissen

Bevorzugt: Stitch-MCP für Projekt-/Screenauswahl, Erzeugung, Varianten, Abruf und
Designsystem. Nur benötigte Designfunktionen einrichten. Zugangsdaten gehören in
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
Ablauf nutzbar: Codex bereitet Bilder und kopierbare Prompts vor, der Benutzer oder
ein verfügbarer Browserzugriff führt den Schritt in Stitch aus, anschließend werden
die ausgewählten Ergebnisse zurückgegeben. Nach einem gezielten Nachversuch die
Alternative nutzen; die Designaufgabe nicht in eine Integrationsbaustelle verwandeln.
Es werden weder kostenpflichtige Zugänge aktiviert noch Generierungslimits umgangen.

## Gestalterische Leitplanken für den Versuch

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

Alle folgenden Pakete sind offen und noch nicht zur Umsetzung freigegeben.

### Kleiner Kern und optionale Vertiefungen

Die fünf Pakete und die ausführlichen Überlegungen bleiben erhalten. Für den
ersten vollständigen Versuch gilt jedoch nur der folgende Kern:

| Paket | Kern des ersten Versuchs | Optional später / Nice to have |
| --- | --- | --- |
| 1 | Eine Profilansicht am Desktop und schmal aufnehmen, gezielte Bestandskritik, kurzen Stitch-Verbindungs-/Übergabetest durchführen | Weitere Seiten und Sprachvarianten aufnehmen, HTML-Export erproben, Automatisierung ausbauen |
| 2 | Drei vergleichbare Designrichtungen vorstellen und eine auswählen | Zusätzliche Varianten oder weitere Screens erzeugen |
| 3 | Gewählte Richtung als kleine Vaadin-Spielwiese mit Profil, einem Dialog und einem Fehlerzustand erleben | Sicherheit/Sitzungen, weitere Dialoge und umfassenderen DESIGN.md-Austausch ergänzen |
| 4 | Kein Pflichtschritt für den ersten Versuch | Benachrichtigungen, Pagination, Grid und zusätzliche Zustände als Transferprobe ergänzen |
| 5 | Nach dem kleinen Versuch über Fortführung, Übernahme oder Verwerfen entscheiden | Bei Bedarf nach einer Vertiefung erneut entscheiden |

Die Reihenfolge des Kerns ist **1 → 2 → 3 → 5**. Paket 4 und die anderen optionalen
Vertiefungen werden nur bei erkennbarem Nutzen und mit einem passenden Folgeauftrag
aufgenommen. Eine Freigabe des kleinen Versuchs umfasst diese Erweiterungen nicht
automatisch. Sie müssen auch nicht erledigt sein, um den ersten Versuch abzunehmen
oder zu verwerfen. Übernahme in den gemeinsamen Theme-Standard bleibt gesondert abzustimmen.

Optional ist der zusätzliche Umfang, nicht die Qualität bereits geänderter Bereiche:
Isolation, Datenschutz, bestehende Fachverträge, lesbare Darstellung, Fokus,
schmale Bedienbarkeit und erforderliche gezielte Regressionen bleiben verbindlich.

### Paket 1 – Kleinen Ausgangspunkt aufnehmen und Stitch-Weg erproben

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
  Bild-/Prompt-Weg verwenden, damit erste Entwürfe nicht an der Integration warten.
  Dokumentierte Funktionen von tatsächlich getesteten unterscheiden; verwendete
  automatisierte Änderungen weiterhin durch erneuten Abruf kontrollieren.

**Optional später / Nice to have:**

- Sicherheit, Sitzungen, Inbox, ein Dialog sowie Home/Appkopf zusätzlich erfassen;
  längere DE/EN-Texte und weitere Ansichtsgrößen als Referenz ergänzen.
- Den kleinen HTML-Versuch durchführen, falls der Export die Übergabe tatsächlich
  verbessert. Bilder bleiben der tragfähige Ausgangspunkt.
- Den automatisierten Ablauf für Import, Generierung, gezielte Änderung, erneuten
  Abruf und Download vertiefen, soweit die wiederholte Nutzung den Aufwand rechtfertigt.

**Ergebnis:** nachvollziehbare Ausgangsansichten, konkrete Kritik, abgegrenztes
Verbesserungsbriefing und ein funktionierender Übergabeweg. Fehlende Verbindung
blockiert nicht die Vorbereitung des Briefings.

### Paket 2 – Drei vergleichbare Richtungen in Stitch

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

### Paket 3 – Gewählte Richtung verfeinern und in Vaadin erleben

- Zunächst ein bis zwei gezielte Stitch-Runden: pro Runde die benannten Probleme
  ändern und gelungene Eigenschaften festhalten. Bei mehreren widersprüchlichen
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

**Optional später / Nice to have:** Sicherheit/Sitzungen als bedienbare Beispiele,
weitere Dialoge und Zustände sowie ein umfassenderer DESIGN.md-Austausch. Auch dann
bleiben Passwortwechsel, Widerruf und Versand eindeutig simuliert und ohne fachliche
Seiteneffekte. Beispiel-Pagination gehört zur optionalen Transferprobe in Paket 4.

**Ergebnis:** Der Benutzer beurteilt die Gestaltung mit den tatsächlichen
Vaadin-Bedienelementen. Abweichungen vom Stitch-Bild werden erklärt. Ein schönes
Stitch-Preview allein gilt nicht als erfolgreiche Integration.

### Paket 4 – Optional später: Tragfähigkeit an weiteren Beispielen prüfen

Dieses Paket ist eine erhaltene Vertiefungsoption, keine Pflicht für den ersten
Versuch. Erst nach positiver Richtungsaussage zum Konto und passendem Folgeauftrag
den Entwurf auf eine kleine Inbox-Szene und die Komponentenprobe übertragen.
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

Diese Entscheidung folgt bereits auf den kleinen Piloten aus Paket 3. Paket 4
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
> Inbox und Pagination gehören erst zu einer späteren optionalen Transferprobe;
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
Im ersten Versuch: Profilangabe ändern, den Beispieldialog öffnen/schließen und
einen Formularfehler erkennen und korrigieren. In frühen Bildern ist das eine
Durchsicht, in der Spielwiese eine simulierte Bedienung; echte Fachfunktion folgt
erst bei Übernahme. Sicherheitsbereich finden, aktuelle Sitzung erkennen,
ungelesene Nachricht finden und zur nächsten Seite wechseln kommen erst hinzu,
wenn die entsprechenden optionalen Beispiele beauftragt sind.

| Kriterium | Entscheidende Frage |
| --- | --- |
| Ruhe und Hierarchie | Ist sofort erkennbar, wo ich bin und was wichtig ist? |
| Eigenständigkeit | Erkenne ich Cosmos wieder, und gefällt mir die Wirkung auch nach längerer Betrachtung? |
| Bedienbarkeit | Finde und verstehe ich die Aktionen ohne zusätzliche Erklärung? |
| Lesbarkeit | Sind lange Texte, Hilfen und Zustände angenehm erfassbar? |
| Konsistenz | Trägt dieselbe Gestaltung die im gewählten Umfang vorhandenen Controls und Zustände; beim Piloten Profil, Beispieldialog und Formularfehler? |
| Technische Tragfähigkeit | Funktioniert sie mit nativen Vaadin-Controls und lokal begrenzten Styles? |

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
Jetzt keine Builds, Browserjourneys, große QA oder Sonar-Scans.

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

Es wird nur reguläres Codex-Kontingent verwendet, kein Kauf oder Einsatz von Credits,
Reset-Guthaben oder separater API-Abrechnung. Zwischen größeren Schritten Kontingent
prüfen und bei Erschöpfung stoppen. Eine automatische Fortsetzung ist nicht eingerichtet.

Zeit und Kontingent sind eine Obergrenze, kein Verbrauchsziel. Zuerst ein kleiner
vollständiger Kreislauf aus Referenz, Variante, Auswahl und Vaadin-Vergleich; weitere
Runden nur, wenn sie eine konkrete offene Frage beantworten.
