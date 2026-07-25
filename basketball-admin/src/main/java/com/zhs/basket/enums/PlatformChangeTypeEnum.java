package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

public enum PlatformChangeTypeEnum {
    ADD_PLATFORM("ADD_PLATFORM", "新增平台"),
    CHANGE_GAME_ID("CHANGE_GAME_ID", "变更游戏ID");

    private final String code;
    private final String description;

    PlatformChangeTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static PlatformChangeTypeEnum fromCode(String code) {
        if (code == null) {
            throw new ServiceException("未知平台变更类型");
        }
        for (PlatformChangeTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new ServiceException("未知平台变更类型");
    }
}
