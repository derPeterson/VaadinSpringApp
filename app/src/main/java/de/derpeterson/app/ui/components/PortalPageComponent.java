package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;

/** Gemeinsame Inhaltsanordnung mit optionaler Seitennavigation für alle Seitenbereiche. */
public class PortalPageComponent extends Div {
  private Component navigation;
  private Component pageContent;
  private Button navigationToggle;

  public PortalPageComponent() {
    addClassName("account-settings");
    Shortcuts.addShortcutListener(this, this::closeNavigation, Key.ESCAPE);
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
    toggle.getElement().setAttribute("aria-controls", navigation.getId().orElseThrow());
    toggle.getElement().setAttribute("aria-expanded", "false");
    toggle.addClickListener(
        event -> {
          boolean open = !hasClassName("account-navigation-open");
          setClassName("account-navigation-open", open);
          pageContent.getElement().setAttribute("inert", open);
          toggle.getElement().setAttribute("aria-expanded", Boolean.toString(open));
          if (open) {
            navigation
                .getElement()
                .executeJs("this.querySelector('vaadin-side-nav-item, vaadin-button')?.focus()");
          }
        });
  }

  public final void closeNavigation() {
    if (!hasClassName("account-navigation-open")) {
      return;
    }
    removeClassName("account-navigation-open");
    pageContent.getElement().setAttribute("inert", false);
    if (navigationToggle != null) {
      navigationToggle.getElement().setAttribute("aria-expanded", "false");
      navigationToggle.focus();
    }
  }
}
