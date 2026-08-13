package com.zhs.basket.enums.v4;

public enum GroupingModeEnum {
    NONE("NONE", "无分组"),
    OFFLINE_RANDOM_RESULT("OFFLINE_RANDOM_RESULT", "线下抽签结果录入"),
    MANUAL("MANUAL", "人工分组"),
    SEEDED("SEEDED", "种子分组"),
    POT_RANDOM("POT_RANDOM", "分档随机");

    private final String code;
    private final String description;

    GroupingModeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static GroupingModeEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (GroupingModeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知GroupingModeEnum: " + code);
    }
}
