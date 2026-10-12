package de.derpeterson.app.views.admin;

import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.timepicker.TimePicker;
import de.derpeterson.app.i18n.MessageProperties;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Locale;

/**
 * Inclusive local cutoff: a date without a time starts at midnight.
 */
final class AdminUserActivityFilter extends CustomField<LocalDateTime> {
    private final DatePicker date = new DatePicker();
    private final TimePicker time = new TimePicker();
    private final Span caption = new Span();
    private final Span optionalTimeHint = new Span();
    private boolean presenting;

    AdminUserActivityFilter() {
        super(null, true);
        date.setAutoOpen(false);
        time.setAutoOpen(false);
        Span label = new Span(caption, optionalTimeHint);
        label.getElement().setAttribute("slot", "label");
        optionalTimeHint.addClassName("admin-user-activity-optional-note");
        // Screen readers already receive the optional-time input label below.
        optionalTimeHint.getElement().setAttribute("aria-hidden", "true");
        getElement().appendChild(label.getElement());
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
        fields.addClassName("admin-user-activity-inputs");
        add(fields);
        date.addValueChangeListener(event -> inputChanged());
        time.addValueChangeListener(event -> inputChanged());
    }

    void setLocale(Locale locale) {
        date.setLocale(locale);
        time.setLocale(locale);
    }

    void refreshTexts(MessageProperties texts) {
        caption.setText(texts.getAdminUsersFilterActiveSince());
        optionalTimeHint.setText(texts.getAdminUsersActivityTimeOptional());
        date.setAriaLabel(texts.getAdminUsersActivityDate());
        date.setPlaceholder(texts.getDateInputPlaceholder());
        time.setAriaLabel(texts.getAdminUsersActivityTimeOptional());
        time.setPlaceholder(texts.getTimeInputPlaceholder());
    }

    private void inputChanged() {
        if (!presenting) updateValue();
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
