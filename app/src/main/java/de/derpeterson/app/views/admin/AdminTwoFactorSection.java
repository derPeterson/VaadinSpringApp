package de.derpeterson.app.views.admin;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Switch;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
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
@StyleSheet("custom-theme/cosmos-provider-settings.css")
public final class AdminTwoFactorSection extends VerticalLayout {
  private final transient MessageProperties texts;
  private final transient TwoFactorPolicies policies;
  private final transient TwoFactorAuthorization authorization;
  private final transient HttpServletRequest request;
  private final Map<RoleType, Map<TwoFactorAction, Select<TwoFactorMode>>> rules =
      new EnumMap<>(RoleType.class);
  private final Map<RoleType, Tab> roleTabs = new EnumMap<>(RoleType.class);
  private final List<Runnable> fieldTextUpdates = new ArrayList<>();
  private final List<ConfigInfoButton> infoButtons = new ArrayList<>();
  private final Span breadcrumb = new Span();
  private final Span pageTitle = new Span();
  private final Span pageHint = new Span();
  private final H2 title = new H2();
  private final Span hint = new Span();
  private final Span rulesHint = new Span();
  private final Span saveHint = new Span();
  private final Switch inactivity = new Switch();
  private final Span durationLabel = new Span();
  private final AdminDurationField duration;
  private final Switch developmentPause = new Switch();
  private final Switch developmentGoogle = new Switch();
  private final Span developmentTitle = new Span();
  private final Span developmentWarning = new Span();
  private final Button reload = new Button();
  private final Button save = new Button();
  private final AdminSidePanel help;
  private final Button closeHelp = new Button();
  private final TwoFactorActionDialog confirmation;
  private TwoFactorPolicies.Snapshot snapshot;
  private Runnable refreshHelp = () -> {};

  public AdminTwoFactorSection(
      MessageProperties texts,
      TwoFactorPolicies policies,
      TwoFactorAuthorization authorization,
      HttpServletRequest request) {
    this.texts = texts;
    this.policies = policies;
    this.authorization = authorization;
    this.request = request;
    setWidthFull();
    setPadding(false);
    setSpacing(false);
    addClassNames("cosmos-admin-section", "cosmos-provider-settings", "cosmos-mfa-settings");
    setId("admin-two-factor-settings");
    add(new CosmosSectionHeaderComponent(breadcrumb, pageTitle, pageHint));
    confirmation = new TwoFactorActionDialog(texts, authorization, request);
    help = new AdminSidePanel(texts, AppIcon.INFO_CIRCLE);
    help.addClassName("admin-config-help");
    help.setId("two-factor-field-help");
    closeHelp.setWidthFull();
    closeHelp.setPrefixComponent(AppIcon.CLOSE.create());
    closeHelp.addClickListener(event -> help.close());
    help.getFooter().add(closeHelp);
    title.addClassName(CosmosClasses.PANEL_TITLE);
    hint.addClassName(CosmosClasses.PANEL_DESCRIPTION);
    Div heading = new Div(title, hint);
    heading.addClassName(CosmosClasses.PANEL_HEADING);
    Div icon = new Div(AppIcon.LOCK.create());
    icon.addClassName(CosmosClasses.PANEL_ICON);
    Div header = new Div(icon, heading);
    header.addClassName(CosmosClasses.PANEL_HEADER);
    TabSheet tabs = new TabSheet();
    tabs.setId("two-factor-role-tabs");
    tabs.setWidthFull();
    tabs.addThemeVariants(TabSheetVariant.NO_PADDING);
    for (RoleType role : RoleType.values()) {
      Map<TwoFactorAction, Select<TwoFactorMode>> selections = new EnumMap<>(TwoFactorAction.class);
      Div fields = new Div();
      fields.addClassName("mfa-policy-grid");
      for (TwoFactorAction action : TwoFactorAction.values()) {
        Select<TwoFactorMode> select = new Select<>();
        select.setId(
            "two-factor-rule-"
                + role.name().toLowerCase(Locale.ROOT)
                + "-"
                + action.name().toLowerCase(Locale.ROOT));
        select.setWidthFull();
        select.setItems(TwoFactorMode.values());
        select.setEmptySelectionAllowed(false);
        select.setItemLabelGenerator(this::modeLabel);
        selections.put(action, select);
        fields.add(withHelp(select, () -> actionLabel(action), texts::getTwoFactorRuleHelp));
      }
      rules.put(role, selections);
      Tab tab = new Tab();
      roleTabs.put(role, tab);
      tabs.add(tab, fields);
    }
    rulesHint.addClassName(CosmosClasses.PANEL_DESCRIPTION);
    rulesHint.addClassName("mfa-rules-hint");
    inactivity.setId("two-factor-inactivity-enabled");
    inactivity.addClassName("cosmos-config-switch");
    inactivity.setWidthFull();
    duration = new AdminDurationField(texts);
    duration.setAvailableUnits(
        AdminDurationField.Unit.MINUTES,
        AdminDurationField.Unit.HOURS,
        AdminDurationField.Unit.DAYS);
    duration.setId("two-factor-inactivity-duration");
    duration.setDefaultValue("43200");
    durationLabel.setId("two-factor-inactivity-label");
    durationLabel.addClassName("provider-field-label");
    duration.setAriaLabelledBy("two-factor-inactivity-label");
    Div durationField =
        new Div(
            durationLabel,
            withHelp(
                duration,
                texts::getTwoFactorInactivityDuration,
                texts::getTwoFactorInactivityHelp));
    Div common =
        new Div(
            withHelp(
                inactivity,
                texts::getTwoFactorInactivityEnabled,
                texts::getTwoFactorInactivityHelp),
            durationField);
    common.addClassName("mfa-policy-grid");
    inactivity.addValueChangeListener(event -> duration.setControlsEnabled(event.getValue()));
    developmentPause.addClassName("cosmos-config-switch");
    developmentPause.setWidthFull();
    developmentGoogle.addClassName("cosmos-config-switch");
    developmentGoogle.setWidthFull();
    developmentPause.setId("two-factor-development-pause");
    developmentGoogle.setId("two-factor-development-google");
    developmentTitle.addClassName("provider-kicker");
    developmentWarning.addClassName(CosmosClasses.PANEL_DESCRIPTION);
    Div development =
        new Div(developmentTitle, developmentWarning, developmentPause, developmentGoogle);
    development.addClassName("mfa-development-options");
    development.setVisible(policies.developmentOptionsAvailable());
    Div body = new Div(rulesHint, tabs, common, development);
    body.addClassName("mfa-settings-body");
    Div info = new Div(AppIcon.INFO_CIRCLE.create(), saveHint);
    info.addClassName("cosmos-config-save-hint");
    reload.setId("two-factor-reload");
    reload.setPrefixComponent(AppIcon.REFRESH.create());
    reload.addClickListener(event -> reload());
    save.setId("two-factor-save");
    save.setPrefixComponent(AppIcon.CHECK.create());
    save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    save.addClickListener(event -> saveRequested());
    Div actions = new Div(reload, save);
    actions.addClassName("cosmos-config-card-actions");
    EqualButtonWidths.configure(actions);
    Div footer = new Div(info, actions);
    footer.addClassName("cosmos-panel-footer");
    CosmosCardComponent card = new CosmosCardComponent(header, body, footer);
    card.addClassName(CosmosClasses.PANEL_CARD);
    add(card, help, confirmation);
    refreshTexts();
  }

  public void activate() {
    snapshot = policies.load();
    snapshot
        .policy()
        .rules()
        .forEach(
            (role, actions) ->
                actions.forEach((action, mode) -> rules.get(role).get(action).setValue(mode)));
    inactivity.setValue(snapshot.policy().inactivityEnabled());
    duration.setStoredValue(Long.toString(snapshot.policy().inactivity().toSeconds()));
    duration.setControlsEnabled(inactivity.getValue());
    developmentPause.setValue(snapshot.policy().developmentPause());
    developmentGoogle.setValue(snapshot.policy().developmentGoogleConfirmation());
  }

  private void reload() {
    try {
      activate();
      NotificationHelper.getInstance()
          .showNotification(
              texts::getAdminConfigReloadSuccess, NotificationHelper.NotificationType.SUCCESS);
    } catch (RuntimeException failure) {
      failure(failure);
    }
  }

  private void saveRequested() {
    if (snapshot == null) {
      return;
    }
    try {
      Map<RoleType, Map<TwoFactorAction, TwoFactorMode>> desiredRules =
          new EnumMap<>(RoleType.class);
      rules.forEach(
          (role, actions) -> {
            Map<TwoFactorAction, TwoFactorMode> modes = new EnumMap<>(TwoFactorAction.class);
            actions.forEach((action, select) -> modes.put(action, select.getValue()));
            desiredRules.put(role, modes);
          });
      Duration period =
          duration.getStoredValue().isBlank() && !inactivity.getValue()
              ? snapshot.policy().inactivity()
              : Duration.ofSeconds(Long.parseLong(duration.getStoredValue()));
      TwoFactorPolicy desired =
          new TwoFactorPolicy(
              desiredRules,
              inactivity.getValue(),
              period,
              policies.developmentOptionsAvailable() && developmentPause.getValue(),
              policies.developmentOptionsAvailable() && developmentGoogle.getValue());
      long revision = snapshot.revision();
      if (authorization.requires(request, TwoFactorAction.SECURITY_CONFIGURATION)) {
        confirmation.open(TwoFactorAction.SECURITY_CONFIGURATION, () -> save(revision, desired));
      } else {
        save(revision, desired);
      }
    } catch (RuntimeException failure) {
      failure(failure);
    }
  }

  private void save(long revision, TwoFactorPolicy desired) {
    try {
      snapshot = policies.save(request, revision, desired);
      NotificationHelper.getInstance()
          .showNotification(texts::getTwoFactorSaved, NotificationHelper.NotificationType.SUCCESS);
    } catch (RuntimeException failure) {
      failure(failure);
    }
  }

  private void failure(RuntimeException failure) {
    NotificationHelper.getInstance()
        .showNotification(
            texts::getBaseFailedTitle,
            failure instanceof OptimisticLockingFailureException
                ? texts::getTwoFactorConflict
                : texts::getTwoFactorFailed,
            NotificationHelper.NotificationType.ERROR);
  }

  private Div withHelp(Component field, Supplier<String> label, Supplier<String> explanation) {
    ConfigInfoButton button =
        new ConfigInfoButton(
            explanation,
            () -> {
              refreshHelp =
                  () -> {
                    help.setHeading(texts.getAdminConfigHelpTitle());
                    help.setDescription(label.get());
                    help.removeAll();
                    Span copy = new Span(explanation.get());
                    copy.addClassName(CosmosClasses.PANEL_DESCRIPTION);
                    help.add(copy);
                  };
              refreshHelp.run();
              help.open();
            });
    Div row = new Div(field, button);
    row.addClassName("cosmos-config-input-row");
    infoButtons.add(button);
    if (field instanceof Select<?> select) {
      Span caption = new Span();
      caption.setId(field.getId().orElseThrow() + "-label");
      caption.addClassName("provider-field-label");
      select.setAriaLabelledBy(caption.getId().orElseThrow());
      fieldTextUpdates.add(() -> caption.setText(label.get()));
      Div wrapper = new Div(caption, row);
      wrapper.addClassName("provider-help-field");
      return wrapper;
    }
    return row;
  }

  public void refreshTexts() {
    breadcrumb.setText(texts.getAdminHeaderTitle() + " / " + texts.getAdminConfigTitle());
    pageTitle.setText(texts.getAdminConfigTitle());
    pageHint.setText(texts.getTwoFactorSettingsHint());
    title.setText(texts.getTwoFactorSettingsTitle());
    hint.setText(texts.getTwoFactorSettingsHint());
    rulesHint.setText(texts.getTwoFactorRulesHint());
    roleTabs.forEach(
        (role, tab) -> {
          tab.removeAll();
          tab.add(new Span(roleLabel(role)));
        });
    rules.forEach(
        (role, actions) ->
            actions.forEach((action, select) -> select.setItemLabelGenerator(this::modeLabel)));
    fieldTextUpdates.forEach(Runnable::run);
    infoButtons.forEach(ConfigInfoButton::refreshText);
    inactivity.setLabel(texts.getTwoFactorInactivityEnabled());
    durationLabel.setText(texts.getTwoFactorInactivityDuration());
    duration.refreshTexts();
    developmentTitle.setText(texts.getTwoFactorDevelopmentTitle());
    developmentWarning.setText(texts.getTwoFactorDevelopmentWarning());
    developmentPause.setLabel(texts.getTwoFactorDevelopmentPause());
    developmentGoogle.setLabel(texts.getTwoFactorDevelopmentGoogle());
    saveHint.setText(texts.getTwoFactorSaveHint());
    reload.setText(texts.getAdminConfigReload());
    save.setText(texts.getAdminConfigSave());
    closeHelp.setText(texts.getAdminUsersCloseDialog());
    help.refreshCloseText(texts);
    refreshHelp.run();
    confirmation.refreshTexts();
  }

  private String modeLabel(TwoFactorMode mode) {
    return switch (mode) {
      case OFF -> texts.getTwoFactorModeOff();
      case IF_ENROLLED -> texts.getTwoFactorModeEnrolled();
      case REQUIRED -> texts.getTwoFactorModeRequired();
    };
  }

  private String roleLabel(RoleType role) {
    return switch (role) {
      case ROLE_SUPERADMIN -> texts.getRoleSuperadmin();
      case ROLE_ADMIN -> texts.getRoleAdmin();
      case ROLE_USER -> texts.getRoleUser();
      case ROLE_GUEST -> texts.getRoleGuest();
    };
  }

  private String actionLabel(TwoFactorAction action) {
    return ConfigurationCatalog.actionLabel(action, texts);
  }
}
