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
    Span kicker = new Span();
    kicker.addClassName("portal-home-kicker");
    H2 title = new H2();
    Span welcome = new Span();
    title.add(welcome, new Span("."));
    Paragraph description = new Paragraph();
    description.addClassName("portal-home-description");
    Button login =
        new Button(
            "", AppIcon.SIGN_IN.create(), event -> UI.getCurrent().navigate(LoginView.class));
    login.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    Button register =
        new Button(
            "",
            AppIcon.USER_PLUS.create(),
            event -> UI.getCurrent().navigate(RegistrationView.class));
    Div actions = new Div(login, register);
    actions.addClassName("portal-home-actions");
    Div copy = new Div(kicker, title, description, actions);
    copy.addClassName("portal-home-copy");
    Image planet = new Image("icons/portal-home.svg", "");
    planet.addClassName("portal-home-planet");
    Div art = new Div(planet);
    art.addClassName("portal-home-art");
    Div layout = new Div(copy, art);
    layout.addClassName("portal-home-layout");
    Span notice = new Span();
    Span star = new Span("✦");
    star.getElement().setAttribute("aria-hidden", "true");
    Div footer = new Div(star, notice);
    footer.addClassName("portal-home-note");
    add(layout, footer);
    Runnable translate =
        () -> {
          kicker.setText(messageProperties.getPublicHomeKicker());
          welcome.setText(messageProperties.getPortalHomeWelcome());
          description.setText(messageProperties.getPublicHomeDescription());
          notice.setText(messageProperties.getPortalHomeNotice());
          login.setText(messageProperties.getBaseLoginButton());
          register.setText(messageProperties.getBaseRegistrationButton());
        };
    translate.run();
    ComponentUtil.addListener(UI.getCurrent(), LanguageChangeEvent.class, event -> translate.run());
  }
}
