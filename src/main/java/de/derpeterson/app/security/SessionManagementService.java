package de.derpeterson.app.security;

import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.security.DeviceTokenRepository.Saved;
import de.derpeterson.app.security.SessionAdmissionService.Active;
import de.derpeterson.app.service.ConfigService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.util.WebUtils;

/** Eigene Sitzungen und gespeicherte Anmeldungen; kein Zugang zu fremden Konten. */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionManagementService {
  public record Entry(
      String handle,
      DeviceDescription device,
      Instant created,
      Instant lastActivity,
      boolean current,
      boolean active,
      boolean remembered,
      String savedHandle)
      implements Serializable {
    public Entry(
        String handle,
        DeviceDescription device,
        Instant created,
        Instant lastActivity,
        boolean current,
        boolean active,
        boolean remembered) {
      this(handle, device, created, lastActivity, current, active, remembered, null);
    }
  }

  public record Snapshot(List<Entry> entries, int active, int limit) implements Serializable {}

  private record Proof(String identity, String fingerprint, boolean remember, Instant expires)
      implements Serializable {
    @Override
    public String toString() {
      return "Restricted session-management proof";
    }
  }

  private static final String PROOF = SessionManagementService.class.getName() + ".proof";
  private final SessionAdmissionService admissions;
  private final DeviceTokenRepository tokens;
  private final SecurityService security;
  private final CustomUserDetailsService users;
  private final AuthenticationManager authentication;
  private final PasswordEncoder encoder;
  private final ConfigService config;
  private final DeviceRememberMeServices remembered;
  private Clock clock = Clock.systemUTC();

  @Scheduled(cron = "0 0 * * * *")
  public void deleteExpiredSavedLogins() {
    tokens.deleteExpired(savedLoginDuration());
  }

  private int savedLoginDuration() {
    // Dies ist startgebunden wie die Cookieprüfung. Bloßes Speichern einer neuen
    // Konfiguration darf vorhandene Zugänge noch nicht vorzeitig entfernen.
    int duration = remembered.validitySeconds();
    if (duration < 1) {
      throw new AuthenticationServiceException("Invalid saved-login duration");
    }
    return duration;
  }

  public Snapshot list(HttpServletRequest request) {
    String identity = owner(request);
    return admissions.withIdentity(identity, () -> snapshot(identity, currentId(request)));
  }

  public void revoke(HttpServletRequest request, String handle) {
    String identity = owner(request);
    admissions.withIdentity(
        identity,
        () -> {
          revokeOwned(identity, currentId(request), handle);
          return null;
        });
  }

  public void revokeOthers(HttpServletRequest request) {
    String identity = owner(request);
    admissions.withIdentity(
        identity,
        () -> {
          revokeOthersOwned(identity, currentId(request));
          return null;
        });
  }

  private String owner(HttpServletRequest request) {
    UserDetails user =
        security
            .getAuthenticatedUser(request)
            .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Sign in required"));
    new MaintenanceService(config)
        .requireLogin(
            UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
    return user.getUsername();
  }

  private Snapshot snapshot(String identity, String current) {
    int duration = savedLoginDuration();
    List<DeviceTokenRepository.Saved> saved = tokens.saved(identity, duration);
    List<SessionAdmissionService.Active> active = admissions.active(identity);
    Set<String> activeSeries = new HashSet<>();
    List<Entry> entries = new ArrayList<>();
    active.stream()
        .sorted(
            Comparator.comparing(
                    (SessionAdmissionService.Active item) -> !item.sessionId().equals(current))
                .thenComparing(SessionAdmissionService.Active::created))
        .forEach(
            item -> {
              Optional<Saved> savedLogin =
                  saved.stream().filter(grant -> grant.series().equals(item.series())).findFirst();
              boolean remembered = savedLogin.isPresent();
              if (remembered) {
                activeSeries.add(item.series());
              }
              entries.add(
                  new Entry(
                      item.handle(),
                      item.device(),
                      item.created(),
                      admissions.lastActivity(item.sessionId()),
                      item.sessionId().equals(current),
                      true,
                      remembered,
                      savedLogin.map(DeviceTokenRepository.Saved::handle).orElse(null)));
            });
    saved.stream()
        .filter(item -> !activeSeries.contains(item.series()))
        .forEach(
            item ->
                entries.add(
                    new Entry(
                        item.handle(),
                        item.device(),
                        item.created(),
                        item.lastUsed(),
                        false,
                        false,
                        true)));
    int limit = config.getInteger(ConfigEntry.MAX_SESSIONS_PER_USER);
    if (limit < 1) {
      throw new AuthenticationServiceException("Invalid session limit");
    }
    return new Snapshot(List.copyOf(entries), active.size(), limit);
  }

  private void revokeOwned(String identity, String current, String handle) {
    if (handle == null || handle.isBlank()) {
      throw new IllegalArgumentException("Entry required");
    }
    List<Active> active = admissions.active(identity);
    Optional<Saved> savedLogin =
        tokens.saved(identity, savedLoginDuration()).stream()
            .filter(item -> item.handle().equals(handle))
            .findFirst();
    Optional<Active> exact =
        active.stream().filter(item -> item.handle().equals(handle)).findFirst();
    if (exact.isPresent() && exact.get().sessionId().equals(current)) {
      throw new IllegalArgumentException("Use normal logout for the current session");
    }
    Optional<Active> target =
        exact.or(
            () ->
                active.stream()
                    .filter(
                        item ->
                            !item.sessionId().equals(current)
                                && savedLogin.isPresent()
                                && savedLogin.get().series().equals(item.series()))
                    .findFirst());
    if (target.isEmpty()
        && savedLogin.isPresent()
        && active.stream()
            .anyMatch(
                item ->
                    item.sessionId().equals(current)
                        && savedLogin.get().series().equals(item.series()))) {
      throw new IllegalArgumentException("Use normal logout for the current session");
    }
    // Zuerst dauerhaft widerrufen; bei Datenbankfehlern keine nur teilweise abgemeldete Sitzung.
    if (target.isPresent()) {
      Active item = target.get();
      if (item.series() != null) {
        tokens.removeSeries(identity, item.series());
      }
      active.stream()
          .filter(other -> !other.sessionId().equals(current))
          .filter(
              other ->
                  Objects.equals(other.series(), item.series()) && item.series() != null
                      || other.sessionId().equals(item.sessionId()))
          .forEach(other -> admissions.expire(identity, other.sessionId()));
      log.info(
          "Owned device access revoked: activeSession=true, savedLogin={}", item.series() != null);
      return;
    }
    // Fremde und bereits entfernte Kennungen liefern denselben neutralen Zustand.
    savedLogin.ifPresent(
        item -> {
          tokens.removeSeries(identity, item.series());
          log.info("Owned device access revoked: activeSession=false, savedLogin=true");
        });
  }

  private void revokeOthersOwned(String identity, String current) {
    List<Active> active = admissions.active(identity);
    String keep =
        active.stream()
            .filter(item -> item.sessionId().equals(current))
            .map(SessionAdmissionService.Active::series)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    String retainedSeries =
        keep != null
                && active.stream()
                    .anyMatch(
                        item -> !item.sessionId().equals(current) && keep.equals(item.series()))
            ? null
            : keep;
    tokens.atomic(
        () -> {
          tokens.saved(identity, savedLoginDuration()).stream()
              .filter(item -> !Objects.equals(item.series(), retainedSeries))
              .forEach(item -> tokens.removeSeries(identity, item.series()));
          return null;
        });
    active.stream()
        .filter(item -> !item.sessionId().equals(current))
        .forEach(item -> admissions.expire(identity, item.sessionId()));
    log.info("Other device access revoked");
  }

  /**
   * Erneute Passwortprüfung inklusive persistentem Versuchsschutz; niemals einen Principal
   * speichern.
   */
  public void startRecovery(
      HttpServletRequest request, String email, String password, boolean remember) {
    cancelRecovery(request);
    Authentication verified =
        authentication.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(email, password));
    if (!verified.isAuthenticated() || !(verified.getPrincipal() instanceof UserDetails)) {
      throw new BadCredentialsException("Invalid sign-in");
    }
    UserDetails current = users.loadUserByUsername(verified.getName());
    if (!current.isEnabled() || !encoder.matches(password, current.getPassword())) {
      throw new BadCredentialsException("Sign-in changed");
    }
    new MaintenanceService(config).requireLogin(verified);
    request.getSession(true);
    // Auch der eingeschränkte Zugang darf keine vorab bekannte Sitzungskennung übernehmen.
    request.changeSessionId();
    request
        .getSession()
        .setAttribute(
            PROOF,
            new Proof(
                current.getUsername(),
                fingerprint(current),
                remember,
                clock.instant().plusSeconds(300)));
  }

  public boolean hasRecovery(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    return session != null
        && session.getAttribute(PROOF) instanceof Proof proof
        && clock.instant().isBefore(proof.expires());
  }

  public Snapshot recoveryList(HttpServletRequest request) {
    return recovery(request, user -> snapshot(user.getUsername(), null));
  }

  public void recoveryRevoke(HttpServletRequest request, String handle) {
    recovery(
        request,
        user -> {
          revokeOwned(user.getUsername(), null, handle);
          return null;
        });
  }

  public void recoveryRevokeOthers(HttpServletRequest request) {
    recovery(
        request,
        user -> {
          revokeOthersOwned(user.getUsername(), null);
          return null;
        });
  }

  public boolean finishRecovery(HttpServletRequest request, HttpServletResponse response) {
    HttpSession session = request.getSession(false);
    if (session == null) {
      throw new AuthenticationCredentialsNotFoundException("Sign in again");
    }
    // Gleiche Sperrreihenfolge wie die reguläre Zulassung: HTTP-Sitzung vor Konto.
    synchronized (WebUtils.getSessionMutex(session)) {
      return recovery(
          request,
          user -> {
            Proof proof = (Proof) session.getAttribute(PROOF);
            UserDetails principal = User.withUserDetails(user).password("").build();
            security.storeAuthenticatedUser(request, response, principal, proof.remember());
            cancelRecovery(request);
            return principal.getAuthorities().stream()
                .anyMatch(role -> "ROLE_ADMIN".equals(role.getAuthority()));
          });
    }
  }

  public void cancelRecovery(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session != null) {
      session.removeAttribute(PROOF);
    }
  }

  private <T> T recovery(HttpServletRequest request, Function<UserDetails, T> action) {
    HttpSession session = request.getSession(false);
    if (session == null || !(session.getAttribute(PROOF) instanceof Proof proof)) {
      throw new AuthenticationCredentialsNotFoundException("Sign in again");
    }
    return admissions.withIdentity(
        proof.identity(),
        () -> {
          if (session.getAttribute(PROOF) != proof || !clock.instant().isBefore(proof.expires())) {
            cancelRecovery(request);
            throw new AuthenticationCredentialsNotFoundException("Sign in again");
          }
          UserDetails current = users.loadUserByUsername(proof.identity());
          if (!current.isEnabled() || !proof.fingerprint().equals(fingerprint(current))) {
            cancelRecovery(request);
            throw new AuthenticationCredentialsNotFoundException("Sign-in changed");
          }
          new MaintenanceService(config)
              .requireLogin(
                  UsernamePasswordAuthenticationToken.authenticated(
                      current, null, current.getAuthorities()));
          return action.apply(current);
        });
  }

  private static String currentId(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    return session == null ? null : session.getId();
  }

  private static String fingerprint(UserDetails user) {
    try {
      String roles =
          user.getAuthorities().stream()
              .map(role -> role.getAuthority())
              .sorted()
              .reduce("", (a, b) -> a + "," + b);
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest((user.getPassword() + roles).getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException(impossible);
    }
  }
}
