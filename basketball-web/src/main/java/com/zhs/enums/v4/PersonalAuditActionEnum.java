package com.zhs.enums.v4;

public enum PersonalAuditActionEnum {
    SUBMIT("SUBMIT", "提交"),
    APPROVE("APPROVE", "通过"),
    REJECT("REJECT", "驳回"),
    WITHDRAW("WITHDRAW", "撤回"),
    ASSIGN("ASSIGN", "分配"),
    CONVERT("CONVERT", "转换");

    private final String code;
    private final String description;

    PersonalAuditActionEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static PersonalAuditActionEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (PersonalAuditActionEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知PersonalAuditActionEnum: " + code);
    }
}
