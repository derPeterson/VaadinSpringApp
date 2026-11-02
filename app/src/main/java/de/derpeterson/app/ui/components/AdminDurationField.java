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

  private final MessageProperties texts;
  private final Storage storage;
  private final IntegerField amount = new IntegerField();
  private final BigDecimalField preciseAmount = new BigDecimalField();
  private final Select<Unit> unit = new Select<>();
  private String defaultValue = "1209600";
  private boolean loading;
  private SerializableRunnable changeListener = () -> {};
  private final Button decrease = new Button(AppIcon.MINUS.create(), event -> step(-1));
  private final Button increase = new Button(AppIcon.PLUS.create(), event -> step(1));

  public AdminDurationField(MessageProperties texts) {
    this(texts, Storage.SECONDS);
  }

  public AdminDurationField(MessageProperties texts, Storage storage) {
    this.texts = texts;
    this.storage = storage;
    addClassName("admin-duration-field");
    getElement().setAttribute("role", "group");
    setWidthFull();
    amount.setWidthFull();
    amount.setMin(1);
    amount.setStepButtonsVisible(true);
    amount.setClearButtonVisible(true);
    preciseAmount.setWidthFull();
    preciseAmount.setClearButtonVisible(true);
    decrease.addClassName("admin-duration-step");
    increase.addClassName("admin-duration-step");
    preciseAmount.setPrefixComponent(decrease);
    preciseAmount.setSuffixComponent(increase);
    unit.setItems(storage == Storage.DAYS ? new Unit[] {Unit.DAYS} : Unit.values());
    unit.setEmptySelectionAllowed(false);
    unit.setReadOnly(storage == Storage.DAYS);
    unit.getThemeNames().add("cosmos-reference");
    unit.setValue(Unit.DAYS);
    if (storage == Storage.DURATION) {
      add(preciseAmount, unit);
    } else {
      add(amount, unit);
    }
    amount.addValueChangeListener(event -> changed());
    preciseAmount.addValueChangeListener(event -> changed());
    unit.addValueChangeListener(
        event -> {
          updateLimits();
          changed();
        });
    refreshTexts();
    updateLimits();
  }

  private void step(int direction) {
    BigDecimal current = preciseAmount.getValue();
    preciseAmount.setValue(
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
    Unit current = unit.getValue();
    if (available.length == 0 || storage == Storage.DAYS) {
      throw new IllegalArgumentException("Selectable duration units required");
    }
    unit.setItems(available);
    unit.setValue(Arrays.asList(available).contains(current) ? current : available[0]);
  }

  public void setControlsEnabled(boolean enabled) {
    amount.setEnabled(enabled);
    preciseAmount.setEnabled(enabled);
    unit.setEnabled(enabled);
    decrease.setEnabled(enabled);
    increase.setEnabled(enabled);
  }

  public void setStoredValue(String value) {
    loading = true;
    try {
      if (storage == Storage.DAYS && value.isBlank()) {
        unit.setValue(Unit.DAYS);
        amount.clear();
        return;
      }
      BigDecimal seconds = decode(value);
      Unit selected = storage == Storage.DAYS ? Unit.DAYS : exactUnit(seconds);
      unit.setValue(selected);
      BigDecimal number = seconds.divide(BigDecimal.valueOf(selected.seconds)).stripTrailingZeros();
      if (storage == Storage.DURATION) {
        preciseAmount.setValue(number);
      } else {
        amount.setValue(number.intValueExact());
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
        : number.multiply(BigDecimal.valueOf(unit.getValue().seconds)).toPlainString();
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
      return preciseAmount.getValue();
    }
    Integer value = amount.getValue();
    return value == null ? null : BigDecimal.valueOf(value);
  }

  private void updateLimits() {
    Unit selected = unit.getValue();
    if (selected == null) {
      return;
    }
    amount.setMax(
        storage == Storage.DAYS ? Integer.MAX_VALUE : Integer.MAX_VALUE / selected.seconds);
    if (storage == Storage.DAYS) {
      amount.setPlaceholder(texts.getAdminConfigUnlimited());
      return;
    }
    try {
      BigDecimal placeholder =
          decode(defaultValue).divide(BigDecimal.valueOf(selected.seconds)).stripTrailingZeros();
      if (storage == Storage.DURATION) {
        preciseAmount.setPlaceholder(placeholder.toPlainString());
      } else {
        amount.setPlaceholder(placeholder.scale() <= 0 ? placeholder.toPlainString() : "");
      }
    } catch (IllegalArgumentException | ArithmeticException _) {
      // Während des Aufbaus hat das Feld noch keinen zum Speicherformat passenden Standardwert.
      preciseAmount.setPlaceholder("");
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
      return texts.getAdminConfigDefaultEmpty();
    }
    BigDecimal seconds = decode(value);
    Unit selected = storage == Storage.DAYS ? Unit.DAYS : exactUnit(seconds);
    return format(seconds.divide(BigDecimal.valueOf(selected.seconds)), selected);
  }

  public String getDisplayValue() {
    BigDecimal current = number();
    if (current == null) {
      if (storage == Storage.DAYS) {
        return texts.getAdminConfigUnlimited();
      }
      return storage == Storage.SECONDS
          ? displayStoredValue(defaultValue)
          : texts.getAdminConfigDurationInvalid();
    }
    return validAmount() ? format(current, unit.getValue()) : texts.getAdminConfigDurationInvalid();
  }

  public String getEquivalentValue() {
    if (number() == null && storage == Storage.DAYS) {
      return texts.getAdminConfigUnlimited();
    }
    if (!validAmount()) {
      return texts.getAdminConfigDurationInvalid();
    }
    BigDecimal seconds =
        number() == null ? decode(defaultValue) : new BigDecimal(getSecondsValue());
    return texts.getAdminConfigDurationSeconds(seconds.stripTrailingZeros());
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
      case SECONDS -> texts.getAdminConfigDurationSeconds(normalized);
      case MINUTES -> texts.getAdminConfigDurationMinutes(normalized);
      case HOURS -> texts.getAdminConfigDurationHours(normalized);
      case DAYS -> texts.getAdminConfigDurationDays(normalized);
    };
  }

  public void refreshTexts() {
    preciseAmount.setLocale(CustomI18NProvider.getCurrentLocale());
    unit.setAriaLabel(texts.getAdminConfigDurationUnit());
    unit.setItemLabelGenerator(
        value ->
            switch (value) {
              case SECONDS -> texts.getAdminConfigDurationUnitSeconds();
              case MINUTES -> texts.getAdminConfigDurationUnitMinutes();
              case HOURS -> texts.getAdminConfigDurationUnitHours();
              case DAYS -> texts.getAdminConfigDurationUnitDays();
            });
    decrease.setAriaLabel(texts.getAdminConfigDurationDecrease());
    increase.setAriaLabel(texts.getAdminConfigDurationIncrease());
    updateLimits();
  }

  @Override
  public void setAriaLabelledBy(String id) {
    getElement().setAttribute("aria-labelledby", id);
    amount.setAriaLabelledBy(id);
    preciseAmount.setAriaLabelledBy(id);
  }

  public void setInvalid(boolean invalid) {
    amount.setInvalid(invalid);
    preciseAmount.setInvalid(invalid);
  }

  public boolean isInvalid() {
    return storage == Storage.DURATION ? preciseAmount.isInvalid() : amount.isInvalid();
  }

  public void setErrorMessage(String message) {
    amount.setErrorMessage(message);
    preciseAmount.setErrorMessage(message);
  }
}
