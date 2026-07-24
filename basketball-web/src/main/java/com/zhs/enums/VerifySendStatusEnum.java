package com.zhs.enums;

/**
 * 验证码发送状态（basket_verify_code_record.send_status）
 */
public enum VerifySendStatusEnum {

    INIT(0, "初始"),
    SUCCESS(1, "成功"),
    FAIL(2, "失败");

    private final int code;
    private final String label;

    VerifySendStatusEnum(int code, String label) {
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
