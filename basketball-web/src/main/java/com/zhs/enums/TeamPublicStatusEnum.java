package com.zhs.enums;

import com.zhs.exception.ServiceException;

public enum TeamPublicStatusEnum {

    PRIVATE(0, "非公开"),
    PUBLIC(1, "公开");

    private final int code;
    private final String description;

    TeamPublicStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }


    public static TeamPublicStatusEnum fromCode(Integer code) {
        if (code == null) {
            throw new ServiceException("未知状态");
        }
        for (TeamPublicStatusEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new ServiceException("未知状态");
    }
}
