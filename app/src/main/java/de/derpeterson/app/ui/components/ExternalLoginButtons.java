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
      ExternalLoginFlow externalLoginFlow,
      HttpServletRequest httpServletRequest,
      MessageProperties messageProperties,
      BooleanSupplier rememberLoginSupplier) {
    this(
        externalLoginFlow,
        httpServletRequest,
        messageProperties,
        rememberLoginSupplier,
        Presentation.INLINE);
  }

  public ExternalLoginButtons(
      ExternalLoginFlow externalLoginFlow,
      HttpServletRequest httpServletRequest,
      MessageProperties messageProperties,
      BooleanSupplier rememberLoginSupplier,
      Presentation presentation) {
    setClassName("external-login-buttons");

    List<String> providerNames = externalLoginFlow.providers();
    if (providerNames.isEmpty()) {
      setVisible(false);
      return;
    }

    if (presentation == Presentation.PANEL) {
      addClassName("external-login-panel");
      Span panelKickerSpan = new Span();
      panelKickerSpan.addClassName("external-login-kicker");

      H2 panelTitleHeading = new H2();
      panelTitleHeading.setId("external-login-title");
      getElement().setAttribute("role", "region");
      getElement().setAttribute("aria-labelledby", "external-login-title");

      Paragraph panelHintParagraph = new Paragraph();
      panelHintParagraph.addClassName("external-login-hint");

      Runnable refreshLoginTextsAction =
          () -> {
            panelKickerSpan.setText(messageProperties.getExternalPanelKicker());
            panelTitleHeading.setText(messageProperties.getExternalPanelTitle());
            panelHintParagraph.setText(messageProperties.getExternalPanelHint());
          };
      refreshLoginTextsAction.run();
      ComponentUtil.addListener(
          UI.getCurrent(), LanguageChangeEvent.class, event -> refreshLoginTextsAction.run());
      add(panelKickerSpan, panelTitleHeading, panelHintParagraph);
    }
    Supplier<String> googleText =
        presentation == Presentation.PANEL
            ? messageProperties::getExternalContinueGoogle
            : messageProperties::getExternalGoogle;
    Supplier<String> microsoftText =
        presentation == Presentation.PANEL
            ? messageProperties::getExternalContinueMicrosoft
            : messageProperties::getExternalMicrosoft;

    Div providerActionsDiv = new Div();
    providerActionsDiv.addClassName("external-provider-actions");

    for (String providerName : providerNames) {
      providerActionsDiv.add(
          createProviderLoginButton(
              providerName,
              googleText,
              microsoftText,
              externalLoginFlow,
              httpServletRequest,
              rememberLoginSupplier));
    }

    add(providerActionsDiv);

    if (presentation == Presentation.INLINE) {
      Span alternativeLabelSpan = new Span();
      alternativeLabelSpan.addClassName("external-login-alternative");

      Runnable refreshLoginTextsAction =
          () -> alternativeLabelSpan.setText(messageProperties.getExternalOr());
      refreshLoginTextsAction.run();
      ComponentUtil.addListener(
          UI.getCurrent(), LanguageChangeEvent.class, event -> refreshLoginTextsAction.run());
      add(alternativeLabelSpan);
    }
  }

  private Button createProviderLoginButton(
      String providerName,
      Supplier<String> googleLabelSupplier,
      Supplier<String> microsoftLabelSupplier,
      ExternalLoginFlow externalLoginFlow,
      HttpServletRequest httpServletRequest,
      BooleanSupplier rememberLoginSupplier) {
    Button providerButton = new Button();
    providerButton.setId("external-login-" + providerName);
    providerButton.setWidthFull();
    providerButton.addClassName("external-provider-button");

    Image providerLogoImage =
        new Image(
            "icons/external-login/"
                + (providerName.equals("google") ? "google.png" : "microsoft.svg"),
            "");
    providerLogoImage.addClassName("external-provider-mark");
    providerLogoImage.getElement().setAttribute("aria-hidden", "true");
    providerButton.setPrefixComponent(providerLogoImage);

    Runnable refreshProviderButtonText =
        () ->
            providerButton.setText(
                providerName.equals("google")
                    ? googleLabelSupplier.get()
                    : microsoftLabelSupplier.get());
    refreshProviderButtonText.run();
    ComponentUtil.addListener(
        UI.getCurrent(), LanguageChangeEvent.class, event -> refreshProviderButtonText.run());
    providerButton.addClickListener(event ->
        UI.getCurrent()
            .getPage()
            .setLocation(
                externalLoginFlow.beginLogin(
                    httpServletRequest,
                    providerName,
                    rememberLoginSupplier.getAsBoolean(),
                    VaadinSession.getCurrent().getLocale())));

    return providerButton;
  }
}
