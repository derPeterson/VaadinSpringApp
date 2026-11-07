package de.derpeterson.app.security.mfa;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class TwoFactorSecretsTest {
  private TwoFactorSecrets secrets(byte keyByte) {
    byte[] key = new byte[32];
    java.util.Arrays.fill(key, keyByte);
    return new TwoFactorSecrets(
        new MockEnvironment()
            .withProperty(TwoFactorSecrets.KEY_PROPERTY, Base64.getEncoder().encodeToString(key)));
  }

  @Test
  void authenticatesOwnerPurposeCiphertextAndExternalKey() {
    TwoFactorSecrets cipher = secrets((byte) 1);
    byte[] plaintext = Totp.newSecret();
    String encrypted = cipher.encrypt(1L, "factor", plaintext);
    assertArrayEquals(plaintext, cipher.decrypt(1L, "factor", encrypted));
    assertNotEquals(encrypted, cipher.encrypt(1L, "factor", plaintext));
    assertFalse(encrypted.contains(Totp.base32(plaintext)));
    assertThrows(IllegalStateException.class, () -> cipher.decrypt(2L, "factor", encrypted));
    assertThrows(IllegalStateException.class, () -> cipher.decrypt(1L, "enrollment", encrypted));
    TwoFactorSecrets otherCipher = secrets((byte) 2);
    assertThrows(IllegalStateException.class, () -> otherCipher.decrypt(1L, "factor", encrypted));
    byte[] tampered = Base64.getUrlDecoder().decode(encrypted);
    tampered[tampered.length - 1] ^= 1;
    String tamperedEncoded = Base64.getUrlEncoder().withoutPadding().encodeToString(tampered);
    assertThrows(IllegalStateException.class, () -> cipher.decrypt(1L, "factor", tamperedEncoded));
  }

  @Test
  void neverGeneratesAnEphemeralFallbackKey() {
    TwoFactorSecrets missing = new TwoFactorSecrets(new MockEnvironment());
    assertFalse(missing.available());
    byte[] secret = Totp.newSecret();
    assertThrows(IllegalStateException.class, () -> missing.encrypt(1L, "factor", secret));
    MockEnvironment malformedKeyEnvironment =
        new MockEnvironment().withProperty(TwoFactorSecrets.KEY_PROPERTY, "not-base64");
    assertThrows(IllegalStateException.class, () -> new TwoFactorSecrets(malformedKeyEnvironment));
    MockEnvironment shortKeyEnvironment =
        new MockEnvironment().withProperty(TwoFactorSecrets.KEY_PROPERTY, "AAAA");
    assertThrows(IllegalStateException.class, () -> new TwoFactorSecrets(shortKeyEnvironment));
  }

  @Test
  void recoveryCodesHaveIndependentHighEntropyAndAccountBoundDigests() {
    List<String> codes = secrets((byte) 1).newRecoveryCodes();
    assertEquals(10, codes.size());
    assertEquals(10, codes.stream().distinct().count());
    for (String code : codes) {
      assertEquals(35, code.length());
      String digest = TwoFactorSecrets.recoveryDigest(1L, code);
      assertEquals(64, digest.length());
      assertNotEquals(code, digest);
      assertNotEquals(digest, TwoFactorSecrets.recoveryDigest(2L, code));
      assertEquals(
          digest,
          TwoFactorSecrets.recoveryDigest(
              1L, code.toLowerCase(java.util.Locale.ROOT).replace('-', ' ')));
    }
  }

  @Test
  void shortEmailCodesCannotBeVerifiedWithoutTheExternalKeyOrReusedForAnotherContext() {
    java.util.UUID challenge = java.util.UUID.randomUUID();
    String digest = secrets((byte) 1).emailVerifier(1L, challenge, "own@example.test", "12345678");
    assertEquals(64, digest.length());
    assertEquals(
        digest, secrets((byte) 1).emailVerifier(1L, challenge, "own@example.test", "12345678"));
    assertNotEquals(
        digest, secrets((byte) 2).emailVerifier(1L, challenge, "own@example.test", "12345678"));
    assertNotEquals(
        digest, secrets((byte) 1).emailVerifier(2L, challenge, "own@example.test", "12345678"));
    assertNotEquals(
        digest,
        secrets((byte) 1)
            .emailVerifier(1L, java.util.UUID.randomUUID(), "own@example.test", "12345678"));
    assertNotEquals(
        digest, secrets((byte) 1).emailVerifier(1L, challenge, "other@example.test", "12345678"));
  }
}
