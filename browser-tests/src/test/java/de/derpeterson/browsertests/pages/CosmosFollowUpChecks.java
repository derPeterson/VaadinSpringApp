package de.derpeterson.browsertests.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.BoundingBox;
import de.derpeterson.browsertests.i18n.UiTexts;

/** Gemeinsame sichtbare Verträge der gezielt nachgeprüften Konto- und Detailsdarstellung. */
public final class CosmosFollowUpChecks {
  private CosmosFollowUpChecks() {}

  public static void assertNativeRequiredIndicator(Locator field) {
    assertThat(field).hasAttribute("required", "");
    Locator indicator =
        field.locator("[part~=label] > slot[name=label] + [part=required-indicator]");
    assertThat(indicator).isVisible();
    assertThat(indicator).hasCSS("position", "static");
    assertThat(indicator).hasCSS("color", "rgb(248, 113, 113)");
    assertEquals(
        "\"*\"", indicator.evaluate("indicator => getComputedStyle(indicator, '::after').content"));
  }

  public static void assertReadableBadge(Locator badge, int height) {
    assertThat(badge).isVisible();
    assertThat(badge).hasCSS("height", height + "px");
    assertTrue(
        (boolean)
            badge.evaluate(
                """
                badge => badge.textContent.trim().length > 0
                  && badge.scrollWidth <= badge.clientWidth + 1
                  && badge.scrollHeight <= badge.clientHeight + 1
                """),
        "Die Badge-Beschriftung bleibt vollständig lesbar.");
  }

  public static void assertCloseCircle(Locator closeButton, String accessibleName) {
    assertThat(closeButton).isVisible();
    assertThat(closeButton).hasAccessibleName(accessibleName);
    assertThat(closeButton).hasCSS("width", "36px");
    assertThat(closeButton).hasCSS("height", "36px");
    Locator icon = closeButton.locator("vaadin-icon");
    assertThat(icon).hasAttribute("src", "icons/lucide/circle-x.svg");
    assertThat(icon).hasCSS("width", "18px");
    assertThat(icon).hasCSS("height", "18px");
    assertTrue(
        (boolean)
            closeButton.evaluate(
                """
                button => {
                  const buttonBounds = button.getBoundingClientRect();
                  const iconBounds = button.querySelector('vaadin-icon').getBoundingClientRect();
                  return Math.abs(iconBounds.x + iconBounds.width / 2 - buttonBounds.x - buttonBounds.width / 2) < 1
                    && Math.abs(iconBounds.y + iconBounds.height / 2 - buttonBounds.y - buttonBounds.height / 2) < 1;
                }
                """),
        "Das Kreis-X sitzt mittig im Schließen-Button.");
  }

  public static void assertSwitchAppearance(Locator control) {
    Locator input = control.locator("input[slot=input]");
    Locator track = control.locator("[part=switch]");
    Locator marker = control.locator("[part=marker]");
    Locator label = control.locator("[part=label]");
    assertThat(input).isEnabled();
    assertThat(track).isVisible();
    assertThat(track).hasCSS("width", "34px");
    assertThat(track).hasCSS("height", "20px");
    assertThat(track).hasCSS("border-radius", "999px");
    assertThat(marker).hasCSS("width", "14px");
    assertThat(marker).hasCSS("height", "14px");
    assertThat(marker).hasCSS("border-radius", "50%");
    boolean checked = input.isChecked();
    assertThat(track).hasCSS("background-color", checked ? "rgb(0, 119, 182)" : "rgb(9, 17, 32)");
    assertThat(marker)
        .hasCSS("background-color", checked ? "rgb(255, 255, 255)" : "rgb(148, 163, 184)");
    assertThat(label).isVisible();
    assertTrue(
        Boolean.TRUE.equals(label.evaluate("label => label.scrollWidth <= label.clientWidth + 1")),
        "Die Schalterbeschriftung muss vollständig in die Zeile passen.");

    BoundingBox trackBounds = track.boundingBox();
    BoundingBox markerBounds = marker.boundingBox();
    assertEquals(
        trackBounds.y + trackBounds.height / 2, markerBounds.y + markerBounds.height / 2, 1);
    double markerOffset =
        markerBounds.x + markerBounds.width / 2 - trackBounds.x - trackBounds.width / 2;
    assertEquals(checked ? 7 : -7, markerOffset, 1);
  }

  public static void assertSwitchFocus(Locator control) {
    assertThat(control.locator("input[slot=input]")).isFocused();
    Locator track = control.locator("[part=switch]");
    assertThat(track).hasCSS("outline-width", "2px");
    assertThat(track).hasCSS("outline-offset", "3px");
    assertThat(track).hasCSS("outline-color", "rgb(56, 189, 248)");
  }

  public static Locator assertChoiceScrollerPadding(Page page, boolean multiSelect) {
    String componentName = multiSelect ? "vaadin-multi-select-combo-box" : "vaadin-combo-box";
    Locator overlay = page.locator(componentName + "-overlay[opened]");
    assertThat(overlay.locator("[part=overlay]")).isVisible();
    assertThat(overlay).not().hasAttribute("opening", "");
    assertThat(overlay.locator("[part=content]")).hasCSS("padding", "0px");
    Locator scroller = page.locator(componentName + "-scroller:visible");
    assertThat(scroller.locator("#selector")).hasCSS("border-width", "8px");
    return scroller;
  }

  public static void assertSingleChoiceSpacing(Page page) {
    Locator scroller = assertChoiceScrollerPadding(page, false);
    Locator optionRows =
        scroller.locator("vaadin-combo-box-item:visible .cosmos-reference-option-row");
    assertThat(optionRows.nth(1)).isVisible();
    BoundingBox firstRow = optionRows.first().boundingBox();
    BoundingBox secondRow = optionRows.nth(1).boundingBox();
    assertEquals(2, secondRow.y - firstRow.y - firstRow.height, 0.5);
    for (Locator selectedOption :
        scroller.locator("vaadin-combo-box-item[selected]:visible").all()) {
      Locator selectionIcon = selectedOption.locator(".cosmos-reference-option-check");
      assertThat(selectionIcon).isVisible();
      assertThat(selectionIcon).hasCSS("opacity", "1");
    }
    if (Boolean.TRUE.equals(
        scroller.evaluate("scroller => scroller.scrollHeight <= scroller.clientHeight + 1"))) {
      BoundingBox scrollerBounds = scroller.boundingBox();
      BoundingBox lastRow = optionRows.last().boundingBox();
      assertEquals(9, firstRow.y - scrollerBounds.y, 1);
      assertEquals(9, scrollerBounds.y + scrollerBounds.height - lastRow.y - lastRow.height, 1);
    }
  }

  /** Prüft einen anfangs eingeklappten Schalter und hinterlässt ihn wieder eingeklappt. */
  public static void assertDetailsWithoutHover(Locator detailsToggleLocator) {
    String backgroundColor =
        detailsToggleLocator
            .evaluate("element => getComputedStyle(element).backgroundColor")
            .toString();

    detailsToggleLocator.hover();
    assertThat(detailsToggleLocator).hasCSS("background-color", backgroundColor);
    assertThat(detailsToggleLocator).hasCSS("cursor", "pointer");

    detailsToggleLocator.focus();
    detailsToggleLocator.press("Enter");
    assertThat(detailsToggleLocator).hasAttribute("aria-expanded", "true");
    detailsToggleLocator.press("Enter");
    assertThat(detailsToggleLocator).hasAttribute("aria-expanded", "false");
  }

  public static void assertPendingAccountStyle(Page browserPage, UiTexts uiTexts) {
    assertThat(browserPage.locator(".cosmos-status-icon--warning"))
        .hasCSS("color", "rgb(251, 191, 36)");

    Locator emailAddressLocator = browserPage.locator(".pending-verification-address");
    assertThat(emailAddressLocator.locator(".pending-verification-address-label"))
        .hasText(uiTexts.getPublicEmailField());
    Locator statusBadgeLocator = emailAddressLocator.locator("vaadin-badge");
    assertThat(statusBadgeLocator).hasCount(1);
    assertThat(statusBadgeLocator).hasCSS("border-top-width", "1px");

    assertThat(browserPage.locator(".pending-verification-folder-emphasis"))
        .hasText(uiTexts.getEmailVerificationSpamFolderLabel());
    assertThat(browserPage.locator(".pending-verification-folder-emphasis"))
        .hasCSS("font-weight", "700");

    Locator resendHintLocator = browserPage.locator(".pending-verification-resend-hint");
    assertTrue(
        (Boolean)
            resendHintLocator.evaluate(
                "element => element.scrollWidth <= element.clientWidth + 1"));
    if (browserPage.viewportSize().width >= 1000) {
      assertTrue(
          resendHintLocator.boundingBox().height <= 24,
          "Der kurze Versandhinweis muss auf Desktop einzeilig bleiben.");
    }
  }

  public static void assertAccountType(
      Locator dialogLocator, UiTexts uiTexts, boolean developmentAccountExpected) {
    Locator accountTypeBadgeLocator = dialogLocator.locator("#admin-user-account-type");
    assertThat(accountTypeBadgeLocator)
        .hasText(
            developmentAccountExpected
                ? uiTexts.getDevelopmentAccountLabel()
                : uiTexts.getStandardAccountLabel());
    assertThat(accountTypeBadgeLocator)
        .hasCSS("color", developmentAccountExpected ? "rgb(216, 180, 254)" : "rgb(56, 189, 248)");
    assertThat(accountTypeBadgeLocator).hasCSS("border-top-width", "1px");
  }
}
