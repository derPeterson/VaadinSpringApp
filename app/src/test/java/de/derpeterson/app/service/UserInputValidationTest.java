package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import de.derpeterson.app.helper.ui.ValidationHelper;
import de.derpeterson.app.model.EmailIdentity;
import de.derpeterson.app.model.PasswordResetTokenEntity;
import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.Gender;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.model.enums.TokenStatus;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.repository.PasswordResetTokenRepository;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.websocket.UserProfileBroadcaster;
import de.derpeterson.app.websocket.UserStatusBroadcaster;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserInputValidationTest {
  private UserRepository userRepository;
  private PasswordEncoder passwordEncoder;
  private UserStatusBroadcaster userStatusBroadcaster;
  private UserService userService;

  @BeforeEach
  void setup() {
    userRepository = mock(UserRepository.class);
    passwordEncoder = mock(PasswordEncoder.class);
    userStatusBroadcaster = mock(UserStatusBroadcaster.class);
    userService =
        new UserService(
            userRepository,
            userStatusBroadcaster,
            passwordEncoder,
            mock(UserProfileBroadcaster.class),
            mock(de.derpeterson.app.security.AdminRolePolicy.class),
            mock(org.springframework.context.ApplicationEventPublisher.class),
            mock(AccountDeletionService.class));
  }

  private UserEntity user() {
    return UserEntity.builder()
        .id(1L)
        .version(2L)
        .email("test@example.com")
        .firstName("Test")
        .lastName("User")
        .gender(Gender.OTHER)
        .birthDate(LocalDate.of(1990, 1, 1))
        .preferredLocale(Locale.ENGLISH)
        .password("old-hash")
        .roleEntities(List.of(RoleEntity.builder().name(RoleType.ROLE_USER).build()))
        .build();
  }

  private void invalidate(UserEntity testUserEntity, String field) {
    switch (field) {
      case "first" -> testUserEntity.setFirstName(" ");
      case "last" -> testUserEntity.setLastName(null);
      case "email" -> testUserEntity.setEmail("invalid-address");
      case "birth" -> testUserEntity.setBirthDate(LocalDate.now());
      case "roles" -> testUserEntity.setRoleEntities(null);
      case "role-null" -> testUserEntity.setRoleEntities(Arrays.asList((RoleEntity) null));
      case "role-name" -> testUserEntity.setRoleEntities(List.of(new RoleEntity()));
      default -> throw new AssertionError(field);
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"first", "last", "email", "birth", "roles", "role-null", "role-name"})
  void invalidSavedFieldsAreRejectedBeforeRepositoryOrEncoding(String field) {
    UserEntity candidate = user();
    invalidate(candidate, field);

    assertThrows(IllegalArgumentException.class, () -> userService.save(candidate));
    assertThrows(IllegalArgumentException.class, () -> userService.saveUser(candidate));
    verifyNoInteractions(userRepository, passwordEncoder, userStatusBroadcaster);
  }

  @ParameterizedTest
  @ValueSource(strings = {"first", "last", "email", "birth", "roles", "role-null", "role-name"})
  void invalidAdminInputNeverPartiallyMutatesTheManagedUser(String field) {
    UserEntity storedUserEntity = user();
    UserEntity edited = user();
    edited.setFirstName("Attempted edit");
    invalidate(edited, field);
    when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(storedUserEntity));

    assertThrows(
        IllegalArgumentException.class, () -> userService.updateAdminUser(edited, 2L, "Password!"));
    assertEquals("Test", storedUserEntity.getFirstName());
    assertEquals("old-hash", storedUserEntity.getPassword());
    verify(userRepository, never()).saveAndFlush(any());
    verifyNoInteractions(passwordEncoder, userStatusBroadcaster);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "short!A", "lowercase!", "NoSpecialCharacter"})
  void allRawPasswordEntrypointsUseExactlyTheExistingRuleWithoutEncodingOrMutation(
      String password) {
    assertFalse(ValidationHelper.isPasswordSecure(password));
    UserEntity testUserEntity = user();

    assertThrows(
        IllegalArgumentException.class, () -> userService.updatePassword(testUserEntity, password));
    assertEquals("old-hash", testUserEntity.getPassword());
    PasswordResetTokenRepository tokens = mock(PasswordResetTokenRepository.class);
    PasswordResetService reset =
        new PasswordResetService(
            null,
            tokens,
            userRepository,
            mock(ConfigService.class),
            passwordEncoder,
            null,
            mock(LoginAttemptService.class),
            mock(UserNotificationService.class));

    assertThrows(IllegalArgumentException.class, () -> reset.resetPassword("token", password));
    verifyNoInteractions(tokens, userRepository, passwordEncoder, userStatusBroadcaster);
  }

  @Test
  void invalidAdminPasswordIsCheckedBeforeAnyStoredFieldChanges() {
    UserEntity storedUserEntity = user();
    UserEntity edited = user();
    edited.setFirstName("Attempted edit");
    when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(storedUserEntity));

    assertThrows(
        IllegalArgumentException.class, () -> userService.updateAdminUser(edited, 2L, "weak"));
    assertEquals("Test", storedUserEntity.getFirstName());
    verify(userRepository, never()).saveAndFlush(any());
    verifyNoInteractions(passwordEncoder);
  }

  @Test
  void nullStatusLocaleAndSchedulerArgumentsAreRejectedBeforeLookupAndNotifications() {
    UserEntity targetUser = user();

    assertThrows(
        IllegalArgumentException.class, () -> userService.updateUserStatus(targetUser, null, true));
    assertThrows(
        IllegalArgumentException.class,
        () -> userService.updateUserLocale("test@example.com", null));
    assertThrows(IllegalArgumentException.class, () -> userService.updateLastActivity(null));
    assertThrows(
        IllegalArgumentException.class,
        () -> userService.updateScheduledStatus(1L, UserStatus.ABSENT, null));
    verifyNoInteractions(userRepository, passwordEncoder, userStatusBroadcaster);
  }

  @Test
  void emailExistenceUsesOnlyOneTargetedQueryWithCanonicalParameter() {
    when(userRepository.emailExistsForOtherUser("test@example.com", 7L)).thenReturn(true);

    assertTrue(userService.emailExistsForOtherUser(" TEST@EXAMPLE.COM ", 7L));
    verify(userRepository).emailExistsForOtherUser("test@example.com", 7L);
    verifyNoMoreInteractions(userRepository);
  }

  @Test
  void identityDoesNotDependOnTurkishJvmLocaleAndPasswordIsNotTrimmedBeforeEncoding() {
    Locale previous = Locale.getDefault();
    try {
      Locale.setDefault(Locale.forLanguageTag("tr-TR"));

      assertEquals("identity@example.com", EmailIdentity.canonicalize(" IDENTITY@EXAMPLE.COM "));
      assertEquals("identity@example.com", ValidationHelper.normalize(" IDENTITY@EXAMPLE.COM "));
    } finally {
      Locale.setDefault(previous);
    }
    String raw = "  Password!  ";
    when(passwordEncoder.encode(raw)).thenReturn("new-hash");
    UserEntity testUserEntity = user();
    userService.updatePassword(testUserEntity, raw);

    verify(passwordEncoder).encode(raw);
    assertEquals("new-hash", testUserEntity.getPassword());
  }

  @Test
  void validResetPasswordStillEncodesAndConsumesTheToken() {
    PasswordResetTokenRepository tokens = mock(PasswordResetTokenRepository.class);
    PasswordResetService reset =
        new PasswordResetService(
            null,
            tokens,
            userRepository,
            mock(ConfigService.class),
            passwordEncoder,
            null,
            mock(LoginAttemptService.class),
            mock(UserNotificationService.class));
    UserEntity testUserEntity = user();
    testUserEntity.setEnabled(true);
    when(tokens.findUserIdByToken("token")).thenReturn(Optional.of(testUserEntity.getId()));
    when(userRepository.lockVerificationUser(testUserEntity.getId()))
        .thenReturn(Optional.of(testUserEntity));
    PasswordResetTokenEntity token =
        new PasswordResetTokenEntity(
            null, "token", testUserEntity, LocalDateTime.now().plusHours(1), TokenStatus.ACTIVE);
    when(tokens.findByTokenAndStatus("token", TokenStatus.ACTIVE)).thenReturn(Optional.of(token));
    when(passwordEncoder.encode("Password!")).thenReturn("new-hash");

    assertTrue(reset.resetPassword("token", "Password!"));
    assertEquals("new-hash", testUserEntity.getPassword());
    assertEquals(TokenStatus.USED, token.getStatus());
    verify(userRepository).save(testUserEntity);
  }
}
