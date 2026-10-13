package de.derpeterson.app.views.admin;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.ui.components.CosmosCardComponent;
import de.derpeterson.app.ui.components.CosmosSectionHeaderComponent;
import org.springframework.security.core.userdetails.UserDetails;

public class AdminDashboardSection extends VerticalLayout {

    private final transient MessageProperties messageProperties;
    private final transient UserDetails authenticatedUser;

    private final H1 dashboardTitle;
    private final Span dashboardDescription;
    private final H2 summaryTitle;
    private final Span summaryDescription;
    private final Span userInfoText;
    private final Span roleInfoText;

    public AdminDashboardSection(MessageProperties messageProperties, UserDetails authenticatedUser) {
        this.messageProperties = messageProperties;
        this.authenticatedUser = authenticatedUser;

        setPadding(false);
        setSpacing(false);
        setWidthFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        addClassNames("cosmos-admin-section", "cosmos-admin-section--dashboard");

        dashboardTitle = new H1();


        dashboardDescription = new Span();
        dashboardDescription.addClassNames(
                LumoUtility.TextColor.SECONDARY
        );

        summaryTitle = new H2();
        summaryTitle.addClassName("cosmos-admin-summary-title");
        summaryDescription = new Span();
        summaryDescription.addClassName("cosmos-dashboard-description");

        userInfoText = new Span();
        userInfoText.addClassNames(
                LumoUtility.FontSize.LARGE,
                LumoUtility.FontWeight.SEMIBOLD
        );

        roleInfoText = new Span();
        roleInfoText.addClassNames(
                LumoUtility.TextColor.SECONDARY
        );

        VerticalLayout infoLayout = new VerticalLayout(
                summaryTitle,
                summaryDescription,
                userInfoText,
                roleInfoText
        );

        infoLayout.setPadding(false);
        infoLayout.setSpacing(false);
        infoLayout.addClassName("cosmos-admin-account-summary");
        infoLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        CosmosCardComponent infoCard = new CosmosCardComponent(infoLayout);
        infoCard.addClassName("cosmos-admin-dashboard-card");
        add(new CosmosSectionHeaderComponent(dashboardTitle, dashboardDescription), infoCard);
    }

    public void refreshTexts() {

        dashboardTitle.setText(messageProperties.getAdminDashboardTitle());
        dashboardDescription.setText(messageProperties.getAdminDashboardDescription());
        summaryTitle.setText(messageProperties.getAdminDashboardTitle());
        summaryDescription.setText(messageProperties.getAdminDashboardDescription());

        if (authenticatedUser != null) {
            userInfoText.setText(
                    messageProperties.getAdminDashboardWelcome()
                            + ": "
                            + authenticatedUser.getUsername()
            );

            roleInfoText.setText(
                    messageProperties.getAdminDashboardRoles()
                            + ": "
                            + authenticatedUser.getAuthorities()
            );
        } else {
            userInfoText.setText(
                    messageProperties.getAdminDashboardNotAuthenticated()
            );
            roleInfoText.setText("");
        }
    }
}