package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.BoundingBox;
import com.microsoft.playwright.options.KeyboardModifier;
import com.microsoft.playwright.options.ScreenshotAnimations;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.pages.AdminUserDialog;
import de.derpeterson.browsertests.pages.AdminUsersPage;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.NotificationComposePage;
import de.derpeterson.browsertests.pages.UserMenu;
import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Eigene Empfänger über echte Benutzerformulare anlegen; Vorschau prüfen, nichts versenden. */
@Tag("browser")
public class AvatarGroupBrowserTest extends BrowserFixture {
  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  void recipientAvatarGroupOpensSearchablePreviewWithoutSending(int width) {
    LoginPage loginPage = new LoginPage(browserPage);
    loginPage.open();
    loginPage.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    browserPage.setViewportSize(width, width == 390 ? 844 : 1000);
    List<String> recipientEmails = createRecipients();
    NotificationComposePage composePage = new NotificationComposePage(browserPage);
    composePage.open();
    // Frühere Fälle desselben Workers dürfen Versandeinträge hinterlassen; die Vorschau
    // darf keinen weiteren erzeugen.
    int sentRowCountBeforePreview = browserPage.locator(".cosmos-notification-sent-row").count();

    int selectedRecipientCount = 0;
    for (int recipientCount : new int[] {1, 2, 3, 5, 6, 7, 9}) {
      while (selectedRecipientCount < recipientCount) {
        String email = recipientEmails.get(selectedRecipientCount++);
        composePage.recipients().locator("input").fill(email);
        browserPage
            .getByRole(AriaRole.OPTION)
            .filter(new Locator.FilterOptions().setHasText(email))
            .click();
        composePage.recipients().locator("input").press("Escape");
        assertThat(composePage.root().locator(".cosmos-notification-compose-recipient-summary"))
            .hasText(uiTexts.getNotificationsSelectedUsers(selectedRecipientCount));
      }
      composePage.draft("Avatar Group", "Empfängervorschau / Recipient preview");
      Locator recipientAvatarGroupLocator =
          browserPage.locator("#admin-notifications-recipient-avatars");
      Locator individualAvatarLocators =
          recipientAvatarGroupLocator.locator(":scope > vaadin-avatar:not([slot]):visible");
      Locator overflowAvatarLocator =
          recipientAvatarGroupLocator.locator("vaadin-avatar[slot='overflow']");
      assertThat(individualAvatarLocators).hasCount(recipientCount <= 6 ? recipientCount : 5);
      assertAvatarFrameMatchesHeader(individualAvatarLocators.first());
      String recipientCountLabel =
          recipientCount == 1
              ? uiTexts.getNotificationsAvatarGroupOneRecipient()
              : uiTexts
                  .getNotificationsAvatarGroupManyRecipients()
                  .replace("{count}", Integer.toString(recipientCount));
      assertThat(recipientAvatarGroupLocator).hasAttribute("aria-label", recipientCountLabel);
      assertThat(recipientAvatarGroupLocator).hasAttribute("role", "button");
      assertThat(recipientAvatarGroupLocator).hasAttribute("aria-haspopup", "dialog");
      if (recipientCount <= 6) {
        assertThat(overflowAvatarLocator).not().isVisible();
      } else {
        assertThat(overflowAvatarLocator).hasJSProperty("abbr", "+" + (recipientCount - 5));
      }
      capture("Empfaenger " + recipientCount);
      composePage.review().focus();
      composePage.review().press("Tab");
      assertThat(recipientAvatarGroupLocator).isFocused();
      recipientAvatarGroupLocator.press("Enter");
      assertRecipientListFitsVisibleRows(recipientCount);
      verifyRecipientPopover(recipientEmails.get(recipientCount - 1), width);
      closeRecipientPopoverAndAssertSendEnabled(composePage, recipientAvatarGroupLocator);

      if (recipientCount == 2) {
        individualAvatarLocators.first().click();
        verifySelectionPersistsAcrossSearches(recipientEmails.get(0), recipientEmails.get(1));
        browserPage.mouse().click(1, 1);
        assertRecipientPopoverClosed(composePage, recipientAvatarGroupLocator);
      }
      if (recipientCount == 9) {
        overflowAvatarLocator.click();
        verifyRecipientPopover(recipientEmails.getFirst(), width);
        closeRecipientPopoverAndAssertSendEnabled(composePage, recipientAvatarGroupLocator);
      }
    }
    assertThat(browserPage.locator(".cosmos-notification-sent-row"))
        .hasCount(sentRowCountBeforePreview);
  }

  private void assertRecipientListFitsVisibleRows(int recipientCount) {
    Locator recipientList = browserPage.locator("#admin-notifications-recipient-list");
    Locator firstRecipient = recipientList.locator(".cosmos-recipient-button:visible").first();
    assertThat(firstRecipient).isVisible();
    BoundingBox rowBounds = firstRecipient.boundingBox();
    assertNotNull(rowBounds);
    double listContentHeight =
        ((Number) recipientList.evaluate("list => list.clientHeight")).doubleValue();
    assertEquals(Math.min(recipientCount, 5) * rowBounds.height, listContentHeight, 1);
  }

  private void verifySelectionPersistsAcrossSearches(String firstEmail, String secondEmail) {
    Locator recipientPopover = browserPage.locator("#admin-notifications-preview-recipients");
    Locator searchInput = recipientPopover.locator("#admin-notifications-recipient-search input");
    Locator recipientRows = recipientPopover.locator(".cosmos-recipient-button:visible");
    Locator firstRow = recipientRows.filter(new Locator.FilterOptions().setHasText(firstEmail));
    Locator secondRow = recipientRows.filter(new Locator.FilterOptions().setHasText(secondEmail));

    searchInput.fill("Ada Berg");
    assertThat(recipientRows).hasCount(2);
    firstRow.press("Enter");
    assertThat(firstRow).hasAttribute("aria-pressed", "true");
    assertThat(secondRow).hasAttribute("aria-pressed", "false");

    searchInput.fill("no-matching-recipient");
    assertThat(recipientPopover.locator(".cosmos-recipient-empty"))
        .containsText(uiTexts.getNotificationsRecipientSearchEmpty());
    assertThat(recipientRows).hasCount(0);
    searchInput.clear();
    assertThat(firstRow).hasAttribute("aria-pressed", "true");
    secondRow.press("Space");
    assertThat(firstRow).hasAttribute("aria-pressed", "false");
    assertThat(secondRow).hasAttribute("aria-pressed", "true");
    secondRow.click(new Locator.ClickOptions().setModifiers(List.of(KeyboardModifier.CONTROL)));
    assertThat(secondRow).hasAttribute("aria-pressed", "false");
  }

  private void assertAvatarFrameMatchesHeader(Locator recipientAvatarLocator) {
    Locator headerAvatarLocator =
        browserPage.locator(".cosmos-topbar vaadin-avatar.cosmos-avatar-circle");
    String frameStyleScript =
        "avatar => { const s = getComputedStyle(avatar); return "
            + "[s.borderTopWidth,s.borderTopStyle,s.borderTopColor,s.backgroundColor,s.color]; }";
    assertEquals(
        headerAvatarLocator.evaluate(frameStyleScript),
        recipientAvatarLocator.evaluate(frameStyleScript));
  }

  private void verifyRecipientPopover(String emailAddress, int viewportWidth) {
    Locator recipientPopoverLocator =
        browserPage.locator("#admin-notifications-preview-recipients");
    Locator searchInputLocator =
        recipientPopoverLocator.locator("#admin-notifications-recipient-search input");
    assertThat(recipientPopoverLocator).hasAttribute("opened", "");
    assertThat(searchInputLocator).isFocused();
    assertThat(browserPage.locator("vaadin-avatar-group-menu:visible")).hasCount(0);

    Locator popoverFrameLocator = recipientPopoverLocator.locator("[part~='overlay']");
    assertTrue(popoverFrameLocator.boundingBox().x >= 0);
    assertTrue(
        popoverFrameLocator.boundingBox().x + popoverFrameLocator.boundingBox().width
            <= viewportWidth + 1);
    searchInputLocator.fill(emailAddress);
    Locator recipientButtonLocator =
        recipientPopoverLocator.locator(".cosmos-recipient-button:visible");
    assertThat(recipientButtonLocator).hasCount(1);
    assertThat(recipientButtonLocator).containsText(emailAddress);
    capture("Gefilterte Empfaengerliste");
    searchInputLocator.clear();
  }

  private void closeRecipientPopoverAndAssertSendEnabled(
      NotificationComposePage composePage, Locator recipientAvatarGroupLocator) {
    browserPage.keyboard().press("Escape");
    assertRecipientPopoverClosed(composePage, recipientAvatarGroupLocator);
  }

  private void assertRecipientPopoverClosed(
      NotificationComposePage composePage, Locator recipientAvatarGroupLocator) {
    assertThat(browserPage.locator("#admin-notifications-preview-recipients"))
        .not()
        .hasAttribute("opened", "");
    assertThat(recipientAvatarGroupLocator).isFocused();
    assertThat(composePage.send()).isEnabled();
  }

  private List<String> createRecipients() {
    String[][] recipientNames = {
      {"Ada", "Berg"}, {"Ada", "Berg"}, {"Cem", "Lang"},
      {"Dana", "Roth"}, {"Emil", "Wolf"}, {"Fara", "Jung"},
      {"Gina", "Koch"}, {"Hugo", "Brandt"}, {"Iris", "Voss"}
    };
    List<String> recipientEmails = new ArrayList<>();
    AdminUsersPage usersPage = new AdminUsersPage(browserPage);
    for (String[] recipientName : recipientNames) {
      String email = "avatars-" + UUID.randomUUID() + "@example.test";
      usersPage.open();
      AdminUserDialog userDialog = usersPage.openCreate();
      userDialog.fillNewUser(email, BrowserTestAccounts.USER_PASSWORD, true, false);
      userDialog.firstName().fill(recipientName[0]);
      userDialog.lastName().fill(recipientName[1]);
      userDialog.save();
      usersPage.verifyCreatedAccount(email, recipientName[0] + " " + recipientName[1]);
      recipientEmails.add(email);
    }
    return recipientEmails;
  }

  private void capture(String state) {
    Locator previewCardLocator = browserPage.locator(".admin-notification-preview-card");
    previewCardLocator.scrollIntoViewIfNeeded();
    browserPage.mouse().move(0, 0);
    String name = "Avatargruppe " + state + " " + browserPage.viewportSize().width;
    Allure.attachment(
        name,
        "image/png",
        new ByteArrayInputStream(
            previewCardLocator.screenshot(
                new Locator.ScreenshotOptions().setAnimations(ScreenshotAnimations.DISABLED))),
        AttachmentOptions.empty());
    Allure.attachment(
        name + " Seite",
        "image/png",
        new ByteArrayInputStream(
            browserPage.screenshot(
                new Page.ScreenshotOptions().setAnimations(ScreenshotAnimations.DISABLED))),
        AttachmentOptions.empty());
  }
}
