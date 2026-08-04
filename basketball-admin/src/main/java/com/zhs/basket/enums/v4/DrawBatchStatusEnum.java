package com.zhs.basket.enums.v4;

public enum DrawBatchStatusEnum {
    DRAFT("DRAFT", "草稿"),
    CONFIRMED("CONFIRMED", "已确认"),
    CANCELLED("CANCELLED", "已取消"),
    CONVERTED("CONVERTED", "已转换");

    private final String code;
    private final String description;

    DrawBatchStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static DrawBatchStatusEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (DrawBatchStatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知DrawBatchStatusEnum: " + code);
    }
}
