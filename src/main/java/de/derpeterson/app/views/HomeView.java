package de.derpeterson.app.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.config.AppRouteConstants;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.NavigationErrorCode;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.UserService;
import de.derpeterson.app.ui.base.TrackedUserAppLayout;
import de.derpeterson.app.ui.components.UserPopoverMenu;
import de.derpeterson.app.websocket.UserStatusBroadcaster;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

@Route(AppRouteConstants.HOME_ROUTE)
@UIScope
@PageTitle(AppRouteConstants.HOME_PAGE_TITLE)
@AnonymousAllowed
public class HomeView extends TrackedUserAppLayout implements AfterNavigationObserver {

    private final transient ConfigService configService;

    private final Span serviceNameSpan = new Span();

    @Autowired
    public HomeView(MessageProperties messageProperties,
                    SecurityService securityService,
                    HttpServletRequest request,
                    ConfigService configService,
                    UserService userService,
                    UserStatusBroadcaster userStatusBroadcaster) {
        super(messageProperties, securityService, userService, userStatusBroadcaster, request);

        this.configService = configService;

        addToNavbar(createHeader());
    }

    private HorizontalLayout createHeader() {
        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        headerLayout.addClassNames(
                LumoUtility.Padding.Vertical.SMALL,
                LumoUtility.Padding.Horizontal.MEDIUM
        );

        HorizontalLayout serviceLayout = createServiceLayout();
        HorizontalLayout userLayout = createUserLayout();

        headerLayout.add(serviceLayout, userLayout);

        return headerLayout;
    }

    private HorizontalLayout createServiceLayout() {
        HorizontalLayout serviceLayout = new HorizontalLayout();
        serviceLayout.setWidthFull();
        serviceLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        serviceLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        serviceLayout.addClassNames(LumoUtility.Gap.SMALL);

        Image serviceLogoIcon = new Image("themes/custom-theme/service_logo.png", "Service Icon");
        serviceLogoIcon.setWidth("48px");
        serviceLogoIcon.setHeight("48px");

        serviceNameSpan.addClassNames(
                LumoUtility.FontSize.XXLARGE,
                LumoUtility.FontWeight.SEMIBOLD,
                LumoUtility.FlexWrap.NOWRAP
        );

        refreshServiceName();

        serviceLayout.add(serviceLogoIcon, serviceNameSpan);
        return serviceLayout;
    }

    private void refreshServiceName() {
        serviceNameSpan.setText(configService.getString(ConfigEntry.SERVICE_NAME));
    }

    private HorizontalLayout createUserLayout() {
        HorizontalLayout userLayout = new HorizontalLayout();
        userLayout.setWidthFull();
        userLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        Button userButton = createTrackedUserButton();
        createTrackedUserPopover(createPopoverActions());

        userLayout.add(userButton);
        return userLayout;
    }

    private UserPopoverMenu.Actions createPopoverActions() {
        Runnable manageApplicationAction = null;

        if (currentUser != null && currentUser.hasRole(RoleType.ROLE_ADMIN)) {
            manageApplicationAction = () -> UI.getCurrent().navigate(AdminView.class);
        }

        return new UserPopoverMenu.Actions(
                manageApplicationAction,
                null,
                () -> UI.getCurrent().navigate(RegistrationView.class),
                () -> UI.getCurrent().navigate(LoginView.class)
        );
    }

    @Override
    protected void onTrackedUserLanguageChanged() {
        // Keine statischen, sprachabhängigen Header-Texte vorhanden.
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        Map<String, List<String>> queryParameters = event.getLocation().getQueryParameters().getParameters();
        List<String> errorParameters = queryParameters.get("error");

        if (errorParameters == null || errorParameters.isEmpty()) {
            return;
        }

        String errorCode = errorParameters.getFirst();

        NavigationErrorCode.fromCode(errorCode).ifPresent(error ->
                NotificationHelper.getInstance().showNotification(
                        messageProperties::getBaseFailedTitle,
                        () -> error.getMessage(messageProperties),
                        NotificationHelper.NotificationType.ERROR
                )
        );

        UI.getCurrent().getPage().getHistory().replaceState(null, AppRouteConstants.HOME_SECURITY_ROUTE);
    }
}