"""Offline /restart regressions: existing real Git branches, no provider/server calls."""
import contextlib
import csv
import io
import itertools
import json
import subprocess
import sys
import unittest
from datetime import datetime
from pathlib import Path
from unittest.mock import patch

from tests import test_workflow as fixtures
from workflow import cli, runner, usage
from workflow.common import WorkflowError, read_json, write_json


class RestartCase(unittest.TestCase):
    # Reuse only fixtures, not the original tests or their discovery hook.
    git = fixtures.RepoCase.git
    export = fixtures.RepoCase.export
    commit_task = fixtures.RepoCase.commit_task

    def setUp(self):
        fixtures.RepoCase.setUp(self)
        self.git("switch", "-c", "feature/existing")
        self.commit_task()  # Existing feature work must be excluded from the new patch.
        self.baseline = self.git("rev-parse", "HEAD")
        self.request.update(branch=None, modules="benchmark", session_id="ses_test")

    def prepare(self, request=None):
        with patch("workflow.benchmark.collect", return_value=fixtures.METRICS):
            return runner.prepare(self.repo, self.store_root, request or self.request, fixtures.CATALOG)

    def new_task_commit(self, text="Nachbesserung ä\n"):
        (self.repo / "followup.txt").write_text(text, encoding="utf-8")
        self.git("add", "--", "followup.txt")
        self.git("commit", "-m", "Nachbesserung umgesetzt")

    def test_existing_feature_baseline_and_patch_contain_only_new_work(self):
        prepared = self.prepare()
        started = runner.begin(self.repo, self.store_root, None, fixtures.CATALOG, prepared_id=prepared["id"])
        self.assertEqual("feature/existing", started["branch"])
        self.assertEqual(self.baseline, self.git("rev-parse", "HEAD"))
        self.new_task_commit()
        with patch("workflow.benchmark.collect", return_value=fixtures.METRICS):
            result = runner.finish(self.repo, self.store_root, started["id"])
        folder = Path(started["folder"])
        self.assertEqual(self.baseline, result["StartCommit"])
        self.assertEqual(1, result["ChangedFiles"])
        self.assertIn(b"followup.txt", (folder / "diff.patch").read_bytes())
        self.assertNotIn(b"source.txt", (folder / "diff.patch").read_bytes())
        self.assertNotIn(b"binary.dat", (folder / "diff.patch").read_bytes())
        self.assertEqual("feature/existing", self.git("branch", "--show-current"))

    def test_complete_and_all_module_combinations_use_same_engine(self):
        selections = ["complete"] + [",".join(c) for n in (1, 2, 3)
                                      for c in itertools.combinations(runner.RESTART_MODULES, n)]
        epoch = datetime.fromisoformat("2026-10-08T10:00:00+00:00").timestamp() * 1000
        for selector in selections:
            with self.subTest(selector=selector):
                expected = list(runner.RESTART_MODULES) if selector == "complete" else runner.modules(selector)
                before = usage.snapshot(self.export([]), self.repo, "ses_test", captured_ms=epoch + 1000)
                after = usage.snapshot(self.export([fixtures.assistant()]), self.repo, "ses_test", captured_ms=epoch + 3000)
                request = runner.restart_request(dict(self.request, modules=selector))
                with patch("workflow.runner.now", return_value="2026-10-08T10:00:00+00:00"), \
                        patch("workflow.benchmark.collect", return_value=fixtures.METRICS) as collect, \
                        patch("workflow.prompt.improve", return_value="Präzisierter Auftrag") as improve, \
                        patch("workflow.usage.capture", side_effect=[before, before, after]) as capture:
                    prepared = runner.prepare(self.repo, self.store_root, request, fixtures.CATALOG)
                    started = runner.begin(self.repo, self.store_root, None, fixtures.CATALOG, prepared_id=prepared["id"])
                    self.assertEqual(expected, started["modules"])
                    self.assertEqual("feature/existing", started["branch"])
                    self.new_task_commit(selector + "\n")
                    result = runner.finish(self.repo, self.store_root, started["id"])
                self.assertEqual(",".join(expected), result["Modules"])
                self.assertEqual("feature/existing", result["Branch"])
                self.assertEqual(2 if "benchmark" in expected else 0, collect.call_count)
                self.assertEqual(1 if "prompt" in expected else 0, improve.call_count)
                self.assertEqual(3 if "usage" in expected else 0, capture.call_count)
                self.assertEqual(100 if "usage" in expected else None, result["InputTokens"])
                self.assertEqual(3 if "benchmark" in expected else None, result["TestsBefore"])

    def test_restart_rejects_branch_names_unknown_and_duplicate_modules_before_mutation(self):
        for value in ("branch", "branch,prompt", "reviewer", "prompt,prompt", "complete,usage", [], ["branch"]):
            with self.subTest(value=value), self.assertRaises(WorkflowError):
                runner.restart_request(dict(self.request, modules=value))
        with self.assertRaises(WorkflowError):
            runner.restart_request(dict(self.request, modules="prompt", branch="other"))
        for value in (None, [], "task"):
            with self.subTest(request=value), self.assertRaises(WorkflowError):
                runner.restart_request(value)
        self.assertEqual(self.baseline, self.git("rev-parse", "HEAD"))
        self.assertFalse(self.store_root.exists())

    def test_restart_rejects_main_other_branches_and_detached_head(self):
        request = runner.restart_request(dict(self.request, modules="prompt"))
        for target in ("main", "unrelated", "HEAD"):
            if target == "unrelated":
                self.git("switch", "-c", target)
            elif target == "HEAD":
                self.git("switch", "--detach", "HEAD")
            else:
                self.git("switch", target)
            with self.subTest(branch=target), patch("workflow.prompt.improve") as improve, self.assertRaises(WorkflowError):
                runner.prepare(self.repo, self.store_root, request, fixtures.CATALOG)
            improve.assert_not_called()

    def test_existing_branch_is_checked_even_without_benchmark(self):
        request = runner.restart_request(dict(self.request, modules="prompt"))
        with patch("workflow.prompt.improve", return_value="Task"):
            started = runner.begin(self.repo, self.store_root, request, fixtures.CATALOG)
        self.git("switch", "-c", "feature/wrong")
        with patch("workflow.benchmark.collect") as collect, self.assertRaisesRegex(WorkflowError, "feature/existing"):
            runner.finish(self.repo, self.store_root, started["id"])
        collect.assert_not_called()
        self.assertEqual("active", runner.status(self.repo, self.store_root, started["id"])["status"])

    def test_rejects_dirty_restart_before_calls(self):
        (self.repo / "unexpected.txt").write_text("user change", encoding="utf-8")
        with patch("workflow.benchmark.collect") as collect, patch("workflow.prompt.improve") as improve, \
                self.assertRaisesRegex(WorkflowError, "not clean"):
            runner.prepare(self.repo, self.store_root, runner.restart_request(self.request), fixtures.CATALOG)
        collect.assert_not_called()
        improve.assert_not_called()
        self.assertTrue((self.repo / "unexpected.txt").is_file())

    def test_changed_head_or_branch_after_prepare_stops_before_prompt(self):
        prepared = self.prepare()
        self.git("switch", "-c", "feature/other")
        with self.assertRaisesRegex(WorkflowError, "changed after prepare"):
            runner.begin(self.repo, self.store_root, None, fixtures.CATALOG, prepared_id=prepared["id"])
        self.git("switch", "feature/existing")
        self.new_task_commit()
        with patch("workflow.prompt.improve") as improve, self.assertRaisesRegex(WorkflowError, "changed after prepare"):
            runner.begin(self.repo, self.store_root, None, fixtures.CATALOG, prepared_id=prepared["id"])
        improve.assert_not_called()

    def test_start_complete_still_requires_main_and_creates_branch(self):
        with self.assertRaisesRegex(WorkflowError, "main"):
            runner.validate_request(self.repo, dict(self.request, modules="complete", branch="new"), fixtures.CATALOG)
        self.git("switch", "main")
        with self.assertRaisesRegex(WorkflowError, "feature"):
            runner.validate_request(self.repo, dict(self.request, modules="benchmark"), fixtures.CATALOG)
        self.assertEqual(list(runner.MODULES), runner.modules("complete"))
        self.assertEqual(list(runner.RESTART_MODULES), runner.restart_request({"task": "x"})["modules"])

    def test_new_runs_preserve_previous_archive_and_have_distinct_ids(self):
        with patch("workflow.benchmark.collect", return_value=fixtures.METRICS):
            started = runner.begin(self.repo, self.store_root, self.request, fixtures.CATALOG)
        self.new_task_commit()
        with patch("workflow.benchmark.collect", return_value=fixtures.METRICS):
            result = runner.finish(self.repo, self.store_root, started["id"])
        folder = Path(started["folder"])
        original = {p.name: p.read_bytes() for p in folder.iterdir()}
        with patch("workflow.benchmark.collect", return_value=fixtures.METRICS):
            second = runner.begin(self.repo, self.store_root, self.request, fixtures.CATALOG)
        self.assertNotEqual(started["id"], second["id"])
        state = read_json(self.store_root / self.repo.name / "state" / (second["id"] + ".json"))
        self.assertEqual(result["EndCommit"], state["startCommit"])
        self.assertEqual(original, {p.name: p.read_bytes() for p in folder.iterdir()})

    def test_existing_active_run_is_not_reopened_or_aborted(self):
        with patch("workflow.benchmark.collect", return_value=fixtures.METRICS):
            first = runner.begin(self.repo, self.store_root, self.request, fixtures.CATALOG)
        original = {p.name: p.read_bytes() for p in Path(first["folder"]).iterdir()}
        with self.assertRaisesRegex(WorkflowError, "Unfinished"):
            self.prepare(runner.restart_request(self.request))
        self.assertEqual(first["id"], runner.status(self.repo, self.store_root)["id"])
        self.assertEqual(original, {p.name: p.read_bytes() for p in Path(first["folder"]).iterdir()})

    def test_analysis_restart_keeps_commit_and_produces_empty_diff(self):
        request = runner.restart_request(dict(self.request, modules="benchmark", task_mode="analysis"))
        with patch("workflow.benchmark.collect", return_value=fixtures.METRICS):
            started = runner.begin(self.repo, self.store_root, request, fixtures.CATALOG)
            result = runner.finish(self.repo, self.store_root, started["id"])
        self.assertEqual(self.baseline, result["StartCommit"])
        self.assertEqual(self.baseline, result["EndCommit"])
        self.assertEqual(b"", (Path(started["folder"]) / "diff.patch").read_bytes())

    def test_restart_python_entrypoint_help_is_read_only(self):
        script = Path(__file__).resolve().parents[1] / "restart.py"
        for args in ([], ["--help"], ["begin", "--help"]):
            result = subprocess.run([sys.executable, "-B", str(script), *args], capture_output=True, text=True, encoding="utf-8")
            self.assertEqual(0, result.returncode, result.stderr)
            self.assertIn("usage:", result.stdout)
        self.assertFalse(self.store_root.exists())

    def test_restart_cli_preserves_task_and_same_branch(self):
        path = self.base / "request.json"
        write_json(path, dict(self.request, modules="usage", task_mode="analysis"))
        export = self.base / "before.json"
        write_json(export, self.export([]))
        command = ["--repo", str(self.repo), "--store-root", str(self.store_root), "begin", "--request", str(path),
                   "--usage-export", str(export)]
        with contextlib.redirect_stdout(io.StringIO()) as output:
            self.assertEqual(0, cli.main(command, restart=True))
        started = json.loads(output.getvalue())
        self.assertEqual(self.request["task"], started["task"])
        self.assertEqual(["usage"], started["modules"])
        self.assertEqual("feature/existing", started["branch"])
        state = read_json(self.store_root / self.repo.name / "state" / (started["id"] + ".json"))
        self.assertEqual("restart", state["entrypoint"])
        self.assertEqual(self.baseline, state["startCommit"])

    def test_restart_cli_cannot_begin_a_start_run_that_would_create_a_branch(self):
        self.git("switch", "main")
        request = dict(self.request, modules="branch", branch="new")
        prepared = self.prepare(request)
        args = ["--repo", str(self.repo), "--store-root", str(self.store_root), "begin", "--id", prepared["id"]]
        with contextlib.redirect_stderr(io.StringIO()) as error:
            self.assertEqual(1, cli.main(args, restart=True))
        self.assertIn("created through /restart", error.getvalue())
        self.assertEqual("main", self.git("branch", "--show-current"))


if __name__ == "__main__":
    unittest.main()
