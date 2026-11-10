package de.derpeterson.app.ui.components;

import static de.derpeterson.app.ui.components.CosmosClasses.REFERENCE_PORTAL;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.i18n.MessageTexts;
import de.derpeterson.app.i18n.PublicLanguagePreference;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.AppRoute;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.IconSize;
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
  private boolean accountSecurity;
  private SerializableRunnable refreshTexts;
  private final transient MessageProperties messageProperties;
  private final VerticalLayout pageCardsLayout = new VerticalLayout();
  private final HorizontalLayout securityIdentityLayout = new HorizontalLayout();

  public PublicPageComponent(
      ConfigService configService, MessageProperties messageProperties, Page page) {
    this.page = page;
    this.messageProperties = messageProperties;
    addClassNames("cosmos-theme", REFERENCE_PORTAL, "cosmos-reference-public");
    getElement().setAttribute("data-public-page", page.name().toLowerCase(Locale.ROOT));
    String serviceNameText = configService.getString(ConfigEntry.SERVICE_NAME);

    Image serviceLogoImage = new Image("icons/service-logo.svg", "");
    boolean restrictedSecurityPage = page == Page.TWO_FACTOR || page == Page.RECOVERY_HELP;
    Component brandComponent =
        restrictedSecurityPage
            ? new Div(serviceLogoImage, new Span(serviceNameText))
            : new Anchor(
                AppRoute.HOME.getSecurityRoute(), serviceLogoImage, new Span(serviceNameText));
    brandComponent.getElement().getClassList().add("public-brand");
    brandComponent.getElement().setAttribute("aria-label", serviceNameText);

    Button homeButton =
        new Button("", AppIcon.HOME.create(), event -> UI.getCurrent().navigate(HomeView.class));

    Button loginButton =
        new Button(
            "", AppIcon.SIGN_IN.create(), event -> UI.getCurrent().navigate(LoginView.class));

    Button registerButton =
        new Button(
            "",
            AppIcon.USER_PLUS.create(),
            event -> UI.getCurrent().navigate(RegistrationView.class));
    homeButton.setId("public-nav-home");
    loginButton.setId("public-nav-login");
    registerButton.setId("public-nav-register");
    homeButton.setClassName(ACTIVE_NAVIGATION_CLASS, page == Page.HOME);
    loginButton.setClassName(
        ACTIVE_NAVIGATION_CLASS,
        page == Page.LOGIN || page == Page.TWO_FACTOR || page == Page.RECOVERY_HELP);
    registerButton.setClassName(ACTIVE_NAVIGATION_CLASS, page == Page.REGISTER);

    Button germanLanguageButton = language("DE", Locale.GERMAN);
    Button englishLanguageButton = language("EN", Locale.ENGLISH);
    Div languageButtonsDiv = new Div(germanLanguageButton, englishLanguageButton);
    languageButtonsDiv.addClassName("public-languages");
    languageButtonsDiv.getElement().setAttribute("role", "group");

    Div publicHeaderDiv =
        restrictedSecurityPage
            ? createPublicHeader(brandComponent, true, languageButtonsDiv)
            : createPublicHeader(
                brandComponent, false, homeButton, loginButton, registerButton, languageButtonsDiv);

    Span pageKickerSpan = new Span();
    pageKickerSpan.addClassName("public-kicker");
    H1 pageTitleHeading = new H1();
    Span pageDescriptionSpan = new Span();
    pageDescriptionSpan.addClassName("public-page-description");

    Div pageTitleRowDiv = new Div(pageTitleHeading, pageDescriptionSpan);
    pageTitleRowDiv.addClassName("public-title-row");
    Div pageHeadingDiv = new Div(pageKickerSpan, pageTitleRowDiv);
    pageHeadingDiv.addClassName("public-page-heading");
    pageCardsLayout.setPadding(false);
    pageCardsLayout.setSpacing(false);
    pageCardsLayout.addClassName("public-cards");
    if (page == Page.HOME) {
      pageCardsLayout.add(new PublicHomeComponent(messageProperties));
    }

    PortalPageComponent publicPageLayout =
        new PortalPageComponent(pageHeadingDiv, null, pageCardsLayout);
    publicPageLayout.addClassName("public-main");
    setFrameContent(
        publicHeaderDiv,
        publicPageLayout,
        new CosmosCopyrightComponent(
            configService,
            Clock.systemDefaultZone(),
            messageProperties::getAccountCopyrightTemplate));

    SerializableRunnable refreshPageTextsAction =
        () -> {
          homeButton.setText(messageProperties.getHomeTitle());
          homeButton.setAriaLabel(messageProperties.getHomeTitle());
          loginButton.setAriaLabel(messageProperties.getBaseLoginButton());
          registerButton.setAriaLabel(messageProperties.getBaseRegistrationButton());
          loginButton.setText(messageProperties.getBaseLoginButton());
          registerButton.setText(messageProperties.getBaseRegistrationButton());
          pageKickerSpan.setText(
              restrictedSecurityPage
                  ? messageProperties.getTwoFactorAreaLabel(accountSecurity)
                  : messageProperties.getPublicAreaLabel());
          pageTitleHeading.setText(this.page.title.apply(messageProperties));
          pageDescriptionSpan.setText(this.page.description.apply(messageProperties));
          germanLanguageButton
              .getElement()
              .setAttribute(
                  "aria-pressed",
                  Boolean.toString(
                      "de".equals(VaadinSession.getCurrent().getLocale().getLanguage())));
          englishLanguageButton
              .getElement()
              .setAttribute(
                  "aria-pressed",
                  Boolean.toString(
                      "en".equals(VaadinSession.getCurrent().getLocale().getLanguage())));
        };
    refreshTexts = refreshPageTextsAction;
    refreshPageTextsAction.run();
    ComponentUtil.addListener(
        UI.getCurrent(), LanguageChangeEvent.class, event -> refreshPageTextsAction.run());
  }

  private Div createPublicHeader(
      Component brandComponent, boolean restrictedSecurityPage, Component... navigationComponents) {
    Div publicNavigationDiv = new Div(navigationComponents);
    publicNavigationDiv.addClassName("public-navigation");
    publicNavigationDiv.getElement().setAttribute("role", "navigation");
    if (restrictedSecurityPage) {
      publicNavigationDiv.addClassName("public-security-navigation");
      securityIdentityLayout.setId("public-security-identity");
      securityIdentityLayout.setSpacing(false);
      securityIdentityLayout.setAlignItems(Alignment.CENTER);
      securityIdentityLayout.addClassName("public-security-identity");
      securityIdentityLayout.setVisible(false);
      publicNavigationDiv.add(securityIdentityLayout);
    }

    Div headerInnerDiv = new Div(brandComponent, publicNavigationDiv);
    headerInnerDiv.addClassName("public-header-inner");
    Div publicHeaderDiv = new Div(headerInnerDiv);
    publicHeaderDiv.addClassName("public-header");
    return publicHeaderDiv;
  }

  private Button language(String languageLabelText, Locale buttonLocale) {
    Button languageButton =
        new Button(
            languageLabelText,
            event -> {
              VaadinSession.getCurrent().setLocale(buttonLocale);
              PublicLanguagePreference.save(buttonLocale);
              LanguageChangeEvent.fire(UI.getCurrent(), buttonLocale);
            });
    languageButton.getElement().setAttribute("lang", buttonLocale.getLanguage());
    languageButton.setAriaLabel(buttonLocale.getDisplayLanguage(buttonLocale));
    return languageButton;
  }

  public void showPage(Page page) {
    this.page = page;
    getElement().setAttribute("data-public-page", page.name().toLowerCase(Locale.ROOT));
    refreshTexts.run();
  }

  public VerticalLayout getCardContainer() {
    return pageCardsLayout;
  }

  public void setAccountSecurity(boolean accountSecurity, UserEntity accountUserEntity) {
    this.accountSecurity = accountSecurity;
    securityIdentityLayout.removeAll();
    securityIdentityLayout.setVisible(accountUserEntity != null);

    if (accountUserEntity != null) {
      OverlayUserIcon accountAvatarIcon =
          new OverlayUserIcon(messageProperties, accountUserEntity, null, IconSize.PIXEL_32);
      accountAvatarIcon.getElement().setAttribute("aria-hidden", "true");
      Span accountNameSpan =
          new Span(
              (accountUserEntity.getFirstName() + " " + accountUserEntity.getLastName()).strip());
      Div accountDetailsDiv = new Div(accountNameSpan, new Span(accountUserEntity.getEmail()));
      accountDetailsDiv.addClassName("cosmos-header-identity");
      securityIdentityLayout.add(accountAvatarIcon, accountDetailsDiv);
    }
    refreshTexts.run();
  }
}
