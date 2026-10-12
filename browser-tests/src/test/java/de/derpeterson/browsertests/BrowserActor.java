package de.derpeterson.browsertests;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.ServiceWorkerPolicy;
import de.derpeterson.browsertests.i18n.UiTexts;
import de.derpeterson.browsertests.pages.*;
import io.qameta.allure.playwright.*;
import static de.derpeterson.browsertests.BrowserRoutes.*;
import static de.derpeterson.app.config.AppRouteConstants.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** A genuinely separate user's context; no cookies or application state are injected. */
public final class BrowserActor implements AutoCloseable {
    public final BrowserContext context;
    public final Page page;
    private final TraceSession trace;

    public BrowserActor(Browser browser) {
        UiTexts texts = UiTexts.selected();
        context = browser.newContext(new Browser.NewContextOptions()
                .setBaseURL(System.getProperty("browser.tests.baseUrl"))
                .setViewportSize(1280, 900).setLocale(texts.browserLocale())
                .setTimezoneId("Europe/Berlin").setServiceWorkers(ServiceWorkerPolicy.BLOCK));
        context.setDefaultTimeout(15_000);
        AllurePlaywright.register(context);
        trace = AllurePlaywright.startTracing(context);
        page = context.newPage();
        page.navigate(path(HOME_ROUTE));
        assertThat(page).hasTitle(HOME_PAGE_TITLE);
        new UserMenu(page).selectTestLanguage();
    }

    public void login(String email, String password) {login(email, password, false);}

    public void login(String email, String password, boolean remember) {
        LoginPage login = new LoginPage(page); login.open();
        page.getByRole(com.microsoft.playwright.options.AriaRole.CHECKBOX, new Page.GetByRoleOptions()
                .setName(UiTexts.selected().getLoginRememberMeCheckbox()).setExact(true)).setChecked(remember);
        login.signIn(email, password);
    }

    public AdminUsersPage loginAdmin(String email, String password) {return loginAdmin(email, password, false);}

    public AdminUsersPage loginAdmin(String email, String password, boolean remember) {
        login(email, password, remember);
        new UserMenu(page).selectTestLanguage();
        new LoginPage(page).assertAdminSignedIn(email);
        return new AdminUsersPage(page);
    }

    @Override public void close() {
        try { if (!page.isClosed()) AllurePlaywright.attachScreenshot("Zusätzlicher Benutzerkontext am Testende", page); }
        finally { try { trace.close(); } finally { context.close(); } }
    }
}
