package de.derpeterson.browsertests.pages;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.BoundingBox;

/** Wartet nach echten UI-Aktionen auf die Vaadin-Serverantwort vor Navigation/Gegenaktionen. */
public final class UiActions {
  private static final ObjectMapper JSON = new ObjectMapper();

  private UiActions() {}

  public static void complete(Page page, Runnable action) {
    page.waitForResponse(UiActions::isUiResponse, action).finished();
  }

  public static void completeValueChange(Page page, Runnable action) {
    // Dropdown-Öffnen und Auswahl können getrennte Antworten erzeugen. Erst die
    // Antwort auf die tatsächlich übermittelte Wertänderung bestätigt deren Verarbeitung.
    page.waitForResponse(response -> isUiResponse(response) && carriesValueChange(response), action)
        .finished();
  }

  /** Prüft den echten CSS-Ziehgriff, ohne Höhen oder Feldwerte per JavaScript zu setzen. */
  public static void resizeTextArea(Page page, Locator field) {
    com.microsoft.playwright.options.ViewportSize viewport = page.viewportSize();
    // Für den Ziehweg mehr Höhe bereitstellen; die tatsächlich geprüfte Breite bleibt gleich.
    page.setViewportSize(viewport.width, 1800);
    try {
      field.scrollIntoViewIfNeeded();
      BoundingBox original = field.boundingBox();
      dragCorner(page, original, 260);
      org.junit.jupiter.api.Assertions.assertTrue(
          field.boundingBox().height > original.height + 220);
      org.junit.jupiter.api.Assertions.assertEquals(original.width, field.boundingBox().width, 1);
      BoundingBox input = field.locator("[part~='input-field']").boundingBox();
      org.junit.jupiter.api.Assertions.assertEquals(
          field.boundingBox().y + field.boundingBox().height,
          input.y + input.height,
          3,
          "Der Griff muss am unteren Eingaberand bleiben.");
      double grown = field.boundingBox().height;
      field.locator("textarea").fill("Resize regression");
      org.junit.jupiter.api.Assertions.assertEquals(grown, field.boundingBox().height, 1);
      dragCorner(page, field.boundingBox(), -200);
      org.junit.jupiter.api.Assertions.assertTrue(field.boundingBox().height < grown - 160);
      field.locator("textarea").clear();
    } finally {
      page.setViewportSize(viewport.width, viewport.height);
    }
  }

  private static void dragCorner(Page page, BoundingBox bounds, double delta) {
    page.mouse().move(bounds.x + bounds.width - 3, bounds.y + bounds.height - 3);
    page.mouse().down();
    page.mouse()
        .move(
            bounds.x + bounds.width - 3,
            bounds.y + bounds.height - 3 + delta,
            new com.microsoft.playwright.Mouse.MoveOptions().setSteps(12));
    page.mouse().up();
  }

  private static boolean isUiResponse(Response response) {
    return response.request().method().equals("POST")
        && response.url().startsWith(System.getProperty("browser.tests.baseUrl"))
        && response.url().contains("v-r=uidl");
  }

  private static boolean carriesValueChange(Response response) {
    String body = response.request().postData();
    if (body == null) {
      return false;
    }
    try {
      for (JsonNode change : JSON.readTree(body).path("rpc")) {
        String property = change.path("property").asText();
        if (change.path("type").asText().equals("mSync")
            && (property.equals("value") || property.equals("selectedItem"))) {
          return true;
        }
      }
    } catch (JsonProcessingException _) {
      // Andere POST-Inhalte sind keine bestätigte Feldänderung.
    }
    return false;
  }
}
