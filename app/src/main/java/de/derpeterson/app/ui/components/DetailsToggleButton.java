package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.details.Details;
import de.derpeterson.app.i18n.MessageProperties;

/** Gemeinsamer rahmenloser Details-Schalter mit synchronem Pfeil und Zugänglichkeitszustand. */
public final class DetailsToggleButton extends Button {
  public DetailsToggleButton(MessageProperties messageProperties, Details details, String topic) {
    super(messageProperties.getBaseDetailsButton());
    setIconAfterText(true);
    addClassName("two-factor-details-toggle");
    addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    getElement().setAttribute("aria-controls", details.getId().orElseThrow());
    setAriaLabel(topic + ": " + messageProperties.getBaseDetailsButton());
    refreshDetailsToggleState(details.isOpened());
    addClickListener(event -> details.setOpened(!details.isOpened()));

    details.addOpenedChangeListener(event -> refreshDetailsToggleState(event.isOpened()));
  }

  private void refreshDetailsToggleState(boolean detailsOpened) {
    setIcon(detailsOpened ? AppIcon.ANGLE_DOWN.create() : AppIcon.ANGLE_RIGHT.create());
    getElement().setAttribute("aria-expanded", Boolean.toString(detailsOpened));
  }
}
