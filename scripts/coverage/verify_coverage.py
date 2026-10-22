"""Prüft Maven-Analyseumfang und gemeinsame JUnit-Coverage ohne Sonar-Zugriff."""

import argparse
import json
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

NS = {"m": "http://maven.apache.org/POM/4.0.0"}
MODULES = ("app", "shared", "browser-tests")


class CoverageError(Exception):
    pass


def required_file_timestamp(path):
    if not path.is_file() or path.stat().st_size == 0:
        raise CoverageError(f"Erforderlicher Coverage-/Buildnachweis fehlt: {path}")
    return path.stat().st_mtime_ns


def verify_compiled_sources(project, root, module, kind):
    source_tag, output_tag = (
        ("sourceDirectory", "outputDirectory")
        if kind == "sources"
        else ("testSourceDirectory", "testOutputDirectory")
    )
    source = Path(project.findtext(f"m:build/m:{source_tag}", namespaces=NS)).resolve()
    output = Path(project.findtext(f"m:build/m:{output_tag}", namespaces=NS)).resolve()
    if not source.is_relative_to(root / module) or not output.is_relative_to(
        root / module / "target"
    ):
        raise CoverageError(f"Modulübergreifender Quell-/Klassenpfad: {module}/{kind}")
    files = sorted(source.rglob("*.java"))
    timestamps = []
    for file in files:
        compiled = output / file.relative_to(source).with_suffix(".class")
        compiled_time = required_file_timestamp(compiled)
        source_time = file.stat().st_mtime_ns
        if compiled_time < source_time:
            raise CoverageError(f"Quellen sind neuer als ihre Klassen: {file}")
        timestamps.extend((source_time, compiled_time))
    return source, len(files), timestamps


def read_module_coverage(group, source_root, module):
    if not group.findall("package/class"):
        raise CoverageError(f"Keine Klassen im JaCoCo-Modul: {module}")
    for package in group.findall("package"):
        for file in package.findall("sourcefile"):
            source = (
                source_root / package.get("name", "") / file.get("name", "")
            ).resolve()
            if not source.is_relative_to(source_root) or not source.is_file():
                raise CoverageError(
                    f"JaCoCo-Quelle nicht ihrem Modul zugeordnet: {source}"
                )
    counter = group.find("counter[@type='LINE']")
    if counter is None:
        raise CoverageError(f"Zeilenzähler fehlt: {module}")
    covered, missed = int(counter.get("covered")), int(counter.get("missed"))
    if covered < 0 or missed < 0:
        raise CoverageError(f"Ungültige Coverage-Zähler: {module}")
    return {
        "coveredLines": covered,
        "missedLines": missed,
        "lineCoverage": 100 * covered / (covered + missed)
        if covered + missed
        else None,
    }


def verify_coverage(root, model):
    root = root.resolve()
    model_time = required_file_timestamp(model)
    for pom in (
        root / "pom.xml",
        *(root / module / "pom.xml" for module in (*MODULES, "coverage-report")),
    ):
        if model_time < required_file_timestamp(pom):
            raise CoverageError(f"Maven-Modell ist älter als seine POM: {pom}")
    document = ET.parse(model).getroot()
    projects = document.findall("m:project", NS)
    by_id = {p.findtext("m:artifactId", namespaces=NS): p for p in projects}
    if set(by_id) != {"vaadin-spring-app", "coverage-report", *MODULES}:
        raise CoverageError("Coverage benötigt den vollständigen Maven-Reactor.")
    key = by_id["vaadin-spring-app"].findtext(
        "m:properties/m:sonar.projectKey", namespaces=NS
    )
    if key != "vaadin-spring-app":
        raise CoverageError("Abweichender gemeinsamer Sonar-Projektkey.")
    if (
        by_id["coverage-report"].findtext("m:properties/m:sonar.skip", namespaces=NS)
        != "true"
    ):
        raise CoverageError("Das technische Berichtsmodul muss sonar.skip setzen.")
    report = root / "coverage-report/target/site/jacoco-aggregate/jacoco.xml"
    report_time = required_file_timestamp(report)
    inputs = []
    module_inputs = {}
    measurements = {}
    modules = {}
    source_roots = {}
    for module in MODULES:
        project = by_id[module]
        properties = project.find("m:properties", NS)
        if (
            any(
                properties.find(f"m:{name}", NS) is not None
                for name in (
                    "sonar.sources",
                    "sonar.tests",
                    "sonar.exclusions",
                    "sonar.test.exclusions",
                    "sonar.coverage.exclusions",
                )
            )
            or properties.findtext("m:sonar.skip", namespaces=NS) == "true"
        ):
            raise CoverageError(f"Ungeprüfter Sonar-Scope-Override: {module}")
        configured = properties.findtext(
            "m:sonar.coverage.jacoco.xmlReportPaths", namespaces=NS
        )
        if not configured or Path(configured).resolve() != report:
            raise CoverageError(
                f"Gemeinsamer JaCoCo-Bericht nicht zugeordnet: {module}"
            )
        modules[module] = {}
        module_inputs[module] = []
        roots = {}
        for kind in ("sources", "tests"):
            source, count, timestamps = verify_compiled_sources(
                project, root, module, kind
            )
            modules[module][kind] = count
            roots[kind] = source
            inputs.extend(timestamps)
            module_inputs[module].extend(timestamps)
            if kind == "sources":
                source_roots[module] = source
        if roots["sources"].is_relative_to(roots["tests"]) or roots[
            "tests"
        ].is_relative_to(roots["sources"]):
            raise CoverageError(f"Überlappende Quell- und Testverzeichnisse: {module}")
        if module in ("app", "browser-tests"):
            if modules[module]["tests"] == 0:
                raise CoverageError(f"Testquellen fehlen: {module}")
            measurement_time = required_file_timestamp(
                root / module / "target/jacoco.exec"
            )
            measurements[module] = measurement_time
            inputs.append(measurement_time)
        if module in ("app", "shared") and modules[module]["sources"] == 0:
            raise CoverageError(f"Anwendungsquellen fehlen: {module}")
    if modules["browser-tests"]["sources"] != 0:
        raise CoverageError("Browsercode muss im Testquellbaum bleiben.")
    if report_time < max(inputs):
        raise CoverageError(
            "JaCoCo-Bericht ist älter als seine Klassen oder Messdaten."
        )
    # Ein neu erzeugter Bericht macht Messdaten vor einer Neukompilierung nicht aktuell.
    # Beide Testmodule verwenden Shared, das Browsermodul jedoch keine App-Klassen.
    for module, measurement_time in measurements.items():
        if measurement_time < max(module_inputs[module] + module_inputs["shared"]):
            raise CoverageError(
                f"Messdaten sind älter als ihre Klassen oder Quellen: {module}"
            )
    coverage = ET.parse(report).getroot()
    sessions = {session.get("id") for session in coverage.findall("sessioninfo")}
    if not {"app", "browser-tests"}.issubset(sessions):
        raise CoverageError("App- oder Helfermessung fehlt im aggregierten Bericht.")
    groups = {group.get("name"): group for group in coverage.findall("group")}
    # JaCoCo verwendet die Maven-Artefakt-ID als Gruppenidentität, nicht den Anzeigenamen.
    if set(groups) != {"app", "shared"}:
        raise CoverageError(
            "JaCoCo muss genau App und Shared als Quellmodule enthalten."
        )
    for module in ("app", "shared"):
        modules[module].update(
            read_module_coverage(groups[module], source_roots[module], module)
        )
    return {"projectKey": key, "modules": modules, "report": str(report)}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--root", type=Path, default=Path(__file__).resolve().parents[2]
    )
    parser.add_argument("--model", type=Path)
    args = parser.parse_args()
    model = args.model or args.root / "coverage-report/target/effective-pom.xml"
    try:
        print(
            json.dumps(verify_coverage(args.root, model), ensure_ascii=False, indent=2)
        )
    except (CoverageError, OSError, ET.ParseError, ValueError, TypeError) as error:
        print(f"Coverage-Prüfung fehlgeschlagen: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
