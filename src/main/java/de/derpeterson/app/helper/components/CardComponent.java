package de.derpeterson.app.helper.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;

public class CardComponent extends VerticalLayout {
    public CardComponent(Component... contentComponents) {
        addClassNames(LumoUtility.BoxShadow.MEDIUM);
        addClassNames(LumoUtility.Border.ALL);
        addClassNames(LumoUtility.BorderRadius.LARGE);
        addClassNames(LumoUtility.Background.BASE);
        addClassNames(LumoUtility.Padding.LARGE);

        setSpacing(false);
        setPadding(false);

        add(contentComponents);
    }
}