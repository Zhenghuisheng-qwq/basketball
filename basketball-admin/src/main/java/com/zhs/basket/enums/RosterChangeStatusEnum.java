package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

public enum RosterChangeStatusEnum {
    PENDING(0, "待审核"),
    APPROVED(1, "已通过"),
    REJECTED(2, "已驳回"),
    WITHDRAWN(3, "已撤回");

    private final int code;
    private final String description;

    RosterChangeStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }

    public static RosterChangeStatusEnum fromCode(Integer code) {
        if (code == null) throw new ServiceException("未知阵容变更状态");
        for (RosterChangeStatusEnum v : values()) {
            if (v.code == code) return v;
        }
        throw new ServiceException("未知阵容变更状态");
    }
}
