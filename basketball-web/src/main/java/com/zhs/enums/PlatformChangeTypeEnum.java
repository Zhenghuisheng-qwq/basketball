package com.zhs.enums;

import com.zhs.exception.ServiceException;

/**
 * 平台游戏 ID 变更类型
 */
public enum PlatformChangeTypeEnum {

    ADD_PLATFORM("ADD_PLATFORM", "新增平台"),
    CHANGE_GAME_ID("CHANGE_GAME_ID", "修改游戏 ID");

    private final String code;
    private final String label;

    PlatformChangeTypeEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static PlatformChangeTypeEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new ServiceException("变更类型不能为空");
        }
        String normalized = code.trim().toUpperCase();
        for (PlatformChangeTypeEnum value : values()) {
            if (value.code.equals(normalized)) {
                return value;
            }
        }
        throw new ServiceException("不支持的变更类型");
    }
}
