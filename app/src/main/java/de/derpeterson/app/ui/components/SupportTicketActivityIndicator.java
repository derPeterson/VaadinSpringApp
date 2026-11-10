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
  private final transient SupportTicketService service;
  private final Tooltip tooltip;
  private transient HttpSession session;
  private transient volatile Long viewerId;
  private transient Registration registration;

  public SupportTicketActivityIndicator(
      MessageProperties messageProperties,
      SupportTicketService service,
      HttpServletRequest request,
      SideNavItem navigation) {
    this.messageProperties = messageProperties;
    this.service = service;
    setId("admin-nav-support-activity");
    addClassName("admin-nav-support-activity");
    addThemeVariants(BadgeVariant.ERROR);
    setVisible(false);
    navigation.setSuffixComponent(this);
    tooltip = Tooltip.forComponent(navigation);
    // Am stets angehängten Navigationseintrag registrieren, auch wenn der Punkt unsichtbar ist.
    navigation.addAttachListener(event -> {
      HttpSession attachedSession = request.getSession(false);
      session = attachedSession;
      registration =
          service
              .events()
              .register(change -> {
                if (!change.affects(viewerId, true)) {
                  return;
                }
                event
                    .getUI()
                    .access(() -> {
                      if (navigation.isAttached() && session == attachedSession) {
                        refresh();
                      }
                    });
              });
      refresh();
    });
    navigation.addDetachListener(event -> {
      if (registration != null) {
        registration.remove();
        registration = null;
      }
      session = null;
      viewerId = null;
    });
  }

  private void refresh() {
    try {
      Activity state = service.activity(session);
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
    tooltip.setText(label);
    getElement().setAttribute("aria-label", label);
  }
}
