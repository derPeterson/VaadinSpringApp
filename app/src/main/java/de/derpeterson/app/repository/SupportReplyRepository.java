package de.derpeterson.app.repository;

import de.derpeterson.app.model.SupportReplyEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SupportReplyRepository extends JpaRepository<SupportReplyEntity, Long> {
  interface UnreadCount {
    Long getTicketId();

    long getAmount();
  }

  @Query(
      "select r from SupportReplyEntity r where r.ticket.id = :ticket and (:internal = true or r.internal = false) order by r.createdAt, r.id")
  List<SupportReplyEntity> visible(Long ticket, boolean internal);

  @Query("select count(r) from SupportReplyEntity r where r.ticket.id = :ticket")
  long countFor(Long ticket);

  @Query(
      """
      select r.ticket.id as ticketId, count(r) as amount from SupportReplyEntity r
      left join SupportTicketViewerEntity v on v.ticketId = r.ticket.id and v.userId = :user
      where r.ticket.id in :tickets
        and ((:administration = false and r.supportAuthor = true and r.internal = false
              and r.id > coalesce(v.requesterReadThrough, 0))
          or (:administration = true and
              (r.supportAuthor = false or r.author.id is null or r.author.id <> :user)
              and r.id > coalesce(v.adminReadThrough, 0)))
      group by r.ticket.id
      """)
  List<UnreadCount> unreadCounts(List<Long> tickets, Long user, boolean administration);
}
