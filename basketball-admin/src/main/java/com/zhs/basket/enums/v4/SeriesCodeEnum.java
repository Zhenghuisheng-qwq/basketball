package com.zhs.basket.enums.v4;

/**
 * CNPA 赛事域 V4 枚举：SeriesCodeEnum
 * 存储方式：VARCHAR 字符串 code，与旧整型枚举隔离。
 */
public enum SeriesCodeEnum {
    LEAGUE("LEAGUE", "League"),
    ARENA("ARENA", "Arena"),
    CUP("CUP", "Cup"),
    OPEN("OPEN", "Open");

    private final String code;
    private final String description;

    SeriesCodeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static SeriesCodeEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (SeriesCodeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知SeriesCodeEnum: " + code);
    }
}
