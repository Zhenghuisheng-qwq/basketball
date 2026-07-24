package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

public enum RosterRoleEnum {
    PLAYER("PLAYER", "球员"),
    CAPTAIN("CAPTAIN", "队长");

    private final String code;
    private final String description;

    RosterRoleEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static RosterRoleEnum fromCode(String code) {
        if (code == null || code.isBlank()) throw new ServiceException("未知出场角色");
        for (RosterRoleEnum v : values()) {
            if (v.code.equalsIgnoreCase(code.trim())) return v;
        }
        throw new ServiceException("未知出场角色");
    }
}
