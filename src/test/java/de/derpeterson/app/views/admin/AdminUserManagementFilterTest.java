package de.derpeterson.app.views.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
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
  private UI ui;
  private VaadinSession session;
  private AdminUserManagementSection section;
  private MessageProperties texts;
  private final AtomicReference<Locale> locale = new AtomicReference<>(Locale.ENGLISH);
  private final LocalDateTime cutoff = LocalDateTime.of(2026, 10, 12, 10, 30);

  @BeforeEach
  void setup() {
    ui = new UI();
    UI.setCurrent(ui);
    session = mock(VaadinSession.class);
    when(session.getLocale()).thenAnswer(call -> locale.get());
    VaadinSession.setCurrent(session);
    UserService service = mock(UserService.class);
    List<UserEntity> users =
        IntStream.rangeClosed(1, 25)
            .mapToObj(
                i ->
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
    when(service.findAllUsers()).thenReturn(users);
    texts = new MessageProperties(new CustomI18NProvider());
    section =
        new AdminUserManagementSection(
            texts, service, mock(RoleService.class), mock(SecurityService.class));
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
    assertEquals(
        List.of("user24@example.test"), visibleUsers().stream().map(UserEntity::getEmail).toList());
    assertEquals(0, field("currentPage"));
    filter("enabledFilter").setValue(false);
    applyFilters();
    assertTrue(visibleUsers().isEmpty());
    filter("enabledFilter").clear();
    applyFilters();
    assertEquals(1, visibleUsers().size());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void enumFiltersAndActivityStaySelectedAcrossLanguageChanges(String language) {
    filter("enabledFilter").setValue(true);
    filter("statusFilter").setValue(UserStatus.AVAILABLE);
    filter("roleFilter").setValue(RoleType.ROLE_USER);
    ((AdminUserActivityFilter) field("lastActivityFilter")).setValue(cutoff);
    applyFilters();
    assertEquals(6, visibleUsers().size());
    locale.set(Locale.forLanguageTag(language));
    section.refreshTexts();
    assertEquals(true, filter("enabledFilter").getValue());
    assertEquals(UserStatus.AVAILABLE, filter("statusFilter").getValue());
    assertEquals(RoleType.ROLE_USER, filter("roleFilter").getValue());
    assertEquals(cutoff, ((AdminUserActivityFilter) field("lastActivityFilter")).getValue());
    assertEquals(texts.getAdminUsersFilterRole(), filter("roleFilter").getLabel());
    assertEquals(6, visibleUsers().size());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void repeatedAutumnHourHasDistinctTooltipsAndChronologicalSortingAndFiltering(String language) {
    locale.set(Locale.forLanguageTag(language));
    Instant first = Instant.parse("2026-10-25T00:30:00Z");
    Instant second = Instant.parse("2026-10-25T01:30:00Z");
    UserEntity earlier =
        UserEntity.builder().id(1L).firstName("Z earlier").lastActivity(first).build();
    UserEntity later =
        UserEntity.builder().id(2L).firstName("A later").lastActivity(second).build();
    UserService service = mock(UserService.class);
    when(service.findAllUsers()).thenReturn(List.of(later, earlier));
    Instant now = Instant.parse("2026-10-25T01:45:00Z");
    section =
        new AdminUserManagementSection(
            texts,
            service,
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
    Span earlierLabel = ReflectionTestUtils.invokeMethod(section, "createLastActivity", earlier);
    Span laterLabel = ReflectionTestUtils.invokeMethod(section, "createLastActivity", later);
    assertEquals(texts.getAdminUsersActivityHours(1), earlierLabel.getText());
    assertEquals(texts.getAdminUsersActivityMinutes(15), laterLabel.getText());
    assertTrue(earlierLabel.getElement().getAttribute("title").endsWith("+02:00"));
    assertTrue(laterLabel.getElement().getAttribute("title").endsWith("+01:00"));
    ((AdminUserActivityFilter) field("lastActivityFilter"))
        .setValue(LocalDateTime.of(2026, 10, 25, 2, 30));
    applyFilters();
    assertEquals(List.of(earlier, later), visibleUsers());
    assertTrue(chips().getFirst().getElement().getTextRecursively().contains("+02:00"));
    // A nonexistent draft must preserve the already applied autumn cutoff.
    ((AdminUserActivityFilter) field("lastActivityFilter"))
        .setValue(LocalDateTime.of(2026, 3, 29, 2, 30));
    applyFilters();
    assertEquals(List.of(earlier, later), visibleUsers());
    assertTrue(((AdminUserActivityFilter) field("lastActivityFilter")).isInvalid());
    UserEntity future = UserEntity.builder().lastActivity(now.plusSeconds(3600)).build();
    Span futureLabel = ReflectionTestUtils.invokeMethod(section, "createLastActivity", future);
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
    ((ComboBox<Integer>) ReflectionTestUtils.getField(field("pagination"), "pageSize"))
        .setValue(20);
    assertEquals(20, visibleUsers().size());
    paginationButtons()
        .filter(button -> button.getText().equals("2"))
        .findFirst()
        .orElseThrow()
        .click();
    assertEquals(5, visibleUsers().size());
    ((TextField) field("searchField")).setValue("no-such-user");
    assertTrue(visibleUsers().isEmpty());
    assertEquals(0, field("currentPage"));
    assertTrue(
        paginationButtons()
            .filter(button -> button.getText().isEmpty())
            .noneMatch(Button::isEnabled));
  }

  @Test
  void manyPagesUseBoundedNumberedNavigationWithFirstAndLast() {
    assertEquals(List.of(0, 1, 2, 3, 19), AdminUserPagination.pageIndices(0, 20));
    assertEquals(List.of(0, 8, 9, 10, 19), AdminUserPagination.pageIndices(9, 20));
    assertEquals(List.of(0, 16, 17, 18, 19), AdminUserPagination.pageIndices(19, 20));
    assertEquals(List.of(0), AdminUserPagination.pageIndices(0, 1));
  }

  @Test
  void draftFiltersWaitForApplyWhileSearchRemainsLive() {
    filter("enabledFilter").setValue(false);
    assertEquals(10, visibleUsers().size());
    ((TextField) field("searchField")).setValue("user24@example.test");
    assertEquals(1, visibleUsers().size());
    applyFilters();
    assertTrue(visibleUsers().isEmpty());
    assertEquals(1, chips().size());
  }

  @Test
  void removingAnAppliedBadgePreservesOtherAppliedCriteriaAndPendingEdits() {
    filter("enabledFilter").setValue(true);
    filter("statusFilter").setValue(UserStatus.AVAILABLE);
    applyFilters();
    filter("roleFilter").setValue(RoleType.ROLE_ADMIN);
    removeFilter(texts.getAdminUsersGridEnabled());
    assertNull(filter("enabledFilter").getValue());
    assertEquals(RoleType.ROLE_ADMIN, filter("roleFilter").getValue());
    assertEquals(10, visibleUsers().size());
    assertEquals(1, chips().size());
    applyFilters();
    assertTrue(visibleUsers().isEmpty());
    assertEquals(2, chips().size());
  }

  @Test
  void invalidActivityDoesNotReplaceAppliedFiltersAndResetClearsAllDrafts() {
    filter("enabledFilter").setValue(false);
    applyFilters();
    filter("enabledFilter").setValue(true);
    ((AdminUserActivityFilter) field("lastActivityFilter")).setInvalid(true);
    applyFilters();
    assertFalse(visibleUsers().getFirst().isEnabled());
    filterButton(texts.getAdminUsersClearFilters()).click();
    assertNull(filter("enabledFilter").getValue());
    assertFalse(((AdminUserActivityFilter) field("lastActivityFilter")).isInvalid());
    assertTrue(chips().isEmpty());
    assertEquals(10, visibleUsers().size());
  }

  @Test
  void openingAndClosingThePanelPreservesDraftsAndAppliedBadges() {
    filterButton(texts.getAdminUsersAddFilter()).click();
    filter("enabledFilter").setValue(false);
    filterButton(texts.getAdminUsersAddFilter()).click();
    assertEquals(false, filter("enabledFilter").getValue());
    assertTrue(chips().isEmpty());
    filterButton(texts.getAdminUsersAddFilter()).click();
    applyFilters();
    assertEquals(1, chips().size());
    locale.set(Locale.GERMAN);
    section.refreshTexts();
    assertTrue(
        chips().getFirst().getElement().getTextRecursively().contains(texts.getAdminUsersNo()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void newRoleFiltersUseAssignedRolesAndTranslateAppliedBadges(String language) {
    ComboBox<Object> roleFilter = filter("roleFilter");
    assertEquals(List.of(RoleType.values()), roleFilter.getListDataView().getItems().toList());
    UserEntity user = visibleUsers().getFirst();
    user.setRoleEntities(List.of(RoleEntity.builder().name(RoleType.ROLE_SUPERADMIN).build()));
    roleFilter.setValue(RoleType.ROLE_SUPERADMIN);
    applyFilters();
    assertEquals(List.of(user), visibleUsers());
    roleFilter.setValue(RoleType.ROLE_ADMIN);
    applyFilters();
    assertTrue(
        visibleUsers().stream().noneMatch(candidate -> candidate.getId().equals(user.getId())));
    roleFilter.setValue(RoleType.ROLE_SUPERADMIN);
    applyFilters();
    locale.set(Locale.forLanguageTag(language));
    section.refreshTexts();
    assertEquals(
        texts.getRoleSuperadmin(),
        roleFilter.getItemLabelGenerator().apply(RoleType.ROLE_SUPERADMIN));
    assertTrue(
        chips().getFirst().getElement().getTextRecursively().contains(texts.getRoleSuperadmin()));
    user.setRoleEntities(List.of(RoleEntity.builder().name(RoleType.ROLE_GUEST).build()));
    roleFilter.setValue(RoleType.ROLE_GUEST);
    applyFilters();
    assertEquals(List.of(user), visibleUsers());
    assertEquals(
        texts.getRoleGuest(), roleFilter.getItemLabelGenerator().apply(RoleType.ROLE_GUEST));
    assertTrue(chips().getFirst().getElement().getTextRecursively().contains(texts.getRoleGuest()));
  }

  private void applyFilters() {
    filterButton(texts.getAdminUsersApplyFilters()).click();
  }

  private Button filterButton(String label) {
    return descendants((com.vaadin.flow.component.Component) field("filterBar"))
        .filter(Button.class::isInstance)
        .map(Button.class::cast)
        .filter(button -> button.getText().equals(label))
        .findFirst()
        .orElseThrow();
  }

  private List<com.vaadin.flow.component.Component> chips() {
    return descendants((com.vaadin.flow.component.Component) field("filterBar"))
        .filter(component -> component.hasClassName("admin-user-active-filter-chip"))
        .toList();
  }

  private void removeFilter(String label) {
    descendants((com.vaadin.flow.component.Component) field("filterBar"))
        .filter(Button.class::isInstance)
        .map(Button.class::cast)
        .filter(
            button ->
                button.getAriaLabel().orElse("").equals(texts.getAdminUsersRemoveFilter(label)))
        .findFirst()
        .orElseThrow()
        .click();
  }

  private Object field(String name) {
    return ReflectionTestUtils.getField(section, name);
  }

  private ComboBox<Object> filter(String name) {
    return (ComboBox<Object>) field(name);
  }

  private Grid<UserEntity> grid() {
    return (Grid<UserEntity>) field("userGrid");
  }

  private List<UserEntity> visibleUsers() {
    return grid().getListDataView().getItems().toList();
  }

  private java.util.stream.Stream<Button> paginationButtons() {
    return descendants((com.vaadin.flow.component.Component) field("pagination"))
        .filter(Button.class::isInstance)
        .map(Button.class::cast);
  }

  private java.util.stream.Stream<com.vaadin.flow.component.Component> descendants(
      com.vaadin.flow.component.Component component) {
    return java.util.stream.Stream.concat(
        java.util.stream.Stream.of(component), component.getChildren().flatMap(this::descendants));
  }
}
