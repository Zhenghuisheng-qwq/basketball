package com.zhs.enums;

import com.zhs.exception.ServiceException;

public enum TeamInvitationStatusEnum {

    PENDING(0, "待响应"),
    ACCEPTED(1, "已接受"),
    REJECTED(2, "已拒绝"),
    EXPIRED(3, "已过期"),
    CANCELLED(4, "已取消");

    private final int code;
    private final String description;

    TeamInvitationStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public boolean canRespond() {
        return this == PENDING;
    }

    public static TeamInvitationStatusEnum fromCode(Integer code) {
        if (code == null) {
            throw new ServiceException("未知状态");
        }
        for (TeamInvitationStatusEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new ServiceException("未知状态");
    }
}
