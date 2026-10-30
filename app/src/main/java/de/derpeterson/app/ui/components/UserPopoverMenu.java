package de.derpeterson.app.ui.components;

import static com.vaadin.flow.component.button.ButtonVariant.LUMO_TERTIARY_INLINE;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.popover.PopoverPosition;
import com.vaadin.flow.component.popover.PopoverVariant;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.helper.ui.ComponentPrefixHelper;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.helper.ui.StyleHelper;
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
import java.util.function.Consumer;
import org.springframework.dao.OptimisticLockingFailureException;

public class UserPopoverMenu {

  public record Actions(
      Runnable manageApplicationAction,
      Runnable homeAction,
      Runnable registrationAction,
      Runnable loginAction) {}

  private final MessageProperties messageProperties;
  private final SecurityService securityService;
  private final UserService userService;
  private final UserEntity currentUser;
  private Span profileName;
  private Span emailText;
  private Span menuPresence;
  private final Span languageHeading = new Span();
  private boolean referenceStyle;
  private final Map<UserStatus, Button> referenceStatusButtons = new EnumMap<>(UserStatus.class);
  private Span referencePresenceHeading;
  private final Actions actions;
  private final Consumer<UserStatus> statusIconConsumer;
  private final Popover userPopover;

  private Span accountStateText = null;
  private Button manageAccountButton = null;
  private Button manageApplicationButton = null;
  private Button logoutButton = null;
  private Button registrationButton = null;
  private Button loginButton = null;
  private Button homeButton = null;
  private Image deFlagIcon = null;
  private Image enFlagIcon = null;
  private OverlayUserIcon overlayIcon = null;
  private Select<UserStatus> userStatusSelect = null;

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
    this.currentUser = currentUser;
    this.actions = actions;
    this.statusIconConsumer = statusIconConsumer;
    this.currentStatus =
        currentUser != null ? currentUser.getStatus() : UserStatus.getDefaultStatus();
    this.userPopover = createUserPopover(targetButton);
  }

  public Popover getPopover() {
    return userPopover;
  }

  /** Markiert auch das ausgelagerte Select-Overlay, damit View-Styles gezielt bleiben. */
  public void addThemeName(String themeName) {
    userPopover.getElement().getThemeList().add(themeName);
    userStatusSelect.getElement().getThemeList().add(themeName);
    if ("cosmos".equals(themeName)) {
      // Native Platzierung außerhalb des Felds; bei wenig Platz darf Vaadin nach oben ausweichen.
      userStatusSelect.setNoVerticalOverlap(true);
      userPopover
          .getChildren()
          .forEach(
              content -> {
                content.getElement().getClassList().add("cosmos-theme");
                content.getElement().getClassList().add("cosmos-user-menu");
              });
    }
    if ("cosmos-reference".equals(themeName)) {
      enableReferenceStyle();
    }
  }

  private void enableReferenceStyle() {
    if (referenceStyle || !hasCurrentUser()) {
      return;
    }
    referenceStyle = true;
    userPopover.addClassName("portal-user-menu-popover");
    userPopover.setPosition(PopoverPosition.BOTTOM_END);
    userPopover.removeThemeVariants(PopoverVariant.ARROW);
    VerticalLayout outer = (VerticalLayout) userPopover.getChildren().findFirst().orElseThrow();
    outer.addClassName("cosmos-reference-overlay");
    outer.setSpacing(false);
    outer
        .getChildren()
        .filter(c -> c.getElement().getClassList().contains("user-popover-header"))
        .forEach(c -> c.setVisible(false));
    VerticalLayout body =
        (VerticalLayout)
            outer
                .getChildren()
                .filter(c -> c.getElement().getClassList().contains("user-popover-body"))
                .findFirst()
                .orElseThrow();
    body.addClassName("portal-user-menu-body");
    body.setSpacing(false);
    overlayIcon.addClassName("portal-menu-avatar");
    if (profileName != null) {
      profileName.addClassName("portal-menu-name");
    }
    emailText.addClassName("portal-menu-email");
    accountStateText.addClassName("portal-menu-account-state");
    menuPresence = new Span();
    menuPresence.addClassName("portal-menu-presence");
    body.remove(accountStateText);
    Div badges = new Div(accountStateText, menuPresence);
    badges.addClassName("portal-menu-badges");
    body.addComponentAtIndex(3, badges);
    manageAccountButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
    manageAccountButton.setPrefixComponent(AppIcon.COG.create());
    if (manageApplicationButton != null) {
      manageApplicationButton.setPrefixComponent(AppIcon.SHIELD_COG.create());
    }
    if (homeButton != null) {
      homeButton.setPrefixComponent(AppIcon.HOME.create());
    }
    logoutButton.setPrefixComponent(AppIcon.SIGN_OUT.create());
    logoutButton.addClassName("portal-menu-logout");
    for (Button item :
        new Button[] {manageAccountButton, manageApplicationButton, homeButton, logoutButton}) {
      if (item != null) {
        item.addThemeVariants(LUMO_TERTIARY_INLINE);
        item.addClassName("portal-menu-action");
      }
    }
    userStatusSelect.setLabel(messageProperties.getPortalPresenceHeading());
    languageHeading.setVisible(true);
    for (Image flag : new Image[] {deFlagIcon, enFlagIcon}) {
      if (flag != null && flag.getParent().orElse(null) instanceof Button button) {
        boolean german = flag == deFlagIcon;
        button.setId(german ? "portal-language-de" : "portal-language-en");
        button.setText(german ? "DE" : "EN");
        button.setAriaLabel(german ? "Deutsch" : "English");
        button.removeThemeVariants(LUMO_TERTIARY_INLINE);
        flag.setVisible(false);
      }
    }
    arrangeReferenceMenu(body);
    refreshTexts();
    refreshLanguageFlags();
  }

  private void arrangeReferenceMenu(VerticalLayout body) {
    HorizontalLayout languageButtons =
        (HorizontalLayout) deFlagIcon.getParent().orElseThrow().getParent().orElseThrow();
    body.removeAll();
    Div identity = new Div();
    identity.addClassName("portal-menu-identity-copy");
    if (profileName != null) {
      identity.add(profileName);
    }
    Div badges = new Div(accountStateText, menuPresence);
    badges.addClassName("portal-menu-badges");
    identity.add(emailText, badges);
    Div header = new Div(overlayIcon, identity);
    header.addClassName("portal-menu-identity");
    body.add(header, manageAccountButton);
    manageAccountButton.removeClassName("portal-menu-action");
    manageAccountButton.removeThemeVariants(LUMO_TERTIARY_INLINE);
    manageAccountButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    manageAccountButton.addClassName("portal-menu-primary");
    manageAccountButton.setWidth(null);
    if (manageApplicationButton != null) {
      manageApplicationButton.removeThemeVariants(LUMO_TERTIARY_INLINE);
      manageApplicationButton.setWidth(null);
      body.add(manageApplicationButton);
    }
    referencePresenceHeading = new Span();
    referencePresenceHeading.addClassName("portal-menu-section");
    Div choices = new Div(referencePresenceHeading);
    choices.addClassName("portal-menu-status-options");
    for (UserStatus status : UserStatus.values()) {
      Button option = new Button();
      option.addClickListener(event -> handleUserStatusChange(status, true));
      option.addClassName("portal-status-option");
      option.getStyle().set("--portal-option-status-color", status.getColor());
      option.getElement().setAttribute("data-status", status.name().toLowerCase(Locale.ROOT));
      referenceStatusButtons.put(status, option);
      choices.add(option);
    }
    Div languageLabel = new Div(AppIcon.LANGUAGES.create(), languageHeading);
    languageLabel.addClassName("portal-menu-language-label");
    Div language = new Div(languageLabel, languageButtons);
    language.addClassName("portal-menu-language-row");
    Div footer = new Div();
    footer.addClassName("portal-menu-footer");
    if (homeButton != null) {
      homeButton.setWidth(null);
      footer.add(homeButton);
    }
    logoutButton.setWidth(null);
    footer.add(logoutButton);
    body.add(choices, language, footer);
  }

  private void refreshReferenceStatusButtons() {
    for (Map.Entry<UserStatus, Button> entry : referenceStatusButtons.entrySet()) {
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
    refreshUserStatusSelect();
    refreshLanguageFlags();
  }

  public void applyExternalStatus(UserStatus newStatus) {
    if (newStatus == null) {
      return;
    }

    currentStatus = newStatus;

    if (hasCurrentUser()) {
      currentUser.setStatus(newStatus);
    }

    updateStatusIcons(newStatus);
    refreshReferenceStatusButtons();

    if (userStatusSelect != null) {
      userStatusSelect.setValue(newStatus);
      updateStatusSelectPrefix(newStatus);
    }
  }

  private Popover createUserPopover(Button targetButton) {
    Popover popover = createConfiguredPopover(targetButton);

    VerticalLayout popoverLayout = createPopoverLayout();
    HorizontalLayout headerLayout = createHeaderLayout(popover);
    VerticalLayout contentLayout = createContentLayout();

    addUserIcon(contentLayout);
    addUserInfoSection(contentLayout);
    addManageAccountButton(contentLayout);
    addManageApplicationButton(contentLayout);
    addUserStatusSelect(contentLayout);
    addLanguageButtons(contentLayout);
    addHomeButton(contentLayout);
    addAuthButtons(contentLayout);

    popoverLayout.add(headerLayout, contentLayout);
    popover.add(popoverLayout);

    return popover;
  }

  private Popover createConfiguredPopover(Button targetButton) {
    Popover popover = new Popover();
    popover.setTarget(targetButton);
    popover.addThemeVariants(PopoverVariant.ARROW);
    popover.setPosition(PopoverPosition.BOTTOM);
    popover.setModal(true);
    popover.setCloseOnEsc(true);
    popover.setCloseOnOutsideClick(true);
    return popover;
  }

  private VerticalLayout createPopoverLayout() {
    VerticalLayout popoverLayout = new VerticalLayout();
    // Keine inline vorgegebene Breite: das jeweilige Theme bestimmt die Menübreite.
    popoverLayout.setWidth(null);
    popoverLayout.setSpacing(false);
    popoverLayout.setPadding(false);
    popoverLayout.addClassNames(LumoUtility.Padding.SMALL);
    popoverLayout.addClassName("user-popover-content");
    return popoverLayout;
  }

  private HorizontalLayout createHeaderLayout(Popover popover) {
    HorizontalLayout headerLayout = new HorizontalLayout();
    headerLayout.addClassName("user-popover-header");
    headerLayout.setWidthFull();
    headerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

    Button closeButton = new Button(AppIcon.CLOSE_CIRCLE.create(), clickEvent -> popover.close());
    closeButton.addClassNames(LumoUtility.TextColor.PRIMARY);
    closeButton.addThemeVariants(LUMO_TERTIARY_INLINE);

    headerLayout.add(closeButton);
    return headerLayout;
  }

  private VerticalLayout createContentLayout() {
    VerticalLayout contentLayout = new VerticalLayout();
    contentLayout.addClassName("user-popover-body");
    contentLayout.setWidthFull();
    contentLayout.setSpacing(false);
    contentLayout.setPadding(false);
    contentLayout.setAlignItems(FlexComponent.Alignment.CENTER);
    contentLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
    contentLayout.addClassNames(
        LumoUtility.Padding.Top.SMALL, LumoUtility.Padding.Bottom.SMALL, LumoUtility.Gap.SMALL);
    return contentLayout;
  }

  private void addUserIcon(VerticalLayout contentLayout) {
    overlayIcon =
        new OverlayUserIcon(messageProperties, currentUser, currentStatus, IconSize.PIXEL_96, true);
    overlayIcon.addClassName("user-popover-avatar");
    contentLayout.add(overlayIcon);
  }

  private void addUserInfoSection(VerticalLayout contentLayout) {
    if (hasCurrentUser()) {
      addAccountInfo(contentLayout);
    }
  }

  private void addAccountInfo(VerticalLayout contentLayout) {
    accountStateText =
        new Span(
            currentUser.isEnabled()
                ? messageProperties.getAccountSettingsEnabled()
                : messageProperties.getAccountSettingsDisabled());
    accountStateText.addClassNames(LumoUtility.TextColor.PRIMARY, LumoUtility.FontWeight.SEMIBOLD);
    contentLayout.add(accountStateText);

    if (hasUserName()) {
      profileName = new Span(buildFullName());
      profileName.addClassNames(LumoUtility.FontWeight.BOLD);
      contentLayout.add(profileName);
    }

    emailText = new Span(currentUser.getEmail());
    emailText.addClassNames(LumoUtility.TextColor.SECONDARY);
    contentLayout.add(emailText);
  }

  public void configureAvatars(UserAvatarService avatars) {
    if (overlayIcon != null) {
      overlayIcon.configureAvatar(avatars, null);
    }
  }

  public void refreshProfileName() {
    if (profileName != null) {
      profileName.setText(buildFullName());
    }
    if (overlayIcon != null) {
      overlayIcon.setIdentity(currentUser);
    }
  }

  private void addManageAccountButton(VerticalLayout contentLayout) {
    manageAccountButton = new Button(messageProperties.getBaseManageAccountButton());
    manageAccountButton.setPrefixComponent(AppIcon.COG.create());
    manageAccountButton.setEnabled(hasCurrentUser());
    manageAccountButton.addClickListener(
        event -> {
          if (hasCurrentUser()) {
            userPopover.close();
            UI.getCurrent().navigate(AccountView.class);
          }
        });

    if (hasCurrentUser()) {
      manageAccountButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    }

    manageAccountButton.setWidthFull();
    contentLayout.add(manageAccountButton);
  }

  private void addManageApplicationButton(VerticalLayout contentLayout) {
    if (!hasCurrentUser() || actions.manageApplicationAction() == null) {
      return;
    }

    manageApplicationButton =
        new Button(
            messageProperties.getBaseManageApplicationButton(),
            clickEvent -> actions.manageApplicationAction().run());
    manageApplicationButton.setPrefixComponent(AppIcon.SHIELD_COG.create());
    manageApplicationButton.setWidthFull();
    contentLayout.add(manageApplicationButton);
  }

  private void addUserStatusSelect(VerticalLayout contentLayout) {
    userStatusSelect = createStatusSelect();
    userStatusSelect.setWidthFull();
    updateStatusSelectPrefix(currentStatus);
    contentLayout.add(userStatusSelect);
  }

  /** Gleiche Auswahl und Speicherlogik für Benutzermenü und Kontobild. */
  public Select<UserStatus> createStatusSelect() {
    Select<UserStatus> select = new Select<UserStatus>();
    select.getElement().getThemeList().add("cosmos-status");
    select.setEnabled(hasCurrentUser());
    select.setItems(UserStatus.values());
    select.setItemLabelGenerator(messageProperties::getUserStatusLabel);
    select.setRenderer(new ComponentRenderer<>(this::createUserStatusRenderer));
    select.setValue(currentStatus);
    select.addValueChangeListener(
        event -> {
          if (event.isFromClient()) {
            handleUserStatusChange(event.getValue(), true);
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

  private void handleUserStatusChange(UserStatus userStatus, boolean isFromClient) {
    if (userStatus == null) {
      return;
    }
    if (isFromClient && hasCurrentUser()) {
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
    } else {
      currentStatus = userStatus;
      updateStatusSelectPrefix(userStatus);
      updateStatusIcons(userStatus);
    }
  }

  private void addLanguageButtons(VerticalLayout contentLayout) {
    Locale locale = VaadinSession.getCurrent().getLocale();

    Button deFlagButton =
        createLanguageButton(
            "custom-theme/flag_de.png", "German Flag SvgIcon", Locale.GERMAN, locale);
    deFlagIcon =
        (Image) deFlagButton.getChildren().filter(Image.class::isInstance).findFirst().orElse(null);

    Button enFlagButton =
        createLanguageButton(
            "custom-theme/flag_en.png", "English Flag SvgIcon", Locale.ENGLISH, locale);
    enFlagIcon =
        (Image) enFlagButton.getChildren().filter(Image.class::isInstance).findFirst().orElse(null);

    HorizontalLayout flagLayout = new HorizontalLayout(deFlagButton, enFlagButton);
    flagLayout.addClassName("portal-language-switch");
    languageHeading.addClassName("portal-menu-section");
    languageHeading.setVisible(false);
    contentLayout.add(languageHeading, flagLayout);
  }

  private Button createLanguageButton(
      String imagePath, String altText, Locale targetLocale, Locale currentLocale) {
    Image flagIcon = new Image(imagePath, altText);
    flagIcon.setWidth("32px");
    flagIcon.setHeight("32px");
    updateFlagStyle(flagIcon, currentLocale.equals(targetLocale));

    Button flagButton = new Button(flagIcon, clickEvent -> switchLanguage(targetLocale));
    flagButton.addThemeVariants(LUMO_TERTIARY_INLINE);
    return flagButton;
  }

  private void switchLanguage(Locale locale) {
    if (hasCurrentUser()) {
      userService.updateUserLocale(currentUser.getEmail(), locale);
    }

    VaadinSession.getCurrent().setLocale(locale);
    LanguageChangeEvent.fire(UI.getCurrent(), locale);
  }

  private void addHomeButton(VerticalLayout contentLayout) {
    if (actions.homeAction() == null) {
      return;
    }

    homeButton =
        new Button(messageProperties.getBaseHomeButton(), clickEvent -> actions.homeAction().run());
    homeButton.setPrefixComponent(AppIcon.HOME.create());
    homeButton.setWidthFull();
    contentLayout.add(homeButton);
  }

  private void addAuthButtons(VerticalLayout contentLayout) {
    if (hasCurrentUser()) {
      addLogoutButton(contentLayout);
      return;
    }

    addRegistrationButton(contentLayout);
    addLoginButton(contentLayout);
  }

  private void addLogoutButton(VerticalLayout contentLayout) {
    logoutButton =
        new Button(messageProperties.getBaseLogoutButton(), clickEvent -> securityService.logout());
    logoutButton.setPrefixComponent(AppIcon.SIGN_OUT.create());
    logoutButton.setWidthFull();
    contentLayout.add(logoutButton);
  }

  private void addRegistrationButton(VerticalLayout contentLayout) {
    if (actions.registrationAction() == null) {
      return;
    }

    registrationButton =
        new Button(
            messageProperties.getBaseRegistrationButton(),
            clickEvent -> actions.registrationAction().run());
    registrationButton.setPrefixComponent(AppIcon.EDIT.create());
    registrationButton.setWidthFull();
    contentLayout.add(registrationButton);
  }

  private void addLoginButton(VerticalLayout contentLayout) {
    if (actions.loginAction() == null) {
      return;
    }

    loginButton =
        new Button(
            messageProperties.getBaseLoginButton(), clickEvent -> actions.loginAction().run());
    loginButton.setPrefixComponent(AppIcon.SIGN_IN.create());
    loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    loginButton.setWidthFull();
    contentLayout.add(loginButton);
  }

  private void refreshTexts() {
    if (referenceStyle) {
      languageHeading.setText(messageProperties.getPortalLanguageHeading());
      userStatusSelect.setLabel(messageProperties.getPortalPresenceHeading());
      referencePresenceHeading.setText(messageProperties.getPortalPresenceHeading());
      refreshReferenceStatusButtons();
      menuPresence.setText(messageProperties.getUserStatusLabel(currentStatus));
      menuPresence.getStyle().set("--account-presence-color", currentStatus.getColor());
    }
    if (accountStateText != null) {
      accountStateText.setText(
          currentUser.isEnabled()
              ? messageProperties.getAccountSettingsEnabled()
              : messageProperties.getAccountSettingsDisabled());
    }
    if (manageAccountButton != null) {
      manageAccountButton.setText(messageProperties.getBaseManageAccountButton());
    }
    if (manageApplicationButton != null) {
      manageApplicationButton.setText(messageProperties.getBaseManageApplicationButton());
    }
    if (homeButton != null) {
      homeButton.setText(messageProperties.getBaseHomeButton());
    }
    if (logoutButton != null) {
      logoutButton.setText(messageProperties.getBaseLogoutButton());
    }
    if (registrationButton != null) {
      registrationButton.setText(messageProperties.getBaseRegistrationButton());
    }
    if (loginButton != null) {
      loginButton.setText(messageProperties.getBaseLoginButton());
    }
  }

  private void refreshUserStatusSelect() {
    if (userStatusSelect == null) {
      return;
    }

    userStatusSelect.setItemLabelGenerator(messageProperties::getUserStatusLabel);
    userStatusSelect.setItems(UserStatus.values());
    userStatusSelect.getDataProvider().refreshAll();
    userStatusSelect.setValue(currentStatus);
    updateStatusSelectPrefix(currentStatus);
  }

  private void refreshLanguageFlags() {
    if (referenceStyle) {
      Locale selected = VaadinSession.getCurrent().getLocale();
      for (Image flag : new Image[] {deFlagIcon, enFlagIcon}) {
        if (flag != null && flag.getParent().orElse(null) instanceof Button button) {
          boolean active = (flag == deFlagIcon ? Locale.GERMAN : Locale.ENGLISH).equals(selected);
          button.setClassName("portal-language-active", active);
          button.getElement().setAttribute("aria-pressed", Boolean.toString(active));
        }
      }
    }
    Locale locale = VaadinSession.getCurrent().getLocale();

    if (deFlagIcon != null) {
      updateFlagStyle(deFlagIcon, locale.equals(Locale.GERMAN));
    }

    if (enFlagIcon != null) {
      updateFlagStyle(enFlagIcon, locale.equals(Locale.ENGLISH));
    }
  }

  private void updateFlagStyle(Image flagIcon, boolean active) {
    if (active) {
      StyleHelper.removeFilter(flagIcon);
    } else {
      StyleHelper.addFilterWithGrayscale100Percent(flagIcon);
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

  private void updateStatusSelectPrefix(UserStatus userStatus) {
    if (userStatus == null) {
      ComponentPrefixHelper.clearSlot(userStatusSelect, "prefix");
      return;
    }

    Object prefixComponent = userStatus.getOutlineComponent(IconSize.PIXEL_16, false);

    if (prefixComponent instanceof SvgIcon statusIcon) {
      ComponentPrefixHelper.setPrefixComponent(userStatusSelect, createPrefixLayout(statusIcon));
      return;
    }

    if (prefixComponent instanceof Div statusDiv) {
      ComponentPrefixHelper.setPrefixComponent(userStatusSelect, createPrefixLayout(statusDiv));
    }
  }

  private HorizontalLayout createPrefixLayout(Component component) {
    HorizontalLayout layout = new HorizontalLayout();
    layout.addClassNames(LumoUtility.Padding.Left.SMALL, LumoUtility.Padding.Right.SMALL);
    layout.add(component);
    return layout;
  }

  private boolean hasCurrentUser() {
    return currentUser != null;
  }

  private boolean hasUserName() {
    return hasCurrentUser()
        && (StringUtils.isNotEmpty(currentUser.getFirstName())
            || StringUtils.isNotEmpty(currentUser.getLastName()));
  }

  private String buildFullName() {
    String firstName = currentUser.getFirstName() != null ? currentUser.getFirstName() : "";
    String lastName = currentUser.getLastName() != null ? currentUser.getLastName() : "";
    return (firstName + " " + lastName).trim();
  }
}
