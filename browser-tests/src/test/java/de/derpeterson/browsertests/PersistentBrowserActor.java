package de.derpeterson.browsertests;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.ServiceWorkerPolicy;
import de.derpeterson.browsertests.i18n.UiTexts;
import de.derpeterson.browsertests.pages.UserMenu;
import io.qameta.allure.playwright.*;
import java.nio.file.Path;
import static de.derpeterson.browsertests.BrowserRoutes.*;
import static de.derpeterson.app.config.AppRouteConstants.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

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
        context = playwright.chromium().launchPersistentContext(profile, new BrowserType.LaunchPersistentContextOptions()
                .setHeadless(!Boolean.getBoolean("browser.tests.headed"))
                .setBaseURL(System.getProperty("browser.tests.baseUrl"))
                .setViewportSize(1280, 900).setLocale(texts.browserLocale())
                .setTimezoneId("Europe/Berlin").setServiceWorkers(ServiceWorkerPolicy.BLOCK));
        context.setDefaultTimeout(15_000); AllurePlaywright.register(context); trace = AllurePlaywright.startTracing(context);
        page = context.newPage(); page.navigate(path(HOME_ROUTE));
        assertThat(page).hasURL(urlPattern(expectedRoute));
        assertThat(page).hasTitle(expectedRoute.equals(LOGIN_ROUTE) ? LOGIN_PAGE_TITLE : HOME_PAGE_TITLE);
        if (expectedRoute.equals(HOME_ROUTE)) new UserMenu(page).selectTestLanguage();
    }
    @Override public void close() {
        try {AllurePlaywright.attachScreenshot("Separater Browser vor echtem Prozessende", page);}
        finally {try {trace.close();} finally {context.close();}}
    }
}
