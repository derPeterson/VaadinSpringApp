package de.derpeterson.browsertests.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.i18n.UiTexts;

public final class UnsavedChangesPrompt {
  private final Locator confirmationDialogLocator;
  private final UiTexts uiTexts;

  public UnsavedChangesPrompt(Page browserPage, UiTexts uiTexts) {
    confirmationDialogLocator = browserPage.locator("#unsaved-changes-confirmation");
    this.uiTexts = uiTexts;
  }

  public Locator root() {
    return confirmationDialogLocator;
  }

  public void keepEditing() {
    choose(uiTexts.getUnsavedChangesKeepEditing());
  }

  public void discard() {
    choose(uiTexts.getUnsavedChangesDiscard());
  }

  public void discardIfShown() {
    if (confirmationDialogLocator.count() > 0) {
      discard();
    }
  }

  private void choose(String actionButtonLabel) {
    confirmationDialogLocator
        .getByRole(
            AriaRole.BUTTON,
            new Locator.GetByRoleOptions().setName(actionButtonLabel).setExact(true))
        .click();
    assertThat(confirmationDialogLocator).hasCount(0);
  }
}
