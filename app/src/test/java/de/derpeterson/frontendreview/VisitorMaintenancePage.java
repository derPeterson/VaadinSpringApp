package de.derpeterson.frontendreview;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.security.MaintenanceService;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.views.MaintenanceView;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.env.Environment;

/** Echte Wartungsansicht in der isolierten Sichtung, ohne alle anderen Testszenen zu sperren. */
@Route("images/visitor-review/maintenance")
@AnonymousAllowed
public class VisitorMaintenancePage extends MaintenanceView {
  public VisitorMaintenancePage(
      ConfigService config,
      MessageProperties texts,
      MaintenanceService maintenance,
      SecurityService security,
      HttpServletRequest request,
      Environment environment) {
    super(config, texts, maintenance, security, request);
    if (!environment.getProperty("frontend.review.visitors", Boolean.class, false)) {
      throw new IllegalStateException("Only available in the isolated visitor review");
    }
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    // Nur diese Testansicht bleibt bei ausgeschaltetem Wartungsmodus zur Sichtung geöffnet.
  }
}
