package de.derpeterson.app.security;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SecurityService {

  private static final String AUTH_USER_SESSION_KEY = "authenticatedUser";

  private final PersistentTokenBasedRememberMeServices rememberMeServices;

  private final UserService userService;
  private final UserRepository userRepository;
  private final CustomUserDetailsService userDetailsService;
  private final SecurityContextRepository securityContextRepository;
  private final SessionAdmissionService admissions;

  /**
   * Runs before Vaadin resolves/locks a UI, including for each WebSocket message. Use the
   * connection's HTTP session, not a worker-thread context or VaadinSession attributes (which
   * require the session lock).
   */
  public boolean hasCurrentSessionPrivileges(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session == null) {
      return true;
    }
    Authentication authentication = sessionAuthentication(session);
    boolean hadPrincipal =
        authentication != null && authentication.getPrincipal() instanceof UserDetails
            || session.getAttribute(AUTH_USER_SESSION_KEY) instanceof UserDetails;
    Optional<Authentication> refreshed =
        hadPrincipal ? refreshSessionAuthentication(session) : Optional.<Authentication>empty();
    if (admissions.isExpired(request) || hadPrincipal && refreshed.isEmpty()) {
      session.removeAttribute(AUTH_USER_SESSION_KEY);
      session.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
      session.invalidate();
      SecurityContextHolder.clearContext();
      return false;
    }
    refreshed.ifPresent(this::useAuthentication);
    return true;
  }

  public Optional<UserDetails> getAuthenticatedUser(HttpServletRequest request) {
    if (admissions.isExpired(request)) {
      clearStaleAuthentication(request);
      return Optional.empty();
    }
    HttpSession session = request == null ? null : request.getSession(false);
    Authentication auth = authenticationFromAvailableContexts(session);
    if (auth == null || !(auth.getPrincipal() instanceof UserDetails)) {
      return Optional.empty();
    }
    Optional<Authentication> fresh = refreshedAuthentication(auth);
    if (fresh.isEmpty()) {
      clearStaleAuthentication(request);
      return Optional.empty();
    }
    install(session, fresh.get());
    useAuthentication(fresh.get());
    return Optional.of((UserDetails) fresh.get().getPrincipal());
  }

  private Authentication authenticationFromAvailableContexts(HttpSession session) {
    Authentication auth = session == null ? null : sessionAuthentication(session);
    UserDetails stored = null;
    if (session != null
        && session.getAttribute(AUTH_USER_SESSION_KEY) instanceof UserDetails principal) {
      stored = principal;
    }
    if (auth == null && stored != null) {
      auth =
          UsernamePasswordAuthenticationToken.authenticated(stored, null, stored.getAuthorities());
    }
    if (auth == null) {
      auth = SecurityContextHolder.getContext().getAuthentication();
    }
    if ((auth == null || !(auth.getPrincipal() instanceof UserDetails))
        && VaadinSession.getCurrent() != null
        && VaadinSession.getCurrent().hasLock()) {
      UserDetails sessionPrincipal = VaadinSession.getCurrent().getAttribute(UserDetails.class);
      if (sessionPrincipal != null) {
        auth =
            UsernamePasswordAuthenticationToken.authenticated(
                sessionPrincipal, null, sessionPrincipal.getAuthorities());
      }
    }
    return auth;
  }

  /** Frische Rechte der gebundenen Sitzung; kein Zugriff auf den Kontext eines fremden Threads. */
  public Optional<Authentication> refreshSessionAuthentication(HttpSession session) {
    if (session == null) {
      return Optional.empty();
    }
    try {
      if (admissions.isSessionExpired(session.getId())) {
        return Optional.empty();
      }
      Authentication auth = sessionAuthentication(session);
      if (auth == null && session.getAttribute(AUTH_USER_SESSION_KEY) instanceof UserDetails p) {
        auth = UsernamePasswordAuthenticationToken.authenticated(p, null, p.getAuthorities());
      }
      Optional<Authentication> fresh = refreshedAuthentication(auth);
      fresh.ifPresent(a -> install(session, a));
      return fresh;
    } catch (IllegalStateException _) {
      return Optional.empty();
    }
  }

  public Optional<UserEntity> getSessionUser(HttpSession session) {
    return refreshSessionAuthentication(session)
        .flatMap(a -> userRepository.findByEmail(((UserDetails) a.getPrincipal()).getUsername()));
  }

  private Authentication sessionAuthentication(HttpSession session) {
    Object value =
        session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
    return value instanceof SecurityContext context ? context.getAuthentication() : null;
  }

  private void install(HttpSession session, Authentication authentication) {
    if (session == null) {
      return;
    }
    Objects.requireNonNull(authentication, "Authentication required");
    Authentication existing = sessionAuthentication(session);
    if (existing != authentication) {
      SecurityContext context = SecurityContextHolder.createEmptyContext();
      context.setAuthentication(authentication);
      session.setAttribute(
          HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    }
    session.setAttribute(AUTH_USER_SESSION_KEY, authentication.getPrincipal());
  }

  private void useAuthentication(Authentication authentication) {
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
    if (VaadinSession.getCurrent() != null && VaadinSession.getCurrent().hasLock()) {
      VaadinSession.getCurrent()
          .setAttribute(UserDetails.class, (UserDetails) authentication.getPrincipal());
    }
  }

  public void touchSession(HttpServletRequest request) {
    admissions.touch(request);
  }

  public Optional<UserEntity> getCurrentUser() {
    HttpServletRequest request = (HttpServletRequest) VaadinRequest.getCurrent();
    return getCurrentUser(request);
  }

  public Optional<UserEntity> getCurrentUser(HttpServletRequest request) {
    Optional<UserDetails> authenticatedUser = getAuthenticatedUser(request);
    if (authenticatedUser.isPresent()) {
      return userRepository.findByEmail(authenticatedUser.get().getUsername());
    }
    return Optional.empty();
  }

  public void storeAuthenticatedUser(
      HttpServletRequest request,
      HttpServletResponse response,
      UserDetails user,
      Boolean rememberMe) {
    Authentication auth =
        UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities());
    try {
      admissions.completeLogin(
          auth,
          request,
          response,
          () -> completeLoginContext(request, response, user, rememberMe, auth));
    } catch (RuntimeException failure) {
      if (VaadinSession.getCurrent() != null) {
        VaadinSession.getCurrent().setAttribute(UserDetails.class, null);
      }
      SecurityContextHolder.clearContext();
      if (Boolean.TRUE.equals(rememberMe) && !response.isCommitted()) {
        try {
          rememberMeServices.loginFail(request, response);
        } catch (RuntimeException cleanupFailure) {
          failure.addSuppressed(cleanupFailure);
        }
      }
      if (failure instanceof AuthenticationException authenticationFailure) {
        throw authenticationFailure;
      }
      throw new AuthenticationServiceException("Login completion failed", failure);
    }
  }

  private void completeLoginContext(
      HttpServletRequest request,
      HttpServletResponse response,
      UserDetails user,
      Boolean rememberMe,
      Authentication auth) {
    handleLogin(user);
    if (VaadinSession.getCurrent() != null) {
      VaadinSession.getCurrent().setAttribute(UserDetails.class, user);
      userService
          .findByEmail(user.getUsername())
          .ifPresent(entity -> VaadinSession.getCurrent().setLocale(entity.getPreferredLocale()));
    }
    request.getSession().setAttribute(AUTH_USER_SESSION_KEY, user);
    restoreSecurityContext(user);
    securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response);
    if (rememberMeServices instanceof DeviceRememberMeServices devices) {
      devices.preparePasswordLogin(request, response, user.getUsername());
    }
    if (Boolean.TRUE.equals(rememberMe)) {
      // Checkbox selection belongs to this request, never to the singleton service.
      var rememberedRequest =
          new HttpServletRequestWrapper(request) {
            @Override
            public String getParameter(String name) {
              return "remember-me".equals(name) ? "true" : super.getParameter(name);
            }
          };
      rememberMeServices.loginSuccess(rememberedRequest, response, auth);
    }
  }

  private Authentication restoreSecurityContext(UserDetails user) {
    Authentication auth =
        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(auth);
    SecurityContextHolder.setContext(context);
    return auth;
  }

  public void logout() {
    HttpServletRequest request = (HttpServletRequest) VaadinRequest.getCurrent();
    HttpServletResponse response = (HttpServletResponse) VaadinResponse.getCurrent();

    HttpSession httpSession = request.getSession(false);
    if (rememberMeServices != null
        && SecurityContextHolder.getContext().getAuthentication() != null) {
      rememberMeServices.logout(
          request, response, SecurityContextHolder.getContext().getAuthentication());
    }
    if (httpSession != null) {
      httpSession.invalidate();
    } else if (VaadinSession.getCurrent() != null) {
      VaadinSession.getCurrent().getSession().invalidate();
    }

    SecurityContextHolder.clearContext();
  }

  private Optional<Authentication> refreshedAuthentication(Authentication auth) {
    if (auth == null
        || !auth.isAuthenticated()
        || !(auth.getPrincipal() instanceof UserDetails principal)) {
      return Optional.empty();
    }
    Set<String> old = accountAuthorities(principal.getAuthorities());
    Set<String> known =
        java.util.Arrays.stream(de.derpeterson.app.model.enums.RoleType.values())
            .map(Enum::name)
            .collect(Collectors.toSet());
    // Nur gespeicherte Kontorollen und echte Framework-Faktoren akzeptieren; keine erfundenen
    // Privilegien.
    if (!known.containsAll(old) || !old.equals(accountAuthorities(auth.getAuthorities()))) {
      return Optional.empty();
    }
    try {
      UserDetails current = userDetailsService.loadUserByUsername(principal.getUsername());
      if (!current.isEnabled()) {
        return Optional.empty();
      }
      if (old.equals(accountAuthorities(current.getAuthorities()))) {
        return Optional.of(auth);
      }
      ArrayList<GrantedAuthority> authorities =
          new java.util.ArrayList<GrantedAuthority>(current.getAuthorities());
      auth.getAuthorities().stream()
          .filter(FactorGrantedAuthority.class::isInstance)
          .forEach(authorities::add);
      org.springframework.security.authentication.AbstractAuthenticationToken refreshed;
      if (auth
          instanceof org.springframework.security.authentication.RememberMeAuthenticationToken) {
        refreshed =
            new org.springframework.security.authentication.RememberMeAuthenticationToken(
                rememberMeServices.getKey(), current, authorities);
      } else {
        refreshed = UsernamePasswordAuthenticationToken.authenticated(current, null, authorities);
      }
      refreshed.setDetails(auth.getDetails());
      return Optional.of(refreshed);
    } catch (UsernameNotFoundException _) {
      return Optional.empty();
    }
  }

  private Set<String> accountAuthorities(Collection<? extends GrantedAuthority> authorities) {
    // Spring's typed factor evidence records HOW login occurred; it is not a stored account role.
    // Do not ignore arbitrary strings with a FACTOR_ prefix: only genuine framework factor objects.
    return authorities.stream()
        .filter(authority -> !(authority instanceof FactorGrantedAuthority))
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.toSet());
  }

  private void clearStaleAuthentication(HttpServletRequest request) {
    if (VaadinSession.getCurrent() != null && VaadinSession.getCurrent().hasLock()) {
      VaadinSession.getCurrent().setAttribute(UserDetails.class, null);
    }

    HttpSession httpSession = request == null ? null : request.getSession(false);
    if (httpSession != null) {
      httpSession.removeAttribute(AUTH_USER_SESSION_KEY);
      httpSession.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
      // Discard the old Vaadin UIs too: a later remember-me authentication
      // with reduced roles must not resume callbacks on an old admin view.
      httpSession.invalidate();
    }

    SecurityContextHolder.getContext().setAuthentication(null);
    SecurityContextHolder.clearContext();
  }

  // Der injizierte UserService setzt die Persistenzgrenze; dieser Adapter schreibt selbst nichts.
  public void handleLogin(UserDetails user) throws AuthenticationException {
    userService.recordLogin(user.getUsername());
  }
}
