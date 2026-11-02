package de.derpeterson.browsertests.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Page;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Eigener Test-Authenticator. Geheimnisse nur aus dem regulären, kontogebundenen Einrichtungspfad.
 */
public final class TwoFactorPage {
  private static final Map<String, String> SEEDS = new HashMap<>();
  private static final Map<String, ArrayDeque<String>> CODES = new HashMap<>();
  private static final Map<String, Long> LAST_STEP = new HashMap<>();

  private TwoFactorPage() {}

  public static void completeIfRequired(Page page, String email, String password) {
    if (!URI.create(page.url()).getPath().equals("/two-factor")) {
      return;
    }
    new UserMenu(page).selectTestLanguage();
    page.waitForCondition(
        () ->
            page.locator("#two-factor-password").isVisible()
                || page.locator("#two-factor-code").isVisible()
                || page.locator("#two-factor-recovery-codes").isVisible());
    if (page.locator("#two-factor-password").isVisible()) {
      page.locator("#two-factor-password input").fill(password);
      page.locator("#two-factor-submit").click();
      enrollCurrent(page, email);
    } else {
      page.locator("#two-factor-code input").fill(nextCode(page, email));
      page.locator("#two-factor-submit").click();
    }
    page.waitForURL(url -> !URI.create(url).getPath().equals("/two-factor"));
  }

  public static void enrollCurrent(Page page, String email) {
    confirmEnrollment(page, email);
    page.locator("#two-factor-submit").click();
  }

  /**
   * Ersten TOTP bestätigen, die geschützte einmalige Ausgabe aber für weitere UI-Prüfungen offen
   * lassen.
   */
  public static void confirmEnrollment(Page page, String email) {
    assertThat(page.locator("#two-factor-qr")).isVisible();
    String href =
        page.locator("a[href*='/internal/two-factor/setup/'][href$='/key']").getAttribute("href");
    String key =
        (String)
            page.evaluate(
                "async url => { const response = await fetch(url, {cache:'no-store'}); if (!response.ok) throw new Error('Protected setup key unavailable'); return response.text(); }",
                href);
    SEEDS.put(email, key);
    page.locator("#two-factor-code input").fill(totp(page, email));
    page.locator("#two-factor-submit").click();
    captureCodes(page, email);
  }

  public static void captureCodes(Page page, String email) {
    assertThat(page.locator("#two-factor-recovery-codes")).isVisible();
    String values =
        page.frameLocator("#two-factor-recovery-codes")
            .locator("#recovery-code-values")
            .innerText();
    CODES.put(email, new ArrayDeque<>(Arrays.asList(values.strip().split("\\s+"))));
    if (CODES.get(email).size() != 10) {
      throw new AssertionError("Ten issued recovery codes required");
    }
  }

  public static String nextCode(Page page, String email) {
    ArrayDeque<String> codes = CODES.get(email);
    return codes != null && !codes.isEmpty() ? codes.removeFirst() : totp(page, email);
  }

  public static int remainingRecoveryCodes(String email) {
    ArrayDeque<String> codes = CODES.get(email);
    return codes == null ? 0 : codes.size();
  }

  public static String totp(Page page, String email) {
    String base32 = SEEDS.get(email);
    if (base32 == null) {
      throw new AssertionError("Test account must complete real authenticator setup first");
    }
    page.waitForCondition(
        () -> Instant.now().getEpochSecond() / 30 > LAST_STEP.getOrDefault(email, -1L),
        new Page.WaitForConditionOptions().setTimeout(35_000));
    long step = Instant.now().getEpochSecond() / 30;
    LAST_STEP.put(email, step);
    byte[] seed = new byte[base32.length() * 5 / 8];
    int buffer = 0;
    int bits = 0;
    int offset = 0;
    for (char character : base32.toCharArray()) {
      buffer = (buffer << 5) | "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".indexOf(character);
      bits += 5;
      if (bits >= 8) {
        bits -= 8;
        seed[offset++] = (byte) (buffer >>> bits);
      }
    }
    try {
      Mac mac = Mac.getInstance("HmacSHA1");
      mac.init(new SecretKeySpec(seed, "HmacSHA1"));
      byte[] digest = mac.doFinal(ByteBuffer.allocate(8).putLong(step).array());
      int index = digest[19] & 15;
      int value = ByteBuffer.wrap(digest, index, 4).getInt() & 0x7fffffff;
      return String.format(Locale.ROOT, "%06d", value % 1_000_000);
    } catch (GeneralSecurityException failure) {
      throw new IllegalStateException("Test authenticator unavailable", failure);
    } finally {
      Arrays.fill(seed, (byte) 0);
    }
  }
}
