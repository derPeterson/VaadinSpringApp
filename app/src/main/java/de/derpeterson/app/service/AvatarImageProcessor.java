package de.derpeterson.app.service;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Iterator;
import java.util.concurrent.Semaphore;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import org.springframework.stereotype.Component;

/** Begrenztes Raster-Decoding; Metadaten und Originalname werden nie übernommen. */
@Component
public class AvatarImageProcessor {
  public static final int MAX_UPLOAD = 5 * 1024 * 1024;
  public static final int MAX_RESULT = 1024 * 1024;
  private final Semaphore decoders = new Semaphore(2);

  public record Crop(double zoom, double x, double y) implements Serializable {
    public static Crop centered() {
      return new Crop(1, 0, 0);
    }
  }

  public enum Problem {
    TYPE,
    SIZE,
    OUTPUT,
    IMAGE,
    DIMENSIONS,
    CROP,
    BUSY,
    EXPIRED,
    MISSING
  }

  public static class InvalidImage extends IllegalArgumentException {
    private final Problem problem;

    public InvalidImage(Problem problem) {
      super(problem.name());
      this.problem = problem;
    }

    public Problem problem() {
      return problem;
    }
  }

  public static byte[] readLimited(InputStream input) throws IOException {
    byte[] bytes = input.readNBytes(MAX_UPLOAD + 1);
    if (bytes.length > MAX_UPLOAD) {
      throw new InvalidImage(Problem.SIZE);
    }
    return bytes;
  }

  public void validate(byte[] data) {
    process(data, Crop.centered(), false);
  }

  public byte[] normalize(byte[] data, Crop crop) {
    return process(data, crop, true);
  }

  // Nur Position und Länge im bereits begrenzten Upload; keine Metadatenkopie.
  private record ExifBlock(int offset, int length) {}

  private byte[] process(byte[] data, Crop crop, boolean encode) {
    boolean png = requireImageType(data);
    ExifBlock pngExif = png ? inspectPng(data) : null;
    if (!decoders.tryAcquire()) {
      throw new InvalidImage(Problem.BUSY);
    }
    try (MemoryCacheImageInputStream input =
        new MemoryCacheImageInputStream(new ByteArrayInputStream(data))) {
      ImageReader reader = requireImageReader(input);
      try {
        BufferedImage image = readRaster(reader, input, png);
        try {
          if (!encode) {
            return new byte[0];
          }
          int orientation = png ? pngOrientation(data, pngExif) : jpegOrientation(data);
          return cropAndEncode(image, crop, orientation);
        } finally {
          if (image != null) {
            image.flush();
          }
        }
      } finally {
        reader.dispose();
      }
    } catch (InvalidImage failure) {
      throw failure;
    } catch (IOException | RuntimeException _) {
      throw new InvalidImage(Problem.IMAGE);
    } finally {
      decoders.release();
    }
  }

  private boolean requireImageType(byte[] data) {
    if (data == null || data.length == 0) {
      throw new InvalidImage(Problem.IMAGE);
    }
    if (data.length > MAX_UPLOAD) {
      throw new InvalidImage(Problem.SIZE);
    }
    boolean png = data.length >= 8 && ByteBuffer.wrap(data).getLong() == 0x89504e470d0a1a0aL;
    boolean jpeg =
        data.length >= 3
            && (data[0] & 255) == 255
            && (data[1] & 255) == 216
            && (data[2] & 255) == 255;
    if (!png && !jpeg) {
      throw new InvalidImage(Problem.TYPE);
    }
    return png;
  }

  private ImageReader requireImageReader(MemoryCacheImageInputStream input) {
    Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
    if (!readers.hasNext()) {
      throw new InvalidImage(Problem.IMAGE);
    }
    return readers.next();
  }

  private BufferedImage readRaster(
      ImageReader reader, MemoryCacheImageInputStream input, boolean png) throws IOException {
    String format = reader.getFormatName();
    if (!(png && "png".equalsIgnoreCase(format) || !png && "jpeg".equalsIgnoreCase(format))) {
      throw new InvalidImage(Problem.TYPE);
    }
    reader.setInput(input, true, true);
    int width = reader.getWidth(0);
    int height = reader.getHeight(0);
    // Header vor dem vollständigen Decode prüfen; die Vorschau allein ist kein Sicherheitsfilter.
    if (width < 1
        || height < 1
        || width > 8192
        || height > 8192
        || (long) width * height > 20_000_000) {
      throw new InvalidImage(Problem.DIMENSIONS);
    }
    return reader.read(0);
  }

  private byte[] cropAndEncode(BufferedImage image, Crop crop, int orientation) throws IOException {
    int width = image.getWidth();
    int height = image.getHeight();
    int orientedWidth = orientation >= 5 ? height : width;
    int orientedHeight = orientation >= 5 ? width : height;
    checkCrop(crop, orientedWidth, orientedHeight);
    double side = Math.min(orientedWidth, orientedHeight) / crop.zoom();
    double left = (orientedWidth - side) / 2 - crop.x() * side;
    double top = (orientedHeight - side) / 2 - crop.y() * side;
    BufferedImage result = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
    try {
      Graphics2D graphics = result.createGraphics();
      try {
        graphics.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.scale(512 / side, 512 / side);
        graphics.translate(-left, -top);
        graphics.transform(transform(orientation, width, height));
        graphics.drawImage(image, 0, 0, null);
      } finally {
        graphics.dispose();
      }
      return encodePng(result);
    } finally {
      result.flush();
    }
  }

  private byte[] encodePng(BufferedImage image) throws IOException {
    try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      try (FilterOutputStream bounded =
          new FilterOutputStream(output) {
            private int count;

            @Override
            public void write(int value) throws IOException {
              limit(1);
              out.write(value);
            }

            @Override
            public void write(byte[] bytes, int offset, int length) throws IOException {
              limit(length);
              out.write(bytes, offset, length);
            }

            private void limit(int length) {
              count += length;
              if (count > MAX_RESULT) {
                throw new InvalidImage(Problem.OUTPUT);
              }
            }
          }) {
        try (MemoryCacheImageOutputStream encoded = new MemoryCacheImageOutputStream(bounded)) {
          ImageIO.write(image, "png", encoded);
          encoded.flush();
        }
      }
      return output.toByteArray();
    }
  }

  private void checkCrop(Crop crop, int w, int h) {
    if (crop == null
        || !Double.isFinite(crop.zoom())
        || !Double.isFinite(crop.x())
        || !Double.isFinite(crop.y())
        || crop.zoom() < 1
        || crop.zoom() > 3) {
      throw new InvalidImage(Problem.CROP);
    }
    double shortSide = Math.min(w, h);
    if (Math.abs(crop.x()) > (w / shortSide * crop.zoom() - 1) / 2 + 0.000001
        || Math.abs(crop.y()) > (h / shortSide * crop.zoom() - 1) / 2 + 0.000001) {
      throw new InvalidImage(Problem.CROP);
    }
  }

  // Ein double-Literal wählt die double-Matrix statt der float-Überladung.
  private AffineTransform transform(int o, int w, int h) {
    return switch (o) {
      case 2 -> new AffineTransform(-1.0, 0, 0, 1, w, 0);
      case 3 -> new AffineTransform(-1.0, 0, 0, -1, w, h);
      case 4 -> new AffineTransform(1.0, 0, 0, -1, 0, h);
      case 5 -> new AffineTransform(0.0, 1, 1, 0, 0, 0);
      case 6 -> new AffineTransform(0.0, 1, -1, 0, h, 0);
      case 7 -> new AffineTransform(0.0, -1, -1, 0, h, w);
      case 8 -> new AffineTransform(0.0, -1, 1, 0, 0, w);
      default -> new AffineTransform();
    };
  }

  // PNG einmal durchlaufen: APNG weiterhin vor dem Decode ablehnen und nur den ersten
  // eXIf-Block merken. TIFF wird wie bisher erst bei der Normalisierung ausgewertet.
  private ExifBlock inspectPng(byte[] data) {
    int position = 8;
    ExifBlock exif = null;
    while (position <= data.length - 12) {
      long length = Integer.toUnsignedLong(ByteBuffer.wrap(data, position, 4).getInt());
      if (length > data.length - position - 12) {
        return exif;
      }
      int type = ByteBuffer.wrap(data, position + 4, 4).getInt();
      if (type == 0x6163544c) {
        throw new InvalidImage(Problem.TYPE);
      }
      if (type == 0x65584966 && exif == null) {
        exif = new ExifBlock(position + 8, (int) length);
      }
      // Erst nach der Restlängenprüfung in int umwandeln; auch leere Chunks rücken vor.
      position += (int) length + 12;
    }
    return exif;
  }

  private int pngOrientation(byte[] data, ExifBlock exif) {
    return exif == null ? 1 : tiff(data, exif.offset(), exif.length());
  }

  // Nur Metadaten vor SOS lesen. Füllbytes und Marker ohne Längenfeld verbrauchen
  // immer Eingabebytes; ungültige Blockgrenzen beenden die Suche mit Orientierung 1.
  private int jpegOrientation(byte[] data) {
    int position = 2;
    while (position < data.length - 3) {
      if ((data[position] & 255) != 255) {
        return 1;
      }
      int marker = data[position + 1] & 255;
      position += 2;
      if (marker == 255) {
        position--;
      } else if (marker == 218 || marker == 217) {
        return 1;
      } else if (!isStandaloneJpegMarker(marker)) {
        int length = (data[position] & 255) * 256 + (data[position + 1] & 255);
        if (length < 2 || length > data.length - position) {
          return 1;
        }
        if (isJpegExif(data, position, length, marker)) {
          return tiff(data, position + 8, length - 8);
        }
        position += length;
      }
    }
    return 1;
  }

  private boolean isStandaloneJpegMarker(int marker) {
    return marker == 1 || marker >= 208 && marker <= 215;
  }

  private boolean isJpegExif(byte[] data, int position, int length, int marker) {
    return marker == 225
        && length >= 8
        && data[position + 2] == 'E'
        && data[position + 3] == 'x'
        && data[position + 4] == 'i'
        && data[position + 5] == 'f'
        && data[position + 6] == 0
        && data[position + 7] == 0;
  }

  // JPEG-APP1 und PNG-eXIf enthalten dieselbe TIFF-Struktur. Alle Offsets bleiben
  // relativ zum begrenzten Block; Metadaten werden nicht in das Ergebnis kopiert.
  private int tiff(byte[] data, int start, int length) {
    if (length < 8) {
      return 1;
    }
    ByteBuffer b = ByteBuffer.wrap(data, start, length).slice();
    if (b.getShort(0) == 0x4949) {
      b.order(ByteOrder.LITTLE_ENDIAN);
    } else if (b.getShort(0) != 0x4d4d) {
      return 1;
    }
    if (b.getShort(2) != 42) {
      return 1;
    }
    long offset = Integer.toUnsignedLong(b.getInt(4));
    if (offset > length - 2) {
      return 1;
    }
    int count = Short.toUnsignedInt(b.getShort((int) offset));
    for (int i = 0; i < count; i++) {
      long pos = offset + 2 + (long) i * 12;
      if (pos > length - 12) {
        return 1;
      }
      int p = (int) pos;
      if (Short.toUnsignedInt(b.getShort(p)) == 0x112
          && b.getShort(p + 2) == 3
          && b.getInt(p + 4) == 1) {
        int value = Short.toUnsignedInt(b.getShort(p + 8));
        return value >= 1 && value <= 8 ? value : 1;
      }
    }
    return 1;
  }
}
