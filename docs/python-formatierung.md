# Python-Formatierung mit Ruff

Ruff ist der verbindliche Formatter für Python-Änderungen. Die zentrale
Konfiguration liegt in [`ruff.toml`](../ruff.toml), die Entwicklungsabhängigkeit
in [`requirements-dev.txt`](../requirements-dev.txt). Beide schreiben Version
**0.17.1** fest; bei einem späteren Update beide Einträge gemeinsam ändern.

Der Stil verwendet 4 Leerzeichen, 88 Zeichen Ziel-Zeilenlänge, doppelte
Anführungszeichen und vorhandene Zeilenenden. Vorschaufunktionen sind deaktiviert.
Die Syntax-Zielversion bleibt Python 3.10 entsprechend der bestehenden
[Workflow-Anleitung](../.opencode/README.md); geprüft wurde mit Python 3.14.8.

## Einrichtung

Vom Projektroot aus, bei fehlender virtueller Umgebung zunächst
`python -m venv .venv` ausführen:

```powershell
.\.venv\Scripts\python.exe -m pip install -r requirements-dev.txt
.\.venv\Scripts\python.exe -m ruff --version
```

Ruff ist ein Entwicklungswerkzeug. Die Laufzeitabhängigkeiten der Workflow-Skripte
bleiben separat in `.opencode/requirements.txt`; Java/Maven benötigen Ruff nicht.

## Verbindlicher Ablauf für Coding-Agenten

Nach jeder eigenen Python-Änderung, vor Tests und vor dem Abschluss, beide Befehle
mit den tatsächlich bearbeiteten Dateien ausführen. Beispiel:

```powershell
.\.venv\Scripts\python.exe -m ruff format .opencode/scripts/start.py
.\.venv\Scripts\python.exe -m ruff format --check .opencode/scripts/start.py
```

Mehrere Pfade dürfen gemeinsam angegeben werden; Pfade mit Leerzeichen zitieren.
Nach weiteren Python-Korrekturen beide Schritte wiederholen. Die dauerhafte Regel
steht in [AGENTS.md](../AGENTS.md).

Ruff berücksichtigt hier keine Git-Änderungsauswahl. Deshalb immer explizit die
selbst bearbeiteten Dateien angeben, besonders bei vorhandenen fremden Änderungen.
Die ausgewählten Dateien werden vollständig formatiert. Ein persönlicher
IDE-Speicher-Hook oder Hintergrund-Dateiwächter wird nicht installiert.

## Gesamter Bestand und Grenzen

Nur mit ausdrücklichem Auftrag zur Gesamtformatierung:

```powershell
.\.venv\Scripts\python.exe -m ruff format .
.\.venv\Scripts\python.exe -m ruff format --check .
```

Die Konfiguration erfasst `.py` und `.pyi`, einschließlich der Skripte und Tests im
versteckten `.opencode`-Ordner. Abhängigkeiten, virtuelle Umgebungen, generierte
Dateien, Buildausgaben, Daten, Logs und Run-Ordner werden ausgeschlossen;
`.gitignore` wird berücksichtigt. Ausschlüsse gelten auch für explizite Dateipfade.
Java, CSS, Properties, Markdown und Notebooks werden nicht formatiert.

`ruff format --check` prüft nur die Formatierung. `ruff check` ist der separate
Linter und gehört nicht zum hier eingeführten Pflichtschritt. Automatische
Lint-Korrekturen, Import-Sortierung und fachliche Refactorings sind nicht Teil
dieser Formatierung. Ein Formatter ersetzt weder Tests noch Codeprüfung.

## Erstformatierung

Am 16.10.2026 wurden alle 50 versionierten Python-Dateien geprüft: 40 wurden
formatiert, 10 waren bereits passend. Die Python-Syntaxbäume waren vor und nach
der Formatierung identisch; ein erneuter Lauf änderte keine Datei mehr.
Die vorhandene Python-Offline-Suite (`unittest discover -s tests -v` aus
`.opencode/scripts`) lief mit 205 Tests ohne Fehler, Fehlschläge oder übersprungene
Fälle durch. Live-Provider-, Java- und Browsertests wurden nicht gestartet.

Grundlagen: [Ruff-Formatter](https://docs.astral.sh/ruff/formatter/),
[Ruff-Konfiguration](https://docs.astral.sh/ruff/configuration/).
