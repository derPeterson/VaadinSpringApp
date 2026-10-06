"""Maven/JaCoCo/Surefire and commit-diff metrics."""
import csv
import os
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path
from .common import write_json

from .common import WorkflowError, run


def fresh_reports(repo: Path) -> dict:
    wrapper = repo / ("mvnw.cmd" if os.name == "nt" else "mvnw")
    if not wrapper.is_file() or not (repo / "pom.xml").is_file():
        raise WorkflowError("Benchmark requires pom.xml and the Maven Wrapper.")
    version = run(repo, str(wrapper), "--version")
    if not re.search(r"Java version:\s*25(?:[.\s,]|$)", version):
        raise WorkflowError("Benchmark requires Java 25. Maven reports:\n" + version)
    # Stream builds so progress and failures remain visible; never reuse stale reports.
    checks = [{"arguments": ["--version"], "exitCode": 0}]
    for goals in (("clean", "test"), ("jacoco:report",)):
        result = subprocess.run([str(wrapper), *goals], cwd=repo,
                                stdout=sys.stderr, stderr=sys.stderr)
        if result.returncode:
            raise WorkflowError(f"Maven {' '.join(goals)} failed ({result.returncode}).")
        checks.append({"arguments": list(goals), "exitCode": result.returncode})
    return {"javaMajor": 25, "checks": checks}


def class_row(repo: Path, class_name: str) -> dict:
    report = repo / "target/site/jacoco/jacoco.csv"
    if not report.is_file():
        raise WorkflowError(f"JaCoCo report not found: {report}")
    with report.open(encoding="utf-8-sig", newline="") as handle:
        rows = [row for row in csv.DictReader(handle) if row["CLASS"] == class_name]
    if len(rows) > 1:
        sources = list((repo / "src/main/java").rglob(class_name + ".java"))
        if len(sources) != 1:
            raise WorkflowError(f"Ambiguous JaCoCo class: {class_name}")
        match = re.search(r"^\s*package\s+([\w.]+)\s*;",
                          sources[0].read_text(encoding="utf-8"), re.MULTILINE)
        rows = [row for row in rows if match and
                row["PACKAGE"].replace("/", ".") == match[1]]
    if len(rows) != 1:
        raise WorkflowError(f"Class not uniquely found in JaCoCo: {class_name}")
    return rows[0]


def test_suites(repo: Path) -> list[dict]:
    reports = sorted((repo / "target/surefire-reports").glob("TEST-*.xml"))
    if not reports:
        raise WorkflowError("No fresh Surefire TEST-*.xml reports found.")
    result = []
    for path in reports:
        root = ET.parse(path).getroot()
        suites = [root] if root.tag == "testsuite" else list(root.findall("testsuite"))
        for suite in suites:
            result.append({"name": suite.get("name", ""),
                           **{key: int(suite.get(key, "0")) for key in ("tests", "failures", "errors", "skipped")},
                           "time": float(suite.get("time", "0"))})
    return result


def metrics(repo: Path, class_name: str) -> dict:
    row = class_row(repo, class_name)
    result = {}
    for prefix, name in (("LINE", "lines"), ("BRANCH", "branches")):
        covered = int(row[prefix + "_COVERED"])
        total = covered + int(row[prefix + "_MISSED"])
        result[name + "Covered"] = covered
        result[name + "Total"] = total
        result["lineCoverage" if prefix == "LINE" else "branchCoverage"] = (
            round(covered / total * 100, 2) if total else 100.0)
    result.update(tests=0, failures=0, errors=0, skipped=0, testTime=0.0)
    for suite in test_suites(repo):
        for key in ("tests", "failures", "errors", "skipped"):
            result[key] += suite[key]
        result["testTime"] += suite["time"]
    result["testTime"] = round(result["testTime"], 3)
    if result["failures"] or result["errors"]:
        raise WorkflowError("Surefire reports contain failures/errors.")
    return result


def collect(repo: Path, class_name: str, evidence: Path | None = None) -> dict:
    build = fresh_reports(repo)
    result = metrics(repo, class_name)
    if evidence:
        write_json(evidence, {"version": 1, "targetClass": class_name,
                              "build": build, "jacoco": class_row(repo, class_name),
                              "surefire": test_suites(repo), "metrics": result})
    return result


def changes(repo: Path, start: str, end: str, destination: Path) -> dict:
    run(repo, "git", "merge-base", "--is-ancestor", start, end)
    revision = f"{start}..{end}"
    # Preserve byte-for-byte Git output, including binary patches and LF endings.
    diff = subprocess.run(["git", "diff", "--binary", "--full-index", revision],
                          cwd=repo, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if diff.returncode:
        raise WorkflowError("git diff failed: " + diff.stderr.decode("utf-8", "replace"))
    destination.write_bytes(diff.stdout)
    files = run(repo, "git", "diff", "--name-only", revision).splitlines()
    additions = deletions = 0
    for line in run(repo, "git", "diff", "--numstat", revision).splitlines():
        parts = line.split("\t")
        if len(parts) >= 2:
            additions += int(parts[0]) if parts[0].isdigit() else 0
            deletions += int(parts[1]) if parts[1].isdigit() else 0
    return {"ChangedFiles": len(files), "Insertions": additions,
            "Deletions": deletions, "files": files}


def append_csv(path: Path, result: dict) -> None:
    """Write the current CSV schema atomically; retries never duplicate a run."""
    old_rows = []
    fields = list(result)
    if path.exists():
        with path.open(encoding="utf-8-sig", newline="") as handle:
            reader = csv.DictReader(handle)
            existing_fields = list(reader.fieldnames or [])
            old_rows = list(reader)
            if existing_fields != fields:
                additions = {"PromptProvider", "PromptModel", "PromptDurationSeconds", "CostStatus",
                             "UsageStartUTC", "UsageEndUTC", "UsageWindowSeconds"}
                renamed = ["MessageElapsedSeconds" if key == "InferenceSeconds" else key for key in existing_fields]
                if renamed != [key for key in fields if key not in additions] and renamed != fields:
                    raise WorkflowError("results.csv has a different schema. Use an empty benchmark store.")
                for row in old_rows:
                    if "InferenceSeconds" in row:
                        row["MessageElapsedSeconds"] = row.pop("InferenceSeconds")
                    for key in additions:
                        row.setdefault(key, "")
        if any(row.get("Id") == result["Id"] for row in old_rows):
            return  # Resume after a late error must not duplicate the result.
    formats = {"DurationSeconds": ".1f", "TestTimeBeforeSeconds": ".3f",
               "TestTimeAfterSeconds": ".3f", "LineCoverageBefore": ".2f",
               "LineCoverageAfter": ".2f", "BranchCoverageBefore": ".2f",
               "BranchCoverageAfter": ".2f"}
    row = {key: format(value, formats[key]) if key in formats and value is not None
           else value for key, value in result.items()}
    temporary = path.with_suffix(".csv.tmp")
    with temporary.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fields)
        writer.writeheader()
        writer.writerows(old_rows)
        writer.writerow(row)
    os.replace(temporary, path)
