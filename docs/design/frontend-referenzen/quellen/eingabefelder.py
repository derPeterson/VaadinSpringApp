"""Platzhalter aus dem Anwendungskatalog und gemeinsame Feldicons einbetten."""

import hashlib
import re
from pathlib import Path

from lxml import etree, html

root = Path(__file__).parent
reference = root.parent
catalog_path = (
    reference.parents[2] / "app/src/main/resources/i18n/messages_de.properties"
)


def application_texts():
    values = {}
    for line in catalog_path.read_text(encoding="iso-8859-1").splitlines():
        if "=" in line and not line.startswith(("#", "!")):
            key, value = line.split("=", 1)
            # Java-Properties dürfen einzelne Unicodezeichen als vier Hexziffern ablegen.
            values[key] = re.sub(
                r"\\u([0-9a-fA-F]{4})", lambda m: chr(int(m[1], 16)), value
            )
    return values


def placeholders(area, texts):
    def account(name):
        return texts[f"account.form.{name}.placeholder"]

    if area == "admin":
        return {
            "editorFirst": account("first_name"),
            "editorLast": account("last_name"),
            "editorEmail": account("email"),
            "editorPassword": account("password"),
            "editorConfirm": account("confirm_password"),
            "birthCalendarInput": texts["registrationView.birth_date_placeholder"],
            "userSearch": texts["admin.users.search.placeholder"],
            "activityCalendarInput": "TT.MM.JJJJ",
            "filterTime": "HH:MM",
        }
    if area == "portal":
        return {
            "vorname": account("first_name"),
            "nachname": account("last_name"),
            "email": account("email"),
            "geburtsdatum": texts["registrationView.birth_date_placeholder"],
            "n-search": texts["notifications.search"],
            "newEmail": account("email"),
            "confirmEmail": account("confirm_email"),
            "emailPassword": account("password"),
            "newPassword": account("new_password"),
            "confirmPassword": account("confirm_password"),
            "currentPassword": account("password"),
        }
    return {
        "login-email": account("email"),
        "login-password": account("password"),
        "register-firstname": account("first_name"),
        "register-lastname": account("last_name"),
        "register-email": account("email"),
        "register-email-confirm": account("confirm_email"),
        "register-password": account("password"),
        "register-password-confirm": account("confirm_password"),
        "register-birth": texts["registrationView.birth_date_placeholder"],
        "forgot-email": account("email"),
        "reset-password": account("new_password"),
        "reset-password-confirm": account("confirm_password"),
    }


def harmonize_document(document, area):
    texts = application_texts()
    for identifier, value in placeholders(area, texts).items():
        document.get_element_by_id(identifier).set("placeholder", value)
    for wrapper in decorative_prefix_wrappers(document):
        # Nur die direkten dekorativen Präfixe entfernen, rechte Bedienbuttons erhalten.
        for icon in wrapper.xpath("./svg"):
            wrapper.remove(icon)
        wrapper.set("class", wrapper.get("class") + " ref-no-prefix")
    if (
        area == "public"
        and document.get_element_by_id("genderTrigger").get("data-empty") == "true"
    ):
        document.get_element_by_id("selectedGenderText").text = texts[
            "account.form.gender.placeholder"
        ]
    for button in document.xpath("//button"):
        classes = button.get("class", "").split()
        if (
            button.get("data-reveal") is not None
            or "calendar-trigger" in classes
            or button.get("role") == "combobox"
            and button.xpath("./svg")
        ):
            button.set("data-ref-field-action", "")
            if button.get("id") in ("editorGender", "genderTrigger"):
                button.set(
                    "data-ref-placeholder", texts["account.form.gender.placeholder"]
                )
                if button.get("id") == "editorGender" and not button.get("data-value"):
                    button.xpath("./span")[0].text = texts[
                        "account.form.gender.placeholder"
                    ]
    previous = document.xpath('//style[@id="reference-field-controls"]')
    style = (
        previous[0]
        if previous
        else etree.SubElement(
            document.find("head"), "style", id="reference-field-controls"
        )
    )
    style.text = (root / "eingabefelder.css").read_text(encoding="utf-8")


def decorative_prefix_wrappers(document):
    return document.xpath(
        '//*[contains(concat(" ",@class," ")," portal-security-input ")]'
        '[./svg][./input[not(@type="search")]]'
    )


def update_reference(area):
    path = reference / f"{area}.html"
    source = path.read_text(encoding="utf-8")
    document = html.fromstring(source)
    # Nur betroffene Controls austauschen, die übrige feste Referenz erhalten.
    prefix_wrappers = decorative_prefix_wrappers(document)
    original_wrappers = [
        (wrapper, html.tostring(wrapper, encoding="unicode", with_tail=False))
        for wrapper in prefix_wrappers
    ]
    # Ganze Präfixgruppen ersetzen; ihre enthaltenen Controls nicht ein zweites Mal patchen.
    nested_controls = {
        node
        for wrapper in prefix_wrappers
        for node in wrapper.xpath(".//input | .//button")
    }
    nodes = [
        node
        for node in document.xpath("//input | //button")
        if node not in nested_controls
    ]
    originals = [
        (
            node,
            html.tostring(node, encoding="unicode", with_tail=False).split(">", 1)[0]
            + ">",
        )
        for node in nodes
    ]
    old_style = document.xpath('//style[@id="reference-field-controls"]')
    gender_text = (
        document.get_element_by_id("selectedGenderText") if area == "public" else None
    )
    original_gender_text = (
        html.tostring(gender_text, encoding="unicode", with_tail=False)
        if gender_text is not None
        else None
    )
    original_style = (
        html.tostring(old_style[0], encoding="unicode", with_tail=False)
        if old_style
        else None
    )
    harmonize_document(document, area)
    for wrapper, before in original_wrappers:
        after = html.tostring(wrapper, encoding="unicode", with_tail=False)
        source, count = re.subn(
            re.escape(before),
            lambda _, markup=after: markup,
            source,
            count=1,
            flags=re.IGNORECASE,
        )
        assert count == 1, wrapper.xpath("./input")[0].get("id")
    if original_gender_text:
        source = source.replace(
            original_gender_text,
            html.tostring(gender_text, encoding="unicode", with_tail=False),
            1,
        )
    for node, before in originals:
        after = (
            html.tostring(node, encoding="unicode", with_tail=False).split(">", 1)[0]
            + ">"
        )
        if before != after:
            # SVG-Attributschreibweisen innerhalb der Controls bleiben dadurch unangetastet.
            source, count = re.subn(
                re.escape(before),
                lambda _, markup=after: markup,
                source,
                count=1,
                flags=re.IGNORECASE,
            )
            assert count == 1, node.get("id")
    style = html.tostring(
        document.get_element_by_id("reference-field-controls"),
        encoding="unicode",
        with_tail=False,
    )
    source = (
        source.replace(original_style, style, 1)
        if original_style
        else source.replace("</head>", style + "</head>", 1)
    )
    path.write_text(source, encoding="utf-8")
    preview = reference.parents[2] / "target/designkritik-stitch/portal/preview"
    if preview.exists():
        (preview / path.name).write_bytes(path.read_bytes())


def refresh_viewers():
    preview = reference.parents[2] / "target/designkritik-stitch/portal/preview"
    for name, target in (
        ("index.html", "portal.html"),
        ("visitors.html", "public.html"),
    ):
        path = reference / name
        revision = hashlib.sha256((reference / target).read_bytes()).hexdigest()[:12]
        # Nur den Revisionsparameter der zugehörigen eingebetteten Oberfläche erneuern.
        source, count = re.subn(
            re.escape(target) + r"\?rev=[a-f0-9]+",
            target + "?rev=" + revision,
            path.read_text(encoding="utf-8"),
        )
        assert count == 1, name
        path.write_text(source, encoding="utf-8")
        if preview.exists():
            (preview / name).write_bytes(path.read_bytes())


if __name__ == "__main__":
    for area in ("portal", "public"):
        update_reference(area)
    refresh_viewers()
