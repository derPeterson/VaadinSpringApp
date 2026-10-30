package de.derpeterson.frontendreview;

import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.VaadinServiceInitListener;
import de.derpeterson.app.model.EmailChangeTokenEntity;
import de.derpeterson.app.model.PasswordResetTokenEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.VerificationTokenEntity;
import de.derpeterson.app.model.enums.TokenStatus;
import de.derpeterson.app.repository.EmailChangeTokenRepository;
import de.derpeterson.app.repository.PasswordResetTokenRepository;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.repository.VerificationTokenRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Nur separat kompilierte, austauschbare Linkbeispiele; keine Auslieferung im Anwendungs-JAR. */
@Configuration(proxyBeanMethods = false)
public class VisitorReviewExamples {
  private final Environment environment;
  private final UserRepository users;
  private final PasswordResetTokenRepository resets;
  private final VerificationTokenRepository verifications;
  private final EmailChangeTokenRepository emails;
  private final PlatformTransactionManager transactions;

  public VisitorReviewExamples(
      Environment environment,
      UserRepository users,
      PasswordResetTokenRepository resets,
      VerificationTokenRepository verifications,
      EmailChangeTokenRepository emails,
      PlatformTransactionManager transactions) {
    this.environment = environment;
    this.users = users;
    this.resets = resets;
    this.verifications = verifications;
    this.emails = emails;
    this.transactions = transactions;
  }

  @Bean
  public VaadinServiceInitListener visitorReviewRoutes() {
    return event -> {
      if (environment.getProperty("frontend.review.visitors", Boolean.class, false)) {
        RouteConfiguration routes =
            RouteConfiguration.forRegistry(event.getSource().getRouter().getRegistry());
        routes.setRoute("images/visitor-review/maintenance", VisitorMaintenancePage.class);
        routes.setRoute("images/visitor-review/recovery", VisitorRecoveryPage.class);
      }
    };
  }

  @EventListener(ApplicationReadyEvent.class)
  @Order(0)
  public void prepare() throws IOException {
    if (!environment.getProperty("frontend.review.visitors", Boolean.class, false)) {
      return;
    }
    if (!environment
        .getRequiredProperty("spring.datasource.url")
        .startsWith("jdbc:h2:mem:account-reference-review;")) {
      throw new IllegalStateException("Visitor examples require the isolated in-memory database");
    }
    new TransactionTemplate(transactions)
        .executeWithoutResult(
            status -> {
              UserEntity user = users.findByEmail("reference-user@example.test").orElseThrow();
              reset(user, "review-reset-ready", LocalDateTime.now().plusDays(2));
              reset(user, "review-reset-expired", LocalDateTime.now().minusHours(1));
              verification(
                  user, "review-verified", TokenStatus.USED, LocalDateTime.now().plusDays(2));
              UserEntity pending =
                  UserEntity.builder()
                      .firstName("Mia")
                      .lastName("Beispiel")
                      .email("visitor-pending@example.test")
                      .password(user.getPassword())
                      .gender(user.getGender())
                      .birthDate(user.getBirthDate())
                      .enabled(false)
                      .roleEntities(new ArrayList<>(user.getRoleEntities()))
                      .preferredLocale(user.getPreferredLocale())
                      .build();
              users.saveAndFlush(pending);
              verification(
                  pending,
                  "review-verification-expired",
                  TokenStatus.EXPIRED,
                  LocalDateTime.now().minusHours(1));
              email(user, emailToken(false), LocalDateTime.now().plusDays(2));
              email(user, emailToken(true), LocalDateTime.now().minusHours(1));
            });
    Path folder = Path.of("www/images/visitor-review");
    Files.createDirectories(folder);
    Files.writeString(
        folder.resolve("links.json"),
        "{\"email-ready\":\"/confirm-email-change/"
            + emailToken(false)
            + "\",\"email-expired\":\"/confirm-email-change/"
            + emailToken(true)
            + "\"}");
  }

  private void reset(UserEntity user, String token, LocalDateTime expiry) {
    PasswordResetTokenEntity entry = new PasswordResetTokenEntity();
    entry.setUserEntity(user);
    entry.setToken(token);
    entry.setExpiryDate(expiry);
    resets.save(entry);
  }

  private void verification(
      UserEntity user, String token, TokenStatus status, LocalDateTime expiry) {
    VerificationTokenEntity entry = new VerificationTokenEntity();
    entry.setUserEntity(user);
    entry.setToken(token);
    entry.setStatus(status);
    entry.setExpiryDate(expiry);
    verifications.save(entry);
  }

  private void email(UserEntity user, String token, LocalDateTime expiry) {
    EmailChangeTokenEntity entry = new EmailChangeTokenEntity();
    entry.setUserEntity(user);
    entry.setDigest(digest(token));
    entry.setCredentialDigest(digest(user.getPassword()));
    entry.setOldEmail(user.getEmail());
    entry.setNewEmail("visitor-new-address@example.test");
    entry.setCreatedAt(LocalDateTime.now());
    entry.setExpiryDate(expiry);
    emails.save(entry);
  }

  private static String emailToken(boolean expired) {
    byte[] bytes = new byte[32];
    bytes[0] = (byte) (expired ? 92 : 91);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private static String digest(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException(impossible);
    }
  }
}
