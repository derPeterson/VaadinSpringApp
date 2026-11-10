package de.derpeterson.app.ui.components;

import static de.derpeterson.app.helper.logging.LogSafety.safeFailure;
import static de.derpeterson.app.ui.components.CosmosClasses.DELETE_BUTTON;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_ICON;

import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.security.SessionManagementService.Entry;
import de.derpeterson.app.security.SessionManagementService.Snapshot;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

/** Gleiche Sitzungsliste im Konto und im eingeschränkten Zugang nach Passwortprüfung. */
@Slf4j
public class AccountSessionsComponent extends AccountPanelComponent {
  private static final String ENTRY_DESCRIPTION_CLASS = "account-entry-description";
  private static final String SESSION_BADGE_CLASS = "account-session-badge";

  private final transient MessageProperties messageProperties;
  private SerializableSupplier<Snapshot> sessionSnapshotSupplier;
  private SerializableConsumer<String> revokeSessionAction;
  private SerializableRunnable revokeOtherSessionsAction;
  private SerializableRunnable failureAction = () -> {};
  private Snapshot sessionSnapshot;
  private boolean restricted;
  private final Div sessionRowsDiv = new Div();
  private final Badge activeSessionCountBadge = new Badge();
  private final Span sessionActivityHintSpan = new Span();
  private final Badge sessionLimitBadge = new Badge();
  private final Span sessionFooterHintSpan = new Span();
  private final Button signOutOtherSessionsButton = new Button();
  private final Button refreshSessionsButton = new Button();
  private final PortalConfirmationDialog sessionConfirmationDialog;

  public AccountSessionsComponent(MessageProperties messageProperties, int initialLimit) {
    super("account-panel-sessions", AppIcon.DESKTOP);
    this.messageProperties = messageProperties;
    sessionConfirmationDialog =
        new PortalConfirmationDialog(messageProperties, "sessions-confirmation");
    sessionConfirmationDialog.addClassName("portal-session-confirmation");
    add(sessionConfirmationDialog);
    sessionSnapshot = new Snapshot(List.of(), 0, initialLimit);
    SvgIcon devicesSvgIcon = AppIcon.DEVICES.create();
    devicesSvgIcon.addClassName("account-devices-icon");
    devicesSvgIcon.getElement().setAttribute("aria-hidden", "true");
    setIcon(devicesSvgIcon);
    activeSessionCountBadge.addClassName("account-session-count");
    sessionActivityHintSpan.addClassName(ENTRY_DESCRIPTION_CLASS);
    sessionLimitBadge.addClassName(SESSION_BADGE_CLASS);
    sessionLimitBadge.addThemeVariants(BadgeVariant.WARNING);
    sessionLimitBadge.setId("account-sessions-limit");

    Div sessionSummaryDiv = new Div(activeSessionCountBadge, sessionActivityHintSpan);
    sessionSummaryDiv.addClassName("account-session-summary");
    Div heading = new Div(sessionSummaryDiv, sessionLimitBadge);
    heading.addClassName("account-session-heading");
    sessionRowsDiv.setId("account-session-entries");
    getBody().add(heading, sessionRowsDiv);
    signOutOtherSessionsButton.setId("account-logout-others");
    signOutOtherSessionsButton.setPrefixComponent(AppIcon.SIGN_OUT.create());
    signOutOtherSessionsButton.addClassName(DELETE_BUTTON);
    signOutOtherSessionsButton.addClickListener(event -> confirmOthers());
    refreshSessionsButton.setId("account-sessions-refresh");
    refreshSessionsButton.setPrefixComponent(AppIcon.REFRESH.create());
    refreshSessionsButton.addClickListener(event -> {
      if (reloadSessionSnapshot()) {
        NotificationHelper.getInstance()
            .showNotification(
                messageProperties::getSessionRefreshSuccess,
                null,
                NotificationHelper.NotificationType.SUCCESS);
      }
    });

    Div sessionActionsDiv = new Div(refreshSessionsButton, signOutOtherSessionsButton);
    sessionActionsDiv.addClassName("cosmos-panel-actions");
    addFooter(sessionFooterHintSpan, sessionActionsDiv);
    refreshTexts();
  }

  public void configure(
      SerializableSupplier<Snapshot> sessionSnapshotSupplier,
      SerializableConsumer<String> revokeSessionAction,
      SerializableRunnable revokeOtherSessionsAction) {
    this.sessionSnapshotSupplier = sessionSnapshotSupplier;
    this.revokeSessionAction = revokeSessionAction;
    this.revokeOtherSessionsAction = revokeOtherSessionsAction;
    refreshTexts();
  }

  public void setRestricted(boolean restricted) {
    this.restricted = restricted;
    refreshTexts();
  }

  public void setFailureAction(SerializableRunnable action) {
    failureAction = action;
  }

  public boolean reloadSessionSnapshot() {
    if (sessionSnapshotSupplier == null) {
      return false;
    }
    try {
      sessionSnapshot = Objects.requireNonNull(sessionSnapshotSupplier.get());
      refreshTexts();
      return true;
    } catch (RuntimeException failure) {
      sessionSnapshot = new Snapshot(List.of(), 0, sessionSnapshot.limit());
      refreshTexts();
      showSessionActionFailure(failure);
      return false;
    }
  }

  public void refreshTexts() {
    sessionConfirmationDialog.refreshTexts();
    setHeading(
        restricted
            ? messageProperties.getSessionRecoveryTitle()
            : messageProperties.getAccountSettingsSessionsTitle(),
        restricted
            ? messageProperties.getSessionRecoveryHint()
            : messageProperties.getAccountSettingsSessionsDescription(),
        restricted ? "" : messageProperties.getAccountSettingsSessionsMeta());
    refreshActiveSessionCountBadge();

    sessionActivityHintSpan.setText(messageProperties.getAccountSettingsSessionsActivity());
    sessionLimitBadge.setText(
        messageProperties.getAccountSettingsSessionsLimit(sessionSnapshot.limit()));
    sessionFooterHintSpan.setText(
        restricted
            ? messageProperties.getSessionRecoveryListHint()
            : messageProperties.getSessionManagementHint());
    signOutOtherSessionsButton.setText(logoutActionText());
    refreshSessionsButton.setText(messageProperties.getSessionRefresh());
    refreshSessionsButton.setEnabled(sessionSnapshotSupplier != null);
    signOutOtherSessionsButton.setEnabled(
        revokeOtherSessionsAction != null
            && sessionSnapshot.entries().stream().anyMatch(entry -> !entry.current()));

    rebuildSessionGroups();
  }

  private void refreshActiveSessionCountBadge() {
    String activeCountText =
        messageProperties.getPortalSessionActiveCount(sessionSnapshot.active());
    // Die DE-/EN-Texte beginnen mit der formatierten Anzahl; der Wortlaut bleibt erhalten.
    int activeCountLabelStartIndex = activeCountText.indexOf(' ');
    if (activeCountLabelStartIndex > 0) {
      Span activeSessionCountSpan =
          new Span(activeCountText.substring(0, activeCountLabelStartIndex));
      activeSessionCountSpan.addClassName("account-session-count-value");
      Span activeCountDividerSpan = new Span();
      activeCountDividerSpan.addClassName("account-session-count-divider");
      activeCountDividerSpan.getElement().setAttribute("aria-hidden", "true");
      Span activeCountLabelSpan = new Span(activeCountText.substring(activeCountLabelStartIndex));
      Span activeCountContentSpan =
          new Span(activeSessionCountSpan, activeCountDividerSpan, activeCountLabelSpan);
      activeCountContentSpan.addClassName("account-session-count-content");
      activeSessionCountBadge.setContent(activeCountContentSpan);
    } else {
      activeSessionCountBadge.setText(activeCountText);
    }
  }

  private void rebuildSessionGroups() {
    sessionRowsDiv.removeAll();
    List<Entry> activeSessionEntries =
        sessionSnapshot.entries().stream().filter(Entry::active).toList();
    List<Entry> savedSessionEntries =
        sessionSnapshot.entries().stream().filter(entry -> !entry.active()).toList();

    if (!activeSessionEntries.isEmpty()) {
      sessionRowsDiv.add(
          sessionGroup(
              "active",
              messageProperties.getPortalSessionActiveGroup(activeSessionEntries.size()),
              activeSessionEntries));
    }
    if (!savedSessionEntries.isEmpty()) {
      sessionRowsDiv.add(
          sessionGroup(
              "saved",
              messageProperties.getPortalSessionSavedGroup(savedSessionEntries.size()),
              savedSessionEntries));
    }
  }

  private Section sessionGroup(
      String sessionGroupState, String sessionGroupLabelText, List<Entry> sessionEntries) {
    H3 sessionGroupHeading = new H3(sessionGroupLabelText);
    sessionGroupHeading.setId("account-session-group-" + sessionGroupState);
    Section group = new Section();
    group.addClassName("account-session-group");
    group.getElement().setAttribute("data-session-group", sessionGroupState);
    group.getElement().setAttribute("aria-labelledby", sessionGroupHeading.getId().orElseThrow());
    group.add(sessionGroupHeading);
    sessionEntries.forEach(entry -> group.add(buildSessionEntryRow(entry)));
    return group;
  }

  private Div buildSessionEntryRow(Entry entry) {
    Div deviceIconDiv =
        new Div((entry.device().mobile() ? AppIcon.MOBILE : AppIcon.DESKTOP).create());
    deviceIconDiv.addClassName(PANEL_ICON);

    Span deviceNameSpan = new Span(deviceName(entry));
    deviceNameSpan.addClassName("account-entry-title");

    Div sessionDetailsDiv = new Div(deviceNameSpan);
    sessionDetailsDiv.addClassName("account-entry-copy");
    sessionDetails(entry, ENTRY_DESCRIPTION_CLASS).forEach(sessionDetailsDiv::add);
    sessionDetailsDiv.add(sessionBadges(entry));

    Button sessionActionButton = sessionAction(entry);
    Div sessionRowDiv = new Div(deviceIconDiv, sessionDetailsDiv, sessionActionButton);
    sessionRowDiv.addClassName("account-entry");
    sessionRowDiv.getElement().setAttribute("data-session-state", sessionState(entry));

    return sessionRowDiv;
  }

  private List<Span> sessionDetails(Entry entry, String detailClass) {
    DateTimeFormatter formatter =
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
            .withLocale(
                VaadinSession.getCurrent() == null
                    ? getLocale()
                    : VaadinSession.getCurrent().getLocale())
            .withZone(ZoneId.systemDefault());
    List<Span> details = new ArrayList<>();
    details.add(
        sessionDetail(
            entry.active()
                ? messageProperties.getSessionLastActivity()
                : messageProperties.getPortalSessionLastUsed(),
            formatter.format(entry.lastActivity()),
            detailClass));
    details.add(
        sessionDetail(
            entry.active()
                ? messageProperties.getSessionStarted()
                : messageProperties.getSessionSavedSince(),
            formatter.format(entry.created()),
            detailClass));
    if (entry.active() && entry.signInMethod() != null) {
      Span signInMethodSpan =
          sessionDetail(
              messageProperties.getSessionSignedInViaLabel(),
              messageProperties.getSignInMethodLabel(entry.signInMethod()),
              detailClass);
      signInMethodSpan.addClassName("account-session-sign-in-method");
      details.add(signInMethodSpan);
    }
    return details;
  }

  private Span sessionDetail(String detailLabelText, String detailValueText, String detailClass) {
    Span sessionDetailSpan = new Span(detailLabelText + ": ");
    sessionDetailSpan.addClassName(detailClass);

    Span detailValueSpan = new Span(detailValueText);
    detailValueSpan.addClassName("account-session-detail-value");
    sessionDetailSpan.add(detailValueSpan);

    return sessionDetailSpan;
  }

  private Div sessionBadges(Entry entry) {
    Div sessionBadgesDiv = new Div();
    sessionBadgesDiv.addClassName("account-session-badges");
    if (entry.current()) {
      Badge badge = new Badge(messageProperties.getAccountSettingsCurrentDevice());
      badge.addClassNames(SESSION_BADGE_CLASS, "account-current-device");
      badge.addThemeVariants(BadgeVariant.SUCCESS);
      sessionBadgesDiv.add(badge);
    } else {
      Badge state =
          new Badge(
              entry.active()
                  ? messageProperties.getPortalSessionActive()
                  : messageProperties.getPortalSessionInactive());
      state.addClassNames(SESSION_BADGE_CLASS, "account-session-state");
      state.getElement().setAttribute("data-active", Boolean.toString(entry.active()));
      sessionBadgesDiv.add(state);
      if (entry.remembered()) {
        Badge remembered = new Badge(messageProperties.getPortalSessionRemembered());
        remembered.addClassName(SESSION_BADGE_CLASS);
        sessionBadgesDiv.add(remembered);
      }
    }

    return sessionBadgesDiv;
  }

  private Button sessionAction(Entry entry) {
    String actionLabel;
    if (entry.current()) {
      actionLabel = messageProperties.getAccountSettingsCurrentSession();
    } else if (entry.active()) {
      actionLabel = messageProperties.getBaseLogoutButton();
    } else {
      actionLabel = messageProperties.getPortalSessionRevokeAction();
    }
    Button sessionActionButton = new Button(actionLabel);
    sessionActionButton.setPrefixComponent(
        entry.current() ? AppIcon.CHECK.create() : AppIcon.SIGN_OUT.create());
    sessionActionButton.addClassName("account-session-action");
    if (!entry.current()) {
      sessionActionButton.addClassName(DELETE_BUTTON);
      sessionActionButton.setAriaLabel(
          entry.active()
              ? messageProperties.getPortalSessionLogoutAria(deviceName(entry))
              : messageProperties.getPortalSessionSavedTitle());
      sessionActionButton.setTooltipText(sessionActionButton.getAriaLabel().orElseThrow());
    }
    sessionActionButton.setEnabled(!entry.current() && revokeSessionAction != null);
    sessionActionButton.setId(
        entry.current() ? "account-current-session" : "session-revoke-" + entry.handle());
    sessionActionButton.addClickListener(event -> confirmEntry(entry));
    return sessionActionButton;
  }

  private String deviceName(Entry entry) {
    String deviceNameText =
        String.join(
            " · ",
            Stream.of(entry.device().system(), entry.device().browser())
                .filter(value -> !value.isBlank())
                .toList());
    return deviceNameText.isBlank() ? messageProperties.getSessionUnknownDevice() : deviceNameText;
  }

  private PortalContextCard buildSessionContextCard(Entry entry) {
    PortalContextCard card =
        new PortalContextCard(
            (entry.device().mobile() ? AppIcon.MOBILE : AppIcon.DESKTOP).create(),
            deviceName(entry));
    sessionDetails(entry, "portal-context-detail").forEach(card::addMetadata);
    card.addMetadata(sessionBadges(entry));
    return card;
  }

  private String confirmationExplanation(Entry entry) {
    if (restricted) {
      return messageProperties.getSessionRecoveryLogoutHint();
    }
    return entry.active()
        ? messageProperties.getPortalSessionRetainedHint()
        : messageProperties.getPortalSessionSavedExplanation();
  }

  private void confirmEntry(Entry entry) {
    if (entry.current() || revokeSessionAction == null) {
      return;
    }
    sessionConfirmationDialog.open(
        new PortalConfirmationDialog.Prompt(
            entry.active()
                ? messageProperties::getPortalSessionLogoutTitle
                : messageProperties::getPortalSessionSavedTitle,
            messageProperties::getPortalSessionCheckSelection,
            entry.active()
                ? messageProperties::getPortalSessionLogoutQuestion
                : messageProperties::getPortalSessionSavedQuestion,
            () -> confirmationExplanation(entry),
            () -> buildSessionContextCard(entry),
            () -> "",
            entry.active()
                ? messageProperties::getBaseLogoutButton
                : messageProperties::getPortalSessionRevokeAction,
            true,
            AppIcon.SIGN_OUT::create),
        () ->
            runSessionAction(() ->
                revokeSessionAction.accept(
                    entry.savedHandle() == null ? entry.handle() : entry.savedHandle())));
  }

  private String logoutActionText() {
    return restricted
        ? messageProperties.getSessionRecoveryLogoutAll()
        : messageProperties.getAccountSettingsLogoutOthers();
  }

  private void confirmOthers() {
    if (revokeOtherSessionsAction == null) {
      return;
    }
    List<Entry> selected =
        sessionSnapshot.entries().stream().filter(entry -> !entry.current()).toList();
    if (selected.isEmpty()) {
      return;
    }
    sessionConfirmationDialog.open(
        new PortalConfirmationDialog.Prompt(
            this::logoutActionText,
            () ->
                messageProperties.getPortalSessionSelection(
                    (int) selected.stream().filter(Entry::active).count(),
                    (int) selected.stream().filter(entry -> !entry.active()).count()),
            messageProperties::getPortalSessionOthersQuestion,
            () ->
                restricted
                    ? messageProperties.getSessionRecoveryLogoutHint()
                    : messageProperties.getPortalSessionRetainedHint(),
            () -> {
              Div sessionContextCardsDiv = new Div();
              sessionContextCardsDiv.addClassName("portal-context-list");
              selected.forEach(entry -> sessionContextCardsDiv.add(buildSessionContextCard(entry)));
              return sessionContextCardsDiv;
            },
            () -> "",
            this::logoutActionText,
            true,
            AppIcon.SIGN_OUT::create),
        () -> runSessionAction(() -> revokeOtherSessionsAction.run()));
  }

  private String sessionState(Entry entry) {
    if (entry.current()) {
      return "current";
    }
    return entry.active() ? "active" : "saved";
  }

  private void runSessionAction(Runnable action) {
    try {
      action.run();
      sessionSnapshot = Objects.requireNonNull(sessionSnapshotSupplier.get());
      refreshTexts();
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getBaseSuccessTitle,
              messageProperties::getSessionRevoked,
              NotificationHelper.NotificationType.SUCCESS);
    } catch (RuntimeException failure) {
      showSessionActionFailure(failure);
    }
  }

  private void showSessionActionFailure(RuntimeException failure) {
    log.warn("Own session management failed", safeFailure(failure));
    NotificationHelper.getInstance()
        .showNotification(
            messageProperties::getBaseFailedTitle,
            messageProperties::getSessionManagementFailed,
            -1,
            NotificationHelper.NotificationType.ERROR);
    failureAction.run();
  }
}
