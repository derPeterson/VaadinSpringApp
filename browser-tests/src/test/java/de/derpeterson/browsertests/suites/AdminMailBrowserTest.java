package de.derpeterson.browsertests.suites;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.app.config.AdminConfigGroup;
import de.derpeterson.browsertests.*;
import de.derpeterson.browsertests.pages.*;
import de.derpeterson.browsertests.mail.MailpitInbox;
import io.qameta.allure.*;
import io.qameta.allure.playwright.AllurePlaywright;
import org.junit.jupiter.api.*;
import java.util.UUID;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static de.derpeterson.browsertests.BrowserRoutes.*;
import static de.derpeterson.app.config.AppRouteConstants.*;

@Tag("browser") @Feature("Admin-Testmail")
public class AdminMailBrowserTest extends BrowserFixture {
    @Test @DisplayName("MAIL-01: Eingabefehler korrigieren und echte Unicode-Testmail in Mailpit empfangen")
    @Description("""
        1. Als Admin Mailtest öffnen; ungültigen Empfänger und anschließend leeren Betreff/Text einzeln absenden.
        2. Lokalisierte Hinweise und markierte Felder prüfen; über deren echte Eingabe korrigieren.
        3. Eigene Testadresse, Unicode-Betreff und Text setzen; bewusst per Enter im Betreff versenden und Erfolgshinweis prüfen.
        4. Tatsächlich empfangene SMTP-Mail nach eigenem Empfänger und Betreff prüfen; Inhalt exakt vergleichen.
        **Erwartet:** Unicode bleibt erhalten, genau eine eigene Mail; regulärer SMTP-Zeilenabschluss berücksichtigt.
        """)
    void validationThenActualUnicodeMail() {
        LoginPage login = new LoginPage(page); login.open(); login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
        new UserMenu(page).selectTestLanguage();
        ConfigurationPage config = new ConfigurationPage(page); config.open(AdminConfigGroup.EMAIL_SENDER);
        String sender = config.value(texts.getAdminConfigEntryEmailFrom());
        AdminMailPage mail = new AdminMailPage(page); mail.open();
        String email = "mail-" + UUID.randomUUID() + "@example.test";
        mail.input(texts.getAdminMailRecipient()).fill("invalid"); mail.send(); mail.notice(texts.getAdminMailValidationRecipient());
        assertThat(page.locator("vaadin-email-field[invalid]")).hasCount(1);
        mail.input(texts.getAdminMailRecipient()).fill(email); mail.input(texts.getAdminMailSubject()).fill("");
        mail.send(); mail.notice(texts.getAdminMailValidationSubject()); assertThat(page.locator("vaadin-text-field[invalid]")).hasCount(1);
        String subject = "Browser SMTP ÄÖÜ ß 中文 " + UUID.randomUUID(), body = "Unicode ÄÖÜ ß 中文 – geprüft";
        mail.input(texts.getAdminMailSubject()).fill(subject); mail.input(texts.getAdminMailBody()).fill("");
        mail.send(); mail.notice(texts.getAdminMailValidationBody()); assertThat(page.locator("vaadin-text-area[invalid]")).hasCount(1);
        mail.input(texts.getAdminMailBody()).fill(body); mail.input(texts.getAdminMailSubject()).focus();
        mail.sendFromSubject(); mail.notice(texts.getAdminMailSuccess());
        assertThat(page.locator("vaadin-email-field[invalid],vaadin-text-field[invalid],vaadin-text-area[invalid]")).hasCount(0);
        MailpitInbox inbox = new MailpitInbox(context); var captured = inbox.awaitAdminMail(email, subject);
        inbox.assertPlainContent(captured, body); inbox.assertSender(captured, sender);
    }
    @Test @DisplayName("MAIL-02: Drei manuelle Feldinfos und Sprachwechsel erhalten die ungesendete Nachricht")
    @Description("""
        1. Mail Test öffnen und einen eigenen Empfänger, Betreff und Text eingeben, ohne zu senden.
        2. Alle drei Info-Buttons öffnen: passende Beschreibung und Eingaberegel; per Außenklick schließen.
        3. Sprache wechseln: Beschriftungen und Tooltip übersetzen sich, alle Eingaben bleiben erhalten.
        4. Sprache wiederherstellen und die weiterhin ungesendete Eingabe prüfen.
        **Erwartet:** Tooltip-Bedienung und Sprachwechsel verändern keine Nachrichteneingaben und lösen keinen Versand aus.
        """)
    void fieldInfosAndLanguagePreserveUnsentMessage() {
        LoginPage login = new LoginPage(page); login.open(); login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
        new UserMenu(page).selectTestLanguage();
        AdminMailPage mail = new AdminMailPage(page); mail.open();
        String email = "info-" + UUID.randomUUID() + "@example.test", subject = "Unsent subject", body = "Unsent message ÄÖÜ";
        mail.input(texts.getAdminMailRecipient()).fill(email);
        mail.input(texts.getAdminMailSubject()).fill(subject);
        mail.input(texts.getAdminMailBody()).fill(body); mail.input(texts.getAdminMailBody()).press("Tab");
        String[][] infos = {{texts.getAdminMailRecipient(), texts.getAdminMailRecipientHint(), texts.getAdminMailValidationRecipient()},
                {texts.getAdminMailSubject(), texts.getAdminMailSubjectHint(), texts.getAdminMailValidationSubject()},
                {texts.getAdminMailBody(), texts.getAdminMailBodyHint(), texts.getAdminMailValidationBody()}};
        for (String[] info : infos) {
            Locator button = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(info[0]).setExact(true));
            button.focus();
            UiActions.complete(page, () -> page.keyboard().press("Enter"));
            Locator tooltip = page.getByRole(AriaRole.TOOLTIP);
            assertThat(tooltip).containsText(info[1]); assertThat(tooltip).containsText(info[2]);
            page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName(texts.getAdminMailTitle()).setLevel(1).setExact(true)).click();
            assertThat(tooltip).hasCount(0);
        }
        var other = new de.derpeterson.browsertests.i18n.UiTexts(texts.language().equals("de") ? "en" : "de");
        try {
            new UserMenu(page).selectLanguage(other.language());
            assertThat(mail.input(other.getAdminMailRecipient())).hasValue(email);
            assertThat(mail.input(other.getAdminMailSubject())).hasValue(subject);
            assertThat(mail.input(other.getAdminMailBody())).hasValue(body);
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(other.getAdminMailBody()).setExact(true)).click();
            assertThat(page.getByRole(AriaRole.TOOLTIP)).containsText(other.getAdminMailBodyHint());
            AllurePlaywright.attachScreenshot("Mail-Info und erhaltene Nachricht nach Sprachwechsel", page);
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(other.getAdminMailBody()).setExact(true)).click();
        } finally { new UserMenu(page).selectTestLanguage(); }
        assertThat(mail.input(texts.getAdminMailBody())).hasValue(body);
        assertThat(page.locator("vaadin-notification-card:not([closing])")).hasCount(0);
    }

    @Test @DisplayName("MAIL-03: Enter im Nachrichtentext fügt eine neue Zeile ein und versendet nicht")
    @Description("""
        1. Mail Test öffnen und eine eigene Empfängeradresse samt Betreff eingeben.
        2. Im Nachrichtentext eine erste Zeile, Enter und eine zweite Zeile tippen; Fokus anschließend verlassen.
        3. Beide Zeilen unverändert prüfen; kein Versand- oder Fehlerhinweis darf erscheinen.
        4. Bewusst über den Sendebutton versenden und beide Zeilen in der eigenen echten SMTP-Mail prüfen.
        **Erwartet:** Mehrzeilige Texte sind ohne unbeabsichtigten Versand möglich; erst der Button sendet.
        """)
    void bodyEnterInsertsNewlineWithoutSending() {
        LoginPage login = new LoginPage(page); login.open(); login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
        new UserMenu(page).selectTestLanguage();
        AdminMailPage mail = new AdminMailPage(page); mail.open();
        String email = "multiline-" + UUID.randomUUID() + "@example.test", subject = "Multiline " + UUID.randomUUID();
        mail.input(texts.getAdminMailRecipient()).fill(email); mail.input(texts.getAdminMailSubject()).fill(subject);
        Locator body = mail.input(texts.getAdminMailBody());
        UiActions.completeValueChange(page, () -> {
            body.fill("First line"); body.press("End"); body.press("Enter"); body.pressSequentially("Second line ÄÖÜ"); body.press("Tab");
        });
        assertThat(body).hasValue("First line\nSecond line ÄÖÜ");
        assertThat(page.locator("vaadin-notification-card:not([closing])")).hasCount(0);
        mail.send(); mail.notice(texts.getAdminMailSuccess());
        MailpitInbox inbox = new MailpitInbox(context);
        inbox.assertPlainContent(inbox.awaitAdminMail(email, subject), "First line\nSecond line ÄÖÜ");
    }

}
