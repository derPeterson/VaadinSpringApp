package de.derpeterson.app.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import de.derpeterson.app.ComponentTestTree;
import org.junit.jupiter.api.Test;

class AccountPanelComponentTest {
  @Test
  void nativePanelKeepsAccessibleHeadingBodyAndActionFooter() {
    AccountPanelComponent accountPanel = new AccountPanelComponent("profile", AppIcon.USER);
    accountPanel.setHeading("Personal data", "Edit your profile", "Metadata");
    Span profileContentSpan = new Span("Profile content");
    accountPanel.getBody().add(profileContentSpan);
    Button saveProfileButton = new Button("Save");
    accountPanel.addActionFooter(saveProfileButton);

    assertEquals("vaadin-card", accountPanel.getElement().getTag());
    assertSame(accountPanel, accountPanel.getHeader().getParent().orElseThrow());
    assertEquals(1, accountPanel.getFooterComponents().length);
    assertSame(accountPanel.getBody(), profileContentSpan.getParent().orElseThrow());
    assertTrue(
        ComponentTestTree.streamComponentTree(accountPanel).anyMatch(saveProfileButton::equals));
    assertTrue(accountPanel.getElement().hasAttribute("aria-labelledby"));
  }
}
