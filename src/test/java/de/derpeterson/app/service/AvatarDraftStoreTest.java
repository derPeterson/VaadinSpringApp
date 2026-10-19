package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.derpeterson.app.service.AvatarImageProcessor.InvalidImage;
import de.derpeterson.app.service.AvatarImageProcessor.Problem;
import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class AvatarDraftStoreTest {
  @Test
  void expiryOwnershipAndCapacityBoundTemporaryOriginals() throws Exception {
    AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-10-17T12:00:00Z"));
    Clock clock =
        new Clock() {
          public ZoneId getZone() {
            return ZoneOffset.UTC;
          }

          public Clock withZone(ZoneId zone) {
            return this;
          }

          public Instant instant() {
            return now.get();
          }
        };
    AvatarDraftStore drafts = new AvatarDraftStore(new AvatarImageProcessor(), clock);
    UUID token = drafts.begin(1L);
    UUID id =
        drafts.upload(
            token, 1L, new ByteArrayInputStream(AvatarImageProcessorTest.image("png", 10, 10)));
    assertNotNull(drafts.image(token, 1L, id));
    assertEquals(
        Problem.EXPIRED,
        assertThrows(InvalidImage.class, () -> drafts.image(token, 2L, id)).problem());
    for (int i = 0; i < 15; i++) {
      drafts.begin(1L);
    }
    assertEquals(Problem.BUSY, assertThrows(InvalidImage.class, () -> drafts.begin(1L)).problem());
    now.set(now.get().plusSeconds(601));
    drafts.expire();
    assertEquals(
        Problem.EXPIRED,
        assertThrows(InvalidImage.class, () -> drafts.image(token, 1L, id)).problem());
    assertNotNull(drafts.begin(1L));
  }

  @Test
  void invalidReplacementRetainsTheLastAcceptedOriginal() throws Exception {
    AvatarDraftStore drafts = new AvatarDraftStore(new AvatarImageProcessor());
    UUID token = drafts.begin(1L);
    UUID id =
        drafts.upload(
            token, 1L, new ByteArrayInputStream(AvatarImageProcessorTest.image("png", 10, 10)));
    assertThrows(
        InvalidImage.class,
        () -> drafts.upload(token, 1L, new ByteArrayInputStream("bad image".getBytes())));
    assertEquals(id, drafts.image(token, 1L, id).id());
    assertEquals(
        Problem.MISSING,
        assertThrows(InvalidImage.class, () -> drafts.image(token, 1L, UUID.randomUUID()))
            .problem());
    drafts.release(token);
    assertEquals(
        Problem.EXPIRED,
        assertThrows(InvalidImage.class, () -> drafts.image(token, 1L, id)).problem());
  }

  @Test
  void parallelUploadIsRejectedAndLateCompletionCannotRestoreACancelledDraft() throws Exception {
    AvatarDraftStore drafts = new AvatarDraftStore(new AvatarImageProcessor());
    UUID token = drafts.begin(1L);
    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch finish = new CountDownLatch(1);
    byte[] bytes = AvatarImageProcessorTest.image("png", 10, 10);
    var input =
        new FilterInputStream(new ByteArrayInputStream(bytes)) {
          @Override
          public int read(byte[] data, int offset, int length) throws IOException {
            entered.countDown();
            try {
              if (!finish.await(5, TimeUnit.SECONDS)) {
                throw new IOException("test synchronization timeout");
              }
            } catch (InterruptedException failure) {
              Thread.currentThread().interrupt();
              throw new IOException(failure);
            }
            return super.read(data, offset, length);
          }
        };
    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      Future<UUID> transfer = executor.submit(() -> drafts.upload(token, 1L, input));
      try {
        assertTrue(entered.await(5, TimeUnit.SECONDS));
        assertEquals(
            Problem.BUSY,
            assertThrows(
                    InvalidImage.class,
                    () -> drafts.upload(token, 1L, new ByteArrayInputStream(bytes)))
                .problem());
        drafts.release(token);
      } finally {
        finish.countDown();
      }
      ExecutionException failure =
          assertThrows(ExecutionException.class, () -> transfer.get(5, TimeUnit.SECONDS));
      assertInstanceOf(InvalidImage.class, failure.getCause());
      assertEquals(Problem.EXPIRED, ((InvalidImage) failure.getCause()).problem());
    }
  }
}
