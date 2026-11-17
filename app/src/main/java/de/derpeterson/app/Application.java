package de.derpeterson.app;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.ColorScheme;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.aura.Aura;
import de.derpeterson.app.ui.components.CosmosBackgroundComponent;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@Push
@EnableScheduling
@ComponentScan(basePackages = "de.derpeterson.app")
@EntityScan(basePackages = {"de.derpeterson.app.model"})
@PWA(name = "VaadinSpringApp", shortName = "App", iconPath = "icons/service-logo.png")
@StyleSheet(Aura.STYLESHEET)
// Reihenfolge: Aura, Palette, Typografie, Tokens, Aura-Abgleich, gemeinsame Bausteine,
// App-Rahmen, Fachbereiche. Fachliche Einzeldateien laden die zuständigen Views selbst.
@StyleSheet("custom-theme/cosmos-palette.css")
@StyleSheet("custom-theme/typography.css")
@StyleSheet("custom-theme/cosmos-tokens.css")
@StyleSheet("custom-theme/cosmos-aura.css")
@StyleSheet("custom-theme/cosmos-scrollbars.css")
@StyleSheet("custom-theme/cosmos-duration-field.css")
@StyleSheet("custom-theme/cosmos-breadcrumbs.css")
@StyleSheet("custom-theme/cosmos-segmented-tabs.css")
@StyleSheet("custom-theme/cosmos-theme.css")
@StyleSheet("custom-theme/cosmos-utilities.css")
@StyleSheet("custom-theme/cosmos-cards.css")
@StyleSheet("custom-theme/cosmos-avatar.css")
@StyleSheet("custom-theme/cosmos-shell.css")
@StyleSheet(CosmosBackgroundComponent.STYLESHEET)
@StyleSheet("custom-theme/cosmos-admin.css")
@StyleSheet("custom-theme/cosmos-notifications.css")
@ColorScheme(ColorScheme.Value.DARK)
public class Application implements AppShellConfigurator {

  @Override
  public void configurePage(AppShellSettings appShellSettings) {
    appShellSettings.addFavIcon("icon", "icons/service-logo.svg", "any");
  }

  static void main(String[] applicationArguments) {
    SpringApplication.run(Application.class, applicationArguments);
  }
}
