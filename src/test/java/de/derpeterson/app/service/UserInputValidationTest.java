package de.derpeterson.app.service;

import de.derpeterson.app.helper.ui.ValidationHelper;
import de.derpeterson.app.model.*;
import de.derpeterson.app.model.enums.*;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.repository.PasswordResetTokenRepository;
import de.derpeterson.app.websocket.UserStatusBroadcaster;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserInputValidationTest {
    private UserRepository repository;
    private PasswordEncoder encoder;
    private UserStatusBroadcaster broadcaster;
    private UserService service;

    @BeforeEach
    void setup() {
        repository = mock(UserRepository.class);
        encoder = mock(PasswordEncoder.class);
        broadcaster = mock(UserStatusBroadcaster.class);
        service = new UserService(repository, broadcaster, encoder);
    }

    private UserEntity user() {
        return UserEntity.builder().id(1L).version(2L).email("test@example.com").firstName("Test").lastName("User")
                .gender(Gender.OTHER).birthDate(LocalDate.of(1990, 1, 1)).preferredLocale(Locale.ENGLISH).password("old-hash")
                .roleEntities(List.of(RoleEntity.builder().name(RoleType.ROLE_USER).build())).build();
    }

    private void invalidate(UserEntity user, String field) {
        switch (field) {
            case "first" -> user.setFirstName(" ");
            case "last" -> user.setLastName(null);
            case "email" -> user.setEmail("invalid-address");
            case "gender" -> user.setGender(null);
            case "birth" -> user.setBirthDate(LocalDate.now());
            case "roles" -> user.setRoleEntities(null);
            case "role-null" -> user.setRoleEntities(Arrays.asList((RoleEntity) null));
            case "role-name" -> user.setRoleEntities(List.of(new RoleEntity()));
            default -> throw new AssertionError(field);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"first", "last", "email", "gender", "birth", "roles", "role-null", "role-name"})
    void invalidSavedFieldsAreRejectedBeforeRepositoryOrEncoding(String field) {
        UserEntity candidate = user();
        invalidate(candidate, field);
        assertThrows(IllegalArgumentException.class, () -> service.save(candidate));
        assertThrows(IllegalArgumentException.class, () -> service.saveUser(candidate));
        verifyNoInteractions(repository, encoder, broadcaster);
    }

    @ParameterizedTest
    @ValueSource(strings = {"first", "last", "email", "gender", "birth", "roles", "role-null", "role-name"})
    void invalidAdminInputNeverPartiallyMutatesTheManagedUser(String field) {
        UserEntity stored = user();
        UserEntity edited = user();
        edited.setFirstName("Attempted edit");
        invalidate(edited, field);
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(stored));
        assertThrows(IllegalArgumentException.class, () -> service.updateAdminUser(edited, 2L, "Password!"));
        assertEquals("Test", stored.getFirstName());
        assertEquals("old-hash", stored.getPassword());
        verify(repository, never()).saveAndFlush(any());
        verifyNoInteractions(encoder, broadcaster);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "short!A", "lowercase!", "NoSpecialCharacter"})
    void allRawPasswordEntrypointsUseExactlyTheExistingRuleWithoutEncodingOrMutation(String password) {
        assertFalse(ValidationHelper.isPasswordSecure(password));
        UserEntity user = user();
        assertThrows(IllegalArgumentException.class, () -> service.updatePassword(user, password));
        assertEquals("old-hash", user.getPassword());
        var tokens = mock(PasswordResetTokenRepository.class);
        var reset = new PasswordResetService(null, tokens, repository, null, encoder, null);
        assertThrows(IllegalArgumentException.class, () -> reset.resetPassword("token", password));
        verifyNoInteractions(tokens, repository, encoder, broadcaster);
    }

    @Test
    void invalidAdminPasswordIsCheckedBeforeAnyStoredFieldChanges() {
        UserEntity stored = user();
        UserEntity edited = user();
        edited.setFirstName("Attempted edit");
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(stored));
        assertThrows(IllegalArgumentException.class, () -> service.updateAdminUser(edited, 2L, "weak"));
        assertEquals("Test", stored.getFirstName());
        verify(repository, never()).saveAndFlush(any());
        verifyNoInteractions(encoder);
    }

    @Test
    void nullStatusLocaleAndSchedulerArgumentsAreRejectedBeforeLookupAndNotifications() {
        assertThrows(IllegalArgumentException.class, () -> service.updateUserStatus(user(), null, true));
        assertThrows(IllegalArgumentException.class, () -> service.updateUserLocale("test@example.com", null));
        assertThrows(IllegalArgumentException.class, () -> service.updateLastActivity(null));
        assertThrows(IllegalArgumentException.class, () -> service.updateScheduledStatus(1L, UserStatus.ABSENT, null));
        verifyNoInteractions(repository, encoder, broadcaster);
    }

    @Test
    void emailExistenceUsesOnlyOneTargetedQueryWithCanonicalParameter() {
        when(repository.emailExistsForOtherUser("test@example.com", 7L)).thenReturn(true);
        assertTrue(service.emailExistsForOtherUser(" TEST@EXAMPLE.COM ", 7L));
        verify(repository).emailExistsForOtherUser("test@example.com", 7L);
        verifyNoMoreInteractions(repository);
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
        when(encoder.encode(raw)).thenReturn("new-hash");
        UserEntity user = user();
        service.updatePassword(user, raw);
        verify(encoder).encode(raw);
        assertEquals("new-hash", user.getPassword());
    }

    @Test
    void validResetPasswordStillEncodesAndConsumesTheToken() {
        var tokens = mock(PasswordResetTokenRepository.class);
        var reset = new PasswordResetService(null, tokens, repository, null, encoder, null);
        UserEntity user = user();
        user.setEnabled(true);
        when(tokens.findUserIdByToken("token")).thenReturn(Optional.of(user.getId()));
        when(repository.lockVerificationUser(user.getId())).thenReturn(Optional.of(user));
        var token = new PasswordResetTokenEntity(null, "token", user, java.time.LocalDateTime.now().plusHours(1), TokenStatus.ACTIVE);
        when(tokens.findByTokenAndStatus("token", TokenStatus.ACTIVE)).thenReturn(Optional.of(token));
        when(encoder.encode("Password!")).thenReturn("new-hash");
        assertTrue(reset.resetPassword("token", "Password!"));
        assertEquals("new-hash", user.getPassword());
        assertEquals(TokenStatus.USED, token.getStatus());
        verify(repository).save(user);
    }
}
