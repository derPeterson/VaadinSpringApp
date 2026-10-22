package de.derpeterson.app.helper.image;

import java.io.IOException;
import java.io.InputStream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.core.io.ClassPathResource;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ImageHelper {

  /** Liest eine Bildressource auch aus einem JAR und schließt den Classpath-Stream. */
  public static byte[] readImageBytes(String resourcePath) throws IOException {
    try (InputStream input = new ClassPathResource(resourcePath).getInputStream()) {
      return input.readAllBytes();
    }
  }
}
