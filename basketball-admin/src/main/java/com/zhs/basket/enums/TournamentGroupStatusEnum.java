package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

public enum TournamentGroupStatusEnum {
    DISABLED(0, "已禁用"),
    ACTIVE(1, "正常");

    private final int code;
    private final String description;

    TournamentGroupStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }

    public static TournamentGroupStatusEnum fromCode(Integer code) {
        if (code == null) throw new ServiceException("未知分组状态");
        for (TournamentGroupStatusEnum v : values()) {
            if (v.code == code) return v;
        }
        throw new ServiceException("未知分组状态");
    }
}
