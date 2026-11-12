package de.derpeterson.app.ui.components;

import static de.derpeterson.app.helper.logging.LogSafety.safeFailure;
import static de.derpeterson.app.ui.components.CosmosCssClasses.DELETE_BUTTON;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_DESCRIPTION;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_HEADING;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_ICON;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_TITLE;
import static de.derpeterson.app.ui.components.CosmosCssClasses.REFERENCE_OVERLAY;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ModalityMode;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.Autocomplete;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.function.SerializableSupplier;
import de.derpeterson.app.helper.ui.EqualButtonWidths;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.helper.ui.NotificationHelper.NotificationType;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.AppRoute;
import de.derpeterson.app.security.AccountActionNotice;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.AccountLifecycleService;
import de.derpeterson.app.service.AccountLifecycleService.AccountAction;
import de.derpeterson.app.service.AccountLifecycleService.LastSuperadminException;
import de.derpeterson.app.service.AccountLifecycleService.ProofMethod;
import jakarta.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.List;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

@Slf4j
public final class AccountLifecycleComponent extends VerticalLayout {
  private record ProofEntry(
      Component proofInputComponent,
      SerializableSupplier<String> proofInputSupplier,
      SerializableRunnable focusProofInputAction)
      implements Serializable {}

  private final transient MessageProperties messageProperties;
  private final transient AccountLifecycleService accountLifecycleService;
  private final transient SecurityService securityService;
  private final transient HttpServletRequest httpServletRequest;
  private final Span introductionSpan = new Span();
  private final Button deactivateAccountButton =
      new Button(
          AppIcon.USER_PAUSE.create(),
          clickEvent -> openAccountProofDialog(AccountAction.DEACTIVATE));
  private final Button deleteAccountButton =
      new Button(
          AppIcon.USER_X.create(), clickEvent -> openAccountProofDialog(AccountAction.DELETE));

  public AccountLifecycleComponent(
      MessageProperties messageProperties,
      AccountLifecycleService accountLifecycleService,
      SecurityService securityService,
      HttpServletRequest httpServletRequest) {
    this.messageProperties = messageProperties;
    this.accountLifecycleService = accountLifecycleService;
    this.securityService = securityService;
    this.httpServletRequest = httpServletRequest;

    setWidthFull();
    addClassName("account-tab-panel-content");
    deactivateAccountButton.addClassName("cosmos-warning-button");
    deactivateAccountButton.setId("account-deactivate");
    deleteAccountButton.setId("account-delete");
    deleteAccountButton.addClassName(DELETE_BUTTON);

    HorizontalLayout accountActionsLayout =
        new HorizontalLayout(deactivateAccountButton, deleteAccountButton);
    accountActionsLayout.setWidthFull();
    accountActionsLayout.setJustifyContentMode(JustifyContentMode.END);
    accountActionsLayout.getStyle().set("flex-wrap", "wrap");
    HorizontalLayout introductionLayout =
        new HorizontalLayout(AppIcon.ERROR.create(), introductionSpan);
    introductionLayout.setAlignItems(Alignment.START);
    introductionLayout.setPadding(false);
    VerticalLayout warningLayout = new VerticalLayout(introductionLayout, accountActionsLayout);
    warningLayout.setPadding(false);
    warningLayout.addClassNames("cosmos-security-warning", "account-lifecycle-overview");
    add(warningLayout);
    refreshLifecycleTexts();
  }

  public void refreshLifecycleTexts() {
    introductionSpan.setText(messageProperties.getAccountLifecycleIntro());
    deactivateAccountButton.setText(messageProperties.getAccountDeactivateAction());
    deleteAccountButton.setText(messageProperties.getAccountDeleteAction());
  }

  private void openAccountProofDialog(AccountAction accountAction) {
    ProofMethod proofMethod;
    try {
      proofMethod = accountLifecycleService.proofMethod(httpServletRequest);
    } catch (AccessDeniedException _) {
      showProofFailure(messageProperties::getAccountLifecycleFailure);
      return;
    }
    if (proofMethod == ProofMethod.UNAVAILABLE) {
      showProofFailure(messageProperties::getAccountLifecycleProofUnavailable);
      return;
    }

    boolean deletingAccount = accountAction == AccountAction.DELETE;
    String accountActionLabel =
        deletingAccount
            ? messageProperties.getAccountDeleteAction()
            : messageProperties.getAccountDeactivateAction();
    String accountActionExplanation =
        deletingAccount
            ? messageProperties.getAccountDeleteExplanation()
            : messageProperties.getAccountDeactivateExplanation();
    Dialog proofDialog = new Dialog();
    proofDialog.setId("account-lifecycle-proof");
    proofDialog.setModality(ModalityMode.STRICT);
    proofDialog.addClassNames("cosmos-theme", REFERENCE_OVERLAY, "account-change-dialog");
    proofDialog.getThemeNames().addAll(List.of("cosmos", "cosmos-reference"));
    proofDialog.setWidth("min(580px, calc(100vw - 24px))");
    proofDialog.addClassName(
        deletingAccount ? "account-lifecycle-delete" : "account-lifecycle-deactivate");
    addProofDialogHeader(proofDialog, accountActionLabel, deletingAccount);

    VerticalLayout proofFieldsLayout = new VerticalLayout();
    proofFieldsLayout.setPadding(false);
    ProofEntry proofEntry = createProofEntry(proofMethod, proofDialog);
    Span warningLabelSpan = new Span(messageProperties.getSupportSecurityNoticeLabel() + " ");
    warningLabelSpan.addClassName("cosmos-security-warning-label");
    HorizontalLayout warningLayout =
        new HorizontalLayout(
            AppIcon.ERROR.create(), new Span(warningLabelSpan, new Span(accountActionExplanation)));
    warningLayout.setPadding(false);
    warningLayout.setSpacing(false);
    warningLayout.setAlignItems(Alignment.START);
    warningLayout.addClassName("cosmos-security-warning");
    proofFieldsLayout.add(warningLayout, proofEntry.proofInputComponent());
    Div proofFormDiv = new Div(proofFieldsLayout);
    proofFormDiv.addClassNames("cosmos-dialog-form", "account-change-dialog-form");
    proofDialog.add(proofFormDiv);
    addProofDialogActions(
        proofDialog, accountAction, accountActionLabel, proofEntry.proofInputSupplier());
    proofDialog.open();
    proofEntry.focusProofInputAction().run();
  }

  private void addProofDialogHeader(
      Dialog proofDialog, String accountActionLabel, boolean deletingAccount) {
    Div proofIconDiv = new Div((deletingAccount ? AppIcon.USER_X : AppIcon.USER_PAUSE).create());
    proofIconDiv.addClassName(PANEL_ICON);
    H2 proofTitleH2 = new H2(accountActionLabel);
    proofTitleH2.setId("account-lifecycle-title");
    proofTitleH2.addClassName(PANEL_TITLE);
    Span identitySubtitleSpan = new Span(messageProperties.getExternalProof());
    identitySubtitleSpan.addClassName(PANEL_DESCRIPTION);
    Div proofHeadingDiv = new Div(proofTitleH2, identitySubtitleSpan);
    proofHeadingDiv.addClassName(PANEL_HEADING);
    Button closeProofDialogButton =
        new Button(AppIcon.CLOSE_CIRCLE.create(), closeEvent -> proofDialog.close());
    closeProofDialogButton.addClassName("cosmos-overlay-close");
    closeProofDialogButton.setAriaLabel(messageProperties.getNotificationsClose());
    Div proofHeaderDiv = new Div(proofIconDiv, proofHeadingDiv, closeProofDialogButton);
    proofHeaderDiv.addClassName("account-change-dialog-header");
    proofDialog.getHeader().add(proofHeaderDiv);
    proofDialog.getElement().setAttribute("aria-labelledby", "account-lifecycle-title");
  }

  private ProofEntry createProofEntry(ProofMethod proofMethod, Dialog proofDialog) {
    if (proofMethod == ProofMethod.AUTHENTICATOR) {
      TwoFactorCodeInput accountCodeInput =
          new TwoFactorCodeInput(
              messageProperties,
              httpServletRequest,
              "account-lifecycle",
              "account-lifecycle",
              proofDialog::close);
      return new ProofEntry(
          accountCodeInput,
          () -> accountCodeInput.validate() ? accountCodeInput.getValue() : null,
          accountCodeInput::focusAppCode);
    }

    PasswordField currentPasswordField =
        new PasswordField(messageProperties.getAccountLifecyclePassword());
    currentPasswordField.setId("account-lifecycle-password");
    currentPasswordField.setWidthFull();
    currentPasswordField.setRequiredIndicatorVisible(true);
    currentPasswordField.setAutocomplete(Autocomplete.CURRENT_PASSWORD);
    Span passwordHintSpan = new Span(messageProperties.getExternalPasswordProofHint());
    passwordHintSpan.addClassName("account-lifecycle-password-hint");
    VerticalLayout passwordProofLayout = new VerticalLayout(passwordHintSpan, currentPasswordField);
    passwordProofLayout.setPadding(false);
    return new ProofEntry(
        passwordProofLayout, currentPasswordField::getValue, currentPasswordField::focus);
  }

  private void addProofDialogActions(
      Dialog proofDialog,
      AccountAction accountAction,
      String accountActionLabel,
      SerializableSupplier<String> proofInputSupplier) {
    Button cancelProofButton =
        new Button(
            messageProperties.getAdminUsersCancel(),
            AppIcon.CLOSE.create(),
            clickEvent -> proofDialog.close());
    Button confirmAccountActionButton =
        new Button(
            accountActionLabel,
            clickEvent -> {
              String enteredProof = proofInputSupplier.get();
              if (enteredProof != null && !enteredProof.isBlank()) {
                confirmAccountAction(proofDialog, accountAction, enteredProof);
              }
            });
    confirmAccountActionButton.setId("account-lifecycle-submit");
    confirmAccountActionButton.setIcon(
        (accountAction == AccountAction.DELETE ? AppIcon.USER_X : AppIcon.USER_PAUSE).create());
    confirmAccountActionButton.addClassName(
        accountAction == AccountAction.DELETE ? DELETE_BUTTON : "cosmos-warning-button");
    Div proofActionsDiv = new Div(cancelProofButton, confirmAccountActionButton);
    proofActionsDiv.addClassName("cosmos-dialog-actions");
    EqualButtonWidths.configureEqualButtonWidths(proofActionsDiv);
    proofDialog.getFooter().add(proofActionsDiv);
  }

  private void confirmAccountAction(
      Dialog proofDialog, AccountAction accountAction, String enteredProof) {
    boolean mailQueued;
    try {
      mailQueued =
          accountLifecycleService.changeOwnAccount(httpServletRequest, accountAction, enteredProof);
    } catch (LastSuperadminException _) {
      showProofFailure(messageProperties::getAccountLifecycleLastSuperadmin);
      return;
    } catch (AuthenticationException _) {
      showProofFailure(messageProperties::getAccountLifecycleInvalidProof);
      return;
    } catch (AccessDeniedException _) {
      showProofFailure(messageProperties::getAccountLifecycleFailure);
      return;
    } catch (RuntimeException accountActionFailure) {
      log.warn("Account lifecycle action rejected", safeFailure(accountActionFailure));
      logoutWithNotice(proofDialog, new AccountActionNotice(accountAction, false, false));
      return;
    }

    logoutWithNotice(proofDialog, new AccountActionNotice(accountAction, true, mailQueued));
  }

  private void logoutWithNotice(Dialog proofDialog, AccountActionNotice accountActionNotice) {
    UI accountUi = UI.getCurrent();
    proofDialog.close();
    securityService.logout();
    accountActionNotice.storeAfterLogout(httpServletRequest);
    accountUi.getPage().setLocation(AppRoute.LOGIN.getSecurityRoute());
  }

  private void showProofFailure(Supplier<String> failureTextSupplier) {
    NotificationHelper.getInstance()
        .showNotification(
            messageProperties::getBaseFailedTitle, failureTextSupplier, NotificationType.ERROR);
  }
}
