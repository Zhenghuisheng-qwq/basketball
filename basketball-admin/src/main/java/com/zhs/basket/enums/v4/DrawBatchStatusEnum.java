package com.zhs.basket.enums.v4;

public enum DrawBatchStatusEnum {
    DRAFT("DRAFT", "草稿"),
    GENERATED("GENERATED", "已生成"),
    ADJUSTING("ADJUSTING", "调整中"),
    LOCKED("LOCKED", "已锁定"),
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

    public boolean isEditable() {
        return this == DRAFT || this == GENERATED || this == ADJUSTING || this == CONFIRMED;
    }

    public boolean isLockedOrConverted() {
        return this == LOCKED || this == CONVERTED;
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
