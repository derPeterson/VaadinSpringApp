package de.derpeterson.app.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum RoleType {
    ROLE_ADMIN("ADMIN", "ROLE_ADMIN"),
    ROLE_USER("USER", "ROLE_USER");

    private final String name;
    private final String longName;
}
