package com.zhs.enums;

import com.zhs.exception.ServiceException;

/**
 * 验证码用途
 */
public enum VerifyPurposeEnum {

    REGISTER("REGISTER", "注册"),
    RESET_PASSWORD("RESET_PASSWORD", "重置密码"),
    CHANGE_EMAIL("CHANGE_EMAIL", "修改邮箱");

    private final String code;
    private final String label;

    VerifyPurposeEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static VerifyPurposeEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new ServiceException("验证码用途不能为空");
        }
        String normalized = code.trim().toUpperCase();
        for (VerifyPurposeEnum value : values()) {
            if (value.code.equals(normalized)) {
                return value;
            }
        }
        throw new ServiceException("不支持的验证码用途");
    }
}
