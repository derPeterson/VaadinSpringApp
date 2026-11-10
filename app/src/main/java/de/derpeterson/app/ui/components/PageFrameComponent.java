package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;

/** Gemeinsamer Seitenrahmen: fester Servicekopf und gemeinsam scrollender Inhaltsbereich. */
public class PageFrameComponent extends Div {
  private final Div frameHeaderContainer = new Div();
  private final Div frameMainContainer = new Div();

  public PageFrameComponent() {
    addClassName("cosmos-page-frame");
    frameHeaderContainer.addClassName("cosmos-frame-header");
    frameMainContainer.addClassName("cosmos-app-main");
    frameMainContainer.getElement().setAttribute("role", "main");
    add(new CosmosBackgroundComponent(), frameHeaderContainer, frameMainContainer);
  }

  public PageFrameComponent(
      Component pageHeaderComponent,
      Component pageContentComponent,
      Component pageFooterComponent) {
    this();
    setFrameContent(pageHeaderComponent, pageContentComponent, pageFooterComponent);
  }

  protected final void setFrameContent(
      Component pageHeaderComponent,
      Component pageContentComponent,
      Component pageFooterComponent) {
    this.frameHeaderContainer.removeAll();
    this.frameHeaderContainer.add(pageHeaderComponent);
    frameMainContainer.removeAll();
    frameMainContainer.add(pageContentComponent, pageFooterComponent);
  }
}
