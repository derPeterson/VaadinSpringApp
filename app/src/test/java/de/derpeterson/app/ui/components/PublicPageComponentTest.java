package de.derpeterson.app.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.service.ConfigService;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class PublicPageComponentTest {
  private UI ui;
  private VaadinSession session;
  private final AtomicReference<Locale> locale = new AtomicReference<>(Locale.GERMAN);
  private final MessageProperties texts = new MessageProperties(new CustomI18NProvider());

  @BeforeEach
  void setup() {
    ui = new UI();
    session = mock(VaadinSession.class);
    when(session.hasLock()).thenReturn(true);
    when(session.getLocale()).thenAnswer(call -> locale.get());
    doAnswer(
            call -> {
              locale.set(call.getArgument(0));
              return null;
            })
        .when(session)
        .setLocale(any());
    VaadinSession.setCurrent(session);
    UI.setCurrent(ui);
    ui.getInternals().setSession(session);
  }

  @AfterEach
  void cleanup() {
    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
  }

  @ParameterizedTest
  @EnumSource(PublicPageComponent.Page.class)
  void everyPublicPageKeepsNavigationAndLiveLanguageWithoutAvatar(PublicPageComponent.Page scene) {
    ConfigService config = mock(ConfigService.class);
    when(config.getString(ConfigEntry.SERVICE_NAME)).thenReturn("Review Service");
    when(config.getString(ConfigEntry.COPYRIGHT_TEXT)).thenReturn("");
    PublicPageComponent page = new PublicPageComponent(config, texts, scene);
    Button nativeAction = new Button("Action");
    AtomicReference<Boolean> clicked = new AtomicReference<>(false);
    nativeAction.addClickListener(event -> clicked.set(true));
    Span title = new Span(texts.getLoginTitle());
    page.getCardContainer()
        .add(
            new PublicPanelComponent(
                AppIcon.SIGN_IN.create(),
                title,
                texts::getPublicLoginSubtitle,
                new Component[] {new Span("Example")},
                nativeAction));
    assertEquals(
        "/home",
        tree(page)
            .filter(Anchor.class::isInstance)
            .map(Anchor.class::cast)
            .findFirst()
            .orElseThrow()
            .getHref());
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
    assertEquals(Locale.ENGLISH, locale.get());
    assertEquals("true", english.getElement().getAttribute("aria-pressed"));
    if (scene == PublicPageComponent.Page.HOME) {
      assertTrue(
          tree(page)
              .filter(Span.class::isInstance)
              .map(Span.class::cast)
              .anyMatch(span -> span.getText().equals(texts.getPublicHomeKicker())));
    }
    assertEquals(
        texts.getBaseLoginButton(),
        tree(page)
            .filter(c -> c.getId().orElse("").equals("public-nav-login"))
            .map(Button.class::cast)
            .findFirst()
            .orElseThrow()
            .getText());
    assertTrue(
        tree(page)
            .filter(Span.class::isInstance)
            .map(Span.class::cast)
            .anyMatch(span -> span.getText().equals(texts.getPublicLoginSubtitle())));
    nativeAction.click();
    assertTrue(clicked.get());
  }

  private Stream<Component> tree(Component root) {
    return Stream.concat(Stream.of(root), root.getChildren().flatMap(this::tree));
  }
}
