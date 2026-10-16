package de.derpeterson.app.security;

import de.derpeterson.app.model.EmailIdentity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.service.ConfigService;
import jakarta.servlet.http.*;
import java.util.Arrays;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.web.util.WebUtils;

/** One-instance admission. Fixed stripes bound lock memory independently of login identities. */
public class SessionAdmissionService implements SessionAuthenticationStrategy {
  public static final String FAILURE_REASON = SessionAdmissionService.class.getName() + ".failure";
  private static final String REGISTRATION =
      SessionAdmissionService.class.getName() + ".registration";
  private static final String NEW_SESSION = SessionAdmissionService.class.getName() + ".newSession";
  private final ConfigService config;
  private final SessionRegistry registry;
  private final Object[] locks = new Object[256];
  private final ChangeSessionIdAuthenticationStrategy fixation =
      new ChangeSessionIdAuthenticationStrategy();

  public SessionAdmissionService(ConfigService config, SessionRegistry registry) {
    this.config = config;
    this.registry = registry;
    Arrays.setAll(locks, index -> new Object());
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
      new MaintenanceService(config).requireLogin(authentication);
    } catch (MaintenanceAuthenticationException failure) {
      request.setAttribute(FAILURE_REASON, "maintenance");
      throw failure;
    } catch (AuthenticationServiceException failure) {
      request.setAttribute(FAILURE_REASON, "unavailable");
      throw failure;
    }
    complete(authentication, request, response, completion);
  }

  /** Reserve before publishing any principal/cookie; roll back a failed completion. */
  public void complete(
      Authentication authentication,
      HttpServletRequest request,
      HttpServletResponse response,
      Runnable completion) {
    var session = request.getSession(false);
    if (session != null) {
      // Different account logins in the same browser must not overwrite each other's reservation.
      synchronized (WebUtils.getSessionMutex(session)) {
        completeForIdentity(authentication, request, response, completion);
      }
    } else completeForIdentity(authentication, request, response, completion);
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
    synchronized (locks[(identity.hashCode() & Integer.MAX_VALUE) % locks.length]) {
      try {
        var session = request.getSession(false);
        var existing = session == null ? null : registry.getSessionInformation(session.getId());
        if (existing != null) {
          if (existing.isExpired() || !identity.equals(existing.getPrincipal())) {
            throw new AuthenticationServiceException("Session identity changed");
          }
          // Context restoration and extra tabs reuse their slot, even after a limit reduction.
          completion.run();
          return;
        }
        new MaintenanceService(config).requireLogin(authentication);
        int limit;
        try {
          limit = config.getInteger(ConfigEntry.MAX_SESSIONS_PER_USER);
        } catch (RuntimeException failure) {
          throw new AuthenticationServiceException("Session configuration unavailable", failure);
        }
        if (limit < 1) throw new AuthenticationServiceException("Invalid session limit");
        if (registry.getAllSessions(identity, true).size() >= limit)
          throw new SessionLimitException();

        // Keep Vaadin's UI/session attributes, changing only the identifier against fixation.
        fixation.onAuthentication(authentication, request, response);
        session = request.getSession(true);
        request.setAttribute(NEW_SESSION, session);
        registry.registerNewSession(session.getId(), identity);
        session.setAttribute(REGISTRATION, new Registration(registry));
        completion.run();
      } catch (RuntimeException failure) {
        if (failure instanceof MaintenanceAuthenticationException)
          request.setAttribute(FAILURE_REASON, "maintenance");
        else if (failure instanceof SessionLimitException)
          request.setAttribute(FAILURE_REASON, "sessions");
        else if (failure instanceof AuthenticationServiceException)
          request.setAttribute(FAILURE_REASON, "unavailable");
        rollback(request);
        throw failure;
      }
    }
  }

  /** Only this request's new reservation may be undone; never evict existing devices. */
  public boolean rollback(HttpServletRequest request) {
    if (request.getAttribute(NEW_SESSION) instanceof HttpSession session) {
      // Keep the request-local marker: nested failure handlers may also need to cancel a cookie.
      registry.removeSessionInformation(session.getId());
      try {
        session.invalidate();
      } catch (IllegalStateException alreadyEnded) {
        /* Already released. */
      }
      SecurityContextHolder.clearContext();
      return true;
    }
    return false;
  }

  /** Binding callback also releases MockHttpSessions and explicit invalidations immediately. */
  private record Registration(SessionRegistry registry) implements HttpSessionBindingListener {
    @Override
    public void valueUnbound(HttpSessionBindingEvent event) {
      registry.removeSessionInformation(event.getSession().getId());
    }
  }
}
