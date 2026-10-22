package de.derpeterson.app.events;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import java.util.Locale;
import lombok.Getter;

@Getter
public class LanguageChangeEvent extends ComponentEvent<UI> {

  private final Locale newLocale;

  public LanguageChangeEvent(UI source, Locale newLocale) {
    super(source, false);
    this.newLocale = newLocale;
  }

  public static void fire(UI ui, Locale newLocale) {
    ComponentUtil.fireEvent(ui, new LanguageChangeEvent(ui, newLocale));
  }
}
