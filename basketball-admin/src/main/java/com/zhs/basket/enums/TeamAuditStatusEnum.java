package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

public enum TeamAuditStatusEnum {
    PENDING(0, "待审核"),
    APPROVED(1, "已通过"),
    REJECTED(2, "已驳回"),
    REVIEWING(3, "审核中");

    private final int code;
    private final String description;

    TeamAuditStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }

    public static TeamAuditStatusEnum fromCode(Integer code) {
        if (code == null) throw new ServiceException("未知审核状态");
        for (TeamAuditStatusEnum v : values()) {
            if (v.code == code) return v;
        }
        throw new ServiceException("未知审核状态");
    }
}
