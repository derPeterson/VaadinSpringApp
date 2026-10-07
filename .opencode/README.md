# Modularer OpenCode-Workflow

`/start` führt einen Auftrag mit den ausgewählten Modulen aus. Es gibt genau
vier Module: **branch**, **prompt**, **benchmark**, **usage**. **complete** ist
das Preset für alle vier. Die eigentliche Coding-Arbeit übernimmt weiterhin
OpenCode nach den Projektregeln; Python bereitet den Lauf vor und wertet ihn aus.

## Inhaltsverzeichnis

- [Auftrag, Improver, Workflow und Projektregeln](#auftrag-improver-workflow-und-projektregeln)
- [Installation](#installation)
- [Aufruf in OpenCode](#aufruf-in-opencode)
- [Ablauf und unabhängige Module](#ablauf-und-unabhängige-module)
  - [Analyse und Implementierung](#analyse-und-implementierung)
- [Python-CLI und Request-Datei](#python-cli-und-request-datei)
  - [Hilfe im Terminal anzeigen](#hilfe-im-terminal-anzeigen)
  - [Globale Optionen](#globale-optionen)
  - [Aktion prepare](#aktion-prepare)
  - [Aktion begin](#aktion-begin)
  - [Aktion finish](#aktion-finish)
  - [Aktion status](#aktion-status)
  - [Aktion abort](#aktion-abort)
  - [Felder der Request-Datei](#felder-der-request-datei)
  - [Fehlgeschlagene Läufe und Abbrüche](#fehlgeschlagene-läufe-und-abbrüche)
- [Ergebnisse](#ergebnisse)
  - [Bericht und Findings](#bericht-und-findings)
- [Usage und Messgrenzen](#usage-und-messgrenzen)
  - [Prompt-Improver: eigene Usage und Fehlerbehandlung](#prompt-improver-eigene-usage-und-fehlerbehandlung)
  - [Vergleichbare Benchmarks](#vergleichbare-benchmarks)
- [Tests](#tests)
- [Änderungen und bewusste Grenzen](#änderungen-und-bewusste-grenzen)
- [OpenCode-Referenzen](#opencode-referenzen)

## Auftrag, Improver, Workflow und Projektregeln

Ein kurzer Auftrag genügt, wenn Ziel, Umfang und benötigte Quellen eindeutig sind.
Build-, Benchmark- und Abschlussregeln müssen nicht bei jedem Auftrag wiederholt werden.

| Ebene | Aufgabe |
|---|---|
| Benutzerauftrag | Fachliches Ziel, gewünschter Umfang, Quellen und besondere Grenzen festlegen. |
| Prompt-Improver | Den Auftragstext präzisieren und strukturieren; keine Anforderungen oder Entscheidungen erfinden. |
| `/start` | Gewählte Module, Vorbereitung, Aufgabenbearbeitung, Prüfung, Git-Abschluss und Run-Artefakte koordinieren. |
| Projekt-`AGENTS.md` | Dauerhafte Architektur-, Stil-, Test-, Übersetzungs- und Arbeitsregeln festlegen. |

Der Improver erhält ausschließlich den Auftragstext. Er liest weder Projektdateien
noch `AGENTS.md` oder eine erwähnte `findings.md`. Das liest der ausführende Agent
im `/start`-Ablauf: Originalauftrag und verbesserten Prompt zusammen berücksichtigen,
referenzierte Quellen für die beauftragten IDs lesen und fehlenden Kontext zunächst
dort prüfen. Eine Referenz allein ist kein Nachweis des Dateiinhalts. Der Improver
entscheidet weder über die Implementierung noch über Berechtigungen.

Die konfigurierten Improver-Defaults bleiben allgemeine Vorgaben. Sie erlauben
keine zusätzlichen Änderungen bei einem Analyseauftrag und ersetzen keine
Projektregeln. Der Workflow kennt Analyse und Implementierung getrennt; explizite
Benutzervorgaben gelten weiterhin.

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
Branch → Analyse oder Implementierung/Tests/Commit durch OpenCode → Endzeit/Usage → finaler Benchmark**.
Bei einem Fehler wird nicht mit einem angeblich erfolgreichen Ergebnis
weitergearbeitet. Fehlgeschlagene Vorbereitung erzeugt keinen Feature-Branch;
bei einem Fehler während/nach `git switch` bleibt der tatsächliche Git-Zustand
zur Prüfung erhalten.

Ein nicht ausgewähltes Modul wird nicht implizit ausgeführt. Ohne `branch`
ändert Python den Branch nicht. Ein reiner `benchmark`-Lauf benötigt weiter
`main` für die Baseline und `feature/*` für Finish; der Aufrufer muss den
Feature-Branch zwischen beiden Schritten ausdrücklich anlegen. `prompt`/`usage`
allein benötigen nur einen sauberen, nicht detached Git-Branch. Der Aufruf
autorisiert die in `start.md` beschriebene Durchführung im Umfang des
Originalauftrags auch bei diesen Kombinationen.

Die Regeln in `commands/start.md` für Java 25, Tests, Diff-Prüfung, gezieltes Staging,
Korrekturrunden und Commit gelten weiterhin. `findings.md` ist genau die
erlaubte untracked Ausnahme beim Finish. Sie wird archiviert und erst nach
erfolgreichem Schreiben aller Artefakte/CSV aus dem Projekt entfernt.
Keine automatischen Fetches, Merges, Rebases, Pushes oder Rückkehr zu `main`.

### Analyse und Implementierung

Die ursprüngliche Aufgabe bestimmt den erlaubten Umfang. OpenCode setzt im
Request `task_mode` auf `analysis`, wenn ausschließlich eine Analyse gewünscht
ist; bei einem Änderungsauftrag auf `implementation`. Lösungsvorschläge allein
machen aus einer Analyse keinen Änderungsauftrag. Der Improver darf diesen
Umfang nicht erweitern. Es gibt keinen zusätzlichen Slash-Parameter und kein
neues Modul; `complete` verwendet weiterhin alle vier bestehenden Module.

Bei **analysis** werden Produktionsdateien, Tests und andere Task-Dateien nicht
geändert. Offene Probleme stehen in `findings.md`; Umfang, Prüfungen und Grenzen
der Analyse im Arbeitsbericht. Es gibt keinen Task-Commit oder leeren Commit.
Die gewählten Branch-/Benchmark-Module dürfen weiterhin einen Feature-Branch
anlegen beziehungsweise bestehende Tests und JaCoCo ausführen. Benchmark-Builds
erzeugen ihre üblichen ignorierten Build-Dateien. Ein fehlgeschlagener Check
wird als Problem oder Grenze dokumentiert, nicht durch Codeänderungen repariert.
Ein fehlgeschlagener Benchmark-Build kann nicht erfolgreich abgeschlossen werden.

Python prüft beim Abschluss zusätzlich, dass HEAD noch dem Start-Commit
entspricht. Unerwartete Task-Commits führen zum Abbruch des Finish-Aufrufs;
die Arbeit wird nicht automatisch zurückgesetzt. Uncommittete Änderungen
werden weiterhin durch die vorhandene Clean-Tree-Prüfung abgewiesen.
Eine erfolgreiche Analyse erzeugt einen leeren `diff.patch`, unveränderte
Start-/End-Commits und null geänderte Dateien.

Bei **implementation** gelten die bestehenden Implementierungs-, Test- und
Commit-Regeln. Direkte Python-Requests ohne `task_mode` behalten dieses bisherige
Verhalten. Bei älteren gespeicherten Runs fehlt die Auftragsart; Status/Report
kennzeichnen sie als nicht erfasst und raten nicht anhand des Aufgabentexts.

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
python .opencode/scripts/start.py status --help
python .opencode/scripts/start.py abort --help
```

Alle sechs Aufrufe zeigen nur Hilfe an. Sie starten keinen Run, erstellen keinen
Branch und senden keine Modellanfrage. `-h` ist die Kurzform von `--help`.

### Globale Optionen

| Option | Pflicht? | Wirkung / Standard |
|---|---|---|
| `--repo <Pfad>` | Nein | Projektroot mit Git und Maven Wrapper. Automatisch der Elternordner von `.opencode`, unabhängig vom aktuellen Terminalordner. |
| `--store-root <Pfad>` | Nein | Speicherwurzel außerhalb des Projekts; darunter entsteht `<Projektname>/`. Standard: `OPENCODE_BENCHMARK_ROOT`, sonst unter Windows `C:/Dev/AI-Benchmarks`, auf anderen Systemen `~/AI-Benchmarks`. |
| `-h`, `--help` | Nein | Hilfe anzeigen und beenden. |

Globale Optionen müssen **vor** der Aktion stehen. Aktionsoptionen stehen
**nach** `prepare`, `begin`, `finish`, `status` oder `abort`. Innerhalb der jeweiligen Gruppe ist
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
den Artefaktordner, `task_mode` und die effektive Aufgabe als JSON aus. Die Durchführung
der Aufgabe erfolgt danach durch OpenCode, nicht durch `begin` selbst.

| Option | Pflicht? | Wirkung |
|---|---|---|
| `--request <Datei>` | Alternativ zu `--id` | Direkter Start ohne separate Vorbereitung; bei `benchmark,usage` zusammen ist erst prepare erforderlich. |
| `--id <Run-ID>` | Alternativ zu `--request` | Den vorbereiteten Run starten. Genau eine der beiden Optionen ist Pflicht. Repository und Commit müssen seit prepare unverändert sein. |
| `--usage-export <Datei>` | Nein | Expliziter vollständiger **Vorher**-Export der OpenCode-Sitzung. Bei ausgewähltem `usage` ersetzt er den automatischen `opencode export`-Aufruf; sonst ohne Wirkung. |
| `-h`, `--help` | Nein | Nur die Hilfe für `begin` anzeigen. |

### Aktion `finish`

Nach erfolgreich durchgeführter Analyse oder geprüfter Implementierung mit Commit abschließen. Erfasst
Final-Metriken, archiviert Findings, erzeugt Patch/Report und beendet den Run.

| Option | Pflicht? | Wirkung / Standard |
|---|---|---|
| `--id <Run-ID>` | Ja | Die von `begin` erzeugte ID: 32 kleine Hex-Zeichen. Keine Commit-ID und keine OpenCode-Session-ID. |
| `--human-interventions <Anzahl>` | Nein | Neue fachliche Korrekturen oder zusätzliche Aufgabenanweisungen des Benutzers seit Begin. Zugriffsgenehmigungen, automatische Wiederaufnahmen, rein technisches Fortsetzen, Kontextwiederherstellung und selbstständige Modellkorrekturen zählen nicht. Ganze Zahl ≥ 0, Standard `0`. |
| `--correction-rounds <Anzahl>` | Nein | Fehlgeschlagene Verifikationsläufe, die eine weitere Codeänderung erforderten. Ganze Zahl ≥ 0, Standard `0`. |
| `--usage-export <Datei>` | Nein | Expliziter vollständiger **Nachher**-Export derselben Session. Bei `usage` ersetzt er den automatischen Export; ohne `usage` ohne Wirkung. |
| `--summary-file <Datei>` | Bei `/start` ja; direkte CLI optional | UTF-8 Markdown außerhalb Git: durchgeführte Arbeit, bei Analyse der geprüfte Umfang und Grenzen; bei Implementierung auch Verhaltensänderungen für `report.md`. Keine zusätzliche dauerhafte Run-Datei. |
| `-h`, `--help` | Nein | Nur die Hilfe für `finish` anzeigen. |

```powershell
python .opencode/scripts/start.py finish --id ACTUAL_RUN_ID --summary-file C:/Temp/start-summary.md --correction-rounds 1 --human-interventions 0
```

Die Anzahl wird vom Aufrufer erfasst und übergeben; Python zählt die Arbeit
des Coding-Agenten nicht selbst mit. Die Platzhalter in Beispielen müssen durch
die tatsächlich zurückgegebenen IDs ersetzt werden. Bei Verwendung eigener
`--repo`-/`--store-root`-Werte dieselben Werte für alle Aktionen verwenden.

### Aktion `status`

Liest den gespeicherten Zustand als JSON. Ohne ID wird ausschließlich der
aktive Lauf ausgewählt. Gibt es keinen aktiven Lauf, lautet der Status `idle`;
der zuletzt abgeschlossene Lauf wird nicht stillschweigend ausgewählt.

| Option | Pflicht? | Wirkung |
|---|---|---|
| `--id <Run-ID>` | Nein | Gezielt einen vorhandenen aktiven, abgeschlossenen oder abgebrochenen Run lesen. Ohne ID den aktiven Run anzeigen. |
| `-h`, `--help` | Nein | Hilfe anzeigen. |

```powershell
python .opencode/scripts/start.py status
python .opencode/scripts/start.py status --id ACTUAL_RUN_ID
```

Die Ausgabe zeigt die Phase, Auftragsart, ausgewählten Module, den gespeicherten
Branch, Run-/Zustandspfad, vorhandene Hauptartefakte und Hinweise zu möglichen
nächsten Schritten. `active_id` benennt den derzeit aktiven Lauf, auch wenn
mit `--id` ein anderer historischer Run ausgewählt wurde. `error_recorded`
zeigt an, ob Fehlerdetails in der Zustandsdatei vorhanden sind; diese werden
nicht ungefiltert in der Statusausgabe wiederholt.
`failure_count`, `last_failure_phase` und `failure_archive_error_recorded`
zeigen den erfassten Fehlerverlauf und eine noch unvollständige Archivierung.

Die Abfrage startet keine Tests oder Modellanfragen, führt keine Git-Befehle
aus, legt weder Store noch Lockdatei an und schreibt keine Dateien. Sie kann
während eines laufenden Workflow-Prozesses benutzt werden. Die Ausgabe ist
eine Momentaufnahme gespeicherter Angaben; sie bestätigt weder den aktuellen
Git-Zustand noch, ob ein Prozess noch läuft. Bei `preparing`/`finishing` zunächst
einen noch laufenden Prozess abwarten. Die Hinweise führen keinen Retry oder
Abort aus und ersetzen keine Prüfung der Voraussetzungen.

### Aktion `abort`

| Option | Pflicht? | Wirkung |
|---|---|---|
| `--id <Run-ID>` | Ja | Den aktiven Lauf mit dieser ID abbrechen und für einen neuen Run freigeben. |
| `--reason <Text>` | Nein | Grund im Report und in der CSV festhalten. Ein ausdrücklich leerer Grund wird abgewiesen. |
| `--failed` | Nein | Auftrag endgültig fehlgeschlagen: `Outcome=failed`, `Terminal=true`. Ohne Flag: bewusst abgebrochen (`aborted`). |
| `--human-interventions <Zahl>` | Nein | Bekannte fachliche Korrekturen oder zusätzliche Aufgabenanweisungen; gleiche Zählregel wie bei `finish`. Zugriffsgenehmigungen und technische Wiederaufnahmen zählen nicht. Ganze Zahl >= 0. Ohne Angabe letzter erfasster Wert, sonst unbekannt. |
| `--correction-rounds <Zahl>` | Nein | Bekannte Korrekturrunden, ganze Zahl >= 0. Ohne Angabe letzter erfasster Wert, sonst unbekannt. |
| `-h`, `--help` | Nein | Nur die Hilfe für `abort` anzeigen. |

Abort schreibt `result.json`, `report.md` und bei Benchmark eine CSV-Zeile.
Es setzt keine Änderungen zurück, löscht keine Run-Artefakte, verschiebt keine
Findings und wechselt keinen Branch. Es führt weder Git-Befehle noch Tests,
Usage-Exporte oder Modellanfragen aus. Ein erfolgreich abgeschlossener Run kann
nicht abgebrochen werden. Ein wiederholter erfolgreicher Abort ist idempotent.
Exit-Code 0 bedeutet hier: Abbruch erfolgreich archiviert, nicht Auftrag erfolgreich.

```powershell
python .opencode/scripts/start.py abort --id ACTUAL_ID --failed --reason "Testfehler nicht behoben" --correction-rounds 2 --human-interventions 0
```

### Felder der Request-Datei

Modulauswahl, Branch, Modell und Aufgabe sind **JSON-Felder**, keine direkten
CLI-Optionen wie `--model` oder `--task`. Die Reihenfolge der JSON-Felder ist egal.

| Feld | Pflicht? | Bedeutung |
|---|---|---|
| `modules` | Nein | `"complete"` (Standard), eine Auswahl wie `"branch,prompt"` oder eine Liste wie `["branch", "usage"]`. Nur die vier bekannten Module; keine doppelten Einträge. Ausführungsreihenfolge wird vom Workflow bestimmt. |
| `task` | Immer | Vollständiger, nicht leerer Originalauftrag. |
| `task_mode` | Nein; `/start` setzt es | `"analysis"` für reine Analyse ohne Task-Änderungen/Commits, `"implementation"` für Änderungsaufträge. Direkte Requests ohne Feld verwenden `"implementation"`. Keine Erweiterung des Originalauftrags. |
| `branch` | Bei `branch` | Name ohne `feature/`, z. B. `userservice_tests`; erlaubt sind kleine Buchstaben, Ziffern, `_` und `-`. |
| `target_class` | Bei `benchmark` | Einfacher Java-/JaCoCo-Klassenname, z. B. `UserService`; kein Dateipfad und kein Packagepräfix. |
| `model` | Bei `benchmark` | Schlüssel aus `benchmark-models.json`, z. B. `gpt61-sol` oder `qwen3-coder`; dient der Run-Zuordnung. Schaltet selbst kein aktives OpenCode-Modell um. |
| `session_id` | Bei `usage` im `/start`-Ablauf | Das Tool `workflow_usage_snapshot` liefert die echte ID automatisch. Bei einem expliziten Vorher-Export kann sie aus dessen `info.id` übernommen werden. |
| `prompt_provider` | Nein | Nur für das `prompt`-Modul: Override `chatgpt`, `openai` oder `ollama`; sonst Wert aus der Improver-Config. |
| `prompt_model` | Nein | Nur für das `prompt`-Modul: Modell-Override des Improvers; unabhängig vom Coding-Modell und dem Feld `model`. Ohne Override gilt die Improver-Config. |

Nicht verwendete optionale JSON-Felder weglassen oder auf `null` setzen,
ausgenommen `modules` und `task_mode`.
`modules` nur weglassen, wenn alle vier Module gewünscht sind; `null` ist hier
keine gültige Auswahl. `task_mode` weglassen oder explizit setzen; `null` ist
auch hier ungültig. Ein `-` als Platzhalter gehört nur zur Slash-Syntax;
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
  "task_mode": "implementation",
  "session_id": "ses_REPLACE_WITH_ACTUAL_SESSION"
}
```

```powershell
# Das OpenCode-Tool liefert zuerst den tatsächlichen Preflight-Pfad:
.\.venv\Scripts\python.exe .opencode/scripts/start.py prepare --request C:/Temp/start-request.json --usage-export ACTUAL_PREFLIGHT_PATH
# Nach der Baseline workflow_usage_snapshot erneut aufrufen:
.\.venv\Scripts\python.exe .opencode/scripts/start.py begin --id ACTUAL_ID --usage-export ACTUAL_FRESH_BEGIN_PATH
# OpenCode führt jetzt den zurückgegebenen task aus, prüft und committet.
# Umsetzung als UTF-8 Markdown außerhalb Git nach C:/Temp/start-summary.md schreiben.
# Offene Probleme oder ausdrücklich keine weiteren Findings in findings.md erfassen.
# Vor finish nochmals einen frischen Snapshot derselben Session erzeugen:
.\.venv\Scripts\python.exe .opencode/scripts/start.py finish --id ACTUAL_ID --summary-file C:/Temp/start-summary.md --usage-export ACTUAL_FRESH_FINISH_PATH --human-interventions 0 --correction-rounds 0
```

Optional `prompt_provider` / `prompt_model` im Request: Overrides nur für den
Improver, unabhängig vom Coding-Modell. Die unveränderte Default-Config nutzt
ChatGPT und setzt vorhandene Credentials voraus. Die vorhandenen manuellen
Login-/Provider-Smoke-Skripte stehen unter `scripts/prompt/tests/`; sie sind
keine Voraussetzung für die Offline-Test-Suite.

Globale Optionen stehen **vor** `prepare`/`begin`/`finish`/`status`/`abort`:

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

Abort beendet den aktiven Workflow-Status erst nach erfolgreicher Archivierung.
Es bewahrt Run-Artefakte und ändert weder Branches noch Task-Dateien. Alle neuen Benchmark-Läufe verwenden
dasselbe CSV-Schema, unabhängig von der gewählten Modulkombination. Nicht
ausgewählte Usage-Felder bleiben leer. Die Erweiterung des bisherigen Python-
Schemas erhält vorhandene Zeilen und benennt `InferenceSeconds` in
`MessageElapsedSeconds` um. Neue Metadaten alter Runs bleiben leer, weil sie
nicht nachträglich belegt werden können. Historische CSV-Zeilen aus den bisherigen
Schemas erhalten `Outcome=completed` und `Terminal=true`: Diese Versionen haben
ausschließlich erfolgreich geprüfte Abschlüsse in die CSV geschrieben. Ihre
Fehleranzahl bleibt unbekannt. Historische Run-Dateien bleiben
unverändert. Andere abweichende Schemas werden abgewiesen.

### Fehlgeschlagene Läufe und Abbrüche

Sobald ein echter Run angelegt wurde, archiviert Python Fehler in Vorbereitung,
Begin und Abschluss: `failures.json` enthält Zeitpunkt, Phase, Exception-Typ
und Fehlermeldung. `result.json`, Report und Benchmark-CSV enthalten denselben
Run mit `Outcome=failed`, `Terminal=false`; die gespeicherte Lifecycle-Phase
bleibt beispielsweise `prepare-failed`, `begin-failed`, `prepared` bei einem
abgewiesenen Begin-Snapshot oder `finishing` für einen wiederholbaren Abschluss.
Fehlerdetails stehen im Archiv, nicht als mehrzeiliger Exception-Text in der CSV.
Reine Vorbedingungen wie eine ungültige Request-Datei, ein schmutziger Git-Stand
vor dem Start oder eine ungültige Summary vor Finish erzeugen keinen neuen
Benchmark-Versuch. Nach einem Prozessabbruch muss der erhaltene Lauf geprüft
und ausdrücklich mit `abort` archiviert werden; es gibt keinen Hintergrundmonitor.

| Feld | Bedeutung |
|---|---|
| Outcome | `completed`: erfolgreich abgeschlossen; `failed`: fehlgeschlagen; `aborted`: bewusst beendet |
| Terminal | `false`: vorläufiger Fehlereintrag, Lauf bleibt erhalten; `true`: finaler Abschluss oder archivierter Stop |
| FailureCount | Anzahl aufgezeichneter Workflow-Fehler; getrennt von fachlichen Korrekturrunden. Historische Fehlerzahl unbekannt |
| LastFailurePhase / LastFailureType | Letzter aufgezeichneter Workflow-Fehler, auch nach erfolgreichem Retry; Details in `failures.json` |
| StopReason | Übergebener Abbruch-/Fehlschlagsgrund; ohne Angabe unbekannt |

`Terminal` ist in JSON ein Boolean (`true`/`false`); in CSV steht `True`/`False`.

Die CSV hat weiterhin **eine Zeile pro Run-ID**. Ein erfolgreicher Retry ersetzt
den vorläufigen Fehlereintrag und erhält den Fehlerverlauf. Ein abschließender
Abort ersetzt ihn durch den finalen Ausgang. Andere Läufe werden nicht geändert.
Für Erfolgsquoten ausschließlich `Terminal=true` betrachten; für Zeit-/Coverage-
Vergleiche zusätzlich den Auftrag, Ausgangsstand und Messstatus berücksichtigen.

Teilresultate behaupten keinen validierten Endcommit, finalen Diff oder
Nachher-Metriken: Diese Felder bleiben unbekannt, auch wenn von einem früheren
Abschlussversuch bereits Dateien existieren. Vorhandene Dateien bleiben erhalten
und werden im Report als möglicherweise unvollständig oder veraltet erklärt.
Ohne tatsächlichen Begin ist die Run-Dauer unbekannt. Ab Begin zählt sie bis
zum Messabschluss bzw. archivierten Stop, inklusive Pausen. Eine bereits validierbare
Usage-Teilmessung wird mit ihren ursprünglichen Zeitgrenzen erhalten; ein Abort
verlängert sie nicht durch erfundene Tokens. Ohne finalen Snapshot bleibt Usage
unbekannt. Nach einem Usage-Fehler kann Finish einen neuen expliziten Snapshot
verarbeiten; nach einem Build-/Artefaktfehler bleibt die vorhandene Messgrenze
beim Retry ohne neue Task-Commits erhalten.

Ist beispielsweise die CSV gesperrt, bleibt der aktive Lauf erhalten. Der
ursprüngliche Fehler wird nicht durch den Archivierungsfehler verdeckt; die CLI
meldet die unvollständige Archivierung zusätzlich. Zustand und `status` zeigen
den Fehler an. Nach Behebung der Ursache Finish bzw. Abort mit derselben ID
wiederholen. Die bei Abort gespeicherte Entscheidung, Zeit und Zähler bleiben
bei diesem Retry gleich. `stopping` bedeutet: Archivierung des Stops offen.

## Ergebnisse

```text
C:/Dev/AI-Benchmarks/<Projekt>/
  project.json
  active.json                  # nur während eines aktiven Laufs
  state/<id>.json               # Status/Audit, bleibt nach Abschluss erhalten
  results.csv                  # nur bei ausgewähltem benchmark
  runs/YYYY-MM-DD_HH-MM-SS_UTC__<id>/
    report.md                  # durchgeführte Arbeit, Verhaltensänderungen und Messwerte
    result.json
    diff.patch                 # vollständiger Git-Diff inkl. Binärdateien
    findings.md                # offene Probleme / ausdrücklicher Erfassungsstatus
    original-prompt.md
    improved-prompt.md          # bei prompt
    prompt-metadata.json        # Improver-Provider/Modell/Dauer, Status und eigene Usage
    prompt-usage.json           # separate Provider-Messung, auch bei Improver-Fehlern
    benchmark-before.json       # bei benchmark: Build-/Surefire-/JaCoCo-Belege
    benchmark-after.json
    usage-preflight.json        # bei usage: nur Vorprüfung, kein Messbeginn
    usage-before.json          # bei usage, nur Metriken/IDs
    usage-after.json
    usage.json
    failures.json              # nur bei aufgezeichneten Workflow-Fehlern
```

Neue Run-Ordner bekommen einen festen UTC-Zeitstempel vor ihrer ID, beispielsweise
`2026-10-07_14-30-12_UTC__d7b68bdd34654af0869feffb50e074a0`.
Im Explorer nach **Name** sortieren: aufsteigend für älteste zuerst, absteigend
für neueste zuerst. Spätere Dateiänderungen beeinflussen diese Reihenfolge nicht.
Läufe innerhalb derselben Sekunde haben dank ihrer unterschiedlichen IDs getrennte
Ordner; ihr Zeitpräfix ist dann gleich.

Der Zeitstempel entsteht einmal beim Anlegen des Runs, noch vor der Baseline.
Er wird als `createdAt` im Zustand gespeichert und bleibt bei Begin, Finish,
Retry und Abort unverändert. Er ist nicht der Startzeitpunkt des Benchmark-
Messfensters. UTC vermeidet mehrdeutige Namen beim Sommer-/Winterzeitwechsel;
die lokale Uhrzeit in Deutschland ist je nach Jahreszeit eine oder zwei Stunden später.

Die CLI-ID bleibt die reine 32-stellige Run-ID, und `state/<id>.json` bleibt
gleich aufgebaut. Artefaktpfade aus dem zurückgegebenen `folder` bzw. `RunFolder`
verwenden; nicht selbst aus der ID zusammensetzen. Bestehende Ordner `runs/<id>/`
werden nicht umbenannt: historische Verweise, Report-/CSV-Pfade und noch aktive
Läufe des bisherigen Stands funktionieren weiterhin.

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
Die fachlichen Improver-Regeln, Config und das Pydantic-Prompt-Schema bleiben
unverändert. Provider und Session-Speicherung sind für die unten beschriebene
Fehlerbehandlung und eigene Messung ergänzt.

`benchmark-before.json` und `benchmark-after.json` enthalten die originalen
JaCoCo-CSV-Felder der Zielklasse, die Zähler/Zeiten jeder Surefire-Suite und
die erfolgreichen Maven-Aufrufe mit Exitcodes sowie den geprüften Java-Major.
Damit lassen sich Testanzahl, Testzeit und Coverage aus der ZIP nachrechnen.
Bei null gezählten Branches bedeutet die rechnerische 100-%-Anzeige weiterhin
keinen Nachweis getesteter Verzweigungen. XML-Properties, stdout/stderr und
Systemumgebungen werden nicht archiviert. Die Dateien sind Messbelege,
kein kryptografischer Nachweis oder vollständiges Build-Log.

### Bericht und Findings

`report.md` enthält die Auftragsart und unter **Durchgeführte Arbeit und Verhaltensänderungen**, was erledigt
wurde, welche Verträge oder Verhaltensweisen sich geändert haben, wichtige
Entscheidungen sowie Prüfungen und deren Grenzen. Die Behebung mitgegebener
Findings wird hier erklärt. Danach folgen die automatisch erzeugten Messwerte,
Tests, Coverage, Usage und Artefaktpfade.
Bei einer reinen Analyse werden geprüfter Umfang, Methoden und Grenzen genannt,
mit der ausdrücklichen Angabe, dass keine Codeänderungen vorgenommen wurden.

`findings.md` enthält ausschließlich **neu entdeckte oder weiterhin offene
Probleme**, mit Fundstelle, Beleg, Auswirkung und begründeter Schwere. Erledigte
Arbeiten und bereits behobene Findings gehören in den Report. Wurden während
der Aufgabe keine weiteren offenen Probleme festgestellt, steht dort ausdrücklich:
**„Keine weiteren offenen Findings festgestellt.“** Das ist keine Zusicherung,
dass der gesamte Code fehlerfrei ist; es wird kein zusätzliches Review-Modul ausgeführt.

OpenCode schreibt den Arbeitsbericht zunächst in eine temporäre UTF-8
Markdown-Datei **außerhalb Git** und übergibt deren tatsächlichen Pfad mit
`finish --summary-file`. Nur den Inhalt für den Reportabschnitt schreiben, ohne
zusätzliche Dokumentüberschrift oder wiederholte Benchmark-Tabellen. Python
speichert den Text im vorhandenen Run-Zustand und erzeugt daraus `report.md`.
Die Übergabedatei wird weder als weiteres Run-Artefakt archiviert noch vom
Workflow gelöscht. Die Angaben entstehen vor dem finalen Usage-Snapshot.

Fehlt bei einem direkten CLI-Aufruf der Arbeitsbericht, kennzeichnet der
Report dies ausdrücklich. Fehlt `findings.md`, archiviert Python einen Hinweis
auf fehlende Angaben und unbekannten Findings-Status; es erfindet keine Entwarnung.
Vorhandene Findings werden unverändert archiviert und erst nach erfolgreichem
Abschluss aus dem Projektroot entfernt.

Bei einem fehlgeschlagenen Finish bleibt der übergebene Bericht für einen Retry
erhalten, auch wenn die temporäre Datei inzwischen fehlt. Nach weiteren
Task-Commits muss ein aktualisierter Bericht übergeben werden; ohne ihn wird
die überholte Erklärung verworfen und als fehlend gekennzeichnet. Ein bereits
abgeschlossener Run bleibt bei erneutem Finish unverändert. Historische Runs
werden nicht umgeschrieben; das CSV-Schema bleibt unverändert.

## Usage und Messgrenzen

Im `/start`-Ablauf ist die Quelle das Plugin-Tool `workflow_usage_snapshot`.
Es bekommt die echte Session-ID aus OpenCodes Tool-Kontext und liest die
Sitzung über den SDK-Client des aktiven Servers. V1 verwendet den bereitgestellten
Client. In **2.0.19** fehlt der vollständige Export im Plugin-Kontext; deshalb
verwendet der Adapter den öffentlichen V2-Client und die Dienstregistrierung.
Er prüft Serverversion und Prozess-ID gegen den Prozess, in dem das Plugin läuft.
Ein fremder Server wird vor dem Export abgewiesen.
Für Pair darf der Dienst auf `0.0.0.0` oder `::` lauschen. Der Usage-Adapter
übersetzt diese Bind-Adressen ausschließlich für seinen eigenen Zugriff nach
`127.0.0.1` beziehungsweise `::1`, behält Port und Authentifizierung bei und
prüft weiterhin dieselbe Prozess-ID und Serverversion. Die Netzwerkfreigabe
des Dienstes bleibt unverändert. Beliebige LAN-/Remote-Adressen werden nicht
als Usage-Endpunkt akzeptiert. Nach Adapteränderungen den Dienst neu starten
und dieselbe bestehende Sitzung fortsetzen; keinen neuen Run beginnen. Der vollständige Export
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
Session-Export nicht enthalten; seine tatsächlich gemeldeten Werte stehen
separat in `prompt-usage.json` und den `Prompt*`-Spalten.
Hilfsanfragen ohne exportierte Message, etwa Titelgenerierung, werden nicht
gezählt. Liefert ein beim Begin laufender Request seine Zähler erst später,
geht dessen gemeldeter Gesamtwert in das Delta ein; Tokens werden nicht anhand
der Laufzeit aufgeteilt.
### Prompt-Improver: eigene Usage und Fehlerbehandlung

Das `prompt`-Modul und die eigenständige Improver-CLI verwenden denselben
Ausführungsweg. Mit einem Ausgabeordner entstehen `prompt-metadata.json`
und `prompt-usage.json`, auch bei Provider-, Auth- oder Strukturfehlern.
Die Messdateien enthalten keine Credentials und keinen Antwort-/Reasoning-Text.
Original- und Improved-Prompt bleiben die bewusst gespeicherten Prompt-Artefakte.

| Bericht / CSV | Bedeutung |
|---|---|
| PromptProvider / PromptModel | An die Factory übergebene Einstellungen, unabhängig vom Coding-Modell |
| PromptDurationSeconds | Gesamtdauer des Improvers einschließlich Auth, Anfrage und Prompt-Artefakten; keine zusätzliche Run-Dauer |
| PromptInputTokens / PromptOutputTokens / PromptTotalTokens | Provider-Meldung; OpenAI-Input enthält Cache-Reads, Output enthält Reasoning |
| PromptReasoningTokens / PromptCacheReadTokens / PromptCacheWriteTokens | Nur explizit gemeldete Teilmengen, nicht nochmals zu Input/Output addieren |
| PromptRequests / PromptCompletedRequests / PromptRetries | Inference-Versuche / erfolgreich abgeschlossene Provider-Antworten / automatische Inference-Retries |
| PromptProviderDurationSeconds | Wenn vorhanden: Provider-Gesamtdauer, bei Ollama Nanosekunden in Sekunden umgerechnet |
| PromptEstimatedCostUSD / PromptCostStatus | Bei diesen Providern unbekannt / unavailable; keine Abonnement- oder Tokenpreis-Schätzung erfunden |
| PromptUsageStatus | reported bei vorhandener Provider-Usage, sonst unavailable |
| PromptStatus / PromptErrorCategory | completed oder failed für den Improver; bei Fehlern eine bereinigte Kategorie |

ChatGPT bewahrt Text-Ereignisse bis zum Abschluss auf. Falls die terminale
Response keinen Text enthält, wird der tatsächlich gestreamte Text verwendet;
finalisierte Textteile werden nicht nochmals zu ihren Deltas addiert. Erst ein
erfolgreicher Abschluss ohne Refusal und eine gültige Struktur erlauben die Weiterarbeit.

OpenAI/ChatGPT lesen Usage aus der endgültigen Responses-Antwort; Stream-Deltas
werden nicht zusätzlich gezählt. Ollama liefert Prompt-/Output-Zähler und
gegebenenfalls Serverdauer. Reasoning-, Cache- und Kostenangaben bleiben dort
unbekannt, wenn keine entsprechenden Zähler vorhanden sind. JSON `null`, leere
CSV-Felder und „nicht verfügbar“ bedeuten unbekannt; echte gemeldete Nullen
bleiben Nullen. Alte CSV-Zeilen bekommen leere neue Spalten, historische
Run-Artefakte bleiben erhalten. `prompt` muss dafür ausgewählt sein; die
Erfassung benötigt nicht zusätzlich das OpenCode-`usage`-Modul.

Jeder Provider hat einen HTTP-Timeout von 120 Sekunden und führt keine
automatischen Inference-Retries aus. Bei fehlendem Login zählen 0 Inference-
Versuche; Auth-/Refresh-Anfragen sind in `PromptRequests` nicht enthalten.
Ein vollständig empfangener Provider-Response kann trotzdem an der
Pydantic-Strukturprüfung scheitern: `PromptCompletedRequests=1`, aber
`PromptStatus=failed`. Die bis dahin gemeldeten Tokens bleiben erhalten.
Unvollständige Antworten, Refusals, leere Ausgaben und abgerissene Streams
werden abgewiesen; es gibt keinen stillen Rückfall auf den Original-Prompt
oder einen anderen Provider. HTTP-Fehler werden kategorisiert, ohne rohe
Provider-Meldungen in Report/CSV zu übernehmen.

ChatGPT-Refreshs sind über einen gemeinsamen OS-Lock des Benutzerprofils
serialisiert, auch zwischen Projekten; Wartezeit maximal 60 Sekunden.
Nach dem Lock wird der aktuelle Credential-Stand erneut gelesen. Die rotierenden
Tokens samt Ablaufzeit werden als versionierter Snapshot im Keyring gespeichert.
Erst nach vollständigem Schreiben wird der Snapshot veröffentlicht; anschließend
werden alte bekannte Chunks aufgeräumt. Vorhandene Legacy-Credentials bleiben
lesbar und werden beim nächsten Speichern übernommen. Tokens stehen weiterhin
ausschließlich im Keyring, nicht in `profile.json` oder Benchmark-Artefakten.
Ein Prozessabbruch nach serverseitiger Rotation vor dem Speichern kann trotzdem
eine erneute Anmeldung erforderlich machen.

Endgültig ungültige Tokens verlangen eine erneute Anmeldung. Gespeicherte
Invalidierung verhindert wiederholte Refreshs derselben ungültigen Sitzung;
Netzwerkfehler invalidieren Credentials nicht. Ein laufender Benchmark öffnet
keinen Browser. Für die bewusste Anmeldung vom `.opencode/scripts`-Ordner:

```powershell
python -B -m prompt.tests.test_chatgpt_login
```

Dieses bestehende Login-Skript versucht den synchronisierten Refresh und öffnet
bei erforderlicher Anmeldung den vorhandenen PKCE-Login. Temporäre Fehler
starten keinen neuen Browser-Login. Nach fehlgeschlagenem `begin`: gespeicherten
Run mit `status` prüfen und mit `abort --id <id> --failed --reason <Grund>`
archivieren, dann nach Behebung einen neuen Auftrag starten.

Referenzen: [Responses und Usage](https://developers.openai.com/api/reference/python/resources/responses/methods/create),
[ChatGPT-Sessions und Refresh](https://developers.openai.com/siwc/token-sharing-open-source/profiles-and-sessions),
[Auth-Fehler und Wiederanmeldung](https://developers.openai.com/siwc/token-sharing-open-source/errors-and-recovery),
[Ollama-Chat-Messwerte](https://docs.ollama.com/api/chat).

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
Metriken, Trennung von Report und offenen Findings, fehlende Angaben,
Berichtserhalt bei Retry, reine Analyse ohne Codeänderungen/Commits,
lesende Statusabfragen einschließlich paralleler Zustandswechsel,
Fehler-/Abbruchzeilen, Teilmessungen ohne erfundene Werte, gesperrte CSV,
idempotenten Stop und erfolgreiche Retries ohne doppelte Run-IDs,
Patch-/Findings-/CSV-Erhalt, Locking, Modellgenerator und Integration
des existierenden Prompt-Improvers, Provider-Fehler/Streaming/Usage,
atomare Keyring-Snapshots mit Fake-Keyring und einen echten Refresh-Wettlauf
zweier Python-Prozesse. Sie verbraucht keine Modellanfragen und
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
- Prompt-Kern, Config und Prompt-Schema unverändert; Provider messen ihre
  eigene Usage und behandeln Fehler/Incomplete/Refusal ausdrücklich. Ein
  eigener globaler ChatGPT-Session-Lock schützt Refreshs und Credential-Speicherung.
  Das bestehende Login-Skript unterstützt die bewusste erneute Anmeldung.
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
