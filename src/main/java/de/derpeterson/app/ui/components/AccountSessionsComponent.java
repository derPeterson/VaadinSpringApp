package de.derpeterson.app.ui.components;

import static de.derpeterson.app.helper.logging.LogSafety.safeFailure;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.icon.VaadinIcon;
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

  public AccountSessionsComponent(MessageProperties messages, int initialLimit) {
    super("account-panel-sessions", VaadinIcon.DESKTOP);
    this.messages = messages;
    snapshot = new Snapshot(List.of(), 0, initialLimit);
    SvgIcon devices = new SvgIcon("icons/devices.svg");
    devices.addClassName("account-devices-icon");
    devices.getElement().setAttribute("aria-hidden", "true");
    setIcon(devices);
    count.addClassName("account-entry-title");
    activity.addClassName(ENTRY_DESCRIPTION_CLASS);
    limit.addClassName("cosmos-admin-role-chip");
    limit.setId("account-sessions-limit");
    Div heading = new Div(new Div(count, activity), limit);
    heading.addClassName("account-session-heading");
    rows.setId("account-session-entries");
    getBody().add(heading, rows);
    logout.setId("account-logout-others");
    logout.setPrefixComponent(VaadinIcon.SIGN_OUT.create());
    logout.addClassName("cosmos-delete-button");
    logout.addClickListener(event -> perform(() -> revokeOthers.run()));
    refresh.setId("account-sessions-refresh");
    refresh.setPrefixComponent(VaadinIcon.REFRESH.create());
    refresh.addClickListener(event -> reload());
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

  public void reload() {
    if (loader == null) {
      return;
    }
    try {
      snapshot = Objects.requireNonNull(loader.get());
      refreshTexts();
    } catch (RuntimeException failure) {
      snapshot = new Snapshot(List.of(), 0, snapshot.limit());
      refreshTexts();
      failed(failure);
    }
  }

  public void refreshTexts() {
    setHeading(
        messages.getAccountSettingsSessionsTitle(),
        messages.getAccountSettingsSessionsDescription(),
        messages.getAccountSettingsSessionsMeta());
    count.setText(snapshot.active() + " " + messages.getAccountSettingsSessionsActiveTitle());
    activity.setText(messages.getAccountSettingsSessionsActivity());
    limit.setText(messages.getAccountSettingsSessionsLimit(snapshot.limit()));
    hint.setText(
        restricted ? messages.getSessionRecoveryLogoutHint() : messages.getSessionManagementHint());
    logout.setText(messages.getAccountSettingsLogoutOthers());
    refresh.setText(messages.getSessionRefresh());
    refresh.setEnabled(loader != null);
    logout.setEnabled(
        revokeOthers != null && snapshot.entries().stream().anyMatch(entry -> !entry.current()));
    rows.removeAll();
    snapshot.entries().forEach(entry -> rows.add(row(entry)));
  }

  private Div row(Entry entry) {
    Div symbol =
        new Div((entry.device().mobile() ? VaadinIcon.MOBILE : VaadinIcon.DESKTOP).create());
    symbol.addClassName("cosmos-panel-icon");
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
        (entry.active() ? messages.getSessionLastActivity() : messages.getSessionSavedLogin())
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
    if (entry.current()) {
      Span badge = new Span(messages.getAccountSettingsCurrentDevice());
      badge.getElement().getThemeList().addAll(List.of("badge", "success"));
      badge.addClassName("account-current-device");
      copy.add(new Div(badge));
    } else if (entry.active() && entry.remembered()) {
      Span remembered = new Span(messages.getLoginRememberMeCheckbox());
      remembered.addClassName(ENTRY_DESCRIPTION_CLASS);
      copy.add(remembered);
    }
    String actionLabel;
    String sessionState;
    if (entry.current()) {
      actionLabel = messages.getAccountSettingsCurrentSession();
      sessionState = "current";
    } else if (entry.active()) {
      actionLabel = messages.getBaseLogoutButton();
      sessionState = "active";
    } else {
      actionLabel = messages.getSessionRevokeSaved();
      sessionState = "saved";
    }
    Button action = new Button(actionLabel);
    action.setPrefixComponent(VaadinIcon.SIGN_OUT.create());
    action.setEnabled(!entry.current() && revoke != null);
    action.setId(entry.current() ? "account-current-session" : "session-revoke-" + entry.handle());
    action.addClickListener(
        event ->
            perform(
                () ->
                    revoke.accept(
                        entry.savedHandle() == null ? entry.handle() : entry.savedHandle())));
    Div row = new Div(symbol, copy, action);
    row.addClassName("account-entry");
    row.getElement().setAttribute("data-session-state", sessionState);
    return row;
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
