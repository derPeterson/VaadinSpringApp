package de.derpeterson.app.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.*;
import de.derpeterson.app.service.PasswordResetService;
import lombok.RequiredArgsConstructor;

@Route("reset-password")
@PageTitle("Reset Password")
@RequiredArgsConstructor
public class ResetPasswordView extends VerticalLayout implements HasUrlParameter<String> {

    private final PasswordResetService passwordResetService;
    private String token;
    private PasswordField passwordField;

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String token) {
        this.token = token;
        if (passwordResetService.validateToken(token)) {
            showResetForm();
        } else {
            add(new Paragraph("Ungültiges oder abgelaufenes Token."));
        }
    }

    private void showResetForm() {
        H1 title = new H1("Neues Passwort eingeben");
        passwordField = new PasswordField("Neues Passwort");
        Button resetButton = new Button("Passwort ändern", event -> resetPassword());

        add(title, passwordField, resetButton);
    }

    private void resetPassword() {
        boolean success = passwordResetService.resetPassword(token, passwordField.getValue());
        if (success) {
            Notification.show("Passwort erfolgreich geändert.");
            getUI().ifPresent(ui -> ui.navigate("login"));
        } else {
            Notification.show("Passwort konnte nicht geändert werden.");
        }
    }
}
