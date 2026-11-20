package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.ACCOUNT_ROUTE;
import static de.derpeterson.app.config.AppRouteConstants.ADMIN_ROUTE;
import static de.derpeterson.app.config.AppRouteConstants.HOME_ROUTE;
import static de.derpeterson.browsertests.BrowserRoutes.path;
import static de.derpeterson.browsertests.BrowserRoutes.urlPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.BoundingBox;
import com.microsoft.playwright.options.ForcedColors;
import de.derpeterson.app.config.AdminConfigGroup;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.i18n.UiTexts;
import de.derpeterson.browsertests.pages.AccountPage;
import de.derpeterson.browsertests.pages.ActionGridChecks;
import de.derpeterson.browsertests.pages.AdminConfigurationPage;
import de.derpeterson.browsertests.pages.AdminMailPage;
import de.derpeterson.browsertests.pages.AdminNavigation;
import de.derpeterson.browsertests.pages.AdminUserDialog;
import de.derpeterson.browsertests.pages.AdminUsersPage;
import de.derpeterson.browsertests.pages.CosmosFollowUpChecks;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.NotificationComposePage;
import de.derpeterson.browsertests.pages.UiActions;
import de.derpeterson.browsertests.pages.UnsavedChangesPrompt;
import de.derpeterson.browsertests.pages.UserMenu;
import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.playwright.AllurePlaywright;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Tag("browser")
@Feature("Gemeinsame Cosmos-Komponenten")
public class CosmosComponentsBrowserTest extends BrowserFixture {
  @Test
  @Tag("breadcrumbs")
  @DisplayName(
      "COMPONENT-11: Seitenpfade navigieren einheitlich und respektieren Sicherheitssperren")
  void pageBreadcrumbsFollowHierarchyAndKeepSecurityFlowsLocked() {
    for (int viewportWidth : new int[] {1280, 390}) {
      browserPage.setViewportSize(viewportWidth, 900);
      for (String publicRoute : List.of("login", "registration", "forgot-password")) {
        browserPage.navigate(path(publicRoute));
        assertBreadcrumbPath(List.of(uiTexts.getAccountNavigationPortal()), List.of("/home"));
        attachBreadcrumbScreenshot(publicRoute, viewportWidth);
      }
    }

    LoginPage superadminLoginPage = new LoginPage(browserPage);
    superadminLoginPage.open();
    superadminLoginPage.signIn(
        BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();

    for (int viewportWidth : new int[] {1280, 390}) {
      browserPage.setViewportSize(viewportWidth, 900);
      assertPortalAndAdminPagePaths(viewportWidth);
      assertConfigurationGroupPaths(viewportWidth);
      assertBreadcrumbNavigationProtectsProfileDraft();
      assertSecurityFlowBreadcrumbsStayUnlinked(viewportWidth);
    }

    new UserMenu(browserPage).signOut();
    signIn();
    for (int viewportWidth : new int[] {1280, 390}) {
      browserPage.setViewportSize(viewportWidth, 900);
      browserPage.navigate(path("admin?section=configuration&group=security"));
      assertBreadcrumbPath(
          List.of(uiTexts.getAdminHeaderTitle(), uiTexts.getAdminConfigTitle()),
          List.of("/admin", "/admin?section=configuration&group=general"));
      attachBreadcrumbScreenshot("admin-security-comparison", viewportWidth);
      breadcrumbLink(1).click();
      assertThat(browserPage)
          .hasURL(Pattern.compile(".*/admin\\?section=configuration&group=general$"));
      assertThat(browserPage.locator(".cosmos-section-title"))
          .hasText(uiTexts.getAdminConfigGroupGeneral() + ".");
    }
  }

  private void assertPortalAndAdminPagePaths(int viewportWidth) {
    for (String portalRoute :
        List.of(
            "home",
            "notifications",
            "account?section=profile",
            "account?section=security&tab=portal",
            "account?section=sessions")) {
      browserPage.navigate(path(portalRoute));
      boolean isAccountPage = portalRoute.startsWith("account");
      assertBreadcrumbPath(
          isAccountPage
              ? List.of(uiTexts.getAccountNavigationPortal(), uiTexts.getAccountSettingsOverview())
              : List.of(uiTexts.getAccountNavigationPortal()),
          isAccountPage ? List.of("/home", "/account?section=profile") : List.of("/home"));
      attachBreadcrumbScreenshot(portalRoute, viewportWidth);
    }

    for (String adminSection :
        List.of("dashboard", "user_management", "support", "notifications", "mail_test")) {
      browserPage.navigate(path("admin?section=" + adminSection));
      assertBreadcrumbPath(List.of(uiTexts.getAdminHeaderTitle()), List.of("/admin"));
      attachBreadcrumbScreenshot(adminSection, viewportWidth);
    }
  }

  private void assertConfigurationGroupPaths(int viewportWidth) {
    for (AdminConfigGroup configGroup : AdminConfigGroup.values()) {
      browserPage.navigate(
          path("admin?section=configuration&group=" + configGroup.name().toLowerCase(Locale.ROOT)));
      assertBreadcrumbPath(
          List.of(uiTexts.getAdminHeaderTitle(), uiTexts.getAdminConfigTitle()),
          List.of("/admin", "/admin?section=configuration&group=general"));
      assertThat(browserPage.locator(".cosmos-section-title"))
          .hasText(uiTexts.getAdminConfigGroupLabel(configGroup) + ".");
      attachBreadcrumbScreenshot("configuration-" + configGroup.name(), viewportWidth);
    }

    String otherLanguageTag = uiTexts.language().equals("de") ? "en" : "de";
    UiTexts otherUiTexts = new UiTexts(otherLanguageTag);
    new UserMenu(browserPage).selectLanguage(otherLanguageTag);
    assertBreadcrumbPath(
        List.of(otherUiTexts.getAdminHeaderTitle(), otherUiTexts.getAdminConfigTitle()),
        List.of("/admin", "/admin?section=configuration&group=general"));
    assertThat(browserPage.locator(".cosmos-section-title"))
        .hasText(otherUiTexts.getAdminConfigGroupLabel(AdminConfigGroup.QUEUE) + ".");
    new UserMenu(browserPage).selectTestLanguage();

    assertBreadcrumbOverflowMenu();

    breadcrumbLink(1).focus();
    assertThat(breadcrumbLink(1)).isFocused();
    breadcrumbLink(1).press("Enter");
    assertThat(browserPage.locator(".cosmos-section-title"))
        .hasText(uiTexts.getAdminConfigGroupGeneral() + ".");
  }

  private void assertBreadcrumbOverflowMenu() {
    Locator pageBreadcrumbsLocator = browserPage.locator(".cosmos-breadcrumbs");
    pageBreadcrumbsLocator.evaluate("element => element.style.maxWidth = '160px'");
    assertThat(pageBreadcrumbsLocator).hasAttribute("has-overflow", "");
    Locator overflowButtonLocator = pageBreadcrumbsLocator.locator("[part=overflow-button]");
    assertThat(overflowButtonLocator).hasAttribute("aria-label", uiTexts.getPageBreadcrumbsMore());
    overflowButtonLocator.click();
    assertThat(overflowButtonLocator).hasAttribute("aria-expanded", "true");
    assertThat(pageBreadcrumbsLocator.locator("vaadin-breadcrumbs-item[slot=overlay] a"))
        .hasAttribute("href", "/admin");
    overflowButtonLocator.press("Escape");
    assertThat(overflowButtonLocator).hasAttribute("aria-expanded", "false");
    pageBreadcrumbsLocator.evaluate("element => element.style.removeProperty('max-width')");
    assertThat(pageBreadcrumbsLocator).not().hasAttribute("has-overflow", "");
  }

  private void assertBreadcrumbNavigationProtectsProfileDraft() {
    browserPage.navigate(path("account?section=profile"));
    Locator firstNameInputLocator = browserPage.locator("#account-first-name input");
    String savedFirstName = firstNameInputLocator.inputValue();
    firstNameInputLocator.fill("Breadcrumb draft");
    breadcrumbLink(0).click();
    UnsavedChangesPrompt unsavedChangesPrompt = new UnsavedChangesPrompt(browserPage, uiTexts);
    assertThat(unsavedChangesPrompt.root()).hasAttribute("opened", "");
    unsavedChangesPrompt.keepEditing();
    assertThat(firstNameInputLocator).hasValue("Breadcrumb draft");
    breadcrumbLink(0).click();
    unsavedChangesPrompt.discard();
    assertThat(browserPage).hasURL(Pattern.compile(".*/home$"));

    browserPage.navigate(path("account?section=profile"));
    assertThat(firstNameInputLocator).hasValue(savedFirstName);
    breadcrumbLink(1).click();
    assertThat(browserPage).hasURL(Pattern.compile(".*/account\\?section=profile$"));
  }

  private void assertSecurityFlowBreadcrumbsStayUnlinked(int viewportWidth) {
    browserPage.navigate(path("two-factor?recover"));
    assertLockedAccountBreadcrumbs(uiTexts);
    String otherLanguageTag = uiTexts.language().equals("de") ? "en" : "de";
    new UserMenu(browserPage).selectLanguage(otherLanguageTag);
    assertLockedAccountBreadcrumbs(new UiTexts(otherLanguageTag));
    new UserMenu(browserPage).selectTestLanguage();
    attachBreadcrumbScreenshot("two-factor-locked", viewportWidth);
    browserPage.navigate(path("admin"));
    assertLockedAccountBreadcrumbs(uiTexts);
    assertThat(browserPage).hasURL(urlPattern("two-factor"));
    UiActions.complete(browserPage, () -> browserPage.locator("#two-factor-cancel").click());
    assertThat(browserPage).hasURL(Pattern.compile(".*/account\\?section=security&tab=portal$"));
    browserPage.navigate(path("recovery-help"));
    assertLockedAccountBreadcrumbs(uiTexts);
    attachBreadcrumbScreenshot("recovery-help-locked", viewportWidth);
    browserPage.navigate(path("home"));
    assertLockedAccountBreadcrumbs(uiTexts);
    assertThat(browserPage).hasURL(Pattern.compile(".*/recovery-help$"));
    UiActions.complete(browserPage, () -> browserPage.locator("#support-back").click());
    assertThat(browserPage).hasURL(Pattern.compile(".*/account\\?section=security&tab=portal$"));
  }

  private void assertLockedAccountBreadcrumbs(UiTexts expectedUiTexts) {
    Locator breadcrumbItemsLocator =
        browserPage.locator(".cosmos-breadcrumbs > vaadin-breadcrumbs-item");
    assertThat(breadcrumbItemsLocator)
        .hasText(
            new String[] {
              expectedUiTexts.getAccountNavigationPortal(),
              expectedUiTexts.getAccountSettingsOverview(),
              expectedUiTexts.getAccountSettingsSecurityTab()
            });
    assertThat(browserPage.locator(".cosmos-breadcrumbs a")).hasCount(0);
  }

  private void assertBreadcrumbPath(
      List<String> breadcrumbLabels, List<String> breadcrumbLocations) {
    Locator breadcrumbItemsLocator =
        browserPage.locator(".cosmos-breadcrumbs > vaadin-breadcrumbs-item");
    assertThat(breadcrumbItemsLocator).hasText(breadcrumbLabels.toArray(String[]::new));
    for (int itemIndex = 0; itemIndex < breadcrumbLocations.size(); itemIndex++) {
      assertThat(breadcrumbLink(itemIndex))
          .hasAttribute("href", breadcrumbLocations.get(itemIndex));
      assertThat(breadcrumbLink(itemIndex)).hasCSS("cursor", "pointer");
    }
    assertThat(browserPage.locator(".cosmos-breadcrumbs")).hasCSS("text-transform", "uppercase");
    assertThat(browserPage.locator(".cosmos-breadcrumbs")).hasCSS("color", "rgb(56, 189, 248)");
    assertTrue(
        (Boolean) browserPage.evaluate("() => document.documentElement.scrollWidth <= innerWidth"));
  }

  private Locator breadcrumbLink(int itemIndex) {
    return browserPage
        .locator(".cosmos-breadcrumbs > vaadin-breadcrumbs-item")
        .nth(itemIndex)
        .locator("a");
  }

  private void attachBreadcrumbScreenshot(String pageName, int viewportWidth) {
    AllurePlaywright.attachScreenshot(pageName + " nachher " + viewportWidth, browserPage);
  }

  @Test
  @Tag("duration-field")
  @Tag("component-follow-up")
  @DisplayName("COMPONENT-10: Integrierte Zeiteinheit bleibt mittig innerhalb der Zahlenfläche")
  @Description(
      """
      1. Ganzzahliges und präzises Zeitfeld auf Desktop und Smartphone vergleichen.
      2. Schrittaktion, Leeren und erneute Eingabe prüfen. Auch das Sitzungslimit leeren.
      **Erwartet:** Einheit, Feldaktionen und Eingabemitte bleiben ausgerichtet. Der leere Wert
      zeigt einen gedämpften Platzhalter, das Löschkreuz verschwindet. Keine Speicherung.
      """)
  void durationUnitIsCenteredInsideNumericField() {
    signIn();
    AdminConfigurationPage configurationPage = new AdminConfigurationPage(browserPage);

    for (int viewportWidth : new int[] {1280, 390}) {
      browserPage.setViewportSize(viewportWidth, 900);
      configurationPage.open(AdminConfigGroup.SECURITY);
      checkDurationFieldGeometryAndActions(
          configurationPage, uiTexts.getAdminConfigEntryRememberMeDuration(), viewportWidth);
      Locator sessionLimitInput =
          configurationPage.input(uiTexts.getAdminConfigEntryMaxSessionsPerUser());
      assertClearedNumberKeepsCenteredPlaceholder(
          sessionLimitInput,
          sessionLimitInput.locator("xpath=..").locator("[part~=clear-button]"),
          sessionLimitInput.inputValue());

      configurationPage.open(AdminConfigGroup.QUEUE);
      checkDurationFieldGeometryAndActions(
          configurationPage,
          uiTexts.getAdminConfigEntryEmailQueueSentLiveDuration(),
          viewportWidth);
    }
  }

  private void checkDurationFieldGeometryAndActions(
      AdminConfigurationPage configurationPage, String fieldLabelText, int viewportWidth) {
    Locator durationFieldLocator = configurationPage.duration(fieldLabelText);
    Locator amountField =
        durationFieldLocator.locator(
            "vaadin-integer-field:visible, vaadin-big-decimal-field:visible");
    assertThat(amountField.locator("input")).hasCSS("font-size", "14px");
    assertThat(amountField.locator("[part=input-field]:not([theme~=cosmos-duration-unit])"))
        .hasCSS("height", "40px");
    assertDurationUnitCentered(durationFieldLocator, fieldLabelText, viewportWidth);
    assertDurationActionGeometry(durationFieldLocator, fieldLabelText, viewportWidth);
    assertDurationStepAndClear(configurationPage, fieldLabelText);
    assertDurationUnitOverlayStyle(durationFieldLocator.locator("vaadin-select"), viewportWidth);
  }

  private void assertDurationUnitCentered(
      Locator durationFieldLocator, String fieldLabelText, int viewportWidth) {
    Locator amountSurfaceLocator =
        durationFieldLocator
            .locator("vaadin-integer-field, vaadin-big-decimal-field")
            .locator("[part=input-field]")
            .first();
    Locator unitSelectLocator = durationFieldLocator.locator("vaadin-select");
    Locator unitSurfaceLocator = unitSelectLocator.locator("[part=input-field]");
    Locator unitLabelLocator =
        unitSelectLocator.locator("vaadin-select-value-button > vaadin-select-item");
    Locator unitArrowLocator = unitSelectLocator.locator("[part~=toggle-button]");
    assertThat(unitLabelLocator).hasText(uiTexts.getAdminConfigDurationUnitDays());

    String fieldMeasurementsText =
        (String)
            unitSelectLocator.evaluate(
                """
                select => JSON.stringify(Object.fromEntries([
                  ['select', select],
                  ['surface', select.shadowRoot.querySelector('[part=input-field]')],
                  ['value', select.querySelector('vaadin-select-value-button')],
                  ['item', select.querySelector('vaadin-select-value-button > vaadin-select-item')],
                  ['arrow', select.shadowRoot.querySelector('[part~=toggle-button]')]
                ].map(([name, element]) => {
                  const box = element.getBoundingClientRect();
                  const style = getComputedStyle(element);
                  return [name, {top:box.top, height:box.height, bottom:box.bottom,
                    display:style.display, padding:style.padding, margin:style.margin,
                    lineHeight:style.lineHeight, minHeight:style.minHeight,
                    alignItems:style.alignItems, alignSelf:style.alignSelf}];
                })), null, 2)
                """);
    Allure.attachment(
        "Zeitfeldmessung " + fieldLabelText + " " + viewportWidth,
        "application/json",
        fieldMeasurementsText);
    Allure.attachment(
        "Zeitfeld " + fieldLabelText + " " + viewportWidth,
        "image/png",
        new ByteArrayInputStream(durationFieldLocator.screenshot()),
        AttachmentOptions.empty());
    AllurePlaywright.attachScreenshot("Integrierte Zeiteinheit " + viewportWidth, browserPage);

    BoundingBox amountSurfaceBounds = amountSurfaceLocator.boundingBox();
    double amountCenterY = amountSurfaceBounds.y + amountSurfaceBounds.height / 2;
    for (Locator unitPartLocator :
        List.of(unitSurfaceLocator, unitLabelLocator, unitArrowLocator)) {
      BoundingBox unitPartBounds = unitPartLocator.boundingBox();
      assertEquals(
          amountCenterY, unitPartBounds.y + unitPartBounds.height / 2, 1, fieldMeasurementsText);
      assertTrue(unitPartBounds.y >= amountSurfaceBounds.y + 1, fieldMeasurementsText);
      assertTrue(
          unitPartBounds.y + unitPartBounds.height
              <= amountSurfaceBounds.y + amountSurfaceBounds.height - 1,
          fieldMeasurementsText);
    }
    assertThat(unitLabelLocator).hasCSS("line-height", "20px");
  }

  private void assertDurationActionGeometry(
      Locator durationFieldLocator, String fieldLabelText, int viewportWidth) {
    Locator clearButtonLocator = durationFieldLocator.locator("[part~=clear-button]");
    Locator increaseButtonLocator = findDurationIncreaseButton(durationFieldLocator);
    assertThat(clearButtonLocator).isVisible();
    assertThat(increaseButtonLocator).isVisible();
    BoundingBox clearButtonBounds = clearButtonLocator.boundingBox();
    BoundingBox increaseButtonBounds = increaseButtonLocator.boundingBox();
    BoundingBox unitSelectBounds = durationFieldLocator.locator("vaadin-select").boundingBox();
    double clearToIncreaseGap =
        increaseButtonBounds.x - clearButtonBounds.x - clearButtonBounds.width;
    double increaseToUnitGap =
        unitSelectBounds.x - increaseButtonBounds.x - increaseButtonBounds.width;
    String actionMeasurementsText =
        Map.of(
                "clear",
                List.of(clearButtonBounds.width, clearButtonBounds.height),
                "increase",
                List.of(increaseButtonBounds.width, increaseButtonBounds.height),
                "clearToIncreaseGap",
                clearToIncreaseGap,
                "increaseToUnitGap",
                increaseToUnitGap)
            .toString();
    Allure.attachment(
        "Aktionsmessung " + fieldLabelText + " " + viewportWidth, actionMeasurementsText);

    for (Locator actionButtonLocator : List.of(clearButtonLocator, increaseButtonLocator)) {
      BoundingBox actionButtonBounds = actionButtonLocator.boundingBox();
      assertEquals(24, actionButtonBounds.width, 0.1, actionMeasurementsText);
      assertEquals(28, actionButtonBounds.height, 0.1, actionMeasurementsText);
      if (actionButtonLocator.locator("vaadin-icon").count() > 0) {
        assertThat(actionButtonLocator.locator("vaadin-icon")).hasCSS("width", "16px");
        assertThat(actionButtonLocator.locator("vaadin-icon")).hasCSS("height", "16px");
      } else {
        assertEquals(
            "16px",
            actionButtonLocator.evaluate("button => getComputedStyle(button, '::before').width"));
        assertEquals(
            "16px",
            actionButtonLocator.evaluate("button => getComputedStyle(button, '::before').height"));
      }
    }
    assertEquals(4, clearToIncreaseGap, 0.1, actionMeasurementsText);
    assertEquals(4, increaseToUnitGap, 0.1, actionMeasurementsText);
  }

  private Locator findDurationIncreaseButton(Locator durationFieldLocator) {
    return durationFieldLocator.locator(
        "[part~=increase-button], .cosmos-duration-suffix .cosmos-duration-step");
  }

  private void assertDurationStepAndClear(
      AdminConfigurationPage configurationPage, String fieldLabelText) {
    Locator durationFieldLocator = configurationPage.duration(fieldLabelText);
    Locator amountInputLocator = configurationPage.input(fieldLabelText);
    String originalAmountText = amountInputLocator.inputValue();
    String increasedAmountText =
        new BigDecimal(originalAmountText).add(BigDecimal.ONE).toPlainString();
    findDurationIncreaseButton(durationFieldLocator).click();
    assertThat(amountInputLocator).hasValue(increasedAmountText);

    Locator clearButtonLocator = durationFieldLocator.locator("[part~=clear-button]");
    assertClearedNumberKeepsCenteredPlaceholder(
        amountInputLocator, clearButtonLocator, originalAmountText);
  }

  private void assertClearedNumberKeepsCenteredPlaceholder(
      Locator amountInputLocator, Locator clearButtonLocator, String originalAmountText) {
    assertThat(amountInputLocator).hasCSS("text-align", "center");
    BoundingBox filledInputBounds = amountInputLocator.boundingBox();
    clearButtonLocator.click();

    assertThat(amountInputLocator).hasValue("");
    assertThat(clearButtonLocator).not().isVisible();
    assertThat(amountInputLocator.and(browserPage.locator("input:placeholder-shown"))).isVisible();
    assertThat(amountInputLocator).hasCSS("text-align", "center");
    assertEquals(
        "rgb(124, 138, 160)",
        amountInputLocator.evaluate("input => getComputedStyle(input, '::placeholder').color"));
    BoundingBox emptyInputBounds = amountInputLocator.boundingBox();
    assertEquals(
        filledInputBounds.x + filledInputBounds.width / 2,
        emptyInputBounds.x + emptyInputBounds.width / 2,
        1);

    amountInputLocator.fill(originalAmountText);
    amountInputLocator.press("Tab");
    assertThat(amountInputLocator).hasValue(originalAmountText);
    assertThat(clearButtonLocator).isVisible();
  }

  @Test
  @Tag("component-follow-up")
  @DisplayName("Zeitfelder behalten Zahl, Tastaturbedienung und gültige Einheiten")
  void durationVariantsKeepKeyboardAndValidationContracts() {
    LoginPage loginPage = new LoginPage(browserPage);
    loginPage.open();
    loginPage.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    AdminConfigurationPage configurationPage = new AdminConfigurationPage(browserPage);

    for (int viewportWidth : new int[] {1280, 390}) {
      browserPage.setViewportSize(viewportWidth, 1000);
      configurationPage.open(AdminConfigGroup.SECURITY);
      String durationLabelText = uiTexts.getAdminConfigEntryRememberMeDuration();
      Locator amountInputLocator = configurationPage.input(durationLabelText);
      String originalAmountText = amountInputLocator.inputValue();

      assertInvalidDurationCanBeCorrected(configurationPage, durationLabelText, originalAmountText);
      checkUnitKeyboardSelectionAndRestoreDays(configurationPage, durationLabelText, viewportWidth);
      checkDurationDraftAcrossLanguagesAndRestore(
          configurationPage, durationLabelText, originalAmountText);
      assertPreciseDurationPersistence(configurationPage);
      assertInactivityControlsAndRestrictedUnits(configurationPage);
    }
  }

  private void assertInvalidDurationCanBeCorrected(
      AdminConfigurationPage configurationPage,
      String durationLabelText,
      String originalAmountText) {
    Locator amountInputLocator = configurationPage.input(durationLabelText);
    amountInputLocator.fill("0");
    amountInputLocator.press("Tab");
    configurationPage.submit();
    assertThat(browserPage.locator("vaadin-notification-card:not([closing])"))
        .containsText(uiTexts.getAdminConfigDurationInvalid());
    browserPage
        .locator("vaadin-notification-card:not([closing])")
        .getByRole(AriaRole.BUTTON)
        .click();
    assertThat(configurationPage.duration(durationLabelText).locator("vaadin-integer-field"))
        .hasAttribute("invalid", "");
    amountInputLocator.fill(originalAmountText);
    amountInputLocator.press("Tab");
  }

  private void checkUnitKeyboardSelectionAndRestoreDays(
      AdminConfigurationPage configurationPage, String durationLabelText, int viewportWidth) {
    Locator amountInputLocator = configurationPage.input(durationLabelText);
    String originalAmountText = amountInputLocator.inputValue();
    Locator unitSelectLocator =
        configurationPage.duration(durationLabelText).locator("vaadin-select");
    Locator unitValueButtonLocator = unitSelectLocator.locator("vaadin-select-value-button");
    unitValueButtonLocator.focus();
    unitValueButtonLocator.press("Space");
    Locator firstUnitOptionLocator =
        browserPage.getByRole(
            AriaRole.OPTION,
            new Page.GetByRoleOptions()
                .setName(uiTexts.getAdminConfigDurationUnitSeconds())
                .setExact(true));
    assertThat(firstUnitOptionLocator).isVisible();
    firstUnitOptionLocator.press("Home");
    firstUnitOptionLocator.press("Enter");
    assertThat(amountInputLocator).hasValue(originalAmountText);
    assertThat(unitSelectLocator.locator("vaadin-select-value-button"))
        .hasText(uiTexts.getAdminConfigDurationUnitSeconds());
    unitValueButtonLocator.press("Space");
    browserPage.keyboard().press("Escape");
    assertThat(firstUnitOptionLocator).not().isVisible();
    unitValueButtonLocator.press("Tab");
    assertThat(unitValueButtonLocator).not().isFocused();
    AllurePlaywright.attachScreenshot("Zeitfeld per Tastatur " + viewportWidth, browserPage);
    unitSelectLocator.click();
    browserPage
        .getByRole(
            AriaRole.OPTION,
            new Page.GetByRoleOptions()
                .setName(uiTexts.getAdminConfigDurationUnitDays())
                .setExact(true))
        .click();
  }

  private void checkDurationDraftAcrossLanguagesAndRestore(
      AdminConfigurationPage configurationPage,
      String durationLabelText,
      String originalAmountText) {
    Locator amountInputLocator = configurationPage.input(durationLabelText);
    String draftAmountText = Integer.toString(Integer.parseInt(originalAmountText) + 1);
    amountInputLocator.fill(draftAmountText);
    amountInputLocator.press("Tab");
    UiTexts alternateUiTexts = new UiTexts(uiTexts.language().equals("de") ? "en" : "de");
    new UserMenu(browserPage).selectLanguage(alternateUiTexts.language());
    String translatedDurationLabelText = alternateUiTexts.getAdminConfigEntryRememberMeDuration();
    assertThat(
            configurationPage
                .duration(translatedDurationLabelText)
                .locator("vaadin-select-value-button"))
        .hasText(alternateUiTexts.getAdminConfigDurationUnitDays());
    assertThat(configurationPage.input(translatedDurationLabelText)).hasValue(draftAmountText);
    new UserMenu(browserPage).selectTestLanguage();
    assertThat(amountInputLocator).hasValue(draftAmountText);
    amountInputLocator.fill(originalAmountText);
    amountInputLocator.press("Tab");
  }

  private void assertPreciseDurationPersistence(AdminConfigurationPage configurationPage) {
    configurationPage.open(AdminConfigGroup.USER_STATUS);
    String preciseDurationLabelText = uiTexts.getAdminConfigEntryUserAutoAbsentTimeout();
    java.math.BigDecimal originalPreciseDurationSeconds =
        configurationPage.seconds(preciseDurationLabelText);
    configurationPage.enterSeconds(preciseDurationLabelText, new java.math.BigDecimal("120.125"));
    assertEquals(
        0,
        new java.math.BigDecimal("120.125")
            .compareTo(configurationPage.seconds(preciseDurationLabelText)));
    configurationPage.save();
    configurationPage.reload();
    assertEquals(
        0,
        new java.math.BigDecimal("120.125")
            .compareTo(configurationPage.seconds(preciseDurationLabelText)));
    configurationPage.enterSeconds(preciseDurationLabelText, originalPreciseDurationSeconds);
    configurationPage.save();
  }

  private void assertInactivityControlsAndRestrictedUnits(
      AdminConfigurationPage configurationPage) {
    configurationPage.open(AdminConfigGroup.TWO_FACTOR);
    Locator inactivitySwitchLocator =
        browserPage.getByRole(
            AriaRole.SWITCH,
            new Page.GetByRoleOptions()
                .setName(uiTexts.getTwoFactorInactivityEnabled())
                .setExact(true));
    boolean wasInactivityEnabled = inactivitySwitchLocator.isChecked();
    inactivitySwitchLocator.setChecked(false);
    Locator inactivityUnitLocator = browserPage.locator(".cosmos-duration-field vaadin-select");
    assertThat(inactivityUnitLocator).hasAttribute("disabled", "");
    inactivitySwitchLocator.setChecked(true);
    assertThat(inactivityUnitLocator).not().hasAttribute("disabled", "");
    inactivityUnitLocator.click();
    assertThat(browserPage.getByRole(AriaRole.OPTION)).hasCount(3);
    assertThat(
            browserPage.getByRole(
                AriaRole.OPTION,
                new Page.GetByRoleOptions()
                    .setName(uiTexts.getAdminConfigDurationUnitSeconds())
                    .setExact(true)))
        .hasCount(0);
    browserPage.keyboard().press("Escape");
    inactivitySwitchLocator.setChecked(wasInactivityEnabled);
  }

  private void assertDurationUnitOverlayStyle(Locator unitSelectLocator, int viewportWidth) {
    unitSelectLocator.click();
    Locator selectedUnitOptionLocator =
        browserPage.getByRole(
            AriaRole.OPTION,
            new Page.GetByRoleOptions()
                .setName(uiTexts.getAdminConfigDurationUnitDays())
                .setExact(true));
    assertThat(selectedUnitOptionLocator).isVisible();
    Locator unitOverlayLocator = unitSelectLocator.locator("[part=overlay]");
    assertThat(unitOverlayLocator).hasCSS("background-color", "rgb(9, 17, 32)");
    assertThat(unitOverlayLocator).hasCSS("border-radius", "10px");
    assertThat(unitOverlayLocator).hasCSS("opacity", "1");

    String optionMeasurementsText =
        (String)
            selectedUnitOptionLocator.evaluate(
                """
                option => {
                  const ancestry = [];
                  for (let element = option; element; element = element.parentElement) {
                    const style = getComputedStyle(element);
                    ancestry.push({tag:element.localName, classes:element.className,
                      theme:element.getAttribute('theme'), height:element.getBoundingClientRect().height,
                      font:style.font, padding:style.padding, border:style.border,
                      background:style.backgroundColor});
                  }
                  return JSON.stringify(ancestry, null, 2);
                }
                """);
    Allure.attachment(
        "Einheitenoverlay " + viewportWidth, "application/json", optionMeasurementsText);
    AllurePlaywright.attachScreenshot("Einheitenoverlay " + viewportWidth, browserPage);

    assertThat(selectedUnitOptionLocator).hasCSS("font-size", "12px");
    assertThat(selectedUnitOptionLocator).hasCSS("font-weight", "500");
    assertThat(selectedUnitOptionLocator).hasCSS("border-radius", "8px");
    assertThat(selectedUnitOptionLocator).hasCSS("border-top-width", "1px");
    assertThat(selectedUnitOptionLocator).hasCSS("border-top-color", "rgb(0, 119, 182)");
    assertThat(selectedUnitOptionLocator).hasCSS("background-color", "rgb(18, 35, 66)");
    assertThat(selectedUnitOptionLocator.locator("[part=checkmark]"))
        .hasCSS("color", "rgb(56, 189, 248)");
    selectedUnitOptionLocator.press("Escape");
    assertThat(selectedUnitOptionLocator).not().isVisible();
  }

  @Test
  @Tag("scrollbar-style")
  @Tag("form-details")
  @DisplayName("COMPONENT-06: Globale Scrollleisten teilen Stil und erhalten die native Bedienung")
  @Description(
      "Synthetische native Komponenten in der isolierten Anwendung prüfen: Dokument, Chat-Scroller, TextArea und Dialog erhalten den gemeinsamen Himmelblau-Strich ohne Pfeile; das innere Grid-Raster erbt dieselbe Farbe. Chatabstände bleiben lokal; horizontales und vertikales Scrollen sowie Systemkontrast bleiben bedienbar.")
  void globalScrollbarsPreserveNativeScrollingAndLocalChatSpacing() {
    signIn();
    browserPage.navigate("/admin?section=support");

    assertThat(browserPage.locator("#support-ticket-categories")).isVisible();
    browserPage.evaluate(
        """
        async () => {
          await Promise.all(['vaadin-scroller', 'vaadin-grid', 'vaadin-grid-column',
            'vaadin-text-area', 'vaadin-dialog'].map(tag => customElements.whenDefined(tag)));
          const fixture = document.createElement('vaadin-vertical-layout');
          fixture.id = 'scrollbar-test';
          fixture.className = 'cosmos-theme cosmos-reference-portal';
          fixture.style.cssText = 'position:fixed;inset:80px 16px 16px;z-index:100;background:#091120;padding:16px;gap:16px;overflow:auto';
          const chat = document.createElement('vaadin-scroller');
          chat.id = 'scrollbar-test-chat';
          chat.className = 'support-message-scroll';
          chat.style.cssText = 'height:130px;min-height:130px;width:100%';
          const thread = document.createElement('vaadin-vertical-layout');
          thread.className = 'support-message-thread';
          for (let index = 0; index < 12; index++) {
            const line = document.createElement('span');
            line.textContent = `Synthetic message ${index + 1}`;
            thread.append(line);
          }
          chat.append(thread);
          const grid = document.createElement('vaadin-grid');
          grid.id = 'scrollbar-test-grid';
          grid.style.cssText = 'height:130px;min-height:130px;width:100%';
          const column = document.createElement('vaadin-grid-column');
          column.header = 'Synthetic rows';
          column.width = '2000px';
          column.flexGrow = 0;
          column.renderer = (root, _column, data) => root.textContent = data.item.label;
          grid.append(column);
          grid.items = Array.from({length:30}, (_, index) => ({label:`Row ${index + 1}`}));
          const field = document.createElement('vaadin-text-area');
          field.id = 'scrollbar-test-field';
          field.label = 'Synthetic text';
          field.style.cssText = 'width:100%;max-height:130px;min-height:130px';
          field.value = Array.from({length:30}, (_, index) => `Line ${index + 1}`).join('\\n');
          fixture.append(chat, grid, field);
          const spacer = document.createElement('div');
          spacer.id = 'scrollbar-test-spacer';
          spacer.style.height = '200vh';
          document.body.append(fixture, spacer);
        }
        """);
    try {
      for (int width : new int[] {1280, 390}) {
        browserPage.setViewportSize(width, 900);
        Locator chat = browserPage.locator("#scrollbar-test-chat");
        Locator grid = browserPage.locator("#scrollbar-test-grid #table");
        Locator field = browserPage.locator("#scrollbar-test-field [part=input-field]");
        for (Locator area : List.of(chat, field)) {
          assertScrollbarStyleAndMovement(area);
        }
        assertInheritedGridScrollbarColorAndMovement(grid);
        browserPage.waitForCondition(() ->
            (Boolean)
                browserPage.evaluate(
                    """
                    () => {
                      const thread = document.querySelector('#scrollbar-test-chat .support-message-thread');
                      const grid = document.querySelector('#scrollbar-test-grid');
                      return getComputedStyle(thread).paddingRight === '23px'
                        && getComputedStyle(grid).paddingRight === '0px'
                        && getComputedStyle(document.documentElement, '::-webkit-scrollbar').width
                          === getComputedStyle(document.documentElement).getPropertyValue('--cosmos-scrollbar-size').trim();
                    }
                    """));
        grid.evaluate("element => element.scrollLeft = 40");

        assertTrue((Boolean) grid.evaluate("element => element.scrollLeft > 0"));
        chat.evaluate("element => { element.scrollTop = 0; element.focus(); }");
        browserPage.keyboard().press("PageDown");
        browserPage.waitForCondition(
            () -> (Boolean) chat.evaluate("element => element.scrollTop > 0"));
        AllurePlaywright.attachScreenshot(
            "Gemeinsame native Scrollleisten bei " + width + "px", browserPage);
        browserPage.evaluate(
            """
            () => {
              const dialog = document.createElement('vaadin-dialog');
              dialog.id = 'scrollbar-test-dialog';
              dialog.className = 'cosmos-reference-overlay';
              dialog.setAttribute('theme', 'cosmos');
              dialog.height = '180px';
              dialog.width = '280px';
              // Höher als der Viewport: Aura begrenzt die Dialogfläche auf 100 %, der Inhalt scrollt.
              dialog.renderer = root => {
                const content = document.createElement('vaadin-vertical-layout');
                content.style.minHeight = '2000px';
                content.textContent = 'Synthetic dialog content';
                root.replaceChildren(content);
              };
              document.body.append(dialog);
              dialog.opened = true;
            }
            """);
        Locator content = browserPage.locator("#scrollbar-test-dialog [part=content]").first();
        assertScrollbarStyleAndMovement(content);
        AllurePlaywright.attachScreenshot(
            "Native Dialogscrollleiste bei " + width + "px", browserPage);
        browserPage.evaluate("document.querySelector('#scrollbar-test-dialog').remove()");
      }
      browserPage.evaluate("window.scrollTo(0, 50)");

      assertTrue((Boolean) browserPage.evaluate("window.scrollY > 0"));
      browserPage.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.ACTIVE));

      assertThat(browserPage.locator("#scrollbar-test-chat")).hasCSS("scrollbar-color", "auto");
      assertThat(browserPage.locator("#scrollbar-test-chat")).hasCSS("scrollbar-width", "auto");
    } finally {
      browserPage.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.NONE));
      browserPage.evaluate(
          "document.querySelectorAll('[id^=scrollbar-test]').forEach(element => element.remove())");
    }
  }

  private void assertScrollbarStyleAndMovement(Locator area) {
    assertThat(area).isVisible();
    // Prüfwerte der gemeinsamen Scrollleiste; ok fasst alle Bedingungen zusammen.
    String scrollbarStyleValues =
        """
        element => {
          const root = document.documentElement;
          const thumb = getComputedStyle(element, '::-webkit-scrollbar-thumb');
          // Sollfarbe aus dem Token auflösen; das Dokument selbst gilt als überfahren.
          const probe = document.createElement('div');
          probe.style.color = getComputedStyle(root).getPropertyValue('--cosmos-scrollbar-thumb');
          document.body.append(probe);
          const expectedThumb = getComputedStyle(probe).color;
          probe.remove();
          const values = {
            scrollHeight: element.scrollHeight,
            clientHeight: element.clientHeight,
            width: getComputedStyle(element, '::-webkit-scrollbar').width,
            expectedWidth: getComputedStyle(element).getPropertyValue('--cosmos-scrollbar-size').trim(),
            thumb: thumb.backgroundColor,
            expectedThumb,
            inset: thumb.borderTopWidth,
            expectedInset: getComputedStyle(root).getPropertyValue('--cosmos-scrollbar-inset').trim(),
            buttons: getComputedStyle(element, '::-webkit-scrollbar-button').display
          };
          values.ok = values.scrollHeight > values.clientHeight
            && values.width === values.expectedWidth
            && values.thumb === values.expectedThumb
            && values.inset === values.expectedInset
            && values.buttons === 'none';
          return values;
        }\
        """;
    // Ruhezustand prüfen: Ein Zeiger über der Leiste würde die Hover-Farbe auslösen.
    browserPage.mouse().move(0, 0);
    try {
      browserPage.waitForCondition(
          () -> (Boolean) area.evaluate("element => (" + scrollbarStyleValues + ")(element).ok"));
    } catch (TimeoutError _) {
      // Die Einzelwerte machen sichtbar, welche Bedingung der gemeinsamen Leiste fehlt.
      fail("Cosmos-Scrollleiste nicht wie erwartet: " + area.evaluate(scrollbarStyleValues));
    }
    assertWheelScrolling(area);
  }

  /**
   * Das innere Grid-Raster hat keinen öffentlichen Part. Es erbt die Cosmos-Farbe über
   * scrollbar-color; Breite und Form zeichnet der Browser (siehe docs/theme.md).
   */
  private void assertInheritedGridScrollbarColorAndMovement(Locator gridTable) {
    assertThat(gridTable).isVisible();
    browserPage.waitForCondition(() ->
        (Boolean)
            gridTable.evaluate(
                """
                element => {
                  const probe = document.createElement('div');
                  probe.style.color = getComputedStyle(document.documentElement)
                    .getPropertyValue('--cosmos-scrollbar-thumb');
                  document.body.append(probe);
                  const thumbColor = getComputedStyle(probe).color;
                  probe.remove();
                  return element.scrollHeight > element.clientHeight
                    && getComputedStyle(element).scrollbarColor.startsWith(thumbColor);
                }
                """));
    assertWheelScrolling(gridTable);
  }

  private void assertWheelScrolling(Locator area) {
    area.evaluate("element => element.scrollTop = 0");
    area.hover();
    browserPage.mouse().wheel(0, 80);
    browserPage.waitForCondition(() -> (Boolean) area.evaluate("element => element.scrollTop > 0"));
  }

  @Test
  @DisplayName("COMPONENT-01: Rollenfilter erhält Auswahl und Anzahl in Anlage und Bearbeitung")
  @Tag("form-details")
  @Description(
      "Auf Desktop und Smartphone in beiden Dialogen eine ausgewählte Rolle erhalten, nach einer anderen Rolle tippen und sie ergänzen. Eine erfolglose Suche und das Leeren des Suchtexts ändern die zwei ausgewählten Rollen nicht. Nach Wiederöffnen sind beide Optionen ausgewählt; Abbrechen speichert nichts. Bei Rollen und zweizeiligen Empfängern bleiben die vollständigen Rahmen mit 2 px Abstand sichtbar.")
  void roleSearchPreservesSelectedRolesInBothDialogs() {
    LoginPage loginPage = new LoginPage(browserPage);
    loginPage.open();
    loginPage.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();

    AdminUsersPage adminUsersPage = new AdminUsersPage(browserPage);
    String testUserEmail = "role-search-" + UUID.randomUUID() + "@example.test";
    adminUsersPage.createActiveUser(testUserEmail, BrowserTestAccounts.USER_PASSWORD);
    String recipientPrefix = "overlay-recipient-" + UUID.randomUUID();
    for (int recipientIndex = 0; recipientIndex < 12; recipientIndex++) {
      adminUsersPage.createActiveUser(
          recipientPrefix + "-" + recipientIndex + "@example.test",
          BrowserTestAccounts.USER_PASSWORD);
    }

    for (int viewportWidth : new int[] {1280, 390}) {
      browserPage.setViewportSize(viewportWidth, 900);
      adminUsersPage.open();
      assertRoleSearchPreservesSelection(adminUsersPage.openCreate());
      assertRoleSearchPreservesSelection(adminUsersPage.openEdit(testUserEmail));
      assertRecipientSelectionFramesAndSpacing(viewportWidth, recipientPrefix + "-0@example.test");
    }
  }

  private void assertRoleSearchPreservesSelection(AdminUserDialog adminUserDialog) {
    if (adminUserDialog.deleteButton().isVisible()) {
      adminUserDialog.deleteButton().hover();
      assertThat(adminUserDialog.deleteButton()).hasCSS("color", "rgb(252, 165, 165)");
      assertThat(adminUserDialog.deleteButton())
          .hasCSS("border-color", "color(srgb 0.972549 0.443137 0.443137 / 0.4)");
    }
    adminUserDialog.setRole(uiTexts.getRoleUser(), true);
    Locator roleSearchInputLocator = adminUserDialog.root().locator(".admin-user-roles input");
    Locator roleSummaryLocator = adminUserDialog.root().locator(".admin-user-role-summary");

    assertThat(roleSummaryLocator).hasText(uiTexts.getAdminUsersSelectedRoles(1));
    roleSearchInputLocator.fill(uiTexts.getRoleAdmin().substring(0, 4));
    Locator administratorOptionLocator =
        browserPage.getByRole(
            AriaRole.OPTION,
            new Page.GetByRoleOptions().setName(uiTexts.getRoleAdmin()).setExact(true));

    assertThat(administratorOptionLocator).isVisible();
    administratorOptionLocator.click();

    assertThat(roleSummaryLocator).hasText(uiTexts.getAdminUsersSelectedRoles(2));
    roleSearchInputLocator.fill("no-role-matches-this");

    assertThat(browserPage.locator("vaadin-multi-select-combo-box-item:visible")).hasCount(0);
    assertThat(roleSummaryLocator).hasText(uiTexts.getAdminUsersSelectedRoles(2));
    roleSearchInputLocator.fill("");

    assertThat(adminUserDialog.role(uiTexts.getRoleUser())).hasAttribute("aria-selected", "true");
    assertThat(administratorOptionLocator).hasAttribute("aria-selected", "true");
    roleSearchInputLocator.press("Escape");
    roleSearchInputLocator.click();

    Locator userOptionLocator = adminUserDialog.role(uiTexts.getRoleUser());
    assertThat(userOptionLocator).hasAttribute("aria-selected", "true");
    assertThat(administratorOptionLocator).hasAttribute("aria-selected", "true");
    assertThat(roleSummaryLocator).hasText(uiTexts.getAdminUsersSelectedRoles(2));
    assertSelectedOptionFramesAndSpacing(administratorOptionLocator, userOptionLocator);

    AllurePlaywright.attachScreenshot("Rollen nach Suche und Wiederöffnen", browserPage);
    adminUserDialog.cancel();
  }

  private void assertRecipientSelectionFramesAndSpacing(int viewportWidth, String matchingEmail) {
    NotificationComposePage notificationComposePage = new NotificationComposePage(browserPage);
    notificationComposePage.open();
    Locator recipientSearchInputLocator = notificationComposePage.recipients().locator("input");
    browserPage.setViewportSize(viewportWidth, 560);
    recipientSearchInputLocator.click();
    Locator scroller = CosmosFollowUpChecks.assertChoiceScrollerPadding(browserPage, true);
    assertWheelScrolling(scroller);
    scroller.evaluate("scroller => scroller.scrollTop = 0");
    recipientSearchInputLocator.fill(matchingEmail);
    Locator filteredOption = browserPage.locator("vaadin-multi-select-combo-box-item:visible");
    assertThat(filteredOption).hasCount(1);
    assertThat(filteredOption).containsText(matchingEmail);
    recipientSearchInputLocator.fill("");
    Locator recipientOptionsLocator =
        browserPage.locator("vaadin-multi-select-combo-box-item:visible");
    assertThat(recipientOptionsLocator.nth(1)).isVisible();
    recipientOptionsLocator.nth(0).click();
    recipientOptionsLocator.nth(1).click();

    Locator selectedRecipientOptionsLocator =
        browserPage.locator("vaadin-multi-select-combo-box-item[aria-selected=true]:visible");
    assertThat(selectedRecipientOptionsLocator).hasCount(2);
    assertSelectedOptionFramesAndSpacing(
        selectedRecipientOptionsLocator.nth(0), selectedRecipientOptionsLocator.nth(1));
    AllurePlaywright.attachScreenshot(
        "Vollständige Empfängerrahmen bei " + viewportWidth + " px", browserPage);
    recipientSearchInputLocator.press("Escape");
    browserPage.setViewportSize(viewportWidth, 900);
  }

  private void assertSelectedOptionFramesAndSpacing(
      Locator firstSelectedOptionLocator, Locator secondSelectedOptionLocator) {
    CosmosFollowUpChecks.assertChoiceScrollerPadding(browserPage, true);
    BoundingBox firstOptionBounds = firstSelectedOptionLocator.boundingBox();
    BoundingBox secondOptionBounds = secondSelectedOptionLocator.boundingBox();
    double paintedBottomOffset =
        ((Number)
                firstSelectedOptionLocator.evaluate(
                    "element => parseFloat(getComputedStyle(element, '::after').bottom) + parseFloat(getComputedStyle(element).borderBottomWidth)"))
            .doubleValue();
    double paintedTopOffset =
        ((Number)
                secondSelectedOptionLocator.evaluate(
                    "element => parseFloat(getComputedStyle(element, '::after').top) + parseFloat(getComputedStyle(element).borderTopWidth)"))
            .doubleValue();
    for (Locator selectedOptionLocator :
        List.of(firstSelectedOptionLocator, secondSelectedOptionLocator)) {
      assertThat(selectedOptionLocator.locator("[part=checkmark]")).isVisible();
      assertTrue(
          (Boolean)
              selectedOptionLocator.evaluate(
                  """
                  element => {
                    const frame = getComputedStyle(element, '::after');
                    return ['top', 'right', 'bottom', 'left'].every(side =>
                      parseFloat(frame[side]) >= 0 && parseFloat(frame.getPropertyValue(`border-${side}-width`)) > 0);
                  }
                  """));
    }
    assertEquals(
        2,
        secondOptionBounds.y
            - firstOptionBounds.y
            - firstOptionBounds.height
            + paintedBottomOffset
            + paintedTopOffset,
        0.5);
  }

  @Test
  @Tag("form-details")
  @DisplayName(
      "Auswahllisten und feste Tage bleiben auf Desktop und Smartphone vollständig sichtbar")
  void choiceHoverPaginationAndDaySuffixUseTheSharedFieldStyle() {
    LoginPage loginPage = new LoginPage(browserPage);
    loginPage.open();
    loginPage.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    AdminUsersPage adminUsersPage = new AdminUsersPage(browserPage);
    AdminConfigurationPage configurationPage = new AdminConfigurationPage(browserPage);

    for (int viewportWidth : new int[] {1280, 390}) {
      browserPage.setViewportSize(viewportWidth, 900);
      adminUsersPage.open();
      assertActionAndGridHoverColors(adminUsersPage);
      assertPaginationOverlayContainsSelection(viewportWidth);

      assertChoiceHoverUsesSharedColor(adminUsersPage.openCreate(), viewportWidth);

      new AccountPage(browserPage).open();
      for (String profileChoiceId : List.of("account-gender", "account-language")) {
        assertSingleChoiceOverlay(browserPage.locator("#" + profileChoiceId));
      }

      browserPage.navigate(path(ADMIN_ROUTE));
      configurationPage.open(AdminConfigGroup.TWO_FACTOR);
      Locator factorRule =
          browserPage.locator("vaadin-combo-box[id^=two-factor-rule-]:visible").first();
      assertSingleChoiceOverlay(factorRule);

      configurationPage.open(AdminConfigGroup.NOTIFICATIONS);
      assertFixedDaySuffixesRemainInsideFields(configurationPage);
      AllurePlaywright.attachScreenshot(
          "Feste Tage im nativen Zahlenfeld bei " + viewportWidth + " px", browserPage);
    }
  }

  private void assertSingleChoiceOverlay(Locator choice) {
    choice.locator("[part~=toggle-button]").click();
    CosmosFollowUpChecks.assertSingleChoiceSpacing(browserPage);
    choice.locator("input").press("Escape");
  }

  private void assertActionAndGridHoverColors(AdminUsersPage adminUsersPage) {
    Locator createUserButtonLocator =
        browserPage.getByRole(
            AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName(uiTexts.getAdminUsersAdd()).setExact(true));
    createUserButtonLocator.hover();
    assertThat(createUserButtonLocator).hasCSS("background-color", "rgb(0, 103, 158)");

    Locator refreshUsersButtonLocator =
        browserPage.getByRole(
            AriaRole.BUTTON,
            new Page.GetByRoleOptions()
                .setName(uiTexts.getAdminUsersRefreshTooltip())
                .setExact(true));
    refreshUsersButtonLocator.hover();
    assertThat(refreshUsersButtonLocator).hasCSS("background-color", "rgb(21, 34, 56)");

    Locator firstUserCellLocator =
        adminUsersPage.grid().locator("[part~=body-cell]:visible").first();
    firstUserCellLocator.hover();
    // Im Benutzergrid zeichnet die ganze Zeile den Hover; ihre Zellen sind transparent.
    assertThat(firstUserCellLocator.locator("..")).hasCSS("background-color", "rgb(21, 34, 56)");
  }

  private void assertPaginationOverlayContainsSelection(int viewportWidth) {
    Locator pageSizeComboBoxLocator = browserPage.locator(".portal-pagination-size");
    assertThat(pageSizeComboBoxLocator.locator("[part=input-field]")).hasCSS("height", "32px");
    pageSizeComboBoxLocator.locator("[part~=toggle-button]").click();
    CosmosFollowUpChecks.assertSingleChoiceSpacing(browserPage);
    Locator selectedPageSizeRowLocator =
        browserPage.locator(
            "vaadin-combo-box-item.portal-pagination-option[selected] .cosmos-reference-option-row");
    assertThat(selectedPageSizeRowLocator).isVisible();
    assertThat(selectedPageSizeRowLocator).hasCSS("height", "32px");
    BoundingBox overlayBounds = pageSizeComboBoxLocator.locator("[part=overlay]").boundingBox();
    BoundingBox selectionBounds = selectedPageSizeRowLocator.boundingBox();
    assertTrue(overlayBounds.width >= 88);
    assertTrue(selectionBounds.x >= overlayBounds.x);
    assertTrue(selectionBounds.x + selectionBounds.width <= overlayBounds.x + overlayBounds.width);
    AllurePlaywright.attachScreenshot(
        "Vollständige Seitengrößenauswahl bei " + viewportWidth + " px", browserPage);
    pageSizeComboBoxLocator.locator("input").press("Escape");
  }

  private void assertChoiceHoverUsesSharedColor(
      AdminUserDialog adminUserDialog, int viewportWidth) {
    Locator genderComboBoxLocator = adminUserDialog.root().locator("vaadin-combo-box").first();
    genderComboBoxLocator.locator("[part~=toggle-button]").click();
    CosmosFollowUpChecks.assertSingleChoiceSpacing(browserPage);
    Locator hoveredChoiceRowLocator =
        browserPage
            .locator(
                "vaadin-combo-box-item.cosmos-reference-option:visible:not([selected]) .cosmos-reference-option-row")
            .first();
    hoveredChoiceRowLocator.hover();
    assertThat(hoveredChoiceRowLocator).hasCSS("background-color", "rgb(21, 34, 56)");
    assertThat(hoveredChoiceRowLocator).hasCSS("height", "42px");
    Locator hoveredChoiceItemLocator = hoveredChoiceRowLocator.locator("..");
    assertThat(hoveredChoiceItemLocator).hasCSS("background-color", "rgba(0, 0, 0, 0)");
    assertThat(hoveredChoiceItemLocator).hasCSS("outline-style", "none");
    AllurePlaywright.attachScreenshot("Blauer Hover bei " + viewportWidth + " px", browserPage);
    genderComboBoxLocator.locator("input").press("Escape");
    Locator firstNameInputSurfaceLocator =
        adminUserDialog.root().locator("vaadin-text-field").first().locator("[part=input-field]");
    adminUserDialog.firstName().hover();
    assertThat(firstNameInputSurfaceLocator).hasCSS("background-color", "rgb(21, 34, 56)");
    assertCalendarHoverPreservesSelectedDate(adminUserDialog);
    adminUserDialog.cancel();
  }

  private void assertCalendarHoverPreservesSelectedDate(AdminUserDialog adminUserDialog) {
    Locator birthdayInputLocator =
        adminUserDialog
            .root()
            .getByLabel(
                uiTexts.getRegistrationBirthDateField(),
                new Locator.GetByLabelOptions().setExact(true));
    birthdayInputLocator.fill(uiTexts.dateInput(LocalDate.of(2020, 6, 15)));
    adminUserDialog.firstName().click();
    adminUserDialog.root().locator("vaadin-date-picker [part~=toggle-button]").click();
    Locator selectedMonthLocator =
        browserPage
            .locator("vaadin-month-calendar")
            .filter(
                new Locator.FilterOptions()
                    .setHas(browserPage.locator("[part~=date][part~=selected]")));
    Locator selectedDateLocator = selectedMonthLocator.locator("[part~=date][part~=selected]");
    assertThat(selectedDateLocator).hasText("15");
    String selectedDateBackgroundColor =
        (String)
            selectedDateLocator.evaluate(
                "element => getComputedStyle(element, '::after').backgroundColor");

    // Exakt der benachbarte Tag; keine andere Zahl aus den gerenderten Monatszeilen.
    Locator unselectedDateLocator =
        selectedMonthLocator
            .locator("[part~=date]:not([part~=selected])")
            .filter(new Locator.FilterOptions().setHasText(Pattern.compile("^14$")));
    unselectedDateLocator.hover();
    assertEquals(
        "rgb(21, 34, 56)",
        unselectedDateLocator.evaluate(
            "element => getComputedStyle(element, '::after').backgroundColor"));
    assertEquals(
        selectedDateBackgroundColor,
        selectedDateLocator.evaluate(
            "element => getComputedStyle(element, '::after').backgroundColor"));
    AllurePlaywright.attachScreenshot("Kalenderhover mit unveränderter Datumauswahl", browserPage);
    Locator calendarDialogLocator =
        browserPage.getByRole(
            AriaRole.DIALOG,
            new Page.GetByRoleOptions().setName(uiTexts.getDatePickerCalendar()).setExact(true));
    Locator cancelButton = calendarButton(calendarDialogLocator, uiTexts.getDatePickerCancel());
    assertCalendarActions(calendarDialogLocator, cancelButton);
    assertCalendarYearColumn(calendarDialogLocator);
    cancelButton.click();
    assertThat(calendarDialogLocator).not().isVisible();
  }

  private Locator calendarButton(Locator calendarDialog, String label) {
    return calendarDialog.getByRole(
        AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(label).setExact(true));
  }

  private void assertCalendarActions(Locator calendarDialog, Locator cancelButton) {
    Locator todayButton = calendarButton(calendarDialog, uiTexts.getDatePickerToday());
    for (Locator button : List.of(todayButton, cancelButton)) {
      assertThat(button).hasCSS("background-color", "rgba(0, 0, 0, 0)");
      assertThat(button).hasCSS("border-top-color", "rgba(0, 0, 0, 0)");
      assertThat(button).hasCSS("color", "rgb(56, 189, 248)");
      button.focus();
      assertThat(button).isFocused();
    }
  }

  private void assertCalendarYearColumn(Locator calendarDialog) {
    Locator yearToggle = calendarDialog.locator("[part=years-toggle-button]");
    if (yearToggle.isVisible()) {
      yearToggle.click();
    }
    Locator yearScroller = calendarDialog.locator("vaadin-date-picker-year-scroller");
    Locator currentSelection = yearScroller.locator("vaadin-date-picker-year[selected]");
    assertThat(currentSelection).hasText("2020");
    assertThat(currentSelection.locator("[part=year-number]")).hasCSS("color", "rgb(56, 189, 248)");
    assertEquals(
        "none", currentSelection.evaluate("year => getComputedStyle(year, '::after').content"));
    assertTrue(
        (boolean)
            yearScroller.evaluate(
                """
                scroller => {
                  const bounds = scroller.getBoundingClientRect();
                  const style = getComputedStyle(scroller);
                  return bounds.left >= 0 && bounds.right <= innerWidth + 1
                    && parseFloat(style.borderInlineStartWidth) > 0
                    && style.backgroundColor !== 'rgba(0, 0, 0, 0)';
                }
                """));
  }

  private void assertFixedDaySuffixesRemainInsideFields(AdminConfigurationPage configurationPage) {
    for (String fieldLabel :
        List.of(
            uiTexts.getAdminConfigEntryNotificationInboxRetention(),
            uiTexts.getAdminConfigEntryNotificationHistoryRetention())) {
      Locator dayFieldLocator = configurationPage.duration(fieldLabel);
      Locator unitSuffixLocator =
          dayFieldLocator.locator(
              "vaadin-integer-field > .cosmos-duration-unit-label[slot=suffix]");
      assertThat(unitSuffixLocator).hasText(uiTexts.getAdminConfigDurationUnitDays());
      assertThat(dayFieldLocator.locator("vaadin-select")).hasCount(0);
      BoundingBox inputBounds = dayFieldLocator.locator("[part=input-field]").boundingBox();
      BoundingBox suffixBounds = unitSuffixLocator.boundingBox();
      BoundingBox increaseButtonBounds =
          dayFieldLocator
              .locator("vaadin-integer-field")
              .locator("[part~=increase-button]")
              .boundingBox();
      assertTrue(suffixBounds.x > inputBounds.x);
      assertTrue(suffixBounds.x >= increaseButtonBounds.x + increaseButtonBounds.width);
      assertTrue(suffixBounds.x + suffixBounds.width <= inputBounds.x + inputBounds.width);
      dayFieldLocator.locator("vaadin-integer-field input").focus();
      assertThat(unitSuffixLocator).hasCSS("color", "rgb(255, 255, 255)");
      browserPage
          .getByRole(
              AriaRole.HEADING,
              new Page.GetByRoleOptions()
                  .setName(uiTexts.getAdminConfigGroupLabel(AdminConfigGroup.NOTIFICATIONS))
                  .setExact(true)
                  .setLevel(2))
          .click();
      assertThat(unitSuffixLocator).hasCSS("color", "rgb(148, 163, 184)");
    }
  }

  @Test
  @Tag("switch-style")
  @DisplayName(
      "COMPONENT-02: Konfigurationsswitches bleiben links beschriftet und rechts bedienbar")
  @Description(
      "Alle fünf Boolean-Einstellungen der Sicherheits- und SMTP-Gruppen auf Desktop und Smartphone vermessen: Text links, Switch rechts, Info außerhalb. Label und Schalter schalten denselben Zustand. Der Marker bleibt innerhalb der Bahn vertikal zentriert. Keine Einstellung wird gespeichert.")
  void configurationSwitchGeometryAndBothClickTargets() {
    signIn();
    AdminConfigurationPage settings = new AdminConfigurationPage(browserPage);
    for (int width : new int[] {1280, 390}) {
      browserPage.setViewportSize(width, 900);
      int checkedFields = 0;
      for (AdminConfigGroup group : List.of(AdminConfigGroup.SECURITY, AdminConfigGroup.SMTP)) {
        settings.open(group);
        for (Locator row :
            browserPage.locator(".admin-config-switch .cosmos-config-input-row").all()) {
          Locator toggle = row.locator("vaadin-switch");
          Locator input = toggle.locator("input[slot=input]");
          Locator label = toggle.locator("[id^=config-label-]");
          Locator track = toggle.locator("[part=switch]");
          Locator info = row.locator(".cosmos-config-info");
          BoundingBox textBounds = label.boundingBox(), trackBounds = track.boundingBox();

          assertThat(label).hasCSS("font-size", "12px");
          assertThat(label).hasCSS("font-weight", "500");
          assertTrue(textBounds.x + textBounds.width <= trackBounds.x + 1);
          assertEquals(
              toggle.boundingBox().x + toggle.boundingBox().width,
              trackBounds.x + trackBounds.width,
              1);
          assertEquals(
              textBounds.y + textBounds.height / 2, trackBounds.y + trackBounds.height / 2, 2);
          assertTrue(info.boundingBox().x >= toggle.boundingBox().x + toggle.boundingBox().width);
          assertEquals(
              trackBounds.y + trackBounds.height / 2,
              info.boundingBox().y + info.boundingBox().height / 2,
              1);
          CosmosFollowUpChecks.assertSwitchAppearance(toggle);
          boolean original = input.isChecked();
          label.click();

          assertThat(input)
              .isChecked(
                  new com.microsoft.playwright.assertions.LocatorAssertions.IsCheckedOptions()
                      .setChecked(!original));
          CosmosFollowUpChecks.assertSwitchAppearance(toggle);
          BoundingBox clickTarget = track.boundingBox();
          browserPage
              .mouse()
              .click(clickTarget.x + clickTarget.width / 2, clickTarget.y + clickTarget.height / 2);

          assertThat(input)
              .isChecked(
                  new com.microsoft.playwright.assertions.LocatorAssertions.IsCheckedOptions()
                      .setChecked(original));
          CosmosFollowUpChecks.assertSwitchAppearance(toggle);
          checkedFields++;
        }
      }
      assertEquals(5, checkedFields);
      assertReadonlySwitchUsesAuraColors();
      AllurePlaywright.attachScreenshot("Switchzeilen bei " + width + "px", browserPage);
    }
  }

  private void assertReadonlySwitchUsesAuraColors() {
    browserPage.evaluate(
        """
        label => {
          const control = document.createElement('vaadin-switch');
          control.id = 'browser-readonly-switch';
          control.label = label;
          control.readonly = true;
          control.checked = true;
          document.body.append(control);
        }
        """,
        uiTexts.getLoginRememberMeCheckbox());
    Locator control = browserPage.locator("#browser-readonly-switch");
    try {
      Locator input = control.locator("input[slot=input]");
      Locator track = control.locator("[part=switch]");
      Locator marker = control.locator("[part=marker]");
      assertThat(control).hasAttribute("readonly", "");
      assertThat(track).hasCSS("background-color", "rgba(0, 0, 0, 0)");
      assertThat(marker)
          .hasCSS("background-color", resolveFixtureColor(marker, "--vaadin-text-color-secondary"));
      control.locator("[part=label]").click();
      assertThat(input).isChecked();
      control.evaluate("control => control.checked = false");
      assertThat(marker)
          .hasCSS("background-color", resolveFixtureColor(marker, "--vaadin-text-color-disabled"));
    } finally {
      control.evaluate("control => control.remove()");
    }
  }

  @Test
  @DisplayName(
      "COMPONENT-03: SideNav-Hover verschiebt nichts; aktive Haupt- und Unterpunkte bleiben blau")
  @Tag("form-details")
  @Description(
      "Portal und Admin bei 1280/390px prüfen. Handzeiger, linke Icon-/Textausrichtung, vertikale Mitte und stabile Klickfläche bei Hover. Aktive Hauptpunkte sind 600 statt 500, Unterpunkte 500 statt 400. Hover erhält die aktive blaue Fläche.")
  void navigationStatesAreConsistentInPortalAndAdministration() {
    signIn();
    for (int width : new int[] {1280, 390}) {
      browserPage.setViewportSize(width, 900);
      AdminNavigation navigation = new AdminNavigation(browserPage);
      navigation.openGroup(AdminConfigGroup.GENERAL);
      navigation.openDrawer();
      checkNav(browserPage.locator("#admin-nav-mail_test"), "500", false);
      checkNav(navigation.configuration(), "600", true);
      checkNav(browserPage.locator("#admin-nav-configuration-general"), "500", true);
      checkNav(browserPage.locator("#admin-nav-configuration-security"), "400", false);
      navigation.closeOverlayDrawer();
      browserPage.navigate(path(HOME_ROUTE));

      assertThat(browserPage.locator("#portal-home-account")).isVisible();
      ActionGridChecks.assertActions(browserPage.locator(".portal-home-actions"), 2, width <= 479);
      openPortalNavigation();
      checkNav(browserPage.locator("#account-nav-home"), "600", true);
      checkNav(browserPage.locator("#account-nav-notifications"), "500", false);
      checkNav(browserPage.locator("#account-nav-profile"), "500", false);
      UiActions.complete(
          browserPage, () -> browserPage.locator("#account-nav-profile [part=link]").click());
      openPortalNavigation();
      checkNav(browserPage.locator("#account-tab-profile"), "600", true);
      checkNav(browserPage.locator("#account-tab-security"), "500", false);
      AllurePlaywright.attachScreenshot("Portalnavigation bei " + width + "px", browserPage);
      browserPage.navigate(path(HOME_ROUTE));
      browserPage.navigate(path(ADMIN_ROUTE));
    }
  }

  private void openPortalNavigation() {
    Locator toggle = browserPage.locator("#account-navigation-toggle");

    assertThat(toggle).hasCount(1);
    if (toggle.isVisible() && !"true".equals(toggle.getAttribute("aria-expanded"))) {
      UiActions.complete(browserPage, toggle::click);
    }
    assertThat(browserPage.locator("#account-sidebar")).isVisible();
  }

  private void checkNav(Locator item, String weight, boolean active) {
    Locator link = item.locator(":scope > [part=content] > [part=link]");
    Locator content = item.locator(":scope > [part=content]");

    assertThat(link).hasCSS("cursor", "pointer");
    assertThat(link).hasCSS("font-weight", weight);
    assertThat(link).hasCSS("text-align", "left");
    BoundingBox before = content.boundingBox();
    browserPage.mouse().move(1, 1);
    String background = (String) content.evaluate("el => getComputedStyle(el).backgroundColor");
    link.hover();
    BoundingBox after = content.boundingBox();

    assertEquals(before.x, after.x, 1);
    assertEquals(before.y, after.y, 1);
    assertEquals(before.width, after.width, 1);
    assertEquals(before.height, after.height, 1);
    assertThat(content).hasCSS("background-color", active ? background : "rgb(21, 34, 56)");
    if (active) {
      assertNotEquals("rgba(0, 0, 0, 0)", background);
    }
    BoundingBox icon = item.locator(":scope > vaadin-icon").boundingBox();
    List<?> label =
        (List<?>)
            item.evaluate(
                """
                el => {
                  const node = el.shadowRoot.querySelector('slot:not([name])').assignedNodes({flatten:true})
                    .find(node => node.textContent.trim());
                  const range = document.createRange(); range.selectNodeContents(node);
                  const box = range.getBoundingClientRect();
                  return [box.x, box.y + box.height / 2];
                }
                """);

    assertEquals(8, ((Number) label.get(0)).doubleValue() - icon.x - icon.width, 2);
    assertEquals(icon.y + icon.height / 2, ((Number) label.get(1)).doubleValue(), 2);
    browserPage.mouse().move(1, 1);
  }

  @Test
  @DisplayName("COMPONENT-04: Aktions-Tooltips und seitliche Feldhilfe bleiben unterscheidbar")
  @Description(
      "Auf Desktop und Smartphone die Gestaltung der Aktions-Tooltips bei Hover und Fokus prüfen. Die Mail-Feldhilfe öffnet als eigener Dialog und schließt per Escape.")
  void actionTooltipsAndFieldHelpUseDistinctOverlays() {
    signIn();
    for (int width : new int[] {1280, 390}) {
      browserPage.setViewportSize(width, 900);
      Locator account =
          browserPage
              .locator(".cosmos-topbar")
              .getByRole(
                  AriaRole.BUTTON,
                  new Locator.GetByRoleOptions()
                      .setName(uiTexts.getBaseManageAccountButton())
                      .setExact(true));
      account.hover();
      assertTooltip(browserPage.locator("vaadin-tooltip-overlay[opened] [part=overlay]"));
      browserPage.mouse().move(1, 1);
      account.focus();
      assertTooltip(browserPage.locator("vaadin-tooltip-overlay[opened] [part=overlay]"));
      browserPage.keyboard().press("Escape");
      new AdminMailPage(browserPage).open();
      browserPage
          .getByRole(
              AriaRole.BUTTON,
              new Page.GetByRoleOptions().setName(uiTexts.getAdminMailRecipient()).setExact(true))
          .click();
      Locator fieldHelp = browserPage.locator("#admin-mail-field-help[opened]");

      assertThat(fieldHelp.locator("vaadin-dialog-overlay > [part~=overlay]")).isVisible();
      assertThat(fieldHelp.locator(".admin-help-description"))
          .hasText(uiTexts.getAdminMailRecipientHint());
      assertThat(browserPage.getByRole(AriaRole.TOOLTIP)).hasCount(0);
      AllurePlaywright.attachScreenshot(
          "Seitliche Mail-Feldhilfe bei " + width + "px", browserPage);
      browserPage.keyboard().press("Escape");

      assertThat(fieldHelp).hasCount(0);
    }
  }

  @Test
  @DisplayName(
      "COMPONENT-05: Bereinigtes Benutzermenü bleibt auf allen angemeldeten Seiten bedienbar")
  @Description(
      "Admin, Startseite und Konto auf Desktop und Smartphone öffnen. Identität, vier sichtbare Statusoptionen, Sprachwahl und rechtsbündiges Abmelden bleiben innerhalb des Viewports. Das Menü enthält keine zweite versteckte Statusauswahl oder alte Kopfzeile. Mein Konto navigiert, Escape und Außenklick schließen. Bei vorausgewähltem Deutsch und Englisch aktualisiert ein Sprachwechsel sowohl Feldwert als auch Optionen, ohne die Auswahl zu ändern. Keine Statusänderung und kein Speichern des Profilformulars.")
  void userMenuKeepsItsLayoutNavigationAndNativeClosing() {
    signIn();
    UserMenu userMenu = new UserMenu(browserPage);
    for (int width : new int[] {1280, 390}) {
      browserPage.setViewportSize(width, 900);
      for (String route : List.of(ADMIN_ROUTE, HOME_ROUTE, ACCOUNT_ROUTE)) {
        browserPage.navigate(path(route));
        userMenu.open();
        Locator body = browserPage.locator(".portal-user-menu-body:visible");
        Locator overlay = browserPage.locator("vaadin-popover-overlay[opened] > [part~='overlay']");

        assertThat(body.locator(".portal-menu-name")).isVisible();
        assertThat(body.locator(".portal-menu-email")).isVisible();
        assertThat(body.locator(".portal-menu-language-label"))
            .hasCSS("color", "rgb(226, 232, 240)");
        assertThat(body.locator(".portal-menu-language-label .portal-menu-section"))
            .hasCSS("color", "rgb(226, 232, 240)");
        assertThat(body.locator(".portal-status-option")).hasCount(4);
        assertThat(body.locator(".portal-status-option[aria-pressed=true]")).hasCount(1);
        assertThat(body.locator(".portal-language-switch vaadin-button")).hasCount(2);
        assertThat(body.locator("vaadin-select, .user-popover-header")).hasCount(0);
        Locator account =
            body.getByRole(
                AriaRole.BUTTON,
                new Locator.GetByRoleOptions()
                    .setName(uiTexts.getBaseManageAccountButton())
                    .setExact(true));

        assertThat(account).isVisible();
        Locator home =
            body.getByRole(
                AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName(uiTexts.getBaseHomeButton()).setExact(true));

        assertThat(home).hasCount(route.equals(HOME_ROUTE) ? 0 : 1);
        Locator admin =
            body.getByRole(
                AriaRole.BUTTON,
                new Locator.GetByRoleOptions()
                    .setName(uiTexts.getBaseManageApplicationButton())
                    .setExact(true));

        assertThat(admin).hasCount(route.equals(ADMIN_ROUTE) ? 0 : 1);
        BoundingBox bounds = overlay.boundingBox();

        assertTrue(bounds.x >= 0 && bounds.x + bounds.width <= width + 1);
        assertTrue(bounds.y >= 0 && bounds.y + bounds.height <= 901);
        BoundingBox footer = body.locator(".portal-menu-footer").boundingBox();
        Locator logoutButton = body.locator(".portal-menu-logout");
        BoundingBox logout = logoutButton.boundingBox();

        assertEquals(footer.x + footer.width - 8, logout.x + logout.width, 1);
        assertUserMenuFooterButtonSpacing(logoutButton);
        if (!route.equals(HOME_ROUTE)) {
          assertUserMenuFooterButtonSpacing(home);
        }
        AllurePlaywright.attachScreenshot(
            "Benutzermenü " + route + " bei " + width + "px", browserPage);
        userMenu.close();
        userMenu.open();
        browserPage.mouse().click(1, 880);

        assertThat(
                browserPage.locator(
                    "vaadin-popover-overlay[opened], vaadin-popover-overlay[closing]"))
            .hasCount(0);
        userMenu.open();
        account.click();

        assertThat(browserPage.locator("#account-panel-profile")).isVisible();
        assertThat(browserPage.locator("vaadin-popover-overlay[opened]")).hasCount(0);
      }
      verifyUserMenuFooterNavigation(userMenu);
      browserPage.navigate(path(ACCOUNT_ROUTE));
      assertPreferredLanguageLabels(userMenu);
    }
  }

  private void assertUserMenuFooterButtonSpacing(Locator footerButton) {
    assertThat(footerButton).hasCSS("padding-left", "12px");
    assertThat(footerButton).hasCSS("padding-right", "12px");
    BoundingBox initialBounds = footerButton.boundingBox();
    footerButton.hover();
    BoundingBox hoverBounds = footerButton.boundingBox();
    assertEquals(initialBounds.x, hoverBounds.x, 1);
    assertEquals(initialBounds.width, hoverBounds.width, 1);
  }

  private void verifyUserMenuFooterNavigation(UserMenu userMenu) {
    Locator footer = browserPage.locator(".portal-menu-footer:visible");
    Locator homeButton =
        footer.getByRole(
            AriaRole.BUTTON,
            new Locator.GetByRoleOptions().setName(uiTexts.getBaseHomeButton()).setExact(true));
    userMenu.open();
    homeButton.click();
    browserPage.waitForURL("**" + path(HOME_ROUTE));

    browserPage.navigate(path(ADMIN_ROUTE));
    userMenu.open();
    homeButton.focus();
    homeButton.press("Enter");
    browserPage.waitForURL("**" + path(HOME_ROUTE));

    userMenu.open();
    Locator logoutButton = footer.locator(".portal-menu-logout");
    logoutButton.focus();
    UiActions.complete(browserPage, () -> logoutButton.press("Enter"));
    browserPage.navigate(path(ACCOUNT_ROUTE));
    new LoginPage(browserPage).assertFormVisible();
    signIn();
  }

  private void assertPreferredLanguageLabels(UserMenu userMenu) {
    Locator field = browserPage.locator("#account-language");
    Locator input = field.locator("input");
    Locale originalInterface = Locale.forLanguageTag(uiTexts.language());
    String otherLanguage = uiTexts.language().equals("de") ? "en" : "de";
    for (Locale selected : List.of(Locale.GERMAN, Locale.ENGLISH)) {
      field.click();
      browserPage
          .getByRole(
              AriaRole.OPTION,
              new Page.GetByRoleOptions()
                  .setName(selected.getDisplayLanguage(originalInterface))
                  .setExact(true))
          .click();
      browserPage.locator("#account-first-name input").click();
      for (String language : List.of(otherLanguage, uiTexts.language())) {
        userMenu.selectLanguage(language);
        Locale currentInterface = Locale.forLanguageTag(language);
        String expected = selected.getDisplayLanguage(currentInterface);

        assertThat(input).hasValue(expected);
        field.click();
        for (Locale option : List.of(Locale.GERMAN, Locale.ENGLISH)) {
          assertThat(
                  browserPage.getByRole(
                      AriaRole.OPTION,
                      new Page.GetByRoleOptions()
                          .setName(option.getDisplayLanguage(currentInterface))
                          .setExact(true)))
              .isVisible();
        }
        input.press("Escape");

        assertThat(input).hasValue(expected);
      }
    }
    AllurePlaywright.attachScreenshot("Vorausgewählte Sprache nach Sprachwechsel", browserPage);
  }

  private void assertTooltip(Locator tooltip) {
    assertThat(tooltip).isVisible();
    assertThat(tooltip).hasCSS("background-color", "rgb(14, 23, 38)");
    assertThat(tooltip).hasCSS("border-top-color", "rgb(51, 65, 85)");
    assertThat(tooltip).hasCSS("border-top-width", "1px");
    assertThat(tooltip).hasCSS("border-radius", "4px");
    assertThat(tooltip).hasCSS("font-size", "11px");
    assertThat(tooltip).hasCSS("font-weight", "400");
    BoundingBox bounds = tooltip.boundingBox();

    assertTrue(bounds.width <= 221);
    assertTrue(bounds.x >= 0 && bounds.x + bounds.width <= browserPage.viewportSize().width + 1);
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName(
      "COMPONENT-12: Headeraktionen und Benachrichtigungsfilter zeigen eindeutige Mauszeiger")
  @Tag("component-follow-up")
  void headerActionsAndReadFiltersUsePointerWithoutProfileHover(int viewportWidth) {
    browserPage.setViewportSize(viewportWidth, 900);
    signIn();
    for (String route : new String[] {"/home", "/admin"}) {
      browserPage.navigate(route);
      Locator profileButtonLocator = browserPage.locator("vaadin-button.tracked-user-button");
      assertThat(profileButtonLocator).hasCSS("cursor", "pointer");
      profileButtonLocator.hover();
      assertThat(profileButtonLocator).hasCSS("background-color", "rgba(0, 0, 0, 0)");
      assertThat(
              profileButtonLocator.locator(
                  ":is(.cosmos-avatar-initials, vaadin-avatar [part=abbr])"))
          .hasCSS("cursor", "pointer");
      for (String buttonId :
          new String[] {"account-header-notifications", "account-header-settings"}) {
        Locator actionButtonLocator = browserPage.locator("#" + buttonId);
        assertThat(actionButtonLocator).hasCSS("cursor", "pointer");
        BoundingBox buttonBounds = actionButtonLocator.boundingBox();
        BoundingBox iconBounds = actionButtonLocator.locator("vaadin-icon").boundingBox();
        assertEquals(
            buttonBounds.x + buttonBounds.width / 2, iconBounds.x + iconBounds.width / 2, 0.1);
      }
      Locator homeLinkLocator = browserPage.locator(".cosmos-topbar-home");
      homeLinkLocator.hover();
      assertThat(homeLinkLocator).hasCSS("text-decoration-line", "none");
      assertThat(homeLinkLocator).hasCSS("cursor", "pointer");

      profileButtonLocator.focus();
      profileButtonLocator.press("Enter");
      assertThat(browserPage.locator("vaadin-popover-overlay[opened]")).isVisible();
      Locator presenceToggleLocator = browserPage.locator("#portal-menu-presence-toggle");
      if ("true".equals(presenceToggleLocator.getAttribute("aria-expanded"))) {
        presenceToggleLocator.click();
      }
      CosmosFollowUpChecks.assertDetailsWithoutHover(presenceToggleLocator);
      new UserMenu(browserPage).close();
      AllurePlaywright.attachScreenshot("Headeraktionen " + route, browserPage);
    }

    browserPage.navigate("/notifications");
    assertReadFilterPointers(browserPage.locator("#notifications-inbox-filter"));
    for (Locator pageButtonLocator :
        browserPage.locator(".admin-user-pagination vaadin-button").all()) {
      assertThat(pageButtonLocator)
          .hasCSS("cursor", pageButtonLocator.isEnabled() ? "pointer" : "not-allowed");
    }
    assertThat(browserPage.locator("#notifications-inbox-search input")).hasCSS("cursor", "text");
    browserPage.locator("#account-header-notifications").click();
    assertReadFilterPointers(browserPage.locator("#notifications-preview-filter"));
    AllurePlaywright.attachScreenshot("Benachrichtigungsfilter mit Handzeiger", browserPage);
  }

  private void assertReadFilterPointers(Locator filterGroupLocator) {
    assertThat(filterGroupLocator).isVisible();
    assertThat(filterGroupLocator.locator("vaadin-tab")).hasCount(3);
    for (Locator filterOptionLocator : filterGroupLocator.locator("vaadin-tab").all()) {
      assertThat(filterOptionLocator).hasCSS("cursor", "pointer");
      assertThat(filterOptionLocator.locator(".portal-read-filter-option"))
          .hasCSS("cursor", "pointer");
      assertThat(filterOptionLocator.locator(".portal-filter-count")).hasCSS("cursor", "pointer");
    }
  }

  private void signIn() {
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName("AURA-01: Native Felder teilen Deaktiviert-Zustand und kleine Fehlertexte")
  @Description(
      "Native Feldzustände auf Desktop und Smartphone prüfen. Leere Zahlenfelder behalten einen gedämpften Platzhalter auch ohne Löschaktion, bei Disabled und bei Read-only.")
  @Tag("aura-fields")
  void nativeFieldsShareDisabledColorsAndErrorPresentation(int viewportWidth) {
    browserPage.setViewportSize(viewportWidth, 1000);
    signIn();
    browserPage.evaluate(
        """
        async () => {
          const tags = ['vaadin-text-field', 'vaadin-email-field', 'vaadin-password-field',
            'vaadin-integer-field', 'vaadin-big-decimal-field', 'vaadin-text-area',
            'vaadin-combo-box', 'vaadin-date-picker', 'vaadin-time-picker'];
          await Promise.all(tags.map(tag => customElements.whenDefined(tag)));
          const fixture = document.createElement('vaadin-vertical-layout');
          fixture.id = 'aura-field-fixture';
          fixture.className = 'cosmos-theme cosmos-reference-portal';
          fixture.style.cssText = 'position:fixed;inset:80px 16px 16px;z-index:1000;overflow:auto;background:#091120;padding:16px;gap:8px';
          for (const tag of tags) {
            const field = document.createElement(tag);
            field.label = tag;
            field.style.width = '100%';
            field.placeholder = 'Sample';
            field.disabled = true;
            field.setAttribute('data-aura-disabled', '');
            if (tag === 'vaadin-combo-box') field.items = ['Sample'];
            field.value = tag === 'vaadin-date-picker' ? '2026-01-01'
              : tag === 'vaadin-time-picker' ? '07:30'
              : tag.includes('integer') || tag.includes('decimal') ? '12' : 'Sample';
            fixture.append(field);
          }
          for (const state of ['disabled', 'readonly', 'without-clear']) {
            const field = document.createElement('vaadin-integer-field');
            field.label = state;
            field.placeholder = '12';
            field.clearButtonVisible = state !== 'without-clear';
            field.disabled = state === 'disabled';
            field.readonly = state === 'readonly';
            field.setAttribute('data-numeric-state', state);
            fixture.append(field);
          }
          const invalid = document.createElement('vaadin-text-field');
          invalid.label = 'Invalid field';
          invalid.errorMessage = 'Synthetic validation message';
          invalid.invalid = true;
          invalid.id = 'aura-invalid-field';
          fixture.append(invalid);
          const secondary = document.createElement('vaadin-button');
          secondary.id = 'aura-secondary-button';
          secondary.textContent = 'Secondary action';
          const secondaryIcon = document.createElement('vaadin-icon');
          secondaryIcon.setAttribute('src', '/icons/lucide/chevron-left.svg');
          secondaryIcon.classList.add('cosmos-lucide-icon');
          secondaryIcon.slot = 'prefix';
          secondary.append(secondaryIcon);
          fixture.append(secondary);
          for (const tag of ['vaadin-checkbox', 'vaadin-switch', 'vaadin-button']) {
            const control = document.createElement(tag);
            control.disabled = true;
            control.textContent = 'Disabled action';
            control.setAttribute('data-aura-action', '');
            fixture.append(control);
          }
          document.body.append(fixture);
        }
        """);
    try {
      Locator fixtureLocator = browserPage.locator("#aura-field-fixture");
      assertThat(fixtureLocator.locator("[data-aura-disabled]")).hasCount(9);
      String disabledColor = resolveFixtureColor(fixtureLocator, "--cosmos-text-disabled-color");
      for (Locator fieldLocator : fixtureLocator.locator("[data-aura-disabled]").all()) {
        assertThat(fieldLocator).hasAttribute("disabled", "");
        assertThat(fieldLocator.locator("[part=label]")).hasCSS("opacity", "0.5");
        Locator inputLocator = fieldLocator.locator("input[slot=input], textarea[slot=textarea]");
        assertThat(inputLocator).hasCSS("color", disabledColor);
        assertEquals(
            disabledColor,
            inputLocator.evaluate("element => getComputedStyle(element, '::placeholder').color"));
      }
      assertEmptyNumericFieldStates(fixtureLocator, disabledColor);
      Locator invalidField = fixtureLocator.locator("#aura-invalid-field");
      assertThat(invalidField.locator("[part=input-field]"))
          .hasCSS("border-top-color", "rgb(248, 113, 113)");
      assertThat(invalidField.locator("[part=required-indicator]")).not().isVisible();
      Locator errorLocator = invalidField.locator("[part=error-message]");
      assertThat(errorLocator).hasCSS("font-size", "13px");
      assertEquals(
          "none",
          errorLocator.evaluate("element => getComputedStyle(element, '::before').display"));
      Locator secondaryButtonLocator = fixtureLocator.locator("#aura-secondary-button");
      assertThat(secondaryButtonLocator).hasCSS("color", "rgb(255, 255, 255)");
      assertThat(secondaryButtonLocator.locator("vaadin-icon path")).hasCount(1);
      assertThat(secondaryButtonLocator.locator("vaadin-icon"))
          .hasCSS("color", resolveFixtureColor(fixtureLocator, "--cosmos-primary-text-color"));
      for (Locator actionLocator : fixtureLocator.locator("[data-aura-action]").all()) {
        assertThat(actionLocator).hasAttribute("disabled", "");
        if (actionLocator.evaluate("element => element.localName").equals("vaadin-button")) {
          assertThat(actionLocator).hasCSS("opacity", "0.45");
        } else {
          // Playwright erkennt den Zustand am nativen Input, nicht an jeder Vaadin-Hülle.
          assertThat(actionLocator.locator("input")).isDisabled();
          String markerPart =
              actionLocator.evaluate("element => element.localName").equals("vaadin-checkbox")
                  ? "checkbox"
                  : "marker";
          Locator markerLocator = actionLocator.locator("[part=" + markerPart + "]");
          String disabledMarkerColor =
              (String)
                  markerLocator.evaluate("element => getComputedStyle(element).backgroundColor");
          actionLocator.evaluate("element => element.disabled = false");
          assertThat(markerLocator).not().hasCSS("background-color", disabledMarkerColor);
          actionLocator.evaluate("element => element.disabled = true");
          assertThat(markerLocator).hasCSS("background-color", disabledMarkerColor);
          assertThat(actionLocator.locator("input")).isDisabled();
        }
      }
      AllurePlaywright.attachScreenshot("Aura-Felder " + viewportWidth, browserPage);
    } finally {
      browserPage.evaluate("document.querySelector('#aura-field-fixture')?.remove()");
    }
  }

  private void assertEmptyNumericFieldStates(Locator fixtureLocator, String disabledColor) {
    String placeholderColor =
        resolveFixtureColor(fixtureLocator, "--cosmos-input-placeholder-color");
    for (String state : List.of("disabled", "readonly", "without-clear")) {
      Locator numberField = fixtureLocator.locator("[data-numeric-state='" + state + "']");
      Locator numberInput = numberField.locator("input:placeholder-shown");
      assertThat(numberInput).hasValue("");
      assertThat(numberField.locator("[part~=clear-button]")).not().isVisible();
      assertEquals(
          state.equals("disabled") ? disabledColor : placeholderColor,
          numberInput.evaluate("input => getComputedStyle(input, '::placeholder').color"));
    }
    assertThat(fixtureLocator.locator("[data-numeric-state='disabled'] input")).isDisabled();
    assertThat(fixtureLocator.locator("[data-numeric-state='readonly'] input"))
        .hasAttribute("readonly", "");
    assertThat(fixtureLocator.locator("[data-numeric-state='without-clear']"))
        .not()
        .hasAttribute("clear-button-visible", "");
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @Tag("aura-fields")
  @DisplayName("AURA-03: Native Checkboxen zeigen weiße Haken und Teilauswahlstriche")
  @Description(
      """
      1. Einzelcheckbox per Tastatur wählen, Fokus und Hover auf der nativen Fläche prüfen.
      2. Gruppenoption per Label wählen. Gesamtauswahl des nativen Grid-Headers prüfen.
      3. Voll- und Teilauswahl sowie die Darstellung bei Disabled und Read-only vergleichen.
      **Erwartet:** Haken und Strich sind weiß. Gesperrte Zustände bleiben erkennbar.
      """)
  void nativeCheckboxesKeepWhiteSelectionMarks(int viewportWidth) {
    browserPage.setViewportSize(viewportWidth, 900);
    signIn();
    addCheckboxAppearanceFixture();
    Locator fixtureLocator = browserPage.locator("#aura-checkbox-fixture");
    try {
      Locator singleCheckbox = fixtureLocator.locator("#aura-single-checkbox");
      Locator singleInput = singleCheckbox.getByRole(AriaRole.CHECKBOX);
      fixtureLocator.locator("button").focus();
      browserPage.keyboard().press("Tab");
      assertThat(singleInput).isFocused();
      assertThat(checkboxMark(singleCheckbox)).hasCSS("outline-style", "solid");
      singleInput.press("Space");
      assertThat(singleInput).isChecked();
      assertWhiteCheckboxMark(singleCheckbox);
      singleCheckbox.hover();
      browserPage.waitForCondition(() ->
          "0.04"
              .equals(
                  checkboxMark(singleCheckbox)
                      .evaluate("mark => getComputedStyle(mark, '::before').opacity")));

      Locator groupCheckbox = fixtureLocator.locator("vaadin-checkbox-group vaadin-checkbox");
      groupCheckbox.locator("label[slot=label]").click();
      assertThat(groupCheckbox.getByRole(AriaRole.CHECKBOX)).isChecked();
      assertWhiteCheckboxMark(groupCheckbox);

      Locator selectAllCheckbox =
          fixtureLocator.locator("vaadin-grid .vaadin-grid-select-all-checkbox");
      assertThat(selectAllCheckbox).hasAttribute("checked", "");
      assertWhiteCheckboxMark(selectAllCheckbox);
      String fullMark = checkboxMarkMask(selectAllCheckbox);
      // In dieser reinen Darstellungsfixture kommt der Zwischenzustand ohne Serverauswahl aus.
      selectAllCheckbox.evaluate(
          "checkbox => { checkbox.checked = false; checkbox.indeterminate = true; }");
      assertThat(selectAllCheckbox).hasAttribute("indeterminate", "");
      assertThat(selectAllCheckbox.getByRole(AriaRole.CHECKBOX))
          .hasJSProperty("indeterminate", true);
      assertWhiteCheckboxMark(selectAllCheckbox);
      assertNotEquals(fullMark, checkboxMarkMask(selectAllCheckbox));

      Locator disabledCheckbox = fixtureLocator.locator("#aura-disabled-checkbox");
      assertThat(disabledCheckbox.getByRole(AriaRole.CHECKBOX)).isDisabled();
      assertThat(checkboxMark(disabledCheckbox)).not().hasCSS("color", "rgb(255, 255, 255)");
      Locator readonlyCheckbox = fixtureLocator.locator("#aura-readonly-checkbox");
      readonlyCheckbox.locator("label[slot=label]").click();
      assertThat(readonlyCheckbox.getByRole(AriaRole.CHECKBOX)).isChecked();
      assertThat(checkboxMark(readonlyCheckbox)).hasCSS("border-top-style", "dashed");
      AllurePlaywright.attachScreenshot("Native Checkboxen " + viewportWidth, browserPage);
    } finally {
      fixtureLocator.evaluate("fixture => fixture.remove()");
    }
  }

  private void addCheckboxAppearanceFixture() {
    browserPage.evaluate(
        """
        () => {
          const fixture = document.createElement('section');
          fixture.id = 'aura-checkbox-fixture';
          fixture.className = 'cosmos-theme cosmos-reference-portal';
          fixture.style.cssText = 'position:fixed;inset:80px 16px 16px;z-index:1000;overflow:auto;background:#091120;padding:16px;display:grid;gap:12px;align-content:start';
          const anchor = document.createElement('button');
          anchor.textContent = 'Start';
          fixture.append(anchor);
          for (const state of ['single', 'disabled', 'readonly']) {
            const checkbox = document.createElement('vaadin-checkbox');
            checkbox.id = `aura-${state}-checkbox`;
            checkbox.label = state;
            checkbox.checked = state !== 'single';
            checkbox.disabled = state === 'disabled';
            checkbox.readonly = state === 'readonly';
            fixture.append(checkbox);
          }
          const group = document.createElement('vaadin-checkbox-group');
          const choice = document.createElement('vaadin-checkbox');
          choice.label = 'Group option';
          choice.value = 'option';
          group.append(choice);
          fixture.append(group);

          const grid = document.createElement('vaadin-grid');
          grid.style.height = '160px';
          grid.items = [{name: 'First'}, {name: 'Second'}];
          const selection = document.createElement('vaadin-grid-flow-selection-column');
          selection.selectAll = true;
          const column = document.createElement('vaadin-grid-column');
          column.path = 'name';
          grid.append(selection, column);
          fixture.append(grid);
          document.body.append(fixture);
        }
        """);
  }

  private Locator checkboxMark(Locator checkboxLocator) {
    return checkboxLocator.locator("[part=checkbox]");
  }

  private void assertWhiteCheckboxMark(Locator checkboxLocator) {
    Locator markLocator = checkboxMark(checkboxLocator);
    assertThat(markLocator).isVisible();
    assertEquals(
        "rgb(255, 255, 255)",
        markLocator.evaluate("mark => getComputedStyle(mark, '::after').backgroundColor"));
  }

  private String checkboxMarkMask(Locator checkboxLocator) {
    return (String)
        checkboxMark(checkboxLocator)
            .evaluate("mark => getComputedStyle(mark, '::after').maskImage");
  }

  private String resolveFixtureColor(Locator fixtureLocator, String colorToken) {
    return (String)
        fixtureLocator.evaluate(
            """
            (element, token) => {
              const probe = document.createElement('span');
              probe.style.color = `var(${token})`;
              element.append(probe);
              const color = getComputedStyle(probe).color;
              probe.remove();
              return color;
            }
            """,
            colorToken);
  }
}
