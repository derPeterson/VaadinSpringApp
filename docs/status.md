# Prüfstand

**Die letzte vollständig belegte große Testrunde ist vom 15.11.2026.**
Der spätere Sonar-/JUnit-Nachweis vom 16.11.2026 und gezielte Komponentenprüfungen
sind getrennt aufgeführt. Sie sind keine vollständige Abnahme des aktuellen
Aura-Branches `design/aura-theme`.

Die gemeinsame DE/EN-Browserrunde für Aura ist nach der Versionierungsumsetzung
beauftragt; ihr Ergebnis steht weiterhin aus. Offene automatisierte Szenarien stehen im
[Testbedarf](testbedarf.md), menschliche Abnahmen im
[manuellen Katalog](manual-tests/README.md). Die redaktionelle Überarbeitung
dieser Dokumentation hat keine neue Testausführung ausgelöst.

## Ergebnisse

### Große Testrunde vom 15.11.2026

Vergleichsbasis: `7d127bcb`. Geprüfter Quell- und Teststand:
`f3c21623246996988a6fb26372ca07e3fc104d82`, lokal nach main integriert.
Kein Push oder Release. Die gesamte Vaadin-Komponentenumstellung bleibt offen.

| Prüfung | Belegtes Ergebnis dieses Stands |
| --- | --- |
| Root-Reactor, Produktionsbuild und Coverage | Bestanden; Shared vor seinen Verbrauchern gebaut |
| Anwendungs-JUnit | 3.086 bestanden; zwei ausdrücklich optionale Caddy-Prüfungen übersprungen |
| Browser-Helfertests | 131 bestanden, keine Fehler oder Skips |
| Frontend-Offlineprüfungen | 20 bestanden |
| Coverage-/Sonar-Pythonprüfungen | 18 beziehungsweise 49 bestanden |
| Java-Formatierung und Checkstyle | Bestanden |
| Frisches Main-JAR | Isoliert mit neuer H2 gestartet; Anmeldung HTTP 200; eigene Umgebung abgebaut |
| Gesamte Browsersuite Deutsch | 358 von 358 bestanden, keine Fehler oder Skips |
| Gesamte Browsersuite Englisch | 358 von 358 bestanden, keine Fehler oder Skips |
| Aufbau, Abbau und HTML-Berichte | In beiden Sprachen bestanden |
| Lokale Zeilenabdeckung | Anwendung 85,23 %, Shared 100 %; gemeinsamer Prüfer bestanden |
| SonarQube auf damaligem main | Null offene Issues, null Hotspots, Quality Gate OK |

Beide Browserläufe verwendeten dasselbe unveränderte damalige Advanced-Image:
`sha256:2e23e8aa6d8992cd367b6a38342bc05797882793e05034b7b0d46f8df7284aa4`.
Je Sprache liefen vier unabhängige Worker mit eigenen Umgebungen; innerhalb eines
Workers sequenziell. Desktop- und emulierte Smartphone-Szenarien liefen in Chromium.

Sonar-Analyse `e63beeba-34c4-450a-a73c-9d7df0714cbe` gehört zu `f3c21623`.
Die zwölf damaligen Findings wurden behoben und nachgescannt. Die 46 vorhandenen
Ausnahmen blieben unverändert; keine neuen Statusausnahmen zur Zahlenbereinigung.

## Geprüfte Änderungen und besondere Entscheidungen

Die Gesamtrunde deckte diese Pakete ab:

- **Native Cards:** Konto-, Admin- und Ticketpanels einschließlich geeigneter
  Innen-, Vorschau- und Kontextkarten; öffentliche Slots und Cosmos-APIs erhalten.
- **Native ConfirmDialogs:** Zehn weitere Bestätigungen mit Abbruchwegen, Fokus,
  einmaliger Ausführung, erneuter Bestätigung nach Fehlern und langen Inhalten.
- **Mailqueue-Gesamtbereinigung:** Kurze interne 100er-Pakete, dauerhaftes Warten,
  Wiederaufnahme, Rechteentzug, Rollback und Teilergebnisse. Browserfälle prüften
  unter anderem 101 Aufträge, echten SMTP-Wartezustand, F5 und Rechte.
- **Gestaltung:** Vorher-/Nachher-Vergleiche der Karten und Dialoge in DE/EN auf
  Desktop und Smartphone; gesonderte damalige Prüfungen beider Hintergrundvarianten.
- **Testtreiber:** Korrigierte Tab-Abbauten und Ticketnavigation. Das vollständige
  Workerbudget wurde auf 90 Minuten erhöht; einzelne UI-Wartezeiten und
  Sicherheitsprüfungen wurden nicht abgeschwächt.

Die früheren Hintergrundvarianten sind kein aktueller Konfigurationsvertrag.
Die heute gültige einzelne Szene steht in der [Theme-Anleitung](theme.md#hintergrund).

## Gezielte Nachbearbeitung nach der Gesamtrunde

### Abgenommene Nachbearbeitung

Die neun Nachbearbeitungspunkte wurden jeweils gezielt geprüft und vom Benutzer
anhand der Vergleichsaufnahmen abgenommen. Sie ersetzen keine neue Gesamt-QA.

| Bereich | Umfang und Nachweisgrenze |
| --- | --- |
| Seitenreiter und segmentierte Umschalter | DE/EN, Desktop bis 320 px, native Pfeile, Tastatur und Chromium-Touch |
| Queue-Warnsymbole und roter Brush | Gezielte Komponenten-/Browserprüfungen und unveränderte Abbruchwege |
| Kürzerer Queue-Wartetext | Je ein DE-/EN-Gesamtlauf mit echtem Warten und 101 Aufträgen |
| Lange Namen und E-Mail-Adressen | Vier Anzeigeorte, Tooltips, Zeilengrenzen und unveränderte Daten nach Abbruch |
| Mobile Formularschutz-/Sammelaktionen | Vollbreite, Reihenfolge und Fokus; Einzelaktion bleibt getrennt |
| Mobile Kontoaktionen | Rechtsausrichtung und abbrechbare Identitätsdialoge |
| Mobile Konfigurationsfooter | Allgemeine Gruppen, Queue, Anbieter und alle 2FA-Rollen |

Der Mailqueue-Assistent ist auch vom Benutzer direkt einschließlich F5 geprüft.
Karten und Bestätigungen wurden anhand der Screenshots abgenommen; nicht jeder
Dialog wurde zusätzlich einzeln in der laufenden Anwendung ausgeführt.

### Weitere Komponentenpakete

| Paket | Vorhandene gezielte Nachweise | Grenze |
| --- | --- | --- |
| Native Notification-Texte | 710 JUnit-Prüfungen; je zehn DE-/EN-Browserfälle für die repräsentativen Varianten; menschlich abgenommen | Übrige Aufrufer anschließend auf Wunsch nur mit JUnit/Stil geprüft |
| Einheitliche Dauer-CustomFields | 129 Anwendungs-JUnit-Prüfungen; DE/EN-Browservergleich für den vorherigen 2FA-Piloten | Übrige Felder ohne zusätzliche Browserrunde übernommen und vom Benutzer gesichtet |
| Native AvatarGroup | 187 Anwendungs-JUnit-Prüfungen; Vorher-/Nachher-Läufe mit je zwei Fällen pro Sprache; zusätzlich 200 synthetische Widgeteinträge | Menschliche Rahmen-/Menüsichtung ist im manuellen Katalog noch offen |

Die Notification-Prüfungen umfassen Sprach-/Fensterisolation, Titel ohne Leerabsatz,
Hervorhebungen und Listen. Die damaligen Messwerte stimmen überein; 23 von 24
Meldungsbildern waren bytegleich. Bei einem englischen mobilen Bild unterschieden
sich 21 Randpixel um höchstens einen Farbwert.

Die Dauerfeld-Prüfungen umfassen alle Speicherformate, Leerwerte, Fehler, Read-only,
Enabled, präzise Fristen, Feldhilfe, Änderungsvergleich und 2FA. Der Pilot hatte
zwölf bytegleiche zugeschnittene Vergleichsbilder.

Die AvatarGroup-Prüfungen umfassen drei, fünf, sechs und neun echte Testempfänger,
Rahmen/Farben, zugängliche Bezeichnungen, vollständige Details, Maus/Tastatur,
Rückfokus und begrenztes Scrollen. Die 200 synthetischen Einträge wurden ohne
entsprechende Datenbankkonten geprüft; die Vorschau versendete nichts.

Die gezielten Browservergleiche verwendeten jeweils denselben unveränderten
Stand für DE und EN. Die erzeugten Bundles sind Buildausgaben, keine von Hand
gepflegte zweite Oberfläche.

## Aktueller Sonar- und JUnit-Stand vom 16.11.2026

Die bestätigte Endanalyse `87587cf9-0fe0-4aeb-b291-2f1c368c157f` gehört zum
Codecommit `35fa9e0284a8ec68b16bcdb4da149bd2f6073667` auf main.
Sie ist kein Sonarnachweis für spätere Aura-Änderungen.

Der einzige offene Hinweis `java:S2259` im `NotificationHelper` wurde durch eine
frühe Rückgabe bei fehlender/leerer Hervorhebung beseitigt. Drei Regressionen
prüfen das Verschwinden und Wiederkehren der Hervorhebung ohne Ersetzen des Literaltexts.

- Null offene Issues, null Hotspots, Quality Gate OK.
- 46 bestehende Ausnahmen unverändert, keine neue Ausnahme oder Regelabschaltung.
- Optimierter Reactor-Install, gemeinsame Coverage, Maven-Modell und Prüfer bestanden.
- 3.123 Anwendungstests bestanden; zwei optionale Caddy-Fälle übersprungen.
- 141 Browser-Helfertests bestanden.
- Abschließend alle 26 Fälle des `NotificationHelperIsolationTest` bestanden.

Keine zusätzliche Browserrunde oder neue große Testrunde gehörte zu diesem Nachweis.

## Abnahme, offene Nachbearbeitung und Grenzen

- **Aura:** Die gemeinsame DE/EN-Runde ist offen. Die instabile Queue-Meldungsprüfung
  und die angepassten Dauerfeld-Fehlerprüfungen benötigen ihren passenden Nachtest;
  Details bleiben in [BUGS](../BUGS.md) und [Testbedarf](testbedarf.md).
- **Scrollleisten:** Die einheitliche Aura-Gestaltung wurde auf dem Branch mit
  COMPONENT-06 in DE/EN geprüft. Das ersetzt weder die offene Gesamt-QA noch die
  menschliche Geräte-/Kontrastprüfung.
- **Menschliche Grenzen:** Echte Mobilgeräte, reale Passwortmanager, Screenreader
  sowie echte Anbieter-/SMTP-Integrationen sind gesonderte Prüfungen.
  Abnahmen und noch offene Sichtungen stehen unter [HUM-01](manual-tests/README.md#hum-01)
  und den weiteren Fällen des manuellen Katalogs.
- **Daten:** Die Mailqueue-Erweiterung änderte das H2-Schema. Ältere lokale
  Entwicklungsdatenbanken müssen frisch angelegt werden; keine automatische Migration.
  Die damaligen Prüfungen änderten keine vorhandenen Entwicklerdaten.
- **Ausführung:** Es wurden keine echten E-Mails versendet oder gespeicherten
  Passwörter als Fixtures verwendet. Teilprüfungen belegen nur ihren jeweiligen Stand.

Die weiter offene Gesamtumsetzung steht im
[Komponentenplan](planung/vaadin-komponenten-abgleich.md).
