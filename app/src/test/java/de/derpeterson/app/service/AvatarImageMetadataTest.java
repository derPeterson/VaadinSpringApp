package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.derpeterson.app.service.AvatarImageProcessor.Crop;
import de.derpeterson.app.service.AvatarImageProcessor.InvalidImage;
import de.derpeterson.app.service.AvatarImageProcessor.Problem;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Echte Bilddateien mit kontrollierten Metadaten; ausschließlich öffentliche Upload-APIs. */
class AvatarImageMetadataTest {
  private static final List<Color> SOURCE =
      List.of(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW);
  private final AvatarImageProcessor processor = new AvatarImageProcessor();

  static Stream<Arguments> orientations() {
    return Stream.of("png", "jpeg")
        .flatMap(
            format ->
                Stream.of(ByteOrder.LITTLE_ENDIAN, ByteOrder.BIG_ENDIAN)
                    .flatMap(
                        order ->
                            IntStream.rangeClosed(1, 8)
                                .mapToObj(
                                    orientation -> Arguments.of(format, order, orientation))));
  }

  @ParameterizedTest
  @MethodSource("orientations")
  void appliesAllOrientationsAndBothByteOrdersWithoutKeepingMetadata(
      String format, ByteOrder order, int orientation) throws Exception {
    byte[] image = withMetadata(format, tiff(order, orientation));
    processor.validate(image);
    assertCorners(processor.normalize(image, Crop.centered()), expectedCorners(orientation));
  }

  static Stream<Arguments> malformedMetadata() {
    return Stream.of("png", "jpeg")
        .flatMap(
            format ->
                Stream.of(
                        "empty",
                        "short-header",
                        "byte-order",
                        "magic",
                        "unsigned-offset",
                        "last-byte-offset",
                        "empty-directory",
                        "short-entry",
                        "huge-directory",
                        "wrong-tag",
                        "wrong-type",
                        "wrong-count",
                        "zero",
                        "nine")
                    .map(kind -> Arguments.of(format, kind)));
  }

  @ParameterizedTest
  @MethodSource("malformedMetadata")
  void malformedOrUnsupportedTiffFallsBackToTheOriginalOrientation(String format, String kind)
      throws Exception {
    byte[] metadata = tiff(ByteOrder.LITTLE_ENDIAN, 2);
    ByteBuffer buffer = ByteBuffer.wrap(metadata).order(ByteOrder.LITTLE_ENDIAN);
    switch (kind) {
      case "empty" -> metadata = new byte[0];
      case "short-header" -> metadata = Arrays.copyOf(metadata, 7);
      case "byte-order" -> buffer.putShort(0, (short) 0);
      case "magic" -> buffer.putShort(2, (short) 43);
      case "unsigned-offset" -> buffer.putInt(4, -1);
      case "last-byte-offset" -> buffer.putInt(4, metadata.length - 1);
      case "empty-directory" -> buffer.putShort(8, (short) 0);
      case "short-entry" -> metadata = Arrays.copyOf(metadata, 21);
      case "huge-directory" -> {
        buffer.putShort(8, (short) -1);
        buffer.putShort(10, (short) 0);
      }
      case "wrong-tag" -> buffer.putShort(10, (short) 0x113);
      case "wrong-type" -> buffer.putShort(12, (short) 4);
      case "wrong-count" -> buffer.putInt(14, 2);
      case "zero" -> buffer.putShort(18, (short) 0);
      case "nine" -> buffer.putShort(18, (short) 9);
      default -> throw new IllegalArgumentException("Unknown TIFF fixture: " + kind);
    }
    byte[] image = withMetadata(format, metadata);
    processor.validate(image);
    assertCorners(processor.normalize(image, Crop.centered()), SOURCE);
  }

  @ParameterizedTest
  @ValueSource(strings = {"png", "jpeg"})
  void usesOnlyTheFirstExifBlockEvenIfItsTiffIsInvalid(String format) throws Exception {
    byte[] second = withMetadata(format, tiff(ByteOrder.BIG_ENDIAN, 6));
    byte[] first = insertMetadata(format, second, new byte[0]);
    assertCorners(processor.normalize(first, Crop.centered()), SOURCE);
  }

  @ParameterizedTest
  @ValueSource(strings = {"png", "jpeg"})
  void preservesTheFirstOrientationWhenAnotherExifBlockFollows(String format) throws Exception {
    byte[] second = withMetadata(format, tiff(ByteOrder.BIG_ENDIAN, 6));
    byte[] first = insertMetadata(format, second, tiff(ByteOrder.LITTLE_ENDIAN, 2));
    assertCorners(processor.normalize(first, Crop.centered()), expectedCorners(2));
  }

  @Test
  void animatedPngIsStillRejectedAfterAnExifBlock() throws Exception {
    byte[] png = withMetadata("png", tiff(ByteOrder.LITTLE_ENDIAN, 2));
    byte[] animated = insertPngChunk(png, 33 + 26 + 12, "acTL", new byte[8]);
    assertEquals(
        Problem.TYPE,
        assertThrows(InvalidImage.class, () -> processor.validate(animated)).problem());
  }

  @Test
  void pngSkipsUnknownAndEmptyChunksBeforeFindingExif() throws Exception {
    byte[] png = withMetadata("png", tiff(ByteOrder.LITTLE_ENDIAN, 4));
    png = insertPngChunk(png, 33, "vpAg", new byte[0]);
    png = insertPngChunk(png, 33, "vpBg", new byte[] {1, 2, 3});
    assertCorners(processor.normalize(png, Crop.centered()), expectedCorners(4));
  }

  @Test
  void jpegSkipsFillBytesAndNonExifApplicationSegments() throws Exception {
    byte[] jpeg = withMetadata("jpeg", tiff(ByteOrder.LITTLE_ENDIAN, 3));
    try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      output.write(jpeg, 0, 2);
      output.write(new byte[] {(byte) 255, (byte) 255, (byte) 225, 0, 8});
      output.write("NotExif".getBytes(StandardCharsets.US_ASCII), 0, 6);
      output.write(jpeg, 2, jpeg.length - 2);
      assertCorners(processor.normalize(output.toByteArray(), Crop.centered()), expectedCorners(3));
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, Integer.MAX_VALUE, 100000})
  void oversizedPngChunkDoesNotEscapeIntoFollowingExif(int declaredLength) throws Exception {
    byte[] png = withMetadata("png", tiff(ByteOrder.LITTLE_ENDIAN, 2));
    ByteBuffer.wrap(png).putInt(33, declaredLength);
    Crop crop = Crop.centered();
    assertEquals(
        Problem.IMAGE,
        assertThrows(InvalidImage.class, () -> processor.normalize(png, crop)).problem());
  }

  private static List<Color> expectedCorners(int orientation) {
    return switch (orientation) {
      case 1 -> SOURCE;
      case 2 -> List.of(Color.GREEN, Color.RED, Color.YELLOW, Color.BLUE);
      case 3 -> List.of(Color.YELLOW, Color.BLUE, Color.GREEN, Color.RED);
      case 4 -> List.of(Color.BLUE, Color.YELLOW, Color.RED, Color.GREEN);
      case 5 -> List.of(Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW);
      case 6 -> List.of(Color.BLUE, Color.RED, Color.YELLOW, Color.GREEN);
      case 7 -> List.of(Color.YELLOW, Color.GREEN, Color.BLUE, Color.RED);
      case 8 -> List.of(Color.GREEN, Color.YELLOW, Color.RED, Color.BLUE);
      default -> throw new IllegalArgumentException("Invalid orientation fixture");
    };
  }

  private static void assertCorners(byte[] normalized, List<Color> expected) throws IOException {
    try (ByteArrayInputStream input = new ByteArrayInputStream(normalized)) {
      BufferedImage image = ImageIO.read(input);
      try {
        assertEquals(512, image.getWidth());
        assertEquals(512, image.getHeight());
        for (int corner = 0; corner < 4; corner++) {
          Color actual =
              new Color(image.getRGB(corner % 2 == 0 ? 128 : 384, corner < 2 ? 128 : 384));
          Color color = expected.get(corner);
          int distance =
              Math.abs(actual.getRed() - color.getRed())
                  + Math.abs(actual.getGreen() - color.getGreen())
                  + Math.abs(actual.getBlue() - color.getBlue());
          assertTrue(distance < 40, "Unexpected color at corner " + corner + ": " + actual);
        }
      } finally {
        image.flush();
      }
    }
    String encoded = new String(normalized, StandardCharsets.ISO_8859_1);
    assertFalse(encoded.contains("Exif"));
    assertFalse(encoded.contains("eXIf"));
  }

  private static byte[] tiff(ByteOrder order, int orientation) {
    return ByteBuffer.allocate(26)
        .order(order)
        .putShort(order == ByteOrder.LITTLE_ENDIAN ? (short) 0x4949 : (short) 0x4d4d)
        .putShort((short) 42)
        .putInt(8)
        .putShort((short) 1)
        .putShort((short) 0x112)
        .putShort((short) 3)
        .putInt(1)
        .putShort((short) orientation)
        .putShort((short) 0)
        .putInt(0)
        .array();
  }

  private static byte[] withMetadata(String format, byte[] metadata) throws IOException {
    BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
    try {
      Graphics2D graphics = image.createGraphics();
      try {
        for (int corner = 0; corner < 4; corner++) {
          graphics.setColor(SOURCE.get(corner));
          graphics.fillRect(corner % 2 * 32, corner / 2 * 32, 32, 32);
        }
      } finally {
        graphics.dispose();
      }
      try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
        ImageIO.write(image, format, output);
        return insertMetadata(format, output.toByteArray(), metadata);
      }
    } finally {
      image.flush();
    }
  }

  private static byte[] insertMetadata(String format, byte[] image, byte[] metadata)
      throws IOException {
    if (format.equals("png")) {
      return insertPngChunk(image, 33, "eXIf", metadata);
    }
    try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      output.write(image, 0, 2);
      output.write(
          ByteBuffer.allocate(4)
              .putShort((short) 0xffe1)
              .putShort((short) (metadata.length + 8))
              .array());
      output.write(new byte[] {'E', 'x', 'i', 'f', 0, 0});
      output.write(metadata);
      output.write(image, 2, image.length - 2);
      return output.toByteArray();
    }
  }

  private static byte[] insertPngChunk(byte[] image, int offset, String type, byte[] payload)
      throws IOException {
    byte[] chunk =
        ByteBuffer.allocate(payload.length + 12)
            .putInt(payload.length)
            .put(type.getBytes(StandardCharsets.US_ASCII))
            .put(payload)
            .putInt(0)
            .array();
    CRC32 checksum = new CRC32();
    checksum.update(chunk, 4, payload.length + 4);
    ByteBuffer.wrap(chunk).putInt(payload.length + 8, (int) checksum.getValue());
    try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      output.write(image, 0, offset);
      output.write(chunk);
      output.write(image, offset, image.length - offset);
      return output.toByteArray();
    }
  }
}
