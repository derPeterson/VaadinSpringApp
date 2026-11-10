package de.derpeterson.app.security.session;

import static de.derpeterson.app.helper.logging.LogSafety.safeFailure;

import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedHttpSession;
import de.derpeterson.app.security.DeviceRememberMeServices;
import de.derpeterson.app.security.SessionAdmissionService.Active;
import de.derpeterson.app.security.SignInMethod;
import de.derpeterson.app.security.TwoFactorFlow;
import de.derpeterson.app.security.mfa.TwoFactorAuthorization;
import jakarta.servlet.http.HttpSessionBindingEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.web.util.WebUtils;

/** Dauerhafter Sicherheitszustand; der Vaadin-Objektgraph bleibt genau einer Instanz zugeordnet. */
public final class DurableSessionRepository<S extends Session>
    implements FindByIndexNameSessionRepository<DurableSessionRepository<S>.LiveSession> {
  public static final String ACTIVE = DurableSessionRepository.class.getName() + ".active";
  public static final String EXPIRED = DurableSessionRepository.class.getName() + ".expired";
  private static final String CONTEXT =
      HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;
  private static final String CSRF =
      "org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository.CSRF_TOKEN";
  private static final Set<String> PERSISTED =
      Set.of(
          CONTEXT,
          CSRF,
          ACTIVE,
          EXPIRED,
          TwoFactorAuthorization.SESSION_PROOF,
          TwoFactorFlow.SESSION_ATTRIBUTE,
          TwoFactorFlow.INTRO_ACCEPTED,
          PRINCIPAL_NAME_INDEX_NAME,
          DeviceRememberMeServices.SERIES);
  private static final Logger logger = LoggerFactory.getLogger(DurableSessionRepository.class);
  private final FindByIndexNameSessionRepository<S> backingSessionRepository;
  private final UserDetailsService userDetailsService;
  private final Supplier<String> rememberKey;
  private final Supplier<Duration> sessionTimeoutSupplier;
  private final Map<String, RuntimeState> runtimeSessionsById = new ConcurrentHashMap<>();

  /** Ein bereits geladener Request darf nach paralleler Abmeldung keine Rechte mehr erhalten. */
  public static final class MissingSessionException extends IllegalStateException {
    private MissingSessionException() {
      super("Session no longer exists");
    }
  }

  private static final class RuntimeState {
    private String id;
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    private RuntimeState(String id) {
      this.id = id;
      attributes.put(WebUtils.SESSION_MUTEX_ATTRIBUTE, new Object());
    }
  }

  public DurableSessionRepository(
      FindByIndexNameSessionRepository<S> backingSessionRepository,
      UserDetailsService userDetailsService,
      Supplier<String> rememberKey,
      Supplier<Duration> sessionTimeoutSupplier) {
    this.backingSessionRepository = backingSessionRepository;
    this.userDetailsService = userDetailsService;
    this.rememberKey = rememberKey;
    this.sessionTimeoutSupplier = sessionTimeoutSupplier;
  }

  @Override
  public LiveSession createSession() {
    S storedSession = backingSessionRepository.createSession();
    storedSession.setMaxInactiveInterval(sessionTimeoutSupplier.get());
    backingSessionRepository.save(storedSession);
    return wrap(storedSession);
  }

  @Override
  public LiveSession findById(String id) {
    RuntimeState known = runtimeSessionsById.get(id);
    if (known != null) {
      // Ein parallel laufender ID-Wechsel darf nicht als Sitzungsende bereinigt werden.
      synchronized (known) {
        return findStoredById(id);
      }
    }
    return findStoredById(id);
  }

  private LiveSession findStoredById(String id) {
    S storedSession = backingSessionRepository.findById(id);
    if (storedSession == null) {
      discardRuntime(id);
      return null;
    }
    try {
      Object authentication = storedSession.getAttribute(CONTEXT);
      if (authentication instanceof DurableAuthentication durable) {
        durable.restore(userDetailsService, rememberKey.get());
      } else if (authentication != null) {
        throw new IllegalArgumentException("Unexpected persisted authentication");
      }
      return wrap(storedSession);
    } catch (AuthenticationException | IllegalArgumentException | ConversionException failure) {
      logger.warn("Persisted session rejected; fresh sign-in required", safeFailure(failure));
      // Widerrufsnachweis bis zur HTTP-Invalidierung erhalten: alte RPCs brauchen eine
      // ausdrückliche 401 und dürfen nicht als neue anonyme Vaadin-Sitzung weiterlaufen.
      storedSession.removeAttribute(CONTEXT);
      storedSession.setAttribute(EXPIRED, Boolean.TRUE);
      backingSessionRepository.save(storedSession);
      return wrap(storedSession);
    }
  }

  private LiveSession wrap(S session) {
    RuntimeState state = runtimeSessionsById.computeIfAbsent(session.getId(), RuntimeState::new);
    return new LiveSession(session, state);
  }

  @Override
  public void save(LiveSession session) {
    synchronized (session.state) {
      // Ein anderer Request kann sich inzwischen abgemeldet haben. Der abschließende
      // Spring-Session-Commit darf die gelöschte Sitzung weder neu anlegen noch HTTP 500 auslösen.
      if (!session.refreshIfPresent()) {
        return;
      }
      backingSessionRepository.save(session.storedSession);
    }
  }

  /** Nur nach dem bestätigten eigenen Passwortwechsel; andere Sitzungen bleiben ungültig. */
  public void retainAfterPasswordChange(String id, String identity) {
    RuntimeState state = runtimeSessionsById.computeIfAbsent(id, RuntimeState::new);
    synchronized (state) {
      S session = backingSessionRepository.findById(state.id);
      if (session == null || Boolean.TRUE.equals(session.getAttribute(EXPIRED))) {
        return;
      }
      Object value = session.getAttribute(CONTEXT);
      if (value instanceof DurableAuthentication authentication
          && identity.equals(authentication.username())) {
        session.setAttribute(CONTEXT, authentication.withCurrentCredentials(userDetailsService));
        backingSessionRepository.save(session);
      }
    }
  }

  boolean retainAfterProviderUnlink(
      String id,
      String identity,
      String previousVersion,
      String newVersion,
      Set<SignInMethod> remainingMethods) {
    RuntimeState state = runtimeSessionsById.computeIfAbsent(id, RuntimeState::new);
    synchronized (state) {
      S session = backingSessionRepository.findById(state.id);
      if (session == null || Boolean.TRUE.equals(session.getAttribute(EXPIRED))) {
        return false;
      }
      Object value = session.getAttribute(CONTEXT);
      if (!(value instanceof DurableAuthentication authentication)
          || !identity.equals(authentication.username())
          || !previousVersion.equals(authentication.credentialVersion())
          || !remainingMethods.contains(authentication.signInMethod())
          || authentication.signInMethod() == SignInMethod.PORTAL
              && !authentication.hasPasswordProof()) {
        return false;
      }
      DurableAuthentication retained;
      try {
        retained = authentication.withCurrentCredentials(userDetailsService);
      } catch (AuthenticationException _) {
        return false;
      }
      if (!newVersion.equals(retained.credentialVersion())) {
        return false;
      }
      session.setAttribute(CONTEXT, retained);
      backingSessionRepository.save(session);
      return true;
    }
  }

  @Override
  public void deleteById(String id) {
    backingSessionRepository.deleteById(id);
    discardRuntime(id);
  }

  void discardRuntime(String id) {
    RuntimeState removed = runtimeSessionsById.remove(id);
    if (removed == null) {
      return;
    }
    Set<VaadinSession> unbound = new HashSet<>();
    removed.attributes.forEach((name, value) -> {
      if (value instanceof VaadinSession session && unbound.add(session)) {
        if (session.getSession() instanceof WrappedHttpSession wrapped) {
          // Vaadins Binding-Lifecycle erhält die UI bis zum Ende einer laufenden UIDL-Antwort.
          // Direktes fireSessionDestroy würde sie davor ablösen und den Metadatenaufbau stören.
          session.valueUnbound(new HttpSessionBindingEvent(wrapped.getHttpSession(), name));
        } else if (session.getService() != null) {
          session.getService().fireSessionDestroy(session);
        }
      }
    });
    removed.attributes.clear();
  }

  @Override
  public Map<String, LiveSession> findByIndexNameAndIndexValue(
      String indexName, String indexValue) {
    Map<String, LiveSession> liveSessionsById = new HashMap<>();
    backingSessionRepository
        .findByIndexNameAndIndexValue(indexName, indexValue)
        .forEach((sessionId, indexedStoredSession) -> {
          LiveSession liveSession = findById(sessionId);
          if (liveSession != null) {
            liveSessionsById.put(sessionId, liveSession);
          }
        });

    return liveSessionsById;
  }

  @Scheduled(fixedDelay = 60000)
  public void cleanExpiredRuntimeState() {
    for (String id : Set.copyOf(runtimeSessionsById.keySet())) {
      // Dieselbe Synchronisierung wie HTTP-Lookups schützt auch den periodischen Cleanup.
      findById(id);
    }
  }

  public final class LiveSession implements Session {
    private S storedSession;
    private final RuntimeState state;

    private LiveSession(S storedSession, RuntimeState state) {
      this.storedSession = storedSession;
      this.state = state;
    }

    private void current() {
      if (!storedSession.getId().equals(state.id)) {
        S replacement = backingSessionRepository.findById(state.id);
        if (replacement == null) {
          throw new MissingSessionException();
        }
        storedSession = replacement;
      }
    }

    private void fresh() {
      if (!refreshIfPresent()) {
        throw new MissingSessionException();
      }
    }

    private boolean refreshIfPresent() {
      S latest = backingSessionRepository.findById(state.id);
      if (latest == null) {
        return false;
      }
      storedSession = latest;
      return true;
    }

    Active activeMetadata() {
      synchronized (state) {
        if (!refreshIfPresent() || Boolean.TRUE.equals(storedSession.getAttribute(EXPIRED))) {
          return null;
        }
        Active active = storedSession.getAttribute(ACTIVE);
        if (active == null) {
          return null;
        }
        SignInMethod method =
            storedSession.getAttribute(CONTEXT) instanceof DurableAuthentication authentication
                ? authentication.signInMethod()
                : null;
        return new Active(
            state.id,
            active.handle(),
            active.identity(),
            active.device(),
            active.created(),
            active.series(),
            method);
      }
    }

    @Override
    public String getId() {
      synchronized (state) {
        return state.id;
      }
    }

    @Override
    public String changeSessionId() {
      synchronized (state) {
        current();
        String previous = state.id;
        String changed = storedSession.changeSessionId();
        backingSessionRepository.save(storedSession);
        state.id = changed;
        runtimeSessionsById.remove(previous, state);
        runtimeSessionsById.put(changed, state);
        return changed;
      }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String name) {
      synchronized (state) {
        current();
        if (!PERSISTED.contains(name)) {
          return (T) state.attributes.get(name);
        }
        // Sicherheitsattribute müssen auch Änderungen anderer bereits geladener Requests sehen.
        fresh();
        Object value = storedSession.getAttribute(name);
        if (CONTEXT.equals(name) && value instanceof DurableAuthentication authentication) {
          return (T) authentication.restore(userDetailsService, rememberKey.get());
        }
        return (T) value;
      }
    }

    @Override
    public Set<String> getAttributeNames() {
      synchronized (state) {
        current();
        Set<String> names = new HashSet<>(storedSession.getAttributeNames());
        names.addAll(state.attributes.keySet());
        return names;
      }
    }

    @Override
    public void setAttribute(String name, @Nullable Object value) {
      synchronized (state) {
        current();
        if (!PERSISTED.contains(name)) {
          if (value == null) {
            state.attributes.remove(name);
          } else {
            state.attributes.put(name, value);
          }
          return;
        }
        fresh();
        Object storedValue = value;
        if (CONTEXT.equals(name) && value instanceof SecurityContext context) {
          Authentication authentication = context.getAuthentication();
          storedValue =
              authentication == null
                  ? null
                  : DurableAuthentication.capture(authentication, userDetailsService);
        }
        if (storedValue == null) {
          storedSession.removeAttribute(name);
        } else {
          storedSession.setAttribute(name, storedValue);
        }
        backingSessionRepository.save(storedSession);
      }
    }

    @Override
    public void removeAttribute(String name) {
      setAttribute(name, null);
    }

    @Override
    public Instant getCreationTime() {
      synchronized (state) {
        current();
        return storedSession.getCreationTime();
      }
    }

    @Override
    public Instant getLastAccessedTime() {
      synchronized (state) {
        current();
        return storedSession.getLastAccessedTime();
      }
    }

    @Override
    public void setLastAccessedTime(Instant time) {
      synchronized (state) {
        current();
        fresh();
        // Ein verspätet abgeschlossener Request darf die Inaktivitätsfrist nicht zurücksetzen.
        if (time.isAfter(storedSession.getLastAccessedTime())) {
          storedSession.setLastAccessedTime(time);
        }
        backingSessionRepository.save(storedSession);
      }
    }

    @Override
    public Duration getMaxInactiveInterval() {
      synchronized (state) {
        current();
        return storedSession.getMaxInactiveInterval();
      }
    }

    @Override
    public void setMaxInactiveInterval(Duration interval) {
      synchronized (state) {
        current();
        fresh();
        storedSession.setMaxInactiveInterval(interval);
        backingSessionRepository.save(storedSession);
      }
    }

    @Override
    public boolean isExpired() {
      synchronized (state) {
        current();
        return storedSession.isExpired();
      }
    }
  }
}
