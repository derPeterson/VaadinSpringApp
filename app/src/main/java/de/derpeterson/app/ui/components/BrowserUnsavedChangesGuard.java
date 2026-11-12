package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.data.value.HasValueChangeMode;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.SerializableSupplier;
import java.io.Serializable;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.UUID;

/** Ergänzt den bestehenden fachlichen Änderungsvergleich um den nativen Browserhinweis. */
public final class BrowserUnsavedChangesGuard implements Serializable {
  private final Component formComponent;
  private final SerializableSupplier<Boolean> unsavedChangesSupplier;
  private final String guardId = UUID.randomUUID().toString();
  private final Set<Component> observedComponents =
      Collections.newSetFromMap(new IdentityHashMap<>());

  public BrowserUnsavedChangesGuard(
      Component formComponent, SerializableSupplier<Boolean> unsavedChangesSupplier) {
    this.formComponent = formComponent;
    this.unsavedChangesSupplier = unsavedChangesSupplier;
    formComponent.addAttachListener(event -> {
      observeFields(formComponent);
      refresh();
    });
    formComponent.addDetachListener(event -> publishWarning(event.getUI(), false));
    // Auch ungültige Datumseingaben können sich ändern, ohne einen neuen Feldwert zu liefern.
    formComponent.getElement().addEventListener("change", event -> refresh());
  }

  public void observeFields(Component component) {
    if (!observedComponents.add(component)) {
      return;
    }
    if (component instanceof HasValueChangeMode valueChangeComponent) {
      valueChangeComponent.setValueChangeMode(ValueChangeMode.EAGER);
    }
    if (component instanceof HasValue<?, ?> valueComponent) {
      valueComponent.addValueChangeListener(event -> refresh());
    }
    component.getChildren().forEach(this::observeFields);
  }

  public void refresh() {
    formComponent
        .getUI()
        .ifPresent(ui ->
            ui.beforeClientResponse(
                formComponent, context -> publishWarning(ui, unsavedChangesSupplier.get())));
  }

  private void publishWarning(UI ui, boolean unsavedChanges) {
    // Mehrere Formulare können gleichzeitig existieren. Ein sauberes Formular darf den
    // Schutz eines anderen nicht entfernen. Kein Passwort oder Formularinhalt verlässt Java.
    ui.getPage()
        .executeJs(
            """
            const state = window.cosmosUnsavedForms ??= {
              ids: new Set(),
              listener: event => { event.preventDefault(); event.returnValue = ''; }
            };
            if ($1) { state.ids.add($0); } else { state.ids.delete($0); }
            window.removeEventListener('beforeunload', state.listener);
            if (state.ids.size > 0) { window.addEventListener('beforeunload', state.listener); }
            """,
            guardId,
            unsavedChanges);
  }
}
