package de.derpeterson.frontendreview;

import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.security.SessionManagementService;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.views.LoginView;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;

/** Vorbereiteter echter eingeschränkter Anmeldeschritt, ausschließlich für das feste Testkonto. */
@Route("images/visitor-review/recovery")
@AnonymousAllowed
public class VisitorRecoveryPage extends LoginView {
  public VisitorRecoveryPage(
      MessageProperties texts,
      AuthenticationManager authentication,
      SecurityService security,
      HttpServletRequest request,
      ConfigService config,
      SessionManagementService sessions,
      Environment environment) {
    super(texts, authentication, security, request, config, sessions);
    if (!environment.getProperty("frontend.review.visitors", Boolean.class, false)
        || !environment
            .getRequiredProperty("spring.datasource.url")
            .startsWith("jdbc:h2:mem:account-reference-review;")) {
      throw new IllegalStateException("Only available with the disposable visitor test account");
    }
    sessions.startRecovery(request, "reference-user@example.test", "ReferenceUser1!", false);
  }
}
