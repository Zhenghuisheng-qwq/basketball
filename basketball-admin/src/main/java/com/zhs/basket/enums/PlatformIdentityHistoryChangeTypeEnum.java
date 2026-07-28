package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

public enum PlatformIdentityHistoryChangeTypeEnum {
    INITIAL_BIND("INITIAL_BIND", "首次绑定"),
    UPDATE_GAME_ID("UPDATE_GAME_ID", "修改游戏 ID");

    private final String code;
    private final String description;

    PlatformIdentityHistoryChangeTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static PlatformIdentityHistoryChangeTypeEnum fromCode(String code) {
        if (code == null) {
            throw new ServiceException("未知变更类型");
        }
        for (PlatformIdentityHistoryChangeTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new ServiceException("未知变更类型");
    }

    public static String mapRequestChangeType(String requestChangeType) {
        if (requestChangeType == null) {
            return INITIAL_BIND.getCode();
        }
        String normalized = requestChangeType.trim().toUpperCase();
        if ("CHANGE_GAME_ID".equals(normalized) || UPDATE_GAME_ID.code.equals(normalized)) {
            return UPDATE_GAME_ID.getCode();
        }
        return INITIAL_BIND.getCode();
    }
}
