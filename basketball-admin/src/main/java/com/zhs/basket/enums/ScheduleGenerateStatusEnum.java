package com.zhs.basket.enums;

public enum ScheduleGenerateStatusEnum {
    SUCCESS(1, "成功"),
    FAILED(2, "失败");

    private final int code;
    private final String description;

    ScheduleGenerateStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }
}
