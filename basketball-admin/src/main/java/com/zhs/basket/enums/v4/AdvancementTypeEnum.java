package com.zhs.basket.enums.v4;

public enum AdvancementTypeEnum {
    NONE("NONE", "无晋级"),
    OVERALL_TOP_N("OVERALL_TOP_N", "总排名前N"),
    GROUP_TOP_N("GROUP_TOP_N", "小组前N"),
    SERIES_WINNER("SERIES_WINNER", "系列赛胜者"),
    MANUAL("MANUAL", "人工晋级");

    private final String code;
    private final String description;

    AdvancementTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static AdvancementTypeEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (AdvancementTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知AdvancementTypeEnum: " + code);
    }

    public boolean requiresAdvancementCount() {
        return this == OVERALL_TOP_N || this == GROUP_TOP_N;
    }
}
