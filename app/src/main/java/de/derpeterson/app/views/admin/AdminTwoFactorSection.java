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
import de.derpeterson.app.ui.components.AdminSidePanelDialog;
import de.derpeterson.app.ui.components.AppIcon;
import de.derpeterson.app.ui.components.ConfigInfoButton;
import de.derpeterson.app.ui.components.CosmosCardComponent;
import de.derpeterson.app.ui.components.CosmosComboBoxStyling;
import de.derpeterson.app.ui.components.CosmosCssClasses;
import de.derpeterson.app.ui.components.CosmosSectionHeaderComponent;
import de.derpeterson.app.ui.components.TwoFactorActionDialog;
import de.derpeterson.app.ui.components.UnsavedChangesDialog;
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
  private final transient HttpServletRequest httpServletRequest;
  private final Map<RoleType, Map<TwoFactorAction, ComboBox<TwoFactorMode>>>
      ruleModeComboBoxesByRole = new EnumMap<>(RoleType.class);
  private final Map<RoleType, Tab> tabsByRole = new EnumMap<>(RoleType.class);
  private final List<Runnable> fieldTextRefreshActions = new ArrayList<>();
  private final List<ConfigInfoButton> fieldInfoButtons = new ArrayList<>();
  private final Span configurationBreadcrumbSpan = new Span();
  private final Span configurationTitleSpan = new Span();
  private final Span configurationHintSpan = new Span();
  private final H2 twoFactorPolicyHeadingH2 = new H2();
  private final Span twoFactorPolicyDescriptionSpan = new Span();
  private final Span policySaveHintSpan = new Span();
  private final Switch reconfirmAfterInactivitySwitch = new Switch();
  private final Span inactivityDurationLabelSpan = new Span();
  private final AdminDurationField inactivityDurationField;
  private final Switch developmentPauseSwitch = new Switch();
  private final Switch developmentGoogleConfirmationSwitch = new Switch();
  private final Span developmentOptionsTitleSpan = new Span();
  private final Span developmentWarningSpan = new Span();
  private final Button reloadPolicyButton = new Button();
  private final Button savePolicyButton = new Button();
  private final AdminSidePanelDialog fieldHelpPanel;
  private final Button closeFieldHelpButton = new Button();
  private final TwoFactorActionDialog twoFactorConfirmationDialog;
  private TwoFactorPolicies.Snapshot twoFactorPolicySnapshot;
  private transient Runnable refreshFieldHelpAction;

  public AdminTwoFactorSection(
      MessageProperties messageProperties,
      TwoFactorPolicies twoFactorPolicies,
      TwoFactorAuthorization twoFactorAuthorization,
      HttpServletRequest httpServletRequest) {
    this.messageProperties = messageProperties;
    this.twoFactorPolicies = twoFactorPolicies;
    this.twoFactorAuthorization = twoFactorAuthorization;
    this.httpServletRequest = httpServletRequest;

    setWidthFull();
    setPadding(false);
    setSpacing(false);
    addClassNames("cosmos-admin-section", "cosmos-provider-settings", "cosmos-mfa-settings");
    setId("admin-two-factor-settings");
    add(
        new CosmosSectionHeaderComponent(
            configurationBreadcrumbSpan, configurationTitleSpan, configurationHintSpan));

    twoFactorConfirmationDialog =
        new TwoFactorActionDialog(messageProperties, twoFactorAuthorization, httpServletRequest);

    fieldHelpPanel = new AdminSidePanelDialog(messageProperties, AppIcon.INFO_CIRCLE);
    fieldHelpPanel.addClassName("admin-config-help");
    fieldHelpPanel.setId("two-factor-field-help");
    closeFieldHelpButton.setWidthFull();
    closeFieldHelpButton.setPrefixComponent(AppIcon.CLOSE.create());
    closeFieldHelpButton.addClickListener(_ -> fieldHelpPanel.close());
    fieldHelpPanel.getFooter().add(closeFieldHelpButton);

    twoFactorPolicyHeadingH2.addClassName(CosmosCssClasses.PANEL_TITLE);
    twoFactorPolicyDescriptionSpan.addClassName(CosmosCssClasses.PANEL_DESCRIPTION);
    Div policyPanelHeadingDiv = new Div(twoFactorPolicyHeadingH2, twoFactorPolicyDescriptionSpan);
    policyPanelHeadingDiv.addClassName(CosmosCssClasses.PANEL_HEADING);
    Div policyIconDiv = new Div(AppIcon.SHIELD_KEYHOLE.create());
    policyIconDiv.addClassName(CosmosCssClasses.PANEL_ICON);
    Div policyPanelHeaderDiv = new Div(policyIconDiv, policyPanelHeadingDiv);
    policyPanelHeaderDiv.addClassNames(CosmosCssClasses.PANEL_HEADER, "mfa-settings-header");

    TabSheet roleTabSheet = new TabSheet();
    roleTabSheet.setId("two-factor-role-tabs");
    roleTabSheet.addClassName(CosmosCssClasses.PANEL_TABS);
    roleTabSheet.setWidthFull();
    roleTabSheet.addThemeVariants(TabSheetVariant.NO_PADDING);
    for (RoleType role : RoleType.values()) {
      Map<TwoFactorAction, ComboBox<TwoFactorMode>> modeComboBoxesByAction =
          new EnumMap<>(TwoFactorAction.class);
      VerticalLayout roleRuleGroupsLayout = new VerticalLayout();
      roleRuleGroupsLayout.setPadding(false);
      roleRuleGroupsLayout.addClassName("mfa-rule-groups");
      roleRuleGroupsLayout.add(
          ruleGroup(
              role,
              modeComboBoxesByAction,
              AppIcon.NUMBER_ONE,
              messageProperties::getTwoFactorAccessGroup,
              List.of(TwoFactorAction.LOGIN, TwoFactorAction.ADMINISTRATION)),
          ruleGroup(
              role,
              modeComboBoxesByAction,
              AppIcon.NUMBER_TWO,
              messageProperties::getTwoFactorAccountGroup,
              List.of(
                  TwoFactorAction.PASSWORD,
                  TwoFactorAction.EMAIL,
                  TwoFactorAction.SECURITY_CONFIGURATION)),
          ruleGroup(
              role,
              modeComboBoxesByAction,
              AppIcon.NUMBER_THREE,
              messageProperties::getTwoFactorMethodsGroup,
              List.of(
                  TwoFactorAction.LINK_PROVIDER,
                  TwoFactorAction.UNLINK_PROVIDER,
                  TwoFactorAction.MANAGE_FACTOR,
                  TwoFactorAction.MANAGE_RECOVERY_CODES)));
      Div roleTabContentDiv = new Div(roleRuleGroupsLayout, roleGuide(role));
      roleTabContentDiv.addClassName("mfa-role-content");
      ruleModeComboBoxesByRole.put(role, modeComboBoxesByAction);
      Tab roleTab = new Tab();
      tabsByRole.put(role, roleTab);
      roleTabSheet.add(roleTab, roleTabContentDiv);
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
    inactivityDurationLabelSpan.setId("two-factor-inactivity-label");
    inactivityDurationLabelSpan.addClassName("provider-field-label");
    inactivityDurationField.setAriaLabelledBy("two-factor-inactivity-label");
    Div inactivityDurationDiv =
        new Div(
            inactivityDurationLabelSpan,
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
    inactivityDurationDiv.addClassName("provider-help-field");

    Div inactivityFieldsDiv =
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
            inactivityDurationDiv);
    inactivityFieldsDiv.addClassName("mfa-policy-grid");
    VerticalLayout inactivitySectionLayout =
        new VerticalLayout(
            sectionHeading(AppIcon.NUMBER_FOUR, messageProperties::getTwoFactorInactivityGroup),
            inactivityFieldsDiv);
    inactivitySectionLayout.setSpacing(false);
    inactivitySectionLayout.setPadding(false);
    inactivitySectionLayout.addClassName("mfa-inactivity-section");
    reconfirmAfterInactivitySwitch.addValueChangeListener(
        event -> inactivityDurationField.setControlsEnabled(event.getValue()));

    developmentPauseSwitch.addClassName(CONFIG_SWITCH_CLASS);
    developmentPauseSwitch.setWidthFull();
    developmentGoogleConfirmationSwitch.addClassName(CONFIG_SWITCH_CLASS);
    developmentGoogleConfirmationSwitch.setWidthFull();
    developmentPauseSwitch.setId("two-factor-development-pause");
    developmentGoogleConfirmationSwitch.setId("two-factor-development-google");
    developmentOptionsTitleSpan.addClassName("provider-kicker");
    developmentWarningSpan.addClassName(CosmosCssClasses.PANEL_DESCRIPTION);
    Div developmentOptionsDiv =
        new Div(
            developmentOptionsTitleSpan,
            developmentWarningSpan,
            developmentPauseSwitch,
            developmentGoogleConfirmationSwitch);
    developmentOptionsDiv.addClassName("mfa-development-options");
    developmentOptionsDiv.setVisible(twoFactorPolicies.developmentOptionsAvailable());

    Div policyPanelBodyDiv = new Div(roleTabSheet, inactivitySectionLayout, developmentOptionsDiv);
    policyPanelBodyDiv.addClassName("mfa-settings-body");
    Div saveHintDiv = new Div(AppIcon.INFO_CIRCLE.create(), policySaveHintSpan);
    saveHintDiv.addClassName("cosmos-config-save-hint");

    reloadPolicyButton.setId("two-factor-reload");
    reloadPolicyButton.setPrefixComponent(AppIcon.REFRESH.create());
    reloadPolicyButton.addClickListener(_ -> requestTwoFactorPolicyReload());

    savePolicyButton.setId("two-factor-save");
    savePolicyButton.setPrefixComponent(AppIcon.CHECK.create());
    savePolicyButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    savePolicyButton.addClickListener(_ -> requestTwoFactorPolicySave());

    Div policyActionsDiv = new Div(reloadPolicyButton, savePolicyButton);
    policyActionsDiv.addClassName("cosmos-config-card-actions");
    EqualButtonWidths.configureEqualButtonWidths(policyActionsDiv);
    Div policyPanelFooterDiv = new Div(saveHintDiv, policyActionsDiv);
    policyPanelFooterDiv.addClassName("cosmos-panel-footer");

    CosmosCardComponent policyCardComponent =
        new CosmosCardComponent(policyPanelHeaderDiv, policyPanelBodyDiv, policyPanelFooterDiv);
    policyCardComponent.addClassName(CosmosCssClasses.PANEL_CARD);
    add(policyCardComponent, fieldHelpPanel, twoFactorConfirmationDialog);
    refreshTexts();
  }

  private VerticalLayout ruleGroup(
      RoleType role,
      Map<TwoFactorAction, ComboBox<TwoFactorMode>> modeComboBoxesByAction,
      AppIcon sectionAppIcon,
      Supplier<String> sectionTitleTextSupplier,
      List<TwoFactorAction> actions) {
    Div ruleFieldsDiv = new Div();
    ruleFieldsDiv.addClassName("mfa-policy-grid");
    for (TwoFactorAction action : actions) {
      ComboBox<TwoFactorMode> ruleModeComboBox = new ComboBox<>();
      ruleModeComboBox.setId(
          "two-factor-rule-"
              + role.name().toLowerCase(Locale.ROOT)
              + "-"
              + action.name().toLowerCase(Locale.ROOT));
      ruleModeComboBox.setWidthFull();
      ruleModeComboBox.setItems(TwoFactorMode.values());
      ruleModeComboBox.setAllowCustomValue(false);
      CosmosComboBoxStyling.styleChoice(ruleModeComboBox, this::modeLabel, true);
      ruleModeComboBox.addValueChangeListener(event -> {
        if (event.getValue() == null && event.getOldValue() != null) {
          ruleModeComboBox.setValue(event.getOldValue());
        }
      });
      modeComboBoxesByAction.put(action, ruleModeComboBox);
      ruleFieldsDiv.add(
          withHelp(
              ruleModeComboBox,
              new FieldHelp(
                  sectionTitleTextSupplier,
                  () -> actionLabel(action),
                  () -> ruleDescription(action),
                  () -> modeLabel(DEFAULT_POLICY.rules().get(role).get(action)),
                  messageProperties::twoFactorRuleHelpItems,
                  messageProperties::getAdminConfigEffectiveTwoFactorRule)));
    }

    VerticalLayout ruleGroupLayout =
        new VerticalLayout(sectionHeading(sectionAppIcon, sectionTitleTextSupplier), ruleFieldsDiv);
    ruleGroupLayout.setPadding(false);
    ruleGroupLayout.setSpacing(false);
    return ruleGroupLayout;
  }

  private H3 sectionHeading(AppIcon headingAppIcon, Supplier<String> headingTextSupplier) {
    Span headingTextSpan = new Span();
    fieldTextRefreshActions.add(() -> headingTextSpan.setText(headingTextSupplier.get()));
    H3 sectionHeadingH3 = new H3();
    sectionHeadingH3.add(headingAppIcon.create(), headingTextSpan);
    sectionHeadingH3.addClassName("mfa-config-heading");
    return sectionHeadingH3;
  }

  private VerticalLayout roleGuide(RoleType role) {
    Span roleScopeSpan = new Span();
    fieldTextRefreshActions.add(() -> refreshRoleScope(roleScopeSpan, role));
    HorizontalLayout roleScopeInfoLayout =
        new HorizontalLayout(AppIcon.INFO_CIRCLE.create(), roleScopeSpan);
    roleScopeInfoLayout.setPadding(false);
    roleScopeInfoLayout.setSpacing(false);
    roleScopeInfoLayout.setAlignItems(Alignment.CENTER);
    roleScopeInfoLayout.addClassName("mfa-role-scope");

    VerticalLayout roleGuideLayout =
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
            roleScopeInfoLayout);
    roleGuideLayout.addClassName("mfa-role-guide");
    return roleGuideLayout;
  }

  private void refreshRoleScope(Span roleScopeSpan, RoleType role) {
    String rolePlaceholderText = "{role}";
    String scopeMessageText = messageProperties.getTwoFactorRoleScope(rolePlaceholderText);
    int rolePosition = scopeMessageText.indexOf(rolePlaceholderText);
    if (rolePosition < 0) {
      roleScopeSpan.setText(messageProperties.getTwoFactorRoleScope(roleLabel(role)));
      return;
    }

    Span roleNameSpan = new Span(roleLabel(role));
    roleNameSpan.addClassName("mfa-role-scope-name");
    roleScopeSpan.removeAll();
    roleScopeSpan.add(
        new Text(scopeMessageText.substring(0, rolePosition)),
        roleNameSpan,
        new Text(scopeMessageText.substring(rolePosition + rolePlaceholderText.length())));
  }

  private HorizontalLayout guideItem(
      AppIcon headingAppIcon,
      Supplier<String> guideTitleTextSupplier,
      Supplier<String> guideDescriptionTextSupplier) {
    Span guideTitleSpan = new Span();
    guideTitleSpan.addClassName("mfa-guide-item-title");
    Span guideDescriptionSpan = new Span();
    guideDescriptionSpan.addClassName(CosmosCssClasses.PANEL_DESCRIPTION);

    fieldTextRefreshActions.add(() -> {
      guideTitleSpan.setText(guideTitleTextSupplier.get());
      guideDescriptionSpan.setText(guideDescriptionTextSupplier.get());
    });

    VerticalLayout guideTextLayout = new VerticalLayout(guideTitleSpan, guideDescriptionSpan);
    guideTextLayout.setPadding(false);
    guideTextLayout.setSpacing(false);
    HorizontalLayout guideItemLayout =
        new HorizontalLayout(headingAppIcon.create(), guideTextLayout);
    guideItemLayout.addClassName("mfa-guide-item");
    return guideItemLayout;
  }

  public void activate() {
    twoFactorPolicySnapshot = twoFactorPolicies.load();
    twoFactorPolicySnapshot
        .policy()
        .rules()
        .forEach((role, actions) ->
            actions.forEach(
                (action, mode) -> ruleModeComboBoxesByRole.get(role).get(action).setValue(mode)));

    reconfirmAfterInactivitySwitch.setValue(twoFactorPolicySnapshot.policy().inactivityEnabled());
    inactivityDurationField.setStoredValue(
        Long.toString(twoFactorPolicySnapshot.policy().inactivity().toSeconds()));
    inactivityDurationField.setControlsEnabled(reconfirmAfterInactivitySwitch.getValue());

    developmentPauseSwitch.setValue(twoFactorPolicySnapshot.policy().developmentPause());
    developmentGoogleConfirmationSwitch.setValue(
        twoFactorPolicySnapshot.policy().developmentGoogleConfirmation());
  }

  public boolean hasUnsavedChanges() {
    if (twoFactorPolicySnapshot == null) {
      return false;
    }

    try {
      return !twoFactorPolicySnapshot.policy().equals(readTwoFactorPolicyDraft());
    } catch (IllegalArgumentException _) {
      return true;
    }
  }

  private void requestTwoFactorPolicyReload() {
    if (hasUnsavedChanges()) {
      UnsavedChangesDialog.confirmDiscard(messageProperties, this::reloadTwoFactorPolicy);
    } else {
      reloadTwoFactorPolicy();
    }
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

  private void requestTwoFactorPolicySave() {
    if (twoFactorPolicySnapshot == null) {
      return;
    }

    try {
      TwoFactorPolicy draftTwoFactorPolicy = readTwoFactorPolicyDraft();

      long loadedPolicyRevision = twoFactorPolicySnapshot.revision();
      if (twoFactorAuthorization.requires(
          httpServletRequest, TwoFactorAction.SECURITY_CONFIGURATION)) {
        twoFactorConfirmationDialog.open(
            TwoFactorAction.SECURITY_CONFIGURATION,
            () -> saveTwoFactorPolicy(loadedPolicyRevision, draftTwoFactorPolicy));
      } else {
        saveTwoFactorPolicy(loadedPolicyRevision, draftTwoFactorPolicy);
      }
    } catch (RuntimeException failure) {
      showTwoFactorPolicyFailure(failure);
    }
  }

  private TwoFactorPolicy readTwoFactorPolicyDraft() {
    Map<RoleType, Map<TwoFactorAction, TwoFactorMode>> draftRulesByRole =
        new EnumMap<>(RoleType.class);
    ruleModeComboBoxesByRole.forEach((role, actions) -> {
      Map<TwoFactorAction, TwoFactorMode> draftModesByAction = new EnumMap<>(TwoFactorAction.class);
      actions.forEach((action, ruleModeComboBox) ->
          draftModesByAction.put(action, ruleModeComboBox.getValue()));
      draftRulesByRole.put(role, draftModesByAction);
    });

    Duration inactivityDuration =
        inactivityDurationField.getStoredValue().isBlank()
                && !Boolean.TRUE.equals(reconfirmAfterInactivitySwitch.getValue())
            ? twoFactorPolicySnapshot.policy().inactivity()
            : Duration.ofSeconds(Long.parseLong(inactivityDurationField.getStoredValue()));

    return new TwoFactorPolicy(
        draftRulesByRole,
        reconfirmAfterInactivitySwitch.getValue(),
        inactivityDuration,
        twoFactorPolicies.developmentOptionsAvailable() && developmentPauseSwitch.getValue(),
        twoFactorPolicies.developmentOptionsAvailable()
            && developmentGoogleConfirmationSwitch.getValue());
  }

  private void saveTwoFactorPolicy(
      long loadedPolicyRevision, TwoFactorPolicy draftTwoFactorPolicy) {
    try {
      twoFactorPolicySnapshot =
          twoFactorPolicies.save(httpServletRequest, loadedPolicyRevision, draftTwoFactorPolicy);

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

  private Div withHelp(Component policyFieldComponent, FieldHelp fieldHelp) {
    ConfigInfoButton fieldInfoButton =
        new ConfigInfoButton(
            fieldHelp::tooltip,
            () -> {
              refreshFieldHelpAction = () -> renderHelp(fieldHelp);
              refreshFieldHelpAction.run();
              fieldHelpPanel.open();
            });

    Div fieldRowDiv = new Div(policyFieldComponent, fieldInfoButton);
    fieldRowDiv.addClassName("cosmos-config-input-row");
    fieldInfoButtons.add(fieldInfoButton);
    fieldTextRefreshActions.add(() ->
        fieldInfoButton.setAriaLabel(
            messageProperties.getAdminConfigHelpTitle()
                + " – "
                + fieldHelp.titleTextSupplier().get()));

    if (policyFieldComponent instanceof ComboBox<?> modeComboBox) {
      Span fieldLabelSpan = new Span();
      fieldLabelSpan.setId(policyFieldComponent.getId().orElseThrow() + "-label");
      fieldLabelSpan.addClassName("provider-field-label");
      modeComboBox.setAriaLabelledBy(fieldLabelSpan.getId().orElseThrow());
      fieldTextRefreshActions.add(
          () -> fieldLabelSpan.setText(fieldHelp.titleTextSupplier().get()));
      Div labelledFieldDiv = new Div(fieldLabelSpan, fieldRowDiv);
      labelledFieldDiv.addClassName("provider-help-field");
      return labelledFieldDiv;
    }
    return fieldRowDiv;
  }

  private void renderHelp(FieldHelp fieldHelp) {
    fieldHelpPanel.setHeading(messageProperties.getAdminConfigHelpTitle());
    fieldHelpPanel.setDescription(messageProperties.getAdminConfigHelpDescription());

    Span helpGroupSpan = new Span(fieldHelp.groupTextSupplier().get());
    helpGroupSpan.addClassName("admin-help-group");
    H3 fieldHelpTitleH3 = new H3(fieldHelp.titleTextSupplier().get());
    fieldHelpTitleH3.addClassName("admin-help-field-title");
    Span fieldHelpDescriptionSpan = new Span(fieldHelp.descriptionTextSupplier().get());
    fieldHelpDescriptionSpan.addClassName("admin-help-description");

    Div fieldDefaultValueDiv =
        new Div(
            new Span(messageProperties.getAdminConfigDefault()),
            new Span(fieldHelp.defaultValueTextSupplier().get()));
    fieldDefaultValueDiv.addClassName("admin-help-default");

    UnorderedList fieldRulesList = new UnorderedList();
    fieldHelp.ruleTextsSupplier().get().forEach(rule -> fieldRulesList.add(new ListItem(rule)));
    Div fieldRulesDiv =
        new Div(new H4(messageProperties.getAdminConfigInputRules()), fieldRulesList);
    fieldRulesDiv.addClassName("admin-help-rules");

    fieldHelpPanel.removeAll();
    fieldHelpPanel.add(
        helpGroupSpan,
        fieldHelpTitleH3,
        fieldHelpDescriptionSpan,
        fieldDefaultValueDiv,
        fieldRulesDiv);
    fieldHelpPanel.addEffectiveUse(
        messageProperties,
        List.of(
            fieldHelp.effectiveUseTextSupplier().get(),
            messageProperties.getAdminConfigEffectiveTwoFactorExisting()));
  }

  public void refreshTexts() {
    configurationBreadcrumbSpan.setText(
        messageProperties.getAdminHeaderTitle() + " / " + messageProperties.getAdminConfigTitle());
    configurationTitleSpan.setText(messageProperties.getAdminConfigTitle());
    configurationHintSpan.setText(messageProperties.getTwoFactorSettingsPageHint());
    twoFactorPolicyHeadingH2.setText(messageProperties.getTwoFactorSettingsTitle());
    twoFactorPolicyDescriptionSpan.setText(messageProperties.getTwoFactorSettingsHint());

    tabsByRole.forEach((role, tab) -> {
      tab.removeAll();
      tab.add(new Span(roleLabel(role)));
    });
    ruleModeComboBoxesByRole.forEach((role, actions) ->
        actions.forEach((action, modeField) -> modeField.setItemLabelGenerator(this::modeLabel)));
    fieldTextRefreshActions.forEach(Runnable::run);
    fieldInfoButtons.forEach(ConfigInfoButton::refreshText);

    reconfirmAfterInactivitySwitch.setLabel(messageProperties.getTwoFactorInactivityEnabled());
    inactivityDurationLabelSpan.setText(messageProperties.getTwoFactorInactivityDuration());
    inactivityDurationField.refreshTexts();

    developmentOptionsTitleSpan.setText(messageProperties.getTwoFactorDevelopmentTitle());
    developmentWarningSpan.setText(messageProperties.getTwoFactorDevelopmentWarning());
    developmentPauseSwitch.setLabel(messageProperties.getTwoFactorDevelopmentPause());
    developmentGoogleConfirmationSwitch.setLabel(messageProperties.getTwoFactorDevelopmentGoogle());

    policySaveHintSpan.setText(messageProperties.getTwoFactorSaveHint());
    reloadPolicyButton.setText(messageProperties.getAdminConfigReload());
    savePolicyButton.setText(messageProperties.getAdminConfigSave());
    closeFieldHelpButton.setText(messageProperties.getAdminUsersCloseDialog());
    fieldHelpPanel.refreshCloseText(messageProperties);
    if (refreshFieldHelpAction != null) {
      refreshFieldHelpAction.run();
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
