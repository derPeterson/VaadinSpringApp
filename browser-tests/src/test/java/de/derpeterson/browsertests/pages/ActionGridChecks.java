package de.derpeterson.browsertests.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.BoundingBox;

/** Prüft sichtbare Textaktionen im responsiven Raster. */
public final class ActionGridChecks {
  private ActionGridChecks() {}

  public static void assertActions(Locator actionsLocator, int buttonCount, boolean stacked) {
    assertThat(actionsLocator).hasCSS("display", "grid");
    Locator buttonsLocator = actionsLocator.locator(":scope > vaadin-button:visible");
    assertThat(buttonsLocator).hasCount(buttonCount);

    double contentWidth =
        ((Number)
                actionsLocator.evaluate(
                    """
                    actions => {
                      const style = getComputedStyle(actions);
                      return actions.clientWidth - parseFloat(style.paddingLeft)
                        - parseFloat(style.paddingRight);
                    }
                    """))
            .doubleValue();
    BoundingBox firstButtonBounds = buttonsLocator.first().boundingBox();
    if (stacked || buttonCount == 1) {
      assertEquals(contentWidth, firstButtonBounds.width, 1);
    }

    BoundingBox previousButtonBounds = null;
    for (Locator buttonLocator : buttonsLocator.all()) {
      assertLabelFits(buttonLocator);
      BoundingBox buttonBounds = buttonLocator.boundingBox();
      assertEquals(firstButtonBounds.width, buttonBounds.width, 1);

      if (previousButtonBounds != null) {
        if (stacked) {
          assertEquals(previousButtonBounds.x, buttonBounds.x, 1);
          assertTrue(
              previousButtonBounds.y + previousButtonBounds.height <= buttonBounds.y
                  || buttonBounds.y + buttonBounds.height <= previousButtonBounds.y);
        } else {
          assertEquals(previousButtonBounds.y, buttonBounds.y, 1);
          assertTrue(previousButtonBounds.x + previousButtonBounds.width <= buttonBounds.x);
        }
      }

      previousButtonBounds = buttonBounds;
    }
  }

  public static void assertLabelFits(Locator buttonLocator) {
    Locator labelLocator = buttonLocator.locator("[part~='label']");
    assertThat(labelLocator).isVisible();
    assertTrue(
        (Boolean)
            labelLocator.evaluate(
                """
                label => label.scrollWidth <= label.clientWidth + 1
                  && label.scrollHeight <= label.clientHeight + 1
                """),
        () -> "Beschriftung wird abgeschnitten: " + buttonLocator.innerText());
  }
}
