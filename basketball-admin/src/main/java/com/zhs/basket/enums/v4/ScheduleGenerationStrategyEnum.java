package com.zhs.basket.enums.v4;

public enum ScheduleGenerationStrategyEnum {
    ROUND_ROBIN("ROUND_ROBIN", "循环赛"),
    GROUP_STAGE("GROUP_STAGE", "小组赛"),
    SINGLE_ELIMINATION("SINGLE_ELIMINATION", "单淘汰");

    private final String code;
    private final String description;

    ScheduleGenerationStrategyEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ScheduleGenerationStrategyEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (ScheduleGenerationStrategyEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知ScheduleGenerationStrategyEnum: " + code);
    }
}
