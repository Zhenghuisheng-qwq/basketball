package com.zhs.basket.enums.v4;

public enum GroupingResultSourceEnum {
    OFFLINE_RANDOM("OFFLINE_RANDOM", "线下随机"),
    MANUAL("MANUAL", "人工分配"),
    IMPORT("IMPORT", "导入");

    private final String code;
    private final String description;

    GroupingResultSourceEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static GroupingResultSourceEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (GroupingResultSourceEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知GroupingResultSourceEnum: " + code);
    }
}
