package de.derpeterson.app.views.admin;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.ui.components.CosmosCardComponent;
import de.derpeterson.app.ui.components.CosmosSectionHeaderComponent;
import org.springframework.security.core.userdetails.UserDetails;

public class AdminDashboardSection extends VerticalLayout {

    private final transient MessageProperties messageProperties;
    private final transient UserDetails authenticatedUser;

    private final Span dashboardOverview;
    private final Span dashboardTitle;
    private final Span dashboardDescription;
    private final Span summaryEyebrow;
    private final H2 summaryTitle;
    private final Span summaryDescription;
    private final Span welcomeLabel;
    private final Span userEmail;
    private final Span roleInfoText;
    private final Div roleBadges;
    private final Div roleRow;

    public AdminDashboardSection(MessageProperties messageProperties, UserDetails authenticatedUser) {
        this.messageProperties = messageProperties;
        this.authenticatedUser = authenticatedUser;

        setPadding(false);
        setSpacing(false);
        setWidthFull();
        addClassNames("cosmos-admin-section", "cosmos-admin-section--dashboard");

        dashboardOverview = new Span();
        dashboardTitle = new Span();
        dashboardDescription = new Span();

        summaryEyebrow = new Span();
        summaryEyebrow.addClassName("cosmos-admin-summary-eyebrow");
        summaryTitle = new H2();
        summaryTitle.addClassName("cosmos-admin-summary-title");
        summaryDescription = new Span();
        summaryDescription.addClassName("cosmos-admin-summary-description");

        welcomeLabel = new Span();
        userEmail = new Span();
        userEmail.addClassName("cosmos-admin-summary-email");
        Span userInfoText = new Span(welcomeLabel, userEmail);
        userInfoText.addClassName("cosmos-admin-summary-user");

        roleInfoText = new Span();
        roleBadges = new Div();
        roleBadges.addClassName("cosmos-admin-summary-roles");
        roleRow = new Div(roleInfoText, roleBadges);
        roleRow.addClassName("cosmos-admin-summary-role-row");

        VerticalLayout infoLayout = new VerticalLayout(
                summaryEyebrow,
                summaryTitle,
                summaryDescription,
                userInfoText,
                roleRow
        );
        infoLayout.setPadding(false);
        infoLayout.setSpacing(false);
        infoLayout.addClassName("cosmos-admin-account-summary");
        infoLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        CosmosCardComponent infoCard = new CosmosCardComponent(infoLayout);
        infoCard.addClassName("cosmos-admin-dashboard-card");
        add(new CosmosSectionHeaderComponent(dashboardOverview, dashboardTitle, dashboardDescription), infoCard);
    }

    public void refreshTexts() {
        dashboardOverview.setText(messageProperties.getAdminHeaderTitle() + " / " + messageProperties.getHomeOverview());
        dashboardTitle.setText(messageProperties.getAdminDashboardTitle());
        dashboardDescription.setText(messageProperties.getAdminDashboardDescription());
        summaryEyebrow.setText(messageProperties.getAdminDashboardWelcomeBack());
        summaryTitle.setText(messageProperties.getAdminDashboardTitle());
        summaryDescription.setText(messageProperties.getAdminDashboardDescription());

        roleBadges.removeAll();
        roleRow.setVisible(authenticatedUser != null);
        if (authenticatedUser != null) {
            welcomeLabel.setText(messageProperties.getAdminDashboardWelcome() + ": ");
            userEmail.setText(authenticatedUser.getUsername());
            roleInfoText.setText(messageProperties.getAdminDashboardRoles() + ":");
            authenticatedUser.getAuthorities().stream()
                    .filter(authority -> !(authority instanceof org.springframework.security.core.authority.FactorGrantedAuthority))
                    .forEach(authority -> {
                Span badge = new Span(authority.getAuthority());
                badge.addClassName("cosmos-admin-role-chip");
                roleBadges.add(badge);
            });
        } else {
            welcomeLabel.setText(messageProperties.getAdminDashboardNotAuthenticated());
            userEmail.setText("");
            roleInfoText.setText("");
        }
    }
}
