package de.derpeterson.browsertests.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.BoundingBox;
import com.microsoft.playwright.options.ViewportSize;
import java.util.List;

/** Prüft Maße und Beschriftungen responsiver Aktionen. */
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

  public static void assertAtMobileWidths(Page page, Runnable layoutChecks) {
    ViewportSize originalViewport = page.viewportSize();
    layoutChecks.run();
    if (originalViewport.width > 320) {
      page.setViewportSize(320, originalViewport.height);
      try {
        layoutChecks.run();
      } finally {
        page.setViewportSize(originalViewport.width, originalViewport.height);
      }
    }
  }

  public static void assertButtonsFillColumn(Locator columnLocator, Locator buttonsLocator) {
    assertThat(columnLocator).isVisible();
    assertThat(buttonsLocator.first()).isVisible();

    List<?> columnEdges =
        (List<?>)
            columnLocator.evaluate(
                """
                column => {
                  const bounds = column.getBoundingClientRect();
                  const style = getComputedStyle(column);
                  return [
                    bounds.left + parseFloat(style.paddingLeft) + parseFloat(style.borderLeftWidth),
                    bounds.right - parseFloat(style.paddingRight) - parseFloat(style.borderRightWidth)
                  ];
                }
                """);
    double columnLeft = ((Number) columnEdges.getFirst()).doubleValue();
    double columnRight = ((Number) columnEdges.getLast()).doubleValue();

    for (Locator buttonLocator : buttonsLocator.all()) {
      BoundingBox buttonBounds = buttonLocator.boundingBox();
      assertEquals(columnLeft, buttonBounds.x, 1, "Linker Rand der Aktion");
      assertEquals(columnRight, buttonBounds.x + buttonBounds.width, 1, "Rechter Rand der Aktion");
      assertLabelFits(buttonLocator);
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
