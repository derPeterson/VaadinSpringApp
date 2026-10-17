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
import de.derpeterson.app.helper.ui.ValidationHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.Gender;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.security.IsNotAuthenticatedBaseView;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.RegistrationService;
import de.derpeterson.app.service.RoleService;
import de.derpeterson.app.service.UserService;
import de.derpeterson.app.ui.components.CosmosCardComponent;
import de.derpeterson.app.ui.components.CosmosPageComponent;
import de.derpeterson.app.validation.EmailConflict;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

@Route(AppRouteConstants.REGISTRATION_ROUTE)
@PageTitle(AppRouteConstants.REGISTRATION_PAGE_TITLE)
@AnonymousAllowed
public class RegistrationView extends IsNotAuthenticatedBaseView<HorizontalLayout> {

  private static final Logger logger = LoggerFactory.getLogger(RegistrationView.class);

  private final transient MessageProperties messageProperties;
  private final transient UserService userService;
  private final transient RoleService roleService;
  private final transient PasswordEncoder passwordEncoder;
  private final transient RegistrationService registrationService;

  private final transient FormComponents formComponents;

  private final List<
          ? extends
              AbstractSinglePropertyField<? extends HasAllowedCharPattern, ? extends Serializable>>
      requiredFormComponents;

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

  public RegistrationView(
      MessageProperties messageProperties,
      UserService userService,
      RoleService roleService,
      PasswordEncoder passwordEncoder,
      RegistrationService registrationService,
      SecurityService securityService,
      HttpServletRequest request,
      ConfigService configService) {

    super(securityService, request, new HorizontalLayout());

    this.messageProperties = messageProperties;
    this.userService = userService;
    this.roleService = roleService;
    this.passwordEncoder = passwordEncoder;
    this.registrationService = registrationService;

    ComponentUtil.addListener(
        UI.getCurrent(),
        LanguageChangeEvent.class,
        event -> {
          VaadinSession.getCurrent().setLocale(event.getNewLocale());

          Map<Component, Supplier<String>> componentTranslationSupplierMap = new HashMap<>();
          Optional.ofNullable(title)
              .ifPresent(
                  component ->
                      componentTranslationSupplierMap.put(
                          component, this.messageProperties::getRegistrationTitle));
          Optional.ofNullable(createAccountQuestionText)
              .ifPresent(
                  component ->
                      componentTranslationSupplierMap.put(
                          component, this.messageProperties::getRegistrationLoginQuestion));
          Optional.ofNullable(loginLink)
              .ifPresent(
                  component ->
                      componentTranslationSupplierMap.put(
                          component, this.messageProperties::getRegistrationLoginLink));
          Optional.ofNullable(firstNameField)
              .ifPresent(
                  component ->
                      componentTranslationSupplierMap.put(
                          component, this.messageProperties::getRegistrationFirstNameField));
          Optional.ofNullable(lastNameField)
              .ifPresent(
                  component ->
                      componentTranslationSupplierMap.put(
                          component, this.messageProperties::getRegistrationLastNameField));
          Optional.ofNullable(emailField)
              .ifPresent(
                  component ->
                      componentTranslationSupplierMap.put(
                          component, this.messageProperties::getRegistrationEmailField));
          Optional.ofNullable(confirmEmailField)
              .ifPresent(
                  component ->
                      componentTranslationSupplierMap.put(
                          component, this.messageProperties::getRegistrationConfirmField));
          Optional.ofNullable(passwordField)
              .ifPresent(
                  component ->
                      componentTranslationSupplierMap.put(
                          component, this.messageProperties::getRegistrationPasswordField));
          Optional.ofNullable(confirmPasswordField)
              .ifPresent(
                  component ->
                      componentTranslationSupplierMap.put(
                          component, this.messageProperties::getRegistrationConfirmField));
          Optional.ofNullable(genderComboBox)
              .ifPresent(
                  component ->
                      componentTranslationSupplierMap.put(
                          component, this.messageProperties::getRegistrationGenderCombobox));
          Optional.ofNullable(birthDatePicker)
              .ifPresent(
                  component ->
                      componentTranslationSupplierMap.put(
                          component, this.messageProperties::getRegistrationBirthDateField));
          Optional.ofNullable(registrationButton)
              .ifPresent(
                  component ->
                      componentTranslationSupplierMap.put(
                          component, this.messageProperties::getBaseRegistrationButton));

          ComponentTextUpdateHelper.updateComponents(componentTranslationSupplierMap);
          updateInputPlaceholders();
          Optional.ofNullable(birthDatePicker)
              .ifPresent(
                  picker ->
                      picker.setPlaceholder(
                          this.messageProperties.getRegistrationBirthDatePlaceholder()));
          Optional.ofNullable(genderComboBox)
              .ifPresent(
                  comboBox -> {
                    comboBox.setItemLabelGenerator(this::formatGender);
                    comboBox.getDataProvider().refreshAll();
                  });
          NotificationHelper.getInstance().updateText();
        });

    getContent().setWidthFull();
    getContent().setPadding(false);
    setSpacing(false);
    setAlignItems(FlexComponent.Alignment.CENTER);
    setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

    getContent().removeAll();

    formComponents = createFormComponents();
    requiredFormComponents = createRequiredFormComponents();
    Button registerButton = createRegisterButton();

    CosmosPageComponent pageLayout =
        new CosmosPageComponent(
            configService, true, messageProperties::getAccountCopyrightTemplate);
    VerticalLayout mainContent = pageLayout.getCardContainer();

    // Layout
    VerticalLayout cardContentLayout = new VerticalLayout();
    cardContentLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
    cardContentLayout.setPadding(false);
    cardContentLayout.setSpacing(false);
    cardContentLayout.addClassName("cosmos-form");

    this.title = new H1(messageProperties.getRegistrationTitle());
    title.addClassName("cosmos-headline");

    HorizontalLayout cardTitleLayout = new HorizontalLayout();
    cardTitleLayout.addClassName("cosmos-title-row");
    cardTitleLayout.setWidthFull();
    cardTitleLayout.setPadding(false);
    cardTitleLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
    cardTitleLayout.add(title);

    this.createAccountQuestionText = new Span(messageProperties.getRegistrationLoginQuestion());

    HorizontalLayout cardSecondaryTitleLayout = new HorizontalLayout();
    cardSecondaryTitleLayout.addClassNames("cosmos-intro", "cosmos-form-text");
    cardSecondaryTitleLayout.setWidthFull();
    cardSecondaryTitleLayout.setSpacing(false);
    cardSecondaryTitleLayout.setPadding(false);
    cardSecondaryTitleLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
    cardSecondaryTitleLayout.addClassNames(LumoUtility.Gap.SMALL, "cosmos-form-links");
    cardSecondaryTitleLayout.add(createAccountQuestionText);
    cardSecondaryTitleLayout.add(formComponents.loginLink);

    var firstLineForm =
        new HorizontalLayout(formComponents.firstNameField, formComponents.lastNameField);
    firstLineForm.setWidthFull();
    firstLineForm.setSpacing(false);
    firstLineForm.addClassName("cosmos-form-row");
    var secondLineForm =
        new HorizontalLayout(formComponents.emailField, formComponents.confirmEmailField);
    secondLineForm.setWidthFull();
    secondLineForm.setSpacing(false);
    secondLineForm.addClassName("cosmos-form-row");
    var thirdLineForm =
        new HorizontalLayout(formComponents.passwordField, formComponents.confirmPasswordField);
    thirdLineForm.setWidthFull();
    thirdLineForm.setSpacing(false);
    thirdLineForm.addClassName("cosmos-form-row");
    var fourthLineForm =
        new HorizontalLayout(formComponents.genderComboBox, formComponents.birthDatePicker);
    fourthLineForm.setWidthFull();
    fourthLineForm.setSpacing(false);
    fourthLineForm.addClassName("cosmos-form-row");

    cardContentLayout.add(
        cardTitleLayout,
        cardSecondaryTitleLayout,
        firstLineForm,
        secondLineForm,
        thirdLineForm,
        fourthLineForm,
        registerButton);

    CosmosCardComponent registrationCardComponent = new CosmosCardComponent(cardContentLayout);

    mainContent.add(registrationCardComponent);

    updateInputPlaceholders();
    add(pageLayout);
  }

  private Button createRegisterButton() {
    this.registrationButton = new Button(messageProperties.getBaseRegistrationButton());
    registrationButton.setPrefixComponent(VaadinIcon.EDIT.create());
    registrationButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    registrationButton.setWidthFull();
    registrationButton.addClassName("cosmos-submit");
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
    return ValidationHelper.validateBirthDateBeforeInput(
        formComponents.birthDatePicker, messageProperties);
  }

  private boolean validatePasswordInputs() {
    return ValidationHelper.validatePasswordConfirmInputs(
            formComponents.passwordField, formComponents.confirmPasswordField, messageProperties)
        && ValidationHelper.validatePasswordSecureInputs(
            List.of(formComponents.passwordField, formComponents.confirmPasswordField),
            messageProperties);
  }

  private boolean validateEmailInputs() {
    return ValidationHelper.validateEmailValidInputs(
            List.of(formComponents.emailField, formComponents.confirmEmailField), messageProperties)
        && ValidationHelper.validateEmailConfirmInputs(
            formComponents.emailField, formComponents.confirmEmailField, messageProperties)
        && ValidationHelper.validateEmailNotExistsInput(
            formComponents.emailField,
            formComponents.confirmEmailField,
            userService,
            messageProperties);
  }

  private boolean validateRequiredInputs() {
    return ValidationHelper.validateRequiredInputs(requiredFormComponents, messageProperties);
  }

  private void saveUserAndSendEmail() {
    Optional<RoleEntity> userRole = roleService.findByName(RoleType.ROLE_USER);
    if (userRole.isEmpty()) {
      logger.error(
          "Registration aborted: required role ROLE_USER is missing from the role repository.");
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getBaseFailedTitle,
              messageProperties::getRegistrationUnavailableMessage,
              -1,
              NotificationHelper.NotificationType.ERROR);
      return;
    }

    UserEntity userEntity =
        UserEntity.builder()
            .firstName(formComponents.firstNameField.getValue())
            .lastName(formComponents.lastNameField.getValue())
            .email(formComponents.emailField.getValue())
            .password(passwordEncoder.encode(formComponents.passwordField.getValue()))
            .gender(formComponents.genderComboBox.getValue())
            .birthDate(formComponents.birthDatePicker.getValue())
            .enabled(false)
            .preferredLocale(CustomI18NProvider.getCurrentLocale())
            .roleEntities(List.of(userRole.get()))
            .build();

    boolean emailSent;
    try {
      emailSent = registrationService.register(userEntity);
    } catch (IllegalArgumentException failure) {
      // Only handle invalid form values. Configuration/encoder/service
      // errors must not be relabelled as input errors.
      if (validateInputs()) {
        throw failure;
      }
      return;
    } catch (DataIntegrityViolationException failure) {
      if (!EmailConflict.isDuplicate(failure)) {
        throw failure;
      }
      formComponents.emailField.setInvalid(true);
      formComponents.confirmEmailField.setInvalid(true);
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getBaseFailedTitle,
              messageProperties::getBaseValidationEmailExistsMessage,
              -1,
              NotificationHelper.NotificationType.ERROR);
      return;
    } catch (IOException failure) {
      logger.error("Registration mail could not be prepared", failure);
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getBaseFailedTitle,
              messageProperties::getBaseFailedMessage,
              -1,
              NotificationHelper.NotificationType.ERROR);
      return;
    }

    if (emailSent) {
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getBaseSuccessTitle,
              () -> messageProperties.getRegistrationSuccessMessage(userEntity.getEmail()),
              -1,
              NotificationHelper.NotificationType.SUCCESS);
    } else {
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getBaseFailedTitle,
              messageProperties::getBaseFailedMessage,
              -1,
              NotificationHelper.NotificationType.ERROR);
    }
  }

  private List<
          ? extends
              AbstractSinglePropertyField<? extends HasAllowedCharPattern, ? extends Serializable>>
      createRequiredFormComponents() {
    formComponents.firstNameField.setRequiredIndicatorVisible(true);
    formComponents.lastNameField.setRequiredIndicatorVisible(true);
    formComponents.emailField.setRequiredIndicatorVisible(true);
    formComponents.confirmEmailField.setRequiredIndicatorVisible(true);
    formComponents.passwordField.setRequiredIndicatorVisible(true);
    formComponents.confirmPasswordField.setRequiredIndicatorVisible(true);
    formComponents.genderComboBox().setRequiredIndicatorVisible(true);
    formComponents.birthDatePicker.setRequiredIndicatorVisible(true);

    return List.of(
        formComponents.firstNameField,
        formComponents.lastNameField,
        formComponents.emailField,
        formComponents.confirmEmailField,
        formComponents.passwordField,
        formComponents.confirmPasswordField,
        formComponents.genderComboBox,
        formComponents.birthDatePicker);
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
    genderComboBox.setItemLabelGenerator(this::formatGender);
    this.birthDatePicker = new DatePicker(messageProperties.getRegistrationBirthDateField());
    birthDatePicker.setPrefixComponent(VaadinIcon.CALENDAR_USER.create());
    birthDatePicker.setAutoOpen(false);
    birthDatePicker.setPlaceholder(messageProperties.getRegistrationBirthDatePlaceholder());
    birthDatePicker.setClearButtonVisible(true);
    birthDatePicker.setWidthFull();

    return new FormComponents(
        loginLink,
        firstNameField,
        lastNameField,
        emailField,
        confirmEmailField,
        passwordField,
        confirmPasswordField,
        genderComboBox,
        birthDatePicker);
  }

  private String formatGender(Gender gender) {
    if (gender == null) {
      return "-";
    }

    return switch (gender) {
      case MALE -> messageProperties.getGenderMale();
      case FEMALE -> messageProperties.getGenderFemale();
      case OTHER -> messageProperties.getGenderOther();
    };
  }

  private record FormComponents(
      RouterLink loginLink,
      TextField firstNameField,
      TextField lastNameField,
      EmailField emailField,
      EmailField confirmEmailField,
      PasswordField passwordField,
      PasswordField confirmPasswordField,
      ComboBox<Gender> genderComboBox,
      DatePicker birthDatePicker) {}

  private void updateInputPlaceholders() {
    if (firstNameField != null)
      firstNameField.setPlaceholder(messageProperties.getAccountFirstNamePlaceholder());
    if (lastNameField != null)
      lastNameField.setPlaceholder(messageProperties.getAccountLastNamePlaceholder());
    if (emailField != null)
      emailField.setPlaceholder(messageProperties.getAccountEmailPlaceholder());
    if (confirmEmailField != null)
      confirmEmailField.setPlaceholder(messageProperties.getAccountConfirmEmailPlaceholder());
    if (passwordField != null)
      passwordField.setPlaceholder(messageProperties.getAccountPasswordPlaceholder());
    if (confirmPasswordField != null)
      confirmPasswordField.setPlaceholder(messageProperties.getAccountConfirmPasswordPlaceholder());
    if (genderComboBox != null)
      genderComboBox.setPlaceholder(messageProperties.getAccountGenderPlaceholder());
  }
}
