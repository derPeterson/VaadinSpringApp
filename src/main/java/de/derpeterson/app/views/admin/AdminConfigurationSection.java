package de.derpeterson.app.views.admin;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H1;
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
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.ui.components.CardComponent;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.convert.DurationStyle;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class AdminConfigurationSection extends VerticalLayout {

    private static final Logger logger = LoggerFactory.getLogger(AdminConfigurationSection.class);

    private static final String CONFIG_GROUP_GENERAL = "adminView.config.group.general";
    private static final String CONFIG_GROUP_SECURITY = "adminView.config.group.security";
    private static final String CONFIG_GROUP_TOKENS = "adminView.config.group.tokens";
    private static final String CONFIG_GROUP_USER_STATUS = "adminView.config.group.user_status";
    private static final String CONFIG_GROUP_EMAIL_SENDER = "adminView.config.group.email_sender";
    private static final String CONFIG_GROUP_SMTP = "adminView.config.group.smtp";
    private static final String CONFIG_GROUP_QUEUE = "adminView.config.group.queue";

    private static final int SERVICE_NAME_MAX_LENGTH = 30;
    private static final int SECRET_KEY_MIN_LENGTH = 8;
    private static final Pattern DURATION_NUMBER_ONLY_PATTERN = Pattern.compile("^\\d+$");

    private static final Set<ConfigEntry> DURATION_ENTRIES = Set.of(
            ConfigEntry.USER_AUTO_ABSENT_TIMEOUT,
            ConfigEntry.VERIFICATION_TOKEN_VALID_DURATION,
            ConfigEntry.VERIFICATION_TOKEN_LIVE_DURATION,
            ConfigEntry.PASSWORD_RESET_TOKEN_VALID_DURATION,
            ConfigEntry.PASSWORD_RESET_TOKEN_LIVE_DURATION,
            ConfigEntry.EMAIL_QUEUE_SENT_LIVE_DURATION
    );

    private final transient MessageProperties messageProperties;
    private final transient ConfigService configService;
    private final transient Runnable serviceNameChangedAction;

    private final EnumMap<ConfigEntry, Component> configInputs = new EnumMap<>(ConfigEntry.class);
    private final EnumMap<ConfigEntry, VerticalLayout> configInputWrappers = new EnumMap<>(ConfigEntry.class);
    private final EnumMap<ConfigEntry, Span> configLabelSpans = new EnumMap<>(ConfigEntry.class);
    private final EnumMap<ConfigEntry, Span> configMetaTextSpans = new EnumMap<>(ConfigEntry.class);
    private final Map<String, Predicate<ConfigEntry>> configGroupFilters = new LinkedHashMap<>();

    private final H1 configurationTitle;
    private final Span configurationDescription;
    private final H2 currentGroupTitle;
    private final VerticalLayout configFieldsContainer;

    private final Button saveConfigButton;
    private final Button reloadConfigButton;

    private String selectedGroupKey = null;

    public AdminConfigurationSection(MessageProperties messageProperties,
                                     ConfigService configService,
                                     Runnable serviceNameChangedAction) {
        this.messageProperties = messageProperties;
        this.configService = configService;
        this.serviceNameChangedAction = serviceNameChangedAction;

        setPadding(false);
        setSpacing(true);
        setWidthFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        addClassNames(LumoUtility.Gap.MEDIUM);

        configurationTitle = new H1();
        configurationTitle.addClassNames(
                LumoUtility.Margin.NONE,
                LumoUtility.FontSize.XXLARGE
        );

        configurationDescription = new Span();
        configurationDescription.addClassNames(LumoUtility.TextColor.SECONDARY);

        currentGroupTitle = new H2();
        currentGroupTitle.addClassNames(
                LumoUtility.FontSize.XLARGE,
                LumoUtility.FontWeight.SEMIBOLD
        );

        configFieldsContainer = new VerticalLayout();
        configFieldsContainer.setPadding(false);
        configFieldsContainer.setSpacing(true);
        configFieldsContainer.setWidthFull();

        registerConfigGroups();

        for (ConfigEntry configEntry : ConfigEntry.values()) {
            configInputWrappers.put(configEntry, createConfigInput(configEntry));
        }

        saveConfigButton = new Button();
        saveConfigButton.setPrefixComponent(VaadinIcon.CHECK.create());
        saveConfigButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveConfigButton.addClickListener(clickEvent -> saveConfigValues());
        saveConfigButton.addClickShortcut(Key.ENTER);
        saveConfigButton.setWidthFull();

        reloadConfigButton = new Button();
        reloadConfigButton.setPrefixComponent(VaadinIcon.REFRESH.create());
        reloadConfigButton.addClickListener(clickEvent -> reloadConfigValues());
        reloadConfigButton.setWidthFull();

        VerticalLayout configLayout = getButtonVerticalLayout();

        CardComponent configCard = new CardComponent(configLayout);
        configCard.setWidthFull();
        configCard.setMaxWidth("980px");

        add(configCard);

        if (!configGroupFilters.isEmpty()) {
            selectedGroupKey = configGroupFilters.keySet().iterator().next();
        }

        refreshTexts();
        renderSelectedGroup();
    }

    private @NonNull VerticalLayout getButtonVerticalLayout() {
        HorizontalLayout saveWrapper = new HorizontalLayout(saveConfigButton);
        saveWrapper.setPadding(false);
        saveWrapper.setSpacing(false);
        saveWrapper.setWidthFull();

        HorizontalLayout reloadWrapper = new HorizontalLayout(reloadConfigButton);
        reloadWrapper.setPadding(false);
        reloadWrapper.setSpacing(false);
        reloadWrapper.setWidthFull();

        HorizontalLayout buttonLayout = new HorizontalLayout(saveWrapper, reloadWrapper);
        buttonLayout.setPadding(false);
        buttonLayout.setSpacing(true);
        buttonLayout.setWidthFull();
        buttonLayout.expand(saveWrapper, reloadWrapper);

        VerticalLayout configLayout = new VerticalLayout(
                configurationTitle,
                configurationDescription,
                currentGroupTitle,
                configFieldsContainer,
                buttonLayout
        );
        configLayout.setPadding(false);
        configLayout.setSpacing(true);
        configLayout.setAlignItems(Alignment.START);
        return configLayout;
    }

    public void refreshTexts() {
        configurationTitle.setText(messageProperties.getAdminConfigTitle());
        configurationDescription.setText(messageProperties.getAdminConfigDescription());
        saveConfigButton.setText(messageProperties.getAdminConfigSave());
        reloadConfigButton.setText(messageProperties.getAdminConfigReload());

        refreshInputLabels();
        refreshInputMetaTexts();
        refreshCurrentGroupTitle();
        refreshValidationMessagesForInvalidFields();
    }

    public void loadConfigValues() {
        for (ConfigEntry configEntry : ConfigEntry.values()) {
            Component input = configInputs.get(configEntry);
            if (input == null) {
                continue;
            }

            String safeValue = getSafeConfigValue(configEntry);
            applyConfigValue(input, configEntry, safeValue);
        }

        clearAllValidationStates();
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

    private void setIntegerFieldValue(IntegerField integerField, ConfigEntry configEntry, String safeValue) {
        try {
            integerField.setValue(Integer.parseInt(safeValue));
        } catch (NumberFormatException ex) {
            integerField.setValue(configEntry.getDefaultValueInteger());
        }
    }

    public void showGroup(String titleKey) {
        if (titleKey == null || !configGroupFilters.containsKey(titleKey)) {
            return;
        }

        selectedGroupKey = titleKey;
        renderSelectedGroup();
    }

    private void reloadConfigValues() {
        loadConfigValues();
        NotificationHelper.getInstance().showNotification(
                messageProperties::getBaseSuccessTitle,
                messageProperties::getAdminConfigReloadSuccess,
                NotificationHelper.NotificationType.SUCCESS
        );
    }

    private void renderSelectedGroup() {
        if (selectedGroupKey == null || !configGroupFilters.containsKey(selectedGroupKey)) {
            if (configGroupFilters.isEmpty()) {
                return;
            }
            selectedGroupKey = configGroupFilters.keySet().iterator().next();
        }

        refreshCurrentGroupTitle();
        configFieldsContainer.removeAll();

        Predicate<ConfigEntry> filter = configGroupFilters.get(selectedGroupKey);
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

    private void refreshCurrentGroupTitle() {
        if (selectedGroupKey != null) {
            currentGroupTitle.setText(getConfigGroupLabel(selectedGroupKey));
        }
    }

    private void refreshInputLabels() {
        configLabelSpans.forEach((configEntry, labelSpan) ->
                labelSpan.setText(messageProperties.getAdminConfigEntryLabel(configEntry)));
    }

    private void refreshInputMetaTexts() {
        configMetaTextSpans.forEach((configEntry, metaTextSpan) ->
                metaTextSpan.setText(buildMetaText(configEntry)));

        configInputs.forEach((configEntry, input) ->
                updatePlaceholder(input, buildPlaceholder(configEntry)));
    }

    private void refreshValidationMessagesForInvalidFields() {
        configInputs.forEach((configEntry, input) -> {
            if (isInputInvalid(input)) {
                String errorMessage = validateValue(configEntry, extractInputValue(configEntry));
                if (errorMessage != null) {
                    setValidationError(input, errorMessage);
                }
            }
        });
    }

    private void updatePlaceholder(Component input, String placeholder) {
        String safePlaceholder = placeholder == null ? "" : placeholder;

        if (input instanceof EmailField emailField) {
            emailField.setPlaceholder(safePlaceholder);
        } else if (input instanceof TextField textField) {
            textField.setPlaceholder(safePlaceholder);
        }
    }

    private void registerConfigGroups() {
        configGroupFilters.put(CONFIG_GROUP_GENERAL, this::isGeneralConfig);
        configGroupFilters.put(CONFIG_GROUP_SECURITY, this::isSecurityConfig);
        configGroupFilters.put(CONFIG_GROUP_TOKENS, this::isTokenConfig);
        configGroupFilters.put(CONFIG_GROUP_USER_STATUS, this::isUserStatusConfig);
        configGroupFilters.put(CONFIG_GROUP_EMAIL_SENDER, this::isEmailSenderConfig);
        configGroupFilters.put(CONFIG_GROUP_SMTP, this::isSmtpConfig);
        configGroupFilters.put(CONFIG_GROUP_QUEUE, this::isQueueConfig);
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
            emailField.setClearButtonVisible(true);
            emailField.setWidthFull();
            input = emailField;
        } else if (defaultValue instanceof Integer) {
            IntegerField integerField = new IntegerField();
            integerField.setWidthFull();
            integerField.setStepButtonsVisible(true);
            integerField.setClearButtonVisible(true);
            integerField.setMin(0);

            if (configEntry == ConfigEntry.MAIL_PORT) {
                integerField.setMin(1);
                integerField.setMax(65535);
            }

            input = integerField;
        } else if (configEntry == ConfigEntry.REMEMBER_ME_SECRET_KEY || configEntry == ConfigEntry.MAIL_PASSWORD) {
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
        wrapper.addClassNames(LumoUtility.Gap.XSMALL);

        HorizontalLayout headerLayout = createFieldHeader(configEntry);
        Span metaTextSpan = createMetaTextSpan(configEntry);

        wrapper.add(headerLayout, input, metaTextSpan);
        return wrapper;
    }

    private Span createMetaTextSpan(ConfigEntry configEntry) {
        Span metaTextSpan = new Span(buildMetaText(configEntry));
        metaTextSpan.addClassNames(
                LumoUtility.FontSize.SMALL,
                LumoUtility.TextColor.SECONDARY
        );
        configMetaTextSpans.put(configEntry, metaTextSpan);
        return metaTextSpan;
    }

    private HorizontalLayout createFieldHeader(ConfigEntry configEntry) {
        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setPadding(false);
        headerLayout.setSpacing(true);
        headerLayout.setWidthFull();
        headerLayout.setAlignItems(Alignment.CENTER);
        headerLayout.addClassNames(LumoUtility.Gap.SMALL);

        Span labelSpan = new Span(messageProperties.getAdminConfigEntryLabel(configEntry));
        labelSpan.addClassNames(
                LumoUtility.FontWeight.MEDIUM,
                LumoUtility.FontSize.SMALL
        );
        configLabelSpans.put(configEntry, labelSpan);

        Component infoPopover = createInfoPopover(configEntry);

        headerLayout.add(labelSpan, infoPopover);

        return headerLayout;
    }

    private Component createInfoPopover(ConfigEntry configEntry) {
        Icon icon = createTooltipIcon();

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(messageProperties.getAdminConfigEntryLabel(configEntry));
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(true);
        dialog.setDraggable(true);
        dialog.setResizable(true);
        dialog.getElement().getStyle().set("border-radius", "12px");

        Span content = new Span(messageProperties.getAdminConfigEntryTooltip(configEntry));
        content.getStyle()
                .set("white-space", "normal")
                .set("line-height", "1.5");

        Button closeButton = new Button(messageProperties.getOkayHomeButton(), e -> dialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout footer = new HorizontalLayout(closeButton);
        footer.setWidthFull();
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        footer.setPadding(false);
        footer.setSpacing(false);

        VerticalLayout layout = new VerticalLayout(content, footer);
        layout.setSpacing(true);
        layout.setPadding(true);

        dialog.add(layout);

        icon.addClickListener(e -> dialog.open());

        return icon;
    }

    private Icon createTooltipIcon() {
        Icon icon = VaadinIcon.INFO_CIRCLE_O.create();
        icon.addClassNames(
                LumoUtility.TextColor.SECONDARY,
                LumoUtility.Margin.Left.XSMALL
        );
        icon.getStyle().set("cursor", "help");
        icon.getStyle().set("flex-shrink", "0");
        icon.getStyle().set("width", "16px");
        icon.getStyle().set("height", "16px");
        icon.getStyle().set("margin-top", "1px");
        return icon;
    }

    private void saveConfigValues() {
        if (!validateAllInputs()) {
            NotificationHelper.getInstance().showNotification(
                    messageProperties::getBaseFailedTitle,
                    messageProperties::getAdminConfigValidationInvalidFields,
                    NotificationHelper.NotificationType.ERROR
            );
            return;
        }

        try {
            boolean serviceNameChanged = false;

            for (ConfigEntry configEntry : ConfigEntry.values()) {
                String newValue = normalizeValue(configEntry, extractInputValue(configEntry));
                String oldValue = configService.getString(configEntry);

                if (configEntry == ConfigEntry.SERVICE_NAME && !Objects.equals(oldValue, newValue)) {
                    serviceNameChanged = true;
                }

                configService.set(configEntry, newValue);
            }

            loadConfigValues();

            if (serviceNameChanged && serviceNameChangedAction != null) {
                serviceNameChangedAction.run();
            }

            NotificationHelper.getInstance().showNotification(
                    messageProperties::getBaseSuccessTitle,
                    messageProperties::getAdminConfigSaveSuccess,
                    NotificationHelper.NotificationType.SUCCESS
            );
        } catch (Exception ex) {
            logger.error("❌ Failed to store configuration values.", ex);
            NotificationHelper.getInstance().showNotification(
                    messageProperties::getBaseFailedTitle,
                    messageProperties::getBaseFailedMessage,
                    -1,
                    NotificationHelper.NotificationType.ERROR
            );
        }
    }


    private boolean validateAllInputs() {
        boolean allValid = true;
        Component firstInvalid = null;
        ConfigEntry firstInvalidEntry = null;

        for (ConfigEntry configEntry : ConfigEntry.values()) {
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
            logger.warn("⚠️ First invalid config field: {}", firstInvalidEntry.name());
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
        logger.warn("⚠️ Invalid config field: {} with value '{}'", configEntry.name(), rawValue);
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
            case BASE_URL -> !isValidHttpUrl(trimmed)
                    ? messageProperties.getAdminConfigValidationUrl()
                    : null;
            case REMEMBER_ME_DURATION -> isInvalidPositiveInteger(trimmed)
                    ? messageProperties.getAdminConfigValidationSeconds()
                    : null;
            case REMEMBER_ME_SECRET_KEY -> trimmed.length() < SECRET_KEY_MIN_LENGTH
                    ? messageProperties.getAdminConfigValidationSecretKey(SECRET_KEY_MIN_LENGTH)
                    : null;
            case USER_AUTO_ABSENT_TIMEOUT,
                 VERIFICATION_TOKEN_VALID_DURATION,
                 VERIFICATION_TOKEN_LIVE_DURATION,
                 PASSWORD_RESET_TOKEN_VALID_DURATION,
                 PASSWORD_RESET_TOKEN_LIVE_DURATION,
                 EMAIL_QUEUE_SENT_LIVE_DURATION -> !isValidSpringDuration(trimmed)
                    ? messageProperties.getAdminConfigValidationDuration()
                    : null;
            case LOGIN_ATTEMPTS_LIMIT,
                 MAX_SESSIONS_PER_USER,
                 EMAIL_QUEUE_POOL_SIZE,
                 EMAIL_QUEUE_CAPACITY -> isInvalidPositiveInteger(trimmed)
                    ? messageProperties.getAdminConfigValidationMinOne()
                    : null;
            case EMAIL_QUEUE_MAX_RETRY -> !isValidNonNegativeInteger(trimmed)
                    ? messageProperties.getAdminConfigValidationNonNegative()
                    : null;
            case EMAIL_FROM,
                 EMAIL_ADMIN -> !isValidEmail(trimmed)
                    ? messageProperties.getAdminConfigValidationEmail()
                    : null;
            case MAIL_HOST -> isInvalidHost(trimmed)
                    ? messageProperties.getAdminConfigValidationHost()
                    : null;
            case MAIL_PORT -> !isValidPort(trimmed)
                    ? messageProperties.getAdminConfigValidationPort()
                    : null;
            case MAIL_SMTP_SSL_TRUST -> !isValidSslTrustValue(trimmed)
                    ? messageProperties.getAdminConfigValidationHost()
                    : null;
            case MAIL_SMTP_SOCKETFACTORY_CLASS -> !isValidJavaClassName(trimmed)
                    ? messageProperties.getAdminConfigValidationJavaClass()
                    : null;
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
            return value == null
                    ? String.valueOf(configEntry.getDefaultValueInteger())
                    : String.valueOf(value);
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

    private String buildMetaText(ConfigEntry configEntry) {
        String defaultText = buildDefaultText(configEntry);

        if (DURATION_ENTRIES.contains(configEntry)) {
            return defaultText + " • " + messageProperties.getAdminConfigHelperDurationFormats();
        }

        if (configEntry == ConfigEntry.REMEMBER_ME_DURATION) {
            return defaultText + " • " + messageProperties.getAdminConfigHelperSeconds();
        }

        if (configEntry == ConfigEntry.REMEMBER_ME_SECRET_KEY) {
            return defaultText + " • " +
                    messageProperties.getAdminConfigHelperSecretKeyMin(SECRET_KEY_MIN_LENGTH);
        }

        if (configEntry == ConfigEntry.SERVICE_NAME) {
            return defaultText + " • " +
                    messageProperties.getAdminConfigHelperServiceNameMax(SERVICE_NAME_MAX_LENGTH);
        }

        return defaultText;
    }

    private String buildPlaceholder(ConfigEntry configEntry) {
        return switch (configEntry) {
            case BASE_URL -> messageProperties.getAdminConfigPlaceholderBaseUrl();
            case EMAIL_FROM, EMAIL_ADMIN -> messageProperties.getAdminConfigPlaceholderEmail();
            case MAIL_HOST -> messageProperties.getAdminConfigPlaceholderMailHost();
            case USER_AUTO_ABSENT_TIMEOUT,
                 VERIFICATION_TOKEN_VALID_DURATION,
                 VERIFICATION_TOKEN_LIVE_DURATION,
                 PASSWORD_RESET_TOKEN_VALID_DURATION,
                 PASSWORD_RESET_TOKEN_LIVE_DURATION,
                 EMAIL_QUEUE_SENT_LIVE_DURATION -> messageProperties.getAdminConfigPlaceholderDuration();
            default -> "";
        };
    }

    private String buildDefaultText(ConfigEntry configEntry) {
        return messageProperties.getAdminConfigDefault() + ": " + configEntry.getDefaultValueString();
    }

    private String getConfigGroupLabel(String groupKey) {
        return switch (groupKey) {
            case CONFIG_GROUP_GENERAL -> messageProperties.getAdminConfigGroupGeneral();
            case CONFIG_GROUP_SECURITY -> messageProperties.getAdminConfigGroupSecurity();
            case CONFIG_GROUP_TOKENS -> messageProperties.getAdminConfigGroupTokens();
            case CONFIG_GROUP_USER_STATUS -> messageProperties.getAdminConfigGroupUserStatus();
            case CONFIG_GROUP_EMAIL_SENDER -> messageProperties.getAdminConfigGroupEmailSender();
            case CONFIG_GROUP_SMTP -> messageProperties.getAdminConfigGroupSmtp();
            case CONFIG_GROUP_QUEUE -> messageProperties.getAdminConfigGroupQueue();
            default -> messageProperties.getTranslation(groupKey);
        };
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

    private void clearAllValidationStates() {
        configInputs.values().forEach(this::clearValidationState);
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
                    && ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()));
        } catch (URISyntaxException ex) {
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
        } catch (IllegalArgumentException ex) {
            logger.warn("⚠️ Invalid spring duration format: '{}'", trimmed, ex);
            return false;
        }
    }

    private boolean isInvalidPositiveInteger(String value) {
        try {
            return Integer.parseInt(value) >= 1;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private boolean isValidNonNegativeInteger(String value) {
        try {
            return Integer.parseInt(value) >= 0;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private boolean isValidEmail(String value) {
        return value != null && !value.isBlank() && value.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }

    private boolean isInvalidHost(String value) {
        return value != null
                && !value.isBlank()
                && !value.contains("://")
                && !value.contains("/")
                && !value.contains(" ");
    }

    private boolean isValidPort(String value) {
        try {
            int port = Integer.parseInt(value);
            return port >= 1 && port <= 65535;
        } catch (NumberFormatException ex) {
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

        String[] hosts = value.split(",");
        for (String host : hosts) {
            if (isInvalidHost(host.trim())) {
                return false;
            }
        }
        return true;
    }

    private boolean isValidJavaClassName(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }

        int start = 0;
        int length = value.length();

        while (start < length) {
            int dotIndex = value.indexOf('.', start);
            int end = dotIndex >= 0 ? dotIndex : length;

            if (start == end || !isValidJavaIdentifier(value, start, end)) {
                return false;
            }

            start = end + 1;
        }

        return true;
    }

    private boolean isValidJavaIdentifier(String value, int start, int end) {
        if (!Character.isJavaIdentifierStart(value.charAt(start))) {
            return false;
        }

        for (int i = start + 1; i < end; i++) {
            if (!Character.isJavaIdentifierPart(value.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    private boolean isGeneralConfig(ConfigEntry configEntry) {
        return configEntry == ConfigEntry.SERVICE_NAME
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
        return configEntry == ConfigEntry.EMAIL_FROM
                || configEntry == ConfigEntry.EMAIL_ADMIN;
    }

    private boolean isSmtpConfig(ConfigEntry configEntry) {
        return configEntry.name().startsWith("MAIL_");
    }

    private boolean isQueueConfig(ConfigEntry configEntry) {
        return configEntry.name().startsWith("EMAIL_QUEUE_");
    }
}