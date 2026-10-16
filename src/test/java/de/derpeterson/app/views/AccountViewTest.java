package de.derpeterson.app.views;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.UserService;
import de.derpeterson.app.ui.components.AccountSettingsComponent;
import de.derpeterson.app.ui.components.UserPopoverMenu;
import de.derpeterson.app.websocket.UserStatusBroadcaster;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AccountViewTest {
  private UI ui;
  private SecurityService security;
  private HttpServletRequest request;
  private AccountView view;
  private final AtomicReference<Locale> locale = new AtomicReference<>(Locale.GERMAN);

  @BeforeEach
  void setup() {
    ui = spy(new UI());
    UI.setCurrent(ui);
    var session = mock(VaadinSession.class);
    when(session.hasLock()).thenReturn(true);
    when(session.getLocale()).thenAnswer(call -> locale.get());
    doAnswer(
            call -> {
              locale.set(call.getArgument(0));
              return null;
            })
        .when(session)
        .setLocale(any());
    VaadinSession.setCurrent(session);
    request = mock(HttpServletRequest.class);
    security = mock(SecurityService.class);
    var config = mock(ConfigService.class);
    when(config.getString(any(ConfigEntry.class)))
        .thenAnswer(call -> ((ConfigEntry) call.getArgument(0)).getDefaultValueString());
    var user =
        UserEntity.builder()
            .id(12L)
            .firstName("Own")
            .lastName("Account")
            .email("own@example.test")
            .enabled(true)
            .build();
    when(security.getCurrentUser(request)).thenReturn(Optional.of(user));
    view =
        new AccountView(
            new MessageProperties(new CustomI18NProvider()),
            security,
            request,
            config,
            mock(UserService.class),
            new UserStatusBroadcaster());
  }

  @AfterEach
  void cleanup() {
    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
  }

  @Test
  void viewForwardsAnUnauthenticatedVisitorToLogin() {
    var event = mock(BeforeEnterEvent.class);
    view.beforeEnter(event);
    verify(event).forwardTo(LoginView.class);
  }

  @Test
  void existingAccountMenuOpensTheNewAccountRouteAndClosesThePopover() {
    var menu = (UserPopoverMenu) ReflectionTestUtils.getField(view, "userPopoverMenu");
    var button = (Button) ReflectionTestUtils.getField(menu, "manageAccountButton");
    assertTrue(button.isEnabled());
    doReturn(Optional.empty()).when(ui).navigate(AccountView.class);
    menu.getPopover().setOpened(true);
    button.click();
    assertFalse(menu.getPopover().isOpened());
    verify(ui).navigate(AccountView.class);
    verify(security).getCurrentUser(request);
    verifyNoMoreInteractions(security);
  }

  @Test
  void trackedLanguageEventRefreshesTheAccountSurface() {
    LanguageChangeEvent.fire(ui, Locale.ENGLISH);
    var content = (AccountSettingsComponent) ReflectionTestUtils.getField(view, "accountSettings");
    var texts = MessageProperties.forLocale(new CustomI18NProvider(), Locale.ENGLISH);
    assertTrue(content.getElement().getTextRecursively().contains(texts.getAccountSettingsTitle()));
  }
}
