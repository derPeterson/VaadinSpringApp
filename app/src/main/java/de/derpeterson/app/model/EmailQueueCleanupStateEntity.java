package de.derpeterson.app.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** Ein gemeinsamer Laufstand pro Datenbank; enthält keine Empfänger oder Mailinhalte. */
@Entity
@Table(name = "email_queue_cleanup_state")
@Getter
@Setter
public class EmailQueueCleanupStateEntity {
  @Id private Long id = 1L;
  private LocalDateTime lastRunAt;
  private int resolvedCount;
  private boolean busy;
}
