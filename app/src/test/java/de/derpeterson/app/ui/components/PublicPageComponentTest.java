package de.derpeterson.app.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.i18n.PublicLanguagePreference;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.service.ConfigService;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

class PublicPageComponentTest {
  private UI testUi;
  private VaadinSession vaadinSession;
  private final AtomicReference<Locale> localeReference = new AtomicReference<>(Locale.GERMAN);
  private final MessageProperties messageProperties =
      new MessageProperties(new CustomI18NProvider());

  @BeforeEach
  void setup() {
    testUi = new UI();
    vaadinSession = mock(VaadinSession.class);
    when(vaadinSession.hasLock()).thenReturn(true);
    when(vaadinSession.getLocale()).thenAnswer(call -> localeReference.get());
    doAnswer(
            call -> {
              localeReference.set(call.getArgument(0));
              return null;
            })
        .when(vaadinSession)
        .setLocale(any());
    VaadinSession.setCurrent(vaadinSession);
    UI.setCurrent(testUi);
    testUi.getInternals().setSession(vaadinSession);
  }

  @AfterEach
  void cleanup() {
    CurrentInstance.set(VaadinRequest.class, null);
    CurrentInstance.set(VaadinResponse.class, null);
    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void publicLanguageChoiceIsRestoredInANewSession(String language) {
    VaadinRequest request = mock(VaadinRequest.class);
    VaadinResponse response = mock(VaadinResponse.class);
    when(request.getContextPath()).thenReturn("");
    when(request.isSecure()).thenReturn(true);
    CurrentInstance.set(VaadinRequest.class, request);
    CurrentInstance.set(VaadinResponse.class, response);
    ConfigService config = mock(ConfigService.class);
    when(config.getString(ConfigEntry.SERVICE_NAME)).thenReturn("Review Service");
    when(config.getString(ConfigEntry.COPYRIGHT_TEXT)).thenReturn("");
    PublicPageComponent page =
        new PublicPageComponent(config, messageProperties, PublicPageComponent.Page.LOGIN);
    verifyNoInteractions(response);

    tree(page)
        .filter(Button.class::isInstance)
        .map(Button.class::cast)
        .filter(button -> language.equals(button.getElement().getAttribute("lang")))
        .findFirst()
        .orElseThrow()
        .click();

    ArgumentCaptor<Cookie> cookies = ArgumentCaptor.forClass(Cookie.class);
    verify(response).addCookie(cookies.capture());
    Cookie saved = cookies.getValue();
    assertEquals("public-language", saved.getName());
    assertEquals(language, saved.getValue());
    assertEquals("/", saved.getPath());
    assertEquals(365 * 24 * 60 * 60, saved.getMaxAge());
    assertEquals("Lax", saved.getAttribute("SameSite"));
    assertTrue(saved.isHttpOnly());
    assertTrue(saved.getSecure());

    VaadinRequest nextRequest = mock(VaadinRequest.class);
    VaadinSession nextSession = mock(VaadinSession.class);
    when(nextRequest.getCookies()).thenReturn(new Cookie[] {saved});
    PublicLanguagePreference.restore(nextRequest, nextSession);
    verify(nextSession).setLocale(Locale.forLanguageTag(language));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "fr"})
  void unsupportedLanguageCookieLeavesTheBrowserDefaultInPlace(String language) {
    VaadinRequest request = mock(VaadinRequest.class);
    VaadinSession nextSession = mock(VaadinSession.class);
    when(request.getCookies()).thenReturn(new Cookie[] {new Cookie("public-language", language)});

    PublicLanguagePreference.restore(request, nextSession);

    verifyNoInteractions(nextSession);
  }

  @ParameterizedTest
  @EnumSource(PublicPageComponent.Page.class)
  void publicHeaderKeepsLiveLanguageAndOnlyOffersNavigationOutsideSecurity(
      PublicPageComponent.Page scene) {
    ConfigService config = mock(ConfigService.class);
    when(config.getString(ConfigEntry.SERVICE_NAME)).thenReturn("Review Service");
    when(config.getString(ConfigEntry.COPYRIGHT_TEXT)).thenReturn("");
    PublicPageComponent page = new PublicPageComponent(config, messageProperties, scene);
    Button nativeAction = new Button("Action");
    AtomicReference<Boolean> clicked = new AtomicReference<>(false);
    nativeAction.addClickListener(event -> clicked.set(true));
    Span title = new Span(messageProperties.getLoginTitle());
    page.getCardContainer()
        .add(
            new PublicPanelComponent(
                AppIcon.SIGN_IN.create(),
                title,
                messageProperties::getPublicLoginSubtitle,
                new Component[] {new Span("Example")},
                nativeAction));
    boolean restricted =
        scene == PublicPageComponent.Page.TWO_FACTOR
            || scene == PublicPageComponent.Page.RECOVERY_HELP;
    List<String> brandLinks =
        tree(page)
            .filter(component -> component.hasClassName("public-brand"))
            .filter(Anchor.class::isInstance)
            .map(Anchor.class::cast)
            .map(Anchor::getHref)
            .toList();
    assertEquals(restricted ? List.of() : List.of("/home"), brandLinks);
    assertEquals(
        restricted ? 0 : 3,
        tree(page)
            .filter(component -> component.getId().orElse("").startsWith("public-nav-"))
            .count());
    assertFalse(tree(page).anyMatch(c -> c.hasClassName("tracked-user-button")));
    assertTrue(
        tree(page)
            .filter(H1.class::isInstance)
            .map(H1.class::cast)
            .allMatch(h -> !h.getText().isBlank()));
    Button english =
        tree(page)
            .filter(Button.class::isInstance)
            .map(Button.class::cast)
            .filter(button -> "en".equals(button.getElement().getAttribute("lang")))
            .findFirst()
            .orElseThrow();
    english.click();
    assertEquals(Locale.ENGLISH, localeReference.get());
    assertEquals("true", english.getElement().getAttribute("aria-pressed"));
    if (scene == PublicPageComponent.Page.HOME) {
      assertTrue(
          tree(page)
              .filter(Span.class::isInstance)
              .map(Span.class::cast)
              .anyMatch(span -> span.getText().equals(messageProperties.getPublicHomeKicker())));
    }
    if (!restricted) {
      Button login =
          tree(page)
              .filter(c -> c.getId().orElse("").equals("public-nav-login"))
              .map(Button.class::cast)
              .findFirst()
              .orElseThrow();
      assertEquals(messageProperties.getBaseLoginButton(), login.getText());
      assertEquals(
          scene == PublicPageComponent.Page.LOGIN, login.hasClassName("public-nav-active"));
    }
    assertTrue(
        tree(page)
            .filter(Span.class::isInstance)
            .map(Span.class::cast)
            .anyMatch(span -> span.getText().equals(messageProperties.getPublicLoginSubtitle())));
    nativeAction.click();
    assertTrue(clicked.get());
  }

  private Stream<Component> tree(Component root) {
    return Stream.concat(Stream.of(root), root.getChildren().flatMap(this::tree));
  }
}
