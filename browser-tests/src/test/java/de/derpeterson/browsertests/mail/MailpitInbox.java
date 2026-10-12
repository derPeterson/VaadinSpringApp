package de.derpeterson.browsertests.mail;

import static de.derpeterson.browsertests.BrowserRoutes.*;
import static de.derpeterson.app.config.AppRouteConstants.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import de.derpeterson.browsertests.pages.ResetPasswordPage;
import de.derpeterson.browsertests.pages.VerificationPage;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import io.qameta.allure.playwright.AllurePlaywright;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/** Reads captured SMTP mail only; never creates mail or bypasses the application's UI. */
public final class MailpitInbox {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final BrowserContext context;
    private final de.derpeterson.browsertests.i18n.UiTexts texts = de.derpeterson.browsertests.i18n.UiTexts.selected();
    private final String baseUrl = System.getProperty("browser.tests.mailpitUrl");

    public MailpitInbox(BrowserContext context) { this.context = context; }

    public record CapturedMail(String id, String recipient, String subject) { }

    @Step("Auf die regulär versandte Verifikationsmail an die eigene Testadresse warten")
    public CapturedMail awaitVerificationMail(String recipient) {
        return awaitMail(recipient, texts.getEmailVerificationSubject());
    }

    @Step("Auf die regulär versandte Reset-Mail an die eigene Testadresse warten")
    public CapturedMail awaitPasswordResetMail(String recipient) {
        return awaitMail(recipient, texts.getEmailResetPasswordSubject());
    }

    public CapturedMail awaitVerificationMailAfter(String recipient, CapturedMail previous) {
        return awaitMail(recipient, texts.getEmailVerificationSubject(), previous.id(), true);
    }

    public CapturedMail awaitAdminMail(String recipient, String subject) {
        return awaitMail(recipient, subject, null, false);
    }

    @Step("Unicode-Betreff und echten Textinhalt der Adminmail prüfen")
    public void assertPlainContent(CapturedMail mail, String body) {
        String received = getJson("/api/v1/message/" + mail.id()).path("Text").asText();
        // SMTP may append exactly one terminal line break; other content stays byte-for-byte significant.
        if (received.endsWith("\r\n")) received = received.substring(0, received.length() - 2);
        else if (received.endsWith("\n")) received = received.substring(0, received.length() - 1);
        assertEquals(body, received, "Unicode-Mailinhalt wurde verändert.");
    }

    @Step("Absender der tatsächlich empfangenen SMTP-Mail prüfen")
    public void assertSender(CapturedMail mail, String expectedSender) {
        assertEquals(expectedSender, getJson("/api/v1/message/" + mail.id()).path("From").path("Address").asText());
    }

    private CapturedMail awaitMail(String recipient, String expectedSubject) {
        return awaitMail(recipient, expectedSubject, null, true);
    }

    private CapturedMail awaitMail(String recipient, String expectedSubject, String previousId, boolean htmlExpected) {
        AtomicReference<JsonNode> found = new AtomicReference<>();
        context.waitForCondition(() -> {
            List<JsonNode> matches = new ArrayList<>();
            for (JsonNode mail : getJson("/api/v1/messages?limit=100").path("messages")) {
                if (addressedTo(mail, recipient) && expectedSubject.equals(mail.path("Subject").asText())
                        && !mail.path("ID").asText().equals(previousId)) matches.add(mail);
            }
            assertTrue(matches.size() <= 1, "Mehrere Mails desselben Typs an dieselbe neue Testadresse.");
            if (matches.isEmpty()) return false;
            found.set(matches.getFirst());
            return true;
        }, new BrowserContext.WaitForConditionOptions().setTimeout(30_000));
        JsonNode mail = getJson("/api/v1/message/" + found.get().path("ID").asText());
        assertTrue(addressedTo(mail, recipient), "Mail gehört nicht zur eigenen Testadresse.");
        String subject = mail.path("Subject").asText();
        assertEquals(expectedSubject, subject, "Mailbetreff muss zum angefragten Typ und zur ausgewählten Sprache passen.");
        String html = mail.path("HTML").asText();
        if (htmlExpected) assertFalse(html.isBlank(), "Erfasste Mail hat keinen HTML-Inhalt.");
        assertFalse(html.contains("{{"), "Nicht ersetzte Platzhalter in der versandten Mail.");
        Allure.attachment("SMTP-Mail: Empfänger und Betreff", "text/plain",
                "Empfänger: " + recipient + "\nBetreff: " + subject + "\nMailpit-ID: " + mail.path("ID").asText());
        return new CapturedMail(mail.path("ID").asText(), recipient, subject);
    }

    @Step("Empfangene HTML-Mail ansehen und ihren echten Aktivierungsbutton anklicken")
    public Page openVerificationLink(CapturedMail mail) {
        Page activation = openVerificationAction(mail);
        new VerificationPage(activation).assertActivated();
        AllurePlaywright.attachScreenshot("Aktivierung über den Mailbutton", activation);
        return activation;
    }

    @Step("Empfangene HTML-Mail ansehen und ihren echten Reset-Button anklicken")
    public Page openPasswordResetLink(CapturedMail mail) {
        Page reset = openPasswordResetAction(mail);
        new ResetPasswordPage(reset).assertFormVisible();
        AllurePlaywright.attachScreenshot("Passwortformular über den echten Mailbutton", reset);
        return reset;
    }

    @Step("Echten Verifikationsbutton öffnen; Ergebnis separat prüfen")
    public Page openVerificationAction(CapturedMail mail) {
        return openActionLink(mail, VERIFICATION_ROUTE, "Tatsächlich empfangene Verifikationsmail");
    }

    @Step("Echten Resetbutton öffnen; Ergebnis separat prüfen")
    public Page openPasswordResetAction(CapturedMail mail) {
        return openActionLink(mail, RESET_PASSWORD_ROUTE, "Tatsächlich empfangene Reset-Mail");
    }

    private Page openActionLink(CapturedMail mail, String route, String screenshotName) {
        Page messagePage = context.newPage();
        try {
            messagePage.navigate(baseUrl + "/view/" + mail.id() + ".html");
            if (VERIFICATION_ROUTE.equals(route)) assertThat(messagePage.locator("body")).containsText("Browser Testuser");
            assertThat(messagePage.locator("body")).containsText("VaadinSpringApp (Browser-Test)");
            Locator link = messagePage.locator("a[href*='" + path(route) + "/']");
            assertThat(link).hasCount(1);
            assertThat(link).isVisible();
            assertLocalActionLink(link.getAttribute("href"), route);
            // Reuse the selected original template instead of a second translated footer catalogue.
            assertThat(messagePage.getByText(expectedFooter(route), new Page.GetByTextOptions().setExact(true))).hasCount(1);
            assertThat(messagePage.locator("body")).not().containsText("{ServiceName}");
            AllurePlaywright.attachScreenshot(screenshotName, messagePage);
            return context.waitForPage(link::click);
        } finally {
            messagePage.close();
        }
    }

    private String expectedFooter(String route) {
        String resource = "/email/" + (VERIFICATION_ROUTE.equals(route) ? "welcome_" : "reset_password_")
                + texts.language() + ".html";
        try (var source = Objects.requireNonNull(MailpitInbox.class.getResourceAsStream(resource), "Mailvorlage fehlt: " + resource)) {
            String template = new String(source.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            // Ab Copyrightzeichen alle Textzeichen bis zum nächsten HTML-Tag; kein eigener Übersetzungstext.
            var footer = java.util.regex.Pattern.compile("©[^<]+").matcher(template);
            assertTrue(footer.find(), "Die kanonische Vorlage enthält keinen Copyright-Footer.");
            // Ein oder mehrere Leerraumzeichen werden wie beim sichtbaren HTML-Text zu einem Leerzeichen.
            return footer.group().strip().replace("{{SERVICE_NAME}}", "VaadinSpringApp (Browser-Test)").replaceAll("\\s+", " ");
        } catch (IOException failure) {
            throw new IllegalStateException("Mailvorlage ist nicht lesbar: " + resource, failure);
        }
    }

    static void assertLocalVerificationLink(String link) { assertLocalActionLink(link, VERIFICATION_ROUTE); }
    static void assertLocalPasswordResetLink(String link) { assertLocalActionLink(link, RESET_PASSWORD_ROUTE); }

    private static void assertLocalActionLink(String link, String route) {
        URI actual = URI.create(Objects.requireNonNull(link, "Mail-Link fehlt."));
        URI expected = URI.create(System.getProperty("browser.tests.baseUrl"));
        assertEquals(expected.getScheme(), actual.getScheme(), "Falsches Link-Protokoll.");
        assertEquals(expected.getHost(), actual.getHost(), "Link zeigt auf einen fremden Server.");
        assertEquals(expected.getPort(), actual.getPort(), "Link zeigt auf einen fremden Port.");
        assertNull(actual.getUserInfo());
        assertNull(actual.getQuery());
        assertNull(actual.getFragment());
        // Pattern.quote schützt die Route; der Token muss 36 Zeichen aus kleinen Hexziffern und Minus enthalten.
        // Dies prüft die vorhandene Token-Form, nicht die Position der UUID-Trennstriche.
        assertTrue(actual.getPath().matches(java.util.regex.Pattern.quote(path(route)) + "/[0-9a-f-]{36}"), "Falsches Mail-Linkziel.");
    }

    private static boolean addressedTo(JsonNode mail, String recipient) {
        for (JsonNode address : mail.path("To"))
            if (recipient.equalsIgnoreCase(address.path("Address").asText())) return true;
        return false;
    }

    private JsonNode getJson(String path) {
        APIResponse response = context.request().get(baseUrl + path);
        try {
            assertTrue(response.ok(), "Mailpit-API nicht erfolgreich: " + response.status());
            return JSON.readTree(response.body());
        } catch (IOException failure) {
            throw new IllegalStateException("Mailpit-Antwort ist kein lesbares JSON.", failure);
        } finally {
            response.dispose();
        }
    }
}
