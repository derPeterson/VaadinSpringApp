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
  private final Span appCodeLabel = new Span();
  private final Span recoveryTabLabel = new Span();
  private final Tab appCodeTab = new Tab(AppIcon.DEVICES.create(), appCodeLabel);
  private final Tab recoveryTab = new Tab(AppIcon.KEY.create(), recoveryTabLabel);
  private final Tabs codeMethodTabs = new Tabs(appCodeTab, recoveryTab);
  private final Span codeEntryHint = new Span();
  private final Span helpPrompt = new Span();
  private final Button openRecoveryHelpButton = new Button();
  private final VerticalLayout codeFieldsLayout =
      new VerticalLayout(codeEntryHint, appCodeField, recoveryCodeField);

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
      refreshMethod();
    });
    codeEntryHint.addClassName(CosmosClasses.PANEL_DESCRIPTION);
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
    HorizontalLayout helpRow = new HorizontalLayout(helpPrompt, openRecoveryHelpButton);
    helpRow.setAlignItems(Alignment.CENTER);
    helpRow.addClassName("two-factor-help-row");
    add(codeMethodTabs, codeFieldsLayout, helpRow);
    refreshTexts();
  }

  void refreshTexts() {
    appCodeLabel.setText(messageProperties.getTwoFactorConfirmationAppTab());
    recoveryTabLabel.setText(messageProperties.getTwoFactorRecoveryCodeLabel());
    appCodeField.setLabel(messageProperties.getTwoFactorSetupCodeLabel());
    recoveryCodeField.setLabel(messageProperties.getTwoFactorRecoveryCodeLabel());
    helpPrompt.setText(messageProperties.getSupportHelpSubtitle());
    openRecoveryHelpButton.setText(messageProperties.getSupportHelpTitle());
    refreshMethod();
  }

  private void refreshMethod() {
    boolean recovery = codeMethodTabs.getSelectedTab() == recoveryTab;
    appCodeField.setVisible(!recovery);
    recoveryCodeField.setVisible(recovery);
    Tab selected = recovery ? recoveryTab : appCodeTab;
    codeFieldsLayout.getElement().setAttribute("aria-labelledby", selected.getId().orElseThrow());
    codeEntryHint.setText(
        recovery
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
