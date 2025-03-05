package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;

public class CardComponent extends VerticalLayout {
    public CardComponent(Component... contentComponents) {
        addClassNames(LumoUtility.BoxShadow.MEDIUM);
        addClassNames(LumoUtility.Padding.LARGE);
        addClassNames(LumoUtility.Border.ALL);
        addClassNames(LumoUtility.BorderRadius.LARGE);
        addClassNames(LumoUtility.Background.BASE);

        setSpacing(false);

        add(contentComponents);
    }
}