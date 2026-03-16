package de.derpeterson.app.model.enums;

import de.derpeterson.app.i18n.MessageProperties;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

@Getter
@RequiredArgsConstructor
public enum NavigationErrorCode {

    ADMIN_ACCESS_DENIED(
            "admin-access-denied",
            MessageProperties::getBaseErrorAdminAccessDenied
    ),

    ACCESS_DENIED(
            "access-denied",
            MessageProperties::getBaseErrorAccessDenied
    ),

    PATH_NOT_FOUND(
            "path-not-found",
            MessageProperties::getBaseErrorPathNotFound
    );

    private final String code;
    private final Function<MessageProperties, String> messageResolver;

    public String getMessage(MessageProperties messageProperties) {
        return messageResolver.apply(messageProperties);
    }

    public static Optional<NavigationErrorCode> fromCode(String code) {
        return Arrays.stream(values())
                .filter(value -> value.code.equals(code))
                .findFirst();
    }
}