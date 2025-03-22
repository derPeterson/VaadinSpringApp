package de.derpeterson.app.helper.ui;

import com.vaadin.flow.component.Component;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StyleHelper {
    public static void addFilterWithGrayscale100Percent(Component component) {
        component.getStyle().set("filter", "grayscale(100%)");
    }

    public static void removeFilter(Component component) {
        component.getStyle().remove("filter");
    }
}
