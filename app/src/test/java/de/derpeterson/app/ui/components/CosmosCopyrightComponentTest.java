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
  private static final Clock COPYRIGHT_CLOCK =
      Clock.fixed(Instant.parse("2031-01-01T12:00:00Z"), ZoneOffset.UTC);

  private CosmosCopyrightComponent createCopyrightComponent(String copyrightTemplateText) {
    ConfigService configService = mock(ConfigService.class);
    when(configService.getString(ConfigEntry.SERVICE_NAME)).thenReturn("Example & Company");
    when(configService.getString(ConfigEntry.COPYRIGHT_TEXT)).thenReturn(copyrightTemplateText);

    return new CosmosCopyrightComponent(configService, COPYRIGHT_CLOCK, null);
  }

  @Test
  void configuredCopyrightUsesTheInjectedYearAndServiceName() {
    assertEquals(
        "© 2031 Example & Company", createCopyrightComponent("© {year} {serviceName}").getText());
  }

  @Test
  void configurationIsRenderedAsPlainTextWithoutHtmlElements() {
    CosmosCopyrightComponent copyrightComponent =
        createCopyrightComponent("<b>{serviceName}</b> & <script>alert(1)</script>");

    assertEquals(
        "<b>Example & Company</b> & <script>alert(1)</script>", copyrightComponent.getText());
    assertEquals(1, copyrightComponent.getElement().getChildCount());
    assertTrue(copyrightComponent.getElement().getChild(0).isTextNode());
  }

  @Test
  void blankCopyrightHidesTheFooter() {
    assertFalse(createCopyrightComponent("  ").isVisible());
  }
}
