# Migrationsbericht – 6. Oktober 2026

## Ergebnis

Der Stand aus der gelieferten `.opencode.zip` wurde in einer separaten
Arbeitskopie nach Python migriert. Das ursprüngliche Vaadin-Projekt und die
Eingangs-ZIP wurden nicht überschrieben. Das Ergebnis liegt als
`opencode-python-migration.zip` mit einem `.opencode/`-Ordner vor.

Umgesetzt sind ausschließlich die vier Module `branch`, `prompt`, `benchmark`
und `usage`; `complete` expandiert auf alle vier. Die 22 Modellzuordnungen
bleiben erhalten. Ihre alten `bench-*.md`-Commands wurden durch `start-*.md`
ersetzt, einschließlich der vier bisherigen forced-Modell-IDs. Zusätzlich
gibt es den generischen `/start`-Command.

Der separate `/feature`-Command wurde anschließend ebenfalls entfernt.
Seine Implementierungs-, Java-25-, Test-, Diff- und Commit-Regeln stehen
vollständig im generischen `commands/start.md`. Die Modellvarianten verwenden
weiterhin diesen gemeinsamen Ablauf; eine zweite Workflow-Datei ist nicht nötig.

Die lokalen Anzeigenamen und Command-Schlüssel entsprechen jetzt dem
Ollama-Namen vor dem Doppelpunkt: `qwen3-coder`, `qwen3-coder-q3-tools`,
`devstral-small-2` und `qwen3.8`. Die vollständigen Modell-IDs inklusive Tags
bleiben unverändert. Die lokale Ollama-Liste wurde gelesen: `qwen3-coder:latest`
und `qwen3-coder:30b` haben beide die ID `06c1097efce0`; im Workflow gibt es
dafür weiterhin genau einen Eintrag. Ollama-Tags wurden nicht gelöscht.

Der Benchmark behält Baseline/Final-Builds, Klassen-/Package-Auflösung,
JaCoCo/Surefire-Metriken, Testzeiten, Korrekturrunden, menschliche Eingriffe,
Commit-/Diff-Metriken, externe CSV/Reports und Findings bei. `diff.patch`
enthält direkt die vollständigen Git-Bytes samt Binary-Patches. Usage wird
als Delta derselben OpenCode-Sitzung erfasst und im Benchmark gespeichert.

Die Prompt-Integration verwendet dieselbe Factory, Config, fachlichen Regeln,
Structured Outputs und Artefakt-Schreiber. Der Kern und sämtliche Provider-/
OAuth-Implementierungen wurden anhand der Eingangs-ZIP bytegenau auf Erhalt
geprüft. Änderungen im Prompt-Verzeichnis beschränken sich auf README,
CLI-Hilfetext und die zwei beschriebenen Smoke-Test-Korrekturen.

## Tatsächlich ausgeführte Prüfungen

| Prüfung | Ergebnis |
|---|---|
| Automatische Python-Suite | **54 Tests, 0 Fehler, 0 Failures, 0 Skips** nach dem V2-Usage-Fix |
| Echte temporäre Git-Repositories | Branch-/Status-Prüfungen, Commits, Findings, Text-/Binary-Diff und sauberer Endzustand erfolgreich |
| CLI als eigener Prozess | Begin/Finish, sichere JSON-Task-Übergabe mit Quotes/Zeilenumbrüchen/Shell-Sonderzeichen erfolgreich |
| Fehlerpfade | Dirty Tree, vorhandene lokale/remote Branches, falscher Branch, tracked Findings, fehlende Usage-Session, Build-/CSV-Fehler korrekt abgewiesen |
| Wiederaufnahme und Locking | Retained State/Findings, Retry, idempotentes Finish und konkurrierender Store-Lock geprüft |
| CSV | Einheitliches Schema für neue Runs; bestehende Runs dieses Schemas bleiben erhalten; fremde Schemas abgewiesen; keine doppelte ID; kulturunabhängige Dezimalzahlen |
| Modellgenerator | 22 Zuordnungen; selected/forced erhalten; manuelle Commands geschützt; Konfigurationsfehler vor Mutation abgefangen |
| Usage-Parser mit Offline-Fixtures | Delta, aktive Messages, Teilintervalle, Retries, mehrere Steps, fehlende Werte, Modell-IDs und Vermeidung von Doppelzählung geprüft |
| Installiertes OpenCode **1.18.32** | Bereinigter echter Export erfolgreich mit demselben Usage-Parser ausgewertet; nur Schema/Metriken gespeichert |
| Live-Prompt-Improver mit ChatGPT | CLI → Config → Factory → Client → Structured Output → Pydantic → Original-/Improved-Artefakte erfolgreich |
| Echte Maven-/JaCoCo-Integration | Zweimal vollständig erfolgreich, zuletzt nach Abschluss der Codeänderungen; Java **25.0.4.1**, Maven Wrapper **3.9.16**, JaCoCo **0.8.15** |
| Syntax und Paketprüfung | Alle Python-Quellen geparst; Prompt-/Katalog-Erhalt, entfernte Legacy-Dateien, ZIP-Integrität und Bytecodefreiheit geprüft |

Der echte Maven-Test verwendete ein isoliertes minimales Java-Projekt, keine
Vaadin-Anwendung, keine Datenbank und keinen Applikationsstart. Der Lauf
ging über die tatsächliche Python-CLI, nicht über gemockte Build-Ergebnisse.

| Maven-Testmetrik | Baseline | Final |
|---|---:|---:|
| Tests | 1 | 2 |
| Failures | 0 | 0 |
| Errors | 0 | 0 |
| Skips | 0 | 0 |
| Line Coverage | 75 % | 100 % |
| Branch Coverage | 50 % | 100 % |

Danach lag der Lauf auf `feature/negative_test` mit sauberem Working Tree.
`report.md`, `result.json`, `results.csv`, archivierte `findings.md` und
`diff.patch` waren vorhanden. `git apply --reverse --check` für den Patch
war erfolgreich. Build-Ausgaben laufen auf stderr, damit CLI-stdout ein
gültiges JSON-Dokument bleibt.

Die ersten Live-Prüfungen wurden durch die eingeschränkte Ausführungsumgebung
am Maven-Cache bzw. Windows Credential Manager blockiert. Nach Freigabe wurden
dieselben Prüfungen erfolgreich ausgeführt. Das waren Umgebungszugriffsfehler,
keine als erfolgreich ausgegebenen fehlgeschlagenen Tests.

## Grenzen und verbleibende Punkte

- Der gesamte Vaadin-/Spring-Anwendungsbuild wurde nicht ausgeführt, da seine
  Quelldateien nicht Teil der zu migrierenden ZIP sind und der Umbau ausschließlich
  die Workflow-Infrastruktur betrifft. Der Maven-Aufruf und seine Metriken wurden
  stattdessen im echten isolierten Java-Projekt geprüft.
- Ein vollständiger `/start complete`-Coding-Lauf in der OpenCode-TUI wurde
  nicht ausgeführt. Command-Dateien/Generator, Python-Lifecycle, echter
  Session-Export, echter Maven-Benchmark und Live-Improver wurden einzeln und
  über die dokumentierten Integrationstests geprüft. Die TUI-Modellauswahl und
  Modellauswahl und End-to-End-Coding bleiben beim nächsten echten Auftrag zu prüfen.
  Plugin-Ladung und Tool-Ausführung sind inzwischen im echten Server 2.0.19
  geprüft; die aktive ID wird automatisch übergeben.
- Usage endet am Finish-Snapshot. Laufende Inference-Schritte und die danach
  gesendete Abschlussantwort sind nicht vollständig enthalten. Requests zählt
  abgeschlossene Inference-Schritte, nicht garantiert alle HTTP-Versuche.
- Token- und Kostenwerte stammen aus OpenCode, fehlende Werte bleiben
  unbekannt. `cost: 0` wird als gemeldete Schätzung erhalten und ist kein
  Beleg für reale kostenlose Nutzung. Externe Improver-Aufrufe sind keine
  OpenCode-Session-Nutzung.
- OAuth-Refresh-Lock über mehrere Prozesse/Projekte, automatisches Re-Login,
  spezifisches `response.incomplete`-Handling und alte Keyring-Chunks wurden
  wie gewünscht nicht in den unveränderten Improver eingebaut. Der neue
  Workflow-Store-Lock schützt nur Run-Zustand und CSV.
- Der Stand ist für einen Neustart mit leerem Benchmark-Ordner vorgesehen.
  Es gibt keine Übernahme oder Konvertierung vorheriger Runs/CSV-Schemas und
  keine Bereinigung alter Commands durch den Generator. Er verwaltet allein
  seine eigenen `start-*.md`-Dateien mit dem aktuellen `MARKER`.

## Einspielen und erster Auftrag

### Nachtrag: aktive Sitzung unabhängig von der Oberfläche

Der erste Desktop-Probelauf stoppte vor `begin`, weil die separate Windows-CLI
die aktuelle Desktop-Sitzung nicht kannte. Die Sitzungs-ID war vorhanden,
`opencode.cmd export` lieferte dennoch „Session not found“. Das saubere
Arbeitsverzeichnis ist eine separate Voraussetzung.

`plugins/workflow-usage.js` stellt jetzt `workflow_usage_snapshot` bereit.
Das Tool erhält die tatsächliche Session-ID aus OpenCodes Tool-Kontext und
verwendet in V1 den bereitgestellten SDK-Client. In V2 registriert der
Default-Export mit `id/setup` das Tool über `ctx.tool.transform`; die öffentliche
Client-Abhängigkeit 2.0.19 liest den vollständigen Export. Dienstregistrierung,
Version und Prozess-ID bestätigen, dass genau der aktive Server angesprochen
wird. `session.context` wird wegen fehlender Nachrichten vor Komprimierungen
nicht als Export-Ersatz verwendet. Vor Begin und Finish werden
getrennte bereinigte Exporte geschrieben; das bestehende Python-Usage-Modul
validiert Sitzung, Projekt, Frische und Messzeitpunkt. Es gibt keine Suche nach
der neuesten Sitzung, keine öffentliche Freigabe und kein zusätzliches
fachliches Modul. Die Sperrdatei enthält den zusätzlich benötigten V2-Client.

Die zusätzlichen Prüfungen verwenden das echte Plugin und die Clients
**1.18.32 / 2.0.19** mit einem kontrollierten HTTP-Transport, keine Modellanfragen:
**11 Node-Tests erfolgreich**, einschließlich Server-/Auth-Erhalt, 151 Messages
ohne künstliches Limit, konkurrierenden Sessions, Temp-Dateien, fehlenden
Sitzungen, falschen Serverprozessen, V2-Komprimierungen und Entfernung von
Text-/Credential-Daten. **54 Python-Tests erfolgreich**. Die Python-Suite prüft
auch einen vollständigen Complete-Lifecycle mit zwei SDK-Snapshots, einem
echten temporären Git-Repository und gemockten Maven-/Prompt-Aufrufen.
Ein isolierter echter Server **2.0.19** hat das lokale `.js`-Plugin geladen,
das registrierte Tool ausgeführt und dessen Session über den nativen Client
exportiert. Die Session-ID stammt aus dem Command-/Tool-Aufrufkontext. Zusätzlich
wurde der reale Desktop-Export mit 150 Assistant-Messages und einer Komprimierung
durch den Adapter und den Python-Parser geprüft. Eine alte Nachricht hat keine
Token-/Kostenwerte; solche Lücken bleiben unbekannt und werden nicht als 0 ergänzt.
Kein
Modell wurde angefragt; der produktive Desktop-Server wurde nicht verändert.
Vier verschiedene Oberflächen und ein kompletter Coding-Lauf wurden damit
nicht live getestet. Fehlende Komprimierungszeiten und V2-Retry-Verläufe bleiben
unbekannt. Die gesamte Workflow-Dauer wird weiterhin separat gemessen.

Zum Nachrüsten des bereits installierten Stands: README, `commands/start.md`,
`.gitignore`, `scripts/workflow/usage.py`, `scripts/workflow/cli.py` und die
Tests und beide NPM-Dateien aktualisieren sowie `plugins/workflow-usage.js` und
`scripts/workflow_usage_bridge.mjs` hinzufügen.
`npm --prefix .opencode ci` aus dem Projektroot ausführen, den tatsächlichen
Desktop-Dienst mit dessen 2.0.19-Binary neu starten, Änderungen vor dem
Benchmark auf `main` committen und dann denselben
ConfigService-Auftrag neu senden. Die 22 Modell-Commands und der Katalog
haben sich durch diesen Fix nicht verändert.

Vorhandene `.opencode` sichern und durch den Ordner aus der ZIP ersetzen.
Nicht nur darüberkopieren: sonst bleiben die abgelösten Commands aktiv.
Lokale Zusatzdateien aus der Sicherung gezielt übernehmen, Dependencies mit
`requirements.txt` bereitstellen. Die ausführliche Anleitung steht in `README.md`.

Beispiel bei sichtbar ausgewähltem GPT-6.1 Sol:

```text
/start complete userservice_tests UserService gpt61-sol Create complete tests for UserService.
```

Keine Reviewer-, RAG- oder sonstigen fachlichen Zusatzmodule wurden gebaut.
