# Große Testrunde vom 15.11.2026

Die fünf freigegebenen Nachtpunkte sind technisch abgeschlossen: native Karten,
native Bestätigungen, vollständige Mailqueue-Bereinigung, lokale Zwischenintegration
mit Sonar-Bereinigung und abschließende Gesamtprüfung. Die Gesamtumsetzung der
Vaadin-Komponenten bleibt ausdrücklich offen. Die neuen Karten, Bestätigungen
und der Mailqueue-Assistent sind vom Benutzer abgenommen; die dabei gesammelte
Nachbearbeitung ist zur gezielten Umsetzung freigegeben. Weitere Entscheidungen stehen im
[Komponentenplan](planung/vaadin-komponenten-abgleich.md).

Vergleichsbasis ist die letzte vollständig belegte QA bei `7d127bcb`.
Der geprüfte Quell- und Teststand ist `f3c21623246996988a6fb26372ca07e3fc104d82`,
lokal nach `main` integriert. Die abschließende Dokumentationsbereinigung erfolgt
auf `codex/vaadin-komponenten-folgearbeiten-4`; danach wurden keine Anwendungs-,
Test- oder Buildquellen verändert. Kein Push oder Release wurde durchgeführt.

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

## Abnahme, offene Nachbearbeitung und Grenzen

Der Benutzer hat Karten/Panels und die weiteren Bestätigungsdialoge anhand der
bereitgestellten Screenshots abgenommen. Eine zusätzliche vollständige manuelle
Durchklickprüfung aller Dialoge wurde nicht durchgeführt. Der Mailqueue-Assistent
ist nach eigener Prüfung einschließlich F5 ebenfalls abgenommen. Einzelheiten
stehen unter [HUM-01](manual-tests/README.md#hum-01).
Die dabei gesammelten neun Nachbearbeitungspunkte bleiben offen; ihre Umsetzung
mit gezielten Screenshots, Review und Commit je Punkt ist freigegeben.
Dazu gehören insbesondere die gemeldeten gequetschten
Sicherheits- und 2FA-Rollenreiter auf Smartphonebreite, siehe [BUGS](../BUGS.md).
Die obigen erfolgreichen Tests belegen den geprüften Ausgangsstand, keine
Behebung oder Prüfung dieser Folgepunkte. Echte Mobilgeräte, reale Passwortmanager,
Screenreader und externe Anbieter-/SMTP-Sonderprüfungen bleiben gesonderte
menschliche beziehungsweise ausdrücklich freizugebende Integrationsprüfungen.
Es wurden keine echten E-Mails versendet und keine vorhandenen Entwicklerdaten
oder gespeicherten Passwörter als Testdaten verwendet.

Der gemeldete [Scrollfehler](../BUGS.md#scrollbereiche-erneut-fehlerhaft) bleibt
auf ausdrücklichen Benutzerwunsch zurückgestellt. Betroffene Ansicht, konkrete
Reproduktion sowie gewünschtes Aussehen und Verhalten müssen noch gemeinsam
geklärt werden. Normale Erreichbarkeit wurde geprüft; das ersetzt diesen Nachtest
nicht. Der wartende Bedarf bleibt in [testbedarf.md](testbedarf.md).

Die Mailqueue-Erweiterung ändert das aktuelle H2-Schema. Für ältere lokale
Entwicklungsdatenbanken ist eine frische Datenbank erforderlich; eine Migration
ist im vereinbarten Entwicklungsstand nicht vorgesehen. Die bestehende Datenbank
wurde nicht verändert. Für die Übernahme der Java-Änderungen sind Build und
Anwendungsneustart erforderlich.
