package de.derpeterson.app.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.helper.ui.ValidationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.security.ExternalLoginFlow;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.ExternalIdentityService;
import de.derpeterson.app.ui.components.AppIcon;
import de.derpeterson.app.ui.components.PublicPageComponent;
import de.derpeterson.app.ui.components.PublicPanelComponent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;

/** Fehlende Angaben nach Anbieterprüfung; diese Seite verleiht selbst keine Portalanmeldung. */
@Route("external-registration")
@AnonymousAllowed
@StyleSheet("custom-theme/cosmos-account.css")
@StyleSheet("custom-theme/cosmos-external-login.css")
public final class ExternalRegistrationView extends VerticalLayout
    implements BeforeEnterObserver, HasDynamicTitle {
  private final transient ExternalLoginFlow flow;
  private final transient ExternalIdentityService identities;
  private final transient MessageProperties texts;
  private final transient HttpServletRequest request;
  private final PublicPageComponent page;
  private final H2 title = new H2();
  private final TextField first = new TextField();
  private final TextField last = new TextField();
  private final EmailField email = new EmailField();
  private final Span error = new Span();
  private final Button submit = new Button();
  private final Button cancel = new Button();
  private boolean awaitingVerification;
  private boolean expiredRegistration;

  public ExternalRegistrationView(
      ConfigService config,
      MessageProperties texts,
      ExternalLoginFlow flow,
      ExternalIdentityService identities,
      HttpServletRequest request) {
    this.flow = flow;
    this.identities = identities;
    this.texts = texts;
    this.request = request;
    setPadding(false);
    setSpacing(false);
    setWidthFull();
    page = new PublicPageComponent(config, texts, PublicPageComponent.Page.REGISTER);
    title.addClassName("cosmos-headline");
    first.setId("external-register-first");
    last.setId("external-register-last");
    email.setId("external-register-email");
    first.setRequiredIndicatorVisible(true);
    last.setRequiredIndicatorVisible(true);
    email.setRequiredIndicatorVisible(true);
    first.setMaxLength(255);
    last.setMaxLength(255);
    email.setMaxLength(255);
    first.setWidthFull();
    last.setWidthFull();
    email.setWidthFull();
    submit.setId("external-register-submit");
    submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    submit.setPrefixComponent(AppIcon.USER_PLUS.create());
    submit.addClickListener(event -> register());
    cancel.setPrefixComponent(AppIcon.CLOSE.create());
    cancel.addClickListener(
        event -> {
          flow.cancel(request);
          UI.getCurrent().navigate(LoginView.class);
        });
    PublicPanelComponent panel =
        new PublicPanelComponent(
            AppIcon.USER_PLUS.create(),
            title,
            texts::getExternalCompleteHint,
            new Component[] {error, first, last, email},
            cancel,
            submit);
    panel.addClassName("external-registration-panel");
    page.getCardContainer().add(panel);
    add(page);
    ComponentUtil.addListener(UI.getCurrent(), LanguageChangeEvent.class, event -> refreshTexts());
    refreshTexts();
  }

  private void refreshTexts() {
    title.setText(texts.getExternalCompleteTitle());
    first.setLabel(texts.getRegistrationFirstNameField());
    last.setLabel(texts.getRegistrationLastNameField());
    email.setLabel(texts.getPublicEmailField());
    first.setPlaceholder(texts.getAccountFirstNamePlaceholder());
    last.setPlaceholder(texts.getAccountLastNamePlaceholder());
    email.setPlaceholder(texts.getAccountEmailPlaceholder());
    submit.setText(texts.getBaseRegistrationButton());
    cancel.setText(texts.getAdminUsersCancel());
    if (error.isVisible()) {
      error.setText(currentErrorText());
    }
  }

  private String currentErrorText() {
    if (awaitingVerification) {
      return texts.getExternalVerifyEmail();
    }
    return expiredRegistration ? texts.getExternalDraftExpired() : texts.getExternalError();
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    error.setVisible(false);
    if (event.getLocation().getQueryParameters().getParameters().containsKey("error")) {
      hideForm();
      error.setText(texts.getExternalError());
      error.setVisible(true);
      return;
    }
    try {
      ExternalLoginFlow.Registration pending = flow.registration(request);
      first.setValue(value(pending.identity().firstName()));
      last.setValue(value(pending.identity().lastName()));
      email.setValue(value(pending.identity().email()));
    } catch (AccessDeniedException _) {
      expiredRegistration = true;
      hideForm();
      error.setText(texts.getExternalDraftExpired());
      error.setVisible(true);
    }
  }

  private void register() {
    if (!ValidationHelper.validateRequiredInputs(List.of(first, last, email), texts)
        || !ValidationHelper.validateEmailValidInputs(List.of(email), texts)) {
      return;
    }
    submit.setEnabled(false);
    try {
      ExternalLoginFlow.Registration pending = flow.registration(request);
      UserEntity user =
          identities.register(
              pending.identity(),
              email.getValue(),
              first.getValue(),
              last.getValue(),
              VaadinSession.getCurrent().getLocale());
      flow.cancel(request);
      if (user.isEnabled()) {
        UI.getCurrent()
            .getPage()
            .setLocation(
                flow.finishLogin(
                    request,
                    (HttpServletResponse) VaadinResponse.getCurrent(),
                    user,
                    pending.identity().provider(),
                    pending.remember()));
      } else {
        hideForm();
        awaitingVerification = true;
        error.setText(texts.getExternalVerifyEmail());
        error.setVisible(true);
      }
    } catch (IOException | RuntimeException _) {
      NotificationHelper.getInstance()
          .showNotification(
              texts::getBaseFailedTitle,
              texts::getExternalError,
              -1,
              NotificationHelper.NotificationType.ERROR);
    } finally {
      submit.setEnabled(true);
    }
  }

  @Override
  public String getPageTitle() {
    return texts.getExternalCompleteTitle();
  }

  private void hideForm() {
    first.setVisible(false);
    last.setVisible(false);
    email.setVisible(false);
    submit.setVisible(false);
  }

  private String value(String value) {
    return value == null ? "" : value;
  }
}
