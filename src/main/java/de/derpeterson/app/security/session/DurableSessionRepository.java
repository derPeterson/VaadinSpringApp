package de.derpeterson.app.security.session;

import static de.derpeterson.app.helper.logging.LogSafety.safeFailure;

import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.security.DeviceRememberMeServices;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionException;
import org.springframework.scheduling.annotation.Scheduled;
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
          PRINCIPAL_NAME_INDEX_NAME,
          DeviceRememberMeServices.SERIES);
  private static final Logger logger = LoggerFactory.getLogger(DurableSessionRepository.class);
  private final FindByIndexNameSessionRepository<S> backend;
  private final UserDetailsService users;
  private final Supplier<String> rememberKey;
  private final Map<String, RuntimeState> runtime = new ConcurrentHashMap<>();

  private static final class RuntimeState {
    private String id;
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    private RuntimeState(String id) {
      this.id = id;
      attributes.put(WebUtils.SESSION_MUTEX_ATTRIBUTE, new Object());
    }
  }

  public DurableSessionRepository(
      FindByIndexNameSessionRepository<S> backend,
      UserDetailsService users,
      Supplier<String> rememberKey) {
    this.backend = backend;
    this.users = users;
    this.rememberKey = rememberKey;
  }

  @Override
  public LiveSession createSession() {
    S stored = backend.createSession();
    backend.save(stored);
    return wrap(stored);
  }

  @Override
  public LiveSession findById(String id) {
    RuntimeState known = runtime.get(id);
    if (known != null) {
      // Ein parallel laufender ID-Wechsel darf nicht als Sitzungsende bereinigt werden.
      synchronized (known) {
        return findStoredById(id);
      }
    }
    return findStoredById(id);
  }

  private LiveSession findStoredById(String id) {
    S stored = backend.findById(id);
    if (stored == null) {
      discardRuntime(id);
      return null;
    }
    try {
      Object authentication = stored.getAttribute(CONTEXT);
      if (authentication instanceof DurableAuthentication durable) {
        durable.restore(users, rememberKey.get());
      } else if (authentication != null) {
        throw new IllegalArgumentException("Unexpected persisted authentication");
      }
      return wrap(stored);
    } catch (AuthenticationException | IllegalArgumentException | ConversionException failure) {
      logger.warn("Persisted session rejected; fresh sign-in required", safeFailure(failure));
      // Widerrufsnachweis bis zur HTTP-Invalidierung erhalten: alte RPCs brauchen eine
      // ausdrückliche 401 und dürfen nicht als neue anonyme Vaadin-Sitzung weiterlaufen.
      stored.setAttribute(CONTEXT, null);
      stored.setAttribute(EXPIRED, Boolean.TRUE);
      backend.save(stored);
      return wrap(stored);
    }
  }

  private LiveSession wrap(S session) {
    RuntimeState state = runtime.computeIfAbsent(session.getId(), RuntimeState::new);
    return new LiveSession(session, state);
  }

  @Override
  public void save(LiveSession session) {
    synchronized (session.state) {
      session.fresh();
      backend.save(session.stored);
    }
  }

  /** Nur nach dem bestätigten eigenen Passwortwechsel; andere Sitzungen bleiben ungültig. */
  public void retainAfterPasswordChange(String id, String identity) {
    RuntimeState state = runtime.computeIfAbsent(id, RuntimeState::new);
    synchronized (state) {
      S session = backend.findById(state.id);
      if (session == null || Boolean.TRUE.equals(session.getAttribute(EXPIRED))) {
        return;
      }
      Object value = session.getAttribute(CONTEXT);
      if (value instanceof DurableAuthentication authentication
          && identity.equals(authentication.username())) {
        session.setAttribute(CONTEXT, authentication.withCurrentCredentials(users));
        backend.save(session);
      }
    }
  }

  @Override
  public void deleteById(String id) {
    backend.deleteById(id);
    discardRuntime(id);
  }

  void discardRuntime(String id) {
    RuntimeState removed = runtime.remove(id);
    if (removed == null) {
      return;
    }
    removed.attributes.values().stream()
        .filter(VaadinSession.class::isInstance)
        .map(VaadinSession.class::cast)
        .distinct()
        .forEach(
            session -> {
              if (session.getService() != null) {
                session.getService().fireSessionDestroy(session);
              }
            });
    removed.attributes.clear();
  }

  @Override
  public Map<String, LiveSession> findByIndexNameAndIndexValue(
      String indexName, String indexValue) {
    Map<String, LiveSession> result = new HashMap<>();
    backend
        .findByIndexNameAndIndexValue(indexName, indexValue)
        .forEach(
            (id, ignored) -> {
              LiveSession session = findById(id);
              if (session != null) {
                result.put(id, session);
              }
            });
    return result;
  }

  @Scheduled(fixedDelay = 60000)
  public void cleanExpiredRuntimeState() {
    for (String id : Set.copyOf(runtime.keySet())) {
      // Dieselbe Synchronisierung wie HTTP-Lookups schützt auch den periodischen Cleanup.
      findById(id);
    }
  }

  public final class LiveSession implements Session {
    private S stored;
    private final RuntimeState state;

    private LiveSession(S stored, RuntimeState state) {
      this.stored = stored;
      this.state = state;
    }

    private void current() {
      if (!stored.getId().equals(state.id)) {
        S replacement = backend.findById(state.id);
        if (replacement == null) {
          throw new IllegalStateException("Session no longer exists");
        }
        stored = replacement;
      }
    }

    private void fresh() {
      S latest = backend.findById(state.id);
      if (latest == null) {
        throw new IllegalStateException("Session no longer exists");
      }
      stored = latest;
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
        String changed = stored.changeSessionId();
        backend.save(stored);
        state.id = changed;
        runtime.remove(previous, state);
        runtime.put(changed, state);
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
        Object value = stored.getAttribute(name);
        if (CONTEXT.equals(name) && value instanceof DurableAuthentication authentication) {
          return (T) authentication.restore(users, rememberKey.get());
        }
        return (T) value;
      }
    }

    @Override
    public Set<String> getAttributeNames() {
      synchronized (state) {
        current();
        Set<String> names = new HashSet<>(stored.getAttributeNames());
        names.addAll(state.attributes.keySet());
        return names;
      }
    }

    @Override
    public void setAttribute(String name, Object value) {
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
          storedValue =
              context.getAuthentication() == null
                  ? null
                  : DurableAuthentication.capture(context.getAuthentication(), users);
        }
        stored.setAttribute(name, storedValue);
        backend.save(stored);
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
        return stored.getCreationTime();
      }
    }

    @Override
    public Instant getLastAccessedTime() {
      synchronized (state) {
        current();
        return stored.getLastAccessedTime();
      }
    }

    @Override
    public void setLastAccessedTime(Instant time) {
      synchronized (state) {
        current();
        fresh();
        // Ein verspätet abgeschlossener Request darf die Inaktivitätsfrist nicht zurücksetzen.
        if (time.isAfter(stored.getLastAccessedTime())) {
          stored.setLastAccessedTime(time);
        }
        backend.save(stored);
      }
    }

    @Override
    public Duration getMaxInactiveInterval() {
      synchronized (state) {
        current();
        return stored.getMaxInactiveInterval();
      }
    }

    @Override
    public void setMaxInactiveInterval(Duration interval) {
      synchronized (state) {
        current();
        fresh();
        stored.setMaxInactiveInterval(interval);
        backend.save(stored);
      }
    }

    @Override
    public boolean isExpired() {
      synchronized (state) {
        current();
        return stored.isExpired();
      }
    }
  }
}
