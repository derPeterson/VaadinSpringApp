package de.derpeterson.browsertests.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.BoundingBox;
import com.microsoft.playwright.options.ScreenshotAnimations;
import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import java.io.ByteArrayInputStream;

/** Sichert dieselben sichtbaren Dialogzonen vor und nach der nativen Übernahme. */
public final class ConfirmationAppearanceChecks {
  private ConfirmationAppearanceChecks() {}

  /** Prüft die Aktionsbreite innerhalb der vorhandenen nativen Footer-Innenabstände. */
  public static void assertMobileFullWidthActions(
      Page browserPage, Locator confirmationDialogLocator) {
    if (browserPage.viewportSize().width > 600) {
      return;
    }
    Locator footerLocator =
        confirmationDialogLocator.locator("[part=overlay]").first().locator("[part=footer]");
    Locator actionButtonsLocator = confirmationDialogLocator.locator(".cosmos-confirmation-action");
    assertThat(actionButtonsLocator).hasCount(2);
    double availableWidth =
        ((Number)
                footerLocator.evaluate(
                    "element => element.clientWidth - parseFloat(getComputedStyle(element).paddingLeft) - parseFloat(getComputedStyle(element).paddingRight)"))
            .doubleValue();
    double expectedLeft =
        footerLocator.boundingBox().x
            + ((Number)
                    footerLocator.evaluate(
                        "element => parseFloat(getComputedStyle(element).paddingLeft)"))
                .doubleValue();
    double rowGap =
        ((Number) footerLocator.evaluate("element => parseFloat(getComputedStyle(element).rowGap)"))
            .doubleValue();
    BoundingBox firstActionBounds = actionButtonsLocator.nth(0).boundingBox();
    BoundingBox secondActionBounds = actionButtonsLocator.nth(1).boundingBox();
    assertEquals(availableWidth, firstActionBounds.width, 1);
    assertEquals(availableWidth, secondActionBounds.width, 1);
    assertEquals(expectedLeft, firstActionBounds.x, 1);
    assertEquals(expectedLeft, secondActionBounds.x, 1);
    assertEquals(firstActionBounds.height, secondActionBounds.height, 1);
    assertEquals(
        rowGap, Math.abs(secondActionBounds.y - firstActionBounds.y) - firstActionBounds.height, 1);
  }

  public static void capture(Page browserPage, Locator confirmationDialogLocator, String caseName) {
    Locator overlayLocator = confirmationDialogLocator.locator("[part~='overlay']").first();
    assertThat(overlayLocator).isVisible();
    browserPage.evaluate("() => document.fonts.ready");
    browserPage.mouse().move(0, 0);
    String evidenceName = caseName + " " + browserPage.viewportSize().width;
    Allure.attachment(
        evidenceName,
        "image/png",
        new ByteArrayInputStream(
            overlayLocator.screenshot(
                new Locator.ScreenshotOptions().setAnimations(ScreenshotAnimations.DISABLED))),
        AttachmentOptions.empty());
    String measurementsJson =
        (String)
            confirmationDialogLocator.evaluate(
                """
                dialog => {
                  const elements = [...dialog.querySelectorAll(
                    '.cosmos-panel-title,.portal-confirm-body,.portal-confirm-question,' +
                    '.portal-confirm-explanation,.portal-context-card,vaadin-button')];
                  return JSON.stringify(elements.filter(element => element && element.getBoundingClientRect().height > 0)
                    .map(element => {
                      const rect = element.getBoundingClientRect();
                      const style = getComputedStyle(element);
                      return {width:rect.width, height:rect.height, padding:style.padding,
                        gap:style.gap, font:style.font, color:style.color, background:style.backgroundColor,
                        border:style.border, borderRadius:style.borderRadius};
                    }));
                }
                """);
    Allure.attachment(evidenceName + " Messwerte", "application/json", measurementsJson);
    assertTrue(
        (Boolean)
            overlayLocator.evaluate("overlay => overlay.scrollHeight <= overlay.clientHeight + 1"),
        "Langer Inhalt muss innerhalb der Dialogzonen scrollen und darf den Footer nicht abschneiden.");
    String overlayDetailsJson =
        (String)
            overlayLocator.evaluate(
                """
                overlay => {
                  const describe = element => {
                    if (!element) return null;
                    const style = getComputedStyle(element);
                    return {tag:element.localName, width:element.getBoundingClientRect().width,
                      maxWidth:style.maxWidth, inset:style.inset, overflowY:style.overflowY,
                      clientWidth:element.clientWidth, scrollWidth:element.scrollWidth,
                      height:element.getBoundingClientRect().height,
                      clientHeight:element.clientHeight, scrollHeight:element.scrollHeight,
                      flex:style.flex, display:style.display, direction:style.flexDirection,
                      minHeight:style.minHeight,
                      viewportInset:style.getPropertyValue('--vaadin-overlay-viewport-inset'),
                      dialogMaxWidth:style.getPropertyValue('--vaadin-dialog-max-width'),
                      confirmMaxWidth:style.getPropertyValue('--vaadin-confirm-dialog-max-width')};
                  };
                  return JSON.stringify({overlay:describe(overlay),
                    parent:describe(overlay.parentElement),
                    content:describe(overlay.querySelector('[part~="content"]')),
                    message:describe(overlay.querySelector('[part~="message"]')),
                    header:describe(overlay.querySelector('[part~="header"]')),
                    footer:describe(overlay.querySelector('[part~="footer"]')),
                    host:describe(overlay.getRootNode().host), viewport:innerWidth});
                }
                """);
    Allure.attachment(evidenceName + " Overlaydetails", "application/json", overlayDetailsJson);
  }
}
