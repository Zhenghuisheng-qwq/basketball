package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

public enum PlatformChangeStatusEnum {
    PENDING(0, "待审核"),
    APPROVED(1, "已通过"),
    REJECTED(2, "已驳回"),
    WITHDRAWN(3, "已撤回");

    private final int code;
    private final String description;

    PlatformChangeStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static PlatformChangeStatusEnum fromCode(Integer code) {
        if (code == null) {
            return PENDING;
        }
        for (PlatformChangeStatusEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new ServiceException("未知平台变更状态");
    }
}
