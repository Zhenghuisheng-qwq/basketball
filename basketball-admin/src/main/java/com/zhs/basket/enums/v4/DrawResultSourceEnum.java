package com.zhs.basket.enums.v4;

public enum DrawResultSourceEnum {
    OFFLINE_RANDOM("OFFLINE_RANDOM", "线下随机"),
    MANUAL_ADJUSTMENT("MANUAL_ADJUSTMENT", "人工调整"),
    IMPORT("IMPORT", "导入");

    private final String code;
    private final String description;

    DrawResultSourceEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static DrawResultSourceEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (DrawResultSourceEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知DrawResultSourceEnum: " + code);
    }
}
