package de.derpeterson.app.helper.ui;

import static com.vaadin.flow.component.button.ButtonVariant.LUMO_TERTIARY_INLINE;
import static de.derpeterson.app.ui.components.CosmosClasses.REFERENCE_PORTAL;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.ui.components.AppIcon;
import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * Kurzlebige UI-Rückmeldungen (Toasts); gespeicherte Hinweise verwaltet UserNotificationService.
 */
@NoArgsConstructor
public class NotificationHelper implements Serializable {

  private Span titleText = null;
  // Request-/Service-Supplier nicht mit einer passivierten UI serialisieren.
  private transient Supplier<String> titleSupplier = null;
  private Html messageText = null;
  private transient Supplier<String> messageSupplier = null;
  private transient Supplier<List<String>> detailsSupplier;
  private VerticalLayout textLayout = null;
  private Button closeButton;

  public enum NotificationType {
    NORMAL,
    ERROR,
    SUCCESS,
    WARNING
  }

  public static final Integer DEFAULT_NOTIFICATION_DURATION = 3000;

  private Notification currentNotification = new Notification();

  public static NotificationHelper getInstance() {
    UI currentUi = UI.getCurrent();
    if (currentUi == null) {
      throw new IllegalStateException("Notifications require a current UI");
    }
    // Meldungen gehören zum einzelnen Browserfenster, auch bei mehreren Tabs derselben Sitzung.
    // Kein globaler Helfer darf Hinweise anderer Benutzer schließen oder deren Texte verändern.
    NotificationHelper uiNotificationHelper =
        ComponentUtil.getData(currentUi, NotificationHelper.class);

    if (uiNotificationHelper == null) {
      uiNotificationHelper = new NotificationHelper();
      ComponentUtil.setData(currentUi, NotificationHelper.class, uiNotificationHelper);
    }

    return uiNotificationHelper;
  }

  public void showNotification(Supplier<String> messageSupplier) {
    showNotification(null, messageSupplier, DEFAULT_NOTIFICATION_DURATION, NotificationType.NORMAL);
  }

  public void showNotification(
      Supplier<String> messageSupplier, NotificationType notificationType) {
    showNotification(null, messageSupplier, DEFAULT_NOTIFICATION_DURATION, notificationType);
  }

  public void showNotification(
      Supplier<String> titleSupplier,
      Supplier<String> messageSupplier,
      NotificationType notificationType) {
    showNotification(
        titleSupplier, messageSupplier, DEFAULT_NOTIFICATION_DURATION, notificationType);
  }

  public void showNotification(
      Supplier<String> titleSupplier,
      Supplier<String> messageSupplier,
      Integer displayDurationMillis,
      NotificationType notificationType) {
    showNotificationWithDetails(
        titleSupplier, messageSupplier, null, displayDurationMillis, notificationType);
  }

  public void showNotificationWithDetails(
      Supplier<String> titleSupplier,
      Supplier<String> messageSupplier,
      Supplier<List<String>> detailsSupplier,
      Integer displayDurationMillis,
      NotificationType notificationType) {
    closeAndClearAllNotifications();

    this.titleSupplier = titleSupplier;
    this.messageSupplier = messageSupplier;
    this.detailsSupplier = detailsSupplier;

    String notificationTitle = null;
    if (titleSupplier != null) {
      notificationTitle = titleSupplier.get();
    }

    String notificationMessage = null;
    if (messageSupplier != null) {
      notificationMessage = messageSupplier.get();
    }

    if (containsClass(UI.getCurrent(), "cosmos-theme")) {
      currentNotification.getElement().getThemeList().add("cosmos");
    }
    boolean usesReferenceTheme = containsClass(UI.getCurrent(), REFERENCE_PORTAL);
    if (usesReferenceTheme) {
      currentNotification.getElement().getThemeList().add("cosmos-reference");
      currentNotification
          .getElement()
          .getThemeList()
          .add("cosmos-reference-" + notificationType.name().toLowerCase(Locale.ROOT));
    }
    currentNotification.setPosition(Notification.Position.TOP_CENTER);
    currentNotification.setDuration(displayDurationMillis);

    buildNotificationContent(
        notificationTitle, notificationMessage, notificationType, usesReferenceTheme);
    currentNotification.open();
  }

  private void buildNotificationContent(
      String notificationTitle,
      String notificationMessage,
      NotificationType notificationType,
      boolean usesReferenceTheme) {
    this.titleText = null;
    if (StringUtils.isNotEmpty(notificationTitle)) {
      titleText = new Span(notificationTitle);
      titleText.addClassNames("cosmos-toast-title", LumoUtility.FontWeight.SEMIBOLD);
      currentNotification.add(titleText);
    }

    HorizontalLayout notificationContentLayout = new HorizontalLayout();
    notificationContentLayout.setAlignItems(FlexComponent.Alignment.CENTER);
    notificationContentLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
    notificationContentLayout.setWidthFull();
    notificationContentLayout.setSpacing(false);
    notificationContentLayout.addClassNames(LumoUtility.Gap.LARGE);

    addStatusIcon(notificationContentLayout, notificationType, usesReferenceTheme);

    this.textLayout = new VerticalLayout();
    textLayout.addClassNames(LumoUtility.Padding.SMALL);
    textLayout.setSpacing(false);
    textLayout.setWidth(null);

    createTextLayoutContent(notificationMessage);

    notificationContentLayout.add(textLayout);

    currentNotification.add(notificationContentLayout);
    closeButton = createNotificationCloseButton(currentNotification);
    closeButton.setAriaLabel(localizedCloseLabel());
    if (usesReferenceTheme) {
      closeButton.setIcon(AppIcon.CLOSE_CIRCLE.create());
    }
    currentNotification.add(closeButton);
  }

  private void addStatusIcon(
      HorizontalLayout notificationContentLayout,
      NotificationType notificationType,
      boolean usesReferenceTheme) {
    SvgIcon statusSvgIcon = null;
    if (notificationType == NotificationType.ERROR) {
      statusSvgIcon = AppIcon.ERROR.create();
      statusSvgIcon.addClassNames(LumoUtility.TextColor.ERROR);
      if (titleText != null) {
        titleText.addClassNames(LumoUtility.TextColor.ERROR);
      }
    } else if (notificationType == NotificationType.SUCCESS) {
      statusSvgIcon = AppIcon.CHECK_CIRCLE.create();
      statusSvgIcon.addClassNames(LumoUtility.TextColor.SUCCESS);
      if (titleText != null) {
        titleText.addClassNames(LumoUtility.TextColor.SUCCESS);
      }
    } else if (notificationType == NotificationType.WARNING) {
      statusSvgIcon = AppIcon.WARNING.create();
      statusSvgIcon.addClassNames(LumoUtility.TextColor.WARNING);
      if (titleText != null) {
        titleText.addClassNames(LumoUtility.TextColor.WARNING);
      }
    }

    if (statusSvgIcon != null) {
      statusSvgIcon.addClassName("cosmos-notification-icon");
    }

    if (usesReferenceTheme) {
      AppIcon statusAppIcon =
          switch (notificationType) {
            case ERROR -> AppIcon.ERROR;
            case SUCCESS -> AppIcon.CHECK_CIRCLE;
            case WARNING -> AppIcon.WARNING;
            case NORMAL -> AppIcon.INFO_CIRCLE;
          };
      SvgIcon referenceStatusSvgIcon = statusAppIcon.create();
      referenceStatusSvgIcon.addClassName("cosmos-notification-icon");
      notificationContentLayout.add(referenceStatusSvgIcon);
    } else if (statusSvgIcon != null) {
      notificationContentLayout.add(statusSvgIcon);
    }
  }

  /** Auch verschachtelte Seitenrahmen bestimmen die Palette der ausgelagerten Meldung. */
  private static boolean containsClass(Component rootComponent, String className) {
    return rootComponent != null
        && (rootComponent.getElement().getClassList().contains(className)
            || rootComponent
                .getChildren()
                .anyMatch(childComponent -> containsClass(childComponent, className)));
  }

  private static String localizedCloseLabel() {
    return new MessageProperties(new CustomI18NProvider()).getNotificationsClose();
  }

  private static Button createNotificationCloseButton(Notification targetNotification) {
    Button closeButton =
        new Button(AppIcon.CLOSE_CIRCLE.create(), clickEvent -> targetNotification.close());
    closeButton.addClassNames(LumoUtility.TextColor.PRIMARY);
    closeButton.addThemeVariants(LUMO_TERTIARY_INLINE);

    return closeButton;
  }

  private void createTextLayoutContent(String notificationMessage) {
    textLayout.removeAll();
    if (titleText != null) {
      textLayout.add(titleText);
    }
    messageText = null;
    if (StringUtils.isNotBlank(notificationMessage)) {
      messageText = new Html(HtmlHelper.ensureParagraphTags(notificationMessage));
      messageText.addClassNames("cosmos-notification-message", LumoUtility.Margin.NONE);
      messageText.getStyle().set("max-width", "520px");
      textLayout.add(messageText);
    }

    if (detailsSupplier != null) {
      UnorderedList detailsList = new UnorderedList();
      detailsList.addClassNames("cosmos-notification-message", "cosmos-notification-details");
      detailsSupplier.get().forEach(detailText -> detailsList.add(new ListItem(detailText)));
      textLayout.add(detailsList);
    }
  }

  public void closeAndClearAllNotifications() {
    if (currentNotification.isOpened()) {
      currentNotification.close();
    }

    this.titleText = null;
    this.titleSupplier = null;
    this.messageText = null;
    this.messageSupplier = null;
    this.detailsSupplier = null;
    this.closeButton = null;

    currentNotification = new Notification();
  }

  public void updateText() {
    if (closeButton != null) {
      closeButton.setAriaLabel(localizedCloseLabel());
    }

    if (titleText != null && titleSupplier != null) {
      titleText.setText(titleSupplier.get());
    }
    if (messageSupplier != null || detailsSupplier != null) {
      createTextLayoutContent(messageSupplier == null ? null : messageSupplier.get());
    }
  }
}
