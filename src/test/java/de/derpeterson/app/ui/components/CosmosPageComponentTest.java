package de.derpeterson.app.ui.components;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.service.ConfigService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class CosmosPageComponentTest {
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2031-01-01T12:00:00Z"), ZoneOffset.UTC);

  private CosmosPageComponent page(String text) {
    ConfigService config = mock(ConfigService.class);
    when(config.getString(ConfigEntry.SERVICE_NAME)).thenReturn("Example & Company");
    when(config.getString(ConfigEntry.COPYRIGHT_TEXT)).thenReturn(text);
    return new CosmosPageComponent(config, false, CLOCK);
  }

  private Stream<Component> components(Component root) {
    return Stream.concat(Stream.of(root), root.getChildren().flatMap(this::components));
  }

  private Span footer(CosmosPageComponent root) {
    return (Span)
        components(root)
            .filter(c -> c.getElement().getClassList().contains("cosmos-footer"))
            .findFirst()
            .orElseThrow();
  }

  @Test
  void configuredCopyrightUsesTheInjectedYearAndServiceName() {
    assertEquals("© 2031 Example & Company", footer(page("© {year} {serviceName}")).getText());
  }

  @Test
  void configurationIsRenderedAsPlainTextWithoutHtmlElements() {
    var page = page("<b>{serviceName}</b> & <script>alert(1)</script>");
    assertEquals("<b>Example & Company</b> & <script>alert(1)</script>", footer(page).getText());
    assertEquals(1, footer(page).getElement().getChildCount());
    assertTrue(footer(page).getElement().getChild(0).isTextNode());
  }

  @Test
  void blankCopyrightHidesTheFooter() {
    assertFalse(footer(page("  ")).isVisible());
  }

  @Test
  void notificationScopeFollowsTheActuallyAttachedPage() {
    UI ui = new UI();
    assertFalse(CosmosPageComponent.isDisplayed(null));
    assertFalse(CosmosPageComponent.isDisplayed(ui));
    var page = page("Copyright");
    var wrapper = new Div(page);
    ui.add(wrapper);
    assertTrue(CosmosPageComponent.isDisplayed(ui));
    wrapper.remove(page);
    assertFalse(CosmosPageComponent.isDisplayed(ui));
  }
}
