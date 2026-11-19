package de.derpeterson.app;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.masterdetaillayout.MasterDetailLayout;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

/** Testbaum mit Card-Slots und dem Detailbereich des MasterDetailLayout. */
public final class ComponentTestTree {
  private ComponentTestTree() {}

  public static Stream<Component> streamComponentTree(Component rootComponent) {
    return Stream.concat(
        Stream.of(rootComponent),
        streamChildComponents(rootComponent).flatMap(ComponentTestTree::streamComponentTree));
  }

  public static Stream<Component> streamChildComponents(Component parentComponent) {
    if (parentComponent instanceof MasterDetailLayout masterDetailLayout) {
      return Stream.concat(
              masterDetailLayout.getChildren(), Stream.ofNullable(masterDetailLayout.getDetail()))
          .distinct();
    }

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
