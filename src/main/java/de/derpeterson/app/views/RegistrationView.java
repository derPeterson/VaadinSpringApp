package de.derpeterson.app.views;

import com.vaadin.flow.component.AbstractSinglePropertyField;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.HasAllowedCharPattern;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.helper.components.RegistrationCardComponent;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.helper.ui.VaadinUIHelper;
import de.derpeterson.app.helper.ui.ValidationHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.Gender;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.security.IsNotAuthentificatedBaseView;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.RoleService;
import de.derpeterson.app.service.UserService;
import de.derpeterson.app.service.VerificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Route("registration")
@PageTitle("Registration")
@AnonymousAllowed
public class RegistrationView extends IsNotAuthentificatedBaseView<HorizontalLayout> {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationView.class);

    private static final String BASE_FAILED_TITLE_MESSAGE_KEY = "base.failed.title";

    private final transient CustomI18NProvider i18nProvider;
    private final transient UserService userService;
    private final transient RoleService roleService;
    private final transient PasswordEncoder passwordEncoder;
    private final transient VerificationService verificationService;

    private final transient FormComponents formComponents;

    private final List<? extends AbstractSinglePropertyField<? extends HasAllowedCharPattern, ? extends Serializable>> requiredFormComponts;

    public RegistrationView(CustomI18NProvider i18nProvider, UserService userService,
                            RoleService roleService, PasswordEncoder passwordEncoder,
                            VerificationService verificationService,
                            SecurityService securityService, HttpServletRequest request) {

        super(securityService, request, new HorizontalLayout());

        this.i18nProvider = i18nProvider;
        this.userService = userService;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
        this.verificationService = verificationService;

        setSizeFull();
        setSpacing(false);
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        addClassNames("registration-bg-fullscreen");

        formComponents = createFormComponents();
        requiredFormComponts = createRequiredFormCompontents();
        Button registerButton = createRegisterButton();

        VerticalLayout mainContent = new VerticalLayout();
        mainContent.setPadding(false);
        mainContent.setSpacing(false);
        mainContent.setAlignItems(FlexComponent.Alignment.CENTER);
        mainContent.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        // Layout
        VerticalLayout cardContentLayout = new VerticalLayout();
        cardContentLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        cardContentLayout.addClassNames(LumoUtility.TextColor.SECONDARY);

        H1 title = new H1(i18nProvider.getTranslation("registrationView.title"));
        title.addClassNames(LumoUtility.FontSize.XXXLARGE, LumoUtility.FontWeight.BOLD);

        HorizontalLayout cardTitleLayout = new HorizontalLayout();
        cardTitleLayout.setWidthFull();
        cardTitleLayout.setPadding(false);
        cardTitleLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        cardTitleLayout.add(title);

        Span createAccountQuestionText = new Span(i18nProvider.getTranslation("registrationView.login_question"));

        HorizontalLayout cardSecondaryTitleLayout = new HorizontalLayout();
        cardSecondaryTitleLayout.setWidthFull();
        cardSecondaryTitleLayout.setSpacing(false);
        cardSecondaryTitleLayout.setPadding(false);
        cardSecondaryTitleLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
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
        return validateRequiredInputs()
                && validateEmailInputs()
                && validatePasswordInputs()
                && validateBirthDate();
    }

    private boolean validateBirthDate() {
        return ValidationHelper.validateBirthDateBeforeInput(formComponents.birthDatePicker, i18nProvider);
    }

    private boolean validatePasswordInputs() {
        return ValidationHelper.validatePasswordConfirmInputs(formComponents.passwordField, formComponents.confirmPasswordField, i18nProvider)
                && ValidationHelper.validatePasswordSecureInputs(List.of(formComponents.passwordField, formComponents.confirmPasswordField), i18nProvider);
    }

    private boolean validateEmailInputs() {
        return ValidationHelper.validateEmailValidInputs(List.of(formComponents.emailField, formComponents.confirmEmailField), i18nProvider)
                && ValidationHelper.validateEmailConfirmInputs(formComponents.emailField, formComponents.confirmEmailField, i18nProvider)
                && ValidationHelper.validateEmailNotExistsInput(formComponents.emailField, formComponents.confirmEmailField, userService, i18nProvider);
    }

    private boolean validateRequiredInputs() {
        return ValidationHelper.validateRequiredInputs(requiredFormComponts, i18nProvider);
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

        try {
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
        } catch (IOException e) {
            logger.error("Exception occurred:", e);

            NotificationHelper.getInstance().showNotification(i18nProvider.getTranslation(BASE_FAILED_TITLE_MESSAGE_KEY),
                    i18nProvider.getTranslation("base.failed.message"),
                    -1, NotificationHelper.NotificationType.ERROR);
        }
    }

    private List<? extends AbstractSinglePropertyField<? extends HasAllowedCharPattern, ? extends Serializable>> createRequiredFormCompontents() {
        formComponents.firstNameField.setRequiredIndicatorVisible(true);
        formComponents.lastNameField.setRequiredIndicatorVisible(true);
        formComponents.emailField.setRequiredIndicatorVisible(true);
        formComponents.confirmEmailField.setRequiredIndicatorVisible(true);
        formComponents.passwordField.setRequiredIndicatorVisible(true);
        formComponents.confirmPasswordField.setRequiredIndicatorVisible(true);
        formComponents.genderComboBox().setRequiredIndicatorVisible(true);
        formComponents.birthDatePicker.setRequiredIndicatorVisible(true);

        return List.of(formComponents.firstNameField, formComponents.lastNameField, formComponents.emailField, formComponents.confirmEmailField,
                formComponents.passwordField, formComponents.confirmPasswordField, formComponents.genderComboBox, formComponents.birthDatePicker);
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

}
