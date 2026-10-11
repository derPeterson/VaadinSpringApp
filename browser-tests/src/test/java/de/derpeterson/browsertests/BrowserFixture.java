package de.derpeterson.browsertests;

import com.microsoft.playwright.*;
import io.qameta.allure.playwright.AllurePlaywright;
import de.derpeterson.browsertests.i18n.UiTexts;
import de.derpeterson.browsertests.pages.UserMenu;
import io.qameta.allure.playwright.TraceSession;
import org.junit.jupiter.api.*;

public abstract class BrowserFixture {
    protected static Playwright playwright;
    protected static Browser browser;
    protected BrowserContext context;
    protected Page page;
    protected final UiTexts texts = UiTexts.selected();
    protected TraceSession trace;

    @BeforeAll
    protected static void launchBrowser() {
        if (!Boolean.getBoolean("browser.tests.managed"))
            throw new IllegalStateException("Use run.ps1: browser tests require a disposable Docker environment.");
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(!Boolean.getBoolean("browser.tests.headed")));
        System.setProperty("browser.tests.browserVersion", browser.version());
    }

    @BeforeEach
    protected void openPage() {
        context = browser.newContext(new Browser.NewContextOptions().setBaseURL(System.getProperty("browser.tests.baseUrl"))
                .setViewportSize(1280, 900).setLocale(texts.browserLocale()).setTimezoneId("Europe/Berlin")
                .setServiceWorkers(com.microsoft.playwright.options.ServiceWorkerPolicy.BLOCK));
        context.setDefaultTimeout(15_000);
        AllurePlaywright.register(context);
        trace = AllurePlaywright.startTracing(context);
        page = context.newPage();
        page.navigate("/home");
        com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat(page).hasTitle("Home");
        new UserMenu(page).selectTestLanguage();
    }

    @AfterEach
    protected void closePage() {
        try {
            if (page != null && !page.isClosed()) AllurePlaywright.attachScreenshot("Browser am Testende", page);
        } finally {
            try { if (trace != null) trace.close(); }
            finally { if (context != null) context.close(); }
        }
    }

    @AfterAll
    protected static void closeBrowser() {
        if (playwright != null) playwright.close();
    }
}
