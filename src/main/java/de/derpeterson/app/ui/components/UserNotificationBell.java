package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.popover.*;
import com.vaadin.flow.shared.Registration;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.service.UserNotificationService;
import jakarta.servlet.http.*;

/** Keine Request-Proxys auf Push-Threads: die beim Attach gebundene Sitzung wird frisch geprüft. */
public class UserNotificationBell extends Div {
  private final transient MessageProperties texts;
  private final transient UserNotificationService service;
  private final Button target;
  private final Span count = new Span();
  private final Popover popover = new Popover();
  private transient HttpSession session;
  private transient Registration registration;
  private UserNotificationsPanel panel;

  public UserNotificationBell(
      MessageProperties texts,
      UserNotificationService service,
      HttpServletRequest request,
      Button target) {
    this.texts = texts;
    this.service = service;
    this.target = target;
    addClassName("cosmos-notification-bell");
    count.addClassName("cosmos-notification-count");
    count.setVisible(false);
    count.getElement().setAttribute("aria-hidden", "true");
    add(target, count, popover);
    popover.setTarget(target);
    popover.setOpenOnClick(true);
    popover.setOpenOnFocus(false);
    popover.setOpenOnHover(false);
    popover.setCloseOnEsc(true);
    popover.setCloseOnOutsideClick(true);
    popover.setAutofocus(true);
    popover.setPosition(PopoverPosition.BOTTOM_END);
    popover.getThemeNames().add("cosmos");
    popover.setAriaLabel(texts.getNotificationsTitle());
    popover.addOpenedChangeListener(
        e -> {
          if (e.isOpened()) refresh();
        });
    addAttachListener(
        event -> {
          session = request.getSession(false);
          panel = new UserNotificationsPanel(texts, service, session, true);
          popover.removeAll();
          popover.add(panel);
          try {
            var owner = service.ownerId(session);
            registration =
                service
                    .events()
                    .register(
                        ids -> {
                          if (!ids.contains(owner)) return;
                          event
                              .getUI()
                              .access(
                                  () -> {
                                    if (isAttached()) refresh();
                                  });
                        });
          } catch (org.springframework.security.access.AccessDeniedException denied) {
            target.setEnabled(false);
          }
          refresh();
        });
    addDetachListener(
        event -> {
          if (registration != null) registration.remove();
          registration = null;
          session = null;
          popover.close();
        });
  }

  public void refreshTexts() {
    popover.setAriaLabel(texts.getNotificationsTitle());
    if (panel != null) panel.refreshTexts();
    if (isAttached()) refresh();
  }

  private void refresh() {
    try {
      long unread = service.unreadCount(session);
      target.setEnabled(true);
      count.setVisible(unread > 0);
      count.setText(unread > 99 ? "99+" : Long.toString(unread));
      target.setAriaLabel(texts.getNotificationsTitle() + " (" + unread + ")");
      if (popover.isOpened() && panel != null) panel.refresh();
    } catch (RuntimeException failure) {
      count.setVisible(false);
      target.setEnabled(false);
      popover.close();
      if (panel != null) panel.unavailable();
    }
  }
}
