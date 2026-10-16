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
import java.util.Collection;
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
import org.springframework.transaction.annotation.Transactional;

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
      return true; // Public requests have no authenticated UI to revoke.
    }
    Object storedContext =
        session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
    Authentication authentication =
        storedContext instanceof SecurityContext context ? context.getAuthentication() : null;
    Object storedUser = session.getAttribute(AUTH_USER_SESSION_KEY);
    boolean valid =
        (authentication == null
                || !(authentication.getPrincipal() instanceof UserDetails principal)
                || hasCurrentPrivileges(principal, authentication))
            && (!(storedUser instanceof UserDetails storedPrincipal)
                || hasCurrentPrivileges(storedPrincipal, null));
    if (!valid) {
      session.removeAttribute(AUTH_USER_SESSION_KEY);
      session.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
      session.invalidate(); // Discard every old UI associated with this connection.
      SecurityContextHolder.clearContext();
    }
    return valid;
  }

  public Optional<UserDetails> getAuthenticatedUser(HttpServletRequest request) {
    SecurityContext securityContext = SecurityContextHolder.getContext();
    Authentication authentication = securityContext.getAuthentication();
    if (authentication != null
        && authentication.isAuthenticated()
        && authentication.getPrincipal() instanceof UserDetails userDetails) {
      if (hasCurrentPrivileges(userDetails, authentication)) {
        return Optional.of(userDetails);
      }
      clearStaleAuthentication(request);
      return Optional.empty();
    }

    UserDetails userDetails = null;
    if (VaadinSession.getCurrent() != null) {
      userDetails = VaadinSession.getCurrent().getAttribute(UserDetails.class);
    }

    if (userDetails == null) {
      HttpSession httpSession = request.getSession(false);
      if (httpSession != null) {
        userDetails = (UserDetails) httpSession.getAttribute(AUTH_USER_SESSION_KEY);
      }
    }

    if (userDetails != null) {
      if (!hasCurrentPrivileges(userDetails, null)) {
        clearStaleAuthentication(request);
        return Optional.empty();
      }
      restoreSecurityContext(userDetails);
      return Optional.of(userDetails);
    }

    return Optional.empty();
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
          () -> {
            handleLogin(user);
            if (VaadinSession.getCurrent() != null) {
              VaadinSession.getCurrent().setAttribute(UserDetails.class, user);
              userService
                  .findByEmail(user.getUsername())
                  .ifPresent(
                      entity -> VaadinSession.getCurrent().setLocale(entity.getPreferredLocale()));
            }
            request.getSession().setAttribute(AUTH_USER_SESSION_KEY, user);
            restoreSecurityContext(user);
            securityContextRepository.saveContext(
                SecurityContextHolder.getContext(), request, response);
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
          });
    } catch (RuntimeException failure) {
      if (VaadinSession.getCurrent() != null)
        VaadinSession.getCurrent().setAttribute(UserDetails.class, null);
      SecurityContextHolder.clearContext();
      if (Boolean.TRUE.equals(rememberMe) && !response.isCommitted()) {
        try {
          rememberMeServices.loginFail(request, response);
        } catch (RuntimeException cleanupFailure) {
          failure.addSuppressed(cleanupFailure);
        }
      }
      if (failure instanceof AuthenticationException authenticationFailure)
        throw authenticationFailure;
      throw new AuthenticationServiceException("Login completion failed", failure);
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

  private boolean hasCurrentPrivileges(UserDetails principal, Authentication authentication) {
    try {
      UserDetails current = userDetailsService.loadUserByUsername(principal.getUsername());
      var authorities = accountAuthorities(current.getAuthorities());
      return current.isEnabled()
          && authorities.equals(accountAuthorities(principal.getAuthorities()))
          && (authentication == null
              || authorities.equals(accountAuthorities(authentication.getAuthorities())));
    } catch (UsernameNotFoundException exception) {
      return false;
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
    if (VaadinSession.getCurrent() != null) {
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

  @Transactional
  public void handleLogin(UserDetails user) throws AuthenticationException {
    userService.recordLogin(user.getUsername());
  }
}
