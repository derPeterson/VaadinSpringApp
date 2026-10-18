package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.*;
import de.derpeterson.browsertests.i18n.UiTexts;
import de.derpeterson.browsertests.pages.*;
import io.qameta.allure.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Tag("browser")
@Feature("Kontoverwaltung: Profil und E-Mail-Dialogentwurf")
public class AccountSettingsBrowserTest extends BrowserFixture {
  private String account() {
    var login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    String email = "account-" + UUID.randomUUID() + "@example.test";
    new AdminUsersPage(page).createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
    new UserMenu(page).signOut();
    login.open();
    login.signIn(email, BrowserTestAccounts.USER_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    return email;
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
    var form = new AccountPage(page);
    form.open();
    form.fill("Profile", "Saved", LocalDate.of(1992, 3, 4));
    form.save();
    form.assertMessage(texts.getAccountSettingsSaveSuccess());
    assertThat(page.locator(".account-summary-name")).hasText("Profile Saved");
    assertThat(page.locator(".cosmos-topbar .cosmos-avatar-initials")).hasText("PS");
    new UserMenu(page).open();
    assertThat(page.locator(".user-popover-avatar .cosmos-avatar-initials")).hasText("PS");
    new UserMenu(page).close();
    assertThat(form.host("account-email")).hasJSProperty("disabled", true);
    assertThat(form.input("account-email")).isDisabled();
    assertThat(form.input("account-email")).hasValue(email);
    page.reload();
    assertThat(form.input("account-first-name")).hasValue("Profile");
    assertThat(form.input("account-last-name")).hasValue("Saved");
    assertThat(form.host("account-birth-date")).hasJSProperty("value", "1992-03-04");
    form.fill("Second", "Saved", LocalDate.of(1993, 4, 5));
    form.save();
    form.assertMessage(texts.getAccountSettingsSaveSuccess());
    page.reload();
    assertThat(form.input("account-first-name")).hasValue("Second");
    assertThat(page.locator(".cosmos-topbar .cosmos-avatar-initials")).hasText("SS");
    assertThat(form.host("account-birth-date")).hasJSProperty("value", "1993-04-05");
    assertThat(form.input("account-email")).hasValue(email);
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
    var form = new AccountPage(page);
    form.open();
    String original = form.input("account-first-name").inputValue();
    form.input("account-first-name").fill(" ");
    form.input("account-last-name").fill("");
    form.save();
    form.assertMessage(texts.getBaseValidationRequiredMessage());
    assertThat(form.host("account-first-name")).hasAttribute("invalid", "");
    assertThat(form.host("account-last-name")).hasAttribute("invalid", "");
    form.closeNotification();
    for (var date :
        new LocalDate[] {
          LocalDate.now(ZoneId.of("Europe/Berlin")),
          LocalDate.now(ZoneId.of("Europe/Berlin")).plusDays(1)
        }) {
      form.fill("NotSaved", "Invalid", date);
      form.save();
      form.assertMessage(texts.getBaseValidationBirthDatePastMessage());
      assertThat(form.host("account-birth-date")).hasAttribute("invalid", "");
      form.closeNotification();
    }
    page.reload();
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
    var form = new AccountPage(page);
    form.open();
    String other = texts.language().equals("de") ? "en" : "de";
    UiTexts translated = new UiTexts(other);
    form.language(other);
    form.save();
    form.assertMessage(translated.getAccountSettingsSaveSuccess());
    assertThat(page.locator(".account-settings"))
        .containsText(translated.getAccountSettingsTitle());
    // Logout mit der tatsächlich gespeicherten Sprache, ohne sie vor der Prüfung
    // zurückzustellen.
    new UserMenu(page).open();
    UiActions.complete(
        page,
        () ->
            page.getByRole(
                    AriaRole.BUTTON,
                    new Page.GetByRoleOptions()
                        .setName(translated.getBaseLogoutButton())
                        .setExact(true))
                .click());
    new LoginPage(page).assertFormVisible();
    var login = new LoginPage(page);
    login.open();
    login.signIn(email, BrowserTestAccounts.USER_PASSWORD);
    new UserMenu(page).open();
    assertThat(
            page.getByRole(
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
      1. Eigenes Konto öffnen und ungespeicherten Profilnamen eingeben; Tabs wechseln.
      2. Fehlenden Profil-Ändern-Button prüfen; E-Mail-Dialog unter Sicherheit öffnen: lokalisierte Felder, Bestätigung, Passwort, Info und Blur prüfen.
      3. Erfundenen E-Mail-/Passwortentwurf eingeben; per Icon, Abbrechen, Escape oder Außenklick schließen.
      4. Unter Sicherheit erneut öffnen: beide E-Mail-Felder und Passwort leer, Profilentwurf unverändert.
      **Erwartet:** Sicherheit öffnet den Dialog; Schließen verwirft nur Dialogeingaben. Kein echter E-Mail-Wechsel.
      """)
  void emailDraftClosesWithoutChangingTheProfile(String close, String caseId) {
    String email = account();
    var form = new AccountPage(page);
    form.open();
    form.input("account-first-name").fill("Draft");
    form.host("account-tab-security").click();
    form.host("account-tab-profile").click();
    assertThat(form.input("account-first-name")).hasValue("Draft");
    assertThat(form.host("account-email-edit-profile")).hasCount(0);
    form.openEmail();
    assertThat(form.host("account-email-new"))
        .hasJSProperty("label", texts.getAccountEmailDialogNewEmail());
    assertThat(form.host("account-email-confirm"))
        .hasJSProperty("label", texts.getAccountEmailDialogConfirmEmail());
    assertThat(form.host("account-email-password"))
        .hasJSProperty("label", texts.getAccountEmailDialogPassword());
    assertThat(form.dialog()).containsText(texts.getAccountEmailDialogHint());
    assertThat(form.host("account-email-request")).hasText(texts.getAccountEmailDialogRequest());
    assertThat(form.host("account-email-request")).isEnabled();
    assertThat(form.host("account-email-request").locator("vaadin-icon"))
        .hasAttribute("icon", "vaadin:check");
    assertEquals(
        "blur(6px)",
        form.dialog()
            .locator("[part~='backdrop']")
            .evaluate("e => getComputedStyle(e).backdropFilter"));
    form.input("account-email-new").fill("draft@example.test");
    form.input("account-email-confirm").fill("draft@example.test");
    form.input("account-email-password").fill("Local-Draft-Only!");
    form.closeEmail(close);
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
      "Eigenes Konto: Bild über Dateiauswahl hochladen, zuschneiden und speichern; Kopfzeile, Popup und Konto prüfen. Wiederöffnen behält den Bildmodus. Lokales Abbrechen und Footer-Abbrechen verwerfen Entwürfe; Initialen erhalten das Bild, Ersetzen und Löschen werden gespeichert. Desktop und schmale Ansicht.")
  void avatarPersistsAcrossAllModes(int width, int height) throws Exception {
    String email = account();
    page.setViewportSize(width, height);
    var form = new AccountPage(page);
    form.open();
    openAvatar();
    chooseMode(texts.getAccountAvatarCustom());
    uploadAvatar(java.awt.Color.BLUE);
    assertThat(page.locator(".account-avatar-upload")).isHidden();
    var reset = page.locator("[data-avatar='reset']");
    var cancel = page.locator("#account-avatar-cancel-selection");
    assertEquals(reset.boundingBox().width, cancel.boundingBox().width, 1);
    assertEquals(reset.boundingBox().y, cancel.boundingBox().y, 1);
    io.qameta.allure.playwright.AllurePlaywright.attachScreenshot(
        "Profilbilddialog mit lokalem Ausschnitt", page);
    page.locator("[data-avatar='zoom']").focus();
    page.keyboard().press("ArrowRight");
    reset.click();
    assertThat(page.locator("[data-avatar='percent']")).hasText("100 %");
    page.locator("[data-avatar='zoom']").focus();
    page.keyboard().press("ArrowRight");
    saveAvatar();
    assertPhotos();
    page.reload();
    assertPhotos();
    try (BrowserActor administrator = new BrowserActor(browser)) {
      var users =
          administrator.loginAdmin(
              BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
      users.open();
      users.search(email);
      assertThat(users.emailCells(email)).hasCount(1);
      assertThat(users.grid().locator(".cosmos-avatar-photo:visible")).hasCount(1);
      assertThat(users.grid().locator(".cosmos-avatar-photo:visible")).isVisible();
      assertThat(users.grid().locator(".cosmos-avatar-status")).hasCount(0);
      new UserMenu(administrator.page).signOut();
    }
    openAvatar();
    assertThat(page.locator("account-avatar-editor")).hasAttribute("data-mode", "CUSTOM");
    assertThat(page.locator(".account-avatar-upload")).isVisible();
    assertThat(page.locator(".account-avatar-crop-controls")).isHidden();
    uploadAvatar(java.awt.Color.RED);
    cancel.click();
    assertThat(page.locator(".account-avatar-upload")).isVisible();
    assertThat(page.locator("account-avatar-editor")).hasAttribute("data-local-image", "false");
    uploadAvatar(java.awt.Color.RED);
    page.locator("#account-avatar-cancel").click();
    openAvatar();
    assertThat(page.locator("account-avatar-editor")).hasAttribute("data-local-image", "false");
    chooseMode(texts.getAccountAvatarInitials());
    saveAvatar();
    assertThat(page.locator(".account-summary-avatar .cosmos-avatar-photo")).isHidden();
    openAvatar();
    chooseMode(texts.getAccountAvatarCustom());
    saveAvatar();
    assertPhotos();
    openAvatar();
    uploadAvatar(java.awt.Color.RED);
    saveAvatar();
    assertPhotos();
    io.qameta.allure.playwright.AllurePlaywright.attachScreenshot("Gespeichertes Profilbild", page);
    openAvatar();
    page.locator("#account-avatar-delete").click();
    saveAvatar();
    page.reload();
    assertThat(page.locator(".account-summary-avatar .cosmos-avatar-photo")).isHidden();
    openAvatar();
    assertThat(page.locator("#account-avatar-delete")).isHidden();
    page.locator("#account-avatar-cancel").click();
  }

  private void openAvatar() {
    page.locator("#account-avatar-edit").click();
    assertThat(page.locator("#account-avatar-choose")).isAttached();
  }

  private void chooseMode(String label) {
    page.getByRole(AriaRole.RADIO, new Page.GetByRoleOptions().setName(label).setExact(true))
        .click();
  }

  private void uploadAvatar(java.awt.Color color) throws Exception {
    var image =
        new java.awt.image.BufferedImage(160, 120, java.awt.image.BufferedImage.TYPE_INT_RGB);
    var graphics = image.createGraphics();
    graphics.setColor(color);
    graphics.fillRect(0, 0, 160, 120);
    graphics.setColor(java.awt.Color.WHITE);
    graphics.fillRect(60, 40, 40, 40);
    graphics.dispose();
    var bytes = new java.io.ByteArrayOutputStream();
    javax.imageio.ImageIO.write(image, color.equals(java.awt.Color.RED) ? "jpeg" : "png", bytes);
    page.waitForFileChooser(() -> page.locator("#account-avatar-choose").click())
        .setFiles(
            new com.microsoft.playwright.options.FilePayload(
                color.equals(java.awt.Color.RED) ? "fixture.jpeg" : "fixture.png",
                color.equals(java.awt.Color.RED) ? "image/jpeg" : "image/png",
                bytes.toByteArray()));
    assertThat(page.locator("account-avatar-editor")).hasAttribute("data-local-image", "true");
    assertThat(page.locator(".account-avatar-crop-controls")).isVisible();
  }

  private void saveAvatar() {
    UiActions.complete(page, () -> page.locator("#account-avatar-save").click());
    assertThat(page.locator("vaadin-dialog.account-avatar-dialog[opened]")).hasCount(0);
    new AccountPage(page).assertMessage(texts.getAccountAvatarSaved());
    new AccountPage(page).closeNotification();
  }

  private void assertPhotos() {
    for (String selector : new String[] {".account-summary-avatar", ".cosmos-topbar"}) {
      var image = page.locator(selector + " .cosmos-avatar-photo");
      assertThat(image).isVisible();
      assertThat(image).hasJSProperty("complete", true);
      assertTrue(((Number) image.evaluate("e => e.naturalWidth")).intValue() > 0);
    }
    new UserMenu(page).open();
    assertThat(page.locator(".user-popover-avatar .cosmos-avatar-photo")).isVisible();
    new UserMenu(page).close();
  }

  @Test
  @DisplayName("INPUT-01: Kompakte Geburtstage werden bei Blur und Enter lokalisiert übernommen")
  @Description(
      "Im echten Kontodatumsfeld vier/sechs/acht Ziffern und 1950/2049 prüfen; ungültiger Tag bleibt ungültig. Enter und Tab verwenden den geladenen Frontend-Parser in DE/EN.")
  void compactBirthDatesUseTheRealFrontendParser() {
    account();
    var form = new AccountPage(page);
    form.open();
    boolean german = texts.language().equals("de");
    String[][] samples = {
      {"4586", "5486", "1986-05-04"},
      {"260486", "042686", "1986-04-26"},
      {"26041986", "04261986", "1986-04-26"},
      {"010150", "010150", "1950-01-01"},
      {"010149", "010149", "2049-01-01"},
      {"1.1.50", "1/1/50", "1950-01-01"},
      {"1.1.49", "1/1/49", "2049-01-01"}
    };
    for (int i = 0; i < samples.length; i++) {
      form.input("account-birth-date").fill(samples[i][german ? 0 : 1]);
      form.input("account-birth-date").press(i % 2 == 0 ? "Tab" : "Enter");
      assertThat(form.host("account-birth-date")).hasJSProperty("value", samples[i][2]);
      assertThat(form.input("account-birth-date"))
          .hasValue(texts.dateInput(LocalDate.parse(samples[i][2])));
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
    new AccountPage(page).open();
    var statusButton = page.locator("#account-avatar-status vaadin-select-value-button");
    statusButton.focus();
    assertThat(statusButton).isFocused();
    statusButton.press("Space");
    assertThat(
            page.getByRole(AriaRole.OPTION)
                .filter(
                    new com.microsoft.playwright.Locator.FilterOptions()
                        .setHas(
                            page.getByText(
                                texts.getUserStatusAvailable(),
                                new Page.GetByTextOptions().setExact(true)))))
        .isVisible();
    page.keyboard().press("Escape");
    for (String label :
        new String[] {
          texts.getUserStatusEmployed(),
          texts.getUserStatusAbsent(),
          texts.getUserStatusOffline(),
          texts.getUserStatusAvailable()
        }) {
      page.locator("#account-avatar-status").click();
      var option =
          page.getByRole(AriaRole.OPTION)
              .filter(
                  new com.microsoft.playwright.Locator.FilterOptions()
                      .setHas(page.getByText(label, new Page.GetByTextOptions().setExact(true))));
      assertThat(option.locator("svg")).hasCount(1);
      UiActions.completeValueChange(page, () -> option.click());
      page.locator("#account-avatar-status").click();
      var selected =
          page.getByRole(AriaRole.OPTION)
              .filter(
                  new com.microsoft.playwright.Locator.FilterOptions()
                      .setHas(page.getByText(label, new Page.GetByTextOptions().setExact(true))));
      assertThat(selected).hasAttribute("selected", java.util.regex.Pattern.compile(".*"));
      assertThat(selected.locator("[part~='checkmark']")).isHidden();
      assertNotEquals(
          "rgba(0, 0, 0, 0)", selected.evaluate("e => getComputedStyle(e).backgroundColor"));
      io.qameta.allure.playwright.AllurePlaywright.attachScreenshot(
          "Statusauswahl am Kontobild", page);
      page.keyboard().press("Escape");
      var circle = page.locator(".account-summary-avatar .cosmos-avatar-circle");
      assertEquals("2px", circle.evaluate("e => getComputedStyle(e).borderTopWidth"));
      assertNotEquals("none", circle.evaluate("e => getComputedStyle(e).boxShadow"));
      String color = (String) circle.evaluate("e => getComputedStyle(e).borderTopColor");
      assertEquals(
          color,
          page.locator(".cosmos-topbar .cosmos-avatar-circle")
              .evaluate("e => getComputedStyle(e).borderTopColor"));
      new UserStatusMenu(page).expect(label);
      assertEquals(
          color,
          page.locator(".user-popover-avatar .cosmos-avatar-circle")
              .evaluate("e => getComputedStyle(e).borderTopColor"));
      new UserMenu(page).close();
    }
    page.reload();
    assertThat(page.locator("#account-avatar-status")).containsText(texts.getUserStatusAvailable());
  }
}
