package de.derpeterson.app.security;

import de.derpeterson.app.helper.jdbc.JdbcHelper;
import de.derpeterson.app.model.enums.AppRoute;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.NavigationErrorCode;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.LoginAttemptService;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.RememberMeAuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

  private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

  private final VaadinSecurityFilter vaadinSecurityFilter;

  private final CustomUserDetailsService userDetailsService;

  private final ConfigService configService;

  @Value("${app.security.expose-h2-console:false}")
  private boolean exposeH2Console;

  @Bean
  public AuthenticationManager authenticationManager(
      LoginAttemptService attempts, PasswordEncoder encoder) {
    var provider = new DaoAuthenticationProvider(userDetailsService::loadPasswordUserByUsername);
    provider.setPasswordEncoder(encoder);
    return new ProviderManager(
        java.util.List.of(
            new LoginAuthenticationService(provider, attempts),
            new RememberMeAuthenticationProvider(
                configService.getString(ConfigEntry.REMEMBER_ME_SECRET_KEY))));
  }

  @Bean
  public SecurityContextRepository securityContextRepository(SessionAdmissionService admissions) {
    return new AdmittedSecurityContextRepository(admissions);
  }

  @Bean
  public org.springframework.web.util.HttpSessionMutexListener httpSessionMutexListener() {
    return new org.springframework.web.util.HttpSessionMutexListener();
  }

  @Bean
  public SessionRegistryImpl sessionRegistry() {
    return new SessionRegistryImpl();
  }

  @Bean
  public HttpSessionEventPublisher httpSessionEventPublisher() {
    return new HttpSessionEventPublisher();
  }

  @Bean
  public SessionAdmissionService sessionAdmissionService(SessionRegistry registry) {
    return new SessionAdmissionService(configService, registry);
  }

  @Bean
  public FilterRegistrationBean<VaadinSecurityFilter> vaadinSecurityFilterRegistration() {
    var registration = new FilterRegistrationBean<>(vaadinSecurityFilter);
    registration.setEnabled(false);
    return registration;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public PersistentTokenRepository tokenRepository(
      DataSource dataSource, JdbcTemplate jdbcTemplate) {
    JdbcTokenRepositoryImpl tokenRepository = new JdbcTokenRepositoryImpl();
    tokenRepository.setDataSource(dataSource);
    if (!JdbcHelper.tableExists(jdbcTemplate, "persistent_logins")) {
      tokenRepository.setCreateTableOnStartup(true);
    }
    return tokenRepository;
  }

  @Bean
  public PersistentTokenBasedRememberMeServices rememberMeServices(
      PersistentTokenRepository tokenRepository, CustomUserDetailsService userDetailsService) {

    String secretKey = configService.getString(ConfigEntry.REMEMBER_ME_SECRET_KEY);
    int duration = configService.getInteger(ConfigEntry.REMEMBER_ME_DURATION);

    PersistentTokenBasedRememberMeServices tempRememberMeServices =
        new PersistentTokenBasedRememberMeServices(secretKey, userDetailsService, tokenRepository);
    tempRememberMeServices.setTokenValiditySeconds(duration);
    return tempRememberMeServices;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      PersistentTokenBasedRememberMeServices rememberMeServices,
      AuthenticationManager authenticationManager,
      SessionAdmissionService admissions,
      SecurityContextRepository contextRepository)
      throws Exception {
    logger.debug("🔥 SecurityConfig is loading!");

    http.authenticationManager(authenticationManager)
        .csrf(csrf -> csrf.disable())
        .headers(
            headers ->
                headers.frameOptions(
                    frameOptions -> {
                      if (exposeH2Console) {
                        frameOptions.disable();
                      } else {
                        frameOptions.sameOrigin();
                      }
                    }))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/VAADIN/**",
                        "/HILLA/**",
                        "/themes/**",
                        "/custom-theme/**",
                        "/lumo/**",
                        "/frontend/**",
                        "/favicon.ico",
                        "/manifest.webmanifest",
                        "/sw.js",
                        "/sw-runtime-resources-precache.js",
                        "/offline.html",
                        "/icons/**",
                        "/images/**",
                        "/line-awesome/**",
                        "/webjars/**",
                        "/error")
                    .permitAll()
                    .requestMatchers(
                        AppRoute.BASE.getSecurityRoute(),
                        AppRoute.HOME.getSecurityRoute(),
                        AppRoute.LOGIN.getSecurityRoute(),
                        AppRoute.REGISTRATION.getSecurityRoute(),
                        AppRoute.VERIFICATION.getSecurityRoute(),
                        AppRoute.FORGOT_PASSWORD.getSecurityRoute(),
                        AppRoute.RESET_PASSWORD.getSecurityRoute())
                    .permitAll()
                    .requestMatchers(
                        AppRoute.H2.getSecurityRoute(), AppRoute.H2.getSecurityRoute() + "/**")
                    .access((authentication, context) -> new AuthorizationDecision(exposeH2Console))
                    .requestMatchers(AppRoute.ADMIN.getSecurityRoute())
                    .hasRole(RoleType.ROLE_ADMIN.getName())
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            exceptionHandling ->
                exceptionHandling
                    .authenticationEntryPoint(
                        (request, response, authException) -> {
                          String requestUri = request.getServletPath();
                          String redirectTarget = AppRoute.LOGIN.getSecurityRoute();

                          if (!isKnownApplicationPath(requestUri)) {
                            redirectTarget =
                                AppRoute.HOME.getSecurityRoute()
                                    + "?error="
                                    + NavigationErrorCode.PATH_NOT_FOUND.getCode();
                          }

                          if (!response.isCommitted()) {
                            response.sendRedirect(redirectTarget);
                          }
                        })
                    .accessDeniedHandler(
                        (request, response, accessDeniedException) -> {
                          String redirectTarget =
                              AppRoute.HOME.getSecurityRoute()
                                  + "?error="
                                  + NavigationErrorCode.ACCESS_DENIED.getCode();
                          String requestUri = request.getServletPath();

                          if (requestUri != null
                              && requestUri.startsWith(AppRoute.ADMIN.getSecurityRoute())) {
                            redirectTarget =
                                AppRoute.HOME.getSecurityRoute()
                                    + "?error="
                                    + NavigationErrorCode.ADMIN_ACCESS_DENIED.getCode();
                          }

                          if (!response.isCommitted()) {
                            response.sendRedirect(redirectTarget);
                          }
                        }))
        .addFilterBefore(vaadinSecurityFilter, AuthorizationFilter.class)
        .addFilterBefore(
            new LoginCompletionFilter(admissions, rememberMeServices),
            UsernamePasswordAuthenticationFilter.class)
        .sessionManagement(session -> session.sessionFixation().changeSessionId())
        .securityContext(
            securityContext -> securityContext.securityContextRepository(contextRepository))
        .formLogin(
            login -> {
              login.addObjectPostProcessor(
                  new ObjectPostProcessor<UsernamePasswordAuthenticationFilter>() {
                    @Override
                    public <O extends UsernamePasswordAuthenticationFilter> O postProcess(
                        O filter) {
                      filter.setSessionAuthenticationStrategy(admissions);
                      return filter;
                    }
                  });
              login
                  .loginPage(AppRoute.LOGIN.getSecurityRoute())
                  .defaultSuccessUrl(AppRoute.ADMIN.getSecurityRoute(), true)
                  .successHandler(
                      (request, response, authentication) -> {
                        boolean admin =
                            authentication.getAuthorities().stream()
                                .anyMatch(
                                    authority ->
                                        RoleType.ROLE_ADMIN
                                            .name()
                                            .equals(authority.getAuthority()));
                        response.sendRedirect(
                            (admin ? AppRoute.ADMIN : AppRoute.HOME).getSecurityRoute());
                      })
                  .failureHandler(
                      (request, response, failure) -> {
                        String error =
                            failure instanceof SessionLimitException
                                ? "sessions"
                                : failure instanceof LoginThrottledException
                                    ? "throttled"
                                    : failure instanceof AuthenticationServiceException
                                        ? "unavailable"
                                        : "invalid";
                        response.sendRedirect(
                            AppRoute.LOGIN.getSecurityRoute() + "?error=" + error);
                      })
                  .permitAll();
            })
        .logout(
            logout ->
                logout
                    .logoutUrl(AppRoute.LOGOUT.getSecurityRoute())
                    .logoutSuccessUrl(AppRoute.LOGIN.getSecurityRoute())
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID", "remember-me"))
        .rememberMe(
            rememberMe ->
                rememberMe
                    .rememberMeServices(rememberMeServices)
                    .addObjectPostProcessor(
                        new ObjectPostProcessor<RememberMeAuthenticationFilter>() {
                          @Override
                          public <O extends RememberMeAuthenticationFilter> O postProcess(
                              O filter) {
                            filter.setSessionAuthenticationStrategy(admissions);
                            return filter;
                          }
                        }));

    logger.debug("🔧 SecurityConfig loaded!");

    return http.build();
  }

  private boolean isKnownApplicationPath(String requestUri) {
    if (requestUri == null || requestUri.isBlank()) {
      return false;
    }

    return requestUri.equals(AppRoute.BASE.getSecurityRoute())
        || requestUri.equals(AppRoute.HOME.getSecurityRoute())
        || requestUri.equals(AppRoute.ADMIN.getSecurityRoute())
        || requestUri.equals(AppRoute.LOGIN.getSecurityRoute())
        || requestUri.equals(AppRoute.LOGOUT.getSecurityRoute())
        || requestUri.equals(AppRoute.REGISTRATION.getSecurityRoute())
        || requestUri.equals(AppRoute.FORGOT_PASSWORD.getSecurityRoute())
        || requestUri.startsWith(AppRoute.VERIFICATION.getSecurityRoute().replace("/**", ""))
        || requestUri.startsWith(AppRoute.RESET_PASSWORD.getSecurityRoute().replace("/**", ""))
        || requestUri.startsWith(AppRoute.H2.getSecurityRoute());
  }
}
