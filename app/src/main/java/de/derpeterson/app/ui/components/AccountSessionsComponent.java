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
  private SerializableRunnable revokeOthers;
  private SerializableRunnable failureAction = () -> {};
  private Snapshot sessionSnapshot;
  private boolean restricted;
  private final Div sessionRows = new Div();
  private final Badge activeSessionCountBadge = new Badge();
  private final Span sessionActivityHint = new Span();
  private final Badge sessionLimitBadge = new Badge();
  private final Span sessionFooterHint = new Span();
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
    SvgIcon devices = AppIcon.DEVICES.create();
    devices.addClassName("account-devices-icon");
    devices.getElement().setAttribute("aria-hidden", "true");
    setIcon(devices);
    activeSessionCountBadge.addClassName("account-session-count");
    sessionActivityHint.addClassName(ENTRY_DESCRIPTION_CLASS);
    sessionLimitBadge.addClassName(SESSION_BADGE_CLASS);
    sessionLimitBadge.addThemeVariants(BadgeVariant.WARNING);
    sessionLimitBadge.setId("account-sessions-limit");
    Div summary = new Div(activeSessionCountBadge, sessionActivityHint);
    summary.addClassName("account-session-summary");
    Div heading = new Div(summary, sessionLimitBadge);
    heading.addClassName("account-session-heading");
    sessionRows.setId("account-session-entries");
    getBody().add(heading, sessionRows);
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
    Div actions = new Div(refreshSessionsButton, signOutOtherSessionsButton);
    actions.addClassName("cosmos-panel-actions");
    addFooter(sessionFooterHint, actions);
    refreshTexts();
  }

  public void configure(
      SerializableSupplier<Snapshot> sessionSnapshotSupplier,
      SerializableConsumer<String> revokeSessionAction,
      SerializableRunnable revokeOthers) {
    this.sessionSnapshotSupplier = sessionSnapshotSupplier;
    this.revokeSessionAction = revokeSessionAction;
    this.revokeOthers = revokeOthers;
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
    String activeCountText =
        messageProperties.getPortalSessionActiveCount(sessionSnapshot.active());
    // Die DE-/EN-Texte beginnen mit der formatierten Anzahl; der Wortlaut bleibt erhalten.
    int labelStart = activeCountText.indexOf(' ');
    if (labelStart > 0) {
      Span value = new Span(activeCountText.substring(0, labelStart));
      value.addClassName("account-session-count-value");
      Span divider = new Span();
      divider.addClassName("account-session-count-divider");
      divider.getElement().setAttribute("aria-hidden", "true");
      Span label = new Span(activeCountText.substring(labelStart));
      Span content = new Span(value, divider, label);
      content.addClassName("account-session-count-content");
      activeSessionCountBadge.setContent(content);
    } else {
      activeSessionCountBadge.setText(activeCountText);
    }
    sessionActivityHint.setText(messageProperties.getAccountSettingsSessionsActivity());
    sessionLimitBadge.setText(
        messageProperties.getAccountSettingsSessionsLimit(sessionSnapshot.limit()));
    sessionFooterHint.setText(
        restricted
            ? messageProperties.getSessionRecoveryListHint()
            : messageProperties.getSessionManagementHint());
    signOutOtherSessionsButton.setText(logoutActionText());
    refreshSessionsButton.setText(messageProperties.getSessionRefresh());
    refreshSessionsButton.setEnabled(sessionSnapshotSupplier != null);
    signOutOtherSessionsButton.setEnabled(
        revokeOthers != null
            && sessionSnapshot.entries().stream().anyMatch(entry -> !entry.current()));
    sessionRows.removeAll();
    List<Entry> active = sessionSnapshot.entries().stream().filter(Entry::active).toList();
    List<Entry> saved =
        sessionSnapshot.entries().stream().filter(entry -> !entry.active()).toList();
    if (!active.isEmpty()) {
      sessionRows.add(
          sessionGroup(
              "active", messageProperties.getPortalSessionActiveGroup(active.size()), active));
    }
    if (!saved.isEmpty()) {
      sessionRows.add(
          sessionGroup("saved", messageProperties.getPortalSessionSavedGroup(saved.size()), saved));
    }
  }

  private Section sessionGroup(String state, String label, List<Entry> entries) {
    H3 heading = new H3(label);
    heading.setId("account-session-group-" + state);
    Section group = new Section();
    group.addClassName("account-session-group");
    group.getElement().setAttribute("data-session-group", state);
    group.getElement().setAttribute("aria-labelledby", heading.getId().orElseThrow());
    group.add(heading);
    entries.forEach(entry -> group.add(buildSessionEntryRow(entry)));
    return group;
  }

  private Div buildSessionEntryRow(Entry entry) {
    Div symbol = new Div((entry.device().mobile() ? AppIcon.MOBILE : AppIcon.DESKTOP).create());
    symbol.addClassName(PANEL_ICON);
    Span title = new Span(deviceName(entry));
    title.addClassName("account-entry-title");
    Div copy = new Div(title);
    copy.addClassName("account-entry-copy");
    sessionDetails(entry, ENTRY_DESCRIPTION_CLASS).forEach(copy::add);
    copy.add(sessionBadges(entry));
    Button action = sessionAction(entry);
    Div row = new Div(symbol, copy, action);
    row.addClassName("account-entry");
    row.getElement().setAttribute("data-session-state", sessionState(entry));
    return row;
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
      Span method =
          sessionDetail(
              messageProperties.getSessionSignedInViaLabel(),
              messageProperties.getSignInMethodLabel(entry.signInMethod()),
              detailClass);
      method.addClassName("account-session-sign-in-method");
      details.add(method);
    }
    return details;
  }

  private Span sessionDetail(String label, String value, String detailClass) {
    Span detail = new Span(label + ": ");
    detail.addClassName(detailClass);
    Span emphasis = new Span(value);
    emphasis.addClassName("account-session-detail-value");
    detail.add(emphasis);
    return detail;
  }

  private Div sessionBadges(Entry entry) {
    Div badges = new Div();
    badges.addClassName("account-session-badges");
    if (entry.current()) {
      Badge badge = new Badge(messageProperties.getAccountSettingsCurrentDevice());
      badge.addClassNames(SESSION_BADGE_CLASS, "account-current-device");
      badge.addThemeVariants(BadgeVariant.SUCCESS);
      badges.add(badge);
    } else {
      Badge state =
          new Badge(
              entry.active()
                  ? messageProperties.getPortalSessionActive()
                  : messageProperties.getPortalSessionInactive());
      state.addClassNames(SESSION_BADGE_CLASS, "account-session-state");
      state.getElement().setAttribute("data-active", Boolean.toString(entry.active()));
      badges.add(state);
      if (entry.remembered()) {
        Badge remembered = new Badge(messageProperties.getPortalSessionRemembered());
        remembered.addClassName(SESSION_BADGE_CLASS);
        badges.add(remembered);
      }
    }
    return badges;
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
    Button action = new Button(actionLabel);
    action.setPrefixComponent(entry.current() ? AppIcon.CHECK.create() : AppIcon.SIGN_OUT.create());
    action.addClassName("account-session-action");
    if (!entry.current()) {
      action.addClassName(DELETE_BUTTON);
      action.setAriaLabel(
          entry.active()
              ? messageProperties.getPortalSessionLogoutAria(deviceName(entry))
              : messageProperties.getPortalSessionSavedTitle());
      action.setTooltipText(action.getAriaLabel().orElseThrow());
    }
    action.setEnabled(!entry.current() && revokeSessionAction != null);
    action.setId(entry.current() ? "account-current-session" : "session-revoke-" + entry.handle());
    action.addClickListener(event -> confirmEntry(entry));
    return action;
  }

  private String deviceName(Entry entry) {
    String name =
        String.join(
            " · ",
            Stream.of(entry.device().system(), entry.device().browser())
                .filter(value -> !value.isBlank())
                .toList());
    return name.isBlank() ? messageProperties.getSessionUnknownDevice() : name;
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
    if (revokeOthers == null) {
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
              Div cards = new Div();
              cards.addClassName("portal-context-list");
              selected.forEach(entry -> cards.add(buildSessionContextCard(entry)));
              return cards;
            },
            () -> "",
            this::logoutActionText,
            true,
            AppIcon.SIGN_OUT::create),
        () -> runSessionAction(() -> revokeOthers.run()));
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
