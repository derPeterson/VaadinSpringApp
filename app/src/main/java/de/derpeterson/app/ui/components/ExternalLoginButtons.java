package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.security.ExternalLoginFlow;
import jakarta.servlet.http.HttpServletRequest;
import java.util.function.BooleanSupplier;

/** Dieselben Anbieteraktionen in Anmeldung und Registrierung. */
@StyleSheet("custom-theme/cosmos-external-login.css")
public final class ExternalLoginButtons extends Div {
  public ExternalLoginButtons(
      ExternalLoginFlow flow,
      HttpServletRequest request,
      MessageProperties texts,
      BooleanSupplier remember) {
    setClassName("external-login-buttons");
    if (flow.providers().isEmpty()) {
      setVisible(false);
      return;
    }
    for (String provider : flow.providers()) {
      Button button = new Button();
      button.setId("external-login-" + provider);
      button.setWidthFull();
      button.addClassName("external-provider-button");
      Image brand =
          new Image(
              "icons/external-login/"
                  + (provider.equals("google") ? "google.png" : "microsoft.svg"),
              "");
      brand.addClassName("external-provider-mark");
      brand.getElement().setAttribute("aria-hidden", "true");
      button.setPrefixComponent(brand);
      Runnable translate =
          () ->
              button.setText(
                  provider.equals("google")
                      ? texts.getExternalGoogle()
                      : texts.getExternalMicrosoft());
      translate.run();
      ComponentUtil.addListener(
          UI.getCurrent(), LanguageChangeEvent.class, event -> translate.run());
      button.addClickListener(
          event ->
              UI.getCurrent()
                  .getPage()
                  .setLocation(
                      flow.beginLogin(
                          request,
                          provider,
                          remember.getAsBoolean(),
                          VaadinSession.getCurrent().getLocale())));
      add(button);
    }
    Span alternative = new Span();
    alternative.addClassName("external-login-alternative");
    Runnable translate = () -> alternative.setText(texts.getExternalOr());
    translate.run();
    ComponentUtil.addListener(UI.getCurrent(), LanguageChangeEvent.class, event -> translate.run());
    add(alternative);
  }
}
