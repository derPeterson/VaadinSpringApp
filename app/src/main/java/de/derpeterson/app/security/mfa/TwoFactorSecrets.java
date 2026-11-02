package de.derpeterson.app.security.mfa;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.IntStream;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Geheimnisse verlassen diesen Baustein nur kurzzeitig im Arbeitsspeicher. Kein Schlüssel in der
 * DB.
 */
@Component
public final class TwoFactorSecrets {
  public static final String KEY_PROPERTY = "app.security.totp-encryption-key";
  private final byte[] key;
  private final SecureRandom random = new SecureRandom();

  public TwoFactorSecrets(Environment environment) {
    String configured = environment.getProperty(KEY_PROPERTY, "").trim();
    if (configured.isEmpty()) {
      key = null;
      return;
    }
    try {
      key = Base64.getDecoder().decode(configured);
    } catch (IllegalArgumentException failure) {
      throw new IllegalStateException("Invalid TOTP encryption key encoding");
    }
    if (key.length != 32) {
      throw new IllegalStateException("TOTP encryption key must contain 32 random bytes");
    }
  }

  public boolean available() {
    return key != null;
  }

  public String emailVerifier(Long owner, UUID challenge, String contact, String code) {
    requireAvailable();
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(key, "HmacSHA256"));
      // Ein kurzer Mailcode darf auch bei lesender Datenbankkenntnis nicht offline erraten werden.
      return HexFormat.of()
          .formatHex(
              mac.doFinal(
                  ("mfa-email:1:" + owner + ":" + challenge + ":" + contact + ":" + code)
                      .getBytes(StandardCharsets.UTF_8)));
    } catch (GeneralSecurityException failure) {
      throw new IllegalStateException("Cannot protect enrollment email verification", failure);
    }
  }

  public String encrypt(Long userId, String purpose, byte[] plaintext) {
    requireAvailable();
    byte[] nonce = new byte[12];
    random.nextBytes(nonce);
    try {
      Cipher cipher = cipher(Cipher.ENCRYPT_MODE, userId, purpose, nonce);
      byte[] ciphertext = cipher.doFinal(plaintext);
      byte[] envelope = new byte[1 + nonce.length + ciphertext.length];
      envelope[0] = 1;
      System.arraycopy(nonce, 0, envelope, 1, nonce.length);
      System.arraycopy(ciphertext, 0, envelope, 13, ciphertext.length);
      return Base64.getUrlEncoder().withoutPadding().encodeToString(envelope);
    } catch (GeneralSecurityException failure) {
      throw new IllegalStateException("TOTP encryption failed", failure);
    }
  }

  public byte[] decrypt(Long userId, String purpose, String encrypted) {
    requireAvailable();
    try {
      byte[] envelope = Base64.getUrlDecoder().decode(encrypted);
      if (envelope.length < 30 || envelope.length > 4096 || envelope[0] != 1) {
        throw new IllegalArgumentException("Invalid encrypted TOTP envelope");
      }
      Cipher cipher =
          cipher(Cipher.DECRYPT_MODE, userId, purpose, Arrays.copyOfRange(envelope, 1, 13));
      return cipher.doFinal(envelope, 13, envelope.length - 13);
    } catch (GeneralSecurityException | IllegalArgumentException failure) {
      // Weder Ciphertext noch Klartext in Fehlermeldungen aufnehmen.
      throw new IllegalStateException("TOTP secret unavailable", failure);
    }
  }

  private Cipher cipher(int mode, Long userId, String purpose, byte[] nonce)
      throws GeneralSecurityException {
    if (userId == null || userId < 1 || purpose == null || purpose.isBlank()) {
      throw new IllegalArgumentException("Secret owner and purpose required");
    }
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(mode, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
    cipher.updateAAD(("mfa:1:" + userId + ":" + purpose).getBytes(StandardCharsets.UTF_8));
    return cipher;
  }

  private void requireAvailable() {
    if (!available()) {
      throw new IllegalStateException("TOTP encryption key is not configured");
    }
  }

  public List<String> newRecoveryCodes() {
    return IntStream.range(0, 10).mapToObj(index -> randomCode()).toList();
  }

  private String randomCode() {
    byte[] bytes = new byte[16];
    random.nextBytes(bytes);
    String hex = HexFormat.of().withUpperCase().formatHex(bytes);
    return String.join(
        "-", hex.substring(0, 8), hex.substring(8, 16), hex.substring(16, 24), hex.substring(24));
  }

  public static String recoveryDigest(Long userId, String code) {
    String normalized =
        code == null ? "" : code.replace("-", "").replace(" ", "").toUpperCase(Locale.ROOT);
    // 128 zufällige Bits in Hexdarstellung. Nur die Anzeige-Trennzeichen werden normalisiert.
    if (!normalized.matches("[0-9A-F]{32}")) {
      return null;
    }
    return digest("recovery:" + userId + ":" + normalized);
  }

  static String digest(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (GeneralSecurityException failure) {
      throw new IllegalStateException("SHA-256 unavailable", failure);
    }
  }
}
