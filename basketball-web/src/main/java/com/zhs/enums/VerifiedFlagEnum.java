package com.zhs.enums;

/**
 * 邮箱/手机验证标记（email_verified / mobile_verified）
 */
public enum VerifiedFlagEnum {

    NO(0, "未验证"),
    YES(1, "已验证");

    private final int code;
    private final String label;

    VerifiedFlagEnum(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static boolean isYes(Integer value) {
        return value != null && value == YES.code;
    }
}
