package de.derpeterson.app.ui.components;

import static de.derpeterson.app.config.AppRouteConstants.ACCOUNT_LIFECYCLE_TAB;
import static de.derpeterson.app.config.AppRouteConstants.ACCOUNT_PORTAL_TAB;
import static de.derpeterson.app.config.AppRouteConstants.ACCOUNT_PROVIDERS_TAB;
import static de.derpeterson.app.config.AppRouteConstants.ACCOUNT_SECTION_PARAMETER;
import static de.derpeterson.app.config.AppRouteConstants.ACCOUNT_SECURITY_SECTION;
import static de.derpeterson.app.config.AppRouteConstants.ACCOUNT_TAB_PARAMETER;
import static de.derpeterson.app.helper.logging.LogSafety.safeFailure;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_ICON;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
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
public class AccountSettingsComponent extends PageContentLayout {
  private static final String ARIA_HIDDEN = "aria-hidden";

  public enum AccountSection {
    PROFILE,
    SECURITY,
    SESSIONS;

    public String urlValue() {
      return this == SECURITY ? ACCOUNT_SECURITY_SECTION : name().toLowerCase(Locale.ROOT);
    }

    public QueryParameters queryParameters() {
      QueryParameters sectionQueryParameters =
          QueryParameters.of(ACCOUNT_SECTION_PARAMETER, urlValue());
      return this == SECURITY
          ? sectionQueryParameters.merging(ACCOUNT_TAB_PARAMETER, ACCOUNT_PORTAL_TAB)
          : sectionQueryParameters;
    }
  }

  private final transient MessageProperties messageProperties;
  private final List<Runnable> localizedTextRefreshActions = new ArrayList<>();

  // Seitenrahmen und Navigation
  private final Span accountPageTitleSpan = new Span();
  private final Span accountPageDescriptionSpan = new Span();
  private final Div accountSectionContentDiv = new Div();
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
  private final SerializableFunction<Changes, Profile> persistProfileFunction;
  private Long profileVersion;
  private Changes savedProfileFields;
  private final List<SerializableConsumer<Profile>> profileSavedListeners = new ArrayList<>();

  // Sicherheit und Anmeldeanbieter
  private final TabSheet securityTabs = new TabSheet();
  private Tab portalAccessTab;
  private Tab providerAccessTab;
  private Tab lifecycleTab;
  private final Div providerAccessContentDiv = new Div();
  private Div portalSecurityCardsDiv;
  private final AccountEmailChangeDialog emailChangeDialog;
  private final AccountPasswordChangeDialog passwordChangeDialog;
  private ExternalConnectionsComponent externalLoginSettings;
  private TwoFactorAccountComponent twoFactorAccountComponent;
  private boolean portalPasswordConfigured;
  private final Badge passwordStatusBadge = new Badge();
  private Button passwordActionButton;
  private final boolean developmentAccount;

  // Kontoleiste und Sitzungen
  private final PortalSidebarComponent accountSidebar;
  private final AccountAvatarDialog avatarDialog;
  private final AccountSessionsComponent accountSessionsPanel;

  public AccountSettingsComponent(
      MessageProperties messageProperties,
      UserEntity accountUser,
      SerializableFunction<Changes, Profile> persistProfileFunction,
      int maxSessionsPerUser) {
    this.messageProperties = messageProperties;
    developmentAccount = accountUser != null && accountUser.isDevelopmentAccount();
    portalPasswordConfigured =
        accountUser != null
            && accountUser.getPassword() != null
            && !accountUser.getPassword().isBlank();
    profileEmailField.setPattern(UserInputRules.EMAIL_REGEX);
    accountSessionsPanel = new AccountSessionsComponent(messageProperties, maxSessionsPerUser);
    emailChangeDialog = new AccountEmailChangeDialog(messageProperties);
    passwordChangeDialog = new AccountPasswordChangeDialog(messageProperties);
    avatarDialog = new AccountAvatarDialog(messageProperties, accountUser);
    this.persistProfileFunction = persistProfileFunction;
    this.profileVersion = accountUser == null ? null : accountUser.getVersion();

    Span accountBreadcrumbSpan =
        withTextOnRefresh(new Span(), messageProperties::getAccountSettingsOverview);
    CosmosSectionHeaderComponent accountSectionHeader =
        new CosmosSectionHeaderComponent(
            accountBreadcrumbSpan, accountPageTitleSpan, accountPageDescriptionSpan);
    localizedTextRefreshActions.add(this::refreshSectionHeading);
    accountNavigation.setId("account-navigation");
    localizedTextRefreshActions.add(() ->
        accountNavigation
            .getElement()
            .setAttribute("aria-label", messageProperties.getAccountSettingsTitle()));

    AccountPanelComponent profilePanel = createProfilePanel(accountUser);
    addSectionNavigation(
        AccountSection.PROFILE,
        AppIcon.USER.create(),
        messageProperties::getAccountSettingsProfileTab,
        profilePanel);
    addSectionNavigation(
        AccountSection.SECURITY,
        AppIcon.LOCK.create(),
        messageProperties::getAccountSettingsSecurityTab,
        createSecurityPanel(accountUser));
    addSectionNavigation(
        AccountSection.SESSIONS,
        devicesIcon(),
        messageProperties::getAccountSettingsSessionsTab,
        accountSessionsPanel);

    accountSidebar =
        new PortalSidebarComponent(
            messageProperties,
            accountUser,
            AppRoute.ACCOUNT,
            accountNavigation,
            avatarDialog::open);
    accountSectionContentDiv.add(panelsBySection.values().toArray(AccountPanelComponent[]::new));
    setPageContent(accountSectionHeader, accountSidebar, accountSectionContentDiv);
    add(emailChangeDialog, passwordChangeDialog, avatarDialog);

    refreshTexts();
    selectSection(AccountSection.PROFILE);
  }

  /** Registriert den Text für den gesammelten Textaufbau und spätere Sprachwechsel. */
  private <T extends HasText> T withTextOnRefresh(
      T translatedComponent, Supplier<String> localizedTextSupplier) {
    localizedTextRefreshActions.add(() -> translatedComponent.setText(localizedTextSupplier.get()));
    return translatedComponent;
  }

  private Button createAccountActionButton(
      AppIcon actionAppIcon, Supplier<String> actionLabelSupplier, boolean primaryAction) {
    Button accountActionButton = withTextOnRefresh(new Button(), actionLabelSupplier);
    accountActionButton.setPrefixComponent(actionAppIcon.create());
    if (primaryAction) {
      accountActionButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    }

    return accountActionButton;
  }

  private Button createChangeEmailButton(String changeEmailButtonId) {
    Button changeEmailButton =
        createAccountActionButton(
            AppIcon.EDIT, messageProperties::getAccountSettingsChangeAction, false);
    changeEmailButton.setId(changeEmailButtonId);
    changeEmailButton.setEnabled(!developmentAccount);
    if (developmentAccount) {
      localizedTextRefreshActions.add(() ->
          changeEmailButton.setTooltipText(
              messageProperties.getDevelopmentAccountMailUnavailable()));
    }

    localizedTextRefreshActions.add(
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

  private Button createChangePasswordButton() {
    passwordActionButton = new Button();
    passwordActionButton.setPrefixComponent(AppIcon.EDIT.create());
    passwordActionButton.setId("account-password-edit-security");
    passwordActionButton.addClickListener(event -> startPasswordChange());

    return passwordActionButton;
  }

  private Badge createPasswordStatusBadge() {
    passwordStatusBadge.setId("account-password-status");
    passwordStatusBadge.addClassName("account-password-status");
    localizedTextRefreshActions.add(this::refreshPasswordConfiguration);
    refreshPasswordConfiguration();
    return passwordStatusBadge;
  }

  private void refreshPasswordConfiguration() {
    passwordStatusBadge.setText(
        portalPasswordConfigured
            ? messageProperties.getAccountPasswordConfigured()
            : messageProperties.getAccountPasswordNotConfigured());
    if (portalPasswordConfigured) {
      passwordStatusBadge.addThemeVariants(BadgeVariant.SUCCESS);
    } else {
      passwordStatusBadge.removeThemeVariants(BadgeVariant.SUCCESS);
    }

    passwordActionButton.setText(
        portalPasswordConfigured
            ? messageProperties.getAccountSettingsChangeAction()
            : messageProperties.getAccountPasswordSetupAction());
    passwordActionButton.setAriaLabel(
        portalPasswordConfigured
            ? messageProperties.getAccountSettingsChangePassword()
            : messageProperties.getAccountPasswordSetupAction());
  }

  private void startPasswordChange() {
    if (externalLoginSettings == null) {
      passwordChangeDialog.open();
    } else {
      externalLoginSettings.requestPasswordChange();
    }
  }

  private AccountPanelComponent createAccountPanel(
      String sectionId,
      AppIcon panelAppIcon,
      Supplier<String> panelTitleSupplier,
      Supplier<String> panelDescriptionSupplier,
      Supplier<String> panelMetadataSupplier) {
    AccountPanelComponent accountPanel =
        new AccountPanelComponent("account-panel-" + sectionId, panelAppIcon);

    localizedTextRefreshActions.add(() ->
        accountPanel.setHeading(
            panelTitleSupplier.get(), panelDescriptionSupplier.get(), panelMetadataSupplier.get()));
    return accountPanel;
  }

  private void addSectionNavigation(
      AccountSection accountSection,
      Component navigationIconComponent,
      Supplier<String> sectionLabelSupplier,
      AccountPanelComponent sectionPanel) {
    String sectionId = accountSection.urlValue();
    navigationIconComponent.getElement().setAttribute(ARIA_HIDDEN, "true");
    SideNavItem sectionNavItem =
        new SideNavItem("", AppRoute.ACCOUNT.getSecurityRoute(), navigationIconComponent);
    sectionNavItem.setId("account-tab-" + sectionId);
    sectionNavItem.setQueryParameters(accountSection.queryParameters());
    localizedTextRefreshActions.add(() -> sectionNavItem.setLabel(sectionLabelSupplier.get()));

    sectionNavItem.getElement().setAttribute("aria-controls", "account-panel-" + sectionId);
    sectionPanel.getElement().setAttribute("role", "region");
    sectionPanel.getElement().setAttribute("aria-labelledby", "account-tab-" + sectionId);

    panelsBySection.put(accountSection, sectionPanel);
    navigationItemsBySection.put(accountSection, sectionNavItem);
    accountNavigation.registerAction(
        sectionNavItem, () -> navigateToAccountSection(accountSection));
    accountNavigation.addItem(sectionNavItem);
  }

  private void navigateToAccountSection(AccountSection accountSection) {
    if (accountSection == AccountSection.SECURITY && selectedSection == accountSection) {
      selectSecurityTab(ACCOUNT_PORTAL_TAB);
      updateSecurityTabLocation();
      closeNavigation();
      return;
    }

    closeNavigation();
    if (sectionChangeListeners.isEmpty()) {
      selectSection(accountSection);
      return;
    }
    sectionChangeListeners.forEach(
        sectionChangeListener -> sectionChangeListener.accept(accountSection));
  }

  public AccountSection getSelectedSection() {
    return selectedSection;
  }

  public void addSectionChangeListener(SerializableConsumer<AccountSection> sectionChangeListener) {
    sectionChangeListeners.add(sectionChangeListener);
  }

  public void selectSection(AccountSection accountSection) {
    // URL-Wiederherstellung darf keinen weiteren Historieneintrag auslösen.
    selectedSection = Objects.requireNonNull(accountSection);
    navigationItemsBySection.forEach((itemSection, sectionNavItem) ->
        PageNavigationComponent.setItemActive(sectionNavItem, itemSection == accountSection));

    showSelectedAccountPanel();
    if (getSelectedSection() == AccountSection.SESSIONS) {
      accountSessionsPanel.reloadSessionSnapshot();
    }

    refreshSectionHeading();
  }

  public void selectSecurityTab(String securityTabParameter) {
    Tab requestedSecurityTab =
        switch (securityTabParameter) {
          case ACCOUNT_PROVIDERS_TAB -> providerAccessTab;
          case ACCOUNT_LIFECYCLE_TAB -> lifecycleTab == null ? portalAccessTab : lifecycleTab;
          case null, default -> portalAccessTab;
        };
    securityTabs.setSelectedTab(requestedSecurityTab);
  }

  private void updateSecurityTabLocation() {
    if (selectedSection != AccountSection.SECURITY) {
      return;
    }

    Tab selectedSecurityTab = securityTabs.getSelectedTab();
    String securityTabValue = ACCOUNT_PORTAL_TAB;
    if (selectedSecurityTab == providerAccessTab) {
      securityTabValue = ACCOUNT_PROVIDERS_TAB;
    } else if (selectedSecurityTab == lifecycleTab) {
      securityTabValue = ACCOUNT_LIFECYCLE_TAB;
    }
    QueryParameters securityQueryParameters =
        AccountSection.SECURITY.queryParameters().merging(ACCOUNT_TAB_PARAMETER, securityTabValue);

    getUI()
        .ifPresent(ui ->
            ui.getPage()
                .getHistory()
                .replaceState(
                    null, new Location(AppRouteConstants.ACCOUNT_ROUTE, securityQueryParameters)));
  }

  private void selectProvidersTabAndUpdateUrl() {
    if (securityTabs.getSelectedTab() != providerAccessTab) {
      selectSecurityTab(ACCOUNT_PROVIDERS_TAB);
      updateSecurityTabLocation();
    }
  }

  private void showSelectedAccountPanel() {
    panelsBySection.forEach(
        (panelSection, sectionPanel) -> sectionPanel.setVisible(panelSection == selectedSection));
  }

  private void refreshSectionHeading() {
    AccountSection selectedAccountSection = getSelectedSection();
    accountPageTitleSpan.setText(
        switch (selectedAccountSection) {
          case PROFILE -> messageProperties.getAccountSettingsProfileTitle();
          case SECURITY -> messageProperties.getAccountSettingsSecurityTab();
          case SESSIONS -> messageProperties.getAccountSettingsSessionsTab();
        });
    accountPageDescriptionSpan.setText(
        switch (selectedAccountSection) {
          case PROFILE -> messageProperties.getAccountSettingsDescription();
          case SECURITY -> messageProperties.getAccountSettingsSecurityIntroduction();
          case SESSIONS -> messageProperties.getAccountSettingsSessionsIntroduction();
        });
  }

  public void updateUnreadCount(long unreadNotificationCount) {
    accountSidebar.updateUnreadCount(unreadNotificationCount);
  }

  private AccountPanelComponent createProfilePanel(UserEntity accountUser) {
    AccountPanelComponent profilePanel =
        createAccountPanel(
            "profile",
            AppIcon.USER,
            messageProperties::getAccountSettingsProfileTitle,
            messageProperties::getAccountSettingsProfileDescription,
            () -> "");
    profilePanel.setIcon(AppIcon.USER.create());

    configureProfileFields();
    populateProfileFields(accountUser);
    savedProfileFields = readProfileChanges(null);
    localizedTextRefreshActions.add(this::refreshProfileFieldTexts);

    styleChoices(genderSelector, this::genderLabel);
    styleChoices(preferredLanguageSelector, this::languageLabel);

    Div profileEmailGroupDiv = new Div(profileEmailField);
    profileEmailGroupDiv.addClassName("account-profile-email-group");
    Span readOnlyEmailSpan =
        withTextOnRefresh(new Span(), messageProperties::getAccountReadOnlyLabel);
    readOnlyEmailSpan.addClassName("account-readonly-label");
    profileEmailGroupDiv.add(readOnlyEmailSpan);

    Div profileFormDiv =
        new Div(
            firstNameField,
            lastNameField,
            profileEmailGroupDiv,
            genderSelector,
            birthDatePicker,
            preferredLanguageSelector);
    profileFormDiv.addClassName("account-profile-form");
    profilePanel.getBody().add(profileFormDiv);

    Button saveProfileButton =
        createAccountActionButton(AppIcon.CHECK, messageProperties::getAccountSettingsSave, true);
    saveProfileButton.setId("account-save");
    saveProfileButton.addClickListener(event -> saveProfile());
    profilePanel.addActionFooter(saveProfileButton);
    return profilePanel;
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

  private void populateProfileFields(UserEntity accountUser) {
    if (accountUser == null) {
      return;
    }

    firstNameField.setValue(Objects.toString(accountUser.getFirstName(), ""));
    lastNameField.setValue(Objects.toString(accountUser.getLastName(), ""));
    profileEmailField.setValue(Objects.toString(accountUser.getEmail(), ""));
    genderSelector.setValue(accountUser.getGender());
    birthDatePicker.setValue(accountUser.getBirthDate());

    if (accountUser.getPreferredLocale() != null) {
      preferredLanguageSelector.setValue(
          Locale.GERMAN.getLanguage().equals(accountUser.getPreferredLocale().getLanguage())
              ? Locale.GERMAN
              : Locale.ENGLISH);
    }
  }

  private void refreshProfileFieldTexts() {
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

    Profile savedProfile;
    try {
      savedProfile = persistProfileFunction.apply(readProfileChanges(profileVersion));
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

    applySavedProfile(savedProfile);
    profileSavedListeners.forEach(
        profileSavedListener -> profileSavedListener.accept(savedProfile));

    NotificationHelper.getInstance()
        .showNotification(
            messageProperties::getBaseSuccessTitle,
            messageProperties::getAccountSettingsSaveSuccess,
            NotificationHelper.NotificationType.SUCCESS);
  }

  private void applySavedProfile(Profile savedProfile) {
    profileVersion = savedProfile.version();
    firstNameField.setValue(savedProfile.firstName());
    lastNameField.setValue(savedProfile.lastName());
    birthDatePicker.setValue(savedProfile.birthDate());
    genderSelector.setValue(savedProfile.gender());
    preferredLanguageSelector.setValue(savedProfile.language());
    savedProfileFields = readProfileChanges(null);
    accountSidebar.refreshName(savedProfile.firstName(), savedProfile.lastName());
  }

  private Changes readProfileChanges(Long expectedProfileVersion) {
    return new Changes(
        expectedProfileVersion,
        firstNameField.getValue(),
        lastNameField.getValue(),
        birthDatePicker.getValue(),
        preferredLanguageSelector.getValue(),
        genderSelector.getValue());
  }

  public boolean hasUnsavedProfileChanges() {
    return selectedSection == AccountSection.PROFILE
        && (!readProfileChanges(null).equals(savedProfileFields) || birthDatePicker.isInvalid());
  }

  public void discardProfileChanges() {
    firstNameField.setValue(savedProfileFields.firstName());
    lastNameField.setValue(savedProfileFields.lastName());
    birthDatePicker.setValue(savedProfileFields.birthDate());
    preferredLanguageSelector.setValue(savedProfileFields.language());
    genderSelector.setValue(savedProfileFields.gender());

    firstNameField.setInvalid(false);
    lastNameField.setInvalid(false);
    birthDatePicker.setInvalid(false);
    preferredLanguageSelector.setInvalid(false);
  }

  private Div createSecurityActionRow(
      AppIcon rowAppIcon,
      Supplier<String> rowTitleSupplier,
      Supplier<String> rowDescriptionSupplier,
      Button rowActionButton,
      Component rowStatusComponent) {
    Div rowIconDiv = new Div(rowAppIcon.create());
    rowIconDiv.addClassName(PANEL_ICON);

    Span rowTitleSpan = withTextOnRefresh(new Span(), rowTitleSupplier);
    rowTitleSpan.addClassName("account-entry-title");
    Span rowDescriptionSpan = withTextOnRefresh(new Span(), rowDescriptionSupplier);
    rowDescriptionSpan.addClassName("account-entry-description");

    HorizontalLayout rowHeadingLayout = new HorizontalLayout(rowTitleSpan);
    rowHeadingLayout.setSpacing(false);
    rowHeadingLayout.setAlignItems(Alignment.CENTER);
    rowHeadingLayout.addClassName("account-security-heading");
    if (rowStatusComponent != null) {
      rowHeadingLayout.add(rowStatusComponent);
    }

    Div rowContentDiv = new Div(rowHeadingLayout, rowDescriptionSpan);
    rowContentDiv.addClassName("account-entry-copy");

    Div accountActionRowDiv = new Div(rowIconDiv, rowContentDiv, rowActionButton);
    accountActionRowDiv.addClassName("account-entry");
    accountActionRowDiv.setClassName(
        "account-email-security-entry", rowAppIcon == AppIcon.ENVELOPE);

    return accountActionRowDiv;
  }

  private AccountPanelComponent createSecurityPanel(UserEntity accountUser) {
    AccountPanelComponent securityPanel =
        createAccountPanel(
            "security",
            AppIcon.LOCK,
            messageProperties::getAccountSettingsSecurityTitle,
            messageProperties::getAccountSettingsSecurityDescription,
            messageProperties::getAccountSettingsSecurityTab);
    securityPanel.setIcon(AppIcon.LOCK.create());

    Div portalAccessDiv = createPortalAccessContent(accountUser);
    configureSecurityTabSheet(portalAccessDiv);
    securityPanel.getBody().add(securityTabs);
    return securityPanel;
  }

  private Div createPortalAccessContent(UserEntity accountUser) {
    Div securityCardsDiv =
        new Div(
            createSecurityActionRow(
                AppIcon.KEY,
                messageProperties::getRegistrationPasswordField,
                messageProperties::getAccountSettingsPasswordHint,
                createChangePasswordButton(),
                createPasswordStatusBadge()),
            createSecurityActionRow(
                AppIcon.ENVELOPE,
                messageProperties::getAccountEmailAddressLabel,
                () -> accountUser == null ? "" : Objects.toString(accountUser.getEmail(), ""),
                createChangeEmailButton("account-email-edit-security"),
                withTextOnRefresh(
                    new EmailVerificationBadge(
                        messageProperties, accountUser.getEmailVerificationStatus()),
                    () ->
                        switch (accountUser.getEmailVerificationStatus()) {
                          case VERIFIED -> messageProperties.getEmailVerified();
                          case PENDING -> messageProperties.getEmailVerificationPending();
                          case NOT_CHECKED -> messageProperties.getEmailNotChecked();
                        })));
    securityCardsDiv.addClassName("account-security-cards");
    portalSecurityCardsDiv = securityCardsDiv;

    Div securityHintDiv =
        new Div(
            AppIcon.INFO_CIRCLE.create(),
            withTextOnRefresh(new Span(), messageProperties::getAccountSettingsAccountHint));
    securityHintDiv.addClassNames("cosmos-panel-hint", "account-security-hint");

    Div portalAccessDiv = new Div(securityCardsDiv, securityHintDiv);
    portalAccessDiv.setId("account-security-portal");
    return portalAccessDiv;
  }

  private void configureSecurityTabSheet(Div portalAccessDiv) {
    providerAccessContentDiv.setId("account-security-providers");
    Span providerUnavailableSpan =
        withTextOnRefresh(new Span(), messageProperties::getAccountSecurityNoProviders);
    providerUnavailableSpan.addClassNames("cosmos-panel-description", "account-security-empty");
    providerAccessContentDiv.add(providerUnavailableSpan);

    Image portalLogoImage = new Image("icons/service-logo.svg", "");
    portalLogoImage.getElement().setAttribute(ARIA_HIDDEN, "true");
    portalAccessTab =
        new Tab(
            portalLogoImage,
            withTextOnRefresh(new Span(), messageProperties::getAccountSecurityPortalAccess));
    portalAccessTab.setId("account-security-tab-portal");
    providerAccessTab =
        new Tab(
            AppIcon.CLOUD_LOCK.create(),
            withTextOnRefresh(new Span(), messageProperties::getProviderSettingsPanelTitle));
    providerAccessTab.setId("account-security-tab-providers");

    securityTabs.setId("account-security-tabs");
    securityTabs.setWidthFull();
    securityTabs.addThemeVariants(TabSheetVariant.NO_PADDING);
    securityTabs.add(portalAccessTab, portalAccessDiv);
    securityTabs.add(providerAccessTab, providerAccessContentDiv);
    securityTabs.addSelectedChangeListener(event -> {
      if (event.isFromClient()) {
        updateSecurityTabLocation();
      }
    });
  }

  private SvgIcon devicesIcon() {
    SvgIcon devicesSvgIcon = AppIcon.DEVICES.create();
    devicesSvgIcon.addClassName("account-devices-icon");
    devicesSvgIcon.getElement().setAttribute(ARIA_HIDDEN, "true");
    return devicesSvgIcon;
  }

  public void addAccountLifecycleTab(AccountLifecycleComponent accountLifecycleComponent) {
    // Die nachträgliche Ergänzung liegt hinter dem ersten gesammelten Textaufbau.
    lifecycleTab =
        new Tab(
            AppIcon.LOCK.create(),
            withTextOnRefresh(
                new Span(messageProperties.getAccountLifecycleTab()),
                messageProperties::getAccountLifecycleTab));
    lifecycleTab.setId("account-security-tab-lifecycle");
    securityTabs.add(lifecycleTab, accountLifecycleComponent);
    localizedTextRefreshActions.add(accountLifecycleComponent::refreshLifecycleTexts);
  }

  public void configureSessionActions(
      SerializableSupplier<Snapshot> sessionSnapshotSupplier,
      SerializableConsumer<String> revokeSessionAction,
      SerializableRunnable revokeOtherSessionsAction) {
    accountSessionsPanel.configure(
        sessionSnapshotSupplier, revokeSessionAction, revokeOtherSessionsAction);
    accountSessionsPanel.refreshTexts();
  }

  public void configureTwoFactor(TwoFactorAccountComponent accountTwoFactorComponent) {
    twoFactorAccountComponent = accountTwoFactorComponent;
    portalSecurityCardsDiv.add(accountTwoFactorComponent);
  }

  public void configureSecurityActions(
      SerializableFunction<PasswordChange, PasswordChanged> changePasswordFunction,
      SerializableConsumer<EmailChange> requestEmailChangeAction) {
    passwordChangeDialog.setChangeAction(passwordChange -> {
      PasswordChanged passwordChangeResult = changePasswordFunction.apply(passwordChange);

      portalPasswordConfigured = true;
      refreshPasswordConfiguration();

      // Nur den eigenen unveränderten Profilstand fortschreiben; fremde Änderungen bleiben
      // Konflikte.
      if (Objects.equals(profileVersion, passwordChangeResult.previousVersion())) {
        profileVersion = passwordChangeResult.version();
      }

      if (externalLoginSettings != null) {
        externalLoginSettings.refreshAccount();
      }
      return passwordChangeResult;
    });
    emailChangeDialog.setRequestAction(requestEmailChangeAction);
  }

  public void addProfileSavedListener(SerializableConsumer<Profile> profileSavedListener) {
    profileSavedListeners.add(profileSavedListener);
  }

  public void configureAvatars(
      UserAvatarService userAvatarService, HttpServletRequest httpServletRequest) {
    accountSidebar.configureAvatars(userAvatarService);
    avatarDialog.configure(userAvatarService, httpServletRequest);
  }

  public void configureStatusSelection(Select<UserStatus> userStatusSelect) {
    accountSidebar.configureStatusSelection(userStatusSelect);
  }

  public void refreshSummaryName(String firstName, String lastName) {
    accountSidebar.refreshName(firstName, lastName);
  }

  public void updateUserStatus(UserStatus status) {
    accountSidebar.updateStatus(status);
  }

  private <T> void styleChoices(
      ComboBox<T> profileChoiceComboBox, SerializableFunction<T, String> choiceLabelFunction) {
    CosmosComboBoxStyling.styleChoice(profileChoiceComboBox, choiceLabelFunction, false);
  }

  private String genderLabel(Gender gender) {
    if (gender == null) {
      return "";
    }

    return switch (gender) {
      case MALE -> messageProperties.getGenderMale();
      case FEMALE -> messageProperties.getGenderFemale();
      case OTHER -> messageProperties.getGenderOther();
    };
  }

  private String languageLabel(Locale language) {
    return language.getDisplayLanguage(CustomI18NProvider.getCurrentLocale());
  }

  public void refreshTexts() {
    localizedTextRefreshActions.forEach(Runnable::run);

    genderSelector.getDataProvider().refreshAll();
    preferredLanguageSelector.getDataProvider().refreshAll();

    emailChangeDialog.refreshTexts();
    passwordChangeDialog.refreshTexts();
    avatarDialog.refreshTexts();

    accountSidebar.refreshTexts();
    accountSessionsPanel.refreshTexts();
    if (twoFactorAccountComponent != null) {
      twoFactorAccountComponent.refreshTwoFactorAccountSection();
    }
  }

  public void configureExternalLogins(
      ExternalConnectionsComponent externalConnectionsComponent,
      BooleanSupplier passwordChangeRequiresCurrentPassword,
      BooleanSupplier emailChangeRequiresCurrentPassword) {
    passwordChangeDialog.setCurrentPasswordRequired(passwordChangeRequiresCurrentPassword);
    emailChangeDialog.setCurrentPasswordRequired(emailChangeRequiresCurrentPassword);
    externalLoginSettings = externalConnectionsComponent;
    passwordChangeDialog.setDevelopmentWarning(
        externalConnectionsComponent::developmentExceptionActive);
    emailChangeDialog.setDevelopmentWarning(
        externalConnectionsComponent::developmentExceptionActive);

    // Der Bestätigungsdialog muss auch vom Portal-Tab aus bedienbar bleiben.
    add(externalConnectionsComponent.getConfirmationDialog());
    externalConnectionsComponent.configureAccountActions(
        passwordChangeDialog::open, emailChangeDialog::open, this::selectProvidersTabAndUpdateUrl);

    providerAccessContentDiv.removeAll();
    providerAccessContentDiv.add(externalConnectionsComponent);
  }
}
