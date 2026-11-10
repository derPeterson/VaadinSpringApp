package de.derpeterson.app.ui.components;

import static de.derpeterson.app.ui.components.CosmosClasses.ACCOUNT_NAVIGATION_OPEN;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;

/** Gemeinsame Inhaltsanordnung mit optionaler Seitennavigation für alle Seitenbereiche. */
public class PortalPageComponent extends Div {
  private static final String ARIA_EXPANDED = "aria-expanded";

  private static final String FOCUS_NAVIGATION_SCRIPT =
      """
      const item = this.querySelector('vaadin-side-nav-item');
      const target = item?.shadowRoot?.querySelector('[part=link]')
        ?? this.querySelector('vaadin-button');
      target?.focus();
      """;

  private Component navigationComponent;
  private Component portalContentComponent;
  private Button navigationToggleButton;

  public PortalPageComponent() {
    addClassName("account-settings");
    addDetachListener(event -> closeNavigation());
  }

  public PortalPageComponent(
      Component pageHeadingComponent, Component sidebarComponent, Component pageContentComponent) {
    this();
    setPortalContent(pageHeadingComponent, sidebarComponent, pageContentComponent);
  }

  protected final void setPortalContent(
      Component pageHeadingComponent, Component sidebarComponent, Component pageContentComponent) {
    navigationComponent = sidebarComponent;
    portalContentComponent = pageContentComponent;

    if (sidebarComponent == null) {
      add(pageHeadingComponent, pageContentComponent);
      return;
    }

    pageContentComponent.getElement().getClassList().add("account-panels");
    Div portalColumnsDiv = new Div(sidebarComponent, pageContentComponent);
    portalColumnsDiv.addClassName("account-columns");

    Div navigationShadeDiv = new Div();
    navigationShadeDiv.addClassName("account-navigation-shade");
    navigationShadeDiv.addClickListener(event -> closeNavigation());

    add(pageHeadingComponent, portalColumnsDiv, navigationShadeDiv);
  }

  public final void configureNavigationToggle(Button portalNavigationButton) {
    navigationToggleButton = portalNavigationButton;
    // Escape nur innerhalb der Navigation behandeln. Ein globales Shortcut würde
    // die native Escape-Behandlung von Popovers und Dialogen abfangen.
    Shortcuts.addShortcutListener(this, this::closeNavigation, Key.ESCAPE)
        .listenOn(navigationComponent, portalNavigationButton);
    portalNavigationButton
        .getElement()
        .setAttribute("aria-controls", navigationComponent.getId().orElseThrow());
    portalNavigationButton.getElement().setAttribute(ARIA_EXPANDED, "false");

    portalNavigationButton.addClickListener(event -> {
      boolean navigationOpened = !hasClassName(ACCOUNT_NAVIGATION_OPEN);
      setClassName(ACCOUNT_NAVIGATION_OPEN, navigationOpened);
      portalContentComponent.getElement().setAttribute("inert", navigationOpened);
      portalNavigationButton
          .getElement()
          .setAttribute(ARIA_EXPANDED, Boolean.toString(navigationOpened));

      if (navigationOpened) {
        navigationComponent.getElement().executeJs(FOCUS_NAVIGATION_SCRIPT);
      }
    });
  }

  public final void closeNavigation() {
    if (!hasClassName(ACCOUNT_NAVIGATION_OPEN)) {
      return;
    }

    removeClassName(ACCOUNT_NAVIGATION_OPEN);
    portalContentComponent.getElement().setAttribute("inert", false);

    if (navigationToggleButton != null) {
      navigationToggleButton.getElement().setAttribute(ARIA_EXPANDED, "false");
      navigationToggleButton.focus();
    }
  }
}
