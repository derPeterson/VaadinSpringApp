package de.derpeterson.app.events;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import java.util.Locale;
import lombok.Getter;

@Getter
public class LanguageChangeEvent extends ComponentEvent<UI> {

  private final Locale newLocale;

  public LanguageChangeEvent(UI sourceUi, Locale newLocale) {
    super(sourceUi, false);
    this.newLocale = newLocale;
  }

  public static void fire(UI targetUi, Locale newLocale) {
    ComponentUtil.fireEvent(targetUi, new LanguageChangeEvent(targetUi, newLocale));
  }
}
