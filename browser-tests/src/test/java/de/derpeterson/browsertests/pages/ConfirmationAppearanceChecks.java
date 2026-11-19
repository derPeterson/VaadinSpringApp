package de.derpeterson.browsertests.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.BoundingBox;
import com.microsoft.playwright.options.ScreenshotAnimations;
import com.microsoft.playwright.options.ViewportSize;
import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.regex.Pattern;

/** Sichert dieselben sichtbaren Dialogzonen vor und nach der nativen Übernahme. */
public final class ConfirmationAppearanceChecks {
  private ConfirmationAppearanceChecks() {}

  private static void assertActionLayout(Locator confirmationDialogLocator, int viewportWidth) {
    Locator footerLocator =
        confirmationDialogLocator.locator("[part~='overlay'] > [part~='footer']");
    Locator cancelButtonLocator =
        confirmationDialogLocator.locator("vaadin-button[slot=cancel-button]");
    Locator confirmButtonLocator =
        confirmationDialogLocator.locator("vaadin-button[slot=confirm-button]");
    boolean discardsFirst =
        (Boolean)
            confirmationDialogLocator.evaluate(
                "dialog => dialog.matches('.cosmos-unsaved-changes-dialog')");
    boolean stacksAt600 =
        discardsFirst
            || (Boolean)
                confirmationDialogLocator.evaluate(
                    "dialog => dialog.matches('.portal-other-sessions-confirmation')");
    Locator firstButtonLocator = discardsFirst ? confirmButtonLocator : cancelButtonLocator;
    Locator secondButtonLocator = discardsFirst ? cancelButtonLocator : confirmButtonLocator;

    assertThat(footerLocator).hasCSS("display", "grid");
    for (Locator buttonLocator : List.of(firstButtonLocator, secondButtonLocator)) {
      assertThat(buttonLocator).isVisible();
      assertThat(buttonLocator).hasText(Pattern.compile("\\S"));

      Locator labelLocator = buttonLocator.locator("[part~='label']");
      assertThat(labelLocator).isVisible();
      assertTrue(
          (Boolean)
              labelLocator.evaluate(
                  """
                  label => label.scrollWidth <= label.clientWidth + 1
                    && label.scrollHeight <= label.clientHeight + 1
                  """));
    }

    BoundingBox firstButtonBounds = firstButtonLocator.boundingBox();
    BoundingBox secondButtonBounds = secondButtonLocator.boundingBox();
    assertEquals(firstButtonBounds.width, secondButtonBounds.width, 1);

    if (viewportWidth <= (stacksAt600 ? 600 : 479)) {
      double availableWidth =
          ((Number)
                  footerLocator.evaluate(
                      """
                      footer => {
                        const style = getComputedStyle(footer);
                        return footer.clientWidth - parseFloat(style.paddingLeft)
                          - parseFloat(style.paddingRight);
                      }
                      """))
              .doubleValue();
      assertEquals(availableWidth, firstButtonBounds.width, 1);
      assertEquals(firstButtonBounds.x, secondButtonBounds.x, 1);
      double rowGap =
          ((Number) footerLocator.evaluate("footer => parseFloat(getComputedStyle(footer).rowGap)"))
              .doubleValue();
      assertEquals(
          rowGap, secondButtonBounds.y - firstButtonBounds.y - firstButtonBounds.height, 1);
    } else {
      assertEquals(
          firstButtonBounds.y + firstButtonBounds.height / 2,
          secondButtonBounds.y + secondButtonBounds.height / 2,
          1);
      assertTrue(firstButtonBounds.x + firstButtonBounds.width <= secondButtonBounds.x);
    }
  }

  public static void capture(Page browserPage, Locator confirmationDialogLocator, String caseName) {
    Locator overlayLocator = confirmationDialogLocator.locator("[part~='overlay']").first();
    assertThat(overlayLocator).isVisible();
    assertThat(confirmationDialogLocator.locator("vaadin-confirm-dialog-overlay[opening]"))
        .hasCount(0);
    browserPage.evaluate("() => document.fonts.ready");

    ViewportSize viewportSize = browserPage.viewportSize();
    assertActionLayout(confirmationDialogLocator, viewportSize.width);
    if (viewportSize.width == 390) {
      browserPage.setViewportSize(320, viewportSize.height);
      try {
        assertActionLayout(confirmationDialogLocator, 320);
      } finally {
        browserPage.setViewportSize(viewportSize.width, viewportSize.height);
      }
    }

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
