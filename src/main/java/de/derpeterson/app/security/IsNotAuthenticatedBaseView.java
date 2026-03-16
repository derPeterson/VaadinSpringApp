package de.derpeterson.app.security;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import de.derpeterson.app.views.AdminView;
import de.derpeterson.app.views.HomeView;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.userdetails.UserDetails;

public abstract class IsNotAuthenticatedBaseView<T extends Component & FlexComponent> extends Composite<T> implements BeforeEnterObserver {

    private final transient SecurityService securityService;

    private final transient HttpServletRequest request;

    protected IsNotAuthenticatedBaseView(SecurityService securityService, HttpServletRequest request, T layout) {
        this.securityService = securityService;
        this.request = request;
        getContent().add(layout);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        securityService.getAuthenticatedUser(this.request).ifPresent(user -> {
            if (hasAdminRole(user)) {
                event.forwardTo(AdminView.class);
            } else {
                event.forwardTo(HomeView.class);
            }
        });
    }

    private boolean hasAdminRole(UserDetails user) {
        return user.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    public void setSpacing(boolean spacing) {
        if (getContent() instanceof VerticalLayout verticalLayout) {
            verticalLayout.setSpacing(spacing);
        } else if (getContent() instanceof HorizontalLayout horizontalLayout) {
            horizontalLayout.setSpacing(spacing);
        } else {
            throw new UnsupportedOperationException("setSpacing() is not supported by " + getContent().getClass().getSimpleName() + ".");
        }
    }

    public void setSizeFull() {
        getContent().setSizeFull();
    }

    public void setAlignItems(FlexComponent.Alignment alignment) {
        getContent().setAlignItems(alignment);
    }

    public void setJustifyContentMode(FlexComponent.JustifyContentMode mode) {
        getContent().setJustifyContentMode(mode);
    }

    public void add(Component... components) {
        getContent().add(components);
    }
}
