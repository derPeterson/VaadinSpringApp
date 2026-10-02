package de.derpeterson.app.service;

import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.websocket.UserStatusBroadcaster;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

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
}