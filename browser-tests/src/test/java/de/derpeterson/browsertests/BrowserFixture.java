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
  protected static Playwright playwright;
  protected static Browser browser;
  protected BrowserContext context;
  protected Page page;
  protected final UiTexts texts = UiTexts.selected();
  protected TraceSession trace;

  @BeforeAll
  protected static void launchBrowser() {
    if (!Boolean.getBoolean("browser.tests.managed")) {
      throw new IllegalStateException(
          "Use run.ps1: browser tests require a disposable Docker environment.");
    }
    playwright = Playwright.create();
    browser =
        playwright
            .chromium()
            .launch(
                new BrowserType.LaunchOptions()
                    // Native Scrollleisten müssen auch auf Headless-Screenshots sichtbar bleiben.
                    .setIgnoreDefaultArgs(List.of("--hide-scrollbars"))
                    .setHeadless(!Boolean.getBoolean("browser.tests.headed")));
    System.setProperty("browser.tests.browserVersion", browser.version());
  }

  @BeforeEach
  protected void openPage() {
    context =
        browser.newContext(
            new Browser.NewContextOptions()
                .setBaseURL(System.getProperty("browser.tests.baseUrl"))
                .setViewportSize(1280, 900)
                .setLocale(texts.browserLocale())
                .setTimezoneId("Europe/Berlin")
                .setServiceWorkers(com.microsoft.playwright.options.ServiceWorkerPolicy.BLOCK));
    context.setDefaultTimeout(15_000);
    AllurePlaywright.register(context);
    trace = AllurePlaywright.startTracing(context);
    page = context.newPage();
    page.navigate(path(HOME_ROUTE));
    com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat(page)
        .hasTitle(HOME_PAGE_TITLE);
    new UserMenu(page).selectTestLanguage();
  }

  @AfterEach
  protected void closePage() {
    try {
      if (page != null && !page.isClosed()) {
        AllurePlaywright.attachScreenshot("Browser am Testende", page);
        // A closed browser intentionally keeps its server slot until timeout. Explicitly
        // sign out the shared fixture account so independent cases do not consume its limit.
        page.navigate(path(HOME_ROUTE));
        page.waitForCondition(
            () ->
                page.locator(".public-header, vaadin-button.tracked-user-button")
                    .first()
                    .isVisible());
        if (java.net.URI.create(page.url()).getPath().equals("/two-factor")) {
          page.locator("#two-factor-cancel").click();
          page.waitForURL(url -> java.net.URI.create(url).getPath().equals("/login"));
        }
        UserMenu menu = new UserMenu(page);
        menu.selectTestLanguage();
        // Besucher haben nur die öffentliche Navigation und kein Avatar-Popup.
        if (!page.locator(".public-header").isVisible()) {
          menu.requestSignOut();
        }
      }
    } finally {
      try {
        if (trace != null) {
          trace.close();
        }
      } finally {
        if (context != null) {
          context.close();
        }
      }
    }
  }

  @AfterAll
  protected static void closeBrowser() {
    if (playwright != null) {
      playwright.close();
    }
  }
}
