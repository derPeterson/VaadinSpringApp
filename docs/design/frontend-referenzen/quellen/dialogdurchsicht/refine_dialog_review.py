"""Freigegebene Dialogdurchsicht gezielt auf die bestehende Referenz anwenden."""

import re
from copy import deepcopy
from pathlib import Path

from lxml import etree, html

root = Path(__file__).parent


def refine_session(dialog):
    classes = dialog.get("class", "").split()
    if "portal-confirm-dialog" not in classes:
        dialog.set("class", " ".join([*classes, "portal-confirm-dialog"]))
    body = dialog.xpath('./div[contains(@class,"n-dialog-body")]')[0]
    if not body.xpath('./div[@class="portal-confirm-summary"]'):
        summary = etree.Element("div", {"class": "portal-confirm-summary"})
        body.insert(0, summary)
        description = dialog.get_element_by_id("session-confirm-description")
        description.set("class", "portal-confirm-description")
        summary.append(description)
        hint = dialog.get_element_by_id("session-confirm-hint")
        old_info = hint.getparent()
        hint.tag = "p"
        hint.set("class", "portal-confirm-explanation")
        summary.append(hint)
        old_info.getparent().remove(old_info)
    dialog.get_element_by_id("session-confirm-title").text = "Gerät abmelden"
    dialog.get_element_by_id("session-confirm-label").text = "Abmelden"
    dialog.get_element_by_id("session-confirm-context").set(
        "class", "portal-confirm-context-list"
    )


def add_profile_gender(document):
    if document.xpath('//*[@id="profileGenderTrigger"]'):
        mark_profile_gender_required(document)
        return
    field = deepcopy(document.get_element_by_id("languageSelectContainer"))
    mapping = {
        "languageSelectContainer": "profileGenderContainer",
        "languageLabel": "profileGenderLabel",
        "spracheTrigger": "profileGenderTrigger",
        "languageDropdown": "profileGenderDropdown",
        "selectedLanguageText": "selectedProfileGenderText",
    }
    for node in field.iter():
        for attribute in ("id", "for", "aria-controls", "aria-labelledby"):
            if node.get(attribute) in mapping:
                node.set(attribute, mapping[node.get(attribute)])
    field.xpath("./label")[0].text = "Geschlecht"
    field.get_element_by_id("selectedProfileGenderText").text = "Männlich"
    field.remove(field.xpath("./span")[0])
    etree.SubElement(
        field,
        "input",
        type="hidden",
        id="profileGender",
        name="geschlecht",
        value="Männlich",
    )
    birth = document.get_element_by_id("geburtsdatum").getparent().getparent()
    birth.addprevious(field)
    menu = deepcopy(document.get_element_by_id("languageDropdown"))
    menu.set("id", "profileGenderDropdown")
    menu.set("aria-labelledby", "profileGenderLabel")
    wrapper = menu[0]
    option = deepcopy(wrapper[0])
    wrapper.clear()
    wrapper.set("class", "p-1.5 space-y-1")
    for index, value in enumerate(("Männlich", "Weiblich", "Divers")):
        row = deepcopy(option)
        row.set("id", f"profile-gender-option-{index}")
        row.attrib.pop("data-language", None)
        row.set("data-gender", value)
        row.set("aria-selected", str(index == 0).lower())
        row.xpath("./div/span[last()]")[0].text = value
        wrapper.append(row)
    scripts = document.xpath("//body/script")
    if scripts:
        scripts[0].addprevious(menu)
    else:
        document.find("body").append(menu)
    mark_profile_gender_required(document)


def mark_profile_gender_required(document):
    label = document.get_element_by_id("profileGenderLabel")
    if not label.xpath('./span[@aria-hidden="true"]'):
        label.text = "Geschlecht "
        etree.SubElement(
            label,
            "span",
            attrib={
                "class": "text-sky-400 text-sm leading-none",
                "aria-hidden": "true",
            },
        ).text = "•"
    document.get_element_by_id("profileGenderTrigger").set("aria-required", "true")


def review_script(source):
    sessions = (root / "sessions.js").read_text(encoding="utf-8").strip()
    # Nur den abgeschlossenen Sitzungscontroller ersetzen; andere IIFEs erhalten.
    source, count = re.subn(
        r"\(\(\) => \{\n  const panel = document.querySelector\('\[data-portal-page=\"sessions\"\]'\);[\s\S]*?\n  render\(\);\n\}\)\(\);",
        lambda _: sessions,
        source,
        count=1,
    )
    assert count == 1, "Sitzungscontroller fehlt"
    gender = (root / "profile-gender.js").read_text(encoding="utf-8")
    if "// Beginn Profil-Geschlechtsauswahl." in source:
        source = re.sub(
            r"// Beginn Profil-Geschlechtsauswahl\.[\s\S]*?// Ende Profil-Geschlechtsauswahl\.",
            lambda _: gender.strip(),
            source,
            count=1,
        )
    else:
        source += "\n" + gender
    return source


def apply_dialog_review(document):
    refine_session(document.get_element_by_id("session-confirm"))
    add_profile_gender(document)
    mark_presence_heading(document)
    for script in document.xpath("//script[not(@src)]"):
        if (
            "const panel = document.querySelector('[data-portal-page=\"sessions\"]');"
            in (script.text or "")
        ):
            script.text = review_script(script.text)


def mark_presence_heading(document):
    for node in document.get_element_by_id("userMenuPopover").xpath(
        './/div[normalize-space(text())="Anwesenheitsstatus"]'
    ):
        classes = node.get("class", "").split()
        if "portal-presence-heading" not in classes:
            node.set("class", " ".join([*classes, "portal-presence-heading"]))


def update_saved_review(path):
    source = path.read_text(encoding="utf-8")
    document = html.fromstring(source)
    if document.xpath('//*[@id="profileGenderTrigger"]'):
        label = document.get_element_by_id("profileGenderLabel")
        previous_label = html.tostring(label, encoding="unicode", with_tail=False)
        mark_profile_gender_required(document)
        assert previous_label in source
        source = source.replace(
            previous_label,
            html.tostring(label, encoding="unicode", with_tail=False),
            1,
        )
        # Nur das Pflichtattribut am benannten Auslöser ersetzen, seine SVGs erhalten.
        source = re.sub(
            r'(<button\b[^>]*id="profileGenderTrigger"[^>]*)(>)',
            lambda match: (
                re.sub(r'\saria-required="[^"]*"', "", match[1])
                + ' aria-required="true"'
                + match[2]
            ),
            source,
            count=1,
        )
    # Den erklärenden Zusatz beim Geschlecht vollständig entfernen, ohne Leerraumfeld.
    for node in document.xpath('//*[@id="profileGenderContainer"]/span'):
        markup = html.tostring(node, encoding="unicode", with_tail=False)
        assert markup in source
        source = source.replace(markup, "", 1)
        node.getparent().remove(node)
    menu = document.get_element_by_id("userMenuPopover")
    previous_menu = html.tostring(menu, encoding="unicode", with_tail=False)
    mark_presence_heading(document)
    assert previous_menu in source
    source = source.replace(
        previous_menu, html.tostring(menu, encoding="unicode", with_tail=False), 1
    )
    session = document.get_element_by_id("session-confirm")
    refine_session(session)
    # Nur den benannten Dialog und das betroffene Formularraster serialisieren.
    source = re.sub(
        r'<dialog\b[^>]*id="session-confirm"[\s\S]*?</dialog>',
        lambda _: html.tostring(session, encoding="unicode", with_tail=False),
        source,
        count=1,
    )
    birth = document.get_element_by_id("geburtsdatum").getparent().getparent()
    # Der Quelltext enthält identische Attribute; die originalen Zeilen erhalten.
    if not document.xpath('//*[@id="profileGenderTrigger"]'):
        add_profile_gender(document)
        new_field = document.get_element_by_id("profileGenderContainer")
        # An der bestehenden Geburtstagsgruppe einfügen statt das ganze Dokument umzuschreiben.
        original_birth = html.tostring(birth, encoding="unicode", with_tail=False)
        assert original_birth in source
        source = source.replace(
            original_birth,
            html.tostring(new_field, encoding="unicode") + original_birth,
            1,
        )
        menu = document.get_element_by_id("profileGenderDropdown")
        source = source.replace(
            "<script", html.tostring(menu, encoding="unicode") + "<script", 1
        )
    else:
        menu = document.get_element_by_id("profileGenderDropdown")
        markup = html.tostring(menu, encoding="unicode", with_tail=False)
        assert markup in source
        source = source.replace(markup, "", 1)
        source = source.replace("<script", markup + "<script", 1)
    # Der einzige Interaktionsblock hat seinen vorhandenen CSP-Nonce.
    source = re.sub(
        r"(<script\b[^>]*>)([\s\S]*?)(</script>)",
        lambda match: match[1] + review_script(match[2]) + match[3],
        source,
        count=1,
    )
    path.write_text(source, encoding="utf-8")
