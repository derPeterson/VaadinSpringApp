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
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AvatarImageProcessorTest {
  private final AvatarImageProcessor processor = new AvatarImageProcessor();

  static byte[] image(String format, int width, int height) throws IOException {
    BufferedImage image =
        new BufferedImage(
            width,
            height,
            format.equals("png") ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
    Graphics2D g = image.createGraphics();
    g.setColor(Color.RED);
    g.fillRect(0, 0, width / 2, height);
    g.setColor(Color.BLUE);
    g.fillRect(width / 2, 0, width, height);
    g.dispose();
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    ImageIO.write(image, format, output);
    image.flush();
    return output.toByteArray();
  }

  @ParameterizedTest
  @ValueSource(strings = {"png", "jpeg"})
  void normalizesBothFormatsAndAppliesTheSelectedCrop(String format) throws Exception {
    byte[] original = image(format, 200, 100);
    byte[] bytes = processor.normalize(original, new Crop(1, 0.5, 0));
    BufferedImage normalized = ImageIO.read(new ByteArrayInputStream(bytes));
    assertEquals(512, normalized.getWidth());
    assertEquals(512, normalized.getHeight());
    assertTrue(bytes.length <= AvatarImageProcessor.MAX_RESULT);
    assertTrue(new Color(normalized.getRGB(256, 256)).getRed() > 200);
    assertFalse(new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1).contains("Exif"));
  }

  @Test
  void rejectsInvalidFormatsCorruptionAndCoordinates() throws Exception {
    assertEquals(
        Problem.TYPE,
        assertThrows(
                InvalidImage.class, () -> processor.normalize("<svg/>".getBytes(), Crop.centered()))
            .problem());
    assertEquals(
        Problem.IMAGE,
        assertThrows(
                InvalidImage.class,
                () ->
                    processor.normalize(
                        new byte[] {(byte) 255, (byte) 216, (byte) 255}, Crop.centered()))
            .problem());
    byte[] bytes = image("png", 20, 20);
    for (Crop crop :
        new Crop[] {
          new Crop(Double.NaN, 0, 0),
          new Crop(4, 0, 0),
          new Crop(1, 1, 0),
          new Crop(1, 0, Double.POSITIVE_INFINITY)
        }) {
      assertEquals(
          Problem.CROP,
          assertThrows(InvalidImage.class, () -> processor.normalize(bytes, crop)).problem());
    }
  }

  @Test
  void checksPixelDimensionsBeforeDecodingPixels() throws Exception {
    byte[] bytes = image("png", 2, 2);
    ByteBuffer.wrap(bytes, 16, 8).putInt(8193).putInt(2);
    CRC32 crc = new CRC32();
    crc.update(bytes, 12, 17);
    ByteBuffer.wrap(bytes, 29, 4).putInt((int) crc.getValue());
    assertEquals(
        Problem.DIMENSIONS,
        assertThrows(InvalidImage.class, () -> processor.validate(bytes)).problem());
  }

  @Test
  void limitsActualBytesEvenWithoutAContentLength() {
    var input =
        new InputStream() {
          int read;

          @Override
          public int read() {
            return ++read <= AvatarImageProcessor.MAX_UPLOAD + 100 ? 0 : -1;
          }
        };
    assertEquals(
        Problem.SIZE,
        assertThrows(InvalidImage.class, () -> AvatarImageProcessor.readLimited(input)).problem());
  }

  @ParameterizedTest
  @ValueSource(ints = {2, 3, 4, 5, 6, 7, 8})
  void honorsExifOrientationAndDoesNotKeepExif(int orientation) throws Exception {
    byte[] original = image("jpeg", 120, 60);
    ByteBuffer tiff = ByteBuffer.allocate(26).order(java.nio.ByteOrder.LITTLE_ENDIAN);
    tiff.putShort((short) 0x4949)
        .putShort((short) 42)
        .putInt(8)
        .putShort((short) 1)
        .putShort((short) 0x112)
        .putShort((short) 3)
        .putInt(1)
        .putShort((short) orientation)
        .putShort((short) 0)
        .putInt(0);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    out.write(original, 0, 2);
    out.write(new byte[] {(byte) 255, (byte) 225, 0, 34});
    out.write(new byte[] {'E', 'x', 'i', 'f', 0, 0});
    out.write(tiff.array());
    out.write(original, 2, original.length - 2);
    byte[] bytes = processor.normalize(out.toByteArray(), Crop.centered());
    BufferedImage result = ImageIO.read(new ByteArrayInputStream(bytes));
    boolean swapped = orientation >= 5;
    boolean reversed = orientation == 2 || orientation == 3 || orientation == 7 || orientation == 8;
    int first = result.getRGB(swapped ? 256 : 80, swapped ? 80 : 256);
    Color color = new Color(first);
    assertTrue(reversed ? color.getBlue() > 180 : color.getRed() > 180);
    assertFalse(new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1).contains("Exif"));
  }

  @Test
  void preservesPngTransparency() throws Exception {
    BufferedImage raw = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ImageIO.write(raw, "png", out);
    BufferedImage result =
        ImageIO.read(
            new ByteArrayInputStream(processor.normalize(out.toByteArray(), Crop.centered())));
    assertEquals(0, (result.getRGB(256, 256) >>> 24));
  }

  @Test
  void animatedPngIsRejectedBeforeDecoding() throws Exception {
    byte[] png = image("png", 2, 2);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    output.write(png, 0, 33);
    byte[] chunk =
        ByteBuffer.allocate(20).putInt(8).putInt(0x6163544c).putInt(2).putInt(0).putInt(0).array();
    CRC32 crc = new CRC32();
    crc.update(chunk, 4, 12);
    ByteBuffer.wrap(chunk, 16, 4).putInt((int) crc.getValue());
    output.write(chunk);
    output.write(png, 33, png.length - 33);
    assertEquals(
        Problem.TYPE,
        assertThrows(InvalidImage.class, () -> processor.validate(output.toByteArray())).problem());
  }
}
