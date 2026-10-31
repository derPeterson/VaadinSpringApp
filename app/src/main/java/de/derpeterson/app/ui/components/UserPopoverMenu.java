package de.derpeterson.app.ui.components;

import static com.vaadin.flow.component.button.ButtonVariant.LUMO_TERTIARY_INLINE;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.popover.PopoverPosition;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.IconSize;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.UserAvatarService;
import de.derpeterson.app.service.UserService;
import de.derpeterson.app.service.UserService.StatusUpdate;
import de.derpeterson.app.views.AccountView;
import io.micrometer.common.util.StringUtils;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import org.springframework.dao.OptimisticLockingFailureException;

public class UserPopoverMenu {

  public record Actions(Runnable manageApplicationAction, Runnable homeAction) {}

  private final MessageProperties messageProperties;
  private final SecurityService securityService;
  private final UserService userService;
  private final UserEntity currentUser;
  private Span profileName;
  private Span emailText;
  private Span menuPresence;
  private final Span languageHeading = new Span();
  private final Map<UserStatus, Button> statusButtons = new EnumMap<>(UserStatus.class);
  private Span presenceHeading;
  private final Actions actions;
  private final Consumer<UserStatus> statusIconConsumer;
  private final Popover userPopover;

  private Span accountStateText = null;
  private Button manageAccountButton = null;
  private Button manageApplicationButton = null;
  private Button logoutButton = null;
  private Button homeButton = null;
  private Button germanLanguageButton;
  private Button englishLanguageButton;
  private OverlayUserIcon overlayIcon = null;

  private UserStatus currentStatus;

  public UserPopoverMenu(
      MessageProperties messageProperties,
      SecurityService securityService,
      UserService userService,
      UserEntity currentUser,
      Button targetButton,
      Actions actions,
      Consumer<UserStatus> statusIconConsumer) {
    this.messageProperties = messageProperties;
    this.securityService = securityService;
    this.userService = userService;
    this.currentUser = Objects.requireNonNull(currentUser);
    this.actions = actions;
    this.statusIconConsumer = statusIconConsumer;
    this.currentStatus = currentUser.getStatus();
    this.userPopover = createUserPopover(targetButton);
    refreshTexts();
    refreshLanguageButtons();
  }

  public Popover getPopover() {
    return userPopover;
  }

  public void addThemeName(String themeName) {
    userPopover.getElement().getThemeList().add(themeName);
  }

  private Popover createUserPopover(Button targetButton) {
    Popover popover = new Popover();
    popover.setTarget(targetButton);
    popover.addClassName("portal-user-menu-popover");
    popover.setPosition(PopoverPosition.BOTTOM_END);
    popover.setModal(true);
    popover.setCloseOnEsc(true);
    popover.setCloseOnOutsideClick(true);

    VerticalLayout body = new VerticalLayout();
    body.addClassNames("user-popover-body", "portal-user-menu-body");
    body.setWidthFull();
    body.setSpacing(false);
    body.setPadding(false);
    body.add(createIdentity());
    addManageAccountButton(body);
    addManageApplicationButton(body);
    body.add(createStatusOptions(), createLanguageChoice(), createFooter());

    VerticalLayout outer = new VerticalLayout(body);
    outer.addClassNames(
        "user-popover-content", "cosmos-theme", "cosmos-user-menu", "cosmos-reference-overlay");
    outer.setWidth(null);
    outer.setSpacing(false);
    outer.setPadding(false);
    popover.add(outer);
    return popover;
  }

  private Div createIdentity() {
    overlayIcon =
        new OverlayUserIcon(messageProperties, currentUser, currentStatus, IconSize.PIXEL_96, true);
    overlayIcon.addClassNames("user-popover-avatar", "portal-menu-avatar");
    profileName = new Span();
    profileName.addClassName("portal-menu-name");
    refreshProfileName();
    emailText = new Span(currentUser.getEmail());
    emailText.addClassName("portal-menu-email");
    accountStateText = new Span();
    accountStateText.addClassName("portal-menu-account-state");
    menuPresence = new Span();
    menuPresence.addClassName("portal-menu-presence");
    Div badges = new Div(accountStateText, menuPresence);
    badges.addClassName("portal-menu-badges");
    Div identity = new Div(profileName, emailText, badges);
    identity.addClassName("portal-menu-identity-copy");
    Div header = new Div(overlayIcon, identity);
    header.addClassName("portal-menu-identity");
    return header;
  }

  private Div createStatusOptions() {
    presenceHeading = new Span();
    presenceHeading.addClassName("portal-menu-section");
    Div choices = new Div(presenceHeading);
    choices.addClassName("portal-menu-status-options");
    for (UserStatus status : UserStatus.values()) {
      Button option = new Button();
      option.addClickListener(event -> handleUserStatusChange(status));
      option.addClassName("portal-status-option");
      option.getStyle().set("--portal-option-status-color", status.getColor());
      option.getElement().setAttribute("data-status", status.name().toLowerCase(Locale.ROOT));
      statusButtons.put(status, option);
      choices.add(option);
    }
    return choices;
  }

  private Div createLanguageChoice() {
    germanLanguageButton = createLanguageButton("DE", "Deutsch", Locale.GERMAN);
    englishLanguageButton = createLanguageButton("EN", "English", Locale.ENGLISH);
    HorizontalLayout languageButtons =
        new HorizontalLayout(germanLanguageButton, englishLanguageButton);
    languageButtons.addClassName("portal-language-switch");
    languageHeading.addClassName("portal-menu-section");
    Div languageLabel = new Div(AppIcon.LANGUAGES.create(), languageHeading);
    languageLabel.addClassName("portal-menu-language-label");
    Div language = new Div(languageLabel, languageButtons);
    language.addClassName("portal-menu-language-row");
    return language;
  }

  private Div createFooter() {
    Div footer = new Div();
    footer.addClassName("portal-menu-footer");
    if (actions.homeAction() != null) {
      homeButton =
          new Button(messageProperties.getBaseHomeButton(), event -> actions.homeAction().run());
      homeButton.setPrefixComponent(AppIcon.HOME.create());
      homeButton.addThemeVariants(LUMO_TERTIARY_INLINE);
      homeButton.addClassName("portal-menu-action");
      footer.add(homeButton);
    }
    logoutButton =
        new Button(messageProperties.getBaseLogoutButton(), event -> securityService.logout());
    logoutButton.setPrefixComponent(AppIcon.SIGN_OUT.create());
    logoutButton.addThemeVariants(LUMO_TERTIARY_INLINE);
    logoutButton.addClassNames("portal-menu-action", "portal-menu-logout");
    footer.add(logoutButton);
    return footer;
  }

  private void refreshStatusButtons() {
    for (Map.Entry<UserStatus, Button> entry : statusButtons.entrySet()) {
      UserStatus status = entry.getKey();
      Button button = entry.getValue();
      Span label = new Span(messageProperties.getUserStatusLabel(status));
      label.addClassName("portal-status-label");
      Span description = new Span(messageProperties.getUserStatusDescription(status));
      description.addClassName("portal-status-description");
      Div copy = new Div(label, description);
      copy.addClassName("portal-status-copy");
      Div content = new Div(status.getSelectionComponent(), copy);
      content.addClassName("portal-status-option-content");
      button.setPrefixComponent(content);
      button.setAriaLabel(label.getText());
      button.getElement().setAttribute("aria-pressed", Boolean.toString(status == currentStatus));
    }
  }

  public void refreshForLanguageChange() {
    refreshTexts();
    refreshLanguageButtons();
  }

  public void applyExternalStatus(UserStatus newStatus) {
    if (newStatus == null) {
      return;
    }

    currentStatus = newStatus;

    currentUser.setStatus(newStatus);

    updateStatusIcons(newStatus);
    refreshStatusButtons();
  }

  public void configureAvatars(UserAvatarService avatars) {
    if (overlayIcon != null) {
      overlayIcon.configureAvatar(avatars, null);
    }
  }

  public void refreshProfileName() {
    if (profileName != null) {
      profileName.setText(buildFullName());
      profileName.setVisible(hasUserName());
    }
    if (overlayIcon != null) {
      overlayIcon.setIdentity(currentUser);
    }
  }

  private void addManageAccountButton(VerticalLayout contentLayout) {
    manageAccountButton = new Button(messageProperties.getBaseManageAccountButton());
    manageAccountButton.setPrefixComponent(AppIcon.COG.create());
    manageAccountButton.addClickListener(
        event -> {
          userPopover.close();
          UI.getCurrent().navigate(AccountView.class);
        });

    manageAccountButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    manageAccountButton.addClassName("portal-menu-primary");
    contentLayout.add(manageAccountButton);
  }

  private void addManageApplicationButton(VerticalLayout contentLayout) {
    if (actions.manageApplicationAction() == null) {
      return;
    }

    manageApplicationButton =
        new Button(
            messageProperties.getBaseManageApplicationButton(),
            clickEvent -> actions.manageApplicationAction().run());
    manageApplicationButton.setPrefixComponent(AppIcon.SHIELD_COG.create());
    manageApplicationButton.addClassName("portal-menu-action");
    contentLayout.add(manageApplicationButton);
  }

  /** Gleiche Auswahl und Speicherlogik für Benutzermenü und Kontobild. */
  public Select<UserStatus> createStatusSelect() {
    Select<UserStatus> select = new Select<UserStatus>();
    select.getElement().getThemeList().add("cosmos-status");
    select.setItems(UserStatus.values());
    select.setItemLabelGenerator(messageProperties::getUserStatusLabel);
    select.setRenderer(new ComponentRenderer<>(this::createUserStatusRenderer));
    select.setValue(currentStatus);
    select.addValueChangeListener(
        event -> {
          if (event.isFromClient()) {
            handleUserStatusChange(event.getValue());
          }
        });
    return select;
  }

  private Component createUserStatusRenderer(UserStatus userStatus) {
    HorizontalLayout contentComboBox = new HorizontalLayout();
    contentComboBox.addClassName("portal-status-option-content");
    contentComboBox.getStyle().set("--portal-option-status-color", userStatus.getColor());
    contentComboBox.setWidthFull();

    addStatusVisualComponent(contentComboBox, userStatus);
    contentComboBox.add(createStatusTextLayout(userStatus));

    return contentComboBox;
  }

  private void addStatusVisualComponent(HorizontalLayout parent, UserStatus userStatus) {
    Object statusComponent = userStatus.getSelectionComponent();

    if (statusComponent instanceof SvgIcon statusIcon) {
      parent.add(createCenteredWrapper(statusIcon));
      return;
    }

    if (statusComponent instanceof Div statusDiv) {
      parent.add(createCenteredWrapper(statusDiv));
    }
  }

  private VerticalLayout createCenteredWrapper(Component component) {
    VerticalLayout wrapper = new VerticalLayout();
    wrapper.setPadding(false);
    wrapper.setSpacing(false);
    wrapper.setWidth(null);
    wrapper.setSizeUndefined();
    wrapper.setAlignItems(FlexComponent.Alignment.CENTER);
    wrapper.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
    wrapper.add(component);
    return wrapper;
  }

  private VerticalLayout createStatusTextLayout(UserStatus userStatus) {
    VerticalLayout textLayout = new VerticalLayout();
    textLayout.setPadding(false);
    textLayout.setSpacing(false);
    textLayout.setWidthFull();
    textLayout.addClassName("portal-status-copy");

    Span userStatusText = new Span(messageProperties.getUserStatusLabel(userStatus));
    userStatusText.addClassName("user-popover-status-label");
    Span userStatusSecondaryText = new Span(messageProperties.getUserStatusDescription(userStatus));
    userStatusSecondaryText.addClassNames(
        LumoUtility.FontSize.XXSMALL, LumoUtility.TextColor.SECONDARY);

    textLayout.add(userStatusText);
    textLayout.add(new Hr());
    textLayout.add(userStatusSecondaryText);

    return textLayout;
  }

  private void handleUserStatusChange(UserStatus userStatus) {
    if (userStatus == null) {
      return;
    }
    try {
      StatusUpdate result = userService.updateUserStatus(currentUser, userStatus, true);
      currentUser.setStatusManuallySet(result.manuallySet());
      applyExternalStatus(result.status());
    } catch (RuntimeException exception) {
      applyExternalStatus(currentStatus);
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getBaseFailedTitle,
              exception instanceof OptimisticLockingFailureException
                  ? messageProperties::getBaseUserUpdateConflict
                  : messageProperties::getBaseUserStatusFailed,
              NotificationHelper.NotificationType.ERROR);
    }
  }

  private Button createLanguageButton(String label, String accessibleName, Locale targetLocale) {
    Button button = new Button(label, event -> switchLanguage(targetLocale));
    button.setId("portal-language-" + targetLocale.getLanguage());
    button.setAriaLabel(accessibleName);
    return button;
  }

  private void switchLanguage(Locale locale) {
    userService.updateUserLocale(currentUser.getEmail(), locale);

    VaadinSession.getCurrent().setLocale(locale);
    LanguageChangeEvent.fire(UI.getCurrent(), locale);
  }

  private void refreshTexts() {
    languageHeading.setText(messageProperties.getPortalLanguageHeading());
    presenceHeading.setText(messageProperties.getPortalPresenceHeading());
    refreshStatusButtons();
    menuPresence.setText(messageProperties.getUserStatusLabel(currentStatus));
    menuPresence.getStyle().set("--account-presence-color", currentStatus.getColor());
    accountStateText.setText(
        currentUser.isEnabled()
            ? messageProperties.getAccountSettingsEnabled()
            : messageProperties.getAccountSettingsDisabled());
    manageAccountButton.setText(messageProperties.getBaseManageAccountButton());
    if (manageApplicationButton != null) {
      manageApplicationButton.setText(messageProperties.getBaseManageApplicationButton());
    }
    if (homeButton != null) {
      homeButton.setText(messageProperties.getBaseHomeButton());
    }
    logoutButton.setText(messageProperties.getBaseLogoutButton());
  }

  private void refreshLanguageButtons() {
    Locale selected = VaadinSession.getCurrent().getLocale();
    for (Button button : new Button[] {germanLanguageButton, englishLanguageButton}) {
      if (button != null) {
        boolean active =
            (button == germanLanguageButton ? Locale.GERMAN : Locale.ENGLISH).equals(selected);
        button.setClassName("portal-language-active", active);
        button.getElement().setAttribute("aria-pressed", Boolean.toString(active));
      }
    }
  }

  private void updateStatusIcons(UserStatus userStatus) {
    if (menuPresence != null) {
      menuPresence.setText(messageProperties.getUserStatusLabel(userStatus));
      menuPresence.getStyle().set("--account-presence-color", userStatus.getColor());
    }
    if (userStatus == null) {
      return;
    }

    if (overlayIcon != null) {
      overlayIcon.setUserStatus(userStatus);
    }

    if (statusIconConsumer != null) {
      statusIconConsumer.accept(userStatus);
    }
  }

  private boolean hasUserName() {
    return (StringUtils.isNotEmpty(currentUser.getFirstName())
        || StringUtils.isNotEmpty(currentUser.getLastName()));
  }

  private String buildFullName() {
    String firstName = currentUser.getFirstName() != null ? currentUser.getFirstName() : "";
    String lastName = currentUser.getLastName() != null ? currentUser.getLastName() : "";
    return (firstName + " " + lastName).trim();
  }
}
