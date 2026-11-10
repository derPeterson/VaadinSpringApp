package de.derpeterson.app.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.details.Details;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.service.SupportTicketService;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RecoveryInstructionsComponentTest {
  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void localizedInstructionsKeepFourStepsAndEverySentWord(String language) {
    MessageProperties messageProperties =
        MessageProperties.forLocale(new CustomI18NProvider(), Locale.forLanguageTag(language));
    String snapshot = messageProperties.getSupportRecoveryInstructions();
    RecoveryInstructionsComponent guide = new RecoveryInstructionsComponent(snapshot);
    assertTrue(snapshot.length() <= SupportTicketService.MESSAGE_LIMIT);
    assertEquals(4, tree(guide).filter(Badge.class::isInstance).count());
    String rendered =
        guide.getHeading() + guide.getDescription() + guide.getElement().getTextRecursively();
    for (String paragraph : snapshot.lines().filter(line -> !line.isBlank()).toList()) {
      assertTrue(rendered.contains(paragraph), paragraph);
    }
    assertEquals(
        1, tree(guide).filter(part -> part.hasClassName("cosmos-security-warning")).count());
    assertEquals(1, tree(guide).filter(part -> part.hasClassName("support-help-notice")).count());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void sharedDetailsButtonKeepsArrowAndExpandedStateSynchronized(String language) {
    MessageProperties messageProperties =
        MessageProperties.forLocale(new CustomI18NProvider(), Locale.forLanguageTag(language));
    Details details = new Details();
    details.setId("instruction-details");
    DetailsToggleButton toggle = new DetailsToggleButton(messageProperties, details, "Guide");
    assertEquals("false", toggle.getElement().getAttribute("aria-expanded"));
    toggle.click();
    assertTrue(details.isOpened());
    assertEquals("true", toggle.getElement().getAttribute("aria-expanded"));
    toggle.click();
    assertFalse(details.isOpened());
    assertEquals("false", toggle.getElement().getAttribute("aria-expanded"));
    assertEquals(messageProperties.getBaseDetailsButton(), toggle.getText());
  }

  private static Stream<Component> tree(Component component) {
    return Stream.concat(
        Stream.of(component),
        component.getChildren().flatMap(RecoveryInstructionsComponentTest::tree));
  }
}
