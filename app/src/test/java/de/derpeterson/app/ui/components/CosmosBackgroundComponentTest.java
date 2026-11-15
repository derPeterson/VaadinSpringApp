package de.derpeterson.app.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import de.derpeterson.app.Application;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CosmosBackgroundComponentTest {
  @Test
  void applicationLoadsExactlyTheSelectedBackgroundResource() {
    List<String> backgroundStylesheets =
        Arrays.stream(Application.class.getAnnotationsByType(StyleSheet.class))
            .map(StyleSheet::value)
            .filter(stylesheetPath -> stylesheetPath.startsWith("custom-theme/cosmos-background-"))
            .toList();

    assertEquals(List.of(CosmosBackgroundComponent.STYLESHEET), backgroundStylesheets);
  }

  @ParameterizedTest
  @ValueSource(strings = {"base", "advanced"})
  void bothSwitchableBackgroundsAreAvailableFromTheClasspath(String variantName) throws Exception {
    try (InputStream stylesheetStream =
        getClass()
            .getResourceAsStream(
                "/META-INF/resources/custom-theme/cosmos-background-" + variantName + ".css")) {
      assertNotNull(stylesheetStream, variantName);
      String stylesheetContent =
          new String(stylesheetStream.readAllBytes(), StandardCharsets.UTF_8);

      assertTrue(stylesheetContent.contains("pointer-events: none"), variantName);
      assertTrue(stylesheetContent.contains("prefers-reduced-motion: reduce"), variantName);
    }
  }

  @Test
  void sharedFrameKeepsDecorativeBackgroundOutsideItsInteractiveContent() {
    Div headerDiv = new Div("Servicekopf");
    Div contentDiv = new Div("Seiteninhalt");
    Div footerDiv = new Div("Footer");
    PageFrameComponent pageFrame = new PageFrameComponent(headerDiv, contentDiv, footerDiv);
    CosmosBackgroundComponent backgroundComponent =
        pageFrame
            .getChildren()
            .filter(CosmosBackgroundComponent.class::isInstance)
            .map(CosmosBackgroundComponent.class::cast)
            .findFirst()
            .orElseThrow();

    assertEquals("true", backgroundComponent.getElement().getAttribute("aria-hidden"));
    assertEquals(pageFrame, backgroundComponent.getParent().orElseThrow());
    assertTrue(headerDiv.getParent().orElseThrow().hasClassName("cosmos-frame-header"));
    assertTrue(contentDiv.getParent().orElseThrow().hasClassName("cosmos-app-main"));
    assertEquals(contentDiv.getParent(), footerDiv.getParent());
    assertTrue(backgroundComponent.getChildren().allMatch(Div.class::isInstance));
  }
}
