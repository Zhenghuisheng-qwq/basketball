package com.zhs.basket.enums.v4;

public enum QualificationRecordStatusEnum {
    PREVIEW("PREVIEW", "预览"),
    CONFIRMED("CONFIRMED", "已确认"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String description;

    QualificationRecordStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static QualificationRecordStatusEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (QualificationRecordStatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知QualificationRecordStatusEnum: " + code);
    }
}
