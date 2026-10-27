"""Kandidat K in die gesicherten HTML-Referenzköpfe einbetten."""

import re
import runpy
from pathlib import Path

from lxml import etree

reference = Path(__file__).parents[1]
logo_path = reference.parent / "service-logo/logo-k.svg"


def logo_element():
    logo = etree.fromstring(logo_path.read_bytes())
    logo.attrib.pop("role", None)
    logo.attrib.pop("aria-label", None)
    logo.set("aria-hidden", "true")
    logo.set("focusable", "false")
    logo.set("class", "w-7 h-7 flex-shrink-0")
    logo.set("width", "28")
    logo.set("height", "28")
    logo.set("style", "transform:scale(1.4285714286);transform-origin:center")
    logo.set("data-service-logo", "k")
    # Eigener Namensraum für eingebettete Farbverläufe, getrennt von anderen SVGs.
    identifiers = {
        node.get("id"): "service-logo-" + node.get("id")
        for node in logo.iter()
        if node.get("id")
    }
    for node in logo.iter():
        if node.get("id") in identifiers:
            node.set("id", identifiers[node.get("id")])
        for name, value in list(node.attrib.items()):
            for before, after in identifiers.items():
                value = value.replace(f"url(#{before})", f"url(#{after})")
            node.set(name, value)
    return logo


def replace_header_logo(document):
    logos = document.xpath(
        '//header//svg[contains(concat(" ",normalize-space(@class)," ")," w-7 ")]'
    )
    assert len(logos) == 1, "Genau ein Servicezeichen im Kopf erwartet"
    old = logos[0]
    replacement = logo_element()
    replacement.tail = old.tail
    old.getparent().replace(old, replacement)


def update_snapshot(name):
    path = reference / name
    source = path.read_text(encoding="utf-8")
    # Nur das Kopfzeichen ersetzen; die übrige abgestimmte Momentaufnahme erhalten.
    pattern = r'<svg\b[^>]*class="[^"]*\bw-7\b[^"]*"[^>]*>[\s\S]*?</svg>'
    markup = etree.tostring(logo_element(), encoding="unicode", with_tail=False)
    # Die zusätzlichen Kandidaten liegen außerhalb des Headers in einer inerten
    # Vorschau-Bibliothek. Nur das tatsächlich angezeigte Kopfzeichen ersetzen.
    header = re.search(r"<header\b[\s\S]*?</header>", source)
    assert header is not None, name
    replacement, count = re.subn(pattern, lambda _: markup, header.group())
    assert count == 1, name
    source = source[: header.start()] + replacement + source[header.end() :]
    path.write_text(source, encoding="utf-8")
    preview = reference.parents[2] / "target/designkritik-stitch/portal/preview"
    if preview.exists():
        (preview / name).write_bytes(path.read_bytes())


if __name__ == "__main__":
    for name in ("portal.html", "public.html"):
        update_snapshot(name)
    runpy.run_path(str(Path(__file__).parent / "eingabefelder.py"))["refresh_viewers"]()
