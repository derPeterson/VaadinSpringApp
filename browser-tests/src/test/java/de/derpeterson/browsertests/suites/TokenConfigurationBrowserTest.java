package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.*;
import static de.derpeterson.browsertests.BrowserRoutes.*;

import de.derpeterson.app.config.TokenDurationPolicy;
import de.derpeterson.browsertests.*;
import de.derpeterson.browsertests.pages.*;
import io.qameta.allure.*;
import io.qameta.allure.playwright.AllurePlaywright;
import java.time.Duration;
import org.junit.jupiter.api.*;

@Tag("browser")
@Feature("Mindestdauer für Tokens")
public class TokenConfigurationBrowserTest extends BrowserFixture {
  @Test
  @DisplayName("CONFIG-03A: Verifikationsgültigkeit verlangt mindestens eine Minute")
  @Description(
      """
        1. Als Admin Token-Konfiguration öffnen; Ausgangswerte merken und Mindesthinweis/Erklärung prüfen.
        2. Syntaxfehler, Null, negative Dauer, eine Sekunde und knapp unter dem Minimum einzeln speichern versuchen.
        3. Feldfehler prüfen, jeweils neu laden: Ausgangswert muss erhalten bleiben.
        4. Genau die Mindestdauer als Minutenangabe speichern und ISO-Wert frisch prüfen; Ausgangswerte wiederherstellen.
        **Erwartet:** Alle vier Felder konsistent; Gültigkeit mindestens eine Minute. Keine SQL-/Servicevorbereitung.
        """)
  void verificationValidity() {
    check(
        texts.getAdminConfigEntryVerificationTokenValidDuration(),
        TokenDurationPolicy.MIN_VALIDITY,
        texts.getAdminConfigEntryVerificationTokenValidDurationTooltip());
  }

  @Test
  @DisplayName("CONFIG-03B: Verifikationstoken-Aufbewahrung verlangt mindestens zwei Minuten")
  @Description(
      """
        1. Als Admin Ausgangswerte und Mindesthinweis/Erklärung der Token-Aufbewahrung prüfen.
        2. Ungültige Syntax, Null, negative/einsekündige Dauer und knapp unter zwei Minuten versuchen.
        3. Ablehnung und unveränderten gespeicherten Wert jeweils nach Neuladen prüfen.
        4. Exakt zwei Minuten speichern, ISO-Wert neu laden und Original wiederherstellen.
        **Erwartet:** Aufbewahrung nach Ablauf; weder Linkverlängerung noch unbegrenzte Bedeutung von Null.
        """)
  void verificationRetention() {
    check(
        texts.getAdminConfigEntryVerificationTokenLiveDuration(),
        TokenDurationPolicy.MIN_RETENTION,
        texts.getAdminConfigEntryVerificationTokenLiveDurationTooltip());
  }

  @Test
  @DisplayName("CONFIG-03C: Resetgültigkeit verlangt mindestens eine Minute")
  @Description(
      """
        1. Als Admin Ausgangswerte und Mindesthinweis/Erklärung des Resetlinks prüfen.
        2. Syntaxfehler, Null, negative/einsekündige Dauer und knapp unter einer Minute versuchen.
        3. Jede Ablehnung und unveränderten gespeicherten Wert nach Neuladen prüfen.
        4. Exakt eine Minute speichern, ISO-Wert neu laden und Original wiederherstellen.
        **Erwartet:** Der zuvor reproduzierte Null-Dauer-Fehler ist behoben; Mindestgrenze wird angenommen.
        """)
  void resetValidity() {
    check(
        texts.getAdminConfigEntryPasswordResetTokenValidDuration(),
        TokenDurationPolicy.MIN_VALIDITY,
        texts.getAdminConfigEntryPasswordResetTokenValidDurationTooltip());
  }

  @Test
  @DisplayName("CONFIG-03D: Resettoken-Aufbewahrung verlangt mindestens zwei Minuten")
  @Description(
      """
        1. Als Admin Ausgangswerte und Mindesthinweis/Erklärung der Reset-Aufbewahrung prüfen.
        2. Ungültige Syntax, Null, negative/einsekündige Dauer und knapp unter zwei Minuten versuchen.
        3. Jede Ablehnung und unveränderten gespeicherten Wert nach Neuladen prüfen.
        4. Exakt zwei Minuten speichern, ISO-Wert neu laden und Original wiederherstellen.
        **Erwartet:** Aufbewahrung nach Ablauf ist mindestens zwei Minuten, ohne unbegrenzte Null-Sonderregel.
        """)
  void resetRetention() {
    check(
        texts.getAdminConfigEntryPasswordResetTokenLiveDuration(),
        TokenDurationPolicy.MIN_RETENTION,
        texts.getAdminConfigEntryPasswordResetTokenLiveDurationTooltip());
  }

  private void check(String label, Duration minimum, String explanation) {
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    assertThat(page).hasURL(urlPattern(ADMIN_ROUTE));
    new UserMenu(page).selectTestLanguage();
    TokenConfigurationPage settings = new TokenConfigurationPage(page);
    settings.open();
    String original = settings.value(label);
    settings.explain(label, minimum.toMinutes(), explanation);
    for (String invalid :
        new String[] {"invalid", "PT0S", "PT-1S", "PT1S", minimum.minusNanos(1).toString()}) {
      settings.enter(label, invalid);
      settings.reject(
          label,
          invalid.equals("invalid")
              ? texts.getAdminConfigValidationTokenDuration(minimum.toMinutes())
              : texts.getAdminConfigValidationDurationMinimum(minimum.toMinutes()));
      settings.reload();
      assertThat(settings.field(label).locator("input")).hasValue(original);
    }
    AllurePlaywright.attachScreenshot("Unveränderte Werte nach abgelehnten Dauern", page);
    try {
      settings.enter(label, minimum.toMinutes() + "m");
      settings.save();
      settings.reload();
      assertThat(settings.field(label).locator("input")).hasValue(minimum.toString());
    } finally {
      settings.enter(label, original);
      settings.save();
    }
  }
}
