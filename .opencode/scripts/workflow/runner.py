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
    task_mode = request.get("task_mode", "implementation")
    if task_mode not in ("analysis", "implementation"):
        raise WorkflowError("task_mode must be analysis or implementation.")
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
    return {"modules": selected, "task": task, "taskMode": task_mode, "branchName": request.get("branch"),
            "branch": target_branch, "targetClass": request.get("target_class"),
            "model": model["name"] if model else "not specified",
            "modelKey": key, "provider": model["provider"] if model else "not specified",
            "expectedModel": model.get("model") if model and model["mode"] == "forced" else None,
            "sessionId": request.get("session_id"),
            "promptProvider": request.get("prompt_provider"),
            "promptModel": request.get("prompt_model")}


def prepare(repo: Path, root: Path, request: dict, catalog: Path,
            usage_export: Path | None = None) -> dict:
    repo = repo.resolve()
    store = store_path(repo, root.resolve())
    with store_lock(store):
        return _prepare(repo, store, request, catalog, usage_export)


def _prepare(repo, store, request, catalog, usage_export):
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
                 initialBranch=branch.current(repo),
                 startCommit=run(repo, "git", "rev-parse", "HEAD").strip())
    state_path = store / "state" / (run_id + ".json")
    write_json(state_path, state)
    write_json(active, {"id": run_id})
    try:
        if "benchmark" in state["modules"]:
            state["before"] = benchmark.collect(repo, state["targetClass"], folder / "benchmark-before.json")
            branch.require_clean(repo)  # Maven may generate tracked files.
        if before_usage is not None:
            state["sessionId"] = before_usage["sessionId"]
            write_json(folder / "usage-preflight.json", before_usage)
        state.update(status="prepared", preparedAt=now())
        write_json(state_path, state)
    except Exception as error:
        state.update(status="prepare-failed", error=str(error))
        write_json(state_path, state)
        raise
    return {"id": run_id, "folder": str(folder), "status": "prepared", "session_id": state["sessionId"]}

def begin(repo: Path, root: Path, request: dict | None, catalog: Path,
          usage_export: Path | None = None, prepared_id: str | None = None) -> dict:
    repo = repo.resolve()
    store = store_path(repo, root.resolve())
    with store_lock(store):
        if prepared_id is None:
            if request and {"benchmark", "usage"} <= set(modules(request.get("modules", "complete"))):
                raise WorkflowError("benchmark+usage requires prepare --request, a fresh usage snapshot, then begin --id.")
            prepared_id = _prepare(repo, store, request, catalog, usage_export)["id"]
        state_path, state = load_state(repo, store, prepared_id)
        if state["status"] != "prepared":
            raise WorkflowError("Begin requires a prepared run.")
        active = store / "active.json"
        if not active.exists() or read_json(active)["id"] != prepared_id:
            raise WorkflowError("Prepared run is not the active run.")
        branch.require_clean(repo)
        if (branch.current(repo) != state["initialBranch"] or
                run(repo, "git", "rev-parse", "HEAD").strip() != state["startCommit"]):
            raise WorkflowError("Repository changed after prepare; abort and prepare again.")
        folder = Path(state["folder"])
        if "usage" in state["modules"]:
            measured = usage.capture(repo, state["sessionId"], usage_export)
            if "benchmark" in state["modules"] and measured["capturedMs"] < datetime.fromisoformat(state["preparedAt"]).timestamp() * 1000:
                raise WorkflowError("Begin requires a fresh usage snapshot AFTER baseline preparation.")
            state["start"] = usage.timestamp(measured["capturedMs"])
            write_json(folder / "usage-before.json", measured)
        else:
            state["start"] = now()
        try:
            (folder / "original-prompt.md").write_text(state["task"].strip() + "\n", encoding="utf-8")
            if "prompt" in state["modules"]:
                state["effectiveTask"] = prompt.improve(state["task"], folder,
                                                        state["promptProvider"], state["promptModel"])
                metadata = folder / "prompt-metadata.json"
                if metadata.exists():
                    state["promptMetadata"] = read_json(metadata)
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
        return {"id": prepared_id, "modules": state["modules"], "branch": state["branch"],
                "folder": str(folder), "task": state["effectiveTask"],
                "task_mode": state.get("taskMode", "implementation"),
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
             "Auftragsart: " + {"analysis": "Analyse", "implementation": "Implementierung"}.get(state.get("taskMode"), "nicht erfasst"), "",
             "## Durchgeführte Arbeit und Verhaltensänderungen", "",
             state.get("implementationSummary", "Für diesen Lauf wurde kein Arbeitsbericht übergeben."), "",
             "## Ergebnis", ""]
    lines.extend(f"- {key}: {'nicht verfügbar' if value is None else value}" for key, value in result.items())
    if "usage" in state["modules"]:
        lines += ["", usage.cost_description(result["EstimatedCostUSD"]),
                  "MessageElapsedSeconds umfasst Message-, Tool- und Wartezeiten; keine reine Modell-Rechenzeit."]
    if after:
        lines += ["", "## Tests und Coverage", "", "| Messwert | Vorher | Nachher |", "|---|---:|---:|"]
        lines.extend(f"| {key} | {state['before'][key]} | {after[key]} |" for key in after)
    lines += ["", "## Geänderte Dateien", "", *changed["files"], "", "## OpenCode Usage", ""]
    if usage_result:
        lines.extend(f"- {key}: {'nicht verfügbar' if value is None else value}" for key, value in usage_result.items())
    else:
        lines += ["Usage-Modul nicht ausgewählt."]
    lines += ["", "## Offene Findings", "", "Siehe findings.md für gemeldete offene Probleme und den Erfassungsstatus.",
              "", "## Artefakte", "", "- result.json", "- findings.md", "- diff.patch", "- original-prompt.md"]
    if "prompt" in state["modules"]:
        lines += ["- improved-prompt.md", "- prompt-metadata.json"]
    if "benchmark" in state["modules"]:
        lines += ["- benchmark-before.json", "- benchmark-after.json"]
    if usage_result:
        lines += ["- usage-before.json", "- usage-after.json", "- usage.json"]
    return "\n".join(lines) + "\n"


def finish(repo: Path, root: Path, run_id: str, interventions: int = 0,
           corrections: int = 0, usage_export: Path | None = None,
           summary_file: Path | None = None) -> dict:
    if interventions < 0 or corrections < 0:
        raise WorkflowError("Interventions and correction rounds must not be negative.")
    repo = repo.resolve()
    store = store_path(repo, root.resolve())
    with store_lock(store):
        path, state = load_state(repo, store, run_id)
        if state["status"] == "completed":
            return read_json(Path(state["folder"]) / "result.json")
        if state["status"] not in ("active", "finishing"):
            raise WorkflowError("Finish requires an active run; begin a prepared run or abort it.")
        summary = None
        if summary_file is not None:
            summary = outside_repo(repo, summary_file).read_text(encoding="utf-8-sig").strip()
            if not summary:
                raise WorkflowError("The work summary must not be empty.")
        actual_branch = branch.current(repo)
        if "branch" in state["modules"] and actual_branch != state["branch"]:
            raise WorkflowError(f"Finish requires exactly {state['branch']}.")
        if "benchmark" in state["modules"] and not actual_branch.startswith("feature/"):
            raise WorkflowError("Benchmark finish requires a feature/* branch.")
        branch.require_clean(repo, findings=True)
        end_commit = run(repo, "git", "rev-parse", "HEAD").strip()
        if state.get("taskMode") == "analysis" and end_commit != state["startCommit"]:
            raise WorkflowError("Analysis runs must not contain task commits. Inspect the changes; do not reset them automatically.")
        run(repo, "git", "merge-base", "--is-ancestor", state["startCommit"], end_commit)
        folder = Path(state["folder"])
        # Preserve the original cutoff when retrying a failed final build/artifact write.
        state.setdefault("end", now())
        if state.get("finishCommit") and state["finishCommit"] != end_commit:
            state["end"] = now()
            state.pop("usageAfter", None)
            state.pop("implementationSummary", None)  # Old prose does not describe a new commit.
        if summary is not None:
            state["implementationSummary"] = summary
        state.update(status="finishing", finishCommit=end_commit)
        if "usage" in state["modules"] and "usageAfter" not in state:
            state["usageAfter"] = usage.capture(repo, state["sessionId"], usage_export)
            state["end"] = usage.timestamp(state["usageAfter"]["capturedMs"])
        write_json(path, state)
        usage_result = None
        if "usage" in state["modules"]:
            usage_result = usage.difference(read_json(folder / "usage-before.json"), state["usageAfter"])
            if state["expectedModel"] and any(model != state["expectedModel"] for model in usage_result["ActualModels"]):
                raise WorkflowError("Usage model differs from the forced model. Do not relabel the run.")
            write_json(folder / "usage-after.json", state["usageAfter"])
            write_json(folder / "usage.json", usage_result)
        after = benchmark.collect(repo, state["targetClass"], folder / "benchmark-after.json") if "benchmark" in state["modules"] else None
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
        result.update(CostStatus="unavailable", UsageStartUTC=None, UsageEndUTC=None, UsageWindowSeconds=None)
        metadata = state.get("promptMetadata", {})
        result.update(PromptProvider=metadata.get("provider"), PromptModel=metadata.get("model"),
                      PromptDurationSeconds=metadata.get("durationSeconds"))
        if usage_result:
            result.update({key: usage_result[key] for key in usage.VALUE_FIELDS})
            result.update(SessionId=usage_result["SessionId"], ActualModels=",".join(usage_result["ActualModels"]),
                          PendingMessages=usage_result["PendingMessages"])
            result.update({key: usage_result[key] for key in ("CostStatus", "UsageStartUTC", "UsageEndUTC", "UsageWindowSeconds")})
        source = repo / "findings.md"
        destination = folder / "findings.md"
        if source.is_file():
            shutil.copyfile(source, destination)  # Archive before removing the untracked original.
        elif not destination.exists():
            destination.write_text("# Offene Findings\n\nFür diesen Lauf wurden keine Findings-Angaben übergeben. "
                                   "Es ist nicht dokumentiert, ob weitere offene Probleme festgestellt wurden.\n", encoding="utf-8")
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


def status(repo: Path, root: Path, run_id: str | None = None) -> dict:
    """Read atomic state files without creating a store/lock or running Git/providers."""
    repo = repo.resolve()
    store = store_path(repo, root.resolve())
    project = store / "project.json"
    if project.exists() and Path(read_json(project)["repo"]).resolve() != repo:
        raise WorkflowError("Another repository uses this benchmark project directory. Choose another --store-root.")

    def active_id():
        try:
            return read_json(store / "active.json")["id"]
        except FileNotFoundError:
            return None

    # A writer may complete/replace the active run between the two reads.
    for _ in range(3):
        active = active_id()
        selected = run_id if run_id is not None else active
        state_path, state = load_state(repo, store, selected) if selected is not None else (None, None)
        if active == active_id():
            break
    else:
        raise WorkflowError("Workflow state changed during status lookup; retry status.")
    result = {"repo": str(repo), "store": str(store), "active": selected is not None and selected == active,
              "active_id": active, "id": selected, "status": state["status"] if state else "idle"}
    if state is None:
        result["next_steps"] = ["Kein aktiver Lauf. Für einen neuen Auftrag /start verwenden."]
        return result
    result.update(task_mode=state.get("taskMode"), modules=state["modules"],
                  branch=state["branch"], folder=state["folder"], state_file=str(state_path),
                  start_commit=state["startCommit"], error_recorded=bool(state.get("error")))
    artifact_names = ("report.md", "findings.md", "diff.patch", "result.json")
    folder = Path(state["folder"])
    result["artifacts"] = {name: str(folder / name) for name in artifact_names if (folder / name).is_file()}
    phase = state["status"]
    if phase == "prepared":
        steps = [f"Vorbereitung abgeschlossen. begin --id {selected} verwenden."]
        if "usage" in state["modules"]:
            steps += ["Zuerst einen frischen Usage-Snapshot derselben Session erfassen und mit --usage-export übergeben."]
    elif phase == "active":
        steps = ["Auftrag im erlaubten Umfang durchführen und prüfen; Arbeitsbericht und offene Findings dokumentieren.",
                 f"Danach finish --id {selected} mit --summary-file und den tatsächlichen Zählern aufrufen."]
        if "usage" in state["modules"]:
            steps += ["Vor finish einen frischen Usage-Snapshot derselben Session erfassen und mit --usage-export übergeben."]
    elif phase in ("preparing", "finishing"):
        steps = ["Falls der Workflow-Prozess noch läuft: seinen Abschluss abwarten."]
        if phase == "finishing":
            steps += ["Falls er beendet wurde: Fehler prüfen und finish mit denselben Angaben wiederholen. Nach neuen Task-Commits Bericht und Usage-Snapshot aktualisieren."]
        else:
            steps += ["Falls er beendet wurde: Ursache prüfen, den Lauf mit abort --id freigeben und neu vorbereiten."]
    elif phase in ("prepare-failed", "begin-failed"):
        steps = ["Fehlerdetails in der Zustandsdatei prüfen.",
                 f"Nach Klärung abort --id {selected} verwenden und die Voraussetzungen für einen neuen Lauf prüfen. Abort setzt keine Git-Änderungen zurück."]
    elif phase == "completed":
        steps = ["Lauf abgeschlossen. Report, Findings und Diff im Artefaktordner prüfen."]
    elif phase == "aborted":
        steps = ["Lauf abgebrochen. Git-Zustand und erhaltene Artefakte prüfen, bevor ein neuer Auftrag gestartet wird."]
    else:
        steps = ["Unbekannter Zustand: Zustandsdatei prüfen; keine automatische Wiederaufnahme."]
    if selected != active and phase not in ("completed", "aborted"):
        steps = ["Dieser Lauf ist nicht als aktiv registriert. Zustandsdatei und aktuellen Lauf prüfen; keine automatische Wiederaufnahme."]
    result["next_steps"] = steps
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
