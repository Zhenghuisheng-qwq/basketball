package com.zhs.enums;

import com.zhs.exception.ServiceException;

public enum RegistrationPlayerValidationStatusEnum {

    PENDING(0, "待校验"),
    PASS(1, "通过"),
    FAIL(2, "不通过");

    private final int code;
    private final String description;

    RegistrationPlayerValidationStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }


    public static RegistrationPlayerValidationStatusEnum fromCode(Integer code) {
        if (code == null) {
            throw new ServiceException("未知状态");
        }
        for (RegistrationPlayerValidationStatusEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new ServiceException("未知状态");
    }
}
