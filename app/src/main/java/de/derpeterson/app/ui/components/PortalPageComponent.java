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

  private Component navigation;
  private Component pageContent;
  private Button navigationToggle;

  public PortalPageComponent() {
    addClassName("account-settings");
    addDetachListener(event -> closeNavigation());
  }

  public PortalPageComponent(Component heading, Component sidebar, Component content) {
    this();
    setPortalContent(heading, sidebar, content);
  }

  protected final void setPortalContent(Component heading, Component sidebar, Component content) {
    navigation = sidebar;
    pageContent = content;
    if (sidebar == null) {
      add(heading, content);
      return;
    }
    content.getElement().getClassList().add("account-panels");
    Div columns = new Div(sidebar, content);
    columns.addClassName("account-columns");
    Div shade = new Div();
    shade.addClassName("account-navigation-shade");
    shade.addClickListener(event -> closeNavigation());
    add(heading, columns, shade);
  }

  public final void configureNavigationToggle(Button toggle) {
    navigationToggle = toggle;
    // Escape nur innerhalb der Navigation behandeln. Ein globales Shortcut würde
    // die native Escape-Behandlung von Popovers und Dialogen abfangen.
    Shortcuts.addShortcutListener(this, this::closeNavigation, Key.ESCAPE)
        .listenOn(navigation, toggle);
    toggle.getElement().setAttribute("aria-controls", navigation.getId().orElseThrow());
    toggle.getElement().setAttribute(ARIA_EXPANDED, "false");
    toggle.addClickListener(
        event -> {
          boolean open = !hasClassName(ACCOUNT_NAVIGATION_OPEN);
          setClassName(ACCOUNT_NAVIGATION_OPEN, open);
          pageContent.getElement().setAttribute("inert", open);
          toggle.getElement().setAttribute(ARIA_EXPANDED, Boolean.toString(open));
          if (open) {
            navigation.getElement().executeJs(FOCUS_NAVIGATION_SCRIPT);
          }
        });
  }

  public final void closeNavigation() {
    if (!hasClassName(ACCOUNT_NAVIGATION_OPEN)) {
      return;
    }
    removeClassName(ACCOUNT_NAVIGATION_OPEN);
    pageContent.getElement().setAttribute("inert", false);
    if (navigationToggle != null) {
      navigationToggle.getElement().setAttribute(ARIA_EXPANDED, "false");
      navigationToggle.focus();
    }
  }
}
