package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.timepicker.TimePicker;
import java.util.Locale;

/** Vaadin-Zeitfeld mit zusätzlicher dreistelliger bzw. vierstelliger HHmm-Eingabe. */
@JsModule("./compact-time-input.js")
public class CompactTimePicker extends TimePicker {
  @Override
  protected void onAttach(AttachEvent attachEvent) {
    super.onAttach(attachEvent);
    installParser();
  }

  @Override
  public void setLocale(Locale pickerLocale) {
    super.setLocale(pickerLocale);
    installParser();
  }

  private void installParser() {
    getUI()
        .ifPresent(attachedUi ->
            attachedUi.beforeClientResponse(
                this,
                attachContext ->
                    getElement()
                        .executeJs(
                            "requestAnimationFrame(() => window.Vaadin.Flow.compactTimeInput.install(this))")));
  }
}
