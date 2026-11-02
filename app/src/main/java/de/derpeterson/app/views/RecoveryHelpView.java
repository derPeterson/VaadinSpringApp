package de.derpeterson.app.views;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.SupportTicketService;
import de.derpeterson.app.ui.components.PublicPageComponent;
import de.derpeterson.app.ui.components.SupportTicketsComponent;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;

/** Ein erster Anmeldenachweis erlaubt ausschließlich Hilfe zum eigenen Konto. */
@Route(RecoveryHelpView.ROUTE)
@PermitAll
@StyleSheet("custom-theme/cosmos-account.css")
@StyleSheet("custom-theme/cosmos-mfa.css")
public final class RecoveryHelpView extends VerticalLayout {
  public static final String ROUTE = "recovery-help";

  public RecoveryHelpView(
      ConfigService config,
      MessageProperties texts,
      SupportTicketService tickets,
      HttpServletRequest request) {
    setPadding(false);
    setSpacing(false);
    setWidthFull();
    PublicPageComponent page =
        new PublicPageComponent(config, texts, PublicPageComponent.Page.RECOVERY_HELP);
    SupportTicketsComponent support = new SupportTicketsComponent(texts, tickets, request, false);
    Button back =
        new Button(
            texts.getTwoFactorBack(),
            event ->
                UI.getCurrent().getPage().setLocation(request.getContextPath() + "/two-factor"));
    page.getCardContainer().add(support, back);
    add(page);
    ComponentUtil.addListener(
        UI.getCurrent(),
        LanguageChangeEvent.class,
        event -> {
          support.refreshTexts();
          back.setText(texts.getTwoFactorBack());
        });
  }
}
