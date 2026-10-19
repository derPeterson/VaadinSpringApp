package de.derpeterson.app.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.security.DeviceDescription;
import de.derpeterson.app.security.SessionManagementService.Entry;
import de.derpeterson.app.security.SessionManagementService.Snapshot;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

class AccountSessionsComponentTest {
  private final AtomicReference<Locale> locale = new AtomicReference<>(Locale.GERMAN);
  private UI ui;
  private VaadinSession session;
  private MessageProperties texts;
  private CustomI18NProvider provider;
  private AccountSessionsComponent component;
  private final Instant when = Instant.parse("2026-10-17T02:00:00Z");
  private final Entry current =
      new Entry(
          "current-handle",
          DeviceDescription.from("Windows Chrome/130"),
          when,
          when,
          true,
          true,
          true);
  private final Entry other =
      new Entry(
          "other-handle",
          DeviceDescription.from("Android Mobile Firefox/130"),
          when,
          when,
          false,
          true,
          true);
  private final Entry saved =
      new Entry("saved-handle", DeviceDescription.from(null), when, when, false, false, true);
  private final AtomicReference<Snapshot> snapshot = new AtomicReference<>();
  private final List<String> revoked = new ArrayList<>();
  private final AtomicInteger all = new AtomicInteger();

  @BeforeEach
  void setup() {
    ui = new UI();
    UI.setCurrent(ui);
    session = mock(VaadinSession.class);
    when(session.hasLock()).thenReturn(true);
    when(session.getLocale()).thenAnswer(call -> locale.get());
    VaadinSession.setCurrent(session);
    ui.getInternals().setSession(session);
    provider = new CustomI18NProvider();
    texts = new MessageProperties(provider);
    snapshot.set(new Snapshot(List.of(current, other, saved), 2, 7));
    component = new AccountSessionsComponent(texts, 7);
    component.configure(
        snapshot::get,
        handle -> {
          revoked.add(handle);
          snapshot.set(new Snapshot(List.of(current, saved), 1, 7));
        },
        () -> {
          all.incrementAndGet();
          snapshot.set(new Snapshot(List.of(current), 1, 7));
        });
    component.reload();
  }

  @AfterEach
  void cleanup() {
    NotificationHelper.getInstance().closeAndClearAllNotifications();
    VaadinSession.setCurrent(null);
    UI.setCurrent(null);
    ui = null;
    session = null;
  }

  private Stream<Component> tree(Component root) {
    return Stream.concat(Stream.of(root), root.getChildren().flatMap(this::tree));
  }

  private Button button(String id) {
    return tree(component)
        .filter(item -> item.getId().orElse("").equals(id))
        .map(Button.class::cast)
        .findFirst()
        .orElseThrow();
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void realRowsDistinguishCurrentActiveAndSavedSignInsAndTranslate(String language) {
    locale.set(Locale.forLanguageTag(language));
    component.refreshTexts();
    MessageProperties expected = MessageProperties.forLocale(provider, locale.get());
    String content = component.getElement().getTextRecursively();
    assertTrue(content.contains("2 " + expected.getAccountSettingsSessionsActiveTitle()));
    assertTrue(content.contains(expected.getAccountSettingsSessionsLimit(7)));
    assertTrue(content.contains(expected.getSessionSavedLogin()));
    assertTrue(content.contains(expected.getSessionUnknownDevice()));
    assertFalse(button("account-current-session").isEnabled());
    assertTrue(button("session-revoke-other-handle").isEnabled());
    assertEquals(expected.getSessionRevokeSaved(), button("session-revoke-saved-handle").getText());
    assertFalse(content.contains("Laptop · Edge"));
    assertTrue(revoked.isEmpty());
  }

  @Test
  void selectedLogoutReloadsRowsAndShowsSuccessOnlyAfterActionSucceeded() {
    button("session-revoke-other-handle").click();
    assertEquals(List.of("other-handle"), revoked);
    assertFalse(
        tree(component)
            .anyMatch(item -> item.getId().orElse("").equals("session-revoke-other-handle")));
    assertTrue(
        component
            .getElement()
            .getTextRecursively()
            .contains("1 " + texts.getAccountSettingsSessionsActiveTitle()));
    Supplier<String> supplier =
        (Supplier<String>)
            ReflectionTestUtils.getField(NotificationHelper.getInstance(), "messageSupplier");
    assertEquals(texts.getSessionRevoked(), supplier.get());
  }

  @Test
  void logoutOthersKeepsCurrentMarkerAndDisablesActionWhenNothingElseRemains() {
    button("account-logout-others").click();
    assertEquals(1, all.get());
    assertFalse(button("account-logout-others").isEnabled());
    assertFalse(button("account-current-session").isEnabled());
    assertTrue(revoked.isEmpty());
  }

  @Test
  void failedActionHasErrorNotificationAndDoesNotPretendRowWasRemoved() {
    component.configure(
        snapshot::get,
        handle -> {
          throw new IllegalStateException("internal database secret");
        },
        () -> {});
    button("session-revoke-other-handle").click();
    assertTrue(button("session-revoke-other-handle").isEnabled());
    Supplier<String> supplier =
        (Supplier<String>)
            ReflectionTestUtils.getField(NotificationHelper.getInstance(), "messageSupplier");
    assertEquals(texts.getSessionManagementFailed(), supplier.get());
    assertFalse(supplier.get().contains("internal database secret"));
  }

  @Test
  void refreshDropsStaleRowsOnExpiredAccessAndRunsTheRecoveryExitAction() {
    AtomicInteger exited = new AtomicInteger();
    component.setFailureAction(exited::incrementAndGet);
    component.configure(
        () -> {
          throw new IllegalStateException("expired");
        },
        handle -> {},
        () -> {});
    component.reload();
    assertEquals(1, exited.get());
    assertFalse(
        tree(component)
            .anyMatch(item -> item.getId().orElse("").equals("account-current-session")));
    assertFalse(button("account-logout-others").isEnabled());
  }

  @Test
  void refreshReflectsChangedSessionLimitAndLatestActivityWithoutMutations() {
    snapshot.set(new Snapshot(List.of(current), 1, 11));
    button("account-sessions-refresh").click();
    assertTrue(
        component
            .getElement()
            .getTextRecursively()
            .contains(texts.getAccountSettingsSessionsLimit(11)));
    assertTrue(revoked.isEmpty());
    assertEquals(0, all.get());
  }
}
