package com.zhs.basket.enums.v4;

public enum AssignmentRoleEnum {
    CAPTAIN("CAPTAIN", "队长"),
    MEMBER("MEMBER", "队员");

    private final String code;
    private final String description;

    AssignmentRoleEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static AssignmentRoleEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (AssignmentRoleEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知AssignmentRoleEnum: " + code);
    }
}
