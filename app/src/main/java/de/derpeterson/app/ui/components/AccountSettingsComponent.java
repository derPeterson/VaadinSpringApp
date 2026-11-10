package de.derpeterson.app.ui.components;

import static de.derpeterson.app.config.AppRouteConstants.ACCOUNT_PORTAL_TAB;
import static de.derpeterson.app.config.AppRouteConstants.ACCOUNT_PROVIDERS_TAB;
import static de.derpeterson.app.config.AppRouteConstants.ACCOUNT_SECTION_PARAMETER;
import static de.derpeterson.app.config.AppRouteConstants.ACCOUNT_SECURITY_SECTION;
import static de.derpeterson.app.config.AppRouteConstants.ACCOUNT_TAB_PARAMETER;
import static de.derpeterson.app.helper.logging.LogSafety.safeFailure;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_ICON;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.TabSheetVariant;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.QueryParameters;
import de.derpeterson.app.config.AppRouteConstants;
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
  private static final String ARIA_HIDDEN = "aria-hidden";

  public enum AccountSection {
    PROFILE,
    SECURITY,
    SESSIONS;

    public String urlValue() {
      return this == SECURITY ? ACCOUNT_SECURITY_SECTION : name().toLowerCase(Locale.ROOT);
    }

    public QueryParameters queryParameters() {
      QueryParameters query = QueryParameters.of(ACCOUNT_SECTION_PARAMETER, urlValue());
      return this == SECURITY ? query.merging(ACCOUNT_TAB_PARAMETER, ACCOUNT_PORTAL_TAB) : query;
    }
  }

  private final transient MessageProperties messageProperties;
  private final List<Runnable> localizedTextUpdates = new ArrayList<>();

  // Seitenrahmen und Navigation
  private final Span pageTitle = new Span();
  private final Span pageDescription = new Span();
  private final Div accountSectionContent = new Div();
  private final PageNavigationComponent accountNavigation = new PageNavigationComponent();
  private final Map<AccountSection, AccountPanelComponent> panelsBySection = new LinkedHashMap<>();
  private final Map<AccountSection, SideNavItem> navigationItemsBySection = new LinkedHashMap<>();
  private final List<SerializableConsumer<AccountSection>> sectionChangeListeners =
      new ArrayList<>();
  private AccountSection selectedSection = AccountSection.PROFILE;

  // Persönliche Daten
  private final TextField firstNameField = new TextField();
  private final TextField lastNameField = new TextField();
  private final EmailField profileEmailField = new EmailField();
  private final DatePicker birthDatePicker = new DatePicker();
  private final ComboBox<Locale> preferredLanguageSelector = new ComboBox<>();
  private final ComboBox<Gender> genderSelector = new ComboBox<>();
  private final SerializableFunction<Changes, Profile> persistProfile;
  private Long profileVersion;
  private final List<SerializableConsumer<Profile>> profileSavedListeners = new ArrayList<>();

  // Sicherheit und Anmeldeanbieter
  private final TabSheet securityTabs = new TabSheet();
  private Tab portalAccessTab;
  private Tab providerAccessTab;
  private final Div providerAccessContent = new Div();
  private Div portalSecurityCards;
  private final AccountEmailChangeDialog emailChangeDialog;
  private final AccountPasswordChangeDialog passwordChangeDialog;
  private ExternalConnectionsComponent externalLoginSettings;
  private TwoFactorAccountComponent twoFactorAccount;

  // Kontoleiste und Sitzungen
  private final PortalSidebarComponent accountSidebar;
  private final AccountAvatarDialog avatarDialog;
  private final AccountSessionsComponent accountSessionsPanel;

  public AccountSettingsComponent(
      MessageProperties messageProperties,
      UserEntity user,
      SerializableFunction<Changes, Profile> persistProfile,
      int maxSessionsPerUser) {
    this.messageProperties = messageProperties;
    profileEmailField.setPattern(UserInputRules.EMAIL_REGEX);
    accountSessionsPanel = new AccountSessionsComponent(messageProperties, maxSessionsPerUser);
    emailChangeDialog = new AccountEmailChangeDialog(messageProperties);
    passwordChangeDialog = new AccountPasswordChangeDialog(messageProperties);
    avatarDialog = new AccountAvatarDialog(messageProperties, user);
    this.persistProfile = persistProfile;
    this.profileVersion = user == null ? null : user.getVersion();

    Span breadcrumb = withTextOnRefresh(new Span(), messageProperties::getAccountSettingsOverview);
    CosmosSectionHeaderComponent header =
        new CosmosSectionHeaderComponent(breadcrumb, pageTitle, pageDescription);
    localizedTextUpdates.add(this::refreshSectionHeading);
    accountNavigation.setId("account-navigation");
    localizedTextUpdates.add(() ->
        accountNavigation
            .getElement()
            .setAttribute("aria-label", messageProperties.getAccountSettingsTitle()));

    AccountPanelComponent profilePanel = createProfilePanel(user);
    addSectionNavigation(
        AccountSection.PROFILE,
        AppIcon.USER.create(),
        messageProperties::getAccountSettingsProfileTab,
        profilePanel);
    addSectionNavigation(
        AccountSection.SECURITY,
        AppIcon.LOCK.create(),
        messageProperties::getAccountSettingsSecurityTab,
        createSecurityPanel(user));
    addSectionNavigation(
        AccountSection.SESSIONS,
        devicesIcon(),
        messageProperties::getAccountSettingsSessionsTab,
        accountSessionsPanel);

    accountSidebar =
        new PortalSidebarComponent(
            messageProperties, user, AppRoute.ACCOUNT, accountNavigation, avatarDialog::open);
    accountSectionContent.add(panelsBySection.values().toArray(AccountPanelComponent[]::new));
    setPortalContent(header, accountSidebar, accountSectionContent);
    add(emailChangeDialog, passwordChangeDialog, avatarDialog);

    refreshTexts();
    selectSection(AccountSection.PROFILE);
  }

  /** Registriert den Text für den ersten Aufbau und spätere Sprachwechsel. */
  private <T extends HasText> T withTextOnRefresh(T component, Supplier<String> localizedText) {
    localizedTextUpdates.add(() -> component.setText(localizedText.get()));
    return component;
  }

  private Button actionButton(AppIcon icon, Supplier<String> label, boolean primary) {
    Button button = withTextOnRefresh(new Button(), label);
    button.setPrefixComponent(icon.create());
    if (primary) {
      button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    }
    return button;
  }

  private Button emailChangeButton(String id) {
    Button changeEmailButton =
        actionButton(AppIcon.EDIT, messageProperties::getAccountSettingsChangeAction, false);
    changeEmailButton.setId(id);
    localizedTextUpdates.add(
        () -> changeEmailButton.setAriaLabel(messageProperties.getAccountSettingsChangeEmail()));
    changeEmailButton.addClickListener(event -> startEmailChange());
    return changeEmailButton;
  }

  private void startEmailChange() {
    if (externalLoginSettings == null) {
      emailChangeDialog.open();
    } else {
      externalLoginSettings.requestEmailChange();
    }
  }

  private Button passwordChangeButton() {
    Button changePasswordButton =
        actionButton(AppIcon.EDIT, messageProperties::getAccountSettingsChangeAction, false);
    changePasswordButton.setId("account-password-edit-security");
    localizedTextUpdates.add(() ->
        changePasswordButton.setAriaLabel(messageProperties.getAccountSettingsChangePassword()));
    changePasswordButton.addClickListener(event -> startPasswordChange());
    return changePasswordButton;
  }

  private void startPasswordChange() {
    if (externalLoginSettings == null) {
      passwordChangeDialog.open();
    } else {
      externalLoginSettings.requestPasswordChange();
    }
  }

  private AccountPanelComponent createAccountPanel(
      String id,
      AppIcon icon,
      Supplier<String> title,
      Supplier<String> description,
      Supplier<String> meta) {
    AccountPanelComponent panel = new AccountPanelComponent("account-panel-" + id, icon);
    localizedTextUpdates.add(() -> panel.setHeading(title.get(), description.get(), meta.get()));
    return panel;
  }

  private void addSectionNavigation(
      AccountSection section,
      Component symbol,
      Supplier<String> label,
      AccountPanelComponent panel) {
    String id = section.urlValue();
    symbol.getElement().setAttribute(ARIA_HIDDEN, "true");
    SideNavItem item = new SideNavItem("", AppRoute.ACCOUNT.getSecurityRoute(), symbol);
    item.setId("account-tab-" + id);
    item.setQueryParameters(section.queryParameters());
    localizedTextUpdates.add(() -> item.setLabel(label.get()));
    item.getElement().setAttribute("aria-controls", "account-panel-" + id);
    panel.getElement().setAttribute("role", "region");
    panel.getElement().setAttribute("aria-labelledby", "account-tab-" + id);
    panelsBySection.put(section, panel);
    navigationItemsBySection.put(section, item);
    accountNavigation.registerAction(
        item,
        () -> {
          if (section == AccountSection.SECURITY && selectedSection == section) {
            selectSecurityTab(ACCOUNT_PORTAL_TAB);
            updateSecurityTabLocation();
            closeNavigation();
            return;
          }
          selectSection(section);
          closeNavigation();
          sectionChangeListeners.forEach(listener -> listener.accept(section));
        });
    accountNavigation.addItem(item);
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
    navigationItemsBySection.forEach(
        (value, item) -> PageNavigationComponent.setItemActive(item, value == section));
    showSelectedPanel();
    refreshSectionHeading();
  }

  public void selectSecurityTab(String tabParameter) {
    securityTabs.setSelectedTab(
        ACCOUNT_PROVIDERS_TAB.equals(tabParameter) ? providerAccessTab : portalAccessTab);
  }

  private void updateSecurityTabLocation() {
    if (selectedSection != AccountSection.SECURITY) {
      return;
    }
    String tab =
        securityTabs.getSelectedTab() == providerAccessTab
            ? ACCOUNT_PROVIDERS_TAB
            : ACCOUNT_PORTAL_TAB;
    QueryParameters query =
        AccountSection.SECURITY.queryParameters().merging(ACCOUNT_TAB_PARAMETER, tab);
    getUI()
        .ifPresent(ui ->
            ui.getPage()
                .getHistory()
                .replaceState(null, new Location(AppRouteConstants.ACCOUNT_ROUTE, query)));
  }

  private void selectProvidersTabAndUpdateUrl() {
    if (securityTabs.getSelectedTab() != providerAccessTab) {
      selectSecurityTab(ACCOUNT_PROVIDERS_TAB);
      updateSecurityTabLocation();
    }
  }

  private void showSelectedPanel() {
    panelsBySection.forEach((section, panel) -> panel.setVisible(section == selectedSection));
    if (getSelectedSection() == AccountSection.SESSIONS) {
      accountSessionsPanel.reloadSessionSnapshot();
    }
  }

  private void refreshSectionHeading() {
    AccountSection selected = getSelectedSection();
    pageTitle.setText(
        switch (selected) {
          case PROFILE -> messageProperties.getAccountSettingsProfileTitle();
          case SECURITY -> messageProperties.getAccountSettingsSecurityTab();
          case SESSIONS -> messageProperties.getAccountSettingsSessionsTab();
        });
    pageDescription.setText(
        switch (selected) {
          case PROFILE -> messageProperties.getAccountSettingsDescription();
          case SECURITY -> messageProperties.getAccountSettingsSecurityIntroduction();
          case SESSIONS -> messageProperties.getAccountSettingsSessionsIntroduction();
        });
  }

  public void updateUnreadCount(long count) {
    accountSidebar.updateUnreadCount(count);
  }

  private AccountPanelComponent createProfilePanel(UserEntity user) {
    AccountPanelComponent panel =
        createAccountPanel(
            "profile",
            AppIcon.USER,
            messageProperties::getAccountSettingsProfileTitle,
            messageProperties::getAccountSettingsProfileDescription,
            () -> "");
    panel.setIcon(AppIcon.USER.create());
    configureProfileFields();
    populateProfileFields(user);
    registerProfileFieldTexts();

    styleChoices(genderSelector, this::genderLabel);
    styleChoices(preferredLanguageSelector, this::languageLabel);

    Div profileEmailGroup = new Div(profileEmailField);
    profileEmailGroup.addClassName("account-profile-email-group");
    Span readOnly = withTextOnRefresh(new Span(), messageProperties::getAccountReadOnlyLabel);
    readOnly.addClassName("account-readonly-label");
    profileEmailGroup.add(readOnly);
    Div profileForm =
        new Div(
            firstNameField,
            lastNameField,
            profileEmailGroup,
            genderSelector,
            birthDatePicker,
            preferredLanguageSelector);
    profileForm.addClassName("account-profile-form");
    panel.getBody().add(profileForm);

    Button saveProfileButton =
        actionButton(AppIcon.CHECK, messageProperties::getAccountSettingsSave, true);
    saveProfileButton.setId("account-save");
    saveProfileButton.addClickListener(event -> saveProfile());
    panel.addActionFooter(saveProfileButton);
    return panel;
  }

  private void configureProfileFields() {
    firstNameField.setId("account-first-name");
    lastNameField.setId("account-last-name");
    profileEmailField.setId("account-email");
    profileEmailField.addClassName("account-profile-email");
    birthDatePicker.setId("account-birth-date");
    preferredLanguageSelector.setId("account-language");
    genderSelector.setId("account-gender");
    genderSelector.setItems(Gender.values());
    genderSelector.setItemLabelGenerator(this::genderLabel);
    genderSelector.setClearButtonVisible(true);
    firstNameField.setRequiredIndicatorVisible(true);
    lastNameField.setRequiredIndicatorVisible(true);
    birthDatePicker.setClearButtonVisible(true);
    preferredLanguageSelector.setRequiredIndicatorVisible(true);
    profileEmailField.setReadOnly(true);
    profileEmailField.setSuffixComponent(AppIcon.LOCK.create());
    preferredLanguageSelector.setItems(Locale.GERMAN, Locale.ENGLISH);
    preferredLanguageSelector.setItemLabelGenerator(this::languageLabel);
    preferredLanguageSelector.setAllowCustomValue(false);
    birthDatePicker.setAutoOpen(false);
    CompactDateInput.configureDatePickerInput(birthDatePicker, messageProperties);
  }

  private void populateProfileFields(UserEntity user) {
    if (user != null) {
      firstNameField.setValue(Objects.toString(user.getFirstName(), ""));
      lastNameField.setValue(Objects.toString(user.getLastName(), ""));
      profileEmailField.setValue(Objects.toString(user.getEmail(), ""));
      genderSelector.setValue(user.getGender());
      birthDatePicker.setValue(user.getBirthDate());
      if (user.getPreferredLocale() != null) {
        preferredLanguageSelector.setValue(
            Locale.GERMAN.getLanguage().equals(user.getPreferredLocale().getLanguage())
                ? Locale.GERMAN
                : Locale.ENGLISH);
      }
    }
  }

  private void registerProfileFieldTexts() {
    localizedTextUpdates.add(() -> {
      firstNameField.setLabel(messageProperties.getRegistrationFirstNameField());
      firstNameField.setPlaceholder(messageProperties.getAccountFirstNamePlaceholder());
      lastNameField.setLabel(messageProperties.getRegistrationLastNameField());
      lastNameField.setPlaceholder(messageProperties.getAccountLastNamePlaceholder());
      profileEmailField.setLabel(messageProperties.getAccountEmailAddressLabel());
      profileEmailField.setPlaceholder(messageProperties.getAccountEmailPlaceholder());
      birthDatePicker.setLabel(messageProperties.getRegistrationBirthDateField());
      birthDatePicker.setLocale(CustomI18NProvider.getCurrentLocale());
      CompactDateInput.configureDatePickerInput(birthDatePicker, messageProperties);
      birthDatePicker.setPlaceholder(messageProperties.getRegistrationBirthDatePlaceholder());
      birthDatePicker.setHelperText(messageProperties.getAccountBirthDateFormatHint());
      genderSelector.setLabel(messageProperties.getRegistrationGenderCombobox());
      genderSelector.setPlaceholder(messageProperties.getAccountGenderPlaceholder());
      genderSelector.setItemLabelGenerator(this::genderLabel);
      preferredLanguageSelector.setLabel(messageProperties.getAccountSettingsLanguage());
      preferredLanguageSelector.setHelperText(messageProperties.getAccountSettingsLanguageHint());
      // Vaadin aktualisiert dabei auch die separat gehaltene Beschriftung der Auswahl.
      preferredLanguageSelector.setItemLabelGenerator(this::languageLabel);
    });
  }

  private void saveProfile() {
    // Wie in der Registrierung erst beim Absenden prüfen, nicht beim ersten Tastendruck.
    birthDatePicker.setInvalid(false);
    if (!ValidationHelper.validateRequiredInputs(
        List.of(firstNameField, lastNameField, preferredLanguageSelector), messageProperties)) {
      return;
    }
    if (!ValidationHelper.validateBirthDateBeforeInput(birthDatePicker, messageProperties)) {
      return;
    }
    Profile saved;
    try {
      saved =
          persistProfile.apply(
              new Changes(
                  profileVersion,
                  firstNameField.getValue(),
                  lastNameField.getValue(),
                  birthDatePicker.getValue(),
                  preferredLanguageSelector.getValue(),
                  genderSelector.getValue()));
    } catch (OptimisticLockingFailureException _) {
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getBaseFailedTitle,
              messageProperties::getBaseUserUpdateConflict,
              -1,
              NotificationHelper.NotificationType.ERROR);
      return;
    } catch (RuntimeException failure) {
      log.warn("Kontoprofil konnte nicht gespeichert werden", safeFailure(failure));
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getBaseFailedTitle,
              messageProperties::getAccountSettingsSaveFailed,
              -1,
              NotificationHelper.NotificationType.ERROR);
      return;
    }
    profileVersion = saved.version();
    firstNameField.setValue(saved.firstName());
    lastNameField.setValue(saved.lastName());
    birthDatePicker.setValue(saved.birthDate());
    genderSelector.setValue(saved.gender());
    preferredLanguageSelector.setValue(saved.language());
    accountSidebar.refreshName(saved.firstName(), saved.lastName());
    profileSavedListeners.forEach(listener -> listener.accept(saved));
    NotificationHelper.getInstance()
        .showNotification(
            messageProperties::getBaseSuccessTitle,
            messageProperties::getAccountSettingsSaveSuccess,
            NotificationHelper.NotificationType.SUCCESS);
  }

  private Div actionRow(
      AppIcon icon, Supplier<String> title, Supplier<String> description, Button action) {
    Div symbol = new Div((icon == AppIcon.LOCK ? AppIcon.KEY : AppIcon.ENVELOPE).create());
    symbol.addClassName(PANEL_ICON);
    Span heading = withTextOnRefresh(new Span(), title);
    heading.addClassName("account-entry-title");
    Span detail = withTextOnRefresh(new Span(), description);
    detail.addClassName("account-entry-description");
    Div copy = new Div(heading, detail);
    copy.addClassName("account-entry-copy");
    Div row = new Div(symbol, copy, action);
    row.addClassName("account-entry");
    row.setClassName("account-email-security-entry", icon == AppIcon.ENVELOPE);
    return row;
  }

  private AccountPanelComponent createSecurityPanel(UserEntity user) {
    AccountPanelComponent panel =
        createAccountPanel(
            "security",
            AppIcon.LOCK,
            messageProperties::getAccountSettingsSecurityTitle,
            messageProperties::getAccountSettingsSecurityDescription,
            messageProperties::getAccountSettingsSecurityTab);
    panel.setIcon(AppIcon.LOCK.create());
    Div cards =
        new Div(
            actionRow(
                AppIcon.LOCK,
                messageProperties::getRegistrationPasswordField,
                messageProperties::getAccountSettingsPasswordHint,
                passwordChangeButton()),
            actionRow(
                AppIcon.ENVELOPE,
                messageProperties::getAccountEmailAddressLabel,
                () -> user == null ? "" : Objects.toString(user.getEmail(), ""),
                emailChangeButton("account-email-edit-security")));
    cards.addClassName("account-security-cards");
    portalSecurityCards = cards;
    Div hint =
        new Div(
            AppIcon.INFO_CIRCLE.create(),
            withTextOnRefresh(new Span(), messageProperties::getAccountSettingsAccountHint));
    hint.addClassNames("cosmos-panel-hint", "account-security-hint");
    Div portalAccess = new Div(cards, hint);
    portalAccess.setId("account-security-portal");
    providerAccessContent.setId("account-security-providers");
    Span unavailable =
        withTextOnRefresh(new Span(), messageProperties::getAccountSecurityNoProviders);
    unavailable.addClassNames("cosmos-panel-description", "account-security-empty");
    providerAccessContent.add(unavailable);
    Image logo = new Image("icons/service-logo.svg", "");
    logo.getElement().setAttribute(ARIA_HIDDEN, "true");
    portalAccessTab =
        new Tab(
            logo, withTextOnRefresh(new Span(), messageProperties::getAccountSecurityPortalAccess));
    portalAccessTab.setId("account-security-tab-portal");
    providerAccessTab =
        new Tab(
            AppIcon.CLOUD_LOCK.create(),
            withTextOnRefresh(new Span(), messageProperties::getProviderSettingsPanelTitle));
    providerAccessTab.setId("account-security-tab-providers");
    securityTabs.setId("account-security-tabs");
    securityTabs.setWidthFull();
    securityTabs.addThemeVariants(TabSheetVariant.NO_PADDING);
    securityTabs.add(portalAccessTab, portalAccess);
    securityTabs.add(providerAccessTab, providerAccessContent);
    securityTabs.addSelectedChangeListener(event -> {
      if (event.isFromClient()) {
        updateSecurityTabLocation();
      }
    });
    panel.getBody().add(securityTabs);
    return panel;
  }

  private SvgIcon devicesIcon() {
    SvgIcon devices = AppIcon.DEVICES.create();
    devices.addClassName("account-devices-icon");
    devices.getElement().setAttribute(ARIA_HIDDEN, "true");
    return devices;
  }

  public void configureSessionActions(
      SerializableSupplier<Snapshot> loader,
      SerializableConsumer<String> revoke,
      SerializableRunnable revokeOthers) {
    accountSessionsPanel.configure(loader, revoke, revokeOthers);
    accountSessionsPanel.refreshTexts();
  }

  public void configureTwoFactor(TwoFactorAccountComponent component) {
    twoFactorAccount = component;
    portalSecurityCards.add(component);
  }

  public void configureSecurityActions(
      SerializableFunction<PasswordChange, PasswordChanged> passwordAction,
      SerializableConsumer<EmailChange> emailAction) {
    passwordChangeDialog.setChangeAction(change -> {
      PasswordChanged changed = passwordAction.apply(change);
      // Nur den eigenen unveränderten Profilstand fortschreiben; fremde Änderungen bleiben
      // Konflikte.
      if (Objects.equals(profileVersion, changed.previousVersion())) {
        profileVersion = changed.version();
      }
      if (externalLoginSettings != null) {
        externalLoginSettings.refreshAccount();
      }
      return changed;
    });
    emailChangeDialog.setRequestAction(emailAction);
  }

  public void addProfileSavedListener(SerializableConsumer<Profile> listener) {
    profileSavedListeners.add(listener);
  }

  public void configureAvatars(UserAvatarService avatars, HttpServletRequest request) {
    accountSidebar.configureAvatars(avatars);
    avatarDialog.configure(avatars, request);
  }

  public void configureStatusSelection(Select<UserStatus> selection) {
    accountSidebar.configureStatusSelection(selection);
  }

  public void refreshSummaryName(String first, String last) {
    accountSidebar.refreshName(first, last);
  }

  public void updateUserStatus(UserStatus status) {
    accountSidebar.updateStatus(status);
  }

  private <T> void styleChoices(ComboBox<T> field, SerializableFunction<T, String> label) {
    PortalFields.styleChoice(field, label, true);
  }

  private String genderLabel(Gender value) {
    if (value == null) {
      return "";
    }
    return switch (value) {
      case MALE -> messageProperties.getGenderMale();
      case FEMALE -> messageProperties.getGenderFemale();
      case OTHER -> messageProperties.getGenderOther();
    };
  }

  private String languageLabel(Locale language) {
    return language.getDisplayLanguage(CustomI18NProvider.getCurrentLocale());
  }

  public void refreshTexts() {
    localizedTextUpdates.forEach(Runnable::run);
    genderSelector.getDataProvider().refreshAll();
    preferredLanguageSelector.getDataProvider().refreshAll();
    emailChangeDialog.refreshTexts();
    passwordChangeDialog.refreshTexts();
    avatarDialog.refreshTexts();
    accountSidebar.refreshTexts();
    accountSessionsPanel.refreshTexts();
    if (twoFactorAccount != null) {
      twoFactorAccount.refreshTwoFactorAccountSection();
    }
  }

  public void configureExternalLogins(
      ExternalConnectionsComponent connections,
      BooleanSupplier passwordChangeRequiresCurrentPassword,
      BooleanSupplier emailChangeRequiresCurrentPassword) {
    passwordChangeDialog.setCurrentPasswordRequired(passwordChangeRequiresCurrentPassword);
    emailChangeDialog.setCurrentPasswordRequired(emailChangeRequiresCurrentPassword);
    externalLoginSettings = connections;
    passwordChangeDialog.setDevelopmentWarning(connections::developmentExceptionActive);
    emailChangeDialog.setDevelopmentWarning(connections::developmentExceptionActive);
    // Der Bestätigungsdialog muss auch vom Portal-Tab aus bedienbar bleiben.
    add(connections.getConfirmationDialog());
    connections.configureAccountActions(
        passwordChangeDialog::open, emailChangeDialog::open, this::selectProvidersTabAndUpdateUrl);
    providerAccessContent.removeAll();
    providerAccessContent.add(connections);
  }
}
