package de.derpeterson.app.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.security.ExternalLoginFlow;
import de.derpeterson.app.security.ExternalLoginFlow.Purpose;
import de.derpeterson.app.service.ExternalIdentityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.util.ReflectionTestUtils;

class ExternalConnectionsComponentTest {
  private final ExternalLoginFlow flow = mock(ExternalLoginFlow.class);
  private final ExternalIdentityService identities = mock(ExternalIdentityService.class);
  private final HttpServletRequest request = mock(HttpServletRequest.class);
  private final AtomicReference<Purpose> grant = new AtomicReference<>();
  private final UserEntity user = UserEntity.builder().id(1L).email("own@example.test").build();
  private final AtomicInteger actions = new AtomicInteger();
  private final AtomicReference<Locale> locale = new AtomicReference<>(Locale.GERMAN);
  private MessageProperties texts;
  private UI ui;

  @BeforeEach
  void setup() {
    ui = new UI();
    UI.setCurrent(ui);
    VaadinSession session = mock(VaadinSession.class);
    when(session.hasLock()).thenReturn(true);
    when(session.getLocale()).thenAnswer(call -> locale.get());
    VaadinSession.setCurrent(session);
    ui.getInternals().setSession(session);
    texts = new MessageProperties(new CustomI18NProvider());
    when(flow.owner(request)).thenReturn(user);
    when(flow.providers()).thenReturn(List.of("google", "microsoft"));
    when(identities.connected(1L)).thenReturn(List.of("google"));
    when(flow.confirmedPurpose(request)).thenAnswer(call -> Optional.ofNullable(grant.get()));
  }

  @AfterEach
  void cleanup() {
    NotificationHelper.getInstance().closeAndClearAllNotifications();
    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void overviewKeepsLastMethodProtectedAndRequestsOnlyTheChosenAction(String language) {
    locale.set(Locale.forLanguageTag(language));
    ExternalConnectionsComponent component =
        new ExternalConnectionsComponent(flow, identities, request, texts);
    assertEquals(0, tree(component).filter(ComboBox.class::isInstance).count());
    assertFalse(((Button) find(component, "external-connect-google")).isEnabled());
    assertFalse(component.getConfirmationDialog().isOpened());
    ((Button) find(component, "external-connect-microsoft")).click();
    ExternalIdentityConfirmationDialog dialog = component.getConfirmationDialog();
    assertTrue(dialog.isOpened());
    assertFalse(find(dialog, "external-confirm-password").isVisible());
    assertFalse(((Button) find(dialog, "external-confirm-local")).isEnabled());
    assertTrue(find(dialog, "external-confirm-google").isVisible());
    assertEquals(
        0,
        tree(dialog)
            .filter(c -> c.getId().orElse("").equals("external-confirm-microsoft"))
            .count());
    verify(flow, never()).beginLink(request, "microsoft", locale.get());
  }

  @ParameterizedTest
  @CsvSource({"false, de", "true, de", "false, en", "true, en"})
  void disabledProvidersKeepConnectionsAndRequireAnExistingConfirmationMethod(
      boolean hasPassword, String language) {
    locale.set(Locale.forLanguageTag(language));
    user.setPassword(hasPassword ? "encoded" : null);
    when(flow.providers()).thenReturn(List.of());
    ExternalConnectionsComponent component =
        new ExternalConnectionsComponent(flow, identities, request, texts);
    component.configureAccountActions(actions::incrementAndGet, actions::incrementAndGet, () -> {});
    assertEquals(1, tree(component).filter(c -> c.hasClassName("external-connection-row")).count());
    assertEquals(hasPassword, ((Button) find(component, "external-connect-google")).isEnabled());
    assertTrue(
        tree(component)
            .filter(Span.class::isInstance)
            .map(Span.class::cast)
            .anyMatch(span -> span.getText().equals(texts.getExternalProviderUnavailable())));
    component.requestPasswordChange();
    component.requestEmailChange();
    assertEquals(hasPassword ? 2 : 0, actions.get());
    assertFalse(component.getConfirmationDialog().isOpened());
    if (!hasPassword) {
      Supplier<?> message =
          (Supplier<?>)
              ReflectionTestUtils.getField(NotificationHelper.getInstance(), "messageSupplier");
      assertEquals(texts.getExternalProofUnavailable(), message.get());
      locale.set(language.equals("de") ? Locale.ENGLISH : Locale.GERMAN);
      assertEquals(texts.getExternalProofUnavailable(), message.get());
    }
  }

  @Test
  void refreshingConfirmationAfterDeactivationShowsNoPasswordPromptOrProviderAction() {
    ExternalIdentityConfirmationDialog dialog =
        new ExternalIdentityConfirmationDialog(flow, identities, request, texts);
    dialog.open(Purpose.PASSWORD, actions::incrementAndGet);
    when(flow.providers()).thenReturn(List.of());
    dialog.refreshTexts();
    assertFalse(find(dialog, "external-confirm-password").isVisible());
    assertFalse(((Button) find(dialog, "external-confirm-local")).isEnabled());
    assertEquals(0, tree(dialog).filter(c -> c.hasClassName("external-proof-provider")).count());
    assertEquals(
        texts.getExternalProofUnavailable(),
        ((Span) ReflectionTestUtils.getField(dialog, "explanation")).getText());
    assertEquals(0, actions.get());
  }

  @Test
  void providerDisabledAfterOpeningClosesTheStaleConfirmationWithoutStartingAnAction() {
    ExternalIdentityConfirmationDialog dialog =
        new ExternalIdentityConfirmationDialog(flow, identities, request, texts);
    dialog.open(Purpose.PASSWORD, actions::incrementAndGet);
    Button provider = (Button) find(dialog, "external-confirm-google");
    when(flow.providers()).thenReturn(List.of());
    doThrow(new AccessDeniedException("Provider unavailable"))
        .when(flow)
        .beginProof(request, "google", Purpose.PASSWORD, Locale.GERMAN);
    provider.click();
    assertFalse(dialog.isOpened());
    assertEquals(0, actions.get());
    Supplier<?> message =
        (Supplier<?>)
            ReflectionTestUtils.getField(NotificationHelper.getInstance(), "messageSupplier");
    assertEquals(texts.getExternalProviderUnavailable(), message.get());
  }

  @Test
  void localProofIsBoundToTheChosenActionAndClosingDiscardsSecrets() {
    user.setPassword("encoded");
    grant.set(Purpose.EMAIL);
    ExternalIdentityConfirmationDialog dialog =
        new ExternalIdentityConfirmationDialog(flow, identities, request, texts);
    dialog.open(Purpose.LINK_MICROSOFT, actions::incrementAndGet);
    PasswordField password = (PasswordField) find(dialog, "external-confirm-password");
    password.setValue("OwnPortal1!");
    doAnswer(
            call -> {
              grant.set(Purpose.LINK_MICROSOFT);
              return null;
            })
        .when(flow)
        .confirmPassword(request, "OwnPortal1!", Purpose.LINK_MICROSOFT);
    ((Button) find(dialog, "external-confirm-local")).click();
    verify(flow).confirmPassword(request, "OwnPortal1!", Purpose.LINK_MICROSOFT);
    assertEquals(1, actions.get());
    assertFalse(dialog.isOpened());
    assertTrue(password.isEmpty());
    dialog.open(Purpose.EMAIL, actions::incrementAndGet);
    password.setValue("DiscardMe");
    dialog.close();
    assertTrue(password.isEmpty());
    assertEquals(1, actions.get());
  }

  @Test
  void emptyProofUsesTheRequiredNotificationWithoutInlineErrorText() {
    user.setPassword("encoded");
    ExternalIdentityConfirmationDialog dialog =
        new ExternalIdentityConfirmationDialog(flow, identities, request, texts);
    dialog.open(Purpose.LINK_GOOGLE, actions::incrementAndGet);
    ((Button) find(dialog, "external-confirm-local")).click();
    PasswordField password = (PasswordField) find(dialog, "external-confirm-password");
    assertTrue(password.isInvalid());
    assertTrue(password.getErrorMessage() == null || password.getErrorMessage().isBlank());
    Supplier<?> message =
        (Supplier<?>)
            ReflectionTestUtils.getField(NotificationHelper.getInstance(), "messageSupplier");
    assertEquals(texts.getBaseValidationRequiredMessage(), message.get());
    assertEquals(0, actions.get());
  }

  @Test
  void failedProofKeepsDialogOpenAndDoesNotRunTheAction() {
    user.setPassword("encoded");
    doThrow(new BadCredentialsException("Rejected"))
        .when(flow)
        .confirmPassword(request, "wrong", Purpose.UNLINK_GOOGLE);
    ExternalIdentityConfirmationDialog dialog =
        new ExternalIdentityConfirmationDialog(flow, identities, request, texts);
    dialog.open(Purpose.UNLINK_GOOGLE, actions::incrementAndGet);
    PasswordField password = (PasswordField) find(dialog, "external-confirm-password");
    password.setValue("wrong");
    ((Button) find(dialog, "external-confirm-local")).click();
    assertEquals(0, actions.get());
    assertTrue(dialog.isOpened());
    assertTrue(password.isEmpty());
    assertTrue(password.isInvalid());
    assertTrue(password.getErrorMessage() == null || password.getErrorMessage().isBlank());
    Supplier<?> message =
        (Supplier<?>)
            ReflectionTestUtils.getField(NotificationHelper.getInstance(), "messageSupplier");
    assertEquals(texts.getAccountSecurityWrongPassword(), message.get());
  }

  @Test
  void expiredProviderProofCannotExecuteAPasswordlessAction() {
    grant.set(Purpose.UNLINK_GOOGLE);
    ExternalIdentityConfirmationDialog dialog =
        new ExternalIdentityConfirmationDialog(flow, identities, request, texts);
    dialog.open(Purpose.UNLINK_GOOGLE, actions::incrementAndGet);
    Button confirm = (Button) find(dialog, "external-confirm-local");
    assertTrue(confirm.isEnabled());
    grant.set(null);
    confirm.click();
    assertEquals(0, actions.get());
    assertTrue(dialog.isOpened());
    assertFalse(confirm.isEnabled());
  }

  @Test
  void languageRefreshPreservesTheOpenPasswordDraft() {
    user.setPassword("encoded");
    ExternalIdentityConfirmationDialog dialog =
        new ExternalIdentityConfirmationDialog(flow, identities, request, texts);
    dialog.open(Purpose.PASSWORD, actions::incrementAndGet);
    PasswordField password = (PasswordField) find(dialog, "external-confirm-password");
    password.setValue("Draft1!");
    locale.set(Locale.ENGLISH);
    dialog.refreshTexts();
    assertEquals("Draft1!", password.getValue());
    assertEquals(
        texts.getExternalContinueAction(),
        ((Button) find(dialog, "external-confirm-local")).getText());
  }

  @Test
  void providerReturnResumesOnlyOnceAndRequiresTheMatchingProof() {
    HttpSession session = mock(HttpSession.class);
    Map<String, Object> attributes = new HashMap<>();
    when(request.getSession(false)).thenReturn(session);
    when(session.getAttribute(org.mockito.ArgumentMatchers.anyString()))
        .thenAnswer(call -> attributes.get(call.getArgument(0)));
    doAnswer(
            call -> {
              attributes.put(call.getArgument(0), call.getArgument(1));
              return null;
            })
        .when(session)
        .setAttribute(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
    doAnswer(
            call -> {
              attributes.remove(call.getArgument(0));
              return null;
            })
        .when(session)
        .removeAttribute(org.mockito.ArgumentMatchers.anyString());
    when(flow.beginProof(request, "google", Purpose.LINK_MICROSOFT, Locale.GERMAN))
        .thenReturn("/own-provider");
    ExternalIdentityConfirmationDialog dialog =
        new ExternalIdentityConfirmationDialog(flow, identities, request, texts);
    dialog.open(Purpose.LINK_MICROSOFT, actions::incrementAndGet);
    ((Button) find(dialog, "external-confirm-google")).click();
    grant.set(Purpose.LINK_MICROSOFT);
    assertEquals(Optional.of(Purpose.LINK_MICROSOFT), dialog.consumeProviderReturn());
    assertTrue(dialog.consumeProviderReturn().isEmpty());
    ((Button) find(dialog, "external-confirm-google")).click();
    grant.set(Purpose.EMAIL);
    assertTrue(dialog.consumeProviderReturn().isEmpty());
    assertEquals(0, actions.get());
  }

  private Component find(Component root, String id) {
    return tree(root).filter(c -> c.getId().orElse("").equals(id)).findFirst().orElseThrow();
  }

  private Stream<Component> tree(Component component) {
    Stream<Component> children = component.getChildren().flatMap(this::tree);
    if (component instanceof Dialog dialog) {
      children =
          Stream.concat(
              children,
              Stream.concat(
                      dialog.getHeader().getElement().getChildren(),
                      dialog.getFooter().getElement().getChildren())
                  .flatMap(element -> element.getComponent().stream())
                  .flatMap(this::tree));
    }
    return Stream.concat(Stream.of(component), children);
  }
}
