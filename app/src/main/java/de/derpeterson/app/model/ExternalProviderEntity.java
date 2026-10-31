package de.derpeterson.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Zugangsdaten werden wie die SMTP-Konfiguration ausschließlich serverseitig gespeichert. */
@Entity
@Table(name = "external_providers")
@Getter
@Setter
@NoArgsConstructor
public class ExternalProviderEntity {
  @Id
  @Column(length = 16)
  private String provider;

  @Column(name = "client_id", nullable = false, length = 512)
  private String clientId;

  @Column(name = "client_secret", nullable = false, length = 2048)
  private String clientSecret;

  @Column(nullable = false)
  private boolean enabled;

  @Version private Long version;
}
