package com.zhs.enums;

import com.zhs.exception.ServiceException;

/**
 * 球员参赛资格状态（basket_player_profile.player_status）
 */
public enum PlayerStatusEnum {

    NORMAL(1, "正常"),
    RESTRICTED(2, "受限");

    private final int code;
    private final String label;

    PlayerStatusEnum(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public boolean isRestricted() {
        return this == RESTRICTED;
    }

    public static PlayerStatusEnum fromCode(Integer code) {
        if (code == null) {
            return NORMAL;
        }
        for (PlayerStatusEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new ServiceException("未知球员状态");
    }
}
