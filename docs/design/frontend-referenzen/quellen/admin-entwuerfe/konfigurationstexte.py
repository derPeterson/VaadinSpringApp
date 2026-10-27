"""Texte und Eingabegrenzen aus den bestehenden typisierten Adminquellen übernehmen."""

import json
import re
import runpy
from pathlib import Path

root = Path(__file__).parent
repo = root.parents[4]
java_root = repo / "app/src/main/java/de/derpeterson/app"
shared_root = repo / "shared/src/main/java/de/derpeterson/app"


def method_body(source, name):
    # Methodendeklaration bis zur zugehörigen schließenden Klammer isolieren.
    match = re.search(r"\b" + name + r"\([^)]*\)\s*\{", source)
    assert match is not None, name
    start = match.end()
    depth = 1
    for index in range(start, len(source)):
        if source[index] == "{":
            depth += 1
        elif source[index] == "}":
            depth -= 1
            if depth == 0:
                return source[start:index]
    raise AssertionError(name)


def switch_getters(source, method):
    # Die Enum-ID bleibt fachliche Identität, der Getter liefert den Katalogtext.
    return dict(
        re.findall(
            r"\bcase\s+([A-Z_]+)\s*->\s*(get\w+)\(\);",
            method_body(source, method),
        )
    )


def getter_calls(source):
    # Getterargumente einschließlich verschachtelter Duration-Aufrufe erhalten.
    for match in re.finditer(r"messageProperties\.(get\w+)\(", source):
        depth = 1
        for index in range(match.end(), len(source)):
            if source[index] == "(":
                depth += 1
            elif source[index] == ")":
                depth -= 1
                if depth == 0:
                    yield match[1], source[match.end() : index].strip()
                    break


def configuration_texts(inventory):
    texts = runpy.run_path(str(root.parent / "eingabefelder.py"))["application_texts"]()
    catalog = (shared_root / "i18n/MessageTexts.java").read_text(encoding="utf-8")
    properties = (java_root / "i18n/MessageProperties.java").read_text(encoding="utf-8")
    section = (java_root / "views/admin/AdminConfigurationSection.java").read_text(
        encoding="utf-8"
    )
    entries = (java_root / "model/enums/ConfigEntry.java").read_text(encoding="utf-8")
    token_policy = (shared_root / "config/TokenDurationPolicy.java").read_text(
        encoding="utf-8"
    )
    # Nur die vorhandene Getter-zu-Schlüssel-Zuordnung aus Shared lesen.
    getter_keys = dict(
        re.findall(
            r'public String (get\w+)\([^)]*\)\s*\{\s*return get(?:Formatted)?Translation\(\s*"([^"]+)"',
            catalog,
        )
    )

    def text(getter, *arguments):
        value = texts[getter_keys[getter]]
        for index, argument in enumerate(arguments):
            value = value.replace("{" + str(index) + "}", str(argument))
        return value

    mail_section = (java_root / "views/admin/AdminMailTestSection.java").read_text(
        encoding="utf-8"
    )
    mail_texts = {}
    # Typisierte Getter sowohl bei Aufrufen mit Punkt als auch bei Supplier-Referenzen (::).
    for getter in sorted(
        set(re.findall(r"messageProperties(?:\.|::)(getAdminMail\w+)", mail_section))
    ):
        name = getter.removeprefix("getAdminMail")
        mail_texts[name[0].lower() + name[1:]] = text(getter)
    inventory["mail"]["texts"] = mail_texts
    inventory["mail"]["notificationTitles"] = {
        "error": text("getBaseFailedTitle"),
        "success": text("getBaseSuccessTitle"),
    }
    inventory["mail"]["fields"] = [
        mail_texts[identifier] for identifier in ("recipient", "subject", "body")
    ]
    inventory["mail"]["placeholders"] = {
        "recipient": texts["account.form.email.placeholder"],
        "subject": mail_texts["defaultSubject"],
        "body": mail_texts["defaultBody"],
    }
    inventory["mail"]["status"] = {
        state: text(getter)
        for state, getter in {
            "ready": "getMaintenanceStatusOnline",
            "maintenance": "getMaintenanceStatusActive",
            "unavailable": "getMaintenanceStatusUnavailable",
        }.items()
    }

    notification_section = (
        java_root / "views/admin/AdminUserNotificationsSection.java"
    ).read_text(encoding="utf-8")
    notification_texts = {}
    # Direkte Aufrufe und Supplier-Referenzen des vorhandenen Textkatalogs erfassen.
    for getter in sorted(
        set(re.findall(r"texts(?:\.|::)(getNotifications\w+)", notification_section))
    ):
        name = getter.removeprefix("getNotifications")
        name = name[0].lower() + name[1:]
        if getter in getter_keys:
            notification_texts[name] = text(getter)
        else:
            # Die beiden vorhandenen Pluralmethoden wählen zwischen je zwei Textschlüsseln.
            keys = re.findall(r'"(notifications\.[^"]+)"', method_body(catalog, getter))
            assert len(keys) == 2, getter
            notification_texts[name] = {
                "one": texts[keys[0]],
                "other": texts[keys[1]],
            }
    notification_texts["sourceAdministration"] = text(
        "getNotificationsSourceAdministration"
    )
    notification_texts["accessDenied"] = text("getBaseErrorAccessDenied")
    inventory["notifications"]["texts"] = notification_texts
    inventory["notifications"]["notificationTitles"] = inventory["mail"][
        "notificationTitles"
    ].copy()
    inventory["notifications"]["status"] = inventory["mail"]["status"].copy()
    pagination_source = (java_root / "views/admin/AdminUserPagination.java").read_text(
        encoding="utf-8"
    )
    # Auch die sichtbaren Paginationstexte stammen aus den tatsächlichen Aufrufern.
    inventory["notifications"]["pagination"] = {
        getter.removeprefix("getAdminUsersPagination"): text(getter)
        for getter in sorted(
            set(re.findall(r"texts\.(getAdminUsersPagination\w+)", pagination_source))
        )
    }
    service_source = (java_root / "service/UserNotificationService.java").read_text(
        encoding="utf-8"
    )
    # Die benannten fachlichen Längenbegrenzungen verwenden, nicht Texte zurückparsen.
    for constant, field in (("TITLE_LIMIT", "titleLimit"), ("BODY_LIMIT", "bodyLimit")):
        match = re.search(
            r"public static final int " + constant + r" = (\d+);", service_source
        )
        assert match is not None, constant
        inventory["notifications"][field] = int(match[1])
    inventory["notifications"]["fields"] = [
        notification_texts[field]
        for field in ("everyone", "recipients", "subject", "body")
    ]

    # Die Seitengrößen des tatsächlich verwendeten Historien-Paginators übernehmen.
    page_sizes = re.search(
        r"List\.of\(([\d,\s]+)\)",
        method_body(notification_section, "createHistoryPagination"),
    )
    assert page_sizes is not None
    inventory["notifications"]["pageSizes"] = [
        int(value.strip()) for value in page_sizes[1].split(",")
    ]
    inventory["notifications"]["history"] = (
        "Nur eigene gesendete Benachrichtigungen mit aufklappbaren Mitteilungsdetails "
        "und paginierter Historie. Empfängernamen und E-Mail-Adressen werden in der "
        "geprüften Vorschau angezeigt, nicht in der Historie. Kein E-Mail-Versand."
    )

    # Benannte ganzzahlige Prüfgrenzen und Token-Mindestzeiten aus den Deklarationen lesen.
    constants = {
        name: int(value)
        for name, value in re.findall(
            r"private static final int (\w+) = (\d+);", section
        )
    }
    constants["Integer.MAX_VALUE"] = 2147483647
    minimums = dict(
        re.findall(r"(MIN_\w+) = Duration\.ofMinutes\((\d+)\)", token_policy)
    )
    # Kommagetrennte Enumfälle der jeweiligen MIN-Konstante zuordnen.
    token_groups = re.findall(
        r"case\s+([A-Z_,\s]+)\s*->\s*TokenDurationPolicy\.(MIN_\w+);", entries
    )
    token_minimums = {
        key.strip(): int(minimums[minimum])
        for keys, minimum in token_groups
        for key in keys.split(",")
    }
    # Ausschließlich öffentliche Enum-Standardwerte, keine Umgebung/ConfigService-Werte.
    defaults = {}
    pattern = r'\b([A-Z_]+)\(\s*"[^"]+",\s*(Duration\.of(?:Minutes|Hours|Days)\(\d+\)\.toString\(\)|"(?:\\.|[^"\\])*"|\d+|true|false)\s*\)'
    for name, literal in re.findall(pattern, entries):
        if literal.startswith('"'):
            defaults[name] = json.loads(literal)
        elif literal.startswith("Duration."):
            unit, count = re.search(
                r"of(Minutes|Hours|Days)\((\d+)\)", literal
            ).groups()
            defaults[name] = (
                f"PT{int(count) * 24}H"
                if unit == "Days"
                else f"PT{count}{'H' if unit == 'Hours' else 'M'}"
            )
        else:
            defaults[name] = literal

    # Je switch-Fall die List.of-Argumente bis zum nächsten Fall beziehungsweise Switchende erfassen.
    rule_groups = re.findall(
        r"\bcase\s+([A-Z_,\s]+)\s*->\s*List\.of\((.*?)\);(?=\s*(?:case|\};))",
        method_body(section, "buildInputRules"),
        re.DOTALL,
    )
    rules_by_entry = {
        key.strip(): list(getter_calls(calls))
        for keys, calls in rule_groups
        for key in keys.split(",")
    }
    labels = switch_getters(properties, "getAdminConfigEntryLabel")
    tooltips = switch_getters(properties, "getAdminConfigEntryTooltip")
    expected = {field["id"] for field in inventory["fields"]}
    assert (
        expected == set(defaults) == set(rules_by_entry) == set(labels) == set(tooltips)
    )
    for field in inventory["fields"]:
        identifier = field["id"]
        rules = []
        for getter, arguments in rules_by_entry[identifier]:
            values = []
            for argument in arguments.split(",") if arguments else []:
                argument = argument.strip()
                values.append(
                    token_minimums[identifier]
                    if argument == "configEntry.getMinimumTokenDuration().toMinutes()"
                    else constants[argument]
                    if argument in constants
                    else int(argument)
                )
            rules.append(text(getter, *values))
        default = defaults[identifier]
        confidential = identifier in {"REMEMBER_ME_SECRET_KEY", "MAIL_PASSWORD"}
        display_default = (
            text("getAdminConfigDefaultEmpty")
            if not default
            else text("getAdminConfigDefaultConfidential")
            if confidential
            else default
        )
        retention = identifier in {
            "USER_NOTIFICATIONS_INBOX_RETENTION_DAYS",
            "USER_NOTIFICATIONS_HISTORY_RETENTION_DAYS",
        }
        field.update(
            required=field["type"] != "boolean"
            and not any(
                getter
                in {
                    "getAdminConfigRuleOptional",
                    "getAdminConfigRuleEmptyUsesDefault",
                    "getAdminConfigRuleEmptyUnlimited",
                }
                for getter, _ in rules_by_entry[identifier]
            ),
            label=text(labels[identifier]),
            tooltip=text(tooltips[identifier]),
            defaultText=(
                text(
                    "getAdminConfigInitialValue"
                    if retention
                    else "getAdminConfigDefault"
                )
                + ": "
                + display_default
            ),
            rules=rules,
            placeholder=(
                text("getAdminConfigUnlimited")
                if retention
                else text("getAdminConfigPlaceholderEmpty")
                if not default
                else text("getAdminConfigDefaultConfidential")
                if confidential
                else default
            ),
        )
        field.pop("optional", None)
    group_labels = switch_getters(catalog, "getAdminConfigGroupLabel")
    group_descriptions = switch_getters(catalog, "getAdminConfigGroupDescription")
    assert set(inventory["groups"]) == set(group_labels) == set(group_descriptions)
    return {
        "groups": {
            group: {
                "label": text(group_labels[group]),
                "description": text(group_descriptions[group]),
            }
            for group in inventory["groups"]
        },
        "title": text("getAdminConfigTitle"),
        "description": text("getAdminConfigDescription"),
        "save": text("getAdminConfigSave"),
        "reload": text("getAdminConfigReload"),
        "saveHint": text("getAdminConfigSaveHint"),
        "inputRules": text("getAdminConfigInputRules"),
        "countOne": texts["adminView.config.settings_count.one"],
        "countOther": texts["adminView.config.settings_count.other"],
    }
