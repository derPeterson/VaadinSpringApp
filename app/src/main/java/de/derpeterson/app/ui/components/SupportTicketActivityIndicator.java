package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.shared.Registration;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.service.SupportTicketService;
import de.derpeterson.app.service.SupportTicketService.Activity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/** Zeigt dieselben relevanten ungelesenen Nachrichten wie die administrative Vorgangsliste. */
public final class SupportTicketActivityIndicator extends Badge {
  private final transient MessageProperties messageProperties;
  private final transient SupportTicketService supportTicketService;
  private final Tooltip unreadTicketTooltip;
  private transient HttpSession httpSession;
  private transient volatile Long viewerId;
  private transient Registration ticketChangeRegistration;

  public SupportTicketActivityIndicator(
      MessageProperties messageProperties,
      SupportTicketService supportTicketService,
      HttpServletRequest request,
      SideNavItem navigation) {
    this.messageProperties = messageProperties;
    this.supportTicketService = supportTicketService;
    setId("admin-nav-support-activity");
    addClassName("admin-nav-support-activity");
    addThemeVariants(BadgeVariant.ERROR);
    setVisible(false);
    navigation.setSuffixComponent(this);
    unreadTicketTooltip = Tooltip.forComponent(navigation);
    // Am stets angehängten Navigationseintrag registrieren, auch wenn der Punkt unsichtbar ist.
    navigation.addAttachListener(event -> {
      HttpSession attachedSession = request.getSession(false);
      httpSession = attachedSession;
      ticketChangeRegistration =
          supportTicketService
              .events()
              .register(change -> {
                if (!change.affects(viewerId, true)) {
                  return;
                }
                event
                    .getUI()
                    .access(() -> {
                      if (navigation.isAttached() && httpSession == attachedSession) {
                        refreshUnreadTicketIndicator();
                      }
                    });
              });
      refreshUnreadTicketIndicator();
    });
    navigation.addDetachListener(event -> {
      if (ticketChangeRegistration != null) {
        ticketChangeRegistration.remove();
        ticketChangeRegistration = null;
      }
      httpSession = null;
      viewerId = null;
    });
  }

  private void refreshUnreadTicketIndicator() {
    try {
      Activity state = supportTicketService.loadTicketActivity(httpSession);
      viewerId = state.viewerId();
      setVisible(state.unread());
    } catch (RuntimeException _) {
      // Ohne gültigen aktuellen Zugriff keinen Aktivitätshinweis anzeigen.
      setVisible(false);
    }
    refreshTexts();
  }

  public void refreshTexts() {
    String label = isVisible() ? messageProperties.getSupportNewActivity() : "";
    unreadTicketTooltip.setText(label);
    getElement().setAttribute("aria-label", label);
  }
}
