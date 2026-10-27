"""Freigegebene Benutzerverwaltungsreferenz aus den gesicherten Quellen erzeugen."""

import hashlib
import json
import re
import runpy
from copy import deepcopy
from pathlib import Path

from lxml import etree, html

root = Path(__file__).parent
reference = root.parents[1]
repo = reference.parents[2]
preview = repo / "target/designkritik-stitch/portal/preview"
source = (root / "ausgangsbasis.html").read_text(encoding="utf-8")
fields_helper = runpy.run_path(str(root.parent / "eingabefelder.py"))
logo_helper = runpy.run_path(str(root.parent / "service_logo.py"))
texts = fields_helper["application_texts"]()
# Die bestehende Portalreferenz gemeinsam nutzen, keine zweite Portalkopie pflegen.
source = source.replace("admin-portal.html", "portal.html")
portal_revision = hashlib.sha256((reference / "portal.html").read_bytes()).hexdigest()[
    :12
]
source = re.sub(
    r'window\.AdminPortalRevision="[a-f0-9]+"',
    f'window.AdminPortalRevision="{portal_revision}"',
    source,
)
source = re.sub(
    r"portal\.html\?rev=[a-f0-9]+", f"portal.html?rev={portal_revision}", source
)


def script(document, text):
    etree.SubElement(document.find("body"), "script", nonce="cosmos-admin").text = text


def write(name, document):
    result = html.tostring(document, encoding="unicode", doctype="<!doctype html>")
    result = "\n".join(line.rstrip() for line in result.splitlines()) + "\n"
    (reference / name).write_text(result, encoding="utf-8")
    if preview.exists():
        (preview / name).write_text(result, encoding="utf-8")


def prepare_reference(document):
    document.find("body").set("data-admin-reference", "users")
    controllers = [
        node
        for node in document.xpath("//script")
        if "const visibleUsers = () =>" in (node.text or "")
    ]
    assert len(controllers) == 1
    controller = controllers[0]
    # Suche und Mehrfachkriterien gemeinsam erst mit Anwenden übernehmen.
    replacements = {
        '<small class="au-help"> · Sie</small>': ' <span class="au-self-badge">Dein Benutzer</span>',
        "const query = byId('userSearch').value.trim().toLocaleLowerCase('de');": "const query = (filters.query || '').trim().toLocaleLowerCase('de');",
        "user.status === filters.status": "filters.status.split(',').includes(user.status)",
        "user.roles.includes(filters.role)": "filters.role.split(',').some(role => user.roles.includes(role))",
        "const active = [['enabled','Aktiv',filters.enabled]": "const active = [['query','Suche',filters.query],['enabled','Aktiv',filters.enabled]",
        "byId('userSearch').addEventListener('input',() => { page = 0; render(); });": "",
        "closeTime(); byId('userSearch').value = ''; filters = {}; page = 0; render();": "closeTime(); byId('userSearch').value = '';",
        "filters = {enabled:byId('filterEnabled').dataset.value": "filters = {query:byId('userSearch').value.trim(),enabled:byId('filterEnabled').dataset.value",
        "if (key === 'cutoff') {": "if (key === 'query') { byId('userSearch').value = ''; }\n    else if (key === 'cutoff') {",
        "byId('editorPassword').placeholder = user ? 'Unverändert lassen' : '';": "byId('editorPassword').placeholder = user ? "
        + json.dumps(texts["admin.users.password.placeholder.edit"], ensure_ascii=False)
        + " : "
        + json.dumps(texts["account.form.password.placeholder"], ensure_ascii=False)
        + ";",
        "value || (list.trigger.getAttribute('aria-required') === 'true' ? 'Bitte auswählen' : 'Alle')": "value || (list.trigger.getAttribute('aria-required') === 'true' ? (list.trigger.dataset.refPlaceholder || 'Bitte auswählen') : 'Alle')",
    }
    for before, after in replacements.items():
        assert controller.text.count(before) == 1, before
        controller.text = controller.text.replace(before, after)
    return controller


drawer = html.fromstring(source)
controller = prepare_reference(drawer)
logo_helper["replace_header_logo"](drawer)
for button in drawer.xpath("//table//th/button[@data-sort]"):
    previous_icon = button.find("svg")
    button.remove(previous_icon)
    indicator = etree.SubElement(
        button, "span", attrib={"class": "au-sort-indicator", "aria-hidden": "true"}
    )
    icon = etree.SubElement(indicator, "svg", viewBox="0 0 12 12", fill="currentColor")
    for name, path in (
        ("idle", "M6 1l3 4H3l3-4zm0 10l3-4H3l3 4z"),
        ("up", "M6 2l4 5H2l4-5z"),
        ("down", "M6 10l4-5H2l4 5z"),
    ):
        etree.SubElement(icon, "path", d=path, attrib={"class": f"au-sort-{name}"})
password_hint = drawer.get_element_by_id("passwordHint")
drawer.get_element_by_id("editorPassword").getparent().getparent().append(password_hint)
confirm_hint = etree.SubElement(
    drawer.get_element_by_id("editorConfirm").getparent().getparent(),
    "p",
    id="confirmPasswordHint",
    attrib={"class": "au-help au-password-help"},
)
password_hint.set("class", "au-help au-password-help")
drawer.get_element_by_id("editorPassword").set("aria-describedby", "passwordHint")
drawer.get_element_by_id("editorConfirm").set("aria-describedby", "confirmPasswordHint")
hint_start = controller.text.index("    const passwordHint = '")
hint_end = controller.text.index("    byId('editorEnabled')", hint_start)
controller.text = (
    controller.text[:hint_start]
    + "    updatePasswordHints();\n"
    + controller.text[hint_end:]
)
controller.text = controller.text.replace(
    "  const closeEditor = () =>",
    """  const updatePasswordHints = () => {
    const present = byId('editorPassword').value.length > 0;
    byId('passwordHint').textContent = present || editedId === null ? 'Mindestens 8 Zeichen, ein Großbuchstabe und ein Sonderzeichen.' : 'Leer lassen, um das bisherige Passwort beizubehalten.';
    byId('confirmPasswordHint').textContent = present ? 'Passwort zur Bestätigung erneut eingeben.' : editedId === null ? 'Wird aktiv, sobald ein Passwort eingegeben ist.' : 'Wird aktiv, sobald ein neues Passwort eingegeben ist.';
  };
  const closeEditor = () =>""",
)
hint_input_end = "if (!present) { byId('editorConfirm').value = ''; } });"
assert controller.text.count(hint_input_end) == 1
controller.text = controller.text.replace(
    hint_input_end,
    "if (!present) { byId('editorConfirm').value = ''; } updatePasswordHints(); });",
)
roles = drawer.get_element_by_id("userEditor").xpath('.//fieldset[@class="au-roles"]')[
    0
]
roles_section = etree.Element("div", attrib={"class": "au-editor-roles-section"})
roles.getparent().replace(roles, roles_section)
roles_section.append(roles)
role_hint = roles_section.getnext()
assert role_hint.tag == "p" and "Mindestens eine Rolle" in role_hint.text
role_hint.set("class", "au-help au-role-help")
role_hint.set("id", "editorRoleHint")
roles.set("aria-describedby", "editorRoleHint")
roles_section.append(role_hint)
drawer.xpath('//*[contains(concat(" ",@class," ")," au-account-total ")]//dt')[
    0
].text = "Konten"
old_toggle = "byId('filterToggle').addEventListener('click',() => { const panel = byId('filterPanel'); panel.hidden = !panel.hidden; byId('filterToggle').setAttribute('aria-expanded',String(!panel.hidden)); });"
assert controller.text.count(old_toggle) == 1
controller.text = controller.text.replace(
    old_toggle,
    "byId('filterToggle').addEventListener('click',() => document.dispatchEvent(new Event('admin-filter-toggle')));",
)
apply_end = (
    "dateLabel:dateText + (timeText ? ' ' + timeText : '')}; page = 0; render();"
)
assert controller.text.count(apply_end) == 1
controller.text = controller.text.replace(
    apply_end,
    apply_end + " document.dispatchEvent(new Event('admin-filters-applied'));",
)
# Native Auswahlgruppen ersetzen die drei Filter-Comboboxen vollständig.
combo_adapter = """const field = byId(id);
    if (field.classList.contains('au-filter-choice')) {
      field.dataset.value = value;
      field.querySelectorAll('input').forEach(input => { input.checked = input.type === 'radio' ? input.value === value : value.split(',').includes(input.value); });
      return;
    }
    """
controller.text = controller.text.replace(
    "const setCombo = (id, value) => {",
    "const setCombo = (id, value) => {\n    " + combo_adapter,
)
# Alle vier Werte einer Gruppe schränken das Ergebnis nicht ein.
for identifier in ("filterStatus", "filterRole"):
    expression = f"byId('{identifier}').dataset.value"
    controller.text = controller.text.replace(
        expression,
        f"({expression}.split(',').filter(Boolean).length === 4 ? '' : {expression})",
    )
chip_render_start = controller.text.index("    byId('activeFilters').hidden")
chip_render_end = controller.text.index("\n  };", chip_render_start)
controller.text = (
    controller.text[:chip_render_start]
    + "    document.dispatchEvent(new CustomEvent('admin-filter-rendered',{detail:active.length}));"
    + controller.text[chip_render_end:]
)
chip_handler_start = controller.text.index("  byId('activeFilters').addEventListener")
chip_handler_end = controller.text.index("  byId('refreshUsers')", chip_handler_start)
controller.text = (
    controller.text[:chip_handler_start] + controller.text[chip_handler_end:]
)
chips = drawer.get_element_by_id("activeFilters")
chips.getparent().remove(chips)

panel = drawer.get_element_by_id("filterPanel")
fields = panel.xpath('./div[@class="au-filter-fields"]')[0]
time_label = fields.xpath('.//label[@for="filterTime"]')[0]
time_field = time_label.getparent()
time_field.remove(time_label)
time_field.set("class", "au-field au-time-field")
drawer.get_element_by_id("filterTime").set("aria-label", "Uhrzeit (optional)")
symbols = {
    node.get("data-symbol"): node
    for node in drawer.xpath('//template[@id="vp-symbols"]//*[@data-symbol]')
}
choice_groups = (
    (
        "filterEnabled",
        "Aktiv",
        "radio",
        (("", "Alle", None), ("Ja", "Ja", None), ("Nein", "Nein", None)),
    ),
    (
        "filterStatus",
        "Status",
        "checkbox",
        (
            ("Verfügbar", "Verfügbar", "success"),
            ("Beschäftigt", "Beschäftigt", "minus"),
            ("Abwesend", "Abwesend", "clock"),
            ("Offline", "Offline", "close"),
        ),
    ),
    (
        "filterRole",
        "Rollen",
        "checkbox",
        tuple(
            (value, value, None)
            for value in ("Superadmin", "Administrator", "Benutzer", "Gast")
        ),
    ),
)
for identifier, title, kind, choices in choice_groups:
    old = drawer.get_element_by_id(identifier).getparent()
    group = etree.Element(
        "fieldset",
        id=identifier,
        attrib={"class": "au-filter-choice", "data-value": ""},
    )
    etree.SubElement(group, "legend").text = title
    grid = etree.SubElement(
        group,
        "div",
        attrib={"class": "au-choice-segment" if kind == "radio" else "au-choice-grid"},
    )
    for index, (value, label, icon) in enumerate(choices):
        item = etree.SubElement(grid, "label", attrib={"class": "au-choice-option"})
        checkbox = etree.SubElement(
            item, "input", type=kind, name=identifier, value=value
        )
        if kind == "radio" and value == "":
            checkbox.set("checked", "")
        tone = (
            ("green", "red", "amber", "offline")[index]
            if identifier == "filterStatus"
            else "role"
            if identifier == "filterRole"
            else ""
        )
        card = etree.SubElement(
            item, "span", attrib={"class": "au-choice-card " + tone}
        )
        text = etree.SubElement(card, "span", attrib={"class": "au-choice-label"})
        if icon:
            text.append(deepcopy(symbols[icon][0]))
        etree.SubElement(text, "span").text = label
        check = deepcopy(symbols["check"][0])
        check.set("class", "au-choice-check")
        card.append(check)
    old.getparent().replace(old, group)
    menu = drawer.get_element_by_id(identifier + "Menu")
    menu.getparent().remove(menu)
reset = drawer.get_element_by_id("clearFilters")
count = drawer.get_element_by_id("filterCount")
apply_button = drawer.get_element_by_id("applyFilters")
info = panel.xpath('.//p[@class="au-info"]')[0]
info.set("class", "portal-security-hint au-drawer-info")
panel.tag = "dialog"
panel.set("class", "au-filter-drawer")
panel.attrib.pop("hidden", None)
panel.set("aria-describedby", "drawerDescription")
for child in list(panel):
    panel.remove(child)
head = etree.SubElement(panel, "div", {"class": "portal-card-head au-drawer-head"})
category = etree.SubElement(
    head, "span", {"class": "portal-reference-icon", "aria-hidden": "true"}
)
category.append(deepcopy(drawer.get_element_by_id("filterToggle").xpath("./svg")[0]))
heading = etree.SubElement(head, "div", {"class": "portal-reference-heading"})
etree.SubElement(heading, "h2", id="filterTitle").text = "Filter"
etree.SubElement(
    heading, "p", id="drawerDescription"
).text = "Benutzerkonten gezielt eingrenzen."
close = deepcopy(
    drawer.get_element_by_id("userEditor").xpath(".//button[@data-close-editor]")[0]
)
close.attrib.pop("data-close-editor", None)
close.set("class", "n-close au-drawer-close")
close.set("data-close-filters", "")
close.set("aria-label", "Filter schließen")
head.append(close)
body = etree.SubElement(panel, "div", {"class": "au-drawer-body"})
tools = etree.SubElement(body, "div", {"class": "au-drawer-tools"})
tools.append(count)
tools.append(reset)
search_field = etree.SubElement(body, "div", {"class": "au-field au-drawer-search"})
etree.SubElement(search_field, "label", {"for": "userSearch"}).text = "Suche"
search_field.append(drawer.get_element_by_id("userSearch").getparent())
body.append(fields)
body.append(info)
foot = etree.SubElement(panel, "div", {"class": "portal-card-foot au-drawer-footer"})
cancel = deepcopy(
    drawer.get_element_by_id("userEditor").xpath(".//button[@data-close-editor]")[1]
)
cancel.attrib.pop("data-close-editor", None)
cancel.set("data-close-filters", "")
foot.append(cancel)
foot.append(apply_button)
for identifier in (
    "filterTimeMenu",
    "activityCalendarDropdown",
):
    panel.append(drawer.get_element_by_id(identifier))
drawer.xpath("//body/script")[0].addprevious(panel)
trigger = drawer.get_element_by_id("filterToggle")
trigger.set("aria-haspopup", "dialog")
counter = etree.SubElement(
    trigger,
    "span",
    id="drawerAppliedCount",
    hidden="",
    attrib={"class": "au-drawer-trigger-count", "aria-hidden": "true"},
)
counter.text = "0"
etree.SubElement(drawer.find("head"), "style", id="admin-drawer-style").text = (
    root / "drawer.css"
).read_text(encoding="utf-8")
script(drawer, (root / "drawer.js").read_text(encoding="utf-8"))
etree.SubElement(drawer.find("head"), "style", id="admin-table-header-style").text = (
    root / "table-header.css"
).read_text(encoding="utf-8")
drawer.find("head/title").text = "Cosmos · Benutzerverwaltung · Filter-Drawer"
fields_helper["harmonize_document"](drawer, "admin")
write("admin.html", drawer)
revision = hashlib.sha256((reference / "admin.html").read_bytes()).hexdigest()[:12]

viewer = html.fromstring((root / "viewer.html").read_text(encoding="utf-8"))
viewer.get_element_by_id("account-preview").set("src", "admin.html?rev=" + revision)
write("admin-users.html", viewer)
print("Freigegebene Benutzerverwaltung mit Vorschau aktualisiert.")
