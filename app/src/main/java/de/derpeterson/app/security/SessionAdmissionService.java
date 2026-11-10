package de.derpeterson.app.security;

import de.derpeterson.app.model.EmailIdentity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.security.session.DurableSessionRegistry;
import de.derpeterson.app.security.session.DurableSessionRepository;
import de.derpeterson.app.service.ConfigService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionBindingEvent;
import jakarta.servlet.http.HttpSessionBindingListener;
import java.io.Serializable;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.util.WebUtils;

/** One-instance admission. Fixed stripes bound lock memory independently of login identities. */
@Slf4j
public class SessionAdmissionService implements SessionAuthenticationStrategy {
  public static final String FAILURE_REASON = SessionAdmissionService.class.getName() + ".failure";
  private static final String REGISTRATION =
      SessionAdmissionService.class.getName() + ".registration";
  private static final String NEW_SESSION = SessionAdmissionService.class.getName() + ".newSession";
  private final ConfigService configService;
  private final SessionRegistry sessionRegistry;

  public record Active(
      String sessionId,
      String handle,
      String identity,
      DeviceDescription device,
      Instant created,
      String series,
      SignInMethod signInMethod)
      implements Serializable {
    public Active(
        String sessionId,
        String handle,
        String identity,
        DeviceDescription device,
        Instant created,
        String series) {
      this(sessionId, handle, identity, device, created, series, null);
    }
  }

  private final Map<String, Active> activeSessionsBySessionId = new ConcurrentHashMap<>();
  private DeviceTokenRepository deviceTokenRepository;
  private final Object[] identityLocks = new Object[256];
  private final CsrfAuthenticationStrategy csrfAuthentication = csrfAuthenticationStrategy();
  private final ChangeSessionIdAuthenticationStrategy changeSessionIdStrategy =
      new ChangeSessionIdAuthenticationStrategy();

  public SessionAdmissionService(ConfigService configService, SessionRegistry sessionRegistry) {
    this.configService = configService;
    this.sessionRegistry = sessionRegistry;
    Arrays.setAll(identityLocks, index -> new Object());
  }

  private static CsrfAuthenticationStrategy csrfAuthenticationStrategy() {
    // Stateless repository instances share Spring's standard key in the same HTTP session.
    CsrfAuthenticationStrategy strategy =
        new CsrfAuthenticationStrategy(new HttpSessionCsrfTokenRepository());
    strategy.setRequestHandler(new XorCsrfTokenRequestAttributeHandler());
    return strategy;
  }

  public void setDeviceTokens(DeviceTokenRepository deviceTokenRepository) {
    this.deviceTokenRepository = deviceTokenRepository;
  }

  public <T> T withIdentity(String identity, Supplier<T> action) {
    String key = EmailIdentity.canonicalize(identity);
    if (key == null || key.isBlank()) {
      throw new AuthenticationServiceException("Identity required");
    }
    synchronized (identityLocks[(key.hashCode() & Integer.MAX_VALUE) % identityLocks.length]) {
      return action.get();
    }
  }

  public List<Active> active(String identity) {
    return withIdentity(
        identity,
        () -> {
          if (sessionRegistry instanceof DurableSessionRegistry<?> persistent) {
            return sessionRegistry.getAllSessions(identity, false).stream()
                .map(info -> persistent.metadata(info.getSessionId()))
                .filter(java.util.Objects::nonNull)
                .toList();
          }
          activeSessionsBySessionId
              .entrySet()
              .removeIf(entry ->
                  identity.equals(entry.getValue().identity())
                      && sessionRegistry.getSessionInformation(entry.getKey()) == null);
          return sessionRegistry.getAllSessions(identity, false).stream()
              .map(info ->
                  activeSessionsBySessionId.computeIfAbsent(
                      info.getSessionId(),
                      id ->
                          new Active(
                              id,
                              UUID.randomUUID().toString(),
                              identity,
                              DeviceDescription.from(null),
                              info.getLastRequest().toInstant(),
                              null)))
              .toList();
        });
  }

  /** Registry und Metadaten beim ID-Wechsel unter derselben Kontosperre veröffentlichen. */
  public void sessionIdChanged(String previous, String current, Runnable publishRegistryEvent) {
    Active old = activeSessionsBySessionId.get(previous);
    if (old == null) {
      publishRegistryEvent.run();
      return;
    }
    withIdentity(
        old.identity(),
        () -> {
          publishRegistryEvent.run();
          sessionIdChanged(previous, current);
          return null;
        });
  }

  public void sessionIdChanged(String previous, String current) {
    Active old = activeSessionsBySessionId.remove(previous);
    if (old != null) {
      activeSessionsBySessionId.put(
          current,
          new Active(
              current,
              old.handle(),
              old.identity(),
              old.device(),
              old.created(),
              old.series(),
              old.signInMethod()));
    }
  }

  public Instant lastActivity(String sessionId) {
    SessionInformation info = sessionRegistry.getSessionInformation(sessionId);
    return info == null ? Instant.now() : info.getLastRequest().toInstant();
  }

  public void expire(String identity, String sessionId) {
    SessionInformation info = sessionRegistry.getSessionInformation(sessionId);
    if (info != null && identity.equals(info.getPrincipal())) {
      info.expireNow();
    }
  }

  public void touch(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session == null) {
      return;
    }
    SessionInformation info = sessionRegistry.getSessionInformation(session.getId());
    if (info != null && !info.isExpired()) {
      sessionRegistry.refreshLastRequest(session.getId());
    }
  }

  public void bindSeries(HttpServletRequest request, String series) {
    request.setAttribute(DeviceRememberMeServices.SERIES, series);
    HttpSession session = request.getSession(false);
    if (session == null) {
      return;
    }
    session.setAttribute(DeviceRememberMeServices.SERIES, series);
    if (sessionRegistry instanceof DurableSessionRegistry<?> persistent) {
      Active previous = persistent.metadata(session.getId());
      if (previous != null) {
        session.setAttribute(
            DurableSessionRepository.ACTIVE,
            new Active(
                session.getId(),
                previous.handle(),
                previous.identity(),
                previous.device(),
                previous.created(),
                series));
      }
      return;
    }
    activeSessionsBySessionId.computeIfPresent(
        session.getId(),
        (id, old) ->
            new Active(
                id,
                old.handle(),
                old.identity(),
                old.device(),
                old.created(),
                series,
                old.signInMethod()));
  }

  public boolean isExpired(HttpServletRequest request) {
    HttpSession session = request == null ? null : request.getSession(false);
    if (session == null) {
      return false;
    }
    SessionInformation information = sessionRegistry.getSessionInformation(session.getId());
    if (information != null) {
      return information.isExpired();
    }
    // Ein alter Socket darf einen entfernten dauerhaften Bestand nicht neu zulassen.
    return sessionRegistry instanceof DurableSessionRegistry<?>
        && session.getAttribute(DurableSessionRepository.ACTIVE) != null;
  }

  public boolean isSessionExpired(String sessionId) {
    SessionInformation information = sessionRegistry.getSessionInformation(sessionId);
    return information == null
        ? sessionRegistry instanceof DurableSessionRegistry<?>
        : information.isExpired();
  }

  @Override
  public void onAuthentication(
      Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
    completeLogin(authentication, request, response, () -> {});
  }

  /** Explicit sign-in must not use an existing slot to bypass the current maintenance policy. */
  public void completeLogin(
      Authentication authentication,
      HttpServletRequest request,
      HttpServletResponse response,
      Runnable completion) {
    try {
      new MaintenanceService(configService).requireLogin(authentication);
    } catch (MaintenanceAuthenticationException failure) {
      request.setAttribute(FAILURE_REASON, "maintenance");
      throw failure;
    } catch (AuthenticationServiceException failure) {
      request.setAttribute(FAILURE_REASON, "unavailable");
      throw failure;
    }
    complete(
        authentication,
        request,
        response,
        () -> {
          // All explicit login paths reach this completion after fixation/admission, including
          // Flow and Remember-me. Ordinary context persistence calls complete() directly.
          csrfAuthentication.onAuthentication(authentication, request, response);
          completion.run();
        });
  }

  /** Reserve before publishing any principal/cookie; roll back a failed completion. */
  public void complete(
      Authentication authentication,
      HttpServletRequest request,
      HttpServletResponse response,
      Runnable completion) {
    HttpSession session = request.getSession(false);
    if (session != null) {
      // Different account logins in the same browser must not overwrite each other's reservation.
      synchronized (WebUtils.getSessionMutex(session)) {
        completeForIdentity(authentication, request, response, completion);
      }
    } else {
      completeForIdentity(authentication, request, response, completion);
    }
  }

  private void completeForIdentity(
      Authentication authentication,
      HttpServletRequest request,
      HttpServletResponse response,
      Runnable completion) {
    String identity = EmailIdentity.canonicalize(authentication.getName());
    if (!authentication.isAuthenticated() || identity == null || identity.isBlank()) {
      throw new AuthenticationServiceException("Authenticated identity required");
    }
    synchronized (identityLocks[(identity.hashCode() & Integer.MAX_VALUE) % identityLocks.length]) {
      try {
        completeLocked(authentication, identity, request, response, completion);
      } catch (RuntimeException failure) {
        recordFailureReason(request, failure);
        rollback(request);
        throw failure;
      }
    }
  }

  private void completeLocked(
      Authentication authentication,
      String identity,
      HttpServletRequest request,
      HttpServletResponse response,
      Runnable completion) {
    String returningSeries = (String) request.getAttribute(DeviceRememberMeServices.SERIES);
    if (returningSeries != null
        && deviceTokenRepository != null
        && !deviceTokenRepository.belongsTo(returningSeries, identity)) {
      throw new AuthenticationServiceException("Saved login was revoked");
    }
    HttpSession session = request.getSession(false);
    SessionInformation existing =
        session == null ? null : sessionRegistry.getSessionInformation(session.getId());
    if (existing != null) {
      if (existing.isExpired() || !identity.equals(existing.getPrincipal())) {
        throw new AuthenticationServiceException("Session identity changed");
      }
      // Context restoration and extra tabs reuse their slot, even after a limit reduction.
      completion.run();
      return;
    }
    new MaintenanceService(configService).requireLogin(authentication);
    int limit = sessionLimit();
    if (sessionRegistry.getAllSessions(identity, false).size() >= limit) {
      throw new SessionLimitException();
    }

    registerSession(authentication, identity, returningSeries, request, response);
    completion.run();
    log.info(
        "Authenticated session admitted: authenticationType={}",
        authentication.getClass().getSimpleName());
  }

  private int sessionLimit() {
    int limit;
    try {
      limit = configService.getInteger(ConfigEntry.MAX_SESSIONS_PER_USER);
    } catch (RuntimeException failure) {
      throw new AuthenticationServiceException("Session configuration unavailable", failure);
    }
    if (limit < 1) {
      throw new AuthenticationServiceException("Invalid session limit");
    }
    return limit;
  }

  private void registerSession(
      Authentication authentication,
      String identity,
      String returningSeries,
      HttpServletRequest request,
      HttpServletResponse response) {
    // Keep Vaadin's UI/session attributes, changing only the identifier against fixation.
    changeSessionIdStrategy.onAuthentication(authentication, request, response);
    HttpSession session = request.getSession(true);
    request.setAttribute(NEW_SESSION, session);
    sessionRegistry.registerNewSession(session.getId(), identity);
    if (returningSeries != null) {
      session.setAttribute(DeviceRememberMeServices.SERIES, returningSeries);
    }
    Active active =
        new Active(
            session.getId(),
            UUID.randomUUID().toString(),
            identity,
            DeviceDescription.from(request.getHeader("User-Agent")),
            Instant.now(),
            (String) session.getAttribute(DeviceRememberMeServices.SERIES));
    if (sessionRegistry instanceof DurableSessionRegistry<?>) {
      session.setAttribute(DurableSessionRepository.ACTIVE, active);
    } else {
      activeSessionsBySessionId.put(
          session.getId(),
          new Active(
              active.sessionId(),
              active.handle(),
              active.identity(),
              active.device(),
              active.created(),
              active.series(),
              SignInMethod.fromAuthentication(authentication)));
    }
    session.setAttribute(
        REGISTRATION, new Registration(sessionRegistry, activeSessionsBySessionId));
  }

  private void recordFailureReason(HttpServletRequest request, RuntimeException failure) {
    if (failure instanceof MaintenanceAuthenticationException) {
      request.setAttribute(FAILURE_REASON, "maintenance");
    } else if (failure instanceof SessionLimitException) {
      request.setAttribute(FAILURE_REASON, "sessions");
    } else if (failure instanceof AuthenticationServiceException) {
      request.setAttribute(FAILURE_REASON, "unavailable");
    }
  }

  /** Only this request's new reservation may be undone; never evict existing devices. */
  public boolean rollback(HttpServletRequest request) {
    if (request.getAttribute(NEW_SESSION) instanceof HttpSession session) {
      // Keep the request-local marker: nested failure handlers may also need to cancel a cookie.
      sessionRegistry.removeSessionInformation(session.getId());
      activeSessionsBySessionId.remove(session.getId());
      try {
        session.invalidate();
      } catch (IllegalStateException _) {
        /* Already released. */
      }
      SecurityContextHolder.clearContext();
      return true;
    }
    return false;
  }

  /** Freigabe auch für einfache Servlet-/Test-Sitzungen; keine Registry wird serialisiert. */
  private static final class Registration implements HttpSessionBindingListener, Serializable {
    private final transient SessionRegistry sessionRegistry;
    private final transient Map<String, Active> activeSessionsBySessionId;

    private Registration(
        SessionRegistry sessionRegistry, Map<String, Active> activeSessionsBySessionId) {
      this.sessionRegistry = sessionRegistry;
      this.activeSessionsBySessionId = activeSessionsBySessionId;
    }

    @Override
    public void valueUnbound(HttpSessionBindingEvent event) {
      if (sessionRegistry != null) {
        sessionRegistry.removeSessionInformation(event.getSession().getId());
        activeSessionsBySessionId.remove(event.getSession().getId());
        return;
      }
      // Nach Lesen an die aktuelle Anwendung anbinden, niemals einen alten Dienstgraph nutzen.
      SessionAdmissionService current =
          WebApplicationContextUtils.getRequiredWebApplicationContext(
                  event.getSession().getServletContext())
              .getBean(SessionAdmissionService.class);
      current.releaseRegistration(event.getSession().getId());
    }
  }

  private void releaseRegistration(String id) {
    sessionRegistry.removeSessionInformation(id);
    activeSessionsBySessionId.remove(id);
  }
}
