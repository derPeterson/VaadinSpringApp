package de.derpeterson.app.service;

import static de.derpeterson.app.service.AvatarImageProcessor.Problem.*;

import de.derpeterson.app.service.AvatarImageProcessor.InvalidImage;
import java.io.*;
import java.time.*;
import java.util.*;
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
    boolean uploading;

    Draft(Long owner, Instant expires) {
      this.owner = owner;
      this.expires = expires;
    }
  }

  public record Image(UUID id, byte[] bytes) {}

  private final Map<UUID, Draft> drafts = new HashMap<>();
  private final Semaphore transfers = new Semaphore(2);
  private final AvatarImageProcessor images;
  private final Clock clock;

  @Autowired
  public AvatarDraftStore(AvatarImageProcessor images) {
    this(images, Clock.systemUTC());
  }

  AvatarDraftStore(AvatarImageProcessor images, Clock clock) {
    this.images = images;
    this.clock = clock;
  }

  public synchronized UUID begin(Long owner) {
    expire();
    if (drafts.size() >= 16) throw new InvalidImage(BUSY);
    UUID id = UUID.randomUUID();
    drafts.put(id, new Draft(owner, clock.instant().plus(Duration.ofMinutes(10))));
    return id;
  }

  private synchronized Draft require(UUID token, Long owner) {
    expire();
    var draft = drafts.get(token);
    if (draft == null || !Objects.equals(owner, draft.owner)) throw new InvalidImage(EXPIRED);
    return draft;
  }

  public UUID upload(UUID token, Long owner, InputStream input) throws IOException {
    Draft draft = require(token, owner);
    synchronized (this) {
      if (draft.uploading || !transfers.tryAcquire()) throw new InvalidImage(BUSY);
      draft.uploading = true;
    }
    try {
      byte[] bytes = AvatarImageProcessor.readLimited(input);
      images.validate(bytes);
      synchronized (this) {
        if (require(token, owner) != draft) throw new InvalidImage(EXPIRED);
        draft.bytes = bytes;
        draft.imageId = UUID.randomUUID();
        return draft.imageId;
      }
    } finally {
      synchronized (this) {
        draft.uploading = false;
      }
      transfers.release();
    }
  }

  public synchronized Image image(UUID token, Long owner, UUID imageId) {
    var draft = require(token, owner);
    if (draft.uploading) throw new InvalidImage(BUSY);
    if (draft.bytes == null || imageId == null || !imageId.equals(draft.imageId))
      throw new InvalidImage(MISSING);
    return new Image(draft.imageId, draft.bytes);
  }

  public synchronized void release(UUID token) {
    if (token != null) drafts.remove(token);
  }

  @Scheduled(fixedDelay = 60_000)
  public synchronized void expire() {
    drafts.values().removeIf(draft -> !clock.instant().isBefore(draft.expires));
  }
}
