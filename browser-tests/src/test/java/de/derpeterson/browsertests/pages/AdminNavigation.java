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
    Locator drawerToggleLocator = browserPage.locator("#account-navigation-toggle");
    // Nach direktem Seitenaufruf erst den Vaadin-Aufbau abwarten. Ein noch nicht
    // vorhandener Schalter ist nicht gleichbedeutend mit Desktopnavigation.
    assertThat(drawerToggleLocator).hasCount(1);
    if (drawerToggleLocator.isVisible()
        && !"true".equals(drawerToggleLocator.getAttribute("aria-expanded"))) {
      UiActions.complete(browserPage, drawerToggleLocator::click);

      assertThat(drawerToggleLocator).hasAttribute("aria-expanded", "true");
    }
    assertThat(browserPage.locator("#admin-sidebar")).isVisible();
  }

  @Step("Konfigurationsgruppe über das native Untermenü auswählen")
  public void openGroup(AdminConfigGroup configGroup) {
    openDrawer();
    if (!Boolean.TRUE.equals(configuration().evaluate("el => el.expanded"))) {
      configuration().getByRole(AriaRole.BUTTON).click();
    }
    assertThat(configuration()).hasJSProperty("expanded", true);
    Locator navigationLinkLocator = groupLink(configGroup);
    UiActions.complete(browserPage, navigationLinkLocator::click);
    closeOverlayDrawer();

    assertThat(
            browserPage.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions()
                    .setName(uiTexts.getAdminConfigGroupLabel(configGroup))
                    .setExact(true)
                    .setLevel(2)))
        .isVisible();
  }

  public Locator groupLink(AdminConfigGroup configGroup) {
    String navigationLabelText =
        configGroup == AdminConfigGroup.TWO_FACTOR
            ? uiTexts.getTwoFactorNavigationLabel()
            : uiTexts.getAdminConfigGroupLabel(configGroup);
    return configuration()
        .getByRole(
            AriaRole.LINK,
            new Locator.GetByRoleOptions().setName(navigationLabelText).setExact(true));
  }

  @Step("Adminbereich über seinen SideNav-Link auswählen")
  public void openSection(String navigationLabelText) {
    openDrawer();
    Locator navigationLinkLocator =
        browserPage
            .locator("#admin-sidebar .cosmos-side-navigation")
            .first()
            .getByRole(
                AriaRole.LINK,
                new Locator.GetByRoleOptions().setName(navigationLabelText).setExact(true));
    UiActions.complete(browserPage, navigationLinkLocator::click);
    closeOverlayDrawer();
  }

  public void closeOverlayDrawer() {
    Locator drawerToggleLocator = browserPage.locator("#account-navigation-toggle");
    if (drawerToggleLocator.isVisible()
        && "true".equals(drawerToggleLocator.getAttribute("aria-expanded"))) {
      browserPage.keyboard().press("Escape");

      assertThat(drawerToggleLocator).hasAttribute("aria-expanded", "false");
    }
  }
}
