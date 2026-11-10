package de.derpeterson.app.ui.components;

import static de.derpeterson.app.ui.components.CosmosClasses.REFERENCE_OVERLAY;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.popover.PopoverPosition;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.shared.Registration;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.service.UserNotificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/** Keine Request-Proxys auf Push-Threads: die beim Attach gebundene Sitzung wird frisch geprüft. */
public class UserNotificationBell extends Div {
  private final transient MessageProperties messageProperties;
  private final transient UserNotificationService service;
  private final Button target;
  private final Span count = new Span();
  private final Popover popover = new Popover();
  private transient HttpSession session;
  private transient Registration registration;
  private UserNotificationsPanel panel;
  private SerializableConsumer<Long> unreadCountListener = value -> {};

  public void setUnreadCountListener(SerializableConsumer<Long> listener) {
    unreadCountListener = listener;
  }

  public UserNotificationBell(
      MessageProperties messageProperties,
      UserNotificationService service,
      HttpServletRequest request,
      Button target) {
    this.messageProperties = messageProperties;
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
    popover.getThemeNames().add("cosmos-notifications");
    popover.setAriaLabel(messageProperties.getNotificationsTitle());
    popover.addOpenedChangeListener(e -> {
      if (e.isOpened()) {
        refresh();
      }
    });
    addAttachListener(event -> {
      session = request.getSession(false);
      panel = new UserNotificationsPanel(messageProperties, service, session, true);
      if (referenceStyle) {
        panel.addClassName(REFERENCE_OVERLAY);
      }
      panel.setCloseAction(popover::close);
      panel.setDetailOpenListener(open -> {
        popover.setCloseOnOutsideClick(!open);
        popover.setCloseOnEsc(!open);
      });
      popover.removeAll();
      popover.add(panel);
      try {
        Long owner = service.ownerId(session);
        registration =
            service
                .events()
                .register(ids -> {
                  if (!ids.contains(owner)) {
                    return;
                  }
                  event
                      .getUI()
                      .access(() -> {
                        if (isAttached()) {
                          refresh();
                        }
                      });
                });
      } catch (org.springframework.security.access.AccessDeniedException _) {
        target.setEnabled(false);
      }
      refresh();
    });
    addDetachListener(event -> {
      if (registration != null) {
        registration.remove();
      }
      registration = null;
      session = null;
      popover.close();
    });
  }

  private boolean referenceStyle;

  public void useReferenceStyle() {
    referenceStyle = true;
    popover.getThemeNames().add("cosmos-reference");
    if (panel != null) {
      panel.addClassName(REFERENCE_OVERLAY);
    }
  }

  public void refreshTexts() {
    popover.setAriaLabel(messageProperties.getNotificationsTitle());
    if (panel != null) {
      panel.refreshTexts();
    }
    if (isAttached()) {
      refresh();
    }
  }

  private void refresh() {
    try {
      long unread = service.unreadCount(session);
      unreadCountListener.accept(unread);
      target.setEnabled(true);
      count.setVisible(unread > 0);
      count.setText(unread > 99 ? "99+" : Long.toString(unread));
      target.setAriaLabel(messageProperties.getNotificationsTitle() + " (" + unread + ")");
      if (popover.isOpened() && panel != null) {
        panel.refresh();
      }
    } catch (RuntimeException _) {
      unreadCountListener.accept(-1L);
      count.setVisible(false);
      target.setEnabled(false);
      popover.close();
      if (panel != null) {
        panel.unavailable();
      }
    }
  }
}
