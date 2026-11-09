package de.derpeterson.app.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import java.util.Date;
import java.util.function.IntSupplier;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.rememberme.CookieTheftException;
import org.springframework.security.web.authentication.rememberme.InvalidCookieException;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationException;

/** Verknüpft eine bestätigte Tokenserie mit ihrer HTTP-Sitzung. */
public class DeviceRememberMeServices extends PersistentTokenBasedRememberMeServices {
  public static final String SERIES = DeviceRememberMeServices.class.getName() + ".series";
  private final DeviceTokenRepository tokens;
  private final SessionAdmissionService admissions;
  private final IntSupplier configuredValidity;
  private final ScopedValue<Integer> requestValidity = ScopedValue.newInstance();

  public DeviceRememberMeServices(
      String key,
      UserDetailsService users,
      DeviceTokenRepository tokens,
      SessionAdmissionService admissions,
      IntSupplier configuredValidity) {
    super(key, users, tokens);
    this.tokens = tokens;
    this.admissions = admissions;
    this.configuredValidity = configuredValidity;
  }

  @Override
  public @Nullable Authentication autoLogin(
      HttpServletRequest request, HttpServletResponse response) {
    try {
      return super.autoLogin(request, response);
    } catch (CookieTheftException _) {
      // Spring hat bereits alle Token des Kontos widerrufen und das Cookie entfernt.
      // Die normale Zugriffskontrolle fordert eine neue Anmeldung statt einer HTTP-500-Seite.
      logger.warn("Saved sign-in token mismatch rejected; fresh sign-in required");
      return null;
    }
  }

  @Override
  protected void onLoginSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
    prepareVerifiedLogin(request, response, authentication.getName());
    admissions.withIdentity(
        authentication.getName(),
        () ->
            tokens.atomic(
                () -> {
                  PersistentRememberMeToken token =
                      new PersistentRememberMeToken(
                          authentication.getName(),
                          generateSeriesData(),
                          generateTokenData(),
                          new Date());
                  tokens.createNewToken(token);
                  tokens.describe(
                      token.getSeries(),
                      token.getUsername(),
                      DeviceDescription.from(request.getHeader("User-Agent")),
                      Instant.now());
                  admissions.bindSeries(request, token.getSeries());
                  setCookie(
                      new String[] {token.getSeries(), token.getTokenValue()},
                      getTokenValiditySeconds(),
                      request,
                      response);
                  return null;
                }));
  }

  @Override
  protected UserDetails processAutoLoginCookie(
      String[] cookie, HttpServletRequest request, HttpServletResponse response) {
    if (cookie.length != 2) {
      throw new InvalidCookieException("Invalid persistent login");
    }
    PersistentRememberMeToken token = tokens.getTokenForSeries(cookie[0]);
    if (token == null) {
      throw new RememberMeAuthenticationException("Saved login no longer valid");
    }
    return admissions.withIdentity(
        token.getUsername(),
        () -> {
          UserDetails user =
              ScopedValue.where(requestValidity, validitySeconds())
                  .call(() -> super.processAutoLoginCookie(cookie, request, response));
          request.setAttribute(SERIES, cookie[0]);
          admissions.bindSeries(request, cookie[0]);
          return user;
        });
  }

  /**
   * Nur nach bestätigter Benutzeranmeldung aufrufen; ersetzt die alte Anmeldung dieses Browsers.
   */
  public void prepareVerifiedLogin(
      HttpServletRequest request, HttpServletResponse response, String identity) {
    admissions.withIdentity(
        identity,
        () -> {
          String series = validatedCookieSeries(request, identity);
          if (series != null) {
            tokens.removeSeries(identity, series);
          }
          cancelCookie(request, response);
          admissions.bindSeries(request, null);
          return null;
        });
  }

  @Override
  public void logout(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
    if (authentication != null) {
      admissions.withIdentity(
          authentication.getName(),
          () -> {
            HttpSession session = request.getSession(false);
            String series = session == null ? null : (String) session.getAttribute(SERIES);
            if (series == null) {
              series = validatedCookieSeries(request, authentication.getName());
            }
            if (series != null) {
              tokens.removeSeries(authentication.getName(), series);
            }
            return null;
          });
    }
    cancelCookie(request, response);
  }

  public int validitySeconds() {
    int seconds = configuredValidity.getAsInt();
    if (seconds < 1) {
      throw new IllegalArgumentException("Saved sign-in validity must be positive");
    }
    return seconds;
  }

  @Override
  protected int getTokenValiditySeconds() {
    // Prüfung und Erneuerung desselben Cookies verwenden eine gemeinsame Gültigkeitsdauer.
    return requestValidity.isBound() ? requestValidity.get() : validitySeconds();
  }

  public boolean requested(HttpServletRequest request) {
    return rememberMeRequested(request, getParameter());
  }

  private String validatedCookieSeries(HttpServletRequest request, String identity) {
    String cookie = extractRememberMeCookie(request);
    if (cookie == null) {
      return null;
    }
    try {
      String[] parts = decodeCookie(cookie);
      if (parts.length != 2) {
        return null;
      }
      PersistentRememberMeToken token = tokens.getTokenForSeries(parts[0]);
      return token != null
              && identity.equals(token.getUsername())
              && token.getTokenValue().equals(parts[1])
          ? parts[0]
          : null;
    } catch (InvalidCookieException _) {
      return null;
    }
  }
}
