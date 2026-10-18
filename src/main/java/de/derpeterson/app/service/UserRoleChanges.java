package de.derpeterson.app.service;

import de.derpeterson.app.events.UserRolesChanged;
import de.derpeterson.app.model.enums.UserMessageType;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.websocket.UserRoleBroadcaster;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.*;

@Component
@RequiredArgsConstructor
public class UserRoleChanges {
  private final UserMessageService messages;
  private final UserRepository users;
  private final UserRoleBroadcaster events;

  @EventListener
  @Transactional(propagation = Propagation.MANDATORY)
  public void changed(UserRolesChanged change) {
    if (change.before().equals(change.after())) return;
    messages.recordSecurityEvent(
        users.getReferenceById(change.userId()), UserMessageType.ROLES_CHANGED);
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            events.publish(change.userId());
          }
        });
  }
}
