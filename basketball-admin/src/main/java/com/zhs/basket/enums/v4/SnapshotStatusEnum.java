package com.zhs.basket.enums.v4;

public enum SnapshotStatusEnum {
    DRAFT("DRAFT", "草稿"),
    LOCKED("LOCKED", "已锁定"),
    IN_USE("IN_USE", "使用中"),
    ARCHIVED("ARCHIVED", "已归档");

    private final String code;
    private final String description;

    SnapshotStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static SnapshotStatusEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (SnapshotStatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知SnapshotStatusEnum: " + code);
    }

    public boolean isLockedOrBeyond() {
        return this == LOCKED || this == IN_USE || this == ARCHIVED;
    }
}
