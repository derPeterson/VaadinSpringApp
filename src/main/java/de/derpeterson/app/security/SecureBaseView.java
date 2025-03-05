package de.derpeterson.app.security;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import de.derpeterson.app.views.LoginView;
import jakarta.servlet.http.HttpServletRequest;

public abstract class SecureBaseView<T extends Component & FlexComponent> extends Composite<T> implements BeforeEnterObserver {

    private final SecurityService securityService;

    private final HttpServletRequest request;

    protected SecureBaseView(SecurityService securityService, HttpServletRequest request, T layout) {
        this.securityService = securityService;
        this.request = request;
        getContent().add(layout);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (securityService.getAuthenticatedUser(this.request).isEmpty()) {
            event.forwardTo(LoginView.class);
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
