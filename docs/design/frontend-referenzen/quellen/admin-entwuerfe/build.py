"""Getrennte Adminentwürfe aus aktueller Referenz und gesichertem Inventar bauen."""

import base64
import hashlib
import json
import runpy
import sys
from copy import deepcopy
from pathlib import Path

from lxml import etree, html

root = Path(__file__).parent
reference = root.parents[1]
preview = reference.parents[2] / "target/designkritik-stitch/portal/preview"
inventory = json.loads((root / "inventar.json").read_text(encoding="utf-8"))
inventory["configuration"] = runpy.run_path(str(root / "konfigurationstexte.py"))[
    "configuration_texts"
](inventory)
(root / "inventar.json").write_text(
    json.dumps(inventory, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
)
groups = {
    group: values["label"]
    for group, values in inventory["configuration"]["groups"].items()
}


def write(name, document):
    source = html.tostring(document, encoding="unicode", doctype="<!doctype html>")
    source = "\n".join(line.rstrip() for line in source.splitlines()) + "\n"
    (reference / name).write_text(source, encoding="utf-8")
    if preview.exists():
        (preview / name).write_text(source, encoding="utf-8")


document = html.fromstring(
    (reference / "admin-inhalt.html").read_text(encoding="utf-8")
)
for extension in document.xpath("//*[@data-admin-extension]"):
    extension.getparent().remove(extension)
document.find("body").attrib.pop("data-admin-config-approved", None)
document.find("body").attrib.pop("data-admin-dashboard-approved", None)

document.find("body").attrib.pop("data-admin-notifications-approved", None)
runpy.run_path(str(root.parent / "pagination.py"))["harmonize_document"](document)
base_nodes = set(document.iter())
document.find("head/title").text = "Cosmos · Administration · Entwürfe A/B"
document.find("body").set("data-admin-draft", "true")
navigation_icons = html.fromstring(
    (root / "navigation-icons.html").read_text(encoding="utf-8")
)
for symbol in navigation_icons.xpath(".//*[@data-symbol]"):
    document.get_element_by_id("vp-symbols").append(deepcopy(symbol))
etree.SubElement(document.find("head"), "style", id="admin-proposal-style").text = (
    root / "entwurf.css"
).read_text(encoding="utf-8")
users = document.xpath(
    '//*[contains(concat(" ",normalize-space(@class)," ")," au-content ")]'
)[0]
etree.SubElement(
    users.getparent(),
    "section",
    id="admin-proposal",
    attrib={"aria-label": "Adminentwürfe"},
)
dashboard_visual = etree.SubElement(
    document.find("body"), "template", id="ad-dashboard-visual"
)
etree.SubElement(
    dashboard_visual,
    "img",
    src="data:image/jpeg;base64,"
    + base64.b64encode(
        (root / "stitch/dashboard-platzhalter-motiv.jpg").read_bytes()
    ).decode("ascii"),
    alt="Dunkler Planet mit dezent leuchtendem türkisfarbenem Horizont im Weltall",
    width="512",
    height="286",
)
etree.SubElement(document.find("body"), "script", nonce="cosmos-admin").text = (
    "window.AdminDraftInventory = "
    + json.dumps(inventory, ensure_ascii=False).replace("</", "<\\/")
    + ";"
)
etree.SubElement(document.find("body"), "script", nonce="cosmos-admin").text = (
    root / "entwurf.js"
).read_text(encoding="utf-8")
for node in document.iter():
    if node not in base_nodes and node.getparent() in base_nodes:
        node.set("data-admin-extension", "")
write("admin-entwurf.html", document)

viewer = html.fromstring((reference / "admin.html").read_text(encoding="utf-8"))
for extension in viewer.xpath("//*[@data-admin-extension]"):
    extension.getparent().remove(extension)
base_viewer_nodes = set(viewer.iter())
viewer.find("head/title").text = "Cosmos · Administration · Entwürfe"
viewer.xpath('//h1[@class="preview-title"]')[
    0
].text = "Cosmos · Administration · Entwürfe"
notes = viewer.xpath('//p[@class="preview-note"]')
notes[
    0
].text = "Zusätzliche Sichtung der abgestimmten Adminbereiche: Dashboard, Benutzerverwaltung, Konfiguration, Mailtest und Benachrichtigungen B. Alle Bereiche sind in der festen Adminreferenz übernommen. Alle Aktionen sind lokal simuliert."
notes[1].clear()
notes[1].set("class", "preview-note")
for index, (label, href) in enumerate(
    (
        ("Angemeldete Referenz", "index.html"),
        ("Besucherreferenz", "visitors.html"),
        ("Abgenommene Benutzerverwaltung", "admin.html"),
        ("Direktansicht der Entwürfe", "admin-entwurf.html"),
    )
):
    node = etree.SubElement(notes[1], "a", href=href)
    node.text = label
    if index < 3:
        node.tail = " · "
toolbar = viewer.xpath('//div[@class="preview-toolbar"]')[0]
controls = etree.SubElement(
    toolbar, "div", attrib={"class": "preview-appearance-controls"}
)
page_label = etree.SubElement(controls, "label")
page_label.text = "Bereich "
page = etree.SubElement(page_label, "select", id="ad-page")
for value, label in (
    ("dashboard", "Dashboard"),
    ("users", "Benutzerverwaltung · bestehend"),
):
    etree.SubElement(page, "option", value=value).text = label
option_group = etree.SubElement(
    page, "optgroup", label="Konfiguration · alle acht Bereiche"
)
for group, label in groups.items():
    etree.SubElement(option_group, "option", value=f"settings:{group}").text = label
for value, label in (
    ("mail", inventory["mail"]["texts"]["title"]),
    ("send", "Interne Benachrichtigungen"),
):
    etree.SubElement(page, "option", value=value).text = label
variant_label = etree.SubElement(controls, "label")
variant_label.text = "Layout "
variant = etree.SubElement(variant_label, "select", id="ad-variant")
etree.SubElement(
    variant, "option", value="a"
).text = "A · Bild und Schnellzugriffe / Formulare"
etree.SubElement(
    variant, "option", value="b"
).text = "B · Kompakte Zeilen / Arbeitsbereich mit Vorschau"
state_label = etree.SubElement(controls, "label")
state_label.text = "Beispielzustand "
state = etree.SubElement(state_label, "select", id="ad-state")
for value, label in (
    ("ready", "Ausgangszustand"),
    ("empty", "Leerer Bestand"),
    ("maintenance", "Wartungsmodus"),
    ("unavailable", "Technisch nicht verfügbar"),
    ("validation", "Pflichteingabe fehlt"),
    ("error", "Fehler / ungültiger Wert"),
    ("success", "Erfolg · simuliert"),
    ("permission-denied", "Keine Berechtigung"),
    ("preview-valid", "Aktuelle Vorschau"),
    ("preview-invalidated", "Vorschau nach Änderung ungültig"),
    ("no-recipients", "Keine berechtigten Empfänger"),
):
    etree.SubElement(state, "option", value=value).text = label
frame = viewer.get_element_by_id("account-preview")
revision = hashlib.sha256((reference / "admin-entwurf.html").read_bytes()).hexdigest()[
    :12
]
frame.set("src", "admin-entwurf.html?rev=" + revision)
frame.set("title", "Administration · zusätzliche Layoutentwürfe")
etree.SubElement(viewer.find("body"), "script", nonce="cosmos-viewer").text = (
    root / "viewer.js"
).read_text(encoding="utf-8")
for node in viewer.iter():
    if node not in base_viewer_nodes and node.getparent() in base_viewer_nodes:
        node.set("data-admin-extension", "")
write("admin-entwuerfe.html", viewer)
print(
    "Adminsichtung mit 12 Bereichen, 30 Konfigurationsfeldern und Varianten A/B erzeugt."
)

# Die drei Feldhilfe-Ideen bleiben eine eigene Sichtung außerhalb der normalen Referenz.
help_document = html.fromstring(
    (reference / "admin-entwurf.html").read_text(encoding="utf-8")
)
help_document.find("head/title").text = "Cosmos · Konfiguration · Feldhilfe-Vorschläge"
etree.SubElement(
    help_document.find("head"), "style", id="field-help-proposal-style"
).text = (root / "hilfe-vorschlaege.css").read_text(encoding="utf-8")
etree.SubElement(help_document.find("body"), "script", nonce="cosmos-admin").text = (
    root / "hilfe-vorschlaege.js"
).read_text(encoding="utf-8")
write("konfiguration-hilfe.html", help_document)

help_viewer = deepcopy(viewer)
help_viewer.find("head/title").text = "Cosmos · Feldhilfe · Referenz-Nachbauten"
help_viewer.xpath('//h1[@class="preview-title"]')[
    0
].text = "Cosmos · Konfiguration · Referenz-Nachbauten"
help_viewer.xpath('//p[@class="preview-note"]')[0].text = (
    "Eigene Nachbauten nach Stitch-Ideen, mit angepasstem Layout und unseren Referenzkomponenten. Keine originalgetreue Wiedergabe. "
    "Historischer Vergleich; die freigegebene Konfiguration verwendet inzwischen das rechte Hilfeoverlay C. Alle Aktionen sind synthetisch."
)
original_link = etree.SubElement(
    help_viewer.xpath('//p[@class="preview-note"]')[1],
    "a",
    href="konfiguration-hilfe-originale.html",
)
original_link.text = " · Unveränderte Stitch-Originale ansehen"
for link in help_viewer.xpath('//a[@href="admin-entwurf.html"]'):
    link.set("href", "konfiguration-hilfe.html")
    link.text = "Direktansicht der Feldhilfe"
normal_link = etree.SubElement(
    help_viewer.xpath('//p[@class="preview-note"]')[1],
    "a",
    href="admin-entwuerfe.html",
)
normal_link.text = " · Normale Konfiguration"
for option in help_viewer.xpath('//*[@id="ad-page"]//option'):
    if not option.get("value", "").startswith("settings:"):
        option.getparent().remove(option)
help_viewer.get_element_by_id("ad-variant").getparent().set("style", "display:none")
help_controls = etree.SubElement(
    help_viewer.xpath('//div[@class="preview-toolbar"]')[0],
    "div",
    attrib={"class": "preview-appearance-controls"},
)
help_label = etree.SubElement(help_controls, "label")
help_label.text = "Feldhilfe "
help_mode = etree.SubElement(help_label, "select", id="field-help-mode")
for value, label in (
    ("a", "A · Infoicon rechts / Popover"),
    ("b", "B · Inline-Hilfe am Label"),
    ("c", "C · Gemeinsames Kontextpanel"),
):
    etree.SubElement(help_mode, "option", value=value).text = label
etree.SubElement(
    help_viewer.xpath('//div[@class="preview-toolbar"]')[0],
    "p",
    id="field-help-description",
    attrib={"class": "preview-note"},
)
help_frame = help_viewer.get_element_by_id("account-preview")
help_revision = hashlib.sha256(
    (reference / "konfiguration-hilfe.html").read_bytes()
).hexdigest()[:12]
help_frame.set("src", "konfiguration-hilfe.html?rev=" + help_revision)
help_frame.set("title", "Konfiguration · Feldhilfe-Vorschläge A/B/C")
etree.SubElement(help_viewer.find("body"), "script", nonce="cosmos-viewer").text = (
    root / "hilfe-viewer.js"
).read_text(encoding="utf-8")
write("konfiguration-hilfe-vorschlaege.html", help_viewer)
print("Getrennten Feldhilfe-Vergleich A/B/C erzeugt.")

original_source = (root / "hilfe-originale.html").read_text(encoding="utf-8")
for candidate in ("a", "b", "c"):
    image = (root / "stitch" / f"field-help-{candidate}.png").read_bytes()
    original_source = original_source.replace(
        f"@IMAGE_{candidate.upper()}@",
        "data:image/png;base64," + base64.b64encode(image).decode("ascii"),
    )
write("konfiguration-hilfe-originale.html", html.fromstring(original_source))
print("Unveränderte Stitch-Entwurfsboards A/B/C eingebettet.")

# Erste Runde von C als Seitenleiste, getrennt von Originalen und Nachbauten.
sidebar_document = deepcopy(document)
sidebar_document.find("body").set("data-config-help-preview", "")
sidebar_document.find("head/title").text = "Cosmos · Konfiguration · Hilfeseitenleiste"
etree.SubElement(
    sidebar_document.find("head"), "style", attrib={"data-admin-extension": ""}
).text = (root / "hilfe-seitenleiste.css").read_text(encoding="utf-8")
etree.SubElement(
    sidebar_document.find("body"),
    "script",
    nonce="cosmos-admin",
    attrib={"data-admin-extension": ""},
).text = (root / "hilfe-seitenleiste.js").read_text(encoding="utf-8")
write("konfiguration-seitenleiste-inhalt.html", sidebar_document)

sidebar_viewer = deepcopy(viewer)
sidebar_viewer.find("head/title").text = "Cosmos · Feldhilfe · Seitenleiste C"
sidebar_viewer.xpath('//h1[@class="preview-title"]')[
    0
].text = "Konfiguration · Hilfeseitenleiste · Komponentenansicht"
sidebar_viewer.xpath('//p[@class="preview-note"]')[
    0
].text = "C als rechtes Overlay im Filterstil. Infoicon öffnet die Hilfe, ohne Formularfelder zu verschieben. X, Schließen, Escape oder Außenklick schließen die Leiste. Nur synthetische Beispiele, noch keine Übernahme."
sidebar_links = sidebar_viewer.xpath('//p[@class="preview-note"]')[1]
sidebar_links.clear()
sidebar_links.set("class", "preview-note")
for label, href in (
    ("Stitch-Originale", "konfiguration-hilfe-originale.html"),
    ("Aktuelle Konfiguration", "admin-entwuerfe.html"),
    ("Direktansicht der Seitenleiste", "konfiguration-seitenleiste-inhalt.html"),
):
    link = etree.SubElement(sidebar_links, "a", href=href)
    link.text = label
    link.tail = " · "
for option in sidebar_viewer.xpath('//*[@id="ad-page"]//option'):
    if not option.get("value", "").startswith("settings:"):
        option.getparent().remove(option)
sidebar_viewer.get_element_by_id("ad-variant").getparent().set("style", "display:none")
sidebar_frame = sidebar_viewer.get_element_by_id("account-preview")
sidebar_revision = hashlib.sha256(
    (reference / "konfiguration-seitenleiste-inhalt.html").read_bytes()
).hexdigest()[:12]
sidebar_frame.set(
    "src", "konfiguration-seitenleiste-inhalt.html?rev=" + sidebar_revision
)
sidebar_frame.set("title", "Konfiguration · Erste Hilfeseitenleiste C")
write("konfiguration-seitenleiste.html", sidebar_viewer)
print("Separate erste Runde C mit Hilfeseitenleiste erzeugt.")

# Auch die Entwurfsnavigation zeigt die bereits freigegebene Konfiguration.
current_draft = deepcopy(sidebar_document)
current_draft.find("head/title").text = "Cosmos · Administration · Entwürfe A/B"
current_draft.find("body").attrib.pop("data-config-help-preview", None)
write("admin-entwurf.html", current_draft)
revision = hashlib.sha256((reference / "admin-entwurf.html").read_bytes()).hexdigest()[
    :12
]
viewer.get_element_by_id("account-preview").set(
    "src", "admin-entwurf.html?rev=" + revision
)
write("admin-entwuerfe.html", viewer)

# Alle abgestimmten Adminbereiche teilen dieselbe feste Referenz.
# Reine Entwurfsrunden lassen die abgenommenen HTML-Dateien bytegleich stehen.
if "--draft-only" not in sys.argv:
    approved_document = deepcopy(sidebar_document)
    approved_document.find("head/title").text = "Cosmos · Administration · Referenz"
    approved_document.find("body").attrib.pop("data-admin-draft", None)
    approved_document.find("body").attrib.pop("data-config-help-preview", None)
    approved_document.find("body").set("data-admin-config-approved", "")
    approved_document.find("body").set("data-admin-dashboard-approved", "")
    approved_document.find("body").set("data-admin-notifications-approved", "")
    approved_document.get_element_by_id("admin-proposal").set(
        "aria-label", "Administration"
    )
    write("admin-inhalt.html", approved_document)

    approved_viewer = deepcopy(viewer)
    approved_viewer.find("head/title").text = "Cosmos · Administration · Referenz"
    approved_viewer.xpath('//h1[@class="preview-title"]')[
        0
    ].text = "Cosmos · Administration"
    approved_notes = approved_viewer.xpath('//p[@class="preview-note"]')
    approved_notes[
        0
    ].text = "Abgestimmte Referenz: Dashboard, Benutzerverwaltung, Konfiguration mit acht Bereichen, Mailtest und interne Benachrichtigungen. Navigation und Schnellzugriffe öffnen alle Bereiche innerhalb dieser Referenz. Alle Aktionen sind lokal simuliert."
    for link in approved_notes[1].xpath("./a"):
        if link.get("href") == "admin-entwurf.html":
            link.set("href", "admin-inhalt.html")
            link.text = "Direktansicht der Adminreferenz"
        elif link.get("href") == "admin.html":
            link.set("href", "admin-entwuerfe.html")
            link.text = "Zusätzliche Adminsichtung"
    approved_viewer.get_element_by_id("ad-variant").getparent().set(
        "style", "display:none"
    )
    approved_frame = approved_viewer.get_element_by_id("account-preview")
    approved_revision = hashlib.sha256(
        (reference / "admin-inhalt.html").read_bytes()
    ).hexdigest()[:12]
    approved_frame.set("src", "admin-inhalt.html?rev=" + approved_revision)
    approved_frame.set(
        "title",
        "Administration · Dashboard, Benutzerverwaltung, Konfiguration, Mailtest und Benachrichtigungen",
    )
    write("admin.html", approved_viewer)
    print(
        "Alle freigegebenen Adminbereiche einschließlich Benachrichtigungen in Referenz und Viewer übernommen."
    )
