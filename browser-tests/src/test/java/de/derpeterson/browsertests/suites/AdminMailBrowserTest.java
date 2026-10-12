package de.derpeterson.browsertests.suites;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.app.config.AdminConfigGroup;
import de.derpeterson.browsertests.*;
import de.derpeterson.browsertests.pages.*;
import de.derpeterson.browsertests.mail.MailpitInbox;
import io.qameta.allure.*;
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
        3. Eigene Testadresse, Unicode-Betreff und Text setzen; versenden und Erfolgshinweis prüfen.
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
        mail.input(texts.getAdminMailBody()).fill(body); mail.send(); mail.notice(texts.getAdminMailSuccess());
        assertThat(page.locator("vaadin-email-field[invalid],vaadin-text-field[invalid],vaadin-text-area[invalid]")).hasCount(0);
        MailpitInbox inbox = new MailpitInbox(context); var captured = inbox.awaitAdminMail(email, subject);
        inbox.assertPlainContent(captured, body); inbox.assertSender(captured, sender);
    }
}
