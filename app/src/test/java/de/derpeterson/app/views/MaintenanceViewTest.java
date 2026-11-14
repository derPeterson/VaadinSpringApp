package de.derpeterson.app.views;

import static de.derpeterson.app.ComponentTestTree.streamComponentTree;
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

/** Wartungsaktionen und Sichtbarkeit ohne laufende Anwendung oder Datenbank. */
class MaintenanceViewTest {
  private UI testUi;
  // CurrentInstance hält nur schwache Referenzen; das Fixture hält die Sitzung bis Testende fest.
  private VaadinSession vaadinSession;
  private Page browserPage;
  private SecurityService securityService;
  private ConfigService configService;
  private MaintenanceView maintenanceView;
  private MockHttpServletRequest mockHttpServletRequest;
  private final AtomicReference<Locale> activeLocaleReference =
      new AtomicReference<>(Locale.GERMAN);
  private final CustomI18NProvider customI18NProvider = new CustomI18NProvider();

  @BeforeEach
  void createMaintenanceUi() {
    testUi = spy(new UI());
    browserPage = mock(Page.class);
    doReturn(browserPage).when(testUi).getPage();
    UI.setCurrent(testUi);

    vaadinSession = mock(VaadinSession.class);
    when(vaadinSession.hasLock()).thenReturn(true);
    VaadinService vaadinService = mock(VaadinService.class);
    DeploymentConfiguration deploymentConfiguration = mock(DeploymentConfiguration.class);
    when(vaadinSession.getService()).thenReturn(vaadinService);
    when(vaadinService.getDeploymentConfiguration()).thenReturn(deploymentConfiguration);
    when(deploymentConfiguration.getUrlSafeSchemes()).thenReturn(Set.of("http", "https"));
    when(vaadinSession.getLocale()).thenAnswer(mockInvocation -> activeLocaleReference.get());
    VaadinSession.setCurrent(vaadinSession);
    testUi.getInternals().setSession(vaadinSession);

    securityService = mock(SecurityService.class);
    configService = mock(ConfigService.class);
    when(configService.getString(ConfigEntry.SERVICE_NAME)).thenReturn("Test");
    when(configService.getString(ConfigEntry.COPYRIGHT_TEXT)).thenReturn("");
    when(configService.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);

    mockHttpServletRequest = new MockHttpServletRequest();
    mockHttpServletRequest.setContextPath("/app");
  }

  @AfterEach
  void clearMaintenanceUi() {
    NotificationHelper.getInstance().closeAndClearAllNotifications();
    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
    vaadinSession = null;
  }

  private void createMaintenanceView(RoleType roleType) {
    if (roleType != null) {
      UserDetails authenticatedUserDetails =
          User.withUsername("own-test@example.test")
              .password("unused")
              .authorities(roleType.getLongName())
              .build();
      UsernamePasswordAuthenticationToken passwordAuthenticationToken =
          UsernamePasswordAuthenticationToken.authenticated(
              authenticatedUserDetails, null, authenticatedUserDetails.getAuthorities());
      mockHttpServletRequest
          .getSession()
          .setAttribute(
              HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
              new SecurityContextImpl(passwordAuthenticationToken));
      when(securityService.getAuthenticatedUser(mockHttpServletRequest))
          .thenReturn(Optional.of(authenticatedUserDetails));
    }

    maintenanceView =
        new MaintenanceView(
            configService,
            new MessageProperties(customI18NProvider),
            new MaintenanceService(configService),
            securityService,
            mockHttpServletRequest);
    testUi.add(maintenanceView);
  }

  private Button findFooterButton(String labelText) {
    return streamComponentTree(maintenanceView)
        .filter(Button.class::isInstance)
        .map(Button.class::cast)
        .filter(candidateButton ->
            candidateButton
                .getParent()
                .map(parentComponent -> parentComponent.hasClassName("public-panel-footer"))
                .orElse(false))
        .filter(candidateButton -> labelText.equals(candidateButton.getText()))
        .findFirst()
        .orElseThrow();
  }

  @ParameterizedTest
  @EnumSource(RoleType.class)
  void authenticatedVisitorOnlySeesLogoutAndRetryWithoutChangingSession(RoleType roleType) {
    createMaintenanceView(roleType);
    Object storedSecurityContextObject =
        mockHttpServletRequest
            .getSession()
            .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);

    for (Locale languageLocale : new Locale[] {Locale.GERMAN, Locale.ENGLISH}) {
      activeLocaleReference.set(languageLocale);
      LanguageChangeEvent.fire(testUi, languageLocale);
      MessageProperties messageProperties =
          MessageProperties.forLocale(customI18NProvider, languageLocale);

      assertFalse(findFooterButton(messageProperties.getMaintenanceLoginButton()).isVisible());
      assertTrue(findFooterButton(messageProperties.getBaseLogoutButton()).isVisible());
      assertTrue(findFooterButton(messageProperties.getMaintenanceRetryButton()).isVisible());
    }

    assertSame(
        storedSecurityContextObject,
        mockHttpServletRequest
            .getSession()
            .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
    verifyNoInteractions(browserPage);
    verify(securityService, never()).logout();
  }

  @Test
  void anonymousVisitorOnlySeesLoginAndRetryAndCanOpenLogin() {
    createMaintenanceView(null);
    SvgIcon maintenanceSvgIcon =
        streamComponentTree(maintenanceView)
            .filter(SvgIcon.class::isInstance)
            .map(SvgIcon.class::cast)
            .filter(icon -> icon.hasClassName("cosmos-status-icon"))
            .findFirst()
            .orElseThrow();

    assertEquals("icons/lucide/wrench.svg", maintenanceSvgIcon.getSrc());
    assertFalse(maintenanceSvgIcon.hasClassName("cosmos-status-icon--warning"));

    for (Locale languageLocale : new Locale[] {Locale.GERMAN, Locale.ENGLISH}) {
      activeLocaleReference.set(languageLocale);
      LanguageChangeEvent.fire(testUi, languageLocale);
      MessageProperties messageProperties =
          MessageProperties.forLocale(customI18NProvider, languageLocale);

      assertTrue(findFooterButton(messageProperties.getMaintenanceLoginButton()).isVisible());
      assertFalse(findFooterButton(messageProperties.getBaseLogoutButton()).isVisible());
      assertTrue(findFooterButton(messageProperties.getMaintenanceRetryButton()).isVisible());
    }

    findFooterButton(
            MessageProperties.forLocale(customI18NProvider, activeLocaleReference.get())
                .getMaintenanceLoginButton())
        .click();

    verify(browserPage).setLocation("/app" + AppRoute.LOGIN.getSecurityRoute());
    assertNull(ReflectionTestUtils.getField(NotificationHelper.getInstance(), "messageText"));
    verify(securityService, never()).logout();
  }
}
