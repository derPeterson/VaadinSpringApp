package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.security.ExternalLoginFlow;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Dieselben Anbieteraktionen in Anmeldung und Registrierung. */
@StyleSheet("custom-theme/cosmos-external-login.css")
public final class ExternalLoginButtons extends Div {
  public enum Presentation {
    INLINE,
    PANEL
  }

  public ExternalLoginButtons(
      ExternalLoginFlow flow,
      HttpServletRequest request,
      MessageProperties messageProperties,
      BooleanSupplier remember) {
    this(flow, request, messageProperties, remember, Presentation.INLINE);
  }

  public ExternalLoginButtons(
      ExternalLoginFlow flow,
      HttpServletRequest request,
      MessageProperties messageProperties,
      BooleanSupplier remember,
      Presentation presentation) {
    setClassName("external-login-buttons");
    List<String> providers = flow.providers();
    if (providers.isEmpty()) {
      setVisible(false);
      return;
    }
    if (presentation == Presentation.PANEL) {
      addClassName("external-login-panel");
      Span kicker = new Span();
      kicker.addClassName("external-login-kicker");
      H2 title = new H2();
      title.setId("external-login-title");
      getElement().setAttribute("role", "region");
      getElement().setAttribute("aria-labelledby", "external-login-title");
      Paragraph hint = new Paragraph();
      hint.addClassName("external-login-hint");
      Runnable translate =
          () -> {
            kicker.setText(messageProperties.getExternalPanelKicker());
            title.setText(messageProperties.getExternalPanelTitle());
            hint.setText(messageProperties.getExternalPanelHint());
          };
      translate.run();
      ComponentUtil.addListener(
          UI.getCurrent(), LanguageChangeEvent.class, event -> translate.run());
      add(kicker, title, hint);
    }
    Supplier<String> googleText =
        presentation == Presentation.PANEL
            ? messageProperties::getExternalContinueGoogle
            : messageProperties::getExternalGoogle;
    Supplier<String> microsoftText =
        presentation == Presentation.PANEL
            ? messageProperties::getExternalContinueMicrosoft
            : messageProperties::getExternalMicrosoft;
    Div actions = new Div();
    actions.addClassName("external-provider-actions");
    for (String provider : providers) {
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
          () -> button.setText(provider.equals("google") ? googleText.get() : microsoftText.get());
      translate.run();
      ComponentUtil.addListener(
          UI.getCurrent(), LanguageChangeEvent.class, event -> translate.run());
      button.addClickListener(event ->
          UI.getCurrent()
              .getPage()
              .setLocation(
                  flow.beginLogin(
                      request,
                      provider,
                      remember.getAsBoolean(),
                      VaadinSession.getCurrent().getLocale())));
      actions.add(button);
    }
    add(actions);
    if (presentation == Presentation.INLINE) {
      Span alternative = new Span();
      alternative.addClassName("external-login-alternative");
      Runnable translate = () -> alternative.setText(messageProperties.getExternalOr());
      translate.run();
      ComponentUtil.addListener(
          UI.getCurrent(), LanguageChangeEvent.class, event -> translate.run());
      add(alternative);
    }
  }
}
