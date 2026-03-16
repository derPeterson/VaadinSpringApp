package de.derpeterson.app.ui.base;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.*;
import de.derpeterson.app.model.enums.NavigationErrorCode;
import de.derpeterson.app.views.HomeView;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

public class RouteNotFoundHandler extends Div implements HasErrorParameter<NotFoundException> {

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<NotFoundException> parameter) {
        event.forwardTo(HomeView.class, QueryParameters.simple(Map.of("error", NavigationErrorCode.PATH_NOT_FOUND.getCode())));
        return HttpServletResponse.SC_NOT_FOUND;
    }
}
