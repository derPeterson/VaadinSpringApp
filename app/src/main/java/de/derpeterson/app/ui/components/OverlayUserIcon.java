package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.shared.Registration;
import de.derpeterson.app.config.AppConstants;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.AvatarMode;
import de.derpeterson.app.model.enums.IconSize;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.service.UserAvatarService;
import java.text.BreakIterator;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/** Gemeinsames Profilbild mit optionalem Statuszeichen bzw. direkter Statusauswahl. */
public class OverlayUserIcon extends Div {
  private static final String ARIA_HIDDEN = "aria-hidden";

  private final transient MessageProperties messageProperties;
  private final IconSize iconSize;
  private final boolean showStatus;
  private final Span avatarInitialsLabel = new Span();
  private final Div statusIndicatorContainer = new Div();
  private final Image avatarPhotoImage = new Image();
  private Long userId;
  private transient UserAvatarService userAvatarService;
  private transient Registration profileRegistration;
  private UUID displayedRevision;
  private final Tooltip avatarIdentityTooltip;
  private UserStatus userStatus;
  private Select<UserStatus> statusSelection;
  private String avatarDisplayName = "";
  private String identityTooltipEmail;
  private transient Registration languageRegistration;

  public OverlayUserIcon(
      MessageProperties messageProperties,
      UserEntity user,
      UserStatus userStatus,
      IconSize iconSize) {
    this(messageProperties, user, userStatus, iconSize, false);
  }

  public OverlayUserIcon(
      MessageProperties messageProperties,
      UserEntity user,
      UserStatus userStatus,
      IconSize iconSize,
      boolean showStatus) {
    this.messageProperties = messageProperties;
    this.showStatus = showStatus;
    this.iconSize = iconSize;
    addClassName("cosmos-user-avatar");
    getStyle().set("--cosmos-avatar-size", iconSize.getWidth() + iconSize.getUnit().getSymbol());
    getElement().setAttribute("role", "img");
    avatarInitialsLabel.addClassName("cosmos-avatar-initials");
    avatarInitialsLabel.getElement().setAttribute(ARIA_HIDDEN, "true");
    avatarPhotoImage.addClassName("cosmos-avatar-photo");
    avatarPhotoImage.getElement().setAttribute(ARIA_HIDDEN, "true");
    avatarPhotoImage.setVisible(false);
    avatarPhotoImage
        .getElement()
        .addEventListener(
            "error",
            event -> {
              avatarPhotoImage.setVisible(false);
              avatarInitialsLabel.setVisible(true);
            });
    Div avatarCircleContainer = new Div(avatarInitialsLabel, avatarPhotoImage);
    avatarCircleContainer.addClassName("cosmos-avatar-circle");
    statusIndicatorContainer.addClassName("cosmos-avatar-status");
    statusIndicatorContainer.setVisible(showStatus);
    statusIndicatorContainer.getElement().setAttribute(ARIA_HIDDEN, "true");
    add(avatarCircleContainer, statusIndicatorContainer);
    avatarIdentityTooltip = Tooltip.forComponent(this).withPosition(Tooltip.TooltipPosition.BOTTOM);
    setIdentity(user);
    setUserStatus(userStatus);
    addAttachListener(event -> {
      refreshTexts();
      if (identityTooltipEmail != null) {
        // Flow hängt freie Tooltips an die UI. Über das native Ziel bleibt der Stil lokal.
        getElement()
            .executeJs(
                """
                const tooltip = Array.from(this.ownerDocument.querySelectorAll('vaadin-tooltip'))
                    .find(candidate => candidate.target === this);
                tooltip?.classList.add('support-identity-tooltip');
                """);
      }
      listenProfiles();
      languageRegistration =
          ComponentUtil.addListener(
              event.getUI(), LanguageChangeEvent.class, change -> refreshTexts());
    });
    addDetachListener(event -> {
      if (profileRegistration != null) {
        profileRegistration.remove();
        profileRegistration = null;
      }
      if (languageRegistration != null) {
        languageRegistration.remove();
        languageRegistration = null;
      }
    });
  }

  public void setIdentity(UserEntity user) {
    userId = user == null ? null : user.getId();
    setName(user == null ? null : user.getFirstName(), user == null ? null : user.getLastName());
  }

  public void showIdentityTooltip(String email) {
    identityTooltipEmail = Objects.requireNonNull(email);
    avatarIdentityTooltip.withPosition(Tooltip.TooltipPosition.TOP);
    getElement().setAttribute("tabindex", "0");
    refreshTexts();
  }

  public void setName(String firstName, String lastName) {
    avatarDisplayName =
        (Objects.toString(firstName, "").strip() + " " + Objects.toString(lastName, "").strip())
            .strip();
    avatarInitialsLabel.removeAll();
    avatarInitialsLabel.setText(initialsFor(firstName, lastName));
    if (avatarInitialsLabel.getText().isEmpty()) {
      avatarInitialsLabel.add(AppIcon.USER.create());
    }
    refreshTexts();
  }

  static String initialsFor(String firstName, String lastName) {
    return initial(firstName) + initial(lastName);
  }

  private static String initial(String value) {
    String avatarDisplayName = Objects.toString(value, "").strip();
    if (avatarDisplayName.isEmpty()) {
      return "";
    }
    BreakIterator characters = BreakIterator.getCharacterInstance(Locale.ROOT);
    characters.setText(avatarDisplayName);
    String upper = avatarDisplayName.substring(0, characters.next()).toUpperCase(Locale.ROOT);
    // Auch bei Großschreibung mit mehreren Zeichen (z.B. ß → SS) nur ein Initial anzeigen.
    characters.setText(upper);
    return upper.substring(0, characters.next());
  }

  public void setUserStatus(UserStatus value) {
    userStatus = value == null ? UserStatus.getDefaultStatus() : value;
    getElement().setAttribute("data-status", userStatus.name().toLowerCase(Locale.ROOT));
    statusIndicatorContainer.removeAll();
    if (showStatus) {
      statusIndicatorContainer.add(userStatus.getOutlineComponent(iconSize, true));
      addClassName("cosmos-avatar-with-status");
      getStyle().set("--cosmos-avatar-status-color", userStatus.getColor());
      getStyle()
          .set(
              "--cosmos-avatar-status-shadow",
              "var(--cosmos-avatar-" + userStatus.name().toLowerCase(Locale.ROOT) + "-shadow)");
    }
    if (statusSelection != null) {
      statusSelection.setValue(userStatus);
    }
    refreshTexts();
  }

  public void setStatusSelection(Select<UserStatus> selection) {
    if (statusSelection != null) {
      remove(statusSelection);
    }
    statusSelection = Objects.requireNonNull(selection);
    statusSelection.addClassName("cosmos-avatar-status-select");
    statusSelection.getElement().getThemeList().add("cosmos");
    statusSelection.setNoVerticalOverlap(true);
    statusSelection.setValue(userStatus);
    getElement().setAttribute("role", "group");
    add(statusSelection);
    refreshTexts();
  }

  public void configureAvatar(UserAvatarService service, UserAvatarService.State metadata) {
    userAvatarService = service;
    applyState(metadata == null ? service.state(userId) : metadata);
    listenProfiles();
  }

  private void applyState(UserAvatarService.State metadata) {
    if (metadata == null) {
      metadata = UserAvatarService.State.initial();
    }
    boolean custom = metadata.mode() == AvatarMode.CUSTOM && metadata.hasImage();
    if (custom && !Objects.equals(displayedRevision, metadata.imageRevision())) {
      avatarPhotoImage.setSrc(
          AvatarImageResource.createAvatarImageDownloadHandler(
              userAvatarService, userId, metadata.imageRevision(), false));
      displayedRevision = metadata.imageRevision();
    }
    if (!custom) {
      displayedRevision = null;
      avatarPhotoImage.getElement().removeAttribute("src");
    }
    avatarPhotoImage.setVisible(custom);
    avatarInitialsLabel.setVisible(!custom);
  }

  private void listenProfiles() {
    if (userAvatarService == null
        || userId == null
        || !isAttached()
        || profileRegistration != null) {
      return;
    }
    UI ui = getUI().orElseThrow();
    profileRegistration =
        userAvatarService
            .events()
            .register(message -> {
              if (!Objects.equals(userId, message.userId())) {
                return;
              }
              ui.access(() -> {
                if (!isAttached()) {
                  return;
                }
                setName(message.firstName(), message.lastName());
                applyState(userAvatarService.state(userId));
              });
            });
  }

  public void refreshTexts() {
    String statusLabel =
        messageProperties.getUserStatusLabel(
            userStatus == null ? UserStatus.getDefaultStatus() : userStatus);
    String label = avatarDisplayName.isEmpty() ? AppConstants.USER_ICON_ALT : avatarDisplayName;
    getElement()
        .setAttribute(
            "aria-label",
            showStatus && !avatarDisplayName.isEmpty() ? label + " – " + statusLabel : label);
    avatarIdentityTooltip.setManual(statusSelection != null);
    String tooltipText = "";
    if (statusSelection == null) {
      if (identityTooltipEmail != null) {
        tooltipText =
            avatarDisplayName.isEmpty()
                ? identityTooltipEmail
                : avatarDisplayName + "\n" + identityTooltipEmail;
      } else {
        tooltipText = showStatus ? statusLabel : label;
      }
    }
    avatarIdentityTooltip.setText(tooltipText);
    if (statusSelection != null) {
      avatarIdentityTooltip.setOpened(false);
      statusSelection.setAriaLabel(messageProperties.getAdminUsersGridStatus());
      statusSelection.setItemLabelGenerator(messageProperties::getUserStatusLabel);
      statusSelection.getDataProvider().refreshAll();
      statusSelection.setValue(userStatus);
    }
  }
}
