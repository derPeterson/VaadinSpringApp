package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

public final class NotificationEmptyState extends Div {
  public NotificationEmptyState(String emptyInboxMessage) {
    Span emptyInboxLabel = new Span(emptyInboxMessage);
    emptyInboxLabel.addClassName("cosmos-notification-empty-title");
    emptyInboxLabel.getElement().setAttribute("role", "heading");
    emptyInboxLabel.getElement().setAttribute("aria-level", "3");
    add(AppIcon.BELL.create(), emptyInboxLabel);
    addClassName("cosmos-notification-empty");
  }
}
