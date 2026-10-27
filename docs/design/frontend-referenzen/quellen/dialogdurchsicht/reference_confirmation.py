"""Gemeinsamer Bestätigungsbaustein aus der vorhandenen Benachrichtigungsreferenz."""

import re
from copy import deepcopy
from pathlib import Path

from lxml import etree, html

root = Path(__file__).parent
notification_context = "byId('n-confirm-context').textContent = kind === 'delete' ? messages.find(item => item.id === ids[0])?.title || 'Benachrichtigung' : `${ids.length} ${ids.length === 1 ? 'Nachricht' : 'Nachrichten'} betroffen`;"


def update_notification_context(source):
    replacement = (root / "confirmation-context.js").read_text(encoding="utf-8").strip()
    # Den bereits eingebetteten Kontext bis zur abschließenden DOM-Ersetzung erneuern.
    source = re.sub(
        r"(?:// Beginn Nachrichtenkontext:[^\n]*\n)?const contextTitle = document.createElement\('strong'\);[\s\S]*?byId\('n-confirm-context'\)\.replaceChildren\(contextIcon,contextTitle\);",
        lambda _: replacement,
        source,
    )
    # Neuere markierte Fassungen lassen sich ohne andere Controllerteile ersetzen.
    source = re.sub(
        r"// Beginn Nachrichtenkontext:[\s\S]*?// Ende Nachrichtenkontext\.",
        lambda _: replacement,
        source,
    )
    source = source.replace(
        notification_context,
        replacement,
    )
    source = source.replace(
        "Nachricht entfernen: ${item.title}", "Nachricht löschen: ${item.title}"
    )
    source = source.replace(
        "'Gelesene Nachrichten entfernen?'", "'Gelesene Benachrichtigungen löschen'"
    )
    source = source.replace(
        "'Benachrichtigung entfernen?'", "'Benachrichtigung löschen'"
    )
    source = source.replace(
        "aus Ihrem Postfach entfernen?", "aus Ihrem Postfach löschen?"
    )
    source = source.replace(
        "aus Ihrem persönlichen Postfach entfernt.",
        "aus Ihrem persönlichen Postfach gelöscht.",
    )
    source = source.replace("bulk ? 'Gelesene entfernen' : 'Entfernen'", "'Löschen'")
    description_end = (
        "'Möchten Sie diese Benachrichtigung aus Ihrem Postfach löschen?';"
    )
    if "byId('n-confirm-explanation').textContent" not in source:
        source = source.replace(
            description_end,
            description_end
            + "\n    byId('n-confirm-explanation').textContent = bulk ? 'Die gelesenen Benachrichtigungen werden anschließend nicht mehr in Ihrer Nachrichtenliste aufgeführt.' : 'Der Eintrag wird anschließend nicht mehr in Ihrer Nachrichtenliste aufgeführt.';"
            + "\n    byId('n-confirm-explanation').hidden = neutral;",
        )
    return source


def mark_confirmation(dialog):
    classes = dialog.get("class", "").split()
    if "portal-confirm-dialog" not in classes:
        dialog.set("class", " ".join([*classes, "portal-confirm-dialog"]))
    body = dialog.xpath('./div[contains(@class,"n-dialog-body")]')[0]
    description = body.xpath(
        './/p[@id="n-confirm-description" or @id="deleteDescription"]'
    )[0]
    description.set("class", "portal-confirm-description")
    summaries = body.xpath('./div[@class="portal-confirm-summary"]')
    summary = (
        summaries[0]
        if summaries
        else etree.Element("div", {"class": "portal-confirm-summary"})
    )
    if not summaries:
        body.insert(0, summary)
    summary.insert(0, description)
    if dialog.get("id") == "deleteUserDialog":
        warning = dialog.get_element_by_id("deleteWarning")
        warning.tag = "p"
        warning.set("class", "portal-confirm-explanation")
        summary.append(warning)
        for redundant in dialog.xpath(
            './/*[@id="n-confirm-explanation"] | .//*[contains(concat(" ",@class," ")," n-info ")]'
        ):
            redundant.getparent().remove(redundant)
    else:
        dialog.get_element_by_id("n-confirm-title").text = "Benachrichtigung löschen"
        dialog.get_element_by_id("n-confirm-label").text = "Löschen"
        dialog.get_element_by_id(
            "n-confirm-hint"
        ).text = "Es werden ausschließlich Nachrichten aus Ihrem persönlichen Postfach gelöscht."
        description.text = (
            "Möchten Sie diese Benachrichtigung aus Ihrem Postfach löschen?"
        )
        if not dialog.xpath('.//*[@id="n-confirm-explanation"]'):
            etree.SubElement(
                summary,
                "p",
                id="n-confirm-explanation",
                attrib={"class": "portal-confirm-explanation"},
            )
    context = dialog.xpath('.//*[contains(@class,"n-confirm-context")]')[0]
    context.set("class", "n-confirm-context portal-confirm-context")


def apply_reference_confirmation(document):
    for dialog in document.xpath('//dialog[@id="n-confirm" or @id="deleteUserDialog"]'):
        mark_confirmation(dialog)
    styles = document.xpath('//*[@id="portal-confirmation-style"]')
    style = (
        styles[0]
        if styles
        else etree.SubElement(
            document.find("head"), "style", id="portal-confirmation-style"
        )
    )
    style.text = (root / "reference-confirmation.css").read_text(encoding="utf-8")
    for script in document.xpath("//script[not(@src)]"):
        script.text = update_notification_context(script.text or "")


def create_user_confirmation(reference):
    dialog = deepcopy(reference.get_element_by_id("n-confirm"))
    identifiers = {
        "n-confirm": "deleteUserDialog",
        "n-confirm-title": "deleteTitle",
        "n-confirm-subtitle": "deleteSubtitle",
        "n-confirm-description": "deleteDescription",
        "n-confirm-context": "deleteIdentity",
        "n-confirm-hint": "deleteWarning",
        "n-confirm-symbol": "deleteSymbol",
        "n-confirm-submit": "confirmDelete",
        "n-confirm-label": "deleteLabel",
    }
    for node in dialog.iter():
        for attribute in ("id", "aria-labelledby", "aria-describedby"):
            value = node.get(attribute)
            if value:
                node.set(
                    attribute,
                    " ".join(identifiers.get(part, part) for part in value.split()),
                )
    dialog.set("class", "n-dialog n-confirm danger portal-confirm-dialog")
    dialog.get_element_by_id("deleteTitle").text = "Benutzer löschen"
    dialog.get_element_by_id("deleteSubtitle").text = "Bitte prüfen Sie Ihre Auswahl."
    dialog.get_element_by_id(
        "deleteDescription"
    ).text = "Diesen Benutzer wirklich löschen?"
    dialog.get_element_by_id("deleteIdentity").text = None
    dialog.get_element_by_id(
        "deleteWarning"
    ).text = "Diese Aktion kann nicht rückgängig gemacht werden. Bitte überprüfen Sie, ob Sie das richtige Benutzerkonto ausgewählt haben."
    dialog.get_element_by_id("deleteLabel").text = "Löschen"
    for button in dialog.xpath('.//button[@data-n-close="confirm"]'):
        button.attrib.pop("data-n-close")
        button.set("data-close-delete", "")
    dialog.get_element_by_id("confirmDelete").attrib.pop("data-n-action", None)
    library = reference.get_element_by_id("n-icons")
    for node in dialog.xpath(".//*[@data-n-symbol]"):
        for child in list(node):
            node.remove(child)
        node.append(
            deepcopy(
                library.xpath(
                    ".//*[@data-n-icon=$name]", name=node.get("data-n-symbol")
                )[0]
            )
        )
    mark_confirmation(dialog)
    return dialog


def update_saved_confirmation(path):
    source = path.read_text(encoding="utf-8")

    def replace_dialog(match):
        dialog = html.fragment_fromstring(match[0])
        mark_confirmation(dialog)
        return html.tostring(dialog, encoding="unicode", with_tail=False)

    # Nur den benannten statischen Dialog vom Start- bis zum Endtag ersetzen.
    updated, count = re.subn(
        r'<dialog\b[^>]*id="n-confirm"[\s\S]*?</dialog>',
        replace_dialog,
        source,
        count=1,
    )
    assert count == 1
    style = (
        '<style id="portal-confirmation-style">'
        + (root / "reference-confirmation.css").read_text(encoding="utf-8")
        + "</style>"
    )
    if 'id="portal-confirmation-style"' in updated:
        # Genau den eigenen Styleblock bis zu dessen schließendem Tag aktualisieren.
        updated = re.sub(
            r'<style\b[^>]*id="portal-confirmation-style"[\s\S]*?</style>',
            lambda _: style,
            updated,
            count=1,
        )
    else:
        updated = updated.replace("</head>", style + "</head>", 1)
    updated = update_notification_context(updated)
    path.write_text(updated, encoding="utf-8")
