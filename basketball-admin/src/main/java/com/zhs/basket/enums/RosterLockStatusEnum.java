package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

public enum RosterLockStatusEnum {
    UNLOCKED(0, "未锁定"),
    LOCKED(1, "已锁定");

    private final int code;
    private final String description;

    RosterLockStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }

    public static RosterLockStatusEnum fromCode(Integer code) {
        if (code == null) throw new ServiceException("未知阵容锁定状态");
        for (RosterLockStatusEnum v : values()) {
            if (v.code == code) return v;
        }
        throw new ServiceException("未知阵容锁定状态");
    }
}
