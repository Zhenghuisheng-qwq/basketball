package com.zhs.enums.v4;

/**
 * CNPA 赛事域 V4 枚举：DivisionStatusEnum
 * 存储方式：VARCHAR 字符串 code，与旧整型枚举隔离。
 */
public enum DivisionStatusEnum {
    DRAFT("DRAFT", "草稿"),
    REGISTRATION_OPEN("REGISTRATION_OPEN", "报名中"),
    REGISTRATION_CLOSED("REGISTRATION_CLOSED", "报名截止"),
    SCHEDULED("SCHEDULED", "已排赛"),
    PLAYING("PLAYING", "比赛中"),
    FINISHED("FINISHED", "已结束"),
    ARCHIVED("ARCHIVED", "已归档");

    private final String code;
    private final String description;

    DivisionStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static DivisionStatusEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (DivisionStatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知DivisionStatusEnum: " + code);
    }
}
