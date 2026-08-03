package com.zhs.basket.enums.v4;

/**
 * CNPA 赛事域 V4 枚举：SeasonStatusEnum
 * 存储方式：VARCHAR 字符串 code，与旧整型枚举隔离。
 */
public enum SeasonStatusEnum {
    NOT_STARTED("NOT_STARTED", "未开始"),
    REGISTRATION_OPEN("REGISTRATION_OPEN", "报名中"),
    REGISTRATION_CLOSED("REGISTRATION_CLOSED", "报名截止"),
    PLAYING("PLAYING", "比赛中"),
    FINISHED("FINISHED", "已结束"),
    ARCHIVED("ARCHIVED", "已归档");

    private final String code;
    private final String description;

    SeasonStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static SeasonStatusEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (SeasonStatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知SeasonStatusEnum: " + code);
    }
}
