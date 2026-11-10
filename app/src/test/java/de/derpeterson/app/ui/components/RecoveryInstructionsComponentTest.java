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
  void localizedInstructionsKeepFourStepsAndEverySentWord(String languageTag) {
    MessageProperties messageProperties =
        MessageProperties.forLocale(new CustomI18NProvider(), Locale.forLanguageTag(languageTag));
    String instructionSnapshotText = messageProperties.getSupportRecoveryInstructions();
    RecoveryInstructionsComponent recoveryInstructionsComponent =
        new RecoveryInstructionsComponent(instructionSnapshotText);

    assertTrue(instructionSnapshotText.length() <= SupportTicketService.MESSAGE_LIMIT);
    assertEquals(
        4,
        streamComponentTree(recoveryInstructionsComponent).filter(Badge.class::isInstance).count());

    String renderedInstructionsText =
        recoveryInstructionsComponent.getHeading()
            + recoveryInstructionsComponent.getDescription()
            + recoveryInstructionsComponent.getElement().getTextRecursively();
    for (String instructionParagraph :
        instructionSnapshotText
            .lines()
            .filter(instructionLine -> !instructionLine.isBlank())
            .toList()) {
      assertTrue(renderedInstructionsText.contains(instructionParagraph), instructionParagraph);
    }

    assertEquals(
        1,
        streamComponentTree(recoveryInstructionsComponent)
            .filter(
                candidateComponent -> candidateComponent.hasClassName("cosmos-security-warning"))
            .count());
    assertEquals(
        1,
        streamComponentTree(recoveryInstructionsComponent)
            .filter(candidateComponent -> candidateComponent.hasClassName("support-help-notice"))
            .count());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void sharedDetailsButtonSynchronizesExpandedStateAndKeepsItsLabel(String languageTag) {
    MessageProperties messageProperties =
        MessageProperties.forLocale(new CustomI18NProvider(), Locale.forLanguageTag(languageTag));
    Details instructionDetails = new Details();
    instructionDetails.setId("instruction-details");
    DetailsToggleButton detailsToggleButton =
        new DetailsToggleButton(messageProperties, instructionDetails, "Guide");

    assertEquals("false", detailsToggleButton.getElement().getAttribute("aria-expanded"));

    detailsToggleButton.click();

    assertTrue(instructionDetails.isOpened());
    assertEquals("true", detailsToggleButton.getElement().getAttribute("aria-expanded"));

    detailsToggleButton.click();

    assertFalse(instructionDetails.isOpened());
    assertEquals("false", detailsToggleButton.getElement().getAttribute("aria-expanded"));
    assertEquals(messageProperties.getBaseDetailsButton(), detailsToggleButton.getText());
  }

  private static Stream<Component> streamComponentTree(Component rootComponent) {
    return Stream.concat(
        Stream.of(rootComponent),
        rootComponent
            .getChildren()
            .flatMap(RecoveryInstructionsComponentTest::streamComponentTree));
  }
}
