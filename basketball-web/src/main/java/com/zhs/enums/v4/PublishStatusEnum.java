package com.zhs.enums.v4;

/**
 * CNPA 赛事域 V4 枚举：PublishStatusEnum
 * 存储方式：VARCHAR 字符串 code，与旧整型枚举隔离。
 */
public enum PublishStatusEnum {
    DRAFT("DRAFT", "草稿"),
    PUBLISHED("PUBLISHED", "已发布"),
    OFFLINE("OFFLINE", "已下架");

    private final String code;
    private final String description;

    PublishStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static PublishStatusEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (PublishStatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知PublishStatusEnum: " + code);
    }
}
