package de.derpeterson.app.views.admin;

import static de.derpeterson.app.ConfirmDialogTestActions.click;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.PollEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.EmailQueueCleanupStatus;
import de.derpeterson.app.service.EmailQueueCleanupService;
import de.derpeterson.app.service.EmailQueueCleanupService.CleanupResult;
import de.derpeterson.app.ui.components.PortalConfirmationDialog;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

class AdminMailQueueCleanupComponentTest {
  private UI testUi;
  private VaadinSession testVaadinSession;
  private final AtomicReference<Locale> currentLocale = new AtomicReference<>(Locale.GERMAN);
  private MessageProperties messageProperties;
  private EmailQueueCleanupService emailQueueCleanupService;
  private AdminMailQueueCleanupComponent cleanupComponent;
  private PortalConfirmationDialog confirmationDialog;

  @BeforeEach
  void createCleanupConfirmationEnvironment() {
    testVaadinSession = mock(VaadinSession.class);
    when(testVaadinSession.hasLock()).thenReturn(true);
    when(testVaadinSession.getLocale()).thenAnswer(ignored -> currentLocale.get());
    VaadinSession.setCurrent(testVaadinSession);
    testUi = new UI();
    testUi.getInternals().setSession(testVaadinSession);
    UI.setCurrent(testUi);

    messageProperties = new MessageProperties(new CustomI18NProvider());
    emailQueueCleanupService = mock(EmailQueueCleanupService.class);
    when(emailQueueCleanupService.runManually())
        .thenReturn(result(EmailQueueCleanupStatus.COMPLETED, 2));
    cleanupComponent =
        new AdminMailQueueCleanupComponent(messageProperties, emailQueueCleanupService);
    testUi.add(cleanupComponent);
    confirmationDialog =
        (PortalConfirmationDialog)
            ReflectionTestUtils.getField(cleanupComponent, "cleanupConfirmationDialog");
  }

  @AfterEach
  void clearCleanupConfirmationEnvironment() {
    confirmationDialog.close();
    testUi.remove(cleanupComponent);
    NotificationHelper.getInstance().closeAndClearAllNotifications();
    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void openingShowsLocalizedConsequencesOnceWithoutStartingCleanup(String languageTag) {
    currentLocale.set(Locale.forLanguageTag(languageTag));
    cleanupComponent.refreshTexts();
    Button manualCleanupButton =
        (Button) ReflectionTestUtils.getField(cleanupComponent, "runCleanupButton");
    assertEquals(
        messageProperties.getAdminQueueCleanupManualTitle(), manualCleanupButton.getText());

    manualCleanupButton.click();

    H2 dialogTitleHeading =
        (H2) ReflectionTestUtils.getField(confirmationDialog, "dialogTitleHeading");
    assertEquals(messageProperties.getAdminQueueCleanupManualTitle(), dialogTitleHeading.getText());
    assertEquals(
        messageProperties.getAdminQueueCleanupRun(), readDialogButton("confirmButton").getText());
    assertEquals(
        messageProperties.getAdminUsersCancel(), readDialogButton("cancelButton").getText());
    assertConfirmationDetails();
    assertTrue(confirmationDialog.isOpened());
    verify(emailQueueCleanupService, never()).runManually();
  }

  @Test
  void languageChangeKeepsThePendingActionAndRefreshesBothBoxes() {
    openConfirmationDialog();
    currentLocale.set(Locale.ENGLISH);

    cleanupComponent.refreshTexts();

    assertConfirmationDetails();
    assertTrue(confirmationDialog.isOpened());
    click(readDialogButton("confirmButton"));
    verify(emailQueueCleanupService).runManually();
  }

  @ParameterizedTest
  @ValueSource(strings = {"cancelButton", "closeDialogButton"})
  void dismissingNeverRunsCleanupOrLeavesAnExecutableAction(String buttonFieldName) {
    openConfirmationDialog();

    click(readDialogButton(buttonFieldName));
    click(readDialogButton("confirmButton"));

    assertFalse(confirmationDialog.isOpened());
    verify(emailQueueCleanupService, never()).runManually();
  }

  @Test
  void confirmationRunsCleanupOnlyOnceAndKeepsTheResult() {
    openConfirmationDialog();

    click(readDialogButton("confirmButton"));
    click(readDialogButton("confirmButton"));

    verify(emailQueueCleanupService, times(1)).runManually();
    assertFalse(confirmationDialog.isOpened());
    assertEquals(
        result(EmailQueueCleanupStatus.COMPLETED, 2),
        ReflectionTestUtils.getField(cleanupComponent, "latestCleanupResult"));
  }

  private void openConfirmationDialog() {
    ((Button) ReflectionTestUtils.getField(cleanupComponent, "runCleanupButton")).click();
  }

  @ParameterizedTest
  @EnumSource(EmailQueueCleanupStatus.class)
  void statusAndExplanationStayLocalizedWithoutChangingTheRecordedOutcome(
      EmailQueueCleanupStatus status) {
    when(emailQueueCleanupService.latestResult()).thenReturn(result(status, 250));
    ((Button) ReflectionTestUtils.getField(cleanupComponent, "refreshResultButton")).click();
    Badge badge = (Badge) ReflectionTestUtils.getField(cleanupComponent, "cleanupStatusBadge");
    Span output = (Span) ReflectionTestUtils.getField(cleanupComponent, "cleanupResultSpan");
    assertEquals(status.name(), badge.getElement().getAttribute("data-status"));
    assertTrue(output.getText().contains("250"));
    assertTrue(output.getText().contains("\n"));
    String germanLabel = badge.getText();
    currentLocale.set(Locale.ENGLISH);
    cleanupComponent.refreshTexts();
    org.junit.jupiter.api.Assertions.assertNotEquals(germanLabel, badge.getText());
    assertEquals(status.name(), badge.getElement().getAttribute("data-status"));
    assertEquals(
        !status.isActive(),
        ((Button) ReflectionTestUtils.getField(cleanupComponent, "runCleanupButton")).isEnabled());
  }

  @Test
  void notYetExecutedHasNoAdditionalExplanationOrSuccessClaim() {
    Badge badge = (Badge) ReflectionTestUtils.getField(cleanupComponent, "cleanupStatusBadge");
    assertEquals(messageProperties.getAdminQueueCleanupNeverRun(), badge.getText());
    assertFalse(
        ((Span) ReflectionTestUtils.getField(cleanupComponent, "cleanupExplanationSpan"))
            .isVisible());
    verify(emailQueueCleanupService, never()).runManually();
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 500, 12000})
  void waitingRunPollsItsOwnOutcomeOnceAndRestoresThePreviousInterval(int previousInterval) {
    testUi.setPollInterval(previousInterval);
    when(emailQueueCleanupService.runManually())
        .thenReturn(result(EmailQueueCleanupStatus.WAITING, 0));
    when(emailQueueCleanupService.latestResult())
        .thenReturn(result(EmailQueueCleanupStatus.WAITING, 0));
    when(emailQueueCleanupService.resultFor(1L))
        .thenReturn(result(EmailQueueCleanupStatus.WAITING, 0));
    openConfirmationDialog();
    click(readDialogButton("confirmButton"));
    assertEquals(
        previousInterval > 0 ? Math.min(previousInterval, 1000) : 1000, testUi.getPollInterval());
    Notification waitingNotification = currentNotification();
    when(emailQueueCleanupService.resultFor(1L))
        .thenReturn(result(EmailQueueCleanupStatus.COMPLETED, 250));
    when(emailQueueCleanupService.latestResult())
        .thenReturn(result(EmailQueueCleanupStatus.COMPLETED, 250));
    ComponentUtil.fireEvent(testUi, new PollEvent(testUi, true));
    assertEquals(previousInterval, testUi.getPollInterval());
    assertFalse(waitingNotification.isOpened());
    assertTrue(currentNotification().isOpened());
    Notification completionNotification = currentNotification();
    ComponentUtil.fireEvent(testUi, new PollEvent(testUi, true));
    assertEquals(completionNotification, currentNotification());
    verify(emailQueueCleanupService, times(1)).resultFor(1L);
  }

  @Test
  void detachingAnActiveWatcherRemovesOnlyItsOwnPolling() {
    testUi.setPollInterval(12000);
    when(emailQueueCleanupService.runManually())
        .thenReturn(result(EmailQueueCleanupStatus.WAITING, 0));
    openConfirmationDialog();
    click(readDialogButton("confirmButton"));
    testUi.remove(cleanupComponent);
    assertEquals(12000, testUi.getPollInterval());
    ComponentUtil.fireEvent(testUi, new PollEvent(testUi, true));
    verify(emailQueueCleanupService, never()).resultFor(1L);
  }

  private Notification currentNotification() {
    return (Notification)
        ReflectionTestUtils.getField(NotificationHelper.getInstance(), "currentNotification");
  }

  @ParameterizedTest
  @CsvSource({
    "de,COMPLETED,0,success",
    "en,COMPLETED,250,success",
    "de,FAILED,0,error",
    "en,FAILED,0,error",
    "de,PARTIAL,100,warning",
    "en,PARTIAL,100,warning"
  })
  void manualOutcomesUseTheCorrectLocalizedNotificationIncludingAnEmptySuccessfulRun(
      String language, EmailQueueCleanupStatus status, long count, String notificationTheme) {
    currentLocale.set(Locale.forLanguageTag(language));
    testUi.getElement().getClassList().add("cosmos-reference-portal");
    when(emailQueueCleanupService.runManually()).thenReturn(result(status, count));
    openConfirmationDialog();
    assertFalse(currentNotification().isOpened());
    click(readDialogButton("confirmButton"));
    assertTrue(currentNotification().isOpened());
    assertTrue(
        currentNotification()
            .getElement()
            .getThemeList()
            .contains("cosmos-reference-" + notificationTheme));
    Supplier<?> messageSupplier =
        (Supplier<?>)
            ReflectionTestUtils.getField(NotificationHelper.getInstance(), "messageSupplier");
    String expectedMessage =
        switch (status) {
          case COMPLETED ->
              MessageFormat.format(messageProperties.getAdminQueueCleanupCompleted(), count);
          case PARTIAL ->
              MessageFormat.format(messageProperties.getAdminQueueCleanupPartialCompleted(), count);
          default -> messageProperties.getAdminQueueCleanupFailureExplanation();
        };
    assertEquals(expectedMessage, messageSupplier.get());
  }

  private CleanupResult result(EmailQueueCleanupStatus status, long count) {
    LocalDateTime requestedAt = LocalDateTime.of(2026, 11, 15, 3, 0);
    return new CleanupResult(
        1L,
        status,
        requestedAt,
        status == EmailQueueCleanupStatus.WAITING ? null : requestedAt,
        status.isActive() ? null : requestedAt.plusSeconds(1),
        count,
        false);
  }

  private Button readDialogButton(String buttonFieldName) {
    return (Button) ReflectionTestUtils.getField(confirmationDialog, buttonFieldName);
  }

  private void assertConfirmationDetails() {
    Div confirmationContextDiv =
        (Div) ReflectionTestUtils.getField(confirmationDialog, "confirmationContextDiv");
    Component confirmationDetailsLayout =
        confirmationContextDiv.getChildren().findFirst().orElseThrow();
    Component deliveryWarningLayout = confirmationDetailsLayout.getChildren().toList().get(0);
    Component parametersInfoLayout = confirmationDetailsLayout.getChildren().toList().get(1);

    assertTrue(deliveryWarningLayout.hasClassName("cosmos-security-warning"));
    assertEquals(
        messageProperties.getAdminQueueCleanupWarningTitle()
            + " "
            + messageProperties.getAdminQueueCleanupWarningDescription(),
        deliveryWarningLayout.getElement().getTextRecursively());
    assertTrue(parametersInfoLayout.hasClassName("admin-queue-cleanup-info"));
    assertEquals(
        messageProperties.getAdminQueueCleanupParametersHint()
            + messageProperties.getAdminQueueCleanupParametersScopeHint(),
        parametersInfoLayout.getElement().getTextRecursively());
    Component oldHintComponent =
        (Component) ReflectionTestUtils.getField(confirmationDialog, "confirmationNoticeDiv");
    assertFalse(oldHintComponent.isVisible());
  }
}
