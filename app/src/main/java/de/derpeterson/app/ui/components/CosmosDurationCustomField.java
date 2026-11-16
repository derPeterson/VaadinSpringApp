package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.HasAriaLabel;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.shared.Registration;
import de.derpeterson.app.i18n.MessageProperties;
import java.util.ArrayList;
import java.util.List;

/**
 * Native Feldhülle für das bestehende Zeitfeld. Der Modellwert behält dessen Speicherformat; ein
 * leerer Entwurf bleibt leer und wird weiterhin fachlich beim Speichern geprüft.
 */
public final class CosmosDurationCustomField extends CustomField<String> implements HasAriaLabel {
  private final CosmosDurationField durationField;
  private final List<SerializableRunnable> inputChangeListeners = new ArrayList<>();
  private boolean updatingPresentation;

  public CosmosDurationCustomField(MessageProperties messageProperties) {
    this(messageProperties, CosmosDurationField.Storage.SECONDS);
  }

  public CosmosDurationCustomField(
      MessageProperties messageProperties, CosmosDurationField.Storage durationStorage) {
    this(new CosmosDurationField(messageProperties, durationStorage));
  }

  public CosmosDurationCustomField(CosmosDurationField durationField) {
    // Die Kindfelder liefern bereits synchronisierte Werte samt Ereignisursprung.
    super("", true);
    this.durationField = durationField;
    addClassName("cosmos-duration-custom-field");
    setWidthFull();
    add(durationField);
    durationField.addChangeListener(fromClient -> {
      if (!updatingPresentation) {
        setModelValue(generateModelValue(), fromClient);
        List.copyOf(inputChangeListeners).forEach(SerializableRunnable::run);
      }
    });
    setModelValue(generateModelValue(), false);
  }

  @Override
  public void setValue(String storedDurationText) {
    String normalizedDurationText =
        storedDurationText == null ? getEmptyValue() : storedDurationText;
    // Auch wertgleiche Entwürfe neu darstellen; Modellwerte behalten das kanonische Speicherformat.
    setPresentationValue(normalizedDurationText);
    setModelValue(generateModelValue(), false);
  }

  public void requireWholeDays(int minimumDays) {
    durationField.requireWholeDays(minimumDays);
  }

  public void setAvailableUnits(CosmosDurationField.Unit... durationUnits) {
    durationField.setAvailableUnits(durationUnits);
  }

  public void setDefaultValue(String defaultDurationText) {
    durationField.setDefaultValue(defaultDurationText);
  }

  public void refreshTexts() {
    durationField.refreshTexts();
  }

  public String displayStoredValue(String storedDurationText) {
    return durationField.displayStoredValue(storedDurationText);
  }

  public String getDisplayValue() {
    return durationField.getDisplayValue();
  }

  public String getEquivalentValue() {
    return durationField.getEquivalentValue();
  }

  /** Erfasst auch geänderte ungültige Entwürfe mit demselben Modellwert, etwa „invalid“. */
  public Registration addInputChangeListener(SerializableRunnable inputChangeListener) {
    inputChangeListeners.add(inputChangeListener);
    return () -> inputChangeListeners.remove(inputChangeListener);
  }

  @Override
  public void setAriaLabelledBy(String labelElementId) {
    HasAriaLabel.super.setAriaLabelledBy(labelElementId);
    durationField.setAriaLabelledBy(labelElementId);
  }

  @Override
  protected String generateModelValue() {
    return durationField.getStoredValue();
  }

  @Override
  protected void setPresentationValue(String storedDurationText) {
    updatingPresentation = true;
    try {
      if (storedDurationText == null || storedDurationText.isEmpty()) {
        durationField.clear();
      } else {
        durationField.setStoredValue(storedDurationText);
      }
    } finally {
      updatingPresentation = false;
    }
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    // Auch explizit deaktivierte Schrittbuttons nach einem Read-only-Wechsel aktualisieren.
    durationField.setControlsEnabled(enabled);
  }

  @Override
  public void setReadOnly(boolean readOnly) {
    super.setReadOnly(readOnly);
    durationField.setReadOnly(readOnly);
  }

  @Override
  public void setInvalid(boolean invalid) {
    super.setInvalid(invalid);
    durationField.setInvalid(invalid);
  }
}
