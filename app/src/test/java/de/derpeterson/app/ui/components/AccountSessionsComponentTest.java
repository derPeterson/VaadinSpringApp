package de.derpeterson.app.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.security.DeviceDescription;
import de.derpeterson.app.security.SessionManagementService.Entry;
import de.derpeterson.app.security.SessionManagementService.Snapshot;
import de.derpeterson.app.security.SignInMethod;
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
          true,
          null,
          SignInMethod.GOOGLE);
  private final Entry other =
      new Entry(
          "other-handle",
          DeviceDescription.from("Android Mobile Firefox/130"),
          when,
          when,
          false,
          true,
          true,
          null,
          SignInMethod.PORTAL);
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

  private void confirmPending() {
    PortalConfirmationDialog confirmation =
        (PortalConfirmationDialog) ReflectionTestUtils.getField(component, "confirmation");
    assertTrue(confirmation.isOpened());
    ((Button) ReflectionTestUtils.getField(confirmation, "confirm")).click();
    assertFalse(confirmation.isOpened());
  }

  @Test
  void cancelDoesNotRevokeAndKeepsTheSelectedEntry() {
    button("session-revoke-other-handle").click();
    PortalConfirmationDialog confirmation =
        (PortalConfirmationDialog) ReflectionTestUtils.getField(component, "confirmation");
    assertTrue(confirmation.isOpened());
    assertTrue(revoked.isEmpty());
    ((Button) ReflectionTestUtils.getField(confirmation, "cancel")).click();
    assertTrue(revoked.isEmpty());
    assertTrue(button("session-revoke-other-handle").isEnabled());
    assertEquals(0, all.get());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void realRowsDistinguishCurrentActiveAndSavedSignInsAndTranslate(String language) {
    locale.set(Locale.forLanguageTag(language));
    component.refreshTexts();
    MessageProperties expected = MessageProperties.forLocale(provider, locale.get());
    String content = component.getElement().getTextRecursively();
    assertTrue(content.contains(expected.getPortalSessionActiveCount(2)));
    Component countValue =
        tree(component)
            .filter(
                item -> item.getElement().getClassList().contains("account-session-count-value"))
            .findFirst()
            .orElseThrow();
    assertEquals("2", countValue.getElement().getText());
    assertTrue(content.contains(expected.getAccountSettingsSessionsLimit(7)));
    assertTrue(content.contains(expected.getPortalSessionRemembered()));
    assertTrue(content.contains(expected.getSessionUnknownDevice()));
    assertEquals(
        List.of(
            expected.getSessionSignedInVia(expected.getExternalGoogle()),
            expected.getSessionSignedInVia(expected.getSignInPortalPassword())),
        tree(component)
            .filter(
                item -> item.getElement().getClassList().contains("account-session-sign-in-method"))
            .map(item -> item.getElement().getText())
            .toList());
    assertFalse(button("account-current-session").isEnabled());
    assertTrue(button("session-revoke-other-handle").isEnabled());
    assertFalse(button("account-current-session").hasClassName("cosmos-delete-button"));
    assertTrue(button("session-revoke-other-handle").hasClassName("cosmos-delete-button"));
    assertTrue(button("session-revoke-saved-handle").hasClassName("cosmos-delete-button"));
    assertEquals(
        expected.getPortalSessionRevokeAction(), button("session-revoke-saved-handle").getText());
    assertFalse(content.contains("Laptop · Edge"));
    assertTrue(revoked.isEmpty());
  }

  @Test
  void selectedLogoutReloadsRowsAndShowsSuccessOnlyAfterActionSucceeded() {
    button("session-revoke-other-handle").click();
    confirmPending();
    assertEquals(List.of("other-handle"), revoked);
    assertFalse(
        tree(component)
            .anyMatch(item -> item.getId().orElse("").equals("session-revoke-other-handle")));
    assertTrue(
        component.getElement().getTextRecursively().contains(texts.getPortalSessionActiveCount(1)));
    Supplier<String> supplier =
        (Supplier<String>)
            ReflectionTestUtils.getField(NotificationHelper.getInstance(), "messageSupplier");
    assertEquals(texts.getSessionRevoked(), supplier.get());
  }

  @Test
  void logoutOthersKeepsCurrentMarkerAndDisablesActionWhenNothingElseRemains() {
    button("account-logout-others").click();
    confirmPending();
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
    confirmPending();
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
    button("account-sessions-refresh").click();
    assertEquals(1, exited.get());
    Supplier<String> failureText =
        (Supplier<String>)
            ReflectionTestUtils.getField(NotificationHelper.getInstance(), "messageSupplier");
    assertEquals(texts.getSessionManagementFailed(), failureText.get());
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
    NotificationHelper helper = NotificationHelper.getInstance();
    Supplier<String> successText =
        (Supplier<String>) ReflectionTestUtils.getField(helper, "titleSupplier");
    assertEquals(texts.getSessionRefreshSuccess(), successText.get());
    assertNull(ReflectionTestUtils.getField(helper, "messageSupplier"));
    locale.set(Locale.ENGLISH);
    helper.updateText();
    assertEquals(
        texts.getSessionRefreshSuccess(),
        ((Span) ReflectionTestUtils.getField(helper, "titleText")).getText());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void everyDeviceConfirmationKeepsItsReferenceIconAndCompleteContext(String language) {
    locale.set(Locale.forLanguageTag(language));
    component.refreshTexts();
    for (Entry entry : List.of(other, saved)) {
      button("session-revoke-" + entry.handle()).click();
      PortalConfirmationDialog dialog =
          (PortalConfirmationDialog) ReflectionTestUtils.getField(component, "confirmation");
      assertTrue(dialog.isOpened());
      Button confirm = (Button) ReflectionTestUtils.getField(dialog, "confirm");
      assertEquals(
          "icons/lucide/log-out.svg",
          tree(confirm)
              .filter(SvgIcon.class::isInstance)
              .findFirst()
              .orElseThrow()
              .getElement()
              .getAttribute("src"));
      assertEquals(
          entry.active() ? texts.getBaseLogoutButton() : texts.getPortalSessionRevokeAction(),
          confirm.getText());
      Div symbol = (Div) ReflectionTestUtils.getField(dialog, "symbol");
      assertEquals(
          "icons/lucide/log-out.svg", symbol.getComponentAt(0).getElement().getAttribute("src"));
      assertEquals(
          texts.getPortalSessionCheckSelection(),
          ((Span) ReflectionTestUtils.getField(dialog, "description")).getText());
      Component context = (Component) ReflectionTestUtils.getField(dialog, "context");
      String metadata = context.getElement().getTextRecursively();
      assertTrue(
          metadata.contains(
              entry.active() ? texts.getSessionLastActivity() : texts.getPortalSessionLastUsed()));
      assertTrue(
          metadata.contains(
              entry.active() ? texts.getSessionStarted() : texts.getSessionSavedSince()));
      assertTrue(
          metadata.contains(
              entry.active() ? texts.getPortalSessionActive() : texts.getPortalSessionInactive()));
      assertTrue(metadata.contains(texts.getPortalSessionRemembered()));
      assertFalse(((Component) ReflectionTestUtils.getField(dialog, "note")).isVisible());
      ((Button) ReflectionTestUtils.getField(dialog, "cancel")).click();
      assertTrue(revoked.isEmpty());
    }
    button("account-logout-others").click();
    PortalConfirmationDialog dialog =
        (PortalConfirmationDialog) ReflectionTestUtils.getField(component, "confirmation");
    assertEquals(
        texts.getPortalSessionSelection(1, 1),
        ((Span) ReflectionTestUtils.getField(dialog, "description")).getText());
    Component context = (Component) ReflectionTestUtils.getField(dialog, "context");
    assertEquals(2, context.getChildren().findFirst().orElseThrow().getChildren().count());
    ((Button) ReflectionTestUtils.getField(dialog, "cancel")).click();
    assertEquals(0, all.get());
  }

  @Test
  void restrictedPanelUsesReferenceCopyWithoutChangingTheNormalAccountPanel() {
    for (Locale language : List.of(Locale.GERMAN, Locale.ENGLISH)) {
      locale.set(language);
      component.setRestricted(true);
      String restrictedCopy = component.getElement().getTextRecursively();
      assertTrue(restrictedCopy.contains(texts.getSessionRecoveryTitle()));
      assertTrue(restrictedCopy.contains(texts.getSessionRecoveryHint()));
      assertTrue(restrictedCopy.contains(texts.getSessionRecoveryListHint()));
      assertEquals(texts.getSessionRecoveryLogoutAll(), button("account-logout-others").getText());
      component.setRestricted(false);
      assertTrue(
          component
              .getElement()
              .getTextRecursively()
              .contains(texts.getAccountSettingsSessionsTitle()));
      assertEquals(
          texts.getAccountSettingsLogoutOthers(), button("account-logout-others").getText());
    }
  }

  @Test
  void restrictedConfirmationDoesNotClaimAnExistingSignedInSession() {
    component.setRestricted(true);
    button("session-revoke-other-handle").click();
    PortalConfirmationDialog dialog =
        (PortalConfirmationDialog) ReflectionTestUtils.getField(component, "confirmation");
    String explanation = ((Span) ReflectionTestUtils.getField(dialog, "explanation")).getText();
    assertTrue(explanation.contains(texts.getSessionRecoveryLogoutHint()));
    assertFalse(explanation.contains(texts.getPortalSessionRetainedHint()));
    ((Button) ReflectionTestUtils.getField(dialog, "cancel")).click();
    assertTrue(revoked.isEmpty());
  }
}
