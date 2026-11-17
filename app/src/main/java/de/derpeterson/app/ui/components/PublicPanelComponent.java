package de.derpeterson.app.ui.components;

import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_CARD;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_DESCRIPTION;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_HEADER;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_HEADING;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_ICON;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.derpeterson.app.events.LanguageChangeEvent;
import java.util.function.Supplier;

/** Öffentliches Standardpanel mit nativem Card-Kopf, Inhalt und Aktionsfooter im Cosmos-Stil. */
public class PublicPanelComponent extends Card {
  public PublicPanelComponent(
      Component panelIconComponent,
      Component panelTitleComponent,
      Supplier<String> panelSubtitleSupplier,
      Component[] panelBodyComponents,
      Component... panelFooterActionComponents) {
    addClassNames("cosmos-card", PANEL_CARD, "public-panel", "public-card-panel");
    setWidthFull();
    setAriaLabelledBy(panelTitleComponent);

    Span panelSubtitleSpan = new Span(panelSubtitleSupplier.get());
    panelSubtitleSpan.addClassName(PANEL_DESCRIPTION);
    setHeader(buildPanelHeaderLayout(panelIconComponent, panelTitleComponent, panelSubtitleSpan));
    add(buildPanelBodyLayout(panelBodyComponents));
    addToFooter(buildPanelFooterLayout(panelFooterActionComponents));

    ComponentUtil.addListener(
        UI.getCurrent(),
        LanguageChangeEvent.class,
        languageChangeEvent -> panelSubtitleSpan.setText(panelSubtitleSupplier.get()));
  }

  private HorizontalLayout buildPanelHeaderLayout(
      Component panelIconComponent, Component panelTitleComponent, Span panelSubtitleSpan) {
    Div panelIconContainerDiv = new Div(panelIconComponent);
    panelIconContainerDiv.addClassName(PANEL_ICON);

    VerticalLayout panelHeadingLayout = new VerticalLayout(panelTitleComponent, panelSubtitleSpan);
    panelHeadingLayout.setPadding(false);
    panelHeadingLayout.setSpacing(false);
    panelHeadingLayout.setAlignItems(Alignment.STRETCH);
    panelHeadingLayout.addClassName(PANEL_HEADING);

    HorizontalLayout panelHeaderLayout =
        new HorizontalLayout(panelIconContainerDiv, panelHeadingLayout);
    panelHeaderLayout.setPadding(false);
    panelHeaderLayout.setSpacing(false);
    panelHeaderLayout.setAlignItems(Alignment.CENTER);
    panelHeaderLayout.addClassName(PANEL_HEADER);
    return panelHeaderLayout;
  }

  private VerticalLayout buildPanelBodyLayout(Component[] panelBodyComponents) {
    VerticalLayout panelBodyLayout = new VerticalLayout(panelBodyComponents);
    panelBodyLayout.setWidthFull();
    panelBodyLayout.setPadding(false);
    panelBodyLayout.setSpacing(false);
    panelBodyLayout.setAlignItems(Alignment.STRETCH);
    panelBodyLayout.addClassName("public-panel-body");
    return panelBodyLayout;
  }

  private HorizontalLayout buildPanelFooterLayout(Component[] panelFooterActionComponents) {
    HorizontalLayout panelFooterLayout = new HorizontalLayout(panelFooterActionComponents);
    panelFooterLayout.setWidthFull();
    panelFooterLayout.setPadding(false);
    panelFooterLayout.setSpacing(false);
    panelFooterLayout.addClassName("public-panel-footer");
    return panelFooterLayout;
  }
}
