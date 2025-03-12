package de.derpeterson.app.model.enums;

import com.vaadin.flow.component.Unit;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IconSize {
    PIXEL_16(16, 16, Unit.PIXELS),
    PIXEL_24(24, 24, Unit.PIXELS),
    PIXEL_32(32, 32, Unit.PIXELS),
    PIXEL_48(48, 48, Unit.PIXELS),
    PIXEL_64(64, 64, Unit.PIXELS),
    PIXEL_96(96, 96, Unit.PIXELS),
    PIXEL_128(128, 128, Unit.PIXELS),
    PIXEL_256(256, 256, Unit.PIXELS),
    PIXEL_512(512, 512, Unit.PIXELS);

    private final double width;
    private final double height;
    private final Unit unit;
}