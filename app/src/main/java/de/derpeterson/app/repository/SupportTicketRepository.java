package de.derpeterson.app.repository;

import de.derpeterson.app.model.SupportTicketEntity;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface SupportTicketRepository
    extends JpaRepository<SupportTicketEntity, Long>,
        JpaSpecificationExecutor<SupportTicketEntity> {
  @Override
  @EntityGraph(attributePaths = {"owner", "assignee"})
  Page<SupportTicketEntity> findAll(
      Specification<SupportTicketEntity> specification, Pageable page);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select t from SupportTicketEntity t where t.id = :id")
  Optional<SupportTicketEntity> lock(Long id);

  @Query(
      "select t from SupportTicketEntity t where t.owner.id = :owner order by t.publicUpdatedAt desc, t.id desc")
  List<SupportTicketEntity> own(Long owner, Pageable page);

  @Query(
      "select count(t) from SupportTicketEntity t where t.owner.id = :owner and t.status in :activeStatuses")
  long openFor(Long owner, List<SupportTicketEntity.Status> activeStatuses);

  @Query(
      "select count(t) from SupportTicketEntity t where t.owner.id = :owner and t.createdAt > :after")
  long createdSince(Long owner, Instant after);

  @Modifying
  @Query(
      "delete from SupportTicketEntity t where t.status in :closedStatuses and t.updatedAt < :before")
  int deleteClosedBefore(Instant before, List<SupportTicketEntity.Status> closedStatuses);
}
