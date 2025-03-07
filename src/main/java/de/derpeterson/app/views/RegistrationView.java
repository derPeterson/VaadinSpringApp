package de.derpeterson.app.views;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.AbstractSinglePropertyField;
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
import com.vaadin.flow.component.shared.HasValidationProperties;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.model.Role;
import de.derpeterson.app.model.User;
import de.derpeterson.app.model.User.Gender;
import de.derpeterson.app.service.RoleService;
import de.derpeterson.app.service.UserService;
import de.derpeterson.app.ui.components.RegistrationCardComponent;
import de.derpeterson.app.ui.helper.VaadinUIHelper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Route("registration")
@PageTitle("Registration")
@AnonymousAllowed
public class RegistrationView extends HorizontalLayout {


    public RegistrationView(CustomI18NProvider i18nProvider, UserService userService, RoleService roleService, PasswordEncoder passwordEncoder) {

        setSizeFull();
        setSpacing(false);
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        addClassNames("registration-bg-fullscreen");

        // Formular Felder erstellen
        RouterLink loginLink = new RouterLink(i18nProvider.getTranslation("registrationView.login_link"), LoginView.class);
        TextField firstNameField = new TextField(i18nProvider.getTranslation("registrationView.first_name_field"));
        firstNameField.setPrefixComponent(VaadinIcon.USER.create());
        firstNameField.setClearButtonVisible(true);
        firstNameField.setWidthFull();
        TextField lastNameField = new TextField(i18nProvider.getTranslation("registrationView.last_name_field"));
        lastNameField.setPrefixComponent(VaadinIcon.USER.create());
        lastNameField.setClearButtonVisible(true);
        lastNameField.setWidthFull();
        EmailField emailField = new EmailField(i18nProvider.getTranslation("registrationView.email_field"));
        emailField.setPrefixComponent(VaadinIcon.ENVELOPE.create());
        emailField.setClearButtonVisible(true);
        emailField.setWidthFull();
        EmailField confirmEmailField = new EmailField(i18nProvider.getTranslation("registrationView.confirm_field"));
        confirmEmailField.setPrefixComponent(VaadinIcon.ENVELOPE.create());
        confirmEmailField.setClearButtonVisible(true);
        confirmEmailField.setWidthFull();
        PasswordField passwordField = new PasswordField(i18nProvider.getTranslation("registrationView.password_field"));
        passwordField.setPrefixComponent(VaadinIcon.LOCK.create());
        passwordField.setClearButtonVisible(true);
        passwordField.setWidthFull();
        PasswordField confirmPasswordField = new PasswordField(i18nProvider.getTranslation("registrationView.confirm_field"));
        confirmPasswordField.setPrefixComponent(VaadinIcon.LOCK.create());
        confirmPasswordField.setClearButtonVisible(true);
        confirmPasswordField.setWidthFull();
        ComboBox<Gender> genderComboBox = new ComboBox<>(i18nProvider.getTranslation("registrationView.gender_combobox"));
        genderComboBox.setPrefixComponent(VaadinIcon.USERS.create());
        genderComboBox.setClearButtonVisible(true);
        genderComboBox.setWidthFull();
        genderComboBox.setItems(Gender.MALE, Gender.FEMALE, Gender.OTHER);
        genderComboBox.setItemLabelGenerator(Gender::name);
        DatePicker birthDatePicker = new DatePicker(i18nProvider.getTranslation("registrationView.birth_date_field"));
        birthDatePicker.setPrefixComponent(VaadinIcon.CALENDAR_USER.create());
        birthDatePicker.setClearButtonVisible(true);
        birthDatePicker.setWidthFull();

        // Validierungen
        emailField.setRequiredIndicatorVisible(true);
        confirmEmailField.setRequiredIndicatorVisible(true);
        passwordField.setRequiredIndicatorVisible(true);
        confirmPasswordField.setRequiredIndicatorVisible(true);
        birthDatePicker.setRequiredIndicatorVisible(true);

        List<AbstractSinglePropertyField> allRequiredComponents = List.of(emailField, confirmEmailField, passwordField, confirmPasswordField, birthDatePicker);

        Button registerButton = new Button(i18nProvider.getTranslation("base.registration_button"), event -> {
            // CHECK REQUIRED
            if (allRequiredComponents.stream().anyMatch(AbstractField::isEmpty)) {
                VaadinUIHelper.showNotification(i18nProvider.getTranslation("registrationView.validation.error_title"),
                        i18nProvider.getTranslation("registrationView.validation.required_message"),
                        -1, VaadinUIHelper.NotificationType.ERROR);

                allRequiredComponents.stream().filter(AbstractField::isEmpty).map(HasValidationProperties.class::cast).forEach(field -> field.setInvalid(true));
                allRequiredComponents.stream().filter(Predicate.not(AbstractField::isEmpty)).map(HasValidationProperties.class::cast).forEach(field -> field.setInvalid(false));
                return;
            } else {
                allRequiredComponents.stream().map(HasValidationProperties.class::cast).forEach(field -> field.setInvalid(false));
            }

            // CHECK EMAIL FORMAT
            if (!emailField.getValue().matches(EmailValidator.PATTERN) || !confirmEmailField.getValue().matches(EmailValidator.PATTERN)) {
                VaadinUIHelper.showNotification(i18nProvider.getTranslation("registrationView.validation.error_title"),
                        i18nProvider.getTranslation("registrationView.validation.email_invalid_message"),
                        -1, VaadinUIHelper.NotificationType.ERROR);

                emailField.setInvalid(!emailField.getValue().matches(EmailValidator.PATTERN));
                confirmEmailField.setInvalid(!confirmEmailField.getValue().matches(EmailValidator.PATTERN));

                return;
            } else {
                emailField.setInvalid(false);
                confirmEmailField.setInvalid(false);
            }

            // CHECK EMAIL MATCH
            if (!StringUtils.equals(emailField.getValue(), confirmEmailField.getValue())) {
                VaadinUIHelper.showNotification(i18nProvider.getTranslation("registrationView.validation.error_title"),
                        i18nProvider.getTranslation("registrationView.validation.email_confirm_message"),
                        -1, VaadinUIHelper.NotificationType.ERROR);

                emailField.setInvalid(true);
                confirmEmailField.setInvalid(true);

                return;
            } else {
                passwordField.setInvalid(false);
                confirmEmailField.setInvalid(false);
            }

            // CHECK EMAIL EXISTS
            if (userService.findByEmail(emailField.getValue()).isPresent()) {
                VaadinUIHelper.showNotification(i18nProvider.getTranslation("registrationView.validation.error_title"),
                        i18nProvider.getTranslation("registrationView.validation.email_exists_message"),
                        -1, VaadinUIHelper.NotificationType.ERROR);

                emailField.setInvalid(true);
                confirmEmailField.setInvalid(true);

                return;
            } else {
                passwordField.setInvalid(false);
                confirmEmailField.setInvalid(false);
            }

            // CHECK PASSWORD SECURE
            if (!passwordField.getValue().matches(User.PASSWORD_REGEX) || !confirmPasswordField.getValue().matches(User.PASSWORD_REGEX)) {
                VaadinUIHelper.showNotification(i18nProvider.getTranslation("registrationView.validation.error_title"),
                        i18nProvider.getTranslation("registrationView.validation.password_invalid_message"),
                        -1, VaadinUIHelper.NotificationType.ERROR);

                passwordField.setInvalid(passwordField.getValue().isEmpty());
                confirmPasswordField.setInvalid(confirmPasswordField.getValue().isEmpty());

                return;
            } else {
                passwordField.setInvalid(false);
                confirmPasswordField.setInvalid(false);
            }

            // CHECK PASSWORD MATCH
            if (!StringUtils.equals(passwordField.getValue(), confirmPasswordField.getValue())) {
                VaadinUIHelper.showNotification(i18nProvider.getTranslation("registrationView.validation.error_title"),
                        i18nProvider.getTranslation("registrationView.validation.password_confirm_message"),
                        -1, VaadinUIHelper.NotificationType.ERROR);

                passwordField.setInvalid(true);
                confirmPasswordField.setInvalid(true);

                return;
            } else {
                passwordField.setInvalid(false);
                confirmPasswordField.setInvalid(false);
            }

            // CHECK BIRTH DATE PAST
            if (birthDatePicker.getValue().isAfter(LocalDate.now())) {
                Notification.show("Das Geburtsdatum muss in der Vergangenheit liegen.");
                VaadinUIHelper.showNotification(i18nProvider.getTranslation("registrationView.validation.error_title"),
                        i18nProvider.getTranslation("registrationView.validation.birth_date_past_message"),
                        -1, VaadinUIHelper.NotificationType.ERROR);

                birthDatePicker.setInvalid(true);

                return;
            } else {
                birthDatePicker.setInvalid(false);
            }

            Optional<Role> userRole = roleService.findByName("ROLE_USER");

            User user = User.builder()
                    .firstName(firstNameField.getValue())
                    .lastName(lastNameField.getValue())
                    .email(emailField.getValue())
                    .password(passwordEncoder.encode(passwordField.getValue()))
                    .gender(genderComboBox.getValue())
                    .birthDate(birthDatePicker.getValue())
                    .enabled(false)
                    .roles(userRole.map(List::of).orElse(Collections.emptyList()))
                    .build();

            userService.saveUser(user);

            VaadinUIHelper.showNotification(i18nProvider.getTranslation("registrationView.registration.success_title"),
                    MessageFormat.format(i18nProvider.getTranslation("registrationView.registration.success_message"), user.getEmail()),
                    -1, VaadinUIHelper.NotificationType.SUCCESS);

        });
        registerButton.setPrefixComponent(VaadinIcon.ARROW_FORWARD.create());
        registerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        registerButton.setWidthFull();

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

        var firstLineForm = new HorizontalLayout(firstNameField, lastNameField);
        firstLineForm.setWidthFull();
        var secondLineForm = new HorizontalLayout(emailField, confirmEmailField);
        secondLineForm.setWidthFull();
        var thirdLineForm = new HorizontalLayout(passwordField, confirmPasswordField);
        thirdLineForm.setWidthFull();
        var fourthLineForm = new HorizontalLayout(genderComboBox, birthDatePicker);
        fourthLineForm.setWidthFull();

        cardContentLayout.add(cardTitleLayout, cardSecondaryTitleLayout, firstLineForm, secondLineForm, thirdLineForm, fourthLineForm, registerButton);

        RegistrationCardComponent registrationCardComponent = new RegistrationCardComponent(cardContentLayout);
        registrationCardComponent.setWidth(null);

        mainContent.add(registrationCardComponent);

        add(VaadinUIHelper.createFullHorizontalSpace());
        add(mainContent);
        add(VaadinUIHelper.createFullHorizontalSpace());
    }
}
