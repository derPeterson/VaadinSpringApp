package de.derpeterson.app.security;

import de.derpeterson.app.config.ConfigurationCatalog.StartupSetting;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Anbietergebundene Verschlüsselung. Der eigene Startschlüssel bleibt außerhalb der Datenbank. */
@Component
public final class ExternalProviderSecrets {
  public static final String KEY_PROPERTY =
      StartupSetting.PROVIDER_SECRETS_ENCRYPTION_KEY.property();
  private final byte[] key;
  private final SecureRandom random = new SecureRandom();

  public ExternalProviderSecrets(Environment environment) {
    String configured = environment.getProperty(KEY_PROPERTY, "").trim();
    if (configured.isEmpty()) {
      key = null;
      return;
    }
    try {
      key = Base64.getDecoder().decode(configured);
    } catch (IllegalArgumentException _) {
      throw new IllegalStateException("Invalid provider encryption key encoding");
    }
    if (key.length != 32) {
      throw new IllegalStateException("Provider encryption key must contain 32 random bytes");
    }
  }

  public String encrypt(String provider, String secret) {
    if (secret.isEmpty()) {
      return "";
    }
    byte[] plaintext = secret.getBytes(StandardCharsets.UTF_8);
    byte[] nonce = new byte[12];
    random.nextBytes(nonce);
    try {
      byte[] ciphertext = cipher(Cipher.ENCRYPT_MODE, provider, nonce).doFinal(plaintext);
      byte[] envelope = new byte[13 + ciphertext.length];
      envelope[0] = 1;
      System.arraycopy(nonce, 0, envelope, 1, nonce.length);
      System.arraycopy(ciphertext, 0, envelope, 13, ciphertext.length);
      return Base64.getUrlEncoder().withoutPadding().encodeToString(envelope);
    } catch (GeneralSecurityException _) {
      throw new IllegalStateException("Provider secret encryption failed");
    } finally {
      Arrays.fill(plaintext, (byte) 0);
    }
  }

  public String decrypt(String provider, String encrypted) {
    if (encrypted.isEmpty()) {
      return "";
    }
    byte[] plaintext = null;
    try {
      byte[] envelope = Base64.getUrlDecoder().decode(encrypted);
      // Format: Versionsbyte, 12-Byte-Nonce, UTF-8-Inhalt (höchstens 2048 Zeichen), 16-Byte-Tag.
      if (envelope.length < 30 || envelope.length > 8221 || envelope[0] != 1) {
        throw new IllegalArgumentException("Invalid encrypted provider envelope");
      }
      plaintext =
          cipher(Cipher.DECRYPT_MODE, provider, Arrays.copyOfRange(envelope, 1, 13))
              .doFinal(envelope, 13, envelope.length - 13);
      return new String(plaintext, StandardCharsets.UTF_8);
    } catch (GeneralSecurityException | IllegalArgumentException _) {
      // Keine Zugangsdaten oder verschlüsselten Werte in Fehlermeldungen aufnehmen.
      throw new IllegalStateException("Provider secret cannot be decrypted");
    } finally {
      if (plaintext != null) {
        Arrays.fill(plaintext, (byte) 0);
      }
    }
  }

  private Cipher cipher(int mode, String provider, byte[] nonce) throws GeneralSecurityException {
    if (key == null) {
      throw new IllegalStateException("Provider secrets encryption key is not configured");
    }
    if (!ExternalProviderSettings.PROVIDERS.contains(provider)) {
      throw new IllegalArgumentException("Unsupported sign-in provider");
    }
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(mode, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
    cipher.updateAAD(("external-provider:1:" + provider).getBytes(StandardCharsets.UTF_8));
    return cipher;
  }
}
