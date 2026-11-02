package de.derpeterson.app.security.mfa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TotpTest {
  private final byte[] secret = "12345678901234567890".getBytes(StandardCharsets.US_ASCII);

  @ParameterizedTest
  @CsvSource({
    "59,287082",
    "1111111109,081804",
    "1111111111,050471",
    "1234567890,005924",
    "2000000000,279037",
    "20000000000,353130"
  })
  void matchesRfc6238Sha1VectorsWithSixDigits(long seconds, String expected) {
    assertEquals(expected, Totp.code(secret, seconds / 30));
  }

  @Test
  void acceptsOnlyTheTightTimeWindowAndRejectsReplay() {
    Instant now = Instant.ofEpochSecond(3000);
    assertEquals(99, Totp.verify(secret, Totp.code(secret, 99), now, -1));
    assertEquals(100, Totp.verify(secret, Totp.code(secret, 100), now, -1));
    assertEquals(101, Totp.verify(secret, Totp.code(secret, 101), now, -1));
    assertEquals(-1, Totp.verify(secret, Totp.code(secret, 98), now, -1));
    assertEquals(-1, Totp.verify(secret, Totp.code(secret, 102), now, -1));
    assertEquals(-1, Totp.verify(secret, Totp.code(secret, 100), now, 100));
    assertEquals(-1, Totp.verify(secret, Totp.code(secret, 99), now, 100));
    assertEquals(-1, Totp.verify(secret, " 123456", now, -1));
    assertEquals(-1, Totp.verify(secret, "１２３４５６", now, -1));
  }

  @Test
  void exportsTheStandardBase32SecretWithoutPadding() {
    assertEquals("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ", Totp.base32(secret));
    assertEquals(32, Totp.base32(Totp.newSecret()).length());
    assertNotEquals(Totp.base32(Totp.newSecret()), Totp.base32(Totp.newSecret()));
    assertThrows(IllegalArgumentException.class, () -> Totp.code(new byte[4], 1));
  }
}
