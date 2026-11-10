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
    return vaadinService -> {
      StaticFileServer fileServer = new StaticFileServer(vaadinService);
      if (vaadinService.getDeploymentConfiguration().isProductionMode()) {
        return fileServer;
      }

      return (httpRequest, httpResponse) -> {
        String resourcePath =
            httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
        if (resourcePath.startsWith("/custom-theme/") && resourcePath.endsWith(".css")) {
          // Die IDE aktualisiert die CSS-Datei, aber keine gepackten Kopien
          // aus früheren Produktionsbuilds. Vaadin soll hier die CSS-Datei lesen.
          return fileServer.serveStaticResource(
              new UncompressedCssRequest(httpRequest), httpResponse);
        }
        return fileServer.serveStaticResource(httpRequest, httpResponse);
      };
    };
  }

  private static final class UncompressedCssRequest extends HttpServletRequestWrapper {

    private UncompressedCssRequest(HttpServletRequest httpRequest) {
      super(httpRequest);
    }

    @Override
    public String getHeader(String headerName) {
      if ("Accept-Encoding".equalsIgnoreCase(headerName)) {
        return "identity";
      }
      return super.getHeader(headerName);
    }

    @Override
    public Enumeration<String> getHeaders(String headerName) {
      if ("Accept-Encoding".equalsIgnoreCase(headerName)) {
        return Collections.enumeration(Collections.singletonList("identity"));
      }
      return super.getHeaders(headerName);
    }
  }
}
