package de.derpeterson.app.helper.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.dom.Element;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ComponentPrefixHelper {

  private static final String PREFIX_SLOT_NAME = "prefix";

  private static Stream<Element> getElementsInSlot(HasElement target, String slot) {
    return target
        .getElement()
        .getChildren()
        .filter(child -> slot.equals(child.getAttribute("slot")));
  }

  public static void setPrefixComponent(Component target, Component component) {
    clearSlot(target, PREFIX_SLOT_NAME);

    if (component != null) {
      component.getElement().setAttribute("slot", PREFIX_SLOT_NAME);
      target.getElement().appendChild(component.getElement());
    }
  }

  public static void clearSlot(Component target, String slot) {
    getElementsInSlot(target, slot).toList().forEach(target.getElement()::removeChild);
  }
}
