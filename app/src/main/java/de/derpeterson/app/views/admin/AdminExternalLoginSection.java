package de.derpeterson.app.views.admin;

import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_CARD;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_DESCRIPTION;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_HEADER;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_HEADING;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_ICON;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_TITLE;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import de.derpeterson.app.helper.logging.LogSafety;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.helper.ui.NotificationHelper.NotificationType;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.security.ExternalProviderSettings;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.ui.components.AppIcon;
import de.derpeterson.app.ui.components.CosmosCardComponent;
import de.derpeterson.app.ui.components.CosmosSectionHeaderComponent;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;

/** Geführte Einrichtung auf den vorhandenen Konfigurations-/Panelbausteinen. */
@StyleSheet("custom-theme/cosmos-provider-settings.css")
public class AdminExternalLoginSection extends VerticalLayout {
  private static final Logger logger = LoggerFactory.getLogger(AdminExternalLoginSection.class);
  private final transient MessageProperties texts;
  private final transient ExternalProviderSettings settings;
  private final transient ConfigService config;
  private final List<ProviderCard> cards = new ArrayList<>();
  private final Span breadcrumb = new Span();
  private final Span title = new Span();
  private final Span description = new Span();

  public AdminExternalLoginSection(
      MessageProperties texts, ExternalProviderSettings settings, ConfigService config) {
    this.texts = texts;
    this.settings = settings;
    this.config = config;
    setWidthFull();
    setPadding(false);
    setSpacing(false);
    addClassNames("cosmos-admin-section", "cosmos-provider-settings");
    setId("admin-provider-settings");
    add(new CosmosSectionHeaderComponent(breadcrumb, title, description));
    for (String provider : ExternalProviderSettings.PROVIDERS) {
      ProviderCard card = new ProviderCard(provider);
      cards.add(card);
      add(card.card);
    }
    refreshTexts();
  }

  public void activate() {
    for (ProviderCard card : cards) {
      card.load();
    }
    refreshTexts();
  }

  public void refreshTexts() {
    breadcrumb.setText(texts.getAdminHeaderTitle() + " / " + texts.getAdminConfigTitle());
    title.setText(texts.getAdminConfigTitle());
    description.setText(texts.getAdminConfigDescription());
    cards.forEach(ProviderCard::refreshTexts);
  }

  private final class ProviderCard {
    private final String provider;
    private final CosmosCardComponent card;
    private final Span status = new Span();
    private final Span subtitle = new Span();
    private final Span guide = new Span();
    private final Anchor console;
    private final TextField redirect = new TextField();
    private final Button copy = new Button();
    private final TextField clientId = new TextField();
    private final PasswordField secret = new PasswordField();
    private final Checkbox enabled = new Checkbox();
    private final Span enabledLabel = new Span();
    private final Span hint = new Span();
    private final Button reload = new Button();
    private final Button save = new Button();
    private ExternalProviderSettings.Settings stored;

    private ProviderCard(String provider) {
      this.provider = provider;
      String name = provider.equals("google") ? "Google" : "Microsoft";
      Image logo =
          new Image(
              "icons/external-login/" + provider + (provider.equals("google") ? ".png" : ".svg"),
              name);
      Div icon = new Div(logo);
      icon.addClassName(PANEL_ICON);
      H2 heading = new H2(name);
      heading.addClassName(PANEL_TITLE);
      subtitle.addClassName(PANEL_DESCRIPTION);
      Div headingText = new Div(heading, subtitle);
      headingText.addClassName(PANEL_HEADING);
      status.addClassName("cosmos-config-setting-count");
      status.setId("provider-" + provider + "-status");
      Div header = new Div(icon, headingText, status);
      header.addClassName(PANEL_HEADER);
      console =
          new Anchor(
              provider.equals("google")
                  ? "https://console.cloud.google.com/auth/clients"
                  : "https://entra.microsoft.com/#view/Microsoft_AAD_RegisteredApps/ApplicationsListBlade",
              "");
      console.setTarget("_blank");
      console.getElement().setAttribute("rel", "noopener noreferrer");
      redirect.setReadOnly(true);
      redirect.setAutoselect(true);
      redirect.setWidthFull();
      redirect.setId("provider-" + provider + "-redirect");
      copy.addClickListener(
          event ->
              redirect
                  .getElement()
                  .executeJs(
                      "return navigator.clipboard ? navigator.clipboard.writeText($0).then(() => true, () => false) : Promise.resolve(false)",
                      redirect.getValue())
                  .then(
                      Boolean.class,
                      copied ->
                          NotificationHelper.getInstance()
                              .showNotification(
                                  copied
                                      ? texts::getProviderSettingsCopied
                                      : texts::getProviderSettingsCopyFailed,
                                  null,
                                  copied ? NotificationType.SUCCESS : NotificationType.ERROR)));
      Div address = new Div(redirect, copy);
      address.addClassName("provider-redirect");
      clientId.setWidthFull();
      clientId.setMaxLength(512);
      clientId.setId("provider-" + provider + "-client-id");
      secret.setWidthFull();
      secret.setMaxLength(2048);
      secret.setId("provider-" + provider + "-secret");
      secret.getElement().setAttribute("autocomplete", "new-password");
      Div credentials = new Div(clientId, secret);
      credentials.addClassName("provider-credentials");
      enabled.setId("provider-" + provider + "-enabled");
      enabledLabel
          .getElement()
          .addEventListener("click", event -> enabled.setValue(!enabled.getValue()));
      Div activation = new Div(enabledLabel, enabled);
      activation.addClassName("provider-activation");
      Div fields = new Div(guide, console, address, credentials, activation);
      fields.addClassName("provider-fields");
      Div info = new Div(AppIcon.INFO_CIRCLE.create(), hint);
      info.addClassName("cosmos-config-save-hint");
      reload.setPrefixComponent(AppIcon.REFRESH.create());
      reload.addClickListener(event -> load());
      save.setPrefixComponent(AppIcon.CHECK.create());
      save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
      save.setId("provider-" + provider + "-save");
      save.addClickListener(event -> save());
      Div actions = new Div(reload, save);
      actions.addClassName("cosmos-config-card-actions");
      Div footer = new Div(info, actions);
      footer.addClassName("cosmos-panel-footer");
      Div content = new Div(header, fields, footer);
      card = new CosmosCardComponent(content);
      card.addClassNames("cosmos-admin-form-card", PANEL_CARD);
    }

    private void load() {
      stored = settings.load(provider);
      clientId.setValue(stored.clientId());
      secret.clear();
      enabled.setValue(stored.enabled());
      clientId.setInvalid(false);
      secret.setInvalid(false);
      // BASE_URL ist dieselbe vertrauenswürdige öffentliche Adresse wie für Bestätigungslinks.
      String base = config.getString(ConfigEntry.BASE_URL);
      URI origin = URI.create(base);
      redirect.setValue(origin.resolve("/login/oauth2/code/" + provider).toString());
      refreshTexts();
    }

    private void save() {
      clientId.setInvalid(enabled.getValue() && clientId.getValue().isBlank());
      secret.setInvalid(
          enabled.getValue() && secret.getValue().isBlank() && !stored.secretStored());
      try {
        stored =
            settings.save(
                provider,
                clientId.getValue(),
                secret.getValue(),
                enabled.getValue(),
                stored.revision());
        secret.clear();
        refreshTexts();
        NotificationHelper.getInstance()
            .showNotification(texts::getProviderSettingsSaved, null, NotificationType.SUCCESS);
      } catch (IllegalArgumentException failure) {
        NotificationHelper.getInstance()
            .showNotification(texts::getProviderSettingsIncomplete, null, NotificationType.ERROR);
      } catch (OptimisticLockingFailureException failure) {
        NotificationHelper.getInstance()
            .showNotification(texts::getProviderSettingsConflict, null, NotificationType.ERROR);
      } catch (RuntimeException failure) {
        logger.warn("Sign-in provider settings could not be saved", LogSafety.safeFailure(failure));
        NotificationHelper.getInstance()
            .showNotification(texts::getProviderSettingsFailed, null, NotificationType.ERROR);
      }
    }

    private void refreshTexts() {
      subtitle.setText(texts.getProviderSettingsDescription());
      guide.setText(
          provider.equals("google")
              ? texts.getProviderSettingsGoogleGuide()
              : texts.getProviderSettingsMicrosoftGuide());
      console.setText(texts.getProviderSettingsOpenConsole());
      redirect.setLabel(texts.getProviderSettingsRedirect());
      copy.setText(texts.getProviderSettingsCopy());
      clientId.setLabel(texts.getProviderSettingsClientId());
      clientId.setPlaceholder(texts.getProviderSettingsClientIdPlaceholder());
      secret.setLabel(texts.getProviderSettingsSecret());
      secret.setPlaceholder(
          stored != null && stored.secretStored()
              ? texts.getProviderSettingsSecretStored()
              : texts.getProviderSettingsSecretPlaceholder());
      secret.setHelperText(texts.getProviderSettingsSecretHint());
      enabledLabel.setText(texts.getProviderSettingsEnable());
      enabled.setAriaLabel(texts.getProviderSettingsEnable());
      hint.setText(texts.getProviderSettingsHint());
      reload.setText(texts.getAdminConfigReload());
      save.setText(texts.getProviderSettingsSave());
      status.setText(
          stored == null || stored.clientId().isBlank() || !stored.secretStored()
              ? texts.getProviderSettingsUnconfigured()
              : stored.enabled()
                  ? texts.getProviderSettingsActive()
                  : texts.getProviderSettingsInactive());
    }
  }
}
