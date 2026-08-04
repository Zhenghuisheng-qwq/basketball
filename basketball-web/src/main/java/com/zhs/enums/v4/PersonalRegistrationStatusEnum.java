package com.zhs.enums.v4;

public enum PersonalRegistrationStatusEnum {
    DRAFT("DRAFT", "草稿"),
    PENDING_REVIEW("PENDING_REVIEW", "待审核"),
    APPROVED("APPROVED", "已通过"),
    REJECTED("REJECTED", "已驳回"),
    WITHDRAWN("WITHDRAWN", "已撤回"),
    ASSIGNED("ASSIGNED", "已分配"),
    CONVERTED("CONVERTED", "已转换");

    private final String code;
    private final String description;

    PersonalRegistrationStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static PersonalRegistrationStatusEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (PersonalRegistrationStatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知PersonalRegistrationStatusEnum: " + code);
    }
}
