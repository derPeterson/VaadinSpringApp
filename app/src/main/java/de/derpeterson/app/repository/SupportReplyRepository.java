package de.derpeterson.app.repository;

import de.derpeterson.app.model.SupportReplyEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SupportReplyRepository extends JpaRepository<SupportReplyEntity, Long> {
  @Query(
      "select r from SupportReplyEntity r where r.ticket.id = :ticket and (:internal = true or r.internal = false) order by r.createdAt, r.id")
  List<SupportReplyEntity> visible(Long ticket, boolean internal);

  @Query("select count(r) from SupportReplyEntity r where r.ticket.id = :ticket")
  long countFor(Long ticket);
}
