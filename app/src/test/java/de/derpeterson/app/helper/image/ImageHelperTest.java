package de.derpeterson.app.helper.image;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.flow.server.PWA;
import de.derpeterson.app.Application;
import de.derpeterson.app.helper.email.EmailLogo;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ImageHelperTest {

  @Test
  void loadsRealMailLogoBytesFromClasspath() throws IOException {
    String resourcePath = "META-INF/resources/icons/service-logo.png";
    byte[] imageBytes = ImageHelper.readImageBytes(resourcePath);

    try (InputStream original = ImageHelperTest.class.getResourceAsStream("/" + resourcePath)) {
      assertNotNull(original, "The production mail logo must be available on the classpath");
      assertArrayEquals(original.readAllBytes(), imageBytes);
    }

    BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
    assertNotNull(image, "The classpath bytes must contain a readable image");
    assertEquals(512, image.getWidth());
    assertEquals(512, image.getHeight());
    assertTrue(image.getColorModel().hasAlpha());
    assertEquals(0, image.getRGB(0, 0) >>> 24, "The logo background must stay transparent");
    assertEquals("META-INF/resources/icons/service-logo.png", EmailLogo.RESOURCE_PATH);
    assertEquals(
        resourcePath,
        "META-INF/resources/" + Application.class.getAnnotation(PWA.class).iconPath());
  }
}
