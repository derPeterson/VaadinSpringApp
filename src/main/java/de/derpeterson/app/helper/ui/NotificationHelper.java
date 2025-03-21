package de.derpeterson.app.helper.ui;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Supplier;

import static com.vaadin.flow.component.button.ButtonVariant.LUMO_TERTIARY_INLINE;

@NoArgsConstructor
public class NotificationHelper {

    private Span titleText = null;
    private Supplier<String> titleSupplier = null;
    private Html messageText = null;
    private Supplier<String> messageSupplier = null;
    private VerticalLayout textLayout = null;


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

    public void showNotification(Supplier<String> messageSupplier) {
        showNotification(null, messageSupplier, DEFAULT_NOTIFICATION_DURATION, NotificationType.NORMAL);
    }

    public void showNotification(Supplier<String> messageSupplier, NotificationType notificationType) {
        showNotification(null, messageSupplier, DEFAULT_NOTIFICATION_DURATION, notificationType);
    }

    public void showNotification(Supplier<String> titleSupplier, Supplier<String> messageSupplier, NotificationType notificationType) {
        showNotification(titleSupplier, messageSupplier, DEFAULT_NOTIFICATION_DURATION, notificationType);
    }

    public void showNotification(Supplier<String> titleSupplier, Supplier<String> messageSupplier, Integer duration, NotificationType notificationType) {
        closeAndClearAllNotifications();

        this.titleSupplier = titleSupplier;
        this.messageSupplier = messageSupplier;

        String title = null;
        if (titleSupplier != null) {
            title = titleSupplier.get();
        }
        String message = null;
        if (messageSupplier != null) {
            message = messageSupplier.get();
        }

        currentNotification.setPosition(Notification.Position.TOP_CENTER);
        currentNotification.setDuration(duration);

        this.titleText = null;
        if (StringUtils.isNotEmpty(title)) {
            titleText = new Span(title);
            titleText.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.SEMIBOLD);
            currentNotification.add(titleText);
        }

        HorizontalLayout contentLayout = new HorizontalLayout();
        contentLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        contentLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        contentLayout.setWidthFull();
        contentLayout.setSpacing(false);
        contentLayout.addClassNames(LumoUtility.Gap.LARGE);

        Icon icon = null;
        if (notificationType == NotificationType.ERROR) {
            icon = VaadinIcon.WARNING.create();
            icon.addClassNames(LumoUtility.TextColor.ERROR);
            if (titleText != null) {
                titleText.addClassNames(LumoUtility.TextColor.ERROR);
            }
        } else if (notificationType == NotificationType.SUCCESS) {
            icon = VaadinIcon.CHECK_CIRCLE.create();
            icon.addClassNames(LumoUtility.TextColor.SUCCESS);
            if (titleText != null) {
                titleText.addClassNames(LumoUtility.TextColor.SUCCESS);
            }
        } else if (notificationType == NotificationType.WARNING) {
            icon = VaadinIcon.BELL.create();
            icon.addClassNames(LumoUtility.TextColor.WARNING);
            if (titleText != null) {
                titleText.addClassNames(LumoUtility.TextColor.WARNING);
            }
        }

        if (icon != null) {
            icon.addClassNames(LumoUtility.FontSize.XLARGE);
        }

        contentLayout.add(icon);

        this.textLayout = new VerticalLayout();
        textLayout.addClassNames(LumoUtility.Padding.SMALL);
        textLayout.setSpacing(false);
        textLayout.setWidth(null);

        createTextLayoutContent(message);

        contentLayout.add(textLayout);

        currentNotification.add(contentLayout);
        currentNotification.add(createNotificationCloseButton(currentNotification));
        currentNotification.open();
    }

    private static Button createNotificationCloseButton(Notification notification) {
        Button closeButton = new Button(VaadinIcon.CLOSE_CIRCLE.create(),
                clickEvent -> notification.close());
        closeButton.addClassNames(LumoUtility.TextColor.PRIMARY);
        closeButton.addThemeVariants(LUMO_TERTIARY_INLINE);

        return closeButton;
    }

    private void createTextLayoutContent(String message) {
        this.messageText = new Html(HtmlHelper.ensureParagraphTags(message));
        messageText.addClassNames(LumoUtility.Margin.NONE);
        messageText.getStyle().set("max-width", "520px");

        textLayout.removeAll();

        if (titleText != null) {
            textLayout.add(titleText);
            textLayout.addClassNames(LumoUtility.TextColor.SECONDARY);
            messageText.addClassNames(LumoUtility.FontSize.SMALL);
        }
        textLayout.add(messageText);
    }

    public void closeAndClearAllNotifications() {
        if (currentNotification.isOpened()) {
            currentNotification.close();
        }

        this.titleText = null;
        this.titleSupplier = null;
        this.messageText = null;
        this.messageSupplier = null;

        currentNotification = new Notification();
    }

    public void updateText() {
        if (titleText != null && titleSupplier != null) {
            titleText.setText(titleSupplier.get());
        }
        if (messageText != null && messageSupplier != null) {
            createTextLayoutContent(messageSupplier.get());
        }
    }

}
