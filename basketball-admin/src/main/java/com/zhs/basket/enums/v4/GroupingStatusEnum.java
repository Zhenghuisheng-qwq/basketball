package com.zhs.basket.enums.v4;

public enum GroupingStatusEnum {
    DRAFT("DRAFT", "草稿"),
    LOCKED("LOCKED", "已锁定"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String description;

    GroupingStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static GroupingStatusEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (GroupingStatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知GroupingStatusEnum: " + code);
    }
}
