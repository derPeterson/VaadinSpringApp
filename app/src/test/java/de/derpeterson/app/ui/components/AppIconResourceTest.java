package de.derpeterson.app.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.flow.component.icon.SvgIcon;
import de.derpeterson.app.config.AdminConfigGroup;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

class AppIconResourceTest {
  @Test
  void everyApplicationIconHasALocalSvgAndDecorativeSemantics() throws Exception {
    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    documentBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

    for (AppIcon appIcon : AppIcon.values()) {
      SvgIcon svgIcon = appIcon.create();

      assertEquals(appIcon.getSource(), svgIcon.getSrc());
      assertEquals("true", svgIcon.getElement().getAttribute("aria-hidden"));
      String iconLibraryName = appIcon.getSource().split("/")[1];

      assertTrue(svgIcon.hasClassName("cosmos-" + iconLibraryName + "-icon"), appIcon.name());

      try (InputStream resourceInputStream = openClasspathResource(appIcon.getSource())) {
        Element svgElement =
            documentBuilderFactory
                .newDocumentBuilder()
                .parse(resourceInputStream)
                .getDocumentElement();

        assertEquals("svg", svgElement.getTagName(), appIcon.name());

        switch (iconLibraryName) {
          case "lucide", "tabler" -> {
            assertEquals("0 0 24 24", svgElement.getAttribute("viewBox"), appIcon.name());
            assertEquals("none", svgElement.getAttribute("fill"), appIcon.name());
            assertEquals("currentColor", svgElement.getAttribute("stroke"), appIcon.name());
            assertEquals("2", svgElement.getAttribute("stroke-width"), appIcon.name());
          }
          case "material-symbols" -> {
            assertEquals("0 -960 960 960", svgElement.getAttribute("viewBox"), appIcon.name());
            Element pathElement = (Element) svgElement.getElementsByTagName("path").item(0);

            assertNotNull(pathElement, appIcon.name());
            assertEquals("currentColor", pathElement.getAttribute("fill"), appIcon.name());
          }
          default -> throw new AssertionError("Unknown local icon library: " + iconLibraryName);
        }
      }
    }
  }

  @Test
  void licenseIsAvailableFromClasspath() throws Exception {
    try (InputStream resourceInputStream = openClasspathResource("icons/lucide/LICENSE")) {
      String licenseText = new String(resourceInputStream.readAllBytes(), StandardCharsets.UTF_8);

      assertTrue(licenseText.contains("ISC"));
      assertTrue(licenseText.contains("Lucide"));
    }

    try (InputStream resourceInputStream = openClasspathResource("icons/tabler/LICENSE")) {
      String licenseText = new String(resourceInputStream.readAllBytes(), StandardCharsets.UTF_8);

      assertTrue(licenseText.contains("MIT License"));
      assertTrue(licenseText.contains("Kuna"));
    }

    try (InputStream resourceInputStream =
        openClasspathResource("icons/material-symbols/LICENSE")) {
      String licenseText = new String(resourceInputStream.readAllBytes(), StandardCharsets.UTF_8);

      assertTrue(licenseText.contains("Apache License"));
    }
  }

  @Test
  void providerConfigurationUsesTheApprovedTablerIcon() {
    assertEquals(AppIcon.CLOUD_LOCK, ConfigGroupIcons.forGroup(AdminConfigGroup.EXTERNAL_LOGIN));
    assertEquals("icons/tabler/cloud-lock.svg", AppIcon.CLOUD_LOCK.getSource());
  }

  private InputStream openClasspathResource(String resourcePath) {
    InputStream resourceInputStream =
        getClass().getResourceAsStream("/META-INF/resources/" + resourcePath);

    assertNotNull(resourceInputStream, resourcePath);

    return resourceInputStream;
  }
}
