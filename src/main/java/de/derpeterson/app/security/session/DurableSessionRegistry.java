package de.derpeterson.app.security.session;

import de.derpeterson.app.security.SessionAdmissionService.Active;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;

/**
 * Auch Reservierungen ohne veröffentlichten SecurityContext zählen sofort zum bestehenden Limit.
 */
public final class DurableSessionRegistry<S extends Session> implements SessionRegistry {
  private final FindByIndexNameSessionRepository<S> repository;

  public DurableSessionRegistry(FindByIndexNameSessionRepository<S> repository) {
    this.repository = repository;
  }

  @Override
  public List<Object> getAllPrincipals() {
    throw new UnsupportedOperationException("Use account-scoped session lookup");
  }

  @Override
  public List<SessionInformation> getAllSessions(Object principal, boolean includeExpiredSessions) {
    return repository.findByPrincipalName(principal.toString()).values().stream()
        .map(this::information)
        .filter(Objects::nonNull)
        .filter(info -> includeExpiredSessions || !info.isExpired())
        .toList();
  }

  @Override
  public SessionInformation getSessionInformation(String id) {
    S session = repository.findById(id);
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
    S session = repository.findById(id);
    if (session != null) {
      session.setLastAccessedTime(java.time.Instant.now());
      repository.save(session);
    }
  }

  @Override
  public void registerNewSession(String id, Object principal) {
    S session = repository.findById(id);
    if (session == null) {
      throw new AuthenticationServiceException("Session reservation unavailable");
    }
    session.setAttribute(
        FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, principal.toString());
    repository.save(session);
  }

  @Override
  public void removeSessionInformation(String id) {
    repository.deleteById(id);
  }

  public void retainAfterPasswordChange(String id, String identity) {
    if (repository instanceof DurableSessionRepository<?> durable) {
      durable.retainAfterPasswordChange(id, identity);
    }
  }

  public Active metadata(String id) {
    S session = repository.findById(id);
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
      repository.save(session);
      // Die alte UI bleibt bis zu ihrer abgewehrten Anfrage erhalten. Ihre eigentliche
      // Invalidierung bzw. der Timeout räumt Laufzeitobjekte und Listener auf.
      super.expireNow();
    }
  }
}
