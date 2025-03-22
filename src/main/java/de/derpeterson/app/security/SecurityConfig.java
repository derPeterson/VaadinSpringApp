package de.derpeterson.app.security;

import de.derpeterson.app.helper.jdbc.JdbcHelper;
import de.derpeterson.app.model.enums.AppRoute;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.service.ConfigService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import javax.sql.DataSource;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    private final VaadinSecurityFilter vaadinSecurityFilter;

    private final CustomUserDetailsService userDetailsService;

    private final ConfigService configService;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public PersistentTokenRepository tokenRepository(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        JdbcTokenRepositoryImpl tokenRepository = new JdbcTokenRepositoryImpl();
        tokenRepository.setDataSource(dataSource);
        if (!JdbcHelper.tableExists(jdbcTemplate, "persistent_logins")) {
            tokenRepository.setCreateTableOnStartup(true);
        }
        return tokenRepository;
    }

    @Bean
    public PersistentTokenBasedRememberMeServices rememberMeServices(PersistentTokenRepository tokenRepository, CustomUserDetailsService userDetailsService) {

        String secretKey = configService.getString(ConfigEntry.REMEMBER_ME_SECRET_KEY);
        int duration = configService.getInteger(ConfigEntry.REMEMBER_ME_DURATION);

        PersistentTokenBasedRememberMeServices tempRememberMeServices = new PersistentTokenBasedRememberMeServices(
                secretKey, userDetailsService, tokenRepository);
        tempRememberMeServices.setTokenValiditySeconds(duration);
        return tempRememberMeServices;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, PersistentTokenBasedRememberMeServices rememberMeServices) throws Exception {
        logger.debug("🔥 SecurityConfig is loading!");

        http.csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(AppRoute.BASE.getSecurityRoute(), AppRoute.HOME.getSecurityRoute(), AppRoute.LOGIN.getSecurityRoute(), AppRoute.REGISTRATION.getSecurityRoute(),
                                AppRoute.VERIFICATION.getSecurityRoute(), AppRoute.FORGOT_PASSWORD.getSecurityRoute(), AppRoute.RESET_PASSWORD.getSecurityRoute(),
                                AppRoute.H2.getSecurityRoute()).permitAll()
                        .requestMatchers(AppRoute.ADMIN.getSecurityRoute()).hasRole(RoleType.ROLE_ADMIN.getName())
                        .anyRequest().permitAll()
                )
                .addFilterBefore(vaadinSecurityFilter, UsernamePasswordAuthenticationFilter.class)
                .sessionManagement(session -> session
                        .sessionFixation().newSession()
                )
                .securityContext(securityContext -> securityContext
                        .securityContextRepository(securityContextRepository())
                )
                .formLogin(login -> login
                        .loginPage(AppRoute.LOGIN.getSecurityRoute())
                        .defaultSuccessUrl(AppRoute.ADMIN.getSecurityRoute(), true)
                        .successHandler((request, response, authentication) -> response.sendRedirect(AppRoute.ADMIN.getSecurityRoute()))
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl(AppRoute.LOGOUT.getSecurityRoute())
                        .logoutSuccessUrl(AppRoute.LOGIN.getSecurityRoute())
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "remember-me")
                )
                .rememberMe(rememberMe -> rememberMe
                        .rememberMeServices(rememberMeServices)
                )
                .userDetailsService(userDetailsService);

        logger.debug("🔧 SecurityConfig loaded!");

        return http.build();
    }
}
