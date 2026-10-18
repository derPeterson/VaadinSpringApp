package de.derpeterson.app.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UserMessagePolicy {
  private final int retentionDays;

  public UserMessagePolicy(@Value("${app.user-messages.retention-days:180}") int retentionDays) {
    if (retentionDays < 1)
      throw new IllegalArgumentException("Positive message retention required");
    this.retentionDays = retentionDays;
  }

  public int retentionDays() {
    return retentionDays;
  }
}
