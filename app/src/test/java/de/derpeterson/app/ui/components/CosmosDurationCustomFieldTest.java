package de.derpeterson.app.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.binder.Binder;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

class CosmosDurationCustomFieldTest {
  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void preciseStepButtonsRecoverAfterReadOnlyChangesWhileDisabled(String language) {
    CosmosDurationCustomField customField =
        new CosmosDurationCustomField(
            MessageProperties.forLocale(new CustomI18NProvider(), Locale.forLanguageTag(language)),
            CosmosDurationField.Storage.DURATION);
    CosmosDurationField durationField =
        (CosmosDurationField) ReflectionTestUtils.getField(customField, "durationField");
    Button increaseButton =
        (Button) ReflectionTestUtils.getField(durationField, "increaseDurationButton");
    Button decreaseButton =
        (Button) ReflectionTestUtils.getField(durationField, "decreaseDurationButton");
    customField.setValue("PT2H");
    customField.setEnabled(false);
    customField.setReadOnly(true);
    customField.setReadOnly(false);
    customField.setEnabled(true);

    assertTrue(increaseButton.isEnabled());
    assertTrue(decreaseButton.isEnabled());
    increaseButton.click();
    assertEquals("PT3H", customField.getValue());
    decreaseButton.click();
    assertEquals("PT2H", customField.getValue());

    customField.setReadOnly(true);
    customField.setEnabled(false);
    customField.setEnabled(true);
    assertFalse(increaseButton.isEnabled());
    assertFalse(decreaseButton.isEnabled());
    customField.setReadOnly(false);
    assertTrue(increaseButton.isEnabled());
    assertTrue(decreaseButton.isEnabled());
  }

  @ParameterizedTest
  @CsvSource({
    "de,SECONDS,43200,43200",
    "en,SECONDS,43200,43200",
    "de,DURATION,PT90.123456789S,PT1M30.123456789S",
    "en,DURATION,PT90.123456789S,PT1M30.123456789S",
    "de,DAYS,14,14",
    "en,DAYS,14,14"
  })
  void allStorageFormatsKeepCanonicalValuesEmptyDraftsAndNativeFieldStates(
      String language,
      CosmosDurationField.Storage storage,
      String storedValue,
      String canonicalValue) {
    CosmosDurationCustomField customField =
        new CosmosDurationCustomField(
            MessageProperties.forLocale(new CustomI18NProvider(), Locale.forLanguageTag(language)),
            storage);
    CosmosDurationField durationField =
        (CosmosDurationField) ReflectionTestUtils.getField(customField, "durationField");
    List<String> values = new ArrayList<>();
    customField.addValueChangeListener(event -> values.add(event.getValue()));
    customField.setValue(storedValue);
    assertEquals(canonicalValue, customField.getValue());
    assertEquals(List.of(canonicalValue), values);
    customField.setAriaLabelledBy("duration-label");
    assertEquals("duration-label", durationField.getElement().getAttribute("aria-labelledby"));
    assertEquals("duration-label", amount(durationField).getAriaLabelledBy().orElseThrow());
    BigDecimalField preciseAmountField =
        (BigDecimalField)
            ReflectionTestUtils.getField(durationField, "preciseDurationBigDecimalField");
    customField.setEnabled(false);
    assertFalse(
        storage == CosmosDurationField.Storage.DURATION
            ? preciseAmountField.isEnabled()
            : amount(durationField).isEnabled());
    customField.setEnabled(true);
    assertTrue(
        storage == CosmosDurationField.Storage.DURATION
            ? preciseAmountField.isEnabled()
            : amount(durationField).isEnabled());
    customField.setReadOnly(true);
    assertTrue(amount(durationField).isReadOnly());
    assertTrue(unit(durationField).isReadOnly());
    customField.setReadOnly(false);
    customField.refreshTexts();
    assertEquals(canonicalValue, customField.getValue());
    customField.clear();
    assertEquals("", customField.getValue());
    assertEquals(List.of(canonicalValue, ""), values);
    customField.setValue(null);
    assertEquals(List.of(canonicalValue, ""), values);
  }

  @Test
  void editsOfDifferentInvalidPrecisionsStillNotifyWithoutInventingValueChanges() {
    CosmosDurationCustomField customField =
        new CosmosDurationCustomField(
            MessageProperties.forLocale(new CustomI18NProvider(), Locale.GERMAN),
            CosmosDurationField.Storage.DURATION);
    CosmosDurationField durationField =
        (CosmosDurationField) ReflectionTestUtils.getField(customField, "durationField");
    BigDecimalField amountField =
        (BigDecimalField)
            ReflectionTestUtils.getField(durationField, "preciseDurationBigDecimalField");
    customField.setValue("PT1S");
    AtomicInteger inputChangeCount = new AtomicInteger();
    AtomicInteger valueChangeCount = new AtomicInteger();
    customField.addInputChangeListener(inputChangeCount::incrementAndGet);
    customField.addValueChangeListener(event -> valueChangeCount.incrementAndGet());
    amountField.setValue(new BigDecimal("0.0000000001"));
    amountField.setValue(new BigDecimal("0.0000000002"));
    assertEquals("invalid", customField.getValue());
    assertEquals(2, inputChangeCount.get());
    assertEquals(1, valueChangeCount.get());
  }

  private CosmosDurationField durationField(String language) {
    CosmosDurationField durationField =
        new CosmosDurationField(
            MessageProperties.forLocale(new CustomI18NProvider(), Locale.forLanguageTag(language)));
    durationField.setAvailableUnits(
        CosmosDurationField.Unit.MINUTES,
        CosmosDurationField.Unit.HOURS,
        CosmosDurationField.Unit.DAYS);
    return durationField;
  }

  private IntegerField amount(CosmosDurationField durationField) {
    return (IntegerField) ReflectionTestUtils.getField(durationField, "durationAmountIntegerField");
  }

  @SuppressWarnings("unchecked")
  private Select<CosmosDurationField.Unit> unit(CosmosDurationField durationField) {
    return (Select<CosmosDurationField.Unit>)
        ReflectionTestUtils.getField(durationField, "durationUnitSelect");
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void nativeValuePreservesUnitsEventOriginAndEmptyDrafts(String language) {
    CosmosDurationField durationField = durationField(language);
    CosmosDurationCustomField customField = new CosmosDurationCustomField(durationField);
    List<Boolean> eventOrigins = new ArrayList<>();
    List<String> eventValues = new ArrayList<>();
    customField.addValueChangeListener(event -> {
      eventOrigins.add(event.isFromClient());
      eventValues.add(event.getValue());
    });
    customField.setValue("43200");
    assertEquals(12, amount(durationField).getValue());
    assertEquals(CosmosDurationField.Unit.HOURS, unit(durationField).getValue());
    assertEquals(List.of(false), eventOrigins);
    ReflectionTestUtils.invokeMethod(
        unit(durationField), "setModelValue", CosmosDurationField.Unit.DAYS, true);
    assertEquals(12, amount(durationField).getValue());
    assertEquals("1036800", customField.getValue());
    assertEquals(List.of(false, true), eventOrigins);
    ReflectionTestUtils.invokeMethod(amount(durationField), "setModelValue", 2, true);
    assertEquals("172800", customField.getValue());
    assertEquals(CosmosDurationField.Unit.DAYS, unit(durationField).getValue());
    amount(durationField).clear();
    assertEquals("", customField.getValue());
    assertEquals(List.of(false, true, true, false), eventOrigins);
    assertEquals(List.of("43200", "1036800", "172800", ""), eventValues);
    customField.setValue("43200");
    customField.clear();
    assertTrue(amount(durationField).isEmpty());
    assertEquals("", customField.getValue());
    customField.setValue(null);
    assertEquals("", customField.getValue());
  }

  @Test
  void reloadingAnEquivalentDraftRestoresTheUnitWithoutAnExtraValueChange() {
    CosmosDurationField durationField = durationField("de");
    CosmosDurationCustomField customField = new CosmosDurationCustomField(durationField);
    customField.setValue("43200");
    unit(durationField).setValue(CosmosDurationField.Unit.MINUTES);
    amount(durationField).setValue(720);
    assertEquals("43200", customField.getValue());
    AtomicInteger changeCount = new AtomicInteger();
    customField.addValueChangeListener(event -> changeCount.incrementAndGet());
    customField.setValue("43200");
    assertEquals(12, amount(durationField).getValue());
    assertEquals(CosmosDurationField.Unit.HOURS, unit(durationField).getValue());
    assertEquals(0, changeCount.get());
  }

  @Test
  void binderRejectsInvalidDraftWithoutReplacingTheLastValidValue() {
    CosmosDurationField durationField = durationField("de");
    CosmosDurationCustomField customField = new CosmosDurationCustomField(durationField);
    List<String> storedValues = new ArrayList<>();
    Binder<List<String>> binder = new Binder<>();
    binder
        .forField(customField)
        .withValidator(value -> !value.isBlank() && Long.parseLong(value) >= 60, "Invalid duration")
        .bind(
            values -> values.getFirst(),
            (values, value) -> {
              values.set(0, value);
            });
    storedValues.add("43200");
    binder.setBean(storedValues);
    amount(durationField).setValue(0);
    assertFalse(binder.isValid());
    assertTrue(customField.isInvalid());
    assertTrue(amount(durationField).isInvalid());
    assertEquals("43200", storedValues.getFirst());
    amount(durationField).setValue(2);
    assertTrue(binder.isValid());
    assertFalse(customField.isInvalid());
    assertEquals("7200", storedValues.getFirst());
    customField.setReadOnly(true);
    assertTrue(amount(durationField).isReadOnly());
    assertTrue(unit(durationField).isReadOnly());
    customField.setReadOnly(false);
    assertFalse(amount(durationField).isReadOnly());
  }
}
