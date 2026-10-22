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
  public boolean matches(HttpServletRequest request) {
    if (!CsrfFilter.DEFAULT_CSRF_MATCHER.matches(request)) {
      return false;
    }
    String path = HandlerHelper.getRequestPathInsideContext(request);
    Optional<String> inside = HandlerHelper.getPathIfInsideServlet(servletMapping, path);
    if (inside.isEmpty() || !HandlerHelper.isFrameworkInternalRequest(servletMapping, request)) {
      return true;
    }
    String relative = inside.get();
    if (relative.isEmpty() || "/".equals(relative)) {
      String type = request.getParameter("v-r");
      return Arrays.stream(HandlerHelper.RequestType.values())
          .noneMatch(known -> known.getIdentifier().equals(type));
    }
    // HandlerHelper additionally validates dynamic-resource paths against traversal.
    // Their upload security key and UIDL/Push tokens are checked by the Flow handlers.
    boolean push = "VAADIN/push".equals(relative) && "push".equals(request.getParameter("v-r"));
    return !push && !relative.startsWith("VAADIN/dynamic/resource/");
  }
}
