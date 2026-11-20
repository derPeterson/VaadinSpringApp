package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.BoundingBox;
import de.derpeterson.app.config.AdminConfigGroup;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.i18n.UiTexts;
import de.derpeterson.browsertests.mail.MailpitInbox;
import de.derpeterson.browsertests.mail.MailpitInbox.CapturedMail;
import de.derpeterson.browsertests.pages.AdminConfigurationPage;
import de.derpeterson.browsertests.pages.AdminMailPage;
import de.derpeterson.browsertests.pages.CosmosFollowUpChecks;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.UiActions;
import de.derpeterson.browsertests.pages.UserMenu;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.playwright.AllurePlaywright;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("browser")
@Feature("Admin-Testmail")
public class AdminMailBrowserTest extends BrowserFixture {
  @Tag("fast")
  @Test
  @DisplayName("MAIL-01: Eingabefehler korrigieren und echte Unicode-Testmail in Mailpit empfangen")
  @Description(
      """
      1. Als Admin Mailtest öffnen; ungültigen Empfänger und anschließend leeren Betreff/Text einzeln absenden.
      2. Lokalisierte Hinweise und markierte Felder prüfen; über deren echte Eingabe korrigieren.
      3. Eigene Testadresse, Unicode-Betreff und Text setzen; bewusst per Enter im Betreff versenden und Erfolgshinweis prüfen.
      4. Tatsächlich empfangene SMTP-Mail nach eigenem Empfänger und Betreff prüfen; Inhalt exakt vergleichen.
      **Erwartet:** Unicode bleibt erhalten, genau eine eigene Mail; regulärer SMTP-Zeilenabschluss berücksichtigt.
      """)
  void validationThenActualUnicodeMail() {
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    AdminConfigurationPage adminConfigurationPage = new AdminConfigurationPage(browserPage);
    adminConfigurationPage.open(AdminConfigGroup.EMAIL_SENDER);
    String sender = adminConfigurationPage.value(uiTexts.getAdminConfigEntryEmailFrom());
    AdminMailPage mail = new AdminMailPage(browserPage);
    mail.open();
    String email = "mail-" + UUID.randomUUID() + "@example.test";
    Locator mailForm = browserPage.locator(".cosmos-mail-card");
    Locator recipientInput = mail.input(uiTexts.getAdminMailRecipient());
    Locator subjectInput = mail.input(uiTexts.getAdminMailSubject());
    Locator bodyInput = mail.input(uiTexts.getAdminMailBody());
    for (Locator input : List.of(recipientInput, subjectInput, bodyInput)) {
      input.clear();
      input.press("Tab");
    }
    assertThat(mailForm.locator("[invalid]")).hasCount(0);
    mail.send();
    mail.notice(uiTexts.getAdminMailValidationRecipient());
    assertThat(mailForm.locator("vaadin-email-field[invalid]")).hasCount(1);
    assertThat(mailForm.locator("vaadin-text-field[invalid]")).hasCount(1);
    assertThat(mailForm.locator("vaadin-text-area[invalid]")).hasCount(1);
    assertThat(mailForm.locator("[part=error-message]:visible")).hasCount(0);

    mail.input(uiTexts.getAdminMailRecipient()).fill("invalid");
    mail.send();
    mail.notice(uiTexts.getAdminMailValidationRecipient());

    assertThat(browserPage.locator("vaadin-email-field[invalid]")).hasCount(1);
    mail.input(uiTexts.getAdminMailRecipient()).fill(email);
    assertThat(mailForm.locator("vaadin-email-field[invalid]")).hasCount(0);
    assertThat(mailForm.locator("vaadin-text-field[invalid]")).hasCount(1);
    assertThat(mailForm.locator("vaadin-text-area[invalid]")).hasCount(1);
    mail.input(uiTexts.getAdminMailSubject()).fill("");
    mail.send();
    mail.notice(uiTexts.getAdminMailValidationSubject());

    assertThat(browserPage.locator("vaadin-text-field[invalid]")).hasCount(1);
    String subject = "Browser SMTP ÄÖÜ ß 中文 " + UUID.randomUUID(),
        body = "Unicode ÄÖÜ ß 中文 – geprüft";
    mail.input(uiTexts.getAdminMailSubject()).fill(subject);
    mail.input(uiTexts.getAdminMailBody()).fill("");
    mail.send();
    mail.notice(uiTexts.getAdminMailValidationBody());

    assertThat(browserPage.locator("vaadin-text-area[invalid]")).hasCount(1);
    mail.input(uiTexts.getAdminMailBody()).fill(body);
    mail.input(uiTexts.getAdminMailSubject()).focus();
    mail.sendFromSubject();
    mail.notice(uiTexts.getAdminMailSuccess());

    assertThat(
            browserPage.locator(
                "vaadin-email-field[invalid],vaadin-text-field[invalid],vaadin-text-area[invalid]"))
        .hasCount(0);
    MailpitInbox inbox = new MailpitInbox(browserContext);
    CapturedMail captured = inbox.awaitAdminMail(email, subject);
    inbox.assertPlainContent(captured, body);
    inbox.assertSender(captured, sender);
  }

  @Test
  @DisplayName("MAIL-02: Seitliche Feldhilfen und Sprachwechsel erhalten die ungesendete Nachricht")
  @Description(
      """
      1. Mail Test öffnen und einen eigenen Empfänger, Betreff und Text eingeben, ohne zu senden.
      2. Auf Desktop und Smartphone alle drei Feldhilfen per Tastatur öffnen und per Schließen-Button schließen.
      3. Sprache wechseln und die Feldhilfe erneut öffnen. Die Übersetzung und alle Eingaben prüfen.
      4. Sprache wiederherstellen und die weiterhin ungesendete Eingabe prüfen.
      **Erwartet:** Feldhilfen und Sprachwechsel verändern keine Nachrichteneingaben und lösen keinen Versand aus.
      """)
  void fieldInfosAndLanguagePreserveUnsentMessage() {
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    AdminMailPage mail = new AdminMailPage(browserPage);
    mail.open();
    for (int width : new int[] {1280, 390}) {
      browserPage.setViewportSize(width, 900);
      assertMailFieldHelpButtonsAreCentered();
      UiActions.resizeTextArea(
          browserPage, browserPage.locator("vaadin-text-area.cosmos-mail-body-field"), 144);
      assertMailFieldHelpButtonsAreCentered();
    }
    browserPage.setViewportSize(1280, 900);
    String email = "info-" + UUID.randomUUID() + "@example.test",
        subject = "Unsent subject",
        body = "Unsent message ÄÖÜ";
    mail.input(uiTexts.getAdminMailRecipient()).fill(email);
    mail.input(uiTexts.getAdminMailSubject()).fill(subject);
    mail.input(uiTexts.getAdminMailBody()).fill(body);
    mail.input(uiTexts.getAdminMailBody()).press("Tab");
    String[][] infos = {
      {
        uiTexts.getAdminMailRecipient(),
        uiTexts.getAdminMailRecipientHint(),
        uiTexts.getAdminMailValidationRecipient()
      },
      {
        uiTexts.getAdminMailSubject(),
        uiTexts.getAdminMailSubjectHint(),
        uiTexts.getAdminMailValidationSubject()
      },
      {
        uiTexts.getAdminMailBody(),
        uiTexts.getAdminMailBodyHint(),
        uiTexts.getAdminMailValidationBody()
      }
    };
    for (int width : new int[] {1280, 390}) {
      browserPage.setViewportSize(width, 900);
      for (String[] info : infos) {
        Locator help = openFieldHelp(info[0]);
        assertHelpContentAndClose(help, info[1], info[2]);
      }
      for (String[] info : List.of(infos[1], infos[2])) {
        fieldHelpButton(info[0]).click();
        assertHelpContentAndClose(assertOpenFieldHelp(info[0]), info[1], info[2]);
      }
    }
    UiTexts other =
        new de.derpeterson.browsertests.i18n.UiTexts(uiTexts.language().equals("de") ? "en" : "de");
    try {
      new UserMenu(browserPage).selectLanguage(other.language());
      Locator localizedBody = mail.input(other.getAdminMailBody());

      assertThat(mail.input(other.getAdminMailRecipient())).hasValue(email);
      assertThat(mail.input(other.getAdminMailSubject())).hasValue(subject);
      assertThat(localizedBody).hasValue(body);
      // Der Sprachwechsel gibt den Fokus an den Menübutton mit eigenem Tooltip zurück.
      localizedBody.focus();
      Locator help = openFieldHelp(other.getAdminMailBody());

      assertThat(help.locator(".admin-help-description")).hasText(other.getAdminMailBodyHint());
      assertThat(help.locator(".admin-help-rules li")).hasText(other.getAdminMailValidationBody());
      AllurePlaywright.attachScreenshot(
          "Mail-Feldhilfe und erhaltene Nachricht nach Sprachwechsel", browserPage);
      browserPage.keyboard().press("Escape");

      assertThat(help).hasCount(0);
      mail.input(other.getAdminMailSubject()).clear();
      localizedBody.clear();
      localizedBody.press("Tab");
    } finally {
      new UserMenu(browserPage).selectTestLanguage();
    }
    assertThat(mail.input(uiTexts.getAdminMailRecipient())).hasValue(email);
    assertThat(mail.input(uiTexts.getAdminMailSubject())).hasValue("");
    assertThat(mail.input(uiTexts.getAdminMailBody())).hasValue("");
    assertThat(browserPage.locator("vaadin-notification-card:not([closing])")).hasCount(0);
  }

  private Locator fieldHelpButton(String fieldLabel) {
    return browserPage.getByRole(
        AriaRole.BUTTON, new Page.GetByRoleOptions().setName(fieldLabel).setExact(true));
  }

  private Locator openFieldHelp(String fieldLabel) {
    Locator button = fieldHelpButton(fieldLabel);
    button.hover();
    assertThat(button.locator("vaadin-tooltip")).hasCount(0);
    assertThat(browserPage.locator("vaadin-tooltip-overlay[opened]")).hasCount(0);
    button.focus();
    assertThat(browserPage.locator("vaadin-tooltip-overlay[opened]")).hasCount(0);
    UiActions.complete(browserPage, () -> browserPage.keyboard().press("Enter"));
    return assertOpenFieldHelp(fieldLabel);
  }

  private Locator assertOpenFieldHelp(String fieldLabel) {
    Locator help = browserPage.locator("#admin-mail-field-help[opened]");

    assertThat(help.locator("vaadin-dialog-overlay > [part~=overlay]")).isVisible();
    assertThat(help.locator(".admin-help-field-title")).hasText(fieldLabel);
    return help;
  }

  private void assertHelpContentAndClose(Locator help, String hint, String rule) {
    assertThat(help.locator(".admin-help-description")).hasText(hint);
    assertThat(help.locator(".admin-help-rules h4")).hasText(uiTexts.getAdminConfigInputRules());
    assertThat(help.locator(".admin-help-rules li")).hasText(rule);
    Locator closeButton = help.locator(".cosmos-overlay-close");
    CosmosFollowUpChecks.assertCloseCircle(closeButton, uiTexts.getNotificationsClose());
    closeButton.hover();
    assertThat(browserPage.getByRole(AriaRole.TOOLTIP)).hasText(uiTexts.getNotificationsClose());
    closeButton.focus();
    assertThat(closeButton).isFocused();
    closeButton.press("Enter");
    assertThat(help).hasCount(0);
  }

  private void assertMailFieldHelpButtonsAreCentered() {
    for (Locator mailFieldRowLocator : browserPage.locator(".cosmos-mail-field-row").all()) {
      BoundingBox inputFieldBoundingBox =
          mailFieldRowLocator.locator("[part~='input-field']").first().boundingBox();
      BoundingBox fieldHelpButtonBoundingBox =
          mailFieldRowLocator.locator(".cosmos-config-info").boundingBox();

      assertEquals(
          inputFieldBoundingBox.y + inputFieldBoundingBox.height / 2,
          fieldHelpButtonBoundingBox.y + fieldHelpButtonBoundingBox.height / 2,
          1,
          "Mail-Infoicon muss zur Eingabefläche statt zum Feldlabel zentriert sein");
    }
  }

  @Test
  @DisplayName("MAIL-03: Enter im Nachrichtentext fügt eine neue Zeile ein und versendet nicht")
  @Description(
      """
      1. Mail Test öffnen und eine eigene Empfängeradresse samt Betreff eingeben.
      2. Im Nachrichtentext eine erste Zeile, Enter und eine zweite Zeile tippen; Fokus anschließend verlassen.
      3. Beide Zeilen unverändert prüfen; kein Versand- oder Fehlerhinweis darf erscheinen.
      4. Bewusst über den Sendebutton versenden und beide Zeilen in der eigenen echten SMTP-Mail prüfen.
      **Erwartet:** Mehrzeilige Texte sind ohne unbeabsichtigten Versand möglich; erst der Button sendet.
      """)
  void bodyEnterInsertsNewlineWithoutSending() {
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    AdminMailPage mail = new AdminMailPage(browserPage);
    mail.open();
    String email = "multiline-" + UUID.randomUUID() + "@example.test",
        subject = "Multiline " + UUID.randomUUID();
    mail.input(uiTexts.getAdminMailRecipient()).fill(email);
    mail.input(uiTexts.getAdminMailSubject()).fill(subject);
    Locator body = mail.input(uiTexts.getAdminMailBody());
    UiActions.resizeTextArea(
        browserPage, browserPage.locator("vaadin-text-area.cosmos-mail-body-field"), 144);
    UiActions.completeValueChange(
        browserPage,
        () -> {
          body.fill("First line");
          body.press("End");
          body.press("Enter");
          body.pressSequentially("Second line ÄÖÜ");
          body.press("Tab");
        });

    assertThat(body).hasValue("First line\nSecond line ÄÖÜ");
    assertThat(browserPage.locator("vaadin-notification-card:not([closing])")).hasCount(0);
    mail.send();
    mail.notice(uiTexts.getAdminMailSuccess());
    MailpitInbox inbox = new MailpitInbox(browserContext);
    inbox.assertPlainContent(inbox.awaitAdminMail(email, subject), "First line\nSecond line ÄÖÜ");
  }
}
