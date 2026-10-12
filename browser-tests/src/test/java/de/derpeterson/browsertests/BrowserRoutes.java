package de.derpeterson.browsertests;

import java.util.Objects;
import java.util.regex.Pattern;

/** Browser paths derive from the application's shared route constants, never from security wildcards. */
public final class BrowserRoutes {
    private BrowserRoutes() { }

    public static String path(String route) {
        return "/" + route;
    }

    public static Pattern urlPattern(String route) {
        // Optionaler Query-Teil: ein wörtliches ? gefolgt von beliebigen Zeichen; die ganze Gruppe darf fehlen.
        return urlPattern(route, "(?:\\?.*)?");
    }

    public static Pattern urlPattern(String route, String suffixPattern) {
        String origin = Objects.requireNonNull(System.getProperty("browser.tests.baseUrl"), "Runner origin missing.");
        // Playwright forwards this pattern to JavaScript, which does not support Java's \Q...\E syntax.
        // Die Zeichenklasse enthält Regex-Sonderzeichen; jedes wird mit einem Backslash escaped ($0 = gesamter Treffer).
        String literal = (origin + path(route)).replaceAll("[\\\\.^$|?*+()\\[\\]{}]", "\\\\$0");
        // ^/$ verankern die gesamte URL; Origin und Route sind wörtlich, nur suffixPattern ist ein Regex.
        return Pattern.compile("^" + literal + suffixPattern + "$");
    }
}
