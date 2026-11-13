package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.dependency.StyleSheet;
import de.derpeterson.app.model.enums.IconSize;
import java.text.BreakIterator;
import java.util.Locale;
import java.util.Objects;

/** Nativer Bild-/Initialenkern; Status, Tooltip und Bildberechtigung bleiben beim Aufrufer. */
@StyleSheet("custom-theme/cosmos-avatar.css")
public class CosmosAvatar extends Avatar {
  public CosmosAvatar() {
    addClassName("cosmos-avatar-circle");
    setTooltipEnabled(false);
    getElement().setAttribute("tabindex", "-1");
    getElement().setAttribute("aria-hidden", "true");
  }

  public CosmosAvatar(IconSize avatarIconSize) {
    this();

    getStyle()
        .set(
            "--cosmos-avatar-size",
            avatarIconSize.getWidth() + avatarIconSize.getUnit().getSymbol());
  }

  public void setIdentity(String firstNameText, String lastNameText) {
    String displayNameText =
        (Objects.toString(firstNameText, "").strip()
                + " "
                + Objects.toString(lastNameText, "").strip())
            .strip();

    setName(displayNameText);
    setAbbreviation(initialsFor(firstNameText, lastNameText));
  }

  static String initialsFor(String firstNameText, String lastNameText) {
    return firstUnicodeInitial(firstNameText) + firstUnicodeInitial(lastNameText);
  }

  private static String firstUnicodeInitial(String nameText) {
    String trimmedNameText = Objects.toString(nameText, "").strip();
    if (trimmedNameText.isEmpty()) {
      return "";
    }

    BreakIterator characterBreakIterator = BreakIterator.getCharacterInstance(Locale.ROOT);
    characterBreakIterator.setText(trimmedNameText);
    String uppercaseInitialText =
        trimmedNameText.substring(0, characterBreakIterator.next()).toUpperCase(Locale.ROOT);

    // Auch bei Großschreibung mit mehreren Zeichen (z.B. ß → SS) nur ein Initial anzeigen.
    characterBreakIterator.setText(uppercaseInitialText);
    return uppercaseInitialText.substring(0, characterBreakIterator.next());
  }
}
