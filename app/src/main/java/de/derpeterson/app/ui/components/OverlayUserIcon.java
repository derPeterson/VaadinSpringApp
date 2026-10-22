package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
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

  private final transient MessageProperties messages;
  private final IconSize iconSize;
  private final boolean showStatus;
  private final Span initials = new Span();
  private final Div status = new Div();
  private final Image photo = new Image();
  private Long userId;
  private transient UserAvatarService avatars;
  private transient Registration profileRegistration;
  private UUID displayedRevision;
  private final Tooltip tooltip;
  private UserStatus userStatus;
  private Select<UserStatus> statusSelection;
  private String name = "";
  private transient Registration languageRegistration;

  public OverlayUserIcon(
      MessageProperties messages, UserEntity user, UserStatus userStatus, IconSize iconSize) {
    this(messages, user, userStatus, iconSize, false);
  }

  public OverlayUserIcon(
      MessageProperties messages,
      UserEntity user,
      UserStatus userStatus,
      IconSize iconSize,
      boolean showStatus) {
    this.messages = messages;
    this.showStatus = showStatus;
    this.iconSize = iconSize;
    addClassName("cosmos-user-avatar");
    getStyle().set("--cosmos-avatar-size", iconSize.getWidth() + iconSize.getUnit().getSymbol());
    getElement().setAttribute("role", "img");
    initials.addClassName("cosmos-avatar-initials");
    initials.getElement().setAttribute(ARIA_HIDDEN, "true");
    photo.addClassName("cosmos-avatar-photo");
    photo.getElement().setAttribute(ARIA_HIDDEN, "true");
    photo.setVisible(false);
    photo
        .getElement()
        .addEventListener(
            "error",
            event -> {
              photo.setVisible(false);
              initials.setVisible(true);
            });
    Div circle = new Div(initials, photo);
    circle.addClassName("cosmos-avatar-circle");
    status.addClassName("cosmos-avatar-status");
    status.setVisible(showStatus);
    status.getElement().setAttribute(ARIA_HIDDEN, "true");
    add(circle, status);
    tooltip = Tooltip.forComponent(this).withPosition(Tooltip.TooltipPosition.BOTTOM);
    setIdentity(user);
    setUserStatus(userStatus);
    addAttachListener(
        event -> {
          refreshTexts();
          listenProfiles();
          languageRegistration =
              ComponentUtil.addListener(
                  event.getUI(), LanguageChangeEvent.class, change -> refreshTexts());
        });
    addDetachListener(
        event -> {
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

  public void setName(String firstName, String lastName) {
    name =
        (Objects.toString(firstName, "").strip() + " " + Objects.toString(lastName, "").strip())
            .strip();
    initials.removeAll();
    initials.setText(initialsFor(firstName, lastName));
    if (initials.getText().isEmpty()) {
      initials.add(VaadinIcon.USER.create());
    }
    refreshTexts();
  }

  static String initialsFor(String firstName, String lastName) {
    return initial(firstName) + initial(lastName);
  }

  private static String initial(String value) {
    String name = Objects.toString(value, "").strip();
    if (name.isEmpty()) {
      return "";
    }
    BreakIterator characters = BreakIterator.getCharacterInstance(Locale.ROOT);
    characters.setText(name);
    String upper = name.substring(0, characters.next()).toUpperCase(Locale.ROOT);
    // Auch bei Großschreibung mit mehreren Zeichen (z.B. ß → SS) nur ein Initial anzeigen.
    characters.setText(upper);
    return upper.substring(0, characters.next());
  }

  public void setUserStatus(UserStatus value) {
    userStatus = value == null ? UserStatus.getDefaultStatus() : value;
    status.removeAll();
    if (showStatus) {
      status.add(userStatus.getOutlineComponent(iconSize, true));
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
    avatars = service;
    applyState(metadata == null ? service.state(userId) : metadata);
    listenProfiles();
  }

  private void applyState(UserAvatarService.State metadata) {
    if (metadata == null) {
      metadata = UserAvatarService.State.initial();
    }
    boolean custom = metadata.mode() == AvatarMode.CUSTOM && metadata.hasImage();
    if (custom && !Objects.equals(displayedRevision, metadata.imageRevision())) {
      photo.setSrc(AvatarImageResource.handler(avatars, userId, metadata.imageRevision(), false));
      displayedRevision = metadata.imageRevision();
    }
    if (!custom) {
      displayedRevision = null;
      photo.getElement().removeAttribute("src");
    }
    photo.setVisible(custom);
    initials.setVisible(!custom);
  }

  private void listenProfiles() {
    if (avatars == null || userId == null || !isAttached() || profileRegistration != null) {
      return;
    }
    UI ui = getUI().orElseThrow();
    profileRegistration =
        avatars
            .events()
            .register(
                message -> {
                  if (!Objects.equals(userId, message.userId())) {
                    return;
                  }
                  ui.access(
                      () -> {
                        if (!isAttached()) {
                          return;
                        }
                        setName(message.firstName(), message.lastName());
                        applyState(avatars.state(userId));
                      });
                });
  }

  public void refreshTexts() {
    String statusLabel =
        messages.getUserStatusLabel(
            userStatus == null ? UserStatus.getDefaultStatus() : userStatus);
    String label = name.isEmpty() ? AppConstants.USER_ICON_ALT : name;
    getElement()
        .setAttribute(
            "aria-label", showStatus && !name.isEmpty() ? label + " – " + statusLabel : label);
    tooltip.setManual(statusSelection != null);
    String tooltipText = "";
    if (statusSelection == null) {
      tooltipText = showStatus ? statusLabel : label;
    }
    tooltip.setText(tooltipText);
    if (statusSelection != null) {
      tooltip.setOpened(false);
      statusSelection.setAriaLabel(messages.getAdminUsersGridStatus());
      statusSelection.setItemLabelGenerator(messages::getUserStatusLabel);
      statusSelection.getDataProvider().refreshAll();
      statusSelection.setValue(userStatus);
    }
  }
}
