package de.derpeterson.app.service;

import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.websocket.UserStatusBroadcaster;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserStatusBroadcaster userStatusBroadcaster;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, userStatusBroadcaster, passwordEncoder);
    }

    @Test
    void testEmailExistsForOtherUser_NullEmail() {
        boolean result = userService.emailExistsForOtherUser(null, 1L);
        assertFalse(result);
    }

    @Test
    void testEmailExistsForOtherUser_EmptyEmail() {
        boolean result = userService.emailExistsForOtherUser("", 1L);
        assertFalse(result);
    }

    @Test
    void testEmailExistsForOtherUser_WhitespaceOnlyEmail() {
        boolean result = userService.emailExistsForOtherUser("   ", 1L);
        assertFalse(result);
    }

    @Test
    void testEmailExistsForOtherUser_NoMatchingUser() {
        when(userRepository.findAll()).thenReturn(Collections.emptyList());
        boolean result = userService.emailExistsForOtherUser("test@example.com", 1L);
        assertFalse(result);
    }

    @Test
    void testEmailExistsForOtherUser_AnotherUserWithMatchingEmail() {
        UserEntity user1 = UserEntity.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        UserEntity user2 = UserEntity.builder()
                .id(2L)
                .email("other@example.com")
                .build();

        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2));
        boolean result = userService.emailExistsForOtherUser("other@example.com", 1L);
        assertTrue(result);
    }

    @Test
    void testEmailExistsForOtherUser_CaseDifference() {
        UserEntity user1 = UserEntity.builder()
                .id(1L)
                .email("TEST@EXAMPLE.COM")
                .build();

        when(userRepository.findAll()).thenReturn(Collections.singletonList(user1));
        boolean result = userService.emailExistsForOtherUser("test@example.com", 2L);
        assertTrue(result);
    }

    @Test
    void testEmailExistsForOtherUser_WithOuterWhitespace() {
        UserEntity user1 = UserEntity.builder()
                .id(1L)
                .email("   TEST@EXAMPLE.COM   ")
                .build();

        when(userRepository.findAll()).thenReturn(Collections.singletonList(user1));
        boolean result = userService.emailExistsForOtherUser("test@example.com", 2L);
        assertTrue(result);
    }

    @Test
    void testEmailExistsForOtherUser_MatchBelongsToCurrent() {
        UserEntity user1 = UserEntity.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        when(userRepository.findAll()).thenReturn(Collections.singletonList(user1));
        boolean result = userService.emailExistsForOtherUser("test@example.com", 1L);
        assertFalse(result);
    }

    @Test
    void testEmailExistsForOtherUser_NullCurrentUserId() {
        UserEntity user1 = UserEntity.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        when(userRepository.findAll()).thenReturn(Collections.singletonList(user1));
        boolean result = userService.emailExistsForOtherUser("test@example.com", null);
        assertTrue(result);
    }

    @Test
    void testEmailExistsForOtherUser_UsersWithNullEmail() {
        UserEntity user1 = UserEntity.builder()
                .id(1L)
                .email(null)
                .build();

        UserEntity user2 = UserEntity.builder()
                .id(2L)
                .email("test@example.com")
                .build();

        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2));
        boolean result = userService.emailExistsForOtherUser("test@example.com", 1L);
        assertTrue(result);
    }

    @Test
    void testEmailExistsForOtherUser_ListWithCurrentAndAnotherUser() {
        UserEntity currentUser = UserEntity.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        UserEntity matchingUser = UserEntity.builder()
                .id(2L)
                .email("test@example.com")
                .build();

        when(userRepository.findAll()).thenReturn(Arrays.asList(currentUser, matchingUser));
        boolean result = userService.emailExistsForOtherUser("test@example.com", 1L);
        assertTrue(result);
    }

    @Test
    void testEmailExistsForOtherUser_WithOuterWhitespaceDirect() {
        UserEntity user1 = UserEntity.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        when(userRepository.findAll()).thenReturn(Collections.singletonList(user1));
        boolean result = userService.emailExistsForOtherUser("  TEST@EXAMPLE.COM  ", 2L);
        assertTrue(result);
    }

    @Test
    void testCanDeleteUser_NullUser() {
        boolean result = userService.canDeleteUser(null);
        assertFalse(result);
    }

    @Test
    void testCanDeleteUser_OrdinaryEnabledUser() {
        UserEntity user = UserEntity.builder()
                .id(1L)
                .enabled(true)
                .roleEntities(Collections.emptyList())
                .build();

        boolean result = userService.canDeleteUser(user);
        assertTrue(result);
    }

    @Test
    void testCanDeleteUser_DisabledAdmin() {
        RoleEntity adminRole = RoleEntity.builder()
                .id(1L)
                .name(RoleType.ROLE_ADMIN)
                .build();

        UserEntity user = UserEntity.builder()
                .id(1L)
                .enabled(false)
                .roleEntities(Collections.singletonList(adminRole))
                .build();

        boolean result = userService.canDeleteUser(user);
        assertTrue(result);
    }

    @Test
    void testCanDeleteUser_TheOnlyEnabledAdmin() {
        RoleEntity adminRole = RoleEntity.builder()
                .id(1L)
                .name(RoleType.ROLE_ADMIN)
                .build();

        UserEntity user = UserEntity.builder()
                .id(1L)
                .enabled(true)
                .roleEntities(Collections.singletonList(adminRole))
                .build();

        boolean result = userService.canDeleteUser(user);
        assertFalse(result);
    }

    @Test
    void testCanDeleteUser_EnabledAdminWithAnotherEnabledAdmin() {
        RoleEntity adminRole = RoleEntity.builder()
                .id(1L)
                .name(RoleType.ROLE_ADMIN)
                .build();

        UserEntity user1 = UserEntity.builder()
                .id(1L)
                .enabled(true)
                .roleEntities(Collections.singletonList(adminRole))
                .build();

        UserEntity user2 = UserEntity.builder()
                .id(2L)
                .enabled(true)
                .roleEntities(Collections.singletonList(adminRole))
                .build();

        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2));
        boolean result = userService.canDeleteUser(user1);
        assertTrue(result);
    }

    @Test
    void testWouldRemoveLastEnabledAdmin_OnlyEditedUserAsEnabledAdmin() {
        RoleEntity adminRole = RoleEntity.builder()
                .id(1L)
                .name(RoleType.ROLE_ADMIN)
                .build();

        UserEntity user1 = UserEntity.builder()
                .id(1L)
                .enabled(true)
                .roleEntities(Collections.singletonList(adminRole))
                .build();

        when(userRepository.findAll()).thenReturn(Collections.singletonList(user1));
        boolean result = userService.wouldRemoveLastEnabledAdmin(1L, false, Collections.singletonList(adminRole));
        assertTrue(result);
    }

    @Test
    void testWouldRemoveLastEnabledAdmin_OnlyEditedUserAsEnabledAdminRemovesRole() {
        RoleEntity adminRole = RoleEntity.builder()
                .id(1L)
                .name(RoleType.ROLE_ADMIN)
                .build();

        UserEntity user1 = UserEntity.builder()
                .id(1L)
                .enabled(true)
                .roleEntities(Collections.singletonList(adminRole))
                .build();

        when(userRepository.findAll()).thenReturn(Collections.singletonList(user1));
        boolean result = userService.wouldRemoveLastEnabledAdmin(1L, true, Collections.emptyList());
        assertTrue(result);
    }

    @Test
    void testWouldRemoveLastEnabledAdmin_OnlyEditedUserNullAndEmptyRoles() {
        RoleEntity adminRole = RoleEntity.builder()
                .id(1L)
                .name(RoleType.ROLE_ADMIN)
                .build();

        UserEntity user1 = UserEntity.builder()
                .id(1L)
                .enabled(true)
                .roleEntities(Collections.singletonList(adminRole))
                .build();

        when(userRepository.findAll()).thenReturn(Collections.singletonList(user1));
        boolean result = userService.wouldRemoveLastEnabledAdmin(1L, true, null);
        assertTrue(result);

        result = userService.wouldRemoveLastEnabledAdmin(1L, true, Collections.emptyList());
        assertTrue(result);
    }

    @Test
    void testWouldRemoveLastEnabledAdmin_AnotherEnabledAdminRemainsAfterRoleRemoval() {
        RoleEntity adminRole = RoleEntity.builder()
                .id(1L)
                .name(RoleType.ROLE_ADMIN)
                .build();

        UserEntity user1 = UserEntity.builder()
                .id(1L)
                .enabled(true)
                .roleEntities(Collections.singletonList(adminRole))
                .build();

        UserEntity user2 = UserEntity.builder()
                .id(2L)
                .enabled(true)
                .roleEntities(Collections.singletonList(adminRole))
                .build();

        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2));
        boolean result = userService.wouldRemoveLastEnabledAdmin(2L, true, Collections.emptyList());
        assertFalse(result);
    }

    @Test
    void testWouldRemoveLastEnabledAdmin_OrdinaryAndDisabledAdminsNotCounted() {
        RoleEntity adminRole = RoleEntity.builder()
                .id(1L)
                .name(RoleType.ROLE_ADMIN)
                .build();

        UserEntity enabledAdmin = UserEntity.builder()
                .id(1L)
                .enabled(true)
                .roleEntities(Collections.singletonList(adminRole))
                .build();

        UserEntity ordinaryUser = UserEntity.builder()
                .id(2L)
                .enabled(true)
                .roleEntities(Collections.emptyList())
                .build();

        UserEntity disabledAdmin = UserEntity.builder()
                .id(3L)
                .enabled(false)
                .roleEntities(Collections.singletonList(adminRole))
                .build();

        when(userRepository.findAll()).thenReturn(Arrays.asList(enabledAdmin, ordinaryUser, disabledAdmin));
        boolean result = userService.wouldRemoveLastEnabledAdmin(1L, false, Collections.singletonList(adminRole));
        assertTrue(result);
    }

    @Test
    void testWouldRemoveLastEnabledAdmin_NoRepositoryStubbingNeeded() {
        RoleEntity adminRole = RoleEntity.builder()
                .id(1L)
                .name(RoleType.ROLE_ADMIN)
                .build();

        boolean result = userService.wouldRemoveLastEnabledAdmin(1L, true, Collections.singletonList(adminRole));
        assertFalse(result);
    }

    @Test
    void testWouldRemoveLastEnabledAdmin_AnotherEnabledAdminRemainsAfterDisabling() {
        RoleEntity adminRole = RoleEntity.builder()
                .id(1L)
                .name(RoleType.ROLE_ADMIN)
                .build();

        UserEntity user1 = UserEntity.builder()
                .id(1L)
                .enabled(true)
                .roleEntities(Collections.singletonList(adminRole))
                .build();

        UserEntity user2 = UserEntity.builder()
                .id(2L)
                .enabled(true)
                .roleEntities(Collections.singletonList(adminRole))
                .build();

        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2));
        boolean result = userService.wouldRemoveLastEnabledAdmin(1L, false, Collections.singletonList(adminRole));
        assertFalse(result);
    }

    @Test
    void testCanDeleteUser_OnlyTargetEnabledAdmin() {
        RoleEntity adminRole = RoleEntity.builder()
                .id(1L)
                .name(RoleType.ROLE_ADMIN)
                .build();

        UserEntity user = UserEntity.builder()
                .id(1L)
                .enabled(true)
                .roleEntities(Collections.singletonList(adminRole))
                .build();

        when(userRepository.findAll()).thenReturn(Collections.singletonList(user));
        boolean result = userService.canDeleteUser(user);
        assertFalse(result);
    }

    @Test
    void testCanDeleteUser_OnlyTargetEnabledAdminWithOthersButNoOtherEnabledAdmins() {
        RoleEntity adminRole = RoleEntity.builder()
                .id(1L)
                .name(RoleType.ROLE_ADMIN)
                .build();

        UserEntity enabledAdmin = UserEntity.builder()
                .id(1L)
                .enabled(true)
                .roleEntities(Collections.singletonList(adminRole))
                .build();

        UserEntity ordinaryUser = UserEntity.builder()
                .id(2L)
                .enabled(true)
                .roleEntities(Collections.emptyList())
                .build();

        UserEntity disabledAdmin = UserEntity.builder()
                .id(3L)
                .enabled(false)
                .roleEntities(Collections.singletonList(adminRole))
                .build();

        when(userRepository.findAll()).thenReturn(Arrays.asList(enabledAdmin, ordinaryUser, disabledAdmin));
        boolean result = userService.canDeleteUser(enabledAdmin);
        assertFalse(result);
    }

    @Test
    void testUpdateUserLocale_UserExists() {
        RoleEntity adminRole = RoleEntity.builder()
                .id(1L)
                .name(RoleType.ROLE_ADMIN)
                .build();

        UserEntity user = UserEntity.builder()
                .id(1L)
                .email("test@example.com")
                .enabled(true)
                .roleEntities(Collections.singletonList(adminRole))
                .preferredLocale(Locale.ENGLISH)
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        Locale newLocale = Locale.FRENCH;
        userService.updateUserLocale("test@example.com", newLocale);

        assertEquals(newLocale, user.getPreferredLocale());
        verify(userRepository).save(user);
    }

    @Test
    void testUpdateUserLocale_UserDoesNotExist() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        Locale newLocale = Locale.FRENCH;
        userService.updateUserLocale("nonexistent@example.com", newLocale);

        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void testUpdateUserLocale_NullLocale() {
        RoleEntity adminRole = RoleEntity.builder()
                .id(1L)
                .name(RoleType.ROLE_ADMIN)
                .build();

        UserEntity user = UserEntity.builder()
                .id(1L)
                .email("test@example.com")
                .enabled(true)
                .roleEntities(Collections.singletonList(adminRole))
                .preferredLocale(Locale.ENGLISH)
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        userService.updateUserLocale("test@example.com", null);

        assertNull(user.getPreferredLocale());
        verify(userRepository).save(user);
    }

    @Test
    void testUpdateUserLocale_BlankLocale() {
        RoleEntity adminRole = RoleEntity.builder()
                .id(1L)
                .name(RoleType.ROLE_ADMIN)
                .build();

        UserEntity user = UserEntity.builder()
                .id(1L)
                .email("test@example.com")
                .enabled(true)
                .roleEntities(Collections.singletonList(adminRole))
                .preferredLocale(Locale.ENGLISH)
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        userService.updateUserLocale("test@example.com", Locale.forLanguageTag(""));

        assertEquals(Locale.forLanguageTag(""), user.getPreferredLocale());
        verify(userRepository).save(user);
    }

    @Test
    void testUpdatePassword_UserExists() {
        UserEntity user = UserEntity.builder()
                .id(1L)
                .email("test@example.com")
                .password("rawPassword")
                .build();

        String rawPassword = "newRawPassword";
        String encodedPassword = "encodedPassword";

        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);

        userService.updatePassword(user, rawPassword);

        verify(passwordEncoder).encode(rawPassword);
        assertEquals(encodedPassword, user.getPassword());
    }

    @Test
    void testUpdatePassword_UserIsNull() {
        // Testet das Verhalten bei null-Benutzer - sollte NullPointerException werfen
        UserEntity user = null;
        String rawPassword = "newRawPassword";
        
        assertThrows(NullPointerException.class, () -> {
            userService.updatePassword(user, rawPassword);
        });
    }

    @Test
    void testUpdatePassword_EmptyPassword() {
        // Testet das Verhalten mit leerem Passwort
        UserEntity user = UserEntity.builder()
                .id(1L)
                .email("test@example.com")
                .password("rawPassword")
                .build();

        String rawPassword = "";
        String encodedPassword = "encodedEmptyPassword";

        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);

        userService.updatePassword(user, rawPassword);

        verify(passwordEncoder).encode(rawPassword);
        assertEquals(encodedPassword, user.getPassword());
    }

    @Test
    void testUpdatePassword_PasswordEncoderThrowsException() {
        // Testet das Verhalten wenn PasswordEncoder eine Exception wirft
        UserEntity user = UserEntity.builder()
                .id(1L)
                .email("test@example.com")
                .password("existingPassword")
                .build();

        String rawPassword = "newRawPassword";
        IllegalStateException exception = new IllegalStateException("Encoding failed");

        // Konfiguriere den mocked PasswordEncoder um eine Exception zu werfen
        when(passwordEncoder.encode(rawPassword)).thenThrow(exception);

        // Assert that the same exception is propagated
        assertThrows(IllegalStateException.class, () -> {
            userService.updatePassword(user, rawPassword);
        });

        // Assert that the user's existing password remains unchanged
        assertEquals("existingPassword", user.getPassword());
        
        // Verify that the repository does not save the user (kein verify UserRepository aufruf hier)
    }
}
