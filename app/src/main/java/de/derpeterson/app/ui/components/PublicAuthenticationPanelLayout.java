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
public final class PublicAuthenticationPanelLayout extends Div {
  public PublicAuthenticationPanelLayout(
      Component authenticationFormComponent,
      ExternalLoginFlow externalLoginFlow,
      HttpServletRequest httpServletRequest,
      MessageProperties messageProperties,
      BooleanSupplier rememberLoginSupplier) {
    super(authenticationFormComponent);
    addClassName("public-auth-panels");

    ExternalLoginButtons externalLoginButtons =
        new ExternalLoginButtons(
            externalLoginFlow,
            httpServletRequest,
            messageProperties,
            rememberLoginSupplier,
            ExternalLoginButtons.Presentation.PANEL);

    if (!externalLoginButtons.isVisible()) {
      return;
    }

    addClassName("public-auth-panels--external");
    Span alternativeLabelSpan = new Span(messageProperties.getExternalOr());
    Div loginDividerDiv = new Div(alternativeLabelSpan);
    loginDividerDiv.addClassName("external-login-divider");
    loginDividerDiv.getElement().setAttribute("aria-hidden", "true");

    ComponentUtil.addListener(
        UI.getCurrent(),
        LanguageChangeEvent.class,
        event -> alternativeLabelSpan.setText(messageProperties.getExternalOr()));

    add(loginDividerDiv, externalLoginButtons);
  }
}
