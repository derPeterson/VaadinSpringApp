package de.derpeterson.app.ui.components;

import static com.vaadin.flow.component.button.ButtonVariant.LUMO_TERTIARY_INLINE;
import static de.derpeterson.app.ui.components.CosmosCssClasses.REFERENCE_OVERLAY;
import static de.derpeterson.app.ui.components.CosmosCssClasses.USER_MENU_ACTION;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.details.Details;
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

  public record UserPopoverActions(Runnable manageApplicationAction, Runnable homeAction) {}

  private final MessageProperties messageProperties;
  private final SecurityService securityService;
  private final UserService userService;
  private final UserEntity currentUser;
  private Span profileNameSpan;
  private Badge menuPresenceBadge;
  private final Span languageHeadingSpan = new Span();
  private final Map<UserStatus, Button> statusButtons = new EnumMap<>(UserStatus.class);
  private Span presenceHeadingSpan;
  private DetailsToggleButton presenceToggle;
  private final UserPopoverActions userPopoverActions;
  private final Consumer<UserStatus> statusIconConsumer;
  private final Popover userPopover;

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
      UserPopoverActions userPopoverActions,
      Consumer<UserStatus> statusIconConsumer) {
    this.messageProperties = messageProperties;
    this.securityService = securityService;
    this.userService = userService;
    this.currentUser = Objects.requireNonNull(currentUser);
    this.userPopoverActions = userPopoverActions;
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
    Popover userMenuPopover = new Popover();
    userMenuPopover.setTarget(targetButton);
    userMenuPopover.addClassName("portal-user-menu-popover");
    userMenuPopover.setPosition(PopoverPosition.BOTTOM_END);
    userMenuPopover.setModal(true);
    userMenuPopover.setCloseOnEsc(true);
    userMenuPopover.setCloseOnOutsideClick(true);

    VerticalLayout menuBodyLayout = new VerticalLayout();
    menuBodyLayout.addClassNames("user-popover-body", "portal-user-menu-body");
    menuBodyLayout.setWidthFull();
    menuBodyLayout.setSpacing(false);
    menuBodyLayout.setPadding(false);
    menuBodyLayout.add(createIdentity());
    addManageAccountButton(menuBodyLayout);
    addManageApplicationButton(menuBodyLayout);
    menuBodyLayout.add(createStatusOptions(), createLanguageChoice(), createFooter());

    VerticalLayout menuContentLayout = new VerticalLayout(menuBodyLayout);
    menuContentLayout.addClassNames(
        "user-popover-content", "cosmos-theme", "cosmos-user-menu", REFERENCE_OVERLAY);
    menuContentLayout.setWidth(null);
    menuContentLayout.setSpacing(false);
    menuContentLayout.setPadding(false);
    userMenuPopover.add(menuContentLayout);
    return userMenuPopover;
  }

  private Div createIdentity() {
    overlayIcon =
        new OverlayUserIcon(messageProperties, currentUser, currentStatus, IconSize.PIXEL_96, true);
    overlayIcon.addClassNames("user-popover-avatar", "portal-menu-avatar");

    profileNameSpan = new Span();
    profileNameSpan.addClassName("portal-menu-name");
    refreshProfileName();

    Span accountEmailSpan = new Span(currentUser.getEmail());
    accountEmailSpan.addClassName("portal-menu-email");

    menuPresenceBadge = new Badge();
    menuPresenceBadge.addClassName("portal-menu-presence");

    Div accountBadgesDiv = new Div(menuPresenceBadge);
    accountBadgesDiv.addClassName("portal-menu-badges");
    Div accountIdentityDiv = new Div(profileNameSpan, accountEmailSpan, accountBadgesDiv);
    accountIdentityDiv.addClassName("portal-menu-identity-copy");

    Div identityHeaderDiv = new Div(overlayIcon, accountIdentityDiv);
    identityHeaderDiv.addClassName("portal-menu-identity");
    return identityHeaderDiv;
  }

  private VerticalLayout createStatusOptions() {
    presenceHeadingSpan = new Span();
    presenceHeadingSpan.addClassName("portal-menu-section");
    presenceHeadingSpan.setId("portal-menu-presence-heading");

    VerticalLayout statusChoicesLayout = new VerticalLayout();
    statusChoicesLayout.setPadding(false);
    statusChoicesLayout.setSpacing(false);
    for (UserStatus statusChoice : UserStatus.values()) {
      Button statusChoiceButton = new Button();
      statusChoiceButton.addClickListener(event -> handleUserStatusChange(statusChoice));
      statusChoiceButton.addClassName("portal-status-option");
      statusChoiceButton.getStyle().set("--portal-option-status-color", statusChoice.getColor());
      statusChoiceButton
          .getElement()
          .setAttribute("data-status", statusChoice.name().toLowerCase(Locale.ROOT));
      statusButtons.put(statusChoice, statusChoiceButton);
      statusChoicesLayout.add(statusChoiceButton);
    }

    Details presenceDetails = new Details("", statusChoicesLayout);
    presenceDetails.setId("portal-menu-presence-details");
    presenceDetails.setOpened(true);
    presenceDetails.setWidthFull();
    presenceDetails.addClassName("portal-menu-presence-details");
    presenceDetails
        .getElement()
        .setAttribute("aria-labelledby", presenceHeadingSpan.getId().orElseThrow());
    presenceToggle =
        new DetailsToggleButton(
            messageProperties, presenceDetails, messageProperties.getPortalPresenceHeading());
    presenceToggle.setId("portal-menu-presence-toggle");
    presenceToggle.setText("");

    HorizontalLayout presenceHeaderLayout =
        new HorizontalLayout(presenceHeadingSpan, presenceToggle);
    presenceHeaderLayout.setWidthFull();
    presenceHeaderLayout.setPadding(false);
    presenceHeaderLayout.setSpacing(false);
    presenceHeaderLayout.setAlignItems(FlexComponent.Alignment.CENTER);
    presenceHeaderLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
    presenceHeaderLayout.addClassName("portal-menu-presence-header");

    VerticalLayout presenceSectionLayout =
        new VerticalLayout(presenceHeaderLayout, presenceDetails);
    presenceSectionLayout.setPadding(false);
    presenceSectionLayout.setSpacing(false);
    presenceSectionLayout.setWidthFull();
    presenceSectionLayout.addClassName("portal-menu-status-options");
    return presenceSectionLayout;
  }

  private Div createLanguageChoice() {
    germanLanguageButton = createLanguageButton("DE", "Deutsch", Locale.GERMAN);
    englishLanguageButton = createLanguageButton("EN", "English", Locale.ENGLISH);

    HorizontalLayout languageButtonsLayout =
        new HorizontalLayout(germanLanguageButton, englishLanguageButton);
    languageButtonsLayout.addClassName("portal-language-switch");
    languageHeadingSpan.addClassName("portal-menu-section");

    Div languageLabelDiv = new Div(AppIcon.LANGUAGES.create(), languageHeadingSpan);
    languageLabelDiv.addClassName("portal-menu-language-label");

    Div languageChoiceDiv = new Div(languageLabelDiv, languageButtonsLayout);
    languageChoiceDiv.addClassName("portal-menu-language-row");
    return languageChoiceDiv;
  }

  private Div createFooter() {
    Div menuFooterDiv = new Div();
    menuFooterDiv.addClassName("portal-menu-footer");
    if (userPopoverActions.homeAction() != null) {
      homeButton =
          new Button(
              messageProperties.getBaseHomeButton(),
              event -> userPopoverActions.homeAction().run());
      homeButton.setPrefixComponent(AppIcon.HOME.create());
      homeButton.addThemeVariants(LUMO_TERTIARY_INLINE);
      homeButton.addClassName(USER_MENU_ACTION);
      menuFooterDiv.add(homeButton);
    }

    logoutButton =
        new Button(messageProperties.getBaseLogoutButton(), event -> securityService.logout());
    logoutButton.setPrefixComponent(AppIcon.SIGN_OUT.create());
    logoutButton.addThemeVariants(LUMO_TERTIARY_INLINE);
    logoutButton.addClassNames(USER_MENU_ACTION, "portal-menu-logout");
    menuFooterDiv.add(logoutButton);

    return menuFooterDiv;
  }

  private void refreshStatusButtons() {
    for (Map.Entry<UserStatus, Button> entry : statusButtons.entrySet()) {
      UserStatus status = entry.getKey();
      Button statusChoiceButton = entry.getValue();
      Span statusLabelSpan = new Span(messageProperties.getUserStatusLabel(status));
      statusLabelSpan.addClassName("portal-status-label");
      Span statusDescriptionSpan = new Span(messageProperties.getUserStatusDescription(status));
      statusDescriptionSpan.addClassName("portal-status-description");
      Div statusChoiceTextDiv = new Div(statusLabelSpan, statusDescriptionSpan);
      statusChoiceTextDiv.addClassName("portal-status-copy");
      Div content = new Div(status.getSelectionComponent(), statusChoiceTextDiv);
      content.addClassName("portal-status-option-content");
      statusChoiceButton.setPrefixComponent(content);
      statusChoiceButton.setAriaLabel(statusLabelSpan.getText());
      statusChoiceButton
          .getElement()
          .setAttribute("aria-pressed", Boolean.toString(status == currentStatus));
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
    if (profileNameSpan != null) {
      profileNameSpan.setText(buildFullName());
      profileNameSpan.setVisible(hasUserName());
    }
    if (overlayIcon != null) {
      overlayIcon.setIdentity(currentUser);
    }
  }

  private void addManageAccountButton(VerticalLayout contentLayout) {
    manageAccountButton = new Button(messageProperties.getBaseManageAccountButton());
    manageAccountButton.setPrefixComponent(AppIcon.COG.create());
    manageAccountButton.addClickListener(event -> {
      userPopover.close();
      UI.getCurrent().navigate(AccountView.class);
    });

    manageAccountButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    manageAccountButton.addClassName("portal-menu-primary");
    contentLayout.add(manageAccountButton);
  }

  private void addManageApplicationButton(VerticalLayout contentLayout) {
    if (userPopoverActions.manageApplicationAction() == null) {
      return;
    }

    manageApplicationButton =
        new Button(
            messageProperties.getBaseManageApplicationButton(),
            clickEvent -> userPopoverActions.manageApplicationAction().run());
    manageApplicationButton.setPrefixComponent(AppIcon.SHIELD_COG.create());
    manageApplicationButton.addClassName(USER_MENU_ACTION);
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
    select.addValueChangeListener(event -> {
      if (event.isFromClient()) {
        handleUserStatusChange(event.getValue());
      }
    });
    return select;
  }

  private Component createUserStatusRenderer(UserStatus userStatus) {
    HorizontalLayout statusOptionLayout = new HorizontalLayout();
    statusOptionLayout.addClassName("portal-status-option-content");
    statusOptionLayout.getStyle().set("--portal-option-status-color", userStatus.getColor());
    statusOptionLayout.setWidthFull();

    addStatusVisualComponent(statusOptionLayout, userStatus);
    statusOptionLayout.add(createStatusTextLayout(userStatus));

    return statusOptionLayout;
  }

  private void addStatusVisualComponent(
      HorizontalLayout statusOptionLayout, UserStatus userStatus) {
    Object statusVisualComponent = userStatus.getSelectionComponent();

    if (statusVisualComponent instanceof SvgIcon statusSvgIcon) {
      statusOptionLayout.add(createCenteredWrapper(statusSvgIcon));
      return;
    }

    if (statusVisualComponent instanceof Div statusIndicatorDiv) {
      statusOptionLayout.add(createCenteredWrapper(statusIndicatorDiv));
    }
  }

  private VerticalLayout createCenteredWrapper(Component statusVisualComponent) {
    VerticalLayout statusVisualWrapperLayout = new VerticalLayout();
    statusVisualWrapperLayout.setPadding(false);
    statusVisualWrapperLayout.setSpacing(false);
    statusVisualWrapperLayout.setWidth(null);
    statusVisualWrapperLayout.setSizeUndefined();
    statusVisualWrapperLayout.setAlignItems(FlexComponent.Alignment.CENTER);
    statusVisualWrapperLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
    statusVisualWrapperLayout.add(statusVisualComponent);

    return statusVisualWrapperLayout;
  }

  private VerticalLayout createStatusTextLayout(UserStatus userStatus) {
    VerticalLayout statusTextLayout = new VerticalLayout();
    statusTextLayout.setPadding(false);
    statusTextLayout.setSpacing(false);
    statusTextLayout.setWidthFull();
    statusTextLayout.addClassName("portal-status-copy");

    Span statusNameSpan = new Span(messageProperties.getUserStatusLabel(userStatus));
    statusNameSpan.addClassName("user-popover-status-label");
    Span statusDescriptionSpan = new Span(messageProperties.getUserStatusDescription(userStatus));
    statusDescriptionSpan.addClassNames(
        LumoUtility.FontSize.XXSMALL, LumoUtility.TextColor.SECONDARY);

    statusTextLayout.add(statusNameSpan);
    statusTextLayout.add(new Hr());
    statusTextLayout.add(statusDescriptionSpan);

    return statusTextLayout;
  }

  private void handleUserStatusChange(UserStatus userStatus) {
    if (userStatus == null) {
      return;
    }

    try {
      StatusUpdate statusUpdate = userService.updateUserStatus(currentUser, userStatus, true);
      currentUser.setStatusManuallySet(statusUpdate.manuallySet());
      applyExternalStatus(statusUpdate.status());
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

  private Button createLanguageButton(
      String languageLabelText, String accessibleLanguageName, Locale targetLocale) {
    Button languageButton = new Button(languageLabelText, event -> switchLanguage(targetLocale));
    languageButton.setId("portal-language-" + targetLocale.getLanguage());
    languageButton.setAriaLabel(accessibleLanguageName);
    return languageButton;
  }

  private void switchLanguage(Locale locale) {
    userService.updateUserLocale(currentUser.getEmail(), locale);

    VaadinSession.getCurrent().setLocale(locale);
    LanguageChangeEvent.fire(UI.getCurrent(), locale);
  }

  private void refreshTexts() {
    languageHeadingSpan.setText(messageProperties.getPortalLanguageHeading());
    presenceHeadingSpan.setText(messageProperties.getPortalPresenceHeading());
    presenceToggle.setAriaLabel(messageProperties.getPortalPresenceHeading());
    refreshStatusButtons();
    menuPresenceBadge.setText(messageProperties.getUserStatusLabel(currentStatus));
    menuPresenceBadge.getStyle().set("--account-presence-color", currentStatus.getColor());
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
    for (Button languageButton : new Button[] {germanLanguageButton, englishLanguageButton}) {
      if (languageButton != null) {
        boolean active =
            (languageButton == germanLanguageButton ? Locale.GERMAN : Locale.ENGLISH)
                .equals(selected);
        languageButton.setClassName("portal-language-active", active);
        languageButton.getElement().setAttribute("aria-pressed", Boolean.toString(active));
      }
    }
  }

  private void updateStatusIcons(UserStatus userStatus) {
    if (menuPresenceBadge != null) {
      menuPresenceBadge.setText(messageProperties.getUserStatusLabel(userStatus));
      menuPresenceBadge.getStyle().set("--account-presence-color", userStatus.getColor());
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
