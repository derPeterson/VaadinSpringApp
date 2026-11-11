package de.derpeterson.app.views.admin;

import static de.derpeterson.app.security.ExternalProviderSettings.GOOGLE;
import static de.derpeterson.app.security.ExternalProviderSettings.GOOGLE_NAME;
import static de.derpeterson.app.security.ExternalProviderSettings.MICROSOFT_NAME;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_CARD;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_DESCRIPTION;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_HEADER;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_HEADING;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_ICON;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_TABS;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_TITLE;

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
import de.derpeterson.app.ui.components.AdminSidePanelDialog;
import de.derpeterson.app.ui.components.AppIcon;
import de.derpeterson.app.ui.components.ConfigGroupIcons;
import de.derpeterson.app.ui.components.ConfigInfoButton;
import de.derpeterson.app.ui.components.CosmosCardComponent;
import de.derpeterson.app.ui.components.CosmosSectionHeaderComponent;
import de.derpeterson.app.ui.components.TwoFactorActionDialog;
import de.derpeterson.app.ui.components.UnsavedChangesDialog;
import jakarta.servlet.http.HttpServletRequest;
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
public class AdminExternalProviderSettingsSection extends VerticalLayout {
  private static final String PROVIDER_PREFIX = "provider-";
  private static final Logger logger =
      LoggerFactory.getLogger(AdminExternalProviderSettingsSection.class);
  private final transient MessageProperties messageProperties;
  private final transient ExternalProviderSettings externalProviderSettings;
  private final transient ConfigService configService;
  private final List<ProviderForm> providerForms = new ArrayList<>();
  private final Span configurationBreadcrumbSpan = new Span();
  private final Span configurationTitleSpan = new Span();
  private final Span configurationDescriptionSpan = new Span();
  private final H2 providerPanelTitleH2 = new H2();
  private final Span providerPanelDescriptionSpan = new Span();
  private final TabSheet providerTabSheet = new TabSheet();
  private final AdminSidePanelDialog fieldHelpSidePanel;
  private final Button closeFieldHelpButton = new Button(AppIcon.CLOSE.create());
  private ProviderForm fieldHelpProviderForm;
  private ProviderField fieldHelpProviderField;
  private transient TwoFactorAuthorization twoFactorAuthorization;
  private transient HttpServletRequest httpServletRequest;
  private TwoFactorActionDialog twoFactorConfirmationDialog;

  public void configureConfirmation(
      TwoFactorAuthorization twoFactorAuthorization, HttpServletRequest httpServletRequest) {
    this.twoFactorAuthorization = twoFactorAuthorization;
    this.httpServletRequest = httpServletRequest;
    twoFactorConfirmationDialog =
        new TwoFactorActionDialog(messageProperties, twoFactorAuthorization, httpServletRequest);
    add(twoFactorConfirmationDialog);
  }

  public AdminExternalProviderSettingsSection(
      MessageProperties messageProperties,
      ExternalProviderSettings externalProviderSettings,
      ConfigService configService) {
    this.messageProperties = messageProperties;
    this.externalProviderSettings = externalProviderSettings;
    this.configService = configService;

    fieldHelpSidePanel = new AdminSidePanelDialog(messageProperties, AppIcon.INFO_CIRCLE);
    fieldHelpSidePanel.addClassName("admin-config-help");
    fieldHelpSidePanel.setId("provider-field-help");
    closeFieldHelpButton.setWidthFull();
    closeFieldHelpButton.addClickListener(_ -> fieldHelpSidePanel.close());
    fieldHelpSidePanel.getFooter().add(closeFieldHelpButton);

    setWidthFull();
    setPadding(false);
    setSpacing(false);
    addClassNames("cosmos-admin-section", "cosmos-provider-settings");
    setId("admin-provider-settings");
    add(
        new CosmosSectionHeaderComponent(
            configurationBreadcrumbSpan, configurationTitleSpan, configurationDescriptionSpan));

    providerPanelTitleH2.addClassName(PANEL_TITLE);
    providerPanelDescriptionSpan.addClassName(PANEL_DESCRIPTION);
    Div providerPanelHeadingDiv = new Div(providerPanelTitleH2, providerPanelDescriptionSpan);
    providerPanelHeadingDiv.addClassName(PANEL_HEADING);
    Div providerIconDiv =
        new Div(ConfigGroupIcons.forGroup(AdminConfigGroup.EXTERNAL_LOGIN).create());
    providerIconDiv.addClassName(PANEL_ICON);
    Div providerStatusesDiv = new Div();
    providerStatusesDiv.addClassName("provider-statuses");
    Div providerHeaderDiv = new Div(providerIconDiv, providerPanelHeadingDiv, providerStatusesDiv);
    providerHeaderDiv.addClassNames(PANEL_HEADER, "provider-panel-header");

    providerTabSheet.setId("admin-provider-tabs");
    providerTabSheet.addClassName(PANEL_TABS);
    providerTabSheet.setWidthFull();
    providerTabSheet.addThemeVariants(TabSheetVariant.NO_PADDING);

    for (String providerId : ExternalProviderSettings.PROVIDERS) {
      ProviderForm providerForm = new ProviderForm(providerId);
      providerForms.add(providerForm);
      providerStatusesDiv.add(providerForm.providerStatusBadge);
      String providerNameText = providerId.equals(GOOGLE) ? GOOGLE_NAME : MICROSOFT_NAME;
      Image providerLogoImage = new Image(resolveProviderLogoPath(providerId), "");
      providerLogoImage.getElement().setAttribute("aria-hidden", "true");
      Tab providerTab = new Tab(providerLogoImage, new Span(providerNameText));
      providerTab.setId("provider-tab-" + providerId);
      providerTabSheet.add(providerTab, providerForm.providerFormContentDiv);
    }

    CosmosCardComponent providerCardComponent =
        new CosmosCardComponent(providerHeaderDiv, providerTabSheet);
    providerCardComponent.addClassNames("cosmos-admin-form-card", PANEL_CARD);
    add(providerCardComponent, fieldHelpSidePanel);
    refreshTexts();
  }

  private static String resolveProviderLogoPath(String providerId) {
    return "icons/external-login/" + providerId + (providerId.equals(GOOGLE) ? ".png" : ".svg");
  }

  public void activate() {
    for (ProviderForm form : providerForms) {
      form.loadProviderSettings();
    }
    refreshTexts();
  }

  public boolean hasUnsavedChanges() {
    return providerForms.stream().anyMatch(ProviderForm::hasUnsavedChanges);
  }

  public void refreshTexts() {
    configurationBreadcrumbSpan.setText(
        messageProperties.getAdminHeaderTitle() + " / " + messageProperties.getAdminConfigTitle());
    configurationTitleSpan.setText(messageProperties.getAdminConfigTitle());
    configurationDescriptionSpan.setText(messageProperties.getAdminConfigDescription());
    providerPanelTitleH2.setText(messageProperties.getProviderSettingsPanelTitle());
    providerPanelDescriptionSpan.setText(messageProperties.getProviderSettingsPanelDescription());

    providerForms.forEach(ProviderForm::refreshTexts);
    closeFieldHelpButton.setText(messageProperties.getNotificationsClose());
    if (fieldHelpSidePanel.isOpened()) {
      fieldHelpProviderForm.refreshHelp();
    }
  }

  private final class ProviderForm implements Serializable {
    private final String providerId;
    private final Div providerFormContentDiv;
    private final Badge providerStatusBadge = new Badge();
    private final Tooltip providerStatusTooltip = Tooltip.forComponent(providerStatusBadge);
    private final Span assistantKickerSpan = new Span();
    private final Span assistantKickerTextSpan = new Span();
    private final H3 assistantTitleH3 = new H3();
    private final Span assistantDescriptionSpan = new Span();
    private final List<Span> setupStepCaptionSpans = new ArrayList<>();
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
    private final Map<ProviderField, Span> labelSpansByProviderField =
        new EnumMap<>(ProviderField.class);
    private final Map<ProviderField, ConfigInfoButton> infoButtonsByProviderField =
        new EnumMap<>(ProviderField.class);
    private ExternalProviderSettings.Settings savedProviderSettings;

    private ProviderForm(String providerId) {
      this.providerId = providerId;

      assistantKickerSpan.addClassName("provider-assistant-kicker");
      assistantKickerSpan.add(AppIcon.BOT.create(), assistantKickerTextSpan);
      assistantTitleH3.addClassName("provider-assistant-title");
      assistantDescriptionSpan.addClassName("provider-assistant-description");
      Div assistantIntroDiv =
          new Div(assistantKickerSpan, assistantTitleH3, assistantDescriptionSpan);
      assistantIntroDiv.addClassName("provider-assistant-intro");

      providerStatusBadge.addClassName("provider-status");
      providerStatusBadge.setId(PROVIDER_PREFIX + providerId + "-status");
      providerStatusBadge.setIcon(new Image(resolveProviderLogoPath(providerId), ""));
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
      copyRedirectUriButton.addClickListener(_ ->
          redirectUriTextField
              .getElement()
              .executeJs(
                  "return navigator.clipboard ? navigator.clipboard.writeText($0).then(() => true, () => false) : Promise.resolve(false)",
                  redirectUriTextField.getValue())
              .then(
                  Boolean.class,
                  wasRedirectUriCopied ->
                      NotificationHelper.getInstance()
                          .showNotification(
                              Boolean.TRUE.equals(wasRedirectUriCopied)
                                  ? messageProperties::getProviderSettingsCopied
                                  : messageProperties::getProviderSettingsCopyFailed,
                              null,
                              Boolean.TRUE.equals(wasRedirectUriCopied)
                                  ? NotificationType.SUCCESS
                                  : NotificationType.ERROR)));
      Div redirectUriRowDiv = new Div(redirectUriTextField, copyRedirectUriButton);
      redirectUriRowDiv.addClassName("provider-redirect");

      clientIdTextField.setWidthFull();
      clientIdTextField.setMaxLength(512);
      clientIdTextField.setId(PROVIDER_PREFIX + providerId + "-client-id");
      clientIdTextField.setValueChangeMode(ValueChangeMode.EAGER);

      clientSecretPasswordField.setWidthFull();
      clientSecretPasswordField.setMaxLength(2048);
      clientSecretPasswordField.setId(PROVIDER_PREFIX + providerId + "-secret");
      clientSecretPasswordField.getElement().setAttribute("autocomplete", "new-password");
      clientSecretPasswordField.setValueChangeMode(ValueChangeMode.EAGER);
      Div credentialsDiv =
          new Div(
              fieldWithHelp(clientIdTextField, ProviderField.CLIENT_ID),
              fieldWithHelp(clientSecretPasswordField, ProviderField.SECRET));
      credentialsDiv.addClassName("provider-credentials");

      providerEnabledSwitch.setId(PROVIDER_PREFIX + providerId + "-enabled");
      providerEnabledSwitch.setWidthFull();
      providerEnabledSwitch.addClassName("cosmos-config-switch");
      providerEnabledSwitch.setLabelComponent(providerEnabledLabelSpan);
      Div activationDiv = new Div(providerEnabledSwitch);
      activationDiv.addClassName("provider-activation");

      Div consoleGuideDiv = new Div(providerConsoleGuideSpan, providerConsoleLink);
      consoleGuideDiv.addClassName("provider-console-guide");
      Div providerSetupDiv = new Div(consoleGuideDiv, redirectUriRowDiv);
      providerSetupDiv.addClassName("provider-step-body");
      Div providerFieldsDiv =
          new Div(
              assistantIntroDiv,
              buildProviderSetupStepDiv(providerSetupDiv),
              buildProviderSetupStepDiv(credentialsDiv),
              buildProviderSetupStepDiv(fieldWithHelp(activationDiv, ProviderField.ENABLED)));
      providerFieldsDiv.addClassName("provider-fields");

      Div providerSaveHintDiv = new Div(AppIcon.INFO_CIRCLE.create(), providerSettingsHintSpan);
      providerSaveHintDiv.addClassName("cosmos-config-save-hint");
      reloadProviderSettingsButton.setPrefixComponent(AppIcon.REFRESH.create());
      reloadProviderSettingsButton.addClickListener(_ -> requestProviderSettingsReload());

      saveProviderSettingsButton.setPrefixComponent(AppIcon.CHECK.create());
      saveProviderSettingsButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
      saveProviderSettingsButton.setId(PROVIDER_PREFIX + providerId + "-save");
      saveProviderSettingsButton.addClickListener(_ -> saveProviderSettings());
      Div providerActionsDiv = new Div(reloadProviderSettingsButton, saveProviderSettingsButton);
      providerActionsDiv.addClassName("cosmos-config-card-actions");
      Div providerFooterDiv = new Div(providerSaveHintDiv, providerActionsDiv);
      providerFooterDiv.addClassName("cosmos-panel-footer");
      providerFormContentDiv = new Div(providerFieldsDiv, providerFooterDiv);
      providerFormContentDiv.setId(PROVIDER_PREFIX + providerId + "-content");
      providerFormContentDiv.addClassName("provider-content");

      clientIdTextField.addValueChangeListener(_ -> refreshProviderActivationAvailability());
      clientSecretPasswordField.addValueChangeListener(
          _ -> refreshProviderActivationAvailability());
      providerEnabledSwitch.addValueChangeListener(_ -> refreshProviderActivationAvailability());
    }

    private void openProviderFieldHelp(ProviderField field) {
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

      Span providerNameSpan = new Span(providerId.equals(GOOGLE) ? GOOGLE_NAME : MICROSOFT_NAME);
      providerNameSpan.addClassName("admin-help-group");
      H3 fieldTitleH3 = new H3(fieldLabel(fieldHelpProviderField));
      fieldTitleH3.addClassName("admin-help-field-title");
      Span fieldDescriptionSpan = new Span(fieldHelp(fieldHelpProviderField));
      fieldDescriptionSpan.addClassName("admin-help-description");
      UnorderedList fieldRulesList =
          new UnorderedList(new ListItem(fieldRule(fieldHelpProviderField)));
      Div fieldRulesDiv =
          new Div(new H4(messageProperties.getAdminConfigInputRules()), fieldRulesList);
      fieldRulesDiv.addClassName("admin-help-rules");
      fieldHelpSidePanel.add(providerNameSpan, fieldTitleH3, fieldDescriptionSpan, fieldRulesDiv);

      String effectiveUseText =
          switch (fieldHelpProviderField) {
            case CLIENT_ID -> messageProperties.getAdminConfigEffectiveProviderClient();
            case SECRET -> messageProperties.getAdminConfigEffectiveProviderSecret();
            case ENABLED -> messageProperties.getAdminConfigEffectiveProviderEnabled();
          };
      fieldHelpSidePanel.addEffectiveUse(
          messageProperties,
          List.of(effectiveUseText, messageProperties.getAdminConfigEffectiveProviderExisting()));
    }

    private Div buildProviderSetupStepDiv(Component stepBodyComponent) {
      Span stepLabelSpan = new Span();
      stepLabelSpan.addClassName("provider-step-label");
      AppIcon stepAppIcon =
          switch (setupStepCaptionSpans.size()) {
            case 0 -> AppIcon.NUMBER_ONE;
            case 1 -> AppIcon.NUMBER_TWO;
            case 2 -> AppIcon.NUMBER_THREE;
            default -> throw new IllegalStateException("Unsupported provider setup step");
          };
      stepLabelSpan.add(stepAppIcon.create());
      Span stepCaptionSpan = new Span();
      stepLabelSpan.add(stepCaptionSpan);
      setupStepCaptionSpans.add(stepCaptionSpan);
      Div setupStepDiv = new Div(stepLabelSpan, stepBodyComponent);
      setupStepDiv.addClassName("provider-step");
      return setupStepDiv;
    }

    private Div fieldWithHelp(Component fieldInputComponent, ProviderField field) {
      ConfigInfoButton fieldInfoButton =
          new ConfigInfoButton(() -> fieldHelp(field), () -> openProviderFieldHelp(field));
      fieldInfoButton.setId(
          PROVIDER_PREFIX
              + providerId
              + "-"
              + field.name().toLowerCase(java.util.Locale.ROOT)
              + "-info");
      infoButtonsByProviderField.put(field, fieldInfoButton);
      Div fieldRowDiv = new Div(fieldInputComponent, fieldInfoButton);
      fieldRowDiv.addClassNames("cosmos-config-input-row", "provider-help-row");

      if (field == ProviderField.ENABLED) {
        fieldRowDiv.addClassName("provider-help-toggle");
        return fieldRowDiv;
      }
      Span stepLabelSpan = new Span();
      stepLabelSpan.setId(fieldInfoButton.getId().orElseThrow() + "-label");
      stepLabelSpan.addClassNames("provider-field-label", "admin-config-required");
      labelSpansByProviderField.put(field, stepLabelSpan);
      if (fieldInputComponent instanceof TextField textField) {
        textField.setAriaLabelledBy(stepLabelSpan.getId().orElseThrow());
      } else if (fieldInputComponent instanceof PasswordField passwordField) {
        passwordField.setAriaLabelledBy(stepLabelSpan.getId().orElseThrow());
      }
      Div labelledFieldDiv = new Div(stepLabelSpan, fieldRowDiv);
      labelledFieldDiv.addClassNames("cosmos-config-field", "provider-help-field");
      return labelledFieldDiv;
    }

    private String fieldLabel(ProviderField field) {
      return field.label(messageProperties);
    }

    private String fieldHelp(ProviderField field) {
      return field.description(messageProperties, providerId);
    }

    private boolean hasUnsavedChanges() {
      return savedProviderSettings != null
          && (!savedProviderSettings.clientId().equals(clientIdTextField.getValue())
              || !clientSecretPasswordField.isEmpty()
              || savedProviderSettings.enabled() != providerEnabledSwitch.getValue());
    }

    private void requestProviderSettingsReload() {
      if (hasUnsavedChanges()) {
        UnsavedChangesDialog.confirmDiscard(messageProperties, this::loadProviderSettings);
      } else {
        loadProviderSettings();
      }
    }

    private void loadProviderSettings() {
      savedProviderSettings = externalProviderSettings.load(providerId);
      clientIdTextField.setValue(savedProviderSettings.clientId());
      clientSecretPasswordField.clear();
      providerEnabledSwitch.setValue(savedProviderSettings.enabled());
      clientIdTextField.setInvalid(false);
      clientSecretPasswordField.setInvalid(false);

      // BASE_URL ist dieselbe vertrauenswürdige öffentliche Adresse wie für Bestätigungslinks.
      String publicBaseUrl = configService.getString(ConfigEntry.BASE_URL);
      URI publicBaseUri = URI.create(publicBaseUrl);
      redirectUriTextField.setValue(
          publicBaseUri.resolve("/login/oauth2/code/" + providerId).toString());
      refreshTexts();
    }

    private void saveProviderSettings() {
      if (twoFactorAuthorization != null
          && twoFactorAuthorization.requires(
              httpServletRequest, TwoFactorAction.SECURITY_CONFIGURATION)) {
        twoFactorConfirmationDialog.open(
            TwoFactorAction.SECURITY_CONFIGURATION, this::persistConfirmedProviderSettings);
      } else {
        persistConfirmedProviderSettings();
      }
    }

    private void persistConfirmedProviderSettings() {
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

        clientIdTextField.setValue(savedProviderSettings.clientId());
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
      labelSpansByProviderField.forEach((field, label) -> label.setText(fieldLabel(field)));

      infoButtonsByProviderField.forEach((field, info) -> {
        info.refreshText();
        info.setAriaLabel(messageProperties.getAdminConfigHelpTitle() + ": " + fieldLabel(field));
      });
      String providerNameText = providerId.equals(GOOGLE) ? GOOGLE_NAME : MICROSOFT_NAME;
      assistantKickerTextSpan.setText(messageProperties.getProviderSettingsAssistantKicker());
      assistantTitleH3.setText(
          messageProperties.getProviderSettingsAssistantTitle(providerNameText));
      assistantDescriptionSpan.setText(messageProperties.getProviderSettingsAssistantDescription());

      for (int index = 0; index < setupStepCaptionSpans.size(); index++) {
        setupStepCaptionSpans
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

      refreshProviderActivationAvailability();
      refreshProviderStatusBadge();
    }

    private void refreshProviderActivationAvailability() {
      boolean hasProviderCredentials =
          !clientIdTextField.getValue().isBlank()
              && (!clientSecretPasswordField.getValue().isBlank()
                  || savedProviderSettings != null && savedProviderSettings.secretStored());
      boolean canChangeProviderActivation =
          hasProviderCredentials || providerEnabledSwitch.getValue();
      providerEnabledSwitch.setEnabled(canChangeProviderActivation);
      providerEnabledLabelSpan
          .getElement()
          .setAttribute("aria-disabled", Boolean.toString(!canChangeProviderActivation));
    }

    private void refreshProviderStatusBadge() {
      boolean isProviderConfigured =
          savedProviderSettings != null
              && !savedProviderSettings.clientId().isBlank()
              && savedProviderSettings.secretStored();
      boolean isProviderActive = isProviderConfigured && savedProviderSettings.enabled();
      String providerNameText = providerId.equals(GOOGLE) ? GOOGLE_NAME : MICROSOFT_NAME;
      String providerStatusText =
          isProviderActive
              ? messageProperties.getProviderSettingsActive()
              : messageProperties.getProviderSettingsInactive();

      providerStatusBadge.setText(providerStatusText);
      providerStatusBadge
          .getElement()
          .setAttribute("aria-label", providerNameText + ": " + providerStatusText);
      providerStatusBadge.removeThemeVariants(BadgeVariant.SUCCESS, BadgeVariant.WARNING);
      providerStatusBadge.addThemeVariants(
          isProviderActive ? BadgeVariant.SUCCESS : BadgeVariant.WARNING);
      providerStatusBadge
          .getElement()
          .setAttribute("data-state", isProviderActive ? "active" : "inactive");

      String statusExplanationText;
      if (!isProviderConfigured) {
        statusExplanationText =
            messageProperties.getProviderSettingsUnconfiguredHint(providerNameText);
      } else if (isProviderActive) {
        statusExplanationText = messageProperties.getProviderSettingsActiveHint(providerNameText);
      } else {
        statusExplanationText = messageProperties.getProviderSettingsInactiveHint(providerNameText);
      }
      providerStatusTooltip.setText(statusExplanationText);
    }
  }
}
