package de.derpeterson.app.views.admin;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasHelper;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.ui.components.CardComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

public class AdminConfigurationSection extends VerticalLayout {

    private static final Logger logger = LoggerFactory.getLogger(AdminConfigurationSection.class);

    private static final String CONFIG_GROUP_GENERAL = "adminView.config.group.general";
    private static final String CONFIG_GROUP_SECURITY = "adminView.config.group.security";
    private static final String CONFIG_GROUP_TOKENS = "adminView.config.group.tokens";
    private static final String CONFIG_GROUP_USER_STATUS = "adminView.config.group.user_status";
    private static final String CONFIG_GROUP_EMAIL_SENDER = "adminView.config.group.email_sender";
    private static final String CONFIG_GROUP_SMTP = "adminView.config.group.smtp";
    private static final String CONFIG_GROUP_QUEUE = "adminView.config.group.queue";

    private final transient MessageProperties messageProperties;
    private final transient ConfigService configService;
    private final transient Runnable serviceNameChangedAction;

    private final EnumMap<ConfigEntry, Component> configInputs = new EnumMap<>(ConfigEntry.class);
    private final EnumMap<ConfigEntry, VerticalLayout> configInputWrappers = new EnumMap<>(ConfigEntry.class);
    private final EnumMap<ConfigEntry, Tooltip> configTooltips = new EnumMap<>(ConfigEntry.class);
    private final EnumMap<ConfigEntry, Span> configLabelSpans = new EnumMap<>(ConfigEntry.class);
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

        reloadConfigButton = new Button();
        reloadConfigButton.setPrefixComponent(VaadinIcon.REFRESH.create());
        reloadConfigButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        reloadConfigButton.addClickListener(clickEvent -> reloadConfigValues());

        HorizontalLayout buttonLayout = new HorizontalLayout(saveConfigButton, reloadConfigButton);
        buttonLayout.setPadding(false);
        buttonLayout.setSpacing(true);
        buttonLayout.setWidthFull();
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

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

    public void refreshTexts() {
        configurationTitle.setText(messageProperties.getAdminConfigTitle());
        configurationDescription.setText(messageProperties.getAdminConfigDescription());
        saveConfigButton.setText(messageProperties.getAdminConfigSave());
        reloadConfigButton.setText(messageProperties.getAdminConfigReload());

        refreshInputLabels();
        refreshDefaultHelperTexts();
        refreshCurrentGroupTitle();
        refreshInputTooltips();
    }

    public void loadConfigValues() {
        for (ConfigEntry configEntry : ConfigEntry.values()) {
            Component input = configInputs.get(configEntry);
            if (input == null) {
                continue;
            }

            String value = configService.getString(configEntry);

            if (input instanceof Checkbox checkbox) {
                checkbox.setValue(Boolean.parseBoolean(value));
                continue;
            }

            if (input instanceof IntegerField integerField) {
                try {
                    integerField.setValue(Integer.parseInt(value));
                } catch (NumberFormatException ex) {
                    integerField.setValue(configEntry.getDefaultValueInteger());
                }
                continue;
            }

            if (input instanceof PasswordField passwordField) {
                passwordField.setValue(value == null ? "" : value);
                continue;
            }

            if (input instanceof TextField textField) {
                textField.setValue(value == null ? "" : value);
            }
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

    private void refreshDefaultHelperTexts() {
        configInputs.forEach((configEntry, input) ->
                updateHelperText(input, buildDefaultText(configEntry)));
    }

    private void refreshInputTooltips() {
        configTooltips.forEach((configEntry, tooltip) ->
                tooltip.setText(messageProperties.getAdminConfigEntryTooltip(configEntry)));
    }

    private void updateHelperText(Component input, String helperText) {
        if (input instanceof HasHelper hasHelper) {
            hasHelper.setHelperText(helperText);
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
        String helperText = buildDefaultText(configEntry);

        Component input;

        if (defaultValue instanceof Boolean) {
            Checkbox checkbox = new Checkbox();
            checkbox.setWidthFull();
            checkbox.setHelperText(helperText);
            input = checkbox;
        } else if (defaultValue instanceof Integer) {
            IntegerField integerField = new IntegerField();
            integerField.setWidthFull();
            integerField.setStepButtonsVisible(true);
            integerField.setClearButtonVisible(true);
            integerField.setHelperText(helperText);
            integerField.setMin(0);
            input = integerField;
        } else if (configEntry == ConfigEntry.REMEMBER_ME_SECRET_KEY || configEntry == ConfigEntry.MAIL_PASSWORD) {
            PasswordField passwordField = new PasswordField();
            passwordField.setRevealButtonVisible(true);
            passwordField.setClearButtonVisible(true);
            passwordField.setHelperText(helperText);
            passwordField.setWidthFull();
            input = passwordField;
        } else {
            TextField textField = new TextField();
            textField.setClearButtonVisible(true);
            textField.setHelperText(helperText);
            textField.setWidthFull();
            input = textField;
        }

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
        wrapper.add(headerLayout, input);

        return wrapper;
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

        Icon tooltipIcon = createTooltipIcon();
        Tooltip tooltip = Tooltip.forComponent(tooltipIcon);
        tooltip.setText(messageProperties.getAdminConfigEntryTooltip(configEntry));
        configTooltips.put(configEntry, tooltip);

        headerLayout.add(labelSpan, tooltipIcon);

        return headerLayout;
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
        try {
            boolean serviceNameChanged = false;

            for (ConfigEntry configEntry : ConfigEntry.values()) {
                String newValue = extractInputValue(configEntry);
                String oldValue = configService.getString(configEntry);

                if (configEntry == ConfigEntry.SERVICE_NAME && !Objects.equals(oldValue, newValue)) {
                    serviceNameChanged = true;
                }

                configService.set(configEntry, newValue);
            }

            if (serviceNameChanged && serviceNameChangedAction != null) {
                serviceNameChangedAction.run();
            }

            NotificationHelper.getInstance().showNotification(
                    messageProperties::getBaseSuccessTitle,
                    messageProperties::getAdminConfigSaveSuccess,
                    NotificationHelper.NotificationType.SUCCESS
            );
        } catch (Exception ex) {
            logger.error("Failed to store configuration values.", ex);
            NotificationHelper.getInstance().showNotification(
                    messageProperties::getBaseFailedTitle,
                    messageProperties::getBaseFailedMessage,
                    -1,
                    NotificationHelper.NotificationType.ERROR
            );
        }
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

        if (input instanceof TextField textField) {
            return textField.getValue() == null ? "" : textField.getValue();
        }

        return configEntry.getDefaultValueString();
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