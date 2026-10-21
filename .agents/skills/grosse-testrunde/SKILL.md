---
name: grosse-testrunde
description: "Große Testrunde für VaadinSpringApp durchführen oder nach Unterbrechung fortsetzen: Änderungen seit der letzten vollständigen QA bewerten, sinnvolle Regressionen ergänzen, JUnit und Browser DE/EN prüfen und Fehler beheben. Bei ausdrücklich beauftragter Gesamt-QA verwenden, nicht für einzelne Änderungen, gezielte Tests oder das Erstellen/Erklären dieses Skills."
---

# Große Testrunde

Arbeite im tatsächlichen VaadinSpringApp-Repository. Lies zuerst `README.md`,
`AGENTS.md`, `docs/tests.md`, `docs/status.md` und die relevanten Teile von
`browser-tests/README.md`. Die vorhandenen Anleitungen und Runner sind maßgeblich;
keinen zweiten Testrunner und keine OpenCode-Portierung aufbauen.

## Auftrag und Grenzen

- Ein ausdrückliches „Starte die große Testrunde“ beauftragt den unten beschriebenen
  Gesamtumfang einschließlich Browser DE/EN, sinnvoller Testergänzungen und
  fachlicher Fehlerkorrekturen. Eine solche Freigabe nicht erneut abfragen.
- Die automatische Auswahl dieses Skills allein erlaubt keine Testausführung.
  Bei „Skill anlegen/prüfen“, Planungsfragen oder einem ausdrücklich verlangten
  Trockenlauf nur die beauftragte Analyse/Validierung durchführen, keine Gesamtrunde.
- Bestehende Funktions-, Sicherheits- und Designverträge erhalten. Bei neuer
  Fachfunktion oder grundlegender Verhaltens-/Architekturänderung gezielt fragen;
  unabhängige Prüfungen währenddessen fortsetzen. Kein eigenmächtiger Umbau.
- Keine Commits, Merges, Pushes, realen Mails, Sonar-Statuswechsel oder Eingriffe in
  Entwicklerdaten/fremde Prozesse. Eigene isolierte Testumgebungen sind enthalten.
  OpenCode-Dateien und -Konfiguration bleiben unverändert.

## 1. Stand und Fortsetzung prüfen

- Git-Branch, Arbeitsstand und noch laufende eigene Prüfungen ermitteln. Fremde
  Änderungen erhalten. Vorhandene eigene Prozesse zuerst zuordnen/abwarten;
  keine doppelten Runner oder konkurrierenden Image-Builds starten.
- Den [Zustandsvertrag](references/zustand.md) lesen. Letzte vollständig erfolgreiche
  Runde und gegebenenfalls unterbrochene aktuelle Runde unterscheiden. Keine
  alte Testanzahl oder allein ein Datum als Beleg für den aktuellen Code verwenden.
- Änderungen seit dem belegten Vergleichsstand über tatsächliche Git-Diffs,
  uncommittete Quellen und Tests bewerten, nicht nur über Commit-Nachrichten.
  Bei fehlender/ungeeigneter Baseline die Unsicherheit nennen und einen ersten
  aktuellen Vergleichsstand aufbauen; keine erfundene Änderungshistorie erzeugen.
- Eine kurze Arbeitsliste nach Funktionen führen: bereits passend getestet,
  JUnit-Lücke, Browserlücke oder menschliche Sonderprüfung. Diese Liste dient dem
  aktuellen Auftrag, nicht einer neuen dauerhaften Triage-Sammlung.

## 2. Kontingent und Wiederaufnahme

- Beim Start, zwischen größeren Abschnitten und gelegentlich während längerer
  Arbeit das Kontingent über die verfügbare Kontostatusfunktion prüfen, etwa
  `get_usage_limits`. Nicht nach jedem Werkzeugaufruf pollen. Alle relevanten
  Fenster beachten: `usedPercent` ist verbraucht, 100 bedeutet 0 Prozent Rest.
- Bei ausgeschöpftem Kontingent den Zwischenstand sichern und substantive Arbeit
  beenden. Kein bewusster längerer Weiterbetrieb auf Credits; ein geringer Verbrauch
  bis zum Erkennen ist toleriert. Keine Credits kaufen, Resets einlösen oder zur
  API-Abrechnung wechseln. Bei fehlenden Statusdaten die Unsicherheit offenlegen.
- Bei knappem Kontingent die freigegebene Fortsetzung schon vor einem möglichen
  Abbruch vorbereiten: vorhandenen Folgeauftrag wiederverwenden oder über die
  App-Automationsfunktion eine Nachkontrolle im selben Chat nach dem relevanten
  Reset einrichten. Kein eigener Scheduler, kein geratenes Resetdatum.
- Bei mehreren erschöpften Fenstern den späteren nötigen Reset berücksichtigen;
  beim Aufwachen Kontingent erneut prüfen. Zwischenstand, Repository, Branch,
  Befugnisse und nächste Schritte im Folgeauftrag eindeutig benennen.
- Ein Folgeauftrag prüft zuerst Fertigstellung und laufende Prozesse. Nur offene
  Arbeit fortsetzen, keine erfolgreichen unveränderten Prüfungen duplizieren.
  Nach Abschluss den eigenen Folgeauftrag entfernen. Bei fehlender technischer
  Unterstützung die nicht eingerichtete Fortsetzung ausdrücklich melden.
- Ein ausdrückliches Stopp des Benutzers beendet die Arbeit und hebt die automatische
  Fortsetzung auf. Nicht als Kontingentpause behandeln. Keine Wiederaufnahme erzwingen.

## 3. Sinnvolle Regressionen und Pflege

- Vorhandene Tests zuerst prüfen. Fachregeln, Rechte, Grenzen, Transaktionen und
  Konkurrenz bevorzugt in JUnit; sichtbare Journeys/Zusammenspiel im Browser testen.
  Keine Testkopien, Methodenaufruf-Tests oder Browserfälle für jedes interne Refactoring.
- Fehlende relevante Fälle ergänzen und vorhandene Fälle fachlich anpassen.
  Browserfälle in die passende Suite einordnen, Schritte/IDs und Sprachverträge
  pflegen. Keine Assertions abschwächen, Fehler verstecken oder pauschal wiederholen.
- Änderungen auf überflüssigen Code und Ressourcen prüfen; DE-/EN-Kataloge,
  dynamische Textverwendungen und Dokumentationsverweise berücksichtigen.
  Nur nachweislich unbenutzte Teile entfernen. Keine zusätzliche Großbereinigung.
- Nach Java-/Pythonänderungen die vorgeschriebenen Formatierungs-/Stilprüfungen
  vor Tests ausführen. Die jeweils gültigen Befehle stehen in `AGENTS.md`.

## 4. Vollständige Prüfung und Fehlerkorrektur

- Java 25 bestätigen; Shared-Modul bei frischem Stand/Änderungen installieren.
  Vollständige Anwendungstests mit optimiertem Produktionsbuild sowie vorhandene
  Shared-, Browser-Helfer-, Frontend- und Offline-Werkzeugprüfungen ausführen.
  Provider-Smokes und der echte Mail-Sichtversand gehören nicht zur Gesamtrunde.
- Vorhandenen Browserrunner nutzen: `all` auf DE und EN, eigene isolierte H2-/Mailpit-
  Umgebungen und dasselbe unveränderliche aktuelle Anwendungsimage. Worker wie in
  der Projektanleitung einsetzen; keinen doppelten Build für die zweite Sprache.
- Keine Appimages während laufender Browser-/Neustartfälle bauen, umhängen oder
  entfernen. Quellstand und Image-ID der Belege festhalten.
- Fehler reproduzieren und Ursache unterscheiden: Anwendung, falsche Testannahme,
  Infrastruktur oder instabiler Test. Innerhalb der bestehenden Verträge korrigieren
  und passende Nachtests ausführen. Blockierte Teile klar benennen.
- Nach Anwendungskorrekturen am finalen Stand neu bauen und betroffene Prüfungen
  wiederholen. Für einen abschließend vollständig aktuellen Browsernachweis müssen
  beide Sprachen zum finalen Image passen. Erfolg aus einem früheren Image nicht
  stillschweigend übertragen; unveränderte passende Belege weiterverwenden.
- Sonar über eine verfügbare konfigurierte Anbindung mitprüfen. Schwerpunkt dieser
  Aufgabe bleibt die Testabnahme, nicht die vollständige Sonar-Bereinigung.
  Projekt/Analysestand prüfen und kleine echte Fehler oder Blocker beheben, wenn die
  Korrektur fachlich klar, lokal begrenzt und mit passenden Regressionen absicherbar ist.
  Nicht allein nach Schweregrad oder Zeilenzahl entscheiden.
- Größere Refactorings, neue Architektur oder umfangreiche Finding-Pakete für den
  separaten Auftrag „Sonar bereinigen“ festhalten. Keine fortlaufende Umbau-/Scan-
  Schleife bis null Issues in die Testrunde aufnehmen. Dringende ungelöste Risiken
  deutlich melden; sie nicht wegen eines bestandenen Tests als unbedenklich darstellen.
- Nach finalen Änderungen den vorgesehenen Scan abschließen und Findings/Gate
  ehrlich berichten. Testabschluss und Sonar-Qualitätsstand getrennt beurteilen:
  Tests können vollständig bestanden sein, obwohl größere Sonar-Findings offen sind
  oder das Gate fehlschlägt. Das ist kein insgesamt grüner Qualitätsstand, aber auch
  kein Auftrag, die Nacht um unbegrenzte Umbauten zu verlängern.
- Keine Credentials aus Dateien herauslesen und keine fehlende Anbindung im Rahmen
  dieses Skills installieren. Keine Sonar-Statusänderungen zur Zahlenbereinigung.
- Solange Sonar noch nicht angebunden ist, verfügbare Exporte nur mit ihrem wirklichen
  Stand kennzeichnen. Offenen Sonar-Nachweis gesondert ausweisen; keine bestätigten
  Behebungszahlen oder grünen Gates aus einer veralteten Analyse ableiten.

## 5. Abschluss

- Vollständig nur melden, wenn die erforderlichen Prüfungen zum Endstand belegt
  sind. Fehler, nicht ausgeführte Fälle, unerwartete Skips, Abbauprobleme und offene
  Sonar-Nachweise sichtbar halten. Geplante manuelle Opt-in-Tests getrennt nennen.
- `docs/status.md` als einzigen knappen aktuellen Ergebnisbericht ersetzen:
  Vergleichs-/Endstand, wichtige Korrekturen, neue/überarbeitete Tests nach Funktion,
  echte Testzahlen je Ebene/Sprache, Nachtests, Sonar und verbleibende Lücken.
  Normalerweise 1–2, höchstens etwa 4 lesbare Seiten; keine Einzelfall-Protokollflut.
- Gültiges Verhalten in der bestehenden Fachdokumentation aktualisieren. Erledigte
  Pläne/TODO-Verweise gemäß `AGENTS.md` entfernen, keine historischen Runberichte
  oder Planarchive hinzufügen. Keine zusätzliche Coverageauswertung beauftragen.
- Zustand sichern, nur eigene Testumgebungen abbauen, eigene erledigte Folgeaufträge
  entfernen, Diff prüfen und knapp auf Deutsch berichten. Die erfolgreiche Baseline
  nur bei vollständigem belegtem Abschluss ersetzen.
