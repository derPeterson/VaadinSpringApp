package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.EmailQueueEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.EmailStatus;
import de.derpeterson.app.model.enums.EmailVerificationStatus;
import de.derpeterson.app.model.enums.RegistrationChannel;
import de.derpeterson.app.model.enums.TokenStatus;
import de.derpeterson.app.repository.AdminNotificationAttemptRepository;
import de.derpeterson.app.repository.EmailQueueRepository;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.repository.VerificationTokenRepository;
import de.derpeterson.app.security.AdminRolePolicy;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Reale Konten, Tokens und Mailqueue mit eigener H2, ohne SMTP oder Anwendungsstart. */
@SpringJUnitConfig({
  UserServicePersistenceTest.Config.class,
  AdminAccountVerificationPersistenceTest.MailConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AdminAccountVerificationPersistenceTest {
  @Autowired private AdminAccountMailService adminAccountMailService;
  @Autowired private UserService userService;
  @Autowired private VerificationService verificationService;
  @Autowired private PasswordResetService passwordResetService;
  @Autowired private AdminRolePolicy adminRolePolicy;
  @Autowired private UserRepository userRepository;
  @Autowired private VerificationTokenRepository verificationTokenRepository;
  @Autowired private EmailQueueRepository emailQueueRepository;
  @Autowired private TransactionTemplate transactionTemplate;

  @BeforeEach
  void clearAccountFixtures() {
    transactionTemplate.executeWithoutResult(transactionStatus -> {
      emailQueueRepository.deleteAll();
      verificationTokenRepository.deleteAll();
      userRepository.deleteAll();
    });
    reset(adminRolePolicy, passwordResetService);
  }

  private UserEntity newAccount() {
    return UserEntity.builder()
        .firstName("Test")
        .lastName("User")
        .email("pending@example.test")
        .enabled(true)
        .preferredLocale(Locale.ENGLISH)
        .roleEntities(List.of())
        .build();
  }

  private UserEntity readEditableAccount(Long accountUserId) {
    return transactionTemplate.execute(transactionStatus -> {
      UserEntity editableUserEntity = userRepository.findById(accountUserId).orElseThrow();
      editableUserEntity.getRoleEntities().size();
      return editableUserEntity;
    });
  }

  @Test
  void defaultAdministrativeAccountRequiresProofAndInitialMail() throws IOException {
    UserEntity accountUserEntity = newAccount();
    adminAccountMailService.createUser(accountUserEntity, "KnownPassword!", false);

    UserEntity storedUserEntity = userRepository.findById(accountUserEntity.getId()).orElseThrow();
    assertEquals(RegistrationChannel.ADMINISTRATION, storedUserEntity.getRegistrationChannel());
    assertEquals(EmailVerificationStatus.PENDING, storedUserEntity.getEmailVerificationStatus());
    assertFalse(
        userRepository.findSecurityUser(storedUserEntity.getEmail()).getFirst().isEnabled());
    assertTrue(
        userRepository
            .findSecurityUser(storedUserEntity.getEmail())
            .getFirst()
            .getPendingEmailVerification());
    assertEquals(1, emailQueueRepository.count());

    String verificationToken = verificationTokenRepository.findAll().getFirst().getToken();
    assertTrue(verificationService.validateToken(verificationToken));
    assertEquals(
        EmailVerificationStatus.VERIFIED,
        userRepository
            .findById(storedUserEntity.getId())
            .orElseThrow()
            .getEmailVerificationStatus());
    assertTrue(userRepository.findSecurityUser(storedUserEntity.getEmail()).getFirst().isEnabled());
  }

  @Test
  void uncheckedCreationRequiresSuperadminAndQueuesNoVerification() throws IOException {
    UserEntity accountUserEntity = newAccount();
    adminAccountMailService.createUser(accountUserEntity, "KnownPassword!", true);

    verify(adminRolePolicy).requireSuperadmin();
    assertEquals(
        EmailVerificationStatus.NOT_CHECKED,
        userRepository
            .findById(accountUserEntity.getId())
            .orElseThrow()
            .getEmailVerificationStatus());
    assertTrue(
        userRepository.findSecurityUser(accountUserEntity.getEmail()).getFirst().isEnabled());
    assertEquals(0, emailQueueRepository.count());
  }

  @Test
  void ordinaryAdminCannotBypassVerification() {
    doThrow(new AccessDeniedException("Superadmin required"))
        .when(adminRolePolicy)
        .requireSuperadmin();

    assertThrows(
        AccessDeniedException.class,
        () -> adminAccountMailService.createUser(newAccount(), "KnownPassword!", true));
    assertEquals(0, userRepository.count());
    assertEquals(0, emailQueueRepository.count());
  }

  @Test
  void duplicateResendKeepsOneMailAndOneActiveLink() throws IOException {
    UserEntity accountUserEntity = newAccount();
    adminAccountMailService.createUser(accountUserEntity, "KnownPassword!", false);

    assertFalse(
        adminAccountMailService.resendVerification(
            accountUserEntity.getId(), accountUserEntity.getEmail()));
    assertEquals(1, emailQueueRepository.count());
    assertEquals(1, verificationTokenRepository.count());
  }

  @Test
  void developerCreationRequiresSuperadminAndDisablesMailWithoutGrantingRights()
      throws IOException {
    UserEntity developmentUserEntity = newAccount();
    developmentUserEntity.setDevelopmentAccount(true);
    adminAccountMailService.createUser(developmentUserEntity, "KnownPassword!", false);

    verify(adminRolePolicy).requireSuperadmin();
    UserEntity storedUserEntity =
        userRepository.findById(developmentUserEntity.getId()).orElseThrow();
    assertTrue(storedUserEntity.isDevelopmentAccount());
    assertEquals(
        EmailVerificationStatus.NOT_CHECKED, storedUserEntity.getEmailVerificationStatus());
    assertFalse(
        adminAccountMailService.resendVerification(
            storedUserEntity.getId(), storedUserEntity.getEmail()));
    assertFalse(
        adminAccountMailService.sendPasswordReset(
            storedUserEntity.getId(), storedUserEntity.getEmail()));
    assertEquals(0, emailQueueRepository.count());
    assertEquals(0, verificationTokenRepository.count());
  }

  @Test
  void ordinaryAdminCannotCreateDeveloperAccountEvenWithVerificationSelected() {
    doThrow(new AccessDeniedException("Superadmin required"))
        .when(adminRolePolicy)
        .requireSuperadmin();
    UserEntity developmentUserEntity = newAccount();
    developmentUserEntity.setDevelopmentAccount(true);

    assertThrows(
        AccessDeniedException.class,
        () -> adminAccountMailService.createUser(developmentUserEntity, "KnownPassword!", false));
    assertEquals(0, userRepository.count());
  }

  @Test
  void changedAddressInvalidatesOldLinksAndOnlyUsesSavedAddress() throws IOException {
    UserEntity accountUserEntity = newAccount();
    adminAccountMailService.createUser(accountUserEntity, "KnownPassword!", false);
    String oldVerificationToken = verificationTokenRepository.findAll().getFirst().getToken();
    UserEntity editedUserEntity = readEditableAccount(accountUserEntity.getId());
    Long expectedVersion = editedUserEntity.getVersion();
    editedUserEntity.setEmail("new@example.test");

    userService.updateAdminUser(editedUserEntity, expectedVersion, "");

    assertFalse(verificationService.validateToken(oldVerificationToken));
    assertEquals(
        TokenStatus.INACTIVE,
        verificationTokenRepository.findByToken(oldVerificationToken).orElseThrow().getStatus());
    assertEquals(0, emailQueueRepository.count());
    assertThrows(
        AccessDeniedException.class,
        () ->
            adminAccountMailService.resendVerification(
                accountUserEntity.getId(), "pending@example.test"));
    assertTrue(
        adminAccountMailService.resendVerification(accountUserEntity.getId(), "new@example.test"));
    String queuedRecipientEmail =
        transactionTemplate.execute(transactionStatus ->
            emailQueueRepository.findAll().getFirst().getUserEntity().getEmail());
    assertEquals("new@example.test", queuedRecipientEmail);
  }

  @Test
  void consumedLinkDoesNotReportSuccessForAnUnverifiedReplacementAddress() throws IOException {
    UserEntity accountUserEntity = newAccount();
    adminAccountMailService.createUser(accountUserEntity, "KnownPassword!", false);
    String previousVerificationToken = verificationTokenRepository.findAll().getFirst().getToken();
    assertTrue(verificationService.validateToken(previousVerificationToken));

    UserEntity editedUserEntity = readEditableAccount(accountUserEntity.getId());
    editedUserEntity.setEmail("replacement@example.test");
    userService.updateAdminUser(editedUserEntity, editedUserEntity.getVersion(), "");

    assertFalse(verificationService.validateToken(previousVerificationToken));
    assertEquals(
        EmailVerificationStatus.PENDING,
        userRepository
            .findById(accountUserEntity.getId())
            .orElseThrow()
            .getEmailVerificationStatus());
    assertFalse(userRepository.findSecurityUser("replacement@example.test").getFirst().isEnabled());
  }

  @ParameterizedTest
  @CsvSource({
    "PENDING,ADMINISTRATION",
    "VERIFIED,PORTAL",
    "VERIFIED,GOOGLE",
    "NOT_CHECKED,APPLICATION_START",
    "NOT_CHECKED,ADMINISTRATION"
  })
  void addressChangeQueuesProofUnlessTheStoredAccountIsExempt(
      EmailVerificationStatus originalVerificationStatus, RegistrationChannel registrationChannel)
      throws IOException {
    UserEntity accountUserEntity = newAccount();
    boolean verificationExempt = originalVerificationStatus == EmailVerificationStatus.NOT_CHECKED;
    accountUserEntity.setRegistrationChannel(registrationChannel);
    accountUserEntity.setEmailVerificationStatus(originalVerificationStatus);
    accountUserEntity.setPassword("isolated-fixture-hash");
    userService.saveUser(accountUserEntity);
    if (originalVerificationStatus == EmailVerificationStatus.PENDING) {
      assertTrue(verificationService.sendVerificationEmailByUser(accountUserEntity));
    }
    UserEntity editedUserEntity = readEditableAccount(accountUserEntity.getId());
    editedUserEntity.setEmail("replacement@example.test");

    adminAccountMailService.updateUser(editedUserEntity, editedUserEntity.getVersion(), "");

    UserEntity storedUserEntity = userRepository.findById(accountUserEntity.getId()).orElseThrow();
    assertEquals("replacement@example.test", storedUserEntity.getEmail());
    assertEquals(registrationChannel, storedUserEntity.getRegistrationChannel());
    assertEquals(
        verificationExempt ? EmailVerificationStatus.NOT_CHECKED : EmailVerificationStatus.PENDING,
        storedUserEntity.getEmailVerificationStatus());
    assertEquals(verificationExempt ? 0 : 1, emailQueueRepository.count());
    assertEquals(
        verificationExempt,
        userRepository.findSecurityUser(storedUserEntity.getEmail()).getFirst().isEnabled());
    if (!verificationExempt) {
      assertEquals(
          "replacement@example.test", emailQueueRepository.findAll().getFirst().getRecipient());
      assertFalse(
          adminAccountMailService.resendVerification(
              storedUserEntity.getId(), storedUserEntity.getEmail()));
      assertEquals(1, emailQueueRepository.count());
    }
  }

  @Test
  void unrelatedEditDoesNotRequestAnotherVerificationMail() throws IOException {
    UserEntity accountUserEntity = newAccount();
    adminAccountMailService.createUser(accountUserEntity, "KnownPassword!", false);
    UserEntity editedUserEntity = readEditableAccount(accountUserEntity.getId());
    editedUserEntity.setFirstName("Updated");
    editedUserEntity.setEmail("PENDING@EXAMPLE.TEST");
    long previousTokenCount = verificationTokenRepository.count();

    adminAccountMailService.updateUser(editedUserEntity, editedUserEntity.getVersion(), "");

    assertEquals(1, emailQueueRepository.count());
    assertEquals(previousTokenCount, verificationTokenRepository.count());
    assertEquals(
        "Updated", userRepository.findById(accountUserEntity.getId()).orElseThrow().getFirstName());
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void unresolvedOldAddressMailDoesNotBlockTheReplacementAddress(boolean hasFixedRecipient)
      throws IOException {
    UserEntity accountUserEntity = newAccount();
    adminAccountMailService.createUser(accountUserEntity, "KnownPassword!", false);
    Long unresolvedQueueId =
        transactionTemplate.execute(transactionStatus -> {
          EmailQueueEntity queuedEmailEntity = emailQueueRepository.findAll().getFirst();
          queuedEmailEntity.setStatus(EmailStatus.IN_PROGRESS);
          if (!hasFixedRecipient) {
            queuedEmailEntity.setRecipient(null);
          }
          return emailQueueRepository.saveAndFlush(queuedEmailEntity).getId();
        });
    UserEntity editedUserEntity = readEditableAccount(accountUserEntity.getId());
    editedUserEntity.setEmail("replacement@example.test");

    adminAccountMailService.updateUser(editedUserEntity, editedUserEntity.getVersion(), "");

    EmailQueueEntity unresolvedEmailEntity =
        emailQueueRepository.findById(unresolvedQueueId).orElseThrow();
    assertEquals(EmailStatus.IN_PROGRESS, unresolvedEmailEntity.getStatus());
    assertEquals("pending@example.test", unresolvedEmailEntity.getRecipient());
    assertEquals(2, emailQueueRepository.count());
    EmailQueueEntity replacementEmailEntity =
        emailQueueRepository.findAll().stream()
            .filter(queueEntry -> !queueEntry.getId().equals(unresolvedQueueId))
            .findFirst()
            .orElseThrow();
    assertEquals(EmailStatus.PENDING, replacementEmailEntity.getStatus());
    assertEquals("replacement@example.test", replacementEmailEntity.getRecipient());
  }

  @Test
  void mailPreparationFailureRollsBackAddressTokensAndQueueTogether() throws IOException {
    UserEntity accountUserEntity = newAccount();
    adminAccountMailService.createUser(accountUserEntity, "KnownPassword!", false);
    String previousVerificationToken = verificationTokenRepository.findAll().getFirst().getToken();
    UserEntity editedUserEntity = readEditableAccount(accountUserEntity.getId());
    editedUserEntity.setEmail("replacement@example.test");

    // Die fehlende Sprachressource erzeugt eine echte IOException vor der Queueaufnahme.
    try (MockedStatic<CustomI18NProvider> localeProvider = mockStatic(CustomI18NProvider.class)) {
      localeProvider
          .when(CustomI18NProvider::getCurrentLocale)
          .thenReturn(Locale.forLanguageTag("zz"));
      assertThrows(
          IOException.class,
          () ->
              adminAccountMailService.updateUser(
                  editedUserEntity, editedUserEntity.getVersion(), ""));
    }

    assertEquals(
        "pending@example.test",
        userRepository.findById(accountUserEntity.getId()).orElseThrow().getEmail());
    assertEquals(1, emailQueueRepository.count());
    assertEquals(1, verificationTokenRepository.count());
    assertEquals(
        TokenStatus.ACTIVE,
        verificationTokenRepository
            .findByToken(previousVerificationToken)
            .orElseThrow()
            .getStatus());
  }

  @Test
  void changingTheAddressOfABlockedAccountDoesNotQueueAnActivation() throws IOException {
    UserEntity accountUserEntity = newAccount();
    adminAccountMailService.createUser(accountUserEntity, "KnownPassword!", false);
    UserEntity editedUserEntity = readEditableAccount(accountUserEntity.getId());
    editedUserEntity.setEmail("replacement@example.test");
    editedUserEntity.setAdministrativelyBlocked(true);

    adminAccountMailService.updateUser(editedUserEntity, editedUserEntity.getVersion(), "");

    assertEquals(0, emailQueueRepository.count());
    assertFalse(userRepository.findById(accountUserEntity.getId()).orElseThrow().isEnabled());
  }

  @Test
  void blockedAccountCannotBeActivatedByOldMailOrAdministrativeResend() throws IOException {
    UserEntity accountUserEntity = newAccount();
    adminAccountMailService.createUser(accountUserEntity, "KnownPassword!", false);
    String verificationToken = verificationTokenRepository.findAll().getFirst().getToken();
    UserEntity editedUserEntity = readEditableAccount(accountUserEntity.getId());
    editedUserEntity.setAdministrativelyBlocked(true);
    userService.updateAdminUser(editedUserEntity, editedUserEntity.getVersion(), "");

    assertFalse(verificationService.validateToken(verificationToken));
    assertFalse(
        adminAccountMailService.resendVerification(
            editedUserEntity.getId(), editedUserEntity.getEmail()));
    assertFalse(
        adminAccountMailService.sendPasswordReset(
            editedUserEntity.getId(), editedUserEntity.getEmail()));
  }

  @Test
  void resetMailRequiresVerifiedOrExemptPasswordAccountAndDoesNotChangePassword()
      throws IOException {
    UserEntity accountUserEntity = newAccount();
    adminAccountMailService.createUser(accountUserEntity, "KnownPassword!", false);
    assertFalse(
        adminAccountMailService.sendPasswordReset(
            accountUserEntity.getId(), accountUserEntity.getEmail()));
    verificationService.validateToken(verificationTokenRepository.findAll().getFirst().getToken());
    String previousPasswordHash =
        userRepository.findById(accountUserEntity.getId()).orElseThrow().getPassword();
    when(passwordResetService.sendPasswordResetEmail(accountUserEntity.getEmail()))
        .thenReturn(true);

    assertTrue(
        adminAccountMailService.sendPasswordReset(
            accountUserEntity.getId(), accountUserEntity.getEmail()));
    assertFalse(
        adminAccountMailService.sendPasswordReset(
            accountUserEntity.getId(), accountUserEntity.getEmail()));
    verify(passwordResetService).sendPasswordResetEmail(accountUserEntity.getEmail());
    assertEquals(
        previousPasswordHash,
        userRepository.findById(accountUserEntity.getId()).orElseThrow().getPassword());
  }

  @Configuration
  static class MailConfig {
    @Bean
    ConfigService configService() {
      ConfigService configService = mock(ConfigService.class);
      when(configService.getString(ConfigEntry.VERIFICATION_TOKEN_VALID_DURATION))
          .thenReturn("PT1H");
      when(configService.getString(ConfigEntry.SERVICE_NAME)).thenReturn("Test Service");
      when(configService.getString(ConfigEntry.BASE_URL)).thenReturn("https://example.test/");
      when(configService.getString(ConfigEntry.COPYRIGHT_TEXT)).thenReturn("Test {year}");
      return configService;
    }

    @Bean
    EmailQueueService emailQueueService(
        ConfigService configService,
        EmailQueueRepository emailQueueRepository,
        UserRepository userRepository,
        PlatformTransactionManager transactionManager) {
      return new EmailQueueService(
          configService,
          emailQueueRepository,
          mock(EmailService.class),
          userRepository,
          transactionManager,
          mock(AdminNotificationAttemptRepository.class),
          QueueTestWorkers.inline());
    }

    @Bean
    VerificationService verificationService(
        ConfigService configService,
        EmailQueueService emailQueueService,
        UserRepository userRepository,
        VerificationTokenRepository verificationTokenRepository) {
      MessageProperties messageProperties = mock(MessageProperties.class);
      when(messageProperties.getEmailVerificationSubject()).thenReturn("Verify account");
      return new VerificationService(
          messageProperties,
          verificationTokenRepository,
          userRepository,
          configService,
          emailQueueService);
    }

    @Bean
    PasswordResetService passwordResetService() {
      return mock(PasswordResetService.class);
    }

    @Bean
    AdminAccountMailService adminAccountMailService(
        UserService userService,
        UserRepository userRepository,
        AdminRolePolicy adminRolePolicy,
        VerificationService verificationService,
        PasswordResetService passwordResetService) {
      return new AdminAccountMailService(
          userService, userRepository, adminRolePolicy, verificationService, passwordResetService);
    }
  }
}
