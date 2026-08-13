package com.zhs.basket.enums.v4;

public enum ScheduleStatusEnum {
    UNSCHEDULED("UNSCHEDULED", "未排期"),
    SCHEDULED("SCHEDULED", "已排期");

    private final String code;
    private final String description;

    ScheduleStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static ScheduleStatusEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (ScheduleStatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown ScheduleStatusEnum: " + code);
    }
}
