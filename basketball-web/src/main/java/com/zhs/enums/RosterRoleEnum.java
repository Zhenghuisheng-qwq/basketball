package com.zhs.enums;

import com.zhs.exception.ServiceException;

public enum RosterRoleEnum {

    PLAYER("PLAYER", "球员"),
    CAPTAIN("CAPTAIN", "队长");

    private final String code;
    private final String description;

    RosterRoleEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static RosterRoleEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            return PLAYER;
        }
        for (RosterRoleEnum value : values()) {
            if (value.code.equalsIgnoreCase(code.trim())) {
                return value;
            }
        }
        throw new ServiceException("未知赛事身份");
    }
}
