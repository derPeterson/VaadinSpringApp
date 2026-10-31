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

  private final Map<String, SerializableRunnable> actions = new LinkedHashMap<>();

  public PageNavigationComponent() {
    addClassName("cosmos-side-navigation");
    setWidthFull();
    setAutoExpand(false);
    getElement()
        .addEventListener(
            "cosmos-page-navigate",
            event -> {
              String id = event.getEventData().path("event.detail.id").asString();
              SerializableRunnable action = actions.get(id);
              if (action != null) {
                action.run();
              }
            })
        .addEventData("event.detail.id");
    addAttachListener(event -> getElement().executeJs(NAVIGATION_SCRIPT));
  }

  public void registerAction(SideNavItem item, SerializableRunnable action) {
    String id = item.getId().orElseThrow();
    if (actions.putIfAbsent(id, Objects.requireNonNull(action)) != null) {
      throw new IllegalArgumentException("Navigation item ID must be unique: " + id);
    }
  }

  /** Verwendet denselben Seitenwechsel wie Maus- und Tastaturaktivierung. */
  public void activate(SideNavItem item) {
    SerializableRunnable action = actions.get(item.getId().orElseThrow());
    if (action != null) {
      action.run();
    }
  }

  public static void setItemActive(SideNavItem item, boolean active) {
    if (active) {
      item.getElement().getClassList().add("cosmos-nav-item--active");
      item.getElement().setAttribute("aria-current", "page");
    } else {
      item.getElement().getClassList().remove("cosmos-nav-item--active");
      item.getElement().removeAttribute("aria-current");
    }
  }
}
