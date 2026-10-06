from __future__ import annotations

import re
import shutil
import uuid
from datetime import datetime, timezone
from pathlib import Path

from . import benchmark, branch, prompt, usage
from .common import WorkflowError, outside_repo, read_json, run, store_lock, write_json

MODULES = ("branch", "prompt", "benchmark", "usage")
OWNER = "opencode-start-v1"


def modules(value) -> list[str]:
    if value == "complete":
        return list(MODULES)
    items = value.split(",") if isinstance(value, str) else value
    if not isinstance(items, list) or not items or any(item not in MODULES for item in items):
        raise WorkflowError("Modules must be complete or a nonempty selection of branch,prompt,benchmark,usage.")
    if len(set(items)) != len(items):
        raise WorkflowError("Duplicate modules are not allowed.")
    return [item for item in MODULES if item in items]


def now() -> str:
    return datetime.now(timezone.utc).isoformat()


def store_path(repo: Path, root: Path) -> Path:
    # Preserve the original project's directory name; detect name collisions below.
    return outside_repo(repo, root / repo.name)


def identity(repo: Path, store: Path) -> None:
    path = store / "project.json"
    if path.exists() and Path(read_json(path)["repo"]).resolve() != repo:
        raise WorkflowError("Another repository uses this benchmark project directory. Choose another --store-root.")
    if not path.exists():
        write_json(path, {"repo": str(repo)})


def validate_request(repo: Path, request: dict, catalog: Path) -> dict:
    selected = modules(request.get("modules", "complete"))
    task = request.get("task")
    if not isinstance(task, str) or not task.strip():
        raise WorkflowError("A nonempty task is required.")
    branch.require_clean(repo)
    target_branch = branch.current(repo)
    if not target_branch:
        raise WorkflowError("Detached HEAD is not supported.")
    if "branch" in selected:
        target_branch = branch.validate(repo, request.get("branch"))
    if "benchmark" in selected:
        if branch.current(repo) != "main":
            raise WorkflowError("Benchmark baseline must start on main.")
        target = request.get("target_class", "")
        if not isinstance(target, str) or not re.fullmatch(r"[A-Za-z_$][\w$]*", target):
            raise WorkflowError("benchmark requires a simple Java/JaCoCo target_class.")
    entries = read_json(catalog)["models"]
    key = request.get("model")
    model = next((entry for entry in entries if entry["command"] == key), None)
    if not model and "benchmark" in selected:
        raise WorkflowError("benchmark requires a model key from benchmark-models.json.")
    return {"modules": selected, "task": task, "branchName": request.get("branch"),
            "branch": target_branch, "targetClass": request.get("target_class"),
            "model": model["name"] if model else "not specified",
            "modelKey": key, "provider": model["provider"] if model else "not specified",
            "expectedModel": model.get("model") if model and model["mode"] == "forced" else None,
            "sessionId": request.get("session_id"),
            "promptProvider": request.get("prompt_provider"),
            "promptModel": request.get("prompt_model")}


def begin(repo: Path, root: Path, request: dict, catalog: Path,
          usage_export: Path | None = None) -> dict:
    repo = repo.resolve()
    store = store_path(repo, root.resolve())
    with store_lock(store):
        state = validate_request(repo, request, catalog)
        identity(repo, store)
        active = store / "active.json"
        if active.exists():
            raise WorkflowError(f"Unfinished workflow {read_json(active)['id']}; finish or abort it first.")
        # Usage preflight happens before Maven, prompt-provider calls or Git mutations.
        before_usage = usage.capture(repo, state["sessionId"], usage_export) if "usage" in state["modules"] else None
        run_id = uuid.uuid4().hex
        folder = store / "runs" / run_id
        folder.mkdir(parents=True)
        state.update(owner=OWNER, id=run_id, repo=str(repo), folder=str(folder), status="preparing",
                     startCommit=run(repo, "git", "rev-parse", "HEAD").strip())
        state_path = store / "state" / (run_id + ".json")
        write_json(state_path, state)
        write_json(active, {"id": run_id})
        try:
            if "benchmark" in state["modules"]:
                state["before"] = benchmark.collect(repo, state["targetClass"])
                branch.require_clean(repo)  # Maven may generate tracked files.
            state["start"] = now()  # Excludes baseline builds, includes prompt and branch work.
            if before_usage is not None:
                state["sessionId"] = before_usage["sessionId"]
                write_json(folder / "usage-before.json", before_usage)
            (folder / "original-prompt.md").write_text(state["task"].strip() + "\n", encoding="utf-8")
            if "prompt" in state["modules"]:
                state["effectiveTask"] = prompt.improve(state["task"], folder,
                                                        state["promptProvider"], state["promptModel"])
            else:
                state["effectiveTask"] = state["task"]
            if "branch" in state["modules"]:
                state["branch"] = branch.create(repo, state["branchName"])
            state["status"] = "active"
            write_json(state_path, state)
        except Exception as error:
            state.update(status="begin-failed", error=str(error))
            write_json(state_path, state)
            raise
        return {"id": run_id, "modules": state["modules"], "branch": state["branch"],
                "folder": str(folder), "task": state["effectiveTask"],
                "originalTask": state["task"], "expectedModel": state["expectedModel"]}


def load_state(repo: Path, store: Path, run_id: str) -> tuple[Path, dict]:
    if not re.fullmatch(r"[a-f0-9]{32}", run_id):
        raise WorkflowError("Invalid workflow ID.")
    path = store / "state" / (run_id + ".json")
    if not path.exists():
        raise WorkflowError(f"Workflow state not found: {path}")
    state = read_json(path)
    if state.get("owner") != OWNER or state.get("id") != run_id or Path(state["repo"]).resolve() != repo:
        raise WorkflowError("Invalid workflow state or repository.")
    if Path(state["folder"]).resolve() != (store / "runs" / run_id).resolve():
        raise WorkflowError("Invalid artifact directory in workflow state.")
    return path, state


def result_row(state: dict, end: str, end_commit: str, current_branch: str,
               after: dict | None, changed: dict, interventions: int, corrections: int) -> dict:
    result = {"Date": datetime.fromisoformat(end).strftime("%Y-%m-%d %H:%M:%S"),
              "Id": state["id"], "Task": state["task"], "Model": state["model"],
              "Provider": state["provider"], "TargetClass": state["targetClass"],
              "StartCommit": state["startCommit"], "EndCommit": end_commit,
              "Branch": current_branch,
              "DurationSeconds": round((datetime.fromisoformat(end) - datetime.fromisoformat(state["start"])).total_seconds(), 1)}
    before = state.get("before", {})
    after = after or {}
    for column, source, key in (("TestsBefore", before, "tests"), ("TestsAfter", after, "tests"),
                               ("FailuresAfter", after, "failures"), ("ErrorsAfter", after, "errors"),
                               ("SkippedAfter", after, "skipped"),
                               ("TestTimeBeforeSeconds", before, "testTime"), ("TestTimeAfterSeconds", after, "testTime"),
                               ("LineCoverageBefore", before, "lineCoverage"), ("LineCoverageAfter", after, "lineCoverage"),
                               ("BranchCoverageBefore", before, "branchCoverage"), ("BranchCoverageAfter", after, "branchCoverage")):
        result[column] = source.get(key)
    result.update(HumanInterventions=interventions, CorrectionRounds=corrections,
                  **{key: changed[key] for key in ("ChangedFiles", "Insertions", "Deletions")})
    return result


def report(state: dict, result: dict, after: dict | None, changed: dict, usage_result: dict | None) -> str:
    lines = ["# AI Coding Benchmark / Start Workflow", "", "## Aufgabe", "", state["task"], "",
             "## Ergebnis", ""]
    lines.extend(f"- {key}: {'nicht verfügbar' if value is None else value}" for key, value in result.items())
    if after:
        lines += ["", "## Tests und Coverage", "", "| Messwert | Vorher | Nachher |", "|---|---:|---:|"]
        lines.extend(f"| {key} | {state['before'][key]} | {after[key]} |" for key in after)
    lines += ["", "## Geänderte Dateien", "", *changed["files"], "", "## OpenCode Usage", ""]
    if usage_result:
        lines.extend(f"- {key}: {'nicht verfügbar' if value is None else value}" for key, value in usage_result.items())
    else:
        lines += ["Usage-Modul nicht ausgewählt."]
    lines += ["", "## Artefakte", "", "- result.json", "- findings.md", "- diff.patch", "- original-prompt.md"]
    if "prompt" in state["modules"]:
        lines += ["- improved-prompt.md"]
    if usage_result:
        lines += ["- usage-before.json", "- usage-after.json", "- usage.json"]
    return "\n".join(lines) + "\n"


def finish(repo: Path, root: Path, run_id: str, interventions: int = 0,
           corrections: int = 0, usage_export: Path | None = None) -> dict:
    if interventions < 0 or corrections < 0:
        raise WorkflowError("Interventions and correction rounds must not be negative.")
    repo = repo.resolve()
    store = store_path(repo, root.resolve())
    with store_lock(store):
        path, state = load_state(repo, store, run_id)
        if state["status"] == "completed":
            return read_json(Path(state["folder"]) / "result.json")
        if state["status"] not in ("active", "finishing"):
            raise WorkflowError("Cannot finish a workflow whose begin failed; abort it.")
        actual_branch = branch.current(repo)
        if "branch" in state["modules"] and actual_branch != state["branch"]:
            raise WorkflowError(f"Finish requires exactly {state['branch']}.")
        if "benchmark" in state["modules"] and not actual_branch.startswith("feature/"):
            raise WorkflowError("Benchmark finish requires a feature/* branch.")
        branch.require_clean(repo, findings=True)
        end_commit = run(repo, "git", "rev-parse", "HEAD").strip()
        run(repo, "git", "merge-base", "--is-ancestor", state["startCommit"], end_commit)
        folder = Path(state["folder"])
        # Preserve the original cutoff when retrying a failed final build/artifact write.
        state.setdefault("end", now())
        if state.get("finishCommit") and state["finishCommit"] != end_commit:
            state["end"] = now()
            state.pop("usageAfter", None)
        state.update(status="finishing", finishCommit=end_commit)
        if "usage" in state["modules"] and "usageAfter" not in state:
            state["usageAfter"] = usage.capture(repo, state["sessionId"], usage_export)
        write_json(path, state)
        usage_result = None
        if "usage" in state["modules"]:
            usage_result = usage.difference(read_json(folder / "usage-before.json"), state["usageAfter"])
            if state["expectedModel"] and any(model != state["expectedModel"] for model in usage_result["ActualModels"]):
                raise WorkflowError("Usage model differs from the forced model. Do not relabel the run.")
            write_json(folder / "usage-after.json", state["usageAfter"])
            write_json(folder / "usage.json", usage_result)
        after = benchmark.collect(repo, state["targetClass"]) if "benchmark" in state["modules"] else None
        branch.require_clean(repo, findings=True)
        changed = benchmark.changes(repo, state["startCommit"], end_commit, folder / "diff.patch")
        result = result_row(state, state["end"], end_commit, actual_branch, after, changed, interventions, corrections)
        result["Modules"] = ",".join(state["modules"])
        result.update(RunFolder=str(folder), ReportPath=str(folder / "report.md"),
                      FindingsPath=str(folder / "findings.md"), DiffPath=str(folder / "diff.patch"),
                      CsvPath=str(store / "results.csv") if "benchmark" in state["modules"] else None)
        # One schema for every module selection; unselected usage is unavailable.
        result.update({key: None for key in usage.VALUE_FIELDS})
        result.update(SessionId=None, ActualModels=None, PendingMessages=None)
        if usage_result:
            result.update({key: usage_result[key] for key in usage.VALUE_FIELDS})
            result.update(SessionId=usage_result["SessionId"], ActualModels=",".join(usage_result["ActualModels"]),
                          PendingMessages=usage_result["PendingMessages"])
        source = repo / "findings.md"
        destination = folder / "findings.md"
        if source.is_file():
            shutil.copyfile(source, destination)  # Archive before removing the untracked original.
        elif not destination.exists():
            destination.write_text("# Findings\n\nFür diesen Lauf wurde keine findings.md erzeugt.\n", encoding="utf-8")
        write_json(folder / "result.json", result)
        (folder / "report.md").write_text(report(state, result, after, changed, usage_result), encoding="utf-8")
        if "benchmark" in state["modules"]:
            benchmark.append_csv(store / "results.csv", result)
        if source.is_file():
            source.unlink()
        branch.require_clean(repo)
        state["status"] = "completed"
        state.pop("usageAfter", None)
        write_json(path, state)
        active = store / "active.json"
        if active.exists() and read_json(active)["id"] == run_id:
            active.unlink()
        return result


def abort(repo: Path, root: Path, run_id: str) -> dict:
    repo = repo.resolve()
    store = store_path(repo, root.resolve())
    with store_lock(store):
        path, state = load_state(repo, store, run_id)
        if state["status"] == "completed":
            raise WorkflowError("Completed workflows cannot be aborted.")
        state.update(status="aborted", aborted=now())
        write_json(path, state)
        active = store / "active.json"
        if active.exists() and read_json(active)["id"] == run_id:
            active.unlink()
        return {"id": run_id, "status": "aborted", "folder": state["folder"]}
