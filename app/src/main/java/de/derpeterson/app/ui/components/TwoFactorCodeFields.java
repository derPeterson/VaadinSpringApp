package de.derpeterson.app.ui.components;

import com.flowingcode.vaadin.addons.otpfield.OtpCompleteEvent;
import com.flowingcode.vaadin.addons.otpfield.OtpField;
import com.vaadin.flow.component.textfield.Autocomplete;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;

public final class TwoFactorCodeFields {
  private static final String OTP_STYLESHEET_SCRIPT =
      """
      const field = this;
      customElements.whenDefined(field.localName).then(async () => {
        await field.updateComplete;
        const root = field.shadowRoot;
        if (!root || root.querySelector('link[data-cosmos-otp]')) { return; }
        const sheet = document.createElement('link');
        sheet.rel = 'stylesheet';
        sheet.href = $0;
        sheet.dataset.cosmosOtp = '';
        root.append(sheet);
      });
      """;
  private static final String RECOVERY_CODE_EXAMPLE = "XXXXXXXX-XXXXXXXX-XXXXXXXX-XXXXXXXX";

  private TwoFactorCodeFields() {}

  public static void configureOtpField(OtpField otpField, String contextPath) {
    otpField.setLength(6);
    otpField.setAutofocus(true);
    otpField.setPlaceholder("•");
    otpField.setManualValidation(true);
    otpField.setRequiredIndicatorVisible(true);
    otpField.setValueChangeMode(ValueChangeMode.ON_CHANGE);
    otpField.addClassName("two-factor-otp");
    String stylesheetPath = contextPath + "/custom-theme/cosmos-mfa-otp.css";
    otpField.addAttachListener(
        attachEvent -> otpField.getElement().executeJs(OTP_STYLESHEET_SCRIPT, stylesheetPath));
  }

  public static boolean isCurrentCompleteOtp(OtpField otpField, OtpCompleteEvent otpCompleteEvent) {
    // Genau sechs ASCII-Ziffern und kein veraltetes Ereignis nach Leeren oder Methodenwechsel.
    return otpCompleteEvent.isFromClient()
        && otpField.isAttached()
        && otpField.isVisible()
        && otpField.getValue().matches("\\d{6}")
        && otpField.getValue().equals(otpCompleteEvent.getValue());
  }

  public static void configureRecoveryCodeField(TextField recoveryCodeField) {
    recoveryCodeField.setMaxLength(RECOVERY_CODE_EXAMPLE.length());
    recoveryCodeField.setPlaceholder(RECOVERY_CODE_EXAMPLE);
    recoveryCodeField.setAutocomplete(Autocomplete.OFF);
    recoveryCodeField.addClassName("two-factor-recovery-input");
  }
}
