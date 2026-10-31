package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.security.ExternalLoginFlow;
import jakarta.servlet.http.HttpServletRequest;
import java.util.function.BooleanSupplier;

/** Gemeinsame Anordnung für lokales Formular und optionalen externen Schnellzugang. */
@StyleSheet("custom-theme/cosmos-external-login.css")
public final class PublicAuthenticationPanels extends Div {
  public PublicAuthenticationPanels(
      Component form,
      ExternalLoginFlow flow,
      HttpServletRequest request,
      MessageProperties texts,
      BooleanSupplier remember) {
    super(form);
    addClassName("public-auth-panels");
    ExternalLoginButtons external =
        new ExternalLoginButtons(
            flow, request, texts, remember, ExternalLoginButtons.Presentation.PANEL);
    if (!external.isVisible()) {
      return;
    }
    addClassName("public-auth-panels--external");
    Span alternative = new Span(texts.getExternalOr());
    Div divider = new Div(alternative);
    divider.addClassName("external-login-divider");
    divider.getElement().setAttribute("aria-hidden", "true");
    ComponentUtil.addListener(
        UI.getCurrent(),
        LanguageChangeEvent.class,
        event -> alternative.setText(texts.getExternalOr()));
    add(divider, external);
  }
}
