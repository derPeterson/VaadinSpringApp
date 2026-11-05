package de.derpeterson.app.ui.components;

import static de.derpeterson.app.ui.components.CosmosClasses.REFERENCE_PORTAL;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.i18n.MessageTexts;
import de.derpeterson.app.i18n.PublicLanguagePreference;
import de.derpeterson.app.model.enums.AppRoute;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.views.HomeView;
import de.derpeterson.app.views.LoginView;
import de.derpeterson.app.views.RegistrationView;
import java.time.Clock;
import java.util.Locale;
import java.util.function.Function;

/** Öffentlicher Referenzrahmen; Formulare und ihre Dienste bleiben in den zuständigen Views. */
@StyleSheet("custom-theme/cosmos-public.css")
public class PublicPageComponent extends PageFrameComponent {
  private static final String ACTIVE_NAVIGATION_CLASS = "public-nav-active";

  public enum Page {
    HOME(MessageTexts::getHomeTitle, MessageTexts::getPublicHomeIntroduction),
    LOGIN(MessageTexts::getBaseLoginButton, MessageTexts::getPublicLoginIntroduction),
    REGISTER(MessageTexts::getBaseRegistrationButton, MessageTexts::getPublicRegisterIntroduction),
    FORGOT(MessageTexts::getPublicForgotTitle, MessageTexts::getPublicForgotIntroduction),
    RESET(MessageTexts::getResetPasswordTitle, MessageTexts::getPublicResetIntroduction),
    VERIFICATION(
        MessageTexts::getPublicVerificationTitle, MessageTexts::getPublicVerificationIntroduction),
    EMAIL_CONFIRM(
        MessageTexts::getAccountEmailConfirmationTitle, MessageTexts::getPublicEmailIntroduction),
    RECOVERY(MessageTexts::getPublicRecoveryTitle, MessageTexts::getPublicRecoveryIntroduction),
    TWO_FACTOR(MessageTexts::getTwoFactorPageTitle, MessageTexts::getTwoFactorPageHint),
    RECOVERY_HELP(MessageTexts::getSupportHelpTitle, MessageTexts::getSupportHelpPageHint),
    MAINTENANCE(MessageTexts::getMaintenanceTitle, MessageTexts::getPublicMaintenanceIntroduction);

    private final Function<MessageTexts, String> title;
    private final Function<MessageTexts, String> description;

    Page(Function<MessageTexts, String> title, Function<MessageTexts, String> description) {
      this.title = title;
      this.description = description;
    }
  }

  private Page page;
  private SerializableRunnable refreshTexts;
  private final VerticalLayout cards = new VerticalLayout();

  public PublicPageComponent(ConfigService config, MessageProperties texts, Page page) {
    this.page = page;
    addClassNames("cosmos-theme", REFERENCE_PORTAL, "cosmos-reference-public");
    getElement().setAttribute("data-public-page", page.name().toLowerCase(Locale.ROOT));
    String serviceName = config.getString(ConfigEntry.SERVICE_NAME);
    Image logo = new Image("icons/service-logo.svg", "");
    Anchor brand = new Anchor(AppRoute.HOME.getSecurityRoute(), logo, new Span(serviceName));
    brand.addClassName("public-brand");
    brand.getElement().setAttribute("aria-label", serviceName);
    Button home =
        new Button("", AppIcon.HOME.create(), event -> UI.getCurrent().navigate(HomeView.class));
    Button login =
        new Button(
            "", AppIcon.SIGN_IN.create(), event -> UI.getCurrent().navigate(LoginView.class));
    Button register =
        new Button(
            "",
            AppIcon.USER_PLUS.create(),
            event -> UI.getCurrent().navigate(RegistrationView.class));
    home.setId("public-nav-home");
    login.setId("public-nav-login");
    register.setId("public-nav-register");
    home.setClassName(ACTIVE_NAVIGATION_CLASS, page == Page.HOME);
    login.setClassName(
        ACTIVE_NAVIGATION_CLASS,
        page == Page.LOGIN || page == Page.TWO_FACTOR || page == Page.RECOVERY_HELP);
    register.setClassName(ACTIVE_NAVIGATION_CLASS, page == Page.REGISTER);
    Button german = language("DE", Locale.GERMAN);
    Button english = language("EN", Locale.ENGLISH);
    Div languages = new Div(german, english);
    languages.addClassName("public-languages");
    languages.getElement().setAttribute("role", "group");
    Div navigation = new Div(home, login, register, languages);
    navigation.addClassName("public-navigation");
    navigation.getElement().setAttribute("role", "navigation");
    Div headerInner = new Div(brand, navigation);
    headerInner.addClassName("public-header-inner");
    Div header = new Div(headerInner);
    header.addClassName("public-header");
    Span kicker = new Span();
    kicker.addClassName("public-kicker");
    H1 title = new H1();
    Span description = new Span();
    description.addClassName("public-page-description");
    Div titleRow = new Div(title, description);
    titleRow.addClassName("public-title-row");
    Div heading = new Div(kicker, titleRow);
    heading.addClassName("public-page-heading");
    cards.setPadding(false);
    cards.setSpacing(false);
    cards.addClassName("public-cards");
    if (page == Page.HOME) {
      cards.add(new PublicHomeComponent(texts));
    }
    PortalPageComponent main = new PortalPageComponent(heading, null, cards);
    main.addClassName("public-main");
    setFrameContent(
        header,
        main,
        new CosmosCopyrightComponent(
            config, Clock.systemDefaultZone(), texts::getAccountCopyrightTemplate));
    SerializableRunnable translate =
        () -> {
          home.setText(texts.getHomeTitle());
          home.setAriaLabel(texts.getHomeTitle());
          login.setAriaLabel(texts.getBaseLoginButton());
          register.setAriaLabel(texts.getBaseRegistrationButton());
          login.setText(texts.getBaseLoginButton());
          register.setText(texts.getBaseRegistrationButton());
          kicker.setText(texts.getPublicAreaLabel());
          title.setText(this.page.title.apply(texts));
          description.setText(this.page.description.apply(texts));
          german
              .getElement()
              .setAttribute(
                  "aria-pressed",
                  Boolean.toString(
                      "de".equals(VaadinSession.getCurrent().getLocale().getLanguage())));
          english
              .getElement()
              .setAttribute(
                  "aria-pressed",
                  Boolean.toString(
                      "en".equals(VaadinSession.getCurrent().getLocale().getLanguage())));
        };
    refreshTexts = translate;
    translate.run();
    ComponentUtil.addListener(UI.getCurrent(), LanguageChangeEvent.class, event -> translate.run());
  }

  private Button language(String label, Locale locale) {
    Button button =
        new Button(
            label,
            event -> {
              VaadinSession.getCurrent().setLocale(locale);
              PublicLanguagePreference.save(locale);
              LanguageChangeEvent.fire(UI.getCurrent(), locale);
            });
    button.getElement().setAttribute("lang", locale.getLanguage());
    button.setAriaLabel(locale.getDisplayLanguage(locale));
    return button;
  }

  public void showPage(Page page) {
    this.page = page;
    getElement().setAttribute("data-public-page", page.name().toLowerCase(Locale.ROOT));
    refreshTexts.run();
  }

  public VerticalLayout getCardContainer() {
    return cards;
  }
}
