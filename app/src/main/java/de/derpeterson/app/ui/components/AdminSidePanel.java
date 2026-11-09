package de.derpeterson.app.ui.components;

import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_DESCRIPTION;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_HEADING;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_ICON;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_TITLE;
import static de.derpeterson.app.ui.components.CosmosClasses.REFERENCE_OVERLAY;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import de.derpeterson.app.i18n.MessageProperties;
import java.util.List;

/** Rechte, modal überlagerte Seitenleiste; native Fokus- und Escape-Bedienung bleibt erhalten. */
public class AdminSidePanel extends Dialog {
  private final H2 title = new H2();
  private final Span description = new Span();
  private final Button close = new Button(AppIcon.CLOSE_CIRCLE.create());

  public AdminSidePanel(MessageProperties texts, AppIcon icon) {
    addClassNames("cosmos-theme", REFERENCE_OVERLAY, "admin-reference-drawer");
    getThemeNames().addAll(java.util.List.of("cosmos", "cosmos-reference"));
    title.addClassName(PANEL_TITLE);
    description.addClassName(PANEL_DESCRIPTION);
    description.setVisible(false);
    Div heading = new Div(title, description);
    heading.addClassName(PANEL_HEADING);
    Div symbol = new Div(icon.create());
    symbol.addClassName(PANEL_ICON);
    close.addClassName("cosmos-overlay-close");
    close.addClickListener(event -> close());
    Div header = new Div(symbol, heading, close);
    header.addClassName("admin-reference-drawer-heading");
    getHeader().add(header);
    setCloseOnEsc(true);
    setCloseOnOutsideClick(true);
    addOpenedChangeListener(event -> updateBounds());
    refreshCloseText(texts);
  }

  public void setHeading(String text) {
    title.setText(text);
  }

  public void setDescription(String text) {
    description.setText(text);
    description.setVisible(!text.isBlank());
  }

  public void addEffectiveUse(MessageProperties texts, List<String> notes) {
    UnorderedList list = new UnorderedList();
    notes.forEach(note -> list.add(new ListItem(note)));
    Div section = new Div(new H4(texts.getAdminConfigEffectiveTitle()), list);
    section.addClassNames("admin-help-rules", "admin-help-effective-use");
    add(section);
  }

  /** Bindet die Leiste an den Seiteninhalt und hält sichtbaren Kopf/Copyrightbereich frei. */
  private void updateBounds() {
    getElement()
        .executeJs(
            """
            this._cosmosStopBounds?.();
            if (!this.opened) return;
            const drawer = this;
            const update = () => {
              if (!drawer.isConnected || !drawer.opened) { stop(); return; }
              const layout = document.querySelector('vaadin-app-layout.cosmos-reference-admin');
              const header = layout?.querySelector('.cosmos-topbar')?.getBoundingClientRect();
              const footer = layout?.querySelector('.cosmos-footer')?.getBoundingClientRect();
              const content = layout?.querySelector('.cosmos-admin-sections')?.getBoundingClientRect();
              const viewportWidth = document.documentElement.clientWidth;
              const height = window.innerHeight;
              const top = Math.min(height, Math.max(0, header?.bottom ?? 65));
              const bottom = footer ? Math.max(0, Math.min(height, footer.bottom) - Math.max(0, footer.top)) : 0;
              const right = window.innerWidth >= 1024 && content ? Math.max(0, viewportWidth - content.right) : 0;
              drawer.style.setProperty('--admin-drawer-top', top + 'px');
              drawer.style.setProperty('--admin-drawer-bottom', bottom + 'px');
              drawer.style.setProperty('--admin-drawer-right', right + 'px');
            };
            const stop = () => {
              window.removeEventListener('resize', update);
              document.removeEventListener('scroll', update, true);
              delete drawer._cosmosStopBounds;
            };
            drawer._cosmosStopBounds = stop;
            window.addEventListener('resize', update);
            document.addEventListener('scroll', update, true);
            update();
            """);
  }

  public void refreshCloseText(MessageProperties texts) {
    close.setAriaLabel(texts.getNotificationsClose());
    close.setTooltipText(texts.getNotificationsClose());
  }
}
