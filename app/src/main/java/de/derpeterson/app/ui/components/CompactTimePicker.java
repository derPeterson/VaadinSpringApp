package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.timepicker.TimePicker;
import java.util.Locale;

/** Vaadin-Zeitfeld mit zusätzlicher dreistelliger bzw. vierstelliger HHmm-Eingabe. */
@JsModule("./compact-time-input.js")
public class CompactTimePicker extends TimePicker {
  @Override
  protected void onAttach(AttachEvent event) {
    super.onAttach(event);
    installParser();
  }

  @Override
  public void setLocale(Locale locale) {
    super.setLocale(locale);
    installParser();
  }

  private void installParser() {
    getUI()
        .ifPresent(
            ui ->
                ui.beforeClientResponse(
                    this,
                    context ->
                        getElement()
                            .executeJs(
                                "requestAnimationFrame(() => window.Vaadin.Flow.compactTimeInput.install(this))")));
  }
}
