package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.details.Details;
import de.derpeterson.app.i18n.MessageProperties;

/** Gemeinsamer rahmenloser Details-Schalter mit synchronem Pfeil und Zugänglichkeitszustand. */
public final class DetailsToggleButton extends Button {
  public DetailsToggleButton(MessageProperties texts, Details details, String topic) {
    super(texts.getBaseDetailsButton());
    setIconAfterText(true);
    addClassName("two-factor-details-toggle");
    addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    getElement().setAttribute("aria-controls", details.getId().orElseThrow());
    setAriaLabel(topic + ": " + texts.getBaseDetailsButton());
    updateState(details.isOpened());
    addClickListener(event -> details.setOpened(!details.isOpened()));
    details.addOpenedChangeListener(event -> updateState(event.isOpened()));
  }

  private void updateState(boolean opened) {
    setIcon(opened ? AppIcon.ANGLE_DOWN.create() : AppIcon.ANGLE_RIGHT.create());
    getElement().setAttribute("aria-expanded", Boolean.toString(opened));
  }
}
