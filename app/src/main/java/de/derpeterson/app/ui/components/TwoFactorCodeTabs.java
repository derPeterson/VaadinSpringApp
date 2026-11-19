package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import de.derpeterson.app.i18n.MessageProperties;

/**
 * Native Reiter für die Auswahl von App-Code und Ersatzcode in einem gemeinsamen Eingabebereich.
 */
@StyleSheet("custom-theme/cosmos-provider-settings.css")
public final class TwoFactorCodeTabs extends Tabs {
  private final Span appCodeLabelSpan = new Span();
  private final Span recoveryCodeLabelSpan = new Span();
  private final Tab appCodeTab = new Tab(AppIcon.DEVICES.create(), appCodeLabelSpan);
  private final Tab recoveryCodeTab = new Tab(AppIcon.KEY.create(), recoveryCodeLabelSpan);

  public TwoFactorCodeTabs(
      String appCodeTabId,
      String recoveryCodeTabId,
      Component codePanel,
      MessageProperties messageProperties) {
    appCodeTab.setId(appCodeTabId);
    recoveryCodeTab.setId(recoveryCodeTabId);

    String codePanelId = codePanel.getId().orElseThrow();
    codePanel.getElement().setAttribute("role", "tabpanel");
    appCodeTab.getElement().setAttribute("aria-controls", codePanelId);
    recoveryCodeTab.getElement().setAttribute("aria-controls", codePanelId);

    setWidthFull();
    addClassNames(CosmosCssClasses.PANEL_TABS, "two-factor-code-tabs");
    add(appCodeTab, recoveryCodeTab);
    refreshTexts(messageProperties);

    updateCodePanelLabel(codePanel);
    addSelectedChangeListener(event -> updateCodePanelLabel(codePanel));
  }

  public void refreshTexts(MessageProperties messageProperties) {
    appCodeLabelSpan.setText(messageProperties.getTwoFactorConfirmationAppTab());
    recoveryCodeLabelSpan.setText(messageProperties.getTwoFactorRecoveryCodeLabel());
  }

  public boolean isRecoveryCodeSelected() {
    return getSelectedTab() == recoveryCodeTab;
  }

  public void setRecoveryCodeSelected(boolean selected) {
    setSelectedTab(selected ? recoveryCodeTab : appCodeTab);
  }

  private void updateCodePanelLabel(Component codePanel) {
    Tab selectedTab = isRecoveryCodeSelected() ? recoveryCodeTab : appCodeTab;
    codePanel.getElement().setAttribute("aria-labelledby", selectedTab.getId().orElseThrow());
  }
}
