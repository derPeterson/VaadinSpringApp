"""Actual Windows shells: native stderr must not turn successful prepare into failure."""
import base64
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import unittest

from tests import test_workflow as fixtures


def quote(value):
    return "'" + str(value).replace("'", "''") + "'"


def logged_command(arguments, log, *, explicit=True):
    native = "& " + " ".join(quote(value) for value in arguments)
    pipeline = native + " 2>&1 | Tee-Object -FilePath " + quote(log)
    if not explicit:
        return pipeline
    return (
        "try { " + pipeline + " -ErrorAction Stop; "
        "$workflowExit = $LASTEXITCODE; exit $workflowExit "
        "} catch { Write-Error $_ -ErrorAction Continue; exit 1 }"
    )


@unittest.skipUnless(os.name == "nt", "Requires Windows native shell behavior")
class PowerShellExitTests(unittest.TestCase):
    git = fixtures.RepoCase.git

    def setUp(self):
        fixtures.RepoCase.setUp(self)
        self.launcher = self.base / "prepare_probe.py"
        self.request_file = self.base / "request.json"
        self.request_file.write_text(json.dumps(self.request), encoding="utf-8")
        self.head_before = self.git("rev-parse", "HEAD")

    def shell(self, name, command):
        executable = shutil.which(name)
        if not executable:
            self.skipTest(name + " is not installed")
        encoded = base64.b64encode(command.encode("utf-16-le")).decode("ascii")
        return subprocess.run(
            [executable, "-NoProfile", "-NonInteractive", "-EncodedCommand", encoded],
            cwd=self.repo, capture_output=True, timeout=60,
        )

    def prepare(self, shell="powershell.exe", *, failure=False, explicit=True):
        scripts = Path(__file__).resolve().parents[1]
        self.launcher.write_text(
            "import sys\n"
            "from unittest.mock import patch\n"
            f"sys.path.insert(0, {str(scripts)!r})\n"
            "from workflow import cli\n"
            "from workflow.common import WorkflowError\n"
            "def collect(*args, **kwargs):\n"
            "    print('WARNING: simulated JVM warning', file=sys.stderr)\n"
            + ("    raise WorkflowError('Maven clean test failed (7)')\n" if failure
               else f"    return {fixtures.METRICS!r}\n")
            + "with patch('workflow.benchmark.collect', side_effect=collect):\n"
            "    raise SystemExit(cli.main())\n",
            encoding="utf-8",
        )
        log = self.base / "prepare.log"
        arguments = [sys.executable, "-B", self.launcher, "--repo", self.repo,
                     "--store-root", self.store_root, "prepare", "--request", self.request_file]
        result = self.shell(shell, logged_command(arguments, log, explicit=explicit))
        states = list((self.store_root / self.repo.name / "state").glob("*.json"))
        self.assertEqual(1, len(states), result.stderr)
        state = json.loads(states[0].read_text(encoding="utf-8"))
        self.assertEqual("main", self.git("branch", "--show-current"))
        self.assertEqual(self.head_before, self.git("rev-parse", "HEAD"))
        self.assertEqual("", self.git("status", "--porcelain"))
        return result, state, log

    def test_windows_powershell_reproduces_false_failure_after_successful_prepare(self):
        result, state, log = self.prepare(explicit=False)
        self.assertEqual(1, result.returncode)
        self.assertEqual("prepared", state["status"])
        text = log.read_text(encoding="utf-16")
        self.assertIn('"status": "prepared"', text)
        self.assertIn("simulated JVM warning", text)

    def check_prepare_success(self, shell):
        result, state, log = self.prepare(shell)
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual("prepared", state["status"])
        self.assertTrue(log.exists())
        self.assertEqual(fixtures.METRICS, state["before"])

    def test_windows_powershell_preserves_successful_prepare(self):
        self.check_prepare_success("powershell.exe")

    def test_powershell7_preserves_successful_prepare(self):
        self.check_prepare_success("pwsh.exe")

    def check_prepare_failure(self, shell):
        result, state, log = self.prepare(shell, failure=True)
        self.assertEqual(1, result.returncode)
        self.assertEqual("prepare-failed", state["status"])
        self.assertTrue(log.exists())
        self.assertFalse((Path(state["folder"]) / "benchmark-before.json").exists())

    def test_windows_powershell_preserves_real_prepare_failure(self):
        self.check_prepare_failure("powershell.exe")

    def test_powershell7_preserves_real_prepare_failure(self):
        self.check_prepare_failure("pwsh.exe")

    def check_logging_failure(self, shell):
        # Test shell failure independently: Tee may stop the pipeline before
        # Python has written its state, so no lifecycle-state assertion here.
        probe = self.base / "native.py"
        probe.write_text("print('success')\n", encoding="utf-8")
        result = self.shell(shell, logged_command(
            [sys.executable, "-B", probe], self.base / "missing/log.txt"))
        self.assertEqual(1, result.returncode)

    def test_windows_powershell_logging_failure_does_not_become_success(self):
        self.check_logging_failure("powershell.exe")

    def test_powershell7_logging_failure_does_not_become_success(self):
        self.check_logging_failure("pwsh.exe")

    def check_native_exit(self, shell):
        probe = self.base / "native.py"
        probe.write_text("import sys\nprint('warning', file=sys.stderr)\nsys.exit(int(sys.argv[1]))\n",
                         encoding="utf-8")
        for native_exit in (0, 7):
            with self.subTest(native_exit=native_exit):
                result = self.shell(shell, logged_command(
                    [sys.executable, "-B", probe, native_exit], self.base / "native.log"))
                self.assertEqual(native_exit, result.returncode, result.stderr)

    def test_windows_powershell_preserves_exact_native_exit(self):
        self.check_native_exit("powershell.exe")

    def test_powershell7_preserves_exact_native_exit(self):
        self.check_native_exit("pwsh.exe")


if __name__ == "__main__":
    unittest.main()
