package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.function.SerializableRunnable;

/** Öffnet die seitliche Hilfe für ein Formularfeld. */
public class ConfigInfoButton extends Button {
  public ConfigInfoButton(SerializableRunnable openHelpAction) {
    SvgIcon helpSvgIcon = AppIcon.INFO_CIRCLE.create();
    helpSvgIcon.addClassName(CosmosCssClasses.TEXT_PRIMARY);
    setIcon(helpSvgIcon);
    addThemeVariants(ButtonVariant.TERTIARY);
    addClassNames(CosmosCssClasses.INLINE_BUTTON, "cosmos-config-info");
    getElement().setAttribute("aria-haspopup", "dialog");

    addClickListener(event -> openHelpAction.run());
  }
}
