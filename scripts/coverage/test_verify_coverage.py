"""Offline-Regressionen für Scope, Quellzuordnung und fehlende Coverage-Nachweise."""

import os
import tempfile
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

from verify_coverage import NS, CoverageError, verify_coverage


def element(parent, name, text=None):
    node = ET.SubElement(parent, "{" + NS["m"] + "}" + name)
    node.text = text
    return node


class CoverageVerificationTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name).resolve()
        self.model = self.root / "effective.xml"
        self.report = (
            self.root / "coverage-report/target/site/jacoco-aggregate/jacoco.xml"
        )
        self.report.parent.mkdir(parents=True)
        self.projects = ET.Element("projects")
        self.properties = {}
        self.groups = ET.Element("report")
        for module in ("app", "browser-tests"):
            ET.SubElement(self.groups, "sessioninfo", id=module, start="0", dump="1")
        for module in (
            "vaadin-spring-app",
            "app",
            "shared",
            "browser-tests",
            "coverage-report",
        ):
            self.write(
                "pom.xml" if module == "vaadin-spring-app" else module + "/pom.xml",
                b"<project/>",
            )
            project = element(self.projects, "project")
            element(project, "artifactId", module)
            element(project, "name", "Display name: " + module)
            properties = element(project, "properties")
            self.properties[module] = properties
            if module == "vaadin-spring-app":
                element(properties, "sonar.projectKey", "vaadin-spring-app")
            if module == "coverage-report":
                element(properties, "sonar.skip", "true")
            if module not in ("app", "shared", "browser-tests"):
                continue
            element(
                properties, "sonar.coverage.jacoco.xmlReportPaths", str(self.report)
            )
            build = element(project, "build")
            for tag, relative in (
                ("sourceDirectory", "src/main/java"),
                ("testSourceDirectory", "src/test/java"),
                ("outputDirectory", "target/classes"),
                ("testOutputDirectory", "target/test-classes"),
            ):
                element(build, tag, str(self.root / module / relative))
            if module != "browser-tests":
                self.write(
                    module + "/src/main/java/example/Example.java", b"source fixture"
                )
                self.write(
                    module + "/target/classes/example/Example.class", b"class fixture"
                )
                group = ET.SubElement(self.groups, "group", name=module)
                package = ET.SubElement(group, "package", name="example")
                ET.SubElement(package, "class", name="example/Example")
                ET.SubElement(package, "sourcefile", name="Example.java")
                ET.SubElement(group, "counter", type="LINE", covered="2", missed="3")
            if module != "shared":
                self.write(
                    module + "/src/test/java/example/ExampleTest.java", b"test fixture"
                )
                self.write(
                    module + "/target/test-classes/example/ExampleTest.class",
                    b"compiled test fixture",
                )
        for module in ("app", "browser-tests"):
            self.write(module + "/target/jacoco.exec", b"execution data fixture")
        ET.ElementTree(self.projects).write(self.model)
        self.write_report()

    def write(self, relative, content):
        path = self.root / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(content)
        return path

    def write_report(self):
        ET.ElementTree(self.groups).write(self.report)

    def verify(self):
        return verify_coverage(self.root, self.model)

    def test_app_and_shared_have_one_report_and_browser_code_remains_test_code(self):
        result = self.verify()
        self.assertEqual("vaadin-spring-app", result["projectKey"])
        self.assertEqual(40, result["modules"]["shared"]["lineCoverage"])
        self.assertEqual(0, result["modules"]["browser-tests"]["sources"])
        self.assertEqual(1, result["modules"]["browser-tests"]["tests"])

    def test_measured_zero_is_a_valid_result(self):
        for counter in self.groups.findall("group/counter"):
            counter.set("covered", "0")
        self.write_report()
        self.assertEqual(0, self.verify()["modules"]["shared"]["lineCoverage"])

    def test_missing_report_is_not_measured_zero(self):
        self.report.unlink()
        with self.assertRaisesRegex(CoverageError, "nachweis fehlt"):
            self.verify()

    def test_missing_execution_data_is_not_a_complete_measurement(self):
        (self.root / "browser-tests/target/jacoco.exec").unlink()
        with self.assertRaisesRegex(CoverageError, "nachweis fehlt"):
            self.verify()

    def test_existing_data_files_do_not_prove_they_were_aggregated(self):
        self.groups.remove(self.groups.find("sessioninfo[@id='browser-tests']"))
        self.write_report()
        with self.assertRaisesRegex(CoverageError, "Helfermessung fehlt"):
            self.verify()

    def test_partial_reactor_is_rejected(self):
        self.projects.remove(self.projects[-1])
        ET.ElementTree(self.projects).write(self.model)
        with self.assertRaisesRegex(CoverageError, "vollständigen Maven-Reactor"):
            self.verify()

    def test_old_maven_model_cannot_hide_changed_pom_configuration(self):
        path = self.root / "app/pom.xml"
        future = self.model.stat().st_mtime_ns + 10_000_000_000
        os.utime(path, ns=(future, future))
        with self.assertRaisesRegex(CoverageError, "Maven-Modell ist älter"):
            self.verify()

    def test_global_source_override_is_rejected(self):
        element(self.properties["shared"], "sonar.sources", "../app/src/main/java")
        ET.ElementTree(self.projects).write(self.model)
        with self.assertRaisesRegex(CoverageError, "Scope-Override"):
            self.verify()

    def test_main_and_test_sources_cannot_be_indexed_twice(self):
        app = self.projects.find("m:project[m:artifactId='app']", NS)
        app.find("m:build/m:testSourceDirectory", NS).text = str(
            self.root / "app/src/main/java"
        )
        app.find("m:build/m:testOutputDirectory", NS).text = str(
            self.root / "app/target/classes"
        )
        ET.ElementTree(self.projects).write(self.model)
        with self.assertRaisesRegex(CoverageError, "Überlappende Quell-"):
            self.verify()

    def test_wrong_report_mapping_is_rejected(self):
        self.properties["app"].find(
            "m:sonar.coverage.jacoco.xmlReportPaths", NS
        ).text = "wrong.xml"
        ET.ElementTree(self.projects).write(self.model)
        with self.assertRaisesRegex(CoverageError, "nicht zugeordnet"):
            self.verify()

    def test_missing_test_bytecode_is_rejected(self):
        (
            self.root / "browser-tests/target/test-classes/example/ExampleTest.class"
        ).unlink()
        with self.assertRaisesRegex(CoverageError, "nachweis fehlt"):
            self.verify()

    def test_changed_class_after_reporting_is_rejected(self):
        path = self.root / "app/target/classes/example/Example.class"
        future = self.report.stat().st_mtime_ns + 10_000_000_000
        os.utime(path, ns=(future, future))
        with self.assertRaisesRegex(CoverageError, "Bericht ist älter"):
            self.verify()

    def test_changed_source_without_compilation_is_rejected(self):
        path = self.root / "app/src/main/java/example/Example.java"
        future = self.report.stat().st_mtime_ns + 10_000_000_000
        os.utime(path, ns=(future, future))
        with self.assertRaisesRegex(CoverageError, "Quellen sind neuer"):
            self.verify()

    def test_new_report_cannot_hide_app_measurement_before_recompilation(self):
        future = self.report.stat().st_mtime_ns + 10_000_000_000
        path = self.root / "app/target/classes/example/Example.class"
        os.utime(path, ns=(future, future))
        os.utime(self.report, ns=(future + 1, future + 1))
        with self.assertRaisesRegex(CoverageError, "Messdaten sind älter.*app"):
            self.verify()

    def test_new_report_cannot_hide_helper_measurement_before_test_recompilation(self):
        future = self.report.stat().st_mtime_ns + 10_000_000_000
        path = self.root / "browser-tests/target/test-classes/example/ExampleTest.class"
        os.utime(path, ns=(future, future))
        os.utime(self.report, ns=(future + 1, future + 1))
        with self.assertRaisesRegex(
            CoverageError, "Messdaten sind älter.*browser-tests"
        ):
            self.verify()

    def test_shared_recompilation_requires_new_measurements_from_both_consumers(self):
        future = self.report.stat().st_mtime_ns + 10_000_000_000
        path = self.root / "shared/target/classes/example/Example.class"
        os.utime(path, ns=(future, future))
        os.utime(self.report, ns=(future + 1, future + 1))
        with self.assertRaisesRegex(CoverageError, "Messdaten sind älter.*app"):
            self.verify()
        os.utime(self.root / "app/target/jacoco.exec", ns=(future, future))
        with self.assertRaisesRegex(
            CoverageError, "Messdaten sind älter.*browser-tests"
        ):
            self.verify()

    def test_browser_test_code_cannot_become_an_application_coverage_group(self):
        ET.SubElement(self.groups, "group", name="browser-tests")
        self.write_report()
        with self.assertRaisesRegex(CoverageError, "genau App und Shared"):
            self.verify()

    def test_coverage_source_cannot_be_assigned_to_another_module(self):
        self.groups.find("group/package/sourcefile").set("name", "Missing.java")
        self.write_report()
        with self.assertRaisesRegex(
            CoverageError, "Quelle nicht ihrem Modul zugeordnet"
        ):
            self.verify()


if __name__ == "__main__":
    unittest.main()
