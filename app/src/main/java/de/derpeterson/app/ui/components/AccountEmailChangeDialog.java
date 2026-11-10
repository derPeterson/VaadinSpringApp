package de.derpeterson.app.ui.components;

import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_DESCRIPTION;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_HEADING;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_ICON;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_TITLE;
import static de.derpeterson.app.ui.components.CosmosClasses.REFERENCE_OVERLAY;

import com.vaadin.flow.component.ModalityMode;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.textfield.Autocomplete;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.helper.ui.EqualButtonWidths;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.helper.ui.ValidationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.service.AccountSecurityService.EmailChange;
import de.derpeterson.app.ui.components.AccountSecurityFeedback.RejectedSecurityInput;
import de.derpeterson.app.validation.UserInputRules;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/** Fordert eine verifizierte Adressänderung an; die bisherige Kontoadresse bleibt erhalten. */
public class AccountEmailChangeDialog extends Dialog {
  private final transient MessageProperties messageProperties;
  private final H2 dialogTitleHeading = new H2();
  private final Span dialogDescriptionSpan = new Span();
  private final Span emailChangeHintSpan = new Span();
  private final Span developmentWarningSpan = new Span();
  private transient BooleanSupplier developmentExceptionSupplier = () -> false;
  private final EmailField newEmailField = new EmailField();
  private final EmailField confirmEmailField = new EmailField();
  private final PasswordField currentPasswordField = new PasswordField();
  private transient BooleanSupplier currentPasswordRequiredSupplier = () -> true;
  private final Button closeDialogButton =
      new Button(AppIcon.CLOSE_CIRCLE.create(), event -> close());
  private final Button cancelEmailChangeButton = new Button("", event -> close());
  private final Button requestEmailChangeButton = new Button();
  private SerializableConsumer<EmailChange> requestEmailChangeConsumer;

  public AccountEmailChangeDialog(MessageProperties messageProperties) {
    this.messageProperties = messageProperties;
    newEmailField.setPattern(UserInputRules.EMAIL_REGEX);
    confirmEmailField.setPattern(UserInputRules.EMAIL_REGEX);
    setId("account-email-change-dialog");
    addClassNames("cosmos-theme", "account-change-dialog", "account-email-dialog");
    getThemeNames().addAll(java.util.List.of("cosmos", "cosmos-reference"));
    addClassName(REFERENCE_OVERLAY);
    setModality(ModalityMode.STRICT);
    setCloseOnEsc(true);
    setCloseOnOutsideClick(true);

    dialogTitleHeading.addClassName(PANEL_TITLE);
    dialogDescriptionSpan.addClassName(PANEL_DESCRIPTION);

    Div dialogHeadingDiv = new Div(dialogTitleHeading, dialogDescriptionSpan);
    dialogHeadingDiv.addClassName(PANEL_HEADING);

    SvgIcon dialogSvgIcon = AppIcon.ENVELOPE.create();
    dialogSvgIcon.getElement().setAttribute("aria-hidden", "true");
    Div dialogIconDiv = new Div(dialogSvgIcon);
    dialogIconDiv.addClassName(PANEL_ICON);
    closeDialogButton.setId("account-email-close");
    closeDialogButton.setIcon(AppIcon.CLOSE_CIRCLE.create());
    closeDialogButton.addClassNames("cosmos-overlay-close", LumoUtility.TextColor.PRIMARY);
    closeDialogButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);

    Div dialogHeaderDiv = new Div(dialogIconDiv, dialogHeadingDiv, closeDialogButton);
    dialogHeaderDiv.addClassName("account-change-dialog-header");
    getHeader().add(dialogHeaderDiv);

    newEmailField.setId("account-email-new");
    confirmEmailField.setId("account-email-confirm");
    currentPasswordField.setId("account-email-password");
    newEmailField.setRequiredIndicatorVisible(true);
    confirmEmailField.setRequiredIndicatorVisible(true);
    currentPasswordField.setRequiredIndicatorVisible(true);
    // Escape löst nicht zuverlässig einen Blur aus. Auch unbestätigte Eingaben
    // rechtzeitig synchronisieren, damit Schließen sie tatsächlich verwerfen kann.
    newEmailField.setValueChangeMode(ValueChangeMode.EAGER);
    confirmEmailField.setValueChangeMode(ValueChangeMode.EAGER);
    currentPasswordField.setValueChangeMode(ValueChangeMode.EAGER);
    newEmailField.setWidthFull();
    confirmEmailField.setWidthFull();
    currentPasswordField.setWidthFull();
    currentPasswordField.setRevealButtonVisible(true);
    currentPasswordField.setAutocomplete(Autocomplete.CURRENT_PASSWORD);

    SvgIcon securityInfoSvgIcon = AppIcon.INFO_CIRCLE.create();
    securityInfoSvgIcon.getElement().setAttribute("aria-hidden", "true");
    Div securityInfoDiv = new Div(securityInfoSvgIcon, emailChangeHintSpan);
    securityInfoDiv.addClassName("cosmos-panel-hint");

    Div changeFormDiv =
        new Div(newEmailField, confirmEmailField, currentPasswordField, securityInfoDiv);
    changeFormDiv.addClassNames("cosmos-dialog-form", "account-change-dialog-form");
    developmentWarningSpan.addClassName(PANEL_DESCRIPTION);
    developmentWarningSpan.setVisible(false);
    changeFormDiv.add(developmentWarningSpan);
    add(changeFormDiv);

    cancelEmailChangeButton.setId("account-email-cancel");
    cancelEmailChangeButton.setPrefixComponent(AppIcon.CLOSE.create());
    requestEmailChangeButton.setId("account-email-request");
    requestEmailChangeButton.setPrefixComponent(AppIcon.CHECK.create());
    requestEmailChangeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    requestEmailChangeButton.addClickListener(event -> submitEmailChangeRequest());

    Div dialogActionsDiv = new Div(cancelEmailChangeButton, requestEmailChangeButton);
    dialogActionsDiv.addClassName("cosmos-dialog-actions");
    EqualButtonWidths.configureEqualButtonWidths(dialogActionsDiv);
    getFooter().add(dialogActionsDiv);
    addOpenedChangeListener(event -> {
      if (!event.isOpened()) {
        clearDraft();
      }
    });

    refreshTexts();
  }

  public void setRequestAction(SerializableConsumer<EmailChange> action) {
    requestEmailChangeConsumer = Objects.requireNonNull(action);
  }

  public void setCurrentPasswordRequired(BooleanSupplier required) {
    currentPasswordRequiredSupplier = Objects.requireNonNull(required);
  }

  private void submitEmailChangeRequest() {
    if (!ValidationHelper.validateRequiredInputs(
            currentPasswordField.isVisible()
                ? List.of(newEmailField, confirmEmailField, currentPasswordField)
                : List.of(newEmailField, confirmEmailField),
            messageProperties)
        || !ValidationHelper.validateEmailValidInputs(
            List.of(newEmailField, confirmEmailField), messageProperties)
        || !ValidationHelper.validateEmailConfirmInputs(
            newEmailField, confirmEmailField, messageProperties)) {
      return;
    }

    requestEmailChangeButton.setEnabled(false);

    try {
      Objects.requireNonNull(requestEmailChangeConsumer, "Email change action required")
          .accept(
              new EmailChange(
                  newEmailField.getValue(),
                  confirmEmailField.getValue(),
                  currentPasswordField.getValue()));
      close();
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getBaseSuccessTitle,
              messageProperties::getAccountSecurityEmailRequested,
              NotificationHelper.NotificationType.SUCCESS);
    } catch (RuntimeException failure) {
      RejectedSecurityInput rejectedSecurityInput =
          AccountSecurityFeedback.showSecurityActionFailure(messageProperties, failure);
      if (rejectedSecurityInput == AccountSecurityFeedback.RejectedSecurityInput.CURRENT_PASSWORD) {
        currentPasswordField.setInvalid(true);
      }
      if (rejectedSecurityInput == AccountSecurityFeedback.RejectedSecurityInput.EMAIL) {
        newEmailField.setInvalid(true);
        confirmEmailField.setInvalid(true);
      }
    } finally {
      requestEmailChangeButton.setEnabled(true);
    }
  }

  @Override
  public void open() {
    developmentWarningSpan.setText(messageProperties.getTwoFactorDevelopmentWarning());
    developmentWarningSpan.setVisible(developmentExceptionSupplier.getAsBoolean());
    boolean required = currentPasswordRequiredSupplier.getAsBoolean();
    currentPasswordField.setVisible(required);
    currentPasswordField.setRequiredIndicatorVisible(required);

    clearDraft();
    super.open();
  }

  private void clearDraft() {
    newEmailField.clear();
    confirmEmailField.clear();
    currentPasswordField.clear();
    newEmailField.setInvalid(false);
    confirmEmailField.setInvalid(false);
    currentPasswordField.setInvalid(false);
  }

  public void refreshTexts() {
    dialogTitleHeading.setText(messageProperties.getAccountSettingsChangeEmail());
    getElement().setProperty("ariaLabel", dialogTitleHeading.getText());
    dialogDescriptionSpan.setText(messageProperties.getAccountEmailDialogDescription());
    closeDialogButton.setAriaLabel(messageProperties.getAdminUsersCloseDialog());
    newEmailField.setLabel(messageProperties.getAccountEmailDialogNewEmail());
    newEmailField.setPlaceholder(messageProperties.getAccountEmailPlaceholder());
    confirmEmailField.setLabel(messageProperties.getAccountEmailDialogConfirmEmail());
    confirmEmailField.setPlaceholder(messageProperties.getAccountConfirmEmailPlaceholder());
    currentPasswordField.setLabel(messageProperties.getAccountEmailDialogPassword());
    currentPasswordField.setPlaceholder(messageProperties.getAccountPasswordPlaceholder());
    emailChangeHintSpan.setText(messageProperties.getAccountEmailDialogHint());
    cancelEmailChangeButton.setText(messageProperties.getAdminUsersCancel());
    requestEmailChangeButton.setText(messageProperties.getAccountEmailDialogRequest());
  }

  public void setDevelopmentWarning(BooleanSupplier active) {
    developmentExceptionSupplier = Objects.requireNonNull(active);
  }
}
