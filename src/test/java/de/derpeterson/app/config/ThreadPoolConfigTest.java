package de.derpeterson.app.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.EmailQueueWorkerPool;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class ThreadPoolConfigTest {
  @ParameterizedTest
  @ValueSource(ints = {0, -1})
  void invalidWorkerCountsAreRejectedBeforeAnyThreadStarts(int count) {
    ConfigService config = mock(ConfigService.class);
    when(config.getInteger(ConfigEntry.EMAIL_QUEUE_POOL_SIZE)).thenReturn(count);
    ThreadPoolConfig threadPoolConfig = new ThreadPoolConfig(config);
    assertThrows(IllegalArgumentException.class, () -> threadPoolConfig.taskExecutor());
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 3})
  void poolUsesItsStartupSizeAndDoesNotUseTheLiveBatchCapacity(int count) throws Exception {
    ConfigService config = mock(ConfigService.class);
    when(config.getInteger(ConfigEntry.EMAIL_QUEUE_POOL_SIZE)).thenReturn(count);
    ThreadPoolExecutor executor = new ThreadPoolConfig(config).taskExecutor();
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
    InheritableThreadLocal<String> inherited = new InheritableThreadLocal<String>();
    inherited.set("fixture-caller-context");
    ConfigService config = mock(ConfigService.class);
    when(config.getInteger(ConfigEntry.EMAIL_QUEUE_POOL_SIZE)).thenReturn(1);
    ThreadPoolExecutor executor = new ThreadPoolConfig(config).taskExecutor();
    try {
      assertNull(executor.submit(inherited::get).get());
    } finally {
      inherited.remove();
      executor.shutdownNow();
    }
  }

  @Test
  void springInjectsTheNamedExecutorAndClosesItWithTheWorkerComponent() {
    ConfigService config = mock(ConfigService.class);
    when(config.getInteger(ConfigEntry.EMAIL_QUEUE_POOL_SIZE)).thenReturn(2);
    AtomicReference<String> thread = new AtomicReference<String>();
    java.util.concurrent.ThreadPoolExecutor executor;
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
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
