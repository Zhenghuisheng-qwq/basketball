package com.zhs.basket.enums.v4;

public enum DivisionStageStatusEnum {
    DRAFT("DRAFT", "草稿"),
    READY("READY", "就绪"),
    RUNNING("RUNNING", "进行中"),
    COMPLETED("COMPLETED", "已完成"),
    LOCKED("LOCKED", "已锁定"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String description;

    DivisionStageStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static DivisionStageStatusEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (DivisionStageStatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知DivisionStageStatusEnum: " + code);
    }
}
