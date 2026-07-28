package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

public enum CaptainQualificationStatusEnum {
    REVOKED(0, "已收回"),
    ACTIVE(1, "有效"),
    EXPIRED(2, "已过期");

    private final int code;
    private final String description;

    CaptainQualificationStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }

    public static CaptainQualificationStatusEnum fromCode(Integer code) {
        if (code == null) throw new ServiceException("未知资格状态");
        for (CaptainQualificationStatusEnum v : values()) {
            if (v.code == code) return v;
        }
        throw new ServiceException("未知资格状态");
    }
}
