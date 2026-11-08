package de.derpeterson.app.ui.components;

import com.flowingcode.vaadin.addons.otpfield.OtpField;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Span;
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
  private final transient MessageProperties texts;
  private final OtpField appCode = new OtpField();
  private final TextField recoveryCode = new TextField();
  private final Span appCodeLabel = new Span();
  private final Span recoveryTabLabel = new Span();
  private final Tab appCodeTab = new Tab(AppIcon.DEVICES.create(), appCodeLabel);
  private final Tab recoveryTab = new Tab(AppIcon.KEY.create(), recoveryTabLabel);
  private final Tabs methods = new Tabs(appCodeTab, recoveryTab);
  private final Span hint = new Span();
  private final Button help = new Button();
  private final VerticalLayout input = new VerticalLayout(hint, appCode, recoveryCode);

  TwoFactorCodeInput(
      MessageProperties texts,
      HttpServletRequest request,
      String fieldPrefix,
      String methodPrefix,
      SerializableRunnable closeAction) {
    this.texts = texts;
    appCode.setId(fieldPrefix + "-factor");
    TwoFactorCodeFields.configureOtp(appCode, request.getContextPath());
    recoveryCode.setId(fieldPrefix + "-recovery-code");
    recoveryCode.setWidthFull();
    recoveryCode.setManualValidation(true);
    recoveryCode.setRequiredIndicatorVisible(true);
    recoveryCode.setValueChangeMode(ValueChangeMode.EAGER);
    TwoFactorCodeFields.configureRecoveryCode(recoveryCode);

    String inputId = methodPrefix + "-code-input";
    appCodeTab.setId(methodPrefix + "-app-code-tab");
    recoveryTab.setId(methodPrefix + "-recovery-code-tab");
    for (Tab tab : List.of(appCodeTab, recoveryTab)) {
      tab.getElement().setAttribute("aria-controls", inputId);
    }
    methods.setWidthFull();
    methods.addClassName("two-factor-method-tabs");
    methods.addThemeVariants(TabsVariant.LUMO_EQUAL_WIDTH_TABS);
    methods.addSelectedChangeListener(
        event -> {
          resetFields();
          refreshMethod();
        });
    hint.addClassName(CosmosClasses.PANEL_DESCRIPTION);
    input.setId(inputId);
    input.addClassName("two-factor-confirmation-input");
    input.getElement().setAttribute("role", "tabpanel");
    input.setPadding(false);
    input.setSpacing(false);
    setPadding(false);
    setSpacing(false);
    addClassName("two-factor-dialog-codes");
    help.setId(methodPrefix + "-help");
    help.setPrefixComponent(AppIcon.INFO_CIRCLE.create());
    help.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    help.addClickListener(
        event -> {
          closeAction.run();
          TwoFactorFlow.ACCOUNT_HELP.enter(request);
          UI.getCurrent()
              .getPage()
              .setLocation(request.getContextPath() + TwoFactorFlow.ACCOUNT_HELP.location());
        });
    add(methods, input, help);
    refreshTexts();
  }

  void refreshTexts() {
    appCodeLabel.setText(texts.getTwoFactorConfirmationAppTab());
    recoveryTabLabel.setText(texts.getTwoFactorRecoveryCodeLabel());
    appCode.setLabel(texts.getTwoFactorSetupCodeLabel());
    recoveryCode.setLabel(texts.getTwoFactorRecoveryCodeLabel());
    help.setText(texts.getSupportHelpSubtitle());
    refreshMethod();
  }

  private void refreshMethod() {
    boolean recovery = methods.getSelectedTab() == recoveryTab;
    appCode.setVisible(!recovery);
    recoveryCode.setVisible(recovery);
    Tab selected = recovery ? recoveryTab : appCodeTab;
    input.getElement().setAttribute("aria-labelledby", selected.getId().orElseThrow());
    hint.setText(recovery ? texts.getTwoFactorActionRecoveryHint() : texts.getTwoFactorLoginHint());
  }

  void addAppCodeCompleteListener(SerializableRunnable listener) {
    appCode.addCompleteListener(
        event -> {
          if (isVisible() && TwoFactorCodeFields.isCurrentCompleteOtp(appCode, event)) {
            listener.run();
          }
        });
  }

  void focusAppCode() {
    appCode.focus();
  }

  void reset() {
    methods.setSelectedTab(appCodeTab);
    resetFields();
  }

  private void resetFields() {
    clear();
    appCode.setInvalid(false);
    recoveryCode.setInvalid(false);
  }

  void clear() {
    appCode.clear();
    recoveryCode.clear();
  }

  boolean validate() {
    return methods.getSelectedTab() == recoveryTab
        ? ValidationHelper.validateRequiredInputs(List.of(recoveryCode), texts)
        : ValidationHelper.validateRequiredInputs(List.of(appCode), texts);
  }

  String getValue() {
    return methods.getSelectedTab() == recoveryTab ? recoveryCode.getValue() : appCode.getValue();
  }

  void markInvalid() {
    if (methods.getSelectedTab() == recoveryTab) {
      recoveryCode.setInvalid(true);
    } else {
      appCode.setInvalid(true);
    }
  }
}
