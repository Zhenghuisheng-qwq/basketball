package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

public enum SeasonRosterStatusEnum {
    INACTIVE(0, "未生效"),
    ACTIVE(1, "生效中"),
    TRANSFERRED_OUT(2, "已转出"),
    REMOVED(3, "已移除");

    private final int code;
    private final String description;

    SeasonRosterStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }

    public static SeasonRosterStatusEnum fromCode(Integer code) {
        if (code == null) throw new ServiceException("未知阵容状态");
        for (SeasonRosterStatusEnum v : values()) {
            if (v.code == code) return v;
        }
        throw new ServiceException("未知阵容状态");
    }
}
