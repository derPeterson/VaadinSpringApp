package de.derpeterson.app.views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.AppRoute;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.security.MaintenanceService;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.ConfigService;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.util.ReflectionTestUtils;

/** Actual maintenance actions and visibility, without a running application or database. */
class MaintenanceViewTest {
  private UI testUi;
  // CurrentInstance only retains weak references; keep the locale fixture alive for every
  // assertion.
  private VaadinSession vaadinSession;
  private Page browserPage;
  private SecurityService securityService;
  private ConfigService configService;
  private MaintenanceView view;
  private MockHttpServletRequest mockHttpRequest;
  private final AtomicReference<Locale> localeReference = new AtomicReference<>(Locale.GERMAN);
  private final CustomI18NProvider i18nProvider = new CustomI18NProvider();

  @BeforeEach
  void setup() {
    testUi = spy(new UI());
    browserPage = mock(Page.class);
    doReturn(browserPage).when(testUi).getPage();
    UI.setCurrent(testUi);
    vaadinSession = mock(VaadinSession.class);
    when(vaadinSession.hasLock()).thenReturn(true);
    VaadinService vaadin = mock(VaadinService.class);
    DeploymentConfiguration deploymentConfiguration = mock(DeploymentConfiguration.class);
    when(vaadinSession.getService()).thenReturn(vaadin);
    when(vaadin.getDeploymentConfiguration()).thenReturn(deploymentConfiguration);
    when(deploymentConfiguration.getUrlSafeSchemes()).thenReturn(Set.of("http", "https"));
    when(vaadinSession.getLocale()).thenAnswer(call -> localeReference.get());
    VaadinSession.setCurrent(vaadinSession);
    testUi.getInternals().setSession(vaadinSession);
    securityService = mock(SecurityService.class);
    configService = mock(ConfigService.class);
    when(configService.getString(ConfigEntry.SERVICE_NAME)).thenReturn("Test");
    when(configService.getString(ConfigEntry.COPYRIGHT_TEXT)).thenReturn("");
    when(configService.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
    mockHttpRequest = new MockHttpServletRequest();
    mockHttpRequest.setContextPath("/app");
  }

  @AfterEach
  void cleanup() {
    NotificationHelper.getInstance().closeAndClearAllNotifications();
    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
    vaadinSession = null;
  }

  private void create(RoleType role) {
    if (role != null) {
      UserDetails user =
          User.withUsername("own-test@example.test")
              .password("unused")
              .authorities(role.getLongName())
              .build();
      UsernamePasswordAuthenticationToken authentication =
          UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities());
      mockHttpRequest
          .getSession()
          .setAttribute(
              HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
              new SecurityContextImpl(authentication));
      when(securityService.getAuthenticatedUser(mockHttpRequest)).thenReturn(Optional.of(user));
    }
    view =
        new MaintenanceView(
            configService,
            new MessageProperties(i18nProvider),
            new MaintenanceService(configService),
            securityService,
            mockHttpRequest);
    testUi.add(view);
  }

  private Stream<Component> tree(Component component) {
    return Stream.concat(Stream.of(component), component.getChildren().flatMap(this::tree));
  }

  private Button button(String label) {
    return tree(view)
        .filter(Button.class::isInstance)
        .map(Button.class::cast)
        .filter(button ->
            button
                .getParent()
                .map(parent -> parent.hasClassName("public-panel-footer"))
                .orElse(false))
        .filter(button -> label.equals(button.getText()))
        .findFirst()
        .orElseThrow();
  }

  @ParameterizedTest
  @EnumSource(RoleType.class)
  void authenticatedVisitorOnlySeesLogoutAndRetryWithoutChangingSession(RoleType role) {
    create(role);
    Object stored =
        mockHttpRequest
            .getSession()
            .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
    for (Locale language : new Locale[] {Locale.GERMAN, Locale.ENGLISH}) {
      localeReference.set(language);
      LanguageChangeEvent.fire(testUi, language);
      MessageProperties messageProperties = MessageProperties.forLocale(i18nProvider, language);

      assertFalse(button(messageProperties.getMaintenanceLoginButton()).isVisible());
      assertTrue(button(messageProperties.getBaseLogoutButton()).isVisible());
      assertTrue(button(messageProperties.getMaintenanceRetryButton()).isVisible());
    }
    assertSame(
        stored,
        mockHttpRequest
            .getSession()
            .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
    verifyNoInteractions(browserPage);
    verify(securityService, never()).logout();
  }

  @Test
  void anonymousVisitorOnlySeesLoginAndRetryAndCanOpenLogin() {
    create(null);
    SvgIcon maintenanceIcon =
        tree(view)
            .filter(SvgIcon.class::isInstance)
            .map(SvgIcon.class::cast)
            .filter(icon -> icon.hasClassName("cosmos-status-icon"))
            .findFirst()
            .orElseThrow();

    assertEquals("icons/lucide/wrench.svg", maintenanceIcon.getSrc());
    assertFalse(maintenanceIcon.hasClassName("cosmos-status-icon--warning"));
    for (Locale language : new Locale[] {Locale.GERMAN, Locale.ENGLISH}) {
      localeReference.set(language);
      LanguageChangeEvent.fire(testUi, language);
      MessageProperties messageProperties = MessageProperties.forLocale(i18nProvider, language);

      assertTrue(button(messageProperties.getMaintenanceLoginButton()).isVisible());
      assertFalse(button(messageProperties.getBaseLogoutButton()).isVisible());
      assertTrue(button(messageProperties.getMaintenanceRetryButton()).isVisible());
    }
    button(
            MessageProperties.forLocale(i18nProvider, localeReference.get())
                .getMaintenanceLoginButton())
        .click();

    verify(browserPage).setLocation("/app" + AppRoute.LOGIN.getSecurityRoute());
    assertNull(ReflectionTestUtils.getField(NotificationHelper.getInstance(), "messageText"));
    verify(securityService, never()).logout();
  }
}
