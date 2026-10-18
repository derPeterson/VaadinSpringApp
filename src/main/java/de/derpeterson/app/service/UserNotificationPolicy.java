package de.derpeterson.app.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UserNotificationPolicy {
  private final int retentionDays;

  public UserNotificationPolicy(
      @Value("${app.user-notifications.retention-days:180}") int retentionDays) {
    if (retentionDays < 1)
      throw new IllegalArgumentException("Positive notification retention required");
    this.retentionDays = retentionDays;
  }

  public int retentionDays() {
    return retentionDays;
  }
}
