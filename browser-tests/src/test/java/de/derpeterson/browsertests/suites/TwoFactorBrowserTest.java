package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.BoundingBox;
import de.derpeterson.browsertests.BrowserActor;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.BrowserTestRunner;
import de.derpeterson.browsertests.PersistentBrowserActor;
import de.derpeterson.browsertests.i18n.UiTexts;
import de.derpeterson.browsertests.mail.MailpitInbox;
import de.derpeterson.browsertests.pages.AdminUsersPage;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.NotificationInboxPage;
import de.derpeterson.browsertests.pages.TwoFactorPage;
import de.derpeterson.browsertests.pages.UiActions;
import de.derpeterson.browsertests.pages.UserMenu;
import io.qameta.allure.playwright.AllurePlaywright;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
  @DisplayName("MFA-23: Vorgangsseiten erhalten Details und Entwurf bei 10, 20 und 50 Einträgen")
  void ticketPagesPreserveDetailsAndDrafts() {
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    AdminUsersPage users = new AdminUsersPage(page);
    List<String> numbers = new ArrayList<>();
    for (int index = 0; index < 12; index++) {
      String email = "ticket-pages-" + UUID.randomUUID() + "@example.test";
      users.createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
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
        paging.locator("vaadin-combo-box").click();
        UiActions.completeValueChange(
            page,
            () ->
                page.getByRole(
                        AriaRole.OPTION,
                        new Page.GetByRoleOptions().setName(Integer.toString(size)).setExact(true))
                    .click());
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
        page.navigate("/admin?section=support");
        assertThat(page.locator("#support-select-ticket")).isVisible();
        requester.page.locator("#support-reply textarea").fill("Unread activity at width " + width);
        UiActions.complete(
            requester.page, () -> requester.page.locator("#support-add-reply").click());
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
        panel
            .locator("#support-filter-reading")
            .getByText(
                texts.getNotificationsUnread(), new Locator.GetByTextOptions().setExact(true))
            .click();
        panel.locator("#support-filter-apply").click();
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
      pasteOtp(actor, valid);
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
  @DisplayName("MFA-20: Ticketzähler und Glocke folgen Zuständigkeit, Beobachtung und Abschluss")
  void ticketUnreadAndNotificationsFollowAssignmentAndClosure(int width) {
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    String suffix = UUID.randomUUID().toString();
    String requesterEmail = "ticket-requester-" + suffix + "@example.test";
    String assigneeEmail = "ticket-assignee-" + suffix + "@example.test";
    AdminUsersPage users = new AdminUsersPage(page);
    users.createActiveUser(requesterEmail, BrowserTestAccounts.USER_PASSWORD);
    users.createUser(assigneeEmail, BrowserTestAccounts.USER_PASSWORD, true, true);
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
      verifyWatchedAndUnwatchedReplies(admin, user, number, previousNotifications);
      UiActions.complete(page, () -> ticketCard(page, number).click());
      setTicketWatch(page, true);
      page.navigate("/home");
      admin.navigate("/home");
      UiActions.complete(user, () -> user.locator("#support-close").click());
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
    UiActions.complete(page, () -> ticketCard(page, number).click());
    assertNotificationCount(page, previousNotifications);
    setTicketWatch(page, false);
    page.navigate("/home");
    user.locator("#support-reply textarea").fill("Ohne Beobachtung bleibt die Glocke ruhig.");
    UiActions.complete(user, () -> user.locator("#support-add-reply").click());
    // Erst die tatsächlich eingetroffene Live-Aktualisierung prüfen, dann das Ausbleiben des
    // Zählers.
    assertThat(ticketCard(admin, number).locator(".support-ticket-unread")).hasText("2");
    page.navigate("/admin?section=support");
    assertThat(ticketCard(page, number).locator(".support-ticket-unread")).hasCount(0);
    assertNotificationCount(page, previousNotifications);
    assertNotificationCount(admin, 0);
  }

  private void verifyClaimNotification(
      Page admin, NotificationInboxPage inbox, String number, int previousNotifications) {
    inbox.open();
    UiActions.completeValueChange(
        page, () -> inbox.filter(inbox.inbox(), texts.getNotificationsUnread()));
    inbox
        .row(inbox.inbox(), texts.getSupportRecoverySubject())
        .locator(".cosmos-notification-row-open")
        .first()
        .click();
    assertThat(inbox.dialog().locator(".cosmos-notification-detail-state"))
        .hasText(texts.getNotificationsUnread());
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
  @DisplayName("MFA-19: Ticketliste, Panel-Tabs und Auswahlhinweis passen zur verfügbaren Breite")
  void administrativeTicketLayoutKeepsTabsAndCardsUnclipped() {
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(page).selectTestLanguage();
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
    assertThat(hint).containsText(texts.getSupportSelectTicket());
    assertThat(hint.locator("[data-lucide=mouse-pointer-click]")).isVisible();
    UiActions.complete(page, () -> categories.locator("vaadin-tab").nth(1).click());
    assertTicketLocation("recovery", null);
    page.reload();
    assertThat(categories.locator("vaadin-tab[selected]"))
        .hasText(texts.getSupportRecoveryCategory());
    assertThat(hint.locator("[data-lucide=mouse-pointer-click]")).isVisible();
    assertThat(ticketCard(page, ownClosedNumber)).hasCount(1);
    assertThat(ticketCard(page, ownOpenNumber)).hasCount(1);
    for (int width : new int[] {1600, 1280, 1024, 390}) {
      page.setViewportSize(width, 1000);
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
    reload.hover();
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
  @DisplayName(
      "MFA-07: Admin übernimmt eine eingeschränkte Hilfsanfrage und sendet die geprüfte Anleitung")
  @Tag("recovery-instructions")
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
      UiActions.complete(admin, () -> admin.locator("#support-add-reply").click());
      assertThat(admin.locator(".support-internal-message")).containsText(privateNote);
      adminComposer.fill(publicReply);
      UiActions.complete(admin, () -> admin.locator("#support-add-reply").click());
      assertThat(page.locator(".support-message-thread")).containsText(publicReply);
      assertThat(page.locator(".support-message-thread")).not().containsText(privateNote);
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
      guide.scrollIntoViewIfNeeded();
      AllurePlaywright.attachScreenshot("Anleitung kompakt im Nachrichtenverlauf", page);
      UiActions.complete(page, () -> guide.locator(".support-instruction-toggle").click());
      assertThat(guide.locator(".support-instruction-details")).hasAttribute("opened", "");
      assertThat(guide.locator(".support-instruction-step")).hasCount(4);
      assertThat(guide.locator(".cosmos-security-warning")).isVisible();
      UiActions.complete(page, () -> page.locator("#support-reload").click());
      assertThat(guide.locator(".support-instruction-details")).hasAttribute("opened", "");
      guide.scrollIntoViewIfNeeded();
      AllurePlaywright.attachScreenshot("Anleitung aufgeklappt im Nachrichtenverlauf", page);
      UiActions.complete(admin, () -> admin.locator("#support-close").click());
      new UserMenu(admin).signOut();
    }
    page.navigate("/admin");
    assertThat(page).hasURL(Pattern.compile(".*/recovery-help$"));
    page.locator("#support-sign-out").click();
    new LoginPage(page).assertFormVisible();
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
        .containsText(texts.getTwoFactorInvalidCode());
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
    TwoFactorPage.enterActionCode(
        page, TwoFactorPage.nextCode(page, BrowserTestAccounts.ADMIN_EMAIL));
    page.locator("#two-factor-action-confirm").click();
    assertThat(page.locator("#two-factor-recovery-toggle")).isVisible();
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
  @DisplayName("MFA-18: Codebestätigung, geschützter Faktorwechsel und Hilfe folgen Variante A")
  void confirmationRecoveryAndHelpUseNativeControls(int width) {
    page.setViewportSize(width, 900);
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    String email = "confirmation-mfa-" + UUID.randomUUID() + "@example.test";
    new AdminUsersPage(page).createUser(email, BrowserTestAccounts.USER_PASSWORD, true, true);
    new UserMenu(page).signOut();
    login.signIn(email, BrowserTestAccounts.USER_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    new UserMenu(page).signOut();
    login.signInFirstFactor(email, BrowserTestAccounts.USER_PASSWORD);
    new UserMenu(page).selectTestLanguage();
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
    assertThat(page.locator(".cosmos-panel-footer #support-back")).isVisible();
    assertThat(page.locator("#support-ticket-grid")).isHidden();
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
    assertTrue(
        (Boolean) page.evaluate("document.documentElement.scrollWidth <= window.innerWidth"));
    page.locator("#support-back").click();
    assertThat(page.locator("#two-factor-code")).isVisible();
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
