package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

public enum PlatformAccountStatusEnum {
    PENDING(0, "待审核"),
    ACTIVE(1, "生效"),
    INVALID(2, "失效"),
    REJECTED(3, "驳回");

    private final int code;
    private final String description;

    PlatformAccountStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static PlatformAccountStatusEnum fromCode(Integer code) {
        if (code == null) {
            return PENDING;
        }
        for (PlatformAccountStatusEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new ServiceException("未知平台账号状态");
    }
}
