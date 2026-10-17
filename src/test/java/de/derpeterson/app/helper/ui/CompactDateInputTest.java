package de.derpeterson.app.helper.ui;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

class CompactDateInputTest {
  private final AtomicReference<Locale> locale = new AtomicReference<>(Locale.GERMAN);
  private MessageProperties messages;
  private DatePicker field;

  @BeforeEach
  void setup() {
    var ui = new UI();
    var session = mock(VaadinSession.class);
    when(session.hasLock()).thenReturn(true);
    when(session.getLocale()).thenAnswer(call -> locale.get());
    UI.setCurrent(ui);
    VaadinSession.setCurrent(session);
    ui.getInternals().setSession(session);
    messages = new MessageProperties(new CustomI18NProvider());
    field = new DatePicker();
    field.setLocale(Locale.GERMAN);
    CompactDateInput.configure(field, messages);
  }

  @AfterEach
  void cleanup() {
    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
  }

  @ParameterizedTest
  @CsvSource({
    "de,260486,1986-04-26",
    "de,26041986,1986-04-26",
    "de,4586,1986-05-04",
    "de,2604,2004-06-02",
    "en,042686,1986-04-26",
    "en,04261986,1986-04-26",
    "en,4586,1986-04-05",
    "de,290200,2000-02-29",
    "en,022900,2000-02-29",
    "de,010150,1950-01-01",
    "de,010149,2049-01-01",
    "de,010100,2000-01-01",
    "en,123199,1999-12-31",
    "de,01010001,0001-01-01"
  })
  void compactDatesRespectOrderLeapYearsAndTheFixedCentury(
      String language, String input, String expected) throws Exception {
    field.setLocale(Locale.forLanguageTag(language));
    var parsed = field.getFallbackParser().apply(input);
    assertFalse(parsed.isError());
    assertEquals(LocalDate.parse(expected), parsed.getOrThrow(IllegalArgumentException::new));
    assertEquals(LocalDate.of(2000, 1, 1), field.getI18n().getReferenceDate());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "31022026",
        "290201",
        "000086",
        "260086",
        "320186",
        "01010000",
        "11086",
        "1101986",
        "0064",
        "123",
        "123456789",
        "26a486",
        "+260486",
        "26.04.1986"
      })
  void invalidAndAmbiguousFallbackInputNeverBecomesADate(String input) {
    assertTrue(field.getFallbackParser().apply(input).isError());
  }

  private void commit(String input) {
    field.getElement().setProperty("_inputElementValue", input);
    ReflectionTestUtils.invokeMethod(field, "setModelValue", null, true);
  }

  @Test
  void actualDatePickerFallbackCommitsAClientValueAndRetainsItsRangeValidation() {
    List<LocalDate> changes = new ArrayList<>();
    field.addValueChangeListener(
        event -> {
          assertTrue(event.isFromClient());
          changes.add(event.getValue());
        });
    field.setMax(LocalDate.of(2026, 10, 18));
    commit(" 260486 ");
    assertEquals(LocalDate.of(1986, 4, 26), field.getValue());
    assertEquals(List.of(LocalDate.of(1986, 4, 26)), changes);
    assertEquals("1986-04-26", field.getElement().getProperty("value"));
    assertFalse(field.isInvalid());
    commit("31022026");
    assertNull(field.getValue());
    assertTrue(field.isInvalid());
    assertEquals(messages.getBaseValidationDateInvalidMessage(), field.getErrorMessage());
    commit("01012050");
    assertEquals(LocalDate.of(2050, 1, 1), field.getValue());
    assertTrue(field.isInvalid());
    commit("");
    assertNull(field.getValue());
    assertFalse(field.isInvalid());
  }

  @Test
  void languageChangesUseTheCurrentFieldOrderAndTranslateNewErrors() {
    assertEquals(
        messages.getBaseValidationDateInvalidMessage(),
        field.getFallbackParser().apply("bad").getMessage().orElseThrow());
    locale.set(Locale.ENGLISH);
    field.setLocale(Locale.ENGLISH);
    assertEquals(
        "Please enter a valid date.",
        field.getFallbackParser().apply("bad").getMessage().orElseThrow());
    commit("4586");
    assertEquals(LocalDate.of(1986, 4, 5), field.getValue());
    locale.set(Locale.GERMAN);
    field.setLocale(Locale.GERMAN);
    assertEquals(LocalDate.of(1986, 4, 5), field.getValue());
    commit("4586");
    assertEquals(LocalDate.of(1986, 5, 4), field.getValue());
  }
}
