package de.derpeterson.app.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.VerificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Router callback contracts; actual rendering and page title are checked by VER-02A. */
class VerificationViewNavigationTest {
    private UI ui;
    private VaadinSession session;
    private VerificationService service;
    private VerificationView view;
    private final MessageProperties texts = new MessageProperties(new CustomI18NProvider());

    @BeforeEach
    void setup() {
        ui = spy(new UI());
        session = mock(VaadinSession.class);
        when(session.getLocale()).thenReturn(Locale.ENGLISH);
        UI.setCurrent(ui);
        VaadinSession.setCurrent(session);
        service = mock(VerificationService.class);
        view = new VerificationView(texts, service, mock(SecurityService.class), mock(HttpServletRequest.class));
    }

    @AfterEach
    void cleanup() {
        UI.setCurrent(null);
        VaadinSession.setCurrent(null);
        view = null;
        service = null;
        session = null;
        ui = null;
    }

    @ParameterizedTest
    @NullAndEmptySource
    void missingTokenForwardsWithinCurrentNavigationWithoutCallingTokenService(String token) {
        var event = mock(BeforeEvent.class);
        view.setParameter(event, token);
        verify(event).forwardTo(HomeView.class);
        verifyNoMoreInteractions(event);
        verifyNoInteractions(service);
        verify(ui, never()).navigate(HomeView.class);
        assertNull(ReflectionTestUtils.getField(view, "successTitle"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"valid", "expired", "unknown", " "})
    void suppliedTokenRetainsValidationAndCardSelectionWithoutForward(String token) {
        when(service.validateToken(token)).thenReturn(token.equals("valid"));
        if (!token.equals("valid")) when(service.existsToken(token)).thenReturn(token.equals("expired"));
        var event = mock(BeforeEvent.class);
        view.setParameter(event, token);
        verify(service).validateToken(token);
        if (!token.equals("valid")) verify(service).existsToken(token);
        verifyNoMoreInteractions(service);
        verifyNoInteractions(event);
        verify(ui, never()).navigate(HomeView.class);
        String field = token.equals("valid") ? "successTitle" : token.equals("expired") ? "expiredTitle" : "notFoundTitle";
        String title = token.equals("valid") ? texts.getVerificationSuccessTitle()
                : token.equals("expired") ? texts.getVerificationExpiredTitle() : texts.getVerificationNotFoundTitle();
        assertEquals(title, ((H1) ReflectionTestUtils.getField(view, field)).getText());
        if (!token.equals("valid")) assertNull(ReflectionTestUtils.getField(view, "successTitle"));
    }
}
