package de.derpeterson.app.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Startvorgaben für Anbieterzugänge. Gespeicherte Anbietereinstellungen haben Vorrang. */
@ConfigurationProperties("app.external-login")
@Getter
public class ExternalLoginProperties {
  private final Provider google = new Provider();
  private final Provider microsoft = new Provider();

  @Getter
  @Setter
  public static class Provider {
    private String clientId;
    private String clientSecret;
    private boolean enabled;
    // Explizite Endpointüberschreibungen ermöglichen eigene isolierte OIDC-Testdienste.
    private String authorizationUri;
    private String tokenUri;
    private String jwkSetUri;

    public boolean configured() {
      return enabled
          && clientId != null
          && !clientId.isBlank()
          && clientSecret != null
          && !clientSecret.isBlank();
    }
  }
}
