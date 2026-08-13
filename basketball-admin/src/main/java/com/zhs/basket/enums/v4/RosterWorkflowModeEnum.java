package com.zhs.basket.enums.v4;

public enum RosterWorkflowModeEnum {
    LEGACY("LEGACY", "LEGACY"),
    VERSIONED("VERSIONED", "VERSIONED");

    private final String code;
    private final String description;

    RosterWorkflowModeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static RosterWorkflowModeEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return LEGACY;
        }
        for (RosterWorkflowModeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("unknown RosterWorkflowModeEnum: " + code);
    }

    public boolean isVersioned() {
        return this == VERSIONED;
    }
}
