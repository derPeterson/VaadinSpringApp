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
    // Logout mit der tatsächlich gespeicherten Sprache, ohne sie vor der Prüfung zurückzustellen.
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
    // Die gemeinsame Fixture meldet das Konto anschließend ab und stellt die Testsprache zurück.
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
      2. E-Mail-Dialog im Profil öffnen: lokalisierte Felder, Bestätigung, Passwort, Info und Blur prüfen.
      3. Erfundenen E-Mail-/Passwortentwurf eingeben; per Icon, Abbrechen, Escape oder Außenklick schließen.
      4. Über Sicherheit erneut öffnen: beide E-Mail-Felder und Passwort leer, Profilentwurf unverändert.
      **Erwartet:** Beide Einstiegspunkte funktionieren; Schließen verwirft nur Dialogeingaben. Kein echter E-Mail-Wechsel.
      """)
  void emailDraftClosesWithoutChangingTheProfile(String close, String caseId) {
    String email = account();
    var form = new AccountPage(page);
    form.open();
    form.input("account-first-name").fill("Draft");
    form.host("account-tab-security").click();
    form.host("account-tab-profile").click();
    assertThat(form.input("account-first-name")).hasValue("Draft");
    form.openEmail(false);
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
    form.openEmail(true);
    assertThat(form.input("account-email-new")).hasValue("");
    assertThat(form.input("account-email-confirm")).hasValue("");
    assertThat(form.input("account-email-password")).hasValue("");
    form.closeEmail("cancel");
    form.host("account-tab-profile").click();
    assertThat(form.input("account-first-name")).hasValue("Draft");
    assertThat(form.input("account-email")).hasValue(email);
  }
}
