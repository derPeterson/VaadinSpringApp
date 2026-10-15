package de.derpeterson.browsertests;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BrowserRoutesTest {
  @Test
  void localOriginIsLiteralAndCannotMatchASimilarHostOrAPrefixedUrl() {
    withOrigin(
        () -> {
          var pattern = BrowserRoutes.urlPattern("home");
          assertTrue(pattern.matcher("http://127.0.0.1:18082/home").matches());
          assertTrue(pattern.matcher("http://127.0.0.1:18082/home?tab=account").matches());
          assertFalse(pattern.matcher("http://127X0X0X1:18082/home").matches());
          assertFalse(
              pattern.matcher("http://example.test/?url=http://127.0.0.1:18082/home").matches());
          assertFalse(pattern.matcher("http://127.0.0.1:18080/home").matches());
        });
  }

  @Test
  void routeMetacharactersAreLiteralAndOtherPathsAreRejected() {
    withOrigin(
        () -> {
          var pattern = BrowserRoutes.urlPattern("screen.v2");
          assertTrue(pattern.matcher("http://127.0.0.1:18082/screen.v2").matches());
          assertFalse(pattern.matcher("http://127.0.0.1:18082/screenXv2").matches());
          assertFalse(pattern.matcher("http://127.0.0.1:18082/screen.v2-extra").matches());
        });
  }

  private static void withOrigin(Runnable test) {
    String previous = System.getProperty("browser.tests.baseUrl");
    try {
      System.setProperty("browser.tests.baseUrl", "http://127.0.0.1:18082");
      test.run();
    } finally {
      if (previous == null) System.clearProperty("browser.tests.baseUrl");
      else System.setProperty("browser.tests.baseUrl", previous);
    }
  }
}
