"""Standalone, local integration after an explicit /merge invocation.

OpenCode reads the evidence, writes the German message and verifies tests.
This helper validates Git/run identity and performs only the pinned merge.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import tempfile

from workflow.common import WorkflowError, outside_repo, read_json, store_lock, write_json


def git_bytes(repo: Path, *args: str) -> bytes:
    result = subprocess.run(["git", "--no-optional-locks", *args], cwd=repo,
                            capture_output=True)
    if result.returncode:
        detail = (result.stderr + result.stdout).decode("utf-8", errors="replace").strip()
        raise WorkflowError(f"Git fehlgeschlagen ({result.returncode}): {detail}")
    return result.stdout


def git(repo: Path, *args: str) -> str:
    return git_bytes(repo, *args).decode("utf-8", errors="replace")


def ancestor(repo: Path, older: str, newer: str) -> bool:
    result = subprocess.run(["git", "merge-base", "--is-ancestor", older, newer], cwd=repo,
                            capture_output=True, encoding="utf-8", errors="replace")
    if result.returncode not in (0, 1):
        raise WorkflowError("Commit-Zugehörigkeit konnte nicht geprüft werden: " + result.stderr.strip())
    return result.returncode == 0


def branch_name(value: str) -> str:
    name = value.removeprefix("feature/")
    if not re.fullmatch(r"[a-z0-9_-]+", name):
        raise WorkflowError("Branch muss feature/<name> oder <name> mit kleinen Buchstaben, Ziffern, _ und - sein.")
    return "feature/" + name


def require_idle(repo: Path, store: Path, branch: str) -> None:
    if (store / "active.json").exists():
        raise WorkflowError("Ein /start-Lauf ist noch aktiv. Zuerst denselben Lauf abschließen oder bewusst abbrechen.")
    if git(repo, "status", "--porcelain", "--untracked-files=all").strip():
        raise WorkflowError("Arbeitsbaum ist nicht sauber. Keine automatische Speicherung, Bereinigung oder Stash.")
    current = git(repo, "branch", "--show-current").strip()
    if current not in ("main", branch):
        raise WorkflowError("Auf main oder dem angegebenen Feature-Branch aufrufen; kein Wechsel von einem fremden/detached Branch.")
    for marker in ("MERGE_HEAD", "CHERRY_PICK_HEAD", "REVERT_HEAD", "rebase-merge", "rebase-apply", "sequencer"):
        path = Path(git(repo, "rev-parse", "--git-path", marker).strip())
        if (path if path.is_absolute() else repo / path).exists():
            raise WorkflowError("Eine Git-Operation ist noch offen: " + marker)
    location = None
    for field in git(repo, "worktree", "list", "--porcelain", "-z").split("\0"):
        if field.startswith("worktree "):
            location = Path(field.removeprefix("worktree ")).resolve()
        elif field in ("branch refs/heads/main", "branch refs/heads/" + branch) and location != repo:
            raise WorkflowError("main oder der Feature-Branch ist in einem anderen Worktree ausgecheckt.")


def prepare(repo: Path, store_root: Path, name: str | None = None) -> dict:
    repo = repo.resolve()
    if Path(git(repo, "rev-parse", "--show-toplevel").strip()).resolve() != repo:
        raise WorkflowError("--repo muss das tatsächliche Git-Projektroot sein.")
    if name is None:
        current = git(repo, "branch", "--show-current").strip()
        if not current.startswith("feature/"):
            raise WorkflowError("Ohne Branchname ist /merge nur auf einem aktuellen Feature-Branch erlaubt. Auf main, fremden oder detached Branches einen Feature-Branch ausdrücklich angeben.")
        branch = branch_name(current)
    else:
        branch = branch_name(name)
    store = outside_repo(repo, (store_root / repo.name).resolve())
    project = store / "project.json"
    if not project.is_file() or Path(read_json(project)["repo"]).resolve() != repo:
        raise WorkflowError("Benchmark-Speicher ist nicht diesem Projekt zugeordnet.")
    require_idle(repo, store, branch)
    source = git(repo, "rev-parse", "--verify", "refs/heads/" + branch + "^{commit}").strip()
    main = git(repo, "rev-parse", "--verify", "refs/heads/main^{commit}").strip()
    base = dict(version=1, repo=str(repo), storeRoot=str(store_root.resolve()),
                branch=branch, sourceCommit=source, mainCommit=main)
    if ancestor(repo, source, main):
        return dict(base, status="already_integrated")
    if not ancestor(repo, main, source):
        raise WorkflowError("main und Feature-Branch sind auseinander gelaufen. Kein automatischer Konflikt-Merge; separat integrieren und prüfen.")
    candidates = []
    for path in (store / "state").glob("*.json"):
        state = read_json(path)
        if state.get("branch") == branch and Path(state.get("repo", "")).resolve() == repo:
            candidates.append((state.get("createdAt", ""), path, state))
    if not candidates:
        raise WorkflowError("Kein zugehöriger Benchmark-Lauf für diesen Branch gefunden.")
    _, state_path, state = max(candidates, key=lambda item: item[0])
    run_id = state.get("id", "")
    if not re.fullmatch(r"[0-9a-f]{32}", run_id) or state_path.name != run_id + ".json":
        raise WorkflowError("Ungültige Run-ID im Zustand.")
    if state.get("status") != "completed":
        raise WorkflowError("Der zugehörige Lauf ist nicht erfolgreich abgeschlossen.")
    folder = Path(state["folder"]).resolve()
    if not folder.is_relative_to((store / "runs").resolve()):
        raise WorkflowError("Run-Ordner liegt außerhalb des Projektarchivs.")
    result = read_json(folder / "result.json")
    finish = state.get("finishCommit", "")
    if not all(isinstance(value, str) and re.fullmatch(r"[0-9a-f]{40}|[0-9a-f]{64}", value)
               for value in (finish, state.get("startCommit"))):
        raise WorkflowError("Ungültige Commit-IDs im Run-Zustand.")
    if not (result.get("Outcome") == "completed" and result.get("Terminal") is True
            and result.get("Id") == run_id and result.get("Branch") == branch
            and result.get("EndCommit") == finish and result.get("StartCommit") == state.get("startCommit")
            and Path(result.get("RunFolder", "")).resolve() == folder):
        raise WorkflowError("Run-Zustand und Ergebnis widersprechen sich.")
    if not ancestor(repo, state["startCommit"], finish) or not ancestor(repo, finish, source):
        raise WorkflowError("Der abgeschlossene Lauf gehört nicht zur aktuellen Branch-Historie.")
    files = [state_path, folder / "result.json", folder / "report.md", folder / "findings.md"]
    hashes = {str(path): hashlib.sha256(path.read_bytes()).hexdigest() for path in files}
    # The current diff is authoritative, even if extra commits followed finish.
    patch = git_bytes(repo, "diff", "--binary", "--full-index", main, source)
    git(repo, "diff", "--check", main, source)
    return dict(base, status="ready", runId=run_id, runCommit=finish,
                additionalCommits=finish != source, report=str(folder / "report.md"),
                findings=str(folder / "findings.md"), evidenceHashes=hashes,
                diffSha256=hashlib.sha256(patch).hexdigest(),
                changedFiles=git(repo, "diff", "--name-only", main, source).splitlines(),
                commits=git(repo, "log", "--format=%H %s", main + ".." + source).splitlines())


def apply(repo: Path, plan_file: Path, message_file: Path | None) -> dict:
    repo = repo.resolve()
    plan_file = outside_repo(repo, plan_file)
    plan = read_json(plan_file)
    if Path(plan["repo"]).resolve() != repo:
        raise WorkflowError("Merge-Plan gehört zu einem anderen Projekt.")
    store_root = Path(plan["storeRoot"])
    store = outside_repo(repo, (store_root / repo.name).resolve())
    # Same OS lock as /start: no new workflow begins during the merge.
    with store_lock(store):
        fresh = prepare(repo, store_root, plan["branch"])
        if fresh != plan:
            raise WorkflowError("Branch, main oder Messbelege haben sich geändert. Neu vorbereiten und aktuelle Änderungen prüfen.")
        if plan["status"] == "already_integrated":
            git(repo, "switch", "main")
            return dict(status="already_integrated", branch="main", commit=plan["mainCommit"],
                        featureBranch=plan["branch"])
        if message_file is None:
            raise WorkflowError("Für den Merge ist eine UTF-8-Commit-Nachricht erforderlich.")
        message_file = outside_repo(repo, message_file)
        message = message_file.read_text(encoding="utf-8-sig").strip()
        lines = message.splitlines()
        if not lines or not lines[0].strip() or len(lines) < 3 or lines[1].strip() or not any(line.startswith("- ") for line in lines[2:]):
            raise WorkflowError("Nachricht braucht einen Titel, eine Leerzeile und mindestens einen Stichpunkt über erledigte Arbeit.")
        message += f"\n\nBranch: {plan['branch']}\nBenchmark-Run: {plan['runId']}\n"
        with tempfile.NamedTemporaryFile(mode="w", encoding="utf-8", prefix="opencode-merge-",
                                         suffix=".txt", dir=message_file.parent, delete=False) as handle:
            handle.write(message)
            commit_message = Path(handle.name)
        try:
            git(repo, "switch", "main")
            # Merge the pinned SHA, not a potentially moved branch name. Keep hooks/signing.
            git(repo, "merge", "--no-ff", "--no-edit", "--no-log", "--no-stat",
                "-F", str(commit_message), plan["sourceCommit"])
        finally:
            commit_message.unlink()
        merged = git(repo, "rev-parse", "HEAD").strip()
        parents = git(repo, "rev-list", "--parents", "-n", "1", merged).split()[1:]
        if (parents != [plan["mainCommit"], plan["sourceCommit"]]
                or git(repo, "rev-parse", "HEAD^{tree}") != git(repo, "rev-parse", plan["sourceCommit"] + "^{tree}")
                or git(repo, "rev-parse", "refs/heads/" + plan["branch"]).strip() != plan["sourceCommit"]):
            raise WorkflowError("Merge wurde ausgeführt, aber seine Eltern/Dateien/Branch-Spitze sind unerwartet. Tatsächlichen Zustand prüfen; kein automatisches Zurücksetzen.")
        require_idle(repo, store, plan["branch"])
        if git(repo, "branch", "--show-current").strip() != "main":
            raise WorkflowError("Merge ausgeführt, aber aktiver Branch ist nicht main.")
        return dict(status="merged", branch="main", commit=merged, featureBranch=plan["branch"],
                    featureCommit=plan["sourceCommit"], runId=plan["runId"], message=message)


def main(argv=None) -> int:
    for stream in (sys.stdout, sys.stderr):
        if hasattr(stream, "reconfigure"):
            stream.reconfigure(encoding="utf-8")
    parser = argparse.ArgumentParser(description="Eigenständiger lokaler /merge-Command; kein /start-Modul")
    parser.add_argument("--repo", type=Path, default=Path(__file__).resolve().parents[2])
    parser.add_argument("--store-root", type=Path, default=Path(os.environ.get("OPENCODE_BENCHMARK_ROOT", "C:/Dev/AI-Benchmarks" if os.name == "nt" else str(Path.home() / "AI-Benchmarks"))))
    commands = parser.add_subparsers(dest="action", required=True)
    prep = commands.add_parser("prepare", help="Git und Run prüfen; externen Plan schreiben, keine Branchänderung")
    prep.add_argument("--branch", help="Optional: Feature-Branch; ohne Angabe wird ausschließlich der aktuelle feature/*-Branch verwendet")
    prep.add_argument("--plan-file", type=Path, required=True)
    merge = commands.add_parser("apply", help="Geprüften Plan und Commit-Text anwenden; main bleibt aktiv")
    merge.add_argument("--plan-file", type=Path, required=True)
    merge.add_argument("--message-file", type=Path)
    args = parser.parse_args(argv)
    try:
        repo = args.repo.resolve()
        if args.action == "prepare":
            plan_file = outside_repo(repo, args.plan_file)
            result = prepare(repo, args.store_root.resolve(), args.branch)
            write_json(plan_file, result)
        else:
            result = apply(repo, args.plan_file, args.message_file)
        print(json.dumps(result, ensure_ascii=False, indent=2))
        return 0
    except (WorkflowError, OSError, ValueError, KeyError) as error:
        print(f"Merge gestoppt: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
