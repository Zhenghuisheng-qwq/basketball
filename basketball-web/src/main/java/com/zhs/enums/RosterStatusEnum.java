package com.zhs.enums;

import com.zhs.exception.ServiceException;

public enum RosterStatusEnum {

    ACTIVE(1, "生效中"),
    TRANSFERRED_OUT(2, "已转出"),
    REMOVED(3, "已移除");

    private final int code;
    private final String description;

    RosterStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static RosterStatusEnum fromCode(Integer code) {
        if (code == null) {
            throw new ServiceException("未知阵容状态");
        }
        for (RosterStatusEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new ServiceException("未知阵容状态");
    }
}
