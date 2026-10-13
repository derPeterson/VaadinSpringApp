package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.shared.Registration;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.helper.text.CopyrightTextFormatter;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.service.ConfigService;

import java.time.Clock;
import java.util.function.Supplier;

/** Gemeinsamer Hintergrund, Marke, Card-Bereich und konfigurierbarer Copyright-Fußtext. */
public class CosmosPageComponent extends Div {
    private final VerticalLayout cards = new VerticalLayout();
    private transient Registration languageRegistration;

    public CosmosPageComponent(ConfigService config, boolean wide) {
        this(config, wide, Clock.systemDefaultZone());
    }

    public CosmosPageComponent(ConfigService config, boolean wide, Clock clock) {
        this(config, wide, clock, null);
    }

    public CosmosPageComponent(ConfigService config, boolean wide, Supplier<String> localizedDefaultTemplate) {
        this(config, wide, Clock.systemDefaultZone(), localizedDefaultTemplate);
    }

    private CosmosPageComponent(ConfigService config, boolean wide, Clock clock,
                                Supplier<String> localizedDefaultTemplate) {
        addClassNames("cosmos-page", "cosmos-theme");
        if (wide) addClassName("cosmos-page--wide");
        String serviceName = config.getString(ConfigEntry.SERVICE_NAME);
        String template = config.getString(ConfigEntry.COPYRIGHT_TEXT);

        Div brand = layer("cosmos-brand");
        Image logo = new Image("custom-theme/service_logo.png", serviceName);
        logo.addClassName("cosmos-logo");
        Span name = new Span(serviceName);
        name.addClassName("cosmos-brand-name");
        brand.add(logo, name);

        cards.addClassName("cosmos-cards");
        cards.setPadding(false);
        cards.setSpacing(false);
        cards.setAlignItems(FlexComponent.Alignment.STRETCH);

        Span footer = new Span();
        footer.addClassName("cosmos-footer");
        Runnable updateCopyright = () -> {
            String copyright = CopyrightTextFormatter.format(template, serviceName, clock, localizedDefaultTemplate);
            // Plain text statt HTML: auch eine bearbeitete Konfiguration bleibt sicherer Text.
            footer.setText(copyright);
            footer.setVisible(!copyright.isBlank());
        };
        updateCopyright.run();
        if (localizedDefaultTemplate != null) {
            addAttachListener(event -> {
                updateCopyright.run();
                languageRegistration = ComponentUtil.addListener(event.getUI(), LanguageChangeEvent.class,
                        languageEvent -> updateCopyright.run());
            });
            addDetachListener(event -> {
                if (languageRegistration != null) {
                    languageRegistration.remove();
                    languageRegistration = null;
                }
            });
        }

        VerticalLayout content = new VerticalLayout(brand, cards, footer);
        content.addClassName("cosmos-content");
        content.setPadding(false);
        content.setSpacing(false);
        content.setAlignItems(FlexComponent.Alignment.CENTER);
        content.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        add(createSky(), content);
    }

    public VerticalLayout getCardContainer() {
        return cards;
    }

    /** Portalisierte Notifications verwenden die Palette der aktuell angezeigten Kontoansicht. */
    public static boolean isDisplayed(UI ui) {
        return ui != null && containsCosmosPage(ui);
    }

    private static boolean containsCosmosPage(Component component) {
        return component instanceof CosmosPageComponent
                || component.getChildren().anyMatch(CosmosPageComponent::containsCosmosPage);
    }

    private static Div createSky() {
        Div sky = layer("cosmos-sky");
        sky.getElement().setAttribute("aria-hidden", "true");
        sky.add(layer("cosmos-stars", "cosmos-stars--small"),
                layer("cosmos-stars", "cosmos-stars--far"), layer("cosmos-galaxy"),
                layer("cosmos-glow", "cosmos-glow--blue"), layer("cosmos-glow", "cosmos-glow--violet"),
                layer("cosmos-horizon"), layer("cosmos-planet"), layer("cosmos-moon"));
        return sky;
    }

    private static Div layer(String... classes) {
        Div layer = new Div();
        layer.addClassNames(classes);
        return layer;
    }
}
