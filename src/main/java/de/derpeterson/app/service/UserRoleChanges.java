package de.derpeterson.app.service;

import de.derpeterson.app.events.UserRolesChanged;
import de.derpeterson.app.model.enums.UserNotificationType;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.websocket.UserRoleBroadcaster;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class UserRoleChanges {
  private final UserNotificationService notifications;
  private final UserRepository users;
  private final UserRoleBroadcaster events;

  @EventListener
  @Transactional(propagation = Propagation.MANDATORY)
  public void changed(UserRolesChanged change) {
    if (change.before().equals(change.after())) {
      return;
    }
    notifications.recordSecurityEvent(
        users.getReferenceById(change.userId()), UserNotificationType.ROLES_CHANGED);
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            events.publish(change.userId());
          }
        });
  }
}
