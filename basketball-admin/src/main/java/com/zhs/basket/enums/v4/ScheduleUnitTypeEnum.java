package com.zhs.basket.enums.v4;

public enum ScheduleUnitTypeEnum {
    MATCH("MATCH", "BO1 Match"),
    MATCH_SERIES("MATCH_SERIES", "BO3/5/7 MatchSeries");

    private final String code;
    private final String description;

    ScheduleUnitTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static ScheduleUnitTypeEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (ScheduleUnitTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown ScheduleUnitTypeEnum: " + code);
    }
}
