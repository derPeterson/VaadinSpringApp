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
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    AdminUsersPage users = new AdminUsersPage(page);
    users.open();
    List<String> numbers = new ArrayList<>();
    for (int index = 0; index < 12; index++) {
      String email = "ticket-pages-" + UUID.randomUUID() + "@example.test";
      AdminUserDialog requesterAccount = users.openCreate();
      requesterAccount.fillNewUser(email, BrowserTestAccounts.USER_PASSWORD, true, false);
      requesterAccount.save();
      try (BrowserActor requester = new BrowserActor(browser)) {
        requester.login(email, BrowserTestAccounts.USER_PASSWORD);
        requester.page.navigate("/recovery-help");
        requester
            .page
            .locator("#support-reply textarea")
            .fill("Synthetic request for pagination " + index);
        UiActions.complete(requester.page, () -> requester.page.locator("#support-create").click());
        numbers.add(
            requester
                .page
                .locator("#support-ticket-grid")
                .getByText(Pattern.compile("^#[0-9]+$"))
                .first()
                .innerText()
                .substring(1));
      }
    }
    for (int width : new int[] {1280, 390}) {
      page.setViewportSize(width, 1000);
      page.navigate("/admin?section=support&ticket=" + numbers.getFirst());
      assertThat(page.locator("#support-ticket-panel")).isVisible();
      if (page.locator("#support-claim").isVisible()) {
        UiActions.complete(page, () -> page.locator("#support-claim").click());
        jumpToTicket(numbers.getFirst());
      }
      Locator paging = page.locator("#support-pagination");
      Locator next = paging.locator(".cosmos-pagination-next");
      Locator previous = paging.locator(".cosmos-pagination-previous");
      Locator rows = page.locator("#support-ticket-grid .support-admin-ticket-card:visible");
      Locator input = page.locator("#support-reply textarea");
      assertThat(rows).hasCount(10);
      Locator scroll = page.locator(".support-admin-ticket-scroll");
      assertTrue((Boolean) scroll.evaluate("area => area.scrollHeight > area.clientHeight"));
      rows.last().scrollIntoViewIfNeeded();
      assertTrue((Boolean) scroll.evaluate("area => area.scrollTop > 0"));
      assertThat(previous).isDisabled();
      assertThat(next).isEnabled();
      assertThat(paging.locator(".admin-user-pagination-arrow:visible")).hasCount(2);
      input.fill("This unsent reply survives changing ticket pages.");
      UiActions.complete(page, next::click);
      assertThat(previous).isEnabled();
      assertThat(page.locator("#support-ticket-panel .support-ticket-number"))
          .hasText(texts.getSupportTicketNumber("#" + numbers.getFirst()));
      assertThat(input).hasValue("This unsent reply survives changing ticket pages.");
      UiActions.complete(page, previous::click);
      assertThat(previous).isDisabled();
      for (int size : new int[] {20, 50, 10}) {
        if (width == 390) {
          page.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.ACTIVE));
        }
        paging.locator("vaadin-combo-box").click();
        UiActions.completeValueChange(
            page,
            () ->
                page.getByRole(
                        AriaRole.OPTION,
                        new Page.GetByRoleOptions().setName(Integer.toString(size)).setExact(true))
                    .click());
        if (width == 390) {
          page.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.NONE));
        }
        assertThat(paging.locator("vaadin-combo-box input")).hasValue(Integer.toString(size));
        assertThat(previous).isDisabled();
        String range = paging.locator(".admin-user-pagination-total").innerText();
        int total = Integer.parseInt(range.substring(range.lastIndexOf(' ') + 1));
        assertThat(rows).hasCount(Math.min(size, total));
        assertThat(input).hasValue("This unsent reply survives changing ticket pages.");
      }
      AllurePlaywright.attachScreenshot("Vorgänge mit gekürzter Pagination " + width, page);
      input.clear();
    }
    verifyLivePaginationAndFilteredDraft(numbers.getFirst());
  }

  private void verifyLivePaginationAndFilteredDraft(String assigned) {
    String route = "/admin?section=support";
    String rowSelector = "#support-ticket-grid .support-admin-ticket-card:visible";
    try (Page mobile = context.newPage();
        Page editor = context.newPage()) {
      page.setViewportSize(1280, 1000);
      mobile.setViewportSize(390, 900);
      for (Page view : List.of(page, mobile)) {
        view.navigate(route + "&ticket=" + assigned);
        assertThat(view.locator("#support-ticket-panel")).isVisible();
        view.locator("#support-reply textarea").fill("Draft retained outside the filtered list.");
        view.locator("#support-filter").click();
        Locator filter = view.locator("#support-filter-panel");
        filter
            .locator("#support-filter-status")
            .getByText(texts.getSupportOpen(), new Locator.GetByTextOptions().setExact(true))
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
          page.locator(rowSelector).getByText(Pattern.compile("^#[0-9]+$")).allTextContents();
      assertTrue(!lastNumbers.isEmpty());
      assertThat(mobile.locator(rowSelector)).hasCount(lastNumbers.size());
      for (int index = 0; index < lastNumbers.size(); index++) {
        if (index == lastNumbers.size() - 1) {
          assertSingleTicketListFits(page);
          assertSingleTicketListFits(mobile);
        }
        String number = lastNumbers.get(index);
        editor.navigate(route + "&ticket=" + number.substring(1));
        UiActions.complete(editor, () -> editor.locator("#support-claim").click());
        int remaining = lastNumbers.size() - index - 1;
        for (Page view : List.of(page, mobile)) {
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
    page.locator("#support-filter").click();
    Locator filter = page.locator("#support-filter-panel");
    filter.locator("#support-jump-number input").fill(number);
    UiActions.complete(page, () -> filter.locator("#support-jump").click());
    assertThat(filter).not().hasAttribute("opened", "");
  }

  @Test
  @Tag("ticket-filters")
  @Tag("follow-up-regression")
  @DisplayName("MFA-22: Vorgangsfilter und Ticketsprung verwenden die gemeinsamen Filterbausteine")
  void ticketFiltersAndDirectJumpUseNativeControls() {
    context.grantPermissions(List.of("clipboard-read", "clipboard-write"));
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    String email = "ticket-filter-" + UUID.randomUUID() + "@example.test";
    new AdminUsersPage(page).createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
    try (BrowserActor requester = new BrowserActor(browser)) {
      requester.login(email, BrowserTestAccounts.USER_PASSWORD);
      requester.page.navigate("/recovery-help");
      requester
          .page
          .locator("#support-reply textarea")
          .fill("Synthetic request for ticket filters.");
      UiActions.complete(requester.page, () -> requester.page.locator("#support-create").click());
      String number =
          requester
              .page
              .locator("#support-ticket-grid")
              .getByText(Pattern.compile("^#[0-9]+$"))
              .first()
              .innerText();
      for (int width : new int[] {1280, 390}) {
        page.setViewportSize(width, 900);
        page.navigate("/admin");
        requester.page.locator("#support-reply textarea").fill("Unread activity at width " + width);
        UiActions.complete(
            requester.page, () -> requester.page.locator("#support-add-reply").click());
        assertSupportNavigationActivity(width);
        page.navigate("/admin?section=support");
        assertThat(page.locator("#support-select-ticket")).isVisible();
        page.locator("#support-filter").click();
        Locator panel = page.locator("#support-filter-panel");
        assertThat(page.locator("#support-filter-panel"))
            .hasClass(Pattern.compile(".*admin-user-filter-drawer.*"));
        panel
            .locator("#support-filter-status")
            .getByText(texts.getSupportOpen(), new Locator.GetByTextOptions().setExact(true))
            .click();
        panel
            .locator("#support-filter-assignment")
            .getByText(texts.getSupportFilterMine(), new Locator.GetByTextOptions().setExact(true))
            .click();
        panel
            .locator("#support-filter-reading")
            .getByText(
                texts.getNotificationsUnread(), new Locator.GetByTextOptions().setExact(true))
            .click();
        assertThat(panel.locator("#support-filter-draft-count"))
            .hasText(texts.getAdminUsersActiveFilterCount(3));
        assertThat(panel.locator("#support-jump-info"))
            .hasText(texts.getSupportJumpTicketNumberHint());
        AllurePlaywright.attachScreenshot("Vorgangsfilter mit Testwerten " + width, page);
        panel.locator("#support-filter-cancel").click();
        page.locator("#support-filter").click();
        assertThat(panel.locator("#support-filter-status input:checked")).hasCount(0);
        Locator openStatus =
            panel
                .locator("#support-filter-status")
                .getByText(texts.getSupportOpen(), new Locator.GetByTextOptions().setExact(true));
        openStatus.click();
        panel.locator("#support-filter-reset").click();
        assertThat(panel.locator("#support-filter-status input:checked")).hasCount(0);
        openStatus.click();
        page.keyboard().press("Escape");
        assertThat(panel).not().hasAttribute("opened", "");
        page.locator("#support-filter").click();
        assertThat(panel.locator("#support-filter-status input:checked")).hasCount(0);
        // Auf dem schmalen Display lässt nur die Queransicht eine Fläche neben dem Drawer frei.
        page.setViewportSize(width, width == 390 ? 340 : 900);
        openStatus.click();
        Locator overlay = panel.locator("vaadin-dialog-overlay [part~=overlay]");
        assertThat(overlay).isVisible();
        BoundingBox overlayBounds = overlay.boundingBox();
        assertTrue(overlayBounds.x > 0, "Der Außenklick braucht eine sichtbare Fläche");
        if (width == 390) {
          Locator drawerBody = panel.locator("vaadin-dialog-overlay [part=content]");
          assertTouchScroll(page, drawerBody);
        }
        page.mouse().click(overlayBounds.x / 2, 150);
        assertThat(panel).not().hasAttribute("opened", "");
        page.setViewportSize(width, 900);
        page.locator("#support-filter").click();
        assertThat(panel.locator("#support-filter-status input:checked")).hasCount(0);
        panel
            .locator("#support-filter-reading")
            .getByText(
                texts.getNotificationsUnread(), new Locator.GetByTextOptions().setExact(true))
            .click();
        panel.locator("#support-filter-apply").click();
        assertThat(page.locator("#support-active-filter-count")).hasText("1");
        Locator unread = ticketCard(page, number).locator(".support-ticket-unread");
        int previousUnread = Integer.parseInt(unread.innerText());
        requester.page.locator("#support-reply textarea").fill("Live change at width " + width);
        UiActions.complete(
            requester.page, () -> requester.page.locator("#support-add-reply").click());
        assertThat(unread).hasText(Integer.toString(previousUnread + 1));
        assertThat(page.locator("#support-active-filter-count")).hasText("1");
        assertThat(ticketCard(page, number)).isVisible();
        UiActions.complete(page, () -> ticketCard(page, number).click());
        assertThat(page.locator("#support-ticket-panel")).isVisible();
        assertThat(ticketCard(page, number)).not().isVisible();
        assertThat(page.locator("#support-ticket-panel")).isVisible();
        page.locator("#support-filter").click();
        Locator numberInput = panel.locator("#support-jump-number input");
        numberInput.fill("0");
        UiActions.complete(page, () -> panel.locator("#support-jump").click());
        assertThat(panel).hasAttribute("opened", "");
        assertThat(page.locator("#support-active-filter-count")).hasText("1");
        numberInput.clear();
        numberInput.pressSequentially(number.substring(1));
        UiActions.complete(page, () -> panel.locator("#support-jump").click());
        assertThat(panel).not().hasAttribute("opened", "");
        assertThat(page.locator("#support-ticket-panel")).isVisible();
        assertThat(page)
            .hasURL(Pattern.compile(".*[?&]ticket=" + number.substring(1) + "(?:&.*)?$"));
        assertThat(page.locator("#support-active-filter-count")).not().isVisible();
        page.locator("#support-filter").click();
        numberInput.click();
        assertThat(numberInput).isFocused();
        page.evaluate("value => navigator.clipboard.writeText(value)", number);
        numberInput.press("ControlOrMeta+V");
        assertThat(numberInput).hasValue(number);
        UiActions.complete(page, () -> panel.locator("#support-jump").click());
        assertThat(panel).not().hasAttribute("opened", "");
        assertThat(page.locator("#support-ticket-panel .support-ticket-number"))
            .hasText(texts.getSupportTicketNumber(number));
      }
    }
  }

  private void assertSupportNavigationActivity(int width) {
    if (width == 390) {
      Locator toggle = page.locator("#account-navigation-toggle");
      assertThat(toggle).isVisible();
      UiActions.complete(page, toggle::click);
      assertThat(toggle).hasAttribute("aria-expanded", "true");
    }
    assertThat(page.locator("#admin-nav-support-activity"))
        .hasAttribute("aria-label", texts.getSupportNewActivity());
    Locator item = page.locator("#admin-nav-support");
    item.hover();
    assertThat(page.getByRole(AriaRole.TOOLTIP)).hasText(texts.getSupportNewActivity());
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
    LoginPage administrator = new LoginPage(page);
    administrator.open();
    administrator.signIn(
        BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    String email = "otp-behavior-" + UUID.randomUUID() + "@example.test";
    String adminEmail = "otp-admin-" + UUID.randomUUID() + "@example.test";
    AdminUsersPage users = new AdminUsersPage(page);
    users.createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
    users.createUser(adminEmail, BrowserTestAccounts.USER_PASSWORD, true, true);
    try (BrowserActor actor = new BrowserActor(browser)) {
      actor.page.setViewportSize(width, 900);
      actor.login(email, BrowserTestAccounts.USER_PASSWORD);
      new UserMenu(actor.page).selectTestLanguage();
      actor.page.navigate("/account?section=security&tab=portal");
      actor.page.locator("#account-two-factor-setup").click();
      actor.page.locator("#two-factor-password input").fill(BrowserTestAccounts.USER_PASSWORD);
      actor.page.locator("#two-factor-submit").click();
      checkEnrollmentOtp(actor.page, email, false);

      actor.page.locator("#account-two-factor-replace").click();
      TwoFactorPage.enterActionCode(actor.page, TwoFactorPage.nextCode(actor.page, email));
      actor.page.locator("#two-factor-action-confirm").click();
      checkEnrollmentOtp(actor.page, email, true);
      signOutToLogin(actor.page);

      LoginPage login = new LoginPage(actor.page);
      login.signInFirstFactor(email, BrowserTestAccounts.USER_PASSWORD);
      assertOtpFocus(actor.page, "two-factor-code");
      TwoFactorPage.selectTestLanguageIfNeeded(actor.page);
      actor.page.locator("#two-factor-recovery-tab").click();
      actor.page.locator("#two-factor-authenticator-tab").click();
      checkOtpInputAndConfirm(actor.page, email, "two-factor-code", false);
      actor.page.waitForURL(url -> !URI.create(url).getPath().equals("/two-factor"));
      signOutToLogin(actor.page);

      login.signInFirstFactor(email, BrowserTestAccounts.USER_PASSWORD);
      TwoFactorPage.selectTestLanguageIfNeeded(actor.page);
      TwoFactorPage.openLoginHelp(actor.page);
      UiActions.complete(actor.page, () -> actor.page.locator("#two-factor-recover").click());
      Locator recoveryInput = actor.page.locator("vaadin-text-field#two-factor-code input");
      assertThat(recoveryInput).isVisible();
      recoveryInput.fill(TwoFactorPage.nextCode(actor.page, email));
      actor.page.locator("#two-factor-submit").click();
      checkEnrollmentOtp(actor.page, email, true);
      signOutToLogin(actor.page);

      login.signInFirstFactor(adminEmail, BrowserTestAccounts.USER_PASSWORD);
      TwoFactorPage.selectTestLanguageIfNeeded(actor.page);
      assertThat(actor.page.locator("#two-factor-introduction-title")).isVisible();
      actor.page.locator("#two-factor-submit").click();
      checkEnrollmentOtp(actor.page, adminEmail, true);
      signOutToLogin(actor.page);
      login.signInFirstFactor(adminEmail, BrowserTestAccounts.USER_PASSWORD);
      actor.page.navigate("/two-factor?administration");
      assertOtpFocus(actor.page, "two-factor-code");
      TwoFactorPage.selectTestLanguageIfNeeded(actor.page);
      checkOtpInputAndConfirm(actor.page, adminEmail, "two-factor-code", true);
      actor.page.waitForURL(url -> !URI.create(url).getPath().equals("/two-factor"));
      signOutToLogin(actor.page);
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
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    String suffix = UUID.randomUUID().toString();
    String requesterEmail = "ticket-requester-" + suffix + "@example.test";
    String otherRequesterEmail = "ticket-other-" + suffix + "@example.test";
    String assigneeEmail = "ticket-assignee-" + suffix + "@example.test";
    String firstName = "AlexandervonWiederherstellungsanfragen";
    String lastName = "MusterbearbeiterMitBesondersLangemNamen";
    AdminUsersPage users = new AdminUsersPage(page);
    users.createActiveUser(requesterEmail, BrowserTestAccounts.USER_PASSWORD);
    users.createActiveUser(otherRequesterEmail, BrowserTestAccounts.USER_PASSWORD);
    AdminUserDialog assigneeForm = users.openCreate();
    assigneeForm.fillNewUser(assigneeEmail, BrowserTestAccounts.USER_PASSWORD, true, true);
    assigneeForm.firstName().fill(firstName);
    assigneeForm.lastName().fill(lastName);
    assigneeForm.save();
    page.setViewportSize(width, 1000);
    page.navigate("/home");
    assertThat(page.locator("#account-header-notifications")).isVisible();
    int previousNotifications = notificationCount(page);
    try (BrowserActor assignee = new BrowserActor(browser);
        BrowserActor requester = new BrowserActor(browser)) {
      assignee.login(assigneeEmail, BrowserTestAccounts.USER_PASSWORD);
      requester.login(requesterEmail, BrowserTestAccounts.USER_PASSWORD);
      Page admin = assignee.page;
      Page user = requester.page;
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
      assertNotificationCount(page, previousNotifications + 1);
      assertNotificationCount(admin, 1);
      NotificationInboxPage inbox = new NotificationInboxPage(page);
      verifyClaimNotification(admin, inbox, number, previousNotifications);
      inbox.closeDetail();
      page.navigate("/admin?section=support");
      assertThat(ticketCard(page, number).locator(".support-ticket-unread")).hasCount(0);
      UiActions.complete(page, () -> ticketCard(page, number).click());
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
      UiActions.complete(page, () -> ticketCard(page, number).click());
      setTicketWatch(page, true);
      page.navigate("/admin?section=support");
      admin.navigate("/home");
      verifyRequesterClosureSurvivesOlderRead(user, number, previousNotifications);
      assertNotificationCount(page, previousNotifications + 1);
      // Der Bearbeiter erhält den Nutzerabschluss ausdrücklich auch ohne aktiviertes Beobachten.
      assertNotificationCount(admin, 1);
      assertThat(user.locator("#support-add-reply")).not().isVisible();
      verifyClosureNotification(admin, inbox, number, previousNotifications);

      user.locator("#support-reply textarea").fill("Zweite Anfrage ohne Bearbeiter.");
      UiActions.complete(user, () -> user.locator("#support-create").click());
      assertThat(
              user.locator("#support-ticket-grid").getByText(Pattern.compile("^#[0-9]+$")).first())
          .not()
          .hasText(number);
      String unassignedNumber =
          user.locator("#support-ticket-grid")
              .getByText(Pattern.compile("^#[0-9]+$"))
              .first()
              .innerText();
      UiActions.complete(user, () -> user.locator("#support-close").click());
      assertNotificationCount(page, previousNotifications + 1);
      assertNotificationCount(admin, 1);
      assertThat(ticketCard(admin, unassignedNumber).locator(".support-ticket-unread"))
          .hasText("1");
      UiActions.complete(admin, () -> ticketCard(admin, unassignedNumber).click());
      assertNotificationCount(admin, 0);
      UiActions.complete(admin, () -> admin.locator("#support-close").click());
      assertNotificationCount(page, previousNotifications);
      assertThat(admin.locator("#support-ticket-status-note"))
          .hasText(texts.getSupportClosedHint());
      verifyOtherTicketCounterSurvivesReading(admin, user, number);
      verifyLiveRequesterSwitch(number, otherRequesterEmail);
      AllurePlaywright.attachScreenshot("Abgeschlossener Vorgang ohne Ungelesen-Zähler", admin);
    }
  }

  private void verifyWatchedAndUnwatchedReplies(
      Page admin, Page user, String number, int previousNotifications) {
    setTicketWatch(page, true);
    page.navigate("/home");
    admin.navigate("/home");
    user.locator("#support-reply textarea")
        .fill("Neue Nachricht für den beobachtenden Superadmin.");
    UiActions.complete(user, () -> user.locator("#support-add-reply").click());
    assertNotificationCount(page, previousNotifications + 1);
    assertNotificationCount(admin, 0);
    page.navigate("/admin?section=support");
    admin.navigate("/admin?section=support");
    assertThat(ticketCard(page, number).locator(".support-ticket-unread")).hasText("1");
    assertThat(ticketCard(admin, number).locator(".support-ticket-unread")).hasText("1");
    try (Page second = page.context().newPage()) {
      second.setViewportSize(page.viewportSize().width, 900);
      second.navigate("/admin?section=support");
      assertThat(ticketCard(second, number).locator(".support-ticket-unread")).hasText("1");
      Locator unread = ticketCard(second, number).locator(".support-ticket-unread");
      withDelayedTicketRead(
          page,
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
    assertNotificationCount(page, previousNotifications);
    setTicketWatch(page, false);
    page.navigate("/home");
    user.locator("#support-reply textarea").fill("Ohne Beobachtung bleibt die Glocke ruhig.");
    UiActions.complete(user, () -> user.locator("#support-add-reply").click());
    // Erst die tatsächlich eingetroffene Live-Aktualisierung prüfen, dann das Ausbleiben des
    // Zählers.
    assertThat(ticketCard(admin, number).locator(".support-ticket-unread")).hasText("3");
    page.navigate("/admin?section=support");
    assertThat(ticketCard(page, number).locator(".support-ticket-unread")).hasCount(0);
    assertNotificationCount(page, previousNotifications);
    assertNotificationCount(admin, 0);
  }

  private void verifyRequesterClosureSurvivesOlderRead(
      Page requester, String number, int previousNotifications) {
    requester.locator("#support-reply textarea").fill("Letzte Nachricht vor dem Nutzerabschluss.");
    UiActions.complete(requester, () -> requester.locator("#support-add-reply").click());
    assertNotificationCount(page, previousNotifications + 1);
    try (Page reader = page.context().newPage()) {
      reader.setViewportSize(page.viewportSize().width, 900);
      reader.navigate("/admin?section=support");
      withDelayedTicketRead(
          reader,
          number,
          acknowledge -> {
            UiActions.complete(requester, () -> requester.locator("#support-close").click());
            assertNotificationCount(page, previousNotifications + 1);
            assertThat(ticketCard(page, number).locator(".support-ticket-status"))
                .hasText(texts.getSupportClosedByRequester());
            Locator unread = ticketCard(page, number).locator(".support-ticket-unread");
            assertThat(unread).hasText("1");
            UiActions.complete(reader, acknowledge);
            assertThat(unread).hasCount(0);
            assertNotificationCount(page, previousNotifications + 1);
            reader.navigate("/home");
          });
    }
  }

  private void withDelayedTicketRead(Page reader, String number, Consumer<Runnable> whileHeld) {
    // Die echte Vaadin-Rückmeldung anhalten, bis die neuere Änderung gespeichert ist.
    Pattern uidl = Pattern.compile(".*[?&]v-r=uidl(?:&.*)?$");
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
    page.navigate("/admin?section=support");
    assertThat(ticketCard(page, number)).isVisible();
    page.locator("#support-filter").click();
    Locator filters = page.locator("#support-filter-panel");
    filters
        .locator("#support-filter-assignment")
        .getByText(texts.getSupportFilterMine(), new Locator.GetByTextOptions().setExact(true))
        .click();
    UiActions.complete(page, () -> filters.locator("#support-filter-apply").click());
    assertThat(page.locator("#support-active-filter-count")).hasText("1");
    assertThat(ticketCard(page, number)).isHidden();
    inbox.openPreview();
    inbox
        .row(inbox.preview(), texts.getSupportRecoverySubject())
        .locator(".cosmos-notification-row-open")
        .first()
        .click();
    assertThat(inbox.dialog().locator(".cosmos-notification-detail-state"))
        .hasText(texts.getNotificationsUnread());
    Locator target = inbox.dialog().locator(".cosmos-notification-detail-ticket");
    assertThat(target).hasAccessibleName(texts.getNotificationsOpenTicket());
    assertThat(inbox.dialog().locator(".cosmos-notification-detail-account")).hasCount(0);
    if (page.viewportSize().width == 390) {
      assertThat(target).hasCSS("grid-column-start", "1");
      assertThat(target).hasCSS("grid-column-end", "-1");
    }
    target.click();
    assertThat(page)
        .hasURL(
            Pattern.compile(
                "(?=.*[?&]section=support(?:&|$))(?=.*[?&]ticket="
                    + number.substring(1)
                    + "(?:&|$)).*/admin\\?.*"));
    assertThat(page.locator("#support-ticket-panel .support-ticket-number"))
        .hasText(texts.getSupportTicketNumber(number));
    assertThat(page.locator("#support-active-filter-count")).isHidden();
    assertThat(ticketCard(page, number)).isVisible();
    assertThat(page.locator("#support-claim")).isVisible();
    // Erst der sichtbare Verlauf quittiert die darin dargestellte Nachricht.
    assertNotificationCount(page, previousNotifications);
    inbox.open();
    inbox.filter(inbox.inbox(), texts.getNotificationsAll());
    inbox
        .row(inbox.inbox(), texts.getSupportRecoverySubject())
        .locator(".cosmos-notification-row-open")
        .first()
        .click();
    assertThat(inbox.dialog().locator(".cosmos-notification-detail-state"))
        .hasText(texts.getNotificationsRead());
    admin.navigate("/admin?section=support");
    assertThat(ticketCard(admin, number).locator(".support-ticket-unread")).hasText("1");
    UiActions.complete(admin, () -> ticketCard(admin, number).click());
    assertNotificationCount(admin, 0);
    assertThat(admin.locator("#support-ticket-status-note"))
        .hasText(texts.getSupportUnassignedHint());
    UiActions.complete(admin, () -> admin.locator("#support-claim").click());
    assertThat(admin.locator("#support-ticket-status-note")).not().isVisible();
    assertThat(inbox.dialog().locator("[data-relevance=TICKET_ASSIGNED]"))
        .hasText(texts.getNotificationsTicketAssigned());
    assertNotificationCount(page, previousNotifications);
    AllurePlaywright.attachScreenshot("Übernommener Hinweis im geöffneten Glockendialog", page);
  }

  private void verifyClosureNotification(
      Page admin, NotificationInboxPage inbox, String number, int previousNotifications) {
    inbox.open();
    inbox.search(texts.getSupportTicketNumber("").strip());
    inbox
        .row(inbox.inbox(), texts.getSupportRequesterClosedNotificationTitle(number))
        .locator(".cosmos-notification-row-open")
        .click();
    admin.navigate("/admin?section=support");
    UiActions.complete(admin, () -> ticketCard(admin, number).click());
    assertNotificationCount(admin, 0);
    assertThat(admin.locator("#support-ticket-status-note"))
        .hasText(texts.getSupportRequesterClosedHint());
    assertThat(admin.locator("#support-ticket-status-note")).hasCSS("text-align", "center");
    assertThat(admin.locator("#support-ticket-status-note")).hasCSS("font-style", "italic");
    assertThat(admin.locator("#support-add-reply")).not().isVisible();
    AllurePlaywright.attachScreenshot("Nutzerabschluss mit prüfbarem Statushinweis", admin);
    UiActions.complete(admin, () -> admin.locator("#support-close").click());
    assertThat(admin.locator("#support-ticket-status-note")).hasText(texts.getSupportClosedHint());
    assertThat(ticketCard(admin, number).locator(".support-ticket-unread")).hasCount(0);
    assertThat(inbox.dialog().locator("[data-relevance=TICKET_CLOSED]"))
        .hasText(texts.getSupportClosed());
    assertNotificationCount(page, previousNotifications);
    AllurePlaywright.attachScreenshot(
        "Erledigter Glockenhinweis bleibt als Historie sichtbar", page);
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
        .hasText(texts.getSupportTicketNumber(closed));
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
    assertThat(activeCard.locator(".support-ticket-status")).hasText(texts.getSupportInProgress());
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
    page.navigate("/admin?section=support&ticket=" + firstTicket.substring(1));
    Locator avatar = page.locator("#support-detail-requester .cosmos-user-avatar");
    assertThat(avatar).isVisible();
    String firstEmail =
        page.locator("#support-detail-requester .support-admin-person-email").innerText();
    assertIdentityTooltip(page, avatar, "Browser Testuser", firstEmail, false);
    try (BrowserActor other = new BrowserActor(browser)) {
      other.login(otherEmail, BrowserTestAccounts.USER_PASSWORD);
      other.page.setViewportSize(page.viewportSize().width, 1000);
      other.page.navigate("/recovery-help");
      other.page.locator("#support-reply textarea").fill("Anfrage eines anderen Testkontos.");
      UiActions.complete(other.page, () -> other.page.locator("#support-create").click());
      String number =
          other.page.locator("#support-ticket-grid .support-ticket-number").first().innerText();
      UiActions.complete(page, () -> ticketCard(page, number).click());
      assertIdentityTooltip(page, avatar, "Browser Testuser", otherEmail, false);
      String followUp = "Ergänzung des zweiten Testkontos im Live-Verlauf.";
      other.page.locator("#support-reply textarea").fill(followUp);
      UiActions.complete(other.page, () -> other.page.locator("#support-add-reply").click());
      Locator message =
          page.locator("#admin-support .support-message")
              .filter(new Locator.FilterOptions().setHasText(followUp));
      assertThat(message).isVisible();
      assertIdentityTooltip(
          page, message.locator(".cosmos-user-avatar"), "Browser Testuser", otherEmail, false);
      assertIdentityTooltip(page, avatar, "Browser Testuser", otherEmail, false);
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
    Locator foreign = page.locator("#support-detail-assignee");
    Locator own = assignee.locator("#support-detail-assignee");
    Locator requester = page.locator("#support-detail-requester");
    assertThat(requester.locator(".support-admin-person-label"))
        .hasText(texts.getSupportRequester());
    assertThat(foreign.locator(".support-admin-person-label"))
        .hasText(texts.getSupportAssigneeColumn());
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
    if (page.viewportSize().width == 390) {
      assertTrue(
          (Boolean)
              name.evaluate(
                  """
          name => name.getBoundingClientRect().height > parseFloat(getComputedStyle(name).lineHeight)
              && name.scrollWidth <= name.clientWidth
          """));
    }
    assertThat(foreign.locator(".support-assignee-self")).hasCount(0);
    assertThat(ticketCard(page, number).locator(".support-assignee-self")).hasCount(0);
    assertThat(own.locator(".support-assignee-self")).hasText(texts.getSupportYou());
    assertThat(own.locator(".support-assignee-self"))
        .hasCSS("border-top-color", "rgba(16, 185, 129, 0.2)");
    assertThat(own.locator(".support-assignee-self")).hasCSS("border-top-width", "1px");
    assertThat(ticketCard(assignee, number).locator(".support-assignee-self"))
        .hasText(texts.getSupportYou());
    assertThat(foreign.locator(".cosmos-user-avatar")).hasCSS("width", "32px");
    assertThat(own.locator(".cosmos-user-avatar")).hasCSS("width", "32px");
    assertIdentityTooltip(
        page,
        ticketCard(page, number).locator(".cosmos-user-avatar"),
        assigneeName,
        assigneeEmail,
        false);
    assertIdentityTooltip(
        page, foreign.locator(".cosmos-user-avatar"), assigneeName, assigneeEmail, true);
    assertThat(requester.locator(".support-admin-person-email")).hasText(requesterEmail);
    assertThat(requester.locator(".support-admin-person-name")).hasText("Browser Testuser");
    assertThat(requester.locator(".cosmos-user-avatar")).hasCSS("width", "32px");
    assertIdentityTooltip(
        page, requester.locator(".cosmos-user-avatar"), "Browser Testuser", requesterEmail, false);
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
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    page.navigate("/admin?section=configuration&group=external_login");
    Locator providerTab = page.locator("#admin-provider-tabs vaadin-tab[selected]");
    Map<Integer, Object> providerTabStyles = new HashMap<>();
    for (int width : new int[] {1280, 390}) {
      page.setViewportSize(width, 1000);
      assertThat(providerTab).isVisible();
      providerTabStyles.put(width, selectedTabStyle(providerTab));
    }
    page.navigate("/recovery-help");
    page.locator("#support-reply textarea")
        .fill("Synthetische erste Anfrage für die Layoutprüfung.");
    UiActions.complete(page, () -> page.locator("#support-create").click());
    String ownClosedNumber =
        page.locator("#support-ticket-grid")
            .getByText(Pattern.compile("^#[0-9]+$"))
            .first()
            .innerText();
    UiActions.complete(page, () -> page.locator("#support-close").click());
    page.locator("#support-reply textarea")
        .fill("Synthetische offene Anfrage für die Layoutprüfung.");
    UiActions.complete(page, () -> page.locator("#support-create").click());
    assertThat(page.locator("#support-ticket-grid").getByText(Pattern.compile("^#[0-9]+$")).first())
        .not()
        .hasText(ownClosedNumber);
    String ownOpenNumber =
        page.locator("#support-ticket-grid")
            .getByText(Pattern.compile("^#[0-9]+$"))
            .first()
            .innerText();
    page.locator("#support-back").click();
    assertThat(page.locator("#account-panel-security")).isVisible();
    page.navigate("/admin?section=support");
    Locator categories = page.locator("#support-ticket-categories");
    Locator hint = page.locator("#support-select-ticket");
    Locator heading = page.locator("#support-page-header");
    assertThat(heading).containsText(texts.getSupportPageDescription());
    assertThat(page.locator("#support-panel .cosmos-panel-description").first())
        .hasText(texts.getSupportInboxDescription());
    assertThat(hint).containsText(texts.getSupportSelectTicket());
    assertThat(hint.locator("[data-lucide=mouse-pointer-click]")).isVisible();
    UiActions.complete(page, () -> categories.locator("vaadin-tab").nth(1).click());
    assertTicketLocation("recovery", null);
    assertThat(heading).containsText(texts.getSupportPageDescription());
    UiTexts otherLanguage = new UiTexts(texts.language().equals("de") ? "en" : "de");
    new UserMenu(page).selectLanguage(otherLanguage.language());
    assertThat(heading).containsText(otherLanguage.getSupportPageDescription());
    assertThat(categories.locator("vaadin-tab[selected]"))
        .hasText(otherLanguage.getSupportRecoveryCategory());
    new UserMenu(page).selectTestLanguage();
    page.reload();
    assertThat(categories.locator("vaadin-tab[selected]"))
        .hasText(texts.getSupportRecoveryCategory());
    assertThat(hint.locator("[data-lucide=mouse-pointer-click]")).isVisible();
    assertThat(ticketCard(page, ownClosedNumber)).hasCount(1);
    assertThat(ticketCard(page, ownOpenNumber)).hasCount(1);
    for (int width : new int[] {1600, 1280, 1024, 390}) {
      page.setViewportSize(width, 1000);
      if (providerTabStyles.containsKey(width)) {
        assertEquals(
            providerTabStyles.get(width),
            selectedTabStyle(categories.locator("vaadin-tab[selected]")));
      }
      Locator unassignedCard = ticketCard(page, ownOpenNumber);
      assertThat(unassignedCard.locator(".support-ticket-unassigned"))
          .hasCSS("font-style", "italic");
      assertThat(unassignedCard.locator(".support-admin-ticket-metadata .support-ticket-status"))
          .isVisible();
      assertThat(unassignedCard.locator(":scope > .support-ticket-title")).isVisible();
      assertThat(unassignedCard.locator(".support-admin-ticket-footer .support-ticket-updated"))
          .isVisible();
      page.waitForCondition(
          () ->
              (Boolean)
                  page.evaluate(
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
          "Ticketübersicht und Auswahlhinweis bei " + width + "px", page);
    }
    Locator ticketPanel = page.locator("#support-ticket-panel");
    Locator tools = ticketPanel.locator("#support-ticket-tools");
    Locator footer = ticketPanel.locator("#support-ticket-footer");
    String openNumber = verifyTicketSelectionAfterReload(categories, ticketPanel);
    verifyOpenTicketAndFocusMode(ticketPanel, tools, footer, openNumber);
    String openStatusBackground = verifyAssignedTicketLayout(ticketPanel, tools, footer);
    verifyTicketHistoryAndInternalEditor(ticketPanel, openStatusBackground);
    page.navigate("/recovery-help");
    assertThat(page.locator(".support-history-event")).hasCount(0);
    assertThat(page.locator(".support-internal-message")).hasCount(0);
    assertThat(page.locator(".support-message-copy")).not().containsText("Nur intern:");
    page.locator("#support-back").click();
    assertThat(page.locator("#account-panel-security")).isVisible();
    page.navigate("/notifications");
    assertThat(page.locator(".portal-notification-source-system").first()).isVisible();
    AllurePlaywright.attachScreenshot("Gemeinsame Systemfarbe im Benachrichtigungsbadge", page);
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
        .containsText(texts.getSupportOpen());
    assertThat(ticketPanel.locator(".support-history-event[data-event=STATUS_CHANGED]"))
        .containsText(texts.getSupportInProgress());
    assertThat(ticketPanel.locator(".support-section-header vaadin-badge"))
        .hasText(texts.getSupportMessageCount(1));
    String createdTime =
        ticketPanel.locator("#support-ticket-header .support-ticket-updated").innerText();
    Locator editor = ticketPanel.locator(".support-admin-editor");
    for (int width : new int[] {1280, 390}) {
      page.setViewportSize(width, 1000);
      assertThat(editor.locator("#support-public-reply")).hasAttribute("selected", "");
      assertThat(ticketPanel.locator("#support-reply"))
          .hasJSProperty("label", texts.getSupportReply());
      UiActions.complete(page, () -> editor.locator("#support-internal-note").click());
      assertThat(editor).hasAttribute("data-internal", "true");
      assertThat(editor.locator(".cosmos-security-warning"))
          .containsText(texts.getSupportInternalSecurityNotice());
      assertThat(editor.locator(".support-help-notice")).hasCount(0);
      assertThat(ticketPanel.locator("#support-reply"))
          .hasJSProperty("label", texts.getSupportInternal());
      assertThat(ticketPanel.locator("#support-add-reply")).hasText(texts.getSupportSaveInternal());
      assertThat(ticketPanel.locator("#support-reply textarea"))
          .hasValue("Dieser Entwurf bleibt beim Vergrößern erhalten.");
      editor.scrollIntoViewIfNeeded();
      assertTrue(
          (Boolean) page.evaluate("document.documentElement.scrollWidth <= window.innerWidth"));
      AllurePlaywright.attachScreenshot("Interner Editor und History bei " + width + "px", page);
      UiActions.complete(page, () -> editor.locator("#support-public-reply").click());
      assertThat(editor.locator(".cosmos-security-warning"))
          .containsText(texts.getSupportSecurityNotice());
    }
    UiActions.complete(page, () -> editor.locator("#support-internal-note").click());
    ticketPanel.locator("#support-reply textarea").fill("Nur intern: synthetischer Vermerk.");
    UiActions.complete(page, () -> ticketPanel.locator("#support-add-reply").click());
    assertThat(ticketPanel.locator(".support-internal-message .support-message-copy"))
        .hasText("Nur intern: synthetischer Vermerk.");
    assertThat(ticketPanel.locator(".support-section-header vaadin-badge"))
        .hasText(texts.getSupportMessageCount(2));
    assertThat(ticketPanel.locator(".support-history-event")).hasCount(2);
    assertThat(ticketPanel.locator("#support-ticket-header .support-ticket-updated"))
        .hasText(createdTime);
    Locator messageScroll = ticketPanel.locator(".support-message-scroll");
    for (int width : new int[] {1280, 390}) {
      page.setViewportSize(width, 1000);
      messageScroll.scrollIntoViewIfNeeded();
      messageScroll.evaluate("element => element.scrollTop = element.scrollHeight");
      assertTrue(
          (Boolean)
              messageScroll.evaluate("element => element.scrollWidth <= element.clientWidth"));
      assertThat(ticketPanel.locator(".support-internal-message .support-message-copy"))
          .isVisible();
      AllurePlaywright.attachScreenshot(
          "Vermerk und native Scrollleiste bei " + width + "px", page);
    }
  }

  private String verifyAssignedTicketLayout(Locator ticketPanel, Locator tools, Locator footer) {
    String openStatusBackground =
        (String)
            ticketPanel
                .locator(".support-admin-ticket-metadata > .support-ticket-status")
                .evaluate("badge => getComputedStyle(badge).backgroundColor");
    UiActions.complete(page, () -> tools.locator("#support-claim").click());
    assertThat(ticketPanel.locator("#support-ticket-header"))
        .containsText(texts.getSupportInProgress());
    assertThat(tools.locator("#support-instructions")).isVisible();
    assertThat(footer.locator("#support-add-reply")).isVisible();
    assertThat(footer.locator("#support-close")).isVisible();
    assertThat(ticketPanel.locator("#support-ticket-status-note")).not().isVisible();
    ticketPanel
        .locator("#support-reply textarea")
        .fill("Dieser Entwurf bleibt beim Vergrößern erhalten.");
    UiActions.complete(page, () -> ticketPanel.locator("#support-focus-toggle").click());
    UiActions.complete(page, () -> ticketPanel.locator("#support-focus-toggle").click());
    assertThat(ticketPanel.locator("#support-reply textarea"))
        .hasValue("Dieser Entwurf bleibt beim Vergrößern erhalten.");
    Locator requesterAvatar = ticketPanel.locator("#support-detail-requester .cosmos-user-avatar");
    assertThat(requesterAvatar.locator(".cosmos-avatar-initials")).hasCSS("cursor", "default");
    requesterAvatar.hover();
    assertThat(page.locator("vaadin-tooltip-overlay[opened] [part=overlay]"))
        .hasCSS("pointer-events", "none");
    assertThat(requesterAvatar.locator(".cosmos-avatar-initials")).hasCSS("cursor", "default");
    Locator reload = page.locator("#support-reload");
    assertThat(reload).hasCSS("cursor", "pointer");
    assertThat(reload).hasAttribute("aria-label", texts.getSessionRefresh());
    assertThat(reload.locator("[data-lucide=refresh-cw]")).isVisible();
    BoundingBox reloadSize = reload.boundingBox();
    assertEquals(reloadSize.width, reloadSize.height, 1);
    reload.hover();
    assertThat(
            page.getByRole(AriaRole.TOOLTIP)
                .filter(new Locator.FilterOptions().setHasText(texts.getSessionRefresh())))
        .hasText(texts.getSessionRefresh());
    assertThat(reload).hasCSS("cursor", "pointer");
    assertThat(ticketPanel.locator("#support-reply textarea")).hasCSS("cursor", "text");
    for (int width : new int[] {1280, 390}) {
      page.setViewportSize(width, 1000);
      page.waitForCondition(
          () ->
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
          "Ticket-Panelkopf und Werkzeugleiste bei " + width + "px", page);
      footer.scrollIntoViewIfNeeded();
      AllurePlaywright.attachScreenshot("Ticket-Aktionsfooter bei " + width + "px", page);
      page.locator("#support-instructions").click();
      Locator preview = page.locator("#support-instruction-preview");
      assertThat(preview).hasJSProperty("opened", true);
      page.waitForCondition(
          () ->
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
      page.locator("#support-instruction-preview-cancel").click();
      assertThat(preview).hasJSProperty("opened", false);
    }
    return openStatusBackground;
  }

  private void verifyOpenTicketAndFocusMode(
      Locator ticketPanel, Locator tools, Locator footer, String openNumber) {
    AllurePlaywright.attachScreenshot("Offenes Ticket mit wiederherstellbarer URL", page);
    assertThat(ticketPanel.locator("#support-ticket-header"))
        .containsText(texts.getSupportRecoverySubject());
    assertThat(tools.locator("#support-claim")).isVisible();
    assertThat(tools.locator("#support-claim")).hasAttribute("theme", "primary");
    assertThat(ticketPanel.locator(".support-admin-ticket-metadata > .support-ticket-status"))
        .hasCSS("margin-inline-start", "0px");
    assertThat(footer).not().isVisible();
    assertThat(ticketPanel.locator("#support-ticket-status-note")).isVisible();
    assertThat(ticketPanel.locator(".support-admin-policy-label"))
        .hasText(texts.getSupportSecurityPolicy());
    assertThat(tools.locator("vaadin-switch.cosmos-config-switch")).isVisible();
    for (int width : new int[] {1280, 390}) {
      page.setViewportSize(width, 500);
      Locator queueScroll = page.locator(".support-admin-ticket-scroll");
      queueScroll.evaluate("scroller => scroller.scrollTop = scroller.scrollHeight");
      page.waitForCondition(
          () ->
              ((Number) queueScroll.evaluate("scroller => scroller.scrollTop")).doubleValue() > 0);
      double scrollPosition =
          ((Number) queueScroll.evaluate("scroller => scroller.scrollTop")).doubleValue();
      Locator focus = ticketPanel.locator("#support-focus-toggle");
      UiActions.complete(page, focus::click);
      UiActions.complete(page, focus::click);
      page.waitForCondition(
          () ->
              Math.abs(
                      ((Number) queueScroll.evaluate("scroller => scroller.scrollTop"))
                              .doubleValue()
                          - scrollPosition)
                  < 2);
      page.setViewportSize(width, 1000);
      UiActions.complete(page, focus::click);
      assertThat(page.locator(".support-admin-queue")).not().isVisible();
      assertThat(focus).hasAttribute("aria-label", texts.getSupportRestoreSplitView());
      assertThat(focus.locator("[data-lucide=minimize]")).isVisible();
      assertTicketLocation("all", openNumber);
      page.waitForCondition(
          () ->
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
      AllurePlaywright.attachScreenshot("Ticket-Fokusansicht bei " + width + "px", page);
      UiActions.complete(page, focus::click);
      assertThat(page.locator(".support-admin-queue")).isVisible();
      assertThat(focus.locator("[data-lucide=expand]")).isVisible();
      assertThat(
              page.locator(
                      ".support-admin-ticket-list vaadin-grid-cell-content .support-ticket-number")
                  .first())
          .hasText("#" + openNumber);
    }
  }

  private String verifyTicketSelectionAfterReload(Locator categories, Locator ticketPanel) {
    page.setViewportSize(1280, 1000);
    String openNumber =
        page.locator(".support-admin-ticket-card .support-ticket-number")
            .first()
            .innerText()
            .substring(1);
    UiActions.complete(page, () -> page.locator(".support-admin-ticket-card").first().click());
    assertTicketLocation("recovery", openNumber);
    page.reload();
    assertThat(ticketPanel.locator(".support-ticket-number"))
        .hasText(texts.getSupportTicketNumber("#" + openNumber));
    assertThat(categories.locator("vaadin-tab[selected]"))
        .hasText(texts.getSupportRecoveryCategory());
    UiActions.complete(page, () -> categories.locator("vaadin-tab").first().click());
    assertTicketLocation("all", openNumber);
    page.reload();
    assertThat(categories.locator("vaadin-tab[selected]")).hasText(texts.getSupportAllTickets());
    assertThat(ticketPanel.locator(".support-ticket-number"))
        .hasText(texts.getSupportTicketNumber("#" + openNumber));
    String closedNumber =
        page.locator(".support-admin-ticket-card .support-ticket-number")
            .nth(1)
            .innerText()
            .substring(1);
    UiActions.complete(page, () -> page.locator(".support-admin-ticket-card").nth(1).click());
    assertTicketLocation("all", closedNumber);
    page.reload();
    assertThat(ticketPanel.locator(".support-ticket-number"))
        .hasText(texts.getSupportTicketNumber("#" + closedNumber));
    UiActions.complete(page, () -> page.locator(".support-admin-ticket-card").first().click());
    assertTicketLocation("all", openNumber);
    return openNumber;
  }

  private void assertTicketLocation(String tab, String number) {
    page.waitForCondition(
        () ->
            (Boolean)
                page.evaluate(
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
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    String email = "intro-mfa-" + UUID.randomUUID() + "@example.test";
    new AdminUsersPage(page).createUser(email, BrowserTestAccounts.USER_PASSWORD, true, true);
    try (BrowserActor actor = new BrowserActor(browser)) {
      actor.page.setViewportSize(width, 900);
      LoginPage fresh = new LoginPage(actor.page);
      fresh.open();
      fresh.signInFirstFactor(email, BrowserTestAccounts.USER_PASSWORD);
      new UserMenu(actor.page).selectTestLanguage();
      assertThat(actor.page.locator("#two-factor-introduction-title"))
          .hasText(texts.getTwoFactorRequiredTitle());
      assertThat(actor.page.locator("#two-factor-password")).hasCount(0);
      assertThat(actor.page.locator("#two-factor-submit")).hasText(texts.getTwoFactorBeginSetup());
      assertThat(actor.page.locator("#two-factor-cancel")).hasText(texts.getBaseLogoutButton());
      assertTrue(
          (Boolean)
              actor
                  .page
                  .locator(".two-factor-introduction-art")
                  .evaluate("image => image.complete && image.naturalWidth > 0"));
      assertTrue(
          (Boolean)
              actor.page.evaluate("document.documentElement.scrollWidth <= window.innerWidth"));
      AllurePlaywright.attachScreenshot(
          "Vaadin: verpflichtender Einstieg A bei " + width + "px", actor.page);
      actor.page.locator("#two-factor-cancel").click();
      assertThat(actor.page).hasURL(Pattern.compile(".*/login$"));
      actor.page.navigate("/home");
      assertThat(actor.page.locator(".public-header")).isVisible();
      actor.page.navigate("/account");
      assertThat(actor.page).hasURL(Pattern.compile(".*/login.*"));
      new UserMenu(actor.page).selectTestLanguage();
      fresh.assertFormVisible();
      fresh.signInFirstFactor(email, BrowserTestAccounts.USER_PASSWORD);
      actor.page.reload();
      assertThat(actor.page.locator("#two-factor-introduction-title")).isVisible();
      actor.page.navigate("/admin");
      assertThat(actor.page).hasURL(Pattern.compile(".*/two-factor.*"));
      actor.page.locator("#two-factor-submit").click();
      assertThat(actor.page.locator("#two-factor-qr")).isVisible();
      assertThat(actor.page.locator("#two-factor-password")).hasCount(0);
      assertEnrollmentControls(actor.page);
      TwoFactorPage.enrollCurrent(actor.page, email);
      assertThat(actor.page).hasURL(Pattern.compile(".*/admin$"));
    }
  }

  private void assertEnrollmentControls(Page enrollment) {
    assertThat(enrollment.locator(".two-factor-method-tabs")).hasCSS("height", "42px");
    assertThat(enrollment.locator(".two-factor-app-links a")).hasCount(3);
    assertThat(enrollment.locator(".two-factor-app-compatibility"))
        .hasText(texts.getTwoFactorAppCompatibility());
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
                .filter(new Locator.FilterOptions().setHasText(texts.getTwoFactorManualCopied())))
        .isVisible();
    org.junit.jupiter.api.Assertions.assertEquals(
        formattedKey.replace(" ", "").replace("\n", ""),
        enrollment.evaluate("navigator.clipboard.readText()"));
    TwoFactorPage.assertCopyFailure(
        enrollment, manual.locator("#two-factor-copy-key"), texts.getTwoFactorManualCopyFailed());
    assertThat(manualInput).hasValue(formattedKey);
    assertThat(manual).hasJSProperty("value", "");
    UiTexts other = new UiTexts(texts.language().equals("de") ? "en" : "de");
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
                  .filter(new Locator.FilterOptions().setHasText(texts.getTwoFactorInvalidCode())))
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
                        .setHasText(texts.getBaseValidationRequiredMessage())))
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
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    String email = "recover-mfa-" + UUID.randomUUID() + "@example.test";
    new AdminUsersPage(page).createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
    new UserMenu(page).signOut();
    page.setViewportSize(width, 900);
    login.signIn(email, BrowserTestAccounts.USER_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    page.navigate("/account?section=security&tab=portal");
    page.locator("#account-two-factor-setup").click();
    page.locator("#two-factor-password input").fill(BrowserTestAccounts.USER_PASSWORD);
    page.locator("#two-factor-submit").click();
    TwoFactorPage.enrollCurrent(page, email);
    assertThat(page).hasURL(Pattern.compile(".*/account\\?section=security&tab=portal$"));
    new UserMenu(page).selectTestLanguage();
    if (!provider.equals("password")) {
      page.locator("#account-security-tab-providers").click();
      page.locator("#external-connect-" + provider).click();
      page.locator("#external-confirm-factor input")
          .pressSequentially(TwoFactorPage.totp(page, email));
      providerIdentity(email);
      assertThat(page.locator("#external-connect-" + provider))
          .hasText(texts.getExternalDisconnect(provider.equals("google") ? "Google" : "Microsoft"));
      page.navigate("/account?section=security&tab=portal");
    }
    // Echte Bestätigungen ohne Kontoänderung verbrauchen alle bis auf den letzten ausgegebenen
    // Code.
    int confirmations = TwoFactorPage.remainingRecoveryCodes(email) - 1;
    for (int index = 0; index < confirmations; index++) {
      // Unterschiedliche Zwecke erfordern jeweils einen neuen Nachweis statt einer offenen
      // Freigabe.
      String action = index % 2 == 0 ? "password" : "email";
      page.locator("#account-" + action + "-edit-security").click();
      page.locator("#external-proof-recovery-code-tab").click();
      page.locator("#external-confirm-recovery-code input")
          .fill(TwoFactorPage.nextCode(page, email));
      page.locator("#external-confirm-local").click();
      assertThat(page.locator("#account-" + action + "-new")).isVisible();
      page.locator("#account-" + action + "-cancel").click();
      assertThat(page.locator("#account-" + action + "-new")).isHidden();
    }
    String last = TwoFactorPage.nextCode(page, email);
    new UserMenu(page).requestSignOut();
    assertThat(page.locator(".public-header")).isVisible();
    signInUntilSecondFactor(email, provider);
    TwoFactorPage.openLoginHelp(page);
    page.locator("#two-factor-recover").click();
    assertThat(page.locator("#two-factor-panel .cosmos-panel-header"))
        .containsText(texts.getTwoFactorRecoverSubtitle());
    TwoFactorPage.enterCode(page, last);
    page.locator("#two-factor-submit").click();
    assertThat(page.locator("#two-factor-qr")).isVisible();
    try (Page blocked = context.newPage()) {
      blocked.navigate("/account?tab=profile");
      assertThat(blocked).hasURL(Pattern.compile(".*/two-factor.*"));
      assertThat(blocked.locator("#two-factor-qr")).isVisible();
      assertThat(blocked.locator("#account-panel-profile")).hasCount(0);
    }
    page.reload();
    TwoFactorPage.enrollCurrent(page, email);
    assertThat(page).hasURL(Pattern.compile(".*/account\\?section=security&tab=portal$"));
    assertThat(page.locator("#account-two-factor")).containsText(texts.getTwoFactorEnrolled());
    AllurePlaywright.attachScreenshot("Wiederhergestellter Authenticator mit neuem Codesatz", page);
    new UserMenu(page).requestSignOut();
    assertThat(page.locator(".public-header")).isVisible();
    signInUntilSecondFactor(email, provider);
    TwoFactorPage.enterCode(page, last);
    page.locator("#two-factor-submit").click();
    assertThat(page.locator("#two-factor-code")).hasAttribute("invalid", "");
    assertThat(page.locator("vaadin-notification-card:not([closing])"))
        .containsText(texts.getTwoFactorInvalidCode());
    String nextCode = TwoFactorPage.nextCode(page, email);
    TwoFactorPage.enterCode(page, nextCode);
    if (nextCode.length() > 6) {
      page.locator("#two-factor-submit").click();
    }
    assertThat(page).hasURL(Pattern.compile(".*/home$"));
  }

  private void signInUntilSecondFactor(String email, String provider) {
    LoginPage login = new LoginPage(page);
    login.open();
    if (provider.equals("password")) {
      login.signInFirstFactor(email, BrowserTestAccounts.USER_PASSWORD);
    } else {
      page.locator("#external-login-" + provider).click();
      providerIdentity(email);
    }
    assertThat(page).hasURL(Pattern.compile(".*/two-factor.*"));
    TwoFactorPage.selectTestLanguageIfNeeded(page);
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName(
      "MFA-11: Freiwilliger Authenticator wird nur mit eigenem Faktor gewechselt und entfernt")
  @Tag("follow-up-regression")
  void ownAuthenticatorCanBeReplacedAndRemovedWithIndependentProof(int width) {
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    String email = "manage-mfa-" + UUID.randomUUID() + "@example.test";
    new AdminUsersPage(page).createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
    try (BrowserActor user = new BrowserActor(browser)) {
      user.page.setViewportSize(width, 900);
      user.login(email, BrowserTestAccounts.USER_PASSWORD);
      user.page.navigate("/account?section=security&tab=portal");
      user.page.locator("#account-two-factor-setup").click();
      user.page.locator("#two-factor-password input").fill(BrowserTestAccounts.USER_PASSWORD);
      user.page.locator("#two-factor-submit").click();
      TwoFactorPage.enrollCurrent(user.page, email);
      assertThat(user.page).hasURL(Pattern.compile(".*/account\\?section=security&tab=portal$"));
      new UserMenu(user.page).selectTestLanguage();
      user.page.locator("#account-two-factor-replace").click();
      Locator actionOtp = user.page.locator("#two-factor-action-factor");
      assertThat(actionOtp.locator("input[slot=input]")).isFocused();
      assertThat(actionOtp.locator("[part=slot]").first())
          .hasCSS("font-family", Pattern.compile(".*Space Mono.*"));
      TwoFactorPage.enterActionCode(user.page, TwoFactorPage.nextCode(user.page, email));
      assertThat(user.page.locator("#two-factor-action-recovery-code input"))
          .hasCSS("font-family", Pattern.compile(".*Space Mono.*"));
      user.page.locator("#two-factor-action-recovery-code input").press("Tab");
      assertThat(user.page.locator("#two-factor-action-confirm")).isVisible();
      assertThat(user.page).hasURL(Pattern.compile(".*/account\\?section=security&tab=portal$"));
      user.page.locator("#two-factor-action-confirm").click();
      TwoFactorPage.enrollCurrent(user.page, email);
      assertThat(user.page).hasURL(Pattern.compile(".*/account\\?section=security&tab=portal$"));
      user.page.locator("#account-two-factor-remove").click();
      TwoFactorPage.enterActionCode(user.page, TwoFactorPage.nextCode(user.page, email));
      user.page.locator("#two-factor-action-confirm").click();
      assertThat(user.page.locator("#account-two-factor-setup")).isVisible();
      assertThat(user.page.locator("#account-two-factor vaadin-badge"))
          .hasText(texts.getTwoFactorNotEnrolled());
      assertThat(user.page.locator("vaadin-notification-card:not([closing])"))
          .containsText(texts.getTwoFactorDisabled());
      AllurePlaywright.attachScreenshot(
          "Freiwilliger Faktor nach geschütztem Wechsel und Entfernen", user.page);
      new UserMenu(user.page).signOut();
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName(
      "MFA-09: Neue Pflichtregel schützt ein bereits angemeldetes Konto und bleibt konfigurierbar")
  void policyChangesProtectAnExistingSession(int width) {
    page.setViewportSize(width, 900);
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    String email = "policy-mfa-" + UUID.randomUUID() + "@example.test";
    new AdminUsersPage(page).createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
    try (BrowserActor user = new BrowserActor(browser)) {
      user.login(email, BrowserTestAccounts.USER_PASSWORD);
      try {
        changeUserLoginRule(texts.getTwoFactorModeRequired());
        user.page.navigate("/account?tab=profile");
        assertThat(user.page).hasURL(Pattern.compile(".*/two-factor.*"));
        assertThat(user.page.locator("#account-first-name")).hasCount(0);
        TwoFactorPage.completeIfRequired(user.page, email, BrowserTestAccounts.USER_PASSWORD);
        user.page.navigate("/account?section=security&tab=portal");
        assertThat(user.page.locator("#account-two-factor")).isVisible();
        AllurePlaywright.attachScreenshot(
            "Bestehende Sitzung nach neu vorgeschriebener Einrichtung", user.page);
      } finally {
        changeUserLoginRule(texts.getTwoFactorModeEnrolled());
        user.page.navigate("/home");
        TwoFactorPage.completeIfRequired(user.page, email, BrowserTestAccounts.USER_PASSWORD);
        new UserMenu(user.page).requestSignOut();
        assertThat(user.page.locator(".public-header")).isVisible();
      }
    }
  }

  private void changeUserLoginRule(String label) {
    page.navigate("/admin?section=configuration&group=two_factor");
    TwoFactorPage.completeIfRequired(
        page, BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    page.navigate("/admin?section=configuration&group=two_factor");
    new UserMenu(page).selectTestLanguage();
    page.locator("#two-factor-role-tabs vaadin-tab")
        .getByText(texts.getRoleUser(), new Locator.GetByTextOptions().setExact(true))
        .click();
    page.locator("#two-factor-rule-role_user-login [part~='toggle-button']").click();
    page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(label).setExact(true))
        .click();
    page.locator("#two-factor-save").click();
    TwoFactorPage.enterActionCode(
        page, TwoFactorPage.nextCode(page, BrowserTestAccounts.SUPERADMIN_EMAIL));
    UiActions.complete(page, () -> page.locator("#two-factor-action-confirm").click());
    page.waitForCondition(
        () ->
            java.net.URI.create(page.url()).getPath().equals("/two-factor")
                || page.locator("vaadin-notification-card:not([closing])")
                    .filter(new Locator.FilterOptions().setHasText(texts.getTwoFactorSaved()))
                    .isVisible());
    // Die alte Ansicht darf parallel bereits den neuen Regelstand erkennen und weiterleiten.
    // Ein neuer Tab prüft die neue Anforderung ohne konkurrierende Navigation dieser Ansicht.
    try (Page fresh = context.newPage()) {
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
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    page.navigate("/account?section=security&tab=portal");
    try (Page second = context.newPage()) {
      second.navigate("/account?section=security&tab=portal");
      assertThat(second.locator("#account-two-factor")).isVisible();
      BrowserTestRunner.restartTestApplication();
      page.reload();
      second.reload();
      assertThat(page).hasURL(Pattern.compile(".*/account\\?section=security&tab=portal$"));
      assertThat(second).hasURL(Pattern.compile(".*/account\\?section=security&tab=portal$"));
      assertThat(page.locator("#account-two-factor")).isVisible();
      assertThat(second.locator("#account-two-factor")).isVisible();
      assertThat(page.locator("#two-factor-code")).hasCount(0);
    }
  }

  @Test
  @DisplayName("MFA-08: Echter Browserneustart mit Remember-me verlangt einen neuen zweiten Faktor")
  void rememberedFirstFactorDoesNotRestoreThePreviousSecondFactor() {
    LoginPage administrator = new LoginPage(page);
    administrator.open();
    administrator.signIn(
        BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    String email = "remember-mfa-" + UUID.randomUUID() + "@example.test";
    new AdminUsersPage(page).createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
    try (PersistentBrowserActor actor = new PersistentBrowserActor(playwright, profile)) {
      LoginPage login = new LoginPage(actor.page);
      login.open();
      actor
          .page
          .getByRole(
              AriaRole.SWITCH,
              new Page.GetByRoleOptions()
                  .setName(texts.getLoginRememberMeCheckbox())
                  .setExact(true))
          .setChecked(true);
      login.signIn(email, BrowserTestAccounts.USER_PASSWORD);
      actor.page.navigate("/account?section=security&tab=portal");
      actor.page.locator("#account-two-factor-setup").click();
      actor.page.locator("#two-factor-password input").fill(BrowserTestAccounts.USER_PASSWORD);
      actor.page.locator("#two-factor-submit").click();
      TwoFactorPage.enrollCurrent(actor.page, email);
      assertThat(actor.page).hasURL(Pattern.compile(".*/account\\?section=security&tab=portal$"));
      new UserMenu(actor.page).selectTestLanguage();
    }
    try (PersistentBrowserActor actor =
        new PersistentBrowserActor(playwright, profile, "two-factor")) {
      assertThat(actor.page.locator("#two-factor-code")).isVisible();
      assertThat(actor.page.locator("#admin-sidebar")).hasCount(0);
      TwoFactorPage.completeIfRequired(actor.page, email, BrowserTestAccounts.USER_PASSWORD);
      actor.page.navigate("/account?tab=sessions");
      actor.page.locator("#account-logout-others").click();
      actor.page.locator("#sessions-confirmation-submit").click();
      new UserMenu(actor.page).signOut();
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
    page.setViewportSize(width, 900);
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    String email = "support-mfa-" + UUID.randomUUID() + "@example.test";
    new AdminUsersPage(page).createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
    new UserMenu(page).signOut();
    login.signIn(email, BrowserTestAccounts.USER_PASSWORD);
    page.navigate("/account?section=security&tab=portal");
    page.locator("#account-two-factor-setup").click();
    page.locator("#two-factor-password input").fill(BrowserTestAccounts.USER_PASSWORD);
    page.locator("#two-factor-submit").click();
    TwoFactorPage.enrollCurrent(page, email);
    assertThat(page).hasURL(Pattern.compile(".*/account\\?section=security&tab=portal$"));
    new UserMenu(page).selectTestLanguage();
    new UserMenu(page).signOut();
    login.signInFirstFactor(email, BrowserTestAccounts.USER_PASSWORD);
    TwoFactorPage.openLoginHelp(page);
    page.locator("#two-factor-help").click();
    assertThat(page.locator("#account-support")).isVisible();
    page.locator("#support-reply textarea")
        .fill("Synthetische Bitte um Hilfe mit meinen Ersatzcodes.");
    UiActions.complete(page, () -> page.locator("#support-create").click());
    assertThat(page.locator("#support-request-count")).hasText(texts.getSupportRequestCount(1));
    String ticketNumber =
        page.locator("#support-ticket-grid")
            .getByText(Pattern.compile("^#[0-9]+$"))
            .first()
            .innerText();
    try (BrowserActor administrator = new BrowserActor(browser)) {
      administrator.login(
          BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
      Page admin = administrator.page;
      new UserMenu(admin).selectTestLanguage();
      admin.setViewportSize(width, 900);
      admin.navigate("/admin?section=support");
      Locator ticketCategories = admin.locator("#support-ticket-categories");
      assertThat(ticketCategories).containsText(texts.getSupportAllTickets());
      UiActions.complete(
          admin, () -> ticketCategories.getByText(texts.getSupportRecoveryCategory()).click());
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
      assertThat(page.locator("#support-ticket-grid")).containsText(texts.getSupportInProgress());
      assertThat(admin.locator("#admin-support .cosmos-security-warning")).isVisible();
      assertThat(admin.locator("#support-add-reply")).isVisible();
      String draft = "Eine Antwort bleibt beim Aktualisieren erhalten.";
      String followUp = "Eine synthetische Ergänzung zur Anfrage.";
      String privateNote = "Synthetischer interner Vermerk.";
      String publicReply = "Eine sichtbare Rückmeldung aus der Administration.";
      Locator adminComposer = admin.locator("#support-reply textarea");
      adminComposer.fill(draft);
      page.locator("#support-reply textarea").fill(followUp);
      UiActions.complete(page, () -> page.locator("#support-add-reply").click());
      assertThat(admin.locator(".support-message-thread")).containsText(followUp);
      assertThat(adminComposer).hasValue(draft);
      adminComposer.fill(privateNote);
      admin.locator("#support-internal-note").click();
      Locator requesterThread = page.locator("#account-support .support-message-thread");
      String requesterCountBeforeNote = page.locator("#support-request-count").innerText();
      Locator requesterMessageCount =
          page.locator("#account-support .support-section-count")
              .filter(new Locator.FilterOptions().setHasText(texts.getSupportMessageCount(2)));
      assertThat(requesterMessageCount).isVisible();
      UiActions.complete(admin, () -> admin.locator("#support-add-reply").click());
      assertThat(admin.locator(".support-internal-message")).containsText(privateNote);
      adminComposer.fill(publicReply);
      UiActions.complete(admin, () -> admin.locator("#support-add-reply").click());
      assertThat(page.locator(".support-message-thread")).containsText(publicReply);
      assertThat(page.locator(".support-message-thread")).not().containsText(privateNote);
      assertThat(page.locator("#support-request-count")).hasText(requesterCountBeforeNote);
      assertThat(
              page.getByText(
                  texts.getSupportMessageCount(3), new Page.GetByTextOptions().setExact(true)))
          .isVisible();
      assertThat(requesterThread).not().containsText(BrowserTestAccounts.SUPERADMIN_EMAIL);
      assertThat(requesterThread).not().containsText(assigneeName);
      assertThat(page.locator("#account-support .support-message-sender"))
          .hasText(texts.getSupportMessageSupport());
      AllurePlaywright.attachScreenshot("Admin-Tickets mit ausgewählter Anfrage", admin);
      admin.locator("#support-instructions").click();
      assertThat(admin.locator("#support-instruction-preview")).hasJSProperty("opened", true);
      assertThat(admin.locator("#support-instruction-preview"))
          .containsText(texts.getSupportRecoveryInstructions().lines().findFirst().orElseThrow());
      assertThat(admin.locator("#support-instruction-preview .support-instruction-step"))
          .hasCount(4);
      assertThat(admin.locator("#support-instruction-preview .cosmos-security-warning"))
          .isVisible();
      admin.evaluate(
          "async () => { await Promise.all(document.getAnimations().filter(animation => animation.effect?.getTiming().iterations !== Infinity).map(animation => animation.finished.catch(() => {}))); }");
      if (width == 390) {
        admin.setViewportSize(width, 1200);
      }
      AllurePlaywright.attachScreenshot("Geprüfte Hilfeantwort ohne neue Zugangscodes", admin);
      admin.setViewportSize(width, 900);
      admin.locator("#support-instruction-preview-submit").click();
      assertThat(admin.locator("vaadin-notification-card:not([closing])"))
          .containsText(texts.getSupportQueued());
      new MailpitInbox(administrator.context)
          .awaitSecurityMail(email, texts.getSupportRecoverySubject());
      Locator guide = page.locator(".support-instruction-message");
      assertThat(guide).containsText(texts.getSupportInstructionMessageTitle());
      assertThat(guide.locator(".support-instruction-details")).not().hasAttribute("opened", "");
      guide.evaluate("element => element.scrollIntoView({block: 'nearest'})");
      AllurePlaywright.attachScreenshot("Anleitung kompakt im Nachrichtenverlauf", page);
      UiActions.complete(page, () -> guide.locator(".support-instruction-toggle").click());
      assertThat(guide.locator(".support-instruction-details")).hasAttribute("opened", "");
      assertThat(guide.locator(".support-instruction-step")).hasCount(4);
      assertThat(guide.locator(".cosmos-security-warning")).isVisible();
      if (width == 390) {
        page.setViewportSize(width, 650);
        assertTouchScroll(page, page.locator("html"));
        page.setViewportSize(width, 900);
        Locator thread = page.locator("#account-support .support-message-scroll");
        thread.scrollIntoViewIfNeeded();
        assertTouchScroll(page, thread);
      }
      UiActions.complete(page, () -> page.locator("#support-reload").click());
      assertThat(guide.locator(".support-instruction-details")).hasAttribute("opened", "");
      guide.evaluate("element => element.scrollIntoView({block: 'nearest'})");
      AllurePlaywright.attachScreenshot("Anleitung aufgeklappt im Nachrichtenverlauf", page);
      page.locator("#support-reply textarea").fill("Dieser Entwurf gehört nur zur alten Anfrage.");
      UiActions.complete(admin, () -> admin.locator("#support-close").click());
      assertThat(page.locator("#support-ticket-grid")).containsText(texts.getSupportClosed());
      assertThat(page.locator("#support-create")).isVisible();
      assertThat(page.locator("#support-reply textarea")).hasValue("");
      UiActions.complete(page, () -> page.locator("#support-reload").click());
      assertThat(page.locator("vaadin-notification-card:not([closing])"))
          .containsText(texts.getSupportRefreshed());
      page.locator("#support-reply textarea").fill("Eine zweite isolierte Anfrage.");
      UiActions.complete(page, () -> page.locator("#support-create").click());
      assertThat(page.locator("#support-request-count")).hasText(texts.getSupportRequestCount(2));
      UiActions.complete(page, () -> page.locator("#support-close").click());
      new UserMenu(admin).signOut();
    }
    page.navigate("/admin");
    assertThat(page).hasURL(Pattern.compile(".*/recovery-help$"));
    page.locator("#support-sign-out").click();
    new LoginPage(page).assertFormVisible();
    login.signIn(email, BrowserTestAccounts.USER_PASSWORD);
    page.navigate("/admin?section=support&ticket=" + ticketNumber.substring(1));
    assertThat(page.locator("#support-ticket-panel, #admin-nav-support")).hasCount(0);
  }

  @ParameterizedTest
  @CsvSource({"google,1280", "google,390", "microsoft,1280", "microsoft,390"})
  @DisplayName(
      "MFA-05: Externes Konto richtet 2FA sicher ein, bestätigt neu und verbindet einen zweiten Anbieter")
  void externalAccountEnrollmentAndIndependentSensitiveProof(String provider, int width) {
    page.setViewportSize(width, 900);
    String identity = "mfa-" + UUID.randomUUID() + "@gmail.com";
    page.navigate("/login");
    page.locator("#external-login-" + provider).click();
    providerIdentity(identity);
    page.locator("#external-register-first input").fill("Browser");
    page.locator("#external-register-last input").fill("Testuser");
    page.locator("#external-register-submit").click();
    if (provider.equals("microsoft")) {
      MailpitInbox inbox = new MailpitInbox(context);
      try (Page activation = inbox.openVerificationLink(inbox.awaitVerificationMail(identity))) {
        assertThat(activation.locator(".public-panel")).isVisible();
      }
      page.navigate("/login");
      page.locator("#external-login-" + provider).click();
      providerIdentity(identity);
    }
    assertThat(page).hasURL(Pattern.compile(".*/home$"));
    page.navigate("/account?section=security&tab=portal");
    page.locator("#account-two-factor-setup").click();
    page.locator("#two-factor-confirm-" + provider).click();
    providerIdentity(identity);
    if (provider.equals("google")) {
      assertThat(page.locator("#two-factor-code")).isVisible();
      page.locator("#two-factor-code input")
          .fill(new MailpitInbox(context).awaitAuthenticatorSetupCode(identity));
      page.locator("#two-factor-submit").click();
    }
    TwoFactorPage.enrollCurrent(page, identity);
    assertThat(page).hasURL(Pattern.compile(".*/account\\?section=security&tab=portal$"));
    new UserMenu(page).signOut();
    page.locator("#external-login-" + provider).click();
    providerIdentity(identity);
    assertThat(page).hasURL(Pattern.compile(".*/two-factor$"));
    TwoFactorPage.completeIfRequired(page, identity, null);
    assertThat(page).hasURL(Pattern.compile(".*/home$"));
    page.navigate("/account?section=security&tab=portal");
    new UserMenu(page).selectTestLanguage();
    page.locator("#account-password-edit-security").click();
    assertThat(page.locator("#external-confirm-password")).isHidden();
    assertThat(page.locator("#external-confirm-" + provider)).hasCount(0);
    page.locator("#external-confirm-factor input").fill("123");
    page.locator("#external-confirm-local").click();
    assertThat(page.locator("#external-confirm-factor")).hasJSProperty("invalid", true);
    assertThat(page.locator("vaadin-notification-card:not([closing])"))
        .containsText(texts.getBaseValidationRequiredMessage());
    assertThat(page.locator("vaadin-notification-card:not([closing])"))
        .hasAttribute("theme", Pattern.compile(".*cosmos-reference-error.*"));
    page.locator("vaadin-notification-card:not([closing])").getByRole(AriaRole.BUTTON).click();
    page.locator("#external-confirm-factor input").fill(TwoFactorPage.totp(page, identity));
    page.locator("#external-confirm-local").click();
    page.locator("#account-password-new input").fill("OwnOptionalPortal1!");
    page.locator("#account-password-confirm input").fill("OwnOptionalPortal1!");
    page.locator("#account-password-save").click();
    assertThat(page.locator("vaadin-notification-card:not([closing])"))
        .containsText(texts.getAccountSecurityPasswordSuccess());
    page.locator("#account-security-tab-providers").click();
    String second = provider.equals("google") ? "microsoft" : "google";
    page.locator("#external-connect-" + second).click();
    assertThat(page.locator("#external-confirm-factor input")).isFocused();
    context.grantPermissions(List.of("clipboard-read", "clipboard-write"));
    pasteOtp(page, TwoFactorPage.totp(page, identity));
    providerIdentity("linked-" + UUID.randomUUID() + "@gmail.com");
    assertThat(page.locator("#external-connect-" + second))
        .hasText(texts.getExternalDisconnect(second.equals("google") ? "Google" : "Microsoft"));
    AllurePlaywright.attachScreenshot("Eigenständige 2FA bei verbundenen Anmeldeanbietern", page);
  }

  private void providerIdentity(String identity) {
    assertThat(page.locator("#fixture-email")).isVisible();
    page.locator("#fixture-email").fill(identity);
    page.locator("#fixture-mode").selectOption("valid");
    page.locator("#fixture-signin").click();
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName(
      "MFA-01: Authenticator einrichten und dieselbe Sitzung bei Refresh und weiterem Tab erhalten")
  void requiredAuthenticatorSetupAndSessionContinuity(int width) {
    page.setViewportSize(width, 900);
    LoginPage login = new LoginPage(page);
    login.open();
    login.signInFirstFactor(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    assertThat(page).hasURL(Pattern.compile(".*/two-factor$"));
    assertThat(page.locator("#two-factor-panel")).isVisible();
    if (page.locator("#two-factor-password").isVisible()) {
      page.locator("#two-factor-password input").fill("WrongSetupPassword!");
      page.locator("#two-factor-submit").click();
      assertThat(page.locator("#two-factor-password")).hasAttribute("invalid", "");
      assertThat(page.locator("vaadin-notification-card:not([closing])"))
          .containsText(texts.getAccountSecurityWrongPassword());
    }
    AllurePlaywright.attachScreenshot("Eingeschränkter Zwei-Faktor-Zugang", page);
    TwoFactorPage.completeIfRequired(
        page, BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    assertThat(page).hasURL(Pattern.compile(".*/admin$"));
    page.reload();
    assertThat(page.locator("#admin-sidebar")).isAttached();
    try (Page tab = context.newPage()) {
      tab.navigate("/account?section=security&tab=portal");
      new UserMenu(tab).selectTestLanguage();
      assertThat(tab.locator("#account-two-factor")).containsText(texts.getTwoFactorEnrolled());
      assertThat(tab).hasURL(Pattern.compile(".*/account\\?section=security&tab=portal$"));
    }
    page.navigate("/account?section=security&tab=portal");
    new UserMenu(page).selectTestLanguage();
    assertThat(page.locator("#account-two-factor")).containsText(texts.getTwoFactorEnrolled());
    AllurePlaywright.attachScreenshot("Eigene Zwei-Faktor-Verwaltung", page);
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName("MFA-02: Neue Ersatzcodes nach eigener Bestätigung und geschütztes PDF")
  @Tag("recovery-output")
  void protectedRecoveryCodeIssuance(int width) throws Exception {
    page.setViewportSize(width, 900);
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    page.navigate("/account?section=security&tab=portal");
    page.locator("#account-two-factor-renew").click();
    assertAccountActionCodeLayout();
    page.locator("#two-factor-action-help").click();
    assertThat(page.locator("#account-support")).isVisible();
    page.locator("#support-back").click();
    assertThat(page.locator("#account-two-factor-renew")).isVisible();
    assertThat(page.locator("#two-factor-action-confirm")).isHidden();
    page.locator("#account-two-factor-renew").click();
    TwoFactorPage.enterActionCode(
        page, TwoFactorPage.nextCode(page, BrowserTestAccounts.ADMIN_EMAIL));
    try (Page previous = context.newPage()) {
      previous.navigate("/account?section=security&tab=portal");
      Locator oldRenew = previous.locator("#account-two-factor-renew");
      assertThat(oldRenew).isVisible();
      page.locator("#two-factor-action-confirm").click();
      assertThat(page.locator("#two-factor-recovery-toggle")).isVisible();
      oldRenew.click();
      assertThat(previous).hasURL(page.url());
      assertThat(previous.locator("#two-factor-recovery-toggle")).isVisible();
    }
    java.time.Instant shortProofExpired = java.time.Instant.now().plusSeconds(301);
    assertThat(page.locator("#two-factor-recovery-codes")).isVisible();
    page.locator("#two-factor-recovery-toggle").click();
    assertThat(page.locator("#two-factor-recovery-codes")).isHidden();
    assertThat(page.locator("#two-factor-submit")).isDisabled();
    assertThat(page.locator("#two-factor-cancel")).isHidden();
    String outputPage = page.url();
    assertThat(page.locator(".public-header a.public-brand")).hasCount(0);
    assertThat(page.locator("#public-nav-home, #public-nav-login, #public-nav-register"))
        .hasCount(0);
    assertThat(page.locator(".public-languages vaadin-button")).hasCount(2);
    try (Page second = context.newPage()) {
      for (String path : List.of("/home", "/admin", "/account", "/recovery-help")) {
        second.navigate(path);
        assertThat(second).hasURL(outputPage);
        assertThat(second.locator("#two-factor-recovery-toggle")).isVisible();
      }
    }
    assertThat(
            page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                    .setName(texts.getTwoFactorDownloadPdf())
                    .setExact(true)))
        .isVisible();
    String url = page.locator("#two-factor-download-pdf").getAttribute("data-download-url");
    @SuppressWarnings("unchecked")
    Map<String, Object> pdf =
        (Map<String, Object>)
            page.evaluate(
                "async url => { const response = await fetch(url, {cache:'no-store'}); return {status:response.status, cache:response.headers.get('cache-control'), size:(await response.arrayBuffer()).byteLength}; }",
                url);
    org.junit.jupiter.api.Assertions.assertEquals(200, ((Number) pdf.get("status")).intValue());
    org.junit.jupiter.api.Assertions.assertTrue(((String) pdf.get("cache")).contains("no-store"));
    org.junit.jupiter.api.Assertions.assertTrue(((Number) pdf.get("size")).intValue() > 1000);
    TwoFactorPage.captureCodes(page, BrowserTestAccounts.ADMIN_EMAIL);
    java.util.List<String> originalCodes = TwoFactorPage.recoveryCodeValues(page);
    for (int index = 1; index <= 10; index++) {
      assertThat(page.locator("#two-factor-recovery-code-" + index)).hasJSProperty("value", "");
      assertThat(page.locator("#two-factor-recovery-code-" + index + " input"))
          .hasAttribute("readonly", "");
    }
    verifyUnavailableRecoveryDownload(url, outputPage, originalCodes);
    com.microsoft.playwright.Download download =
        page.waitForDownload(() -> page.locator("#two-factor-download-pdf").click());
    org.junit.jupiter.api.Assertions.assertNull(download.failure());
    byte[] bytes = java.nio.file.Files.readAllBytes(download.path());
    assertEquals("%PDF-", new String(bytes, 0, 5, java.nio.charset.StandardCharsets.US_ASCII));
    assertThat(page).hasURL(outputPage);
    org.junit.jupiter.api.Assertions.assertEquals(
        originalCodes, TwoFactorPage.recoveryCodeValues(page));
    if (width == 1280) {
      // Die echte Fünf-Minuten-Frist darf nur den kurzen Sicherheitsnachweis beenden,
      // nicht die noch unbestätigte sitzungsgebundene Codeausgabe oder deren PDF.
      page.waitForCondition(
          () -> java.time.Instant.now().isAfter(shortProofExpired),
          new Page.WaitForConditionOptions().setTimeout(310_000));
      page.reload();
      org.junit.jupiter.api.Assertions.assertEquals(
          originalCodes, TwoFactorPage.recoveryCodeValues(page));
      com.microsoft.playwright.Download laterDownload =
          page.waitForDownload(() -> page.locator("#two-factor-download-pdf").click());
      org.junit.jupiter.api.Assertions.assertNull(laterDownload.failure());
      byte[] laterBytes = java.nio.file.Files.readAllBytes(laterDownload.path());
      assertEquals(
          "%PDF-", new String(laterBytes, 0, 5, java.nio.charset.StandardCharsets.US_ASCII));
      assertThat(page).hasURL(outputPage);
    }
    AllurePlaywright.attachScreenshot("Einmalige Ausgabe synthetischer Ersatzcodes", page);
    page.locator("#two-factor-codes-saved input").setChecked(true);
    page.locator("#two-factor-submit").click();
    assertThat(page).hasURL(Pattern.compile(".*/account\\?section=security&tab=portal$"));
    org.junit.jupiter.api.Assertions.assertTrue(
        ((Number) page.evaluate("async url => (await fetch(url, {cache:'no-store'})).status", url))
                .intValue()
            >= 400);
  }

  private void assertAccountActionCodeLayout() {
    Locator dialog = page.locator("#two-factor-action-dialog");
    Locator appCode = page.locator("#two-factor-action-factor");
    assertThat(appCode.locator("input[slot=input]")).isFocused();
    assertThat(appCode.locator("[part=slot]").first())
        .hasCSS("font-family", Pattern.compile(".*Space Mono.*"));
    Locator header = dialog.locator(".account-change-dialog-header");
    Locator close = header.locator(".cosmos-overlay-close");
    assertThat(close).isVisible();
    BoundingBox headerBounds = header.boundingBox();
    assertTrue(close.boundingBox().x > headerBounds.x + headerBounds.width / 2);
    assertThat(dialog.locator(".two-factor-method-tabs")).hasCSS("height", "42px");
    UiActions.complete(page, page.locator("#two-factor-action-recovery-code-tab")::click);
    assertThat(page.locator("#two-factor-action-recovery-code input"))
        .hasCSS("font-family", Pattern.compile(".*Space Mono.*"));
    assertThat(appCode).isHidden();
  }

  private void verifyUnavailableRecoveryDownload(
      String url, String outputPage, List<String> originalCodes) {
    // Ein tatsächlich nicht verfügbarer Codesatz wird vom geschützten Serverendpunkt
    // abgelehnt. Die native Downloadaktion muss auch dabei die sichtbare Ausgabe erhalten.
    String unavailable = "/internal/two-factor/recovery/" + UUID.randomUUID() + "/codes.pdf";
    page.locator("#two-factor-download-pdf")
        .evaluate("(button, url) => button.setAttribute('data-download-url', url)", unavailable);
    java.util.List<com.microsoft.playwright.Download> failedDownloads = new java.util.ArrayList<>();
    java.util.function.Consumer<com.microsoft.playwright.Download> captureDownload =
        failedDownloads::add;
    page.onDownload(captureDownload);
    try {
      page.locator("#two-factor-download-pdf").click();
      assertThat(
              page.getByText(
                  texts.getTwoFactorDownloadFailed(), new Page.GetByTextOptions().setExact(true)))
          .isVisible();
      assertTrue(failedDownloads.isEmpty(), "Error responses must not offer a file download");
    } finally {
      page.offDownload(captureDownload);
      page.locator("#two-factor-download-pdf")
          .evaluate("(button, url) => button.setAttribute('data-download-url', url)", url);
    }
    assertThat(page).hasURL(outputPage);
    org.junit.jupiter.api.Assertions.assertEquals(
        originalCodes, TwoFactorPage.recoveryCodeValues(page));
    assertThat(page.locator("#two-factor-submit")).isDisabled();
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName(
      "MFA-03: Sicherheitskonfiguration nur für Superadmins mit nativen Rollenregeln und Feldhilfe")
  @Tag("two-factor-configuration")
  void superadminOnlyConfiguration(int width) {
    page.setViewportSize(width, 900);
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    page.navigate("/admin?section=configuration&group=two_factor");
    assertThat(page.locator("#two-factor-save")).isVisible();
    assertThat(
            page.getByText(
                texts.getTwoFactorSettingsPageHint(), new Page.GetByTextOptions().setExact(true)))
        .isVisible();
    assertThat(page.locator(".mfa-settings-header").getByText(texts.getTwoFactorSettingsHint()))
        .isVisible();
    Locator toggle = page.locator("#account-navigation-toggle");
    if (toggle.isVisible()) {
      UiActions.complete(page, toggle::click);
    }
    Locator configuration = page.locator("#admin-nav-configuration-two_factor");
    configuration.hover();
    assertThat(page.getByRole(AriaRole.TOOLTIP)).hasText(texts.getTwoFactorSettingsTitle());
    if (toggle.isVisible()) {
      UiActions.complete(page, toggle::click);
    }
    Locator tabs = page.locator("#two-factor-role-tabs vaadin-tab");
    assertThat(tabs).hasCount(4);
    Locator roleContent = page.locator(".mfa-role-content:visible");
    assertThat(roleContent.locator("vaadin-combo-box")).hasCount(9);
    assertThat(roleContent.locator(".mfa-config-heading").first())
        .hasText(texts.getTwoFactorAccessGroup());
    Locator guide = roleContent.locator(".mfa-role-guide");
    assertThat(guide).containsText(texts.getTwoFactorRoleScope(texts.getRoleSuperadmin()));
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
    Locator administration = page.locator("#two-factor-rule-role_superadmin-administration");
    verifyRuleChoice(administration);
    page.getByRole(
            AriaRole.BUTTON,
            new Page.GetByRoleOptions()
                .setName(
                    texts.getAdminConfigHelpTitle()
                        + " – "
                        + texts.getTwoFactorRuleAdministration())
                .setExact(true))
        .click();
    Locator help = page.locator("#two-factor-field-help");
    Locator helpOverlay = help.locator("vaadin-dialog-overlay > [part~=overlay]");
    assertThat(helpOverlay).isVisible();
    assertThat(help).containsText(texts.getTwoFactorAdminAccessHelp());
    assertThat(help.locator(".admin-help-field-title"))
        .hasText(texts.getTwoFactorRuleAdministration());
    assertThat(help.locator(".admin-help-default span").last())
        .hasText(texts.getTwoFactorModeOff());
    List<String> helpItems = texts.twoFactorRuleHelpItems();
    assertThat(help.locator(".admin-help-rules li")).hasText(helpItems.toArray(String[]::new));
    AllurePlaywright.attachScreenshot("2FA-Feldhilfe " + width, page);
    page.keyboard().press("Escape");
    assertThat(helpOverlay).isHidden();
    tabs.nth(2).click();
    assertThat(guide).containsText(texts.getTwoFactorRoleScope(texts.getRoleUser()));
    assertThat(page.locator("#two-factor-inactivity-enabled")).isVisible();
    assertTrue(
        (Boolean) page.evaluate("document.documentElement.scrollWidth <= window.innerWidth"));
    assertThat(
            page.getByText(
                texts.getTwoFactorDevelopmentTitle(), new Page.GetByTextOptions().setExact(true)))
        .isHidden();
    AllurePlaywright.attachScreenshot("Superadmin-Regeln für Zwei-Faktor-Authentifizierung", page);
    try (BrowserActor administrator = new BrowserActor(browser)) {
      administrator.login(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
      new UserMenu(administrator.page).selectTestLanguage();
      administrator.page.navigate("/admin?section=configuration&group=two_factor");
      assertThat(administrator.page.locator("#two-factor-save")).hasCount(0);
      assertThat(administrator.page.locator("#admin-nav-configuration-two_factor")).isHidden();
      administrator.page.navigate("/admin?section=configuration&group=external_login");
      assertThat(administrator.page.locator("#admin-provider-settings")).hasCount(0);
      new UserMenu(administrator.page).signOut();
    }
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
        page.getByRole(
            AriaRole.OPTION,
            new Page.GetByRoleOptions().setName(texts.getTwoFactorModeOff()).setExact(true));
    assertThat(off).hasAttribute("aria-selected", "true");
    page.keyboard().press("Escape");
    assertThat(menu).isHidden();
    input.press("ArrowDown");
    assertThat(menu).isVisible();
    Locator required =
        page.getByRole(
            AriaRole.OPTION,
            new Page.GetByRoleOptions().setName(texts.getTwoFactorModeRequired()).setExact(true));
    UiActions.completeValueChange(page, required::click);
    assertThat(input).hasValue(texts.getTwoFactorModeRequired());
    input.clear();
    input.press("Tab");
    assertThat(input).hasValue(texts.getTwoFactorModeRequired());
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @Tag("confirmation-screens")
  @Tag("follow-up-regression")
  @DisplayName("MFA-18: Codebestätigung, geschützter Faktorwechsel und Hilfe folgen Variante A")
  void confirmationRecoveryAndHelpUseNativeControls(int width) {
    page.setViewportSize(width, 900);
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    String email = "confirmation-long-" + UUID.randomUUID() + "@example.test";
    String firstName = "Alexandra Maria Elisabeth";
    String lastName = "Beispielmann Sonnenschein";
    AdminUsersPage users = new AdminUsersPage(page);
    users.open();
    AdminUserDialog account = users.openCreate();
    account.fillNewUser(email, BrowserTestAccounts.USER_PASSWORD, true, true);
    account.firstName().fill(firstName);
    account.lastName().fill(lastName);
    account.save();
    new UserMenu(page).signOut();
    login.signIn(email, BrowserTestAccounts.USER_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    new UserMenu(page).signOut();
    login.signInFirstFactor(email, BrowserTestAccounts.USER_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    assertReducedSecurityHeader(firstName + " " + lastName, email);
    assertThat(page.locator("#two-factor-code")).hasJSProperty("length", 6);
    assertThat(page.locator("#two-factor-code [part=slot]")).hasCount(6);
    page.locator("#two-factor-code input").fill("12345");
    assertThat(page.locator("#two-factor-code [part=slot]"))
        .hasText(new String[] {"1", "2", "3", "4", "5", "•"});
    page.locator("#two-factor-recovery-tab").click();
    String recoveryCode = "12345678-12345678-12345678-12345678";
    TwoFactorPage.enterCode(page, recoveryCode);
    assertThat(page.locator("#two-factor-code input")).hasValue(recoveryCode);
    UiTexts other = new UiTexts(texts.language().equals("de") ? "en" : "de");
    new UserMenu(page).selectLanguage(other.language());
    assertThat(page.locator("#two-factor-recovery-tab")).hasAttribute("selected", "");
    assertThat(page.locator("#two-factor-code"))
        .hasJSProperty("label", other.getTwoFactorRecoveryCodeLabel());
    new UserMenu(page).selectTestLanguage();
    page.locator("#two-factor-authenticator-tab").click();
    assertThat(page.locator("#two-factor-code [part=slot]")).hasCount(6);
    assertTrue(
        (Boolean) page.evaluate("document.documentElement.scrollWidth <= window.innerWidth"));
    AllurePlaywright.attachScreenshot("Anmeldung bestätigen mit nativer Methodenauswahl", page);
    assertThat(page.locator("#two-factor-recover, #two-factor-help").first()).isHidden();
    TwoFactorPage.openLoginHelp(page);
    page.locator("#two-factor-recover").click();
    assertThat(page.locator("vaadin-text-field#two-factor-code input"))
        .hasAttribute("maxlength", "35");
    assertThat(page.locator(".cosmos-panel-footer #two-factor-recovery-back"))
        .hasText(texts.getTwoFactorReturnToCode());
    assertThat(page.locator("#two-factor-help")).isHidden();
    assertThat(page.locator("#two-factor-panel"))
        .containsText(texts.getTwoFactorRecoveryConsumptionHint());
    AllurePlaywright.attachScreenshot(
        "Authenticator mit Ersatzcode geschützt wiederherstellen", page);
    page.locator("#two-factor-recovery-back").click();
    assertThat(page).hasURL(Pattern.compile(".*/two-factor$"));
    assertThat(page.locator("#two-factor-authenticator-tab")).isVisible();
    TwoFactorPage.openLoginHelp(page);
    page.locator("#two-factor-help").click();
    assertThat(page.locator("#account-support")).containsText(texts.getSupportEmpty());
    assertReducedSecurityHeader(firstName + " " + lastName, email);
    assertThat(page.locator(".cosmos-panel-footer #support-back")).isVisible();
    assertThat(page.locator("#support-ticket-grid")).isHidden();
    assertThat(page.locator("#support-reload, #support-close, #support-add-reply")).hasCount(0);
    assertThat(page.locator(".support-user-footer #support-sign-out ~ #support-create"))
        .isVisible();
    String draft = "Synthetische Bitte um Hilfe zu meinen gesicherten Ersatzcodes.";
    page.locator("#support-reply textarea").fill(draft);
    new UserMenu(page).selectLanguage(other.language());
    assertThat(page.locator("#support-reply textarea")).hasValue(draft);
    new UserMenu(page).selectTestLanguage();
    AllurePlaywright.attachScreenshot(
        "Hilfeanfrage mit Leerzustand und nativen Footeraktionen", page);
    page.locator("#support-create").click();
    assertThat(page.locator("#support-ticket-grid"))
        .containsText(texts.getSupportRecoverySubject());
    assertThat(page.locator("#support-add-reply")).isVisible();
    assertThat(page.locator("#support-back")).isVisible();
    assertThat(page.locator(".support-conversation-actions #support-close ~ #support-reload"))
        .isVisible();
    assertThat(page.locator(".support-user-footer #support-sign-out ~ #support-add-reply"))
        .isVisible();
    UiActions.complete(page, () -> page.locator("#support-close").click());
    assertThat(page.locator("#support-add-reply")).isHidden();
    assertThat(page.locator("#support-close")).isHidden();
    assertThat(page.locator(".support-conversation-actions #support-reload")).isVisible();
    assertThat(page.locator(".support-user-footer #support-sign-out ~ #support-create"))
        .isVisible();
    assertTrue(
        (Boolean) page.evaluate("document.documentElement.scrollWidth <= window.innerWidth"));
    page.locator("#support-back").click();
    assertThat(page.locator("#two-factor-code")).isVisible();
  }

  private void assertReducedSecurityHeader(String name, String email) {
    Locator identity = page.locator("#public-security-identity");
    Locator languages = page.locator(".public-security-navigation .public-languages");
    assertThat(identity.locator(".cosmos-header-identity > span"))
        .hasText(new String[] {name, email});
    assertThat(identity.locator(".cosmos-user-avatar")).isVisible();
    assertThat(identity.locator(".cosmos-avatar-status")).isHidden();
    assertThat(identity.locator("vaadin-button, a")).hasCount(0);
    assertThat(page.locator("#public-nav-home, #public-nav-login, #public-nav-register"))
        .hasCount(0);
    assertThat(languages.locator("vaadin-button")).hasCount(2);
    BoundingBox languageBounds = languages.boundingBox();
    BoundingBox identityBounds = identity.boundingBox();
    assertTrue(languageBounds.x + languageBounds.width <= identityBounds.x);
    assertTrue(identityBounds.x + identityBounds.width <= page.viewportSize().width);
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName(
      "MFA-04: Hilfszugang ohne zweiten Faktor zeigt nur eigene Wiederherstellungsanfragen")
  void restrictedRecoveryHelp(int width) {
    page.setViewportSize(width, 900);
    LoginPage login = new LoginPage(page);
    login.open();
    login.signInFirstFactor(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    assertThat(page).hasURL(Pattern.compile(".*/two-factor$"));
    TwoFactorPage.openLoginHelp(page);
    Locator help = page.locator("#two-factor-help");
    assertThat(help).isVisible();
    assertTrue(
        (Boolean)
            help.evaluate(
                "button => { const box = button.getBoundingClientRect(); const area = button.parentElement.getBoundingClientRect(); const field = document.getElementById('two-factor-code').getBoundingClientRect(); return box.top >= field.bottom + 12 && box.left >= area.left - 1 && box.right <= area.right + 1 && button.scrollWidth <= button.clientWidth + 1; }"),
        "Der übersetzte Hilfebutton muss innerhalb des Inhalts bleiben.");
    AllurePlaywright.attachScreenshot("Zwei-Faktor-Hilfe ohne überbreiten Button", page);
    // Der erste Fall hat dieses feste Testkonto eingerichtet. Im getrennten Hilfszugang
    // bleiben Authenticator und Backupcodes absichtlich unbenutzt.
    help.click();
    assertThat(page.locator("#account-support")).isVisible();
    page.locator("#support-reply textarea")
        .fill("Synthetische Anfrage: Ich finde meine gesicherten Ersatzcodes nicht.");
    if (page.locator("#support-create").isVisible()) {
      page.locator("#support-create").click();
      assertThat(page.locator("#support-ticket-grid"))
          .containsText(texts.getSupportRecoverySubject());
    }
    AllurePlaywright.attachScreenshot("Eingeschränkte eigene Wiederherstellungsanfrage", page);
    page.navigate("/admin");
    assertThat(page).hasURL(Pattern.compile(".*/recovery-help$"));
    page.locator("#support-sign-out").click();
    assertThat(page).hasURL(Pattern.compile(".*/login$"));
  }
}
