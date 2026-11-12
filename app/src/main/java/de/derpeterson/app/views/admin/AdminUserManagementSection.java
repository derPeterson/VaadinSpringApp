package de.derpeterson.app.views.admin;

import static de.derpeterson.app.helper.logging.LogSafety.safeFailure;
import static de.derpeterson.app.ui.components.CosmosCssClasses.DELETE_BUTTON;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_CARD;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_DESCRIPTION;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_HEADER;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_HEADING;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_ICON;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_TITLE;
import static de.derpeterson.app.ui.components.CosmosCssClasses.REFERENCE_OVERLAY;

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
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.Autocomplete;
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
import de.derpeterson.app.service.AdminAccountMailService;
import de.derpeterson.app.service.ExternalIdentityService;
import de.derpeterson.app.service.ExternalIdentityService.SignInSummary;
import de.derpeterson.app.service.RoleService;
import de.derpeterson.app.service.UserAvatarService;
import de.derpeterson.app.service.UserService;
import de.derpeterson.app.ui.components.AppIcon;
import de.derpeterson.app.ui.components.BrowserUnsavedChangesGuard;
import de.derpeterson.app.ui.components.CosmosBreadcrumbsComponent.ParentPath;
import de.derpeterson.app.ui.components.CosmosCardComponent;
import de.derpeterson.app.ui.components.CosmosComboBoxStyling;
import de.derpeterson.app.ui.components.CosmosPagination;
import de.derpeterson.app.ui.components.CosmosSectionHeaderComponent;
import de.derpeterson.app.ui.components.CosmosSegmentedTabs;
import de.derpeterson.app.ui.components.DetailsToggleButton;
import de.derpeterson.app.ui.components.EmailVerificationBadge;
import de.derpeterson.app.ui.components.OverlayUserIcon;
import de.derpeterson.app.ui.components.UnsavedChangesDialog;
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
import java.time.ZoneOffset;
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
import org.springframework.security.access.AccessDeniedException;

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

  // Seitenkopf und Tabelle
  private final Span userManagementTitleSpan;
  private final H2 usersCardTitleH2;
  private final Span usersCardDescriptionSpan;
  private final Span accountCountSpan;
  private final Span userManagementDescriptionSpan;
  private final Button addUserButton;
  private final TextField searchField;
  private final Button refreshButton;
  private final Grid<UserEntity> userGrid = new Grid<>(UserEntity.class, false);

  // Eingaben im Filterpanel
  private final CosmosSegmentedTabs<AdminUserFilters.Enabled> enabledFilterTabs =
      new CosmosSegmentedTabs<>();
  private final CheckboxGroup<UserStatus> statusFilter = new CheckboxGroup<>();
  private final CheckboxGroup<RoleType> roleFilter = new CheckboxGroup<>();
  private final AdminUserActivityFilter lastActivityFilter;
  private final AdminUserFilterBar filterBar;

  // Tabellenspalten und Daten der aktuell geladenen Seite
  private Grid.Column<UserEntity> nameColumn;
  private Grid.Column<UserEntity> emailColumn;
  private Grid.Column<UserEntity> enabledColumn;
  private Grid.Column<UserEntity> statusColumn;
  private Grid.Column<UserEntity> rolesColumn;
  private Grid.Column<UserEntity> lastActivityColumn;
  private Grid.Column<UserEntity> actionsColumn;
  private final Span actionsHeaderSpan = new Span();

  private final CosmosPagination userTablePagination;
  private List<UserEntity> users = new ArrayList<>();
  private List<UserEntity> filteredUsers = new ArrayList<>();
  private AdminUserFilters appliedFilters =
      new AdminUserFilters("", null, Set.of(), Set.of(), null);
  private int currentPage = 0;
  private int pageSize = 10;

  private transient UserAvatarService userAvatarService;
  private Map<Long, UserAvatarService.State> userAvatarStates = Map.of();

  private transient ExternalIdentityService externalIdentityService;
  private transient AdminAccountMailService adminAccountMailService;

  public void configureAccountMailActions(AdminAccountMailService adminAccountMailService) {
    this.adminAccountMailService = adminAccountMailService;
  }

  private final List<Runnable> dialogTextRefreshActions = new ArrayList<>();

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

    userManagementTitleSpan = new Span();
    usersCardTitleH2 = new H2();
    usersCardTitleH2.addClassName(COSMOS_PANEL_TITLE_CLASS);
    usersCardDescriptionSpan = new Span();
    usersCardDescriptionSpan.addClassName(COSMOS_PANEL_DESCRIPTION_CLASS);
    accountCountSpan = new Span();
    accountCountSpan.addClassNames("cosmos-panel-meta", "admin-accounts-metric");

    userManagementDescriptionSpan = new Span();
    userManagementDescriptionSpan.addClassNames(LumoUtility.TextColor.SECONDARY);

    addUserButton = new Button();
    addUserButton.setPrefixComponent(AppIcon.USER_PLUS.create());
    addUserButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    addUserButton.addClickListener(clickEvent -> openDialog(new UserEntity()));

    // Die Suche gehört zum Filterentwurf. Erst Anwenden verändert die Tabelle.
    searchField = new TextField();
    searchField.setPrefixComponent(AppIcon.SEARCH.create());
    searchField.setClearButtonVisible(true);
    searchField.setWidthFull();
    searchField.setMaxWidth("360px");
    searchField.setValueChangeMode(ValueChangeMode.EAGER);

    refreshButton = new Button(AppIcon.REFRESH.create(), event -> refreshUsersWithFeedback());
    refreshButton.addClassName("admin-user-refresh");

    userTablePagination =
        new CosmosPagination(
            messageProperties,
            selectedPageIndex -> {
              currentPage = selectedPageIndex;
              updateGridPage();
            },
            selectedPageSize -> {
              pageSize = selectedPageSize;
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
                enabledFilterTabs, statusFilter, roleFilter, lastActivityFilter),
            this::applyFilterInputs);

    Div paginationFooterDiv = new Div(userTablePagination);
    paginationFooterDiv.addClassName("cosmos-panel-footer");
    VerticalLayout usersCardContentLayout =
        new VerticalLayout(createUsersCardHeader(), filterBar, userGrid, paginationFooterDiv);
    usersCardContentLayout.setPadding(false);
    usersCardContentLayout.setSpacing(false);
    usersCardContentLayout.addClassName("cosmos-admin-users-content");
    usersCardContentLayout.setWidthFull();
    usersCardContentLayout.setAlignItems(Alignment.START);

    CosmosCardComponent usersCardComponent = new CosmosCardComponent(usersCardContentLayout);
    usersCardComponent.addClassNames("cosmos-admin-users-card", PANEL_CARD);
    add(
        new CosmosSectionHeaderComponent(
            messageProperties,
            ParentPath.ADMINISTRATION,
            userManagementTitleSpan,
            userManagementDescriptionSpan),
        usersCardComponent);

    refreshTexts();
    refreshGrid();
  }

  public void configureAvatars(UserAvatarService userAvatarService) {
    this.userAvatarService = userAvatarService;
    updateGridPage();
  }

  public void configureSignInDetails(ExternalIdentityService externalIdentityService) {
    this.externalIdentityService = externalIdentityService;
  }

  private Div createUsersCardHeader() {
    SvgIcon usersSvgIcon = AppIcon.USERS.create();
    usersSvgIcon.getElement().setAttribute(ARIA_HIDDEN, "true");
    Div usersIconBadgeDiv = new Div(usersSvgIcon);
    usersIconBadgeDiv.addClassName(PANEL_ICON);
    Div usersCardHeadingDiv = new Div(usersCardTitleH2, usersCardDescriptionSpan);
    usersCardHeadingDiv.addClassName(COSMOS_PANEL_HEADING_CLASS);
    Div usersCardHeaderDiv = new Div(usersIconBadgeDiv, usersCardHeadingDiv, accountCountSpan);
    usersCardHeaderDiv.addClassName(PANEL_HEADER);
    return usersCardHeaderDiv;
  }

  public void refreshTexts() {
    usersCardTitleH2.setText(messageProperties.getAdminUsersCardTitle());
    usersCardDescriptionSpan.setText(messageProperties.getAdminUsersCardDescription());
    userManagementTitleSpan.setText(messageProperties.getAdminUsersTitle());
    userManagementDescriptionSpan.setText(messageProperties.getAdminUsersDescription());
    addUserButton.setText(messageProperties.getAdminUsersAdd());

    searchField.setLabel(messageProperties.getAdminUsersSearchLabel());
    searchField.setAriaLabel(messageProperties.getAdminUsersSearchLabel());
    searchField.setPlaceholder(messageProperties.getAdminUsersSearchPlaceholder());

    refreshButton.setTooltipText(messageProperties.getAdminUsersRefreshTooltip());
    refreshButton.setAriaLabel(messageProperties.getAdminUsersRefreshTooltip());
    enabledFilterTabs.setLabel(messageProperties.getAdminUsersGridEnabled());

    enabledFilterTabs.setItemLabelGenerator(enabled ->
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
      actionsHeaderSpan.setText(messageProperties.getAdminUsersGridActions());
    }

    refreshEmptyState();
    updateGridPage();
    updateActiveFilters();
    List.copyOf(dialogTextRefreshActions).forEach(Runnable::run);
  }

  private void refreshEmptyState() {
    Span emptyGridTitleSpan = new Span(messageProperties.getAdminUsersGridEmptyState());
    emptyGridTitleSpan.addClassName("admin-grid-empty-title");
    Span emptyGridHintSpan = new Span(messageProperties.getAdminUsersEmptyHint());
    Div emptyGridMessageDiv =
        new Div(AppIcon.USERS.create(), emptyGridTitleSpan, emptyGridHintSpan);
    emptyGridMessageDiv.addClassName("admin-grid-empty");
    userGrid.setEmptyStateComponent(emptyGridMessageDiv);
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
                    userEntity -> normalizeSearchText(userEntity.getEmail()),
                    String.CASE_INSENSITIVE_ORDER))
            .setFlexGrow(1);

    enabledColumn =
        userGrid
            .addComponentColumn(this::createEnabledBadge)
            .setHeader(messageProperties.getAdminUsersGridEnabled())
            .setAutoWidth(true)
            .setSortable(true)
            .setComparator(
                Comparator.comparing(userEntity -> !userEntity.isAdministrativelyBlocked()));

    statusColumn =
        userGrid
            .addComponentColumn(this::createStatusBadge)
            .setHeader(messageProperties.getAdminUsersGridStatus())
            .setAutoWidth(true)
            .setSortable(true)
            .setComparator(
                Comparator.comparing(
                    userEntity ->
                        Optional.ofNullable(userEntity.getStatus())
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

    actionsHeaderSpan.setText(messageProperties.getAdminUsersGridActions());
    actionsHeaderSpan.addClassName("admin-users-actions-header");
    actionsColumn =
        userGrid
            .addComponentColumn(this::createActionButtons)
            .setHeader(actionsHeaderSpan)
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
    enabledFilterTabs.setItems(AdminUserFilters.Enabled.values());
    enabledFilterTabs.setValue(AdminUserFilters.Enabled.ALL);
    enabledFilterTabs.addClassName("admin-filter-enabled");
    statusFilter.setItems(UserStatus.values());
    roleFilter.setItems(RoleType.values());
    statusFilter.addClassName("admin-filter-choices");
    roleFilter.addClassName("admin-filter-choices");
  }

  private void refreshStatusChoices() {
    statusFilter.setRenderer(
        new com.vaadin.flow.data.renderer.ComponentRenderer<>(userStatus -> {
          Component statusIconComponent = userStatus.getOutlineComponent(IconSize.PIXEL_16, false);
          Div statusChoiceLabelDiv =
              new Div(
                  statusIconComponent, new Span(messageProperties.getUserStatusLabel(userStatus)));
          statusChoiceLabelDiv.addClassName("admin-filter-status-label");
          statusChoiceLabelDiv
              .getElement()
              .setAttribute("data-status", userStatus.name().toLowerCase(Locale.ROOT));
          return statusChoiceLabelDiv;
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
            enabledFilterTabs.getValue().value(),
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

    Comparator<UserEntity> gridSortComparator =
        userGrid.getSortOrder().stream()
            .map(columnSortOrder ->
                (Comparator<UserEntity>)
                    columnSortOrder.getSorted().getComparator(columnSortOrder.getDirection()))
            .reduce(Comparator::thenComparing)
            .orElse((firstUserEntity, secondUserEntity) -> 0);
    List<UserEntity> sortedUsers = filteredUsers.stream().sorted(gridSortComparator).toList();
    List<UserEntity> pageUsers = sortedUsers.subList(fromIndex, toIndex);

    if (userAvatarService != null) {
      userAvatarStates =
          userAvatarService.states(pageUsers.stream().map(UserEntity::getId).toList());
    }

    userGrid.setItems(pageUsers);
    userTablePagination.showPage(currentPage, filteredUsers.size(), pageSize);

    refreshAccountCount();
  }

  private void refreshAccountCount() {
    accountCountSpan.removeAll();
    accountCountSpan.add(
        new Span(messageProperties.getAdminUsersAccountsLabel()),
        new Span(Integer.toString(users.size())));
    accountCountSpan
        .getElement()
        .setAttribute("aria-label", messageProperties.getAdminUsersAccountCount(users.size()));
  }

  private void deleteUser(UserEntity userEntity) {
    deleteUser(userEntity, () -> {});
  }

  private void deleteUser(UserEntity userEntity, Runnable afterDeleteAction) {
    try {
      if (isCurrentUser(userEntity)) {
        showError(messageProperties::getAdminUsersErrorDeleteOwnUser);
        return;
      }

      if (!userService.canDeleteUser(userEntity)) {
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

    H2 deleteDialogTitleH2 = new H2(messageProperties.getAdminUsersDeleteDialogTitle());
    deleteDialogTitleH2.addClassName(COSMOS_PANEL_TITLE_CLASS);
    Span deleteDialogDescriptionSpan =
        new Span(messageProperties.getAdminUsersDeleteDialogDescription());
    deleteDialogDescriptionSpan.addClassName(COSMOS_PANEL_DESCRIPTION_CLASS);
    Div deleteDialogHeadingDiv = new Div(deleteDialogTitleH2, deleteDialogDescriptionSpan);
    deleteDialogHeadingDiv.addClassName(COSMOS_PANEL_HEADING_CLASS);
    Button closeDeleteUserDialogButton =
        new Button(AppIcon.CLOSE_CIRCLE.create(), event -> deleteUserDialog.close());
    closeDeleteUserDialogButton.addClassNames(
        "cosmos-overlay-close", LumoUtility.TextColor.PRIMARY);
    closeDeleteUserDialogButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    closeDeleteUserDialogButton.setAriaLabel(messageProperties.getAdminUsersCloseDialog());
    Div deleteIconBadgeDiv = new Div(AppIcon.TRASH.create());
    deleteIconBadgeDiv.addClassNames(PANEL_ICON, "cosmos-delete-accent");
    Div deleteDialogHeaderDiv =
        new Div(deleteIconBadgeDiv, deleteDialogHeadingDiv, closeDeleteUserDialogButton);
    deleteDialogHeaderDiv.addClassName("admin-user-dialog-header");
    deleteUserDialog.getHeader().add(deleteDialogHeaderDiv);
    deleteUserDialog.getElement().setProperty(ARIA_LABEL_PROPERTY, deleteDialogTitleH2.getText());

    SvgIcon deleteWarningSvgIcon = AppIcon.TRASH.create();
    deleteWarningSvgIcon.getElement().setAttribute(ARIA_HIDDEN, "true");
    Div deleteWarningIconBadgeDiv = new Div(deleteWarningSvgIcon);
    deleteWarningIconBadgeDiv.addClassNames(PANEL_ICON, "cosmos-delete-accent");
    Span deleteWarningTitleSpan = new Span(messageProperties.getAdminUsersDeleteDialogQuestion());
    deleteWarningTitleSpan.addClassName("admin-user-delete-warning-title");
    Span deleteWarningTextSpan = new Span(messageProperties.getAdminUsersDeleteDialogWarning());
    deleteWarningTextSpan.addClassName(COSMOS_PANEL_DESCRIPTION_CLASS);
    Div deleteWarningHeadingDiv = new Div(deleteWarningTitleSpan, deleteWarningTextSpan);
    deleteWarningHeadingDiv.addClassName(COSMOS_PANEL_HEADING_CLASS);
    Div deleteWarningDiv = new Div(deleteWarningHeadingDiv);
    deleteWarningDiv.addClassName("admin-user-delete-warning");

    OverlayUserIcon deletedUserAvatarIcon =
        new OverlayUserIcon(
            messageProperties, userEntity, userEntity.getStatus(), IconSize.PIXEL_32);
    if (userAvatarService != null) {
      deletedUserAvatarIcon.configureAvatar(
          userAvatarService,
          userAvatarStates.getOrDefault(userEntity.getId(), UserAvatarService.State.initial()));
    }
    deletedUserAvatarIcon.addClassName("admin-user-table-avatar");
    Span deletedUserNameSpan = new Span(getFullName(userEntity));
    deletedUserNameSpan.addClassName("admin-user-delete-name");
    Span deletedUserEmailSpan = new Span(Optional.ofNullable(userEntity.getEmail()).orElse("-"));
    deletedUserEmailSpan.addClassName(COSMOS_PANEL_DESCRIPTION_CLASS);
    Div deletedUserHeadingDiv = new Div(deletedUserNameSpan, deletedUserEmailSpan);
    deletedUserHeadingDiv.addClassName(COSMOS_PANEL_HEADING_CLASS);
    Div deletedUserIdentityDiv = new Div(deletedUserAvatarIcon, deletedUserHeadingDiv);
    deletedUserIdentityDiv.addClassName("admin-user-delete-identity");

    Button cancelUserDeletionButton =
        new Button(messageProperties.getAdminUsersCancel(), event -> deleteUserDialog.close());
    cancelUserDeletionButton.setPrefixComponent(AppIcon.CLOSE.create());
    Button confirmUserDeletionButton =
        new Button(
            messageProperties.getPortalDeleteAction(),
            event -> confirmUserDeletion(userEntity, deleteUserDialog, afterDeleteAction));
    confirmUserDeletionButton.setPrefixComponent(AppIcon.TRASH.create());
    confirmUserDeletionButton.addClassName(DELETE_BUTTON);
    confirmUserDeletionButton.addClassName("admin-user-delete-confirm");

    HorizontalLayout deleteDialogActionsLayout =
        new HorizontalLayout(cancelUserDeletionButton, confirmUserDeletionButton);
    deleteDialogActionsLayout.addClassName("cosmos-dialog-actions");
    EqualButtonWidths.configureEqualButtonWidths(deleteDialogActionsLayout);
    deleteDialogActionsLayout.setWidthFull();
    deleteDialogActionsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

    deleteUserDialog.getFooter().add(deleteDialogActionsLayout);
    VerticalLayout deleteDialogContentLayout =
        new VerticalLayout(deleteWarningDiv, deletedUserIdentityDiv);
    deleteDialogContentLayout.setPadding(false);
    deleteDialogContentLayout.setSpacing(false);
    deleteDialogContentLayout.addClassName("cosmos-dialog-form");

    deleteUserDialog.add(deleteDialogContentLayout);

    Runnable refreshDeleteDialogTextsAction =
        () -> {
          deleteDialogTitleH2.setText(messageProperties.getAdminUsersDeleteDialogTitle());
          deleteUserDialog
              .getElement()
              .setProperty(ARIA_LABEL_PROPERTY, deleteDialogTitleH2.getText());
          deleteDialogDescriptionSpan.setText(
              messageProperties.getAdminUsersDeleteDialogDescription());
          closeDeleteUserDialogButton.setAriaLabel(messageProperties.getAdminUsersCloseDialog());
          deleteWarningTitleSpan.setText(messageProperties.getAdminUsersDeleteDialogQuestion());
          deleteWarningTextSpan.setText(messageProperties.getAdminUsersDeleteDialogWarning());
          cancelUserDeletionButton.setText(messageProperties.getAdminUsersCancel());
          confirmUserDeletionButton.setText(messageProperties.getPortalDeleteAction());
        };
    dialogTextRefreshActions.add(refreshDeleteDialogTextsAction);
    deleteUserDialog.addOpenedChangeListener(event -> {
      if (!event.isOpened()) {
        dialogTextRefreshActions.remove(refreshDeleteDialogTextsAction);
      }
    });
    deleteUserDialog.addDetachListener(
        event -> dialogTextRefreshActions.remove(refreshDeleteDialogTextsAction));
    deleteUserDialog.open();
  }

  private void confirmUserDeletion(
      UserEntity userEntity, Dialog deleteUserDialog, Runnable afterDeleteAction) {
    try {
      userService.deleteUser(userEntity);
      deleteUserDialog.close();
      afterDeleteAction.run();
      refreshGrid();

      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getBaseSuccessTitle,
              messageProperties::getAdminUsersDeleteSuccess,
              NotificationHelper.NotificationType.SUCCESS);
    } catch (RuntimeException exception) {
      showMutationError(exception, true);
    }
  }

  private void openDialog(UserEntity userEntity) {
    new UserEditor(userEntity).open();
  }

  /** Zustand und Rückmeldungen genau eines geöffneten Benutzerformulars. */
  private final class UserEditor {
    private final UserEntity editedUserEntity;
    private final boolean editMode;
    private final AdminUserFormData userFormData;
    private final Long expectedUserVersion;

    // Dialogkopf und Formularfelder
    private Dialog editUserDialog;
    private H2 editorTitleH2;
    private Span editorDescriptionSpan;
    private Button closeEditUserDialogButton;
    private Binder<AdminUserFormData> userFormBinder;
    private TextField firstNameField;
    private TextField lastNameField;
    private EmailField emailField;
    private PasswordField passwordField;
    private Span passwordHintSpan;
    private Span confirmPasswordHintSpan;
    private PasswordField confirmPasswordField;
    private ComboBox<Gender> genderComboBox;
    private DatePicker birthDatePicker;
    private Checkbox administrativelyBlockedCheckbox;
    private Span blockedCaptionSpan;
    private Span blockedHintSpan;
    private final Checkbox emailVerificationRequiredCheckbox = new Checkbox();
    private final Span emailVerificationCaptionSpan = new Span();
    private final Span emailVerificationHintSpan = new Span();
    private final Checkbox developmentAccountCheckbox = new Checkbox();
    private final Span developmentAccountCaptionSpan = new Span();
    private final Span developmentAccountHintSpan = new Span();
    private MultiSelectComboBox<RoleType> rolesComboBox;
    private Span rolesSummarySpan;
    private Span technicalInfoSpan;
    private Div securityNoteDiv;

    // Validierungszustand und Formularaktionen
    private AtomicBoolean emailCheckFailed;
    private AtomicBoolean showPasswordErrors;
    private AtomicBoolean showConfirmPasswordErrors;
    private Binding<AdminUserFormData, String> passwordBinding;
    private Binding<AdminUserFormData, String> confirmPasswordBinding;
    private Button saveUserButton;
    private Button cancelUserEditButton;
    private List<Object> loadedUserFormValues;

    // Anmeldeinformationen eines vorhandenen Kontos
    private SignInSummary signInSummary;
    private final H3 signInTitleH3 = new H3();
    private DetailsToggleButton signInDetailsToggleButton;
    private final VerticalLayout lifecycleStatusLayout = new VerticalLayout();
    private final Span registrationLabelSpan = new Span();
    private final Badge registeredViaBadge = new Badge();
    private final Span signInMethodsLabelSpan = new Span();
    private final Div availableSignInMethodsDiv = new Div();
    private final Span accountTypeLabelSpan = new Span();
    private final Badge accountTypeBadge = new Badge();
    private final VerticalLayout verificationDetailsLayout = new VerticalLayout();
    private final VerticalLayout administrativeMailActionsLayout = new VerticalLayout();
    private final Button resendVerificationMailButton = new Button();
    private final Button sendPasswordResetMailButton = new Button();

    private UserEditor(UserEntity editedUserEntity) {
      this.editedUserEntity = editedUserEntity;
      editMode = editedUserEntity.getId() != null;
      userFormData = AdminUserFormData.fromUser(editedUserEntity);
      expectedUserVersion = editedUserEntity.getVersion();
    }

    private void open() {
      configureEditorDialogHeader();
      configureUserFormFields();
      bindUserFormFields();
      loadedUserFormValues = readUserFormValues();
      configurePasswordFeedback();

      configureEditorButtons();
      composeEditorDialogLayout();

      registerTextRefresh();
      BrowserUnsavedChangesGuard browserChangesGuard =
          new BrowserUnsavedChangesGuard(
              editUserDialog,
              () ->
                  editUserDialog.isOpened()
                      && (!loadedUserFormValues.equals(readUserFormValues())
                          || birthDatePicker.isInvalid()));
      editUserDialog.addOpenedChangeListener(event -> browserChangesGuard.refresh());
      editUserDialog.open();
    }

    private void configureEditorDialogHeader() {
      editUserDialog = new Dialog();
      editUserDialog.addClassNames("cosmos-theme", REFERENCE_OVERLAY, "admin-reference-dialog");
      editUserDialog.getThemeNames().add("cosmos-reference");
      editUserDialog.getElement().getThemeList().add("cosmos");
      editUserDialog.setCloseOnEsc(true);
      editUserDialog.setCloseOnOutsideClick(true);
      editUserDialog.addDialogCloseActionListener(closeEvent -> requestCloseUserEditor());
      editUserDialog.setDraggable(false);
      editUserDialog.setResizable(false);
      editUserDialog.addClassName("admin-user-dialog");

      editorTitleH2 =
          new H2(
              editMode
                  ? messageProperties.getAdminUsersEditDialogTitle()
                  : messageProperties.getAdminUsersCreateDialogTitle());
      editorTitleH2.addClassName(COSMOS_PANEL_TITLE_CLASS);
      editorDescriptionSpan = new Span(messageProperties.getAdminUsersDialogDescription());
      editorDescriptionSpan.addClassName(COSMOS_PANEL_DESCRIPTION_CLASS);
      Div editorHeadingDiv = new Div(editorTitleH2, editorDescriptionSpan);
      editorHeadingDiv.addClassName(COSMOS_PANEL_HEADING_CLASS);

      SvgIcon userSvgIcon = AppIcon.USERS.create();
      userSvgIcon.getElement().setAttribute(ARIA_HIDDEN, "true");
      Div userIconBadgeDiv = new Div(userSvgIcon);
      userIconBadgeDiv.addClassName(PANEL_ICON);

      closeEditUserDialogButton =
          new Button(AppIcon.CLOSE_CIRCLE.create(), event -> requestCloseUserEditor());
      closeEditUserDialogButton.addClassNames(
          "cosmos-overlay-close", LumoUtility.TextColor.PRIMARY);
      closeEditUserDialogButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
      closeEditUserDialogButton.setAriaLabel(messageProperties.getAdminUsersCloseDialog());

      Div editorHeaderDiv = new Div(userIconBadgeDiv, editorHeadingDiv, closeEditUserDialogButton);
      editorHeaderDiv.addClassName("admin-user-dialog-header");
      editUserDialog.getHeader().add(editorHeaderDiv);
      editUserDialog.getElement().setProperty(ARIA_LABEL_PROPERTY, editorTitleH2.getText());
    }

    private void configureUserFormFields() {
      userFormBinder = new Binder<>(AdminUserFormData.class);
      resendVerificationMailButton.addClickListener(event -> requestAdministrativeMail(true));
      sendPasswordResetMailButton.addClickListener(event -> requestAdministrativeMail(false));

      firstNameField = new TextField(messageProperties.getAdminUsersFieldFirstName());
      firstNameField.setRequiredIndicatorVisible(true);
      firstNameField.setWidthFull();
      firstNameField.setPlaceholder(messageProperties.getAccountFirstNamePlaceholder());

      lastNameField = new TextField(messageProperties.getAdminUsersFieldLastName());
      lastNameField.setRequiredIndicatorVisible(true);
      lastNameField.setWidthFull();
      lastNameField.setPlaceholder(messageProperties.getAccountLastNamePlaceholder());

      emailField = new EmailField(messageProperties.getAdminUsersFieldEmail());
      emailField.setAutocomplete(Autocomplete.EMAIL);
      emailField.setPattern(UserInputRules.EMAIL_REGEX);
      emailField.setRequiredIndicatorVisible(true);
      emailField.setWidthFull();
      emailField.setPlaceholder(messageProperties.getAccountEmailPlaceholder());
      emailField.setClearButtonVisible(true);

      passwordField = new PasswordField(messageProperties.getAdminUsersFieldPassword());
      passwordField.setAutocomplete(Autocomplete.NEW_PASSWORD);
      passwordField.setWidthFull();
      passwordField.setRevealButtonVisible(true);
      passwordField.setValueChangeMode(ValueChangeMode.EAGER);
      passwordField.setRequiredIndicatorVisible(!editMode);
      passwordField.setPlaceholder(
          editMode
              ? messageProperties.getAdminUsersPasswordPlaceholderEdit()
              : messageProperties.getAccountPasswordPlaceholder());
      passwordHintSpan = new Span();
      passwordHintSpan.addClassName("admin-user-password-hint");
      refreshPasswordHintText();
      passwordField.setAriaDescribedBy(passwordHintSpan);

      confirmPasswordField = new PasswordField(messageProperties.getPublicConfirmPasswordField());
      confirmPasswordField.setAutocomplete(Autocomplete.NEW_PASSWORD);
      confirmPasswordField.setWidthFull();
      confirmPasswordField.setRevealButtonVisible(true);
      confirmPasswordField.setValueChangeMode(ValueChangeMode.EAGER);
      confirmPasswordField.setPlaceholder(messageProperties.getAccountConfirmPasswordPlaceholder());
      confirmPasswordField.setEnabled(false);
      confirmPasswordField.setAriaDescribedBy(passwordHintSpan);
      confirmPasswordHintSpan = new Span(messageProperties.getAdminUsersPasswordConfirmHelper());
      confirmPasswordHintSpan.addClassName("admin-user-password-hint");

      genderComboBox = new ComboBox<>(messageProperties.getRegistrationGenderCombobox());
      genderComboBox.setItems(Gender.values());
      CosmosComboBoxStyling.styleChoice(
          genderComboBox, AdminUserManagementSection.this::formatGender, false);
      genderComboBox.setPlaceholder(messageProperties.getAccountGenderPlaceholder());
      genderComboBox.setWidthFull();
      genderComboBox.setClearButtonVisible(true);
      genderComboBox.setItemLabelGenerator(AdminUserManagementSection.this::formatGender);

      birthDatePicker = new DatePicker(messageProperties.getRegistrationBirthDateField());
      birthDatePicker.setAutoOpen(false);
      CompactDateInput.configureDatePickerInput(birthDatePicker, messageProperties);
      refreshBirthDatePicker();
      birthDatePicker.setWidthFull();
      birthDatePicker.setClearButtonVisible(true);
      birthDatePicker.setMax(LocalDate.now(ZoneId.systemDefault()));

      administrativelyBlockedCheckbox = new Checkbox();
      administrativelyBlockedCheckbox.addClassNames(
          "admin-user-option-checkbox", "admin-user-enabled-checkbox");
      blockedCaptionSpan = new Span(messageProperties.getAdminUsersFieldBlocked());
      blockedHintSpan = new Span(messageProperties.getAdminUsersBlockedHint());
      blockedHintSpan.addClassName("admin-user-option-hint");
      blockedHintSpan.getElement().setAttribute(ARIA_HIDDEN, "true");
      Div blockedLabelDiv = new Div(blockedCaptionSpan, blockedHintSpan);
      blockedLabelDiv.addClassName("admin-user-option-label");
      administrativelyBlockedCheckbox.setLabelComponent(blockedLabelDiv);
      administrativelyBlockedCheckbox.setAriaDescribedBy(blockedHintSpan);

      configureEmailVerificationCheckbox();

      rolesComboBox = new MultiSelectComboBox<>(messageProperties.getAdminUsersFieldRoles());
      rolesComboBox.addClassName("admin-user-roles");
      rolesComboBox.setWidthFull();
      rolesComboBox
          .setItems(RoleType.values())
          .setFilter(roleType ->
              roleType != RoleType.ROLE_SUPERADMIN || userService.canManageSuperadmins());
      rolesComboBox.setItemLabelGenerator(AdminUserManagementSection.this::formatRoleType);
      rolesComboBox.setClassNameGenerator(roleType -> "admin-user-role-option");
      rolesComboBox.setRequiredIndicatorVisible(true);
      rolesComboBox.setHelperText(messageProperties.getAdminUsersRolesHelper());
      rolesSummarySpan = new Span();
      rolesSummarySpan.addClassName("admin-user-role-summary");
      rolesSummarySpan.getElement().setAttribute(ARIA_HIDDEN, "true");
      rolesComboBox.addValueChangeListener(event -> refreshRolesSummaryText());
      refreshRolesSummaryText();

      technicalInfoSpan =
          new Span(
              editMode
                  ? messageProperties.getAdminUsersTechnicalInfoEdit()
                  : messageProperties.getAdminUsersTechnicalInfoCreate());
      technicalInfoSpan.addClassName(LumoUtility.TextColor.SECONDARY);
      SvgIcon securitySvgIcon = AppIcon.SHIELD_CHECK.create();
      securitySvgIcon.getElement().setAttribute(ARIA_HIDDEN, "true");
      securityNoteDiv = new Div(securitySvgIcon, technicalInfoSpan);
      securityNoteDiv.addClassNames("cosmos-panel-hint", "admin-user-security-note");
    }

    private void configureEmailVerificationCheckbox() {
      emailVerificationRequiredCheckbox.setId("admin-email-verification-required");
      emailVerificationRequiredCheckbox.setValue(true);
      emailVerificationRequiredCheckbox.addClassName("admin-user-option-checkbox");

      emailVerificationCaptionSpan.setText(messageProperties.getAdminEmailVerificationRequired());
      emailVerificationHintSpan.setText(messageProperties.getAdminEmailVerificationHint());
      emailVerificationHintSpan.addClassName("admin-user-option-hint");
      emailVerificationHintSpan.getElement().setAttribute(ARIA_HIDDEN, "true");
      Div verificationLabelDiv = new Div(emailVerificationCaptionSpan, emailVerificationHintSpan);
      verificationLabelDiv.addClassName("admin-user-option-label");
      emailVerificationRequiredCheckbox.setLabelComponent(verificationLabelDiv);
      emailVerificationRequiredCheckbox.setAriaDescribedBy(emailVerificationHintSpan);

      developmentAccountCheckbox.setId("admin-development-account");
      developmentAccountCheckbox.addClassName("admin-user-option-checkbox");
      developmentAccountCaptionSpan.setText(messageProperties.getDevelopmentAccountLabel());
      developmentAccountHintSpan.setText(messageProperties.getDevelopmentAccountHint());
      developmentAccountHintSpan.addClassName("admin-user-option-hint");
      developmentAccountHintSpan.getElement().setAttribute(ARIA_HIDDEN, "true");
      Div developmentAccountLabelDiv =
          new Div(developmentAccountCaptionSpan, developmentAccountHintSpan);
      developmentAccountLabelDiv.addClassName("admin-user-option-label");
      developmentAccountCheckbox.setLabelComponent(developmentAccountLabelDiv);
      developmentAccountCheckbox.setAriaDescribedBy(developmentAccountHintSpan);
      developmentAccountCheckbox.addValueChangeListener(event -> {
        emailVerificationRequiredCheckbox.setEnabled(!event.getValue());
        emailVerificationRequiredCheckbox.setValue(!event.getValue());
      });
    }

    private void refreshBirthDatePicker() {
      birthDatePicker.setLocale(
          VaadinSession.getCurrent() != null
              ? VaadinSession.getCurrent().getLocale()
              : Locale.getDefault());
      CompactDateInput.configureDatePickerInput(birthDatePicker, messageProperties);
      birthDatePicker.setLabel(messageProperties.getRegistrationBirthDateField());
      birthDatePicker.setPlaceholder(messageProperties.getRegistrationBirthDatePlaceholder());
    }

    private void bindUserFormFields() {
      userFormBinder
          .forField(firstNameField)
          .asRequired(context -> messageProperties.getBaseValidationRequiredMessage())
          .withValidator(
              ValidationHelper::isRequiredTextValid,
              context -> messageProperties.getBaseValidationRequiredMessage())
          .bind(AdminUserFormData::getFirstName, AdminUserFormData::setFirstName);

      userFormBinder
          .forField(lastNameField)
          .asRequired(context -> messageProperties.getBaseValidationRequiredMessage())
          .withValidator(
              ValidationHelper::isRequiredTextValid,
              context -> messageProperties.getBaseValidationRequiredMessage())
          .bind(AdminUserFormData::getLastName, AdminUserFormData::setLastName);

      bindEmailField();
      bindPasswordFields();

      userFormBinder
          .forField(genderComboBox)
          .bind(AdminUserFormData::getGender, AdminUserFormData::setGender);

      userFormBinder
          .forField(birthDatePicker)
          .withValidator(
              ValidationHelper::isBirthDateValid,
              context -> messageProperties.getBaseValidationBirthDatePastMessage())
          .bind(AdminUserFormData::getBirthDate, AdminUserFormData::setBirthDate);

      userFormBinder
          .forField(administrativelyBlockedCheckbox)
          .bind(
              AdminUserFormData::isAdministrativelyBlocked,
              AdminUserFormData::setAdministrativelyBlocked);

      userFormBinder
          .forField(rolesComboBox)
          .withValidator(
              selectedRoles -> selectedRoles != null && !selectedRoles.isEmpty(),
              context -> messageProperties.getBaseValidationRequiredMessage())
          .bind(AdminUserFormData::getRoles, AdminUserFormData::setRoles);

      userFormBinder.readBean(userFormData);
    }

    private void bindEmailField() {
      emailCheckFailed = new AtomicBoolean();
      userFormBinder
          .forField(emailField)
          .asRequired(context -> messageProperties.getBaseValidationRequiredMessage())
          .withValidator(
              ValidationHelper::isEmailValid,
              context -> messageProperties.getBaseValidationEmailInvalidMessage())
          .withValidator((submittedEmailAddress, context) -> {
            emailCheckFailed.set(false);
            try {
              return userService.emailExistsForOtherUser(
                      ValidationHelper.normalize(submittedEmailAddress), userFormData.getId())
                  ? ValidationResult.error(messageProperties.getBaseValidationEmailExistsMessage())
                  : ValidationResult.ok();
            } catch (RuntimeException emailCheckException) {
              log.error("E-Mail-Vorprüfung fehlgeschlagen", safeFailure(emailCheckException));
              emailCheckFailed.set(true);
              return ValidationResult.error(messageProperties.getAdminUsersErrorSaveFailed());
            }
          })
          .bind(AdminUserFormData::getEmail, AdminUserFormData::setEmail);
    }

    private void bindPasswordFields() {
      showPasswordErrors = new AtomicBoolean();
      showConfirmPasswordErrors = new AtomicBoolean();
      passwordBinding =
          userFormBinder
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
          userFormBinder
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
        refreshPasswordHintText();

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

    private void refreshPasswordHintText() {
      boolean passwordRequired =
          !editMode || ValidationHelper.isRequiredTextValid(passwordField.getValue());
      passwordHintSpan.setText(
          passwordRequired
              ? messageProperties.getAdminUsersPasswordHelperCreate()
              : messageProperties.getAdminUsersPasswordHelperEdit());
    }

    private void configureEditorButtons() {
      saveUserButton = new Button(messageProperties.getAdminUsersSave());
      saveUserButton.setPrefixComponent(AppIcon.CHECK.create());
      saveUserButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
      saveUserButton.setEnabled(userFormBinder.isValid());

      userFormBinder.addStatusChangeListener(
          event -> saveUserButton.setEnabled(userFormBinder.isValid()));

      saveUserButton.addClickListener(event -> saveUser());

      cancelUserEditButton =
          new Button(messageProperties.getAdminUsersCancel(), event -> requestCloseUserEditor());
      cancelUserEditButton.setPrefixComponent(AppIcon.CLOSE.create());
    }

    private List<Object> readUserFormValues() {
      List<Object> currentFormValues =
          new ArrayList<>(
              userFormBinder.getFields().map(field -> (Object) field.getValue()).toList());
      currentFormValues.add(emailVerificationRequiredCheckbox.getValue());
      currentFormValues.add(developmentAccountCheckbox.getValue());
      return currentFormValues;
    }

    private void requestCloseUserEditor() {
      if (loadedUserFormValues.equals(readUserFormValues()) && !birthDatePicker.isInvalid()) {
        editUserDialog.close();
        return;
      }

      UnsavedChangesDialog.confirmDiscard(messageProperties, editUserDialog::close);
    }

    private void composeEditorDialogLayout() {
      HorizontalLayout userNameRowLayout = new HorizontalLayout(firstNameField, lastNameField);
      userNameRowLayout.setWidthFull();
      userNameRowLayout.addClassName(COSMOS_FORM_ROW_CLASS);
      userNameRowLayout.expand(firstNameField, lastNameField);

      Div passwordColumnDiv = new Div(passwordField, passwordHintSpan);
      passwordColumnDiv.addClassName("admin-user-password-column");
      Div confirmPasswordColumnDiv = new Div(confirmPasswordField, confirmPasswordHintSpan);
      confirmPasswordColumnDiv.addClassName("admin-user-password-column");
      HorizontalLayout passwordRowLayout =
          new HorizontalLayout(passwordColumnDiv, confirmPasswordColumnDiv);
      passwordRowLayout.setWidthFull();
      passwordRowLayout.setSpacing(false);
      passwordRowLayout.addClassName(COSMOS_FORM_ROW_CLASS);
      passwordRowLayout.expand(passwordColumnDiv, confirmPasswordColumnDiv);
      Div passwordFieldsDiv = new Div(passwordRowLayout);
      passwordFieldsDiv.addClassName("admin-user-password-fields");

      HorizontalLayout personalDetailsRowLayout =
          new HorizontalLayout(genderComboBox, birthDatePicker);
      personalDetailsRowLayout.setWidthFull();
      personalDetailsRowLayout.addClassName(COSMOS_FORM_ROW_CLASS);
      personalDetailsRowLayout.expand(genderComboBox, birthDatePicker);

      HorizontalLayout editorActionsLayout = new HorizontalLayout();
      editorActionsLayout.addClassName("cosmos-dialog-actions");
      EqualButtonWidths.configureEqualButtonWidths(editorActionsLayout);
      editorActionsLayout.setWidthFull();
      editorActionsLayout.setAlignItems(Alignment.CENTER);

      if (editMode) {
        Button deleteButton =
            new Button(
                messageProperties.getAdminUsersDelete(),
                AppIcon.TRASH.create(),
                event -> deleteUser(editedUserEntity, editUserDialog::close));
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteButton.addClassName(DELETE_BUTTON);
        deleteButton.setEnabled(!isCurrentUser(editedUserEntity));
        editorActionsLayout.add(deleteButton);
      }
      cancelUserEditButton.addClassName("cosmos-dialog-cancel");
      editorActionsLayout.add(cancelUserEditButton, saveUserButton);

      Div roleSelectionDiv = new Div(rolesComboBox, rolesSummarySpan);
      roleSelectionDiv.addClassName("admin-user-role-field");
      Div permissionsRowDiv = new Div(roleSelectionDiv, administrativelyBlockedCheckbox);
      permissionsRowDiv.addClassName("admin-user-permissions");
      VerticalLayout editorFormLayout =
          new VerticalLayout(
              userNameRowLayout,
              emailField,
              passwordFieldsDiv,
              personalDetailsRowLayout,
              permissionsRowDiv);
      if (!editMode && userService.canManageSuperadmins()) {
        editorFormLayout.add(emailVerificationRequiredCheckbox, developmentAccountCheckbox);
      }
      if (editMode && externalIdentityService != null) {
        externalIdentityService
            .adminSignInSummary(editedUserEntity.getId())
            .ifPresent(summary -> {
              signInSummary = summary;
              editorFormLayout.add(createSignInDetailsLayout());
            });
      }
      Div editorFooterDiv = new Div(securityNoteDiv, editorActionsLayout);
      editorFooterDiv.addClassName("admin-user-editor-footer");
      editUserDialog.getFooter().add(editorFooterDiv);
      editorFormLayout.setPadding(false);
      editorFormLayout.setSpacing(false);
      editorFormLayout.addClassName("cosmos-dialog-form");

      editUserDialog.add(editorFormLayout);
    }

    private VerticalLayout createSignInDetailsLayout() {
      signInTitleH3.addClassNames(COSMOS_PANEL_TITLE_CLASS, "admin-user-sign-in-title");
      SvgIcon signInInfoSvgIcon = AppIcon.INFO_CIRCLE.create();
      signInInfoSvgIcon.getElement().setAttribute(ARIA_HIDDEN, "true");
      HorizontalLayout signInHeadingLayout = new HorizontalLayout(signInInfoSvgIcon, signInTitleH3);
      signInHeadingLayout.setPadding(false);
      signInHeadingLayout.setSpacing(false);
      signInHeadingLayout.setAlignItems(Alignment.CENTER);
      signInHeadingLayout.addClassName("admin-user-sign-in-heading");

      registeredViaBadge.setId("admin-user-registered-via");
      accountTypeBadge.setId("admin-user-account-type");
      accountTypeBadge
          .getElement()
          .setAttribute(
              "data-development-account",
              Boolean.toString(editedUserEntity.isDevelopmentAccount()));
      accountTypeLabelSpan.addClassName("admin-user-account-type-label");
      Div accountTypeDetailDiv = new Div(accountTypeLabelSpan, accountTypeBadge);
      accountTypeDetailDiv.addClassName("admin-user-sign-in-detail");
      availableSignInMethodsDiv.setId("admin-user-sign-in-methods");
      registrationLabelSpan.addClassName("admin-user-registration-label");
      signInMethodsLabelSpan.addClassName("admin-user-methods-label");
      Div registrationDetailDiv = new Div(registrationLabelSpan, registeredViaBadge);
      Div signInMethodsDetailDiv = new Div(signInMethodsLabelSpan, availableSignInMethodsDiv);
      registrationDetailDiv.addClassName("admin-user-sign-in-detail");
      signInMethodsDetailDiv.addClassName("admin-user-sign-in-detail");
      HorizontalLayout signInDetailsRowLayout =
          new HorizontalLayout(registrationDetailDiv, signInMethodsDetailDiv);
      signInDetailsRowLayout.setWidthFull();
      signInDetailsRowLayout.addClassName(COSMOS_FORM_ROW_CLASS);
      signInDetailsRowLayout.expand(registrationDetailDiv, signInMethodsDetailDiv);

      HorizontalLayout signInStatusRowLayout =
          new HorizontalLayout(verificationDetailsLayout, accountTypeDetailDiv);
      signInStatusRowLayout.setWidthFull();
      signInStatusRowLayout.addClassName(COSMOS_FORM_ROW_CLASS);
      signInStatusRowLayout.expand(verificationDetailsLayout, accountTypeDetailDiv);

      VerticalLayout signInDetailsLayout =
          new VerticalLayout(
              signInDetailsRowLayout,
              signInStatusRowLayout,
              lifecycleStatusLayout,
              administrativeMailActionsLayout);
      signInDetailsLayout.setWidthFull();
      signInDetailsLayout.setPadding(false);
      signInDetailsLayout.setSpacing(false);
      signInDetailsLayout.setAlignItems(Alignment.STRETCH);
      signInDetailsLayout.addClassName("admin-user-sign-in-content");

      Details signInDetails = new Details("", signInDetailsLayout);
      signInDetails.setId("admin-user-sign-in-details");
      signInDetails.setWidthFull();
      signInDetails.setOpened(false);
      signInDetails.addClassName("admin-user-sign-in-content-details");

      signInDetailsToggleButton =
          new DetailsToggleButton(
              messageProperties, signInDetails, messageProperties.getSignInDetailsTitle());
      signInDetailsToggleButton.setId("admin-user-sign-in-toggle");
      signInHeadingLayout.setWidthFull();
      signInHeadingLayout.expand(signInTitleH3);
      signInHeadingLayout.add(signInDetailsToggleButton);

      VerticalLayout signInPanelLayout = new VerticalLayout(signInHeadingLayout, signInDetails);
      signInPanelLayout.setWidthFull();
      signInPanelLayout.setPadding(false);
      signInPanelLayout.setSpacing(false);
      signInPanelLayout.addClassNames("cosmos-panel-hint", "admin-user-sign-in");

      refreshSignInDetails();
      return signInPanelLayout;
    }

    private void refreshSignInDetails() {
      if (signInSummary == null) {
        return;
      }
      signInTitleH3.setText(messageProperties.getSignInDetailsTitle());
      signInDetailsToggleButton.setText(messageProperties.getBaseDetailsButton());
      signInDetailsToggleButton.setAriaLabel(
          messageProperties.getSignInDetailsTitle()
              + ": "
              + messageProperties.getBaseDetailsButton());
      accountTypeLabelSpan.setText(messageProperties.getAccountTypeLabel());
      accountTypeBadge.setText(
          editedUserEntity.isDevelopmentAccount()
              ? messageProperties.getDevelopmentAccountLabel()
              : messageProperties.getStandardAccountLabel());
      refreshVerificationDetails();
      refreshLifecycleStatus();
      refreshAdministrativeMailActions();

      registrationLabelSpan.setText(messageProperties.getSignInRegisteredVia());
      registeredViaBadge.setText(
          messageProperties.getRegistrationChannelLabel(signInSummary.registration()));
      registeredViaBadge
          .getElement()
          .setAttribute(
              "data-self-registered", Boolean.toString(signInSummary.registration() != null));
      signInMethodsLabelSpan.setText(messageProperties.getSignInAvailableMethods());
      availableSignInMethodsDiv.removeAll();
      if (signInSummary.available().isEmpty()) {
        availableSignInMethodsDiv.add(new Span(messageProperties.getSignInNone()));
      } else {
        signInSummary.available().stream()
            .sorted()
            .map(messageProperties::getSignInMethodLabel)
            .forEach(signInMethodLabel -> {
              Badge signInMethodBadge = new Badge(signInMethodLabel);
              signInMethodBadge.addThemeVariants(BadgeVariant.SUCCESS);
              availableSignInMethodsDiv.add(signInMethodBadge);
            });
      }
    }

    private void refreshLifecycleStatus() {
      lifecycleStatusLayout.removeAll();
      lifecycleStatusLayout.setPadding(false);
      lifecycleStatusLayout.setSpacing(false);
      lifecycleStatusLayout.setWidthFull();
      lifecycleStatusLayout.setAlignItems(Alignment.START);
      lifecycleStatusLayout.addClassName("admin-user-lifecycle-status");
      boolean selfDeactivated = editedUserEntity.getAccountDeactivation() != null;
      lifecycleStatusLayout.setVisible(selfDeactivated);
      if (!selfDeactivated) {
        return;
      }

      Span selfDeactivationLabelSpan =
          new Span(messageProperties.getAdminUserSelfDeactivated() + ": ");
      selfDeactivationLabelSpan.setId("admin-user-self-deactivation");
      selfDeactivationLabelSpan.addClassName("cosmos-security-warning-label");
      String deletionDateText =
          DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                  .withLocale(CustomI18NProvider.getCurrentLocale())
                  .withZone(ZoneOffset.UTC)
                  .format(editedUserEntity.getAccountDeactivation().deletionAt())
              + " UTC";
      Span deletionDeadlineSpan =
          new Span(messageProperties.getAdminUserScheduledDeletion(deletionDateText));
      deletionDeadlineSpan.setId("admin-user-deletion-deadline");
      Span deactivationWarningTextSpan = new Span(selfDeactivationLabelSpan, deletionDeadlineSpan);
      SvgIcon deactivationWarningSvgIcon = AppIcon.ERROR.create();
      deactivationWarningSvgIcon.getElement().setAttribute(ARIA_HIDDEN, "true");
      HorizontalLayout deactivationWarningLayout =
          new HorizontalLayout(deactivationWarningSvgIcon, deactivationWarningTextSpan);
      deactivationWarningLayout.setId("admin-user-self-deactivation-warning");
      deactivationWarningLayout.setPadding(false);
      deactivationWarningLayout.setSpacing(false);
      deactivationWarningLayout.setAlignItems(Alignment.START);
      deactivationWarningLayout.addClassName("cosmos-security-warning");
      lifecycleStatusLayout.add(deactivationWarningLayout);
    }

    private void refreshVerificationDetails() {
      verificationDetailsLayout.removeAll();
      verificationDetailsLayout.setPadding(false);
      verificationDetailsLayout.setSpacing(false);
      verificationDetailsLayout.setAlignItems(Alignment.START);
      verificationDetailsLayout.addClassName("admin-user-verification-detail");
      Span verificationLabelSpan = new Span(messageProperties.getEmailVerificationLabel());
      verificationLabelSpan.addClassName("admin-user-verification-label");
      verificationDetailsLayout.add(
          verificationLabelSpan,
          new EmailVerificationBadge(
              messageProperties, editedUserEntity.getEmailVerificationStatus()));
    }

    private void refreshAdministrativeMailActions() {
      administrativeMailActionsLayout.removeAll();
      administrativeMailActionsLayout.setPadding(false);
      administrativeMailActionsLayout.setSpacing(false);
      administrativeMailActionsLayout.setVisible(adminAccountMailService != null);
      administrativeMailActionsLayout.addClassName("admin-user-mail-actions");
      if (adminAccountMailService == null) {
        return;
      }

      resendVerificationMailButton.setId("admin-resend-verification-mail");
      resendVerificationMailButton.setText(messageProperties.getAdminSendVerificationLink());
      resendVerificationMailButton.setPrefixComponent(AppIcon.ENVELOPE.create());
      resendVerificationMailButton.setEnabled(
          !editedUserEntity.isDevelopmentAccount() && editedUserEntity.requiresEmailVerification());
      sendPasswordResetMailButton.setId("admin-send-password-reset");
      sendPasswordResetMailButton.setText(messageProperties.getAdminPasswordResetMail());
      sendPasswordResetMailButton.setPrefixComponent(AppIcon.KEY.create());
      sendPasswordResetMailButton.setEnabled(
          !editedUserEntity.isDevelopmentAccount()
              && editedUserEntity.isEnabled()
              && !editedUserEntity.requiresEmailVerification()
              && editedUserEntity.getPassword() != null
              && !editedUserEntity.getPassword().isBlank());

      HorizontalLayout mailActionsLayout =
          new HorizontalLayout(resendVerificationMailButton, sendPasswordResetMailButton);
      mailActionsLayout.setWidthFull();
      mailActionsLayout.setPadding(false);
      mailActionsLayout.setSpacing(false);
      mailActionsLayout.addClassName("admin-user-mail-action-buttons");

      Hr mailActionsDividerHr = new Hr();
      mailActionsDividerHr.addClassName("admin-user-mail-actions-divider");

      Span mailActionsHintSpan =
          new Span(
              editedUserEntity.isDevelopmentAccount()
                  ? messageProperties.getDevelopmentAccountMailUnavailable()
                  : messageProperties.getAdminAccountMailActionsHint());
      SvgIcon mailActionsInfoSvgIcon = AppIcon.INFO_CIRCLE.create();
      mailActionsInfoSvgIcon.getElement().setAttribute(ARIA_HIDDEN, "true");
      HorizontalLayout mailActionsHintLayout =
          new HorizontalLayout(mailActionsInfoSvgIcon, mailActionsHintSpan);
      mailActionsHintLayout.setWidthFull();
      mailActionsHintLayout.setPadding(false);
      mailActionsHintLayout.setSpacing(false);
      mailActionsHintLayout.setAlignItems(Alignment.CENTER);
      mailActionsHintLayout.addClassName("admin-user-mail-actions-hint");

      administrativeMailActionsLayout.add(
          mailActionsDividerHr, mailActionsLayout, mailActionsHintLayout);
    }

    private void requestAdministrativeMail(boolean verificationMail) {
      try {
        boolean mailRequestAccepted =
            verificationMail
                ? adminAccountMailService.resendVerification(
                    editedUserEntity.getId(), editedUserEntity.getEmail())
                : adminAccountMailService.sendPasswordReset(
                    editedUserEntity.getId(), editedUserEntity.getEmail());
        NotificationHelper.getInstance()
            .showNotification(
                messageProperties::getSignInDetailsTitle,
                mailRequestAccepted
                    ? messageProperties::getEmailVerificationQueued
                    : messageProperties::getEmailVerificationWait,
                mailRequestAccepted
                    ? NotificationHelper.NotificationType.SUCCESS
                    : NotificationHelper.NotificationType.WARNING);
      } catch (IOException | RuntimeException failure) {
        log.warn("Administrative account mail request failed", safeFailure(failure));
        showError(messageProperties::getBaseFailedMessage);
      }
    }

    private void refreshRolesSummaryText() {
      String selectedRolesSummaryText =
          messageProperties.getAdminUsersSelectedRoles(rolesComboBox.getValue().size());
      rolesSummarySpan.setText(selectedRolesSummaryText);
      rolesComboBox.setPlaceholder(selectedRolesSummaryText);
    }

    private void registerTextRefresh() {
      Runnable refreshEditorTextsAction = this::refreshEditorTexts;
      dialogTextRefreshActions.add(refreshEditorTextsAction);
      editUserDialog.addOpenedChangeListener(event -> {
        if (!event.isOpened()) {
          dialogTextRefreshActions.remove(refreshEditorTextsAction);
        }
      });
      editUserDialog.addDetachListener(
          event -> dialogTextRefreshActions.remove(refreshEditorTextsAction));
    }

    private void refreshEditorTexts() {
      developmentAccountCaptionSpan.setText(messageProperties.getDevelopmentAccountLabel());
      developmentAccountHintSpan.setText(messageProperties.getDevelopmentAccountHint());
      emailVerificationCaptionSpan.setText(messageProperties.getAdminEmailVerificationRequired());
      emailVerificationHintSpan.setText(messageProperties.getAdminEmailVerificationHint());
      refreshBirthDatePicker();
      editorTitleH2.setText(
          editMode
              ? messageProperties.getAdminUsersEditDialogTitle()
              : messageProperties.getAdminUsersCreateDialogTitle());
      editUserDialog.getElement().setProperty(ARIA_LABEL_PROPERTY, editorTitleH2.getText());
      editorDescriptionSpan.setText(messageProperties.getAdminUsersDialogDescription());
      closeEditUserDialogButton.setAriaLabel(messageProperties.getAdminUsersCloseDialog());

      confirmPasswordField.setLabel(messageProperties.getPublicConfirmPasswordField());
      confirmPasswordHintSpan.setText(messageProperties.getAdminUsersPasswordConfirmHelper());
      confirmPasswordField.setPlaceholder(messageProperties.getAccountConfirmPasswordPlaceholder());

      saveUserButton.setText(messageProperties.getAdminUsersSave());
      cancelUserEditButton.setText(messageProperties.getAdminUsersCancel());

      rolesComboBox.setLabel(messageProperties.getAdminUsersFieldRoles());
      rolesComboBox.setHelperText(messageProperties.getAdminUsersRolesHelper());
      rolesComboBox.getDataProvider().refreshAll();
      refreshRolesSummaryText();

      refreshSignInDetails();
      blockedCaptionSpan.setText(messageProperties.getAdminUsersFieldBlocked());
      blockedHintSpan.setText(messageProperties.getAdminUsersBlockedHint());

      passwordField.setPlaceholder(
          editMode
              ? messageProperties.getAdminUsersPasswordPlaceholderEdit()
              : messageProperties.getAccountPasswordPlaceholder());
      refreshPasswordHintText();

      technicalInfoSpan.setText(
          editMode
              ? messageProperties.getAdminUsersTechnicalInfoEdit()
              : messageProperties.getAdminUsersTechnicalInfoCreate());

      if (userFormBinder
          .getFields()
          .anyMatch(field ->
              field instanceof com.vaadin.flow.component.shared.HasValidationProperties validation
                  && validation.isInvalid())) {
        userFormBinder.validate();
      }
    }

    private void saveUser() {
      try {
        if (!validateUserForm()) {
          return;
        }

        Set<RoleEntity> roleEntities = resolveSelectedRoleEntities(userFormData.getRoles());
        if (roleEntities.size() != userFormData.getRoles().size()) {
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

    private boolean validateUserForm() {
      try {
        emailCheckFailed.set(false);
        showPasswordErrors.set(true);
        showConfirmPasswordErrors.set(true);
        userFormBinder.writeBean(userFormData);
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
      if (isCurrentUser(editedUserEntity) && userFormData.isAdministrativelyBlocked()) {
        showError(messageProperties::getAdminUsersErrorDisableOwnUser);
        return false;
      }

      if (isCurrentUser(editedUserEntity)
          && userFormData.getRoles().stream().noneMatch(RoleType::isAdministrative)) {
        showError(messageProperties::getAdminUsersErrorRemoveOwnAdminRole);
        return false;
      }

      if (userService.wouldRemoveLastEnabledSuperadmin(
          userFormData.getId(),
          !userFormData.isAdministrativelyBlocked()
              && editedUserEntity.getAccountDeactivation() == null,
          roleEntities)) {
        showError(messageProperties::getAdminUsersErrorLastActiveSuperadmin);
        return false;
      }

      return true;
    }

    private void persistUser(Set<RoleEntity> roleEntities) {
      UserEntity updatedUserEntity =
          UserEntity.builder()
              .id(userFormData.getId())
              .firstName(trimOptionalText(userFormData.getFirstName()))
              .lastName(trimOptionalText(userFormData.getLastName()))
              .email(trimOptionalText(userFormData.getEmail()))
              .gender(userFormData.getGender())
              .birthDate(userFormData.getBirthDate())
              .enabled(true)
              .administrativelyBlocked(userFormData.isAdministrativelyBlocked())
              .verificationPending(false)
              .developmentAccount(!editMode && developmentAccountCheckbox.getValue())
              .preferredLocale(CustomI18NProvider.getCurrentLocale())
              .roleEntities(roleEntities)
              .build();
      if (editMode) {
        if (adminAccountMailService != null) {
          try {
            adminAccountMailService.updateUser(
                updatedUserEntity, expectedUserVersion, userFormData.getPassword());
          } catch (IOException failure) {
            throw new IllegalStateException(
                "Verification mail for the changed address could not be prepared", failure);
          }
        } else {
          userService.updateAdminUser(
              updatedUserEntity, expectedUserVersion, userFormData.getPassword());
        }
      } else {
        if (adminAccountMailService != null) {
          try {
            adminAccountMailService.createUser(
                updatedUserEntity,
                userFormData.getPassword(),
                !emailVerificationRequiredCheckbox.getValue());
          } catch (IOException failure) {
            throw new IllegalStateException(
                "Initial verification mail could not be prepared", failure);
          }
        } else {
          userService.createAdminUser(updatedUserEntity, userFormData.getPassword());
        }
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

    private Set<RoleEntity> resolveSelectedRoleEntities(Set<RoleType> selectedRoles) {
      return Optional.ofNullable(selectedRoles).orElseGet(Set::of).stream()
          .map(roleService::findByName)
          .flatMap(Optional::stream)
          .collect(Collectors.toSet());
    }
  }

  private Component createActionButtons(UserEntity userEntity) {
    Button editUserButton = new Button(AppIcon.EDIT.create(), clickEvent -> openDialog(userEntity));
    editUserButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    editUserButton.setTooltipText(messageProperties.getAdminUsersEdit());
    boolean protectedAccount =
        userEntity.hasRole(RoleType.ROLE_SUPERADMIN) && !userService.canManageSuperadmins();
    editUserButton.setEnabled(!protectedAccount);

    Button deleteUserButton =
        new Button(AppIcon.TRASH.create(), clickEvent -> deleteUser(userEntity));
    deleteUserButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
    deleteUserButton.addClassName(DELETE_BUTTON);
    deleteUserButton.setTooltipText(messageProperties.getAdminUsersDelete());
    deleteUserButton.setEnabled(!isCurrentUser(userEntity) && !protectedAccount);

    HorizontalLayout userActionsLayout = new HorizontalLayout(editUserButton, deleteUserButton);
    userActionsLayout.addClassName("admin-user-table-actions");
    userActionsLayout.setPadding(false);
    userActionsLayout.setSpacing(true);
    userActionsLayout.setJustifyContentMode(JustifyContentMode.CENTER);
    return userActionsLayout;
  }

  private Component createEnabledBadge(UserEntity userEntity) {
    boolean accountEnabled = !userEntity.isAdministrativelyBlocked();
    Badge accountEnabledBadge =
        new Badge(
            accountEnabled
                ? messageProperties.getAdminUsersYes()
                : messageProperties.getAdminUsersNo());
    accountEnabledBadge.addThemeVariants(
        accountEnabled ? BadgeVariant.SUCCESS : BadgeVariant.ERROR);
    accountEnabledBadge.addClassName("admin-user-table-enabled");
    return accountEnabledBadge;
  }

  private Component createUserIdentity(UserEntity userEntity) {
    OverlayUserIcon tableUserAvatarIcon =
        new OverlayUserIcon(
            messageProperties, userEntity, userEntity.getStatus(), IconSize.PIXEL_32);
    if (userAvatarService != null) {
      tableUserAvatarIcon.configureAvatar(
          userAvatarService,
          userAvatarStates.getOrDefault(userEntity.getId(), UserAvatarService.State.initial()));
    }
    tableUserAvatarIcon.addClassName("admin-user-table-avatar");

    Span tableUserNameSpan = new Span(getFullName(userEntity));
    tableUserNameSpan.addClassName("admin-user-table-name");
    Div tableUserLabelsDiv = new Div(tableUserNameSpan);
    if (isCurrentUser(userEntity)) {
      Badge currentUserBadge = new Badge(messageProperties.getAdminUsersSelf());
      currentUserBadge.addThemeVariants(BadgeVariant.SUCCESS);
      currentUserBadge.addClassName("admin-user-self");
      tableUserLabelsDiv.add(currentUserBadge);
    }

    HorizontalLayout tableUserIdentityLayout =
        new HorizontalLayout(tableUserAvatarIcon, tableUserLabelsDiv);
    tableUserIdentityLayout.addClassName("admin-user-table-identity");
    tableUserIdentityLayout.setAlignItems(Alignment.CENTER);
    tableUserIdentityLayout.setPadding(false);
    tableUserIdentityLayout.setSpacing(true);
    return tableUserIdentityLayout;
  }

  private Component createStatusBadge(UserEntity userEntity) {
    UserStatus userStatus = userEntity.getStatus();
    Badge userStatusBadge =
        new Badge(userStatus == null ? "-" : messageProperties.getUserStatusLabel(userStatus));
    BadgeVariant statusBadgeVariant =
        userStatus == null
            ? BadgeVariant.CONTRAST
            : switch (userStatus) {
              case AVAILABLE -> BadgeVariant.SUCCESS;
              case ABSENT -> BadgeVariant.WARNING;
              case EMPLOYED -> BadgeVariant.ERROR;
              case OFFLINE -> BadgeVariant.CONTRAST;
            };
    userStatusBadge.addThemeVariants(statusBadgeVariant);
    userStatusBadge.addClassName("admin-user-table-status");

    if (userStatus != null) {
      AppIcon statusAppIcon =
          switch (userStatus) {
            case AVAILABLE -> AppIcon.CHECK_CIRCLE;
            case EMPLOYED -> AppIcon.CIRCLE_MINUS;
            case ABSENT -> AppIcon.CLOCK;
            case OFFLINE -> AppIcon.CLOSE_CIRCLE;
          };
      SvgIcon statusSvgIcon = statusAppIcon.create();
      statusSvgIcon.getElement().setAttribute(ARIA_HIDDEN, "true");
      userStatusBadge.setIcon(statusSvgIcon);
    }

    return userStatusBadge;
  }

  private Component createRoleBadges(UserEntity userEntity) {
    Div roleBadgesDiv = new Div();
    roleBadgesDiv.addClassName("admin-user-table-roles");
    Optional.ofNullable(userEntity.getRoleEntities()).orElseGet(Set::of).stream()
        .map(RoleEntity::getName)
        .map(this::formatRoleType)
        .sorted(String.CASE_INSENSITIVE_ORDER)
        .forEach(roleName -> {
          Badge roleBadge = new Badge(roleName);
          roleBadge.addClassName("cosmos-admin-role-chip");
          roleBadgesDiv.add(roleBadge);
        });
    return roleBadgesDiv;
  }

  private String formatRoleNames(UserEntity userEntity) {
    return Optional.ofNullable(userEntity.getRoleEntities()).orElseGet(Set::of).stream()
        .map(RoleEntity::getName)
        .map(this::formatRoleType)
        .sorted(String.CASE_INSENSITIVE_ORDER)
        .collect(Collectors.joining(", "));
  }

  private String getFullName(UserEntity userEntity) {
    String firstName = trimOptionalText(userEntity.getFirstName());
    String lastName = trimOptionalText(userEntity.getLastName());
    String fullName = (firstName + " " + lastName).trim();
    return fullName.isBlank() ? "-" : fullName;
  }

  private Component createLastActivity(UserEntity userEntity) {
    Instant lastActivityInstant = userEntity.getLastActivity();
    Instant currentInstant = userManagementClock.instant();
    String lastActivityText =
        lastActivityInstant != null && lastActivityInstant.isAfter(currentInstant)
            ? formatDateTime(lastActivityInstant)
            : AdminUserActivityText.format(lastActivityInstant, currentInstant, messageProperties);

    Span lastActivitySpan = new Span(lastActivityText);
    if (lastActivityInstant != null) {
      lastActivitySpan.getElement().setAttribute("title", formatDateTime(lastActivityInstant));
    }

    return lastActivitySpan;
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

  private boolean isCurrentUser(UserEntity userEntity) {
    return securityService
        .getCurrentUser()
        .map(currentUser -> Objects.equals(currentUser.getId(), userEntity.getId()))
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
    if (exception instanceof AccessDeniedException) {
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

  private String trimOptionalText(String optionalText) {
    return optionalText == null ? "" : optionalText.trim();
  }

  private String normalizeSearchText(String optionalText) {
    return trimOptionalText(optionalText).toLowerCase(Locale.ROOT);
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
    private boolean administrativelyBlocked;
    private Set<RoleType> roles;

    private static AdminUserFormData fromUser(UserEntity userEntity) {
      AdminUserFormData userFormData = new AdminUserFormData();
      userFormData.id = userEntity.getId();
      userFormData.firstName = Optional.ofNullable(userEntity.getFirstName()).orElse("");
      userFormData.lastName = Optional.ofNullable(userEntity.getLastName()).orElse("");
      userFormData.email = Optional.ofNullable(userEntity.getEmail()).orElse("");
      userFormData.password = "";
      userFormData.confirmPassword = "";
      userFormData.gender = userEntity.getGender();
      userFormData.birthDate = userEntity.getBirthDate();
      userFormData.administrativelyBlocked = userEntity.isAdministrativelyBlocked();
      userFormData.roles =
          Optional.ofNullable(userEntity.getRoleEntities()).orElseGet(Set::of).stream()
              .map(RoleEntity::getName)
              .collect(Collectors.toSet());
      return userFormData;
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
