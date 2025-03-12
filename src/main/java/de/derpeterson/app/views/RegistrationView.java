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
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.HasAllowedCharPattern;
import com.vaadin.flow.component.shared.HasValidationProperties;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.Gender;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.EmailQueueService;
import de.derpeterson.app.service.RoleService;
import de.derpeterson.app.service.UserService;
import de.derpeterson.app.service.VerificationService;
import de.derpeterson.app.ui.components.RegistrationCardComponent;
import de.derpeterson.app.ui.helper.NotificationHelper;
import de.derpeterson.app.ui.helper.VaadinUIHelper;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.Serializable;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Route("registration")
@PageTitle("Registration")
@AnonymousAllowed
public class RegistrationView extends HorizontalLayout implements BeforeEnterObserver {

    private static final String BASE_FAILED_TITLE_MESSAGE_KEY = "base.failed.title";

    private final transient CustomI18NProvider i18nProvider;
    private final transient UserService userService;
    private final transient RoleService roleService;
    private final transient PasswordEncoder passwordEncoder;
    private final transient EmailQueueService emailQueueService;
    private final transient VerificationService verificationService;

    private final transient SecurityService securityService;
    private final transient HttpServletRequest request;

    private final transient FormComponents formComponents;

    private final List<? extends AbstractSinglePropertyField<? extends HasAllowedCharPattern, ? extends Serializable>> requiredFormComponts;

    public RegistrationView(CustomI18NProvider i18nProvider, UserService userService,
                            RoleService roleService, PasswordEncoder passwordEncoder,
                            EmailQueueService emailQueueService, VerificationService verificationService,
                            SecurityService securityService, HttpServletRequest request) {
        this.i18nProvider = i18nProvider;
        this.userService = userService;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
        this.emailQueueService = emailQueueService;
        this.verificationService = verificationService;
        this.securityService = securityService;
        this.request = request;

        setSizeFull();
        setSpacing(false);
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        addClassNames("registration-bg-fullscreen");

        formComponents = createFormComponents();
        requiredFormComponts = createRequiredFormCompontents();
        Button registerButton = createRegisterButton();

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

        HorizontalLayout cardSecondaryTitleLayout = new HorizontalLayout();
        cardSecondaryTitleLayout.setWidthFull();
        cardSecondaryTitleLayout.setSpacing(false);
        cardSecondaryTitleLayout.setPadding(false);
        cardSecondaryTitleLayout.setJustifyContentMode(JustifyContentMode.START);
        cardSecondaryTitleLayout.addClassNames(LumoUtility.Gap.SMALL);
        cardSecondaryTitleLayout.add(createAccountQuestionText);
        cardSecondaryTitleLayout.add(formComponents.loginLink);

        var firstLineForm = new HorizontalLayout(formComponents.firstNameField, formComponents.lastNameField);
        firstLineForm.setWidthFull();
        var secondLineForm = new HorizontalLayout(formComponents.emailField, formComponents.confirmEmailField);
        secondLineForm.setWidthFull();
        var thirdLineForm = new HorizontalLayout(formComponents.passwordField, formComponents.confirmPasswordField);
        thirdLineForm.setWidthFull();
        var fourthLineForm = new HorizontalLayout(formComponents.genderComboBox, formComponents.birthDatePicker);
        fourthLineForm.setWidthFull();

        cardContentLayout.add(cardTitleLayout, cardSecondaryTitleLayout, firstLineForm, secondLineForm, thirdLineForm, fourthLineForm, registerButton);

        RegistrationCardComponent registrationCardComponent = new RegistrationCardComponent(cardContentLayout);
        registrationCardComponent.setWidth(null);

        mainContent.add(registrationCardComponent);

        add(VaadinUIHelper.createFullHorizontalSpace());
        add(mainContent);
        add(VaadinUIHelper.createFullHorizontalSpace());
    }

    private Button createRegisterButton() {
        Button regButton = new Button(i18nProvider.getTranslation("base.registration_button"));
        regButton.setPrefixComponent(VaadinIcon.EDIT.create());
        regButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        regButton.setWidthFull();
        regButton.addClickListener(event -> handleRegistration());
        return regButton;
    }

    private void handleRegistration() {
        if (!validateInputs()) {
            return;
        }
        saveUserAndSendEmail();
    }

    private boolean validateInputs() {
        return validateRequiredInputs() && validateEmailInputs() && validatePasswordInputs() && validateBirthDate();
    }

    private boolean validateBirthDate() {
        // CHECK BIRTH DATE PAST
        if (formComponents.birthDatePicker.getValue().isAfter(LocalDate.now())) {
            NotificationHelper.getInstance().showNotification(i18nProvider.getTranslation(BASE_FAILED_TITLE_MESSAGE_KEY),
                    i18nProvider.getTranslation("registrationView.validation.birth_date_past_message"),
                    -1, NotificationHelper.NotificationType.ERROR);

            formComponents.birthDatePicker.setInvalid(true);

            return false;
        } else {
            formComponents.birthDatePicker.setInvalid(false);
        }

        return true;
    }

    private boolean validatePasswordInputs() {
        // CHECK PASSWORD SECURE
        if (!formComponents.passwordField.getValue().matches(UserEntity.PASSWORD_REGEX) || !formComponents.confirmPasswordField.getValue().matches(UserEntity.PASSWORD_REGEX)) {
            NotificationHelper.getInstance().showNotification(i18nProvider.getTranslation(BASE_FAILED_TITLE_MESSAGE_KEY),
                    i18nProvider.getTranslation("registrationView.validation.password_invalid_message"),
                    -1, NotificationHelper.NotificationType.ERROR);

            formComponents.passwordField.setInvalid(!formComponents.passwordField.getValue().matches(UserEntity.PASSWORD_REGEX));
            formComponents.confirmPasswordField.setInvalid(!formComponents.confirmPasswordField.getValue().matches(UserEntity.PASSWORD_REGEX));

            return false;
        } else {
            formComponents.passwordField.setInvalid(false);
            formComponents.confirmPasswordField.setInvalid(false);
        }

        // CHECK PASSWORD MATCH
        if (!StringUtils.equals(formComponents.passwordField.getValue(), formComponents.confirmPasswordField.getValue())) {
            NotificationHelper.getInstance().showNotification(i18nProvider.getTranslation(BASE_FAILED_TITLE_MESSAGE_KEY),
                    i18nProvider.getTranslation("registrationView.validation.password_confirm_message"),
                    -1, NotificationHelper.NotificationType.ERROR);

            formComponents.passwordField.setInvalid(true);
            formComponents.confirmPasswordField.setInvalid(true);

            return false;
        } else {
            formComponents.passwordField.setInvalid(false);
            formComponents.confirmPasswordField.setInvalid(false);
        }

        return true;
    }

    private boolean validateEmailInputs() {
        // CHECK EMAIL FORMAT
        if (!formComponents.emailField.getValue().matches(EmailValidator.PATTERN) || !formComponents.confirmEmailField.getValue().matches(EmailValidator.PATTERN)) {
            NotificationHelper.getInstance().showNotification(i18nProvider.getTranslation(BASE_FAILED_TITLE_MESSAGE_KEY),
                    i18nProvider.getTranslation("registrationView.validation.email_invalid_message"),
                    -1, NotificationHelper.NotificationType.ERROR);

            formComponents.emailField.setInvalid(!formComponents.emailField.getValue().matches(EmailValidator.PATTERN));
            formComponents.confirmEmailField.setInvalid(!formComponents.confirmEmailField.getValue().matches(EmailValidator.PATTERN));

            return false;
        } else {
            formComponents.emailField.setInvalid(false);
            formComponents.confirmEmailField.setInvalid(false);
        }

        // CHECK EMAIL MATCH
        if (!StringUtils.equals(formComponents.emailField.getValue(), formComponents.confirmEmailField.getValue())) {
            NotificationHelper.getInstance().showNotification(i18nProvider.getTranslation(BASE_FAILED_TITLE_MESSAGE_KEY),
                    i18nProvider.getTranslation("registrationView.validation.email_confirm_message"),
                    -1, NotificationHelper.NotificationType.ERROR);

            formComponents.emailField.setInvalid(true);
            formComponents.confirmEmailField.setInvalid(true);

            return false;
        } else {
            formComponents.passwordField.setInvalid(false);
            formComponents.confirmEmailField.setInvalid(false);
        }

        // CHECK EMAIL EXISTS
        if (userService.findByEmail(formComponents.emailField.getValue()).isPresent()) {
            NotificationHelper.getInstance().showNotification(i18nProvider.getTranslation(BASE_FAILED_TITLE_MESSAGE_KEY),
                    i18nProvider.getTranslation("registrationView.validation.email_exists_message"),
                    -1, NotificationHelper.NotificationType.ERROR);

            formComponents.emailField.setInvalid(true);
            formComponents.confirmEmailField.setInvalid(true);

            return false;
        } else {
            formComponents.passwordField.setInvalid(false);
            formComponents.confirmEmailField.setInvalid(false);
        }

        return true;
    }

    private boolean validateRequiredInputs() {
        if (requiredFormComponts.stream().anyMatch(AbstractField::isEmpty)) {
            NotificationHelper.getInstance().showNotification(i18nProvider.getTranslation(BASE_FAILED_TITLE_MESSAGE_KEY),
                    i18nProvider.getTranslation("registrationView.validation.required_message"),
                    -1, NotificationHelper.NotificationType.ERROR);

            requiredFormComponts.stream().filter(AbstractField::isEmpty).map(HasValidationProperties.class::cast).forEach(field -> field.setInvalid(true));
            requiredFormComponts.stream().filter(Predicate.not(AbstractField::isEmpty)).map(HasValidationProperties.class::cast).forEach(field -> field.setInvalid(false));
            return false;
        } else {
            requiredFormComponts.stream().map(HasValidationProperties.class::cast).forEach(field -> field.setInvalid(false));
        }

        return true;
    }

    private void saveUserAndSendEmail() {
        Optional<RoleEntity> userRole = roleService.findByName(RoleType.ROLE_USER);

        UserEntity userEntity = UserEntity.builder()
                .firstName(formComponents.firstNameField.getValue())
                .lastName(formComponents.lastNameField.getValue())
                .email(formComponents.emailField.getValue())
                .password(passwordEncoder.encode(formComponents.passwordField.getValue()))
                .gender(formComponents.genderComboBox.getValue())
                .birthDate(formComponents.birthDatePicker.getValue())
                .enabled(false)
                .roleEntities(userRole.map(List::of).orElse(Collections.emptyList()))
                .build();

        userService.saveUser(userEntity);

        boolean emailSent = verificationService.sendVerificationEmailByUser(userEntity);

        if (emailSent) {
            NotificationHelper.getInstance().showNotification(i18nProvider.getTranslation("base.success.title"),
                    MessageFormat.format(i18nProvider.getTranslation("registrationView.registration.success_message"), userEntity.getEmail()),
                    -1, NotificationHelper.NotificationType.SUCCESS);
        } else {
            NotificationHelper.getInstance().showNotification(i18nProvider.getTranslation(BASE_FAILED_TITLE_MESSAGE_KEY),
                    i18nProvider.getTranslation("base.failed.message"),
                    -1, NotificationHelper.NotificationType.ERROR);
        }
    }

    private List<? extends AbstractSinglePropertyField<? extends HasAllowedCharPattern, ? extends Serializable>> createRequiredFormCompontents() {
        formComponents.emailField.setRequiredIndicatorVisible(true);
        formComponents.confirmEmailField.setRequiredIndicatorVisible(true);
        formComponents.passwordField.setRequiredIndicatorVisible(true);
        formComponents.confirmPasswordField.setRequiredIndicatorVisible(true);
        formComponents.birthDatePicker.setRequiredIndicatorVisible(true);

        return List.of(formComponents.emailField, formComponents.confirmEmailField, formComponents.passwordField, formComponents.confirmPasswordField, formComponents.birthDatePicker);
    }

    private FormComponents createFormComponents() {
        RouterLink loginLink = new RouterLink(i18nProvider.getTranslation("registrationView.login_link"), LoginView.class);
        loginLink.addClassNames("underline");
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

        return new FormComponents(loginLink, firstNameField, lastNameField, emailField, confirmEmailField, passwordField, confirmPasswordField, genderComboBox, birthDatePicker);
    }

    private record FormComponents(RouterLink loginLink, TextField firstNameField, TextField lastNameField,
                                  EmailField emailField,
                                  EmailField confirmEmailField, PasswordField passwordField,
                                  PasswordField confirmPasswordField, ComboBox<Gender> genderComboBox,
                                  DatePicker birthDatePicker) {
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        if (securityService.getAuthenticatedUser(this.request).isPresent()) {
            beforeEnterEvent.forwardTo(AdminView.class);
        }
    }
}
