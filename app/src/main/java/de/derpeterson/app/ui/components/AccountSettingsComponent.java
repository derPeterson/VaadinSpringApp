package de.derpeterson.app.ui.components;

import static de.derpeterson.app.helper.logging.LogSafety.safeFailure;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_ICON;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.router.QueryParameters;
import de.derpeterson.app.helper.ui.CompactDateInput;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.helper.ui.ValidationHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.AppRoute;
import de.derpeterson.app.model.enums.Gender;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.security.SessionManagementService.Snapshot;
import de.derpeterson.app.service.AccountProfileService.Changes;
import de.derpeterson.app.service.AccountProfileService.Profile;
import de.derpeterson.app.service.AccountSecurityService.EmailChange;
import de.derpeterson.app.service.AccountSecurityService.PasswordChange;
import de.derpeterson.app.service.AccountSecurityService.PasswordChanged;
import de.derpeterson.app.service.UserAvatarService;
import de.derpeterson.app.validation.UserInputRules;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;

/** Eigene Profildaten, Kontosicherheit, Profilbild und Sitzungen. */
@Slf4j
public class AccountSettingsComponent extends PortalPageComponent {
  public enum AccountSection {
    PROFILE,
    SECURITY,
    SESSIONS
  }

  private final transient MessageProperties messages;
  private final SerializableFunction<Changes, Profile> saveProfile;
  private final List<SerializableConsumer<Profile>> profileSavedListeners = new ArrayList<>();
  private Long version;
  private final Span pageTitle = new Span();
  private final Span pageDescription = new Span();
  private final Div content = new Div();
  private final PortalSidebarComponent sidebar;
  private final List<Runnable> textUpdates = new ArrayList<>();
  private final Map<AccountSection, AccountPanelComponent> panels = new LinkedHashMap<>();
  private final Map<AccountSection, SideNavItem> sectionItems = new LinkedHashMap<>();
  private final List<SerializableConsumer<AccountSection>> sectionChangeListeners =
      new ArrayList<>();
  private AccountSection selectedSection = AccountSection.PROFILE;
  private final AccountSessionsComponent sessionPanel;
  private final PageNavigationComponent navigation = new PageNavigationComponent();
  private final TextField firstName = new TextField();
  private final TextField lastName = new TextField();
  private final EmailField email = new EmailField();
  private final AccountEmailChangeDialog emailChangeDialog;
  private final AccountPasswordChangeDialog passwordChangeDialog;
  private ExternalConnectionsComponent externalConnections;
  private final AccountAvatarDialog avatarDialog;
  private final DatePicker birthDate = new DatePicker();
  private final ComboBox<Locale> preferredLanguage = new ComboBox<>();
  private final ComboBox<Gender> gender = new ComboBox<>();

  public AccountSettingsComponent(
      MessageProperties messages,
      UserEntity user,
      SerializableFunction<Changes, Profile> saveProfile,
      int maxSessionsPerUser) {
    this.messages = messages;
    email.setPattern(UserInputRules.EMAIL_REGEX);
    sessionPanel = new AccountSessionsComponent(messages, maxSessionsPerUser);
    emailChangeDialog = new AccountEmailChangeDialog(messages);
    passwordChangeDialog = new AccountPasswordChangeDialog(messages);
    avatarDialog = new AccountAvatarDialog(messages, user);
    this.saveProfile = saveProfile;
    this.version = user == null ? null : user.getVersion();
    Span breadcrumb = text(new Span(), messages::getAccountSettingsOverview);
    CosmosSectionHeaderComponent header =
        new CosmosSectionHeaderComponent(breadcrumb, pageTitle, pageDescription);
    textUpdates.add(this::refreshSectionHeading);
    navigation.setId("account-navigation");
    textUpdates.add(
        () ->
            navigation.getElement().setAttribute("aria-label", messages.getAccountSettingsTitle()));
    AccountPanelComponent profile = profile(user);
    addSectionNavigation(
        AccountSection.PROFILE,
        AppIcon.USER.create(),
        messages::getAccountSettingsProfileTab,
        profile);
    addSectionNavigation(
        AccountSection.SECURITY,
        AppIcon.LOCK.create(),
        messages::getAccountSettingsSecurityTab,
        security(user));
    addSectionNavigation(
        AccountSection.SESSIONS,
        devicesIcon(),
        messages::getAccountSettingsSessionsTab,
        sessionPanel);
    sidebar =
        new PortalSidebarComponent(
            messages, user, AppRoute.ACCOUNT, navigation, avatarDialog::open);
    content.add(panels.values().toArray(AccountPanelComponent[]::new));
    setPortalContent(header, sidebar, content);
    add(emailChangeDialog, passwordChangeDialog, avatarDialog);
    refreshTexts();
    selectSection(AccountSection.PROFILE);
  }

  private <T extends HasText> T text(T component, Supplier<String> value) {
    textUpdates.add(() -> component.setText(value.get()));
    return component;
  }

  private Button actionButton(AppIcon icon, Supplier<String> label, boolean primary) {
    Button button = text(new Button(), label);
    button.setPrefixComponent(icon.create());
    if (primary) {
      button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    }
    return button;
  }

  private Button emailChangeButton(String id) {
    Button button = actionButton(AppIcon.EDIT, messages::getAccountSettingsChangeAction, false);
    button.setId(id);
    textUpdates.add(() -> button.setAriaLabel(messages.getAccountSettingsChangeEmail()));
    button.addClickListener(event -> emailChangeDialog.open());
    return button;
  }

  private Button passwordChangeButton() {
    Button button = actionButton(AppIcon.EDIT, messages::getAccountSettingsChangeAction, false);
    button.setId("account-password-edit-security");
    textUpdates.add(() -> button.setAriaLabel(messages.getAccountSettingsChangePassword()));
    button.addClickListener(event -> passwordChangeDialog.open());
    return button;
  }

  private AccountPanelComponent panel(
      String id,
      AppIcon icon,
      Supplier<String> title,
      Supplier<String> description,
      Supplier<String> meta) {
    AccountPanelComponent panel = new AccountPanelComponent("account-panel-" + id, icon);
    textUpdates.add(() -> panel.setHeading(title.get(), description.get(), meta.get()));
    return panel;
  }

  private void addSectionNavigation(
      AccountSection section,
      Component symbol,
      Supplier<String> label,
      AccountPanelComponent panel) {
    String id = section.name().toLowerCase(Locale.ROOT);
    symbol.getElement().setAttribute("aria-hidden", "true");
    SideNavItem item = new SideNavItem("", AppRoute.ACCOUNT.getSecurityRoute(), symbol);
    item.setId("account-tab-" + id);
    item.setQueryParameters(QueryParameters.of("tab", id));
    textUpdates.add(() -> item.setLabel(label.get()));
    item.getElement().setAttribute("aria-controls", "account-panel-" + id);
    panel.getElement().setAttribute("role", "region");
    panel.getElement().setAttribute("aria-labelledby", "account-tab-" + id);
    panels.put(section, panel);
    sectionItems.put(section, item);
    navigation.registerAction(
        item,
        () -> {
          selectSection(section);
          closeNavigation();
          sectionChangeListeners.forEach(listener -> listener.accept(section));
        });
    navigation.addItem(item);
  }

  public AccountSection getSelectedSection() {
    return selectedSection;
  }

  public void addSectionChangeListener(SerializableConsumer<AccountSection> listener) {
    sectionChangeListeners.add(listener);
  }

  public void selectSection(AccountSection section) {
    // URL-Wiederherstellung darf keinen weiteren Historieneintrag auslösen.
    selectedSection = Objects.requireNonNull(section);
    sectionItems.forEach(
        (value, item) -> PageNavigationComponent.setItemActive(item, value == section));
    showSelectedPanel();
    refreshSectionHeading();
  }

  private void showSelectedPanel() {
    panels.forEach((section, panel) -> panel.setVisible(section == selectedSection));
    if (getSelectedSection() == AccountSection.SESSIONS) {
      sessionPanel.reload();
    }
  }

  private void refreshSectionHeading() {
    AccountSection selected = getSelectedSection();
    pageTitle.setText(
        switch (selected) {
          case PROFILE -> messages.getAccountSettingsProfileTitle();
          case SECURITY -> messages.getAccountSettingsSecurityTab();
          case SESSIONS -> messages.getAccountSettingsSessionsTab();
        });
    pageDescription.setText(
        switch (selected) {
          case PROFILE -> messages.getAccountSettingsDescription();
          case SECURITY -> messages.getAccountSettingsSecurityIntroduction();
          case SESSIONS -> messages.getAccountSettingsSessionsIntroduction();
        });
  }

  public void updateUnreadCount(long count) {
    sidebar.updateUnreadCount(count);
  }

  private AccountPanelComponent profile(UserEntity user) {
    AccountPanelComponent panel =
        panel(
            "profile",
            AppIcon.USER,
            messages::getAccountSettingsProfileTitle,
            messages::getAccountSettingsProfileDescription,
            () -> "");
    panel.setIcon(AppIcon.USER.create());
    firstName.setId("account-first-name");
    lastName.setId("account-last-name");
    email.setId("account-email");
    email.addClassName("account-profile-email");
    birthDate.setId("account-birth-date");
    preferredLanguage.setId("account-language");
    gender.setId("account-gender");
    gender.setItems(Gender.values());
    gender.setItemLabelGenerator(this::genderLabel);
    gender.setClearButtonVisible(true);
    firstName.setRequiredIndicatorVisible(true);
    lastName.setRequiredIndicatorVisible(true);
    birthDate.setClearButtonVisible(true);
    preferredLanguage.setRequiredIndicatorVisible(true);
    email.setReadOnly(true);
    email.setSuffixComponent(AppIcon.LOCK.create());
    preferredLanguage.setItems(Locale.GERMAN, Locale.ENGLISH);
    preferredLanguage.setItemLabelGenerator(language -> language.getDisplayLanguage(language));
    preferredLanguage.setAllowCustomValue(false);
    birthDate.setAutoOpen(false);
    CompactDateInput.configure(birthDate, messages);
    if (user != null) {
      firstName.setValue(Objects.toString(user.getFirstName(), ""));
      lastName.setValue(Objects.toString(user.getLastName(), ""));
      email.setValue(Objects.toString(user.getEmail(), ""));
      gender.setValue(user.getGender());
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
          email.setLabel(messages.getAccountEmailAddressLabel());
          email.setPlaceholder(messages.getAccountEmailPlaceholder());
          birthDate.setLabel(messages.getRegistrationBirthDateField());
          birthDate.setLocale(CustomI18NProvider.getCurrentLocale());
          CompactDateInput.configure(birthDate, messages);
          birthDate.setPlaceholder(messages.getRegistrationBirthDatePlaceholder());
          birthDate.setHelperText(messages.getAccountBirthDateFormatHint());
          gender.setLabel(messages.getRegistrationGenderCombobox());
          gender.setPlaceholder(messages.getAccountGenderPlaceholder());
          gender.setItemLabelGenerator(this::genderLabel);
          preferredLanguage.setLabel(messages.getAccountSettingsLanguage());
          preferredLanguage.setHelperText(messages.getAccountSettingsLanguageHint());
        });
    styleChoices(gender, this::genderLabel);
    styleChoices(preferredLanguage, language -> language.getDisplayLanguage(language));
    Div emailGroup = new Div(email);
    emailGroup.addClassName("account-profile-email-group");
    Span readOnly = text(new Span(), messages::getAccountReadOnlyLabel);
    readOnly.addClassName("account-readonly-label");
    emailGroup.add(readOnly);
    Div form = new Div(firstName, lastName, emailGroup, gender, birthDate, preferredLanguage);
    form.addClassName("account-profile-form");
    panel.getBody().add(form);
    Button save = actionButton(AppIcon.CHECK, messages::getAccountSettingsSave, true);
    save.setId("account-save");
    save.addClickListener(event -> saveProfile());
    panel.addActionFooter(save);
    return panel;
  }

  private void saveProfile() {
    // Wie in der Registrierung erst beim Absenden prüfen, nicht beim ersten Tastendruck.
    birthDate.setInvalid(false);
    if (!ValidationHelper.validateRequiredInputs(
        List.of(firstName, lastName, preferredLanguage), messages)) {
      return;
    }
    if (!ValidationHelper.validateBirthDateBeforeInput(birthDate, messages)) {
      return;
    }
    Profile saved;
    try {
      saved =
          saveProfile.apply(
              new Changes(
                  version,
                  firstName.getValue(),
                  lastName.getValue(),
                  birthDate.getValue(),
                  preferredLanguage.getValue(),
                  gender.getValue()));
    } catch (OptimisticLockingFailureException _) {
      NotificationHelper.getInstance()
          .showNotification(
              messages::getBaseFailedTitle,
              messages::getBaseUserUpdateConflict,
              -1,
              NotificationHelper.NotificationType.ERROR);
      return;
    } catch (RuntimeException failure) {
      log.warn("Kontoprofil konnte nicht gespeichert werden", safeFailure(failure));
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
    gender.setValue(saved.gender());
    preferredLanguage.setValue(saved.language());
    sidebar.refreshName(saved.firstName(), saved.lastName());
    profileSavedListeners.forEach(listener -> listener.accept(saved));
    NotificationHelper.getInstance()
        .showNotification(
            messages::getBaseSuccessTitle,
            messages::getAccountSettingsSaveSuccess,
            NotificationHelper.NotificationType.SUCCESS);
  }

  private Div actionRow(
      AppIcon icon, Supplier<String> title, Supplier<String> description, Button action) {
    Div symbol = new Div((icon == AppIcon.LOCK ? AppIcon.KEY : AppIcon.ENVELOPE).create());
    symbol.addClassName(PANEL_ICON);
    Span heading = text(new Span(), title);
    heading.addClassName("account-entry-title");
    Span detail = text(new Span(), description);
    detail.addClassName("account-entry-description");
    Div copy = new Div(heading, detail);
    copy.addClassName("account-entry-copy");
    Div row = new Div(symbol, copy, action);
    row.addClassName("account-entry");
    row.setClassName("account-email-security-entry", icon == AppIcon.ENVELOPE);
    return row;
  }

  private AccountPanelComponent security(UserEntity user) {
    AccountPanelComponent panel =
        panel(
            "security",
            AppIcon.LOCK,
            messages::getAccountSettingsSecurityTitle,
            messages::getAccountSettingsSecurityDescription,
            messages::getAccountSettingsSecurityTab);
    panel.setIcon(AppIcon.LOCK.create());
    panel
        .getBody()
        .add(
            actionRow(
                AppIcon.LOCK,
                messages::getRegistrationPasswordField,
                messages::getAccountSettingsPasswordHint,
                passwordChangeButton()),
            actionRow(
                AppIcon.ENVELOPE,
                messages::getAccountEmailAddressLabel,
                () -> user == null ? "" : Objects.toString(user.getEmail(), ""),
                emailChangeButton("account-email-edit-security")));
    panel.addFooter(text(new Span(), messages::getAccountSettingsAccountHint), new Span());
    return panel;
  }

  private SvgIcon devicesIcon() {
    SvgIcon devices = AppIcon.DEVICES.create();
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
          PasswordChanged changed = passwordAction.apply(change);
          // Nur den eigenen unveränderten Profilstand fortschreiben; fremde Änderungen bleiben
          // Konflikte.
          if (Objects.equals(version, changed.previousVersion())) {
            version = changed.version();
          }
          if (externalConnections != null) {
            externalConnections.refreshAccount();
          }
          return changed;
        });
    emailChangeDialog.setRequestAction(emailAction);
  }

  public void addProfileSavedListener(SerializableConsumer<Profile> listener) {
    profileSavedListeners.add(listener);
  }

  public void configureAvatars(UserAvatarService avatars, HttpServletRequest request) {
    sidebar.configureAvatars(avatars);
    avatarDialog.configure(avatars, request);
  }

  public void configureStatusSelection(Select<UserStatus> selection) {
    sidebar.configureStatusSelection(selection);
  }

  public void refreshSummaryName(String first, String last) {
    sidebar.refreshName(first, last);
  }

  public void updateUserStatus(UserStatus status) {
    sidebar.updateStatus(status);
  }

  private <T> void styleChoices(ComboBox<T> field, SerializableFunction<T, String> label) {
    PortalFields.styleChoice(field, label, true);
  }

  private String genderLabel(Gender value) {
    if (value == null) {
      return "";
    }
    return switch (value) {
      case MALE -> messages.getGenderMale();
      case FEMALE -> messages.getGenderFemale();
      case OTHER -> messages.getGenderOther();
    };
  }

  public void refreshTexts() {
    textUpdates.forEach(Runnable::run);
    gender.getDataProvider().refreshAll();
    preferredLanguage.getDataProvider().refreshAll();
    emailChangeDialog.refreshTexts();
    passwordChangeDialog.refreshTexts();
    avatarDialog.refreshTexts();
    sidebar.refreshTexts();
    sessionPanel.refreshTexts();
  }

  public void configureExternalLogins(
      ExternalConnectionsComponent connections,
      BooleanSupplier passwordRequired,
      BooleanSupplier emailPasswordRequired) {
    passwordChangeDialog.setCurrentPasswordRequired(passwordRequired);
    emailChangeDialog.setCurrentPasswordRequired(emailPasswordRequired);
    externalConnections = connections;
    panels.get(AccountSection.SECURITY).getBody().add(connections);
  }
}
