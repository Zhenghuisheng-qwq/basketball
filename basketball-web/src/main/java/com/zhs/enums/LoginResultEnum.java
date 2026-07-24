package com.zhs.enums;

/**
 * 登录结果（basket_login_log.login_result）
 */
public enum LoginResultEnum {

    FAIL(0, "失败"),
    SUCCESS(1, "成功");

    private final int code;
    private final String label;

    LoginResultEnum(int code, String label) {
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
