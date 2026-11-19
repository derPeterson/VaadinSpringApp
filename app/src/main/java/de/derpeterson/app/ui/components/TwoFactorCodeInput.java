package de.derpeterson.app.ui.components;

import com.flowingcode.vaadin.addons.otpfield.OtpField;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.dialog.Dialog.DialogHeader;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.SerializableRunnable;
import de.derpeterson.app.helper.ui.ValidationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.security.TwoFactorFlow;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/** App- und Ersatzcode für Bestätigungsdialoge. Die Fachaktion bleibt beim Dialog. */
@StyleSheet("custom-theme/cosmos-mfa.css")
@StyleSheet("custom-theme/cosmos-mfa-methods.css")
@StyleSheet("custom-theme/cosmos-mfa-confirmation.css")
final class TwoFactorCodeInput extends VerticalLayout {
  private final transient MessageProperties messageProperties;
  private final OtpField appCodeField = new OtpField();
  private final TextField recoveryCodeField = new TextField();
  private final TwoFactorCodeTabs codeMethodTabs;
  private final Span codeEntryHintSpan = new Span();
  private final Span helpPromptSpan = new Span();
  private final Button openRecoveryHelpButton = new Button();
  private final VerticalLayout codeFieldsLayout =
      new VerticalLayout(codeEntryHintSpan, appCodeField, recoveryCodeField);

  TwoFactorCodeInput(
      MessageProperties messageProperties,
      HttpServletRequest request,
      DialogHeader dialogHeader,
      String fieldPrefix,
      String methodPrefix,
      SerializableRunnable closeAction) {
    this.messageProperties = messageProperties;
    appCodeField.setId(fieldPrefix + "-factor");
    TwoFactorCodeFields.configureOtpField(appCodeField);
    recoveryCodeField.setId(fieldPrefix + "-recovery-code");
    recoveryCodeField.setWidthFull();
    recoveryCodeField.setManualValidation(true);
    recoveryCodeField.setRequiredIndicatorVisible(true);
    recoveryCodeField.setValueChangeMode(ValueChangeMode.EAGER);
    TwoFactorCodeFields.configureRecoveryCodeField(recoveryCodeField);

    codeEntryHintSpan.addClassName(CosmosCssClasses.PANEL_DESCRIPTION);
    codeFieldsLayout.setId(methodPrefix + "-code-input");
    codeFieldsLayout.addClassName("two-factor-confirmation-input");
    codeFieldsLayout.setPadding(false);
    codeFieldsLayout.setSpacing(false);

    codeMethodTabs =
        new TwoFactorCodeTabs(
            methodPrefix + "-app-code-tab",
            methodPrefix + "-recovery-code-tab",
            codeFieldsLayout,
            messageProperties);
    codeMethodTabs.addSelectedChangeListener(event -> {
      refreshSelectedCodeInput();
      resetFields();
    });
    dialogHeader.add(codeMethodTabs);

    setPadding(false);
    setSpacing(false);
    addClassName("two-factor-dialog-codes");

    openRecoveryHelpButton.setId(methodPrefix + "-help");
    openRecoveryHelpButton.setIcon(AppIcon.OPEN_SECTION.create());
    openRecoveryHelpButton.setIconAfterText(true);
    openRecoveryHelpButton.addThemeVariants(ButtonVariant.TERTIARY);
    openRecoveryHelpButton.addClassName(CosmosCssClasses.INLINE_BUTTON);
    openRecoveryHelpButton.addClickListener(event -> {
      closeAction.run();
      TwoFactorFlow.ACCOUNT_HELP.enter(request);
      UI.getCurrent()
          .getPage()
          .setLocation(request.getContextPath() + TwoFactorFlow.ACCOUNT_HELP.location());
    });

    HorizontalLayout recoveryHelpRowLayout =
        new HorizontalLayout(helpPromptSpan, openRecoveryHelpButton);
    recoveryHelpRowLayout.setAlignItems(Alignment.CENTER);
    recoveryHelpRowLayout.addClassName("two-factor-help-row");
    add(codeFieldsLayout, recoveryHelpRowLayout);
    refreshTexts();
  }

  void setCodeEntryVisible(boolean visible) {
    setVisible(visible);
    codeMethodTabs.setVisible(visible);
  }

  void refreshTexts() {
    codeMethodTabs.refreshTexts(messageProperties);
    appCodeField.setLabel(messageProperties.getTwoFactorSetupCodeLabel());
    recoveryCodeField.setLabel(messageProperties.getTwoFactorRecoveryCodeLabel());
    helpPromptSpan.setText(messageProperties.getSupportHelpSubtitle());
    openRecoveryHelpButton.setText(messageProperties.getSupportHelpTitle());
    refreshSelectedCodeInput();
  }

  private void refreshSelectedCodeInput() {
    boolean recoveryCodeSelected = codeMethodTabs.isRecoveryCodeSelected();
    appCodeField.setVisible(!recoveryCodeSelected);
    recoveryCodeField.setVisible(recoveryCodeSelected);

    codeEntryHintSpan.setText(
        recoveryCodeSelected
            ? messageProperties.getTwoFactorActionRecoveryHint()
            : messageProperties.getTwoFactorLoginHint());
  }

  void addAppCodeCompleteListener(SerializableRunnable listener) {
    appCodeField.addCompleteListener(event -> {
      if (isVisible() && TwoFactorCodeFields.isCurrentCompleteOtp(appCodeField, event)) {
        listener.run();
      }
    });
  }

  void focusAppCode() {
    appCodeField.focus();
  }

  void reset() {
    codeMethodTabs.setRecoveryCodeSelected(false);
    resetFields();
  }

  private void resetFields() {
    clear();
    appCodeField.setInvalid(false);
    recoveryCodeField.setInvalid(false);
  }

  void clear() {
    appCodeField.clear();
    recoveryCodeField.clear();

    if (appCodeField.isVisible()) {
      // Ein Teilcode kann bis zum change-Ereignis nur im Browser stehen.
      appCodeField.getElement().callJsFunction("clear");
    }
  }

  boolean validate() {
    return ValidationHelper.validateRequiredInputs(
        List.of(codeMethodTabs.isRecoveryCodeSelected() ? recoveryCodeField : appCodeField),
        messageProperties);
  }

  String getValue() {
    return codeMethodTabs.isRecoveryCodeSelected()
        ? recoveryCodeField.getValue()
        : appCodeField.getValue();
  }

  void markInvalid() {
    if (codeMethodTabs.isRecoveryCodeSelected()) {
      recoveryCodeField.setInvalid(true);
    } else {
      appCodeField.setInvalid(true);
    }
  }
}
