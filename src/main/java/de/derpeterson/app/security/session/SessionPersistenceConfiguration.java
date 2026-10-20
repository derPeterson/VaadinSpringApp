package de.derpeterson.app.security.session;

import de.derpeterson.app.security.CustomUserDetailsService;
import de.derpeterson.app.security.DeviceRememberMeServices;
import java.time.Duration;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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
      DataSource source,
      PlatformTransactionManager manager,
      @Value("${app.sessions.timeout-seconds:1800}") int timeoutSeconds) {
    if (timeoutSeconds < 1) {
      throw new IllegalArgumentException("Session timeout must be positive");
    }
    // Projektvertrag: H2; feste idempotente DDL, keine fremden Tabellen oder Inhalte verändern.
    new ResourceDatabasePopulator(new ClassPathResource("db/durable-sessions-h2.sql"))
        .execute(source);
    TransactionTemplate transaction = new TransactionTemplate(manager);
    transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    JdbcIndexedSessionRepository backend =
        new JdbcIndexedSessionRepository(new JdbcTemplate(source), transaction);
    backend.setDefaultMaxInactiveInterval(Duration.ofSeconds(timeoutSeconds));
    SessionAttributeCodec codec = new SessionAttributeCodec();
    GenericConversionService conversions = new GenericConversionService();
    conversions.addConverter(Object.class, byte[].class, codec::encode);
    conversions.addConverter(byte[].class, Object.class, codec::decode);
    backend.setConversionService(conversions);
    return backend;
  }

  @Bean
  @Primary
  // JdbcSession ist paketprivat; der Wildcard hält den Backendtyp ohne Rawtype/Cast gekapselt.
  public DurableSessionRepository<?> sessionRepository(
      @Qualifier("durableJdbcSessions") JdbcIndexedSessionRepository backend,
      CustomUserDetailsService users,
      ObjectProvider<DeviceRememberMeServices> remembered) {
    return new DurableSessionRepository<>(backend, users, () -> remembered.getObject().getKey());
  }

  @Bean
  public DefaultCookieSerializer cookieSerializer() {
    DefaultCookieSerializer cookie = new DefaultCookieSerializer();
    cookie.setCookieName("JSESSIONID");
    cookie.setUseHttpOnlyCookie(true);
    cookie.setSameSite("Lax");
    return cookie;
  }
}
