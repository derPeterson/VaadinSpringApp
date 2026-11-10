package de.derpeterson.browsertests.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import io.qameta.allure.Step;

/** Sicherheitsdialoge ausschließlich über ihre sichtbaren Felder und Buttons. */
public final class AccountSecurityPage {
  private final Page browserPage;

  public AccountSecurityPage(Page browserPage) {
    this.browserPage = browserPage;
  }

  public Locator input(String id) {
    return browserPage.locator("#" + id + " input");
  }

  public Locator host(String id) {
    return browserPage.locator("#" + id);
  }

  @Step("Passwortdialog unter Sicherheit öffnen")
  public void openPassword() {
    new AccountPage(browserPage).section("account-tab-security");
    host("account-password-edit-security").click();
    assertThat(input("account-password-new")).isVisible();
  }

  @Step("Neues Passwort, Bestätigung und aktuelles Passwort eingeben")
  public void password(String value, String confirmation, String current) {
    input("account-password-new").fill(value);
    input("account-password-confirm").fill(confirmation);
    input("account-password-current").fill(current);
  }

  @Step("Passwortänderung bewusst absenden")
  public void savePassword() {
    UiActions.complete(browserPage, () -> host("account-password-save").click());
  }

  @Step("E-Mail-Dialog unter Sicherheit öffnen")
  public void openEmail() {
    new AccountPage(browserPage).openEmail();
  }

  @Step("Neue Adresse, Bestätigung und aktuelles Passwort eingeben")
  public void email(String value, String confirmation, String current) {
    input("account-email-new").fill(value);
    input("account-email-confirm").fill(confirmation);
    input("account-email-password").fill(current);
  }

  @Step("Verifizierungsanfrage über den Dialog absenden")
  public void requestEmail() {
    UiActions.complete(browserPage, () -> host("account-email-request").click());
  }
}
