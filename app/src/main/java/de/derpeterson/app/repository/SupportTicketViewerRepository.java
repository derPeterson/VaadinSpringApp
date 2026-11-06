package de.derpeterson.app.repository;

import de.derpeterson.app.model.SupportTicketViewerEntity;
import de.derpeterson.app.model.UserEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SupportTicketViewerRepository
    extends JpaRepository<SupportTicketViewerEntity, Long> {
  Optional<SupportTicketViewerEntity> findByTicketIdAndUserId(Long ticketId, Long userId);

  @Query(
      "select v.ticketId from SupportTicketViewerEntity v where v.userId = :userId and v.ticketId in :ticketIds and v.subscribed = true")
  List<Long> subscribedTickets(Long userId, List<Long> ticketIds);

  @Query(
      """
      select distinct u from SupportTicketViewerEntity v join UserEntity u on u.id = v.userId
      join u.roleEntities role
      where v.ticketId = :ticket and v.subscribed = true and u.id <> :sender
        and u.enabled = true and u.verificationPending = false
        and (role.name = de.derpeterson.app.model.enums.RoleType.ROLE_SUPERADMIN
          or (u.id = :assignee and role.name = de.derpeterson.app.model.enums.RoleType.ROLE_ADMIN))
      """)
  List<UserEntity> recipients(Long ticket, Long assignee, Long sender);

  @Query(
      """
      select distinct u from UserEntity u join u.roleEntities role
      where u.enabled = true and u.verificationPending = false and u.id <> :sender
        and role.name in (de.derpeterson.app.model.enums.RoleType.ROLE_ADMIN,
          de.derpeterson.app.model.enums.RoleType.ROLE_SUPERADMIN)
        and (:assignee is null or u.id = :assignee
          or (role.name = de.derpeterson.app.model.enums.RoleType.ROLE_SUPERADMIN
            and exists (select v.id from SupportTicketViewerEntity v
              where v.ticketId = :ticket and v.userId = u.id and v.subscribed = true)))
      """)
  List<UserEntity> requesterClosedRecipients(Long ticket, Long assignee, Long sender);
}
