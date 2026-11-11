package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.RegistrationChannel;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.repository.ExternalIdentityRepository;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.security.AdminRolePolicy;
import de.derpeterson.app.security.CustomUserDetailsService;
import de.derpeterson.app.security.DeviceTokenRepository;
import de.derpeterson.app.security.ExternalAuthenticationToken;
import de.derpeterson.app.security.ExternalIdentityEvidence;
import de.derpeterson.app.security.ExternalLoginConfiguration;
import de.derpeterson.app.security.ExternalLoginProperties;
import de.derpeterson.app.security.MaintenanceAccessException;
import de.derpeterson.app.security.SignInMethod;
import de.derpeterson.app.security.session.DurableAuthentication;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Eigene H2-Transaktionen prüfen Identitätszuordnung und echte persistierte Kontozustände. */
@SpringJUnitConfig(ExternalIdentityServicePersistenceTest.Config.class)
class ExternalIdentityServicePersistenceTest {
  @Configuration
  @Import({UserServicePersistenceTest.Config.class, ExternalIdentityService.class})
  static class Config {
    @Bean
    ExternalLoginConfiguration.Registrations clients() {
      ExternalLoginProperties properties = new ExternalLoginProperties();
      properties.getGoogle().setClientId("own-client");
      properties.getGoogle().setEnabled(true);
      properties.getGoogle().setClientSecret("own-secret");
      properties.getMicrosoft().setClientId("own-client");
      properties.getMicrosoft().setEnabled(true);
      properties.getMicrosoft().setClientSecret("own-secret");
      return new ExternalLoginConfiguration().externalRegistrations(properties);
    }

    @Bean
    SessionRegistry sessions() {
      return new SessionRegistryImpl();
    }

    @Bean
    DeviceTokenRepository remembered() {
      return mock(DeviceTokenRepository.class);
    }

    @Bean
    ConfigService externalConfig() {
      return mock(ConfigService.class);
    }

    @Bean
    VerificationService externalVerification() {
      return mock(VerificationService.class);
    }
  }

  @Autowired private ExternalIdentityService externalIdentityService;
  @Autowired private ExternalIdentityRepository identities;
  @Autowired private UserRepository userRepository;
  @Autowired private ConfigService configService;
  @Autowired private VerificationService verificationService;
  @Autowired private TransactionTemplate transactionTemplate;
  @Autowired private PlatformTransactionManager transactionManager;
  @Autowired private SessionRegistry sessionRegistry;
  @Autowired private DeviceTokenRepository remembered;
  @PersistenceContext private EntityManager entityManager;

  @BeforeEach
  void setup() throws IOException {
    reset(configService, verificationService, remembered);
    when(verificationService.sendVerificationEmailByUser(any())).thenReturn(true);
    transactionTemplate.executeWithoutResult(status -> {
      identities.deleteAll();
      userRepository.deleteAll();
      userRepository.flush();
      entityManager.createQuery("delete from RoleEntity").executeUpdate();
      for (RoleType type : RoleType.values()) {
        entityManager.persist(RoleEntity.builder().name(type).build());
      }
    });
  }

  private ExternalIdentityEvidence evidence(String provider, String subject, boolean trusted) {
    return new ExternalIdentityEvidence(
        provider,
        provider.equals("google")
            ? "https://accounts.google.com"
            : "https://login.microsoftonline.com/tenant/v2.0",
        subject,
        "person@example.test",
        trusted,
        "Alex",
        "Example",
        Instant.now());
  }

  private UserEntity register(ExternalIdentityEvidence identity) throws IOException {
    return externalIdentityService.register(
        identity, "person@example.test", "Alex", "Example", Locale.GERMAN);
  }

  @Test
  void verifiedIdentityCreatesOnlyUserRoleWithoutPasswordOrInventedProfileValues()
      throws IOException {
    UserEntity testUserEntity = register(evidence("google", "subject-one", true));
    UserEntity storedUserEntity = userRepository.findById(testUserEntity.getId()).orElseThrow();

    assertTrue(storedUserEntity.isEnabled());
    assertFalse(storedUserEntity.isVerificationPending());
    assertNull(storedUserEntity.getPassword());
    assertEquals(RegistrationChannel.GOOGLE, storedUserEntity.getRegistrationChannel());
    assertEquals(
        Set.of(SignInMethod.GOOGLE),
        externalIdentityService.availableSignInMethods(storedUserEntity.getId()));
    assertNull(storedUserEntity.getGender());
    assertNull(storedUserEntity.getBirthDate());
    assertEquals(List.of("google"), externalIdentityService.connected(testUserEntity.getId()));
    CustomUserDetailsService details = new CustomUserDetailsService(userRepository);

    assertEquals(
        List.of("ROLE_USER"),
        details.loadUserByUsername(storedUserEntity.getEmail()).getAuthorities().stream()
            .map(a -> a.getAuthority())
            .toList());
    assertThrows(
        UsernameNotFoundException.class,
        () ->
            transactionTemplate.execute(
                status -> details.loadPasswordUserByUsername(storedUserEntity.getEmail())));
    assertEquals(
        testUserEntity.getId(),
        externalIdentityService
            .resolve(evidence("google", "subject-one", true))
            .orElseThrow()
            .getId());
  }

  @Test
  void microsoftAndChangedContactNeedPortalVerificationBeforeLogin() throws IOException {
    UserEntity testUserEntity = register(evidence("microsoft", "subject-two", false));

    assertFalse(testUserEntity.isEnabled());
    assertTrue(testUserEntity.isVerificationPending());
    ExternalIdentityEvidence pendingIdentity = evidence("microsoft", "subject-two", false);

    assertTrue(
        externalIdentityService.resolve(pendingIdentity).orElseThrow().requiresEmailVerification());
    assertFalse(
        new CustomUserDetailsService(userRepository)
            .loadUserByUsername(testUserEntity.getEmail())
            .isEnabled());
    Long userId = testUserEntity.getId();
    String version = testUserEntity.getAccessVersion();
    ExternalIdentityEvidence additionalIdentity = evidence("google", "other", true);

    assertThrows(
        DisabledException.class,
        () -> externalIdentityService.link(userId, version, additionalIdentity));
    UserEntity changedContact =
        externalIdentityService.register(
            evidence("google", "changed-contact", true),
            "changed@example.test",
            "Alex",
            "Example",
            Locale.GERMAN);

    assertFalse(changedContact.isEnabled());
    assertTrue(changedContact.isVerificationPending());
  }

  @Test
  void matchingEmailNeverLinksAnotherIdentityOrCreatesAnotherAccount() throws IOException {
    register(evidence("google", "subject-one", true));
    ExternalIdentityEvidence microsoftIdentity = evidence("microsoft", "subject-two", false);
    ExternalIdentityEvidence differentIdentity = evidence("google", "subject-different", true);

    assertThrows(AccessDeniedException.class, () -> register(microsoftIdentity));
    assertThrows(AccessDeniedException.class, () -> register(differentIdentity));
    assertEquals(1, userRepository.count());
    assertEquals(1, identities.count());
  }

  @Test
  void lastMethodCannotBeRemovedAndUnlinkRevokesPersistedAuthentication() throws IOException {
    UserEntity testUserEntity = register(evidence("google", "one", true));
    Long userId = testUserEntity.getId();
    String version = testUserEntity.getAccessVersion();
    MockHttpServletRequest mockHttpRequest = new MockHttpServletRequest();

    assertThrows(
        AccessDeniedException.class,
        () -> externalIdentityService.unlink(mockHttpRequest, userId, version, "google"));
    externalIdentityService.link(
        testUserEntity.getId(),
        testUserEntity.getAccessVersion(),
        evidence("microsoft", "two", false));
    sessionRegistry.registerNewSession("own-active-session", testUserEntity.getEmail());
    sessionRegistry.registerNewSession("other-active-session", testUserEntity.getEmail());
    CustomUserDetailsService details = new CustomUserDetailsService(userRepository);
    DurableAuthentication durable =
        DurableAuthentication.capture(
            new ExternalAuthenticationToken(
                details.loadUserByUsername(testUserEntity.getEmail()), "google"),
            details);

    assertTrue(
        durable.restore(details, "test-key").getAuthentication()
            instanceof ExternalAuthenticationToken);
    assertTrue(durable.factors().isEmpty());
    externalIdentityService.unlink(
        new MockHttpServletRequest(),
        testUserEntity.getId(),
        testUserEntity.getAccessVersion(),
        "google");

    assertTrue(sessionRegistry.getSessionInformation("own-active-session").isExpired());
    assertTrue(sessionRegistry.getSessionInformation("other-active-session").isExpired());
    verify(remembered).removeUserTokens(testUserEntity.getEmail());
    assertNotEquals(
        testUserEntity.getAccessVersion(),
        userRepository.findById(testUserEntity.getId()).orElseThrow().getAccessVersion());
    assertThrows(AuthenticationServiceException.class, () -> durable.restore(details, "test-key"));
    assertEquals(List.of("microsoft"), externalIdentityService.connected(testUserEntity.getId()));
    assertEquals(
        Set.of(SignInMethod.MICROSOFT),
        externalIdentityService.availableSignInMethods(testUserEntity.getId()));
    assertEquals(
        RegistrationChannel.GOOGLE,
        userRepository.findById(userId).orElseThrow().getRegistrationChannel());
    ExternalIdentityEvidence additionalIdentity = evidence("google", "three", true);

    assertThrows(
        AccessDeniedException.class,
        () -> externalIdentityService.link(userId, version, additionalIdentity));
  }

  @Test
  void failedVerificationQueueRollsBackTheAccountAndIdentity() throws IOException {
    when(verificationService.sendVerificationEmailByUser(any()))
        .thenThrow(new IOException("Own queue unavailable"));

    assertThrows(IOException.class, () -> register(evidence("microsoft", "two", false)));
    assertEquals(0, userRepository.count());
    assertEquals(0, identities.count());
  }

  @Test
  void maintenancePreventsPublicExternalRegistration() {
    when(configService.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
    ExternalIdentityEvidence identity = evidence("google", "one", true);

    assertThrows(MaintenanceAccessException.class, () -> register(identity));
    assertEquals(0, userRepository.count());
  }

  @Test
  void disabledAccountCannotBeOpenedByItsConnectedIdentity() throws IOException {
    ExternalIdentityEvidence identity = evidence("google", "one", true);
    UserEntity testUserEntity = register(identity);
    transactionTemplate.executeWithoutResult(status -> {
      UserEntity changed = userRepository.findById(testUserEntity.getId()).orElseThrow();
      changed.setEnabled(false);
      userRepository.saveAndFlush(changed);
    });

    assertThrows(DisabledException.class, () -> externalIdentityService.resolve(identity));
  }

  @Test
  void concurrentFirstRegistrationCreatesExactlyOneLocalAccountAndIdentity() throws Exception {
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    try (ExecutorService executorService = Executors.newFixedThreadPool(2)) {
      List<Future<Boolean>> results = new ArrayList<>();
      for (int attempt = 0; attempt < 2; attempt++) {
        results.add(
            executorService.submit(() -> {
              ready.countDown();

              assertTrue(start.await(10, TimeUnit.SECONDS));
              try {
                register(evidence("google", "same-identity", true));
                return true;
              } catch (AccessDeniedException _) {
                return false;
              }
            }));
      }
      assertTrue(ready.await(10, TimeUnit.SECONDS));
      start.countDown();
      int succeeded = 0;
      for (Future<Boolean> result : results) {
        if (result.get(20, TimeUnit.SECONDS)) {
          succeeded++;
        }
      }
      assertEquals(1, succeeded);
      assertEquals(1, userRepository.count());
      assertEquals(1, identities.count());
    }
  }

  @Test
  void concurrentUnlinkCannotRemoveBothRemainingProviders() throws Exception {
    UserEntity testUserEntity = register(evidence("google", "one", true));
    externalIdentityService.link(
        testUserEntity.getId(),
        testUserEntity.getAccessVersion(),
        evidence("microsoft", "two", false));
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    try (ExecutorService executorService = Executors.newFixedThreadPool(2)) {
      List<Future<Boolean>> results = new ArrayList<>();
      for (String provider : List.of("google", "microsoft")) {
        results.add(
            executorService.submit(() -> {
              ready.countDown();

              assertTrue(start.await(10, TimeUnit.SECONDS));
              try {
                externalIdentityService.unlink(
                    new MockHttpServletRequest(),
                    testUserEntity.getId(),
                    testUserEntity.getAccessVersion(),
                    provider);
                return true;
              } catch (AccessDeniedException _) {
                return false;
              }
            }));
      }
      assertTrue(ready.await(10, TimeUnit.SECONDS));
      start.countDown();
      int succeeded = 0;
      for (Future<Boolean> result : results) {
        if (result.get(20, TimeUnit.SECONDS)) {
          succeeded++;
        }
      }
      assertEquals(1, succeeded);
      assertEquals(1, externalIdentityService.connected(testUserEntity.getId()).size());
    }
  }

  @Test
  void concurrentLinkCannotAssignOneIdentityToTwoDifferentAccounts() throws Exception {
    UserEntity firstUserEntity = register(evidence("google", "first-owner", true));
    ExternalIdentityEvidence secondIdentity =
        new ExternalIdentityEvidence(
            "google",
            "https://accounts.google.com",
            "second-owner",
            "second@example.test",
            true,
            "Second",
            "Owner",
            Instant.now());
    UserEntity secondUserEntity =
        externalIdentityService.register(
            secondIdentity, "second@example.test", "Second", "Owner", Locale.ENGLISH);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    try (ExecutorService executorService = Executors.newFixedThreadPool(2)) {
      List<Future<Boolean>> results = new ArrayList<>();
      for (UserEntity user : List.of(firstUserEntity, secondUserEntity)) {
        results.add(
            executorService.submit(() -> {
              ready.countDown();

              assertTrue(start.await(10, TimeUnit.SECONDS));
              try {
                externalIdentityService.link(
                    user.getId(), user.getAccessVersion(), evidence("microsoft", "shared", false));
                return true;
              } catch (AccessDeniedException | DataIntegrityViolationException _) {
                return false;
              }
            }));
      }
      assertTrue(ready.await(10, TimeUnit.SECONDS));
      start.countDown();
      int succeeded = 0;
      for (Future<Boolean> result : results) {
        if (result.get(20, TimeUnit.SECONDS)) {
          succeeded++;
        }
      }
      assertEquals(1, succeeded);
      assertTrue(
          externalIdentityService.belongsTo(
                  firstUserEntity.getId(), evidence("microsoft", "shared", false))
              ^ externalIdentityService.belongsTo(
                  secondUserEntity.getId(), evidence("microsoft", "shared", false)));
      assertEquals(3, identities.count());
    }
  }

  @Test
  void disabledProviderDoesNotCountAsTheLastUsableAlternative() throws Exception {
    UserEntity testUserEntity = register(evidence("google", "one", true));
    externalIdentityService.link(
        testUserEntity.getId(),
        testUserEntity.getAccessVersion(),
        evidence("microsoft", "two", false));
    ExternalLoginProperties properties = new ExternalLoginProperties();
    properties.getGoogle().setClientId("own-client");
    properties.getGoogle().setEnabled(true);
    properties.getGoogle().setClientSecret("own-secret");
    ExternalIdentityService restricted =
        new ExternalIdentityService(
            identities,
            userRepository,
            verificationService,
            configService,
            sessionRegistry,
            remembered,
            new ExternalLoginConfiguration().externalRegistrations(properties),
            transactionManager,
            new AdminRolePolicy(userRepository));

    assertThrows(
        AccessDeniedException.class,
        () ->
            transactionTemplate.executeWithoutResult(status ->
                restricted.unlink(
                    new MockHttpServletRequest(),
                    testUserEntity.getId(),
                    testUserEntity.getAccessVersion(),
                    "google")));
    assertEquals(2, externalIdentityService.connected(testUserEntity.getId()).size());
  }
}
