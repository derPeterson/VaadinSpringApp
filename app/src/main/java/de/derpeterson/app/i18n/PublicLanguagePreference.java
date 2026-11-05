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

  public static void restore(VaadinRequest request, VaadinSession session) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return;
    }
    for (Cookie cookie : cookies) {
      if (COOKIE_NAME.equals(cookie.getName())) {
        String language = cookie.getValue();
        if ("de".equals(language)) {
          session.setLocale(Locale.GERMAN);
        } else if ("en".equals(language)) {
          session.setLocale(Locale.ENGLISH);
        }
        return;
      }
    }
  }

  public static void save(Locale locale) {
    VaadinRequest request = VaadinRequest.getCurrent();
    VaadinResponse response = VaadinResponse.getCurrent();
    if (request == null || response == null) {
      return;
    }
    Cookie cookie = new Cookie(COOKIE_NAME, locale.getLanguage());
    String contextPath = request.getContextPath();
    cookie.setPath(contextPath.isEmpty() ? "/" : contextPath);
    cookie.setHttpOnly(true);
    cookie.setSecure(request.isSecure());
    cookie.setAttribute("SameSite", "Lax");
    cookie.setMaxAge(MAX_AGE_SECONDS);
    response.addCookie(cookie);
  }
}
