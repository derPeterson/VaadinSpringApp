"""Vorschauauswahl einbetten, ohne die eigenständige Standardansicht zu ändern."""

import hashlib
import json
import re
from pathlib import Path

from lxml import etree, html

root = Path(__file__).parent
reference = root.parent
preview = reference.parents[2] / "target/designkritik-stitch/portal/preview"
marker_start = "<!-- appearance:start -->"
marker_end = "<!-- appearance:end -->"


def replace_block(source, markup, before):
    if marker_start in source:
        start = source.index(marker_start)
        end = source.index(marker_end, start) + len(marker_end)
        source = source[:start] + source[end:]
    return source.replace(before, marker_start + markup + marker_end + before, 1)


def logo_library():
    templates = []
    for letter in "abcdefghijklmno":
        logo = etree.fromstring(
            (reference.parent / f"service-logo/logo-{letter}.svg").read_bytes()
        )
        logo.attrib.pop("role", None)
        logo.attrib.pop("aria-label", None)
        logo.attrib.update(
            {
                "class": "w-7 h-7 flex-shrink-0",
                "width": "28",
                "height": "28",
                "aria-hidden": "true",
                "focusable": "false",
                "style": "transform:scale(1.4285714286);transform-origin:center",
                "data-service-logo": letter,
                "data-preview-logo": letter,
            }
        )
        identifiers = {
            node.get("id"): f"preview-{letter}-{node.get('id')}"
            for node in logo.iter()
            if node.get("id")
        }
        for node in logo.iter():
            if node.get("id") in identifiers:
                node.set("id", identifiers[node.get("id")])
            for key, value in list(node.attrib.items()):
                for old, new in identifiers.items():
                    value = value.replace(f"url(#{old})", f"url(#{new})")
                node.set(key, value)
        templates.append(etree.tostring(logo, encoding="unicode"))
    return '<template id="preview-logo-library">' + "".join(templates) + "</template>"


def appearance_markup(nonce, backgrounds):
    allowed = "reference," + ",".join(item["id"] for item in backgrounds)
    style = """
      #preview-sky{display:none;position:fixed;inset:0;z-index:-1;pointer-events:none;background-color:#050811}
      body[data-preview-background]:not([data-preview-background=reference]){isolation:isolate;background:#050811!important}
      body[data-preview-background]:not([data-preview-background=reference]) #preview-sky{display:block}
      body[data-preview-background]:not([data-preview-background=reference]) #sourceBackground,
      body[data-preview-background]:not([data-preview-background=reference]) .planet-glow-curve,
      body[data-preview-background]:not([data-preview-background=reference]) .star-canvas-overlay{display:none!important}
    """
    for item in backgrounds:
        assert re.fullmatch(r"[a-z][a-z0-9-]*", item["id"])
        assert not any(token in item["css"] for token in ("url(", "</", "{", "}"))
        style += f'body[data-preview-background="{item["id"]}"] #preview-sky{{background:{item["css"]};background-size:{item["size"]}}}'
    return (
        f'<style id="preview-appearance-style">{style}</style>'
        + f'<div id="preview-sky" aria-hidden="true" data-allowed="{allowed}"></div>'
        + logo_library()
        + f'<script nonce="{nonce}" id="preview-appearance-controller">'
        + (root / "erscheinungsbild.js").read_text(encoding="utf-8")
        + "</script>"
    )


def viewer_markup(backgrounds):
    names = [
        "Orbitale Rosette",
        "Stern und Umlaufbahn",
        "Abstraktes Portal",
        "Farbige Blüte",
        "Geschwungene Flügel",
        "Vierfarbiger Ring",
        "Verschränkte Formen",
        "Prisma",
        "Offener Orbit",
        "Orbital-Korona",
        "Pulsar-Nova",
        "Nebula-Vortex",
        "Korona-Finsternis",
        "Astro-Sextant",
        "Kometen-Drift",
    ]
    controls = '<div id="preview-appearance-controls" class="preview-appearance-controls"><label>Service-Logo <select id="preview-logo">'
    for letter, name in zip("abcdefghijklmno", names, strict=True):
        selected = ' selected=""' if letter == "k" else ""
        controls += (
            f'<option value="{letter}"{selected}>{letter.upper()} · {name}</option>'
        )
    controls += '</select></label><label>Hintergrund <select id="preview-background"><option value="reference">Aktuelle Referenz</option>'
    for item in backgrounds:
        controls += f'<option value="{item["id"]}">{item["name"]}</option>'
    return (
        controls
        + '</select></label><a href="admin-entwuerfe.html">Weitere Adminentwürfe A/B</a></div>'
    )


def apply():
    backgrounds = json.loads((root / "hintergruende.json").read_text(encoding="utf-8"))
    for name in ("portal", "public", "admin-inhalt"):
        path = reference / f"{name}.html"
        source = path.read_text(encoding="utf-8")
        nonce = html.fromstring(source).xpath("//script[@nonce]")[0].get("nonce")
        source = replace_block(source, appearance_markup(nonce, backgrounds), "</body>")
        path.write_text(source, encoding="utf-8")
    for name, inner in (
        ("index", "portal"),
        ("visitors", "public"),
        ("admin", "admin-inhalt"),
    ):
        path = reference / f"{name}.html"
        source = path.read_text(encoding="utf-8")
        document = html.fromstring(source)
        for fieldset in document.xpath('//fieldset[.//input[@name="background"]]'):
            fieldset.getparent().remove(fieldset)
        controls = document.xpath('//*[@id="preview-appearance-controls"]')
        for control in controls:
            control.getparent().remove(control)
        toolbar = document.xpath('//*[@class="preview-toolbar"]')[0]
        toolbar.append(html.fromstring(viewer_markup(backgrounds)))
        source = html.tostring(document, encoding="unicode", doctype="<!doctype html>")
        source = source.replace(
            "background:document.querySelector('[name=\"background\"]:checked').value,",
            "",
        )
        markup = "<style>.preview-appearance-controls{display:flex;flex-wrap:wrap;gap:12px 24px;width:100%}.preview-appearance-controls label{max-width:100%;flex-wrap:wrap}.preview-appearance-controls select{background:#091120;color:#e2e8f0;border:1px solid #ffffff26;border-radius:6px;padding:7px;max-width:100%;font:inherit}body[data-size=auto] .preview-viewport{box-sizing:border-box;height:calc(100dvh - var(--preview-toolbar-height,220px) - 40px);min-height:360px}</style>"
        markup += (
            '<script nonce="cosmos-viewer" id="preview-appearance-sender">'
            + (root / "erscheinungsbild-viewer.js").read_text(encoding="utf-8")
            + "</script>"
        )
        source = replace_block(source, markup, "</body>")
        revision = hashlib.sha256(
            (reference / f"{inner}.html").read_bytes()
        ).hexdigest()[:12]
        # Nur die Revision des zugehörigen Vorschauframes erneuern.
        source = re.sub(
            rf'{inner}\.html(?:\?rev=[a-f0-9]+)?(?=")',
            f"{inner}.html?rev={revision}",
            source,
        )
        path.write_text(source, encoding="utf-8")
    for name in ("portal", "public", "admin-inhalt", "index", "visitors", "admin"):
        if preview.exists():
            (preview / f"{name}.html").write_bytes(
                (reference / f"{name}.html").read_bytes()
            )


if __name__ == "__main__":
    apply()
