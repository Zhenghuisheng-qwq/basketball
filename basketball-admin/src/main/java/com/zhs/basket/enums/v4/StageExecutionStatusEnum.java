package com.zhs.basket.enums.v4;

public enum StageExecutionStatusEnum {
    DRAFT("DRAFT", "草稿"),
    READY("READY", "就绪"),
    SCHEDULE_GENERATED("SCHEDULE_GENERATED", "赛程已生成"),
    RUNNING("RUNNING", "进行中"),
    RESULT_PENDING_CONFIRM("RESULT_PENDING_CONFIRM", "结果待确认"),
    COMPLETED("COMPLETED", "已完成"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String description;

    StageExecutionStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static StageExecutionStatusEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (StageExecutionStatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知StageExecutionStatusEnum: " + code);
    }
}
