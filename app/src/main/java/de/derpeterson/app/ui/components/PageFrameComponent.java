package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;

/** Gemeinsamer Seitenrahmen: fester Servicekopf und gemeinsam scrollender Inhaltsbereich. */
public class PageFrameComponent extends Div {
  private final Div header = new Div();
  private final Div main = new Div();

  public PageFrameComponent() {
    addClassName("cosmos-page-frame");
    header.addClassName("cosmos-frame-header");
    main.addClassName("cosmos-app-main");
    main.getElement().setAttribute("role", "main");
    add(new CosmosBackgroundComponent(), header, main);
  }

  public PageFrameComponent(Component header, Component content, Component footer) {
    this();
    setFrameContent(header, content, footer);
  }

  protected final void setFrameContent(Component header, Component content, Component footer) {
    this.header.removeAll();
    this.header.add(header);
    main.removeAll();
    main.add(content, footer);
  }
}
