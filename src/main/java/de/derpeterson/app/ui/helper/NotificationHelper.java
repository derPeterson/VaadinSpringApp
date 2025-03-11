package de.derpeterson.app.ui.helper;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

import static com.vaadin.flow.component.button.ButtonVariant.LUMO_TERTIARY_INLINE;

@NoArgsConstructor
public class NotificationHelper {

    public enum NotificationType {
        NORMAL,
        ERROR,
        SUCCESS,
        WARNING
    }

    public static final Integer DEFAULT_NOTIFICATION_DURATION = 3000;

    // Singleton-Instanz
    private static NotificationHelper instance = new NotificationHelper();

    private Notification currentNotification = new Notification();

    public static synchronized NotificationHelper getInstance() {
        if (instance == null) {
            instance = new NotificationHelper();
        }
        return instance;
    }

    public void showNotification(String message) {
        showNotification(null, message, DEFAULT_NOTIFICATION_DURATION, NotificationType.NORMAL);
    }

    public void showNotification(String message, NotificationType notificationType) {
        showNotification(null, message, DEFAULT_NOTIFICATION_DURATION, notificationType);
    }

    public void showNotification(String title, String message, NotificationType notificationType) {
        showNotification(title, message, DEFAULT_NOTIFICATION_DURATION, notificationType);
    }

    public void showNotification(String title, String message, Integer duration, NotificationType notificationType) {
        closeAndClearAllNotifications();

        if (Objects.requireNonNull(notificationType) == NotificationType.ERROR) {
            currentNotification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        } else if (notificationType == NotificationType.SUCCESS) {
            currentNotification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } else if (notificationType == NotificationType.WARNING) {
            currentNotification.addThemeVariants(NotificationVariant.LUMO_WARNING);
        }

        currentNotification.setPosition(Notification.Position.TOP_STRETCH);
        currentNotification.setDuration(duration);

        H4 titleText = null;
        if (StringUtils.isNotEmpty(title)) {
            titleText = new H4(title);
            currentNotification.add(titleText);
        }

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
        Text messageText = new Text(message);
        VerticalLayout textLayout = new VerticalLayout();
        textLayout.setSpacing(false);
        textLayout.setWidth(null);
        if (titleText != null) {
            textLayout.add(titleText);
            textLayout.addClassNames(LumoUtility.TextColor.SECONDARY);
        }
        textLayout.add(messageText);
        contentLayout.add(textLayout);

        currentNotification.add(contentLayout);
        currentNotification.add(createNotificationCloseButton(currentNotification));
        currentNotification.open();
    }

    private static Button createNotificationCloseButton(Notification notification) {
        Button closeButton = new Button(VaadinIcon.CLOSE_BIG.create(),
                clickEvent -> notification.close());
        closeButton.addThemeVariants(LUMO_TERTIARY_INLINE);

        return closeButton;
    }

    public void closeAndClearAllNotifications() {
        if (currentNotification.isOpened()) {
            currentNotification.close();
        }

        currentNotification = new Notification();
    }
}
