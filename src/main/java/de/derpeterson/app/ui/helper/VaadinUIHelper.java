package de.derpeterson.app.ui.helper;

import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class VaadinUIHelper {

    public static HorizontalLayout createFullHorizontalSpace() {
        HorizontalLayout hLayout = new HorizontalLayout();
        hLayout.setWidthFull();
        return hLayout;
    }


}
