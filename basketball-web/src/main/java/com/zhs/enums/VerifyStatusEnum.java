package com.zhs.enums;

/**
 * 验证码校验状态（basket_verify_code_record.verify_status）
 */
public enum VerifyStatusEnum {

    PENDING(0, "未验证"),
    USED(1, "已验证"),
    INVALID(2, "已失效");

    private final int code;
    private final String label;

    VerifyStatusEnum(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }
}
