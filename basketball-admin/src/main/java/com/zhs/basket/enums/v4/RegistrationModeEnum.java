package com.zhs.basket.enums.v4;

/**
 * CNPA 赛事域 V4 枚举：RegistrationModeEnum
 * 存储方式：VARCHAR 字符串 code，与旧整型枚举隔离。
 */
public enum RegistrationModeEnum {
    TEAM("TEAM", "球队报名"),
    PERSONAL("PERSONAL", "个人报名");

    private final String code;
    private final String description;

    RegistrationModeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static RegistrationModeEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (RegistrationModeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知RegistrationModeEnum: " + code);
    }
}
