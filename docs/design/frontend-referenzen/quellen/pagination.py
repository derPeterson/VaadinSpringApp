"""Gestaltete Seitengrößenauswahl in die eigenständigen HTML-Referenzen einbetten."""

import hashlib
import re
from pathlib import Path

from lxml import etree, html

root = Path(__file__).parent
reference = root.parent
preview = reference.parents[2] / "target/designkritik-stitch/portal/preview"


def harmonize_document(document):
    for tag, identifier, filename in (
        ("style", "reference-pagination-style", "pagination.css"),
        ("script", "reference-pagination-controller", "pagination.js"),
    ):
        existing = document.xpath(f'//*[@id="{identifier}"]')
        attributes = {"id": identifier}
        if tag == "script":
            attributes["nonce"] = document.xpath("//script[@nonce]")[0].get("nonce")
        node = (
            existing[0]
            if existing
            else etree.SubElement(
                document.find("head" if tag == "style" else "body"), tag, attributes
            )
        )
        node.text = (root / filename).read_text(encoding="utf-8")


def apply():
    for name in ("portal.html", "admin-inhalt.html"):
        path = reference / name
        source = path.read_text(encoding="utf-8")
        document = html.fromstring(source)
        harmonize_document(document)
        for identifier in (
            "reference-pagination-style",
            "reference-pagination-controller",
        ):
            node = document.get_element_by_id(identifier)
            markup = html.tostring(node, encoding="unicode", with_tail=False)
            pattern = rf'<{node.tag}\b[^>]*id="{identifier}"[^>]*>[\s\S]*?</{node.tag}>'
            source, count = re.subn(
                pattern, lambda _, value=markup: value, source, count=1
            )
            if count == 0:
                closing = "</head>" if node.tag == "style" else "</body>"
                source = source.replace(closing, markup + closing, 1)
        path.write_text(source, encoding="utf-8")
        if preview.exists():
            (preview / name).write_bytes(path.read_bytes())
    portal_revision = hashlib.sha256(
        (reference / "portal.html").read_bytes()
    ).hexdigest()[:12]
    for path in reference.glob("*.html"):
        source = re.sub(
            r"portal\.html\?rev=[a-f0-9]+",
            "portal.html?rev=" + portal_revision,
            path.read_text(encoding="utf-8"),
        )
        path.write_text(source, encoding="utf-8")
        if preview.exists():
            (preview / path.name).write_bytes(path.read_bytes())
    # Der Inhalt hat durch den aktualisierten Portalzugang ebenfalls eine neue Revision.
    path = reference / "admin.html"
    revision = hashlib.sha256(
        (reference / "admin-inhalt.html").read_bytes()
    ).hexdigest()[:12]
    source, count = re.subn(
        r"admin-inhalt\.html\?rev=[a-f0-9]+",
        "admin-inhalt.html?rev=" + revision,
        path.read_text(encoding="utf-8"),
    )
    assert count == 1, path.name
    path.write_text(source, encoding="utf-8")
    if preview.exists():
        (preview / path.name).write_bytes(path.read_bytes())


if __name__ == "__main__":
    apply()
