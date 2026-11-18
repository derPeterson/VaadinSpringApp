package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.IconSize;
import de.derpeterson.app.service.UserNotificationService.Target;
import java.util.List;
import java.util.Locale;

/** Vollständige, durchsuchbare Empfängerliste für die freigegebene Avatar-Vorschau. */
@JsModule("./notification-recipient-popover.js")
public class NotificationRecipientPopover extends Popover {
  private final TextField searchTextField = new TextField();
  private final Grid<Target> recipientGrid = new Grid<>();

  public NotificationRecipientPopover(
      AvatarGroup recipientAvatarGroup,
      List<Target> recipientTargets,
      MessageProperties messageProperties) {
    List<Target> reviewedRecipientTargets = List.copyOf(recipientTargets);
    setId("admin-notifications-preview-recipients");
    addClassName("cosmos-recipient-popover");
    setTarget(recipientAvatarGroup);
    setOpenOnClick(false);
    setOpenOnFocus(false);
    setOpenOnHover(false);
    setAutofocus(true);
    setModal(true, false);
    setAriaRole("dialog");
    setAriaLabel(messageProperties.getNotificationsRecipients());

    searchTextField.setId("admin-notifications-recipient-search");
    searchTextField.setPlaceholder(messageProperties.getNotificationsRecipientSearch());
    searchTextField.setAriaLabel(messageProperties.getNotificationsRecipientSearch());
    searchTextField.setClearButtonVisible(true);
    searchTextField.setValueChangeMode(ValueChangeMode.EAGER);
    searchTextField.setWidthFull();
    searchTextField.setAutofocus(true);

    configureRecipientGrid(reviewedRecipientTargets, messageProperties);

    Button closeButton = new Button(AppIcon.CLOSE.create(), _ -> close());
    closeButton.addThemeVariants(ButtonVariant.TERTIARY);
    closeButton.setAriaLabel(messageProperties.getNotificationsClose());
    Span headingSpan = new Span(messageProperties.getNotificationsRecipients());
    HorizontalLayout headingLayout = new HorizontalLayout(headingSpan, closeButton);
    headingLayout.setWidthFull();
    headingLayout.setAlignItems(HorizontalLayout.Alignment.CENTER);
    headingLayout.expand(headingSpan);
    headingLayout.addClassName("cosmos-recipient-heading");

    VerticalLayout contentLayout =
        new VerticalLayout(headingLayout, searchTextField, recipientGrid);
    contentLayout.setPadding(false);
    contentLayout.setSpacing(false);
    contentLayout.addClassName("cosmos-recipient-content");
    add(contentLayout);
    configureAvatarTrigger(recipientAvatarGroup, messageProperties);
  }

  private void configureRecipientGrid(
      List<Target> reviewedRecipientTargets, MessageProperties messageProperties) {
    recipientGrid.setId("admin-notifications-recipient-list");
    recipientGrid.addClassName("cosmos-recipient-grid");
    recipientGrid.setSelectionMode(Grid.SelectionMode.NONE);
    recipientGrid.setAriaLabel(messageProperties.getNotificationsRecipients());
    recipientGrid.setEmptyStateText(messageProperties.getNotificationsRecipientSearchEmpty());
    recipientGrid.addComponentColumn(this::createRecipientRow).setFlexGrow(1);
    recipientGrid.setItems(reviewedRecipientTargets);
    searchTextField.addValueChangeListener(valueChangeEvent -> {
      String searchText = valueChangeEvent.getValue().strip().toLowerCase(Locale.ROOT);
      recipientGrid.setItems(
          reviewedRecipientTargets.stream()
              .filter(recipientTarget ->
                  recipientTarget.name().toLowerCase(Locale.ROOT).contains(searchText)
                      || recipientTarget.email().toLowerCase(Locale.ROOT).contains(searchText))
              .toList());
    });
  }

  private void configureAvatarTrigger(
      AvatarGroup recipientAvatarGroup, MessageProperties messageProperties) {
    recipientAvatarGroup.getElement().setAttribute("role", "button");
    recipientAvatarGroup.getElement().setAttribute("tabindex", "0");
    recipientAvatarGroup.getElement().setAttribute("aria-haspopup", "dialog");
    recipientAvatarGroup.getElement().setAttribute("aria-expanded", "false");
    recipientAvatarGroup
        .getElement()
        .setAttribute("aria-label", messageProperties.getNotificationsRecipientOpen());
    recipientAvatarGroup.getElement().setAttribute("aria-controls", getId().orElseThrow());
    recipientAvatarGroup.getElement().addEventListener("recipient-list-open", _ -> open());
    addOpenedChangeListener(openedChangeEvent ->
        recipientAvatarGroup
            .getElement()
            .setAttribute("aria-expanded", Boolean.toString(openedChangeEvent.isOpened())));
    // Der gekapselte Listener fängt auch den nativen Überlauf ab, ohne dessen private API.
    addAttachListener(_ ->
        recipientAvatarGroup.getElement().executeJs("window.CosmosRecipientPopover.connect(this)"));
    addDetachListener(_ ->
        recipientAvatarGroup
            .getElement()
            .executeJs("window.CosmosRecipientPopover.disconnect(this)"));
  }

  private HorizontalLayout createRecipientRow(Target recipientTarget) {
    CosmosAvatar recipientAvatar = new CosmosAvatar(IconSize.PIXEL_32);
    recipientAvatar.setName(recipientTarget.name());
    Span nameSpan = new Span(recipientTarget.name());
    nameSpan.addClassName("cosmos-recipient-name");
    Span emailSpan = new Span(recipientTarget.email());
    emailSpan.addClassName("cosmos-recipient-email");
    VerticalLayout identityLayout = new VerticalLayout(nameSpan, emailSpan);
    identityLayout.setPadding(false);
    identityLayout.setSpacing(false);
    identityLayout.addClassName("cosmos-recipient-identity");
    HorizontalLayout recipientRowLayout = new HorizontalLayout(recipientAvatar, identityLayout);
    recipientRowLayout.setAlignItems(HorizontalLayout.Alignment.CENTER);
    recipientRowLayout.addClassName("cosmos-recipient-row");
    return recipientRowLayout;
  }
}
