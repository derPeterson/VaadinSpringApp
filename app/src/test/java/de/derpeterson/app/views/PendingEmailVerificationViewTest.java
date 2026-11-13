package de.derpeterson.app.views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.security.PendingEmailVerificationFlow;
import de.derpeterson.app.service.ConfigService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Native View und echte Texte, ohne Browser oder laufende Anwendung. */
class PendingEmailVerificationViewTest {
  private final PendingEmailVerificationFlow pendingVerificationFlow =
      mock(PendingEmailVerificationFlow.class);
  private final HttpServletRequest httpRequest = mock(HttpServletRequest.class);
  private MessageProperties messageProperties;

  // Vaadin hält die aktuelle UI nur schwach. Das Fixture hält sie bis zum Testende fest.
  private UI testUi;
  private VaadinSession vaadinSession;

  private PendingEmailVerificationView createVerificationView(String languageCode) {
    vaadinSession = mock(VaadinSession.class);
    when(vaadinSession.hasLock()).thenReturn(true);
    DeploymentConfiguration deploymentConfiguration = mock(DeploymentConfiguration.class);
    when(vaadinSession.getConfiguration()).thenReturn(deploymentConfiguration);
    when(vaadinSession.getLocale()).thenReturn(Locale.forLanguageTag(languageCode));
    VaadinSession.setCurrent(vaadinSession);
    testUi = new UI();
    UI.setCurrent(testUi);
    testUi.getInternals().setSession(vaadinSession);

    ConfigService configService = mock(ConfigService.class);
    when(configService.getString(ConfigEntry.SERVICE_NAME)).thenReturn("Test Service");
    when(configService.getString(ConfigEntry.COPYRIGHT_TEXT)).thenReturn("");
    messageProperties = new MessageProperties(new CustomI18NProvider());
    return new PendingEmailVerificationView(
        messageProperties, pendingVerificationFlow, httpRequest, configService);
  }

  @AfterEach
  void clearUiEnvironment() {
    NotificationHelper.getInstance().closeAndClearAllNotifications();
    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
    testUi = null;
    vaadinSession = null;
  }

  private Stream<Component> componentTree(Component rootComponent) {
    return Stream.concat(
        Stream.of(rootComponent), rootComponent.getChildren().flatMap(this::componentTree));
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void missingProofReturnsToLoginWithoutDisplayingAnAddress(String languageCode) {
    PendingEmailVerificationView verificationView = createVerificationView(languageCode);
    when(pendingVerificationFlow.currentAccount(httpRequest)).thenReturn(Optional.empty());
    BeforeEnterEvent beforeEnterEvent = mock(BeforeEnterEvent.class);

    verificationView.beforeEnter(beforeEnterEvent);

    verify(beforeEnterEvent).forwardTo(LoginView.class);
    Span addressSpan =
        componentTree(verificationView)
            .filter(component -> component.getId().orElse("").equals("pending-verification-email"))
            .map(Span.class::cast)
            .findFirst()
            .orElseThrow();
    assertTrue(addressSpan.getText().isEmpty());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void validProofShowsLocalizedWarningAndResendsOnlyForThisProof(String languageCode)
      throws IOException {
    PendingEmailVerificationView verificationView = createVerificationView(languageCode);
    when(pendingVerificationFlow.currentAccount(httpRequest))
        .thenReturn(Optional.of(UserEntity.builder().email("pending@example.test").build()));
    when(pendingVerificationFlow.resendVerification(httpRequest)).thenReturn(true);

    verificationView.beforeEnter(mock(BeforeEnterEvent.class));

    assertEquals(messageProperties.getEmailVerificationTitle(), verificationView.getPageTitle());

    assertTrue(
        componentTree(verificationView)
            .filter(Span.class::isInstance)
            .map(Span.class::cast)
            .anyMatch(span ->
                span.getText().equals(messageProperties.getEmailVerificationInstructions())));
    assertTrue(
        componentTree(verificationView)
            .filter(Span.class::isInstance)
            .map(Span.class::cast)
            .anyMatch(span ->
                span.getElement()
                    .getTextRecursively()
                    .equals(messageProperties.getEmailVerificationSpamHint())));
    assertEquals(
        messageProperties.getEmailVerificationSpamFolderLabel(),
        componentTree(verificationView)
            .filter(component -> component.hasClassName("pending-verification-folder-emphasis"))
            .map(Span.class::cast)
            .findFirst()
            .orElseThrow()
            .getText());
    assertEquals(
        1,
        componentTree(verificationView)
            .filter(component -> component.hasClassName("cosmos-security-warning"))
            .count());
    assertEquals(
        0,
        componentTree(verificationView)
            .filter(component -> component.getId().orElse("").startsWith("public-nav-"))
            .count());
    Button resendMailButton =
        componentTree(verificationView)
            .filter(component -> component.getId().orElse("").equals("resend-verification-email"))
            .map(Button.class::cast)
            .findFirst()
            .orElseThrow();
    assertEquals(messageProperties.getEmailVerificationResend(), resendMailButton.getText());

    resendMailButton.click();

    verify(pendingVerificationFlow).resendVerification(httpRequest);
  }
}
