package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.helper.ui.ComponentTextUpdateHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.IconSize;
import de.derpeterson.app.model.enums.UserStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class OverlayUserIcon extends Div {

    private final transient MessageProperties messageProperties;
    private final UserStatus userStatus;

    private Tooltip userImageToolTip = null;
    private Tooltip overlayIconTooltip = null;

    public OverlayUserIcon(MessageProperties messageProperties, Image userImage, UserStatus userStatus, IconSize iconSize) {
        this.messageProperties = messageProperties;
        this.userStatus = userStatus;

        ComponentUtil.addListener(UI.getCurrent(), LanguageChangeEvent.class, event -> {
            VaadinSession.getCurrent().setLocale(event.getNewLocale());

            Map<Tooltip, Supplier<String>> tooltipTranslationSupplierMap = new HashMap<>();
            Optional.ofNullable(this.userImageToolTip)
                    .ifPresent(tooltip -> tooltipTranslationSupplierMap.put(tooltip, () -> this.messageProperties.getTranslation(this.userStatus.getTextKey())));
            Optional.ofNullable(this.overlayIconTooltip)
                    .ifPresent(tooltip -> tooltipTranslationSupplierMap.put(tooltip, () -> this.messageProperties.getTranslation(this.userStatus.getTextKey())));

            ComponentTextUpdateHelper.updateToolTipComponents(tooltipTranslationSupplierMap);
        });

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

        this.userImageToolTip = Tooltip.forComponent(userImage)
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

            this.overlayIconTooltip = Tooltip.forComponent(overlayIcon)
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
