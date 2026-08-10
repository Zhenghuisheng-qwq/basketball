package com.zhs.enums;

import com.zhs.exception.ServiceException;

public enum TeamStatusEnum {

    DISABLED(0, "已禁用"),
    ACTIVE(1, "正常"),
    DISSOLVED(2, "已解散"),
    ARCHIVED(3, "已归档");

    private final int code;
    private final String description;

    TeamStatusEnum(int code, String description) {
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

    public static TeamStatusEnum fromCode(Integer code) {
        if (code == null) {
            throw new ServiceException("未知状态");
        }
        for (TeamStatusEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new ServiceException("未知状态");
    }
}
