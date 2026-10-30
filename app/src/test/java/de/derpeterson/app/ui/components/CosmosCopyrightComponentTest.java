package de.derpeterson.app.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.service.ConfigService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class CosmosCopyrightComponentTest {
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2031-01-01T12:00:00Z"), ZoneOffset.UTC);

  private CosmosCopyrightComponent copyright(String text) {
    ConfigService config = mock(ConfigService.class);
    when(config.getString(ConfigEntry.SERVICE_NAME)).thenReturn("Example & Company");
    when(config.getString(ConfigEntry.COPYRIGHT_TEXT)).thenReturn(text);
    return new CosmosCopyrightComponent(config, CLOCK, null);
  }

  @Test
  void configuredCopyrightUsesTheInjectedYearAndServiceName() {
    assertEquals("© 2031 Example & Company", copyright("© {year} {serviceName}").getText());
  }

  @Test
  void configurationIsRenderedAsPlainTextWithoutHtmlElements() {
    CosmosCopyrightComponent footer = copyright("<b>{serviceName}</b> & <script>alert(1)</script>");
    assertEquals("<b>Example & Company</b> & <script>alert(1)</script>", footer.getText());
    assertEquals(1, footer.getElement().getChildCount());
    assertTrue(footer.getElement().getChild(0).isTextNode());
  }

  @Test
  void blankCopyrightHidesTheFooter() {
    assertFalse(copyright("  ").isVisible());
  }
}
