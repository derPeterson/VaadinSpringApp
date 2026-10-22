package de.derpeterson.app.config;

import com.vaadin.flow.server.StaticFileHandlerFactory;
import com.vaadin.flow.server.StaticFileServer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.util.Collections;
import java.util.Enumeration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class DevelopmentCssConfiguration {

  @Bean
  public StaticFileHandlerFactory staticFileHandlerFactory() {
    return service -> {
      StaticFileServer fileServer = new StaticFileServer(service);
      if (service.getDeploymentConfiguration().isProductionMode()) {
        return fileServer;
      }

      return (request, response) -> {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.startsWith("/custom-theme/") && path.endsWith(".css")) {
          // Die IDE aktualisiert die CSS-Datei, aber keine gepackten Kopien
          // aus früheren Produktionsbuilds. Vaadin soll hier die CSS-Datei lesen.
          return fileServer.serveStaticResource(new UncompressedCssRequest(request), response);
        }
        return fileServer.serveStaticResource(request, response);
      };
    };
  }

  private static final class UncompressedCssRequest extends HttpServletRequestWrapper {

    private UncompressedCssRequest(HttpServletRequest request) {
      super(request);
    }

    @Override
    public String getHeader(String name) {
      if ("Accept-Encoding".equalsIgnoreCase(name)) {
        return "identity";
      }
      return super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
      if ("Accept-Encoding".equalsIgnoreCase(name)) {
        return Collections.enumeration(Collections.singletonList("identity"));
      }
      return super.getHeaders(name);
    }
  }
}
