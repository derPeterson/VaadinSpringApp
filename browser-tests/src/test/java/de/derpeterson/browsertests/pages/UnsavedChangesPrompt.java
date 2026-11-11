package de.derpeterson.browsertests.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.i18n.UiTexts;
import java.util.function.BooleanSupplier;

public final class UnsavedChangesPrompt {
  private final Page browserPage;
  private final Locator confirmationDialogLocator;
  private final UiTexts uiTexts;

  public UnsavedChangesPrompt(Page browserPage, UiTexts uiTexts) {
    this.browserPage = browserPage;
    confirmationDialogLocator = browserPage.locator("#unsaved-changes-confirmation");
    this.uiTexts = uiTexts;
  }

  public Locator root() {
    return confirmationDialogLocator;
  }

  public void keepEditing() {
    clickConfirmationAction(uiTexts.getUnsavedChangesKeepEditing());
  }

  public void discard() {
    clickConfirmationAction(uiTexts.getUnsavedChangesDiscard());
  }

  /**
   * Wartet auf den sichtbaren Abschluss oder den Schutzdialog, auch nach einer vorherigen
   * Wertänderung.
   */
  public void discardAfterActionIfNeeded(BooleanSupplier actionCompletionSupplier) {
    browserPage.waitForCondition(
        () -> confirmationDialogLocator.count() > 0 || actionCompletionSupplier.getAsBoolean());
    if (confirmationDialogLocator.count() > 0) {
      discard();
    }
  }

  private void clickConfirmationAction(String actionButtonLabel) {
    confirmationDialogLocator
        .getByRole(
            AriaRole.BUTTON,
            new Locator.GetByRoleOptions().setName(actionButtonLabel).setExact(true))
        .click();
    assertThat(confirmationDialogLocator).hasCount(0);
  }
}
