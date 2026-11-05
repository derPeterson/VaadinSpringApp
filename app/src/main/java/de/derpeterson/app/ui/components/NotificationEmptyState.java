package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

public final class NotificationEmptyState extends Div {
  public NotificationEmptyState(String message) {
    Span title = new Span(message);
    title.addClassName("cosmos-notification-empty-title");
    title.getElement().setAttribute("role", "heading");
    title.getElement().setAttribute("aria-level", "3");
    add(AppIcon.BELL.create(), title);
    addClassName("cosmos-notification-empty");
  }
}
