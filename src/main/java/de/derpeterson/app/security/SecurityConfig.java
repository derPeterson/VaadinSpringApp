package de.derpeterson.app.security;

import static de.derpeterson.app.helper.logging.LogSafety.safeFailure;

import de.derpeterson.app.model.enums.AppRoute;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.NavigationErrorCode;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.LoginAttemptService;
import jakarta.servlet.http.HttpSessionEvent;
import java.util.List;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.RememberMeAuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.util.HttpSessionMutexListener;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

  private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

  private final VaadinSecurityFilter vaadinSecurityFilter;

  private final CustomUserDetailsService userDetailsService;

  private final ConfigService configService;

  @Value("${app.security.expose-h2-console:false}")
  private boolean exposeH2Console;

  @Value("${vaadin.url-mapping:/*}")
  private String vaadinServletMapping;

  @Bean
  public AuthenticationManager authenticationManager(
      LoginAttemptService attempts, PasswordEncoder encoder) {
    DaoAuthenticationProvider provider =
        new DaoAuthenticationProvider(userDetailsService::loadPasswordUserByUsername);
    provider.setPasswordEncoder(encoder);
    return new ProviderManager(
        List.of(
            new LoginAuthenticationService(provider, attempts),
            new RememberMeAuthenticationProvider(
                configService.getString(ConfigEntry.REMEMBER_ME_SECRET_KEY))));
  }

  @Bean
  public SecurityContextRepository securityContextRepository(SessionAdmissionService admissions) {
    return new AdmittedSecurityContextRepository(admissions);
  }

  @Bean
  public HttpSessionMutexListener httpSessionMutexListener() {
    return new HttpSessionMutexListener();
  }

  @Bean
  public MaintenanceService maintenanceService() {
    return new MaintenanceService(configService);
  }

  @Bean
  public SessionRegistryImpl sessionRegistry() {
    return new SessionRegistryImpl();
  }

  @Bean
  public HttpSessionEventPublisher httpSessionEventPublisher(SessionAdmissionService admissions) {
    return new HttpSessionEventPublisher() {
      @Override
      public void sessionIdChanged(HttpSessionEvent event, String previous) {
        admissions.sessionIdChanged(
            previous, event.getSession().getId(), () -> super.sessionIdChanged(event, previous));
      }
    };
  }

  @Bean
  public SessionAdmissionService sessionAdmissionService(
      SessionRegistry registry, DeviceTokenRepository tokens) {
    SessionAdmissionService admissions = new SessionAdmissionService(configService, registry);
    admissions.setDeviceTokens(tokens);
    return admissions;
  }

  @Bean
  public FilterRegistrationBean<VaadinSecurityFilter> vaadinSecurityFilterRegistration() {
    FilterRegistrationBean<VaadinSecurityFilter> registration =
        new FilterRegistrationBean<>(vaadinSecurityFilter);
    registration.setEnabled(false);
    return registration;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public DeviceTokenRepository tokenRepository(
      DataSource dataSource, PlatformTransactionManager manager) {
    return new DeviceTokenRepository(dataSource, manager);
  }

  @Bean
  public DeviceRememberMeServices rememberMeServices(
      DeviceTokenRepository tokenRepository,
      CustomUserDetailsService userDetailsService,
      SessionAdmissionService admissions) {

    String secretKey = configService.getString(ConfigEntry.REMEMBER_ME_SECRET_KEY);
    int duration = configService.getInteger(ConfigEntry.REMEMBER_ME_DURATION);

    DeviceRememberMeServices tempRememberMeServices =
        new DeviceRememberMeServices(secretKey, userDetailsService, tokenRepository, admissions);
    tempRememberMeServices.setTokenValiditySeconds(duration);
    return tempRememberMeServices;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      PersistentTokenBasedRememberMeServices rememberMeServices,
      AuthenticationManager authenticationManager,
      SessionAdmissionService admissions,
      MaintenanceService maintenance,
      SecurityContextRepository contextRepository,
      ObjectProvider<SessionManagementService> sessionManagement,
      @Qualifier("h2Console") ObjectProvider<ServletRegistrationBean<?>> h2Console)
      throws Exception {
    logger.debug("🔥 SecurityConfig is loading!");

    H2ConsoleOriginFilter consoleProtection =
        new H2ConsoleOriginFilter(exposeH2Console, h2Console::getIfAvailable);
    FlowCsrfRequestMatcher flowProtection = new FlowCsrfRequestMatcher(vaadinServletMapping);
    http.authenticationManager(authenticationManager)
        .csrf(
            csrf ->
                csrf.requireCsrfProtectionMatcher(
                    request ->
                        flowProtection.matches(request)
                            && !consoleProtection.allowsConsoleRequest(request)))
        .headers(headers -> headers.frameOptions(FrameOptionsConfig::sameOrigin))
        .addFilterBefore(consoleProtection, CsrfFilter.class)
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(consoleProtection::isConsoleRequest)
                    .access((authentication, context) -> new AuthorizationDecision(exposeH2Console))
                    .requestMatchers(
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
                        AppRoute.MAINTENANCE.getSecurityRoute(),
                        AppRoute.REGISTRATION.getSecurityRoute(),
                        AppRoute.VERIFICATION.getSecurityRoute(),
                        AppRoute.FORGOT_PASSWORD.getSecurityRoute(),
                        AppRoute.RESET_PASSWORD.getSecurityRoute(),
                        AppRoute.EMAIL_CHANGE.getSecurityRoute())
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
                          if (accessDeniedException instanceof CsrfException) {
                            response.sendError(403);
                            return;
                          }
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
        .addFilterAfter(new LoginCsrfTokenFilter(), RememberMeAuthenticationFilter.class)
        .addFilterBefore(vaadinSecurityFilter, AuthorizationFilter.class)
        .addFilterAfter(new MaintenanceHttpFilter(maintenance), VaadinSecurityFilter.class)
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
                        if (rememberMeServices instanceof DeviceRememberMeServices devices
                            && !devices.requested(request)) {
                          devices.preparePasswordLogin(request, response, authentication.getName());
                        }
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
                        if (failure instanceof SessionLimitException) {
                          SessionManagementService sessions = sessionManagement.getIfAvailable();
                          if (sessions != null) {
                            try {
                              sessions.startRecovery(
                                  request,
                                  request.getParameter("username"),
                                  request.getParameter("password"),
                                  rememberMeServices instanceof DeviceRememberMeServices devices
                                      && devices.requested(request));
                            } catch (AuthenticationException rejected) {
                              failure = rejected;
                            } catch (RuntimeException unavailable) {
                              logger.warn(
                                  "Restricted session management unavailable",
                                  safeFailure(unavailable));
                              failure =
                                  new AuthenticationServiceException(
                                      "Sign-in temporarily unavailable");
                            }
                          }
                        }
                        String error = authenticationFailureCode(failure);
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

  private String authenticationFailureCode(AuthenticationException failure) {
    if (failure instanceof MaintenanceAuthenticationException) {
      return "maintenance";
    }
    if (failure instanceof SessionLimitException) {
      return "sessions";
    }
    if (failure instanceof LoginThrottledException) {
      return "throttled";
    }
    if (failure instanceof AuthenticationServiceException) {
      return "unavailable";
    }
    return "invalid";
  }

  private boolean isKnownApplicationPath(String requestUri) {
    if (requestUri == null || requestUri.isBlank()) {
      return false;
    }

    return requestUri.equals(AppRoute.BASE.getSecurityRoute())
        || requestUri.equals(AppRoute.HOME.getSecurityRoute())
        || requestUri.equals(AppRoute.ACCOUNT.getSecurityRoute())
        || requestUri.equals(AppRoute.NOTIFICATIONS.getSecurityRoute())
        || requestUri.equals(AppRoute.ADMIN.getSecurityRoute())
        || requestUri.equals(AppRoute.LOGIN.getSecurityRoute())
        || requestUri.equals(AppRoute.MAINTENANCE.getSecurityRoute())
        || requestUri.equals(AppRoute.LOGOUT.getSecurityRoute())
        || requestUri.equals(AppRoute.REGISTRATION.getSecurityRoute())
        || requestUri.equals(AppRoute.FORGOT_PASSWORD.getSecurityRoute())
        || requestUri.startsWith(AppRoute.VERIFICATION.getSecurityRoute().replace("/**", ""))
        || requestUri.startsWith(AppRoute.RESET_PASSWORD.getSecurityRoute().replace("/**", ""))
        || requestUri.startsWith(AppRoute.EMAIL_CHANGE.getSecurityRoute().replace("/**", ""))
        || requestUri.startsWith(AppRoute.H2.getSecurityRoute());
  }
}
