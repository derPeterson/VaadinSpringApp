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
  private final AtomicReference<Locale> activeLocaleReference =
      new AtomicReference<>(Locale.GERMAN);
  private final MessageProperties messageProperties =
      new MessageProperties(new CustomI18NProvider());

  @BeforeEach
  void createPublicPageEnvironment() {
    testUi = new UI();
    vaadinSession = mock(VaadinSession.class);
    when(vaadinSession.hasLock()).thenReturn(true);
    when(vaadinSession.getLocale()).thenAnswer(mockInvocation -> activeLocaleReference.get());
    doAnswer(
            mockInvocation -> {
              activeLocaleReference.set(mockInvocation.getArgument(0));
              return null;
            })
        .when(vaadinSession)
        .setLocale(any());

    VaadinSession.setCurrent(vaadinSession);
    UI.setCurrent(testUi);
    testUi.getInternals().setSession(vaadinSession);
  }

  @AfterEach
  void clearPublicPageEnvironment() {
    CurrentInstance.set(VaadinRequest.class, null);
    CurrentInstance.set(VaadinResponse.class, null);

    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void publicLanguageChoiceIsRestoredInANewSession(String languageTag) {
    VaadinRequest vaadinRequest = mock(VaadinRequest.class);
    VaadinResponse vaadinResponse = mock(VaadinResponse.class);
    when(vaadinRequest.getContextPath()).thenReturn("");
    when(vaadinRequest.isSecure()).thenReturn(true);
    CurrentInstance.set(VaadinRequest.class, vaadinRequest);
    CurrentInstance.set(VaadinResponse.class, vaadinResponse);

    ConfigService configService = mock(ConfigService.class);
    when(configService.getString(ConfigEntry.SERVICE_NAME)).thenReturn("Review Service");
    when(configService.getString(ConfigEntry.COPYRIGHT_TEXT)).thenReturn("");

    PublicPageComponent publicPageComponent =
        new PublicPageComponent(configService, messageProperties, PublicPageComponent.Page.LOGIN);

    verifyNoInteractions(vaadinResponse);

    streamComponentTree(publicPageComponent)
        .filter(Button.class::isInstance)
        .map(Button.class::cast)
        .filter(candidateButton ->
            languageTag.equals(candidateButton.getElement().getAttribute("lang")))
        .findFirst()
        .orElseThrow()
        .click();

    ArgumentCaptor<Cookie> languageCookieCaptor = ArgumentCaptor.forClass(Cookie.class);

    verify(vaadinResponse).addCookie(languageCookieCaptor.capture());
    Cookie languageCookie = languageCookieCaptor.getValue();

    assertEquals("public-language", languageCookie.getName());
    assertEquals(languageTag, languageCookie.getValue());
    assertEquals("/", languageCookie.getPath());
    assertEquals(365 * 24 * 60 * 60, languageCookie.getMaxAge());
    assertEquals("Lax", languageCookie.getAttribute("SameSite"));
    assertTrue(languageCookie.isHttpOnly());
    assertTrue(languageCookie.getSecure());

    VaadinRequest nextVaadinRequest = mock(VaadinRequest.class);
    VaadinSession nextVaadinSession = mock(VaadinSession.class);
    when(nextVaadinRequest.getCookies()).thenReturn(new Cookie[] {languageCookie});
    PublicLanguagePreference.restore(nextVaadinRequest, nextVaadinSession);

    verify(nextVaadinSession).setLocale(Locale.forLanguageTag(languageTag));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "fr"})
  void unsupportedLanguageCookieLeavesTheBrowserDefaultInPlace(String languageTag) {
    VaadinRequest vaadinRequest = mock(VaadinRequest.class);
    VaadinSession nextVaadinSession = mock(VaadinSession.class);
    when(vaadinRequest.getCookies())
        .thenReturn(new Cookie[] {new Cookie("public-language", languageTag)});

    PublicLanguagePreference.restore(vaadinRequest, nextVaadinSession);

    verifyNoInteractions(nextVaadinSession);
  }

  @ParameterizedTest
  @EnumSource(PublicPageComponent.Page.class)
  void publicHeaderKeepsLiveLanguageAndOnlyOffersNavigationOutsideSecurity(
      PublicPageComponent.Page publicPageType) {
    ConfigService configService = mock(ConfigService.class);
    when(configService.getString(ConfigEntry.SERVICE_NAME)).thenReturn("Review Service");
    when(configService.getString(ConfigEntry.COPYRIGHT_TEXT)).thenReturn("");
    PublicPageComponent publicPageComponent =
        new PublicPageComponent(configService, messageProperties, publicPageType);
    Button panelActionButton = new Button("Action");
    AtomicReference<Boolean> actionClickedReference = new AtomicReference<>(false);
    panelActionButton.addClickListener(clickEvent -> actionClickedReference.set(true));
    Span panelTitleSpan = new Span(messageProperties.getLoginTitle());
    publicPageComponent
        .getCardContainer()
        .add(
            new PublicPanelComponent(
                AppIcon.SIGN_IN.create(),
                panelTitleSpan,
                messageProperties::getPublicLoginSubtitle,
                new Component[] {new Span("Example")},
                panelActionButton));
    boolean isSecurityPage =
        publicPageType == PublicPageComponent.Page.TWO_FACTOR
            || publicPageType == PublicPageComponent.Page.RECOVERY_HELP
            || publicPageType == PublicPageComponent.Page.EMAIL_VERIFICATION_PENDING
            || publicPageType == PublicPageComponent.Page.ACCOUNT_REACTIVATION_PENDING;

    assertPublicHeaderNavigation(publicPageComponent, isSecurityPage);

    Button englishLanguageButton =
        streamComponentTree(publicPageComponent)
            .filter(Button.class::isInstance)
            .map(Button.class::cast)
            .filter(
                candidateButton -> "en".equals(candidateButton.getElement().getAttribute("lang")))
            .findFirst()
            .orElseThrow();

    englishLanguageButton.click();

    assertEquals(Locale.ENGLISH, activeLocaleReference.get());
    assertEquals("true", englishLanguageButton.getElement().getAttribute("aria-pressed"));
    if (publicPageType == PublicPageComponent.Page.HOME) {
      assertTrue(
          streamComponentTree(publicPageComponent)
              .filter(Span.class::isInstance)
              .map(Span.class::cast)
              .anyMatch(candidateSpan ->
                  candidateSpan.getText().equals(messageProperties.getPublicHomeKicker())));
    }

    if (!isSecurityPage) {
      Button loginNavigationButton =
          streamComponentTree(publicPageComponent)
              .filter(candidateComponent ->
                  candidateComponent.getId().orElse("").equals("public-nav-login"))
              .map(Button.class::cast)
              .findFirst()
              .orElseThrow();

      assertEquals(messageProperties.getBaseLoginButton(), loginNavigationButton.getText());
      assertEquals(
          publicPageType == PublicPageComponent.Page.LOGIN,
          loginNavigationButton.hasClassName("public-nav-active"));
    }

    assertTrue(
        streamComponentTree(publicPageComponent)
            .filter(Span.class::isInstance)
            .map(Span.class::cast)
            .anyMatch(candidateSpan ->
                candidateSpan.getText().equals(messageProperties.getPublicLoginSubtitle())));

    panelActionButton.click();

    assertTrue(actionClickedReference.get());
  }

  private void assertPublicHeaderNavigation(
      PublicPageComponent publicPageComponent, boolean isSecurityPage) {
    List<String> brandLinkHrefs =
        streamComponentTree(publicPageComponent)
            .filter(candidateComponent -> candidateComponent.hasClassName("public-brand"))
            .filter(Anchor.class::isInstance)
            .map(Anchor.class::cast)
            .map(Anchor::getHref)
            .toList();

    assertEquals(isSecurityPage ? List.of() : List.of("/home"), brandLinkHrefs);
    assertEquals(
        isSecurityPage ? 0 : 3,
        streamComponentTree(publicPageComponent)
            .filter(candidateComponent ->
                candidateComponent.getId().orElse("").startsWith("public-nav-"))
            .count());
    assertFalse(
        streamComponentTree(publicPageComponent)
            .anyMatch(
                candidateComponent -> candidateComponent.hasClassName("tracked-user-button")));
    List<H1> pageHeadingH1Components =
        streamComponentTree(publicPageComponent)
            .filter(H1.class::isInstance)
            .map(H1.class::cast)
            .toList();
    assertEquals(1, pageHeadingH1Components.size());
    assertFalse(pageHeadingH1Components.getFirst().getText().isBlank());
  }

  private Stream<Component> streamComponentTree(Component rootComponent) {
    return Stream.concat(
        Stream.of(rootComponent), rootComponent.getChildren().flatMap(this::streamComponentTree));
  }
}
