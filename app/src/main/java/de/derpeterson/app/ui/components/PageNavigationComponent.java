package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.function.SerializableRunnable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Gemeinsame native Seitennavigation; Einträge können weitere Unterpunkte enthalten. */
public class PageNavigationComponent extends SideNav {
  private static final String NAVIGATION_SCRIPT =
      """
      this.onNavigate = ({originalEvent}) => {
        const item = originalEvent.composedPath().find(node =>
          node.localName === 'vaadin-side-nav-item');
        if (item?.id) this.dispatchEvent(new CustomEvent(
          'cosmos-page-navigate', {detail: {id: item.id}}));
      };
      """;

  private final Map<String, SerializableRunnable> navigationActionsByItemId = new LinkedHashMap<>();

  public PageNavigationComponent() {
    addClassName("cosmos-side-navigation");
    setWidthFull();
    setAutoExpand(false);
    getElement()
        .addEventListener(
            "cosmos-page-navigate",
            event -> {
              String navigationItemId = event.getEventData().path("event.detail.id").asString();
              SerializableRunnable navigationAction =
                  navigationActionsByItemId.get(navigationItemId);
              if (navigationAction != null) {
                navigationAction.run();
              }
            })
        .addEventData("event.detail.id");
    addAttachListener(event -> getElement().executeJs(NAVIGATION_SCRIPT));
  }

  public void registerAction(SideNavItem navigationItem, SerializableRunnable navigationAction) {
    String navigationItemId = navigationItem.getId().orElseThrow();
    if (navigationActionsByItemId.putIfAbsent(
            navigationItemId, Objects.requireNonNull(navigationAction))
        != null) {
      throw new IllegalArgumentException("Navigation item ID must be unique: " + navigationItemId);
    }
  }

  /** Verwendet denselben Seitenwechsel wie Maus- und Tastaturaktivierung. */
  public void activate(SideNavItem navigationItem) {
    SerializableRunnable navigationAction =
        navigationActionsByItemId.get(navigationItem.getId().orElseThrow());
    if (navigationAction != null) {
      navigationAction.run();
    }
  }

  public static void setItemActive(SideNavItem navigationItem, boolean active) {
    if (active) {
      navigationItem.getElement().getClassList().add("cosmos-nav-item--active");
      navigationItem.getElement().setAttribute("aria-current", "page");
    } else {
      navigationItem.getElement().getClassList().remove("cosmos-nav-item--active");
      navigationItem.getElement().removeAttribute("aria-current");
    }
  }
}
