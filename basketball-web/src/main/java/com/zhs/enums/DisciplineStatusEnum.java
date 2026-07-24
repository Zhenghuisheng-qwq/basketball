package com.zhs.enums;

import com.zhs.exception.ServiceException;

public enum DisciplineStatusEnum {

    ACTIVE(1, "生效中"),
    REVOKED(0, "已撤销"),
    EXPIRED(2, "已过期");

    private final int code;
    private final String description;

    DisciplineStatusEnum(int code, String description) {
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

    public static DisciplineStatusEnum fromCode(Integer code) {
        if (code == null) {
            throw new ServiceException("未知状态");
        }
        for (DisciplineStatusEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new ServiceException("未知状态");
    }
}
