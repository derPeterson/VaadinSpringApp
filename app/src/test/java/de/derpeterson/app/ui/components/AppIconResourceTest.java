package de.derpeterson.app.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.flow.component.icon.SvgIcon;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

class AppIconResourceTest {
  @Test
  void everyApplicationIconHasALocalLucideContourAndDecorativeSemantics() throws Exception {
    DocumentBuilderFactory parser = DocumentBuilderFactory.newInstance();
    parser.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    for (AppIcon type : AppIcon.values()) {
      SvgIcon icon = type.create();
      assertEquals(type.getSource(), icon.getSrc());
      assertEquals("true", icon.getElement().getAttribute("aria-hidden"));
      assertTrue(icon.hasClassName("cosmos-lucide-icon"));
      try (InputStream stream = resource(type.getSource())) {
        Element svg = parser.newDocumentBuilder().parse(stream).getDocumentElement();
        assertEquals("svg", svg.getTagName(), type.name());
        assertEquals("0 0 24 24", svg.getAttribute("viewBox"), type.name());
        assertEquals("none", svg.getAttribute("fill"), type.name());
        assertEquals("currentColor", svg.getAttribute("stroke"), type.name());
        assertEquals("2", svg.getAttribute("stroke-width"), type.name());
      }
    }
  }

  @Test
  void licenseIsAvailableFromClasspath() throws Exception {
    try (InputStream stream = resource("icons/lucide/LICENSE")) {
      String license = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
      assertTrue(license.contains("ISC"));
      assertTrue(license.contains("Lucide"));
    }
  }

  private InputStream resource(String path) {
    InputStream stream = getClass().getResourceAsStream("/META-INF/resources/" + path);
    assertNotNull(stream, path);
    return stream;
  }
}
