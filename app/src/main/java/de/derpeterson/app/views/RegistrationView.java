package de.derpeterson.app.views;

import static de.derpeterson.app.helper.logging.LogSafety.safeFailure;

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
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.HasAllowedCharPattern;
import com.vaadin.flow.component.textfield.Autocomplete;
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
import de.derpeterson.app.helper.ui.CompactDateInput;
import de.derpeterson.app.helper.ui.ComponentTextUpdateHelper;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.helper.ui.ValidationHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.Gender;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.security.ExternalLoginFlow;
import de.derpeterson.app.security.IsNotAuthenticatedBaseView;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.RegistrationService;
import de.derpeterson.app.service.RoleService;
import de.derpeterson.app.service.UserService;
import de.derpeterson.app.ui.components.AppIcon;
import de.derpeterson.app.ui.components.CosmosCardComponent;
import de.derpeterson.app.ui.components.CosmosComboBoxStyling;
import de.derpeterson.app.ui.components.PublicAuthenticationPanelLayout;
import de.derpeterson.app.ui.components.PublicPageComponent;
import de.derpeterson.app.ui.components.PublicPanelComponent;
import de.derpeterson.app.validation.EmailConflict;
import de.derpeterson.app.validation.UserInputRules;
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
  private static final String COSMOS_FORM_ROW_CLASS = "cosmos-form-row";

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

  private H1 registrationTitleH1 = null;
  private Span existingAccountQuestionSpan = null;
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
      HttpServletRequest httpServletRequest,
      ConfigService configService,
      ExternalLoginFlow externalLoginFlow) {

    super(securityService, httpServletRequest, new HorizontalLayout());

    this.messageProperties = messageProperties;
    this.userService = userService;
    this.roleService = roleService;
    this.passwordEncoder = passwordEncoder;
    this.registrationService = registrationService;

    ComponentUtil.addListener(
        UI.getCurrent(), LanguageChangeEvent.class, this::applyRegistrationPageLanguage);

    getContent().setWidthFull();
    getContent().setPadding(false);
    setSpacing(false);
    setAlignItems(FlexComponent.Alignment.CENTER);
    setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

    getContent().removeAll();

    formComponents = createFormComponents();
    requiredFormComponents = createRequiredFormComponents();
    Button submitRegistrationButton = buildRegistrationButton();

    PublicPageComponent registrationPageComponent =
        new PublicPageComponent(
            configService, messageProperties, PublicPageComponent.Page.REGISTER);
    VerticalLayout registrationContentLayout = registrationPageComponent.getCardContainer();

    this.registrationTitleH1 = new H1(messageProperties.getRegistrationTitle());
    registrationTitleH1.addClassName("cosmos-headline");

    this.existingAccountQuestionSpan = new Span(messageProperties.getRegistrationLoginQuestion());

    HorizontalLayout existingAccountLinkLayout = new HorizontalLayout();
    existingAccountLinkLayout.addClassNames("cosmos-intro", "cosmos-form-text");
    existingAccountLinkLayout.setWidthFull();
    existingAccountLinkLayout.setSpacing(false);
    existingAccountLinkLayout.setPadding(false);
    existingAccountLinkLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
    existingAccountLinkLayout.addClassNames(LumoUtility.Gap.SMALL, "cosmos-form-links");
    existingAccountLinkLayout.add(existingAccountQuestionSpan);
    existingAccountLinkLayout.add(formComponents.loginLink);

    HorizontalLayout nameFieldsLayout =
        new HorizontalLayout(formComponents.firstNameField, formComponents.lastNameField);
    nameFieldsLayout.setWidthFull();
    nameFieldsLayout.setSpacing(false);
    nameFieldsLayout.addClassName(COSMOS_FORM_ROW_CLASS);

    HorizontalLayout emailFieldsLayout =
        new HorizontalLayout(formComponents.emailField, formComponents.confirmEmailField);
    emailFieldsLayout.setWidthFull();
    emailFieldsLayout.setSpacing(false);
    emailFieldsLayout.addClassName(COSMOS_FORM_ROW_CLASS);

    HorizontalLayout passwordFieldsLayout =
        new HorizontalLayout(formComponents.passwordField, formComponents.confirmPasswordField);
    passwordFieldsLayout.setWidthFull();
    passwordFieldsLayout.setSpacing(false);
    passwordFieldsLayout.addClassName(COSMOS_FORM_ROW_CLASS);

    HorizontalLayout personalDetailsLayout =
        new HorizontalLayout(formComponents.genderComboBox, formComponents.birthDatePicker);
    personalDetailsLayout.setWidthFull();
    personalDetailsLayout.setSpacing(false);
    personalDetailsLayout.addClassName(COSMOS_FORM_ROW_CLASS);

    CosmosCardComponent registrationCardComponent =
        new PublicPanelComponent(
            AppIcon.USER_PLUS.create(),
            registrationTitleH1,
            messageProperties::getPublicRegisterSubtitle,
            new Component[] {
              nameFieldsLayout, emailFieldsLayout, passwordFieldsLayout, personalDetailsLayout
            },
            existingAccountLinkLayout,
            submitRegistrationButton);

    PublicAuthenticationPanelLayout registrationPanels =
        new PublicAuthenticationPanelLayout(
            registrationCardComponent,
            externalLoginFlow,
            httpServletRequest,
            messageProperties,
            () -> false);
    registrationPanels.addClassName("public-auth-panels--register");
    registrationContentLayout.add(registrationPanels);

    updateRegistrationFieldPlaceholders();
    add(registrationPageComponent);
  }

  private void applyRegistrationPageLanguage(LanguageChangeEvent languageChangeEvent) {
    VaadinSession.getCurrent().setLocale(languageChangeEvent.getNewLocale());

    Map<Component, Supplier<String>> textSuppliersByComponent = new HashMap<>();
    Optional.ofNullable(registrationTitleH1)
        .ifPresent(translatedComponent ->
            textSuppliersByComponent.put(
                translatedComponent, this.messageProperties::getRegistrationTitle));
    Optional.ofNullable(existingAccountQuestionSpan)
        .ifPresent(translatedComponent ->
            textSuppliersByComponent.put(
                translatedComponent, this.messageProperties::getRegistrationLoginQuestion));
    Optional.ofNullable(loginLink)
        .ifPresent(translatedComponent ->
            textSuppliersByComponent.put(
                translatedComponent, this.messageProperties::getRegistrationLoginLink));

    Optional.ofNullable(firstNameField)
        .ifPresent(translatedComponent ->
            textSuppliersByComponent.put(
                translatedComponent, this.messageProperties::getRegistrationFirstNameField));
    Optional.ofNullable(lastNameField)
        .ifPresent(translatedComponent ->
            textSuppliersByComponent.put(
                translatedComponent, this.messageProperties::getRegistrationLastNameField));

    Optional.ofNullable(emailField)
        .ifPresent(translatedComponent ->
            textSuppliersByComponent.put(
                translatedComponent, this.messageProperties::getPublicEmailField));
    Optional.ofNullable(confirmEmailField)
        .ifPresent(translatedComponent ->
            textSuppliersByComponent.put(
                translatedComponent, this.messageProperties::getPublicConfirmEmailField));
    Optional.ofNullable(passwordField)
        .ifPresent(translatedComponent ->
            textSuppliersByComponent.put(
                translatedComponent, this.messageProperties::getRegistrationPasswordField));
    Optional.ofNullable(confirmPasswordField)
        .ifPresent(translatedComponent ->
            textSuppliersByComponent.put(
                translatedComponent, this.messageProperties::getPublicConfirmPasswordField));

    Optional.ofNullable(genderComboBox)
        .ifPresent(translatedComponent ->
            textSuppliersByComponent.put(
                translatedComponent, this.messageProperties::getRegistrationGenderCombobox));

    Optional.ofNullable(birthDatePicker)
        .ifPresent(translatedComponent ->
            textSuppliersByComponent.put(
                translatedComponent, this.messageProperties::getRegistrationBirthDateField));
    Optional.ofNullable(registrationButton)
        .ifPresent(translatedComponent ->
            textSuppliersByComponent.put(
                translatedComponent, this.messageProperties::getBaseRegistrationButton));

    ComponentTextUpdateHelper.updateComponentTexts(textSuppliersByComponent);
    updateRegistrationFieldPlaceholders();

    Optional.ofNullable(birthDatePicker)
        .ifPresent(registrationBirthDatePicker -> {
          registrationBirthDatePicker.setLocale(languageChangeEvent.getNewLocale());
          CompactDateInput.configureDatePickerInput(registrationBirthDatePicker, messageProperties);
          registrationBirthDatePicker.setPlaceholder(
              this.messageProperties.getRegistrationBirthDatePlaceholder());
        });

    Optional.ofNullable(genderComboBox)
        .ifPresent(registrationGenderComboBox -> {
          registrationGenderComboBox.setItemLabelGenerator(this::formatGenderLabel);
          registrationGenderComboBox.getDataProvider().refreshAll();
        });

    NotificationHelper.getInstance().updateText();
  }

  private Button buildRegistrationButton() {
    this.registrationButton = new Button(messageProperties.getBaseRegistrationButton());
    registrationButton.setPrefixComponent(AppIcon.USER_PLUS.create());
    registrationButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    registrationButton.setWidthFull();
    registrationButton.addClassName("cosmos-submit");
    registrationButton.addClickListener(_ -> submitRegistrationForm());
    return registrationButton;
  }

  private void submitRegistrationForm() {
    if (!validateRegistrationInputs()) {
      return;
    }

    registerAccountAndRequestVerificationEmail();
  }

  private boolean validateRegistrationInputs() {
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

  private void registerAccountAndRequestVerificationEmail() {
    Optional<RoleEntity> portalUserRole = roleService.findByName(RoleType.ROLE_USER);
    if (portalUserRole.isEmpty()) {
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
            .roleEntities(List.of(portalUserRole.get()))
            .build();

    boolean wasVerificationEmailQueued;
    try {
      wasVerificationEmailQueued = registrationService.register(userEntity);
    } catch (IllegalArgumentException failure) {
      // Only handle invalid form values. Configuration/encoder/service
      // errors must not be relabelled as input errors.
      if (validateRegistrationInputs()) {
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
      logger.error("Registration mail could not be prepared", safeFailure(failure));
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getBaseFailedTitle,
              messageProperties::getBaseFailedMessage,
              -1,
              NotificationHelper.NotificationType.ERROR);
      return;
    }

    if (wasVerificationEmailQueued) {
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

    return List.of(
        formComponents.firstNameField,
        formComponents.lastNameField,
        formComponents.emailField,
        formComponents.confirmEmailField,
        formComponents.passwordField,
        formComponents.confirmPasswordField);
  }

  private FormComponents createFormComponents() {
    this.loginLink = new RouterLink(messageProperties.getRegistrationLoginLink(), LoginView.class);
    loginLink.addClassNames("underline");

    this.firstNameField = new TextField(messageProperties.getRegistrationFirstNameField());
    firstNameField.setClearButtonVisible(true);
    firstNameField.setWidthFull();

    this.lastNameField = new TextField(messageProperties.getRegistrationLastNameField());
    lastNameField.setClearButtonVisible(true);
    lastNameField.setWidthFull();

    this.emailField = new EmailField(messageProperties.getPublicEmailField());
    emailField.setAutocomplete(Autocomplete.EMAIL);
    emailField.setPattern(UserInputRules.EMAIL_REGEX);
    emailField.setClearButtonVisible(true);
    emailField.setWidthFull();

    this.confirmEmailField = new EmailField(messageProperties.getPublicConfirmEmailField());
    confirmEmailField.setAutocomplete(Autocomplete.EMAIL);
    confirmEmailField.setPattern(UserInputRules.EMAIL_REGEX);
    confirmEmailField.setClearButtonVisible(true);
    confirmEmailField.setWidthFull();

    this.passwordField = new PasswordField(messageProperties.getRegistrationPasswordField());
    passwordField.setAutocomplete(Autocomplete.NEW_PASSWORD);
    passwordField.setClearButtonVisible(true);
    passwordField.setWidthFull();

    this.confirmPasswordField =
        new PasswordField(messageProperties.getPublicConfirmPasswordField());
    confirmPasswordField.setAutocomplete(Autocomplete.NEW_PASSWORD);
    confirmPasswordField.setClearButtonVisible(true);
    confirmPasswordField.setWidthFull();

    this.genderComboBox = new ComboBox<>(messageProperties.getRegistrationGenderCombobox());
    genderComboBox.setClearButtonVisible(true);
    genderComboBox.setWidthFull();
    genderComboBox.setItems(Gender.MALE, Gender.FEMALE, Gender.OTHER);
    genderComboBox.setItemLabelGenerator(this::formatGenderLabel);
    CosmosComboBoxStyling.styleChoice(genderComboBox, this::formatGenderLabel, false);

    this.birthDatePicker = new DatePicker(messageProperties.getRegistrationBirthDateField());
    birthDatePicker.setAutoOpen(false);
    birthDatePicker.setLocale(CustomI18NProvider.getCurrentLocale());
    CompactDateInput.configureDatePickerInput(birthDatePicker, messageProperties);
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

  private String formatGenderLabel(Gender gender) {
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

  private void updateRegistrationFieldPlaceholders() {
    if (firstNameField != null) {
      firstNameField.setPlaceholder(messageProperties.getAccountFirstNamePlaceholder());
    }
    if (lastNameField != null) {
      lastNameField.setPlaceholder(messageProperties.getAccountLastNamePlaceholder());
    }
    if (emailField != null) {
      emailField.setPlaceholder(messageProperties.getAccountEmailPlaceholder());
    }
    if (confirmEmailField != null) {
      confirmEmailField.setPlaceholder(messageProperties.getAccountConfirmEmailPlaceholder());
    }
    if (passwordField != null) {
      passwordField.setPlaceholder(messageProperties.getAccountPasswordPlaceholder());
    }
    if (confirmPasswordField != null) {
      confirmPasswordField.setPlaceholder(messageProperties.getAccountConfirmPasswordPlaceholder());
    }
    if (genderComboBox != null) {
      genderComboBox.setPlaceholder(messageProperties.getAccountGenderPlaceholder());
    }
    if (birthDatePicker != null) {
      birthDatePicker.setHelperText(messageProperties.getAccountBirthDateFormatHint());
    }
  }
}
