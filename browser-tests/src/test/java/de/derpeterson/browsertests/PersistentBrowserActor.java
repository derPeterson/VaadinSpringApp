package de.derpeterson.browsertests;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.HOME_PAGE_TITLE;
import static de.derpeterson.app.config.AppRouteConstants.HOME_ROUTE;
import static de.derpeterson.app.config.AppRouteConstants.LOGIN_PAGE_TITLE;
import static de.derpeterson.app.config.AppRouteConstants.LOGIN_ROUTE;
import static de.derpeterson.browsertests.BrowserRoutes.path;
import static de.derpeterson.browsertests.BrowserRoutes.urlPattern;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.ServiceWorkerPolicy;
import de.derpeterson.browsertests.i18n.UiTexts;
import de.derpeterson.browsertests.pages.UserMenu;
import io.qameta.allure.playwright.AllurePlaywright;
import io.qameta.allure.playwright.TraceSession;
import java.nio.file.Path;

/** Real separate Chromium process using only its own temporary on-disk profile. */
public final class PersistentBrowserActor implements AutoCloseable {
  public final BrowserContext context;
  public final Page page;
  private final TraceSession trace;

  public PersistentBrowserActor(Playwright playwright, Path profile) {
    this(playwright, profile, HOME_ROUTE);
  }

  public PersistentBrowserActor(Playwright playwright, Path profile, String expectedRoute) {
    UiTexts texts = UiTexts.selected();
    context =
        playwright
            .chromium()
            .launchPersistentContext(
                profile,
                new BrowserType.LaunchPersistentContextOptions()
                    .setHeadless(!Boolean.getBoolean("browser.tests.headed"))
                    .setBaseURL(System.getProperty("browser.tests.baseUrl"))
                    .setViewportSize(1280, 900)
                    .setLocale(texts.browserLocale())
                    .setTimezoneId("Europe/Berlin")
                    .setServiceWorkers(ServiceWorkerPolicy.BLOCK));
    context.setDefaultTimeout(15_000);
    AllurePlaywright.register(context);
    trace = AllurePlaywright.startTracing(context);
    page = context.newPage();
    page.navigate(path(HOME_ROUTE));
    assertThat(page).hasURL(urlPattern(expectedRoute));
    assertThat(page)
        .hasTitle(expectedRoute.equals(LOGIN_ROUTE) ? LOGIN_PAGE_TITLE : HOME_PAGE_TITLE);
    if (expectedRoute.equals(HOME_ROUTE)) {
      new UserMenu(page).selectTestLanguage();
    }
  }

  @Override
  public void close() {
    try {
      AllurePlaywright.attachScreenshot("Separater Browser vor echtem Prozessende", page);
    } finally {
      try {
        trace.close();
      } finally {
        context.close();
      }
    }
  }
}
