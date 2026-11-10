package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.ADMIN_ROUTE;
import static de.derpeterson.browsertests.BrowserRoutes.urlPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;

import de.derpeterson.app.config.TokenDurationPolicy;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.TokenConfigurationPage;
import de.derpeterson.browsertests.pages.UserMenu;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.playwright.AllurePlaywright;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("browser")
@Feature("Mindestdauer für Tokens")
public class TokenConfigurationBrowserTest extends BrowserFixture {
  @Test
  @DisplayName("CONFIG-03A: Verifikationsgültigkeit verlangt mindestens eine Minute")
  @Description(
      """
      1. Als Admin Token-Konfiguration öffnen; Ausgangswerte merken und Mindesthinweis/Erklärung prüfen.
      2. Null, negative Dauer, eine Sekunde und knapp unter dem Minimum einzeln speichern versuchen.
      3. Feldfehler prüfen, jeweils neu laden: Ausgangswert muss erhalten bleiben.
      4. Genau die Mindestdauer als Minutenangabe speichern und ISO-Wert frisch prüfen; Ausgangswerte wiederherstellen.
      **Erwartet:** Alle vier Felder konsistent; Gültigkeit mindestens eine Minute. Keine SQL-/Servicevorbereitung.
      """)
  void verificationValidity() {
    check(
        uiTexts.getAdminConfigEntryVerificationTokenValidDuration(),
        TokenDurationPolicy.MINIMUM_TOKEN_VALIDITY,
        uiTexts.getAdminConfigEntryVerificationTokenValidDurationTooltip());
  }

  @Test
  @DisplayName("CONFIG-03B: Verifikationstoken-Aufbewahrung verlangt mindestens zwei Minuten")
  @Description(
      """
      1. Als Admin Ausgangswerte und Mindesthinweis/Erklärung der Token-Aufbewahrung prüfen.
      2. Null, negative/einsekündige Dauer und knapp unter zwei Minuten versuchen.
      3. Ablehnung und unveränderten gespeicherten Wert jeweils nach Neuladen prüfen.
      4. Exakt zwei Minuten speichern, ISO-Wert neu laden und Original wiederherstellen.
      **Erwartet:** Aufbewahrung nach Ablauf; weder Linkverlängerung noch unbegrenzte Bedeutung von Null.
      """)
  void verificationRetention() {
    check(
        uiTexts.getAdminConfigEntryVerificationTokenLiveDuration(),
        TokenDurationPolicy.MINIMUM_TOKEN_RETENTION,
        uiTexts.getAdminConfigEntryVerificationTokenLiveDurationTooltip());
  }

  @Test
  @DisplayName("CONFIG-03C: Resetgültigkeit verlangt mindestens eine Minute")
  @Description(
      """
      1. Als Admin Ausgangswerte und Mindesthinweis/Erklärung des Resetlinks prüfen.
      2. Null, negative/einsekündige Dauer und knapp unter einer Minute versuchen.
      3. Jede Ablehnung und unveränderten gespeicherten Wert nach Neuladen prüfen.
      4. Exakt eine Minute speichern, ISO-Wert neu laden und Original wiederherstellen.
      **Erwartet:** Der zuvor reproduzierte Null-Dauer-Fehler ist behoben; Mindestgrenze wird angenommen.
      """)
  void resetValidity() {
    check(
        uiTexts.getAdminConfigEntryPasswordResetTokenValidDuration(),
        TokenDurationPolicy.MINIMUM_TOKEN_VALIDITY,
        uiTexts.getAdminConfigEntryPasswordResetTokenValidDurationTooltip());
  }

  @Test
  @DisplayName("CONFIG-03D: Resettoken-Aufbewahrung verlangt mindestens zwei Minuten")
  @Description(
      """
      1. Als Admin Ausgangswerte und Mindesthinweis/Erklärung der Reset-Aufbewahrung prüfen.
      2. Null, negative/einsekündige Dauer und knapp unter zwei Minuten versuchen.
      3. Jede Ablehnung und unveränderten gespeicherten Wert nach Neuladen prüfen.
      4. Exakt zwei Minuten speichern, ISO-Wert neu laden und Original wiederherstellen.
      **Erwartet:** Aufbewahrung nach Ablauf ist mindestens zwei Minuten, ohne unbegrenzte Null-Sonderregel.
      """)
  void resetRetention() {
    check(
        uiTexts.getAdminConfigEntryPasswordResetTokenLiveDuration(),
        TokenDurationPolicy.MINIMUM_TOKEN_RETENTION,
        uiTexts.getAdminConfigEntryPasswordResetTokenLiveDurationTooltip());
  }

  private void check(String label, Duration minimum, String explanation) {
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);

    assertThat(browserPage).hasURL(urlPattern(ADMIN_ROUTE));
    new UserMenu(browserPage).selectTestLanguage();
    TokenConfigurationPage settings = new TokenConfigurationPage(browserPage);
    settings.open();
    String original = settings.value(label);
    settings.explain(label, minimum.toMinutes(), explanation);
    for (String invalid :
        new String[] {"PT0S", "PT-1S", "PT1S", minimum.minusNanos(1).toString()}) {
      settings.enter(label, invalid);
      settings.reject(label, uiTexts.getAdminConfigValidationDurationMinimum(minimum.toMinutes()));
      settings.reload();

      assertEquals(original, settings.value(label));
    }
    AllurePlaywright.attachScreenshot("Unveränderte Werte nach abgelehnten Dauern", browserPage);
    try {
      settings.enter(label, minimum.toMinutes() + "m");
      settings.save();
      settings.reload();

      assertEquals(minimum.toString(), settings.value(label));
    } finally {
      settings.enter(label, original);
      settings.save();
    }
  }
}
