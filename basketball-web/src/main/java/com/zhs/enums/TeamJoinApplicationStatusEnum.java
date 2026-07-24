package com.zhs.enums;

import com.zhs.exception.ServiceException;

public enum TeamJoinApplicationStatusEnum {

    PENDING(0, "待处理"),
    APPROVED(1, "已通过"),
    REJECTED(2, "已驳回"),
    CANCELLED(3, "已取消");

    private final int code;
    private final String description;

    TeamJoinApplicationStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public boolean canProcess() {
        return this == PENDING;
    }

    public boolean canCancel() {
        return this == PENDING;
    }

    public static TeamJoinApplicationStatusEnum fromCode(Integer code) {
        if (code == null) {
            throw new ServiceException("未知状态");
        }
        for (TeamJoinApplicationStatusEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new ServiceException("未知状态");
    }
}
