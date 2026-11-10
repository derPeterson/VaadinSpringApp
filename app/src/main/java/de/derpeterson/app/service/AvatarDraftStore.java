package de.derpeterson.app.service;

import static de.derpeterson.app.service.AvatarImageProcessor.Problem.BUSY;
import static de.derpeterson.app.service.AvatarImageProcessor.Problem.EXPIRED;
import static de.derpeterson.app.service.AvatarImageProcessor.Problem.MISSING;

import de.derpeterson.app.service.AvatarImageProcessor.InvalidImage;
import de.derpeterson.app.service.AvatarImageProcessor.SourceMetadata;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Höchstens 16 Originale, je 5 MiB, maximal zehn Minuten; kein Dateisystemarchiv. */
@Component
public class AvatarDraftStore {
  private static final class Draft {
    final Long owner;
    final Instant expires;
    byte[] bytes;
    UUID imageId;
    Instant uploadedAt;
    SourceMetadata metadata;
    boolean uploading;

    Draft(Long owner, Instant expires) {
      this.owner = owner;
      this.expires = expires;
    }
  }

  public record Image(UUID id, byte[] bytes, Instant uploadedAt, SourceMetadata metadata) {
    public Image {
      bytes = bytes == null ? null : bytes.clone();
    }

    @Override
    public byte[] bytes() {
      return bytes == null ? null : bytes.clone();
    }

    @Override
    public boolean equals(Object other) {
      return this == other
          || other instanceof Image image
              && Objects.equals(id, image.id)
              && Objects.equals(uploadedAt, image.uploadedAt)
              && Objects.equals(metadata, image.metadata)
              && Arrays.equals(bytes, image.bytes);
    }

    @Override
    public int hashCode() {
      return 31 * Objects.hash(id, uploadedAt, metadata) + Arrays.hashCode(bytes);
    }

    @Override
    public String toString() {
      return "Image[id="
          + id
          + ", byteCount="
          + (bytes == null ? "null" : Integer.toString(bytes.length))
          + "]";
    }
  }

  private final Map<UUID, Draft> avatarDraftsById = new HashMap<>();
  private final Semaphore uploadTransferPermits = new Semaphore(2);
  private final AvatarImageProcessor avatarImageProcessor;
  private final Clock avatarDraftClock;

  @Autowired
  public AvatarDraftStore(AvatarImageProcessor avatarImageProcessor) {
    this(avatarImageProcessor, Clock.systemUTC());
  }

  AvatarDraftStore(AvatarImageProcessor avatarImageProcessor, Clock avatarDraftClock) {
    this.avatarImageProcessor = avatarImageProcessor;
    this.avatarDraftClock = avatarDraftClock;
  }

  public synchronized UUID begin(Long owner) {
    expire();
    if (avatarDraftsById.size() >= 16) {
      throw new InvalidImage(BUSY);
    }
    UUID id = UUID.randomUUID();
    avatarDraftsById.put(
        id, new Draft(owner, avatarDraftClock.instant().plus(Duration.ofMinutes(10))));
    return id;
  }

  private synchronized Draft require(UUID token, Long owner) {
    expire();
    Draft draft = avatarDraftsById.get(token);
    if (draft == null || !Objects.equals(owner, draft.owner)) {
      throw new InvalidImage(EXPIRED);
    }
    return draft;
  }

  public UUID upload(UUID avatarDraftId, Long ownerUserId, InputStream uploadInputStream)
      throws IOException {
    Draft avatarDraft = require(avatarDraftId, ownerUserId);

    synchronized (this) {
      if (avatarDraft.uploading || !uploadTransferPermits.tryAcquire()) {
        throw new InvalidImage(BUSY);
      }
      avatarDraft.uploading = true;
    }

    try {
      byte[] uploadedImageBytes = AvatarImageProcessor.readLimited(uploadInputStream);
      SourceMetadata uploadedImageMetadata = avatarImageProcessor.validate(uploadedImageBytes);

      synchronized (this) {
        if (require(avatarDraftId, ownerUserId) != avatarDraft) {
          throw new InvalidImage(EXPIRED);
        }
        avatarDraft.bytes = uploadedImageBytes;
        avatarDraft.imageId = UUID.randomUUID();
        avatarDraft.uploadedAt = avatarDraftClock.instant();
        avatarDraft.metadata = uploadedImageMetadata;
        return avatarDraft.imageId;
      }
    } finally {
      synchronized (this) {
        avatarDraft.uploading = false;
      }
      uploadTransferPermits.release();
    }
  }

  public synchronized Image image(UUID token, Long owner, UUID imageId) {
    Draft draft = require(token, owner);
    if (draft.uploading) {
      throw new InvalidImage(BUSY);
    }
    if (draft.bytes == null || imageId == null || !imageId.equals(draft.imageId)) {
      throw new InvalidImage(MISSING);
    }
    return new Image(draft.imageId, draft.bytes, draft.uploadedAt, draft.metadata);
  }

  public synchronized void release(UUID token) {
    if (token != null) {
      avatarDraftsById.remove(token);
    }
  }

  @Scheduled(fixedDelay = 60_000)
  public synchronized void expire() {
    avatarDraftsById
        .values()
        .removeIf(draft -> !avatarDraftClock.instant().isBefore(draft.expires));
  }
}
