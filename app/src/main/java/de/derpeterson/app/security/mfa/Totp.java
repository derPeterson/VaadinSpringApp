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
    byte[] secretBytes = new byte[20];
    RANDOM.nextBytes(secretBytes);
    return secretBytes;
  }

  public static String base32(byte[] secretBytes) {
    requireSecret(secretBytes);
    StringBuilder base32TextBuilder = new StringBuilder((secretBytes.length * 8 + 4) / 5);
    int pendingBitCount = 0;
    int bitBuffer = 0;
    for (byte secretByte : secretBytes) {
      bitBuffer = (bitBuffer << 8) | (secretByte & 255);
      pendingBitCount += 8;
      while (pendingBitCount >= 5) {
        pendingBitCount -= 5;
        base32TextBuilder.append(BASE32[(bitBuffer >>> pendingBitCount) & 31]);
      }
    }

    if (pendingBitCount > 0) {
      base32TextBuilder.append(BASE32[(bitBuffer << (5 - pendingBitCount)) & 31]);
    }

    return base32TextBuilder.toString();
  }

  public static String code(byte[] secretBytes, long timeStep) {
    requireSecret(secretBytes);
    if (timeStep < 0) {
      throw new IllegalArgumentException("Negative TOTP step");
    }

    try {
      Mac hmac = Mac.getInstance("HmacSHA1");
      hmac.init(new SecretKeySpec(secretBytes, "HmacSHA1"));
      byte[] hmacBytes = hmac.doFinal(ByteBuffer.allocate(Long.BYTES).putLong(timeStep).array());

      int truncationOffset = hmacBytes[hmacBytes.length - 1] & 15;
      int truncatedCodeValue =
          ByteBuffer.wrap(hmacBytes, truncationOffset, Integer.BYTES).getInt() & Integer.MAX_VALUE;

      Arrays.fill(hmacBytes, (byte) 0);
      return String.format(Locale.ROOT, "%06d", truncatedCodeValue % 1_000_000);
    } catch (GeneralSecurityException failure) {
      throw new IllegalStateException("TOTP algorithm unavailable", failure);
    }
  }

  /**
   * Liefert nur einen noch nicht verbrauchten Zeitschritt. Der Aufrufer speichert ihn unter
   * Kontolock.
   */
  public static long verify(
      byte[] secretBytes, String submittedCode, Instant verificationTime, long lastAcceptedStep) {
    // Genau sechs ASCII-Ziffern. Leerzeichen, Unicode-Ziffern und variable Längen sind unzulässig.
    if (submittedCode == null || !submittedCode.matches("\\d{6}")) {
      return -1;
    }

    long currentTimeStep = verificationTime.getEpochSecond() / PERIOD_SECONDS;
    byte[] submittedCodeBytes = submittedCode.getBytes(StandardCharsets.US_ASCII);
    long matchedTimeStep = -1;

    for (long timeStep = Math.max(0, currentTimeStep - 1);
        timeStep <= currentTimeStep + 1;
        timeStep++) {
      boolean codeMatches =
          MessageDigest.isEqual(
              submittedCodeBytes, code(secretBytes, timeStep).getBytes(StandardCharsets.US_ASCII));
      if (codeMatches && timeStep > lastAcceptedStep) {
        matchedTimeStep = timeStep;
      }
    }

    return matchedTimeStep;
  }

  private static void requireSecret(byte[] secretBytes) {
    if (secretBytes == null || secretBytes.length < 20 || secretBytes.length > 64) {
      throw new IllegalArgumentException("Invalid TOTP secret size");
    }
  }
}
