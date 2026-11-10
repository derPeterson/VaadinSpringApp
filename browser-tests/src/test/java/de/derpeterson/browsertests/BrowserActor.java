package de.derpeterson.browsertests;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.HOME_PAGE_TITLE;
import static de.derpeterson.app.config.AppRouteConstants.HOME_ROUTE;
import static de.derpeterson.browsertests.BrowserRoutes.path;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.ServiceWorkerPolicy;
import de.derpeterson.browsertests.i18n.UiTexts;
import de.derpeterson.browsertests.pages.AdminUsersPage;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.UserMenu;
import io.qameta.allure.playwright.AllurePlaywright;
import io.qameta.allure.playwright.TraceSession;

/** A genuinely separate user's context; no cookies or application state are injected. */
public final class BrowserActor implements AutoCloseable {
  public final BrowserContext browserContext;
  public final Page browserPage;
  private final TraceSession traceSession;

  public BrowserActor(Browser testBrowser) {
    this(testBrowser, HOME_PAGE_TITLE);
  }

  public BrowserActor(Browser testBrowser, String expectedInitialTitle) {
    UiTexts uiTexts = UiTexts.selected();
    browserContext =
        testBrowser.newContext(
            new Browser.NewContextOptions()
                .setBaseURL(System.getProperty("browser.tests.baseUrl"))
                .setViewportSize(1280, 900)
                .setLocale(uiTexts.browserLocale())
                .setTimezoneId("Europe/Berlin")
                .setServiceWorkers(ServiceWorkerPolicy.BLOCK));
    browserContext.setDefaultTimeout(15_000);
    AllurePlaywright.register(browserContext);
    traceSession = AllurePlaywright.startTracing(browserContext);
    browserPage = browserContext.newPage();
    browserPage.navigate(path(HOME_ROUTE));
    assertThat(browserPage).hasTitle(expectedInitialTitle);
    if (HOME_PAGE_TITLE.equals(expectedInitialTitle)) {
      new UserMenu(browserPage).selectTestLanguage();
    }
  }

  public void login(String email, String password) {
    login(email, password, false);
  }

  public void login(String email, String password, boolean remember) {
    LoginPage login = new LoginPage(browserPage);
    login.open();
    browserPage
        .getByRole(
            AriaRole.SWITCH,
            new Page.GetByRoleOptions()
                .setName(UiTexts.selected().getLoginRememberMeCheckbox())
                .setExact(true))
        .setChecked(remember);
    login.signIn(email, password);
  }

  public AdminUsersPage loginAdmin(String email, String password) {
    return loginAdmin(email, password, false);
  }

  public AdminUsersPage loginAdmin(String email, String password, boolean remember) {
    login(email, password, remember);
    new UserMenu(browserPage).selectTestLanguage();
    new LoginPage(browserPage).assertAdminSignedIn(email);
    return new AdminUsersPage(browserPage);
  }

  @Override
  public void close() {
    try {
      if (!browserPage.isClosed()) {
        AllurePlaywright.attachScreenshot("Zusätzlicher Benutzerkontext am Testende", browserPage);
      }
    } finally {
      try {
        traceSession.close();
      } finally {
        browserContext.close();
      }
    }
  }
}
