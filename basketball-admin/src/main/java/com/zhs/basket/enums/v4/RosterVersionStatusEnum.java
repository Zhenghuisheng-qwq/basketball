package com.zhs.basket.enums.v4;

/**
 * Division 名单版本状态。
 */
public enum RosterVersionStatusEnum {
    DRAFT("DRAFT", "草稿"),
    LOCKED("LOCKED", "已锁定"),
    INVALID("INVALID", "已失效");

    private final String code;
    private final String description;

    RosterVersionStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static RosterVersionStatusEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (RosterVersionStatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知RosterVersionStatusEnum: " + code);
    }
}
