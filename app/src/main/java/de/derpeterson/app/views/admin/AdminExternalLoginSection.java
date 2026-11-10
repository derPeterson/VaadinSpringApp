package de.derpeterson.app.views.admin;

import static de.derpeterson.app.security.ExternalProviderSettings.GOOGLE;
import static de.derpeterson.app.security.ExternalProviderSettings.GOOGLE_NAME;
import static de.derpeterson.app.security.ExternalProviderSettings.MICROSOFT_NAME;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_CARD;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_DESCRIPTION;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_HEADER;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_HEADING;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_ICON;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_TABS;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_TITLE;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Switch;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.TabSheetVariant;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import de.derpeterson.app.config.AdminConfigGroup;
import de.derpeterson.app.config.ConfigurationCatalog.ProviderField;
import de.derpeterson.app.helper.logging.LogSafety;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.helper.ui.NotificationHelper.NotificationType;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.security.ExternalProviderSettings;
import de.derpeterson.app.security.mfa.TwoFactorAction;
import de.derpeterson.app.security.mfa.TwoFactorAuthorization;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.ui.components.AdminSidePanel;
import de.derpeterson.app.ui.components.AppIcon;
import de.derpeterson.app.ui.components.ConfigGroupIcons;
import de.derpeterson.app.ui.components.ConfigInfoButton;
import de.derpeterson.app.ui.components.CosmosCardComponent;
import de.derpeterson.app.ui.components.CosmosSectionHeaderComponent;
import de.derpeterson.app.ui.components.TwoFactorActionDialog;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;

/** Geführte Einrichtung auf den vorhandenen Konfigurations-/Panelbausteinen. */
@StyleSheet("custom-theme/cosmos-provider-settings.css")
public class AdminExternalLoginSection extends VerticalLayout {
  private static final String PROVIDER_PREFIX = "provider-";
  private static final Logger logger = LoggerFactory.getLogger(AdminExternalLoginSection.class);
  private final transient MessageProperties messageProperties;
  private final transient ExternalProviderSettings externalProviderSettings;
  private final transient ConfigService configService;
  private final List<ProviderForm> forms = new ArrayList<>();
  private final Span configurationBreadcrumbSpan = new Span();
  private final Span configurationTitleSpan = new Span();
  private final Span configurationDescriptionSpan = new Span();
  private final H2 panelTitle = new H2();
  private final Span providerPanelDescriptionSpan = new Span();
  private final TabSheet providerTabSheet = new TabSheet();
  private final AdminSidePanel fieldHelpSidePanel;
  private final Button closeFieldHelpButton = new Button(AppIcon.CLOSE.create());
  private ProviderForm fieldHelpProviderForm;
  private ProviderField fieldHelpProviderField;
  private transient TwoFactorAuthorization twoFactorAuthorization;
  private transient jakarta.servlet.http.HttpServletRequest httpRequest;
  private TwoFactorActionDialog twoFactorConfirmationDialog;

  public void configureConfirmation(
      TwoFactorAuthorization twoFactorAuthorization,
      jakarta.servlet.http.HttpServletRequest httpRequest) {
    this.twoFactorAuthorization = twoFactorAuthorization;
    this.httpRequest = httpRequest;
    twoFactorConfirmationDialog =
        new TwoFactorActionDialog(messageProperties, twoFactorAuthorization, httpRequest);
    add(twoFactorConfirmationDialog);
  }

  public AdminExternalLoginSection(
      MessageProperties messageProperties,
      ExternalProviderSettings externalProviderSettings,
      ConfigService configService) {
    this.messageProperties = messageProperties;
    this.externalProviderSettings = externalProviderSettings;
    this.configService = configService;
    fieldHelpSidePanel = new AdminSidePanel(messageProperties, AppIcon.INFO_CIRCLE);
    fieldHelpSidePanel.addClassName("admin-config-help");
    fieldHelpSidePanel.setId("provider-field-help");
    closeFieldHelpButton.setWidthFull();
    closeFieldHelpButton.addClickListener(event -> fieldHelpSidePanel.close());
    fieldHelpSidePanel.getFooter().add(closeFieldHelpButton);
    setWidthFull();
    setPadding(false);
    setSpacing(false);
    addClassNames("cosmos-admin-section", "cosmos-provider-settings");
    setId("admin-provider-settings");
    add(
        new CosmosSectionHeaderComponent(
            configurationBreadcrumbSpan, configurationTitleSpan, configurationDescriptionSpan));
    panelTitle.addClassName(PANEL_TITLE);
    providerPanelDescriptionSpan.addClassName(PANEL_DESCRIPTION);
    Div heading = new Div(panelTitle, providerPanelDescriptionSpan);
    heading.addClassName(PANEL_HEADING);
    Div symbol = new Div(ConfigGroupIcons.forGroup(AdminConfigGroup.EXTERNAL_LOGIN).create());
    symbol.addClassName(PANEL_ICON);
    Div statuses = new Div();
    statuses.addClassName("provider-statuses");
    Div header = new Div(symbol, heading, statuses);
    header.addClassNames(PANEL_HEADER, "provider-panel-header");
    providerTabSheet.setId("admin-provider-tabs");
    providerTabSheet.addClassName(PANEL_TABS);
    providerTabSheet.setWidthFull();
    providerTabSheet.addThemeVariants(TabSheetVariant.NO_PADDING);
    for (String providerId : ExternalProviderSettings.PROVIDERS) {
      ProviderForm form = new ProviderForm(providerId);
      forms.add(form);
      statuses.add(form.providerStatusBadge);
      String name = providerId.equals(GOOGLE) ? GOOGLE_NAME : MICROSOFT_NAME;
      Image logo = new Image(providerLogo(providerId), "");
      logo.getElement().setAttribute("aria-hidden", "true");
      Tab tab = new Tab(logo, new Span(name));
      tab.setId("provider-tab-" + providerId);
      providerTabSheet.add(tab, form.providerFormContentDiv);
    }
    CosmosCardComponent card = new CosmosCardComponent(header, providerTabSheet);
    card.addClassNames("cosmos-admin-form-card", PANEL_CARD);
    add(card, fieldHelpSidePanel);
    refreshTexts();
  }

  private static String providerLogo(String providerId) {
    return "icons/external-login/" + providerId + (providerId.equals(GOOGLE) ? ".png" : ".svg");
  }

  public void activate() {
    for (ProviderForm form : forms) {
      form.loadProviderSettings();
    }
    refreshTexts();
  }

  public void refreshTexts() {
    configurationBreadcrumbSpan.setText(
        messageProperties.getAdminHeaderTitle() + " / " + messageProperties.getAdminConfigTitle());
    configurationTitleSpan.setText(messageProperties.getAdminConfigTitle());
    configurationDescriptionSpan.setText(messageProperties.getAdminConfigDescription());
    panelTitle.setText(messageProperties.getProviderSettingsPanelTitle());
    providerPanelDescriptionSpan.setText(messageProperties.getProviderSettingsPanelDescription());
    forms.forEach(ProviderForm::refreshTexts);
    closeFieldHelpButton.setText(messageProperties.getNotificationsClose());
    if (fieldHelpSidePanel.isOpened()) {
      fieldHelpProviderForm.refreshHelp();
    }
  }

  private final class ProviderForm implements Serializable {
    private final String providerId;
    private final Div providerFormContentDiv;
    private final Badge providerStatusBadge = new Badge();
    private final Tooltip statusTooltip = Tooltip.forComponent(providerStatusBadge);
    private final Span assistantKickerSpan = new Span();
    private final Span assistantKickerTextSpan = new Span();
    private final H3 assistantTitle = new H3();
    private final Span assistantDescriptionSpan = new Span();
    private final List<Span> stepLabels = new ArrayList<>();
    private final Span providerConsoleGuideSpan = new Span();
    private final Anchor providerConsoleLink;
    private final Span providerConsoleLinkCaptionSpan = new Span();
    private final TextField redirectUriTextField = new TextField();
    private final Button copyRedirectUriButton = new Button();
    private final TextField clientIdTextField = new TextField();
    private final PasswordField clientSecretPasswordField = new PasswordField();
    private final Switch providerEnabledSwitch = new Switch();
    private final Span providerEnabledLabelSpan = new Span();
    private final Span providerSettingsHintSpan = new Span();
    private final Button reloadProviderSettingsButton = new Button();
    private final Button saveProviderSettingsButton = new Button();
    private final Map<ProviderField, Span> fieldLabels = new EnumMap<>(ProviderField.class);
    private final Map<ProviderField, ConfigInfoButton> infoButtons =
        new EnumMap<>(ProviderField.class);
    private ExternalProviderSettings.Settings savedProviderSettings;

    private ProviderForm(String providerId) {
      this.providerId = providerId;
      assistantKickerSpan.addClassName("provider-assistant-kicker");
      assistantKickerSpan.add(AppIcon.BOT.create(), assistantKickerTextSpan);
      assistantTitle.addClassName("provider-assistant-title");
      assistantDescriptionSpan.addClassName("provider-assistant-description");
      Div introduction = new Div(assistantKickerSpan, assistantTitle, assistantDescriptionSpan);
      introduction.addClassName("provider-assistant-intro");
      providerStatusBadge.addClassName("provider-status");
      providerStatusBadge.setId(PROVIDER_PREFIX + providerId + "-status");
      providerStatusBadge.setIcon(new Image(providerLogo(providerId), ""));
      providerStatusBadge.getElement().setAttribute("tabindex", "0");
      providerConsoleLink =
          new Anchor(
              providerId.equals(GOOGLE)
                  ? "https://console.cloud.google.com/auth/clients"
                  : "https://entra.microsoft.com/#view/Microsoft_AAD_RegisteredApps/ApplicationsListBlade",
              "");
      providerConsoleLink.setTarget("_blank");
      providerConsoleLink.getElement().setAttribute("rel", "noopener noreferrer");
      providerConsoleLink.addClassName("provider-console-link");
      providerConsoleLink.add(
          AppIcon.EXTERNAL_LINK_TABLER.create(), providerConsoleLinkCaptionSpan);
      redirectUriTextField.setReadOnly(true);
      redirectUriTextField.setAutoselect(true);
      redirectUriTextField.setWidthFull();
      redirectUriTextField.setId(PROVIDER_PREFIX + providerId + "-redirect");
      copyRedirectUriButton.addClassName("provider-copy");
      copyRedirectUriButton.setPrefixComponent(AppIcon.CLIPBOARD_COPY.create());
      copyRedirectUriButton.addClickListener(event ->
          redirectUriTextField
              .getElement()
              .executeJs(
                  "return navigator.clipboard ? navigator.clipboard.writeText($0).then(() => true, () => false) : Promise.resolve(false)",
                  redirectUriTextField.getValue())
              .then(
                  Boolean.class,
                  copied ->
                      NotificationHelper.getInstance()
                          .showNotification(
                              Boolean.TRUE.equals(copied)
                                  ? messageProperties::getProviderSettingsCopied
                                  : messageProperties::getProviderSettingsCopyFailed,
                              null,
                              Boolean.TRUE.equals(copied)
                                  ? NotificationType.SUCCESS
                                  : NotificationType.ERROR)));
      Div address = new Div(redirectUriTextField, copyRedirectUriButton);
      address.addClassName("provider-redirect");
      clientIdTextField.setWidthFull();
      clientIdTextField.setMaxLength(512);
      clientIdTextField.setId(PROVIDER_PREFIX + providerId + "-client-id");
      clientIdTextField.setValueChangeMode(ValueChangeMode.EAGER);
      clientSecretPasswordField.setWidthFull();
      clientSecretPasswordField.setMaxLength(2048);
      clientSecretPasswordField.setId(PROVIDER_PREFIX + providerId + "-secret");
      clientSecretPasswordField.getElement().setAttribute("autocomplete", "new-password");
      clientSecretPasswordField.setValueChangeMode(ValueChangeMode.EAGER);
      Div credentials =
          new Div(
              fieldWithHelp(clientIdTextField, ProviderField.CLIENT_ID),
              fieldWithHelp(clientSecretPasswordField, ProviderField.SECRET));
      credentials.addClassName("provider-credentials");
      providerEnabledSwitch.setId(PROVIDER_PREFIX + providerId + "-enabled");
      providerEnabledSwitch.setWidthFull();
      providerEnabledSwitch.addClassName("cosmos-config-switch");
      providerEnabledSwitch.setLabelComponent(providerEnabledLabelSpan);
      Div activation = new Div(providerEnabledSwitch);
      activation.addClassName("provider-activation");
      Div consoleGuide = new Div(providerConsoleGuideSpan, providerConsoleLink);
      consoleGuide.addClassName("provider-console-guide");
      Div setup = new Div(consoleGuide, address);
      setup.addClassName("provider-step-body");
      Div fields =
          new Div(
              introduction,
              step(setup),
              step(credentials),
              step(fieldWithHelp(activation, ProviderField.ENABLED)));
      fields.addClassName("provider-fields");
      Div info = new Div(AppIcon.INFO_CIRCLE.create(), providerSettingsHintSpan);
      info.addClassName("cosmos-config-save-hint");
      reloadProviderSettingsButton.setPrefixComponent(AppIcon.REFRESH.create());
      reloadProviderSettingsButton.addClickListener(event -> loadProviderSettings());
      saveProviderSettingsButton.setPrefixComponent(AppIcon.CHECK.create());
      saveProviderSettingsButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
      saveProviderSettingsButton.setId(PROVIDER_PREFIX + providerId + "-save");
      saveProviderSettingsButton.addClickListener(event -> saveProviderSettings());
      Div actions = new Div(reloadProviderSettingsButton, saveProviderSettingsButton);
      actions.addClassName("cosmos-config-card-actions");
      Div footer = new Div(info, actions);
      footer.addClassName("cosmos-panel-footer");
      providerFormContentDiv = new Div(fields, footer);
      providerFormContentDiv.setId(PROVIDER_PREFIX + providerId + "-content");
      providerFormContentDiv.addClassName("provider-content");
      clientIdTextField.addValueChangeListener(event -> refreshAvailability());
      clientSecretPasswordField.addValueChangeListener(event -> refreshAvailability());
      providerEnabledSwitch.addValueChangeListener(event -> refreshAvailability());
    }

    private void openHelp(ProviderField field) {
      fieldHelpProviderForm = this;
      fieldHelpProviderField = field;
      fieldHelpProviderForm.refreshHelp();
      fieldHelpSidePanel.open();
    }

    private String fieldRule(ProviderField field) {
      return field.rule(messageProperties);
    }

    private void refreshHelp() {
      fieldHelpSidePanel.removeAll();
      fieldHelpSidePanel.setHeading(messageProperties.getAdminConfigHelpTitle());
      fieldHelpSidePanel.setDescription(messageProperties.getAdminConfigHelpDescription());
      fieldHelpSidePanel.refreshCloseText(messageProperties);
      Span group = new Span(providerId.equals(GOOGLE) ? GOOGLE_NAME : MICROSOFT_NAME);
      group.addClassName("admin-help-group");
      H3 fieldTitle = new H3(fieldLabel(fieldHelpProviderField));
      fieldTitle.addClassName("admin-help-field-title");
      Span explanation = new Span(fieldHelp(fieldHelpProviderField));
      explanation.addClassName("admin-help-description");
      UnorderedList rules = new UnorderedList(new ListItem(fieldRule(fieldHelpProviderField)));
      Div rulesSection = new Div(new H4(messageProperties.getAdminConfigInputRules()), rules);
      rulesSection.addClassName("admin-help-rules");
      fieldHelpSidePanel.add(group, fieldTitle, explanation, rulesSection);
      String timing =
          switch (fieldHelpProviderField) {
            case CLIENT_ID -> messageProperties.getAdminConfigEffectiveProviderClient();
            case SECRET -> messageProperties.getAdminConfigEffectiveProviderSecret();
            case ENABLED -> messageProperties.getAdminConfigEffectiveProviderEnabled();
          };
      fieldHelpSidePanel.addEffectiveUse(
          messageProperties,
          List.of(timing, messageProperties.getAdminConfigEffectiveProviderExisting()));
    }

    private Div step(Component body) {
      Span label = new Span();
      label.addClassName("provider-step-label");
      AppIcon icon =
          switch (stepLabels.size()) {
            case 0 -> AppIcon.NUMBER_ONE;
            case 1 -> AppIcon.NUMBER_TWO;
            case 2 -> AppIcon.NUMBER_THREE;
            default -> throw new IllegalStateException("Unsupported provider setup step");
          };
      label.add(icon.create());
      Span caption = new Span();
      label.add(caption);
      stepLabels.add(caption);
      Div step = new Div(label, body);
      step.addClassName("provider-step");
      return step;
    }

    private Div fieldWithHelp(Component input, ProviderField field) {
      ConfigInfoButton info = new ConfigInfoButton(() -> fieldHelp(field), () -> openHelp(field));
      info.setId(
          PROVIDER_PREFIX
              + providerId
              + "-"
              + field.name().toLowerCase(java.util.Locale.ROOT)
              + "-info");
      infoButtons.put(field, info);
      Div row = new Div(input, info);
      row.addClassNames("cosmos-config-input-row", "provider-help-row");
      if (field == ProviderField.ENABLED) {
        row.addClassName("provider-help-toggle");
        return row;
      }
      Span label = new Span();
      label.setId(info.getId().orElseThrow() + "-label");
      label.addClassNames("provider-field-label", "admin-config-required");
      fieldLabels.put(field, label);
      if (input instanceof TextField textField) {
        textField.setAriaLabelledBy(label.getId().orElseThrow());
      } else if (input instanceof PasswordField passwordField) {
        passwordField.setAriaLabelledBy(label.getId().orElseThrow());
      }
      Div wrapper = new Div(label, row);
      wrapper.addClassNames("cosmos-config-field", "provider-help-field");
      return wrapper;
    }

    private String fieldLabel(ProviderField field) {
      return field.label(messageProperties);
    }

    private String fieldHelp(ProviderField field) {
      return field.description(messageProperties, providerId);
    }

    private void loadProviderSettings() {
      savedProviderSettings = externalProviderSettings.load(providerId);
      clientIdTextField.setValue(savedProviderSettings.clientId());
      clientSecretPasswordField.clear();
      providerEnabledSwitch.setValue(savedProviderSettings.enabled());
      clientIdTextField.setInvalid(false);
      clientSecretPasswordField.setInvalid(false);
      // BASE_URL ist dieselbe vertrauenswürdige öffentliche Adresse wie für Bestätigungslinks.
      String base = configService.getString(ConfigEntry.BASE_URL);
      URI origin = URI.create(base);
      redirectUriTextField.setValue(origin.resolve("/login/oauth2/code/" + providerId).toString());
      refreshTexts();
    }

    private void saveProviderSettings() {
      if (twoFactorAuthorization != null
          && twoFactorAuthorization.requires(httpRequest, TwoFactorAction.SECURITY_CONFIGURATION)) {
        twoFactorConfirmationDialog.open(
            TwoFactorAction.SECURITY_CONFIGURATION, this::saveConfirmed);
      } else {
        saveConfirmed();
      }
    }

    private void saveConfirmed() {
      clientIdTextField.setInvalid(
          providerEnabledSwitch.getValue() && clientIdTextField.getValue().isBlank());
      clientSecretPasswordField.setInvalid(
          providerEnabledSwitch.getValue()
              && clientSecretPasswordField.getValue().isBlank()
              && !savedProviderSettings.secretStored());
      try {
        savedProviderSettings =
            externalProviderSettings.save(
                providerId,
                clientIdTextField.getValue(),
                clientSecretPasswordField.getValue(),
                providerEnabledSwitch.getValue(),
                savedProviderSettings.revision());
        clientSecretPasswordField.clear();
        refreshTexts();
        NotificationHelper.getInstance()
            .showNotification(
                messageProperties::getProviderSettingsSaved, null, NotificationType.SUCCESS);
      } catch (IllegalArgumentException _) {
        NotificationHelper.getInstance()
            .showNotification(
                messageProperties::getProviderSettingsIncomplete, null, NotificationType.ERROR);
      } catch (OptimisticLockingFailureException _) {
        NotificationHelper.getInstance()
            .showNotification(
                messageProperties::getProviderSettingsConflict, null, NotificationType.ERROR);
      } catch (RuntimeException failure) {
        logger.warn("Sign-in provider settings could not be saved", LogSafety.safeFailure(failure));
        NotificationHelper.getInstance()
            .showNotification(
                messageProperties::getProviderSettingsFailed, null, NotificationType.ERROR);
      }
    }

    private void refreshTexts() {
      fieldLabels.forEach((field, label) -> label.setText(fieldLabel(field)));
      infoButtons.forEach((field, info) -> {
        info.refreshText();
        info.setAriaLabel(messageProperties.getAdminConfigHelpTitle() + ": " + fieldLabel(field));
      });
      String name = providerId.equals(GOOGLE) ? GOOGLE_NAME : MICROSOFT_NAME;
      assistantKickerTextSpan.setText(messageProperties.getProviderSettingsAssistantKicker());
      assistantTitle.setText(messageProperties.getProviderSettingsAssistantTitle(name));
      assistantDescriptionSpan.setText(messageProperties.getProviderSettingsAssistantDescription());
      for (int index = 0; index < stepLabels.size(); index++) {
        stepLabels
            .get(index)
            .setText(
                switch (index) {
                  case 0 -> messageProperties.getProviderSettingsSetupProvider();
                  case 1 -> messageProperties.getProviderSettingsSetupCredentials();
                  case 2 -> messageProperties.getProviderSettingsEnableSignIn();
                  default -> throw new IllegalStateException("Unsupported provider setup step");
                });
      }
      providerConsoleGuideSpan.setText(
          providerId.equals(GOOGLE)
              ? messageProperties.getProviderSettingsGoogleGuide()
              : messageProperties.getProviderSettingsMicrosoftGuide());
      providerConsoleLinkCaptionSpan.setText(messageProperties.getProviderSettingsOpenConsole());
      redirectUriTextField.setLabel(messageProperties.getProviderSettingsRedirect());
      copyRedirectUriButton.setText(messageProperties.getProviderSettingsCopy());
      clientIdTextField.setLabel(messageProperties.getProviderSettingsClientId());
      clientIdTextField.setPlaceholder(messageProperties.getProviderSettingsClientIdPlaceholder());
      clientSecretPasswordField.setLabel(messageProperties.getProviderSettingsSecret());
      clientSecretPasswordField.setPlaceholder(
          savedProviderSettings != null && savedProviderSettings.secretStored()
              ? messageProperties.getProviderSettingsSecretStored()
              : messageProperties.getProviderSettingsSecretPlaceholder());
      clientSecretPasswordField.setHelperText(messageProperties.getProviderSettingsSecretHint());
      providerEnabledLabelSpan.setText(messageProperties.getProviderSettingsEnable());
      providerEnabledSwitch.setAriaLabel(messageProperties.getProviderSettingsEnable());
      providerSettingsHintSpan.setText(messageProperties.getProviderSettingsHint());
      reloadProviderSettingsButton.setText(messageProperties.getAdminConfigReload());
      saveProviderSettingsButton.setText(messageProperties.getAdminConfigSave());
      refreshAvailability();
      refreshStatus();
    }

    private void refreshAvailability() {
      boolean credentialsPresent =
          !clientIdTextField.getValue().isBlank()
              && (!clientSecretPasswordField.getValue().isBlank()
                  || savedProviderSettings != null && savedProviderSettings.secretStored());
      boolean editable = credentialsPresent || providerEnabledSwitch.getValue();
      providerEnabledSwitch.setEnabled(editable);
      providerEnabledLabelSpan
          .getElement()
          .setAttribute("aria-disabled", Boolean.toString(!editable));
    }

    private void refreshStatus() {
      boolean configured =
          savedProviderSettings != null
              && !savedProviderSettings.clientId().isBlank()
              && savedProviderSettings.secretStored();
      boolean active = configured && savedProviderSettings.enabled();
      String name = providerId.equals(GOOGLE) ? GOOGLE_NAME : MICROSOFT_NAME;
      String state =
          active
              ? messageProperties.getProviderSettingsActive()
              : messageProperties.getProviderSettingsInactive();
      providerStatusBadge.setText(state);
      providerStatusBadge.getElement().setAttribute("aria-label", name + ": " + state);
      providerStatusBadge.removeThemeVariants(BadgeVariant.SUCCESS, BadgeVariant.WARNING);
      providerStatusBadge.addThemeVariants(active ? BadgeVariant.SUCCESS : BadgeVariant.WARNING);
      providerStatusBadge.getElement().setAttribute("data-state", active ? "active" : "inactive");
      String explanation;
      if (!configured) {
        explanation = messageProperties.getProviderSettingsUnconfiguredHint(name);
      } else if (active) {
        explanation = messageProperties.getProviderSettingsActiveHint(name);
      } else {
        explanation = messageProperties.getProviderSettingsInactiveHint(name);
      }
      statusTooltip.setText(explanation);
    }
  }
}
