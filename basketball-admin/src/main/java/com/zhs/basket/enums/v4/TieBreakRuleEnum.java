package com.zhs.basket.enums.v4;

public enum TieBreakRuleEnum {
    HEAD_TO_HEAD("HEAD_TO_HEAD", "相互战绩"),
    WIN_COUNT("WIN_COUNT", "胜场数"),
    POINT_DIFFERENCE("POINT_DIFFERENCE", "净胜分"),
    TOTAL_POINTS("TOTAL_POINTS", "总得分"),
    MANUAL("MANUAL", "人工裁定");

    private final String code;
    private final String description;

    TieBreakRuleEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static TieBreakRuleEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (TieBreakRuleEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知TieBreakRuleEnum: " + code);
    }
}
