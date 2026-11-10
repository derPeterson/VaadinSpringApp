package de.derpeterson.app.ui.components;

import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_CARD;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_DESCRIPTION;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_HEADER;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_HEADING;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_ICON;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_TITLE;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;

/** Kontopanel verwendet unverändert die vorhandenen Cosmos-Panelbausteine. */
public class AccountPanelComponent extends CosmosCardComponent {
  private final H2 panelTitleHeading = new H2();
  private final Span panelDescriptionSpan = new Span();
  private final Span panelMetadataSpan = new Span();
  private final Div panelBodyDiv = new Div();
  private final Div panelIconDiv = new Div();

  public AccountPanelComponent(String panelId, AppIcon panelAppIcon) {
    setId(panelId);
    addClassNames(PANEL_CARD, "account-panel");

    panelIconDiv.add(panelAppIcon.create());
    panelIconDiv.addClassName(PANEL_ICON);
    panelTitleHeading.addClassName(PANEL_TITLE);
    panelDescriptionSpan.addClassName(PANEL_DESCRIPTION);
    panelMetadataSpan.addClassName("cosmos-panel-meta");

    Div panelHeadingDiv = new Div(panelTitleHeading, panelDescriptionSpan);
    panelHeadingDiv.addClassName(PANEL_HEADING);
    Div panelHeaderDiv = new Div(panelIconDiv, panelHeadingDiv, panelMetadataSpan);
    panelHeaderDiv.addClassName(PANEL_HEADER);

    panelBodyDiv.addClassName("account-panel-body");
    add(panelHeaderDiv, panelBodyDiv);
  }

  public void setIcon(Component panelIconComponent) {
    panelIconDiv.removeAll();

    panelIconDiv.add(panelIconComponent);
  }

  public Div getBody() {
    return panelBodyDiv;
  }

  public void setHeading(
      String panelTitleText, String panelDescriptionText, String panelMetadataText) {
    panelTitleHeading.setText(panelTitleText);
    panelDescriptionSpan.setText(panelDescriptionText);
    panelMetadataSpan.setText(panelMetadataText);
  }

  public void addFooter(Component footerHintComponent, Component footerActionComponent) {
    Div footerHintDiv = new Div(AppIcon.INFO_CIRCLE.create(), footerHintComponent);
    footerHintDiv.addClassName("cosmos-panel-hint");

    Div footerActionsDiv = new Div(footerActionComponent);
    footerActionsDiv.addClassName("cosmos-panel-actions");
    Div panelFooterDiv = new Div(footerHintDiv, footerActionsDiv);
    panelFooterDiv.addClassName("cosmos-panel-footer");

    add(panelFooterDiv);
  }

  public void addActionFooter(Component footerActionComponent) {
    Div footerActionsDiv = new Div(footerActionComponent);
    footerActionsDiv.addClassName("cosmos-panel-actions");
    Div panelFooterDiv = new Div(footerActionsDiv);
    panelFooterDiv.addClassName("cosmos-panel-footer");

    add(panelFooterDiv);
  }
}
