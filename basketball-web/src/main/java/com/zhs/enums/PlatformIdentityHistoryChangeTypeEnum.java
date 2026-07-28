package com.zhs.enums;

import com.zhs.exception.ServiceException;

/**
 * 平台游戏 ID 有效变更历史类型
 */
public enum PlatformIdentityHistoryChangeTypeEnum {

    INITIAL_BIND("INITIAL_BIND", "首次绑定"),
    UPDATE_GAME_ID("UPDATE_GAME_ID", "修改游戏 ID");

    private final String code;
    private final String label;

    PlatformIdentityHistoryChangeTypeEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static PlatformIdentityHistoryChangeTypeEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new ServiceException("变更类型不能为空");
        }
        String normalized = code.trim().toUpperCase();
        for (PlatformIdentityHistoryChangeTypeEnum value : values()) {
            if (value.code.equals(normalized)) {
                return value;
            }
        }
        throw new ServiceException("不支持的变更类型: " + code);
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
