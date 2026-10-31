package de.derpeterson.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Dauerhafte Anbieterzuordnung, ohne Zugriffs- oder Refresh-Tokens. */
@Entity
@Table(
    name = "external_identities",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_external_subject",
          columnNames = {"provider", "issuer", "subject"}),
      @UniqueConstraint(
          name = "uk_external_user_provider",
          columnNames = {"user_id", "provider"})
    })
@Getter
@Setter
@NoArgsConstructor
public class ExternalIdentityEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 16)
  private String provider;

  @Column(nullable = false, length = 255)
  private String issuer;

  @Column(nullable = false, length = 255)
  private String subject;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private UserEntity user;
}
