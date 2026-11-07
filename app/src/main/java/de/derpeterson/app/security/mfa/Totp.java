package de.derpeterson.app.security.mfa;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** RFC 6238: SHA-1, sechs Ziffern und 30 Sekunden; mit üblichen Authenticator-Apps kompatibel. */
public final class Totp {
  public static final int PERIOD_SECONDS = 30;
  private static final char[] BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
  private static final SecureRandom RANDOM = new SecureRandom();

  private Totp() {}

  public static byte[] newSecret() {
    byte[] secret = new byte[20];
    RANDOM.nextBytes(secret);
    return secret;
  }

  public static String base32(byte[] secret) {
    requireSecret(secret);
    StringBuilder encoded = new StringBuilder((secret.length * 8 + 4) / 5);
    int bits = 0;
    int buffer = 0;
    for (byte value : secret) {
      buffer = (buffer << 8) | (value & 255);
      bits += 8;
      while (bits >= 5) {
        bits -= 5;
        encoded.append(BASE32[(buffer >>> bits) & 31]);
      }
    }
    if (bits > 0) {
      encoded.append(BASE32[(buffer << (5 - bits)) & 31]);
    }
    return encoded.toString();
  }

  public static String code(byte[] secret, long step) {
    requireSecret(secret);
    if (step < 0) {
      throw new IllegalArgumentException("Negative TOTP step");
    }
    try {
      Mac mac = Mac.getInstance("HmacSHA1");
      mac.init(new SecretKeySpec(secret, "HmacSHA1"));
      byte[] digest = mac.doFinal(ByteBuffer.allocate(Long.BYTES).putLong(step).array());
      int offset = digest[digest.length - 1] & 15;
      int binary = ByteBuffer.wrap(digest, offset, Integer.BYTES).getInt() & Integer.MAX_VALUE;
      Arrays.fill(digest, (byte) 0);
      return String.format(Locale.ROOT, "%06d", binary % 1_000_000);
    } catch (GeneralSecurityException failure) {
      throw new IllegalStateException("TOTP algorithm unavailable", failure);
    }
  }

  /**
   * Liefert nur einen noch nicht verbrauchten Zeitschritt. Der Aufrufer speichert ihn unter
   * Kontolock.
   */
  public static long verify(byte[] secret, String input, Instant now, long lastAcceptedStep) {
    // Genau sechs ASCII-Ziffern. Leerzeichen, Unicode-Ziffern und variable Längen sind unzulässig.
    if (input == null || !input.matches("\\d{6}")) {
      return -1;
    }
    long current = now.getEpochSecond() / PERIOD_SECONDS;
    byte[] candidate = input.getBytes(StandardCharsets.US_ASCII);
    long matched = -1;
    for (long step = Math.max(0, current - 1); step <= current + 1; step++) {
      boolean equal =
          MessageDigest.isEqual(candidate, code(secret, step).getBytes(StandardCharsets.US_ASCII));
      if (equal && step > lastAcceptedStep) {
        matched = step;
      }
    }
    return matched;
  }

  private static void requireSecret(byte[] secret) {
    if (secret == null || secret.length < 20 || secret.length > 64) {
      throw new IllegalArgumentException("Invalid TOTP secret size");
    }
  }
}
