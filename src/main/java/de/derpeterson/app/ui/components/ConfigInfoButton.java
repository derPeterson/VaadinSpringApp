package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.function.Supplier;

/** Gemeinsamer Info-Button: Klick-Tooltip, Außenklick und aktuelle Übersetzung. */
public class ConfigInfoButton extends Button {
  private static final String OUTSIDE_CLICK_SCRIPT =
      """
                const button = this;
                button.cosmosTooltipCleanup?.();
                const controller = new AbortController();
                const tooltip = button.querySelector('vaadin-tooltip[slot="tooltip"]');
                if (!tooltip) return;

                const observer = new MutationObserver(() => {
                    if (!button.isConnected) cleanup();
                });
                const cleanup = () => {
                    controller.abort();
                    observer.disconnect();
                    delete button.cosmosTooltipCleanup;
                };
                button.cosmosTooltipCleanup = cleanup;
                observer.observe(document.body, {childList: true, subtree: true});

                document.addEventListener('pointerdown', event => {
                    if (!tooltip.opened) return;
                    const path = event.composedPath();
                    if (path.includes(button) || path.includes(tooltip)) return;
                    tooltip.opened = false;
                    button.setAttribute('aria-expanded', 'false');
                    button.dispatchEvent(new CustomEvent('cosmos-tooltip-dismiss'));
                }, {capture: true, signal: controller.signal});
                """;

  private static final String ARIA_EXPANDED = "aria-expanded";
  private static final String ARIA_COLLAPSED = "false";

  private final Tooltip tooltip;
  private final transient Supplier<String> textSupplier;

  public ConfigInfoButton(Supplier<String> textSupplier) {
    this.textSupplier = textSupplier;
    Icon icon = VaadinIcon.INFO_CIRCLE.create();
    icon.addClassName(LumoUtility.TextColor.PRIMARY);
    setIcon(icon);
    addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    addClassName("cosmos-config-info");
    getElement().setAttribute(ARIA_EXPANDED, ARIA_COLLAPSED);
    tooltip =
        getTooltip()
            .withText(textSupplier.get())
            .withPosition(Tooltip.TooltipPosition.BOTTOM)
            .withManual(true);
    addClickListener(
        event -> {
          boolean opened = !tooltip.isOpened();
          tooltip.setOpened(opened);
          getElement().setAttribute(ARIA_EXPANDED, Boolean.toString(opened));
        });
    addDetachListener(event -> closeTooltip());
    installOutsideClick();
  }

  public void refreshText() {
    tooltip.setText(textSupplier.get());
  }

  public void closeTooltip() {
    tooltip.setOpened(false);
    getElement().setAttribute(ARIA_EXPANDED, ARIA_COLLAPSED);
  }

  private void installOutsideClick() {
    Button infoButton = this;
    // Der manuelle Tooltip schließt nicht selbst bei Außenklicks. Das Ereignis
    // meldet die clientseitige Schließung zurück, damit der nächste Klick wieder öffnet.
    infoButton
        .getElement()
        .addEventListener(
            "cosmos-tooltip-dismiss",
            event -> {
              tooltip.setOpened(false);
              infoButton.getElement().setAttribute(ARIA_EXPANDED, ARIA_COLLAPSED);
            });
    infoButton.addAttachListener(event -> infoButton.getElement().executeJs(OUTSIDE_CLICK_SCRIPT));
  }
}
