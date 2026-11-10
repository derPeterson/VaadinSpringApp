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
  private boolean isUpdatingPresentation;
  private LocalTime placeholderLocalTime = LocalTime.MIDNIGHT;

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

    HorizontalLayout activityInputsLayout =
        new HorizontalLayout(activityDatePicker, activityTimePicker);
    activityInputsLayout.setPadding(false);
    activityInputsLayout.setSpacing(false);
    activityInputsLayout.setWidthFull();
    activityInputsLayout.setAlignItems(FlexComponent.Alignment.END);
    activityInputsLayout.addClassName("admin-user-activity-inputs");
    add(activityInputsLayout);

    activityDatePicker.addValueChangeListener(_ -> handleActivityInputChange());
    activityDatePicker.addValidationStatusChangeListener(_ -> handleActivityInputChange());
    activityTimePicker.addValueChangeListener(_ -> handleActivityInputChange());
    activityTimePicker.addValidationStatusChangeListener(_ -> handleActivityInputChange());

    activityTimePicker.addFocusListener(_ -> refreshActivityTimePlaceholder());
    activityTimePicker
        .getElement()
        .addEventListener(
            "opened-changed",
            openedChangeDomEvent -> {
              if (openedChangeDomEvent.getEventData().path("event.detail.value").asBoolean()
                  && activityTimePicker.isEmpty()) {
                scrollToActivityTimePlaceholder();
              }
            })
        .addEventData("event.detail.value");
  }

  void setLocale(Locale locale) {
    activityDatePicker.setLocale(locale);
    activityTimePicker.setLocale(locale);
    refreshActivityTimePlaceholder();
  }

  void refreshTexts(MessageProperties messageProperties) {
    CompactDateInput.configureDatePickerInput(activityDatePicker, messageProperties);
    activityDatePicker.setLabel(messageProperties.getAdminUsersFilterActiveSince());
    activityDatePicker.setAriaLabel(messageProperties.getAdminUsersActivityDate());
    activityDatePicker.setPlaceholder(messageProperties.getRegistrationBirthDatePlaceholder());
    refreshActivityTimePlaceholder();
    activityTimePicker.setAriaLabel(messageProperties.getAdminUsersActivityTimeOptional());
  }

  void refreshActivityTimePlaceholder() {
    Locale locale = activityDatePicker.getLocale();
    boolean isGermanLocale = Locale.GERMAN.getLanguage().equals(locale.getLanguage());
    LocalDateTime currentActivityLocalDateTime =
        LocalDateTime.ofInstant(activityClock.instant(), UserActivityTime.DISPLAY_ZONE);
    int stepMinutes = Math.toIntExact(activityTimePicker.getStep().toMinutes());
    LocalTime roundedPlaceholderLocalTime =
        currentActivityLocalDateTime
            .toLocalTime()
            .withMinute(currentActivityLocalDateTime.getMinute() / stepMinutes * stepMinutes)
            .withSecond(0)
            .withNano(0);
    placeholderLocalTime = roundedPlaceholderLocalTime;

    String placeholderTimePattern = isGermanLocale ? "HH:mm" : "h:mm a";
    activityTimePicker.setPlaceholder(
        roundedPlaceholderLocalTime.format(
            DateTimeFormatter.ofPattern(placeholderTimePattern, locale)));
  }

  private void scrollToActivityTimePlaceholder() {
    refreshActivityTimePlaceholder();
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
            placeholderLocalTime.format(DateTimeFormatter.ofPattern("HH:mm")));
  }

  private void handleActivityInputChange() {
    if (!isUpdatingPresentation) {
      updateActivityTimePickerAvailability();
      updateValue();
    }
  }

  private void updateActivityTimePickerAvailability() {
    boolean hasValidActivityDate =
        activityDatePicker.getValue() != null && !activityDatePicker.isInvalid();
    activityTimePicker.setEnabled(hasValidActivityDate);
    if (!hasValidActivityDate) {
      boolean wasUpdatingPresentation = isUpdatingPresentation;
      isUpdatingPresentation = true;
      try {
        activityTimePicker.clear();
        activityTimePicker.setInvalid(false);
      } finally {
        isUpdatingPresentation = wasUpdatingPresentation;
      }
    }
  }

  @Override
  protected LocalDateTime generateModelValue() {
    boolean hasInvalidActivityInput =
        activityDatePicker.isInvalid()
            || activityTimePicker.isInvalid()
            || (activityDatePicker.getValue() == null && activityTimePicker.getValue() != null);
    setInvalid(hasInvalidActivityInput);
    if (hasInvalidActivityInput || activityDatePicker.getValue() == null) {
      return null;
    }

    LocalDateTime activityLocalDateTime =
        activityDatePicker
            .getValue()
            .atTime(
                activityTimePicker.getValue() == null
                    ? LocalTime.MIDNIGHT
                    : activityTimePicker.getValue());

    try {
      UserActivityTime.inclusiveCutoff(activityLocalDateTime);
      return activityLocalDateTime;
    } catch (DateTimeException _) {
      setInvalid(true);
      return null;
    }
  }

  @Override
  protected void setPresentationValue(LocalDateTime activityDateTimeValue) {
    isUpdatingPresentation = true;
    try {
      activityDatePicker.setValue(
          activityDateTimeValue == null ? null : activityDateTimeValue.toLocalDate());
      activityTimePicker.setValue(
          activityDateTimeValue == null ? null : activityDateTimeValue.toLocalTime());
      if (activityDateTimeValue == null) {
        activityDatePicker.setInvalid(false);
        activityTimePicker.setInvalid(false);
        setInvalid(false);
      }

      updateActivityTimePickerAvailability();
      generateModelValue();
    } finally {
      isUpdatingPresentation = false;
    }
  }

  @Override
  public void clear() {
    // Also clear partial input when the combined model value is already null.
    setPresentationValue(null);
    updateValue();
  }
}
