package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Route;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.BoundingBox;
import com.microsoft.playwright.options.ForcedColors;
import de.derpeterson.browsertests.BrowserActor;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.BrowserTestRunner;
import de.derpeterson.browsertests.PersistentBrowserActor;
import de.derpeterson.browsertests.i18n.UiTexts;
import de.derpeterson.browsertests.mail.MailpitInbox;
import de.derpeterson.browsertests.pages.AdminUserDialog;
import de.derpeterson.browsertests.pages.AdminUsersPage;
import de.derpeterson.browsertests.pages.ConfigurationPage;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.NotificationInboxPage;
import de.derpeterson.browsertests.pages.TwoFactorPage;
import de.derpeterson.browsertests.pages.UiActions;
import de.derpeterson.browsertests.pages.UserMenu;
import io.qameta.allure.Description;
import io.qameta.allure.playwright.AllurePlaywright;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@Tag("browser")
public class TwoFactorBrowserTest extends BrowserFixture {
  @Test
  @Tag("ticket-filters")
  @Tag("follow-up-regression")
  @Tag("ticket-browser-detail")
  @DisplayName("MFA-23: Vorgangsseiten erhalten Details und Entwurf bei 10, 20 und 50 Einträgen")
  @Description(
      "Auf Desktop und Smartphone bleiben Details und Entwurf beim Blättern und beim Wechsel der Seitengröße erhalten. Live-Übernahmen leeren die letzte gefilterte Seite und beide Ansichten wechseln auf die vorige Seite.")
  void ticketPagesPreserveDetailsAndDrafts() {
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    AdminUsersPage users = new AdminUsersPage(browserPage);
    users.open();
    List<String> numbers = new ArrayList<>();
    for (int index = 0; index < 12; index++) {
      String email = "ticket-pages-" + UUID.randomUUID() + "@example.test";
      AdminUserDialog requesterAccount = users.openCreate();
      requesterAccount.fillNewUser(email, BrowserTestAccounts.USER_PASSWORD, true, false);
      requesterAccount.save();
      try (BrowserActor requester = new BrowserActor(testBrowser)) {
        requester.login(email, BrowserTestAccounts.USER_PASSWORD);
        requester.browserPage.navigate("/recovery-help");
        requester
            .browserPage
            .locator("#support-reply textarea")
            .fill("Synthetic request for pagination " + index);
        UiActions.complete(
            requester.browserPage, () -> requester.browserPage.locator("#support-create").click());
        numbers.add(
            requester
                .browserPage
                .locator("#support-ticket-grid")
                .getByText(Pattern.compile("^#[0-9]+$"))
                .first()
                .innerText()
                .substring(1));
      }
    }
    for (int width : new int[] {1280, 390}) {
      browserPage.setViewportSize(width, 1000);
      browserPage.navigate("/admin?section=support&ticket=" + numbers.getFirst());

      assertThat(browserPage.locator("#support-ticket-panel")).isVisible();
      if (browserPage.locator("#support-claim").isVisible()) {
        UiActions.complete(browserPage, () -> browserPage.locator("#support-claim").click());
        jumpToTicket(numbers.getFirst());
      }
      Locator paging = browserPage.locator("#support-pagination");
      Locator next = paging.locator(".cosmos-pagination-next");
      Locator previous = paging.locator(".cosmos-pagination-previous");
      Locator rows = browserPage.locator("#support-ticket-grid .support-admin-ticket-card:visible");
      Locator input = browserPage.locator("#support-reply textarea");

      assertThat(rows).hasCount(10);
      Locator scroll = browserPage.locator(".support-admin-ticket-scroll");

      assertTrue((Boolean) scroll.evaluate("area => area.scrollHeight > area.clientHeight"));
      rows.last().scrollIntoViewIfNeeded();

      assertTrue((Boolean) scroll.evaluate("area => area.scrollTop > 0"));
      assertThat(previous).isDisabled();
      assertThat(next).isEnabled();
      assertThat(paging.locator(".admin-user-pagination-arrow:visible")).hasCount(2);
      input.fill("This unsent reply survives changing ticket pages.");
      UiActions.complete(browserPage, next::click);

      assertThat(previous).isEnabled();
      assertThat(browserPage.locator("#support-ticket-panel .support-ticket-number"))
          .hasText(uiTexts.getSupportTicketNumber("#" + numbers.getFirst()));
      assertThat(input).hasValue("This unsent reply survives changing ticket pages.");
      UiActions.complete(browserPage, previous::click);

      assertThat(previous).isDisabled();
      for (int size : new int[] {20, 50, 10}) {
        if (width == 390) {
          browserPage.emulateMedia(
              new Page.EmulateMediaOptions().setForcedColors(ForcedColors.ACTIVE));
        }
        paging.locator("vaadin-combo-box").click();
        UiActions.completeValueChange(
            browserPage,
            () ->
                browserPage
                    .getByRole(
                        AriaRole.OPTION,
                        new Page.GetByRoleOptions().setName(Integer.toString(size)).setExact(true))
                    .click());
        if (width == 390) {
          browserPage.emulateMedia(
              new Page.EmulateMediaOptions().setForcedColors(ForcedColors.NONE));
        }
        assertThat(paging.locator("vaadin-combo-box input")).hasValue(Integer.toString(size));
        assertThat(previous).isDisabled();
        String range = paging.locator(".admin-user-pagination-total").innerText();
        int total = Integer.parseInt(range.substring(range.lastIndexOf(' ') + 1));

        assertThat(rows).hasCount(Math.min(size, total));
        assertThat(input).hasValue("This unsent reply survives changing ticket pages.");
      }
      AllurePlaywright.attachScreenshot("Vorgänge mit gekürzter Pagination " + width, browserPage);
      input.clear();
    }
    verifyLivePaginationAndFilteredDraft(numbers.getFirst());
  }

  private void verifyLivePaginationAndFilteredDraft(String assigned) {
    String route = "/admin?section=support";
    String rowSelector = "#support-ticket-grid .support-admin-ticket-card:visible";
    try (Page mobile = browserContext.newPage();
        Page editor = browserContext.newPage()) {
      browserPage.setViewportSize(1280, 1000);
      mobile.setViewportSize(390, 900);
      for (Page view : List.of(browserPage, mobile)) {
        view.navigate(route + "&ticket=" + assigned);

        assertThat(view.locator("#support-ticket-panel")).isVisible();
        view.locator("#support-reply textarea").fill("Draft retained outside the filtered list.");
        view.locator("#support-filter").click();
        Locator filter = view.locator("#support-filter-panel");
        filter
            .locator("#support-filter-status")
            .getByText(uiTexts.getSupportOpen(), new Locator.GetByTextOptions().setExact(true))
            .click();
        UiActions.complete(view, () -> filter.locator("#support-filter-apply").click());

        assertThat(
                view.locator(rowSelector)
                    .filter(new Locator.FilterOptions().setHasText("#" + assigned)))
            .hasCount(0);
        assertThat(view.locator("#support-reply textarea"))
            .hasValue("Draft retained outside the filtered list.");
        Locator next = view.locator("#support-pagination .cosmos-pagination-next");
        Locator range = view.locator("#support-pagination .admin-user-pagination-total");
        String firstRange = range.innerText();
        int total = Integer.parseInt(firstRange.substring(firstRange.lastIndexOf(' ') + 1));
        int pages = (total + 9) / 10;

        assertTrue(pages > 1, "Für das Abklemmen muss es mehrere Seiten geben");
        for (int current = 1; current < pages; current++) {
          String before = range.innerText();
          UiActions.complete(view, next::click);

          assertThat(range).not().hasText(before);
        }
        assertThat(next).isDisabled();
      }
      List<String> lastNumbers =
          browserPage
              .locator(rowSelector)
              .getByText(Pattern.compile("^#[0-9]+$"))
              .allTextContents();

      assertTrue(!lastNumbers.isEmpty());
      assertThat(mobile.locator(rowSelector)).hasCount(lastNumbers.size());
      for (int index = 0; index < lastNumbers.size(); index++) {
        if (index == lastNumbers.size() - 1) {
          assertSingleTicketListFits(browserPage);
          assertSingleTicketListFits(mobile);
        }
        String number = lastNumbers.get(index);
        editor.navigate(route + "&ticket=" + number.substring(1));
        UiActions.complete(editor, () -> editor.locator("#support-claim").click());
        int remaining = lastNumbers.size() - index - 1;
        for (Page view : List.of(browserPage, mobile)) {
          assertThat(
                  view.locator(rowSelector).filter(new Locator.FilterOptions().setHasText(number)))
              .hasCount(0);
          // Vor der letzten Übernahme bleibt die Seite erhalten, danach greift die vorige Seite.
          assertThat(view.locator(rowSelector)).hasCount(remaining == 0 ? 10 : remaining);
          assertThat(view.locator("#support-pagination .cosmos-pagination-next")).isDisabled();
          assertThat(view.locator("#support-active-filter-count")).hasText("1");
          assertThat(view.locator("#support-reply textarea"))
              .hasValue("Draft retained outside the filtered list.");
        }
      }
    }
  }

  private void assertSingleTicketListFits(Page view) {
    Locator scroll = view.locator(".support-admin-ticket-scroll");

    assertTrue(
        (Boolean)
            scroll.evaluate(
                """
                area => {
                  const grid = area.querySelector('#support-ticket-grid').getBoundingClientRect();
                  const footer = document.querySelector('#support-queue-footer').getBoundingClientRect();
                  return area.scrollHeight <= area.clientHeight + 1
                    && Math.abs(grid.bottom - footer.top) <= 1;
                }
                """));
  }

  private void jumpToTicket(String number) {
    browserPage.locator("#support-filter").click();
    Locator filter = browserPage.locator("#support-filter-panel");
    filter.locator("#support-jump-number input").fill(number);
    UiActions.complete(browserPage, () -> filter.locator("#support-jump").click());

    assertThat(filter).not().hasAttribute("opened", "");
  }

  @Test
  @Tag("ticket-filters")
  @Tag("follow-up-regression")
  @DisplayName("MFA-22: Vorgangsfilter und Ticketsprung verwenden die gemeinsamen Filterbausteine")
  void ticketFiltersAndDirectJumpUseNativeControls() {
    browserContext.grantPermissions(List.of("clipboard-read", "clipboard-write"));
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    String email = "ticket-filter-" + UUID.randomUUID() + "@example.test";
    new AdminUsersPage(browserPage).createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
    try (BrowserActor requester = new BrowserActor(testBrowser)) {
      requester.login(email, BrowserTestAccounts.USER_PASSWORD);
      requester.browserPage.navigate("/recovery-help");
      requester
          .browserPage
          .locator("#support-reply textarea")
          .fill("Synthetic request for ticket filters.");
      UiActions.complete(
          requester.browserPage, () -> requester.browserPage.locator("#support-create").click());
      String number =
          requester
              .browserPage
              .locator("#support-ticket-grid")
              .getByText(Pattern.compile("^#[0-9]+$"))
              .first()
              .innerText();
      for (int width : new int[] {1280, 390}) {
        browserPage.setViewportSize(width, 900);
        browserPage.navigate("/admin");
        requester
            .browserPage
            .locator("#support-reply textarea")
            .fill("Unread activity at width " + width);
        UiActions.complete(
            requester.browserPage,
            () -> requester.browserPage.locator("#support-add-reply").click());
        assertSupportNavigationActivity(width);
        browserPage.navigate("/admin?section=support");

        assertThat(browserPage.locator("#support-select-ticket")).isVisible();
        browserPage.locator("#support-filter").click();
        Locator panel = browserPage.locator("#support-filter-panel");

        assertThat(browserPage.locator("#support-filter-panel"))
            .hasClass(Pattern.compile(".*admin-user-filter-drawer.*"));
        panel
            .locator("#support-filter-status")
            .getByText(uiTexts.getSupportOpen(), new Locator.GetByTextOptions().setExact(true))
            .click();
        panel
            .locator("#support-filter-assignment")
            .getByText(
                uiTexts.getSupportFilterMine(), new Locator.GetByTextOptions().setExact(true))
            .click();
        panel
            .locator("#support-filter-reading")
            .getByText(
                uiTexts.getNotificationsUnread(), new Locator.GetByTextOptions().setExact(true))
            .click();

        assertThat(panel.locator("#support-filter-draft-count"))
            .hasText(uiTexts.getAdminUsersActiveFilterCount(3));
        assertThat(panel.locator("#support-jump-info"))
            .hasText(uiTexts.getSupportJumpTicketNumberHint());
        AllurePlaywright.attachScreenshot("Vorgangsfilter mit Testwerten " + width, browserPage);
        panel.locator("#support-filter-cancel").click();
        browserPage.locator("#support-filter").click();

        assertThat(panel.locator("#support-filter-status input:checked")).hasCount(0);
        Locator openStatus =
            panel
                .locator("#support-filter-status")
                .getByText(uiTexts.getSupportOpen(), new Locator.GetByTextOptions().setExact(true));
        openStatus.click();
        panel.locator("#support-filter-reset").click();

        assertThat(panel.locator("#support-filter-status input:checked")).hasCount(0);
        openStatus.click();
        browserPage.keyboard().press("Escape");

        assertThat(panel).not().hasAttribute("opened", "");
        browserPage.locator("#support-filter").click();

        assertThat(panel.locator("#support-filter-status input:checked")).hasCount(0);
        // Auf dem schmalen Display lässt nur die Queransicht eine Fläche neben dem Drawer frei.
        browserPage.setViewportSize(width, width == 390 ? 340 : 900);
        openStatus.click();
        Locator overlay = panel.locator("vaadin-dialog-overlay [part~=overlay]");

        assertThat(overlay).isVisible();
        BoundingBox overlayBounds = overlay.boundingBox();

        assertTrue(overlayBounds.x > 0, "Der Außenklick braucht eine sichtbare Fläche");
        if (width == 390) {
          Locator drawerBody = panel.locator("vaadin-dialog-overlay [part=content]");
          assertTouchScroll(browserPage, drawerBody);
        }
        browserPage.mouse().click(overlayBounds.x / 2, 150);

        assertThat(panel).not().hasAttribute("opened", "");
        browserPage.setViewportSize(width, 900);
        verifyUnreadFilterAndDirectJump(requester.browserPage, panel, number, width);
      }
    }
  }

  private void verifyUnreadFilterAndDirectJump(
      Page requester, Locator panel, String number, int width) {
    browserPage.locator("#support-filter").click();

    assertThat(panel.locator("#support-filter-status input:checked")).hasCount(0);
    panel
        .locator("#support-filter-reading")
        .getByText(uiTexts.getNotificationsUnread(), new Locator.GetByTextOptions().setExact(true))
        .click();
    panel.locator("#support-filter-apply").click();

    assertThat(browserPage.locator("#support-active-filter-count")).hasText("1");
    Locator unread = ticketCard(browserPage, number).locator(".support-ticket-unread");
    int previousUnread = Integer.parseInt(unread.innerText());
    requester.locator("#support-reply textarea").fill("Live change at width " + width);
    UiActions.complete(requester, () -> requester.locator("#support-add-reply").click());

    assertThat(unread).hasText(Integer.toString(previousUnread + 1));
    assertThat(browserPage.locator("#support-active-filter-count")).hasText("1");
    assertThat(ticketCard(browserPage, number)).isVisible();
    UiActions.complete(browserPage, () -> ticketCard(browserPage, number).click());

    assertThat(browserPage.locator("#support-ticket-panel")).isVisible();
    assertThat(ticketCard(browserPage, number)).not().isVisible();
    assertThat(browserPage.locator("#support-ticket-panel")).isVisible();
    browserPage.locator("#support-filter").click();
    Locator numberInput = panel.locator("#support-jump-number input");
    numberInput.fill("0");
    UiActions.complete(browserPage, () -> panel.locator("#support-jump").click());

    assertThat(panel).hasAttribute("opened", "");
    assertThat(browserPage.locator("#support-active-filter-count")).hasText("1");
    numberInput.clear();
    numberInput.pressSequentially(number.substring(1));
    UiActions.complete(browserPage, () -> panel.locator("#support-jump").click());

    assertThat(panel).not().hasAttribute("opened", "");
    assertThat(browserPage.locator("#support-ticket-panel")).isVisible();
    assertThat(browserPage)
        .hasURL(Pattern.compile(".*[?&]ticket=" + number.substring(1) + "(?:&.*)?$"));
    assertThat(browserPage.locator("#support-active-filter-count")).not().isVisible();
    browserPage.locator("#support-filter").click();
    numberInput.click();

    assertThat(numberInput).isFocused();
    browserPage.evaluate("value => navigator.clipboard.writeText(value)", number);
    numberInput.press("ControlOrMeta+V");

    assertThat(numberInput).hasValue(number);
    UiActions.complete(browserPage, () -> panel.locator("#support-jump").click());

    assertThat(panel).not().hasAttribute("opened", "");
    assertThat(browserPage.locator("#support-ticket-panel .support-ticket-number"))
        .hasText(uiTexts.getSupportTicketNumber(number));
  }

  private void assertSupportNavigationActivity(int width) {
    if (width == 390) {
      Locator toggle = browserPage.locator("#account-navigation-toggle");

      assertThat(toggle).isVisible();
      UiActions.complete(browserPage, toggle::click);

      assertThat(toggle).hasAttribute("aria-expanded", "true");
    }
    assertThat(browserPage.locator("#admin-nav-support-activity"))
        .hasAttribute("aria-label", uiTexts.getSupportNewActivity());
    Locator item = browserPage.locator("#admin-nav-support");
    item.hover();

    assertThat(browserPage.getByRole(AriaRole.TOOLTIP)).hasText(uiTexts.getSupportNewActivity());
  }

  private void assertTouchScroll(Page target, Locator scroller) {
    scroller.evaluate("element => element.scrollTop = 0");

    assertTrue(
        (Boolean) scroller.evaluate("element => element.scrollHeight > element.clientHeight"));
    UiActions.swipeUp(target, scroller);
    target.waitForCondition(() -> (Boolean) scroller.evaluate("element => element.scrollTop > 0"));
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @Tag("otp-behavior")
  @DisplayName("MFA-21: Alle sechsstelligen OTP-Ansichten fokussieren und bestätigen automatisch")
  void otpFocusAndAutomaticConfirmationCoverSetupLoginReplacementAndRecovery(int width) {
    LoginPage administrator = new LoginPage(browserPage);
    administrator.open();
    administrator.signIn(
        BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    String email = "otp-behavior-" + UUID.randomUUID() + "@example.test";
    String adminEmail = "otp-admin-" + UUID.randomUUID() + "@example.test";
    AdminUsersPage users = new AdminUsersPage(browserPage);
    users.createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
    users.createUser(adminEmail, BrowserTestAccounts.USER_PASSWORD, true, true);
    try (BrowserActor actor = new BrowserActor(testBrowser)) {
      actor.browserPage.setViewportSize(width, 900);
      actor.login(email, BrowserTestAccounts.USER_PASSWORD);
      new UserMenu(actor.browserPage).selectTestLanguage();
      actor.browserPage.navigate("/account?section=security&tab=portal");
      actor.browserPage.locator("#account-two-factor-setup").click();
      actor
          .browserPage
          .locator("#two-factor-password input")
          .fill(BrowserTestAccounts.USER_PASSWORD);
      actor.browserPage.locator("#two-factor-submit").click();
      checkEnrollmentOtp(actor.browserPage, email, false);

      actor.browserPage.locator("#account-two-factor-replace").click();
      TwoFactorPage.enterActionCode(
          actor.browserPage, TwoFactorPage.nextCode(actor.browserPage, email));
      actor.browserPage.locator("#two-factor-action-confirm").click();
      checkEnrollmentOtp(actor.browserPage, email, true);
      signOutToLogin(actor.browserPage);

      LoginPage login = new LoginPage(actor.browserPage);
      login.signInFirstFactor(email, BrowserTestAccounts.USER_PASSWORD);
      assertOtpFocus(actor.browserPage, "two-factor-code");
      TwoFactorPage.selectTestLanguageIfNeeded(actor.browserPage);
      actor.browserPage.locator("#two-factor-recovery-tab").click();
      actor.browserPage.locator("#two-factor-authenticator-tab").click();
      checkOtpInputAndConfirm(actor.browserPage, email, "two-factor-code", false);
      actor.browserPage.waitForURL(url -> !URI.create(url).getPath().equals("/two-factor"));
      signOutToLogin(actor.browserPage);

      login.signInFirstFactor(email, BrowserTestAccounts.USER_PASSWORD);
      TwoFactorPage.selectTestLanguageIfNeeded(actor.browserPage);
      TwoFactorPage.openLoginHelp(actor.browserPage);
      UiActions.complete(
          actor.browserPage, () -> actor.browserPage.locator("#two-factor-recover").click());
      Locator recoveryInput = actor.browserPage.locator("vaadin-text-field#two-factor-code input");

      assertThat(recoveryInput).isVisible();
      recoveryInput.fill(TwoFactorPage.nextCode(actor.browserPage, email));
      actor.browserPage.locator("#two-factor-submit").click();
      checkEnrollmentOtp(actor.browserPage, email, true);
      signOutToLogin(actor.browserPage);

      login.signInFirstFactor(adminEmail, BrowserTestAccounts.USER_PASSWORD);
      TwoFactorPage.selectTestLanguageIfNeeded(actor.browserPage);

      assertThat(actor.browserPage.locator("#two-factor-introduction-title")).isVisible();
      actor.browserPage.locator("#two-factor-submit").click();
      checkEnrollmentOtp(actor.browserPage, adminEmail, true);
      signOutToLogin(actor.browserPage);
      login.signInFirstFactor(adminEmail, BrowserTestAccounts.USER_PASSWORD);
      actor.browserPage.navigate("/two-factor?administration");
      assertOtpFocus(actor.browserPage, "two-factor-code");
      TwoFactorPage.selectTestLanguageIfNeeded(actor.browserPage);
      checkOtpInputAndConfirm(actor.browserPage, adminEmail, "two-factor-code", true);
      actor.browserPage.waitForURL(url -> !URI.create(url).getPath().equals("/two-factor"));
      signOutToLogin(actor.browserPage);
    }
  }

  private void signOutToLogin(Page actor) {
    new UserMenu(actor).requestSignOut();

    assertThat(actor.locator(".public-header")).isVisible();
    new LoginPage(actor).open();
  }

  private void checkEnrollmentOtp(Page actor, String email, boolean pasteValidCode) {
    assertThat(actor.locator("#two-factor-qr")).isVisible();
    TwoFactorPage.rememberEnrollment(actor, email);
    checkOtpInputAndConfirm(actor, email, "two-factor-enrollment-code", pasteValidCode);
    TwoFactorPage.captureCodes(actor, email);
    actor.locator("#two-factor-codes-saved input").setChecked(true);
    actor.locator("#two-factor-submit").click();
    actor.waitForURL(url -> !URI.create(url).getPath().equals("/two-factor"));
  }

  private void assertOtpFocus(Page actor, String id) {
    Locator otp = actor.locator("#" + id);

    assertThat(otp.locator("input")).isFocused();
    assertThat(otp.locator("[part=slot]").first()).hasAttribute("active", "");
    assertTrue((Boolean) otp.locator("input").evaluate("input => input.selectionStart === 0"));
  }

  private void checkOtpInputAndConfirm(
      Page actor, String email, String id, boolean pasteValidCode) {
    assertOtpFocus(actor, id);
    Locator otp = actor.locator("#" + id);
    Locator input = otp.locator("input");
    String invalid = TwoFactorPage.invalidCode(email);
    actor.keyboard().type(invalid.substring(0, 5));

    assertThat(input).hasValue(invalid.substring(0, 5));
    assertThat(otp.locator("[part=slot][filled]")).hasCount(5);
    actor.keyboard().type(invalid.substring(5));

    assertThat(otp).hasAttribute("invalid", "");
    assertThat(input).hasValue("");
    assertThat(input).isFocused();

    actor.context().grantPermissions(List.of("clipboard-read", "clipboard-write"));
    pasteOtp(actor, invalid.substring(0, 5));

    assertThat(input).hasValue(invalid.substring(0, 5));
    actor.keyboard().press("ControlOrMeta+A");
    pasteOtp(actor, invalid.substring(0, 3) + " " + invalid.substring(3));

    assertThat(input).hasValue("");
    assertThat(input).isFocused();
    assertThat(actor.locator("#two-factor-submit")).isEnabled();
    AllurePlaywright.attachScreenshot("OTP nach Tippen und Einfügen: " + id, actor);
    String valid = TwoFactorPage.totp(actor, email);
    if (pasteValidCode) {
      pasteOtp(actor, valid + "99");
    } else {
      actor.keyboard().type(valid);
    }
  }

  private void pasteOtp(Page actor, String value) {
    actor.evaluate("value => navigator.clipboard.writeText(value)", value);
    actor.keyboard().press("ControlOrMeta+V");
  }

  @TempDir Path profile;

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @Tag("ticket-layout")
  @Tag("follow-up-regression")
  @Tag("ticket-browser-detail")
  @DisplayName("MFA-20: Ticketzähler und Glocke folgen Zuständigkeit, Beobachtung und Abschluss")
  void ticketUnreadAndNotificationsFollowAssignmentAndClosure(int width) {
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    String suffix = UUID.randomUUID().toString();
    String requesterEmail = "ticket-requester-" + suffix + "@example.test";
    String otherRequesterEmail = "ticket-other-" + suffix + "@example.test";
    String assigneeEmail = "ticket-assignee-" + suffix + "@example.test";
    String firstName = "AlexandervonWiederherstellungsanfragen";
    String lastName = "MusterbearbeiterMitBesondersLangemNamen";
    AdminUsersPage users = new AdminUsersPage(browserPage);
    users.createActiveUser(requesterEmail, BrowserTestAccounts.USER_PASSWORD);
    users.createActiveUser(otherRequesterEmail, BrowserTestAccounts.USER_PASSWORD);
    AdminUserDialog assigneeForm = users.openCreate();
    assigneeForm.fillNewUser(assigneeEmail, BrowserTestAccounts.USER_PASSWORD, true, true);
    assigneeForm.firstName().fill(firstName);
    assigneeForm.lastName().fill(lastName);
    assigneeForm.save();
    browserPage.setViewportSize(width, 1000);
    browserPage.navigate("/home");

    assertThat(browserPage.locator("#account-header-notifications")).isVisible();
    int previousNotifications = notificationCount(browserPage);
    try (BrowserActor assignee = new BrowserActor(testBrowser);
        BrowserActor requester = new BrowserActor(testBrowser)) {
      assignee.login(assigneeEmail, BrowserTestAccounts.USER_PASSWORD);
      requester.login(requesterEmail, BrowserTestAccounts.USER_PASSWORD);
      Page admin = assignee.browserPage;
      Page user = requester.browserPage;
      new UserMenu(admin).selectTestLanguage();
      new UserMenu(user).selectTestLanguage();
      admin.setViewportSize(width, 1000);
      user.setViewportSize(width, 1000);
      user.navigate("/recovery-help");
      user.locator("#support-reply textarea")
          .fill("Eigene isolierte Anfrage für die Glockenregeln.");
      UiActions.complete(user, () -> user.locator("#support-create").click());
      String number =
          user.locator("#support-ticket-grid")
              .getByText(Pattern.compile("^#[0-9]+$"))
              .first()
              .innerText();
      assertNotificationCount(browserPage, previousNotifications + 1);
      assertNotificationCount(admin, 1);
      NotificationInboxPage inbox = new NotificationInboxPage(browserPage);
      verifyClaimNotification(admin, inbox, number, previousNotifications);
      inbox.closeDetail();
      browserPage.navigate("/admin?section=support");

      assertThat(ticketCard(browserPage, number).locator(".support-ticket-unread")).hasCount(0);
      UiActions.complete(browserPage, () -> ticketCard(browserPage, number).click());
      verifyAssignedIdentities(
          admin, number, requesterEmail, assigneeEmail, firstName + " " + lastName);
      assertRequesterCardLayout(user, number);
      verifyWatchedAndUnwatchedReplies(admin, user, number, previousNotifications);
      UiActions.complete(admin, () -> ticketCard(admin, number).click());
      setTicketWatch(admin, true);
      admin.navigate("/home");
      user.locator("#support-reply textarea")
          .fill("Neue Nachricht für den beobachtenden Bearbeiter.");
      UiActions.complete(user, () -> user.locator("#support-add-reply").click());
      assertNotificationCount(admin, 1);
      admin.navigate("/admin?section=support");
      UiActions.complete(admin, () -> ticketCard(admin, number).click());
      assertNotificationCount(admin, 0);
      setTicketWatch(admin, false);
      UiActions.complete(browserPage, () -> ticketCard(browserPage, number).click());
      setTicketWatch(browserPage, true);
      browserPage.navigate("/admin?section=support");
      admin.navigate("/home");
      verifyRequesterClosureSurvivesOlderRead(user, number, previousNotifications);
      assertNotificationCount(browserPage, previousNotifications + 1);
      // Der Bearbeiter erhält den Nutzerabschluss ausdrücklich auch ohne aktiviertes Beobachten.
      assertNotificationCount(admin, 1);

      assertThat(user.locator("#support-add-reply")).not().isVisible();
      verifyClosureNotification(admin, inbox, number, previousNotifications);

      verifyUnassignedClosure(admin, user, number, previousNotifications);
      verifyLiveRequesterSwitch(number, otherRequesterEmail);
      AllurePlaywright.attachScreenshot("Abgeschlossener Vorgang ohne Ungelesen-Zähler", admin);
    }
  }

  private void verifyUnassignedClosure(
      Page admin, Page user, String number, int previousNotifications) {
    user.locator("#support-reply textarea").fill("Zweite Anfrage ohne Bearbeiter.");
    UiActions.complete(user, () -> user.locator("#support-create").click());

    assertThat(user.locator("#support-ticket-grid").getByText(Pattern.compile("^#[0-9]+$")).first())
        .not()
        .hasText(number);
    String unassignedNumber =
        user.locator("#support-ticket-grid")
            .getByText(Pattern.compile("^#[0-9]+$"))
            .first()
            .innerText();
    UiActions.complete(user, () -> user.locator("#support-close").click());
    assertNotificationCount(browserPage, previousNotifications + 1);
    assertNotificationCount(admin, 1);

    assertThat(ticketCard(admin, unassignedNumber).locator(".support-ticket-unread")).hasText("1");
    UiActions.complete(admin, () -> ticketCard(admin, unassignedNumber).click());
    assertNotificationCount(admin, 0);
    UiActions.complete(admin, () -> admin.locator("#support-close").click());
    assertNotificationCount(browserPage, previousNotifications);

    assertThat(admin.locator("#support-ticket-status-note"))
        .hasText(uiTexts.getSupportClosedHint());
    verifyOtherTicketCounterSurvivesReading(admin, user, number);
  }

  private void verifyWatchedAndUnwatchedReplies(
      Page admin, Page user, String number, int previousNotifications) {
    setTicketWatch(browserPage, true);
    browserPage.navigate("/home");
    admin.navigate("/home");
    user.locator("#support-reply textarea")
        .fill("Neue Nachricht für den beobachtenden Superadmin.");
    UiActions.complete(user, () -> user.locator("#support-add-reply").click());
    assertNotificationCount(browserPage, previousNotifications + 1);
    assertNotificationCount(admin, 0);
    browserPage.navigate("/admin?section=support");
    admin.navigate("/admin?section=support");

    assertThat(ticketCard(browserPage, number).locator(".support-ticket-unread")).hasText("1");
    assertThat(ticketCard(admin, number).locator(".support-ticket-unread")).hasText("1");
    try (Page second = browserPage.context().newPage()) {
      second.setViewportSize(browserPage.viewportSize().width, 900);
      second.navigate("/admin?section=support");

      assertThat(ticketCard(second, number).locator(".support-ticket-unread")).hasText("1");
      Locator unread = ticketCard(second, number).locator(".support-ticket-unread");
      withDelayedTicketRead(
          browserPage,
          number,
          acknowledge -> {
            assertLongTicketCardFits(ticketCard(second, number));
            user.locator("#support-reply textarea").fill("Nachricht während der Lesequittierung.");
            UiActions.complete(user, () -> user.locator("#support-add-reply").click());

            assertThat(unread).hasText("2");
            acknowledge.run();

            assertThat(unread).hasText("1");
          });

      assertThat(ticketCard(second, number).locator(".support-ticket-unread")).hasCount(0);
      assertNotificationCount(second, previousNotifications);
    }
    assertNotificationCount(browserPage, previousNotifications);
    setTicketWatch(browserPage, false);
    browserPage.navigate("/home");
    user.locator("#support-reply textarea").fill("Ohne Beobachtung bleibt die Glocke ruhig.");
    UiActions.complete(user, () -> user.locator("#support-add-reply").click());
    // Erst die tatsächlich eingetroffene Live-Aktualisierung prüfen, dann das Ausbleiben des
    // Zählers.
    assertThat(ticketCard(admin, number).locator(".support-ticket-unread")).hasText("3");
    browserPage.navigate("/admin?section=support");

    assertThat(ticketCard(browserPage, number).locator(".support-ticket-unread")).hasCount(0);
    assertNotificationCount(browserPage, previousNotifications);
    assertNotificationCount(admin, 0);
  }

  private void verifyRequesterClosureSurvivesOlderRead(
      Page requester, String number, int previousNotifications) {
    requester.locator("#support-reply textarea").fill("Letzte Nachricht vor dem Nutzerabschluss.");
    UiActions.complete(requester, () -> requester.locator("#support-add-reply").click());
    assertNotificationCount(browserPage, previousNotifications + 1);
    try (Page reader = browserPage.context().newPage()) {
      reader.setViewportSize(browserPage.viewportSize().width, 900);
      reader.navigate("/admin?section=support");
      withDelayedTicketRead(
          reader,
          number,
          acknowledge -> {
            UiActions.complete(requester, () -> requester.locator("#support-close").click());
            assertNotificationCount(browserPage, previousNotifications + 1);

            assertThat(ticketCard(browserPage, number).locator(".support-ticket-status"))
                .hasText(uiTexts.getSupportClosedByRequester());
            Locator unread = ticketCard(browserPage, number).locator(".support-ticket-unread");

            assertThat(unread).hasText("1");
            UiActions.complete(reader, acknowledge);

            assertThat(unread).hasCount(0);
            assertNotificationCount(browserPage, previousNotifications + 1);
            reader.navigate("/home");
          });
    }
  }

  private void withDelayedTicketRead(Page reader, String number, Consumer<Runnable> whileHeld) {
    // Die echte Vaadin-Rückmeldung anhalten, bis die neuere Änderung gespeichert ist.
    Predicate<String> uidl =
        url -> {
          String query = URI.create(url).getRawQuery();
          return query != null && List.of(query.split("&")).contains("v-r=uidl");
        };
    Deque<Route> acknowledgements = new ArrayDeque<>();
    Consumer<Route> hold =
        route -> {
          if (UiActions.carriesSuccessfulReturn(route.request())) {
            acknowledgements.addLast(route);
          } else {
            route.resume();
          }
        };
    reader.route(uidl, hold);
    try {
      reader.bringToFront();
      UiActions.complete(reader, () -> ticketCard(reader, number).click());
      reader.waitForCondition(() -> !acknowledgements.isEmpty());
      whileHeld.accept(() -> acknowledgements.removeFirst().resume());
    } finally {
      reader.unroute(uidl, hold);
      while (!acknowledgements.isEmpty()) {
        acknowledgements.removeFirst().resume();
      }
    }
  }

  private void verifyClaimNotification(
      Page admin, NotificationInboxPage inbox, String number, int previousNotifications) {
    browserPage.navigate("/admin?section=support");

    assertThat(ticketCard(browserPage, number)).isVisible();
    browserPage.locator("#support-filter").click();
    Locator filters = browserPage.locator("#support-filter-panel");
    filters
        .locator("#support-filter-assignment")
        .getByText(uiTexts.getSupportFilterMine(), new Locator.GetByTextOptions().setExact(true))
        .click();
    UiActions.complete(browserPage, () -> filters.locator("#support-filter-apply").click());

    assertThat(browserPage.locator("#support-active-filter-count")).hasText("1");
    assertThat(ticketCard(browserPage, number)).isHidden();
    inbox.openPreview();
    inbox
        .row(inbox.preview(), uiTexts.getSupportRecoverySubject())
        .locator(".cosmos-notification-row-open")
        .first()
        .click();

    assertThat(inbox.dialog().locator(".cosmos-notification-detail-state"))
        .hasText(uiTexts.getNotificationsUnread());
    Locator target = inbox.dialog().locator(".cosmos-notification-detail-ticket");

    assertThat(target).hasAccessibleName(uiTexts.getNotificationsOpenTicket());
    assertThat(inbox.dialog().locator(".cosmos-notification-detail-account")).hasCount(0);
    if (browserPage.viewportSize().width == 390) {
      assertThat(target).hasCSS("grid-column-start", "1");
      assertThat(target).hasCSS("grid-column-end", "-1");
    }
    target.click();

    assertThat(browserPage)
        .hasURL(
            Pattern.compile(
                "(?=.*[?&]section=support(?:&|$))(?=.*[?&]ticket="
                    + number.substring(1)
                    + "(?:&|$)).*/admin\\?.*"));
    assertThat(browserPage.locator("#support-ticket-panel .support-ticket-number"))
        .hasText(uiTexts.getSupportTicketNumber(number));
    assertThat(browserPage.locator("#support-active-filter-count")).isHidden();
    assertThat(ticketCard(browserPage, number)).isVisible();
    assertThat(browserPage.locator("#support-claim")).isVisible();
    // Erst der sichtbare Verlauf quittiert die darin dargestellte Nachricht.
    assertNotificationCount(browserPage, previousNotifications);
    inbox.open();
    inbox.filter(inbox.inbox(), uiTexts.getNotificationsAll());
    inbox
        .row(inbox.inbox(), uiTexts.getSupportRecoverySubject())
        .locator(".cosmos-notification-row-open")
        .first()
        .click();

    assertThat(inbox.dialog().locator(".cosmos-notification-detail-state"))
        .hasText(uiTexts.getNotificationsRead());
    admin.navigate("/admin?section=support");

    assertThat(ticketCard(admin, number).locator(".support-ticket-unread")).hasText("1");
    UiActions.complete(admin, () -> ticketCard(admin, number).click());
    assertNotificationCount(admin, 0);

    assertThat(admin.locator("#support-ticket-status-note"))
        .hasText(uiTexts.getSupportUnassignedHint());
    UiActions.complete(admin, () -> admin.locator("#support-claim").click());

    assertThat(admin.locator("#support-ticket-status-note")).not().isVisible();
    assertThat(inbox.dialog().locator("[data-relevance=TICKET_ASSIGNED]"))
        .hasText(uiTexts.getNotificationsTicketAssigned());
    assertNotificationCount(browserPage, previousNotifications);
    AllurePlaywright.attachScreenshot(
        "Übernommener Hinweis im geöffneten Glockendialog", browserPage);
  }

  private void verifyClosureNotification(
      Page admin, NotificationInboxPage inbox, String number, int previousNotifications) {
    inbox.open();
    inbox.search(uiTexts.getSupportTicketNumber("").strip());
    inbox
        .row(inbox.inbox(), uiTexts.getSupportRequesterClosedNotificationTitle(number))
        .locator(".cosmos-notification-row-open")
        .click();
    admin.navigate("/admin?section=support");
    UiActions.complete(admin, () -> ticketCard(admin, number).click());
    assertNotificationCount(admin, 0);

    assertThat(admin.locator("#support-ticket-status-note"))
        .hasText(uiTexts.getSupportRequesterClosedHint());
    assertThat(admin.locator("#support-ticket-status-note")).hasCSS("text-align", "center");
    assertThat(admin.locator("#support-ticket-status-note")).hasCSS("font-style", "italic");
    assertThat(admin.locator("#support-add-reply")).not().isVisible();
    AllurePlaywright.attachScreenshot("Nutzerabschluss mit prüfbarem Statushinweis", admin);
    UiActions.complete(admin, () -> admin.locator("#support-close").click());

    assertThat(admin.locator("#support-ticket-status-note"))
        .hasText(uiTexts.getSupportClosedHint());
    assertThat(ticketCard(admin, number).locator(".support-ticket-unread")).hasCount(0);
    assertThat(inbox.dialog().locator("[data-relevance=TICKET_CLOSED]"))
        .hasText(uiTexts.getSupportClosed());
    assertNotificationCount(browserPage, previousNotifications);
    AllurePlaywright.attachScreenshot(
        "Erledigter Glockenhinweis bleibt als Historie sichtbar", browserPage);
    inbox.closeDetail();
  }

  private Locator ticketCard(Page target, String number) {
    return target
        .locator(".support-admin-ticket-card")
        .filter(
            new Locator.FilterOptions()
                .setHas(target.getByText(number, new Page.GetByTextOptions().setExact(true))));
  }

  private void verifyOtherTicketCounterSurvivesReading(Page admin, Page requester, String closed) {
    Locator newestRequest =
        requester.locator("#support-ticket-grid").getByText(Pattern.compile("^#[0-9]+$")).first();
    String previous = newestRequest.innerText();
    requester.locator("#support-reply textarea").fill("Unabhängiger Vorgang für den Zähler.");
    UiActions.complete(requester, () -> requester.locator("#support-create").click());

    assertThat(newestRequest).not().hasText(previous);
    String other = newestRequest.innerText();
    Locator unread = ticketCard(admin, other).locator(".support-ticket-unread");

    assertThat(unread).hasText("1");
    UiActions.complete(admin, () -> ticketCard(admin, closed).click());

    assertThat(admin.locator("#support-ticket-panel .support-ticket-number"))
        .hasText(uiTexts.getSupportTicketNumber(closed));
    assertThat(unread).hasText("1");
    assertNotificationCount(admin, 1);
    UiActions.complete(admin, () -> ticketCard(admin, other).click());
    assertNotificationCount(admin, 0);
    UiActions.complete(admin, () -> admin.locator("#support-claim").click());
    verifyInternalNoteLeavesHiddenConversationUnread(admin, requester, closed, other);
    UiActions.complete(admin, () -> admin.locator("#support-close").click());
  }

  private Locator requesterCard(Page requester, String number) {
    return requester
        .locator(".support-user-requests .support-ticket-row")
        .filter(
            new Locator.FilterOptions()
                .setHas(requester.getByText(number, new Page.GetByTextOptions().setExact(true))));
  }

  private void verifyInternalNoteLeavesHiddenConversationUnread(
      Page admin, Page requester, String closed, String other) {
    Locator activeCard = requesterCard(requester, other);

    assertThat(activeCard.locator(".support-ticket-status"))
        .hasText(uiTexts.getSupportInProgress());
    requester.bringToFront();
    UiActions.complete(
        requester,
        () -> requesterCard(requester, closed).locator(".support-ticket-number").click());
    Locator displayedNumber =
        requester.locator(".support-conversation-heading .support-ticket-number");

    assertThat(displayedNumber).hasText(closed);
    Locator composer = admin.locator("#support-reply textarea");
    String reply = "Öffentliche Antwort für den Ungelesen-Zähler.";
    composer.fill(reply);
    UiActions.complete(admin, () -> admin.locator("#support-add-reply").click());
    Locator unread = activeCard.locator(".support-ticket-unread");

    assertThat(unread).hasText("1");
    assertThat(requester.locator(".support-message-thread")).not().containsText(reply);

    String note = "Interner Vermerk während ein anderer Verlauf geöffnet ist.";
    composer.fill(note);
    admin.locator("#support-internal-note").click();
    UiActions.complete(admin, () -> admin.locator("#support-add-reply").click());

    assertThat(admin.locator(".support-internal-message")).containsText(note);
    UiActions.complete(requester, () -> requester.locator("#support-reload").click());

    assertThat(unread).hasText("1");
    assertThat(displayedNumber).hasText(closed);
    assertThat(requester.locator(".support-message-thread")).not().containsText(reply);
    UiActions.complete(requester, () -> activeCard.locator(".support-ticket-number").click());

    assertThat(displayedNumber).hasText(other);
    assertThat(requester.locator(".support-message-thread")).containsText(reply);
    assertThat(requester.locator(".support-message-thread")).not().containsText(note);
    assertThat(unread).hasCount(0);
  }

  private void verifyLiveRequesterSwitch(String firstTicket, String otherEmail) {
    browserPage.navigate("/admin?section=support&ticket=" + firstTicket.substring(1));
    Locator avatar = browserPage.locator("#support-detail-requester .cosmos-user-avatar");

    assertThat(avatar).isVisible();
    String firstEmail =
        browserPage.locator("#support-detail-requester .support-admin-person-email").innerText();
    assertIdentityTooltip(browserPage, avatar, "Browser Testuser", firstEmail, false);
    try (BrowserActor other = new BrowserActor(testBrowser)) {
      other.login(otherEmail, BrowserTestAccounts.USER_PASSWORD);
      other.browserPage.setViewportSize(browserPage.viewportSize().width, 1000);
      other.browserPage.navigate("/recovery-help");
      other
          .browserPage
          .locator("#support-reply textarea")
          .fill("Anfrage eines anderen Testkontos.");
      UiActions.complete(
          other.browserPage, () -> other.browserPage.locator("#support-create").click());
      String number =
          other
              .browserPage
              .locator("#support-ticket-grid .support-ticket-number")
              .first()
              .innerText();
      UiActions.complete(browserPage, () -> ticketCard(browserPage, number).click());
      assertIdentityTooltip(browserPage, avatar, "Browser Testuser", otherEmail, false);
      String followUp = "Ergänzung des zweiten Testkontos im Live-Verlauf.";
      other.browserPage.locator("#support-reply textarea").fill(followUp);
      UiActions.complete(
          other.browserPage, () -> other.browserPage.locator("#support-add-reply").click());
      Locator message =
          browserPage
              .locator("#admin-support .support-message")
              .filter(new Locator.FilterOptions().setHasText(followUp));

      assertThat(message).isVisible();
      assertIdentityTooltip(
          browserPage,
          message.locator(".cosmos-user-avatar"),
          "Browser Testuser",
          otherEmail,
          false);
      assertIdentityTooltip(browserPage, avatar, "Browser Testuser", otherEmail, false);
    }
  }

  private void assertRequesterCardLayout(Page requester, String number) {
    Locator card = requesterCard(requester, number);

    assertThat(card.locator(".support-ticket-details .support-ticket-number")).hasText(number);
    assertThat(card.locator(".support-ticket-details .support-ticket-title")).isVisible();
    assertThat(card.locator(".support-ticket-details .support-ticket-updated")).isVisible();
    assertThat(card.locator(".support-ticket-indicators .support-ticket-status")).isVisible();
    assertThat(card.locator(".support-admin-ticket-footer")).hasCount(0);
  }

  private void assertLongTicketCardFits(Locator card) {
    assertThat(card.locator(".support-ticket-status")).isVisible();
    assertThat(card.locator(".support-ticket-unread")).hasText("1");
    assertTrue(
        (Boolean)
            card.evaluate(
                """
                card => {
                  const title = card.querySelector('.support-ticket-title');
                  const original = title.textContent;
                  // Wiederherstellungsanfragen haben einen festen Betreff. Für das Layout verlängern
                  // wir den gerenderten Text, ohne die Ticketdaten oder den Serverzustand zu ändern.
                  try {
                    title.textContent = (original + ' ').repeat(8);
                    const bounds = card.getBoundingClientRect();
                    const subject = title.getBoundingClientRect();
                    const badges = ['.support-ticket-status', '.support-ticket-unread']
                        .map(selector => card.querySelector(selector).getBoundingClientRect());
                    return title.scrollWidth <= title.clientWidth
                        && subject.height > parseFloat(getComputedStyle(title).lineHeight)
                        && badges.every(badge => badge.width > 0 && badge.left >= bounds.left - 1
                            && badge.right <= bounds.right + 1 && badge.bottom <= subject.top + 1);
                  } finally {
                    title.textContent = original;
                  }
                }
                """),
        "Ein langer Betreff muss umbrechen und Platz für beide Badges lassen.");
  }

  private void verifyAssignedIdentities(
      Page assignee,
      String number,
      String requesterEmail,
      String assigneeEmail,
      String assigneeName) {
    Locator foreign = browserPage.locator("#support-detail-assignee");
    Locator own = assignee.locator("#support-detail-assignee");
    Locator requester = browserPage.locator("#support-detail-requester");

    assertThat(requester.locator(".support-admin-person-label"))
        .hasText(uiTexts.getSupportRequester());
    assertThat(foreign.locator(".support-admin-person-label"))
        .hasText(uiTexts.getSupportAssigneeColumn());
    for (Locator person : List.of(requester, foreign)) {
      assertThat(person.locator(".support-admin-person-label"))
          .hasCSS("text-transform", "uppercase");
      assertThat(person.locator(".support-admin-person-label"))
          .hasCSS("color", "rgb(56, 189, 248)");
    }
    assertThat(foreign).hasCSS("align-items", "flex-end");
    assertThat(foreign.locator(".support-admin-person-email")).hasText(assigneeEmail);
    Locator name = foreign.locator(".support-admin-person-name");

    assertThat(name).hasText(assigneeName);
    if (browserPage.viewportSize().width == 390) {
      assertTrue(
          (Boolean)
              name.evaluate(
                  """
                  name => name.getBoundingClientRect().height > parseFloat(getComputedStyle(name).lineHeight)
                      && name.scrollWidth <= name.clientWidth
                  """));
    }
    assertThat(foreign.locator(".support-assignee-self")).hasCount(0);
    assertThat(ticketCard(browserPage, number).locator(".support-assignee-self")).hasCount(0);
    assertThat(own.locator(".support-assignee-self")).hasText(uiTexts.getSupportYou());
    assertThat(own.locator(".support-assignee-self"))
        .hasCSS("border-top-color", "rgba(16, 185, 129, 0.2)");
    assertThat(own.locator(".support-assignee-self")).hasCSS("border-top-width", "1px");
    assertThat(ticketCard(assignee, number).locator(".support-assignee-self"))
        .hasText(uiTexts.getSupportYou());
    assertThat(foreign.locator(".cosmos-user-avatar")).hasCSS("width", "32px");
    assertThat(own.locator(".cosmos-user-avatar")).hasCSS("width", "32px");
    assertIdentityTooltip(
        browserPage,
        ticketCard(browserPage, number).locator(".cosmos-user-avatar"),
        assigneeName,
        assigneeEmail,
        false);
    assertIdentityTooltip(
        browserPage, foreign.locator(".cosmos-user-avatar"), assigneeName, assigneeEmail, true);

    assertThat(requester.locator(".support-admin-person-email")).hasText(requesterEmail);
    assertThat(requester.locator(".support-admin-person-name")).hasText("Browser Testuser");
    assertThat(requester.locator(".cosmos-user-avatar")).hasCSS("width", "32px");
    assertIdentityTooltip(
        browserPage,
        requester.locator(".cosmos-user-avatar"),
        "Browser Testuser",
        requesterEmail,
        false);
  }

  private void assertIdentityTooltip(
      Page view, Locator avatar, String name, String email, boolean keyboard) {
    if (keyboard) {
      view.mouse().move(0, 0);
      view.keyboard().press("Tab");
      avatar.focus();

      assertThat(avatar).isFocused();
    } else {
      avatar.hover();
    }
    Locator tooltip =
        view.getByRole(AriaRole.TOOLTIP).filter(new Locator.FilterOptions().setHasText(email));

    assertThat(tooltip).isVisible();
    assertEquals(name + "\n" + email, tooltip.textContent());
    assertTrue(
        (Boolean)
            tooltip.evaluate(
                """
                text => {
                  const bounds = text.getBoundingClientRect();
                  const lineHeight = parseFloat(getComputedStyle(text).lineHeight);
                  const width = document.documentElement.clientWidth;
                  return Math.abs(bounds.height - 2 * lineHeight) < 1
                      && bounds.left >= 0 && bounds.right <= width
                      && (width < 1000 || text.scrollWidth <= text.clientWidth);
                }
                """),
        "Name und E-Mail müssen innerhalb des Fensters je eine Zeile belegen.");
  }

  private int notificationCount(Page target) {
    return ((Number)
            target.evaluate(
                """
                () => {
                  const badge = document.querySelector('.cosmos-notification-count');
                  return badge?.getClientRects().length ? Number(badge.textContent) : 0;
                }
                """))
        .intValue();
  }

  private void assertNotificationCount(Page target, int expected) {
    assertThat(target.locator("#account-header-notifications")).isVisible();
    target.waitForCondition(() -> notificationCount(target) == expected);
  }

  private void setTicketWatch(Page target, boolean checked) {
    Locator control = target.locator("#support-subscription");
    // Eine noch laufende Lesequittierung ist keine Bestätigung der Switch-Änderung.
    target
        .waitForResponse(
            response ->
                response.request().method().equals("POST")
                    && response.url().contains("v-r=uidl")
                    && response.request().postData() != null
                    && response.request().postData().contains("\"checked\""),
            () -> control.getByRole(AriaRole.SWITCH).setChecked(checked))
        .finished();
    if (checked) {
      assertThat(control).hasAttribute("checked", "");
    } else {
      assertThat(control).not().hasAttribute("checked", "");
    }
  }

  @Test
  @Tag("ticket-layout")
  @Tag("ticket-panel-style")
  @Tag("follow-up-regression")
  @Tag("ticket-browser-detail")
  @DisplayName("MFA-19: Ticketliste, Panel-Tabs und Auswahlhinweis passen zur verfügbaren Breite")
  void administrativeTicketLayoutKeepsTabsAndCardsUnclipped() {
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    browserPage.navigate("/admin?section=configuration&group=external_login");
    Locator providerTab = browserPage.locator("#admin-provider-tabs vaadin-tab[selected]");
    Map<Integer, Object> providerTabStyles = new HashMap<>();
    for (int width : new int[] {1280, 390}) {
      browserPage.setViewportSize(width, 1000);

      assertThat(providerTab).isVisible();
      providerTabStyles.put(width, selectedTabStyle(providerTab));
    }
    browserPage.navigate("/recovery-help");
    browserPage
        .locator("#support-reply textarea")
        .fill("Synthetische erste Anfrage für die Layoutprüfung.");
    UiActions.complete(browserPage, () -> browserPage.locator("#support-create").click());
    String ownClosedNumber =
        browserPage
            .locator("#support-ticket-grid")
            .getByText(Pattern.compile("^#[0-9]+$"))
            .first()
            .innerText();
    UiActions.complete(browserPage, () -> browserPage.locator("#support-close").click());
    browserPage
        .locator("#support-reply textarea")
        .fill("Synthetische offene Anfrage für die Layoutprüfung.");
    UiActions.complete(browserPage, () -> browserPage.locator("#support-create").click());

    assertThat(
            browserPage
                .locator("#support-ticket-grid")
                .getByText(Pattern.compile("^#[0-9]+$"))
                .first())
        .not()
        .hasText(ownClosedNumber);
    String ownOpenNumber =
        browserPage
            .locator("#support-ticket-grid")
            .getByText(Pattern.compile("^#[0-9]+$"))
            .first()
            .innerText();
    browserPage.locator("#support-back").click();

    assertThat(browserPage.locator("#account-panel-security")).isVisible();
    browserPage.navigate("/admin?section=support");
    Locator categories = browserPage.locator("#support-ticket-categories");
    Locator hint = browserPage.locator("#support-select-ticket");
    Locator heading = browserPage.locator("#support-page-header");

    assertThat(heading).containsText(uiTexts.getSupportPageDescription());
    assertThat(browserPage.locator("#support-panel .cosmos-panel-description").first())
        .hasText(uiTexts.getSupportInboxDescription());
    assertThat(hint).containsText(uiTexts.getSupportSelectTicket());
    assertThat(hint.locator("[data-lucide=mouse-pointer-click]")).isVisible();
    UiActions.complete(browserPage, () -> categories.locator("vaadin-tab").nth(1).click());
    assertTicketLocation("recovery", null);

    assertThat(heading).containsText(uiTexts.getSupportPageDescription());
    UiTexts otherLanguage = new UiTexts(uiTexts.language().equals("de") ? "en" : "de");
    new UserMenu(browserPage).selectLanguage(otherLanguage.language());

    assertThat(heading).containsText(otherLanguage.getSupportPageDescription());
    assertThat(categories.locator("vaadin-tab[selected]"))
        .hasText(otherLanguage.getSupportRecoveryCategory());
    new UserMenu(browserPage).selectTestLanguage();
    browserPage.reload();

    assertThat(categories.locator("vaadin-tab[selected]"))
        .hasText(uiTexts.getSupportRecoveryCategory());
    assertThat(hint.locator("[data-lucide=mouse-pointer-click]")).isVisible();
    assertThat(ticketCard(browserPage, ownClosedNumber)).hasCount(1);
    assertThat(ticketCard(browserPage, ownOpenNumber)).hasCount(1);
    verifyUnselectedTicketLayout(categories, ownOpenNumber, providerTabStyles);
    Locator ticketPanel = browserPage.locator("#support-ticket-panel");
    Locator tools = ticketPanel.locator("#support-ticket-tools");
    Locator footer = ticketPanel.locator("#support-ticket-footer");
    String openNumber = verifyTicketSelectionAfterReload(categories, ticketPanel);
    verifyOpenTicketAndFocusMode(ticketPanel, tools, footer, openNumber);
    String openStatusBackground = verifyAssignedTicketLayout(ticketPanel, tools, footer);
    verifyTicketHistoryAndInternalEditor(ticketPanel, openStatusBackground);
    browserPage.navigate("/recovery-help");

    assertThat(browserPage.locator(".support-history-event")).hasCount(0);
    assertThat(browserPage.locator(".support-internal-message")).hasCount(0);
    assertThat(browserPage.locator(".support-message-copy")).not().containsText("Nur intern:");
    browserPage.locator("#support-back").click();

    assertThat(browserPage.locator("#account-panel-security")).isVisible();
    browserPage.navigate("/notifications");

    assertThat(browserPage.locator(".portal-notification-source-system").first()).isVisible();
    AllurePlaywright.attachScreenshot(
        "Gemeinsame Systemfarbe im Benachrichtigungsbadge", browserPage);
  }

  private void verifyUnselectedTicketLayout(
      Locator categories, String ownOpenNumber, Map<Integer, Object> providerTabStyles) {
    for (int width : new int[] {1600, 1280, 1024, 390}) {
      browserPage.setViewportSize(width, 1000);
      if (providerTabStyles.containsKey(width)) {
        assertEquals(
            providerTabStyles.get(width),
            selectedTabStyle(categories.locator("vaadin-tab[selected]")));
      }
      Locator unassignedCard = ticketCard(browserPage, ownOpenNumber);

      assertThat(unassignedCard.locator(".support-ticket-unassigned"))
          .hasCSS("font-style", "italic");
      assertThat(unassignedCard.locator(".support-admin-ticket-metadata .support-ticket-status"))
          .isVisible();
      assertThat(unassignedCard.locator(":scope > .support-ticket-title")).isVisible();
      assertThat(unassignedCard.locator(".support-admin-ticket-footer .support-ticket-updated"))
          .isVisible();
      browserPage.waitForCondition(() ->
          (Boolean)
              browserPage.evaluate(
                  """
                  () => {
                    const body = document.querySelector('#support-panel > .account-panel-body').getBoundingClientRect();
                    const tabs = document.querySelector('#support-ticket-categories').getBoundingClientRect();
                    const queue = document.querySelector('.support-admin-queue').getBoundingClientRect();
                    const detail = document.querySelector('.support-admin-detail-column').getBoundingClientRect();
                    const hint = document.querySelector('#support-select-ticket').getBoundingClientRect();
                    const tabItems = [...document.querySelectorAll('#support-ticket-categories vaadin-tab')];
                    const tabsFit = tabs.left >= body.left - 1 && tabs.right <= body.right + 1
                        && tabs.height >= 48 && tabItems.every(item => {
                          const rect = item.getBoundingClientRect();
                          return rect.top >= tabs.top - 1 && rect.bottom <= tabs.bottom + 1;
                        });
                    const columnsFit = body.width >= 820
                        ? queue.width >= 330 && detail.left >= queue.right + 15
                        : detail.top >= queue.bottom - 1 && Math.abs(detail.width - queue.width) < 2;
                    const centered = Math.abs(hint.left + hint.width / 2 - detail.left - detail.width / 2) < 2
                        && Math.abs(hint.top + hint.height / 2 - detail.top - detail.height / 2) < 2;
                    return tabsFit && columnsFit && centered
                        && document.documentElement.scrollWidth <= window.innerWidth;
                  }
                  """));
      AllurePlaywright.attachScreenshot(
          "Ticketübersicht und Auswahlhinweis bei " + width + "px", browserPage);
    }
  }

  private Object selectedTabStyle(Locator tab) {
    return tab.evaluate(
        """
        tab => {
          const style = getComputedStyle(tab);
          return [tab.offsetHeight, style.fontFamily, style.fontSize, style.fontWeight,
            style.borderTopWidth, style.borderTopColor, style.borderBottomWidth, style.borderBottomColor];
        }
        """);
  }

  private void verifyTicketHistoryAndInternalEditor(
      Locator ticketPanel, String openStatusBackground) {
    assertThat(ticketPanel.locator(".support-history-event")).hasCount(2);
    assertThat(ticketPanel.locator(".support-history-event [data-status=OPEN]"))
        .hasCSS("background-color", openStatusBackground);
    assertThat(ticketPanel.locator(".support-history-event[data-event=STATUS_CHANGED]"))
        .containsText(uiTexts.getSupportOpen());
    assertThat(ticketPanel.locator(".support-history-event[data-event=STATUS_CHANGED]"))
        .containsText(uiTexts.getSupportInProgress());
    assertThat(ticketPanel.locator(".support-section-header vaadin-badge"))
        .hasText(uiTexts.getSupportMessageCount(1));
    String createdTime =
        ticketPanel.locator("#support-ticket-header .support-ticket-updated").innerText();
    Locator editor = ticketPanel.locator(".support-admin-editor");
    for (int width : new int[] {1280, 390}) {
      browserPage.setViewportSize(width, 1000);

      assertThat(editor.locator("#support-public-reply")).hasAttribute("selected", "");
      assertThat(ticketPanel.locator("#support-reply"))
          .hasJSProperty("label", uiTexts.getSupportReply());
      UiActions.complete(browserPage, () -> editor.locator("#support-internal-note").click());

      assertThat(editor).hasAttribute("data-internal", "true");
      assertThat(editor.locator(".cosmos-security-warning"))
          .containsText(uiTexts.getSupportInternalSecurityNotice());
      assertThat(editor.locator(".support-help-notice")).hasCount(0);
      assertThat(ticketPanel.locator("#support-reply"))
          .hasJSProperty("label", uiTexts.getSupportInternal());
      assertThat(ticketPanel.locator("#support-add-reply"))
          .hasText(uiTexts.getSupportSaveInternal());
      assertThat(ticketPanel.locator("#support-reply textarea"))
          .hasValue("Dieser Entwurf bleibt beim Vergrößern erhalten.");
      editor.scrollIntoViewIfNeeded();

      assertTrue(
          (Boolean)
              browserPage.evaluate("document.documentElement.scrollWidth <= window.innerWidth"));
      AllurePlaywright.attachScreenshot(
          "Interner Editor und History bei " + width + "px", browserPage);
      UiActions.complete(browserPage, () -> editor.locator("#support-public-reply").click());

      assertThat(editor.locator(".cosmos-security-warning"))
          .containsText(uiTexts.getSupportSecurityNotice());
    }
    UiActions.complete(browserPage, () -> editor.locator("#support-internal-note").click());
    ticketPanel.locator("#support-reply textarea").fill("Nur intern: synthetischer Vermerk.");
    UiActions.complete(browserPage, () -> ticketPanel.locator("#support-add-reply").click());

    assertThat(ticketPanel.locator(".support-internal-message .support-message-copy"))
        .hasText("Nur intern: synthetischer Vermerk.");
    assertThat(ticketPanel.locator(".support-section-header vaadin-badge"))
        .hasText(uiTexts.getSupportMessageCount(2));
    assertThat(ticketPanel.locator(".support-history-event")).hasCount(2);
    assertThat(ticketPanel.locator("#support-ticket-header .support-ticket-updated"))
        .hasText(createdTime);
    Locator messageScroll = ticketPanel.locator(".support-message-scroll");
    for (int width : new int[] {1280, 390}) {
      browserPage.setViewportSize(width, 1000);
      messageScroll.scrollIntoViewIfNeeded();
      messageScroll.evaluate("element => element.scrollTop = element.scrollHeight");

      assertTrue(
          (Boolean)
              messageScroll.evaluate("element => element.scrollWidth <= element.clientWidth"));
      assertThat(ticketPanel.locator(".support-internal-message .support-message-copy"))
          .isVisible();
      AllurePlaywright.attachScreenshot(
          "Vermerk und native Scrollleiste bei " + width + "px", browserPage);
    }
  }

  private String verifyAssignedTicketLayout(Locator ticketPanel, Locator tools, Locator footer) {
    String openStatusBackground =
        (String)
            ticketPanel
                .locator(".support-admin-ticket-metadata > .support-ticket-status")
                .evaluate("badge => getComputedStyle(badge).backgroundColor");
    UiActions.complete(browserPage, () -> tools.locator("#support-claim").click());

    assertThat(ticketPanel.locator("#support-ticket-header"))
        .containsText(uiTexts.getSupportInProgress());
    assertThat(tools.locator("#support-instructions")).isVisible();
    assertThat(footer.locator("#support-add-reply")).isVisible();
    assertThat(footer.locator("#support-close")).isVisible();
    assertThat(ticketPanel.locator("#support-ticket-status-note")).not().isVisible();
    ticketPanel
        .locator("#support-reply textarea")
        .fill("Dieser Entwurf bleibt beim Vergrößern erhalten.");
    UiActions.complete(browserPage, () -> ticketPanel.locator("#support-focus-toggle").click());
    UiActions.complete(browserPage, () -> ticketPanel.locator("#support-focus-toggle").click());

    assertThat(ticketPanel.locator("#support-reply textarea"))
        .hasValue("Dieser Entwurf bleibt beim Vergrößern erhalten.");
    Locator requesterAvatar = ticketPanel.locator("#support-detail-requester .cosmos-user-avatar");

    assertThat(requesterAvatar.locator(".cosmos-avatar-initials")).hasCSS("cursor", "default");
    requesterAvatar.hover();

    assertThat(browserPage.locator("vaadin-tooltip-overlay[opened] [part=overlay]"))
        .hasCSS("pointer-events", "none");
    assertThat(requesterAvatar.locator(".cosmos-avatar-initials")).hasCSS("cursor", "default");
    Locator reload = browserPage.locator("#support-reload");

    assertThat(reload).hasCSS("cursor", "pointer");
    assertThat(reload).hasAttribute("aria-label", uiTexts.getSessionRefresh());
    assertThat(reload.locator("[data-lucide=refresh-cw]")).isVisible();
    BoundingBox reloadSize = reload.boundingBox();

    assertEquals(reloadSize.width, reloadSize.height, 1);
    reload.hover();

    assertThat(
            browserPage
                .getByRole(AriaRole.TOOLTIP)
                .filter(new Locator.FilterOptions().setHasText(uiTexts.getSessionRefresh())))
        .hasText(uiTexts.getSessionRefresh());
    assertThat(reload).hasCSS("cursor", "pointer");
    assertThat(ticketPanel.locator("#support-reply textarea")).hasCSS("cursor", "text");
    for (int width : new int[] {1280, 390}) {
      browserPage.setViewportSize(width, 1000);
      browserPage.waitForCondition(() ->
          (Boolean)
              ticketPanel.evaluate(
                  """
                  panel => {
                    const bounds = panel.getBoundingClientRect();
                    const sections = ['#support-ticket-header', '#support-ticket-tools', '#support-ticket-footer']
                        .map(selector => panel.querySelector(selector));
                    const policy = panel.querySelector('#support-ticket-policy');
                    const policyIcon = policy.querySelector('vaadin-icon').getBoundingClientRect();
                    const policyText = policy.querySelector(':scope > span').getBoundingClientRect();
                    const warning = panel.querySelector('.cosmos-security-warning');
                    const warningIcon = warning.querySelector('vaadin-icon').getBoundingClientRect();
                    const warningText = warning.querySelector(':scope > span').getBoundingClientRect();
                    const tools = panel.querySelector('#support-ticket-tools');
                    const toolsRight = tools.getBoundingClientRect().right
                        - parseFloat(getComputedStyle(tools).paddingRight);
                    const instructions = tools.querySelector('#support-instructions').getBoundingClientRect();
                    return sections.every(section => {
                      const rect = section.getBoundingClientRect();
                      return rect.width > 0 && rect.left >= bounds.left && rect.right <= bounds.right
                          && [...section.querySelectorAll('vaadin-button')].every(button => {
                            if (button.hidden) return true;
                            const action = button.getBoundingClientRect();
                            return action.left >= rect.left && action.right <= rect.right;
                          });
                    }) && Math.abs(policyIcon.top - policyText.top - 2) < 1
                        && Math.abs(warningIcon.top - warningText.top - 1) < 1
                        && Math.abs(instructions.right - toolsRight) < 1
                        && document.documentElement.scrollWidth <= window.innerWidth;
                  }
                  """));
      ticketPanel.locator("#support-ticket-header").scrollIntoViewIfNeeded();
      AllurePlaywright.attachScreenshot(
          "Ticket-Panelkopf und Werkzeugleiste bei " + width + "px", browserPage);
      footer.scrollIntoViewIfNeeded();
      AllurePlaywright.attachScreenshot("Ticket-Aktionsfooter bei " + width + "px", browserPage);
      browserPage.locator("#support-instructions").click();
      Locator preview = browserPage.locator("#support-instruction-preview");

      assertThat(preview).hasJSProperty("opened", true);
      browserPage.waitForCondition(() ->
          (Boolean)
              preview.evaluate(
                  """
                  dialog => {
                    const notice = dialog.querySelector('.support-help-notice');
                    const icon = notice.querySelector('vaadin-icon').getBoundingClientRect();
                    const copy = notice.querySelector(':scope > span').getBoundingClientRect();
                    return Math.abs(icon.top + icon.height / 2 - copy.top - copy.height / 2) < 1;
                  }
                  """));
      browserPage.locator("#support-instruction-preview-cancel").click();

      assertThat(preview).hasJSProperty("opened", false);
    }
    return openStatusBackground;
  }

  private void verifyOpenTicketAndFocusMode(
      Locator ticketPanel, Locator tools, Locator footer, String openNumber) {
    AllurePlaywright.attachScreenshot("Offenes Ticket mit wiederherstellbarer URL", browserPage);

    assertThat(ticketPanel.locator("#support-ticket-header"))
        .containsText(uiTexts.getSupportRecoverySubject());
    assertThat(tools.locator("#support-claim")).isVisible();
    assertThat(tools.locator("#support-claim")).hasAttribute("theme", "primary");
    assertThat(ticketPanel.locator(".support-admin-ticket-metadata > .support-ticket-status"))
        .hasCSS("margin-inline-start", "0px");
    assertThat(footer).not().isVisible();
    assertThat(ticketPanel.locator("#support-ticket-status-note")).isVisible();
    assertThat(ticketPanel.locator(".support-admin-policy-label"))
        .hasText(uiTexts.getSupportSecurityPolicy());
    assertThat(tools.locator("vaadin-switch.cosmos-config-switch")).isVisible();
    for (int width : new int[] {1280, 390}) {
      browserPage.setViewportSize(width, 500);
      Locator queueScroll = browserPage.locator(".support-admin-ticket-scroll");
      queueScroll.evaluate("scroller => scroller.scrollTop = scroller.scrollHeight");
      browserPage.waitForCondition(() ->
          ((Number) queueScroll.evaluate("scroller => scroller.scrollTop")).doubleValue() > 0);
      double scrollPosition =
          ((Number) queueScroll.evaluate("scroller => scroller.scrollTop")).doubleValue();
      Locator focus = ticketPanel.locator("#support-focus-toggle");
      UiActions.complete(browserPage, focus::click);
      UiActions.complete(browserPage, focus::click);
      browserPage.waitForCondition(() ->
          Math.abs(
                  ((Number) queueScroll.evaluate("scroller => scroller.scrollTop")).doubleValue()
                      - scrollPosition)
              < 2);
      browserPage.setViewportSize(width, 1000);
      UiActions.complete(browserPage, focus::click);

      assertThat(browserPage.locator(".support-admin-queue")).not().isVisible();
      assertThat(focus).hasAttribute("aria-label", uiTexts.getSupportRestoreSplitView());
      assertThat(focus.locator("[data-lucide=minimize]")).isVisible();
      assertTicketLocation("all", openNumber);
      browserPage.waitForCondition(() ->
          (Boolean)
              ticketPanel.evaluate(
                  """
                  panel => {
                    const detail = panel.getBoundingClientRect();
                    const workspace = document.querySelector('.support-admin-columns');
                    const bounds = workspace.getBoundingClientRect();
                    const style = getComputedStyle(workspace);
                    return Math.abs(detail.width - bounds.width + parseFloat(style.paddingLeft)
                        + parseFloat(style.paddingRight)) < 2;
                  }
                  """));
      ticketPanel.scrollIntoViewIfNeeded();
      AllurePlaywright.attachScreenshot("Ticket-Fokusansicht bei " + width + "px", browserPage);
      UiActions.complete(browserPage, focus::click);

      assertThat(browserPage.locator(".support-admin-queue")).isVisible();
      assertThat(focus.locator("[data-lucide=expand]")).isVisible();
      assertThat(
              browserPage
                  .locator(
                      ".support-admin-ticket-list vaadin-grid-cell-content .support-ticket-number")
                  .first())
          .hasText("#" + openNumber);
    }
  }

  private String verifyTicketSelectionAfterReload(Locator categories, Locator ticketPanel) {
    browserPage.setViewportSize(1280, 1000);
    String openNumber =
        browserPage
            .locator(".support-admin-ticket-card .support-ticket-number")
            .first()
            .innerText()
            .substring(1);
    UiActions.complete(
        browserPage, () -> browserPage.locator(".support-admin-ticket-card").first().click());
    assertTicketLocation("recovery", openNumber);
    browserPage.reload();

    assertThat(ticketPanel.locator(".support-ticket-number"))
        .hasText(uiTexts.getSupportTicketNumber("#" + openNumber));
    assertThat(categories.locator("vaadin-tab[selected]"))
        .hasText(uiTexts.getSupportRecoveryCategory());
    UiActions.complete(browserPage, () -> categories.locator("vaadin-tab").first().click());
    assertTicketLocation("all", openNumber);
    browserPage.reload();

    assertThat(categories.locator("vaadin-tab[selected]")).hasText(uiTexts.getSupportAllTickets());
    assertThat(ticketPanel.locator(".support-ticket-number"))
        .hasText(uiTexts.getSupportTicketNumber("#" + openNumber));
    String closedNumber =
        browserPage
            .locator(".support-admin-ticket-card .support-ticket-number")
            .nth(1)
            .innerText()
            .substring(1);
    UiActions.complete(
        browserPage, () -> browserPage.locator(".support-admin-ticket-card").nth(1).click());
    assertTicketLocation("all", closedNumber);
    browserPage.reload();

    assertThat(ticketPanel.locator(".support-ticket-number"))
        .hasText(uiTexts.getSupportTicketNumber("#" + closedNumber));
    UiActions.complete(
        browserPage, () -> browserPage.locator(".support-admin-ticket-card").first().click());
    assertTicketLocation("all", openNumber);
    return openNumber;
  }

  private void assertTicketLocation(String tab, String number) {
    browserPage.waitForCondition(() ->
        (Boolean)
            browserPage.evaluate(
                """
                expected => {
                  const parameters = new URL(window.location.href).searchParams;
                  return parameters.get('section') === 'support'
                      && parameters.get('tab') === expected.tab
                      && (parameters.get('ticket') ?? '') === expected.number;
                }
                """,
                Map.of("tab", tab, "number", number == null ? "" : number)));
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @Tag("setup-screen")
  @DisplayName(
      "MFA-17: Verpflichtender Einstieg A nutzt die frische Passwortanmeldung ohne zweite Abfrage")
  void requiredIntroductionReusesFreshPasswordWithoutGrantingPortalAccess(int width) {
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    String email = "intro-mfa-" + UUID.randomUUID() + "@example.test";
    new AdminUsersPage(browserPage)
        .createUser(email, BrowserTestAccounts.USER_PASSWORD, true, true);
    try (BrowserActor actor = new BrowserActor(testBrowser)) {
      actor.browserPage.setViewportSize(width, 900);
      LoginPage fresh = new LoginPage(actor.browserPage);
      fresh.open();
      fresh.signInFirstFactor(email, BrowserTestAccounts.USER_PASSWORD);
      new UserMenu(actor.browserPage).selectTestLanguage();

      assertThat(actor.browserPage.locator("#two-factor-introduction-title"))
          .hasText(uiTexts.getTwoFactorRequiredTitle());
      assertThat(actor.browserPage.locator("#two-factor-password")).hasCount(0);
      assertThat(actor.browserPage.locator("#two-factor-submit"))
          .hasText(uiTexts.getTwoFactorBeginSetup());
      assertThat(actor.browserPage.locator("#two-factor-cancel"))
          .hasText(uiTexts.getBaseLogoutButton());
      assertTrue(
          (Boolean)
              actor
                  .browserPage
                  .locator(".two-factor-introduction-art")
                  .evaluate("image => image.complete && image.naturalWidth > 0"));
      assertTrue(
          (Boolean)
              actor.browserPage.evaluate(
                  "document.documentElement.scrollWidth <= window.innerWidth"));
      AllurePlaywright.attachScreenshot(
          "Vaadin: verpflichtender Einstieg A bei " + width + "px", actor.browserPage);
      actor.browserPage.locator("#two-factor-cancel").click();

      assertThat(actor.browserPage).hasURL(Pattern.compile(".*/login$"));
      actor.browserPage.navigate("/home");

      assertThat(actor.browserPage.locator(".public-header")).isVisible();
      actor.browserPage.navigate("/account");

      assertThat(actor.browserPage).hasURL(Pattern.compile(".*/login.*"));
      new UserMenu(actor.browserPage).selectTestLanguage();
      fresh.assertFormVisible();
      fresh.signInFirstFactor(email, BrowserTestAccounts.USER_PASSWORD);
      actor.browserPage.reload();

      assertThat(actor.browserPage.locator("#two-factor-introduction-title")).isVisible();
      actor.browserPage.navigate("/admin");

      assertThat(actor.browserPage).hasURL(Pattern.compile(".*/two-factor.*"));
      actor.browserPage.locator("#two-factor-submit").click();

      assertThat(actor.browserPage.locator("#two-factor-qr")).isVisible();
      assertThat(actor.browserPage.locator("#two-factor-password")).hasCount(0);
      assertEnrollmentControls(actor.browserPage);
      TwoFactorPage.enrollCurrent(actor.browserPage, email);

      assertThat(actor.browserPage).hasURL(Pattern.compile(".*/admin$"));
    }
  }

  private void assertEnrollmentControls(Page enrollment) {
    assertThat(enrollment.locator(".two-factor-method-tabs")).hasCSS("height", "42px");
    assertThat(enrollment.locator(".two-factor-app-links a")).hasCount(3);
    assertThat(enrollment.locator(".two-factor-app-compatibility"))
        .hasText(uiTexts.getTwoFactorAppCompatibility());
    assertThat(enrollment.locator("#two-factor-qr")).hasJSProperty("complete", true);
    assertTrue(
        (Boolean)
            enrollment
                .locator("#two-factor-qr-pane")
                .evaluate(
                    """
                    pane => {
                      const image = pane.querySelector('img');
                      const bounds = pane.getBoundingClientRect();
                      const qr = image.getBoundingClientRect();
                      return image.complete && image.naturalWidth > 0
                        && Math.abs(qr.x + qr.width / 2 - bounds.x - bounds.width / 2) <= 1;
                    }
                    """));
    enrollment.locator("#two-factor-manual-tab").click();
    Locator manual = enrollment.locator("#two-factor-manual-key");
    Locator manualInput = manual.locator("textarea");
    // Genau zwei Zeilen aus Base32-Zeichen und Gruppierungsleerzeichen zulassen.
    assertThat(manualInput).hasValue(Pattern.compile("[A-Z2-7 ]+\\n[A-Z2-7 ]+"));
    assertThat(manual).hasJSProperty("value", "");
    assertThat(manualInput).hasAttribute("readonly", "");
    String formattedKey = manualInput.inputValue();
    manualInput.focus();

    assertThat(manual).hasJSProperty("value", "");
    assertThat(manual.locator("#two-factor-copy-key")).hasCSS("color", "rgb(56, 189, 248)");
    enrollment.context().grantPermissions(java.util.List.of("clipboard-read", "clipboard-write"));
    manual.locator("#two-factor-copy-key").click();

    assertThat(
            enrollment
                .locator("vaadin-notification-card:not([closing])")
                .filter(new Locator.FilterOptions().setHasText(uiTexts.getTwoFactorManualCopied())))
        .isVisible();
    org.junit.jupiter.api.Assertions.assertEquals(
        formattedKey.replace(" ", "").replace("\n", ""),
        enrollment.evaluate("navigator.clipboard.readText()"));
    TwoFactorPage.assertCopyFailure(
        enrollment, manual.locator("#two-factor-copy-key"), uiTexts.getTwoFactorManualCopyFailed());

    assertThat(manualInput).hasValue(formattedKey);
    assertThat(manual).hasJSProperty("value", "");
    UiTexts other = new UiTexts(uiTexts.language().equals("de") ? "en" : "de");
    new UserMenu(enrollment).selectLanguage(other.language());

    assertThat(enrollment.locator("#two-factor-manual-tab")).hasAttribute("selected", "");
    assertThat(manual).hasJSProperty("label", other.getTwoFactorManualLabel());
    assertThat(manualInput).hasValue(formattedKey);
    assertThat(manual).hasJSProperty("value", "");
    new UserMenu(enrollment).selectTestLanguage();

    assertThat(manualInput).hasValue(formattedKey);
    AllurePlaywright.attachScreenshot(
        "Manueller Einrichtungsschlüssel nach Sprachwechsel", enrollment);
    enrollment.locator("#two-factor-qr-tab").click();

    assertThat(enrollment.locator("#two-factor-qr")).isVisible();
    assertThat(enrollment.locator("#two-factor-qr")).hasJSProperty("complete", true);
    AllurePlaywright.attachScreenshot("QR-Einrichtung mit nativer OTP-Eingabe", enrollment);
    assertOtpInput(enrollment);
    AllurePlaywright.attachScreenshot("Fehlermarkierung bei leerem Authenticator-Code", enrollment);
  }

  private void assertOtpInput(Page enrollment) {
    Locator otp = enrollment.locator("#two-factor-enrollment-code");
    Locator slots = otp.locator("[part=slot]");
    Locator input = otp.locator("input");

    assertThat(slots).hasCount(6);
    assertThat(slots).hasText(new String[] {"•", "•", "•", "•", "•", "•"});
    assertThat(otp.locator("[part=slot][active]:not([filled])")).hasCount(1);
    assertThat(input).not().isFocused();
    input.pressSequentially("12");

    assertThat(otp.locator("[part=slot][filled]")).hasCount(2);
    assertThat(slots.nth(2)).hasAttribute("active", "");
    input.press("Backspace");

    assertThat(input).hasValue("1");
    assertThat(slots.nth(1)).hasAttribute("active", "");
    input.press("ArrowLeft");

    assertThat(slots.first()).hasAttribute("active", "");
    input.press("ArrowRight");

    assertThat(slots.nth(1)).hasAttribute("active", "");
    // Auch eingefügte, vom Add-on auf sechs Ziffern begrenzte Codes werden sofort geprüft.
    for (String copiedCode : new String[] {"1234567", "123 456 7"}) {
      enrollment.evaluate("value => navigator.clipboard.writeText(value)", copiedCode);
      input.press("ControlOrMeta+A");
      input.press("ControlOrMeta+V");

      assertThat(otp).hasAttribute("invalid", "");
      assertThat(input).hasValue("");
      assertThat(
              enrollment
                  .locator("vaadin-notification-card:not([closing])")
                  .filter(
                      new Locator.FilterOptions().setHasText(uiTexts.getTwoFactorInvalidCode())))
          .isVisible();
      assertThat(input).hasCSS("color", "rgba(0, 0, 0, 0)");
      assertTrue(
          (Boolean)
              enrollment.evaluate("document.documentElement.scrollWidth <= window.innerWidth"));
    }
    input.fill("");
    enrollment.locator("#two-factor-submit").click();

    assertThat(otp).hasAttribute("invalid", "");
    assertThat(
            enrollment
                .locator("vaadin-notification-card:not([closing])")
                .filter(
                    new Locator.FilterOptions()
                        .setHasText(uiTexts.getBaseValidationRequiredMessage())))
        .isVisible();
    assertThat(enrollment.locator("#two-factor-qr")).isVisible();
  }

  @ParameterizedTest
  @CsvSource({
    "password,1280",
    "password,390",
    "google,1280",
    "google,390",
    "microsoft,1280",
    "microsoft,390"
  })
  @DisplayName(
      "MFA-12: Letzter Ersatzcode erlaubt nur den gezielten Wechsel vor neuem Kontozugriff")
  void lastRecoveryCodeReplacesTheLostAuthenticatorWithoutGeneralAccess(
      String provider, int width) {
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    String email = "recover-mfa-" + UUID.randomUUID() + "@example.test";
    new AdminUsersPage(browserPage).createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
    new UserMenu(browserPage).signOut();
    browserPage.setViewportSize(width, 900);
    login.signIn(email, BrowserTestAccounts.USER_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    browserPage.navigate("/account?section=security&tab=portal");
    browserPage.locator("#account-two-factor-setup").click();
    browserPage.locator("#two-factor-password input").fill(BrowserTestAccounts.USER_PASSWORD);
    browserPage.locator("#two-factor-submit").click();
    TwoFactorPage.enrollCurrent(browserPage, email);

    assertThat(browserPage).hasURL(Pattern.compile(".*/account\\?section=security&tab=portal$"));
    new UserMenu(browserPage).selectTestLanguage();
    if (!provider.equals("password")) {
      browserPage.locator("#account-security-tab-providers").click();
      browserPage.locator("#external-connect-" + provider).click();
      browserPage
          .locator("#external-confirm-factor input")
          .pressSequentially(TwoFactorPage.totp(browserPage, email));
      providerIdentity(email);

      assertThat(browserPage.locator("#external-connect-" + provider))
          .hasText(
              uiTexts.getExternalDisconnect(provider.equals("google") ? "Google" : "Microsoft"));
      browserPage.navigate("/account?section=security&tab=portal");
    }
    // Echte Bestätigungen ohne Kontoänderung verbrauchen alle bis auf den letzten ausgegebenen
    // Code.
    int confirmations = TwoFactorPage.remainingRecoveryCodes(email) - 1;
    for (int index = 0; index < confirmations; index++) {
      // Unterschiedliche Zwecke erfordern jeweils einen neuen Nachweis statt einer offenen
      // Freigabe.
      String action = index % 2 == 0 ? "password" : "email";
      browserPage.locator("#account-" + action + "-edit-security").click();
      browserPage.locator("#external-proof-recovery-code-tab").click();
      browserPage
          .locator("#external-confirm-recovery-code input")
          .fill(TwoFactorPage.nextCode(browserPage, email));
      browserPage.locator("#external-confirm-local").click();

      assertThat(browserPage.locator("#account-" + action + "-new")).isVisible();
      browserPage.locator("#account-" + action + "-cancel").click();

      assertThat(browserPage.locator("#account-" + action + "-new")).isHidden();
    }
    String last = TwoFactorPage.nextCode(browserPage, email);
    new UserMenu(browserPage).requestSignOut();

    assertThat(browserPage.locator(".public-header")).isVisible();
    signInUntilSecondFactor(email, provider);
    TwoFactorPage.openLoginHelp(browserPage);
    browserPage.locator("#two-factor-recover").click();

    assertThat(browserPage.locator("#two-factor-panel .cosmos-panel-header"))
        .containsText(uiTexts.getTwoFactorRecoverSubtitle());
    TwoFactorPage.enterCode(browserPage, last);
    browserPage.locator("#two-factor-submit").click();

    assertThat(browserPage.locator("#two-factor-qr")).isVisible();
    try (Page blocked = browserContext.newPage()) {
      blocked.navigate("/account?tab=profile");

      assertThat(blocked).hasURL(Pattern.compile(".*/two-factor.*"));
      assertThat(blocked.locator("#two-factor-qr")).isVisible();
      assertThat(blocked.locator("#account-panel-profile")).hasCount(0);
    }
    browserPage.reload();
    TwoFactorPage.enrollCurrent(browserPage, email);

    assertThat(browserPage).hasURL(Pattern.compile(".*/account\\?section=security&tab=portal$"));
    assertThat(browserPage.locator("#account-two-factor"))
        .containsText(uiTexts.getTwoFactorEnrolled());
    AllurePlaywright.attachScreenshot(
        "Wiederhergestellter Authenticator mit neuem Codesatz", browserPage);
    new UserMenu(browserPage).requestSignOut();

    assertThat(browserPage.locator(".public-header")).isVisible();
    signInUntilSecondFactor(email, provider);
    TwoFactorPage.enterCode(browserPage, last);
    browserPage.locator("#two-factor-submit").click();

    assertThat(browserPage.locator("#two-factor-code")).hasAttribute("invalid", "");
    assertThat(browserPage.locator("vaadin-notification-card:not([closing])"))
        .containsText(uiTexts.getTwoFactorInvalidCode());
    String nextCode = TwoFactorPage.nextCode(browserPage, email);
    TwoFactorPage.enterCode(browserPage, nextCode);
    if (nextCode.length() > 6) {
      browserPage.locator("#two-factor-submit").click();
    }
    assertThat(browserPage).hasURL(Pattern.compile(".*/home$"));
  }

  private void signInUntilSecondFactor(String email, String provider) {
    LoginPage login = new LoginPage(browserPage);
    login.open();
    if (provider.equals("password")) {
      login.signInFirstFactor(email, BrowserTestAccounts.USER_PASSWORD);
    } else {
      browserPage.locator("#external-login-" + provider).click();
      providerIdentity(email);
    }
    assertThat(browserPage).hasURL(Pattern.compile(".*/two-factor.*"));
    TwoFactorPage.selectTestLanguageIfNeeded(browserPage);
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName(
      "MFA-11: Freiwilliger Authenticator wird nur mit eigenem Faktor gewechselt und entfernt")
  @Tag("follow-up-regression")
  void ownAuthenticatorCanBeReplacedAndRemovedWithIndependentProof(int width) {
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    String email = "manage-mfa-" + UUID.randomUUID() + "@example.test";
    new AdminUsersPage(browserPage).createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
    try (BrowserActor user = new BrowserActor(testBrowser)) {
      user.browserPage.setViewportSize(width, 900);
      user.login(email, BrowserTestAccounts.USER_PASSWORD);
      user.browserPage.navigate("/account?section=security&tab=portal");
      user.browserPage.locator("#account-two-factor-setup").click();
      user.browserPage
          .locator("#two-factor-password input")
          .fill(BrowserTestAccounts.USER_PASSWORD);
      user.browserPage.locator("#two-factor-submit").click();
      TwoFactorPage.enrollCurrent(user.browserPage, email);

      assertThat(user.browserPage)
          .hasURL(Pattern.compile(".*/account\\?section=security&tab=portal$"));
      new UserMenu(user.browserPage).selectTestLanguage();
      user.browserPage.locator("#account-two-factor-replace").click();
      Locator actionOtp = user.browserPage.locator("#two-factor-action-factor");

      assertThat(actionOtp.locator("input[slot=input]")).isFocused();
      assertThat(actionOtp.locator("[part=slot]").first())
          .hasCSS("font-family", Pattern.compile(".*Space Mono.*"));
      TwoFactorPage.enterActionCode(
          user.browserPage, TwoFactorPage.nextCode(user.browserPage, email));

      assertThat(user.browserPage.locator("#two-factor-action-recovery-code input"))
          .hasCSS("font-family", Pattern.compile(".*Space Mono.*"));
      user.browserPage.locator("#two-factor-action-recovery-code input").press("Tab");

      assertThat(user.browserPage.locator("#two-factor-action-confirm")).isVisible();
      assertThat(user.browserPage)
          .hasURL(Pattern.compile(".*/account\\?section=security&tab=portal$"));
      user.browserPage.locator("#two-factor-action-confirm").click();
      TwoFactorPage.enrollCurrent(user.browserPage, email);

      assertThat(user.browserPage)
          .hasURL(Pattern.compile(".*/account\\?section=security&tab=portal$"));
      user.browserPage.locator("#account-two-factor-remove").click();
      TwoFactorPage.enterActionCode(
          user.browserPage, TwoFactorPage.nextCode(user.browserPage, email));
      user.browserPage.locator("#two-factor-action-confirm").click();

      assertThat(user.browserPage.locator("#account-two-factor-setup")).isVisible();
      assertThat(user.browserPage.locator("#account-two-factor vaadin-badge"))
          .hasText(uiTexts.getTwoFactorNotEnrolled());
      assertThat(user.browserPage.locator("vaadin-notification-card:not([closing])"))
          .containsText(uiTexts.getTwoFactorDisabled());
      AllurePlaywright.attachScreenshot(
          "Freiwilliger Faktor nach geschütztem Wechsel und Entfernen", user.browserPage);
      new UserMenu(user.browserPage).signOut();
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName(
      "MFA-09: Neue Pflichtregel schützt ein bereits angemeldetes Konto und bleibt konfigurierbar")
  void policyChangesProtectAnExistingSession(int width) {
    browserPage.setViewportSize(width, 900);
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    String email = "policy-mfa-" + UUID.randomUUID() + "@example.test";
    new AdminUsersPage(browserPage).createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
    try (BrowserActor user = new BrowserActor(testBrowser)) {
      user.login(email, BrowserTestAccounts.USER_PASSWORD);
      try {
        changeUserLoginRule(uiTexts.getTwoFactorModeRequired());
        user.browserPage.navigate("/account?tab=profile");

        assertThat(user.browserPage).hasURL(Pattern.compile(".*/two-factor.*"));
        assertThat(user.browserPage.locator("#account-first-name")).hasCount(0);
        TwoFactorPage.completeIfRequired(
            user.browserPage, email, BrowserTestAccounts.USER_PASSWORD);
        user.browserPage.navigate("/account?section=security&tab=portal");

        assertThat(user.browserPage.locator("#account-two-factor")).isVisible();
        AllurePlaywright.attachScreenshot(
            "Bestehende Sitzung nach neu vorgeschriebener Einrichtung", user.browserPage);
      } finally {
        changeUserLoginRule(uiTexts.getTwoFactorModeEnrolled());
        user.browserPage.navigate("/home");
        TwoFactorPage.completeIfRequired(
            user.browserPage, email, BrowserTestAccounts.USER_PASSWORD);
        new UserMenu(user.browserPage).requestSignOut();

        assertThat(user.browserPage.locator(".public-header")).isVisible();
      }
    }
  }

  private void changeUserLoginRule(String label) {
    browserPage.navigate("/admin?section=configuration&group=two_factor");
    TwoFactorPage.completeIfRequired(
        browserPage, BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    browserPage.navigate("/admin?section=configuration&group=two_factor");
    new UserMenu(browserPage).selectTestLanguage();
    browserPage
        .locator("#two-factor-role-tabs vaadin-tab")
        .getByText(uiTexts.getRoleUser(), new Locator.GetByTextOptions().setExact(true))
        .click();
    browserPage.locator("#two-factor-rule-role_user-login [part~='toggle-button']").click();
    browserPage
        .getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(label).setExact(true))
        .click();
    browserPage.locator("#two-factor-save").click();
    TwoFactorPage.enterActionCode(
        browserPage, TwoFactorPage.nextCode(browserPage, BrowserTestAccounts.SUPERADMIN_EMAIL));
    UiActions.complete(
        browserPage, () -> browserPage.locator("#two-factor-action-confirm").click());
    browserPage.waitForCondition(() ->
        java.net.URI.create(browserPage.url()).getPath().equals("/two-factor")
            || browserPage
                .locator("vaadin-notification-card:not([closing])")
                .filter(new Locator.FilterOptions().setHasText(uiTexts.getTwoFactorSaved()))
                .isVisible());
    // Die alte Ansicht darf parallel bereits den neuen Regelstand erkennen und weiterleiten.
    // Ein neuer Tab prüft die neue Anforderung ohne konkurrierende Navigation dieser Ansicht.
    try (Page fresh = browserContext.newPage()) {
      fresh.navigate("/admin?section=configuration&group=two_factor");
      TwoFactorPage.completeIfRequired(
          fresh, BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
      fresh.navigate("/admin?section=configuration&group=two_factor");

      assertThat(fresh.locator("#two-factor-save")).isVisible();
    }
  }

  @Test
  @DisplayName(
      "MFA-10: Echter Anwendungsneustart erhält den vorhandenen Faktorstatus derselben Sitzung")
  void serverRestartDoesNotInventOrDiscardSecondFactorFreshness() throws Exception {
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    browserPage.navigate("/account?section=security&tab=portal");
    try (Page second = browserContext.newPage()) {
      second.navigate("/account?section=security&tab=portal");

      assertThat(second.locator("#account-two-factor")).isVisible();
      BrowserTestRunner.restartTestApplication();
      browserPage.reload();
      second.reload();

      assertThat(browserPage).hasURL(Pattern.compile(".*/account\\?section=security&tab=portal$"));
      assertThat(second).hasURL(Pattern.compile(".*/account\\?section=security&tab=portal$"));
      assertThat(browserPage.locator("#account-two-factor")).isVisible();
      assertThat(second.locator("#account-two-factor")).isVisible();
      assertThat(browserPage.locator("#two-factor-code")).hasCount(0);
    }
  }

  @Test
  @DisplayName("MFA-08: Echter Browserneustart mit Remember-me verlangt einen neuen zweiten Faktor")
  void rememberedFirstFactorDoesNotRestoreThePreviousSecondFactor() {
    LoginPage administrator = new LoginPage(browserPage);
    administrator.open();
    administrator.signIn(
        BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    String email = "remember-mfa-" + UUID.randomUUID() + "@example.test";
    new AdminUsersPage(browserPage).createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
    try (PersistentBrowserActor actor = new PersistentBrowserActor(playwrightRuntime, profile)) {
      LoginPage login = new LoginPage(actor.browserPage);
      login.open();
      actor
          .browserPage
          .getByRole(
              AriaRole.SWITCH,
              new Page.GetByRoleOptions()
                  .setName(uiTexts.getLoginRememberMeCheckbox())
                  .setExact(true))
          .setChecked(true);
      login.signIn(email, BrowserTestAccounts.USER_PASSWORD);
      actor.browserPage.navigate("/account?section=security&tab=portal");
      actor.browserPage.locator("#account-two-factor-setup").click();
      actor
          .browserPage
          .locator("#two-factor-password input")
          .fill(BrowserTestAccounts.USER_PASSWORD);
      actor.browserPage.locator("#two-factor-submit").click();
      TwoFactorPage.enrollCurrent(actor.browserPage, email);

      assertThat(actor.browserPage)
          .hasURL(Pattern.compile(".*/account\\?section=security&tab=portal$"));
      new UserMenu(actor.browserPage).selectTestLanguage();
    }
    try (PersistentBrowserActor actor =
        new PersistentBrowserActor(playwrightRuntime, profile, "two-factor")) {
      assertThat(actor.browserPage.locator("#two-factor-code")).isVisible();
      assertThat(actor.browserPage.locator("#admin-sidebar")).hasCount(0);
      TwoFactorPage.completeIfRequired(actor.browserPage, email, BrowserTestAccounts.USER_PASSWORD);
      actor.browserPage.navigate("/account?tab=sessions");
      actor.browserPage.locator("#account-logout-others").click();
      actor.browserPage.locator("#sessions-confirmation-submit").click();
      new UserMenu(actor.browserPage).signOut();
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @Tag("ticket-browser-detail")
  @DisplayName(
      "MFA-07: Admin übernimmt eine eingeschränkte Hilfsanfrage und sendet die geprüfte Anleitung")
  @Tag("recovery-instructions")
  @Tag("follow-up-regression")
  void recoverySupportRemainsContactInsteadOfFactorReset(int width) {
    browserPage.setViewportSize(width, 900);
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    String email = "support-mfa-" + UUID.randomUUID() + "@example.test";
    new AdminUsersPage(browserPage).createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
    new UserMenu(browserPage).signOut();
    login.signIn(email, BrowserTestAccounts.USER_PASSWORD);
    browserPage.navigate("/account?section=security&tab=portal");
    browserPage.locator("#account-two-factor-setup").click();
    browserPage.locator("#two-factor-password input").fill(BrowserTestAccounts.USER_PASSWORD);
    browserPage.locator("#two-factor-submit").click();
    TwoFactorPage.enrollCurrent(browserPage, email);

    assertThat(browserPage).hasURL(Pattern.compile(".*/account\\?section=security&tab=portal$"));
    new UserMenu(browserPage).selectTestLanguage();
    new UserMenu(browserPage).signOut();
    login.signInFirstFactor(email, BrowserTestAccounts.USER_PASSWORD);
    TwoFactorPage.openLoginHelp(browserPage);
    browserPage.locator("#two-factor-help").click();

    assertThat(browserPage.locator("#account-support")).isVisible();
    browserPage
        .locator("#support-reply textarea")
        .fill("Synthetische Bitte um Hilfe mit meinen Ersatzcodes.");
    UiActions.complete(browserPage, () -> browserPage.locator("#support-create").click());

    assertThat(browserPage.locator("#support-request-count"))
        .hasText(uiTexts.getSupportRequestCount(1));
    String ticketNumber =
        browserPage
            .locator("#support-ticket-grid")
            .getByText(Pattern.compile("^#[0-9]+$"))
            .first()
            .innerText();
    try (BrowserActor administrator = new BrowserActor(testBrowser)) {
      administrator.login(
          BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
      Page admin = administrator.browserPage;
      new UserMenu(admin).selectTestLanguage();
      admin.setViewportSize(width, 900);
      admin.navigate("/admin?section=support");
      Locator ticketCategories = admin.locator("#support-ticket-categories");

      assertThat(ticketCategories).containsText(uiTexts.getSupportAllTickets());
      UiActions.complete(
          admin, () -> ticketCategories.getByText(uiTexts.getSupportRecoveryCategory()).click());
      Locator ticket =
          admin
              .locator("#support-ticket-grid")
              .getByText(ticketNumber, new Locator.GetByTextOptions().setExact(true));
      UiActions.complete(admin, ticket::click);

      assertThat(admin.locator("#admin-support .support-message[data-side=other]"))
          .containsText("Synthetische Bitte um Hilfe mit meinen Ersatzcodes.");
      UiActions.complete(admin, () -> admin.locator("#support-claim").click());
      String assigneeName =
          admin.locator("#support-detail-assignee .cosmos-user-avatar").getAttribute("aria-label");

      assertThat(browserPage.locator("#support-ticket-grid"))
          .containsText(uiTexts.getSupportInProgress());
      assertThat(admin.locator("#admin-support .cosmos-security-warning")).isVisible();
      assertThat(admin.locator("#support-add-reply")).isVisible();
      verifyPublicAndInternalRecoveryReplies(admin, assigneeName);
      verifyRecoveryInstructions(administrator, email, width);
      browserPage
          .locator("#support-reply textarea")
          .fill("Dieser Entwurf gehört nur zur alten Anfrage.");
      UiActions.complete(admin, () -> admin.locator("#support-close").click());

      assertThat(browserPage.locator("#support-ticket-grid"))
          .containsText(uiTexts.getSupportClosed());
      assertThat(browserPage.locator("#support-create")).isVisible();
      assertThat(browserPage.locator("#support-reply textarea")).hasValue("");
      UiActions.complete(browserPage, () -> browserPage.locator("#support-reload").click());

      assertThat(browserPage.locator("vaadin-notification-card:not([closing])"))
          .containsText(uiTexts.getSupportRefreshed());
      browserPage.locator("#support-reply textarea").fill("Eine zweite isolierte Anfrage.");
      UiActions.complete(browserPage, () -> browserPage.locator("#support-create").click());

      assertThat(browserPage.locator("#support-request-count"))
          .hasText(uiTexts.getSupportRequestCount(2));
      UiActions.complete(browserPage, () -> browserPage.locator("#support-close").click());
      new UserMenu(admin).signOut();
    }
    browserPage.navigate("/admin");

    assertThat(browserPage).hasURL(Pattern.compile(".*/recovery-help$"));
    browserPage.locator("#support-sign-out").click();
    new LoginPage(browserPage).assertFormVisible();
    login.signIn(email, BrowserTestAccounts.USER_PASSWORD);
    browserPage.navigate("/admin?section=support&ticket=" + ticketNumber.substring(1));

    assertThat(browserPage.locator("#support-ticket-panel, #admin-nav-support")).hasCount(0);
  }

  private void verifyPublicAndInternalRecoveryReplies(Page admin, String assigneeName) {
    String draft = "Eine Antwort bleibt beim Aktualisieren erhalten.";
    String followUp = "Eine synthetische Ergänzung zur Anfrage.";
    String privateNote = "Synthetischer interner Vermerk.";
    String publicReply = "Eine sichtbare Rückmeldung aus der Administration.";
    Locator adminComposer = admin.locator("#support-reply textarea");
    adminComposer.fill(draft);
    browserPage.locator("#support-reply textarea").fill(followUp);
    UiActions.complete(browserPage, () -> browserPage.locator("#support-add-reply").click());

    assertThat(admin.locator(".support-message-thread")).containsText(followUp);
    assertThat(adminComposer).hasValue(draft);
    adminComposer.fill(privateNote);
    admin.locator("#support-internal-note").click();
    Locator requesterThread = browserPage.locator("#account-support .support-message-thread");
    String requesterCountBeforeNote = browserPage.locator("#support-request-count").innerText();
    Locator requesterMessageCount =
        browserPage
            .locator("#account-support .support-section-count")
            .filter(new Locator.FilterOptions().setHasText(uiTexts.getSupportMessageCount(2)));

    assertThat(requesterMessageCount).isVisible();
    UiActions.complete(admin, () -> admin.locator("#support-add-reply").click());

    assertThat(admin.locator(".support-internal-message")).containsText(privateNote);
    adminComposer.fill(publicReply);
    UiActions.complete(admin, () -> admin.locator("#support-add-reply").click());

    assertThat(browserPage.locator(".support-message-thread")).containsText(publicReply);
    assertThat(browserPage.locator(".support-message-thread")).not().containsText(privateNote);
    assertThat(browserPage.locator("#support-request-count")).hasText(requesterCountBeforeNote);
    assertThat(
            browserPage.getByText(
                uiTexts.getSupportMessageCount(3), new Page.GetByTextOptions().setExact(true)))
        .isVisible();
    assertThat(requesterThread).not().containsText(BrowserTestAccounts.SUPERADMIN_EMAIL);
    assertThat(requesterThread).not().containsText(assigneeName);
    assertThat(browserPage.locator("#account-support .support-message-sender"))
        .hasText(uiTexts.getSupportMessageSupport());
    AllurePlaywright.attachScreenshot("Admin-Tickets mit ausgewählter Anfrage", admin);
  }

  private void verifyRecoveryInstructions(BrowserActor administrator, String email, int width) {
    Page admin = administrator.browserPage;
    admin.locator("#support-instructions").click();

    assertThat(admin.locator("#support-instruction-preview")).hasJSProperty("opened", true);
    assertThat(admin.locator("#support-instruction-preview"))
        .containsText(uiTexts.getSupportRecoveryInstructions().lines().findFirst().orElseThrow());
    assertThat(admin.locator("#support-instruction-preview .support-instruction-step")).hasCount(4);
    assertThat(admin.locator("#support-instruction-preview .cosmos-security-warning")).isVisible();
    admin.evaluate(
        "async () => { await Promise.all(document.getAnimations().filter(animation => animation.effect?.getTiming().iterations !== Infinity).map(animation => animation.finished.catch(() => {}))); }");
    if (width == 390) {
      admin.setViewportSize(width, 1200);
    }
    AllurePlaywright.attachScreenshot("Geprüfte Hilfeantwort ohne neue Zugangscodes", admin);
    admin.setViewportSize(width, 900);
    admin.locator("#support-instruction-preview-submit").click();

    assertThat(admin.locator("vaadin-notification-card:not([closing])"))
        .containsText(uiTexts.getSupportQueued());
    new MailpitInbox(administrator.browserContext)
        .awaitSecurityMail(email, uiTexts.getSupportRecoverySubject());
    Locator guide = browserPage.locator(".support-instruction-message");

    assertThat(guide).containsText(uiTexts.getSupportInstructionMessageTitle());
    assertThat(guide.locator(".support-instruction-details")).not().hasAttribute("opened", "");
    guide.evaluate("element => element.scrollIntoView({block: 'nearest'})");
    AllurePlaywright.attachScreenshot("Anleitung kompakt im Nachrichtenverlauf", browserPage);
    UiActions.complete(browserPage, () -> guide.locator(".support-instruction-toggle").click());

    assertThat(guide.locator(".support-instruction-details")).hasAttribute("opened", "");
    assertThat(guide.locator(".support-instruction-step")).hasCount(4);
    assertThat(guide.locator(".cosmos-security-warning")).isVisible();
    if (width == 390) {
      browserPage.setViewportSize(width, 650);
      assertTouchScroll(browserPage, browserPage.locator("html"));
      browserPage.setViewportSize(width, 900);
      Locator thread = browserPage.locator("#account-support .support-message-scroll");
      thread.scrollIntoViewIfNeeded();
      assertTouchScroll(browserPage, thread);
    }
    UiActions.complete(browserPage, () -> browserPage.locator("#support-reload").click());

    assertThat(guide.locator(".support-instruction-details")).hasAttribute("opened", "");
    guide.evaluate("element => element.scrollIntoView({block: 'nearest'})");
    AllurePlaywright.attachScreenshot("Anleitung aufgeklappt im Nachrichtenverlauf", browserPage);
  }

  @ParameterizedTest
  @CsvSource({"google,1280", "google,390", "microsoft,1280", "microsoft,390"})
  @DisplayName(
      "MFA-05: Externes Konto richtet 2FA sicher ein, bestätigt neu und verbindet einen zweiten Anbieter")
  void externalAccountEnrollmentAndIndependentSensitiveProof(String provider, int width) {
    browserPage.setViewportSize(width, 900);
    String identity = "mfa-" + UUID.randomUUID() + "@gmail.com";
    browserPage.navigate("/login");
    browserPage.locator("#external-login-" + provider).click();
    providerIdentity(identity);
    browserPage.locator("#external-register-first input").fill("Browser");
    browserPage.locator("#external-register-last input").fill("Testuser");
    browserPage.locator("#external-register-submit").click();
    if (provider.equals("microsoft")) {
      MailpitInbox inbox = new MailpitInbox(browserContext);
      try (Page activation = inbox.openVerificationLink(inbox.awaitVerificationMail(identity))) {
        assertThat(activation.locator(".public-panel")).isVisible();
      }
      browserPage.navigate("/login");
      browserPage.locator("#external-login-" + provider).click();
      providerIdentity(identity);
    }
    assertThat(browserPage).hasURL(Pattern.compile(".*/home$"));
    browserPage.navigate("/account?section=security&tab=portal");
    browserPage.locator("#account-two-factor-setup").click();
    browserPage.locator("#two-factor-confirm-" + provider).click();
    providerIdentity(identity);
    if (provider.equals("google")) {
      assertThat(browserPage.locator("#two-factor-code")).isVisible();
      browserPage
          .locator("#two-factor-code input")
          .fill(new MailpitInbox(browserContext).awaitAuthenticatorSetupCode(identity));
      browserPage.locator("#two-factor-submit").click();
    }
    TwoFactorPage.enrollCurrent(browserPage, identity);

    assertThat(browserPage).hasURL(Pattern.compile(".*/account\\?section=security&tab=portal$"));
    new UserMenu(browserPage).signOut();
    browserPage.locator("#external-login-" + provider).click();
    providerIdentity(identity);

    assertThat(browserPage).hasURL(Pattern.compile(".*/two-factor$"));
    TwoFactorPage.completeIfRequired(browserPage, identity, null);

    assertThat(browserPage).hasURL(Pattern.compile(".*/home$"));
    browserPage.navigate("/account?section=security&tab=portal");
    new UserMenu(browserPage).selectTestLanguage();
    browserPage.locator("#account-password-edit-security").click();

    assertThat(browserPage.locator("#external-confirm-password")).isHidden();
    assertThat(browserPage.locator("#external-confirm-" + provider)).hasCount(0);
    browserPage.locator("#external-confirm-factor input").fill("123");
    browserPage.locator("#external-confirm-local").click();

    assertThat(browserPage.locator("#external-confirm-factor")).hasJSProperty("invalid", true);
    assertThat(browserPage.locator("vaadin-notification-card:not([closing])"))
        .containsText(uiTexts.getBaseValidationRequiredMessage());
    assertThat(browserPage.locator("vaadin-notification-card:not([closing])"))
        .hasAttribute("theme", Pattern.compile(".*cosmos-reference-error.*"));
    browserPage
        .locator("vaadin-notification-card:not([closing])")
        .getByRole(AriaRole.BUTTON)
        .click();
    browserPage
        .locator("#external-confirm-factor input")
        .fill(TwoFactorPage.totp(browserPage, identity));
    browserPage.locator("#external-confirm-local").click();
    browserPage.locator("#account-password-new input").fill("OwnOptionalPortal1!");
    browserPage.locator("#account-password-confirm input").fill("OwnOptionalPortal1!");
    browserPage.locator("#account-password-save").click();

    assertThat(browserPage.locator("vaadin-notification-card:not([closing])"))
        .containsText(uiTexts.getAccountSecurityPasswordSuccess());
    browserPage.locator("#account-security-tab-providers").click();
    String second = provider.equals("google") ? "microsoft" : "google";
    browserPage.locator("#external-connect-" + second).click();

    assertThat(browserPage.locator("#external-confirm-factor input")).isFocused();
    browserContext.grantPermissions(List.of("clipboard-read", "clipboard-write"));
    pasteOtp(browserPage, TwoFactorPage.totp(browserPage, identity));
    providerIdentity("linked-" + UUID.randomUUID() + "@gmail.com");

    assertThat(browserPage.locator("#external-connect-" + second))
        .hasText(uiTexts.getExternalDisconnect(second.equals("google") ? "Google" : "Microsoft"));
    AllurePlaywright.attachScreenshot(
        "Eigenständige 2FA bei verbundenen Anmeldeanbietern", browserPage);
  }

  private void providerIdentity(String identity) {
    assertThat(browserPage.locator("#fixture-email")).isVisible();
    browserPage.locator("#fixture-email").fill(identity);
    browserPage.locator("#fixture-mode").selectOption("valid");
    browserPage.locator("#fixture-signin").click();
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName(
      "MFA-01: Authenticator einrichten und dieselbe Sitzung bei Refresh und weiterem Tab erhalten")
  void requiredAuthenticatorSetupAndSessionContinuity(int width) {
    browserPage.setViewportSize(width, 900);
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signInFirstFactor(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);

    assertThat(browserPage).hasURL(Pattern.compile(".*/two-factor$"));
    assertThat(browserPage.locator("#two-factor-panel")).isVisible();
    if (browserPage.locator("#two-factor-password").isVisible()) {
      browserPage.locator("#two-factor-password input").fill("WrongSetupPassword!");
      browserPage.locator("#two-factor-submit").click();

      assertThat(browserPage.locator("#two-factor-password")).hasAttribute("invalid", "");
      assertThat(browserPage.locator("vaadin-notification-card:not([closing])"))
          .containsText(uiTexts.getAccountSecurityWrongPassword());
    }
    AllurePlaywright.attachScreenshot("Eingeschränkter Zwei-Faktor-Zugang", browserPage);
    TwoFactorPage.completeIfRequired(
        browserPage, BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);

    assertThat(browserPage).hasURL(Pattern.compile(".*/admin$"));
    browserPage.reload();

    assertThat(browserPage.locator("#admin-sidebar")).isAttached();
    try (Page tab = browserContext.newPage()) {
      tab.navigate("/account?section=security&tab=portal");
      new UserMenu(tab).selectTestLanguage();

      assertThat(tab.locator("#account-two-factor")).containsText(uiTexts.getTwoFactorEnrolled());
      assertThat(tab).hasURL(Pattern.compile(".*/account\\?section=security&tab=portal$"));
    }
    browserPage.navigate("/account?section=security&tab=portal");
    new UserMenu(browserPage).selectTestLanguage();

    assertThat(browserPage.locator("#account-two-factor"))
        .containsText(uiTexts.getTwoFactorEnrolled());
    AllurePlaywright.attachScreenshot("Eigene Zwei-Faktor-Verwaltung", browserPage);
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName("MFA-02: Neue Ersatzcodes nach eigener Bestätigung und geschütztes PDF")
  @Tag("recovery-output")
  void protectedRecoveryCodeIssuance(int width) throws Exception {
    browserPage.setViewportSize(width, 900);
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    browserPage.navigate("/account?section=security&tab=portal");
    browserPage.locator("#account-two-factor-renew").click();
    assertAccountActionCodeLayout();
    browserPage.locator("#two-factor-action-help").click();

    assertThat(browserPage.locator("#account-support")).isVisible();
    browserPage.locator("#support-back").click();

    assertThat(browserPage.locator("#account-two-factor-renew")).isVisible();
    assertThat(browserPage.locator("#two-factor-action-confirm")).isHidden();
    browserPage.locator("#account-two-factor-renew").click();
    TwoFactorPage.enterActionCode(
        browserPage, TwoFactorPage.nextCode(browserPage, BrowserTestAccounts.ADMIN_EMAIL));
    try (Page previous = browserContext.newPage()) {
      previous.navigate("/account?section=security&tab=portal");
      Locator oldRenew = previous.locator("#account-two-factor-renew");

      assertThat(oldRenew).isVisible();
      browserPage.locator("#two-factor-action-confirm").click();

      assertThat(browserPage.locator("#two-factor-recovery-toggle")).isVisible();
      oldRenew.click();

      assertThat(previous).hasURL(browserPage.url());
      assertThat(previous.locator("#two-factor-recovery-toggle")).isVisible();
    }
    java.time.Instant shortProofExpired = java.time.Instant.now().plusSeconds(301);
    String outputPage = browserPage.url();
    String url = assertRestrictedRecoveryOutput(outputPage);
    TwoFactorPage.captureCodes(browserPage, BrowserTestAccounts.ADMIN_EMAIL);
    java.util.List<String> originalCodes = TwoFactorPage.recoveryCodeValues(browserPage);
    for (int index = 1; index <= 10; index++) {
      assertThat(browserPage.locator("#two-factor-recovery-code-" + index))
          .hasJSProperty("value", "");
      assertThat(browserPage.locator("#two-factor-recovery-code-" + index + " input"))
          .hasAttribute("readonly", "");
    }
    verifyUnavailableRecoveryDownload(url, outputPage, originalCodes);
    com.microsoft.playwright.Download download =
        browserPage.waitForDownload(() -> browserPage.locator("#two-factor-download-pdf").click());
    org.junit.jupiter.api.Assertions.assertNull(download.failure());
    byte[] bytes = java.nio.file.Files.readAllBytes(download.path());

    assertEquals("%PDF-", new String(bytes, 0, 5, java.nio.charset.StandardCharsets.US_ASCII));
    assertThat(browserPage).hasURL(outputPage);
    org.junit.jupiter.api.Assertions.assertEquals(
        originalCodes, TwoFactorPage.recoveryCodeValues(browserPage));
    if (width == 1280) {
      // Die echte Fünf-Minuten-Frist darf nur den kurzen Sicherheitsnachweis beenden,
      // nicht die noch unbestätigte sitzungsgebundene Codeausgabe oder deren PDF.
      browserPage.waitForCondition(
          () -> java.time.Instant.now().isAfter(shortProofExpired),
          new Page.WaitForConditionOptions().setTimeout(310_000));
      browserPage.reload();
      org.junit.jupiter.api.Assertions.assertEquals(
          originalCodes, TwoFactorPage.recoveryCodeValues(browserPage));
      com.microsoft.playwright.Download laterDownload =
          browserPage.waitForDownload(
              () -> browserPage.locator("#two-factor-download-pdf").click());
      org.junit.jupiter.api.Assertions.assertNull(laterDownload.failure());
      byte[] laterBytes = java.nio.file.Files.readAllBytes(laterDownload.path());

      assertEquals(
          "%PDF-", new String(laterBytes, 0, 5, java.nio.charset.StandardCharsets.US_ASCII));
      assertThat(browserPage).hasURL(outputPage);
    }
    AllurePlaywright.attachScreenshot("Einmalige Ausgabe synthetischer Ersatzcodes", browserPage);
    browserPage.locator("#two-factor-codes-saved input").setChecked(true);
    browserPage.locator("#two-factor-submit").click();

    assertThat(browserPage).hasURL(Pattern.compile(".*/account\\?section=security&tab=portal$"));
    org.junit.jupiter.api.Assertions.assertTrue(
        ((Number)
                    browserPage.evaluate(
                        "async url => (await fetch(url, {cache:'no-store'})).status", url))
                .intValue()
            >= 400);
  }

  private String assertRestrictedRecoveryOutput(String outputPage) {
    assertThat(browserPage.locator("#two-factor-recovery-codes")).isVisible();
    browserPage.locator("#two-factor-recovery-toggle").click();

    assertThat(browserPage.locator("#two-factor-recovery-codes")).isHidden();
    assertThat(browserPage.locator("#two-factor-submit")).isDisabled();
    assertThat(browserPage.locator("#two-factor-cancel")).isHidden();
    assertThat(browserPage.locator(".public-header a.public-brand")).hasCount(0);
    assertThat(browserPage.locator("#public-nav-home, #public-nav-login, #public-nav-register"))
        .hasCount(0);
    assertThat(browserPage.locator(".public-languages vaadin-button")).hasCount(2);
    try (Page second = browserContext.newPage()) {
      for (String path : List.of("/home", "/admin", "/account", "/recovery-help")) {
        second.navigate(path);

        assertThat(second).hasURL(outputPage);
        assertThat(second.locator("#two-factor-recovery-toggle")).isVisible();
      }
    }
    assertThat(
            browserPage.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                    .setName(uiTexts.getTwoFactorDownloadPdf())
                    .setExact(true)))
        .isVisible();
    String url = browserPage.locator("#two-factor-download-pdf").getAttribute("data-download-url");
    @SuppressWarnings("unchecked")
    Map<String, Object> pdf =
        (Map<String, Object>)
            browserPage.evaluate(
                "async url => { const response = await fetch(url, {cache:'no-store'}); return {status:response.status, cache:response.headers.get('cache-control'), size:(await response.arrayBuffer()).byteLength}; }",
                url);
    org.junit.jupiter.api.Assertions.assertEquals(200, ((Number) pdf.get("status")).intValue());
    org.junit.jupiter.api.Assertions.assertTrue(((String) pdf.get("cache")).contains("no-store"));
    org.junit.jupiter.api.Assertions.assertTrue(((Number) pdf.get("size")).intValue() > 1000);
    return url;
  }

  private void assertAccountActionCodeLayout() {
    Locator dialog = browserPage.locator("#two-factor-action-dialog");
    Locator appCode = browserPage.locator("#two-factor-action-factor");

    assertThat(appCode.locator("input[slot=input]")).isFocused();
    assertThat(appCode.locator("[part=slot]").first())
        .hasCSS("font-family", Pattern.compile(".*Space Mono.*"));
    Locator header = dialog.locator(".account-change-dialog-header");
    Locator close = header.locator(".cosmos-overlay-close");

    assertThat(close).isVisible();
    BoundingBox headerBounds = header.boundingBox();

    assertTrue(close.boundingBox().x > headerBounds.x + headerBounds.width / 2);
    assertThat(dialog.locator(".two-factor-method-tabs")).hasCSS("height", "42px");
    UiActions.complete(
        browserPage, browserPage.locator("#two-factor-action-recovery-code-tab")::click);

    assertThat(browserPage.locator("#two-factor-action-recovery-code input"))
        .hasCSS("font-family", Pattern.compile(".*Space Mono.*"));
    assertThat(appCode).isHidden();
  }

  private void verifyUnavailableRecoveryDownload(
      String url, String outputPage, List<String> originalCodes) {
    // Ein tatsächlich nicht verfügbarer Codesatz wird vom geschützten Serverendpunkt
    // abgelehnt. Die native Downloadaktion muss auch dabei die sichtbare Ausgabe erhalten.
    String unavailable = "/internal/two-factor/recovery/" + UUID.randomUUID() + "/codes.pdf";
    browserPage
        .locator("#two-factor-download-pdf")
        .evaluate("(button, url) => button.setAttribute('data-download-url', url)", unavailable);
    java.util.List<com.microsoft.playwright.Download> failedDownloads = new java.util.ArrayList<>();
    java.util.function.Consumer<com.microsoft.playwright.Download> captureDownload =
        failedDownloads::add;
    browserPage.onDownload(captureDownload);
    try {
      browserPage.locator("#two-factor-download-pdf").click();

      assertThat(
              browserPage.getByText(
                  uiTexts.getTwoFactorDownloadFailed(), new Page.GetByTextOptions().setExact(true)))
          .isVisible();
      assertTrue(failedDownloads.isEmpty(), "Error responses must not offer a file download");
    } finally {
      browserPage.offDownload(captureDownload);
      browserPage
          .locator("#two-factor-download-pdf")
          .evaluate("(button, url) => button.setAttribute('data-download-url', url)", url);
    }
    assertThat(browserPage).hasURL(outputPage);
    org.junit.jupiter.api.Assertions.assertEquals(
        originalCodes, TwoFactorPage.recoveryCodeValues(browserPage));

    assertThat(browserPage.locator("#two-factor-submit")).isDisabled();
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName(
      "MFA-03: Sicherheitskonfiguration nur für Superadmins mit nativen Rollenregeln und Feldhilfe")
  @Tag("two-factor-configuration")
  @Tag("configuration-field-help")
  void superadminOnlyConfiguration(int width) {
    browserPage.setViewportSize(width, 900);
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    browserPage.navigate("/admin?section=configuration&group=two_factor");

    assertThat(browserPage.locator("#two-factor-save")).isVisible();
    assertThat(
            browserPage.getByText(
                uiTexts.getTwoFactorSettingsPageHint(), new Page.GetByTextOptions().setExact(true)))
        .isVisible();
    assertThat(
            browserPage
                .locator(".mfa-settings-header")
                .getByText(uiTexts.getTwoFactorSettingsHint()))
        .isVisible();
    Locator toggle = browserPage.locator("#account-navigation-toggle");
    if (toggle.isVisible()) {
      UiActions.complete(browserPage, toggle::click);
    }
    Locator configuration = browserPage.locator("#admin-nav-configuration-two_factor");
    configuration.hover();

    assertThat(browserPage.getByRole(AriaRole.TOOLTIP))
        .hasText(uiTexts.getTwoFactorSettingsTitle());
    if (toggle.isVisible()) {
      UiActions.complete(browserPage, toggle::click);
    }
    Locator tabs = browserPage.locator("#two-factor-role-tabs vaadin-tab");

    assertThat(tabs).hasCount(4);
    Locator roleContent = browserPage.locator(".mfa-role-content:visible");

    assertThat(roleContent.locator("vaadin-combo-box")).hasCount(9);
    assertThat(roleContent.locator(".mfa-config-heading").first())
        .hasText(uiTexts.getTwoFactorAccessGroup());
    Locator guide = roleContent.locator(".mfa-role-guide");

    assertThat(guide).containsText(uiTexts.getTwoFactorRoleScope(uiTexts.getRoleSuperadmin()));
    assertTrue(
        (Boolean)
            guide.evaluate(
                """
                (guide, desktop) => {
                  const fields = guide.parentElement.firstElementChild.getBoundingClientRect();
                  const help = guide.getBoundingClientRect();
                  return desktop ? help.left >= fields.right : help.top >= fields.bottom;
                }
                """,
                width == 1280));
    Locator administration = browserPage.locator("#two-factor-rule-role_superadmin-administration");
    verifyRuleChoice(administration);
    browserPage
        .getByRole(
            AriaRole.BUTTON,
            new Page.GetByRoleOptions()
                .setName(
                    uiTexts.getAdminConfigHelpTitle()
                        + " – "
                        + uiTexts.getTwoFactorRuleAdministration())
                .setExact(true))
        .click();
    Locator help = browserPage.locator("#two-factor-field-help");
    Locator helpOverlay = help.locator("vaadin-dialog-overlay > [part~=overlay]");

    assertThat(helpOverlay).isVisible();
    assertThat(help).containsText(uiTexts.getTwoFactorAdminAccessHelp());
    assertThat(help.locator(".admin-help-field-title"))
        .hasText(uiTexts.getTwoFactorRuleAdministration());
    assertThat(help.locator(".admin-help-default span").last())
        .hasText(uiTexts.getTwoFactorModeOff());
    List<String> helpItems = uiTexts.twoFactorRuleHelpItems();

    assertThat(help.locator(".admin-help-rules:not(.admin-help-effective-use) li"))
        .hasText(helpItems.toArray(String[]::new));
    ConfigurationPage.assertEffectiveUse(
        help,
        uiTexts,
        uiTexts.getAdminConfigEffectiveTwoFactorRule(),
        uiTexts.getAdminConfigEffectiveTwoFactorExisting());
    AllurePlaywright.attachScreenshot("2FA-Feldhilfe " + width, browserPage);
    browserPage.keyboard().press("Escape");

    assertThat(helpOverlay).isHidden();
    assertTwoFactorFieldTiming(tabs, help);
    tabs.nth(2).click();

    assertThat(guide).containsText(uiTexts.getTwoFactorRoleScope(uiTexts.getRoleUser()));
    assertThat(browserPage.locator("#two-factor-inactivity-enabled")).isVisible();
    assertTrue(
        (Boolean)
            browserPage.evaluate("document.documentElement.scrollWidth <= window.innerWidth"));
    assertThat(
            browserPage.getByText(
                uiTexts.getTwoFactorDevelopmentTitle(), new Page.GetByTextOptions().setExact(true)))
        .isHidden();
    AllurePlaywright.attachScreenshot(
        "Superadmin-Regeln für Zwei-Faktor-Authentifizierung", browserPage);
    try (BrowserActor administrator = new BrowserActor(testBrowser)) {
      administrator.login(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
      new UserMenu(administrator.browserPage).selectTestLanguage();
      administrator.browserPage.navigate("/admin?section=configuration&group=two_factor");

      assertThat(administrator.browserPage.locator("#two-factor-save")).hasCount(0);
      assertThat(administrator.browserPage.locator("#admin-nav-configuration-two_factor"))
          .isHidden();
      administrator.browserPage.navigate("/admin?section=configuration&group=external_login");

      assertThat(administrator.browserPage.locator("#admin-provider-settings")).hasCount(0);
      new UserMenu(administrator.browserPage).signOut();
    }
  }

  private void assertTwoFactorFieldTiming(Locator tabs, Locator help) {
    for (Locator tab : tabs.all()) {
      tab.click();
      Locator fields = browserPage.locator(".mfa-role-content:visible .cosmos-config-info");

      assertThat(fields).hasCount(9);
      for (Locator info : fields.all()) {
        info.click();
        assertTwoFactorTimingAndClose(help, uiTexts.getAdminConfigEffectiveTwoFactorRule());
      }
    }
    browserPage.locator("#two-factor-inactivity-enabled + vaadin-button").click();
    assertTwoFactorTimingAndClose(help, uiTexts.getAdminConfigEffectiveTwoFactorInactivitySwitch());
    browserPage.locator("#two-factor-inactivity-duration + vaadin-button").click();
    assertTwoFactorTimingAndClose(
        help, uiTexts.getAdminConfigEffectiveTwoFactorInactivityDuration());
  }

  private void assertTwoFactorTimingAndClose(Locator help, String timing) {
    ConfigurationPage.assertEffectiveUse(
        help, uiTexts, timing, uiTexts.getAdminConfigEffectiveTwoFactorExisting());
    browserPage.keyboard().press("Escape");

    assertThat(help.locator("vaadin-dialog-overlay > [part~=overlay]")).isHidden();
  }

  private void verifyRuleChoice(Locator field) {
    Locator input = field.locator("input");
    Locator menu = field.locator("vaadin-combo-box-overlay [part~=overlay]");
    field.locator("[part~=toggle-button]").click();

    assertThat(menu).isVisible();
    BoundingBox control = field.locator("[part~=input-field]").boundingBox();
    BoundingBox overlay = menu.boundingBox();

    assertTrue(
        overlay.y >= control.y + control.height - 1 || overlay.y + overlay.height <= control.y + 1);
    Locator off =
        browserPage.getByRole(
            AriaRole.OPTION,
            new Page.GetByRoleOptions().setName(uiTexts.getTwoFactorModeOff()).setExact(true));

    assertThat(off).hasAttribute("aria-selected", "true");
    browserPage.keyboard().press("Escape");

    assertThat(menu).isHidden();
    input.press("ArrowDown");

    assertThat(menu).isVisible();
    Locator required =
        browserPage.getByRole(
            AriaRole.OPTION,
            new Page.GetByRoleOptions().setName(uiTexts.getTwoFactorModeRequired()).setExact(true));
    UiActions.completeValueChange(browserPage, required::click);

    assertThat(input).hasValue(uiTexts.getTwoFactorModeRequired());
    input.clear();
    input.press("Tab");

    assertThat(input).hasValue(uiTexts.getTwoFactorModeRequired());
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @Tag("confirmation-screens")
  @Tag("follow-up-regression")
  @DisplayName("MFA-18: Codebestätigung, geschützter Faktorwechsel und Hilfe folgen Variante A")
  void confirmationRecoveryAndHelpUseNativeControls(int width) {
    browserPage.setViewportSize(width, 900);
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    String email = "confirmation-long-" + UUID.randomUUID() + "@example.test";
    String firstName = "Alexandra Maria Elisabeth";
    String lastName = "Beispielmann Sonnenschein";
    AdminUsersPage users = new AdminUsersPage(browserPage);
    users.open();
    AdminUserDialog account = users.openCreate();
    account.fillNewUser(email, BrowserTestAccounts.USER_PASSWORD, true, true);
    account.firstName().fill(firstName);
    account.lastName().fill(lastName);
    account.save();
    new UserMenu(browserPage).signOut();
    login.signIn(email, BrowserTestAccounts.USER_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    new UserMenu(browserPage).signOut();
    login.signInFirstFactor(email, BrowserTestAccounts.USER_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    assertReducedSecurityHeader(firstName + " " + lastName, email);
    UiTexts other = new UiTexts(uiTexts.language().equals("de") ? "en" : "de");
    assertNativeConfirmationFields(other);
    assertNativeRecoveryFields();
    TwoFactorPage.openLoginHelp(browserPage);
    browserPage.locator("#two-factor-help").click();

    assertThat(browserPage.locator("#account-support")).containsText(uiTexts.getSupportEmpty());
    assertReducedSecurityHeader(firstName + " " + lastName, email);

    assertThat(browserPage.locator(".cosmos-panel-footer #support-back")).isVisible();
    assertThat(browserPage.locator("#support-ticket-grid")).isHidden();
    assertThat(browserPage.locator("#support-reload, #support-close, #support-add-reply"))
        .hasCount(0);
    assertThat(browserPage.locator(".support-user-footer #support-sign-out ~ #support-create"))
        .isVisible();
    String draft = "Synthetische Bitte um Hilfe zu meinen gesicherten Ersatzcodes.";
    browserPage.locator("#support-reply textarea").fill(draft);
    new UserMenu(browserPage).selectLanguage(other.language());

    assertThat(browserPage.locator("#support-reply textarea")).hasValue(draft);
    new UserMenu(browserPage).selectTestLanguage();
    AllurePlaywright.attachScreenshot(
        "Hilfeanfrage mit Leerzustand und nativen Footeraktionen", browserPage);
    browserPage.locator("#support-create").click();

    assertThat(browserPage.locator("#support-ticket-grid"))
        .containsText(uiTexts.getSupportRecoverySubject());
    assertThat(browserPage.locator("#support-add-reply")).isVisible();
    assertThat(browserPage.locator("#support-back")).isVisible();
    assertThat(
            browserPage.locator(".support-conversation-actions #support-close ~ #support-reload"))
        .isVisible();
    assertThat(browserPage.locator(".support-user-footer #support-sign-out ~ #support-add-reply"))
        .isVisible();
    UiActions.complete(browserPage, () -> browserPage.locator("#support-close").click());

    assertThat(browserPage.locator("#support-add-reply")).isHidden();
    assertThat(browserPage.locator("#support-close")).isHidden();
    assertThat(browserPage.locator(".support-conversation-actions #support-reload")).isVisible();
    assertThat(browserPage.locator(".support-user-footer #support-sign-out ~ #support-create"))
        .isVisible();
    assertTrue(
        (Boolean)
            browserPage.evaluate("document.documentElement.scrollWidth <= window.innerWidth"));
    browserPage.locator("#support-back").click();

    assertThat(browserPage.locator("#two-factor-code")).isVisible();
  }

  private void assertNativeConfirmationFields(UiTexts other) {
    assertThat(browserPage.locator("#two-factor-code")).hasJSProperty("length", 6);
    assertThat(browserPage.locator("#two-factor-code [part=slot]")).hasCount(6);
    browserPage.locator("#two-factor-code input").fill("12345");

    assertThat(browserPage.locator("#two-factor-code [part=slot]"))
        .hasText(new String[] {"1", "2", "3", "4", "5", "•"});
    browserPage.locator("#two-factor-recovery-tab").click();
    String recoveryCode = "12345678-12345678-12345678-12345678";
    TwoFactorPage.enterCode(browserPage, recoveryCode);

    assertThat(browserPage.locator("#two-factor-code input")).hasValue(recoveryCode);
    new UserMenu(browserPage).selectLanguage(other.language());

    assertThat(browserPage.locator("#two-factor-recovery-tab")).hasAttribute("selected", "");
    assertThat(browserPage.locator("#two-factor-code"))
        .hasJSProperty("label", other.getTwoFactorRecoveryCodeLabel());
    new UserMenu(browserPage).selectTestLanguage();
    browserPage.locator("#two-factor-authenticator-tab").click();

    assertThat(browserPage.locator("#two-factor-code [part=slot]")).hasCount(6);
    assertTrue(
        (Boolean)
            browserPage.evaluate("document.documentElement.scrollWidth <= window.innerWidth"));
    AllurePlaywright.attachScreenshot(
        "Anmeldung bestätigen mit nativer Methodenauswahl", browserPage);

    assertThat(browserPage.locator("#two-factor-recover, #two-factor-help").first()).isHidden();
  }

  private void assertNativeRecoveryFields() {
    TwoFactorPage.openLoginHelp(browserPage);
    browserPage.locator("#two-factor-recover").click();

    assertThat(browserPage.locator("vaadin-text-field#two-factor-code input"))
        .hasAttribute("maxlength", "35");
    assertThat(browserPage.locator(".cosmos-panel-footer #two-factor-recovery-back"))
        .hasText(uiTexts.getTwoFactorReturnToCode());
    assertThat(browserPage.locator("#two-factor-help")).isHidden();
    assertThat(browserPage.locator("#two-factor-panel"))
        .containsText(uiTexts.getTwoFactorRecoveryConsumptionHint());
    AllurePlaywright.attachScreenshot(
        "Authenticator mit Ersatzcode geschützt wiederherstellen", browserPage);
    browserPage.locator("#two-factor-recovery-back").click();

    assertThat(browserPage).hasURL(Pattern.compile(".*/two-factor$"));
    assertThat(browserPage.locator("#two-factor-authenticator-tab")).isVisible();
  }

  private void assertReducedSecurityHeader(String name, String email) {
    Locator identity = browserPage.locator("#public-security-identity");
    Locator languages = browserPage.locator(".public-security-navigation .public-languages");

    assertThat(identity.locator(".cosmos-header-identity > span"))
        .hasText(new String[] {name, email});
    assertThat(identity.locator(".cosmos-user-avatar")).isVisible();
    assertThat(identity.locator(".cosmos-avatar-status")).isHidden();
    assertThat(identity.locator("vaadin-button, a")).hasCount(0);
    assertThat(browserPage.locator("#public-nav-home, #public-nav-login, #public-nav-register"))
        .hasCount(0);
    assertThat(languages.locator("vaadin-button")).hasCount(2);
    BoundingBox languageBounds = languages.boundingBox();
    BoundingBox identityBounds = identity.boundingBox();

    assertTrue(languageBounds.x + languageBounds.width <= identityBounds.x);
    assertTrue(identityBounds.x + identityBounds.width <= browserPage.viewportSize().width);
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName(
      "MFA-04: Hilfszugang ohne zweiten Faktor zeigt nur eigene Wiederherstellungsanfragen")
  void restrictedRecoveryHelp(int width) {
    browserPage.setViewportSize(width, 900);
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signInFirstFactor(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);

    assertThat(browserPage).hasURL(Pattern.compile(".*/two-factor$"));
    TwoFactorPage.openLoginHelp(browserPage);
    Locator help = browserPage.locator("#two-factor-help");

    assertThat(help).isVisible();
    assertTrue(
        (Boolean)
            help.evaluate(
                "button => { const box = button.getBoundingClientRect(); const area = button.parentElement.getBoundingClientRect(); const field = document.getElementById('two-factor-code').getBoundingClientRect(); return box.top >= field.bottom + 12 && box.left >= area.left - 1 && box.right <= area.right + 1 && button.scrollWidth <= button.clientWidth + 1; }"),
        "Der übersetzte Hilfebutton muss innerhalb des Inhalts bleiben.");
    AllurePlaywright.attachScreenshot("Zwei-Faktor-Hilfe ohne überbreiten Button", browserPage);
    // Der erste Fall hat dieses feste Testkonto eingerichtet. Im getrennten Hilfszugang
    // bleiben Authenticator und Backupcodes absichtlich unbenutzt.
    help.click();

    assertThat(browserPage.locator("#account-support")).isVisible();
    browserPage
        .locator("#support-reply textarea")
        .fill("Synthetische Anfrage: Ich finde meine gesicherten Ersatzcodes nicht.");
    if (browserPage.locator("#support-create").isVisible()) {
      browserPage.locator("#support-create").click();

      assertThat(browserPage.locator("#support-ticket-grid"))
          .containsText(uiTexts.getSupportRecoverySubject());
    }
    AllurePlaywright.attachScreenshot(
        "Eingeschränkte eigene Wiederherstellungsanfrage", browserPage);
    browserPage.navigate("/admin");

    assertThat(browserPage).hasURL(Pattern.compile(".*/recovery-help$"));
    browserPage.locator("#support-sign-out").click();

    assertThat(browserPage).hasURL(Pattern.compile(".*/login$"));
  }
}
