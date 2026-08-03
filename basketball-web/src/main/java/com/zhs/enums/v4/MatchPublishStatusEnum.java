package com.zhs.enums.v4;

/**
 * CNPA 赛事域 V4 枚举：MatchPublishStatusEnum
 * 存储方式：VARCHAR 字符串 code，与旧整型枚举隔离。
 */
public enum MatchPublishStatusEnum {
    DRAFT("DRAFT", "草稿"),
    PUBLISHED("PUBLISHED", "已发布"),
    OFFLINE("OFFLINE", "已下架");

    private final String code;
    private final String description;

    MatchPublishStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static MatchPublishStatusEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (MatchPublishStatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知MatchPublishStatusEnum: " + code);
    }
}
