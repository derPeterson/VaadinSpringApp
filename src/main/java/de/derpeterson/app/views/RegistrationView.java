package de.derpeterson.app.views;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.model.User;
import de.derpeterson.app.model.User.Gender;
import de.derpeterson.app.service.UserService;
import de.derpeterson.app.ui.components.RegistrationCardComponent;
import de.derpeterson.app.ui.helper.VaadinUIHelper;

import java.time.LocalDate;

@Route("registration")
@PageTitle("Registration")
@AnonymousAllowed
public class RegistrationView extends HorizontalLayout {


    public RegistrationView(CustomI18NProvider i18nProvider, UserService userService) {

        setSizeFull();
        setSpacing(false);
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        addClassNames("registration-bg-fullscreen");

        // Formular Felder erstellen
        RouterLink loginLink = new RouterLink(i18nProvider.getTranslation("registrationView.login_link"), LoginView.class);
        TextField firstNameField = new TextField(i18nProvider.getTranslation("registrationView.first_name_field"));
        firstNameField.setWidth(260, Unit.PIXELS);
        firstNameField.setPrefixComponent(VaadinIcon.USER.create());
        firstNameField.setClearButtonVisible(true);
        TextField lastNameField = new TextField(i18nProvider.getTranslation("registrationView.last_name_field"));
        lastNameField.setWidth(260, Unit.PIXELS);
        lastNameField.setPrefixComponent(VaadinIcon.USER.create());
        lastNameField.setClearButtonVisible(true);
        EmailField emailField = new EmailField(i18nProvider.getTranslation("registrationView.email_name_field"));
        emailField.setWidth(260, Unit.PIXELS);
        emailField.setPrefixComponent(VaadinIcon.ENVELOPE.create());
        emailField.setClearButtonVisible(true);
        EmailField confirmEmailField = new EmailField(i18nProvider.getTranslation("registrationView.confirm_field"));
        confirmEmailField.setWidth(260, Unit.PIXELS);
        confirmEmailField.setPrefixComponent(VaadinIcon.ENVELOPE.create());
        confirmEmailField.setClearButtonVisible(true);
        PasswordField passwordField = new PasswordField(i18nProvider.getTranslation("registrationView.password_field"));
        passwordField.setWidth(260, Unit.PIXELS);
        passwordField.setPrefixComponent(VaadinIcon.LOCK.create());
        passwordField.setClearButtonVisible(true);
        PasswordField confirmPasswordField = new PasswordField(i18nProvider.getTranslation("registrationView.confirm_field"));
        confirmPasswordField.setWidth(260, Unit.PIXELS);
        confirmPasswordField.setPrefixComponent(VaadinIcon.LOCK.create());
        confirmPasswordField.setClearButtonVisible(true);
        ComboBox<Gender> genderComboBox = new ComboBox<>(i18nProvider.getTranslation("registrationView.gender_combobox"));
        genderComboBox.setWidth(260, Unit.PIXELS);
        genderComboBox.setPrefixComponent(VaadinIcon.USERS.create());
        genderComboBox.setClearButtonVisible(true);
        DatePicker birthDatePicker = new DatePicker(i18nProvider.getTranslation("registrationView.birth_date_field"));
        birthDatePicker.setWidth(260, Unit.PIXELS);
        birthDatePicker.setPrefixComponent(VaadinIcon.CALENDAR_USER.create());
        birthDatePicker.setClearButtonVisible(true);
        Button registerButton = new Button(i18nProvider.getTranslation("base.registration_button"), event -> {
            if (emailField.isEmpty() || passwordField.isEmpty() || genderComboBox.isEmpty() || birthDatePicker.isEmpty()) {
                Notification.show("Bitte füllen Sie alle erforderlichen Felder aus.");
                return;
            }

            if (birthDatePicker.getValue().isAfter(LocalDate.now())) {
                Notification.show("Das Geburtsdatum muss in der Vergangenheit liegen.");
                return;
            }

            if (!passwordField.getValue().matches("^(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$")) {
                Notification.show("Das Passwort muss mindestens 8 Zeichen lang sein, einen Großbuchstaben und ein Sonderzeichen enthalten.");
                return;
            }

            User user = User.builder()
                    .firstName(firstNameField.getValue())
                    .lastName(lastNameField.getValue())
                    .email(emailField.getValue())
                    .password(passwordField.getValue())
                    .gender(genderComboBox.getValue())
                    .birthDate(birthDatePicker.getValue())
                    .enabled(true)
                    .build();

            userService.saveUser(user);
            Notification.show("Registrierung erfolgreich!");
        });
        registerButton.setPrefixComponent(VaadinIcon.ARROW_FORWARD.create());
        registerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        registerButton.setWidthFull();


        // Geschlechtsauswahl
        genderComboBox.setItems(Gender.MALE, Gender.FEMALE, Gender.OTHER);
        genderComboBox.setItemLabelGenerator(Gender::name);

        // Validierungen
        emailField.setRequiredIndicatorVisible(true);
        confirmEmailField.setRequiredIndicatorVisible(true);
        passwordField.setRequiredIndicatorVisible(true);
        confirmPasswordField.setRequiredIndicatorVisible(true);
        birthDatePicker.setRequiredIndicatorVisible(true);

        VerticalLayout mainContent = new VerticalLayout();
        mainContent.setPadding(false);
        mainContent.setSpacing(false);
        mainContent.setAlignItems(Alignment.CENTER);
        mainContent.setJustifyContentMode(JustifyContentMode.CENTER);

        // Layout
        VerticalLayout cardContentLayout = new VerticalLayout();
        cardContentLayout.setAlignItems(Alignment.CENTER);
        cardContentLayout.addClassNames(LumoUtility.TextColor.SECONDARY);

        H1 title = new H1(i18nProvider.getTranslation("registrationView.title"));
        title.addClassNames(LumoUtility.FontSize.XXXLARGE, LumoUtility.FontWeight.BOLD);

        HorizontalLayout cardTitleLayout = new HorizontalLayout();
        cardTitleLayout.setWidthFull();
        cardTitleLayout.setPadding(false);
        cardTitleLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        cardTitleLayout.add(title);

        Span createAccountQuestionText = new Span(i18nProvider.getTranslation("registrationView.login_question"));
        loginLink.addClassNames("underline");

        HorizontalLayout cardSecondaryTitleLayout = new HorizontalLayout();
        cardSecondaryTitleLayout.setWidthFull();
        cardSecondaryTitleLayout.setSpacing(false);
        cardSecondaryTitleLayout.setPadding(false);
        cardSecondaryTitleLayout.setJustifyContentMode(JustifyContentMode.START);
        cardSecondaryTitleLayout.addClassNames(LumoUtility.Gap.SMALL);
        cardSecondaryTitleLayout.add(createAccountQuestionText);
        cardSecondaryTitleLayout.add(loginLink);

        cardContentLayout.add(cardTitleLayout, cardSecondaryTitleLayout, new HorizontalLayout(firstNameField, lastNameField), new HorizontalLayout(emailField, confirmEmailField),
                new HorizontalLayout(passwordField, confirmPasswordField), new HorizontalLayout(genderComboBox, birthDatePicker), registerButton);

        RegistrationCardComponent registrationCardComponent = new RegistrationCardComponent(cardContentLayout);
        registrationCardComponent.setWidth(null);

        mainContent.add(registrationCardComponent);

        add(VaadinUIHelper.createFullHorizontalSpace());
        add(mainContent);
        add(VaadinUIHelper.createFullHorizontalSpace());
    }
}
