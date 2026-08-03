package com.zhs.enums.v4;

/**
 * CNPA 赛事域 V4 枚举：CaptainGrantStatusEnum
 * 存储方式：VARCHAR 字符串 code，与旧整型枚举隔离。
 */
public enum CaptainGrantStatusEnum {
    ACTIVE("ACTIVE", "有效"),
    REVOKED("REVOKED", "已撤销"),
    EXPIRED("EXPIRED", "已过期");

    private final String code;
    private final String description;

    CaptainGrantStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static CaptainGrantStatusEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (CaptainGrantStatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知CaptainGrantStatusEnum: " + code);
    }
}
