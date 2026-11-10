package de.derpeterson.app.security.session;

import de.derpeterson.app.security.SessionAdmissionService.Active;
import de.derpeterson.app.security.SignInMethod;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;

/**
 * Auch Reservierungen ohne veröffentlichten SecurityContext zählen sofort zum bestehenden Limit.
 */
public final class DurableSessionRegistry<S extends Session> implements SessionRegistry {
  private final FindByIndexNameSessionRepository<S> sessionRepository;

  public DurableSessionRegistry(FindByIndexNameSessionRepository<S> sessionRepository) {
    this.sessionRepository = sessionRepository;
  }

  @Override
  public List<Object> getAllPrincipals() {
    throw new UnsupportedOperationException("Use account-scoped session lookup");
  }

  @Override
  public List<SessionInformation> getAllSessions(Object principal, boolean includeExpiredSessions) {
    return sessionRepository.findByPrincipalName(principal.toString()).values().stream()
        .map(this::information)
        .filter(Objects::nonNull)
        .filter(info -> includeExpiredSessions || !info.isExpired())
        .toList();
  }

  @Override
  public SessionInformation getSessionInformation(String id) {
    S session = sessionRepository.findById(id);
    return session == null ? null : information(session);
  }

  private SessionInformation information(S session) {
    String identity =
        session.getAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME);
    if (identity == null) {
      return null;
    }
    return new DurableInformation(identity, session);
  }

  @Override
  public void refreshLastRequest(String id) {
    S session = sessionRepository.findById(id);
    if (session != null) {
      session.setLastAccessedTime(java.time.Instant.now());
      sessionRepository.save(session);
    }
  }

  @Override
  public void registerNewSession(String id, Object principal) {
    S session = sessionRepository.findById(id);
    if (session == null) {
      throw new AuthenticationServiceException("Session reservation unavailable");
    }
    session.setAttribute(
        FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, principal.toString());
    sessionRepository.save(session);
  }

  @Override
  public void removeSessionInformation(String id) {
    sessionRepository.deleteById(id);
  }

  public void retainAfterPasswordChange(String id, String identity) {
    if (sessionRepository instanceof DurableSessionRepository<?> durable) {
      durable.retainAfterPasswordChange(id, identity);
    }
  }

  public boolean retainAfterProviderUnlink(
      String id,
      String identity,
      String previousVersion,
      String newVersion,
      Set<SignInMethod> remainingMethods) {
    return sessionRepository instanceof DurableSessionRepository<?> durable
        && durable.retainAfterProviderUnlink(
            id, identity, previousVersion, newVersion, remainingMethods);
  }

  public Active metadata(String id) {
    S session = sessionRepository.findById(id);
    if (session instanceof DurableSessionRepository<?>.LiveSession live) {
      return live.activeMetadata();
    }
    Active active = session == null ? null : session.getAttribute(DurableSessionRepository.ACTIVE);
    return active == null
        ? null
        : new Active(
            session.getId(),
            active.handle(),
            active.identity(),
            active.device(),
            active.created(),
            active.series());
  }

  private final class DurableInformation extends SessionInformation {
    private final transient S session;

    private DurableInformation(String identity, S session) {
      // Spring Security verlangt hier Date; intern und in JDBC bleiben Zeitpunkte Instant.
      super(identity, session.getId(), Date.from(session.getLastAccessedTime()));
      this.session = session;
      if (Boolean.TRUE.equals(session.getAttribute(DurableSessionRepository.EXPIRED))) {
        super.expireNow();
      }
    }

    @Override
    public void expireNow() {
      session.setAttribute(DurableSessionRepository.EXPIRED, Boolean.TRUE);
      sessionRepository.save(session);
      // Die alte UI bleibt bis zu ihrer abgewehrten Anfrage erhalten. Ihre eigentliche
      // Invalidierung bzw. der Timeout räumt Laufzeitobjekte und Listener auf.
      super.expireNow();
    }
  }
}
