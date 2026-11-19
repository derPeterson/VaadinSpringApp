package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.IconSize;
import de.derpeterson.app.service.UserNotificationService.Target;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Vollständige, durchsuchbare Empfängerliste für die freigegebene Avatar-Vorschau. */
@JsModule("./notification-recipient-popover.js")
public class NotificationRecipientPopover extends Popover {
  private static final int MAX_VISIBLE_RECIPIENTS = 5;
  private final TextField searchTextField = new TextField();
  private final VirtualList<Target> recipientVirtualList = new VirtualList<>();
  private final ComponentRenderer<Button, Target> recipientRenderer =
      new ComponentRenderer<>(
          this::createRecipientButton,
          (component, recipientTarget) -> {
            updateRecipientButton((Button) component, recipientTarget);
            return component;
          });
  private final VerticalLayout emptyResultLayout = new VerticalLayout();
  private Long selectedRecipientId;

  public NotificationRecipientPopover(
      AvatarGroup recipientAvatarGroup,
      List<Target> recipientTargets,
      MessageProperties messageProperties) {
    List<Target> reviewedRecipientTargets = List.copyOf(recipientTargets);
    setId("admin-notifications-preview-recipients");
    addClassNames("cosmos-recipient-popover", "cosmos-theme", CosmosCssClasses.REFERENCE_OVERLAY);
    addThemeName("cosmos");
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
    searchTextField.setPrefixComponent(AppIcon.SEARCH.create());

    configureRecipientList(reviewedRecipientTargets, messageProperties);

    Button closeButton = new Button(AppIcon.CLOSE_CIRCLE.create(), _ -> close());
    closeButton.addThemeVariants(ButtonVariant.TERTIARY);
    closeButton.addClassNames("cosmos-overlay-close", CosmosCssClasses.INLINE_BUTTON);
    closeButton.setAriaLabel(messageProperties.getNotificationsClose());
    Span headingSpan = new Span(messageProperties.getNotificationsRecipients());
    headingSpan.addClassName(CosmosCssClasses.PANEL_TITLE);
    Span descriptionSpan = new Span(messageProperties.getNotificationsRecipientDescription());
    descriptionSpan.addClassName(CosmosCssClasses.PANEL_DESCRIPTION);
    VerticalLayout headingCopyLayout = new VerticalLayout(headingSpan, descriptionSpan);
    headingCopyLayout.setPadding(false);
    headingCopyLayout.setSpacing(false);
    headingCopyLayout.addClassName(CosmosCssClasses.PANEL_HEADING);
    Span headingIconSpan = new Span(AppIcon.USERS.create());
    headingIconSpan.addClassName(CosmosCssClasses.PANEL_ICON);
    headingIconSpan.getElement().setAttribute("aria-hidden", "true");
    HorizontalLayout headingLayout =
        new HorizontalLayout(headingIconSpan, headingCopyLayout, closeButton);
    headingLayout.setWidthFull();
    headingLayout.setAlignItems(HorizontalLayout.Alignment.CENTER);
    headingLayout.expand(headingCopyLayout);
    headingLayout.addClassName("cosmos-recipient-heading");

    VerticalLayout contentLayout =
        new VerticalLayout(searchTextField, recipientVirtualList, emptyResultLayout);
    contentLayout.setPadding(false);
    contentLayout.setSpacing(false);
    contentLayout.addClassName("cosmos-recipient-content");
    VerticalLayout popoverLayout = new VerticalLayout(headingLayout, contentLayout);
    popoverLayout.setPadding(false);
    popoverLayout.setSpacing(false);
    popoverLayout.setWidth(null);
    popoverLayout.addClassName("cosmos-recipient-layout");
    add(popoverLayout);
    configureAvatarTrigger(recipientAvatarGroup, messageProperties);
  }

  private void configureRecipientList(
      List<Target> reviewedRecipientTargets, MessageProperties messageProperties) {
    recipientVirtualList.setId("admin-notifications-recipient-list");
    recipientVirtualList.addClassName("cosmos-recipient-list");
    recipientVirtualList
        .getElement()
        .setAttribute("aria-label", messageProperties.getNotificationsRecipients());
    recipientVirtualList.setItemAccessibleNameGenerator(
        recipientTarget -> recipientTarget.name() + ", " + recipientTarget.email());
    recipientVirtualList.setRenderer(recipientRenderer);
    Span emptyTitleSpan = new Span(messageProperties.getNotificationsRecipientSearchEmpty());
    emptyTitleSpan.addClassName("cosmos-notification-empty-title");
    Span emptyHintSpan = new Span(messageProperties.getNotificationsRecipientSearchHint());
    emptyHintSpan.addClassName(CosmosCssClasses.PANEL_DESCRIPTION);
    emptyResultLayout.add(AppIcon.USER_QUESTION.create(), emptyTitleSpan, emptyHintSpan);
    emptyResultLayout.setPadding(false);
    emptyResultLayout.setSpacing(false);
    emptyResultLayout.setAlignItems(VerticalLayout.Alignment.CENTER);
    emptyResultLayout.addClassName("cosmos-recipient-empty");
    emptyResultLayout.getElement().setAttribute("role", "status");

    ListDataProvider<Target> recipientDataProvider =
        new ListDataProvider<>(reviewedRecipientTargets);
    recipientVirtualList.setDataProvider(recipientDataProvider);
    refreshRecipientResults(reviewedRecipientTargets.size());
    searchTextField.addValueChangeListener(valueChangeEvent -> {
      String searchText = valueChangeEvent.getValue().strip().toLowerCase(Locale.ROOT);
      recipientDataProvider.setFilter(recipientTarget ->
          recipientTarget.name().toLowerCase(Locale.ROOT).contains(searchText)
              || recipientTarget.email().toLowerCase(Locale.ROOT).contains(searchText));
      refreshRecipientResults(recipientDataProvider.size(new Query<>()));
    });
  }

  private void refreshRecipientResults(int recipientCount) {
    recipientVirtualList.setVisible(recipientCount > 0);
    emptyResultLayout.setVisible(recipientCount == 0);
    // Ab fünf Zeilen bleibt die Höhe fest; weitere Empfänger werden virtualisiert.
    int visibleRecipientRows = Math.max(1, Math.min(recipientCount, MAX_VISIBLE_RECIPIENTS));
    recipientVirtualList.setHeight(
        "calc("
            + visibleRecipientRows
            + " * var(--cosmos-recipient-row-height) + 2 * var(--cosmos-border-width))");
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

  private Button createRecipientButton(Target recipientTarget) {
    Button recipientButton = new Button();
    recipientButton.addThemeVariants(ButtonVariant.TERTIARY);
    recipientButton.addClassNames(CosmosCssClasses.INLINE_BUTTON, "cosmos-recipient-button");
    recipientButton.setWidthFull();
    recipientButton.addClickListener(event -> {
      selectedRecipientId =
          event.isCtrlKey() && Objects.equals(selectedRecipientId, recipientTarget.id())
              ? null
              : recipientTarget.id();
      recipientVirtualList.getDataProvider().refreshAll();
    });
    updateRecipientButton(recipientButton, recipientTarget);
    return recipientButton;
  }

  private void updateRecipientButton(Button recipientButton, Target recipientTarget) {
    recipientButton.setPrefixComponent(createRecipientRow(recipientTarget));
    recipientButton.setAriaLabel(recipientTarget.name() + ", " + recipientTarget.email());
    recipientButton
        .getElement()
        .setAttribute(
            "aria-pressed",
            Boolean.toString(Objects.equals(selectedRecipientId, recipientTarget.id())));
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
