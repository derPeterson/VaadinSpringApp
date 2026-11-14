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

/**
 * Vergleicht die bestehende Profilanordnung mit dem nativen FormLayout ohne Eingaben zu verändern.
 */
public final class ProfileFormAppearanceChecks {
  private ProfileFormAppearanceChecks() {}

  public static void assertLayoutAndCaptureEvidence(Page browserPage, boolean includeScreenshot) {
    int viewportWidth = browserPage.viewportSize().width;
    Locator profilePanelLocator = browserPage.locator("#account-panel-profile");
    Locator profileFormLocator = profilePanelLocator.locator(".account-profile-form");
    assertThat(profileFormLocator).isVisible();
    browserPage.evaluate("() => document.fonts.ready");
    profilePanelLocator.locator(".cosmos-panel-title").click();

    assertProfileFieldArrangement(browserPage, profileFormLocator, viewportWidth);

    if (includeScreenshot) {
      Allure.attachment(
          "Profilformular " + viewportWidth,
          "image/png",
          new ByteArrayInputStream(
              profilePanelLocator.screenshot(
                  new Locator.ScreenshotOptions().setAnimations(ScreenshotAnimations.DISABLED))),
          AttachmentOptions.empty());
    }

    attachFormMeasurements(profileFormLocator, viewportWidth);
  }

  private static void assertProfileFieldArrangement(
      Page browserPage, Locator profileFormLocator, int viewportWidth) {
    BoundingBox firstNameFieldBounds = browserPage.locator("#account-first-name").boundingBox();
    BoundingBox lastNameFieldBounds = browserPage.locator("#account-last-name").boundingBox();
    BoundingBox emailFieldBounds = browserPage.locator("#account-email").boundingBox();
    BoundingBox profileFormBounds = profileFormLocator.boundingBox();

    if (viewportWidth >= 640) {
      assertEquals(firstNameFieldBounds.y, lastNameFieldBounds.y, 0.1);
      assertTrue(lastNameFieldBounds.x > firstNameFieldBounds.x + firstNameFieldBounds.width);
    } else {
      assertEquals(firstNameFieldBounds.x, lastNameFieldBounds.x, 0.1);
      assertTrue(lastNameFieldBounds.y > firstNameFieldBounds.y + firstNameFieldBounds.height);
    }
    assertEquals(profileFormBounds.width, emailFieldBounds.width, 0.1);
  }

  private static void attachFormMeasurements(Locator profileFormLocator, int viewportWidth) {
    String profileMeasurementsJson =
        (String)
            profileFormLocator.evaluate(
                """
                form => {
                  const panel = form.closest('#account-panel-profile');
                  const bounds = panel.getBoundingClientRect();
                  const fields = ['account-first-name', 'account-last-name', 'account-email',
                    'account-gender', 'account-birth-date', 'account-language'].map(id => {
                      const field = form.querySelector('#' + id);
                      const rect = field.getBoundingClientRect();
                      const input = field.shadowRoot?.querySelector('[part="input-field"]');
                      const style = input ? getComputedStyle(input) : getComputedStyle(field);
                      return {id, x: rect.x - bounds.x, y: rect.y - bounds.y,
                        width: rect.width, height: rect.height,
                        inputHeight: input?.getBoundingClientRect().height ?? null,
                        font: style.font, color: style.color, border: style.border,
                        background: style.backgroundColor};
                    });
                  return JSON.stringify({tag: form.localName, width: bounds.width,
                    height: bounds.height, fieldOrder: Array.from(form.querySelectorAll('[id]'))
                      .map(field => field.id).filter(id => fields.some(field => field.id === id)),
                    fields});
                }
                """);
    Allure.attachment(
        "Profilformular Messwerte " + viewportWidth, "application/json", profileMeasurementsJson);
  }
}
