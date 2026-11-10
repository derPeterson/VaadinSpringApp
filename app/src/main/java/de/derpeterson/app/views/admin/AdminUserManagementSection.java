package de.derpeterson.app.views.admin;

import static de.derpeterson.app.helper.logging.LogSafety.safeFailure;
import static de.derpeterson.app.ui.components.CosmosClasses.DELETE_BUTTON;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_CARD;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_DESCRIPTION;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_HEADER;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_HEADING;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_ICON;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_TITLE;
import static de.derpeterson.app.ui.components.CosmosClasses.REFERENCE_OVERLAY;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Binder.Binding;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.helper.time.UserActivityTime;
import de.derpeterson.app.helper.ui.CompactDateInput;
import de.derpeterson.app.helper.ui.EqualButtonWidths;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.helper.ui.ValidationHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.Gender;
import de.derpeterson.app.model.enums.IconSize;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.ExternalIdentityService;
import de.derpeterson.app.service.ExternalIdentityService.SignInSummary;
import de.derpeterson.app.service.RoleService;
import de.derpeterson.app.service.UserAvatarService;
import de.derpeterson.app.service.UserService;
import de.derpeterson.app.ui.components.AppIcon;
import de.derpeterson.app.ui.components.CosmosCardComponent;
import de.derpeterson.app.ui.components.CosmosPagination;
import de.derpeterson.app.ui.components.CosmosSectionHeaderComponent;
import de.derpeterson.app.ui.components.OverlayUserIcon;
import de.derpeterson.app.ui.components.PortalFields;
import de.derpeterson.app.validation.EmailConflict;
import de.derpeterson.app.validation.UserInputRules;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;

@Slf4j
public class AdminUserManagementSection extends VerticalLayout {
  private static final String COSMOS_PANEL_TITLE_CLASS = PANEL_TITLE;
  private static final String COSMOS_PANEL_DESCRIPTION_CLASS = PANEL_DESCRIPTION;
  private static final String ARIA_HIDDEN = "aria-hidden";
  private static final String COSMOS_PANEL_HEADING_CLASS = PANEL_HEADING;
  private static final String ARIA_LABEL_PROPERTY = "ariaLabel";
  private static final String COSMOS_FORM_ROW_CLASS = "cosmos-form-row";

  private static final DateTimeFormatter LAST_ACTIVITY_FORMATTER =
      new DateTimeFormatterBuilder()
          .appendLocalized(FormatStyle.MEDIUM, FormatStyle.MEDIUM)
          .appendPattern(" XXX")
          .toFormatter()
          .withZone(UserActivityTime.DISPLAY_ZONE);

  private final transient MessageProperties messageProperties;
  private final transient UserService userService;
  private final transient RoleService roleService;
  private final transient SecurityService securityService;
  private final Clock userManagementClock;

  private final Span userManagementBreadcrumbSpan;
  private final Span userManagementTitleSpan;
  private final H2 cardTitle;
  private final Span cardDescription;
  private final Span accountCount;
  private final Span userManagementDescriptionSpan;
  private final Button addUserButton;
  private final TextField searchField;
  private final Button refreshButton;
  private final Grid<UserEntity> userGrid = new Grid<>(UserEntity.class, false);

  private final RadioButtonGroup<AdminUserFilters.Enabled> enabledFilter = new RadioButtonGroup<>();
  private transient UserAvatarService userAvatarService;
  private transient ExternalIdentityService externalIdentityService;
  private Map<Long, UserAvatarService.State> avatarStates = Map.of();

  public void configureAvatars(UserAvatarService userAvatarService) {
    this.userAvatarService = userAvatarService;
    updateGridPage();
  }

  public void configureSignInDetails(ExternalIdentityService externalIdentityService) {
    this.externalIdentityService = externalIdentityService;
  }

  private final CheckboxGroup<UserStatus> statusFilter = new CheckboxGroup<>();
  private final CheckboxGroup<RoleType> roleFilter = new CheckboxGroup<>();
  private final AdminUserActivityFilter lastActivityFilter;
  private final CosmosPagination userTablePagination;
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
  private AdminUserFilters appliedFilters =
      new AdminUserFilters("", null, Set.of(), Set.of(), null);
  private final List<Runnable> dialogErrorRefreshers = new ArrayList<>();

  private int currentPage = 0;
  private int pageSize = 10;

  public AdminUserManagementSection(
      MessageProperties messageProperties,
      UserService userService,
      RoleService roleService,
      SecurityService securityService) {
    this(messageProperties, userService, roleService, securityService, Clock.systemUTC());
  }

  AdminUserManagementSection(
      MessageProperties messageProperties,
      UserService userService,
      RoleService roleService,
      SecurityService securityService,
      Clock userManagementClock) {
    this.userManagementClock = Objects.requireNonNull(userManagementClock);
    this.lastActivityFilter = new AdminUserActivityFilter(userManagementClock);
    this.messageProperties = messageProperties;
    this.userService = userService;
    this.roleService = roleService;
    this.securityService = securityService;

    setPadding(false);
    setSpacing(false);
    setWidthFull();
    setAlignItems(FlexComponent.Alignment.CENTER);
    addClassNames("cosmos-admin-section", "cosmos-admin-section--users");

    userManagementBreadcrumbSpan = new Span();
    userManagementTitleSpan = new Span();
    cardTitle = new H2();
    cardTitle.addClassName(COSMOS_PANEL_TITLE_CLASS);
    cardDescription = new Span();
    cardDescription.addClassName(COSMOS_PANEL_DESCRIPTION_CLASS);
    accountCount = new Span();
    accountCount.addClassNames("cosmos-panel-meta", "admin-accounts-metric");

    userManagementDescriptionSpan = new Span();
    userManagementDescriptionSpan.addClassNames(LumoUtility.TextColor.SECONDARY);

    addUserButton = new Button();
    addUserButton.setPrefixComponent(AppIcon.USER_PLUS.create());
    addUserButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    addUserButton.addClickListener(e -> openDialog(new UserEntity()));

    searchField = new TextField();
    searchField.setPrefixComponent(AppIcon.SEARCH.create());
    searchField.setClearButtonVisible(true);
    searchField.setWidthFull();
    searchField.setMaxWidth("360px");
    searchField.setValueChangeMode(ValueChangeMode.EAGER);
    // Die Suche gehört zum Filterentwurf; erst Anwenden verändert die Tabelle.

    refreshButton = new Button(AppIcon.REFRESH.create(), event -> refreshUsersWithFeedback());
    refreshButton.addClassName("admin-user-refresh");

    userTablePagination =
        new CosmosPagination(
            messageProperties,
            page -> {
              currentPage = page;
              updateGridPage();
            },
            size -> {
              pageSize = size;
              currentPage = 0;
              updateGridPage();
            });
    configureFilters();
    configureGrid();

    filterBar =
        new AdminUserFilterBar(
            messageProperties,
            searchField,
            refreshButton,
            addUserButton,
            new AdminUserFilterBar.FilterControls(
                enabledFilter, statusFilter, roleFilter, lastActivityFilter),
            this::applyFilterInputs);

    Div paginationFooter = new Div(userTablePagination);
    paginationFooter.addClassName("cosmos-panel-footer");
    VerticalLayout layout =
        new VerticalLayout(createUsersCardHeader(), filterBar, userGrid, paginationFooter);
    layout.setPadding(false);
    layout.setSpacing(false);
    layout.addClassName("cosmos-admin-users-content");
    layout.setWidthFull();
    layout.setAlignItems(Alignment.START);

    CosmosCardComponent card = new CosmosCardComponent(layout);
    card.addClassNames("cosmos-admin-users-card", PANEL_CARD);
    add(
        new CosmosSectionHeaderComponent(
            userManagementBreadcrumbSpan, userManagementTitleSpan, userManagementDescriptionSpan),
        card);

    refreshTexts();
    refreshGrid();
  }

  private Div createUsersCardHeader() {
    SvgIcon usersIcon = AppIcon.USERS.create();
    usersIcon.getElement().setAttribute(ARIA_HIDDEN, "true");
    Div iconBadge = new Div(usersIcon);
    iconBadge.addClassName(PANEL_ICON);
    Div heading = new Div(cardTitle, cardDescription);
    heading.addClassName(COSMOS_PANEL_HEADING_CLASS);
    Div header = new Div(iconBadge, heading, accountCount);
    header.addClassName(PANEL_HEADER);
    return header;
  }

  public void refreshTexts() {
    userManagementBreadcrumbSpan.setText(
        messageProperties.getAdminHeaderTitle() + " / " + messageProperties.getAdminUsersTitle());
    cardTitle.setText(messageProperties.getAdminUsersCardTitle());
    cardDescription.setText(messageProperties.getAdminUsersCardDescription());
    userManagementTitleSpan.setText(messageProperties.getAdminUsersTitle());
    userManagementDescriptionSpan.setText(messageProperties.getAdminUsersDescription());
    addUserButton.setText(messageProperties.getAdminUsersAdd());

    searchField.setLabel(messageProperties.getAdminUsersSearchLabel());
    searchField.setAriaLabel(messageProperties.getAdminUsersSearchLabel());
    searchField.setPlaceholder(messageProperties.getAdminUsersSearchPlaceholder());

    refreshButton.setTooltipText(messageProperties.getAdminUsersRefreshTooltip());
    refreshButton.setAriaLabel(messageProperties.getAdminUsersRefreshTooltip());
    enabledFilter.setLabel(messageProperties.getAdminUsersGridEnabled());

    enabledFilter.setItemLabelGenerator(enabled ->
        switch (enabled) {
          case ALL -> messageProperties.getAdminUsersFilterAll();
          case YES -> messageProperties.getAdminUsersYes();
          case NO -> messageProperties.getAdminUsersNo();
        });
    statusFilter.setLabel(messageProperties.getAdminUsersGridStatus());

    statusFilter.setItemLabelGenerator(messageProperties::getUserStatusLabel);
    refreshStatusChoices();
    roleFilter.setLabel(messageProperties.getAdminUsersFilterRole());

    roleFilter.setItemLabelGenerator(this::formatRoleType);
    lastActivityFilter
        .getElement()
        .setAttribute("title", messageProperties.getAdminUsersFilterActiveSinceHelp());
    lastActivityFilter.setErrorMessage(messageProperties.getAdminUsersFilterActiveSinceError());
    Locale locale =
        VaadinSession.getCurrent() != null
            ? VaadinSession.getCurrent().getLocale()
            : Locale.getDefault();
    lastActivityFilter.setLocale(locale);
    lastActivityFilter.refreshTexts(messageProperties);
    userTablePagination.refreshTexts();
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

    refreshEmptyState();

    updateGridPage();
    updateActiveFilters();
    List.copyOf(dialogErrorRefreshers).forEach(Runnable::run);
  }

  private void refreshEmptyState() {
    Span emptyTitle = new Span(messageProperties.getAdminUsersGridEmptyState());
    emptyTitle.addClassName("admin-grid-empty-title");
    Span hint = new Span(messageProperties.getAdminUsersEmptyHint());
    Div empty = new Div(AppIcon.USERS.create(), emptyTitle, hint);
    empty.addClassName("admin-grid-empty");
    userGrid.setEmptyStateComponent(empty);
  }

  private void configureGrid() {
    nameColumn =
        userGrid
            .addComponentColumn(this::createUserIdentity)
            .setHeader(messageProperties.getAdminUsersGridName())
            .setAutoWidth(true)
            .setSortable(true)
            .setComparator(Comparator.comparing(this::getFullName, String.CASE_INSENSITIVE_ORDER));

    emailColumn =
        userGrid
            .addColumn(UserEntity::getEmail)
            .setHeader(messageProperties.getAdminUsersGridEmail())
            .setAutoWidth(true)
            .setSortable(true)
            .setComparator(
                Comparator.comparing(
                    user -> normalize(user.getEmail()), String.CASE_INSENSITIVE_ORDER))
            .setFlexGrow(1);

    enabledColumn =
        userGrid
            .addComponentColumn(this::createEnabledBadge)
            .setHeader(messageProperties.getAdminUsersGridEnabled())
            .setAutoWidth(true)
            .setSortable(true)
            .setComparator(Comparator.comparing(UserEntity::isEnabled));

    statusColumn =
        userGrid
            .addComponentColumn(this::createStatusBadge)
            .setHeader(messageProperties.getAdminUsersGridStatus())
            .setAutoWidth(true)
            .setSortable(true)
            .setComparator(
                Comparator.comparing(
                    user ->
                        Optional.ofNullable(user.getStatus())
                            .map(messageProperties::getUserStatusLabel)
                            .orElse(""),
                    String.CASE_INSENSITIVE_ORDER));

    rolesColumn =
        userGrid
            .addComponentColumn(this::createRoleBadges)
            .setHeader(messageProperties.getAdminUsersGridRoles())
            .setAutoWidth(true)
            .setSortable(true)
            .setComparator(
                Comparator.comparing(this::formatRoleNames, String.CASE_INSENSITIVE_ORDER))
            .setAutoWidth(false)
            .setWidth("140px")
            .setFlexGrow(0);

    lastActivityColumn =
        userGrid
            .addComponentColumn(this::createLastActivity)
            .setHeader(messageProperties.getAdminUsersGridLastActivity())
            .setAutoWidth(true)
            .setSortable(true)
            .setComparator(
                Comparator.comparing(
                    UserEntity::getLastActivity, Comparator.nullsLast(Instant::compareTo)));

    actionsHeader.setText(messageProperties.getAdminUsersGridActions());
    actionsHeader.addClassName("admin-users-actions-header");
    actionsColumn =
        userGrid
            .addComponentColumn(this::createActionButtons)
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
    refreshEmptyState();
    userGrid.sort(
        List.of(
            new GridSortOrder<>(
                nameColumn, com.vaadin.flow.data.provider.SortDirection.ASCENDING)));

    List<Grid.Column<UserEntity>> columns =
        List.of(
            nameColumn,
            emailColumn,
            enabledColumn,
            statusColumn,
            rolesColumn,
            lastActivityColumn,
            actionsColumn);
    List<String> widths = List.of("186px", "160px", "85px", "108px", "106px", "160px", "114px");
    for (int index = 0; index < columns.size(); index++) {
      columns
          .get(index)
          .setAutoWidth(false)
          .setWidth(widths.get(index))
          .setFlexGrow(index == 1 ? 1 : 0);
    }
    userGrid.addClassName("admin-users-grid");
    userGrid.addThemeVariants(GridVariant.ROW_STRIPES);
  }

  private void configureFilters() {
    enabledFilter.setItems(AdminUserFilters.Enabled.values());
    enabledFilter.setValue(AdminUserFilters.Enabled.ALL);
    enabledFilter.addClassName("admin-filter-enabled");
    statusFilter.setItems(UserStatus.values());
    roleFilter.setItems(RoleType.values());
    statusFilter.addClassName("admin-filter-choices");
    roleFilter.addClassName("admin-filter-choices");
  }

  private void refreshStatusChoices() {
    statusFilter.setRenderer(
        new com.vaadin.flow.data.renderer.ComponentRenderer<>(status -> {
          Component symbol = status.getOutlineComponent(IconSize.PIXEL_16, false);
          Div label = new Div(symbol, new Span(messageProperties.getUserStatusLabel(status)));
          label.addClassName("admin-filter-status-label");
          label.getElement().setAttribute("data-status", status.name().toLowerCase(Locale.ROOT));
          return label;
        }));
    lastActivityFilter.addClassName("admin-user-activity-filter");
  }

  private int getTotalPages() {
    if (filteredUsers.isEmpty()) {
      return 1;
    }
    return (int) Math.ceil((double) filteredUsers.size() / pageSize);
  }

  private void refreshUsersWithFeedback() {
    try {
      refreshGrid();
    } catch (RuntimeException failure) {
      log.warn("User list refresh failed", safeFailure(failure));
      showError(messageProperties::getAdminUsersRefreshFailed);
      return;
    }
    NotificationHelper.getInstance()
        .showNotification(
            messageProperties::getAdminUsersRefreshSuccess,
            null,
            NotificationHelper.NotificationType.SUCCESS);
  }

  private void refreshGrid() {
    users =
        userService.findAllUsers().stream()
            .sorted(Comparator.comparing(this::getFullName, String.CASE_INSENSITIVE_ORDER))
            .toList();
    currentPage = 0;
    applyGridFilter();
  }

  private void applyFilterInputs() {
    if (lastActivityFilter.isInvalid()) {
      return;
    }
    appliedFilters =
        new AdminUserFilters(
            searchField.getValue(),
            enabledFilter.getValue().value(),
            statusFilter.getValue(),
            roleFilter.getValue(),
            UserActivityTime.inclusiveCutoff(lastActivityFilter.getValue()));
    applyGridFilter();
  }

  private void applyGridFilter() {
    filteredUsers = users.stream().filter(appliedFilters::matches).toList();
    currentPage = 0;
    updateGridPage();
    updateActiveFilters();
  }

  private void updateActiveFilters() {
    filterBar.showActiveCount(appliedFilters.activeGroupCount());
  }

  private void updateGridPage() {
    int totalPages = getTotalPages();

    if (currentPage >= totalPages) {
      currentPage = Math.max(0, totalPages - 1);
    }

    int fromIndex = Math.min(currentPage * pageSize, filteredUsers.size());
    int toIndex = Math.min(fromIndex + pageSize, filteredUsers.size());

    Comparator<UserEntity> comparator =
        userGrid.getSortOrder().stream()
            .map(order ->
                (Comparator<UserEntity>) order.getSorted().getComparator(order.getDirection()))
            .reduce(Comparator::thenComparing)
            .orElse((left, right) -> 0);
    List<UserEntity> sortedUsers = filteredUsers.stream().sorted(comparator).toList();
    List<UserEntity> page = sortedUsers.subList(fromIndex, toIndex);
    if (userAvatarService != null) {
      avatarStates = userAvatarService.states(page.stream().map(UserEntity::getId).toList());
    }
    userGrid.setItems(page);
    userTablePagination.showPage(currentPage, filteredUsers.size(), pageSize);
    accountCount.removeAll();
    accountCount.add(
        new Span(messageProperties.getAdminUsersAccountsLabel()),
        new Span(Integer.toString(users.size())));
    accountCount
        .getElement()
        .setAttribute("aria-label", messageProperties.getAdminUsersAccountCount(users.size()));
  }

  private void deleteUser(UserEntity user) {
    deleteUser(user, () -> {});
  }

  private void deleteUser(UserEntity user, Runnable afterDelete) {
    try {
      if (isCurrentUser(user)) {
        showError(messageProperties::getAdminUsersErrorDeleteOwnUser);
        return;
      }

      if (!userService.canDeleteUser(user)) {
        showError(messageProperties::getAdminUsersErrorDeleteLastSuperadmin);
        return;
      }
    } catch (RuntimeException exception) {
      showMutationError(exception, true);
      return;
    }

    Dialog deleteUserDialog = new Dialog();
    deleteUserDialog.addClassNames("cosmos-theme", REFERENCE_OVERLAY, "admin-reference-dialog");
    deleteUserDialog.getThemeNames().add("cosmos-reference");
    deleteUserDialog.getElement().getThemeList().add("cosmos");
    deleteUserDialog.addClassName("admin-user-delete-dialog");
    deleteUserDialog.setCloseOnEsc(true);
    deleteUserDialog.setCloseOnOutsideClick(true);

    H2 dialogTitle = new H2(messageProperties.getAdminUsersDeleteDialogTitle());
    dialogTitle.addClassName(COSMOS_PANEL_TITLE_CLASS);
    Span dialogDescription = new Span(messageProperties.getAdminUsersDeleteDialogDescription());
    dialogDescription.addClassName(COSMOS_PANEL_DESCRIPTION_CLASS);
    Div dialogHeading = new Div(dialogTitle, dialogDescription);
    dialogHeading.addClassName(COSMOS_PANEL_HEADING_CLASS);
    Button closeDeleteUserDialogButton =
        new Button(AppIcon.CLOSE_CIRCLE.create(), event -> deleteUserDialog.close());
    closeDeleteUserDialogButton.addClassNames(
        "cosmos-overlay-close", LumoUtility.TextColor.PRIMARY);
    closeDeleteUserDialogButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    closeDeleteUserDialogButton.setAriaLabel(messageProperties.getAdminUsersCloseDialog());
    Div deleteIcon = new Div(AppIcon.TRASH.create());
    deleteIcon.addClassNames(PANEL_ICON, "cosmos-delete-accent");
    Div dialogHeader = new Div(deleteIcon, dialogHeading, closeDeleteUserDialogButton);
    dialogHeader.addClassName("admin-user-dialog-header");
    deleteUserDialog.getHeader().add(dialogHeader);
    deleteUserDialog.getElement().setProperty(ARIA_LABEL_PROPERTY, dialogTitle.getText());

    SvgIcon warningIcon = AppIcon.TRASH.create();
    warningIcon.getElement().setAttribute(ARIA_HIDDEN, "true");
    Div warningBadge = new Div(warningIcon);
    warningBadge.addClassNames(PANEL_ICON, "cosmos-delete-accent");
    Span warningTitle = new Span(messageProperties.getAdminUsersDeleteDialogQuestion());
    warningTitle.addClassName("admin-user-delete-warning-title");
    Span warningText = new Span(messageProperties.getAdminUsersDeleteDialogWarning());
    warningText.addClassName(COSMOS_PANEL_DESCRIPTION_CLASS);
    Div warningHeading = new Div(warningTitle, warningText);
    warningHeading.addClassName(COSMOS_PANEL_HEADING_CLASS);
    Div warning = new Div(warningHeading);
    warning.addClassName("admin-user-delete-warning");

    OverlayUserIcon avatar =
        new OverlayUserIcon(messageProperties, user, user.getStatus(), IconSize.PIXEL_32);
    if (userAvatarService != null) {
      avatar.configureAvatar(
          userAvatarService,
          avatarStates.getOrDefault(user.getId(), UserAvatarService.State.initial()));
    }
    avatar.addClassName("admin-user-table-avatar");
    Span userName = new Span(getFullName(user));
    userName.addClassName("admin-user-delete-name");
    Span userEmail = new Span(Optional.ofNullable(user.getEmail()).orElse("-"));
    userEmail.addClassName(COSMOS_PANEL_DESCRIPTION_CLASS);
    Div userHeading = new Div(userName, userEmail);
    userHeading.addClassName(COSMOS_PANEL_HEADING_CLASS);
    Div userIdentity = new Div(avatar, userHeading);
    userIdentity.addClassName("admin-user-delete-identity");

    Button cancelButton =
        new Button(messageProperties.getAdminUsersCancel(), event -> deleteUserDialog.close());
    cancelButton.setPrefixComponent(AppIcon.CLOSE.create());
    Button confirmButton =
        new Button(
            messageProperties.getPortalDeleteAction(),
            event -> {
              try {
                userService.deleteUser(user);
                deleteUserDialog.close();
                afterDelete.run();
                refreshGrid();
                NotificationHelper.getInstance()
                    .showNotification(
                        messageProperties::getBaseSuccessTitle,
                        messageProperties::getAdminUsersDeleteSuccess,
                        NotificationHelper.NotificationType.SUCCESS);
              } catch (RuntimeException exception) {
                showMutationError(exception, true);
              }
            });
    confirmButton.setPrefixComponent(AppIcon.TRASH.create());
    confirmButton.addClassName(DELETE_BUTTON);
    confirmButton.addClassName("admin-user-delete-confirm");

    HorizontalLayout footer = new HorizontalLayout(cancelButton, confirmButton);
    footer.addClassName("cosmos-dialog-actions");
    EqualButtonWidths.configureEqualButtonWidths(footer);
    footer.setWidthFull();
    footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

    deleteUserDialog.getFooter().add(footer);
    VerticalLayout dialogLayout = new VerticalLayout(warning, userIdentity);
    dialogLayout.setPadding(false);
    dialogLayout.setSpacing(false);
    dialogLayout.addClassName("cosmos-dialog-form");

    deleteUserDialog.add(dialogLayout);
    Runnable refreshTexts =
        () -> {
          dialogTitle.setText(messageProperties.getAdminUsersDeleteDialogTitle());
          deleteUserDialog.getElement().setProperty(ARIA_LABEL_PROPERTY, dialogTitle.getText());
          dialogDescription.setText(messageProperties.getAdminUsersDeleteDialogDescription());
          closeDeleteUserDialogButton.setAriaLabel(messageProperties.getAdminUsersCloseDialog());
          warningTitle.setText(messageProperties.getAdminUsersDeleteDialogQuestion());
          warningText.setText(messageProperties.getAdminUsersDeleteDialogWarning());
          cancelButton.setText(messageProperties.getAdminUsersCancel());
          confirmButton.setText(messageProperties.getPortalDeleteAction());
        };
    dialogErrorRefreshers.add(refreshTexts);
    deleteUserDialog.addOpenedChangeListener(event -> {
      if (!event.isOpened()) {
        dialogErrorRefreshers.remove(refreshTexts);
      }
    });
    deleteUserDialog.addDetachListener(event -> dialogErrorRefreshers.remove(refreshTexts));
    deleteUserDialog.open();
  }

  private void openDialog(UserEntity user) {
    new UserEditor(user).open();
  }

  /** Zustand und Rückmeldungen genau eines geöffneten Benutzerformulars. */
  private final class UserEditor {
    private final UserEntity user;
    private final boolean editMode;
    private final AdminUserFormData formData;
    private final Long expectedVersion;
    private Dialog editUserDialog;
    private H2 dialogTitle;
    private Span dialogDescription;
    private Button closeEditUserDialogButton;
    private Binder<AdminUserFormData> binder;
    private TextField firstNameField;
    private TextField lastNameField;
    private EmailField emailField;
    private PasswordField passwordField;
    private Span passwordHint;
    private Span confirmPasswordHint;
    private PasswordField confirmPasswordField;
    private ComboBox<Gender> genderComboBox;
    private DatePicker birthDatePicker;
    private Checkbox enabledCheckbox;
    private Span enabledCaption;
    private Span enabledHint;
    private MultiSelectComboBox<RoleType> rolesField;
    private Span rolesSummary;
    private Span technicalInfo;
    private Div securityNote;
    private AtomicBoolean emailCheckFailed;
    private AtomicBoolean showPasswordErrors;
    private AtomicBoolean showConfirmPasswordErrors;
    private Binding<AdminUserFormData, String> passwordBinding;
    private Binding<AdminUserFormData, String> confirmPasswordBinding;
    private Button saveButton;
    private Button cancelButton;
    private SignInSummary signInSummary;
    private final H3 signInTitle = new H3();
    private final Span registrationLabel = new Span();
    private final Badge registeredVia = new Badge();
    private final Span methodsLabel = new Span();
    private final Div availableMethods = new Div();

    private UserEditor(UserEntity user) {
      this.user = user;
      editMode = user.getId() != null;
      formData = AdminUserFormData.fromUser(user);
      expectedVersion = user.getVersion();
    }

    private void open() {
      configureHeader();
      configureFields();
      bindFields();
      configurePasswordFeedback();
      configureActions();
      composeLayout();
      registerTextRefresh();
      editUserDialog.open();
    }

    private void configureHeader() {
      editUserDialog = new Dialog();
      editUserDialog.addClassNames("cosmos-theme", REFERENCE_OVERLAY, "admin-reference-dialog");
      editUserDialog.getThemeNames().add("cosmos-reference");
      editUserDialog.getElement().getThemeList().add("cosmos");
      editUserDialog.setCloseOnEsc(true);
      editUserDialog.setCloseOnOutsideClick(true);
      editUserDialog.setDraggable(false);
      editUserDialog.setResizable(false);
      editUserDialog.addClassName("admin-user-dialog");
      dialogTitle =
          new H2(
              editMode
                  ? messageProperties.getAdminUsersEditDialogTitle()
                  : messageProperties.getAdminUsersCreateDialogTitle());
      dialogTitle.addClassName(COSMOS_PANEL_TITLE_CLASS);
      dialogDescription = new Span(messageProperties.getAdminUsersDialogDescription());
      dialogDescription.addClassName(COSMOS_PANEL_DESCRIPTION_CLASS);
      Div dialogHeading = new Div(dialogTitle, dialogDescription);
      dialogHeading.addClassName(COSMOS_PANEL_HEADING_CLASS);
      SvgIcon userIcon = AppIcon.USERS.create();
      userIcon.getElement().setAttribute(ARIA_HIDDEN, "true");
      Div iconBadge = new Div(userIcon);
      iconBadge.addClassName(PANEL_ICON);
      closeEditUserDialogButton =
          new Button(AppIcon.CLOSE_CIRCLE.create(), event -> editUserDialog.close());
      closeEditUserDialogButton.addClassNames(
          "cosmos-overlay-close", LumoUtility.TextColor.PRIMARY);
      closeEditUserDialogButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
      closeEditUserDialogButton.setAriaLabel(messageProperties.getAdminUsersCloseDialog());
      Div dialogHeader = new Div(iconBadge, dialogHeading, closeEditUserDialogButton);
      dialogHeader.addClassName("admin-user-dialog-header");
      editUserDialog.getHeader().add(dialogHeader);
      editUserDialog.getElement().setProperty(ARIA_LABEL_PROPERTY, dialogTitle.getText());
    }

    private void configureFields() {
      binder = new Binder<>(AdminUserFormData.class);

      firstNameField = new TextField(messageProperties.getAdminUsersFieldFirstName());
      firstNameField.setRequiredIndicatorVisible(true);
      firstNameField.setWidthFull();
      firstNameField.setPlaceholder(messageProperties.getAccountFirstNamePlaceholder());

      lastNameField = new TextField(messageProperties.getAdminUsersFieldLastName());
      lastNameField.setRequiredIndicatorVisible(true);
      lastNameField.setWidthFull();
      lastNameField.setPlaceholder(messageProperties.getAccountLastNamePlaceholder());

      emailField = new EmailField(messageProperties.getAdminUsersFieldEmail());
      emailField.setPattern(UserInputRules.EMAIL_REGEX);
      emailField.setRequiredIndicatorVisible(true);
      emailField.setWidthFull();
      emailField.setPlaceholder(messageProperties.getAccountEmailPlaceholder());
      emailField.setClearButtonVisible(true);

      passwordField = new PasswordField(messageProperties.getAdminUsersFieldPassword());
      passwordField.setWidthFull();
      passwordField.setRevealButtonVisible(true);
      passwordField.setValueChangeMode(ValueChangeMode.EAGER);
      passwordField.setRequiredIndicatorVisible(!editMode);
      passwordField.setPlaceholder(
          editMode
              ? messageProperties.getAdminUsersPasswordPlaceholderEdit()
              : messageProperties.getAccountPasswordPlaceholder());
      passwordHint = new Span();
      passwordHint.addClassName("admin-user-password-hint");
      refreshPasswordHint();
      passwordField.setAriaDescribedBy(passwordHint);

      confirmPasswordField = new PasswordField(messageProperties.getPublicConfirmPasswordField());
      confirmPasswordField.setWidthFull();
      confirmPasswordField.setRevealButtonVisible(true);
      confirmPasswordField.setValueChangeMode(ValueChangeMode.EAGER);
      confirmPasswordField.setPlaceholder(messageProperties.getAccountConfirmPasswordPlaceholder());
      confirmPasswordField.setEnabled(false);
      confirmPasswordField.setAriaDescribedBy(passwordHint);
      confirmPasswordHint = new Span(messageProperties.getAdminUsersPasswordConfirmHelper());
      confirmPasswordHint.addClassName("admin-user-password-hint");

      genderComboBox = new ComboBox<>(messageProperties.getRegistrationGenderCombobox());
      genderComboBox.setItems(Gender.values());
      PortalFields.styleChoice(genderComboBox, AdminUserManagementSection.this::formatGender, true);
      genderComboBox.setPlaceholder(messageProperties.getAccountGenderPlaceholder());
      genderComboBox.setWidthFull();
      genderComboBox.setClearButtonVisible(true);
      genderComboBox.setItemLabelGenerator(AdminUserManagementSection.this::formatGender);

      birthDatePicker = new DatePicker(messageProperties.getRegistrationBirthDateField());
      birthDatePicker.setAutoOpen(false);
      CompactDateInput.configureDatePickerInput(birthDatePicker, messageProperties);
      refreshBirthDate();
      birthDatePicker.setWidthFull();
      birthDatePicker.setClearButtonVisible(true);
      birthDatePicker.setMax(LocalDate.now(ZoneId.systemDefault()));

      enabledCheckbox = new Checkbox();
      enabledCheckbox.addClassName("admin-user-enabled-checkbox");
      enabledCaption = new Span(messageProperties.getAdminUsersFieldEnabled());
      enabledHint = new Span(messageProperties.getAdminUsersEnabledHint());
      enabledHint.addClassName("admin-user-enabled-hint");
      enabledHint.getElement().setAttribute(ARIA_HIDDEN, "true");
      Div enabledLabel = new Div(enabledCaption, enabledHint);
      enabledLabel.addClassName("admin-user-enabled-label");
      enabledCheckbox.setLabelComponent(enabledLabel);
      enabledCheckbox.setAriaDescribedBy(enabledHint);
      rolesField = new MultiSelectComboBox<>(messageProperties.getAdminUsersFieldRoles());
      rolesField.addClassName("admin-user-roles");
      rolesField.setWidthFull();
      rolesField
          .setItems(RoleType.values())
          .setFilter(
              role -> role != RoleType.ROLE_SUPERADMIN || userService.canManageSuperadmins());
      rolesField.setItemLabelGenerator(AdminUserManagementSection.this::formatRoleType);
      rolesField.setClassNameGenerator(role -> "admin-user-role-option");
      rolesField.setRequiredIndicatorVisible(true);
      rolesField.setHelperText(messageProperties.getAdminUsersRolesHelper());
      rolesSummary = new Span();
      rolesSummary.addClassName("admin-user-role-summary");
      rolesSummary.getElement().setAttribute(ARIA_HIDDEN, "true");
      rolesField.addValueChangeListener(event -> refreshRolesSummary());
      refreshRolesSummary();

      technicalInfo =
          new Span(
              editMode
                  ? messageProperties.getAdminUsersTechnicalInfoEdit()
                  : messageProperties.getAdminUsersTechnicalInfoCreate());
      technicalInfo.addClassName(LumoUtility.TextColor.SECONDARY);
      SvgIcon securityIcon = AppIcon.SHIELD_CHECK.create();
      securityIcon.getElement().setAttribute(ARIA_HIDDEN, "true");
      securityNote = new Div(securityIcon, technicalInfo);
      securityNote.addClassNames("cosmos-panel-hint", "admin-user-security-note");
    }

    private void refreshBirthDate() {

      birthDatePicker.setLocale(
          VaadinSession.getCurrent() != null
              ? VaadinSession.getCurrent().getLocale()
              : Locale.getDefault());
      CompactDateInput.configureDatePickerInput(birthDatePicker, messageProperties);
      birthDatePicker.setLabel(messageProperties.getRegistrationBirthDateField());
      birthDatePicker.setPlaceholder(messageProperties.getRegistrationBirthDatePlaceholder());
    }

    private void bindFields() {
      binder
          .forField(firstNameField)
          .asRequired(context -> messageProperties.getBaseValidationRequiredMessage())
          .withValidator(
              ValidationHelper::isRequiredTextValid,
              context -> messageProperties.getBaseValidationRequiredMessage())
          .bind(AdminUserFormData::getFirstName, AdminUserFormData::setFirstName);

      binder
          .forField(lastNameField)
          .asRequired(context -> messageProperties.getBaseValidationRequiredMessage())
          .withValidator(
              ValidationHelper::isRequiredTextValid,
              context -> messageProperties.getBaseValidationRequiredMessage())
          .bind(AdminUserFormData::getLastName, AdminUserFormData::setLastName);

      emailCheckFailed = new AtomicBoolean();
      binder
          .forField(emailField)
          .asRequired(context -> messageProperties.getBaseValidationRequiredMessage())
          .withValidator(
              ValidationHelper::isEmailValid,
              context -> messageProperties.getBaseValidationEmailInvalidMessage())
          .withValidator((email, context) -> {
            emailCheckFailed.set(false);
            try {
              return userService.emailExistsForOtherUser(
                      ValidationHelper.normalize(email), formData.getId())
                  ? ValidationResult.error(messageProperties.getBaseValidationEmailExistsMessage())
                  : ValidationResult.ok();
            } catch (RuntimeException exception) {
              log.error("E-Mail-Vorprüfung fehlgeschlagen", safeFailure(exception));
              emailCheckFailed.set(true);
              return ValidationResult.error(messageProperties.getAdminUsersErrorSaveFailed());
            }
          })
          .bind(AdminUserFormData::getEmail, AdminUserFormData::setEmail);

      bindPasswordFields();

      binder
          .forField(genderComboBox)
          .bind(AdminUserFormData::getGender, AdminUserFormData::setGender);

      binder
          .forField(birthDatePicker)
          .withValidator(
              ValidationHelper::isBirthDateValid,
              context -> messageProperties.getBaseValidationBirthDatePastMessage())
          .bind(AdminUserFormData::getBirthDate, AdminUserFormData::setBirthDate);

      binder
          .forField(enabledCheckbox)
          .bind(AdminUserFormData::isEnabled, AdminUserFormData::setEnabled);

      binder
          .forField(rolesField)
          .withValidator(
              selectedRoles -> selectedRoles != null && !selectedRoles.isEmpty(),
              context -> messageProperties.getBaseValidationRequiredMessage())
          .bind(AdminUserFormData::getRoles, AdminUserFormData::setRoles);

      binder.readBean(formData);
    }

    private void bindPasswordFields() {
      showPasswordErrors = new AtomicBoolean();
      showConfirmPasswordErrors = new AtomicBoolean();
      passwordBinding =
          binder
              .forField(passwordField)
              .withValidationStatusHandler(status -> {
                boolean showError = showPasswordErrors.get() && status.isError();
                passwordField.setErrorMessage(showError ? status.getMessage().orElse("") : "");
                passwordField.setInvalid(showError);
              })
              .withValidator(
                  value -> ValidationHelper.isPasswordSecure(value, editMode),
                  context ->
                      editMode
                          ? messageProperties.getAdminUsersPasswordValidationEdit()
                          : messageProperties.getAdminUsersPasswordValidationCreate())
              .bind(AdminUserFormData::getPassword, AdminUserFormData::setPassword);

      confirmPasswordBinding =
          binder
              .forField(confirmPasswordField)
              .withValidationStatusHandler(status -> {
                boolean showError = showConfirmPasswordErrors.get() && status.isError();
                confirmPasswordField.setErrorMessage(
                    showError ? status.getMessage().orElse("") : "");
                confirmPasswordField.setInvalid(showError);
              })
              .withValidator(
                  value ->
                      !ValidationHelper.isRequiredTextValid(passwordField.getValue())
                          || ValidationHelper.isRequiredTextValid(value),
                  context -> messageProperties.getBaseValidationRequiredMessage())
              .withValidator(
                  value ->
                      !ValidationHelper.isRequiredTextValid(passwordField.getValue())
                          || Objects.equals(passwordField.getValue(), value),
                  context -> messageProperties.getBaseValidationPasswordConfirmMessage())
              .bind(AdminUserFormData::getConfirmPassword, AdminUserFormData::setConfirmPassword);
    }

    private void configurePasswordFeedback() {
      passwordField.addValueChangeListener(event -> {
        boolean passwordEntered = ValidationHelper.isRequiredTextValid(event.getValue());
        refreshPasswordHint();
        confirmPasswordField.setEnabled(passwordEntered);
        confirmPasswordField.setRequiredIndicatorVisible(passwordEntered);
        if (!passwordEntered) {
          showConfirmPasswordErrors.set(false);
          confirmPasswordField.clear();
        }
        confirmPasswordBinding.validate();
      });

      // Sofort prüfen, aber Fehlermeldungen erst nach dem Verlassen des Feldes anzeigen.
      passwordField.addFocusListener(event -> {
        showPasswordErrors.set(false);
        passwordField.setInvalid(false);
        passwordField.setErrorMessage("");
      });
      passwordField.addBlurListener(event -> {
        showPasswordErrors.set(true);
        passwordBinding.validate();
      });
      confirmPasswordField.addFocusListener(event -> {
        showConfirmPasswordErrors.set(false);
        confirmPasswordField.setInvalid(false);
        confirmPasswordField.setErrorMessage("");
      });
      confirmPasswordField.addBlurListener(event -> {
        showConfirmPasswordErrors.set(true);
        confirmPasswordBinding.validate();
      });
    }

    private void refreshPasswordHint() {
      boolean passwordRequired =
          !editMode || ValidationHelper.isRequiredTextValid(passwordField.getValue());
      passwordHint.setText(
          passwordRequired
              ? messageProperties.getAdminUsersPasswordHelperCreate()
              : messageProperties.getAdminUsersPasswordHelperEdit());
    }

    private void configureActions() {
      saveButton = new Button(messageProperties.getAdminUsersSave());
      saveButton.setPrefixComponent(AppIcon.CHECK.create());
      saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
      saveButton.setEnabled(binder.isValid());

      binder.addStatusChangeListener(event -> saveButton.setEnabled(binder.isValid()));

      saveButton.addClickListener(event -> saveUser());

      cancelButton =
          new Button(messageProperties.getAdminUsersCancel(), e -> editUserDialog.close());
      cancelButton.setPrefixComponent(AppIcon.CLOSE.create());
    }

    private void composeLayout() {
      HorizontalLayout nameRow = new HorizontalLayout(firstNameField, lastNameField);
      nameRow.setWidthFull();
      nameRow.addClassName(COSMOS_FORM_ROW_CLASS);
      nameRow.expand(firstNameField, lastNameField);

      Div passwordColumn = new Div(passwordField, passwordHint);
      passwordColumn.addClassName("admin-user-password-column");
      Div confirmPasswordColumn = new Div(confirmPasswordField, confirmPasswordHint);
      confirmPasswordColumn.addClassName("admin-user-password-column");
      HorizontalLayout passwordRow = new HorizontalLayout(passwordColumn, confirmPasswordColumn);
      passwordRow.setWidthFull();
      passwordRow.setSpacing(false);
      passwordRow.addClassName(COSMOS_FORM_ROW_CLASS);
      passwordRow.expand(passwordColumn, confirmPasswordColumn);
      Div passwordFields = new Div(passwordRow);
      passwordFields.addClassName("admin-user-password-fields");

      HorizontalLayout masterDataRow = new HorizontalLayout(genderComboBox, birthDatePicker);
      masterDataRow.setWidthFull();
      masterDataRow.addClassName(COSMOS_FORM_ROW_CLASS);
      masterDataRow.expand(genderComboBox, birthDatePicker);

      HorizontalLayout footer = new HorizontalLayout();
      footer.addClassName("cosmos-dialog-actions");
      EqualButtonWidths.configureEqualButtonWidths(footer);
      footer.setWidthFull();
      footer.setAlignItems(Alignment.CENTER);
      if (editMode) {
        Button deleteButton =
            new Button(
                messageProperties.getAdminUsersDelete(),
                AppIcon.TRASH.create(),
                event -> deleteUser(user, editUserDialog::close));
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteButton.addClassName(DELETE_BUTTON);
        deleteButton.setEnabled(!isCurrentUser(user));
        footer.add(deleteButton);
      }
      cancelButton.addClassName("cosmos-dialog-cancel");
      footer.add(cancelButton, saveButton);

      Div roleSelection = new Div(rolesField, rolesSummary);
      roleSelection.addClassName("admin-user-role-field");
      Div permissionsRow = new Div(roleSelection, enabledCheckbox);
      permissionsRow.addClassName("admin-user-permissions");
      VerticalLayout dialogLayout =
          new VerticalLayout(nameRow, emailField, passwordFields, masterDataRow, permissionsRow);
      if (editMode && externalIdentityService != null) {
        externalIdentityService
            .adminSignInSummary(user.getId())
            .ifPresent(summary -> {
              signInSummary = summary;
              dialogLayout.add(signInDetails());
            });
      }
      Div footerSection = new Div(securityNote, footer);
      footerSection.addClassName("admin-user-editor-footer");
      editUserDialog.getFooter().add(footerSection);
      dialogLayout.setPadding(false);
      dialogLayout.setSpacing(false);
      dialogLayout.addClassName("cosmos-dialog-form");

      editUserDialog.add(dialogLayout);
    }

    private HorizontalLayout signInDetails() {
      signInTitle.addClassNames(COSMOS_PANEL_TITLE_CLASS, "admin-user-sign-in-title");
      registeredVia.setId("admin-user-registered-via");
      availableMethods.setId("admin-user-sign-in-methods");
      registrationLabel.addClassName("admin-user-registration-label");
      methodsLabel.addClassName("admin-user-methods-label");
      Div registration = new Div(registrationLabel, registeredVia);
      Div methods = new Div(methodsLabel, availableMethods);
      registration.addClassName("admin-user-sign-in-detail");
      methods.addClassName("admin-user-sign-in-detail");
      HorizontalLayout fields = new HorizontalLayout(registration, methods);
      fields.setWidthFull();
      fields.addClassName(COSMOS_FORM_ROW_CLASS);
      fields.expand(registration, methods);
      VerticalLayout content = new VerticalLayout(signInTitle, fields);
      content.setPadding(false);
      content.setMinWidth("0");
      SvgIcon infoIcon = AppIcon.INFO_CIRCLE.create();
      infoIcon.getElement().setAttribute(ARIA_HIDDEN, "true");
      HorizontalLayout section = new HorizontalLayout(infoIcon, content);
      section.setWidthFull();
      section.setPadding(false);
      section.setSpacing(false);
      section.setAlignItems(Alignment.START);
      section.expand(content);
      section.addClassNames("cosmos-panel-hint", "admin-user-sign-in");
      refreshSignInDetails();
      return section;
    }

    private void refreshSignInDetails() {
      if (signInSummary == null) {
        return;
      }
      signInTitle.setText(messageProperties.getSignInDetailsTitle());
      registrationLabel.setText(messageProperties.getSignInRegisteredVia());
      registeredVia.setText(
          messageProperties.getRegistrationChannelLabel(signInSummary.registration()));
      registeredVia
          .getElement()
          .setAttribute(
              "data-self-registered", Boolean.toString(signInSummary.registration() != null));
      methodsLabel.setText(messageProperties.getSignInAvailableMethods());
      availableMethods.removeAll();
      if (signInSummary.available().isEmpty()) {
        availableMethods.add(new Span(messageProperties.getSignInNone()));
      } else {
        signInSummary.available().stream()
            .sorted()
            .map(messageProperties::getSignInMethodLabel)
            .forEach(label -> {
              Badge badge = new Badge(label);
              badge.addThemeVariants(BadgeVariant.SUCCESS);
              availableMethods.add(badge);
            });
      }
    }

    private void refreshRolesSummary() {
      String summary = messageProperties.getAdminUsersSelectedRoles(rolesField.getValue().size());
      rolesSummary.setText(summary);
      rolesField.setPlaceholder(summary);
    }

    private void registerTextRefresh() {
      Runnable refreshErrors =
          () -> {
            refreshBirthDate();
            dialogTitle.setText(
                editMode
                    ? messageProperties.getAdminUsersEditDialogTitle()
                    : messageProperties.getAdminUsersCreateDialogTitle());
            editUserDialog.getElement().setProperty(ARIA_LABEL_PROPERTY, dialogTitle.getText());
            dialogDescription.setText(messageProperties.getAdminUsersDialogDescription());
            closeEditUserDialogButton.setAriaLabel(messageProperties.getAdminUsersCloseDialog());
            confirmPasswordField.setLabel(messageProperties.getPublicConfirmPasswordField());
            confirmPasswordHint.setText(messageProperties.getAdminUsersPasswordConfirmHelper());
            confirmPasswordField.setPlaceholder(
                messageProperties.getAccountConfirmPasswordPlaceholder());
            saveButton.setText(messageProperties.getAdminUsersSave());
            cancelButton.setText(messageProperties.getAdminUsersCancel());
            rolesField.setLabel(messageProperties.getAdminUsersFieldRoles());
            rolesField.setHelperText(messageProperties.getAdminUsersRolesHelper());
            rolesField.getDataProvider().refreshAll();
            refreshRolesSummary();
            refreshSignInDetails();
            enabledCaption.setText(messageProperties.getAdminUsersFieldEnabled());
            enabledHint.setText(messageProperties.getAdminUsersEnabledHint());
            passwordField.setPlaceholder(
                editMode
                    ? messageProperties.getAdminUsersPasswordPlaceholderEdit()
                    : messageProperties.getAccountPasswordPlaceholder());
            refreshPasswordHint();
            technicalInfo.setText(
                editMode
                    ? messageProperties.getAdminUsersTechnicalInfoEdit()
                    : messageProperties.getAdminUsersTechnicalInfoCreate());
            if (binder
                .getFields()
                .anyMatch(field ->
                    field
                            instanceof
                            com.vaadin.flow.component.shared.HasValidationProperties validation
                        && validation.isInvalid())) {
              binder.validate();
            }
          };
      dialogErrorRefreshers.add(refreshErrors);
      editUserDialog.addOpenedChangeListener(event -> {
        if (!event.isOpened()) {
          dialogErrorRefreshers.remove(refreshErrors);
        }
      });
      editUserDialog.addDetachListener(event -> dialogErrorRefreshers.remove(refreshErrors));
    }

    private void saveUser() {
      try {
        if (!validateForm()) {
          return;
        }

        Set<RoleEntity> roleEntities = resolveRoles(formData.getRoles());
        if (roleEntities.size() != formData.getRoles().size()) {
          showError(messageProperties::getAdminUsersErrorRoleMissing);
          return;
        }

        if (!validateAccountChange(roleEntities)) {
          return;
        }
        persistUser(roleEntities);
      } catch (RuntimeException exception) {
        showMutationError(exception, false);
      }
    }

    private boolean validateForm() {
      try {
        emailCheckFailed.set(false);
        showPasswordErrors.set(true);
        showConfirmPasswordErrors.set(true);
        binder.writeBean(formData);
      } catch (ValidationException _) {
        showError(
            emailCheckFailed.get()
                ? messageProperties::getAdminUsersErrorSaveFailed
                : messageProperties::getAdminUsersValidationCheckFields);
        return false;
      }
      // Status listeners may recheck validity during writeBean. Never
      // proceed if such a check failed technically either.
      if (emailCheckFailed.get()) {
        showError(messageProperties::getAdminUsersErrorSaveFailed);
        return false;
      }

      return true;
    }

    private boolean validateAccountChange(Set<RoleEntity> roleEntities) {
      if (isCurrentUser(user) && !formData.isEnabled()) {
        showError(messageProperties::getAdminUsersErrorDisableOwnUser);
        return false;
      }

      if (isCurrentUser(user)
          && formData.getRoles().stream().noneMatch(RoleType::isAdministrative)) {
        showError(messageProperties::getAdminUsersErrorRemoveOwnAdminRole);
        return false;
      }

      if (userService.wouldRemoveLastEnabledSuperadmin(
          formData.getId(), formData.isEnabled(), roleEntities)) {
        showError(messageProperties::getAdminUsersErrorLastActiveSuperadmin);
        return false;
      }

      return true;
    }

    private void persistUser(Set<RoleEntity> roleEntities) {
      UserEntity edited =
          UserEntity.builder()
              .id(formData.getId())
              .firstName(clean(formData.getFirstName()))
              .lastName(clean(formData.getLastName()))
              .email(clean(formData.getEmail()))
              .gender(formData.getGender())
              .birthDate(formData.getBirthDate())
              .enabled(formData.isEnabled())
              .verificationPending(false)
              .preferredLocale(CustomI18NProvider.getCurrentLocale())
              .roleEntities(roleEntities)
              .build();
      if (editMode) {
        userService.updateAdminUser(edited, expectedVersion, formData.getPassword());
      } else {
        userService.createAdminUser(edited, formData.getPassword());
      }
      editUserDialog.close();
      refreshGrid();

      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getBaseSuccessTitle,
              () ->
                  editMode
                      ? messageProperties.getAdminUsersSaveSuccessUpdated()
                      : messageProperties.getAdminUsersSaveSuccessCreated(),
              NotificationHelper.NotificationType.SUCCESS);
    }

    private Set<RoleEntity> resolveRoles(Set<RoleType> selectedRoles) {
      return Optional.ofNullable(selectedRoles).orElseGet(Set::of).stream()
          .map(roleService::findByName)
          .flatMap(Optional::stream)
          .collect(Collectors.toSet());
    }
  }

  private Component createActionButtons(UserEntity user) {
    Button editButton = new Button(AppIcon.EDIT.create(), e -> openDialog(user));
    editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    editButton.setTooltipText(messageProperties.getAdminUsersEdit());
    boolean protectedAccount =
        user.hasRole(RoleType.ROLE_SUPERADMIN) && !userService.canManageSuperadmins();
    editButton.setEnabled(!protectedAccount);

    Button deleteButton = new Button(AppIcon.TRASH.create(), e -> deleteUser(user));
    deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
    deleteButton.addClassName(DELETE_BUTTON);
    deleteButton.setTooltipText(messageProperties.getAdminUsersDelete());
    deleteButton.setEnabled(!isCurrentUser(user) && !protectedAccount);

    HorizontalLayout actions = new HorizontalLayout(editButton, deleteButton);
    actions.addClassName("admin-user-table-actions");
    actions.setPadding(false);
    actions.setSpacing(true);
    actions.setJustifyContentMode(JustifyContentMode.CENTER);
    return actions;
  }

  private Component createEnabledBadge(UserEntity user) {
    boolean enabled = user.isEnabled();
    Span badge =
        new Span(
            enabled ? messageProperties.getAdminUsersYes() : messageProperties.getAdminUsersNo());
    badge.getElement().getThemeList().add("badge " + (enabled ? "success" : "error"));
    badge.addClassName("admin-user-table-enabled");
    return badge;
  }

  private Component createUserIdentity(UserEntity user) {
    OverlayUserIcon avatar =
        new OverlayUserIcon(messageProperties, user, user.getStatus(), IconSize.PIXEL_32);
    if (userAvatarService != null) {
      avatar.configureAvatar(
          userAvatarService,
          avatarStates.getOrDefault(user.getId(), UserAvatarService.State.initial()));
    }
    avatar.addClassName("admin-user-table-avatar");
    Span name = new Span(getFullName(user));
    name.addClassName("admin-user-table-name");
    Div copy = new Div(name);
    if (isCurrentUser(user)) {
      Span self = new Span(messageProperties.getAdminUsersSelf());
      self.addClassName("admin-user-self");
      copy.add(self);
    }
    HorizontalLayout identity = new HorizontalLayout(avatar, copy);
    identity.addClassName("admin-user-table-identity");
    identity.setAlignItems(Alignment.CENTER);
    identity.setPadding(false);
    identity.setSpacing(true);
    return identity;
  }

  private Component createStatusBadge(UserEntity user) {
    UserStatus status = user.getStatus();
    Span badge = new Span(status == null ? "-" : messageProperties.getUserStatusLabel(status));
    String variant =
        status == null
            ? "contrast"
            : switch (status) {
              case AVAILABLE -> "success";
              case ABSENT -> "warning";
              case EMPLOYED -> "error";
              case OFFLINE -> "contrast";
            };
    badge.getElement().getThemeList().add("badge " + variant);
    badge.addClassName("admin-user-table-status");
    if (status != null) {
      AppIcon statusIcon =
          switch (status) {
            case AVAILABLE -> AppIcon.CHECK_CIRCLE;
            case EMPLOYED -> AppIcon.CIRCLE_MINUS;
            case ABSENT -> AppIcon.CLOCK;
            case OFFLINE -> AppIcon.CLOSE_CIRCLE;
          };
      SvgIcon icon = statusIcon.create();
      icon.getElement().setAttribute(ARIA_HIDDEN, "true");
      badge.addComponentAtIndex(0, icon);
    }
    return badge;
  }

  private Component createRoleBadges(UserEntity user) {
    Div badges = new Div();
    badges.addClassName("admin-user-table-roles");
    Optional.ofNullable(user.getRoleEntities()).orElseGet(Set::of).stream()
        .map(RoleEntity::getName)
        .map(this::formatRoleType)
        .sorted(String.CASE_INSENSITIVE_ORDER)
        .forEach(roleName -> {
          Span badge = new Span(roleName);
          badge.addClassName("cosmos-admin-role-chip");
          badges.add(badge);
        });
    return badges;
  }

  private String formatRoleNames(UserEntity user) {
    return Optional.ofNullable(user.getRoleEntities()).orElseGet(Set::of).stream()
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

  private Component createLastActivity(UserEntity user) {
    Instant activity = user.getLastActivity();
    Instant now = userManagementClock.instant();
    String text =
        activity != null && activity.isAfter(now)
            ? formatDateTime(activity)
            : AdminUserActivityText.format(activity, now, messageProperties);
    Span label = new Span(text);
    if (activity != null) {
      label.getElement().setAttribute("title", formatDateTime(activity));
    }
    return label;
  }

  private String formatDateTime(Instant dateTime) {
    if (dateTime == null) {
      return "-";
    }
    Locale locale =
        VaadinSession.getCurrent() != null && VaadinSession.getCurrent().getLocale() != null
            ? VaadinSession.getCurrent().getLocale()
            : Locale.getDefault();
    return LAST_ACTIVITY_FORMATTER.withLocale(locale).format(dateTime);
  }

  private String formatRoleType(RoleType roleType) {
    if (roleType == null) {
      return "-";
    }
    return switch (roleType) {
      case ROLE_SUPERADMIN -> messageProperties.getRoleSuperadmin();
      case ROLE_GUEST -> messageProperties.getRoleGuest();
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
    return securityService
        .getCurrentUser()
        .map(currentUser -> Objects.equals(currentUser.getId(), user.getId()))
        .orElse(false);
  }

  private void showError(java.util.function.Supplier<String> message) {
    NotificationHelper.getInstance()
        .showNotification(
            messageProperties::getBaseFailedTitle,
            message,
            NotificationHelper.NotificationType.ERROR);
  }

  private void showMutationError(RuntimeException exception, boolean deleting) {
    log.error(
        "Benutzer {} fehlgeschlagen", deleting ? "löschen" : "speichern", safeFailure(exception));
    if (exception instanceof org.springframework.security.access.AccessDeniedException) {
      showError(messageProperties::getRolesPermissionDenied);
    } else if (exception instanceof OptimisticLockingFailureException) {
      showError(messageProperties::getBaseUserUpdateConflict);
    } else if (!deleting && EmailConflict.isDuplicate(exception)) {
      showError(messageProperties::getBaseValidationEmailExistsMessage);
    } else if (exception instanceof IllegalStateException
        && "Der letzte aktive Superadmin muss erhalten bleiben.".equals(exception.getMessage())) {
      // Nur dieses fachliche Schutzsignal zuordnen, keine beliebigen IllegalStateExceptions.
      showError(
          deleting
              ? messageProperties::getAdminUsersErrorDeleteLastSuperadmin
              : messageProperties::getAdminUsersErrorLastActiveSuperadmin);
    } else if (!deleting
        && exception instanceof IllegalArgumentException
        && Set.of(
                "First name must not be blank",
                "Last name must not be blank",
                "Invalid email",
                "Gender must not be null",
                "Birth date must be in the past",
                "Roles must not be null",
                "Invalid role",
                "Invalid password",
                "Encoded password must not be blank",
                "Locale must not be null",
                "Status must not be null")
            .contains(Optional.ofNullable(exception.getMessage()).orElse(""))) {
      showError(messageProperties::getAdminUsersValidationCheckFields);
    } else {
      showError(
          deleting
              ? messageProperties::getAdminUsersErrorDeleteFailed
              : messageProperties::getAdminUsersErrorSaveFailed);
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
    private String confirmPassword;
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
      formData.confirmPassword = "";
      formData.gender = user.getGender();
      formData.birthDate = user.getBirthDate();
      formData.enabled = user.isEnabled();
      formData.roles =
          Optional.ofNullable(user.getRoleEntities()).orElseGet(Set::of).stream()
              .map(RoleEntity::getName)
              .collect(Collectors.toSet());
      return formData;
    }
  }

  // Die dauerhafte HTTP-Sitzung speichert nur Sicherheitsdaten. Dieses UI enthält
  // JPA-/Formular-/Uploadzustand und wird nach Neustart bewusst frisch aufgebaut.
  @Serial
  private void writeObject(ObjectOutputStream output) throws IOException {
    throw new NotSerializableException("Runtime UI must be rebuilt from the authenticated session");
  }

  @Serial
  private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
    throw new InvalidObjectException("Runtime UI cannot be restored from a Java object graph");
  }
}
