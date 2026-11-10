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
  private final UserNotificationService userNotificationService;
  private final UserRepository userRepository;
  private final UserRoleBroadcaster userRoleBroadcaster;

  @EventListener
  @Transactional(propagation = Propagation.MANDATORY)
  public void changed(UserRolesChanged userRolesChangedEvent) {
    if (userRolesChangedEvent
        .rolesBeforeChange()
        .equals(userRolesChangedEvent.rolesAfterChange())) {
      return;
    }
    userNotificationService.recordSecurityEvent(
        userRepository.getReferenceById(userRolesChangedEvent.userId()),
        UserNotificationType.ROLES_CHANGED);
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            userRoleBroadcaster.publish(userRolesChangedEvent.userId());
          }
        });
  }
}
