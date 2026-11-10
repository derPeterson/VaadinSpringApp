package de.derpeterson.app.helper.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasLabel;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import java.util.Map;
import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ComponentTextUpdateHelper {
  public static void updateComponentTexts(
      Map<Component, Supplier<String>> componentTranslationSupplierMap) {
    componentTranslationSupplierMap.forEach(ComponentTextUpdateHelper::updateComponentText);
  }

  private static void updateComponentText(
      Component translatedComponent, Supplier<String> translationSupplier) {
    if (translatedComponent instanceof HasText hasTextComponent) {
      hasTextComponent.setText(translationSupplier.get());
    }

    if (translatedComponent instanceof HasLabel hasLabelComponent) {
      hasLabelComponent.setLabel(translationSupplier.get());
    }
    if (translatedComponent instanceof EmailField emailFieldComponent) {
      emailFieldComponent.setLabel(translationSupplier.get());
    }
    if (translatedComponent instanceof PasswordField passwordFieldComponent) {
      passwordFieldComponent.setLabel(translationSupplier.get());
    }
    if (translatedComponent instanceof Checkbox checkBoxComponent) {
      checkBoxComponent.setLabel(translationSupplier.get());
    }
    if (translatedComponent instanceof DatePicker datePickerComponent) {
      datePickerComponent.setLabel(translationSupplier.get());
    }
  }
}
