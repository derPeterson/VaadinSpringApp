package de.derpeterson.app.helper.ui;

import static com.vaadin.flow.component.button.ButtonVariant.LUMO_TERTIARY_INLINE;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.ui.components.AppIcon;
import de.derpeterson.app.ui.components.CosmosPageComponent;
import java.io.Serializable;
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
    UI ui = UI.getCurrent();
    if (ui == null) {
      throw new IllegalStateException("Notifications require a current UI");
    }
    // Meldungen gehören zum einzelnen Browserfenster, auch bei mehreren Tabs derselben Sitzung.
    // Kein globaler Helfer darf Hinweise anderer Benutzer schließen oder deren Texte verändern.
    NotificationHelper helper = ComponentUtil.getData(ui, NotificationHelper.class);
    if (helper == null) {
      helper = new NotificationHelper();
      ComponentUtil.setData(ui, NotificationHelper.class, helper);
    }
    return helper;
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
      Integer duration,
      NotificationType notificationType) {
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

    if (CosmosPageComponent.isDisplayed(UI.getCurrent())) {
      currentNotification.getElement().getThemeList().add("cosmos");
    }
    boolean referenceAccount = isReferencePortal(UI.getCurrent());
    if (referenceAccount) {
      currentNotification.getElement().getThemeList().add("cosmos-reference");
      currentNotification
          .getElement()
          .getThemeList()
          .add("cosmos-reference-" + notificationType.name().toLowerCase(Locale.ROOT));
    }
    currentNotification.setPosition(Notification.Position.TOP_CENTER);
    currentNotification.setDuration(duration);

    this.titleText = null;
    if (StringUtils.isNotEmpty(title)) {
      titleText = new Span(title);
      titleText.addClassNames("cosmos-toast-title", LumoUtility.FontWeight.SEMIBOLD);
      currentNotification.add(titleText);
    }

    HorizontalLayout contentLayout = new HorizontalLayout();
    contentLayout.setAlignItems(FlexComponent.Alignment.CENTER);
    contentLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
    contentLayout.setWidthFull();
    contentLayout.setSpacing(false);
    contentLayout.addClassNames(LumoUtility.Gap.LARGE);

    SvgIcon icon = null;
    if (notificationType == NotificationType.ERROR) {
      icon = AppIcon.ERROR.create();
      icon.addClassNames(LumoUtility.TextColor.ERROR);
      if (titleText != null) {
        titleText.addClassNames(LumoUtility.TextColor.ERROR);
      }
    } else if (notificationType == NotificationType.SUCCESS) {
      icon = AppIcon.CHECK_CIRCLE.create();
      icon.addClassNames(LumoUtility.TextColor.SUCCESS);
      if (titleText != null) {
        titleText.addClassNames(LumoUtility.TextColor.SUCCESS);
      }
    } else if (notificationType == NotificationType.WARNING) {
      icon = AppIcon.WARNING.create();
      icon.addClassNames(LumoUtility.TextColor.WARNING);
      if (titleText != null) {
        titleText.addClassNames(LumoUtility.TextColor.WARNING);
      }
    }

    if (icon != null) {
      icon.addClassName("cosmos-notification-icon");
    }

    if (referenceAccount) {
      AppIcon symbol =
          switch (notificationType) {
            case ERROR -> AppIcon.ERROR;
            case SUCCESS -> AppIcon.CHECK_CIRCLE;
            case WARNING -> AppIcon.WARNING;
            case NORMAL -> AppIcon.INFO_CIRCLE;
          };
      SvgIcon referenceIcon = symbol.create();
      referenceIcon.addClassName("cosmos-notification-icon");
      contentLayout.add(referenceIcon);
    } else if (icon != null) {
      contentLayout.add(icon);
    }

    this.textLayout = new VerticalLayout();
    textLayout.addClassNames(LumoUtility.Padding.SMALL);
    textLayout.setSpacing(false);
    textLayout.setWidth(null);

    createTextLayoutContent(message);

    contentLayout.add(textLayout);

    currentNotification.add(contentLayout);
    closeButton = createNotificationCloseButton(currentNotification);
    closeButton.setAriaLabel(localizedCloseLabel());
    if (referenceAccount) {
      closeButton.setIcon(AppIcon.CLOSE_CIRCLE.create());
    }
    currentNotification.add(closeButton);
    currentNotification.open();
  }

  private static boolean isReferencePortal(Component component) {
    return component != null
        && (component.getElement().getClassList().contains("cosmos-reference-portal")
            || component.getChildren().anyMatch(NotificationHelper::isReferencePortal));
  }

  private static String localizedCloseLabel() {
    return new MessageProperties(new CustomI18NProvider()).getNotificationsClose();
  }

  private static Button createNotificationCloseButton(Notification notification) {
    Button closeButton =
        new Button(AppIcon.CLOSE_CIRCLE.create(), clickEvent -> notification.close());
    closeButton.addClassNames(LumoUtility.TextColor.PRIMARY);
    closeButton.addThemeVariants(LUMO_TERTIARY_INLINE);

    return closeButton;
  }

  private void createTextLayoutContent(String message) {
    textLayout.removeAll();
    if (titleText != null) {
      textLayout.add(titleText);
    }
    messageText = null;
    if (StringUtils.isNotBlank(message)) {
      messageText = new Html(HtmlHelper.ensureParagraphTags(message));
      messageText.addClassNames("cosmos-notification-message", LumoUtility.Margin.NONE);
      messageText.getStyle().set("max-width", "520px");
      textLayout.add(messageText);
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
    if (messageSupplier != null) {
      createTextLayoutContent(messageSupplier.get());
    }
  }
}
