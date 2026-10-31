package de.derpeterson.app.ui.components;

import static de.derpeterson.app.helper.logging.LogSafety.safeFailure;
import static de.derpeterson.app.ui.components.CosmosClasses.DELETE_BUTTON;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_ICON;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
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
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

/** Gleiche Sitzungsliste im Konto und im eingeschränkten Zugang nach Passwortprüfung. */
@Slf4j
public class AccountSessionsComponent extends AccountPanelComponent {
  private static final String ENTRY_DESCRIPTION_CLASS = "account-entry-description";

  private final transient MessageProperties messages;
  private SerializableSupplier<Snapshot> loader;
  private SerializableConsumer<String> revoke;
  private SerializableRunnable revokeOthers;
  private SerializableRunnable failureAction = () -> {};
  private Snapshot snapshot;
  private boolean restricted;
  private final Div rows = new Div();
  private final Span count = new Span();
  private final Span activity = new Span();
  private final Span limit = new Span();
  private final Span hint = new Span();
  private final Button logout = new Button();
  private final Button refresh = new Button();
  private final PortalConfirmationDialog confirmation;

  public AccountSessionsComponent(MessageProperties messages, int initialLimit) {
    super("account-panel-sessions", AppIcon.DESKTOP);
    this.messages = messages;
    confirmation = new PortalConfirmationDialog(messages, "sessions-confirmation");
    confirmation.addClassName("portal-session-confirmation");
    add(confirmation);
    snapshot = new Snapshot(List.of(), 0, initialLimit);
    SvgIcon devices = AppIcon.DEVICES.create();
    devices.addClassName("account-devices-icon");
    devices.getElement().setAttribute("aria-hidden", "true");
    setIcon(devices);
    count.addClassNames("account-entry-title", "account-session-count");
    activity.addClassName(ENTRY_DESCRIPTION_CLASS);
    limit.addClassName("cosmos-admin-role-chip");
    limit.setId("account-sessions-limit");
    Div heading = new Div(new Div(count, activity), limit);
    heading.addClassName("account-session-heading");
    rows.setId("account-session-entries");
    getBody().add(heading, rows);
    logout.setId("account-logout-others");
    logout.setPrefixComponent(AppIcon.SIGN_OUT.create());
    logout.addClassName(DELETE_BUTTON);
    logout.addClickListener(event -> confirmOthers());
    refresh.setId("account-sessions-refresh");
    refresh.setPrefixComponent(AppIcon.REFRESH.create());
    refresh.addClickListener(
        event -> {
          if (reload()) {
            NotificationHelper.getInstance()
                .showNotification(
                    messages::getSessionRefreshSuccess,
                    null,
                    NotificationHelper.NotificationType.SUCCESS);
          }
        });
    Div actions = new Div(refresh, logout);
    actions.addClassName("cosmos-panel-actions");
    addFooter(hint, actions);
    refreshTexts();
  }

  public void configure(
      SerializableSupplier<Snapshot> loader,
      SerializableConsumer<String> revoke,
      SerializableRunnable revokeOthers) {
    this.loader = loader;
    this.revoke = revoke;
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

  public boolean reload() {
    if (loader == null) {
      return false;
    }
    try {
      snapshot = Objects.requireNonNull(loader.get());
      refreshTexts();
      return true;
    } catch (RuntimeException failure) {
      snapshot = new Snapshot(List.of(), 0, snapshot.limit());
      refreshTexts();
      failed(failure);
      return false;
    }
  }

  public void refreshTexts() {
    confirmation.refreshTexts();
    setHeading(
        restricted
            ? messages.getSessionRecoveryTitle()
            : messages.getAccountSettingsSessionsTitle(),
        restricted
            ? messages.getSessionRecoveryHint()
            : messages.getAccountSettingsSessionsDescription(),
        restricted ? "" : messages.getAccountSettingsSessionsMeta());
    String activeCountText = messages.getPortalSessionActiveCount(snapshot.active());
    // Die DE-/EN-Texte beginnen mit der formatierten Anzahl; der Wortlaut bleibt erhalten.
    int labelStart = activeCountText.indexOf(' ');
    count.removeAll();
    if (labelStart > 0) {
      Span value = new Span(activeCountText.substring(0, labelStart));
      value.addClassName("account-session-count-value");
      Span divider = new Span();
      divider.addClassName("account-session-count-divider");
      divider.getElement().setAttribute("aria-hidden", "true");
      Span label = new Span(activeCountText.substring(labelStart));
      label.addClassName("account-session-count-label");
      count.add(value, divider, label);
    } else {
      count.setText(activeCountText);
    }
    activity.setText(messages.getAccountSettingsSessionsActivity());
    limit.setText(messages.getAccountSettingsSessionsLimit(snapshot.limit()));
    hint.setText(
        restricted ? messages.getSessionRecoveryListHint() : messages.getSessionManagementHint());
    logout.setText(logoutActionText());
    refresh.setText(messages.getSessionRefresh());
    refresh.setEnabled(loader != null);
    logout.setEnabled(
        revokeOthers != null && snapshot.entries().stream().anyMatch(entry -> !entry.current()));
    rows.removeAll();
    snapshot.entries().forEach(entry -> rows.add(row(entry)));
  }

  private Div row(Entry entry) {
    Div symbol = new Div((entry.device().mobile() ? AppIcon.MOBILE : AppIcon.DESKTOP).create());
    symbol.addClassName(PANEL_ICON);
    String device =
        String.join(
            " · ",
            Stream.of(entry.device().system(), entry.device().browser())
                .filter(value -> !value.isBlank())
                .toList());
    Span title = new Span(device.isBlank() ? messages.getSessionUnknownDevice() : device);
    title.addClassName("account-entry-title");
    DateTimeFormatter formatter =
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
            .withLocale(
                VaadinSession.getCurrent() == null
                    ? getLocale()
                    : VaadinSession.getCurrent().getLocale())
            .withZone(ZoneId.systemDefault());
    String description =
        (entry.active() ? messages.getSessionLastActivity() : messages.getPortalSessionLastUsed())
            + ": "
            + formatter.format(entry.lastActivity());
    Span detail = new Span(description);
    detail.addClassName(ENTRY_DESCRIPTION_CLASS);
    Div copy = new Div(title, detail);
    copy.addClassName("account-entry-copy");
    Span since =
        new Span(
            (entry.active() ? messages.getSessionStarted() : messages.getSessionSavedSince())
                + ": "
                + formatter.format(entry.created()));
    since.addClassName(ENTRY_DESCRIPTION_CLASS);
    copy.add(since);
    copy.add(sessionBadges(entry));
    Button action = sessionAction(entry);
    Div row = new Div(symbol, copy, action);
    row.addClassName("account-entry");
    row.getElement().setAttribute("data-session-state", sessionState(entry));
    return row;
  }

  private Div sessionBadges(Entry entry) {
    Div badges = new Div();
    badges.addClassName("account-session-badges");
    if (entry.current()) {
      Span badge = new Span(messages.getAccountSettingsCurrentDevice());
      badge.getElement().getThemeList().addAll(List.of("badge", "success"));
      badge.addClassName("account-current-device");
      badges.add(badge);
    } else {
      Span state =
          new Span(
              entry.active()
                  ? messages.getPortalSessionActive()
                  : messages.getPortalSessionInactive());
      state.addClassName("account-session-state");
      state.getElement().setAttribute("data-active", Boolean.toString(entry.active()));
      badges.add(state);
      if (entry.remembered()) {
        Span remembered = new Span(messages.getPortalSessionRemembered());
        remembered.addClassName("account-session-remembered");
        badges.add(remembered);
      }
    }
    return badges;
  }

  private Button sessionAction(Entry entry) {
    String actionLabel;
    if (entry.current()) {
      actionLabel = messages.getAccountSettingsCurrentSession();
    } else if (entry.active()) {
      actionLabel = messages.getBaseLogoutButton();
    } else {
      actionLabel = messages.getPortalSessionRevokeAction();
    }
    Button action = new Button(actionLabel);
    action.setPrefixComponent(entry.current() ? AppIcon.CHECK.create() : AppIcon.SIGN_OUT.create());
    action.addClassName("account-session-action");
    if (!entry.current()) {
      action.addClassName(DELETE_BUTTON);
      action.setAriaLabel(
          entry.active()
              ? messages.getPortalSessionLogoutAria(deviceName(entry))
              : messages.getPortalSessionSavedTitle());
      action.setTooltipText(action.getAriaLabel().orElseThrow());
    }
    action.setEnabled(!entry.current() && revoke != null);
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
    return name.isBlank() ? messages.getSessionUnknownDevice() : name;
  }

  private PortalContextCard context(Entry entry) {
    DateTimeFormatter formatter =
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
            .withLocale(
                VaadinSession.getCurrent() == null
                    ? getLocale()
                    : VaadinSession.getCurrent().getLocale())
            .withZone(ZoneId.systemDefault());
    PortalContextCard card =
        new PortalContextCard(
            (entry.device().mobile() ? AppIcon.MOBILE : AppIcon.DESKTOP).create(),
            deviceName(entry),
            (entry.active()
                    ? messages.getSessionLastActivity()
                    : messages.getPortalSessionLastUsed())
                + ": "
                + formatter.format(entry.lastActivity()),
            (entry.active() ? messages.getSessionStarted() : messages.getSessionSavedSince())
                + ": "
                + formatter.format(entry.created()));
    card.addMetadata(sessionBadges(entry));
    return card;
  }

  private String confirmationExplanation(Entry entry) {
    if (restricted) {
      return messages.getSessionRecoveryLogoutHint();
    }
    return entry.active()
        ? messages.getPortalSessionRetainedHint()
        : messages.getPortalSessionSavedExplanation();
  }

  private void confirmEntry(Entry entry) {
    if (entry.current() || revoke == null) {
      return;
    }
    confirmation.open(
        new PortalConfirmationDialog.Prompt(
            entry.active()
                ? messages::getPortalSessionLogoutTitle
                : messages::getPortalSessionSavedTitle,
            messages::getPortalSessionCheckSelection,
            entry.active()
                ? messages::getPortalSessionLogoutQuestion
                : messages::getPortalSessionSavedQuestion,
            () -> confirmationExplanation(entry),
            () -> context(entry),
            () -> "",
            entry.active() ? messages::getBaseLogoutButton : messages::getPortalSessionRevokeAction,
            true,
            AppIcon.SIGN_OUT::create),
        () ->
            perform(
                () ->
                    revoke.accept(
                        entry.savedHandle() == null ? entry.handle() : entry.savedHandle())));
  }

  private String logoutActionText() {
    return restricted
        ? messages.getSessionRecoveryLogoutAll()
        : messages.getAccountSettingsLogoutOthers();
  }

  private void confirmOthers() {
    if (revokeOthers == null) {
      return;
    }
    List<Entry> selected = snapshot.entries().stream().filter(entry -> !entry.current()).toList();
    if (selected.isEmpty()) {
      return;
    }
    confirmation.open(
        new PortalConfirmationDialog.Prompt(
            this::logoutActionText,
            () ->
                messages.getPortalSessionSelection(
                    (int) selected.stream().filter(Entry::active).count(),
                    (int) selected.stream().filter(entry -> !entry.active()).count()),
            messages::getPortalSessionOthersQuestion,
            () ->
                restricted
                    ? messages.getSessionRecoveryLogoutHint()
                    : messages.getPortalSessionRetainedHint(),
            () -> {
              Div cards = new Div();
              cards.addClassName("portal-context-list");
              selected.forEach(entry -> cards.add(context(entry)));
              return cards;
            },
            () -> "",
            this::logoutActionText,
            true,
            AppIcon.SIGN_OUT::create),
        () -> perform(() -> revokeOthers.run()));
  }

  private String sessionState(Entry entry) {
    if (entry.current()) {
      return "current";
    }
    return entry.active() ? "active" : "saved";
  }

  private void perform(Runnable action) {
    try {
      action.run();
      snapshot = Objects.requireNonNull(loader.get());
      refreshTexts();
      NotificationHelper.getInstance()
          .showNotification(
              messages::getBaseSuccessTitle,
              messages::getSessionRevoked,
              NotificationHelper.NotificationType.SUCCESS);
    } catch (RuntimeException failure) {
      failed(failure);
    }
  }

  private void failed(RuntimeException failure) {
    log.warn("Own session management failed", safeFailure(failure));
    NotificationHelper.getInstance()
        .showNotification(
            messages::getBaseFailedTitle,
            messages::getSessionManagementFailed,
            -1,
            NotificationHelper.NotificationType.ERROR);
    failureAction.run();
  }
}
