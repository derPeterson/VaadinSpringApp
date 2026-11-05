package de.derpeterson.app.security.mfa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.SupportTicketEntity.Status;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.repository.SupportReplyRepository;
import de.derpeterson.app.repository.SupportTicketRepository;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.service.AccountSecurityMail;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.EmailQueueService;
import de.derpeterson.app.service.SupportTicketService;
import de.derpeterson.app.service.SupportTicketService.Reply;
import de.derpeterson.app.service.SupportTicketService.ReplyAuthor;
import de.derpeterson.app.service.SupportTicketService.Ticket;
import de.derpeterson.app.service.UserNotificationService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.support.TransactionTemplate;

@SpringJUnitConfig({
  TwoFactorStoreTest.Config.class,
  TwoFactorAuthorizationTest.Config.class,
  SupportTicketServiceTest.Config.class
})
class SupportTicketServiceTest {
  @Autowired SupportTicketService service;
  @Autowired TwoFactorAuthorization authorization;
  @Autowired TwoFactorStore factors;
  @Autowired UserRepository users;
  @Autowired TransactionTemplate transaction;
  @Autowired EmailQueueService queue;
  @Autowired UserNotificationService notifications;
  @Autowired TwoFactorStoreTest.TestClock clock;
  @PersistenceContext EntityManager entityManager;
  private UserEntity user;
  private MockHttpServletRequest owner;
  private MockHttpServletRequest administrator;
  private MockHttpServletRequest otherAdministrator;

  @BeforeEach
  void setup() {
    clock.now.set(java.time.Instant.parse("2026-11-02T12:00:00Z"));
    notifications =
        org.springframework.test.util.AopTestUtils.getUltimateTargetObject(notifications);
    queue = org.springframework.test.util.AopTestUtils.getUltimateTargetObject(queue);
    reset(queue, notifications);
    user = create(RoleType.ROLE_USER);
    owner = TwoFactorAuthorizationTest.requestFor(user.getEmail());
    administrator = confirmedAdmin();
    otherAdministrator = confirmedAdmin();
  }

  @Test
  void ownRequestDoesNotGrantPortalAccessAndNeverExposesInternalNotes() {
    Ticket ticket = service.createRecovery(owner, "Ich benötige Hilfe.");
    verify(notifications).recordRecoveryRequest();
    assertThrows(IllegalStateException.class, () -> service.createRecovery(owner, "Doppelt"));
    MockHttpServletRequest foreign =
        TwoFactorAuthorizationTest.requestFor(create(RoleType.ROLE_USER).getEmail());
    assertTrue(service.list(foreign, false).isEmpty());
    assertThrows(
        AccessDeniedException.class, () -> service.conversation(foreign, ticket.id(), false));
    assertThrows(AccessDeniedException.class, () -> service.list(owner, true));
    Ticket claimed = service.claim(administrator, ticket.id(), ticket.version());
    Ticket changed =
        service.reply(
            administrator, claimed.id(), claimed.version(), "Interne Prüfung", true, true);
    assertEquals(1, service.conversation(owner, ticket.id(), false).size());
    assertEquals(2, service.conversation(administrator, ticket.id(), true).size());
    assertThrows(
        AccessDeniedException.class,
        () -> service.reply(owner, changed.id(), changed.version(), "Verbergen", true, false));
    service.reply(
        administrator, changed.id(), changed.version(), "Öffentliche Antwort", false, true);
    List<Reply> visible = service.conversation(owner, ticket.id(), false);
    assertEquals(
        List.of(ReplyAuthor.REQUESTER, ReplyAuthor.SUPPORT),
        visible.stream().map(Reply::author).toList());
    assertFalse(authorization.state(authorization.owner(owner)).enrolled());
  }

  @Test
  void adminReplyToOwnTicketRetainsSupportRole() {
    Ticket ticket = service.createRecovery(administrator, "Ich benötige Hilfe.");
    Ticket claimed = service.claim(administrator, ticket.id(), ticket.version());
    Ticket answered =
        service.reply(
            administrator, claimed.id(), claimed.version(), "Ich prüfe die Anfrage.", false, true);
    service.reply(
        administrator, answered.id(), answered.version(), "Danke für die Antwort.", false, false);

    assertEquals(
        List.of(ReplyAuthor.REQUESTER, ReplyAuthor.SUPPORT, ReplyAuthor.REQUESTER),
        service.conversation(administrator, ticket.id(), false).stream()
            .map(Reply::author)
            .toList());
  }

  @Test
  void blankRequestAndBlankReplyDoNotChangeTheTicketVersion() {
    assertThrows(IllegalArgumentException.class, () -> service.createRecovery(owner, " "));
    assertTrue(service.list(owner, false).isEmpty());

    Ticket ticket = service.createRecovery(owner, "Hilfe bitte.");
    Ticket claimed = service.claim(administrator, ticket.id(), ticket.version());
    assertThrows(
        IllegalArgumentException.class,
        () -> service.reply(administrator, claimed.id(), claimed.version(), " ", false, true));
    service.reply(administrator, claimed.id(), claimed.version(), "Ich prüfe das.", false, true);
    assertEquals(2, service.conversation(owner, ticket.id(), false).size());
  }

  @Test
  void requesterCanCloseOwnTicketAndAdministratorCanFinalizeItWithoutReopening() {
    Ticket open = service.createRecovery(owner, "Hilfe bitte.");
    MockHttpServletRequest foreign =
        TwoFactorAuthorizationTest.requestFor(create(RoleType.ROLE_USER).getEmail());
    assertThrows(
        AccessDeniedException.class, () -> service.closeOwn(foreign, open.id(), open.version()));
    Ticket resolved = service.closeOwn(owner, open.id(), open.version());
    assertEquals(Status.CLOSED_BY_REQUESTER, resolved.status());
    assertThrows(
        AccessDeniedException.class,
        () -> service.reply(owner, resolved.id(), resolved.version(), "Nachtrag", false, false));
    assertThrows(
        AccessDeniedException.class,
        () ->
            service.reply(
                administrator, resolved.id(), resolved.version(), "Nachtrag", false, true));
    assertThrows(
        AccessDeniedException.class,
        () -> service.sendInstructions(administrator, resolved.id(), resolved.version()));
    assertEquals(Status.OPEN, service.createRecovery(owner, "Ein neues Anliegen.").status());
    Ticket closed = service.close(administrator, resolved.id(), resolved.version());
    assertEquals(Status.CLOSED, closed.status());
    assertThrows(
        AccessDeniedException.class,
        () -> service.reply(administrator, closed.id(), closed.version(), "Nachtrag", false, true));
    assertEquals(1, service.conversation(owner, closed.id(), false).size());
  }

  @Test
  void competingClaimAndStaleUpdatesCannotOverwriteAnAssignee() {
    Ticket ticket = service.createRecovery(owner, "Hilfe bitte.");
    Ticket claimed = service.claim(administrator, ticket.id(), ticket.version());
    assertThrows(
        OptimisticLockingFailureException.class,
        () -> service.claim(otherAdministrator, ticket.id(), ticket.version()));
    assertThrows(
        IllegalStateException.class,
        () -> service.claim(otherAdministrator, claimed.id(), claimed.version()));
    assertThrows(
        AccessDeniedException.class,
        () -> service.close(otherAdministrator, claimed.id(), claimed.version()));
  }

  @Test
  void parallelClaimSucceedsForExactlyOneAdministrator() throws Exception {
    Ticket ticket = service.createRecovery(owner, "Gemeinsame Anfrage.");
    java.util.concurrent.CountDownLatch ready = new java.util.concurrent.CountDownLatch(2);
    java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
    try (java.util.concurrent.ExecutorService executor =
        java.util.concurrent.Executors.newFixedThreadPool(2)) {
      java.util.function.Function<MockHttpServletRequest, Boolean> claim =
          actor -> {
            ready.countDown();
            try {
              assertTrue(start.await(10, java.util.concurrent.TimeUnit.SECONDS));
              service.claim(actor, ticket.id(), ticket.version());
              return true;
            } catch (OptimisticLockingFailureException | IllegalStateException expected) {
              return false;
            } catch (InterruptedException interrupted) {
              Thread.currentThread().interrupt();
              throw new IllegalStateException(interrupted);
            }
          };
      var first = executor.submit(() -> claim.apply(administrator));
      var second = executor.submit(() -> claim.apply(otherAdministrator));
      assertTrue(ready.await(10, java.util.concurrent.TimeUnit.SECONDS));
      start.countDown();
      org.junit.jupiter.api.Assertions.assertNotEquals(
          first.get(10, java.util.concurrent.TimeUnit.SECONDS),
          second.get(10, java.util.concurrent.TimeUnit.SECONDS));
    }
  }

  @Test
  void restrictedFirstFactorCanAskForHelpButCannotReadAdministrationOrExceedLimits() {
    UserEntity required = create(RoleType.ROLE_ADMIN);
    MockHttpServletRequest firstFactor = TwoFactorAuthorizationTest.requestFor(required.getEmail());
    assertFalse(authorization.sessionConfirmed(firstFactor));
    assertThrows(IllegalArgumentException.class, () -> service.createRecovery(firstFactor, " "));
    assertThrows(
        IllegalArgumentException.class,
        () -> service.createRecovery(firstFactor, "x".repeat(2001)));
    for (int attempt = 0; attempt < 3; attempt++) {
      Ticket ticket = service.createRecovery(firstFactor, "Verlorener Authenticator.");
      assertFalse(authorization.sessionConfirmed(firstFactor));
      assertThrows(AccessDeniedException.class, () -> service.list(firstFactor, true));
      Ticket claimed = service.claim(administrator, ticket.id(), ticket.version());
      Ticket closed = service.close(administrator, claimed.id(), claimed.version());
      assertThrows(
          AccessDeniedException.class,
          () -> service.claim(administrator, closed.id(), closed.version()));
    }
    assertThrows(IllegalStateException.class, () -> service.createRecovery(firstFactor, "Zu oft"));
  }

  @Test
  void preparedInstructionsUseVerifiedStoredContactAndNeverUnlockAnything() {
    Ticket ticket =
        service.createRecovery(owner, "Bitte schicken Sie Hilfe an fremd@example.test.");
    Ticket claimed = service.claim(administrator, ticket.id(), ticket.version());
    String preview = service.instructionPreview(administrator, claimed.id());
    assertTrue(preview.contains("Ersatzcode"));
    Ticket sent = service.sendInstructions(administrator, claimed.id(), claimed.version());
    verify(queue).addAddressedNotification(any(), eq(user.getEmail()), any(), any());
    assertTrue(sent.instructionSent());
    assertThrows(
        AccessDeniedException.class,
        () -> service.sendInstructions(administrator, sent.id(), sent.version()));
    service.close(administrator, sent.id(), sent.version());
    assertFalse(authorization.state(authorization.owner(owner)).enrolled());
  }

  @Test
  void queueFailureKeepsInstructionsUnsentAndLeavesConversationUnchanged() {
    Ticket ticket = service.createRecovery(owner, "Hilfe.");
    Ticket claimed = service.claim(administrator, ticket.id(), ticket.version());
    doThrow(new IllegalStateException("test queue failure"))
        .when(queue)
        .addAddressedNotification(any(), any(), any(), any());
    assertThrows(
        IllegalStateException.class,
        () -> service.sendInstructions(administrator, claimed.id(), claimed.version()));
    assertFalse(service.list(owner, false).getFirst().instructionSent());
    assertEquals(1, service.conversation(owner, claimed.id(), false).size());
  }

  private MockHttpServletRequest confirmedAdmin() {
    UserEntity admin = create(RoleType.ROLE_ADMIN);
    MockHttpServletRequest request = TwoFactorAuthorizationTest.requestFor(admin.getEmail());
    byte[] seed = Totp.newSecret();
    UUID revision = factors.initialize(admin.getId(), admin.getAccessVersion());
    List<String> codes =
        factors
            .enroll(
                admin.getId(),
                admin.getAccessVersion(),
                revision,
                seed,
                Totp.code(seed, clock.instant().getEpochSecond() / 30),
                false)
            .recoveryCodes();
    authorization.confirmSession(request, codes.getFirst());
    return request;
  }

  private UserEntity create(RoleType type) {
    return transaction.execute(
        status -> {
          RoleEntity role =
              entityManager
                  .createQuery("select r from RoleEntity r where r.name = :name", RoleEntity.class)
                  .setParameter("name", type)
                  .getResultStream()
                  .findFirst()
                  .orElse(null);
          if (role == null) {
            role = RoleEntity.builder().name(type).build();
            entityManager.persist(role);
          }
          return users.saveAndFlush(
              UserEntity.builder()
                  .email(UUID.randomUUID() + "@example.test")
                  .firstName("Support")
                  .lastName("Fixture")
                  .preferredLocale(Locale.GERMAN)
                  .enabled(true)
                  .roleEntities(List.of(role))
                  .build());
        });
  }

  @Configuration
  static class Config {
    @Bean
    EmailQueueService queue() {
      return mock(EmailQueueService.class);
    }

    @Bean
    UserNotificationService notifications() {
      return mock(UserNotificationService.class);
    }

    @Bean
    AccountSecurityMail templates() {
      ConfigService config = mock(ConfigService.class);
      when(config.getString(any()))
          .thenAnswer(call -> ((ConfigEntry) call.getArgument(0)).getDefaultValueString());
      when(config.getString(ConfigEntry.BASE_URL)).thenReturn("https://fixture.invalid/");
      return new AccountSecurityMail(config, new CustomI18NProvider());
    }

    @Bean
    SupportTicketService support(
        TwoFactorAuthorization authorization,
        SupportTicketRepository tickets,
        SupportReplyRepository replies,
        UserRepository users,
        EntityManagerFactory factory,
        UserNotificationService notifications,
        AccountSecurityMail templates,
        EmailQueueService queue) {
      return new SupportTicketService(
          authorization,
          tickets,
          replies,
          users,
          SharedEntityManagerCreator.createSharedEntityManager(factory),
          notifications,
          templates,
          queue);
    }
  }
}
