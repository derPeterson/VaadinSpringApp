package de.derpeterson.app.views;

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
import com.vaadin.flow.component.page.Page;
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
  private UI ui;
  private Page page;
  private SecurityService security;
  private ConfigService config;
  private MaintenanceView view;
  private MockHttpServletRequest request;
  private final AtomicReference<Locale> locale = new AtomicReference<>(Locale.GERMAN);
  private final CustomI18NProvider provider = new CustomI18NProvider();

  @BeforeEach
  void setup() {
    ui = spy(new UI());
    page = mock(Page.class);
    doReturn(page).when(ui).getPage();
    UI.setCurrent(ui);
    VaadinSession session = mock(VaadinSession.class);
    when(session.hasLock()).thenReturn(true);
    when(session.getLocale()).thenAnswer(call -> locale.get());
    VaadinSession.setCurrent(session);
    security = mock(SecurityService.class);
    config = mock(ConfigService.class);
    when(config.getString(ConfigEntry.SERVICE_NAME)).thenReturn("Test");
    when(config.getString(ConfigEntry.COPYRIGHT_TEXT)).thenReturn("");
    when(config.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
    request = new MockHttpServletRequest();
    request.setContextPath("/app");
  }

  @AfterEach
  void cleanup() {
    NotificationHelper.getInstance().closeAndClearAllNotifications();
    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
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
      request
          .getSession()
          .setAttribute(
              HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
              new SecurityContextImpl(authentication));
      when(security.getAuthenticatedUser(request)).thenReturn(Optional.of(user));
    }
    view =
        new MaintenanceView(
            config,
            new MessageProperties(provider),
            new MaintenanceService(config),
            security,
            request);
    ui.add(view);
  }

  private Stream<Component> tree(Component component) {
    return Stream.concat(Stream.of(component), component.getChildren().flatMap(this::tree));
  }

  private Button button(String label) {
    return tree(view)
        .filter(Button.class::isInstance)
        .map(Button.class::cast)
        .filter(button -> label.equals(button.getText()))
        .findFirst()
        .orElseThrow();
  }

  @ParameterizedTest
  @EnumSource(RoleType.class)
  void authenticatedVisitorOnlySeesLogoutAndRetryWithoutChangingSession(RoleType role) {
    create(role);
    Object stored =
        request
            .getSession()
            .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
    for (Locale language : new Locale[] {Locale.GERMAN, Locale.ENGLISH}) {
      locale.set(language);
      LanguageChangeEvent.fire(ui, language);
      MessageProperties texts = MessageProperties.forLocale(provider, language);
      assertFalse(button(texts.getMaintenanceLoginButton()).isVisible());
      assertTrue(button(texts.getBaseLogoutButton()).isVisible());
      assertTrue(button(texts.getMaintenanceRetryButton()).isVisible());
    }
    assertSame(
        stored,
        request
            .getSession()
            .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
    verifyNoInteractions(page);
    verify(security, never()).logout();
  }

  @Test
  void anonymousVisitorOnlySeesLoginAndRetryAndCanOpenLogin() {
    create(null);
    for (Locale language : new Locale[] {Locale.GERMAN, Locale.ENGLISH}) {
      locale.set(language);
      LanguageChangeEvent.fire(ui, language);
      MessageProperties texts = MessageProperties.forLocale(provider, language);
      assertTrue(button(texts.getMaintenanceLoginButton()).isVisible());
      assertFalse(button(texts.getBaseLogoutButton()).isVisible());
      assertTrue(button(texts.getMaintenanceRetryButton()).isVisible());
    }
    button(MessageProperties.forLocale(provider, locale.get()).getMaintenanceLoginButton()).click();
    verify(page).setLocation("/app" + AppRoute.LOGIN.getSecurityRoute());
    assertNull(ReflectionTestUtils.getField(NotificationHelper.getInstance(), "messageText"));
    verify(security, never()).logout();
  }
}
