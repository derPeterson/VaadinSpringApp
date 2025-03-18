package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.shared.Tooltip;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.IconSize;
import de.derpeterson.app.model.enums.UserStatus;

public class OverlayUserIcon extends Div {

    public OverlayUserIcon(MessageProperties messageProperties, Image userImage, UserStatus userStatus, IconSize iconSize) {
        Div container = new Div();

        container.getStyle()
                .set("position", "relative")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center");

        userImage.setWidth(iconSize.getWidth() + iconSize.getUnit().getSymbol());
        userImage.setHeight(iconSize.getHeight() + iconSize.getUnit().getSymbol());
        userImage.getStyle().set("background-color", "black")
                .set("border-radius", "50%")
                .set("box-shadow", "0 0 0 0.5px black");

        Tooltip.forComponent(userImage)
                .withText(messageProperties.getTranslation(userStatus.getTextKey()))
                .withPosition(Tooltip.TooltipPosition.BOTTOM)
                .withFocusDelay(1000)
                .withHoverDelay(1000)
                .withHideDelay(1000);

        Icon overlayIcon = null;
        if (userStatus == UserStatus.AVAILABLE) {
            overlayIcon = VaadinIcon.CHECK_CIRCLE.create();
        }
        if (userStatus == UserStatus.ABSENT) {
            overlayIcon = VaadinIcon.CLOCK.create();
        }
        if (userStatus == UserStatus.EMPLOYED) {
            overlayIcon = VaadinIcon.MINUS_CIRCLE.create();
        }
        if (userStatus == UserStatus.OFFLINE) {
            overlayIcon = VaadinIcon.CLOSE_CIRCLE.create();
        }

        if (overlayIcon != null) {
            overlayIcon.getStyle()
                    .set("width", String.valueOf(Math.round((iconSize.getWidth() * 20) / 100) + iconSize.getUnit().getSymbol()))
                    .set("height", String.valueOf(Math.round((iconSize.getHeight() * 20) / 100) + iconSize.getUnit().getSymbol()))
                    .set("position", "absolute")
                    .set("bottom", String.valueOf(Math.round((iconSize.getHeight() * 5) / 100) + iconSize.getUnit().getSymbol()))
                    .set("right", String.valueOf(Math.round((iconSize.getWidth() * 5) / 100) + iconSize.getUnit().getSymbol()))
                    .set("color", userStatus.getColor())
                    .set("background-color", userStatus.getBackgroundColor())
                    .set("border-radius", "50%");

            if (userStatus != UserStatus.ABSENT) {
                overlayIcon.getStyle().set("box-shadow", "0 0 0 0.5px " + userStatus.getBackgroundColor());
            }

            Tooltip.forComponent(overlayIcon)
                    .withText(messageProperties.getTranslation(userStatus.getTextKey()))
                    .withPosition(Tooltip.TooltipPosition.BOTTOM)
                    .withFocusDelay(1000)
                    .withHoverDelay(1000)
                    .withHideDelay(1000);

            container.add(userImage, overlayIcon);
        }
        add(container);
    }
}
