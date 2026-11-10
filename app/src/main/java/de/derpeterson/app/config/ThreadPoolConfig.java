package de.derpeterson.app.config;

import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.service.ConfigService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ThreadPoolConfig {
  private final ConfigService configService;

  @Bean(name = "emailTaskExecutor", destroyMethod = "shutdown")
  public ThreadPoolExecutor taskExecutor() {
    return newExecutor(configService.getInteger(ConfigEntry.EMAIL_QUEUE_POOL_SIZE));
  }

  public static ThreadPoolExecutor newExecutor(int poolSize) {
    if (poolSize < 1) {
      throw new IllegalArgumentException("Email worker pool size must be positive.");
    }
    // Keine vererbten Aufruferkontexte; blockierende Fremdbibliotheken dürfen das JVM-Ende nicht
    // verhindern.
    ThreadFactory workerThreadFactory =
        Thread.ofPlatform()
            .name("EmailSender-", 1)
            .daemon(true)
            .inheritInheritableThreadLocals(false)
            .factory();
    // Nur begrenzte Übergabe; die dauerhafte Warteschlange bleibt in der Datenbank.
    return new ThreadPoolExecutor(
        poolSize,
        poolSize,
        0,
        TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<>(poolSize),
        workerThreadFactory,
        new ThreadPoolExecutor.AbortPolicy());
  }
}
