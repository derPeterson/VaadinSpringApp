package de.derpeterson.app.designstudy;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import de.derpeterson.app.helper.ui.CompactDateInput;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.Gender;
import de.derpeterson.app.ui.components.CosmosBackgroundComponent;
import de.derpeterson.app.ui.components.CosmosCardComponent;
import de.derpeterson.app.ui.components.CosmosSectionHeaderComponent;
import java.util.Locale;

/**
 * Isolierte Layoutstudie ohne Route, Spring-Registrierung oder fachliche Aktionen. Die Texte werden
 * beim Erzeugen aus dem bestehenden Katalog übernommen.
 */
public final class VisitorLayoutStudy extends Div {
  public enum Scene {
    LOGIN,
    REGISTRATION
  }

  public VisitorLayoutStudy(
      MessageProperties messages, String serviceName, Component serviceLogo, Scene scene) {
    addClassNames("cosmos-page", "cosmos-theme", "visitor-layout-study");

    Div brand = new Div(serviceLogo, new Span(serviceName));
    brand.addClassName("study-brand");
    Div navigation =
        new Div(
            new Button(messages.getBaseLoginButton(), VaadinIcon.SIGN_IN.create()),
            new Button(messages.getBaseRegistrationButton(), VaadinIcon.USER_CHECK.create()));
    navigation.addClassName("study-navigation");
    Div header = new Div(brand, navigation);
    header.addClassName("study-header");

    boolean registration = scene == Scene.REGISTRATION;
    String title = registration ? messages.getRegistrationTitle() : messages.getLoginTitle();
    CosmosSectionHeaderComponent introduction =
        new CosmosSectionHeaderComponent(
            new Span(messages.getHomeOverview()), new Span(title), new Span());
    CosmosCardComponent card = registration ? registrationCard(messages) : loginCard(messages);
    Div main = new Div(introduction, card);
    main.addClassName("study-main");
    main.getElement().setAttribute("role", "main");
    add(new CosmosBackgroundComponent(), header, main);
  }

  private CosmosCardComponent loginCard(MessageProperties messages) {
    EmailField email = new EmailField(messages.getLoginEmailField());
    email.setPlaceholder(messages.getAccountEmailPlaceholder());
    email.setRequiredIndicatorVisible(true);
    PasswordField password = new PasswordField(messages.getLoginPasswordField());
    password.setPlaceholder(messages.getAccountPasswordPlaceholder());
    password.setRequiredIndicatorVisible(true);
    Checkbox remember = new Checkbox(messages.getLoginRememberMeCheckbox());
    Div fields = new Div(email, password, remember);
    fields.addClassNames("cosmos-form", "study-fields", "study-fields--single");
    Button action = new Button(messages.getBaseLoginButton(), VaadinIcon.SIGN_IN.create());
    action.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    return card(messages.getLoginTitle(), fields, action);
  }

  private CosmosCardComponent registrationCard(MessageProperties messages) {
    TextField firstName = new TextField(messages.getRegistrationFirstNameField());
    firstName.setPlaceholder(messages.getAccountFirstNamePlaceholder());
    firstName.setRequiredIndicatorVisible(true);
    TextField lastName = new TextField(messages.getRegistrationLastNameField());
    lastName.setPlaceholder(messages.getAccountLastNamePlaceholder());
    lastName.setRequiredIndicatorVisible(true);
    EmailField email = new EmailField(messages.getRegistrationEmailField());
    email.setPlaceholder(messages.getAccountEmailPlaceholder());
    email.setRequiredIndicatorVisible(true);
    EmailField confirmEmail = new EmailField(messages.getRegistrationConfirmField());
    confirmEmail.setPlaceholder(messages.getAccountConfirmEmailPlaceholder());
    confirmEmail.setRequiredIndicatorVisible(true);
    PasswordField password = new PasswordField(messages.getRegistrationPasswordField());
    password.setPlaceholder(messages.getAccountPasswordPlaceholder());
    password.setRequiredIndicatorVisible(true);
    PasswordField confirmPassword = new PasswordField(messages.getRegistrationConfirmField());
    confirmPassword.setPlaceholder(messages.getAccountConfirmPasswordPlaceholder());
    confirmPassword.setRequiredIndicatorVisible(true);
    ComboBox<Gender> gender = new ComboBox<>(messages.getRegistrationGenderCombobox());
    gender.setItems(Gender.values());
    gender.setItemLabelGenerator(
        value ->
            switch (value) {
              case MALE -> messages.getGenderMale();
              case FEMALE -> messages.getGenderFemale();
              case OTHER -> messages.getGenderOther();
            });
    gender.setPlaceholder(messages.getAccountGenderPlaceholder());
    gender.setRequiredIndicatorVisible(true);
    DatePicker birthday = new DatePicker(messages.getRegistrationBirthDateField());
    birthday.setLocale(Locale.GERMAN);
    CompactDateInput.configure(birthday, messages);
    birthday.setPlaceholder(messages.getRegistrationBirthDatePlaceholder());
    birthday.setRequiredIndicatorVisible(true);
    birthday.setAutoOpen(false);
    Div fields =
        new Div(
            firstName, lastName, email, confirmEmail, password, confirmPassword, gender, birthday);
    fields.addClassNames("cosmos-form", "study-fields");
    Button action =
        new Button(messages.getBaseRegistrationButton(), VaadinIcon.USER_CHECK.create());
    action.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    return card(messages.getRegistrationTitle(), fields, action);
  }

  private CosmosCardComponent card(String title, Component fields, Button action) {
    Div head = new Div(new H2(title));
    head.addClassName("study-card-head");
    Div footer = new Div(action);
    footer.addClassName("study-card-foot");
    CosmosCardComponent card = new CosmosCardComponent(head, fields, footer);
    card.addClassName("study-card");
    return card;
  }
}
