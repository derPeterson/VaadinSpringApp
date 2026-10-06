# Modularer OpenCode-Workflow

`/start` führt einen Auftrag mit den ausgewählten Modulen aus. Es gibt genau
vier Module: **branch**, **prompt**, **benchmark**, **usage**. **complete** ist
das Preset für alle vier. Die eigentliche Coding-Arbeit übernimmt weiterhin
OpenCode nach den Projektregeln; Python bereitet den Lauf vor und wertet ihn aus.

## Inhaltsverzeichnis

- [Installation](#installation)
- [Aufruf in OpenCode](#aufruf-in-opencode)
- [Ablauf und unabhängige Module](#ablauf-und-unabhängige-module)
- [Python-CLI und Request-Datei](#python-cli-und-request-datei)
  - [Hilfe im Terminal anzeigen](#hilfe-im-terminal-anzeigen)
  - [Globale Optionen](#globale-optionen)
  - [Aktion prepare](#aktion-prepare)
  - [Aktion begin](#aktion-begin)
  - [Aktion finish](#aktion-finish)
  - [Aktion abort](#aktion-abort)
  - [Felder der Request-Datei](#felder-der-request-datei)
- [Ergebnisse](#ergebnisse)
- [Usage und Messgrenzen](#usage-und-messgrenzen)
  - [Vergleichbare Benchmarks](#vergleichbare-benchmarks)
- [Tests](#tests)
- [Änderungen und bewusste Grenzen](#änderungen-und-bewusste-grenzen)
- [OpenCode-Referenzen](#opencode-referenzen)

## Installation

Die ZIP enthält den vollständigen Ordner `.opencode` für eine saubere
Neuinstallation. Bei einem bestehenden Python-Stand kann das Änderungspaket
darüberkopiert werden. Vorhandene Python-Runs bleiben erhalten; beim nächsten
CSV-Eintrag wird das Schema des ersten Runs um die neuen Metadaten erweitert.
Alte PowerShell-CSV-Schemas werden weiterhin abgewiesen. Zugangsdaten im Credential Manager und Benutzerprofil bleiben
für den bestehenden Prompt-Improver nutzbar.

Vom Projektroot, mit Python **3.10 oder neuer** (geprüft: 3.14.8):

```powershell
.\.venv\Scripts\python.exe -m pip install -r .opencode/requirements.txt
.\.venv\Scripts\python.exe .opencode/scripts/start.py --help
```

Ohne bestehende virtuelle Umgebung zuerst `python -m venv .venv` ausführen.
Nur `prompt` benötigt die Python-Pakete. Die anderen Module laufen mit der
Standardbibliothek, Git und gegebenenfalls OpenCode/Maven. Für Benchmark:
Java 25, Maven Wrapper, JaCoCo CSV und Surefire XML im bestehenden Projekt.
JDK-/Buildfehler werden nicht durch Versionswechsel oder Test-Skips umgangen.

Für `usage` gehört außerdem `plugins/workflow-usage.js` mit
`scripts/workflow_usage_bridge.mjs` zum installierten Stand. OpenCode lädt das
lokale Plugin beim Start. Die NPM-Abhängigkeiten gehören ebenfalls dazu:
`@opencode/client` **2.0.19** für den Desktop-Server und die vorhandene
V1-Abhängigkeit für CLI **1.18.32**. Aus dem Projektroot installieren:

```powershell
npm --prefix .opencode ci
```

Nach dem Einspielen
OpenCode beziehungsweise dessen Server neu starten; ein laufender Chat lädt
neue Plugin-Dateien nicht automatisch. Danach muss das Tool
`workflow_usage_snapshot` verfügbar sein. Die V2-Plugin-Ladung und der Tool-Aufruf
wurden im echten Server **2.0.19** ohne Modellanfrage geprüft. Die V1-Anbindung
wurde mit dem SDK **1.18.32** geprüft. Kein öffentlicher Share-Link ist erforderlich.

Beim Windows-Desktop läuft der Hintergrunddienst auch nach dem Schließen des
Fensters weiter. Diesen Dienst neu starten, anschließend die App erneut öffnen:

```powershell
& "$env:APPDATA/ai.opencode.desktop/cli/2.0.19/opencode-cli.exe" service restart
& "$env:APPDATA/ai.opencode.desktop/cli/2.0.19/opencode-cli.exe" service status
```

Ein separat installiertes `opencode.cmd` kann eine andere Version verwenden.

## Aufruf in OpenCode

```text
/start complete userservice_tests UserService gpt61-sol Create complete tests for UserService.
/start branch,prompt username_validation - - Reject empty usernames.
/start branch,benchmark userservice_tests UserService gpt61-sol Create complete tests for UserService.
/start branch,usage username_validation - - Reject empty usernames.
```

Format: `/start <modules> <branch|-> <targetClass|-> <modelKey|-> <task>`.
Der Task ist der komplette Rest inklusive Zeilenumbrüchen und Anführungszeichen;
er muss nicht als gesamter String gequotet werden. Ein `-` steht für ein
unbenutztes Feld. Module werden mit Kommas ausgewählt; unbekannte oder doppelte
Module führen zum Fehler. Der Modellschlüssel ist bei Benchmark verpflichtend.

Alle **22 Modellzuordnungen** aus `benchmark-models.json` bleiben erhalten.
Die früheren `/bench-<model>`-Commands heißen jetzt `/start-<model>`:

```text
/start-gpt61-sol complete userservice_tests UserService Create complete tests for UserService.
/start-qwen3-coder branch,benchmark,usage userservice_tests UserService Create complete tests for UserService.
```

Diese Varianten haben das Format
`/start-<modelKey> <modules> <branch|-> <targetClass|-> <task>`.
Die 18 `selected`-Modelle behalten die sichtbare OpenCode-Auswahl bei; sie
muss vor Aufruf zum Modellnamen passen. Die vier `forced`-Ollama-Modelle behalten
ihre bisherigen Modell-IDs im Command-Frontmatter. Generisches `/start` setzt
kein Modell um: bei einem forced-Modell dessen passende Variante verwenden
oder das exakte Modell vorher auswählen. Kataloglabel und tatsächlich im
Export gemessene Provider-/Modell-IDs werden getrennt gespeichert.

Lokale Anzeigenamen und Command-Schlüssel entsprechen dem Ollama-Namen vor
dem Doppelpunkt. Der Tag bleibt intern ausdrücklich erhalten:

| Anzeigename / Modellschlüssel | Command | Tatsächliche Modell-ID |
|---|---|---|
| qwen3-coder | `/start-qwen3-coder` | `ollama-test/qwen3-coder:30b` |
| qwen3-coder-q3-tools | `/start-qwen3-coder-q3-tools` | `ollama-test/qwen3-coder-q3-tools:latest` |
| devstral-small-2 | `/start-devstral-small-2` | `ollama-test/devstral-small-2:24b` |
| qwen3.8 | `/start-qwen3.8` | `ollama-test/qwen3.8:27b` |

`qwen3-coder:latest` und `qwen3-coder:30b` haben in deiner Ollama-Liste
dieselbe ID `06c1097efce0`. Dafür gibt es einen einzigen Workflow-Eintrag,
mit dem expliziten Tag `:30b`. Der doppelte Ollama-Tag wurde nicht gelöscht.
Der alternative `qwen3-coder-q3-tools`-Eintrag hat eine andere ID.

Nach Änderungen am Katalog:

```powershell
.\.venv\Scripts\python.exe .opencode/scripts/generate_start_commands.py
```

Der Generator validiert den gesamten Katalog vor Änderungen. Er überschreibt
und entfernt nur eigene markierte Commands. Manuelle Commands bleiben erhalten.

## Ablauf und unabhängige Module

| Modul | Begin | Finish |
|---|---|---|
| branch | Sauberes `main`, freie lokale/remote Branchnamen prüfen; nach Vorbereitung `feature/<name>` erstellen | Exakt diesen Branch prüfen, dort bleiben |
| prompt | Bestehende Factory/Config/Improver verwenden; Original und verbesserten Prompt extern speichern | Artefakte im Run-Ordner behalten |
| benchmark | Auf sauberem `main` frischen `clean test` + `jacoco:report`-Baseline messen | Auf `feature/*` frisch messen; Vorher/Nachher, CSV, Report und Patch |
| usage | Exakte OpenCode-Sitzung für dieses Projekt exportieren; bisherige Zähler sichern | Delta desselben Exports, Modell-IDs, Tokens, Cache, Reasoning, Requests, Kosten |

Reihenfolge: **Usage-Preflight → Benchmark-Baseline (prepare) → frischer
Usage-Snapshot/Startzeit (begin) → Prompt →
Branch → Coding/Test/Commit durch OpenCode → Endzeit/Usage → finaler Benchmark**.
Bei einem Fehler wird nicht mit einem angeblich erfolgreichen Ergebnis
weitergearbeitet. Fehlgeschlagene Vorbereitung erzeugt keinen Feature-Branch;
bei einem Fehler während/nach `git switch` bleibt der tatsächliche Git-Zustand
zur Prüfung erhalten.

Ein nicht ausgewähltes Modul wird nicht implizit ausgeführt. Ohne `branch`
ändert Python den Branch nicht. Ein reiner `benchmark`-Lauf benötigt weiter
`main` für die Baseline und `feature/*` für Finish; der Aufrufer muss den
Feature-Branch zwischen beiden Schritten ausdrücklich anlegen. `prompt`/`usage`
allein benötigen nur einen sauberen, nicht detached Git-Branch. Der Aufruf
autorisiert die in `start.md` beschriebene Task-Implementierung samt geprüftem
Commit auch bei diesen Kombinationen.

Die Regeln in `commands/start.md` für Java 25, Tests, Diff-Prüfung, gezieltes Staging,
Korrekturrunden und Commit gelten weiterhin. `findings.md` ist genau die
erlaubte untracked Ausnahme beim Finish. Sie wird archiviert und erst nach
erfolgreichem Schreiben aller Artefakte/CSV aus dem Projekt entfernt.
Keine automatischen Fetches, Merges, Rebases, Pushes oder Rückkehr zu `main`.

## Python-CLI und Request-Datei

Der öffentliche Einstieg ist `.opencode/scripts/start.py`. Er ruft
`workflow/cli.py` auf; diese Datei liest Optionen ein und übergibt die gewählte
Aktion an `workflow/runner.py`.

### Hilfe im Terminal anzeigen

Vom Projektroot (statt `python` bei Bedarf `.\.venv\Scripts\python.exe`):

```powershell
python .opencode/scripts/start.py --help
python .opencode/scripts/start.py prepare --help
python .opencode/scripts/start.py begin --help
python .opencode/scripts/start.py finish --help
python .opencode/scripts/start.py abort --help
```

Alle fünf Aufrufe zeigen nur Hilfe an. Sie starten keinen Run, erstellen keinen
Branch und senden keine Modellanfrage. `-h` ist die Kurzform von `--help`.

### Globale Optionen

| Option | Pflicht? | Wirkung / Standard |
|---|---|---|
| `--repo <Pfad>` | Nein | Projektroot mit Git und Maven Wrapper. Automatisch der Elternordner von `.opencode`, unabhängig vom aktuellen Terminalordner. |
| `--store-root <Pfad>` | Nein | Speicherwurzel außerhalb des Projekts; darunter entsteht `<Projektname>/`. Standard: `OPENCODE_BENCHMARK_ROOT`, sonst unter Windows `C:/Dev/AI-Benchmarks`, auf anderen Systemen `~/AI-Benchmarks`. |
| `-h`, `--help` | Nein | Hilfe anzeigen und beenden. |

Globale Optionen müssen **vor** der Aktion stehen. Aktionsoptionen stehen
**nach** `prepare`, `begin`, `finish` oder `abort`. Innerhalb der jeweiligen Gruppe ist
die Reihenfolge der benannten Optionen egal:

```powershell
python .opencode/scripts/start.py --repo C:/Dev/Projects/MyApp --store-root C:/Dev/AI-Benchmarks prepare --request C:/Temp/start-request.json
```

Pfade mit Leerzeichen in Anführungszeichen setzen. `--store-root` ist die
Wurzel, nicht bereits der Projekt-Unterordner.

### Aktion `prepare`

Prüft die Voraussetzungen und misst die Benchmark-Baseline. Noch kein
Improver-Aufruf, kein Feature-Branch und kein laufendes Messfenster. Gibt
`id`, `folder`, `status` und `session_id` als JSON zurück. Diese Run-ID wird
anschließend an `begin --id` übergeben. Prepare ist eine interne Aktion,
kein fünftes Modul. Der `/start`-Command übernimmt diese Schritte automatisch.

| Option | Pflicht? | Wirkung |
|---|---|---|
| `--request <Datei>` | Ja | UTF-8 JSON mit Modulen und Auftrag. |
| `--usage-export <Datei>` | Bei `usage` im `/start`-Ablauf | Preflight-Export zur Prüfung von Sitzung und Projekt vor dem Build. Nach prepare einen neuen Snapshot für begin erzeugen. |
| `-h`, `--help` | Nein | Hilfe anzeigen. |

### Aktion `begin`

Bereitet die ausgewählten Module vor und gibt die echte Run-ID, den Branch,
den Artefaktordner und die effektive Aufgabe als JSON aus. Die Implementierung
der Aufgabe erfolgt danach durch OpenCode, nicht durch `begin` selbst.

| Option | Pflicht? | Wirkung |
|---|---|---|
| `--request <Datei>` | Alternativ zu `--id` | Direkter Start ohne separate Vorbereitung; bei `benchmark,usage` zusammen ist erst prepare erforderlich. |
| `--id <Run-ID>` | Alternativ zu `--request` | Den vorbereiteten Run starten. Genau eine der beiden Optionen ist Pflicht. Repository und Commit müssen seit prepare unverändert sein. |
| `--usage-export <Datei>` | Nein | Expliziter vollständiger **Vorher**-Export der OpenCode-Sitzung. Bei ausgewähltem `usage` ersetzt er den automatischen `opencode export`-Aufruf; sonst ohne Wirkung. |
| `-h`, `--help` | Nein | Nur die Hilfe für `begin` anzeigen. |

### Aktion `finish`

Nach erfolgreicher Implementierung, Prüfung und Commit abschließen. Erfasst
Final-Metriken, archiviert Findings, erzeugt Patch/Report und beendet den Run.

| Option | Pflicht? | Wirkung / Standard |
|---|---|---|
| `--id <Run-ID>` | Ja | Die von `begin` erzeugte ID: 32 kleine Hex-Zeichen. Keine Commit-ID und keine OpenCode-Session-ID. |
| `--human-interventions <Anzahl>` | Nein | Neue Korrekturen oder zusätzliche Vorgaben des Benutzers seit Begin. Selbstständige Modellkorrekturen zählen nicht. Ganze Zahl ≥ 0, Standard `0`. |
| `--correction-rounds <Anzahl>` | Nein | Fehlgeschlagene Verifikationsläufe, die eine weitere Codeänderung erforderten. Ganze Zahl ≥ 0, Standard `0`. |
| `--usage-export <Datei>` | Nein | Expliziter vollständiger **Nachher**-Export derselben Session. Bei `usage` ersetzt er den automatischen Export; ohne `usage` ohne Wirkung. |
| `-h`, `--help` | Nein | Nur die Hilfe für `finish` anzeigen. |

```powershell
python .opencode/scripts/start.py finish --id ACTUAL_RUN_ID --correction-rounds 1 --human-interventions 0
```

Die Anzahl wird vom Aufrufer erfasst und übergeben; Python zählt die Arbeit
des Coding-Agenten nicht selbst mit. Die Platzhalter in Beispielen müssen durch
die tatsächlich zurückgegebenen IDs ersetzt werden. Bei Verwendung eigener
`--repo`-/`--store-root`-Werte dieselben Werte für alle Aktionen verwenden.

### Aktion `abort`

| Option | Pflicht? | Wirkung |
|---|---|---|
| `--id <Run-ID>` | Ja | Den aktiven Lauf mit dieser ID abbrechen und für einen neuen Run freigeben. |
| `-h`, `--help` | Nein | Nur die Hilfe für `abort` anzeigen. |

Abort setzt keine Änderungen zurück, löscht keine Run-Artefakte und wechselt
keinen Branch. Ein abgeschlossener Run kann nicht abgebrochen werden.

### Felder der Request-Datei

Modulauswahl, Branch, Modell und Aufgabe sind **JSON-Felder**, keine direkten
CLI-Optionen wie `--model` oder `--task`. Die Reihenfolge der JSON-Felder ist egal.

| Feld | Pflicht? | Bedeutung |
|---|---|---|
| `modules` | Nein | `"complete"` (Standard), eine Auswahl wie `"branch,prompt"` oder eine Liste wie `["branch", "usage"]`. Nur die vier bekannten Module; keine doppelten Einträge. Ausführungsreihenfolge wird vom Workflow bestimmt. |
| `task` | Immer | Vollständiger, nicht leerer Originalauftrag. |
| `branch` | Bei `branch` | Name ohne `feature/`, z. B. `userservice_tests`; erlaubt sind kleine Buchstaben, Ziffern, `_` und `-`. |
| `target_class` | Bei `benchmark` | Einfacher Java-/JaCoCo-Klassenname, z. B. `UserService`; kein Dateipfad und kein Packagepräfix. |
| `model` | Bei `benchmark` | Schlüssel aus `benchmark-models.json`, z. B. `gpt61-sol` oder `qwen3-coder`; dient der Run-Zuordnung. Schaltet selbst kein aktives OpenCode-Modell um. |
| `session_id` | Bei `usage` im `/start`-Ablauf | Das Tool `workflow_usage_snapshot` liefert die echte ID automatisch. Bei einem expliziten Vorher-Export kann sie aus dessen `info.id` übernommen werden. |
| `prompt_provider` | Nein | Nur für das `prompt`-Modul: Override `chatgpt`, `openai` oder `ollama`; sonst Wert aus der Improver-Config. |
| `prompt_model` | Nein | Nur für das `prompt`-Modul: Modell-Override des Improvers; unabhängig vom Coding-Modell und dem Feld `model`. Ohne Override gilt die Improver-Config. |

Nicht verwendete optionale JSON-Felder weglassen oder auf `null` setzen.
`modules` nur weglassen, wenn alle vier Module gewünscht sind; `null` ist hier
keine gültige Auswahl. Ein `-` als Platzhalter gehört nur zur Slash-Syntax;
OpenCode übersetzt ihn für unbenutzte JSON-Felder in `null`.

Die Slash-Commands lassen OpenCode eine UTF-8 JSON-Datei **außerhalb Git**
schreiben und Python mit deren Pfad aufrufen. Task-Text wird niemals als
Shell-Code ausgewertet. Beispiel `C:/Temp/start-request.json`:

```json
{
  "modules": "complete",
  "branch": "userservice_tests",
  "target_class": "UserService",
  "model": "gpt61-sol",
  "task": "Create complete tests for UserService.",
  "session_id": "ses_REPLACE_WITH_ACTUAL_SESSION"
}
```

```powershell
# Das OpenCode-Tool liefert zuerst den tatsächlichen Preflight-Pfad:
.\.venv\Scripts\python.exe .opencode/scripts/start.py prepare --request C:/Temp/start-request.json --usage-export ACTUAL_PREFLIGHT_PATH
# Nach der Baseline workflow_usage_snapshot erneut aufrufen:
.\.venv\Scripts\python.exe .opencode/scripts/start.py begin --id ACTUAL_ID --usage-export ACTUAL_FRESH_BEGIN_PATH
# OpenCode führt jetzt den zurückgegebenen task aus, prüft und committet.
# Vor finish nochmals einen frischen Snapshot derselben Session erzeugen:
.\.venv\Scripts\python.exe .opencode/scripts/start.py finish --id ACTUAL_ID --usage-export ACTUAL_FRESH_FINISH_PATH --human-interventions 0 --correction-rounds 0
```

Optional `prompt_provider` / `prompt_model` im Request: Overrides nur für den
Improver, unabhängig vom Coding-Modell. Die unveränderte Default-Config nutzt
ChatGPT und setzt vorhandene Credentials voraus. Die vorhandenen manuellen
Login-/Provider-Smoke-Skripte stehen unter `scripts/prompt/tests/`; sie sind
keine Voraussetzung für die Offline-Test-Suite.

Globale Optionen stehen **vor** `prepare`/`begin`/`finish`/`abort`:

```powershell
python .opencode/scripts/start.py --repo C:/Dev/Projects/MyApp --store-root C:/Dev/AI-Benchmarks prepare --request C:/Temp/start-request.json
```

Default-Repo ist der Elternordner von `.opencode`. Default-Store unter Windows:
`C:/Dev/AI-Benchmarks/<Projektname>`; auf anderen Systemen `~/AI-Benchmarks`.
`OPENCODE_BENCHMARK_ROOT` oder `--store-root` überschreiben die Wurzel.
Ein Store im Git-Repository ist verboten. Gleichnamige unterschiedliche
Projekte benötigen getrennte Wurzeln; der Store erkennt die Kollision.

Je Projekt gibt es nur einen aktiven Lauf. Ein Betriebssystem-Lock verhindert
gleichzeitige Zustands-/CSV-Schreibzugriffe; der Lock wird beim Prozessende
freigegeben. `finish` ist für denselben fertigen Lauf wiederholbar, ohne doppelte
CSV-Zeile. Bei finalen Buildfehlern bleiben ID, Zustand, Findings und Messgrenze
für einen Retry erhalten. Nach zusätzlichen Task-Commits setzt ein Retry die
Endzeit und Usage-Grenze neu.

```powershell
python .opencode/scripts/start.py abort --id ACTUAL_ID
```

Abort beendet nur den aktiven Workflow-Status. Es bewahrt Run-Artefakte und
ändert weder Branches noch Task-Dateien. Alle neuen Benchmark-Läufe verwenden
dasselbe CSV-Schema, unabhängig von der gewählten Modulkombination. Nicht
ausgewählte Usage-Felder bleiben leer. Die Erweiterung des bisherigen Python-
Schemas erhält vorhandene Zeilen und benennt `InferenceSeconds` in
`MessageElapsedSeconds` um. Neue Metadaten alter Runs bleiben leer, weil sie
nicht nachträglich belegt werden können. Historische Run-Dateien bleiben
unverändert. Andere abweichende Schemas werden abgewiesen.

## Ergebnisse

```text
C:/Dev/AI-Benchmarks/<Projekt>/
  project.json
  active.json                  # nur während eines aktiven Laufs
  state/<id>.json               # Status/Audit, bleibt nach Abschluss erhalten
  results.csv                  # nur bei ausgewähltem benchmark
  runs/<id>/
    report.md
    result.json
    diff.patch                 # vollständiger Git-Diff inkl. Binärdateien
    findings.md
    original-prompt.md
    improved-prompt.md          # bei prompt
    prompt-metadata.json        # effektiver Improver-Provider/Modell/Dauer
    benchmark-before.json       # bei benchmark: Build-/Surefire-/JaCoCo-Belege
    benchmark-after.json
    usage-preflight.json        # bei usage: nur Vorprüfung, kein Messbeginn
    usage-before.json          # bei usage, nur Metriken/IDs
    usage-after.json
    usage.json
```

Erhaltene Benchmark-Metriken: Testanzahl, Failures, Errors, Skips, reine Testzeit,
Line-/Branch-Coverage der Zielklasse, abgedeckte/gesamte Linien und Branches im
Report, Start-/End-Commit, Branch, Dauer, geänderte Dateien, Insertions/Deletions,
HumanInterventions und Korrekturrunden. Doppelte JaCoCo-Klassennamen werden wie
vorher anhand der eindeutigen Java-Quelldatei und ihres Packages aufgelöst.
Fehlende Reports/Klassen oder Testfehler führen zum Abbruch.

`DurationSeconds` verwendet echte UTC-Zeitstempel. Die Baseline und abschließende
Benchmark-Builds sind ausgeschlossen; Prompt-Vorbereitung, Branch und Coding
sind enthalten. Bei `usage` entsprechen Start und Ende exakt den beiden
Snapshot-Zeitpunkten; `UsageWindowSeconds` zeigt dasselbe Fenster mit drei
Nachkommastellen, `DurationSeconds` mit einer. Die externe Improver-Dauer ist
in der Run-Dauer enthalten, seine Tokens und Kosten fehlen im OpenCode-Export.
CSV-Zahlen verwenden unabhängig von Windows-Locale einen Punkt.

`prompt-metadata.json` speichert die tatsächlich an die bestehende Factory
übergebenen Provider-/Modellwerte aus Config oder Override sowie Zeitpunkte
und Dauer. Das sind die effektiven Improver-Einstellungen, kein unabhängiger
Nachweis einer serverseitigen Modellzuordnung. Die Werte stehen auch in
Report und CSV als `PromptProvider`, `PromptModel`, `PromptDurationSeconds`.
Der Improver-Kern und die Provider bleiben unverändert.

`benchmark-before.json` und `benchmark-after.json` enthalten die originalen
JaCoCo-CSV-Felder der Zielklasse, die Zähler/Zeiten jeder Surefire-Suite und
die erfolgreichen Maven-Aufrufe mit Exitcodes sowie den geprüften Java-Major.
Damit lassen sich Testanzahl, Testzeit und Coverage aus der ZIP nachrechnen.
Bei null gezählten Branches bedeutet die rechnerische 100-%-Anzeige weiterhin
keinen Nachweis getesteter Verzweigungen. XML-Properties, stdout/stderr und
Systemumgebungen werden nicht archiviert. Die Dateien sind Messbelege,
kein kryptografischer Nachweis oder vollständiges Build-Log.

## Usage und Messgrenzen

Im `/start`-Ablauf ist die Quelle das Plugin-Tool `workflow_usage_snapshot`.
Es bekommt die echte Session-ID aus OpenCodes Tool-Kontext und liest die
Sitzung über den SDK-Client des aktiven Servers. V1 verwendet den bereitgestellten
Client. In **2.0.19** fehlt der vollständige Export im Plugin-Kontext; deshalb
verwendet der Adapter den öffentlichen V2-Client und die Dienstregistrierung.
Er prüft Serverversion und Prozess-ID gegen den Prozess, in dem das Plugin läuft.
Ein fremder Server wird vor dem Export abgewiesen. Der vollständige Export
enthält auch Nachrichten vor Komprimierungen; `session.context` wäre dafür zu kurz.
Desktop-App, Terminal, Browser und IDE am selben Dienst verwenden denselben Weg. Ein separates lokales
`opencode.cmd` muss die Sitzung nicht kennen. Es gibt keine Zuordnung anhand
von Sitzungstiteln, der neuesten Sitzung oder einer manuell kopierten ID.

Für einen eigenständigen V2-Server außerhalb des Hintergrunddienstes lässt sich
seine lokale Adresse explizit als Plugin-Option konfigurieren. Auch dann muss
die Prozess-ID passen. Beispiel in der Projektkonfiguration `opencode.json`:

```json
{
  "plugins": [{
    "package": "./.opencode/plugins/workflow-usage.js",
    "options": { "serverUrl": "http://127.0.0.1:4096" }
  }]
}
```

Bestehende Konfiguration ergänzen; keine zweite Kopie des Plugins anlegen.
Die Option richtet keinen Server ein und startet keinen neuen Dienst.

Das Tool liefert `session_id` und `usage_export`. OpenCode übernimmt die ID
in die Request-Datei. Bei Benchmark dient der erste Export als Preflight für
`prepare`; nach der Baseline wird das Tool nochmals aufgerufen und der neue
Export mit `--usage-export` an `begin --id` übergeben. Ein Snapshot vor dem
Abschluss der Baseline wird beim Begin abgewiesen. Ohne Benchmark reicht
der direkte Aufruf `begin --request` mit einem Snapshot.
Unmittelbar vor `finish` ruft es das Tool erneut auf und übergibt den neuen
Export. Session-ID und Projektverzeichnis müssen passen; Python weist
veraltete Plugin-Exporte (über fünf Minuten), ungültige Zeitstempel und eine
Wiederverwendung des Begin-Snapshots bei Finish zurück. Vorherige
Session-Nutzung wird abgezogen. Laufende Zeitspannen enden am tatsächlichen
Exportzeitpunkt, nicht am späteren Einlesen durch Python.

Die temporären JSON-Dateien enthalten ausschließlich Metriken, IDs und das
Projektverzeichnis. Gesprächs-, Code-, Tool- und Credential-Texte werden vor
dem Schreiben entfernt. Das Plugin löscht seine eigenen temporären Dateien
beim Beenden des Servers; nach einem Prozessabbruch können diese bereinigten
Dateien im System-Temp verbleiben. Python speichert die Messwerte im Run-Ordner.
Bei einem entfernten Server laufen Plugin und Python auf dessen Rechner;
die von OpenCode ausgeführten Shell-Aufrufe müssen den Exportpfad lesen können.

Für einen direkten Python-Aufruf außerhalb OpenCode bleiben explizite
V1-Session-Exporte mit `--usage-export` und `opencode export <exactSessionId>`
verfügbar. Ohne SDK-Capture-Zeitstempel gilt dabei der Python-Einlesezeitpunkt;
die echte Capture-Grenze kann nur mit dem Plugin garantiert werden. Auch bei
manuellen Exporten nach prepare erneut exportieren und vor finish wiederholen.
Der rohe V2-CLI-Export hat ein anderes Format; für V2 den bereinigten
Plugin-Export verwenden. Globale `opencode stats` werden nicht als Ersatz verwendet.

| Feld | Bedeutung |
|---|---|
| InputTokens / OutputTokens | Die normalisierten OpenCode-Zähler; keine zusätzliche Addition des Reasonings |
| ReasoningTokens | Separater vom Provider/OpenCode gemeldeter Zähler |
| CacheReadTokens / CacheWriteTokens | Cache-Zähler getrennt von ungecachtem Input |
| Requests | V1: abgeschlossene `step-finish`-Schritte oder Assistant-Messages; V2: abgeschlossene Assistant- und Komprimierungsanfragen |
| RetryEvents | V1: sichtbare Retry-Ereignisse; V2: unbekannt, da kein vollständiger Retry-Verlauf exportiert wird |
| ReasoningSeconds | Zeitspannen der gemeldeten Reasoning-Parts |
| MessageElapsedSeconds | Summe der Message-Zeitspannen innerhalb des Messfensters, inklusive Tool-/Wartezeiten; keine reine Modell-Rechenzeit. Überlappende Messages können zusammen länger sein als die Run-Dauer. Für V2-Komprimierungen fehlt der Endzeitpunkt: unbekannt |
| EstimatedCostUSD | Summe von OpenCodes `cost`, dessen katalogbasierter Kostenschätzung |
| CostStatus | `unavailable`, `reported-zero-actual-unknown` oder `reported-estimate-actual-unknown`; tatsächliche Kosten bleiben unbekannt |
| UsageStartUTC / UsageEndUTC / UsageWindowSeconds | Tatsächliche SDK-Capture-Grenzen und Laufzeit des Usage-Fensters, passend zur Run-Dauer |
| ActualModels | Tatsächliche Provider-/Modell-IDs; mehrere Modelle bleiben sichtbar |
| PendingMessages | Noch laufende Assistant-Messages am Ende des Snapshots |

`step-finish`-Werte und aggregierte Message-Werte werden **nicht doppelt** addiert.
Fehlende oder ungültige Metriken sind JSON `null`, CSV leer und im Report
„nicht verfügbar“. Ein explizit gemeldetes `cost: 0` bleibt 0; das belegt weder
kostenlose Nutzung noch reale Abonnement-/API-Abrechnung. Der Report sagt:
„OpenCode meldet 0 USD; tatsächliche Kosten unbekannt.“ Es werden keine
Preise erfunden. Reasoning kann trotz sichtbarer Reasoning-Zeit 0 Tokens melden.
Ein Provider kann Zähler als 0 liefern, ohne sie differenziert zu unterstützen.

Der Snapshot endet **innerhalb** der Coding-Sitzung bei Finish. Noch nicht
abgeschlossene Inference-Schritte und die anschließende Abschlussantwort sind
nicht vollständig enthalten. Die eigentliche CLI-Aufrufzahl ist nicht immer
die Zahl der HTTP-Requests. Prompt-Improver-Aufrufe außerhalb OpenCode sind im
Session-Export nicht enthalten; es gibt dafür keine vorgetäuschten Usage-Zahlen.
Hilfsanfragen ohne exportierte Message, etwa Titelgenerierung, werden nicht
gezählt. Liefert ein beim Begin laufender Request seine Zähler erst später,
geht dessen gemeldeter Gesamtwert in das Delta ein; Tokens werden nicht anhand
der Laufzeit aufgeteilt.
### Vergleichbare Benchmarks

Für jeden Modellvergleich möglichst eine **frische OpenCode-Sitzung** mit
demselben Ausgangscommit, Auftrag, Modulen und Improver-Einstellungen verwenden.
Die erste ConfigService-Sitzung enthielt viel Vorgeschichte: Das erhöht den
Kontext und Cache-Anteil auch nach Abzug vorheriger Zähler. Ein Delta allein
stellt daher keine identischen Vergleichsbedingungen her. Alte Sessions
bleiben verwendbar; der Workflow erstellt oder wechselt keine Sitzung selbst.
Kein Wechsel des Coding-Modells während eines forced-Laufs.

## Tests

Vom `.opencode/scripts`-Ordner:

```powershell
python -B -m unittest discover -s tests -v
node --test tests/test_workflow_usage.mjs
```

Die automatische Suite nutzt temporäre echte Git-Repositories, Offline-Exports
und gemockte Provider/Benchmark-Builds. Sie prüft die Lifecycle- und Fehlerpfade,
Metriken, Patch-/Findings-/CSV-Erhalt, Locking, Modellgenerator und Integration
des existierenden Prompt-Improvers. Sie verbraucht keine Modellanfragen und
startet keine Anwendung. Live-Provider-Smoke-Tests separat und bewusst ausführen.
Die Node-Suite benötigt die in `.opencode/package.json` deklarierte
NPM-Abhängigkeiten (`npm ci` in `.opencode`).
Sie prüft das echte Plugin und SDK mit einem kontrollierten HTTP-Transport:
aktiver Server und Authentifizierung, mehrere Sitzungen, vollständige
Message-Liste, V2-Registrierung und -Export, Serverprozess-Prüfung, Komprimierungen,
bereinigte Exporte und Fehlerpfade. Sie startet keine Modellanfrage.
Die konkreten Prüfungen und Grenzen dieser Migration stehen in `MIGRATION_REPORT.md`.

## Änderungen und bewusste Grenzen

- `ai-benchmark.ps1`, `generate-benchmark-commands.ps1` und `feature-time.ps1`
  entfernt. Benchmark und Command-Generator sind Python.
- Die 22 alten generierten `bench-*.md` durch 22 `start-*.md` ersetzt; zusätzlich
  ein generisches `start.md`. Der bisherige `/feature`-Command ist entfernt;
  seine Implementierungs-, Prüf- und Commit-Regeln stehen direkt in `start.md`.
- Lokale Modellnamen vereinheitlicht: nur der Name vor `:`, ohne Kürzel wie
  `qwen30b` oder `devstral24b`. Vollständige Modell-IDs mit Tags bleiben gleich.
- Bytecode entfernt und ignoriert. Python-Abhängigkeiten deklarativ erfasst.
- Callback-Smoke-Test startet den Listener; Improver-Smoke-Test verwendet die
  Config-Factory; CLI-Hilfe nennt alle drei Provider.
- Prompt-Kern, Config und Provider-/OAuth-Implementierung unverändert. Die
  früher genannten Refresh-Lock-, Re-Login-, `response.incomplete`- und
  Keyring-Chunk-Aufräumpunkte bleiben für später; der Workflow-Store-Lock ist
  kein globaler OAuth-Refresh-Lock für unterschiedliche Projekte/Prozesse.
- `package.json` / `package-lock.json` aktualisiert: V2-Client **2.0.19** ergänzt,
  vorhandene V1-Abhängigkeit erhalten und JavaScript als ESM deklariert.
  Benchmark, Lifecycle und Prompt-Integration bleiben Python.
- Keine Reviewer-, RAG- oder sonstigen fachlichen Module hinzugefügt.

## OpenCode-Referenzen

[Command-Argumente und Modell-Frontmatter](https://opencode.ai/docs/commands/),
[CLI-Session-Export](https://opencode.ai/docs/cli/),
[Lokale Plugins und Tool-Kontext](https://opencode.ai/docs/plugins/),
[Exportstruktur](https://github.com/anomalyco/opencode/blob/dev/packages/opencode/src/cli/cmd/export.ts),
[Usage-Zähler im Session-Schema](https://github.com/anomalyco/opencode/blob/dev/packages/schema/src/v1/session.ts).
[V2-Plugin-Migration](https://opencode.ai/v2/docs/build/plugins/migrate-v1),
[V2-Client](https://opencode.ai/v2/docs/build/client),
[V2-API](https://opencode.ai/v2/docs/api).
Die installierten Versionen 1.18.32 und 2.0.19 wurden getrennt geprüft.
