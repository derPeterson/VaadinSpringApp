package de.derpeterson.app.ui.components;

import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_CARD;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_DESCRIPTION;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_HEADER;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_HEADING;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_ICON;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import de.derpeterson.app.events.LanguageChangeEvent;
import java.util.function.Supplier;

/** Dieselben Panelzonen wie im Konto, mit den echten Eingaben und Aktionen der Besucheransicht. */
public class PublicPanelComponent extends CosmosCardComponent {
  public PublicPanelComponent(
      Component panelIconComponent,
      Component panelTitleComponent,
      Supplier<String> panelSubtitleSupplier,
      Component[] panelBodyComponents,
      Component... panelFooterActionComponents) {
    addClassNames(PANEL_CARD, "public-panel");

    Div panelIconContainer = new Div(panelIconComponent);
    panelIconContainer.addClassName(PANEL_ICON);
    Span panelSubtitleSpan = new Span(panelSubtitleSupplier.get());
    panelSubtitleSpan.addClassName(PANEL_DESCRIPTION);
    Div panelHeadingContainer = new Div(panelTitleComponent, panelSubtitleSpan);
    panelHeadingContainer.addClassName(PANEL_HEADING);
    Div panelHeaderContainer = new Div(panelIconContainer, panelHeadingContainer);
    panelHeaderContainer.addClassName(PANEL_HEADER);

    Div panelBodyContainer = new Div(panelBodyComponents);
    panelBodyContainer.addClassName("public-panel-body");

    Div panelFooterContainer = new Div(panelFooterActionComponents);
    panelFooterContainer.addClassName("public-panel-footer");

    add(panelHeaderContainer, panelBodyContainer, panelFooterContainer);

    ComponentUtil.addListener(
        UI.getCurrent(),
        LanguageChangeEvent.class,
        attachEvent -> panelSubtitleSpan.setText(panelSubtitleSupplier.get()));
  }
}
