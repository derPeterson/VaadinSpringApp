package de.derpeterson.app.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.server.VaadinSession;
import java.util.List;
import org.junit.jupiter.api.Test;

class BrowserUnsavedChangesGuardTest {
  @Test
  void changeRevertSaveAndDetachUpdateOnlyThisFormsBrowserWarning() {
    UI testUi = new UI();
    VaadinSession testSession = mock(VaadinSession.class);
    when(testSession.hasLock()).thenReturn(true);
    testUi.getInternals().setSession(testSession);
    TextField nameTextField = new TextField();
    nameTextField.setValue("Saved name");
    VerticalLayout formLayout = new VerticalLayout(nameTextField);
    String[] savedName = {"Saved name"};
    BrowserUnsavedChangesGuard browserGuard =
        new BrowserUnsavedChangesGuard(
            formLayout, () -> !nameTextField.getValue().equals(savedName[0]));

    testUi.add(formLayout);
    assertEquals(ValueChangeMode.EAGER, nameTextField.getValueChangeMode());
    assertEquals(false, lastWarningParameters(testUi).get(1));

    nameTextField.setValue("Changed name");
    List<Object> changedParameters = lastWarningParameters(testUi);
    assertEquals(true, changedParameters.get(1));
    nameTextField.setValue("Saved name");
    assertEquals(false, lastWarningParameters(testUi).get(1));

    nameTextField.setValue("Saved again");
    savedName[0] = nameTextField.getValue();
    browserGuard.refresh();
    assertEquals(false, lastWarningParameters(testUi).get(1));
    testUi.remove(formLayout);
    List<Object> detachedParameters = lastWarningParameters(testUi);
    assertEquals(changedParameters.get(0), detachedParameters.get(0));
    assertEquals(false, detachedParameters.get(1));
  }

  private List<Object> lastWarningParameters(UI testUi) {
    testUi.getInternals().getStateTree().runExecutionsBeforeClientResponse();
    return testUi.getInternals().dumpPendingJavaScriptInvocations().stream()
        .map(PendingJavaScriptInvocation::getInvocation)
        .filter(invocation -> invocation.getExpression().contains("cosmosUnsavedForms"))
        .reduce((firstInvocation, lastInvocation) -> lastInvocation)
        .orElseThrow()
        .getParameters();
  }
}
