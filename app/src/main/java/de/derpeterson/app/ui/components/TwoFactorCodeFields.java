package de.derpeterson.app.ui.components;

import com.flowingcode.vaadin.addons.otpfield.OtpCompleteEvent;
import com.flowingcode.vaadin.addons.otpfield.OtpField;
import com.vaadin.flow.component.textfield.Autocomplete;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;

public final class TwoFactorCodeFields {
  private static final String RECOVERY_CODE_EXAMPLE = "XXXXXXXX-XXXXXXXX-XXXXXXXX-XXXXXXXX";

  private TwoFactorCodeFields() {}

  public static void configureOtpField(OtpField otpField) {
    otpField.setLength(6);
    otpField.setAutofocus(true);
    otpField.setPlaceholder("•");
    otpField.setManualValidation(true);
    otpField.setRequiredIndicatorVisible(true);
    otpField.setValueChangeMode(ValueChangeMode.ON_CHANGE);
    otpField.addClassName("two-factor-otp");
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
    recoveryCodeField.setClearButtonVisible(true);
    recoveryCodeField.setMaxLength(RECOVERY_CODE_EXAMPLE.length());
    recoveryCodeField.setPlaceholder(RECOVERY_CODE_EXAMPLE);
    recoveryCodeField.setAutocomplete(Autocomplete.OFF);
    recoveryCodeField.addClassName("two-factor-recovery-input");
  }
}
