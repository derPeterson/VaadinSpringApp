import argparse
import json
import os
import sys
from pathlib import Path

from . import runner
from .common import read_json


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(
        description="Modularer OpenCode /start-Workflow",
        epilog="Globale Optionen vor begin/finish/abort, Aktionsoptionen danach. Details: .opencode/README.md.")
    parser.add_argument("--repo", type=Path, default=Path(__file__).resolve().parents[3],
                        help="Projektroot mit Git und Maven Wrapper; Standard: Elternordner von .opencode")
    parser.add_argument("--store-root", type=Path,
                        default=Path(os.environ.get("OPENCODE_BENCHMARK_ROOT", "C:/Dev/AI-Benchmarks" if os.name == "nt"
                                                   else str(Path.home() / "AI-Benchmarks"))),
                        help="Externe Speicherwurzel; Standard: OPENCODE_BENCHMARK_ROOT oder C:/Dev/AI-Benchmarks (Windows), ~/AI-Benchmarks (sonst)")
    commands = parser.add_subparsers(dest="action", required=True)
    begin = commands.add_parser("begin", help="Module vorbereiten; Run-ID und effektiven Auftrag ausgeben",
                                description="Begin bereitet den Lauf vor. OpenCode implementiert den Auftrag danach.")
    begin.add_argument("--request", type=Path, required=True, help="Pflicht: UTF-8 JSON mit Modulen und Auftrag; Task-Text bleibt Daten")
    begin.add_argument("--usage-export", type=Path, help="Optionaler VORHER-Session-Export als JSON; bei usage statt automatischem OpenCode-Export")
    finish = commands.add_parser("finish", help="Finale Metriken erfassen und Run-Artefakte schreiben",
                                 description="Finish schließt einen erfolgreich implementierten und geprüften Auftrag ab.")
    finish.add_argument("--id", required=True, help="Pflicht: echte 32-stellige Run-ID aus begin, keine OpenCode-Session-ID")
    finish.add_argument("--human-interventions", type=int, default=0,
                       help="Anzahl neuer Benutzereingriffe seit begin; ganze Zahl >= 0, Standard: 0")
    finish.add_argument("--correction-rounds", type=int, default=0,
                       help="Fehlgeschlagene Prüfläufe, die eine weitere Codeänderung erforderten; ganze Zahl >= 0, Standard: 0")
    finish.add_argument("--usage-export", type=Path, help="Optionaler NACHHER-Export derselben Session als JSON; bei usage statt automatischem Export")
    abort = commands.add_parser("abort", help="Aktiven Run freigeben; Artefakte und Git-Zustand behalten",
                                description="Abort beendet den Workflow-Status, ohne Task-Dateien oder Branches zurückzusetzen.")
    abort.add_argument("--id", required=True, help="Pflicht: echte 32-stellige Run-ID des abzubrechenden Laufs")
    args = parser.parse_args(argv)
    try:
        repo = args.repo.resolve()
        if args.action == "begin":
            catalog = Path(__file__).resolve().parents[2] / "benchmark-models.json"
            result = runner.begin(repo, args.store_root, read_json(args.request), catalog, args.usage_export)
        elif args.action == "finish":
            result = runner.finish(repo, args.store_root, args.id, args.human_interventions,
                                   args.correction_rounds, args.usage_export)
        else:
            result = runner.abort(repo, args.store_root, args.id)
        print(json.dumps(result, ensure_ascii=False, indent=2))
        return 0
    except Exception as error:
        print(f"Workflow failed: {error}", file=sys.stderr)
        return 1
