package de.derpeterson.app;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.card.Card;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

/** Testbaum einschließlich der Card-Slots; Card.getChildren() liefert nur den normalen Inhalt. */
public final class ComponentTestTree {
  private ComponentTestTree() {}

  public static Stream<Component> streamComponentTree(Component rootComponent) {
    return Stream.concat(
        Stream.of(rootComponent),
        streamChildComponents(rootComponent).flatMap(ComponentTestTree::streamComponentTree));
  }

  public static Stream<Component> streamChildComponents(Component parentComponent) {
    if (!(parentComponent instanceof Card cardComponent)) {
      return parentComponent.getChildren();
    }

    Stream<Component> headerComponents =
        Stream.of(
                cardComponent.getHeader(),
                cardComponent.getTitle(),
                cardComponent.getSubtitle(),
                cardComponent.getHeaderPrefix(),
                cardComponent.getHeaderSuffix())
            .filter(Objects::nonNull);
    Stream<Component> footerComponents = Arrays.stream(cardComponent.getFooterComponents());
    return Stream.of(cardComponent.getChildren(), headerComponents, footerComponents)
        .flatMap(childComponents -> childComponents)
        .distinct();
  }
}
