package de.derpeterson.app.views.admin;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.ui.components.CardComponent;
import org.springframework.security.core.userdetails.UserDetails;

public class AdminDashboardSection extends VerticalLayout {

    private final transient MessageProperties messageProperties;
    private final transient UserDetails authenticatedUser;

    private final H1 dashboardTitle;
    private final Span dashboardDescription;
    private final Span userInfoText;
    private final Span roleInfoText;

    public AdminDashboardSection(MessageProperties messageProperties, UserDetails authenticatedUser) {
        this.messageProperties = messageProperties;
        this.authenticatedUser = authenticatedUser;

        setPadding(false);
        setSpacing(true);
        setWidthFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        addClassNames(LumoUtility.Gap.MEDIUM);

        dashboardTitle = new H1();
        dashboardTitle.addClassNames(
                LumoUtility.Margin.NONE,
                LumoUtility.FontSize.XXLARGE
        );

        dashboardDescription = new Span();
        dashboardDescription.addClassNames(
                LumoUtility.TextColor.SECONDARY
        );

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
                dashboardTitle,
                dashboardDescription,
                userInfoText,
                roleInfoText
        );

        infoLayout.setPadding(false);
        infoLayout.setSpacing(true);
        infoLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        CardComponent infoCard = new CardComponent(infoLayout);
        infoCard.setWidthFull();
        infoCard.setMaxWidth("760px");
        infoCard.addClassNames(LumoUtility.Background.SHADE_10);

        add(infoCard);
    }

    public void refreshTexts() {

        dashboardTitle.setText(messageProperties.getAdminDashboardTitle());
        dashboardDescription.setText(messageProperties.getAdminDashboardDescription());

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