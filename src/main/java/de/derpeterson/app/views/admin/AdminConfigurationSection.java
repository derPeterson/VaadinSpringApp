package de.derpeterson.app.views.admin;

import static de.derpeterson.app.helper.logging.LogSafety.safeFailure;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasAriaLabel;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.config.AdminConfigGroup;
import de.derpeterson.app.config.TokenDurationPolicy;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.ui.components.ConfigGroupIcons;
import de.derpeterson.app.ui.components.ConfigInfoButton;
import de.derpeterson.app.ui.components.CosmosCardComponent;
import de.derpeterson.app.ui.components.CosmosSectionHeaderComponent;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
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

  private static final Set<ConfigEntry> DURATION_ENTRIES =
      Set.of(
          ConfigEntry.USER_AUTO_ABSENT_TIMEOUT,
          ConfigEntry.VERIFICATION_TOKEN_VALID_DURATION,
          ConfigEntry.VERIFICATION_TOKEN_LIVE_DURATION,
          ConfigEntry.PASSWORD_RESET_TOKEN_VALID_DURATION,
          ConfigEntry.PASSWORD_RESET_TOKEN_LIVE_DURATION,
          ConfigEntry.EMAIL_QUEUE_SENT_LIVE_DURATION);

  private final transient MessageProperties messageProperties;
  private final transient ConfigService configService;
  private final transient Runnable configurationChangedAction;

  private final EnumMap<ConfigEntry, Component> configInputs = new EnumMap<>(ConfigEntry.class);
  private final EnumMap<ConfigEntry, VerticalLayout> configInputWrappers =
      new EnumMap<>(ConfigEntry.class);
  private final EnumMap<ConfigEntry, Span> configLabelSpans = new EnumMap<>(ConfigEntry.class);
  private final EnumMap<ConfigEntry, ConfigInfoButton> configInfoButtons =
      new EnumMap<>(ConfigEntry.class);
  private final Map<AdminConfigGroup, Predicate<ConfigEntry>> configGroupFilters =
      new LinkedHashMap<>();

  private final Span configurationBreadcrumb;
  private final Span configurationTitle;
  private final Span configurationDescription;
  private final H2 currentGroupTitle;
  private final Icon currentGroupIcon;
  private final Span currentGroupDescription;
  private final Span currentGroupSettingCount;
  private final Span configurationSaveHint;
  private final VerticalLayout configFieldsContainer;

  private final Button saveConfigButton;
  private final Button reloadConfigButton;

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

    configurationBreadcrumb = new Span();
    configurationTitle = new Span();

    configurationDescription = new Span();
    configurationDescription.addClassNames(LumoUtility.TextColor.SECONDARY);

    currentGroupTitle = new H2();
    currentGroupTitle.addClassName("cosmos-config-group-title");
    currentGroupIcon = VaadinIcon.COG.create();
    currentGroupIcon.getElement().setAttribute("aria-hidden", "true");
    currentGroupDescription = new Span();
    currentGroupDescription.addClassName("cosmos-config-group-description");
    currentGroupSettingCount = new Span();
    currentGroupSettingCount.addClassName("cosmos-config-setting-count");
    configurationSaveHint = new Span();

    configFieldsContainer = new VerticalLayout();
    configFieldsContainer.setPadding(false);
    configFieldsContainer.setSpacing(true);
    configFieldsContainer.setWidthFull();
    configFieldsContainer.addClassName("cosmos-config-fields");

    registerConfigGroups();

    for (ConfigEntry configEntry : ConfigEntry.values()) {
      configInputWrappers.put(configEntry, createConfigInput(configEntry));
    }

    saveConfigButton = new Button();
    saveConfigButton.setPrefixComponent(VaadinIcon.CHECK.create());
    saveConfigButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    saveConfigButton.addClickListener(clickEvent -> saveConfigValues());
    // Enter in der Navigation oder an einem Info-Button darf keine Werte speichern.
    saveConfigButton
        .addClickShortcut(Key.ENTER)
        .listenOn(configInputs.values().toArray(Component[]::new))
        .resetFocusOnActiveElement();

    reloadConfigButton = new Button();
    reloadConfigButton.setPrefixComponent(VaadinIcon.REFRESH.create());
    reloadConfigButton.addClickListener(clickEvent -> reloadConfigValues());

    VerticalLayout configLayout = createConfigurationCardContent();

    CosmosCardComponent configCard = new CosmosCardComponent(configLayout);
    configCard.addClassNames("cosmos-admin-form-card", "cosmos-config-card");
    add(
        new CosmosSectionHeaderComponent(
            configurationBreadcrumb, configurationTitle, configurationDescription),
        configCard);

    if (!configGroupFilters.isEmpty()) {
      selectedGroup = configGroupFilters.keySet().iterator().next();
    }

    refreshTexts();
    renderSelectedGroup();
    loadConfigValues();
  }

  private @NonNull VerticalLayout createConfigurationCardContent() {
    Div iconBadge = new Div(currentGroupIcon);
    iconBadge.addClassName("cosmos-config-group-icon");
    Div headingText = new Div(currentGroupTitle, currentGroupDescription);
    headingText.addClassName("cosmos-config-group-heading");
    Div cardHeader = new Div(iconBadge, headingText, currentGroupSettingCount);
    cardHeader.addClassName("cosmos-config-card-header");

    Icon hintIcon = VaadinIcon.INFO_CIRCLE.create();
    hintIcon.getElement().setAttribute("aria-hidden", "true");
    Div saveHint = new Div(hintIcon, configurationSaveHint);
    saveHint.addClassName("cosmos-config-save-hint");
    Div buttonLayout = new Div(reloadConfigButton, saveConfigButton);
    buttonLayout.addClassName("cosmos-config-card-actions");
    Div cardFooter = new Div(saveHint, buttonLayout);
    cardFooter.addClassName("cosmos-config-card-footer");

    VerticalLayout configLayout = new VerticalLayout(cardHeader, configFieldsContainer, cardFooter);
    configLayout.setPadding(false);
    configLayout.setSpacing(false);
    configLayout.addClassNames("cosmos-admin-form", "cosmos-config-form");
    configLayout.setAlignItems(Alignment.START);
    return configLayout;
  }

  public void refreshTexts() {
    configurationTitle.setText(messageProperties.getAdminConfigTitle());
    configurationDescription.setText(messageProperties.getAdminConfigDescription());
    saveConfigButton.setText(messageProperties.getAdminConfigSave());
    reloadConfigButton.setText(messageProperties.getAdminConfigReload());
    configurationSaveHint.setText(messageProperties.getAdminConfigSaveHint());

    refreshInputLabels();
    refreshInputHelp();
    refreshCurrentGroupTitle();
    refreshValidationMessagesForInvalidFields();
  }

  public void loadConfigValues() {
    for (ConfigEntry configEntry : getVisibleConfigEntries()) {
      Component input = configInputs.get(configEntry);
      if (input == null) {
        continue;
      }

      String safeValue = getSafeConfigValue(configEntry);
      applyConfigValue(input, configEntry, safeValue);
    }

    clearValidationStatesForVisibleInputs();
  }

  private String getSafeConfigValue(ConfigEntry configEntry) {
    String value = configService.getString(configEntry);
    return value == null ? "" : value;
  }

  private void applyConfigValue(Component input, ConfigEntry configEntry, String safeValue) {
    switch (input) {
      case Checkbox checkbox -> checkbox.setValue(Boolean.parseBoolean(safeValue));
      case IntegerField integerField -> setIntegerFieldValue(integerField, configEntry, safeValue);
      case PasswordField passwordField -> passwordField.setValue(safeValue);
      case EmailField emailField -> emailField.setValue(safeValue);
      case TextField textField -> textField.setValue(safeValue);
      default -> {
        // no-op
      }
    }
  }

  private void setIntegerFieldValue(
      IntegerField integerField, ConfigEntry configEntry, String safeValue) {
    if (isNotificationRetention(configEntry) && safeValue.isBlank()) {
      integerField.clear();
      return;
    }
    try {
      integerField.setValue(Integer.parseInt(safeValue));
    } catch (NumberFormatException _) {
      integerField.setValue(Integer.parseInt(configuredDefault(configEntry)));
    }
  }

  public void showGroup(AdminConfigGroup group) {
    if (group == null || !configGroupFilters.containsKey(group)) {
      return;
    }

    selectedGroup = group;
    renderSelectedGroup();
    loadConfigValues();
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
    configFieldsContainer.removeAll();

    Predicate<ConfigEntry> filter = configGroupFilters.get(selectedGroup);
    if (filter == null) {
      return;
    }

    for (ConfigEntry configEntry : ConfigEntry.values()) {
      if (!filter.test(configEntry)) {
        continue;
      }

      VerticalLayout wrapper = configInputWrappers.get(configEntry);
      if (wrapper != null) {
        configFieldsContainer.add(wrapper);
      }
    }
  }

  private List<ConfigEntry> getVisibleConfigEntries() {
    if (selectedGroup == null) {
      return List.of();
    }

    Predicate<ConfigEntry> filter = configGroupFilters.get(selectedGroup);
    if (filter == null) {
      return List.of();
    }

    return Arrays.stream(ConfigEntry.values()).filter(filter).toList();
  }

  private void clearValidationStatesForVisibleInputs() {
    for (ConfigEntry configEntry : getVisibleConfigEntries()) {
      Component input = configInputs.get(configEntry);
      if (input != null) {
        clearValidationState(input);
      }
    }
  }

  private void refreshCurrentGroupTitle() {
    String breadcrumb =
        messageProperties.getAdminHeaderTitle() + " / " + messageProperties.getAdminConfigTitle();
    if (selectedGroup != null) {
      String groupLabel = messageProperties.getAdminConfigGroupLabel(selectedGroup);
      currentGroupTitle.setText(groupLabel);
      currentGroupDescription.setText(
          messageProperties.getAdminConfigGroupDescription(selectedGroup));
      currentGroupSettingCount.setText(
          messageProperties.getAdminConfigSettingsCount(getVisibleConfigEntries().size()));
      currentGroupIcon.setIcon(ConfigGroupIcons.forGroup(selectedGroup));
      breadcrumb += " / " + groupLabel;
    }
    configurationBreadcrumb.setText(breadcrumb);
  }

  private void refreshInputLabels() {
    configLabelSpans.forEach(
        (configEntry, labelSpan) ->
            labelSpan.setText(messageProperties.getAdminConfigEntryLabel(configEntry)));
  }

  private void refreshInputHelp() {
    configInputs.forEach(
        (configEntry, input) -> updatePlaceholder(input, buildPlaceholder(configEntry)));
    configInfoButtons.values().forEach(ConfigInfoButton::refreshText);
  }

  private void refreshValidationMessagesForInvalidFields() {
    for (ConfigEntry configEntry : getVisibleConfigEntries()) {
      Component input = configInputs.get(configEntry);
      if (isInputInvalid(input)) {
        String errorMessage = validateValue(configEntry, extractInputValue(configEntry));
        if (errorMessage != null) {
          setValidationError(input, errorMessage);
        }
      }
    }
  }

  private void updatePlaceholder(Component input, String placeholder) {
    String safePlaceholder = placeholder == null ? "" : placeholder;
    switch (input) {
      case EmailField field -> field.setPlaceholder(safePlaceholder);
      case PasswordField field -> field.setPlaceholder(safePlaceholder);
      case IntegerField field -> field.setPlaceholder(safePlaceholder);
      case TextField field -> field.setPlaceholder(safePlaceholder);
      default -> {
        /* Checkboxen haben keinen Platzhalter. */
      }
    }
  }

  private void registerConfigGroups() {
    configGroupFilters.put(AdminConfigGroup.GENERAL, this::isGeneralConfig);
    configGroupFilters.put(AdminConfigGroup.SECURITY, this::isSecurityConfig);
    configGroupFilters.put(AdminConfigGroup.TOKENS, this::isTokenConfig);
    configGroupFilters.put(AdminConfigGroup.USER_STATUS, this::isUserStatusConfig);
    configGroupFilters.put(
        AdminConfigGroup.NOTIFICATIONS,
        entry ->
            entry == ConfigEntry.USER_NOTIFICATIONS_INBOX_RETENTION_DAYS
                || entry == ConfigEntry.USER_NOTIFICATIONS_HISTORY_RETENTION_DAYS);
    configGroupFilters.put(AdminConfigGroup.EMAIL_SENDER, this::isEmailSenderConfig);
    configGroupFilters.put(AdminConfigGroup.SMTP, this::isSmtpConfig);
    configGroupFilters.put(AdminConfigGroup.QUEUE, this::isQueueConfig);
  }

  private VerticalLayout createConfigInput(ConfigEntry configEntry) {
    Object defaultValue = configEntry.getDefaultValue();
    Component input;

    if (isBooleanLikeEntry(configEntry, defaultValue)) {
      Checkbox checkbox = new Checkbox();
      checkbox.setWidthFull();
      input = checkbox;
    } else if (isEmailEntry(configEntry)) {
      EmailField emailField = new EmailField();
      emailField.setPattern(SMTP_EMAIL_REGEX);
      emailField.setClearButtonVisible(true);
      emailField.setWidthFull();
      input = emailField;
    } else if (defaultValue instanceof Integer) {
      IntegerField integerField = new IntegerField();
      integerField.setWidthFull();
      integerField.setStepButtonsVisible(true);
      integerField.setClearButtonVisible(true);
      integerField.setMin(configEntry == ConfigEntry.EMAIL_QUEUE_MAX_RETRY ? 0 : 1);

      if (configEntry == ConfigEntry.MAIL_PORT) {
        integerField.setMin(1);
        integerField.setMax(65535);
      }

      input = integerField;
    } else if (configEntry == ConfigEntry.REMEMBER_ME_SECRET_KEY
        || configEntry == ConfigEntry.MAIL_PASSWORD) {
      PasswordField passwordField = new PasswordField();
      passwordField.setRevealButtonVisible(true);
      passwordField.setClearButtonVisible(true);
      if (configEntry == ConfigEntry.REMEMBER_ME_SECRET_KEY) {
        passwordField.setMinLength(SECRET_KEY_MIN_LENGTH);
      }
      passwordField.setWidthFull();
      input = passwordField;
    } else {
      TextField textField = new TextField();
      textField.setClearButtonVisible(true);
      textField.setWidthFull();
      input = textField;
    }

    updatePlaceholder(input, buildPlaceholder(configEntry));
    installValidationResetListener(input);
    configInputs.put(configEntry, input);

    return createFieldWrapper(input, configEntry);
  }

  private VerticalLayout createFieldWrapper(Component input, ConfigEntry configEntry) {
    VerticalLayout wrapper = new VerticalLayout();
    wrapper.setPadding(false);
    wrapper.setSpacing(false);
    wrapper.setWidthFull();
    wrapper.addClassNames(LumoUtility.Gap.XSMALL, "cosmos-config-field");

    Span labelSpan = new Span(messageProperties.getAdminConfigEntryLabel(configEntry));
    labelSpan.setId("config-label-" + configEntry.name().toLowerCase(Locale.ROOT));
    labelSpan.addClassNames(LumoUtility.FontWeight.MEDIUM, LumoUtility.FontSize.SMALL);
    configLabelSpans.put(configEntry, labelSpan);
    if (input instanceof Checkbox checkbox) {
      checkbox.setLabelComponent(labelSpan);
    } else {
      HorizontalLayout headerLayout = new HorizontalLayout();
      headerLayout.setPadding(false);
      headerLayout.setSpacing(false);
      headerLayout.setWidthFull();
      headerLayout.add(labelSpan);
      wrapper.add(headerLayout);
    }
    String labelId = labelSpan.getId().orElseThrow();
    if (input instanceof HasAriaLabel labelledInput) {
      labelledInput.setAriaLabelledBy(labelId);
    }

    ConfigInfoButton infoButton = new ConfigInfoButton(() -> buildConfigTooltipText(configEntry));
    infoButton.getElement().setAttribute("aria-labelledby", labelId);
    configInfoButtons.put(configEntry, infoButton);
    Div inputRow = new Div(input, infoButton);
    inputRow.addClassName("cosmos-config-input-row");
    wrapper.add(inputRow);
    return wrapper;
  }

  private String buildConfigTooltipText(ConfigEntry configEntry) {
    StringBuilder text =
        new StringBuilder(messageProperties.getAdminConfigEntryTooltip(configEntry));
    text.append("\n\n").append(buildDefaultText(configEntry));
    text.append("\n\n").append(messageProperties.getAdminConfigInputRules()).append(":");
    buildInputRules(configEntry).forEach(rule -> text.append("\n• ").append(rule));
    return text.toString();
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
      case REMEMBER_ME_DURATION ->
          List.of(
              messageProperties.getAdminConfigRuleInteger(1, Integer.MAX_VALUE),
              messageProperties.getAdminConfigHelperSeconds(),
              messageProperties.getAdminConfigRuleEmptyUsesDefault());
      case LOGIN_ATTEMPTS_LIMIT,
          MAX_SESSIONS_PER_USER,
          EMAIL_QUEUE_POOL_SIZE,
          EMAIL_QUEUE_CAPACITY ->
          List.of(
              messageProperties.getAdminConfigRuleInteger(1, Integer.MAX_VALUE),
              messageProperties.getAdminConfigRuleEmptyUsesDefault());
      case USER_NOTIFICATIONS_INBOX_RETENTION_DAYS, USER_NOTIFICATIONS_HISTORY_RETENTION_DAYS ->
          List.of(
              messageProperties.getAdminConfigRuleInteger(1, Integer.MAX_VALUE),
              messageProperties.getAdminConfigRuleEmptyUnlimited());
      case EMAIL_QUEUE_MAX_RETRY ->
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
              messageProperties.getAdminConfigHelperDurationFormats(),
              messageProperties.getAdminConfigRuleDurationNoMinimum());
      case EMAIL_QUEUE_SENT_LIVE_DURATION ->
          List.of(
              messageProperties.getAdminConfigHelperDurationFormats(),
              messageProperties.getAdminConfigValidationPositiveDuration());
      case VERIFICATION_TOKEN_VALID_DURATION,
          VERIFICATION_TOKEN_LIVE_DURATION,
          PASSWORD_RESET_TOKEN_VALID_DURATION,
          PASSWORD_RESET_TOKEN_LIVE_DURATION ->
          List.of(
              messageProperties.getAdminConfigHelperDurationFormats(),
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
      boolean configurationChanged = false;
      int changedEntries = 0;

      for (ConfigEntry configEntry : getVisibleConfigEntries()) {
        String newValue = normalizeValue(configEntry, extractInputValue(configEntry));
        String oldValue = configService.getString(configEntry);

        if ((configEntry == ConfigEntry.SERVICE_NAME || configEntry == ConfigEntry.MAINTENANCE_MODE)
            && !Objects.equals(oldValue, newValue)) {
          configurationChanged = true;
        }

        configService.set(configEntry, newValue);
        if (!Objects.equals(oldValue, newValue)) {
          changedEntries++;
        }
      }

      loadConfigValues();
      if (changedEntries > 0) {
        logger.info(
            "Configuration group stored: group={}, changedEntries={}",
            selectedGroup,
            changedEntries);
      }

      if (configurationChanged && configurationChangedAction != null) {
        configurationChangedAction.run();
      }

      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getBaseSuccessTitle,
              messageProperties::getAdminConfigSaveSuccess,
              NotificationHelper.NotificationType.SUCCESS);
    } catch (Exception ex) {
      logger.error(
          "Configuration group update interrupted: group={}, individual entries may already be committed",
          selectedGroup,
          safeFailure(ex));
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getBaseFailedTitle,
              messageProperties::getBaseFailedMessage,
              -1,
              NotificationHelper.NotificationType.ERROR);
    }
  }

  private boolean validateVisibleInputs() {
    boolean allValid = true;
    Component firstInvalid = null;
    ConfigEntry firstInvalidEntry = null;

    for (ConfigEntry configEntry : getVisibleConfigEntries()) {
      ValidationResult result = validateInput(configEntry);

      if (result.invalid()) {
        if (firstInvalid == null) {
          firstInvalid = result.input();
          firstInvalidEntry = configEntry;
        }
        allValid = false;
      }
    }

    if (firstInvalid != null) {
      logger.debug("Configuration validation rejected field: key={}", firstInvalidEntry.name());
      firstInvalid.getElement().callJsFunction("focus");
    }

    return allValid;
  }

  private ValidationResult validateInput(ConfigEntry configEntry) {
    Component input = configInputs.get(configEntry);
    if (input == null) {
      return ValidationResult.valid();
    }

    clearValidationState(input);

    String rawValue = extractInputValue(configEntry);
    String errorMessage = validateValue(configEntry, rawValue);

    if (errorMessage == null) {
      return ValidationResult.valid();
    }

    setValidationError(input, errorMessage);
    logger.debug("Configuration validation rejected field: key={}", configEntry.name());
    return ValidationResult.invalid(input);
  }

  private record ValidationResult(boolean invalid, Component input) {
    private static ValidationResult valid() {
      return new ValidationResult(false, null);
    }

    private static ValidationResult invalid(Component input) {
      return new ValidationResult(true, input);
    }
  }

  private String validateValue(ConfigEntry configEntry, String value) {
    String trimmed = value == null ? "" : value.trim();
    if (isNotificationRetention(configEntry) && trimmed.isEmpty()) {
      return null;
    }

    return switch (configEntry) {
      case SERVICE_NAME -> {
        if (trimmed.isBlank()) {
          yield messageProperties.getAdminConfigValidationServiceName();
        }
        if (trimmed.length() > SERVICE_NAME_MAX_LENGTH) {
          yield messageProperties.getAdminConfigValidationServiceNameMax(SERVICE_NAME_MAX_LENGTH);
        }
        yield null;
      }
      case BASE_URL ->
          !isValidHttpUrl(trimmed) ? messageProperties.getAdminConfigValidationUrl() : null;
      case REMEMBER_ME_DURATION ->
          isInvalidPositiveInteger(trimmed)
              ? messageProperties.getAdminConfigValidationSeconds()
              : null;
      case REMEMBER_ME_SECRET_KEY ->
          trimmed.length() < SECRET_KEY_MIN_LENGTH
              ? messageProperties.getAdminConfigValidationSecretKey(SECRET_KEY_MIN_LENGTH)
              : null;
      case USER_AUTO_ABSENT_TIMEOUT ->
          !isValidSpringDuration(trimmed)
              ? messageProperties.getAdminConfigValidationDuration()
              : null;
      case EMAIL_QUEUE_SENT_LIVE_DURATION -> {
        if (!isValidSpringDuration(trimmed)) {
          yield messageProperties.getAdminConfigValidationDuration();
        }
        Duration duration = DurationStyle.detectAndParse(trimmed);
        yield duration.isZero() || duration.isNegative()
            ? messageProperties.getAdminConfigValidationPositiveDuration()
            : null;
      }
      case VERIFICATION_TOKEN_VALID_DURATION,
          VERIFICATION_TOKEN_LIVE_DURATION,
          PASSWORD_RESET_TOKEN_VALID_DURATION,
          PASSWORD_RESET_TOKEN_LIVE_DURATION -> {
        Duration minimum = configEntry.getMinimumTokenDuration();
        if (!isValidSpringDuration(trimmed)) {
          yield messageProperties.getAdminConfigValidationTokenDuration(minimum.toMinutes());
        }
        yield TokenDurationPolicy.isAtLeast(DurationStyle.detectAndParse(trimmed), minimum)
            ? null
            : messageProperties.getAdminConfigValidationDurationMinimum(minimum.toMinutes());
      }
      case LOGIN_ATTEMPTS_LIMIT,
          MAX_SESSIONS_PER_USER,
          EMAIL_QUEUE_POOL_SIZE,
          EMAIL_QUEUE_CAPACITY,
          USER_NOTIFICATIONS_INBOX_RETENTION_DAYS,
          USER_NOTIFICATIONS_HISTORY_RETENTION_DAYS ->
          isInvalidPositiveInteger(trimmed)
              ? messageProperties.getAdminConfigValidationMinOne()
              : null;
      case EMAIL_QUEUE_MAX_RETRY ->
          !isValidNonNegativeInteger(trimmed)
              ? messageProperties.getAdminConfigValidationNonNegative()
              : null;
      case EMAIL_FROM, EMAIL_ADMIN ->
          !isValidEmail(trimmed) ? messageProperties.getAdminConfigValidationEmail() : null;
      case MAIL_HOST ->
          isInvalidHost(trimmed) ? messageProperties.getAdminConfigValidationHost() : null;
      case MAIL_PORT ->
          !isValidPort(trimmed) ? messageProperties.getAdminConfigValidationPort() : null;
      case MAIL_SMTP_SSL_TRUST ->
          !isValidSslTrustValue(trimmed) ? messageProperties.getAdminConfigValidationHost() : null;
      default -> null;
    };
  }

  private String normalizeValue(ConfigEntry configEntry, String value) {
    String trimmed = value == null ? "" : value.trim();

    if (DURATION_ENTRIES.contains(configEntry) && !trimmed.isBlank()) {
      return DurationStyle.detectAndParse(trimmed).toString();
    }

    return trimmed;
  }

  private String extractInputValue(ConfigEntry configEntry) {
    Component input = configInputs.get(configEntry);

    if (input instanceof Checkbox checkbox) {
      return String.valueOf(checkbox.getValue());
    }

    if (input instanceof IntegerField integerField) {
      Integer value = integerField.getValue();
      if (value != null) {
        return String.valueOf(value);
      }
      return isNotificationRetention(configEntry) ? "" : configuredDefault(configEntry);
    }

    if (input instanceof PasswordField passwordField) {
      return passwordField.getValue() == null ? "" : passwordField.getValue();
    }

    if (input instanceof EmailField emailField) {
      return emailField.getValue() == null ? "" : emailField.getValue();
    }

    if (input instanceof TextField textField) {
      return textField.getValue() == null ? "" : textField.getValue();
    }

    return configEntry.getDefaultValueString();
  }

  private String configuredDefault(ConfigEntry configEntry) {
    String value = configService.getDefaultValue(configEntry);
    return value == null ? configEntry.getDefaultValueString() : value;
  }

  private boolean isSecretEntry(ConfigEntry configEntry) {
    return configEntry == ConfigEntry.REMEMBER_ME_SECRET_KEY
        || configEntry == ConfigEntry.MAIL_PASSWORD;
  }

  private boolean isNotificationRetention(ConfigEntry entry) {
    return entry == ConfigEntry.USER_NOTIFICATIONS_INBOX_RETENTION_DAYS
        || entry == ConfigEntry.USER_NOTIFICATIONS_HISTORY_RETENTION_DAYS;
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
    String defaultValue = configuredDefault(configEntry);
    String displayValue = defaultValue;
    if (defaultValue.isEmpty()) {
      displayValue = messageProperties.getAdminConfigDefaultEmpty();
    } else if (isSecretEntry(configEntry)) {
      displayValue = messageProperties.getAdminConfigDefaultConfidential();
    }
    return (isNotificationRetention(configEntry)
            ? messageProperties.getAdminConfigInitialValue()
            : messageProperties.getAdminConfigDefault())
        + ": "
        + displayValue;
  }

  private void installValidationResetListener(Component input) {
    if (input instanceof EmailField emailField) {
      emailField.addValueChangeListener(event -> clearValidationState(emailField));
    } else if (input instanceof PasswordField passwordField) {
      passwordField.addValueChangeListener(event -> clearValidationState(passwordField));
    } else if (input instanceof TextField textField) {
      textField.addValueChangeListener(event -> clearValidationState(textField));
    } else if (input instanceof IntegerField integerField) {
      integerField.addValueChangeListener(event -> clearValidationState(integerField));
    } else if (input instanceof Checkbox checkbox) {
      checkbox.addValueChangeListener(event -> clearValidationState(checkbox));
    }
  }

  private void clearValidationState(Component input) {
    if (input instanceof EmailField emailField) {
      emailField.setInvalid(false);
      emailField.setErrorMessage(null);
    } else if (input instanceof PasswordField passwordField) {
      passwordField.setInvalid(false);
      passwordField.setErrorMessage(null);
    } else if (input instanceof TextField textField) {
      textField.setInvalid(false);
      textField.setErrorMessage(null);
    } else if (input instanceof IntegerField integerField) {
      integerField.setInvalid(false);
      integerField.setErrorMessage(null);
    }
  }

  private void setValidationError(Component input, String errorMessage) {
    if (input instanceof EmailField emailField) {
      emailField.setInvalid(true);
      emailField.setErrorMessage(errorMessage);
    } else if (input instanceof PasswordField passwordField) {
      passwordField.setInvalid(true);
      passwordField.setErrorMessage(errorMessage);
    } else if (input instanceof TextField textField) {
      textField.setInvalid(true);
      textField.setErrorMessage(errorMessage);
    } else if (input instanceof IntegerField integerField) {
      integerField.setInvalid(true);
      integerField.setErrorMessage(errorMessage);
    }
  }

  private boolean isInputInvalid(Component input) {
    if (input instanceof EmailField emailField) {
      return emailField.isInvalid();
    }
    if (input instanceof PasswordField passwordField) {
      return passwordField.isInvalid();
    }
    if (input instanceof TextField textField) {
      return textField.isInvalid();
    }
    if (input instanceof IntegerField integerField) {
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

  private boolean isValidHttpUrl(String value) {
    if (value == null || value.isBlank()) {
      return false;
    }

    try {
      URI uri = new URI(value);
      return uri.getScheme() != null
          && uri.getHost() != null
          && ("http".equalsIgnoreCase(uri.getScheme())
              || "https".equalsIgnoreCase(uri.getScheme()));
    } catch (URISyntaxException _) {
      return false;
    }
  }

  private boolean isValidSpringDuration(String value) {
    if (value == null) {
      return false;
    }

    String trimmed = value.trim();
    if (trimmed.isBlank()) {
      return false;
    }

    if (DURATION_NUMBER_ONLY_PATTERN.matcher(trimmed).matches()) {
      return false;
    }

    try {
      DurationStyle.detectAndParse(trimmed);
      return true;
    } catch (IllegalArgumentException _) {
      return false;
    }
  }

  private boolean isInvalidPositiveInteger(String value) {
    try {
      return Integer.parseInt(value) < 1;
    } catch (NumberFormatException _) {
      return true;
    }
  }

  private boolean isValidNonNegativeInteger(String value) {
    try {
      return Integer.parseInt(value) >= 0;
    } catch (NumberFormatException _) {
      return false;
    }
  }

  private boolean isValidEmail(String value) {
    return value != null && SMTP_EMAIL_PATTERN.matcher(value).matches();
  }

  private boolean isInvalidHost(String value) {
    return value == null
        || value.isBlank()
        || value.contains("://")
        || value.contains("/")
        || value.contains(" ");
  }

  private boolean isValidPort(String value) {
    try {
      int port = Integer.parseInt(value);
      return port >= 1 && port <= 65535;
    } catch (NumberFormatException _) {
      return false;
    }
  }

  private boolean isValidSslTrustValue(String value) {
    if (value == null || value.isBlank()) {
      return false;
    }

    if ("*".equals(value.trim())) {
      return true;
    }

    // Das Komma ist hier ein wörtliches Trennzeichen für die Hostliste, kein komplexes
    // Regex-Muster.
    String[] hosts = value.split(",");
    for (String host : hosts) {
      if (isInvalidHost(host.trim())) {
        return false;
      }
    }
    return true;
  }

  private boolean isGeneralConfig(ConfigEntry configEntry) {
    return configEntry == ConfigEntry.SERVICE_NAME
        || configEntry == ConfigEntry.COPYRIGHT_TEXT
        || configEntry == ConfigEntry.BASE_URL;
  }

  private boolean isSecurityConfig(ConfigEntry configEntry) {
    return configEntry == ConfigEntry.REMEMBER_ME_DURATION
        || configEntry == ConfigEntry.REMEMBER_ME_SECRET_KEY
        || configEntry == ConfigEntry.LOGIN_ATTEMPTS_LIMIT
        || configEntry == ConfigEntry.MAX_SESSIONS_PER_USER
        || configEntry == ConfigEntry.MAINTENANCE_MODE;
  }

  private boolean isTokenConfig(ConfigEntry configEntry) {
    return configEntry == ConfigEntry.VERIFICATION_TOKEN_VALID_DURATION
        || configEntry == ConfigEntry.VERIFICATION_TOKEN_LIVE_DURATION
        || configEntry == ConfigEntry.PASSWORD_RESET_TOKEN_VALID_DURATION
        || configEntry == ConfigEntry.PASSWORD_RESET_TOKEN_LIVE_DURATION;
  }

  private boolean isUserStatusConfig(ConfigEntry configEntry) {
    return configEntry == ConfigEntry.USER_AUTO_ABSENT_TIMEOUT;
  }

  private boolean isEmailSenderConfig(ConfigEntry configEntry) {
    return configEntry == ConfigEntry.EMAIL_FROM || configEntry == ConfigEntry.EMAIL_ADMIN;
  }

  private boolean isSmtpConfig(ConfigEntry configEntry) {
    return configEntry.name().startsWith("MAIL_");
  }

  private boolean isQueueConfig(ConfigEntry configEntry) {
    return configEntry.name().startsWith("EMAIL_QUEUE_");
  }
}
