"""Lokalen Iconvergleich aus inventarisierten Quellen und geprüften SVGs bauen."""

import html
import json
import re
import xml.etree.ElementTree as ET
from copy import deepcopy
from pathlib import Path

FOLDER = Path(__file__).resolve().parent
ROOT = FOLDER.parents[4]
SVG_NAMESPACE = "http://www.w3.org/2000/svg"
ET.register_namespace("", SVG_NAMESPACE)


def svg_markup(source, instance):
    """Nur passive SVG-Geometrie übernehmen; Masken-IDs je Instanz trennen."""
    svg = ET.fromstring(source)
    allowed = {
        "svg",
        "g",
        "path",
        "circle",
        "rect",
        "line",
        "polyline",
        "polygon",
        "ellipse",
        "defs",
        "mask",
    }
    for element in svg.iter():
        if element.tag.split("}")[-1] not in allowed:
            raise ValueError(f"Unerwartetes SVG-Element: {element.tag}")
        for attribute, value in element.attrib.items():
            if attribute.lower().startswith("on") or "href" in attribute:
                raise ValueError(f"Aktives SVG-Attribut: {attribute}")
            if "url(" in value and not re.fullmatch(r"url\(#[\w-]+\)", value):
                raise ValueError("SVG darf keine externen Ressourcen laden")
    svg = deepcopy(svg)
    identifiers = {
        element.get("id"): f"{instance}-{element.get('id')}"
        for element in svg.iter()
        if element.get("id")
    }
    for element in svg.iter():
        if element.get("id") in identifiers:
            element.set("id", identifiers[element.get("id")])
        for attribute, value in list(element.attrib.items()):
            for old, new in identifiers.items():
                value = value.replace(f"url(#{old})", f"url(#{new})")
            element.set(attribute, value)
    svg.set("aria-hidden", "true")
    return ET.tostring(svg, encoding="unicode")


def sample(source, label, instance, external=False):
    plain = svg_markup(source, f"{instance}-plain")
    button = svg_markup(source, f"{instance}-button")
    css = "sample external" if external else "sample"
    return f'<div class="{css}"><span class="standalone">{plain}</span><span class="example-button">{button}<span>{html.escape(label)}</span></span></div>'


def render():
    catalog = json.loads((FOLDER / "catalog.json").read_text(encoding="utf-8"))
    old = (FOLDER / "vaadin.html").read_text(encoding="utf-8")
    # Den lokalen Styleblock einschließlich seiner Zeilenumbrüche übernehmen.
    style = re.search(r"<style>(.*?)</style>", old, re.DOTALL).group(1)
    native_source = (
        ROOT / "app/node_modules/@vaadin/icons/vaadin-iconset.js"
    ).read_text(encoding="utf-8")
    # Je eine vollständige, einzeilige Icongruppe aus dem installierten Iconset lesen.
    native = dict(re.findall(r'<g id="vaadin:([^"]+)">(.*?)</g>\s*\n', native_source))
    metadata = {
        name: json.loads(
            (FOLDER / "assets" / name / "source.json").read_text(encoding="utf-8")
        )
        for name in ("tabler", "lucide")
    }
    groups = {
        "reference": "Referenz",
        "application": "Weitere Anwendung",
        "status": "Anwesenheitsstatus",
        "component": "Komponentenintern",
    }
    cards = []
    for item in catalog:
        key, label = item["key"], item["label"]
        current = item["current"]
        columns = []
        if current:
            if "file" in current:
                source = (ROOT / current["file"]).read_text(encoding="utf-8")
                description = Path(current["file"]).name
            elif "native" in current:
                symbol = current["native"].lower().replace("_", "-")
                source = f'<svg xmlns="{SVG_NAMESPACE}" viewBox="0 0 16 16" fill="currentColor">{native[symbol]}</svg>'
                description = f"VaadinIcon.{current['native']}"
            else:
                source, description = current["svg"], "Aktuelle Statuskontur"
            columns.append(
                f'<div class="comparison"><h3>Aktuell · {groups[item["group"]]}</h3>{sample(source, label, key)}<code>{html.escape(description)}</code></div>'
            )
        else:
            columns.append(
                '<div class="comparison"><h3>Aktuell · Vaadin-Komponente</h3><p class="component-note">Kein nachgebautes Ist-Symbol.<br>Native Komponenten gesondert vergleichen.</p></div>'
            )
        for family, title in (("tabler", "Tabler Outline"), ("lucide", "Lucide")):
            for name in item[family]:
                source = (FOLDER / "assets" / family / f"{name}.svg").read_text(
                    encoding="utf-8"
                )
                columns.append(
                    f'<div class="comparison"><h3>{title}</h3>{sample(source, label, key + "-" + family, external=True)}<code>{name}</code></div>'
                )
        sources = "".join(
            f"<li><code>{html.escape(path)}</code></li>" for path in item["uses"]
        )
        details = (
            f'<details class="sources"><summary>Fundstellen ({len(item["uses"])})</summary><ul>{sources}</ul></details>'
            if sources
            else ""
        )
        search = " ".join(
            [key, label, *item["tabler"], *item["lucide"], *item["uses"]]
        ).lower()
        badge = "Andere Bildaussage" if item.get("exception") else groups[item["group"]]
        cards.append(
            f'<article data-group="{item["group"]}" data-search="{html.escape(search)}"><header><h2>{html.escape(label)}</h2><span class="badge">{badge}</span></header><div class="variants">{"".join(columns)}</div><p class="note">{html.escape(item["note"])}</p>{details}</article>'
        )
    licenses = "".join(
        f"<details><summary>{title} · Lizenz und Paketquelle</summary><p>{html.escape(metadata[family]['package'])} {html.escape(metadata[family]['version'])}</p><pre>{html.escape((FOLDER / 'assets' / family / 'LICENSE').read_text(encoding='utf-8'))}</pre></details>"
        for family, title in (("tabler", "Tabler"), ("lucide", "Lucide"))
    )
    style += """nav{display:flex;gap:16px;margin-bottom:18px}.variants{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:20px}.comparison{min-width:0}.component-note{font-size:12px;color:#94a3b8}.external svg{stroke-width:var(--external-stroke,2)}.sources{padding:10px 20px;border-top:1px solid #ffffff14;color:#94a3b8;font-size:11px}.sources summary,footer summary{cursor:pointer;color:#38bdf8}.sources ul{padding-left:20px}.sources code{margin-top:3px}footer details{margin:12px 0}pre{white-space:pre-wrap;overflow-wrap:anywhere}.toolbar{align-items:end}.toolbar label:first-child{flex:1 1 220px}.toolbar input{width:100%}.summary{margin-left:0}.empty{color:#94a3b8;padding:20px}@media(max-width:800px){.variants{grid-template-columns:1fr}.toolbar label{flex:1 1 140px}.toolbar label:first-child{flex-basis:100%}.example-button{max-width:100%}}"""
    script = """const search=document.querySelector('#search'),group=document.querySelector('#group'),size=document.querySelector('#size'),stroke=document.querySelector('#stroke'),summary=document.querySelector('#summary');function update(){const query=search.value.trim().toLocaleLowerCase('de');let visible=0;for(const card of document.querySelectorAll('article')){card.hidden=!(card.dataset.search.includes(query)&&(!group.value||card.dataset.group===group.value));if(!card.hidden)visible++;}summary.textContent=visible+' von 58 Vergleichspunkten';document.querySelector('#empty').hidden=visible!==0;document.documentElement.style.setProperty('--icon-size',size.value+'px');document.documentElement.style.setProperty('--external-stroke',stroke.value);}search.addEventListener('input',update);for(const control of [group,size,stroke])control.addEventListener('change',update);update();"""
    page = f"""<!doctype html><html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Referenz · Tabler · Lucide</title><style>{style}</style></head><body><main><nav><a href="index.html" aria-current="page">Externe Sets</a><a href="vaadin.html">Bisheriger Vaadin-Vergleich</a></nav><h1>Referenz · Tabler · Lucide</h1><p class="intro">Einheitliche externe Iconsets im direkten Vergleich. Gleiche Farbe und äußere Größe; links der aktuelle Stand, daneben die unveränderten Konturen beider Bibliotheken. Die Strichstärke lässt sich nur für die beiden externen Sets gemeinsam ändern. Beispielbuttons zeigen die Wirkung im Kontext. Die Anwendung und die festen Referenzen bleiben unverändert.</p><p class="intro">Inventar: 24 Referenzicons, alle 44 ausdrücklich verwendeten Vaadin-Konstanten zugeordnet, zwei zusätzliche SVG-Bausteine und vier Statuskonturen. Komponenteninterne Symbole sind als zwei zusätzliche Funktionsbeispiele gekennzeichnet; ihre tatsächliche Vaadin-Integration ist noch nicht geprüft. Fundstellen stehen unter jeder Zeile. Logo und Kosmosillustration sind keine UI-Icons.</p><div class="toolbar"><label>Icon suchen<input id="search" type="search" placeholder="Name, Funktion oder Datei suchen"></label><label>Bereich<select id="group"><option value="reference">Referenzicons</option><option value="application">Weitere Anwendung</option><option value="status">Anwesenheitsstatus</option><option value="component">Komponentenintern</option><option value="">Alle Bereiche</option></select></label><label>Icongröße<select id="size"><option value="16">16 px · Button</option><option value="18">18 px · Schließen</option><option value="20">20 px · Panel</option><option value="24">24 px · groß</option></select></label><label>Externe Strichstärke<select id="stroke"><option value="2">Original · 2 px</option><option value="1.75">1,75 px</option><option value="1.5">1,5 px</option></select></label><output id="summary" class="summary" aria-live="polite"></output></div>{"".join(cards)}<p id="empty" class="empty" hidden>Keine passenden Icons. Suche oder Bereich ändern.</p><footer>Lokale SVG-Konturen aus Tabler Outline und Lucide, keine externen Skripte oder CDN-Aufrufe. SVGs und Paketlizenzen sind unter assets gesichert. Diese Seite ist selbstständig auch ohne Server verwendbar. {licenses}</footer></main><script>{script}</script></body></html>"""
    (FOLDER / "index.html").write_text(page, encoding="utf-8")
    print(
        f"{len(catalog)} Vergleichspunkte mit Originalkonturen aus beiden Iconsets erzeugt."
    )


if __name__ == "__main__":
    render()
