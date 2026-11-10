package de.derpeterson.app.views.admin;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Switch;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.TabSheetVariant;
import de.derpeterson.app.config.ConfigurationCatalog;
import de.derpeterson.app.helper.ui.EqualButtonWidths;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.security.mfa.TwoFactorAction;
import de.derpeterson.app.security.mfa.TwoFactorAuthorization;
import de.derpeterson.app.security.mfa.TwoFactorMode;
import de.derpeterson.app.security.mfa.TwoFactorPolicies;
import de.derpeterson.app.security.mfa.TwoFactorPolicy;
import de.derpeterson.app.ui.components.AdminDurationField;
import de.derpeterson.app.ui.components.AdminSidePanel;
import de.derpeterson.app.ui.components.AppIcon;
import de.derpeterson.app.ui.components.ConfigInfoButton;
import de.derpeterson.app.ui.components.CosmosCardComponent;
import de.derpeterson.app.ui.components.CosmosClasses;
import de.derpeterson.app.ui.components.CosmosSectionHeaderComponent;
import de.derpeterson.app.ui.components.PortalFields;
import de.derpeterson.app.ui.components.TwoFactorActionDialog;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.dao.OptimisticLockingFailureException;

/** Eine zusammen gespeicherte, rollenbezogene MFA-Konfiguration ausschließlich für Superadmins. */
@StyleSheet("custom-theme/cosmos-mfa.css")
@StyleSheet("custom-theme/cosmos-mfa-confirmation.css")
@StyleSheet("custom-theme/cosmos-provider-settings.css")
public final class AdminTwoFactorSection extends VerticalLayout {
  private static final String CONFIG_SWITCH_CLASS = "cosmos-config-switch";
  private static final TwoFactorPolicy DEFAULT_POLICY = TwoFactorPolicy.defaults();
  private static final String DEFAULT_INACTIVITY_SECONDS =
      Long.toString(DEFAULT_POLICY.inactivity().toSeconds());

  private record FieldHelp(
      Supplier<String> groupTextSupplier,
      Supplier<String> titleTextSupplier,
      Supplier<String> descriptionTextSupplier,
      Supplier<String> defaultValueTextSupplier,
      Supplier<List<String>> ruleTextsSupplier,
      Supplier<String> effectiveUseTextSupplier) {
    String tooltip() {
      return descriptionTextSupplier.get() + " " + String.join(" ", ruleTextsSupplier.get());
    }
  }

  private final transient MessageProperties messageProperties;
  private final transient TwoFactorPolicies twoFactorPolicies;
  private final transient TwoFactorAuthorization twoFactorAuthorization;
  private final transient HttpServletRequest httpRequest;
  private final Map<RoleType, Map<TwoFactorAction, ComboBox<TwoFactorMode>>> rules =
      new EnumMap<>(RoleType.class);
  private final Map<RoleType, Tab> roleTabs = new EnumMap<>(RoleType.class);
  private final List<Runnable> fieldTextUpdates = new ArrayList<>();
  private final List<ConfigInfoButton> infoButtons = new ArrayList<>();
  private final Span configurationBreadcrumbSpan = new Span();
  private final Span pageTitle = new Span();
  private final Span pageHint = new Span();
  private final H2 twoFactorPolicyHeading = new H2();
  private final Span twoFactorPolicyDescriptionSpan = new Span();
  private final Span saveHint = new Span();
  private final Switch reconfirmAfterInactivitySwitch = new Switch();
  private final Span durationLabel = new Span();
  private final AdminDurationField inactivityDurationField;
  private final Switch developmentPause = new Switch();
  private final Switch developmentGoogle = new Switch();
  private final Span developmentTitle = new Span();
  private final Span developmentWarning = new Span();
  private final Button reloadPolicyButton = new Button();
  private final Button savePolicyButton = new Button();
  private final AdminSidePanel fieldHelpPanel;
  private final Button closeFieldHelpButton = new Button();
  private final TwoFactorActionDialog twoFactorConfirmationDialog;
  private TwoFactorPolicies.Snapshot twoFactorPolicySnapshot;
  private transient Runnable refreshHelp;

  public AdminTwoFactorSection(
      MessageProperties messageProperties,
      TwoFactorPolicies twoFactorPolicies,
      TwoFactorAuthorization twoFactorAuthorization,
      HttpServletRequest httpRequest) {
    this.messageProperties = messageProperties;
    this.twoFactorPolicies = twoFactorPolicies;
    this.twoFactorAuthorization = twoFactorAuthorization;
    this.httpRequest = httpRequest;
    setWidthFull();
    setPadding(false);
    setSpacing(false);
    addClassNames("cosmos-admin-section", "cosmos-provider-settings", "cosmos-mfa-settings");
    setId("admin-two-factor-settings");
    add(new CosmosSectionHeaderComponent(configurationBreadcrumbSpan, pageTitle, pageHint));
    twoFactorConfirmationDialog =
        new TwoFactorActionDialog(messageProperties, twoFactorAuthorization, httpRequest);
    fieldHelpPanel = new AdminSidePanel(messageProperties, AppIcon.INFO_CIRCLE);
    fieldHelpPanel.addClassName("admin-config-help");
    fieldHelpPanel.setId("two-factor-field-help");
    closeFieldHelpButton.setWidthFull();
    closeFieldHelpButton.setPrefixComponent(AppIcon.CLOSE.create());
    closeFieldHelpButton.addClickListener(event -> fieldHelpPanel.close());
    fieldHelpPanel.getFooter().add(closeFieldHelpButton);
    twoFactorPolicyHeading.addClassName(CosmosClasses.PANEL_TITLE);
    twoFactorPolicyDescriptionSpan.addClassName(CosmosClasses.PANEL_DESCRIPTION);
    Div heading = new Div(twoFactorPolicyHeading, twoFactorPolicyDescriptionSpan);
    heading.addClassName(CosmosClasses.PANEL_HEADING);
    Div icon = new Div(AppIcon.SHIELD_KEYHOLE.create());
    icon.addClassName(CosmosClasses.PANEL_ICON);
    Div header = new Div(icon, heading);
    header.addClassNames(CosmosClasses.PANEL_HEADER, "mfa-settings-header");
    TabSheet tabs = new TabSheet();
    tabs.setId("two-factor-role-tabs");
    tabs.addClassName(CosmosClasses.PANEL_TABS);
    tabs.setWidthFull();
    tabs.addThemeVariants(TabSheetVariant.NO_PADDING);
    for (RoleType role : RoleType.values()) {
      Map<TwoFactorAction, ComboBox<TwoFactorMode>> selections =
          new EnumMap<>(TwoFactorAction.class);
      VerticalLayout groups = new VerticalLayout();
      groups.setPadding(false);
      groups.addClassName("mfa-rule-groups");
      groups.add(
          ruleGroup(
              role,
              selections,
              AppIcon.NUMBER_ONE,
              messageProperties::getTwoFactorAccessGroup,
              List.of(TwoFactorAction.LOGIN, TwoFactorAction.ADMINISTRATION)),
          ruleGroup(
              role,
              selections,
              AppIcon.NUMBER_TWO,
              messageProperties::getTwoFactorAccountGroup,
              List.of(
                  TwoFactorAction.PASSWORD,
                  TwoFactorAction.EMAIL,
                  TwoFactorAction.SECURITY_CONFIGURATION)),
          ruleGroup(
              role,
              selections,
              AppIcon.NUMBER_THREE,
              messageProperties::getTwoFactorMethodsGroup,
              List.of(
                  TwoFactorAction.LINK_PROVIDER,
                  TwoFactorAction.UNLINK_PROVIDER,
                  TwoFactorAction.MANAGE_FACTOR,
                  TwoFactorAction.MANAGE_RECOVERY_CODES)));
      Div content = new Div(groups, roleGuide(role));
      content.addClassName("mfa-role-content");
      rules.put(role, selections);
      Tab tab = new Tab();
      roleTabs.put(role, tab);
      tabs.add(tab, content);
    }
    reconfirmAfterInactivitySwitch.setId("two-factor-inactivity-enabled");
    reconfirmAfterInactivitySwitch.addClassName(CONFIG_SWITCH_CLASS);
    reconfirmAfterInactivitySwitch.setWidthFull();
    inactivityDurationField = new AdminDurationField(messageProperties);
    inactivityDurationField.setAvailableUnits(
        AdminDurationField.Unit.MINUTES,
        AdminDurationField.Unit.HOURS,
        AdminDurationField.Unit.DAYS);
    inactivityDurationField.setId("two-factor-inactivity-duration");
    inactivityDurationField.setDefaultValue(DEFAULT_INACTIVITY_SECONDS);
    durationLabel.setId("two-factor-inactivity-label");
    durationLabel.addClassName("provider-field-label");
    inactivityDurationField.setAriaLabelledBy("two-factor-inactivity-label");
    Div durationField =
        new Div(
            durationLabel,
            withHelp(
                inactivityDurationField,
                new FieldHelp(
                    messageProperties::getTwoFactorInactivityGroup,
                    messageProperties::getTwoFactorInactivityDuration,
                    messageProperties::getTwoFactorInactivityDescription,
                    () -> inactivityDurationField.displayStoredValue(DEFAULT_INACTIVITY_SECONDS),
                    () ->
                        List.of(
                            messageProperties.getAdminConfigDurationNumberRule(),
                            messageProperties.getTwoFactorInactivityRange()),
                    messageProperties::getAdminConfigEffectiveTwoFactorInactivityDuration)));
    durationField.addClassName("provider-help-field");
    Div common =
        new Div(
            withHelp(
                reconfirmAfterInactivitySwitch,
                new FieldHelp(
                    messageProperties::getTwoFactorInactivityGroup,
                    messageProperties::getTwoFactorInactivityEnabled,
                    messageProperties::getTwoFactorInactivityDescription,
                    () ->
                        DEFAULT_POLICY.inactivityEnabled()
                            ? messageProperties.getAdminUsersYes()
                            : messageProperties.getAdminUsersNo(),
                    () -> List.of(messageProperties.getTwoFactorInactivityRange()),
                    messageProperties::getAdminConfigEffectiveTwoFactorInactivitySwitch)),
            durationField);
    common.addClassName("mfa-policy-grid");
    VerticalLayout inactivitySection =
        new VerticalLayout(
            sectionHeading(AppIcon.NUMBER_FOUR, messageProperties::getTwoFactorInactivityGroup),
            common);
    inactivitySection.setSpacing(false);
    inactivitySection.setPadding(false);
    inactivitySection.addClassName("mfa-inactivity-section");
    reconfirmAfterInactivitySwitch.addValueChangeListener(
        event -> inactivityDurationField.setControlsEnabled(event.getValue()));
    developmentPause.addClassName(CONFIG_SWITCH_CLASS);
    developmentPause.setWidthFull();
    developmentGoogle.addClassName(CONFIG_SWITCH_CLASS);
    developmentGoogle.setWidthFull();
    developmentPause.setId("two-factor-development-pause");
    developmentGoogle.setId("two-factor-development-google");
    developmentTitle.addClassName("provider-kicker");
    developmentWarning.addClassName(CosmosClasses.PANEL_DESCRIPTION);
    Div development =
        new Div(developmentTitle, developmentWarning, developmentPause, developmentGoogle);
    development.addClassName("mfa-development-options");
    development.setVisible(twoFactorPolicies.developmentOptionsAvailable());
    Div body = new Div(tabs, inactivitySection, development);
    body.addClassName("mfa-settings-body");
    Div info = new Div(AppIcon.INFO_CIRCLE.create(), saveHint);
    info.addClassName("cosmos-config-save-hint");
    reloadPolicyButton.setId("two-factor-reload");
    reloadPolicyButton.setPrefixComponent(AppIcon.REFRESH.create());
    reloadPolicyButton.addClickListener(event -> reloadTwoFactorPolicy());
    savePolicyButton.setId("two-factor-save");
    savePolicyButton.setPrefixComponent(AppIcon.CHECK.create());
    savePolicyButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    savePolicyButton.addClickListener(event -> saveRequested());
    Div actions = new Div(reloadPolicyButton, savePolicyButton);
    actions.addClassName("cosmos-config-card-actions");
    EqualButtonWidths.configureEqualButtonWidths(actions);
    Div footer = new Div(info, actions);
    footer.addClassName("cosmos-panel-footer");
    CosmosCardComponent card = new CosmosCardComponent(header, body, footer);
    card.addClassName(CosmosClasses.PANEL_CARD);
    add(card, fieldHelpPanel, twoFactorConfirmationDialog);
    refreshTexts();
  }

  private VerticalLayout ruleGroup(
      RoleType role,
      Map<TwoFactorAction, ComboBox<TwoFactorMode>> selections,
      AppIcon icon,
      Supplier<String> heading,
      List<TwoFactorAction> actions) {
    Div fields = new Div();
    fields.addClassName("mfa-policy-grid");
    for (TwoFactorAction action : actions) {
      ComboBox<TwoFactorMode> modeField = new ComboBox<>();
      modeField.setId(
          "two-factor-rule-"
              + role.name().toLowerCase(Locale.ROOT)
              + "-"
              + action.name().toLowerCase(Locale.ROOT));
      modeField.setWidthFull();
      modeField.setItems(TwoFactorMode.values());
      modeField.setAllowCustomValue(false);
      PortalFields.styleChoice(modeField, this::modeLabel, false);
      modeField.addValueChangeListener(event -> {
        if (event.getValue() == null && event.getOldValue() != null) {
          modeField.setValue(event.getOldValue());
        }
      });
      selections.put(action, modeField);
      fields.add(
          withHelp(
              modeField,
              new FieldHelp(
                  heading,
                  () -> actionLabel(action),
                  () -> ruleDescription(action),
                  () -> modeLabel(DEFAULT_POLICY.rules().get(role).get(action)),
                  messageProperties::twoFactorRuleHelpItems,
                  messageProperties::getAdminConfigEffectiveTwoFactorRule)));
    }
    VerticalLayout group = new VerticalLayout(sectionHeading(icon, heading), fields);
    group.setPadding(false);
    group.setSpacing(false);
    return group;
  }

  private H3 sectionHeading(AppIcon icon, Supplier<String> label) {
    Span caption = new Span();
    fieldTextUpdates.add(() -> caption.setText(label.get()));
    H3 heading = new H3();
    heading.add(icon.create(), caption);
    heading.addClassName("mfa-config-heading");
    return heading;
  }

  private VerticalLayout roleGuide(RoleType role) {
    Span scope = new Span();
    fieldTextUpdates.add(() -> refreshRoleScope(scope, role));
    HorizontalLayout scopeInfo = new HorizontalLayout(AppIcon.INFO_CIRCLE.create(), scope);
    scopeInfo.setPadding(false);
    scopeInfo.setSpacing(false);
    scopeInfo.setAlignItems(Alignment.CENTER);
    scopeInfo.addClassName("mfa-role-scope");
    VerticalLayout guide =
        new VerticalLayout(
            sectionHeading(AppIcon.INFO_CIRCLE, messageProperties::getTwoFactorRoleGuide),
            guideItem(
                AppIcon.NUMBER_ONE,
                messageProperties::getTwoFactorRolePriority,
                messageProperties::getTwoFactorRolePriorityHint),
            guideItem(
                AppIcon.NUMBER_TWO,
                messageProperties::getTwoFactorExistingFactors,
                messageProperties::getTwoFactorExistingFactorsHint),
            guideItem(
                AppIcon.NUMBER_THREE,
                messageProperties::getTwoFactorRuleTiming,
                messageProperties::getTwoFactorSaveHint),
            new Hr(),
            scopeInfo);
    guide.addClassName("mfa-role-guide");
    return guide;
  }

  private void refreshRoleScope(Span scope, RoleType role) {
    String placeholder = "{role}";
    String message = messageProperties.getTwoFactorRoleScope(placeholder);
    int rolePosition = message.indexOf(placeholder);
    if (rolePosition < 0) {
      scope.setText(messageProperties.getTwoFactorRoleScope(roleLabel(role)));
      return;
    }
    Span roleName = new Span(roleLabel(role));
    roleName.addClassName("mfa-role-scope-name");
    scope.removeAll();
    scope.add(
        new Text(message.substring(0, rolePosition)),
        roleName,
        new Text(message.substring(rolePosition + placeholder.length())));
  }

  private HorizontalLayout guideItem(
      AppIcon icon,
      Supplier<String> guideTitleTextSupplier,
      Supplier<String> guideDescriptionTextSupplier) {
    Span heading = new Span();
    heading.addClassName("mfa-guide-item-title");
    Span copy = new Span();
    copy.addClassName(CosmosClasses.PANEL_DESCRIPTION);
    fieldTextUpdates.add(() -> {
      heading.setText(guideTitleTextSupplier.get());
      copy.setText(guideDescriptionTextSupplier.get());
    });
    VerticalLayout text = new VerticalLayout(heading, copy);
    text.setPadding(false);
    text.setSpacing(false);
    HorizontalLayout item = new HorizontalLayout(icon.create(), text);
    item.addClassName("mfa-guide-item");
    return item;
  }

  public void activate() {
    twoFactorPolicySnapshot = twoFactorPolicies.load();
    twoFactorPolicySnapshot
        .policy()
        .rules()
        .forEach((role, actions) ->
            actions.forEach((action, mode) -> rules.get(role).get(action).setValue(mode)));
    reconfirmAfterInactivitySwitch.setValue(twoFactorPolicySnapshot.policy().inactivityEnabled());
    inactivityDurationField.setStoredValue(
        Long.toString(twoFactorPolicySnapshot.policy().inactivity().toSeconds()));
    inactivityDurationField.setControlsEnabled(reconfirmAfterInactivitySwitch.getValue());
    developmentPause.setValue(twoFactorPolicySnapshot.policy().developmentPause());
    developmentGoogle.setValue(twoFactorPolicySnapshot.policy().developmentGoogleConfirmation());
  }

  private void reloadTwoFactorPolicy() {
    try {
      activate();
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getAdminConfigReloadSuccess,
              NotificationHelper.NotificationType.SUCCESS);
    } catch (RuntimeException failure) {
      showTwoFactorPolicyFailure(failure);
    }
  }

  private void saveRequested() {
    if (twoFactorPolicySnapshot == null) {
      return;
    }
    try {
      Map<RoleType, Map<TwoFactorAction, TwoFactorMode>> desiredRules =
          new EnumMap<>(RoleType.class);
      rules.forEach((role, actions) -> {
        Map<TwoFactorAction, TwoFactorMode> modes = new EnumMap<>(TwoFactorAction.class);
        actions.forEach((action, modeField) -> modes.put(action, modeField.getValue()));
        desiredRules.put(role, modes);
      });
      Duration period =
          inactivityDurationField.getStoredValue().isBlank()
                  && !Boolean.TRUE.equals(reconfirmAfterInactivitySwitch.getValue())
              ? twoFactorPolicySnapshot.policy().inactivity()
              : Duration.ofSeconds(Long.parseLong(inactivityDurationField.getStoredValue()));
      TwoFactorPolicy desired =
          new TwoFactorPolicy(
              desiredRules,
              reconfirmAfterInactivitySwitch.getValue(),
              period,
              twoFactorPolicies.developmentOptionsAvailable() && developmentPause.getValue(),
              twoFactorPolicies.developmentOptionsAvailable() && developmentGoogle.getValue());
      long revision = twoFactorPolicySnapshot.revision();
      if (twoFactorAuthorization.requires(httpRequest, TwoFactorAction.SECURITY_CONFIGURATION)) {
        twoFactorConfirmationDialog.open(
            TwoFactorAction.SECURITY_CONFIGURATION, () -> saveTwoFactorPolicy(revision, desired));
      } else {
        saveTwoFactorPolicy(revision, desired);
      }
    } catch (RuntimeException failure) {
      showTwoFactorPolicyFailure(failure);
    }
  }

  private void saveTwoFactorPolicy(long revision, TwoFactorPolicy desired) {
    try {
      twoFactorPolicySnapshot = twoFactorPolicies.save(httpRequest, revision, desired);
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getTwoFactorSaved, NotificationHelper.NotificationType.SUCCESS);
    } catch (RuntimeException failure) {
      showTwoFactorPolicyFailure(failure);
    }
  }

  private void showTwoFactorPolicyFailure(RuntimeException failure) {
    NotificationHelper.getInstance()
        .showNotification(
            messageProperties::getBaseFailedTitle,
            failure instanceof OptimisticLockingFailureException
                ? messageProperties::getTwoFactorConflict
                : messageProperties::getTwoFactorFailed,
            NotificationHelper.NotificationType.ERROR);
  }

  private Div withHelp(Component field, FieldHelp fieldHelp) {
    ConfigInfoButton button =
        new ConfigInfoButton(
            fieldHelp::tooltip,
            () -> {
              refreshHelp = () -> renderHelp(fieldHelp);
              refreshHelp.run();
              fieldHelpPanel.open();
            });
    Div row = new Div(field, button);
    row.addClassName("cosmos-config-input-row");
    infoButtons.add(button);
    fieldTextUpdates.add(() ->
        button.setAriaLabel(
            messageProperties.getAdminConfigHelpTitle()
                + " – "
                + fieldHelp.titleTextSupplier().get()));
    if (field instanceof ComboBox<?> modeField) {
      Span caption = new Span();
      caption.setId(field.getId().orElseThrow() + "-label");
      caption.addClassName("provider-field-label");
      modeField.setAriaLabelledBy(caption.getId().orElseThrow());
      fieldTextUpdates.add(() -> caption.setText(fieldHelp.titleTextSupplier().get()));
      Div wrapper = new Div(caption, row);
      wrapper.addClassName("provider-help-field");
      return wrapper;
    }
    return row;
  }

  private void renderHelp(FieldHelp fieldHelp) {
    fieldHelpPanel.setHeading(messageProperties.getAdminConfigHelpTitle());
    fieldHelpPanel.setDescription(messageProperties.getAdminConfigHelpDescription());
    Span group = new Span(fieldHelp.groupTextSupplier().get());
    group.addClassName("admin-help-group");
    H3 fieldTitle = new H3(fieldHelp.titleTextSupplier().get());
    fieldTitle.addClassName("admin-help-field-title");
    Span description = new Span(fieldHelp.descriptionTextSupplier().get());
    description.addClassName("admin-help-description");
    Div defaultValue =
        new Div(
            new Span(messageProperties.getAdminConfigDefault()),
            new Span(fieldHelp.defaultValueTextSupplier().get()));
    defaultValue.addClassName("admin-help-default");
    UnorderedList rulesList = new UnorderedList();
    fieldHelp.ruleTextsSupplier().get().forEach(rule -> rulesList.add(new ListItem(rule)));
    Div rulesSection = new Div(new H4(messageProperties.getAdminConfigInputRules()), rulesList);
    rulesSection.addClassName("admin-help-rules");
    fieldHelpPanel.removeAll();
    fieldHelpPanel.add(group, fieldTitle, description, defaultValue, rulesSection);
    fieldHelpPanel.addEffectiveUse(
        messageProperties,
        List.of(
            fieldHelp.effectiveUseTextSupplier().get(),
            messageProperties.getAdminConfigEffectiveTwoFactorExisting()));
  }

  public void refreshTexts() {
    configurationBreadcrumbSpan.setText(
        messageProperties.getAdminHeaderTitle() + " / " + messageProperties.getAdminConfigTitle());
    pageTitle.setText(messageProperties.getAdminConfigTitle());
    pageHint.setText(messageProperties.getTwoFactorSettingsPageHint());
    twoFactorPolicyHeading.setText(messageProperties.getTwoFactorSettingsTitle());
    twoFactorPolicyDescriptionSpan.setText(messageProperties.getTwoFactorSettingsHint());
    roleTabs.forEach((role, tab) -> {
      tab.removeAll();
      tab.add(new Span(roleLabel(role)));
    });
    rules.forEach((role, actions) ->
        actions.forEach((action, modeField) -> modeField.setItemLabelGenerator(this::modeLabel)));
    fieldTextUpdates.forEach(Runnable::run);
    infoButtons.forEach(ConfigInfoButton::refreshText);
    reconfirmAfterInactivitySwitch.setLabel(messageProperties.getTwoFactorInactivityEnabled());
    durationLabel.setText(messageProperties.getTwoFactorInactivityDuration());
    inactivityDurationField.refreshTexts();
    developmentTitle.setText(messageProperties.getTwoFactorDevelopmentTitle());
    developmentWarning.setText(messageProperties.getTwoFactorDevelopmentWarning());
    developmentPause.setLabel(messageProperties.getTwoFactorDevelopmentPause());
    developmentGoogle.setLabel(messageProperties.getTwoFactorDevelopmentGoogle());
    saveHint.setText(messageProperties.getTwoFactorSaveHint());
    reloadPolicyButton.setText(messageProperties.getAdminConfigReload());
    savePolicyButton.setText(messageProperties.getAdminConfigSave());
    closeFieldHelpButton.setText(messageProperties.getAdminUsersCloseDialog());
    fieldHelpPanel.refreshCloseText(messageProperties);
    if (refreshHelp != null) {
      refreshHelp.run();
    }
    twoFactorConfirmationDialog.refreshTexts();
  }

  private String modeLabel(TwoFactorMode mode) {
    return switch (mode) {
      case OFF -> messageProperties.getTwoFactorModeOff();
      case IF_ENROLLED -> messageProperties.getTwoFactorModeEnrolled();
      case REQUIRED -> messageProperties.getTwoFactorModeRequired();
    };
  }

  private String roleLabel(RoleType role) {
    return switch (role) {
      case ROLE_SUPERADMIN -> messageProperties.getRoleSuperadmin();
      case ROLE_ADMIN -> messageProperties.getRoleAdmin();
      case ROLE_USER -> messageProperties.getRoleUser();
      case ROLE_GUEST -> messageProperties.getRoleGuest();
    };
  }

  private String actionLabel(TwoFactorAction action) {
    return ConfigurationCatalog.actionLabel(action, messageProperties);
  }

  private String ruleDescription(TwoFactorAction action) {
    return switch (action) {
      case ADMINISTRATION -> messageProperties.getTwoFactorAdminAccessHelp();
      case SECURITY_CONFIGURATION -> messageProperties.getTwoFactorSaveHint();
      default -> messageProperties.getTwoFactorRulesHint();
    };
  }
}
