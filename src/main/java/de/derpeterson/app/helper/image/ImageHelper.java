package de.derpeterson.app.helper.image;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.codec.binary.Base64;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ImageHelper {

    /** Encodes a classpath image as Base64 without a data URI prefix. */
    public static String convertImageToBase64(String resourcePath) throws IOException {
        try (InputStream input = new ClassPathResource(resourcePath).getInputStream()) {
            return Base64.encodeBase64String(input.readAllBytes());
        }
    }
}
