package de.derpeterson.frontendreview;

import de.derpeterson.app.Application;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.UserNotificationEntity;
import de.derpeterson.app.model.UserNotificationRecipientEntity;
import de.derpeterson.app.model.enums.UserNotificationType;
import de.derpeterson.app.repository.UserNotificationRecipientRepository;
import de.derpeterson.app.repository.UserNotificationRepository;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.security.DeviceDescription;
import de.derpeterson.app.security.DeviceTokenRepository;
import de.derpeterson.app.security.SessionAdmissionService.Active;
import de.derpeterson.app.security.session.DurableSessionRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.session.Session;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Ausschließlich separat gestartete Sichtungsumgebung; kein Bestandteil des Anwendungs-JARs. */
public final class FrontendReviewApplication {
  private static final String IDENTITY = "reference-user@example.test";

  private FrontendReviewApplication() {}

  public static void main(String[] args) {
    SpringApplication.run(
        new Class<?>[] {Application.class, ReviewData.class, VisitorReviewExamples.class}, args);
  }

  @Configuration(proxyBeanMethods = false)
  public static class ReviewData {
    private final Environment environment;
    private final UserRepository users;
    private final UserNotificationRepository notifications;
    private final UserNotificationRecipientRepository recipients;
    private final PlatformTransactionManager transactions;
    private final DurableSessionRepository<?> sessions;
    private final DeviceTokenRepository tokens;

    public ReviewData(
        Environment environment,
        UserRepository users,
        UserNotificationRepository notifications,
        UserNotificationRecipientRepository recipients,
        PlatformTransactionManager transactions,
        DurableSessionRepository<?> sessions,
        DeviceTokenRepository tokens) {
      this.environment = environment;
      this.users = users;
      this.notifications = notifications;
      this.recipients = recipients;
      this.transactions = transactions;
      this.sessions = sessions;
      this.tokens = tokens;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void prepareExamples() throws IOException {
      if (!environment
          .getRequiredProperty("spring.datasource.url")
          .startsWith("jdbc:h2:mem:account-reference-review;")) {
        throw new IllegalStateException("Frontend review requires its own in-memory database");
      }
      new TransactionTemplate(transactions)
          .executeWithoutResult(
              status -> {
                UserEntity user = users.findByEmail(IDENTITY).orElseThrow();
                Instant now = Instant.now();
                for (int index = 13; index >= 0; index--) {
                  UserNotificationEntity notification = new UserNotificationEntity();
                  notification.setRequestKey("frontend-review:" + index);
                  notification.setType(
                      index % 3 == 0
                          ? UserNotificationType.ADMIN_ANNOUNCEMENT
                          : UserNotificationType.EMAIL_CHANGED);
                  if (notification.getType() == UserNotificationType.ADMIN_ANNOUNCEMENT) {
                    notification.setTitle(
                        index == 0
                            ? "Wartungsfenster am Wochenende"
                            : "Hinweis zum Benutzerportal");
                    notification.setBody(
                        "Synthetisches Beispiel: Am Samstag steht das Portal vorübergehend nicht zur Verfügung. Vielen Dank für Ihr Verständnis.");
                    notification.setSender(user);
                  }
                  notification.setCreatedAt(now.minusSeconds(index * 3600L));
                  notification.setRecipientCount(1);
                  notifications.saveAndFlush(notification);
                  UserNotificationRecipientEntity recipient = new UserNotificationRecipientEntity();
                  recipient.setUser(user);
                  recipient.setNotification(notification);
                  if (index % 4 == 3) {
                    recipient.setReadAt(now);
                  }
                  recipients.save(recipient);
                }
              });
      String activeSeries = seedSavedLogin(new DeviceDescription("Android", "Firefox", true));
      seedSession(sessions, activeSeries);
      if (environment.getProperty("frontend.review.visitors", Boolean.class, false)) {
        seedSession(sessions, null);
        seedSession(sessions, null);
      }
      seedSavedLogin(new DeviceDescription("Windows", "Edge", false));
      Path readyFile = Path.of(environment.getRequiredProperty("frontend.review.ready-file"));
      if (!readyFile.toAbsolutePath().getParent().equals(Path.of("").toAbsolutePath())) {
        throw new IllegalStateException(
            "Review readiness file must stay in its own runtime folder");
      }
      Files.writeString(readyFile, "ready", StandardOpenOption.CREATE_NEW);
    }

    private String seedSavedLogin(DeviceDescription device) {
      String series = UUID.randomUUID().toString();
      tokens.createNewToken(
          new PersistentRememberMeToken(
              IDENTITY, series, UUID.randomUUID().toString(), new Date()));
      tokens.describe(series, IDENTITY, device, Instant.now());
      return series;
    }

    private <S extends Session> void seedSession(
        DurableSessionRepository<S> repository, String series) {
      DurableSessionRepository<S>.LiveSession session = repository.createSession();
      session.setMaxInactiveInterval(Duration.ofHours(12));
      session.setAttribute(DurableSessionRepository.PRINCIPAL_NAME_INDEX_NAME, IDENTITY);
      session.setAttribute(
          DurableSessionRepository.ACTIVE,
          new Active(
              session.getId(),
              UUID.randomUUID().toString(),
              IDENTITY,
              new DeviceDescription("Android", "Firefox", true),
              Instant.now(),
              series));
      repository.save(session);
    }
  }
}
