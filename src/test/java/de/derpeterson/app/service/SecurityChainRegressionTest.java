package de.derpeterson.app.service;

import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.Gender;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.security.CustomUserDetailsService;
import de.derpeterson.app.security.SecurityConfig;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.security.VaadinSecurityFilter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Actual SecurityConfig/filter order in an isolated web context, without a running server. */
@SpringJUnitConfig(SecurityChainRegressionTest.Config.class)
@WebAppConfiguration
class SecurityChainRegressionTest {
    @Autowired SecurityFilterChain chain;
    @Autowired FilterRegistrationBean<VaadinSecurityFilter> registration;
    @Autowired UserService users;
    @Autowired UserRepository repository;
    @Autowired TransactionTemplate transaction;
    @Autowired ConfigService settings;
    @Autowired org.springframework.security.authentication.AuthenticationManager passwordManager;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder encoder;
    @Autowired de.derpeterson.app.repository.LoginAttemptRepository attempts;
    @PersistenceContext EntityManager entityManager;

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        when(settings.getInteger(ConfigEntry.LOGIN_ATTEMPTS_LIMIT)).thenReturn(10);
    }

    @Test
    void revalidationRunsAfterRememberMeBeforeAuthorizationAndNotAsAContainerFilter() {
        var filters = chain.getFilters();
        assertTrue(index(filters, RememberMeAuthenticationFilter.class) < index(filters, VaadinSecurityFilter.class));
        assertTrue(index(filters, VaadinSecurityFilter.class) < index(filters, AuthorizationFilter.class));
        assertFalse(registration.isEnabled());
    }

    private int index(List<Filter> filters, Class<?> type) {
        for (int i = 0; i < filters.size(); i++) {
            if (type.isInstance(filters.get(i))) {
                return i;
            }
        }
        fail("Missing filter: " + type.getSimpleName());
        return -1;
    }

    @ParameterizedTest
    @ValueSource(strings = {"/admin", "/VAADIN/uidl"})
    void actualChainRejectsRestoredStaleAdminBeforeServletOrVaadinCallback(String path) throws Exception {
        Long victimId = transaction.execute(tx -> {
            repository.deleteAll();
            repository.flush();
            entityManager.createQuery("delete from RoleEntity").executeUpdate();
            var admin = RoleEntity.builder().name(RoleType.ROLE_ADMIN).build();
            entityManager.persist(admin);
            Long victim = create("chain-victim@example.com", admin);
            create("chain-survivor@example.com", admin);
            return victim;
        });
        var principal = new CustomUserDetailsService(repository).loadUserByUsername("chain-victim@example.com");
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
        var session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        var edited = repository.findAll().stream().filter(user -> user.getId().equals(victimId)).findFirst().orElseThrow();
        edited.setRoleEntities(new ArrayList<>());
        users.updateAdminUser(edited, edited.getVersion(), "");
        var request = new MockHttpServletRequest("POST", path);
        request.setServletPath(path);
        request.setSession(session);
        var response = new MockHttpServletResponse();
        FilterChain action = mock(FilterChain.class);
        new FilterChainProxy(chain).doFilter(request, response, action);
        assertEquals(401, response.getStatus());
        assertTrue(session.isInvalid());
        verifyNoInteractions(action);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @ParameterizedTest
    @EnumSource(RoleType.class)
    void currentRoleGrantsHomeAndOnlyAdministratorsReachAdmin(RoleType roleType) throws Exception {
        transaction.executeWithoutResult(tx -> {
            repository.deleteAll();
            repository.flush();
            entityManager.createQuery("delete from RoleEntity").executeUpdate();
            var role = RoleEntity.builder().name(roleType).build();
            entityManager.persist(role);
            create("role-access@example.test", role);
        });
        var principal = new CustomUserDetailsService(repository).loadUserByUsername("role-access@example.test");
        var expected = roleType == RoleType.ROLE_SUPERADMIN
                ? java.util.Set.of("ROLE_SUPERADMIN", "ROLE_ADMIN") : java.util.Set.of(roleType.name());
        assertEquals(expected, principal.getAuthorities().stream().map(Object::toString).collect(java.util.stream.Collectors.toSet()));
        assertTrue(repository.findAll().getFirst().hasRole(roleType));
        if (roleType == RoleType.ROLE_SUPERADMIN) {
            assertFalse(repository.findAll().getFirst().hasRole(RoleType.ROLE_ADMIN),
                    "Inherited permission must not add a stored assignment");
        }
        for (String path : List.of("/home", "/admin")) {
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
            var session = new MockHttpSession();
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
            var request = new MockHttpServletRequest("GET", path);
            request.setServletPath(path);
            request.setSession(session);
            var response = new MockHttpServletResponse();
            FilterChain action = mock(FilterChain.class);
            new FilterChainProxy(chain).doFilter(request, response, action);
            if (path.equals("/home") || roleType == RoleType.ROLE_ADMIN || roleType == RoleType.ROLE_SUPERADMIN) {
                verify(action).doFilter(any(), any());
                assertEquals(200, response.getStatus());
            } else {
                verifyNoInteractions(action);
                assertEquals(302, response.getStatus());
                assertEquals("/home?error=admin-access-denied", response.getRedirectedUrl());
            }
        }
    }

    @ParameterizedTest @EnumSource(RoleType.class)
    void actualHttpPasswordFilterCannotBypassThrottleAndExistingSessionSurvives(RoleType roleType) throws Exception {
        var providers = assertInstanceOf(org.springframework.security.authentication.ProviderManager.class, passwordManager);
        assertTrue(providers.getProviders().stream().anyMatch(de.derpeterson.app.security.LoginAuthenticationService.class::isInstance));
        String email = "password-" + java.util.UUID.randomUUID() + "@example.test";
        transaction.executeWithoutResult(tx -> {
            var role = entityManager.createQuery("select r from RoleEntity r where r.name = :role", RoleEntity.class)
                    .setParameter("role", roleType).getResultStream().findFirst().orElse(null);
            if (role == null) { role = RoleEntity.builder().name(roleType).build(); entityManager.persist(role); }
            Long id = create(email, role);
            entityManager.find(UserEntity.class, id).setPassword(encoder.encode("OriginalPassword!"));
        });
        when(settings.getInteger(ConfigEntry.LOGIN_ATTEMPTS_LIMIT)).thenReturn(2);
        var successful = postPassword(email, "OriginalPassword!");
        assertEquals(roleType.isAdministrative() ? "/admin" : "/home", successful.response().getRedirectedUrl());
        var originalSession = successful.request().getSession(false);
        assertNotNull(originalSession.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
        assertEquals("/login?error=invalid", postPassword(email, "WrongPassword!").response().getRedirectedUrl());
        assertEquals("/login?error=invalid", postPassword(email, "WrongPassword!").response().getRedirectedUrl());
        var blocked = postPassword(email, "OriginalPassword!");
        assertEquals("/login?error=throttled", blocked.response().getRedirectedUrl());
        assertFalse(java.util.Arrays.stream(blocked.response().getCookies()).anyMatch(cookie ->
                cookie.getName().equals("remember-me") && cookie.getMaxAge() != 0));
        assertTrue(blocked.request().getSession(false) == null || blocked.request().getSession(false)
                .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY) == null);
        assertEquals(2, attempts.findById(LoginAttemptService.identityKey(email)).orElseThrow().getFailures());
        var saved = (org.springframework.security.core.context.SecurityContext) originalSession.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        var oldPrincipal = (org.springframework.security.core.userdetails.UserDetails) saved.getAuthentication().getPrincipal();
        var currentPrincipal = new CustomUserDetailsService(repository).loadUserByUsername(email);
        var currentRoles = currentPrincipal.getAuthorities().stream().map(Object::toString).collect(java.util.stream.Collectors.toSet());
        assertEquals(currentRoles, oldPrincipal.getAuthorities().stream()
                .filter(authority -> !(authority instanceof org.springframework.security.core.authority.FactorGrantedAuthority))
                .map(Object::toString).collect(java.util.stream.Collectors.toSet()));
        assertTrue(saved.getAuthentication().getAuthorities().stream()
                .anyMatch(org.springframework.security.core.authority.FactorGrantedAuthority.class::isInstance));
        assertTrue(currentPrincipal.isEnabled()); assertTrue(oldPrincipal.isEnabled());
        var oldRequest = new MockHttpServletRequest("GET", roleType.isAdministrative() ? "/admin" : "/home");
        oldRequest.setServletPath(oldRequest.getRequestURI()); oldRequest.setSession(originalSession);
        var response = new MockHttpServletResponse(); var servlet = mock(FilterChain.class);
        new FilterChainProxy(chain).doFilter(oldRequest, response, servlet);
        assertEquals(200, response.getStatus(), "Existing session: " + response.getRedirectedUrl() + " / " + response.getErrorMessage());
        verify(servlet).doFilter(any(), any());
        // Fixture-only state change proves framework factor evidence cannot preserve a disabled account.
        transaction.executeWithoutResult(tx -> repository.findByEmail(email).orElseThrow().setEnabled(false));
        var revoked = new MockHttpServletRequest("GET", "/home"); revoked.setServletPath("/home"); revoked.setSession(originalSession);
        var denied = new MockHttpServletResponse();
        new FilterChainProxy(chain).doFilter(revoked, denied, mock(FilterChain.class));
        assertEquals(401, denied.getStatus());
    }

    @Test
    void unknownHttpIdentityIsLimitedAndTechnicalConfigurationFailureDoesNotCount() throws Exception {
        String email = "unknown-" + java.util.UUID.randomUUID() + "@example.test";
        when(settings.getInteger(ConfigEntry.LOGIN_ATTEMPTS_LIMIT)).thenReturn(1);
        assertEquals("/login?error=invalid", postPassword(email, "WrongPassword!").response().getRedirectedUrl());
        assertEquals("/login?error=throttled", postPassword(email, "WrongPassword!").response().getRedirectedUrl());
        when(settings.getInteger(ConfigEntry.LOGIN_ATTEMPTS_LIMIT)).thenReturn(0);
        String other = "technical-" + java.util.UUID.randomUUID() + "@example.test";
        assertEquals("/login?error=unavailable", postPassword(other, "WrongPassword!").response().getRedirectedUrl());
        assertFalse(attempts.existsById(LoginAttemptService.identityKey(other)));
    }

    @Test
    void rememberMeUsesItsOwnProviderWithoutPasswordCountersEvenWithInvalidPasswordLimit() {
        when(settings.getInteger(ConfigEntry.LOGIN_ATTEMPTS_LIMIT)).thenReturn(0);
        long before = attempts.count();
        var principal = org.springframework.security.core.userdetails.User.withUsername("remember-fixture@example.test")
                .password("unused").roles("USER").build();
        var token = new org.springframework.security.authentication.RememberMeAuthenticationToken(
                "isolated-test-key", principal, principal.getAuthorities());
        assertTrue(passwordManager.authenticate(token).isAuthenticated());
        var forged = new org.springframework.security.authentication.RememberMeAuthenticationToken(
                "wrong-key", principal, principal.getAuthorities());
        assertThrows(org.springframework.security.authentication.BadCredentialsException.class, () -> passwordManager.authenticate(forged));
        assertEquals(before, attempts.count());
    }

    private PasswordResponse postPassword(String email, String password) throws Exception {
        SecurityContextHolder.clearContext();
        var request = new MockHttpServletRequest("POST", "/login"); request.setServletPath("/login");
        request.setParameter("username", email); request.setParameter("password", password);
        var response = new MockHttpServletResponse();
        new FilterChainProxy(chain).doFilter(request, response, mock(FilterChain.class));
        assertEquals(302, response.getStatus()); return new PasswordResponse(request, response);
    }
    private record PasswordResponse(MockHttpServletRequest request, MockHttpServletResponse response) { }

    private Long create(String email, RoleEntity role) {
        var user = UserEntity.builder().email(email).firstName("Test").lastName("User").password("hash").enabled(true)
                .gender(Gender.OTHER).birthDate(LocalDate.of(1990, 1, 1)).preferredLocale(Locale.ENGLISH)
                .roleEntities(new ArrayList<>(List.of(role))).build();
        entityManager.persist(user);
        return user.getId();
    }

    @Configuration
    @EnableWebSecurity
    @Import({UserServicePersistenceTest.Config.class, SecurityConfig.class, LoginAttemptService.class})
    static class Config {
        @Bean
        CustomUserDetailsService userDetailsService(UserRepository repository) {
            return new CustomUserDetailsService(repository);
        }

        @Bean
        VaadinSecurityFilter vaadinSecurityFilter(ObjectProvider<SecurityService> security) {
            return new VaadinSecurityFilter(security);
        }

        @Bean
        ConfigService configService() {
            var config = mock(ConfigService.class);
            when(config.getString(ConfigEntry.REMEMBER_ME_SECRET_KEY)).thenReturn("isolated-test-key");
            when(config.getInteger(ConfigEntry.REMEMBER_ME_DURATION)).thenReturn(3600);
            when(config.getInteger(ConfigEntry.LOGIN_ATTEMPTS_LIMIT)).thenReturn(10);
            return config;
        }

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }
    }
}
