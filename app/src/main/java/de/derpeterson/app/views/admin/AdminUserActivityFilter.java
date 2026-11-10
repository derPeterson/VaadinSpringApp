package de.derpeterson.app.views.admin;

import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.timepicker.TimePicker;
import de.derpeterson.app.helper.time.UserActivityTime;
import de.derpeterson.app.helper.ui.CompactDateInput;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.ui.components.CompactTimePicker;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/** Berlin calendar input; a date without a time starts at midnight. DST gaps are invalid. */
final class AdminUserActivityFilter extends CustomField<LocalDateTime> {
  private final DatePicker activityDatePicker = new DatePicker();
  private final TimePicker activityTimePicker = new CompactTimePicker();
  private final Clock activityClock;
  private boolean presenting;
  private LocalTime placeholderTime = LocalTime.MIDNIGHT;

  AdminUserActivityFilter() {
    this(Clock.systemUTC());
  }

  AdminUserActivityFilter(Clock activityClock) {
    super(null, true);
    this.activityClock = Objects.requireNonNull(activityClock);
    activityDatePicker.setAutoOpen(false);
    activityTimePicker.setAutoOpen(false);
    activityTimePicker.setEnabled(false);
    activityTimePicker.addClassName("admin-user-activity-time-picker");
    activityDatePicker.setClearButtonVisible(true);
    activityTimePicker.setClearButtonVisible(true);
    // Vaadin offers a time dropdown for steps of at least fifteen minutes.
    activityTimePicker.setStep(Duration.ofMinutes(15));
    activityDatePicker.setWidthFull();
    activityTimePicker.setWidthFull();
    HorizontalLayout fields = new HorizontalLayout(activityDatePicker, activityTimePicker);
    fields.setPadding(false);
    fields.setSpacing(false);
    fields.setWidthFull();
    fields.setAlignItems(FlexComponent.Alignment.END);
    fields.addClassName("admin-user-activity-inputs");
    add(fields);
    activityDatePicker.addValueChangeListener(event -> inputChanged());
    activityDatePicker.addValidationStatusChangeListener(event -> inputChanged());
    activityTimePicker.addValueChangeListener(event -> inputChanged());
    activityTimePicker.addValidationStatusChangeListener(event -> inputChanged());
    activityTimePicker.addFocusListener(event -> refreshPlaceholders());
    activityTimePicker
        .getElement()
        .addEventListener(
            "opened-changed",
            event -> {
              if (event.getEventData().path("event.detail.value").asBoolean()
                  && activityTimePicker.isEmpty()) {
                scrollToTimePlaceholder();
              }
            })
        .addEventData("event.detail.value");
  }

  void setLocale(Locale locale) {
    activityDatePicker.setLocale(locale);
    activityTimePicker.setLocale(locale);
    refreshPlaceholders();
  }

  void refreshTexts(MessageProperties messageProperties) {
    CompactDateInput.configureDatePickerInput(activityDatePicker, messageProperties);
    activityDatePicker.setLabel(messageProperties.getAdminUsersFilterActiveSince());
    activityDatePicker.setAriaLabel(messageProperties.getAdminUsersActivityDate());
    activityDatePicker.setPlaceholder(messageProperties.getRegistrationBirthDatePlaceholder());
    refreshPlaceholders();
    activityTimePicker.setAriaLabel(messageProperties.getAdminUsersActivityTimeOptional());
  }

  void refreshPlaceholders() {
    Locale locale = activityDatePicker.getLocale();
    boolean german = Locale.GERMAN.getLanguage().equals(locale.getLanguage());
    LocalDateTime now =
        LocalDateTime.ofInstant(activityClock.instant(), UserActivityTime.DISPLAY_ZONE);
    int stepMinutes = Math.toIntExact(activityTimePicker.getStep().toMinutes());
    LocalTime roundedTime =
        now.toLocalTime()
            .withMinute(now.getMinute() / stepMinutes * stepMinutes)
            .withSecond(0)
            .withNano(0);
    placeholderTime = roundedTime;
    String timePattern = german ? "HH:mm" : "h:mm a";
    activityTimePicker.setPlaceholder(
        roundedTime.format(DateTimeFormatter.ofPattern(timePattern, locale)));
  }

  private void scrollToTimePlaceholder() {
    refreshPlaceholders();
    // Vaadin 25 bietet keine öffentliche Java-Scrollmethode für den TimePicker.
    // Nur die Liste scrollen: Ein fokussierter Eintrag würde beim Schließen übernommen.
    activityTimePicker
        .getElement()
        .executeJs(
            """
            requestAnimationFrame(() => requestAnimationFrame(() => {
                if (!this.opened || this.value || this.inputElement?.value || this._focusedIndex >= 0) return;
                const scroller = this._scroller;
                if (!scroller?.opened || typeof scroller.scrollIntoView !== 'function') return;
                const index = scroller.items?.findIndex(item => item.value === $0);
                if (index >= 0) scroller.scrollIntoView(index);
            }));
            """,
            placeholderTime.format(DateTimeFormatter.ofPattern("HH:mm")));
  }

  private void inputChanged() {
    if (!presenting) {
      updateTimeAvailability();
      updateValue();
    }
  }

  private void updateTimeAvailability() {
    boolean validDate = activityDatePicker.getValue() != null && !activityDatePicker.isInvalid();
    activityTimePicker.setEnabled(validDate);
    if (!validDate) {
      boolean previousPresenting = presenting;
      presenting = true;
      try {
        activityTimePicker.clear();
        activityTimePicker.setInvalid(false);
      } finally {
        presenting = previousPresenting;
      }
    }
  }

  @Override
  protected LocalDateTime generateModelValue() {
    boolean invalid =
        activityDatePicker.isInvalid()
            || activityTimePicker.isInvalid()
            || (activityDatePicker.getValue() == null && activityTimePicker.getValue() != null);
    setInvalid(invalid);
    if (invalid || activityDatePicker.getValue() == null) {
      return null;
    }
    LocalDateTime local =
        activityDatePicker
            .getValue()
            .atTime(
                activityTimePicker.getValue() == null
                    ? LocalTime.MIDNIGHT
                    : activityTimePicker.getValue());
    try {
      UserActivityTime.inclusiveCutoff(local);
      return local;
    } catch (DateTimeException _) {
      setInvalid(true);
      return null;
    }
  }

  @Override
  protected void setPresentationValue(LocalDateTime value) {
    presenting = true;
    try {
      activityDatePicker.setValue(value == null ? null : value.toLocalDate());
      activityTimePicker.setValue(value == null ? null : value.toLocalTime());
      if (value == null) {
        activityDatePicker.setInvalid(false);
        activityTimePicker.setInvalid(false);
        setInvalid(false);
      }
      updateTimeAvailability();
      generateModelValue();
    } finally {
      presenting = false;
    }
  }

  @Override
  public void clear() {
    // Also clear partial input when the combined model value is already null.
    setPresentationValue(null);
    updateValue();
  }
}
