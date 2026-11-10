package de.derpeterson.app.i18n;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.util.Locale;

public final class PublicLanguagePreference {
  private static final String COOKIE_NAME = "public-language";
  private static final int MAX_AGE_SECONDS = Math.toIntExact(Duration.ofDays(365).toSeconds());

  private PublicLanguagePreference() {}

  public static void restore(VaadinRequest vaadinRequest, VaadinSession vaadinSession) {
    Cookie[] requestCookies = vaadinRequest.getCookies();
    if (requestCookies == null) {
      return;
    }

    for (Cookie languageCookie : requestCookies) {
      if (COOKIE_NAME.equals(languageCookie.getName())) {
        String languageTag = languageCookie.getValue();
        if ("de".equals(languageTag)) {
          vaadinSession.setLocale(Locale.GERMAN);
        } else if ("en".equals(languageTag)) {
          vaadinSession.setLocale(Locale.ENGLISH);
        }
        return;
      }
    }
  }

  public static void save(Locale selectedLocale) {
    VaadinRequest vaadinRequest = VaadinRequest.getCurrent();
    VaadinResponse vaadinResponse = VaadinResponse.getCurrent();

    if (vaadinRequest == null || vaadinResponse == null) {
      return;
    }

    Cookie languageCookie = new Cookie(COOKIE_NAME, selectedLocale.getLanguage());
    String contextPath = vaadinRequest.getContextPath();
    languageCookie.setPath(contextPath.isEmpty() ? "/" : contextPath);
    languageCookie.setHttpOnly(true);
    languageCookie.setSecure(vaadinRequest.isSecure());
    languageCookie.setAttribute("SameSite", "Lax");
    languageCookie.setMaxAge(MAX_AGE_SECONDS);

    vaadinResponse.addCookie(languageCookie);
  }
}
