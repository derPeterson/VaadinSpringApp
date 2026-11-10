package de.derpeterson.app.ui.components;

import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_CARD;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_DESCRIPTION;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_HEADER;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_HEADING;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_ICON;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_TITLE;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;

/** Kontopanel verwendet unverändert die vorhandenen Cosmos-Panelbausteine. */
public class AccountPanelComponent extends CosmosCardComponent {
  private final H2 panelTitle = new H2();
  private final Span panelDescription = new Span();
  private final Span panelMetaText = new Span();
  private final Div panelBody = new Div();
  private final Div panelIconContainer = new Div();

  public AccountPanelComponent(String panelId, AppIcon iconType) {
    setId(panelId);
    addClassNames(PANEL_CARD, "account-panel");
    panelIconContainer.add(iconType.create());
    panelIconContainer.addClassName(PANEL_ICON);
    panelTitle.addClassName(PANEL_TITLE);
    panelDescription.addClassName(PANEL_DESCRIPTION);
    panelMetaText.addClassName("cosmos-panel-meta");
    Div panelHeadingLayout = new Div(panelTitle, panelDescription);
    panelHeadingLayout.addClassName(PANEL_HEADING);
    Div panelHeaderLayout = new Div(panelIconContainer, panelHeadingLayout, panelMetaText);
    panelHeaderLayout.addClassName(PANEL_HEADER);
    panelBody.addClassName("account-panel-body");
    add(panelHeaderLayout, panelBody);
  }

  public void setIcon(Component panelIconComponent) {
    panelIconContainer.removeAll();
    panelIconContainer.add(panelIconComponent);
  }

  public Div getBody() {
    return panelBody;
  }

  public void setHeading(String panelTitle, String panelDescription, String panelMetaText) {
    this.panelTitle.setText(panelTitle);
    this.panelDescription.setText(panelDescription);
    this.panelMetaText.setText(panelMetaText);
  }

  public void addFooter(Component footerHintComponent, Component footerActionComponent) {
    Div footerHintLayout = new Div(AppIcon.INFO_CIRCLE.create(), footerHintComponent);
    footerHintLayout.addClassName("cosmos-panel-hint");
    Div footerActionsLayout = new Div(footerActionComponent);
    footerActionsLayout.addClassName("cosmos-panel-actions");
    Div panelFooterLayout = new Div(footerHintLayout, footerActionsLayout);
    panelFooterLayout.addClassName("cosmos-panel-footer");
    add(panelFooterLayout);
  }

  public void addActionFooter(Component footerActionComponent) {
    Div footerActionsLayout = new Div(footerActionComponent);
    footerActionsLayout.addClassName("cosmos-panel-actions");
    Div panelFooterLayout = new Div(footerActionsLayout);
    panelFooterLayout.addClassName("cosmos-panel-footer");
    add(panelFooterLayout);
  }
}
