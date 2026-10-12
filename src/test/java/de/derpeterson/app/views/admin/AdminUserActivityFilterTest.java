package de.derpeterson.app.views.admin;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.timepicker.TimePicker;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AdminUserActivityFilterTest {
    private UI ui;
    private AdminUserActivityFilter field;
    private final LocalDate day = LocalDate.of(2026, 10, 12);

    @BeforeEach
    void setup() { ui = new UI(); UI.setCurrent(ui); field = new AdminUserActivityFilter(); }

    @AfterEach
    void cleanup() { UI.setCurrent(null); }

    @Test
    void dateWithoutTimeFiltersFromMidnightAndLeavesTheTimeInputEmpty() {
        date().setValue(day);
        assertEquals(day.atStartOfDay(), field.getValue());
        assertNull(time().getValue());
        assertFalse(field.isInvalid());
    }

    @Test
    void selectedTimeIsUsedAndClearingOnlyTheTimeReturnsToMidnight() {
        date().setValue(day);
        time().setValue(LocalTime.of(14, 45));
        assertEquals(day.atTime(14, 45), field.getValue());
        time().clear();
        assertEquals(day.atStartOfDay(), field.getValue());
        assertNull(time().getValue());
    }

    @Test
    void timeWithoutDateIsInvalidAndClearRemovesPartialInputEvenWithANullModel() {
        time().setValue(LocalTime.NOON);
        assertNull(field.getValue());
        assertTrue(field.isInvalid());
        field.clear();
        assertNull(date().getValue());
        assertNull(time().getValue());
        assertNull(field.getValue());
        assertFalse(field.isInvalid());
    }

    @Test
    void programmaticValueProducesOneConsistentChangeAndClearRemovesBothParts() {
        List<LocalDateTime> changes = new ArrayList<>();
        field.addValueChangeListener(event -> changes.add(event.getValue()));
        LocalDateTime value = day.atTime(9, 30);
        field.setValue(value);
        assertEquals(day, date().getValue());
        assertEquals(LocalTime.of(9, 30), time().getValue());
        assertEquals(List.of(value), changes);
        field.clear();
        assertNull(field.getValue());
        assertNull(date().getValue());
        assertNull(time().getValue());
        assertEquals(Arrays.asList(value, null), changes);
    }

    @Test
    void invalidDateInputCannotBecomeAValidCutoffAndClearResetsTheError() {
        date().setInvalid(true);
        assertNull(field.generateModelValue());
        assertTrue(field.isInvalid());
        field.clear();
        assertFalse(date().isInvalid());
        assertFalse(field.isInvalid());
    }

    @ParameterizedTest
    @ValueSource(strings = {"de", "en"})
    void languageChangesUpdateBothInputLabelsWithoutChangingTheCutoff(String language) {
        Locale locale = Locale.forLanguageTag(language);
        MessageProperties texts = MessageProperties.forLocale(new CustomI18NProvider(), locale);
        LocalDateTime cutoff = day.atTime(10, 15);
        field.setValue(cutoff);
        field.setLocale(locale);
        field.refreshTexts(texts);
        assertEquals(cutoff, field.getValue());
        assertEquals(locale, date().getLocale());
        assertEquals(locale, time().getLocale());
        assertEquals(texts.getAdminUsersActivityDate(), date().getAriaLabel().orElseThrow());
        assertEquals(texts.getDateInputPlaceholder(), date().getPlaceholder());
        assertEquals(texts.getTimeInputPlaceholder(), time().getPlaceholder());
        assertEquals(texts.getAdminUsersActivityTimeOptional(),
                ((com.vaadin.flow.component.html.Span) ReflectionTestUtils.getField(field, "optionalTimeHint")).getText());
        assertFalse(date().isAutoOpen());
        assertFalse(time().isAutoOpen());
        assertEquals(texts.getAdminUsersActivityTimeOptional(), time().getAriaLabel().orElseThrow());
    }

    private DatePicker date() { return (DatePicker) ReflectionTestUtils.getField(field, "date"); }
    private TimePicker time() { return (TimePicker) ReflectionTestUtils.getField(field, "time"); }
}
