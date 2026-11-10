package de.derpeterson.browsertests;

import static de.derpeterson.app.config.AppRouteConstants.HOME_PAGE_TITLE;
import static de.derpeterson.app.config.AppRouteConstants.HOME_ROUTE;
import static de.derpeterson.browsertests.BrowserRoutes.path;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import de.derpeterson.browsertests.i18n.UiTexts;
import de.derpeterson.browsertests.pages.UserMenu;
import io.qameta.allure.playwright.AllurePlaywright;
import io.qameta.allure.playwright.TraceSession;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

public abstract class BrowserFixture {
  protected static Playwright playwrightRuntime;
  protected static Browser testBrowser;
  protected BrowserContext browserContext;
  protected Page browserPage;
  protected final UiTexts uiTexts = UiTexts.selected();
  protected TraceSession traceSession;

  @BeforeAll
  protected static void launchTestBrowser() {
    if (!Boolean.getBoolean("browser.tests.managed")) {
      throw new IllegalStateException(
          "Use run.ps1: browser tests require a disposable Docker environment.");
    }
    playwrightRuntime = Playwright.create();
    testBrowser =
        playwrightRuntime
            .chromium()
            .launch(
                new BrowserType.LaunchOptions()
                    // Native Scrollleisten müssen auch auf Headless-Screenshots sichtbar bleiben.
                    .setIgnoreDefaultArgs(List.of("--hide-scrollbars"))
                    .setHeadless(!Boolean.getBoolean("browser.tests.headed")));
    System.setProperty("browser.tests.browserVersion", testBrowser.version());
  }

  @BeforeEach
  protected void openIsolatedBrowserPage() {
    browserContext =
        testBrowser.newContext(
            new Browser.NewContextOptions()
                .setBaseURL(System.getProperty("browser.tests.baseUrl"))
                .setViewportSize(1280, 900)
                .setLocale(uiTexts.browserLocale())
                .setTimezoneId("Europe/Berlin")
                .setServiceWorkers(com.microsoft.playwright.options.ServiceWorkerPolicy.BLOCK));
    browserContext.setDefaultTimeout(15_000);
    AllurePlaywright.register(browserContext);
    traceSession = AllurePlaywright.startTracing(browserContext);
    browserPage = browserContext.newPage();
    browserPage.navigate(path(HOME_ROUTE));
    com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat(browserPage)
        .hasTitle(HOME_PAGE_TITLE);
    new UserMenu(browserPage).selectTestLanguage();
  }

  @AfterEach
  protected void closeIsolatedBrowserPage() {
    try {
      if (browserPage != null && !browserPage.isClosed()) {
        AllurePlaywright.attachScreenshot("Browser am Testende", browserPage);
        // A closed browser intentionally keeps its server slot until timeout. Explicitly
        // sign out the shared fixture account so independent cases do not consume its limit.
        browserPage.navigate(path(HOME_ROUTE));
        browserPage.waitForCondition(() ->
            browserPage
                .locator(".public-header, vaadin-button.tracked-user-button")
                .first()
                .isVisible());
        if (java.net.URI.create(browserPage.url()).getPath().equals("/two-factor")) {
          browserPage.locator("#two-factor-cancel").click();
          browserPage.waitForURL(url -> java.net.URI.create(url).getPath().equals("/login"));
        }
        UserMenu menu = new UserMenu(browserPage);
        menu.selectTestLanguage();
        // Besucher haben nur die öffentliche Navigation und kein Avatar-Popup.
        if (!browserPage.locator(".public-header").isVisible()) {
          menu.requestSignOut();
        }
      }
    } finally {
      try {
        if (traceSession != null) {
          traceSession.close();
        }
      } finally {
        if (browserContext != null) {
          browserContext.close();
        }
      }
    }
  }

  @AfterAll
  protected static void closePlaywrightRuntime() {
    if (playwrightRuntime != null) {
      playwrightRuntime.close();
    }
  }
}
