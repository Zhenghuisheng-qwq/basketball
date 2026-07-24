package com.zhs.enums;

import com.zhs.exception.ServiceException;

public enum CaptainQualificationStatusEnum {

    ACTIVE(1, "有效"),
    REVOKED(0, "已撤销"),
    EXPIRED(2, "已过期");

    private final int code;
    private final String description;

    CaptainQualificationStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return this == ACTIVE;
    }

    public static CaptainQualificationStatusEnum fromCode(Integer code) {
        if (code == null) {
            throw new ServiceException("未知状态");
        }
        for (CaptainQualificationStatusEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new ServiceException("未知状态");
    }
}
