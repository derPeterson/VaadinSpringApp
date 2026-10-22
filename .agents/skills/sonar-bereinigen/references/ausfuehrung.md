# Paket-/Komplettmodus: lokale Umsetzung und Nachweis

Nur für einen ausdrücklich beauftragten Paket- oder Komplettlauf lesen/anwenden.
Der Analysemodus ändert keine Codes oder Issue-Status.

## Branches und Integration

1. Paket und aktuelle Finding-Zuordnung prüfen; nach vorangegangenen Paketen kann
   ein ursprünglich zugeordnetes Finding bereits erledigt oder verschoben sein.
2. Quell-/Zielbranch und eigenen/fremden Arbeitsstand zuordnen. Eigene Planänderungen
   sind keine unbekannten Fremdänderungen. Eine sichere Ausgangsbasis herstellen,
   ohne fremde Änderungen einzusammeln, zurückzusetzen oder ungefragt wegzustashen.
   Ist eine sichere Integration nicht möglich, nur den betroffenen Git-Schritt stoppen.
3. Einen passenden eigenen Paket-Branch vom aktuellen lokalen main anlegen. Im
   Komplettmodus Pakete sequenziell abschließen, nicht alle vom alten main abzweigen.
4. Paket innerhalb der geltenden Verträge umsetzen. Nach eigenen Java-/Pythonänderungen
   die jeweiligen Projekt-Stilprüfungen vor Tests ausführen; sinnvolle Regressionen
   und vorhandene relevante Tests prüfen. Keine Assertions zur Warnungsberuhigung schwächen.
5. Eigenen Diff reviewen, nur eigene Paketdateien explizit stagen und mit deutscher
   abgeschlossener Commit-Message lokal committen. Kein pauschales git add und kein Push.
   Bei Commitfehlern anhalten, Paket nicht als integriert ausgeben.
6. Nach bestandenen Prüfungen lokal nach main übernehmen. Konflikte anhand beider
   fachlichen Verträge lösen, passende Nachtests durchführen; keine fremden Arbeiten
   verwerfen oder erfolglose Integration durch destruktiven Reset verschleiern.
7. main über `scripts/sonar/sonar.py scan` als vollständigen Reactor samt gemeinsamer
   Coverage neu bauen/analysieren, den konkreten Serverabschluss abwarten und die
   Paket-Findings prüfen. Keine konkurrierenden Scans desselben Projekts starten.
   Ohne zuordenbaren Nachscan bleibt die Paketabnahme offen.
8. Erst danach im Komplettmodus zum nächsten unabhängigen Paket gehen. Paketmodus
   endet nach diesem Paket. Große neue Folgeprobleme neu planen, kleine lokale
   Folgekorrekturen auf main mit eigenen passenden Commits/Tests und Nachscan beheben.

Der Community-Standard ist: Branch -> Tests -> lokaler Commit/Merge -> main-Nachscan.
Ein separates Sonar-Prüfprojekt für Vorab-Branchscans oder eine andere Edition wäre
ein zusätzlicher Einrichtungsauftrag. Nicht selbst Projektkeys, Branchparameter oder
Hauptprojektbezüge austauschen und dadurch den main-Befund überschreiben.

## Ausnahmen und Statusentscheidungen

Die Benutzerfreigabe für diese beiden Modi umfasst fachlich begründete Entscheidungen:

- **False positive:** Regel trifft nach Prüfung des tatsächlichen Kontexts nicht zu.
- **Accepted:** Hinweis ist berechtigt, die Abweichung ist nach aktuellem fachlichem
  Vertrag vertretbar. Nicht nur akzeptieren, weil Arbeit groß ist, Zeit fehlt oder der
  Benutzer noch nicht geantwortet hat. Grundlegende neue Risikoentscheidungen zurückstellen.
- **Fixed:** Sonar bestätigt eine Codebehebung im Nachscan; nicht manuell als Erfolg setzen.

Jede eigene Ausnahme bekommt direkt am Finding einen eigenständig verständlichen
deutschen Kommentar: konkreter Sachverhalt, geltender Vertrag, weshalb die Regel hier
nicht greift bzw. die Abweichung vertretbar ist, gegebenenfalls verbleibende Grenze.
Keine ausschließlichen Links auf lokale Docs/Tickets und keine Secrets. Wenn die
verfügbare Schnittstelle Kommentar und Status getrennt schreibt, zuerst Begründung
ablegen und beide Schritte nachlesen; Teilerfolg offen melden, nicht blind doppelt posten.

Keine pauschalen Suppressions, Regelabschaltungen, Severity-/Scope-/Gateänderungen,
um die Zahl zu drücken. Bestehende akzeptierte Ausnahmen nur bei konkretem Anlass neu prüfen.

## Tests und offene Abnahme

- Technische Fach-/Sicherheitsregeln in gezielten JUnit-/H2-Regressionen prüfen.
  Vollständige Maven-Anwendungstests zum Abschluss des Komplettlaufs ausführen.
  Nach Integrationsänderungen betroffene Nachweise aktualisieren; Build und Tests unterscheiden.
- Browserfälle sind ein eigener ausdrücklich freizugebender Lauf. Ein Sonar-Auftrag
  allein startet keine Browserrunde. Notwendige Fälle in `docs/testbedarf.md` erhalten
  und im Chat nennen; keine vollständige UI-Abnahme behaupten, wenn nur JUnit lief.
- Keine echte SMTP-Zustellung, Entwicklerdatenbank oder fremden Container als Fixture.
  Neue Anwendungsfunktionen nicht als Nebenprodukt einer Sonar-Regel einführen.

## Wiederaufnahme und Abbruch

Ein lokaler aktueller Zustand unter `.codex/sonar/state.json` hält Modus, Repository,
Branch/Commit, eigenen Diff, Analyse-/Task-IDs, offene Pakete, eigene Paket-/Mergecommits,
Testergebnisse, Folgeauftrag und nächsten Schritt fest. Keine Reportgeschichte oder
Credentials speichern. Nach größeren Phasen und vor Unterbrechung sicher aktualisieren.

Beim Fortsetzen zuerst Git, eigene Prozesse, Scanaufträge und aktuellen Plan abgleichen.
Nie einen bereits erzeugten Paketcommit erneut erstellen oder einen erfolgten Merge
blind wiederholen. Belege nur bei passendem Stand übernehmen. Ein ausdrückliches Stopp
hebt die automatische Fortsetzung auf; eigene Folgeaufträge entfernen.

Wenn Nachscans keinen Fortschritt zeigen oder eine Korrekturschleife entsteht, Ursache
untersuchen. Ohne begründeten nächsten Schritt anhalten und den Rest als Paket/Risiko
melden. Kein unbegrenztes Nachscannen/Refactoring und kein Credit-Dauerbetrieb bis null.
