package de.derpeterson.app.views.admin;

import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_ICON;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.router.QueryParameters;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.ui.components.AppIcon;
import de.derpeterson.app.ui.components.CosmosSectionHeaderComponent;
import de.derpeterson.app.views.AdminView;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.userdetails.UserDetails;

/** Abgestimmter Einstieg mit echten Schnellzugriffen, ohne erfundene Kennzahlen. */
public class AdminDashboardSection extends VerticalLayout {
  private final MessageProperties messageProperties;
  private final List<SerializableRunnable> translations = new ArrayList<>();

  public AdminDashboardSection(MessageProperties messageProperties, UserDetails authenticatedUser) {
    this.messageProperties = messageProperties;
    setPadding(false);
    setSpacing(false);
    setWidthFull();
    addClassNames("cosmos-admin-section", "cosmos-admin-section--dashboard");
    Span overview = copy(messageProperties::getAdminHeaderTitle);
    Span title = copy(messageProperties::getAdminDashboardTitle);
    Span description = copy(messageProperties::getAdminDashboardDescription);
    Div eyebrow = new Div(copy(messageProperties::getAdminDashboardHeroEyebrow));
    eyebrow.addClassName("admin-dashboard-eyebrow");
    H2 welcome = new H2();
    translations.add(() -> welcome.setText(messageProperties.getAdminDashboardHeroTitle() + "."));
    Div copy = new Div(eyebrow, welcome, copy(messageProperties::getAdminDashboardHeroDescription));
    copy.addClassName("admin-dashboard-copy");
    Image picture = new Image("images/admin-dashboard.jpg", "");
    picture.addClassName("admin-dashboard-image");
    Div intro = new Div(copy, picture);
    intro.addClassName("admin-dashboard-intro");
    Div hint =
        new Div(AppIcon.INFO_CIRCLE.create(), copy(messageProperties::getAdminDashboardFutureHint));
    hint.addClassName("admin-dashboard-hint");
    Div hero = new Div(intro, hint);
    hero.addClassName("admin-dashboard-hero");
    H2 shortcuts = new H2();
    translations.add(() -> shortcuts.setText(messageProperties.getAdminDashboardShortcuts()));
    Div grid = new Div();
    grid.addClassName("admin-dashboard-shortcuts");
    grid.add(
        shortcut(
            AppIcon.USERS,
            messageProperties::getAdminUsersTitle,
            messageProperties::getAdminUsersDescription,
            "user_management"),
        shortcut(
            AppIcon.COGS,
            messageProperties::getAdminNavConfiguration,
            messageProperties::getAdminConfigDescription,
            "configuration"),
        shortcut(
            AppIcon.ENVELOPE,
            messageProperties::getAdminMailTitle,
            messageProperties::getAdminMailDescription,
            "mail_test"),
        shortcut(
            AppIcon.MESSAGES,
            messageProperties::getSupportTitle,
            messageProperties::getSupportShortcutDescription,
            "support"));
    boolean canSend =
        authenticatedUser != null
            && authenticatedUser.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_SUPERADMIN"));
    if (canSend) {
      grid.add(
          shortcut(
              AppIcon.BELL,
              messageProperties::getNotificationsTitle,
              messageProperties::getNotificationsAdminPageDescription,
              "notifications"));
    }
    Div accessHeading = new Div(shortcuts, copy(messageProperties::getAdminDashboardShortcutsHint));
    accessHeading.addClassName("admin-dashboard-access-heading");
    add(new CosmosSectionHeaderComponent(overview, title, description), hero, accessHeading, grid);
    refreshTexts();
  }

  private Span copy(SerializableSupplier<String> source) {
    Span span = new Span();
    translations.add(() -> span.setText(source.get()));
    return span;
  }

  private Div shortcut(
      AppIcon icon,
      SerializableSupplier<String> name,
      SerializableSupplier<String> description,
      String section) {
    Div symbol = new Div(icon.create());
    symbol.addClassName(PANEL_ICON);
    H3 title = new H3();
    translations.add(() -> title.setText(name.get()));
    Div copy = new Div(title, copy(description));
    Button open =
        new Button(
            "",
            AppIcon.OPEN_SECTION.create(),
            event ->
                UI.getCurrent()
                    .navigate(AdminView.class, QueryParameters.simple(Map.of("section", section))));
    translations.add(() -> {
      open.setText(messageProperties.getAdminDashboardOpen());
      open.setAriaLabel(name.get() + ": " + messageProperties.getAdminDashboardOpen());
    });
    Div card = new Div(symbol, copy, open);
    card.addClassName("admin-dashboard-shortcut");
    return card;
  }

  public void refreshTexts() {
    translations.forEach(SerializableRunnable::run);
  }
}
