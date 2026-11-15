package de.derpeterson.browsertests.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.ScreenshotAnimations;
import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import java.io.ByteArrayInputStream;
import java.util.regex.Pattern;

/** Originalaufnahmen und tatsächliche Gestaltung der repräsentativen Meldungsvarianten. */
public final class NotificationAppearanceChecks {
  private NotificationAppearanceChecks() {}

  public static void capture(Page page, String scenario, String type, int duration) {
    Locator card = page.locator("vaadin-notification-card:not([closing])");
    assertThat(card).hasCount(1);
    // Der Schweregrad ist ein vollständiger Bestandteil der gemeinsamen Theme-Liste.
    assertThat(card)
        .hasAttribute("theme", Pattern.compile(".*\\bcosmos-reference-" + type + "\\b.*"));
    assertTrue(
        (boolean)
            page.locator("vaadin-notification")
                .evaluateAll(
                    """
                    (notifications, duration) => {
                      const opened = notifications.filter(notification => notification.opened);
                      return opened.length === 1 && opened[0].duration === duration
                        && opened[0].position === 'top-center';
                    }
                    """,
                    duration));
    page.mouse().move(0, 0);
    String name = "Notification " + scenario + " " + page.viewportSize().width;
    Allure.attachment(
        name + " Messwerte",
        "application/json",
        (String)
            card.evaluate(
                """
                card => JSON.stringify([card, ...card.querySelectorAll(
                  '.cosmos-toast-title,.cosmos-notification-message,.text-primary,.cosmos-notification-details > li')]
                  .map(element => {
                    const bounds = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return {width:bounds.width, height:bounds.height, font:style.font,
                      color:style.color, background:style.backgroundColor, padding:style.padding,
                      margin:style.margin, border:style.border};
                  }))
                """));
    Allure.attachment(
        name,
        "image/png",
        new ByteArrayInputStream(
            card.screenshot(
                new Locator.ScreenshotOptions().setAnimations(ScreenshotAnimations.DISABLED))),
        AttachmentOptions.empty());
    Allure.attachment(
        name + " Seite",
        "image/png",
        new ByteArrayInputStream(
            page.screenshot(
                new Page.ScreenshotOptions().setAnimations(ScreenshotAnimations.DISABLED))),
        AttachmentOptions.empty());
  }

  public static void close(Page page) {
    page.locator("vaadin-notification-card:not([closing])").getByRole(AriaRole.BUTTON).click();
    assertThat(page.locator("vaadin-notification-card")).hasCount(0);
  }
}
