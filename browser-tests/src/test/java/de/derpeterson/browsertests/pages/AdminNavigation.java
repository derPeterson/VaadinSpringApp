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
  private final Page page;
  private final UiTexts texts;

  public AdminNavigation(Page page) {
    this(page, UiTexts.selected());
  }

  public AdminNavigation(Page page, UiTexts texts) {
    this.page = page;
    this.texts = texts;
  }

  public Locator configuration() {
    return page.locator(
        "#admin-sidebar .cosmos-side-navigation > vaadin-side-nav-item[has-children]");
  }

  @Step("Adminnavigation bei Bedarf öffnen")
  public void openDrawer() {
    Locator toggle = page.locator("#account-navigation-toggle");
    if (toggle.isVisible() && !"true".equals(toggle.getAttribute("aria-expanded"))) {
      UiActions.complete(page, toggle::click);
    }
    assertThat(page.locator("#admin-sidebar")).isVisible();
  }

  @Step("Konfigurationsgruppe über das native Untermenü auswählen")
  public void openGroup(AdminConfigGroup group) {
    openDrawer();
    if (!Boolean.TRUE.equals(configuration().evaluate("el => el.expanded"))) {
      configuration().getByRole(AriaRole.BUTTON).click();
    }
    assertThat(configuration()).hasJSProperty("expanded", true);
    Locator link =
        configuration()
            .getByRole(
                AriaRole.LINK,
                new Locator.GetByRoleOptions()
                    .setName(texts.getAdminConfigGroupLabel(group))
                    .setExact(true));
    UiActions.complete(page, link::click);
    closeOverlayDrawer();
    assertThat(
            page.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions()
                    .setName(texts.getAdminConfigGroupLabel(group))
                    .setExact(true)
                    .setLevel(2)))
        .isVisible();
  }

  @Step("Adminbereich über seinen SideNav-Link auswählen")
  public void openSection(String label) {
    openDrawer();
    Locator link =
        page.locator("#admin-sidebar .cosmos-side-navigation")
            .first()
            .getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName(label).setExact(true));
    UiActions.complete(page, link::click);
    closeOverlayDrawer();
  }

  public void closeOverlayDrawer() {
    Locator toggle = page.locator("#account-navigation-toggle");
    if (toggle.isVisible() && "true".equals(toggle.getAttribute("aria-expanded"))) {
      page.keyboard().press("Escape");
      assertThat(toggle).hasAttribute("aria-expanded", "false");
    }
  }
}
