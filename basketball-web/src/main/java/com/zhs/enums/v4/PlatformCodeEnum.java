package com.zhs.enums.v4;

/**
 * CNPA 赛事域 V4 枚举：PlatformCodeEnum
 * 存储方式：VARCHAR 字符串 code，与旧整型枚举隔离。
 */
public enum PlatformCodeEnum {
    PC("PC", "PC"),
    PS("PS", "PS"),
    XBOX("XBOX", "Xbox");

    private final String code;
    private final String description;

    PlatformCodeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static PlatformCodeEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (PlatformCodeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知PlatformCodeEnum: " + code);
    }
}
