package de.derpeterson.app.views;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.derpeterson.app.config.AppRouteConstants;

@Route(AppRouteConstants.BASE_ROUTE)
@PageTitle(AppRouteConstants.BASE_PAGE_TITLE)
public class HomeRedirectView extends VerticalLayout implements BeforeEnterObserver {

  @Override
  public void beforeEnter(BeforeEnterEvent navigationEvent) {
    navigationEvent.forwardTo(HomeView.class);
  }
}
