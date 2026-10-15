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
    return page.locator(".cosmos-nav > vaadin-side-nav-item[has-children]");
  }

  @Step("Adminnavigation bei Bedarf öffnen")
  public void openDrawer() {
    Locator app = page.locator("vaadin-app-layout");
    if (!Boolean.TRUE.equals(app.evaluate("el => el.hasAttribute('drawer-opened')")))
      UiActions.complete(page, () -> page.locator(".cosmos-drawer-toggle").click());
    assertThat(app).hasAttribute("drawer-opened", "");
    assertThat(page.locator("vaadin-app-layout [part='drawer']"))
        .hasCSS("transform", "matrix(1, 0, 0, 1, 0, 0)");
  }

  @Step("Konfigurationsgruppe über das native Untermenü auswählen")
  public void openGroup(AdminConfigGroup group) {
    openDrawer();
    if (!Boolean.TRUE.equals(configuration().evaluate("el => el.expanded")))
      configuration().getByRole(AriaRole.BUTTON).click();
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
        page.locator(".cosmos-nav")
            .getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName(label).setExact(true));
    UiActions.complete(page, link::click);
    closeOverlayDrawer();
  }

  public void closeOverlayDrawer() {
    Locator app = page.locator("vaadin-app-layout");
    if (Boolean.TRUE.equals(
        app.evaluate("el => el.hasAttribute('overlay') && el.hasAttribute('drawer-opened')"))) {
      page.keyboard().press("Escape");
      assertThat(app).not().hasAttribute("drawer-opened", "");
    }
  }
}
