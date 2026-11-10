package de.derpeterson.app.security.session;

import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.security.CustomUserDetailsService;
import de.derpeterson.app.security.DeviceRememberMeServices;
import de.derpeterson.app.service.ConfigService;
import java.time.Duration;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.session.config.annotation.web.http.EnableSpringHttpSession;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
@EnableSpringHttpSession
public class SessionPersistenceConfiguration {
  @Bean
  public JdbcIndexedSessionRepository durableJdbcSessions(
      DataSource sessionDataSource,
      PlatformTransactionManager transactionManager,
      ConfigService configService) {
    Duration startupTimeout = sessionTimeout(configService);
    // Projektvertrag: H2; feste idempotente DDL, keine fremden Tabellen oder Inhalte verändern.
    new ResourceDatabasePopulator(new ClassPathResource("db/durable-sessions-h2.sql"))
        .execute(sessionDataSource);
    TransactionTemplate sessionTransactionTemplate = new TransactionTemplate(transactionManager);
    sessionTransactionTemplate.setPropagationBehavior(
        TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    JdbcIndexedSessionRepository jdbcSessionRepository =
        new JdbcIndexedSessionRepository(
            new JdbcTemplate(sessionDataSource), sessionTransactionTemplate);
    jdbcSessionRepository.setDefaultMaxInactiveInterval(startupTimeout);
    SessionAttributeCodec sessionAttributeCodec = new SessionAttributeCodec();
    GenericConversionService sessionConversionService = new GenericConversionService();
    sessionConversionService.addConverter(
        Object.class, byte[].class, sessionAttributeCodec::encode);
    sessionConversionService.addConverter(
        byte[].class, Object.class, sessionAttributeCodec::decode);
    jdbcSessionRepository.setConversionService(sessionConversionService);
    return jdbcSessionRepository;
  }

  @Bean
  @Primary
  // JdbcSession ist paketprivat; der Wildcard hält den Backendtyp ohne Rawtype/Cast gekapselt.
  public DurableSessionRepository<?> sessionRepository(
      @Qualifier("durableJdbcSessions") JdbcIndexedSessionRepository jdbcSessionRepository,
      CustomUserDetailsService customUserDetailsService,
      ObjectProvider<DeviceRememberMeServices> deviceRememberMeServicesProvider,
      ConfigService configService) {
    return new DurableSessionRepository<>(
        jdbcSessionRepository,
        customUserDetailsService,
        () -> deviceRememberMeServicesProvider.getObject().getKey(),
        () -> sessionTimeout(configService));
  }

  static Duration sessionTimeout(ConfigService configService) {
    int sessionTimeoutSeconds = configService.getInteger(ConfigEntry.SESSION_TIMEOUT_SECONDS);
    if (sessionTimeoutSeconds < 1) {
      throw new IllegalArgumentException("Session timeout must be positive");
    }

    return Duration.ofSeconds(sessionTimeoutSeconds);
  }

  @Bean
  public DefaultCookieSerializer cookieSerializer() {
    DefaultCookieSerializer sessionCookieSerializer = new DefaultCookieSerializer();
    sessionCookieSerializer.setCookieName("JSESSIONID");
    sessionCookieSerializer.setUseHttpOnlyCookie(true);
    sessionCookieSerializer.setSameSite("Lax");
    return sessionCookieSerializer;
  }
}
