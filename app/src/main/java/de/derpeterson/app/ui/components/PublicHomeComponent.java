package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.views.LoginView;
import de.derpeterson.app.views.RegistrationView;

/** Öffentlicher Startseitenausblick mit demselben lokal gesicherten Kosmosmotiv. */
public class PublicHomeComponent extends Div {
  public PublicHomeComponent(MessageProperties messageProperties) {
    addClassNames("portal-home-page", "portal-home-card", "public-home");

    Span welcomeKickerSpan = new Span();
    welcomeKickerSpan.addClassName("portal-home-kicker");
    H2 welcomeHeading = new H2();
    Span welcomeTextSpan = new Span();
    welcomeHeading.add(welcomeTextSpan, new Span("."));
    Paragraph publicHomeDescriptionParagraph = new Paragraph();
    publicHomeDescriptionParagraph.addClassName("portal-home-description");

    Button loginButton =
        new Button(
            "", AppIcon.SIGN_IN.create(), event -> UI.getCurrent().navigate(LoginView.class));
    loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    Button registerButton =
        new Button(
            "",
            AppIcon.USER_PLUS.create(),
            event -> UI.getCurrent().navigate(RegistrationView.class));

    Div authenticationActionsDiv = new Div(loginButton, registerButton);
    authenticationActionsDiv.addClassName("portal-home-actions");
    Div welcomeContentDiv =
        new Div(
            welcomeKickerSpan,
            welcomeHeading,
            publicHomeDescriptionParagraph,
            authenticationActionsDiv);
    welcomeContentDiv.addClassName("portal-home-copy");

    Image planetImage = new Image("icons/portal-home.svg", "");
    planetImage.addClassName("portal-home-planet");
    Div planetArtDiv = new Div(planetImage);
    planetArtDiv.addClassName("portal-home-art");
    Div welcomeLayoutDiv = new Div(welcomeContentDiv, planetArtDiv);
    welcomeLayoutDiv.addClassName("portal-home-layout");

    Span publicHomeNoticeSpan = new Span();
    Span noticeStarSpan = new Span("✦");
    noticeStarSpan.getElement().setAttribute("aria-hidden", "true");
    Div publicHomeFooterDiv = new Div(noticeStarSpan, publicHomeNoticeSpan);
    publicHomeFooterDiv.addClassName("portal-home-note");

    add(welcomeLayoutDiv, publicHomeFooterDiv);

    Runnable refreshHomeTextsAction =
        () -> {
          welcomeKickerSpan.setText(messageProperties.getPublicHomeKicker());
          welcomeTextSpan.setText(messageProperties.getPortalHomeWelcome());
          publicHomeDescriptionParagraph.setText(messageProperties.getPublicHomeDescription());
          publicHomeNoticeSpan.setText(messageProperties.getPortalHomeNotice());
          loginButton.setText(messageProperties.getBaseLoginButton());
          registerButton.setText(messageProperties.getBaseRegistrationButton());
        };

    refreshHomeTextsAction.run();
    ComponentUtil.addListener(
        UI.getCurrent(), LanguageChangeEvent.class, event -> refreshHomeTextsAction.run());
  }
}
