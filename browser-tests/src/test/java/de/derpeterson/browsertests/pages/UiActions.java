package de.derpeterson.browsertests.pages;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;

/** Wartet nach echten UI-Aktionen auf die Vaadin-Serverantwort vor Navigation/Gegenaktionen. */
public final class UiActions {
    private static final ObjectMapper JSON = new ObjectMapper();
    private UiActions() {}

    public static void complete(Page page, Runnable action) {
        page.waitForResponse(UiActions::isUiResponse, action).finished();
    }

    public static void completeValueChange(Page page, Runnable action) {
        // Dropdown-Öffnen und Auswahl können getrennte Antworten erzeugen. Erst die
        // Antwort auf die tatsächlich übermittelte Wertänderung bestätigt deren Verarbeitung.
        page.waitForResponse(response -> isUiResponse(response) && carriesValueChange(response), action).finished();
    }

    private static boolean isUiResponse(Response response) {
        return response.request().method().equals("POST")
                && response.url().startsWith(System.getProperty("browser.tests.baseUrl"))
                && response.url().contains("v-r=uidl");
    }

    private static boolean carriesValueChange(Response response) {
        String body = response.request().postData();
        if (body == null) return false;
        try {
            for (var change : JSON.readTree(body).path("rpc")) {
                String property = change.path("property").asText();
                if (change.path("type").asText().equals("mSync")
                        && (property.equals("value") || property.equals("selectedItem"))) return true;
            }
        } catch (JsonProcessingException ignored) {
            // Andere POST-Inhalte sind keine bestätigte Feldänderung.
        }
        return false;
    }
}
