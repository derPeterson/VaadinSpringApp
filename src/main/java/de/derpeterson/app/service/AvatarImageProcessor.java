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

  private byte[] process(byte[] data, Crop crop, boolean encode) {
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
    if (png) {
      // Animierte PNGs würden in der Vorschau andere Frames zeigen als der Rasterdecoder.
      for (int p = 8; p <= data.length - 12; ) {
        long length = Integer.toUnsignedLong(ByteBuffer.wrap(data, p, 4).getInt());
        if (length > data.length - p - 12) {
          break;
        }
        if (ByteBuffer.wrap(data, p + 4, 4).getInt() == 0x6163544c) {
          throw new InvalidImage(Problem.TYPE);
        }
        p += (int) length + 12;
      }
    }
    if (!decoders.tryAcquire()) {
      throw new InvalidImage(Problem.BUSY);
    }
    try (MemoryCacheImageInputStream input =
        new MemoryCacheImageInputStream(new ByteArrayInputStream(data))) {
      Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
      if (!readers.hasNext()) {
        throw new InvalidImage(Problem.IMAGE);
      }
      ImageReader reader = readers.next();
      try {
        String format = reader.getFormatName();
        if (!(png && "png".equalsIgnoreCase(format) || jpeg && "jpeg".equalsIgnoreCase(format))) {
          throw new InvalidImage(Problem.TYPE);
        }
        reader.setInput(input, true, true);
        int w = reader.getWidth(0);
        int h = reader.getHeight(0);
        if (w < 1 || h < 1 || w > 8192 || h > 8192 || (long) w * h > 20_000_000) {
          throw new InvalidImage(Problem.DIMENSIONS);
        }
        // Header vor dem vollständigen Decode prüfen; die Vorschau allein ist kein
        // Sicherheitsfilter.
        BufferedImage image = reader.read(0);
        try {
          if (!encode) {
            return new byte[0];
          }
          int orientation = orientation(data, png);
          int ow = orientation >= 5 ? h : w;
          int oh = orientation >= 5 ? w : h;
          checkCrop(crop, ow, oh);
          double side = Math.min(ow, oh) / crop.zoom();
          double left = (ow - side) / 2 - crop.x() * side;
          double top = (oh - side) / 2 - crop.y() * side;
          BufferedImage result = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
          Graphics2D graphics = result.createGraphics();
          try {
            graphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.scale(512 / side, 512 / side);
            graphics.translate(-left, -top);
            graphics.transform(transform(orientation, w, h));
            graphics.drawImage(image, 0, 0, null);
          } finally {
            graphics.dispose();
          }
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

                  private void limit(int n) {
                    if ((count += n) > MAX_RESULT) {
                      throw new InvalidImage(Problem.OUTPUT);
                    }
                  }
                }) {
              try (MemoryCacheImageOutputStream encoded =
                  new MemoryCacheImageOutputStream(bounded)) {
                ImageIO.write(result, "png", encoded);
                encoded.flush();
              }
            }
            return output.toByteArray();
          } finally {
            result.flush();
          }
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

  private AffineTransform transform(int o, int w, int h) {
    return switch (o) {
      case 2 -> new AffineTransform(-1, 0, 0, 1, w, 0);
      case 3 -> new AffineTransform(-1, 0, 0, -1, w, h);
      case 4 -> new AffineTransform(1, 0, 0, -1, 0, h);
      case 5 -> new AffineTransform(0, 1, 1, 0, 0, 0);
      case 6 -> new AffineTransform(0, 1, -1, 0, h, 0);
      case 7 -> new AffineTransform(0, -1, -1, 0, h, w);
      case 8 -> new AffineTransform(0, -1, 1, 0, 0, w);
      default -> new AffineTransform();
    };
  }

  // JPEG-APP1 und PNG-eXIf enthalten dieselbe TIFF-Struktur. Nur den Orientation-Wert
  // lesen, alle Offsets gegen den jeweiligen Block prüfen; keine Metadaten kopieren.
  private int orientation(byte[] data, boolean png) {
    if (png) {
      for (int p = 8; p <= data.length - 12; ) {
        long length = Integer.toUnsignedLong(ByteBuffer.wrap(data, p, 4).getInt());
        if (length > data.length - p - 12) {
          break;
        }
        if (ByteBuffer.wrap(data, p + 4, 4).getInt() == 0x65584966) {
          return tiff(data, p + 8, (int) length);
        }
        p += (int) length + 12;
      }
    } else {
      int p = 2;
      while (p < data.length - 3) {
        if ((data[p++] & 255) != 255) {
          break;
        }
        int marker = data[p++] & 255;
        if (marker == 255) {
          p--;
          continue;
        }
        if (marker == 218 || marker == 217) {
          break;
        }
        if (marker == 1 || marker >= 208 && marker <= 215) {
          continue;
        }
        int length = (data[p] & 255) * 256 + (data[p + 1] & 255);
        if (length < 2 || length > data.length - p) {
          break;
        }
        if (marker == 225
            && length >= 8
            && data[p + 2] == 'E'
            && data[p + 3] == 'x'
            && data[p + 4] == 'i'
            && data[p + 5] == 'f'
            && data[p + 6] == 0
            && data[p + 7] == 0) {
          return tiff(data, p + 8, length - 8);
        }
        p += length;
      }
    }
    return 1;
  }

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
