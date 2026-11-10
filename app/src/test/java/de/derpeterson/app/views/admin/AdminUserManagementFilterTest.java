package de.derpeterson.app.views.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.helper.time.UserActivityTime;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.RoleService;
import de.derpeterson.app.service.UserService;
import de.derpeterson.app.ui.components.CosmosPagination;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

/** Filter and pagination callbacks with application entities; no database or browser. */
class AdminUserManagementFilterTest {
  private UI testUi;
  private VaadinSession vaadinSession;
  private AdminUserManagementSection userManagementSection;
  private MessageProperties messageProperties;
  private final AtomicReference<Locale> localeReference = new AtomicReference<>(Locale.ENGLISH);
  private final LocalDateTime cutoff = LocalDateTime.of(2026, 10, 12, 10, 30);

  @BeforeEach
  void setup() {
    testUi = new UI();
    UI.setCurrent(testUi);
    vaadinSession = mock(VaadinSession.class);
    when(vaadinSession.getLocale()).thenAnswer(call -> localeReference.get());
    VaadinSession.setCurrent(vaadinSession);
    UserService userService = mock(UserService.class);
    List<UserEntity> users =
        IntStream.rangeClosed(1, 25)
            .mapToObj(i ->
                UserEntity.builder()
                    .id((long) i)
                    .firstName(String.format("Name%02d", i))
                    .lastName("Test")
                    .email("user" + i + "@example.test")
                    .enabled(i % 2 == 0)
                    .status(i % 2 == 0 ? UserStatus.AVAILABLE : UserStatus.OFFLINE)
                    .lastActivity(UserActivityTime.inclusiveCutoff(cutoff.plusMinutes(i - 13)))
                    .roleEntities(
                        List.of(
                            RoleEntity.builder()
                                .name(i == 25 ? RoleType.ROLE_ADMIN : RoleType.ROLE_USER)
                                .build()))
                    .build())
            .toList();
    when(userService.findAllUsers()).thenReturn(users);
    messageProperties = new MessageProperties(new CustomI18NProvider());
    userManagementSection =
        new AdminUserManagementSection(
            messageProperties, userService, mock(RoleService.class), mock(SecurityService.class));
  }

  @AfterEach
  void cleanup() {
    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
  }

  @Test
  void changingFiltersResetsPaginationAndFiltersTheWholeList() {
    paginationButtons()
        .filter(button -> button.getText().equals("3"))
        .findFirst()
        .orElseThrow()
        .click();

    assertEquals(5, visibleUsers().size());
    ((TextField) field("searchField")).setValue("user24@example.test");
    applyFilters();

    assertEquals(
        List.of("user24@example.test"), visibleUsers().stream().map(UserEntity::getEmail).toList());
    assertEquals(0, field("currentPage"));
    filter("enabledFilter").setValue(AdminUserFilters.Enabled.NO);
    applyFilters();

    assertTrue(visibleUsers().isEmpty());
    filter("enabledFilter").setValue(AdminUserFilters.Enabled.ALL);
    applyFilters();

    assertEquals(1, visibleUsers().size());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void enumFiltersAndActivityStaySelectedAcrossLanguageChanges(String language) {
    filter("enabledFilter").setValue(AdminUserFilters.Enabled.YES);
    filter("statusFilter").setValue(Set.of(UserStatus.AVAILABLE));
    filter("roleFilter").setValue(Set.of(RoleType.ROLE_USER));
    ((AdminUserActivityFilter) field("lastActivityFilter")).setValue(cutoff);
    applyFilters();

    assertEquals(6, visibleUsers().size());
    localeReference.set(Locale.forLanguageTag(language));
    userManagementSection.refreshTexts();

    assertEquals(AdminUserFilters.Enabled.YES, filter("enabledFilter").getValue());
    assertEquals(Set.of(UserStatus.AVAILABLE), filter("statusFilter").getValue());
    assertEquals(Set.of(RoleType.ROLE_USER), filter("roleFilter").getValue());
    assertEquals(cutoff, ((AdminUserActivityFilter) field("lastActivityFilter")).getValue());
    assertEquals(
        messageProperties.getAdminUsersFilterRole(),
        ((com.vaadin.flow.component.checkbox.CheckboxGroup<?>) field("roleFilter")).getLabel());
    assertEquals(6, visibleUsers().size());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void repeatedAutumnHourHasDistinctTooltipsAndChronologicalSortingAndFiltering(String language) {
    localeReference.set(Locale.forLanguageTag(language));
    Instant first = Instant.parse("2026-10-25T00:30:00Z");
    Instant second = Instant.parse("2026-10-25T01:30:00Z");
    UserEntity earlier =
        UserEntity.builder().id(1L).firstName("Z earlier").lastActivity(first).build();
    UserEntity later =
        UserEntity.builder().id(2L).firstName("A later").lastActivity(second).build();
    UserService userService = mock(UserService.class);
    when(userService.findAllUsers()).thenReturn(List.of(later, earlier));
    Instant now = Instant.parse("2026-10-25T01:45:00Z");
    userManagementSection =
        new AdminUserManagementSection(
            messageProperties,
            userService,
            mock(RoleService.class),
            mock(SecurityService.class),
            Clock.fixed(now, ZoneId.of("America/New_York")));
    Grid.Column<UserEntity> column = (Grid.Column<UserEntity>) field("lastActivityColumn");
    grid()
        .sort(
            List.of(
                new com.vaadin.flow.component.grid.GridSortOrder<>(
                    column, SortDirection.ASCENDING)));

    assertEquals(List.of(earlier, later), visibleUsers());
    Span earlierLabel =
        ReflectionTestUtils.invokeMethod(userManagementSection, "createLastActivity", earlier);
    Span laterLabel =
        ReflectionTestUtils.invokeMethod(userManagementSection, "createLastActivity", later);

    assertEquals(messageProperties.getAdminUsersActivityHours(1), earlierLabel.getText());
    assertEquals(messageProperties.getAdminUsersActivityMinutes(15), laterLabel.getText());
    assertTrue(earlierLabel.getElement().getAttribute("title").endsWith("+02:00"));
    assertTrue(laterLabel.getElement().getAttribute("title").endsWith("+01:00"));
    ((AdminUserActivityFilter) field("lastActivityFilter"))
        .setValue(LocalDateTime.of(2026, 10, 25, 2, 30));
    applyFilters();

    assertEquals(List.of(earlier, later), visibleUsers());
    assertEquals(first, ((AdminUserFilters) field("appliedFilters")).activeSince());
    // A nonexistent draft must preserve the already applied autumn cutoff.
    ((AdminUserActivityFilter) field("lastActivityFilter"))
        .setValue(LocalDateTime.of(2026, 3, 29, 2, 30));
    applyFilters();

    assertEquals(List.of(earlier, later), visibleUsers());
    assertTrue(((AdminUserActivityFilter) field("lastActivityFilter")).isInvalid());
    UserEntity future = UserEntity.builder().lastActivity(now.plusSeconds(3600)).build();
    Span futureLabel =
        ReflectionTestUtils.invokeMethod(userManagementSection, "createLastActivity", future);

    assertEquals(futureLabel.getElement().getAttribute("title"), futureLabel.getText());
    assertTrue(futureLabel.getText().endsWith("+01:00"));
  }

  @Test
  void sortingIsAppliedBeforeSlicingPages() {
    Grid<UserEntity> grid = grid();
    Grid.Column<UserEntity> column = (Grid.Column<UserEntity>) field("nameColumn");
    grid.sort(
        List.of(
            new com.vaadin.flow.component.grid.GridSortOrder<>(
                column, com.vaadin.flow.data.provider.SortDirection.DESCENDING)));

    assertEquals("Name25", visibleUsers().getFirst().getFirstName());
    paginationButtons()
        .filter(button -> button.getText().equals("3"))
        .findFirst()
        .orElseThrow()
        .click();

    assertEquals("Name05", visibleUsers().getFirst().getFirstName());
  }

  @Test
  void navigationSizeChangesAndEmptyResultsRemainConsistent() {
    ((ComboBox<Integer>)
            ReflectionTestUtils.getField(field("userTablePagination"), "pageSizeSelector"))
        .setValue(20);

    assertEquals(20, visibleUsers().size());
    paginationButtons()
        .filter(button -> button.getText().equals("2"))
        .findFirst()
        .orElseThrow()
        .click();

    assertEquals(5, visibleUsers().size());
    ((TextField) field("searchField")).setValue("no-such-user");
    applyFilters();

    assertTrue(visibleUsers().isEmpty());
    assertEquals(0, field("currentPage"));
    assertTrue(
        paginationButtons()
            .filter(button -> button.getText().isEmpty())
            .noneMatch(Button::isEnabled));
  }

  @Test
  void manyPagesUseBoundedNumberedNavigationWithFirstAndLast() {
    assertEquals(List.of(0, 1, 2, 3, 19), CosmosPagination.pageIndices(0, 20));
    assertEquals(List.of(0, 8, 9, 10, 19), CosmosPagination.pageIndices(9, 20));
    assertEquals(List.of(0, 16, 17, 18, 19), CosmosPagination.pageIndices(19, 20));
    assertEquals(List.of(0), CosmosPagination.pageIndices(0, 1));
  }

  @Test
  void pageNavigationClampsExtremeIndicesWithoutIntegerOverflow() {
    assertEquals(List.of(0, 16, 17, 18, 19), CosmosPagination.pageIndices(Integer.MAX_VALUE, 20));
    assertEquals(List.of(0, 1, 2, 3, 19), CosmosPagination.pageIndices(Integer.MIN_VALUE, 20));
    assertEquals(
        List.of(
            0,
            Integer.MAX_VALUE - 4,
            Integer.MAX_VALUE - 3,
            Integer.MAX_VALUE - 2,
            Integer.MAX_VALUE - 1),
        CosmosPagination.pageIndices(Integer.MAX_VALUE, Integer.MAX_VALUE));
  }

  @Test
  void resetWaitsForApplyAndCancellationRestoresTheAppliedDraft() {
    filter("enabledFilter").setValue(AdminUserFilters.Enabled.NO);
    applyFilters();

    assertFalse(visibleUsers().getFirst().isEnabled());
    filterButton(messageProperties.getAdminUsersAddFilter()).click();
    filterButton(messageProperties.getAdminUsersClearFilters()).click();

    assertFalse(visibleUsers().getFirst().isEnabled());
    filterButton(messageProperties.getAdminUsersCancel()).click();

    assertEquals(AdminUserFilters.Enabled.NO, filter("enabledFilter").getValue());
    filterButton(messageProperties.getAdminUsersAddFilter()).click();
    filterButton(messageProperties.getAdminUsersClearFilters()).click();
    applyFilters();

    assertEquals(AdminUserFilters.Enabled.ALL, filter("enabledFilter").getValue());
    assertEquals(25, ((List<?>) field("filteredUsers")).size());
  }

  @Test
  void searchAndMultipleChoicesWaitForApplyAndInvalidActivityPreservesTheTable() {
    ((TextField) field("searchField")).setValue("user24@example.test");
    filter("statusFilter").setValue(Set.of(UserStatus.AVAILABLE, UserStatus.OFFLINE));

    assertEquals(10, visibleUsers().size());
    applyFilters();

    assertEquals(
        List.of("user24@example.test"), visibleUsers().stream().map(UserEntity::getEmail).toList());
    ((AdminUserActivityFilter) field("lastActivityFilter")).setInvalid(true);
    ((TextField) field("searchField")).clear();
    applyFilters();

    assertEquals(1, visibleUsers().size());
  }

  private void applyFilters() {
    filterButton(messageProperties.getAdminUsersApplyFilters()).click();
  }

  private Button filterButton(String label) {
    Object bar = field("filterBar");
    return List.of(
            "filterButton", "applyFiltersButton", "clearFiltersButton", "cancelFilterChangesButton")
        .stream()
        .map(name -> (Button) ReflectionTestUtils.getField(bar, name))
        .filter(button -> button.getText().equals(label))
        .findFirst()
        .orElseThrow();
  }

  private Object field(String name) {
    return ReflectionTestUtils.getField(userManagementSection, name);
  }

  private HasValue filter(String name) {
    return (HasValue) field(name);
  }

  private Grid<UserEntity> grid() {
    return (Grid<UserEntity>) field("userGrid");
  }

  private List<UserEntity> visibleUsers() {
    return grid().getListDataView().getItems().toList();
  }

  private java.util.stream.Stream<Button> paginationButtons() {
    return descendants((com.vaadin.flow.component.Component) field("userTablePagination"))
        .filter(Button.class::isInstance)
        .map(Button.class::cast);
  }

  private java.util.stream.Stream<com.vaadin.flow.component.Component> descendants(
      com.vaadin.flow.component.Component component) {
    return java.util.stream.Stream.concat(
        java.util.stream.Stream.of(component), component.getChildren().flatMap(this::descendants));
  }
}
