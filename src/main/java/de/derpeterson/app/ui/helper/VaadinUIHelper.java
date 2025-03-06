package de.derpeterson.app.ui.helper;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.Objects;

import static com.vaadin.flow.component.button.ButtonVariant.LUMO_TERTIARY_INLINE;

public class VaadinUIHelper {

    public enum NotificationType {
        NORMAL,
        ERROR,
        SUCCESS,
        WARNING
    }

    public static final Integer DEFAULT_NOTIFICATION_DURATION = 3000;

    private VaadinUIHelper() {
    }

    public static HorizontalLayout createFullHorizontalSpace() {
        HorizontalLayout hLayout = new HorizontalLayout();
        hLayout.setWidthFull();
        return hLayout;
    }

    public static void showNotification(String message) {
        showNotification(message, DEFAULT_NOTIFICATION_DURATION, NotificationType.NORMAL);
    }

    public static void showNotification(String message, NotificationType notificationType) {
        showNotification(message, DEFAULT_NOTIFICATION_DURATION, notificationType);
    }

    public static void showNotification(String message, Integer duration, NotificationType notificationType) {
        Notification notification = new Notification();
        if (Objects.requireNonNull(notificationType) == NotificationType.ERROR) {
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        } else if (notificationType == NotificationType.SUCCESS) {
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } else if (notificationType == NotificationType.WARNING) {
            notification.addThemeVariants(NotificationVariant.LUMO_WARNING);
        }

        notification.setPosition(Notification.Position.TOP_STRETCH);
        notification.setDuration(duration);

        HorizontalLayout contentLayout = new HorizontalLayout();
        contentLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        contentLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        contentLayout.setWidthFull();
        contentLayout.setSpacing(false);
        contentLayout.addClassNames(LumoUtility.Gap.SMALL);
        if (notificationType == NotificationType.ERROR) {
            contentLayout.add(VaadinIcon.WARNING.create());
        } else if (notificationType == NotificationType.SUCCESS) {
            contentLayout.add(VaadinIcon.CHECK_CIRCLE.create());
        } else if (notificationType == NotificationType.WARNING) {
            contentLayout.add(VaadinIcon.BELL.create());
        }
        contentLayout.add(new Text(message));

        notification.add(contentLayout);
        notification.add(createNotificationCloseButton(notification));
        notification.open();
    }

    private static Button createNotificationCloseButton(Notification notification) {
        Button closeButton = new Button(VaadinIcon.CLOSE_SMALL.create(),
                clickEvent -> notification.close());
        closeButton.addThemeVariants(LUMO_TERTIARY_INLINE);

        return closeButton;
    }
}
