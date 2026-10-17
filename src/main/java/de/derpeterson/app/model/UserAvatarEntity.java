package de.derpeterson.app.model;

import de.derpeterson.app.model.enums.AvatarMode;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "user_avatar")
@Getter
@Setter
@NoArgsConstructor
public class UserAvatarEntity {
  @Id private Long userId;

  @MapsId
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private UserEntity user;

  @Version private Long version;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private AvatarMode mode = AvatarMode.INITIALS;

  @Column(nullable = false)
  private boolean hasImage;

  private UUID imageRevision;

  @Lob
  @Column(name = "image_data")
  private byte[] image;
}
