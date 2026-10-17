package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.function.SerializableSupplier;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.helper.ui.ValidationHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.IconSize;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.security.SessionManagementService.Snapshot;
import de.derpeterson.app.service.AccountProfileService.Changes;
import de.derpeterson.app.service.AccountProfileService.Profile;
import de.derpeterson.app.service.AccountSecurityService.EmailChange;
import de.derpeterson.app.service.AccountSecurityService.PasswordChange;
import de.derpeterson.app.service.AccountSecurityService.PasswordChanged;
import de.derpeterson.app.service.UserAvatarService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;

/** Eigene Profildaten, Kontosicherheit, Profilbild und Sitzungen. */
@Slf4j
public class AccountSettingsComponent extends Div {
  public enum AccountSection {
    PROFILE,
    SECURITY,
    SESSIONS
  }

  private final transient MessageProperties messages;
  private final SerializableFunction<Changes, Profile> saveProfile;
  private final List<SerializableConsumer<Profile>> profileSavedListeners = new ArrayList<>();
  private Long version;
  private OverlayUserIcon summaryAvatar;
  private final Span summaryName = new Span();
  private final List<Runnable> textUpdates = new ArrayList<>();
  private final Map<Tab, AccountPanelComponent> panels = new LinkedHashMap<>();
  private final Map<AccountSection, Tab> sectionTabs = new LinkedHashMap<>();
  private final List<SerializableConsumer<AccountSection>> sectionChangeListeners =
      new ArrayList<>();
  private boolean restoringSection;
  private final AccountSessionsComponent sessionPanel;
  private final Tabs tabs = new Tabs();
  private final TextField firstName = new TextField();
  private final TextField lastName = new TextField();
  private final EmailField email = new EmailField();
  private final AccountEmailChangeDialog emailChangeDialog;
  private final AccountPasswordChangeDialog passwordChangeDialog;
  private final AccountAvatarDialog avatarDialog;
  private final DatePicker birthDate = new DatePicker();
  private final ComboBox<Locale> preferredLanguage = new ComboBox<>();

  public AccountSettingsComponent(
      MessageProperties messages,
      UserEntity user,
      SerializableFunction<Changes, Profile> saveProfile,
      int maxSessionsPerUser) {
    this.messages = messages;
    sessionPanel = new AccountSessionsComponent(messages, maxSessionsPerUser);
    emailChangeDialog = new AccountEmailChangeDialog(messages);
    passwordChangeDialog = new AccountPasswordChangeDialog(messages);
    avatarDialog = new AccountAvatarDialog(messages, user);
    this.saveProfile = saveProfile;
    this.version = user == null ? null : user.getVersion();
    addClassName("account-settings");
    var breadcrumb = text(new Span(), messages::getAccountSettingsOverview);
    var title = text(new Span(), messages::getAccountSettingsTitle);
    var description = text(new Span(), messages::getAccountSettingsDescription);
    add(new CosmosSectionHeaderComponent(breadcrumb, title, description), summary(user));

    tabs.setId("account-tabs");
    tabs.addClassName("account-tabs");
    textUpdates.add(
        () -> tabs.getElement().setAttribute("aria-label", messages.getAccountSettingsTitle()));
    var profile = profile(user);
    addTab(
        AccountSection.PROFILE,
        VaadinIcon.USER.create(),
        messages::getAccountSettingsProfileTab,
        profile);
    addTab(
        AccountSection.SECURITY,
        VaadinIcon.LOCK.create(),
        messages::getAccountSettingsSecurityTab,
        security(user));
    addTab(
        AccountSection.SESSIONS,
        devicesIcon(),
        messages::getAccountSettingsSessionsTab,
        sessionPanel);
    tabs.addSelectedChangeListener(
        event -> {
          showSelectedPanel();
          if (!restoringSection) {
            sectionChangeListeners.forEach(listener -> listener.accept(getSelectedSection()));
          }
        });
    var content = new Div(panels.values().toArray(AccountPanelComponent[]::new));
    content.addClassName("account-panels");
    add(tabs, content, emailChangeDialog, passwordChangeDialog, avatarDialog);
    refreshTexts();
    showSelectedPanel();
  }

  private <T extends HasText> T text(T component, Supplier<String> value) {
    textUpdates.add(() -> component.setText(value.get()));
    return component;
  }

  private Button actionButton(VaadinIcon icon, Supplier<String> label, boolean primary) {
    var button = text(new Button(), label);
    button.setPrefixComponent(icon.create());
    if (primary) button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    return button;
  }

  private Button emailChangeButton(String id) {
    var button = actionButton(VaadinIcon.EDIT, messages::getAccountSettingsChangeEmail, false);
    button.setId(id);
    button.addClickListener(event -> emailChangeDialog.open());
    return button;
  }

  private Button passwordChangeButton() {
    var button = actionButton(VaadinIcon.EDIT, messages::getAccountSettingsChangePassword, false);
    button.setId("account-password-edit-security");
    button.addClickListener(event -> passwordChangeDialog.open());
    return button;
  }

  private CosmosCardComponent summary(UserEntity user) {
    var avatar =
        new OverlayUserIcon(
            messages,
            user,
            user == null ? UserStatus.getDefaultStatus() : user.getStatus(),
            IconSize.PIXEL_72);
    summaryAvatar = avatar;
    avatar.addClassName("account-summary-avatar");
    summaryName.setText(
        user == null
            ? ""
            : (Objects.toString(user.getFirstName(), "")
                    + " "
                    + Objects.toString(user.getLastName(), ""))
                .trim());
    summaryName.addClassName("account-summary-name");
    var address = new Span(user == null ? "" : Objects.toString(user.getEmail(), ""));
    address.addClassName("account-summary-email");
    var active =
        text(
            new Span(),
            user != null && user.isEnabled()
                ? messages::getAccountSettingsEnabled
                : messages::getAccountSettingsDisabled);
    active.addClassName("account-summary-status");
    active.getElement().getThemeList().add("badge");
    active.getElement().getThemeList().add(user != null && user.isEnabled() ? "success" : "error");
    var info = new Div(summaryName, address, active);
    info.addClassName("account-summary-details");
    var edit = actionButton(VaadinIcon.EDIT, messages::getAccountSettingsEditPicture, false);
    edit.setId("account-avatar-edit");
    edit.addClickListener(event -> avatarDialog.open());
    var row = new Div(avatar, info, edit);
    row.addClassName("account-summary-row");
    var card = new CosmosCardComponent(row);
    card.addClassName("account-summary");
    return card;
  }

  private AccountPanelComponent panel(
      String id,
      VaadinIcon icon,
      Supplier<String> title,
      Supplier<String> description,
      Supplier<String> meta) {
    var panel = new AccountPanelComponent("account-panel-" + id, icon);
    textUpdates.add(() -> panel.setHeading(title.get(), description.get(), meta.get()));
    return panel;
  }

  private void addTab(
      AccountSection section,
      Component symbol,
      Supplier<String> label,
      AccountPanelComponent panel) {
    var id = section.name().toLowerCase(Locale.ROOT);
    symbol.getElement().setAttribute("aria-hidden", "true");
    var tab = new Tab(symbol, text(new Span(), label));
    tab.setId("account-tab-" + id);
    tab.getElement().setAttribute("aria-controls", "account-panel-" + id);
    panel.getElement().setAttribute("role", "tabpanel");
    panel.getElement().setAttribute("aria-labelledby", "account-tab-" + id);
    panels.put(tab, panel);
    sectionTabs.put(section, tab);
    tabs.add(tab);
  }

  public AccountSection getSelectedSection() {
    return sectionTabs.entrySet().stream()
        .filter(entry -> entry.getValue() == tabs.getSelectedTab())
        .map(Map.Entry::getKey)
        .findFirst()
        .orElse(AccountSection.PROFILE);
  }

  public void addSectionChangeListener(SerializableConsumer<AccountSection> listener) {
    sectionChangeListeners.add(listener);
  }

  public void selectSection(AccountSection section) {
    // URL-Wiederherstellung darf keinen weiteren Historieneintrag auslösen.
    restoringSection = true;
    try {
      boolean sameTab = tabs.getSelectedTab() == sectionTabs.get(Objects.requireNonNull(section));
      tabs.setSelectedTab(sectionTabs.get(section));
      if (sameTab && section == AccountSection.SESSIONS) sessionPanel.reload();
    } finally {
      restoringSection = false;
    }
  }

  private void showSelectedPanel() {
    panels.forEach((tab, panel) -> panel.setVisible(tab == tabs.getSelectedTab()));
    if (getSelectedSection() == AccountSection.SESSIONS) sessionPanel.reload();
  }

  private AccountPanelComponent profile(UserEntity user) {
    var panel =
        panel(
            "profile",
            VaadinIcon.USER,
            messages::getAccountSettingsProfileTitle,
            messages::getAccountSettingsProfileDescription,
            messages::getAccountSettingsProfileMeta);
    firstName.setId("account-first-name");
    lastName.setId("account-last-name");
    email.setId("account-email");
    email.addClassName("account-profile-email");
    birthDate.setId("account-birth-date");
    preferredLanguage.setId("account-language");
    firstName.setRequiredIndicatorVisible(true);
    lastName.setRequiredIndicatorVisible(true);
    birthDate.setRequiredIndicatorVisible(true);
    preferredLanguage.setRequiredIndicatorVisible(true);
    email.setReadOnly(true);
    email.setEnabled(false);
    preferredLanguage.setItems(Locale.GERMAN, Locale.ENGLISH);
    preferredLanguage.setItemLabelGenerator(language -> language.getDisplayLanguage(language));
    preferredLanguage.setAllowCustomValue(false);
    birthDate.setAutoOpen(false);
    if (user != null) {
      firstName.setValue(Objects.toString(user.getFirstName(), ""));
      lastName.setValue(Objects.toString(user.getLastName(), ""));
      email.setValue(Objects.toString(user.getEmail(), ""));
      birthDate.setValue(user.getBirthDate());
      if (user.getPreferredLocale() != null) {
        preferredLanguage.setValue(
            Locale.GERMAN.getLanguage().equals(user.getPreferredLocale().getLanguage())
                ? Locale.GERMAN
                : Locale.ENGLISH);
      }
    }
    textUpdates.add(
        () -> {
          firstName.setLabel(messages.getRegistrationFirstNameField());
          firstName.setPlaceholder(messages.getAccountFirstNamePlaceholder());
          lastName.setLabel(messages.getRegistrationLastNameField());
          lastName.setPlaceholder(messages.getAccountLastNamePlaceholder());
          email.setLabel(messages.getRegistrationEmailField());
          email.setPlaceholder(messages.getAccountEmailPlaceholder());
          birthDate.setLabel(messages.getRegistrationBirthDateField());
          birthDate.setLocale(CustomI18NProvider.getCurrentLocale());
          birthDate.setPlaceholder(messages.getRegistrationBirthDatePlaceholder());
          preferredLanguage.setLabel(messages.getAccountSettingsLanguage());
          preferredLanguage.setHelperText(messages.getAccountSettingsLanguageHint());
        });
    var form = new Div(firstName, lastName, email, birthDate, preferredLanguage);
    form.addClassName("account-profile-form");
    panel.getBody().add(form);
    var save = actionButton(VaadinIcon.CHECK, messages::getAccountSettingsSave, true);
    save.setId("account-save");
    save.addClickListener(event -> saveProfile());
    panel.addFooter(text(new Span(), messages::getAccountSettingsAccountHint), save);
    return panel;
  }

  private void saveProfile() {
    // Wie in der Registrierung erst beim Absenden prüfen, nicht beim ersten Tastendruck.
    birthDate.setInvalid(false);
    if (!ValidationHelper.validateRequiredInputs(
        List.of(firstName, lastName, birthDate, preferredLanguage), messages)) return;
    if (!ValidationHelper.validateBirthDateBeforeInput(birthDate, messages)) return;
    Profile saved;
    try {
      saved =
          saveProfile.apply(
              new Changes(
                  version,
                  firstName.getValue(),
                  lastName.getValue(),
                  birthDate.getValue(),
                  preferredLanguage.getValue()));
    } catch (OptimisticLockingFailureException conflict) {
      NotificationHelper.getInstance()
          .showNotification(
              messages::getBaseFailedTitle,
              messages::getBaseUserUpdateConflict,
              -1,
              NotificationHelper.NotificationType.ERROR);
      return;
    } catch (RuntimeException failure) {
      log.warn("Kontoprofil konnte nicht gespeichert werden", failure);
      NotificationHelper.getInstance()
          .showNotification(
              messages::getBaseFailedTitle,
              messages::getAccountSettingsSaveFailed,
              -1,
              NotificationHelper.NotificationType.ERROR);
      return;
    }
    version = saved.version();
    firstName.setValue(saved.firstName());
    lastName.setValue(saved.lastName());
    birthDate.setValue(saved.birthDate());
    preferredLanguage.setValue(saved.language());
    summaryName.setText(saved.firstName() + " " + saved.lastName());
    summaryAvatar.setName(saved.firstName(), saved.lastName());
    profileSavedListeners.forEach(listener -> listener.accept(saved));
    NotificationHelper.getInstance()
        .showNotification(
            messages::getBaseSuccessTitle,
            messages::getAccountSettingsSaveSuccess,
            NotificationHelper.NotificationType.SUCCESS);
  }

  private Div actionRow(
      VaadinIcon icon, Supplier<String> title, Supplier<String> description, Button action) {
    var symbol = new Div(icon.create());
    symbol.addClassName("cosmos-panel-icon");
    var heading = text(new Span(), title);
    heading.addClassName("account-entry-title");
    var detail = text(new Span(), description);
    detail.addClassName("account-entry-description");
    var copy = new Div(heading, detail);
    copy.addClassName("account-entry-copy");
    var row = new Div(symbol, copy, action);
    row.addClassName("account-entry");
    return row;
  }

  private AccountPanelComponent security(UserEntity user) {
    var panel =
        panel(
            "security",
            VaadinIcon.LOCK,
            messages::getAccountSettingsSecurityTitle,
            messages::getAccountSettingsSecurityDescription,
            messages::getAccountSettingsSecurityTab);
    panel
        .getBody()
        .add(
            actionRow(
                VaadinIcon.LOCK,
                messages::getRegistrationPasswordField,
                messages::getAccountSettingsPasswordHint,
                passwordChangeButton()),
            actionRow(
                VaadinIcon.ENVELOPE,
                messages::getRegistrationEmailField,
                () -> user == null ? "" : Objects.toString(user.getEmail(), ""),
                emailChangeButton("account-email-edit-security")));
    panel.addFooter(text(new Span(), messages::getAccountSettingsAccountHint), new Span());
    return panel;
  }

  private SvgIcon devicesIcon() {
    var devices = new SvgIcon("icons/devices.svg");
    devices.addClassName("account-devices-icon");
    devices.getElement().setAttribute("aria-hidden", "true");
    return devices;
  }

  public void configureSessionActions(
      SerializableSupplier<Snapshot> loader,
      SerializableConsumer<String> revoke,
      SerializableRunnable revokeOthers) {
    sessionPanel.configure(loader, revoke, revokeOthers);
    sessionPanel.refreshTexts();
  }

  public void configureSecurityActions(
      SerializableFunction<PasswordChange, PasswordChanged> passwordAction,
      SerializableConsumer<EmailChange> emailAction) {
    passwordChangeDialog.setChangeAction(
        change -> {
          var changed = passwordAction.apply(change);
          // Nur den eigenen unveränderten Profilstand fortschreiben; fremde Änderungen bleiben
          // Konflikte.
          if (Objects.equals(version, changed.previousVersion())) version = changed.version();
          return changed;
        });
    emailChangeDialog.setRequestAction(emailAction);
  }

  public void addProfileSavedListener(SerializableConsumer<Profile> listener) {
    profileSavedListeners.add(listener);
  }

  public void configureAvatars(UserAvatarService avatars, HttpServletRequest request) {
    summaryAvatar.configureAvatar(avatars, null);
    avatarDialog.configure(avatars, request);
  }

  public void refreshSummaryName(String first, String last) {
    summaryName.setText(first + " " + last);
  }

  public void updateUserStatus(UserStatus status) {
    summaryAvatar.setUserStatus(status);
  }

  public void refreshTexts() {
    textUpdates.forEach(Runnable::run);
    emailChangeDialog.refreshTexts();
    passwordChangeDialog.refreshTexts();
    avatarDialog.refreshTexts();
    summaryAvatar.refreshTexts();
    sessionPanel.refreshTexts();
  }
}
