package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import de.derpeterson.app.config.AppConstants;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.UserEntity;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/** Erste Gestaltungsstufe: lokale Formulareingaben und Tabwechsel, keine Kontoänderungen. */
public class AccountSettingsComponent extends Div {
  private final transient MessageProperties messages;
  private final List<Runnable> textUpdates = new ArrayList<>();
  private final Map<Tab, AccountPanelComponent> panels = new LinkedHashMap<>();
  private final Tabs tabs = new Tabs();
  private final TextField firstName = new TextField();
  private final TextField lastName = new TextField();
  private final EmailField email = new EmailField();
  private final DatePicker birthDate = new DatePicker();
  private final ComboBox<Locale> preferredLanguage = new ComboBox<>();

  public AccountSettingsComponent(MessageProperties messages, UserEntity user) {
    this.messages = messages;
    addClassName("account-settings");
    var breadcrumb = text(new Span(), messages::getAccountSettingsOverview);
    var title = text(new Span(), messages::getAccountSettingsTitle);
    var description = text(new Span(), messages::getAccountSettingsDescription);
    add(new CosmosSectionHeaderComponent(breadcrumb, title, description), summary(user));

    tabs.setId("account-tabs");
    tabs.addClassName("account-tabs");
    textUpdates.add(
        () -> tabs.getElement().setAttribute("aria-label", messages.getAccountSettingsTitle()));
    var profile = profile(user);
    addTab("profile", VaadinIcon.USER, messages::getAccountSettingsProfileTab, profile);
    addTab("security", VaadinIcon.LOCK, messages::getAccountSettingsSecurityTab, security(user));
    addTab("sessions", VaadinIcon.DESKTOP, messages::getAccountSettingsSessionsTab, sessions());
    tabs.addSelectedChangeListener(event -> showSelectedPanel());
    var content = new Div(panels.values().toArray(AccountPanelComponent[]::new));
    content.addClassName("account-panels");
    add(tabs, content);
    refreshTexts();
    showSelectedPanel();
  }

  private <T extends HasText> T text(T component, Supplier<String> value) {
    textUpdates.add(() -> component.setText(value.get()));
    return component;
  }

  private Button previewButton(VaadinIcon icon, Supplier<String> label, boolean primary) {
    var button = text(new Button(), label);
    button.setPrefixComponent(icon.create());
    if (primary) button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    // Für die Gestaltungsabnahme aktiv, aber ohne Aktionslistener oder Serviceaufruf.
    // Diese erste Stufe darf weiterhin keine Kontodaten oder Sitzungen ändern.
    return button;
  }

  private CosmosCardComponent summary(UserEntity user) {
    var avatar = new Image(AppConstants.USER_ICON_PATH, AppConstants.USER_ICON_ALT);
    avatar.addClassName("account-summary-avatar");
    var name =
        new Span(
            user == null
                ? ""
                : (Objects.toString(user.getFirstName(), "")
                        + " "
                        + Objects.toString(user.getLastName(), ""))
                    .trim());
    name.addClassName("account-summary-name");
    var address = new Span(user == null ? "" : Objects.toString(user.getEmail(), ""));
    address.addClassName("account-summary-email");
    var active =
        text(
            new Span(),
            user != null && user.isEnabled()
                ? messages::getAccountSettingsEnabled
                : messages::getAccountSettingsDisabled);
    active.addClassName("account-summary-status");
    active.getElement().getThemeList().add("badge");
    active.getElement().getThemeList().add(user != null && user.isEnabled() ? "success" : "error");
    var info = new Div(name, address, active);
    info.addClassName("account-summary-details");
    var edit = previewButton(VaadinIcon.EDIT, messages::getAccountSettingsEditPicture, false);
    var row = new Div(avatar, info, edit);
    row.addClassName("account-summary-row");
    var card = new CosmosCardComponent(row);
    card.addClassName("account-summary");
    return card;
  }

  private AccountPanelComponent panel(
      String id,
      VaadinIcon icon,
      Supplier<String> title,
      Supplier<String> description,
      Supplier<String> meta) {
    var panel = new AccountPanelComponent("account-panel-" + id, icon);
    textUpdates.add(() -> panel.setHeading(title.get(), description.get(), meta.get()));
    return panel;
  }

  private void addTab(
      String id, VaadinIcon icon, Supplier<String> label, AccountPanelComponent panel) {
    var symbol = icon.create();
    symbol.getElement().setAttribute("aria-hidden", "true");
    var tab = new Tab(symbol, text(new Span(), label));
    tab.setId("account-tab-" + id);
    tab.getElement().setAttribute("aria-controls", "account-panel-" + id);
    panel.getElement().setAttribute("role", "tabpanel");
    panel.getElement().setAttribute("aria-labelledby", "account-tab-" + id);
    panels.put(tab, panel);
    tabs.add(tab);
  }

  private void showSelectedPanel() {
    panels.forEach((tab, panel) -> panel.setVisible(tab == tabs.getSelectedTab()));
  }

  private AccountPanelComponent profile(UserEntity user) {
    var panel =
        panel(
            "profile",
            VaadinIcon.USER,
            messages::getAccountSettingsProfileTitle,
            messages::getAccountSettingsProfileDescription,
            messages::getAccountSettingsProfileMeta);
    firstName.setId("account-first-name");
    lastName.setId("account-last-name");
    email.setId("account-email");
    birthDate.setId("account-birth-date");
    preferredLanguage.setId("account-language");
    firstName.setRequiredIndicatorVisible(true);
    lastName.setRequiredIndicatorVisible(true);
    birthDate.setRequiredIndicatorVisible(true);
    email.setReadOnly(true);
    preferredLanguage.setItems(Locale.GERMAN, Locale.ENGLISH);
    preferredLanguage.setItemLabelGenerator(language -> language.getDisplayLanguage(language));
    preferredLanguage.setAllowCustomValue(false);
    birthDate.setAutoOpen(false);
    if (user != null) {
      firstName.setValue(Objects.toString(user.getFirstName(), ""));
      lastName.setValue(Objects.toString(user.getLastName(), ""));
      email.setValue(Objects.toString(user.getEmail(), ""));
      birthDate.setValue(user.getBirthDate());
      if (user.getPreferredLocale() != null) {
        preferredLanguage.setValue(
            Locale.GERMAN.getLanguage().equals(user.getPreferredLocale().getLanguage())
                ? Locale.GERMAN
                : Locale.ENGLISH);
      }
    }
    textUpdates.add(
        () -> {
          firstName.setLabel(messages.getRegistrationFirstNameField());
          firstName.setPlaceholder(messages.getAccountFirstNamePlaceholder());
          lastName.setLabel(messages.getRegistrationLastNameField());
          lastName.setPlaceholder(messages.getAccountLastNamePlaceholder());
          email.setLabel(messages.getRegistrationEmailField());
          email.setPlaceholder(messages.getAccountEmailPlaceholder());
          email.setHelperText(messages.getAccountSettingsEmailHint());
          birthDate.setLabel(messages.getRegistrationBirthDateField());
          birthDate.setLocale(CustomI18NProvider.getCurrentLocale());
          birthDate.setPlaceholder(messages.getRegistrationBirthDatePlaceholder());
          preferredLanguage.setLabel(messages.getAccountSettingsLanguage());
          preferredLanguage.setHelperText(messages.getAccountSettingsLanguageHint());
        });
    var emailAction =
        previewButton(VaadinIcon.EDIT, messages::getAccountSettingsChangeEmail, false);
    var emailRow = new Div(email, emailAction);
    emailRow.addClassName("account-email-row");
    var form = new Div(firstName, lastName, emailRow, birthDate, preferredLanguage);
    form.addClassName("account-profile-form");
    panel.getBody().add(form);
    panel.addFooter(
        text(new Span(), messages::getAccountSettingsAccountHint),
        previewButton(VaadinIcon.CHECK, messages::getAccountSettingsSave, true));
    return panel;
  }

  private Div actionRow(
      VaadinIcon icon, Supplier<String> title, Supplier<String> description, Button action) {
    var symbol = new Div(icon.create());
    symbol.addClassName("cosmos-panel-icon");
    var heading = text(new Span(), title);
    heading.addClassName("account-entry-title");
    var detail = text(new Span(), description);
    detail.addClassName("account-entry-description");
    var copy = new Div(heading, detail);
    copy.addClassName("account-entry-copy");
    var row = new Div(symbol, copy, action);
    row.addClassName("account-entry");
    return row;
  }

  private AccountPanelComponent security(UserEntity user) {
    var panel =
        panel(
            "security",
            VaadinIcon.LOCK,
            messages::getAccountSettingsSecurityTitle,
            messages::getAccountSettingsSecurityDescription,
            messages::getAccountSettingsSecurityTab);
    panel
        .getBody()
        .add(
            actionRow(
                VaadinIcon.LOCK,
                messages::getRegistrationPasswordField,
                messages::getAccountSettingsPasswordHint,
                previewButton(VaadinIcon.EDIT, messages::getAccountSettingsChangePassword, false)),
            actionRow(
                VaadinIcon.ENVELOPE,
                messages::getRegistrationEmailField,
                () -> user == null ? "" : Objects.toString(user.getEmail(), ""),
                previewButton(VaadinIcon.EDIT, messages::getAccountSettingsChangeEmail, false)));
    var notice = text(new Span(), messages::getAccountSettingsSecurityNotice);
    notice.addClassName("account-info-text");
    panel.getBody().add(notice);
    panel.addFooter(text(new Span(), messages::getAccountSettingsAccountHint), new Span());
    return panel;
  }

  private AccountPanelComponent sessions() {
    var panel =
        panel(
            "sessions",
            VaadinIcon.DESKTOP,
            messages::getAccountSettingsSessionsTitle,
            messages::getAccountSettingsSessionsDescription,
            messages::getAccountSettingsSessionsMeta);
    var count = text(new Span(), messages::getAccountSettingsSessionsCount);
    count.addClassName("account-entry-title");
    var demo = text(new Span(), messages::getAccountSettingsSessionsDemo);
    demo.addClassName("account-entry-description");
    var limit = text(new Span(), messages::getAccountSettingsSessionsLimit);
    limit.addClassName("cosmos-admin-role-chip");
    var heading = new Div(new Div(count, demo), limit);
    heading.addClassName("account-session-heading");
    var current =
        actionRow(
            VaadinIcon.DESKTOP,
            () -> "Windows-PC · Chrome",
            messages::getAccountSettingsActiveNow,
            previewButton(VaadinIcon.DESKTOP, messages::getAccountSettingsCurrentSession, false));
    var currentBadge = text(new Span(), messages::getAccountSettingsCurrentDevice);
    currentBadge.getElement().getThemeList().add("badge");
    currentBadge.getElement().getThemeList().add("success");
    current
        .getChildren()
        .skip(1)
        .findFirst()
        .orElseThrow()
        .getElement()
        .appendChild(currentBadge.getElement());
    panel
        .getBody()
        .add(
            heading,
            current,
            actionRow(
                VaadinIcon.MOBILE,
                () -> "Android · Chrome",
                messages::getAccountSettingsActiveHours,
                previewButton(VaadinIcon.SIGN_OUT, messages::getBaseLogoutButton, false)),
            actionRow(
                VaadinIcon.DESKTOP,
                () -> "Laptop · Edge",
                messages::getAccountSettingsActiveYesterday,
                previewButton(VaadinIcon.SIGN_OUT, messages::getBaseLogoutButton, false)));
    var notice = text(new Span(), messages::getAccountSettingsSessionsHint);
    notice.addClassName("account-info-text");
    panel.getBody().add(notice);
    var logout =
        previewButton(VaadinIcon.SIGN_OUT, messages::getAccountSettingsLogoutOthers, false);
    logout.addClassName("cosmos-delete-button");
    panel.addFooter(text(new Span(), messages::getAccountSettingsLogoutHint), logout);
    return panel;
  }

  public void refreshTexts() {
    textUpdates.forEach(Runnable::run);
  }
}
