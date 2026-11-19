package de.derpeterson.browsertests.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.BoundingBox;
import com.microsoft.playwright.options.ScreenshotAnimations;
import com.microsoft.playwright.options.ScreenshotScale;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;

/** Prüft den Übergang von zwei Reitern im Dialogkopf zum Inhalt. */
public final class DialogTabAssertions {
  private DialogTabAssertions() {}

  public static void assertDialogTabSurfaces(
      Page browserPage, Locator tabs, Locator selectedTab, Locator content) {
    assertThat(selectedTab).hasAttribute("selected", "");

    BoundingBox tabBounds = selectedTab.boundingBox();
    BoundingBox contentBounds = content.boundingBox();
    int tabCenterX = (int) Math.round(tabBounds.x + tabBounds.width / 2);
    int contentTopY = (int) Math.floor(contentBounds.y);

    BoundingBox inactiveTabBounds = tabs.locator("vaadin-tab:not([selected])").boundingBox();
    int inactiveTabCenterX = (int) Math.round(inactiveTabBounds.x + inactiveTabBounds.width / 2);
    int adjacentHeaderX =
        inactiveTabBounds.x < tabBounds.x
            ? (int) Math.floor(inactiveTabBounds.x) - 4
            : (int) Math.ceil(inactiveTabBounds.x + inactiveTabBounds.width) + 4;

    byte[] screenshot =
        browserPage.screenshot(
            new Page.ScreenshotOptions()
                .setAnimations(ScreenshotAnimations.DISABLED)
                .setScale(ScreenshotScale.CSS));
    BufferedImage dialogImage =
        assertDoesNotThrow(() -> ImageIO.read(new ByteArrayInputStream(screenshot)));
    int contentPixel = dialogImage.getRGB(tabCenterX, contentTopY + 3);

    for (int y = contentTopY - 3; y < contentTopY + 3; y++) {
      assertEquals(
          contentPixel,
          dialogImage.getRGB(tabCenterX, y),
          "Unter dem ausgewählten Reiter geht die Hintergrundfarbe bündig in den Inhalt über");
      assertEquals(
          dialogImage.getRGB(adjacentHeaderX, y),
          dialogImage.getRGB(inactiveTabCenterX, y),
          "Die Trennlinie unter dem inaktiven Reiter schließt seitlich bündig an");
    }

    int headerPixel = dialogImage.getRGB(tabCenterX, (int) Math.floor(tabBounds.y) - 3);
    for (Locator tab : tabs.locator("vaadin-tab").all()) {
      BoundingBox bounds = tab.boundingBox();
      int topY = (int) Math.ceil(bounds.y) + 1;
      int leftX = (int) Math.ceil(bounds.x) + 1;
      int rightX = (int) Math.floor(bounds.x + bounds.width) - 2;

      assertEquals(
          headerPixel, dialogImage.getRGB(leftX, topY), "Linke Rundung auf der Kopffläche");
      assertEquals(
          headerPixel, dialogImage.getRGB(rightX, topY), "Rechte Rundung auf der Kopffläche");
    }
  }
}
