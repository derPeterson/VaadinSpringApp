package de.derpeterson.app.views.admin;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import de.derpeterson.app.config.AppConstants;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.helper.ui.ValidationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.Gender;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.RoleService;
import de.derpeterson.app.service.UserService;
import de.derpeterson.app.ui.components.CosmosCardComponent;
import de.derpeterson.app.ui.components.CosmosSectionHeaderComponent;
import de.derpeterson.app.validation.EmailConflict;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
public class AdminUserManagementSection extends VerticalLayout {

    private static final DateTimeFormatter LAST_ACTIVITY_FORMATTER =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);

    private final transient MessageProperties messageProperties;
    private final transient UserService userService;
    private final transient RoleService roleService;
    private final transient SecurityService securityService;

    private final H1 title;
    private final Span description;
    private final Button addUserButton;
    private final TextField searchField;
    private final Button refreshButton;
    private final Grid<UserEntity> userGrid = new Grid<>(UserEntity.class, false);

    private final ComboBox<Boolean> enabledFilter = new ComboBox<>();
    private final ComboBox<UserStatus> statusFilter = new ComboBox<>();
    private final ComboBox<RoleType> roleFilter = new ComboBox<>();
    private final AdminUserActivityFilter lastActivityFilter = new AdminUserActivityFilter();
    private final AdminUserPagination pagination;
    private final AdminUserFilterBar filterBar;

    private Grid.Column<UserEntity> nameColumn;
    private Grid.Column<UserEntity> emailColumn;
    private Grid.Column<UserEntity> enabledColumn;
    private Grid.Column<UserEntity> statusColumn;
    private Grid.Column<UserEntity> rolesColumn;
    private Grid.Column<UserEntity> lastActivityColumn;
    private Grid.Column<UserEntity> actionsColumn;
    private final Span actionsHeader = new Span();

    private List<UserEntity> users = new ArrayList<>();
    private List<UserEntity> filteredUsers = new ArrayList<>();
    private final List<Runnable> dialogErrorRefreshers = new ArrayList<>();

    private int currentPage = 0;
    private int pageSize = 10;

    public AdminUserManagementSection(MessageProperties messageProperties,
                                      UserService userService,
                                      RoleService roleService,
                                      SecurityService securityService) {
        this.messageProperties = messageProperties;
        this.userService = userService;
        this.roleService = roleService;
        this.securityService = securityService;

        setPadding(false);
        setSpacing(false);
        setWidthFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        addClassNames("cosmos-admin-section", "cosmos-admin-section--users");

        title = new H1();


        description = new Span();
        description.addClassNames(LumoUtility.TextColor.SECONDARY);

        addUserButton = new Button();
        addUserButton.setPrefixComponent(VaadinIcon.PLUS.create());
        addUserButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addUserButton.addClickListener(e -> openDialog(new UserEntity()));

        searchField = new TextField();
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.setWidthFull();
        searchField.setMaxWidth("360px");
        searchField.setValueChangeMode(ValueChangeMode.EAGER);
        searchField.addValueChangeListener(event -> applyGridFilter());

        refreshButton = new Button(VaadinIcon.REFRESH.create(), event -> refreshGrid());
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        pagination = new AdminUserPagination(messageProperties, page -> {
            currentPage = page;
            updateGridPage();
        }, size -> {
            pageSize = size;
            currentPage = 0;
            updateGridPage();
        });
        configureFilters();
        configureGrid();

        filterBar = new AdminUserFilterBar(messageProperties, searchField, refreshButton, addUserButton,
                enabledFilter, statusFilter, roleFilter, lastActivityFilter);

        VerticalLayout layout = new VerticalLayout(
                filterBar,
                userGrid,
                pagination
        );
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.addClassName("cosmos-admin-users-content");
        layout.setWidthFull();
        layout.setAlignItems(Alignment.START);

        CosmosCardComponent card = new CosmosCardComponent(layout);
        card.addClassName("cosmos-admin-users-card");
        add(new CosmosSectionHeaderComponent(title, description), card);

        refreshTexts();
        refreshGrid();
    }

    public void refreshTexts() {
        title.setText(messageProperties.getAdminUsersTitle());
        description.setText(messageProperties.getAdminUsersDescription());
        addUserButton.setText(messageProperties.getAdminUsersAdd());

        searchField.setLabel(null);
        searchField.setAriaLabel(messageProperties.getAdminUsersSearchLabel());
        searchField.setPlaceholder(messageProperties.getAdminUsersSearchPlaceholder());

        refreshButton.setTooltipText(messageProperties.getAdminUsersRefreshTooltip());
        refreshButton.setAriaLabel(messageProperties.getAdminUsersRefreshTooltip());
        enabledFilter.setLabel(messageProperties.getAdminUsersGridEnabled());
        enabledFilter.setPlaceholder(messageProperties.getAdminUsersFilterAll());
        enabledFilter.setItemLabelGenerator(enabled -> enabled
                ? messageProperties.getAdminUsersYes() : messageProperties.getAdminUsersNo());
        statusFilter.setLabel(messageProperties.getAdminUsersGridStatus());
        statusFilter.setPlaceholder(messageProperties.getAdminUsersFilterAll());
        statusFilter.setItemLabelGenerator(messageProperties::getUserStatusLabel);
        roleFilter.setLabel(messageProperties.getAdminUsersFilterRole());
        roleFilter.setPlaceholder(messageProperties.getAdminUsersFilterAll());
        roleFilter.setItemLabelGenerator(this::formatRoleType);
        lastActivityFilter.setLabel(messageProperties.getAdminUsersFilterActiveSince());
        lastActivityFilter.getElement().setAttribute("title", messageProperties.getAdminUsersFilterActiveSinceHelp());
        lastActivityFilter.setErrorMessage(messageProperties.getAdminUsersFilterActiveSinceError());
        Locale locale = VaadinSession.getCurrent() != null
                ? VaadinSession.getCurrent().getLocale() : Locale.getDefault();
        lastActivityFilter.setLocale(locale);
        lastActivityFilter.refreshTexts(messageProperties);
        pagination.refreshTexts();
        filterBar.refreshTexts();

        if (nameColumn != null) {
            nameColumn.setHeader(messageProperties.getAdminUsersGridName());
        }
        if (emailColumn != null) {
            emailColumn.setHeader(messageProperties.getAdminUsersGridEmail());
        }
        if (enabledColumn != null) {
            enabledColumn.setHeader(messageProperties.getAdminUsersGridEnabled());
        }
        if (statusColumn != null) {
            statusColumn.setHeader(messageProperties.getAdminUsersGridStatus());
        }
        if (rolesColumn != null) {
            rolesColumn.setHeader(messageProperties.getAdminUsersGridRoles());
        }
        if (lastActivityColumn != null) {
            lastActivityColumn.setHeader(messageProperties.getAdminUsersGridLastActivity());
        }
        if (actionsColumn != null) {
            actionsHeader.setText(messageProperties.getAdminUsersGridActions());
        }

        userGrid.setEmptyStateText(messageProperties.getAdminUsersGridEmptyState());

        updateGridPage();
        List.copyOf(dialogErrorRefreshers).forEach(Runnable::run);
    }

    private void configureGrid() {
        nameColumn = userGrid.addComponentColumn(this::createUserIdentity)
                .setHeader(messageProperties.getAdminUsersGridName())
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(Comparator.comparing(this::getFullName, String.CASE_INSENSITIVE_ORDER));

        emailColumn = userGrid.addColumn(UserEntity::getEmail)
                .setHeader(messageProperties.getAdminUsersGridEmail())
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(Comparator.comparing(
                        user -> normalize(user.getEmail()),
                        String.CASE_INSENSITIVE_ORDER
                ))
                .setFlexGrow(1);

        enabledColumn = userGrid.addComponentColumn(this::createEnabledBadge)
                .setHeader(messageProperties.getAdminUsersGridEnabled())
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(Comparator.comparing(UserEntity::isEnabled));

        statusColumn = userGrid.addComponentColumn(this::createStatusBadge)
                .setHeader(messageProperties.getAdminUsersGridStatus())
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(Comparator.comparing(
                        user -> Optional.ofNullable(user.getStatus())
                                .map(status -> messageProperties.getUserStatusLabel(status))
                                .orElse(""),
                        String.CASE_INSENSITIVE_ORDER
                ));

        rolesColumn = userGrid.addColumn(this::formatRoleNames)
                .setHeader(messageProperties.getAdminUsersGridRoles())
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(Comparator.comparing(this::formatRoleNames, String.CASE_INSENSITIVE_ORDER))
                .setAutoWidth(false)
                .setWidth("140px")
                .setFlexGrow(0);

        lastActivityColumn = userGrid.addComponentColumn(this::createLastActivity)
                .setHeader(messageProperties.getAdminUsersGridLastActivity())
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(Comparator.comparing(
                        UserEntity::getLastActivity,
                        Comparator.nullsLast(LocalDateTime::compareTo)
                ));

        actionsHeader.setText(messageProperties.getAdminUsersGridActions());
        actionsHeader.addClassName("admin-users-actions-header");
        actionsColumn = userGrid.addComponentColumn(this::createActionButtons)
                .setHeader(actionsHeader)
                .setTextAlign(ColumnTextAlign.CENTER)
                .setAutoWidth(true)
                .setFlexGrow(0);

        userGrid.addSortListener(event -> {
            currentPage = 0;
            updateGridPage();
        });
        userGrid.setWidthFull();
        userGrid.setAllRowsVisible(true);
        userGrid.setEmptyStateText(messageProperties.getAdminUsersGridEmptyState());
        userGrid.sort(List.of(new GridSortOrder<>(
                nameColumn,
                com.vaadin.flow.data.provider.SortDirection.ASCENDING
        )));

        userGrid.addClassName("admin-users-grid");
        userGrid.addThemeVariants(GridVariant.ROW_STRIPES);
    }

    private void configureFilters() {
        enabledFilter.setItems(true, false);
        statusFilter.setItems(UserStatus.values());
        roleFilter.setItems(RoleType.values());
        for (ComboBox<?> filter : List.of(enabledFilter, statusFilter, roleFilter)) {
            filter.setClearButtonVisible(true);
            filter.setAllowCustomValue(false);
            filter.addClassName("admin-user-select-filter");
            filter.addValueChangeListener(event -> applyGridFilter());
        }
        lastActivityFilter.addClassName("admin-user-activity-filter");
        lastActivityFilter.addValueChangeListener(event -> applyGridFilter());
    }

    private int getTotalPages() {
        if (filteredUsers.isEmpty()) {
            return 1;
        }
        return (int) Math.ceil((double) filteredUsers.size() / pageSize);
    }

    private void refreshGrid() {
        users = userService.findAllUsers().stream()
                .sorted(Comparator.comparing(this::getFullName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        currentPage = 0;
        applyGridFilter();
    }

    private void applyGridFilter() {
        AdminUserFilters filters = new AdminUserFilters(searchField.getValue(), enabledFilter.getValue(),
                statusFilter.getValue(), roleFilter.getValue(), lastActivityFilter.getValue());
        filteredUsers = users.stream().filter(filters::matches).collect(Collectors.toList());

        currentPage = 0;
        updateGridPage();
    }

    private void updateGridPage() {
        int totalPages = getTotalPages();

        if (currentPage >= totalPages) {
            currentPage = Math.max(0, totalPages - 1);
        }

        int fromIndex = Math.min(currentPage * pageSize, filteredUsers.size());
        int toIndex = Math.min(fromIndex + pageSize, filteredUsers.size());

        Comparator<UserEntity> comparator = userGrid.getSortOrder().stream()
                .map(order -> (Comparator<UserEntity>) order.getSorted().getComparator(order.getDirection()))
                .reduce((first, next) -> first.thenComparing(next))
                .orElse((left, right) -> 0);
        List<UserEntity> sortedUsers = filteredUsers.stream().sorted(comparator).toList();
        userGrid.setItems(sortedUsers.subList(fromIndex, toIndex));
        pagination.showPage(currentPage, filteredUsers.size(), pageSize);
    }

    private void deleteUser(UserEntity user) {
        deleteUser(user, () -> { });
    }

    private void deleteUser(UserEntity user, Runnable afterDelete) {
        try {
            if (isCurrentUser(user)) {
                showError(messageProperties::getAdminUsersErrorDeleteOwnUser);
                return;
            }

            if (!userService.canDeleteUser(user)) {
                showError(messageProperties::getAdminUsersErrorDeleteLastAdmin);
                return;
            }
        } catch (RuntimeException exception) {
            showMutationError(exception, true);
            return;
        }

        Dialog dialog = new Dialog();
        dialog.addClassName("cosmos-theme");
        dialog.getElement().getThemeList().add("cosmos");
        dialog.setHeaderTitle(messageProperties.getAdminUsersDeleteDialogTitle());
        dialog.addClassName("admin-user-delete-dialog");
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(true);

        Span warningText = new Span(messageProperties.getAdminUsersDeleteDialogWarning());
        warningText.addClassNames(LumoUtility.FontWeight.SEMIBOLD);

        Span userText = new Span(getFullName(user) + " <" + Optional.ofNullable(user.getEmail()).orElse("-") + ">");
        userText.addClassName(LumoUtility.FontWeight.BOLD);

        Button cancelButton = new Button(messageProperties.getAdminUsersCancel(), event -> dialog.close());
        Button confirmButton = new Button(messageProperties.getAdminUsersDelete(), event -> {
            try {
                userService.deleteUser(user);
                dialog.close();
                afterDelete.run();
                refreshGrid();
                NotificationHelper.getInstance().showNotification(
                        messageProperties::getBaseSuccessTitle,
                        messageProperties::getAdminUsersDeleteSuccess,
                        NotificationHelper.NotificationType.SUCCESS
                );
            } catch (RuntimeException exception) {
                showMutationError(exception, true);
            }
        });
        confirmButton.setPrefixComponent(VaadinIcon.TRASH.create());
        confirmButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
        confirmButton.addClassName("admin-user-delete-confirm");

        HorizontalLayout footer = new HorizontalLayout(cancelButton, confirmButton);
        footer.addClassName("cosmos-dialog-actions");
        footer.setWidthFull();
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        dialog.getFooter().add(footer);
        VerticalLayout dialogLayout = new VerticalLayout(warningText, userText);
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(false);
        dialogLayout.addClassName("cosmos-dialog-form");

        dialog.add(dialogLayout);
        dialog.open();
    }

    private void openDialog(UserEntity user) {
        boolean editMode = user.getId() != null;
        AdminUserFormData formData = AdminUserFormData.fromUser(user);
        Long expectedVersion = user.getVersion();

        Dialog dialog = new Dialog();
        dialog.addClassName("cosmos-theme");
        dialog.getElement().getThemeList().add("cosmos");
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(false);
        dialog.setDraggable(true);
        dialog.setResizable(true);
        dialog.addClassName("admin-user-dialog");
        dialog.setHeaderTitle(editMode
                ? messageProperties.getAdminUsersEditDialogTitle()
                : messageProperties.getAdminUsersCreateDialogTitle());

        Binder<AdminUserFormData> binder = new Binder<>(AdminUserFormData.class);

        TextField firstNameField = new TextField(messageProperties.getAdminUsersFieldFirstName());
        firstNameField.setRequiredIndicatorVisible(true);
        firstNameField.setWidthFull();

        TextField lastNameField = new TextField(messageProperties.getAdminUsersFieldLastName());
        lastNameField.setRequiredIndicatorVisible(true);
        lastNameField.setWidthFull();

        EmailField emailField = new EmailField(messageProperties.getAdminUsersFieldEmail());
        emailField.setRequiredIndicatorVisible(true);
        emailField.setWidthFull();
        emailField.setClearButtonVisible(true);

        PasswordField passwordField = new PasswordField(messageProperties.getAdminUsersFieldPassword());
        passwordField.setWidthFull();
        passwordField.setRevealButtonVisible(true);
        passwordField.setRequiredIndicatorVisible(!editMode);
        passwordField.setHelperText(editMode
                ? messageProperties.getAdminUsersPasswordHelperEdit()
                : messageProperties.getAdminUsersPasswordHelperCreate());

        ComboBox<Gender> genderComboBox = new ComboBox<>(messageProperties.getRegistrationGenderCombobox());
        genderComboBox.setItems(Gender.values());
        genderComboBox.setWidthFull();
        genderComboBox.setRequired(true);
        genderComboBox.setItemLabelGenerator(this::formatGender);

        DatePicker birthDatePicker = new DatePicker(messageProperties.getRegistrationBirthDateField());
        birthDatePicker.setAutoOpen(false);
        Runnable refreshBirthDate = () -> {
            birthDatePicker.setLocale(VaadinSession.getCurrent() != null
                    ? VaadinSession.getCurrent().getLocale() : Locale.getDefault());
            birthDatePicker.setLabel(messageProperties.getRegistrationBirthDateField());
            birthDatePicker.setPlaceholder(messageProperties.getDateInputPlaceholder());
        };
        refreshBirthDate.run();
        birthDatePicker.setWidthFull();
        birthDatePicker.setRequired(true);
        birthDatePicker.setMax(LocalDate.now());

        Checkbox enabledCheckbox = new Checkbox(messageProperties.getAdminUsersFieldEnabled());

        CheckboxGroup<RoleType> rolesGroup = new CheckboxGroup<>();
        rolesGroup.setLabel(messageProperties.getAdminUsersFieldRoles());
        rolesGroup.setItems(RoleType.values());
        rolesGroup.setItemLabelGenerator(this::formatRoleType);
        rolesGroup.setRequired(true);
        rolesGroup.setHelperText(messageProperties.getAdminUsersRolesHelper());

        Span technicalInfo = new Span(editMode
                ? messageProperties.getAdminUsersTechnicalInfoEdit()
                : messageProperties.getAdminUsersTechnicalInfoCreate());
        technicalInfo.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

        binder.forField(firstNameField)
                .asRequired(context -> messageProperties.getBaseValidationRequiredMessage())
                .withValidator(ValidationHelper::isRequiredTextValid,
                        context -> messageProperties.getBaseValidationRequiredMessage())
                .bind(AdminUserFormData::getFirstName, AdminUserFormData::setFirstName);

        binder.forField(lastNameField)
                .asRequired(context -> messageProperties.getBaseValidationRequiredMessage())
                .withValidator(ValidationHelper::isRequiredTextValid,
                        context -> messageProperties.getBaseValidationRequiredMessage())
                .bind(AdminUserFormData::getLastName, AdminUserFormData::setLastName);

        AtomicBoolean emailCheckFailed = new AtomicBoolean();
        binder.forField(emailField)
                .asRequired(context -> messageProperties.getBaseValidationRequiredMessage())
                .withValidator(ValidationHelper::isEmailValid,
                        context -> messageProperties.getBaseValidationEmailInvalidMessage())
                .withValidator((email, context) -> {
                    emailCheckFailed.set(false);
                    try {
                        return userService.emailExistsForOtherUser(ValidationHelper.normalize(email), formData.getId())
                                ? ValidationResult.error(messageProperties.getBaseValidationEmailExistsMessage())
                                : ValidationResult.ok();
                    } catch (RuntimeException exception) {
                        log.error("E-Mail-Vorprüfung fehlgeschlagen", exception);
                        emailCheckFailed.set(true);
                        return ValidationResult.error(messageProperties.getAdminUsersErrorSaveFailed());
                    }
                })
                .bind(AdminUserFormData::getEmail, AdminUserFormData::setEmail);

        binder.forField(passwordField)
                .withValidator(value -> ValidationHelper.isPasswordSecure(value, editMode),
                        context -> editMode
                                ? messageProperties.getAdminUsersPasswordValidationEdit()
                                : messageProperties.getAdminUsersPasswordValidationCreate())
                .bind(AdminUserFormData::getPassword, AdminUserFormData::setPassword);

        binder.forField(genderComboBox)
                .asRequired(context -> messageProperties.getBaseValidationRequiredMessage())
                .bind(AdminUserFormData::getGender, AdminUserFormData::setGender);

        binder.forField(birthDatePicker)
                .asRequired(context -> messageProperties.getBaseValidationRequiredMessage())
                .withValidator(ValidationHelper::isBirthDateValid,
                        context -> messageProperties.getBaseValidationBirthDatePastMessage())
                .bind(AdminUserFormData::getBirthDate, AdminUserFormData::setBirthDate);

        binder.forField(enabledCheckbox)
                .bind(AdminUserFormData::isEnabled, AdminUserFormData::setEnabled);

        binder.forField(rolesGroup)
                .withValidator(selectedRoles -> selectedRoles != null && !selectedRoles.isEmpty(),
                        context -> messageProperties.getBaseValidationRequiredMessage())
                .bind(AdminUserFormData::getRoles, AdminUserFormData::setRoles);

        binder.readBean(formData);

        Button saveButton = new Button(messageProperties.getAdminUsersSave());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.setEnabled(binder.isValid());

        binder.addStatusChangeListener(event -> saveButton.setEnabled(binder.isValid()));

        saveButton.addClickListener(event -> {
            try {
                try {
                    emailCheckFailed.set(false);
                    binder.writeBean(formData);
                } catch (ValidationException exception) {
                    showError(emailCheckFailed.get() ? messageProperties::getAdminUsersErrorSaveFailed
                            : messageProperties::getAdminUsersValidationCheckFields);
                    return;
                }
                // Status listeners may recheck validity during writeBean. Never
                // proceed if such a check failed technically either.
                if (emailCheckFailed.get()) {
                    showError(messageProperties::getAdminUsersErrorSaveFailed);
                    return;
                }

                Set<RoleEntity> roleEntities = resolveRoles(formData.getRoles());
                if (roleEntities.size() != formData.getRoles().size()) {
                    showError(messageProperties::getAdminUsersErrorRoleMissing);
                    return;
                }

                if (isCurrentUser(user) && !formData.isEnabled()) {
                    showError(messageProperties::getAdminUsersErrorDisableOwnUser);
                    return;
                }

                if (isCurrentUser(user) && !formData.getRoles().contains(RoleType.ROLE_ADMIN)) {
                    showError(messageProperties::getAdminUsersErrorRemoveOwnAdminRole);
                    return;
                }

                if (userService.wouldRemoveLastEnabledAdmin(formData.getId(), formData.isEnabled(), roleEntities)) {
                    showError(messageProperties::getAdminUsersErrorLastActiveAdmin);
                    return;
                }

                UserEntity edited = UserEntity.builder()
                        .id(formData.getId()).firstName(clean(formData.getFirstName()))
                        .lastName(clean(formData.getLastName())).email(clean(formData.getEmail()))
                        .gender(formData.getGender()).birthDate(formData.getBirthDate())
                        .enabled(formData.isEnabled()).verificationPending(false).roleEntities(roleEntities).build();
                if (editMode) {
                    userService.updateAdminUser(edited, expectedVersion, formData.getPassword());
                } else {
                    userService.updatePassword(edited, formData.getPassword());
                    userService.saveUser(edited);
                }
                dialog.close();
                refreshGrid();

                NotificationHelper.getInstance().showNotification(
                        messageProperties::getBaseSuccessTitle,
                        () -> editMode
                                ? messageProperties.getAdminUsersSaveSuccessUpdated()
                                : messageProperties.getAdminUsersSaveSuccessCreated(),
                        NotificationHelper.NotificationType.SUCCESS
                );
            } catch (RuntimeException exception) {
                showMutationError(exception, false);
            }
        });

        Button cancelButton = new Button(messageProperties.getAdminUsersCancel(), e -> dialog.close());

        HorizontalLayout nameRow = new HorizontalLayout(firstNameField, lastNameField);
        nameRow.setWidthFull();
        nameRow.addClassName("cosmos-form-row");
        nameRow.expand(firstNameField, lastNameField);

        HorizontalLayout masterDataRow = new HorizontalLayout(genderComboBox, birthDatePicker);
        masterDataRow.setWidthFull();
        masterDataRow.addClassName("cosmos-form-row");
        masterDataRow.expand(genderComboBox, birthDatePicker);

        HorizontalLayout footer = new HorizontalLayout();
        footer.addClassName("cosmos-dialog-actions");
        footer.setWidthFull();
        footer.setAlignItems(Alignment.CENTER);
        if (editMode) {
            Button deleteButton = new Button(messageProperties.getAdminUsersDelete(),
                    VaadinIcon.TRASH.create(), event -> deleteUser(user, dialog::close));
            deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
            deleteButton.setEnabled(!isCurrentUser(user));
            footer.add(deleteButton);
        }
        cancelButton.addClassName("cosmos-dialog-cancel");
        footer.add(cancelButton, saveButton);

        VerticalLayout dialogLayout = new VerticalLayout(
                nameRow,
                emailField,
                passwordField,
                masterDataRow,
                enabledCheckbox,
                rolesGroup,
                technicalInfo
        );
        dialog.getFooter().add(footer);
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(false);
        dialogLayout.addClassName("cosmos-dialog-form");

        dialog.add(dialogLayout);
        Runnable refreshErrors = () -> {
            refreshBirthDate.run();
            if (binder.getFields().anyMatch(field -> field instanceof com.vaadin.flow.component.shared.HasValidationProperties validation && validation.isInvalid())) {
                binder.validate();
            }
        };
        dialogErrorRefreshers.add(refreshErrors);
        dialog.addOpenedChangeListener(event -> {
            if (!event.isOpened()) {
                dialogErrorRefreshers.remove(refreshErrors);
            }
        });
        dialog.addDetachListener(event -> dialogErrorRefreshers.remove(refreshErrors));
        dialog.open();
    }

    private Set<RoleEntity> resolveRoles(Set<RoleType> selectedRoles) {
        return Optional.ofNullable(selectedRoles)
                .orElseGet(Set::of)
                .stream()
                .map(roleService::findByName)
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());
    }


    private Component createActionButtons(UserEntity user) {
        Button editButton = new Button(VaadinIcon.EDIT.create(), e -> openDialog(user));
        editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        editButton.setTooltipText(messageProperties.getAdminUsersEdit());

        Button deleteButton = new Button(VaadinIcon.TRASH.create(), e -> deleteUser(user));
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        deleteButton.setTooltipText(messageProperties.getAdminUsersDelete());
        deleteButton.setEnabled(!isCurrentUser(user));

        HorizontalLayout actions = new HorizontalLayout(editButton, deleteButton);
        actions.setPadding(false);
        actions.setSpacing(true);
        actions.setJustifyContentMode(JustifyContentMode.CENTER);
        return actions;
    }

    private Component createEnabledBadge(UserEntity user) {
        boolean enabled = user.isEnabled();
        Span badge = new Span(enabled
                ? messageProperties.getAdminUsersYes()
                : messageProperties.getAdminUsersNo());
        badge.getElement().getThemeList().add("badge " + (enabled ? "success" : "error"));
        return badge;
    }

    private Component createUserIdentity(UserEntity user) {
        Avatar avatar = new Avatar(getFullName(user), AppConstants.USER_ICON_PATH);
        avatar.addClassName("admin-user-table-avatar");
        Span name = new Span(getFullName(user));
        HorizontalLayout identity = new HorizontalLayout(avatar, name);
        identity.setAlignItems(Alignment.CENTER);
        identity.setPadding(false);
        identity.setSpacing(true);
        return identity;
    }

    private Component createStatusBadge(UserEntity user) {
        UserStatus status = user.getStatus();
        Span badge = new Span(status == null ? "-" : messageProperties.getUserStatusLabel(status));
        String variant = status == null ? "contrast" : switch (status) {
            case AVAILABLE -> "success";
            case ABSENT -> "warning";
            case EMPLOYED -> "error";
            case OFFLINE -> "contrast";
        };
        badge.getElement().getThemeList().add("badge " + variant);
        return badge;
    }

    private String formatRoleNames(UserEntity user) {
        return Optional.ofNullable(user.getRoleEntities())
                .orElseGet(Set::of)
                .stream()
                .map(RoleEntity::getName)
                .map(this::formatRoleType)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.joining(", "));
    }

    private String getFullName(UserEntity user) {
        String firstName = clean(user.getFirstName());
        String lastName = clean(user.getLastName());
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isBlank() ? "-" : fullName;
    }

    private String getStatusText(UserEntity user) {
        return Optional.ofNullable(user.getStatus())
                .map(status -> messageProperties.getUserStatusLabel(status))
                .orElse("-");
    }

    private Component createLastActivity(UserEntity user) {
        LocalDateTime activity = user.getLastActivity();
        LocalDateTime now = LocalDateTime.now();
        String text = activity != null && activity.isAfter(now)
                ? formatDateTime(activity)
                : AdminUserActivityText.format(activity, now, messageProperties);
        Span label = new Span(text);
        if (activity != null) label.getElement().setAttribute("title", formatDateTime(activity));
        return label;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "-";
        }
        Locale locale = VaadinSession.getCurrent() != null && VaadinSession.getCurrent().getLocale() != null
                ? VaadinSession.getCurrent().getLocale()
                : Locale.getDefault();
        return LAST_ACTIVITY_FORMATTER.withLocale(locale).format(dateTime);
    }

    private String formatRoleType(RoleType roleType) {
        if (roleType == null) {
            return "-";
        }
        return switch (roleType) {
            case ROLE_ADMIN -> messageProperties.getRoleAdmin();
            case ROLE_USER -> messageProperties.getRoleUser();
        };
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

    private boolean isCurrentUser(UserEntity user) {
        return securityService.getCurrentUser()
                .map(currentUser -> Objects.equals(currentUser.getId(), user.getId()))
                .orElse(false);
    }

    private void showError(java.util.function.Supplier<String> message) {
        NotificationHelper.getInstance().showNotification(
                messageProperties::getBaseFailedTitle,
                message,
                NotificationHelper.NotificationType.ERROR
        );
    }

    private void showMutationError(RuntimeException exception, boolean deleting) {
        log.error("Benutzer {} fehlgeschlagen", deleting ? "löschen" : "speichern", exception);
        if (exception instanceof OptimisticLockingFailureException) {
            showError(messageProperties::getBaseUserUpdateConflict);
        } else if (!deleting && EmailConflict.isDuplicate(exception)) {
            showError(messageProperties::getBaseValidationEmailExistsMessage);
        } else if (exception instanceof IllegalStateException
                && "Der letzte aktive Administrator muss erhalten bleiben.".equals(exception.getMessage())) {
            // Legacy service contract has no typed last-admin exception. Match
            // only its exact business sentinel, never arbitrary IllegalStateExceptions.
            showError(deleting ? messageProperties::getAdminUsersErrorDeleteLastAdmin : messageProperties::getAdminUsersErrorLastActiveAdmin);
        } else if (!deleting && exception instanceof IllegalArgumentException && Set.of(
                "First name must not be blank", "Last name must not be blank", "Invalid email",
                "Gender must not be null", "Birth date must be in the past", "Roles must not be null",
                "Invalid role", "Invalid password", "Encoded password must not be blank",
                "Locale must not be null", "Status must not be null").contains(Optional.ofNullable(exception.getMessage()).orElse(""))) {
            showError(messageProperties::getAdminUsersValidationCheckFields);
        } else {
            showError(deleting ? messageProperties::getAdminUsersErrorDeleteFailed : messageProperties::getAdminUsersErrorSaveFailed);
        }
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalize(String value) {
        return clean(value).toLowerCase(Locale.ROOT);
    }

    @Getter
    @Setter
    private static final class AdminUserFormData {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;
        private String password;
        private Gender gender;
        private LocalDate birthDate;
        private boolean enabled;
        private Set<RoleType> roles;

        private static AdminUserFormData fromUser(UserEntity user) {
            AdminUserFormData formData = new AdminUserFormData();
            formData.id = user.getId();
            formData.firstName = Optional.ofNullable(user.getFirstName()).orElse("");
            formData.lastName = Optional.ofNullable(user.getLastName()).orElse("");
            formData.email = Optional.ofNullable(user.getEmail()).orElse("");
            formData.password = "";
            formData.gender = user.getGender();
            formData.birthDate = user.getBirthDate();
            formData.enabled = user.isEnabled();
            formData.roles = Optional.ofNullable(user.getRoleEntities())
                    .orElseGet(Set::of)
                    .stream()
                    .map(RoleEntity::getName)
                    .collect(Collectors.toSet());
            return formData;
        }
    }
}
