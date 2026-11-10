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
  private final DeviceTokenRepository deviceTokenRepository;
  private final SessionAdmissionService sessionAdmissionService;
  private final IntSupplier configuredValidity;
  private final ScopedValue<Integer> requestValidity = ScopedValue.newInstance();

  public DeviceRememberMeServices(
      String key,
      UserDetailsService users,
      DeviceTokenRepository deviceTokenRepository,
      SessionAdmissionService sessionAdmissionService,
      IntSupplier configuredValidity) {
    super(key, users, deviceTokenRepository);
    this.deviceTokenRepository = deviceTokenRepository;
    this.sessionAdmissionService = sessionAdmissionService;
    this.configuredValidity = configuredValidity;
  }

  @Override
  public @Nullable Authentication autoLogin(
      HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
    try {
      return super.autoLogin(httpRequest, httpResponse);
    } catch (CookieTheftException _) {
      // Spring hat bereits alle Token des Kontos widerrufen und das Cookie entfernt.
      // Die normale Zugriffskontrolle fordert eine neue Anmeldung statt einer HTTP-500-Seite.
      logger.warn("Saved sign-in token mismatch rejected; fresh sign-in required");
      return null;
    }
  }

  @Override
  protected void onLoginSuccess(
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse,
      Authentication authentication) {
    prepareVerifiedLogin(httpRequest, httpResponse, authentication.getName());
    sessionAdmissionService.withIdentity(
        authentication.getName(),
        () ->
            deviceTokenRepository.atomic(() -> {
              PersistentRememberMeToken token =
                  new PersistentRememberMeToken(
                      authentication.getName(),
                      generateSeriesData(),
                      generateTokenData(),
                      new Date());
              deviceTokenRepository.createNewToken(token);
              deviceTokenRepository.describe(
                  token.getSeries(),
                  token.getUsername(),
                  DeviceDescription.from(httpRequest.getHeader("User-Agent")),
                  Instant.now());
              sessionAdmissionService.bindSeries(httpRequest, token.getSeries());
              setCookie(
                  new String[] {token.getSeries(), token.getTokenValue()},
                  getTokenValiditySeconds(),
                  httpRequest,
                  httpResponse);
              return null;
            }));
  }

  @Override
  protected UserDetails processAutoLoginCookie(
      String[] cookie, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
    if (cookie.length != 2) {
      throw new InvalidCookieException("Invalid persistent login");
    }
    PersistentRememberMeToken token = deviceTokenRepository.getTokenForSeries(cookie[0]);
    if (token == null) {
      throw new RememberMeAuthenticationException("Saved login no longer valid");
    }
    return sessionAdmissionService.withIdentity(
        token.getUsername(),
        () -> {
          UserDetails user =
              ScopedValue.where(requestValidity, validitySeconds())
                  .call(() -> super.processAutoLoginCookie(cookie, httpRequest, httpResponse));
          httpRequest.setAttribute(SERIES, cookie[0]);
          sessionAdmissionService.bindSeries(httpRequest, cookie[0]);
          return user;
        });
  }

  /**
   * Nur nach bestätigter Benutzeranmeldung aufrufen; ersetzt die alte Anmeldung dieses Browsers.
   */
  public void prepareVerifiedLogin(
      HttpServletRequest httpRequest, HttpServletResponse httpResponse, String identity) {
    sessionAdmissionService.withIdentity(
        identity,
        () -> {
          String series = validatedCookieSeries(httpRequest, identity);
          if (series != null) {
            deviceTokenRepository.removeSeries(identity, series);
          }
          cancelCookie(httpRequest, httpResponse);
          sessionAdmissionService.bindSeries(httpRequest, null);
          return null;
        });
  }

  @Override
  public void logout(
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse,
      Authentication authentication) {
    if (authentication != null) {
      sessionAdmissionService.withIdentity(
          authentication.getName(),
          () -> {
            HttpSession session = httpRequest.getSession(false);
            String series = session == null ? null : (String) session.getAttribute(SERIES);
            if (series == null) {
              series = validatedCookieSeries(httpRequest, authentication.getName());
            }
            if (series != null) {
              deviceTokenRepository.removeSeries(authentication.getName(), series);
            }
            return null;
          });
    }
    cancelCookie(httpRequest, httpResponse);
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

  public boolean requested(HttpServletRequest httpRequest) {
    return rememberMeRequested(httpRequest, getParameter());
  }

  private String validatedCookieSeries(HttpServletRequest httpRequest, String identity) {
    String cookie = extractRememberMeCookie(httpRequest);
    if (cookie == null) {
      return null;
    }
    try {
      String[] parts = decodeCookie(cookie);
      if (parts.length != 2) {
        return null;
      }
      PersistentRememberMeToken token = deviceTokenRepository.getTokenForSeries(parts[0]);
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
