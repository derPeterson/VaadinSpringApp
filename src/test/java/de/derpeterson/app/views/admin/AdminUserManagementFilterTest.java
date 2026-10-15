package de.derpeterson.app.views.admin;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.*;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.RoleService;
import de.derpeterson.app.service.UserService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
        ui = new UI(); UI.setCurrent(ui);
        session = mock(VaadinSession.class);
        when(session.getLocale()).thenAnswer(call -> locale.get());
        VaadinSession.setCurrent(session);
        UserService service = mock(UserService.class);
        List<UserEntity> users = IntStream.rangeClosed(1, 25).mapToObj(i -> UserEntity.builder()
                .id((long) i).firstName(String.format("Name%02d", i)).lastName("Test")
                .email("user" + i + "@example.test").enabled(i % 2 == 0)
                .status(i % 2 == 0 ? UserStatus.AVAILABLE : UserStatus.OFFLINE)
                .lastActivity(cutoff.plusMinutes(i - 13))
                .roleEntities(List.of(RoleEntity.builder().name(i == 25 ? RoleType.ROLE_ADMIN : RoleType.ROLE_USER).build()))
                .build()).toList();
        when(service.findAllUsers()).thenReturn(users);
        texts = new MessageProperties(new CustomI18NProvider());
        section = new AdminUserManagementSection(texts, service, mock(RoleService.class), mock(SecurityService.class));
    }

    @AfterEach
    void cleanup() { UI.setCurrent(null); VaadinSession.setCurrent(null); }

    @Test
    void changingFiltersResetsPaginationAndFiltersTheWholeList() {
        paginationButtons().filter(button -> button.getText().equals("3")).findFirst().orElseThrow().click();
        assertEquals(5, visibleUsers().size());
        ((TextField) field("searchField")).setValue("user24@example.test");
        assertEquals(List.of("user24@example.test"), visibleUsers().stream().map(UserEntity::getEmail).toList());
        assertEquals(0, field("currentPage"));
        filter("enabledFilter").setValue(false);
        applyFilters();
        assertTrue(visibleUsers().isEmpty());
        filter("enabledFilter").clear();
        applyFilters();
        assertEquals(1, visibleUsers().size());
    }

    @ParameterizedTest @ValueSource(strings = {"de", "en"})
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

    @Test
    void sortingIsAppliedBeforeSlicingPages() {
        Grid<UserEntity> grid = grid();
        Grid.Column<UserEntity> column = (Grid.Column<UserEntity>) field("nameColumn");
        grid.sort(List.of(new com.vaadin.flow.component.grid.GridSortOrder<>(column,
                com.vaadin.flow.data.provider.SortDirection.DESCENDING)));
        assertEquals("Name25", visibleUsers().getFirst().getFirstName());
        paginationButtons().filter(button -> button.getText().equals("3")).findFirst().orElseThrow().click();
        assertEquals("Name05", visibleUsers().getFirst().getFirstName());
    }

    @Test
    void navigationSizeChangesAndEmptyResultsRemainConsistent() {
        ((ComboBox<Integer>) ReflectionTestUtils.getField(field("pagination"), "pageSize")).setValue(20);
        assertEquals(20, visibleUsers().size());
        paginationButtons().filter(button -> button.getText().equals("2")).findFirst().orElseThrow().click();
        assertEquals(5, visibleUsers().size());
        ((TextField) field("searchField")).setValue("no-such-user");
        assertTrue(visibleUsers().isEmpty());
        assertEquals(0, field("currentPage"));
        assertTrue(paginationButtons().filter(button -> button.getText().isEmpty()).noneMatch(Button::isEnabled));
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
        assertTrue(chips().getFirst().getElement().getTextRecursively().contains(texts.getAdminUsersNo()));
    }

    private void applyFilters() { filterButton(texts.getAdminUsersApplyFilters()).click(); }
    private Button filterButton(String label) {
        return descendants((com.vaadin.flow.component.Component) field("filterBar"))
                .filter(Button.class::isInstance).map(Button.class::cast)
                .filter(button -> button.getText().equals(label)).findFirst().orElseThrow();
    }
    private List<com.vaadin.flow.component.Component> chips() {
        return descendants((com.vaadin.flow.component.Component) field("filterBar"))
                .filter(component -> component.hasClassName("admin-user-active-filter-chip")).toList();
    }
    private void removeFilter(String label) {
        descendants((com.vaadin.flow.component.Component) field("filterBar"))
                .filter(Button.class::isInstance).map(Button.class::cast)
                .filter(button -> button.getAriaLabel().orElse("").equals(texts.getAdminUsersRemoveFilter(label)))
                .findFirst().orElseThrow().click();
    }

    private Object field(String name) { return ReflectionTestUtils.getField(section, name); }
    private ComboBox<Object> filter(String name) { return (ComboBox<Object>) field(name); }
    private Grid<UserEntity> grid() { return (Grid<UserEntity>) field("userGrid"); }
    private List<UserEntity> visibleUsers() { return grid().getListDataView().getItems().toList(); }
    private java.util.stream.Stream<Button> paginationButtons() {
        return descendants((com.vaadin.flow.component.Component) field("pagination"))
                .filter(Button.class::isInstance).map(Button.class::cast);
    }
    private java.util.stream.Stream<com.vaadin.flow.component.Component> descendants(com.vaadin.flow.component.Component component) {
        return java.util.stream.Stream.concat(java.util.stream.Stream.of(component), component.getChildren().flatMap(this::descendants));
    }
}
