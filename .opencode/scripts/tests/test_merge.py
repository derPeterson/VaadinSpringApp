"""Real temporary Git repositories; no application, provider or live project writes."""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

import merge
from workflow.common import WorkflowError, write_json


class MergeCase(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="merge-test-", dir=Path.cwd())
        self.addCleanup(self.temp.cleanup)
        self.base = Path(self.temp.name)
        self.repo = self.base / "Example Project"
        self.repo.mkdir()
        self.store_root = self.base / "benchmarks"
        self.store = self.store_root / self.repo.name
        self.git("init", "-b", "main")
        self.git("config", "user.name", "Merge Test")
        self.git("config", "user.email", "merge@example.invalid")
        self.git("config", "commit.gpgsign", "false")
        self.git("config", "core.hooksPath", str(self.base / "hooks"))
        (self.repo / "code.txt").write_text("baseline\n", encoding="utf-8")
        self.commit("baseline")
        self.main_commit = self.git("rev-parse", "HEAD")
        self.git("switch", "-c", "feature/change")
        (self.repo / "code.txt").write_text("baseline\nÄnderung\n", encoding="utf-8")
        self.commit("Validierung korrigiert")
        self.feature_commit = self.git("rev-parse", "HEAD")
        self.run_id = "a" * 32
        self.folder = self.store / "runs" / ("2026-10-08_00-00-00_UTC__" + self.run_id)
        self.state_file = self.store / "state" / (self.run_id + ".json")
        write_json(self.store / "project.json", {"repo": str(self.repo)})
        self.state = dict(repo=str(self.repo), id=self.run_id, branch="feature/change", status="completed",
                          createdAt="2026-10-08T00:00:00+00:00", folder=str(self.folder),
                          startCommit=self.main_commit, finishCommit=self.feature_commit)
        self.result = dict(Id=self.run_id, Branch="feature/change", Outcome="completed", Terminal=True,
                           StartCommit=self.main_commit, EndCommit=self.feature_commit, RunFolder=str(self.folder))
        write_json(self.state_file, self.state)
        write_json(self.folder / "result.json", self.result)
        (self.folder / "report.md").write_text("# Bericht\n\nValidierung korrigiert.\n", encoding="utf-8")
        (self.folder / "findings.md").write_text("# Offene Findings\n\nKeine weiteren offenen Findings festgestellt.\n", encoding="utf-8")
        self.plan_file = self.base / "merge-plan.json"
        self.message = self.base / "merge-message.txt"
        self.message.write_text("Validierung und Übersetzungen korrigiert\n\n- Frühen Abbruch geprüft.\n- DE/EN-Hinweise übersetzt; '$()' bleibt Text.\n", encoding="utf-8")

    def git(self, *args):
        return merge.git(self.repo, *args).strip()

    def commit(self, message):
        self.git("add", "--", "code.txt")
        self.git("commit", "-m", message)

    def plan(self):
        value = merge.prepare(self.repo, self.store_root, "change")
        write_json(self.plan_file, value)
        return value

    def snapshots(self):
        return {str(path): hashlib.sha256(path.read_bytes()).hexdigest()
                for path in self.store.rglob("*") if path.is_file() and path.name != ".lock"}

    def test_merge_keeps_commits_branch_archive_and_exact_feature_tree(self):
        before = self.snapshots()
        plan = self.plan()
        self.assertEqual("ready", plan["status"])
        result = merge.apply(self.repo, self.plan_file, self.message)
        self.assertEqual("merged", result["status"])
        self.assertEqual("main", self.git("branch", "--show-current"))
        self.assertEqual("", self.git("status", "--porcelain"))
        self.assertEqual(self.feature_commit, self.git("rev-parse", "feature/change"))
        self.assertEqual([self.main_commit, self.feature_commit], self.git("rev-list", "--parents", "-n", "1", "HEAD").split()[1:])
        self.assertEqual(self.git("rev-parse", "HEAD^{tree}"), self.git("rev-parse", "feature/change^{tree}"))
        text = self.git("log", "-1", "--format=%B")
        self.assertEqual("In main integriert: Validierung und Übersetzungen korrigiert", text.splitlines()[0])
        self.assertTrue(result["message"].startswith(text.splitlines()[0] + "\n"))
        self.assertEqual("Validierung korrigiert", self.git("show", "-s", "--format=%s", self.feature_commit))
        self.assertIn("Übersetzungen korrigiert", text)
        self.assertIn("'$()' bleibt Text", text)
        self.assertIn("Benchmark-Run: " + self.run_id, text)
        self.assertEqual(before, self.snapshots())

    def test_existing_merge_title_prefix_is_not_duplicated_and_body_is_preserved(self):
        body = "\n\n- DE/EN geprüft; '$()' bleibt Text.\n"
        original = "In main integriert: Validierung korrigiert" + body
        self.message.write_text(original, encoding="utf-8")
        self.plan()
        result = merge.apply(self.repo, self.plan_file, self.message)
        actual = self.git("show", "-s", "--format=%B", "HEAD")
        self.assertEqual("In main integriert: Validierung korrigiert", actual.splitlines()[0])
        self.assertEqual(1, actual.splitlines()[0].count("In main integriert:"))
        self.assertIn(body.rstrip(), actual)
        self.assertEqual(original, self.message.read_text(encoding="utf-8"))
        self.assertEqual(actual, result["message"].strip())

    def test_prefix_without_a_description_stops_before_branch_switch_or_commit(self):
        self.message.write_text("In main integriert:  \n\n- Test geprüft.\n", encoding="utf-8")
        before = self.snapshots()
        self.plan()
        with self.assertRaisesRegex(WorkflowError, "konkrete Beschreibung"):
            merge.apply(self.repo, self.plan_file, self.message)
        self.assertEqual("feature/change", self.git("branch", "--show-current"))
        self.assertEqual(self.main_commit, self.git("rev-parse", "main"))
        self.assertEqual(self.feature_commit, self.git("rev-parse", "HEAD"))
        self.assertEqual(before, self.snapshots())

    def test_prepare_does_not_switch_or_create_commit(self):
        self.plan()
        self.assertEqual("feature/change", self.git("branch", "--show-current"))
        self.assertEqual(self.feature_commit, self.git("rev-parse", "HEAD"))

    def test_omitted_branch_selects_current_feature_and_merge_ends_on_main(self):
        implicit = merge.prepare(self.repo, self.store_root)
        self.assertEqual(self.plan(), implicit)
        self.assertEqual("feature/change", implicit["branch"])
        result = merge.apply(self.repo, self.plan_file, self.message)
        self.assertEqual("merged", result["status"])
        self.assertEqual("main", self.git("branch", "--show-current"))

    def test_omitted_branch_on_main_does_not_guess_latest_feature(self):
        self.git("switch", "main")
        with self.assertRaisesRegex(WorkflowError, "Ohne Branchname"):
            merge.prepare(self.repo, self.store_root)
        self.assertEqual(self.main_commit, self.git("rev-parse", "HEAD"))
        self.assertEqual("ready", merge.prepare(self.repo, self.store_root, "change")["status"])

    def test_omitted_branch_on_unrelated_or_detached_head_is_rejected(self):
        self.git("switch", "-c", "unrelated")
        with self.assertRaisesRegex(WorkflowError, "Ohne Branchname"):
            merge.prepare(self.repo, self.store_root)
        self.git("switch", "--detach", self.feature_commit)
        with self.assertRaisesRegex(WorkflowError, "Ohne Branchname"):
            merge.prepare(self.repo, self.store_root)
        self.assertEqual(self.feature_commit, self.git("rev-parse", "HEAD"))

    def test_cli_omitted_branch_and_failure_on_main(self):
        command = [sys.executable, str(Path(merge.__file__)), "--repo", str(self.repo),
                   "--store-root", str(self.store_root), "prepare", "--plan-file", str(self.plan_file)]
        result = subprocess.run(command, capture_output=True)
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual("feature/change", json.loads(result.stdout.decode("utf-8"))["branch"])
        original_plan = self.plan_file.read_bytes()
        self.git("switch", "main")
        rejected = subprocess.run(command, capture_output=True)
        self.assertEqual(1, rejected.returncode)
        self.assertIn("Ohne Branchname", rejected.stderr.decode("utf-8"))
        self.assertEqual(original_plan, self.plan_file.read_bytes())

    def test_retry_after_merge_creates_no_empty_or_duplicate_commit(self):
        self.plan()
        done = merge.apply(self.repo, self.plan_file, self.message)
        self.plan()
        retry = merge.apply(self.repo, self.plan_file, None)
        self.assertEqual("already_integrated", retry["status"])
        self.assertEqual(done["commit"], retry["commit"])

    def test_main_invocation_and_short_or_full_branch_are_supported(self):
        short = self.plan()
        self.git("switch", "main")
        self.assertEqual(short, merge.prepare(self.repo, self.store_root, "feature/change"))
        self.assertEqual("merged", merge.apply(self.repo, self.plan_file, self.message)["status"])

    def test_input_is_not_a_revision_or_shell_expression(self):
        for name in ("main", "--help", "change;whoami", "$(whoami)", "change other", "feature/change/x", "refs/heads/feature/change"):
            with self.subTest(name=name), self.assertRaises(WorkflowError):
                merge.prepare(self.repo, self.store_root, name)
        self.assertEqual(self.feature_commit, self.git("rev-parse", "HEAD"))

    def test_dirty_staged_and_untracked_changes_block_without_stash(self):
        code = self.repo / "code.txt"
        original = code.read_bytes()
        code.write_text("user change\n", encoding="utf-8")
        with self.assertRaisesRegex(WorkflowError, "sauber"):
            self.plan()
        self.git("add", "--", "code.txt")
        with self.assertRaisesRegex(WorkflowError, "sauber"):
            self.plan()
        code.write_bytes(original)
        self.git("add", "--", "code.txt")
        (self.repo / "findings.md").write_text("User-owned file", encoding="utf-8")
        with self.assertRaisesRegex(WorkflowError, "sauber"):
            self.plan()
        self.assertTrue((self.repo / "findings.md").exists())

    def test_active_run_blocks_prepare_and_apply(self):
        self.plan()
        write_json(self.store / "active.json", {"id": "b" * 32})
        with self.assertRaisesRegex(WorkflowError, "aktiv"):
            self.plan()
        with self.assertRaisesRegex(WorkflowError, "aktiv"):
            merge.apply(self.repo, self.plan_file, self.message)
        self.assertEqual(self.main_commit, self.git("rev-parse", "main"))

    def test_diverged_main_is_rejected_before_switch(self):
        self.git("switch", "main")
        (self.repo / "code.txt").write_text("different main\n", encoding="utf-8")
        self.commit("main verändert")
        self.git("switch", "feature/change")
        with self.assertRaisesRegex(WorkflowError, "auseinander"):
            self.plan()
        self.assertEqual("feature/change", self.git("branch", "--show-current"))

    def test_wrong_store_identity_is_rejected(self):
        write_json(self.store / "project.json", {"repo": str(self.base / "another")})
        with self.assertRaisesRegex(WorkflowError, "zugeordnet"):
            self.plan()

    def test_latest_unfinished_run_is_not_replaced_by_an_old_completed_run(self):
        later = dict(self.state, id="b" * 32, status="failed", createdAt="2026-10-09T00:00:00+00:00")
        write_json(self.store / "state" / (later["id"] + ".json"), later)
        with self.assertRaisesRegex(WorkflowError, "abgeschlossen"):
            self.plan()

    def test_missing_run_and_missing_report_are_not_guessed(self):
        self.state_file.unlink()
        with self.assertRaisesRegex(WorkflowError, "Kein zugehöriger"):
            self.plan()
        write_json(self.state_file, self.state)
        (self.folder / "report.md").unlink()
        with self.assertRaises(FileNotFoundError):
            self.plan()

    def test_stale_feature_commit_blocks_apply(self):
        self.plan()
        (self.repo / "code.txt").write_text("extra\n", encoding="utf-8")
        self.commit("zusätzliche Änderung")
        with self.assertRaisesRegex(WorkflowError, "geändert"):
            merge.apply(self.repo, self.plan_file, self.message)
        self.assertEqual(self.main_commit, self.git("rev-parse", "main"))
        self.assertTrue(self.plan()["additionalCommits"])

    def test_stale_report_and_stale_main_block_apply(self):
        self.plan()
        (self.folder / "report.md").write_text("edited report", encoding="utf-8")
        with self.assertRaisesRegex(WorkflowError, "geändert"):
            merge.apply(self.repo, self.plan_file, self.message)
        self.plan()
        self.git("update-ref", "refs/heads/main", self.feature_commit)
        with self.assertRaisesRegex(WorkflowError, "geändert"):
            merge.apply(self.repo, self.plan_file, self.message)

    def test_run_commit_from_another_history_and_mismatched_result_are_rejected(self):
        self.result["EndCommit"] = self.main_commit
        write_json(self.folder / "result.json", self.result)
        with self.assertRaisesRegex(WorkflowError, "widersprechen"):
            self.plan()
        self.result["EndCommit"] = self.feature_commit
        self.state["finishCommit"] = "--all"
        write_json(self.state_file, self.state)
        with self.assertRaisesRegex(WorkflowError, "Commit-IDs"):
            self.plan()

    def test_commit_message_and_plan_must_be_outside_git(self):
        self.plan()
        with self.assertRaises(WorkflowError):
            merge.apply(self.repo, self.repo / "plan.json", self.message)
        with self.assertRaises(WorkflowError):
            merge.apply(self.repo, self.plan_file, self.repo / "message.txt")
        self.message.write_text("title only", encoding="utf-8")
        with self.assertRaisesRegex(WorkflowError, "Stichpunkt"):
            merge.apply(self.repo, self.plan_file, self.message)
        self.assertEqual("feature/change", self.git("branch", "--show-current"))

    def test_open_findings_are_preserved_and_not_silently_resolved(self):
        (self.folder / "findings.md").write_text("# Offene Findings\n\nF3 remains open", encoding="utf-8")
        before = self.snapshots()
        self.plan()
        merge.apply(self.repo, self.plan_file, self.message)
        self.assertEqual(before, self.snapshots())

    def test_other_worktree_on_main_is_rejected(self):
        self.git("worktree", "add", str(self.base / "other worktree"), "main")
        with self.assertRaisesRegex(WorkflowError, "anderen Worktree"):
            self.plan()

    def test_pending_git_operation_and_unrelated_branch_are_rejected(self):
        marker = self.repo / ".git" / "CHERRY_PICK_HEAD"
        marker.write_text(self.main_commit, encoding="ascii")
        with self.assertRaisesRegex(WorkflowError, "Git-Operation"):
            self.plan()
        marker.unlink()
        self.git("switch", "-c", "unrelated")
        with self.assertRaisesRegex(WorkflowError, "fremden"):
            self.plan()

    def test_hook_failure_is_not_bypassed_or_reported_as_success(self):
        self.plan()
        hooks = self.base / "hooks"
        hooks.mkdir()
        hook = hooks / "pre-merge-commit"
        hook.write_text("#!/bin/sh\nexit 1\n", encoding="utf-8")
        hook.chmod(0o755)
        with self.assertRaises(WorkflowError):
            merge.apply(self.repo, self.plan_file, self.message)
        self.assertEqual(self.main_commit, self.git("rev-parse", "main"))
        self.assertTrue((self.repo / ".git" / "MERGE_HEAD").exists())

    def test_cli_help_and_prepare_handle_paths_with_spaces(self):
        script = Path(merge.__file__)
        help_result = subprocess.run([sys.executable, str(script), "--help"], capture_output=True, text=True)
        self.assertEqual(0, help_result.returncode)
        result = subprocess.run([sys.executable, str(script), "--repo", str(self.repo), "--store-root", str(self.store_root),
                                 "prepare", "--branch", "change", "--plan-file", str(self.plan_file)], capture_output=True)
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual("ready", json.loads(result.stdout.decode("utf-8"))["status"])


if __name__ == "__main__":
    unittest.main()
