package de.derpeterson.app.config;

import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.service.ConfigService;
import java.util.concurrent.Executor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@RequiredArgsConstructor
public class ThreadPoolConfig {

  private final ConfigService configService;

  @Bean(name = "emailTaskExecutor")
  public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    int poolSize = configService.getInteger(ConfigEntry.EMAIL_QUEUE_POOL_SIZE);
    int queueCapacity = configService.getInteger(ConfigEntry.EMAIL_QUEUE_CAPACITY);

    executor.setCorePoolSize(poolSize);
    executor.setMaxPoolSize(poolSize);
    executor.setQueueCapacity(queueCapacity);
    executor.setThreadNamePrefix("EmailSender-");
    executor.initialize();
    return executor;
  }
}
