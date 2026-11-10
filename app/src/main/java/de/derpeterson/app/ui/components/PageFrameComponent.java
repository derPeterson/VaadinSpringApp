package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;

/** Gemeinsamer Seitenrahmen: fester Servicekopf und gemeinsam scrollender Inhaltsbereich. */
public class PageFrameComponent extends Div {
  private final Div frameHeaderDiv = new Div();
  private final Div frameMainDiv = new Div();

  public PageFrameComponent() {
    addClassName("cosmos-page-frame");
    frameHeaderDiv.addClassName("cosmos-frame-header");
    frameMainDiv.addClassName("cosmos-app-main");
    frameMainDiv.getElement().setAttribute("role", "main");
    add(new CosmosBackgroundComponent(), frameHeaderDiv, frameMainDiv);
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
    this.frameHeaderDiv.removeAll();
    this.frameHeaderDiv.add(pageHeaderComponent);

    frameMainDiv.removeAll();
    frameMainDiv.add(pageContentComponent, pageFooterComponent);
  }
}
