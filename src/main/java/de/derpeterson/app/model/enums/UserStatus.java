package de.derpeterson.app.model.enums;

import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserStatus {
    OFFLINE("userStatus.offline", "#99a3a4", "#000000", VaadinIcon.CLOSE_CIRCLE.create()),
    ABSENT("userStatus.absent", "#000000", "#f4d03f", VaadinIcon.CLOSE_CIRCLE.create()),
    EMPLOYED("userStatus.employed", "#cd6155", "#000000", VaadinIcon.MINUS_CIRCLE.create()),
    AVAILABLE("userStatus.available", "#27ae60", "#000000", VaadinIcon.CHECK_CIRCLE.create());

    private final String textKey;
    private final String color;
    private final String backgroundColor;
    private final Icon icon;
}