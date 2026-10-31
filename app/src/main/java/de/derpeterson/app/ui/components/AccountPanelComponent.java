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
  private final H2 title = new H2();
  private final Span description = new Span();
  private final Span meta = new Span();
  private final Div body = new Div();
  private final Div icon = new Div();

  public AccountPanelComponent(String id, AppIcon iconType) {
    setId(id);
    addClassNames(PANEL_CARD, "account-panel");
    icon.add(iconType.create());
    icon.addClassName(PANEL_ICON);
    title.addClassName(PANEL_TITLE);
    description.addClassName(PANEL_DESCRIPTION);
    meta.addClassName("cosmos-panel-meta");
    Div heading = new Div(title, description);
    heading.addClassName(PANEL_HEADING);
    Div header = new Div(icon, heading, meta);
    header.addClassName(PANEL_HEADER);
    body.addClassName("account-panel-body");
    add(header, body);
  }

  public void setIcon(Component symbol) {
    icon.removeAll();
    icon.add(symbol);
  }

  public Div getBody() {
    return body;
  }

  public void setHeading(String title, String description, String meta) {
    this.title.setText(title);
    this.description.setText(description);
    this.meta.setText(meta);
  }

  public void addFooter(Component hint, Component action) {
    Div info = new Div(AppIcon.INFO_CIRCLE.create(), hint);
    info.addClassName("cosmos-panel-hint");
    Div actions = new Div(action);
    actions.addClassName("cosmos-panel-actions");
    Div footer = new Div(info, actions);
    footer.addClassName("cosmos-panel-footer");
    add(footer);
  }

  public void addActionFooter(Component action) {
    Div actions = new Div(action);
    actions.addClassName("cosmos-panel-actions");
    Div footer = new Div(actions);
    footer.addClassName("cosmos-panel-footer");
    add(footer);
  }
}
