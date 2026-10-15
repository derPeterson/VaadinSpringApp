package de.derpeterson.app.helper.image;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ImageHelperTest {

  @Test
  void loadsRealMailLogoFromClasspathAsBase64() throws IOException {
    String resourcePath = "META-INF/resources/custom-theme/service_logo.png";
    byte[] decoded = Base64.getDecoder().decode(ImageHelper.convertImageToBase64(resourcePath));

    try (InputStream original = ImageHelperTest.class.getResourceAsStream("/" + resourcePath)) {
      assertNotNull(original, "The production mail logo must be available on the classpath");
      assertArrayEquals(original.readAllBytes(), decoded);
    }

    var image = ImageIO.read(new ByteArrayInputStream(decoded));
    assertNotNull(image, "The Base64 payload must contain a readable image");
    assertTrue(image.getWidth() > 0 && image.getHeight() > 0);
  }
}
