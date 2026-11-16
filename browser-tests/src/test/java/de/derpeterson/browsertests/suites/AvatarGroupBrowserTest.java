package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
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
  void recipientAvatarGroupPreservesPreviewAndExposesNativeOverflow(int width) {
    LoginPage loginPage = new LoginPage(browserPage);
    loginPage.open();
    loginPage.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    browserPage.setViewportSize(width, width == 390 ? 844 : 1000);
    List<String> recipientEmails = createRecipients();
    NotificationComposePage composePage = new NotificationComposePage(browserPage);
    composePage.open();

    int selectedRecipientCount = 0;
    for (int recipientCount : new int[] {3, 5, 6, 9}) {
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
      assertThat(recipientAvatarGroupLocator)
          .hasAttribute(
              "aria-label",
              uiTexts
                  .getNotificationsAvatarGroupManyRecipients()
                  .replace("{count}", Integer.toString(recipientCount)));
      if (recipientCount <= 6) {
        assertThat(overflowAvatarLocator).not().isVisible();
      } else {
        assertThat(overflowAvatarLocator).hasJSProperty("abbr", "+4");
      }
      capture("Empfaenger " + recipientCount);
      composePage.review().focus();
      composePage.review().press("Tab");
      for (Locator recipientAvatarLocator :
          recipientAvatarGroupLocator.locator(":scope > vaadin-avatar:visible").all()) {
        assertThat(recipientAvatarLocator).isFocused();
        recipientAvatarLocator.press("Tab");
      }
      assertThat(browserPage.locator("#admin-notifications-preview-details")).isFocused();
      browserPage.locator("#admin-notifications-preview-details").click();
      Locator recipientDetailsLocator =
          browserPage.locator("#admin-notifications-preview-recipients");
      assertThat(recipientDetailsLocator).hasAttribute("opened", "");
      for (String email : recipientEmails.subList(0, recipientCount)) {
        assertThat(recipientDetailsLocator).containsText(email);
      }
      browserPage.locator("#admin-notifications-preview-details").click();
      assertThat(composePage.send()).isEnabled();
      if (recipientCount == 9) {
        checkNativeOverflow(recipientAvatarGroupLocator, overflowAvatarLocator, width);
      }
    }
    checkNativeOverflowWithTwoHundredSyntheticItems();
    assertThat(browserPage.locator(".cosmos-notification-sent-row")).hasCount(0);
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

  /**
   * Reiner Widget-Mengenfall; die neun real geprüften Empfänger werden danach wiederhergestellt.
   */
  private void checkNativeOverflowWithTwoHundredSyntheticItems() {
    Locator recipientAvatarGroupLocator =
        browserPage.locator("#admin-notifications-recipient-avatars");
    Object reviewedAvatarItems = recipientAvatarGroupLocator.evaluate("group => group.items");
    recipientAvatarGroupLocator.evaluate(
        """
        group => { group.items = Array.from({length:200}, (_,index) => ({name:`Recipient ${index + 1}`})); }
        """);
    Locator overflowAvatarLocator =
        recipientAvatarGroupLocator.locator("vaadin-avatar[slot='overflow']");
    assertThat(overflowAvatarLocator).hasJSProperty("abbr", "+195");
    overflowAvatarLocator.click();
    Locator menuItemLocators = browserPage.locator("vaadin-avatar-group-menu-item:visible");
    assertThat(menuItemLocators).hasCount(195);
    Locator overlayFrameLocator =
        browserPage.locator("vaadin-avatar-group-overlay[opened]").locator("[part='overlay']");
    assertTrue(overlayFrameLocator.boundingBox().height <= 361);
    assertTrue(
        ((Number) overlayFrameLocator.evaluate("frame => frame.scrollHeight")).doubleValue()
            > ((Number) overlayFrameLocator.evaluate("frame => frame.clientHeight")).doubleValue());
    browserPage.keyboard().press("End");
    assertThat(menuItemLocators.last()).isFocused();
    assertThat(menuItemLocators.last()).containsText("Recipient 200");
    assertTrue(
        ((Number) overlayFrameLocator.evaluate("frame => frame.scrollTop")).doubleValue() > 0);
    assertTrue(menuItemLocators.last().boundingBox().y >= overlayFrameLocator.boundingBox().y);
    assertTrue(
        menuItemLocators.last().boundingBox().y + menuItemLocators.last().boundingBox().height
            <= overlayFrameLocator.boundingBox().y + overlayFrameLocator.boundingBox().height + 1);
    capture("200 Eintraege");
    browserPage.keyboard().press("Escape");
    assertThat(browserPage.locator("vaadin-avatar-group-menu:visible")).hasCount(0);
    assertThat(overflowAvatarLocator).isFocused();
    recipientAvatarGroupLocator.evaluate(
        "(group,items) => { group.items = items; }", reviewedAvatarItems);
    assertThat(overflowAvatarLocator).hasJSProperty("abbr", "+4");
  }

  @SuppressWarnings("unchecked")
  private void checkNativeOverflow(
      Locator recipientAvatarGroupLocator, Locator overflowAvatarLocator, int width) {
    List<String> hiddenNames =
        (List<String>)
            recipientAvatarGroupLocator.evaluate(
                "group => group.items.slice(5).map(item => item.name)");
    overflowAvatarLocator.click();
    Locator menuItemLocators = browserPage.locator("vaadin-avatar-group-menu-item:visible");
    assertThat(menuItemLocators).hasCount(4);
    assertAvatarFrameMatchesHeader(menuItemLocators.first().locator("vaadin-avatar"));
    for (int recipientIndex = 0; recipientIndex < hiddenNames.size(); recipientIndex++) {
      assertThat(menuItemLocators.nth(recipientIndex))
          .containsText(hiddenNames.get(recipientIndex));
    }
    Locator recipientMenuLocator = browserPage.locator("vaadin-avatar-group-menu:visible");
    assertTrue(recipientMenuLocator.boundingBox().x >= 0);
    assertTrue(
        recipientMenuLocator.boundingBox().x + recipientMenuLocator.boundingBox().width
            <= width + 1);
    capture("Native Liste");
    browserPage.keyboard().press("Escape");
    assertThat(recipientMenuLocator).not().isVisible();
    assertThat(overflowAvatarLocator).isFocused();
    overflowAvatarLocator.press("Enter");
    assertThat(recipientMenuLocator).isVisible();
    assertThat(menuItemLocators).hasCount(4);
    browserPage.keyboard().press("Escape");
    assertThat(recipientMenuLocator).not().isVisible();
    assertThat(overflowAvatarLocator).isFocused();
  }

  private List<String> createRecipients() {
    String[][] recipientNames = {
      {"Ada", "Berg"}, {"Ben", "Klein"}, {"Cem", "Lang"},
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
