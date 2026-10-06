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
        (self.repo / "findings.md").write_text("# Findings\nä\n", encoding="utf-8")
        with patch("workflow.benchmark.collect", return_value=METRICS):
            result = runner.finish(self.repo, self.store_root, started["id"], 2, 1)
        folder = Path(started["folder"])
        self.assertEqual(2, result["HumanInterventions"])
        self.assertEqual(1, result["CorrectionRounds"])
        self.assertEqual(self.git("rev-parse", "HEAD"), result["EndCommit"])
        self.assertEqual(2, result["ChangedFiles"])
        self.assertEqual(3, result["TestsBefore"])
        self.assertIn("ä", (folder / "findings.md").read_text(encoding="utf-8"))
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
        self.assertEqual(result, runner.finish(self.repo, self.store_root, started["id"]))

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
            started = runner.begin(self.repo, self.store_root, self.request, CATALOG, export_file)
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
                           ("model", "invented"), ("task", " "), ("modules", "reviewer")):
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
        with patch("workflow.benchmark.collect", return_value=METRICS), patch("workflow.benchmark.append_csv", side_effect=OSError("locked")):
            with self.assertRaisesRegex(OSError, "locked"):
                runner.finish(self.repo, self.store_root, started["id"])
        self.assertTrue((self.repo / "findings.md").exists())
        with patch("workflow.benchmark.collect", return_value=METRICS):
            runner.finish(self.repo, self.store_root, started["id"])
        self.assertFalse((self.repo / "findings.md").exists())

    def test_lock_rejects_concurrent_store_access(self):
        with store_lock(self.store_root):
            with self.assertRaisesRegex(WorkflowError, "locked"):
                with store_lock(self.store_root, timeout=0.05):
                    self.fail("Should not acquire lock twice")

    def test_cli_begin_finish_preserves_task_as_data(self):
        request = self.base / "request.json"
        write_json(request, dict(self.request, modules="branch"))
        script = Path(__file__).resolve().parents[1] / "start.py"
        command = [sys.executable, "-B", str(script), "--repo", str(self.repo),
                   "--store-root", str(self.store_root)]
        before = subprocess.run([*command, "begin", "--request", str(request)], capture_output=True, text=True, encoding="utf-8")
        self.assertEqual(0, before.returncode, before.stderr)
        started = json.loads(before.stdout)
        self.assertEqual(self.request["task"], started["task"])
        after = subprocess.run([*command, "finish", "--id", started["id"]], capture_output=True, text=True, encoding="utf-8")
        self.assertEqual(0, after.returncode, after.stderr)
        self.assertEqual("feature/tests", json.loads(after.stdout)["Branch"])

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
        return usage.snapshot(self.export(messages), self.repo, "ses_test", captured_ms=6000)

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
        self.assertEqual(1.5, result["InferenceSeconds"])

    def test_usage_step_and_message_totals_are_not_double_counted(self):
        result = usage.difference(self.snap([]), self.snap([assistant()]))
        self.assertEqual(100, result["InputTokens"])
        self.assertEqual(20, result["OutputTokens"])
        self.assertEqual(5, result["ReasoningTokens"])
        self.assertEqual(40, result["CacheReadTokens"])
        self.assertEqual(3, result["CacheWriteTokens"])
        self.assertEqual(1, result["Requests"])
        self.assertEqual(1.5, result["ReasoningSeconds"])
        self.assertEqual(4, result["InferenceSeconds"])
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

    def test_usage_fallback_completed_assistant_and_explicit_zero_cost(self):
        msg = assistant(steps=False)
        msg["info"]["cost"] = 0
        result = usage.difference(self.snap([]), self.snap([msg]))
        self.assertEqual(1, result["Requests"])
        self.assertEqual(0, result["EstimatedCostUSD"])

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
        before_path, after_path = self.base / "before.json", self.base / "after.json"
        write_json(before_path, self.bridge_export(captured_ms=time.time() * 1000, messages=[assistant("msg_old")]))
        self.request.update(modules="complete", session_id="ses_test")
        with patch("workflow.benchmark.collect", return_value=METRICS), patch("workflow.prompt.improve", return_value="improved"):
            started = runner.begin(self.repo, self.store_root, self.request, CATALOG, before_path)
        self.commit_task()
        write_json(after_path, self.bridge_export(captured_ms=time.time() * 1000,
                                                  messages=[assistant("msg_old"), assistant("msg_new")]))
        with patch("workflow.benchmark.collect", return_value=METRICS):
            result = runner.finish(self.repo, self.store_root, started["id"], usage_export=after_path)
        self.assertEqual(100, result["InputTokens"])
        self.assertEqual("openai/test-model", result["ActualModels"])
        self.assertTrue((Path(started["folder"]) / "usage.json").is_file())
        self.assertEqual("OpenCode SDK", read_json(Path(started["folder"]) / "usage-before.json")["source"])

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
            second = runner.begin(self.repo, self.store_root, self.request, CATALOG, before_path)
        self.commit_task()
        with patch("workflow.benchmark.collect", return_value=METRICS):
            second_result = runner.finish(self.repo, self.store_root, second["id"], usage_export=after_path)
        self.assertEqual(list(first_result), list(second_result))
        with (self.store_root / self.repo.name / "results.csv").open(encoding="utf-8", newline="") as handle:
            rows = list(csv.DictReader(handle))
        self.assertEqual(2, len(rows))
        self.assertEqual("", rows[0]["InputTokens"])
        self.assertEqual("100", rows[1]["InputTokens"])


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
    for cls in (RepoCase, UsageCase, ConfigurationCase):
        for name in cls.__dict__:
            if name.startswith("test_"):
                suite.addTest(cls(name))
    return suite


if __name__ == "__main__":
    unittest.main()
