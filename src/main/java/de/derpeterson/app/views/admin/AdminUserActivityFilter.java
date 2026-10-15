package de.derpeterson.app.views.admin;

import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.timepicker.TimePicker;
import de.derpeterson.app.i18n.MessageProperties;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Inclusive local cutoff: a date without a time starts at midnight.
 */
final class AdminUserActivityFilter extends CustomField<LocalDateTime> {
    private final DatePicker date = new DatePicker();
    private final TimePicker time = new TimePicker();
    private boolean presenting;
    private LocalTime placeholderTime = LocalTime.MIDNIGHT;

    AdminUserActivityFilter() {
        super(null, true);
        date.setAutoOpen(false);
        time.setAutoOpen(false);
        time.setEnabled(false);
        time.addClassName("admin-user-activity-time-picker");
        date.setClearButtonVisible(true);
        time.setClearButtonVisible(true);
        // Vaadin offers a time dropdown for steps of at least fifteen minutes.
        time.setStep(Duration.ofMinutes(15));
        date.setWidthFull();
        time.setWidthFull();
        HorizontalLayout fields = new HorizontalLayout(date, time);
        fields.setPadding(false);
        fields.setSpacing(false);
        fields.setWidthFull();
        fields.setAlignItems(FlexComponent.Alignment.END);
        fields.addClassName("admin-user-activity-inputs");
        add(fields);
        date.addValueChangeListener(event -> inputChanged());
        date.addValidationStatusChangeListener(event -> inputChanged());
        time.addValueChangeListener(event -> inputChanged());
        time.addValidationStatusChangeListener(event -> inputChanged());
        time.addFocusListener(event -> refreshPlaceholders());
        time.getElement().addEventListener("opened-changed", event -> {
            if (event.getEventData().path("event.detail.value").asBoolean() && time.isEmpty()) {
                scrollToTimePlaceholder();
            }
        }).addEventData("event.detail.value");
    }

    void setLocale(Locale locale) {
        date.setLocale(locale);
        time.setLocale(locale);
        refreshPlaceholders();
    }

    void refreshTexts(MessageProperties texts) {
        date.setLabel(texts.getAdminUsersFilterActiveSince());
        date.setAriaLabel(texts.getAdminUsersActivityDate());
        refreshPlaceholders();
        time.setAriaLabel(texts.getAdminUsersActivityTimeOptional());
    }

    void refreshPlaceholders() {
        Locale locale = date.getLocale();
        boolean german = Locale.GERMAN.getLanguage().equals(locale.getLanguage());
        LocalDateTime now = LocalDateTime.now();
        String datePattern = german ? "dd.MM.uuuu" : "MM/dd/uuuu";
        date.setPlaceholder(now.toLocalDate().format(DateTimeFormatter.ofPattern(datePattern, locale)));
        int stepMinutes = Math.toIntExact(time.getStep().toMinutes());
        LocalTime roundedTime = now.toLocalTime()
                .withMinute(now.getMinute() / stepMinutes * stepMinutes)
                .withSecond(0)
                .withNano(0);
        placeholderTime = roundedTime;
        String timePattern = german ? "HH:mm" : "h:mm a";
        time.setPlaceholder(roundedTime.format(DateTimeFormatter.ofPattern(timePattern, locale)));
    }

    private void scrollToTimePlaceholder() {
        refreshPlaceholders();
        // Vaadin 25 bietet keine öffentliche Java-Scrollmethode für den TimePicker.
        // Nur die Liste scrollen: Ein fokussierter Eintrag würde beim Schließen übernommen.
        time.getElement().executeJs("""
                requestAnimationFrame(() => requestAnimationFrame(() => {
                    if (!this.opened || this.value || this.inputElement?.value || this._focusedIndex >= 0) return;
                    const scroller = this._scroller;
                    if (!scroller?.opened || typeof scroller.scrollIntoView !== 'function') return;
                    const index = scroller.items?.findIndex(item => item.value === $0);
                    if (index >= 0) scroller.scrollIntoView(index);
                }));
                """, placeholderTime.format(DateTimeFormatter.ofPattern("HH:mm")));
    }

    private void inputChanged() {
        if (!presenting) {
            updateTimeAvailability();
            updateValue();
        }
    }

    private void updateTimeAvailability() {
        boolean validDate = date.getValue() != null && !date.isInvalid();
        time.setEnabled(validDate);
        if (!validDate) {
            boolean previousPresenting = presenting;
            presenting = true;
            try {
                time.clear();
                time.setInvalid(false);
            } finally {
                presenting = previousPresenting;
            }
        }
    }

    @Override
    protected LocalDateTime generateModelValue() {
        boolean invalid = date.isInvalid() || time.isInvalid()
                || (date.getValue() == null && time.getValue() != null);
        setInvalid(invalid);
        if (invalid || date.getValue() == null) return null;
        return date.getValue().atTime(time.getValue() == null ? LocalTime.MIDNIGHT : time.getValue());
    }

    @Override
    protected void setPresentationValue(LocalDateTime value) {
        presenting = true;
        try {
            date.setValue(value == null ? null : value.toLocalDate());
            time.setValue(value == null ? null : value.toLocalTime());
            if (value == null) {
                date.setInvalid(false);
                time.setInvalid(false);
                setInvalid(false);
            }
            updateTimeAvailability();
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
