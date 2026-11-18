package de.derpeterson.app.views;

import static de.derpeterson.app.ComponentTestTree.streamComponentTree;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.service.AccountSecurityService;
import de.derpeterson.app.service.AccountSecurityService.Confirmation;
import de.derpeterson.app.service.ConfigService;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ConfirmEmailChangeViewTest {
  private UI testUi;
  private VaadinSession vaadinSession;

  private AccountSecurityService accountSecurityService;
  private MessageProperties messageProperties;
  private ConfirmEmailChangeView emailConfirmationView;

  @BeforeEach
  void createEmailConfirmationUi() {
    testUi = new UI();
    UI.setCurrent(testUi);
    vaadinSession = mock(VaadinSession.class);
    when(vaadinSession.hasLock()).thenReturn(true);
    when(vaadinSession.getLocale()).thenReturn(Locale.GERMAN);
    VaadinSession.setCurrent(vaadinSession);
    testUi.getInternals().setSession(vaadinSession);

    ConfigService configService = mock(ConfigService.class);
    when(configService.getString(any()))
        .thenAnswer(mockInvocation ->
            ((ConfigEntry) mockInvocation.getArgument(0)).getDefaultValueString());

    accountSecurityService = mock(AccountSecurityService.class);
    messageProperties = new MessageProperties(new CustomI18NProvider());
    emailConfirmationView =
        new ConfirmEmailChangeView(messageProperties, accountSecurityService, configService);
  }

  @AfterEach
  void clearEmailConfirmationUi() {
    NotificationHelper.getInstance().closeAndClearAllNotifications();
    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
    testUi = null;
    vaadinSession = null;
  }

  private Button findConfirmEmailButton() {
    return (Button)
        streamComponentTree(emailConfirmationView)
            .filter(candidateComponent ->
                candidateComponent.getId().orElse("").equals("account-email-confirm-action"))
            .findFirst()
            .orElseThrow();
  }

  private SvgIcon findConfirmationStatusSvgIcon() {
    return (SvgIcon)
        streamComponentTree(emailConfirmationView)
            .filter(candidateComponent ->
                candidateComponent.getClassNames().contains("cosmos-status-icon"))
            .findFirst()
            .orElseThrow();
  }

  @Test
  void openingTheLinkNeverChangesTheAccountAndConfirmationNeedsAnExplicitClick() {
    when(accountSecurityService.inspect("own-token")).thenReturn(Confirmation.READY);
    when(accountSecurityService.confirm("own-token")).thenReturn(Confirmation.SUCCESS);
    emailConfirmationView.setParameter(mock(BeforeEvent.class), "own-token");

    verify(accountSecurityService, never()).confirm(any());
    assertTrue(findConfirmEmailButton().isVisible());
    Component confirmationHintComponent =
        streamComponentTree(emailConfirmationView)
            .filter(candidateComponent ->
                candidateComponent.getId().orElse("").equals("account-email-confirm-hint"))
            .findFirst()
            .orElseThrow();

    assertTrue(confirmationHintComponent.isVisible());
    assertTrue(
        confirmationHintComponent
            .getElement()
            .getTextRecursively()
            .contains(messageProperties.getPublicEmailConfirmationHint()));
    assertTrue(
        emailConfirmationView
            .getElement()
            .getTextRecursively()
            .contains(messageProperties.getPublicEmailFormTitle()));
    assertEquals(
        "icons/lucide/mail.svg", findConfirmationStatusSvgIcon().getElement().getAttribute("src"));

    findConfirmEmailButton().click();

    verify(accountSecurityService).confirm("own-token");
    assertFalse(findConfirmEmailButton().isVisible());
    assertFalse(confirmationHintComponent.isVisible());
    assertEquals(
        "icons/lucide/circle-check.svg",
        findConfirmationStatusSvgIcon().getElement().getAttribute("src"));
    assertTrue(findConfirmationStatusSvgIcon().hasClassName("cosmos-text-success"));
    assertTrue(
        emailConfirmationView
            .getElement()
            .getTextRecursively()
            .contains(messageProperties.getAccountEmailConfirmationSuccess()));
  }

  @ParameterizedTest
  @EnumSource(
      value = Confirmation.class,
      names = {"INVALID", "EXPIRED", "CONFLICT"})
  void unavailableLinksNeverOfferAnAction(Confirmation confirmationState) {
    when(accountSecurityService.inspect(any())).thenReturn(confirmationState);
    emailConfirmationView.setParameter(mock(BeforeEvent.class), "own-token");

    assertFalse(findConfirmEmailButton().isVisible());
    assertEquals(
        confirmationState == Confirmation.EXPIRED
            ? "icons/lucide/triangle-alert.svg"
            : "icons/lucide/circle-x.svg",
        findConfirmationStatusSvgIcon().getElement().getAttribute("src"));
    assertTrue(
        findConfirmationStatusSvgIcon()
            .hasClassName(
                confirmationState == Confirmation.EXPIRED
                    ? "cosmos-text-warning"
                    : "cosmos-text-error"));
    assertEquals(
        confirmationState == Confirmation.EXPIRED,
        findConfirmationStatusSvgIcon().hasClassName("cosmos-status-icon--warning"));
    verify(accountSecurityService, never()).confirm(any());
  }

  @Test
  void technicalFailureIsLocalizedAndDoesNotExposeBackendDetails() {
    when(accountSecurityService.inspect(any()))
        .thenThrow(new IllegalStateException("secret SQL details"));
    emailConfirmationView.setParameter(mock(BeforeEvent.class), "own-token");

    assertFalse(findConfirmEmailButton().isVisible());
    assertEquals(
        "icons/lucide/circle-x.svg",
        findConfirmationStatusSvgIcon().getElement().getAttribute("src"));
    assertTrue(findConfirmationStatusSvgIcon().hasClassName("cosmos-text-error"));
    String confirmationPageText = emailConfirmationView.getElement().getTextRecursively();

    assertTrue(
        confirmationPageText.contains(messageProperties.getAccountEmailConfirmationFailed()));
    assertFalse(confirmationPageText.contains("secret SQL"));
  }
}
