package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.HasAriaLabel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
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

/**
 * Gemeinsames Cosmos-Zeitfeld aus nativer Zahleneingabe und Einheit im Suffix. Einheitenwechsel
 * erhält die eingegebene Zahl. Vorhandene Speicherformate bleiben erhalten; die fachliche
 * Validierung bleibt beim Aufrufer.
 */
public class CosmosDurationField extends Div implements HasAriaLabel {
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
    private final int secondsPerUnit;

    Unit(int secondsPerUnit) {
      this.secondsPerUnit = secondsPerUnit;
    }
  }

  private final MessageProperties messageProperties;
  private final Storage durationStorage;

  // Zahleneingabe und Einheit
  private final IntegerField durationAmountIntegerField = new IntegerField();
  private final BigDecimalField preciseDurationBigDecimalField = new BigDecimalField();
  private final Select<Unit> durationUnitSelect = new Select<>();
  private final Span fixedDurationUnitSpan = new Span();

  // Geladener Zustand und Änderungsbenachrichtigung
  private String defaultDurationText = "1209600";
  private boolean loadingStoredDuration;
  private boolean emptyDaysAllowed = true;
  private SerializableRunnable durationChangeListener = () -> {};

  // Schrittaktionen für präzise Zeitspannen
  private final Button decreaseDurationButton =
      new Button(AppIcon.MINUS.create(), event -> stepDurationValue(-1));
  private final Button increaseDurationButton =
      new Button(AppIcon.PLUS.create(), event -> stepDurationValue(1));

  public CosmosDurationField(MessageProperties messageProperties) {
    this(messageProperties, Storage.SECONDS);
  }

  public CosmosDurationField(MessageProperties messageProperties, Storage durationStorage) {
    this.messageProperties = messageProperties;
    this.durationStorage = durationStorage;
    addClassName("cosmos-duration-field");
    getElement().setAttribute("role", "group");
    setWidthFull();

    configureDurationAmountFields();
    configureDurationUnitSuffix();

    durationAmountIntegerField.addValueChangeListener(event -> publishDurationChange());
    preciseDurationBigDecimalField.addValueChangeListener(event -> publishDurationChange());
    refreshTexts();
    updateDurationMaximumAndPlaceholder();
  }

  private void configureDurationAmountFields() {
    durationAmountIntegerField.setWidthFull();
    durationAmountIntegerField.setMin(1);
    durationAmountIntegerField.setStepButtonsVisible(true);
    durationAmountIntegerField.setClearButtonVisible(true);

    preciseDurationBigDecimalField.setWidthFull();
    preciseDurationBigDecimalField.setClearButtonVisible(true);

    decreaseDurationButton.addClassName("cosmos-duration-step");
    increaseDurationButton.addClassName("cosmos-duration-step");
    preciseDurationBigDecimalField.setPrefixComponent(decreaseDurationButton);

    add(
        durationStorage == Storage.DURATION
            ? preciseDurationBigDecimalField
            : durationAmountIntegerField);
  }

  private void configureDurationUnitSuffix() {
    if (durationStorage == Storage.DAYS) {
      addClassName("cosmos-duration-fixed-unit");
      fixedDurationUnitSpan.addClassName("cosmos-duration-unit-label");
      durationAmountIntegerField.setSuffixComponent(fixedDurationUnitSpan);
      return;
    }

    durationUnitSelect.addClassName("cosmos-duration-unit-select");
    durationUnitSelect.getElement().getThemeList().add("cosmos");
    durationUnitSelect.getElement().getThemeList().add("cosmos-duration-unit");
    durationUnitSelect.setNoVerticalOverlap(true);
    durationUnitSelect.setItems(Unit.values());
    durationUnitSelect.setValue(Unit.DAYS);
    durationUnitSelect.addValueChangeListener(event -> {
      if (event.isFromClient() && event.getValue() == null && event.getOldValue() != null) {
        durationUnitSelect.setValue(event.getOldValue());
        return;
      }

      updateDurationMaximumAndPlaceholder();
      publishDurationChange();
    });

    if (durationStorage == Storage.DURATION) {
      HorizontalLayout durationSuffixLayout =
          new HorizontalLayout(increaseDurationButton, durationUnitSelect);
      durationSuffixLayout.setPadding(false);
      durationSuffixLayout.setSpacing(false);
      durationSuffixLayout.setAlignItems(HorizontalLayout.Alignment.CENTER);
      durationSuffixLayout.addClassName("cosmos-duration-suffix");
      preciseDurationBigDecimalField.setSuffixComponent(durationSuffixLayout);
    } else {
      durationAmountIntegerField.setSuffixComponent(durationUnitSelect);
    }
  }

  private void stepDurationValue(int stepDirection) {
    BigDecimal currentDurationAmount = preciseDurationBigDecimalField.getValue();
    preciseDurationBigDecimalField.setValue(
        (currentDurationAmount == null ? BigDecimal.ZERO : currentDurationAmount)
            .add(BigDecimal.valueOf(stepDirection)));
  }

  /** Pflichtfristen unterscheiden sich von leeren, unbegrenzten Aufbewahrungswerten. */
  public void requireWholeDays(int minimumDays) {
    if (durationStorage != Storage.DAYS || minimumDays < 0) {
      throw new IllegalArgumentException("Non-negative minimum for fixed days required");
    }

    emptyDaysAllowed = false;
    durationAmountIntegerField.setMin(minimumDays);
    updateDurationMaximumAndPlaceholder();
  }

  public void setSecondsValue(int seconds) {
    setStoredValue(Integer.toString(seconds));
  }

  /**
   * Schränkt die angebotenen Einheiten ein. Die aktuelle Einheit bleibt erhalten, sofern sie
   * weiterhin angeboten wird, sonst wird die erste verfügbare Einheit gewählt.
   */
  public void setAvailableUnits(Unit... availableUnits) {
    Unit currentDurationUnit = durationUnitSelect.getValue();
    if (availableUnits.length == 0 || durationStorage == Storage.DAYS) {
      throw new IllegalArgumentException("Selectable duration units required");
    }

    durationUnitSelect.setItems(availableUnits);
    durationUnitSelect.setValue(
        Arrays.asList(availableUnits).contains(currentDurationUnit)
            ? currentDurationUnit
            : availableUnits[0]);
  }

  public void setControlsEnabled(boolean enabled) {
    durationAmountIntegerField.setEnabled(enabled);
    preciseDurationBigDecimalField.setEnabled(enabled);
    durationUnitSelect.setEnabled(enabled);
    decreaseDurationButton.setEnabled(enabled);
    increaseDurationButton.setEnabled(enabled);
  }

  public void setStoredValue(String storedDurationText) {
    loadingStoredDuration = true;

    try {
      if (durationStorage == Storage.DAYS && storedDurationText.isBlank()) {
        durationAmountIntegerField.clear();
        return;
      }

      BigDecimal parsedDurationSeconds = parseDurationSeconds(storedDurationText);

      Unit selectedDurationUnit =
          durationStorage == Storage.DAYS
              ? Unit.DAYS
              : findExactDurationUnit(parsedDurationSeconds);
      if (durationStorage != Storage.DAYS) {
        durationUnitSelect.setValue(selectedDurationUnit);
      }

      BigDecimal durationAmount =
          parsedDurationSeconds
              .divide(BigDecimal.valueOf(selectedDurationUnit.secondsPerUnit))
              .stripTrailingZeros();

      if (durationStorage == Storage.DURATION) {
        preciseDurationBigDecimalField.setValue(durationAmount);
      } else {
        durationAmountIntegerField.setValue(durationAmount.intValueExact());
      }
      updateDurationMaximumAndPlaceholder();
    } finally {
      loadingStoredDuration = false;
    }
  }

  private BigDecimal parseDurationSeconds(String storedDurationText) {
    if (durationStorage != Storage.DURATION) {
      return new BigDecimal(storedDurationText)
          .multiply(BigDecimal.valueOf(durationStorage == Storage.DAYS ? 86400 : 1));
    }
    Duration parsedDuration = DurationStyle.detectAndParse(storedDurationText);
    return BigDecimal.valueOf(parsedDuration.getSeconds())
        .add(BigDecimal.valueOf(parsedDuration.getNano(), 9));
  }

  public String getSecondsValue() {
    BigDecimal durationAmount = readDurationAmount();
    return durationAmount == null
        ? ""
        : durationAmount
            .multiply(BigDecimal.valueOf(getSelectedDurationUnit().secondsPerUnit))
            .toPlainString();
  }

  /** Ungültige Werte erreichen die vorhandene Prüfung und werden niemals zum Standardwert. */
  public String getStoredValue() {
    BigDecimal currentDurationAmount = readDurationAmount();
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
    this.defaultDurationText = defaultDurationText;
    updateDurationMaximumAndPlaceholder();
  }

  public void addChangeListener(SerializableRunnable durationChangeListener) {
    this.durationChangeListener = durationChangeListener;
  }

  private void publishDurationChange() {
    if (!loadingStoredDuration) {
      durationChangeListener.run();
    }
  }

  private BigDecimal readDurationAmount() {
    if (durationStorage == Storage.DURATION) {
      return preciseDurationBigDecimalField.getValue();
    }

    Integer durationAmount = durationAmountIntegerField.getValue();
    return durationAmount == null ? null : BigDecimal.valueOf(durationAmount);
  }

  private void updateDurationMaximumAndPlaceholder() {
    Unit selectedDurationUnit = getSelectedDurationUnit();
    if (selectedDurationUnit == null) {
      return;
    }

    durationAmountIntegerField.setMax(
        durationStorage == Storage.DAYS
            ? Integer.MAX_VALUE
            : Integer.MAX_VALUE / selectedDurationUnit.secondsPerUnit);

    if (durationStorage == Storage.DAYS) {
      durationAmountIntegerField.setPlaceholder(
          emptyDaysAllowed ? messageProperties.getAdminConfigUnlimited() : defaultDurationText);
      return;
    }

    try {
      BigDecimal durationPlaceholderAmount =
          parseDurationSeconds(defaultDurationText)
              .divide(BigDecimal.valueOf(selectedDurationUnit.secondsPerUnit))
              .stripTrailingZeros();

      if (durationStorage == Storage.DURATION) {
        preciseDurationBigDecimalField.setPlaceholder(durationPlaceholderAmount.toPlainString());
      } else {
        durationAmountIntegerField.setPlaceholder(
            durationPlaceholderAmount.scale() <= 0
                ? durationPlaceholderAmount.toPlainString()
                : "");
      }
    } catch (IllegalArgumentException | ArithmeticException _) {
      // Während des Aufbaus hat das Feld noch keinen zum Speicherformat passenden Standardwert.
      preciseDurationBigDecimalField.setPlaceholder("");
    }
  }

  private Unit getSelectedDurationUnit() {
    return durationStorage == Storage.DAYS ? Unit.DAYS : durationUnitSelect.getValue();
  }

  private Unit findExactDurationUnit(BigDecimal durationSeconds) {
    for (Unit candidateDurationUnit : new Unit[] {Unit.DAYS, Unit.HOURS, Unit.MINUTES}) {
      if (durationSeconds.signum() != 0
          && durationSeconds
                  .remainder(BigDecimal.valueOf(candidateDurationUnit.secondsPerUnit))
                  .signum()
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

    BigDecimal parsedDurationSeconds = parseDurationSeconds(storedDurationText);
    Unit displayDurationUnit =
        durationStorage == Storage.DAYS ? Unit.DAYS : findExactDurationUnit(parsedDurationSeconds);
    return formatDurationValue(
        parsedDurationSeconds.divide(BigDecimal.valueOf(displayDurationUnit.secondsPerUnit)),
        displayDurationUnit);
  }

  public String getDisplayValue() {
    BigDecimal currentDurationAmount = readDurationAmount();
    if (currentDurationAmount == null) {
      if (durationStorage == Storage.DAYS) {
        return messageProperties.getAdminConfigUnlimited();
      }
      return durationStorage == Storage.SECONDS
          ? displayStoredValue(defaultDurationText)
          : messageProperties.getAdminConfigDurationInvalid();
    }
    return hasValidDurationAmount()
        ? formatDurationValue(currentDurationAmount, getSelectedDurationUnit())
        : messageProperties.getAdminConfigDurationInvalid();
  }

  public String getEquivalentValue() {
    if (readDurationAmount() == null && durationStorage == Storage.DAYS) {
      return messageProperties.getAdminConfigUnlimited();
    }
    if (!hasValidDurationAmount()) {
      return messageProperties.getAdminConfigDurationInvalid();
    }
    BigDecimal equivalentDurationSeconds =
        readDurationAmount() == null
            ? parseDurationSeconds(defaultDurationText)
            : new BigDecimal(getSecondsValue());
    return messageProperties.getAdminConfigDurationSeconds(
        equivalentDurationSeconds.stripTrailingZeros());
  }

  private boolean hasValidDurationAmount() {
    BigDecimal currentDurationAmount = readDurationAmount();
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
    preciseDurationBigDecimalField.setLocale(CustomI18NProvider.getCurrentLocale());
    if (durationStorage == Storage.DAYS) {
      fixedDurationUnitSpan.setText(messageProperties.getAdminConfigDurationUnitDays());
      durationAmountIntegerField.setAriaDescribedBy(fixedDurationUnitSpan);
    } else {
      durationUnitSelect.setAriaLabel(messageProperties.getAdminConfigDurationUnit());
      durationUnitSelect.setItemLabelGenerator(this::formatDurationUnitLabel);
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
    durationAmountIntegerField.setAriaLabelledBy(labelElementId);
    preciseDurationBigDecimalField.setAriaLabelledBy(labelElementId);
  }

  public void setInvalid(boolean invalid) {
    durationAmountIntegerField.setInvalid(invalid);
    preciseDurationBigDecimalField.setInvalid(invalid);
  }

  public boolean isInvalid() {
    return durationStorage == Storage.DURATION
        ? preciseDurationBigDecimalField.isInvalid()
        : durationAmountIntegerField.isInvalid();
  }

  public void setErrorMessage(String validationErrorMessage) {
    durationAmountIntegerField.setErrorMessage(validationErrorMessage);
    preciseDurationBigDecimalField.setErrorMessage(validationErrorMessage);
  }
}
