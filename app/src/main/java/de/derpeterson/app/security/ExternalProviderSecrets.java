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
  private final byte[] encryptionKeyBytes;
  private final SecureRandom secureRandom = new SecureRandom();

  public ExternalProviderSecrets(Environment environment) {
    String encodedEncryptionKey = environment.getProperty(KEY_PROPERTY, "").trim();
    if (encodedEncryptionKey.isEmpty()) {
      encryptionKeyBytes = null;
      return;
    }

    try {
      encryptionKeyBytes = Base64.getDecoder().decode(encodedEncryptionKey);
    } catch (IllegalArgumentException _) {
      throw new IllegalStateException("Invalid provider encryption key encoding");
    }

    if (encryptionKeyBytes.length != 32) {
      throw new IllegalStateException("Provider encryption key must contain 32 random bytes");
    }
  }

  public String encrypt(String providerId, String clientSecret) {
    if (clientSecret.isEmpty()) {
      return "";
    }

    byte[] secretBytes = clientSecret.getBytes(StandardCharsets.UTF_8);
    byte[] nonceBytes = new byte[12];
    secureRandom.nextBytes(nonceBytes);
    try {
      byte[] encryptedSecretBytes =
          secretCipher(Cipher.ENCRYPT_MODE, providerId, nonceBytes).doFinal(secretBytes);
      byte[] envelopeBytes = new byte[13 + encryptedSecretBytes.length];
      envelopeBytes[0] = 1;
      System.arraycopy(nonceBytes, 0, envelopeBytes, 1, nonceBytes.length);
      System.arraycopy(encryptedSecretBytes, 0, envelopeBytes, 13, encryptedSecretBytes.length);
      return Base64.getUrlEncoder().withoutPadding().encodeToString(envelopeBytes);
    } catch (GeneralSecurityException _) {
      throw new IllegalStateException("Provider secret encryption failed");
    } finally {
      Arrays.fill(secretBytes, (byte) 0);
    }
  }

  public String decrypt(String providerId, String encryptedEnvelope) {
    if (encryptedEnvelope.isEmpty()) {
      return "";
    }
    byte[] secretBytes = null;
    try {
      byte[] envelopeBytes = Base64.getUrlDecoder().decode(encryptedEnvelope);
      // Format: Versionsbyte, 12-Byte-Nonce, UTF-8-Inhalt (höchstens 2048 Zeichen), 16-Byte-Tag.
      if (envelopeBytes.length < 30 || envelopeBytes.length > 8221 || envelopeBytes[0] != 1) {
        throw new IllegalArgumentException("Invalid encrypted provider envelope");
      }
      secretBytes =
          secretCipher(Cipher.DECRYPT_MODE, providerId, Arrays.copyOfRange(envelopeBytes, 1, 13))
              .doFinal(envelopeBytes, 13, envelopeBytes.length - 13);
      return new String(secretBytes, StandardCharsets.UTF_8);
    } catch (GeneralSecurityException | IllegalArgumentException _) {
      // Keine Zugangsdaten oder verschlüsselten Werte in Fehlermeldungen aufnehmen.
      throw new IllegalStateException("Provider secret cannot be decrypted");
    } finally {
      if (secretBytes != null) {
        Arrays.fill(secretBytes, (byte) 0);
      }
    }
  }

  private Cipher secretCipher(int cipherMode, String providerId, byte[] nonceBytes)
      throws GeneralSecurityException {
    if (encryptionKeyBytes == null) {
      throw new IllegalStateException("Provider secrets encryption key is not configured");
    }

    if (!ExternalProviderSettings.PROVIDERS.contains(providerId)) {
      throw new IllegalArgumentException("Unsupported sign-in provider");
    }

    Cipher secretCipher = Cipher.getInstance("AES/GCM/NoPadding");
    secretCipher.init(
        cipherMode,
        new SecretKeySpec(encryptionKeyBytes, "AES"),
        new GCMParameterSpec(128, nonceBytes));
    secretCipher.updateAAD(("external-provider:1:" + providerId).getBytes(StandardCharsets.UTF_8));
    return secretCipher;
  }
}
