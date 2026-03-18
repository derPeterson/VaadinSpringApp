package de.derpeterson.app.views.admin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.service.RoleService;
import de.derpeterson.app.service.UserService;
import de.derpeterson.app.ui.components.CardComponent;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class AdminUserManagementSection extends VerticalLayout {

    private final transient MessageProperties messageProperties;
    private final transient UserService userService;
    private final transient RoleService roleService;

    private final H1 title;
    private final Span description;
    private final Button addUserButton;
    private final Grid<UserEntity> userGrid = new Grid<>(UserEntity.class, false);

    public AdminUserManagementSection(MessageProperties messageProperties,
                                      UserService userService,
                                      RoleService roleService) {
        this.messageProperties = messageProperties;
        this.userService = userService;
        this.roleService = roleService;

        setPadding(false);
        setSpacing(true);
        setWidthFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        addClassNames(LumoUtility.Gap.MEDIUM);

        title = new H1();
        title.addClassNames(
                LumoUtility.Margin.NONE,
                LumoUtility.FontSize.XXLARGE
        );

        description = new Span();
        description.addClassNames(LumoUtility.TextColor.SECONDARY);

        addUserButton = new Button();
        addUserButton.setPrefixComponent(VaadinIcon.PLUS.create());
        addUserButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addUserButton.addClickListener(e -> openDialog(new UserEntity()));

        configureGrid();

        VerticalLayout layout = new VerticalLayout(title, description, addUserButton, userGrid);
        layout.setPadding(false);
        layout.setSpacing(true);
        layout.setWidthFull();
        layout.setAlignItems(Alignment.START);

        CardComponent card = new CardComponent(layout);
        card.setWidthFull();
        card.setMaxWidth("1100px");

        add(card);

        refreshTexts();
        refreshGrid();
    }

    public void refreshTexts() {
        title.setText(messageProperties.getAdminUsersTitle());
        description.setText(messageProperties.getAdminUsersDescription());
        addUserButton.setText(messageProperties.getAdminUsersAdd());

        userGrid.removeAllColumns();
        configureGrid();
    }

    private void configureGrid() {
        userGrid.addColumn(UserEntity::getFirstName)
                .setHeader(messageProperties.getAdminUsersGridFirstName())
                .setAutoWidth(true);

        userGrid.addColumn(UserEntity::getLastName)
                .setHeader(messageProperties.getAdminUsersGridLastName())
                .setAutoWidth(true);

        userGrid.addColumn(UserEntity::getEmail)
                .setHeader(messageProperties.getAdminUsersGridEmail())
                .setAutoWidth(true)
                .setFlexGrow(1);

        userGrid.addColumn(user -> user.isEnabled()
                        ? messageProperties.getAdminUsersYes()
                        : messageProperties.getAdminUsersNo())
                .setHeader(messageProperties.getAdminUsersGridEnabled())
                .setAutoWidth(true);

        userGrid.addColumn(user -> Optional.ofNullable(user.getRoleEntities())
                        .orElseGet(Set::of)
                        .stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.joining(", ")))
                .setHeader(messageProperties.getAdminUsersGridRoles())
                .setAutoWidth(true)
                .setFlexGrow(1);

        userGrid.addComponentColumn(user -> {
            Button editButton = new Button(VaadinIcon.EDIT.create(), e -> openDialog(user));
            editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            editButton.setTooltipText(messageProperties.getAdminUsersEdit());

            Button deleteButton = new Button(VaadinIcon.TRASH.create(), e -> deleteUser(user));
            deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            deleteButton.setTooltipText(messageProperties.getAdminUsersDelete());

            HorizontalLayout actions = new HorizontalLayout(editButton, deleteButton);
            actions.setPadding(false);
            actions.setSpacing(true);
            return actions;
        }).setHeader(messageProperties.getAdminUsersGridActions()).setAutoWidth(true);

        userGrid.setWidthFull();
    }

    private void refreshGrid() {
        userGrid.setItems(userService.findAllUsers());
    }

    private void deleteUser(UserEntity user) {
        userService.deleteUser(user);

        NotificationHelper.getInstance().showNotification(
                messageProperties::getBaseSuccessTitle,
                messageProperties::getAdminUsersDeleteSuccess,
                NotificationHelper.NotificationType.SUCCESS
        );

        refreshGrid();
    }

    private void openDialog(UserEntity user) {
        boolean editMode = user.getId() != null;

        Dialog dialog = new Dialog();
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(true);
        dialog.setDraggable(true);
        dialog.setResizable(true);
        dialog.setWidth("520px");
        dialog.setHeaderTitle(editMode
                ? messageProperties.getAdminUsersEditDialogTitle()
                : messageProperties.getAdminUsersCreateDialogTitle());

        TextField firstNameField = new TextField(messageProperties.getAdminUsersFieldFirstName());
        firstNameField.setWidthFull();
        firstNameField.setValue(Optional.ofNullable(user.getFirstName()).orElse(""));

        TextField lastNameField = new TextField(messageProperties.getAdminUsersFieldLastName());
        lastNameField.setWidthFull();
        lastNameField.setValue(Optional.ofNullable(user.getLastName()).orElse(""));

        EmailField emailField = new EmailField(messageProperties.getAdminUsersFieldEmail());
        emailField.setWidthFull();
        emailField.setValue(Optional.ofNullable(user.getEmail()).orElse(""));

        PasswordField passwordField = new PasswordField(messageProperties.getAdminUsersFieldPassword());
        passwordField.setWidthFull();
        passwordField.setRevealButtonVisible(true);

        Checkbox enabledCheckbox = new Checkbox(messageProperties.getAdminUsersFieldEnabled());
        enabledCheckbox.setValue(user.isEnabled());

        CheckboxGroup<RoleType> rolesGroup = new CheckboxGroup<>();
        rolesGroup.setLabel(messageProperties.getAdminUsersFieldRoles());
        rolesGroup.setItems(RoleType.values());
        rolesGroup.setValue(Optional.ofNullable(user.getRoleEntities())
                .orElseGet(Set::of)
                .stream()
                .map(RoleEntity::getName)
                .collect(Collectors.toSet()));

        Button saveButton = new Button(messageProperties.getAdminUsersSave(), e -> {
            user.setFirstName(firstNameField.getValue());
            user.setLastName(lastNameField.getValue());
            user.setEmail(emailField.getValue());
            user.setEnabled(enabledCheckbox.getValue());

            Set<RoleEntity> roleEntities = rolesGroup.getValue().stream()
                    .map(roleService::findByName)
                    .flatMap(Optional::stream)
                    .collect(Collectors.toSet());

            user.setRoleEntities(roleEntities);

            if (!passwordField.getValue().isBlank()) {
                userService.updatePassword(user, passwordField.getValue());
            }

            userService.saveUser(user);

            dialog.close();
            refreshGrid();

            NotificationHelper.getInstance().showNotification(
                    messageProperties::getBaseSuccessTitle,
                    () -> editMode
                            ? messageProperties.getAdminUsersSaveSuccessUpdated()
                            : messageProperties.getAdminUsersSaveSuccessCreated(),
                    NotificationHelper.NotificationType.SUCCESS
            );
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button(messageProperties.getAdminUsersCancel(), e -> dialog.close());

        HorizontalLayout footer = new HorizontalLayout(cancelButton, saveButton);
        footer.setWidthFull();
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        VerticalLayout dialogLayout = new VerticalLayout(
                firstNameField,
                lastNameField,
                emailField,
                passwordField,
                enabledCheckbox,
                rolesGroup,
                footer
        );
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(true);

        dialog.add(dialogLayout);
        dialog.open();
    }
}