package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

public enum TournamentStatusEnum {
    DISABLED(0, "已禁用"),
    ACTIVE(1, "已发布"),
    ENDED(2, "已结束"),
    DRAFT(3, "草稿");

    private final int code;
    private final String description;

    TournamentStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }

    public static TournamentStatusEnum fromCode(Integer code) {
        if (code == null) throw new ServiceException("未知赛事状态");
        for (TournamentStatusEnum v : values()) {
            if (v.code == code) return v;
        }
        throw new ServiceException("未知赛事状态");
    }
}
