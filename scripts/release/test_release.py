"""Releasegrenzen mit eigenen Git-Repositories und synthetischen JARs prüfen."""

import copy
import hashlib
import json
import tempfile
import unittest
import zipfile
from pathlib import Path

import release


class ReleaseWorkflowTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory(prefix="release-test-")
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        release.git(self.root, "init", "-q")
        release.git(self.root, "config", "user.name", "Release Test")
        release.git(self.root, "config", "user.email", "release@example.test")
        self.root.joinpath(".gitignore").write_text(
            ".codex/\napp/target/\n", encoding="utf-8"
        )
        for filename in release.POMS:
            path = self.root / filename
            path.parent.mkdir(parents=True, exist_ok=True)
            coordinates = (
                "<artifactId>app</artifactId><version>0.1.0-SNAPSHOT</version>"
                if filename == "app/pom.xml"
                else "<artifactId>vaadin-spring-app</artifactId><version>0.1.0-SNAPSHOT</version>"
                if filename == "pom.xml"
                else "<parent><artifactId>vaadin-spring-app</artifactId><version>0.1.0-SNAPSHOT</version></parent>"
            )
            dependency = (
                "<dependencies><dependency><groupId>de.derpeterson</groupId><artifactId>shared</artifactId><version>${project.version}</version></dependency></dependencies>"
                if filename in ("app/pom.xml", "browser-tests/pom.xml")
                else ""
            )
            path.write_text(
                f'<project xmlns="http://maven.apache.org/POM/4.0.0">{coordinates}{dependency}</project>',
                encoding="utf-8",
            )
        self.notes = "# Änderungen\n\n## [Unreleased]\n\n### DE\n\n- Neue Funktion.\n\n### EN\n\n- New feature.\n"
        self.root.joinpath("CHANGELOG.md").write_text(self.notes, encoding="utf-8")
        self.commit()

    def commit(self):
        release.git(self.root, "add", ".")
        release.git(self.root, "commit", "-q", "-m", "Teststand")
        return release.git(self.root, "rev-parse", "HEAD")

    def candidate(self):
        release.prepare(self.root, "0.1.0")
        return self.commit()

    def artifact(self, commit, **overrides):
        values = {"version": "0.1.0", "revision": commit, "dirty": "false", **overrides}
        path = self.root / "app/target/app.jar"
        path.parent.mkdir(parents=True, exist_ok=True)
        with zipfile.ZipFile(path, "w") as jar:
            jar.writestr(
                "BOOT-INF/classes/META-INF/build-info.properties",
                "\n".join(f"build.{key}={value}" for key, value in values.items()),
            )
            jar.writestr(
                "BOOT-INF/classes/de/derpeterson/app/Application.class", b"test"
            )
        return path

    def qa(self, revision, artifact):
        qualification = {
            "version": "0.1.0",
            "commit": revision,
            "sha256": hashlib.sha256(artifact.read_bytes()).hexdigest(),
            "quality_gate": "OK",
            "sonar_revision": revision,
            "checks": dict.fromkeys(release.REQUIRED_CHECKS, "passed"),
            "browser_image_de": "sha256:" + "a" * 64,
            "browser_image_en": "sha256:" + "a" * 64,
        }
        state = {
            "current": None,
            "last_success": {
                "phase": "complete",
                "commit": revision,
                "release_qualification": qualification,
            },
        }
        self.write_qa(state)
        return state

    def write_qa(self, state):
        path = self.root / ".codex/qa/state.json"
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(state), encoding="utf-8")

    def test_preview_is_read_only_and_ignores_nonrelease_tags(self):
        release.git(self.root, "tag", "archiv/html-referenzen")
        before = {
            path: (self.root / path).read_bytes()
            for path in (*release.POMS, "CHANGELOG.md")
        }
        result = release.preview(self.root)
        self.assertEqual("0.1.0", result["candidate"])
        self.assertIsNone(result["latestRelease"])
        self.assertFalse(result["changesWritten"])
        self.assertEqual(
            before, {path: (self.root / path).read_bytes() for path in before}
        )

    def test_prepare_changes_all_modules_and_both_languages_without_tagging(self):
        release.prepare(self.root, "0.1.0")
        self.assertEqual("0.1.0", release.project_version(self.root))
        content = self.root.joinpath("CHANGELOG.md").read_text(encoding="utf-8")
        sections = release.changelog_sections(content)
        self.assertFalse(content.endswith("\n\n"))
        self.assertEqual(["", ""], sections["Unreleased"]["entries"])
        self.assertIn("Neue Funktion", sections["0.1.0"]["entries"][0])
        self.assertIn("New feature", sections["0.1.0"]["entries"][1])
        self.assertEqual([], release.release_tags(self.root))

    def test_prepare_rejects_dirty_worktree_before_writing(self):
        self.root.joinpath("other.txt").write_text("draft", encoding="utf-8")
        with self.assertRaises(release.ReleaseError):
            release.prepare(self.root, "0.1.0")
        self.assertEqual("0.1.0-SNAPSHOT", release.project_version(self.root))

    def test_prepare_rejects_inconsistent_modules_without_partial_changes(self):
        path = self.root / "app/pom.xml"
        path.write_text(path.read_text().replace("0.1.0-SNAPSHOT", "0.2.0-SNAPSHOT"))
        self.commit()
        with self.assertRaises(release.ReleaseError):
            release.prepare(self.root, "0.1.0")
        self.assertIn("0.1.0-SNAPSHOT", self.root.joinpath("pom.xml").read_text())

    def test_prepare_rejects_wrong_candidate_and_existing_release(self):
        for version in ("0.1.1", "1.0.0", "0.1.0-SNAPSHOT"):
            with self.subTest(version=version), self.assertRaises(release.ReleaseError):
                release.prepare(self.root, version)
        release.git(self.root, "tag", "v0.1.0")
        with self.assertRaises(release.ReleaseError):
            release.prepare(self.root, "0.1.0")

    def test_version_validation_rejects_options_and_noncanonical_numbers(self):
        for version in ("--force", "01.1.0", "0.1", "0.1.0-rc1", "0.1.0\n", "-1.0.0"):
            with self.subTest(version=version), self.assertRaises(release.ReleaseError):
                release.version_numbers(version)

    def test_changelog_rejects_missing_language_duplicate_version_and_invalid_date(
        self,
    ):
        bad_contents = (
            self.notes.replace("### EN", "### FR"),
            self.notes + "\n## [Unreleased]\n\n### DE\n\n### EN\n",
            self.notes + "\n## [0.0.1] - 2026-02-30\n\n### DE\n- A\n### EN\n- B\n",
            self.notes + "\n## [0.0.1] - 2020-01-01\n\n### DE\n### EN\n",
            self.notes.replace("- New feature.", ""),
        )
        for content in bad_contents:
            with self.subTest(content=content), self.assertRaises(release.ReleaseError):
                release.changelog_sections(content)

    def test_internal_dependency_must_follow_project_version(self):
        path = self.root / "browser-tests/pom.xml"
        path.write_text(
            path.read_text().replace("${project.version}", "0.1.0-SNAPSHOT")
        )
        with self.assertRaises(release.ReleaseError):
            release.project_version(self.root)

    def test_artifact_rejects_stale_version_revision_and_dirty_build(self):
        revision = self.candidate()
        for overrides in (
            {"version": "0.0.1"},
            {"revision": "a" * 40},
            {"dirty": "true"},
            {"revision": "unknown"},
        ):
            with (
                self.subTest(overrides=overrides),
                self.assertRaises(release.ReleaseError),
            ):
                release.verify_artifact(self.root, self.artifact(revision, **overrides))

    def test_tag_requires_qa_for_exact_artifact_and_same_browser_image(self):
        revision = self.candidate()
        artifact = self.artifact(revision)
        good = self.qa(revision, artifact)
        cases = []
        for key, value in (
            ("commit", "b" * 40),
            ("version", "0.1.1"),
            ("sha256", "wrong"),
            ("quality_gate", "ERROR"),
            ("sonar_revision", "c" * 40),
            ("browser_image_en", "sha256:other"),
        ):
            state = copy.deepcopy(good)
            state["last_success"]["release_qualification"][key] = value
            cases.append(state)
        for check in release.REQUIRED_CHECKS:
            state = copy.deepcopy(good)
            state["last_success"]["release_qualification"]["checks"][check] = "failed"
            cases.append(state)
        cases.append({**good, "current": {"phase": "running"}})
        old_success = copy.deepcopy(good)
        old_success["last_success"]["commit"] = "d" * 40
        cases.append(old_success)
        for state in cases:
            self.write_qa(state)
            with self.subTest(state=state), self.assertRaises(release.ReleaseError):
                release.tag_release(self.root, artifact)
            self.assertEqual([], release.release_tags(self.root))

    def test_tag_rejects_malformed_qa_without_creating_a_tag(self):
        revision = self.candidate()
        artifact = self.artifact(revision)
        for state in ([], {}, {"last_success": {"phase": "complete"}}):
            self.write_qa(state)
            with self.subTest(state=state), self.assertRaises(release.ReleaseError):
                release.tag_release(self.root, artifact)
            self.assertEqual([], release.release_tags(self.root))

    def test_tag_keeps_verified_artifact_and_next_only_bumps_patch(self):
        revision = self.candidate()
        artifact = self.artifact(revision)
        self.qa(revision, artifact)
        result = release.tag_release(self.root, artifact)
        self.assertEqual(
            revision, release.git(self.root, "rev-list", "-n", "1", "v0.1.0")
        )
        archive = self.root / ".codex/releases/0.1.0/app-0.1.0.jar"
        self.assertEqual(artifact.read_bytes(), archive.read_bytes())
        with self.assertRaises(release.ReleaseError):
            release.tag_release(self.root, artifact)
        following = release.next_snapshot(self.root)
        self.assertEqual("0.1.1-SNAPSHOT", following["developmentVersion"])
        self.assertEqual("0.1.1-SNAPSHOT", release.project_version(self.root))
        self.assertEqual("0.1.0", result["version"])
        self.assertEqual(
            revision, release.git(self.root, "rev-list", "-n", "1", "v0.1.0")
        )

    def test_conflicting_archive_is_not_overwritten_and_no_tag_is_created(self):
        revision = self.candidate()
        artifact = self.artifact(revision)
        self.qa(revision, artifact)
        archive = self.root / ".codex/releases/0.1.0/app-0.1.0.jar"
        archive.parent.mkdir(parents=True)
        archive.write_bytes(b"existing artifact")
        with self.assertRaises(release.ReleaseError):
            release.tag_release(self.root, artifact)
        self.assertEqual(b"existing artifact", archive.read_bytes())
        self.assertEqual([], release.release_tags(self.root))

    def test_next_requires_tag_on_current_revision(self):
        self.candidate()
        with self.assertRaises(release.ReleaseError):
            release.next_snapshot(self.root)
        release.git(self.root, "tag", "v0.1.0")
        self.root.joinpath("change.txt").write_text("new change", encoding="utf-8")
        self.commit()
        with self.assertRaises(release.ReleaseError):
            release.next_snapshot(self.root)


if __name__ == "__main__":
    unittest.main()
