# Lokaler OpenCode-Workflow

Dieses optionale Entwicklungswerkzeug bereitet Aufgaben vor und dokumentiert
deren Ergebnisse. Die eigentliche Codearbeit übernimmt das ausgewählte OpenCode-
Modell nach [AGENTS.md](../AGENTS.md). Die Anwendung selbst benötigt diesen
Workflow nicht. Projektübersicht: [README](../README.md).

## Installation und Einstieg

Python ab 3.10, Git und für Benchmarks Java 25/Maven Wrapper verwenden. Pakete nur
in der eigenen virtuellen Umgebung installieren:

```powershell
.\.venv\Scripts\python.exe -m pip install -r .opencode/requirements.txt
npm --prefix .opencode ci
.\.venv\Scripts\python.exe .opencode/scripts/start.py --help
```

Ohne virtuelle Umgebung vorher `python -m venv .venv` ausführen. Python verwaltet
Vorbereitung, Benchmark und Ergebnisablage. Das lokale Plugin
`plugins/workflow-usage.js` mit `scripts/workflow_usage_bridge.mjs` stellt
`workflow_usage_snapshot` bereit. Nach Pluginänderungen den eigenen OpenCode-
Server neu starten; ein laufender Chat lädt sie nicht automatisch nach.
Die vorhandenen CLI-/Desktop-Anbindungen sind in package.json festgelegt.
Kein öffentlicher Share-Link ist für Usage erforderlich.

## Module und Slash-Commands

| Modul | Aufgabe |
| --- | --- |
| `branch` | Isolierten Feature-Branch vorbereiten |
| `prompt` | Originalauftrag durch den konfigurierten Improver präzisieren |
| `benchmark` | Frische Vorher-/Nachhermessung einer Java-Zielklasse |
| `usage` | Vollständige Exporte derselben Coding-Sitzung auswerten |

`complete` ist das Preset aller vier Module für `/start`. Der Improver liest nur
den Auftragstext, keine Projektdateien. Der ausführende Agent muss Referenzen und
Projektregeln selbst lesen. Originalauftrag und verbesserter Text gelten zusammen;
der Improver darf keine zusätzlichen Anforderungen oder Freigaben erfinden.

```text
/start complete userservice_tests UserService gpt61-sol Create complete tests for UserService.
/start branch,prompt username_validation - - Reject empty usernames.
/start branch,usage username_validation - - Reject empty usernames.
```

Format: `/start <modules> <branch|-> <targetClass|-> <modelKey|-> <task>`.
Der Task ist der vollständige Rest, einschließlich Zeilenumbrüchen. Module werden
mit Kommas ausgewählt; unbekannte/doppelte Werte scheitern. `-` bezeichnet ein
unbenutztes Feld, nicht eine leere Aufgabenbeschreibung.

Die Modellvarianten `/start-<modelKey>` verwenden den Katalog in
`benchmark-models.json`; bei ihnen entfällt das separate modelKey-Argument.
Bei `selected` muss das sichtbare Modell passen, `forced` verwendet die dortige
explizite Modell-ID. Generisches `/start` wechselt das Coding-Modell nicht.
Modellschlüssel, tatsächlich gemessenes Modell und Improver-Modell sind getrennt.

### Auf dem Feature-Branch fortsetzen

```text
/restart complete PasswordResetService gpt61-sol Behebe den beschriebenen Fehler.
```

Format: `/restart <modules> <targetClass|-> <modelKey|-> <task>`.
`complete` bedeutet hier prompt, benchmark und usage; branch ist nicht verfügbar.
Die Baseline stammt vom aktuellen sauberen Feature-HEAD. Vorher/Nachher und Patch
beziehen sich nur auf diesen neuen Run. Main, detached HEAD, fremder Branch,
uncommittete Änderungen oder ein offener Run verhindern einen neuen Start.
Ein bereits offener Run wird mit seinem tatsächlichen Einstieg und seiner ID
fortgesetzt. `/restart` legt keinen Branch an und merged oder pusht nichts.

### Lokaler Merge

`/merge` ist ein eigener, ausdrücklich aufzurufender Vorgang nach dem Review,
kein Modul und kein automatischer Abschluss von `/start`:

```text
/merge
/merge userservice_tests
```

Ohne Argument gilt nur der aktuelle feature/*-Branch; optional genau einen
Feature-Branchnamen angeben. Der Helper prüft abgeschlossenen Run, Projekt, Git-
Zustand, Bericht, Findings und Commit-Historie. Der Agent führt die erforderlichen
Tests aus; das Git-Skript baut nicht selbst. Ein erfolgreicher lokaler Merge
endet auf main. Die deutsche Nachricht beginnt mit `In main integriert:` und
beschreibt erledigte Arbeit. Offene Findings bleiben offen. Kein automatischer Push.

## Python-CLI

Die Einstiege `scripts/start.py` und `scripts/restart.py` verwenden dieselbe Engine.
Globale Optionen wie `--repo` und `--store-root` stehen vor der Aktion; dieselben
Werte durchgehend verwenden. Die genaue Optionsliste kommt aus `--help`.

| Aktion | Wirkung |
| --- | --- |
| `prepare --request DATEI` | Request/Umgebung prüfen und Baseline vorbereiten |
| `begin --id ID` | Tatsächlich vorbereiteten Run beginnen und effektiven Auftrag ausgeben |
| `finish --id ID --summary-file DATEI` | Ergebnisse, Patch, Findings und Messungen abschließen |
| `status [--id ID]` | Gespeicherte Momentaufnahme lesen; ohne ID nur aktiven Run, sonst idle |
| `abort --id ID` | Run kontrolliert beenden, Teilbelege und Fehler erhalten |

`begin --request DATEI` ist alternativ zu einer vorbereiteten ID möglich; bei
benchmark und usage gemeinsam zuerst prepare verwenden. IDs müssen aus dem
tatsächlichen Ergebnis übernommen werden, keine Commit-/Sitzungs-ID einsetzen.
Status startet weder Builds noch Modellanfragen und bestätigt nicht den lebenden
Prozess oder den aktuellen Git-Zustand. Ein fehlgeschlagener Schritt beendet den
Run nicht zwangsläufig: Ursache korrigieren und dieselbe ID gezielt fortsetzen
oder abbrechen. Keine Zustandsdateien zur vermeintlichen Reparatur bearbeiten.

### Request

UTF-8 JSON außerhalb Git ablegen; Auftragstext niemals als Shell-Code auswerten.

```json
{
  "modules": "complete",
  "branch": "userservice_tests",
  "target_class": "UserService",
  "model": "gpt61-sol",
  "task": "Create complete tests for UserService.",
  "task_mode": "implementation",
  "session_id": "ACTUAL_SESSION_ID"
}
```

`task_mode` ist analysis oder implementation. Analyse verändert und committet
keinen Task-Code. Nicht verwendete optionale Felder weglassen oder null setzen;
modules und task_mode dürfen nicht null sein. Branchname ohne feature/ aus kleinen
Buchstaben, Ziffern, Unterstrich oder Bindestrich. target_class bezeichnet den
einfachen Java-/JaCoCo-Namen. Modelkey kommt aus dem Katalog. Für usage die echte
Sitzungs-ID und vollständige frische Exporte verwenden. `prompt_provider` und
`prompt_model` überschreiben nur den Improver, nicht das Coding-Modell.

## Ergebnisse und Messgrenzen

Der externe Run-Speicher hält Zustandsdaten, Vorher-/Nachhermetriken, vollständige
Usagebelege, Patch, Bericht und Findings. Das konkrete Verzeichnis wird vom Run
ausgegeben. Keine zweite Run-Historie in den Anwendungs-Docs pflegen.

Die Zusammenfassung für finish liegt als UTF-8 Markdown außerhalb Git und
beschreibt tatsächlich erledigte Arbeit, Verhaltensänderungen bzw. Analyseumfang
und Grenzen. Findings müssen konkret erfasst oder ausdrücklich als keine weiteren
Findings bezeichnet werden; eine fehlende Datei wird nicht als mängelfrei gewertet.
Fehler und Teilmessungen bleiben sichtbar. Retry darf keine doppelten CSV-Zeilen
oder erfundenen Messwerte erzeugen. Gesperrte Ergebnisse zuerst freigeben, keine
Runbelege zur Umgehung löschen.

`--human-interventions` zählt neue fachliche Korrekturen seit begin, keine reinen
Zugriffsgenehmigungen oder technischen Fortsetzungen. `--correction-rounds` zählt
fehlgeschlagene Prüfungen, die eine Codeänderung erforderten. Python erfasst diese
Zahlen nicht automatisch. Nur tatsächliche Werte übergeben.

Benchmark erstellt vor jeder Messung Shared frisch und wertet Surefire/JaCoCo der
Zielklasse aus. Baseline und Final müssen vergleichbar sein; JDK-/Buildfehler nicht
durch Versionswechsel oder Testskips verdecken. Usage benötigt dieselbe aktive
Sitzung/Projektzuordnung, frische Vorher-/Nachherexporte und das tatsächlich
gemessene Modell. Kontext, Cache und Provider-Messgrenzen begrenzen Vergleiche;
fehlende Angaben bleiben unbekannt. Das Coding-Modell nicht mitten in einem
forced-Lauf wechseln. Keine separaten Modellanfragen zur Berichterstellung.

## Provider und Sicherheit

Der Improver unterstützt die konfigurierten ChatGPT-, OpenAI- und Ollama-Provider.
Anmeldung, Schlüssel und lokale Profile werden ausschließlich durch deren
vorgesehene Login-/Konfigurationswege gepflegt. Keine Credentials, Cookies oder
Token in Request, Report, Log oder Git kopieren. Refresh und Credentialspeicherung
sind gegen konkurrierende Prozesse geschützt; transiente Fehler dürfen eine
vorhandene Anmeldung nicht löschen. Nicht eigenmächtig Profile/Keyring zurücksetzen.

Die Dateien unter `scripts/prompt/tests` sind manuelle Login-/Provider-Smokes und
können echte Anfragen auslösen. Sie sind keine Voraussetzung für Offlineprüfungen.
Unter PowerShell zählt der tatsächliche Exitcode des gestarteten Prozesses;
stderr-Ausgabe allein ist kein zuverlässiger Erfolgs-/Fehlernachweis.

## Offline prüfen

Vom Ordner `.opencode/scripts`, mit der virtuellen Projektumgebung:

```powershell
..\..\.venv\Scripts\python.exe -B -m unittest discover -s tests -v
node --test tests/test_workflow_usage.mjs
```

Diese Tests verwenden temporäre Git-Repositories, Fake-Keyring, gemockte Provider/
Maven-Builds und kontrollierten lokalen HTTP-Transport. Sie starten keine echte
Coding-Aufgabe und senden keine Modellanfrage. Die passenden NPM-Abhängigkeiten
müssen installiert sein. Formatregeln für eigene Pythonänderungen stehen in
[AGENTS.md](../AGENTS.md).
