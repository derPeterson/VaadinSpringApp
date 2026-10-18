package de.derpeterson.app.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.EmailQueueWorkerPool;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class ThreadPoolConfigTest {
  @ParameterizedTest
  @ValueSource(ints = {0, -1})
  void invalidWorkerCountsAreRejectedBeforeAnyThreadStarts(int count) {
    var config = mock(ConfigService.class);
    when(config.getInteger(ConfigEntry.EMAIL_QUEUE_POOL_SIZE)).thenReturn(count);
    assertThrows(IllegalArgumentException.class, () -> new ThreadPoolConfig(config).taskExecutor());
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 3})
  void poolUsesItsStartupSizeAndDoesNotUseTheLiveBatchCapacity(int count) throws Exception {
    var config = mock(ConfigService.class);
    when(config.getInteger(ConfigEntry.EMAIL_QUEUE_POOL_SIZE)).thenReturn(count);
    var executor = new ThreadPoolConfig(config).taskExecutor();
    try {
      assertEquals(count, executor.getCorePoolSize());
      assertEquals(count, executor.getMaximumPoolSize());
      assertEquals(count, executor.getQueue().remainingCapacity());
      assertTrue(executor.submit(() -> Thread.currentThread().isDaemon()).get());
      assertTrue(
          executor.submit(() -> Thread.currentThread().getName()).get().startsWith("EmailSender-"));
      verify(config, never()).getInteger(ConfigEntry.EMAIL_QUEUE_CAPACITY);
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void workerThreadsDoNotInheritCallerContext() throws Exception {
    var inherited = new InheritableThreadLocal<String>();
    inherited.set("fixture-caller-context");
    var config = mock(ConfigService.class);
    when(config.getInteger(ConfigEntry.EMAIL_QUEUE_POOL_SIZE)).thenReturn(1);
    var executor = new ThreadPoolConfig(config).taskExecutor();
    try {
      assertNull(executor.submit(inherited::get).get());
    } finally {
      inherited.remove();
      executor.shutdownNow();
    }
  }

  @Test
  void springInjectsTheNamedExecutorAndClosesItWithTheWorkerComponent() {
    var config = mock(ConfigService.class);
    when(config.getInteger(ConfigEntry.EMAIL_QUEUE_POOL_SIZE)).thenReturn(2);
    var thread = new AtomicReference<String>();
    java.util.concurrent.ThreadPoolExecutor executor;
    try (var context = new AnnotationConfigApplicationContext()) {
      context.registerBean(ConfigService.class, () -> config);
      context.register(ThreadPoolConfig.class, EmailQueueWorkerPool.class);
      context.refresh();
      executor =
          context.getBean("emailTaskExecutor", java.util.concurrent.ThreadPoolExecutor.class);
      context
          .getBean(EmailQueueWorkerPool.class)
          .process(List.of(1L), id -> thread.set(Thread.currentThread().getName()));
      assertTrue(thread.get().startsWith("EmailSender-"));
    }
    assertTrue(executor.isTerminated());
  }
}
