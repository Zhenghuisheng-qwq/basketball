package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

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

    public int getCode() { return code; }
    public String getDescription() { return description; }

    public static TeamStatusEnum fromCode(Integer code) {
        if (code == null) throw new ServiceException("未知球队状态");
        for (TeamStatusEnum v : values()) {
            if (v.code == code) return v;
        }
        throw new ServiceException("未知球队状态");
    }
}
