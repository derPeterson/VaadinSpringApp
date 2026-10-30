package de.derpeterson.designpilot;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.ColorScheme;
import com.vaadin.flow.theme.lumo.Lumo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Isolierter Gestaltungspilot; der Starter verwendet ausschließlich eigene Klassen/Ressourcen. */
@SpringBootApplication
@StyleSheet(Lumo.STYLESHEET)
@StyleSheet("pilot/typography.css")
@StyleSheet("pilot/personal-data.css")
@ColorScheme(ColorScheme.Value.DARK)
public class PersonalDataPilotApplication implements AppShellConfigurator {
  public static void main(String[] args) {
    SpringApplication.run(PersonalDataPilotApplication.class, args);
  }
}
