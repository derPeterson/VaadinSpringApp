package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.shared.Registration;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.EmailVerificationStatus;

/** Gemeinsame native Kennzeichnung des Nachweises für die aktuelle E-Mail-Adresse. */
@StyleSheet("custom-theme/cosmos-account.css")
public final class EmailVerificationBadge extends Badge {
  private Registration languageChangeRegistration;

  public EmailVerificationBadge(
      MessageProperties messageProperties, EmailVerificationStatus emailVerificationStatus) {
    setId("email-verification-status");
    addClassName("email-verification-badge");
    switch (emailVerificationStatus) {
      case VERIFIED -> {
        setText(messageProperties.getEmailVerified());
        addThemeVariants(BadgeVariant.SUCCESS);
      }
      case PENDING -> {
        setText(messageProperties.getEmailVerificationPending());
        addThemeVariants(BadgeVariant.WARNING);
      }
      case NOT_CHECKED -> {
        setText(messageProperties.getEmailNotChecked());
        Tooltip explanationTooltip =
            Tooltip.forComponent(this).withText(messageProperties.getEmailNotCheckedHint());
        addAttachListener(event ->
            languageChangeRegistration =
                ComponentUtil.addListener(
                    event.getUI(),
                    LanguageChangeEvent.class,
                    languageEvent -> {
                      setText(messageProperties.getEmailNotChecked());
                      explanationTooltip.setText(messageProperties.getEmailNotCheckedHint());
                    }));
        addDetachListener(event -> {
          if (languageChangeRegistration != null) {
            languageChangeRegistration.remove();
            languageChangeRegistration = null;
          }
        });
      }
    }
  }
}
