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
import com.vaadin.flow.component.tabs.Tabs;

/** Kontopanel aus den gemeinsamen Cosmos-Bausteinen. */
public class AccountPanelComponent extends CosmosCardComponent {
  private final H2 panelTitleHeading = new H2();
  private final Span panelDescriptionSpan = new Span();
  private final Span panelMetadataSpan = new Span();
  private final Div panelHeaderDiv = new Div();
  private final Div panelBodyDiv = new Div();
  private final Div panelIconDiv = new Div();
  private Tabs panelHeaderTabs;

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
    panelHeaderDiv.add(panelIconDiv, panelHeadingDiv, panelMetadataSpan);
    panelHeaderDiv.addClassName(PANEL_HEADER);

    panelBodyDiv.addClassName("account-panel-body");
    setHeader(panelHeaderDiv);
    setAriaLabelledBy(panelTitleHeading);
    add(panelBodyDiv);
  }

  public void setIcon(Component panelIconComponent) {
    panelIconDiv.removeAll();

    panelIconDiv.add(panelIconComponent);
  }

  public Div getBody() {
    return panelBodyDiv;
  }

  /** Ergänzt die Titelzeile um Reiter. {@code null} entfernt die Reiter beim Schrittwechsel. */
  public void setHeaderTabs(Tabs headerTabs) {
    if (panelHeaderTabs != null) {
      panelHeaderDiv.remove(panelHeaderTabs);
    }

    panelHeaderTabs = headerTabs;
    if (headerTabs != null) {
      panelHeaderDiv.add(headerTabs);
    }
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

    addToFooter(panelFooterDiv);
  }

  public void addActionFooter(Component footerActionComponent) {
    Div footerActionsDiv = new Div(footerActionComponent);
    footerActionsDiv.addClassName("cosmos-panel-actions");
    Div panelFooterDiv = new Div(footerActionsDiv);
    panelFooterDiv.addClassName("cosmos-panel-footer");

    addToFooter(panelFooterDiv);
  }
}
