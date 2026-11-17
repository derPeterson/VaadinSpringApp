# Große Testrunde vom 15.11.2026

Die fünf freigegebenen Nachtpunkte sind technisch abgeschlossen: native Karten,
native Bestätigungen, vollständige Mailqueue-Bereinigung, lokale Zwischenintegration
mit Sonar-Bereinigung und abschließende Gesamtprüfung. Die Gesamtumsetzung der
Vaadin-Komponenten bleibt ausdrücklich offen. Die neuen Karten, Bestätigungen
und der Mailqueue-Assistent sind vom Benutzer abgenommen; die dabei gesammelte
Nachbearbeitung ist nach Sichtung der Vergleichsaufnahmen ebenfalls abgenommen.
Weitere Entscheidungen stehen im
[Komponentenplan](planung/vaadin-komponenten-abgleich.md).

Vergleichsbasis ist die letzte vollständig belegte QA bei `7d127bcb`.
Der geprüfte Quell- und Teststand ist `f3c21623246996988a6fb26372ca07e3fc104d82`,
lokal nach `main` integriert. Die abschließende Dokumentationsbereinigung erfolgt
auf `codex/vaadin-komponenten-folgearbeiten-4`. Die nachfolgend gesondert
ausgewiesene Nachbearbeitung verändert diesen Stand; ihre gezielten Nachweise
ersetzen keine erneute große Testrunde. Kein Push oder Release wurde durchgeführt.

## Ergebnisse

| Prüfung | Tatsächliches Ergebnis |
| --- | --- |
| Root-Reactor, Produktionsbuild und Coverage | Bestanden; Shared vor seinen Verbrauchern gebaut |
| Anwendungs-JUnit | 3.086 bestanden, keine Fehler, zwei ausdrücklich optionale Caddy-Prüfungen übersprungen |
| Browser-Helfertests | 131 bestanden, keine Fehler oder Skips |
| Frontend-Offlineprüfungen | 20 bestanden |
| Coverage-/Sonar-Pythonprüfungen | 18 beziehungsweise 49 bestanden |
| Java-Formatierung und Checkstyle | Bestanden |
| Frisches aktuelles Main-JAR | Isoliert mit neuer H2 gestartet, Anmeldung HTTP 200; eigene Umgebung wieder entfernt |
| Gesamte Browsersuite Deutsch | 358 von 358 bestanden, keine Fehler oder Skips |
| Gesamte Browsersuite Englisch | 358 von 358 bestanden, keine Fehler oder Skips |
| Aufbau, Abbau und HTML-Berichte beider Browserläufe | Bestanden |
| Lokale Zeilenabdeckung | Anwendung 85,23 %, Shared 100 %; gemeinsamer Coverage-Prüfer bestanden |
| SonarQube auf lokalem `main` | Null offene Issues, null Hotspots, Quality Gate OK |

Beide vollständigen Browserläufe verwendeten dasselbe unveränderte Advanced-Image:
`sha256:2e23e8aa6d8992cd367b6a38342bc05797882793e05034b7b0d46f8df7284aa4`.
Je Sprache arbeiteten vier unabhängige Worker mit eigenen frischen Testumgebungen;
innerhalb eines Workers liefen die Testklassen sequenziell. Die Suite umfasst
Desktop- und emulierte Smartphone-Szenarien in Chromium.

Die abschließende Sonar-Analyse `e63beeba-34c4-450a-a73c-9d7df0714cbe`
gehört zum geprüften Commit `f3c21623`. Alle zwölf zunächst offenen Findings wurden
im Code beziehungsweise in Kommentaren behoben und durch Nachscan bestätigt.
Die 46 bestehenden Ausnahmen blieben unverändert; keine neuen Ausnahmen oder
Statuswechsel wurden zur Bereinigung verwendet.

## Geprüfte Änderungen und besondere Entscheidungen

- Gemeinsame Konto-, Admin- und Ticketkarten sowie geeignete Innen-, Vorschau-
  und Kontextkarten verwenden native Vaadin-`Card`. Öffentliche Slots,
  Cosmos-Gestaltung und fachliche APIs bleiben erhalten.
- Zehn weitere Bestätigungen verwenden den nativen `ConfirmDialog`. Abbruchwege,
  Anfangs- und Rückfokus, einmalige Ausführung, erneute Bestätigung nach Fehlern
  und lange Inhalte mit erreichbaren Footeraktionen sind abgesichert.
- Mailqueue-Läufe verarbeiten alle zum Lauf gehörenden passenden Aufträge in
  kurzen internen 100er-Transaktionen. Ein manueller Auftrag wartet bei laufendem
  Versand dauerhaft vorgemerkt und läuft ohne zweiten Klick weiter. JUnit prüft
  unter anderem Rollback, Teilfortschritt, Wiederaufnahme und Rechteentzug.
  Die Browserfälle prüfen 101 abgeschlossene Aufträge, den echten SMTP-Wartezustand,
  Statusmeldungen, Abbruch, URL-Direktaufruf, F5 und Admin-/Superadminrechte.
  Es wird kein neuer Versand ausgelöst. Regeln: [Mailqueue](mailqueue.md).
- Vorher-/Nachher-Vergleiche für Karten und Bestätigungen liegen für DE/EN auf
  Desktop und Smartphone vor. Bei den gemeinsamen Karten stimmen je Sprache
  44 Karten- und 68 Formular-Messwertsätze überein, bei Innen-/Kontextkarten
  44 Karten- und vier Kontext-Messwertsätze. Für Bestätigungen stimmen 61
  Messwertsätze überein; je Sprache liegen 212 Bildpaare zur Sichtung vor.
  Diese gezielten Vergleiche werden nicht zur Anzahl der Gesamtfälle addiert.
- Base und Advanced sind zusätzlich jeweils mit drei gezielten Fällen pro Sprache
  bestanden: dekorative Ebenen, öffentliche und angemeldete Rahmen, echte Navigation,
  Zeigerverhalten und reduzierte Bewegung. Advanced bleibt ausgewählt.
- Testtreiber wurden für gemeinsame Hilfstab-Abbauten, Ticketnavigation mit
  ungelesener Aktivität und die tatsächlich aktive Hintergrundvariante korrigiert.
  Das Zeitbudget eines vollständigen Workers beträgt nun 90 statt 60 Minuten;
  einzelne UI-Wartezeiten und Sicherheitsprüfungen wurden nicht abgeschwächt.

## Gezielte Nachbearbeitung nach der Gesamtrunde

Die Reiter-Nachbearbeitung ist technisch umgesetzt und gezielt geprüft:
43 Komponenten-JUnit-Prüfungen, 38 Testauswahlprüfungen und je vier Browserfälle
für die Seitenreiter sowie drei für die segmentierten Umschalter in DE/EN sind
bestanden. Die Seitenreiter umfassen 1280/1024/390/320 px, native Pfeile,
Tastatur und echte Chromium-Touchereignisse. Die segmentierten Umschalter wurden
bei 1280/390/320 px verglichen. Beide Sprachen nutzen dasselbe korrigierte Image.
Die Vorher-/Nachher-Aufnahmen wurden vom Benutzer gesichtet und abgenommen.
Die Sicherheitswarnungen im Mailqueue-Assistenten und in der manuellen
Bereinigungsbestätigung verwenden zusätzlich das Warndreieck; die separate
Parameter-Infobox behält ihr Info-Icon. 25 Komponenten-/Symbolprüfungen,
39 Testauswahlprüfungen und je zwei
gezielte Browserfälle in DE/EN bei 1280/390 px sind bestanden. Alle vier
Dialogabbruchwege erhalten den bisherigen Bereinigungsstand. Die zugehörigen
Vorher-/Nachher-Aufnahmen sind ebenfalls vom Benutzer abgenommen.
Der Brush im manuellen Dialogkopf verwendet die vorhandene rote Gefahrenvariante;
auch dieser gesonderte Nachtest besteht je zwei Browserfälle in DE/EN bei 1280/390 px.
Der kürzere Wartetext ist zusätzlich mit 15 Textkatalogprüfungen und je einem
Queue-Gesamtlauf in DE/EN bestanden: echter SMTP-Wartezustand und anschließend
101 bereinigte Aufträge ohne erneuten Versand. Jeder Fall prüft 1280/390 px;
die Erklärung bleibt am Desktop einzeilig, die Vormerkungs-Notification blau
mit Info-Icon. Die Vergleichsaufnahmen beider Zustände sind vom Benutzer abgenommen.
Die vier vereinbarten Benutzeranzeigen begrenzen lange Namen und E-Mail-Adressen
und bieten vollständige Werte im nativen Tooltip. 135 vorhandene Komponenten-
JUnit-Prüfungen, 40 Testauswahlprüfungen und je vier Browserfälle in DE/EN
bei 1280/390 px sind bestanden. Geprüft wurden normale und lange Angaben,
maximale Zeilenzahl, Hover und Tastaturfokus, unveränderte Daten nach Abbruch
sowie aktualisierte Tooltips nach einer Namensänderung. Vorher-/Nachher-
Aufnahmen liegen vor. Eine allgemeine Touch-Bedienung wird erst separat geplant;
ein Smartphone-Info-Button ist nicht Teil dieser Änderung.
Formularschutz und Sammelabmeldung verwenden mobil volle Aktionsbreite mit
unveränderter Reihenfolge und gleichem Anfangsfokus. Zwölf Komponenten-JUnit-
Prüfungen, 41 Auswahlprüfungen und je vier Browserfälle in DE/EN bei 1280/390 px
sind bestanden. Abbruch erhält Entwurf beziehungsweise Sitzungen; nach Wechsel
vom Sammel- zum Einzeldialog übernimmt die Einzelaktion keine Vollbreite.
Desktopdarstellung und eigener gespeicherter Zugang bleiben erhalten.
Vorher-/Nachher-Aufnahmen der beiden Dialoge sind vom Benutzer abgenommen.
Die Kontoaktionskarten ordnen ihre Buttons mobil rechts an. 42 Auswahlprüfungen
und je zwei Browserfälle in DE/EN bei 1280/390 px sind bestanden. Beide
Identitätsdialoge lassen sich öffnen und ohne Kontoänderung abbrechen;
Vergleichsaufnahmen zeigen Smartphone- und unveränderte Desktopdarstellung.
Die Konfigurationsfooter nutzen mobil durchgehend volle Buttonbreite. 43
Auswahlprüfungen und je sechs gezielte Browserfälle in DE/EN bei 1280/390 px
sind bestanden: allgemeine Gruppen für Admin/Superadmin, Queue-Assistent,
beide Anbieter und alle vier 2FA-Rollen. Geprüft sind tatsächliche Breiten,
Ausrichtung, gleiche Höhen und vertikale Abstände. Vergleichsaufnahmen der
Footer liegen vor; Desktopdarstellung und Speicherverträge bleiben erhalten.
Diese Änderungen sind im unten ausgewiesenen aktuellen Sonar-Stand enthalten.
Alle neun Nachbearbeitungspunkte sind technisch fertig, gezielt
geprüft und nach Sichtung der Vergleichsaufnahmen vom Benutzer abgenommen.
Die Gesamtumsetzung bleibt im [Komponentenplan](planung/vaadin-komponenten-abgleich.md) offen.

Alle 97 globalen Meldungsaufrufe in 36 Klassen verwenden die zentralen nativen
Textmethoden. Der frühere HTML-Aufbau samt ausschließlich dafür verwendetem
Helfer und Test ist entfernt. 710 gezielte JUnit-Prüfungen sind bestanden;
Stilprüfung und Diffprüfung sind ebenfalls erfolgreich. Abgedeckt sind der
Helfer, Sprach- und Fensterisolation sowie betroffene Konto-, Anmelde-,
Sicherheits-, Benachrichtigungs- und Verwaltungsabläufe.

Der Benutzer hat die repräsentativen Varianten nach Sichtung der
Vorher-/Nachher-Aufnahmen abgenommen: Profil-Erfolg, Pflichtfeldfehler,
vorgemerkte Bereinigung, reiner Aktualisierungstitel, hervorgehobene
Registrierungsadresse und Neustartwarnung mit Aufzählung. Dafür waren 45
Auswahlprüfungen und je zehn Browserfälle in DE/EN bei 1280/390 px erfolgreich;
die beiden Sprachen verwendeten dasselbe korrigierte Image. Aufbau, Abbau und
Berichte waren erfolgreich. Diese Browserprüfung gilt für die Varianten vor
der anschließenden Übernahme der übrigen Aufrufer. Für die Übernahme gab es
auf ausdrücklichen Benutzerwunsch nur gezielte JUnit-/Stilprüfungen.

Alle 24 damaligen Vorher-/Nachher-Messwertsätze stimmen bei Maßen und
Textgestaltung überein; numerische Browserrundungen unter 0,001 px wurden
toleriert. 23 der 24 Meldungsbilder sind bytegleich. Beim englischen mobilen
Registrierungshinweis unterscheiden sich ausschließlich 21 Pixel an den
oberen Rundungen um höchstens einen Farbwert; Maße, Inhalt und Schrift bleiben
gleich. Die Vergleichsseite bettet die unveränderten Originalaufnahmen und
zusätzliche Seitenansichten ein. Diese gezielten Prüfungen ersetzen weder
die obige Gesamt-QA noch einen neuen Sonar-Scan. Die Gesamtumsetzung bleibt
im Komponentenplan offen.

Alle 17 allgemeinen Konfigurationszeitfelder und die 2FA-Inaktivitätsdauer
verwenden jetzt dieselbe native CustomField-Hülle. 129 gezielte Anwendungs-
JUnit-Prüfungen sind bestanden, darunter alle drei Speicherformate in DE/EN,
kanonische Werte, Leerwerte, native Zustände und Beschriftungen, präzise
Tokenfristen, Feldhilfe, Assistentenfreigabe, Änderungsvergleich und 2FA-Abläufe.
Die Prüfungen decken auch das Laden leerer gespeicherter Werte sowie das
Zurücksetzen eines alten Feldfehlers beim Bearbeiten eines weiterhin ungültigen
Präzisionswertes ab. Ein beim Review erkannter Wechsel zwischen deaktiviertem
und schreibgeschütztem Feld ist ebenfalls abgesichert: Die Schrittbuttons werden
nach Freigabe wieder bedienbar und bleiben bei Read-only gesperrt. Spotless und
Checkstyle sind erfolgreich.

Für den vorherigen 2FA-Piloten sind 46 Auswahlprüfungen sowie isolierte Vorher-
und abschließende Nachher-Läufe mit jeweils zwei Fällen pro Sprache bei
1280/390 px bestanden. DE und EN verwendeten pro Vergleichsstand dasselbe
unveränderte Image. Aufbau, Abbau und Berichte waren erfolgreich. Geprüft wurden
Aktivierung, dieselbe Zahl beim Einheitenwechsel, Speichern mit 2FA, erneutes
Öffnen, leere Eingabe, Abbruch und bestätigtes Neuladen. Alle zwölf damaligen
Messwertsätze stimmen überein; sämtliche zwölf zugeschnittenen Vergleichsbilder
sind bytegleich. Die Vergleichsseite enthält unveränderte Originalaufnahmen
und Seitenansichten.

Diese Browserbelege gelten für den Piloten vor der anschließenden Vereinheitlichung.
Für die Übernahme der übrigen Felder sind ausdrücklich nur JUnit-/Stilprüfungen
beauftragt; weitere Screenshots oder Browserläufe wurden nicht ausgeführt.
Der Benutzer hat die übernommenen Felder selbst gesichtet und abgenommen. Ein neuer konkreter
automatisierter Prüfbedarf wurde dabei nicht erkannt. Diese gezielten Nachweise
ersetzen den obigen Gesamtprüfstand und einen neuen Sonar-Scan nicht.

Die administrative Empfängervorschau verwendet die native AvatarGroup.
Avatarfarben, sichtbarer Rahmen und Initialenschrift entsprechen dem Cosmos-Stil.
Bis sechs Empfänger bleiben nach dem
freigegebenen nativen Grenzfall alle sichtbar; ab sieben erscheinen fünf Avatare
und „+N“. Die native Überlaufliste verwendet die Cosmos-Gestaltung. Die bisherige
ausführliche Details-Liste bleibt auf ausdrücklichen Benutzerwunsch für alle Namen
und E-Mail-Adressen erhalten; „+N“ enthält nur ausgeblendete Namen.
187 gezielte Anwendungs-JUnit-Prüfungen und 47 Auswahlprüfungen sind bestanden.
Die Vorher-Läufe und die finalen Nachher-Läufe haben jeweils zwei Fälle pro
Sprache bei 1280/390 px bestanden. DE und EN verwendeten je Vergleichsstand
dasselbe unveränderte Image; Aufbau, Abbau und Berichte sind erfolgreich.
Geprüft sind drei, fünf, sechs und neun eigene, über die Oberfläche angelegte
und bestätigte Empfänger, die sichtbaren Anzahlen, zugängliche DE-/EN-Bezeichnungen,
Tab-Reihenfolge, vollständige bisherige Details, native Überlaufliste mit vier
korrekten Namen, Maus-/Tastaturöffnung sowie Schließen mit Rückfokus. Die Rahmen-
und Farbwerte der Gruppen- und Menüavatare stimmen im Browser mit dem Headeravatar
überein. Dieser verwendet denselben Cosmos-Baustein wie die Benutzerverwaltung.
Zusätzlich sind 200 synthetische Einträge über die öffentliche Widget-API geprüft:
begrenzte Overlayhöhe, Scrollen bis zum letzten Eintrag per End-Taste, vollständige
Erreichbarkeit sowie Schließen mit Rückfokus. Dafür wurden keine 200 Datenbankkonten
angelegt; die eigentliche Vorschau bleibt bei ihren neun realen Testempfängern.
Die reine Vorschau versendet keine Benachrichtigung. Die technische Sichtprüfung
der finalen Aufnahmen bestätigt sichtbare Rahmen und Initialen sowie das begrenzte
Cosmos-Menü auf Desktop und Smartphone. Die neue Vergleichsseite enthält
88 eingebettete Originalaufnahmen: 20 Vorher-/Nachher-Paare samt Ganzansichten
und vier Mengenfallansichten mit jeweils zugehörigem Kartenausschnitt.
Die menschliche Abnahme der Rahmen- und Menügestaltung steht aus.

Der Anwendungsbuild hat `app/src/main/bundles/prod.bundle` automatisch um den
benötigten AvatarGroup-Import und seine Builddateien ergänzt. Der aufgabenbezogene
generierte Stand bleibt erhalten; das Bundle wurde nicht manuell bearbeitet.
Diese gezielten Prüfungen ersetzen weder die obige Gesamt-QA noch einen neuen
Sonar-Scan. Die Gesamtumsetzung bleibt offen.

## Aktueller Sonar- und JUnit-Stand vom 16.11.2026

Der gesondert beauftragte Sonar-Lauf auf `main` ist abgeschlossen. Die bestätigte
Endanalyse `87587cf9-0fe0-4aeb-b291-2f1c368c157f` gehört exakt zum Codecommit
`35fa9e0284a8ec68b16bcdb4da149bd2f6073667`. Die anschließende Dokumentation verändert
den geprüften Anwendungscode nicht.

Der einzige offene Hinweis `java:S2259` im `NotificationHelper` wurde durch eine
ausdrückliche frühe Rückgabe bei fehlender oder leerer Hervorhebung beseitigt.
Das bestehende Verhalten bleibt erhalten; drei zusätzliche Regressionen prüfen,
dass Hervorhebungen beim Aktualisieren verschwinden und wiederkehren können,
ohne den Literaltext oder die Notification zu ersetzen.

- Null offene Issues im Gesamtcode, null Hotspots, Quality Gate OK.
- 46 bestehende Ausnahmen unverändert; keine neue Ausnahme oder Regelabschaltung.
- Vollständiger optimierter Reactor-Install mit gemeinsamer Coverage, wirksames
  Maven-Modell und Coverage-Prüfer bestanden.
- 3.123 Anwendungstests bestanden; zwei ausdrücklich optionale Caddy-Fälle
  übersprungen. 141 Browser-Helfertests bestanden.
- Nach Commit und bestätigendem Scan den kurzen gezielten JUnit-Lauf ausgeführt:
  alle 26 Fälle des `NotificationHelperIsolationTest` bestanden.

Keine zusätzliche Browserrunde und keine neue große Testrunde durchgeführt.
Die obige Gesamt-QA bleibt der frühere Vergleichsstand; ihr zurückgestellter
Scrollpunkt und die übrigen offenen Abnahmen bleiben erhalten. Die gesamte
Vaadin-Komponentenumstellung ist weiterhin ausdrücklich offen.

## Abnahme, offene Nachbearbeitung und Grenzen

Der Benutzer hat Karten/Panels und die weiteren Bestätigungsdialoge anhand der
bereitgestellten Screenshots abgenommen. Eine zusätzliche vollständige manuelle
Durchklickprüfung aller Dialoge wurde nicht durchgeführt. Der Mailqueue-Assistent
ist nach eigener Prüfung einschließlich F5 ebenfalls abgenommen. Einzelheiten
stehen unter [HUM-01](manual-tests/README.md#hum-01).
Die dabei gesammelten neun Nachbearbeitungspunkte sind mit gezielten Screenshots,
Review und Commit je Punkt umgesetzt. Der Benutzer hat die Vorher-/Nachher-
Aufnahmen gesichtet und alle neun Punkte ausdrücklich abgenommen.
Die obigen Gesamtergebnisse belegen den früheren Ausgangsstand,
keine vollständige Prüfung aller Folgeänderungen. Echte Mobilgeräte, reale Passwortmanager,
Screenreader und externe Anbieter-/SMTP-Sonderprüfungen bleiben gesonderte
menschliche beziehungsweise ausdrücklich freizugebende Integrationsprüfungen.
Es wurden keine echten E-Mails versendet und keine vorhandenen Entwicklerdaten
oder gespeicherten Passwörter als Testdaten verwendet.

Der damals gemeldete Scrollfehler (verändertes Verhalten und Farben der
Scrollleisten nach Umbauten) ist auf dem Branch `design/aura-theme` mit der
einheitlichen Scrollleiste behoben; COMPONENT-06 besteht dort in DE und EN.

Die Mailqueue-Erweiterung ändert das aktuelle H2-Schema. Für ältere lokale
Entwicklungsdatenbanken ist eine frische Datenbank erforderlich; eine Migration
ist im vereinbarten Entwicklungsstand nicht vorgesehen. Die bestehende Datenbank
wurde nicht verändert. Für die Übernahme der Java-Änderungen sind Build und
Anwendungsneustart erforderlich.
