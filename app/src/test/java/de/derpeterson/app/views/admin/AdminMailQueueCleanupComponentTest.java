package de.derpeterson.app.views.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.service.EmailQueueCleanupService;
import de.derpeterson.app.service.EmailQueueCleanupService.CleanupResult;
import de.derpeterson.app.ui.components.PortalConfirmationDialog;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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
        .thenReturn(new CleanupResult(LocalDateTime.of(2026, 11, 15, 3, 0), 2, false));
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
    readDialogButton("confirmButton").click();
    verify(emailQueueCleanupService).runManually();
  }

  @ParameterizedTest
  @ValueSource(strings = {"cancelButton", "closeDialogButton"})
  void dismissingNeverRunsCleanupOrLeavesAnExecutableAction(String buttonFieldName) {
    openConfirmationDialog();

    readDialogButton(buttonFieldName).click();
    readDialogButton("confirmButton").click();

    assertFalse(confirmationDialog.isOpened());
    verify(emailQueueCleanupService, never()).runManually();
  }

  @Test
  void confirmationRunsCleanupOnlyOnceAndKeepsTheResult() {
    openConfirmationDialog();

    readDialogButton("confirmButton").click();
    readDialogButton("confirmButton").click();

    verify(emailQueueCleanupService, times(1)).runManually();
    assertFalse(confirmationDialog.isOpened());
    assertEquals(
        new CleanupResult(LocalDateTime.of(2026, 11, 15, 3, 0), 2, false),
        ReflectionTestUtils.getField(cleanupComponent, "latestCleanupResult"));
  }

  private void openConfirmationDialog() {
    ((Button) ReflectionTestUtils.getField(cleanupComponent, "runCleanupButton")).click();
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
        messageProperties.getAdminQueueCleanupParametersHint(),
        parametersInfoLayout.getElement().getTextRecursively());
    Component oldHintComponent =
        (Component) ReflectionTestUtils.getField(confirmationDialog, "confirmationNoticeDiv");
    assertFalse(oldHintComponent.isVisible());
  }
}
