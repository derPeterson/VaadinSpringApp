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
  private final AtomicReference<Locale> activeLocaleReference =
      new AtomicReference<>(Locale.GERMAN);
  private UI testUi;
  private VaadinSession vaadinSession;
  private MessageProperties messageProperties;
  private CustomI18NProvider customI18NProvider;
  private AccountSessionsComponent accountSessionsComponent;

  // Aktuelle, weitere aktive und ausschließlich gespeicherte Anmeldung
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

  // Veränderbarer Servicezustand und protokollierte Widerrufe
  private final AtomicReference<Snapshot> sessionSnapshotReference = new AtomicReference<>();
  private final List<String> revokedSessionIds = new ArrayList<>();
  private final AtomicInteger revokeOtherSessionsCallCounter = new AtomicInteger();

  @BeforeEach
  void createSessionPanelEnvironment() {
    testUi = new UI();
    UI.setCurrent(testUi);

    vaadinSession = mock(VaadinSession.class);
    when(vaadinSession.hasLock()).thenReturn(true);
    when(vaadinSession.getLocale()).thenAnswer(mockInvocation -> activeLocaleReference.get());
    VaadinSession.setCurrent(vaadinSession);
    testUi.getInternals().setSession(vaadinSession);

    customI18NProvider = new CustomI18NProvider();
    messageProperties = new MessageProperties(customI18NProvider);

    sessionSnapshotReference.set(
        new Snapshot(
            List.of(currentSessionEntry, otherSessionEntry, rememberedSessionEntry), 2, 7));
    accountSessionsComponent = new AccountSessionsComponent(messageProperties, 7);
    accountSessionsComponent.configure(
        sessionSnapshotReference::get,
        sessionHandle -> {
          revokedSessionIds.add(sessionHandle);
          sessionSnapshotReference.set(
              new Snapshot(List.of(currentSessionEntry, rememberedSessionEntry), 1, 7));
        },
        () -> {
          revokeOtherSessionsCallCounter.incrementAndGet();
          sessionSnapshotReference.set(new Snapshot(List.of(currentSessionEntry), 1, 7));
        });

    accountSessionsComponent.reloadSessionSnapshot();
  }

  @AfterEach
  void clearSessionPanelEnvironment() {
    NotificationHelper.getInstance().closeAndClearAllNotifications();

    VaadinSession.setCurrent(null);
    UI.setCurrent(null);
    testUi = null;
    vaadinSession = null;
  }

  private Stream<Component> streamComponentTree(Component rootComponent) {
    return Stream.concat(
        Stream.of(rootComponent), rootComponent.getChildren().flatMap(this::streamComponentTree));
  }

  private Button findSessionActionButton(String buttonId) {
    return streamComponentTree(accountSessionsComponent)
        .filter(candidateComponent -> candidateComponent.getId().orElse("").equals(buttonId))
        .map(Button.class::cast)
        .findFirst()
        .orElseThrow();
  }

  private void confirmPendingSessionAction() {
    PortalConfirmationDialog sessionConfirmationDialog = getSessionConfirmationDialog();

    assertTrue(sessionConfirmationDialog.isOpened());
    ((Button) ReflectionTestUtils.getField(sessionConfirmationDialog, "confirmButton")).click();

    assertFalse(sessionConfirmationDialog.isOpened());
  }

  @Test
  void cancelDoesNotRevokeAndKeepsTheSelectedEntry() {
    findSessionActionButton("session-revoke-other-handle").click();
    PortalConfirmationDialog sessionConfirmationDialog = getSessionConfirmationDialog();

    assertTrue(sessionConfirmationDialog.isOpened());
    assertTrue(revokedSessionIds.isEmpty());

    getCancelSessionConfirmationButton(sessionConfirmationDialog).click();

    assertTrue(revokedSessionIds.isEmpty());
    assertTrue(findSessionActionButton("session-revoke-other-handle").isEnabled());
    assertEquals(0, revokeOtherSessionsCallCounter.get());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void realRowsDistinguishCurrentActiveAndSavedSignInsAndTranslate(String languageTag) {
    activeLocaleReference.set(Locale.forLanguageTag(languageTag));
    accountSessionsComponent.refreshTexts();
    MessageProperties expectedMessageProperties =
        MessageProperties.forLocale(customI18NProvider, activeLocaleReference.get());
    String sessionPanelText = accountSessionsComponent.getElement().getTextRecursively();

    assertTrue(sessionPanelText.contains(expectedMessageProperties.getPortalSessionActiveCount(2)));
    Component activeSessionCountComponent =
        streamComponentTree(accountSessionsComponent)
            .filter(candidateComponent ->
                candidateComponent
                    .getElement()
                    .getClassList()
                    .contains("account-session-count-value"))
            .findFirst()
            .orElseThrow();

    assertEquals("2", activeSessionCountComponent.getElement().getText());
    assertTrue(
        sessionPanelText.contains(expectedMessageProperties.getAccountSettingsSessionsLimit(7)));
    assertTrue(sessionPanelText.contains(expectedMessageProperties.getPortalSessionRemembered()));
    assertTrue(sessionPanelText.contains(expectedMessageProperties.getSessionUnknownDevice()));
    assertEquals(
        List.of(
            expectedMessageProperties.getSessionSignedInVia(
                expectedMessageProperties.getExternalGoogle()),
            expectedMessageProperties.getSessionSignedInVia(
                expectedMessageProperties.getSignInPortalPassword())),
        streamComponentTree(accountSessionsComponent)
            .filter(candidateComponent ->
                candidateComponent
                    .getElement()
                    .getClassList()
                    .contains("account-session-sign-in-method"))
            .map(candidateComponent -> candidateComponent.getElement().getTextRecursively())
            .toList());
    assertFalse(findSessionActionButton("account-current-session").isEnabled());
    assertTrue(findSessionActionButton("session-revoke-other-handle").isEnabled());
    assertFalse(
        findSessionActionButton("account-current-session").hasClassName("cosmos-delete-button"));
    assertTrue(
        findSessionActionButton("session-revoke-other-handle")
            .hasClassName("cosmos-delete-button"));
    assertTrue(
        findSessionActionButton("session-revoke-saved-handle")
            .hasClassName("cosmos-delete-button"));
    assertEquals(
        expectedMessageProperties.getPortalSessionRevokeAction(),
        findSessionActionButton("session-revoke-saved-handle").getText());
    assertFalse(sessionPanelText.contains("Laptop · Edge"));
    assertTrue(revokedSessionIds.isEmpty());
  }

  @Test
  void selectedLogoutReloadsRowsAndShowsSuccessOnlyAfterActionSucceeded() {
    findSessionActionButton("session-revoke-other-handle").click();
    confirmPendingSessionAction();

    assertEquals(List.of("other-handle"), revokedSessionIds);
    assertFalse(
        streamComponentTree(accountSessionsComponent)
            .anyMatch(candidateComponent ->
                candidateComponent.getId().orElse("").equals("session-revoke-other-handle")));
    assertTrue(
        accountSessionsComponent
            .getElement()
            .getTextRecursively()
            .contains(messageProperties.getPortalSessionActiveCount(1)));
    Supplier<String> notificationMessageSupplier =
        (Supplier<String>)
            ReflectionTestUtils.getField(NotificationHelper.getInstance(), "messageSupplier");

    assertEquals(messageProperties.getSessionRevoked(), notificationMessageSupplier.get());
  }

  @Test
  void logoutOthersKeepsCurrentMarkerAndDisablesActionWhenNothingElseRemains() {
    findSessionActionButton("account-logout-others").click();
    confirmPendingSessionAction();

    assertEquals(1, revokeOtherSessionsCallCounter.get());
    assertFalse(findSessionActionButton("account-logout-others").isEnabled());
    assertFalse(findSessionActionButton("account-current-session").isEnabled());
    assertTrue(revokedSessionIds.isEmpty());
  }

  @Test
  void failedActionHasErrorNotificationAndDoesNotPretendRowWasRemoved() {
    accountSessionsComponent.configure(
        sessionSnapshotReference::get,
        sessionHandle -> {
          throw new IllegalStateException("internal database secret");
        },
        () -> {});
    findSessionActionButton("session-revoke-other-handle").click();
    confirmPendingSessionAction();

    assertTrue(findSessionActionButton("session-revoke-other-handle").isEnabled());
    Supplier<String> notificationMessageSupplier =
        (Supplier<String>)
            ReflectionTestUtils.getField(NotificationHelper.getInstance(), "messageSupplier");

    assertEquals(messageProperties.getSessionManagementFailed(), notificationMessageSupplier.get());
    assertFalse(notificationMessageSupplier.get().contains("internal database secret"));
  }

  @Test
  void refreshDropsStaleRowsOnExpiredAccessAndRunsTheRecoveryExitAction() {
    AtomicInteger recoveryExitCallCounter = new AtomicInteger();
    accountSessionsComponent.setFailureAction(recoveryExitCallCounter::incrementAndGet);
    accountSessionsComponent.configure(
        () -> {
          throw new IllegalStateException("expired");
        },
        sessionHandle -> {},
        () -> {});
    findSessionActionButton("account-sessions-refresh").click();

    assertEquals(1, recoveryExitCallCounter.get());
    Supplier<String> failureMessageSupplier =
        (Supplier<String>)
            ReflectionTestUtils.getField(NotificationHelper.getInstance(), "messageSupplier");

    assertEquals(messageProperties.getSessionManagementFailed(), failureMessageSupplier.get());
    assertFalse(
        streamComponentTree(accountSessionsComponent)
            .anyMatch(candidateComponent ->
                candidateComponent.getId().orElse("").equals("account-current-session")));
    assertFalse(findSessionActionButton("account-logout-others").isEnabled());
  }

  @Test
  void refreshShowsChangedSessionLimitAndLocalizedSuccessWithoutMutations() {
    sessionSnapshotReference.set(new Snapshot(List.of(currentSessionEntry), 1, 11));
    findSessionActionButton("account-sessions-refresh").click();

    assertTrue(
        accountSessionsComponent
            .getElement()
            .getTextRecursively()
            .contains(messageProperties.getAccountSettingsSessionsLimit(11)));
    assertTrue(revokedSessionIds.isEmpty());
    assertEquals(0, revokeOtherSessionsCallCounter.get());
    NotificationHelper notificationHelper = NotificationHelper.getInstance();
    Supplier<String> successTitleSupplier =
        (Supplier<String>) ReflectionTestUtils.getField(notificationHelper, "titleSupplier");

    assertEquals(messageProperties.getSessionRefreshSuccess(), successTitleSupplier.get());
    assertNull(ReflectionTestUtils.getField(notificationHelper, "messageSupplier"));

    activeLocaleReference.set(Locale.ENGLISH);
    notificationHelper.updateText();

    assertEquals(
        messageProperties.getSessionRefreshSuccess(),
        ((Span) ReflectionTestUtils.getField(notificationHelper, "titleText")).getText());
  }

  // Gerätebestätigung und Wiederherstellungsansicht
  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void everyDeviceConfirmationKeepsItsReferenceIconAndCompleteContext(String languageTag) {
    activeLocaleReference.set(Locale.forLanguageTag(languageTag));
    accountSessionsComponent.refreshTexts();

    for (Entry sessionEntry : List.of(otherSessionEntry, rememberedSessionEntry)) {
      findSessionActionButton("session-revoke-" + sessionEntry.handle()).click();
      PortalConfirmationDialog sessionConfirmationDialog = getSessionConfirmationDialog();

      assertTrue(sessionConfirmationDialog.isOpened());
      Button confirmSessionButton =
          (Button) ReflectionTestUtils.getField(sessionConfirmationDialog, "confirmButton");

      assertEquals(
          "icons/lucide/log-out.svg",
          streamComponentTree(confirmSessionButton)
              .filter(SvgIcon.class::isInstance)
              .findFirst()
              .orElseThrow()
              .getElement()
              .getAttribute("src"));
      assertEquals(
          sessionEntry.active()
              ? messageProperties.getBaseLogoutButton()
              : messageProperties.getPortalSessionRevokeAction(),
          confirmSessionButton.getText());
      Div dialogIconDiv =
          (Div) ReflectionTestUtils.getField(sessionConfirmationDialog, "dialogIconDiv");

      assertEquals(
          "icons/lucide/log-out.svg",
          dialogIconDiv.getComponentAt(0).getElement().getAttribute("src"));
      assertEquals(
          messageProperties.getPortalSessionCheckSelection(),
          getSessionConfirmationDescriptionSpan(sessionConfirmationDialog).getText());

      assertSessionConfirmationContext(sessionConfirmationDialog, sessionEntry);

      getCancelSessionConfirmationButton(sessionConfirmationDialog).click();

      assertTrue(revokedSessionIds.isEmpty());
    }

    findSessionActionButton("account-logout-others").click();
    PortalConfirmationDialog sessionConfirmationDialog = getSessionConfirmationDialog();

    assertEquals(
        messageProperties.getPortalSessionSelection(1, 1),
        getSessionConfirmationDescriptionSpan(sessionConfirmationDialog).getText());
    Component confirmationContextComponent =
        (Component)
            ReflectionTestUtils.getField(sessionConfirmationDialog, "confirmationContextDiv");

    assertEquals(
        2,
        confirmationContextComponent.getChildren().findFirst().orElseThrow().getChildren().count());
    getCancelSessionConfirmationButton(sessionConfirmationDialog).click();

    assertEquals(0, revokeOtherSessionsCallCounter.get());
  }

  @Test
  void restrictedPanelUsesReferenceCopyWithoutChangingTheNormalAccountPanel() {
    for (Locale displayLocale : List.of(Locale.GERMAN, Locale.ENGLISH)) {
      activeLocaleReference.set(displayLocale);
      accountSessionsComponent.setRestricted(true);
      String restrictedPanelText = accountSessionsComponent.getElement().getTextRecursively();

      assertTrue(restrictedPanelText.contains(messageProperties.getSessionRecoveryTitle()));
      assertTrue(restrictedPanelText.contains(messageProperties.getSessionRecoveryHint()));
      assertTrue(restrictedPanelText.contains(messageProperties.getSessionRecoveryListHint()));
      assertEquals(
          messageProperties.getSessionRecoveryLogoutAll(),
          findSessionActionButton("account-logout-others").getText());

      accountSessionsComponent.setRestricted(false);

      assertTrue(
          accountSessionsComponent
              .getElement()
              .getTextRecursively()
              .contains(messageProperties.getAccountSettingsSessionsTitle()));
      assertEquals(
          messageProperties.getAccountSettingsLogoutOthers(),
          findSessionActionButton("account-logout-others").getText());
    }
  }

  @Test
  void restrictedConfirmationDoesNotClaimAnExistingSignedInSession() {
    accountSessionsComponent.setRestricted(true);
    findSessionActionButton("session-revoke-other-handle").click();
    PortalConfirmationDialog portalConfirmationDialog = getSessionConfirmationDialog();
    String confirmationExplanationText =
        ((Span)
                ReflectionTestUtils.getField(
                    portalConfirmationDialog, "confirmationExplanationSpan"))
            .getText();

    assertTrue(
        confirmationExplanationText.contains(messageProperties.getSessionRecoveryLogoutHint()));
    assertFalse(
        confirmationExplanationText.contains(messageProperties.getPortalSessionRetainedHint()));
    getCancelSessionConfirmationButton(portalConfirmationDialog).click();

    assertTrue(revokedSessionIds.isEmpty());
  }

  private void assertSessionConfirmationContext(
      PortalConfirmationDialog sessionConfirmationDialog, Entry sessionEntry) {
    Component confirmationContextComponent =
        (Component)
            ReflectionTestUtils.getField(sessionConfirmationDialog, "confirmationContextDiv");
    String sessionMetadataText = confirmationContextComponent.getElement().getTextRecursively();

    assertTrue(
        sessionMetadataText.contains(
            sessionEntry.active()
                ? messageProperties.getSessionLastActivity()
                : messageProperties.getPortalSessionLastUsed()));
    assertTrue(
        sessionMetadataText.contains(
            sessionEntry.active()
                ? messageProperties.getSessionStarted()
                : messageProperties.getSessionSavedSince()));
    assertTrue(
        sessionMetadataText.contains(
            sessionEntry.active()
                ? messageProperties.getPortalSessionActive()
                : messageProperties.getPortalSessionInactive()));
    assertTrue(sessionMetadataText.contains(messageProperties.getPortalSessionRemembered()));
    assertFalse(
        ((Component)
                ReflectionTestUtils.getField(sessionConfirmationDialog, "confirmationNoticeDiv"))
            .isVisible());
  }

  private PortalConfirmationDialog getSessionConfirmationDialog() {
    return (PortalConfirmationDialog)
        ReflectionTestUtils.getField(accountSessionsComponent, "sessionConfirmationDialog");
  }

  private Button getCancelSessionConfirmationButton(PortalConfirmationDialog confirmationDialog) {
    return (Button) ReflectionTestUtils.getField(confirmationDialog, "cancelButton");
  }

  private Span getSessionConfirmationDescriptionSpan(PortalConfirmationDialog confirmationDialog) {
    return (Span) ReflectionTestUtils.getField(confirmationDialog, "dialogDescriptionSpan");
  }
}
