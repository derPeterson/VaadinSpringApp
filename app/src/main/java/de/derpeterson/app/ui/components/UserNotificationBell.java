package de.derpeterson.app.ui.components;

import static de.derpeterson.app.ui.components.CosmosCssClasses.REFERENCE_OVERLAY;

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
  private final transient UserNotificationService userNotificationService;
  private final Button notificationBellButton;
  private final Span unreadCountBadge = new Span();
  private final Popover notificationPopover = new Popover();
  private transient HttpSession httpSession;
  private transient Registration notificationChangeRegistration;
  private UserNotificationsPanel notificationPreviewPanel;
  private SerializableConsumer<Long> unreadCountListener = value -> {};

  public void setUnreadCountListener(SerializableConsumer<Long> listener) {
    unreadCountListener = listener;
  }

  public UserNotificationBell(
      MessageProperties messageProperties,
      UserNotificationService userNotificationService,
      HttpServletRequest request,
      Button notificationBellButton) {
    this.messageProperties = messageProperties;
    this.userNotificationService = userNotificationService;
    this.notificationBellButton = notificationBellButton;
    addClassName("cosmos-notification-bell");
    unreadCountBadge.addClassName("cosmos-notification-count");
    unreadCountBadge.setVisible(false);
    unreadCountBadge.getElement().setAttribute("aria-hidden", "true");
    add(notificationBellButton, unreadCountBadge, notificationPopover);
    notificationPopover.setTarget(notificationBellButton);
    notificationPopover.setOpenOnClick(true);
    notificationPopover.setOpenOnFocus(false);
    notificationPopover.setOpenOnHover(false);
    notificationPopover.setCloseOnEsc(true);
    notificationPopover.setCloseOnOutsideClick(true);
    notificationPopover.setAutofocus(true);
    notificationPopover.setPosition(PopoverPosition.BOTTOM_END);
    notificationPopover.getThemeNames().add("cosmos");
    notificationPopover.getThemeNames().add("cosmos-notifications");
    notificationPopover.setAriaLabel(messageProperties.getNotificationsTitle());
    notificationPopover.addOpenedChangeListener(openedChangeEvent -> {
      if (openedChangeEvent.isOpened()) {
        refreshNotificationBell();
      }
    });
    addAttachListener(event -> {
      httpSession = request.getSession(false);
      notificationPreviewPanel =
          new UserNotificationsPanel(messageProperties, userNotificationService, httpSession, true);

      if (usesReferenceStyle) {
        notificationPreviewPanel.addClassName(REFERENCE_OVERLAY);
      }
      notificationPreviewPanel.setCloseAction(notificationPopover::close);
      notificationPreviewPanel.setDetailOpenListener(open -> {
        notificationPopover.setCloseOnOutsideClick(!open);
        notificationPopover.setCloseOnEsc(!open);
      });
      notificationPopover.removeAll();
      notificationPopover.add(notificationPreviewPanel);
      try {
        Long owner = userNotificationService.ownerId(httpSession);
        notificationChangeRegistration =
            userNotificationService
                .events()
                .register(ids -> {
                  if (!ids.contains(owner)) {
                    return;
                  }
                  event
                      .getUI()
                      .access(() -> {
                        if (isAttached()) {
                          refreshNotificationBell();
                        }
                      });
                });
      } catch (org.springframework.security.access.AccessDeniedException _) {
        notificationBellButton.setEnabled(false);
      }
      refreshNotificationBell();
    });
    addDetachListener(event -> {
      if (notificationChangeRegistration != null) {
        notificationChangeRegistration.remove();
      }
      notificationChangeRegistration = null;
      httpSession = null;
      notificationPopover.close();
    });
  }

  private boolean usesReferenceStyle;

  public void useReferenceStyle() {
    usesReferenceStyle = true;
    notificationPopover.getThemeNames().add("cosmos-reference");

    if (notificationPreviewPanel != null) {
      notificationPreviewPanel.addClassName(REFERENCE_OVERLAY);
    }
  }

  public void refreshTexts() {
    notificationPopover.setAriaLabel(messageProperties.getNotificationsTitle());

    if (notificationPreviewPanel != null) {
      notificationPreviewPanel.refreshTexts();
    }

    if (isAttached()) {
      refreshNotificationBell();
    }
  }

  private void refreshNotificationBell() {
    try {
      long unreadNotificationCount = userNotificationService.unreadCount(httpSession);
      unreadCountListener.accept(unreadNotificationCount);

      notificationBellButton.setEnabled(true);
      unreadCountBadge.setVisible(unreadNotificationCount > 0);
      unreadCountBadge.setText(
          unreadNotificationCount > 99 ? "99+" : Long.toString(unreadNotificationCount));
      notificationBellButton.setAriaLabel(
          messageProperties.getNotificationsTitle() + " (" + unreadNotificationCount + ")");

      if (notificationPopover.isOpened() && notificationPreviewPanel != null) {
        notificationPreviewPanel.refreshInbox();
      }
    } catch (RuntimeException _) {
      unreadCountListener.accept(-1L);

      unreadCountBadge.setVisible(false);
      notificationBellButton.setEnabled(false);
      notificationPopover.close();

      if (notificationPreviewPanel != null) {
        notificationPreviewPanel.unavailable();
      }
    }
  }
}
