package com.zhs.enums;

public enum RegistrationSourceEnum {

    TEAM_REGISTRATION("TEAM_REGISTRATION", "球队报名"),
    OPEN_REGISTRATION("OPEN_REGISTRATION", "Open 报名");

    private final String code;
    private final String description;

    RegistrationSourceEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
