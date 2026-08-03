package com.zhs.basket.enums.v4;

/**
 * CNPA 赛事域 V4 枚举：SubmissionStatusEnum
 * 存储方式：VARCHAR 字符串 code，与旧整型枚举隔离。
 */
public enum SubmissionStatusEnum {
    DRAFT("DRAFT", "草稿"),
    PENDING_REVIEW("PENDING_REVIEW", "待审核"),
    APPROVED("APPROVED", "已通过"),
    REJECTED("REJECTED", "已驳回"),
    SUPERSEDED("SUPERSEDED", "已替代"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String description;

    SubmissionStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static SubmissionStatusEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (SubmissionStatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知SubmissionStatusEnum: " + code);
    }
}
