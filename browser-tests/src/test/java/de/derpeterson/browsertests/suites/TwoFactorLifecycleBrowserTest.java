package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.BrowserActor;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.BrowserTestRunner;
import de.derpeterson.browsertests.i18n.UiTexts;
import de.derpeterson.browsertests.mail.MailpitInbox;
import de.derpeterson.browsertests.pages.AdminUserDialog;
import de.derpeterson.browsertests.pages.AdminUsersPage;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.TwoFactorPage;
import de.derpeterson.browsertests.pages.UiActions;
import de.derpeterson.browsertests.pages.UserMenu;
import io.qameta.allure.playwright.AllurePlaywright;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Tag("browser")
public class TwoFactorLifecycleBrowserTest extends BrowserFixture {
  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName("MFA-14: Sprachwechsel erhält Mailcode, QR, einmalige Codeausgabe und Hilfsanfrage")
  void languageChangesPreserveTheCurrentSecurityPhase(int width) {
    page.setViewportSize(width, 900);
    new LoginPage(page).open();
    String email = "locale-mfa-" + UUID.randomUUID() + "@gmail.com";
    page.locator("#external-login-google").click();
    providerIdentity(email);
    page.locator("#external-register-first input").fill("Locale");
    page.locator("#external-register-last input").fill("Fixture");
    page.locator("#external-register-submit").click();
    assertThat(page).hasURL(Pattern.compile(".*/home$"));
    new UserMenu(page).selectTestLanguage();
    page.navigate("/account?tab=security");
    page.locator("#account-two-factor-setup").click();
    page.locator("#two-factor-confirm-google").click();
    providerIdentity(email);
    String mailCode = new MailpitInbox(context).awaitAuthenticatorSetupCode(email);
    UiTexts other = new UiTexts(texts.language().equals("de") ? "en" : "de");
    new UserMenu(page).selectLanguage(other.language());
    assertThat(page.locator("#two-factor-code"))
        .hasJSProperty("label", other.getTwoFactorEmailCodeLabel());
    page.locator("#two-factor-code input").fill(mailCode);
    page.locator("#two-factor-submit").click();
    assertThat(page.locator("#two-factor-qr")).isVisible();
    Locator manual = page.locator("a[href*='/internal/two-factor/setup/'][href$='/key']");
    String originalKeyLink = manual.getAttribute("href");
    new UserMenu(page).selectTestLanguage();
    assertThat(manual).hasAttribute("href", originalKeyLink);
    assertThat(page.locator("#two-factor-qr")).hasAttribute("alt", texts.getTwoFactorQrAlt());
    TwoFactorPage.confirmEnrollment(page, email);
    String codes =
        page.frameLocator("#two-factor-recovery-codes")
            .locator("#recovery-code-values")
            .innerText();
    String output = page.locator("#two-factor-download-pdf").getAttribute("href").split("\\?")[0];
    new UserMenu(page).selectLanguage(other.language());
    assertThat(page.frameLocator("#two-factor-recovery-codes").locator("#recovery-code-values"))
        .hasText(codes);
    assertThat(page.locator("#two-factor-download-pdf"))
        .hasAttribute("href", output + "?lang=" + other.language());
    new UserMenu(page).selectTestLanguage();
    assertThat(page.frameLocator("#two-factor-recovery-codes").locator("#recovery-code-values"))
        .hasText(codes);
    page.locator("#two-factor-submit").click();
    assertThat(page).hasURL(Pattern.compile(".*/account\\?tab=security$"));
    new UserMenu(page).signOut();
    page.locator("#external-login-google").click();
    providerIdentity(email);
    page.locator("#two-factor-help").click();
    assertThat(page.locator("#account-support")).isVisible();
    String draft = "Own recovery request " + UUID.randomUUID();
    page.locator("#support-reply textarea").fill(draft);
    new UserMenu(page).selectLanguage(other.language());
    assertThat(page.locator("#support-reply"))
        .hasJSProperty("label", other.getSupportDescription());
    assertThat(page.locator("#support-reply textarea")).hasValue(draft);
    page.locator("#support-create").click();
    Locator ticket = page.locator("#support-ticket-grid").getByText(Pattern.compile("^#[0-9]+$"));
    assertThat(ticket).hasCount(1);
    String number = ticket.innerText();
    new UserMenu(page).selectTestLanguage();
    assertThat(ticket).hasText(number);
    assertThat(page.locator("#account-support")).containsText(draft);
    AllurePlaywright.attachScreenshot("Sprachwechsel im eingeschränkten Hilfszugang", page);
    page.navigate("/home");
    assertThat(page).hasURL(Pattern.compile(".*/two-factor.*"));
    page.locator("#two-factor-cancel").click();
    new LoginPage(page).assertFormVisible();
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName("MFA-15: Superadmin-Entzug entfernt bereits geöffnete 2FA- und Anbieterformulare")
  void withdrawingSuperadminProtectsBothOpenConfigurations(int width) {
    signInAdministrator();
    AdminUsersPage users = new AdminUsersPage(page);
    String email = "role-mfa-" + UUID.randomUUID() + "@example.test";
    users.createUser(email, BrowserTestAccounts.USER_PASSWORD, true, true);
    AdminUserDialog form = users.openEdit(email);
    form.setRole(texts.getRoleSuperadmin(), true);
    form.save();
    try (BrowserActor actor = new BrowserActor(browser)) {
      actor.page.setViewportSize(width, 900);
      actor.loginAdmin(email, BrowserTestAccounts.USER_PASSWORD);
      actor.page.navigate("/admin?section=configuration&group=two_factor");
      assertThat(actor.page.locator("#two-factor-save")).isVisible();
      actor
          .page
          .getByRole(
              AriaRole.SWITCH,
              new Page.GetByRoleOptions()
                  .setName(texts.getTwoFactorInactivityEnabled())
                  .setExact(true))
          .setChecked(true);
      try (Page provider = actor.context.newPage()) {
        provider.setViewportSize(width, 900);
        provider.navigate("/admin?section=configuration&group=external_login");
        assertThat(provider.locator("#provider-google-save")).isVisible();
        String originalId = provider.locator("#provider-google-client-id input").inputValue();
        provider.locator("#provider-google-client-id input").fill("unsaved-own-test-draft");
        form = users.openEdit(email);
        form.setRole(texts.getRoleSuperadmin(), false);
        form.save();
        // Der bestehende Login und die normale Adminrolle bleiben erhalten. Die live
        // zurückgezogenen Sonderrechte müssen beide alten Formulare verwerfen.
        assertThat(actor.page.locator("#two-factor-save")).hasCount(0);
        assertThat(provider.locator("#provider-google-save")).hasCount(0);
        actor.page.navigate("/admin?section=configuration&group=two_factor");
        assertThat(actor.page.locator("#admin-two-factor-settings")).hasCount(0);
        provider.navigate("/admin?section=configuration&group=external_login");
        assertThat(provider.locator("#admin-provider-settings")).hasCount(0);
        AllurePlaywright.attachScreenshot(
            "Zurückgezogene Superadminrechte in offener Ansicht", actor.page);
        openConfiguration();
        assertThat(page.locator("#two-factor-inactivity-enabled")).hasJSProperty("checked", false);
        page.navigate("/admin?section=configuration&group=external_login");
        assertThat(page.locator("#provider-google-client-id input")).hasValue(originalId);
      }
      new UserMenu(actor.page).signOut();
    }
  }

  @Test
  @DisplayName(
      "MFA-16: Startfreigabe allein schwächt nichts; gespeicherte Entwicklungsoptionen sind ohne sie unwirksam")
  void developmentOptionsAreExplicitAndIgnoredAfterRestartWithoutTheGate() throws Exception {
    signInAdministrator();
    try {
      BrowserTestRunner.restartWithDevelopmentOptions(true);
      openConfiguration();
      for (int width : new int[] {1280, 390}) {
        page.setViewportSize(width, 900);
        assertThat(
                page.getByText(
                    texts.getTwoFactorDevelopmentWarning(),
                    new Page.GetByTextOptions().setExact(true)))
            .isVisible();
        assertThat(page.locator("#two-factor-development-pause")).hasJSProperty("checked", false);
        assertThat(page.locator("#two-factor-development-google")).hasJSProperty("checked", false);
      }
      assertFreshLoginNeedsFactor();
      page.getByRole(
              AriaRole.SWITCH,
              new Page.GetByRoleOptions()
                  .setName(texts.getTwoFactorDevelopmentGoogle())
                  .setExact(true))
          .setChecked(true);
      saveConfiguration();
      assertFreshLoginNeedsFactor();
      page.getByRole(
              AriaRole.SWITCH,
              new Page.GetByRoleOptions()
                  .setName(texts.getTwoFactorDevelopmentPause())
                  .setExact(true))
          .setChecked(true);
      saveConfiguration();
      try (BrowserActor actor = new BrowserActor(browser)) {
        LoginPage login = new LoginPage(actor.page);
        login.open();
        login.signInFirstFactor(
            BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
        assertThat(actor.page).hasURL(Pattern.compile(".*/admin$"));
        assertThat(actor.page.locator("#two-factor-code")).hasCount(0);
        new UserMenu(actor.page).selectTestLanguage();
        new UserMenu(actor.page).signOut();
      }
    } finally {
      BrowserTestRunner.restartWithDevelopmentOptions(false);
    }
    openConfiguration();
    assertThat(
            page.getByText(
                texts.getTwoFactorDevelopmentTitle(), new Page.GetByTextOptions().setExact(true)))
        .isHidden();
    assertFreshLoginNeedsFactor();
  }

  private void signInAdministrator() {
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(page).selectTestLanguage();
  }

  private void openConfiguration() {
    page.navigate("/admin?section=configuration&group=two_factor");
    TwoFactorPage.completeIfRequired(
        page, BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    page.navigate("/admin?section=configuration&group=two_factor");
    new UserMenu(page).selectTestLanguage();
    assertThat(page.locator("#two-factor-save")).isVisible();
  }

  private void saveConfiguration() {
    page.locator("#two-factor-save").click();
    page.locator("#two-factor-action-code input")
        .fill(TwoFactorPage.nextCode(page, BrowserTestAccounts.SUPERADMIN_EMAIL));
    UiActions.complete(page, () -> page.locator("#two-factor-action-confirm").click());
    page.waitForCondition(
        () ->
            java.net.URI.create(page.url()).getPath().equals("/two-factor")
                || page.locator("vaadin-notification-card:not([closing])")
                    .filter(new Locator.FilterOptions().setHasText(texts.getTwoFactorSaved()))
                    .isVisible());
    openConfiguration();
  }

  private void assertFreshLoginNeedsFactor() {
    try (BrowserActor actor = new BrowserActor(browser)) {
      LoginPage login = new LoginPage(actor.page);
      login.open();
      login.signInFirstFactor(
          BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
      assertThat(actor.page).hasURL(Pattern.compile(".*/two-factor.*"));
      assertThat(actor.page.locator("#two-factor-code")).isVisible();
      actor.page.locator("#two-factor-cancel").click();
      new LoginPage(actor.page).assertFormVisible();
    }
  }

  private void providerIdentity(String email) {
    assertThat(page.locator("#fixture-email")).isVisible();
    page.locator("#fixture-email").fill(email);
    page.locator("#fixture-mode").selectOption("valid");
    page.locator("#fixture-signin").click();
  }
}
