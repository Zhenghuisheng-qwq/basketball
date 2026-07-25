package com.zhs.enums;

import com.zhs.exception.ServiceException;

public enum RosterChangeStatusEnum {

    PENDING(0, "待审核"),
    APPROVED(1, "已通过"),
    REJECTED(2, "已驳回"),
    WITHDRAWN(3, "已撤回");

    private final int code;
    private final String label;

    RosterChangeStatusEnum(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public boolean canWithdraw() {
        return this == PENDING;
    }

    public static RosterChangeStatusEnum fromCode(Integer code) {
        if (code == null) {
            throw new ServiceException("未知阵容变更状态");
        }
        for (RosterChangeStatusEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new ServiceException("未知阵容变更状态");
    }
}
