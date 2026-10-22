package de.derpeterson.app.security.session;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.MapSession;
import org.springframework.session.Session;

class DurableSessionRotationTest {
  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  @SuppressWarnings("unchecked")
  void parallelOldCookieCannotDestroyTheUiWhileFixationRotationIsPublishing(boolean cleanup)
      throws Exception {
    FindByIndexNameSessionRepository<MapSession> backend =
        mock(FindByIndexNameSessionRepository.class);
    Map<String, MapSession> rows = new ConcurrentHashMap<>();
    AtomicReference<String> oldId = new AtomicReference<>();
    AtomicBoolean rotating = new AtomicBoolean();
    CountDownLatch published = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    CountDownLatch readerStarted = new CountDownLatch(1);
    when(backend.createSession()).thenAnswer(call -> new MapSession());
    when(backend.findById(anyString()))
        .thenAnswer(
            call -> {
              MapSession value = rows.get(call.getArgument(0));
              return value == null ? null : new MapSession(value);
            });
    doAnswer(
            call -> {
              MapSession value = call.getArgument(0);
              if (rotating.get()) {
                rows.remove(oldId.get());
              }
              rows.put(value.getId(), new MapSession(value));
              if (rotating.get()) {
                published.countDown();
                assertTrue(release.await(10, TimeUnit.SECONDS));
              }
              return null;
            })
        .when(backend)
        .save(any(MapSession.class));
    DurableSessionRepository<MapSession> repository =
        new DurableSessionRepository<>(backend, mock(UserDetailsService.class), () -> "fixture");
    Session session = repository.createSession();
    oldId.set(session.getId());
    VaadinSession ui = mock(VaadinSession.class);
    VaadinService service = mock(VaadinService.class);
    when(ui.getService()).thenReturn(service);
    session.setAttribute("runtime-ui", ui);
    rotating.set(true);
    try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
      try {
        Future<String> changed = executor.submit(session::changeSessionId);
        assertTrue(published.await(10, TimeUnit.SECONDS));
        Future<Session> oldRequest =
            executor.submit(
                () -> {
                  readerStarted.countDown();
                  if (cleanup) {
                    repository.cleanExpiredRuntimeState();
                    return null;
                  }
                  return repository.findById(oldId.get());
                });
        assertTrue(readerStarted.await(10, TimeUnit.SECONDS));
        // Der Writer ist nachweislich vor Veröffentlichung seines Runtime-Zustands angehalten.
        assertThrows(TimeoutException.class, () -> oldRequest.get(200, TimeUnit.MILLISECONDS));
        release.countDown();
        String currentId = changed.get(10, TimeUnit.SECONDS);
        assertNotNull(currentId);
        assertNull(oldRequest.get(10, TimeUnit.SECONDS));
        assertSame(ui, repository.findById(currentId).getAttribute("runtime-ui"));
        verify(service, never()).fireSessionDestroy(ui);
      } finally {
        release.countDown();
      }
    }
  }
}
