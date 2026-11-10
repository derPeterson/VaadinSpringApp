package de.derpeterson.browsertests.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.app.config.AdminConfigGroup;
import de.derpeterson.browsertests.i18n.UiTexts;
import io.qameta.allure.Step;

/** Gemeinsame Bedienung der nativen SideNav, einschließlich des mobilen Drawers. */
public final class AdminNavigation {
  private final Page browserPage;
  private final UiTexts uiTexts;

  public AdminNavigation(Page browserPage) {
    this(browserPage, UiTexts.selected());
  }

  public AdminNavigation(Page browserPage, UiTexts uiTexts) {
    this.browserPage = browserPage;
    this.uiTexts = uiTexts;
  }

  public Locator configuration() {
    return browserPage.locator(
        "#admin-sidebar .cosmos-side-navigation > vaadin-side-nav-item[has-children]");
  }

  @Step("Adminnavigation bei Bedarf öffnen")
  public void openDrawer() {
    Locator toggle = browserPage.locator("#account-navigation-toggle");
    // Nach direktem Seitenaufruf erst den Vaadin-Aufbau abwarten. Ein noch nicht
    // vorhandener Schalter ist nicht gleichbedeutend mit Desktopnavigation.
    assertThat(toggle).hasCount(1);
    if (toggle.isVisible() && !"true".equals(toggle.getAttribute("aria-expanded"))) {
      UiActions.complete(browserPage, toggle::click);
      assertThat(toggle).hasAttribute("aria-expanded", "true");
    }
    assertThat(browserPage.locator("#admin-sidebar")).isVisible();
  }

  @Step("Konfigurationsgruppe über das native Untermenü auswählen")
  public void openGroup(AdminConfigGroup group) {
    openDrawer();
    if (!Boolean.TRUE.equals(configuration().evaluate("el => el.expanded"))) {
      configuration().getByRole(AriaRole.BUTTON).click();
    }
    assertThat(configuration()).hasJSProperty("expanded", true);
    Locator link = groupLink(group);
    UiActions.complete(browserPage, link::click);
    closeOverlayDrawer();
    assertThat(
            browserPage.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions()
                    .setName(uiTexts.getAdminConfigGroupLabel(group))
                    .setExact(true)
                    .setLevel(2)))
        .isVisible();
  }

  public Locator groupLink(AdminConfigGroup group) {
    String label =
        group == AdminConfigGroup.TWO_FACTOR
            ? uiTexts.getTwoFactorNavigationLabel()
            : uiTexts.getAdminConfigGroupLabel(group);
    return configuration()
        .getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName(label).setExact(true));
  }

  @Step("Adminbereich über seinen SideNav-Link auswählen")
  public void openSection(String label) {
    openDrawer();
    Locator link =
        browserPage
            .locator("#admin-sidebar .cosmos-side-navigation")
            .first()
            .getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName(label).setExact(true));
    UiActions.complete(browserPage, link::click);
    closeOverlayDrawer();
  }

  public void closeOverlayDrawer() {
    Locator toggle = browserPage.locator("#account-navigation-toggle");
    if (toggle.isVisible() && "true".equals(toggle.getAttribute("aria-expanded"))) {
      browserPage.keyboard().press("Escape");
      assertThat(toggle).hasAttribute("aria-expanded", "false");
    }
  }
}
