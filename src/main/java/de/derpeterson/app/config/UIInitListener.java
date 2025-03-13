package de.derpeterson.app.config;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import de.derpeterson.app.helper.ui.NotificationHelper;
import org.springframework.stereotype.Component;

@Component
public class UIInitListener implements VaadinServiceInitListener {

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(uiInitEvent -> {
            UI ui = uiInitEvent.getUI();
            ui.addBeforeEnterListener(e -> NotificationHelper.getInstance().closeAndClearAllNotifications());
        });
    }
}
