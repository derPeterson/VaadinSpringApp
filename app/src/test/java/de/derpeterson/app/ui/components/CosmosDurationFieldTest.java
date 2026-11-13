package de.derpeterson.app.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CosmosDurationFieldTest {
  @ParameterizedTest
  @CsvSource({"de,SECONDS", "en,SECONDS", "de,DURATION", "en,DURATION"})
  void integratedUnitPreservesAmountPrecisionAndChangeNotifications(
      String language, CosmosDurationField.Storage durationStorage) {
    MessageProperties messageProperties =
        MessageProperties.forLocale(new CustomI18NProvider(), Locale.forLanguageTag(language));
    CosmosDurationField cosmosDurationField =
        new CosmosDurationField(messageProperties, durationStorage);
    AtomicInteger durationChangeCount = new AtomicInteger();
    cosmosDurationField.addChangeListener(durationChangeCount::incrementAndGet);
    cosmosDurationField.setStoredValue(
        durationStorage == CosmosDurationField.Storage.DURATION ? "PT90.125S" : "90");

    Component durationAmountComponent = cosmosDurationField.getChildren().findFirst().orElseThrow();
    Select<CosmosDurationField.Unit> durationUnitSelect =
        findDurationUnitSelect(durationAmountComponent);

    assertEquals(0, durationChangeCount.get());
    assertEquals(
        messageProperties.getAdminConfigDurationUnit(),
        durationUnitSelect.getAriaLabel().orElseThrow());
    assertFalse(durationUnitSelect.isEmptySelectionAllowed());
    assertTrue(durationUnitSelect.isNoVerticalOverlap());
    durationUnitSelect.setValue(CosmosDurationField.Unit.MINUTES);

    assertEquals(
        durationStorage == CosmosDurationField.Storage.DURATION ? "5407.500" : "5400",
        cosmosDurationField.getSecondsValue());
    assertEquals(1, durationChangeCount.get());
    cosmosDurationField.setDefaultValue(
        durationStorage == CosmosDurationField.Storage.DURATION ? "PT120S" : "120");

    assertEquals(
        "2",
        durationAmountComponent instanceof BigDecimalField preciseDurationBigDecimalField
            ? preciseDurationBigDecimalField.getPlaceholder()
            : ((IntegerField) durationAmountComponent).getPlaceholder());
    cosmosDurationField.refreshTexts();

    assertEquals(CosmosDurationField.Unit.MINUTES, durationUnitSelect.getValue());
    assertEquals(1, durationChangeCount.get());
    cosmosDurationField.setAvailableUnits(
        CosmosDurationField.Unit.MINUTES,
        CosmosDurationField.Unit.HOURS,
        CosmosDurationField.Unit.DAYS);

    assertEquals(
        List.of(
            CosmosDurationField.Unit.MINUTES,
            CosmosDurationField.Unit.HOURS,
            CosmosDurationField.Unit.DAYS),
        durationUnitSelect.getListDataView().getItems().toList());
    cosmosDurationField.setControlsEnabled(false);

    assertFalse(durationUnitSelect.isEnabled());
    cosmosDurationField.setControlsEnabled(true);

    assertTrue(durationUnitSelect.isEnabled());
  }

  @ParameterizedTest
  @CsvSource({"de", "en"})
  void fixedDaysKeepTheirSuffixAndMinimumWithoutAUnitSelector(String language) {
    MessageProperties messageProperties =
        MessageProperties.forLocale(new CustomI18NProvider(), Locale.forLanguageTag(language));
    CosmosDurationField cosmosDurationField =
        new CosmosDurationField(messageProperties, CosmosDurationField.Storage.DAYS);
    IntegerField durationAmountIntegerField =
        assertInstanceOf(
            IntegerField.class, cosmosDurationField.getChildren().findFirst().orElseThrow());
    Component fixedUnitComponent = durationAmountIntegerField.getSuffixComponent();

    assertEquals(
        messageProperties.getAdminConfigDurationUnitDays(),
        fixedUnitComponent.getElement().getText());
    assertFalse(fixedUnitComponent instanceof Select<?>);
    cosmosDurationField.requireWholeDays(1);
    cosmosDurationField.setStoredValue("30");
    cosmosDurationField.refreshTexts();

    assertSame(fixedUnitComponent, durationAmountIntegerField.getSuffixComponent());
    assertEquals(1, durationAmountIntegerField.getMin());
    assertEquals("30", cosmosDurationField.getStoredValue());
  }

  @ParameterizedTest
  @CsvSource({"de", "en"})
  void preciseDurationKeepsItsIncreaseButtonInTheLayoutsDefaultSlot(String language) {
    MessageProperties messageProperties =
        MessageProperties.forLocale(new CustomI18NProvider(), Locale.forLanguageTag(language));
    CosmosDurationField cosmosDurationField =
        new CosmosDurationField(messageProperties, CosmosDurationField.Storage.DURATION);
    cosmosDurationField.setStoredValue("PT90.125S");
    BigDecimalField durationAmountBigDecimalField =
        assertInstanceOf(
            BigDecimalField.class, cosmosDurationField.getChildren().findFirst().orElseThrow());
    HorizontalLayout durationSuffixLayout =
        assertInstanceOf(
            HorizontalLayout.class, durationAmountBigDecimalField.getSuffixComponent());
    List<Component> suffixComponents = durationSuffixLayout.getChildren().toList();
    Button increaseDurationButton = assertInstanceOf(Button.class, suffixComponents.getFirst());

    assertEquals(2, suffixComponents.size());
    assertInstanceOf(Select.class, suffixComponents.getLast());
    // Ein alter slot="suffix" würde den Button im HorizontalLayout unsichtbar machen.
    assertFalse(increaseDurationButton.getElement().hasAttribute("slot"));
    increaseDurationButton.click();

    assertEquals("91.125", cosmosDurationField.getSecondsValue());
  }

  @SuppressWarnings("unchecked")
  private Select<CosmosDurationField.Unit> findDurationUnitSelect(
      Component durationAmountComponent) {
    Component durationSuffixComponent;
    if (durationAmountComponent instanceof BigDecimalField preciseDurationBigDecimalField) {
      HorizontalLayout durationSuffixLayout =
          assertInstanceOf(
              HorizontalLayout.class, preciseDurationBigDecimalField.getSuffixComponent());
      durationSuffixComponent =
          durationSuffixLayout
              .getChildren()
              .filter(Select.class::isInstance)
              .findFirst()
              .orElseThrow();
    } else {
      IntegerField durationAmountIntegerField =
          assertInstanceOf(IntegerField.class, durationAmountComponent);
      durationSuffixComponent = durationAmountIntegerField.getSuffixComponent();
    }

    return (Select<CosmosDurationField.Unit>)
        assertInstanceOf(Select.class, durationSuffixComponent);
  }
}
