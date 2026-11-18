"""Lokale Releases vorbereiten und nach belegter Gesamt-QA unveränderlich markieren."""

import argparse
import hashlib
import json
import re
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET
import zipfile
from datetime import date
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
POMS = (
    "pom.xml",
    "app/pom.xml",
    "shared/pom.xml",
    "browser-tests/pom.xml",
    "coverage-report/pom.xml",
)
NAMESPACE = {"m": "http://maven.apache.org/POM/4.0.0"}
# Genau drei nichtnegative Zahlen ohne führende Nullen; optional der Maven-Entwicklungszusatz.
VERSION_PATTERN = re.compile(
    r"(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)(-SNAPSHOT)?"
)
REQUIRED_CHECKS = (
    "junit",
    "frontend",
    "offline",
    "coverage",
    "browser_de",
    "browser_en",
)


class ReleaseError(RuntimeError):
    """Der Release ist nicht hinreichend vorbereitet oder nachgewiesen."""


def version_numbers(version):
    match = VERSION_PATTERN.fullmatch(version)
    if not match:
        raise ReleaseError(f"Ungültige Projektversion: {version}")
    return tuple(int(match[index]) for index in (1, 2, 3))


def git(root, *arguments):
    result = subprocess.run(
        ["git", *arguments], cwd=root, capture_output=True, text=True, encoding="utf-8"
    )
    if result.returncode:
        raise ReleaseError(f"Git-Schritt fehlgeschlagen: {arguments[0]}")
    return result.stdout.strip()


def require_clean(root):
    if git(root, "status", "--porcelain"):
        raise ReleaseError(
            "Arbeitsstand ist nicht sauber; eigene Änderungen zuerst prüfen und committen."
        )
    return git(root, "rev-parse", "HEAD")


def project_version(root):
    versions = []
    for filename in POMS:
        document = ET.parse(root / filename).getroot()
        own_version = document.find("m:version", NAMESPACE)
        parent = document.find("m:parent", NAMESPACE)
        if filename in ("pom.xml", "app/pom.xml"):
            version = own_version.text if own_version is not None else None
        else:
            if (
                parent is None
                or parent.findtext("m:artifactId", namespaces=NAMESPACE)
                != "vaadin-spring-app"
            ):
                raise ReleaseError(f"Unerwarteter Projekt-Parent: {filename}")
            version = parent.findtext("m:version", namespaces=NAMESPACE)
        if not version:
            raise ReleaseError(f"Projektversion fehlt: {filename}")
        version_numbers(version)
        versions.append(version)
        for dependency in document.findall("m:dependencies/m:dependency", NAMESPACE):
            if (
                dependency.findtext("m:groupId", namespaces=NAMESPACE)
                == "de.derpeterson"
            ):
                if (
                    dependency.findtext("m:version", namespaces=NAMESPACE)
                    != "${project.version}"
                ):
                    raise ReleaseError(
                        f"Interne Abhängigkeit verwendet keine Projektversion: {filename}"
                    )
    if len(set(versions)) != 1:
        raise ReleaseError("Die Projekt-POMs enthalten unterschiedliche Versionen.")
    return versions[0]


def version_updates(root, old_version, new_version):
    version_numbers(new_version)
    updates = {}
    # Die erste version-Eigenschaft ist in Root/App die eigene, in den übrigen POMs der Parent.
    pattern = re.compile(r"(<version>)" + re.escape(old_version) + r"(</version>)")
    for filename in POMS:
        path = root / filename
        content = path.read_text(encoding="utf-8")
        first_version = re.search(r"<version>([^<]+)</version>", content)
        if first_version is None or first_version[1] != old_version:
            raise ReleaseError(
                f"Projektversion steht nicht an der erwarteten Stelle: {filename}"
            )
        updates[path] = pattern.sub(
            lambda match: match[1] + new_version + match[2], content, count=1
        )
    return updates


def apply_updates(updates):
    # Alle Inhalte sind vor dem ersten Schreibzugriff validiert. Keine Git-Aktion ausführen.
    for path, content in updates.items():
        path.write_text(content, encoding="utf-8")


def changelog_sections(content):
    # Releaseüberschriften stehen allein auf einer Zeile, mit exakter ISO-Datumsangabe.
    heading = re.compile(
        r"^## \[([^\]\n]+)\](?: - ([0-9]{4}-[0-9]{2}-[0-9]{2}))?$", re.MULTILINE
    )
    matches = list(heading.finditer(content))
    if not matches or matches[0][1] != "Unreleased":
        raise ReleaseError("CHANGELOG.md muss mit einem Unreleased-Abschnitt beginnen.")
    sections = {}
    for index, match in enumerate(matches):
        name, released_on = match[1], match[2]
        if name in sections:
            raise ReleaseError(f"Doppelter Changelog-Abschnitt: {name}")
        if name == "Unreleased":
            if released_on:
                raise ReleaseError(
                    "Unreleased darf kein Veröffentlichungsdatum tragen."
                )
        else:
            version_numbers(name)
            if name.endswith("-SNAPSHOT") or not released_on:
                raise ReleaseError(
                    "Releaseabschnitte benötigen eine feste Version und ein Datum."
                )
            try:
                parsed_date = date.fromisoformat(released_on)
            except ValueError as error:
                raise ReleaseError("Ungültiges Releasedatum.") from error
            if parsed_date > date.today():
                raise ReleaseError("Das Releasedatum darf nicht in der Zukunft liegen.")
        end = matches[index + 1].start() if index + 1 < len(matches) else len(content)
        body = content[match.end() : end].strip()
        # Ganze Sprachüberschriften am Zeilenanfang trennen; Leerraum bis zum Inhalt gehört nicht dazu.
        languages = re.split(r"^### (DE|EN)\s*$", body, flags=re.MULTILINE)
        if (
            len(languages) != 5
            or languages[0].strip()
            or languages[1] != "DE"
            or languages[3] != "EN"
        ):
            raise ReleaseError(
                f"Abschnitt {name} benötigt genau DE und EN in dieser Reihenfolge."
            )
        entries = [part.strip() for part in (languages[2], languages[4])]
        if name != "Unreleased" and any(not entry for entry in entries):
            raise ReleaseError(f"Leere Release-Notizen: {name}")
        if bool(entries[0]) != bool(entries[1]):
            raise ReleaseError(f"Unvollständige Übersetzung: {name}")
        sections[name] = {
            "body": body,
            "start": match.start(),
            "end": end,
            "entries": entries,
        }
    return sections


def release_tags(root):
    tags = []
    for tag in git(root, "tag", "--list", "v*").splitlines():
        version = tag[1:]
        if VERSION_PATTERN.fullmatch(version) and not version.endswith("-SNAPSHOT"):
            tags.append(version)
    return sorted(tags, key=version_numbers)


def preview(root):
    current = project_version(root)
    tags = release_tags(root)
    notes = changelog_sections((root / "CHANGELOG.md").read_text(encoding="utf-8"))
    candidate = current.removesuffix("-SNAPSHOT")
    return {
        "developmentVersion": current,
        "latestRelease": tags[-1] if tags else None,
        "candidate": candidate,
        "kind": "initial"
        if not tags
        else "patch"
        if version_numbers(candidate)[:2] == version_numbers(tags[-1])[:2]
        else "minor-or-major",
        "unreleasedNotesPresent": bool(notes["Unreleased"]["entries"][0]),
        "dirty": bool(git(root, "status", "--porcelain")),
        "changesWritten": False,
    }


def prepare(root, version):
    require_clean(root)
    current = project_version(root)
    version_numbers(version)
    if not current.endswith("-SNAPSHOT") or version != current.removesuffix(
        "-SNAPSHOT"
    ):
        raise ReleaseError("Der Release muss zur aktuellen Entwicklungsversion passen.")
    if version.endswith("-SNAPSHOT"):
        raise ReleaseError("Ein Release darf kein Snapshot sein.")
    tags = release_tags(root)
    if tags and version_numbers(version) <= version_numbers(tags[-1]):
        raise ReleaseError(
            "Die Releaseversion muss größer als der letzte Release sein."
        )
    path = root / "CHANGELOG.md"
    content = path.read_text(encoding="utf-8")
    sections = changelog_sections(content)
    if version in sections or not all(sections["Unreleased"]["entries"]):
        raise ReleaseError(
            "Releaseversion bereits vorhanden oder unveröffentlichte Notizen fehlen."
        )
    unreleased = sections["Unreleased"]
    new_section = f"## [Unreleased]\n\n### DE\n\n### EN\n\n## [{version}] - {date.today().isoformat()}\n\n{unreleased['body']}\n\n"
    changed = (
        content[: unreleased["start"]] + new_section + content[unreleased["end"] :]
    ).rstrip() + "\n"
    changelog_sections(changed)
    updates = version_updates(root, current, version)
    updates[path] = changed
    apply_updates(updates)
    return {
        "candidate": version,
        "tagCreated": False,
        "nextStep": "Review, Kandidatencommit und vollständige QA",
    }


def read_properties(content):
    values = {}
    for line in content.splitlines():
        if line.strip() and not line.lstrip().startswith(("#", "!")) and "=" in line:
            key, value = line.split("=", 1)
            values[key.strip()] = value.strip()
    return values


def verify_artifact(root, artifact):
    revision = require_clean(root)
    version = project_version(root)
    if version.endswith("-SNAPSHOT"):
        raise ReleaseError(
            "Entwicklungsartefakte werden nicht als Release freigegeben."
        )
    sections = changelog_sections((root / "CHANGELOG.md").read_text(encoding="utf-8"))
    if version not in sections:
        raise ReleaseError("Release-Notizen für die Kandidatenversion fehlen.")
    with zipfile.ZipFile(artifact) as jar:
        metadata_path = "META-INF/build-info.properties"
        if jar.getinfo(metadata_path).file_size > 16384:
            raise ReleaseError("Unerwartet große Build-Metadaten.")
        properties = read_properties(jar.read(metadata_path).decode("utf-8"))
        if (
            properties.get("build.version") != version
            or properties.get("build.revision") != revision
        ):
            raise ReleaseError(
                "Artefaktversion oder Git-Revision passt nicht zum Kandidaten."
            )
        if properties.get("build.dirty") != "false":
            raise ReleaseError(
                "Das Artefakt wurde nicht aus einem sauberen Quellstand gebaut."
            )
        if (
            "BOOT-INF/classes/de/derpeterson/app/Application.class"
            not in jar.namelist()
        ):
            raise ReleaseError("Kein ausführbares Anwendungsartefakt.")
    with artifact.open("rb") as stream:
        digest = hashlib.file_digest(stream, "sha256").hexdigest()
    return {"version": version, "commit": revision, "sha256": digest}


def require_qa(root, artifact_metadata):
    state = json.loads((root / ".codex/qa/state.json").read_text(encoding="utf-8"))
    if not isinstance(state, dict):
        raise ReleaseError("Ungültiger QA-Zustand.")
    success = state.get("last_success")
    if not isinstance(success, dict) or not isinstance(
        success.get("release_qualification"), dict
    ):
        raise ReleaseError("Vollständige Releasequalifikation fehlt.")
    qualification = success["release_qualification"]
    if state.get("current") is not None or success.get("phase") != "complete":
        raise ReleaseError("Eine vollständige abgeschlossene Gesamt-QA fehlt.")
    if success.get("commit") != artifact_metadata["commit"]:
        raise ReleaseError("Die erfolgreiche QA gehört zu einer anderen Quellrevision.")
    for key in ("version", "commit", "sha256"):
        if qualification.get(key) != artifact_metadata[key]:
            raise ReleaseError(
                "QA-Freigabe passt nicht exakt zu Version, Revision und Artefakt."
            )
    checks = qualification.get("checks", {})
    if not isinstance(checks, dict) or any(
        checks.get(check) != "passed" for check in REQUIRED_CHECKS
    ):
        raise ReleaseError(
            "Erforderliche QA-Nachweise fehlen oder sind nicht erfolgreich."
        )
    if (
        qualification.get("quality_gate") != "OK"
        or qualification.get("sonar_revision") != artifact_metadata["commit"]
    ):
        raise ReleaseError("Kein grünes Sonar Quality Gate für den Kandidaten belegt.")
    image = qualification.get("browser_image_de")
    # Vollständige Docker-SHA-256-ID: Präfix plus exakt 64 kleine Hexzeichen, keine Teilkennung.
    if (
        not isinstance(image, str)
        or not re.fullmatch(r"sha256:[0-9a-f]{64}", image)
        or image != qualification.get("browser_image_en")
    ):
        raise ReleaseError("DE und EN wurden nicht am selben Image bestätigt.")
    return qualification


def tag_release(root, artifact):
    metadata = verify_artifact(root, artifact)
    qualification = require_qa(root, metadata)
    version = metadata["version"]
    if version in release_tags(root):
        raise ReleaseError(
            "Der Release-Tag existiert bereits und wird nicht verändert."
        )
    directory = root / ".codex/releases" / version
    directory.mkdir(parents=True, exist_ok=True)
    archived_jar = directory / f"app-{version}.jar"
    if archived_jar.exists():
        with archived_jar.open("rb") as stream:
            if hashlib.file_digest(stream, "sha256").hexdigest() != metadata["sha256"]:
                raise ReleaseError(
                    "Ein abweichendes archiviertes Artefakt wird nicht überschrieben."
                )
    else:
        with artifact.open("rb") as source, archived_jar.open("xb") as target:
            shutil.copyfileobj(source, target)
    with archived_jar.open("rb") as stream:
        if hashlib.file_digest(stream, "sha256").hexdigest() != metadata["sha256"]:
            raise ReleaseError(
                "Das kopierte Artefakt entspricht nicht der geprüften SHA-256."
            )
    receipt = {
        **metadata,
        "tag": f"v{version}",
        "artifact": archived_jar.name,
        "qa": qualification,
    }
    manifest = directory / "manifest.json"
    if (
        manifest.exists()
        and json.loads(manifest.read_text(encoding="utf-8")) != receipt
    ):
        raise ReleaseError(
            "Ein abweichender Release-Nachweis wird nicht überschrieben."
        )
    if not manifest.exists():
        manifest.write_text(
            json.dumps(receipt, indent=2, ensure_ascii=False) + "\n", encoding="utf-8"
        )
    # Nach Kopieren/Prüfen noch einmal gegen parallel veränderte Quellen absichern.
    if require_clean(root) != metadata["commit"]:
        raise ReleaseError("Quellrevision während der Releasevorbereitung verändert.")
    git(
        root, "tag", "-a", f"v{version}", metadata["commit"], "-m", f"Release {version}"
    )
    return receipt


def next_snapshot(root):
    revision = require_clean(root)
    current = project_version(root)
    if current.endswith("-SNAPSHOT") or current not in release_tags(root):
        raise ReleaseError("Zuerst den geprüften aktuellen Release markieren.")
    if git(root, "rev-list", "-n", "1", f"v{current}") != revision:
        raise ReleaseError("Die aktuelle Revision ist nicht der markierte Release.")
    major, minor, patch = version_numbers(current)
    following = f"{major}.{minor}.{patch + 1}-SNAPSHOT"
    apply_updates(version_updates(root, current, following))
    return {
        "developmentVersion": following,
        "nextStep": "Snapshotänderung separat committen",
    }


def main(arguments=None):
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    commands.add_parser("preview", help="Version und Notizen ohne Änderungen prüfen")
    candidate = commands.add_parser(
        "prepare", help="Kandidatenversion und zweisprachige Notizen vorbereiten"
    )
    candidate.add_argument("--version", required=True)
    for command in ("verify", "tag"):
        subparser = commands.add_parser(
            command,
            help="Artefakt prüfen"
            if command == "verify"
            else "Nach vollständiger QA lokal archivieren und taggen",
        )
        subparser.add_argument(
            "--artifact", type=Path, default=ROOT / "app/target/app.jar"
        )
    commands.add_parser(
        "next", help="Nach dem Release den nächsten Patch-Snapshot setzen"
    )
    options = parser.parse_args(arguments)
    try:
        if options.command == "preview":
            result = preview(ROOT)
        elif options.command == "prepare":
            result = prepare(ROOT, options.version)
        elif options.command == "verify":
            result = verify_artifact(ROOT, options.artifact.resolve())
        elif options.command == "tag":
            result = tag_release(ROOT, options.artifact.resolve())
        else:
            result = next_snapshot(ROOT)
        print(json.dumps(result, ensure_ascii=False, indent=2))
        return 0
    except (
        ReleaseError,
        OSError,
        ValueError,
        KeyError,
        ET.ParseError,
        zipfile.BadZipFile,
    ) as error:
        print(f"Release nicht abgeschlossen: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
