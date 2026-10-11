package de.derpeterson.app.i18n;

import com.vaadin.flow.server.VaadinSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Locale lifecycle contracts; UI rendering remains in the browser suites. */
class MessagePropertiesLocaleTest {
    private final CustomI18NProvider provider = new CustomI18NProvider() {
        @Override
        public String getTranslation(String key, Locale locale, Object... parameters) {
            return locale.getLanguage();
        }
    };

    @AfterEach
    void cleanup() {
        VaadinSession.setCurrent(null);
    }

    @Test
    void normalGettersFollowTheSessionLanguageWithoutRecreatingTheBean() {
        AtomicReference<Locale> locale = new AtomicReference<>(Locale.GERMAN);
        VaadinSession session = mock(VaadinSession.class);
        when(session.getLocale()).thenAnswer(ignored -> locale.get());
        VaadinSession.setCurrent(session);
        MessageProperties messages = new MessageProperties(provider);
        assertEquals("de", messages.getLoginTitle());
        locale.set(Locale.ENGLISH);
        assertEquals("en", messages.getLoginTitle());
    }

    @Test
    void explicitLocaleWorksWithoutSessionAndDoesNotFollowLaterSessionChanges() {
        MessageProperties messages = MessageProperties.forLocale(provider, Locale.GERMAN);
        assertEquals("de", messages.getLoginTitle());
        VaadinSession session = mock(VaadinSession.class);
        when(session.getLocale()).thenReturn(Locale.ENGLISH);
        VaadinSession.setCurrent(session);
        assertEquals("de", messages.getLoginTitle());
        assertEquals(Locale.ENGLISH, session.getLocale());
    }
}
