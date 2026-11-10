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
  private final AtomicReference<Locale> localeReference = new AtomicReference<>(Locale.GERMAN);
  private UI testUi;
  private VaadinSession vaadinSession;
  private MessageProperties messageProperties;
  private CustomI18NProvider i18nProvider;
  private AccountSessionsComponent accountSessionsComponent;
  private final Instant sessionTimestamp = Instant.parse("2026-10-17T02:00:00Z");
  private final Entry currentSessionEntry =
      new Entry(
          "current-handle",
          DeviceDescription.from("Windows Chrome/130"),
          sessionTimestamp,
          sessionTimestamp,
          true,
          true,
          true,
          null,
          SignInMethod.GOOGLE);
  private final Entry otherSessionEntry =
      new Entry(
          "other-handle",
          DeviceDescription.from("Android Mobile Firefox/130"),
          sessionTimestamp,
          sessionTimestamp,
          false,
          true,
          true,
          null,
          SignInMethod.PORTAL);
  private final Entry rememberedSessionEntry =
      new Entry(
          "saved-handle",
          DeviceDescription.from(null),
          sessionTimestamp,
          sessionTimestamp,
          false,
          false,
          true);
  private final AtomicReference<Snapshot> sessionsSnapshot = new AtomicReference<>();
  private final List<String> revokedSessionIds = new ArrayList<>();
  private final AtomicInteger revokeOthersCallCount = new AtomicInteger();

  @BeforeEach
  void setup() {
    testUi = new UI();
    UI.setCurrent(testUi);
    vaadinSession = mock(VaadinSession.class);
    when(vaadinSession.hasLock()).thenReturn(true);
    when(vaadinSession.getLocale()).thenAnswer(call -> localeReference.get());
    VaadinSession.setCurrent(vaadinSession);
    testUi.getInternals().setSession(vaadinSession);
    i18nProvider = new CustomI18NProvider();
    messageProperties = new MessageProperties(i18nProvider);
    sessionsSnapshot.set(
        new Snapshot(
            List.of(currentSessionEntry, otherSessionEntry, rememberedSessionEntry), 2, 7));
    accountSessionsComponent = new AccountSessionsComponent(messageProperties, 7);
    accountSessionsComponent.configure(
        sessionsSnapshot::get,
        handle -> {
          revokedSessionIds.add(handle);
          sessionsSnapshot.set(
              new Snapshot(List.of(currentSessionEntry, rememberedSessionEntry), 1, 7));
        },
        () -> {
          revokeOthersCallCount.incrementAndGet();
          sessionsSnapshot.set(new Snapshot(List.of(currentSessionEntry), 1, 7));
        });
    accountSessionsComponent.reloadSessionSnapshot();
  }

  @AfterEach
  void cleanup() {
    NotificationHelper.getInstance().closeAndClearAllNotifications();
    VaadinSession.setCurrent(null);
    UI.setCurrent(null);
    testUi = null;
    vaadinSession = null;
  }

  private Stream<Component> tree(Component root) {
    return Stream.concat(Stream.of(root), root.getChildren().flatMap(this::tree));
  }

  private Button button(String id) {
    return tree(accountSessionsComponent)
        .filter(item -> item.getId().orElse("").equals(id))
        .map(Button.class::cast)
        .findFirst()
        .orElseThrow();
  }

  private void confirmPending() {
    PortalConfirmationDialog confirmation = getSessionConfirmationDialog();

    assertTrue(confirmation.isOpened());
    ((Button) ReflectionTestUtils.getField(confirmation, "confirmButton")).click();

    assertFalse(confirmation.isOpened());
  }

  @Test
  void cancelDoesNotRevokeAndKeepsTheSelectedEntry() {
    button("session-revoke-other-handle").click();
    PortalConfirmationDialog confirmation = getSessionConfirmationDialog();

    assertTrue(confirmation.isOpened());
    assertTrue(revokedSessionIds.isEmpty());
    getCancelSessionConfirmationButton(confirmation).click();

    assertTrue(revokedSessionIds.isEmpty());
    assertTrue(button("session-revoke-other-handle").isEnabled());
    assertEquals(0, revokeOthersCallCount.get());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void realRowsDistinguishCurrentActiveAndSavedSignInsAndTranslate(String language) {
    localeReference.set(Locale.forLanguageTag(language));
    accountSessionsComponent.refreshTexts();
    MessageProperties expected = MessageProperties.forLocale(i18nProvider, localeReference.get());
    String content = accountSessionsComponent.getElement().getTextRecursively();

    assertTrue(content.contains(expected.getPortalSessionActiveCount(2)));
    Component countValue =
        tree(accountSessionsComponent)
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
        tree(accountSessionsComponent)
            .filter(
                item -> item.getElement().getClassList().contains("account-session-sign-in-method"))
            .map(item -> item.getElement().getTextRecursively())
            .toList());
    assertFalse(button("account-current-session").isEnabled());
    assertTrue(button("session-revoke-other-handle").isEnabled());
    assertFalse(button("account-current-session").hasClassName("cosmos-delete-button"));
    assertTrue(button("session-revoke-other-handle").hasClassName("cosmos-delete-button"));
    assertTrue(button("session-revoke-saved-handle").hasClassName("cosmos-delete-button"));
    assertEquals(
        expected.getPortalSessionRevokeAction(), button("session-revoke-saved-handle").getText());
    assertFalse(content.contains("Laptop · Edge"));
    assertTrue(revokedSessionIds.isEmpty());
  }

  @Test
  void selectedLogoutReloadsRowsAndShowsSuccessOnlyAfterActionSucceeded() {
    button("session-revoke-other-handle").click();
    confirmPending();

    assertEquals(List.of("other-handle"), revokedSessionIds);
    assertFalse(
        tree(accountSessionsComponent)
            .anyMatch(item -> item.getId().orElse("").equals("session-revoke-other-handle")));
    assertTrue(
        accountSessionsComponent
            .getElement()
            .getTextRecursively()
            .contains(messageProperties.getPortalSessionActiveCount(1)));
    Supplier<String> supplier =
        (Supplier<String>)
            ReflectionTestUtils.getField(NotificationHelper.getInstance(), "messageSupplier");

    assertEquals(messageProperties.getSessionRevoked(), supplier.get());
  }

  @Test
  void logoutOthersKeepsCurrentMarkerAndDisablesActionWhenNothingElseRemains() {
    button("account-logout-others").click();
    confirmPending();

    assertEquals(1, revokeOthersCallCount.get());
    assertFalse(button("account-logout-others").isEnabled());
    assertFalse(button("account-current-session").isEnabled());
    assertTrue(revokedSessionIds.isEmpty());
  }

  @Test
  void failedActionHasErrorNotificationAndDoesNotPretendRowWasRemoved() {
    accountSessionsComponent.configure(
        sessionsSnapshot::get,
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

    assertEquals(messageProperties.getSessionManagementFailed(), supplier.get());
    assertFalse(supplier.get().contains("internal database secret"));
  }

  @Test
  void refreshDropsStaleRowsOnExpiredAccessAndRunsTheRecoveryExitAction() {
    AtomicInteger exited = new AtomicInteger();
    accountSessionsComponent.setFailureAction(exited::incrementAndGet);
    accountSessionsComponent.configure(
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

    assertEquals(messageProperties.getSessionManagementFailed(), failureText.get());
    assertFalse(
        tree(accountSessionsComponent)
            .anyMatch(item -> item.getId().orElse("").equals("account-current-session")));
    assertFalse(button("account-logout-others").isEnabled());
  }

  @Test
  void refreshReflectsChangedSessionLimitAndLatestActivityWithoutMutations() {
    sessionsSnapshot.set(new Snapshot(List.of(currentSessionEntry), 1, 11));
    button("account-sessions-refresh").click();

    assertTrue(
        accountSessionsComponent
            .getElement()
            .getTextRecursively()
            .contains(messageProperties.getAccountSettingsSessionsLimit(11)));
    assertTrue(revokedSessionIds.isEmpty());
    assertEquals(0, revokeOthersCallCount.get());
    NotificationHelper helper = NotificationHelper.getInstance();
    Supplier<String> successText =
        (Supplier<String>) ReflectionTestUtils.getField(helper, "titleSupplier");

    assertEquals(messageProperties.getSessionRefreshSuccess(), successText.get());
    assertNull(ReflectionTestUtils.getField(helper, "messageSupplier"));
    localeReference.set(Locale.ENGLISH);
    helper.updateText();

    assertEquals(
        messageProperties.getSessionRefreshSuccess(),
        ((Span) ReflectionTestUtils.getField(helper, "titleText")).getText());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void everyDeviceConfirmationKeepsItsReferenceIconAndCompleteContext(String language) {
    localeReference.set(Locale.forLanguageTag(language));
    accountSessionsComponent.refreshTexts();
    for (Entry entry : List.of(otherSessionEntry, rememberedSessionEntry)) {
      button("session-revoke-" + entry.handle()).click();
      PortalConfirmationDialog dialog = getSessionConfirmationDialog();

      assertTrue(dialog.isOpened());
      Button confirm = (Button) ReflectionTestUtils.getField(dialog, "confirmButton");

      assertEquals(
          "icons/lucide/log-out.svg",
          tree(confirm)
              .filter(SvgIcon.class::isInstance)
              .findFirst()
              .orElseThrow()
              .getElement()
              .getAttribute("src"));
      assertEquals(
          entry.active()
              ? messageProperties.getBaseLogoutButton()
              : messageProperties.getPortalSessionRevokeAction(),
          confirm.getText());
      Div symbol = (Div) ReflectionTestUtils.getField(dialog, "dialogIconContainer");

      assertEquals(
          "icons/lucide/log-out.svg", symbol.getComponentAt(0).getElement().getAttribute("src"));
      assertEquals(
          messageProperties.getPortalSessionCheckSelection(),
          getSessionConfirmationDescriptionSpan(dialog).getText());
      Component context = (Component) ReflectionTestUtils.getField(dialog, "confirmationContext");
      String metadata = context.getElement().getTextRecursively();

      assertTrue(
          metadata.contains(
              entry.active()
                  ? messageProperties.getSessionLastActivity()
                  : messageProperties.getPortalSessionLastUsed()));
      assertTrue(
          metadata.contains(
              entry.active()
                  ? messageProperties.getSessionStarted()
                  : messageProperties.getSessionSavedSince()));
      assertTrue(
          metadata.contains(
              entry.active()
                  ? messageProperties.getPortalSessionActive()
                  : messageProperties.getPortalSessionInactive()));
      assertTrue(metadata.contains(messageProperties.getPortalSessionRemembered()));
      assertFalse(
          ((Component) ReflectionTestUtils.getField(dialog, "confirmationNotice")).isVisible());
      getCancelSessionConfirmationButton(dialog).click();

      assertTrue(revokedSessionIds.isEmpty());
    }
    button("account-logout-others").click();
    PortalConfirmationDialog dialog = getSessionConfirmationDialog();

    assertEquals(
        messageProperties.getPortalSessionSelection(1, 1),
        getSessionConfirmationDescriptionSpan(dialog).getText());
    Component context = (Component) ReflectionTestUtils.getField(dialog, "confirmationContext");

    assertEquals(2, context.getChildren().findFirst().orElseThrow().getChildren().count());
    getCancelSessionConfirmationButton(dialog).click();

    assertEquals(0, revokeOthersCallCount.get());
  }

  @Test
  void restrictedPanelUsesReferenceCopyWithoutChangingTheNormalAccountPanel() {
    for (Locale language : List.of(Locale.GERMAN, Locale.ENGLISH)) {
      localeReference.set(language);
      accountSessionsComponent.setRestricted(true);
      String restrictedCopy = accountSessionsComponent.getElement().getTextRecursively();

      assertTrue(restrictedCopy.contains(messageProperties.getSessionRecoveryTitle()));
      assertTrue(restrictedCopy.contains(messageProperties.getSessionRecoveryHint()));
      assertTrue(restrictedCopy.contains(messageProperties.getSessionRecoveryListHint()));
      assertEquals(
          messageProperties.getSessionRecoveryLogoutAll(),
          button("account-logout-others").getText());
      accountSessionsComponent.setRestricted(false);

      assertTrue(
          accountSessionsComponent
              .getElement()
              .getTextRecursively()
              .contains(messageProperties.getAccountSettingsSessionsTitle()));
      assertEquals(
          messageProperties.getAccountSettingsLogoutOthers(),
          button("account-logout-others").getText());
    }
  }

  @Test
  void restrictedConfirmationDoesNotClaimAnExistingSignedInSession() {
    accountSessionsComponent.setRestricted(true);
    button("session-revoke-other-handle").click();
    PortalConfirmationDialog portalConfirmationDialog = getSessionConfirmationDialog();
    String explanation =
        ((Span) ReflectionTestUtils.getField(portalConfirmationDialog, "confirmationExplanation"))
            .getText();

    assertTrue(explanation.contains(messageProperties.getSessionRecoveryLogoutHint()));
    assertFalse(explanation.contains(messageProperties.getPortalSessionRetainedHint()));
    getCancelSessionConfirmationButton(portalConfirmationDialog).click();

    assertTrue(revokedSessionIds.isEmpty());
  }

  private PortalConfirmationDialog getSessionConfirmationDialog() {
    return (PortalConfirmationDialog)
        ReflectionTestUtils.getField(accountSessionsComponent, "sessionConfirmationDialog");
  }

  private Button getCancelSessionConfirmationButton(PortalConfirmationDialog confirmationDialog) {
    return (Button) ReflectionTestUtils.getField(confirmationDialog, "cancelButton");
  }

  private Span getSessionConfirmationDescriptionSpan(PortalConfirmationDialog confirmationDialog) {
    return (Span) ReflectionTestUtils.getField(confirmationDialog, "dialogDescription");
  }
}
