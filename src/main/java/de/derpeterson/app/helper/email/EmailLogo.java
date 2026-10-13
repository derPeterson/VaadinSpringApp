package de.derpeterson.app.helper.email;

import de.derpeterson.app.helper.image.ImageHelper;

import java.io.IOException;

/** Gemeinsame Zuordnung zwischen CID im HTML und der eingebetteten PNG-Ressource. */
public final class EmailLogo {

    public static final String CONTENT_ID = "service-logo@vaadinspringapp";
    public static final String RESOURCE_PATH = "META-INF/resources/custom-theme/service_logo.png";

    private EmailLogo() {
    }

    public static byte[] readBytes() throws IOException {
        return ImageHelper.readImageBytes(RESOURCE_PATH);
    }

    public static String validatedContentId() throws IOException {
        // Wie bisher muss eine unlesbare Logo-Ressource die Tokenrotation vor der Queue zurückrollen.
        readBytes();
        return CONTENT_ID;
    }
}
