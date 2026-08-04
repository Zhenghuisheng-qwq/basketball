package com.zhs.basket.enums.v4;

public enum RegistrationSourceEnum {
    TEAM_DIRECT("TEAM_DIRECT", "球队直接报名"),
    PERSONAL_FORMATION("PERSONAL_FORMATION", "个人组队转换");

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

    public static RegistrationSourceEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (RegistrationSourceEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知RegistrationSourceEnum: " + code);
    }
}
