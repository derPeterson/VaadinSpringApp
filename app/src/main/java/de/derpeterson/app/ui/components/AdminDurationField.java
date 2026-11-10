package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.HasAriaLabel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.select.Select;
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
  private final Storage storage;
  private final IntegerField durationAmountField = new IntegerField();
  private final BigDecimalField preciseDurationField = new BigDecimalField();
  private final Select<Unit> durationUnitSelector = new Select<>();
  private String defaultValue = "1209600";
  private boolean loading;
  private SerializableRunnable changeListener = () -> {};
  private final Button decreaseDurationButton =
      new Button(AppIcon.MINUS.create(), event -> step(-1));
  private final Button increaseDurationButton = new Button(AppIcon.PLUS.create(), event -> step(1));

  public AdminDurationField(MessageProperties messageProperties) {
    this(messageProperties, Storage.SECONDS);
  }

  public AdminDurationField(MessageProperties messageProperties, Storage storage) {
    this.messageProperties = messageProperties;
    this.storage = storage;
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
    durationUnitSelector.setItems(storage == Storage.DAYS ? new Unit[] {Unit.DAYS} : Unit.values());
    durationUnitSelector.setEmptySelectionAllowed(false);
    durationUnitSelector.setReadOnly(storage == Storage.DAYS);
    durationUnitSelector.getThemeNames().add("cosmos-reference");
    durationUnitSelector.setValue(Unit.DAYS);
    if (storage == Storage.DURATION) {
      add(preciseDurationField, durationUnitSelector);
    } else {
      add(durationAmountField, durationUnitSelector);
    }
    durationAmountField.addValueChangeListener(event -> changed());
    preciseDurationField.addValueChangeListener(event -> changed());
    durationUnitSelector.addValueChangeListener(event -> {
      updateLimits();
      changed();
    });
    refreshTexts();
    updateLimits();
  }

  private void step(int direction) {
    BigDecimal current = preciseDurationField.getValue();
    preciseDurationField.setValue(
        (current == null ? BigDecimal.ZERO : current).add(BigDecimal.valueOf(direction)));
  }

  public void setSecondsValue(int seconds) {
    setStoredValue(Integer.toString(seconds));
  }

  /**
   * Schränkt nur die angebotenen Einheiten ein; vorhandene Konfigurationsfelder bleiben
   * unverändert.
   */
  public void setAvailableUnits(Unit... available) {
    Unit current = durationUnitSelector.getValue();
    if (available.length == 0 || storage == Storage.DAYS) {
      throw new IllegalArgumentException("Selectable duration units required");
    }
    durationUnitSelector.setItems(available);
    durationUnitSelector.setValue(
        Arrays.asList(available).contains(current) ? current : available[0]);
  }

  public void setControlsEnabled(boolean enabled) {
    durationAmountField.setEnabled(enabled);
    preciseDurationField.setEnabled(enabled);
    durationUnitSelector.setEnabled(enabled);
    decreaseDurationButton.setEnabled(enabled);
    increaseDurationButton.setEnabled(enabled);
  }

  public void setStoredValue(String value) {
    loading = true;
    try {
      if (storage == Storage.DAYS && value.isBlank()) {
        durationUnitSelector.setValue(Unit.DAYS);
        durationAmountField.clear();
        return;
      }
      BigDecimal seconds = decode(value);
      Unit selected = storage == Storage.DAYS ? Unit.DAYS : exactUnit(seconds);
      durationUnitSelector.setValue(selected);
      BigDecimal number = seconds.divide(BigDecimal.valueOf(selected.seconds)).stripTrailingZeros();
      if (storage == Storage.DURATION) {
        preciseDurationField.setValue(number);
      } else {
        durationAmountField.setValue(number.intValueExact());
      }
      updateLimits();
    } finally {
      loading = false;
    }
  }

  private BigDecimal decode(String value) {
    if (storage != Storage.DURATION) {
      return new BigDecimal(value)
          .multiply(BigDecimal.valueOf(storage == Storage.DAYS ? 86400 : 1));
    }
    Duration duration = DurationStyle.detectAndParse(value);
    return BigDecimal.valueOf(duration.getSeconds()).add(BigDecimal.valueOf(duration.getNano(), 9));
  }

  public String getSecondsValue() {
    BigDecimal number = number();
    return number == null
        ? ""
        : number
            .multiply(BigDecimal.valueOf(durationUnitSelector.getValue().seconds))
            .toPlainString();
  }

  /** Ungültige Werte erreichen die vorhandene Prüfung und werden niemals zum Standardwert. */
  public String getStoredValue() {
    BigDecimal current = number();
    if (current == null) {
      return "";
    }
    if (storage == Storage.SECONDS) {
      return getSecondsValue();
    }
    if (storage == Storage.DAYS) {
      return current.toPlainString();
    }
    try {
      BigInteger nanos = new BigDecimal(getSecondsValue()).movePointRight(9).toBigIntegerExact();
      BigInteger[] parts = nanos.divideAndRemainder(BigInteger.valueOf(1_000_000_000));
      return Duration.ofSeconds(parts[0].longValueExact(), parts[1].longValueExact()).toString();
    } catch (ArithmeticException _) {
      return "invalid";
    }
  }

  public void setDefaultValue(String value) {
    defaultValue = value;
    updateLimits();
  }

  public void addChangeListener(SerializableRunnable listener) {
    changeListener = listener;
  }

  private void changed() {
    if (!loading) {
      changeListener.run();
    }
  }

  private BigDecimal number() {
    if (storage == Storage.DURATION) {
      return preciseDurationField.getValue();
    }
    Integer value = durationAmountField.getValue();
    return value == null ? null : BigDecimal.valueOf(value);
  }

  private void updateLimits() {
    Unit selected = durationUnitSelector.getValue();
    if (selected == null) {
      return;
    }
    durationAmountField.setMax(
        storage == Storage.DAYS ? Integer.MAX_VALUE : Integer.MAX_VALUE / selected.seconds);
    if (storage == Storage.DAYS) {
      durationAmountField.setPlaceholder(messageProperties.getAdminConfigUnlimited());
      return;
    }
    try {
      BigDecimal placeholder =
          decode(defaultValue).divide(BigDecimal.valueOf(selected.seconds)).stripTrailingZeros();
      if (storage == Storage.DURATION) {
        preciseDurationField.setPlaceholder(placeholder.toPlainString());
      } else {
        durationAmountField.setPlaceholder(
            placeholder.scale() <= 0 ? placeholder.toPlainString() : "");
      }
    } catch (IllegalArgumentException | ArithmeticException _) {
      // Während des Aufbaus hat das Feld noch keinen zum Speicherformat passenden Standardwert.
      preciseDurationField.setPlaceholder("");
    }
  }

  private Unit exactUnit(BigDecimal seconds) {
    for (Unit candidate : new Unit[] {Unit.DAYS, Unit.HOURS, Unit.MINUTES}) {
      if (seconds.signum() != 0
          && seconds.remainder(BigDecimal.valueOf(candidate.seconds)).signum() == 0) {
        return candidate;
      }
    }
    return Unit.SECONDS;
  }

  public String displayStoredValue(String value) {
    if (value.isBlank()) {
      return messageProperties.getAdminConfigDefaultEmpty();
    }
    BigDecimal seconds = decode(value);
    Unit selected = storage == Storage.DAYS ? Unit.DAYS : exactUnit(seconds);
    return format(seconds.divide(BigDecimal.valueOf(selected.seconds)), selected);
  }

  public String getDisplayValue() {
    BigDecimal current = number();
    if (current == null) {
      if (storage == Storage.DAYS) {
        return messageProperties.getAdminConfigUnlimited();
      }
      return storage == Storage.SECONDS
          ? displayStoredValue(defaultValue)
          : messageProperties.getAdminConfigDurationInvalid();
    }
    return validAmount()
        ? format(current, durationUnitSelector.getValue())
        : messageProperties.getAdminConfigDurationInvalid();
  }

  public String getEquivalentValue() {
    if (number() == null && storage == Storage.DAYS) {
      return messageProperties.getAdminConfigUnlimited();
    }
    if (!validAmount()) {
      return messageProperties.getAdminConfigDurationInvalid();
    }
    BigDecimal seconds =
        number() == null ? decode(defaultValue) : new BigDecimal(getSecondsValue());
    return messageProperties.getAdminConfigDurationSeconds(seconds.stripTrailingZeros());
  }

  private boolean validAmount() {
    BigDecimal current = number();
    if (current == null) {
      return storage != Storage.DURATION;
    }
    if (storage == Storage.DURATION) {
      return !getStoredValue().equals("invalid");
    }
    BigDecimal value = storage == Storage.DAYS ? current : new BigDecimal(getSecondsValue());
    return value.compareTo(BigDecimal.ONE) >= 0
        && value.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) <= 0;
  }

  private String format(BigDecimal value, Unit selected) {
    BigDecimal normalized = value.stripTrailingZeros();
    return switch (selected) {
      case SECONDS -> messageProperties.getAdminConfigDurationSeconds(normalized);
      case MINUTES -> messageProperties.getAdminConfigDurationMinutes(normalized);
      case HOURS -> messageProperties.getAdminConfigDurationHours(normalized);
      case DAYS -> messageProperties.getAdminConfigDurationDays(normalized);
    };
  }

  public void refreshTexts() {
    preciseDurationField.setLocale(CustomI18NProvider.getCurrentLocale());
    durationUnitSelector.setAriaLabel(messageProperties.getAdminConfigDurationUnit());
    durationUnitSelector.setItemLabelGenerator(value ->
        switch (value) {
          case SECONDS -> messageProperties.getAdminConfigDurationUnitSeconds();
          case MINUTES -> messageProperties.getAdminConfigDurationUnitMinutes();
          case HOURS -> messageProperties.getAdminConfigDurationUnitHours();
          case DAYS -> messageProperties.getAdminConfigDurationUnitDays();
        });
    decreaseDurationButton.setAriaLabel(messageProperties.getAdminConfigDurationDecrease());
    increaseDurationButton.setAriaLabel(messageProperties.getAdminConfigDurationIncrease());
    updateLimits();
  }

  @Override
  public void setAriaLabelledBy(String id) {
    getElement().setAttribute("aria-labelledby", id);
    durationAmountField.setAriaLabelledBy(id);
    preciseDurationField.setAriaLabelledBy(id);
  }

  public void setInvalid(boolean invalid) {
    durationAmountField.setInvalid(invalid);
    preciseDurationField.setInvalid(invalid);
  }

  public boolean isInvalid() {
    return storage == Storage.DURATION
        ? preciseDurationField.isInvalid()
        : durationAmountField.isInvalid();
  }

  public void setErrorMessage(String message) {
    durationAmountField.setErrorMessage(message);
    preciseDurationField.setErrorMessage(message);
  }
}
