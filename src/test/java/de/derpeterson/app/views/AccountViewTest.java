package de.derpeterson.app.views;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.AccountProfileService;
import de.derpeterson.app.service.AccountProfileService.Changes;
import de.derpeterson.app.service.AccountProfileService.Profile;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.UserService;
import de.derpeterson.app.ui.components.AccountSettingsComponent;
import de.derpeterson.app.ui.components.UserPopoverMenu;
import de.derpeterson.app.websocket.UserStatusBroadcaster;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AccountViewTest {
  private UI ui;
  private SecurityService security;
  private HttpServletRequest request;
  private AccountView view;
  private AccountProfileService profiles;
  private UserEntity user;
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
    ui.getInternals().setSession(session);
    request = mock(HttpServletRequest.class);
    security = mock(SecurityService.class);
    var config = mock(ConfigService.class);
    when(config.getString(any(ConfigEntry.class)))
        .thenAnswer(call -> ((ConfigEntry) call.getArgument(0)).getDefaultValueString());
    profiles = mock(AccountProfileService.class);
    user =
        UserEntity.builder()
            .id(12L)
            .version(3L)
            .birthDate(LocalDate.of(1990, 1, 1))
            .preferredLocale(Locale.GERMAN)
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
            profiles,
            new UserStatusBroadcaster());
  }

  @AfterEach
  void cleanup() {
    NotificationHelper.getInstance().closeAndClearAllNotifications();
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

  private Stream<Component> tree(Component component) {
    return Stream.concat(Stream.of(component), component.getChildren().flatMap(this::tree));
  }

  @Test
  void saveUsesAuthenticatedRequestRefreshesNameAndAppliesLanguageOnlyAfterSuccess() {
    var content = (AccountSettingsComponent) ReflectionTestUtils.getField(view, "accountSettings");
    var date = LocalDate.of(1990, 1, 1);
    when(profiles.save(eq(request), any()))
        .thenReturn(new Profile(4L, "Changed", "Name", date, Locale.ENGLISH));
    ((TextField)
            tree(content)
                .filter(c -> c.getId().orElse("").equals("account-first-name"))
                .findFirst()
                .orElseThrow())
        .setValue("Changed");
    ((ComboBox<Locale>)
            tree(content)
                .filter(c -> c.getId().orElse("").equals("account-language"))
                .findFirst()
                .orElseThrow())
        .setValue(Locale.ENGLISH);
    ((Button)
            tree(content)
                .filter(c -> c.getId().orElse("").equals("account-save"))
                .findFirst()
                .orElseThrow())
        .click();
    verify(profiles).save(request, new Changes(3L, "Changed", "Account", date, Locale.ENGLISH));
    assertEquals(Locale.ENGLISH, locale.get());
    assertEquals("Changed", user.getFirstName());
    assertEquals(4L, user.getVersion());
    var menu = (UserPopoverMenu) ReflectionTestUtils.getField(view, "userPopoverMenu");
    assertTrue(menu.getPopover().getElement().getTextRecursively().contains("Changed Name"));
    assertTrue(
        content
            .getElement()
            .getTextRecursively()
            .contains(
                MessageProperties.forLocale(new CustomI18NProvider(), Locale.ENGLISH)
                    .getAccountSettingsTitle()));
  }

  @Test
  void failedSaveKeepsDraftAndDoesNotChangeLanguageOrTheUserSnapshot() {
    var content = (AccountSettingsComponent) ReflectionTestUtils.getField(view, "accountSettings");
    when(profiles.save(eq(request), any())).thenThrow(new IllegalStateException("Unavailable"));
    ((TextField)
            tree(content)
                .filter(c -> c.getId().orElse("").equals("account-first-name"))
                .findFirst()
                .orElseThrow())
        .setValue("Draft");
    ((ComboBox<Locale>)
            tree(content)
                .filter(c -> c.getId().orElse("").equals("account-language"))
                .findFirst()
                .orElseThrow())
        .setValue(Locale.ENGLISH);
    ((Button)
            tree(content)
                .filter(c -> c.getId().orElse("").equals("account-save"))
                .findFirst()
                .orElseThrow())
        .click();
    assertEquals(Locale.GERMAN, locale.get());
    assertEquals("Own", user.getFirstName());
    assertEquals(Locale.GERMAN, user.getPreferredLocale());
    assertEquals(
        "Draft",
        ((TextField)
                tree(content)
                    .filter(c -> c.getId().orElse("").equals("account-first-name"))
                    .findFirst()
                    .orElseThrow())
            .getValue());
  }
}
