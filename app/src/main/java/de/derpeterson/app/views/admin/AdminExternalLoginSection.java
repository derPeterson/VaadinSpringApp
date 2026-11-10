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
  private final Span breadcrumb = new Span();
  private final Span title = new Span();
  private final Span description = new Span();
  private final H2 panelTitle = new H2();
  private final Span panelDescription = new Span();
  private final TabSheet providerTabs = new TabSheet();
  private final AdminSidePanel helpPanel;
  private final Button closeFieldHelpButton = new Button(AppIcon.CLOSE.create());
  private ProviderForm helpForm;
  private ProviderField helpField;
  private transient TwoFactorAuthorization twoFactorAuthorization;
  private transient jakarta.servlet.http.HttpServletRequest request;
  private TwoFactorActionDialog twoFactorConfirmationDialog;

  public void configureConfirmation(
      TwoFactorAuthorization twoFactorAuthorization,
      jakarta.servlet.http.HttpServletRequest request) {
    this.twoFactorAuthorization = twoFactorAuthorization;
    this.request = request;
    twoFactorConfirmationDialog =
        new TwoFactorActionDialog(messageProperties, twoFactorAuthorization, request);
    add(twoFactorConfirmationDialog);
  }

  public AdminExternalLoginSection(
      MessageProperties messageProperties,
      ExternalProviderSettings externalProviderSettings,
      ConfigService configService) {
    this.messageProperties = messageProperties;
    this.externalProviderSettings = externalProviderSettings;
    this.configService = configService;
    helpPanel = new AdminSidePanel(messageProperties, AppIcon.INFO_CIRCLE);
    helpPanel.addClassName("admin-config-help");
    helpPanel.setId("provider-field-help");
    closeFieldHelpButton.setWidthFull();
    closeFieldHelpButton.addClickListener(event -> helpPanel.close());
    helpPanel.getFooter().add(closeFieldHelpButton);
    setWidthFull();
    setPadding(false);
    setSpacing(false);
    addClassNames("cosmos-admin-section", "cosmos-provider-settings");
    setId("admin-provider-settings");
    add(new CosmosSectionHeaderComponent(breadcrumb, title, description));
    panelTitle.addClassName(PANEL_TITLE);
    panelDescription.addClassName(PANEL_DESCRIPTION);
    Div heading = new Div(panelTitle, panelDescription);
    heading.addClassName(PANEL_HEADING);
    Div symbol = new Div(ConfigGroupIcons.forGroup(AdminConfigGroup.EXTERNAL_LOGIN).create());
    symbol.addClassName(PANEL_ICON);
    Div statuses = new Div();
    statuses.addClassName("provider-statuses");
    Div header = new Div(symbol, heading, statuses);
    header.addClassNames(PANEL_HEADER, "provider-panel-header");
    providerTabs.setId("admin-provider-tabs");
    providerTabs.addClassName(PANEL_TABS);
    providerTabs.setWidthFull();
    providerTabs.addThemeVariants(TabSheetVariant.NO_PADDING);
    for (String provider : ExternalProviderSettings.PROVIDERS) {
      ProviderForm form = new ProviderForm(provider);
      forms.add(form);
      statuses.add(form.status);
      String name = provider.equals(GOOGLE) ? GOOGLE_NAME : MICROSOFT_NAME;
      Image logo = new Image(providerLogo(provider), "");
      logo.getElement().setAttribute("aria-hidden", "true");
      Tab tab = new Tab(logo, new Span(name));
      tab.setId("provider-tab-" + provider);
      providerTabs.add(tab, form.content);
    }
    CosmosCardComponent card = new CosmosCardComponent(header, providerTabs);
    card.addClassNames("cosmos-admin-form-card", PANEL_CARD);
    add(card, helpPanel);
    refreshTexts();
  }

  private static String providerLogo(String provider) {
    return "icons/external-login/" + provider + (provider.equals(GOOGLE) ? ".png" : ".svg");
  }

  public void activate() {
    for (ProviderForm form : forms) {
      form.load();
    }
    refreshTexts();
  }

  public void refreshTexts() {
    breadcrumb.setText(
        messageProperties.getAdminHeaderTitle() + " / " + messageProperties.getAdminConfigTitle());
    title.setText(messageProperties.getAdminConfigTitle());
    description.setText(messageProperties.getAdminConfigDescription());
    panelTitle.setText(messageProperties.getProviderSettingsPanelTitle());
    panelDescription.setText(messageProperties.getProviderSettingsPanelDescription());
    forms.forEach(ProviderForm::refreshTexts);
    closeFieldHelpButton.setText(messageProperties.getNotificationsClose());
    if (helpPanel.isOpened()) {
      helpForm.refreshHelp();
    }
  }

  private final class ProviderForm implements Serializable {
    private final String provider;
    private final Div content;
    private final Badge status = new Badge();
    private final Tooltip statusTooltip = Tooltip.forComponent(status);
    private final Span assistantKicker = new Span();
    private final Span assistantKickerText = new Span();
    private final H3 assistantTitle = new H3();
    private final Span assistantDescription = new Span();
    private final List<Span> stepLabels = new ArrayList<>();
    private final Span guide = new Span();
    private final Anchor console;
    private final Span consoleCaption = new Span();
    private final TextField redirect = new TextField();
    private final Button copy = new Button();
    private final TextField clientId = new TextField();
    private final PasswordField secret = new PasswordField();
    private final Switch enabled = new Switch();
    private final Span enabledLabel = new Span();
    private final Span hint = new Span();
    private final Button reload = new Button();
    private final Button save = new Button();
    private final Map<ProviderField, Span> fieldLabels = new EnumMap<>(ProviderField.class);
    private final Map<ProviderField, ConfigInfoButton> infoButtons =
        new EnumMap<>(ProviderField.class);
    private ExternalProviderSettings.Settings stored;

    private ProviderForm(String provider) {
      this.provider = provider;
      assistantKicker.addClassName("provider-assistant-kicker");
      assistantKicker.add(AppIcon.BOT.create(), assistantKickerText);
      assistantTitle.addClassName("provider-assistant-title");
      assistantDescription.addClassName("provider-assistant-description");
      Div introduction = new Div(assistantKicker, assistantTitle, assistantDescription);
      introduction.addClassName("provider-assistant-intro");
      status.addClassName("provider-status");
      status.setId(PROVIDER_PREFIX + provider + "-status");
      status.setIcon(new Image(providerLogo(provider), ""));
      status.getElement().setAttribute("tabindex", "0");
      console =
          new Anchor(
              provider.equals(GOOGLE)
                  ? "https://console.cloud.google.com/auth/clients"
                  : "https://entra.microsoft.com/#view/Microsoft_AAD_RegisteredApps/ApplicationsListBlade",
              "");
      console.setTarget("_blank");
      console.getElement().setAttribute("rel", "noopener noreferrer");
      console.addClassName("provider-console-link");
      console.add(AppIcon.EXTERNAL_LINK_TABLER.create(), consoleCaption);
      redirect.setReadOnly(true);
      redirect.setAutoselect(true);
      redirect.setWidthFull();
      redirect.setId(PROVIDER_PREFIX + provider + "-redirect");
      copy.addClassName("provider-copy");
      copy.setPrefixComponent(AppIcon.CLIPBOARD_COPY.create());
      copy.addClickListener(event ->
          redirect
              .getElement()
              .executeJs(
                  "return navigator.clipboard ? navigator.clipboard.writeText($0).then(() => true, () => false) : Promise.resolve(false)",
                  redirect.getValue())
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
      Div address = new Div(redirect, copy);
      address.addClassName("provider-redirect");
      clientId.setWidthFull();
      clientId.setMaxLength(512);
      clientId.setId(PROVIDER_PREFIX + provider + "-client-id");
      clientId.setValueChangeMode(ValueChangeMode.EAGER);
      secret.setWidthFull();
      secret.setMaxLength(2048);
      secret.setId(PROVIDER_PREFIX + provider + "-secret");
      secret.getElement().setAttribute("autocomplete", "new-password");
      secret.setValueChangeMode(ValueChangeMode.EAGER);
      Div credentials =
          new Div(
              fieldWithHelp(clientId, ProviderField.CLIENT_ID),
              fieldWithHelp(secret, ProviderField.SECRET));
      credentials.addClassName("provider-credentials");
      enabled.setId(PROVIDER_PREFIX + provider + "-enabled");
      enabled.setWidthFull();
      enabled.addClassName("cosmos-config-switch");
      enabled.setLabelComponent(enabledLabel);
      Div activation = new Div(enabled);
      activation.addClassName("provider-activation");
      Div consoleGuide = new Div(guide, console);
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
      Div info = new Div(AppIcon.INFO_CIRCLE.create(), hint);
      info.addClassName("cosmos-config-save-hint");
      reload.setPrefixComponent(AppIcon.REFRESH.create());
      reload.addClickListener(event -> load());
      save.setPrefixComponent(AppIcon.CHECK.create());
      save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
      save.setId(PROVIDER_PREFIX + provider + "-save");
      save.addClickListener(event -> save());
      Div actions = new Div(reload, save);
      actions.addClassName("cosmos-config-card-actions");
      Div footer = new Div(info, actions);
      footer.addClassName("cosmos-panel-footer");
      content = new Div(fields, footer);
      content.setId(PROVIDER_PREFIX + provider + "-content");
      content.addClassName("provider-content");
      clientId.addValueChangeListener(event -> refreshAvailability());
      secret.addValueChangeListener(event -> refreshAvailability());
      enabled.addValueChangeListener(event -> refreshAvailability());
    }

    private void openHelp(ProviderField field) {
      helpForm = this;
      helpField = field;
      helpForm.refreshHelp();
      helpPanel.open();
    }

    private String fieldRule(ProviderField field) {
      return field.rule(messageProperties);
    }

    private void refreshHelp() {
      helpPanel.removeAll();
      helpPanel.setHeading(messageProperties.getAdminConfigHelpTitle());
      helpPanel.setDescription(messageProperties.getAdminConfigHelpDescription());
      helpPanel.refreshCloseText(messageProperties);
      Span group = new Span(provider.equals(GOOGLE) ? GOOGLE_NAME : MICROSOFT_NAME);
      group.addClassName("admin-help-group");
      H3 fieldTitle = new H3(fieldLabel(helpField));
      fieldTitle.addClassName("admin-help-field-title");
      Span explanation = new Span(fieldHelp(helpField));
      explanation.addClassName("admin-help-description");
      UnorderedList rules = new UnorderedList(new ListItem(fieldRule(helpField)));
      Div rulesSection = new Div(new H4(messageProperties.getAdminConfigInputRules()), rules);
      rulesSection.addClassName("admin-help-rules");
      helpPanel.add(group, fieldTitle, explanation, rulesSection);
      String timing =
          switch (helpField) {
            case CLIENT_ID -> messageProperties.getAdminConfigEffectiveProviderClient();
            case SECRET -> messageProperties.getAdminConfigEffectiveProviderSecret();
            case ENABLED -> messageProperties.getAdminConfigEffectiveProviderEnabled();
          };
      helpPanel.addEffectiveUse(
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
              + provider
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
      return field.description(messageProperties, provider);
    }

    private void load() {
      stored = externalProviderSettings.load(provider);
      clientId.setValue(stored.clientId());
      secret.clear();
      enabled.setValue(stored.enabled());
      clientId.setInvalid(false);
      secret.setInvalid(false);
      // BASE_URL ist dieselbe vertrauenswürdige öffentliche Adresse wie für Bestätigungslinks.
      String base = configService.getString(ConfigEntry.BASE_URL);
      URI origin = URI.create(base);
      redirect.setValue(origin.resolve("/login/oauth2/code/" + provider).toString());
      refreshTexts();
    }

    private void save() {
      if (twoFactorAuthorization != null
          && twoFactorAuthorization.requires(request, TwoFactorAction.SECURITY_CONFIGURATION)) {
        twoFactorConfirmationDialog.open(
            TwoFactorAction.SECURITY_CONFIGURATION, this::saveConfirmed);
      } else {
        saveConfirmed();
      }
    }

    private void saveConfirmed() {
      clientId.setInvalid(enabled.getValue() && clientId.getValue().isBlank());
      secret.setInvalid(
          enabled.getValue() && secret.getValue().isBlank() && !stored.secretStored());
      try {
        stored =
            externalProviderSettings.save(
                provider,
                clientId.getValue(),
                secret.getValue(),
                enabled.getValue(),
                stored.revision());
        secret.clear();
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
      String name = provider.equals(GOOGLE) ? GOOGLE_NAME : MICROSOFT_NAME;
      assistantKickerText.setText(messageProperties.getProviderSettingsAssistantKicker());
      assistantTitle.setText(messageProperties.getProviderSettingsAssistantTitle(name));
      assistantDescription.setText(messageProperties.getProviderSettingsAssistantDescription());
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
      guide.setText(
          provider.equals(GOOGLE)
              ? messageProperties.getProviderSettingsGoogleGuide()
              : messageProperties.getProviderSettingsMicrosoftGuide());
      consoleCaption.setText(messageProperties.getProviderSettingsOpenConsole());
      redirect.setLabel(messageProperties.getProviderSettingsRedirect());
      copy.setText(messageProperties.getProviderSettingsCopy());
      clientId.setLabel(messageProperties.getProviderSettingsClientId());
      clientId.setPlaceholder(messageProperties.getProviderSettingsClientIdPlaceholder());
      secret.setLabel(messageProperties.getProviderSettingsSecret());
      secret.setPlaceholder(
          stored != null && stored.secretStored()
              ? messageProperties.getProviderSettingsSecretStored()
              : messageProperties.getProviderSettingsSecretPlaceholder());
      secret.setHelperText(messageProperties.getProviderSettingsSecretHint());
      enabledLabel.setText(messageProperties.getProviderSettingsEnable());
      enabled.setAriaLabel(messageProperties.getProviderSettingsEnable());
      hint.setText(messageProperties.getProviderSettingsHint());
      reload.setText(messageProperties.getAdminConfigReload());
      save.setText(messageProperties.getAdminConfigSave());
      refreshAvailability();
      refreshStatus();
    }

    private void refreshAvailability() {
      boolean credentialsPresent =
          !clientId.getValue().isBlank()
              && (!secret.getValue().isBlank() || stored != null && stored.secretStored());
      boolean editable = credentialsPresent || enabled.getValue();
      enabled.setEnabled(editable);
      enabledLabel.getElement().setAttribute("aria-disabled", Boolean.toString(!editable));
    }

    private void refreshStatus() {
      boolean configured = stored != null && !stored.clientId().isBlank() && stored.secretStored();
      boolean active = configured && stored.enabled();
      String name = provider.equals(GOOGLE) ? GOOGLE_NAME : MICROSOFT_NAME;
      String state =
          active
              ? messageProperties.getProviderSettingsActive()
              : messageProperties.getProviderSettingsInactive();
      status.setText(state);
      status.getElement().setAttribute("aria-label", name + ": " + state);
      status.removeThemeVariants(BadgeVariant.SUCCESS, BadgeVariant.WARNING);
      status.addThemeVariants(active ? BadgeVariant.SUCCESS : BadgeVariant.WARNING);
      status.getElement().setAttribute("data-state", active ? "active" : "inactive");
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
