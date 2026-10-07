package de.derpeterson.app.model.enums;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.dom.Style;
import de.derpeterson.app.config.AppConstants;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Getter
@RequiredArgsConstructor
public enum UserStatus {
    AVAILABLE("userStatus.available", "userStatus.available.description", "#27ae60", AppConstants.BLACK_COLOR_HEX_STRING, VaadinIcon.CHECK_CIRCLE, null),
    EMPLOYED("userStatus.employed", "userStatus.employed.description", "#cd6155", AppConstants.BLACK_COLOR_HEX_STRING, VaadinIcon.MINUS_CIRCLE, null),
    ABSENT("userStatus.absent", "userStatus.absent.description", "#f4d03f", AppConstants.BLACK_COLOR_HEX_STRING, null, "/META-INF/resources/custom-theme/icons/clock_circle.svg"),
    OFFLINE("userStatus.offline", "userStatus.offline.description", "#99a3a4", AppConstants.BLACK_COLOR_HEX_STRING, VaadinIcon.CLOSE_CIRCLE, null);

    private final String textKey;
    private final String textDescriptionKey;
    private final String color;
    private final String backgroundColor;
    private final VaadinIcon vaadinIcon;
    private final String fileUrl;


    public Component getComponent() {
        return getComponent(IconSize.PIXEL_24, false);
    }

    public Component getComponent(IconSize iconSize) {
        return getComponent(iconSize, false);
    }

    public Component getComponent(IconSize iconSize, Boolean isOverlayComponent) {
        switch (this) {
            case AVAILABLE, EMPLOYED, OFFLINE -> {
                Icon icon = vaadinIcon.create();
                if (Boolean.TRUE.equals(isOverlayComponent)) {
                    addOverlaySizeAndStylesToIcon(icon, iconSize);
                } else {
                    addSizeAndStylesToIcon(icon, iconSize);
                }
                return icon;
            }
            case ABSENT -> {
                Div absentDiv = (Div) createSvgComponent(fileUrl, color, iconSize, isOverlayComponent);
                if (Boolean.TRUE.equals(isOverlayComponent)) {
                    var width = Math.round((iconSize.getWidth() * 20) / 100) + iconSize.getUnit().getSymbol();
                    var height = Math.round((iconSize.getHeight() * 20) / 100) + iconSize.getUnit().getSymbol();

                    absentDiv.setWidth(width);
                    absentDiv.setHeight(height);
                    absentDiv.getStyle()
                            .setWidth(width)
                            .setHeight(height)
                            .setPosition(Style.Position.ABSOLUTE)
                            .setBottom(Math.round((iconSize.getHeight() * 5) / 100) + iconSize.getUnit().getSymbol())
                            .setRight(Math.round((iconSize.getWidth() * 5) / 100) + iconSize.getUnit().getSymbol())
                            .setColor(color)
                            .setBackgroundColor(backgroundColor)
                            .setBorderRadius("50%");

                } else {
                    var width = iconSize.getWidth() + iconSize.getUnit().getSymbol();
                    var height = iconSize.getHeight() + iconSize.getUnit().getSymbol();

                    absentDiv.setWidth(width);
                    absentDiv.setHeight(height);
                    absentDiv.getStyle()
                            .setWidth(width)
                            .setHeight(height)
                            .setBackgroundColor(backgroundColor)
                            .setBorderRadius("50%");
                }
                return absentDiv;
            }
        }

        return null;
    }

    private void addOverlaySizeAndStylesToIcon(Icon icon, IconSize iconSize) {
        var width = Math.round((iconSize.getWidth() * 20) / 100) + iconSize.getUnit().getSymbol();
        var height = Math.round((iconSize.getHeight() * 20) / 100) + iconSize.getUnit().getSymbol();

        icon.setSize(width);
        icon.getStyle()
                .setWidth(width)
                .setHeight(height)
                .setPosition(Style.Position.ABSOLUTE)
                .setBottom(Math.round((iconSize.getHeight() * 5) / 100) + iconSize.getUnit().getSymbol())
                .setRight(Math.round((iconSize.getWidth() * 5) / 100) + iconSize.getUnit().getSymbol())
                .setColor(color)
                .setBackgroundColor(backgroundColor)
                .setBorderRadius("50%");
    }

    private void addSizeAndStylesToIcon(Icon icon, IconSize iconSize) {
        var width = iconSize.getWidth() + iconSize.getUnit().getSymbol();
        var height = iconSize.getHeight() + iconSize.getUnit().getSymbol();

        icon.setSize(width);
        icon.getStyle()
                .setWidth(width)
                .setHeight(height)
                .setColor(color)
                .setBackgroundColor(backgroundColor)
                .setBorderRadius("50%");
    }

    public static UserStatus getDefaultStatus() {
        return OFFLINE;
    }

    private static Component createSvgComponent(String fileUrl, String color, IconSize iconSize, Boolean isOverlayComponent) {
        try (InputStream stream = UserStatus.class.getResourceAsStream(fileUrl)) {
            if (stream == null) {
                throw new IOException("Status-SVG fehlt im Classpath: " + fileUrl);
            }
            String svgContent = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

            // 2️⃣ `fill`-Farbe ersetzen
            if (svgContent.contains("fill=")) {
                svgContent = svgContent.replaceAll("fill=\"#[0-9a-fA-F]{6}\"", "fill=\"" + color + "\"");
            } else {
                svgContent = svgContent.replace("<svg", "<svg fill=\"" + color + "\"");
            }

            var width = iconSize.getWidth();
            var height = iconSize.getHeight();

            if (Boolean.TRUE.equals(isOverlayComponent)) {
                width = Math.round((iconSize.getWidth() * 20) / 100);
                height = Math.round((iconSize.getHeight() * 20) / 100);

                String overlayStyles = "position: absolute; right: 0px; bottom: 0px;";

                if (svgContent.contains("style=")) {
                    svgContent = svgContent.replaceAll("style=\"([^\"]*)\"", "style=\"$1; " + overlayStyles + "\"");
                } else {
                    svgContent = svgContent.replace("<svg", "<svg style=\"" + overlayStyles + "\"");
                }
            }

            // 3️⃣ Breite und Höhe dynamisch setzen
            if (svgContent.contains("width=") && svgContent.contains("height=")) {
                svgContent = svgContent.replaceAll("width=\"\\d+\"", "width=\"" + width + "\"");
                svgContent = svgContent.replaceAll("height=\"\\d+\"", "height=\"" + height + "\"");
            } else {
                svgContent = svgContent.replace("<svg", "<svg width=\"" + width + "\" height=\"" + height + "\"");
            }

            // 4️⃣ In `Div` als `innerHTML` setzen
            Div svgContainer = new Div();
            svgContainer.getElement().setProperty("innerHTML", svgContent);

            return svgContainer;
        } catch (IOException e) {
            LoggerFactory.getLogger(UserStatus.class).error("❌ Exception occurred:", e);
            return new Div();
        }
    }
}
