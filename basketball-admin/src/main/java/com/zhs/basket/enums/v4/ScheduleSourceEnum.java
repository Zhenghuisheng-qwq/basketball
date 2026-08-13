package com.zhs.basket.enums.v4;

public enum ScheduleSourceEnum {
    MANUAL("MANUAL", "手工"),
    RECOMMENDED("RECOMMENDED", "推荐");

    private final String code;
    private final String description;

    ScheduleSourceEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static ScheduleSourceEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (ScheduleSourceEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown ScheduleSourceEnum: " + code);
    }
}
