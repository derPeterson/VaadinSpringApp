package de.derpeterson.app.ui.components;

import com.flowingcode.vaadin.addons.otpfield.OtpField;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.tabs.TabsVariant;
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
  private final Span appCodeLabelSpan = new Span();
  private final Span recoveryTabLabelSpan = new Span();
  private final Tab appCodeTab = new Tab(AppIcon.DEVICES.create(), appCodeLabelSpan);
  private final Tab recoveryTab = new Tab(AppIcon.KEY.create(), recoveryTabLabelSpan);
  private final Tabs codeMethodTabs = new Tabs(appCodeTab, recoveryTab);
  private final Span codeEntryHintSpan = new Span();
  private final Span helpPromptSpan = new Span();
  private final Button openRecoveryHelpButton = new Button();
  private final VerticalLayout codeFieldsLayout =
      new VerticalLayout(codeEntryHintSpan, appCodeField, recoveryCodeField);

  TwoFactorCodeInput(
      MessageProperties messageProperties,
      HttpServletRequest request,
      String fieldPrefix,
      String methodPrefix,
      SerializableRunnable closeAction) {
    this.messageProperties = messageProperties;
    appCodeField.setId(fieldPrefix + "-factor");
    TwoFactorCodeFields.configureOtpField(appCodeField, request.getContextPath());
    recoveryCodeField.setId(fieldPrefix + "-recovery-code");
    recoveryCodeField.setWidthFull();
    recoveryCodeField.setManualValidation(true);
    recoveryCodeField.setRequiredIndicatorVisible(true);
    recoveryCodeField.setValueChangeMode(ValueChangeMode.EAGER);
    TwoFactorCodeFields.configureRecoveryCodeField(recoveryCodeField);

    String inputId = methodPrefix + "-code-input";
    appCodeTab.setId(methodPrefix + "-app-code-tab");
    recoveryTab.setId(methodPrefix + "-recovery-code-tab");

    for (Tab tab : List.of(appCodeTab, recoveryTab)) {
      tab.getElement().setAttribute("aria-controls", inputId);
    }
    codeMethodTabs.setWidthFull();
    codeMethodTabs.addClassName("two-factor-method-tabs");
    codeMethodTabs.addThemeVariants(TabsVariant.LUMO_EQUAL_WIDTH_TABS);
    codeMethodTabs.addSelectedChangeListener(event -> {
      resetFields();
      refreshSelectedCodeInput();
    });
    codeEntryHintSpan.addClassName(CosmosCssClasses.PANEL_DESCRIPTION);
    codeFieldsLayout.setId(inputId);
    codeFieldsLayout.addClassName("two-factor-confirmation-input");
    codeFieldsLayout.getElement().setAttribute("role", "tabpanel");
    codeFieldsLayout.setPadding(false);
    codeFieldsLayout.setSpacing(false);
    setPadding(false);
    setSpacing(false);
    addClassName("two-factor-dialog-codes");
    openRecoveryHelpButton.setId(methodPrefix + "-help");
    openRecoveryHelpButton.setIcon(AppIcon.OPEN_SECTION.create());
    openRecoveryHelpButton.setIconAfterText(true);
    openRecoveryHelpButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
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
    add(codeMethodTabs, codeFieldsLayout, recoveryHelpRowLayout);
    refreshTexts();
  }

  void refreshTexts() {
    appCodeLabelSpan.setText(messageProperties.getTwoFactorConfirmationAppTab());
    recoveryTabLabelSpan.setText(messageProperties.getTwoFactorRecoveryCodeLabel());
    appCodeField.setLabel(messageProperties.getTwoFactorSetupCodeLabel());
    recoveryCodeField.setLabel(messageProperties.getTwoFactorRecoveryCodeLabel());
    helpPromptSpan.setText(messageProperties.getSupportHelpSubtitle());
    openRecoveryHelpButton.setText(messageProperties.getSupportHelpTitle());
    refreshSelectedCodeInput();
  }

  private void refreshSelectedCodeInput() {
    boolean recoveryCodeSelected = codeMethodTabs.getSelectedTab() == recoveryTab;
    appCodeField.setVisible(!recoveryCodeSelected);
    recoveryCodeField.setVisible(recoveryCodeSelected);

    Tab selectedCodeTab = recoveryCodeSelected ? recoveryTab : appCodeTab;
    codeFieldsLayout
        .getElement()
        .setAttribute("aria-labelledby", selectedCodeTab.getId().orElseThrow());
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
    codeMethodTabs.setSelectedTab(appCodeTab);
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
  }

  boolean validate() {
    return ValidationHelper.validateRequiredInputs(
        List.of(codeMethodTabs.getSelectedTab() == recoveryTab ? recoveryCodeField : appCodeField),
        messageProperties);
  }

  String getValue() {
    return codeMethodTabs.getSelectedTab() == recoveryTab
        ? recoveryCodeField.getValue()
        : appCodeField.getValue();
  }

  void markInvalid() {
    if (codeMethodTabs.getSelectedTab() == recoveryTab) {
      recoveryCodeField.setInvalid(true);
    } else {
      appCodeField.setInvalid(true);
    }
  }
}
