package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.derpeterson.app.model.enums.AvatarImageFormat;
import de.derpeterson.app.service.AvatarDraftStore.Image;
import de.derpeterson.app.service.AvatarImageProcessor.SourceMetadata;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AvatarDraftStoreImageTest {
  private static final Instant UPLOADED_AT = Instant.parse("2026-10-31T12:00:00Z");
  private static final SourceMetadata METADATA =
      new SourceMetadata(AvatarImageFormat.PNG, 3, 10, 10);

  @Test
  void equalImageContentsHaveValueEqualityWithoutDumpingTheirBytes() {
    UUID id = UUID.randomUUID();
    Image first = new Image(id, new byte[] {1, 2, 3}, UPLOADED_AT, METADATA);
    Image second = new Image(id, new byte[] {1, 2, 3}, UPLOADED_AT, METADATA);

    assertEquals(first, second);
    assertEquals(first.hashCode(), second.hashCode());
    assertNotEquals(first, new Image(id, new byte[] {1, 2, 4}, UPLOADED_AT, METADATA));
    assertNotEquals(
        first, new Image(UUID.randomUUID(), new byte[] {1, 2, 3}, UPLOADED_AT, METADATA));
    assertNotEquals(
        first, new Image(id, new byte[] {1, 2, 3}, UPLOADED_AT.plusSeconds(1), METADATA));
    assertNotEquals(
        first,
        new Image(
            id,
            new byte[] {1, 2, 3},
            UPLOADED_AT,
            new SourceMetadata(AvatarImageFormat.JPEG, 3, 10, 10)));
    assertTrue(first.toString().contains("byteCount=3"));
  }

  @Test
  void constructorAndAccessorDoNotShareMutableArrayStorage() {
    byte[] original = {1, 2, 3};
    Image image = new Image(UUID.randomUUID(), original, UPLOADED_AT, METADATA);
    original[0] = 9;
    byte[] read = image.bytes();
    read[1] = 9;

    assertArrayEquals(new byte[] {1, 2, 3}, image.bytes());
  }

  @Test
  void nullableRecordComponentsKeepTheirExistingContract() {
    Image image = new Image(null, null, null, null);

    assertNull(image.id());
    assertNull(image.bytes());
    assertNull(image.uploadedAt());
    assertNull(image.metadata());
    assertEquals(image, new Image(null, null, null, null));
    assertEquals(image.hashCode(), new Image(null, null, null, null).hashCode());
  }

  @Test
  void callersCannotCorruptTheStoredDraftThroughAnImageSnapshot() throws Exception {
    AvatarDraftStore store = new AvatarDraftStore(new AvatarImageProcessor());
    UUID token = store.begin(7L);
    byte[] original = AvatarImageProcessorTest.image("png", 10, 10);
    UUID imageId;
    try (ByteArrayInputStream input = new ByteArrayInputStream(original)) {
      imageId = store.upload(token, 7L, input);
    }
    Image snapshot = store.image(token, 7L, imageId);
    byte[] exposed = snapshot.bytes();
    exposed[0] = 0;

    assertArrayEquals(original, store.image(token, 7L, imageId).bytes());
    store.release(token);
  }
}
