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

  public static void configureOtp(OtpField field, String contextPath) {
    field.setLength(6);
    field.setAutofocus(true);
    field.setPlaceholder("•");
    field.setManualValidation(true);
    field.setRequiredIndicatorVisible(true);
    field.setValueChangeMode(ValueChangeMode.ON_CHANGE);
    field.addClassName("two-factor-otp");
    String stylesheetPath = contextPath + "/custom-theme/cosmos-mfa-otp.css";
    field.addAttachListener(
        event -> field.getElement().executeJs(OTP_STYLESHEET_SCRIPT, stylesheetPath));
  }

  public static boolean isCurrentCompleteOtp(OtpField field, OtpCompleteEvent event) {
    // Genau sechs ASCII-Ziffern und kein veraltetes Ereignis nach Leeren oder Methodenwechsel.
    return event.isFromClient()
        && field.isAttached()
        && field.isVisible()
        && field.getValue().matches("\\d{6}")
        && field.getValue().equals(event.getValue());
  }

  public static void configureRecoveryCode(TextField field) {
    field.setMaxLength(RECOVERY_CODE_EXAMPLE.length());
    field.setPlaceholder(RECOVERY_CODE_EXAMPLE);
    field.setAutocomplete(Autocomplete.OFF);
    field.addClassName("two-factor-recovery-input");
  }
}
