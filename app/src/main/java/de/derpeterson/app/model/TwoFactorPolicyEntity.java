package de.derpeterson.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "two_factor_policy")
@Getter
@Setter
@NoArgsConstructor
public class TwoFactorPolicyEntity {
  @Id private Integer id = 1;
  @Version private Long version;

  @Column(nullable = false, length = 12000)
  private String rules;
}
