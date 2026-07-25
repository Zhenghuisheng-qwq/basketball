package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

public enum TeamPublicStatusEnum {

    PRIVATE(0, "不公开招募"),
    PUBLIC(1, "公开招募");

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
            throw new ServiceException("未知招募状态");
        }
        for (TeamPublicStatusEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new ServiceException("未知招募状态");
    }
}
