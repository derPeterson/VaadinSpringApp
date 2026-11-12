package de.derpeterson.browsertests.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.microsoft.playwright.Dialog;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.i18n.UiTexts;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

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

  public void assertBrowserReloadCanBeCancelled() {
    browserPage.waitForCondition(() ->
        Boolean.TRUE.equals(
            browserPage.evaluate("() => (window.cosmosUnsavedForms?.ids.size ?? 0) > 0")));
    AtomicBoolean warningObserved = new AtomicBoolean();
    Consumer<Dialog> dialogHandler =
        dialog -> {
          assertEquals("beforeunload", dialog.type());
          warningObserved.set(true);
          dialog.dismiss();
        };
    browserPage.onDialog(dialogHandler);
    try {
      browserPage.evaluate("() => window.location.reload()");
      browserPage.waitForCondition(warningObserved::get);
    } finally {
      browserPage.offDialog(dialogHandler);
    }
  }

  public void assertBrowserReloadHasNoWarning() {
    browserPage.waitForCondition(() ->
        Boolean.TRUE.equals(
            browserPage.evaluate("() => (window.cosmosUnsavedForms?.ids.size ?? 0) === 0")));
    AtomicBoolean warningObserved = new AtomicBoolean();
    Consumer<Dialog> dialogHandler =
        dialog -> {
          warningObserved.set(true);
          dialog.accept();
        };
    browserPage.onDialog(dialogHandler);
    try {
      browserPage.reload();
      assertFalse(warningObserved.get(), "Unchanged form must not warn before reload");
    } finally {
      browserPage.offDialog(dialogHandler);
    }
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
