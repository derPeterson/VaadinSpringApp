# Aktueller Prüfstand

Stand: 30.10.2026. Ausschließlich lokale Entwicklung mit eigenen austauschbaren
Testdaten und Testdiensten.

## Ergebnis und geprüfter Stand

Der gemeinsame Seitenrahmen und die Designübernahme sind technisch geprüft.
Ein vollständig zuordenbarer lokaler Quellfingerabdruck der vorigen großen Runde
lag nicht vor. Deshalb wurden alle Browser-Suiten erneut ausgeführt.

Geprüft wurden `feature/designkritik-stitch`, Commit
`ee4861abdb537246896d18cec95c20a6b7333c2c`, und die anschließenden lokalen
QA-Korrekturen. Der Commit wurde nach Review und Gegenprüfung vor der großen
Testrunde erstellt. Die QA-Korrekturen und dieser Bericht bleiben uncommittiert.
Kein Merge oder Push. Die gemeinsame Designabnahme bleibt offen.

| Prüfung | Tatsächliches Ergebnis |
| --- | --- |
| Java 25, Spotless und Checkstyle | Bestanden, keine Checkstyle-Verstöße |
| Production-/Coverage-Reactor | Vollständiges `clean verify` bestanden; abschließende CSS-Ressourcen danach erneut paketiert |
| Anwendungs-JUnit | 2.321 erfasst: 2.319 bestanden, zwei erwartete optionale IDE-/Caddy-Skips, keine Fehler |
| Browser-Helfertests | 85 bestanden, einschließlich lokalisierter Dauerzahlen |
| Frontend-JavaScript | Zwölf bestanden |
| Sonar-/Coverage-Offlinetests | 47 beziehungsweise 18 bestanden |
| Ruff der fünf gepflegten Helferdateien | Formatprüfung und Lint bestanden |
| Gemeinsame Java-Coverage | Aggregat nach finaler Testkompilierung erneuert und gegen das wirksame Maven-Modell geprüft: App 90,19 %, Shared 726/726 Zeilen |
| Browser `all` DE | 165/165 bestanden, vier Worker, Gesamtexit 0, keine Fehler oder Skips |
| Browser `all` EN | 165/165 bestanden, vier Worker, Gesamtexit 0, keine Fehler oder Skips |
| Gezielter Filter-Nachtest EN | Fünf bestanden, einschließlich echter Klicks am freien Badgerand |
| Eigene Browserumgebungen | Alle acht finalen Workerprojekte ohne verbliebene Container, Volumes, Netze oder private Workerdateien |
| Lokale Dokumentationsverweise | 184 Dateiziele geprüft, keines fehlt; keine Prüfung externer Links oder Überschriftenanker |
| Abschließender Diff | `git diff --check` bestanden; nur beauftragte Quellen, Regressionen und Dokumentation verändert |

Beide vollständigen Sprachläufe verwenden dieselbe feste Image-ID:
`sha256:ba9c46665440dcae85420cae066efd3378645f1192389b55f304d7374a73ecec`.
Die finale reine CSS-Korrektur ist darin enthalten. Pro Sprache arbeiten vier
voneinander getrennte Worker; innerhalb eines Workers laufen die Fälle sequenziell.
Die tatsächlich ausgeführten Nachtests werden nicht als zusätzliche neue Fälle
gezählt. Frühere Diagnose- und unterbrochene Läufe sind keine Belege dieses Endstands.
Die lokalen Metadaten und Browserberichte bleiben unter `browser-tests/runs/`.

## Review, Bereinigung und behobene Ursachen

Besucher, Portal und Administration verwenden denselben Seitenrahmen. Nur der
Serviceheader bleibt beim Scrollen stehen. Überschrift, native Seitennavigation
und Inhalt scrollen gemeinsam; Copyright folgt im Dokument. Portal und Admin
verwenden denselben nativen SideNav-Baustein mit möglichen Unterpunkten.
Besucher behalten ausschließlich die obere Navigation. Die abgestimmte Gestaltung
der Felder, Buttons, Tabellen und Dialoge wurde beim Rahmenumbau erhalten.

Der erste Review prüfte Aufrufer, gemeinsame Komponenten, Ressourcen und Texte.
Die Gegenprüfung bezog die Korrekturen, ihre Reichweite und die ausführbaren
Regressionen ein. Die große Runde bestätigte zusätzlich Konten, Speicherung,
Rollen und Zugriffsentzug, Sitzungslimits/Widerruf, Tokenablauf, Wartung, reale
Browser-/Serverneustarts und SMTP-Annahme im eigenen Mailpit.

Behoben und am finalen Stand nachgeprüft:

- Mobile Navigation fokussiert den ersten nativen Link. Escape schließt die
  Navigation und gibt den Fokus zum Menüknopf zurück. Der Shortcut ist auf
  Navigation und Schalter begrenzt, damit native Popovers und Dialoge Escape
  weiterhin selbst behandeln. [Code](../app/src/main/java/de/derpeterson/app/ui/components/PortalPageComponent.java).
- Ein langer öffentlicher Servicename verdrängt die Navigation nicht mehr.
  [CSS](../app/src/main/resources/META-INF/resources/custom-theme/cosmos-public.css).
- Mobile Benachrichtigungen laufen nicht mehr horizontal über: Der Segmentrahmen
  rechnet Padding/Border in seine Breite ein, der Aufbewahrungstext darf umbrechen.
  [CSS](../app/src/main/resources/META-INF/resources/custom-theme/cosmos-notifications.css).
- Status-/Rollenbadges aktivieren ihre ganze sichtbare Fläche. Das native Label
  umfasst auch freie Randbereiche; ein zusätzlicher Klickhandler ist nicht nötig.
  [CSS](../app/src/main/resources/META-INF/resources/custom-theme/cosmos-admin-reference.css).
- Browserhelfer bedienen Filter als Entwurf mit ausdrücklichem Anwenden, native
  Mehrfachauswahlen, die neuen Bestätigungen und den gemeinsamen Seitenrahmen.
  Mobile Sichtbarkeitsentscheidungen warten den Vaadin-Aufbau ab.
- Dauerzahlen im Testkatalog folgen der Testsprache statt der Windows-Sprache.
  Eine fehlgeschlagene Hilfetextprüfung wird nicht mehr durch eine noch offene
  Feldhilfe bei der Bereinigung verdeckt. Die fachlichen Assertions bleiben erhalten.
- Neue/überarbeitete Regressionen prüfen alle neun Dauerfelder, echte
  Textarea-Vergrößerung und -Verkleinerung, Filterentwürfe und Badge-Klickflächen,
  Infoicon-Zentrierung, mobile Navigation, Scrollverhalten und Hilfegrenzen.

Entfernt wurden die unbenutzten Kontotabs und konkurrierende alte Navigationsstyles
sowie 18 unbenutzte typisierte Textgetter samt DE-/EN-Einträgen. 24 alte
Referenzicons wurden aus den Laufzeitressourcen in die Vergleichsquellen verschoben.
Die HTML-Referenzen und Stitch-Originale bleiben bis zur gemeinsamen
Ablageentscheidung erhalten. Der validierte Projekt-Skill `stitch-entwurf` ist
vorhanden; Bedienung unter [Arbeiten mit Codex](codex-skills.md).

## Sonar und Grenzen

Die konfigurierte Sonar-Anbindung wurde lesend geprüft. Verfügbar ist die
main-Analyse `226deebd-5a32-4336-8986-5deb9891eb71` vom 22.10.2026 zu Revision
`951452da7e89fc3f14db910f910734cfd5bf8008`: Gate OK, keine offenen Issues oder
Hotspots, 45 bestehende begründete Ausnahmen. Dieser ältere Stand ist kein
Sonar-Nachweis für den Featurebranch. Kein Upload vom Featurebranch und keine
Statusänderung zur Zahlenbereinigung. Der aktuelle Feature-Sonar-Nachweis bleibt offen.

Die zwei optionalen IDE-/Caddy-Fälle wurden mangels Opt-in nicht ausgeführt.
Externe SMTP-Provider, echter Mail-Sichtversand, Zertifikatseinrichtung/-rückbau
und der eingefrorene OpenCode-Workflow gehören nicht zu dieser Runde.
Entwicklerdaten und fremde Prozesse wurden nicht als Fixtures verwendet.

Browser prüfen Chromium und definierte Desktop-/Smartphone-Fenstergrößen.
Echte Mobilgeräte, Bildschirmtastatur, Zoom, Hilfsmittel sowie die gemeinsame
sprachliche und optische Endabnahme bleiben im [manuellen Katalog](manual-tests/README.md).
Weiterer automatisierter Ergänzungsbedarf bleibt im [Testbedarf](testbedarf.md).
Die [drei HTML-Referenzen](design/frontend-referenzen/README.md) bleiben als
bedienbare Momentaufnahmen erhalten. Die früheren Vergleichsprogramme wurden nach
der manuellen Abnahme von Portal und Admin entfernt. Nachträge und ihre gezielten
Prüfungen ersetzen die oben dokumentierte letzte große Testrunde nicht.

## Offene Fehler

In der ausgeführten Runde sind keine bestätigten Anwendungsfehler offen geblieben.

Der [Abnahme- und Abschlussplan](planung/designkritik-stitch.md) bleibt offen.
Portal und Admin sind manuell abgenommen. Der Benutzer entscheidet nach den
Tooltip- und Bereinigungsnachträgen über den weiteren Branchabschluss.
Ein Merge nach main benötigt einen eigenen Auftrag.
