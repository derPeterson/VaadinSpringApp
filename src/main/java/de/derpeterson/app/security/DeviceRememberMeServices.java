package de.derpeterson.app.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import java.util.Date;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.rememberme.InvalidCookieException;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationException;

/** Verknüpft eine bestätigte Tokenserie mit ihrer HTTP-Sitzung. */
public class DeviceRememberMeServices extends PersistentTokenBasedRememberMeServices {
  public static final String SERIES = DeviceRememberMeServices.class.getName() + ".series";
  private final DeviceTokenRepository tokens;
  private final SessionAdmissionService admissions;

  public DeviceRememberMeServices(
      String key,
      UserDetailsService users,
      DeviceTokenRepository tokens,
      SessionAdmissionService admissions) {
    super(key, users, tokens);
    this.tokens = tokens;
    this.admissions = admissions;
  }

  @Override
  protected void onLoginSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
    preparePasswordLogin(request, response, authentication.getName());
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
          UserDetails user = super.processAutoLoginCookie(cookie, request, response);
          request.setAttribute(SERIES, cookie[0]);
          admissions.bindSeries(request, cookie[0]);
          return user;
        });
  }

  /**
   * Nur nach erfolgreicher Passwortprüfung aufrufen; ersetzt die alte Anmeldung dieses Browsers.
   */
  public void preparePasswordLogin(
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

  /** Die beim Start geladene Dauer gilt auch für Anzeige und Bereinigung gespeicherter Zugänge. */
  public int validitySeconds() {
    return getTokenValiditySeconds();
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
    } catch (InvalidCookieException invalid) {
      return null;
    }
  }
}
