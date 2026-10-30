package de.derpeterson.designpilot;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.derpeterson.app.ui.components.CosmosCardComponent;
import de.derpeterson.app.ui.components.CosmosSectionHeaderComponent;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/** Nur synthetische Zustände im UI-Speicher, ohne Kontodienste, Anmeldung oder Versand. */
@Route("")
@PageTitle("Persönliche Daten · Vaadin-Pilot")
public class PersonalDataPilotView extends Div {
  private final Div navigation = new Div();
  private final Dialog emailDialog = new Dialog();
  private final Main main = new Main();
  private final CosmosCardComponent panel = new CosmosCardComponent();
  private final Button menu = iconButton("Menü öffnen", "menu");
  private final TextField firstName = textField("Vorname", "Vornamen eingeben", "Design");
  private final TextField lastName = textField("Nachname", "Nachnamen eingeben", "Stitch");

  public PersonalDataPilotView() {
    addClassName("personal-data-pilot");
    buildEmailDialog();
    Shortcuts.addShortcutListener(this, this::closeNavigation, Key.ESCAPE);
    main.addClassName("pilot-main");
    CosmosSectionHeaderComponent heading =
        new CosmosSectionHeaderComponent(
            new Span("MEIN KONTO"),
            new Span("Persönliche Daten"),
            new Span("Verwalten Sie Ihre persönlichen Angaben und Ihre bevorzugte Sprache."));
    Div columns = new Div(buildNavigation(), buildForm());
    columns.addClassName("pilot-columns");
    main.add(heading, columns);
    Footer footer = new Footer(new Span("© 2026 {ServiceName}. Alle Rechte vorbehalten."));
    footer.addClassName("pilot-footer");
    add(buildHeader(), main, footer);
  }

  private Header buildHeader() {
    Header header = new Header();
    Div inner = new Div();
    inner.addClassName("pilot-header-inner");
    Image logo = new Image("pilot/logo-k.svg", "");
    logo.addClassName("pilot-logo");
    Button brand = new Button("{ServiceName}");
    brand.setIcon(logo);
    brand.addClassName("pilot-brand");
    brand.addClickListener(event -> closeNavigation());
    menu.addClassName("pilot-menu");
    menu.getElement().setAttribute("aria-expanded", "false");
    menu.addClickListener(
        event -> {
          boolean open = !hasClassName("navigation-open");
          setClassName("navigation-open", open);
          panel.getElement().setAttribute("inert", open);
          menu.getElement().setAttribute("aria-expanded", Boolean.toString(open));
        });
    Div left = new Div(menu, brand);
    left.addClassName("pilot-header-left");
    Button bell = iconButton("Benachrichtigungen: 12 ungelesen", "bell");
    bell.addClassName("pilot-bell");
    bell.setText("12");
    bell.addClickListener(
        event -> showLocalMessage("Im Piloten ist nur Persönliche Daten umgesetzt."));
    Button settings = iconButton("E-Mail-Adresse ändern", "settings");
    settings.addClickListener(event -> emailDialog.open());
    Div identity = new Div(new Span("Design Stitch"), new Span("design@stitch.de"));
    identity.addClassName("pilot-header-identity");
    SvgIcon chevron = icon("chevron-down");
    chevron.addClassName("pilot-header-chevron");
    Div account = new Div(avatar("small"), identity, chevron);
    account.addClassName("pilot-header-account");
    Div right = new Div(bell, settings, account);
    right.addClassName("pilot-header-actions");
    inner.add(left, right);
    header.add(inner);
    header.addClassName("pilot-header");
    return header;
  }

  private Component buildNavigation() {
    navigation.addClassName("pilot-navigation");
    navigation.setId("pilot-navigation");
    menu.getElement().setAttribute("aria-controls", "pilot-navigation");
    navigation.getElement().setAttribute("aria-label", "Kontonavigation");
    Div identity = new Div(new Span("Design Stitch"), new Span("design@stitch.de"));
    identity.addClassName("pilot-identity-copy");
    Span active = new Span("● Konto aktiv");
    active.addClassName("pilot-active-badge");
    Span presence = new Span("● Verfügbar");
    presence.addClassName("pilot-presence-badge");
    Div badges = new Div(active, presence);
    badges.addClassName("pilot-badges");
    identity.add(badges);
    Div person = new Div(avatar("large"), identity);
    person.addClassName("pilot-person");
    Button editAvatar = action("Profilbild bearbeiten", "edit");
    editAvatar.addClickListener(
        event ->
            showLocalMessage(
                "Der Pilot enthält den E-Mail-Dialog. Profilbildbearbeitung folgt später."));
    Div card = new Div(person, editAvatar);
    card.addClassName("pilot-identity-card");
    navigation.add(
        card,
        navSection(
            "PORTAL",
            navItem("Startseite", "home", false),
            navItem("Benachrichtigungen", "bell", false)),
        navSection(
            "MEIN KONTO",
            navItem("Persönliche Daten", "user", true),
            navItem("Sicherheit", "lock", false),
            navItem("Geräte & Sitzungen", "devices", false)));
    Div shade = new Div();
    shade.addClassName("pilot-navigation-shade");
    shade.addClickListener(event -> closeNavigation());
    add(shade);
    return navigation;
  }

  private Component buildForm() {
    panel.addClassName("pilot-panel");
    panel.add(
        panelHeading(
            "Persönliche Daten", "Ihre Kontoinformationen und Grundeinstellungen", "user"));
    Div fields = new Div();
    fields.addClassName("pilot-fields");
    Div names = new Div(firstName, lastName);
    names.addClassName("pilot-form-row");
    fields.add(names);
    EmailField email = new EmailField("E-Mail-Adresse");
    email.setValue("design@stitch.de");
    email.setReadOnly(true);
    email.setSuffixComponent(icon("lock"));
    email.addClassName("pilot-wide");
    Div emailGroup = new Div(email);
    emailGroup.addClassNames("pilot-email-group", "pilot-wide");
    Div readOnly = new Div(icon("lock"), new Span("Schreibgeschützt"));
    readOnly.addClassName("pilot-readonly-label");
    emailGroup.add(readOnly);
    fields.add(emailGroup);
    ComboBox<String> gender = new ComboBox<>("Geschlecht");
    gender.setItems("Männlich", "Weiblich", "Divers");
    gender.setValue("Männlich");
    gender.setRequiredIndicatorVisible(true);
    gender.setPlaceholder("Geschlecht auswählen");
    gender.setAllowCustomValue(false);
    styleChoices(gender);

    DatePicker birthDate = new DatePicker("Geburtsdatum");
    birthDate.setLocale(Locale.GERMANY);
    birthDate.setI18n(
        new DatePicker.DatePickerI18n()
            .setMonthNames(
                List.of(
                    "Januar",
                    "Februar",
                    "März",
                    "April",
                    "Mai",
                    "Juni",
                    "Juli",
                    "August",
                    "September",
                    "Oktober",
                    "November",
                    "Dezember"))
            .setWeekdays(
                List.of(
                    "Sonntag",
                    "Montag",
                    "Dienstag",
                    "Mittwoch",
                    "Donnerstag",
                    "Freitag",
                    "Samstag"))
            .setWeekdaysShort(List.of("So", "Mo", "Di", "Mi", "Do", "Fr", "Sa"))
            .setFirstDayOfWeek(1)
            .setToday("Heute")
            .setCancel("Abbrechen")
            .setDateFormat("dd.MM.yyyy"));
    birthDate.setValue(LocalDate.of(2006, 10, 23));
    birthDate.setRequiredIndicatorVisible(true);
    birthDate.setPlaceholder("z. B. 31.12.1990");
    birthDate.setHelperText("Format: TT.MM.JJJJ");

    ComboBox<String> language = new ComboBox<>("Bevorzugte Sprache");
    language.setItems("Deutsch", "English");
    language.setValue("Deutsch");
    language.setHelperText("Voreinstellung für Ihre Benutzeroberfläche.");
    styleChoices(language);

    Div preferences = new Div(gender, birthDate, language);
    preferences.addClassNames("pilot-form-row", "pilot-preferences");
    fields.add(preferences);
    Button save = action("Änderungen speichern", "check");
    save.addClassName("primary");
    save.addClickListener(
        event -> {
          boolean valid =
              !firstName.isEmpty()
                  && !lastName.isEmpty()
                  && !gender.isEmpty()
                  && !birthDate.isEmpty();
          firstName.setInvalid(firstName.isEmpty());
          lastName.setInvalid(lastName.isEmpty());
          gender.setInvalid(gender.isEmpty());
          birthDate.setInvalid(birthDate.isEmpty());
          showLocalMessage(
              valid
                  ? "Beispieldaten im Piloten geprüft. Es wurde nichts gespeichert."
                  : "Bitte füllen Sie die Pflichtfelder aus.");
        });
    Div actions = new Div(save);
    actions.addClassName("pilot-panel-actions");
    panel.add(fields, actions);
    return panel;
  }

  private void buildEmailDialog() {
    emailDialog.addClassName("pilot-email-dialog");
    emailDialog.setWidth("580px");
    emailDialog.setCloseOnEsc(true);
    emailDialog.setCloseOnOutsideClick(true);
    Div title = panelHeading("E-Mail-Adresse ändern", "Neue Adresse bestätigen", "mail");
    Button close = iconButton("Dialog schließen", "close-circle");
    close.addClickListener(event -> emailDialog.close());
    title.add(close);
    emailDialog.getHeader().add(title);
    EmailField newEmail = new EmailField("Neue E-Mail-Adresse");
    newEmail.setPlaceholder("E-Mail-Adresse eingeben");
    EmailField confirmEmail = new EmailField("Neue E-Mail-Adresse bestätigen");
    confirmEmail.setPlaceholder("E-Mail-Adresse erneut eingeben");
    PasswordField password = new PasswordField("Aktuelles Passwort");
    password.setPlaceholder("Passwort eingeben");
    for (Component field : List.of(newEmail, confirmEmail, password)) {
      field.getElement().setAttribute("required", true);
    }
    newEmail.setRequiredIndicatorVisible(true);
    confirmEmail.setRequiredIndicatorVisible(true);
    password.setRequiredIndicatorVisible(true);
    Div content = new Div(newEmail, confirmEmail, password);
    content.addClassName("pilot-dialog-fields");
    Div info =
        new Div(
            icon("info"),
            new Span(
                "Ihre bisherige E-Mail-Adresse bleibt bis zur Bestätigung gültig. Danach werden alle Sitzungen und gespeicherten Anmeldungen beendet; melden Sie sich mit der neuen Adresse an."));
    info.addClassName("pilot-info");
    content.add(info);
    emailDialog.add(content);
    Button cancel = action("Abbrechen", "close");
    cancel.addClickListener(event -> emailDialog.close());
    Button confirm = action("Bestätigung anfordern", "check");
    confirm.addClassName("primary");
    confirm.addClickListener(
        event -> {
          boolean emailInvalid = newEmail.isEmpty() || newEmail.isInvalid();
          boolean confirmationInvalid =
              !newEmail.getValue().equals(confirmEmail.getValue()) || confirmEmail.isEmpty();
          newEmail.setInvalid(emailInvalid);
          confirmEmail.setInvalid(confirmationInvalid);
          password.setInvalid(password.isEmpty());
          if (!emailInvalid && !confirmationInvalid && !password.isEmpty()) {
            emailDialog.close();
            showLocalMessage("Vorschau geprüft. Es wurde keine Bestätigung versendet.");
          }
        });
    emailDialog.getFooter().add(cancel, confirm);
    emailDialog.addOpenedChangeListener(
        event -> {
          if (event.isOpened()) {
            newEmail.clear();
            confirmEmail.clear();
            password.clear();
            newEmail.setInvalid(false);
            confirmEmail.setInvalid(false);
            password.setInvalid(false);
            newEmail.focus();
          }
        });
  }

  private Div panelHeading(String title, String description, String symbol) {
    Div icon = new Div(icon(symbol));
    icon.addClassName("pilot-panel-icon");
    Div copy = new Div(new H2(title), new Paragraph(description));
    copy.addClassName("pilot-panel-copy");
    Div heading = new Div(icon, copy);
    heading.addClassName("pilot-panel-heading");
    return heading;
  }

  private Button navItem(String text, String symbol, boolean selected) {
    Button item = action(text, symbol);
    item.addClassName("pilot-nav-item");
    if ("bell".equals(symbol)) {
      Span count = new Span("12");
      count.addClassName("pilot-nav-count");
      item.getElement().appendChild(count.getElement());
    }
    item.setClassName("selected", selected);
    if (selected) {
      item.getElement().setAttribute("aria-current", "page");
    }
    item.addClickListener(
        event -> {
          closeNavigation();
          if (!selected) {
            showLocalMessage("Im Piloten ist nur Persönliche Daten umgesetzt.");
          }
        });
    return item;
  }

  private Span navGroup(String title) {
    Span group = new Span(title);
    group.addClassName("pilot-nav-group");
    return group;
  }

  private Div navSection(String title, Button... items) {
    Div list = new Div(items);
    list.addClassName("pilot-nav-list");
    Div section = new Div(navGroup(title), list);
    section.addClassName("pilot-nav-section");
    return section;
  }

  private Div avatar(String size) {
    Div avatar = new Div(new Span("DS"));
    avatar.addClassNames("pilot-avatar", size);
    return avatar;
  }

  private static void styleChoices(ComboBox<String> field) {
    field.addClassName("pilot-dotted-select");
    field.setClassNameGenerator(value -> "pilot-select-option");
    field.setRenderer(
        new ComponentRenderer<>(
            value -> {
              Span dot = new Span();
              dot.addClassName("pilot-option-dot");
              SvgIcon check = icon("check");
              check.addClassName("pilot-option-check");
              Div row = new Div(dot, new Span(value), check);
              row.addClassName("pilot-option-row");
              return row;
            }));
  }

  private void closeNavigation() {
    boolean wasOpen = hasClassName("navigation-open");
    removeClassName("navigation-open");
    panel.getElement().setAttribute("inert", false);
    menu.getElement().setAttribute("aria-expanded", "false");
    if (wasOpen) {
      menu.focus();
    }
  }

  private static TextField textField(String label, String placeholder, String value) {
    TextField field = new TextField(label);
    field.setPlaceholder(placeholder);
    field.setValue(value);
    field.setRequiredIndicatorVisible(true);
    return field;
  }

  private static Button action(String text, String symbol) {
    Button button = new Button(text, icon(symbol));
    button.addClassName("pilot-button");
    return button;
  }

  private static Button iconButton(String label, String symbol) {
    SvgIcon icon = icon(symbol);
    Button button = new Button();
    button.setIcon(icon);
    button.setAriaLabel(label);
    button.addClassNames("pilot-button", "pilot-icon-button");
    return button;
  }

  private static SvgIcon icon(String symbol) {
    return new SvgIcon("pilot/icons.svg", symbol);
  }

  private static void showLocalMessage(String text) {
    Notification notification = Notification.show(text, 4000, Notification.Position.TOP_CENTER);
    notification.addClassName("pilot-notification");
  }
}
