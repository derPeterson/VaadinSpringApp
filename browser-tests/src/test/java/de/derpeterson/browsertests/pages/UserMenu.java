package de.derpeterson.browsertests.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.i18n.UiTexts;
import io.qameta.allure.Step;

/** The same avatar menu a person uses to select a language or sign out. */
public final class UserMenu {
  private final Page page;
  private final UiTexts texts = UiTexts.selected();

  public UserMenu(Page page) {
    this.page = page;
  }

  @Step("Benutzermenü über den Avatar öffnen")
  public void open() {
    // Während der Schließanimation sperrt das modale Overlay weiterhin den Hintergrund.
    assertThat(page.locator("vaadin-popover-overlay[closing]")).hasCount(0);
    if (!page.locator("vaadin-popover-overlay[opened]").isVisible()) {
      UiActions.complete(page, () -> avatar().click());
    }
    assertThat(page.locator("vaadin-popover-overlay[opened]")).isVisible();
  }

  @Step("Testsprache über den sichtbaren Sprachschalter auswählen")
  public void selectTestLanguage() {
    selectLanguage(texts.language());
  }

  @Step("Sprache ausdrücklich über den Sprachschalter wechseln")
  public void selectLanguage(String language) {
    // Nach einer Navigation erst die tatsächlich gelieferte Kopfzeile abwarten.
    assertThat(page.locator(".public-header, vaadin-button.tracked-user-button").first())
        .isVisible();
    Locator publicLanguage =
        page.locator(".public-languages vaadin-button[lang='" + language + "']");
    if (publicLanguage.isVisible()) {
      UiActions.complete(page, publicLanguage::click);
      assertThat(publicLanguage).hasAttribute("aria-pressed", "true");
      return;
    }
    UiTexts expected = new UiTexts(language);
    open();
    Locator languageButton = page.locator("#portal-language-" + language);
    assertThat(languageButton).isVisible();
    languageButton.click();
    assertThat(languageButton).hasAttribute("aria-pressed", "true");
    // The menu may be anonymous or authenticated; both expected labels use exactly this language.
    assertThat(
            page.getByRole(
                    AriaRole.BUTTON,
                    new Page.GetByRoleOptions()
                        .setName(expected.getBaseLoginButton())
                        .setExact(true))
                .or(
                    page.getByRole(
                        AriaRole.BUTTON,
                        new Page.GetByRoleOptions()
                            .setName(expected.getBaseLogoutButton())
                            .setExact(true))))
        .isVisible();
    close();
  }

  @Step("Benutzermenü schließen und die native Schließanimation abwarten")
  public void close() {
    UiActions.complete(page, () -> page.keyboard().press("Escape"));
    assertThat(page.locator("vaadin-popover-overlay[opened], vaadin-popover-overlay[closing]"))
        .hasCount(0);
    assertThat(avatar()).hasAttribute("aria-expanded", "false");
  }

  @Step("Über die Oberfläche abmelden")
  public void signOut() {
    requestSignOut();
    new LoginPage(page).assertFormVisible();
  }

  @Step("Abmelden anklicken und die echte Logout-Antwort abwarten")
  public void requestSignOut() {
    open();
    // Synchronize the actual browser RPC; no synthetic request or state mutation.
    page.waitForResponse(
            response ->
                response.request().method().equals("POST")
                    && response.url().startsWith(System.getProperty("browser.tests.baseUrl"))
                    && response.url().contains("v-r=uidl"),
            () ->
                page.getByRole(
                        AriaRole.BUTTON,
                        new Page.GetByRoleOptions()
                            .setName(texts.getBaseLogoutButton())
                            .setExact(true))
                    .click())
        .finished();
  }

  @Step("Normales Konto ist angemeldet und hat keine Adminaktion im Menü")
  public void assertUserSignedInWithoutAdminAction(String email) {
    open();
    assertThat(
            page.locator(".user-popover-body")
                .getByText(email, new Locator.GetByTextOptions().setExact(true)))
        .isVisible();
    assertThat(
            page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName(texts.getBaseLogoutButton()).setExact(true)))
        .isVisible();
    assertThat(
            page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                    .setName(texts.getBaseManageApplicationButton())
                    .setExact(true)))
        .hasCount(0);
  }

  private Locator avatar() {
    return page.locator("vaadin-button.tracked-user-button");
  }
}
