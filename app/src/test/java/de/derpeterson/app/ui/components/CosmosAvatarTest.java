package de.derpeterson.app.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import de.derpeterson.app.model.enums.IconSize;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class CosmosAvatarTest {
  @ParameterizedTest
  @EnumSource(IconSize.class)
  void identityChangesKeepNativeSizeAndLeaveInteractionToTheWrapper(IconSize avatarIconSize) {
    CosmosAvatar userAvatar = new CosmosAvatar(avatarIconSize);
    userAvatar.setIdentity("Alexandra Maria", "Beispielmann Sonnenschein");

    assertEquals("vaadin-avatar", userAvatar.getElement().getTag());
    assertEquals("Alexandra Maria Beispielmann Sonnenschein", userAvatar.getName());
    assertEquals("AB", userAvatar.getAbbreviation());
    assertEquals(
        avatarIconSize.getWidth() + avatarIconSize.getUnit().getSymbol(),
        userAvatar.getStyle().get("--cosmos-avatar-size"));
    assertFalse(userAvatar.isTooltipEnabled());
    assertEquals("-1", userAvatar.getElement().getAttribute("tabindex"));
    assertEquals("true", userAvatar.getElement().getAttribute("aria-hidden"));

    userAvatar.setIdentity(null, null);

    assertEquals("", userAvatar.getName());
    assertEquals("", userAvatar.getAbbreviation());
  }
}
