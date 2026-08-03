package com.zhs.enums.v4;

/**
 * CNPA 赛事域 V4 枚举：MatchStatusEnum
 * 存储方式：VARCHAR 字符串 code，与旧整型枚举隔离。
 */
public enum MatchStatusEnum {
    DRAFT("DRAFT", "草稿"),
    SCHEDULED("SCHEDULED", "已编排"),
    PLAYING("PLAYING", "比赛中"),
    FINISHED("FINISHED", "已结束"),
    CANCELLED("CANCELLED", "已取消"),
    FORFEITED("FORFEITED", "弃权");

    private final String code;
    private final String description;

    MatchStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static MatchStatusEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (MatchStatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知MatchStatusEnum: " + code);
    }
}
