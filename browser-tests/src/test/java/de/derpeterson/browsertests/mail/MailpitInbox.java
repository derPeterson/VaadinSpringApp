package de.derpeterson.browsertests.mail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
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

/** Reads captured SMTP mail only; never creates mail or bypasses the registration UI. */
public final class MailpitInbox {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final BrowserContext context;
    private final de.derpeterson.browsertests.i18n.UiTexts texts = de.derpeterson.browsertests.i18n.UiTexts.selected();
    private final String baseUrl = System.getProperty("browser.tests.mailpitUrl");

    public MailpitInbox(BrowserContext context) { this.context = context; }

    public record VerificationMail(String id, String recipient, String subject) { }

    @Step("Auf die regulär versandte Verifikationsmail an die eigene Testadresse warten")
    public VerificationMail awaitVerificationMail(String recipient) {
        AtomicReference<JsonNode> found = new AtomicReference<>();
        context.waitForCondition(() -> {
            List<JsonNode> matches = new ArrayList<>();
            for (JsonNode mail : getJson("/api/v1/messages?limit=100").path("messages")) {
                if (addressedTo(mail, recipient)) matches.add(mail);
            }
            assertTrue(matches.size() <= 1, "Mehrere Mails an dieselbe neue Testadresse.");
            if (matches.isEmpty()) return false;
            found.set(matches.getFirst());
            return true;
        }, new BrowserContext.WaitForConditionOptions().setTimeout(30_000));
        JsonNode mail = getJson("/api/v1/message/" + found.get().path("ID").asText());
        assertTrue(addressedTo(mail, recipient), "Mail gehört nicht zur eigenen Testadresse.");
        String subject = mail.path("Subject").asText();
        assertEquals(texts.text("email.verification.subject"), subject, "Verifikationsbetreff muss zur ausgewählten Sprache passen.");
        String html = mail.path("HTML").asText();
        assertFalse(html.isBlank(), "Verifikationsmail hat keinen HTML-Inhalt.");
        assertFalse(html.contains("{{"), "Nicht ersetzte Platzhalter in der versandten Mail.");
        Allure.attachment("SMTP-Mail: Empfänger und Betreff", "text/plain",
                "Empfänger: " + recipient + "\nBetreff: " + subject + "\nMailpit-ID: " + mail.path("ID").asText());
        return new VerificationMail(mail.path("ID").asText(), recipient, subject);
    }

    @Step("Empfangene HTML-Mail ansehen und ihren echten Aktivierungsbutton anklicken")
    public Page openVerificationLink(VerificationMail mail) {
        Page messagePage = context.newPage();
        try {
            messagePage.navigate(baseUrl + "/view/" + mail.id() + ".html");
            assertThat(messagePage.locator("body")).containsText("Browser Testuser");
            assertThat(messagePage.locator("body")).containsText("VaadinSpringApp (Browser-Test)");
            Locator link = messagePage.locator("a[href*='/verification/']");
            assertThat(link).hasCount(1);
            assertThat(link).isVisible();
            assertLocalVerificationLink(link.getAttribute("href"));
            AllurePlaywright.attachScreenshot("Tatsächlich empfangene Verifikationsmail", messagePage);
            Page activation = context.waitForPage(link::click);
            new de.derpeterson.browsertests.pages.VerificationPage(activation).assertActivated();
            AllurePlaywright.attachScreenshot("Aktivierung über den Mailbutton", activation);
            return activation;
        } finally {
            messagePage.close();
        }
    }

    static void assertLocalVerificationLink(String link) {
        URI actual = URI.create(Objects.requireNonNull(link, "Verifikationslink fehlt."));
        URI expected = URI.create(System.getProperty("browser.tests.baseUrl"));
        assertEquals(expected.getScheme(), actual.getScheme(), "Falsches Link-Protokoll.");
        assertEquals(expected.getHost(), actual.getHost(), "Link zeigt auf einen fremden Server.");
        assertEquals(expected.getPort(), actual.getPort(), "Link zeigt auf einen fremden Port.");
        assertNull(actual.getUserInfo());
        assertNull(actual.getQuery());
        assertNull(actual.getFragment());
        assertTrue(actual.getPath().matches("/verification/[0-9a-f-]{36}"), "Falsches Aktivierungsziel.");
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
