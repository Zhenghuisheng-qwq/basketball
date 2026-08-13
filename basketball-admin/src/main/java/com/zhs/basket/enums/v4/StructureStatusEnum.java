package com.zhs.basket.enums.v4;

/**
 * Stage Structure（ScheduleBatch）有效性，与 PublishStatus 独立。
 */
public enum StructureStatusEnum {
    ACTIVE("ACTIVE", "有效"),
    STALE("STALE", "已失效");

    private final String code;
    private final String description;

    StructureStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static StructureStatusEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (StructureStatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知StructureStatusEnum: " + code);
    }

    public boolean isUsable() {
        return this == ACTIVE;
    }
}
