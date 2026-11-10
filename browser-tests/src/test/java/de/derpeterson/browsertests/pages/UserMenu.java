package de.derpeterson.browsertests.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.i18n.UiTexts;
import io.qameta.allure.Step;

/** The same avatar menu a person uses to select a language or sign out. */
public final class UserMenu {
  private final Page browserPage;
  private final UiTexts uiTexts = UiTexts.selected();

  public UserMenu(Page browserPage) {
    this.browserPage = browserPage;
  }

  @Step("Benutzermenü über den Avatar öffnen")
  public void open() {
    // Während der Schließanimation sperrt das modale Overlay weiterhin den Hintergrund.
    assertThat(browserPage.locator("vaadin-popover-overlay[closing]")).hasCount(0);
    if (!browserPage.locator("vaadin-popover-overlay[opened]").isVisible()) {
      UiActions.complete(browserPage, () -> avatar().click());
    }
    assertThat(browserPage.locator("vaadin-popover-overlay[opened]")).isVisible();
  }

  @Step("Testsprache über den sichtbaren Sprachschalter auswählen")
  public void selectTestLanguage() {
    selectLanguage(uiTexts.language());
  }

  @Step("Sprache ausdrücklich über den Sprachschalter wechseln")
  public void selectLanguage(String language) {
    // Nach einer Navigation erst die tatsächlich gelieferte Kopfzeile abwarten.
    assertThat(browserPage.locator(".public-header, vaadin-button.tracked-user-button").first())
        .isVisible();
    Locator publicLanguage =
        browserPage.locator(".public-languages vaadin-button[lang='" + language + "']");
    if (publicLanguage.isVisible()) {
      UiActions.complete(browserPage, publicLanguage::click);
      assertThat(publicLanguage).hasAttribute("aria-pressed", "true");
      return;
    }
    UiTexts expected = new UiTexts(language);
    open();
    Locator languageButton = browserPage.locator("#portal-language-" + language);
    assertThat(languageButton).isVisible();
    languageButton.click();
    assertThat(languageButton).hasAttribute("aria-pressed", "true");
    // The menu may be anonymous or authenticated; both expected labels use exactly this language.
    assertThat(
            browserPage
                .getByRole(
                    AriaRole.BUTTON,
                    new Page.GetByRoleOptions()
                        .setName(expected.getBaseLoginButton())
                        .setExact(true))
                .or(
                    browserPage.getByRole(
                        AriaRole.BUTTON,
                        new Page.GetByRoleOptions()
                            .setName(expected.getBaseLogoutButton())
                            .setExact(true))))
        .isVisible();
    close();
  }

  @Step("Benutzermenü schließen und die native Schließanimation abwarten")
  public void close() {
    UiActions.complete(browserPage, () -> browserPage.keyboard().press("Escape"));
    assertThat(
            browserPage.locator("vaadin-popover-overlay[opened], vaadin-popover-overlay[closing]"))
        .hasCount(0);
    assertThat(avatar()).hasAttribute("aria-expanded", "false");
  }

  @Step("Über die Oberfläche abmelden")
  public void signOut() {
    requestSignOut();
    new LoginPage(browserPage).assertFormVisible();
  }

  @Step("Abmelden anklicken und die echte Logout-Antwort abwarten")
  public void requestSignOut() {
    open();
    // Synchronize the actual browser RPC; no synthetic request or state mutation.
    browserPage
        .waitForResponse(
            response ->
                response.request().method().equals("POST")
                    && response.url().startsWith(System.getProperty("browser.tests.baseUrl"))
                    && response.url().contains("v-r=uidl"),
            () ->
                browserPage
                    .getByRole(
                        AriaRole.BUTTON,
                        new Page.GetByRoleOptions()
                            .setName(uiTexts.getBaseLogoutButton())
                            .setExact(true))
                    .click())
        .finished();
  }

  @Step("Normales Konto ist angemeldet und hat keine Adminaktion im Menü")
  public void assertUserSignedInWithoutAdminAction(String email) {
    open();
    assertThat(
            browserPage
                .locator(".user-popover-body")
                .getByText(email, new Locator.GetByTextOptions().setExact(true)))
        .isVisible();
    assertThat(
            browserPage.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName(uiTexts.getBaseLogoutButton()).setExact(true)))
        .isVisible();
    assertThat(
            browserPage.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                    .setName(uiTexts.getBaseManageApplicationButton())
                    .setExact(true)))
        .hasCount(0);
  }

  private Locator avatar() {
    return browserPage.locator("vaadin-button.tracked-user-button");
  }
}
