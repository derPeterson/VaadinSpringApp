"""Offline regression tests: real temporary Git repos, no live provider calls."""
import csv
import json
import os
import subprocess
import sys
import tempfile
import time
import unittest
from pathlib import Path
from unittest.mock import patch, Mock

from workflow import benchmark, branch, prompt, runner, usage
from workflow.common import WorkflowError, read_json, write_json, store_lock
from generate_start_commands import generate, MARKER

CATALOG = Path(__file__).resolve().parents[2] / "benchmark-models.json"
METRICS = {"tests": 3, "failures": 0, "errors": 0, "skipped": 1,
           "testTime": 0.123, "lineCoverage": 75.0, "branchCoverage": 50.0,
           "linesCovered": 3, "linesTotal": 4, "branchesCovered": 1, "branchesTotal": 2}


class RepoCase(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="workflow-test-", dir=Path.cwd())
        self.addCleanup(self.temp.cleanup)
        self.base = Path(self.temp.name)
        self.repo = self.base / "ExampleProject"
        self.repo.mkdir()
        self.store_root = self.base / "benchmarks"
        self.git("init", "-b", "main")
        self.git("config", "user.name", "Workflow Test")
        self.git("config", "user.email", "workflow@example.invalid")
        (self.repo / ".gitignore").write_text("target/\n", encoding="utf-8")
        (self.repo / "source.txt").write_text("baseline\n", encoding="utf-8")
        self.git("add", "--", ".gitignore", "source.txt")
        self.git("commit", "-m", "baseline")
        self.request = {"modules": "branch,benchmark", "branch": "tests", "target_class": "UserService",
                        "model": "gpt61-sol", "task": 'Add tests; $(bad) `bad` "quoted"\nnext line'}

    def git(self, *args):
        result = subprocess.run(["git", *args], cwd=self.repo, capture_output=True, text=True, encoding="utf-8")
        if result.returncode:
            self.fail(result.stderr)
        return result.stdout.strip()

    def begin(self, selected=None):
        if selected:
            self.request["modules"] = selected
        with patch("workflow.benchmark.collect", return_value=METRICS):
            if {"benchmark", "usage"} <= set(runner.modules(self.request["modules"])):
                prepared = runner.prepare(self.repo, self.store_root, self.request, CATALOG)
                return runner.begin(self.repo, self.store_root, None, CATALOG, prepared_id=prepared["id"])
            return runner.begin(self.repo, self.store_root, self.request, CATALOG)

    def commit_task(self):
        (self.repo / "source.txt").write_text("baseline\nchanged ä\n", encoding="utf-8")
        (self.repo / "binary.dat").write_bytes(b"\0\x01\xff")
        self.git("add", "--", "source.txt", "binary.dat")
        self.git("commit", "-m", "task")

    def export(self, messages=None):
        return {"info": {"id": "ses_test", "directory": str(self.repo)}, "messages": messages or []}

    def write_reports(self, extra=False, class_name="UserService", failures=0):
        jacoco = self.repo / "target/site/jacoco"
        surefire = self.repo / "target/surefire-reports"
        jacoco.mkdir(parents=True, exist_ok=True)
        surefire.mkdir(parents=True, exist_ok=True)
        header = "GROUP,PACKAGE,CLASS,LINE_MISSED,LINE_COVERED,BRANCH_MISSED,BRANCH_COVERED\n"
        rows = f"app,de.app,{class_name},1,3,1,1\n"
        if extra:
            rows += f"app,other,{class_name},4,0,0,0\n"
        (jacoco / "jacoco.csv").write_text(header + rows, encoding="utf-8")
        (surefire / "TEST-Test.xml").write_text(
            f'<testsuite tests="3" failures="{failures}" errors="0" skipped="1" time="0.123"/>', encoding="utf-8")

    def test_full_benchmark_preserves_artifacts_csv_and_clean_tree(self):
        started = self.begin()
        self.assertEqual("feature/tests", self.git("branch", "--show-current"))
        self.commit_task()
        findings = "# Offene Findings\n\nP2: Weiterer Fehler ä, belegt in source.txt.\n"
        (self.repo / "findings.md").write_text(findings, encoding="utf-8")
        summary = self.base / "summary.md"
        summary.write_text("Die Konvertierung wurde korrigiert; ungültige Werte werden abgelehnt.", encoding="utf-8")
        with patch("workflow.benchmark.collect", return_value=METRICS):
            result = runner.finish(self.repo, self.store_root, started["id"], 2, 1, summary_file=summary)
        folder = Path(started["folder"])
        self.assertEqual(2, result["HumanInterventions"])
        self.assertEqual(1, result["CorrectionRounds"])
        self.assertEqual(self.git("rev-parse", "HEAD"), result["EndCommit"])
        self.assertEqual(2, result["ChangedFiles"])
        self.assertEqual(3, result["TestsBefore"])
        self.assertEqual(findings, (folder / "findings.md").read_text(encoding="utf-8"))
        report = (folder / "report.md").read_text(encoding="utf-8")
        self.assertIn("## Durchgeführte Arbeit und Verhaltensänderungen", report)
        self.assertIn(summary.read_text(encoding="utf-8"), report)
        self.assertNotIn("Konvertierung", (folder / "findings.md").read_text(encoding="utf-8"))
        self.assertTrue(summary.exists())  # Caller-owned transport file is not removed.
        self.assertIn(b"GIT binary patch", (folder / "diff.patch").read_bytes())
        self.assertIn(b"changed \xc3\xa4", (folder / "diff.patch").read_bytes())
        self.assertEqual("", self.git("status", "--porcelain"))
        self.assertEqual(self.request["task"], result["Task"])
        store = self.store_root / self.repo.name
        self.assertFalse((store / "active.json").exists())
        with (store / "results.csv").open(encoding="utf-8", newline="") as handle:
            row = next(csv.DictReader(handle))
        self.assertEqual("0.123", row["TestTimeBeforeSeconds"])
        self.assertEqual("75.00", row["LineCoverageBefore"])
        self.assertEqual("", row["InputTokens"])
        # Idempotent finish returns the same result without rerunning Maven or duplicating CSV.
        summary.write_text("Spätere Erklärung", encoding="utf-8")
        self.assertEqual(result, runner.finish(self.repo, self.store_root, started["id"], summary_file=summary))
        self.assertEqual(report, (folder / "report.md").read_text(encoding="utf-8"))

    def test_missing_findings_does_not_claim_no_problems(self):
        started = self.begin("branch")
        runner.finish(self.repo, self.store_root, started["id"])
        folder = Path(started["folder"])
        findings = (folder / "findings.md").read_text(encoding="utf-8")
        self.assertIn("keine Findings-Angaben übergeben", findings)
        self.assertNotIn("Keine weiteren offenen Findings festgestellt.", findings)
        self.assertIn("kein Arbeitsbericht übergeben", (folder / "report.md").read_text(encoding="utf-8"))

    def test_invalid_summary_stops_before_build_and_preserves_state(self):
        started = self.begin()
        summary = self.base / "summary.md"
        summary.write_text(" \n", encoding="utf-8")
        internal = self.repo / "source.txt"
        invalid_utf8 = self.base / "invalid.md"
        invalid_utf8.write_bytes(b"\xff")
        state_path = self.store_root / self.repo.name / "state" / (started["id"] + ".json")
        original = state_path.read_bytes()
        for supplied, error in ((summary, WorkflowError), (internal, WorkflowError),
                                (invalid_utf8, UnicodeError), (self.base / "missing.md", OSError)):
            with self.subTest(path=supplied), patch("workflow.benchmark.collect") as collect:
                with self.assertRaises(error):
                    runner.finish(self.repo, self.store_root, started["id"], summary_file=supplied)
                collect.assert_not_called()
                self.assertEqual(original, state_path.read_bytes())
                self.assertFalse((Path(started["folder"]) / "report.md").exists())

    def test_new_commit_discards_stale_summary_after_failed_finish(self):
        started = self.begin()
        self.commit_task()
        summary = self.base / "summary.md"
        summary.write_text("Erster Stand, später überholt.", encoding="utf-8")
        with patch("workflow.benchmark.collect", side_effect=WorkflowError("Maven failed")):
            with self.assertRaises(WorkflowError):
                runner.finish(self.repo, self.store_root, started["id"], summary_file=summary)
        summary.unlink()
        (self.repo / "source.txt").write_text("additional fix\n", encoding="utf-8")
        self.git("add", "--", "source.txt")
        self.git("commit", "-m", "additional fix")
        with patch("workflow.benchmark.collect", return_value=METRICS):
            runner.finish(self.repo, self.store_root, started["id"])
        report = (Path(started["folder"]) / "report.md").read_text(encoding="utf-8")
        self.assertNotIn("Erster Stand", report)
        self.assertIn("kein Arbeitsbericht übergeben", report)

    def test_analysis_archives_findings_without_changes_or_task_commit(self):
        self.request.update(task_mode="analysis", task="Nur UserService analysieren, keine Änderungen.")
        started = self.begin()
        start_commit = self.git("rev-parse", "HEAD")
        self.assertEqual("analysis", started["task_mode"])
        findings = "# Offene Findings\n\nUS-001: P2, source.txt:1, konkreter Fehler mit Beleg.\n"
        (self.repo / "findings.md").write_text(findings, encoding="utf-8")
        summary = self.base / "analysis-summary.md"
        summary.write_text("UserService und Aufrufer analysiert; keine Codeänderungen. Grenzen: bestehende Tests.", encoding="utf-8")
        with patch("workflow.benchmark.collect", return_value=METRICS):
            result = runner.finish(self.repo, self.store_root, started["id"], summary_file=summary)
        folder = Path(started["folder"])
        self.assertEqual(start_commit, result["EndCommit"])
        self.assertEqual(0, result["ChangedFiles"])
        self.assertEqual(b"", (folder / "diff.patch").read_bytes())
        self.assertEqual(findings, (folder / "findings.md").read_text(encoding="utf-8"))
        self.assertIn("Auftragsart: Analyse", (folder / "report.md").read_text(encoding="utf-8"))
        self.assertIn("keine Codeänderungen", (folder / "report.md").read_text(encoding="utf-8"))
        self.assertEqual("", self.git("status", "--porcelain"))

    def test_analysis_rejects_commits_without_discarding_work(self):
        self.request["task_mode"] = "analysis"
        started = self.begin()
        self.commit_task()
        commit = self.git("rev-parse", "HEAD")
        state_path = self.store_root / self.repo.name / "state" / (started["id"] + ".json")
        original = state_path.read_bytes()
        with patch("workflow.benchmark.collect") as collect:
            with self.assertRaisesRegex(WorkflowError, "Analysis runs must not contain task commits"):
                runner.finish(self.repo, self.store_root, started["id"])
        collect.assert_not_called()
        self.assertEqual(commit, self.git("rev-parse", "HEAD"))
        self.assertEqual(original, state_path.read_bytes())
        self.assertTrue((self.repo / "binary.dat").exists())

    def test_status_idle_creates_nothing_and_calls_no_external_services(self):
        before = {str(path): path.read_bytes() for path in self.base.rglob("*") if path.is_file()}
        with patch("workflow.runner.run", side_effect=AssertionError("Git call")), \
                patch("workflow.runner.write_json", side_effect=AssertionError("State write")), \
                patch("workflow.runner.store_lock", side_effect=AssertionError("Store lock")), \
                patch("workflow.benchmark.collect", side_effect=AssertionError("Build")), \
                patch("workflow.prompt.improve", side_effect=AssertionError("Provider")):
            result = runner.status(self.repo, self.store_root)
        self.assertEqual("idle", result["status"])
        self.assertFalse(result["active"])
        self.assertIsNone(result["id"])
        self.assertFalse(self.store_root.exists())
        self.assertEqual(before, {str(path): path.read_bytes() for path in self.base.rglob("*") if path.is_file()})

    def test_status_reads_active_run_during_writer_lock_without_changing_files(self):
        started = self.begin("branch")
        store = self.store_root / self.repo.name
        with store_lock(store), patch("workflow.runner.run", side_effect=AssertionError("Git call")):
            before = {str(path): path.read_bytes() for path in store.rglob("*") if path.is_file() and path.name != ".lock"}
            result = runner.status(self.repo, self.store_root)
            self.assertEqual(before, {str(path): path.read_bytes() for path in store.rglob("*") if path.is_file() and path.name != ".lock"})
        self.assertTrue(result["active"])
        self.assertEqual(started["id"], result["active_id"])
        self.assertEqual("active", result["status"])
        self.assertEqual("implementation", result["task_mode"])
        self.assertEqual(started["folder"], result["folder"])

    def test_status_reports_completed_and_aborted_runs_without_selecting_latest(self):
        first = self.begin("branch")
        runner.finish(self.repo, self.store_root, first["id"])
        self.assertEqual("idle", runner.status(self.repo, self.store_root)["status"])
        self.git("switch", "main")
        self.request["branch"] = "second"
        second = self.begin("branch")
        completed = runner.status(self.repo, self.store_root, first["id"])
        self.assertEqual("completed", completed["status"])
        self.assertFalse(completed["active"])
        self.assertEqual(second["id"], completed["active_id"])
        self.assertEqual({"report.md", "findings.md", "diff.patch", "result.json"}, set(completed["artifacts"]))
        runner.abort(self.repo, self.store_root, second["id"])
        self.assertEqual("aborted", runner.status(self.repo, self.store_root, second["id"])["status"])

    def test_status_failure_and_legacy_state_are_read_without_mutation(self):
        with patch("workflow.prompt.improve", side_effect=RuntimeError("provider failed")):
            with self.assertRaises(RuntimeError):
                self.begin("branch,prompt")
        run_id = read_json(self.store_root / self.repo.name / "active.json")["id"]
        state_path = self.store_root / self.repo.name / "state" / (run_id + ".json")
        state = read_json(state_path)
        state.pop("taskMode")  # Previously created runs have no recorded task mode.
        write_json(state_path, state)
        before = state_path.read_bytes()
        result = runner.status(self.repo, self.store_root)
        self.assertEqual("begin-failed", result["status"])
        self.assertTrue(result["error_recorded"])
        self.assertIsNone(result["task_mode"])
        self.assertIn("abort --id", " ".join(result["next_steps"]))
        self.assertEqual(before, state_path.read_bytes())

    def test_status_rejects_invalid_unknown_and_foreign_runs(self):
        started = self.begin("branch")
        for run_id in ("../bad", "", "f" * 32):
            with self.subTest(run_id=run_id), self.assertRaises(WorkflowError):
                runner.status(self.repo, self.store_root, run_id)
        state_path = self.store_root / self.repo.name / "state" / (started["id"] + ".json")
        state = read_json(state_path)
        state["folder"] = str(self.base / "foreign")
        write_json(state_path, state)
        with self.assertRaisesRegex(WorkflowError, "artifact directory"):
            runner.status(self.repo, self.store_root)
        write_json(self.store_root / self.repo.name / "project.json", {"repo": str(self.base / "other")})
        with self.assertRaisesRegex(WorkflowError, "Another repository"):
            runner.status(self.repo, self.store_root)

    def test_status_retries_when_run_completes_between_reads(self):
        started = self.begin("branch")
        active_path = self.store_root / self.repo.name / "active.json"
        active_reads = 0
        real_read = runner.read_json
        def read(path):
            nonlocal active_reads
            if path == active_path:
                active_reads += 1
                if active_reads == 2:
                    active_path.unlink()  # Simulate concurrent writer completing the run.
            return real_read(path)
        with patch("workflow.runner.read_json", side_effect=read):
            result = runner.status(self.repo, self.store_root)
        self.assertEqual("idle", result["status"])
        self.assertIsNone(result["id"])

    def test_branch_only_has_no_maven_or_prompt_dependencies(self):
        with patch("workflow.benchmark.collect") as collect, patch("workflow.prompt.improve") as improve:
            started = self.begin("branch")
            result = runner.finish(self.repo, self.store_root, started["id"])
        collect.assert_not_called()
        improve.assert_not_called()
        self.assertIsNone(result["TestsBefore"])
        self.assertFalse((self.store_root / self.repo.name / "results.csv").exists())

    def test_complete_module_order_and_prompt_artifact(self):
        export_file = self.base / "export.json"
        write_json(export_file, self.export())
        self.request.update(modules="complete", session_id="ses_test")
        order = []
        real_create = branch.create
        def fake_collect(*args):
            self.assertEqual("main", branch.current(self.repo))
            order.append("benchmark")
            return METRICS
        def fake_improve(task, output, *args):
            self.assertEqual(self.request["task"], task)
            order.append("prompt")
            (output / "improved-prompt.md").write_text("structured", encoding="utf-8")
            return "structured"
        def create(*args):
            order.append("branch")
            return real_create(*args)
        with patch("workflow.benchmark.collect", side_effect=fake_collect), patch("workflow.prompt.improve", side_effect=fake_improve), patch("workflow.branch.create", side_effect=create):
            prepared = runner.prepare(self.repo, self.store_root, self.request, CATALOG, export_file)
            started = runner.begin(self.repo, self.store_root, None, CATALOG, export_file, prepared["id"])
        self.assertEqual(["benchmark", "prompt", "branch"], order)
        self.assertEqual("structured", started["task"])
        self.assertEqual(list(runner.MODULES), started["modules"])

    def test_prompt_failure_does_not_create_branch_and_can_abort(self):
        with patch("workflow.prompt.improve", side_effect=RuntimeError("provider failed")):
            with self.assertRaisesRegex(RuntimeError, "provider failed"):
                self.begin("branch,prompt")
        self.assertEqual("main", branch.current(self.repo))
        store = self.store_root / self.repo.name
        run_id = read_json(store / "active.json")["id"]
        state = read_json(store / "state" / (run_id + ".json"))
        self.assertEqual("begin-failed", state["status"])
        with self.assertRaises(WorkflowError):
            runner.finish(self.repo, self.store_root, run_id)
        runner.abort(self.repo, self.store_root, run_id)
        self.assertFalse((store / "active.json").exists())
        self.assertTrue(Path(state["folder"]).exists())

    def test_reject_dirty_begin(self):
        (self.repo / "source.txt").write_text("dirty")
        with self.assertRaisesRegex(WorkflowError, "not clean"):
            self.begin()

    def test_reject_existing_local_branch(self):
        self.git("branch", "feature/tests")
        with self.assertRaisesRegex(WorkflowError, "already exists"):
            self.begin()

    def test_reject_existing_remote_branch_without_fetching(self):
        self.git("update-ref", "refs/remotes/origin/feature/tests", self.git("rev-parse", "HEAD"))
        with self.assertRaisesRegex(WorkflowError, "already exists"):
            self.begin()

    def test_reject_wrong_start_branch(self):
        self.git("switch", "-c", "other")
        with self.assertRaisesRegex(WorkflowError, "main"):
            self.begin()

    def test_reject_bad_request_before_mutations(self):
        for key, value in (("branch", "../bad"), ("target_class", "../UserService"),
                           ("model", "invented"), ("task", " "), ("modules", "reviewer"), ("task_mode", "reviewer")):
            with self.subTest(key=key):
                request = dict(self.request, **{key: value})
                with self.assertRaises(WorkflowError):
                    runner.begin(self.repo, self.store_root, request, CATALOG)
                self.assertEqual("main", branch.current(self.repo))

    def test_reject_store_inside_repository(self):
        with self.assertRaisesRegex(WorkflowError, "outside"):
            runner.begin(self.repo, self.repo / "benchmarks", self.request, CATALOG)

    def test_reject_wrong_finish_branch(self):
        started = self.begin()
        self.git("switch", "main")
        with self.assertRaisesRegex(WorkflowError, "exactly"):
            runner.finish(self.repo, self.store_root, started["id"])

    def test_reject_uncommitted_finish(self):
        started = self.begin()
        (self.repo / "other.md").write_text("uncommitted")
        with self.assertRaisesRegex(WorkflowError, "not clean"):
            runner.finish(self.repo, self.store_root, started["id"])

    def test_reject_tracked_findings_even_when_clean(self):
        started = self.begin()
        (self.repo / "findings.md").write_text("tracked")
        self.git("add", "--", "findings.md")
        self.git("commit", "-m", "bad findings")
        with self.assertRaisesRegex(WorkflowError, "untracked"):
            runner.finish(self.repo, self.store_root, started["id"])

    def test_missing_usage_session_stops_before_build_or_branch(self):
        with patch("workflow.benchmark.collect") as collect:
            with self.assertRaisesRegex(WorkflowError, "session_id"):
                self.begin("complete")
        collect.assert_not_called()
        self.assertEqual("main", branch.current(self.repo))

    def test_finish_failed_build_retains_state_and_findings(self):
        started = self.begin()
        self.commit_task()
        (self.repo / "findings.md").write_text("retain me")
        with patch("workflow.benchmark.collect", side_effect=WorkflowError("Maven failed")):
            with self.assertRaisesRegex(WorkflowError, "Maven failed"):
                runner.finish(self.repo, self.store_root, started["id"])
        self.assertTrue((self.repo / "findings.md").exists())
        state_path = self.store_root / self.repo.name / "state" / (started["id"] + ".json")
        cutoff = read_json(state_path)["end"]
        saved_status = runner.status(self.repo, self.store_root)
        self.assertEqual("finishing", saved_status["status"])
        self.assertIn("Falls der Workflow-Prozess noch läuft", " ".join(saved_status["next_steps"]))
        with patch("workflow.benchmark.collect", return_value=METRICS):
            runner.finish(self.repo, self.store_root, started["id"])
        self.assertEqual(cutoff, read_json(state_path)["end"])

    def test_baseline_build_generated_changes_are_rejected(self):
        def collect(*args):
            (self.repo / "source.txt").write_text("generated change")
            return METRICS
        with patch("workflow.benchmark.collect", side_effect=collect):
            with self.assertRaisesRegex(WorkflowError, "not clean"):
                runner.begin(self.repo, self.store_root, self.request, CATALOG)
        self.assertEqual("main", branch.current(self.repo))

    def test_metrics_counts_and_percentages(self):
        self.write_reports()
        self.assertEqual(METRICS, benchmark.metrics(self.repo, "UserService"))

    def test_metrics_ambiguity_resolved_by_java_package(self):
        self.write_reports(extra=True)
        source = self.repo / "src/main/java/de/app/UserService.java"
        source.parent.mkdir(parents=True)
        source.write_text("package de.app;\nclass UserService {}")
        self.assertEqual(75, benchmark.metrics(self.repo, "UserService")["lineCoverage"])

    def test_metrics_ambiguity_without_unique_source_rejected(self):
        self.write_reports(extra=True)
        with self.assertRaisesRegex(WorkflowError, "Ambiguous"):
            benchmark.metrics(self.repo, "UserService")

    def test_missing_target_and_reports_rejected(self):
        with self.assertRaisesRegex(WorkflowError, "report not found"):
            benchmark.metrics(self.repo, "UserService")
        self.write_reports()
        with self.assertRaisesRegex(WorkflowError, "not uniquely"):
            benchmark.metrics(self.repo, "Other")
        (self.repo / "target/surefire-reports/TEST-Test.xml").unlink()
        with self.assertRaisesRegex(WorkflowError, "Surefire"):
            benchmark.metrics(self.repo, "UserService")

    def test_test_report_failure_rejected(self):
        self.write_reports(failures=1)
        with self.assertRaisesRegex(WorkflowError, "failures/errors"):
            benchmark.metrics(self.repo, "UserService")

    def test_zero_branches_means_full_coverage(self):
        self.write_reports(class_name="Empty")
        path = self.repo / "target/site/jacoco/jacoco.csv"
        path.write_text(path.read_text().replace(",1,3,1,1", ",0,0,0,0"))
        result = benchmark.metrics(self.repo, "Empty")
        self.assertEqual(100.0, result["lineCoverage"])
        self.assertEqual(100.0, result["branchCoverage"])

    def test_current_csv_preserves_runs_and_rejects_different_schema(self):
        path = self.base / "results.csv"
        benchmark.append_csv(path, {"Id": "first", "Task": "original task", "TestsBefore": 1,
                                    "InputTokens": None, "EstimatedCostUSD": None})
        benchmark.append_csv(path, {"Id": "second", "Task": "new\nline", "TestsBefore": 2,
                                    "InputTokens": 10, "EstimatedCostUSD": None})
        benchmark.append_csv(path, {"Id": "second", "Task": "duplicate", "TestsBefore": 2,
                                    "InputTokens": 10, "EstimatedCostUSD": None})
        with path.open(encoding="utf-8", newline="") as handle:
            rows = list(csv.DictReader(handle))
        self.assertEqual(2, len(rows))
        self.assertEqual("original task", rows[0]["Task"])
        self.assertEqual("", rows[0]["InputTokens"])
        self.assertEqual("new\nline", rows[1]["Task"])
        self.assertEqual("", rows[1]["EstimatedCostUSD"])
        original_bytes = path.read_bytes()
        with self.assertRaisesRegex(WorkflowError, "different schema"):
            benchmark.append_csv(path, {"Id": "other", "Task": "different schema"})
        self.assertEqual(original_bytes, path.read_bytes())

    def test_csv_failure_keeps_findings_and_is_retryable(self):
        started = self.begin()
        self.commit_task()
        (self.repo / "findings.md").write_text("valuable")
        summary = self.base / "summary.md"
        summary.write_text("Umsetzung ä wurde abgeschlossen.", encoding="utf-8-sig")
        with patch("workflow.benchmark.collect", return_value=METRICS), patch("workflow.benchmark.append_csv", side_effect=OSError("locked")):
            with self.assertRaisesRegex(OSError, "locked"):
                runner.finish(self.repo, self.store_root, started["id"], summary_file=summary)
        self.assertTrue((self.repo / "findings.md").exists())
        summary.unlink()
        with patch("workflow.benchmark.collect", return_value=METRICS):
            runner.finish(self.repo, self.store_root, started["id"])
        self.assertFalse((self.repo / "findings.md").exists())
        folder = Path(started["folder"])
        self.assertIn("Umsetzung ä wurde abgeschlossen.", (folder / "report.md").read_text(encoding="utf-8"))
        self.assertEqual("valuable", (folder / "findings.md").read_text())
        with (self.store_root / self.repo.name / "results.csv").open(encoding="utf-8", newline="") as handle:
            self.assertEqual(1, len(list(csv.DictReader(handle))))

    def test_lock_rejects_concurrent_store_access(self):
        with store_lock(self.store_root):
            with self.assertRaisesRegex(WorkflowError, "locked"):
                with store_lock(self.store_root, timeout=0.05):
                    self.fail("Should not acquire lock twice")

    def test_cli_begin_finish_preserves_task_as_data(self):
        request = self.base / "request.json"
        write_json(request, dict(self.request, modules="branch", task_mode="analysis"))
        script = Path(__file__).resolve().parents[1] / "start.py"
        command = [sys.executable, "-B", str(script), "--repo", str(self.repo),
                   "--store-root", str(self.store_root)]
        idle = subprocess.run([*command, "status"], capture_output=True, text=True, encoding="utf-8")
        self.assertEqual(0, idle.returncode, idle.stderr)
        self.assertEqual("idle", json.loads(idle.stdout)["status"])
        self.assertFalse(self.store_root.exists())
        before = subprocess.run([*command, "begin", "--request", str(request)], capture_output=True, text=True, encoding="utf-8")
        self.assertEqual(0, before.returncode, before.stderr)
        started = json.loads(before.stdout)
        self.assertEqual(self.request["task"], started["task"])
        self.assertEqual("analysis", started["task_mode"])
        summary = self.base / "summary with spaces.md"
        explanation = 'Änderungen geprüft; $(bad) `bad` "quoted"\nWeitere Erklärung.'
        summary.write_text(explanation, encoding="utf-8-sig")
        no_findings = "# Offene Findings\n\nKeine weiteren offenen Findings festgestellt.\n"
        (self.repo / "findings.md").write_text(no_findings, encoding="utf-8")
        after = subprocess.run([*command, "finish", "--summary-file", str(summary), "--id", started["id"]], capture_output=True, text=True, encoding="utf-8")
        self.assertEqual(0, after.returncode, after.stderr)
        self.assertEqual("feature/tests", json.loads(after.stdout)["Branch"])
        folder = Path(started["folder"])
        self.assertIn(explanation, (folder / "report.md").read_text(encoding="utf-8"))
        self.assertEqual(no_findings, (folder / "findings.md").read_text(encoding="utf-8"))
        self.assertEqual("", self.git("status", "--porcelain"))
        status = subprocess.run([*command, "status", "--id", started["id"]], capture_output=True, text=True, encoding="utf-8")
        self.assertEqual(0, status.returncode, status.stderr)
        self.assertEqual("completed", json.loads(status.stdout)["status"])

    def test_fresh_reports_rejects_wrong_java_before_build(self):
        wrapper = self.repo / ("mvnw.cmd" if os.name == "nt" else "mvnw")
        wrapper.write_text("stub")
        (self.repo / "pom.xml").write_text("stub")
        with patch("workflow.benchmark.run", return_value="Java version: 21.0.1"), patch("workflow.benchmark.subprocess.run") as build:
            with self.assertRaisesRegex(WorkflowError, "Java 25"):
                benchmark.fresh_reports(self.repo)
        build.assert_not_called()

    def test_fresh_reports_checks_each_process_exit(self):
        wrapper = self.repo / ("mvnw.cmd" if os.name == "nt" else "mvnw")
        wrapper.write_text("stub")
        (self.repo / "pom.xml").write_text("stub")
        for codes in ((1,), (0, 2)):
            with self.subTest(codes=codes), patch("workflow.benchmark.run", return_value="Java version: 25.0.4.1"), patch("workflow.benchmark.subprocess.run", side_effect=[Mock(returncode=code) for code in codes]) as build:
                with self.assertRaisesRegex(WorkflowError, "Maven"):
                    benchmark.fresh_reports(self.repo)
                self.assertEqual(len(codes), build.call_count)

    def test_benchmark_evidence_reconstructs_counts_without_xml_properties(self):
        self.write_reports()
        xml = self.repo / "target/surefire-reports/TEST-Test.xml"
        xml.write_text('<testsuite name="Example" tests="3" failures="0" errors="0" skipped="1" time="0.123">'
                       '<properties><property name="secret" value="DO_NOT_ARCHIVE"/></properties></testsuite>')
        evidence = self.base / "benchmark-before.json"
        checks = {"javaMajor": 25, "checks": [{"arguments": ["clean", "test"], "exitCode": 0}]}
        with patch("workflow.benchmark.fresh_reports", return_value=checks):
            values = benchmark.collect(self.repo, "UserService", evidence)
        archived = read_json(evidence)
        self.assertNotIn("DO_NOT_ARCHIVE", evidence.read_text())
        self.assertEqual(sum(s["tests"] for s in archived["surefire"]), values["tests"])
        row = archived["jacoco"]
        self.assertEqual(int(row["LINE_COVERED"]) / (int(row["LINE_MISSED"]) + int(row["LINE_COVERED"])) * 100,
                         values["lineCoverage"])
        self.assertEqual(checks, archived["build"])

    def test_prepared_run_rejects_changed_commit_before_prompt_or_branch(self):
        with patch("workflow.benchmark.collect", return_value=METRICS):
            prepared = runner.prepare(self.repo, self.store_root, self.request, CATALOG)
        self.git("commit", "--allow-empty", "-m", "changed baseline")
        with patch("workflow.prompt.improve") as improve, self.assertRaisesRegex(WorkflowError, "changed after prepare"):
            runner.begin(self.repo, self.store_root, None, CATALOG, prepared_id=prepared["id"])
        improve.assert_not_called()
        self.assertEqual("main", branch.current(self.repo))


def assistant(message_id="msg_one", input_tokens=100, completed=True, steps=True):
    values = {"cost": 0.012, "tokens": {"input": input_tokens, "output": 20, "reasoning": 5,
                                          "cache": {"read": 40, "write": 3}}}
    info = {"id": message_id, "role": "assistant", "providerID": "openai", "modelID": "test-model",
            "time": {"created": 1000}, **values}
    if completed:
        info["time"]["completed"] = 5000
    parts = [{"type": "reasoning", "text": "never persist secret text", "time": {"start": 1000, "end": 2500}}]
    if steps:
        parts += [{"id": "prt_one", "type": "step-finish", **values}]
    return {"info": info, "parts": parts}


class UsageCase(RepoCase):
    # Inherit fixtures only, not tests: below class overrides discovery with load_tests.
    def snap(self, messages):
        return usage.snapshot(self.export(messages), self.repo, "ses_test", captured_ms=6000 if messages else 0)

    def bridge_export(self, captured_ms=6000, messages=None):
        return self.export(messages) | {"workflowUsage": {
            "version": 1, "source": "OpenCode SDK", "sessionId": "ses_test", "capturedMs": captured_ms}}

    def test_active_server_export_never_uses_local_cli(self):
        export_path = self.base / "bridge.json"
        write_json(export_path, self.bridge_export(messages=[assistant()]))
        with patch("workflow.usage.run") as local_cli, patch("workflow.usage.time.time", return_value=7):
            measured = usage.capture(self.repo, "ses_test", export_path)
        local_cli.assert_not_called()
        self.assertEqual("OpenCode SDK", measured["source"])
        self.assertEqual(6000, measured["capturedMs"])
        self.assertNotIn("secret text", json.dumps(measured))

    def test_active_server_snapshot_timestamp_clips_pending_reasoning(self):
        active = assistant(completed=False, steps=False)
        active["parts"][0]["time"].pop("end")
        measured = usage.snapshot(self.bridge_export(captured_ms=3500, messages=[active]), self.repo, "ses_test")
        self.assertEqual(2.5, measured["messages"]["msg_one"]["ReasoningSeconds"])

    def test_stale_future_or_invalid_active_server_export_rejected(self):
        export_path = self.base / "bridge.json"
        for bridge in (self.bridge_export(captured_ms=1), self.bridge_export(captured_ms=999999),
                       self.export() | {"workflowUsage": "invalid"}):
            write_json(export_path, bridge)
            with patch("workflow.usage.time.time", return_value=400), self.assertRaises(WorkflowError):
                usage.capture(self.repo, "ses_test", export_path)
        malformed = self.bridge_export()
        malformed["workflowUsage"]["sessionId"] = "ses_other"
        with self.assertRaisesRegex(WorkflowError, "Invalid active-server"):
            usage.snapshot(malformed, self.repo)

    def test_finish_requires_new_snapshot_from_same_transport(self):
        before = usage.snapshot(self.bridge_export(), self.repo, "ses_test")
        with self.assertRaisesRegex(WorkflowError, "fresh"):
            usage.difference(before, before)
        with self.assertRaisesRegex(WorkflowError, "fresh"):
            usage.difference(before, self.snap([]))
        after = usage.snapshot(self.bridge_export(captured_ms=7000, messages=[assistant()]), self.repo, "ses_test")
        self.assertEqual(100, usage.difference(before, after)["InputTokens"])

    def test_usage_partial_intervals_exclude_pre_begin_time(self):
        active = assistant(completed=False, steps=False)
        active["parts"][0]["time"].pop("end")
        finished = assistant()
        finished["parts"][0]["time"]["end"] = 4500
        before = usage.snapshot(self.export([active]), self.repo, "ses_test", captured_ms=3500)
        after = usage.snapshot(self.export([finished]), self.repo, "ses_test", captured_ms=6000)
        result = usage.difference(before, after)
        self.assertEqual(1.0, result["ReasoningSeconds"])
        self.assertEqual(1.5, result["MessageElapsedSeconds"])

    def test_usage_step_and_message_totals_are_not_double_counted(self):
        result = usage.difference(self.snap([]), self.snap([assistant()]))
        self.assertEqual(100, result["InputTokens"])
        self.assertEqual(20, result["OutputTokens"])
        self.assertEqual(5, result["ReasoningTokens"])
        self.assertEqual(40, result["CacheReadTokens"])
        self.assertEqual(3, result["CacheWriteTokens"])
        self.assertEqual(1, result["Requests"])
        self.assertEqual(1.5, result["ReasoningSeconds"])
        self.assertEqual(4, result["MessageElapsedSeconds"])
        self.assertAlmostEqual(0.012, result["EstimatedCostUSD"])
        self.assertNotIn("secret text", json.dumps(self.snap([assistant()])))

    def test_usage_existing_session_delta_and_active_message(self):
        old = assistant("msg_old")
        active = assistant("msg_active", completed=False, steps=False)
        before = self.snap([old, active])
        after = self.snap([old, assistant("msg_active", completed=False)])
        result = usage.difference(before, after)
        self.assertEqual(100, result["InputTokens"])
        self.assertEqual(1, result["Requests"])
        self.assertEqual(1, result["PendingMessages"])

    def test_usage_multiple_steps_and_retries(self):
        msg = assistant()
        msg["parts"].append({"id": "prt_two", "type": "step-finish", "cost": 0.02,
                             "tokens": {"input": 10, "output": 2, "reasoning": 1, "cache": {"read": 0, "write": 0}}})
        msg["parts"].append({"type": "retry", "attempt": 1})
        result = usage.difference(self.snap([]), self.snap([msg]))
        self.assertEqual(2, result["Requests"])
        self.assertEqual(1, result["RetryEvents"])
        self.assertEqual(110, result["InputTokens"])

    def test_usage_missing_fields_are_unavailable(self):
        msg = assistant()
        del msg["parts"][1]["tokens"]["reasoning"]
        del msg["parts"][1]["cost"]
        result = usage.difference(self.snap([]), self.snap([msg]))
        self.assertIsNone(result["ReasoningTokens"])
        self.assertIsNone(result["EstimatedCostUSD"])
        self.assertEqual(100, result["InputTokens"])

    def test_usage_unchanged_old_unknown_counters_do_not_poison_new_delta(self):
        msg = assistant("msg_old")
        del msg["parts"][1]["cost"]
        result = usage.difference(self.snap([msg]), self.snap([msg, assistant("msg_new")]))
        self.assertAlmostEqual(0.012, result["EstimatedCostUSD"])

    def test_unknown_counters_on_changed_message_do_not_become_zero(self):
        before = assistant(input_tokens=100)
        after = assistant(input_tokens=101)
        for msg in (before, after):
            msg["info"]["retryEvents"] = None
            msg["parts"][1]["cost"] = None
        result = usage.difference(self.snap([before]), self.snap([after]))
        self.assertEqual(1, result["InputTokens"])
        self.assertIsNone(result["EstimatedCostUSD"])
        self.assertIsNone(result["RetryEvents"])

    def test_usage_fallback_completed_assistant_and_explicit_zero_cost(self):
        msg = assistant(steps=False)
        msg["info"]["cost"] = 0
        result = usage.difference(self.snap([]), self.snap([msg]))
        self.assertEqual(1, result["Requests"])
        self.assertEqual(0, result["EstimatedCostUSD"])

    def test_v2_completed_compaction_has_tokens_but_no_invented_duration(self):
        msg = assistant(steps=False)
        msg["info"]["completed"] = True
        msg["info"]["retryEvents"] = None
        msg["info"]["time"].pop("completed")
        msg["parts"] = []
        result = usage.difference(self.snap([]), self.snap([msg]))
        self.assertEqual(1, result["Requests"])
        self.assertEqual(100, result["InputTokens"])
        self.assertEqual(0, result["PendingMessages"])
        self.assertIsNone(result["MessageElapsedSeconds"])
        self.assertIsNone(result["RetryEvents"])
        self.assertIsNone(result["ReasoningSeconds"])

    def test_usage_wrong_session_project_and_shape_rejected(self):
        for data in ({}, self.export() | {"info": {"id": "ses_other", "directory": str(self.repo)}},
                     self.export() | {"info": {"id": "ses_test", "directory": str(self.base)}}):
            with self.subTest(data=data), self.assertRaises(WorkflowError):
                usage.snapshot(data, self.repo, "ses_test")

    def test_usage_counter_decrease_and_disappearing_messages_rejected(self):
        before = self.snap([assistant()])
        with self.assertRaisesRegex(WorkflowError, "backwards"):
            usage.difference(before, self.snap([assistant(input_tokens=1)]))
        with self.assertRaisesRegex(WorkflowError, "disappeared"):
            usage.difference(before, self.snap([]))

    def test_usage_invalid_numbers_become_unavailable(self):
        for invalid in (float("nan"), -1, "100", True):
            self.assertIsNone(usage.number(invalid))

    def test_complete_integration_archives_usage_and_csv(self):
        from unittest.mock import Mock
        from prompt.models import ImprovedPrompt
        client = Mock()
        client.chat.return_value = ImprovedPrompt(goal="Improved task", scope=[], requirements=[],
            non_goals=[], verification=[], uncertainties=[]).model_dump_json()
        before_path, after_path = self.base / "before.json", self.base / "after.json"
        write_json(before_path, self.bridge_export(captured_ms=time.time() * 1000, messages=[assistant("msg_old")]))
        self.request.update(modules="complete", session_id="ses_test", prompt_provider="ollama", prompt_model="fixture-improver")
        with patch("workflow.benchmark.collect", return_value=METRICS), patch("prompt.llm_client_factory.LlmClientFactory.create", return_value=client):
            prepared = runner.prepare(self.repo, self.store_root, self.request, CATALOG, before_path)
            write_json(before_path, self.bridge_export(captured_ms=time.time() * 1000, messages=[assistant("msg_old")]))
            started = runner.begin(self.repo, self.store_root, None, CATALOG, before_path, prepared["id"])
        self.commit_task()
        write_json(after_path, self.bridge_export(captured_ms=time.time() * 1000,
                                                  messages=[assistant("msg_old"), assistant("msg_new")]))
        with patch("workflow.benchmark.collect", return_value=METRICS):
            result = runner.finish(self.repo, self.store_root, started["id"], usage_export=after_path)
        self.assertEqual(100, result["InputTokens"])
        self.assertEqual("openai/test-model", result["ActualModels"])
        self.assertEqual("ollama", result["PromptProvider"])
        self.assertEqual("fixture-improver", result["PromptModel"])
        self.assertGreaterEqual(result["PromptDurationSeconds"], 0)
        self.assertIn("fixture-improver", (Path(started["folder"]) / "report.md").read_text(encoding="utf-8"))
        self.assertTrue((Path(started["folder"]) / "usage.json").is_file())
        self.assertEqual("OpenCode SDK", read_json(Path(started["folder"]) / "usage-before.json")["source"])

    def test_complete_analysis_keeps_original_scope_despite_improver_suggestions(self):
        from prompt.models import ImprovedPrompt
        client = Mock()
        client.chat.return_value = ImprovedPrompt(goal="Suggested code fix outside original scope", scope=[], requirements=[],
            non_goals=[], verification=[], uncertainties=[]).model_dump_json()
        before_path, after_path = self.base / "before.json", self.base / "after.json"
        write_json(before_path, self.bridge_export(captured_ms=time.time() * 1000))
        self.request.update(modules="complete", task_mode="analysis", task="Nur analysieren, keine Codeänderungen.",
                            session_id="ses_test", prompt_provider="ollama", prompt_model="fixture-improver")
        with patch("workflow.benchmark.collect", return_value=METRICS), patch("prompt.llm_client_factory.LlmClientFactory.create", return_value=client):
            prepared = runner.prepare(self.repo, self.store_root, self.request, CATALOG, before_path)
            prepared_status = runner.status(self.repo, self.store_root)
            self.assertEqual("prepared", prepared_status["status"])
            self.assertIn("frischen Usage-Snapshot", " ".join(prepared_status["next_steps"]))
            write_json(before_path, self.bridge_export(captured_ms=time.time() * 1000))
            started = runner.begin(self.repo, self.store_root, None, CATALOG, before_path, prepared["id"])
        self.assertEqual("analysis", started["task_mode"])
        self.assertEqual(self.request["task"], started["originalTask"])
        active_status = runner.status(self.repo, self.store_root)
        self.assertIn("Vor finish einen frischen Usage-Snapshot", " ".join(active_status["next_steps"]))
        (self.repo / "findings.md").write_text("# Offene Findings\n\nUS-001: Problem mit Beleg.\n", encoding="utf-8")
        summary = self.base / "summary.md"
        summary.write_text("Analyse durchgeführt; keine Codeänderungen.", encoding="utf-8")
        write_json(after_path, self.bridge_export(captured_ms=time.time() * 1000, messages=[assistant("msg_new")]))
        with patch("workflow.benchmark.collect", return_value=METRICS):
            result = runner.finish(self.repo, self.store_root, started["id"], usage_export=after_path, summary_file=summary)
        self.assertEqual(100, result["InputTokens"])
        self.assertEqual(result["StartCommit"], result["EndCommit"])
        self.assertEqual(0, result["ChangedFiles"])
        self.assertEqual(b"", (Path(started["folder"]) / "diff.patch").read_bytes())
        self.assertEqual("", self.git("status", "--porcelain"))

    def test_module_selections_use_same_csv_schema(self):
        first = self.begin()
        self.commit_task()
        with patch("workflow.benchmark.collect", return_value=METRICS):
            first_result = runner.finish(self.repo, self.store_root, first["id"])
        self.git("switch", "main")
        before_path, after_path = self.base / "before.json", self.base / "after.json"
        write_json(before_path, self.export([]))
        write_json(after_path, self.export([assistant()]))
        self.request.update(modules="branch,benchmark,usage", branch="with_usage", session_id="ses_test")
        with patch("workflow.benchmark.collect", return_value=METRICS):
            prepared = runner.prepare(self.repo, self.store_root, self.request, CATALOG, before_path)
            second = runner.begin(self.repo, self.store_root, None, CATALOG, before_path, prepared["id"])
        self.commit_task()
        with patch("workflow.benchmark.collect", return_value=METRICS):
            second_result = runner.finish(self.repo, self.store_root, second["id"], usage_export=after_path)
        self.assertEqual(list(first_result), list(second_result))
        with (self.store_root / self.repo.name / "results.csv").open(encoding="utf-8", newline="") as handle:
            rows = list(csv.DictReader(handle))
        self.assertEqual(2, len(rows))
        self.assertEqual("", rows[0]["InputTokens"])
        self.assertEqual("100", rows[1]["InputTokens"])

    def test_preflight_cannot_start_run_and_fresh_snapshots_share_duration_boundaries(self):
        self.request.update(modules="branch,benchmark,usage", session_id="ses_test")
        preflight = self.base / "preflight.json"
        write_json(preflight, self.bridge_export(captured_ms=time.time() * 1000))
        with patch("workflow.benchmark.collect", return_value=METRICS):
            prepared = runner.prepare(self.repo, self.store_root, self.request, CATALOG, preflight)
        self.assertEqual("main", branch.current(self.repo))
        with patch("workflow.prompt.improve") as improve, self.assertRaisesRegex(WorkflowError, "AFTER baseline"):
            runner.begin(self.repo, self.store_root, None, CATALOG, preflight, prepared["id"])
        improve.assert_not_called()
        start_ms = time.time() * 1000
        fresh = self.base / "fresh.json"
        write_json(fresh, self.bridge_export(captured_ms=start_ms))
        started = runner.begin(self.repo, self.store_root, None, CATALOG, fresh, prepared["id"])
        self.commit_task()
        end_ms = time.time() * 1000
        after = self.base / "after.json"
        write_json(after, self.bridge_export(captured_ms=end_ms))
        with patch("workflow.benchmark.collect", return_value=METRICS):
            result = runner.finish(self.repo, self.store_root, started["id"], usage_export=after)
        self.assertEqual(usage.timestamp(start_ms), result["UsageStartUTC"])
        self.assertEqual(usage.timestamp(end_ms), result["UsageEndUTC"])
        self.assertAlmostEqual(result["DurationSeconds"], result["UsageWindowSeconds"], delta=.051)
        report = (Path(started["folder"]) / "report.md").read_text(encoding="utf-8")
        self.assertIn("OpenCode meldet 0 USD; tatsächliche Kosten unbekannt", report)

    def test_newly_visible_message_time_is_clipped_to_run_even_if_it_started_earlier(self):
        before = usage.snapshot(self.export([]), self.repo, "ses_test", captured_ms=3500)
        after = usage.snapshot(self.export([assistant()]), self.repo, "ses_test", captured_ms=6000)
        result = usage.difference(before, after)
        self.assertEqual(1.5, result["MessageElapsedSeconds"])
        self.assertEqual(0, result["ReasoningSeconds"])
        self.assertEqual(2.5, result["UsageWindowSeconds"])

    def test_invalid_reasoning_endpoint_is_unknown_instead_of_an_open_interval(self):
        for endpoint in (None, "invalid", -1, True):
            with self.subTest(endpoint=endpoint):
                message = assistant()
                reasoning = next(part for part in message["parts"] if part["type"] == "reasoning")
                reasoning["time"]["end"] = endpoint
                self.assertIsNone(usage.difference(self.snap([]), self.snap([message]))["ReasoningSeconds"])

    def test_first_run_csv_is_preserved_when_new_schema_is_appended(self):
        fields = {"Id": "first", "MessageElapsedSeconds": 225.205, "EstimatedCostUSD": 0,
                  "CostStatus": "reported-zero-actual-unknown", "UsageStartUTC": None, "UsageEndUTC": None,
                  "UsageWindowSeconds": None, "PromptProvider": None, "PromptModel": None,
                  "PromptDurationSeconds": None}
        path = self.base / "legacy-results.csv"
        path.write_text("Id,InferenceSeconds,EstimatedCostUSD\nfirst,225.205,0\n", encoding="utf-8")
        benchmark.append_csv(path, dict(fields, Id="second"))
        with path.open(encoding="utf-8", newline="") as f:
            rows = list(csv.DictReader(f))
        self.assertEqual(["first", "second"], [row["Id"] for row in rows])
        self.assertEqual("225.205", rows[0]["MessageElapsedSeconds"])
        self.assertEqual("", rows[0]["UsageStartUTC"])
        self.assertEqual("reported-zero-actual-unknown", rows[1]["CostStatus"])


    def test_failed_finish_keeps_known_usage_with_original_snapshot_bounds(self):
        before_path, after_path = self.base / "before.json", self.base / "after.json"
        write_json(before_path, self.export([]))
        write_json(after_path, self.export([assistant()]))
        self.request.update(modules="branch,benchmark,usage", session_id="ses_test")
        with patch("workflow.benchmark.collect", return_value=METRICS):
            prepared = runner.prepare(self.repo, self.store_root, self.request, CATALOG, before_path)
            started = runner.begin(self.repo, self.store_root, None, CATALOG, before_path, prepared["id"])
        with patch("workflow.benchmark.collect", side_effect=WorkflowError("build broken")):
            with self.assertRaises(WorkflowError):
                runner.finish(self.repo, self.store_root, started["id"], usage_export=after_path)
        folder = Path(started["folder"])
        partial = read_json(folder / "result.json")
        self.assertEqual(100, partial["InputTokens"])
        self.assertIsNotNone(partial["UsageEndUTC"])
        with patch("workflow.usage.capture", side_effect=AssertionError("unexpected new capture")):
            runner.abort(self.repo, self.store_root, started["id"], "Prüfung beendet", True)
        stopped = read_json(folder / "result.json")
        self.assertEqual(partial["UsageEndUTC"], stopped["UsageEndUTC"])
        self.assertEqual(partial["UsageWindowSeconds"], stopped["UsageWindowSeconds"])
        self.assertEqual(100, stopped["InputTokens"])
        self.assertIsNone(stopped["TestsAfter"])

    def test_invalid_finish_snapshot_can_be_replaced_without_duplicate_csv_row(self):
        before_path, after_path = self.base / "before.json", self.base / "after.json"
        self.request.update(modules="branch,benchmark,usage", session_id="ses_test")
        write_json(before_path, self.bridge_export(captured_ms=time.time() * 1000))
        with patch("workflow.benchmark.collect", return_value=METRICS):
            prepared = runner.prepare(self.repo, self.store_root, self.request, CATALOG, before_path)
            write_json(before_path, self.bridge_export(captured_ms=time.time() * 1000))
            started = runner.begin(self.repo, self.store_root, None, CATALOG, before_path, prepared["id"])
        with self.assertRaisesRegex(WorkflowError, "fresh"):
            runner.finish(self.repo, self.store_root, started["id"], usage_export=before_path)
        folder = Path(started["folder"])
        failed = read_json(folder / "result.json")
        self.assertIsNone(failed["InputTokens"])
        self.assertEqual("finish-usage", failed["LastFailurePhase"])
        write_json(after_path, self.bridge_export(captured_ms=time.time() * 1000, messages=[assistant()]))
        with patch("workflow.benchmark.collect", return_value=METRICS):
            result = runner.finish(self.repo, self.store_root, started["id"], usage_export=after_path)
        self.assertEqual("completed", result["Outcome"])
        self.assertEqual(100, result["InputTokens"])
        with (self.store_root / self.repo.name / "results.csv").open(encoding="utf-8", newline="") as handle:
            self.assertEqual(1, len(list(csv.DictReader(handle))))

    def test_abort_before_final_usage_capture_keeps_tokens_unknown(self):
        before_path = self.base / "before.json"
        write_json(before_path, self.export([assistant()]))
        self.request.update(modules="branch,benchmark,usage", session_id="ses_test")
        with patch("workflow.benchmark.collect", return_value=METRICS):
            prepared = runner.prepare(self.repo, self.store_root, self.request, CATALOG, before_path)
            started = runner.begin(self.repo, self.store_root, None, CATALOG, before_path, prepared["id"])
        with patch("workflow.usage.capture", side_effect=AssertionError("Usage called on abort")):
            runner.abort(self.repo, self.store_root, started["id"])
        result = read_json(Path(started["folder"]) / "result.json")
        self.assertIsNone(result["InputTokens"])
        self.assertIsNone(result["EstimatedCostUSD"])
        self.assertEqual("unavailable", result["CostStatus"])
        self.assertIsNone(result["UsageWindowSeconds"])


class OutcomeCase(RepoCase):
    def saved(self, run_id=None):
        store = self.store_root / self.repo.name
        run_id = run_id or read_json(store / "active.json")["id"]
        return store, store / "state" / (run_id + ".json"), store / "runs" / run_id

    def rows(self):
        with (self.store_root / self.repo.name / "results.csv").open(encoding="utf-8", newline="") as handle:
            return list(csv.DictReader(handle))

    def test_failed_baseline_is_visible_without_invented_duration_or_metrics(self):
        with patch("workflow.benchmark.collect", side_effect=WorkflowError("baseline broken")):
            with self.assertRaisesRegex(WorkflowError, "baseline broken"):
                runner.prepare(self.repo, self.store_root, self.request, CATALOG)
        store, state_path, folder = self.saved()
        result = read_json(folder / "result.json")
        self.assertEqual(("failed", False), (result["Outcome"], result["Terminal"]))
        for key in ("DurationSeconds", "TestsBefore", "TestsAfter", "EndCommit", "ChangedFiles", "DiffPath"):
            self.assertIsNone(result[key], key)
        self.assertEqual("prepare", self.rows()[0]["LastFailurePhase"])
        self.assertEqual("baseline broken", read_json(folder / "failures.json")[0]["message"])
        self.assertEqual("prepare-failed", read_json(state_path)["status"])
        self.assertTrue((store / "active.json").exists())
        self.assertEqual("main", branch.current(self.repo))

    def test_failed_prompt_retains_baseline_and_never_creates_task_branch(self):
        self.request["modules"] = "branch,prompt,benchmark"
        with patch("workflow.benchmark.collect", return_value=METRICS), patch("workflow.prompt.improve", side_effect=RuntimeError("provider down")):
            with self.assertRaisesRegex(RuntimeError, "provider down"):
                runner.begin(self.repo, self.store_root, self.request, CATALOG)
        _, _, folder = self.saved()
        result = read_json(folder / "result.json")
        self.assertEqual(METRICS["tests"], result["TestsBefore"])
        self.assertIsNone(result["TestsAfter"])
        self.assertGreaterEqual(result["DurationSeconds"], 0)
        self.assertEqual("begin", result["LastFailurePhase"])
        self.assertIsNone(result["FindingsPath"])
        self.assertEqual("main", branch.current(self.repo))

    def test_failed_finish_then_success_replaces_row_and_keeps_failure_history(self):
        started = self.begin()
        self.commit_task()
        with patch("workflow.benchmark.collect", side_effect=WorkflowError("final Maven broken")):
            with self.assertRaises(WorkflowError):
                runner.finish(self.repo, self.store_root, started["id"], 2, 1)
        store, state_path, folder = self.saved()
        cutoff = read_json(state_path)["end"]
        failed = read_json(folder / "result.json")
        self.assertEqual("failed", self.rows()[0]["Outcome"])
        self.assertFalse(failed["Terminal"])
        self.assertIsNone(failed["TestsAfter"])
        self.assertIsNone(failed["DiffPath"])
        self.assertEqual(2, failed["HumanInterventions"])
        with patch("workflow.benchmark.collect", return_value=METRICS):
            result = runner.finish(self.repo, self.store_root, started["id"], 2, 1)
        self.assertEqual(1, len(self.rows()))
        self.assertEqual(("completed", "True"), (self.rows()[0]["Outcome"], self.rows()[0]["Terminal"]))
        self.assertEqual(1, result["FailureCount"])
        self.assertEqual("finish-benchmark", result["LastFailurePhase"])
        self.assertEqual(cutoff, read_json(state_path)["end"])
        self.assertEqual(1, len(read_json(folder / "failures.json")))
        self.assertFalse(runner.status(self.repo, self.store_root, started["id"])["error_recorded"])
        self.assertFalse((store / "active.json").exists())

    def test_abort_dirty_task_preserves_files_and_performs_no_external_calls(self):
        started = self.begin()
        (self.repo / "source.txt").write_text("uncommitted task", encoding="utf-8")
        (self.repo / "findings.md").write_text("open finding", encoding="utf-8")
        before = self.git("status", "--porcelain")
        with patch("workflow.runner.run", side_effect=AssertionError("Git called")), patch("workflow.benchmark.collect", side_effect=AssertionError("Maven called")), patch("workflow.usage.capture", side_effect=AssertionError("Usage called")), patch("workflow.prompt.improve", side_effect=AssertionError("Provider called")):
            runner.abort(self.repo, self.store_root, started["id"], "Manuell beendet")
        self.assertEqual(before, self.git("status", "--porcelain"))
        self.assertEqual("uncommitted task", (self.repo / "source.txt").read_text(encoding="utf-8"))
        self.assertEqual("open finding", (self.repo / "findings.md").read_text(encoding="utf-8"))
        result = read_json(Path(started["folder"]) / "result.json")
        self.assertEqual(("aborted", True), (result["Outcome"], result["Terminal"]))
        self.assertIsNone(result["CorrectionRounds"])
        self.assertIsNone(result["HumanInterventions"])
        self.assertIsNone(result["ChangedFiles"])
        self.assertIsNone(result["FindingsPath"])
        self.assertEqual("Manuell beendet", self.rows()[0]["StopReason"])

    def test_abort_prepared_run_has_baseline_but_no_measured_start(self):
        with patch("workflow.benchmark.collect", return_value=METRICS):
            prepared = runner.prepare(self.repo, self.store_root, self.request, CATALOG)
        runner.abort(self.repo, self.store_root, prepared["id"])
        result = read_json(Path(prepared["folder"]) / "result.json")
        self.assertIsNone(result["DurationSeconds"])
        self.assertEqual(3, result["TestsBefore"])
        self.assertIsNone(result["TestsAfter"])

    def test_terminal_failed_task_and_repeat_abort_are_idempotent(self):
        started = self.begin()
        result = runner.abort(self.repo, self.store_root, started["id"], "Tests nicht repariert", True, 0, 3)
        store, _, folder = self.saved(started["id"])
        files = {p: p.read_bytes() for p in (folder / "report.md", folder / "result.json", store / "results.csv")}
        self.assertEqual(result, runner.abort(self.repo, self.store_root, started["id"], "Andere Entscheidung"))
        self.assertEqual(files, {p: p.read_bytes() for p in files})
        self.assertEqual("failed", runner.status(self.repo, self.store_root, started["id"])["status"])
        self.assertEqual("3", self.rows()[0]["CorrectionRounds"])
        self.assertEqual("True", self.rows()[0]["Terminal"])
        with self.assertRaisesRegex(WorkflowError, "active run"):
            runner.finish(self.repo, self.store_root, started["id"])

    def test_abort_csv_failure_keeps_active_run_and_retry_preserves_stop_decision(self):
        started = self.begin()
        with patch("workflow.benchmark.append_csv", side_effect=OSError("CSV locked")):
            with self.assertRaisesRegex(OSError, "CSV locked") as failure:
                runner.abort(self.repo, self.store_root, started["id"], "Abbruchgrund", True, 1, 2)
        store, state_path, folder = self.saved()
        stop_at = read_json(state_path)["stopAt"]
        self.assertEqual("stopping", read_json(state_path)["status"])
        self.assertTrue((store / "active.json").exists())
        self.assertIn("Failure archive/CSV incomplete", " ".join(failure.exception.__notes__))
        runner.abort(self.repo, self.store_root, started["id"], "Anderer Grund", False, 0, 0)
        result = read_json(folder / "result.json")
        self.assertEqual("failed", result["Outcome"])
        self.assertTrue(result["Terminal"])
        self.assertEqual("Abbruchgrund", result["StopReason"])
        self.assertEqual((1, 2), (result["HumanInterventions"], result["CorrectionRounds"]))
        self.assertEqual(1, result["FailureCount"])
        self.assertEqual(stop_at, read_json(state_path)["aborted"])
        self.assertEqual(1, len(self.rows()))
        self.assertFalse((store / "active.json").exists())

    def test_failure_archive_error_never_hides_original_build_error(self):
        started = self.begin()
        with patch("workflow.benchmark.collect", side_effect=WorkflowError("original build failure")), patch("workflow.benchmark.append_csv", side_effect=OSError("CSV locked")):
            with self.assertRaisesRegex(WorkflowError, "original build failure") as failure:
                runner.finish(self.repo, self.store_root, started["id"])
        _, path, folder = self.saved()
        state = read_json(path)
        self.assertEqual("original build failure", state["error"])
        self.assertEqual("CSV locked", state["failureArchiveError"])
        self.assertEqual("original build failure", read_json(folder / "failures.json")[0]["message"])
        self.assertIn("CSV locked", " ".join(failure.exception.__notes__))

    def test_legacy_csv_history_is_preserved_and_marked_as_completed(self):
        started = self.begin()
        store, _, folder = self.saved()
        # Actual previous schema without the new lifecycle columns.
        known = runner.partial_result(read_json(store / "state" / (started["id"] + ".json")), store, runner.now(), "failed", False)
        new_fields = {"Outcome", "Terminal", "FailureCount", "LastFailurePhase", "LastFailureType", "StopReason"}
        legacy = {key: value for key, value in known.items() if key not in new_fields}
        legacy.update(Id="old-run", Task="Historischer Auftrag ä\nzweite Zeile", DurationSeconds=12.3)
        benchmark.append_csv(store / "results.csv", legacy)
        runner.abort(self.repo, self.store_root, started["id"])
        rows = self.rows()
        self.assertEqual(2, len(rows))
        self.assertEqual("old-run", rows[0]["Id"])
        self.assertEqual(legacy["Task"], rows[0]["Task"])
        self.assertEqual("12.3", rows[0]["DurationSeconds"])
        self.assertEqual(("completed", "True", ""), (rows[0]["Outcome"], rows[0]["Terminal"], rows[0]["FailureCount"]))
        self.assertFalse((folder / "failures.json").exists())

    def test_partial_result_ignores_stale_after_metrics_and_diff(self):
        started = self.begin()
        folder = Path(started["folder"])
        write_json(folder / "benchmark-after.json", METRICS)
        (folder / "diff.patch").write_text("unverified previous diff", encoding="utf-8")
        runner.abort(self.repo, self.store_root, started["id"])
        result = read_json(folder / "result.json")
        self.assertIsNone(result["TestsAfter"])
        self.assertIsNone(result["DiffPath"])
        self.assertEqual("unverified previous diff", (folder / "diff.patch").read_text(encoding="utf-8"))
        self.assertIn("veraltet", (folder / "report.md").read_text(encoding="utf-8"))

    def test_abort_completed_run_and_invalid_arguments_preserve_state(self):
        started = self.begin("branch")
        _, path, _ = self.saved()
        before = path.read_bytes()
        for kwargs in ({"reason": "   "}, {"corrections": -1}, {"interventions": -1}):
            with self.assertRaises(WorkflowError):
                runner.abort(self.repo, self.store_root, started["id"], **kwargs)
            self.assertEqual(before, path.read_bytes())
        runner.finish(self.repo, self.store_root, started["id"])
        before = path.read_bytes()
        with self.assertRaisesRegex(WorkflowError, "Completed"):
            runner.abort(self.repo, self.store_root, started["id"])
        self.assertEqual(before, path.read_bytes())

    def test_failure_rows_do_not_overwrite_other_successful_runs(self):
        first = self.begin()
        with patch("workflow.benchmark.collect", return_value=METRICS):
            runner.finish(self.repo, self.store_root, first["id"])
        first_row = dict(self.rows()[0])
        self.git("switch", "main")
        self.request["branch"] = "second"
        second = self.begin()
        with patch("workflow.benchmark.collect", side_effect=WorkflowError("broken")):
            with self.assertRaises(WorkflowError):
                runner.finish(self.repo, self.store_root, second["id"])
        runner.abort(self.repo, self.store_root, second["id"], "Endgültig", True)
        rows = self.rows()
        self.assertEqual(2, len(rows))
        self.assertEqual(first_row, rows[0])
        self.assertEqual("failed", rows[1]["Outcome"])
        self.assertEqual("True", rows[1]["Terminal"])

    def test_abort_cli_preserves_unicode_reason_and_real_exit_code(self):
        started = self.begin()
        command = [sys.executable, "-B", str(Path(__file__).resolve().parents[1] / "start.py"),
                   "--repo", str(self.repo), "--store-root", str(self.store_root), "abort", "--id", started["id"],
                   "--failed", "--reason", 'Prüfung fehlgeschlagen; $(literal) "quoted"', "--correction-rounds", "2"]
        process = subprocess.run(command, capture_output=True, text=True, encoding="utf-8")
        self.assertEqual(0, process.returncode, process.stderr)
        self.assertEqual("failed", json.loads(process.stdout)["status"])
        self.assertEqual('Prüfung fehlgeschlagen; $(literal) "quoted"', self.rows()[0]["StopReason"])
        self.assertEqual("2", self.rows()[0]["CorrectionRounds"])


class ConfigurationCase(unittest.TestCase):
    def test_complete_is_preset_and_module_order_is_canonical(self):
        self.assertEqual(list(runner.MODULES), runner.modules("complete"))
        self.assertEqual(["branch", "usage"], runner.modules("usage,branch"))
        for invalid in ("", "complete,usage", "branch,branch", "rag", None):
            with self.subTest(invalid=invalid), self.assertRaises(WorkflowError):
                runner.modules(invalid)

    def test_generator_preserves_forced_models_and_removes_only_owned_commands(self):
        with tempfile.TemporaryDirectory(dir=Path.cwd()) as temporary:
            root = Path(temporary)
            (root / "commands").mkdir()
            (root / "benchmark-models.json").write_bytes(CATALOG.read_bytes())
            (root / "commands/start-obsolete.md").write_text(MARKER)
            (root / "commands/start-manual.md").write_text("manual")
            self.assertEqual(22, generate(root))
            self.assertFalse((root / "commands/start-obsolete.md").exists())
            self.assertTrue((root / "commands/start-manual.md").exists())
            self.assertIn('model: "ollama-test/qwen3-coder:30b"', (root / "commands/start-qwen3-coder.md").read_text())
            self.assertIn('model: "ollama-test/qwen3.8:27b"', (root / "commands/start-qwen3.8.md").read_text())
            for entry in json.loads(CATALOG.read_text(encoding="utf-8-sig"))["models"]:
                if entry["provider"] == "Ollama lokal":
                    display_name = entry["model"].split("/", 1)[1].split(":", 1)[0]
                    self.assertEqual(display_name, entry["name"])
                    self.assertEqual(display_name, entry["command"])
            self.assertNotIn("\nmodel:", (root / "commands/start-gpt61-sol.md").read_text())
            self.assertEqual(22, generate(root))

    def test_generator_invalid_config_does_not_delete_commands(self):
        with tempfile.TemporaryDirectory(dir=Path.cwd()) as temporary:
            root = Path(temporary)
            (root / "commands").mkdir()
            retained = root / "commands/start-obsolete.md"
            retained.write_text(MARKER)
            (root / "benchmark-models.json").write_text('{"models":[{"command":"../oops"}]}')
            with self.assertRaises(ValueError):
                generate(root)
            self.assertTrue(retained.exists())

    def test_generator_refuses_manual_start_command(self):
        with tempfile.TemporaryDirectory(dir=Path.cwd()) as temporary:
            root = Path(temporary)
            (root / "commands").mkdir()
            (root / "benchmark-models.json").write_bytes(CATALOG.read_bytes())
            retained = root / "commands/start-gpt61-sol.md"
            retained.write_text("manual")
            with self.assertRaisesRegex(ValueError, "manual"):
                generate(root)
            self.assertEqual("manual", retained.read_text())


def load_tests(loader, tests, pattern):
    suite = unittest.TestSuite()
    for cls in (RepoCase, UsageCase, OutcomeCase, ConfigurationCase):
        for name in cls.__dict__:
            if name.startswith("test_"):
                suite.addTest(cls(name))
    return suite


if __name__ == "__main__":
    unittest.main()
