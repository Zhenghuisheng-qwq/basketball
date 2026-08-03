package com.zhs.enums.v4;

/**
 * CNPA 赛事域 V4 枚举：SubmitterRoleEnum
 */
public enum SubmitterRoleEnum {
    CAPTAIN("CAPTAIN", "队长"),
    PLAYER("PLAYER", "球员");

    private final String code;
    private final String description;

    SubmitterRoleEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }
}
