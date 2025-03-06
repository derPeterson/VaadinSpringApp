package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.Arrays;

public class RegistrationCardComponent extends VerticalLayout {
    public RegistrationCardComponent(Component... contentComponents) {
        addClassNames(LumoUtility.BoxShadow.MEDIUM);
        addClassNames(LumoUtility.Border.ALL);
        addClassNames(LumoUtility.BorderRadius.LARGE);
        addClassNames(LumoUtility.Background.BASE);

        setSpacing(false);
        setPadding(false);

        HorizontalLayout mainCardContent = new HorizontalLayout();
        mainCardContent.setPadding(false);
        mainCardContent.setSpacing(false);
        mainCardContent.setMargin(false);

        VerticalLayout leftCardContent = new VerticalLayout();
        leftCardContent.setPadding(false);
        leftCardContent.setSpacing(false);
        leftCardContent.setAlignItems(Alignment.CENTER);
        leftCardContent.setMinWidth("616px");
        leftCardContent.setJustifyContentMode(JustifyContentMode.CENTER);
        leftCardContent.addClassNames(LumoUtility.Background.CONTRAST_20);
        leftCardContent.addClassNames("border-left-rounded");
        leftCardContent.addClassNames(LumoUtility.Border.RIGHT);
        leftCardContent.addClassNames("registration-bg-card");

        VerticalLayout rightCardContent = new VerticalLayout();
        rightCardContent.setSpacing(false);
        rightCardContent.setPadding(false);
        rightCardContent.addClassNames(LumoUtility.Padding.LARGE);

        mainCardContent.setFlexGrow(1, leftCardContent);
        mainCardContent.setFlexGrow(1, rightCardContent);

        Arrays.stream(contentComponents).forEach(contentComponent -> contentComponent.addClassNames(LumoUtility.Padding.MEDIUM));

        rightCardContent.add(contentComponents);

        mainCardContent.add(leftCardContent, rightCardContent);
        add(mainCardContent);
    }
}