package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.security.MaintenanceService;
import java.util.Locale;

/** Kompakter Betriebsstatus; keine technische Prüfung einzelner Hintergrunddienste. */
public class CosmosServiceStatusComponent extends Div {
  private final transient MaintenanceService maintenance;
  private final transient MessageProperties messages;
  private final Span title = new Span();
  private final Span description = new Span();

  public CosmosServiceStatusComponent(MaintenanceService maintenance, MessageProperties messages) {
    this.maintenance = maintenance;
    this.messages = messages;
    setId("admin-service-status");
    addClassName("cosmos-service-status");
    getElement().setAttribute("role", "status");
    var dot = new Span();
    dot.addClassName("cosmos-service-status-dot");
    dot.getElement().setAttribute("aria-hidden", "true");
    title.addClassName("cosmos-service-status-title");
    description.addClassName("cosmos-service-status-description");
    add(dot, new Div(title, description));
    refresh();
  }

  public void refresh() {
    var state = maintenance.state();
    getElement().setAttribute("data-state", state.name().toLowerCase(Locale.ROOT));
    switch (state) {
      case OPEN -> {
        title.setText(messages.getMaintenanceStatusOnline());
        description.setText(messages.getMaintenanceServicesReady());
      }
      case ACTIVE -> {
        title.setText(messages.getMaintenanceStatusActive());
        description.setText(messages.getMaintenanceServicesRestricted());
      }
      case UNAVAILABLE -> {
        title.setText(messages.getMaintenanceStatusUnavailable());
        description.setText(messages.getMaintenanceServicesRestricted());
      }
    }
  }
}
