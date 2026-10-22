"""Offline-Regressionen; kein echter Sonar-, Docker-, Maven- oder Mailzugriff."""

import io
import json
import os
import tempfile
import unittest
from contextlib import redirect_stdout
from pathlib import Path
from unittest.mock import Mock, patch
from urllib.error import HTTPError, URLError

import credentials
import sonar


class PaginationTest(unittest.TestCase):
    def setUp(self):
        self.api = sonar.SonarApi("artificial-test-value")
        self.api.get = Mock()

    @staticmethod
    def page(index, total, *keys):
        return {
            "paging": {"pageIndex": index, "pageSize": 100, "total": total},
            "issues": [{"key": key} for key in keys],
        }

    def test_reads_every_page(self):
        self.api.get.side_effect = [self.page(1, 3, "a", "b"), self.page(2, 3, "c")]
        self.assertEqual(
            ["a", "b", "c"],
            [item["key"] for item in self.api.pages("issues/search", "issues")],
        )
        self.assertEqual(2, self.api.get.call_args.kwargs["p"])

    def test_explicit_zero_is_complete(self):
        self.api.get.return_value = self.page(1, 0)
        self.assertEqual([], self.api.pages("issues/search", "issues"))

    def test_rejects_incomplete_or_inconsistent_pages(self):
        cases = [
            [{}],
            [{"paging": None}],
            [{"paging": {"total": 0, "pageIndex": 1}}],
            [self.page(1, 3, "a"), self.page(2, 3)],
            [self.page(1, 3, "a"), self.page(2, 2, "b")],
            [self.page(1, 2, "a"), self.page(2, 2, "a")],
            [self.page(1, 1, "a", "b")],
            [self.page(2, 1, "a")],
            [self.page(1, 10001, "a")],
            [self.page(1, True)],
            [self.page(1, 1, None)],
            [self.page(1, 1, ["invalid-key"])],
        ]
        for pages in cases:
            with self.subTest(pages=pages):
                self.api.get.side_effect = pages
                with self.assertRaises(sonar.SonarError):
                    self.api.pages("issues/search", "issues")

    def test_later_page_failure_does_not_return_partial_success(self):
        self.api.get.side_effect = [self.page(1, 2, "a"), sonar.SonarError("HTTP 403")]
        with self.assertRaisesRegex(sonar.SonarError, "403"):
            self.api.pages("issues/search", "issues")


class ApiSafetyTest(unittest.TestCase):
    def test_http_errors_never_include_response_or_credentials(self):
        for status in [301, 401, 403, 404, 500]:
            with self.subTest(status=status):
                api = sonar.SonarApi("never-print-this-token")
                api.opener = Mock()
                api.opener.open.side_effect = HTTPError(
                    sonar.HOST_URL,
                    status,
                    "never-print-this-token",
                    {},
                    None,
                )
                with self.assertRaises(sonar.SonarError) as error:
                    api.get("issues/search")
                self.assertIn(str(status), str(error.exception))
                self.assertNotIn("never-print-this-token", str(error.exception))

    def test_connection_and_bad_json_fail_explicitly(self):
        for failure in [
            URLError("private details"),
            TimeoutError(),
            ValueError("bad JSON"),
        ]:
            with self.subTest(failure=type(failure).__name__):
                api = sonar.SonarApi("artificial-test-value")
                api.opener = Mock()
                api.opener.open.side_effect = failure
                with self.assertRaises(sonar.SonarError):
                    api.get("issues/search")

    def test_redirect_is_refused(self):
        self.assertIsNone(
            sonar.NoRedirect().redirect_request(
                None, None, 302, "", {}, "https://example.com"
            )
        )

    def test_unknown_authentication_is_not_valid(self):
        api = Mock()
        for response in [{}, {"valid": False}, {"valid": "true"}]:
            api.get.return_value = response
            with self.assertRaises(sonar.SonarError):
                sonar.check_authentication(api)


class SourceAndScanTest(unittest.TestCase):
    def test_feature_branch_is_rejected_before_scan(self):
        with (
            patch.object(sonar, "git", return_value="feature/sonarqube-anbindung"),
            patch.object(sonar, "run_maven") as maven,
        ):
            with self.assertRaisesRegex(sonar.SonarError, "nur main"):
                sonar.scan(Mock(), "artificial-test-value")
            maven.assert_not_called()

    def test_dirty_main_is_rejected(self):
        with (
            patch.object(sonar, "git", side_effect=["main", " M example.java"]),
            self.assertRaisesRegex(sonar.SonarError, "uncommittierte"),
        ):
            sonar.require_clean_main()

    def test_clean_main_has_verified_revision(self):
        with patch.object(sonar, "git", side_effect=["main", "", "revision"]):
            self.assertEqual("revision", sonar.require_clean_main())

    def test_scanner_arguments_pin_version_and_revision_without_token(self):
        arguments = sonar.scan_arguments("revision", Path("C:/temp/report.properties"))
        self.assertIn(sonar.SCANNER, arguments)
        self.assertIn("-Dsonar.scm.revision=revision", arguments)
        self.assertIn(f"-Dsonar.host.url={sonar.HOST_URL}", arguments)
        self.assertFalse(any("token" in argument.lower() for argument in arguments))
        self.assertFalse(any("branch.name" in argument for argument in arguments))
        self.assertIn("-Pproduction,coverage", arguments)
        self.assertIn("-Dvaadin.productionMode=true", arguments)

    def test_running_server_scan_blocks_build(self):
        api = Mock()
        api.get.side_effect = [{"valid": True}, {"queue": [{"id": "existing"}]}]
        with (
            patch.object(sonar, "require_clean_main", return_value="revision"),
            patch.object(sonar, "run_maven") as maven,
        ):
            with self.assertRaisesRegex(sonar.SonarError, "bereits"):
                sonar.scan(api, "artificial-test-value")
            maven.assert_not_called()

    def test_mcp_forwards_token_only_by_environment_name(self):
        with patch.object(
            sonar.subprocess, "run", return_value=Mock(returncode=0)
        ) as run:
            self.assertEqual(0, sonar.start_mcp("never-print-this-token"))
            command = run.call_args.args[0]
            self.assertNotIn("never-print-this-token", command)
            self.assertIn("SONARQUBE_TOKEN", command)
            self.assertEqual(
                "never-print-this-token", run.call_args.kwargs["env"]["SONARQUBE_TOKEN"]
            )
            self.assertIn("@sha256:", command[-1])
            self.assertNotIn("-v", command)
            self.assertNotIn("-p", command)

    def test_existing_local_configuration_is_preserved(self):
        with (
            tempfile.TemporaryDirectory() as directory,
            patch.object(sonar, "ROOT", Path(directory)),
        ):
            path = Path(directory) / ".codex" / "config.toml"
            path.parent.mkdir()
            path.write_text("# existing user configuration\n", encoding="utf-8")
            with self.assertRaises(sonar.SonarError):
                sonar.configure()
            self.assertEqual(
                "# existing user configuration\n", path.read_text(encoding="utf-8")
            )


class ServerCompletionTest(unittest.TestCase):
    @staticmethod
    def task(status, **changes):
        return {
            "task": {
                "id": "task",
                "componentKey": sonar.PROJECT,
                "status": status,
                **changes,
            }
        }

    def test_pending_then_matching_completed_analysis(self):
        api = Mock()
        api.get.side_effect = [
            self.task("PENDING"),
            self.task("IN_PROGRESS"),
            self.task("SUCCESS", analysisId="analysis"),
        ]
        api.latest_analysis.return_value = {"key": "analysis", "revision": "revision"}
        with patch.object(sonar.time, "sleep"):
            self.assertEqual(
                "analysis", sonar.wait_for_analysis(api, "task", "revision")
            )

    def test_failed_canceled_unknown_or_unrelated_task_is_rejected(self):
        cases = [
            self.task("FAILED"),
            self.task("CANCELED"),
            self.task("UNKNOWN"),
            self.task("SUCCESS", id="foreign"),
            self.task("SUCCESS", componentKey="foreign"),
        ]
        for answer in cases:
            with self.subTest(answer=answer):
                api = Mock()
                api.get.return_value = answer
                with self.assertRaises(sonar.SonarError):
                    sonar.wait_for_analysis(api, "task", "revision")

    def test_success_without_matching_analysis_and_revision_is_rejected(self):
        for analysis in [
            {"key": "foreign", "revision": "revision"},
            {"key": "analysis", "revision": "foreign"},
            {"key": "analysis"},
        ]:
            with self.subTest(analysis=analysis):
                api = Mock()
                api.get.return_value = self.task("SUCCESS", analysisId="analysis")
                api.latest_analysis.return_value = analysis
                with self.assertRaises(sonar.SonarError):
                    sonar.wait_for_analysis(api, "task", "revision")

    def test_timeout_is_not_success(self):
        with self.assertRaisesRegex(sonar.SonarError, "Zeitlimit"):
            sonar.wait_for_analysis(Mock(), "task", "revision", timeout=0)


class CredentialsTest(unittest.TestCase):
    def test_process_environment_does_not_require_a_file(self):
        with (
            patch.dict(os.environ, {"SONARQUBE_TOKEN": "artificial-test-value"}),
            patch.object(credentials, "credential_path") as path,
        ):
            self.assertEqual("artificial-test-value", credentials.load_token())
            path.assert_not_called()

    def test_missing_token_is_explicit(self):
        with (
            tempfile.TemporaryDirectory() as directory,
            patch.dict(os.environ, {"SONARQUBE_TOKEN": ""}),
            patch.object(
                credentials, "credential_path", return_value=Path(directory) / "missing"
            ),
            self.assertRaisesRegex(credentials.CredentialError, "fehlt"),
        ):
            credentials.load_token()

    @unittest.skipUnless(os.name == "nt", "Windows DPAPI")
    def test_dpapi_round_trip_hides_plaintext(self):
        plaintext = b"isolated-artificial-secret"
        encrypted = credentials.protect_data(plaintext)
        self.assertNotIn(plaintext, encrypted)
        self.assertEqual(plaintext, credentials.protect_data(encrypted, decrypt=True))

    @unittest.skipUnless(os.name == "nt", "Windows DPAPI")
    def test_corrupted_dpapi_data_fails_without_exposing_input(self):
        with self.assertRaises(credentials.CredentialError) as error:
            credentials.protect_data(b"private-input", decrypt=True)
        self.assertNotIn("private-input", str(error.exception))


class SnapshotTest(unittest.TestCase):
    def setUp(self):
        self.api = Mock()
        self.api.pages.side_effect = [[{"key": "open"}], [{"key": "accepted"}], []]
        self.api.latest_analysis.return_value = {
            "key": "analysis",
            "revision": "revision",
        }
        self.responses = {
            "authentication/validate": {"valid": True},
            "project_branches/list": {"branches": [{"isMain": True, "name": "main"}]},
            "qualitygates/project_status": {"projectStatus": {"status": "OK"}},
            "measures/component": {
                "component": {
                    "key": sonar.PROJECT,
                    "measures": [{"metric": "coverage", "value": "94.3"}],
                }
            },
        }
        self.api.get.side_effect = lambda path, **kwargs: self.responses[path]

    def test_snapshot_keeps_analysis_identity_and_issue_categories(self):
        with patch.object(sonar, "git", return_value="revision"):
            snapshot = sonar.read_project(self.api)
        self.assertTrue(snapshot["sourceMatchesLocalMain"])
        self.assertEqual(1, len(snapshot["openIssues"]))
        self.assertEqual(1, len(snapshot["exceptions"]))
        self.assertEqual("94.3", snapshot["measures"]["coverage"])
        self.api.pages.assert_any_call(
            "hotspots/search", "hotspots", project=sonar.PROJECT
        )
        self.api.pages.assert_any_call(
            "issues/search",
            "issues",
            components=sonar.PROJECT,
            issueStatuses="OPEN,CONFIRMED",
        )

    def test_unknown_revision_is_not_a_matching_source(self):
        self.api.latest_analysis.return_value = {"key": "analysis"}
        with patch.object(sonar, "git", return_value="revision"):
            snapshot = sonar.read_project(self.api)
        self.assertIsNone(snapshot["sourceMatchesLocalMain"])
        self.assertNotIn("line_coverage", snapshot["measures"])

    def test_old_revision_is_marked_explicitly(self):
        with patch.object(sonar, "git", return_value="new-revision"):
            snapshot = sonar.read_project(self.api)
        self.assertFalse(snapshot["sourceMatchesLocalMain"])

    def test_concurrent_analysis_replacement_is_not_a_consistent_snapshot(self):
        self.api.latest_analysis.side_effect = [{"key": "before"}, {"key": "after"}]
        with self.assertRaisesRegex(sonar.SonarError, "änderte"):
            sonar.read_project(self.api)

    def test_missing_gate_metrics_or_wrong_main_fail(self):
        changes = [
            ("qualitygates/project_status", {}),
            ("measures/component", {}),
            (
                "project_branches/list",
                {"branches": [{"isMain": True, "name": "foreign"}]},
            ),
        ]
        for endpoint, response in changes:
            with self.subTest(endpoint=endpoint):
                self.setUp()
                self.responses[endpoint] = response
                with (
                    patch.object(sonar, "git", return_value="revision"),
                    self.assertRaises(sonar.SonarError),
                ):
                    sonar.read_project(self.api)

    def test_export_contains_revision_complete_categories_and_unicode(self):
        self.api.pages.side_effect = [
            [{"key": "open", "message": "Übersetzung prüfen"}],
            [{"key": "accepted"}],
            [{"key": "hotspot"}],
        ]
        with (
            tempfile.TemporaryDirectory() as directory,
            patch.object(sonar, "ROOT", Path(directory)),
            patch.object(sonar, "git", return_value="revision"),
            redirect_stdout(io.StringIO()) as output,
        ):
            sonar.export_project(self.api)
            path = Path(directory) / "sonar-findings.json"
            exported = json.loads(path.read_text(encoding="utf-8"))
            self.assertEqual("revision", exported["analysis"]["revision"])
            self.assertTrue(exported["sourceMatchesLocalMain"])
            self.assertEqual("Übersetzung prüfen", exported["openIssues"][0]["message"])
            self.assertEqual([{"key": "accepted"}], exported["exceptions"])
            self.assertEqual([{"key": "hotspot"}], exported["hotspots"])
            self.assertEqual("OK", exported["qualityGate"]["status"])
            self.assertEqual(str(path), json.loads(output.getvalue())["exportPath"])
            self.assertEqual([path], list(Path(directory).iterdir()))

    def test_export_keeps_previous_file_when_analysis_changes_during_read(self):
        self.api.latest_analysis.side_effect = [{"key": "before"}, {"key": "after"}]
        with (
            tempfile.TemporaryDirectory() as directory,
            patch.object(sonar, "ROOT", Path(directory)),
        ):
            path = Path(directory) / "sonar-findings.json"
            path.write_text("previous complete export", encoding="utf-8")
            with self.assertRaisesRegex(sonar.SonarError, "änderte"):
                sonar.export_project(self.api)
            self.assertEqual(
                "previous complete export", path.read_text(encoding="utf-8")
            )
            self.assertEqual([path], list(Path(directory).iterdir()))

    def test_export_keeps_previous_file_and_removes_temporary_file_on_write_failure(
        self,
    ):
        with (
            tempfile.TemporaryDirectory() as directory,
            patch.object(sonar, "ROOT", Path(directory)),
            patch.object(sonar, "git", return_value="revision"),
            patch.object(Path, "replace", side_effect=OSError("destination locked")),
        ):
            path = Path(directory) / "sonar-findings.json"
            path.write_text("previous complete export", encoding="utf-8")
            with self.assertRaisesRegex(OSError, "locked"):
                sonar.export_project(self.api)
            self.assertEqual(
                "previous complete export", path.read_text(encoding="utf-8")
            )
            self.assertEqual([path], list(Path(directory).iterdir()))


class ScanLockTest(unittest.TestCase):
    def test_second_scan_is_rejected_and_first_lock_remains(self):
        with (
            tempfile.TemporaryDirectory() as directory,
            patch.object(sonar, "ROOT", Path(directory)),
        ):
            lock_path = Path(directory) / ".codex" / "sonar" / "scan.lock"
            with sonar.scan_lock():
                with self.assertRaises(sonar.SonarError), sonar.scan_lock():
                    self.fail("A second scanner acquired the lock")
                self.assertTrue(lock_path.is_file())
            self.assertFalse(lock_path.exists())

    def test_error_releases_only_the_acquired_lock(self):
        with (
            tempfile.TemporaryDirectory() as directory,
            patch.object(sonar, "ROOT", Path(directory)),
        ):
            with self.assertRaises(ValueError), sonar.scan_lock():
                raise ValueError("isolated failure")
            self.assertFalse(
                (Path(directory) / ".codex" / "sonar" / "scan.lock").exists()
            )


class ConfigurationAndMetadataTest(unittest.TestCase):
    def test_generated_configuration_is_repeatable_and_has_no_credentials(self):
        import tomllib

        with (
            tempfile.TemporaryDirectory() as directory,
            patch.object(sonar, "ROOT", Path(directory)),
            patch("builtins.print"),
        ):
            sonar.configure()
            path = Path(directory) / ".codex" / "config.toml"
            first = path.read_text(encoding="utf-8")
            sonar.configure()
            self.assertEqual(first, path.read_text(encoding="utf-8"))
            settings = tomllib.loads(first)["mcp_servers"]["sonarqube"]
            self.assertEqual("mcp", settings["args"][-1])
            self.assertIn("LOCALAPPDATA", settings["env_vars"])
            self.assertNotIn("env", settings)

    def test_missing_or_malformed_analysis_is_explicit(self):
        api = sonar.SonarApi("artificial-test-value")
        for answer in [
            {},
            {"analyses": None},
            {"analyses": "invalid"},
            {"analyses": [None]},
            {"analyses": [{}]},
        ]:
            with self.subTest(answer=answer):
                api.get = Mock(return_value=answer)
                with self.assertRaises(sonar.SonarError):
                    api.latest_analysis()

    def test_malformed_metrics_do_not_become_zero(self):
        for measures in [
            [{}],
            [None],
            [
                {"metric": "coverage", "value": "1"},
                {"metric": "coverage", "value": "2"},
            ],
        ]:
            with self.subTest(measures=measures):
                fixture = SnapshotTest()
                fixture.setUp()
                fixture.responses["measures/component"]["component"]["measures"] = (
                    measures
                )
                with self.assertRaises(sonar.SonarError):
                    sonar.read_project(fixture.api)


class ScanCompletionContractTest(unittest.TestCase):
    def setUp(self):
        self.directory = Path(self.enterContext(tempfile.TemporaryDirectory()))
        self.metadata = self.directory / ".codex/sonar/scanner-task.properties"
        self.metadata.parent.mkdir(parents=True)
        self.snapshot = {
            "project": sonar.PROJECT,
            "branch": "main",
            "analysis": {"key": "analysis", "revision": "revision"},
            "sourceMatchesLocalMain": True,
            "qualityGate": {"status": "OK"},
            "measures": {},
            "openIssues": [],
            "exceptions": [],
            "hotspots": [],
        }
        self.events = []

        def maven(arguments, token):
            self.events.append(("maven", arguments))
            if sonar.SCANNER in arguments:
                self.metadata.write_text(
                    f"projectKey={sonar.PROJECT}\nserverUrl={sonar.HOST_URL}\nceTaskId=task\n",
                    encoding="utf-8",
                )

        def process(command, **kwargs):
            self.events.append(
                ("java" if "--version" in command else "coverage", command)
            )
            return Mock(returncode=0, stdout="Java version: 25.0.1")

        self.api = Mock()
        self.api.get.side_effect = lambda path, **kwargs: (
            {"valid": True} if path == "authentication/validate" else {}
        )
        self.enterContext(patch.object(sonar, "ROOT", self.directory))
        self.source = self.enterContext(
            patch.object(sonar, "require_clean_main", return_value="revision")
        )
        self.process = self.enterContext(
            patch.object(sonar.subprocess, "run", side_effect=process)
        )
        self.maven = self.enterContext(
            patch.object(sonar, "run_maven", side_effect=maven)
        )
        self.wait = self.enterContext(
            patch.object(sonar, "wait_for_analysis", return_value="analysis")
        )
        self.enterContext(
            patch.object(sonar, "read_project", return_value=self.snapshot)
        )
        self.enterContext(redirect_stdout(io.StringIO()))

    def run_scan(self):
        sonar.scan(self.api, "isolated-artificial-test-value")

    def assert_no_upload(self):
        self.assertFalse(
            any(sonar.SCANNER in call.args[0] for call in self.maven.call_args_list)
        )
        self.wait.assert_not_called()

    def test_matching_task_source_and_gate_complete_successfully(self):
        self.run_scan()
        self.wait.assert_called_once_with(self.api, "task", "revision")

    def test_complete_reactor_and_coverage_check_precede_one_upload(self):
        self.run_scan()
        calls = [call.args[0] for call in self.maven.call_args_list]
        self.assertEqual(["-N", "spotless:check", "checkstyle:check"], calls[0])
        self.assertEqual(
            [
                "clean",
                "install",
                "-Pproduction,coverage",
                "-Dvaadin.productionMode=true",
            ],
            calls[1],
        )
        self.assertEqual(
            [
                "-Pproduction,coverage",
                "-Dvaadin.productionMode=true",
                "help:effective-pom",
                f"-Doutput={(self.directory / 'coverage-report/target/effective-pom.xml').as_posix()}",
            ],
            calls[2],
        )
        self.assertEqual(4, len(calls))
        self.assertEqual(1, sum(sonar.SCANNER in arguments for arguments in calls))
        self.assertEqual(
            ["java", "maven", "maven", "maven", "coverage", "maven"],
            [event[0] for event in self.events],
        )
        command = self.events[4][1]
        self.assertEqual(
            [
                sonar.sys.executable,
                str(self.directory / "scripts/coverage/verify_coverage.py"),
                "--root",
                str(self.directory),
                "--model",
                str(self.directory / "coverage-report/target/effective-pom.xml"),
            ],
            command,
        )
        self.assertFalse(
            any(
                "isolated-artificial-test-value" in argument
                for _, arguments in self.events
                for argument in arguments
            )
        )

    def test_style_module_build_or_effective_model_failure_prevents_upload(self):
        for stage in ("spotless:check", "install", "help:effective-pom"):
            with self.subTest(stage=stage):
                original = self.maven.side_effect

                def maven(arguments, token, stage=stage, original=original):
                    if stage in arguments:
                        raise sonar.SonarError("isolated module/build failure")
                    original(arguments, token)

                with patch.object(
                    sonar, "run_maven", side_effect=maven
                ) as failed_maven:
                    with self.assertRaisesRegex(
                        sonar.SonarError, "module/build failure"
                    ):
                        self.run_scan()
                    self.assertFalse(
                        any(
                            sonar.SCANNER in call.args[0]
                            for call in failed_maven.call_args_list
                        )
                    )
                self.wait.assert_not_called()

    def test_coverage_failure_prevents_upload_even_after_successful_build(self):
        self.process.side_effect = lambda command, **kwargs: Mock(
            returncode=0 if "--version" in command else 1, stdout="Java version: 25.0.1"
        )
        with self.assertRaisesRegex(
            sonar.SonarError, "Coverage-Prüfung fehlgeschlagen"
        ):
            self.run_scan()
        self.assert_no_upload()

    def test_wrong_or_unavailable_java_prevents_build(self):
        for code, output in (
            (0, "Java version: 21.0.1"),
            (1, "Java version: 25.0.1"),
            (0, ""),
        ):
            with self.subTest(code=code, output=output):
                self.process.side_effect = None
                self.process.return_value = Mock(returncode=code, stdout=output)
                with self.assertRaisesRegex(sonar.SonarError, "JDK 25"):
                    self.run_scan()
                self.maven.assert_not_called()

    def test_changed_revision_after_build_or_immediately_before_upload_is_rejected(
        self,
    ):
        for revisions in (("revision", "foreign"), ("revision", "revision", "foreign")):
            with self.subTest(revisions=revisions):
                self.source.side_effect = revisions
                with self.assertRaisesRegex(sonar.SonarError, "Quellstand"):
                    self.run_scan()
                self.assert_no_upload()

    def test_new_server_job_before_upload_is_rejected(self):
        self.api.get.side_effect = [{"valid": True}, {}, {"queue": [{"id": "foreign"}]}]
        with self.assertRaisesRegex(sonar.SonarError, "bereits"):
            self.run_scan()
        self.assert_no_upload()

    def test_missing_new_metadata_cannot_reuse_old_server_task(self):
        self.metadata.write_text("ceTaskId=old-task\n", encoding="utf-8")
        self.maven.side_effect = None
        with self.assertRaisesRegex(sonar.SonarError, "keinen neuen Serverauftrag"):
            self.run_scan()
        self.assertFalse(self.metadata.exists())
        self.wait.assert_not_called()

    def test_revision_change_after_upload_leaves_acceptance_open(self):
        self.source.side_effect = ["revision", "revision", "revision", "foreign"]
        with self.assertRaisesRegex(sonar.SonarError, "nach Upload verändert"):
            self.run_scan()

    def test_another_analysis_of_same_revision_is_not_our_scan(self):
        self.snapshot["analysis"]["key"] = "foreign"
        with self.assertRaisesRegex(sonar.SonarError, "andere Analyse"):
            self.run_scan()

    def test_completed_server_processing_with_red_gate_still_fails(self):
        self.snapshot["qualityGate"]["status"] = "ERROR"
        with self.assertRaisesRegex(sonar.SonarError, "Quality Gate"):
            self.run_scan()


if __name__ == "__main__":
    unittest.main()
