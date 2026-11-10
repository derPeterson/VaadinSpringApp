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

  public void setIdentity(UserEntity avatarUserEntity) {
    userId = avatarUserEntity == null ? null : avatarUserEntity.getId();
    setName(
        avatarUserEntity == null ? null : avatarUserEntity.getFirstName(),
        avatarUserEntity == null ? null : avatarUserEntity.getLastName());
  }

  public void showIdentityTooltip(String identityEmailAddress) {
    identityTooltipEmail = Objects.requireNonNull(identityEmailAddress);
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

  private static String initial(String nameText) {
    String avatarDisplayName = Objects.toString(nameText, "").strip();
    if (avatarDisplayName.isEmpty()) {
      return "";
    }
    BreakIterator characters = BreakIterator.getCharacterInstance(Locale.ROOT);
    characters.setText(avatarDisplayName);

    String uppercaseName =
        avatarDisplayName.substring(0, characters.next()).toUpperCase(Locale.ROOT);
    // Auch bei Großschreibung mit mehreren Zeichen (z.B. ß → SS) nur ein Initial anzeigen.
    characters.setText(uppercaseName);
    return uppercaseName.substring(0, characters.next());
  }

  public void setUserStatus(UserStatus newUserStatus) {
    userStatus = newUserStatus == null ? UserStatus.getDefaultStatus() : newUserStatus;
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

  public void configureAvatar(
      UserAvatarService avatarService, UserAvatarService.State avatarState) {
    userAvatarService = avatarService;
    applyAvatarImageState(avatarState == null ? avatarService.state(userId) : avatarState);
    listenProfiles();
  }

  private void applyAvatarImageState(UserAvatarService.State avatarState) {
    if (avatarState == null) {
      avatarState = UserAvatarService.State.initial();
    }

    boolean hasCustomAvatarImage =
        avatarState.mode() == AvatarMode.CUSTOM && avatarState.hasImage();
    if (hasCustomAvatarImage && !Objects.equals(displayedRevision, avatarState.imageRevision())) {
      avatarPhotoImage.setSrc(
          AvatarImageResource.createAvatarImageDownloadHandler(
              userAvatarService, userId, avatarState.imageRevision(), false));
      displayedRevision = avatarState.imageRevision();
    }
    if (!hasCustomAvatarImage) {
      displayedRevision = null;
      avatarPhotoImage.getElement().removeAttribute("src");
    }

    avatarPhotoImage.setVisible(hasCustomAvatarImage);
    avatarInitialsLabel.setVisible(!hasCustomAvatarImage);
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
                applyAvatarImageState(userAvatarService.state(userId));
              });
            });
  }

  public void refreshTexts() {
    String statusLabel =
        messageProperties.getUserStatusLabel(
            userStatus == null ? UserStatus.getDefaultStatus() : userStatus);
    String avatarLabelText =
        avatarDisplayName.isEmpty() ? AppConstants.USER_ICON_ALT : avatarDisplayName;
    getElement()
        .setAttribute(
            "aria-label",
            showStatus && !avatarDisplayName.isEmpty()
                ? avatarLabelText + " – " + statusLabel
                : avatarLabelText);
    avatarIdentityTooltip.setManual(statusSelection != null);

    String tooltipText = "";
    if (statusSelection == null) {
      if (identityTooltipEmail != null) {
        tooltipText =
            avatarDisplayName.isEmpty()
                ? identityTooltipEmail
                : avatarDisplayName + "\n" + identityTooltipEmail;
      } else {
        tooltipText = showStatus ? statusLabel : avatarLabelText;
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
