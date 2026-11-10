package de.derpeterson.app.security;

import com.vaadin.flow.server.HandlerHelper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

/** Spring protects HTTP mutations; only actual mapped Flow transports use Flow's own protection. */
final class FlowCsrfRequestMatcher implements RequestMatcher {
  private final String servletMapping;

  FlowCsrfRequestMatcher(String servletMapping) {
    this.servletMapping = servletMapping;
  }

  @Override
  public boolean matches(HttpServletRequest httpRequest) {
    if (!CsrfFilter.DEFAULT_CSRF_MATCHER.matches(httpRequest)) {
      return false;
    }

    String requestPath = HandlerHelper.getRequestPathInsideContext(httpRequest);
    Optional<String> vaadinPathOptional =
        HandlerHelper.getPathIfInsideServlet(servletMapping, requestPath);
    if (vaadinPathOptional.isEmpty()
        || !HandlerHelper.isFrameworkInternalRequest(servletMapping, httpRequest)) {
      return true;
    }

    String vaadinRelativePath = vaadinPathOptional.get();
    if (vaadinRelativePath.isEmpty() || "/".equals(vaadinRelativePath)) {
      String vaadinRequestType = httpRequest.getParameter("v-r");
      return Arrays.stream(HandlerHelper.RequestType.values())
          .noneMatch(
              knownRequestType -> knownRequestType.getIdentifier().equals(vaadinRequestType));
    }

    // HandlerHelper additionally validates dynamic-resource paths against traversal.
    // Their upload security key and UIDL/Push tokens are checked by the Flow handlers.
    boolean pushRequest =
        "VAADIN/push".equals(vaadinRelativePath) && "push".equals(httpRequest.getParameter("v-r"));

    return !pushRequest && !vaadinRelativePath.startsWith("VAADIN/dynamic/resource/");
  }
}
