package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ModalityMode;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.function.SerializableRunnable;
import de.derpeterson.app.i18n.MessageProperties;
import java.util.Objects;
import java.util.function.Supplier;

/** Gemeinsame Bestätigung; nur die aufrufende Fachkomponente führt die Aktion aus. */
public class PortalConfirmationDialog extends Dialog {
  public record Prompt(
      Supplier<String> title,
      Supplier<String> description,
      Supplier<String> question,
      Supplier<String> explanation,
      Supplier<Component> context,
      Supplier<String> hint,
      Supplier<String> actionLabel,
      boolean destructive,
      Supplier<Component> actionIcon) {
    public Prompt(
        Supplier<String> title,
        Supplier<String> description,
        Supplier<String> question,
        Supplier<String> explanation,
        Supplier<Component> context,
        Supplier<String> hint,
        Supplier<String> actionLabel,
        boolean destructive) {
      this(
          title,
          description,
          question,
          explanation,
          context,
          hint,
          actionLabel,
          destructive,
          () -> (destructive ? AppIcon.TRASH : AppIcon.CHECK).create());
    }
  }

  private final MessageProperties texts;
  private final H2 title = new H2();
  private final Span description = new Span();
  private final Span question = new Span();
  private final Span explanation = new Span();
  private final Span hint = new Span();
  private final Div context = new Div();
  private final Div symbol = new Div();
  private final Div note = new Div();
  private final Button cancel = new Button();
  private final Button confirm = new Button();
  private final Button close = new Button(AppIcon.CLOSE_CIRCLE.create());
  private transient Prompt prompt;
  private SerializableRunnable pending;

  public PortalConfirmationDialog(MessageProperties texts, String id) {
    this.texts = texts;
    setId(id);
    addClassNames("cosmos-theme", "cosmos-reference-overlay", "portal-confirmation-dialog");
    getThemeNames().addAll(java.util.List.of("cosmos", "cosmos-reference"));
    setModality(ModalityMode.STRICT);
    setCloseOnEsc(true);
    setCloseOnOutsideClick(true);
    title.addClassName("cosmos-panel-title");
    description.addClassName("cosmos-panel-description");
    symbol.addClassName("cosmos-panel-icon");
    Div titles = new Div(title, description);
    titles.addClassName("cosmos-panel-heading");
    close.addClassName("cosmos-overlay-close");
    close.addClickListener(event -> close());
    Div header = new Div(symbol, titles, close);
    header.addClassName("account-change-dialog-header");
    getHeader().add(header);
    question.addClassName("portal-confirm-question");
    explanation.addClassName("portal-confirm-explanation");
    Div lead = new Div(question, explanation);
    lead.addClassName("portal-confirm-lead");
    context.addClassName("portal-confirm-context");
    note.add(AppIcon.INFO_CIRCLE.create(), hint);
    note.addClassName("cosmos-panel-hint");
    Div body = new Div(lead, context, note);
    body.addClassName("portal-confirm-body");
    add(body);
    cancel.setId(id + "-cancel");
    cancel.setPrefixComponent(AppIcon.CLOSE.create());
    cancel.addClickListener(event -> close());
    confirm.setId(id + "-submit");
    confirm.addClickListener(event -> confirmPending());
    Div actions = new Div(cancel, confirm);
    actions.addClassName("cosmos-dialog-actions");
    getFooter().add(actions);
    addOpenedChangeListener(
        event -> {
          if (!event.isOpened()) {
            pending = null;
            prompt = null;
            context.removeAll();
          }
        });
  }

  public void open(Prompt prompt, SerializableRunnable action) {
    this.prompt = Objects.requireNonNull(prompt);
    pending = Objects.requireNonNull(action);
    setClassName("portal-confirm-destructive", prompt.destructive());
    confirm.setClassName("cosmos-delete-button", prompt.destructive());
    confirm.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
    if (!prompt.destructive()) {
      confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    }
    symbol.removeAll();
    symbol.add(prompt.actionIcon().get());
    confirm.setPrefixComponent(prompt.actionIcon().get());
    confirm.setEnabled(true);
    refreshTexts();
    super.open();
  }

  private void confirmPending() {
    SerializableRunnable action = pending;
    if (action == null) {
      return;
    }
    pending = null;
    confirm.setEnabled(false);
    try {
      action.run();
    } finally {
      close();
      confirm.setEnabled(true);
    }
  }

  public void refreshTexts() {
    cancel.setText(texts.getAdminUsersCancel());
    close.setAriaLabel(texts.getAdminUsersCloseDialog());
    if (prompt == null) {
      return;
    }
    title.setText(prompt.title().get());
    getElement().setProperty("ariaLabel", title.getText());
    description.setText(prompt.description().get());
    question.setText(prompt.question().get());
    explanation.setText(prompt.explanation().get());
    hint.setText(prompt.hint().get());
    note.setVisible(!hint.getText().isBlank());
    confirm.setText(prompt.actionLabel().get());
    context.removeAll();
    context.add(prompt.context().get());
  }
}
