package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.LOGIN_ROUTE;
import static de.derpeterson.browsertests.BrowserRoutes.urlPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.JSHandle;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.BoundingBox;
import de.derpeterson.browsertests.BrowserActor;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.i18n.UiTexts;
import de.derpeterson.browsertests.mail.MailpitInbox;
import de.derpeterson.browsertests.mail.MailpitInbox.CapturedMail;
import de.derpeterson.browsertests.pages.AccountPage;
import de.derpeterson.browsertests.pages.AdminUsersPage;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.TwoFactorPage;
import de.derpeterson.browsertests.pages.UiActions;
import de.derpeterson.browsertests.pages.UnsavedChangesPrompt;
import de.derpeterson.browsertests.pages.UserMenu;
import de.derpeterson.browsertests.pages.UserStatusMenu;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.playwright.AllurePlaywright;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@Tag("browser")
@Feature("Kontoverwaltung: Profil und E-Mail-Dialogentwurf")
public class AccountSettingsBrowserTest extends BrowserFixture {
  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName("Selbst deaktiviertes Konto lässt sich über die empfangene E-Mail reaktivieren")
  @Description(
      """
      1. Eigenes Testkonto über die normale Oberfläche anlegen und anmelden.
      2. Deaktivierung mit dem Portalpasswort bestätigen und Abmeldung prüfen.
      3. Tatsächlich zugestellte E-Mail öffnen und ihre Reaktivierung ausdrücklich bestätigen.
      4. Mit dem bisherigen Passwort wieder anmelden.
      """)
  void accountDeactivationSendsAUsableReactivationLink(int viewportWidth) {
    browserPage.setViewportSize(viewportWidth, 900);
    String accountEmail = account();
    openAccountLifecycleTab();
    browserPage.locator("#account-deactivate").click();
    assertThat(browserPage.locator("#account-lifecycle-password input")).isVisible();
    assertThat(browserPage.locator("#account-lifecycle-proof")).not().hasAttribute("opening", "");
    assertThat(browserPage.locator("#account-lifecycle-proof .cosmos-panel-description"))
        .hasCSS("font-size", "12px");
    AllurePlaywright.attachScreenshot(
        "Kontodeaktivierung bei " + viewportWidth + " px", browserPage);
    confirmLifecycleWithPortalPassword();

    assertThat(browserPage).hasURL(urlPattern(LOGIN_ROUTE));
    MailpitInbox mailpitInbox = new MailpitInbox(browserContext);
    CapturedMail deactivationMail =
        mailpitInbox.awaitSecurityMail(accountEmail, uiTexts.getAccountDeactivatedMailSubject());
    try (Page reactivationPage = mailpitInbox.openAccountReactivationAction(deactivationMail)) {
      reactivationPage.setViewportSize(viewportWidth, 900);
      reactivationPage.locator("#account-reactivation-submit").click();
      assertThat(reactivationPage.getByText(uiTexts.getAccountReactivateSuccess())).isVisible();
    }

    LoginPage loginPage = new LoginPage(browserPage);
    loginPage.signIn(accountEmail, BrowserTestAccounts.USER_PASSWORD);
    assertThat(browserPage.locator(".portal-home-page")).isVisible();
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName("Endgültige Kontolöschung erfordert einen Nachweis und verschickt eine Bestätigung")
  @Description(
      """
      1. Eigenes Testkonto öffnen und endgültige Löschung wählen.
      2. Falsches Portalpasswort eingeben und den abgewiesenen Versuch prüfen.
      3. Mit dem richtigen Passwort bestätigen und Abmeldung prüfen.
      4. Die tatsächliche Bestätigungsmail des isolierten Maildienstes abwarten.
      """)
  void accountDeletionRejectsWrongPasswordAndSendsConfirmation(int viewportWidth) {
    browserPage.setViewportSize(viewportWidth, 900);
    String accountEmail = account();
    openAccountLifecycleTab();
    browserPage.locator("#account-delete").click();
    browserPage.locator("#account-lifecycle-password input").fill("wrong-password");
    browserPage.locator("#account-lifecycle-submit").click();
    assertThat(browserPage.getByText(uiTexts.getAccountLifecycleInvalidProof())).isVisible();
    assertThat(browserPage.locator("#account-lifecycle-proof")).hasAttribute("opened", "");
    assertThat(browserPage.locator("#account-lifecycle-password input")).isVisible();

    confirmLifecycleWithPortalPassword();
    assertThat(browserPage).hasURL(urlPattern(LOGIN_ROUTE));
    new MailpitInbox(browserContext)
        .awaitSecurityMail(accountEmail, uiTexts.getAccountDeletedMailSubject());
  }

  private void openAccountLifecycleTab() {
    AccountPage accountPage = new AccountPage(browserPage);
    accountPage.open();
    accountPage.section("account-tab-security");
    browserPage.locator("#account-security-tab-lifecycle").click();
    assertThat(browserPage.locator("#account-delete")).isVisible();
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName("Ein eingerichteter Authenticator bestätigt die Kontolöschung per Ersatzcode")
  @Description(
      """
      1. Ein eigenes Testkonto anlegen und über die Oberfläche einen Authenticator einrichten.
      2. Endgültige Löschung wählen und die gemeinsame App-Code-/Ersatzcode-Eingabe prüfen.
      3. Einen während der Einrichtung gesicherten Ersatzcode eingeben und bewusst bestätigen.
      4. Abmeldung und tatsächliche Löschbestätigung prüfen.
      """)
  void enrolledAccountCanBeDeletedWithARecoveryCode(int viewportWidth) {
    browserPage.setViewportSize(viewportWidth, 900);
    String accountEmail = account();
    browserPage.navigate("/account?section=security&tab=portal");
    browserPage.locator("#account-two-factor-setup").click();
    browserPage.locator("#two-factor-password input").fill(BrowserTestAccounts.USER_PASSWORD);
    browserPage.locator("#two-factor-submit").click();
    TwoFactorPage.enrollCurrent(browserPage, accountEmail);

    openAccountLifecycleTab();
    browserPage.locator("#account-delete").click();
    assertThat(browserPage.locator("#account-lifecycle-password")).hasCount(0);
    browserPage.locator("#account-lifecycle-recovery-code-tab").click();
    browserPage
        .locator("#account-lifecycle-recovery-code input")
        .fill(TwoFactorPage.nextCode(browserPage, accountEmail));
    browserPage.locator("#account-lifecycle-submit").click();

    assertThat(browserPage).hasURL(urlPattern(LOGIN_ROUTE));
    new MailpitInbox(browserContext)
        .awaitSecurityMail(accountEmail, uiTexts.getAccountDeletedMailSubject());
  }

  private void confirmLifecycleWithPortalPassword() {
    browserPage
        .locator("#account-lifecycle-password input")
        .fill(BrowserTestAccounts.USER_PASSWORD);
    browserPage.locator("#account-lifecycle-submit").click();
  }

  private String account() {
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    String email = "account-" + UUID.randomUUID() + "@example.test";
    new AdminUsersPage(browserPage).createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
    new UserMenu(browserPage).signOut();
    login.open();
    login.signIn(email, BrowserTestAccounts.USER_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    return email;
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName(
      "Beim Wechsel des Kontobereichs kann ein Profilentwurf behalten oder verworfen werden")
  @Tag("form-details")
  void profileDraftSurvivesCancelledNavigationAndIsClearedOnDiscard(int viewportWidth) {
    browserPage.setViewportSize(viewportWidth, 900);
    account();
    AccountPage accountPage = new AccountPage(browserPage);
    UnsavedChangesPrompt unsavedChangesPrompt = new UnsavedChangesPrompt(browserPage, uiTexts);
    accountPage.open();
    Locator firstNameInputLocator = accountPage.input("account-first-name");
    String savedFirstName = firstNameInputLocator.inputValue();
    firstNameInputLocator.fill("Temporary draft");

    accountPage.section("account-tab-security");
    assertThat(unsavedChangesPrompt.root().locator("[part=overlay]").first())
        .hasCSS("background-color", "rgb(14, 23, 38)");
    assertThat(unsavedChangesPrompt.root().locator(".cosmos-panel-icon vaadin-icon"))
        .hasAttribute("src", "icons/tabler/file-diff.svg");
    assertThat(unsavedChangesPrompt.root().locator(".portal-confirm-explanation"))
        .hasCSS("font-size", "12px");
    assertThat(unsavedChangesPrompt.root().locator(".portal-confirm-explanation"))
        .hasCSS("font-weight", "400");
    assertThat(unsavedChangesPrompt.root().locator(".portal-confirm-explanation"))
        .hasCSS("color", "rgb(148, 163, 184)");
    Locator discardButtonLocator = unsavedChangesPrompt.root().locator(".cosmos-warning-button");
    Locator keepEditingButtonLocator =
        unsavedChangesPrompt.root().locator("vaadin-button[theme~=primary]");
    browserPage.waitForCondition(() -> {
      BoundingBox discardButtonBounds = discardButtonLocator.boundingBox();
      BoundingBox keepEditingButtonBounds = keepEditingButtonLocator.boundingBox();
      return Math.abs(discardButtonBounds.width - keepEditingButtonBounds.width) < 1
          && (viewportWidth > 600
              ? discardButtonBounds.x < keepEditingButtonBounds.x
              : discardButtonBounds.y < keepEditingButtonBounds.y);
    });
    assertThat(
            unsavedChangesPrompt
                .root()
                .getByRole(
                    AriaRole.BUTTON,
                    new Locator.GetByRoleOptions()
                        .setName(uiTexts.getUnsavedChangesKeepEditing())
                        .setExact(true)))
        .isFocused();
    AllurePlaywright.attachScreenshot(
        "Formularschutz im Konto bei " + viewportWidth + " px", browserPage);
    discardButtonLocator.hover();
    assertThat(discardButtonLocator).hasCSS("border-color", "rgb(245, 158, 11)");
    keepEditingButtonLocator.hover();
    assertThat(keepEditingButtonLocator).hasCSS("background-color", "rgb(0, 120, 180)");
    unsavedChangesPrompt.keepEditing();
    assertThat(firstNameInputLocator).hasValue("Temporary draft");

    accountPage.section("account-tab-security");
    unsavedChangesPrompt.root().locator(".cosmos-overlay-close").click();
    assertThat(unsavedChangesPrompt.root()).hasCount(0);
    assertThat(firstNameInputLocator).hasValue("Temporary draft");

    accountPage.section("account-tab-security");
    assertThat(unsavedChangesPrompt.root().locator(".cosmos-overlay-close")).isVisible();
    browserPage.keyboard().press("Escape");
    assertThat(unsavedChangesPrompt.root()).hasCount(0);
    assertThat(firstNameInputLocator).hasValue("Temporary draft");

    accountPage.section("account-tab-security");
    unsavedChangesPrompt.discard();
    accountPage.section("account-tab-profile");
    assertThat(firstNameInputLocator).hasValue(savedFirstName);
  }

  @Test
  @DisplayName("ACCOUNT-01: Basisdaten speichern, neu laden und erneut speichern")
  @Description(
      """
      1. Eigenes aktives Konto über das Adminformular anlegen und normal anmelden.
      2. Kontoseite über das Benutzermenü öffnen; Namen/Geburtstag ändern und speichern.
      3. Erfolgsmeldung, Übersicht und deaktivierte unveränderte E-Mail prüfen; Seite neu laden.
      4. Gespeicherte Werte prüfen und eine zweite Änderung speichern; erneut neu laden.
      **Erwartet:** Basisdaten dauerhaft gespeichert, wiederholtes Speichern möglich, E-Mail unverändert.
      """)
  void profilePersistsAndCanBeSavedTwice() {
    String email = account();
    AccountPage form = new AccountPage(browserPage);
    form.open();
    form.fill("Profile", "Saved", LocalDate.of(1992, 3, 4));
    form.host("account-gender").click();
    browserPage
        .getByRole(
            AriaRole.OPTION,
            new Page.GetByRoleOptions().setName(uiTexts.getGenderFemale()).setExact(true))
        .click();
    form.save();
    form.assertMessage(uiTexts.getAccountSettingsSaveSuccess());

    assertThat(browserPage.locator(".account-summary-name")).hasText("Profile Saved");
    assertThat(browserPage.locator(".cosmos-topbar .cosmos-avatar-initials")).hasText("PS");
    new UserMenu(browserPage).open();

    assertThat(browserPage.locator(".user-popover-avatar .cosmos-avatar-initials")).hasText("PS");
    new UserMenu(browserPage).close();

    assertThat(form.host("account-email")).hasJSProperty("readonly", true);
    assertThat(form.input("account-email")).isEnabled();
    assertThat(form.input("account-email")).hasAttribute("readonly", "");
    assertThat(form.input("account-email")).hasValue(email);
    browserPage.reload();

    assertThat(form.input("account-first-name")).hasValue("Profile");
    assertThat(form.input("account-last-name")).hasValue("Saved");
    assertThat(form.host("account-birth-date")).hasJSProperty("value", "1992-03-04");
    assertThat(form.input("account-gender")).hasValue(uiTexts.getGenderFemale());
    form.fill("Second", "Saved", LocalDate.of(1993, 4, 5));
    form.save();
    form.assertMessage(uiTexts.getAccountSettingsSaveSuccess());
    browserPage.reload();

    assertThat(form.input("account-first-name")).hasValue("Second");
    assertThat(browserPage.locator(".cosmos-topbar .cosmos-avatar-initials")).hasText("SS");
    assertThat(form.host("account-birth-date")).hasJSProperty("value", "1993-04-05");
    assertThat(form.input("account-email")).hasValue(email);
    form.input("account-birth-date").fill("");
    form.host("account-gender").locator("[part~='clear-button']").click();
    form.input("account-first-name").click();
    form.save();
    form.assertMessage(uiTexts.getAccountSettingsSaveSuccess());
    browserPage.reload();

    assertThat(form.input("account-birth-date")).hasValue("");
    assertThat(form.input("account-gender")).hasValue("");
  }

  @Test
  @DisplayName("ACCOUNT-02: Pflichtfelder und Geburtsdatum verhindern ungültiges Speichern")
  @Description(
      """
      1. Eigenes Konto öffnen und Namen leeren; speichern: rote Felder und Pflichtfeldmeldung.
      2. Namen korrigieren, heutigen und zukünftigen Geburtstag einzeln speichern.
      3. Jeweils rote Datumsmarkierung und Vergangenheitsregel prüfen; Seite neu laden.
      **Erwartet:** Keine ungültigen Änderungen gespeichert; vorherige Werte bleiben erhalten.
      """)
  void invalidProfileNeverPersists() {
    account();
    AccountPage form = new AccountPage(browserPage);
    form.open();
    String original = form.input("account-first-name").inputValue();
    form.input("account-first-name").fill(" ");
    form.input("account-last-name").fill("");
    form.save();
    form.assertMessage(uiTexts.getBaseValidationRequiredMessage());

    assertThat(form.host("account-first-name")).hasAttribute("invalid", "");
    assertThat(form.host("account-last-name")).hasAttribute("invalid", "");
    form.closeNotification();
    for (LocalDate date :
        new LocalDate[] {
          LocalDate.now(ZoneId.of("Europe/Berlin")),
          LocalDate.now(ZoneId.of("Europe/Berlin")).plusDays(1)
        }) {
      form.fill("NotSaved", "Invalid", date);
      form.save();
      form.assertMessage(uiTexts.getBaseValidationBirthDatePastMessage());

      assertThat(form.host("account-birth-date")).hasAttribute("invalid", "");
      form.closeNotification();
    }
    browserPage.reload();

    assertThat(form.input("account-first-name")).hasValue(original);
    assertThat(form.host("account-birth-date")).hasJSProperty("value", "1990-01-01");
  }

  @Test
  @DisplayName("ACCOUNT-03: Gespeicherte Sprache gilt auch nach erneuter Anmeldung")
  @Description(
      """
      1. Eigenes Konto öffnen, Gegen-Sprache im Profil wählen und speichern.
      2. Erfolg und übersetzte Kontoseite prüfen; abmelden und erneut anmelden.
      3. Ohne Sprachkorrektur im Test die gespeicherte Sprache im Benutzermenü prüfen.
      **Erwartet:** Speicherung ändert die Sitzungssprache und die nächste Anmeldung übernimmt sie.
      """)
  void savedLanguageSurvivesNewLogin() {
    String email = account();
    AccountPage form = new AccountPage(browserPage);
    form.open();
    String other = uiTexts.language().equals("de") ? "en" : "de";
    UiTexts translated = new UiTexts(other);
    form.language(other);
    form.save();
    form.assertMessage(translated.getAccountSettingsSaveSuccess());

    assertThat(browserPage.locator(".account-settings"))
        .containsText(translated.getAccountSettingsTitle());
    // Logout mit der tatsächlich gespeicherten Sprache, ohne sie vor der Prüfung
    // zurückzustellen.
    new UserMenu(browserPage).open();
    UiActions.complete(
        browserPage,
        () ->
            browserPage
                .getByRole(
                    AriaRole.BUTTON,
                    new Page.GetByRoleOptions()
                        .setName(translated.getBaseLogoutButton())
                        .setExact(true))
                .click());
    new LoginPage(browserPage).assertFormVisible();
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(email, BrowserTestAccounts.USER_PASSWORD);
    new UserMenu(browserPage).open();

    assertThat(
            browserPage.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                    .setName(translated.getBaseLogoutButton())
                    .setExact(true)))
        .isVisible();
    // Die gemeinsame Fixture meldet das Konto anschließend ab und stellt die Testsprache
    // zurück.
  }

  @ParameterizedTest
  @CsvSource({
    "icon, ACCOUNT-04A",
    "cancel, ACCOUNT-04B",
    "escape, ACCOUNT-04C",
    "outside, ACCOUNT-04D"
  })
  @DisplayName(
      "ACCOUNT-04A–D: E-Mail-Dialog öffnen, schließen und ohne sensible Entwürfe wiederöffnen")
  @Description(
      """
      1. Eigenes Konto öffnen, Profilnamen bearbeiten und Navigation zunächst abbrechen. Profil speichern und Tabs wechseln.
      2. Fehlenden Profil-Ändern-Button prüfen; E-Mail-Dialog unter Sicherheit öffnen: lokalisierte Felder, Bestätigung, Passwort, Info und Blur prüfen.
      3. Erfundenen E-Mail-/Passwortentwurf eingeben; per Icon, Abbrechen, Escape oder Außenklick schließen.
      4. Unter Sicherheit erneut öffnen: beide E-Mail-Felder und Passwort leer, gespeicherter Profilname unverändert.
      **Erwartet:** Sicherheit öffnet den Dialog; Schließen verwirft nur Dialogeingaben. Kein echter E-Mail-Wechsel.
      """)
  void emailDraftClosesWithoutChangingTheProfile(String closeAction, String caseId) {
    String email = account();
    AccountPage form = new AccountPage(browserPage);
    form.open();
    form.input("account-first-name").fill("Draft");
    form.host("account-tab-security").click();
    new UnsavedChangesPrompt(browserPage, uiTexts).keepEditing();

    assertThat(form.input("account-first-name")).hasValue("Draft");
    form.save();
    form.assertMessage(uiTexts.getAccountSettingsSaveSuccess());
    form.closeNotification();
    assertThat(form.host("account-email-edit-profile")).hasCount(0);
    form.openEmail();

    assertThat(form.host("account-email-new"))
        .hasJSProperty("label", uiTexts.getAccountEmailDialogNewEmail());
    assertThat(form.host("account-email-confirm"))
        .hasJSProperty("label", uiTexts.getAccountEmailDialogConfirmEmail());
    assertThat(form.host("account-email-password"))
        .hasJSProperty("label", uiTexts.getAccountEmailDialogPassword());
    assertThat(form.dialog()).containsText(uiTexts.getAccountEmailDialogHint());
    assertThat(form.host("account-email-request")).hasText(uiTexts.getAccountEmailDialogRequest());
    assertThat(form.host("account-email-request")).isEnabled();
    assertThat(form.host("account-email-request").locator("vaadin-icon"))
        .hasAttribute("src", "icons/lucide/check.svg");
    assertEquals(
        "blur(3px)",
        form.dialog()
            .locator("[part~='backdrop']")
            .evaluate("e => getComputedStyle(e).backdropFilter"));
    form.input("account-email-new").fill("draft@example.test");
    form.input("account-email-confirm").fill("draft@example.test");
    form.input("account-email-password").fill("Local-Draft-Only!");
    form.closeEmail(closeAction);
    form.openEmail();

    assertThat(form.input("account-email-new")).hasValue("");
    assertThat(form.input("account-email-confirm")).hasValue("");
    assertThat(form.input("account-email-password")).hasValue("");
    form.closeEmail("cancel");
    form.host("account-tab-profile").click();

    assertThat(form.input("account-first-name")).hasValue("Draft");
    assertThat(form.input("account-email")).hasValue(email);
  }

  @ParameterizedTest
  @CsvSource({"1280,900", "390,844"})
  @DisplayName("AVATAR-01: Upload, Ausschnitt, Wiederöffnen, Initialen, Ersetzen und Löschen")
  @Description(
      "Eigenes Konto: Bild per Drag-and-drop und Dateiauswahl hochladen, zuschneiden und speichern; Kopfzeile, Popup und Konto prüfen. Die Modusauswahl reagiert auch am Rand der Fläche. Wiederöffnen behält den Bildmodus. Lokales Abbrechen und Footer-Abbrechen verwerfen Entwürfe; Initialen erhalten das Bild, Ersetzen und Löschen werden gespeichert. Desktop und schmale Ansicht.")
  void avatarPersistsAcrossAllModes(int width, int height) throws Exception {
    String email = account();
    browserPage.setViewportSize(width, height);
    AccountPage form = new AccountPage(browserPage);
    form.open();
    openAvatar();
    chooseMode(uiTexts.getAccountAvatarCustom());
    chooseMode(uiTexts.getAccountAvatarInitials());

    assertThat(browserPage.locator("account-avatar-editor")).hasAttribute("data-mode", "INITIALS");
    chooseMode(uiTexts.getAccountAvatarCustom());
    uploadAvatar(java.awt.Color.BLUE, true);

    assertThat(browserPage.locator(".account-avatar-upload")).isHidden();
    Locator reset = browserPage.locator("[data-avatar='reset']");
    Locator cancel = browserPage.locator("#account-avatar-cancel-selection");

    assertEquals(reset.boundingBox().width, cancel.boundingBox().width, 1);
    assertEquals(reset.boundingBox().y, cancel.boundingBox().y, 1);
    io.qameta.allure.playwright.AllurePlaywright.attachScreenshot(
        "Profilbilddialog mit lokalem Ausschnitt", browserPage);
    browserPage.locator("[data-avatar='zoom']").focus();
    browserPage.keyboard().press("ArrowRight");
    reset.click();

    assertThat(browserPage.locator("[data-avatar='percent']")).hasText("100 %");
    browserPage.locator("[data-avatar='zoom']").focus();
    browserPage.keyboard().press("ArrowRight");
    saveAvatar();
    assertPhotos();
    browserPage.reload();
    assertPhotos();
    try (BrowserActor administrator = new BrowserActor(testBrowser)) {
      AdminUsersPage users =
          administrator.loginAdmin(
              BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
      users.open();
      users.search(email);

      assertThat(users.emailCells(email)).hasCount(1);
      assertThat(users.grid().locator(".cosmos-avatar-photo:visible")).hasCount(1);
      assertThat(users.grid().locator(".cosmos-avatar-photo:visible")).isVisible();
      assertThat(users.grid().locator(".cosmos-avatar-status")).hasCount(0);
      new UserMenu(administrator.browserPage).signOut();
    }
    openAvatar();

    assertThat(browserPage.locator("account-avatar-editor")).hasAttribute("data-mode", "CUSTOM");
    assertThat(browserPage.locator(".account-avatar-upload")).isVisible();
    assertThat(browserPage.locator(".account-avatar-crop-controls")).isHidden();
    uploadAvatar(java.awt.Color.RED);
    cancel.click();

    assertThat(browserPage.locator(".account-avatar-upload")).isVisible();
    assertThat(browserPage.locator("account-avatar-editor"))
        .hasAttribute("data-local-image", "false");
    uploadAvatar(java.awt.Color.RED);
    browserPage.locator("#account-avatar-cancel").click();
    openAvatar();

    assertThat(browserPage.locator("account-avatar-editor"))
        .hasAttribute("data-local-image", "false");
    chooseMode(uiTexts.getAccountAvatarInitials());
    saveAvatar();

    assertThat(browserPage.locator(".account-summary-avatar .cosmos-avatar-photo")).isHidden();
    openAvatar();
    chooseMode(uiTexts.getAccountAvatarCustom());
    saveAvatar();
    assertPhotos();
    openAvatar();
    uploadAvatar(java.awt.Color.RED);
    saveAvatar();
    assertPhotos();
    io.qameta.allure.playwright.AllurePlaywright.attachScreenshot(
        "Gespeichertes Profilbild", browserPage);
    openAvatar();
    browserPage.locator("#account-avatar-delete").click();
    Locator confirmation = browserPage.locator("#account-avatar-delete-confirmation");

    assertThat(confirmation).hasJSProperty("opened", true);
    assertThat(confirmation).containsText(uiTexts.getAccountAvatarDeleteQuestion());
    assertThat(confirmation).containsText(uiTexts.getAccountAvatarDeleteHint());
    Locator contextIcon = confirmation.locator(".portal-context-card > .cosmos-panel-icon");

    assertThat(contextIcon).isVisible();
    assertEquals("12px", contextIcon.evaluate("e => getComputedStyle(e).borderRadius"));
    Locator hint = confirmation.locator(".portal-confirm-body > .cosmos-panel-hint");
    // Beide Mittelpunkte im selben Frame lesen; die Öffnungsanimation bewegt den Dialog.
    double centerOffset =
        ((Number)
                hint.evaluate(
                    """
                    element => {
                      const icon = element.querySelector('vaadin-icon').getBoundingClientRect();
                      const text = element.querySelector(':scope > span').getBoundingClientRect();
                      return (text.top + text.bottom - icon.top - icon.bottom) / 2;
                    }
                    """))
            .doubleValue();

    assertEquals(0, centerOffset, 1);
    io.qameta.allure.playwright.AllurePlaywright.attachScreenshot(
        "Profilbild löschen bestätigen", browserPage);
    browserPage.locator("#account-avatar-delete-confirmation-cancel").click();

    assertThat(browserPage.locator("account-avatar-editor")).hasAttribute("data-mode", "CUSTOM");
    browserPage.locator("#account-avatar-delete").click();
    browserPage.locator("#account-avatar-delete-confirmation-submit").click();

    assertThat(browserPage.locator("account-avatar-editor")).hasAttribute("data-mode", "INITIALS");
    browserPage.locator("#account-avatar-cancel").click();
    browserPage.reload();
    assertPhotos();
    openAvatar();
    browserPage.locator("#account-avatar-delete").click();
    browserPage.locator("#account-avatar-delete-confirmation-submit").click();
    saveAvatar();
    browserPage.reload();

    assertThat(browserPage.locator(".account-summary-avatar .cosmos-avatar-photo")).isHidden();
    openAvatar();

    assertThat(browserPage.locator("#account-avatar-delete")).isHidden();
    browserPage.locator("#account-avatar-cancel").click();
  }

  @Test
  @DisplayName("AVATAR-02: Beschädigten Bildupload ablehnen und gültiges Bild speichern")
  @Description(
      """
      1. Eigenes Konto anlegen, Profilbilddialog öffnen und Bildmodus wählen.
      2. Eine als PNG bezeichnete Datei mit ungültigen Bilddaten hochladen.
      3. Lokalisierte Bildfehlermeldung und fehlenden lokalen Ausschnitt prüfen.
      4. Im selben Dialog ein gültiges PNG wählen, speichern und Seite neu laden.
      **Erwartet:** Ungültige Daten werden abgelehnt; der Dialog bleibt bedienbar,
      nur das anschließend gültige Profilbild wird dauerhaft angezeigt.
      """)
  void corruptAvatarCanBeCorrectedWithoutReopeningDialog() throws Exception {
    account();
    AccountPage form = new AccountPage(browserPage);
    form.open();
    openAvatar();
    chooseMode(uiTexts.getAccountAvatarCustom());
    browserPage
        .waitForFileChooser(() -> browserPage.locator("#account-avatar-choose").click())
        .setFiles(
            new com.microsoft.playwright.options.FilePayload(
                "broken.png", "image/png", new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10}));

    assertThat(browserPage.locator("[data-avatar='error']"))
        .hasText(uiTexts.getAccountAvatarImageError());
    assertThat(browserPage.locator("account-avatar-editor"))
        .hasAttribute("data-local-image", "false");
    assertThat(browserPage.locator(".account-avatar-crop-controls")).isHidden();
    form.assertMessage(uiTexts.getAccountAvatarImageError());
    form.closeNotification();
    uploadAvatar(java.awt.Color.BLUE);

    assertThat(browserPage.locator("[data-avatar='error']")).isEmpty();
    saveAvatar();
    browserPage.reload();
    assertPhotos();
  }

  private void openAvatar() {
    new AccountPage(browserPage).section("account-avatar-edit");

    assertThat(browserPage.locator("#account-avatar-choose")).isAttached();
  }

  private void chooseMode(String label) {
    Locator radio =
        browserPage
            .locator("#account-avatar-mode vaadin-radio-button")
            .filter(new Locator.FilterOptions().setHasText(label));

    assertThat(radio).isVisible();
    radio.click(new Locator.ClickOptions().setPosition(radio.boundingBox().width - 3, 3));
  }

  private void uploadAvatar(java.awt.Color color) throws Exception {
    uploadAvatar(color, false);
  }

  private void uploadAvatar(java.awt.Color color, boolean drop) throws Exception {
    BufferedImage image =
        new java.awt.image.BufferedImage(160, 120, java.awt.image.BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = image.createGraphics();
    graphics.setColor(color);
    graphics.fillRect(0, 0, 160, 120);
    graphics.setColor(java.awt.Color.WHITE);
    graphics.fillRect(60, 40, 40, 40);
    graphics.dispose();
    ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
    javax.imageio.ImageIO.write(image, color.equals(java.awt.Color.RED) ? "jpeg" : "png", bytes);
    String filename = color.equals(java.awt.Color.RED) ? "fixture.jpeg" : "fixture.png";
    String contentType = color.equals(java.awt.Color.RED) ? "image/jpeg" : "image/png";
    if (drop) {
      JSHandle transfer =
          browserPage.evaluateHandle(
              """
              data => {
                const bytes = Uint8Array.from(atob(data.bytes), c => c.charCodeAt(0));
                const transfer = new DataTransfer();
                transfer.items.add(new File([bytes], data.filename, { type: data.contentType }));
                return transfer;
              }
              """,
              Map.of(
                  "bytes", Base64.getEncoder().encodeToString(bytes.toByteArray()),
                  "filename", filename,
                  "contentType", contentType));
      try {
        Locator upload = browserPage.locator("#account-avatar-upload");
        upload.dispatchEvent("dragover", Map.of("dataTransfer", transfer));

        assertThat(upload).hasAttribute("dragover-valid", "true");
        upload.dispatchEvent("drop", Map.of("dataTransfer", transfer));
      } finally {
        transfer.dispose();
      }
    } else {
      browserPage
          .waitForFileChooser(() -> browserPage.locator("#account-avatar-choose").click())
          .setFiles(
              new com.microsoft.playwright.options.FilePayload(
                  filename, contentType, bytes.toByteArray()));
    }
    assertThat(browserPage.locator("account-avatar-editor"))
        .hasAttribute("data-local-image", "true");
    assertThat(browserPage.locator(".account-avatar-crop-controls")).isVisible();
  }

  private void saveAvatar() {
    UiActions.complete(browserPage, () -> browserPage.locator("#account-avatar-save").click());

    assertThat(browserPage.locator("vaadin-dialog.account-avatar-dialog[opened]")).hasCount(0);
    new AccountPage(browserPage).assertMessage(uiTexts.getAccountAvatarSaved());
    new AccountPage(browserPage).closeNotification();
  }

  private void assertPhotos() {
    Locator navigationToggle = browserPage.locator("#account-navigation-toggle");

    assertThat(navigationToggle).hasCount(1);
    boolean mobileNavigation = navigationToggle.isVisible();
    if (mobileNavigation) {
      if (!"true".equals(navigationToggle.getAttribute("aria-expanded"))) {
        UiActions.complete(browserPage, navigationToggle::click);
      }
      assertThat(navigationToggle).hasAttribute("aria-expanded", "true");
    }
    for (String selector : new String[] {".account-summary-avatar", ".cosmos-topbar"}) {
      Locator image = browserPage.locator(selector + " .cosmos-avatar-photo");

      assertThat(image).isVisible();
      assertThat(image).hasJSProperty("complete", true);
      assertTrue(((Number) image.evaluate("e => e.naturalWidth")).intValue() > 0);
    }
    if (mobileNavigation) {
      UiActions.complete(browserPage, navigationToggle::click);

      assertThat(navigationToggle).hasAttribute("aria-expanded", "false");
    }
    new UserMenu(browserPage).open();

    assertThat(browserPage.locator(".user-popover-avatar .cosmos-avatar-photo")).isVisible();
    new UserMenu(browserPage).close();
  }

  @Test
  @DisplayName("INPUT-01: Kompakte Geburtstage werden bei Blur und Enter lokalisiert übernommen")
  @Description(
      "Im echten Kontodatumsfeld vier/sechs/acht Ziffern und 1950/2049 prüfen; ungültiger Tag bleibt ungültig. Enter und Tab verwenden den geladenen Frontend-Parser in DE/EN.")
  void compactBirthDatesUseTheRealFrontendParser() {
    account();
    AccountPage form = new AccountPage(browserPage);
    form.open();
    boolean german = uiTexts.language().equals("de");
    String[][] samples = {
      {"4586", "5486", "1986-05-04"},
      {"260486", "042686", "1986-04-26"},
      {"26041986", "04261986", "1986-04-26"},
      {"010150", "010150", "1950-01-01"},
      {"010149", "010149", "2049-01-01"},
      {"1.1.50", "1/1/50", "1950-01-01"},
      {"1.1.49", "1/1/49", "2049-01-01"}
    };
    for (int dateSampleIndex = 0; dateSampleIndex < samples.length; dateSampleIndex++) {
      form.input("account-birth-date").fill(samples[dateSampleIndex][german ? 0 : 1]);
      form.input("account-birth-date").press(dateSampleIndex % 2 == 0 ? "Tab" : "Enter");

      assertThat(form.host("account-birth-date"))
          .hasJSProperty("value", samples[dateSampleIndex][2]);
      assertThat(form.input("account-birth-date"))
          .hasValue(uiTexts.dateInput(LocalDate.parse(samples[dateSampleIndex][2])));
    }
    form.input("account-birth-date").fill(german ? "310286" : "023186");
    form.input("account-birth-date").press("Tab");

    assertThat(form.host("account-birth-date")).hasAttribute("invalid", "");
  }

  @Test
  @DisplayName("STATUS-03: Status direkt am Kontobild mit Kontursymbol und gleichem Rahmen")
  @Description(
      "Am Kontobild jeden Status auswählen; Auswahlhintergrund ohne Haken, Kontursymbol, Rahmen und Glow in Konto, Kopfzeile und Popup prüfen, danach neu laden.")
  void accountAvatarStatusIsSynchronized() {
    account();
    new AccountPage(browserPage).open();
    Locator statusButton = browserPage.locator("#account-avatar-status vaadin-select-value-button");
    statusButton.focus();

    assertThat(statusButton).isFocused();
    statusButton.press("Space");

    assertThat(
            browserPage
                .getByRole(AriaRole.OPTION)
                .filter(
                    new com.microsoft.playwright.Locator.FilterOptions()
                        .setHas(
                            browserPage.getByText(
                                uiTexts.getUserStatusAvailable(),
                                new Page.GetByTextOptions().setExact(true)))))
        .isVisible();
    browserPage.keyboard().press("Escape");
    for (String label :
        new String[] {
          uiTexts.getUserStatusEmployed(),
          uiTexts.getUserStatusAbsent(),
          uiTexts.getUserStatusOffline(),
          uiTexts.getUserStatusAvailable()
        }) {
      browserPage.locator("#account-avatar-status").click();
      Locator option =
          browserPage
              .getByRole(AriaRole.OPTION)
              .filter(
                  new com.microsoft.playwright.Locator.FilterOptions()
                      .setHas(
                          browserPage.getByText(
                              label, new Page.GetByTextOptions().setExact(true))));

      assertThat(option.locator("svg")).hasCount(1);
      UiActions.completeValueChange(browserPage, option::click);
      browserPage.locator("#account-avatar-status").click();
      Locator selected =
          browserPage
              .getByRole(AriaRole.OPTION)
              .filter(
                  new com.microsoft.playwright.Locator.FilterOptions()
                      .setHas(
                          browserPage.getByText(
                              label, new Page.GetByTextOptions().setExact(true))));

      assertThat(selected).hasAttribute("selected", java.util.regex.Pattern.compile(".*"));
      assertThat(selected.locator("[part~='checkmark']")).isHidden();
      assertNotEquals(
          "rgba(0, 0, 0, 0)", selected.evaluate("e => getComputedStyle(e).backgroundColor"));
      io.qameta.allure.playwright.AllurePlaywright.attachScreenshot(
          "Statusauswahl am Kontobild", browserPage);
      browserPage.keyboard().press("Escape");
      Locator circle = browserPage.locator(".account-summary-avatar .cosmos-avatar-circle");

      assertEquals("2px", circle.evaluate("e => getComputedStyle(e).borderTopWidth"));
      assertEquals("none", circle.evaluate("e => getComputedStyle(e).boxShadow"));
      String color = (String) circle.evaluate("e => getComputedStyle(e).borderTopColor");

      assertEquals(
          color,
          browserPage
              .locator(".cosmos-topbar .cosmos-avatar-circle")
              .evaluate("e => getComputedStyle(e).borderTopColor"));
      new UserStatusMenu(browserPage).expect(label);

      assertEquals(
          color,
          browserPage
              .locator(".user-popover-avatar .cosmos-avatar-circle")
              .evaluate("e => getComputedStyle(e).borderTopColor"));
      new UserMenu(browserPage).close();
    }
    browserPage.reload();

    assertThat(browserPage.locator("#account-avatar-status"))
        .containsText(uiTexts.getUserStatusAvailable());
  }
}
