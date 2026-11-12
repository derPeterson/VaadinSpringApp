package de.derpeterson.app.views.admin;

import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_ICON;

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
import de.derpeterson.app.ui.components.CosmosBreadcrumbsComponent.ParentPath;
import de.derpeterson.app.ui.components.CosmosSectionHeaderComponent;
import de.derpeterson.app.views.AdminView;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.userdetails.UserDetails;

/** Abgestimmter Einstieg mit echten Schnellzugriffen, ohne erfundene Kennzahlen. */
public class AdminDashboardSection extends VerticalLayout {
  private final MessageProperties messageProperties;
  private final List<SerializableRunnable> textRefreshActions = new ArrayList<>();

  public AdminDashboardSection(MessageProperties messageProperties, UserDetails authenticatedUser) {
    this.messageProperties = messageProperties;

    setPadding(false);
    setSpacing(false);
    setWidthFull();
    addClassNames("cosmos-admin-section", "cosmos-admin-section--dashboard");

    Span dashboardTitleSpan = createTranslatedSpan(messageProperties::getAdminDashboardTitle);
    Span dashboardDescriptionSpan =
        createTranslatedSpan(messageProperties::getAdminDashboardDescription);

    Div dashboardHeroDiv = buildDashboardHeroDiv();

    H2 shortcutsH2 = new H2();
    textRefreshActions.add(
        () -> shortcutsH2.setText(messageProperties.getAdminDashboardShortcuts()));
    Div shortcutGridDiv = new Div();
    shortcutGridDiv.addClassName("admin-dashboard-shortcuts");
    shortcutGridDiv.add(
        buildShortcutCardDiv(
            AppIcon.USERS,
            messageProperties::getAdminUsersTitle,
            messageProperties::getAdminUsersDescription,
            "user_management"),
        buildShortcutCardDiv(
            AppIcon.COGS,
            messageProperties::getAdminNavConfiguration,
            messageProperties::getAdminConfigDescription,
            "configuration"),
        buildShortcutCardDiv(
            AppIcon.ENVELOPE,
            messageProperties::getAdminMailTitle,
            messageProperties::getAdminMailDescription,
            "mail_test"),
        buildShortcutCardDiv(
            AppIcon.MESSAGES,
            messageProperties::getSupportTitle,
            messageProperties::getSupportShortcutDescription,
            "support"));

    boolean canSendNotifications =
        authenticatedUser != null
            && authenticatedUser.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_SUPERADMIN"));
    if (canSendNotifications) {
      shortcutGridDiv.add(
          buildShortcutCardDiv(
              AppIcon.BELL,
              messageProperties::getNotificationsTitle,
              messageProperties::getNotificationsAdminPageDescription,
              "notifications"));
    }

    Div shortcutHeadingDiv =
        new Div(
            shortcutsH2, createTranslatedSpan(messageProperties::getAdminDashboardShortcutsHint));
    shortcutHeadingDiv.addClassName("admin-dashboard-access-heading");
    add(
        new CosmosSectionHeaderComponent(
            messageProperties,
            ParentPath.ADMINISTRATION,
            dashboardTitleSpan,
            dashboardDescriptionSpan),
        dashboardHeroDiv,
        shortcutHeadingDiv,
        shortcutGridDiv);
    refreshTexts();
  }

  private Div buildDashboardHeroDiv() {
    Div heroEyebrowDiv =
        new Div(createTranslatedSpan(messageProperties::getAdminDashboardHeroEyebrow));
    heroEyebrowDiv.addClassName("admin-dashboard-eyebrow");
    H2 welcomeH2 = new H2();
    textRefreshActions.add(
        () -> welcomeH2.setText(messageProperties.getAdminDashboardHeroTitle() + "."));
    Div heroTextDiv =
        new Div(
            heroEyebrowDiv,
            welcomeH2,
            createTranslatedSpan(messageProperties::getAdminDashboardHeroDescription));
    heroTextDiv.addClassName("admin-dashboard-copy");

    Image dashboardImage = new Image("images/admin-dashboard.jpg", "");
    dashboardImage.addClassName("admin-dashboard-image");
    Div heroIntroDiv = new Div(heroTextDiv, dashboardImage);
    heroIntroDiv.addClassName("admin-dashboard-intro");

    Div heroHintDiv =
        new Div(
            AppIcon.INFO_CIRCLE.create(),
            createTranslatedSpan(messageProperties::getAdminDashboardFutureHint));
    heroHintDiv.addClassName("admin-dashboard-hint");

    Div dashboardHeroDiv = new Div(heroIntroDiv, heroHintDiv);
    dashboardHeroDiv.addClassName("admin-dashboard-hero");
    return dashboardHeroDiv;
  }

  private Span createTranslatedSpan(SerializableSupplier<String> textSupplier) {
    Span translatedSpan = new Span();
    textRefreshActions.add(() -> translatedSpan.setText(textSupplier.get()));
    return translatedSpan;
  }

  private Div buildShortcutCardDiv(
      AppIcon shortcutAppIcon,
      SerializableSupplier<String> shortcutNameSupplier,
      SerializableSupplier<String> descriptionTextSupplier,
      String targetSectionKey) {
    Div shortcutIconDiv = new Div(shortcutAppIcon.create());
    shortcutIconDiv.addClassName(PANEL_ICON);

    H3 shortcutTitleH3 = new H3();
    textRefreshActions.add(() -> shortcutTitleH3.setText(shortcutNameSupplier.get()));
    Div shortcutTextDiv = new Div(shortcutTitleH3, createTranslatedSpan(descriptionTextSupplier));

    Button openSectionButton =
        new Button(
            "",
            AppIcon.OPEN_SECTION.create(),
            _ ->
                UI.getCurrent()
                    .navigate(
                        AdminView.class,
                        QueryParameters.simple(Map.of("section", targetSectionKey))));
    textRefreshActions.add(() -> {
      openSectionButton.setText(messageProperties.getAdminDashboardOpen());
      openSectionButton.setAriaLabel(
          shortcutNameSupplier.get() + ": " + messageProperties.getAdminDashboardOpen());
    });

    Div shortcutCardDiv = new Div(shortcutIconDiv, shortcutTextDiv, openSectionButton);
    shortcutCardDiv.addClassName("admin-dashboard-shortcut");
    return shortcutCardDiv;
  }

  public void refreshTexts() {
    textRefreshActions.forEach(SerializableRunnable::run);
  }
}
