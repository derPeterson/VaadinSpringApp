# Arbeiten mit Codex

Die Projekt-Skills liegen unter `.agents/skills/`. Diese Seite beschreibt ihre
Bedienung; die verlinkten `SKILL.md`-Dateien enthalten die vollständigen Agentenregeln.
Aufträge im tatsächlichen Projektverzeichnis stellen. Ein Beispiel zu besprechen
oder diese Anleitung zu lesen startet keinen Ablauf.

| Ich möchte … | Auftrag | Skill |
| --- | --- | --- |
| vollständig testen und sinnvolle Regressionen ergänzen | „Starte die große Testrunde“ | [grosse-testrunde](../.agents/skills/grosse-testrunde/SKILL.md) |
| Sonar bewerten oder bereinigen | „Sonar analysieren und planen“, „Sonar-Paket X umsetzen“ oder „Sonar komplett bereinigen“ | [sonar-bereinigen](../.agents/skills/sonar-bereinigen/SKILL.md) |
| ein Feature planen und schrittweise umsetzen | „Umsetzung starten: FEATURE“ | [umsetzung-starten](../.agents/skills/umsetzung-starten/SKILL.md) |
| den Zwischenstand prüfen und eine Commit-Message erhalten | „Gib mir bitte eine Commit-Message“ | [aenderungen-pruefen](../.agents/skills/aenderungen-pruefen/SKILL.md) |
| Java-Code auf Lesbarkeit prüfen oder überarbeiten | „Prüfe die Lesbarkeit von KLASSE“ oder „Überarbeite die Java-Lesbarkeit projektweit“ | [java-lesbarkeit](../.agents/skills/java-lesbarkeit/SKILL.md) |
| einen fertigen Branch lokal integrieren | „Merge BRANCH nach ZIEL“ | [branch-lokal-mergen](../.agents/skills/branch-lokal-mergen/SKILL.md) |
| einen Entwurf mit Stitch aus der laufenden Vaadin-Oberfläche entwickeln | „Lass Stitch für SEITE zwei Varianten mit ANREGUNGEN erstellen“ | [stitch-entwurf](../.agents/skills/stitch-entwurf/SKILL.md) |

Normale Folgeantworten gehören zum laufenden Auftrag; der Skillname muss nicht bei
jedem Arbeitsschritt wiederholt werden. Explizite Einschränkungen des Benutzers gelten.
Einrichtung und technische Sonar-Bedienung: [Entwicklungsanleitung](entwicklung.md#sonarqube-und-codex).

Alle Abläufe beachten den gemeinsamen Root-Reactor: Anwendung/Frontend liegen in
`app/`, gemeinsame Java-Regeln in `shared/`, Browser-Testcode in `browser-tests/`.
Java-Stilprüfungen laufen zentral mit `-N`; CSS wird mit Prettier und Stylelint
geprüft. Die verbindlichen Schritte stehen unter [Codestil](entwicklung.md#codestil).
Gesamt-Java-Prüfungen bei großer Testrunde
und Merge nutzen das Profil `coverage` samt technischem Berichtsmodul und gemeinsamem
Offline-Prüfer; die Befehle stehen in der Entwicklungsanleitung. Einzelaufträge
behalten ihren begrenzten Umfang. Eine Skill-Anpassung startet keine QA oder Scans.

## Vaadin-Dokumentationszugang und offizielle Skills

Das offizielle Plugin `vaadin-skills@vaadin-marketplace` ergänzt die Projekt-Skills
um den gehosteten [Vaadin-Dokumentations-MCP](https://vaadin.com/docs/latest/building-apps/agentic-development/mcp)
unter `https://mcp.vaadin.com/docs` und Anleitungen für Vaadin Flow. Es wird in Codex
installiert; die Anwendung erhält dafür keine Maven-Abhängigkeit. Der vorhandene
Sonar-MCP bleibt separat bestehen. Keine zusätzliche direkte Vaadin-MCP-Verbindung
anlegen, solange das Plugin dieselbe Verbindung bereits bereitstellt.

### Dokumentation passend zum Projekt abfragen

- Die tatsächlich verwendete Version aus `app/pom.xml` ermitteln und mit
  `get_vaadin_versions` die passende Dokumentationsreihe bestimmen. Der MCP erwartet
  bei aktuellen Versionen die Minor-Reihe: beispielsweise `25.3` für `25.3.5`.
  Nicht pauschal `25` oder `latest` verwenden, weil sich deren Ziel später ändern kann;
  keine Vorab-Version ohne Auftrag einsetzen. Liefert die Reihe inzwischen einen
  neueren Patchstand, neue APIs zusätzlich gegen unsere Abhängigkeit prüfen.
- Für unsere Oberflächen Java/Flow und Aura wählen. Komponenten-API, öffentliche
  Styling-Parts und Theme-Properties gezielt abfragen; vollständige Dokumente nur
  laden, wenn der relevante Ausschnitt zum Verständnis nicht reicht.
- Dokumentationsstand und kompilierten Projektstand unterscheiden. Bei Widersprüchen
  die konkrete API der verwendeten Version prüfen. Wenn der MCP nicht verfügbar ist,
  offizielle Versionsdokumentation oder lokale API-Quellen verwenden und die Grenze
  benennen; keine Signaturen oder Eigenschaften erraten.
- Die Verbindung dient Dokumentationsfragen. Technische Suchbegriffe oder API-Namen
  genügen normalerweise; keine Zugangsdaten, personenbezogenen Anwendungsdaten oder
  pauschalen Projektinhalte übertragen. Es ist kein Zugriff auf unsere Datenbank
  oder laufende Anwendung eingerichtet.

### Welche Anleitungen wir übernehmen

Die offiziellen Skills bleiben im Plugin unverändert. Ihre Beispiele sind technische
Hilfen; die bestehenden Projektregeln, freigegebenen Fachverträge und das Cosmos-Theme
bestimmen die Umsetzung. Insbesondere gelten folgende Unterschiede zu den Vorlagen:

| Skill | Einsatz im Projekt | Grenze gegenüber generischen Beispielen |
| --- | --- | --- |
| `aura-theme` | Aura-Properties und deren Bedeutung nachschlagen; ausdrücklich gewünschte Theme-Anpassungen vorbereiten | Vorhandene Palette, Tokens, Typografie und Ladereihenfolge verwenden. Kein zusätzliches `styles.css`, neuer Theme-Preset, externer Schriftimport oder zweites Farbsystem als Nebenwirkung. |
| `vaadin-frontend-design` | Vorhandene Cosmos-Komponenten mit passenden nativen Vaadin-APIs zusammensetzen | Bestehendes Aussehen übernehmen; Lumo-Beispiele und `LumoUtility` passen nicht zu unserer Aura-Basis. Keine ungefragten globalen Änderungen, Animationen oder eigenen Grundstile für Buttons und Cards. |
| `vaadin-form-layout` | Felder und Abschnitte mit nativen Formular- und Layoutkomponenten anordnen | Vorhandene Beschriftungen, Feldhilfen, Binder, Validierung und Speicherschutz erhalten. Beispiele an unsere Formularbausteine anpassen; keine neuen Felder oder zusätzlichen Pflichtentscheidungen erfinden. |

Die Versionswahl aus dem vorherigen Abschnitt gilt auch dann, wenn ein Skill pauschal
`vaadin_version="25"` empfiehlt. CSS-Änderungen durchlaufen weiterhin Prettier und
Stylelint nach den [zentralen Prüfvorgaben](../AGENTS.md#5-lesbarkeit-und-codestil).
Browserläufe und sichtbare Designänderungen behalten ihre bisherigen Freigabegrenzen.

Das aktuelle Paket enthält zusätzlich `vaadin-browserless-testing` und
`vaadin-usage-review`. Beide sind in der lokalen Projektkonfiguration zunächst
deaktiviert. Ein späterer
Browserless-Pilot oder umfassender API-Review braucht einen eigenen Auftrag; vorhandene
Tests und Reviews werden durch die Installation nicht ersetzt. Dev Loop und das
experimentelle Paket `vaadin-agent-tools` sind nicht eingerichtet.

### Einrichtung auf einem weiteren Entwicklungsrechner

Die Installation erfolgt einmal je Codex-Benutzer, gemäß der
[offiziellen Vaadin-Anleitung](https://vaadin.com/docs/latest/building-apps/agentic-development/agent-skills):

```powershell
codex plugin marketplace add vaadin/agent-marketplace --ref main
codex plugin add vaadin-skills@vaadin-marketplace
codex plugin list --marketplace vaadin-marketplace --json
```

Die letzte Ausgabe muss das Plugin als installiert und aktiviert ausweisen. Die
projektbezogene Auswahl liegt in der ignorierten `.codex/config.toml`; vorhandene
Einträge, insbesondere `mcp_servers.sonarqube`, beim Ergänzen erhalten:

```toml
[[skills.config]]
name = "vaadin-browserless-testing"
enabled = false

[[skills.config]]
name = "vaadin-usage-review"
enabled = false
```

Nach Änderungen an der Skill-Auswahl Codex neu laden bzw. neu starten. Ein Build oder
Neustart der Anwendung ist nicht nötig. Der Sonar-Einrichtungshelfer überschreibt
abweichende bestehende Konfigurationen bewusst nicht; eine spätere Neueinrichtung
des Sonar-Eintrags deshalb mit den vorhandenen Einstellungen manuell abgleichen.
Bei Plugin-Updates die aktivierten Skills und deren Vorgaben erneut gegen diese
Projektgrenzen prüfen. Technische Konfigurationsgrundlagen:
[MCP in Codex](https://developers.openai.com/codex/mcp/) und
[Skill-Auswahl](https://developers.openai.com/codex/skills/).

## Spring-Dokumentation und lokale Diagnose

Für Backend-Fragen stehen zwei ergänzende Zugänge bereit:

| Zugang | Aufgabe | Datenquelle |
| --- | --- | --- |
| **Context7** | Bibliotheksdokumentation und Beispiele gezielt nachschlagen | Indexierte Dokumentation, bevorzugt aus offiziellen Spring-Quellen; Remote-Plugin |
| **Spring Tools** | Spring-Boot-Projektmodell, Java-/Boot-Version, Beans, Endpunkte und Spring-spezifische Diagnosen untersuchen | Lokaler Language Server für das Anwendungsmodul `app` |

### Dokumentation mit Context7

Das [Context7-Plugin](https://github.com/upstash/context7) ist je Benutzer installiert
und verbunden. Vor einer Dokumentationsabfrage zuerst die Bibliotheks-ID mit
`resolve-library-id` bestimmen, anschließend `query-docs` gezielt zu einem Thema
verwenden. Versionen aus unseren POMs ermitteln und eine passende angebotene Version
wählen. Ist der genaue Patchstand nicht verfügbar, die Abweichung berücksichtigen
und neue APIs gegen unsere Abhängigkeit prüfen. Die Verfügbarkeit einer neueren
Version ist kein Auftrag zum Aktualisieren der Anwendung.

Nur die nötigen technischen Suchbegriffe übermitteln, keine Zugangsdaten,
personenbezogenen Daten oder pauschalen Quelltextauszüge. Herkunft und Version der
Ergebnisse prüfen; bei Lücken die offizielle Versionsdokumentation oder lokale APIs
verwenden. Für Vaadin bleibt der spezialisierte Vaadin-MCP der bevorzugte Zugang.

### Lokale Spring Tools

Der MCP `spring-tools` ist projektbezogen in der ignorierten `.codex/config.toml`
eingetragen. Er startet das veröffentlichte, durch die Hersteller-Prüfsumme geprüfte
Standalone-JAR der [offiziellen Spring Tools](https://github.com/spring-projects/spring-tools/tree/main/claude-plugins/spring-tools)
mit unserem JDK. Installationsquelle und JAR liegen lokal unter
`~/.codex/mcp-sources/spring-tools/`; eigene Laufzeitdateien unter
`~/.codex/mcp-data/spring-tools/VaadinSpringApp/`. Das Portal erhält keine zusätzliche
Maven-Abhängigkeit. Es sind keine Spring-Quickfix-Skills oder Plugin-Hooks installiert.

- Das Arbeitsverzeichnis und `spring.boot.ls.project.dir` zeigen ausdrücklich auf
  `app`. Der Reactor-Root allein wird als POM-Projekt erkannt und ist für die
  Spring-Boot-Diagnose nicht der richtige Einstieg. Auf einem anderen Rechner die
  absoluten Java-, JAR-, Modul- und Laufzeitpfade in der lokalen Konfiguration anpassen.
- `spring.config.location=classpath:/application.properties` lädt die Konfiguration
  des Language Servers. Dieser startet das Portal nicht und verwendet dessen
  Entwicklungsdatenbank nicht.
- Zuerst `getProjectList` verwenden. Das Einlesen des Maven-Modells kann beim ersten
  Start einige Minuten benötigen; eine vorübergehend leere Liste ist noch kein
  Beleg für ein fehlendes Projekt. Den zurückgegebenen Projektnamen für nachfolgende
  Abfragen verwenden, nicht den Anzeigenamen des Portals raten.
- Freigeschaltet sind 15 Werkzeuge für Projekt-/Versionsabfragen, Diagnosen, Beans,
  Endpunkte, logische Struktur und Classpath sowie `refreshWorkspace` zum Erneuern
  des internen Index. Nach größeren Änderungen bei Bedarf den Index aktualisieren.
  Diagnosen fachlich prüfen; sie lösen keine automatische Codekorrektur, keinen Build
  und keine Versionsänderung aus. Spring Tools ersetzt weder JUnit noch Sonar.

Die aktive Konfiguration lässt sich mit `codex mcp get spring-tools --json` prüfen.
Nach Konfigurationsänderungen Codex neu laden bzw. neu starten. Ein Portal-Build oder
Neustart ist für die MCP-Einrichtung nicht erforderlich. Bei einem Umzug auf einen
anderen Rechner das passende veröffentlichte Release aus dem
[Spring-Tools-Katalog](https://cdn.spring.io/spring-tools/release/claude-plugins/marketplace.json)
beziehen und dessen Prüfsumme erhalten; keinen unveröffentlichten Entwicklungsstand
oder ungeprüften Ersatz verwenden.

## Stitch, GitHub und Browserwerkzeuge

### Stitch für Entwürfe

Das offizielle Google-Paket
[`stitch-design@stitch-skills`](https://github.com/google-labs-code/stitch-skills)
ergänzt unseren [Stitch-Ablauf](../.agents/skills/stitch-entwurf/SKILL.md) um
Anleitungen für Entwürfe, Varianten, Designsysteme und HTML-Übernahmen. Das
Build-Paket mit React-/Vite- und anderen Framework-Vorlagen ist nicht installiert.
Installation auf einem weiteren Rechner:

```powershell
codex plugin marketplace add google-labs-code/stitch-skills --ref main --sparse .agents/plugins --sparse plugins/stitch-design
codex plugin add stitch-design@stitch-skills --json
```

Der projektbezogene MCP `stitch` verwendet die bereits installierte offizielle
CLI `@google/stitch`: Node startet deren `dist/stitch.js` mit `mcp start`. Die
absoluten Node-, CLI- und Projektpfade stehen ausschließlich in der ignorierten
`.codex/config.toml`. Die CLI verwaltet ihre Anmeldung selbst; weder Schlüssel
kopieren noch Zugangsdaten aus Konfigurationsdateien anderer Werkzeuge auslesen.
Falls eine Anmeldung fehlt, den vorhandenen `stitch login`-Ablauf nutzen. Der
Stitch-MCP ist eine gesonderte Verbindung: Das Designplugin allein richtet ihn
nicht ein. Codex nach der Einrichtung neu laden; das Portal benötigt keinen Build.

- Der lokale Werkzeugkatalog bietet `stitch_find`, `stitch_get`, `stitch_create`,
  `stitch_edit`, `stitch_generate` und `stitch_url`. Beispiele der offiziellen
  Skills mit anderen Werkzeugnamen anhand des aktuellen Katalogs übersetzen.
  Uploads und Artefakt-Downloads laufen weiterhin über die CLI.
- Der bestehende Ablauf mit gerenderter Vaadin-Ansicht, SingleFile und bereinigten
  synthetischen Daten bleibt maßgeblich. Aus Herstellerbeispielen keine weiteren
  Browserverfahren, Agenten oder Anwendungsstarts automatisch übernehmen.
- Cosmos-Tokens und die aktuelle Theme-Anleitung bestimmen das Design. Extrahierte
  Designbeschreibungen sind temporäre Übergaben an Stitch, keine zusätzliche
  maßgebliche Theme-Dokumentation. Designsysteme in Stitch nur im beauftragten
  Umfang erstellen oder verändern; keine globale Neugestaltung ableiten.
- Die Verbindung ist durch einen lesenden Projektaufruf prüfbar. Ein erfolgreicher
  Werkzeugkatalog allein belegt noch keinen Zugriff auf den Canvas. Einrichtung
  startet keine Entwürfe und überträgt keine Anwendungsdaten.

### GitHub für Repository-Arbeit

Das verbundene GitHub-Plugin ergänzt lokale Git-Befehle um Repository-, Issue-,
Pull-Request- und CI-Abfragen. Ein weiterer direkter GitHub-MCP ist dafür nicht
nötig. Der Zugriff hängt vom verbundenen Konto und den freigegebenen Repositories
ab. Die Anmeldung lässt sich mit einer lesenden Kontoabfrage prüfen; daraus folgt
keine Aussage über sämtliche Repository-Rechte.

Commit, Push, Merge und externe Änderungen benötigen weiterhin den jeweiligen
Auftrag. Die Plugin-Verbindung erlaubt für sich allein weder Veröffentlichungen
noch Kommentare oder Nachrichten an andere Personen.

### Vorhandene Browserwerkzeuge weiterverwenden

Ein zusätzlicher Microsoft-Playwright-MCP ist vorerst nicht eingerichtet. Die
vorhandene Browsersteuerung und unsere isolierten Playwright-Testläufe decken
die aktuellen Aufgaben ab. Bei einer konkreten fehlenden Diagnosefunktion erneut
bewerten, statt dieselbe Browseraufgabe parallel über mehrere Server anzubinden.
Browserfreigaben gelten weiterhin je Auftrag.

## Java-Lesbarkeit

Der [Codestil](entwicklung.md#codestil) gilt bei jeder Java-Änderung und jedem
Java-Review. Der Skill führt durch gezielte Prüfungen ganzer Klassen oder Pakete
und durch die projektweite Sichtung. Ein Prüfauftrag liefert Fundstellen und
Vorschläge. Ein Auftrag zur Überarbeitung umfasst Änderungen im benannten Umfang.

## Große Testrunde

Der Gesamtauftrag umfasst vollständige JUnit-/Shared-, Browser-Helfer-, Frontend-
und weitere gepflegte Offlineprüfungen sowie Browser `all` auf DE und EN am selben
aktuellen Produktionsimage in eigenen isolierten Umgebungen. OpenCode wird nicht
mitgetestet. Technische Ausführung: [Testanleitung](tests.md).

Änderungen seit der letzten erfolgreichen vollständigen QA und der zentrale
[Testbedarf](testbedarf.md) bestimmen sinnvolle neue oder anzupassende Regressionen.
Vorhandene passende Tests wiederverwenden. Fehler innerhalb geltender Verträge
beheben; neue Fachfunktionen und grundlegende Umbauten abstimmen. Der ausdrückliche
Gesamtauftrag umfasst die Browserfreigabe, einzelne Änderungen dagegen nicht.

Sonar wird mit passendem Analysestand mitgeprüft. Kleine klare Fehler oder Blocker
dürfen behoben werden; umfangreiche Pakete gehören zu „Sonar bereinigen“.
Keine unbegrenzte Umbau-/Scanschleife bis null während der QA. Testergebnisse,
Findings und Quality Gate getrennt berichten. Keine Sonar-Statusänderungen,
echten Mails oder automatischen Git-Abschlüsse.

Ergebnis ist der ersetzte knappe [Prüfstand](status.md), keine Berichtssammlung.
Die erfolgreiche QA-Baseline und eine laufende Runde bleiben im ignorierten lokalen
`.codex/qa/state.json` getrennt. Details: [Zustandsvertrag](../.agents/skills/grosse-testrunde/references/zustand.md).

## Sonar bereinigen

| Modus | Umfang |
| --- | --- |
| Analysieren und planen | Frischer Scan, vollständige Findings bewerten, priorisierte Arbeitspakete planen; keine Code-, Git- oder Issue-Statusänderungen |
| Paket X umsetzen | Genau ein geplantes Paket auf eigenem Branch mit Regressionen, eigenen lokalen Commits/Merge und main-Nachscan; danach anhalten |
| Komplett bereinigen | Analyse und Planung, geeignete Pakete nacheinander umsetzen, integrieren und nachscannen; Ziel möglichst null berechtigte offene Issues |

Die Ausführungsmodi erlauben begründete Accepted-/False-Positive-Entscheidungen mit
eigenständig verständlichem Kommentar direkt am Befund. Kein Push. Wesentliche neue
Funktionen oder Vertragsänderungen abstimmen; ohne Antwort bleiben sie wartend.
Die benötigte MCP-/Scanner-Anbindung muss verfügbar sein; der Skill installiert sie nicht.
Der vorbereitete Projektscan läuft über `scripts/sonar/sonar.py scan`; Voraussetzungen
und MCP-Bedienung stehen in der [Entwicklungsanleitung](entwicklung.md#sonarqube-und-codex).
Er baut/installiert alle Module mit gemeinsamen Coverage-Profilen, prüft Modell,
Klassen und Messdaten vor genau einer Analyse unter `vaadin-spring-app`.
Browser-Testcode bleibt Testcode; App- und Shared-Coverage werden zusammengeführt.

Community Build analysiert im Hauptprojekt main. Paket-Branches daher zuerst lokal
testen und integrieren, anschließend main scannen. Kein separates Branch-Prüfprojekt.
Browserläufe benötigen einen eigenen Auftrag; nötige Fälle im Testbedarf erhalten.

Es gibt einen aktuellen lokalen Bericht `.codex/sonar/bericht.md` mit Datum und Modus
und daneben `state.json` für die Wiederaufnahme. Beide sind ignoriert und entstehen
erst beim echten Auftrag. Der Bericht wird im Chat verlinkt und beim nächsten Auftrag
ersetzt, nachdem offene Pakete unter `docs/planung/sonar-pakete.md` und offene Tests
im Testbedarf erhalten wurden. Keine datierten Archive. Fiktive Beispiele bleiben
unter einem festen Namen im temporären Chatordner und werden ebenfalls ersetzt.

## Umsetzung starten

Der Startauftrag legt standardmäßig einen Feature-Branch und einen knappen Plan
unter `docs/planung/<feature>.md` an; noch keine Implementierung. Abweichende
Branch-/Ausgangsstände ausdrücklich nennen. Anschließend den Plan besprechen,
anpassen und vollständig oder in einzelnen Schritten freigeben.

Backend und sinnvolle JUnit-Regressionen umsetzen. Bei UI-Funktionen vorhandene
Vaadin-/Cosmos-Komponenten integrieren und die Oberfläche gemeinsam verfeinern.
Technisch fertig bedeutet noch keine gemeinsame UI-Abnahme. Kleine Folgeänderungen
bleiben Teil desselben Features; ein Zwischencommit beendet es nicht.

Browserbedarf zentral festhalten; weder Browser- noch Gesamtrunden automatisch starten.
Größere Zusatzentscheidungen abstimmen. Bei fachlichem Abschluss gültige Dokumentation
pflegen, Provisorien und den erledigten Plan samt Verweisen entfernen; offene Aufgaben
erhalten. „Bitte nach main mergen“ gilt zugleich als Abnahme und Abschlussauftrag.
Commits, Merge und Push benötigen den jeweiligen Auftrag.

## Änderungen prüfen und Commit-Message

Der Auftrag prüft den tatsächlichen uncommittierten Zwischenstand auf Fehler,
Sicherheit, Menschenlesbarkeit, passende Tests und Dokumentation. Kleine zugehörige
Probleme dürfen korrigiert werden; größere Umbauten werden abgestimmt. Explizite
Nur-Lese-/Dateigrenzen beachten und teilweise gestagte Stände klar unterscheiden.

Erfolgreiche unveränderte Prüfungen berücksichtigen und nach Korrekturen gezielt
nachtesten. Keine automatische große QA, Browserrunde oder Sonar-Bereinigung.
Ergebnis nur im Chat: Änderungen, tatsächliche Prüfungen, offene Punkte und eine
kopierbare deutsche Commit-Message. Kein Commit, Merge, Push oder neuer Bericht.

## Branch lokal mergen

Quelle und Ziel müssen aus dem Auftrag eindeutig sein; sie sind nicht zwingend
aktueller Branch und main. Bei uncommittierten Änderungen sofort anhalten, bevor
bereinigt oder gemergt wird. Kein Stashen, Verwerfen oder automatisches Mitcommitten.

Bei sauberem Stand featurebezogene Provisorien, überflüssigen Code und erledigte
Pläne prüfen sowie Dokumentation pflegen. Offene Folge-/Testaufgaben erhalten.
Erzeugt diese Bereinigung Änderungen, mit Commit-Message zur Benutzerprüfung
zurückgeben und vor dem Merge auf Commit und Weiterauftrag warten.

Den sauberen Stand lokal integrieren; Quellbranch und Commit-Historie behalten.
Kein Push oder automatisches Löschen. Überschaubare Konflikte lösen, unklare
Fachentscheidungen abstimmen. Danach auf dem Ziel optimiert bauen, vollständige
JUnit-App-/Shared- und Browser-Helfertests samt gemeinsamer Java-Coverage und einen
eigenen isolierten Start des frischen App-JARs prüfen. Keine automatische
Browserrunde oder große QA; die letzte große QA-Baseline bleibt unverändert.

Ergebnis kurz im Chat, einschließlich offener Fehler und uncommittierter Nachkorrekturen.
Ein Merge nach main schließt das Feature erst nach erfolgreicher Integration und
Nachprüfung ab. Ein anderer Zielbranch bedeutet das nicht automatisch. Ein vom
Benutzer selbst ausgeführter Merge startet keinen Agentenablauf.

## Kontingent und Fortsetzung

Große Testrunde und Sonar-Bereinigung prüfen das Kontingent beim Start, zwischen
größeren Abschnitten und gelegentlich im längeren Lauf. Bei Erschöpfung sichern und
anhalten; kein bewusster längerer Betrieb auf Credits. Nach dem erforderlichen Reset
über einen eigenen Folgeauftrag nur unvollständige Arbeit fortsetzen, vorhandene
Prüfungen und Prozesse berücksichtigen. Ein ausdrückliches Stopp hebt die Fortsetzung
auf. Kein Kauf von Credits, Einlösen eines Resets oder Wechsel zur API-Abrechnung.
