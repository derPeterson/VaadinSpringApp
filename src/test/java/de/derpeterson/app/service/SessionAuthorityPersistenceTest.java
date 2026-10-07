package de.derpeterson.app.service;

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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real committed roles, actual service/filter, strong UI fixtures; no HTTP server. */
@SpringJUnitConfig(UserServicePersistenceTest.Config.class)
class SessionAuthorityPersistenceTest {
    @Autowired UserService users;
    @Autowired UserRepository repository;
    @Autowired SecurityService security;
    @Autowired TransactionTemplate transaction;
    @PersistenceContext EntityManager entityManager;
    private Long victimId;
    private RoleEntity ordinaryRole;
    private UserDetails oldPrincipal;
    private UI ui;
    private VaadinSession vaadin;
    private final AtomicReference<UserDetails> vaadinPrincipal = new AtomicReference<>();

    @BeforeEach
    void setup() {
        transaction.executeWithoutResult(tx -> {
            repository.deleteAll();
            repository.flush();
            entityManager.createQuery("delete from RoleEntity").executeUpdate();
            var admin = RoleEntity.builder().name(RoleType.ROLE_ADMIN).build();
            ordinaryRole = RoleEntity.builder().name(RoleType.ROLE_USER).build();
            entityManager.persist(admin);
            entityManager.persist(ordinaryRole);
            victimId = create("victim@example.com", admin);
            create("survivor@example.com", admin);
        });
        oldPrincipal = new CustomUserDetailsService(repository).loadUserByUsername("victim@example.com");
        ui = new UI();
        vaadin = mock(VaadinSession.class);
        when(vaadin.hasLock()).thenReturn(true);
        when(vaadin.getAttribute(UserDetails.class)).thenAnswer(call -> vaadinPrincipal.get());
        doAnswer(call -> { vaadinPrincipal.set(call.getArgument(1)); return null; }).when(vaadin).setAttribute(eq(UserDetails.class), any());
        UI.setCurrent(ui);
        VaadinSession.setCurrent(vaadin);
        SecurityContextHolder.clearContext();
    }

    private Long create(String email, RoleEntity role) {
        var user = UserEntity.builder().email(email).firstName("Test").lastName("User").password("hash")
                .enabled(true).gender(Gender.OTHER).birthDate(LocalDate.of(1990, 1, 1)).preferredLocale(Locale.ENGLISH)
                .roleEntities(new ArrayList<>(List.of(role))).build();
        entityManager.persist(user);
        return user.getId();
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        VaadinSession.setCurrent(null);
        UI.setCurrent(null);
        vaadinPrincipal.set(null);
        vaadin = null;
        ui = null;
    }

    private UserEntity form() {
        return repository.findAll().stream().filter(user -> user.getId().equals(victimId)).findFirst().orElseThrow();
    }

    private void revoke() {
        UserEntity edited = form();
        edited.setRoleEntities(List.of(ordinaryRole));
        users.updateAdminUser(edited, edited.getVersion(), "");
    }

    private MockHttpServletRequest request(String source, String path) {
        var request = new MockHttpServletRequest("POST", path);
        var session = new MockHttpSession();
        request.setSession(session);
        session.setAttribute("authenticatedUser", oldPrincipal);
        if (source.equals("vaadin")) {
            vaadinPrincipal.set(oldPrincipal);
        } else if (!source.equals("http")) {
            Authentication auth = source.equals("remember")
                    ? new RememberMeAuthenticationToken("test-key", oldPrincipal, oldPrincipal.getAuthorities())
                    : UsernamePasswordAuthenticationToken.authenticated(oldPrincipal, null, oldPrincipal.getAuthorities());
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        }
        return request;
    }

    @ParameterizedTest
    @CsvSource({"context,/admin", "http,/admin", "vaadin,/admin", "remember,/admin",
            "context,/VAADIN/uidl", "http,/VAADIN/uidl", "vaadin,/VAADIN/uidl", "remember,/VAADIN/uidl"})
    void roleRevocationInvalidatesEveryExistingPrincipalSourceBeforeFurtherActions(String source, String path) throws Exception {
        var first = request(source, path);
        var firstSession = (MockHttpSession) first.getSession(false);
        var second = new MockHttpServletRequest("POST", path);
        var secondSession = new MockHttpSession();
        secondSession.setAttribute("authenticatedUser", oldPrincipal);
        second.setSession(secondSession);
        revoke();
        @SuppressWarnings("unchecked")
        ObjectProvider<SecurityService> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(security);
        var filter = new VaadinSecurityFilter(provider);
        var response = new MockHttpServletResponse();
        var action = mock(FilterChain.class);
        filter.doFilter(first, response, action);
        assertEquals(401, response.getStatus());
        verifyNoInteractions(action);
        assertTrue(firstSession.isInvalid());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNull(vaadinPrincipal.get());
        assertTrue(security.getAuthenticatedUser(second).isEmpty());
        assertTrue(secondSession.isInvalid());
        var current = new CustomUserDetailsService(repository).loadUserByUsername(oldPrincipal.getUsername());
        assertEquals(List.of("ROLE_USER"), current.getAuthorities().stream().map(Object::toString).toList());
    }

    @Test
    void rollbackDoesNotInvalidateCurrentAuthoritiesAndReadIgnoresPendingRoleEdits() {
        var request = request("context", "/admin");
        Authentication original = SecurityContextHolder.getContext().getAuthentication();
        transaction.executeWithoutResult(tx -> {
            revoke();
            assertTrue(security.getAuthenticatedUser(request).isPresent());
            assertSame(original, SecurityContextHolder.getContext().getAuthentication());
            tx.setRollbackOnly();
        });
        assertTrue(security.getAuthenticatedUser(request).isPresent());
        assertTrue(form().hasRole(RoleType.ROLE_ADMIN));
        assertFalse(((MockHttpSession) request.getSession(false)).isInvalid());
    }

    @Test
    void committedRevocationCannotHideBehindAStaleManagedUserOrRolesCollection() {
        var request = request("context", "/admin");
        var independent = new TransactionTemplate(transaction.getTransactionManager());
        independent.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transaction.executeWithoutResult(tx -> {
            UserEntity stale = form();
            assertTrue(stale.hasRole(RoleType.ROLE_ADMIN));
            independent.executeWithoutResult(other -> revoke());
            assertTrue(stale.hasRole(RoleType.ROLE_ADMIN));
            assertTrue(security.getAuthenticatedUser(request).isEmpty());
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"context", "remember"})
    void ordinaryStatusActivityAndProfileChangesRetainAuthenticationAndRememberMeType(String source) {
        var request = request(source, "/VAADIN/uidl");
        Authentication original = SecurityContextHolder.getContext().getAuthentication();
        users.updateLastActivity(victimId);
        users.updateUserStatus(form(), UserStatus.EMPLOYED, true);
        UserEntity edited = form();
        edited.setFirstName("Profile changed");
        users.updateAdminUser(edited, edited.getVersion(), "");
        assertTrue(security.getAuthenticatedUser(request).isPresent());
        assertSame(original, SecurityContextHolder.getContext().getAuthentication());
    }

    @ParameterizedTest
    @ValueSource(strings = {"disable", "delete"})
    void disabledAndDeletedAccountsAlsoLoseExistingAuthentication(String change) {
        var request = request("context", "/admin");
        var session = (MockHttpSession) request.getSession(false);
        UserEntity user = form();
        if (change.equals("delete")) {
            users.deleteUser(user);
        } else {
            user.setEnabled(false);
            users.save(user);
        }
        assertTrue(security.getAuthenticatedUser(request).isEmpty());
        assertTrue(session.isInvalid());
    }

    @Test
    void unauthenticatedPublicRequestContinuesWithoutCreatingASession() throws Exception {
        var request = new MockHttpServletRequest("GET", "/home");
        var response = new MockHttpServletResponse();
        @SuppressWarnings("unchecked")
        ObjectProvider<SecurityService> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(security);
        var action = mock(FilterChain.class);
        new VaadinSecurityFilter(provider).doFilter(request, response, action);
        verify(action).doFilter(request, response);
        assertEquals(200, response.getStatus());
        assertNull(request.getSession(false));
    }
}
