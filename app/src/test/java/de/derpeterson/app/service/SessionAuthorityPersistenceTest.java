package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.Gender;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.security.CustomUserDetailsService;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.security.VaadinSecurityFilter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.FilterChain;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Real committed roles, actual service/filter, strong UI fixtures; no HTTP server. */
@SpringJUnitConfig(UserServicePersistenceTest.Config.class)
class SessionAuthorityPersistenceTest {
  @Autowired UserService userService;
  @Autowired UserRepository userRepository;
  @Autowired SecurityService securityService;
  @Autowired TransactionTemplate transactionTemplate;
  @PersistenceContext EntityManager entityManager;
  private Long victimId;
  private RoleEntity ordinaryRole;
  private UserDetails oldPrincipal;
  private UI testUi;
  private VaadinSession vaadinSession;
  private final AtomicReference<UserDetails> vaadinPrincipal = new AtomicReference<>();

  @BeforeEach
  void setup() {
    transactionTemplate.executeWithoutResult(tx -> {
      userRepository.deleteAll();
      userRepository.flush();
      entityManager.createQuery("delete from RoleEntity").executeUpdate();
      RoleEntity admin = RoleEntity.builder().name(RoleType.ROLE_ADMIN).build();
      ordinaryRole = RoleEntity.builder().name(RoleType.ROLE_USER).build();
      entityManager.persist(admin);
      entityManager.persist(ordinaryRole);
      victimId = create("victim@example.com", admin);
      create("survivor@example.com", admin);
    });
    oldPrincipal =
        new CustomUserDetailsService(userRepository).loadUserByUsername("victim@example.com");
    testUi = new UI();
    vaadinSession = mock(VaadinSession.class);
    when(vaadinSession.hasLock()).thenReturn(true);
    when(vaadinSession.getAttribute(UserDetails.class)).thenAnswer(call -> vaadinPrincipal.get());
    doAnswer(
            call -> {
              vaadinPrincipal.set(call.getArgument(1));
              return null;
            })
        .when(vaadinSession)
        .setAttribute(eq(UserDetails.class), any());
    UI.setCurrent(testUi);
    VaadinSession.setCurrent(vaadinSession);
    SecurityContextHolder.clearContext();
  }

  private Long create(String email, RoleEntity role) {
    UserEntity testUserEntity =
        UserEntity.builder()
            .email(email)
            .firstName("Test")
            .lastName("User")
            .password("hash")
            .enabled(true)
            .gender(Gender.OTHER)
            .birthDate(LocalDate.of(1990, 1, 1))
            .preferredLocale(Locale.ENGLISH)
            .roleEntities(new ArrayList<>(List.of(role)))
            .build();
    entityManager.persist(testUserEntity);
    return testUserEntity.getId();
  }

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
    VaadinSession.setCurrent(null);
    UI.setCurrent(null);
    vaadinPrincipal.set(null);
    vaadinSession = null;
    testUi = null;
  }

  private UserEntity form() {
    return userRepository.findAll().stream()
        .filter(user -> user.getId().equals(victimId))
        .findFirst()
        .orElseThrow();
  }

  private void revoke() {
    UserEntity edited = form();
    edited.setRoleEntities(List.of(ordinaryRole));
    userService.updateAdminUser(edited, edited.getVersion(), "");
  }

  private MockHttpServletRequest request(String source, String path) {
    MockHttpServletRequest mockHttpRequest = new MockHttpServletRequest("POST", path);
    MockHttpSession mockHttpSession = new MockHttpSession();
    mockHttpRequest.setSession(mockHttpSession);
    mockHttpSession.setAttribute("authenticatedUser", oldPrincipal);
    if (source.equals("vaadin")) {
      vaadinPrincipal.set(oldPrincipal);
    } else if (!source.equals("http")) {
      Authentication auth =
          source.equals("remember")
              ? new RememberMeAuthenticationToken(
                  "test-key", oldPrincipal, oldPrincipal.getAuthorities())
              : UsernamePasswordAuthenticationToken.authenticated(
                  oldPrincipal, null, oldPrincipal.getAuthorities());
      SecurityContext context = SecurityContextHolder.createEmptyContext();
      context.setAuthentication(auth);
      SecurityContextHolder.setContext(context);
      mockHttpSession.setAttribute(
          HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    }
    return mockHttpRequest;
  }

  @ParameterizedTest
  @CsvSource({
    "context,/admin",
    "http,/admin",
    "vaadin,/admin",
    "remember,/admin",
    "context,/VAADIN/uidl",
    "http,/VAADIN/uidl",
    "vaadin,/VAADIN/uidl",
    "remember,/VAADIN/uidl"
  })
  void roleRevocationRefreshesEveryExistingPrincipalSourceWithoutLogout(String source, String path)
      throws Exception {
    MockHttpServletRequest firstMockHttpRequest = request(source, path);
    MockHttpSession firstSession = (MockHttpSession) firstMockHttpRequest.getSession(false);
    MockHttpServletRequest secondMockHttpRequest = new MockHttpServletRequest("POST", path);
    MockHttpSession secondSession = new MockHttpSession();
    secondSession.setAttribute("authenticatedUser", oldPrincipal);
    secondMockHttpRequest.setSession(secondSession);
    revoke();
    @SuppressWarnings("unchecked")
    ObjectProvider<SecurityService> provider = mock(ObjectProvider.class);
    when(provider.getObject()).thenReturn(securityService);
    VaadinSecurityFilter filter = new VaadinSecurityFilter(provider);
    MockHttpServletResponse mockHttpResponse = new MockHttpServletResponse();
    FilterChain action = mock(FilterChain.class);
    filter.doFilter(firstMockHttpRequest, mockHttpResponse, action);

    assertEquals(200, mockHttpResponse.getStatus());
    verify(action).doFilter(firstMockHttpRequest, mockHttpResponse);
    assertFalse(firstSession.isInvalid());
    assertEquals(
        List.of("ROLE_USER"),
        SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
            .map(Object::toString)
            .toList());
    assertEquals(
        List.of("ROLE_USER"),
        vaadinPrincipal.get().getAuthorities().stream().map(Object::toString).toList());
    assertEquals(
        List.of("ROLE_USER"),
        securityService
            .getAuthenticatedUser(secondMockHttpRequest)
            .orElseThrow()
            .getAuthorities()
            .stream()
            .map(Object::toString)
            .toList());
    assertFalse(secondSession.isInvalid());
    UserDetails current =
        new CustomUserDetailsService(userRepository).loadUserByUsername(oldPrincipal.getUsername());

    assertEquals(
        List.of("ROLE_USER"), current.getAuthorities().stream().map(Object::toString).toList());
  }

  @ParameterizedTest
  @CsvSource({
    "context,ROLE_SUPERADMIN",
    "http,ROLE_SUPERADMIN",
    "vaadin,ROLE_SUPERADMIN",
    "remember,ROLE_SUPERADMIN",
    "context,ROLE_GUEST",
    "http,ROLE_GUEST",
    "vaadin,ROLE_GUEST",
    "remember,ROLE_GUEST"
  })
  void replacingRolesRefreshesEveryExistingPrincipalSource(String source, RoleType roleType) {
    transactionTemplate.executeWithoutResult(tx -> {
      RoleEntity role = RoleEntity.builder().name(roleType).build();
      entityManager.persist(role);
      form().setRoleEntities(new ArrayList<>(List.of(role)));
      // Der Entzug testet die Sitzung; der neue Mindestschutz bleibt separat wirksam.
      if (roleType == RoleType.ROLE_SUPERADMIN) {
        userRepository
            .findByEmail("survivor@example.com")
            .orElseThrow()
            .getRoleEntities()
            .add(role);
      }
    });
    oldPrincipal =
        new CustomUserDetailsService(userRepository).loadUserByUsername("victim@example.com");
    MockHttpServletRequest mockHttpRequest = request(source, "/VAADIN/uidl");
    MockHttpSession oldSession = (MockHttpSession) mockHttpRequest.getSession(false);

    assertTrue(securityService.getAuthenticatedUser(mockHttpRequest).isPresent());
    revoke();

    assertEquals(
        List.of("ROLE_USER"),
        securityService
            .getAuthenticatedUser(mockHttpRequest)
            .orElseThrow()
            .getAuthorities()
            .stream()
            .map(Object::toString)
            .toList());
    assertFalse(oldSession.isInvalid());
    if (source.equals("remember")) {
      assertInstanceOf(
          RememberMeAuthenticationToken.class,
          SecurityContextHolder.getContext().getAuthentication());
    }
  }

  @Test
  void plainStringImitatingAFrameworkFactorIsStillRejectedAsAnUnstoredPrivilege() {
    ArrayList<GrantedAuthority> authorities =
        new ArrayList<org.springframework.security.core.GrantedAuthority>(
            oldPrincipal.getAuthorities());
    authorities.add(
        new org.springframework.security.core.authority.SimpleGrantedAuthority("FACTOR_PASSWORD"));
    oldPrincipal =
        org.springframework.security.core.userdetails.User.withUsername(oldPrincipal.getUsername())
            .password("unused")
            .authorities(authorities)
            .build();
    MockHttpServletRequest mockHttpRequest = request("context", "/admin");

    assertTrue(securityService.getAuthenticatedUser(mockHttpRequest).isEmpty());
  }

  @Test
  void rollbackDoesNotInvalidateCurrentAuthoritiesAndReadIgnoresPendingRoleEdits() {
    MockHttpServletRequest mockHttpRequest = request("context", "/admin");
    Authentication original = SecurityContextHolder.getContext().getAuthentication();
    transactionTemplate.executeWithoutResult(tx -> {
      revoke();

      assertTrue(securityService.getAuthenticatedUser(mockHttpRequest).isPresent());
      assertSame(original, SecurityContextHolder.getContext().getAuthentication());
      tx.setRollbackOnly();
    });

    assertTrue(securityService.getAuthenticatedUser(mockHttpRequest).isPresent());
    assertTrue(form().hasRole(RoleType.ROLE_ADMIN));
    assertFalse(((MockHttpSession) mockHttpRequest.getSession(false)).isInvalid());
  }

  @Test
  void committedRevocationCannotHideBehindAStaleManagedUserOrRolesCollection() {
    MockHttpServletRequest mockHttpRequest = request("context", "/admin");
    TransactionTemplate independent =
        new TransactionTemplate(transactionTemplate.getTransactionManager());
    independent.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    transactionTemplate.executeWithoutResult(tx -> {
      UserEntity stale = form();

      assertTrue(stale.hasRole(RoleType.ROLE_ADMIN));
      independent.executeWithoutResult(other -> revoke());

      assertTrue(stale.hasRole(RoleType.ROLE_ADMIN));
      assertEquals(
          List.of("ROLE_USER"),
          securityService
              .getAuthenticatedUser(mockHttpRequest)
              .orElseThrow()
              .getAuthorities()
              .stream()
              .map(Object::toString)
              .toList());
    });
  }

  @ParameterizedTest
  @ValueSource(strings = {"context", "remember"})
  void ordinaryStatusActivityAndProfileChangesRetainAuthenticationAndRememberMeType(String source) {
    MockHttpServletRequest mockHttpRequest = request(source, "/VAADIN/uidl");
    Authentication original = SecurityContextHolder.getContext().getAuthentication();
    userService.updateLastActivity(victimId);
    userService.updateUserStatus(form(), UserStatus.EMPLOYED, true);
    UserEntity edited = form();
    edited.setFirstName("Profile changed");
    userService.updateAdminUser(edited, edited.getVersion(), "");

    assertTrue(securityService.getAuthenticatedUser(mockHttpRequest).isPresent());
    assertSame(original, SecurityContextHolder.getContext().getAuthentication());
  }

  @ParameterizedTest
  @ValueSource(strings = {"disable", "delete"})
  void disabledAndDeletedAccountsAlsoLoseExistingAuthentication(String change) {
    MockHttpServletRequest mockHttpRequest = request("context", "/admin");
    MockHttpSession mockHttpSession = (MockHttpSession) mockHttpRequest.getSession(false);
    UserEntity testUserEntity = form();
    if (change.equals("delete")) {
      userService.deleteUser(testUserEntity);
    } else {
      testUserEntity.setEnabled(false);
      userService.save(testUserEntity);
    }
    assertTrue(securityService.getAuthenticatedUser(mockHttpRequest).isEmpty());
    assertTrue(mockHttpSession.isInvalid());
  }

  @Test
  void unauthenticatedPublicRequestContinuesWithoutCreatingASession() throws Exception {
    MockHttpServletRequest mockHttpRequest = new MockHttpServletRequest("GET", "/home");
    MockHttpServletResponse mockHttpResponse = new MockHttpServletResponse();
    @SuppressWarnings("unchecked")
    ObjectProvider<SecurityService> provider = mock(ObjectProvider.class);
    when(provider.getObject()).thenReturn(securityService);
    FilterChain action = mock(FilterChain.class);
    new VaadinSecurityFilter(provider).doFilter(mockHttpRequest, mockHttpResponse, action);

    verify(action).doFilter(mockHttpRequest, mockHttpResponse);
    assertEquals(200, mockHttpResponse.getStatus());
    assertNull(mockHttpRequest.getSession(false));
  }
}
