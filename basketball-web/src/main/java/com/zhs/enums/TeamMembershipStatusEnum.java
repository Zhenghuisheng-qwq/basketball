package com.zhs.enums;

import com.zhs.exception.ServiceException;

public enum TeamMembershipStatusEnum {

    LEFT(0, "已离队"),
    ACTIVE(1, "在队"),
    REMOVED(2, "被移除");

    private final int code;
    private final String description;

    TeamMembershipStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }


    public static TeamMembershipStatusEnum fromCode(Integer code) {
        if (code == null) {
            throw new ServiceException("未知状态");
        }
        for (TeamMembershipStatusEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new ServiceException("未知状态");
    }
}
