package de.derpeterson.browsertests.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import java.io.ByteArrayInputStream;

/** Vergleichsbelege für den bisherigen und den nativen Bildkern, einschließlich Statushülle. */
public final class AvatarAppearanceChecks {
  private AvatarAppearanceChecks() {}

  public static void captureAvatar(Locator avatarLocator, String evidenceName) {
    assertThat(avatarLocator).isVisible();
    avatarLocator.scrollIntoViewIfNeeded();
    Allure.attachment(
        evidenceName,
        "image/png",
        new ByteArrayInputStream(avatarLocator.screenshot()),
        AttachmentOptions.empty());

    String avatarMeasurementsJson =
        (String)
            avatarLocator.evaluate(
                """
                avatar => {
                  const circle = avatar.querySelector('.cosmos-avatar-circle') || avatar;
                  const initials = circle.querySelector('.cosmos-avatar-initials')
                      || circle.shadowRoot?.querySelector('[part=abbr]');
                  const visibleInitials = initials?.getClientRects().length ? initials : null;
                  const circleStyle = getComputedStyle(circle);
                  const bounds = avatar.getBoundingClientRect();
                  const textStyle = visibleInitials ? getComputedStyle(visibleInitials) : null;
                  const status = avatar.querySelector('.cosmos-avatar-status > div');
                  const statusBounds = status?.getBoundingClientRect();
                  const effectiveFontSize = !textStyle ? null : circle.shadowRoot
                      ? parseFloat(textStyle.fontSize) * (circle.clientWidth / 100)
                      : parseFloat(textStyle.fontSize);
                  return JSON.stringify({tag: circle.localName, width: bounds.width,
                    height: bounds.height, border: circleStyle.border,
                    background: circleStyle.backgroundColor, shadow: circleStyle.boxShadow,
                    radius: circleStyle.borderRadius, color: textStyle?.color ?? null,
                    weight: textStyle?.fontWeight ?? null, effectiveFontSize,
                    initials: visibleInitials?.textContent.trim() ?? null,
                    status: statusBounds ? {x: statusBounds.x - bounds.x,
                      y: statusBounds.y - bounds.y, width: statusBounds.width,
                      height: statusBounds.height, color: getComputedStyle(status).color,
                      background: getComputedStyle(status).backgroundColor} : null});
                }
                """);
    Allure.attachment(evidenceName + " Messwerte", "application/json", avatarMeasurementsJson);
  }
}
