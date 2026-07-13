package com.showdown.backend.domain;

public enum AppRole {
    ADMIN("system_admin"),
    TOURNAMENT_ADMIN("tournament_admin"),
    REFEREE("scorer"),
    PLAYER("player");

    private final String roleCode;

    AppRole(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public static AppRole fromRoleCode(String roleCode) {
        for (AppRole role : values()) {
            if (role.roleCode.equals(roleCode)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role code: " + roleCode);
    }
}
