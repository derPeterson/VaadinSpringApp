package de.derpeterson.app.helper.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasLabel;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.function.Supplier;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ComponentTextUpdateHelper {
    public static void updateToolTipComponents(Map<Tooltip, Supplier<String>> tooltipTranslationSupplierMap) {
        tooltipTranslationSupplierMap.forEach((tooltip, translationSupplier) -> tooltip.setText(translationSupplier.get()));
    }

    public static void updateComponents(Map<Component, Supplier<String>> componentTranslationSupplierMap) {
        componentTranslationSupplierMap.forEach((component, translationSupplier) -> {
            if (component instanceof HasText hasTextComponent) {
                hasTextComponent.setText(translationSupplier.get());
            }
            if (component instanceof HasLabel hasLabelComponent) {
                hasLabelComponent.setLabel(translationSupplier.get());
            }
            if (component instanceof EmailField emailFieldComponent) {
                emailFieldComponent.setLabel(translationSupplier.get());
            }
            if (component instanceof PasswordField passwordFieldComponent) {
                passwordFieldComponent.setLabel(translationSupplier.get());
            }
            if (component instanceof Checkbox checkBoxComponent) {
                checkBoxComponent.setLabel(translationSupplier.get());
            }
            if (component instanceof DatePicker datePickerComponent) {
                datePickerComponent.setLabel(translationSupplier.get());
            }
        });
    }
}
