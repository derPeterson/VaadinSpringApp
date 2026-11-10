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

  private static Stream<Element> getElementsInSlot(HasElement targetComponent, String slotName) {
    return targetComponent
        .getElement()
        .getChildren()
        .filter(childElement -> slotName.equals(childElement.getAttribute("slot")));
  }

  public static void setPrefixComponent(Component targetComponent, Component prefixComponent) {
    clearSlot(targetComponent, PREFIX_SLOT_NAME);

    if (prefixComponent != null) {
      prefixComponent.getElement().setAttribute("slot", PREFIX_SLOT_NAME);
      targetComponent.getElement().appendChild(prefixComponent.getElement());
    }
  }

  public static void clearSlot(Component targetComponent, String slotName) {
    getElementsInSlot(targetComponent, slotName)
        .toList()
        .forEach(targetComponent.getElement()::removeChild);
  }
}
