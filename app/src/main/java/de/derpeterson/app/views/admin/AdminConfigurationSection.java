package de.derpeterson.app.views.admin;

import static de.derpeterson.app.helper.logging.LogSafety.safeFailure;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasAriaLabel;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Switch;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.config.AdminConfigGroup;
import de.derpeterson.app.config.ConfigurationCatalog;
import de.derpeterson.app.config.TokenDurationPolicy;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.ui.components.AdminSidePanelDialog;
import de.derpeterson.app.ui.components.AppIcon;
import de.derpeterson.app.ui.components.BrowserUnsavedChangesGuard;
import de.derpeterson.app.ui.components.ConfigGroupIcons;
import de.derpeterson.app.ui.components.ConfigInfoButton;
import de.derpeterson.app.ui.components.CosmosBreadcrumbsComponent.ParentPath;
import de.derpeterson.app.ui.components.CosmosCardComponent;
import de.derpeterson.app.ui.components.CosmosDurationField;
import de.derpeterson.app.ui.components.CosmosSectionHeaderComponent;
import de.derpeterson.app.ui.components.UnsavedChangesDialog;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.convert.DurationStyle;

public class AdminConfigurationSection extends VerticalLayout {

  private static final Logger logger = LoggerFactory.getLogger(AdminConfigurationSection.class);

  private static final int SERVICE_NAME_MAX_LENGTH = 30;
  private static final int SECRET_KEY_MIN_LENGTH = 8;
  // Nur eine oder mehrere ASCII-Ziffern, ohne Vorzeichen/Einheit/Leerzeichen; ^/$ begrenzen die
  // ganze Eingabe.
  private static final Pattern DURATION_NUMBER_ONLY_PATTERN = Pattern.compile("^\\d+$");

  // Bestehende SMTP-Adressregel: vor/nach @ und nach einem Domainpunkt nichtleer.
  // ^/$ begrenzen alles; ausgeschlossen sind @, Leerzeichen und ASCII-Steuerraum 09–0D.
  // Die expliziten Hexbereiche erhalten Javas bisherige ASCII-\s-Regel auch in JavaScript,
  // dessen \s zusätzlich Unicode-Leerraum erfassen würde. Nach dem ersten Domainzeichen
  // führt die punktfreie Wiederholung zum ersten weiteren Punkt; der Rest darf Punkte enthalten.
  // Flache Wiederholungen und eindeutige Trenner vermeiden verschachteltes Backtracking.
  private static final String SMTP_EMAIL_REGEX =
      "^[^\\x09-\\x0D @]+@[^\\x09-\\x0D @][^\\x09-\\x0D @.]*\\.[^\\x09-\\x0D @]+$";
  private static final Pattern SMTP_EMAIL_PATTERN = Pattern.compile(SMTP_EMAIL_REGEX);

  private static final String HELP_DEFAULT_CLASS = "admin-help-default";

  private static final Set<ConfigEntry> DURATION_ENTRIES =
      Set.of(
          ConfigEntry.USER_AUTO_ABSENT_TIMEOUT,
          ConfigEntry.VERIFICATION_TOKEN_VALID_DURATION,
          ConfigEntry.VERIFICATION_TOKEN_LIVE_DURATION,
          ConfigEntry.PASSWORD_RESET_TOKEN_VALID_DURATION,
          ConfigEntry.PASSWORD_RESET_TOKEN_LIVE_DURATION,
          ConfigEntry.EMAIL_QUEUE_SENT_LIVE_DURATION);

  private final transient MessageProperties messageProperties;
  private final BrowserUnsavedChangesGuard browserChangesGuard =
      new BrowserUnsavedChangesGuard(this, this::hasUnsavedChanges);
  private final transient ConfigService configService;
  private final transient Runnable configurationChangedAction;

  private final EnumMap<ConfigEntry, Component> configInputs = new EnumMap<>(ConfigEntry.class);
  private final EnumMap<ConfigEntry, String> loadedConfigValues = new EnumMap<>(ConfigEntry.class);
  private final EnumMap<ConfigEntry, VerticalLayout> configInputWrappers =
      new EnumMap<>(ConfigEntry.class);
  private final EnumMap<ConfigEntry, Span> configLabelSpans = new EnumMap<>(ConfigEntry.class);
  private final EnumMap<ConfigEntry, ConfigInfoButton> configInfoButtons =
      new EnumMap<>(ConfigEntry.class);
  private final Map<AdminConfigGroup, Predicate<ConfigEntry>> configGroupFilters =
      new LinkedHashMap<>();

  private final Span configurationTitleSpan;
  private final Span configurationDescriptionSpan;
  private final H2 currentGroupTitle;
  private final SvgIcon currentGroupSvgIcon;
  private final Span currentGroupDescriptionSpan;
  private final Badge currentGroupSettingCountBadge;
  private final Span configurationSaveHintSpan;
  private final VerticalLayout configFieldsLayout;

  private final Button saveConfigButton;
  private final Button reloadConfigButton;

  private final AdminSidePanelDialog helpPanel;
  private ConfigEntry openHelpConfigEntry;
  private AdminConfigGroup selectedGroup = null;

  public AdminConfigurationSection(
      MessageProperties messageProperties,
      ConfigService configService,
      Runnable configurationChangedAction) {
    this.messageProperties = messageProperties;
    this.configService = configService;
    this.configurationChangedAction = configurationChangedAction;

    setPadding(false);
    setSpacing(false);
    setWidthFull();
    setAlignItems(FlexComponent.Alignment.CENTER);
    addClassNames("cosmos-admin-section", "cosmos-admin-section--config");

    helpPanel = new AdminSidePanelDialog(messageProperties, AppIcon.INFO_CIRCLE);
    helpPanel.addClassName("admin-config-help");
    helpPanel.setId("admin-config-help");
    add(helpPanel);

    configurationTitleSpan = new Span();

    configurationDescriptionSpan = new Span();
    configurationDescriptionSpan.addClassNames(LumoUtility.TextColor.SECONDARY);

    currentGroupTitle = new H2();
    currentGroupTitle.addClassName("cosmos-config-group-title");
    currentGroupSvgIcon = AppIcon.COG.create();
    currentGroupSvgIcon.getElement().setAttribute("aria-hidden", "true");
    currentGroupDescriptionSpan = new Span();
    currentGroupDescriptionSpan.addClassName("cosmos-config-group-description");
    currentGroupSettingCountBadge = new Badge();
    currentGroupSettingCountBadge.addClassName("cosmos-config-setting-count");
    configurationSaveHintSpan = new Span();

    configFieldsLayout = new VerticalLayout();
    configFieldsLayout.setPadding(false);
    configFieldsLayout.setSpacing(true);
    configFieldsLayout.setWidthFull();
    configFieldsLayout.addClassName("cosmos-config-fields");

    registerConfigGroups();

    for (ConfigEntry configEntry : ConfigEntry.values()) {
      configInputWrappers.put(configEntry, createConfigInput(configEntry));
      browserChangesGuard.observeFields(configInputs.get(configEntry));
    }

    saveConfigButton = new Button();
    saveConfigButton.setPrefixComponent(AppIcon.CHECK.create());
    saveConfigButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    saveConfigButton.addClickListener(clickEvent -> saveConfigValues());
    // Enter in der Navigation oder an einem Info-Button darf keine Werte speichern.
    saveConfigButton
        .addClickShortcut(Key.ENTER)
        .listenOn(configInputs.values().toArray(Component[]::new))
        .resetFocusOnActiveElement();

    reloadConfigButton = new Button();
    reloadConfigButton.setPrefixComponent(AppIcon.REFRESH.create());
    reloadConfigButton.addClickListener(clickEvent -> requestConfigReload());

    VerticalLayout configLayout = createConfigurationCardContent();

    CosmosCardComponent configCard = new CosmosCardComponent(configLayout);
    configCard.addClassNames("cosmos-admin-form-card", "cosmos-config-card");
    add(
        new CosmosSectionHeaderComponent(
            messageProperties,
            ParentPath.CONFIGURATION,
            configurationTitleSpan,
            configurationDescriptionSpan),
        configCard);

    if (!configGroupFilters.isEmpty()) {
      selectedGroup = configGroupFilters.keySet().iterator().next();
    }

    refreshTexts();
    renderSelectedGroup();
    loadConfigValues();
  }

  private @NonNull VerticalLayout createConfigurationCardContent() {
    Div configGroupIconBadgeDiv = new Div(currentGroupSvgIcon);
    configGroupIconBadgeDiv.addClassName("cosmos-config-group-icon");
    Div configGroupHeadingDiv = new Div(currentGroupTitle, currentGroupDescriptionSpan);
    configGroupHeadingDiv.addClassName("cosmos-config-group-heading");
    Div configCardHeaderDiv =
        new Div(configGroupIconBadgeDiv, configGroupHeadingDiv, currentGroupSettingCountBadge);
    configCardHeaderDiv.addClassName("cosmos-config-card-header");

    SvgIcon saveHintSvgIcon = AppIcon.INFO_CIRCLE.create();
    saveHintSvgIcon.getElement().setAttribute("aria-hidden", "true");
    Div saveHintDiv = new Div(saveHintSvgIcon, configurationSaveHintSpan);
    saveHintDiv.addClassName("cosmos-config-save-hint");
    Div configActionsDiv = new Div(reloadConfigButton, saveConfigButton);
    configActionsDiv.addClassName("cosmos-config-card-actions");
    Div configCardFooterDiv = new Div(saveHintDiv, configActionsDiv);
    configCardFooterDiv.addClassName("cosmos-config-card-footer");

    VerticalLayout configCardLayout =
        new VerticalLayout(configCardHeaderDiv, configFieldsLayout, configCardFooterDiv);
    configCardLayout.setPadding(false);
    configCardLayout.setSpacing(false);
    configCardLayout.addClassNames("cosmos-admin-form", "cosmos-config-form");
    configCardLayout.setAlignItems(Alignment.START);
    return configCardLayout;
  }

  public void refreshTexts() {
    configurationDescriptionSpan.setText(messageProperties.getAdminConfigDescription());
    saveConfigButton.setText(messageProperties.getAdminConfigSave());
    reloadConfigButton.setText(messageProperties.getAdminConfigReload());
    configurationSaveHintSpan.setText(messageProperties.getAdminConfigSaveHint());

    if (openHelpConfigEntry != null && helpPanel.isOpened()) {
      openHelp(openHelpConfigEntry);
    }

    refreshInputLabels();
    refreshInputHelp();
    refreshCurrentGroupTitle();
    refreshValidationMessagesForInvalidFields();
  }

  public void loadConfigValues() {
    loadedConfigValues.clear();
    for (ConfigEntry configEntry : getVisibleConfigEntries()) {
      Component configInputComponent = configInputs.get(configEntry);
      if (configInputComponent == null) {
        continue;
      }

      String storedConfigValue = getSafeConfigValue(configEntry);
      applyConfigValue(configInputComponent, configEntry, storedConfigValue);
      loadedConfigValues.put(configEntry, extractInputValue(configEntry));
    }

    clearValidationStatesForVisibleInputs();
    browserChangesGuard.refresh();
  }

  public boolean hasUnsavedChanges() {
    return loadedConfigValues.entrySet().stream()
        .anyMatch(loadedConfigEntry ->
            !loadedConfigEntry.getValue().equals(extractInputValue(loadedConfigEntry.getKey())));
  }

  private String getSafeConfigValue(ConfigEntry configEntry) {
    String storedConfigValue = configService.getForAdministration(configEntry);
    return storedConfigValue == null ? "" : storedConfigValue;
  }

  private void applyConfigValue(
      Component configInputComponent, ConfigEntry configEntry, String storedConfigValue) {
    switch (configInputComponent) {
      case Switch settingSwitch -> settingSwitch.setValue(Boolean.parseBoolean(storedConfigValue));
      case CosmosDurationField durationField -> {
        try {
          durationField.setStoredValue(storedConfigValue);
        } catch (IllegalArgumentException | ArithmeticException _) {
          durationField.setStoredValue(configuredDefault(configEntry));
        }
      }
      case IntegerField integerField ->
          setIntegerFieldValue(integerField, configEntry, storedConfigValue);
      case PasswordField passwordField -> passwordField.setValue(storedConfigValue);
      case EmailField emailField -> emailField.setValue(storedConfigValue);
      case TextField textField -> textField.setValue(storedConfigValue);
      default -> {
        // no-op
      }
    }
  }

  private void setIntegerFieldValue(
      IntegerField integerField, ConfigEntry configEntry, String storedConfigValue) {
    if (isNotificationRetention(configEntry) && storedConfigValue.isBlank()) {
      integerField.clear();
      return;
    }

    try {
      integerField.setValue(Integer.parseInt(storedConfigValue));
    } catch (NumberFormatException _) {
      integerField.setValue(Integer.parseInt(configuredDefault(configEntry)));
    }
  }

  public void showGroup(AdminConfigGroup configGroup) {
    if (configGroup == null || !configGroupFilters.containsKey(configGroup)) {
      return;
    }

    selectedGroup = configGroup;
    renderSelectedGroup();
    loadConfigValues();
  }

  private void requestConfigReload() {
    if (hasUnsavedChanges()) {
      UnsavedChangesDialog.confirmDiscard(messageProperties, this::reloadConfigValues);
    } else {
      reloadConfigValues();
    }
  }

  private void reloadConfigValues() {
    loadConfigValues();

    NotificationHelper.getInstance()
        .showNotification(
            messageProperties::getBaseSuccessTitle,
            messageProperties::getAdminConfigReloadSuccess,
            NotificationHelper.NotificationType.SUCCESS);
  }

  private void renderSelectedGroup() {
    if (selectedGroup == null || !configGroupFilters.containsKey(selectedGroup)) {
      if (configGroupFilters.isEmpty()) {
        return;
      }
      selectedGroup = configGroupFilters.keySet().iterator().next();
    }

    refreshCurrentGroupTitle();
    configFieldsLayout.removeAll();

    Predicate<ConfigEntry> selectedGroupFilter = configGroupFilters.get(selectedGroup);
    if (selectedGroupFilter == null) {
      return;
    }

    for (ConfigEntry configEntry : ConfigEntry.values()) {
      if (!selectedGroupFilter.test(configEntry)) {
        continue;
      }

      VerticalLayout configFieldLayout = configInputWrappers.get(configEntry);
      if (configFieldLayout != null) {
        configFieldsLayout.add(configFieldLayout);
      }
    }
  }

  private List<ConfigEntry> getVisibleConfigEntries() {
    if (selectedGroup == null) {
      return List.of();
    }

    Predicate<ConfigEntry> selectedGroupFilter = configGroupFilters.get(selectedGroup);
    if (selectedGroupFilter == null) {
      return List.of();
    }

    return Arrays.stream(ConfigEntry.values()).filter(selectedGroupFilter).toList();
  }

  private void clearValidationStatesForVisibleInputs() {
    for (ConfigEntry configEntry : getVisibleConfigEntries()) {
      Component configInputComponent = configInputs.get(configEntry);
      if (configInputComponent != null) {
        clearValidationState(configInputComponent);
      }
    }
  }

  private void refreshCurrentGroupTitle() {
    if (selectedGroup != null) {
      String groupLabelText = messageProperties.getAdminConfigGroupLabel(selectedGroup);
      currentGroupTitle.setText(groupLabelText);
      currentGroupDescriptionSpan.setText(
          messageProperties.getAdminConfigGroupDescription(selectedGroup));
      currentGroupSettingCountBadge.setText(
          messageProperties.getAdminConfigSettingsCount(getVisibleConfigEntries().size()));
      currentGroupSvgIcon.setSrc(ConfigGroupIcons.forGroup(selectedGroup).getSource());
      configurationTitleSpan.setText(groupLabelText);
    }
  }

  private void refreshInputLabels() {
    configLabelSpans.forEach((configEntry, configLabelSpan) ->
        configLabelSpan.setText(messageProperties.getAdminConfigEntryLabel(configEntry)));
  }

  private void refreshInputHelp() {
    configInputs.forEach((configEntry, configInputComponent) ->
        updatePlaceholder(
            configInputComponent,
            configInputComponent instanceof CosmosDurationField
                ? configuredDefault(configEntry)
                : buildPlaceholder(configEntry)));

    configInfoButtons.values().forEach(ConfigInfoButton::refreshText);
  }

  private void refreshValidationMessagesForInvalidFields() {
    for (ConfigEntry configEntry : getVisibleConfigEntries()) {
      Component configInputComponent = configInputs.get(configEntry);
      if (isInputInvalid(configInputComponent)) {
        String errorMessage = validateValue(configEntry, extractInputValue(configEntry));
        if (errorMessage != null) {
          setValidationError(configInputComponent, errorMessage);
        }
      }
    }
  }

  private void updatePlaceholder(Component configInputComponent, String placeholder) {
    String safePlaceholder = placeholder == null ? "" : placeholder;
    switch (configInputComponent) {
      case EmailField emailField -> emailField.setPlaceholder(safePlaceholder);
      case PasswordField passwordField -> passwordField.setPlaceholder(safePlaceholder);
      case IntegerField integerField -> integerField.setPlaceholder(safePlaceholder);
      case CosmosDurationField durationField -> {
        durationField.setDefaultValue(safePlaceholder);
        durationField.refreshTexts();
      }
      case TextField textField -> textField.setPlaceholder(safePlaceholder);
      default -> {
        /* Switches haben keinen Platzhalter. */
      }
    }
  }

  private void registerConfigGroups() {
    for (AdminConfigGroup configGroup : AdminConfigGroup.values()) {
      if (Arrays.stream(ConfigEntry.values())
          .anyMatch(configEntry -> ConfigurationCatalog.group(configEntry) == configGroup)) {
        configGroupFilters.put(
            configGroup, configEntry -> ConfigurationCatalog.group(configEntry) == configGroup);
      }
    }
  }

  private VerticalLayout createConfigInput(ConfigEntry configEntry) {
    Component configInputComponent = createInputComponent(configEntry);

    updatePlaceholder(
        configInputComponent,
        configInputComponent instanceof CosmosDurationField
            ? configuredDefault(configEntry)
            : buildPlaceholder(configEntry));
    installValidationResetListener(configInputComponent);
    configInputs.put(configEntry, configInputComponent);

    return createFieldWrapper(configInputComponent, configEntry);
  }

  private Component createInputComponent(ConfigEntry configEntry) {
    Object defaultValue = configEntry.getDefaultValue();
    Component configInputComponent;

    if (isBooleanLikeEntry(configEntry, defaultValue)) {
      Switch settingSwitch = new Switch();
      settingSwitch.setWidthFull();
      settingSwitch.addClassName("cosmos-config-switch");
      configInputComponent = settingSwitch;
    } else if (isEmailEntry(configEntry)) {
      EmailField emailField = new EmailField();
      emailField.setPattern(SMTP_EMAIL_REGEX);
      emailField.setClearButtonVisible(true);
      emailField.setWidthFull();
      configInputComponent = emailField;
    } else if (usesSecondsDuration(configEntry)
        || DURATION_ENTRIES.contains(configEntry)
        || isFixedDayDuration(configEntry)) {
      CosmosDurationField durationField =
          new CosmosDurationField(messageProperties, durationStorage(configEntry));
      if (ConfigurationCatalog.group(configEntry) == AdminConfigGroup.ACCOUNT_MANAGEMENT) {
        durationField.requireWholeDays(
            configEntry == ConfigEntry.SELF_DEACTIVATION_REMINDER_DAYS ? 0 : 1);
      }
      configInputComponent = durationField;
    } else if (defaultValue instanceof Integer) {
      configInputComponent = createIntegerInput(configEntry);
    } else if (configEntry == ConfigEntry.REMEMBER_ME_SECRET_KEY
        || configEntry == ConfigEntry.MAIL_PASSWORD) {
      PasswordField passwordField = new PasswordField();
      passwordField.setRevealButtonVisible(true);
      passwordField.setClearButtonVisible(true);
      if (configEntry == ConfigEntry.REMEMBER_ME_SECRET_KEY) {
        passwordField.setMinLength(SECRET_KEY_MIN_LENGTH);
      }
      passwordField.setWidthFull();
      configInputComponent = passwordField;
    } else {
      TextField textField = new TextField();
      textField.setClearButtonVisible(true);
      textField.setWidthFull();
      configInputComponent = textField;
    }

    return configInputComponent;
  }

  private static boolean usesSecondsDuration(ConfigEntry configEntry) {
    return configEntry == ConfigEntry.REMEMBER_ME_DURATION
        || configEntry == ConfigEntry.SESSION_TIMEOUT_SECONDS
        || configEntry == ConfigEntry.LOGIN_ATTEMPT_WINDOW_SECONDS
        || configEntry == ConfigEntry.EMAIL_QUEUE_RETRY_DELAY_SECONDS;
  }

  private CosmosDurationField.Storage durationStorage(ConfigEntry configEntry) {
    if (DURATION_ENTRIES.contains(configEntry)) {
      return CosmosDurationField.Storage.DURATION;
    }

    if (isFixedDayDuration(configEntry)) {
      return CosmosDurationField.Storage.DAYS;
    }

    return CosmosDurationField.Storage.SECONDS;
  }

  private IntegerField createIntegerInput(ConfigEntry configEntry) {
    IntegerField configIntegerField = new IntegerField();
    configIntegerField.setWidthFull();
    configIntegerField.setStepButtonsVisible(true);
    configIntegerField.setClearButtonVisible(true);
    boolean allowsZero =
        configEntry == ConfigEntry.EMAIL_QUEUE_MAX_RETRY
            || configEntry == ConfigEntry.SELF_DEACTIVATION_REMINDER_DAYS;
    configIntegerField.setMin(allowsZero ? 0 : 1);

    if (configEntry == ConfigEntry.MAIL_PORT) {
      configIntegerField.setMax(65535);
    }

    return configIntegerField;
  }

  private VerticalLayout createFieldWrapper(
      Component configInputComponent, ConfigEntry configEntry) {
    VerticalLayout configFieldLayout = new VerticalLayout();
    configFieldLayout.setPadding(false);
    configFieldLayout.setSpacing(false);
    configFieldLayout.setWidthFull();
    configFieldLayout.addClassNames(LumoUtility.Gap.XSMALL, "cosmos-config-field");
    configFieldLayout.setClassName("admin-config-switch", configInputComponent instanceof Switch);

    Span configLabelSpan = new Span(messageProperties.getAdminConfigEntryLabel(configEntry));
    configLabelSpan.setId("config-label-" + configEntry.name().toLowerCase(Locale.ROOT));
    configLabelSpan.addClassNames(LumoUtility.FontWeight.MEDIUM, LumoUtility.FontSize.SMALL);
    configLabelSpan.setClassName(
        "admin-config-required",
        !(configInputComponent instanceof Switch)
            && !(configInputComponent instanceof IntegerField)
            && (!(configInputComponent instanceof CosmosDurationField)
                || DURATION_ENTRIES.contains(configEntry))
            && validateValue(configEntry, "") != null);
    configLabelSpans.put(configEntry, configLabelSpan);
    if (configInputComponent instanceof Switch settingSwitch) {
      settingSwitch.setLabelComponent(configLabelSpan);
    } else {
      HorizontalLayout configLabelLayout = new HorizontalLayout();
      configLabelLayout.setPadding(false);
      configLabelLayout.setSpacing(false);
      configLabelLayout.setWidthFull();
      configLabelLayout.add(configLabelSpan);
      configFieldLayout.add(configLabelLayout);
    }
    String configLabelId = configLabelSpan.getId().orElseThrow();
    if (configInputComponent instanceof HasAriaLabel labelledConfigInput) {
      labelledConfigInput.setAriaLabelledBy(configLabelId);
    }

    ConfigInfoButton configInfoButton =
        new ConfigInfoButton(
            () -> buildConfigTooltipText(configEntry), () -> openHelp(configEntry));
    configInfoButton.getElement().setAttribute("aria-labelledby", configLabelId);
    configInfoButtons.put(configEntry, configInfoButton);
    Div configInputRowDiv = new Div(configInputComponent, configInfoButton);
    configInputRowDiv.addClassName("cosmos-config-input-row");
    configFieldLayout.add(configInputRowDiv);
    return configFieldLayout;
  }

  private void openHelp(ConfigEntry configEntry) {
    openHelpConfigEntry = configEntry;
    helpPanel.removeAll();
    helpPanel.getFooter().removeAll();
    helpPanel.setHeading(messageProperties.getAdminConfigHelpTitle());
    helpPanel.setDescription(messageProperties.getAdminConfigHelpDescription());
    helpPanel.refreshCloseText(messageProperties);

    Span helpGroupSpan = new Span(messageProperties.getAdminConfigGroupLabel(selectedGroup));
    helpGroupSpan.addClassName("admin-help-group");
    H3 fieldTitleH3 = new H3(messageProperties.getAdminConfigEntryLabel(configEntry));
    fieldTitleH3.addClassName("admin-help-field-title");
    Span fieldDescriptionSpan = new Span(messageProperties.getAdminConfigEntryTooltip(configEntry));
    fieldDescriptionSpan.addClassName("admin-help-description");

    Div defaultValueDiv =
        new Div(new Span(defaultLabel(configEntry)), new Span(defaultDisplayValue(configEntry)));
    defaultValueDiv.addClassName(HELP_DEFAULT_CLASS);
    H4 rulesTitleH4 = new H4(messageProperties.getAdminConfigInputRules());
    UnorderedList inputRulesList = new UnorderedList();
    buildInputRules(configEntry)
        .forEach(inputRuleText -> inputRulesList.add(new ListItem(inputRuleText)));
    Div inputRulesDiv = new Div(rulesTitleH4, inputRulesList);
    inputRulesDiv.addClassName("admin-help-rules");

    helpPanel.add(helpGroupSpan, fieldTitleH3, fieldDescriptionSpan);
    if (configEntry == ConfigEntry.COPYRIGHT_TEXT) {
      addCopyrightPlaceholderHelp();
    }
    if (configInputs.get(configEntry) instanceof CosmosDurationField durationField) {
      addDurationValueHelp(durationField, !isFixedDayDuration(configEntry));
    }

    helpPanel.add(defaultValueDiv, inputRulesDiv);
    helpPanel.addEffectiveUse(
        messageProperties, messageProperties.getAdminConfigEffectiveUse(configEntry));
    if (ConfigurationCatalog.requiresRestart(configEntry)) {
      addRestartHelp();
    }

    Button closeHelpButton =
        new Button(
            messageProperties.getNotificationsClose(),
            AppIcon.CLOSE.create(),
            event -> helpPanel.close());
    closeHelpButton.setWidthFull();
    helpPanel.getFooter().add(closeHelpButton);
    helpPanel.open();
  }

  private void addCopyrightPlaceholderHelp() {
    Div copyrightPlaceholdersDiv =
        new Div(
            new H4(messageProperties.getAdminConfigHelpPlaceholders()),
            placeholderRow("{year}", messageProperties.getAdminConfigHelpYear()),
            placeholderRow("{serviceName}", messageProperties.getAdminConfigHelpServiceName()));
    copyrightPlaceholdersDiv.addClassName("admin-help-placeholders");
    helpPanel.add(copyrightPlaceholdersDiv);
  }

  private void addDurationValueHelp(
      CosmosDurationField durationField, boolean showEquivalentSeconds) {
    Div currentDurationDiv =
        new Div(
            new Span(messageProperties.getAdminConfigDurationCurrent()),
            new Span(durationField.getDisplayValue()));
    currentDurationDiv.addClassName(HELP_DEFAULT_CLASS);
    helpPanel.add(currentDurationDiv);
    if (showEquivalentSeconds) {
      Div equivalentDurationDiv =
          new Div(
              new Span(messageProperties.getAdminConfigDurationEquivalent()),
              new Span(durationField.getEquivalentValue()));
      equivalentDurationDiv.addClassName(HELP_DEFAULT_CLASS);
      helpPanel.add(equivalentDurationDiv);
    }
  }

  private void addRestartHelp() {
    Span restartWarningHeadingSpan =
        new Span(messageProperties.getAdminConfigRestartRequired() + " ");
    restartWarningHeadingSpan.addClassName("cosmos-security-warning-label");
    HorizontalLayout restartWarningLayout =
        new HorizontalLayout(
            AppIcon.ERROR.create(),
            new Span(
                restartWarningHeadingSpan,
                new Span(messageProperties.getAdminConfigRestartDetail())));
    restartWarningLayout.setSpacing(false);
    restartWarningLayout.setPadding(false);
    restartWarningLayout.setAlignItems(FlexComponent.Alignment.START);
    restartWarningLayout.addClassNames("cosmos-security-warning", "admin-help-restart-warning");
    helpPanel.add(restartWarningLayout);
  }

  private Div placeholderRow(String placeholderToken, String placeholderMeaning) {
    Span placeholderTokenSpan = new Span(placeholderToken);
    placeholderTokenSpan.addClassName("admin-help-token");
    return new Div(placeholderTokenSpan, new Span(placeholderMeaning));
  }

  private String buildConfigTooltipText(ConfigEntry configEntry) {
    StringBuilder tooltipTextBuilder =
        new StringBuilder(messageProperties.getAdminConfigEntryTooltip(configEntry));
    tooltipTextBuilder.append("\n\n").append(buildDefaultText(configEntry));
    tooltipTextBuilder
        .append("\n\n")
        .append(messageProperties.getAdminConfigInputRules())
        .append(":");
    buildInputRules(configEntry)
        .forEach(inputRuleText -> tooltipTextBuilder.append("\n• ").append(inputRuleText));
    return tooltipTextBuilder.toString();
  }

  /** Beschreibt die vorhandene Prüfung; führt keine zweite Validierung aus. */
  private List<String> buildInputRules(ConfigEntry configEntry) {
    return switch (configEntry) {
      case SERVICE_NAME ->
          List.of(
              messageProperties.getAdminConfigValidationServiceName(),
              messageProperties.getAdminConfigHelperServiceNameMax(SERVICE_NAME_MAX_LENGTH));
      case COPYRIGHT_TEXT, MAIL_USERNAME, MAIL_PASSWORD ->
          List.of(messageProperties.getAdminConfigRuleOptional());
      case BASE_URL -> List.of(messageProperties.getAdminConfigValidationUrl());
      case REMEMBER_ME_SECRET_KEY ->
          List.of(messageProperties.getAdminConfigValidationSecretKey(SECRET_KEY_MIN_LENGTH));
      case LOGIN_ATTEMPT_WINDOW_SECONDS ->
          List.of(
              messageProperties.getAdminConfigDurationRule(86_400),
              messageProperties.getAdminConfigDurationUnitHint(),
              messageProperties.getAdminConfigRuleEmptyUsesDefault());
      case REMEMBER_ME_DURATION, SESSION_TIMEOUT_SECONDS, EMAIL_QUEUE_RETRY_DELAY_SECONDS ->
          List.of(
              messageProperties.getAdminConfigDurationRule(Integer.MAX_VALUE),
              messageProperties.getAdminConfigDurationUnitHint(),
              messageProperties.getAdminConfigRuleEmptyUsesDefault());
      case LOGIN_ATTEMPTS_LIMIT,
          MAX_SESSIONS_PER_USER,
          SELF_DEACTIVATION_RETENTION_DAYS,
          EMAIL_QUEUE_POOL_SIZE,
          EMAIL_QUEUE_CAPACITY ->
          List.of(
              messageProperties.getAdminConfigRuleInteger(1, Integer.MAX_VALUE),
              messageProperties.getAdminConfigRuleEmptyUsesDefault());
      case USER_NOTIFICATIONS_INBOX_RETENTION_DAYS, USER_NOTIFICATIONS_HISTORY_RETENTION_DAYS ->
          List.of(
              messageProperties.getAdminConfigRuleInteger(1, Integer.MAX_VALUE),
              messageProperties.getAdminConfigRuleEmptyUnlimited());
      case EMAIL_QUEUE_MAX_RETRY, SELF_DEACTIVATION_REMINDER_DAYS ->
          List.of(
              messageProperties.getAdminConfigRuleInteger(0, Integer.MAX_VALUE),
              messageProperties.getAdminConfigRuleEmptyUsesDefault());
      case MAIL_PORT ->
          List.of(
              messageProperties.getAdminConfigRuleInteger(1, 65535),
              messageProperties.getAdminConfigRuleEmptyUsesDefault());
      case MAINTENANCE_MODE,
          MAIL_SMTP_AUTH,
          MAIL_SMTP_STARTTLS_ENABLE,
          MAIL_SMTP_SSL_ENABLE,
          MAIL_DEBUG ->
          List.of(messageProperties.getAdminConfigRuleBoolean());
      case EMAIL_FROM, EMAIL_ADMIN -> List.of(messageProperties.getAdminConfigValidationEmail());
      case MAIL_HOST -> List.of(messageProperties.getAdminConfigRuleHost());
      case MAIL_SMTP_SSL_TRUST -> List.of(messageProperties.getAdminConfigRuleSslTrust());
      case USER_AUTO_ABSENT_TIMEOUT ->
          List.of(
              messageProperties.getAdminConfigDurationNumberRule(),
              messageProperties.getAdminConfigDurationUnitHint(),
              messageProperties.getAdminConfigRuleDurationNoMinimum());
      case EMAIL_QUEUE_SENT_LIVE_DURATION ->
          List.of(
              messageProperties.getAdminConfigDurationNumberRule(),
              messageProperties.getAdminConfigDurationUnitHint(),
              messageProperties.getAdminConfigValidationPositiveDuration());
      case VERIFICATION_TOKEN_VALID_DURATION,
          VERIFICATION_TOKEN_LIVE_DURATION,
          PASSWORD_RESET_TOKEN_VALID_DURATION,
          PASSWORD_RESET_TOKEN_LIVE_DURATION ->
          List.of(
              messageProperties.getAdminConfigDurationNumberRule(),
              messageProperties.getAdminConfigDurationUnitHint(),
              messageProperties.getAdminConfigValidationDurationMinimum(
                  configEntry.getMinimumTokenDuration().toMinutes()));
    };
  }

  private void saveConfigValues() {
    if (!validateVisibleInputs()) {
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getBaseFailedTitle,
              messageProperties::getAdminConfigValidationInvalidFields,
              NotificationHelper.NotificationType.ERROR);
      return;
    }

    try {
      List<ConfigEntry> changedEntries = persistVisibleConfigValues();
      loadConfigValues();
      if (!changedEntries.isEmpty()) {
        logger.info(
            "Configuration group stored: group={}, changedEntries={}",
            selectedGroup,
            changedEntries.size());
      }

      if (configurationChangedAction != null
          && (changedEntries.contains(ConfigEntry.SERVICE_NAME)
              || changedEntries.contains(ConfigEntry.MAINTENANCE_MODE))) {
        configurationChangedAction.run();
      }

      notifyConfigurationSaved(
          changedEntries.stream().filter(ConfigurationCatalog::requiresRestart).toList());
    } catch (Exception saveException) {
      logger.error(
          "Configuration group update interrupted: group={}, individual entries may already be committed",
          selectedGroup,
          safeFailure(saveException));
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getBaseFailedTitle,
              messageProperties::getBaseFailedMessage,
              -1,
              NotificationHelper.NotificationType.ERROR);
    }
  }

  private List<ConfigEntry> persistVisibleConfigValues() {
    List<ConfigEntry> changedEntries = new ArrayList<>();
    Map<ConfigEntry, String> smtpSettings = new LinkedHashMap<>();
    for (ConfigEntry configEntry : getVisibleConfigEntries()) {
      String submittedConfigValue = normalizeValue(configEntry, extractInputValue(configEntry));
      String previousConfigValue = configService.getForAdministration(configEntry);

      if (selectedGroup == AdminConfigGroup.SMTP) {
        smtpSettings.put(configEntry, submittedConfigValue);
      } else {
        configService.set(configEntry, submittedConfigValue);
      }

      if (!Objects.equals(previousConfigValue, submittedConfigValue)) {
        changedEntries.add(configEntry);
      }
    }

    if (!smtpSettings.isEmpty()) {
      configService.saveSmtpSettings(smtpSettings);
    }

    return changedEntries;
  }

  private void notifyConfigurationSaved(List<ConfigEntry> restartEntries) {
    NotificationHelper notificationHelper = NotificationHelper.getInstance();
    if (restartEntries.isEmpty()) {
      notificationHelper.showNotification(
          messageProperties::getBaseSuccessTitle,
          messageProperties::getAdminConfigSaveSuccess,
          NotificationHelper.NotificationType.SUCCESS);
      return;
    }

    notificationHelper.showNotificationWithDetails(
        messageProperties::getAdminConfigSaveSuccess,
        messageProperties::getAdminConfigRestartSaved,
        () -> restartEntries.stream().map(messageProperties::getAdminConfigEntryLabel).toList(),
        0,
        NotificationHelper.NotificationType.WARNING);
  }

  private boolean validateVisibleInputs() {
    boolean allValid = true;
    Component firstInvalidConfigInput = null;
    ConfigEntry firstInvalidConfigEntry = null;

    for (ConfigEntry configEntry : getVisibleConfigEntries()) {
      ValidationResult inputValidationResult = validateInput(configEntry);

      if (inputValidationResult.invalid()) {
        if (firstInvalidConfigInput == null) {
          firstInvalidConfigInput = inputValidationResult.configInputComponent();
          firstInvalidConfigEntry = configEntry;
        }
        allValid = false;
      }
    }

    if (firstInvalidConfigInput != null) {
      logger.debug(
          "Configuration validation rejected field: key={}", firstInvalidConfigEntry.name());
      firstInvalidConfigInput.getElement().callJsFunction("focus");
    }

    return allValid;
  }

  private ValidationResult validateInput(ConfigEntry configEntry) {
    Component configInputComponent = configInputs.get(configEntry);
    if (configInputComponent == null) {
      return ValidationResult.valid();
    }

    clearValidationState(configInputComponent);

    String submittedConfigValue = extractInputValue(configEntry);
    String validationErrorMessage = validateValue(configEntry, submittedConfigValue);

    if (validationErrorMessage == null) {
      return ValidationResult.valid();
    }

    setValidationError(configInputComponent, validationErrorMessage);
    logger.debug("Configuration validation rejected field: key={}", configEntry.name());
    return ValidationResult.invalid(configInputComponent);
  }

  private record ValidationResult(boolean invalid, Component configInputComponent) {
    private static ValidationResult valid() {
      return new ValidationResult(false, null);
    }

    private static ValidationResult invalid(Component configInputComponent) {
      return new ValidationResult(true, configInputComponent);
    }
  }

  private String validateValue(ConfigEntry configEntry, String submittedConfigValue) {
    String trimmedConfigValue = submittedConfigValue == null ? "" : submittedConfigValue.trim();
    if (isNotificationRetention(configEntry) && trimmedConfigValue.isEmpty()) {
      return null;
    }

    return switch (configEntry) {
      case SERVICE_NAME -> validateServiceName(trimmedConfigValue);
      case BASE_URL ->
          !isValidHttpUrl(trimmedConfigValue)
              ? messageProperties.getAdminConfigValidationUrl()
              : null;
      case LOGIN_ATTEMPT_WINDOW_SECONDS ->
          isInvalidPositiveInteger(trimmedConfigValue)
                  || Integer.parseInt(trimmedConfigValue) > 86_400
              ? messageProperties.getAdminConfigDurationRule(86_400)
              : null;
      case REMEMBER_ME_DURATION, SESSION_TIMEOUT_SECONDS, EMAIL_QUEUE_RETRY_DELAY_SECONDS ->
          isInvalidPositiveInteger(trimmedConfigValue)
              ? messageProperties.getAdminConfigDurationInvalid()
              : null;
      case REMEMBER_ME_SECRET_KEY ->
          trimmedConfigValue.length() < SECRET_KEY_MIN_LENGTH
              ? messageProperties.getAdminConfigValidationSecretKey(SECRET_KEY_MIN_LENGTH)
              : null;
      case USER_AUTO_ABSENT_TIMEOUT ->
          !isValidSpringDuration(trimmedConfigValue)
              ? messageProperties.getAdminConfigValidationDuration()
              : null;
      case EMAIL_QUEUE_SENT_LIVE_DURATION -> validateQueueRetention(trimmedConfigValue);
      case VERIFICATION_TOKEN_VALID_DURATION,
          VERIFICATION_TOKEN_LIVE_DURATION,
          PASSWORD_RESET_TOKEN_VALID_DURATION,
          PASSWORD_RESET_TOKEN_LIVE_DURATION ->
          validateTokenDuration(configEntry, trimmedConfigValue);
      case LOGIN_ATTEMPTS_LIMIT,
          MAX_SESSIONS_PER_USER,
          SELF_DEACTIVATION_RETENTION_DAYS,
          EMAIL_QUEUE_POOL_SIZE,
          EMAIL_QUEUE_CAPACITY,
          USER_NOTIFICATIONS_INBOX_RETENTION_DAYS,
          USER_NOTIFICATIONS_HISTORY_RETENTION_DAYS ->
          isInvalidPositiveInteger(trimmedConfigValue)
              ? messageProperties.getAdminConfigValidationMinOne()
              : null;
      case EMAIL_QUEUE_MAX_RETRY, SELF_DEACTIVATION_REMINDER_DAYS ->
          !isValidNonNegativeInteger(trimmedConfigValue)
              ? messageProperties.getAdminConfigValidationNonNegative()
              : null;
      case EMAIL_FROM, EMAIL_ADMIN ->
          !isValidEmail(trimmedConfigValue)
              ? messageProperties.getAdminConfigValidationEmail()
              : null;
      case MAIL_HOST ->
          isInvalidHost(trimmedConfigValue)
              ? messageProperties.getAdminConfigValidationHost()
              : null;
      case MAIL_PORT ->
          !isValidPort(trimmedConfigValue)
              ? messageProperties.getAdminConfigValidationPort()
              : null;
      case MAIL_SMTP_SSL_TRUST ->
          !isValidSslTrustValue(trimmedConfigValue)
              ? messageProperties.getAdminConfigValidationHost()
              : null;
      default -> null;
    };
  }

  private String validateServiceName(String trimmedConfigValue) {
    if (trimmedConfigValue.isBlank()) {
      return messageProperties.getAdminConfigValidationServiceName();
    }

    if (trimmedConfigValue.length() > SERVICE_NAME_MAX_LENGTH) {
      return messageProperties.getAdminConfigValidationServiceNameMax(SERVICE_NAME_MAX_LENGTH);
    }

    return null;
  }

  private String validateQueueRetention(String trimmedConfigValue) {
    if (!isValidSpringDuration(trimmedConfigValue)) {
      return messageProperties.getAdminConfigValidationDuration();
    }

    Duration queueRetentionDuration = DurationStyle.detectAndParse(trimmedConfigValue);
    return queueRetentionDuration.isZero() || queueRetentionDuration.isNegative()
        ? messageProperties.getAdminConfigValidationPositiveDuration()
        : null;
  }

  private String validateTokenDuration(ConfigEntry configEntry, String trimmedConfigValue) {
    Duration minimumTokenDuration = configEntry.getMinimumTokenDuration();
    if (!isValidSpringDuration(trimmedConfigValue)) {
      return messageProperties.getAdminConfigValidationTokenDuration(
          minimumTokenDuration.toMinutes());
    }

    return TokenDurationPolicy.meetsMinimumDuration(
            DurationStyle.detectAndParse(trimmedConfigValue), minimumTokenDuration)
        ? null
        : messageProperties.getAdminConfigValidationDurationMinimum(
            minimumTokenDuration.toMinutes());
  }

  private String normalizeValue(ConfigEntry configEntry, String submittedConfigValue) {
    String trimmedConfigValue = submittedConfigValue == null ? "" : submittedConfigValue.trim();

    if (DURATION_ENTRIES.contains(configEntry) && !trimmedConfigValue.isBlank()) {
      return DurationStyle.detectAndParse(trimmedConfigValue).toString();
    }

    return trimmedConfigValue;
  }

  private String extractInputValue(ConfigEntry configEntry) {
    Component configInputComponent = configInputs.get(configEntry);

    if (configInputComponent instanceof Switch settingSwitch) {
      return String.valueOf(settingSwitch.getValue());
    }

    if (configInputComponent instanceof CosmosDurationField durationField) {
      String storedDurationValue = durationField.getStoredValue();
      return storedDurationValue.isEmpty() && usesSecondsDuration(configEntry)
          ? configuredDefault(configEntry)
          : storedDurationValue;
    }

    if (configInputComponent instanceof IntegerField integerField) {
      Integer submittedIntegerValue = integerField.getValue();
      if (submittedIntegerValue != null) {
        return String.valueOf(submittedIntegerValue);
      }
      return isNotificationRetention(configEntry) ? "" : configuredDefault(configEntry);
    }

    if (configInputComponent instanceof PasswordField passwordField) {
      return Objects.toString(passwordField.getValue(), "");
    }

    if (configInputComponent instanceof EmailField emailField) {
      return Objects.toString(emailField.getValue(), "");
    }

    if (configInputComponent instanceof TextField textField) {
      return Objects.toString(textField.getValue(), "");
    }

    return configEntry.getDefaultValueString();
  }

  private String configuredDefault(ConfigEntry configEntry) {
    String configuredDefaultValue = configService.getDefaultValue(configEntry);
    return configuredDefaultValue == null
        ? configEntry.getDefaultValueString()
        : configuredDefaultValue;
  }

  private boolean isSecretEntry(ConfigEntry configEntry) {
    return ConfigurationCatalog.secret(configEntry);
  }

  private boolean isFixedDayDuration(ConfigEntry configEntry) {
    return isNotificationRetention(configEntry)
        || ConfigurationCatalog.group(configEntry) == AdminConfigGroup.ACCOUNT_MANAGEMENT;
  }

  private boolean isNotificationRetention(ConfigEntry configEntry) {
    return configEntry == ConfigEntry.USER_NOTIFICATIONS_INBOX_RETENTION_DAYS
        || configEntry == ConfigEntry.USER_NOTIFICATIONS_HISTORY_RETENTION_DAYS;
  }

  private String buildPlaceholder(ConfigEntry configEntry) {
    if (isNotificationRetention(configEntry)) {
      return messageProperties.getAdminConfigUnlimited();
    }

    String defaultValue = configuredDefault(configEntry);
    if (defaultValue.isEmpty()) {
      return messageProperties.getAdminConfigPlaceholderEmpty();
    }

    return isSecretEntry(configEntry)
        ? messageProperties.getAdminConfigDefaultConfidential()
        : defaultValue;
  }

  private String buildDefaultText(ConfigEntry configEntry) {
    return defaultLabel(configEntry) + ": " + defaultDisplayValue(configEntry);
  }

  private String defaultLabel(ConfigEntry configEntry) {
    return isNotificationRetention(configEntry)
        ? messageProperties.getAdminConfigInitialValue()
        : messageProperties.getAdminConfigDefault();
  }

  private String defaultDisplayValue(ConfigEntry configEntry) {
    String defaultValue = configuredDefault(configEntry);
    if (configInputs.get(configEntry) instanceof CosmosDurationField durationField) {
      return durationField.displayStoredValue(defaultValue);
    }

    String displayValue = defaultValue;
    if (defaultValue.isEmpty()) {
      displayValue = messageProperties.getAdminConfigDefaultEmpty();
    } else if (isSecretEntry(configEntry)) {
      displayValue = messageProperties.getAdminConfigDefaultConfidential();
    }

    return displayValue;
  }

  private void installValidationResetListener(Component configInputComponent) {
    if (configInputComponent instanceof CosmosDurationField durationField) {
      durationField.addChangeListener(() -> clearValidationState(durationField));
    } else if (configInputComponent instanceof EmailField emailField) {
      emailField.addValueChangeListener(event -> clearValidationState(emailField));
    } else if (configInputComponent instanceof PasswordField passwordField) {
      passwordField.addValueChangeListener(event -> clearValidationState(passwordField));
    } else if (configInputComponent instanceof TextField textField) {
      textField.addValueChangeListener(event -> clearValidationState(textField));
    } else if (configInputComponent instanceof IntegerField integerField) {
      integerField.addValueChangeListener(event -> clearValidationState(integerField));
    } else if (configInputComponent instanceof Switch settingSwitch) {
      settingSwitch.addValueChangeListener(event -> clearValidationState(settingSwitch));
    }
  }

  private void clearValidationState(Component configInputComponent) {
    if (configInputComponent instanceof CosmosDurationField durationField) {
      durationField.setInvalid(false);
      durationField.setErrorMessage(null);
    } else if (configInputComponent instanceof EmailField emailField) {
      emailField.setInvalid(false);
      emailField.setErrorMessage(null);
    } else if (configInputComponent instanceof PasswordField passwordField) {
      passwordField.setInvalid(false);
      passwordField.setErrorMessage(null);
    } else if (configInputComponent instanceof TextField textField) {
      textField.setInvalid(false);
      textField.setErrorMessage(null);
    } else if (configInputComponent instanceof IntegerField integerField) {
      integerField.setInvalid(false);
      integerField.setErrorMessage(null);
    }
  }

  private void setValidationError(Component configInputComponent, String errorMessage) {
    if (configInputComponent instanceof CosmosDurationField durationField) {
      durationField.setInvalid(true);
      durationField.setErrorMessage(errorMessage);
    } else if (configInputComponent instanceof EmailField emailField) {
      emailField.setInvalid(true);
      emailField.setErrorMessage(errorMessage);
    } else if (configInputComponent instanceof PasswordField passwordField) {
      passwordField.setInvalid(true);
      passwordField.setErrorMessage(errorMessage);
    } else if (configInputComponent instanceof TextField textField) {
      textField.setInvalid(true);
      textField.setErrorMessage(errorMessage);
    } else if (configInputComponent instanceof IntegerField integerField) {
      integerField.setInvalid(true);
      integerField.setErrorMessage(errorMessage);
    }
  }

  private boolean isInputInvalid(Component configInputComponent) {
    if (configInputComponent instanceof CosmosDurationField durationField) {
      return durationField.isInvalid();
    }
    if (configInputComponent instanceof EmailField emailField) {
      return emailField.isInvalid();
    }
    if (configInputComponent instanceof PasswordField passwordField) {
      return passwordField.isInvalid();
    }
    if (configInputComponent instanceof TextField textField) {
      return textField.isInvalid();
    }
    if (configInputComponent instanceof IntegerField integerField) {
      return integerField.isInvalid();
    }

    return false;
  }

  private boolean isBooleanLikeEntry(ConfigEntry configEntry, Object defaultValue) {
    return defaultValue instanceof Boolean || configEntry == ConfigEntry.MAIL_DEBUG;
  }

  private boolean isEmailEntry(ConfigEntry configEntry) {
    return configEntry == ConfigEntry.EMAIL_FROM || configEntry == ConfigEntry.EMAIL_ADMIN;
  }

  private boolean isValidHttpUrl(String urlText) {
    if (urlText == null || urlText.isBlank()) {
      return false;
    }

    try {
      URI uri = new URI(urlText);
      return uri.getScheme() != null
          && uri.getHost() != null
          && ("http".equalsIgnoreCase(uri.getScheme())
              || "https".equalsIgnoreCase(uri.getScheme()));
    } catch (URISyntaxException _) {
      return false;
    }
  }

  private boolean isValidSpringDuration(String durationText) {
    if (durationText == null) {
      return false;
    }

    String trimmedConfigValue = durationText.trim();
    if (trimmedConfigValue.isBlank()) {
      return false;
    }

    if (DURATION_NUMBER_ONLY_PATTERN.matcher(trimmedConfigValue).matches()) {
      return false;
    }

    try {
      DurationStyle.detectAndParse(trimmedConfigValue);
      return true;
    } catch (IllegalArgumentException _) {
      return false;
    }
  }

  private boolean isInvalidPositiveInteger(String integerText) {
    try {
      return Integer.parseInt(integerText) < 1;
    } catch (NumberFormatException _) {
      return true;
    }
  }

  private boolean isValidNonNegativeInteger(String integerText) {
    try {
      return Integer.parseInt(integerText) >= 0;
    } catch (NumberFormatException _) {
      return false;
    }
  }

  private boolean isValidEmail(String emailAddress) {
    return emailAddress != null && SMTP_EMAIL_PATTERN.matcher(emailAddress).matches();
  }

  private boolean isInvalidHost(String hostName) {
    return hostName == null
        || hostName.isBlank()
        || hostName.contains("://")
        || hostName.contains("/")
        || hostName.contains(" ");
  }

  private boolean isValidPort(String portText) {
    try {
      int port = Integer.parseInt(portText);
      return port >= 1 && port <= 65535;
    } catch (NumberFormatException _) {
      return false;
    }
  }

  private boolean isValidSslTrustValue(String sslTrustValue) {
    if (sslTrustValue == null || sslTrustValue.isBlank()) {
      return false;
    }

    if ("*".equals(sslTrustValue.trim())) {
      return true;
    }

    // Das Komma ist hier ein wörtliches Trennzeichen für die Hostliste, kein komplexes
    // Regex-Muster.
    String[] trustedHostNames = sslTrustValue.split(",");
    for (String trustedHostName : trustedHostNames) {
      if (isInvalidHost(trustedHostName.trim())) {
        return false;
      }
    }
    return true;
  }
}
