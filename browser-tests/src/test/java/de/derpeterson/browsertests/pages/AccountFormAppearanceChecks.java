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

/** Sichert echte Formularaufnahmen und Feldmaße für den FormLayout-Abgleich. */
public final class AccountFormAppearanceChecks {
  private AccountFormAppearanceChecks() {}

  public static void assertEqualDialogActions(
      Page browserPage, Locator dialogLocator, String cancelText, String confirmText) {
    ViewportSize viewport = browserPage.viewportSize();
    assertDialogActionsAtWidth(dialogLocator, cancelText, confirmText, viewport.width);

    if (viewport.width == 390) {
      browserPage.setViewportSize(320, viewport.height);
      try {
        assertDialogActionsAtWidth(dialogLocator, cancelText, confirmText, 320);
      } finally {
        browserPage.setViewportSize(viewport.width, viewport.height);
      }
    }
  }

  private static void assertDialogActionsAtWidth(
      Locator dialogLocator, String cancelText, String confirmText, int viewportWidth) {
    Locator actionsLocator = dialogLocator.locator(".cosmos-dialog-actions");
    Locator actionButtonLocators = actionsLocator.locator(":scope > vaadin-button");
    Locator cancelButtonLocator = actionButtonLocators.first();
    Locator confirmButtonLocator = actionButtonLocators.last();

    assertThat(actionsLocator).hasCSS("display", "grid");
    assertThat(actionButtonLocators).hasCount(2);
    assertThat(cancelButtonLocator).hasText(cancelText);
    assertThat(confirmButtonLocator).hasText(confirmText);

    BoundingBox actionsBounds = actionsLocator.boundingBox();
    BoundingBox cancelBounds = cancelButtonLocator.boundingBox();
    BoundingBox confirmBounds = confirmButtonLocator.boundingBox();

    assertEquals(cancelBounds.width, confirmBounds.width, 1);
    if (viewportWidth <= 479) {
      double footerContentWidth =
          ((Number)
                  dialogLocator
                      .locator("[part~='footer']")
                      .evaluate(
                          """
                          footer => {
                            const style = getComputedStyle(footer);
                            return footer.clientWidth - parseFloat(style.paddingLeft)
                              - parseFloat(style.paddingRight);
                          }
                          """))
              .doubleValue();
      assertEquals(footerContentWidth, actionsBounds.width, 1);
      assertEquals(actionsBounds.width, cancelBounds.width, 1);
      assertEquals(cancelBounds.x, confirmBounds.x, 1);
      assertTrue(cancelBounds.y + cancelBounds.height <= confirmBounds.y);
    } else {
      assertEquals(cancelBounds.y, confirmBounds.y, 1);
      assertTrue(cancelBounds.x + cancelBounds.width <= confirmBounds.x);
    }

    for (Locator buttonLocator : actionButtonLocators.all()) {
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
  }

  public static void captureRegistration(Page browserPage) {
    Locator registrationPanelLocator = browserPage.locator(".public-panel").first();
    assertFieldArrangementAndAttachMeasurements(
        browserPage, registrationPanelLocator, "Registrierung", 640);

    browserPage.mouse().move(0, 0);
    byte[] registrationScreenshot =
        browserPage.screenshot(
            new Page.ScreenshotOptions()
                .setFullPage(true)
                .setAnimations(ScreenshotAnimations.DISABLED));
    attachScreenshot(
        registrationScreenshot, "Registrierung " + browserPage.viewportSize().width + " oben");
  }

  public static void captureUserDialog(Page browserPage, String dialogPurpose) {
    Locator userDialogLocator = browserPage.locator("vaadin-dialog.admin-user-dialog[opened]");
    assertFieldArrangementAndAttachMeasurements(browserPage, userDialogLocator, dialogPurpose, 768);

    String evidenceName = dialogPurpose + " " + browserPage.viewportSize().width;
    browserPage.mouse().move(0, 0);
    attachUserDialogScreenshot(browserPage, userDialogLocator, evidenceName + " oben");
    attachDialogScrollMeasurements(userDialogLocator, evidenceName + " Scrollmaße oben");

    Locator signInDetailsLocator = userDialogLocator.locator(".admin-user-sign-in");
    Locator lastFormFieldLocator =
        signInDetailsLocator.count() == 1
            ? signInDetailsLocator
            : userDialogLocator.locator(".cosmos-dialog-form vaadin-checkbox:visible").last();
    lastFormFieldLocator.scrollIntoViewIfNeeded();
    attachUserDialogScreenshot(browserPage, userDialogLocator, evidenceName + " unten");
    attachDialogScrollMeasurements(userDialogLocator, evidenceName + " Scrollmaße unten");
  }

  private static void assertFieldArrangementAndAttachMeasurements(
      Page browserPage, Locator formPanelLocator, String formPurpose, int twoColumnMinWidth) {
    Locator firstNameFieldLocator = formPanelLocator.locator("vaadin-text-field").first();
    Locator lastNameFieldLocator = formPanelLocator.locator("vaadin-text-field").nth(1);
    assertThat(firstNameFieldLocator).isVisible();
    browserPage.evaluate("() => document.fonts.ready");
    firstNameFieldLocator.scrollIntoViewIfNeeded();
    formPanelLocator.locator("h1, h2, h3").first().click();

    BoundingBox firstNameFieldBounds = firstNameFieldLocator.boundingBox();
    BoundingBox lastNameFieldBounds = lastNameFieldLocator.boundingBox();
    if (browserPage.viewportSize().width >= twoColumnMinWidth) {
      assertEquals(firstNameFieldBounds.y, lastNameFieldBounds.y, 0.1);
      assertTrue(lastNameFieldBounds.x > firstNameFieldBounds.x + firstNameFieldBounds.width);
    } else {
      assertEquals(firstNameFieldBounds.x, lastNameFieldBounds.x, 0.1);
      assertTrue(lastNameFieldBounds.y > firstNameFieldBounds.y + firstNameFieldBounds.height);
    }

    String fieldMeasurementsJson =
        (String)
            formPanelLocator.evaluate(
                """
                panel => {
                  const fields = Array.from(panel.querySelectorAll(`vaadin-text-field, vaadin-email-field,
                    vaadin-password-field, vaadin-combo-box, vaadin-date-picker,
                    vaadin-multi-select-combo-box, vaadin-checkbox`));
                  const origin = fields[0].getBoundingClientRect();
                  return JSON.stringify(fields.filter(field => field.getBoundingClientRect().height > 0)
                    .map(field => {
                      const rect = field.getBoundingClientRect();
                      const input = field.shadowRoot?.querySelector('[part="input-field"]');
                      const style = getComputedStyle(input ?? field);
                      return {tag: field.localName, label: field.label, x: rect.x - origin.x,
                        y: rect.y - origin.y, width: rect.width, height: rect.height,
                        font: style.font, color: style.color, border: style.border,
                        background: style.backgroundColor};
                    }));
                }
                """);
    String evidenceName = formPurpose + " " + browserPage.viewportSize().width;
    Allure.attachment(evidenceName + " Messwerte", "application/json", fieldMeasurementsJson);
  }

  private static void attachUserDialogScreenshot(
      Page browserPage, Locator userDialogLocator, String evidenceName) {
    BoundingBox headerBounds = userDialogLocator.locator(".admin-user-dialog-header").boundingBox();
    BoundingBox footerBounds = userDialogLocator.locator(".admin-user-editor-footer").boundingBox();
    Page.ScreenshotOptions screenshotOptions =
        new Page.ScreenshotOptions()
            .setAnimations(ScreenshotAnimations.DISABLED)
            .setClip(
                headerBounds.x - 1,
                headerBounds.y - 1,
                headerBounds.width + 2,
                footerBounds.y + footerBounds.height - headerBounds.y + 2);

    attachScreenshot(browserPage.screenshot(screenshotOptions), evidenceName);
  }

  private static void attachDialogScrollMeasurements(
      Locator userDialogLocator, String evidenceName) {
    String scrollMeasurementsJson =
        (String)
            userDialogLocator
                .locator(".cosmos-dialog-form")
                .evaluate(
                    """
                    form => {
                      const ancestors = [];
                      for (let node = form; node; node = node.assignedSlot ?? node.parentElement ?? node.getRootNode().host) {
                        const style = getComputedStyle(node);
                        const rect = node.getBoundingClientRect();
                        ancestors.push({tag: node.localName, part: node.getAttribute('part'),
                          height: rect.height, clientHeight: node.clientHeight, scrollHeight: node.scrollHeight,
                          scrollTop: node.scrollTop, overflow: style.overflowY});
                      }
                      return JSON.stringify(ancestors);
                    }
                    """);
    Allure.attachment(evidenceName, "application/json", scrollMeasurementsJson);
  }

  private static void attachScreenshot(byte[] screenshotBytes, String evidenceName) {
    Allure.attachment(
        evidenceName,
        "image/png",
        new ByteArrayInputStream(screenshotBytes),
        AttachmentOptions.empty());
  }
}
