package de.derpeterson.app.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;

class ExternalProviderSecretsTest {
  @ParameterizedTest
  @ValueSource(strings = {"google", "microsoft"})
  void authenticatedEncryptionUsesFreshNoncesAndPreservesTheCompleteSecret(String provider) {
    ExternalProviderSecrets secrets = configured((byte) 1);
    String original = "€".repeat(2048);
    String encrypted = secrets.encrypt(provider, original);

    assertNotEquals(original, encrypted);
    assertNotEquals(encrypted, secrets.encrypt(provider, original));
    assertFalse(encrypted.contains(original));
    assertEquals(original, secrets.decrypt(provider, encrypted));
    assertEquals(original, configured((byte) 1).decrypt(provider, encrypted));
  }

  @Test
  void missingOrMalformedKeysNeverFallBackToPlaintext() {
    ExternalProviderSecrets missing = new ExternalProviderSecrets(new MockEnvironment());

    assertEquals("", missing.encrypt("google", ""));
    assertEquals("", missing.decrypt("microsoft", ""));
    assertThrows(IllegalStateException.class, () -> missing.encrypt("google", "own-secret"));
    String encrypted = configured((byte) 1).encrypt("google", "own-secret");

    assertThrows(IllegalStateException.class, () -> missing.decrypt("google", encrypted));
    for (String value :
        new String[] {"not-base64!", Base64.getEncoder().encodeToString(new byte[16])}) {
      MockEnvironment environment =
          new MockEnvironment().withProperty(ExternalProviderSecrets.KEY_PROPERTY, value);

      assertThrows(IllegalStateException.class, () -> new ExternalProviderSecrets(environment));
    }
  }

  @Test
  void anotherProviderAnotherKeyTamperingAndPlaintextAreRejectedWithoutSecretDetails() {
    ExternalProviderSecrets secrets = configured((byte) 1);
    String encrypted = secrets.encrypt("google", "own-secret");

    assertThrows(IllegalStateException.class, () -> secrets.decrypt("microsoft", encrypted));
    ExternalProviderSecrets otherCipher = configured((byte) 2);

    assertThrows(IllegalStateException.class, () -> otherCipher.decrypt("google", encrypted));
    byte[] damaged = Base64.getUrlDecoder().decode(encrypted);
    damaged[damaged.length - 1] ^= 1;
    String tampered = Base64.getUrlEncoder().withoutPadding().encodeToString(damaged);
    for (String invalid : new String[] {tampered, "own-secret", encrypted.substring(0, 5)}) {
      IllegalStateException failure =
          assertThrows(IllegalStateException.class, () -> secrets.decrypt("google", invalid));

      assertFalse(failure.getMessage().contains("own-secret"));
      assertFalse(failure.getMessage().contains(invalid));
    }
    assertFalse(
        new ExternalProviderSettings.Credentials("google", "own-client", "own-secret", true, 0L)
            .toString()
            .contains("own-secret"));
  }

  private ExternalProviderSecrets configured(byte value) {
    byte[] key = new byte[32];
    java.util.Arrays.fill(key, value);
    return new ExternalProviderSecrets(
        new MockEnvironment()
            .withProperty(
                ExternalProviderSecrets.KEY_PROPERTY, Base64.getEncoder().encodeToString(key)));
  }
}
