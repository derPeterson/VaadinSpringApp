package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;

/** Kontopanel verwendet unverändert die vorhandenen Cosmos-Panelbausteine. */
public class AccountPanelComponent extends CosmosCardComponent {
  private final H2 title = new H2();
  private final Span description = new Span();
  private final Span meta = new Span();
  private final Div body = new Div();

  public AccountPanelComponent(String id, VaadinIcon iconType) {
    setId(id);
    addClassNames("cosmos-panel-card", "account-panel");
    var icon = new Div(iconType.create());
    icon.addClassName("cosmos-panel-icon");
    title.addClassName("cosmos-panel-title");
    description.addClassName("cosmos-panel-description");
    meta.addClassName("cosmos-panel-meta");
    var heading = new Div(title, description);
    heading.addClassName("cosmos-panel-heading");
    var header = new Div(icon, heading, meta);
    header.addClassName("cosmos-panel-header");
    body.addClassName("account-panel-body");
    add(header, body);
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
    var info = new Div(VaadinIcon.INFO_CIRCLE_O.create(), hint);
    info.addClassName("cosmos-panel-hint");
    var actions = new Div(action);
    actions.addClassName("cosmos-panel-actions");
    var footer = new Div(info, actions);
    footer.addClassName("cosmos-panel-footer");
    add(footer);
  }
}
