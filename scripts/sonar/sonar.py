"""SonarQube für dieses Projekt: MCP-Start, Leseprüfung und abgesicherter main-Scan."""

import argparse
import json
import os
import re
import subprocess
import sys
import time
from contextlib import contextmanager
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import HTTPRedirectHandler, ProxyHandler, Request, build_opener

import tomllib
from credentials import CredentialError, load_token, store_token_dialog

ROOT = Path(__file__).resolve().parents[2]
PROJECT = "vaadin-spring-app"
HOST_URL = "http://localhost:9000"
MCP_IMAGE = (
    "sonarsource/sonarqube-mcp:1.29.0.4488@"
    "sha256:e1a31da56d1006e45bfb2b0f4f4a9ce2de5051d84e07446c5dafabdff13b0415"
)
SCANNER = "org.sonarsource.scanner.maven:sonar-maven-plugin:5.8.0.7211:sonar"
METRICS = (
    "coverage,line_coverage,branch_coverage,tests,bugs,vulnerabilities,code_smells"
)
TOOLSETS = "issues,projects,quality-gates,measures,coverage,rules,security-hotspots"


class SonarError(RuntimeError):
    pass


class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, request, file, code, message, headers, new_url):
        # Der lokale Token darf nie an ein Weiterleitungsziel gelangen.
        return None


class SonarApi:
    def __init__(self, token):
        self.token = token
        self.opener = build_opener(ProxyHandler({}), NoRedirect())

    def get(self, path, **parameters):
        url = f"{HOST_URL}/api/{path}?{urlencode(parameters)}"
        request = Request(url, headers={"Authorization": f"Bearer {self.token}"})
        try:
            with self.opener.open(request, timeout=30) as response:
                result = json.load(response)
        except HTTPError as error:
            error.close()
            raise SonarError(
                f"Sonar-API {path}: HTTP {error.code}; keine vollständigen Ergebnisse."
            ) from None
        except (URLError, TimeoutError, OSError, ValueError):
            raise SonarError(
                f"Sonar-API {path}: Verbindung oder JSON-Antwort fehlerhaft."
            ) from None
        if not isinstance(result, dict):
            raise SonarError(f"Sonar-API {path}: unerwartetes Antwortformat.")
        return result

    def pages(self, path, collection, **parameters):
        """Alle Seiten verlangen; wechselnde/fehlende Summen sind kein Teilerfolg."""
        result = []
        expected_total = None
        keys = set()
        page = 1
        while True:
            answer = self.get(path, **parameters, p=page, ps=100)
            paging = answer.get("paging", {})
            if not isinstance(paging, dict):
                raise SonarError("Ungültige Seiteninformationen im Serverergebnis.")
            total = paging.get("total")
            entries = answer.get(collection)
            if type(total) is not int or total < 0 or not isinstance(entries, list):
                raise SonarError(
                    f"Sonar-API {path}: unvollständige Seiteninformationen."
                )
            if total > 10000:
                raise SonarError(
                    "Mehr als 10.000 Treffer: gezielte Teilabfragen erforderlich."
                )
            if expected_total is None:
                expected_total = total
            if total != expected_total or paging.get("pageIndex") != page:
                raise SonarError("Ergebnisumfang änderte sich während des Abrufs.")
            for entry in entries:
                key = entry.get("key") if isinstance(entry, dict) else None
                if not isinstance(key, str) or not key or key in keys:
                    raise SonarError(
                        "Fehlende oder doppelte Finding-ID im paginierten Ergebnis."
                    )
                keys.add(key)
            result.extend(entries)
            if len(result) == total:
                return result
            if not entries or len(result) > total:
                raise SonarError("Unvollständiges paginiertes Ergebnis.")
            page += 1

    def latest_analysis(self):
        analyses = self.get("project_analyses/search", project=PROJECT, ps=1).get(
            "analyses", []
        )
        if (
            not isinstance(analyses, list)
            or not analyses
            or not isinstance(analyses[0], dict)
            or not analyses[0].get("key")
        ):
            raise SonarError("Keine zuordenbare Projektanalyse vorhanden.")
        return analyses[0]


def git(*arguments):
    result = subprocess.run(
        ["git", *arguments],
        check=False,
        cwd=ROOT,
        capture_output=True,
        text=True,
        encoding="utf-8",
    )
    if result.returncode:
        raise SonarError("Git-Zustand konnte nicht sicher ermittelt werden.")
    return result.stdout.strip()


def require_clean_main():
    if git("branch", "--show-current") != "main":
        raise SonarError(
            "Scan verweigert: Das Community-Hauptprojekt darf nur main analysieren."
        )
    if git("status", "--porcelain"):
        raise SonarError(
            "Scan verweigert: Arbeitsstand enthält uncommittierte Änderungen."
        )
    return git("rev-parse", "HEAD")


def check_authentication(api):
    if api.get("authentication/validate").get("valid") is not True:
        raise SonarError("Der lokale Sonar-Zugang wurde nicht akzeptiert.")


def read_project(api):
    check_authentication(api)
    branches = api.get("project_branches/list", project=PROJECT).get("branches", [])
    if not isinstance(branches, list) or not any(
        isinstance(branch, dict)
        and branch.get("isMain") is True
        and branch.get("name") == "main"
        for branch in branches
    ):
        raise SonarError("Das Sonar-Hauptprojekt ist nicht eindeutig main zugeordnet.")
    before = api.latest_analysis()
    findings = api.pages(
        "issues/search", "issues", components=PROJECT, issueStatuses="OPEN,CONFIRMED"
    )
    exceptions = api.pages(
        "issues/search",
        "issues",
        components=PROJECT,
        issueStatuses="ACCEPTED,FALSE_POSITIVE",
    )
    hotspots = api.pages("hotspots/search", "hotspots", project=PROJECT)
    gate = api.get("qualitygates/project_status", analysisId=before["key"]).get(
        "projectStatus"
    )
    component = api.get(
        "measures/component", component=PROJECT, metricKeys=METRICS
    ).get("component", {})
    after = api.latest_analysis()
    if before["key"] != after["key"]:
        raise SonarError(
            "Die Analyse änderte sich während des Abrufs; erneut vollständig lesen."
        )
    if not isinstance(gate, dict) or gate.get("status") not in {
        "OK",
        "ERROR",
        "WARN",
        "NONE",
    }:
        raise SonarError("Quality Gate fehlt oder ist unvollständig.")
    if (
        not isinstance(component, dict)
        or component.get("key") != PROJECT
        or not isinstance(component.get("measures"), list)
    ):
        raise SonarError("Projektmetriken fehlen oder sind unvollständig.")
    metrics = {}
    for item in component["measures"]:
        if not isinstance(item, dict) or not isinstance(item.get("metric"), str):
            raise SonarError("Ungültiger Metrikeintrag im Serverergebnis.")
        if item["metric"] in metrics:
            raise SonarError("Doppelte Metrik im Serverergebnis.")
        metrics[item["metric"]] = item.get("value")
    revision = before.get("revision")
    local_main = git("rev-parse", "main")
    return {
        "project": PROJECT,
        "branch": "main",
        "analysis": before,
        "localMainRevision": local_main,
        "sourceMatchesLocalMain": revision == local_main if revision else None,
        "qualityGate": gate,
        "measures": metrics,
        "openIssues": findings,
        "exceptions": exceptions,
        "hotspots": hotspots,
    }


def read_summary(snapshot):
    return {
        "project": snapshot["project"],
        "branch": snapshot["branch"],
        "analysisId": snapshot["analysis"]["key"],
        "analysisDate": snapshot["analysis"].get("date"),
        "analyzedRevision": snapshot["analysis"].get("revision"),
        "sourceMatchesLocalMain": snapshot["sourceMatchesLocalMain"],
        "openIssues": len(snapshot["openIssues"]),
        "exceptions": len(snapshot["exceptions"]),
        "hotspots": len(snapshot["hotspots"]),
        "qualityGate": snapshot["qualityGate"]["status"],
        "measures": snapshot["measures"],
    }


def start_mcp(token):
    environment = os.environ.copy()
    environment.update(
        {
            "SONARQUBE_TOKEN": token,
            "SONARQUBE_URL": "http://host.docker.internal:9000",
            "SONARQUBE_PROJECT_KEY": PROJECT,
            "SONARQUBE_TOOLSETS": TOOLSETS,
            "SONARQUBE_LOG_TO_FILE_DISABLED": "true",
        }
    )
    command = ["docker", "run", "--rm", "-i", "--init", "--pull=never"]
    for name in (
        "SONARQUBE_TOKEN",
        "SONARQUBE_URL",
        "SONARQUBE_PROJECT_KEY",
        "SONARQUBE_TOOLSETS",
        "SONARQUBE_LOG_TO_FILE_DISABLED",
    ):
        command.extend(["-e", name])
    command.append(MCP_IMAGE)
    # Nur der offizielle Server schreibt das MCP-Protokoll auf stdout.
    return subprocess.run(command, check=False, env=environment).returncode


def configure():
    config_path = ROOT / ".codex" / "config.toml"
    executable = Path(sys.executable).as_posix()
    script = (ROOT / "scripts" / "sonar" / "sonar.py").as_posix()
    content = (
        "# Lokal erzeugt durch scripts/sonar/sonar.py configure; ohne Zugangsdaten.\n"
        "[mcp_servers.sonarqube]\n"
        f"command = {json.dumps(executable)}\n"
        f"args = {json.dumps([script, 'mcp'])}\n"
        f"cwd = {json.dumps(ROOT.as_posix())}\n"
        'env_vars = ["LOCALAPPDATA", "SONARQUBE_TOKEN"]\n'
        "startup_timeout_sec = 60\n"
        "tool_timeout_sec = 90\n"
    )
    tomllib.loads(content)
    if config_path.exists() and config_path.read_text(encoding="utf-8") != content:
        raise SonarError(
            "Bestehende lokale MCP-Konfiguration bleibt erhalten; Einrichtung manuell abgleichen."
        )
    config_path.parent.mkdir(exist_ok=True)
    config_path.write_text(content, encoding="utf-8", newline="\n")
    print(
        "Projektbezogene MCP-Konfiguration eingerichtet. Im Codex-Client MCP neu laden."
    )


def run_maven(arguments, token):
    environment = os.environ.copy()
    environment.update({"SONAR_TOKEN": token, "SONAR_HOST_URL": HOST_URL})
    process = subprocess.Popen(
        [str(ROOT / "mvnw.cmd"), *arguments],
        cwd=ROOT,
        env=environment,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    with process:
        for line in process.stdout:
            print(line.replace(token, "[REDACTED]"), end="", flush=True)
        if process.wait():
            raise SonarError(
                "Maven-Prüfung/Scan fehlgeschlagen; keine erfolgreiche Analyseabnahme."
            )


def scan_arguments(revision, report_path):
    return [
        SCANNER,
        f"-Dsonar.projectKey={PROJECT}",
        f"-Dsonar.host.url={HOST_URL}",
        f"-Dsonar.scm.revision={revision}",
        f"-Dsonar.scanner.metadataFilePath={report_path.as_posix()}",
        "-Dsonar.scanner.skipJreProvisioning=true",
    ]


def wait_for_analysis(api, task_id, revision, timeout=600):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        task = api.get("ce/task", id=task_id).get("task", {})
        if (
            not isinstance(task, dict)
            or task.get("id") != task_id
            or task.get("componentKey") != PROJECT
        ):
            raise SonarError(
                "Serverauftrag passt nicht zum angeforderten Projekt/Task."
            )
        status = task.get("status")
        if status in {"FAILED", "CANCELED"}:
            raise SonarError(f"Serveranalyse endete mit {status}.")
        if status == "SUCCESS":
            analysis_id = task.get("analysisId")
            analysis = api.latest_analysis()
            if (
                not analysis_id
                or analysis["key"] != analysis_id
                or analysis.get("revision") != revision
            ):
                raise SonarError(
                    "Serveranalyse ist nicht eindeutig diesem Quellstand zugeordnet."
                )
            return analysis_id
        if status not in {"PENDING", "IN_PROGRESS"}:
            raise SonarError("Unbekannter Zustand des Serverauftrags.")
        time.sleep(2)
    raise SonarError("Serveranalyse noch nicht abgeschlossen; Zeitlimit erreicht.")


def require_idle_server(api):
    queue = api.get("ce/component", component=PROJECT)
    if queue.get("queue") or queue.get("current", {}).get("status") == "IN_PROGRESS":
        raise SonarError("Für dieses Projekt läuft bereits ein Serverauftrag.")


@contextmanager
def scan_lock():
    directory = ROOT / ".codex" / "sonar"
    directory.mkdir(parents=True, exist_ok=True)
    path = directory / "scan.lock"
    try:
        with path.open("x", encoding="utf-8") as lock:
            lock.write(str(os.getpid()))
    except FileExistsError:
        raise SonarError(
            "Lokaler Scan bereits gesperrt. Bestehenden Lauf zuerst prüfen."
        ) from None
    try:
        yield
    finally:
        path.unlink(missing_ok=True)


def scan(api, token):
    revision = require_clean_main()
    check_authentication(api)
    require_idle_server(api)
    version = subprocess.run(
        [str(ROOT / "mvnw.cmd"), "--version"],
        check=False,
        cwd=ROOT,
        capture_output=True,
        text=True,
    )
    if version.returncode or not re.search(
        r"Java version:\s*25(?:[.,\s]|$)", version.stdout
    ):
        raise SonarError("Build benötigt JDK 25; Maven-Version zuerst prüfen.")
    run_maven(["-f", "shared/pom.xml", "clean", "install"], token)
    run_maven(["spotless:check", "checkstyle:check"], token)
    run_maven(
        ["clean", "verify", "-Pproduction", "-Dvaadin.productionMode=true"], token
    )
    if require_clean_main() != revision:
        raise SonarError("Quellstand hat sich beim Build verändert; kein Upload.")
    directory = ROOT / ".codex" / "sonar"
    directory.mkdir(parents=True, exist_ok=True)
    report_path = directory / "scanner-task.properties"
    # Nur eigene frühere Task-Metadaten entfernen, damit kein alter Scan bestätigt wird.
    report_path.unlink(missing_ok=True)
    require_idle_server(api)
    run_maven(scan_arguments(revision, report_path), token)
    if not report_path.is_file():
        raise SonarError("Scanner hat keinen neuen Serverauftrag hinterlegt.")
    metadata = dict(
        line.split("=", 1)
        for line in report_path.read_text(encoding="utf-8").splitlines()
        if "=" in line
    )
    if (
        metadata.get("projectKey") != PROJECT
        or metadata.get("serverUrl", "").rstrip("/") != HOST_URL
    ):
        raise SonarError("Scanner-Metadaten passen nicht zum lokalen Hauptprojekt.")
    task_id = metadata.get("ceTaskId")
    if not task_id:
        raise SonarError("Scanner-Metadaten enthalten keinen Serverauftrag.")
    analysis_id = wait_for_analysis(api, task_id, revision)
    if require_clean_main() != revision:
        raise SonarError("Quellstand nach Upload verändert; Abnahme bleibt offen.")
    snapshot = read_project(api)
    if (
        snapshot["analysis"]["key"] != analysis_id
        or snapshot["analysis"].get("revision") != revision
    ):
        raise SonarError("Zwischenzeitlich wurde eine andere Analyse veröffentlicht.")
    print(json.dumps(read_summary(snapshot), indent=2, ensure_ascii=False))
    if snapshot["qualityGate"]["status"] != "OK":
        raise SonarError(
            "Serveranalyse abgeschlossen, Quality Gate jedoch nicht bestanden."
        )


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=["token", "configure", "mcp", "read", "scan"])
    args = parser.parse_args()
    try:
        if args.action == "token":
            store_token_dialog()
        elif args.action == "configure":
            configure()
        else:
            token = load_token()
            if args.action == "mcp":
                return start_mcp(token)
            api = SonarApi(token)
            if args.action == "scan":
                with scan_lock():
                    scan(api, token)
            else:
                print(
                    json.dumps(
                        read_summary(read_project(api)), indent=2, ensure_ascii=False
                    )
                )
    except (SonarError, CredentialError) as error:
        print(str(error), file=sys.stderr)
        return 1
    except (OSError, ValueError):
        print(
            "Lokale Sonar-Einrichtung fehlgeschlagen; Umgebung und Zugangsablage prüfen.",
            file=sys.stderr,
        )
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
