package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.HasAriaLabel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.function.SerializableRunnable;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.Arrays;
import org.springframework.boot.convert.DurationStyle;

/** Zahl und Einheit ohne Rundung. Das Speicherformat und die Fachprüfung bleiben erhalten. */
public class AdminDurationField extends Div implements HasAriaLabel {
  public enum Storage {
    SECONDS,
    DURATION,
    DAYS
  }

  public enum Unit {
    SECONDS(1),
    MINUTES(60),
    HOURS(3600),
    DAYS(86400);
    private final int seconds;

    Unit(int seconds) {
      this.seconds = seconds;
    }
  }

  private final MessageProperties messageProperties;
  private final Storage durationStorage;
  private final IntegerField durationAmountField = new IntegerField();
  private final BigDecimalField preciseDurationField = new BigDecimalField();
  private final ComboBox<Unit> durationUnitComboBox = new ComboBox<>();
  private final Span fixedDurationUnitSpan = new Span();
  private String defaultValue = "1209600";
  private boolean loadingStoredDuration;
  private SerializableRunnable durationChangeListener = () -> {};
  private final Button decreaseDurationButton =
      new Button(AppIcon.MINUS.create(), event -> stepDurationValue(-1));
  private final Button increaseDurationButton =
      new Button(AppIcon.PLUS.create(), event -> stepDurationValue(1));

  public AdminDurationField(MessageProperties messageProperties) {
    this(messageProperties, Storage.SECONDS);
  }

  public AdminDurationField(MessageProperties messageProperties, Storage durationStorage) {
    this.messageProperties = messageProperties;
    this.durationStorage = durationStorage;
    addClassName("admin-duration-field");
    getElement().setAttribute("role", "group");
    setWidthFull();
    durationAmountField.setWidthFull();
    durationAmountField.setMin(1);
    durationAmountField.setStepButtonsVisible(true);
    durationAmountField.setClearButtonVisible(true);
    preciseDurationField.setWidthFull();
    preciseDurationField.setClearButtonVisible(true);
    decreaseDurationButton.addClassName("admin-duration-step");
    increaseDurationButton.addClassName("admin-duration-step");
    preciseDurationField.setPrefixComponent(decreaseDurationButton);
    preciseDurationField.setSuffixComponent(increaseDurationButton);
    add(durationStorage == Storage.DURATION ? preciseDurationField : durationAmountField);
    if (durationStorage == Storage.DAYS) {
      addClassName("admin-duration-fixed-unit");
      fixedDurationUnitSpan.addClassName("admin-duration-unit-label");
      add(fixedDurationUnitSpan);
    } else {
      durationUnitComboBox.setItems(Unit.values());
      durationUnitComboBox.setValue(Unit.DAYS);
      CosmosComboBoxStyling.styleChoice(durationUnitComboBox, this::formatDurationUnitLabel, true);
      durationUnitComboBox.addValueChangeListener(event -> {
        if (event.isFromClient() && event.getValue() == null && event.getOldValue() != null) {
          durationUnitComboBox.setValue(event.getOldValue());
          return;
        }

        updateDurationMaximumAndPlaceholder();
        publishDurationChange();
      });
      add(durationUnitComboBox);
    }
    durationAmountField.addValueChangeListener(event -> publishDurationChange());
    preciseDurationField.addValueChangeListener(event -> publishDurationChange());
    refreshTexts();
    updateDurationMaximumAndPlaceholder();
  }

  private void stepDurationValue(int stepDirection) {
    BigDecimal currentDurationAmount = preciseDurationField.getValue();
    preciseDurationField.setValue(
        (currentDurationAmount == null ? BigDecimal.ZERO : currentDurationAmount)
            .add(BigDecimal.valueOf(stepDirection)));
  }

  public void setSecondsValue(int seconds) {
    setStoredValue(Integer.toString(seconds));
  }

  /**
   * Schränkt nur die angebotenen Einheiten ein; vorhandene Konfigurationsfelder bleiben
   * unverändert.
   */
  public void setAvailableUnits(Unit... availableUnits) {
    Unit currentDurationUnit = durationUnitComboBox.getValue();
    if (availableUnits.length == 0 || durationStorage == Storage.DAYS) {
      throw new IllegalArgumentException("Selectable duration units required");
    }
    durationUnitComboBox.setItems(availableUnits);
    durationUnitComboBox.setValue(
        Arrays.asList(availableUnits).contains(currentDurationUnit)
            ? currentDurationUnit
            : availableUnits[0]);
  }

  public void setControlsEnabled(boolean enabled) {
    durationAmountField.setEnabled(enabled);
    preciseDurationField.setEnabled(enabled);
    durationUnitComboBox.setEnabled(enabled);
    decreaseDurationButton.setEnabled(enabled);
    increaseDurationButton.setEnabled(enabled);
  }

  public void setStoredValue(String storedDurationText) {
    loadingStoredDuration = true;

    try {
      if (durationStorage == Storage.DAYS && storedDurationText.isBlank()) {
        durationAmountField.clear();
        return;
      }

      BigDecimal parsedDurationSeconds = parseDurationValue(storedDurationText);

      Unit selectedDurationUnit =
          durationStorage == Storage.DAYS ? Unit.DAYS : exactUnit(parsedDurationSeconds);
      if (durationStorage != Storage.DAYS) {
        durationUnitComboBox.setValue(selectedDurationUnit);
      }

      BigDecimal durationAmount =
          parsedDurationSeconds
              .divide(BigDecimal.valueOf(selectedDurationUnit.seconds))
              .stripTrailingZeros();

      if (durationStorage == Storage.DURATION) {
        preciseDurationField.setValue(durationAmount);
      } else {
        durationAmountField.setValue(durationAmount.intValueExact());
      }
      updateDurationMaximumAndPlaceholder();
    } finally {
      loadingStoredDuration = false;
    }
  }

  private BigDecimal parseDurationValue(String storedDurationText) {
    if (durationStorage != Storage.DURATION) {
      return new BigDecimal(storedDurationText)
          .multiply(BigDecimal.valueOf(durationStorage == Storage.DAYS ? 86400 : 1));
    }
    Duration parsedDuration = DurationStyle.detectAndParse(storedDurationText);
    return BigDecimal.valueOf(parsedDuration.getSeconds())
        .add(BigDecimal.valueOf(parsedDuration.getNano(), 9));
  }

  public String getSecondsValue() {
    BigDecimal durationAmount = readDurationValue();
    return durationAmount == null
        ? ""
        : durationAmount
            .multiply(BigDecimal.valueOf(getSelectedDurationUnit().seconds))
            .toPlainString();
  }

  /** Ungültige Werte erreichen die vorhandene Prüfung und werden niemals zum Standardwert. */
  public String getStoredValue() {
    BigDecimal currentDurationAmount = readDurationValue();
    if (currentDurationAmount == null) {
      return "";
    }
    if (durationStorage == Storage.SECONDS) {
      return getSecondsValue();
    }
    if (durationStorage == Storage.DAYS) {
      return currentDurationAmount.toPlainString();
    }

    try {
      BigInteger durationNanoseconds =
          new BigDecimal(getSecondsValue()).movePointRight(9).toBigIntegerExact();
      BigInteger[] durationParts =
          durationNanoseconds.divideAndRemainder(BigInteger.valueOf(1_000_000_000));
      return Duration.ofSeconds(
              durationParts[0].longValueExact(), durationParts[1].longValueExact())
          .toString();
    } catch (ArithmeticException _) {
      return "invalid";
    }
  }

  public void setDefaultValue(String defaultDurationText) {
    defaultValue = defaultDurationText;
    updateDurationMaximumAndPlaceholder();
  }

  public void addChangeListener(SerializableRunnable listener) {
    durationChangeListener = listener;
  }

  private void publishDurationChange() {
    if (!loadingStoredDuration) {
      durationChangeListener.run();
    }
  }

  private BigDecimal readDurationValue() {
    if (durationStorage == Storage.DURATION) {
      return preciseDurationField.getValue();
    }

    Integer durationAmount = durationAmountField.getValue();
    return durationAmount == null ? null : BigDecimal.valueOf(durationAmount);
  }

  private void updateDurationMaximumAndPlaceholder() {
    Unit selectedDurationUnit = getSelectedDurationUnit();
    if (selectedDurationUnit == null) {
      return;
    }

    durationAmountField.setMax(
        durationStorage == Storage.DAYS
            ? Integer.MAX_VALUE
            : Integer.MAX_VALUE / selectedDurationUnit.seconds);

    if (durationStorage == Storage.DAYS) {
      durationAmountField.setPlaceholder(messageProperties.getAdminConfigUnlimited());
      return;
    }

    try {
      BigDecimal durationPlaceholderAmount =
          parseDurationValue(defaultValue)
              .divide(BigDecimal.valueOf(selectedDurationUnit.seconds))
              .stripTrailingZeros();

      if (durationStorage == Storage.DURATION) {
        preciseDurationField.setPlaceholder(durationPlaceholderAmount.toPlainString());
      } else {
        durationAmountField.setPlaceholder(
            durationPlaceholderAmount.scale() <= 0
                ? durationPlaceholderAmount.toPlainString()
                : "");
      }
    } catch (IllegalArgumentException | ArithmeticException _) {
      // Während des Aufbaus hat das Feld noch keinen zum Speicherformat passenden Standardwert.
      preciseDurationField.setPlaceholder("");
    }
  }

  private Unit getSelectedDurationUnit() {
    return durationStorage == Storage.DAYS ? Unit.DAYS : durationUnitComboBox.getValue();
  }

  private Unit exactUnit(BigDecimal durationSeconds) {
    for (Unit candidateDurationUnit : new Unit[] {Unit.DAYS, Unit.HOURS, Unit.MINUTES}) {
      if (durationSeconds.signum() != 0
          && durationSeconds.remainder(BigDecimal.valueOf(candidateDurationUnit.seconds)).signum()
              == 0) {
        return candidateDurationUnit;
      }
    }

    return Unit.SECONDS;
  }

  public String displayStoredValue(String storedDurationText) {
    if (storedDurationText.isBlank()) {
      return messageProperties.getAdminConfigDefaultEmpty();
    }

    BigDecimal parsedDurationSeconds = parseDurationValue(storedDurationText);
    Unit displayDurationUnit =
        durationStorage == Storage.DAYS ? Unit.DAYS : exactUnit(parsedDurationSeconds);
    return formatDurationValue(
        parsedDurationSeconds.divide(BigDecimal.valueOf(displayDurationUnit.seconds)),
        displayDurationUnit);
  }

  public String getDisplayValue() {
    BigDecimal currentDurationAmount = readDurationValue();
    if (currentDurationAmount == null) {
      if (durationStorage == Storage.DAYS) {
        return messageProperties.getAdminConfigUnlimited();
      }
      return durationStorage == Storage.SECONDS
          ? displayStoredValue(defaultValue)
          : messageProperties.getAdminConfigDurationInvalid();
    }
    return validAmount()
        ? formatDurationValue(currentDurationAmount, getSelectedDurationUnit())
        : messageProperties.getAdminConfigDurationInvalid();
  }

  public String getEquivalentValue() {
    if (readDurationValue() == null && durationStorage == Storage.DAYS) {
      return messageProperties.getAdminConfigUnlimited();
    }
    if (!validAmount()) {
      return messageProperties.getAdminConfigDurationInvalid();
    }
    BigDecimal equivalentDurationSeconds =
        readDurationValue() == null
            ? parseDurationValue(defaultValue)
            : new BigDecimal(getSecondsValue());
    return messageProperties.getAdminConfigDurationSeconds(
        equivalentDurationSeconds.stripTrailingZeros());
  }

  private boolean validAmount() {
    BigDecimal currentDurationAmount = readDurationValue();
    if (currentDurationAmount == null) {
      return durationStorage != Storage.DURATION;
    }

    if (durationStorage == Storage.DURATION) {
      return !getStoredValue().equals("invalid");
    }
    BigDecimal boundedDurationAmount =
        durationStorage == Storage.DAYS ? currentDurationAmount : new BigDecimal(getSecondsValue());
    return boundedDurationAmount.compareTo(BigDecimal.ONE) >= 0
        && boundedDurationAmount.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) <= 0;
  }

  private String formatDurationValue(BigDecimal durationAmount, Unit displayDurationUnit) {
    BigDecimal normalizedDurationAmount = durationAmount.stripTrailingZeros();
    return switch (displayDurationUnit) {
      case SECONDS -> messageProperties.getAdminConfigDurationSeconds(normalizedDurationAmount);
      case MINUTES -> messageProperties.getAdminConfigDurationMinutes(normalizedDurationAmount);
      case HOURS -> messageProperties.getAdminConfigDurationHours(normalizedDurationAmount);
      case DAYS -> messageProperties.getAdminConfigDurationDays(normalizedDurationAmount);
    };
  }

  public void refreshTexts() {
    preciseDurationField.setLocale(CustomI18NProvider.getCurrentLocale());
    if (durationStorage == Storage.DAYS) {
      fixedDurationUnitSpan.setText(messageProperties.getAdminConfigDurationUnitDays());
      durationAmountField.setAriaDescribedBy(fixedDurationUnitSpan);
    } else {
      durationUnitComboBox.setAriaLabel(messageProperties.getAdminConfigDurationUnit());
      durationUnitComboBox.setItemLabelGenerator(this::formatDurationUnitLabel);
    }
    decreaseDurationButton.setAriaLabel(messageProperties.getAdminConfigDurationDecrease());
    increaseDurationButton.setAriaLabel(messageProperties.getAdminConfigDurationIncrease());
    updateDurationMaximumAndPlaceholder();
  }

  private String formatDurationUnitLabel(Unit durationUnit) {
    return switch (durationUnit) {
      case SECONDS -> messageProperties.getAdminConfigDurationUnitSeconds();
      case MINUTES -> messageProperties.getAdminConfigDurationUnitMinutes();
      case HOURS -> messageProperties.getAdminConfigDurationUnitHours();
      case DAYS -> messageProperties.getAdminConfigDurationUnitDays();
    };
  }

  @Override
  public void setAriaLabelledBy(String labelElementId) {
    getElement().setAttribute("aria-labelledby", labelElementId);
    durationAmountField.setAriaLabelledBy(labelElementId);
    preciseDurationField.setAriaLabelledBy(labelElementId);
  }

  public void setInvalid(boolean invalid) {
    durationAmountField.setInvalid(invalid);
    preciseDurationField.setInvalid(invalid);
  }

  public boolean isInvalid() {
    return durationStorage == Storage.DURATION
        ? preciseDurationField.isInvalid()
        : durationAmountField.isInvalid();
  }

  public void setErrorMessage(String validationErrorMessage) {
    durationAmountField.setErrorMessage(validationErrorMessage);
    preciseDurationField.setErrorMessage(validationErrorMessage);
  }
}
