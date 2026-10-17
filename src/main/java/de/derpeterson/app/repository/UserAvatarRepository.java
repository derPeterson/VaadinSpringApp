package de.derpeterson.app.repository;

import de.derpeterson.app.model.UserAvatarEntity;
import de.derpeterson.app.model.enums.AvatarMode;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserAvatarRepository extends JpaRepository<UserAvatarEntity, Long> {
  interface Metadata {
    Long getUserId();

    Long getVersion();

    AvatarMode getMode();

    boolean getHasImage();

    UUID getImageRevision();
  }

  @Query(
      "select a.userId as userId, a.version as version, a.mode as mode, a.hasImage as hasImage, a.imageRevision as imageRevision from UserAvatarEntity a where a.userId in :ids")
  List<Metadata> metadata(Collection<Long> ids);
}
