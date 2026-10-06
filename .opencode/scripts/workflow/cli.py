import argparse
import json
import os
import sys
from pathlib import Path

from . import runner
from .common import read_json


def main(argv=None) -> int:
    # Redirected Windows streams otherwise use a locale encoding, breaking JSON umlauts.
    for stream in (sys.stdout, sys.stderr):
        if hasattr(stream, "reconfigure"):
            stream.reconfigure(encoding="utf-8")
    parser = argparse.ArgumentParser(
        description="Modularer OpenCode /start-Workflow",
        epilog="Globale Optionen vor prepare/begin/finish/status/abort, Aktionsoptionen danach. Details: .opencode/README.md.")
    parser.add_argument("--repo", type=Path, default=Path(__file__).resolve().parents[3],
                        help="Projektroot mit Git und Maven Wrapper; Standard: Elternordner von .opencode")
    parser.add_argument("--store-root", type=Path,
                        default=Path(os.environ.get("OPENCODE_BENCHMARK_ROOT", "C:/Dev/AI-Benchmarks" if os.name == "nt"
                                                   else str(Path.home() / "AI-Benchmarks"))),
                        help="Externe Speicherwurzel; Standard: OPENCODE_BENCHMARK_ROOT oder C:/Dev/AI-Benchmarks (Windows), ~/AI-Benchmarks (sonst)")
    commands = parser.add_subparsers(dest="action", required=True)
    prepare = commands.add_parser("prepare", help="Voraussetzungen und Baseline vor dem Messfenster prüfen")
    prepare.add_argument("--request", type=Path, required=True, help="UTF-8 JSON mit Modulen und Auftrag")
    prepare.add_argument("--usage-export", type=Path, help="Usage-Preflight; ersetzt nicht den frischen Snapshot nach prepare")
    begin = commands.add_parser("begin", help="Messfenster starten; Run-ID und effektiven Auftrag ausgeben",
                                description="Begin startet den vorbereiteten Lauf. Ohne benchmark+usage ist ein direkter Start möglich. OpenCode führt den Auftrag danach aus.")
    source = begin.add_mutually_exclusive_group(required=True)
    source.add_argument("--request", type=Path, help="Direkter Start; benchmark+usage erfordert zuerst prepare")
    source.add_argument("--id", help="Vorbereiteter Run aus prepare; keine OpenCode-Session-ID")
    begin.add_argument("--usage-export", type=Path, help="VORHER-Session-Export als JSON; /start verwendet den Pfad aus workflow_usage_snapshot, ohne diesen Parameter lokaler CLI-Export")
    finish = commands.add_parser("finish", help="Finale Metriken erfassen und Run-Artefakte schreiben",
                                 description="Finish schließt einen erfolgreich durchgeführten und geprüften Analyse- oder Implementierungsauftrag ab.")
    finish.add_argument("--id", required=True, help="Pflicht: echte 32-stellige Run-ID aus begin, keine OpenCode-Session-ID")
    finish.add_argument("--human-interventions", type=int, default=0,
                       help="Anzahl neuer Benutzereingriffe seit begin; ganze Zahl >= 0, Standard: 0")
    finish.add_argument("--correction-rounds", type=int, default=0,
                       help="Fehlgeschlagene Prüfläufe, die eine weitere Codeänderung erforderten; ganze Zahl >= 0, Standard: 0")
    finish.add_argument("--usage-export", type=Path, help="Frischer NACHHER-Export derselben Session; /start verwendet workflow_usage_snapshot, Begin-Export nicht wiederverwenden")
    finish.add_argument("--summary-file", type=Path,
                        help="UTF-8 Markdown außerhalb Git: durchgeführte Arbeit und Verhaltensänderungen für report.md; findings.md enthält nur offene Probleme")
    status = commands.add_parser("status", help="Gespeicherten Run-Status lesen; keine Tests, Modellanfragen oder Änderungen")
    status.add_argument("--id", help="Optional: bestimmten Run lesen; ohne ID den aktiven Run, sonst idle")
    abort = commands.add_parser("abort", help="Aktiven Run freigeben; Artefakte und Git-Zustand behalten",
                                description="Abort beendet den Workflow-Status, ohne Task-Dateien oder Branches zurückzusetzen.")
    abort.add_argument("--id", required=True, help="Pflicht: echte 32-stellige Run-ID des abzubrechenden Laufs")
    args = parser.parse_args(argv)
    try:
        repo = args.repo.resolve()
        if args.action in ("prepare", "begin"):
            catalog = Path(__file__).resolve().parents[2] / "benchmark-models.json"
            if args.action == "prepare":
                result = runner.prepare(repo, args.store_root, read_json(args.request), catalog, args.usage_export)
            else:
                request = read_json(args.request) if args.request else None
                result = runner.begin(repo, args.store_root, request, catalog, args.usage_export, args.id)
        elif args.action == "finish":
            result = runner.finish(repo, args.store_root, args.id, args.human_interventions,
                                   args.correction_rounds, args.usage_export, args.summary_file)
        elif args.action == "status":
            result = runner.status(repo, args.store_root, args.id)
        else:
            result = runner.abort(repo, args.store_root, args.id)
        print(json.dumps(result, ensure_ascii=False, indent=2))
        return 0
    except Exception as error:
        print(f"Workflow failed: {error}", file=sys.stderr)
        return 1
