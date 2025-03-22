package de.derpeterson.app.views;

import com.vaadin.flow.component.AbstractSinglePropertyField;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
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
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.config.AppRouteConstants;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.helper.ui.ComponentTextUpdateHelper;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.helper.ui.VaadinUIHelper;
import de.derpeterson.app.helper.ui.ValidationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.Gender;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.security.IsNotAuthentificatedBaseView;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.RoleService;
import de.derpeterson.app.service.UserService;
import de.derpeterson.app.service.VerificationService;
import de.derpeterson.app.ui.components.RegistrationCardComponent;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.function.Supplier;

@Route(AppRouteConstants.REGISTRATION_ROUTE)
@PageTitle(AppRouteConstants.REGISTRATION_PAGE_TITLE)
@AnonymousAllowed
public class RegistrationView extends IsNotAuthentificatedBaseView<HorizontalLayout> {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationView.class);

    private final transient MessageProperties messageProperties;
    private final transient UserService userService;
    private final transient RoleService roleService;
    private final transient PasswordEncoder passwordEncoder;
    private final transient VerificationService verificationService;

    private final transient FormComponents formComponents;

    private final List<? extends AbstractSinglePropertyField<? extends HasAllowedCharPattern, ? extends Serializable>> requiredFormComponents;

    private H1 title = null;
    private Span createAccountQuestionText = null;
    private RouterLink loginLink = null;
    private TextField firstNameField = null;
    private TextField lastNameField = null;
    private EmailField emailField = null;
    private EmailField confirmEmailField = null;
    private PasswordField passwordField = null;
    private PasswordField confirmPasswordField = null;
    private ComboBox<Gender> genderComboBox = null;
    private DatePicker birthDatePicker = null;
    private Button registrationButton = null;

    public RegistrationView(MessageProperties messageProperties, UserService userService,
                            RoleService roleService, PasswordEncoder passwordEncoder,
                            VerificationService verificationService,
                            SecurityService securityService, HttpServletRequest request) {

        super(securityService, request, new HorizontalLayout());

        this.messageProperties = messageProperties;
        this.userService = userService;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
        this.verificationService = verificationService;

        ComponentUtil.addListener(UI.getCurrent(), LanguageChangeEvent.class, event -> {
            VaadinSession.getCurrent().setLocale(event.getNewLocale());

            Map<Component, Supplier<String>> componentTranslationSupplierMap = new HashMap<>();
            Optional.ofNullable(title)
                    .ifPresent(component -> componentTranslationSupplierMap.put(component, this.messageProperties::getRegistrationTitle));
            Optional.ofNullable(createAccountQuestionText)
                    .ifPresent(component -> componentTranslationSupplierMap.put(component, this.messageProperties::getRegistrationLoginQuestion));
            Optional.ofNullable(loginLink)
                    .ifPresent(component -> componentTranslationSupplierMap.put(component, this.messageProperties::getRegistrationLoginLink));
            Optional.ofNullable(firstNameField)
                    .ifPresent(component -> componentTranslationSupplierMap.put(component, this.messageProperties::getRegistrationFirstNameField));
            Optional.ofNullable(lastNameField)
                    .ifPresent(component -> componentTranslationSupplierMap.put(component, this.messageProperties::getRegistrationLastNameField));
            Optional.ofNullable(emailField)
                    .ifPresent(component -> componentTranslationSupplierMap.put(component, this.messageProperties::getRegistrationEmailField));
            Optional.ofNullable(confirmEmailField)
                    .ifPresent(component -> componentTranslationSupplierMap.put(component, this.messageProperties::getRegistrationConfirmField));
            Optional.ofNullable(passwordField)
                    .ifPresent(component -> componentTranslationSupplierMap.put(component, this.messageProperties::getRegistrationPasswordField));
            Optional.ofNullable(confirmPasswordField)
                    .ifPresent(component -> componentTranslationSupplierMap.put(component, this.messageProperties::getRegistrationConfirmField));
            Optional.ofNullable(genderComboBox)
                    .ifPresent(component -> componentTranslationSupplierMap.put(component, this.messageProperties::getRegistrationGenderCombobox));
            Optional.ofNullable(birthDatePicker)
                    .ifPresent(component -> componentTranslationSupplierMap.put(component, this.messageProperties::getRegistrationBirthDateField));
            Optional.ofNullable(registrationButton)
                    .ifPresent(component -> componentTranslationSupplierMap.put(component, this.messageProperties::getBaseRegistrationButton));

            ComponentTextUpdateHelper.updateComponents(componentTranslationSupplierMap);
            NotificationHelper.getInstance().updateText();
        });

        setSizeFull();
        setSpacing(false);
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        addClassNames("registration-bg-fullscreen");

        formComponents = createFormComponents();
        requiredFormComponents = createRequiredFormComponents();
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

        this.title = new H1(messageProperties.getRegistrationTitle());
        title.addClassNames(LumoUtility.FontSize.XXXLARGE, LumoUtility.FontWeight.BOLD);

        HorizontalLayout cardTitleLayout = new HorizontalLayout();
        cardTitleLayout.setWidthFull();
        cardTitleLayout.setPadding(false);
        cardTitleLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        cardTitleLayout.add(title);

        this.createAccountQuestionText = new Span(messageProperties.getRegistrationLoginQuestion());

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
        this.registrationButton = new Button(messageProperties.getBaseRegistrationButton());
        registrationButton.setPrefixComponent(VaadinIcon.EDIT.create());
        registrationButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        registrationButton.setWidthFull();
        registrationButton.addClickListener(event -> handleRegistration());
        return registrationButton;
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
        return ValidationHelper.validateBirthDateBeforeInput(formComponents.birthDatePicker, messageProperties);
    }

    private boolean validatePasswordInputs() {
        return ValidationHelper.validatePasswordConfirmInputs(formComponents.passwordField, formComponents.confirmPasswordField, messageProperties)
                && ValidationHelper.validatePasswordSecureInputs(List.of(formComponents.passwordField, formComponents.confirmPasswordField), messageProperties);
    }

    private boolean validateEmailInputs() {
        return ValidationHelper.validateEmailValidInputs(List.of(formComponents.emailField, formComponents.confirmEmailField), messageProperties)
                && ValidationHelper.validateEmailConfirmInputs(formComponents.emailField, formComponents.confirmEmailField, messageProperties)
                && ValidationHelper.validateEmailNotExistsInput(formComponents.emailField, formComponents.confirmEmailField, userService, messageProperties);
    }

    private boolean validateRequiredInputs() {
        return ValidationHelper.validateRequiredInputs(requiredFormComponents, messageProperties);
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
                NotificationHelper.getInstance().showNotification(messageProperties::getBaseSuccessTitle,
                        () -> messageProperties.getRegistrationSuccessMessage(userEntity.getEmail()),
                        -1, NotificationHelper.NotificationType.SUCCESS);
            } else {
                NotificationHelper.getInstance().showNotification(messageProperties::getBaseFailedTitle,
                        messageProperties::getBaseFailedMessage,
                        -1, NotificationHelper.NotificationType.ERROR);
            }
        } catch (IOException e) {
            logger.error("Exception occurred:", e);

            NotificationHelper.getInstance().showNotification(messageProperties::getBaseFailedTitle,
                    messageProperties::getBaseFailedMessage,
                    -1, NotificationHelper.NotificationType.ERROR);
        }
    }

    private List<? extends AbstractSinglePropertyField<? extends HasAllowedCharPattern, ? extends Serializable>> createRequiredFormComponents() {
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
        this.loginLink = new RouterLink(messageProperties.getRegistrationLoginLink(), LoginView.class);
        loginLink.addClassNames("underline");
        this.firstNameField = new TextField(messageProperties.getRegistrationFirstNameField());
        firstNameField.setPrefixComponent(VaadinIcon.USER.create());
        firstNameField.setClearButtonVisible(true);
        firstNameField.setWidthFull();
        this.lastNameField = new TextField(messageProperties.getRegistrationLastNameField());
        lastNameField.setPrefixComponent(VaadinIcon.USER.create());
        lastNameField.setClearButtonVisible(true);
        lastNameField.setWidthFull();
        this.emailField = new EmailField(messageProperties.getRegistrationEmailField());
        emailField.setPrefixComponent(VaadinIcon.ENVELOPE.create());
        emailField.setClearButtonVisible(true);
        emailField.setWidthFull();
        this.confirmEmailField = new EmailField(messageProperties.getRegistrationConfirmField());
        confirmEmailField.setPrefixComponent(VaadinIcon.ENVELOPE.create());
        confirmEmailField.setClearButtonVisible(true);
        confirmEmailField.setWidthFull();
        this.passwordField = new PasswordField(messageProperties.getRegistrationPasswordField());
        passwordField.setPrefixComponent(VaadinIcon.LOCK.create());
        passwordField.setClearButtonVisible(true);
        passwordField.setWidthFull();
        this.confirmPasswordField = new PasswordField(messageProperties.getRegistrationConfirmField());
        confirmPasswordField.setPrefixComponent(VaadinIcon.LOCK.create());
        confirmPasswordField.setClearButtonVisible(true);
        confirmPasswordField.setWidthFull();
        this.genderComboBox = new ComboBox<>(messageProperties.getRegistrationGenderCombobox());
        genderComboBox.setPrefixComponent(VaadinIcon.USERS.create());
        genderComboBox.setClearButtonVisible(true);
        genderComboBox.setWidthFull();
        genderComboBox.setItems(Gender.MALE, Gender.FEMALE, Gender.OTHER);
        genderComboBox.setItemLabelGenerator(Gender::name);
        this.birthDatePicker = new DatePicker(messageProperties.getRegistrationBirthDateField());
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
