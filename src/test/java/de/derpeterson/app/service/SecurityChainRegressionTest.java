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
    @PersistenceContext EntityManager entityManager;

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
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

    private Long create(String email, RoleEntity role) {
        var user = UserEntity.builder().email(email).firstName("Test").lastName("User").password("hash").enabled(true)
                .gender(Gender.OTHER).birthDate(LocalDate.of(1990, 1, 1)).preferredLocale(Locale.ENGLISH)
                .roleEntities(new ArrayList<>(List.of(role))).build();
        entityManager.persist(user);
        return user.getId();
    }

    @Configuration
    @EnableWebSecurity
    @Import({UserServicePersistenceTest.Config.class, SecurityConfig.class})
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
            return config;
        }

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }
    }
}
